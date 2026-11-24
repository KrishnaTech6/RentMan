@file:OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest

package com.rentman.sharedui.feature.address

import com.rentman.domain.DomainError
import com.rentman.domain.fold
import com.rentman.domain.tenancy.AddAddressUseCase
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.ObserveAddressesUseCase
import com.rentman.sharedui.feature.address.AddressPickerIntent.Add
import com.rentman.sharedui.feature.address.AddressPickerIntent.Confirm
import com.rentman.sharedui.feature.address.AddressPickerIntent.NameChanged
import com.rentman.sharedui.feature.address.AddressPickerIntent.Retry
import com.rentman.sharedui.feature.address.AddressPickerIntent.Select
import com.rentman.sharedui.feature.address.AddressPickerUiState.Content
import com.rentman.sharedui.feature.address.AddressPickerUiState.Error
import com.rentman.sharedui.feature.address.AddressPickerUiState.Loading
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

private typealias State = AddressPickerUiState

internal class AddressPickerViewModel(
    observeAddresses: ObserveAddressesUseCase,
    private val addAddress: AddAddressUseCase,
) : MviViewModel<AddressPickerIntent, State, AddressPickerNavEvent>() {

    private val unchanged: Reducer<State> = { it }

    override val uiState: StateFlow<State> = merge(
        loadReducers(observeAddresses),
        inputReducers(),
        saveReducers(),
        confirmReducers(),
    ).toState(Loading)

    private fun loadReducers(observeAddresses: ObserveAddressesUseCase): Flow<Reducer<State>> =
        intents.filterIsInstance<Retry>().map { }.onStart { emit(Unit) }
            .flatMapLatest {
                observeAddresses()
                    .map { outcome -> outcome.fold(onSuccess = ::withAddresses, onFailure = ::toError) }
                    .onStart { emit { Loading } }
            }

    private fun inputReducers(): Flow<Reducer<State>> = intents.map { intent ->
        when (intent) {
            is NameChanged -> content { it.copy(newName = intent.text, nameError = null) }
            is Select -> content { it.copy(selectedId = intent.id) }
            is Add, is Confirm, Retry -> unchanged
        }
    }

    private fun saveReducers(): Flow<Reducer<State>> = intents.filterIsInstance<Add>().flatMapFirst { add ->
        flow {
            emit(content { it.copy(isSaving = true, nameError = null) })
            emit(
                addAddress(add.name).fold(
                    onSuccess = { address -> content { it.copy(isSaving = false, newName = "", selectedId = address.id) } },
                    onFailure = { error ->
                        if (error is DomainError.Storage) toError(error) else content { it.copy(isSaving = false, nameError = error) }
                    },
                ),
            )
        }
    }

    private fun confirmReducers(): Flow<Reducer<State>> = intents.filterIsInstance<Confirm>()
        .onEach { navigate(AddressPickerNavEvent.Picked(it.id)) }
        .map { unchanged }

    private fun withAddresses(addresses: List<Address>): Reducer<State> = { state ->
        if (state is Content) state.copy(addresses = addresses) else Content(addresses)
    }

    private fun toError(error: DomainError): Reducer<State> = { Error(error) }

    private fun content(change: (Content) -> Content): Reducer<State> = { state ->
        if (state is Content) change(state) else state
    }
}
