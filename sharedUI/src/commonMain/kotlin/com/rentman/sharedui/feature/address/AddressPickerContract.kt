package com.rentman.sharedui.feature.address

import com.rentman.domain.DomainError
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId

internal sealed interface AddressPickerIntent {
    data class NameChanged(val text: String) : AddressPickerIntent
    data class Select(val id: AddressId) : AddressPickerIntent
    data class Add(val name: String) : AddressPickerIntent
    data class Confirm(val id: AddressId) : AddressPickerIntent
    data object Retry : AddressPickerIntent
}

internal sealed interface AddressPickerUiState {
    data object Loading : AddressPickerUiState

    data class Error(val error: DomainError) : AddressPickerUiState

    data class Content(
        val addresses: List<Address>,
        val selectedId: AddressId? = null,
        val newName: String = "",
        val nameError: DomainError? = null,
        val isSaving: Boolean = false,
    ) : AddressPickerUiState {
        val selected: Address? get() = addresses.firstOrNull { it.id == selectedId }
    }
}

internal sealed interface AddressPickerNavEvent {
    data class Picked(val id: AddressId) : AddressPickerNavEvent
}
