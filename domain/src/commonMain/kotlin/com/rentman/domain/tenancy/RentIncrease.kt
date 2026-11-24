package com.rentman.domain.tenancy

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillRepository
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.flatMap
import com.rentman.domain.map
import com.rentman.domain.money.Paise
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import kotlin.jvm.JvmInline

@JvmInline
value class RentChangeId(val value: Long) {
    companion object {
        /** The id of a change that hasn't been saved yet. */
        val None = RentChangeId(0)
    }
}

/** A rent increase the landlord said yes to. The renter's rent from [effectiveMonth] on is [newRent]. See GLOSSARY.md. */
data class RentChange(
    val id: RentChangeId = RentChangeId.None,
    val renterId: RenterId,
    val oldRent: Paise,
    val newRent: Paise,
    val effectiveMonth: BillingMonth,
    val decidedOn: LocalDate,
)

/** A rent increase that is due and waiting for a yes or "not now". Shown on Home as "Raise rent 10%?". */
data class RentIncreaseOffer(
    val summary: RenterSummary,
    val percent: Int,
    val newRent: Paise,
    /** The month of the renter's next bill: the raise applies from there. */
    val effectiveMonth: BillingMonth,
) {
    val currentRent: Paise get() = summary.renter.monthlyRent
}

interface RentIncreaseRepository {
    /** Every raise that wasn't undone. Emits again whenever they change. */
    fun observeAll(): Flow<Outcome<List<RentChange>>>

    /**
     * In one transaction: stores [change], sets the renter's rent to its new rent and clears "not now".
     * Returns the change with its real id.
     */
    suspend fun apply(change: RentChange): Outcome<RentChange>

    /** Hides [change] and puts the renter's old rent back. Undo, so the row stays. */
    suspend fun revert(change: RentChange): Outcome<Unit>

    /** "Not now": the offer is hidden until [until]. */
    suspend fun postpone(renterId: RenterId, until: BillingMonth): Outcome<Unit>
}

/** For each renter who is due a raise, what it would be and from which bill it would apply. */
class ObserveRentIncreaseOffersUseCase(
    private val renters: RenterRepository,
    private val addresses: AddressRepository,
    private val rentChanges: RentIncreaseRepository,
    private val bills: BillRepository,
    private val today: GetTodayUseCase,
) {
    operator fun invoke(): Flow<Outcome<List<RentIncreaseOffer>>> =
        combine(renters.observeActive(), addresses.observeAll(), rentChanges.observeAll(), bills.observeAll()) { r, a, c, b ->
            r.flatMap { renterList ->
                a.flatMap { addressList ->
                    c.flatMap { changeList ->
                        b.map { billList ->
                            val current = BillingMonth.of(today())
                            renterList.mapNotNull { renter ->
                                if (renter.raisePercent <= 0) return@mapNotNull null
                                val address = addressList.firstOrNull { it.id == renter.addressId } ?: return@mapNotNull null
                                // Counted from the agreement start, or from the latest raise.
                                val countFrom = changeList.filter { it.renterId == renter.id }.maxOfOrNull { it.effectiveMonth }
                                    ?: BillingMonth.of(renter.agreementStart)
                                val raiseMonth = RentIncreasePolicy.nextRaise(countFrom, renter.raiseEveryMonths)
                                if (!RentIncreasePolicy.shouldAsk(raiseMonth, current, renter.raiseSnoozedUntil)) return@mapNotNull null
                                val billed = billList.filter { it.renterId == renter.id }.map { it.month }.toSet()
                                RentIncreaseOffer(
                                    summary = RenterSummary(renter, address),
                                    percent = renter.raisePercent,
                                    newRent = RentIncreasePolicy.raised(renter.monthlyRent, renter.raisePercent),
                                    effectiveMonth = generateSequence(renter.firstBillMonth) { it.plusMonths(1) }.first { it !in billed },
                                )
                            }.sortedBy { it.summary.renter.name }
                        }
                    }
                }
            }
        }
}

class ApplyRentIncreaseUseCase(
    private val rentChanges: RentIncreaseRepository,
    private val today: GetTodayUseCase,
) {
    suspend operator fun invoke(offer: RentIncreaseOffer): Outcome<RentChange> = rentChanges.apply(
        RentChange(
            renterId = offer.summary.renter.id,
            oldRent = offer.currentRent,
            newRent = offer.newRent,
            effectiveMonth = offer.effectiveMonth,
            decidedOn = today(),
        ),
    )
}

/** "Not now": the card comes back next month. */
class PostponeRentIncreaseUseCase(
    private val rentChanges: RentIncreaseRepository,
    private val today: GetTodayUseCase,
) {
    suspend operator fun invoke(renterId: RenterId): Outcome<Unit> =
        rentChanges.postpone(renterId, until = BillingMonth.of(today()).plusMonths(1))
}

class UndoRentIncreaseUseCase(private val rentChanges: RentIncreaseRepository) {
    suspend operator fun invoke(change: RentChange): Outcome<Unit> = rentChanges.revert(change)
}
