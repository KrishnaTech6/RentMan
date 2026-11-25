package com.rentman.sharedui.feature.home

import com.rentman.sharedui.components.ErrorMessage
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentman.domain.DomainError
import com.rentman.domain.billing.BillTodo
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.RenterRow
import com.rentman.domain.billing.RenterStatus
import com.rentman.domain.billing.MonthSummary
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.RentIncreaseOffer
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterSummary
import com.rentman.sharedui.components.ActionButton
import com.rentman.sharedui.components.AmountText
import com.rentman.sharedui.components.BalanceText
import com.rentman.sharedui.components.StatusPill
import com.rentman.sharedui.components.UndoBar
import com.rentman.sharedui.components.colors
import com.rentman.sharedui.components.formatRupees
import com.rentman.sharedui.components.label
import com.rentman.sharedui.components.message
import com.rentman.sharedui.components.shortLabel
import com.rentman.sharedui.components.shortName
import com.rentman.sharedui.feature.home.HomeIntent.AddRenter
import com.rentman.sharedui.feature.home.HomeIntent.CreateBill
import com.rentman.sharedui.feature.home.HomeIntent.NotNow
import com.rentman.sharedui.feature.home.HomeIntent.OpenRenter
import com.rentman.sharedui.feature.home.HomeIntent.RaiseRent
import com.rentman.sharedui.feature.home.HomeIntent.UndoRaise
import com.rentman.sharedui.feature.home.HomeIntent.OpenSettings
import com.rentman.sharedui.feature.home.HomeIntent.Retry
import com.rentman.sharedui.feature.home.HomeIntent.SelectFilter
import com.rentman.sharedui.feature.home.HomeUiState.Content
import com.rentman.sharedui.feature.home.HomeUiState.Error
import com.rentman.sharedui.feature.home.HomeUiState.Loading
import com.rentman.sharedui.mvi.NavigationEffect
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.add_renter
import com.rentman.sharedui.resources.all_renters
import com.rentman.sharedui.resources.create_action
import com.rentman.sharedui.resources.create_bill_card
import com.rentman.sharedui.resources.do_next
import com.rentman.sharedui.resources.all_collected
import com.rentman.sharedui.resources.from_earlier
import com.rentman.sharedui.resources.legend_late
import com.rentman.sharedui.resources.legend_not_billed
import com.rentman.sharedui.resources.legend_not_due
import com.rentman.sharedui.resources.month_received
import com.rentman.sharedui.resources.no_bills_month
import com.rentman.sharedui.resources.of_expected
import com.rentman.sharedui.resources.days_late_one
import com.rentman.sharedui.resources.days_late_other
import com.rentman.sharedui.resources.due_on
import com.rentman.sharedui.resources.filter_all
import com.rentman.sharedui.resources.paid_of
import com.rentman.sharedui.resources.left_on
import com.rentman.sharedui.resources.left_renters
import com.rentman.sharedui.resources.loading
import com.rentman.sharedui.resources.no_renters
import com.rentman.sharedui.resources.not_now
import com.rentman.sharedui.resources.raise_action
import com.rentman.sharedui.resources.raise_card_detail
import com.rentman.sharedui.resources.raise_card_title
import com.rentman.sharedui.resources.rent_raised
import com.rentman.sharedui.resources.no_renters_hint
import com.rentman.sharedui.resources.renters_title
import com.rentman.sharedui.resources.settings_open
import com.rentman.sharedui.theme.RentManColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The only composable that knows the ViewModel (ARCHITECTURE.md §5). */
@Composable
internal fun HomeRoute(
    onAddRenter: () -> Unit,
    onCreateBill: (RenterId, BillingMonth) -> Unit,
    onOpenRenter: (RenterId) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(HomeIntent.Refresh) }
    NavigationEffect(viewModel.navigation) { event ->
        when (event) {
            HomeNavEvent.OpenAddRenter -> onAddRenter()
            is HomeNavEvent.OpenCreateBill -> onCreateBill(event.renterId, event.month)
            is HomeNavEvent.OpenRenter -> onOpenRenter(event.renterId)
            HomeNavEvent.OpenSettings -> onOpenSettings()
        }
    }
    HomeScreen(state, viewModel::onIntent)
}

