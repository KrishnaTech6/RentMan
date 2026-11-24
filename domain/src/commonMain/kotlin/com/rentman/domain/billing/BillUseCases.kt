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
import kotlinx.datetime.LocalDate
import com.rentman.domain.settings.SettingsRepository
import com.rentman.domain.tenancy.AddressRepository
import com.rentman.domain.tenancy.ObserveRenterSummariesUseCase
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
                        val current = BillingMonth.of(today())
                        // shortcut: a renter whose address is archived gets no to-do. Archiving isn't a feature yet.
                        renterList.mapNotNull { renter ->
                            val address = addressList.firstOrNull { it.id == renter.addressId } ?: return@mapNotNull null
                            UnbilledMonths.first(renter.firstBillMonth, billList.filter { it.renterId == renter.id }, current)
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
) {
    private companion object {
        const val USUAL_USAGE_BILLS = 3
    }

    operator fun invoke(renterId: RenterId, month: BillingMonth): Flow<Outcome<BillContext>> =
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
 */
data class RenterRow(val summary: RenterSummary, val pending: Paise, val status: RenterStatus, val overdue: Paise)

/**
 * What is owed right now, from the renters who owe, split by whether it is [late] (from bills past their due day) or not due yet.
 * [late] and [notDueYet] add up to [total]. Someone who paid ahead doesn't reduce what the others owe. The [notBilled] renters have a bill to create, so they are not in the amounts;
 * [notBilledRent] is one month's rent for each, with no electricity because that isn't known before the reading.
 */
data class StillToCollect(val late: Paise, val notDueYet: Paise, val owing: Int, val notBilled: Int, val notBilledRent: Paise) {
    val total: Paise get() = late + notDueYet

    companion object {
        fun of(rows: List<RenterRow>, todos: List<BillTodo>): StillToCollect {
            val owed = rows.filter { it.pending > Paise.Zero }
            val late = BillCalculator.total(owed.map { it.overdue })
            return StillToCollect(
                late = late,
                notDueYet = BillCalculator.total(owed.map { it.pending }) - late,
                owing = owed.size,
                notBilled = todos.size,
                notBilledRent = BillCalculator.total(todos.map { it.summary.renter.monthlyRent }),
            )
        }
    }
}

/** What Home shows: every renter with their balance, the bills to create, the rent raises to decide, and the total still to collect. */
data class HomeSnapshot(
    val renters: List<RenterRow>,
    val todos: List<BillTodo>,
    val raises: List<RentIncreaseOffer>,
    /** Renters who have left. They are not in the list, the to-dos or the total. */
    val left: List<RenterSummary>,
    val stillToCollect: StillToCollect,
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
                    val rows = l.renters.map { rowOf(it, billList, paymentList, today()) }
                    HomeSnapshot(
                        renters = rows,
                        todos = l.todos,
                        raises = l.raises,
                        left = l.left,
                        stillToCollect = StillToCollect.of(rows, l.todos),
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
        return RenterRow(summary, pending, status, PendingCalculator.overdue(renterBills, renterPayments.map { it.amount }, today))
    }
}
