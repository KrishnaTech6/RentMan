package com.rentman.domain.billing

import com.rentman.domain.Outcome
import com.rentman.domain.TestData
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.RenterSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class BillMessageTest {
    private val november = BillingMonth.of(2026, 11)

    /** Stands in for the platform ₹ formatter, which the domain doesn't have. */
    private val money: (Paise) -> String = { "₹${it.value / 100}" }

    private fun context(
        renter: com.rentman.domain.tenancy.Renter = TestData.renter(),
        pending: Paise = Paise.Zero,
    ) = BillContext(RenterSummary(renter, TestData.address), november, previousReading = 1240, rate = Paise(800), pendingBefore = pending)

    private fun message(context: BillContext, reading: String = "1325"): String {
        val bill = (BuildBillUseCase()(context, reading, newMeterStart = "") as Outcome.Success).value
        return BillMessage.text(context, bill, monthLabel = "Nov 2026", dueLabel = "5 Dec", money = money)
    }

    @Test
    fun matchesTheExampleInThePlan() {
        assertEquals(
            """
            Hi Ravi, your bill for Nov 2026:
            Rent: ₹7500
            Electricity: 1240 → 1325 = 85 units × ₹8 = ₹680
            Water: ₹200
            Previous pending: ₹1880
            Total due: ₹10260
            Please pay by 5 Dec. Thanks!
            """.trimIndent(),
            message(context(pending = 1_880L.rupees)),
        )
    }

    @Test
    fun leavesOutThePreviousPendingLineWhenNothingIsOwed() {
        val text = message(context())

        assertEquals(false, "Previous pending" in text)
        assertEquals("Total due: ₹8380", text.lines().single { it.startsWith("Total due") })
    }

    @Test
    fun moneyPaidAheadIsShownAsACreditNeverAsANegativePending() {
        val text = message(context(pending = Paise(-100_000)))

        assertEquals(false, "Previous pending" in text)
        assertEquals(false, "-" in text.lines().filter { it.startsWith("Credit") || it.startsWith("Total") }.joinToString())
        assertEquals("Credit from earlier payments: ₹1000", text.lines().single { it.startsWith("Credit") })
        assertEquals("Total due: ₹7380", text.lines().single { it.startsWith("Total due") })
    }

    @Test
    fun aRenterWhoPaysElectricityDirectlyGetsNoElectricityLine() {
        val text = message(context(TestData.renter(billsElectricity = false)), reading = "")

        assertEquals(listOf("Rent: ₹7500", "Water: ₹200"), text.lines().filter { it.startsWith("Rent") || it.startsWith("Water") || it.startsWith("Electricity") })
    }

    @Test
    fun greetsByFirstNameOnly() {
        val text = message(context(TestData.renter(name = "  Sunita   Devi ")))

        assertEquals("Hi Sunita, your bill for Nov 2026:", text.lines().first())
    }

    @Test
    fun aDiscountLineKeepsItsSign() {
        val context = context()
        val bill = (BuildBillUseCase()(context, "1325", "") as Outcome.Success).value
            .let { it.copy(lines = it.lines + BillLine(BillLineKind.Adjustment, "Waived late fee", Paise(-5_000))) }

        val text = BillMessage.text(context, bill, "Nov 2026", "5 Dec", money = { "₹${it.value / 100}" })

        assertEquals("Waived late fee: ₹-50", text.lines().single { it.startsWith("Waived") })
    }
}
