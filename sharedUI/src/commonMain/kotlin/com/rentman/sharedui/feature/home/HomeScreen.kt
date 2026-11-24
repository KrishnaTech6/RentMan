package com.rentman.sharedui.feature.home

import com.rentman.sharedui.components.ErrorMessage
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
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
import com.rentman.domain.billing.StillToCollect
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
import com.rentman.sharedui.components.StatusPill
import com.rentman.sharedui.components.UndoBar
import com.rentman.sharedui.components.formatRupees
import com.rentman.sharedui.components.label
import com.rentman.sharedui.components.message
import com.rentman.sharedui.feature.home.HomeIntent.AddRenter
import com.rentman.sharedui.feature.home.HomeIntent.CreateBill
import com.rentman.sharedui.feature.home.HomeIntent.NotNow
import com.rentman.sharedui.feature.home.HomeIntent.OpenRenter
import com.rentman.sharedui.feature.home.HomeIntent.RaiseRent
import com.rentman.sharedui.feature.home.HomeIntent.UndoRaise
import com.rentman.sharedui.feature.home.HomeIntent.OpenSettings
import com.rentman.sharedui.feature.home.HomeIntent.Retry
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
import com.rentman.sharedui.resources.collect_late
import com.rentman.sharedui.resources.collect_not_billed
import com.rentman.sharedui.resources.collect_not_due
import com.rentman.sharedui.resources.collect_owing
import com.rentman.sharedui.resources.still_to_collect
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
            item(key = "still-to-collect") { StillToCollectCard(state.stillToCollect) }
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
            items(state.renters, key = { it.summary.renter.id.value }) { row ->
                RenterRowCard(row, onClick = { onIntent(OpenRenter(row.summary.renter.id)) })
            }
            if (state.left.isNotEmpty()) {
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

@Composable
private fun StillToCollectCard(amounts: StillToCollect) {
    val shape = RoundedCornerShape(20.dp)
    val detail = TextStyle(fontSize = 14.sp, color = RentManColors.OnPrimary.copy(alpha = 0.85f))
    val split = listOfNotNull(
        amounts.late.takeIf { it > Paise.Zero }?.let { stringResource(Res.string.collect_late, formatRupees(it)) },
        amounts.notDueYet.takeIf { it > Paise.Zero }?.let { stringResource(Res.string.collect_not_due, formatRupees(it)) },
    )
    val counts = listOfNotNull(
        amounts.owing.takeIf { it > 0 }?.let { stringResource(Res.string.collect_owing, it) },
        amounts.notBilled.takeIf { it > 0 }?.let { stringResource(Res.string.collect_not_billed, it, formatRupees(amounts.notBilledRent)) },
    )
    Column(
        Modifier.fillMaxWidth().background(RentManColors.Primary, shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(
            stringResource(Res.string.still_to_collect),
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.OnPrimary),
        )
        AmountText(amounts.total, style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = RentManColors.OnPrimary))
        if (split.isNotEmpty()) BasicText(split.joinToString("  ·  "), style = detail)
        if (counts.isNotEmpty()) BasicText(counts.joinToString("  ·  "), style = detail)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(RentManColors.Surface, shape)
            .border(1.dp, RentManColors.Border, shape)
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
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Someone with no bill yet owes nothing, so their rent is shown instead.
            val shown = if (row.status == RenterStatus.NoBillYet) summary.renter.monthlyRent else row.pending
            AmountText(shown, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text))
            StatusPill(row.status)
        }
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
            RenterRow(RenterSummary(previewRenter, previewHome), 3_380L.rupees, RenterStatus.PartPaid, Paise.Zero),
            RenterRow(RenterSummary(previewRenter.copy(id = RenterId(2), name = "Neha Verma"), previewHome), 8_000L.rupees, RenterStatus.Late, 8_000L.rupees),
        ),
        todos = listOf(BillTodo(RenterSummary(previewRenter, previewHome), BillingMonth.of(2026, 11))),
        raises = listOf(RentIncreaseOffer(RenterSummary(previewRenter, previewHome), 10, 8_250L.rupees, BillingMonth.of(2026, 12))),
        left = listOf(RenterSummary(previewRenter.copy(id = RenterId(3), name = "Sunita Devi", active = false, leftOn = LocalDate(2026, 10, 31)), previewHome)),
        stillToCollect = StillToCollect(late = 8_000L.rupees, notDueYet = 3_380L.rupees, owing = 2, notBilled = 1, notBilledRent = 7_500L.rupees),
    ),
) {}

@Preview
@Composable
private fun EmptyPreview() = HomeScreen(Content(emptyList(), emptyList(), emptyList(), emptyList(), StillToCollect.of(emptyList(), emptyList()))) {}
