package com.rentman.data.renter

import com.rentman.data.db.RenterDao
import com.rentman.data.db.RenterWithCharges
import com.rentman.data.safeCall
import com.rentman.data.safeFlow
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.flatMap
import com.rentman.domain.tenancy.FixedChargeId
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

internal class RoomRenterRepository(
    private val dao: RenterDao,
    private val clock: Clock,
) : RenterRepository {

    override fun observeActive(): Flow<Outcome<List<Renter>>> =
        safeFlow { dao.observeActive().map { rows -> rows.map(RenterWithCharges::toDomain) } }

    override fun observeAll(): Flow<Outcome<List<Renter>>> =
        safeFlow { dao.observeAll().map { rows -> rows.map(RenterWithCharges::toDomain) } }

    override suspend fun update(renter: Renter): Outcome<Renter> =
        safeCall { dao.updateWithCharges(renter.toEntity(createdAt = 0), renter.fixedCharges.map { it.toEntity(renterId = renter.id.value) }) }
            .flatMap { ids ->
                if (ids == null) {
                    Outcome.Failure(DomainError.NotFound)
                } else {
                    Outcome.Success(renter.copy(fixedCharges = renter.fixedCharges.zip(ids) { charge, id -> charge.copy(id = FixedChargeId(id)) }))
                }
            }

    override suspend fun setLeft(renterId: RenterId, leftOn: LocalDate?): Outcome<Unit> =
        safeCall { dao.setLeft(renterId.value, active = leftOn == null, leftOn = leftOn?.toEpochDays()) }

    override suspend fun setRemindersMutedMonth(renterId: RenterId, month: BillingMonth?): Outcome<Unit> =
        safeCall { dao.setRemindersMutedMonth(renterId.value, month?.value) }

    override suspend fun add(renter: Renter): Outcome<Renter> = safeCall {
        val inserted = dao.insertWithCharges(
            renter = renter.toEntity(createdAt = clock.now().toEpochMilliseconds()),
            charges = renter.fixedCharges.map { it.toEntity(renterId = 0) },
        )
        renter.copy(
            id = RenterId(inserted.renterId),
            fixedCharges = renter.fixedCharges.zip(inserted.chargeIds) { charge, id -> charge.copy(id = FixedChargeId(id)) },
        )
    }
}
