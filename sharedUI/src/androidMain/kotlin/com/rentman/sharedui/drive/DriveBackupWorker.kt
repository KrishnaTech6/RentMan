package com.rentman.sharedui.drive

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.drive.AutoBackUpToDriveUseCase
import org.koin.mp.KoinPlatform
import java.util.concurrent.TimeUnit

/**
 * The daily Drive backup on Android, run by the phone about once a day with internet, even when the app is closed. It applies the
 * same rule as opening the app ([AutoBackUpToDriveUseCase]): nothing changed, no Drive or an empty phone mean "not now".
 */
internal class DriveBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (val outcome = KoinPlatform.getKoin().get<AutoBackUpToDriveUseCase>()()) {
        is Outcome.Success -> Result.success()
        // No internet after all, or Google had trouble: try again a little later. Anything else waits for tomorrow.
        is Outcome.Failure -> if (outcome.error == DomainError.DriveUnavailable) Result.retry() else Result.success()
    }

    companion object {
        /** Asks for the daily run once; asking again keeps the one already planned. */
        fun schedule(context: Context) {
            val daily = PeriodicWorkRequestBuilder<DriveBackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("drive-backup", ExistingPeriodicWorkPolicy.KEEP, daily)
        }
    }
}
