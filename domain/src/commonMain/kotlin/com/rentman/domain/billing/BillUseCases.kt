package com.rentman.domain.billing

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.flatMap
import com.rentman.domain.fold
import com.rentman.domain.map
import com.rentman.domain.money.Paise
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentRepository
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.daysUntil
import com.rentman.domain.settings.SettingsRepository
import com.rentman.domain.tenancy.AddressRepository
import com.rentman.domain.tenancy.ObserveRenterSummariesUseCase
import com.rentman.domain.tenancy.RentHistory
import com.rentman.domain.tenancy.RentIncreaseRepository
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterRepository
import com.rentman.domain.tenancy.RentIncreaseOffer
import com.rentman.domain.tenancy.ObserveLeftRentersUseCase
import com.rentman.domain.tenancy.ObserveRentIncreaseOffersUseCase
import com.rentman.domain.tenancy.RenterSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/** Which months of a renter still have no bill. Used for the to-do list and to stop a renter leaving with a month unbilled. */
object UnbilledMonths {
    /** The landlord makes a bill about this many days before its due day. */
    private const val DAYS_AHEAD = 3

    /** The first day [month]'s bill can be made: the 1st, or [DAYS_AHEAD] days before the due day if that is earlier (due on the 1st: the 28th before). */
    fun opensOn(month: BillingMonth, dueDay: Int): LocalDate =
        minOf(month.atStartOfMonth(), month.dueOn(dueDay).minus(DAYS_AHEAD, DateTimeUnit.DAY))

    /** The latest month that can be billed today: this month, or next month once it has opened. */
    fun billable(today: LocalDate, dueDay: Int): BillingMonth {
        val next = BillingMonth.of(today).plusMonths(1)
        return if (today >= opensOn(next, dueDay)) next else BillingMonth.of(today)
    }

    /** The oldest month from [firstBillMonth] up to [current] that none of the renter's [bills] covers, or null. */
    fun first(firstBillMonth: BillingMonth, bills: List<Bill>, current: BillingMonth): BillingMonth? {
        val billed = bills.map { it.month }.toSet()
        return generateSequence(firstBillMonth) { it.plusMonths(1) }.takeWhile { it <= current }.firstOrNull { it !in billed }
    }
}

/** A renter whose bill for [month] hasn't been created yet. Shown on Home as "Create <month> bill". */
data class BillTodo(val summary: RenterSummary, val month: BillingMonth)

/** For each renter, the oldest month since they started being billed that still has no bill. */
class ObserveBillTodosUseCase(
    private val renters: RenterRepository,
    private val addresses: AddressRepository,
    private val bills: BillRepository,
    private val today: GetTodayUseCase,
) {
    operator fun invoke(): Flow<Outcome<List<BillTodo>>> =
        combine(renters.observeActive(), addresses.observeAll(), bills.observeAll()) { renterOutcome, addressOutcome, billOutcome ->
            renterOutcome.flatMap { renterList ->
                addressOutcome.flatMap { addressList ->
                    billOutcome.map { billList ->
                        val day = today()
                        // shortcut: a renter whose address is archived gets no to-do. Archiving isn't a feature yet.
                        renterList.mapNotNull { renter ->
                            val address = addressList.firstOrNull { it.id == renter.addressId } ?: return@mapNotNull null
                            UnbilledMonths.first(renter.firstBillMonth, billList.filter { it.renterId == renter.id }, UnbilledMonths.billable(day, renter.dueDay))
                                ?.let { BillTodo(RenterSummary(renter, address), it) }
                        }.sortedWith(compareBy({ it.month.value }, { it.summary.renter.name }))
                    }
                }
            }
        }
}

