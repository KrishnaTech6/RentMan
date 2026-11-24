package com.rentman.domain.reminders

import com.rentman.domain.TestData
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.settings.AppSettings
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ReminderPlannerTest {
    private val november = BillingMonth.of(2026, 11)

    /** The November bill, due on 5 Dec. */
    private fun bill(renterId: Long = 1, month: BillingMonth = november, total: Paise = 8_380L.rupees) = Bill(
        renterId = RenterId(renterId),
        month = month,
        dueOn = month.plusMonths(1).dueOn(5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", total)),
    )

    private fun plan(
        renters: List<Renter> = listOf(TestData.renter()),
        bills: List<Bill> = listOf(bill()),
        payments: List<Payment> = emptyList(),
        repeatDays: Int = 3,
        minuteOfDay: Int = 540,
        now: String = "2026-12-10T10:00:00Z",
        zone: TimeZone = TimeZone.UTC,
    ) = ReminderPlanner.plan(
        renters, bills, payments,
        AppSettings(reminderMinuteOfDay = minuteOfDay, reminderRepeatDays = repeatDays),
        Instant.parse(now), zone,
    )

    private fun ReminderPlan.rentPending() = items.filter { it.reminder is Reminder.RentPending }
    private fun ReminderPlan.pending() = items.filter { it.reminder is Reminder.RentPending }.map { it.at.toString() }
    private fun ReminderPlan.createBills() = items.filter { it.reminder is Reminder.CreateBills }.map { it.at.toString() }

    @Test
    fun aLateRenterIsRemindedEveryThreeDaysFromTheDayAfterTheDueDate() {
        // Due 5 Dec, so the cadence is 6, 9, 12, 15 ... Dec at 9:00. It is 10 Dec, 10:00 now.
        assertEquals(
            listOf("2026-12-12T09:00:00Z", "2026-12-15T09:00:00Z", "2026-12-18T09:00:00Z", "2026-12-21T09:00:00Z", "2026-12-24T09:00:00Z"),
            plan().pending(),
        )
    }

    @Test
    fun everyTwoDaysKeepsTheCadenceFromTheFirstReminderNotFromToday() {
        // 6, 8, 10 (9:00 has already passed), 12, 14 ...
        assertEquals(
            listOf("2026-12-12T09:00:00Z", "2026-12-14T09:00:00Z", "2026-12-16T09:00:00Z", "2026-12-18T09:00:00Z", "2026-12-20T09:00:00Z"),
            plan(repeatDays = 2).pending(),
        )
    }

    @Test
    fun onceMeansOnlyTheFirstReminderAndNothingAfterItHasPassed() {
        assertEquals(listOf("2026-12-06T09:00:00Z"), plan(repeatDays = 0, now = "2026-12-05T10:00:00Z").pending())
        assertEquals(emptyList(), plan(repeatDays = 0, now = "2026-12-10T10:00:00Z").pending())
    }

    @Test
    fun thePendingAmountAndNameTravelWithTheReminder() {
        val reminder = plan().rentPending().first().reminder

        assertEquals(Reminder.RentPending(RenterId(1), "Ravi Kumar", 8_380L.rupees), reminder)
    }

    @Test
    fun aPartPaymentLowersTheAmountAndPayingInFullStopsTheReminders() {
        val part = Payment(PaymentId(1), RenterId(1), 5_000L.rupees, LocalDate(2026, 12, 8), PaymentMode.Upi)
        val full = part.copy(amount = 8_380L.rupees)

        assertEquals(3_380L.rupees, (plan(payments = listOf(part)).rentPending().first().reminder as Reminder.RentPending).pending)
        assertEquals(emptyList(), plan(payments = listOf(full)).rentPending())
        assertEquals(emptyList(), plan(payments = listOf(full.copy(amount = 9_000L.rupees))).rentPending()) // paid ahead
    }

    @Test
    fun aMutedRenterGetsNothingUntilANewerBillExists() {
        val muted = TestData.renter(remindersMutedMonth = november)
        assertEquals(emptyList(), plan(renters = listOf(muted)).pending())

        // December's bill is newer than the month muted, so reminders resume.
        val withDecember = listOf(bill(), bill(month = BillingMonth.of(2026, 12), total = 5_000L.rupees))
        assertEquals(true, plan(renters = listOf(muted), bills = withDecember, now = "2027-01-10T10:00:00Z").pending().isNotEmpty())
    }

    @Test
    fun aRenterWithoutABillOwesNothingSoThereIsNoReminder() {
        assertEquals(emptyList(), plan(bills = emptyList(), now = "2026-12-02T10:00:00Z").pending())
    }

    @Test
    fun otherRentersAreIndependent() {
        val renters = listOf(TestData.renter(id = 1), TestData.renter(id = 2, name = "Neha", remindersMutedMonth = november))
        val bills = listOf(bill(renterId = 1), bill(renterId = 2))

        val names = plan(renters = renters, bills = bills).rentPending().map { (it.reminder as Reminder.RentPending).renterName }.toSet()

        assertEquals(setOf("Ravi Kumar"), names)
    }

    @Test
    fun theMonthlyPromptRepeatsInsideTheMonthUntilEveryRenterHasABill() {
        // It is 24 Nov and November has no bill yet: 1, 4, ... 25, 28 are the future slots, and none spill into December.
        val plan = plan(bills = emptyList(), now = "2026-11-24T10:00:00Z")

        assertEquals(listOf("2026-11-25T09:00:00Z", "2026-11-28T09:00:00Z"), plan.createBills())
        assertEquals(Reminder.CreateBills(november, missing = 1), plan.items.first().reminder)
    }

    @Test
    fun theMonthlyPromptCountsHowManyRentersStillNeedABillAndStopsWhenNoneDo() {
        val renters = listOf(TestData.renter(id = 1), TestData.renter(id = 2, name = "Neha"), TestData.renter(id = 3, name = "Zoya"))
        val oneBilled = plan(renters = renters, bills = listOf(bill(renterId = 1)), now = "2026-11-24T10:00:00Z")
        val allBilled = plan(renters = renters, bills = renters.map { bill(it.id.value) }, now = "2026-11-24T10:00:00Z")

        assertEquals(Reminder.CreateBills(november, missing = 2), oneBilled.items.first().reminder)
        assertEquals(emptyList(), allBilled.createBills())
    }

    @Test
    fun theMonthlyPromptIsOnceWhenRepeatIsOnce() {
        assertEquals(emptyList(), plan(bills = emptyList(), repeatDays = 0, now = "2026-11-24T10:00:00Z").createBills()) // 1 Nov has passed
        assertEquals(listOf("2026-11-01T09:00:00Z"), plan(bills = emptyList(), repeatDays = 0, now = "2026-11-01T08:00:00Z").createBills())
    }

    @Test
    fun theTimeOfDayIsInTheLandlordsTimeZone() {
        val kolkata = TimeZone.of("Asia/Kolkata") // UTC+5:30, so 9:00 there is 03:30 UTC

        assertEquals("2026-12-12T03:30:00Z", plan(zone = kolkata).pending().first())
    }

    @Test
    fun theReminderTimeFromSettingsIsUsed() {
        assertEquals("2026-12-12T18:30:00Z", plan(minuteOfDay = 18 * 60 + 30).pending().first())
    }

    @Test
    fun theWholePlanIsSortedByTimeAndCappedSoIosStaysUnderItsLimit() {
        val renters = (1L..30L).map { TestData.renter(id = it, name = "Renter $it") }
        val bills = renters.map { bill(it.id.value) }

        val items = plan(renters = renters, bills = bills).items

        assertEquals(ReminderPlanner.MAX_SCHEDULED, items.size)
        assertEquals(items.sortedBy { it.at }, items)
        assertEquals(items.size, items.map { it.id }.toSet().size) // ids are unique
    }

    @Test
    fun theSameDataGivesTheSamePlanAndStableIds() {
        assertEquals(plan(), plan())
        assertEquals("pending-1-2026-12-12", plan().rentPending().first().id)
        assertEquals("create-bills-202611-2026-11-25", plan(bills = emptyList(), now = "2026-11-24T10:00:00Z").items.first().id)
    }
}
