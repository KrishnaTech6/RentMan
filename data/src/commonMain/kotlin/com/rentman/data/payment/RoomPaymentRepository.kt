package com.rentman.data.payment

import com.rentman.data.db.PaymentDao
import com.rentman.data.db.PaymentEntity
import com.rentman.data.safeCall
import com.rentman.data.safeFlow
import com.rentman.domain.Outcome
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

internal class RoomPaymentRepository(
    private val dao: PaymentDao,
    private val clock: Clock,
) : PaymentRepository {

    override fun observeAll(): Flow<Outcome<List<Payment>>> =
        safeFlow { dao.observeActive().map { rows -> rows.map(PaymentEntity::toDomain) } }

    override suspend fun add(payment: Payment): Outcome<Payment> = safeCall {
        val id = dao.insert(payment.toEntity(createdAt = clock.now().toEpochMilliseconds()))
        payment.copy(id = PaymentId(id))
    }

    override suspend fun setVoided(id: PaymentId, voided: Boolean): Outcome<Unit> = safeCall {
        dao.setVoidedAt(id.value, if (voided) clock.now().toEpochMilliseconds() else null)
    }
}
