package com.rentman.data.bill

import com.rentman.data.db.BillDao
import com.rentman.data.db.BillWithLines
import com.rentman.data.safeCall
import com.rentman.data.safeFlow
import com.rentman.domain.Outcome
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillId
import com.rentman.domain.billing.BillRepository
import com.rentman.domain.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal class RoomBillRepository(
    private val dao: BillDao,
    private val clock: Clock,
    private val timeZone: TimeZone,
) : BillRepository {

    override fun observeAll(): Flow<Outcome<List<Bill>>> =
        safeFlow { dao.observeAll().map { rows -> rows.map { it.toDomain(timeZone) } } }

    override suspend fun add(bill: Bill): Outcome<Bill> = safeCall {
        val now = clock.now()
        val id = dao.insertWithLines(
            bill = bill.toEntity(createdAt = now.toEpochMilliseconds()),
            lines = bill.lines.mapIndexed { index, line -> line.toEntity(billId = 0, sortOrder = index) },
        )
        bill.copy(id = BillId(id), createdOn = now.toLocalDateTime(timeZone).date)
    }

    override suspend fun remove(id: BillId): Outcome<Unit> = safeCall { dao.delete(id.value) }

    // The unique (renter, month) index refuses this if the month has been billed again, and the transaction then changes nothing.
    override suspend fun restore(bill: Bill): Outcome<Unit> = safeCall {
        val createdAt = (bill.createdOn ?: now().toLocalDateTime(timeZone).date).atStartOfDayIn(timeZone).toEpochMilliseconds()
        dao.insertWithLines(
            bill = bill.toEntity(createdAt = createdAt),
            lines = bill.lines.mapIndexed { index, line -> line.toEntity(billId = 0, sortOrder = index) },
        )
    }.map { }

    private fun now() = clock.now()
}
