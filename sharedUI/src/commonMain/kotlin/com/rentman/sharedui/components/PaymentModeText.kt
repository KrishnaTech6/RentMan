package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import com.rentman.domain.payments.PaymentMode
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.payment_bank
import com.rentman.sharedui.resources.payment_cash
import com.rentman.sharedui.resources.payment_upi
import org.jetbrains.compose.resources.stringResource

/** "UPI", "Cash" or "Bank". Formatting only. */
@Composable
internal fun PaymentMode.modeLabel(): String = stringResource(
    when (this) {
        PaymentMode.Upi -> Res.string.payment_upi
        PaymentMode.Cash -> Res.string.payment_cash
        PaymentMode.Bank -> Res.string.payment_bank
    },
)
