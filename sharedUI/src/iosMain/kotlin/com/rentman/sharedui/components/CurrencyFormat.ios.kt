package com.rentman.sharedui.components

import com.rentman.domain.money.Paise
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSLocale
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import kotlin.math.abs

@OptIn(ExperimentalForeignApi::class)
internal actual fun formatRupees(amount: Paise): String {
    val formatter = NSNumberFormatter()
    formatter.numberStyle = NSNumberFormatterCurrencyStyle
    formatter.locale = NSLocale(localeIdentifier = "en_IN")
    formatter.minimumFractionDigits = if (amount.value % 100 == 0L) 0uL else 2uL
    formatter.maximumFractionDigits = 2uL
    val number = NSDecimalNumber(mantissa = abs(amount.value).toULong(), exponent = -2, isNegative = amount.value < 0)
    return checkNotNull(formatter.stringFromNumber(number)) { "Could not format ${amount.value} paise" }
}
