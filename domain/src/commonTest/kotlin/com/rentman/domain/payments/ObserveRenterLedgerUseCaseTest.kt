package com.rentman.domain.payments

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.TestData
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.FakeBillRepository
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.FakeAddressRepository
import com.rentman.domain.tenancy.FakeRenterRepository
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveRenterLedgerUseCaseTest {
    private fun bill(
        month: BillingMonth,
        total: Paise,
        renterId: Long = 1,
        createdOn: LocalDate = month.atStartOfMonth(),
    ) = Bill(
        renterId = RenterId(renterId),
        month = month,
        createdOn = createdOn,
        dueOn = month.plusMonths(1).dueOn(5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", total)),
    )

    private fun payment(on: LocalDate, amount: Paise, renterId: Long = 1) = Payment(
        renterId = RenterId(renterId),
        amount = amount,
        paidOn = on,
        mode = PaymentMode.Cash,
    ).copy(id = PaymentId(on.day.toLong() * 100 + amount.value / 100))

    private fun useCase(bills: List<Bill> = emptyList(), payments: List<Payment> = emptyList()) = ObserveRenterLedgerUseCase(
        FakeRenterRepository(listOf(TestData.renter())),
        FakeAddressRepository(listOf(TestData.address)),
        FakeBillRepository(bills),
        FakePaymentRepository(payments),
    )

    private fun Outcome<RenterLedger>.ledger() = (this as Outcome.Success).value

    @Test
    fun aPartPaymentLeavesTheRestPendingAndTheNewestEntryComesFirst() = runTest {
        val november = bill(BillingMonth.of(2026, 11), 8_380L.rupees)
        val paid = payment(LocalDate(2026, 11, 22), 5_000L.rupees)

        useCase(listOf(november), listOf(paid))(RenterId(1)).test {
            val ledger = awaitItem().ledger()
            assertEquals(3_380L.rupees, ledger.pending)
            assertEquals(listOf<LedgerEntry>(
                LedgerEntry.PaymentEntry(paid, LocalDate(2026, 11, 22), balanceAfter = 3_380L.rupees),
                LedgerEntry.BillEntry(november, LocalDate(2026, 11, 1), balanceAfter = 8_380L.rupees),
            ), ledger.entries)
        }
    }

    @Test
    fun theRunningBalanceCarriesPartPaymentsIntoTheNextMonth() = runTest {
        // The plan's example: November 5,880 with 4,000 paid leaves 1,880, then December adds 5,920 -> 7,800.
        val bills = listOf(bill(BillingMonth.of(2026, 11), 5_880L.rupees), bill(BillingMonth.of(2026, 12), 5_920L.rupees))
        val payments = listOf(payment(LocalDate(2026, 11, 20), 4_000L.rupees))

        useCase(bills, payments)(RenterId(1)).test {
            val ledger = awaitItem().ledger()
            assertEquals(listOf(7_800L.rupees, 1_880L.rupees, 5_880L.rupees), ledger.entries.map { it.balanceAfter })
            assertEquals(7_800L.rupees, ledger.pending)
        }
    }

    @Test
    fun onTheSameDayTheBillComesBeforeThePayment() = runTest {
        val november = bill(BillingMonth.of(2026, 11), 5_000L.rupees)
        val paid = payment(LocalDate(2026, 11, 1), 5_000L.rupees)

        useCase(listOf(november), listOf(paid))(RenterId(1)).test {
            val ledger = awaitItem().ledger()
            assertEquals(listOf(Paise.Zero, 5_000L.rupees), ledger.entries.map { it.balanceAfter }) // newest first: payment, then bill
            assertEquals(Paise.Zero, ledger.pending)
        }
    }

    @Test
    fun aPaymentForLastMonthsBillComesBeforeThisMonthsBill() = runTest {
        // October's bill came on 1 Nov and was paid on 3 Nov. November's bill came on 4 Nov.
        val bills = listOf(
            bill(BillingMonth.of(2026, 10), 5_000L.rupees, createdOn = LocalDate(2026, 11, 1)),
            bill(BillingMonth.of(2026, 11), 5_000L.rupees, createdOn = LocalDate(2026, 11, 4)),
        )

        useCase(bills, listOf(payment(LocalDate(2026, 11, 3), 5_000L.rupees)))(RenterId(1)).test {
            // Newest first: the November bill, the payment, the October bill.
            assertEquals(listOf(5_000L.rupees, Paise.Zero, 5_000L.rupees), awaitItem().ledger().entries.map { it.balanceAfter })
        }
    }

    @Test
    fun aBillMadeLongAfterItsDueDateIsPlacedOnItsDueDate() = runTest {
        // September's bill was only entered on 23 Nov. It was due on 5 Oct, so a payment on 20 Oct comes after it.
        val bills = listOf(bill(BillingMonth.of(2026, 9), 5_000L.rupees, createdOn = LocalDate(2026, 11, 23)))

        useCase(bills, listOf(payment(LocalDate(2026, 10, 20), 2_000L.rupees)))(RenterId(1)).test {
            val entries = awaitItem().ledger().entries
            assertEquals(listOf(3_000L.rupees, 5_000L.rupees), entries.map { it.balanceAfter })
            assertEquals(LocalDate(2026, 10, 5), entries.last().date)
        }
    }

    @Test
    fun payingMoreThanOwedLeavesACreditNotZero() = runTest {
        useCase(listOf(bill(BillingMonth.of(2026, 11), 1_000L.rupees)), listOf(payment(LocalDate(2026, 11, 5), 1_100L.rupees)))(RenterId(1)).test {
            assertEquals(Paise(-10_000), awaitItem().ledger().pending)
        }
    }

    @Test
    fun otherRentersBillsAndPaymentsAreLeftOut() = runTest {
        val bills = listOf(bill(BillingMonth.of(2026, 11), 5_000L.rupees), bill(BillingMonth.of(2026, 11), 9_999L.rupees, renterId = 2))
        val payments = listOf(payment(LocalDate(2026, 11, 3), 7_000L.rupees, renterId = 2))

        useCase(bills, payments)(RenterId(1)).test {
            val ledger = awaitItem().ledger()
            assertEquals(1, ledger.entries.size)
            assertEquals(5_000L.rupees, ledger.pending)
        }
    }

    @Test
    fun aRenterWithNothingYetHasAnEmptyLedger() = runTest {
        useCase()(RenterId(1)).test {
            val ledger = awaitItem().ledger()
            assertEquals(emptyList(), ledger.entries)
            assertEquals(Paise.Zero, ledger.pending)
        }
    }

    @Test
    fun theLedgerSaysWhetherRemindersAreStoppedForTheLatestBill() = runTest {
        val bills = FakeBillRepository(listOf(bill(BillingMonth.of(2026, 11), 5_000L.rupees)))
        fun ledgerOf(renter: Renter) = ObserveRenterLedgerUseCase(
            FakeRenterRepository(listOf(renter)), FakeAddressRepository(listOf(TestData.address)), bills, FakePaymentRepository(),
        )(RenterId(1))

        ledgerOf(TestData.renter(remindersMutedMonth = BillingMonth.of(2026, 11))).test {
            assertEquals(true, awaitItem().ledger().remindersMuted)
        }
        ledgerOf(TestData.renter()).test {
            assertEquals(false, awaitItem().ledger().remindersMuted)
        }
        ledgerOf(TestData.renter(remindersMutedMonth = BillingMonth.of(2026, 10))).test {
            assertEquals(false, awaitItem().ledger().remindersMuted) // an older mute ended with the newer bill
        }
    }

    @Test
    fun anUnknownRenterIsNotFoundAndAFailureIsReported() = runTest {
        useCase()(RenterId(99)).test {
            assertEquals(Outcome.Failure(DomainError.NotFound), awaitItem())
        }
        ObserveRenterLedgerUseCase(
            FakeRenterRepository(listOf(TestData.renter())),
            FakeAddressRepository(listOf(TestData.address)),
            FakeBillRepository(),
            FakePaymentRepository(failReads = true),
        )(RenterId(1)).test {
            assertEquals(Outcome.Failure(DomainError.Storage("disk")), awaitItem())
        }
    }
}
