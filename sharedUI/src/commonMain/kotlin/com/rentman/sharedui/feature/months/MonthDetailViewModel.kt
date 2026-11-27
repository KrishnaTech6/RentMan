@file:OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest

package com.rentman.sharedui.feature.months

import com.rentman.domain.DomainError
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.MonthDetail
import com.rentman.domain.billing.ObserveMonthDetailUseCase
import com.rentman.domain.fold
import com.rentman.sharedui.feature.months.MonthDetailIntent.Back
import com.rentman.sharedui.feature.months.MonthDetailIntent.OpenRenter
import com.rentman.sharedui.feature.months.MonthDetailIntent.Refresh
import com.rentman.sharedui.feature.months.MonthDetailIntent.Retry
import com.rentman.sharedui.feature.months.MonthDetailIntent.Share
import com.rentman.sharedui.feature.months.MonthDetailUiState.Content
import com.rentman.sharedui.feature.months.MonthDetailUiState.Error
import com.rentman.sharedui.feature.months.MonthDetailUiState.Loading
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

private typealias MonthDetailState = MonthDetailUiState

internal class MonthDetailViewModel(
    month: BillingMonth,
    observeDetail: ObserveMonthDetailUseCase,
) : MviViewModel<MonthDetailIntent, MonthDetailState, MonthDetailNavEvent>() {
    private val unchanged: Reducer<MonthDetailState> = { it }

    override val uiState: StateFlow<MonthDetailState> = merge(loadReducers(month, observeDetail), navigationReducers()).toState(Loading)

    private fun loadReducers(month: BillingMonth, observeDetail: ObserveMonthDetailUseCase): Flow<Reducer<MonthDetailState>> =
        merge(
            intents.filterIsInstance<Retry>().map { true }.onStart { emit(true) },
            intents.filterIsInstance<Refresh>().map { false },
        ).flatMapLatest { showLoading ->
            observeDetail(month)
                .map { outcome -> outcome.fold(onSuccess = ::withDetail, onFailure = ::toError) }
                .let { reducers -> if (showLoading) reducers.onStart { emit { Loading } } else reducers }
        }

    private fun navigationReducers(): Flow<Reducer<MonthDetailState>> = intents
        .onEach { intent ->
            when (intent) {
                is OpenRenter -> navigate(MonthDetailNavEvent.OpenRenter(intent.renterId))
                Back -> navigate(MonthDetailNavEvent.Close)
                is Share -> navigate(MonthDetailNavEvent.Share(intent.detail))
                Retry, Refresh -> Unit
            }
        }
        .map { unchanged }

    private fun withDetail(detail: MonthDetail): Reducer<MonthDetailState> = { Content(detail) }

    private fun toError(error: DomainError): Reducer<MonthDetailState> = { Error(error) }
}
