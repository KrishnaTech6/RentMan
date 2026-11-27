package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import com.rentman.domain.messages.MessageKind
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.message_bill
import com.rentman.sharedui.resources.message_month_review
import com.rentman.sharedui.resources.message_payment_received
import org.jetbrains.compose.resources.stringResource

/** "Bill to renter", "Payment post to group" or "Month review". Formatting only. */
@Composable
internal fun MessageKind.kindLabel(): String = stringResource(
    when (this) {
        MessageKind.Bill -> Res.string.message_bill
        MessageKind.PaymentReceived -> Res.string.message_payment_received
        MessageKind.MonthReview -> Res.string.message_month_review
    },
)
