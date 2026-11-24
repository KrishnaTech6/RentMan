package com.rentman.sharedui.reminders

import com.rentman.domain.reminders.Reminder
import com.rentman.sharedui.components.formatRupees
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.months_short
import com.rentman.sharedui.resources.reminder_create_bills_body
import com.rentman.sharedui.resources.reminder_create_bills_title
import com.rentman.sharedui.resources.reminder_pending_body
import com.rentman.sharedui.resources.reminder_pending_title
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray

/** What a notification says. The same words on Android and iOS, from the same string resources. */
internal data class ReminderText(val title: String, val body: String)

/** Written when the notification is scheduled or shown, outside any screen, so it uses the suspend string functions. */
internal suspend fun Reminder.text(): ReminderText = when (this) {
    is Reminder.CreateBills -> {
        val monthName = "${getStringArray(Res.array.months_short)[month.month - 1]} ${month.year}"
        ReminderText(
            title = getString(Res.string.reminder_create_bills_title, monthName),
            body = getString(Res.string.reminder_create_bills_body, missing),
        )
    }
    is Reminder.RentPending -> ReminderText(
        title = getString(Res.string.reminder_pending_title, renterName),
        body = getString(Res.string.reminder_pending_body, formatRupees(pending)),
    )
}
