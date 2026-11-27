@file:OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest

package com.rentman.sharedui.feature.payment

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.fold
import com.rentman.domain.money.MoneyInput
import com.rentman.domain.payments.ObserveRenterLedgerUseCase
import com.rentman.domain.payments.PaymentField
import com.rentman.domain.payments.RecordPaymentUseCase
import com.rentman.domain.payments.RenterLedger
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.time.GetTodayUseCase
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.AmountChanged
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.Back
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.DayStepped
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.ModeChanged
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.NoteChanged
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.Retry
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.Save
import com.rentman.sharedui.feature.payment.RecordPaymentIntent.UseFullAmount
import com.rentman.sharedui.feature.payment.RecordPaymentUiState.Content
import com.rentman.sharedui.feature.payment.RecordPaymentUiState.Error
import com.rentman.sharedui.feature.payment.RecordPaymentUiState.Loading
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

private typealias State = RecordPaymentUiState

internal class RecordPaymentViewModel(
    renterId: RenterId,
    observeLedger: ObserveRenterLedgerUseCase,
    private val recordPayment: RecordPaymentUseCase,
    private val getToday: GetTodayUseCase,
) : MviViewModel<RecordPaymentIntent, State, RecordPaymentNavEvent>() {

    private val unchanged: Reducer<State> = { it }

    override val uiState: StateFlow<State> = merge(
        loadReducers(observeLedger, renterId),
        inputReducers(),
        saveReducers(),
        navigationReducers(),
    ).toState(Loading)

    private fun loadReducers(observeLedger: ObserveRenterLedgerUseCase, renterId: RenterId): Flow<Reducer<State>> =
        intents.filterIsInstance<Retry>().map { }.onStart { emit(Unit) }
            .flatMapLatest {
                observeLedger(renterId)
                    .map { outcome -> outcome.fold(onSuccess = ::withLedger, onFailure = ::toError) }
                    .onStart { emit { Loading } }
            }

    private fun inputReducers(): Flow<Reducer<State>> = intents.map { intent ->
        when (intent) {
            is AmountChanged -> edit(PaymentField.Amount) { it.copy(amount = intent.text) }
            UseFullAmount -> edit(PaymentField.Amount) { it.copy(amount = MoneyInput.text(it.pending)) }
            is ModeChanged -> edit { it.copy(mode = intent.mode) }
            is DayStepped -> edit(PaymentField.Date) {
                it.copy(paidOn = minOf(it.paidOn.plus(intent.days, DateTimeUnit.DAY), it.today))
            }
            is NoteChanged -> edit(PaymentField.Note) { it.copy(note = intent.text) }
            is Save, Back, Retry -> unchanged
        }
    }

    private fun saveReducers(): Flow<Reducer<State>> = intents.filterIsInstance<Save>().flatMapFirst { save ->
        flow {
            emit(content { it.copy(isSaving = true, problems = emptyMap(), saveError = null) })
            val outcome = recordPayment(save.form)
            emit(
                outcome.fold(
                    onSuccess = { content { it.copy(isSaving = false) } },
                    onFailure = { error -> content { it.copy(isSaving = false).withError(error) } },
                ),
            )
            if (outcome is Outcome.Success) {
                navigate(RecordPaymentNavEvent.Saved(save.post?.let { PaymentPost(it, outcome.value.amount) }))
            }
        }
    }

    private fun navigationReducers(): Flow<Reducer<State>> = intents
        .onEach { if (it == Back) navigate(RecordPaymentNavEvent.Close) }
        .map { unchanged }

    /** The ledger can arrive again while the user types: keep what they typed. */
    private fun withLedger(ledger: RenterLedger): Reducer<State> = { state ->
        if (state is Content) {
            state.copy(summary = ledger.summary, pending = ledger.pending)
        } else {
            Content(summary = ledger.summary, pending = ledger.pending, today = getToday())
        }
    }

    private fun toError(error: DomainError): Reducer<State> = { Error(error) }

    private fun content(change: (Content) -> Content): Reducer<State> = { state ->
        if (state is Content) change(state) else state
    }

    /** A change to one field clears that field's problem and any save error. */
    private fun edit(vararg fields: PaymentField, change: (Content) -> Content): Reducer<State> =
        content { change(it).copy(problems = it.problems - fields.toSet(), saveError = null) }

    private fun Content.withError(error: DomainError): Content =
        if (error is DomainError.InvalidPayment) copy(problems = error.problems) else copy(saveError = error)
}
