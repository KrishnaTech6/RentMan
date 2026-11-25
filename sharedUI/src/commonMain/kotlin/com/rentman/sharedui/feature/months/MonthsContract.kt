package com.rentman.sharedui.feature.months

import com.rentman.domain.DomainError
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.MonthSummary

internal sealed interface MonthsIntent {
    data class OpenMonth(val month: BillingMonth) : MonthsIntent
    data object Back : MonthsIntent

    /** The screen came back to the foreground: look again, e.g. a new month may have started. */
    data object Refresh : MonthsIntent

    data object Retry : MonthsIntent
}

internal sealed interface MonthsUiState {
    data object Loading : MonthsUiState

    data class Error(val error: DomainError) : MonthsUiState

    /** Newest month first. */
    data class Content(val months: List<MonthSummary>) : MonthsUiState
}

internal sealed interface MonthsNavEvent {
    data class OpenMonth(val month: BillingMonth) : MonthsNavEvent
    data object Close : MonthsNavEvent
}
