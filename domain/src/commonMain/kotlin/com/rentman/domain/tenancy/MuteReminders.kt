package com.rentman.domain.tenancy

import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillRepository
import kotlinx.coroutines.flow.first

/**
 * "Stop this month": no more reminders about the renter's latest bill. The next bill ends it by itself.
 * Resuming ([muted] = false) clears it. The renter's pending amount carries on as usual.
 */
class MuteRemindersUseCase(
    private val renters: RenterRepository,
    private val bills: BillRepository,
) {
    suspend operator fun invoke(renterId: RenterId, muted: Boolean): Outcome<Unit> {
        if (!muted) return renters.setRemindersMutedMonth(renterId, null)
        val latest = (bills.observeAll().first() as? Outcome.Success)?.value
            ?.filter { it.renterId == renterId }?.maxOfOrNull { it.month }
            ?: return Outcome.Success(Unit) // no bill, so there is nothing to be reminded about
        return renters.setRemindersMutedMonth(renterId, latest)
    }
}
