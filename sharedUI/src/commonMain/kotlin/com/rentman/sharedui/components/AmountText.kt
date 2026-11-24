package com.rentman.sharedui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.rentman.domain.money.Paise

/** An amount of money as ₹ text. Formatting only; the number itself comes from the domain. */
@Composable
internal fun AmountText(amount: Paise, style: TextStyle, modifier: Modifier = Modifier) {
    BasicText(text = formatRupees(amount), style = style, modifier = modifier)
}
