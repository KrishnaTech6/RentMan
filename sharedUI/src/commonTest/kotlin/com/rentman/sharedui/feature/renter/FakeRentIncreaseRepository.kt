package com.rentman.sharedui.feature.renter

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.tenancy.RentChange
import com.rentman.domain.tenancy.RentChangeId
import com.rentman.domain.tenancy.RentIncreaseRepository
import com.rentman.domain.tenancy.RenterId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Test fake that does what the real transaction does: a raise also changes the renter's rent (only if it is still the old
 * one), and Undo changes it back.
 * [failWrites] makes every write fail.
 */
internal class FakeRentIncreaseRepository(
    private val renters: FakeRenterRepository,
    var failWrites: Boolean = false,
) : RentIncreaseRepository {
    private val changes = MutableStateFlow<List<RentChange>>(emptyList())
    val active: List<RentChange> get() = changes.value

    override fun observeAll(): Flow<Outcome<List<RentChange>>> = changes.map { Outcome.Success(it) }

    override suspend fun apply(change: RentChange): Outcome<RentChange> {
        if (failWrites) return Outcome.Failure(DomainError.Storage("disk"))
        // Like the real transaction: only if the rent is still the old one.
        if (renters.saved.none { it.id == change.renterId && it.monthlyRent == change.oldRent }) {
            return Outcome.Failure(DomainError.OfferOutdated)
        }
        val stored = change.copy(id = RentChangeId(changes.value.size + 1L))
        renters.change(change.renterId) { it.copy(monthlyRent = change.newRent, raiseSnoozedUntil = null) }
        changes.value += stored
        return Outcome.Success(stored)
    }

    override suspend fun revert(change: RentChange): Outcome<Unit> {
        if (failWrites) return Outcome.Failure(DomainError.Storage("disk"))
        renters.change(change.renterId) { it.copy(monthlyRent = change.oldRent) }
        changes.value -= change
        return Outcome.Success(Unit)
    }

    override suspend fun postpone(renterId: RenterId, until: BillingMonth): Outcome<Unit> {
        if (failWrites) return Outcome.Failure(DomainError.Storage("disk"))
        renters.change(renterId) { it.copy(raiseSnoozedUntil = until) }
        return Outcome.Success(Unit)
    }
}
