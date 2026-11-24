package com.rentman.domain.payments

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class RecordPaymentUseCaseTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-23T10:00:00Z")
    }
    private val today = LocalDate(2026, 11, 23)
    private val repository = FakePaymentRepository()
    private val record = RecordPaymentUseCase(repository, GetTodayUseCase(clock, TimeZone.UTC))

    private fun form(
        amount: String = "4,000",
        paidOn: LocalDate = today,
        mode: PaymentMode = PaymentMode.Upi,
        note: String = "",
    ) = PaymentForm(RenterId(1), amount, paidOn, mode, note)

    private fun problems(outcome: Outcome<Payment>) = ((outcome as Outcome.Failure).error as DomainError.InvalidPayment).problems

    @Test
    fun savesAPartPaymentFromTypedText() = runTest {
        val saved = (record(form(note = "  second half next week ")) as Outcome.Success).value

        assertEquals(PaymentId(1), saved.id)
        assertEquals(RenterId(1), saved.renterId)
        assertEquals(4_000L.rupees, saved.amount)
        assertEquals(PaymentMode.Upi, saved.mode)
        assertEquals(today, saved.paidOn)
        assertEquals("second half next week", saved.note)
        assertEquals(listOf(saved), repository.saved)
    }

    @Test
    fun aPaymentCanHavePaiseBecauseABillCan() = runTest {
        assertEquals(Paise(338_050), (record(form(amount = "3,380.50")) as Outcome.Success).value.amount)
    }

    @Test
    fun aBlankNoteIsStoredAsNoNote() = runTest {
        assertEquals(null, (record(form(note = "   ")) as Outcome.Success).value.note)
    }

    @Test
    fun anEarlierDateIsFineAndSoIsToday() = runTest {
        assertEquals(LocalDate(2026, 11, 1), (record(form(paidOn = LocalDate(2026, 11, 1))) as Outcome.Success).value.paidOn)
        assertEquals(today, (record(form(paidOn = today)) as Outcome.Success).value.paidOn)
    }

    @Test
    fun aDateInTheFutureIsInvalid() = runTest {
        assertEquals(mapOf(PaymentField.Date to FieldProblem.Invalid), problems(record(form(paidOn = LocalDate(2026, 11, 24)))))
        assertEquals(emptyList(), repository.saved)
    }

    @Test
    fun aBlankAmountIsRequiredAndABadOneIsInvalid() = runTest {
        assertEquals(mapOf(PaymentField.Amount to FieldProblem.Required), problems(record(form(amount = " "))))
        assertEquals(mapOf(PaymentField.Amount to FieldProblem.Invalid), problems(record(form(amount = "0"))))
        assertEquals(mapOf(PaymentField.Amount to FieldProblem.Invalid), problems(record(form(amount = "12.505"))))
        assertEquals(mapOf(PaymentField.Amount to FieldProblem.Invalid), problems(record(form(amount = "abc"))))
    }

    @Test
    fun aVeryLongNoteIsInvalid() = runTest {
        assertEquals(mapOf(PaymentField.Note to FieldProblem.Invalid), problems(record(form(note = "x".repeat(101)))))
    }

    @Test
    fun everyProblemIsReportedAtOnce() = runTest {
        val all = problems(record(form(amount = "", paidOn = LocalDate(2027, 1, 1), note = "x".repeat(101))))

        assertEquals(setOf(PaymentField.Amount, PaymentField.Date, PaymentField.Note), all.keys)
    }

    @Test
    fun aStorageFailureIsReportedAndNothingIsKept() = runTest {
        val failing = FakePaymentRepository(failAdd = true)

        val result = RecordPaymentUseCase(failing, GetTodayUseCase(clock, TimeZone.UTC))(form())

        assertEquals(Outcome.Failure(DomainError.Storage("disk")), result)
        assertEquals(emptyList(), failing.saved)
    }

    @Test
    fun aPaymentCanBeRemovedAndPutBack() = runTest {
        val saved = (record(form()) as Outcome.Success).value

        RemovePaymentUseCase(repository)(saved.id)
        assertEquals(emptyList(), repository.saved)

        RestorePaymentUseCase(repository)(saved.id)
        assertEquals(listOf(saved), repository.saved)
    }
}