@Composable
internal fun HomeScreen(state: HomeUiState, onIntent: (HomeIntent) -> Unit) {
    Column(Modifier.fillMaxSize().background(RentManColors.Background)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = stringResource(Res.string.renters_title),
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text),
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { onIntent(OpenSettings) }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    stringResource(Res.string.settings_open),
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Primary),
                )
            }
        }
        when (state) {
            Loading -> Centered { BasicText(stringResource(Res.string.loading), style = mutedStyle) }
            is Error -> ErrorMessage(state.error, onRetry = { onIntent(Retry) })
            is Content -> ContentBody(state, onIntent)
        }
    }
}

@Composable
private fun ColumnScope.ContentBody(state: Content, onIntent: (HomeIntent) -> Unit) {
    if (state.renters.isEmpty() && state.left.isEmpty()) {
        Centered(Modifier.weight(1f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(stringResource(Res.string.no_renters), style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text))
                BasicText(stringResource(Res.string.no_renters_hint), style = mutedStyle)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "this-month") { MonthCard(state.thisMonth) }
            if (state.todos.isNotEmpty() || state.raises.isNotEmpty()) {
                item(key = "do-next") { SectionTitle(stringResource(Res.string.do_next)) }
                items(state.raises, key = { "raise-${it.summary.renter.id.value}" }) { offer ->
                    RaiseRentCard(
                        offer = offer,
                        onRaise = { onIntent(RaiseRent(offer)) },
                        onNotNow = { onIntent(NotNow(offer.summary.renter.id)) },
                    )
                }
                items(state.todos, key = { "todo-${it.summary.renter.id.value}" }) { todo ->
                    BillTodoCard(todo, onCreate = { onIntent(CreateBill(todo.summary.renter.id, todo.month)) })
                }
                item(key = "all-renters") { SectionTitle(stringResource(Res.string.all_renters)) }
            }
            if (state.statusCounts.size > 1) {
                item(key = "filters") { StatusChips(state.statusCounts, state.renters.size, state.filter, onSelect = { onIntent(SelectFilter(it)) }) }
            }
            items(state.shown, key = { it.summary.renter.id.value }) { row ->
                RenterRowCard(row, onClick = { onIntent(OpenRenter(row.summary.renter.id)) })
            }
            // A renter who left has no status, so they would not belong in a filtered list.
            if (state.left.isNotEmpty() && state.filter == null) {
                item(key = "left-renters") { SectionTitle(stringResource(Res.string.left_renters)) }
                items(state.left, key = { "left-${it.renter.id.value}" }) { summary ->
                    LeftRenterRow(summary, onClick = { onIntent(OpenRenter(summary.renter.id)) })
                }
            }
        }
    }
    state.actionError?.let { error ->
        BasicText(error.message(), style = TextStyle(fontSize = 14.sp, color = RentManColors.Danger), modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    }
    state.raiseUndo?.let { change ->
        UndoBar(stringResource(Res.string.rent_raised, formatRupees(change.newRent)), onUndo = { onIntent(UndoRaise(change)) })
    }
    ActionButton(
        text = stringResource(Res.string.add_renter),
        enabled = true,
        onClick = { onIntent(AddRenter) },
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    )
}

@Composable
private fun SectionTitle(text: String) {
    BasicText(
        text = text,
        style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text),
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun BillTodoCard(todo: BillTodo, onCreate: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val where = listOfNotNull(todo.summary.renter.unit, todo.summary.address.name).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(RentManColors.Surface, shape)
            .border(1.dp, RentManColors.Border, shape)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                stringResource(Res.string.create_bill_card, todo.month.label()),
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text),
            )
            BasicText("${todo.summary.renter.name} · $where", style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted))
        }
        ActionButton(text = stringResource(Res.string.create_action), enabled = true, onClick = onCreate)
    }
}

