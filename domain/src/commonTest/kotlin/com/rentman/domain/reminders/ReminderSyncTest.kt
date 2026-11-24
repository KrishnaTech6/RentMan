package com.rentman.domain.reminders

import com.rentman.domain.TestData
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.FakeBillRepository
import com.rentman.domain.billing.FakeSettingsRepository
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.FakePaymentRepository
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.tenancy.FakeRenterRepository
import com.rentman.domain.tenancy.RenterId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import com.rentman.domain.settings.StepReminderTimeUseCase
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderSyncTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-12-10T10:00:00Z")
    }
    private val november = BillingMonth.of(2026, 11)
    private val bill = Bill(
        renterId = RenterId(1),
        month = november,
        dueOn = LocalDate(2026, 12, 5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", 8_380L.rupees)),
    )

    private fun ReminderPlan.rentPending() = items.filter { it.reminder is Reminder.RentPending }

    private fun plan(
        renters: FakeRenterRepository = FakeRenterRepository(listOf(TestData.renter())),
        bills: FakeBillRepository = FakeBillRepository(listOf(bill)),
        payments: FakePaymentRepository = FakePaymentRepository(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
    ) = ObserveReminderPlanUseCase(renters, bills, payments, settings, clock, TimeZone.UTC)

    private fun sync(plan: ObserveReminderPlanUseCase, scheduler: FakeReminderScheduler, scope: TestScope) =
        ReminderSync(plan, scheduler, scope.backgroundScope).also { it.start() }

    @Test
    fun theFirstPlanIsScheduledAsSoonAsSyncStarts() = runTest(UnconfinedTestDispatcher()) {
        val scheduler = FakeReminderScheduler()

        sync(plan(), scheduler, this)

        assertEquals(1, scheduler.synced.size)
        assertEquals(5, scheduler.synced.single().rentPending().size)
    }

    @Test
    fun payingInFullCancelsTheRentRemindersOnTheNextSync() = runTest(UnconfinedTestDispatcher()) {
        val payments = FakePaymentRepository()
        val scheduler = FakeReminderScheduler()
        sync(plan(payments = payments), scheduler, this)

        payments.add(Payment(renterId = RenterId(1), amount = 8_380L.rupees, paidOn = LocalDate(2026, 12, 9), mode = PaymentMode.Cash))

        assertEquals(2, scheduler.synced.size)
        assertEquals(emptyList(), scheduler.synced.last().rentPending())
    }

    @Test
    fun aChangeThatLeavesThePlanTheSameDoesNotScheduleAgain() = runTest(UnconfinedTestDispatcher()) {
        val settings = FakeSettingsRepository()
        val scheduler = FakeReminderScheduler()
        sync(plan(settings = settings), scheduler, this)

        settings.save(settings.current.copy(globalRate = 950L.rupees)) // the rate has nothing to do with reminders

        assertEquals(1, scheduler.synced.size)
    }

    @Test
    fun changingTheReminderTimeSchedulesTheNotificationsAgainAtTheNewTime() = runTest(UnconfinedTestDispatcher()) {
        val settings = FakeSettingsRepository() // 9:00
        val scheduler = FakeReminderScheduler()
        sync(plan(settings = settings), scheduler, this)
        assertEquals(setOf(9), scheduler.synced.last().rentPending().map { it.at.toLocalDateTime(TimeZone.UTC).hour }.toSet())

        StepReminderTimeUseCase(settings)(60) // the Settings screen saves the new time at once

        assertEquals(2, scheduler.synced.size)
        assertEquals(setOf(10), scheduler.synced.last().rentPending().map { it.at.toLocalDateTime(TimeZone.UTC).hour }.toSet())
    }

    @Test
    fun mutingARenterRemovesTheirRemindersOnTheNextSync() = runTest(UnconfinedTestDispatcher()) {
        val renters = FakeRenterRepository(listOf(TestData.renter()))
        val scheduler = FakeReminderScheduler()
        sync(plan(renters = renters), scheduler, this)

        renters.setRemindersMutedMonth(RenterId(1), november)

        assertEquals(emptyList(), scheduler.synced.last().rentPending())
    }

    @Test
    fun resyncSchedulesTheCurrentPlanAgainEvenThoughNothingChanged() = runTest(UnconfinedTestDispatcher()) {
        // e.g. on iOS, where requests made before the person allowed notifications were dropped.
        val scheduler = FakeReminderScheduler()
        val sync = sync(plan(), scheduler, this)

        sync.resync()

        assertEquals(2, scheduler.synced.size)
        assertEquals(scheduler.synced[0], scheduler.synced[1])
    }

    @Test
    fun aSchedulerThatFailsDoesNotStopLaterChangesFromBeingScheduled() = runTest(UnconfinedTestDispatcher()) {
        val payments = FakePaymentRepository()
        val scheduler = FakeReminderScheduler(fail = true)
        sync(plan(payments = payments), scheduler, this)
        assertEquals(1, scheduler.synced.size) // tried, and it failed

        scheduler.fail = false
        payments.add(Payment(renterId = RenterId(1), amount = 8_380L.rupees, paidOn = LocalDate(2026, 12, 9), mode = PaymentMode.Cash))

        assertEquals(2, scheduler.synced.size) // the collector is still alive and tries again with the new plan
        assertEquals(emptyList(), scheduler.synced.last().rentPending())
    }

    @Test
    fun aFailureReadingTheDataKeepsWhatIsScheduled() = runTest(UnconfinedTestDispatcher()) {
        val scheduler = FakeReminderScheduler()

        sync(plan(renters = FakeRenterRepository(listOf(TestData.renter()), failReads = true)), scheduler, this)

        assertEquals(emptyList(), scheduler.synced) // nothing replaced, so earlier reminders stay
    }
}
