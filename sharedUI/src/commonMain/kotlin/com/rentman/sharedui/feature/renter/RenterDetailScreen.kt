package com.rentman.sharedui.feature.renter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentman.domain.DomainError
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.LedgerEntry
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.payments.RenterLedger
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterSummary
import com.rentman.sharedui.components.ActionButton
import com.rentman.sharedui.components.ActionRow
import com.rentman.sharedui.components.BalanceText
import com.rentman.sharedui.components.CenteredMessage
import com.rentman.sharedui.components.ErrorMessage
import com.rentman.sharedui.components.PhotoOpener
import com.rentman.sharedui.components.ScreenTopBar
import com.rentman.sharedui.components.UndoBar
import com.rentman.sharedui.components.label
import com.rentman.sharedui.components.message
import com.rentman.sharedui.components.modeLabel
import com.rentman.sharedui.components.rememberPhotoOpener
import com.rentman.sharedui.components.formatRupees
import com.rentman.sharedui.feature.renter.RenterDetailIntent.Back
import com.rentman.sharedui.feature.renter.RenterDetailIntent.Edit
import com.rentman.sharedui.feature.renter.RenterDetailIntent.MarkLeft
import com.rentman.sharedui.feature.renter.RenterDetailIntent.MoveBack
import com.rentman.sharedui.feature.renter.RenterDetailIntent.RecordPayment
import com.rentman.sharedui.feature.renter.RenterDetailIntent.RemovePayment
import com.rentman.sharedui.feature.renter.RenterDetailIntent.Retry
import com.rentman.sharedui.feature.renter.RenterDetailIntent.SetRemindersMuted
import com.rentman.sharedui.feature.renter.RenterDetailIntent.RemoveBill
import com.rentman.sharedui.feature.renter.RenterDetailIntent.UndoRemove
import com.rentman.sharedui.feature.renter.RenterDetailIntent.UndoRemoveBill
import com.rentman.sharedui.feature.renter.RenterDetailUiState.Content
import com.rentman.sharedui.feature.renter.RenterDetailUiState.Error
import com.rentman.sharedui.feature.renter.RenterDetailUiState.Loading
import com.rentman.sharedui.mvi.NavigationEffect
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.edit_action
import com.rentman.sharedui.resources.history
import com.rentman.sharedui.resources.left_on
import com.rentman.sharedui.resources.mark_left
import com.rentman.sharedui.resources.mark_left_blocked
import com.rentman.sharedui.resources.bill_removed
import com.rentman.sharedui.resources.meter_photo_view
import com.rentman.sharedui.resources.remove_bill
import com.rentman.sharedui.resources.move_back
import com.rentman.sharedui.resources.ledger_bill
import com.rentman.sharedui.resources.loading
import com.rentman.sharedui.resources.no_history
import com.rentman.sharedui.resources.payment_removed
import com.rentman.sharedui.resources.record_payment
import com.rentman.sharedui.resources.reminders_resume
import com.rentman.sharedui.resources.reminders_stop
import com.rentman.sharedui.resources.reminders_stopped
import com.rentman.sharedui.resources.remove_payment
import com.rentman.sharedui.theme.RentManColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The only composable that knows the ViewModel (ARCHITECTURE.md §5). */
@Composable
internal fun RenterDetailRoute(
    renterId: RenterId,
    onRecordPayment: () -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    viewModel: RenterDetailViewModel = koinViewModel { parametersOf(renterId.value) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NavigationEffect(viewModel.navigation) { event ->
        when (event) {
            RenterDetailNavEvent.OpenRecordPayment -> onRecordPayment()
            RenterDetailNavEvent.OpenEdit -> onEdit()
            RenterDetailNavEvent.Close -> onBack()
        }
    }
    RenterDetailScreen(state, viewModel::onIntent)
}

@Composable
internal fun RenterDetailScreen(state: RenterDetailUiState, onIntent: (RenterDetailIntent) -> Unit) {
    Column(Modifier.fillMaxSize().background(RentManColors.Background)) {
        val title = (state as? Content)?.ledger?.summary?.renter?.name.orEmpty()
        ScreenTopBar(
            title = title,
            onBack = { onIntent(Back) },
            action = (state as? Content)?.let { stringResource(Res.string.edit_action) to { onIntent(Edit) } },
        )
        when (state) {
            Loading -> CenteredMessage(stringResource(Res.string.loading))
            is Error -> ErrorMessage(state.error, onRetry = { onIntent(Retry) })
            is Content -> Body(state, onIntent)
        }
    }
}

@Composable
private fun Body(state: Content, onIntent: (RenterDetailIntent) -> Unit) {
    val photoOpener = rememberPhotoOpener()
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "balance") { BalanceCard(state.ledger) }
            val renter = state.ledger.summary.renter
            if (!renter.active) {
                item(key = "left") { LeftRow(renter.leftOn, onMoveBack = { onIntent(MoveBack) }) }
            } else {
                if (state.ledger.pending > Paise.Zero) {
                    item(key = "reminders") {
                        RemindersRow(muted = state.ledger.remindersMuted, onChange = { onIntent(SetRemindersMuted(it)) })
                    }
                }
                item(key = "leave") {
                    val owes = state.ledger.pending > Paise.Zero
                    ActionRow(
                        title = stringResource(Res.string.mark_left),
                        onClick = { onIntent(MarkLeft) },
                        hint = if (owes) stringResource(Res.string.mark_left_blocked) else null,
                        enabled = !owes,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            item(key = "history") {
                BasicText(
                    stringResource(Res.string.history),
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text),
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                )
            }
            if (state.ledger.entries.isEmpty()) {
                item(key = "empty") {
                    BasicText(stringResource(Res.string.no_history), style = TextStyle(fontSize = 15.sp, color = RentManColors.TextMuted))
                }
            }
            items(state.ledger.entries, key = { entry -> entryKey(entry) }) { entry ->
                when (entry) {
                    is LedgerEntry.BillEntry -> BillRow(
                        entry,
                        photoOpener,
                        canRemove = entry.bill.id == state.ledger.removableBill,
                        onRemove = { onIntent(RemoveBill(entry.bill)) },
                    )
                    // Someone who left can't have a payment taken back: they would owe money again while out of the totals.
                    is LedgerEntry.PaymentEntry -> PaymentRow(entry, canRemove = state.ledger.summary.renter.active, onRemove = { onIntent(RemovePayment(entry.payment)) })
                }
            }
        }
        state.actionError?.let { error ->
            BasicText(error.message(), style = TextStyle(fontSize = 14.sp, color = RentManColors.Danger), modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }
        state.removed?.let { removed ->
            UndoBar(stringResource(Res.string.payment_removed, formatRupees(removed.amount)), onUndo = { onIntent(UndoRemove(removed)) })
        }
        state.removedBill?.let { removed ->
            UndoBar(stringResource(Res.string.bill_removed, removed.month.label()), onUndo = { onIntent(UndoRemoveBill(removed)) })
        }
        if (state.ledger.summary.renter.active) {
            ActionButton(
                text = stringResource(Res.string.record_payment),
                enabled = true,
                onClick = { onIntent(RecordPayment) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}

private fun entryKey(entry: LedgerEntry): String = when (entry) {
    is LedgerEntry.BillEntry -> "bill-${entry.bill.id.value}"
    is LedgerEntry.PaymentEntry -> "payment-${entry.payment.id.value}"
}

@Composable
private fun BalanceCard(ledger: RenterLedger) {
    val shape = RoundedCornerShape(20.dp)
    val summary = ledger.summary
    val where = listOfNotNull(summary.renter.unit, summary.address.name).joinToString(" · ")
    Column(
        Modifier.fillMaxWidth().background(RentManColors.Surface, shape).border(1.dp, RentManColors.Border, shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(where, style = TextStyle(fontSize = 14.sp, color = RentManColors.TextMuted))
        BalanceText(ledger.pending, style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = RentManColors.Text))
    }
}

@Composable
private fun LeftRow(leftOn: LocalDate?, onMoveBack: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).background(RentManColors.Surface, shape).border(1.dp, RentManColors.Border, shape).padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = leftOn?.let { stringResource(Res.string.left_on, it.label()) }.orEmpty(),
            style = TextStyle(fontSize = 14.sp, color = RentManColors.Text),
            modifier = Modifier.weight(1f),
        )
        Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onMoveBack).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            BasicText(stringResource(Res.string.move_back), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RentManColors.Primary))
        }
    }
}

