package com.rentman.sharedui.feature.bill

import com.rentman.sharedui.components.CenteredMessage
import com.rentman.sharedui.components.ErrorMessage
import com.rentman.sharedui.components.TextAction
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillContext
import com.rentman.domain.billing.BillField
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillMessage
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.WhatsAppLink
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterSummary
import com.rentman.sharedui.components.ActionButton
import com.rentman.sharedui.components.ActionRow
import com.rentman.sharedui.components.AmountText
import com.rentman.sharedui.components.ScreenTopBar
import com.rentman.sharedui.components.formatRupees
import com.rentman.sharedui.components.label
import com.rentman.sharedui.components.shortLabel
import com.rentman.sharedui.components.message
import com.rentman.sharedui.components.MeterPhotoThumbnail
import com.rentman.sharedui.components.MeterScanFlow
import com.rentman.sharedui.components.rememberPhotoOpener
import com.rentman.sharedui.feature.bill.CreateBillIntent.Back
import com.rentman.sharedui.feature.bill.CreateBillIntent.ChangeReading
import com.rentman.sharedui.feature.bill.CreateBillIntent.ClearMeterPhoto
import com.rentman.sharedui.feature.bill.CreateBillIntent.MeterScanned
import com.rentman.sharedui.feature.bill.CreateBillIntent.ReadingPicked
import com.rentman.sharedui.feature.bill.CreateBillIntent.MeterReplacedChanged
import com.rentman.sharedui.feature.bill.CreateBillIntent.NewMeterStartChanged
import com.rentman.sharedui.feature.bill.CreateBillIntent.ReadingChanged
import com.rentman.sharedui.feature.bill.CreateBillIntent.Retry
import com.rentman.sharedui.feature.bill.CreateBillIntent.Review
import com.rentman.sharedui.feature.bill.CreateBillIntent.Save
import com.rentman.sharedui.feature.bill.CreateBillUiState.Content
import com.rentman.sharedui.feature.bill.CreateBillUiState.Error
import com.rentman.sharedui.feature.bill.CreateBillUiState.Loading
import com.rentman.sharedui.mvi.NavigationEffect
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.bill_title
import com.rentman.sharedui.resources.change_reading
import com.rentman.sharedui.resources.credit_from_payments
import com.rentman.sharedui.resources.electricity
import com.rentman.sharedui.resources.last_reading
import com.rentman.sharedui.resources.meter_photo_attached
import com.rentman.sharedui.resources.meter_photo_gallery
import com.rentman.sharedui.resources.scan_from_gallery
import com.rentman.sharedui.resources.meter_photo_remove
import com.rentman.sharedui.resources.meter_photo_retake
import com.rentman.sharedui.resources.scan_meter
import com.rentman.sharedui.resources.scan_meter_hint
import com.rentman.sharedui.resources.scan_nothing
import com.rentman.sharedui.resources.reading_choices
import com.rentman.sharedui.resources.scan_read
import com.rentman.sharedui.resources.loading
import com.rentman.sharedui.resources.math_rate
import com.rentman.sharedui.resources.math_units
import com.rentman.sharedui.resources.meter_replaced_question
import com.rentman.sharedui.resources.new_meter_required
import com.rentman.sharedui.resources.new_meter_start
import com.rentman.sharedui.resources.new_reading
import com.rentman.sharedui.resources.pay_by
import com.rentman.sharedui.resources.previous_pending
import com.rentman.sharedui.resources.reading_invalid
import com.rentman.sharedui.resources.reading_required
import com.rentman.sharedui.resources.reading_too_low
import com.rentman.sharedui.resources.review_bill
import com.rentman.sharedui.resources.same_meter
import com.rentman.sharedui.resources.save_and_send
import com.rentman.sharedui.resources.save_only
import com.rentman.sharedui.resources.total_due
import com.rentman.sharedui.resources.usage_warning
import com.rentman.sharedui.theme.RentManColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The only composable that knows the ViewModel (ARCHITECTURE.md §5). */
@Composable
internal fun CreateBillRoute(
    renterId: RenterId,
    month: BillingMonth,
    onDone: () -> Unit,
    viewModel: CreateBillViewModel = koinViewModel { parametersOf(renterId.value, month.value) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    NavigationEffect(viewModel.navigation) { event ->
        when (event) {
            is CreateBillNavEvent.Saved -> {
                // The bill is already stored. If no app can open the link, there is nothing more to do.
                event.whatsAppUrl?.let { runCatching { uriHandler.openUri(it) } }
                onDone()
            }
            CreateBillNavEvent.Close -> onDone()
        }
    }
    val photoOpener = rememberPhotoOpener()
    MeterScanFlow(onScanned = { photo, lines -> viewModel.onIntent(MeterScanned(photo, lines)) }) { camera, gallery ->
        CreateBillScreen(month, state, onScan = camera, onPick = gallery, onOpenPhoto = photoOpener.open, onIntent = viewModel::onIntent)
    }
}

@Composable
internal fun CreateBillScreen(
    month: BillingMonth,
    state: CreateBillUiState,
    onScan: () -> Unit = {},
    onPick: () -> Unit = {},
    onOpenPhoto: (photo: String) -> Unit = {},
    onIntent: (CreateBillIntent) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(RentManColors.Background)) {
        ScreenTopBar(title = stringResource(Res.string.bill_title, month.label()), onBack = { onIntent(Back) })
        when (state) {
            Loading -> CenteredMessage(stringResource(Res.string.loading))
            is Error -> ErrorMessage(state.error, onRetry = { onIntent(Retry) })
            is Content -> when (state.step) {
                BillStep.Reading -> ReadingStep(state, onScan, onPick, onOpenPhoto, onIntent)
                BillStep.Review -> ReviewStep(state, onOpenPhoto, onIntent)
            }
        }
    }
}

