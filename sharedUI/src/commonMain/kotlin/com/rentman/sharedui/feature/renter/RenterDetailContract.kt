package com.rentman.sharedui.feature.renter

import com.rentman.domain.DomainError
import com.rentman.domain.billing.Bill
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.RenterLedger

internal sealed interface RenterDetailIntent {
    data object RecordPayment : RenterDetailIntent
    data object Edit : RenterDetailIntent
    /** The renter leaves. Only works once they owe nothing. */
    data object MarkLeft : RenterDetailIntent
    /** The renter lives there again. */
    data object MoveBack : RenterDetailIntent
    /** "Stop this month" ([muted] = true) or "Resume". */
    data class SetRemindersMuted(val muted: Boolean) : RenterDetailIntent
    data class RemovePayment(val payment: Payment) : RenterDetailIntent
    /** Takes a wrong bill out of the history so it can be made again. Only the newest bill is offered for it. */
    data class RemoveBill(val bill: Bill) : RenterDetailIntent
    /** Puts back the payment or bill that was just removed. */
    data class UndoRemove(val entry: RemovedEntry) : RenterDetailIntent
    data object Back : RenterDetailIntent
    data object Retry : RenterDetailIntent
}

/** What was just removed from the history, so Undo knows what to put back. */
internal sealed interface RemovedEntry {
    data class OfPayment(val payment: Payment) : RemovedEntry
    data class OfBill(val bill: Bill) : RemovedEntry
}

internal sealed interface RenterDetailUiState {
    data object Loading : RenterDetailUiState

    data class Error(val error: DomainError) : RenterDetailUiState

    data class Content(
        val ledger: RenterLedger,
        /** The payment or bill just removed, offered for Undo for a few seconds. Only the latest removal is offered. */
        val removed: RemovedEntry? = null,
        /** A failed remove or undo. The ledger on screen is still right. */
        val actionError: DomainError? = null,
    ) : RenterDetailUiState
}

internal sealed interface RenterDetailNavEvent {
    data object OpenRecordPayment : RenterDetailNavEvent
    data object OpenEdit : RenterDetailNavEvent
    data object Close : RenterDetailNavEvent
}