@Composable
private fun RemindersRow(muted: Boolean, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val row = Modifier.fillMaxWidth().heightIn(min = 56.dp).background(RentManColors.Surface, shape).border(1.dp, RentManColors.Border, shape)
    if (muted) {
        Row(row.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                stringResource(Res.string.reminders_stopped),
                style = TextStyle(fontSize = 14.sp, color = RentManColors.Text),
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { onChange(false) }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                BasicText(stringResource(Res.string.reminders_resume), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RentManColors.Primary))
            }
        }
    } else {
        Box(row.clickable(role = Role.Button) { onChange(true) }.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            BasicText(stringResource(Res.string.reminders_stop), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RentManColors.Primary))
        }
    }
}

@Composable
private fun BillRow(entry: LedgerEntry.BillEntry, photoOpener: PhotoOpener, canRemove: Boolean, onRemove: () -> Unit) {
    // The photo is a file on this phone: after a restore on another phone it may not be there, and then there is nothing to open.
    val photo = remember(entry.bill.meterPhotoPath) { entry.bill.meterPhotoPath?.takeIf { photoOpener.exists(it) } }
    EntryCard(
        title = stringResource(Res.string.ledger_bill, entry.bill.month.label()),
        subtitle = entry.date.label(),
        amount = entry.bill.total,
        sign = "+",
        balanceAfter = entry.balanceAfter,
        actions = listOfNotNull(
            photo?.let { EntryAction(stringResource(Res.string.meter_photo_view), RentManColors.Primary) { photoOpener.open(it) } },
            if (canRemove) EntryAction(stringResource(Res.string.remove_bill), RentManColors.Danger, onRemove) else null,
        ),
    )
}

