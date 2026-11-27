# Graph Report - RentMan  (2026-11-27)

## Corpus Check
- 267 files · ~95,063 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 13 file(s) not represented in the graph (top: .xml 6, .properties 3, (none) 1)

## Summary
- 3361 nodes · 10982 edges · 169 communities (80 shown, 89 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 570 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1c872906`
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
- RoomAddressRepository
- Home Screen
- .viewModel
- RecordPaymentUseCaseTest
- .encode
- MainActivity.kt
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
- IosMeterPhotoFiles.kt
- RentChangeEntity
- NotificationPermission.android.kt
- AddressPickerScreen.kt
- Modules.kt
- FakeRenterRepository
- RenterDetailViewModelTest.kt
- base
- MoneyInput
- CreateBillViewModel
- MeterPhotos.ios.kt
- .plan
- .plan
- HomeIntent
- RecordPaymentViewModelTest
- res
- HomeViewModelTest.kt
- FakeRenterRepository
- AddRenterViewModelTest.kt
- MeterPhotoFiles
- .summary
- CropRect
- MeterPhotoFiles.kt
- IosReminderScheduler.kt
- SaveRenterUseCaseTest
- BackupFiles.ios.kt
- Bill
- DomainError
- RenterId
- ReminderSyncTest
- BillLineEntity
- MeterReadingParserTest
- AddRenterViewModel
- ReminderSync
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
- ReminderActionReceiver.kt
- sampleBackup
- rent_change table
- MonthDetail
- PhotoFilesTest.kt
- .of
- SaveSettingsUseCase
- SettingsIntent
- UpdateRenterUseCase
- BillingMonthTest
- MeterPhotoName
- FakeBillRepository
- RenterField
- clockDigits
- ARCHITECTURE.md (engineering contract)
- .months
- NotificationPermission.kt
- Test
- Address (glossary term)
- .pending
- BuildFixedChargeUseCaseTest
- CurrencyFormat.android.kt
- MonthsViewModelTest
- SafeTest.kt
- Pending (glossary term)
- .form
- BackupFiles
- flatMapFirst
- Bill line (glossary term)
- FormatRupeesTest
- ViewControllers.ios.kt
- RentManApplication.kt
- Reminder mute (remindersMutedMonth)
- Backup file JSON and restore transaction (BackupCodec)
- PaiseTest.kt
- Non-negotiable rules
- SettingsNavEvent
- GoldenBackups.kt
- ADR 0004 Settings in the database, not DataStore
- Meter reset (glossary term)
- AppSettingsEntity
- Build and test commands
- KMP structure with AGP 9
- FixedCharge
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
- AppNavigation
- RenterMonthTest
- FakeAddressRepository
- AppNavigation.kt
- Navigator
- AddressPickerViewModel
- StepReminderTimeUseCase
- Corner
- .raviOwesNehaPaid
- RecordPaymentRoute
- .aStorageFailureIsReported
- FakeRentIncreaseRepository
- MonthDetailViewModel
- AppDatabase.kt
- FakeAddressRepository
- CropRect.kt
- MVI unidirectional pattern

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

## Communities (169 total, 89 thin omitted)

### Community 1 - "Shared UI Components"
Cohesion: 0.08
Nodes (6): ActionButton(), TextAction(), ActionRow(), MeterPhotoThumbnail(), SectionTitle(), RentManColors

### Community 2 - "Screen Test Helpers"
Cohesion: 0.12
Nodes (8): AppDatabase, BillLine, BillLineKind, Adjustment, Electricity, FixedCharge, Rent, TestData

### Community 3 - "AddRenterViewModelTest"
Cohesion: 0.13
Nodes (18): AddCharge, AddRenterIntent, AskToRaiseChanged, Back, BillsElectricityChanged, ChargeAmountChanged, ChargeLabelChanged, DueDayChanged (+10 more)

### Community 5 - "AddressPickerViewModelTest"
Cohesion: 0.16
Nodes (7): Add, AddressPickerIntent, Confirm, NameChanged, Retry, Select, AddressPickerViewModelTest

### Community 7 - "Outcome"
Cohesion: 0.04
Nodes (16): RoomBackupRepository, dataModule(), AppDispatchers, RoomRenterRepository, safeCall(), safeFlow(), RoomSettingsRepository, toDomain() (+8 more)

### Community 8 - "Create Bill Screen"
Cohesion: 0.08
Nodes (12): BillCard(), CreateBillScreen(), LineRow(), Math(), MeterPhotoRow(), MeterReplaced(), ProblemText(), ReadingStep() (+4 more)

### Community 9 - "Rent Increase and Billing Month"
Cohesion: 0.11
Nodes (9): toDomain(), RoomRentIncreaseRepository, RentChangeMapperTest, flatMap(), ApplyRentIncreaseUseCase, RentChange, RentChangeId, FakeRentIncreaseRepository (+1 more)

### Community 10 - "Renter Detail ViewModel"
Cohesion: 0.13
Nodes (13): Back, Edit, MarkLeft, MoveBack, RecordPayment, RemoveBill, RemovePayment, RenterDetailIntent (+5 more)

### Community 11 - "Add Renter Screen"
Cohesion: 0.05
Nodes (15): Divider(), SegmentButton(), Segmented(), StepButton(), TextRow(), AddRenterScreen(), AddressRow(), chargeProblem() (+7 more)

### Community 12 - "RoomAddressRepository"
Cohesion: 0.07
Nodes (7): RoomAddressRepository, toDomain(), AddressDao, AddressEntity, RoomAddressRepositoryTest, RoomPaymentRepositoryTest, RoomRentIncreaseRepositoryTest

### Community 13 - "Home Screen"
Cohesion: 0.06
Nodes (26): RenterStatus, Late, NoBillYet, Paid, PartPaid, Unpaid, formatRupees(), colors() (+18 more)

### Community 14 - ".viewModel"
Cohesion: 0.21
Nodes (6): RateChanged, RepeatChanged, RestoreFrom, TimeStepped, FakeSettingsRepository, SettingsViewModelTest

### Community 16 - ".encode"
Cohesion: 0.25
Nodes (3): RoomPaymentRepository, RoomBackupRepositoryTest, Clock

### Community 17 - "MainActivity.kt"
Cohesion: 0.15
Nodes (3): MainActivity, App(), MainViewController()

### Community 18 - "AddressId"
Cohesion: 0.13
Nodes (11): Address, AddressId, AddAddressUseCaseTest, AddressPickerNavEvent, AddressPickerUiState, Content, Error, Loading (+3 more)

### Community 19 - "BuildBillUseCaseTest"
Cohesion: 0.16
Nodes (3): BuildBillUseCase, BillMessageTest, BuildBillUseCaseTest

### Community 20 - "Paise"
Cohesion: 0.07
Nodes (26): BillCalculator, BillTodo, HomeSnapshot, Lists, RenterRow, MonthSummary, RenterMonth, Paise (+18 more)

### Community 21 - "RecordPaymentScreen.kt"
Cohesion: 0.11
Nodes (6): Save, DateStepper(), Form(), problemText(), RecordPaymentScreen(), WithProblemsPreview()

### Community 22 - "SettingsViewModel.kt"
Cohesion: 0.11
Nodes (3): ObserveSettingsUseCase, inputReducers(), loadReducers()

### Community 23 - ".viewModel"
Cohesion: 0.08
Nodes (16): Candidate, MeterReadingParser, SuggestMeterReadingUseCase, Back, ChangeReading, ClearMeterPhoto, CreateBillIntent, MeterReplacedChanged (+8 more)

### Community 25 - "SettingsScreen.kt"
Cohesion: 0.07
Nodes (8): Section(), BackedUpPreview(), Form(), rateProblem(), repeatLabel(), SettingsRoute(), SettingsScreen(), TimeStepper()

### Community 26 - "RenterDetailScreen.kt"
Cohesion: 0.08
Nodes (13): BalanceText(), BalanceCard(), BillRow(), Body(), EntryAction, EntryCard(), entryKey(), LeftRow() (+5 more)

### Community 28 - "FakeSettingsRepository"
Cohesion: 0.11
Nodes (10): BackupExport, BackupRepository, BackupStatus, ExportBackupUseCase, MarkBackedUpUseCase, ObserveBackupStatusUseCase, RestoreBackupUseCase, UndoRestoreUseCase (+2 more)

### Community 30 - "RentChangeEntity"
Cohesion: 0.20
Nodes (3): RentChangeDao, RentChangeEntity, toEntity()

### Community 31 - "NotificationPermission.android.kt"
Cohesion: 0.07
Nodes (8): readText(), rememberBackupFiles(), rememberPhotoImage(), findActivity(), notificationsAllowed(), openNotificationSettings(), rememberNotificationPermission(), rememberNotificationPermission()

### Community 32 - "AddressPickerScreen.kt"
Cohesion: 0.15
Nodes (9): CenteredMessage(), ErrorMessage(), AddressPickerRoute(), AddressPickerScreen(), AddressRow(), ContentBody(), ContentPreview(), NewAddress() (+1 more)

### Community 33 - "Modules.kt"
Cohesion: 0.12
Nodes (23): AppDispatchers, BillRepository, ObserveBillContextUseCase, ObserveBillTodosUseCase, ObserveHomeUseCase, MonthsInput, ObserveMonthDetailUseCase, observeMonthsInput() (+15 more)

### Community 34 - "FakeRenterRepository"
Cohesion: 0.20
Nodes (3): RaiseRent, HomeViewModelTest, FakeRenterRepository

### Community 35 - "RenterDetailViewModelTest.kt"
Cohesion: 0.13
Nodes (8): RemoveBillUseCase, RestoreBillUseCase, fold(), RemovePaymentUseCase, RestorePaymentUseCase, MuteRemindersUseCase, MarkRenterLeftUseCase, Clock

### Community 36 - "base"
Cohesion: 0.06
Nodes (32): ENABLE_USER_SCRIPT_SANDBOXING, EXCLUDED_ARCHS[sdk=iphonesimulator*], FRAMEWORK_SEARCH_PATHS, OTHER_LDFLAGS, PRODUCT_BUNDLE_IDENTIFIER, SWIFT_VERSION, iOS, path (+24 more)

### Community 37 - "MoneyInput"
Cohesion: 0.12
Nodes (4): MoneyInput, PhoneNumber, MoneyInputTest, PhoneNumberTest

### Community 38 - "CreateBillViewModel"
Cohesion: 0.27
Nodes (4): Close, CreateBillNavEvent, Saved, CreateBillViewModel

### Community 39 - "MeterPhotos.ios.kt"
Cohesion: 0.14
Nodes (6): PhotoItem, PhotoPickerDelegate, PreviewSource, rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource()

### Community 41 - ".plan"
Cohesion: 0.13
Nodes (3): ReminderPlan, ReminderPlanner, FakeReminderScheduler

### Community 42 - "HomeIntent"
Cohesion: 0.18
Nodes (13): AddRenter, CreateBill, HomeIntent, HomeNavEvent, NotNow, OpenAddRenter, OpenCreateBill, OpenMonths (+5 more)

### Community 43 - "RecordPaymentViewModelTest"
Cohesion: 0.15
Nodes (9): AmountChanged, Back, DayStepped, ModeChanged, NoteChanged, RecordPaymentIntent, Retry, UseFullAmount (+1 more)

### Community 44 - "res"
Cohesion: 0.08
Nodes (5): message(), messageResource(), modeLabel(), isAfternoon(), timeLabel()

### Community 46 - "FakeRenterRepository"
Cohesion: 0.25
Nodes (3): ObserveHomeUseCaseTest, FakePaymentRepository, FakeRenterRepository

### Community 47 - "AddRenterViewModelTest.kt"
Cohesion: 0.13
Nodes (3): ObserveAddressesUseCase, BuildFixedChargeUseCase, ObserveRenterFormUseCase

### Community 48 - "MeterPhotoFiles"
Cohesion: 0.29
Nodes (4): MeterPhotoFiles, rememberPhotoFiles(), rememberPhotoOpener(), rememberPhotoSource()

### Community 55 - "Bill"
Cohesion: 0.06
Nodes (17): toDomain(), RoomBillRepository, Bill, BillId, BillMessage, BillContext, isNewestOf(), BillStep (+9 more)

### Community 56 - "DomainError"
Cohesion: 0.05
Nodes (50): BillField, NewMeterStart, Reading, BackupFromNewerVersion, BillExists, BillNotRemovable, BlankName, DomainError (+42 more)

### Community 57 - "RenterId"
Cohesion: 0.09
Nodes (11): toDomain(), PaymentMapperTest, Payment, PaymentForm, PaymentId, PaymentMode, Bank, Cash (+3 more)

### Community 59 - "BillLineEntity"
Cohesion: 0.12
Nodes (6): toEntity(), BillDao, BillEntity, BillLineEntity, BillWithLines, BillMapperTest

### Community 62 - "ReminderSync"
Cohesion: 0.15
Nodes (8): androidDataModule(), iosDataModule(), ReminderScheduler, ReminderSync, startRentManKoin(), AndroidReminderScheduler, sharedModules(), startRentManKoin()

### Community 63 - "RenterDetailViewModel"
Cohesion: 0.28
Nodes (5): Close, OpenEdit, OpenRecordPayment, RenterDetailNavEvent, RenterDetailViewModel

### Community 64 - "MeterPhotos.kt"
Cohesion: 0.16
Nodes (14): PhotoFiles, PhotoOpener, PhotoSource, readWithin(), rememberPhotoFiles(), rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource() (+6 more)

### Community 66 - "FakeBillRepository"
Cohesion: 0.25
Nodes (4): CreateBillUseCase, CreateBillUseCaseTest, FakeBillRepository, RemoveBillUseCaseTest

### Community 67 - "ComposeView"
Cohesion: 0.14
Nodes (7): ComposeView, ContentView, .body, iOSApp, .body, sharedUI, SwiftUI

### Community 68 - "MviViewModel base class (toState, Eagerly)"
Cohesion: 0.22
Nodes (11): Data layer (:data, Room 3), Domain layer (:domain), Koin dependency injection, AGP 9 KMP module structure (androidApp, iosApp, sharedUI, data, domain), MviViewModel base class (toState, Eagerly), Navigation 3 (NavKey, NavDisplay), Outcome<T> errors as values, safeFlow / safeCall single catch (+3 more)

### Community 70 - "BillingMonth"
Cohesion: 0.08
Nodes (6): BillingMonth, UnbilledMonths, UnbilledMonth, remindersMutedFor(), RentHistory, RentHistoryTest

### Community 72 - "bill table"
Cohesion: 0.13
Nodes (13): Platform gateways and UI-only expect/actual (photo, backup files), address table, bill table, fixed_charge table, Ledger (bills and payments merged), payment table (voidable), renter table, ADR 0002: Bills are immutable snapshots made of lines (+5 more)

### Community 73 - "MonthDetailScreen.kt"
Cohesion: 0.07
Nodes (15): AmountText(), LegendRow(), MonthBar(), MonthCard(), ScreenTopBar(), CashInRow(), ContentPreview(), MonthDetailRoute() (+7 more)

### Community 74 - "ReminderNotifications.kt"
Cohesion: 0.10
Nodes (5): CreateBill, Reminder, RentPending, ReminderNotifications, ReminderWorker

### Community 75 - "RenterEntity"
Cohesion: 0.13
Nodes (6): FixedChargeEntity, RenterWithCharges, InsertedRenter, RenterDao, RenterEntity, toEntity()

### Community 76 - "ReminderText.kt"
Cohesion: 0.16
Nodes (5): label(), shortLabel(), shortName(), ReminderText, text()

### Community 77 - "MeterCropper.kt"
Cohesion: 0.11
Nodes (3): CropArea(), LightButton(), MeterCropper()

### Community 78 - "BackupCodec.kt"
Cohesion: 0.17
Nodes (3): BackupCodec, BackupFile, GoldenBackupTest

### Community 79 - "PaymentEntity"
Cohesion: 0.23
Nodes (3): PaymentDao, PaymentEntity, toEntity()

### Community 82 - "Pending / balance derivation (PendingCalculator)"
Cohesion: 0.15
Nodes (11): Paise value class (money as Long), bill_line table, Pending / balance derivation (PendingCalculator), Renter status derivation (No bill, Paid, Late, Part paid, Unpaid), Due day (glossary term), Late (glossary term), Not due yet (glossary term), Part paid and Unpaid statuses (glossary terms) (+3 more)

### Community 85 - "rent_change table"
Cohesion: 0.50
Nodes (4): rent_change table, Remade bill uses rent of its own month (RentHistory.rentFor), Rent increase (glossary term), Rent increase feature (Raise rent card, Undo)

### Community 86 - "MonthDetail"
Cohesion: 0.14
Nodes (15): MonthReviewMessage, MonthDetail, MonthReviewMessageTest, Back, Close, Content, Error, Loading (+7 more)

### Community 90 - "SettingsIntent"
Cohesion: 0.11
Nodes (17): SettingsForm, Back, BackupMessage, Restored, RestoreUndone, ShareFailed, BackUpNow, BackupShared (+9 more)

### Community 95 - "RenterField"
Cohesion: 0.08
Nodes (23): ChargeField, Amount, Label, RenterField, Address, Charge, DueDay, Name (+15 more)

### Community 97 - "ARCHITECTURE.md (engineering contract)"
Cohesion: 0.36
Nodes (7): ARCHITECTURE.md (engineering contract), Workflow for every change (ponytail, review, graphify), DATABASE.md (schema v1), Home design, GLOSSARY.md (ubiquitous language), PLAN.md (product plan), Claude Design mockup (design/project)

### Community 99 - "NotificationPermission.kt"
Cohesion: 0.36
Nodes (4): AskForNotificationsOnce(), NotificationPermission, rememberNotificationPermission(), rememberNotificationPermission()

### Community 101 - "Address (glossary term)"
Cohesion: 0.29
Nodes (8): Add renter design, Address picker design, Address filter chips, Address (glossary term), Left renter (glossary term), Renter (glossary term), Unit (glossary term), Renter edit and mark left feature

### Community 107 - "Pending (glossary term)"
Cohesion: 0.17
Nodes (12): Unbilled months derivation, Record payment design, Renter ledger design, ADR 0001 Pending is computed, never stored, ADR 0008 The landlord creates every bill, Paid ahead (glossary term), Payment (glossary term), Pending (glossary term) (+4 more)

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

### Community 121 - "SettingsNavEvent"
Cohesion: 0.67
Nodes (3): Close, SettingsNavEvent, ShareBackup

### Community 130 - "FixedCharge"
Cohesion: 0.29
Nodes (4): toDomain(), RenterMapperTest, FixedCharge, FixedChargeId

### Community 148 - "MonthsViewModel"
Cohesion: 0.23
Nodes (8): Back, Close, MonthsIntent, MonthsNavEvent, OpenMonth, Refresh, Retry, MonthsViewModel

### Community 149 - "AppNavigation"
Cohesion: 0.41
Nodes (11): AppNavigation(), AddRenterKey, AddressPickerKey, CreateBillKey, EditRenterKey, HomeKey, MonthKey, MonthsKey (+3 more)

### Community 156 - "StepReminderTimeUseCase"
Cohesion: 0.31
Nodes (3): SetReminderRepeatUseCase, StepReminderTimeUseCase, ReminderSettingsUseCasesTest

### Community 157 - "Corner"
Cohesion: 0.22
Nodes (8): Corner, BottomLeft, BottomRight, TopLeft, TopRight, Edge, Grab, Whole

### Community 159 - "RecordPaymentRoute"
Cohesion: 0.39
Nodes (3): PaymentMessage, PaymentMessageTest, RecordPaymentRoute()

### Community 166 - "MVI unidirectional pattern"
Cohesion: 0.40
Nodes (3): formatRupees expect/actual (Indian grouping), NavEvent one-shot SharedFlow, UiState sealed interface (Loading, Error, Content)

## Knowledge Gaps
- **129 isolated node(s):** `UnitTaken`, `InvalidBackup`, `OfferOutdated`, `NotFound`, `ReadingBelowPrevious` (+124 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 652 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **89 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `RenterId` connect `RenterId` to `RoomRenterRepositoryTest`, `Screen Test Helpers`, `FixedCharge`, `RenterLeavingUseCasesTest`, `AddRenterViewModelTest`, `.useCase`, `Outcome`, `Create Bill Screen`, `Rent Increase and Billing Month`, `Renter Detail ViewModel`, `Add Renter Screen`, `RoomAddressRepository`, `Home Screen`, `RecordPaymentUseCaseTest`, `.encode`, `.useCase`, `BuildBillUseCaseTest`, `Paise`, `RecordPaymentScreen.kt`, `RenterMonthTest`, `FakeAddressRepository`, `AppNavigation.kt`, `AppNavigation`, `RenterDetailScreen.kt`, `.viewModel`, `RecordPaymentRoute`, `.aStorageFailureIsReported`, `Modules.kt`, `FakeRenterRepository`, `RenterDetailViewModelTest.kt`, `CreateBillViewModel`, `.plan`, `HomeIntent`, `RecordPaymentViewModelTest`, `HomeViewModelTest.kt`, `FakeRenterRepository`, `AddRenterViewModelTest.kt`, `.summary`, `SaveRenterUseCaseTest`, `Bill`, `ReminderSyncTest`, `BillLineEntity`, `AddRenterViewModel`, `RenterDetailViewModel`, `MeterPhotos.kt`, `RoomBillRepositoryTest`, `FakeBillRepository`, `RecordPaymentViewModel.kt`, `BillingMonth`, `.renter`, `MonthDetailScreen.kt`, `ReminderNotifications.kt`, `ReminderNotificationDelegate.kt`, `ReminderActionReceiver.kt`, `MonthDetail`, `UpdateRenterUseCase`, `FakeBillRepository`, `RenterField`, `.months`, `MonthsViewModelTest`, `.form`?**
  _High betweenness centrality (0.156) - this node is a cross-community bridge._
- **Are the 21 inferred relationships involving `RenterId` (e.g. with `.aRenterWithNoBillHasNothingToMuteAndNothingChanges()` and `.aStorageFailureIsReported()`) actually correct?**
  _`RenterId` has 21 INFERRED edges - model-reasoned connections that need verification._
- **What connects `UnitTaken`, `InvalidBackup`, `OfferOutdated` to the rest of the system?**
  _129 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Shared UI Components` be split into smaller, more focused modules?**
  _Cohesion score 0.07668231611893583 - nodes in this community are weakly interconnected._
- **Why does `Paise` connect `Paise` to `RoomRenterRepositoryTest`, `Shared UI Components`, `Screen Test Helpers`, `FixedCharge`, `RenterLeavingUseCasesTest`, `AddRenterViewModelTest`, `Outcome`, `Create Bill Screen`, `Rent Increase and Billing Month`, `Add Renter Screen`, `Home Screen`, `.viewModel`, `RecordPaymentUseCaseTest`, `.encode`, `.useCase`, `.invoke`, `BuildBillUseCaseTest`, `RecordPaymentScreen.kt`, `SettingsViewModel.kt`, `.viewModel`, `RenterDetailScreen.kt`, `AppSettings`, `StepReminderTimeUseCase`, `RecordPaymentRoute`, `Modules.kt`, `RenterDetailViewModelTest.kt`, `MoneyInput`, `.plan`, `RecordPaymentViewModelTest`, `HomeViewModelTest.kt`, `FakeRenterRepository`, `AddRenterViewModelTest.kt`, `SaveRenterUseCaseTest`, `Bill`, `DomainError`, `RenterId`, `BillLineEntity`, `RoomBillRepositoryTest`, `BillingMonth`, `.renter`, `MonthDetailScreen.kt`, `ReminderNotifications.kt`, `MonthDetail`, `SaveSettingsUseCase`, `UpdateRenterUseCase`, `.pending`, `CurrencyFormat.android.kt`, `.form`, `FormatRupeesTest`, `PaiseTest.kt`?**
  _High betweenness centrality (0.129) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Paise` (e.g. with `.aRateIsAtMostNinetyNineRupeesSoAMissingDecimalPointIsCaught()` and `.parsesAPaymentWithUpToTwoDecimals()`) actually correct?**
  _`Paise` has 6 INFERRED edges - model-reasoned connections that need verification._
- **Should `Screen Test Helpers` be split into smaller, more focused modules?**
  _Cohesion score 0.11773417252869307 - nodes in this community are weakly interconnected._