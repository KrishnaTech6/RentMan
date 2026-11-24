package com.rentman.domain.tenancy

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.rupees
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveRenterSummariesUseCaseTest {
    private val a338 = Address(AddressId(1), "338 A")
    private val aims = Address(AddressId(2), "AIMS G-703")

    private fun renter(name: String, address: Address, unit: String? = null) = Renter(
        id = RenterId(name.length.toLong()),
        addressId = address.id,
        unit = unit,
        name = name,
        phone = "919876543210",
        monthlyRent = 5_000L.rupees,
        dueDay = 5,
        agreementStart = LocalDate(2026, 1, 1),
        raisePercent = 10,
        raiseEveryMonths = 11,
        billsElectricity = false,
        startingReading = null,
        customRate = null,
        firstBillMonth = BillingMonth.of(2026, 1),
    )

    @Test
    fun pairsEachRenterWithTheirAddress() = runTest {
        val ravi = renter("Ravi", a338, unit = "Ground floor")
        val neha = renter("Neha", aims)
        val useCase = ObserveRenterSummariesUseCase(
            FakeRenterRepository(listOf(ravi, neha)),
            FakeAddressRepository(listOf(a338, aims)),
        )

        useCase().test {
            val summaries = (awaitItem() as Outcome.Success).value
            assertEquals(listOf(RenterSummary(neha, aims), RenterSummary(ravi, a338)), summaries)
        }
    }

    @Test
    fun aFailureReadingRentersIsReportedNotShownAsAnEmptyList() = runTest {
        val useCase = ObserveRenterSummariesUseCase(
            FakeRenterRepository(listOf(renter("Ravi", a338)), failReads = true),
            FakeAddressRepository(listOf(a338)),
        )

        useCase().test {
            assertEquals(Outcome.Failure(DomainError.Storage("disk")), awaitItem())
        }
    }

    @Test
    fun aFailureReadingAddressesIsReportedToo() = runTest {
        val useCase = ObserveRenterSummariesUseCase(
            FakeRenterRepository(listOf(renter("Ravi", a338))),
            FakeAddressRepository(listOf(a338), failReads = true),
        )

        useCase().test {
            assertEquals(Outcome.Failure(DomainError.Storage("disk")), awaitItem())
        }
    }

    @Test
    fun noRentersIsAnEmptyListNotAnError() = runTest {
        val useCase = ObserveRenterSummariesUseCase(FakeRenterRepository(), FakeAddressRepository(listOf(a338)))

        useCase().test {
            assertEquals(Outcome.Success(emptyList()), awaitItem())
        }
    }
}
