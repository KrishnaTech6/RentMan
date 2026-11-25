package com.rentman.sharedui.feature.months

import app.cash.turbine.test
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.ObserveMonthsUseCase
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.ObserveLeftRentersUseCase
import com.rentman.domain.tenancy.ObserveRenterSummariesUseCase
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.time.GetTodayUseCase
import com.rentman.sharedui.feature.address.FakeAddressRepository
import com.rentman.sharedui.feature.bill.FakeBillRepository
import com.rentman.sharedui.feature.months.MonthsIntent.Back
import com.rentman.sharedui.feature.months.MonthsIntent.OpenMonth
import com.rentman.sharedui.feature.months.MonthsIntent.Refresh
import com.rentman.sharedui.feature.months.MonthsIntent.Retry
import com.rentman.sharedui.feature.months.MonthsUiState.Content
import com.rentman.sharedui.feature.months.MonthsUiState.Error
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
class MonthsViewModelTest {
    private val home = Address(AddressId(1), "338 A")
    private val ravi = Renter(
        id = RenterId(1), addressId = home.id, unit = "Ground floor", name = "Ravi Kumar", phone = "919876543210",
        monthlyRent = 7_500L.rupees, dueDay = 5, agreementStart = LocalDate(2026, 1, 1), raisePercent = 10, raiseEveryMonths = 11,
        billsElectricity = false, customRate = null, startingReading = null, firstBillMonth = BillingMonth.of(2026, 10),
    )

    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private var now = Instant.parse("2026-11-23T10:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = now
    }

    private fun bill(month: BillingMonth) = Bill(
        renterId = ravi.id, month = month, createdOn = month.atStartOfMonth(), dueOn = month.plusMonths(1).dueOn(5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", 7_500L.rupees)),
    )

    private fun viewModel(renters: FakeRenterRepository, bills: FakeBillRepository = FakeBillRepository()): MonthsViewModel {
        val addresses = FakeAddressRepository(listOf(home))
        return MonthsViewModel(
            ObserveMonthsUseCase(
                ObserveRenterSummariesUseCase(renters, addresses),
                ObserveLeftRentersUseCase(renters, addresses),
                bills,
                FakePaymentRepository(),
                GetTodayUseCase(clock, TimeZone.UTC),
            ),
        )
    }

    @Test
    fun showsEveryMonthNewestFirst() = runTest {
        val vm = viewModel(FakeRenterRepository(listOf(ravi)), FakeBillRepository(listOf(bill(BillingMonth.of(2026, 10)))))

        assertEquals(listOf(BillingMonth.of(2026, 11), BillingMonth.of(2026, 10)), (vm.uiState.value as Content).months.map { it.month })
    }

    @Test
    fun noRentersIsAnEmptyContentNotAnError() = runTest {
        assertEquals(Content(emptyList()), viewModel(FakeRenterRepository()).uiState.value)
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
    fun refreshLooksAgainAndAMonthThatStartedWhileOpenAppears() = runTest {
        val vm = viewModel(FakeRenterRepository(listOf(ravi)))
        vm.uiState.test {
            assertEquals(BillingMonth.of(2026, 11), (awaitItem() as Content).months.first().month)

            now = Instant.parse("2026-12-01T10:00:00Z")
            vm.onIntent(Refresh)

            // Loading was never shown in between.
            assertEquals(BillingMonth.of(2026, 12), (awaitItem() as Content).months.first().month)
        }
    }

    @Test
    fun openMonthAsksTheScreenToOpenThatMonth() = runTest {
        val vm = viewModel(FakeRenterRepository(listOf(ravi)))

        vm.navigation.test {
            vm.onIntent(OpenMonth(BillingMonth.of(2026, 10)))

            assertEquals(MonthsNavEvent.OpenMonth(BillingMonth.of(2026, 10)), awaitItem())
        }
    }

    @Test
    fun backAsksTheScreenToClose() = runTest {
        val vm = viewModel(FakeRenterRepository())

        vm.navigation.test {
            vm.onIntent(Back)

            assertEquals(MonthsNavEvent.Close, awaitItem())
        }
    }
}
