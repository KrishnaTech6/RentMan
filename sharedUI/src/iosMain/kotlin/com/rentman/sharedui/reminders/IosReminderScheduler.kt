package com.rentman.sharedui.reminders

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.reminders.Reminder
import com.rentman.domain.reminders.ReminderPlan
import com.rentman.domain.reminders.ReminderScheduler
import com.rentman.domain.reminders.ScheduledReminder
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.reminder_stop_action
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import kotlin.coroutines.cancellation.CancellationException
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionNone
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Schedules each planned reminder as a local notification at its date and time. Everything pending is replaced on every
 * sync. The plan is capped below Apple's limit of 64 pending notifications (ReminderPlanner.MAX_SCHEDULED).
 */
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
internal class IosReminderScheduler(private val timeZone: TimeZone) : ReminderScheduler {

    /** Anything Kotlin throws while building the requests is caught here, once, and returned as a failure. */
    override suspend fun sync(plan: ReminderPlan): Outcome<Unit> = try {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removeAllPendingNotificationRequests()
        center.setNotificationCategories(setOf(stopCategory()))
        plan.items.forEach { item ->
            center.addNotificationRequest(requestFor(item), withCompletionHandler = null)
        }
        Outcome.Success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Outcome.Failure(DomainError.Storage(error.message))
    }

    /** One planned reminder as an iOS notification request, set for its date and time in the landlord's time zone. */
    internal suspend fun requestFor(item: ScheduledReminder): UNNotificationRequest {
        val text = item.reminder.text()
        val content = UNMutableNotificationContent().apply {
            setTitle(text.title)
            setBody(text.body)
            setSound(UNNotificationSound.defaultSound)
            val reminder = item.reminder
            if (reminder is Reminder.RentPending) {
                setCategoryIdentifier(STOP_CATEGORY)
                setUserInfo(mapOf(RENTER_ID_KEY to reminder.renterId.value.toString()))
            }
        }
        val local = item.at.toLocalDateTime(timeZone)
        val date = NSDateComponents().apply {
            setYear(local.year.toLong())
            setMonth(local.month.ordinal + 1L)
            setDay(local.day.toLong())
            setHour(local.hour.toLong())
            setMinute(local.minute.toLong())
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(date, repeats = false)
        return UNNotificationRequest.requestWithIdentifier(item.id, content, trigger)
    }

    private suspend fun stopCategory(): UNNotificationCategory {
        val title = getString(Res.string.reminder_stop_action)
        val stop = UNNotificationAction.actionWithIdentifier(identifier = STOP_ACTION, title = title, options = UNNotificationActionOptionNone)
        return UNNotificationCategory.categoryWithIdentifier(
            identifier = STOP_CATEGORY,
            actions = listOf(stop),
            intentIdentifiers = emptyList<String>(),
            options = UNNotificationCategoryOptionNone,
        )
    }

    internal companion object {
        const val STOP_CATEGORY = "rent-pending"
        const val STOP_ACTION = "stop-this-month"
        const val RENTER_ID_KEY = "renterId"
    }
}
