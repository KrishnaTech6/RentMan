package com.rentman.data.backup

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BackupCodecTest {
    private fun decode(text: String) = BackupCodec.decode(text)

    private fun BackupFile.decodedAgain() = (decode(BackupCodec.encode(this)) as Outcome.Success).value

    private fun invalid(file: BackupFile) = assertEquals(Outcome.Failure(DomainError.InvalidBackup), decode(BackupCodec.encode(file)))

    @Test
    fun aBackupWithEveryTableSurvivesTheRoundTripExactly() {
        assertEquals(sampleBackup(), sampleBackup().decodedAgain())
    }

    @Test
    fun removedRowsAndEveryNullableFieldAreKept() {
        val again = sampleBackup().decodedAgain()

        assertEquals(55L, again.rentChange.last().voidedAt)
        assertEquals(50L, again.payment.last().voidedAt)
        assertEquals(null, again.payment.last().note)
        assertEquals(202_611, again.renter.single().remindersMutedMonth)
    }

    @Test
    fun aBackupWithNoRowsAndNoSettingsIsValid() {
        val empty = BackupFile(BackupCodec.SCHEMA_VERSION, 1, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), null)

        assertEquals(empty, empty.decodedAgain())
    }

    @Test
    fun aFileFromANewerVersionIsRefusedWithItsVersion() {
        val text = BackupCodec.encode(sampleBackup().copy(schemaVersion = BackupCodec.SCHEMA_VERSION + 1))

        assertEquals(Outcome.Failure(DomainError.BackupFromNewerVersion(BackupCodec.SCHEMA_VERSION + 1)), decode(text))
    }

    @Test
    fun aNewerVersionIsRefusedEvenIfItHasFieldsThisVersionDoesNotKnow() {
        assertEquals(Outcome.Failure(DomainError.BackupFromNewerVersion(9)), decode("""{"schemaVersion":9,"somethingNew":[1,2,3]}"""))
    }

    @Test
    fun textThatIsNotABackupIsInvalid() {
        listOf("", "not json", "[]", "{}", """{"schemaVersion":"one"}""", """{"schemaVersion":1}""", """{"schemaVersion":1,"address":"x"}""").forEach {
            assertEquals(Outcome.Failure(DomainError.InvalidBackup), decode(it), "'$it'")
        }
    }

    @Test
    fun anOlderVersionZeroOrNegativeIsInvalid() {
        assertEquals(Outcome.Failure(DomainError.InvalidBackup), decode(BackupCodec.encode(sampleBackup().copy(schemaVersion = 0))))
    }

    @Test
    fun fieldsThisVersionDoesNotKnowAreIgnored() {
        val text = BackupCodec.encode(sampleBackup()).replaceFirst("{", """{"futureField":{"a":1},""")

        assertEquals(sampleBackup(), (decode(text) as Outcome.Success).value)
    }

    @Test
    fun aRowPointingAtSomethingThatIsNotInTheFileIsInvalid() {
        val s = sampleBackup()
        invalid(s.copy(renter = listOf(s.renter.single().copy(addressId = 99))))
        invalid(s.copy(fixedCharge = listOf(s.fixedCharge.single().copy(renterId = 99))))
        invalid(s.copy(rentChange = s.rentChange.map { it.copy(renterId = 99) }))
        invalid(s.copy(bill = listOf(s.bill.single().copy(renterId = 99))))
        invalid(s.copy(billLine = s.billLine.map { it.copy(billId = 99) }))
        invalid(s.copy(payment = s.payment.map { it.copy(renterId = 99) }))
    }

    @Test
    fun twoRowsWithTheSameIdAreInvalid() {
        val s = sampleBackup()
        invalid(s.copy(address = s.address + s.address))
        invalid(s.copy(payment = s.payment.map { it.copy(id = 1) }))
        invalid(s.copy(billLine = s.billLine.map { it.copy(id = 1) }))
    }

    @Test
    fun twoBillsForTheSameRenterAndMonthAreInvalid() {
        val s = sampleBackup()
        invalid(s.copy(bill = s.bill + s.bill.single().copy(id = 2)))
    }

    @Test
    fun aNameThisAppDoesNotKnowForABillLineOrPaymentModeIsInvalid() {
        val s = sampleBackup()
        invalid(s.copy(billLine = s.billLine.map { it.copy(kind = "Surcharge") }))
        invalid(s.copy(payment = s.payment.map { it.copy(mode = "Crypto") }))
    }

    @Test
    fun valuesThatWouldCrashTheAppLaterAreInvalid() {
        val s = sampleBackup()
        val renter = s.renter.single()
        invalid(s.copy(renter = listOf(renter.copy(dueDay = 0))))
        invalid(s.copy(renter = listOf(renter.copy(dueDay = 32))))
        invalid(s.copy(renter = listOf(renter.copy(raisePercent = 101))))
        invalid(s.copy(renter = listOf(renter.copy(raiseEveryMonths = 0))))
        invalid(s.copy(renter = listOf(renter.copy(firstBillMonth = 202_613)))) // there is no month 13
        invalid(s.copy(renter = listOf(renter.copy(remindersMutedMonth = 202_600))))
        invalid(s.copy(renter = listOf(renter.copy(raiseSnoozedUntil = 5))))
        invalid(s.copy(bill = listOf(s.bill.single().copy(month = 202_620))))
        invalid(s.copy(rentChange = s.rentChange.map { it.copy(effectiveMonth = 0) }))
    }

    @Test
    fun datesThatDoNotExistOrAreNowhereNearTodayAreInvalid() {
        val s = sampleBackup()
        val renter = s.renter.single()
        listOf(Long.MAX_VALUE, 9_999_999_999_999, -1, 1_000_000).forEach { day ->
            invalid(s.copy(renter = listOf(renter.copy(agreementStart = day))))
            invalid(s.copy(renter = listOf(renter.copy(leftOn = day))))
            invalid(s.copy(bill = listOf(s.bill.single().copy(dueOn = day))))
            invalid(s.copy(payment = s.payment.map { it.copy(paidOn = day) }))
            invalid(s.copy(rentChange = s.rentChange.map { it.copy(decidedOn = day) }))
        }
    }

    @Test
    fun anOrdinaryRangeOfDatesIsFine() {
        val s = sampleBackup()
        val twoThousand = 10_957L // 1 Jan 2000
        val twoTwoHundred = 84_005L // 31 Dec 2200
        assertEquals(
            true,
            decode(BackupCodec.encode(s.copy(renter = listOf(s.renter.single().copy(agreementStart = twoThousand, leftOn = twoTwoHundred))))) is Outcome.Success,
        )
        assertEquals(true, decode(BackupCodec.encode(s.copy(payment = s.payment.map { it.copy(paidOn = twoTwoHundred) }))) is Outcome.Success)
    }

    @Test
    fun aPhoneNumberTheAppCouldNeverHaveStoredIsInvalid() {
        val s = sampleBackup()
        val renter = s.renter.single()
        // These would end up inside the wa.me link: "9876543210&text=x" changes what the link says.
        listOf("", "9876543210", "9198765432101234", "91987654321&", "+919876543210", "91 98765 43210", "9198765432a0").forEach {
            invalid(s.copy(renter = listOf(renter.copy(phone = it))))
        }
    }

    @Test
    fun theNumbersThisAppStoresAreValid() {
        val s = sampleBackup()
        listOf("919876543210", "971501234567", "123456789012345").forEach {
            assertEquals(true, decode(BackupCodec.encode(s.copy(renter = listOf(s.renter.single().copy(phone = it))))) is Outcome.Success, it)
        }
    }

    @Test
    fun aBillsMeterPhotoSurvivesTheRoundTripAndAFileWithoutOneIsStillValid() {
        val s = sampleBackup()
        val withPhoto = s.copy(bill = listOf(s.bill.single().copy(meterPhotoPath = "9f3a1c-2b.jpg")))

        assertEquals("9f3a1c-2b.jpg", withPhoto.decodedAgain().bill.single().meterPhotoPath)
        assertEquals(null, s.decodedAgain().bill.single().meterPhotoPath)
    }

    @Test
    fun theLandlordsOwnMessagesSurviveTheRoundTripAndAnOlderFileWithoutThemMeansOurs() {
        val s = sampleBackup()
        val own = s.copy(settings = s.settings!!.copy(billMessage = "Namaste {first_name}", monthReviewMessage = "{month}: {received}"))

        assertEquals(own.settings, own.decodedAgain().settings)
        // A file from before version 2 has no such fields at all.
        val older = BackupCodec.encode(s).replace(Regex(""","(billMessage|paymentMessage|monthReviewMessage)":null"""), "")
        assertEquals(false, "billMessage" in older)
        assertEquals(null, (decode(older) as Outcome.Success).value.settings!!.billMessage)
    }

    @Test
    fun aMeterPhotoNameThatCouldReachOutsideThePhotoFolderIsInvalid() {
        val s = sampleBackup()
        // The name is joined to the app's photo folder to open it, so it must be a plain file name.
        listOf("", "../rentman.db", "..\\x.jpg", "a/b.jpg", "/etc/passwd", "photo", "photo.png", ".jpg", "a b.jpg", "x".repeat(70) + ".jpg").forEach {
            invalid(s.copy(bill = listOf(s.bill.single().copy(meterPhotoPath = it))))
        }
    }

    @Test
    fun settingsThatWouldBreakReminderTimesAreInvalid() {
        val settings = sampleBackup().settings!!
        invalid(sampleBackup().copy(settings = settings.copy(reminderMinuteOfDay = -1)))
        invalid(sampleBackup().copy(settings = settings.copy(reminderMinuteOfDay = 1_440)))
        invalid(sampleBackup().copy(settings = settings.copy(reminderRepeatDays = 5)))
        invalid(sampleBackup().copy(settings = settings.copy(globalRatePaise = 0)))
        invalid(sampleBackup().copy(settings = settings.copy(id = 2)))
    }

    @Test
    fun decodeReturnsAFailureNeverAnException() {
        assertIs<Outcome.Failure>(decode("\u0000\u0001 {{{"))
    }
}
