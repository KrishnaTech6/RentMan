@file:OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest

package com.rentman.sharedui.feature.renter

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.flatMap
import com.rentman.domain.map
import com.rentman.domain.fold
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.BuildFixedChargeUseCase
import com.rentman.domain.tenancy.ChargeField
import com.rentman.domain.tenancy.ObserveAddressesUseCase
import com.rentman.domain.tenancy.RenterField
import com.rentman.domain.tenancy.ObserveRenterFormUseCase
import com.rentman.domain.tenancy.RenterForm
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.SaveRenterUseCase
import com.rentman.domain.tenancy.UpdateRenterUseCase
import com.rentman.domain.time.GetTodayUseCase
import com.rentman.sharedui.feature.renter.AddRenterIntent.AddCharge
import com.rentman.sharedui.feature.renter.AddRenterIntent.AskToRaiseChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.Back
import com.rentman.sharedui.feature.renter.AddRenterIntent.BillsElectricityChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.ChargeAmountChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.ChargeLabelChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.DueDayChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.NameChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.OwnRateChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.PhoneChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.PickAddress
import com.rentman.sharedui.feature.renter.AddRenterIntent.RemoveCharge
import com.rentman.sharedui.feature.renter.AddRenterIntent.RentChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.Retry
import com.rentman.sharedui.feature.renter.AddRenterIntent.Save
import com.rentman.sharedui.feature.renter.AddRenterIntent.StartMonthStepped
import com.rentman.sharedui.feature.renter.AddRenterIntent.StartingReadingChanged
import com.rentman.sharedui.feature.renter.AddRenterIntent.UnitChanged
import com.rentman.sharedui.feature.renter.AddRenterUiState.Content
import com.rentman.sharedui.feature.renter.AddRenterUiState.Error
import com.rentman.sharedui.feature.renter.AddRenterUiState.Loading
import com.rentman.sharedui.mvi.MviViewModel
import com.rentman.sharedui.mvi.Reducer
import com.rentman.sharedui.mvi.flatMapFirst
import com.rentman.sharedui.navigation.AddressSelection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

private typealias State = AddRenterUiState

