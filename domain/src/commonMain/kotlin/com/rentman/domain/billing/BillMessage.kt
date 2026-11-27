package com.rentman.domain.billing

import com.rentman.domain.messages.MessageKind
import com.rentman.domain.messages.MessageTemplate
import com.rentman.domain.money.Paise

/**
 * The text sent to a renter: [template] (the landlord's own, or ours) with the bill filled in. Amounts are formatted by [money]
 * (the ₹ formatter is a platform feature), and the month and due date arrive already written out.
 */
object BillMessage {
    fun text(
        context: BillContext,
        bill: Bill,
        monthLabel: String,
        dueLabel: String,
        money: (Paise) -> String,
        template: String = MessageKind.Bill.default,
    ): String {
        val name = context.summary.renter.name.trim()
        val pending = context.pendingBefore
        return MessageTemplate.fill(
            template,
            mapOf(
                "first_name" to name.substringBefore(' '),
                "name" to name,
                "month" to monthLabel,
                "bill_lines" to bill.lines.joinToString("\n") { lineText(it, bill, money) },
                "previous_pending" to if (pending > Paise.Zero) money(pending) else "",
                // Paid ahead: shown as a credit that lowers the total, never as a negative "pending".
                "paid_ahead" to if (pending < Paise.Zero) money(Paise.Zero - pending) else "",
                "total" to money(context.totalDue(bill)),
                "due_date" to dueLabel,
            ),
        )
    }

    private fun lineText(line: BillLine, bill: Bill, money: (Paise) -> String): String {
        val units = bill.units
        val rate = bill.rate
        return if (line.kind == BillLineKind.Electricity && units != null && rate != null) {
            "${line.label}: ${bill.previousReading} → ${bill.currentReading} = $units units × ${money(rate)} = ${money(line.amount)}"
        } else {
            "${line.label}: ${money(line.amount)}"
        }
    }
}
