package com.rentman.sharedui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/** The colours of one status. The pill, a row's side bar and its amount all use these, so they can't drift apart. */
internal class StatusColors(val text: Color, val background: Color)

internal fun RenterStatus.colors(): StatusColors = when (this) {
    RenterStatus.NoBillYet -> StatusColors(RentManColors.TextMuted, RentManColors.Divider)
    RenterStatus.Paid -> StatusColors(RentManColors.Success, RentManColors.SuccessSoft)
    // Part paid is blue: money has started to come in. Unpaid is amber, so it isn't mistaken for a button.
    RenterStatus.PartPaid -> StatusColors(RentManColors.Primary, RentManColors.PrimarySoft)
    RenterStatus.Unpaid -> StatusColors(RentManColors.Warning, RentManColors.WarningSoft)
    RenterStatus.Late -> StatusColors(RentManColors.Danger, RentManColors.DangerSoft)
}

@Composable
internal fun RenterStatus.label(): String = stringResource(
    when (this) {
        RenterStatus.NoBillYet -> Res.string.status_no_bill
        RenterStatus.Paid -> Res.string.status_paid
        RenterStatus.PartPaid -> Res.string.status_part_paid
        RenterStatus.Unpaid -> Res.string.status_unpaid
        RenterStatus.Late -> Res.string.status_late
    },
)

/** A renter's status as a small coloured label. The status itself is decided in the domain. */
@Composable
internal fun StatusPill(status: RenterStatus, modifier: Modifier = Modifier) {
    val colors = status.colors()
    BasicText(
        text = status.label(),
        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text),
        modifier = modifier.background(colors.background, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
