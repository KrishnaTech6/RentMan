package com.rentman.sharedui.feature.payment

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Test fake. Voided payments are kept but not listed. [failReads], [failAdd] and [failVoid] make that call fail,
 * and [holdAdd] makes `add` wait until it is completed, to test a second tap while saving.
 */
internal class FakePaymentRepository(
    initial: List<Payment> = emptyList(),
    var failReads: Boolean = false,
    var failAdd: Boolean = false,
    var failVoid: Boolean = false,
    var holdAdd: CompletableDeferred<Unit>? = null,
) : PaymentRepository {
    private val all = MutableStateFlow(initial.map { it to false })
    var addCalls = 0
        private set

    val saved: List<Payment> get() = all.value.filterNot { it.second }.map { it.first }

    override fun observeAll(): Flow<Outcome<List<Payment>>> = all.map { rows ->
        if (failReads) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success(rows.filterNot { it.second }.map { it.first })
    }

    override suspend fun add(payment: Payment): Outcome<Payment> {
        addCalls++
        holdAdd?.await()
        if (failAdd) return Outcome.Failure(DomainError.Storage("disk"))
        val stored = payment.copy(id = PaymentId(all.value.size + 1L))
        all.value += stored to false
        return Outcome.Success(stored)
    }

    override suspend fun setVoided(id: PaymentId, voided: Boolean): Outcome<Unit> {
        if (failVoid) return Outcome.Failure(DomainError.Storage("disk"))
        all.value = all.value.map { (payment, was) -> payment to if (payment.id == id) voided else was }
        return Outcome.Success(Unit)
    }
}
