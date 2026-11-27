package com.rentman.sharedui.di

import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.BuildBillUseCase
import com.rentman.domain.billing.CreateBillUseCase
import com.rentman.domain.billing.ObserveBillContextUseCase
import com.rentman.domain.billing.SuggestMeterReadingUseCase
import com.rentman.domain.billing.ObserveBillTodosUseCase
import com.rentman.domain.billing.ObserveHomeUseCase
import com.rentman.domain.billing.ObserveMonthDetailUseCase
import com.rentman.domain.billing.ObserveMonthsUseCase
import com.rentman.domain.AppDispatchers
import com.rentman.domain.drive.BackUpToDriveUseCase
import com.rentman.domain.drive.ConnectDriveUseCase
import com.rentman.domain.drive.DisconnectDriveUseCase
import com.rentman.domain.drive.GetDriveStatusUseCase
import com.rentman.domain.drive.RestoreFromDriveUseCase
import com.rentman.domain.messages.MessageKind
import com.rentman.sharedui.feature.drive.DriveBackupViewModel
import com.rentman.domain.messages.ObserveMessageUseCase
import com.rentman.domain.messages.SaveMessageUseCase
import com.rentman.sharedui.feature.messages.MessageEditViewModel
import com.rentman.domain.backup.ExportBackupUseCase
import com.rentman.domain.backup.MarkBackedUpUseCase
import com.rentman.domain.backup.ObserveBackupStatusUseCase
import com.rentman.domain.backup.RestoreBackupUseCase
import com.rentman.domain.backup.UndoRestoreUseCase
import com.rentman.domain.payments.ObserveRenterLedgerUseCase
import com.rentman.domain.payments.RecordPaymentUseCase
import com.rentman.domain.billing.RemoveBillUseCase
import com.rentman.domain.billing.RestoreBillUseCase
import com.rentman.domain.payments.RemovePaymentUseCase
import com.rentman.domain.payments.RestorePaymentUseCase
import com.rentman.domain.reminders.ObserveReminderPlanUseCase
import com.rentman.domain.reminders.ReminderSync
import com.rentman.domain.settings.ObserveSettingsUseCase
import com.rentman.domain.settings.SaveSettingsUseCase
import com.rentman.domain.settings.SetReminderRepeatUseCase
import com.rentman.domain.settings.StepReminderTimeUseCase
import com.rentman.domain.tenancy.AddAddressUseCase
import com.rentman.domain.tenancy.ApplyRentIncreaseUseCase
import com.rentman.domain.tenancy.MarkRenterLeftUseCase
import com.rentman.domain.tenancy.MoveRenterBackUseCase
import com.rentman.domain.tenancy.MuteRemindersUseCase
import com.rentman.domain.tenancy.ObserveLeftRentersUseCase
import com.rentman.domain.tenancy.ObserveRenterFormUseCase
import com.rentman.domain.tenancy.UpdateRenterUseCase
import com.rentman.domain.tenancy.ObserveRentIncreaseOffersUseCase
import com.rentman.domain.tenancy.PostponeRentIncreaseUseCase
import com.rentman.domain.tenancy.UndoRentIncreaseUseCase
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.BuildFixedChargeUseCase
import com.rentman.domain.tenancy.ObserveAddressesUseCase
import com.rentman.domain.tenancy.ObserveRenterSummariesUseCase
import com.rentman.domain.tenancy.SaveRenterUseCase
import com.rentman.domain.time.GetTodayUseCase
import com.rentman.sharedui.feature.address.AddressPickerViewModel
import com.rentman.sharedui.feature.bill.CreateBillViewModel
import com.rentman.sharedui.feature.home.HomeViewModel
import com.rentman.sharedui.feature.months.MonthDetailViewModel
import com.rentman.sharedui.feature.months.MonthsViewModel
import com.rentman.sharedui.feature.payment.RecordPaymentViewModel
import com.rentman.sharedui.feature.renter.AddRenterViewModel
import com.rentman.sharedui.feature.renter.RenterDetailViewModel
import com.rentman.sharedui.feature.settings.SettingsViewModel
import com.rentman.sharedui.navigation.AddressSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Use cases. `:domain` stays free of Koin, so its wiring lives here. */
private val domainModule = module {
    factory { GetTodayUseCase(get(), get()) }
    factory { ObserveMessageUseCase(get()) }
    factory { GetDriveStatusUseCase(get(), get()) }
    factory { ConnectDriveUseCase(get()) }
    factory { BackUpToDriveUseCase(get(), get()) }
    factory { RestoreFromDriveUseCase(get(), get()) }
    factory { DisconnectDriveUseCase(get()) }
    factory { SaveMessageUseCase(get()) }
    factory { ObserveAddressesUseCase(get()) }
    factory { AddAddressUseCase(get()) }
    factory { ObserveRenterSummariesUseCase(get(), get()) }
    factory { SaveRenterUseCase(get(), get()) }
    factory { UpdateRenterUseCase(get()) }
    factory { ObserveRenterFormUseCase(get()) }
    factory { MarkRenterLeftUseCase(get(), get(), get(), get()) }
    factory { MoveRenterBackUseCase(get()) }
    factory { ObserveLeftRentersUseCase(get(), get()) }
    factory { BuildFixedChargeUseCase() }
    factory { BuildBillUseCase() }
    factory { SuggestMeterReadingUseCase() }
    factory { CreateBillUseCase(get()) }
    factory { ObserveBillContextUseCase(get(), get(), get(), get(), get(), get()) }
    factory { ObserveBillTodosUseCase(get(), get(), get(), get()) }
    factory { ExportBackupUseCase(get(), get(), get()) }
    factory { MarkBackedUpUseCase(get(), get()) }
    factory { RestoreBackupUseCase(get()) }
    factory { UndoRestoreUseCase(get()) }
    factory { ObserveBackupStatusUseCase(get(), get(), get()) }
    factory { MuteRemindersUseCase(get(), get()) }
    factory { ObserveReminderPlanUseCase(get(), get(), get(), get(), get(), get()) }
    factory { ObserveRentIncreaseOffersUseCase(get(), get(), get(), get(), get()) }
    factory { ApplyRentIncreaseUseCase(get(), get()) }
    factory { PostponeRentIncreaseUseCase(get(), get()) }
    factory { UndoRentIncreaseUseCase(get()) }
    factory { ObserveHomeUseCase(get(), get(), get(), get(), get(), get(), get()) }
    factory { ObserveMonthsUseCase(get(), get(), get(), get(), get()) }
    factory { ObserveMonthDetailUseCase(get(), get(), get(), get(), get()) }
    factory { ObserveSettingsUseCase(get()) }
    factory { SaveSettingsUseCase(get()) }
    factory { StepReminderTimeUseCase(get()) }
    factory { SetReminderRepeatUseCase(get()) }
    factory { RecordPaymentUseCase(get(), get()) }
    factory { RemovePaymentUseCase(get()) }
    factory { RestorePaymentUseCase(get()) }
    factory { RemoveBillUseCase(get(), get()) }
    factory { RestoreBillUseCase(get()) }
    factory { ObserveRenterLedgerUseCase(get(), get(), get(), get()) }
}

