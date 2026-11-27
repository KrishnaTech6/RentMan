package com.rentman.sharedui.feature.bill

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillContext

internal enum class BillStep { Reading, Review }

/** What the photo gave: a number to check, or nothing the landlord has to type in themselves. */
internal enum class ScanResult { Read, NothingFound }

internal sealed interface CreateBillIntent {
    data class ReadingChanged(val text: String) : CreateBillIntent
    data class NewMeterStartChanged(val text: String) : CreateBillIntent
    data class MeterReplacedChanged(val replaced: Boolean) : CreateBillIntent
    /** The camera took a photo of the meter. [lines] are the text the phone read from it. */
    data class MeterScanned(val photo: String, val lines: List<String>) : CreateBillIntent
    /** One of the numbers read from the photo was tapped as the reading. */
    data class ReadingPicked(val value: Int) : CreateBillIntent
    data object ClearMeterPhoto : CreateBillIntent
    data object Review : CreateBillIntent
    data object ChangeReading : CreateBillIntent
    /** [whatsAppUrl] is set for "Save & send on WhatsApp": the screen opens it once the bill is stored. */
    data class Save(val bill: Bill, val whatsAppUrl: String? = null) : CreateBillIntent
    data object Back : CreateBillIntent
    data object Retry : CreateBillIntent
}

internal sealed interface CreateBillUiState {
    data object Loading : CreateBillUiState

    data class Error(val error: DomainError) : CreateBillUiState

    data class Content(
        val context: BillContext,
        val step: BillStep,
        val reading: String = "",
        val newMeterStart: String = "",
        val meterReplaced: Boolean = false,
        /** The name of the photo of the meter, saved with the bill as proof. */
        val meterPhoto: String? = null,
        /** Set after a scan, until the reading is typed over. */
        val scanResult: ScanResult? = null,
        /** The numbers read from the photo that could be the reading, likeliest first. Kept while the photo is. */
        val readingChoices: List<Int> = emptyList(),
        /** The bill the typed reading would give, or why it can't be made yet. Recalculated on every change. */
        val preview: Outcome<Bill>,
        /** Show the problems in [preview]. Off while typing, so an empty field isn't red before it was tried. */
        val showProblems: Boolean = false,
        /** A failure that isn't about the reading, e.g. the bill already exists or the disk. */
        val saveError: DomainError? = null,
        val isSaving: Boolean = false,
    ) : CreateBillUiState
}

internal sealed interface CreateBillNavEvent {
    data class Saved(val whatsAppUrl: String?) : CreateBillNavEvent
    data object Close : CreateBillNavEvent
}
