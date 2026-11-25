package com.rentman.sharedui.feature.months

import com.rentman.domain.DomainError
import com.rentman.domain.billing.MonthDetail
import com.rentman.domain.tenancy.RenterId

internal sealed interface MonthDetailIntent {
    data class OpenRenter(val renterId: RenterId) : MonthDetailIntent
    data object Back : MonthDetailIntent

    /** The screen came back to the foreground: look again. */
    data object Refresh : MonthDetailIntent

    data object Retry : MonthDetailIntent
}

internal sealed interface MonthDetailUiState {
    data object Loading : MonthDetailUiState

    data class Error(val error: DomainError) : MonthDetailUiState

    data class Content(val detail: MonthDetail) : MonthDetailUiState
}

internal sealed interface MonthDetailNavEvent {
    data class OpenRenter(val renterId: RenterId) : MonthDetailNavEvent
    data object Close : MonthDetailNavEvent
}
