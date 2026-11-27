@file:OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest

package com.rentman.sharedui.feature.messages

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.fold
import com.rentman.domain.messages.MessageKind
import com.rentman.domain.messages.ObserveMessageUseCase
import com.rentman.domain.messages.SaveMessageUseCase
import com.rentman.sharedui.feature.messages.MessageEditIntent.Back
import com.rentman.sharedui.feature.messages.MessageEditIntent.InsertVariable
import com.rentman.sharedui.feature.messages.MessageEditIntent.Retry
import com.rentman.sharedui.feature.messages.MessageEditIntent.Save
import com.rentman.sharedui.feature.messages.MessageEditIntent.TextChanged
import com.rentman.sharedui.feature.messages.MessageEditIntent.UseOurWording
import com.rentman.sharedui.feature.messages.MessageEditUiState.Content
import com.rentman.sharedui.feature.messages.MessageEditUiState.Error
import com.rentman.sharedui.feature.messages.MessageEditUiState.Loading
import com.rentman.sharedui.mvi.MviViewModel
import com.rentman.sharedui.mvi.Reducer
import com.rentman.sharedui.mvi.flatMapFirst
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

private typealias State = MessageEditUiState

internal class MessageEditViewModel(
    private val kind: MessageKind,
    observeMessage: ObserveMessageUseCase,
    private val saveMessage: SaveMessageUseCase,
) : MviViewModel<MessageEditIntent, State, MessageEditNavEvent>() {

    private val unchanged: Reducer<State> = { it }

    override val uiState: StateFlow<State> = merge(
        loadReducers(observeMessage),
        inputReducers(),
        saveReducers(),
        navigationReducers(),
    ).toState(Loading)

    private fun loadReducers(observeMessage: ObserveMessageUseCase): Flow<Reducer<State>> =
        intents.filterIsInstance<Retry>().map { }.onStart { emit(Unit) }
            .flatMapLatest {
                observeMessage(kind)
                    .map { outcome -> outcome.fold(onSuccess = ::withMessage, onFailure = ::toError) }
                    .onStart { emit { Loading } }
            }

    private fun inputReducers(): Flow<Reducer<State>> = intents.map { intent ->
        when (intent) {
            is TextChanged -> edit { it.copy(text = intent.text, cursor = intent.cursor.coerceIn(0, intent.text.length)) }
            is InsertVariable -> edit {
                val variable = "{${intent.name}}"
                it.copy(
                    text = it.text.substring(0, it.cursor) + variable + it.text.substring(it.cursor),
                    cursor = it.cursor + variable.length,
                    replaced = it.replaced + 1,
                )
            }
            UseOurWording -> edit { it.copy(text = kind.default, cursor = kind.default.length, replaced = it.replaced + 1) }
            is Save, Back, Retry -> unchanged
        }
    }

    private fun saveReducers(): Flow<Reducer<State>> = intents.filterIsInstance<Save>().flatMapFirst { save ->
        flow {
            emit(content { it.copy(isSaving = true, unknownVariables = emptyList(), saveError = null) })
            val outcome = saveMessage(kind, save.text)
            emit(outcome.fold(onSuccess = { content { it.copy(isSaving = false) } }, onFailure = { error -> content { it.copy(isSaving = false).withError(error) } }))
            if (outcome is Outcome.Success) navigate(MessageEditNavEvent.Close)
        }
    }

    private fun navigationReducers(): Flow<Reducer<State>> = intents
        .onEach { if (it == Back) navigate(MessageEditNavEvent.Close) }
        .map { unchanged }

    /** The saved wording arrives again after Save: keep what is typed. */
    private fun withMessage(message: String): Reducer<State> = { state -> state as? Content ?: Content(kind, message) }

    private fun toError(error: DomainError): Reducer<State> = { Error(error) }

    private fun content(change: (Content) -> Content): Reducer<State> = { state -> if (state is Content) change(state) else state }

    /** A change to the text clears what Save said about it. */
    private fun edit(change: (Content) -> Content): Reducer<State> = content { change(it).copy(unknownVariables = emptyList(), saveError = null) }

    private fun Content.withError(error: DomainError): Content =
        if (error is DomainError.InvalidMessage) copy(unknownVariables = error.unknownVariables) else copy(saveError = error)
}
