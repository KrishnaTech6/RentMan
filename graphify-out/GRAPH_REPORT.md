# Graph Report - RentMan  (2026-11-27)

## Corpus Check
- 285 files · ~137,722 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 16 file(s) not represented in the graph (top: .xml 9, .properties 3, (none) 1)

## Summary
- 3587 nodes · 11675 edges · 173 communities (77 shown, 96 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 618 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `434e3e89`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AddressId
- MonthCard.kt
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
- MessageEditViewModelTest
- BuildBillUseCaseTest
- Paise
- RecordPaymentScreen.kt
- HomeViewModelTest.kt
- .viewModel
- BackupDao
- SettingsScreen.kt
- RenterDetailScreen.kt
- AppSettings
- Backup.kt
- RentChangeEntity
- NotificationPermission.android.kt
- AddressPickerScreen.kt
- Modules.kt
- FakeRenterRepository
- Clock
- base
- MoneyInput
- .pending
- MeterPhotos.ios.kt
- .plan
- .plan
- HomeIntent
- RecordPaymentViewModelTest
- res
- MonthDetailScreen.kt
- FakeRenterRepository
- AddRenterViewModelTest.kt
- MeterPhotoFiles
- GoldenBackupTest.kt
- CropRect
- MeterPhotoFiles.kt
- IosReminderScheduler.kt
- SaveRenterUseCaseTest
- BackupFiles.ios.kt
- NotificationPermission.ios.kt
- DomainError
- RenterId
- FakeSettingsRepository
- BillLineEntity
- MeterReadingParserTest
- .summary
- ReminderScheduler
- RenterDetailViewModel
- MeterPhotos.kt
- RoomBillRepositoryTest
- FakeBillRepository
- ComposeView
- MviViewModel base class (toState, Eagerly)
- MviViewModel
- Renter
- .renter
- bill table
- MessageEditScreen.kt
- ReminderNotifications.kt
- RenterEntity
- .of
- MeterCropper.kt
- BackupCodec.kt
- PaymentEntity
- HomeViewModel
- ReminderNotificationDelegate.kt
- Pending / balance derivation (PendingCalculator)
- sampleBackup
- renter table
- MonthDetailViewModel
- PhotoFilesTest.kt
- .of
- SettingsIntent
- Payment
- MuteRemindersUseCase
- MeterPhotoName
- RenterDetailIntent
- FakeBillRepository
- BillingMonthTest
- clockDigits
- ARCHITECTURE.md (engineering contract)
- .months
- NotificationPermission.kt
- Test
- Address (glossary term)
- IosMeterPhotoFiles
- AddressEntity
- MeterTextTest
- MonthsViewModelTest
- RoomPaymentRepositoryTest
- Pending (glossary term)
- .form
- BackupFiles
- .settingsAndAddressesSurviveAndTheMessagesStartAsOurs
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
- AppDatabase
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
- .invoke
- MonthsViewModel
- AppNavigation.kt
- RenterMonthTest
- FakeAddressRepository
- Migrations.kt
- CurrencyFormat.ios.kt
- .ledgerOf
- BillMapperTest
- Corner
- FakeBillRepository
- RenterSummary
- .rentFor
- RentChangeMapperTest
- .invoke
- RoomAddressRepositoryTest
- MeterReadingParser
- StringResourcesTest.kt
- MeterPhotoSamples.kt

## God Nodes (most connected - your core abstractions)
1. `Outcome` - 254 edges
2. `RenterId` - 232 edges
3. `Paise` - 220 edges
4. `DomainError` - 159 edges
5. `BillingMonth` - 136 edges
6. `Payment` - 84 edges
7. `Renter` - 79 edges
8. `RenterSummary` - 71 edges
9. `AppSettings` - 70 edges
10. `FakeRenterRepository` - 68 edges

## Surprising Connections (you probably didn't know these)
- `Money history is immutable` --semantically_similar_to--> `Bill snapshot principle (no recompute from current settings)`  [INFERRED] [semantically similar]
  DATABASE.md → docs/adr/0002-bills-are-immutable-snapshots.md
- `Error` --references--> `DomainError`  [EXTRACTED]
  sharedUI/src/commonMain/kotlin/com/rentman/sharedui/feature/address/AddressPickerContract.kt → domain/src/commonMain/kotlin/com/rentman/domain/Outcome.kt
- `Error` --references--> `DomainError`  [EXTRACTED]
  sharedUI/src/commonMain/kotlin/com/rentman/sharedui/feature/bill/CreateBillContract.kt → domain/src/commonMain/kotlin/com/rentman/domain/Outcome.kt
- `Error` --references--> `DomainError`  [EXTRACTED]
  sharedUI/src/commonMain/kotlin/com/rentman/sharedui/feature/home/HomeContract.kt → domain/src/commonMain/kotlin/com/rentman/domain/Outcome.kt
- `Error` --references--> `DomainError`  [EXTRACTED]
  sharedUI/src/commonMain/kotlin/com/rentman/sharedui/feature/messages/MessageEditContract.kt → domain/src/commonMain/kotlin/com/rentman/domain/Outcome.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Bill immutability with the remove-and-remake exception** — docs_adr_0002_bills_are_immutable_snapshots, docs_adr_0009_a_wrong_bill_is_removed_and_made_again, database_bill_table, plan_remove_wrong_bill_feature, database_money_history_immutable [EXTRACTED 1.00]
- **MVI screen contract: UiState, NavEvent and MviViewModel** — architecture_mviviewmodel, architecture_uistate, architecture_navevent, architecture_dumb_ui [EXTRACTED 1.00]
- **Pending is derived from bill lines and payments, never stored** — database_pending_calculation, database_bill_line_table, database_payment_table, database_derived_not_stored, glossary_pending [EXTRACTED 1.00]

## Communities (173 total, 96 thin omitted)

### Community 0 - "AddressId"
Cohesion: 0.05
Nodes (28): toDomain(), toDomain(), RenterMapperTest, RoomRenterRepositoryTest, Address, AddressId, ChargeField, Amount (+20 more)

### Community 1 - "MonthCard.kt"
Cohesion: 0.06
Nodes (9): MainActivity, MonthSummary, App(), LegendRow(), MonthBar(), MonthCard(), ScreenTopBar(), UndoBar() (+1 more)

### Community 2 - "Screen Test Helpers"
Cohesion: 0.08
Nodes (15): toDomain(), Bill, BillId, BillLine, BillLineKind, Adjustment, Electricity, FixedCharge (+7 more)

### Community 3 - "AddRenterViewModelTest"
Cohesion: 0.09
Nodes (27): AddCharge, AddRenterIntent, AddRenterNavEvent, AskToRaiseChanged, Back, BillsElectricityChanged, ChargeAmountChanged, ChargeLabelChanged (+19 more)

### Community 5 - "AddressPickerViewModelTest"
Cohesion: 0.09
Nodes (14): Add, AddressPickerIntent, AddressPickerNavEvent, AddressPickerUiState, Confirm, Content, Error, Loading (+6 more)

### Community 7 - "Outcome"
Cohesion: 0.05
Nodes (15): RoomAddressRepository, RoomBackupRepository, RoomBillRepository, dataModule(), AppDispatchers, RoomRentIncreaseRepository, RoomRenterRepository, safeCall() (+7 more)

### Community 8 - "Create Bill Screen"
Cohesion: 0.07
Nodes (15): AmountText(), BillCard(), CreateBillRoute(), CreateBillScreen(), LineRow(), Math(), MeterPhotoRow(), MeterReplaced() (+7 more)

### Community 9 - "Rent Increase and Billing Month"
Cohesion: 0.13
Nodes (10): toDomain(), ApplyRentIncreaseUseCase, PostponeRentIncreaseUseCase, RentChange, RentChangeId, RentIncreaseRepository, UndoRentIncreaseUseCase, FakeRentIncreaseRepository (+2 more)

### Community 10 - "Renter Detail ViewModel"
Cohesion: 0.16
Nodes (8): RemovePaymentUseCase, RestorePaymentUseCase, MarkRenterLeftUseCase, RemoveBill, RemovePayment, SetRemindersMuted, UndoRemove, RenterDetailViewModelTest

### Community 11 - "Add Renter Screen"
Cohesion: 0.05
Nodes (7): AddRenterRoute(), AddRenterScreen(), AddressRow(), ChargeRow(), FilledWithProblemsPreview(), MonthStepper(), SwitchRow()

### Community 13 - "Home Screen"
Cohesion: 0.06
Nodes (20): formatRupees(), label(), shortLabel(), shortName(), amountStyle(), BillTodoCard(), Centered(), Chip() (+12 more)

### Community 14 - ".viewModel"
Cohesion: 0.20
Nodes (6): RateChanged, RepeatChanged, RestoreFrom, TimeStepped, FakeSettingsRepository, SettingsViewModelTest

### Community 18 - "MessageEditViewModelTest"
Cohesion: 0.12
Nodes (13): Back, Close, Error, InsertVariable, Loading, MessageEditIntent, MessageEditNavEvent, MessageEditUiState (+5 more)

### Community 19 - "BuildBillUseCaseTest"
Cohesion: 0.10
Nodes (16): BillContext, BuildBillUseCaseTest, BillStep, Reading, Review, Close, Content, CreateBillNavEvent (+8 more)

### Community 20 - "Paise"
Cohesion: 0.13
Nodes (7): BillCalculator, Paise, StillOwes, overpayment(), BillCalculatorTest, formatRupees(), formatRupees()

### Community 21 - "RecordPaymentScreen.kt"
Cohesion: 0.10
Nodes (12): Divider(), Section(), SegmentButton(), Segmented(), StepButton(), TextRow(), DateStepper(), Form() (+4 more)

### Community 22 - "HomeViewModelTest.kt"
Cohesion: 0.07
Nodes (12): BuildBillUseCase, RestoreBillUseCase, MessageKind, Bill, MonthReview, PaymentReceived, ObserveMessageUseCase, fold() (+4 more)

### Community 23 - ".viewModel"
Cohesion: 0.12
Nodes (12): Back, ChangeReading, ClearMeterPhoto, CreateBillIntent, MeterReplacedChanged, MeterScanned, NewMeterStartChanged, ReadingChanged (+4 more)

### Community 25 - "SettingsScreen.kt"
Cohesion: 0.06
Nodes (7): BackedUpPreview(), Form(), rateProblem(), repeatLabel(), SettingsRoute(), SettingsScreen(), TimeStepper()

### Community 26 - "RenterDetailScreen.kt"
Cohesion: 0.08
Nodes (15): LedgerEntry, PaymentEntry, BalanceText(), BalanceCard(), BillRow(), Body(), EntryAction, EntryCard() (+7 more)

### Community 27 - "AppSettings"
Cohesion: 0.10
Nodes (5): RoomSettingsRepository, toDomain(), toEntity(), RoomSettingsRepositoryTest, AppSettings

### Community 28 - "Backup.kt"
Cohesion: 0.16
Nodes (8): BackupExport, BackupRepository, ExportBackupUseCase, ObserveBackupStatusUseCase, RestoreBackupUseCase, UndoRestoreUseCase, inputReducers(), loadReducers()

### Community 30 - "RentChangeEntity"
Cohesion: 0.21
Nodes (3): RentChangeDao, RentChangeEntity, toEntity()

### Community 31 - "NotificationPermission.android.kt"
Cohesion: 0.09
Nodes (8): readText(), rememberBackupFiles(), MeterPhotoProvider, rememberPhotoImage(), findActivity(), notificationsAllowed(), openNotificationSettings(), rememberNotificationPermission()

### Community 32 - "AddressPickerScreen.kt"
Cohesion: 0.16
Nodes (7): AddressPickerRoute(), AddressPickerScreen(), AddressRow(), ContentBody(), ContentPreview(), NewAddress(), RadioDot()

### Community 33 - "Modules.kt"
Cohesion: 0.06
Nodes (25): MarkBackedUpUseCase, BillRepository, ObserveBillContextUseCase, ObserveBillTodosUseCase, ObserveHomeUseCase, UnbilledMonths, ObserveMonthDetailUseCase, ObserveMonthsUseCase (+17 more)

### Community 34 - "FakeRenterRepository"
Cohesion: 0.15
Nodes (5): RaiseRent, SelectFilter, UndoRaise, HomeViewModelTest, FakeRenterRepository

### Community 36 - "base"
Cohesion: 0.06
Nodes (32): ENABLE_USER_SCRIPT_SANDBOXING, EXCLUDED_ARCHS[sdk=iphonesimulator*], FRAMEWORK_SEARCH_PATHS, OTHER_LDFLAGS, PRODUCT_BUNDLE_IDENTIFIER, SWIFT_VERSION, iOS, path (+24 more)

### Community 39 - "MeterPhotos.ios.kt"
Cohesion: 0.14
Nodes (6): PhotoItem, PhotoPickerDelegate, PreviewSource, rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource()

### Community 41 - ".plan"
Cohesion: 0.19
Nodes (3): ReminderPlan, ReminderPlanner, FakeReminderScheduler

### Community 42 - "HomeIntent"
Cohesion: 0.20
Nodes (12): AddRenter, CreateBill, HomeIntent, HomeNavEvent, NotNow, OpenAddRenter, OpenCreateBill, OpenMonths (+4 more)

### Community 43 - "RecordPaymentViewModelTest"
Cohesion: 0.13
Nodes (11): AmountChanged, Back, DayStepped, ModeChanged, NoteChanged, PostTo, RecordPaymentIntent, Retry (+3 more)

### Community 44 - "res"
Cohesion: 0.07
Nodes (5): message(), messageResource(), kindLabel(), modeLabel(), timeLabel()

### Community 45 - "MonthDetailScreen.kt"
Cohesion: 0.09
Nodes (12): CenteredMessage(), ErrorMessage(), CashInRow(), ContentPreview(), MonthDetailRoute(), MonthDetailScreen(), RenterMonthRow(), EmptyPreview() (+4 more)

### Community 46 - "FakeRenterRepository"
Cohesion: 0.23
Nodes (3): ObserveHomeUseCaseTest, FakePaymentRepository, FakeRenterRepository

### Community 47 - "AddRenterViewModelTest.kt"
Cohesion: 0.10
Nodes (4): ObserveAddressesUseCase, BuildFixedChargeUseCase, ObserveRenterFormUseCase, FlatMapFirstTest

### Community 48 - "MeterPhotoFiles"
Cohesion: 0.28
Nodes (4): MeterPhotoFiles, rememberPhotoFiles(), rememberPhotoOpener(), rememberPhotoSource()

### Community 53 - "SaveRenterUseCaseTest"
Cohesion: 0.19
Nodes (3): SaveRenterUseCase, BuildFixedChargeUseCaseTest, SaveRenterUseCaseTest

### Community 56 - "DomainError"
Cohesion: 0.05
Nodes (50): BillField, NewMeterStart, Reading, BackupFromNewerVersion, BillExists, BillNotRemovable, BlankName, DomainError (+42 more)

### Community 58 - "FakeSettingsRepository"
Cohesion: 0.07
Nodes (8): BackupStatus, SaveMessageUseCase, BackupUseCasesTest, FakeSettingsRepository, MessageUseCasesTest, ReminderSyncTest, ReminderSettingsUseCasesTest, SaveSettingsUseCaseTest

### Community 59 - "BillLineEntity"
Cohesion: 0.19
Nodes (5): toEntity(), BillDao, BillEntity, BillLineEntity, BillWithLines

### Community 62 - "ReminderScheduler"
Cohesion: 0.18
Nodes (7): androidDataModule(), iosDataModule(), ReminderScheduler, startRentManKoin(), AndroidReminderScheduler, sharedModules(), startRentManKoin()

### Community 64 - "MeterPhotos.kt"
Cohesion: 0.18
Nodes (12): PhotoFiles, PhotoOpener, PhotoSource, readWithin(), rememberPhotoFiles(), rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource() (+4 more)

### Community 66 - "FakeBillRepository"
Cohesion: 0.18
Nodes (5): CreateBillUseCase, RemoveBillUseCase, CreateBillUseCaseTest, FakeBillRepository, RemoveBillUseCaseTest

### Community 67 - "ComposeView"
Cohesion: 0.14
Nodes (7): ComposeView, ContentView, .body, iOSApp, .body, sharedUI, SwiftUI

### Community 68 - "MviViewModel base class (toState, Eagerly)"
Cohesion: 0.14
Nodes (14): Data layer (:data, Room 3), Domain layer (:domain), formatRupees expect/actual (Indian grouping), Koin dependency injection, AGP 9 KMP module structure (androidApp, iosApp, sharedUI, data, domain), MviViewModel base class (toState, Eagerly), NavEvent one-shot SharedFlow, Navigation 3 (NavKey, NavDisplay) (+6 more)

### Community 69 - "MviViewModel"
Cohesion: 0.06
Nodes (10): CreateBillViewModel, MessageEditViewModel, Close, PaymentPost, RecordPaymentNavEvent, Saved, RecordPaymentViewModel, AddRenterViewModel (+2 more)

### Community 70 - "Renter"
Cohesion: 0.10
Nodes (5): remindersMutedFor(), Renter, RenterForm, RenterFormValidator, RenterUnits

### Community 72 - "bill table"
Cohesion: 0.15
Nodes (10): Platform gateways and UI-only expect/actual (photo, backup files), bill table, Ledger (bills and payments merged), payment table (voidable), ADR 0002: Bills are immutable snapshots made of lines, ADR 0009: A wrong bill is removed and made again, Adjustment (glossary term), Electricity rate and meter reading rules (+2 more)

### Community 73 - "MessageEditScreen.kt"
Cohesion: 0.12
Nodes (9): ActionButton(), TextAction(), Editor(), Label(), MessageEditRoute(), MessageEditScreen(), MessageField(), UnknownVariablePreview() (+1 more)

### Community 74 - "ReminderNotifications.kt"
Cohesion: 0.18
Nodes (4): CreateBill, Reminder, RentPending, ReminderWorker

### Community 75 - "RenterEntity"
Cohesion: 0.13
Nodes (6): FixedChargeEntity, RenterWithCharges, InsertedRenter, RenterDao, RenterEntity, toEntity()

### Community 76 - ".of"
Cohesion: 0.18
Nodes (4): MonthsInput, observeMonthsInput(), RenterMonth, previewRow()

### Community 77 - "MeterCropper.kt"
Cohesion: 0.05
Nodes (12): ActionRow(), CropArea(), LightButton(), MeterCropper(), MeterPhotoThumbnail(), MeterScanFlow(), Working(), SectionTitle() (+4 more)

### Community 79 - "PaymentEntity"
Cohesion: 0.23
Nodes (3): PaymentDao, PaymentEntity, toEntity()

### Community 81 - "ReminderNotificationDelegate.kt"
Cohesion: 0.09
Nodes (3): AppDispatchers, ReminderActionReceiver, ReminderNotificationDelegate

### Community 82 - "Pending / balance derivation (PendingCalculator)"
Cohesion: 0.15
Nodes (11): Paise value class (money as Long), bill_line table, Pending / balance derivation (PendingCalculator), Renter status derivation (No bill, Paid, Late, Part paid, Unpaid), Due day (glossary term), Late (glossary term), Not due yet (glossary term), Part paid and Unpaid statuses (glossary terms) (+3 more)

### Community 85 - "renter table"
Cohesion: 0.17
Nodes (12): address table, fixed_charge table, rent_change table, renter table, Unbilled months derivation, ADR 0008 The landlord creates every bill, Remade bill uses rent of its own month (RentHistory.rentFor), Rent increase (glossary term) (+4 more)

### Community 86 - "MonthDetailViewModel"
Cohesion: 0.06
Nodes (20): BillMessage, MonthReviewMessage, MonthDetail, MessageTemplate, BillMessageTest, MonthReviewMessageTest, MessageTemplateTest, Back (+12 more)

### Community 89 - "SettingsIntent"
Cohesion: 0.17
Nodes (12): Back, BackUpNow, BackupShared, BackupShareFailed, Close, OpenMessage, Retry, Save (+4 more)

### Community 90 - "Payment"
Cohesion: 0.12
Nodes (7): toDomain(), RoomPaymentRepository, PaymentMapperTest, Payment, PaymentId, PaymentRepository, FakePaymentRepository

### Community 93 - "RenterDetailIntent"
Cohesion: 0.11
Nodes (17): Back, Close, Edit, Error, Loading, MarkLeft, MoveBack, OfBill (+9 more)

### Community 96 - "clockDigits"
Cohesion: 0.39
Nodes (3): clockDigits(), isAfternoon(), TimeTextTest

### Community 97 - "ARCHITECTURE.md (engineering contract)"
Cohesion: 0.36
Nodes (7): ARCHITECTURE.md (engineering contract), Workflow for every change (ponytail, review, graphify), DATABASE.md (schema v1), Home design, GLOSSARY.md (ubiquitous language), PLAN.md (product plan), Claude Design mockup (design/project)

### Community 99 - "NotificationPermission.kt"
Cohesion: 0.36
Nodes (4): AskForNotificationsOnce(), NotificationPermission, rememberNotificationPermission(), rememberNotificationPermission()

### Community 101 - "Address (glossary term)"
Cohesion: 0.29
Nodes (8): Add renter design, Address picker design, Address filter chips, Address (glossary term), Left renter (glossary term), Renter (glossary term), Unit (glossary term), Renter edit and mark left feature

### Community 104 - "MeterTextTest"
Cohesion: 0.21
Nodes (12): SuggestMeterReadingUseCase, around(), digitRows(), digitsOf(), findMeters(), fit(), MeterSpot, readingLines() (+4 more)

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

### Community 148 - "MonthsViewModel"
Cohesion: 0.15
Nodes (12): Back, Close, Content, Error, Loading, MonthsIntent, MonthsNavEvent, MonthsUiState (+4 more)

### Community 149 - "AppNavigation.kt"
Cohesion: 0.16
Nodes (14): AppNavigation(), Navigator, AddRenterKey, AddressPickerKey, CreateBillKey, EditRenterKey, HomeKey, MessageEditKey (+6 more)

### Community 157 - "Corner"
Cohesion: 0.22
Nodes (8): Corner, BottomLeft, BottomRight, TopLeft, TopRight, Edge, Grab, Whole

### Community 159 - "RenterSummary"
Cohesion: 0.12
Nodes (18): BillTodo, HomeSnapshot, Lists, RenterRow, RenterStatus, Late, NoBillYet, Paid (+10 more)

### Community 164 - "MeterReadingParser"
Cohesion: 0.20
Nodes (4): Candidate, MeterReadingParser, MeterSuggestion, SuggestMeterReadingUseCaseTest

## Knowledge Gaps
- **135 isolated node(s):** `UnitTaken`, `InvalidBackup`, `InvalidMessage`, `OfferOutdated`, `NotFound` (+130 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 699 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **96 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `RenterId` connect `RenterId` to `AddressId`, `Screen Test Helpers`, `AddRenterViewModelTest`, `RenterLeavingUseCasesTest`, `.useCase`, `Outcome`, `Create Bill Screen`, `Rent Increase and Billing Month`, `Renter Detail ViewModel`, `Add Renter Screen`, `RoomRentIncreaseRepositoryTest`, `Home Screen`, `RecordPaymentUseCaseTest`, `.encode`, `.useCase`, `BuildBillUseCaseTest`, `RecordPaymentScreen.kt`, `RenterMonthTest`, `FakeAddressRepository`, `HomeViewModelTest.kt`, `AppNavigation.kt`, `RenterDetailScreen.kt`, `.ledgerOf`, `BillMapperTest`, `.viewModel`, `RenterSummary`, `.rentFor`, `RentChangeMapperTest`, `Modules.kt`, `FakeRenterRepository`, `.pending`, `.plan`, `HomeIntent`, `RecordPaymentViewModelTest`, `MonthDetailScreen.kt`, `FakeRenterRepository`, `AddRenterViewModelTest.kt`, `SaveRenterUseCaseTest`, `DomainError`, `FakeSettingsRepository`, `.summary`, `RenterDetailViewModel`, `RoomBillRepositoryTest`, `FakeBillRepository`, `MviViewModel`, `Renter`, `.renter`, `ReminderNotifications.kt`, `.of`, `ReminderNotificationDelegate.kt`, `AppNavigation.kt`, `MonthDetailViewModel`, `Payment`, `MuteRemindersUseCase`, `FakeBillRepository`, `.months`, `MonthsViewModelTest`, `RoomPaymentRepositoryTest`, `.form`?**
  _High betweenness centrality (0.166) - this node is a cross-community bridge._
- **Are the 21 inferred relationships involving `RenterId` (e.g. with `.aRenterWithNoBillHasNothingToMuteAndNothingChanges()` and `.aStorageFailureIsReported()`) actually correct?**
  _`RenterId` has 21 INFERRED edges - model-reasoned connections that need verification._
- **What connects `UnitTaken`, `InvalidBackup`, `InvalidMessage` to the rest of the system?**
  _135 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AddressId` be split into smaller, more focused modules?**
  _Cohesion score 0.052850877192982454 - nodes in this community are weakly interconnected._
- **Why does `Outcome` connect `Outcome` to `AddressId`, `Screen Test Helpers`, `Create Bill Screen`, `Rent Increase and Billing Month`, `.viewModel`, `RecordPaymentUseCaseTest`, `AndroidReminderScheduler.kt`, `BuildBillUseCaseTest`, `Paise`, `HomeViewModelTest.kt`, `FakeAddressRepository`, `.ledgerOf`, `AppSettings`, `Backup.kt`, `FakeBillRepository`, `RenterSummary`, `Modules.kt`, `.invoke`, `MoneyInput`, `.pending`, `.plan`, `FakeRenterRepository`, `AddRenterViewModelTest.kt`, `GoldenBackupTest.kt`, `IosReminderScheduler.kt`, `SaveRenterUseCaseTest`, `DomainError`, `RenterId`, `FakeSettingsRepository`, `FakeBillRepository`, `MviViewModel`, `Renter`, `ReminderNotifications.kt`, `.of`, `BackupCodec.kt`, `MonthDetailViewModel`, `Payment`, `MuteRemindersUseCase`?**
  _High betweenness centrality (0.129) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Paise` (e.g. with `.aRateIsAtMostNinetyNineRupeesSoAMissingDecimalPointIsCaught()` and `.parsesAPaymentWithUpToTwoDecimals()`) actually correct?**
  _`Paise` has 6 INFERRED edges - model-reasoned connections that need verification._
- **Should `MonthCard.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06485671191553545 - nodes in this community are weakly interconnected._