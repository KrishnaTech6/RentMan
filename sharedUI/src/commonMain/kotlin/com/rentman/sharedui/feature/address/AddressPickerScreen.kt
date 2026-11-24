package com.rentman.sharedui.feature.address

import com.rentman.sharedui.components.CenteredMessage
import com.rentman.sharedui.components.ErrorMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentman.domain.DomainError
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.sharedui.components.ActionButton
import com.rentman.sharedui.components.message
import com.rentman.sharedui.feature.address.AddressPickerIntent.Add
import com.rentman.sharedui.feature.address.AddressPickerIntent.Confirm
import com.rentman.sharedui.feature.address.AddressPickerIntent.NameChanged
import com.rentman.sharedui.feature.address.AddressPickerIntent.Retry
import com.rentman.sharedui.feature.address.AddressPickerIntent.Select
import com.rentman.sharedui.feature.address.AddressPickerUiState.Content
import com.rentman.sharedui.feature.address.AddressPickerUiState.Error
import com.rentman.sharedui.feature.address.AddressPickerUiState.Loading
import com.rentman.sharedui.mvi.NavigationEffect
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.add
import com.rentman.sharedui.resources.choose_address
import com.rentman.sharedui.resources.loading
import com.rentman.sharedui.resources.new_address
import com.rentman.sharedui.resources.new_address_hint
import com.rentman.sharedui.resources.new_address_note
import com.rentman.sharedui.resources.pick_an_address
import com.rentman.sharedui.resources.use_address
import com.rentman.sharedui.theme.RentManColors
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The only composable that knows the ViewModel (ARCHITECTURE.md §5). */
@Composable
internal fun AddressPickerRoute(
    onPicked: (AddressId) -> Unit,
    viewModel: AddressPickerViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NavigationEffect(viewModel.navigation) { event ->
        when (event) {
            is AddressPickerNavEvent.Picked -> onPicked(event.id)
        }
    }
    AddressPickerScreen(state, viewModel::onIntent)
}

@Composable
internal fun AddressPickerScreen(state: AddressPickerUiState, onIntent: (AddressPickerIntent) -> Unit) {
    Column(Modifier.fillMaxSize().background(RentManColors.Background)) {
        BasicText(
            text = stringResource(Res.string.choose_address),
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        when (state) {
            Loading -> CenteredMessage(stringResource(Res.string.loading))
            is Error -> ErrorMessage(state.error, onRetry = { onIntent(Retry) })
            is Content -> ContentBody(state, onIntent)
        }
    }
}

@Composable
private fun ColumnScope.ContentBody(state: Content, onIntent: (AddressPickerIntent) -> Unit) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.addresses, key = { it.id.value }) { address ->
            AddressRow(address, selected = address.id == state.selectedId, onSelect = { onIntent(Select(address.id)) })
        }
        item { NewAddress(state, onIntent) }
    }
    val selected = state.selected
    ActionButton(
        text = if (selected == null) stringResource(Res.string.pick_an_address) else stringResource(Res.string.use_address, selected.name),
        enabled = selected != null,
        onClick = { selected?.let { onIntent(Confirm(it.id)) } },
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    )
}

@Composable
private fun AddressRow(address: Address, selected: Boolean, onSelect: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (selected) RentManColors.PrimarySoft else RentManColors.Surface)
            .border(1.dp, if (selected) RentManColors.Primary else RentManColors.Border, shape)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RadioDot(selected)
        BasicText(address.name, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text))
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier.size(22.dp).border(2.dp, if (selected) RentManColors.Primary else RentManColors.BorderStrong, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).background(RentManColors.Primary, CircleShape))
    }
}

@Composable
private fun NewAddress(state: Content, onIntent: (AddressPickerIntent) -> Unit) {
    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            text = stringResource(Res.string.new_address),
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RentManColors.TextMuted),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = state.newName,
                onValueChange = { onIntent(NameChanged(it)) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = RentManColors.Text),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onIntent(Add(state.newName)) }),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .background(RentManColors.Surface, RoundedCornerShape(12.dp))
                            .border(1.dp, if (state.nameError == null) RentManColors.BorderStrong else RentManColors.Danger, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (state.newName.isEmpty()) {
                            BasicText(stringResource(Res.string.new_address_hint), style = TextStyle(fontSize = 16.sp, color = RentManColors.TextMuted))
                        }
                        inner()
                    }
                },
            )
            ActionButton(
                text = stringResource(Res.string.add),
                enabled = !state.isSaving,
                onClick = { onIntent(Add(state.newName)) },
                color = RentManColors.Text,
                modifier = Modifier.heightIn(min = 52.dp),
            )
        }
        val error = state.nameError
        if (error != null) {
            BasicText(error.message(), style = TextStyle(fontSize = 13.sp, color = RentManColors.Danger), modifier = Modifier.padding(horizontal = 4.dp))
        } else {
            BasicText(
                stringResource(Res.string.new_address_note),
                style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}



private val previewAddresses = listOf("338 A", "338 B", "AIMS G-703").mapIndexed { i, name -> Address(AddressId(i + 1L), name) }

@Preview
@Composable
private fun LoadingPreview() = AddressPickerScreen(Loading) {}

@Preview
@Composable
private fun ErrorPreview() = AddressPickerScreen(Error(DomainError.Storage("disk"))) {}

@Preview
@Composable
private fun ContentPreview() =
    AddressPickerScreen(Content(previewAddresses, selectedId = AddressId(3), nameError = DomainError.DuplicateName("338 A"))) {}
