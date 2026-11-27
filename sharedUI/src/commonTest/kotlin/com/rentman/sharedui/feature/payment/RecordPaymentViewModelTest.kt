package com.rentman.sharedui.feature.payment

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.ObserveRenterLedgerUseCase
import com.rentman.domain.payments.PaymentField
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.payments.RecordPaymentUseCase
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.time.GetTodayUseCase
import com.rentman.sharedui.feature.address.FakeAddressRepository
import com.rentman.sharedui.feature.bill.FakeBillRepository
import com.rentman.sharedui.feature.bill.Ravi
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.AmountChanged
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.Back
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.DayStepped
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.ModeChanged
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.NoteChanged
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.Retry
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.Save
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.UseFullAmount
import com.rentman.sharedui.feature.payment.RecordPaymentUiState.Content
import com.rentman.sharedui.feature.payment.RecordPaymentUiState.Error
import com.rentman.sharedui.feature.renter.FakeRenterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RecordPaymentViewModelTest {
    private val today = LocalDate(2026, 11, 23)
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-23T10:00:00Z")
    }

    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    /** Ravi owes ₹8,380.50: one November bill. */
    private val novemberBill = Bill(
        renterId = RenterId(1),
        month = BillingMonth.of(2026, 11),
        createdOn = LocalDate(2026, 11, 3),
        dueOn = LocalDate(2026, 12, 5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", Paise(838_050))),
    )

    private fun viewModel(
        payments: FakePaymentRepository = FakePaymentRepository(),
        renters: FakeRenterRepository = FakeRenterRepository(listOf(Ravi.renter(billsElectricity = false))),
        renterId: Long = 1,
    ): RecordPaymentViewModel {
        val addresses = FakeAddressRepository(listOf(Ravi.address))
        val bills = FakeBillRepository(listOf(novemberBill))
        return RecordPaymentViewModel(
            renterId = RenterId(renterId),
            observeLedger = ObserveRenterLedgerUseCase(renters, addresses, bills, payments),
            recordPayment = RecordPaymentUseCase(payments, GetTodayUseCase(clock, TimeZone.UTC)),
            getToday = GetTodayUseCase(clock, TimeZone.UTC),
        )
    }

    private fun RecordPaymentViewModel.content() = uiState.value as Content

    @Test
    fun startsOnTodayWithUpiAndShowsWhatIsPending() = runTest {
        val content = viewModel().content()

        assertEquals(today, content.paidOn)
        assertEquals(PaymentMode.Upi, content.mode)
        assertEquals(Paise(838_050), content.pending)
        assertEquals("Ravi Kumar", content.summary.renter.name)
    }

    @Test
    fun typingChangesTheFieldsAndClearsAProblemOnThatField() = runTest {
        val vm = viewModel()
        vm.onIntent(Save(vm.content().toForm()))
        assertEquals(setOf(PaymentField.Amount), vm.content().problems.keys)

        vm.onIntent(AmountChanged("4,000"))
        vm.onIntent(ModeChanged(PaymentMode.Cash))
        vm.onIntent(NoteChanged("first half"))

        val content = vm.content()
        assertEquals(emptyMap(), content.problems)
        assertEquals(Triple("4,000", PaymentMode.Cash, "first half"), Triple(content.amount, content.mode, content.note))
    }

    @Test
    fun fullAmountFillsInEverythingPendingIncludingPaise() = runTest {
        val vm = viewModel()

        vm.onIntent(UseFullAmount)

        assertEquals("8380.50", vm.content().amount)
    }

    @Test
    fun anAmountAbovePendingSetsAnOverpaymentWarningButStillSaves() = runTest {
        val payments = FakePaymentRepository()
        val vm = viewModel(payments)
        assertEquals(null, vm.content().overpayment)

        vm.onIntent(AmountChanged("9,000")) // ₹8,380.50 is pending
        assertEquals(Paise(61_950), vm.content().overpayment)

        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm()))
            assertEquals(RecordPaymentNavEvent.Saved(post = null), awaitItem())
        }
        assertEquals(1, payments.saved.size)

        vm.onIntent(UseFullAmount)
        assertEquals(null, vm.content().overpayment)
    }

    @Test
    fun theDateStepsBackADayAtATimeButNeverPastToday() = runTest {
        val vm = viewModel()

        vm.onIntent(DayStepped(1))
        assertEquals(today, vm.content().paidOn)

        vm.onIntent(DayStepped(-1))
        vm.onIntent(DayStepped(-1))
        assertEquals(LocalDate(2026, 11, 21), vm.content().paidOn)

        vm.onIntent(DayStepped(1))
        assertEquals(LocalDate(2026, 11, 22), vm.content().paidOn)
    }

    @Test
    fun savingStoresThePaymentAndNavigatesAway() = runTest {
        val payments = FakePaymentRepository()
        val vm = viewModel(payments)
        vm.onIntent(AmountChanged("5,000"))
        vm.onIntent(ModeChanged(PaymentMode.Cash))
        vm.onIntent(DayStepped(-2))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm()))

            assertEquals(RecordPaymentNavEvent.Saved(post = null), awaitItem())
        }
        val saved = payments.saved.single()
        assertEquals(5_000L.rupees, saved.amount)
        assertEquals(PaymentMode.Cash, saved.mode)
        assertEquals(LocalDate(2026, 11, 21), saved.paidOn)
        assertEquals(RenterId(1), saved.renterId)
    }

    @Test
    fun saveAndPostHandsTheStoredAmountToTheScreenForWhatsApp() = runTest {
        val payments = FakePaymentRepository()
        val vm = viewModel(payments)
        vm.onIntent(AmountChanged("7,500"))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm(), post = vm.content().summary))

            assertEquals(RecordPaymentNavEvent.Saved(PaymentPost(vm.content().summary, 7_500L.rupees)), awaitItem())
        }
        assertEquals(1, payments.saved.size)
    }

    @Test
    fun aWrongFormIsNotPostedOnWhatsApp() = runTest {
        val vm = viewModel()
        vm.onIntent(AmountChanged("abc"))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm(), post = vm.content().summary))

            expectNoEvents()
        }
    }

    @Test
    fun aWrongFormShowsEveryProblemAndStoresNothing() = runTest {
        val payments = FakePaymentRepository()
        val vm = viewModel(payments)
        vm.onIntent(AmountChanged("abc"))
        vm.onIntent(NoteChanged("x".repeat(101)))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm()))

            expectNoEvents()
        }
        assertEquals(
            mapOf(PaymentField.Amount to FieldProblem.Invalid, PaymentField.Note to FieldProblem.Invalid),
            vm.content().problems,
        )
        assertEquals(false, vm.content().isSaving)
        assertEquals(emptyList(), payments.saved)
    }

    @Test
    fun aStorageFailureKeepsTheFormAndShowsTheError() = runTest {
        val vm = viewModel(FakePaymentRepository(failAdd = true))
        vm.onIntent(AmountChanged("5,000"))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm()))

            expectNoEvents()
        }
        val content = vm.content()
        assertEquals(DomainError.Storage("disk"), content.saveError)
        assertEquals("5,000", content.amount)
        assertEquals(false, content.isSaving)
    }

    @Test
    fun aSecondTapWhileSavingIsIgnored() = runTest {
        val hold = CompletableDeferred<Unit>()
        val payments = FakePaymentRepository(holdAdd = hold)
        val vm = viewModel(payments)
        vm.onIntent(AmountChanged("5,000"))

        vm.onIntent(Save(vm.content().toForm()))
        assertEquals(true, vm.content().isSaving)
        vm.onIntent(Save(vm.content().toForm()))
        hold.complete(Unit)

        assertEquals(1, payments.addCalls)
        assertEquals(1, payments.saved.size)
    }

    @Test
    fun backAsksTheScreenToClose() = runTest {
        val vm = viewModel()

        vm.navigation.test {
            vm.onIntent(Back)

            assertEquals(RecordPaymentNavEvent.Close, awaitItem())
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
}