@Composable
private fun WhoLine(state: Content) {
    val renter = state.context.summary.renter
    val where = listOfNotNull(renter.unit, state.context.summary.address.name).joinToString(" · ")
    BasicText(
        text = "${renter.name} · $where",
        style = TextStyle(fontSize = 14.sp, color = RentManColors.TextMuted),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

@Composable
private fun ColumnScope.ReadingStep(
    state: Content,
    onScan: () -> Unit,
    onPick: () -> Unit,
    onOpenPhoto: (photo: String) -> Unit,
    onIntent: (CreateBillIntent) -> Unit,
) {
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WhoLine(state)
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier.fillMaxWidth().background(RentManColors.Surface, shape).border(2.dp, RentManColors.Primary, shape).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BasicText(stringResource(Res.string.new_reading), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.TextMuted))
            BasicTextField(
                value = state.reading,
                onValueChange = { onIntent(ReadingChanged(it)) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = RentManColors.Text),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            )
            state.context.previousReading?.let {
                BasicText(stringResource(Res.string.last_reading, it.toString()), style = TextStyle(fontSize = 14.sp, color = RentManColors.TextMuted))
            }
            // Hidden when the only number read is already the reading.
            if (state.readingChoices.any { it.toString() != state.reading }) ReadingChoices(state, onPick = { onIntent(ReadingPicked(it)) })
        }
        MeterPhotoRow(state, onScan, onPick, onOpenPhoto, onClear = { onIntent(ClearMeterPhoto) })
        Math(state)
        UsageWarning(state)
        MeterReplaced(state, onIntent)
        ProblemText(state)
    }
    ActionButton(
        text = stringResource(Res.string.review_bill),
        enabled = true,
        onClick = { onIntent(Review) },
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    )
}

/** The other numbers read from the photo, in case the first guess was a serial number or took a decimal digit as a whole one. */
@Composable
private fun ReadingChoices(state: Content, onPick: (Int) -> Unit) {
    BasicText(stringResource(Res.string.reading_choices), style = TextStyle(fontSize = 14.sp, color = RentManColors.TextMuted), modifier = Modifier.padding(top = 6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.readingChoices.forEach { value ->
            val selected = state.reading == value.toString()
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .background(if (selected) RentManColors.PrimarySoft else RentManColors.Surface, shape)
                    .border(if (selected) 2.dp else 1.dp, if (selected) RentManColors.Primary else RentManColors.Border, shape)
                    .clickable(role = Role.Button) { onPick(value) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(value.toString(), style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text))
            }
        }
    }
}

/** The working shown to the landlord and the renter: 1325 − 1240 = 85 units × ₹8 = ₹680. */
@Composable
private fun Math(state: Content) {
    val bill = (state.preview as? Outcome.Success)?.value ?: return
    val units = bill.units ?: return
    val electricity = bill.lines.firstOrNull { it.kind == BillLineKind.Electricity } ?: return
    Column(
        Modifier.fillMaxWidth().background(RentManColors.PrimarySoft, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(
            stringResource(Res.string.math_units, bill.currentReading.toString(), bill.previousReading.toString(), units.toString()),
            style = TextStyle(fontSize = 15.sp, color = RentManColors.Text),
        )
        bill.rate?.let {
            BasicText(stringResource(Res.string.math_rate, formatRupees(it)), style = TextStyle(fontSize = 15.sp, color = RentManColors.Text))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                stringResource(Res.string.electricity),
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text),
                modifier = Modifier.weight(1f),
            )
            AmountText(electricity.amount, style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = RentManColors.Primary))
        }
    }
}

