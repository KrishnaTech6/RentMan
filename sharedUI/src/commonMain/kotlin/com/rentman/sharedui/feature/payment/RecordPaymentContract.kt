package com.rentman.sharedui.feature.payment

import com.rentman.domain.DomainError
import com.rentman.domain.money.Paise
import com.rentman.domain.payments.PaymentField
import com.rentman.domain.payments.PaymentForm
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.payments.overpayment
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.domain.tenancy.RenterSummary
import kotlinx.datetime.LocalDate

internal sealed interface RecordPaymentIntent {
    data class AmountChanged(val text: String) : RecordPaymentIntent
    /** Fills in everything that is pending. */
    data object UseFullAmount : RecordPaymentIntent
    data class ModeChanged(val mode: PaymentMode) : RecordPaymentIntent
    /** Moves the date by [days], never past today. */
    data class DayStepped(val days: Int) : RecordPaymentIntent
    data class NoteChanged(val text: String) : RecordPaymentIntent
    data class Save(val form: PaymentForm) : RecordPaymentIntent
    data object Back : RecordPaymentIntent
    data object Retry : RecordPaymentIntent
}

internal sealed interface RecordPaymentUiState {
    data object Loading : RecordPaymentUiState

    data class Error(val error: DomainError) : RecordPaymentUiState

    data class Content(
        val summary: RenterSummary,
        val pending: Paise,
        val today: LocalDate,
        val amount: String = "",
        val paidOn: LocalDate = today,
        val mode: PaymentMode = PaymentMode.Upi,
        val note: String = "",
        val problems: Map<PaymentField, FieldProblem> = emptyMap(),
        /** A failure that isn't about one field, e.g. the disk. The typed form is kept. */
        val saveError: DomainError? = null,
        val isSaving: Boolean = false,
    ) : RecordPaymentUiState {
        fun toForm() = PaymentForm(summary.renter.id, amount, paidOn, mode, note)

        /** Set while the typed amount is more than what is pending: a warning against a typo, not a block. */
        val overpayment: Paise? get() = toForm().overpayment(pending)
    }
}

internal sealed interface RecordPaymentNavEvent {
    data object Saved : RecordPaymentNavEvent
    data object Close : RecordPaymentNavEvent
}
