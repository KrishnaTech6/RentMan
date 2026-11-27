package com.rentman.sharedui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal data object HomeKey : NavKey

@Serializable
internal data object AddRenterKey : NavKey

@Serializable
internal data object AddressPickerKey : NavKey

/** The bill for one renter and one month. [month] is the BillingMonth value (YYYYMM). */
@Serializable
internal data class CreateBillKey(val renterId: Long, val month: Int) : NavKey

/** One renter's balance and history. */
@Serializable
internal data class RenterKey(val renterId: Long) : NavKey

@Serializable
internal data class RecordPaymentKey(val renterId: Long) : NavKey

@Serializable
internal data object SettingsKey : NavKey

/** The Add renter form filled in with a renter who is already saved. */
@Serializable
internal data class EditRenterKey(val renterId: Long) : NavKey

/** The list of months, newest first. */
@Serializable
internal data object MonthsKey : NavKey

/** Backing up to the landlord's own Google Drive. */
@Serializable
internal data object DriveKey : NavKey

/** The landlord's own wording for one WhatsApp message. [kind] is the MessageKind's name. */
@Serializable
internal data class MessageEditKey(val kind: String) : NavKey

/** One month and every renter in it. [month] is the BillingMonth value (YYYYMM). */
@Serializable
internal data class MonthKey(val month: Int) : NavKey