/** The one app-wide collector that keeps the phone's notifications in line with the data. Started by each platform. */
private val reminderModule = module {
    single { ReminderSync(get(), get(), CoroutineScope(SupervisorJob() + get<AppDispatchers>().io)) }
}

private val uiModule = module {
    single { AddressSelection() }
    viewModel { AddressPickerViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { MonthsViewModel(get()) }
    viewModel { DriveBackupViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { params -> MessageEditViewModel(MessageKind.valueOf(params.get<String>(0)), get(), get()) }
    viewModel { params -> MonthDetailViewModel(BillingMonth.fromValue(params.get<Int>(0)), get(), get()) }
    viewModel { params ->
        CreateBillViewModel(
            renterId = RenterId(params.get<Long>(0)),
            month = BillingMonth.fromValue(params.get<Int>(1)),
            observeContext = get(),
            observeMessage = get(),
            buildBill = get(),
            suggestReading = get(),
            createBill = get(),
        )
    }
    viewModel { params ->
        AddRenterViewModel(
            renterId = params.getOrNull<Long>()?.let(::RenterId),
            observeAddresses = get(),
            observeRenterForm = get(),
            saveRenter = get(),
            updateRenter = get(),
            getToday = get(),
            addressSelection = get(),
            buildFixedCharge = get(),
        )
    }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { params ->
        RenterDetailViewModel(
            renterId = RenterId(params.get<Long>(0)),
            observeLedger = get(),
            muteReminders = get(),
            markLeft = get(),
            moveBack = get(),
            removePayment = get(),
            restorePayment = get(),
            removeBill = get(),
            restoreBill = get(),
        )
    }
    viewModel { params ->
        RecordPaymentViewModel(
            renterId = RenterId(params.get<Long>(0)),
            observeLedger = get(),
            observeMessage = get(),
            recordPayment = get(),
            getToday = get(),
        )
    }
}

/** Everything except the platform-specific data module, which each platform entry adds. */
internal fun sharedModules(): List<Module> = listOf(domainModule, reminderModule, uiModule)
