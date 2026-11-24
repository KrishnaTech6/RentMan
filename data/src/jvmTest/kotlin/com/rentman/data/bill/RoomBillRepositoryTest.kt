package com.rentman.data.bill

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.rentman.data.address.RoomAddressRepository
import com.rentman.data.db.AppDatabase
import com.rentman.data.db.BillLineEntity
import com.rentman.data.renter.RoomRenterRepository
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillId
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.time.Clock

class RoomBillRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val repository = RoomBillRepository(database.billDao(), Clock.System, TimeZone.currentSystemDefault())

    @AfterTest
    fun closeDatabase() = database.close()

    private suspend fun renterId(): RenterId {
        val address = (RoomAddressRepository(database.addressDao(), Clock.System).add("338 A") as Outcome.Success).value
        val renter = Renter(
            addressId = address.id,
            unit = null,
            name = "Ravi Kumar",
            phone = "919876543210",
            monthlyRent = 7_500L.rupees,
            dueDay = 5,
            agreementStart = LocalDate(2026, 1, 1),
            raisePercent = 10,
            raiseEveryMonths = 11,
            billsElectricity = true,
            customRate = null,
            startingReading = 1240,
            firstBillMonth = BillingMonth.of(2026, 11),
        )
        return (RoomRenterRepository(database.renterDao(), Clock.System).add(renter) as Outcome.Success).value.id
    }

    private fun bill(renter: RenterId, month: BillingMonth = BillingMonth.of(2026, 11)) = Bill(
        renterId = renter,
        month = month,
        dueOn = LocalDate(month.year, month.month, 5),
        previousReading = 1240,
        currentReading = 1325,
        rate = Paise(800),
        lines = listOf(
            BillLine(BillLineKind.Rent, "Rent", 7_500L.rupees),
            BillLine(BillLineKind.Electricity, "Electricity", Paise(68_000)),
            BillLine(BillLineKind.FixedCharge, "Water", 200L.rupees),
        ),
    )

    @Test
    fun aSavedBillComesBackWithItsIdAndLinesInOrder() = runTest {
        val renter = renterId()

        val saved = (repository.add(bill(renter)) as Outcome.Success).value

        assertEquals(BillId(1), saved.id)
        repository.observeAll().test {
            val listed = (awaitItem() as Outcome.Success).value.single()
            assertEquals(saved, listed)
            assertEquals(listOf("Rent", "Electricity", "Water"), listed.lines.map { it.label })
            assertEquals(8_380L.rupees, listed.total)
        }
    }

    @Test
    fun observeEmitsAgainWhenABillIsAdded() = runTest {
        val renter = renterId()

        repository.observeAll().test {
            assertEquals(emptyList(), (awaitItem() as Outcome.Success).value)

            repository.add(bill(renter))

            assertEquals(1, (awaitItem() as Outcome.Success).value.size)
        }
    }

    @Test
    fun billsComeBackOldestMonthFirst() = runTest {
        val renter = renterId()
        repository.add(bill(renter, BillingMonth.of(2026, 12)))
        repository.add(bill(renter, BillingMonth.of(2026, 11)))

        repository.observeAll().test {
            assertEquals(
                listOf(BillingMonth.of(2026, 11), BillingMonth.of(2026, 12)),
                (awaitItem() as Outcome.Success).value.map { it.month },
            )
        }
    }

    @Test
    fun theDatabaseRefusesASecondBillForTheSameRenterAndMonth() = runTest {
        val renter = renterId()
        repository.add(bill(renter))

        val second = repository.add(bill(renter))

        assertIs<DomainError.Storage>(assertIs<Outcome.Failure>(second).error)
        repository.observeAll().test {
            assertEquals(1, (awaitItem() as Outcome.Success).value.size)
        }
    }

    @Test
    fun ifALineFailsToSaveNoBillIsLeftBehind() = runTest {
        val renter = renterId()
        val bill = bill(renter).toEntity(createdAt = 1L)
        // Two lines with the same id: the second insert breaks the primary key after the bill row went in.
        val line = BillLineEntity(id = 5, billId = 0, kind = "Rent", label = "Rent", amountPaise = 750_000, sortOrder = 0)

        assertFails { database.billDao().insertWithLines(bill, listOf(line, line.copy(label = "Water"))) }

        repository.observeAll().test {
            assertEquals(emptyList(), (awaitItem() as Outcome.Success).value) // no bill without its lines
        }
    }

    @Test
    fun aBillForAnUnknownRenterIsRejected() = runTest {
        val result = repository.add(bill(RenterId(999)))

        assertIs<Outcome.Failure>(result)
    }

    @Test
    fun aRemovedBillAndItsLinesAreGoneAndTheMonthCanBeBilledAgain() = runTest {
        val renter = renterId()
        val saved = (repository.add(bill(renter)) as Outcome.Success).value

        assertEquals(Outcome.Success(Unit), repository.remove(saved.id))

        repository.observeAll().test { assertEquals(emptyList(), (awaitItem() as Outcome.Success).value) }
        assertEquals(0, database.backupDao().readAll(exportedAt = 0).billLine.size)
        assertIs<Outcome.Success<Bill>>(repository.add(bill(renter)))
    }

    @Test
    fun undoPutsTheRemovedBillBackWithTheSameIdLinesAndDay() = runTest {
        val renter = renterId()
        val saved = (repository.add(bill(renter)) as Outcome.Success).value
        repository.remove(saved.id)

        assertEquals(Outcome.Success(Unit), repository.restore(saved))

        repository.observeAll().test {
            val back = (awaitItem() as Outcome.Success).value.single()
            assertEquals(saved, back)
            assertEquals(listOf("Rent", "Electricity", "Water"), back.lines.map { it.label })
        }
    }

    @Test
    fun undoFailsAndChangesNothingIfTheMonthWasBilledAgain() = runTest {
        val renter = renterId()
        val saved = (repository.add(bill(renter)) as Outcome.Success).value
        repository.remove(saved.id)
        val newer = (repository.add(bill(renter)) as Outcome.Success).value

        assertIs<Outcome.Failure>(repository.restore(saved))

        repository.observeAll().test { assertEquals(listOf(newer), (awaitItem() as Outcome.Success).value) }
    }

    @Test
    fun removingABillThatIsNotThereIsNotAnError() = runTest {
        assertEquals(Outcome.Success(Unit), repository.remove(BillId(99)))
    }
}
