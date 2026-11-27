package com.rentman.domain.messages

import kotlin.test.Test
import kotlin.test.assertEquals

class MessageTemplateTest {
    @Test
    fun fillsEveryVariable() {
        assertEquals("Paid: Ravi - ₹7,500", MessageTemplate.fill("Paid: {name} - {amount}", mapOf("name" to "Ravi", "amount" to "₹7,500")))
    }

    @Test
    fun aLineWithAnEmptyVariableIsLeftOut() {
        val template = "Hi {name}\nPrevious pending: {previous_pending}\nTotal: {total}"

        assertEquals("Hi Ravi\nTotal: ₹10", MessageTemplate.fill(template, mapOf("name" to "Ravi", "previous_pending" to "", "total" to "₹10")))
    }

    @Test
    fun aValueOfSeveralLinesIsPutInAsItIs() {
        assertEquals("Bill:\nRent: ₹1\nWater: ₹2", MessageTemplate.fill("Bill:\n{bill_lines}", mapOf("bill_lines" to "Rent: ₹1\nWater: ₹2")))
    }

    @Test
    fun linesWithoutVariablesAndBlankLinesStay() {
        assertEquals("Hello\n\nThanks", MessageTemplate.fill("Hello\n\nThanks", emptyMap()))
    }

    @Test
    fun anUnknownVariableIsLeftAsTypedAndReported() {
        assertEquals("Hi {nmae}", MessageTemplate.fill("Hi {nmae}", mapOf("name" to "Ravi")))
        assertEquals(listOf("nmae", "amont"), MessageTemplate.unknownVariables("Hi {nmae} {name} {amont}", MessageKind.PaymentReceived))
        assertEquals(emptyList(), MessageTemplate.unknownVariables(MessageKind.PaymentReceived.default, MessageKind.PaymentReceived))
        assertEquals(listOf("Name"), MessageTemplate.unknownVariables("Hi {Name}", MessageKind.PaymentReceived))
    }

    @Test
    fun everyDefaultUsesOnlyItsOwnVariables() {
        MessageKind.entries.forEach { kind -> assertEquals(emptyList(), MessageTemplate.unknownVariables(kind.default, kind), kind.name) }
    }
}
