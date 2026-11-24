package com.rentman.sharedui.feature.renter

import com.rentman.sharedui.components.Section
import com.rentman.sharedui.components.Divider
import com.rentman.sharedui.components.TextRow
import com.rentman.sharedui.components.StepButton
import com.rentman.sharedui.components.Segmented
import com.rentman.sharedui.components.CenteredMessage
import com.rentman.sharedui.components.ErrorMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentman.domain.DomainError
import com.rentman.domain.money.Paise
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.ChargeField
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.domain.tenancy.FixedCharge
import com.rentman.domain.tenancy.RenterField
import com.rentman.domain.tenancy.RenterId
import com.rentman.sharedui.components.ActionButton
import com.rentman.sharedui.components.ScreenTopBar
import com.rentman.sharedui.components.AmountText
import com.rentman.sharedui.components.message
import com.rentman.sharedui.feature.renter.AddRenterIntent.AddCharge
import com.rentman.sharedui.feature.renter.AddRenterIntent.AskToRaiseChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.Back
import com.rentman.sharedui.feature.renter.AddRenterIntent.BillsElectricityChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.ChargeAmountChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.ChargeLabelChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.DueDayChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.NameChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.OwnRateChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.PhoneChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.PickAddress
import com.rentman.sharedui.feature.renter.AddRenterIntent.RemoveCharge
import com.rentman.sharedui.feature.renter.AddRenterIntent.RentChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.Retry
import com.rentman.sharedui.feature.renter.AddRenterIntent.Save
import com.rentman.sharedui.feature.renter.AddRenterIntent.StartMonthStepped
import com.rentman.sharedui.feature.renter.AddRenterIntent.StartingReadingChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.UnitChanged
import com.rentman.sharedui.feature.renter.AddRenterUiState.Content
import com.rentman.sharedui.feature.renter.AddRenterUiState.Error
import com.rentman.sharedui.feature.renter.AddRenterUiState.Loading
import com.rentman.sharedui.mvi.NavigationEffect
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.add_charge
import com.rentman.sharedui.resources.add_renter
import com.rentman.sharedui.resources.ask_to_raise
import com.rentman.sharedui.resources.ask_to_raise_hint
import com.rentman.sharedui.resources.field_address
import com.rentman.sharedui.resources.field_charge_amount
import com.rentman.sharedui.resources.field_charge_label
import com.rentman.sharedui.resources.field_address_placeholder
import com.rentman.sharedui.resources.field_agreement_start
import com.rentman.sharedui.resources.field_due_day
import com.rentman.sharedui.resources.field_name
import com.rentman.sharedui.resources.field_own_rate
import com.rentman.sharedui.resources.field_own_rate_hint
import com.rentman.sharedui.resources.field_phone
import com.rentman.sharedui.resources.field_rent
import com.rentman.sharedui.resources.field_starting_reading
import com.rentman.sharedui.resources.field_unit
import com.rentman.sharedui.resources.i_bill_them
import com.rentman.sharedui.resources.loading
import com.rentman.sharedui.resources.months_short
import com.rentman.sharedui.resources.next_month
import com.rentman.sharedui.resources.previous_month
import com.rentman.sharedui.resources.problem_charge_amount
import com.rentman.sharedui.resources.problem_charge_duplicate
import com.rentman.sharedui.resources.problem_charge_label_long
import com.rentman.sharedui.resources.problem_charge_unfinished
import com.rentman.sharedui.resources.problem_due_day
import com.rentman.sharedui.resources.problem_phone
import com.rentman.sharedui.resources.problem_rate
import com.rentman.sharedui.resources.problem_reading
import com.rentman.sharedui.resources.problem_rent
import com.rentman.sharedui.resources.problem_required
import com.rentman.sharedui.resources.problem_unit_duplicate
import com.rentman.sharedui.resources.problems_summary
import com.rentman.sharedui.resources.problem_unit_required
import com.rentman.sharedui.resources.remove_charge
import com.rentman.sharedui.resources.edit_renter_title
import com.rentman.sharedui.resources.save_changes
import com.rentman.sharedui.resources.save_renter
import com.rentman.sharedui.resources.section_charges
import com.rentman.sharedui.resources.section_electricity
import com.rentman.sharedui.resources.section_rent
import com.rentman.sharedui.resources.section_who
import com.rentman.sharedui.resources.they_pay_directly
import com.rentman.sharedui.theme.RentManColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The only composable that knows the ViewModel (ARCHITECTURE.md §5). */
@Composable
internal fun AddRenterRoute(
    /** Null to add a renter, or the renter to edit. */
    renterId: RenterId?,
    onPickAddress: () -> Unit,
    onDone: () -> Unit,
    viewModel: AddRenterViewModel = koinViewModel { parametersOf(renterId?.value) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NavigationEffect(viewModel.navigation) { event ->
        when (event) {
            AddRenterNavEvent.PickAddress -> onPickAddress()
            AddRenterNavEvent.Saved, AddRenterNavEvent.Close -> onDone()
        }
    }
    AddRenterScreen(state, editing = renterId != null, onIntent = viewModel::onIntent)
}

@Composable
internal fun AddRenterScreen(state: AddRenterUiState, editing: Boolean = false, onIntent: (AddRenterIntent) -> Unit) {
    Column(Modifier.fillMaxSize().background(RentManColors.Background)) {
        ScreenTopBar(title = stringResource(if (editing) Res.string.edit_renter_title else Res.string.add_renter), onBack = { onIntent(Back) })
        when (state) {
            Loading -> CenteredMessage(stringResource(Res.string.loading))
            is Error -> ErrorMessage(state.error, onRetry = { onIntent(Retry) })
            is Content -> Form(state, onIntent, editing)
        }
    }
}

@Composable
private fun ColumnScope.Form(state: Content, onIntent: (AddRenterIntent) -> Unit, editing: Boolean) {
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Section(stringResource(Res.string.section_who)) {
            TextRow(stringResource(Res.string.field_name), state.name, { onIntent(NameChanged(it)) }, problem(state, RenterField.Name))
            Divider()
            TextRow(
                stringResource(Res.string.field_phone), state.phone, { onIntent(PhoneChanged(it)) },
                problem(state, RenterField.Phone), KeyboardType.Phone,
            )
            Divider()
            AddressRow(state.address, problem(state, RenterField.Address), onClick = { onIntent(PickAddress) })
            Divider()
            TextRow(stringResource(Res.string.field_unit), state.unit, { onIntent(UnitChanged(it)) }, problem(state, RenterField.Unit))
        }
        Section(stringResource(Res.string.section_rent)) {
            TextRow(
                stringResource(Res.string.field_rent), state.rent, { onIntent(RentChanged(it)) },
                problem(state, RenterField.Rent), KeyboardType.Number,
            )
            Divider()
            TextRow(
                stringResource(Res.string.field_due_day), state.dueDay, { onIntent(DueDayChanged(it)) },
                problem(state, RenterField.DueDay), KeyboardType.Number,
            )
            Divider()
            MonthStepper(state.agreementStart, onStep = { onIntent(StartMonthStepped(it)) })
            Divider()
            SwitchRow(
                label = stringResource(Res.string.ask_to_raise),
                hint = stringResource(Res.string.ask_to_raise_hint),
                checked = state.askToRaise,
                onChange = { onIntent(AskToRaiseChanged(it)) },
            )
        }
        Section(stringResource(Res.string.section_electricity)) {
            Segmented(
                options = listOf(stringResource(Res.string.i_bill_them), stringResource(Res.string.they_pay_directly)),
                selectedIndex = if (state.billsElectricity) 0 else 1,
                onSelect = { onIntent(BillsElectricityChanged(it == 0)) },
            )
            if (state.billsElectricity) {
                Divider()
                TextRow(
                    stringResource(Res.string.field_starting_reading), state.startingReading, { onIntent(StartingReadingChanged(it)) },
                    problem(state, RenterField.StartingReading), KeyboardType.Number,
                )
                Divider()
                TextRow(
                    stringResource(Res.string.field_own_rate), state.ownRate, { onIntent(OwnRateChanged(it)) },
                    problem(state, RenterField.OwnRate), KeyboardType.Decimal, hint = stringResource(Res.string.field_own_rate_hint),
                )
            }
        }
        ChargesSection(state, onIntent)
    }
    // The form scrolls, so the first problem can be out of sight. This line makes a failed Save visible.
    val failure = state.saveError?.message()
        ?: stringResource(Res.string.problems_summary).takeIf { state.problems.isNotEmpty() }
    failure?.let {
        BasicText(
            it,
            style = TextStyle(fontSize = 14.sp, color = RentManColors.Danger),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
    }
    ActionButton(
        text = stringResource(if (editing) Res.string.save_changes else Res.string.save_renter),
        enabled = !state.isSaving,
        onClick = { onIntent(Save(state.toForm())) },
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    )
}

/** The message under a field, or null when it has no problem. */
@Composable
private fun problem(state: Content, field: RenterField): String? =
    state.problems[field]?.let { problem ->
        stringResource(
            when {
                problem == FieldProblem.Duplicate -> Res.string.problem_unit_duplicate
                field == RenterField.Unit -> Res.string.problem_unit_required
                problem == FieldProblem.Required -> Res.string.problem_required
                field == RenterField.Phone -> Res.string.problem_phone
                field == RenterField.Rent -> Res.string.problem_rent
                field == RenterField.DueDay -> Res.string.problem_due_day
                field == RenterField.StartingReading -> Res.string.problem_reading
                else -> Res.string.problem_rate
            },
        )
    }




@Composable
private fun AddressRow(address: Address?, problem: String?, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(stringResource(Res.string.field_address), style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = address?.name ?: stringResource(Res.string.field_address_placeholder),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (address == null) RentManColors.TextMuted else RentManColors.Text,
                ),
                modifier = Modifier.weight(1f),
            )
            BasicText("›", style = TextStyle(fontSize = 22.sp, color = RentManColors.TextMuted))
        }
        if (problem != null) BasicText(problem, style = TextStyle(fontSize = 13.sp, color = RentManColors.Danger))
    }
}

