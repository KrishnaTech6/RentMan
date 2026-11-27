package com.rentman.sharedui.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentman.domain.DomainError
import com.rentman.domain.messages.MessageKind
import com.rentman.sharedui.components.ActionButton
import com.rentman.sharedui.components.CenteredMessage
import com.rentman.sharedui.components.ErrorMessage
import com.rentman.sharedui.components.ScreenTopBar
import com.rentman.sharedui.components.TextAction
import com.rentman.sharedui.components.kindLabel
import com.rentman.sharedui.components.message
import com.rentman.sharedui.feature.messages.MessageEditIntent.Back
import com.rentman.sharedui.feature.messages.MessageEditIntent.InsertVariable
import com.rentman.sharedui.feature.messages.MessageEditIntent.Retry
import com.rentman.sharedui.feature.messages.MessageEditIntent.Save
import com.rentman.sharedui.feature.messages.MessageEditIntent.TextChanged
import com.rentman.sharedui.feature.messages.MessageEditIntent.UseOurWording
import com.rentman.sharedui.feature.messages.MessageEditUiState.Content
import com.rentman.sharedui.feature.messages.MessageEditUiState.Error
import com.rentman.sharedui.feature.messages.MessageEditUiState.Loading
import com.rentman.sharedui.mvi.NavigationEffect
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.loading
import com.rentman.sharedui.resources.message_preview
import com.rentman.sharedui.resources.message_save
import com.rentman.sharedui.resources.message_unknown
import com.rentman.sharedui.resources.message_use_ours
import com.rentman.sharedui.resources.message_variables
import com.rentman.sharedui.resources.messages_hint
import com.rentman.sharedui.theme.RentManColors
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The only composable that knows the ViewModel (ARCHITECTURE.md §5). */
@Composable
internal fun MessageEditRoute(
    kind: MessageKind,
    onDone: () -> Unit,
    viewModel: MessageEditViewModel = koinViewModel { parametersOf(kind.name) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NavigationEffect(viewModel.navigation) { onDone() }
    MessageEditScreen(kind, state, viewModel::onIntent)
}

@Composable
internal fun MessageEditScreen(kind: MessageKind, state: MessageEditUiState, onIntent: (MessageEditIntent) -> Unit) {
    Column(Modifier.fillMaxSize().background(RentManColors.Background)) {
        ScreenTopBar(title = kind.kindLabel(), onBack = { onIntent(Back) })
        when (state) {
            Loading -> CenteredMessage(stringResource(Res.string.loading))
            is Error -> ErrorMessage(state.error, onRetry = { onIntent(Retry) })
            is Content -> Editor(state, onIntent)
        }
    }
}

@Composable
private fun Editor(state: Content, onIntent: (MessageEditIntent) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BasicText(stringResource(Res.string.messages_hint), style = TextStyle(fontSize = 14.sp, color = RentManColors.TextMuted))
            MessageField(state, onIntent)
            if (state.unknownVariables.isNotEmpty()) {
                val names = state.unknownVariables.joinToString { "{$it}" }
                BasicText(stringResource(Res.string.message_unknown, names), style = TextStyle(fontSize = 14.sp, color = RentManColors.Danger))
            }
            state.saveError?.let { BasicText(it.message(), style = TextStyle(fontSize = 14.sp, color = RentManColors.Danger)) }
            Label(stringResource(Res.string.message_variables))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.kind.variables.forEach { name -> VariableChip(name, onClick = { onIntent(InsertVariable(name)) }) }
            }
            Label(stringResource(Res.string.message_preview))
            BasicText(
                state.preview,
                style = TextStyle(fontSize = 15.sp, color = RentManColors.Text),
                modifier = Modifier.fillMaxWidth().background(RentManColors.PrimarySoft, RoundedCornerShape(12.dp)).padding(14.dp),
            )
            TextAction(stringResource(Res.string.message_use_ours)) { onIntent(UseOurWording) }
        }
        ActionButton(
            text = stringResource(Res.string.message_save),
            enabled = !state.isSaving,
            onClick = { onIntent(Save(state.text)) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

/**
 * The message box. It keeps the phone keyboard's own state (an unfinished word, where the cursor is) while the landlord types, even
 * when the ViewModel's copy of the text is a moment behind; it starts from the ViewModel's text only when the app itself changed it.
 */
@Composable
private fun MessageField(state: Content, onIntent: (MessageEditIntent) -> Unit) {
    var field by remember(state.replaced) { mutableStateOf(TextFieldValue(state.text, TextRange(state.cursor))) }
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = field,
        onValueChange = {
            field = it
            onIntent(TextChanged(it.text, it.selection.start))
        },
        textStyle = TextStyle(fontSize = 16.sp, color = RentManColors.Text),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .background(RentManColors.Surface, shape)
            .border(1.dp, RentManColors.Border, shape)
            .padding(14.dp),
    )
}

@Composable
private fun Label(text: String) {
    BasicText(text, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.TextMuted), modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun VariableChip(name: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(RentManColors.Surface, shape)
            .border(1.dp, RentManColors.Border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText("{$name}", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Primary))
    }
}

private val previewContent = Content(MessageKind.PaymentReceived, "{name} ne {amount} diya")

@Preview
@Composable
private fun LoadingPreview() = MessageEditScreen(MessageKind.Bill, Loading) {}

@Preview
@Composable
private fun ErrorPreview() = MessageEditScreen(MessageKind.Bill, Error(DomainError.Storage("disk"))) {}

@Preview
@Composable
private fun EditingPreview() = MessageEditScreen(MessageKind.PaymentReceived, previewContent) {}

@Preview
@Composable
private fun UnknownVariablePreview() = MessageEditScreen(MessageKind.PaymentReceived, previewContent.copy(text = "Paid {amont}", unknownVariables = listOf("amont"))) {}
