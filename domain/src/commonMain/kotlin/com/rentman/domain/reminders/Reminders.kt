package com.rentman.domain.reminders

import com.rentman.domain.Outcome
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillRepository
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.PendingCalculator
import com.rentman.domain.billing.UnbilledMonths
import com.rentman.domain.flatMap
import com.rentman.domain.map
import com.rentman.domain.money.Paise
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentRepository
import com.rentman.domain.settings.AppSettings
import com.rentman.domain.settings.SettingsRepository
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterRepository
import com.rentman.domain.tenancy.remindersMutedFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** What a notification is about. The platform turns it into text. */
sealed interface Reminder {
    /** [renterName] still has no bill for [month]. */
    data class CreateBill(val renterId: RenterId, val renterName: String, val month: BillingMonth) : Reminder

    /** [renterName] still owes [pending] after the due date. */
    data class RentPending(val renterId: RenterId, val renterName: String, val pending: Paise) : Reminder
}

/** One notification to show at [at]. [id] is the same for the same reminder every time, so scheduling is idempotent. */
data class ScheduledReminder(val id: String, val at: Instant, val reminder: Reminder)

/** Every notification that should exist right now, soonest first. */
data class ReminderPlan(val items: List<ScheduledReminder>)

/**
 * Replaces everything scheduled on the phone with [plan]. Safe to call again and again.
 * A problem with the phone's own scheduler is returned as a failure and never thrown, so it can't crash the app.
 */
interface ReminderScheduler {
    suspend fun sync(plan: ReminderPlan): Outcome<Unit>
}

/**
 * Decides which reminders exist. A notification can't check "is it paid yet?" when it fires, so this plans a few slots
 * ahead and is run again whenever the data changes. See ARCHITECTURE.md section 7.
 */
object ReminderPlanner {
    /** iOS keeps at most 64 pending notifications, so the plan never goes beyond this many. */
    const val MAX_SCHEDULED = 60
    private const val SLOTS_PER_REMINDER = 5

    fun plan(
        renters: List<Renter>,
        bills: List<Bill>,
        payments: List<Payment>,
        settings: AppSettings,
        now: Instant,
        zone: TimeZone,
    ): ReminderPlan {
        fun slots(first: LocalDate): List<Pair<LocalDate, Instant>> =
            generateSequence(first) { if (settings.reminderRepeatDays > 0) it.plus(settings.reminderRepeatDays, DateTimeUnit.DAY) else null }
                .map { it to it.atTime(settings.reminderMinuteOfDay / 60, settings.reminderMinuteOfDay % 60).toInstant(zone) }
                .filter { (_, at) -> at > now }
                .take(SLOTS_PER_REMINDER)
                .toList()

        // The oldest month with no bill, looking one month ahead: a renter due on the 1st is reminded at the end of the month before.
        val nextMonth = BillingMonth.of(now.toLocalDate(zone)).plusMonths(1)
        val createBill = renters.flatMap { renter ->
            val month = UnbilledMonths.first(renter.firstBillMonth, bills.filter { it.renterId == renter.id }, nextMonth)
                ?: return@flatMap emptyList()
            slots(UnbilledMonths.opensOn(month, renter.dueDay)).map { (date, at) ->
                ScheduledReminder("create-bill-${renter.id.value}-${month.value}-$date", at, Reminder.CreateBill(renter.id, renter.name, month))
            }
        }

        val pending = renters.flatMap { renter ->
            val renterBills = bills.filter { it.renterId == renter.id }
            val latest = renterBills.maxByOrNull { it.month } ?: return@flatMap emptyList()
            val owed = PendingCalculator.pending(renterBills.map { it.total }, payments.filter { it.renterId == renter.id }.map { it.amount })
            if (owed <= Paise.Zero || renter.remindersMutedFor(latest.month)) return@flatMap emptyList()
            slots(latest.dueOn.plus(1, DateTimeUnit.DAY)).map { (date, at) ->
                ScheduledReminder("pending-${renter.id.value}-$date", at, Reminder.RentPending(renter.id, renter.name, owed))
            }
        }

        return ReminderPlan((createBill + pending).sortedWith(compareBy({ it.at }, { it.id })).take(MAX_SCHEDULED))
    }

    private fun Instant.toLocalDate(zone: TimeZone): LocalDate = this.toLocalDateTime(zone).date
}

class ObserveReminderPlanUseCase(
    private val renters: RenterRepository,
    private val bills: BillRepository,
    private val payments: PaymentRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    operator fun invoke(): Flow<Outcome<ReminderPlan>> =
        combine(renters.observeActive(), bills.observeAll(), payments.observeAll(), settings.observe()) { r, b, p, s ->
            r.flatMap { renterList ->
                b.flatMap { billList ->
                    p.flatMap { paymentList ->
                        s.map { appSettings ->
                            ReminderPlanner.plan(renterList, billList, paymentList, appSettings, clock.now(), timeZone)
                        }
                    }
                }
            }
        }
}

/**
 * The one app-wide collector (ARCHITECTURE.md section 7): whenever the plan changes it hands it to the scheduler.
 * A failure reading the data keeps what is already scheduled, because it only reacts to a plan.
 * [resync] schedules the current plan again, for when the phone's permission changed rather than our data.
 */
class ReminderSync(
    private val plan: ObserveReminderPlanUseCase,
    private val scheduler: ReminderScheduler,
    private val scope: CoroutineScope,
) {
    private val resyncRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun start(): Job = combine(
        plan().mapNotNull { (it as? Outcome.Success)?.value }.distinctUntilChanged(),
        resyncRequests.onStart { emit(Unit) },
    ) { latest, _ -> latest }
        .onEach { scheduler.sync(it) }
        .launchIn(scope)

    fun resync() {
        resyncRequests.tryEmit(Unit)
    }
}
