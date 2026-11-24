package com.rentman.sharedui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentman.domain.DomainError
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.retry
import com.rentman.sharedui.theme.RentManColors
import org.jetbrains.compose.resources.stringResource

/** A short line in the middle of the screen, e.g. "Loading…". */
@Composable
internal fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(text, style = TextStyle(fontSize = 16.sp, color = RentManColors.TextMuted))
    }
}

/** What went wrong, with a Retry button. Used by every screen's Error state. */
@Composable
internal fun ErrorMessage(error: DomainError, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(error.message(), style = TextStyle(fontSize = 16.sp, color = RentManColors.Text))
        ActionButton(text = stringResource(Res.string.retry), enabled = true, onClick = onRetry)
    }
}
