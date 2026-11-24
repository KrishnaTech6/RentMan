package com.rentman.domain.tenancy

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.fold
import kotlinx.coroutines.flow.first

class ObserveAddressesUseCase(private val repository: AddressRepository) {
    operator fun invoke() = repository.observeAll()
}

class AddAddressUseCase(private val repository: AddressRepository) {
    suspend operator fun invoke(rawName: String): Outcome<Address> {
        val name = rawName.trim().replace(WHITESPACE, " ")
        if (name.isEmpty()) return Outcome.Failure(DomainError.BlankName)
        return repository.observeAll().first().fold(
            onSuccess = { existing ->
                if (existing.any { it.name.equals(name, ignoreCase = true) }) {
                    Outcome.Failure(DomainError.DuplicateName(name))
                } else {
                    repository.add(name)
                }
            },
            onFailure = { Outcome.Failure(it) },
        )
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
