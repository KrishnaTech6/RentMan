package com.rentman.domain.billing

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.TestData
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.FakePaymentRepository
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.settings.AppSettings
import com.rentman.domain.tenancy.FakeAddressRepository
import com.rentman.domain.tenancy.FakeRenterRepository
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterSummary
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveBillContextUseCaseTest {
    private val november = BillingMonth.of(2026, 11)

    private fun useCase(
        renters: List<com.rentman.domain.tenancy.Renter> = listOf(TestData.renter()),
        bills: List<Bill> = emptyList(),
        settings: AppSettings = AppSettings(),
        payments: List<Payment> = emptyList(),
    ) = ObserveBillContextUseCase(
        FakeRenterRepository(renters),
        FakeAddressRepository(listOf(TestData.address)),
        FakeBillRepository(bills),
        FakeSettingsRepository(settings),
        FakePaymentRepository(payments),
    )

    private fun pastBill(month: BillingMonth, total: Paise, reading: Int?, renterId: Long = 1) = Bill(
        renterId = RenterId(renterId),
        month = month,
        dueOn = LocalDate(month.year, month.month, 5),
        previousReading = reading?.minus(50),
        currentReading = reading,
        rate = Paise(800),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", total)),
    )

    @Test
    fun aFirstBillStartsFromTheStartingReadingAtTheGlobalRate() = runTest {
        useCase()(RenterId(1), november).test {
            val context = (awaitItem() as Outcome.Success).value
            assertEquals(RenterSummary(TestData.renter(), TestData.address), context.summary)
            assertEquals(1240, context.previousReading)
            assertEquals(Paise(800), context.rate)
            assertEquals(Paise.Zero, context.pendingBefore)
        }
    }

    @Test
    fun theLastBillsReadingIsTheNextStartingPoint() = runTest {
        val bills = listOf(
            pastBill(BillingMonth.of(2026, 9), 5_000L.rupees, reading = 1290),
            pastBill(BillingMonth.of(2026, 10), 5_000L.rupees, reading = 1340),
        )

        useCase(bills = bills)(RenterId(1), november).test {
            assertEquals(1340, (awaitItem() as Outcome.Success).value.previousReading)
        }
    }

    @Test
    fun afterMonthsOfPayingDirectlyTheNewStartingReadingIsUsedNotTheOldMeterReading() = runTest {
        // Billed for electricity until October (reading 1325), then they paid it directly: Nov to Mar bills have no reading.
        // In April the landlord bills it again and enters the meter as it is now: 1500.
        val bills = listOf(pastBill(BillingMonth.of(2026, 10), 5_000L.rupees, reading = 1325)) +
            listOf(BillingMonth.of(2026, 11), BillingMonth.of(2026, 12), BillingMonth.of(2027, 1), BillingMonth.of(2027, 2), BillingMonth.of(2027, 3))
                .map { pastBill(it, 5_000L.rupees, reading = null) }
        val renter = TestData.renter(startingReading = 1500)

        useCase(renters = listOf(renter), bills = bills)(RenterId(1), BillingMonth.of(2027, 4)).test {
            assertEquals(1500, (awaitItem() as Outcome.Success).value.previousReading)
        }
    }

    @Test
    fun aMonthWithNoBillInBetweenStillContinuesFromTheLastBillsReading() = runTest {
        val bills = listOf(pastBill(BillingMonth.of(2026, 9), 5_000L.rupees, reading = 1290)) // no October bill

        useCase(bills = bills)(RenterId(1), november).test {
            assertEquals(1290, (awaitItem() as Outcome.Success).value.previousReading)
        }
    }

    @Test
    fun theUsualUsageIsTheAverageOfTheLastThreeBillsUnits() = runTest {
        fun withUnits(month: BillingMonth, units: Int) = pastBill(month, 5_000L.rupees, reading = 1000 + units).copy(
            previousReading = 1000,
            currentReading = 1000 + units,
        )
        val bills = listOf(
            withUnits(BillingMonth.of(2026, 6), units = 500), // older than the last three: ignored
            withUnits(BillingMonth.of(2026, 8), units = 60),
            withUnits(BillingMonth.of(2026, 9), units = 90),
            withUnits(BillingMonth.of(2026, 10), units = 120),
        )

        useCase(bills = bills)(RenterId(1), november).test {
            assertEquals(90, (awaitItem() as Outcome.Success).value.usualUnits)
        }
    }

    @Test
    fun noEarlierBillsMeansNoUsualUsageToCompareWith() = runTest {
        useCase()(RenterId(1), november).test {
            assertEquals(null, (awaitItem() as Outcome.Success).value.usualUnits)
        }
    }

    @Test
    fun anOwnRateBeatsTheGlobalRateAndTheGlobalRateIsReadFromSettings() = runTest {
        useCase(renters = listOf(TestData.renter(customRate = Paise(950))))(RenterId(1), november).test {
            assertEquals(Paise(950), (awaitItem() as Outcome.Success).value.rate)
        }
        useCase(settings = AppSettings(globalRate = Paise(900)))(RenterId(1), november).test {
            assertEquals(Paise(900), (awaitItem() as Outcome.Success).value.rate)
        }
    }

    @Test
    fun whatIsStillOwedIsEveryEarlierBillOfThatRenterOnly() = runTest {
        val bills = listOf(
            pastBill(BillingMonth.of(2026, 9), 5_000L.rupees, reading = null),
            pastBill(BillingMonth.of(2026, 10), 880L.rupees, reading = null),
            pastBill(BillingMonth.of(2026, 10), 9_999L.rupees, reading = null, renterId = 2), // someone else
        )

        useCase(bills = bills)(RenterId(1), november).test {
            assertEquals(5_880L.rupees, (awaitItem() as Outcome.Success).value.pendingBefore)
        }
    }

    @Test
    fun paymentsAreSubtractedFromWhatIsStillOwed() = runTest {
        // The plan's example: bills of 5,880 with 4,000 paid leaves 1,880 pending on the next bill.
        val bills = listOf(pastBill(BillingMonth.of(2026, 10), 5_880L.rupees, reading = null))
        val payments = listOf(
            Payment(PaymentId(1), RenterId(1), 4_000L.rupees, LocalDate(2026, 11, 3), PaymentMode.Upi),
            Payment(PaymentId(2), RenterId(2), 9_999L.rupees, LocalDate(2026, 11, 4), PaymentMode.Cash), // someone else
        )

        useCase(bills = bills, payments = payments)(RenterId(1), november).test {
            assertEquals(1_880L.rupees, (awaitItem() as Outcome.Success).value.pendingBefore)
        }
    }

    @Test
    fun aRenterWhoPaysDirectlyHasNoPreviousReading() = runTest {
        useCase(renters = listOf(TestData.renter(billsElectricity = false)))(RenterId(1), november).test {
            assertEquals(null, (awaitItem() as Outcome.Success).value.previousReading)
        }
    }

    @Test
    fun anUnknownRenterIsNotFound() = runTest {
        useCase()(RenterId(99), november).test {
            assertEquals(Outcome.Failure(DomainError.NotFound), awaitItem())
        }
    }
}
