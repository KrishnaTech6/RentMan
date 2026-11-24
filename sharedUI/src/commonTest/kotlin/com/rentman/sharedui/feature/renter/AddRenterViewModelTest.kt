package com.rentman.sharedui.feature.renter

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.BuildFixedChargeUseCase
import com.rentman.domain.tenancy.ChargeField
import com.rentman.domain.tenancy.FixedCharge
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.domain.tenancy.ObserveAddressesUseCase
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterField
import com.rentman.domain.tenancy.SaveRenterUseCase
import com.rentman.domain.time.GetTodayUseCase
import com.rentman.sharedui.feature.address.FakeAddressRepository
import com.rentman.sharedui.feature.renter.AddRenterIntent.AddCharge
import com.rentman.sharedui.feature.renter.AddRenterIntent.AskToRaiseChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.Back
import com.rentman.sharedui.feature.renter.AddRenterIntent.BillsElectricityChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.ChargeAmountChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.ChargeLabelChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.DueDayChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.NameChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.OwnRateChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.PhoneChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.PickAddress
import com.rentman.sharedui.feature.renter.AddRenterIntent.RemoveCharge
import com.rentman.sharedui.feature.renter.AddRenterIntent.RentChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.Retry
import com.rentman.sharedui.feature.renter.AddRenterIntent.Save
import com.rentman.sharedui.feature.renter.AddRenterIntent.StartMonthStepped
import com.rentman.sharedui.feature.renter.AddRenterIntent.StartingReadingChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.UnitChanged
import com.rentman.sharedui.feature.renter.AddRenterUiState.Content
import com.rentman.sharedui.feature.renter.AddRenterUiState.Error
import com.rentman.sharedui.navigation.AddressSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import com.rentman.domain.tenancy.FixedChargeId
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.ObserveRenterFormUseCase
import com.rentman.domain.tenancy.UpdateRenterUseCase
import com.rentman.sharedui.feature.bill.Ravi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AddRenterViewModelTest {
    private val home = Address(AddressId(1), "338 A")
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-23T10:00:00Z")
    }
    private val selection = AddressSelection()

    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel(renters: FakeRenterRepository = FakeRenterRepository(), editing: RenterId? = null) = AddRenterViewModel(
        renterId = editing,
        observeAddresses = ObserveAddressesUseCase(FakeAddressRepository(listOf(home))),
        observeRenterForm = ObserveRenterFormUseCase(renters),
        saveRenter = SaveRenterUseCase(renters, GetTodayUseCase(clock, TimeZone.UTC)),
        updateRenter = UpdateRenterUseCase(renters),
        getToday = GetTodayUseCase(clock, TimeZone.UTC),
        addressSelection = selection,
        buildFixedCharge = BuildFixedChargeUseCase(),
    )

    private fun AddRenterViewModel.content() = uiState.value as Content

    private fun AddRenterViewModel.fillValidForm() {
        selection.pick(home.id)
        onIntent(NameChanged("Ravi Kumar"))
        onIntent(PhoneChanged("98765 43210"))
        onIntent(RentChanged("7,500"))
        onIntent(StartingReadingChanged("1240"))
    }

    @Test
    fun startsWithSensibleDefaults() = runTest {
        val state = viewModel().content()

        assertEquals(LocalDate(2026, 11, 1), state.agreementStart)
        assertEquals("5", state.dueDay)
        assertEquals(true to true, state.askToRaise to state.billsElectricity)
        assertEquals(null, state.address)
    }

    @Test
    fun typingUpdatesTheFieldAndClearsItsProblem() = runTest {
        val vm = viewModel()
        vm.onIntent(Save(vm.content().toForm()))
        assertEquals(FieldProblem.Required, vm.content().problems[RenterField.Name])

        vm.onIntent(NameChanged("Ravi"))

        assertEquals("Ravi", vm.content().name)
        assertEquals(null, vm.content().problems[RenterField.Name])
        assertEquals(FieldProblem.Required, vm.content().problems[RenterField.Phone]) // other problems stay
    }

    @Test
    fun anAddressPickedOnTheOtherScreenIsShown() = runTest {
        val vm = viewModel()

        selection.pick(home.id)

        assertEquals(home, vm.content().address)
    }

    @Test
    fun theMonthStepperMovesTheAgreementStartByWholeMonths() = runTest {
        val vm = viewModel()

        vm.onIntent(StartMonthStepped(-11))
        assertEquals(LocalDate(2025, 12, 1), vm.content().agreementStart)
        vm.onIntent(StartMonthStepped(13))
        assertEquals(LocalDate(2027, 1, 1), vm.content().agreementStart)
    }

    @Test
    fun savingAnEmptyFormShowsEveryProblemAndSavesNothing() = runTest {
        val renters = FakeRenterRepository()
        val vm = viewModel(renters)

        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm()))

            assertEquals(
                setOf(RenterField.Address, RenterField.Name, RenterField.Phone, RenterField.Rent, RenterField.StartingReading),
                vm.content().problems.keys,
            )
            assertEquals(false, vm.content().isSaving)
            assertEquals(emptyList(), renters.saved)
            expectNoEvents()
        }
    }

    @Test
    fun savingAValidFormStoresTheRenterAndNavigatesAway() = runTest {
        val renters = FakeRenterRepository()
        val vm = viewModel(renters)
        vm.fillValidForm()
        vm.onIntent(OwnRateChanged("9"))
        vm.onIntent(AskToRaiseChanged(false))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm()))

            assertEquals(AddRenterNavEvent.Saved, awaitItem())
        }
        val saved: Renter = renters.saved.single()
        assertEquals(listOf("Ravi Kumar", "919876543210", Paise(900)), listOf(saved.name, saved.phone, saved.customRate))
        assertEquals(7_500L.rupees, saved.monthlyRent)
        assertEquals(0, saved.raisePercent)
        assertEquals(BillingMonth.of(2026, 11), saved.firstBillMonth)
        assertEquals(false, vm.content().isSaving)
    }

    @Test
    fun aRenterWhoPaysElectricityDirectlyNeedsNoReading() = runTest {
        val renters = FakeRenterRepository()
        val vm = viewModel(renters)
        vm.fillValidForm()
        vm.onIntent(StartingReadingChanged(""))
        vm.onIntent(BillsElectricityChanged(false))

        vm.onIntent(Save(vm.content().toForm()))

        assertEquals(1, renters.saved.size)
        assertEquals(null, renters.saved.single().startingReading)
    }

    @Test
    fun aSecondRenterAtTheSameAddressNeedsAUnit() = runTest {
        val renters = FakeRenterRepository()
        val vm = viewModel(renters)
        vm.fillValidForm()
        vm.onIntent(UnitChanged("Ground floor"))
        vm.onIntent(Save(vm.content().toForm()))

        vm.onIntent(NameChanged("Neha Verma"))
        vm.onIntent(UnitChanged("ground floor"))
        vm.onIntent(Save(vm.content().toForm()))

        assertEquals(FieldProblem.Duplicate, vm.content().problems[RenterField.Unit])
        assertEquals(1, renters.saved.size)
    }

    @Test
    fun aStorageFailureKeepsTheTypedFormAndShowsAMessage() = runTest {
        val vm = viewModel(FakeRenterRepository(failAdd = true))
        vm.fillValidForm()

        vm.onIntent(Save(vm.content().toForm()))

        val state = vm.content()
        assertEquals(DomainError.Storage("disk"), state.saveError)
        assertEquals("Ravi Kumar", state.name)
        assertEquals(false, state.isSaving)
    }

    @Test
    fun pickAddressAndBackAskTheScreenToNavigate() = runTest {
        val vm = viewModel()

        vm.navigation.test {
            vm.onIntent(PickAddress)
            assertEquals(AddRenterNavEvent.PickAddress, awaitItem())

            vm.onIntent(Back)
            assertEquals(AddRenterNavEvent.Close, awaitItem())
        }
    }

    @Test
    fun addingAValidChargeAppendsItAndClearsTheDraft() = runTest {
        val vm = viewModel()
        vm.onIntent(ChargeLabelChanged("Water"))
        vm.onIntent(ChargeAmountChanged("200"))

        vm.onIntent(AddCharge("Water", "200"))

        val state = vm.content()
        assertEquals(listOf(FixedCharge(label = "Water", amount = 200L.rupees)), state.fixedCharges)
        assertEquals("" to "", state.chargeLabel to state.chargeAmount)
        assertEquals(emptyMap(), state.chargeProblems)
    }

    @Test
    fun anInvalidChargeShowsItsProblemsAndIsNotAdded() = runTest {
        val vm = viewModel()

        vm.onIntent(AddCharge("", "abc"))

        assertEquals(emptyList(), vm.content().fixedCharges)
        assertEquals(
            mapOf(ChargeField.Label to FieldProblem.Required, ChargeField.Amount to FieldProblem.Invalid),
            vm.content().chargeProblems,
        )

        vm.onIntent(ChargeLabelChanged("Water"))

        assertEquals(mapOf(ChargeField.Amount to FieldProblem.Invalid), vm.content().chargeProblems)
    }

    @Test
    fun theSameChargeCannotBeAddedTwice() = runTest {
        val vm = viewModel()

        vm.onIntent(AddCharge("Water", "200"))
        vm.onIntent(AddCharge("Water", "200")) // e.g. a fast double tap

        assertEquals(1, vm.content().fixedCharges.size)
        assertEquals(mapOf(ChargeField.Label to FieldProblem.Duplicate), vm.content().chargeProblems)
    }

    @Test
    fun aChargeLeftInTheFieldsBlocksSaveUntilItIsAddedOrCleared() = runTest {
        val renters = FakeRenterRepository()
        val vm = viewModel(renters)
        vm.fillValidForm()
        vm.onIntent(ChargeLabelChanged("Garbage"))
        vm.onIntent(ChargeAmountChanged("50"))

        vm.onIntent(Save(vm.content().toForm()))

        assertEquals(FieldProblem.Invalid, vm.content().problems[RenterField.Charge])
        assertEquals(emptyList(), renters.saved)

        vm.onIntent(AddCharge("Garbage", "50"))
        vm.onIntent(Save(vm.content().toForm()))

        assertEquals(listOf("Garbage"), renters.saved.single().fixedCharges.map { it.label })
    }

    @Test
    fun typingInTheChargeFieldsClearsTheUnfinishedChargeProblem() = runTest {
        val vm = viewModel()
        vm.fillValidForm()
        vm.onIntent(ChargeLabelChanged("Water"))
        vm.onIntent(Save(vm.content().toForm()))
        assertEquals(FieldProblem.Invalid, vm.content().problems[RenterField.Charge])

        vm.onIntent(ChargeAmountChanged("200"))

        assertEquals(null, vm.content().problems[RenterField.Charge])
    }

    @Test
    fun aChargeCanBeRemovedByItsPosition() = runTest {
        val vm = viewModel()
        vm.onIntent(AddCharge("Water", "200"))
        vm.onIntent(AddCharge("Garbage", "50"))

        vm.onIntent(RemoveCharge(0))

        assertEquals(listOf("Garbage"), vm.content().fixedCharges.map { it.label })
    }

    @Test
    fun theChargesAreSavedWithTheRenter() = runTest {
        val renters = FakeRenterRepository()
        val vm = viewModel(renters)
        vm.fillValidForm()
        vm.onIntent(AddCharge("Water", "200"))

        vm.onIntent(Save(vm.content().toForm()))

        assertEquals(listOf("Water"), renters.saved.single().fixedCharges.map { it.label })
    }

    @Test
    fun dueDayCanBeEdited() = runTest {
        val vm = viewModel()

        vm.onIntent(DueDayChanged("10"))

        assertEquals("10", vm.content().dueDay)
    }

    @Test
    fun aStorageFailureWhileLoadingAddressesShowsErrorAndRetryRecovers() = runTest {
        val addresses = FakeAddressRepository(listOf(home), failReads = true)
        val renters = FakeRenterRepository()
        val vm = AddRenterViewModel(
            renterId = null,
            observeAddresses = ObserveAddressesUseCase(addresses),
            observeRenterForm = ObserveRenterFormUseCase(renters),
            saveRenter = SaveRenterUseCase(renters, GetTodayUseCase(clock, TimeZone.UTC)),
            updateRenter = UpdateRenterUseCase(renters),
            getToday = GetTodayUseCase(clock, TimeZone.UTC),
            addressSelection = selection,
            buildFixedCharge = BuildFixedChargeUseCase(),
        )
        assertIs<Error>(vm.uiState.value)

        addresses.failReads = false
        vm.onIntent(Retry)

        assertIs<Content>(vm.uiState.value)
    }

    // ---- editing a saved renter

    private val saved = Ravi.renter().copy(
        id = RenterId(7),
        customRate = Paise(850),
        fixedCharges = listOf(FixedCharge(FixedChargeId(1), "Water", 200L.rupees), FixedCharge(FixedChargeId(2), "Old", 50L.rupees, active = false)),
    )

    @Test
    fun editingStartsFromWhatIsSavedWithOnlyTheChargesStillInUse() = runTest {
        val state = viewModel(FakeRenterRepository(listOf(saved)), editing = saved.id).content()

        assertEquals(saved.id, state.renterId)
        assertEquals(
            listOf("Ravi Kumar", "919876543210", "7500", "Ground floor", "1240", "8.50"),
            listOf(state.name, state.phone, state.rent, state.unit, state.startingReading, state.ownRate),
        )
        assertEquals(listOf("Water"), state.fixedCharges.map { it.label })
        assertEquals(home.id, state.addressId)
        assertEquals(saved.agreementStart, state.agreementStart)
    }

    @Test
    fun addingStillStartsEmpty() = runTest {
        assertEquals(null, viewModel().content().renterId)
        assertEquals("", viewModel().content().name)
    }

    @Test
    fun savingAnEditChangesTheSameRenterAndAddsNoOne() = runTest {
        val renters = FakeRenterRepository(listOf(saved))
        val vm = viewModel(renters, editing = saved.id)
        vm.onIntent(NameChanged("Ravi K"))
        vm.onIntent(RentChanged("8,000"))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm()))

            assertEquals(AddRenterNavEvent.Saved, awaitItem())
        }
        val stored = renters.saved.single()
        assertEquals(saved.id, stored.id)
        assertEquals("Ravi K", stored.name)
        assertEquals(8_000L.rupees, stored.monthlyRent)
    }

    @Test
    fun anEditedFormIsCheckedLikeANewOneAndKeepsWhatWasTyped() = runTest {
        val renters = FakeRenterRepository(listOf(saved))
        val vm = viewModel(renters, editing = saved.id)
        vm.onIntent(PhoneChanged("123"))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm()))

            expectNoEvents()
        }
        assertEquals(setOf(RenterField.Phone), vm.content().problems.keys)
        assertEquals("123", vm.content().phone)
        assertEquals("919876543210", renters.saved.single().phone) // nothing was saved
    }

    @Test
    fun takingANeighboursUnitIsRefusedButKeepingYourOwnIsFine() = runTest {
        val neighbour = Ravi.renter().copy(id = RenterId(8), name = "Neha", unit = "1st floor")
        val renters = FakeRenterRepository(listOf(saved, neighbour))
        val vm = viewModel(renters, editing = saved.id)

        vm.onIntent(UnitChanged("1st floor"))
        vm.onIntent(Save(vm.content().toForm()))
        assertEquals(mapOf(RenterField.Unit to FieldProblem.Duplicate), vm.content().problems)

        vm.onIntent(UnitChanged("Ground floor"))
        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm()))
            assertEquals(AddRenterNavEvent.Saved, awaitItem())
        }
    }

    @Test
    fun whatWasTypedIsNotReplacedWhenTheSavedRenterChangesBehindTheScreen() = runTest {
        val renters = FakeRenterRepository(listOf(saved))
        val vm = viewModel(renters, editing = saved.id)
        vm.onIntent(NameChanged("Typed"))

        renters.change(saved.id) { it.copy(name = "Changed elsewhere") }

        assertEquals("Typed", vm.content().name)
    }

    @Test
    fun anAddressPickedWhileEditingMovesTheRenter() = runTest {
        val vm = viewModel(FakeRenterRepository(listOf(saved)), editing = saved.id)

        selection.pick(AddressId(5))

        assertEquals(AddressId(5), vm.content().addressId)
    }

    @Test
    fun editingARenterThatDoesNotExistIsAnErrorAndRetryReadsAgain() = runTest {
        val renters = FakeRenterRepository()
        val vm = viewModel(renters, editing = RenterId(99))
        assertEquals(AddRenterUiState.Error(DomainError.NotFound), vm.uiState.value)

        renters.add(saved.copy(id = RenterId(0))) // gets id 1
        val retried = viewModel(renters, editing = RenterId(1))

        assertIs<Content>(retried.uiState.value)
    }
}