/** How the month is going: what has come in out of what is expected, one bar for the parts, and what is left. */
@Composable
private fun MonthCard(month: MonthSummary) {
    val shape = RoundedCornerShape(20.dp)
    val soft = RentManColors.OnPrimary.copy(alpha = 0.7f)
    val note = TextStyle(fontSize = 13.sp, color = soft)
    Column(
        Modifier.fillMaxWidth().background(RentManColors.HeroCard, shape).padding(20.dp).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (month.billed > Paise.Zero) {
            BasicText(stringResource(Res.string.month_received, month.month.label()), style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = soft))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountText(
                    month.collected,
                    style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = RentManColors.OnPrimary),
                    modifier = Modifier.alignByBaseline(),
                )
                BasicText(
                    stringResource(Res.string.of_expected, formatRupees(month.expected)),
                    style = TextStyle(fontSize = 15.sp, color = soft),
                    modifier = Modifier.alignByBaseline(),
                )
            }
            MonthBar(month)
        } else {
            BasicText(stringResource(Res.string.no_bills_month, month.month.label()), style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = RentManColors.OnPrimary))
        }
        val hasLegend = month.notDue > Paise.Zero || month.late > Paise.Zero || month.notBilled > 0
        if (hasLegend || month.earlier > Paise.Zero || month.billed > Paise.Zero) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(RentManColors.OnPrimary.copy(alpha = 0.12f)))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (month.notDue > Paise.Zero) LegendRow(RentManColors.HeroDue, stringResource(Res.string.legend_not_due), month.notDue)
                if (month.late > Paise.Zero) LegendRow(RentManColors.HeroLate, stringResource(Res.string.legend_late), month.late)
                if (month.notBilled > 0) LegendRow(NotBilledColor, stringResource(Res.string.legend_not_billed, month.notBilled), month.notBilledRent)
                if (month.billed > Paise.Zero && month.late + month.notDue == Paise.Zero) {
                    BasicText(stringResource(Res.string.all_collected, month.month.label()), style = note)
                }
                if (month.earlier > Paise.Zero) {
                    BasicText(stringResource(Res.string.from_earlier, formatRupees(month.earlier)), style = note, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** Rent of bills not made yet: a quiet, hollow-looking part of the bar. */
private val NotBilledColor = RentManColors.OnPrimary.copy(alpha = 0.35f)

/** One bar for the whole month: received, not due yet, late, and the rent of bills not made yet. */
@Composable
private fun MonthBar(month: MonthSummary) {
    val parts = listOf(
        month.collected to RentManColors.HeroReceived,
        month.notDue to RentManColors.HeroDue,
        month.late to RentManColors.HeroLate,
        month.notBilledRent to NotBilledColor,
    ).filter { (amount, _) -> amount > Paise.Zero }
    Row(
        Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(RentManColors.HeroTrack),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        parts.forEach { (amount, color) -> Box(Modifier.weight(amount.value.toFloat()).fillMaxHeight().background(color)) }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, amount: Paise) {
    Row(Modifier.heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        BasicText(label, style = TextStyle(fontSize = 13.sp, color = RentManColors.OnPrimary.copy(alpha = 0.8f)), modifier = Modifier.weight(1f))
        AmountText(amount, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.OnPrimary))
    }
}

@Composable
private fun LeftRenterRow(summary: RenterSummary, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val where = listOfNotNull(summary.renter.unit, summary.address.name).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(RentManColors.Surface, shape)
            .border(1.dp, RentManColors.Border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(summary.renter.name, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.TextMuted))
            BasicText(where, style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted))
        }
        summary.renter.leftOn?.let {
            BasicText(stringResource(Res.string.left_on, it.label()), style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted))
        }
    }
}

