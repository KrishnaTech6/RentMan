package com.rentman.domain.tenancy

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

class SaveRenterUseCaseTest {
    private val address = AddressId(1)
    private val clock = object : Clock {
        // 1 Dec 2026, 02:00 in Kolkata, but still 30 Nov in UTC: the month depends on the time zone.
        override fun now(): Instant = Instant.parse("2026-11-30T20:30:00Z")
    }
    private val today = GetTodayUseCase(clock, TimeZone.of("Asia/Kolkata"))

    private fun useCase(existing: List<Renter> = emptyList()) =
        SaveRenterUseCase(FakeRenterRepository(existing), today)

    private fun form(
        unit: String = "",
        name: String = "Ravi Kumar",
        phone: String = "98765 43210",
        rent: String = "7,500",
        dueDay: String = "5",
        askToRaise: Boolean = true,
        billsElectricity: Boolean = true,
        startingReading: String = "1240",
        ownRate: String = "",
        addressId: AddressId? = address,
    ) = RenterForm(
        addressId = addressId,
        unit = unit,
        name = name,
        phone = phone,
        rent = rent,
        dueDay = dueDay,
        agreementStart = LocalDate(2026, 1, 1),
        askToRaise = askToRaise,
        billsElectricity = billsElectricity,
        startingReading = startingReading,
        ownRate = ownRate,
    )

    private fun existingRenter(unit: String?) = Renter(
        id = RenterId(9),
        addressId = address,
        unit = unit,
        name = "Sunita Devi",
        phone = "919812345678",
        monthlyRent = 4_500L.rupees,
        dueDay = 5,
        agreementStart = LocalDate(2026, 1, 1),
        raisePercent = 10,
        raiseEveryMonths = 11,
        billsElectricity = false,
        startingReading = null,
        customRate = null,
        firstBillMonth = BillingMonth.of(2026, 1),
    )

    private fun problems(outcome: Outcome<Renter>) =
        ((outcome as Outcome.Failure).error as DomainError.InvalidRenter).problems

    @Test
    fun savesAValidRenterWithNormalizedFields() = runTest {
        val saved = (useCase()(form(unit = "  Ground floor ")) as Outcome.Success).value

        assertEquals(RenterId(1), saved.id)
        assertEquals("Ravi Kumar", saved.name)
        assertEquals("919876543210", saved.phone)
        assertEquals("Ground floor", saved.unit)
        assertEquals(Paise(750_000), saved.monthlyRent)
        assertEquals(5, saved.dueDay)
        assertEquals(1240, saved.startingReading)
        assertEquals(null, saved.customRate)
        assertEquals(true, saved.active)
    }

    @Test
    fun theFirstBillMonthIsTodaysMonthInTheLocalTimeZone() = runTest {
        val saved = (useCase()(form()) as Outcome.Success).value

        assertEquals(BillingMonth.of(2026, 12), saved.firstBillMonth)
        assertEquals(LocalDate(2026, 12, 1), today())
    }

    @Test
    fun askingToRaiseMeansTenPercentEveryElevenMonths() = runTest {
        val asks = (useCase()(form(askToRaise = true)) as Outcome.Success).value
        val doesNotAsk = (useCase()(form(askToRaise = false)) as Outcome.Success).value

        assertEquals(10 to 11, asks.raisePercent to asks.raiseEveryMonths)
        assertEquals(0, doesNotAsk.raisePercent)
    }

    @Test
    fun theFixedChargesAreSavedWithTheRenter() = runTest {
        val water = FixedCharge(label = "Water", amount = 200L.rupees)
        val garbage = FixedCharge(label = "Garbage", amount = 50L.rupees)

        val saved = (useCase()(form().copy(fixedCharges = listOf(water, garbage))) as Outcome.Success).value

        assertEquals(listOf(water, garbage), saved.fixedCharges)
    }

