# Graph Report - RentMan  (2026-11-24)

## Corpus Check
- 99 files · ~85,093 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3260 nodes · 9706 edges · 182 communities (80 shown, 102 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 558 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Domain rules and tests
- Add renter screen and tests
- Create bill screen and tests
- Settings screen and tests
- Outcome and Room repositories
- Renter repository tests
- Edit renter and validation
- App settings storage
- Shared UI components
- Create bill UI steps
- Settings and backup status
- Backup file tests
- App navigation and routes
- Data module and database
- DomainError group
- Bill group
- SaveRenterUseCaseTest group
- AddRenterScreen.kt group
- SettingsScreen.kt group
- NotificationPermission.android.kt group
- localdate group
- HomeViewModelTest.kt group
- Paise group
- IosMeterPhotoFiles.kt group
- FakeRenterRepository group
- HomeScreen.kt group
- BackupDao group
- RoomBillRepositoryTest group
- res group
- RecordPaymentScreen.kt group
- RenterDetailScreen.kt group
- AddressPickerScreen.kt group
- FakeRenterRepository group
- RoomPaymentRepository.kt group
- base group
- BillUseCases.kt group
- MeterPhotos.ios.kt group
- RenterDetailViewModelTest group
- Reminders.kt group
- Payment group
- RenterLeavingUseCasesTest group
- RecordPaymentViewModelTest group
- MeterCropper.kt group
- Modules.kt group
- HomeViewModel.kt group
- GetTodayUseCase group
- CropRect group
- MeterPhotoFiles.kt group
- BackupFiles.ios.kt group
- BuildFixedChargeUseCaseTest group
- RentChangeEntity group
- safeCall() group
- MeterReadingParserTest group
- .useCase() group
- MeterPhotos.kt group
- Data layer (:data, Room 3, internal) group
- Context group
- RenterId group
- BillContext group
- SettingsField group
- RenterDetailViewModel group
- ComposeView group
- MviViewModel.kt group
- AddressId group
- RentChange group
- PaymentMode group
- PaymentUseCases.kt group
- AddressRepository group
- IosReminderScheduler.kt group
- HomeIntent group
- RenterEntity group
- RecordPaymentUseCaseTest group
- AddressPickerViewModelTest group
- BalanceText.kt group
- BackupCodec group
- IosDataModule.kt group
- .pending() group
- ObserveRenterSummariesUseCase group
- ReminderText.kt group
- renter table group
- .raised() group
- RecordPaymentViewModel group
- ReminderNotificationDelegate.kt group
- Backup file (JSON, BackupCodec, GoldenBackups) group
- ReminderScheduler group
- BillLineEntity group
- BackupCodec.kt group
- Pending (derived balance, PendingCalculator) group
- SafeTest.kt group
- PhotoFilesTest.kt group
- BillingMonth group
- RenterRow group
- .of() group
- .of() group
- ReminderNotifications.kt group
- bill table group
- MeterPhotoName group
- BillMapperTest group
- Bill line group
- RenterField group
- AddressPickerViewModel group
- ARCHITECTURE.md (engineering contract) group
- PaymentFormTest.kt group
- ReminderActionReceiver.kt group
- NotificationPermission.kt group
- RenterDetailIntent group
- BackupDao.kt group
- Test group
- ReminderPlan group
- NavigationEffect.kt group
- AddAddressUseCaseTest group
- BackupFiles group
- FlowExt.kt group
- Creating bills flow (reading, preview, send) group
- RenterStatus group
- ViewControllers.ios.kt group
- RentManApplication.kt group
- FieldProblem group
- PaiseTest.kt group
- RenterDetailNavEvent group
- Non-negotiable rules group
- RecordPaymentNavEvent group
- GoldenBackups.kt group
- ADR 0004 Settings in the database, not DataStore group
- Build and test commands group
- KMP structure with AGP 9 group
- Schema change policy (v1 frozen after first real install) group
- Own rate or global rate choice group
- New address add field group
- WhatsApp message preview group
- Do next to-do cards group
- Raise rent card group
- Status pill group
- Full amount or part payment group
- Send receipt on WhatsApp group
- Meter photo proof group
- Reading calculation (1325 - 1240 = 85 units x rate) group
- History ledger rows group
- Settings design group
- Backup and restore controls group
- ADR 0005 Navigation as one-shot SharedFlow events group

## God Nodes (most connected - your core abstractions)
1. `Outcome` - 242 edges
2. `DomainError` - 137 edges
3. `Paise` - 120 edges
4. `RenterId` - 94 edges
5. `Bill` - 91 edges
6. `FakeRenterRepository` - 61 edges
7. `BillingMonth` - 59 edges
8. `BillLine` - 45 edges
9. `FakeRenterRepository` - 41 edges
10. `FakeSettingsRepository` - 39 edges

## Surprising Connections (you probably didn't know these)
- `Error` --references--> `DomainError`  [EXTRACTED]
  sharedUI/src/commonMain/kotlin/com/rentman/sharedui/feature/address/AddressPickerContract.kt → domain/src/commonMain/kotlin/com/rentman/domain/Outcome.kt
- `Error` --references--> `DomainError`  [EXTRACTED]
  sharedUI/src/commonMain/kotlin/com/rentman/sharedui/feature/payment/RecordPaymentContract.kt → domain/src/commonMain/kotlin/com/rentman/domain/Outcome.kt
- `Renter ledger design` --references--> `Pending`  [INFERRED]
  design/project/Renter.dc.html → GLOSSARY.md
- `Address filter chips` --references--> `Address`  [INFERRED]
  design/project/Main.dc.html → GLOSSARY.md
- `Address picker design` --references--> `Address`  [INFERRED]
  design/project/AddressPicker.dc.html → GLOSSARY.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Domain, data and presentation layers form the layered architecture** — architecture_domain_layer, architecture_data_layer, architecture_presentation_layer, architecture_project_structure [EXTRACTED 0.95]
- **Remove-a-wrong-bill decision across ADR, plan, schema and use cases** — docs_adr_0009_a_wrong_bill_is_removed_and_made_again, plan_remove_wrong_bill, database_bill, architecture_remove_bill_use_cases, architecture_undo_not_confirm [EXTRACTED 0.95]
- **Pending derived from bill lines and payments, never stored** — database_pending_derived, database_bill_line, database_payment, glossary_pending, plan_still_to_collect [EXTRACTED 0.90]

## Communities (182 total, 102 thin omitted)

### Community 0 - "Domain rules and tests"
Cohesion: 0.06
Nodes (8): MuteRemindersUseCase, BillingMonthTest, BuildBillUseCaseTest, ObserveBillTodosUseCaseTest, ObserveRenterLedgerUseCaseTest, ReminderPlannerTest, MuteRemindersUseCaseTest, ObserveRentIncreaseOffersUseCaseTest

### Community 1 - "Add renter screen and tests"
Cohesion: 0.06
Nodes (40): ObserveRenterFormUseCase, AddRenterIntent, AddCharge, AskToRaiseChanged, Back, BillsElectricityChanged, ChargeAmountChanged, ChargeLabelChanged (+32 more)

### Community 2 - "Create bill screen and tests"
Cohesion: 0.06
Nodes (20): MeterReadingParser, Candidate, SuggestMeterReadingUseCase, CreateBillIntent, Back, ChangeReading, ClearMeterPhoto, MeterReplacedChanged (+12 more)

### Community 3 - "Settings screen and tests"
Cohesion: 0.08
Nodes (18): SetReminderRepeatUseCase, StepReminderTimeUseCase, ReminderSyncTest, ReminderSettingsUseCasesTest, SettingsIntent, Back, BackUpNow, BackupShared (+10 more)

### Community 4 - "Outcome and Room repositories"
Cohesion: 0.06
Nodes (8): safeFlow(), BackupRepository, flatMap(), map(), Outcome, Success, FakeBackupRepository, FakeBackupRepository

### Community 5 - "Renter repository tests"
Cohesion: 0.10
Nodes (5): InsertedRenter, RenterDao, RoomRenterRepository, RoomRenterRepositoryTest, FixedCharge

### Community 6 - "Edit renter and validation"
Cohesion: 0.08
Nodes (4): UpdateRenterUseCase, UpdateRenterUseCaseTest, AddRenterViewModel, State

### Community 7 - "App settings storage"
Cohesion: 0.09
Nodes (11): AppSettingsEntity, SettingsDao, RoomSettingsRepository, toDomain(), toEntity(), RoomSettingsRepositoryTest, AppSettings, SettingsRepository (+3 more)

### Community 8 - "Shared UI components"
Cohesion: 0.09
Nodes (6): ActionRow(), MeterPhotoThumbnail(), MeterScanFlow(), StatusPill(), UndoBar(), RentManColors

### Community 9 - "Create bill UI steps"
Cohesion: 0.07
Nodes (14): BillCard(), CreateBillRoute(), CreateBillScreen(), LineRow(), Math(), MeterPhotoRow(), MeterReplaced(), ProblemText() (+6 more)

### Community 10 - "Settings and backup status"
Cohesion: 0.08
Nodes (12): BackupExport, BackupStatus, ExportBackupUseCase, MarkBackedUpUseCase, ObserveBackupStatusUseCase, RestoreBackupUseCase, UndoRestoreUseCase, ObserveSettingsUseCase (+4 more)

### Community 11 - "Backup file tests"
Cohesion: 0.12
Nodes (3): BackupCodecTest, RoomBackupRepositoryTest, Clock

### Community 12 - "App navigation and routes"
Cohesion: 0.06
Nodes (14): MainActivity, App(), AppNavigation(), Navigator, AddRenterKey, AddressPickerKey, CreateBillKey, EditRenterKey (+6 more)

### Community 13 - "Data module and database"
Cohesion: 0.07
Nodes (11): androidDataModule(), RoomAddressRepository, RoomBackupRepository, dataModule(), AppDispatchers, AddressDao, AddressEntity, AppDatabase (+3 more)

### Community 14 - "DomainError group"
Cohesion: 0.05
Nodes (37): BillField, NewMeterStart, Reading, DomainError, BackupFromNewerVersion, BillExists, BillNotRemovable, BlankName (+29 more)

### Community 15 - "Bill group"
Cohesion: 0.10
Nodes (14): toDomain(), Bill, BillId, BillLine, BillLineKind, Adjustment, Electricity, Rent (+6 more)

### Community 16 - "SaveRenterUseCaseTest group"
Cohesion: 0.10
Nodes (6): fold(), RenterFormValidator, RenterUnits, SaveRenterUseCase, toForm(), SaveRenterUseCaseTest

### Community 18 - "SettingsScreen.kt group"
Cohesion: 0.06
Nodes (8): ScreenTopBar(), BackedUpPreview(), Form(), rateProblem(), repeatLabel(), SettingsRoute(), SettingsScreen(), TimeStepper()

### Community 19 - "NotificationPermission.android.kt group"
Cohesion: 0.07
Nodes (8): readText(), rememberBackupFiles(), rememberPhotoImage(), findActivity(), notificationsAllowed(), openNotificationSettings(), rememberNotificationPermission(), rememberNotificationPermission()

### Community 21 - "HomeViewModelTest.kt group"
Cohesion: 0.13
Nodes (4): ChargeField, Amount, Label, Ravi

### Community 22 - "Paise group"
Cohesion: 0.10
Nodes (8): BillCalculator, MoneyInput, Paise, BillCalculatorTest, MoneyInputTest, formatRupees(), formatRupees(), formatRupees()

### Community 24 - "FakeRenterRepository group"
Cohesion: 0.16
Nodes (4): RemoveBillUseCase, ObserveHomeUseCaseTest, RemoveBillUseCaseTest, FakeRenterRepository

### Community 25 - "HomeScreen.kt group"
Cohesion: 0.09
Nodes (12): BillTodoCard(), Centered(), ContentBody(), EmptyPreview(), HomeRoute(), HomeScreen(), initials(), LeftRenterRow() (+4 more)

### Community 27 - "RoomBillRepositoryTest group"
Cohesion: 0.14
Nodes (3): RoomBillRepository, BillDao, RoomBillRepositoryTest

### Community 28 - "res group"
Cohesion: 0.07
Nodes (7): message(), messageResource(), modeLabel(), clockDigits(), isAfternoon(), timeLabel(), TimeTextTest

### Community 29 - "RecordPaymentScreen.kt group"
Cohesion: 0.09
Nodes (12): Divider(), Section(), SegmentButton(), Segmented(), StepButton(), TextRow(), DateStepper(), Form() (+4 more)

### Community 30 - "RenterDetailScreen.kt group"
Cohesion: 0.09
Nodes (11): BalanceCard(), BillRow(), Body(), EntryAction, EntryCard(), LeftRow(), PaymentRow(), RemindersRow() (+3 more)

### Community 31 - "AddressPickerScreen.kt group"
Cohesion: 0.10
Nodes (10): ActionButton(), CenteredMessage(), ErrorMessage(), AddressPickerRoute(), AddressPickerScreen(), AddressRow(), ContentBody(), ContentPreview() (+2 more)

### Community 32 - "FakeRenterRepository group"
Cohesion: 0.17
Nodes (4): RaiseRent, UndoRaise, HomeViewModelTest, FakeRenterRepository

### Community 33 - "RoomPaymentRepository.kt group"
Cohesion: 0.14
Nodes (4): PaymentDao, PaymentEntity, RoomPaymentRepository, RoomPaymentRepositoryTest

### Community 34 - "base group"
Cohesion: 0.06
Nodes (32): ENABLE_USER_SCRIPT_SANDBOXING, EXCLUDED_ARCHS[sdk=iphonesimulator*], FRAMEWORK_SEARCH_PATHS, OTHER_LDFLAGS, PRODUCT_BUNDLE_IDENTIFIER, SWIFT_VERSION, iOS, path (+24 more)

### Community 35 - "BillUseCases.kt group"
Cohesion: 0.13
Nodes (8): BillTodo, HomeSnapshot, ObserveBillContextUseCase, ObserveBillTodosUseCase, ObserveHomeUseCase, Lists, UnbilledMonths, RenterRepository

### Community 36 - "MeterPhotos.ios.kt group"
Cohesion: 0.14
Nodes (6): PhotoItem, PhotoPickerDelegate, PreviewSource, rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource()

### Community 37 - "RenterDetailViewModelTest group"
Cohesion: 0.22
Nodes (4): RemoveBill, RemovePayment, SetRemindersMuted, RenterDetailViewModelTest

### Community 38 - "Reminders.kt group"
Cohesion: 0.10
Nodes (7): ObserveReminderPlanUseCase, Reminder, CreateBills, RentPending, ReminderPlanner, ScheduledReminder, ReminderWorker

### Community 39 - "Payment group"
Cohesion: 0.15
Nodes (8): toDomain(), toEntity(), PaymentMapperTest, Payment, PaymentId, PaymentRepository, FakePaymentRepository, FakePaymentRepository

### Community 40 - "RenterLeavingUseCasesTest group"
Cohesion: 0.18
Nodes (3): MoveRenterBackUseCase, ObserveLeftRentersUseCase, RenterLeavingUseCasesTest

### Community 41 - "RecordPaymentViewModelTest group"
Cohesion: 0.16
Nodes (10): RecordPaymentIntent, AmountChanged, Back, DayStepped, ModeChanged, NoteChanged, Retry, Save (+2 more)

### Community 42 - "MeterCropper.kt group"
Cohesion: 0.10
Nodes (3): CropArea(), LightButton(), MeterCropper()

### Community 43 - "Modules.kt group"
Cohesion: 0.11
Nodes (6): RestoreBillUseCase, ObserveRenterLedgerUseCase, RemovePaymentUseCase, RestorePaymentUseCase, MarkRenterLeftUseCase, Clock

### Community 45 - "GetTodayUseCase group"
Cohesion: 0.17
Nodes (9): ApplyRentIncreaseUseCase, ObserveRentIncreaseOffersUseCase, PostponeRentIncreaseUseCase, RentIncreaseOffer, RentIncreaseRepository, UndoRentIncreaseUseCase, GetTodayUseCase, FakeRentIncreaseRepository (+1 more)

### Community 46 - "CropRect group"
Cohesion: 0.13
Nodes (11): Corner, BottomLeft, BottomRight, TopLeft, TopRight, CropRect, PixelBox, Grab (+3 more)

### Community 49 - "BuildFixedChargeUseCaseTest group"
Cohesion: 0.16
Nodes (4): BuildFixedChargeUseCase, PhoneNumber, BuildFixedChargeUseCaseTest, PhoneNumberTest

### Community 50 - "RentChangeEntity group"
Cohesion: 0.20
Nodes (3): RentChangeDao, RentChangeEntity, toEntity()

### Community 51 - "safeCall() group"
Cohesion: 0.31
Nodes (3): RoomRentIncreaseRepository, safeCall(), RoomRentIncreaseRepositoryTest

### Community 55 - "MeterPhotos.kt group"
Cohesion: 0.18
Nodes (12): PhotoFiles, PhotoOpener, PhotoSource, readWithin(), rememberPhotoFiles(), rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource() (+4 more)

### Community 56 - "Data layer (:data, Room 3, internal) group"
Cohesion: 0.12
Nodes (15): ADR 0007 (errors as values), Data layer (:data, Room 3, internal), Domain layer (:domain, pure Kotlin), Koin DI wiring, MviViewModel base class (toState, Eagerly shared), Navigation 3 (NavKey, NavDisplay, NavEvent), Outcome<T> errors as values, Paise value class (money as Long) (+7 more)

### Community 57 - "Context group"
Cohesion: 0.29
Nodes (4): MeterPhotoFiles, rememberPhotoFiles(), rememberPhotoOpener(), rememberPhotoSource()

### Community 58 - "RenterId group"
Cohesion: 0.24
Nodes (7): toDomain(), toDomain(), RenterMapperTest, FixedCharge, FixedChargeId, Renter, RenterId

### Community 59 - "BillContext group"
Cohesion: 0.23
Nodes (4): BillMessage, BillContext, BuildBillUseCase, BillMessageTest

### Community 60 - "SettingsField group"
Cohesion: 0.13
Nodes (16): SettingsField, Rate, Repeat, Time, SettingsForm, BackupMessage, Restored, RestoreUndone (+8 more)

### Community 62 - "ComposeView group"
Cohesion: 0.14
Nodes (7): ComposeView, ContentView, .body, iOSApp, .body, sharedUI, SwiftUI

### Community 64 - "AddressId group"
Cohesion: 0.14
Nodes (15): toDomain(), Address, AddressId, RenterForm, AddressPickerIntent, Confirm, NameChanged, Retry (+7 more)

### Community 65 - "RentChange group"
Cohesion: 0.22
Nodes (4): RentChangeMapperTest, RentChange, RentChangeId, FakeRentIncreaseRepository

### Community 66 - "PaymentMode group"
Cohesion: 0.14
Nodes (15): overpayment(), PaymentField, Amount, Date, Note, PaymentForm, PaymentMode, Bank (+7 more)

### Community 67 - "PaymentUseCases.kt group"
Cohesion: 0.22
Nodes (6): LedgerEntry, BillEntry, PaymentEntry, Movement, RenterLedger, entryKey()

### Community 68 - "AddressRepository group"
Cohesion: 0.18
Nodes (4): AddressRepository, AddAddressUseCase, ObserveAddressesUseCase, FakeAddressRepository

### Community 70 - "HomeIntent group"
Cohesion: 0.15
Nodes (13): HomeIntent, AddRenter, CreateBill, NotNow, OpenRenter, OpenSettings, Refresh, Retry (+5 more)

### Community 71 - "RenterEntity group"
Cohesion: 0.23
Nodes (4): FixedChargeEntity, RenterWithCharges, RenterEntity, toEntity()

### Community 78 - "ObserveRenterSummariesUseCase group"
Cohesion: 0.26
Nodes (3): ObserveRenterSummariesUseCase, FakeAddressRepository, ObserveRenterSummariesUseCaseTest

### Community 79 - "ReminderText.kt group"
Cohesion: 0.16
Nodes (4): label(), shortLabel(), ReminderText, text()

### Community 80 - "renter table group"
Cohesion: 0.18
Nodes (14): address table, fixed_charge table, rent_change table, renter table, Add renter design, Address picker design, Address filter chips, Address (+6 more)

### Community 82 - "RecordPaymentViewModel group"
Cohesion: 0.41
Nodes (3): RecordPaymentViewModel, State, Reducer

### Community 84 - "Backup file (JSON, BackupCodec, GoldenBackups) group"
Cohesion: 0.15
Nodes (13): Platform pieces (photo, backup files, notifications) as UI-only expect/actual, ReminderSync resync model (plan, scheduler), app_settings table (single row), Backup file (JSON, BackupCodec, GoldenBackups), backup_snapshot table (Undo last restore), Meter photo file name (meter_photos folder), Reminder mute (remindersMutedMonth), Stop this month button (+5 more)

### Community 85 - "ReminderScheduler group"
Cohesion: 0.27
Nodes (6): ReminderScheduler, ReminderSync, startRentManKoin(), AndroidReminderScheduler, sharedModules(), startRentManKoin()

### Community 86 - "BillLineEntity group"
Cohesion: 0.35
Nodes (5): toEntity(), BillEntity, BillLineEntity, BillWithLines, sampleBackup()

### Community 88 - "Pending (derived balance, PendingCalculator) group"
Cohesion: 0.20
Nodes (12): Ledger (bills and payments merged by date), Pending (derived balance, PendingCalculator), Renter status (No bill, Paid, Late, Part paid, Unpaid), Record payment design, Renter ledger design, ADR 0001 Pending is computed, never stored, Paid ahead, Payment (+4 more)

### Community 92 - "RenterRow group"
Cohesion: 0.29
Nodes (6): RenterRow, StillToCollect, HomeUiState, Content, Error, Loading

### Community 96 - "bill table group"
Cohesion: 0.24
Nodes (7): bill table, bill_line table, payment table (voidable), ADR 0009: A wrong bill is removed and made again, ADR 0002 (bill is a snapshot, never edited), Ideas not building now (Money page, FY statement, deposit), Remove a wrong bill (newest bill only, Undo)

### Community 99 - "Bill line group"
Cohesion: 0.27
Nodes (10): Fixed monthly charges list, Bill preview design, Meter reading design, ADR 0002 Bills are immutable snapshots, Adjustment, Bill, Bill line, Billing month (+2 more)

### Community 100 - "RenterField group"
Cohesion: 0.20
Nodes (10): RenterField, Address, Charge, DueDay, Name, OwnRate, Phone, Rent (+2 more)

### Community 102 - "ARCHITECTURE.md (engineering contract) group"
Cohesion: 0.36
Nodes (7): ARCHITECTURE.md (engineering contract), Workflow for every change (ponytail, review, graphify), DATABASE.md (schema v1), Home design, GLOSSARY.md (vocabulary), PLAN.md (product plan), Design mockup (design/project)

### Community 106 - "NotificationPermission.kt group"
Cohesion: 0.36
Nodes (4): AskForNotificationsOnce(), NotificationPermission, rememberNotificationPermission(), rememberNotificationPermission()

### Community 107 - "RenterDetailIntent group"
Cohesion: 0.22
Nodes (9): RenterDetailIntent, Back, Edit, MarkLeft, MoveBack, RecordPayment, Retry, UndoRemove (+1 more)

### Community 113 - "BackupFiles group"
Cohesion: 0.43
Nodes (3): BackupFiles, rememberBackupFiles(), rememberBackupFiles()

### Community 116 - "Creating bills flow (reading, preview, send) group"
Cohesion: 0.40
Nodes (5): Unbilled months, ADR 0008 The landlord creates every bill, Unbilled month, Creating bills flow (reading, preview, send), WhatsApp bill (wa.me link, text only)

### Community 117 - "RenterStatus group"
Cohesion: 0.33
Nodes (6): RenterStatus, Late, NoBillYet, Paid, PartPaid, Unpaid

### Community 120 - "FieldProblem group"
Cohesion: 0.40
Nodes (4): FieldProblem, Duplicate, Invalid, Required

### Community 124 - "RenterDetailNavEvent group"
Cohesion: 0.50
Nodes (4): RenterDetailNavEvent, Close, OpenEdit, OpenRecordPayment

### Community 126 - "RecordPaymentNavEvent group"
Cohesion: 0.67
Nodes (3): RecordPaymentNavEvent, Close, Saved

## Knowledge Gaps
- **151 isolated node(s):** `Back`, `Retry`, `UseFullAmount`, `Retry`, `Close` (+146 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 621 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **102 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Outcome` connect `Outcome and Room repositories` to `Domain rules and tests`, `Create bill screen and tests`, `Settings screen and tests`, `Renter repository tests`, `Edit renter and validation`, `App settings storage`, `Create bill UI steps`, `Settings and backup status`, `Data module and database`, `DomainError group`, `Bill group`, `SaveRenterUseCaseTest group`, `localdate group`, `HomeViewModelTest.kt group`, `Paise group`, `FakeRenterRepository group`, `BackupDao group`, `RoomBillRepositoryTest group`, `FakeRenterRepository group`, `RoomPaymentRepository.kt group`, `BillUseCases.kt group`, `Reminders.kt group`, `Payment group`, `GetTodayUseCase group`, `BuildFixedChargeUseCaseTest group`, `safeCall() group`, `RecordPaymentViewModel.kt group`, `RentChange group`, `PaymentUseCases.kt group`, `AddressRepository group`, `IosReminderScheduler.kt group`, `RecordPaymentUseCaseTest group`, `BackupCodec group`, `ObserveRenterSummariesUseCase group`, `BackupCodec.kt group`, `SafeTest.kt group`, `BillingMonth group`, `.invoke() group`, `ReminderPlan group`, `AndroidReminderScheduler.kt group`, `FieldProblem group`?**
  _High betweenness centrality (0.188) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Paise` (e.g. with `.aRateIsAtMostNinetyNineRupeesSoAMissingDecimalPointIsCaught()` and `.parsesAPaymentWithUpToTwoDecimals()`) actually correct?**
  _`Paise` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Back`, `Retry`, `UseFullAmount` to the rest of the system?**
  _151 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Domain rules and tests` be split into smaller, more focused modules?**
  _Cohesion score 0.06000643500643501 - nodes in this community are weakly interconnected._
- **Why does `DomainError` connect `DomainError group` to `Add renter screen and tests`, `Create bill screen and tests`, `Outcome and Room repositories`, `Edit renter and validation`, `Create bill UI steps`, `Settings and backup status`, `Bill group`, `SaveRenterUseCaseTest group`, `AddRenterScreen.kt group`, `SettingsScreen.kt group`, `localdate group`, `HomeViewModelTest.kt group`, `HomeScreen.kt group`, `res group`, `RecordPaymentScreen.kt group`, `RenterDetailScreen.kt group`, `AddressPickerScreen.kt group`, `BillUseCases.kt group`, `Payment group`, `Modules.kt group`, `HomeViewModel.kt group`, `GetTodayUseCase group`, `RecordPaymentViewModel.kt group`, `SettingsField group`, `RenterDetailViewModel group`, `AddressId group`, `RentChange group`, `PaymentMode group`, `PaymentUseCases.kt group`, `IosReminderScheduler.kt group`, `RecordPaymentViewModel group`, `BackupCodec.kt group`, `SafeTest.kt group`, `RenterRow group`, `AddressPickerViewModel group`, `AndroidReminderScheduler.kt group`, `FieldProblem group`?**
  _High betweenness centrality (0.153) - this node is a cross-community bridge._
- **Are the 13 inferred relationships involving `RenterId` (e.g. with `.aRenterWithNoBillHasNothingToMuteAndNothingChanges()` and `.aStorageFailureIsReported()`) actually correct?**
  _`RenterId` has 13 INFERRED edges - model-reasoned connections that need verification._
- **Should `Add renter screen and tests` be split into smaller, more focused modules?**
  _Cohesion score 0.055593685655456415 - nodes in this community are weakly interconnected._