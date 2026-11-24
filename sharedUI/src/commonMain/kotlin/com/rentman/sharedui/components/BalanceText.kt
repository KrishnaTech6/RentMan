package com.rentman.sharedui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.rentman.domain.money.Paise
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.balance_owes
import com.rentman.sharedui.resources.balance_paid_ahead
import com.rentman.sharedui.resources.balance_settled
import org.jetbrains.compose.resources.stringResource

/** A renter's balance in words: "Owes ₹3,380", "All paid" or "Paid ahead ₹1,000". The one place that reads the sign. */
@Composable
internal fun BalanceText(balance: Paise, style: TextStyle, modifier: Modifier = Modifier) {
    val text = when {
        balance > Paise.Zero -> stringResource(Res.string.balance_owes, formatRupees(balance))
        balance < Paise.Zero -> stringResource(Res.string.balance_paid_ahead, formatRupees(Paise.Zero - balance))
        else -> stringResource(Res.string.balance_settled)
    }
    BasicText(text, style = style, modifier = modifier)
}
