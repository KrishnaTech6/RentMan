package com.rentman.sharedui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.back
import com.rentman.sharedui.theme.RentManColors
import org.jetbrains.compose.resources.stringResource

/** The back arrow and title at the top of a screen that was opened from another one. */
@Composable
internal fun ScreenTopBar(title: String, onBack: () -> Unit, action: Pair<String, () -> Unit>? = null) {
    val backLabel = stringResource(Res.string.back)
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = backLabel },
            contentAlignment = Alignment.Center,
        ) {
            BasicText("‹", style = TextStyle(fontSize = 32.sp, color = RentManColors.Text))
        }
        BasicText(
            text = title,
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text),
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        action?.let { (label, onClick) ->
            Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                BasicText(label, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Primary))
            }
        }
    }
}
