package com.rentman.domain.tenancy

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.TestData
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateRenterUseCaseTest {
    private val ravi = TestData.renter(id = 1, name = "Ravi Kumar").copy(unit = "Ground floor")
    private val neha = TestData.renter(id = 2, name = "Neha Verma").copy(unit = "1st floor")

    private fun repository(vararg renters: Renter) = FakeRenterRepository(renters.toList())

    private fun problems(outcome: Outcome<Renter>) = ((outcome as Outcome.Failure).error as DomainError.InvalidRenter).problems

    @Test
    fun theFormOfARenterShowsWhatIsSavedAsTypedText() {
        val form = ravi.copy(customRate = Paise(850), raisePercent = 0).toForm()

        assertEquals("Ground floor", form.unit)
        assertEquals("Ravi Kumar", form.name)
        assertEquals("919876543210", form.phone)
        assertEquals("7500", form.rent)
        assertEquals("5", form.dueDay)
        assertEquals(false, form.askToRaise)
        assertEquals(true, form.billsElectricity)
        assertEquals("1240", form.startingReading)
        assertEquals("8.50", form.ownRate)
        assertEquals(LocalDate(2026, 1, 1), form.agreementStart)
        assertEquals(ravi.addressId, form.addressId)
    }

    @Test
    fun aForeignNumberIsShownWithItsPlusSoItCanBeSavedAgain() = runTest {
        val abroad = ravi.copy(phone = "971501234567")
        val repository = repository(abroad)

        assertEquals("+971501234567", abroad.toForm().phone)
        assertEquals("919876543210", ravi.toForm().phone) // an Indian number needs no plus
        assertEquals(abroad, (UpdateRenterUseCase(repository)(abroad.id, abroad.toForm()) as Outcome.Success).value)
    }

    @Test
    fun onlyTheChargesStillInUseAreOnTheForm() {
        val withOld = ravi.copy(fixedCharges = listOf(FixedCharge(FixedChargeId(1), "Water", 200L.rupees), FixedCharge(FixedChargeId(2), "Old", 50L.rupees, active = false)))

        assertEquals(listOf("Water"), withOld.toForm().fixedCharges.map { it.label })
    }

    @Test
    fun aFormMadeFromARenterSavesBackUnchanged() = runTest {
        val repository = repository(ravi)

        val saved = (UpdateRenterUseCase(repository)(ravi.id, ravi.toForm()) as Outcome.Success).value

        assertEquals(ravi, saved)
    }

    @Test
    fun changingTheFieldsKeepsEverythingTheFormDoesNotShow() = runTest {
        val busy = ravi.copy(
            raiseSnoozedUntil = BillingMonth.of(2027, 1),
            remindersMutedMonth = BillingMonth.of(2026, 11),
            firstBillMonth = BillingMonth.of(2026, 3),
        )
        val repository = repository(busy)

        val saved = (UpdateRenterUseCase(repository)(busy.id, busy.toForm().copy(name = "  Ravi K  ", phone = "98111 22233", rent = "8,000", dueDay = "10")) as Outcome.Success).value

        assertEquals("Ravi K", saved.name)
        assertEquals("919811122233", saved.phone)
        assertEquals(8_000L.rupees, saved.monthlyRent)
        assertEquals(10, saved.dueDay)
        assertEquals(busy.id, saved.id)
        assertEquals(BillingMonth.of(2026, 3), saved.firstBillMonth) // past bills are counted from here
        assertEquals(BillingMonth.of(2027, 1), saved.raiseSnoozedUntil)
        assertEquals(BillingMonth.of(2026, 11), saved.remindersMutedMonth)
        assertEquals(true, saved.active)
        assertEquals(saved, repository.saved.single())
    }

    @Test
    fun theRaiseSwitchKeepsTheRenterOwnPercentWhileItStaysOn() = runTest {
        val custom = ravi.copy(raisePercent = 7)
        val repository = repository(custom)
        val useCase = UpdateRenterUseCase(repository)

        assertEquals(7, (useCase(custom.id, custom.toForm()) as Outcome.Success).value.raisePercent)
        assertEquals(0, (useCase(custom.id, custom.toForm().copy(askToRaise = false)) as Outcome.Success).value.raisePercent)
        assertEquals(RentIncreasePolicy.DEFAULT_PERCENT, (useCase(custom.id, custom.toForm().copy(askToRaise = true)) as Outcome.Success).value.raisePercent)
    }

    @Test
    fun theRenterCanKeepTheirOwnUnitButNotTakeANeighboursUnit() = runTest {
        val repository = repository(ravi, neha)
        val useCase = UpdateRenterUseCase(repository)

        assertEquals(true, useCase(ravi.id, ravi.toForm()) is Outcome.Success) // their own label is not a clash
        assertEquals(
            mapOf(RenterField.Unit to FieldProblem.Duplicate),
            problems(useCase(ravi.id, ravi.toForm().copy(unit = "1st FLOOR"))),
        )
    }

    @Test
    fun aRenterWhoLeftDoesNotBlockTheirOldUnit() = runTest {
        val left = neha.copy(active = false, leftOn = LocalDate(2026, 10, 1))
        val repository = repository(ravi, left)

        assertEquals(true, UpdateRenterUseCase(repository)(ravi.id, ravi.toForm().copy(unit = "1st floor")) is Outcome.Success)
    }

    @Test
    fun aUnitIsStillNeededWhenSomeoneElseLivesAtTheAddress() = runTest {
        val repository = repository(ravi, neha)

        assertEquals(mapOf(RenterField.Unit to FieldProblem.Required), problems(UpdateRenterUseCase(repository)(ravi.id, ravi.toForm().copy(unit = " "))))
    }

    @Test
    fun movingToAnotherAddressChecksTheNeighboursThere() = runTest {
        val elsewhere = AddressId(2)
        val other = neha.copy(addressId = elsewhere, unit = "Ground floor")
        val repository = repository(ravi, other)

        assertEquals(
            mapOf(RenterField.Unit to FieldProblem.Duplicate),
            problems(UpdateRenterUseCase(repository)(ravi.id, ravi.toForm().copy(addressId = elsewhere))),
        )
    }

    @Test
    fun theSameCheckingAsAddingAppliesAndEveryProblemIsReported() = runTest {
        val repository = repository(ravi)

        val all = problems(
            UpdateRenterUseCase(repository)(ravi.id, ravi.toForm().copy(name = " ", phone = "123", rent = "0", dueDay = "32", startingReading = "", ownRate = "abc", chargeLabel = "Gas")),
        )

        assertEquals(
            setOf(RenterField.Name, RenterField.Phone, RenterField.Rent, RenterField.DueDay, RenterField.StartingReading, RenterField.OwnRate, RenterField.Charge),
            all.keys,
        )
        assertEquals(ravi, repository.saved.single()) // nothing was saved
    }

    @Test
    fun theChargesOnTheFormAreTheNewChargesOfTheRenter() = runTest {
        val water = FixedCharge(FixedChargeId(1), "Water", 200L.rupees)
        val repository = repository(ravi.copy(fixedCharges = listOf(water)))
        val form = repository.saved.single().toForm()

        val saved = (UpdateRenterUseCase(repository)(ravi.id, form.copy(fixedCharges = listOf(FixedCharge(label = "Gas", amount = 300L.rupees)))) as Outcome.Success).value

        assertEquals(listOf("Gas"), saved.fixedCharges.map { it.label })
    }

    @Test
    fun aRenterWhoLeftCanBeEditedToo() = runTest {
        val left = ravi.copy(active = false, leftOn = LocalDate(2026, 10, 1))
        val repository = repository(left)

        val saved = (UpdateRenterUseCase(repository)(left.id, left.toForm().copy(name = "Ravi Kumar Sr")) as Outcome.Success).value

        assertEquals(false, saved.active)
        assertEquals(LocalDate(2026, 10, 1), saved.leftOn)
    }

    @Test
    fun anUnknownRenterIsNotFoundAndAFailureIsReported() = runTest {
        assertEquals(Outcome.Failure(DomainError.NotFound), UpdateRenterUseCase(repository(ravi))(RenterId(99), ravi.toForm()))
        assertEquals(Outcome.Failure(DomainError.Storage("disk")), UpdateRenterUseCase(FakeRenterRepository(listOf(ravi), failReads = true))(ravi.id, ravi.toForm()))
        assertEquals(Outcome.Failure(DomainError.Storage("disk")), UpdateRenterUseCase(FakeRenterRepository(listOf(ravi), failWrites = true))(ravi.id, ravi.toForm()))
    }

    @Test
    fun observingTheFormGivesTheRentersFormAndNotFoundForAnUnknownOne() = runTest {
        val repository = repository(ravi)

        ObserveRenterFormUseCase(repository)(ravi.id).test {
            assertEquals(Outcome.Success(ravi.toForm()), awaitItem())
        }
        ObserveRenterFormUseCase(repository)(RenterId(99)).test {
            assertEquals(Outcome.Failure(DomainError.NotFound), awaitItem())
        }
    }
}
