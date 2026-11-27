package com.rentman.domain.payments

import com.rentman.domain.money.Paise
import com.rentman.domain.tenancy.RenterSummary

/**
 * The line posted in the landlord's WhatsApp group when rent comes in: "Paid: Ravi Kumar (Ground floor · 338 A) - ₹7,500".
 * The amount is formatted by [money] (the ₹ formatter is a platform feature).
 *
 * shortcut: English only. When a second language is needed, the wording moves into a templates object.
 */
object PaymentMessage {
    fun text(summary: RenterSummary, amount: Paise, money: (Paise) -> String): String {
        val where = listOfNotNull(summary.renter.unit, summary.address.name).joinToString(" · ")
        return "Paid: ${summary.renter.name.trim()} ($where) - ${money(amount)}"
    }
}
