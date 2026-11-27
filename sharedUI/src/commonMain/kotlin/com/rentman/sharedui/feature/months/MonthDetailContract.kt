package com.rentman.sharedui.feature.months

import com.rentman.domain.DomainError
import com.rentman.domain.billing.MonthDetail
import com.rentman.domain.tenancy.RenterId

internal sealed interface MonthDetailIntent {
    data class OpenRenter(val renterId: RenterId) : MonthDetailIntent
    data object Back : MonthDetailIntent

    /** Post the month review on WhatsApp. */
    data class Share(val detail: MonthDetail, val message: String) : MonthDetailIntent

    /** The screen came back to the foreground: look again. */
    data object Refresh : MonthDetailIntent

    data object Retry : MonthDetailIntent
}

internal sealed interface MonthDetailUiState {
    data object Loading : MonthDetailUiState

    data class Error(val error: DomainError) : MonthDetailUiState

    /** [message] is the wording of the month review: the landlord's own, or ours. */
    data class Content(val detail: MonthDetail, val message: String) : MonthDetailUiState
}

internal sealed interface MonthDetailNavEvent {
    data class OpenRenter(val renterId: RenterId) : MonthDetailNavEvent
    data object Close : MonthDetailNavEvent
    data class Share(val detail: MonthDetail, val message: String) : MonthDetailNavEvent
}
