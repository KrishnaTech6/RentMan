@file:OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest

package com.rentman.sharedui.feature.bill

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillContext
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.BuildBillUseCase
import com.rentman.domain.billing.CreateBillUseCase
import com.rentman.domain.billing.SuggestMeterReadingUseCase
import com.rentman.domain.billing.ObserveBillContextUseCase
import com.rentman.domain.fold
import com.rentman.domain.map
import com.rentman.domain.tenancy.RenterId
import com.rentman.sharedui.feature.bill.CreateBillIntent.Back
import com.rentman.sharedui.feature.bill.CreateBillIntent.ChangeReading
import com.rentman.sharedui.feature.bill.CreateBillIntent.ClearMeterPhoto
import com.rentman.sharedui.feature.bill.CreateBillIntent.MeterScanned
import com.rentman.sharedui.feature.bill.CreateBillIntent.ReadingPicked
import com.rentman.sharedui.feature.bill.CreateBillIntent.MeterReplacedChanged
import com.rentman.sharedui.feature.bill.CreateBillIntent.NewMeterStartChanged
import com.rentman.sharedui.feature.bill.CreateBillIntent.ReadingChanged
import com.rentman.sharedui.feature.bill.CreateBillIntent.Retry
import com.rentman.sharedui.feature.bill.CreateBillIntent.Review
import com.rentman.sharedui.feature.bill.CreateBillIntent.Save
import com.rentman.sharedui.feature.bill.CreateBillUiState.Content
import com.rentman.sharedui.feature.bill.CreateBillUiState.Error
import com.rentman.sharedui.feature.bill.CreateBillUiState.Loading
import com.rentman.sharedui.mvi.MviViewModel
import com.rentman.sharedui.mvi.Reducer
import com.rentman.sharedui.mvi.flatMapFirst
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

private typealias State = CreateBillUiState

internal class CreateBillViewModel(
    renterId: RenterId,
    month: BillingMonth,
    observeContext: ObserveBillContextUseCase,
    private val buildBill: BuildBillUseCase,
    private val suggestReading: SuggestMeterReadingUseCase,
    private val createBill: CreateBillUseCase,
) : MviViewModel<CreateBillIntent, State, CreateBillNavEvent>() {

    private val unchanged: Reducer<State> = { it }

    override val uiState: StateFlow<State> = merge(
        loadReducers(observeContext, renterId, month),
        inputReducers(),
        saveReducers(),
        navigationReducers(),
    ).toState(Loading)

    private fun loadReducers(observeContext: ObserveBillContextUseCase, renterId: RenterId, month: BillingMonth): Flow<Reducer<State>> =
        intents.filterIsInstance<Retry>().map { }.onStart { emit(Unit) }
            .flatMapLatest {
                observeContext(renterId, month)
                    .map { outcome -> outcome.fold(onSuccess = ::withContext, onFailure = ::toError) }
                    .onStart { emit { Loading } }
            }

    private fun inputReducers(): Flow<Reducer<State>> = intents.map { intent ->
        when (intent) {
            is ReadingChanged -> content { it.copy(reading = intent.text, scanResult = null, showProblems = false, saveError = null).withPreview() }
            is NewMeterStartChanged -> content { it.copy(newMeterStart = intent.text, showProblems = false, saveError = null).withPreview() }
            is MeterReplacedChanged -> content {
                it.copy(
                    meterReplaced = intent.replaced,
                    newMeterStart = if (intent.replaced) it.newMeterStart else "",
                    showProblems = false,
                ).withPreview()
            }
            is MeterScanned -> content { it.scanned(intent) }
            is ReadingPicked -> content {
                it.copy(reading = intent.value.toString(), scanResult = ScanResult.Read, showProblems = false, saveError = null).withPreview()
            }
            ClearMeterPhoto -> content { it.copy(meterPhoto = null, scanResult = null, readingChoices = emptyList()).withPreview() }
            Review -> content { if (it.preview is Outcome.Success) it.copy(step = BillStep.Review) else it.copy(showProblems = true) }
            ChangeReading -> content { it.copy(step = BillStep.Reading, saveError = null) }
            is Save, Back, Retry -> unchanged
        }
    }

    private fun saveReducers(): Flow<Reducer<State>> = intents.filterIsInstance<Save>().flatMapFirst { save ->
        flow {
            emit(content { it.copy(isSaving = true, saveError = null) })
            val outcome = createBill(save.bill)
            emit(
                outcome.fold(
                    onSuccess = { content { it.copy(isSaving = false) } },
                    onFailure = { error -> content { it.copy(isSaving = false, saveError = error) } },
                ),
            )
            if (outcome is Outcome.Success) navigate(CreateBillNavEvent.Saved(save.whatsAppUrl))
        }
    }

    private fun navigationReducers(): Flow<Reducer<State>> = intents
        .onEach { if (it == Back) navigate(CreateBillNavEvent.Close) }
        .map { unchanged }

    /** The context can arrive again while the user types (a bill saved elsewhere): keep what they typed. */
    private fun withContext(context: BillContext): Reducer<State> = { state ->
        if (state is Content) {
            state.copy(context = context).withPreview()
        } else {
            val first = Content(
                context = context,
                // A renter who pays electricity directly has nothing to type, so they start at the bill.
                step = if (context.summary.renter.billsElectricity) BillStep.Reading else BillStep.Review,
                preview = Outcome.Failure(DomainError.InvalidBill(emptyMap())),
            )
            first.withPreview()
        }
    }

    private fun toError(error: DomainError): Reducer<State> = { Error(error) }

    private fun content(change: (Content) -> Content): Reducer<State> = { state ->
        if (state is Content) change(state) else state
    }

    /** A photo is kept even if no number could be read from it. A believable number replaces the reading; all can be tapped. */
    private fun Content.scanned(scan: MeterScanned): Content {
        val suggestion = suggestReading(scan.lines, lastReading)
        return copy(
            meterPhoto = scan.photo,
            reading = suggestion.reading?.toString() ?: reading,
            scanResult = if (suggestion.reading != null) ScanResult.Read else ScanResult.NothingFound,
            readingChoices = suggestion.choices,
            showProblems = false,
            saveError = null,
        ).withPreview()
    }

    private fun Content.withPreview(): Content = copy(
        preview = buildBill(context, reading, if (meterReplaced) newMeterStart else "").map { bill ->
            // The photo is proof of a reading, so it only goes on a bill that has one.
            if (context.summary.renter.billsElectricity) bill.copy(meterPhotoPath = meterPhoto) else bill
        },
    )
}
