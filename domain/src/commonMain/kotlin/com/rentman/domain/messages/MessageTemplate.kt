package com.rentman.domain.messages

/**
 * The WhatsApp messages the app types for the landlord. Each has our wording as its [default], which the landlord can replace
 * with their own (in any language); the app fills in the [variables], written as `{name}`.
 */
enum class MessageKind(val variables: List<String>, val default: String) {
    /** The bill sent to a renter. */
    Bill(
        listOf("first_name", "name", "month", "bill_lines", "previous_pending", "paid_ahead", "total", "due_date"),
        """
        Hi {first_name}, your bill for {month}:
        {bill_lines}
        Previous pending: {previous_pending}
        Credit from earlier payments: {paid_ahead}
        Total due: {total}
        Please pay by {due_date}. Thanks!
        """.trimIndent(),
    ),

    /** The note that a payment was received, sent to whoever the landlord picks (their group, the renter, family). */
    PaymentReceived(
        listOf("name", "unit_address", "amount", "pending"),
        "Paid: {name} ({unit_address}) - {amount}",
    ),

    /** The month's totals posted in the landlord's group. */
    MonthReview(
        listOf("month", "total", "received", "pending", "pending_list"),
        """
        {month}
        Total: {total}
        Received: {received}
        Pending: {pending}
        {pending_list}
        """.trimIndent(),
    ),
}

object MessageTemplate {
    // Capitals too, so a "{Name}" is reported as unknown on Save instead of reaching the renter as typed.
    private val VARIABLE = Regex("""\{([A-Za-z_]+)}""")

    /**
     * [template] with each known `{variable}` replaced by its value in [values]. A line with a variable whose value is empty is
     * left out, so "Previous pending: {previous_pending}" only shows when something is pending. An unknown variable stays as typed.
     */
    fun fill(template: String, values: Map<String, String>): String =
        template.lines()
            .filterNot { line -> VARIABLE.findAll(line).any { values[it.groupValues[1]] == "" } }
            .joinToString("\n") { line -> VARIABLE.replace(line) { values[it.groupValues[1]] ?: it.value } }

    /** The variables in [template] that [kind] doesn't fill in, once each, in the order they are typed: most likely typos. */
    fun unknownVariables(template: String, kind: MessageKind): List<String> =
        VARIABLE.findAll(template).map { it.groupValues[1] }.filterNot { it in kind.variables }.distinct().toList()
}
