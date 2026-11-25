package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import com.rentman.domain.billing.BillingMonth
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.months_short
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource

/** "Nov 2026". Formatting only. */
@Composable
internal fun BillingMonth.label(): String = "${stringArrayResource(Res.array.months_short)[month - 1]} $year"

/** "5 Dec 2026". Formatting only. */
@Composable
internal fun LocalDate.label(): String = "$day ${stringArrayResource(Res.array.months_short)[month.ordinal]} $year"

/** "5 Dec". Used in the message sent to a renter. */
@Composable
internal fun LocalDate.shortLabel(): String = "$day ${stringArrayResource(Res.array.months_short)[month.ordinal]}"

/** "Nov". A month on its own, for lines that name a bill. */
@Composable
internal fun BillingMonth.shortName(): String = stringArrayResource(Res.array.months_short)[month - 1]