/** Gathers what the Create bill screen needs about one renter and one month. */
class ObserveBillContextUseCase(
    private val renters: RenterRepository,
    private val addresses: AddressRepository,
    private val bills: BillRepository,
    private val settings: SettingsRepository,
    private val payments: PaymentRepository,
    private val rentChanges: RentIncreaseRepository,
) {
    private companion object {
        const val USUAL_USAGE_BILLS = 3
    }

    /** The context for [month], with the rent that applied in that month (a raise accepted later doesn't change an earlier month). */
    operator fun invoke(renterId: RenterId, month: BillingMonth): Flow<Outcome<BillContext>> =
        combine(context(renterId, month), rentChanges.observeAll()) { context, changes ->
            context.flatMap { c ->
                changes.map { all ->
                    c.copy(rent = RentHistory.rentFor(month, c.summary.renter.monthlyRent, all.filter { it.renterId == renterId }))
                }
            }
        }

    private fun context(renterId: RenterId, month: BillingMonth): Flow<Outcome<BillContext>> =
        combine(
            renters.observeActive(),
            addresses.observeAll(),
            bills.observeAll(),
            settings.observe(),
            payments.observeAll(),
        ) { r, a, b, s, p ->
            r.flatMap { renterList ->
                a.flatMap { addressList ->
                    b.flatMap { billList ->
                        s.flatMap { appSettings -> p.map { paymentList ->
                            val renter = renterList.firstOrNull { it.id == renterId }
                            val address = addressList.firstOrNull { it.id == renter?.addressId }
                            if (renter == null || address == null) return@map null
                            val earlier = billList.filter { it.renterId == renterId && it.month < month }
                            BillContext(
                                summary = RenterSummary(renter, address),
                                month = month,
                                // The previous bill's reading. If that bill has none (electricity wasn't billed then), the starting
                                // reading is the meter as the landlord entered it when billing it again, not an older bill's.
                                previousReading = renter.startingReading.takeIf { renter.billsElectricity }?.let { start ->
                                    earlier.maxByOrNull { it.month }?.currentReading ?: start
                                },
                                rate = renter.customRate ?: appSettings.globalRate,
                                pendingBefore = PendingCalculator.pending(
                                    billTotals = earlier.map { it.total },
                                    payments = paymentList.filter { it.renterId == renterId }.map { it.amount },
                                ),
                                usualUnits = earlier.sortedByDescending { it.month }
                                    .take(USUAL_USAGE_BILLS)
                                    .mapNotNull { it.units }
                                    .takeIf { it.isNotEmpty() }
                                    ?.average()
                                    ?.roundToInt(),
                            )
                        } }
                    }
                }
            }.flatMap { context -> context?.let { Outcome.Success(it) } ?: Outcome.Failure(DomainError.NotFound) }
        }
}

class CreateBillUseCase(private val bills: BillRepository) {
    suspend operator fun invoke(bill: Bill): Outcome<Bill> =
        bills.observeAll().first().fold(
            onSuccess = { existing ->
                if (existing.any { it.renterId == bill.renterId && it.month == bill.month }) {
                    Outcome.Failure(DomainError.BillExists)
                } else {
                    bills.add(bill)
                }
            },
            onFailure = { Outcome.Failure(it) },
        )
}

/** [Late] means money is owed and the latest bill is past its due date, whether or not some was paid. */
enum class RenterStatus { NoBillYet, Paid, PartPaid, Unpaid, Late }

/**
 * One line of Home's renter list: who, what they owe right now (negative means paid ahead), where they stand, and how much of
 * what they owe is [overdue] (from bills past their due day, whatever the newest bill says).
 * [paidSinceBill] is what they paid since the newest bill came, so "paid of paid + pending" is a share that never passes 100%
 * even when an older bill is still owed. [billMonth] and [dueOn] are the newest bill's month and due day (a month's bill falls due in the
 * next month), and [daysLate] counts from it, only when [status] is Late.
 */
data class RenterRow(
    val summary: RenterSummary,
    val pending: Paise,
    val status: RenterStatus,
    val overdue: Paise,
    val paidSinceBill: Paise = Paise.Zero,
    val billMonth: BillingMonth? = null,
    val dueOn: LocalDate? = null,
    val daysLate: Int = 0,
) {
    /** How much of what was owed when the newest bill came has been paid since, from 0 to 1. */
    val paidShare: Float
        get() {
            val owed = paidSinceBill + pending
            return if (pending <= Paise.Zero || owed <= Paise.Zero) 0f else paidSinceBill.value.toFloat() / owed.value
        }
}

