package com.rentman.domain.billing

import com.rentman.domain.messages.MessageKind
import com.rentman.domain.messages.MessageTemplate
import com.rentman.domain.money.Paise

/**
 * The month review posted in the landlord's WhatsApp group: [template] with the month, its total, what was received and, if
 * anything is left, who still owes how much. Only this month's bills count. Amounts are formatted by [money].
 */
object MonthReviewMessage {
    fun text(detail: MonthDetail, monthLabel: String, money: (Paise) -> String, template: String = MessageKind.MonthReview.default): String {
        val summary = detail.summary
        val pending = summary.late + summary.notDue
        return MessageTemplate.fill(
            template,
            mapOf(
                "month" to monthLabel,
                "total" to money(summary.billed),
                "received" to money(summary.collected),
                "pending" to if (pending > Paise.Zero) money(pending) else "",
                "pending_list" to detail.rows.filter { it.left > Paise.Zero }.joinToString("\n") { "${it.summary.renter.name.trim()} - ${money(it.left)}" },
            ),
        )
    }
}
