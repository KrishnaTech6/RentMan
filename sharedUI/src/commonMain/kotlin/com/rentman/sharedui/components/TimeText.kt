package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.time_am
import com.rentman.sharedui.resources.time_pm
import org.jetbrains.compose.resources.stringResource

/** "9:00" for 540 and "12:30" for 750: the 12-hour digits of a minute of the day. */
internal fun clockDigits(minuteOfDay: Int): String {
    val hour = (minuteOfDay / 60).let { if (it % 12 == 0) 12 else it % 12 }
    return "$hour:${(minuteOfDay % 60).toString().padStart(2, '0')}"
}

internal fun isAfternoon(minuteOfDay: Int): Boolean = minuteOfDay >= 12 * 60

/** "9:00 AM". The 12-hour clock matches how the app's users write times. Formatting only. */
@Composable
internal fun timeLabel(minuteOfDay: Int): String =
    stringResource(if (isAfternoon(minuteOfDay)) Res.string.time_pm else Res.string.time_am, clockDigits(minuteOfDay))