/** What Home shows: every renter with their balance, the bills to create, the rent raises to decide, and how this month is going. */
data class HomeSnapshot(
    val renters: List<RenterRow>,
    val todos: List<BillTodo>,
    val raises: List<RentIncreaseOffer>,
    /** Renters who have left. They are not in the list, the to-dos or the total. */
    val left: List<RenterSummary>,
    val thisMonth: MonthSummary,
)

class ObserveHomeUseCase(
    private val summaries: ObserveRenterSummariesUseCase,
    private val todos: ObserveBillTodosUseCase,
    private val raises: ObserveRentIncreaseOffersUseCase,
    private val leftRenters: ObserveLeftRentersUseCase,
    private val bills: BillRepository,
    private val payments: PaymentRepository,
    private val today: GetTodayUseCase,
) {
    private class Lists(
        val renters: List<RenterSummary>,
        val todos: List<BillTodo>,
        val raises: List<RentIncreaseOffer>,
        val left: List<RenterSummary>,
    )

    operator fun invoke(): Flow<Outcome<HomeSnapshot>> {
        val lists = combine(summaries(), todos(), raises(), leftRenters()) { renterOutcome, todoOutcome, raiseOutcome, leftOutcome ->
            renterOutcome.flatMap { renters ->
                todoOutcome.flatMap { todoList ->
                    raiseOutcome.flatMap { raiseList -> leftOutcome.map { leftList -> Lists(renters, todoList, raiseList, leftList) } }
                }
            }
        }
        val money = combine(bills.observeAll(), payments.observeAll()) { billOutcome, paymentOutcome ->
            billOutcome.flatMap { billList -> paymentOutcome.map { paymentList -> billList to paymentList } }
        }
        return combine(lists, money) { listsOutcome, moneyOutcome ->
            listsOutcome.flatMap { l ->
                moneyOutcome.map { (billList, paymentList) ->
                    val rows = l.renters.map { rowOf(it, billList, paymentList, today()) }.sortedWith(mostUrgentFirst)
                    HomeSnapshot(
                        renters = rows,
                        todos = l.todos,
                        raises = l.raises,
                        left = l.left,
                        thisMonth = MonthSummary.of(BillingMonth.of(today()), today(), l.renters.map { it.renter }, billList, paymentList),
                    )
                }
            }
        }
    }

    private fun rowOf(summary: RenterSummary, allBills: List<Bill>, allPayments: List<Payment>, today: LocalDate): RenterRow {
        val renterId = summary.renter.id
        val renterBills = allBills.filter { it.renterId == renterId }
        val renterPayments = allPayments.filter { it.renterId == renterId }
        val pending = PendingCalculator.pending(renterBills.map { it.total }, renterPayments.map { it.amount })
        val latestBill = renterBills.maxByOrNull { it.month }
        val status = when {
            latestBill == null -> RenterStatus.NoBillYet
            pending <= Paise.Zero -> RenterStatus.Paid
            today > latestBill.dueOn -> RenterStatus.Late
            renterPayments.any { it.paidOn >= latestBill.issuedOn } -> RenterStatus.PartPaid
            else -> RenterStatus.Unpaid
        }
        return RenterRow(
            summary = summary,
            pending = pending,
            status = status,
            overdue = PendingCalculator.overdue(renterBills, renterPayments.map { it.amount }, today),
            paidSinceBill = latestBill?.let { bill -> BillCalculator.total(renterPayments.filter { it.paidOn >= bill.issuedOn }.map { it.amount }) } ?: Paise.Zero,
            billMonth = latestBill?.month,
            dueOn = latestBill?.dueOn,
            daysLate = if (status == RenterStatus.Late && latestBill != null) latestBill.dueOn.daysUntil(today) else 0,
        )
    }

    private companion object {
        /** Who needs a call first: Late, then Part paid, Unpaid, No bill yet, Paid. Larger amounts first, then by name. */
        val mostUrgentFirst = compareBy<RenterRow> {
            when (it.status) {
                RenterStatus.Late -> 0
                RenterStatus.PartPaid -> 1
                RenterStatus.Unpaid -> 2
                RenterStatus.NoBillYet -> 3
                RenterStatus.Paid -> 4
            }
        }.thenByDescending { it.pending }.thenBy { it.summary.renter.name }
    }
}
