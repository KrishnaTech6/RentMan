package com.rentman.domain.tenancy

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.money.MoneyInput
import com.rentman.domain.money.Paise
import kotlin.jvm.JvmInline

@JvmInline
value class FixedChargeId(val value: Long) {
    companion object {
        /** The id of a charge that hasn't been saved yet. */
        val None = FixedChargeId(0)
    }
}

/** A recurring flat amount added to a renter's bill every month, e.g. Water ₹200. See GLOSSARY.md. */
data class FixedCharge(
    val id: FixedChargeId = FixedChargeId.None,
    val label: String,
    val amount: Paise,
    val active: Boolean = true,
)

enum class ChargeField { Label, Amount }

/**
 * Checks one charge as typed on the form, so the list on screen only ever holds valid charges.
 * [existing] are the charges already on the list: a renter can't have the same charge twice on a bill.
 */
class BuildFixedChargeUseCase {
    operator fun invoke(rawLabel: String, rawAmount: String, existing: List<FixedCharge> = emptyList()): Outcome<FixedCharge> {
        val label = rawLabel.trim().replace(WHITESPACE, " ")
        val amount = MoneyInput.rupees(rawAmount)
        val problems = listOfNotNull(
            ChargeField.Label to when {
                label.isEmpty() -> FieldProblem.Required
                label.length > MAX_LABEL_LENGTH -> FieldProblem.Invalid
                existing.any { it.label.equals(label, ignoreCase = true) } -> FieldProblem.Duplicate
                else -> null
            },
            ChargeField.Amount to when {
                rawAmount.isBlank() -> FieldProblem.Required
                amount == null -> FieldProblem.Invalid
                else -> null
            },
        ).mapNotNull { (field, problem) -> problem?.let { field to it } }.toMap()
        return if (problems.isEmpty() && amount != null) {
            Outcome.Success(FixedCharge(label = label, amount = amount))
        } else {
            Outcome.Failure(DomainError.InvalidCharge(problems))
        }
    }

    private companion object {
        const val MAX_LABEL_LENGTH = 40
        val WHITESPACE = Regex("\\s+")
    }
}
