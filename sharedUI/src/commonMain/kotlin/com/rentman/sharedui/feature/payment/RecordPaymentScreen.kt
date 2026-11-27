package com.rentman.sharedui.feature.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentman.domain.DomainError
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.WhatsAppLink
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.PaymentField
import com.rentman.domain.messages.MessageKind
import com.rentman.domain.payments.PaymentMessage
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterSummary
import com.rentman.sharedui.components.ActionButton
import com.rentman.sharedui.components.ActionRow
import com.rentman.sharedui.components.CenteredMessage
import com.rentman.sharedui.components.Divider
import com.rentman.sharedui.components.ErrorMessage
import com.rentman.sharedui.components.ScreenTopBar
import com.rentman.sharedui.components.Section
import com.rentman.sharedui.components.Segmented
import com.rentman.sharedui.components.StepButton
import com.rentman.sharedui.components.TextAction
import com.rentman.sharedui.components.TextRow
import com.rentman.sharedui.components.formatRupees
import com.rentman.sharedui.components.label
import com.rentman.sharedui.components.message
import com.rentman.sharedui.components.modeLabel
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.AmountChanged
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.Back
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.DayStepped
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.ModeChanged
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.NoteChanged
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.Retry
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.Save
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.UseFullAmount
import com.rentman.sharedui.feature.payment.RecordPaymentUiState.Content
import com.rentman.sharedui.feature.payment.RecordPaymentUiState.Error
import com.rentman.sharedui.feature.payment.RecordPaymentUiState.Loading
import com.rentman.sharedui.mvi.NavigationEffect
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.field_amount
import com.rentman.sharedui.resources.field_date
import com.rentman.sharedui.resources.field_note
import com.rentman.sharedui.resources.field_paid_by
import com.rentman.sharedui.resources.full_amount
import com.rentman.sharedui.resources.loading
import com.rentman.sharedui.resources.next_day
import com.rentman.sharedui.resources.overpayment_hint
import com.rentman.sharedui.resources.payment_pending_hint
import com.rentman.sharedui.resources.previous_day
import com.rentman.sharedui.resources.problem_amount
import com.rentman.sharedui.resources.problem_date
import com.rentman.sharedui.resources.problem_note
import com.rentman.sharedui.resources.problem_required
import com.rentman.sharedui.resources.record_payment
import com.rentman.sharedui.resources.save_and_send
import com.rentman.sharedui.resources.save_only
import com.rentman.sharedui.resources.section_note
import com.rentman.sharedui.theme.RentManColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The only composable that knows the ViewModel (ARCHITECTURE.md §5). */
@Composable
internal fun RecordPaymentRoute(
    renterId: RenterId,
    onDone: () -> Unit,
    viewModel: RecordPaymentViewModel = koinViewModel { parametersOf(renterId.value) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    NavigationEffect(viewModel.navigation) { event ->
        if (event is RecordPaymentNavEvent.Saved && event.post != null) {
            // The payment is already stored. No number: WhatsApp asks which chat, so the landlord picks the group.
            val post = event.post
            val message = PaymentMessage.text(post.summary, post.amount, money = ::formatRupees, pending = post.pending, template = post.message)
            runCatching { uriHandler.openUri(WhatsAppLink.of("", message)) }
        }
        onDone()
    }
    RecordPaymentScreen(state, viewModel::onIntent)
}

@Composable
internal fun RecordPaymentScreen(state: RecordPaymentUiState, onIntent: (RecordPaymentIntent) -> Unit) {
    Column(Modifier.fillMaxSize().background(RentManColors.Background)) {
        ScreenTopBar(title = stringResource(Res.string.record_payment), onBack = { onIntent(Back) })
        when (state) {
            Loading -> CenteredMessage(stringResource(Res.string.loading))
            is Error -> ErrorMessage(state.error, onRetry = { onIntent(Retry) })
            is Content -> Form(state, onIntent)
        }
    }
}

@Composable
private fun Form(state: Content, onIntent: (RecordPaymentIntent) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val renter = state.summary.renter
            val where = listOfNotNull(renter.unit, state.summary.address.name).joinToString(" · ")
            BasicText(
                text = "${renter.name} · $where",
                style = TextStyle(fontSize = 14.sp, color = RentManColors.TextMuted),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            Section(stringResource(Res.string.field_amount)) {
                TextRow(
                    label = stringResource(Res.string.payment_pending_hint, formatRupees(state.pending)),
                    value = state.amount,
                    onValueChange = { onIntent(AmountChanged(it)) },
                    problem = problemText(state, PaymentField.Amount),
                    keyboardType = KeyboardType.Decimal,
                    hint = state.overpayment?.let { stringResource(Res.string.overpayment_hint, formatRupees(it)) },
                )
                if (state.pending > Paise.Zero) {
                    Divider()
                    ActionRow(stringResource(Res.string.full_amount, formatRupees(state.pending)), onClick = { onIntent(UseFullAmount) })
                }
            }
            Section(stringResource(Res.string.field_paid_by)) {
                val modes = PaymentMode.entries
                Segmented(
                    options = modes.map { it.modeLabel() },
                    selectedIndex = modes.indexOf(state.mode),
                    onSelect = { onIntent(ModeChanged(modes[it])) },
                )
            }
            Section(stringResource(Res.string.field_date)) {
                DateStepper(state, onStep = { onIntent(DayStepped(it)) })
                problemText(state, PaymentField.Date)?.let {
                    BasicText(it, style = TextStyle(fontSize = 13.sp, color = RentManColors.Danger), modifier = Modifier.padding(bottom = 10.dp))
                }
            }
            Section(stringResource(Res.string.section_note)) {
                TextRow(
                    label = stringResource(Res.string.field_note),
                    value = state.note,
                    onValueChange = { onIntent(NoteChanged(it)) },
                    problem = problemText(state, PaymentField.Note),
                )
            }
            state.saveError?.let {
                BasicText(it.message(), style = TextStyle(fontSize = 14.sp, color = RentManColors.Danger), modifier = Modifier.padding(4.dp))
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(
                text = stringResource(Res.string.save_and_send),
                enabled = !state.isSaving,
                onClick = { onIntent(Save(state.toForm(), post = state.postTo())) },
                modifier = Modifier.fillMaxWidth(),
                color = RentManColors.WhatsApp,
            )
            TextAction(stringResource(Res.string.save_only)) { if (!state.isSaving) onIntent(Save(state.toForm())) }
        }
    }
}

@Composable
private fun problemText(state: Content, field: PaymentField): String? = state.problems[field]?.let { problem ->
    stringResource(
        when {
            problem == FieldProblem.Required -> Res.string.problem_required
            field == PaymentField.Amount -> Res.string.problem_amount
            field == PaymentField.Date -> Res.string.problem_date
            else -> Res.string.problem_note
        },
    )
}

@Composable
private fun DateStepper(state: Content, onStep: (Int) -> Unit) {
    val previous = stringResource(Res.string.previous_day)
    val next = stringResource(Res.string.next_day)
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            text = state.paidOn.label(),
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text),
            modifier = Modifier.weight(1f),
        )
        StepButton("‹", previous) { onStep(-1) }
        StepButton("›", next) { onStep(1) }
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
    billsElectricity = false,
    customRate = null,
    startingReading = null,
    firstBillMonth = BillingMonth.of(2026, 11),
)
private val previewContent = Content(
    summary = RenterSummary(previewRenter, previewAddress),
    pending = 3_380L.rupees,
    message = MessageKind.PaymentReceived.default,
    today = LocalDate(2026, 11, 23),
)

@Preview
@Composable
private fun LoadingPreview() = RecordPaymentScreen(Loading) {}

@Preview
@Composable
private fun ErrorPreview() = RecordPaymentScreen(Error(DomainError.Storage("disk"))) {}

@Preview
@Composable
private fun EmptyPreview() = RecordPaymentScreen(previewContent) {}

@Preview
@Composable
private fun WithProblemsPreview() = RecordPaymentScreen(
    previewContent.copy(
        amount = "abc",
        note = "x".repeat(101),
        problems = mapOf(PaymentField.Amount to FieldProblem.Invalid, PaymentField.Note to FieldProblem.Invalid),
    ),
) {}