@Composable
private fun MonthStepper(start: LocalDate, onStep: (Int) -> Unit) {
    val months = stringArrayResource(Res.array.months_short)
    val previous = stringResource(Res.string.previous_month)
    val next = stringResource(Res.string.next_month)
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            stringResource(Res.string.field_agreement_start),
            style = TextStyle(fontSize = 15.sp, color = RentManColors.TextMuted),
            modifier = Modifier.weight(1f),
        )
        StepButton("‹", previous) { onStep(-1) }
        BasicText(
            text = "${months[start.month.ordinal]} ${start.year}",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text),
            modifier = Modifier.widthIn(min = 84.dp),
        )
        StepButton("›", next) { onStep(1) }
    }
}


@Composable
private fun SwitchRow(label: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(label, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text))
            BasicText(hint, style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted))
        }
        Box(
            Modifier.size(width = 52.dp, height = 30.dp).background(if (checked) RentManColors.Primary else RentManColors.BorderStrong, CircleShape).padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.size(24.dp).background(RentManColors.Surface, CircleShape))
        }
    }
}



@Composable
private fun ChargesSection(state: Content, onIntent: (AddRenterIntent) -> Unit) {
    Section(stringResource(Res.string.section_charges)) {
        state.fixedCharges.forEachIndexed { index, charge ->
            ChargeRow(charge, onRemove = { onIntent(RemoveCharge(index)) })
            Divider()
        }
        TextRow(
            stringResource(Res.string.field_charge_label), state.chargeLabel, { onIntent(ChargeLabelChanged(it)) },
            chargeProblem(state, ChargeField.Label),
        )
        Divider()
        TextRow(
            stringResource(Res.string.field_charge_amount), state.chargeAmount, { onIntent(ChargeAmountChanged(it)) },
            chargeProblem(state, ChargeField.Amount), KeyboardType.Number,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(role = Role.Button) { onIntent(AddCharge(state.chargeLabel, state.chargeAmount)) },
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicText(
                stringResource(Res.string.add_charge),
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Primary),
            )
        }
        if (state.problems.containsKey(RenterField.Charge)) {
            BasicText(
                stringResource(Res.string.problem_charge_unfinished),
                style = TextStyle(fontSize = 13.sp, color = RentManColors.Danger),
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun ChargeRow(charge: FixedCharge, onRemove: () -> Unit) {
    val removeLabel = stringResource(Res.string.remove_charge, charge.label)
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            charge.label,
            style = TextStyle(fontSize = 15.sp, color = RentManColors.Text),
            modifier = Modifier.weight(1f),
        )
        AmountText(charge.amount, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text))
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onRemove)
                .semantics { contentDescription = removeLabel },
            contentAlignment = Alignment.Center,
        ) {
            BasicText("✕", style = TextStyle(fontSize = 16.sp, color = RentManColors.TextMuted))
        }
    }
}