@Composable
private fun RaiseRentCard(offer: RentIncreaseOffer, onRaise: () -> Unit, onNotNow: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RentManColors.Surface, shape)
            .border(1.dp, RentManColors.Border, shape)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            stringResource(Res.string.raise_card_title, offer.summary.renter.name, "${offer.percent}%"),
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text),
        )
        BasicText(
            stringResource(Res.string.raise_card_detail, formatRupees(offer.currentRent), formatRupees(offer.newRent), offer.effectiveMonth.label()),
            style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted),
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onNotNow).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                BasicText(stringResource(Res.string.not_now), style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.TextMuted))
            }
            ActionButton(text = stringResource(Res.string.raise_action), enabled = true, onClick = onRaise)
        }
    }
}

@Composable
private fun RenterRowCard(row: RenterRow, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val summary = row.summary
    val where = listOfNotNull(summary.renter.unit, summary.address.name).joinToString(" · ")
    val colors = row.status.colors()
    // Paid and no-bill rows stay plain, so the ones that need a call stand out.
    val needsAttention = row.status in attentionStatuses
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(RentManColors.Surface, shape)
            .border(1.dp, RentManColors.Border, shape)
            .drawBehind { if (needsAttention) drawRect(colors.text, size = Size(4.dp.toPx(), size.height)) }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(RentManColors.Divider, CircleShape), contentAlignment = Alignment.Center) {
            BasicText(initials(summary.renter.name), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(summary.renter.name, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text))
            BasicText(where, style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted))
            RowDetail(row)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when {
                // The pill already says "Paid", so a ₹0 would be noise. Only paying ahead has something to say.
                row.status == RenterStatus.Paid -> if (row.pending < Paise.Zero) {
                    BalanceText(row.pending, style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted))
                }
                // Someone with no bill yet owes nothing, so their rent is shown instead.
                row.status == RenterStatus.NoBillYet -> AmountText(summary.renter.monthlyRent, style = amountStyle(RentManColors.Text))
                else -> AmountText(row.pending, style = amountStyle(colors.text))
            }
            StatusPill(row.status)
        }
    }
}

private fun amountStyle(color: Color) = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)

private val attentionStatuses = setOf(RenterStatus.Late, RenterStatus.PartPaid, RenterStatus.Unpaid)

/** The line under the address that says what to do about this status: how late, when it is due, or how much has come in. */
@Composable
private fun RowDetail(row: RenterRow) {
    when (row.status) {
        RenterStatus.Late -> BasicText(
            if (row.daysLate == 1) stringResource(Res.string.days_late_one) else stringResource(Res.string.days_late_other, row.daysLate),
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Danger),
        )
        // The bill is named so the line says which one it is ("Nov bill · due 5 Nov"), also for older bills that fell due the month after.
        RenterStatus.Unpaid -> row.billMonth?.let { month ->
            row.dueOn?.let { due ->
                BasicText(
                    stringResource(Res.string.due_on, month.shortName(), due.shortLabel()),
                    style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted),
                )
            }
        }
        RenterStatus.PartPaid -> Column(Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(RentManColors.Divider)) {
                Box(Modifier.fillMaxWidth(row.paidShare).fillMaxHeight().background(RentManColors.Primary))
            }
            BasicText(
                stringResource(Res.string.paid_of, formatRupees(row.paidSinceBill), formatRupees(row.paidSinceBill + row.pending)),
                style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted),
            )
        }
        RenterStatus.Paid, RenterStatus.NoBillYet -> Unit
    }
}

