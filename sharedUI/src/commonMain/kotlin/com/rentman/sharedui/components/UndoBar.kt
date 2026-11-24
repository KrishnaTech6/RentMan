package com.rentman.sharedui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.undo
import com.rentman.sharedui.theme.RentManColors
import org.jetbrains.compose.resources.stringResource

/** "Done: <message>" with an Undo button, shown for a few seconds instead of asking "Are you sure?" first. */
@Composable
internal fun UndoBar(message: String, onUndo: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(RentManColors.Text, shape).padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(message, style = TextStyle(fontSize = 14.sp, color = RentManColors.OnPrimary), modifier = Modifier.weight(1f))
        Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onUndo).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            BasicText(stringResource(Res.string.undo), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RentManColors.OnPrimary))
        }
    }
}
