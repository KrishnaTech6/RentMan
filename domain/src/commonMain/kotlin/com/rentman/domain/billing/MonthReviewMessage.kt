package com.rentman.domain.billing

import com.rentman.domain.money.Paise

/**
 * The month review posted in the landlord's WhatsApp group: the month, its total, what was received and, if anything
 * is left, who still owes how much. Only this month's bills count. Amounts are formatted by [money].
 *
 * shortcut: English only. When a second language is needed, the wording moves into a templates object.
 */
object MonthReviewMessage {
    fun text(detail: MonthDetail, monthLabel: String, money: (Paise) -> String): String {
        val summary = detail.summary
        val pending = summary.late + summary.notDue
        val lines = buildList {
            add(monthLabel)
            add("Total: ${money(summary.billed)}")
            add("Received: ${money(summary.collected)}")
            if (pending > Paise.Zero) {
                add("Pending: ${money(pending)}")
                detail.rows.filter { it.left > Paise.Zero }.forEach { add("${it.summary.renter.name.trim()} - ${money(it.left)}") }
            }
        }
        return lines.joinToString("\n")
    }
}
