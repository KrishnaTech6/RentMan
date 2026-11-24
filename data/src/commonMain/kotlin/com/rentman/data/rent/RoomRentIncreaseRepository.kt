package com.rentman.data.rent

import com.rentman.data.db.RentChangeDao
import com.rentman.data.db.RentChangeEntity
import com.rentman.data.safeCall
import com.rentman.data.safeFlow
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.flatMap
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.tenancy.RentChange
import com.rentman.domain.tenancy.RentChangeId
import com.rentman.domain.tenancy.RentIncreaseRepository
import com.rentman.domain.tenancy.RenterId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

internal class RoomRentIncreaseRepository(
    private val dao: RentChangeDao,
    private val clock: Clock,
) : RentIncreaseRepository {

    override fun observeAll(): Flow<Outcome<List<RentChange>>> =
        safeFlow { dao.observeActive().map { rows -> rows.map(RentChangeEntity::toDomain) } }

    override suspend fun apply(change: RentChange): Outcome<RentChange> =
        safeCall { dao.apply(change.toEntity()) }.flatMap { id ->
            if (id == null) Outcome.Failure(DomainError.OfferOutdated) else Outcome.Success(change.copy(id = RentChangeId(id)))
        }

    override suspend fun revert(change: RentChange): Outcome<Unit> = safeCall {
        dao.revert(change.toEntity(), voidedAt = clock.now().toEpochMilliseconds())
    }

    override suspend fun postpone(renterId: RenterId, until: BillingMonth): Outcome<Unit> = safeCall {
        dao.setSnoozedUntil(renterId.value, until.value)
    }
}
