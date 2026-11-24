package com.rentman.sharedui.feature.renter

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.LedgerEntry
import com.rentman.domain.payments.ObserveRenterLedgerUseCase
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.billing.RemoveBillUseCase
import com.rentman.domain.billing.RestoreBillUseCase
import com.rentman.domain.payments.RemovePaymentUseCase
import com.rentman.domain.payments.RestorePaymentUseCase
import com.rentman.domain.tenancy.MuteRemindersUseCase
import com.rentman.domain.tenancy.RenterId
import com.rentman.sharedui.feature.address.FakeAddressRepository
import com.rentman.sharedui.feature.bill.FakeBillRepository
import com.rentman.sharedui.feature.bill.Ravi
import com.rentman.sharedui.feature.payment.FakePaymentRepository
import com.rentman.sharedui.feature.renter.RenterDetailIntent.Back
import com.rentman.sharedui.feature.renter.RenterDetailIntent.Edit
import com.rentman.sharedui.feature.renter.RenterDetailIntent.MarkLeft
import com.rentman.sharedui.feature.renter.RenterDetailIntent.MoveBack
import com.rentman.sharedui.feature.renter.RenterDetailIntent.RecordPayment
import com.rentman.sharedui.feature.renter.RenterDetailIntent.RemoveBill
import com.rentman.sharedui.feature.renter.RenterDetailIntent.RemovePayment
import com.rentman.sharedui.feature.renter.RenterDetailIntent.UndoRemoveBill
import com.rentman.sharedui.feature.renter.RenterDetailIntent.Retry
import com.rentman.sharedui.feature.renter.RenterDetailIntent.SetRemindersMuted
import com.rentman.sharedui.feature.renter.RenterDetailIntent.UndoRemove
import com.rentman.sharedui.feature.renter.RenterDetailUiState.Content
import com.rentman.sharedui.feature.renter.RenterDetailUiState.Error
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import com.rentman.domain.tenancy.MarkRenterLeftUseCase
import com.rentman.domain.tenancy.MoveRenterBackUseCase
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class RenterDetailViewModelTest {
    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private val novemberBill = Bill(
        id = com.rentman.domain.billing.BillId(1),
        renterId = RenterId(1),
        month = BillingMonth.of(2026, 11),
        createdOn = LocalDate(2026, 11, 3),
        dueOn = LocalDate(2026, 12, 5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", 8_380L.rupees)),
    )
    private val firstHalf = Payment(PaymentId(1), RenterId(1), 5_000L.rupees, LocalDate(2026, 11, 20), PaymentMode.Upi)
    private val secondHalf = Payment(PaymentId(2), RenterId(1), 1_000L.rupees, LocalDate(2026, 11, 22), PaymentMode.Cash, "later")

    private fun viewModel(
        payments: FakePaymentRepository = FakePaymentRepository(listOf(firstHalf, secondHalf)),
        renterId: Long = 1,
        renters: FakeRenterRepository = FakeRenterRepository(listOf(Ravi.renter(billsElectricity = false))),
        bills: FakeBillRepository = FakeBillRepository(listOf(novemberBill)),
    ) = RenterDetailViewModel(
        renterId = RenterId(renterId),
        observeLedger = ObserveRenterLedgerUseCase(
            renters,
            FakeAddressRepository(listOf(Ravi.address)),
            bills,
            payments,
        ),
        muteReminders = MuteRemindersUseCase(renters, bills),
        markLeft = MarkRenterLeftUseCase(renters, bills, payments, GetTodayUseCase(clock, TimeZone.UTC)),
        moveBack = MoveRenterBackUseCase(renters),
        removePayment = RemovePaymentUseCase(payments),
        restorePayment = RestorePaymentUseCase(payments),
        removeBill = RemoveBillUseCase(bills, renters),
        restoreBill = RestoreBillUseCase(bills),
    )

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-30T10:00:00Z") // November is the month of the only bill
    }

    private fun RenterDetailViewModel.content() = uiState.value as Content

    @Test
    fun showsWhatIsPendingAndTheHistoryNewestFirst() = runTest {
        val content = viewModel().content()

        assertEquals(2_380L.rupees, content.ledger.pending)
        assertEquals(listOf(secondHalf, firstHalf), content.ledger.entries.filterIsInstance<LedgerEntry.PaymentEntry>().map { it.payment })
        assertEquals(3, content.ledger.entries.size)
    }

    @Test
    fun removingAPaymentTakesItOutOfTheBalanceAndOffersUndo() = runTest {
        val vm = viewModel()

        vm.onIntent(RemovePayment(secondHalf))

        val content = vm.content()
        assertEquals(3_380L.rupees, content.ledger.pending)
        assertEquals(secondHalf, content.removed)
    }

    @Test
    fun undoPutsThePaymentBackAndClosesTheOffer() = runTest {
        val vm = viewModel()
        vm.onIntent(RemovePayment(secondHalf))

        vm.onIntent(UndoRemove(secondHalf))

        val content = vm.content()
        assertEquals(2_380L.rupees, content.ledger.pending)
        assertEquals(null, content.removed)
    }

    @Test
    fun theUndoOfferEndsAfterAFewSeconds() = runTest {
        val vm = viewModel()
        vm.onIntent(RemovePayment(secondHalf))

        advanceTimeBy(4_900)
        assertEquals(secondHalf, vm.content().removed)

        advanceTimeBy(200)
        assertEquals(null, vm.content().removed)
        assertEquals(3_380L.rupees, vm.content().ledger.pending) // the removal itself stays
    }

    @Test
    fun aSecondRemovalRestartsTheWaitAndOffersUndoForTheNewerOne() = runTest {
        val vm = viewModel()
        vm.onIntent(RemovePayment(secondHalf))
        advanceTimeBy(3_000)

        vm.onIntent(RemovePayment(firstHalf))
        advanceTimeBy(3_000) // 6s after the first removal, but only 3s after the second

        assertEquals(firstHalf, vm.content().removed)
        advanceTimeBy(2_100)
        assertEquals(null, vm.content().removed)
    }

    @Test
    fun aFailedRemovalShowsAnErrorAndKeepsThePayment() = runTest {
        val payments = FakePaymentRepository(listOf(firstHalf, secondHalf), failVoid = true)
        val vm = viewModel(payments)

        vm.onIntent(RemovePayment(secondHalf))

        val content = vm.content()
        assertEquals(DomainError.Storage("disk"), content.actionError)
        assertEquals(null, content.removed)
        assertEquals(2_380L.rupees, content.ledger.pending)
    }

    @Test
    fun stoppingRemindersMutesUntilTheNextBillAndResumingClearsIt() = runTest {
        val renters = FakeRenterRepository(listOf(Ravi.renter(billsElectricity = false)))
        val vm = viewModel(renters = renters)
        assertEquals(false, vm.content().ledger.remindersMuted)

        vm.onIntent(SetRemindersMuted(true))
        assertEquals(true, vm.content().ledger.remindersMuted)
        assertEquals(BillingMonth.of(2026, 11), renters.saved.single().remindersMutedMonth)

        vm.onIntent(SetRemindersMuted(false))
        assertEquals(false, vm.content().ledger.remindersMuted)
    }

    @Test
    fun aFailedStopShowsAnErrorAndLeavesRemindersOn() = runTest {
        val renters = FakeRenterRepository(listOf(Ravi.renter(billsElectricity = false)), failWrites = true)
        val vm = viewModel(renters = renters)

        vm.onIntent(SetRemindersMuted(true))

        assertEquals(DomainError.Storage("disk"), vm.content().actionError)
        assertEquals(false, vm.content().ledger.remindersMuted)
    }

    @Test
    fun aRenterWhoStillOwesCannotBeMarkedAsLeft() = runTest {
        val renters = FakeRenterRepository(listOf(Ravi.renter(billsElectricity = false)))
        val vm = viewModel(renters = renters) // 2,380 is pending

        vm.onIntent(MarkLeft)

        assertEquals(DomainError.StillOwes(2_380L.rupees), vm.content().actionError)
        assertEquals(true, vm.content().ledger.summary.renter.active)
    }

    @Test
    fun onceTheyOweNothingTheRenterCanBeMarkedAsLeftAndMovedBack() = runTest {
        val renters = FakeRenterRepository(listOf(Ravi.renter(billsElectricity = false)))
        val payments = FakePaymentRepository(listOf(firstHalf, secondHalf, Payment(PaymentId(3), RenterId(1), 2_380L.rupees, LocalDate(2026, 11, 29), PaymentMode.Cash)))
        val vm = viewModel(payments = payments, renters = renters)

        vm.onIntent(MarkLeft)

        vm.content().ledger.summary.renter.let {
            assertEquals(false, it.active)
            assertEquals(LocalDate(2026, 11, 30), it.leftOn)
        }
        assertEquals(null, vm.content().actionError)

        vm.onIntent(MoveBack)

        assertEquals(true, vm.content().ledger.summary.renter.active)
        assertEquals(null, vm.content().ledger.summary.renter.leftOn)
    }

    @Test
    fun movingBackIntoAUnitSomeoneElseHasShowsAnError() = runTest {
        val left = Ravi.renter(billsElectricity = false).copy(active = false, leftOn = LocalDate(2026, 12, 1))
        val newcomer = Ravi.renter(billsElectricity = false).copy(id = RenterId(2), name = "Neha")
        val vm = viewModel(renters = FakeRenterRepository(listOf(left, newcomer)))

        vm.onIntent(MoveBack)

        assertEquals(DomainError.UnitTaken, vm.content().actionError)
        assertEquals(false, vm.content().ledger.summary.renter.active)
    }

    @Test
    fun aRenterWithAMonthStillToBillCannotLeaveAndTheScreenSaysWhich() = runTest {
        val december = object : Clock {
            override fun now(): Instant = Instant.parse("2026-12-10T10:00:00Z")
        }
        val renters = FakeRenterRepository(listOf(Ravi.renter(billsElectricity = false)))
        val payments = FakePaymentRepository(listOf(firstHalf, secondHalf, Payment(PaymentId(3), RenterId(1), 2_380L.rupees, LocalDate(2026, 11, 29), PaymentMode.Cash)))
        val vm = RenterDetailViewModel(
            renterId = RenterId(1),
            observeLedger = ObserveRenterLedgerUseCase(renters, FakeAddressRepository(listOf(Ravi.address)), FakeBillRepository(listOf(novemberBill)), payments),
            muteReminders = MuteRemindersUseCase(renters, FakeBillRepository(listOf(novemberBill))),
            markLeft = MarkRenterLeftUseCase(renters, FakeBillRepository(listOf(novemberBill)), payments, GetTodayUseCase(december, TimeZone.UTC)),
            moveBack = MoveRenterBackUseCase(renters),
            removePayment = RemovePaymentUseCase(payments),
            restorePayment = RestorePaymentUseCase(payments),
            removeBill = RemoveBillUseCase(FakeBillRepository(listOf(novemberBill)), renters),
            restoreBill = RestoreBillUseCase(FakeBillRepository(listOf(novemberBill))),
        )

        vm.onIntent(MarkLeft)

        assertEquals(DomainError.UnbilledMonth(BillingMonth.of(2026, 12)), vm.content().actionError)
        assertEquals(true, vm.content().ledger.summary.renter.active)
    }

    @Test
    fun aFailedLeaveShowsAnError() = runTest {
        val renters = FakeRenterRepository(listOf(Ravi.renter(billsElectricity = false)), failWrites = true)
        val payments = FakePaymentRepository(listOf(firstHalf, secondHalf, Payment(PaymentId(3), RenterId(1), 2_380L.rupees, LocalDate(2026, 11, 29), PaymentMode.Cash)))
        val vm = viewModel(payments = payments, renters = renters)

        vm.onIntent(MarkLeft)

        assertEquals(DomainError.Storage("disk"), vm.content().actionError)
    }

    @Test
    fun editAsksTheScreenToOpenTheEditForm() = runTest {
        val vm = viewModel()

        vm.navigation.test {
            vm.onIntent(Edit)

            assertEquals(RenterDetailNavEvent.OpenEdit, awaitItem())
        }
    }

    @Test
    fun recordPaymentAndBackAskTheScreenToNavigate() = runTest {
        val vm = viewModel()

        vm.navigation.test {
            vm.onIntent(RecordPayment)
            assertEquals(RenterDetailNavEvent.OpenRecordPayment, awaitItem())

            vm.onIntent(Back)
            assertEquals(RenterDetailNavEvent.Close, awaitItem())
        }
    }

    @Test
    fun anUnknownRenterIsAnErrorAndRetryRecoversWhenTheDiskDoes() = runTest {
        assertEquals(Error(DomainError.NotFound), viewModel(renterId = 99).uiState.value)

        val payments = FakePaymentRepository(failReads = true)
        val vm = viewModel(payments)
        assertIs<Error>(vm.uiState.value)

        payments.failReads = false
        vm.onIntent(Retry)

        assertIs<Content>(vm.uiState.value)
    }

    @Test
    fun aRenterWithNothingYetHasAnEmptyHistory() = runTest {
        val payments = FakePaymentRepository()
        val vm = RenterDetailViewModel(
            RenterId(1),
            ObserveRenterLedgerUseCase(
                FakeRenterRepository(listOf(Ravi.renter(billsElectricity = false))),
                FakeAddressRepository(listOf(Ravi.address)),
                FakeBillRepository(),
                payments,
            ),
            MuteRemindersUseCase(FakeRenterRepository(), FakeBillRepository()),
            MarkRenterLeftUseCase(FakeRenterRepository(), FakeBillRepository(), payments, GetTodayUseCase(clock, TimeZone.UTC)),
            MoveRenterBackUseCase(FakeRenterRepository()),
            RemovePaymentUseCase(payments),
            RestorePaymentUseCase(payments),
            RemoveBillUseCase(FakeBillRepository(), FakeRenterRepository()),
            RestoreBillUseCase(FakeBillRepository()),
        )

        assertEquals(emptyList(), vm.content().ledger.entries)
        assertEquals(Paise.Zero, vm.content().ledger.pending)
    }

    @Test
    fun onlyTheNewestBillIsOfferedForRemoval() = runTest {
        assertEquals(novemberBill.id, viewModel().content().ledger.removableBill)
    }

    @Test
    fun removingABillTakesItOutOfTheHistoryLeavesThePaymentsAndOffersUndo() = runTest {
        val vm = viewModel()

        vm.onIntent(RemoveBill(novemberBill))

        val content = vm.content()
        assertEquals(emptyList(), content.ledger.entries.filterIsInstance<LedgerEntry.BillEntry>())
        assertEquals(2, content.ledger.entries.filterIsInstance<LedgerEntry.PaymentEntry>().size)
        assertEquals((-6_000L).rupees, content.ledger.pending) // 6,000 was paid and nothing is billed: paid ahead
        assertEquals(null, content.ledger.removableBill)
        assertEquals(novemberBill, content.removedBill)
    }

    @Test
    fun undoPutsTheBillBackAndClosesTheOffer() = runTest {
        val vm = viewModel()
        vm.onIntent(RemoveBill(novemberBill))

        vm.onIntent(UndoRemoveBill(novemberBill))

        val content = vm.content()
        assertEquals(2_380L.rupees, content.ledger.pending)
        assertEquals(novemberBill.id, content.ledger.removableBill)
        assertEquals(null, content.removedBill)
    }

    @Test
    fun theBillUndoOfferEndsAfterAFewSecondsAndTheRemovalStays() = runTest {
        val vm = viewModel()
        vm.onIntent(RemoveBill(novemberBill))

        advanceTimeBy(4_900)
        assertEquals(novemberBill, vm.content().removedBill)

        advanceTimeBy(200)
        assertEquals(null, vm.content().removedBill)
        assertEquals(emptyList(), vm.content().ledger.entries.filterIsInstance<LedgerEntry.BillEntry>())
    }

    @Test
    fun aBillOfARenterWhoLeftCannotBeRemovedAndShowsWhy() = runTest {
        val left = FakeRenterRepository(listOf(Ravi.renter(billsElectricity = false).copy(active = false, leftOn = LocalDate(2026, 11, 28))))
        val bills = FakeBillRepository(listOf(novemberBill))
        val vm = viewModel(renters = left, bills = bills)

        vm.onIntent(RemoveBill(novemberBill))

        assertEquals(DomainError.BillNotRemovable, vm.content().actionError)
        assertEquals(listOf(novemberBill), bills.saved)
        assertEquals(null, vm.content().removedBill)
    }
}
