package com.rentman.sharedui.feature.address

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.AddressRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Test fake. [failReads] makes observeAll emit a storage failure, [failAdd] makes `add` fail,
 * and [gate] holds `add` until completed.
 */
internal class FakeAddressRepository(
    initial: List<Address> = emptyList(),
    var failReads: Boolean = false,
    var failAdd: Boolean = false,
    var gate: CompletableDeferred<Unit>? = null,
) : AddressRepository {
    private val addresses = MutableStateFlow(initial)
    var addCalls = 0
        private set

    override fun observeAll(): Flow<Outcome<List<Address>>> = addresses.map {
        if (failReads) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success(it.sortedBy(Address::name))
    }

    override suspend fun add(name: String): Outcome<Address> {
        addCalls++
        gate?.await()
        if (failAdd) return Outcome.Failure(DomainError.Storage("disk"))
        val address = Address(AddressId(addresses.value.size + 1L), name)
        addresses.value += address
        return Outcome.Success(address)
    }
}