/** "All 6", "Late 1"... One tap shows only those renters, and the chosen chip says so for a screen reader too. */
@Composable
private fun StatusChips(counts: List<Pair<RenterStatus, Int>>, total: Int, selected: RenterStatus?, onSelect: (RenterStatus?) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("${stringResource(Res.string.filter_all)} $total", selected == null) { onSelect(null) }
        counts.forEach { (status, count) -> Chip("${status.label()} $count", selected == status) { onSelect(status) } }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    // The tap area is 48dp tall, the visible chip is smaller.
    Box(Modifier.heightIn(min = 48.dp).selectable(selected = selected, role = Role.Tab, onClick = onClick), contentAlignment = Alignment.Center) {
        BasicText(
            text,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (selected) RentManColors.OnPrimary else RentManColors.Text),
            modifier = Modifier
                .background(if (selected) RentManColors.Primary else RentManColors.Surface, RoundedCornerShape(50))
                .border(1.dp, if (selected) RentManColors.Primary else RentManColors.BorderStrong, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun Centered(modifier: Modifier = Modifier.fillMaxSize(), content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
}

private val mutedStyle = TextStyle(fontSize = 16.sp, color = RentManColors.TextMuted)

private fun initials(name: String): String =
    name.split(' ').filter(String::isNotBlank).take(2).joinToString("") { it.first().uppercase() }

private val previewHome = Address(AddressId(1), "338 A")
private val previewRenter = Renter(
    id = RenterId(1),
    addressId = previewHome.id,
    unit = "Ground floor",
    name = "Ravi Kumar",
    phone = "919876543210",
    monthlyRent = 7_500L.rupees,
    dueDay = 5,
    agreementStart = LocalDate(2026, 1, 1),
    raisePercent = 10,
    raiseEveryMonths = 11,
    billsElectricity = true,
    customRate = null,
    startingReading = 1240,
    firstBillMonth = BillingMonth.of(2026, 11),
)

@Preview
@Composable
private fun LoadingPreview() = HomeScreen(Loading) {}

@Preview
@Composable
private fun ErrorPreview() = HomeScreen(Error(DomainError.Storage("disk"))) {}

@Preview
@Composable
private fun ContentPreview() = HomeScreen(
    Content(
        renters = listOf(
            RenterRow(RenterSummary(previewRenter.copy(id = RenterId(2), name = "Neha Verma"), previewHome), 8_000L.rupees, RenterStatus.Late, 8_000L.rupees, dueOn = LocalDate(2026, 11, 5), daysLate = 18),
            RenterRow(RenterSummary(previewRenter, previewHome), 3_380L.rupees, RenterStatus.PartPaid, Paise.Zero, paidSinceBill = 5_000L.rupees, dueOn = LocalDate(2026, 12, 5)),
            RenterRow(RenterSummary(previewRenter.copy(id = RenterId(4), name = "Amit Shah"), previewHome), 7_500L.rupees, RenterStatus.Unpaid, Paise.Zero, billMonth = BillingMonth.of(2026, 11), dueOn = LocalDate(2026, 12, 5)),
            RenterRow(RenterSummary(previewRenter.copy(id = RenterId(5), name = "Priya Sen"), previewHome), Paise.Zero, RenterStatus.Paid, Paise.Zero),
            RenterRow(RenterSummary(previewRenter.copy(id = RenterId(6), name = "Kiran Rao"), previewHome), (-1_000L).rupees, RenterStatus.Paid, Paise.Zero),
        ),
        todos = listOf(BillTodo(RenterSummary(previewRenter, previewHome), BillingMonth.of(2026, 11))),
        raises = listOf(RentIncreaseOffer(RenterSummary(previewRenter, previewHome), 10, 8_250L.rupees, BillingMonth.of(2026, 12))),
        left = listOf(RenterSummary(previewRenter.copy(id = RenterId(3), name = "Sunita Devi", active = false, leftOn = LocalDate(2026, 10, 31)), previewHome)),
        thisMonth = MonthSummary(BillingMonth.of(2026, 11), collected = 52_000L.rupees, late = 8_000L.rupees, notDue = 6_000L.rupees, notBilled = 2, notBilledRent = 15_000L.rupees, earlier = 2_500L.rupees),
    ),
) {}

@Preview
@Composable
private fun EmptyPreview() = HomeScreen(Content(emptyList(), emptyList(), emptyList(), emptyList(), MonthSummary(BillingMonth.of(2026, 11), Paise.Zero, Paise.Zero, Paise.Zero, 0, Paise.Zero, Paise.Zero))) {}
