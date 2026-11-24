package com.rentman.sharedui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

/** A pure state change. The whole screen state is built by folding a stream of these. */
internal typealias Reducer<S> = (S) -> S

/**
 * Base for every screen ViewModel (ARCHITECTURE.md §5). One [StateFlow] of state, one [SharedFlow] of
 * one-shot navigation events, and intents flowing in. Subclasses build `uiState` as a pipeline of
 * [Reducer]s with [toState].
 */
internal abstract class MviViewModel<I : Any, S : Any, N : Any> : ViewModel() {
    private val intentFlow = MutableSharedFlow<I>(extraBufferCapacity = INTENT_BUFFER)
    private val navigationFlow = MutableSharedFlow<N>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    protected val intents: Flow<I> = intentFlow
    val navigation: SharedFlow<N> = navigationFlow.asSharedFlow()
    abstract val uiState: StateFlow<S>

    fun onIntent(intent: I) {
        intentFlow.tryEmit(intent)
    }

    protected fun navigate(event: N) {
        navigationFlow.tryEmit(event)
    }

    /**
     * Always Eagerly. With WhileSubscribed, `scan` would start again from [initial] after a short stop and
     * flash the Loading state (or reset a form) while the app was in the background.
     */
    protected fun Flow<Reducer<S>>.toState(initial: S): StateFlow<S> =
        scan(initial) { state, reduce -> reduce(state) }.stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    private companion object {
        const val INTENT_BUFFER = 64
    }
}