internal class AddRenterViewModel(
    /** Null to add a renter, or the renter to edit. */
    private val renterId: RenterId?,
    observeAddresses: ObserveAddressesUseCase,
    observeRenterForm: ObserveRenterFormUseCase,
    private val saveRenter: SaveRenterUseCase,
    private val updateRenter: UpdateRenterUseCase,
    getToday: GetTodayUseCase,
    addressSelection: AddressSelection,
    private val buildFixedCharge: BuildFixedChargeUseCase,
) : MviViewModel<AddRenterIntent, State, AddRenterNavEvent>() {

    private val unchanged: Reducer<State> = { it }
    private val thisMonth = BillingMonth.of(getToday())

    override val uiState: StateFlow<State> = merge(
        loadReducers(observeAddresses, observeRenterForm),
        inputReducers(),
        pickedAddressReducers(addressSelection),
        saveReducers(),
        navigationReducers(),
    ).toState(Loading)

    private fun loadReducers(observeAddresses: ObserveAddressesUseCase, observeRenterForm: ObserveRenterFormUseCase): Flow<Reducer<State>> =
        intents.filterIsInstance<Retry>().map { }.onStart { emit(Unit) }
            .flatMapLatest {
                // Editing starts from the saved renter. Adding has nothing to fill in.
                val saved: Flow<Outcome<RenterForm?>> =
                    if (renterId == null) flowOf(Outcome.Success(null)) else observeRenterForm(renterId).map { outcome -> outcome.map { it } }
                combine(observeAddresses(), saved) { addresses, form -> addresses.flatMap { list -> form.map { list to it } } }
                    .map { outcome -> outcome.fold(onSuccess = { (addresses, form) -> withData(addresses, form) }, onFailure = ::toError) }
                    .onStart { emit { Loading } }
            }

    private fun inputReducers(): Flow<Reducer<State>> = intents.map { intent ->
        when (intent) {
            is NameChanged -> edit(RenterField.Name) { it.copy(name = intent.text) }
            is PhoneChanged -> edit(RenterField.Phone) { it.copy(phone = intent.text) }
            is UnitChanged -> edit(RenterField.Unit) { it.copy(unit = intent.text) }
            is RentChanged -> edit(RenterField.Rent) { it.copy(rent = intent.text) }
            is DueDayChanged -> edit(RenterField.DueDay) { it.copy(dueDay = intent.text) }
            is StartMonthStepped -> edit { it.copy(agreementStart = BillingMonth.of(it.agreementStart).plusMonths(intent.months).atStartOfMonth()) }
            is AskToRaiseChanged -> edit { it.copy(askToRaise = intent.ask) }
            is BillsElectricityChanged ->
                edit(RenterField.StartingReading, RenterField.OwnRate) { it.copy(billsElectricity = intent.bills) }
            is StartingReadingChanged -> edit(RenterField.StartingReading) { it.copy(startingReading = intent.text) }
            is OwnRateChanged -> edit(RenterField.OwnRate) { it.copy(ownRate = intent.text) }
            is ChargeLabelChanged -> update {
                it.copy(
                    chargeLabel = intent.text,
                    chargeProblems = it.chargeProblems - ChargeField.Label,
                    problems = it.problems - RenterField.Charge,
                )
            }
            is ChargeAmountChanged -> update {
                it.copy(
                    chargeAmount = intent.text,
                    chargeProblems = it.chargeProblems - ChargeField.Amount,
                    problems = it.problems - RenterField.Charge,
                )
            }
            // The use case is pure, so it can run inside the reducer and see the charges already on the list.
            is AddCharge -> update { content ->
                buildFixedCharge(intent.label, intent.amount, content.fixedCharges).fold(
                    onSuccess = { charge ->
                        content.copy(
                            fixedCharges = content.fixedCharges + charge,
                            chargeLabel = "",
                            chargeAmount = "",
                            chargeProblems = emptyMap(),
                            problems = content.problems - RenterField.Charge,
                        )
                    },
                    onFailure = { error ->
                        content.copy(chargeProblems = (error as? DomainError.InvalidCharge)?.problems.orEmpty())
                    },
                )
            }
            is RemoveCharge -> update { it.copy(fixedCharges = it.fixedCharges.filterIndexed { index, _ -> index != intent.index }) }
            is PickAddress, is Save, Back, Retry -> unchanged
        }
    }

    private fun pickedAddressReducers(addressSelection: AddressSelection): Flow<Reducer<State>> =
        addressSelection.picked.map { id ->
            edit(RenterField.Address, RenterField.Unit) { it.copy(addressId = id) }
        }

    private fun saveReducers(): Flow<Reducer<State>> = intents.filterIsInstance<Save>().flatMapFirst { save ->
        flow {
            emit(update { it.copy(isSaving = true, problems = emptyMap(), saveError = null) })
            val outcome = if (renterId == null) saveRenter(save.form) else updateRenter(renterId, save.form)
            emit(outcome.fold(onSuccess = { saved() }, onFailure = ::failed))
            if (outcome is Outcome.Success) navigate(AddRenterNavEvent.Saved)
        }
    }

    private fun navigationReducers(): Flow<Reducer<State>> = intents
        .onEach { intent ->
            when (intent) {
                PickAddress -> navigate(AddRenterNavEvent.PickAddress)
                Back -> navigate(AddRenterNavEvent.Close)
                else -> Unit
            }
        }
        .map { unchanged }

    /** The first time, the form is filled in. After that only the list of addresses is kept up to date, so typing isn't lost. */
    private fun withData(addresses: List<Address>, saved: RenterForm?): Reducer<State> = { state ->
        when {
            state is Content -> state.copy(addresses = addresses)
            saved == null -> Content(addresses = addresses, agreementStart = thisMonth.atStartOfMonth())
            else -> Content(
                addresses = addresses,
                renterId = renterId,
                addressId = saved.addressId,
                unit = saved.unit,
                name = saved.name,
                phone = saved.phone,
                rent = saved.rent,
                dueDay = saved.dueDay,
                agreementStart = saved.agreementStart,
                askToRaise = saved.askToRaise,
                billsElectricity = saved.billsElectricity,
                startingReading = saved.startingReading,
                ownRate = saved.ownRate,
                fixedCharges = saved.fixedCharges,
            )
        }
    }

    private fun toError(error: DomainError): Reducer<State> = { Error(error) }

    private fun saved(): Reducer<State> = update { it.copy(isSaving = false) }

    private fun failed(error: DomainError): Reducer<State> = update {
        when (error) {
            is DomainError.InvalidRenter -> it.copy(isSaving = false, problems = error.problems)
            else -> it.copy(isSaving = false, saveError = error)
        }
    }

    private fun update(change: (Content) -> Content): Reducer<State> = { state ->
        if (state is Content) change(state) else state
    }

    /** A change the user made to the form. It also clears the problems shown on the [fields] it touches. */
    private fun edit(vararg fields: RenterField, change: (Content) -> Content): Reducer<State> =
        update { content -> change(content).let { it.copy(problems = it.problems - fields.toSet(), saveError = null) } }
}