/** Scan or choose a photo of the meter, or, once there is one, show it with Retake, Gallery and Remove. */
@Composable
private fun MeterPhotoRow(state: Content, onScan: () -> Unit, onPick: () -> Unit, onOpenPhoto: (photo: String) -> Unit, onClear: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().background(RentManColors.Surface, shape).border(1.dp, RentManColors.Border, shape).padding(horizontal = 16.dp),
    ) {
        val photo = state.meterPhoto
        if (photo == null) {
            ActionRow(stringResource(Res.string.scan_meter), onClick = onScan, hint = stringResource(Res.string.scan_meter_hint))
            ActionRow(stringResource(Res.string.scan_from_gallery), onClick = onPick)
        } else {
            MeterPhotoThumbnail(photo, onClick = { onOpenPhoto(photo) }, modifier = Modifier.padding(top = 12.dp))
            BasicText(
                stringResource(Res.string.meter_photo_attached),
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text),
                modifier = Modifier.padding(top = 12.dp),
            )
            state.scanResult?.let {
                BasicText(
                    stringResource(if (it == ScanResult.Read) Res.string.scan_read else Res.string.scan_nothing),
                    style = TextStyle(fontSize = 13.sp, color = if (it == ScanResult.Read) RentManColors.Warning else RentManColors.Danger),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Row(Modifier.fillMaxWidth()) {
                ActionRow(stringResource(Res.string.meter_photo_retake), onClick = onScan, modifier = Modifier.weight(1f))
                ActionRow(stringResource(Res.string.meter_photo_gallery), onClick = onPick, modifier = Modifier.weight(1f))
                ActionRow(stringResource(Res.string.meter_photo_remove), onClick = onClear, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** A typo like 13250 for 1325 would become a ₹96,000 bill, so units far above the usual get a warning. */
@Composable
private fun UsageWarning(state: Content) {
    val bill = (state.preview as? Outcome.Success)?.value ?: return
    val usual = state.context.usualUnits ?: return
    if (!state.context.isUnusualUsage(bill)) return
    BasicText(
        text = stringResource(Res.string.usage_warning, bill.units.toString(), usual.toString()),
        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Danger),
        modifier = Modifier
            .fillMaxWidth()
            .background(RentManColors.DangerSoft, RoundedCornerShape(12.dp))
            .padding(12.dp),
    )
}

@Composable
private fun MeterReplaced(state: Content, onIntent: (CreateBillIntent) -> Unit) {
    if (!state.meterReplaced) {
        TextAction(stringResource(Res.string.meter_replaced_question)) { onIntent(MeterReplacedChanged(true)) }
        return
    }
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().background(RentManColors.Surface, shape).border(1.dp, RentManColors.Border, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(stringResource(Res.string.new_meter_start), style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted))
        BasicTextField(
            value = state.newMeterStart,
            onValueChange = { onIntent(NewMeterStartChanged(it)) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
        )
        TextAction(stringResource(Res.string.same_meter)) { onIntent(MeterReplacedChanged(false)) }
    }
}

@Composable
private fun ProblemText(state: Content) {
    if (!state.showProblems) return
    val error = (state.preview as? Outcome.Failure)?.error ?: return
    val text = when (error) {
        is DomainError.ReadingBelowPrevious -> stringResource(Res.string.reading_too_low, error.previous.toString())
        is DomainError.InvalidBill -> when {
            error.problems[BillField.Reading] == FieldProblem.Required -> stringResource(Res.string.reading_required)
            error.problems[BillField.Reading] == FieldProblem.Invalid -> stringResource(Res.string.reading_invalid)
            else -> stringResource(Res.string.new_meter_required)
        }
        else -> error.message()
    }
    BasicText(text, style = TextStyle(fontSize = 14.sp, color = RentManColors.Danger), modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun ColumnScope.ReviewStep(state: Content, onOpenPhoto: (photo: String) -> Unit, onIntent: (CreateBillIntent) -> Unit) {
    val bill = (state.preview as? Outcome.Success)?.value
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WhoLine(state)
        UsageWarning(state)
        if (bill != null) BillCard(bill, state.context)
        bill?.meterPhotoPath?.let { photo ->
            // Next to the total, so the number on the bill can be checked against the meter before it is saved.
            MeterPhotoThumbnail(photo, onClick = { onOpenPhoto(photo) })
            BasicText(stringResource(Res.string.meter_photo_attached), style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted), modifier = Modifier.padding(horizontal = 4.dp))
        }
        state.saveError?.let {
            BasicText(it.message(), style = TextStyle(fontSize = 14.sp, color = RentManColors.Danger), modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (bill != null) {
            val monthLabel = state.context.month.label()
            val dueLabel = bill.dueOn.shortLabel()
            val renter = state.context.summary.renter
            ActionButton(
                text = stringResource(Res.string.save_and_send),
                enabled = !state.isSaving,
                onClick = {
                    val message = BillMessage.text(state.context, bill, monthLabel, dueLabel, money = ::formatRupees)
                    onIntent(Save(bill, whatsAppUrl = WhatsAppLink.of(renter.phone, message)))
                },
                modifier = Modifier.fillMaxWidth(),
                color = RentManColors.WhatsApp,
            )
            TextAction(stringResource(Res.string.save_only)) { if (!state.isSaving) onIntent(Save(bill)) }
        }
        if (state.context.summary.renter.billsElectricity) {
            TextAction(stringResource(Res.string.change_reading)) { onIntent(ChangeReading) }
        }
    }
}

@Composable
private fun BillCard(bill: Bill, context: BillContext) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().background(RentManColors.Surface, shape).border(1.dp, RentManColors.Border, shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        bill.lines.forEach { line -> LineRow(line, bill) }
        if (context.pendingBefore > Paise.Zero) {
            LineRow(label = stringResource(Res.string.previous_pending), amount = context.pendingBefore, detail = null, muted = true)
        } else if (context.pendingBefore < Paise.Zero) {
            LineRow(label = stringResource(Res.string.credit_from_payments), amount = context.pendingBefore, detail = null, muted = true)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(RentManColors.Border))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                stringResource(Res.string.total_due),
                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text),
                modifier = Modifier.weight(1f),
            )
            AmountText(context.totalDue(bill), style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = RentManColors.Text))
        }
        BasicText(stringResource(Res.string.pay_by, bill.dueOn.label()), style = TextStyle(fontSize = 14.sp, color = RentManColors.TextMuted))
    }
}

@Composable
private fun LineRow(line: BillLine, bill: Bill) {
    val detail = if (line.kind == BillLineKind.Electricity && bill.units != null) {
        stringResource(Res.string.math_units, bill.currentReading.toString(), bill.previousReading.toString(), bill.units.toString())
    } else {
        null
    }
    LineRow(label = line.label, amount = line.amount, detail = detail, muted = false)
}

@Composable
private fun LineRow(label: String, amount: Paise, detail: String?, muted: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(label, style = TextStyle(fontSize = 16.sp, color = if (muted) RentManColors.TextMuted else RentManColors.Text))
            detail?.let { BasicText(it, style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted)) }
        }
        AmountText(
            amount,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (muted) RentManColors.TextMuted else RentManColors.Text),
        )
    }
}