    @Test
    fun aChargeTypedButNotAddedBlocksTheSave() = runTest {
        val renters = FakeRenterRepository()

        val labelOnly = SaveRenterUseCase(renters, today)(form().copy(chargeLabel = "Water"))
        val amountOnly = SaveRenterUseCase(renters, today)(form().copy(chargeAmount = "200"))

        assertEquals(mapOf(RenterField.Charge to FieldProblem.Invalid), problems(labelOnly))
        assertEquals(mapOf(RenterField.Charge to FieldProblem.Invalid), problems(amountOnly))
        assertEquals(emptyList(), renters.saved)
    }

    @Test
    fun blankChargeFieldsDoNotBlockTheSave() = runTest {
        val result = useCase()(form().copy(chargeLabel = "  ", chargeAmount = ""))

        assertIs<Outcome.Success<Renter>>(result)
    }

    @Test
    fun aRenterWhoPaysElectricityDirectlyNeedsNoReadingOrRate() = runTest {
        val saved = (useCase()(form(billsElectricity = false, startingReading = "", ownRate = "9")) as Outcome.Success).value

        assertEquals(false, saved.billsElectricity)
        assertEquals(null, saved.startingReading)
        assertEquals(null, saved.customRate)
    }

    @Test
    fun anOwnRateIsStoredInPaise() = runTest {
        val saved = (useCase()(form(ownRate = "9")) as Outcome.Success).value

        assertEquals(Paise(900), saved.customRate)
    }

    @Test
    fun reportsEveryProblemAtOnce() = runTest {
        val result = useCase()(
            form(addressId = null, name = " ", phone = "123", rent = "0", dueDay = "32", startingReading = "", ownRate = "abc"),
        )

        assertEquals(
            mapOf(
                RenterField.Address to FieldProblem.Required,
                RenterField.Name to FieldProblem.Required,
                RenterField.Phone to FieldProblem.Invalid,
                RenterField.Rent to FieldProblem.Invalid,
                RenterField.DueDay to FieldProblem.Invalid,
                RenterField.StartingReading to FieldProblem.Required,
                RenterField.OwnRate to FieldProblem.Invalid,
            ),
            problems(result),
        )
    }

    @Test
    fun aBlankPhoneIsRequiredAndABadOneIsInvalid() = runTest {
        assertEquals(mapOf(RenterField.Phone to FieldProblem.Required), problems(useCase()(form(phone = " "))))
        assertEquals(mapOf(RenterField.Phone to FieldProblem.Invalid), problems(useCase()(form(phone = "12345"))))
    }

    @Test
    fun blankRentAndDueDayAreRequiredNotInvalid() = runTest {
        val result = useCase()(form(rent = "", dueDay = ""))

        assertEquals(
            mapOf(RenterField.Rent to FieldProblem.Required, RenterField.DueDay to FieldProblem.Required),
            problems(result),
        )
    }

    @Test
    fun ifTheCurrentRentersCannotBeReadNothingIsSaved() = runTest {
        val renters = FakeRenterRepository(failReads = true)

        val result = SaveRenterUseCase(renters, today)(form())

        assertEquals(Outcome.Failure(DomainError.Storage("disk")), result)
        assertEquals(emptyList(), renters.saved)
    }

    @Test
    fun theUnitIsOptionalAtAnEmptyAddress() = runTest {
        val saved = (useCase()(form(unit = "")) as Outcome.Success).value

        assertEquals(null, saved.unit)
    }

    @Test
    fun theUnitIsRequiredWhenTheAddressAlreadyHasARenter() = runTest {
        val result = useCase(listOf(existingRenter(unit = "Ground floor")))(form(unit = ""))

        assertEquals(mapOf(RenterField.Unit to FieldProblem.Required), problems(result))
    }

    @Test
    fun twoRentersAtOneAddressCannotShareAUnitIgnoringCase() = runTest {
        val result = useCase(listOf(existingRenter(unit = "Ground floor")))(form(unit = "ground FLOOR"))

        assertEquals(mapOf(RenterField.Unit to FieldProblem.Duplicate), problems(result))
    }

    @Test
    fun aDifferentUnitAtTheSameAddressIsFine() = runTest {
        val result = useCase(listOf(existingRenter(unit = "Ground floor")))(form(unit = "1st floor"))

        assertIs<Outcome.Success<Renter>>(result)
    }
}
