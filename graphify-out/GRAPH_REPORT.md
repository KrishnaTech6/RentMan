# Graph Report - RentMan  (2026-11-24)

## Corpus Check
- 228 files · ~66,064 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: .xml 5, .properties 3, (none) 1)

## Summary
- 2571 nodes · 8427 edges · 149 communities (71 shown, 78 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 505 edges (avg confidence: 0.82)
- Token cost: 90,897 input · 0 output

## Community Hubs (Navigation)
- Billing Month and Use Cases
- Home and Renter Screen Intents
- ViewModel Reducers and Flow Operators
- Add Renter Form Strings
- Compose UI Primitives
- Repositories and Domain Errors
- Renter Mapping and Tests
- Payment Mapping and Errors
- Backup Use Cases
- Add Renter Intents
- Settings and Backup Strings
- Home Screen Strings
- Data Layer Tests
- Backup DAO
- Platform Interop Android and iOS
- Home Snapshot and Ledger
- Cluster 16
- Cluster 17
- Cluster 18
- Cluster 19
- Cluster 20
- Cluster 21
- Cluster 22
- Cluster 23
- Cluster 24
- Cluster 25
- Cluster 26
- Cluster 27
- Cluster 28
- Cluster 29
- Cluster 30
- Cluster 31
- Cluster 32
- Cluster 33
- Cluster 34
- Cluster 35
- Cluster 36
- Cluster 37
- Cluster 38
- Cluster 39
- Cluster 40
- Cluster 41
- Cluster 42
- Cluster 43
- Cluster 44
- Cluster 45
- Cluster 46
- Cluster 47
- Cluster 48
- Cluster 49
- Cluster 50
- Cluster 51
- Cluster 52
- Cluster 53
- Cluster 54
- Cluster 55
- Cluster 56
- Cluster 57
- Cluster 58
- Cluster 59
- Cluster 60
- Cluster 61
- Cluster 62
- Cluster 63
- Cluster 64
- Cluster 65
- Cluster 66
- Cluster 67
- Cluster 68
- Cluster 69
- Cluster 70
- Cluster 71
- Cluster 72
- Cluster 73
- Cluster 74
- Cluster 75
- Cluster 76
- Cluster 77
- Cluster 78
- Cluster 79
- Cluster 80
- Cluster 81
- Cluster 82
- Cluster 83
- Cluster 84
- Cluster 85
- Cluster 86
- Cluster 87
- Cluster 88
- Cluster 89
- Cluster 90
- Cluster 91
- Cluster 92
- Cluster 93
- Cluster 94
- Cluster 95
- Cluster 96
- Cluster 97
- Cluster 98
- Cluster 99
- Cluster 100
- Cluster 102
- Cluster 104
- Cluster 105
- Cluster 106
- Cluster 113

## God Nodes (most connected - your core abstractions)
1. `Outcome` - 204 edges
2. `Paise` - 174 edges
3. `RenterId` - 169 edges
4. `DomainError` - 128 edges
5. `BillingMonth` - 97 edges
6. `Reducer` - 75 edges
7. `Bill` - 73 edges
8. `Payment` - 56 edges
9. `Renter` - 56 edges
10. `AddressId` - 52 edges

## Surprising Connections (you probably didn't know these)
- `Renter ledger design` --references--> `Pending`  [INFERRED]
  design/project/Renter.dc.html → GLOSSARY.md
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
- **Errors-as-values pipeline (safeFlow, Outcome, ViewModel fold)** — architecture_safeflow_and_safecall, architecture_outcome_t_result_type, architecture_mviviewmodel_base_class, docs_adr_0007_errors_are_values_caught_once_in_the_data_layer [EXTRACTED 0.95]
- **Pending computed from bill lines and payments** — database_bill_line_table, database_payment_table, architecture_pendingcalculator, docs_adr_0001_pending_is_computed_never_stored, glossary_pending [EXTRACTED 0.95]
- **Reminder resync flow (plan, collector, scheduler, mute)** — architecture_observereminderplanusecase, architecture_remindersync_app_scoped_collector, architecture_reminderscheduler_interface, database_reminder_mute_derivation [EXTRACTED 0.95]

## Communities (149 total, 78 thin omitted)

### Community 0 - "Billing Month and Use Cases"
Cohesion: 0.05
Nodes (16): CreateBillUseCase, MuteRemindersUseCase, BillingMonthTest, CreateBillUseCaseTest, FakeBillRepository, ObserveBillContextUseCaseTest, ObserveBillTodosUseCaseTest, ObserveHomeUseCaseTest (+8 more)

### Community 1 - "Home and Renter Screen Intents"
Cohesion: 0.06
Nodes (28): HomeIntent, AddRenter, NotNow, OpenRenter, OpenSettings, RaiseRent, Refresh, Retry (+20 more)

### Community 3 - "Add Renter Form Strings"
Cohesion: 0.05
Nodes (10): Section(), RemoveCharge, AddRenterRoute(), AddressRow(), chargeProblem(), ChargeRow(), ChargesSection(), FilledWithProblemsPreview() (+2 more)

### Community 4 - "Compose UI Primitives"
Cohesion: 0.10
Nodes (3): ActionButton(), ActionRow(), RentManColors

### Community 5 - "Repositories and Domain Errors"
Cohesion: 0.08
Nodes (12): DomainError, BackupFromNewerVersion, BillExists, BlankName, DuplicateName, InvalidBackup, NotFound, OfferOutdated (+4 more)

### Community 6 - "Renter Mapping and Tests"
Cohesion: 0.08
Nodes (22): toDomain(), RenterMapperTest, InvalidCharge, Address, AddressId, ChargeField, Amount, Label (+14 more)

### Community 7 - "Payment Mapping and Errors"
Cohesion: 0.07
Nodes (27): toDomain(), PaymentMapperTest, InvalidBill, InvalidPayment, InvalidRenter, InvalidSettings, overpayment(), Payment (+19 more)

### Community 8 - "Backup Use Cases"
Cohesion: 0.09
Nodes (12): BackupExport, BackupRepository, BackupStatus, ExportBackupUseCase, ObserveBackupStatusUseCase, RestoreBackupUseCase, UndoRestoreUseCase, SettingsRepository (+4 more)

### Community 9 - "Add Renter Intents"
Cohesion: 0.12
Nodes (20): AddRenterIntent, AddCharge, AskToRaiseChanged, Back, BillsElectricityChanged, ChargeAmountChanged, ChargeLabelChanged, DueDayChanged (+12 more)

### Community 10 - "Settings and Backup Strings"
Cohesion: 0.06
Nodes (12): Divider(), SegmentButton(), Segmented(), StepButton(), TextRow(), MonthStepper(), BackedUpPreview(), Form() (+4 more)

### Community 11 - "Home Screen Strings"
Cohesion: 0.08
Nodes (15): AmountText(), formatRupees(), StatusPill(), UndoBar(), BillTodoCard(), Centered(), ContentBody(), EmptyPreview() (+7 more)

### Community 14 - "Platform Interop Android and iOS"
Cohesion: 0.07
Nodes (7): readText(), rememberBackupFiles(), findActivity(), notificationsAllowed(), openNotificationSettings(), rememberNotificationPermission(), rememberNotificationPermission()

### Community 15 - "Home Snapshot and Ledger"
Cohesion: 0.10
Nodes (14): BillTodo, HomeSnapshot, RenterRow, LedgerEntry, BillEntry, PaymentEntry, Movement, RenterLedger (+6 more)

### Community 16 - "Cluster 16"
Cohesion: 0.10
Nodes (11): CreateBillRoute(), CreateBillScreen(), Math(), MeterReplaced(), ProblemText(), ReadingStep(), ReviewPreview(), ReviewStep() (+3 more)

### Community 17 - "Cluster 17"
Cohesion: 0.15
Nodes (3): RentIncreasePolicy, ReminderPlannerTest, RentIncreasePolicyTest

### Community 18 - "Cluster 18"
Cohesion: 0.10
Nodes (10): AppNavigation(), Navigator, AddRenterKey, AddressPickerKey, CreateBillKey, HomeKey, RecordPaymentKey, RenterKey (+2 more)

### Community 19 - "Cluster 19"
Cohesion: 0.13
Nodes (9): RentChangeMapperTest, ApplyRentIncreaseUseCase, PostponeRentIncreaseUseCase, RentChange, RentChangeId, RentIncreaseRepository, UndoRentIncreaseUseCase, FakeRentIncreaseRepository (+1 more)

### Community 20 - "Cluster 20"
Cohesion: 0.11
Nodes (27): AppDispatchers, BillCalculator, Bounded contexts tenancy billing reminders settings backup, Data layer (:data), Domain layer (:domain), Entity mappers inside :data, formatRupees expect/actual, Koin dependency injection (+19 more)

### Community 21 - "Cluster 21"
Cohesion: 0.08
Nodes (5): BalanceText(), message(), modeLabel(), isAfternoon(), timeLabel()

### Community 22 - "Cluster 22"
Cohesion: 0.17
Nodes (3): BuildBillUseCase, BillMessageTest, BuildBillUseCaseTest

### Community 23 - "Cluster 23"
Cohesion: 0.12
Nodes (16): CenteredMessage(), ErrorMessage(), ScreenTopBar(), RecordPaymentScreen(), AddRenterScreen(), BalanceCard(), BillRow(), Body() (+8 more)

### Community 24 - "Cluster 24"
Cohesion: 0.16
Nodes (10): CreateBillIntent, Back, ChangeReading, MeterReplacedChanged, NewMeterStartChanged, ReadingChanged, Retry, Review (+2 more)

### Community 25 - "Cluster 25"
Cohesion: 0.09
Nodes (12): ObserveAddressesUseCase, BuildFixedChargeUseCase, RenterField, Address, Charge, DueDay, Name, OwnRate (+4 more)

### Community 26 - "Cluster 26"
Cohesion: 0.12
Nodes (4): MoneyInput, PhoneNumber, MoneyInputTest, PhoneNumberTest

### Community 27 - "Cluster 27"
Cohesion: 0.08
Nodes (5): DateStepper(), problemText(), RecordPaymentRoute(), WithProblemsPreview(), NavigationEffect()

### Community 28 - "Cluster 28"
Cohesion: 0.19
Nodes (21): BillRepository, ObserveBillContextUseCase, ObserveBillTodosUseCase, ObserveHomeUseCase, flatMap(), fold(), map(), Success (+13 more)

### Community 29 - "Cluster 29"
Cohesion: 0.15
Nodes (6): safeCall(), RoomSettingsRepository, toDomain(), toEntity(), RoomSettingsRepositoryTest, AppSettings

### Community 30 - "Cluster 30"
Cohesion: 0.06
Nodes (31): ENABLE_USER_SCRIPT_SANDBOXING, EXCLUDED_ARCHS[sdk=iphonesimulator*], FRAMEWORK_SEARCH_PATHS, OTHER_LDFLAGS, PRODUCT_BUNDLE_IDENTIFIER, SWIFT_VERSION, iOS, path (+23 more)

### Community 31 - "Cluster 31"
Cohesion: 0.14
Nodes (5): PaymentDao, PaymentEntity, toEntity(), RoomPaymentRepository, RoomPaymentRepositoryTest

### Community 32 - "Cluster 32"
Cohesion: 0.16
Nodes (11): RecordPaymentIntent, AmountChanged, Back, DayStepped, ModeChanged, NoteChanged, Retry, Save (+3 more)

### Community 33 - "Cluster 33"
Cohesion: 0.22
Nodes (11): SettingsIntent, Back, BackUpNow, RateChanged, RepeatChanged, RestoreFrom, Retry, TimeStepped (+3 more)

### Community 34 - "Cluster 34"
Cohesion: 0.08
Nodes (4): Reminder, CreateBills, ReminderNotifications, ReminderWorker

### Community 35 - "Cluster 35"
Cohesion: 0.13
Nodes (14): toDomain(), Bill, BillLine, BillMessage, BillContext, BillStep, Reading, Review (+6 more)

### Community 36 - "Cluster 36"
Cohesion: 0.13
Nodes (6): BackupFile, AddressEntity, FixedChargeEntity, RenterWithCharges, RenterEntity, toEntity()

### Community 38 - "Cluster 38"
Cohesion: 0.14
Nodes (6): toDomain(), BillingMonth, remindersMutedFor(), RenterId, CreateBill, OpenCreateBill

### Community 39 - "Cluster 39"
Cohesion: 0.13
Nodes (5): BillCalculator, Paise, RentPending, BillCalculatorTest, FormatRupeesTest

### Community 40 - "Cluster 40"
Cohesion: 0.17
Nodes (7): AddressPickerIntent, Add, Confirm, NameChanged, Retry, Select, AddressPickerViewModelTest

### Community 41 - "Cluster 41"
Cohesion: 0.14
Nodes (7): AddressPickerRoute(), AddressPickerScreen(), AddressRow(), ContentBody(), ContentPreview(), NewAddress(), RadioDot()

### Community 42 - "Cluster 42"
Cohesion: 0.12
Nodes (3): PickerDelegate, rememberBackupFiles(), topViewController()

### Community 43 - "Cluster 43"
Cohesion: 0.13
Nodes (4): ReminderPlan, ScheduledReminder, FakeReminderScheduler, IosReminderScheduler

### Community 44 - "Cluster 44"
Cohesion: 0.20
Nodes (5): toEntity(), BillDao, BillEntity, BillLineEntity, BillWithLines

### Community 45 - "Cluster 45"
Cohesion: 0.18
Nodes (3): RentChangeDao, RentChangeEntity, toEntity()

### Community 46 - "Cluster 46"
Cohesion: 0.19
Nodes (9): SettingsField, Rate, Repeat, Time, SettingsNavEvent, Close, ShareBackup, SettingsState (+1 more)

### Community 48 - "Cluster 48"
Cohesion: 0.23
Nodes (7): BillLineKind, Adjustment, Electricity, FixedCharge, Rent, FakeAddressRepository, Ravi

### Community 50 - "Cluster 50"
Cohesion: 0.16
Nodes (5): RecordPaymentNavEvent, Close, Saved, RecordPaymentViewModel, State

### Community 52 - "Cluster 52"
Cohesion: 0.26
Nodes (3): RoomBillRepository, RoomBillRepositoryTest, BillId

### Community 54 - "Cluster 54"
Cohesion: 0.14
Nodes (7): ComposeView, ContentView, .body, iOSApp, .body, sharedUI, SwiftUI

### Community 59 - "Cluster 59"
Cohesion: 0.24
Nodes (3): RoomAddressRepository, toDomain(), RoomAddressRepositoryTest

### Community 60 - "Cluster 60"
Cohesion: 0.15
Nodes (15): address table, bill table, Dates as plain integers (epoch days, YYYYMM), fixed_charge table, Ledger with running balance, payment table, Reminder mute derivation, renter table (+7 more)

### Community 61 - "Cluster 61"
Cohesion: 0.22
Nodes (6): androidDataModule(), ReminderScheduler, startRentManKoin(), AndroidReminderScheduler, sharedModules(), startRentManKoin()

### Community 64 - "Cluster 64"
Cohesion: 0.16
Nodes (4): label(), shortLabel(), ReminderText, text()

### Community 65 - "Cluster 65"
Cohesion: 0.27
Nodes (6): HomeNavEvent, OpenAddRenter, OpenRenter, OpenSettings, HomeViewModel, State

### Community 66 - "Cluster 66"
Cohesion: 0.46
Nodes (3): AddRenterViewModel, State, Reducer

### Community 67 - "Cluster 67"
Cohesion: 0.18
Nodes (3): MainActivity, App(), MainViewController()

### Community 68 - "Cluster 68"
Cohesion: 0.21
Nodes (13): Backup file picker and share sheet expect/actual, app_settings table, Backup JSON file and BackupCodec, backup_snapshot table, Restore in one transaction with automatic snapshot, Settings design, Backup and restore controls, ADR 0004 Settings in the database, not DataStore (+5 more)

### Community 69 - "Cluster 69"
Cohesion: 0.15
Nodes (13): rent_change table, Unbilled months derivation, WhatsApp message preview, Do next to-do cards, Raise rent card, ADR 0008 The landlord creates every bill, Billing month, Unbilled month (+5 more)

### Community 73 - "Cluster 73"
Cohesion: 0.30
Nodes (5): CreateBillNavEvent, Close, Saved, CreateBillViewModel, State

### Community 76 - "Cluster 76"
Cohesion: 0.21
Nodes (13): bill_line table, Pending derived query, Renter status derivation, Fixed monthly charges list, Bill preview design, Status pill, Meter reading design, ADR 0001 Pending is computed, never stored (+5 more)

### Community 78 - "Cluster 78"
Cohesion: 0.29
Nodes (5): Workflow for every change (ponytail, review, graphify), Home design, Send receipt on WhatsApp, Mockup vs plan differences, Visual look and light theme

### Community 80 - "Cluster 80"
Cohesion: 0.22
Nodes (10): Record payment design, Full amount or part payment, Due day, Late, Paid ahead, Part paid, Payment, Pending (+2 more)

### Community 81 - "Cluster 81"
Cohesion: 0.22
Nodes (9): SettingsForm, BackupMessage, Restored, RestoreUndone, Save, SettingsUiState, Content, Error (+1 more)

### Community 84 - "Cluster 84"
Cohesion: 0.36
Nodes (4): dataModule(), AppDispatchers, RoomRentIncreaseRepository, AppDispatchers

### Community 91 - "Cluster 91"
Cohesion: 0.29
Nodes (7): Own rate or global rate choice, Reading calculation (1325 - 1240 = 85 units x rate), Meter reading, Meter reset, Rate, Units, Electricity rate rules

### Community 93 - "Cluster 93"
Cohesion: 0.43
Nodes (3): BackupFiles, rememberBackupFiles(), rememberBackupFiles()

### Community 94 - "Cluster 94"
Cohesion: 0.43
Nodes (3): NotificationPermission, rememberNotificationPermission(), rememberNotificationPermission()

### Community 96 - "Cluster 96"
Cohesion: 0.47
Nodes (6): Add renter design, Address picker design, Address filter chips, Address, Renter, Unit

### Community 97 - "Cluster 97"
Cohesion: 0.33
Nodes (6): RenterStatus, Late, NoBillYet, Paid, PartPaid, Unpaid

### Community 100 - "Cluster 100"
Cohesion: 0.40
Nodes (5): Android WorkManager reminders, iOS notifications and 64 pending limit, ObserveReminderPlanUseCase, ReminderScheduler interface, ReminderSync app-scoped collector

### Community 104 - "Cluster 104"
Cohesion: 0.50
Nodes (4): AddRenterNavEvent, Close, PickAddress, Saved

### Community 105 - "Cluster 105"
Cohesion: 0.67
Nodes (3): BillField, NewMeterStart, Reading

### Community 106 - "Cluster 106"
Cohesion: 0.67
Nodes (3): Agreement start, Rent, Rent increase

## Knowledge Gaps
- **122 isolated node(s):** `InvalidBackup`, `BackupFromNewerVersion`, `OfferOutdated`, `NotFound`, `BillExists` (+117 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 480 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **78 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Paise` connect `Cluster 39` to `Billing Month and Use Cases`, `ViewModel Reducers and Flow Operators`, `Add Renter Form Strings`, `Renter Mapping and Tests`, `Payment Mapping and Errors`, `Add Renter Intents`, `Home Screen Strings`, `Data Layer Tests`, `Home Snapshot and Ledger`, `Cluster 16`, `Cluster 17`, `Cluster 19`, `Cluster 21`, `Cluster 22`, `Cluster 23`, `Cluster 24`, `Cluster 25`, `Cluster 26`, `Cluster 27`, `Cluster 28`, `Cluster 29`, `Cluster 32`, `Cluster 33`, `Cluster 34`, `Cluster 35`, `Cluster 37`, `Cluster 38`, `Cluster 47`, `Cluster 48`, `Cluster 51`, `Cluster 52`, `Cluster 53`, `Cluster 55`, `Cluster 71`, `Cluster 72`, `Cluster 86`, `Cluster 87`, `Cluster 88`, `Cluster 92`, `Cluster 102`?**
  _High betweenness centrality (0.146) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Paise` (e.g. with `.aRateIsAtMostNinetyNineRupeesSoAMissingDecimalPointIsCaught()` and `.parsesAPaymentWithUpToTwoDecimals()`) actually correct?**
  _`Paise` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `InvalidBackup`, `BackupFromNewerVersion`, `OfferOutdated` to the rest of the system?**
  _122 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Billing Month and Use Cases` be split into smaller, more focused modules?**
  _Cohesion score 0.053885787891799056 - nodes in this community are weakly interconnected._
- **Why does `Outcome` connect `Repositories and Domain Errors` to `Billing Month and Use Cases`, `ViewModel Reducers and Flow Operators`, `Renter Mapping and Tests`, `Payment Mapping and Errors`, `Backup Use Cases`, `Data Layer Tests`, `Home Snapshot and Ledger`, `Cluster 16`, `Cluster 19`, `Cluster 22`, `Cluster 25`, `Cluster 28`, `Cluster 29`, `Cluster 31`, `Cluster 34`, `Cluster 35`, `Cluster 36`, `Cluster 37`, `Cluster 38`, `Cluster 39`, `Cluster 43`, `Cluster 45`, `Cluster 47`, `Cluster 48`, `Cluster 51`, `Cluster 52`, `Cluster 53`, `Cluster 57`, `Cluster 58`, `Cluster 59`, `Cluster 71`, `Cluster 72`, `Cluster 89`?**
  _High betweenness centrality (0.119) - this node is a cross-community bridge._
- **Are the 13 inferred relationships involving `RenterId` (e.g. with `.aRenterWithNoBillHasNothingToMuteAndNothingChanges()` and `.aStorageFailureIsReported()`) actually correct?**
  _`RenterId` has 13 INFERRED edges - model-reasoned connections that need verification._
- **Should `Home and Renter Screen Intents` be split into smaller, more focused modules?**
  _Cohesion score 0.06426484907497566 - nodes in this community are weakly interconnected._