@Composable
private fun chargeProblem(state: Content, field: ChargeField): String? =
    state.chargeProblems[field]?.let { problem ->
        stringResource(
            when {
                problem == FieldProblem.Required -> Res.string.problem_required
                problem == FieldProblem.Duplicate -> Res.string.problem_charge_duplicate
                field == ChargeField.Label -> Res.string.problem_charge_label_long
                else -> Res.string.problem_charge_amount
            },
        )
    }



private val previewAddress = Address(AddressId(1), "338 A")
private val previewContent = Content(
    addresses = listOf(previewAddress),
    agreementStart = LocalDate(2026, 11, 1),
    fixedCharges = listOf(FixedCharge(label = "Water", amount = Paise(20_000))),
)

@Preview
@Composable
private fun LoadingPreview() = AddRenterScreen(Loading) {}

@Preview
@Composable
private fun ErrorPreview() = AddRenterScreen(Error(DomainError.Storage("disk"))) {}

@Preview
@Composable
private fun EmptyFormPreview() = AddRenterScreen(previewContent) {}

@Preview
@Composable
private fun FilledWithProblemsPreview() = AddRenterScreen(
    previewContent.copy(
        addressId = previewAddress.id,
        name = "Ravi Kumar",
        phone = "123",
        unit = "Ground floor",
        problems = mapOf(RenterField.Phone to FieldProblem.Invalid, RenterField.Rent to FieldProblem.Required),
    ),
) {}
