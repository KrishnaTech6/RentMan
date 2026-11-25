package com.rentman.sharedui.feature.home

import app.cash.turbine.test
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.ObserveBillTodosUseCase
import com.rentman.domain.billing.ObserveHomeUseCase
import com.rentman.domain.billing.RenterStatus
import com.rentman.domain.billing.MonthSummary
import com.rentman.domain.money.Paise
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentMode
import com.rentman.sharedui.feature.payment.FakePaymentRepository
import com.rentman.domain.time.GetTodayUseCase
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.DomainError
import com.rentman.domain.tenancy.ApplyRentIncreaseUseCase
import com.rentman.domain.tenancy.ObserveLeftRentersUseCase
import com.rentman.domain.tenancy.ObserveRentIncreaseOffersUseCase
import com.rentman.domain.tenancy.ObserveRenterSummariesUseCase
import com.rentman.domain.tenancy.PostponeRentIncreaseUseCase
import com.rentman.domain.tenancy.UndoRentIncreaseUseCase
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterSummary
import com.rentman.sharedui.feature.address.FakeAddressRepository
import com.rentman.sharedui.feature.bill.FakeBillRepository
import com.rentman.sharedui.feature.home.HomeIntent.AddRenter
import com.rentman.sharedui.feature.home.HomeIntent.CreateBill
import com.rentman.sharedui.feature.home.HomeIntent.NotNow
import com.rentman.sharedui.feature.home.HomeIntent.OpenRenter
import com.rentman.sharedui.feature.home.HomeIntent.RaiseRent
import com.rentman.sharedui.feature.home.HomeIntent.UndoRaise
import com.rentman.sharedui.feature.home.HomeIntent.OpenSettings
import com.rentman.sharedui.feature.home.HomeIntent.Refresh
import com.rentman.sharedui.feature.home.HomeIntent.Retry
import com.rentman.sharedui.feature.home.HomeIntent.SelectFilter
import com.rentman.sharedui.feature.home.HomeUiState.Content
import com.rentman.sharedui.feature.home.HomeUiState.Error
import com.rentman.sharedui.feature.renter.FakeRentIncreaseRepository
import com.rentman.sharedui.feature.renter.FakeRenterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val home = Address(AddressId(1), "338 A")
    private val ravi = Renter(
        id = RenterId(1),
        addressId = home.id,
        unit = "Ground floor",
        name = "Ravi Kumar",
        phone = "919876543210",
        monthlyRent = 7_500L.rupees,
        dueDay = 5,
        agreementStart = LocalDate(2026, 1, 1),
        raisePercent = 10,
        raiseEveryMonths = 11,
        billsElectricity = false,
        customRate = null,
        startingReading = null,
        firstBillMonth = BillingMonth.of(2026, 11),
    )

    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private var now = Instant.parse("2026-11-23T10:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = now
    }

    private fun viewModel(
        renters: FakeRenterRepository,
        addresses: FakeAddressRepository = FakeAddressRepository(listOf(home)),
        bills: FakeBillRepository = FakeBillRepository(),
        payments: FakePaymentRepository = FakePaymentRepository(),
        rentChanges: FakeRentIncreaseRepository = FakeRentIncreaseRepository(renters),
    ): HomeViewModel {
        val today = GetTodayUseCase(clock, TimeZone.UTC)
        return HomeViewModel(
            ObserveHomeUseCase(
                ObserveRenterSummariesUseCase(renters, addresses),
                ObserveBillTodosUseCase(renters, addresses, bills, today),
                ObserveRentIncreaseOffersUseCase(renters, addresses, rentChanges, bills, today),
                ObserveLeftRentersUseCase(renters, addresses),
                bills,
                payments,
                today,
            ),
            ApplyRentIncreaseUseCase(rentChanges, today),
            PostponeRentIncreaseUseCase(rentChanges, today),
            UndoRentIncreaseUseCase(rentChanges),
        )
    }

    /** Ravi's agreement began in Dec 2025, so on 23 Nov 2026 he is due a raise: ₹7,500 -> ₹8,250. */
    private val dueForRaise = ravi.copy(agreementStart = LocalDate(2025, 12, 1))

    @Test
    fun showsRentersWithTheirAddress() = runTest {
        val vm = viewModel(FakeRenterRepository(listOf(ravi)))

        assertEquals(listOf(RenterSummary(ravi, home)), (vm.uiState.value as Content).renters.map { it.summary })
    }

    @Test
    fun aRenterWithoutABillThisMonthHasAToDo() = runTest {
        val vm = viewModel(FakeRenterRepository(listOf(ravi)))

        assertEquals(listOf(BillingMonth.of(2026, 11)), (vm.uiState.value as Content).todos.map { it.month })
    }

    @Test
    fun onceTheBillExistsTheToDoIsGone() = runTest {
        val bill = Bill(
            renterId = ravi.id,
            month = BillingMonth.of(2026, 11),
            dueOn = LocalDate(2026, 11, 5),
            lines = listOf(BillLine(BillLineKind.Rent, "Rent", 7_500L.rupees)),
        )

        val vm = viewModel(FakeRenterRepository(listOf(ravi)), bills = FakeBillRepository(listOf(bill)))

        assertEquals(emptyList(), (vm.uiState.value as Content).todos)
    }

    @Test
    fun refreshingAfterTheMonthChangesShowsTheNewMonthsToDoWithoutFlashingLoading() = runTest {
        now = Instant.parse("2026-11-30T10:00:00Z")
        val novemberBill = Bill(
            renterId = ravi.id,
            month = BillingMonth.of(2026, 11),
            dueOn = LocalDate(2026, 11, 5),
            lines = listOf(BillLine(BillLineKind.Rent, "Rent", 7_500L.rupees)),
        )
        val vm = viewModel(FakeRenterRepository(listOf(ravi)), bills = FakeBillRepository(listOf(novemberBill)))
        assertEquals(emptyList(), (vm.uiState.value as Content).todos) // 30 Nov: November is billed, nothing to do

        vm.uiState.test {
            assertEquals(emptyList(), (awaitItem() as Content).todos)

            now = Instant.parse("2026-12-01T10:00:00Z") // the app stayed open past midnight
            vm.onIntent(Refresh)

            // The very next state already has December's card: Loading was never shown in between.
            assertEquals(listOf(BillingMonth.of(2026, 12)), (awaitItem() as Content).todos.map { it.month })
        }
    }

    @Test
    fun noRentersIsAnEmptyContentNotAnError() = runTest {
        assertEquals(Content(emptyList(), emptyList(), emptyList(), emptyList(), MonthSummary(BillingMonth.of(2026, 11), Paise.Zero, Paise.Zero, Paise.Zero, 0, Paise.Zero, Paise.Zero)), viewModel(FakeRenterRepository()).uiState.value)
    }

    @Test
    fun showsWhatEachRenterOwesTheirStatusAndHowThisMonthIsGoing() = runTest {
        val bill = Bill(
            renterId = ravi.id,
            month = BillingMonth.of(2026, 11),
            createdOn = LocalDate(2026, 11, 3),
            dueOn = LocalDate(2026, 12, 5),
            lines = listOf(BillLine(BillLineKind.Rent, "Rent", 8_380L.rupees)),
        )
        val payment = Payment(PaymentId(1), ravi.id, 5_000L.rupees, LocalDate(2026, 11, 22), PaymentMode.Upi)

        val vm = viewModel(FakeRenterRepository(listOf(ravi)), bills = FakeBillRepository(listOf(bill)), payments = FakePaymentRepository(listOf(payment)))

        val content = vm.uiState.value as Content
        assertEquals(3_380L.rupees, content.renters.single().pending)
        assertEquals(RenterStatus.PartPaid, content.renters.single().status)
        assertEquals(5_000L.rupees, content.thisMonth.collected)
        assertEquals(3_380L.rupees, content.thisMonth.notDue)
    }

    @Test
    fun aNewPaymentUpdatesHomeWithoutARefresh() = runTest {
        val bill = Bill(
            renterId = ravi.id,
            month = BillingMonth.of(2026, 11),
            createdOn = LocalDate(2026, 11, 3),
            dueOn = LocalDate(2026, 12, 5),
            lines = listOf(BillLine(BillLineKind.Rent, "Rent", 8_380L.rupees)),
        )
        val payments = FakePaymentRepository()
        val vm = viewModel(FakeRenterRepository(listOf(ravi)), bills = FakeBillRepository(listOf(bill)), payments = payments)
        assertEquals(Paise.Zero, (vm.uiState.value as Content).thisMonth.collected)

        payments.add(Payment(renterId = ravi.id, amount = 8_380L.rupees, paidOn = LocalDate(2026, 11, 23), mode = PaymentMode.Cash))

        assertEquals(8_380L.rupees, (vm.uiState.value as Content).thisMonth.collected)
        assertEquals(RenterStatus.Paid, (vm.uiState.value as Content).renters.single().status)
    }

    @Test
    fun aRenterDueARaiseGetsACardWithTheNewRent() = runTest {
        val offer = (viewModel(FakeRenterRepository(listOf(dueForRaise))).uiState.value as Content).raises.single()

        assertEquals(7_500L.rupees, offer.currentRent)
        assertEquals(8_250L.rupees, offer.newRent)
        assertEquals(emptyList(), (viewModel(FakeRenterRepository(listOf(ravi))).uiState.value as Content).raises)
    }

    @Test
    fun raisingTheRentChangesItRemovesTheCardAndOffersUndo() = runTest {
        val renters = FakeRenterRepository(listOf(dueForRaise))
        val changes = FakeRentIncreaseRepository(renters)
        val vm = viewModel(renters, rentChanges = changes)
        val offer = (vm.uiState.value as Content).raises.single()

        vm.onIntent(RaiseRent(offer))

        val content = vm.uiState.value as Content
        assertEquals(emptyList(), content.raises)
        assertEquals(8_250L.rupees, content.renters.single().summary.renter.monthlyRent)
        assertEquals(8_250L.rupees, content.raiseUndo?.newRent)
        assertEquals(1, changes.active.size)
    }

    @Test
    fun aSecondTapOnTheSameCardIsRejectedWithoutBreakingTheFirstRaiseOrItsUndo() = runTest {
        val renters = FakeRenterRepository(listOf(dueForRaise))
        val changes = FakeRentIncreaseRepository(renters)
        val vm = viewModel(renters, rentChanges = changes)
        val offer = (vm.uiState.value as Content).raises.single()
        vm.onIntent(RaiseRent(offer))

        vm.onIntent(RaiseRent(offer)) // the very fast double tap

        val content = vm.uiState.value as Content
        assertEquals(1, changes.active.size)
        assertEquals(DomainError.OfferOutdated, content.actionError)
        assertEquals(8_250L.rupees, content.raiseUndo?.newRent) // the first raise can still be undone

        vm.onIntent(UndoRaise(content.raiseUndo!!))
        assertEquals(7_500L.rupees, (vm.uiState.value as Content).renters.single().summary.renter.monthlyRent)
        assertEquals(emptyList(), changes.active)
    }

    @Test
    fun theErrorGoesAwayWithTheUndoOffer() = runTest {
        val vm = viewModel(FakeRenterRepository(listOf(dueForRaise)))
        val offer = (vm.uiState.value as Content).raises.single()
        vm.onIntent(RaiseRent(offer))
        vm.onIntent(RaiseRent(offer))
        assertEquals(DomainError.OfferOutdated, (vm.uiState.value as Content).actionError)

        advanceTimeBy(5_100)

        assertEquals(null, (vm.uiState.value as Content).actionError)
    }

    @Test
    fun undoingBringsBackTheOldRentTheCardAndClosesTheOffer() = runTest {
        val renters = FakeRenterRepository(listOf(dueForRaise))
        val vm = viewModel(renters)
        vm.onIntent(RaiseRent((vm.uiState.value as Content).raises.single()))

        vm.onIntent(UndoRaise((vm.uiState.value as Content).raiseUndo!!))

        val content = vm.uiState.value as Content
        assertEquals(7_500L.rupees, content.renters.single().summary.renter.monthlyRent)
        assertEquals(1, content.raises.size)
        assertEquals(null, content.raiseUndo)
    }

    @Test
    fun theUndoOfferEndsAfterAFewSecondsAndTheRaiseStays() = runTest {
        val vm = viewModel(FakeRenterRepository(listOf(dueForRaise)))
        vm.onIntent(RaiseRent((vm.uiState.value as Content).raises.single()))

        advanceTimeBy(4_900)
        assertEquals(true, (vm.uiState.value as Content).raiseUndo != null)

        advanceTimeBy(200)
        assertEquals(null, (vm.uiState.value as Content).raiseUndo)
        assertEquals(8_250L.rupees, (vm.uiState.value as Content).renters.single().summary.renter.monthlyRent)
    }

    @Test
    fun notNowHidesTheCardWithoutChangingTheRent() = runTest {
        val vm = viewModel(FakeRenterRepository(listOf(dueForRaise)))

        vm.onIntent(NotNow(dueForRaise.id))

        val content = vm.uiState.value as Content
        assertEquals(emptyList(), content.raises)
        assertEquals(7_500L.rupees, content.renters.single().summary.renter.monthlyRent)
        assertEquals(null, content.raiseUndo)
    }

    @Test
    fun aFailedRaiseShowsAnErrorAndChangesNothing() = runTest {
        val renters = FakeRenterRepository(listOf(dueForRaise))
        val changes = FakeRentIncreaseRepository(renters, failWrites = true)
        val vm = viewModel(renters, rentChanges = changes)

        vm.onIntent(RaiseRent((vm.uiState.value as Content).raises.single()))

        val content = vm.uiState.value as Content
        assertEquals(DomainError.Storage("disk"), content.actionError)
        assertEquals(null, content.raiseUndo)
        assertEquals(1, content.raises.size)
    }

    @Test
    fun aRenterWhoLeftIsListedApartAndStillOpens() = runTest {
        val gone = ravi.copy(id = RenterId(2), name = "Sunita", unit = "1st floor", active = false, leftOn = LocalDate(2026, 10, 31))
        val vm = viewModel(FakeRenterRepository(listOf(ravi, gone)))

        val content = vm.uiState.value as Content
        assertEquals(listOf("Ravi Kumar"), content.renters.map { it.summary.renter.name })
        assertEquals(listOf("Sunita"), content.left.map { it.renter.name })

        vm.navigation.test {
            vm.onIntent(OpenRenter(gone.id))
            assertEquals(HomeNavEvent.OpenRenter(gone.id), awaitItem())
        }
    }

    @Test
    fun openSettingsAsksTheScreenToNavigate() = runTest {
        val vm = viewModel(FakeRenterRepository())

        vm.navigation.test {
            vm.onIntent(OpenSettings)

            assertEquals(HomeNavEvent.OpenSettings, awaitItem())
        }
    }

    @Test
    fun openRenterAsksTheScreenToOpenThatRenter() = runTest {
        val vm = viewModel(FakeRenterRepository(listOf(ravi)))

        vm.navigation.test {
            vm.onIntent(OpenRenter(ravi.id))

            assertEquals(HomeNavEvent.OpenRenter(ravi.id), awaitItem())
        }
    }

    @Test
    fun createBillAsksTheScreenToOpenThatRentersBill() = runTest {
        val vm = viewModel(FakeRenterRepository(listOf(ravi)))

        vm.navigation.test {
            vm.onIntent(CreateBill(ravi.id, BillingMonth.of(2026, 11)))

            assertEquals(HomeNavEvent.OpenCreateBill(ravi.id, BillingMonth.of(2026, 11)), awaitItem())
        }
    }

    @Test
    fun aStorageFailureShowsErrorAndRetryRecovers() = runTest {
        val renters = FakeRenterRepository(listOf(ravi), failReads = true)
        val vm = viewModel(renters)
        assertIs<Error>(vm.uiState.value)

        renters.failReads = false
        vm.onIntent(Retry)

        assertIs<Content>(vm.uiState.value)
    }

    @Test
    fun addRenterAsksTheScreenToNavigate() = runTest {
        val vm = viewModel(FakeRenterRepository())

        vm.navigation.test {
            vm.onIntent(AddRenter)

            assertEquals(HomeNavEvent.OpenAddRenter, awaitItem())
        }
    }

    private val neha = ravi.copy(id = RenterId(2), name = "Neha Verma", unit = "1st floor")

    private fun bill(renter: Renter) = Bill(
        renterId = renter.id,
        month = BillingMonth.of(2026, 11),
        createdOn = LocalDate(2026, 11, 3),
        dueOn = LocalDate(2026, 12, 5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", 8_380L.rupees)),
    )

    /** Ravi has not paid (Unpaid), Neha has paid in full (Paid). */
    private suspend fun raviOwesNehaPaid(payments: FakePaymentRepository = FakePaymentRepository()): HomeViewModel {
        payments.add(Payment(renterId = neha.id, amount = 8_380L.rupees, paidOn = LocalDate(2026, 11, 22), mode = PaymentMode.Upi))
        return viewModel(FakeRenterRepository(listOf(ravi, neha)), bills = FakeBillRepository(listOf(bill(ravi), bill(neha))), payments = payments)
    }

    @Test
    fun theChipsCountEachStatusMostUrgentFirst() = runTest {
        val content = raviOwesNehaPaid().uiState.value as Content

        assertEquals(listOf(RenterStatus.Unpaid to 1, RenterStatus.Paid to 1), content.statusCounts)
    }

    @Test
    fun choosingAStatusShowsOnlyThoseRentersAndAllShowsEveryone() = runTest {
        val vm = raviOwesNehaPaid()

        vm.onIntent(SelectFilter(RenterStatus.Paid))
        assertEquals(listOf("Neha Verma"), (vm.uiState.value as Content).shown.map { it.summary.renter.name })

        vm.onIntent(SelectFilter(null))
        assertEquals(listOf("Ravi Kumar", "Neha Verma"), (vm.uiState.value as Content).shown.map { it.summary.renter.name })
    }

    @Test
    fun theChosenStatusSurvivesNewData() = runTest {
        val payments = FakePaymentRepository()
        val vm = raviOwesNehaPaid(payments)
        vm.onIntent(SelectFilter(RenterStatus.Paid))

        payments.add(Payment(renterId = ravi.id, amount = 1_000L.rupees, paidOn = LocalDate(2026, 11, 23), mode = PaymentMode.Cash))

        val content = vm.uiState.value as Content
        assertEquals(RenterStatus.Paid, content.filter)
        assertEquals(listOf("Neha Verma"), content.shown.map { it.summary.renter.name })
    }

    @Test
    fun theFilterIsDroppedWhenNobodyHasThatStatusAnyMore() = runTest {
        val payments = FakePaymentRepository()
        val vm = raviOwesNehaPaid(payments)
        vm.onIntent(SelectFilter(RenterStatus.Unpaid))

        payments.add(Payment(renterId = ravi.id, amount = 8_380L.rupees, paidOn = LocalDate(2026, 11, 23), mode = PaymentMode.Cash))

        val content = vm.uiState.value as Content
        assertEquals(null, content.filter)
        assertEquals(2, content.shown.size)
    }

    @Test
    fun theFilterIsDroppedWhenEveryoneEndsUpWithThatStatus() = runTest {
        // The chips are hidden when everybody has one status, so a filter left on could never be cleared.
        val bills = FakeBillRepository(listOf(bill(ravi), bill(neha)))
        val payments = FakePaymentRepository(listOf(Payment(PaymentId(1), neha.id, 8_380L.rupees, LocalDate(2026, 11, 22), PaymentMode.Upi)))
        val vm = viewModel(FakeRenterRepository(listOf(ravi, neha)), bills = bills, payments = payments)
        vm.onIntent(SelectFilter(RenterStatus.Unpaid))

        payments.setVoided(PaymentId(1), true) // Neha's payment is taken back: both are Unpaid now

        assertEquals(null, (vm.uiState.value as Content).filter)
    }
}
