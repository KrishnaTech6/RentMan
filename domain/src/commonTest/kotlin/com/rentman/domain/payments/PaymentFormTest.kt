package com.rentman.domain.payments

import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.RenterId
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaymentFormTest {
    private fun form(amount: String) = PaymentForm(RenterId(1), amount, LocalDate(2026, 11, 23), PaymentMode.Upi, "")

    @Test
    fun paymentAboveWhatIsPendingIsOverpaidByTheDifference() {
        assertEquals(45_000L.rupees, form("50,000").overpayment(pending = 5_000L.rupees))
        assertEquals(Paise(50), form("3,380.50").overpayment(pending = 3_380L.rupees))
    }

    @Test
    fun payingExactlyOrLessIsNotOverpaid() {
        assertNull(form("5,000").overpayment(pending = 5_000L.rupees))
        assertNull(form("4,000").overpayment(pending = 5_000L.rupees))
    }

    @Test
    fun anAmountThatCannotBeReadIsNotOverpaid() {
        assertNull(form("").overpayment(pending = 5_000L.rupees))
        assertNull(form("abc").overpayment(pending = 5_000L.rupees))
    }

    @Test
    fun whenNothingIsOwedTheWholePaymentIsExtraEvenIfPaidAheadAlready() {
        assertEquals(500L.rupees, form("500").overpayment(pending = Paise.Zero))
        assertEquals(500L.rupees, form("500").overpayment(pending = Paise(-100_000)))
    }
}
