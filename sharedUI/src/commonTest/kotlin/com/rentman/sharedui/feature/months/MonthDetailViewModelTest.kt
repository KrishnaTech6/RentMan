package com.rentman.sharedui.feature.months

import app.cash.turbine.test
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.ObserveMonthDetailUseCase
import com.rentman.domain.billing.RenterStatus
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.ObserveLeftRentersUseCase
import com.rentman.domain.tenancy.ObserveRenterSummariesUseCase
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.time.GetTodayUseCase
import com.rentman.sharedui.feature.address.FakeAddressRepository
import com.rentman.sharedui.feature.bill.FakeBillRepository
import com.rentman.sharedui.feature.months.MonthDetailIntent.Back
import com.rentman.sharedui.feature.months.MonthDetailIntent.OpenRenter
import com.rentman.sharedui.feature.months.MonthDetailIntent.Refresh
import com.rentman.sharedui.feature.months.MonthDetailIntent.Retry
import com.rentman.sharedui.feature.months.MonthDetailUiState.Content
import com.rentman.sharedui.feature.months.MonthDetailUiState.Error
import com.rentman.sharedui.feature.payment.FakePaymentRepository
import com.rentman.sharedui.feature.renter.FakeRenterRepository
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
class MonthDetailViewModelTest {
    private val home = Address(AddressId(1), "338 A")
    private val november = BillingMonth.of(2026, 11)
    private val ravi = Renter(
        id = RenterId(1), addressId = home.id, unit = "Ground floor", name = "Ravi Kumar", phone = "919876543210",
        monthlyRent = 7_500L.rupees, dueDay = 5, agreementStart = LocalDate(2026, 1, 1), raisePercent = 10, raiseEveryMonths = 11,
        billsElectricity = false, customRate = null, startingReading = null, firstBillMonth = november,
    )
    private val neha = ravi.copy(id = RenterId(2), name = "Neha Verma", unit = "1st floor")

    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private var now = Instant.parse("2026-11-23T10:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = now
    }

    private fun bill(renter: Renter) = Bill(
        renterId = renter.id, month = november, createdOn = LocalDate(2026, 11, 3), dueOn = LocalDate(2026, 12, 5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", 8_380L.rupees)),
    )

    private fun viewModel(
        renters: FakeRenterRepository,
        bills: FakeBillRepository = FakeBillRepository(),
        payments: FakePaymentRepository = FakePaymentRepository(),
    ): MonthDetailViewModel {
        val addresses = FakeAddressRepository(listOf(home))
        return MonthDetailViewModel(
            november,
            ObserveMonthDetailUseCase(
                ObserveRenterSummariesUseCase(renters, addresses),
                ObserveLeftRentersUseCase(renters, addresses),
                bills, payments, GetTodayUseCase(clock, TimeZone.UTC),
            ),
        )
    }

    @Test
    fun showsEveryRenterInTheMonthMostUrgentFirst() = runTest {
        val payments = FakePaymentRepository()
        payments.add(Payment(renterId = neha.id, amount = 8_380L.rupees, paidOn = LocalDate(2026, 11, 20), mode = PaymentMode.Upi))
        val vm = viewModel(FakeRenterRepository(listOf(neha, ravi)), FakeBillRepository(listOf(bill(ravi), bill(neha))), payments)

        val detail = (vm.uiState.value as Content).detail
        assertEquals(listOf("Ravi Kumar", "Neha Verma"), detail.rows.map { it.summary.renter.name })
        assertEquals(listOf(RenterStatus.Unpaid, RenterStatus.Paid), detail.rows.map { it.status })
        assertEquals(8_380L.rupees, detail.summary.collected)
        assertEquals(8_380L.rupees, detail.summary.cashIn)
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
    fun refreshLooksAgainWithoutShowingLoading() = runTest {
        val vm = viewModel(FakeRenterRepository(listOf(ravi)), FakeBillRepository(listOf(bill(ravi))))
        vm.uiState.test {
            assertEquals(RenterStatus.Unpaid, (awaitItem() as Content).detail.rows.single().status)

            now = Instant.parse("2026-12-10T10:00:00Z") // the bill fell due while the app stayed open
            vm.onIntent(Refresh)

            // The very next state already says Late: Loading was never shown in between.
            assertEquals(RenterStatus.Late, (awaitItem() as Content).detail.rows.single().status)
        }
    }

    @Test
    fun openRenterAsksTheScreenToOpenThatRenter() = runTest {
        val vm = viewModel(FakeRenterRepository(listOf(ravi)))

        vm.navigation.test {
            vm.onIntent(OpenRenter(ravi.id))

            assertEquals(MonthDetailNavEvent.OpenRenter(ravi.id), awaitItem())
        }
    }

    @Test
    fun backAsksTheScreenToClose() = runTest {
        val vm = viewModel(FakeRenterRepository())

        vm.navigation.test {
            vm.onIntent(Back)

            assertEquals(MonthDetailNavEvent.Close, awaitItem())
        }
    }
}
