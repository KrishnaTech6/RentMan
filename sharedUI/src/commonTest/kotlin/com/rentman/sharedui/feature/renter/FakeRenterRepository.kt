package com.rentman.sharedui.feature.renter

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** Test fake. [failReads] makes observeActive emit a storage failure and [failAdd] makes `add` fail. */
internal class FakeRenterRepository(
    initial: List<Renter> = emptyList(),
    var failReads: Boolean = false,
    var failAdd: Boolean = false,
    var failWrites: Boolean = false,
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
        change(renter.id) { renter }
        return Outcome.Success(renter)
    }

    override suspend fun setLeft(renterId: RenterId, leftOn: LocalDate?): Outcome<Unit> {
        if (failWrites) return Outcome.Failure(DomainError.Storage("disk"))
        change(renterId) { it.copy(active = leftOn == null, leftOn = leftOn) }
        return Outcome.Success(Unit)
    }

    /** What the real database does in the same step as a rent change or "not now". */
    fun change(id: RenterId, edit: (Renter) -> Renter) {
        renters.value = renters.value.map { if (it.id == id) edit(it) else it }
    }

    override suspend fun setRemindersMutedMonth(renterId: RenterId, month: BillingMonth?): Outcome<Unit> {
        if (failWrites) return Outcome.Failure(DomainError.Storage("disk"))
        change(renterId) { it.copy(remindersMutedMonth = month) }
        return Outcome.Success(Unit)
    }

    override suspend fun add(renter: Renter): Outcome<Renter> {
        if (failAdd) return Outcome.Failure(DomainError.Storage("disk"))
        val stored = renter.copy(id = RenterId(renters.value.size + 1L))
        renters.value += stored
        return Outcome.Success(stored)
    }
}
