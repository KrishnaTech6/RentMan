package com.rentman.domain.billing

import app.cash.turbine.test
import com.rentman.domain.Outcome
import com.rentman.domain.TestData
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.FakeAddressRepository
import com.rentman.domain.tenancy.FakeRenterRepository
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class ObserveBillTodosUseCaseTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-23T10:00:00Z")
    }

    private fun useCase(renters: List<com.rentman.domain.tenancy.Renter>, bills: List<Bill> = emptyList()) =
        ObserveBillTodosUseCase(
            FakeRenterRepository(renters),
            FakeAddressRepository(listOf(TestData.address)),
            FakeBillRepository(bills),
            GetTodayUseCase(clock, TimeZone.UTC),
        )

    private fun billFor(renterId: Long, month: BillingMonth) = Bill(
        renterId = RenterId(renterId),
        month = month,
        dueOn = LocalDate(month.year, month.month, 5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", 5_000L.rupees)),
    )

    private fun Outcome<List<BillTodo>>.todos() = (this as Outcome.Success).value.map { it.summary.renter.name to it.month }

    @Test
    fun aRenterWithNoBillYetNeedsTheCurrentMonthsBill() = runTest {
        useCase(listOf(TestData.renter(firstBillMonth = BillingMonth.of(2026, 11)))).invoke().test {
            assertEquals(listOf("Ravi Kumar" to BillingMonth.of(2026, 11)), awaitItem().todos())
        }
    }

    @Test
    fun onceTheBillExistsThereIsNothingToDo() = runTest {
        val bills = listOf(billFor(1, BillingMonth.of(2026, 11)))

        useCase(listOf(TestData.renter()), bills)().test {
            assertEquals(emptyList(), awaitItem().todos())
        }
    }

    @Test
    fun skippedMonthsComeOneAtATimeOldestFirst() = runTest {
        val renter = TestData.renter(firstBillMonth = BillingMonth.of(2026, 9))

        useCase(listOf(renter), listOf(billFor(1, BillingMonth.of(2026, 9))))().test {
            assertEquals(listOf("Ravi Kumar" to BillingMonth.of(2026, 10)), awaitItem().todos())
        }
    }

    @Test
    fun aMonthBeforeTheFirstBillMonthIsNeverAsked() = runTest {
        // Started billing in December, which hasn't come yet.
        val renter = TestData.renter(firstBillMonth = BillingMonth.of(2026, 12))

        useCase(listOf(renter))().test {
            assertEquals(emptyList(), awaitItem().todos())
        }
    }

    @Test
    fun otherRentersBillsDoNotCount() = runTest {
        val bills = listOf(billFor(2, BillingMonth.of(2026, 11)))

        useCase(listOf(TestData.renter(id = 1)), bills)().test {
            assertEquals(listOf("Ravi Kumar" to BillingMonth.of(2026, 11)), awaitItem().todos())
        }
    }

    @Test
    fun theListIsOrderedByMonthThenName() = runTest {
        val renters = listOf(
            TestData.renter(id = 1, name = "Zoya", firstBillMonth = BillingMonth.of(2026, 11)),
            TestData.renter(id = 2, name = "Amit", firstBillMonth = BillingMonth.of(2026, 11)),
            TestData.renter(id = 3, name = "Neha", firstBillMonth = BillingMonth.of(2026, 10)),
        )

        useCase(renters)().test {
            assertEquals(
                listOf("Neha" to BillingMonth.of(2026, 10), "Amit" to BillingMonth.of(2026, 11), "Zoya" to BillingMonth.of(2026, 11)),
                awaitItem().todos(),
            )
        }
    }
}
