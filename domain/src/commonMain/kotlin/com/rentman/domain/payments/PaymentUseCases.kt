package com.rentman.domain.payments

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillId
import com.rentman.domain.billing.BillRepository
import com.rentman.domain.billing.PendingCalculator
import com.rentman.domain.flatMap
import com.rentman.domain.map
import com.rentman.domain.money.MoneyInput
import com.rentman.domain.money.Paise
import com.rentman.domain.tenancy.AddressRepository
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterRepository
import com.rentman.domain.tenancy.RenterSummary
import com.rentman.domain.tenancy.remindersMutedFor
import com.rentman.domain.time.GetTodayUseCase
import com.rentman.domain.billing.Bill
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate

class RecordPaymentUseCase(
    private val payments: PaymentRepository,
    private val today: GetTodayUseCase,
) {
    suspend operator fun invoke(form: PaymentForm): Outcome<Payment> {
        val amount = MoneyInput.amount(form.amount)
        val note = form.note.trim().ifEmpty { null }
        val problems = listOf(
            PaymentField.Amount to when {
                form.amount.isBlank() -> FieldProblem.Required
                amount == null -> FieldProblem.Invalid
                else -> null
            },
            PaymentField.Date to FieldProblem.Invalid.takeIf { form.paidOn > today() },
            PaymentField.Note to FieldProblem.Invalid.takeIf { (note?.length ?: 0) > MAX_NOTE_LENGTH },
        ).mapNotNull { (field, problem) -> problem?.let { field to it } }.toMap()
        if (problems.isNotEmpty() || amount == null) return Outcome.Failure(DomainError.InvalidPayment(problems))
        return payments.add(Payment(renterId = form.renterId, amount = amount, paidOn = form.paidOn, mode = form.mode, note = note))
    }

    private companion object {
        const val MAX_NOTE_LENGTH = 100
    }
}

/** Takes a payment out of the books. It can be put back with [RestorePaymentUseCase] (Undo). */
class RemovePaymentUseCase(private val payments: PaymentRepository) {
    suspend operator fun invoke(id: PaymentId): Outcome<Unit> = payments.setVoided(id, voided = true)
}

class RestorePaymentUseCase(private val payments: PaymentRepository) {
    suspend operator fun invoke(id: PaymentId): Outcome<Unit> = payments.setVoided(id, voided = false)
}

/** A line in a renter's ledger, with what they owed right after it. */
sealed interface LedgerEntry {
    val date: LocalDate
    val balanceAfter: Paise

    data class BillEntry(val bill: Bill, override val date: LocalDate, override val balanceAfter: Paise) : LedgerEntry
    data class PaymentEntry(val payment: Payment, override val date: LocalDate, override val balanceAfter: Paise) : LedgerEntry
}

/** Every bill and payment of one renter, newest first, and what they owe now (negative means paid ahead). */
data class RenterLedger(
    val summary: RenterSummary,
    val pending: Paise,
    val entries: List<LedgerEntry>,
    /** "Stop this month" is on: no reminders until the next bill. */
    val remindersMuted: Boolean,
    /** The bill that can be removed (ADR 0009): the newest one of a renter who lives there. Null if none can. */
    val removableBill: BillId? = null,
)

class ObserveRenterLedgerUseCase(
    private val renters: RenterRepository,
    private val addresses: AddressRepository,
    private val bills: BillRepository,
    private val payments: PaymentRepository,
) {
    operator fun invoke(renterId: RenterId): Flow<Outcome<RenterLedger>> =
        combine(renters.observeAll(), addresses.observeAll(), bills.observeAll(), payments.observeAll()) { r, a, b, p ->
            r.flatMap { renterList ->
                a.flatMap { addressList ->
                    b.flatMap { billList ->
                        p.map { paymentList ->
                            val renter = renterList.firstOrNull { it.id == renterId }
                            val address = addressList.firstOrNull { it.id == renter?.addressId }
                            if (renter == null || address == null) return@map null
                            ledgerOf(
                                RenterSummary(renter, address),
                                billList.filter { it.renterId == renterId },
                                paymentList.filter { it.renterId == renterId },
                            )
                        }
                    }
                }
            }.flatMap { ledger -> ledger?.let { Outcome.Success(it) } ?: Outcome.Failure(DomainError.NotFound) }
        }

    private class Movement(val date: LocalDate, val sortOrder: Int, val change: Paise, val entry: (Paise) -> LedgerEntry)

    private fun ledgerOf(summary: RenterSummary, bills: List<Bill>, payments: List<Payment>): RenterLedger {
        // On the same day the bill comes before the payment.
        val movements = bills.map { bill ->
            Movement(bill.issuedOn, sortOrder = 0, change = bill.total) { LedgerEntry.BillEntry(bill, bill.issuedOn, it) }
        } + payments.map { payment ->
            Movement(payment.paidOn, sortOrder = 1, change = Paise.Zero - payment.amount) { LedgerEntry.PaymentEntry(payment, payment.paidOn, it) }
        }
        val ordered = movements.sortedWith(compareBy<Movement>({ it.date }, { it.sortOrder }))
        val balances = ordered.runningFold(Paise.Zero) { balance, movement -> balance + movement.change }.drop(1)
        return RenterLedger(
            summary = summary,
            pending = PendingCalculator.pending(bills.map { it.total }, payments.map { it.amount }),
            entries = ordered.zip(balances) { movement, balance -> movement.entry(balance) }.reversed(),
            remindersMuted = summary.renter.remindersMutedFor(bills.maxOfOrNull { it.month }),
            removableBill = bills.maxByOrNull { it.month }?.id?.takeIf { summary.renter.active },
        )
    }
}
