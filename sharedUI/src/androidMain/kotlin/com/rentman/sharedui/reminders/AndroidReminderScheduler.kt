package com.rentman.sharedui.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.reminders.ReminderPlan
import com.rentman.domain.reminders.ReminderScheduler
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

/**
 * Schedules each planned reminder as a one-time WorkManager job. WorkManager treats the delay as the earliest time, so a
 * reminder around 9 AM can arrive a little later: fine here (ARCHITECTURE.md section 7, no exact alarm needed).
 */
internal class AndroidReminderScheduler(
    context: Context,
    private val clock: Clock,
) : ReminderScheduler {
    private val workManager = WorkManager.getInstance(context)

    /** WorkManager can throw (its own database, a full disk). That is caught here, once, and returned as a failure. */
    override suspend fun sync(plan: ReminderPlan): Outcome<Unit> = try {
        // Everything is replaced, so a reminder that is no longer wanted (paid, muted) is gone.
        workManager.cancelAllWorkByTag(TAG).await()
        val now = clock.now()
        plan.items.forEach { item ->
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay((item.at - now).inWholeMilliseconds.coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .setInputData(ReminderWorker.inputFor(item.id.hashCode(), item.reminder))
                .addTag(TAG)
                .build()
            workManager.enqueueUniqueWork(item.id, ExistingWorkPolicy.REPLACE, request).await()
        }
        Outcome.Success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Outcome.Failure(DomainError.Storage(error.message))
    }

    private companion object {
        const val TAG = "reminder"
    }
}
