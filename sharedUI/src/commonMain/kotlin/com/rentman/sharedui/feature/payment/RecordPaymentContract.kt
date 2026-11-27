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
    /** [post] is set for "Save & send on WhatsApp": the screen opens WhatsApp once the payment is stored. */
    data class Save(val form: PaymentForm, val post: PostTo? = null) : RecordPaymentIntent
    data object Back : RecordPaymentIntent
    data object Retry : RecordPaymentIntent
}

internal sealed interface RecordPaymentUiState {
    data object Loading : RecordPaymentUiState

    data class Error(val error: DomainError) : RecordPaymentUiState

    data class Content(
        val summary: RenterSummary,
        val pending: Paise,
        /** The wording of the group post: the landlord's own, or ours. */
        val message: String,
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

        fun postTo() = PostTo(summary, pending, message)

        /** Set while the typed amount is more than what is pending: a warning against a typo, not a block. */
        val overpayment: Paise? get() = toForm().overpayment(pending)
    }
}

/** Who the group post is about, what was pending before this payment, and the wording to use. */
internal data class PostTo(val summary: RenterSummary, val pendingBefore: Paise, val message: String)

/** What the group is told: the stored amount (not the typed text) and what is left after it. */
internal data class PaymentPost(val summary: RenterSummary, val amount: Paise, val pending: Paise, val message: String)

internal sealed interface RecordPaymentNavEvent {
    data class Saved(val post: PaymentPost?) : RecordPaymentNavEvent
    data object Close : RecordPaymentNavEvent
}
