package com.rentman.sharedui.components

import android.icu.text.NumberFormat
import com.rentman.domain.money.Paise
import java.math.BigDecimal
import java.util.Locale

// android.icu (API 24+) groups digits the Indian way; java.text.NumberFormat does not.
internal actual fun formatRupees(amount: Paise): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    format.minimumFractionDigits = if (amount.value % 100 == 0L) 0 else 2
    format.maximumFractionDigits = 2
    return format.format(BigDecimal.valueOf(amount.value, 2))
}
