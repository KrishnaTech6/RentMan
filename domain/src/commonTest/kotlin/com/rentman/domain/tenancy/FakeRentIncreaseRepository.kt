package com.rentman.domain.tenancy

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillingMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory fake. It records what was asked. [failReads] and [failWrites] make reading or writing fail. */
class FakeRentIncreaseRepository(
    initial: List<RentChange> = emptyList(),
    private val failReads: Boolean = false,
    private val failWrites: Boolean = false,
) : RentIncreaseRepository {
    private val changes = MutableStateFlow(initial)
    val applied = mutableListOf<RentChange>()
    val reverted = mutableListOf<RentChange>()
    val postponed = mutableMapOf<RenterId, BillingMonth>()

    override fun observeAll(): Flow<Outcome<List<RentChange>>> = changes.map {
        if (failReads) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success(it)
    }

    override suspend fun apply(change: RentChange): Outcome<RentChange> {
        if (failWrites) return Outcome.Failure(DomainError.Storage("disk"))
        val stored = change.copy(id = RentChangeId(changes.value.size + 1L))
        changes.value += stored
        applied += stored
        return Outcome.Success(stored)
    }

    override suspend fun revert(change: RentChange): Outcome<Unit> {
        if (failWrites) return Outcome.Failure(DomainError.Storage("disk"))
        changes.value -= change
        reverted += change
        return Outcome.Success(Unit)
    }

    override suspend fun postpone(renterId: RenterId, until: BillingMonth): Outcome<Unit> {
        if (failWrites) return Outcome.Failure(DomainError.Storage("disk"))
        postponed[renterId] = until
        return Outcome.Success(Unit)
    }
}
