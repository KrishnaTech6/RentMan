package com.rentman.domain.tenancy

import com.rentman.domain.Outcome
import kotlinx.coroutines.flow.Flow

interface AddressRepository {
    /** Active addresses, sorted by name. Emits again whenever they change. */
    fun observeAll(): Flow<Outcome<List<Address>>>

    suspend fun add(name: String): Outcome<Address>
}
