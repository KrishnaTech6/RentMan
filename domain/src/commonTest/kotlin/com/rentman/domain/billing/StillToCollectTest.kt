package com.rentman.domain.billing

import com.rentman.domain.TestData
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.RenterSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class StillToCollectTest {
    private fun row(id: Long, pending: Paise, status: RenterStatus, overdue: Paise = Paise.Zero) =
        RenterRow(RenterSummary(TestData.renter(id = id, name = "Renter $id"), TestData.address), pending, status, overdue)

    private fun todo(id: Long, rent: Paise) =
        BillTodo(RenterSummary(TestData.renter(id = id, name = "Renter $id", rent = rent), TestData.address), BillingMonth.of(2026, 11))

    @Test
    fun splitsWhatIsOwedIntoLateAndNotDueYetAndTheTwoAddUpToTheTotal() {
        val result = StillToCollect.of(
            listOf(
                row(1, 9_000L.rupees, RenterStatus.Late, overdue = 9_000L.rupees),
                row(2, 2_000L.rupees, RenterStatus.Late, overdue = 2_000L.rupees),
                row(3, 5_796L.rupees, RenterStatus.Unpaid),
                row(4, 1_000L.rupees, RenterStatus.PartPaid),
            ),
            todos = emptyList(),
        )

        assertEquals(11_000L.rupees, result.late)
        assertEquals(6_796L.rupees, result.notDueYet)
        assertEquals(17_796L.rupees, result.total)
        assertEquals(4, result.owing)
    }

    @Test
    fun onlyTheOverdueShareOfWhatARenterOwesIsLate() {
        // Amit owes 13,000: 8,000 from a bill past its due day and 5,000 from the newest bill, which isn't due yet.
        val result = StillToCollect.of(listOf(row(1, 13_000L.rupees, RenterStatus.Unpaid, overdue = 8_000L.rupees)), todos = emptyList())

        assertEquals(8_000L.rupees, result.late)
        assertEquals(5_000L.rupees, result.notDueYet)
        assertEquals(13_000L.rupees, result.total)
    }

    @Test
    fun aRenterWhoIsPaidOrPaidAheadAddsNothingAndDoesNotReduceTheOthers() {
        val result = StillToCollect.of(
            listOf(
                row(1, Paise.Zero, RenterStatus.Paid),
                row(2, Paise(-200_000), RenterStatus.Paid),
                row(3, 3_000L.rupees, RenterStatus.Unpaid),
            ),
            todos = emptyList(),
        )

        assertEquals(3_000L.rupees, result.total)
        assertEquals(1, result.owing)
    }

    @Test
    fun countsTheRentersStillToBillAndTheirRentButNotAnythingElse() {
        val result = StillToCollect.of(
            listOf(row(1, Paise.Zero, RenterStatus.NoBillYet)),
            todos = listOf(todo(1, 22_000L.rupees), todo(2, 7_500L.rupees)),
        )

        assertEquals(2, result.notBilled)
        assertEquals(29_500L.rupees, result.notBilledRent)
        assertEquals(Paise.Zero, result.total)
        assertEquals(0, result.owing)
    }

    @Test
    fun nothingAnywhereIsAllZero() {
        assertEquals(StillToCollect(Paise.Zero, Paise.Zero, 0, 0, Paise.Zero), StillToCollect.of(emptyList(), emptyList()))
    }
}
