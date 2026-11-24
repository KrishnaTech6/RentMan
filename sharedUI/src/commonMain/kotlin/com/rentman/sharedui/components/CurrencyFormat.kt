package com.rentman.sharedui.components

import com.rentman.domain.money.Paise

/**
 * ₹ with Indian digit grouping (₹1,24,000) from the platform's number formatter. Whole rupees show no
 * decimals; anything with paise shows exactly two.
 */
internal expect fun formatRupees(amount: Paise): String
