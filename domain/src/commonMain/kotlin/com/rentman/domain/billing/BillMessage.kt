package com.rentman.domain.billing

import com.rentman.domain.money.Paise

/**
 * The text sent to a renter, as in PLAN.md. Amounts are formatted by [money] (the ₹ formatter is a platform
 * feature), and the month and due date arrive already written out.
 *
 * shortcut: English only. When a second language is needed, the wording moves into a templates object.
 */
object BillMessage {
    fun text(context: BillContext, bill: Bill, monthLabel: String, dueLabel: String, money: (Paise) -> String): String {
        val firstName = context.summary.renter.name.trim().substringBefore(' ')
        val lines = buildList {
            add("Hi $firstName, your bill for $monthLabel:")
            bill.lines.forEach { add(lineText(it, bill, money)) }
            if (context.pendingBefore > Paise.Zero) add("Previous pending: ${money(context.pendingBefore)}")
            // Paid ahead: shown as a credit that lowers the total, never as a negative "pending".
            if (context.pendingBefore < Paise.Zero) add("Credit from earlier payments: ${money(Paise.Zero - context.pendingBefore)}")
            add("Total due: ${money(context.totalDue(bill))}")
            add("Please pay by $dueLabel. Thanks!")
        }
        return lines.joinToString("\n")
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
