package com.rentman.sharedui.feature.address

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.tenancy.AddAddressUseCase
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.ObserveAddressesUseCase
import com.rentman.sharedui.feature.address.AddressPickerIntent.Add
import com.rentman.sharedui.feature.address.AddressPickerIntent.Confirm
import com.rentman.sharedui.feature.address.AddressPickerIntent.NameChanged
import com.rentman.sharedui.feature.address.AddressPickerIntent.Retry
import com.rentman.sharedui.feature.address.AddressPickerIntent.Select
import com.rentman.sharedui.feature.address.AddressPickerUiState.Content
import com.rentman.sharedui.feature.address.AddressPickerUiState.Error
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AddressPickerViewModelTest {
    private val a12 = Address(AddressId(1), "12 A")
    private val parkView = Address(AddressId(2), "Park View G-7")

    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel(repository: FakeAddressRepository) =
        AddressPickerViewModel(ObserveAddressesUseCase(repository), AddAddressUseCase(repository))

    @Test
    fun showsTheAddressesOnceLoaded() = runTest {
        viewModel(FakeAddressRepository(listOf(parkView, a12))).uiState.test {
            assertEquals(Content(addresses = listOf(a12, parkView)), expectMostRecentItem())
        }
    }

    @Test
    fun selectingAnAddressMarksItSelected() = runTest {
        val vm = viewModel(FakeAddressRepository(listOf(a12, parkView)))

        vm.onIntent(Select(parkView.id))

        assertEquals(parkView, (vm.uiState.value as Content).selected)
    }

    @Test
    fun addingSelectsTheNewAddressAndClearsTheField() = runTest {
        val vm = viewModel(FakeAddressRepository(listOf(a12)))
        vm.onIntent(NameChanged("Park View I-3"))

        vm.onIntent(Add("Park View I-3"))

        val state = vm.uiState.value as Content
        assertEquals(listOf("12 A", "Park View I-3"), state.addresses.map(Address::name))
        assertEquals("Park View I-3", state.selected?.name)
        assertEquals("", state.newName)
        assertEquals(false, state.isSaving)
    }

    @Test
    fun aBlankNameShowsAnErrorAndDoesNotSave() = runTest {
        val vm = viewModel(FakeAddressRepository(listOf(a12)))

        vm.onIntent(Add("   "))

        val state = vm.uiState.value as Content
        assertEquals(DomainError.BlankName, state.nameError)
        assertEquals(listOf(a12), state.addresses)
    }

    @Test
    fun aDuplicateNameShowsAnError() = runTest {
        val vm = viewModel(FakeAddressRepository(listOf(a12)))

        vm.onIntent(Add("12 a"))

        assertEquals(DomainError.DuplicateName("12 a"), (vm.uiState.value as Content).nameError)
    }

    @Test
    fun typingClearsTheNameError() = runTest {
        val vm = viewModel(FakeAddressRepository(listOf(a12)))
        vm.onIntent(Add(""))

        vm.onIntent(NameChanged("A"))

        assertEquals(null, (vm.uiState.value as Content).nameError)
    }

    @Test
    fun aSecondAddWhileSavingIsIgnored() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeAddressRepository(listOf(a12), gate = gate)
        val vm = viewModel(repository)

        vm.onIntent(Add("Park View I-3"))
        assertEquals(true, (vm.uiState.value as Content).isSaving)
        vm.onIntent(Add("Park View I-3"))
        gate.complete(Unit)

        assertEquals(1, repository.addCalls)
        assertEquals(false, (vm.uiState.value as Content).isSaving)
    }

    @Test
    fun aStorageFailureWhileSavingShowsTheErrorState() = runTest {
        val vm = viewModel(FakeAddressRepository(listOf(a12), failAdd = true))

        vm.onIntent(Add("Park View I-3"))

        assertEquals(Error(DomainError.Storage("disk")), vm.uiState.value)
    }

    @Test
    fun confirmingEmitsThePickedAddress() = runTest {
        val vm = viewModel(FakeAddressRepository(listOf(a12)))

        vm.navigation.test {
            vm.onIntent(Confirm(a12.id))

            assertEquals(AddressPickerNavEvent.Picked(a12.id), awaitItem())
        }
    }

    @Test
    fun aStorageFailureShowsErrorAndRetryRecovers() = runTest {
        val repository = FakeAddressRepository(listOf(a12), failReads = true)
        val vm = viewModel(repository)
        assertIs<Error>(vm.uiState.value)

        repository.failReads = false
        vm.onIntent(Retry)

        assertEquals(Content(addresses = listOf(a12)), vm.uiState.value)
    }
}
