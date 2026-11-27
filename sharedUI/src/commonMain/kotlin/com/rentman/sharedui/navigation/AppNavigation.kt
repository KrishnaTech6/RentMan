package com.rentman.sharedui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.tenancy.RenterId
import com.rentman.sharedui.feature.address.AddressPickerRoute
import com.rentman.sharedui.feature.bill.CreateBillRoute
import com.rentman.sharedui.feature.home.HomeRoute
import com.rentman.sharedui.feature.months.MonthDetailRoute
import com.rentman.sharedui.feature.months.MonthsRoute
import com.rentman.sharedui.feature.payment.RecordPaymentRoute
import com.rentman.sharedui.feature.renter.AddRenterRoute
import com.rentman.sharedui.feature.renter.RenterDetailRoute
import com.rentman.sharedui.feature.settings.SettingsRoute
import com.rentman.sharedui.feature.messages.MessageEditRoute
import com.rentman.domain.messages.MessageKind
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.koinInject

// iOS has no reflection to find the key serializers, so they are listed here.
private val navigationState = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomeKey::class)
            subclass(AddRenterKey::class)
            subclass(AddressPickerKey::class)
            subclass(CreateBillKey::class)
            subclass(RenterKey::class)
            subclass(RecordPaymentKey::class)
            subclass(SettingsKey::class)
            subclass(EditRenterKey::class)
            subclass(MonthsKey::class)
            subclass(MonthKey::class)
            subclass(MessageEditKey::class)
        }
    }
}

@Composable
internal fun AppNavigation() {
    val backStack = rememberNavBackStack(navigationState, HomeKey)
    val navigator = remember(backStack) { Navigator(backStack) }
    val addressSelection = koinInject<AddressSelection>()

    NavDisplay(
        backStack = backStack,
        onBack = navigator::back,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeKey> {
                HomeRoute(
                    onAddRenter = { navigator.open(AddRenterKey) },
                    onCreateBill = { renterId, month -> navigator.open(CreateBillKey(renterId.value, month.value)) },
                    onOpenRenter = { renterId -> navigator.open(RenterKey(renterId.value)) },
                    onOpenSettings = { navigator.open(SettingsKey) },
                    onOpenMonths = { navigator.open(MonthsKey) },
                )
            }
            entry<AddRenterKey> {
                AddRenterRoute(renterId = null, onPickAddress = { navigator.open(AddressPickerKey) }, onDone = navigator::back)
            }
            entry<EditRenterKey> { key ->
                AddRenterRoute(renterId = RenterId(key.renterId), onPickAddress = { navigator.open(AddressPickerKey) }, onDone = navigator::back)
            }
            entry<CreateBillKey> { key ->
                CreateBillRoute(
                    renterId = RenterId(key.renterId),
                    month = BillingMonth.fromValue(key.month),
                    onDone = navigator::back,
                )
            }
            entry<RenterKey> { key ->
                RenterDetailRoute(
                    renterId = RenterId(key.renterId),
                    onRecordPayment = { navigator.open(RecordPaymentKey(key.renterId)) },
                    onEdit = { navigator.open(EditRenterKey(key.renterId)) },
                    onBack = navigator::back,
                )
            }
            entry<RecordPaymentKey> { key ->
                RecordPaymentRoute(renterId = RenterId(key.renterId), onDone = navigator::back)
            }
            entry<SettingsKey> { SettingsRoute(onOpenMessage = { kind -> navigator.open(MessageEditKey(kind.name)) }, onDone = navigator::back) }
            entry<MessageEditKey> { key -> MessageEditRoute(kind = MessageKind.valueOf(key.kind), onDone = navigator::back) }
            entry<MonthsKey> { MonthsRoute(onOpenMonth = { month -> navigator.open(MonthKey(month.value)) }, onBack = navigator::back) }
            entry<MonthKey> { key ->
                MonthDetailRoute(
                    month = BillingMonth.fromValue(key.month),
                    onOpenRenter = { renterId -> navigator.open(RenterKey(renterId.value)) },
                    onBack = navigator::back,
                )
            }
            entry<AddressPickerKey> {
                AddressPickerRoute(onPicked = { id ->
                    addressSelection.pick(id)
                    navigator.back()
                })
            }
        },
    )
}
