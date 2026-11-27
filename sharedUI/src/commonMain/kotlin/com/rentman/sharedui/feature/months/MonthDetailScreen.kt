package com.rentman.sharedui.feature.months

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentman.domain.DomainError
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.MonthDetail
import com.rentman.domain.billing.MonthReviewMessage
import com.rentman.domain.billing.MonthSummary
import com.rentman.domain.billing.RenterMonth
import com.rentman.domain.billing.WhatsAppLink
import com.rentman.domain.messages.MessageKind
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterSummary
import com.rentman.sharedui.components.AmountText
import com.rentman.sharedui.components.CenteredMessage
import com.rentman.sharedui.components.ErrorMessage
import com.rentman.sharedui.components.MonthCard
import com.rentman.sharedui.components.ScreenTopBar
import com.rentman.sharedui.components.SectionTitle
import com.rentman.sharedui.components.StatusPill
import com.rentman.sharedui.components.colors
import com.rentman.sharedui.components.formatRupees
import com.rentman.sharedui.components.label
import com.rentman.sharedui.feature.months.MonthDetailIntent.Back
import com.rentman.sharedui.feature.months.MonthDetailIntent.OpenRenter
import com.rentman.sharedui.feature.months.MonthDetailIntent.Refresh
import com.rentman.sharedui.feature.months.MonthDetailIntent.Retry
import com.rentman.sharedui.feature.months.MonthDetailIntent.Share
import com.rentman.sharedui.feature.months.MonthDetailUiState.Content
import com.rentman.sharedui.feature.months.MonthDetailUiState.Error
import com.rentman.sharedui.feature.months.MonthDetailUiState.Loading
import com.rentman.sharedui.mvi.NavigationEffect
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.cash_in
import com.rentman.sharedui.resources.loading
import com.rentman.sharedui.resources.share_action
import com.rentman.sharedui.resources.paid_of
import com.rentman.sharedui.resources.renters_in_month
import com.rentman.sharedui.theme.RentManColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The only composable that knows the ViewModel (ARCHITECTURE.md §5). */
@Composable
internal fun MonthDetailRoute(
    month: BillingMonth,
    onOpenRenter: (RenterId) -> Unit,
    onBack: () -> Unit,
    viewModel: MonthDetailViewModel = koinViewModel { parametersOf(month.value) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(Refresh) }
    val uriHandler = LocalUriHandler.current
    val monthLabel = month.label()
    NavigationEffect(viewModel.navigation) { event ->
        when (event) {
            is MonthDetailNavEvent.OpenRenter -> onOpenRenter(event.renterId)
            MonthDetailNavEvent.Close -> onBack()
            // No number: WhatsApp asks which chat, so the landlord picks the group. If no app opens it, nothing is lost.
            is MonthDetailNavEvent.Share -> runCatching {
                uriHandler.openUri(WhatsAppLink.of("", MonthReviewMessage.text(event.detail, monthLabel, money = ::formatRupees, template = event.message)))
            }
        }
    }
    MonthDetailScreen(month, state, viewModel::onIntent)
}

@Composable
internal fun MonthDetailScreen(month: BillingMonth, state: MonthDetailUiState, onIntent: (MonthDetailIntent) -> Unit) {
    Column(Modifier.fillMaxSize().background(RentManColors.Background)) {
        ScreenTopBar(
            title = month.label(),
            onBack = { onIntent(Back) },
            action = (state as? Content)?.let { content -> stringResource(Res.string.share_action) to { onIntent(Share(content.detail, content.message)) } },
        )
        when (state) {
            Loading -> CenteredMessage(stringResource(Res.string.loading))
            is Error -> ErrorMessage(state.error, onRetry = { onIntent(Retry) })
            is Content -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "month") { MonthCard(state.detail.summary) }
                if (state.detail.summary.cashIn > Paise.Zero) {
                    item(key = "cash-in") { CashInRow(month, state.detail.summary.cashIn) }
                }
                if (state.detail.rows.isNotEmpty()) item(key = "renters") { SectionTitle(stringResource(Res.string.renters_in_month)) }
                items(state.detail.rows, key = { it.summary.renter.id.value }) { row ->
                    RenterMonthRow(row, onClick = { onIntent(OpenRenter(row.summary.renter.id)) })
                }
            }
        }
    }
}

/** Payments dated in this month, whichever bill they went to: what reached the bank, not how much of the bills is settled. */
@Composable
private fun CashInRow(month: BillingMonth, cashIn: Paise) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            stringResource(Res.string.cash_in, month.label()),
            style = TextStyle(fontSize = 14.sp, color = RentManColors.TextMuted),
            modifier = Modifier.weight(1f),
        )
        AmountText(cashIn, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text))
    }
}

/** One renter in this month: what was billed and how it stands. Tap for their own history. */
@Composable
private fun RenterMonthRow(row: RenterMonth, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val renter = row.summary.renter
    val where = listOfNotNull(renter.unit, row.summary.address.name).joinToString(" · ")
    val colors = row.status.colors()
    Row(
        Modifier
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
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(renter.name, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text))
            BasicText(where, style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted))
            if (row.collected > Paise.Zero && row.left > Paise.Zero) {
                BasicText(
                    stringResource(Res.string.paid_of, formatRupees(row.collected), formatRupees(row.billed)),
                    style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted),
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when {
                // Nothing was billed: there is no amount to show, only the pill.
                !row.hasBill -> Unit
                row.left > Paise.Zero -> AmountText(row.left, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text))
                else -> AmountText(row.billed, style = TextStyle(fontSize = 15.sp, color = RentManColors.TextMuted))
            }
            StatusPill(row.status)
        }
    }
}

private val previewAddress = Address(AddressId(1), "12 A")
private val previewMonth = BillingMonth.of(2026, 11)

private fun previewRow(id: Long, name: String, billed: Long, collected: Long, late: Long, hasBill: Boolean = true) = RenterMonth(
    summary = RenterSummary(
        Renter(
            id = RenterId(id), addressId = previewAddress.id, unit = "Ground floor", name = name, phone = "919876543210",
            monthlyRent = 7_500L.rupees, dueDay = 5, agreementStart = LocalDate(2026, 1, 1), raisePercent = 10, raiseEveryMonths = 11,
            billsElectricity = false, customRate = null, startingReading = null, firstBillMonth = previewMonth,
        ),
        previewAddress,
    ),
    month = previewMonth,
    billed = billed.rupees,
    collected = collected.rupees,
    late = late.rupees,
    notDue = (billed - collected - late).rupees,
    dueOn = if (hasBill) LocalDate(2026, 11, 5) else null,
)

private val previewDetail = MonthDetail(
    summary = MonthSummary(previewMonth, 13_000L.rupees, 8_000L.rupees, 3_000L.rupees, 1, 7_500L.rupees, 2_000L.rupees, cashIn = 15_000L.rupees),
    rows = listOf(
        previewRow(1, "Neha Verma", 8_000, 0, 8_000),
        previewRow(2, "Ravi Kumar", 8_000, 5_000, 0),
        previewRow(3, "Priya Sen", 5_000, 5_000, 0),
        previewRow(4, "Amit Shah", 0, 0, 0, hasBill = false),
    ),
)

@Preview
@Composable
private fun LoadingPreview() = MonthDetailScreen(previewMonth, Loading) {}

@Preview
@Composable
private fun ErrorPreview() = MonthDetailScreen(previewMonth, Error(DomainError.Storage("disk"))) {}

@Preview
@Composable
private fun ContentPreview() = MonthDetailScreen(previewMonth, Content(previewDetail, MessageKind.MonthReview.default)) {}
