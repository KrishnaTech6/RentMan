package com.rentman.domain.billing

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory fake. [failReads] makes observeAll emit a storage failure. */
class FakeBillRepository(initial: List<Bill> = emptyList(), private val failReads: Boolean = false) : BillRepository {
    private val bills = MutableStateFlow(initial)
    val saved: List<Bill> get() = bills.value

    override fun observeAll(): Flow<Outcome<List<Bill>>> = bills.map {
        if (failReads) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success(it)
    }

    override suspend fun add(bill: Bill): Outcome<Bill> {
        val stored = bill.copy(id = BillId((bills.value.maxOfOrNull { it.id.value } ?: 0) + 1L))
        bills.value += stored
        return Outcome.Success(stored)
    }

    override suspend fun remove(id: BillId): Outcome<Unit> {
        bills.value = bills.value.filterNot { it.id == id }
        return Outcome.Success(Unit)
    }

    override suspend fun restore(bill: Bill): Outcome<Unit> {
        if (bills.value.any { it.renterId == bill.renterId && it.month == bill.month }) return Outcome.Failure(DomainError.Storage("month already billed"))
        bills.value = (bills.value + bill).sortedBy { it.id.value }
        return Outcome.Success(Unit)
    }
}
