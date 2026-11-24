package com.rentman.domain.billing

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.TestData
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.domain.tenancy.FixedCharge
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterSummary
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildBillUseCaseTest {
    private val build = BuildBillUseCase()
    private val november = BillingMonth.of(2026, 11)

    private fun context(
        renter: com.rentman.domain.tenancy.Renter = TestData.renter(),
        previousReading: Int? = 1240,
        rate: Paise = Paise(800),
    ) = BillContext(RenterSummary(renter, TestData.address), november, previousReading, rate, pendingBefore = Paise.Zero)

    private fun bill(outcome: Outcome<Bill>) = (outcome as Outcome.Success).value

    @Test
    fun ravisNovemberBillMatchesThePlan() {
        // Rent 7,500 + electricity 85 units x ₹8 = 680 + water 200 = ₹8,380
        val bill = bill(build(context(), reading = "1325", newMeterStart = ""))

        assertEquals(
            listOf(
                BillLine(BillLineKind.Rent, "Rent", 7_500L.rupees),
                BillLine(BillLineKind.Electricity, "Electricity", Paise(68_000)),
                BillLine(BillLineKind.FixedCharge, "Water", 200L.rupees),
            ),
            bill.lines,
        )
        assertEquals(8_380L.rupees, bill.total)
        assertEquals(listOf(1240, 1325, 85), listOf(bill.previousReading, bill.currentReading, bill.units))
        assertEquals(Paise(800), bill.rate)
        assertEquals(RenterId(1), bill.renterId)
        assertEquals(november, bill.month)
    }

    @Test
    fun theRateOnTheContextIsTheOneUsedAndSavedOnTheBill() {
        val bill = bill(build(context(rate = Paise(900)), reading = "1325", newMeterStart = ""))

        assertEquals(Paise(900), bill.rate)
        assertEquals(Paise(76_500), bill.lines.single { it.kind == BillLineKind.Electricity }.amount)
    }

    @Test
    fun aRenterWhoPaysElectricityDirectlyGetsNoElectricityLineAndNoReadings() {
        val renter = TestData.renter(billsElectricity = false)

        val bill = bill(build(context(renter, previousReading = null), reading = "9999", newMeterStart = ""))

        assertEquals(listOf(BillLineKind.Rent, BillLineKind.FixedCharge), bill.lines.map { it.kind })
        assertEquals(listOf(null, null, null, null), listOf(bill.previousReading, bill.currentReading, bill.units, bill.rate))
    }

    @Test
    fun onlyActiveFixedChargesAreBilled() {
        val renter = TestData.renter(
            fixedCharges = listOf(TestData.water, FixedCharge(label = "Garbage", amount = 50L.rupees, active = false)),
        )

        val bill = bill(build(context(renter), reading = "1240", newMeterStart = ""))

        assertEquals(listOf("Rent", "Electricity", "Water"), bill.lines.map { it.label })
    }

    @Test
    fun noUnitsUsedBillsZeroElectricity() {
        val bill = bill(build(context(), reading = "1240", newMeterStart = ""))

        assertEquals(Paise.Zero, bill.lines.single { it.kind == BillLineKind.Electricity }.amount)
    }

    @Test
    fun aReadingBelowTheLastOneIsAnErrorUnlessTheMeterWasReplaced() {
        assertEquals(
            Outcome.Failure(DomainError.ReadingBelowPrevious(previous = 1240, current = 1200)),
            build(context(), reading = "1200", newMeterStart = ""),
        )
    }

    @Test
    fun aReplacedMeterStartsFromItsNewStartingReading() {
        val bill = bill(build(context(), reading = "120", newMeterStart = "50"))

        assertEquals(true, bill.meterReset)
        assertEquals(listOf(50, 120, 70), listOf(bill.previousReading, bill.currentReading, bill.units))
        assertEquals(Paise(56_000), bill.lines.single { it.kind == BillLineKind.Electricity }.amount)
    }

    @Test
    fun aReadingBelowTheNewMetersStartIsStillAnError() {
        assertEquals(
            Outcome.Failure(DomainError.ReadingBelowPrevious(previous = 50, current = 40)),
            build(context(), reading = "40", newMeterStart = "50"),
        )
    }

    @Test
    fun aMissingOrBadReadingIsReportedOnTheField() {
        fun problems(reading: String, start: String = "") =
            ((build(context(), reading, start) as Outcome.Failure).error as DomainError.InvalidBill).problems

        assertEquals(mapOf(BillField.Reading to FieldProblem.Required), problems(" "))
        assertEquals(mapOf(BillField.Reading to FieldProblem.Invalid), problems("12a"))
        assertEquals(mapOf(BillField.Reading to FieldProblem.Invalid), problems("-5"))
        assertEquals(mapOf(BillField.NewMeterStart to FieldProblem.Invalid), problems("1300", start = "x"))
    }

    @Test
    fun totalDueIsThisBillPlusWhatWasStillOwed() {
        val owing = context().copy(pendingBefore = 1_880L.rupees)

        assertEquals(10_260L.rupees, owing.totalDue(bill(build(owing, reading = "1325", newMeterStart = ""))))
    }

    @Test
    fun anElectricRenterWithNoPreviousReadingIsAskedForTheMetersStartingReading() {
        val result = build(context(previousReading = null), reading = "1325", newMeterStart = "")

        assertEquals(
            Outcome.Failure(DomainError.InvalidBill(mapOf(BillField.NewMeterStart to FieldProblem.Required))),
            result,
        )
    }

    @Test
    fun usageFarAboveTheUsualIsFlaggedButNormalUsageIsNot() {
        val usual85 = context().copy(usualUnits = 85)

        fun unusual(reading: String, context: BillContext = usual85) =
            context.isUnusualUsage(bill(build(context, reading, newMeterStart = "")))

        assertEquals(false, unusual("1325")) // 85 units, the usual
        assertEquals(false, unusual("1440")) // 200 units, busy month but under 3x
        assertEquals(true, unusual("1500")) // 260 units, over 3x of 85 and over 100
        assertEquals(true, unusual("13250")) // the typo: 12,010 units
    }

    @Test
    fun aSmallBaselineDoesNotFlagSmallAmountsOrAFirstBill() {
        val tiny = context().copy(usualUnits = 10)
        val noHistory = context().copy(usualUnits = null)

        assertEquals(false, tiny.isUnusualUsage(bill(build(tiny, "1300", newMeterStart = "")))) // 60 units: over 3x of 10 but under 100
        assertEquals(false, noHistory.isUnusualUsage(bill(build(noHistory, "13250", newMeterStart = "")))) // nothing to compare with
    }

    @Test
    fun aRenterWhoPaysDirectlyIsNeverFlagged() {
        val direct = context(TestData.renter(billsElectricity = false), previousReading = null).copy(usualUnits = 85)

        assertEquals(false, direct.isUnusualUsage(bill(build(direct, "", newMeterStart = ""))))
    }

    @Test
    fun aBillIsDueOnTheDueDayOfTheFollowingMonth() {
        // PLAN.md: the November bill says "Please pay by 5 Dec".
        val bill = bill(build(context(), reading = "1325", newMeterStart = ""))

        assertEquals(LocalDate(2026, 12, 5), bill.dueOn)
    }

    @Test
    fun theDueDateRollsIntoTheNextYearAfterDecember() {
        val december = context().copy(month = BillingMonth.of(2026, 12))

        assertEquals(LocalDate(2027, 1, 5), bill(build(december, reading = "1325", newMeterStart = "")).dueOn)
    }

    @Test
    fun aDueDayThatTheFollowingMonthDoesNotHaveIsClampedToItsLastDay() {
        val january = context(TestData.renter(dueDay = 30)).copy(month = BillingMonth.of(2027, 1))

        assertEquals(LocalDate(2027, 2, 28), bill(build(january, reading = "1325", newMeterStart = "")).dueOn)
    }
}
