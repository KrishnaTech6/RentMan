package com.rentman.domain.tenancy

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.money.rupees
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildFixedChargeUseCaseTest {
    private val build = BuildFixedChargeUseCase()

    @Test
    fun buildsAChargeFromTypedText() {
        assertEquals(
            Outcome.Success(FixedCharge(label = "Water", amount = 200L.rupees)),
            build("  Water ", "₹ 200"),
        )
    }

    @Test
    fun collapsesInnerSpacesInTheLabel() {
        assertEquals("Garbage pickup", (build("Garbage    pickup", "50") as Outcome.Success).value.label)
    }

    @Test
    fun aBlankLabelOrAmountIsRequired() {
        assertEquals(
            Outcome.Failure(DomainError.InvalidCharge(mapOf(ChargeField.Label to FieldProblem.Required, ChargeField.Amount to FieldProblem.Required))),
            build(" ", ""),
        )
    }

    @Test
    fun aBadAmountIsInvalid() {
        assertEquals(
            Outcome.Failure(DomainError.InvalidCharge(mapOf(ChargeField.Amount to FieldProblem.Invalid))),
            build("Water", "0"),
        )
        assertEquals(
            Outcome.Failure(DomainError.InvalidCharge(mapOf(ChargeField.Amount to FieldProblem.Invalid))),
            build("Water", "20.5"),
        )
    }

    @Test
    fun aNameAlreadyOnTheListIsADuplicateIgnoringCase() {
        val existing = listOf(FixedCharge(label = "Water", amount = 200L.rupees))

        assertEquals(
            Outcome.Failure(DomainError.InvalidCharge(mapOf(ChargeField.Label to FieldProblem.Duplicate))),
            build(" water ", "200", existing),
        )
        assertEquals(
            Outcome.Success(FixedCharge(label = "Garbage", amount = 50L.rupees)),
            build("Garbage", "50", existing),
        )
    }

    @Test
    fun aVeryLongLabelIsInvalid() {
        assertEquals(
            Outcome.Failure(DomainError.InvalidCharge(mapOf(ChargeField.Label to FieldProblem.Invalid))),
            build("x".repeat(41), "100"),
        )
    }
}
