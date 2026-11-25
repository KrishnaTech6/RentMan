@file:OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest

package com.rentman.sharedui.feature.home

import com.rentman.domain.DomainError
import com.rentman.domain.billing.HomeSnapshot
import com.rentman.domain.billing.ObserveHomeUseCase
import com.rentman.domain.fold
import com.rentman.domain.tenancy.ApplyRentIncreaseUseCase
import com.rentman.domain.tenancy.PostponeRentIncreaseUseCase
import com.rentman.domain.tenancy.UndoRentIncreaseUseCase
import com.rentman.sharedui.feature.home.HomeIntent.AddRenter
import com.rentman.sharedui.feature.home.HomeIntent.CreateBill
import com.rentman.sharedui.feature.home.HomeIntent.OpenRenter
import com.rentman.sharedui.feature.home.HomeIntent.NotNow
import com.rentman.sharedui.feature.home.HomeIntent.OpenMonths
import com.rentman.sharedui.feature.home.HomeIntent.OpenSettings
import com.rentman.sharedui.feature.home.HomeIntent.RaiseRent
import com.rentman.sharedui.feature.home.HomeIntent.UndoRaise
import com.rentman.sharedui.feature.home.HomeIntent.Refresh
import com.rentman.sharedui.feature.home.HomeIntent.Retry
import com.rentman.sharedui.feature.home.HomeIntent.SelectFilter
import com.rentman.sharedui.feature.home.HomeUiState.Content
import com.rentman.sharedui.feature.home.HomeUiState.Error
import com.rentman.sharedui.feature.home.HomeUiState.Loading
import com.rentman.sharedui.mvi.MviViewModel
import com.rentman.sharedui.mvi.Reducer
import com.rentman.sharedui.mvi.flatMapFirst
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

private typealias State = HomeUiState

internal class HomeViewModel(
    observeHome: ObserveHomeUseCase,
    private val applyRentIncrease: ApplyRentIncreaseUseCase,
    private val postponeRentIncrease: PostponeRentIncreaseUseCase,
    private val undoRentIncrease: UndoRentIncreaseUseCase,
) : MviViewModel<HomeIntent, State, HomeNavEvent>() {

    private val unchanged: Reducer<State> = { it }

    override val uiState: StateFlow<State> = merge(
        loadReducers(observeHome),
        raiseReducers(),
        filterReducers(),
        undoExpiryReducers(),
        navigationReducers(),
    ).toState(Loading)

    /**
     * Loads on start, on Retry (showing Loading) and on Refresh (keeping what is on screen, so nothing flashes).
     * Looking again is what lets a new month's "Create bill" card appear in an app that stayed open past midnight.
     */
    private fun loadReducers(observeHome: ObserveHomeUseCase): Flow<Reducer<State>> =
        merge(
            intents.filterIsInstance<Retry>().map { true }.onStart { emit(true) },
            intents.filterIsInstance<Refresh>().map { false },
        ).flatMapLatest { showLoading ->
            observeHome()
                .map { outcome -> outcome.fold(onSuccess = ::withHome, onFailure = ::toError) }
                .let { reducers -> if (showLoading) reducers.onStart { emit { Loading } } else reducers }
        }

    private fun raiseReducers(): Flow<Reducer<State>> = merge(
        intents.filterIsInstance<RaiseRent>().flatMapFirst { raise ->
            flow {
                emit(
                    applyRentIncrease(raise.offer).fold(
                        onSuccess = { change -> content { it.copy(raiseUndo = change, actionError = null) } },
                        onFailure = ::actionFailed,
                    ),
                )
            }
        },
        intents.filterIsInstance<NotNow>().flatMapFirst { notNow ->
            flow {
                emit(
                    postponeRentIncrease(notNow.renterId).fold(
                        onSuccess = { content { it.copy(actionError = null) } },
                        onFailure = ::actionFailed,
                    ),
                )
            }
        },
        intents.filterIsInstance<UndoRaise>().flatMapFirst { undo ->
            flow {
                emit(
                    undoRentIncrease(undo.change).fold(
                        onSuccess = { content { it.copy(raiseUndo = null, actionError = null) } },
                        onFailure = ::actionFailed,
                    ),
                )
            }
        },
    )

    private fun filterReducers(): Flow<Reducer<State>> =
        intents.filterIsInstance<SelectFilter>().map { select -> content { it.copy(filter = select.status) } }

    /** The Undo offer lasts a few seconds. A newer raise starts the wait again. */
    private fun undoExpiryReducers(): Flow<Reducer<State>> = intents.filterIsInstance<RaiseRent>().flatMapLatest {
        flow {
            delay(UNDO_MILLIS)
            emit(content { it.copy(raiseUndo = null, actionError = null) })
        }
    }

    private fun navigationReducers(): Flow<Reducer<State>> = intents
        .onEach { intent ->
            when (intent) {
                AddRenter -> navigate(HomeNavEvent.OpenAddRenter)
                is CreateBill -> navigate(HomeNavEvent.OpenCreateBill(intent.renterId, intent.month))
                is OpenRenter -> navigate(HomeNavEvent.OpenRenter(intent.renterId))
                OpenSettings -> navigate(HomeNavEvent.OpenSettings)
                OpenMonths -> navigate(HomeNavEvent.OpenMonths)
                is RaiseRent, is NotNow, is UndoRaise, is SelectFilter, Retry, Refresh -> Unit
            }
        }
        .map { unchanged }

    /**
     * Keeps the Undo offer, any error and the chosen filter on screen when the data arrives again, e.g. right after the raise itself.
     * A filter is dropped when nobody matches it any more (the last late renter just paid), and when everybody matches it: the chips
     * are hidden then, so the filter could not be cleared and would keep the left renters out of sight.
     */
    private fun withHome(home: HomeSnapshot): Reducer<State> = { state ->
        val shown = Content(home.renters, home.todos, home.raises, home.left, home.thisMonth)
        if (state is Content) {
            shown.copy(
                raiseUndo = state.raiseUndo,
                actionError = state.actionError,
                filter = state.filter?.takeIf { status -> home.renters.any { it.status == status } && home.renters.any { it.status != status } },
            )
        } else {
            shown
        }
    }

    private fun content(change: (Content) -> Content): Reducer<State> = { state ->
        if (state is Content) change(state) else state
    }

    private fun actionFailed(error: DomainError): Reducer<State> = content { it.copy(actionError = error) }

    private companion object {
        const val UNDO_MILLIS = 5_000L
    }

    private fun toError(error: DomainError): Reducer<State> = { Error(error) }
}
