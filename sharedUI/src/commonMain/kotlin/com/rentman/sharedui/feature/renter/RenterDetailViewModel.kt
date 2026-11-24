@file:OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest

package com.rentman.sharedui.feature.renter

import com.rentman.domain.DomainError
import com.rentman.domain.fold
import com.rentman.domain.payments.ObserveRenterLedgerUseCase
import com.rentman.domain.billing.RemoveBillUseCase
import com.rentman.domain.billing.RestoreBillUseCase
import com.rentman.domain.payments.RemovePaymentUseCase
import com.rentman.domain.payments.RenterLedger
import com.rentman.domain.payments.RestorePaymentUseCase
import com.rentman.domain.tenancy.MarkRenterLeftUseCase
import com.rentman.domain.tenancy.MoveRenterBackUseCase
import com.rentman.domain.tenancy.MuteRemindersUseCase
import com.rentman.domain.tenancy.RenterId
import com.rentman.sharedui.feature.renter.RenterDetailIntent.Back
import com.rentman.sharedui.feature.renter.RenterDetailIntent.Edit
import com.rentman.sharedui.feature.renter.RenterDetailIntent.MarkLeft
import com.rentman.sharedui.feature.renter.RenterDetailIntent.MoveBack
import com.rentman.sharedui.feature.renter.RenterDetailIntent.RecordPayment
import com.rentman.sharedui.feature.renter.RenterDetailIntent.RemoveBill
import com.rentman.sharedui.feature.renter.RenterDetailIntent.RemovePayment
import com.rentman.sharedui.feature.renter.RenterDetailIntent.UndoRemoveBill
import com.rentman.sharedui.feature.renter.RenterDetailIntent.Retry
import com.rentman.sharedui.feature.renter.RenterDetailIntent.SetRemindersMuted
import com.rentman.sharedui.feature.renter.RenterDetailIntent.UndoRemove
import com.rentman.sharedui.feature.renter.RenterDetailUiState.Content
import com.rentman.sharedui.feature.renter.RenterDetailUiState.Error
import com.rentman.sharedui.feature.renter.RenterDetailUiState.Loading
import com.rentman.sharedui.mvi.MviViewModel
import com.rentman.sharedui.mvi.Reducer
import com.rentman.sharedui.mvi.flatMapFirst
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

private typealias DetailState = RenterDetailUiState

