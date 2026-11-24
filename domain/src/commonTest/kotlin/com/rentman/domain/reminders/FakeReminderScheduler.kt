package com.rentman.domain.reminders

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome

/** Records every plan it was asked to schedule. With [fail] set it reports a failure instead, like a broken phone scheduler. */
class FakeReminderScheduler(var fail: Boolean = false) : ReminderScheduler {
    val synced = mutableListOf<ReminderPlan>()

    override suspend fun sync(plan: ReminderPlan): Outcome<Unit> {
        synced += plan
        return if (fail) Outcome.Failure(DomainError.Storage("scheduler")) else Outcome.Success(Unit)
    }
}
