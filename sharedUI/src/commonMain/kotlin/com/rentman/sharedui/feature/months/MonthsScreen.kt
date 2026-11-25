package com.rentman.sharedui.feature.months

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.rentman.domain.billing.MonthSummary
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.sharedui.components.AmountText
import com.rentman.sharedui.components.CenteredMessage
import com.rentman.sharedui.components.ErrorMessage
import com.rentman.sharedui.components.ScreenTopBar
import com.rentman.sharedui.components.formatRupees
import com.rentman.sharedui.components.label
import com.rentman.sharedui.feature.months.MonthsIntent.Back
import com.rentman.sharedui.feature.months.MonthsIntent.OpenMonth
import com.rentman.sharedui.feature.months.MonthsIntent.Refresh
import com.rentman.sharedui.feature.months.MonthsIntent.Retry
import com.rentman.sharedui.feature.months.MonthsUiState.Content
import com.rentman.sharedui.feature.months.MonthsUiState.Error
import com.rentman.sharedui.feature.months.MonthsUiState.Loading
import com.rentman.sharedui.mvi.NavigationEffect
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.loading
import com.rentman.sharedui.resources.month_all_collected
import com.rentman.sharedui.resources.month_billed
import com.rentman.sharedui.resources.month_left
import com.rentman.sharedui.resources.month_no_bills
import com.rentman.sharedui.resources.months_empty
import com.rentman.sharedui.resources.months_title
import com.rentman.sharedui.theme.RentManColors
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The only composable that knows the ViewModel (ARCHITECTURE.md §5). */
@Composable
internal fun MonthsRoute(
    onOpenMonth: (BillingMonth) -> Unit,
    onBack: () -> Unit,
    viewModel: MonthsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(Refresh) }
    NavigationEffect(viewModel.navigation) { event ->
        when (event) {
            is MonthsNavEvent.OpenMonth -> onOpenMonth(event.month)
            MonthsNavEvent.Close -> onBack()
        }
    }
    MonthsScreen(state, viewModel::onIntent)
}

@Composable
internal fun MonthsScreen(state: MonthsUiState, onIntent: (MonthsIntent) -> Unit) {
    Column(Modifier.fillMaxSize().background(RentManColors.Background)) {
        ScreenTopBar(title = stringResource(Res.string.months_title), onBack = { onIntent(Back) })
        when (state) {
            Loading -> CenteredMessage(stringResource(Res.string.loading))
            is Error -> ErrorMessage(state.error, onRetry = { onIntent(Retry) })
            is Content -> if (state.months.isEmpty()) {
                CenteredMessage(stringResource(Res.string.months_empty))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.months, key = { it.month.value }) { month -> MonthRowCard(month, onClick = { onIntent(OpenMonth(month.month)) }) }
                }
            }
        }
    }
}

/** One month on the list: how much has come in, out of how much was billed, and what is left. */
@Composable
private fun MonthRowCard(summary: MonthSummary, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(RentManColors.Surface, shape)
            .border(1.dp, RentManColors.Border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(summary.month.label(), style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text))
                BasicText(
                    if (summary.billed > Paise.Zero) stringResource(Res.string.month_billed, formatRupees(summary.billed)) else stringResource(Res.string.month_no_bills),
                    style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted),
                )
            }
            if (summary.billed > Paise.Zero) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AmountText(summary.collected, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RentManColors.Text))
                    val left = summary.late + summary.notDue
                    if (left > Paise.Zero) {
                        BasicText(
                            stringResource(Res.string.month_left, formatRupees(left)),
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (summary.late > Paise.Zero) RentManColors.Danger else RentManColors.Warning),
                        )
                    } else {
                        BasicText(stringResource(Res.string.month_all_collected), style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Success))
                    }
                }
            }
        }
        if (summary.billed > Paise.Zero) {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(RentManColors.Divider)) {
                Box(Modifier.fillMaxWidth(summary.collectedShare).fillMaxHeight().background(RentManColors.Success))
            }
        }
    }
}

private val previewMonths = listOf(
    MonthSummary(BillingMonth.of(2026, 11), 52_000L.rupees, 8_000L.rupees, 6_000L.rupees, 1, 7_500L.rupees, Paise.Zero),
    MonthSummary(BillingMonth.of(2026, 10), 66_000L.rupees, Paise.Zero, Paise.Zero, 0, Paise.Zero, Paise.Zero),
    MonthSummary(BillingMonth.of(2026, 9), Paise.Zero, Paise.Zero, Paise.Zero, 0, Paise.Zero, Paise.Zero),
)

@Preview
@Composable
private fun LoadingPreview() = MonthsScreen(Loading) {}

@Preview
@Composable
private fun ErrorPreview() = MonthsScreen(Error(DomainError.Storage("disk"))) {}

@Preview
@Composable
private fun ContentPreview() = MonthsScreen(Content(previewMonths)) {}

@Preview
@Composable
private fun EmptyPreview() = MonthsScreen(Content(emptyList())) {}
