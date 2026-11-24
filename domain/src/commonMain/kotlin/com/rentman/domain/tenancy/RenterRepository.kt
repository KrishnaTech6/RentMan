package com.rentman.domain.tenancy

import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillingMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface RenterRepository {
    /** Renters who haven't left, sorted by name. Emits again whenever they change. */
    fun observeActive(): Flow<Outcome<List<Renter>>>

    /** Every renter, including those who have left, sorted by name. Emits again whenever they change. */
    fun observeAll(): Flow<Outcome<List<Renter>>>

    /**
     * Saves changes to an existing renter, keeping the same id. The fixed charges are made to match the renter's list: charges
     * with an id are updated, new ones are added, and charges no longer on the list are switched off, not deleted.
     */
    suspend fun update(renter: Renter): Outcome<Renter>

    /** Marks the renter as left on [leftOn], or as living there again when it is null. Their history stays. */
    suspend fun setLeft(renterId: RenterId, leftOn: LocalDate?): Outcome<Unit>

    /** Stops reminders for the renter's latest bill ([month]), or resumes them with null. */
    suspend fun setRemindersMutedMonth(renterId: RenterId, month: BillingMonth?): Outcome<Unit>

    /** Saves a new renter and returns it with its real id. */
    suspend fun add(renter: Renter): Outcome<Renter>
}
