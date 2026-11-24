package com.rentman.sharedui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentman.domain.billing.RenterStatus
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.status_late
import com.rentman.sharedui.resources.status_no_bill
import com.rentman.sharedui.resources.status_paid
import com.rentman.sharedui.resources.status_part_paid
import com.rentman.sharedui.resources.status_unpaid
import com.rentman.sharedui.theme.RentManColors
import org.jetbrains.compose.resources.stringResource

/** A renter's status as a small coloured label. The status itself is decided in the domain. */
@Composable
internal fun StatusPill(status: RenterStatus, modifier: Modifier = Modifier) {
    val (label, text, background) = when (status) {
        RenterStatus.NoBillYet -> Triple(Res.string.status_no_bill, RentManColors.TextMuted, RentManColors.Divider)
        RenterStatus.Paid -> Triple(Res.string.status_paid, RentManColors.Success, RentManColors.SuccessSoft)
        RenterStatus.PartPaid -> Triple(Res.string.status_part_paid, RentManColors.Warning, RentManColors.WarningSoft)
        RenterStatus.Unpaid -> Triple(Res.string.status_unpaid, RentManColors.Primary, RentManColors.PrimarySoft)
        RenterStatus.Late -> Triple(Res.string.status_late, RentManColors.Danger, RentManColors.DangerSoft)
    }
    BasicText(
        text = stringResource(label),
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = text),
        modifier = modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
