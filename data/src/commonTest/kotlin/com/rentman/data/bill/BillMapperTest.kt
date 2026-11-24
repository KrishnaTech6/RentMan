package com.rentman.data.bill

import com.rentman.data.db.BillWithLines
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillId
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.RenterId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class BillMapperTest {
    private val withElectricity = Bill(
        id = BillId(4),
        renterId = RenterId(2),
        month = BillingMonth.of(2026, 11),
        dueOn = LocalDate(2026, 11, 5),
        previousReading = 1240,
        currentReading = 1325,
        rate = Paise(800),
        meterReset = true,
        meterPhotoPath = "9f3a1c.jpg",
        lines = listOf(
            BillLine(BillLineKind.Rent, "Rent", 7_500L.rupees),
            BillLine(BillLineKind.Electricity, "Electricity", Paise(68_000)),
            BillLine(BillLineKind.FixedCharge, "Water", 200L.rupees),
            BillLine(BillLineKind.Adjustment, "Waived late fee", Paise(-5_000)),
        ),
    )

    private val withoutElectricity = withElectricity.copy(
        previousReading = null,
        currentReading = null,
        rate = null,
        meterReset = false,
        meterPhotoPath = null,
        lines = withElectricity.lines.take(1),
    )

    /** Made on 3 Nov 2026 at 10:00 UTC. */
    private val createdAt = Instant.parse("2026-11-03T10:00:00Z").toEpochMilliseconds()

    private fun Bill.roundTrip(timeZone: TimeZone = TimeZone.UTC): Bill = BillWithLines(
        bill = toEntity(createdAt = createdAt),
        lines = lines.mapIndexed { index, line -> line.toEntity(billId = id.value, sortOrder = index) },
    ).toDomain(timeZone)

    @Test
    fun theDayABillWasMadeIsReadBackInTheLandlordsTimeZone() {
        assertEquals(LocalDate(2026, 11, 3), withElectricity.roundTrip().createdOn)
        // 20:00 UTC on the 3rd is already 01:30 on the 4th in India.
        val lateEvening = BillWithLines(
            bill = withElectricity.toEntity(createdAt = Instant.parse("2026-11-03T20:00:00Z").toEpochMilliseconds()),
            lines = emptyList(),
        )
        assertEquals(LocalDate(2026, 11, 4), lateEvening.toDomain(TimeZone.of("Asia/Kolkata")).createdOn)
    }

    @Test
    fun aBillWithEveryFieldSurvivesTheRoundTrip() {
        assertEquals(withElectricity.copy(createdOn = LocalDate(2026, 11, 3)), withElectricity.roundTrip())
    }

    @Test
    fun theMeterPhotoSurvivesTheRoundTripAndIsStoredAsTheFileName() {
        assertEquals("9f3a1c.jpg", withElectricity.roundTrip().meterPhotoPath)
        assertEquals("9f3a1c.jpg", withElectricity.toEntity(createdAt = 1L).meterPhotoPath)
        assertEquals(null, withoutElectricity.roundTrip().meterPhotoPath)
    }

    @Test
    fun aBillWithoutElectricitySurvivesTheRoundTrip() {
        assertEquals(withoutElectricity.copy(createdOn = LocalDate(2026, 11, 3)), withoutElectricity.roundTrip())
    }

    @Test
    fun linesComeBackInTheOrderTheyWereWrittenEvenIfTheDatabaseReturnsThemShuffled() {
        val rows = BillWithLines(
            bill = withElectricity.toEntity(createdAt = 1L),
            lines = withElectricity.lines.mapIndexed { index, line -> line.toEntity(billId = 4, sortOrder = index) }.reversed(),
        )

        assertEquals(withElectricity.lines, rows.toDomain(TimeZone.UTC).lines)
    }

    @Test
    fun storesMoneyInPaiseAndTheMonthAsYyyymm() {
        val entity = withElectricity.toEntity(createdAt = 99L)

        assertEquals(202611, entity.month)
        assertEquals(800L, entity.ratePaise)
        assertEquals(-5_000L, withElectricity.lines.last().toEntity(billId = 4, sortOrder = 3).amountPaise)
        assertEquals("Adjustment", withElectricity.lines.last().toEntity(billId = 4, sortOrder = 3).kind)
    }
}
