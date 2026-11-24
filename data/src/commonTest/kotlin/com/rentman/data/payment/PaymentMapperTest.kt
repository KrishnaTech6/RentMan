package com.rentman.data.payment

import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.tenancy.RenterId
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentMapperTest {
    private val withNote = Payment(
        id = PaymentId(4),
        renterId = RenterId(2),
        amount = 4_000L.rupees,
        paidOn = LocalDate(2026, 11, 22),
        mode = PaymentMode.Upi,
        note = "second half next week",
    )

    @Test
    fun aPaymentWithANoteSurvivesTheRoundTrip() {
        assertEquals(withNote, withNote.toEntity(createdAt = 1L).toDomain())
    }

    @Test
    fun aPaymentWithoutANoteSurvivesTheRoundTripForEveryMode() {
        PaymentMode.entries.forEach { mode ->
            val payment = withNote.copy(mode = mode, note = null)

            assertEquals(payment, payment.toEntity(createdAt = 1L).toDomain())
        }
    }

    @Test
    fun storesPaiseTheEpochDayAndTheModeByName() {
        val entity = withNote.copy(amount = Paise(85_050)).toEntity(createdAt = 99L)

        assertEquals(85_050L, entity.amountPaise)
        assertEquals(LocalDate(2026, 11, 22).toEpochDays(), entity.paidOn)
        assertEquals("Upi", entity.mode)
        assertEquals(null, entity.voidedAt) // a new payment is never removed
        assertEquals(99L, entity.createdAt)
    }
}
