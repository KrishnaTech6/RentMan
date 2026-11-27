# Graph Report - RentMan  (2026-11-27)

## Corpus Check
- 271 files · ~100,219 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 13 file(s) not represented in the graph (top: .xml 6, .properties 3, (none) 1)

## Summary
- 3419 nodes · 11154 edges · 176 communities (80 shown, 96 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 599 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `9487cba2`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- RoomRenterRepositoryTest
- Shared UI Components
- Screen Test Helpers
- AddRenterViewModelTest
- RenterLeavingUseCasesTest
- AddressPickerViewModelTest
- .useCase
- Outcome
- Create Bill Screen
- Rent Increase and Billing Month
- Renter Detail ViewModel
- Add Renter Screen
- RoomRentIncreaseRepositoryTest
- Home Screen
- .viewModel
- RecordPaymentUseCaseTest
- .encode
- MainViewController
- AddressId
- BuildBillUseCaseTest
- Paise
- RecordPaymentScreen.kt
- SettingsViewModel.kt
- .viewModel
- BackupDao
- SettingsScreen.kt
- RenterDetailScreen.kt
- AppSettings
- FakeSettingsRepository
- RentChangeEntity
- NotificationPermission.android.kt
- AddressPickerScreen.kt
- Modules.kt
- FakeRenterRepository
- Clock
- base
- MoneyInput
- CreateBillViewModel
- MeterPhotos.ios.kt
- .plan
- .plan
- HomeIntent
- RecordPaymentViewModelTest
- res
- MonthsScreen.kt
- FakeRenterRepository
- AddRenterViewModelTest.kt
- MeterPhotoFiles
- safeCall
- CropRect
- MeterPhotoFiles.kt
- IosReminderScheduler.kt
- SaveRenterUseCaseTest
- BackupFiles.ios.kt
- BillContext
- DomainError
- RenterId
- ReminderSyncTest
- BillLineEntity
- MeterReadingParserTest
- AddRenterViewModel
- AndroidReminderScheduler.kt
- RenterDetailViewModel
- MeterPhotos.kt
- RoomBillRepositoryTest
- FakeBillRepository
- ComposeView
- MviViewModel base class (toState, Eagerly)
- RecordPaymentViewModel.kt
- BillingMonth
- .renter
- bill table
- MonthDetailScreen.kt
- ReminderNotifications.kt
- RenterEntity
- ReminderText.kt
- MeterCropper.kt
- BackupCodec.kt
- PaymentEntity
- HomeViewModel
- ReminderNotificationDelegate.kt
- Pending / balance derivation (PendingCalculator)
- Reminders.kt
- sampleBackup
- renter table
- MonthSummary
- PhotoFilesTest.kt
- .of
- SettingsUseCases.kt
- Payment
- UpdateRenterUseCase
- BillingMonthTest
- RenterDetailIntent
- FakeBillRepository
- RenterField
- clockDigits
- ARCHITECTURE.md (engineering contract)
- .months
- NotificationPermission.kt
- safeFlow
- Address (glossary term)
- IosMeterPhotoFiles
- AddressEntity
- MeterTextTest
- MonthsViewModelTest
- RoomPaymentRepositoryTest
- Pending (glossary term)
- .form
- BackupFiles
- Bill line (glossary term)
- FormatRupeesTest
- .normalize
- RentManApplication.kt
- Reminder mute (remindersMutedMonth)
- Backup file JSON and restore transaction (BackupCodec)
- PaiseTest.kt
- Non-negotiable rules
- ReminderNotifications.kt
- GoldenBackups.kt
- ADR 0004 Settings in the database, not DataStore
- Meter reset (glossary term)
- AppSettingsEntity
- Build and test commands
- KMP structure with AGP 9
- Renter
- Own rate or global rate choice
- New address add field
- WhatsApp message preview
- Do next to-do cards
- Raise rent card
- Status pill
- Full amount or part payment
- Send receipt on WhatsApp
- Meter photo proof
- Reading calculation (1325 - 1240 = 85 units x rate)
- History ledger rows
- Settings design
- Backup and restore controls
- ADR 0005 Navigation as one-shot SharedFlow events
- .useCase
- MviViewModel
- .invoke
- MonthsViewModel
- AppNavigation.kt
- RenterMonthTest
- FakeAddressRepository
- TextPiece
- CurrencyFormat.ios.kt
- AddressPickerViewModel
- BillMapperTest
- Corner
- .raviOwesNehaPaid
- .context
- .rentFor
- IosKoin.kt
- MonthDetailViewModel
- RoomAddressRepositoryTest
- SuggestMeterReadingUseCaseTest
- MeterReadingParser
- AddAddressUseCaseTest
- StringResourcesTest.kt
- PaymentMapperTest
- loadReducers
- MeterPhotoSamples.kt

## God Nodes (most connected - your core abstractions)
1. `Outcome` - 248 edges
2. `RenterId` - 232 edges
3. `Paise` - 213 edges
4. `DomainError` - 148 edges
5. `BillingMonth` - 136 edges
6. `Bill` - 103 edges
7. `Payment` - 84 edges
8. `Renter` - 79 edges
9. `RenterSummary` - 70 edges
10. `FakeRenterRepository` - 68 edges

## Surprising Connections (you probably didn't know these)
- `Money history is immutable` --semantically_similar_to--> `Bill snapshot principle (no recompute from current settings)`  [INFERRED] [semantically similar]
  DATABASE.md → docs/adr/0002-bills-are-immutable-snapshots.md
- `toDomain()` --calls--> `AddressId`  [EXTRACTED]
  data/src/commonMain/kotlin/com/rentman/data/address/RoomAddressRepository.kt → domain/src/commonMain/kotlin/com/rentman/domain/tenancy/Address.kt
- `Error` --references--> `DomainError`  [EXTRACTED]
  sharedUI/src/commonMain/kotlin/com/rentman/sharedui/feature/address/AddressPickerContract.kt → domain/src/commonMain/kotlin/com/rentman/domain/Outcome.kt
- `Error` --references--> `DomainError`  [EXTRACTED]
  sharedUI/src/commonMain/kotlin/com/rentman/sharedui/feature/bill/CreateBillContract.kt → domain/src/commonMain/kotlin/com/rentman/domain/Outcome.kt
- `Error` --references--> `DomainError`  [EXTRACTED]
  sharedUI/src/commonMain/kotlin/com/rentman/sharedui/feature/home/HomeContract.kt → domain/src/commonMain/kotlin/com/rentman/domain/Outcome.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Bill immutability with the remove-and-remake exception** — docs_adr_0002_bills_are_immutable_snapshots, docs_adr_0009_a_wrong_bill_is_removed_and_made_again, database_bill_table, plan_remove_wrong_bill_feature, database_money_history_immutable [EXTRACTED 1.00]
- **MVI screen contract: UiState, NavEvent and MviViewModel** — architecture_mviviewmodel, architecture_uistate, architecture_navevent, architecture_dumb_ui [EXTRACTED 1.00]
- **Pending is derived from bill lines and payments, never stored** — database_pending_calculation, database_bill_line_table, database_payment_table, database_derived_not_stored, glossary_pending [EXTRACTED 1.00]

## Communities (176 total, 96 thin omitted)

### Community 1 - "Shared UI Components"
Cohesion: 0.08
Nodes (7): ActionRow(), MeterPhotoThumbnail(), MeterScanFlow(), Working(), ScreenTopBar(), SectionTitle(), UndoBar()

### Community 2 - "Screen Test Helpers"
Cohesion: 0.10
Nodes (15): toDomain(), Bill, BillLine, BillLineKind, Adjustment, Electricity, FixedCharge, Rent (+7 more)

### Community 3 - "AddRenterViewModelTest"
Cohesion: 0.10
Nodes (23): AddCharge, AddRenterIntent, AddRenterNavEvent, AskToRaiseChanged, Back, BillsElectricityChanged, ChargeAmountChanged, ChargeLabelChanged (+15 more)

### Community 5 - "AddressPickerViewModelTest"
Cohesion: 0.16
Nodes (7): Add, AddressPickerIntent, Confirm, NameChanged, Retry, Select, AddressPickerViewModelTest

### Community 7 - "Outcome"
Cohesion: 0.04
Nodes (7): toDomain(), Failure, Outcome, Success, FakeBackupRepository, FakeReminderScheduler, FakeBackupRepository

### Community 8 - "Create Bill Screen"
Cohesion: 0.06
Nodes (14): BillCard(), CreateBillRoute(), CreateBillScreen(), LineRow(), Math(), MeterPhotoRow(), MeterReplaced(), ProblemText() (+6 more)

### Community 9 - "Rent Increase and Billing Month"
Cohesion: 0.10
Nodes (12): toDomain(), RoomRentIncreaseRepository, RentChangeMapperTest, ApplyRentIncreaseUseCase, PostponeRentIncreaseUseCase, RentChange, RentChangeId, RentIncreaseRepository (+4 more)

### Community 10 - "Renter Detail ViewModel"
Cohesion: 0.20
Nodes (3): RemoveBill, RemovePayment, RenterDetailViewModelTest

### Community 11 - "Add Renter Screen"
Cohesion: 0.05
Nodes (10): Section(), RemoveCharge, AddRenterRoute(), AddRenterScreen(), AddressRow(), chargeProblem(), ChargeRow(), ChargesSection() (+2 more)

### Community 13 - "Home Screen"
Cohesion: 0.05
Nodes (19): AmountText(), formatRupees(), LegendRow(), MonthBar(), MonthCard(), amountStyle(), BillTodoCard(), Centered() (+11 more)

### Community 14 - ".viewModel"
Cohesion: 0.13
Nodes (14): Back, BackUpNow, BackupShared, BackupShareFailed, RateChanged, RepeatChanged, RestoreFrom, Retry (+6 more)

### Community 16 - ".encode"
Cohesion: 0.25
Nodes (3): RoomPaymentRepository, RoomBackupRepositoryTest, Clock

### Community 18 - "AddressId"
Cohesion: 0.11
Nodes (10): Address, AddressId, AddressPickerNavEvent, AddressPickerUiState, Content, Error, Loading, Picked (+2 more)

### Community 20 - "Paise"
Cohesion: 0.06
Nodes (22): BillCalculator, PendingCalculator, RenterMonth, Paise, overpayment(), PaymentMessage, BillEntry, LedgerEntry (+14 more)

### Community 21 - "RecordPaymentScreen.kt"
Cohesion: 0.08
Nodes (8): ActionButton(), TextAction(), DateStepper(), Form(), problemText(), RecordPaymentRoute(), RecordPaymentScreen(), WithProblemsPreview()

### Community 23 - ".viewModel"
Cohesion: 0.13
Nodes (13): Back, ChangeReading, ClearMeterPhoto, CreateBillIntent, MeterReplacedChanged, MeterScanned, NewMeterStartChanged, ReadingChanged (+5 more)

### Community 25 - "SettingsScreen.kt"
Cohesion: 0.06
Nodes (13): Divider(), SegmentButton(), Segmented(), StepButton(), TextRow(), MonthStepper(), BackedUpPreview(), Form() (+5 more)

### Community 26 - "RenterDetailScreen.kt"
Cohesion: 0.07
Nodes (13): BalanceText(), BalanceCard(), BillRow(), Body(), EntryAction, EntryCard(), entryKey(), LeftRow() (+5 more)

### Community 27 - "AppSettings"
Cohesion: 0.13
Nodes (5): RoomSettingsRepositoryTest, AppSettings, SetReminderRepeatUseCase, StepReminderTimeUseCase, ReminderSettingsUseCasesTest

### Community 28 - "FakeSettingsRepository"
Cohesion: 0.11
Nodes (11): BackupExport, BackupRepository, BackupStatus, ExportBackupUseCase, MarkBackedUpUseCase, ObserveBackupStatusUseCase, RestoreBackupUseCase, UndoRestoreUseCase (+3 more)

### Community 30 - "RentChangeEntity"
Cohesion: 0.20
Nodes (3): RentChangeDao, RentChangeEntity, toEntity()

### Community 31 - "NotificationPermission.android.kt"
Cohesion: 0.06
Nodes (9): readText(), rememberBackupFiles(), rememberPhotoImage(), findActivity(), notificationsAllowed(), openNotificationSettings(), rememberNotificationPermission(), rememberNotificationPermission() (+1 more)

### Community 32 - "AddressPickerScreen.kt"
Cohesion: 0.10
Nodes (8): AddressPickerRoute(), AddressPickerScreen(), AddressRow(), ContentBody(), ContentPreview(), NewAddress(), RadioDot(), NavigationEffect()

### Community 33 - "Modules.kt"
Cohesion: 0.20
Nodes (24): BillRepository, ObserveBillContextUseCase, ObserveBillTodosUseCase, ObserveHomeUseCase, MonthsInput, ObserveMonthDetailUseCase, observeMonthsInput(), ObserveMonthsUseCase (+16 more)

### Community 34 - "FakeRenterRepository"
Cohesion: 0.16
Nodes (7): CreateBill, NotNow, RaiseRent, UndoRaise, FakeBillRepository, HomeViewModelTest, FakeRenterRepository

### Community 36 - "base"
Cohesion: 0.06
Nodes (32): ENABLE_USER_SCRIPT_SANDBOXING, EXCLUDED_ARCHS[sdk=iphonesimulator*], FRAMEWORK_SEARCH_PATHS, OTHER_LDFLAGS, PRODUCT_BUNDLE_IDENTIFIER, SWIFT_VERSION, iOS, path (+24 more)

### Community 37 - "MoneyInput"
Cohesion: 0.19
Nodes (3): MoneyInput, toForm(), MoneyInputTest

### Community 38 - "CreateBillViewModel"
Cohesion: 0.27
Nodes (4): Close, CreateBillNavEvent, Saved, CreateBillViewModel

### Community 39 - "MeterPhotos.ios.kt"
Cohesion: 0.14
Nodes (6): PhotoItem, PhotoPickerDelegate, PreviewSource, rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource()

### Community 42 - "HomeIntent"
Cohesion: 0.24
Nodes (10): AddRenter, HomeIntent, HomeNavEvent, OpenAddRenter, OpenCreateBill, OpenMonths, OpenRenter, OpenSettings (+2 more)

### Community 43 - "RecordPaymentViewModelTest"
Cohesion: 0.15
Nodes (10): AmountChanged, Back, DayStepped, ModeChanged, NoteChanged, RecordPaymentIntent, Retry, Save (+2 more)

### Community 44 - "res"
Cohesion: 0.08
Nodes (4): message(), messageResource(), modeLabel(), timeLabel()

### Community 45 - "MonthsScreen.kt"
Cohesion: 0.17
Nodes (6): CenteredMessage(), ErrorMessage(), EmptyPreview(), MonthRowCard(), MonthsRoute(), MonthsScreen()

### Community 46 - "FakeRenterRepository"
Cohesion: 0.29
Nodes (3): ObserveHomeUseCaseTest, FakePaymentRepository, FakeRenterRepository

### Community 47 - "AddRenterViewModelTest.kt"
Cohesion: 0.16
Nodes (3): ObserveAddressesUseCase, BuildFixedChargeUseCase, ObserveRenterFormUseCase

### Community 48 - "MeterPhotoFiles"
Cohesion: 0.25
Nodes (6): MeterPhotoFiles, rememberPhotoFiles(), rememberPhotoOpener(), rememberPhotoSource(), readScale(), readWithin()

### Community 49 - "safeCall"
Cohesion: 0.09
Nodes (5): RoomBackupRepository, RoomBillRepository, safeCall(), GoldenBackupTest, BillId

### Community 51 - "MeterPhotoFiles.kt"
Cohesion: 0.08
Nodes (3): AppDispatchers, MeterPhotoProvider, ReminderActionReceiver

### Community 55 - "BillContext"
Cohesion: 0.11
Nodes (14): BillMessage, BillContext, BuildBillUseCase, BillMessageTest, BillStep, Reading, Review, Content (+6 more)

### Community 56 - "DomainError"
Cohesion: 0.05
Nodes (46): BillField, NewMeterStart, Reading, BackupFromNewerVersion, BillExists, BillNotRemovable, BlankName, DomainError (+38 more)

### Community 59 - "BillLineEntity"
Cohesion: 0.18
Nodes (5): toEntity(), BillDao, BillEntity, BillLineEntity, BillWithLines

### Community 62 - "AndroidReminderScheduler.kt"
Cohesion: 0.20
Nodes (4): androidDataModule(), ReminderScheduler, startRentManKoin(), AndroidReminderScheduler

### Community 64 - "MeterPhotos.kt"
Cohesion: 0.19
Nodes (11): PhotoFiles, PhotoOpener, PhotoSource, rememberPhotoFiles(), rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource(), rememberPhotoFiles() (+3 more)

### Community 66 - "FakeBillRepository"
Cohesion: 0.16
Nodes (6): CreateBillUseCase, RemoveBillUseCase, CreateBillUseCaseTest, FakeBillRepository, RemoveBillUseCaseTest, MuteRemindersUseCaseTest

### Community 67 - "ComposeView"
Cohesion: 0.14
Nodes (7): ComposeView, ContentView, .body, iOSApp, .body, sharedUI, SwiftUI

### Community 68 - "MviViewModel base class (toState, Eagerly)"
Cohesion: 0.14
Nodes (14): Data layer (:data, Room 3), Domain layer (:domain), formatRupees expect/actual (Indian grouping), Koin dependency injection, AGP 9 KMP module structure (androidApp, iosApp, sharedUI, data, domain), MviViewModel base class (toState, Eagerly), NavEvent one-shot SharedFlow, Navigation 3 (NavKey, NavDisplay) (+6 more)

### Community 70 - "BillingMonth"
Cohesion: 0.12
Nodes (5): BillingMonth, UnbilledMonth, remindersMutedFor(), RenterForm, RenterFormValidator

### Community 72 - "bill table"
Cohesion: 0.15
Nodes (10): Platform gateways and UI-only expect/actual (photo, backup files), bill table, Ledger (bills and payments merged), payment table (voidable), ADR 0002: Bills are immutable snapshots made of lines, ADR 0009: A wrong bill is removed and made again, Adjustment (glossary term), Electricity rate and meter reading rules (+2 more)

### Community 73 - "MonthDetailScreen.kt"
Cohesion: 0.10
Nodes (9): colors(), label(), StatusColors, StatusPill(), CashInRow(), ContentPreview(), MonthDetailRoute(), MonthDetailScreen() (+1 more)

### Community 74 - "ReminderNotifications.kt"
Cohesion: 0.18
Nodes (4): CreateBill, Reminder, RentPending, ReminderWorker

### Community 75 - "RenterEntity"
Cohesion: 0.13
Nodes (6): FixedChargeEntity, RenterWithCharges, InsertedRenter, RenterDao, RenterEntity, toEntity()

### Community 76 - "ReminderText.kt"
Cohesion: 0.16
Nodes (5): label(), shortLabel(), shortName(), ReminderText, text()

### Community 77 - "MeterCropper.kt"
Cohesion: 0.06
Nodes (6): MainActivity, App(), CropArea(), LightButton(), MeterCropper(), RentManColors

### Community 79 - "PaymentEntity"
Cohesion: 0.21
Nodes (3): PaymentDao, PaymentEntity, toEntity()

### Community 82 - "Pending / balance derivation (PendingCalculator)"
Cohesion: 0.15
Nodes (11): Paise value class (money as Long), bill_line table, Pending / balance derivation (PendingCalculator), Renter status derivation (No bill, Paid, Late, Part paid, Unpaid), Due day (glossary term), Late (glossary term), Not due yet (glossary term), Part paid and Unpaid statuses (glossary terms) (+3 more)

### Community 83 - "Reminders.kt"
Cohesion: 0.09
Nodes (5): flatMap(), map(), ReminderSync, AddAddressUseCase, RenterUnits

### Community 85 - "renter table"
Cohesion: 0.17
Nodes (12): address table, fixed_charge table, rent_change table, renter table, Unbilled months derivation, ADR 0008 The landlord creates every bill, Remade bill uses rent of its own month (RentHistory.rentFor), Rent increase (glossary term) (+4 more)

### Community 86 - "MonthSummary"
Cohesion: 0.07
Nodes (36): BillTodo, HomeSnapshot, Lists, RenterRow, RenterStatus, Late, NoBillYet, Paid (+28 more)

### Community 89 - "SettingsUseCases.kt"
Cohesion: 0.13
Nodes (12): ObserveSettingsUseCase, SaveSettingsUseCase, SettingsField, Rate, Repeat, SettingsForm, SaveSettingsUseCaseTest, BackupMessage (+4 more)

### Community 90 - "Payment"
Cohesion: 0.14
Nodes (4): toDomain(), Payment, PaymentId, FakePaymentRepository

### Community 91 - "UpdateRenterUseCase"
Cohesion: 0.19
Nodes (3): UpdateRenterUseCase, BuildFixedChargeUseCaseTest, UpdateRenterUseCaseTest

### Community 92 - "BillingMonthTest"
Cohesion: 0.15
Nodes (3): MeterPhotoName, BillingMonthTest, MeterPhotoNameTest

### Community 93 - "RenterDetailIntent"
Cohesion: 0.10
Nodes (20): Back, Close, Content, Edit, Error, Loading, MarkLeft, MoveBack (+12 more)

### Community 95 - "RenterField"
Cohesion: 0.18
Nodes (11): RenterField, Address, Charge, DueDay, Name, OwnRate, Phone, Rent (+3 more)

### Community 96 - "clockDigits"
Cohesion: 0.39
Nodes (3): clockDigits(), isAfternoon(), TimeTextTest

### Community 97 - "ARCHITECTURE.md (engineering contract)"
Cohesion: 0.36
Nodes (7): ARCHITECTURE.md (engineering contract), Workflow for every change (ponytail, review, graphify), DATABASE.md (schema v1), Home design, GLOSSARY.md (ubiquitous language), PLAN.md (product plan), Claude Design mockup (design/project)

### Community 99 - "NotificationPermission.kt"
Cohesion: 0.36
Nodes (4): AskForNotificationsOnce(), NotificationPermission, rememberNotificationPermission(), rememberNotificationPermission()

### Community 100 - "safeFlow"
Cohesion: 0.16
Nodes (5): safeFlow(), RoomSettingsRepository, toDomain(), toEntity(), SafeTest

### Community 101 - "Address (glossary term)"
Cohesion: 0.29
Nodes (8): Add renter design, Address picker design, Address filter chips, Address (glossary term), Left renter (glossary term), Renter (glossary term), Unit (glossary term), Renter edit and mark left feature

### Community 107 - "Pending (glossary term)"
Cohesion: 0.29
Nodes (7): Record payment design, Renter ledger design, ADR 0001 Pending is computed, never stored, Paid ahead (glossary term), Payment (glossary term), Pending (glossary term), WhatsApp bill message (wa.me link)

### Community 109 - "BackupFiles"
Cohesion: 0.43
Nodes (3): BackupFiles, rememberBackupFiles(), rememberBackupFiles()

### Community 111 - "Bill line (glossary term)"
Cohesion: 0.40
Nodes (6): Fixed monthly charges list, Bill preview design, Meter reading design, Bill (glossary term), Bill line (glossary term), Fixed charge (glossary term)

### Community 115 - "Reminder mute (remindersMutedMonth)"
Cohesion: 0.50
Nodes (5): ReminderSync and ReminderScheduler (resync, not timer), Reminder mute (remindersMutedMonth), Stop this month button, Reminder mute (glossary term), Reminders feature (ReminderPlanner, WorkManager, UNUserNotificationCenter)

### Community 116 - "Backup file JSON and restore transaction (BackupCodec)"
Cohesion: 0.40
Nodes (5): app_settings table (single row), Backup file JSON and restore transaction (BackupCodec), backup_snapshot table, Schema changes: version 1 frozen from first real install, Backup and restore feature (share sheet, Undo last restore)

### Community 130 - "Renter"
Cohesion: 0.10
Nodes (11): RoomAddressRepository, dataModule(), AppDispatchers, AppDatabase, AppDatabaseConstructor, toDomain(), RoomRenterRepository, RenterMapperTest (+3 more)

### Community 148 - "MonthsViewModel"
Cohesion: 0.21
Nodes (8): Back, Close, MonthsIntent, MonthsNavEvent, OpenMonth, Refresh, Retry, MonthsViewModel

### Community 149 - "AppNavigation.kt"
Cohesion: 0.09
Nodes (13): AppNavigation(), Navigator, AddRenterKey, AddressPickerKey, CreateBillKey, EditRenterKey, HomeKey, MonthKey (+5 more)

### Community 152 - "TextPiece"
Cohesion: 0.47
Nodes (10): around(), digitRows(), digitsOf(), findMeters(), fit(), MeterSpot, readingLines(), runsOf() (+2 more)

### Community 157 - "Corner"
Cohesion: 0.13
Nodes (9): Corner, BottomLeft, BottomRight, TopLeft, TopRight, PixelBox, Edge, Grab (+1 more)

### Community 161 - "IosKoin.kt"
Cohesion: 0.25
Nodes (3): iosDataModule(), sharedModules(), startRentManKoin()

## Knowledge Gaps
- **129 isolated node(s):** `UnitTaken`, `InvalidBackup`, `OfferOutdated`, `NotFound`, `ReadingBelowPrevious` (+124 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 661 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **96 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `RenterId` connect `RenterId` to `RoomRenterRepositoryTest`, `Screen Test Helpers`, `Renter`, `RenterLeavingUseCasesTest`, `AddRenterViewModelTest`, `.useCase`, `Outcome`, `Create Bill Screen`, `Rent Increase and Billing Month`, `Renter Detail ViewModel`, `Add Renter Screen`, `RoomRentIncreaseRepositoryTest`, `Home Screen`, `RecordPaymentUseCaseTest`, `.encode`, `.useCase`, `AddressId`, `BuildBillUseCaseTest`, `Paise`, `RecordPaymentScreen.kt`, `RenterMonthTest`, `FakeAddressRepository`, `SettingsViewModel.kt`, `AppNavigation.kt`, `RenterDetailScreen.kt`, `.viewModel`, `BillMapperTest`, `.context`, `.rentFor`, `Modules.kt`, `FakeRenterRepository`, `CreateBillViewModel`, `.plan`, `PaymentMapperTest`, `HomeIntent`, `RecordPaymentViewModelTest`, `FakeRenterRepository`, `AddRenterViewModelTest.kt`, `safeCall`, `MeterPhotoFiles.kt`, `SaveRenterUseCaseTest`, `DomainError`, `ReminderSyncTest`, `AddRenterViewModel`, `RenterDetailViewModel`, `RoomBillRepositoryTest`, `FakeBillRepository`, `RecordPaymentViewModel.kt`, `BillingMonth`, `.renter`, `MonthDetailScreen.kt`, `ReminderNotifications.kt`, `ReminderNotificationDelegate.kt`, `Reminders.kt`, `MonthSummary`, `Payment`, `UpdateRenterUseCase`, `FakeBillRepository`, `.months`, `MonthsViewModelTest`, `RoomPaymentRepositoryTest`, `.form`?**
  _High betweenness centrality (0.151) - this node is a cross-community bridge._
- **Are the 21 inferred relationships involving `RenterId` (e.g. with `.aRenterWithNoBillHasNothingToMuteAndNothingChanges()` and `.aStorageFailureIsReported()`) actually correct?**
  _`RenterId` has 21 INFERRED edges - model-reasoned connections that need verification._
- **What connects `UnitTaken`, `InvalidBackup`, `OfferOutdated` to the rest of the system?**
  _129 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Shared UI Components` be split into smaller, more focused modules?**
  _Cohesion score 0.07864488808227466 - nodes in this community are weakly interconnected._
- **Why does `Paise` connect `Paise` to `RoomRenterRepositoryTest`, `Shared UI Components`, `Screen Test Helpers`, `Renter`, `RenterLeavingUseCasesTest`, `AddRenterViewModelTest`, `Create Bill Screen`, `Rent Increase and Billing Month`, `Add Renter Screen`, `Home Screen`, `.viewModel`, `RecordPaymentUseCaseTest`, `.encode`, `.useCase`, `AddressId`, `.invoke`, `BuildBillUseCaseTest`, `RecordPaymentScreen.kt`, `SettingsViewModel.kt`, `.viewModel`, `RenterDetailScreen.kt`, `AppSettings`, `BillMapperTest`, `CurrencyFormat.ios.kt`, `.rentFor`, `Modules.kt`, `MoneyInput`, `.plan`, `PaymentMapperTest`, `RecordPaymentViewModelTest`, `MonthsScreen.kt`, `FakeRenterRepository`, `AddRenterViewModelTest.kt`, `SaveRenterUseCaseTest`, `BillContext`, `DomainError`, `RenterId`, `RoomBillRepositoryTest`, `BillingMonth`, `.renter`, `MonthDetailScreen.kt`, `ReminderNotifications.kt`, `Reminders.kt`, `MonthSummary`, `SettingsUseCases.kt`, `Payment`, `UpdateRenterUseCase`, `safeFlow`, `.form`, `FormatRupeesTest`, `PaiseTest.kt`?**
  _High betweenness centrality (0.141) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Paise` (e.g. with `.aRateIsAtMostNinetyNineRupeesSoAMissingDecimalPointIsCaught()` and `.parsesAPaymentWithUpToTwoDecimals()`) actually correct?**
  _`Paise` has 6 INFERRED edges - model-reasoned connections that need verification._
- **Should `Screen Test Helpers` be split into smaller, more focused modules?**
  _Cohesion score 0.10493827160493827 - nodes in this community are weakly interconnected._