package com.rentman.sharedui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentman.sharedui.theme.RentManColors

/** A tappable line inside a card with an optional explanation below it. */
@Composable
internal fun ActionRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, hint: String? = null, enabled: Boolean = true) {
    Column(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            title,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) RentManColors.Primary else RentManColors.Disabled),
        )
        hint?.let { BasicText(it, style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted)) }
    }
}
