package com.rentman.data.payment

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.rentman.data.address.RoomAddressRepository
import com.rentman.data.db.AppDatabase
import com.rentman.data.renter.RoomRenterRepository
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock

class RoomPaymentRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val repository = RoomPaymentRepository(database.paymentDao(), Clock.System)

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
            billsElectricity = false,
            customRate = null,
            startingReading = null,
            firstBillMonth = BillingMonth.of(2026, 11),
        )
        return (RoomRenterRepository(database.renterDao(), Clock.System).add(renter) as Outcome.Success).value.id
    }

    private fun payment(renter: RenterId, on: LocalDate = LocalDate(2026, 11, 22), amount: Long = 4_000) = Payment(
        renterId = renter,
        amount = amount.rupees,
        paidOn = on,
        mode = PaymentMode.Cash,
        note = "part",
    )

    private fun Outcome<List<Payment>>.list() = (this as Outcome.Success).value

    @Test
    fun aSavedPaymentComesBackWithItsId() = runTest {
        val renter = renterId()

        val saved = (repository.add(payment(renter)) as Outcome.Success).value

        assertEquals(PaymentId(1), saved.id)
        repository.observeAll().test {
            assertEquals(listOf(saved), awaitItem().list())
        }
    }

    @Test
    fun paymentsAreListedOldestDateFirstAndEmitAgainWhenOneIsAdded() = runTest {
        val renter = renterId()
        repository.add(payment(renter, LocalDate(2026, 12, 1), amount = 2_000))

        repository.observeAll().test {
            assertEquals(1, awaitItem().list().size)

            repository.add(payment(renter, LocalDate(2026, 11, 3), amount = 1_000))

            assertEquals(listOf(1_000L.rupees, 2_000L.rupees), awaitItem().list().map { it.amount })
        }
    }

    @Test
    fun aRemovedPaymentIsHiddenAndUndoBringsItBack() = runTest {
        val renter = renterId()
        val saved = (repository.add(payment(renter)) as Outcome.Success).value

        repository.observeAll().test {
            assertEquals(listOf(saved), awaitItem().list())

            assertEquals(Outcome.Success(Unit), repository.setVoided(saved.id, voided = true))
            assertEquals(emptyList(), awaitItem().list())

            assertEquals(Outcome.Success(Unit), repository.setVoided(saved.id, voided = false))
            assertEquals(listOf(saved), awaitItem().list())
        }
    }

    @Test
    fun removingOnePaymentLeavesTheOthers() = runTest {
        val renter = renterId()
        val first = (repository.add(payment(renter, amount = 1_000)) as Outcome.Success).value
        val second = (repository.add(payment(renter, amount = 2_000)) as Outcome.Success).value

        repository.setVoided(first.id, voided = true)

        repository.observeAll().test {
            assertEquals(listOf(second), awaitItem().list())
        }
    }

    @Test
    fun aPaymentForAnUnknownRenterIsRejected() = runTest {
        val result = repository.add(payment(RenterId(999)))

        assertIs<DomainError.Storage>(assertIs<Outcome.Failure>(result).error)
    }
}
