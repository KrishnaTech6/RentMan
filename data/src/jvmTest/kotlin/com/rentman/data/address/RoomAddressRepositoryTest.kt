package com.rentman.data.address

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.room3.Room
import app.cash.turbine.test
import com.rentman.data.db.AppDatabase
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.tenancy.Address
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock

class RoomAddressRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val repository = RoomAddressRepository(database.addressDao(), Clock.System)

    @AfterTest
    fun closeDatabase() = database.close()

    private fun Outcome<List<Address>>.names() = (this as Outcome.Success).value.map(Address::name)

    @Test
    fun addedAddressesComeBackSortedIgnoringCase() = runTest {
        repository.add("b-block")
        repository.add("AIMS G-703")
        repository.add("338 A")

        repository.observeAll().test {
            assertEquals(listOf("338 A", "AIMS G-703", "b-block"), awaitItem().names())
        }
    }

    @Test
    fun observeEmitsAgainWhenAnAddressIsAdded() = runTest {
        repository.observeAll().test {
            assertEquals(emptyList(), awaitItem().names())

            repository.add("338 A")

            assertEquals(listOf("338 A"), awaitItem().names())
        }
    }

    @Test
    fun addReturnsTheStoredAddressWithItsId() = runTest {
        val result = repository.add("338 A")

        val address = (result as Outcome.Success).value
        assertEquals("338 A", address.name)
        assertEquals(1L, address.id.value)
    }

    @Test
    fun aNameThatDiffersOnlyByCaseIsRejectedByTheDatabase() = runTest {
        repository.add("338 A")

        val duplicate = repository.add("338 a")

        val failure = assertIs<Outcome.Failure>(duplicate)
        assertIs<DomainError.Storage>(failure.error)
    }
}
