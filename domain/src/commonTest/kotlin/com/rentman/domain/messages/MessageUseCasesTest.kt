package com.rentman.domain.messages

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.FakeSettingsRepository
import com.rentman.domain.money.Paise
import com.rentman.domain.settings.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageUseCasesTest {
    private val ownBill = "Namaste {first_name}, {month} ka bill: {total}"

    @Test
    fun theLandlordsOwnMessageIsSavedAndEverythingElseIsKept() = runTest {
        val settings = FakeSettingsRepository(AppSettings(globalRate = Paise(900)))

        SaveMessageUseCase(settings)(MessageKind.Bill, ownBill)

        assertEquals(ownBill, settings.current.messageFor(MessageKind.Bill))
        assertEquals(MessageKind.PaymentReceived.default, settings.current.messageFor(MessageKind.PaymentReceived))
        assertEquals(Paise(900), settings.current.globalRate)
    }

    @Test
    fun aMessageWithAVariableTheAppDoesNotKnowIsNotSaved() = runTest {
        val settings = FakeSettingsRepository()

        val outcome = SaveMessageUseCase(settings)(MessageKind.Bill, "Hi {nmae}, {total}")

        assertEquals(Outcome.Failure(DomainError.InvalidMessage(listOf("nmae"))), outcome)
        assertEquals(null, settings.current.billMessage)
    }

    @Test
    fun anEmptyMessageOrOurOwnWordingMeansTheDefault() = runTest {
        val settings = FakeSettingsRepository(AppSettings(billMessage = ownBill))

        SaveMessageUseCase(settings)(MessageKind.Bill, "  \n ")
        assertEquals(null, settings.current.billMessage)

        SaveMessageUseCase(settings)(MessageKind.Bill, ownBill)
        SaveMessageUseCase(settings)(MessageKind.Bill, MessageKind.Bill.default)
        assertEquals(null, settings.current.billMessage) // later changes to our wording still reach this landlord
    }

    @Test
    fun theWordingInUseIsTheLandlordsOwnOrOursAndFollowsChanges() = runTest {
        val settings = FakeSettingsRepository()
        val observed = ObserveMessageUseCase(settings)(MessageKind.Bill)
        assertEquals(Outcome.Success(MessageKind.Bill.default), observed.first())

        SaveMessageUseCase(settings)(MessageKind.Bill, ownBill)

        assertEquals(Outcome.Success(ownBill), observed.first())
    }
}
