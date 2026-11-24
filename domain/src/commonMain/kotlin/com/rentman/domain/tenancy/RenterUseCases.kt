package com.rentman.domain.tenancy

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.flatMap
import com.rentman.domain.fold
import com.rentman.domain.map
import com.rentman.domain.money.MoneyInput
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ObserveRenterSummariesUseCase(
    private val renters: RenterRepository,
    private val addresses: AddressRepository,
) {
    operator fun invoke(): Flow<Outcome<List<RenterSummary>>> =
        combine(renters.observeActive(), addresses.observeAll()) { renterOutcome, addressOutcome ->
            renterOutcome.flatMap { renterList ->
                addressOutcome.map { addressList ->
                    // shortcut: a renter whose address is archived drops out of the list. Archiving isn't a feature yet; show them under an "archived" group when it is.
                    renterList.mapNotNull { renter ->
                        addressList.firstOrNull { it.id == renter.addressId }?.let { RenterSummary(renter, it) }
                    }
                }
            }
        }
}

class SaveRenterUseCase(
    private val renters: RenterRepository,
    private val today: GetTodayUseCase,
) {
    suspend operator fun invoke(form: RenterForm): Outcome<Renter> =
        renters.observeActive().first().fold(
            onSuccess = { active ->
                RenterFormValidator.validate(form, active, existing = null, firstBillMonth = BillingMonth.of(today()))
                    .fold(onSuccess = { renters.add(it) }, onFailure = { Outcome.Failure(it) })
            },
            onFailure = { Outcome.Failure(it) },
        )
}

/** Saves what was changed on the Edit renter screen. The checks are the same as when adding a renter. */
class UpdateRenterUseCase(private val renters: RenterRepository) {
    suspend operator fun invoke(renterId: RenterId, form: RenterForm): Outcome<Renter> =
        renters.observeAll().first().flatMap { all ->
            val existing = all.firstOrNull { it.id == renterId } ?: return@flatMap Outcome.Failure(DomainError.NotFound)
            RenterFormValidator.validate(form, all.filter { it.active }, existing, existing.firstBillMonth)
                .fold(onSuccess = { renters.update(it) }, onFailure = { Outcome.Failure(it) })
        }
}

/** The Edit renter form, filled in with what is saved. */
class ObserveRenterFormUseCase(private val renters: RenterRepository) {
    operator fun invoke(renterId: RenterId): Flow<Outcome<RenterForm>> = renters.observeAll().map { outcome ->
        outcome.flatMap { all -> all.firstOrNull { it.id == renterId }?.let { Outcome.Success(it.toForm()) } ?: Outcome.Failure(DomainError.NotFound) }
    }
}

/**
 * The form for a renter as it is saved, so editing starts from what is there. Only the charges still in use are shown.
 * A number from another country is shown with its "+", because that is how it has to be typed to be accepted again.
 */
fun Renter.toForm() = RenterForm(
    addressId = addressId,
    unit = unit.orEmpty(),
    name = name,
    phone = if (phone.startsWith(INDIA_CODE)) phone else "+$phone",
    rent = MoneyInput.text(monthlyRent),
    dueDay = dueDay.toString(),
    agreementStart = agreementStart,
    askToRaise = raisePercent > 0,
    billsElectricity = billsElectricity,
    startingReading = startingReading?.toString().orEmpty(),
    ownRate = customRate?.let(MoneyInput::text).orEmpty(),
    fixedCharges = fixedCharges.filter { it.active },
)

private const val INDIA_CODE = "91"

/** The rule that tells renters at one address apart: when others live there, a unit is needed and it must be different. */
internal object RenterUnits {
    fun problem(unit: String?, neighbours: List<Renter>): FieldProblem? = when {
        neighbours.isEmpty() -> null
        unit == null -> FieldProblem.Required
        neighbours.any { it.unit.equals(unit, ignoreCase = true) } -> FieldProblem.Duplicate
        else -> null
    }
}

/** Checks the Add and Edit renter form and builds the renter from it. */
internal object RenterFormValidator {
    private val DUE_DAYS = 1..31

    /**
     * [active] are the renters who live somewhere now. With an [existing] renter, the result is that renter with the form's
     * changes (and they don't count as their own neighbour). Without one, it is a new renter whose first bill is [firstBillMonth].
     */
    fun validate(form: RenterForm, active: List<Renter>, existing: Renter?, firstBillMonth: BillingMonth): Outcome<Renter> {
        val name = form.name.trim()
        val unit = form.unit.trim().ifEmpty { null }
        val phone = PhoneNumber.normalize(form.phone)
        val rent = MoneyInput.rupees(form.rent)
        val dueDay = form.dueDay.trim().toIntOrNull()?.takeIf { it in DUE_DAYS }
        val reading = form.startingReading.trim().toIntOrNull()?.takeIf { it >= 0 }
        val ownRate = MoneyInput.rate(form.ownRate)
        val neighbours = active.filter { it.addressId == form.addressId && it.id != existing?.id }

        val problems = listOf(
            RenterField.Address to (FieldProblem.Required).takeIf { form.addressId == null },
            RenterField.Unit to RenterUnits.problem(unit, neighbours),
            RenterField.Name to FieldProblem.Required.takeIf { name.isEmpty() },
            RenterField.Phone to problemOf(blank = form.phone.isBlank(), valid = phone != null),
            RenterField.Rent to problemOf(blank = form.rent.isBlank(), valid = rent != null),
            RenterField.DueDay to problemOf(blank = form.dueDay.isBlank(), valid = dueDay != null),
            RenterField.StartingReading to
                problemOf(blank = form.startingReading.isBlank(), valid = reading != null).takeIf { form.billsElectricity },
            RenterField.OwnRate to
                FieldProblem.Invalid.takeIf { form.billsElectricity && form.ownRate.isNotBlank() && ownRate == null },
            // A charge that was typed but never added would be silently dropped, and never billed.
            RenterField.Charge to FieldProblem.Invalid.takeIf { form.chargeLabel.isNotBlank() || form.chargeAmount.isNotBlank() },
        ).mapNotNull { (field, problem) -> problem?.let { field to it } }.toMap()

        if (problems.isNotEmpty() || form.addressId == null || phone == null || rent == null || dueDay == null) {
            return Outcome.Failure(DomainError.InvalidRenter(problems))
        }
        val raisePercent = if (form.askToRaise) {
            existing?.raisePercent?.takeIf { it > 0 } ?: RentIncreasePolicy.DEFAULT_PERCENT
        } else {
            0
        }
        val renter = (existing ?: Renter(
            addressId = form.addressId, unit = unit, name = name, phone = phone, monthlyRent = rent, dueDay = dueDay,
            agreementStart = form.agreementStart, raisePercent = raisePercent, raiseEveryMonths = RentIncreasePolicy.DEFAULT_EVERY_MONTHS,
            billsElectricity = form.billsElectricity, customRate = null, startingReading = null, firstBillMonth = firstBillMonth,
        )).copy(
            addressId = form.addressId,
            unit = unit,
            name = name,
            phone = phone,
            monthlyRent = rent,
            dueDay = dueDay,
            agreementStart = form.agreementStart,
            raisePercent = raisePercent,
            billsElectricity = form.billsElectricity,
            customRate = ownRate.takeIf { form.billsElectricity },
            startingReading = reading.takeIf { form.billsElectricity },
            fixedCharges = form.fixedCharges,
        )
        return Outcome.Success(renter)
    }

    private fun problemOf(blank: Boolean, valid: Boolean): FieldProblem? = when {
        blank -> FieldProblem.Required
        !valid -> FieldProblem.Invalid
        else -> null
    }
}