private val previewAddress = Address(AddressId(1), "338 A")
private val previewRenter = Renter(
    id = RenterId(1),
    addressId = previewAddress.id,
    unit = "Ground floor",
    name = "Ravi Kumar",
    phone = "919876543210",
    monthlyRent = 7_500L.rupees,
    dueDay = 5,
    agreementStart = LocalDate(2026, 1, 1),
    raisePercent = 10,
    raiseEveryMonths = 11,
    billsElectricity = true,
    customRate = null,
    startingReading = 1240,
    firstBillMonth = BillingMonth.of(2026, 11),
)
private val previewMonth = BillingMonth.of(2026, 11)
private val previewContext = BillContext(
    summary = RenterSummary(previewRenter, previewAddress),
    month = previewMonth,
    previousReading = 1240,
    rate = Paise(800),
    pendingBefore = 1_880L.rupees,
)
private val previewBill = Bill(
    renterId = previewRenter.id,
    month = previewMonth,
    dueOn = LocalDate(2026, 11, 5),
    previousReading = 1240,
    currentReading = 1325,
    rate = Paise(800),
    lines = listOf(
        BillLine(BillLineKind.Rent, "Rent", 7_500L.rupees),
        BillLine(BillLineKind.Electricity, "Electricity", Paise(68_000)),
        BillLine(BillLineKind.FixedCharge, "Water", 200L.rupees),
    ),
)

@Preview
@Composable
private fun LoadingPreview() = CreateBillScreen(previewMonth, Loading) {}

@Preview
@Composable
private fun ErrorPreview() = CreateBillScreen(previewMonth, Error(DomainError.Storage("disk"))) {}

@Preview
@Composable
private fun ReadingPreview() = CreateBillScreen(
    previewMonth,
    Content(previewContext, BillStep.Reading, reading = "1325", preview = Outcome.Success(previewBill)),
) {}

@Preview
@Composable
private fun ReadingTooLowPreview() = CreateBillScreen(
    previewMonth,
    Content(
        previewContext,
        BillStep.Reading,
        reading = "120",
        preview = Outcome.Failure(DomainError.ReadingBelowPrevious(1240, 120)),
        showProblems = true,
    ),
) {}

@Preview
@Composable
private fun ReviewPreview() = CreateBillScreen(
    previewMonth,
    Content(previewContext, BillStep.Review, reading = "1325", preview = Outcome.Success(previewBill)),
) {}
