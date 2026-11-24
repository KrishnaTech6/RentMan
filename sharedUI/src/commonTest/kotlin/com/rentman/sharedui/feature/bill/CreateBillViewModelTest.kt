package com.rentman.sharedui.feature.bill

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.BuildBillUseCase
import com.rentman.domain.billing.CreateBillUseCase
import com.rentman.domain.billing.ObserveBillContextUseCase
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentMode
import com.rentman.sharedui.feature.payment.FakePaymentRepository
import com.rentman.domain.tenancy.RenterId
import com.rentman.sharedui.feature.address.FakeAddressRepository
import com.rentman.domain.billing.SuggestMeterReadingUseCase
import com.rentman.sharedui.feature.bill.CreateBillIntent.MeterScanned
import com.rentman.sharedui.feature.bill.CreateBillIntent.ClearMeterPhoto
import com.rentman.sharedui.feature.bill.CreateBillIntent.Back
import com.rentman.sharedui.feature.bill.CreateBillIntent.ChangeReading
import com.rentman.sharedui.feature.bill.CreateBillIntent.MeterReplacedChanged
import com.rentman.sharedui.feature.bill.CreateBillIntent.NewMeterStartChanged
import com.rentman.sharedui.feature.bill.CreateBillIntent.ReadingChanged
import com.rentman.sharedui.feature.bill.CreateBillIntent.Retry
import com.rentman.sharedui.feature.bill.CreateBillIntent.Review
import com.rentman.sharedui.feature.bill.CreateBillIntent.Save
import com.rentman.sharedui.feature.bill.CreateBillUiState.Content
import com.rentman.sharedui.feature.bill.CreateBillUiState.Error
import com.rentman.sharedui.feature.renter.FakeRenterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class CreateBillViewModelTest {
    private val november = BillingMonth.of(2026, 11)

    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel(
        electric: Boolean = true,
        bills: FakeBillRepository = FakeBillRepository(),
        renterId: Long = 1,
        renters: FakeRenterRepository = FakeRenterRepository(listOf(Ravi.renter(electric))),
        payments: FakePaymentRepository = FakePaymentRepository(),
    ) = CreateBillViewModel(
        renterId = RenterId(renterId),
        month = november,
        observeContext = ObserveBillContextUseCase(renters, FakeAddressRepository(listOf(Ravi.address)), bills, FakeSettingsRepository(), payments),
        buildBill = BuildBillUseCase(),
        suggestReading = SuggestMeterReadingUseCase(),
        createBill = CreateBillUseCase(bills),
    )

    private fun CreateBillViewModel.content() = uiState.value as Content

    @Test
    fun moneyPaidAheadShowsAsACreditOnTheBill() = runTest {
        val paidAhead = Payment(PaymentId(1), RenterId(1), 1_000L.rupees, LocalDate(2026, 10, 20), PaymentMode.Cash)

        val vm = viewModel(electric = false, payments = FakePaymentRepository(listOf(paidAhead)))

        assertEquals(Paise(-100_000), vm.content().context.pendingBefore)
        assertEquals(Paise(7_500_00 + 200_00 - 1_000_00), vm.content().context.totalDue(vm.content().bill()))
    }

    private fun Content.bill(): Bill = (preview as Outcome.Success).value

    @Test
    fun aRenterWhoPaysElectricityStartsOnTheReadingStepWithLastMonthsReading() = runTest {
        val state = viewModel().content()

        assertEquals(BillStep.Reading, state.step)
        assertEquals(1240, state.context.previousReading)
        assertIs<Outcome.Failure>(state.preview)
        assertEquals(false, state.showProblems) // no red text before the user has tried anything
    }

    @Test
    fun aRenterWhoPaysDirectlyGoesStraightToTheBill() = runTest {
        val state = viewModel(electric = false).content()

        assertEquals(BillStep.Review, state.step)
        assertEquals(7_700L.rupees, state.bill().total)
        assertEquals(listOf(BillLineKind.Rent, BillLineKind.FixedCharge), state.bill().lines.map { it.kind })
    }

    @Test
    fun typingAReadingShowsTheBillStraightAway() = runTest {
        val vm = viewModel()

        vm.onIntent(ReadingChanged("1325"))

        val state = vm.content()
        assertEquals(8_380L.rupees, state.bill().total)
        assertEquals(85, state.bill().units)
        assertEquals(8_380L.rupees, state.context.totalDue(state.bill()))
    }

    @Test
    fun reviewWithoutAReadingStaysAndShowsTheProblem() = runTest {
        val vm = viewModel()

        vm.onIntent(Review)

        assertEquals(BillStep.Reading, vm.content().step)
        assertEquals(true, vm.content().showProblems)

        vm.onIntent(ReadingChanged("1"))
        assertEquals(false, vm.content().showProblems)
    }

    @Test
    fun reviewWithAGoodReadingMovesToTheBillAndChangeReadingGoesBack() = runTest {
        val vm = viewModel()
        vm.onIntent(ReadingChanged("1325"))

        vm.onIntent(Review)
        assertEquals(BillStep.Review, vm.content().step)

        vm.onIntent(ChangeReading)
        assertEquals(BillStep.Reading, vm.content().step)
        assertEquals("1325", vm.content().reading) // what was typed is kept
    }

    @Test
    fun aReadingBelowLastMonthsIsAnErrorUntilTheMeterIsMarkedReplaced() = runTest {
        val vm = viewModel()
        vm.onIntent(ReadingChanged("120"))
        assertEquals(
            Outcome.Failure(DomainError.ReadingBelowPrevious(previous = 1240, current = 120)),
            vm.content().preview,
        )

        vm.onIntent(MeterReplacedChanged(true))
        vm.onIntent(NewMeterStartChanged("50"))

        assertEquals(70, vm.content().bill().units)
        assertEquals(true, vm.content().bill().meterReset)

        vm.onIntent(MeterReplacedChanged(false)) // changed their mind: the old comparison applies again
        assertIs<Outcome.Failure>(vm.content().preview)
        assertEquals("", vm.content().newMeterStart)
    }

    @Test
    fun savingStoresTheBillAndNavigatesAway() = runTest {
        val bills = FakeBillRepository()
        val vm = viewModel(bills = bills)
        vm.onIntent(ReadingChanged("1325"))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().bill()))

            assertEquals(CreateBillNavEvent.Saved(whatsAppUrl = null), awaitItem())
        }
        val saved = bills.saved.single()
        assertEquals(listOf("Rent", "Electricity", "Water"), saved.lines.map(BillLine::label))
        assertEquals(8_380L.rupees, saved.total)
        assertEquals(LocalDate(2026, 12, 5), saved.dueOn) // the November bill is due on 5 Dec
        assertEquals(false, vm.content().isSaving)
    }

    @Test
    fun savingAndSendingHandsTheWhatsAppLinkToTheScreenOnlyAfterTheBillIsStored() = runTest {
        val bills = FakeBillRepository()
        val vm = viewModel(bills = bills)
        vm.onIntent(ReadingChanged("1325"))
        val link = "https://wa.me/919876543210?text=Hi"

        vm.navigation.test {
            vm.onIntent(Save(vm.content().bill(), whatsAppUrl = link))

            assertEquals(CreateBillNavEvent.Saved(whatsAppUrl = link), awaitItem())
            assertEquals(1, bills.saved.size)
        }
    }

    @Test
    fun noWhatsAppLinkIsHandedOverWhenTheSaveFails() = runTest {
        val vm = viewModel(bills = FakeBillRepository(failAdd = true))
        vm.onIntent(ReadingChanged("1325"))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().bill(), whatsAppUrl = "https://wa.me/1?text=Hi"))

            expectNoEvents()
        }
    }

    @Test
    fun aBillThatAlreadyExistsIsReportedAndNothingNavigates() = runTest {
        val bills = FakeBillRepository()
        val vm = viewModel(bills = bills)
        vm.onIntent(ReadingChanged("1325"))
        val bill = vm.content().bill()
        bills.add(bill) // created from another screen in the meantime

        vm.navigation.test {
            vm.onIntent(Save(bill))

            assertEquals(DomainError.BillExists, vm.content().saveError)
            assertEquals(1, bills.saved.size)
            expectNoEvents()
        }
    }

    @Test
    fun aStorageFailureOnSaveKeepsTheTypedReading() = runTest {
        val bills = FakeBillRepository(failAdd = true)
        val vm = viewModel(bills = bills)
        vm.onIntent(ReadingChanged("1325"))

        vm.onIntent(Save(vm.content().bill()))

        assertEquals(DomainError.Storage("disk"), vm.content().saveError)
        assertEquals("1325", vm.content().reading)
        assertEquals(false, vm.content().isSaving)
    }

    @Test
    fun backAsksTheScreenToClose() = runTest {
        val vm = viewModel()

        vm.navigation.test {
            vm.onIntent(Back)

            assertEquals(CreateBillNavEvent.Close, awaitItem())
        }
    }

    @Test
    fun anUnknownRenterShowsAnErrorAndRetryReadsAgain() = runTest {
        val renters = FakeRenterRepository()
        val vm = viewModel(renters = renters)
        assertEquals(Error(DomainError.NotFound), vm.uiState.value)

        renters.add(Ravi.renter())
        vm.onIntent(Retry)

        assertIs<Content>(vm.uiState.value)
    }

    // ---- the meter photo

    @Test
    fun scanningAMeterFillsInTheReadingFromThePhotoAndKeepsThePhoto() = runTest {
        val vm = viewModel() // the meter was at 1240

        vm.onIntent(MeterScanned("9f3a.jpg", listOf("kWh", "01325")))

        val content = vm.content()
        assertEquals("1325", content.reading)
        assertEquals("9f3a.jpg", content.meterPhoto)
        assertEquals(ScanResult.Read, content.scanResult)
        assertEquals(85, content.bill().units) // the preview is worked out from the suggested number
    }

    @Test
    fun theSavedBillCarriesThePhotoAsProof() = runTest {
        val bills = FakeBillRepository()
        val vm = viewModel(bills = bills)
        vm.onIntent(MeterScanned("9f3a.jpg", listOf("01325")))
        vm.onIntent(Review)

        vm.navigation.test {
            vm.onIntent(Save(vm.content().bill()))
            awaitItem()
        }

        assertEquals("9f3a.jpg", bills.saved.single().meterPhotoPath)
        assertEquals(1325, bills.saved.single().currentReading)
    }

    @Test
    fun aPhotoWithNoNumberInItIsKeptAndTheLandlordIsToldToTypeTheNumber() = runTest {
        val vm = viewModel()
        vm.onIntent(ReadingChanged("1300"))

        vm.onIntent(MeterScanned("9f3a.jpg", listOf("Energy meter", "230 V")))

        val content = vm.content()
        assertEquals("1300", content.reading) // what was typed is not replaced by nothing
        assertEquals(ScanResult.NothingFound, content.scanResult)
        assertEquals("9f3a.jpg", content.meterPhoto)
    }

    @Test
    fun typingAfterAScanTakesTheSuggestionHintAway() = runTest {
        val vm = viewModel()
        vm.onIntent(MeterScanned("9f3a.jpg", listOf("01325")))

        vm.onIntent(ReadingChanged("1326"))

        assertEquals(null, vm.content().scanResult)
        assertEquals("9f3a.jpg", vm.content().meterPhoto) // the photo is still there
    }

    @Test
    fun scanningAgainReplacesThePhotoAndTheSuggestion() = runTest {
        val vm = viewModel()
        vm.onIntent(MeterScanned("first.jpg", listOf("01300")))

        vm.onIntent(MeterScanned("second.jpg", listOf("01325")))

        assertEquals("second.jpg", vm.content().meterPhoto)
        assertEquals("1325", vm.content().reading)
    }

    @Test
    fun removingThePhotoLeavesTheReadingAndSavesNoPhoto() = runTest {
        val vm = viewModel()
        vm.onIntent(MeterScanned("9f3a.jpg", listOf("01325")))

        vm.onIntent(ClearMeterPhoto)

        assertEquals(null, vm.content().meterPhoto)
        assertEquals(null, vm.content().scanResult)
        assertEquals("1325", vm.content().reading)
        assertEquals(null, vm.content().bill().meterPhotoPath)
    }

    @Test
    fun whenTheMeterWasReplacedTheSuggestionIsComparedWithTheNewStartingReading() = runTest {
        val vm = viewModel()
        vm.onIntent(MeterReplacedChanged(true))
        vm.onIntent(NewMeterStartChanged("20"))

        // Old meter was at 1240, so 00064 would be below it, but the new meter started at 20 so 64 is a fine reading.
        vm.onIntent(MeterScanned("9f3a.jpg", listOf("00064", "01325")))

        assertEquals("64", vm.content().reading)
    }

    @Test
    fun aRenterWhoPaysDirectlyHasNoMeterToScan() = runTest {
        val vm = viewModel(electric = false)

        vm.onIntent(MeterScanned("9f3a.jpg", listOf("01325")))

        assertEquals(null, vm.content().bill().meterPhotoPath)
    }
}
