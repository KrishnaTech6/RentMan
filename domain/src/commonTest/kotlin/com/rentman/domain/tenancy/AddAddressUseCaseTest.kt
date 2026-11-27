package com.rentman.domain.tenancy

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AddAddressUseCaseTest {
    private val repository = FakeAddressRepository(listOf(Address(AddressId(1), "12 A")))
    private val addAddress = AddAddressUseCase(repository)
    private val observeAddresses = ObserveAddressesUseCase(repository)

    @Test
    fun addsANewAddress() = runTest {
        val result = addAddress("Park View G-7")

        assertEquals(Outcome.Success(Address(AddressId(2), "Park View G-7")), result)
    }

    @Test
    fun trimsAndCollapsesWhitespace() = runTest {
        val result = addAddress("  Lake Tower   B9-19 ")

        assertEquals("Lake Tower B9-19", (result as Outcome.Success).value.name)
    }

    @Test
    fun rejectsABlankName() = runTest {
        assertEquals(Outcome.Failure(DomainError.BlankName), addAddress("   "))
    }

    @Test
    fun rejectsADuplicateIgnoringCase() = runTest {
        assertEquals(Outcome.Failure(DomainError.DuplicateName("12 a")), addAddress("12 a"))
    }

    @Test
    fun observedListUpdatesAfterAnAdd() = runTest {
        observeAddresses().test {
            assertEquals(listOf("12 A"), (awaitItem() as Outcome.Success).value.map(Address::name))

            addAddress("Park View I-3")

            assertEquals(listOf("12 A", "Park View I-3"), (awaitItem() as Outcome.Success).value.map(Address::name))
        }
    }
}
