package com.rentman.domain.billing

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.flatMap
import com.rentman.domain.tenancy.RenterRepository
import kotlinx.coroutines.flow.first

/**
 * Takes a wrong bill out of the renter's history so it can be made again (ADR 0009). Only the renter's newest bill, and only while
 * they live there: each bill's previous reading is the one before it, so an older bill can't go without breaking the chain, and a
 * renter who left must not get an unbilled month back. Payments are not touched.
 */
class RemoveBillUseCase(private val bills: BillRepository, private val renters: RenterRepository) {
    suspend operator fun invoke(bill: Bill): Outcome<Unit> =
        bills.observeAll().first().flatMap { all ->
            val stored = all.firstOrNull { it.id == bill.id } ?: return@flatMap Outcome.Failure(DomainError.NotFound)
            renters.observeAll().first().flatMap { allRenters ->
                val renter = allRenters.firstOrNull { it.id == stored.renterId }
                if (renter?.active == true && stored.isNewestOf(all)) bills.remove(stored.id) else Outcome.Failure(DomainError.BillNotRemovable)
            }
        }
}

/** Undo of [RemoveBillUseCase]: the same bill, with the same id and lines. */
class RestoreBillUseCase(private val bills: BillRepository) {
    suspend operator fun invoke(bill: Bill): Outcome<Unit> = bills.restore(bill)
}

/** True if no other bill of the same renter is for a later month. */
internal fun Bill.isNewestOf(all: List<Bill>): Boolean = all.none { it.renterId == renterId && it.month > month }
