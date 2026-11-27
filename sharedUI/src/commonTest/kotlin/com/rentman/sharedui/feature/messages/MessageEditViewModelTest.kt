package com.rentman.sharedui.feature.messages

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.messages.MessageKind
import com.rentman.domain.messages.ObserveMessageUseCase
import com.rentman.domain.messages.SaveMessageUseCase
import com.rentman.domain.settings.AppSettings
import com.rentman.sharedui.feature.bill.FakeSettingsRepository
import com.rentman.sharedui.feature.messages.MessageEditIntent.Back
import com.rentman.sharedui.feature.messages.MessageEditIntent.InsertVariable
import com.rentman.sharedui.feature.messages.MessageEditIntent.Save
import com.rentman.sharedui.feature.messages.MessageEditIntent.TextChanged
import com.rentman.sharedui.feature.messages.MessageEditIntent.UseOurWording
import com.rentman.sharedui.feature.messages.MessageEditUiState.Content
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

@OptIn(ExperimentalCoroutinesApi::class)
class MessageEditViewModelTest {
    private val own = "{name} ne {amount} diya"

    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel(settings: FakeSettingsRepository = FakeSettingsRepository(), kind: MessageKind = MessageKind.PaymentReceived) =
        MessageEditViewModel(kind, ObserveMessageUseCase(settings), SaveMessageUseCase(settings))

    private fun MessageEditViewModel.content() = uiState.value as Content

    @Test
    fun startsWithTheWordingInUseAndShowsItFilledInWithAnExample() = runTest {
        assertEquals(MessageKind.PaymentReceived.default, viewModel().content().text)

        val vm = viewModel(FakeSettingsRepository(AppSettings(paymentMessage = own)))
        assertEquals(own, vm.content().text)
        assertEquals("Ravi Kumar ne ₹7,500 diya", vm.content().preview)
    }

    @Test
    fun typingChangesTheTextAndWhereTheCursorIs() = runTest {
        val vm = viewModel()

        vm.onIntent(TextChanged("Paid {amount}", cursor = 5))

        assertEquals("Paid {amount}", vm.content().text)
        assertEquals(5, vm.content().cursor)
    }

    @Test
    fun aTappedVariableGoesInWhereTheCursorIsAndTheCursorMovesPastIt() = runTest {
        val vm = viewModel()
        vm.onIntent(TextChanged("Paid  by", cursor = 5))

        vm.onIntent(InsertVariable("amount"))

        assertEquals("Paid {amount} by", vm.content().text)
        assertEquals(13, vm.content().cursor)
        assertEquals(1, vm.content().replaced) // the box starts from the new text; typing alone never does this
    }

    @Test
    fun useOurWordingPutsOurTextBack() = runTest {
        val vm = viewModel(FakeSettingsRepository(AppSettings(paymentMessage = own)))

        vm.onIntent(UseOurWording)

        assertEquals(MessageKind.PaymentReceived.default, vm.content().text)
    }

    @Test
    fun saveStoresTheWordingAndCloses() = runTest {
        val settings = FakeSettingsRepository()
        val vm = viewModel(settings)
        vm.onIntent(TextChanged(own, cursor = own.length))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().text))

            assertEquals(MessageEditNavEvent.Close, awaitItem())
        }
        assertEquals(own, settings.current.paymentMessage)
    }

    @Test
    fun aVariableTheAppDoesNotKnowIsShownAndNothingIsSaved() = runTest {
        val settings = FakeSettingsRepository()
        val vm = viewModel(settings)
        vm.onIntent(TextChanged("Paid {amont}", cursor = 0))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().text))

            expectNoEvents()
        }
        assertEquals(listOf("amont"), vm.content().unknownVariables)
        assertEquals(false, vm.content().isSaving)
        assertEquals(null, settings.current.paymentMessage)

        vm.onIntent(TextChanged("Paid {amount}", cursor = 0))
        assertEquals(emptyList(), vm.content().unknownVariables) // typing clears it
    }

    @Test
    fun aStorageFailureKeepsTheTextAndShowsTheError() = runTest {
        val vm = viewModel(FakeSettingsRepository(failSave = true))
        vm.onIntent(TextChanged(own, cursor = 0))

        vm.onIntent(Save(vm.content().text))

        assertEquals(DomainError.Storage("disk"), vm.content().saveError)
        assertEquals(own, vm.content().text)
    }

    @Test
    fun backCloses() = runTest {
        val vm = viewModel()

        vm.navigation.test {
            vm.onIntent(Back)

            assertEquals(MessageEditNavEvent.Close, awaitItem())
        }
    }
}
