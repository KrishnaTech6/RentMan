package com.rentman.sharedui.feature.messages

import com.rentman.domain.DomainError
import com.rentman.domain.messages.MessageKind
import com.rentman.domain.messages.MessageTemplate

internal sealed interface MessageEditIntent {
    /** The text or the cursor in it changed. [cursor] is where a tapped variable goes in. */
    data class TextChanged(val text: String, val cursor: Int) : MessageEditIntent
    data class InsertVariable(val name: String) : MessageEditIntent
    /** Puts our wording back in the box. Saving it goes back to ours. */
    data object UseOurWording : MessageEditIntent
    data class Save(val text: String) : MessageEditIntent
    data object Back : MessageEditIntent
    data object Retry : MessageEditIntent
}

internal sealed interface MessageEditUiState {
    data object Loading : MessageEditUiState

    data class Error(val error: DomainError) : MessageEditUiState

    data class Content(
        val kind: MessageKind,
        val text: String,
        val cursor: Int = text.length,
        /** Goes up only when the app itself changes the text (a variable added, our wording put back), so the box starts from it. */
        val replaced: Int = 0,
        /** Set after Save refused the text: the variables the app doesn't fill in. */
        val unknownVariables: List<String> = emptyList(),
        val saveError: DomainError? = null,
        val isSaving: Boolean = false,
    ) : MessageEditUiState {
        /** The text with an example renter filled in, so the landlord sees what will be sent. */
        val preview: String get() = MessageTemplate.fill(text, EXAMPLES.getValue(kind))
    }
}

internal sealed interface MessageEditNavEvent {
    data object Close : MessageEditNavEvent
}

/** Example values for the preview. A blank one shows that its line is left out when there is nothing to say. */
private val EXAMPLES = mapOf(
    MessageKind.Bill to mapOf(
        "first_name" to "Ravi",
        "name" to "Ravi Kumar",
        "month" to "Nov 2026",
        "bill_lines" to "Rent: ₹7,500\nWater: ₹200\nElectricity: 1240 → 1325 = 85 units × ₹8 = ₹680",
        "previous_pending" to "₹1,000",
        "paid_ahead" to "",
        "total" to "₹9,380",
        "due_date" to "5 Dec",
    ),
    MessageKind.PaymentReceived to mapOf(
        "name" to "Ravi Kumar",
        "unit_address" to "Ground floor · 12 A",
        "amount" to "₹7,500",
        "pending" to "₹1,880",
    ),
    MessageKind.MonthReview to mapOf(
        "month" to "Nov 2026",
        "total" to "₹60,000",
        "received" to "₹45,000",
        "pending" to "₹15,000",
        "pending_list" to "Amit Shah - ₹10,500\nMohan Lal - ₹4,500",
    ),
)
