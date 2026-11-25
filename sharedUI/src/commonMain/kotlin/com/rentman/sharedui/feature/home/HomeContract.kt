package com.rentman.sharedui.feature.home

import com.rentman.domain.DomainError
import com.rentman.domain.billing.BillTodo
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.RenterRow
import com.rentman.domain.billing.RenterStatus
import com.rentman.domain.billing.MonthSummary
import com.rentman.domain.tenancy.RentChange
import com.rentman.domain.tenancy.RentIncreaseOffer
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterSummary

internal sealed interface HomeIntent {
    data object AddRenter : HomeIntent
    data class CreateBill(val renterId: RenterId, val month: BillingMonth) : HomeIntent
    data class OpenRenter(val renterId: RenterId) : HomeIntent
    data object OpenSettings : HomeIntent
    data object OpenMonths : HomeIntent
    /** Show only the renters with [status], or everyone when it is null. */
    data class SelectFilter(val status: RenterStatus?) : HomeIntent
    data class RaiseRent(val offer: RentIncreaseOffer) : HomeIntent
    data class NotNow(val renterId: RenterId) : HomeIntent
    data class UndoRaise(val change: RentChange) : HomeIntent
    /** The screen came back to the foreground: look again, e.g. a new month may have started. */
    data object Refresh : HomeIntent

    data object Retry : HomeIntent
}

internal sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Error(val error: DomainError) : HomeUiState

    data class Content(
        val renters: List<RenterRow>,
        val todos: List<BillTodo>,
        val raises: List<RentIncreaseOffer>,
        /** Renters who have left. They can still be opened to see their history or to move them back, and still count in [thisMonth]. */
        val left: List<RenterSummary>,
        val thisMonth: MonthSummary,
        /** The raise just accepted, offered for Undo for a few seconds. */
        val raiseUndo: RentChange? = null,
        /** A failed raise, "not now" or undo. What is on screen is still right. */
        val actionError: DomainError? = null,
        /** The status chosen on the chips, or null for everyone. */
        val filter: RenterStatus? = null,
    ) : HomeUiState {
        /** What the list shows: everyone, or only the renters with the chosen status. */
        val shown: List<RenterRow> get() = filter?.let { status -> renters.filter { it.status == status } } ?: renters

        /** One chip per status somebody has, with how many, most urgent first (the renters are already in that order). */
        val statusCounts: List<Pair<RenterStatus, Int>> get() = renters.groupBy { it.status }.map { (status, rows) -> status to rows.size }
    }
}

internal sealed interface HomeNavEvent {
    data object OpenAddRenter : HomeNavEvent
    data class OpenCreateBill(val renterId: RenterId, val month: BillingMonth) : HomeNavEvent
    data class OpenRenter(val renterId: RenterId) : HomeNavEvent
    data object OpenSettings : HomeNavEvent
    data object OpenMonths : HomeNavEvent
}
