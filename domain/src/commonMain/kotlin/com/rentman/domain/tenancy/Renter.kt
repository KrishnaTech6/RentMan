package com.rentman.domain.tenancy

import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import kotlinx.datetime.LocalDate
import kotlin.jvm.JvmInline

@JvmInline
value class RenterId(val value: Long) {
    companion object {
        /** The id of a renter that hasn't been saved yet. */
        val None = RenterId(0)
    }
}

/** A person who rents a unit and receives a monthly bill. See GLOSSARY.md. */
data class Renter(
    val id: RenterId = RenterId.None,
    val addressId: AddressId,
    val unit: String?,
    val name: String,
    /** Digits with country code, e.g. "919876543210". */
    val phone: String,
    val monthlyRent: Paise,
    val dueDay: Int,
    val agreementStart: LocalDate,
    /** 0 means never ask to raise the rent. */
    val raisePercent: Int,
    val raiseEveryMonths: Int,
    val raiseSnoozedUntil: BillingMonth? = null,
    val billsElectricity: Boolean,
    /** Null means use the global rate. */
    val customRate: Paise?,
    val startingReading: Int?,
    val firstBillMonth: BillingMonth,
    val remindersMutedMonth: BillingMonth? = null,
    val active: Boolean = true,
    val leftOn: LocalDate? = null,
    val fixedCharges: List<FixedCharge> = emptyList(),
)

/** A renter shown in a list, with the address they live at. */
data class RenterSummary(val renter: Renter, val address: Address)

/** What a person typed on the Add renter screen, before any checking. */
data class RenterForm(
    val addressId: AddressId?,
    val unit: String,
    val name: String,
    val phone: String,
    val rent: String,
    val dueDay: String,
    val agreementStart: LocalDate,
    val askToRaise: Boolean,
    val billsElectricity: Boolean,
    val startingReading: String,
    val ownRate: String,
    val fixedCharges: List<FixedCharge> = emptyList(),
    /** What is still typed in the "add a charge" fields. If either has text, the charge was never added. */
    val chargeLabel: String = "",
    val chargeAmount: String = "",
)

enum class RenterField { Address, Unit, Name, Phone, Rent, DueDay, StartingReading, OwnRate, Charge }

enum class FieldProblem { Required, Invalid, Duplicate }

/**
 * True while "Stop this month" still applies: the mute was set at the renter's latest bill or a newer one.
 * A newer bill ends it, so reminders resume with no clean-up. See DATABASE.md "Reminder mute".
 */
fun Renter.remindersMutedFor(latestBill: BillingMonth?): Boolean {
    val mutedAt = remindersMutedMonth ?: return false
    return latestBill != null && mutedAt >= latestBill
}
