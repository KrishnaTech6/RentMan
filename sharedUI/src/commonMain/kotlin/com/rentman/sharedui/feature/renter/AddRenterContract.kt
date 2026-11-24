package com.rentman.sharedui.feature.renter

import com.rentman.domain.DomainError
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.ChargeField
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.domain.tenancy.FixedCharge
import com.rentman.domain.tenancy.RenterField
import com.rentman.domain.tenancy.RenterForm
import com.rentman.domain.tenancy.RenterId
import kotlinx.datetime.LocalDate

internal sealed interface AddRenterIntent {
    data class NameChanged(val text: String) : AddRenterIntent
    data class PhoneChanged(val text: String) : AddRenterIntent
    data class UnitChanged(val text: String) : AddRenterIntent
    data class RentChanged(val text: String) : AddRenterIntent
    data class DueDayChanged(val text: String) : AddRenterIntent
    data class StartMonthStepped(val months: Int) : AddRenterIntent
    data class AskToRaiseChanged(val ask: Boolean) : AddRenterIntent
    data class BillsElectricityChanged(val bills: Boolean) : AddRenterIntent
    data class StartingReadingChanged(val text: String) : AddRenterIntent
    data class OwnRateChanged(val text: String) : AddRenterIntent
    data class ChargeLabelChanged(val text: String) : AddRenterIntent
    data class ChargeAmountChanged(val text: String) : AddRenterIntent
    data class AddCharge(val label: String, val amount: String) : AddRenterIntent
    data class RemoveCharge(val index: Int) : AddRenterIntent
    data object PickAddress : AddRenterIntent
    data class Save(val form: RenterForm) : AddRenterIntent
    data object Back : AddRenterIntent
    data object Retry : AddRenterIntent
}

internal sealed interface AddRenterUiState {
    data object Loading : AddRenterUiState

    data class Error(val error: DomainError) : AddRenterUiState

    data class Content(
        val addresses: List<Address>,
        /** Set when an existing renter is being edited. */
        val renterId: RenterId? = null,
        val addressId: AddressId? = null,
        val unit: String = "",
        val name: String = "",
        val phone: String = "",
        val rent: String = "",
        val dueDay: String = DEFAULT_DUE_DAY,
        val agreementStart: LocalDate,
        val askToRaise: Boolean = true,
        val billsElectricity: Boolean = true,
        val startingReading: String = "",
        val ownRate: String = "",
        val fixedCharges: List<FixedCharge> = emptyList(),
        val chargeLabel: String = "",
        val chargeAmount: String = "",
        val chargeProblems: Map<ChargeField, FieldProblem> = emptyMap(),
        val problems: Map<RenterField, FieldProblem> = emptyMap(),
        /** A failure that isn't about one field, e.g. the disk. The typed form is kept. */
        val saveError: DomainError? = null,
        val isSaving: Boolean = false,
    ) : AddRenterUiState {
        val address: Address? get() = addresses.firstOrNull { it.id == addressId }

        fun toForm() = RenterForm(
            addressId = addressId,
            unit = unit,
            name = name,
            phone = phone,
            rent = rent,
            dueDay = dueDay,
            agreementStart = agreementStart,
            askToRaise = askToRaise,
            billsElectricity = billsElectricity,
            startingReading = startingReading,
            ownRate = ownRate,
            fixedCharges = fixedCharges,
            chargeLabel = chargeLabel,
            chargeAmount = chargeAmount,
        )

        companion object {
            const val DEFAULT_DUE_DAY = "5"
        }
    }
}

internal sealed interface AddRenterNavEvent {
    data object PickAddress : AddRenterNavEvent
    data object Saved : AddRenterNavEvent
    data object Close : AddRenterNavEvent
}
