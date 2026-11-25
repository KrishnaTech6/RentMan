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
    private var now = Instant.parse("2026-11-23T10:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = now
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

    @Test
    fun nextMonthsBillOpensThreeDaysBeforeItsDueDay() = runTest {
        // Due on the 1st: December's bill is made at the end of November. 3 days before 1 Dec is 28 Nov.
        val renter = TestData.renter(dueDay = 1)
        val bills = listOf(billFor(1, BillingMonth.of(2026, 11)))

        now = Instant.parse("2026-11-27T10:00:00Z")
        useCase(listOf(renter), bills)().test { assertEquals(emptyList(), awaitItem().todos()) }

        now = Instant.parse("2026-11-28T10:00:00Z")
        useCase(listOf(renter), bills)().test { assertEquals(listOf("Ravi Kumar" to BillingMonth.of(2026, 12)), awaitItem().todos()) }
    }

    @Test
    fun aLaterDueDayDoesNotOpenNextMonthEarly() = runTest {
        // Due on the 5th: December's bill opens on 2 Dec, and until then November's was the last one to make.
        now = Instant.parse("2026-11-30T10:00:00Z")

        useCase(listOf(TestData.renter(dueDay = 5)), listOf(billFor(1, BillingMonth.of(2026, 11))))().test {
            assertEquals(emptyList(), awaitItem().todos())
        }
    }

    @Test
    fun anUnbilledCurrentMonthStillComesBeforeNextMonth() = runTest {
        now = Instant.parse("2026-11-29T10:00:00Z")

        useCase(listOf(TestData.renter(dueDay = 1, firstBillMonth = BillingMonth.of(2026, 11))))().test {
            assertEquals(listOf("Ravi Kumar" to BillingMonth.of(2026, 11)), awaitItem().todos())
        }
    }
}
