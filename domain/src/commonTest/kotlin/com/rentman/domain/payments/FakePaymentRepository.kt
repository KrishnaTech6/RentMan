package com.rentman.domain.payments

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory fake. Voided payments are kept but never listed, like the real repository. */
class FakePaymentRepository(
    initial: List<Payment> = emptyList(),
    private val failReads: Boolean = false,
    private val failAdd: Boolean = false,
) : PaymentRepository {
    private val all = MutableStateFlow(initial.map { it to false })

    val saved: List<Payment> get() = all.value.filterNot { (_, voided) -> voided }.map { it.first }
    val voidedIds: List<PaymentId> get() = all.value.filter { it.second }.map { it.first.id }

    override fun observeAll(): Flow<Outcome<List<Payment>>> = all.map { rows ->
        if (failReads) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success(rows.filterNot { it.second }.map { it.first })
    }

    override suspend fun add(payment: Payment): Outcome<Payment> {
        if (failAdd) return Outcome.Failure(DomainError.Storage("disk"))
        val stored = payment.copy(id = PaymentId(all.value.size + 1L))
        all.value += stored to false
        return Outcome.Success(stored)
    }

    override suspend fun setVoided(id: PaymentId, voided: Boolean): Outcome<Unit> {
        all.value = all.value.map { (payment, was) -> payment to if (payment.id == id) voided else was }
        return Outcome.Success(Unit)
    }
}
