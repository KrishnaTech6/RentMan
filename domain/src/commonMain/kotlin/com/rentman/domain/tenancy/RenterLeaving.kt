package com.rentman.domain.tenancy

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillRepository
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.UnbilledMonths
import com.rentman.domain.billing.PendingCalculator
import com.rentman.domain.flatMap
import com.rentman.domain.map
import com.rentman.domain.money.Paise
import com.rentman.domain.payments.PaymentRepository
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * The renter leaves: they are marked as left today, and their bills and payments stay. They can only leave once every month
 * up to now has a bill and they owe nothing, because someone who has left no longer counts in "still to collect" or in the
 * bill to-dos, so a month never billed or an amount never paid would be forgotten.
 */
class MarkRenterLeftUseCase(
    private val renters: RenterRepository,
    private val bills: BillRepository,
    private val payments: PaymentRepository,
    private val today: GetTodayUseCase,
) {
    suspend operator fun invoke(renterId: RenterId): Outcome<Unit> =
        renters.observeAll().first().flatMap { allRenters ->
            val renter = allRenters.firstOrNull { it.id == renterId } ?: return@flatMap Outcome.Failure(DomainError.NotFound)
            bills.observeAll().first().flatMap { allBills ->
                payments.observeAll().first().flatMap { allPayments ->
                    val renterBills = allBills.filter { it.renterId == renterId }
                    val unbilled = UnbilledMonths.first(renter.firstBillMonth, renterBills, BillingMonth.of(today()))
                    val owed = PendingCalculator.pending(
                        renterBills.map { it.total },
                        allPayments.filter { it.renterId == renterId }.map { it.amount },
                    )
                    when {
                        unbilled != null -> Outcome.Failure(DomainError.UnbilledMonth(unbilled))
                        owed > Paise.Zero -> Outcome.Failure(DomainError.StillOwes(owed))
                        else -> renters.setLeft(renterId, today())
                    }
                }
            }
        }
}

/** The renter lives there again. The unit rule applies as when adding: with others at the address they need a different unit. */
class MoveRenterBackUseCase(private val renters: RenterRepository) {
    suspend operator fun invoke(renterId: RenterId): Outcome<Unit> =
        renters.observeAll().first().flatMap { all ->
            val renter = all.firstOrNull { it.id == renterId } ?: return@flatMap Outcome.Failure(DomainError.NotFound)
            val neighbours = all.filter { it.active && it.id != renterId && it.addressId == renter.addressId }
            if (RenterUnits.problem(renter.unit, neighbours) != null) Outcome.Failure(DomainError.UnitTaken) else renters.setLeft(renterId, null)
        }
}

/** The renters who have left, so they can still be opened to see their history or to move them back. */
class ObserveLeftRentersUseCase(
    private val renters: RenterRepository,
    private val addresses: AddressRepository,
) {
    operator fun invoke(): Flow<Outcome<List<RenterSummary>>> =
        combine(renters.observeAll(), addresses.observeAll()) { renterOutcome, addressOutcome ->
            renterOutcome.flatMap { renterList ->
                addressOutcome.map { addressList ->
                    renterList.filter { !it.active }.mapNotNull { renter ->
                        addressList.firstOrNull { it.id == renter.addressId }?.let { RenterSummary(renter, it) }
                    }
                }
            }
        }
}
