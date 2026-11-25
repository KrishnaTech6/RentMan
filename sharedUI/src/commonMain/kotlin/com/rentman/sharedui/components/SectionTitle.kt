package com.rentman.sharedui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentman.sharedui.theme.RentManColors

/** A heading above a group of cards in a list. */
@Composable
internal fun SectionTitle(text: String) {
    BasicText(
        text = text,
        style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text),
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
    )
}