internal class RenterDetailViewModel(
    private val renterId: RenterId,
    observeLedger: ObserveRenterLedgerUseCase,
    private val muteReminders: MuteRemindersUseCase,
    private val markLeft: MarkRenterLeftUseCase,
    private val moveBack: MoveRenterBackUseCase,
    private val removePayment: RemovePaymentUseCase,
    private val restorePayment: RestorePaymentUseCase,
    private val removeBill: RemoveBillUseCase,
    private val restoreBill: RestoreBillUseCase,
) : MviViewModel<RenterDetailIntent, DetailState, RenterDetailNavEvent>() {

    private val unchanged: Reducer<DetailState> = { it }

    override val uiState: StateFlow<DetailState> = merge(
        loadReducers(observeLedger, renterId),
        removeReducers(),
        removeBillReducers(),
        muteReducers(),
        leavingReducers(),
        undoReducers(),
        undoExpiryReducers(),
        undoBillReducers(),
        undoBillExpiryReducers(),
        navigationReducers(),
    ).toState(Loading)

    private fun loadReducers(observeLedger: ObserveRenterLedgerUseCase, renterId: RenterId): Flow<Reducer<DetailState>> =
        intents.filterIsInstance<Retry>().map { }.onStart { emit(Unit) }
            .flatMapLatest {
                observeLedger(renterId)
                    .map { outcome -> outcome.fold(onSuccess = ::withLedger, onFailure = ::toError) }
                    .onStart { emit { Loading } }
            }

    private fun removeReducers(): Flow<Reducer<DetailState>> = intents.filterIsInstance<RemovePayment>().flatMapFirst { remove ->
        flow {
            emit(
                removePayment(remove.payment.id).fold(
                    onSuccess = { content { it.copy(removed = remove.payment, actionError = null) } },
                    onFailure = { error -> content { it.copy(actionError = error) } },
                ),
            )
        }
    }

    private fun removeBillReducers(): Flow<Reducer<DetailState>> = intents.filterIsInstance<RemoveBill>().flatMapFirst { remove ->
        flow {
            emit(
                removeBill(remove.bill).fold(
                    onSuccess = { content { it.copy(removedBill = remove.bill, actionError = null) } },
                    onFailure = ::actionFailed,
                ),
            )
        }
    }

    private fun undoBillReducers(): Flow<Reducer<DetailState>> = intents.filterIsInstance<UndoRemoveBill>().flatMapFirst { undo ->
        flow {
            emit(
                restoreBill(undo.bill).fold(
                    onSuccess = { content { it.copy(removedBill = null, actionError = null) } },
                    onFailure = ::actionFailed,
                ),
            )
        }
    }

    /** The bill's Undo offer lasts a few seconds too. */
    private fun undoBillExpiryReducers(): Flow<Reducer<DetailState>> = intents.filterIsInstance<RemoveBill>().flatMapLatest {
        flow {
            delay(UNDO_MILLIS)
            emit(content { it.copy(removedBill = null) })
        }
    }

    private fun muteReducers(): Flow<Reducer<DetailState>> = intents.filterIsInstance<SetRemindersMuted>().flatMapFirst { change ->
        flow {
            emit(
                muteReminders(renterId, change.muted).fold(
                    onSuccess = { content { it.copy(actionError = null) } },
                    onFailure = { error -> content { it.copy(actionError = error) } },
                ),
            )
        }
    }

    private fun leavingReducers(): Flow<Reducer<DetailState>> = merge(
        intents.filterIsInstance<MarkLeft>().flatMapFirst {
            flow { emit(markLeft(renterId).fold(onSuccess = { content { it.copy(actionError = null) } }, onFailure = ::actionFailed)) }
        },
        intents.filterIsInstance<MoveBack>().flatMapFirst {
            flow { emit(moveBack(renterId).fold(onSuccess = { content { it.copy(actionError = null) } }, onFailure = ::actionFailed)) }
        },
    )

    private fun actionFailed(error: DomainError): Reducer<DetailState> = content { it.copy(actionError = error) }

    private fun undoReducers(): Flow<Reducer<DetailState>> = intents.filterIsInstance<UndoRemove>().flatMapFirst { undo ->
        flow {
            emit(
                restorePayment(undo.payment.id).fold(
                    onSuccess = { content { it.copy(removed = null, actionError = null) } },
                    onFailure = { error -> content { it.copy(actionError = error) } },
                ),
            )
        }
    }

    /** The Undo offer lasts a few seconds. A newer removal starts the wait again. */
    private fun undoExpiryReducers(): Flow<Reducer<DetailState>> = intents.filterIsInstance<RemovePayment>().flatMapLatest {
        flow {
            delay(UNDO_MILLIS)
            emit(content { it.copy(removed = null) })
        }
    }

    private fun navigationReducers(): Flow<Reducer<DetailState>> = intents
        .onEach { intent ->
            when (intent) {
                is SetRemindersMuted, MarkLeft, MoveBack -> Unit
                Edit -> navigate(RenterDetailNavEvent.OpenEdit)
                RecordPayment -> navigate(RenterDetailNavEvent.OpenRecordPayment)
                Back -> navigate(RenterDetailNavEvent.Close)
                else -> Unit
            }
        }
        .map { unchanged }

    private fun withLedger(ledger: RenterLedger): Reducer<DetailState> = { state ->
        if (state is Content) state.copy(ledger = ledger) else Content(ledger)
    }

    private fun toError(error: DomainError): Reducer<DetailState> = { Error(error) }

    private fun content(change: (Content) -> Content): Reducer<DetailState> = { state ->
        if (state is Content) change(state) else state
    }

    private companion object {
        const val UNDO_MILLIS = 5_000L
    }
}
