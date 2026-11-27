package com.rentman.sharedui.feature.bill

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillId
import com.rentman.domain.billing.BillRepository
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.rupees
import com.rentman.domain.settings.AppSettings
import com.rentman.domain.settings.SettingsRepository
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.FixedCharge
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** Test fake. [failAdd] makes `add` fail. */
internal class FakeBillRepository(initial: List<Bill> = emptyList(), var failAdd: Boolean = false) : BillRepository {
    private val bills = MutableStateFlow(initial)
    val saved: List<Bill> get() = bills.value

    override fun observeAll(): Flow<Outcome<List<Bill>>> = bills.map { Outcome.Success(it) }

    override suspend fun add(bill: Bill): Outcome<Bill> {
        if (failAdd) return Outcome.Failure(DomainError.Storage("disk"))
        val stored = bill.copy(id = BillId((bills.value.maxOfOrNull { it.id.value } ?: 0) + 1L))
        bills.value += stored
        return Outcome.Success(stored)
    }

    override suspend fun remove(id: BillId): Outcome<Unit> {
        bills.value = bills.value.filterNot { it.id == id }
        return Outcome.Success(Unit)
    }

    override suspend fun restore(bill: Bill): Outcome<Unit> {
        if (bills.value.any { it.renterId == bill.renterId && it.month == bill.month }) return Outcome.Failure(DomainError.Storage("month already billed"))
        bills.value = (bills.value + bill).sortedBy { it.id.value }
        return Outcome.Success(Unit)
    }
}

/** Test fake. [failReads] makes observe emit a storage failure and [failSave] makes `save` fail. */
internal class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
    var failReads: Boolean = false,
    var failSave: Boolean = false,
) : SettingsRepository {
    private val settings = MutableStateFlow(initial)
    val current: AppSettings get() = settings.value

    override fun observe(): Flow<Outcome<AppSettings>> = settings.map {
        if (failReads) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success(it)
    }

    override suspend fun markBackedUp(at: Instant): Outcome<Unit> {
        if (failSave) return Outcome.Failure(DomainError.Storage("disk"))
        settings.value = settings.value.copy(lastBackupAt = at)
        return Outcome.Success(Unit)
    }

    override suspend fun save(settings: AppSettings): Outcome<AppSettings> {
        if (failSave) return Outcome.Failure(DomainError.Storage("disk"))
        this.settings.value = settings
        return Outcome.Success(settings)
    }
}

/** Ravi from PLAN.md: rent ₹7,500, water ₹200, meter at 1240. */
internal object Ravi {
    val address = Address(AddressId(1), "12 A")

    fun renter(billsElectricity: Boolean = true) = Renter(
        id = RenterId(1),
        addressId = address.id,
        unit = "Ground floor",
        name = "Ravi Kumar",
        phone = "919876543210",
        monthlyRent = 7_500L.rupees,
        dueDay = 5,
        agreementStart = LocalDate(2026, 1, 1),
        raisePercent = 10,
        raiseEveryMonths = 11,
        billsElectricity = billsElectricity,
        customRate = null,
        startingReading = 1240.takeIf { billsElectricity },
        firstBillMonth = BillingMonth.of(2026, 11),
        fixedCharges = listOf(FixedCharge(label = "Water", amount = 200L.rupees)),
    )
}
