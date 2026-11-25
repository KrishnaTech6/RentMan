@file:OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest

package com.rentman.sharedui.feature.months

import com.rentman.domain.DomainError
import com.rentman.domain.billing.MonthSummary
import com.rentman.domain.billing.ObserveMonthsUseCase
import com.rentman.domain.fold
import com.rentman.sharedui.feature.months.MonthsIntent.Back
import com.rentman.sharedui.feature.months.MonthsIntent.OpenMonth
import com.rentman.sharedui.feature.months.MonthsIntent.Refresh
import com.rentman.sharedui.feature.months.MonthsIntent.Retry
import com.rentman.sharedui.feature.months.MonthsUiState.Content
import com.rentman.sharedui.feature.months.MonthsUiState.Error
import com.rentman.sharedui.feature.months.MonthsUiState.Loading
import com.rentman.sharedui.mvi.MviViewModel
import com.rentman.sharedui.mvi.Reducer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

private typealias MonthsState = MonthsUiState

internal class MonthsViewModel(observeMonths: ObserveMonthsUseCase) : MviViewModel<MonthsIntent, MonthsState, MonthsNavEvent>() {
    private val unchanged: Reducer<MonthsState> = { it }

    override val uiState: StateFlow<MonthsState> = merge(loadReducers(observeMonths), navigationReducers()).toState(Loading)

    /** Loads on start, on Retry (showing Loading) and on Refresh (keeping what is on screen, so a new month just appears). */
    private fun loadReducers(observeMonths: ObserveMonthsUseCase): Flow<Reducer<MonthsState>> =
        merge(
            intents.filterIsInstance<Retry>().map { true }.onStart { emit(true) },
            intents.filterIsInstance<Refresh>().map { false },
        ).flatMapLatest { showLoading ->
            observeMonths()
                .map { outcome -> outcome.fold(onSuccess = ::withMonths, onFailure = ::toError) }
                .let { reducers -> if (showLoading) reducers.onStart { emit { Loading } } else reducers }
        }

    private fun navigationReducers(): Flow<Reducer<MonthsState>> = intents
        .onEach { intent ->
            when (intent) {
                is OpenMonth -> navigate(MonthsNavEvent.OpenMonth(intent.month))
                Back -> navigate(MonthsNavEvent.Close)
                Retry, Refresh -> Unit
            }
        }
        .map { unchanged }

    private fun withMonths(months: List<MonthSummary>): Reducer<MonthsState> = { Content(months) }

    private fun toError(error: DomainError): Reducer<MonthsState> = { Error(error) }
}
