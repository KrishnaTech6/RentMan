package com.rentman.domain.tenancy

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AddAddressUseCaseTest {
    private val repository = FakeAddressRepository(listOf(Address(AddressId(1), "338 A")))
    private val addAddress = AddAddressUseCase(repository)
    private val observeAddresses = ObserveAddressesUseCase(repository)

    @Test
    fun addsANewAddress() = runTest {
        val result = addAddress("AIMS G-703")

        assertEquals(Outcome.Success(Address(AddressId(2), "AIMS G-703")), result)
    }

    @Test
    fun trimsAndCollapsesWhitespace() = runTest {
        val result = addAddress("  Supertech   B9-1906 ")

        assertEquals("Supertech B9-1906", (result as Outcome.Success).value.name)
    }

    @Test
    fun rejectsABlankName() = runTest {
        assertEquals(Outcome.Failure(DomainError.BlankName), addAddress("   "))
    }

    @Test
    fun rejectsADuplicateIgnoringCase() = runTest {
        assertEquals(Outcome.Failure(DomainError.DuplicateName("338 a")), addAddress("338 a"))
    }

    @Test
    fun observedListUpdatesAfterAnAdd() = runTest {
        observeAddresses().test {
            assertEquals(listOf("338 A"), (awaitItem() as Outcome.Success).value.map(Address::name))

            addAddress("AIMS I-301")

            assertEquals(listOf("338 A", "AIMS I-301"), (awaitItem() as Outcome.Success).value.map(Address::name))
        }
    }
}
