package com.rentman.domain.billing

import com.rentman.domain.TestData
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.RenterSummary
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthReviewMessageTest {
    private val month = BillingMonth.of(2026, 11)
    private val money: (Paise) -> String = { "₹${it.value / 100}" }

    private fun row(id: Long, name: String, billed: Long, collected: Long, late: Long = 0, notDue: Long = 0, hasBill: Boolean = true) =
        RenterMonth(
            summary = RenterSummary(TestData.renter(id = id, name = name), TestData.address),
            month = month,
            billed = billed.rupees,
            collected = collected.rupees,
            late = late.rupees,
            notDue = notDue.rupees,
            dueOn = LocalDate(2026, 12, 5).takeIf { hasBill },
        )

    private fun detail(vararg rows: RenterMonth) = MonthDetail(
        summary = MonthSummary(
            month = month,
            collected = Paise(rows.sumOf { it.collected.value }),
            late = Paise(rows.sumOf { it.late.value }),
            notDue = Paise(rows.sumOf { it.notDue.value }),
            notBilled = rows.count { !it.hasBill },
            notBilledRent = Paise.Zero,
            earlier = Paise.Zero,
        ),
        rows = rows.toList(),
    )

    @Test
    fun listsTheTotalsThenEveryoneWhoStillOwesInRowOrder() {
        val review = detail(
            row(1, "Amit Shah", billed = 10_500, collected = 0, late = 10_500),
            row(2, "Mohan Lal", billed = 7_500, collected = 3_000, notDue = 4_500),
            row(3, "Ravi Kumar", billed = 7_500, collected = 7_500),
        )

        assertEquals(
            """
            November 2026
            Total: ₹25500
            Received: ₹10500
            Pending: ₹15000
            Amit Shah - ₹10500
            Mohan Lal - ₹4500
            """.trimIndent(),
            MonthReviewMessage.text(review, "November 2026", money),
        )
    }

    @Test
    fun whenEverythingIsPaidThereIsNoPendingPart() {
        val review = detail(row(1, "Ravi Kumar", billed = 7_500, collected = 7_500))

        assertEquals("November 2026\nTotal: ₹7500\nReceived: ₹7500", MonthReviewMessage.text(review, "November 2026", money))
    }

    @Test
    fun aRenterWithNoBillIsNotListed() {
        val review = detail(
            row(1, "Ravi Kumar", billed = 7_500, collected = 7_500),
            row(2, "Mohan Lal", billed = 0, collected = 0, hasBill = false),
        )

        assertEquals("November 2026\nTotal: ₹7500\nReceived: ₹7500", MonthReviewMessage.text(review, "November 2026", money))
    }
}