@Composable
private fun PaymentRow(entry: LedgerEntry.PaymentEntry, canRemove: Boolean, onRemove: () -> Unit) {
    val payment = entry.payment
    EntryCard(
        title = listOfNotNull(payment.mode.modeLabel(), payment.note).joinToString(" · "),
        subtitle = entry.date.label(),
        amount = payment.amount,
        sign = "−",
        balanceAfter = entry.balanceAfter,
        actions = listOfNotNull(if (canRemove) EntryAction(stringResource(Res.string.remove_payment), RentManColors.Danger, onRemove) else null),
    )
}

/** A tappable word at the bottom of a history entry, e.g. "Remove". */
private class EntryAction(val text: String, val color: Color, val onClick: () -> Unit)

@Composable
private fun EntryCard(
    title: String,
    subtitle: String,
    amount: Paise,
    sign: String,
    balanceAfter: Paise,
    actions: List<EntryAction>,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().background(RentManColors.Surface, shape).border(1.dp, RentManColors.Border, shape).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(title, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text))
                BasicText(subtitle, style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted))
            }
            BasicText(
                text = sign + formatRupees(amount),
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (sign == "+") RentManColors.Text else RentManColors.Success),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BalanceText(balanceAfter, style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted), modifier = Modifier.weight(1f))
            actions.forEach { action ->
                Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = action.onClick).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                    BasicText(action.text, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = action.color))
                }
            }
        }
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
private val previewBill = Bill(
    id = com.rentman.domain.billing.BillId(1),
    renterId = previewRenter.id,
    month = BillingMonth.of(2026, 11),
    createdOn = LocalDate(2026, 11, 3),
    dueOn = LocalDate(2026, 12, 5),
    lines = listOf(BillLine(BillLineKind.Rent, "Rent", 8_380L.rupees)),
)
private val previewPayment = Payment(PaymentId(1), previewRenter.id, 5_000L.rupees, LocalDate(2026, 11, 22), PaymentMode.Upi, "first half")
private val previewLedger = RenterLedger(
    summary = RenterSummary(previewRenter, previewAddress),
    pending = 3_380L.rupees,
    entries = listOf(
        LedgerEntry.PaymentEntry(previewPayment, previewPayment.paidOn, 3_380L.rupees),
        LedgerEntry.BillEntry(previewBill, LocalDate(2026, 11, 3), 8_380L.rupees),
    ),
    remindersMuted = false,
)

@Preview
@Composable
private fun LoadingPreview() = RenterDetailScreen(Loading) {}

@Preview
@Composable
private fun ErrorPreview() = RenterDetailScreen(Error(DomainError.Storage("disk"))) {}

@Preview
@Composable
private fun ContentPreview() = RenterDetailScreen(Content(previewLedger)) {}

@Preview
@Composable
private fun RemindersStoppedPreview() = RenterDetailScreen(Content(previewLedger.copy(remindersMuted = true))) {}

@Preview
@Composable
private fun LeftPreview() = RenterDetailScreen(
    Content(previewLedger.copy(summary = previewLedger.summary.copy(renter = previewRenter.copy(active = false, leftOn = LocalDate(2026, 12, 10))), pending = Paise.Zero)),
) {}

@Preview
@Composable
private fun UndoPreview() = RenterDetailScreen(Content(previewLedger, removed = previewPayment)) {}
