package com.rentman.domain.payments

import com.rentman.domain.messages.MessageKind
import com.rentman.domain.messages.MessageTemplate
import com.rentman.domain.money.Paise
import com.rentman.domain.tenancy.RenterSummary

/**
 * The line posted in the landlord's WhatsApp group when rent comes in, "Paid: Ravi Kumar (Ground floor · 338 A) - ₹7,500" with
 * our [template]. [pending] is what is left after this payment (never below zero). Amounts are formatted by [money].
 */
object PaymentMessage {
    fun text(
        summary: RenterSummary,
        amount: Paise,
        money: (Paise) -> String,
        pending: Paise = Paise.Zero,
        template: String = MessageKind.PaymentReceived.default,
    ): String = MessageTemplate.fill(
        template,
        mapOf(
            "name" to summary.renter.name.trim(),
            "unit_address" to listOfNotNull(summary.renter.unit, summary.address.name).joinToString(" · "),
            "amount" to money(amount),
            "pending" to money(maxOf(Paise.Zero, pending)),
        ),
    )
}
