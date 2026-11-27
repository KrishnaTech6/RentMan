package com.rentman.domain.payments

import com.rentman.domain.TestData
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.RenterSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentMessageTest {
    private val money: (Paise) -> String = { "₹${it.value / 100}" }

    @Test
    fun namesTheRenterTheirUnitAndAddressAndTheAmount() {
        val summary = RenterSummary(TestData.renter(), TestData.address)

        assertEquals("Paid: Ravi Kumar (Ground floor · 338 A) - ₹7500", PaymentMessage.text(summary, 7_500L.rupees, money))
    }

    @Test
    fun withoutAUnitOnlyTheAddressIsShown() {
        val summary = RenterSummary(TestData.renter().copy(unit = null), TestData.address)

        assertEquals("Paid: Ravi Kumar (338 A) - ₹4000", PaymentMessage.text(summary, 4_000L.rupees, money))
    }

    @Test
    fun extraSpacesAroundTheNameAreDropped() {
        val summary = RenterSummary(TestData.renter(name = "  Ravi Kumar "), TestData.address)

        assertEquals("Paid: Ravi Kumar (Ground floor · 338 A) - ₹100", PaymentMessage.text(summary, 100L.rupees, money))
    }

    @Test
    fun theLandlordsOwnWordingIsUsedWithWhatIsLeftAfterThePayment() {
        val summary = RenterSummary(TestData.renter(), TestData.address)

        assertEquals(
            "Ravi Kumar ne ₹4000 diya, baaki ₹3500",
            PaymentMessage.text(summary, 4_000L.rupees, money, pending = 3_500L.rupees, template = "{name} ne {amount} diya, baaki {pending}"),
        )
        // Paid ahead: nothing is left, never a negative amount.
        assertEquals("baaki ₹0", PaymentMessage.text(summary, 4_000L.rupees, money, pending = Paise(-50_000), template = "baaki {pending}"))
    }
}
