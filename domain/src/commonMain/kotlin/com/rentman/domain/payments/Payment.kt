package com.rentman.domain.payments

import com.rentman.domain.Outcome
import com.rentman.domain.money.MoneyInput
import com.rentman.domain.money.Paise
import com.rentman.domain.tenancy.RenterId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.jvm.JvmInline

@JvmInline
value class PaymentId(val value: Long) {
    companion object {
        /** The id of a payment that hasn't been saved yet. */
        val None = PaymentId(0)
    }
}

/** How the money was handed over. Stored by name, so the names must never be renamed. */
enum class PaymentMode { Upi, Cash, Bank }

/**
 * Money a renter has handed over. It is not tied to a bill and can cover only part of what is owed.
 * See GLOSSARY.md.
 */
data class Payment(
    val id: PaymentId = PaymentId.None,
    val renterId: RenterId,
    val amount: Paise,
    val paidOn: LocalDate,
    val mode: PaymentMode,
    val note: String? = null,
)

/** What a person typed on the Record payment screen, before any checking. */
data class PaymentForm(
    val renterId: RenterId,
    val amount: String,
    val paidOn: LocalDate,
    val mode: PaymentMode,
    val note: String,
)

/**
 * How much of the typed amount is more than [pending], or null if it isn't or can't be read yet.
 * Overpaying is allowed (it becomes a credit), so this is only for a warning against a typo.
 */
fun PaymentForm.overpayment(pending: Paise): Paise? {
    val owed = maxOf(pending, Paise.Zero)
    return MoneyInput.amount(amount)?.takeIf { it > owed }?.let { it - owed }
}

enum class PaymentField { Amount, Date, Note }

interface PaymentRepository {
    /** Payments that haven't been removed. Emits again whenever they change. */
    fun observeAll(): Flow<Outcome<List<Payment>>>

    suspend fun add(payment: Payment): Outcome<Payment>

    /** Removing is only hiding: the row stays so that Undo can bring it back. */
    suspend fun setVoided(id: PaymentId, voided: Boolean): Outcome<Unit>
}
