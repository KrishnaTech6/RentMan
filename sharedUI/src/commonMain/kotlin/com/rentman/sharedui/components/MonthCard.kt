package com.rentman.sharedui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentman.domain.billing.MonthSummary
import com.rentman.domain.money.Paise
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.all_collected
import com.rentman.sharedui.resources.all_months
import com.rentman.sharedui.resources.from_earlier
import com.rentman.sharedui.resources.legend_late
import com.rentman.sharedui.resources.legend_not_billed
import com.rentman.sharedui.resources.legend_not_due
import com.rentman.sharedui.resources.month_received
import com.rentman.sharedui.resources.no_bills_month
import com.rentman.sharedui.resources.of_expected
import com.rentman.sharedui.theme.RentManColors
import org.jetbrains.compose.resources.stringResource

/**
 * How a month is going: what has come in out of what is expected, one bar for the parts, and what is left.
 * With [onOpenMonths] the whole card is a button to the list of months (Home); without it the card only shows (a month's screen).
 */
@Composable
internal fun MonthCard(month: MonthSummary, onOpenMonths: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(20.dp)
    val soft = RentManColors.OnPrimary.copy(alpha = 0.7f)
    val note = TextStyle(fontSize = 13.sp, color = soft)
    val allMonths = stringResource(Res.string.all_months)
    val tap = if (onOpenMonths != null) Modifier.clickable(role = Role.Button, onClickLabel = allMonths, onClick = onOpenMonths) else Modifier
    Column(
        Modifier.fillMaxWidth().clip(shape).background(RentManColors.HeroCard, shape).then(tap).padding(20.dp).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val label = if (month.billed > Paise.Zero) stringResource(Res.string.month_received, month.month.label()) else ""
            BasicText(label, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = soft), modifier = Modifier.weight(1f))
            if (onOpenMonths != null) {
                BasicText("$allMonths ›", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.OnPrimary))
            }
        }
        if (month.billed > Paise.Zero) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountText(
                    month.collected,
                    style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = RentManColors.OnPrimary),
                    modifier = Modifier.alignByBaseline(),
                )
                BasicText(
                    stringResource(Res.string.of_expected, formatRupees(month.expected)),
                    style = TextStyle(fontSize = 15.sp, color = soft),
                    modifier = Modifier.alignByBaseline(),
                )
            }
            MonthBar(month)
        } else {
            BasicText(stringResource(Res.string.no_bills_month, month.month.label()), style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = RentManColors.OnPrimary))
        }
        if (month.billed > Paise.Zero || month.notBilled > 0 || month.earlier > Paise.Zero) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(RentManColors.OnPrimary.copy(alpha = 0.12f)))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (month.notDue > Paise.Zero) LegendRow(RentManColors.HeroDue, stringResource(Res.string.legend_not_due), month.notDue)
                if (month.late > Paise.Zero) LegendRow(RentManColors.HeroLate, stringResource(Res.string.legend_late), month.late)
                if (month.notBilled > 0) LegendRow(NotBilledColor, stringResource(Res.string.legend_not_billed, month.notBilled), month.notBilledRent)
                if (month.billed > Paise.Zero && month.late + month.notDue == Paise.Zero) {
                    BasicText(stringResource(Res.string.all_collected, month.month.label()), style = note)
                }
                if (month.earlier > Paise.Zero) {
                    BasicText(stringResource(Res.string.from_earlier, formatRupees(month.earlier)), style = note, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** Rent of bills not made yet: a quiet, hollow-looking part of the bar. */
private val NotBilledColor = RentManColors.OnPrimary.copy(alpha = 0.35f)

/** One bar for the whole month: received, not due yet, late, and the rent of bills not made yet. */
@Composable
private fun MonthBar(month: MonthSummary) {
    val parts = listOf(
        month.collected to RentManColors.HeroReceived,
        month.notDue to RentManColors.HeroDue,
        month.late to RentManColors.HeroLate,
        month.notBilledRent to NotBilledColor,
    ).filter { (amount, _) -> amount > Paise.Zero }
    Row(
        Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(RentManColors.HeroTrack),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        parts.forEach { (amount, color) -> Box(Modifier.weight(amount.value.toFloat()).fillMaxHeight().background(color)) }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, amount: Paise) {
    Row(Modifier.heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        BasicText(label, style = TextStyle(fontSize = 13.sp, color = RentManColors.OnPrimary.copy(alpha = 0.8f)), modifier = Modifier.weight(1f))
        AmountText(amount, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.OnPrimary))
    }
}
