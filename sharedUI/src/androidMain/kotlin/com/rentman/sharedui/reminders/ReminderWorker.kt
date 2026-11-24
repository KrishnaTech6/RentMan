package com.rentman.sharedui.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.rentman.domain.money.Paise
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.reminders.Reminder
import com.rentman.domain.tenancy.RenterId

/** Shows one reminder when WorkManager runs it, around the time it was planned for. */
internal class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val reminder = inputData.toReminder() ?: return Result.failure()
        ReminderNotifications.show(applicationContext, inputData.getInt(KEY_NOTIFICATION_ID, 0), reminder)
        return Result.success()
    }

    companion object {
        private const val KEY_KIND = "kind"
        private const val KEY_NOTIFICATION_ID = "notificationId"
        private const val KEY_MONTH = "month"
        private const val KEY_MISSING = "missing"
        private const val KEY_RENTER_ID = "renterId"
        private const val KEY_NAME = "name"
        private const val KEY_PENDING = "pending"
        private const val KIND_CREATE_BILLS = "create-bills"
        private const val KIND_RENT_PENDING = "rent-pending"

        fun inputFor(notificationId: Int, reminder: Reminder): Data = when (reminder) {
            is Reminder.CreateBills -> workDataOf(
                KEY_KIND to KIND_CREATE_BILLS, KEY_NOTIFICATION_ID to notificationId,
                KEY_MONTH to reminder.month.value, KEY_MISSING to reminder.missing,
            )
            is Reminder.RentPending -> workDataOf(
                KEY_KIND to KIND_RENT_PENDING, KEY_NOTIFICATION_ID to notificationId,
                KEY_RENTER_ID to reminder.renterId.value, KEY_NAME to reminder.renterName, KEY_PENDING to reminder.pending.value,
            )
        }

        private fun Data.toReminder(): Reminder? = when (getString(KEY_KIND)) {
            KIND_CREATE_BILLS -> Reminder.CreateBills(BillingMonth.fromValue(getInt(KEY_MONTH, 0)), getInt(KEY_MISSING, 0))
            KIND_RENT_PENDING -> Reminder.RentPending(RenterId(getLong(KEY_RENTER_ID, 0)), getString(KEY_NAME).orEmpty(), Paise(getLong(KEY_PENDING, 0)))
            else -> null
        }
    }
}
