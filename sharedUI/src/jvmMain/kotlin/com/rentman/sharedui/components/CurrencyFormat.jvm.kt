package com.rentman.sharedui.components

import com.ibm.icu.text.NumberFormat
import com.rentman.domain.money.Paise
import java.math.BigDecimal
import java.util.Locale

// The JVM target exists only for tests. It uses ICU4J, the same engine behind the Android and iOS formatters,
// because the JDK's own NumberFormat can't do Indian grouping.
internal actual fun formatRupees(amount: Paise): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    format.minimumFractionDigits = if (amount.value % 100 == 0L) 0 else 2
    format.maximumFractionDigits = 2
    return format.format(BigDecimal.valueOf(amount.value, 2))
}
