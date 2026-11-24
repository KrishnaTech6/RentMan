package com.rentman.domain.tenancy

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillingMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** In-memory fake used by use case tests. [failReads] makes observeActive emit a storage failure. */
class FakeRenterRepository(
    initial: List<Renter> = emptyList(),
    private val failReads: Boolean = false,
    private val failWrites: Boolean = false,
) : RenterRepository {
    private val renters = MutableStateFlow(initial)
    val saved: List<Renter> get() = renters.value

    override fun observeActive(): Flow<Outcome<List<Renter>>> = renters.map { list ->
        if (failReads) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success(list.filter(Renter::active).sortedBy(Renter::name))
    }

    override fun observeAll(): Flow<Outcome<List<Renter>>> = renters.map { list ->
        if (failReads) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success(list.sortedBy(Renter::name))
    }

    override suspend fun update(renter: Renter): Outcome<Renter> {
        if (failWrites) return Outcome.Failure(DomainError.Storage("disk"))
        renters.value = renters.value.map { if (it.id == renter.id) renter else it }
        return Outcome.Success(renter)
    }

    override suspend fun setLeft(renterId: RenterId, leftOn: LocalDate?): Outcome<Unit> {
        if (failWrites) return Outcome.Failure(DomainError.Storage("disk"))
        renters.value = renters.value.map { if (it.id == renterId) it.copy(active = leftOn == null, leftOn = leftOn) else it }
        return Outcome.Success(Unit)
    }

    override suspend fun setRemindersMutedMonth(renterId: RenterId, month: BillingMonth?): Outcome<Unit> {
        if (failWrites) return Outcome.Failure(DomainError.Storage("disk"))
        renters.value = renters.value.map { if (it.id == renterId) it.copy(remindersMutedMonth = month) else it }
        return Outcome.Success(Unit)
    }

    override suspend fun add(renter: Renter): Outcome<Renter> {
        val stored = renter.copy(id = RenterId(renters.value.size + 1L))
        renters.value += stored
        return Outcome.Success(stored)
    }
}
