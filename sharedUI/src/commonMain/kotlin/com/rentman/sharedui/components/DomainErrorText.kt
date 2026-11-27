package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import com.rentman.domain.DomainError
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.error_backup_invalid
import com.rentman.sharedui.resources.error_bill_not_removable
import com.rentman.sharedui.resources.error_backup_newer
import com.rentman.sharedui.resources.error_drive_not_connected
import com.rentman.sharedui.resources.error_drive_unavailable
import com.rentman.sharedui.resources.error_nothing_to_back_up
import com.rentman.sharedui.resources.error_bill_exists
import com.rentman.sharedui.resources.error_blank_name
import com.rentman.sharedui.resources.error_duplicate_name
import com.rentman.sharedui.resources.error_generic
import com.rentman.sharedui.resources.error_not_found
import com.rentman.sharedui.resources.error_offer_outdated
import com.rentman.sharedui.resources.error_still_owes
import com.rentman.sharedui.resources.error_storage
import com.rentman.sharedui.resources.error_unbilled_month
import com.rentman.sharedui.resources.error_unit_taken
import org.jetbrains.compose.resources.stringResource

/** The user-facing sentence for a domain error. Shared by every screen that shows one. */
@Composable
internal fun DomainError.message(): String = when (this) {
    is DomainError.StillOwes -> stringResource(Res.string.error_still_owes, formatRupees(pending))
    is DomainError.UnbilledMonth -> stringResource(Res.string.error_unbilled_month, month.label())
    else -> stringResource(messageResource())
}

private fun DomainError.messageResource() = when (this) {
    DomainError.BlankName -> Res.string.error_blank_name
    is DomainError.DuplicateName -> Res.string.error_duplicate_name
    is DomainError.Storage -> Res.string.error_storage
    DomainError.BillExists -> Res.string.error_bill_exists
    DomainError.NotFound -> Res.string.error_not_found
    DomainError.OfferOutdated -> Res.string.error_offer_outdated
    DomainError.UnitTaken -> Res.string.error_unit_taken
    DomainError.BillNotRemovable -> Res.string.error_bill_not_removable
    is DomainError.StillOwes, is DomainError.UnbilledMonth -> Res.string.error_generic
    DomainError.InvalidBackup -> Res.string.error_backup_invalid
    is DomainError.BackupFromNewerVersion -> Res.string.error_backup_newer
    DomainError.DriveNotConnected -> Res.string.error_drive_not_connected
    DomainError.DriveUnavailable -> Res.string.error_drive_unavailable
    DomainError.NothingToBackUp -> Res.string.error_nothing_to_back_up
    // Form problems are shown next to each field, so this is only a fallback.
    is DomainError.InvalidRenter, is DomainError.InvalidCharge, is DomainError.InvalidBill, is DomainError.InvalidPayment,
    is DomainError.InvalidSettings, is DomainError.InvalidMessage,
    is DomainError.ReadingBelowPrevious -> Res.string.error_generic
}
