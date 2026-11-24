package com.rentman.domain.tenancy

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory fake used by use case and ViewModel tests. [failReads] makes observeAll emit a storage failure. */
class FakeAddressRepository(initial: List<Address> = emptyList(), private val failReads: Boolean = false) : AddressRepository {
    private val addresses = MutableStateFlow(initial)

    override fun observeAll(): Flow<Outcome<List<Address>>> = addresses.map {
        if (failReads) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success(it.sortedBy(Address::name))
    }

    override suspend fun add(name: String): Outcome<Address> {
        val address = Address(AddressId(addresses.value.size + 1L), name)
        addresses.value += address
        return Outcome.Success(address)
    }
}
