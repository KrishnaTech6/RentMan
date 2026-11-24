package com.rentman.data.address

import com.rentman.data.db.AddressDao
import com.rentman.data.db.AddressEntity
import com.rentman.data.safeCall
import com.rentman.data.safeFlow
import com.rentman.domain.Outcome
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.AddressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

internal class RoomAddressRepository(
    private val dao: AddressDao,
    private val clock: Clock,
) : AddressRepository {

    override fun observeAll(): Flow<Outcome<List<Address>>> =
        safeFlow { dao.observeActive().map { entities -> entities.map(AddressEntity::toDomain) } }

    override suspend fun add(name: String): Outcome<Address> = safeCall {
        val id = dao.insert(AddressEntity(name = name, createdAt = clock.now().toEpochMilliseconds()))
        Address(AddressId(id), name)
    }
}

internal fun AddressEntity.toDomain() = Address(AddressId(id), name)
