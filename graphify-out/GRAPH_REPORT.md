# Graph Report - RentMan  (2026-11-27)

## Corpus Check
- 307 files · ~147,721 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 16 file(s) not represented in the graph (top: .xml 9, .properties 3, (none) 1)

## Summary
- 4048 nodes · 12901 edges · 189 communities (87 shown, 102 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 651 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `cfe265de`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Renter
- MainActivity.kt
- Screen Test Helpers
- AddRenterViewModelTest
- RenterLeavingUseCasesTest
- AddressPickerViewModelTest
- .useCase
- safeCall
- Create Bill Screen
- Rent Increase and Billing Month
- .viewModel
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
- RenterId
- CreateBillViewModel.kt
- .viewModel
- BackupDao
- SettingsScreen.kt
- RenterDetailScreen.kt
- AppSettings
- Modules.kt
- IosMeterPhotoFiles.kt
- RentChangeEntity
- getvalue
- AddressPickerScreen.kt
- Reminders.kt
- FakeRenterRepository
- KeychainTokenStore.kt
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
- GoogleDriveRepositoryTest
- CropRect
- MeterPhotoFiles.kt
- IosReminderScheduler.kt
- SaveRenterUseCaseTest
- BackupFiles.ios.kt
- DriveSignIn.ios.kt
- FieldProblem
- RenterId
- ReminderSyncTest
- BillLineEntity
- MeterReadingParserTest
- .summary
- ReminderScheduler
- HomeViewModelTest.kt
- MeterPhotos.kt
- RoomBillRepositoryTest
- FakeBillRepository
- ComposeView
- MviViewModel base class (toState, Eagerly)
- MviViewModel
- Outcome
- .renter
- bill table
- MessageEditScreen.kt
- ReminderNotifications.kt
- RenterEntity
- ObserveRenterSummariesUseCase
- MeterCropper.kt
- BackupCodec.kt
- PaymentEntity
- HomeViewModel
- ReminderActionReceiver.kt
- Pending / balance derivation (PendingCalculator)
- sampleBackup
- renter table
- MessageKind
- PhotoFilesTest.kt
- .of
- SettingsIntent
- Payment
- DriveBackupScreen.kt
- BillingMonthTest
- MeterPhotoStore
- FakeBillRepository
- DriveBackupViewModelTest.kt
- RoomRenterRepositoryTest
- ARCHITECTURE.md (engineering contract)
- .months
- NotificationPermission.kt
- DriveUseCasesTest
- Address (glossary term)
- GoogleDriveRepository
- AddressEntity
- MeterTextTest
- MonthsViewModelTest
- GoogleAuth.kt
- Pending (glossary term)
- GoogleDriveRepositoryTest.kt
- BackupFiles
- RoomSettingsRepository.kt
- Bill line (glossary term)
- dataModule
- .normalize
- RentManApplication.kt
- Reminder mute (remindersMutedMonth)
- Backup file JSON and restore transaction (BackupCodec)
- UpdateRenterUseCase
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
- DriveBackupWorker.kt
- .invoke
- MonthsViewModel
- AppNavigation.kt
- RenterMonthTest
- FakeRenterRepository
- Migrations.kt
- CurrencyFormat.ios.kt
- PaymentUseCases.kt
- BillMapperTest
- Corner
- FakeBillRepository
- RenterSummary
- DriveBackupViewModelTest
- CreateBillViewModel
- PlayServicesAuth
- RoomAddressRepositoryTest
- MeterReadingParser
- FakeDriveBackupRepository
- BackupUseCasesTest
- TextPiece
- FakeSettingsRepository
- MviViewModel.kt
- DriveBackupStatus
- StatusPill.kt
- ReminderNotificationDelegate.kt
- MonthDetailIntent
- DriveBackupViewModel
- SaveSettingsUseCaseTest
- BuildFixedChargeUseCaseTest
- MonthDetailViewModel
- DriveBackupIntent
- AddAddressUseCaseTest
- MeterReadingParser

## God Nodes (most connected - your core abstractions)
1. `Outcome` - 305 edges
2. `RenterId` - 234 edges
3. `Paise` - 220 edges
4. `DomainError` - 177 edges
5. `BillingMonth` - 137 edges
6. `Payment` - 84 edges
7. `Renter` - 79 edges
8. `RenterSummary` - 71 edges
9. `AppSettings` - 70 edges
10. `FakeRenterRepository` - 70 edges

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
  sharedUI/src/commonMain/kotlin/com/rentman/sharedui/feature/drive/DriveBackupContract.kt → domain/src/commonMain/kotlin/com/rentman/domain/Outcome.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Bill immutability with the remove-and-remake exception** — docs_adr_0002_bills_are_immutable_snapshots, docs_adr_0009_a_wrong_bill_is_removed_and_made_again, database_bill_table, plan_remove_wrong_bill_feature, database_money_history_immutable [EXTRACTED 1.00]
- **MVI screen contract: UiState, NavEvent and MviViewModel** — architecture_mviviewmodel, architecture_uistate, architecture_navevent, architecture_dumb_ui [EXTRACTED 1.00]
- **Pending is derived from bill lines and payments, never stored** — database_pending_calculation, database_bill_line_table, database_payment_table, database_derived_not_stored, glossary_pending [EXTRACTED 1.00]

## Communities (189 total, 102 thin omitted)

### Community 0 - "Renter"
Cohesion: 0.09
Nodes (17): toDomain(), RenterMapperTest, InvalidCharge, Address, AddressId, ChargeField, Amount, Label (+9 more)

### Community 2 - "Screen Test Helpers"
Cohesion: 0.13
Nodes (7): RoomAddressRepository, PaymentMode, Bank, Cash, Upi, TestData, Ravi

### Community 3 - "AddRenterViewModelTest"
Cohesion: 0.10
Nodes (24): AddCharge, AddRenterIntent, AddRenterNavEvent, AskToRaiseChanged, Back, BillsElectricityChanged, ChargeAmountChanged, ChargeLabelChanged (+16 more)

### Community 5 - "AddressPickerViewModelTest"
Cohesion: 0.09
Nodes (14): Add, AddressPickerIntent, AddressPickerNavEvent, AddressPickerUiState, Confirm, Content, Error, Loading (+6 more)

### Community 7 - "safeCall"
Cohesion: 0.06
Nodes (9): toDomain(), RoomBackupRepository, RoomBillRepository, RoomRenterRepository, safeCall(), safeFlow(), GoldenBackupTest, SafeTest (+1 more)

### Community 8 - "Create Bill Screen"
Cohesion: 0.06
Nodes (14): BillCard(), CreateBillRoute(), CreateBillScreen(), LineRow(), Math(), MeterPhotoRow(), MeterReplaced(), ProblemText() (+6 more)

### Community 9 - "Rent Increase and Billing Month"
Cohesion: 0.10
Nodes (8): toDomain(), RoomRentIncreaseRepository, RentChangeMapperTest, RentChange, RentChangeId, FakeRentIncreaseRepository, RentIncreaseUseCasesTest, FakeRentIncreaseRepository

### Community 10 - ".viewModel"
Cohesion: 0.06
Nodes (27): Back, Close, Content, Edit, Error, Loading, MarkLeft, MoveBack (+19 more)

### Community 11 - "Add Renter Screen"
Cohesion: 0.05
Nodes (8): AddRenterRoute(), AddRenterScreen(), AddressRow(), chargeProblem(), ChargeRow(), FilledWithProblemsPreview(), problem(), SwitchRow()

### Community 13 - "Home Screen"
Cohesion: 0.05
Nodes (23): formatRupees(), label(), shortLabel(), shortName(), message(), messageResource(), UndoBar(), amountStyle() (+15 more)

### Community 14 - ".viewModel"
Cohesion: 0.19
Nodes (6): RateChanged, RepeatChanged, RestoreFrom, TimeStepped, FakeSettingsRepository, SettingsViewModelTest

### Community 18 - "MessageEditViewModelTest"
Cohesion: 0.14
Nodes (8): Back, InsertVariable, MessageEditIntent, Retry, Save, TextChanged, UseOurWording, MessageEditViewModelTest

### Community 19 - "BuildBillUseCaseTest"
Cohesion: 0.07
Nodes (19): BillMessage, BillContext, BuildBillUseCase, BillMessageTest, BuildBillUseCaseTest, BillStep, Reading, Review (+11 more)

### Community 20 - "Paise"
Cohesion: 0.07
Nodes (10): BillCalculator, Paise, RentHistory, BillCalculatorTest, PaiseTest, PaymentFormTest, RentHistoryTest, formatRupees() (+2 more)

### Community 21 - "RenterId"
Cohesion: 0.09
Nodes (17): toDomain(), Bill, BillId, BillLine, BillLineKind, Adjustment, Electricity, FixedCharge (+9 more)

### Community 22 - "CreateBillViewModel.kt"
Cohesion: 0.33
Nodes (4): ObserveMessageUseCase, fold(), flatMapFirst(), MviViewModel

### Community 23 - ".viewModel"
Cohesion: 0.12
Nodes (12): Back, ChangeReading, ClearMeterPhoto, CreateBillIntent, MeterReplacedChanged, MeterScanned, NewMeterStartChanged, ReadingChanged (+4 more)

### Community 25 - "SettingsScreen.kt"
Cohesion: 0.04
Nodes (21): Divider(), Section(), SegmentButton(), Segmented(), StepButton(), TextRow(), DateStepper(), Form() (+13 more)

### Community 26 - "RenterDetailScreen.kt"
Cohesion: 0.07
Nodes (15): LedgerEntry, PaymentEntry, BalanceText(), BalanceCard(), BillRow(), Body(), EntryAction, EntryCard() (+7 more)

### Community 28 - "Modules.kt"
Cohesion: 0.10
Nodes (15): BackupExport, BackupRepository, ExportBackupUseCase, MarkBackedUpUseCase, ObserveBackupStatusUseCase, RestoreBackupUseCase, UndoRestoreUseCase, ObserveReminderPlanUseCase (+7 more)

### Community 30 - "RentChangeEntity"
Cohesion: 0.20
Nodes (3): RentChangeDao, RentChangeEntity, toEntity()

### Community 31 - "getvalue"
Cohesion: 0.07
Nodes (9): GoogleClient, readText(), rememberBackupFiles(), rememberDriveSignIn(), findActivity(), notificationsAllowed(), openNotificationSettings(), rememberNotificationPermission() (+1 more)

### Community 32 - "AddressPickerScreen.kt"
Cohesion: 0.10
Nodes (9): ActionButton(), TextAction(), AddressPickerRoute(), AddressPickerScreen(), AddressRow(), ContentBody(), ContentPreview(), NewAddress() (+1 more)

### Community 34 - "FakeRenterRepository"
Cohesion: 0.13
Nodes (6): NotNow, RaiseRent, SelectFilter, UndoRaise, HomeViewModelTest, FakeRenterRepository

### Community 36 - "base"
Cohesion: 0.06
Nodes (32): ENABLE_USER_SCRIPT_SANDBOXING, EXCLUDED_ARCHS[sdk=iphonesimulator*], FRAMEWORK_SEARCH_PATHS, OTHER_LDFLAGS, PRODUCT_BUNDLE_IDENTIFIER, SWIFT_VERSION, iOS, path (+24 more)

### Community 37 - "MoneyInput"
Cohesion: 0.10
Nodes (12): MoneyInput, SettingsForm, toForm(), MoneyInputTest, BackupMessage, Restored, RestoreUndone, ShareFailed (+4 more)

### Community 39 - "MeterPhotos.ios.kt"
Cohesion: 0.13
Nodes (6): PhotoItem, PhotoPickerDelegate, PreviewSource, rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource()

### Community 42 - "HomeIntent"
Cohesion: 0.28
Nodes (9): AddRenter, HomeIntent, HomeNavEvent, OpenAddRenter, OpenMonths, OpenRenter, OpenSettings, Refresh (+1 more)

### Community 43 - "RecordPaymentViewModelTest"
Cohesion: 0.15
Nodes (9): AmountChanged, Back, DayStepped, ModeChanged, NoteChanged, RecordPaymentIntent, Retry, UseFullAmount (+1 more)

### Community 44 - "res"
Cohesion: 0.11
Nodes (6): kindLabel(), modeLabel(), clockDigits(), isAfternoon(), timeLabel(), TimeTextTest

### Community 45 - "MonthDetailScreen.kt"
Cohesion: 0.07
Nodes (14): AmountText(), LegendRow(), MonthBar(), MonthCard(), CashInRow(), ContentPreview(), MonthDetailRoute(), MonthDetailScreen() (+6 more)

### Community 47 - "AddRenterViewModelTest.kt"
Cohesion: 0.16
Nodes (3): ObserveAddressesUseCase, BuildFixedChargeUseCase, ObserveRenterFormUseCase

### Community 48 - "MeterPhotoFiles"
Cohesion: 0.18
Nodes (5): MeterPhotoFiles, rememberPhotoFiles(), rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource()

### Community 49 - "GoogleDriveRepositoryTest"
Cohesion: 0.16
Nodes (7): FakeGoogle, GoogleDriveRepositoryTest, Item, MemoryLog, MemoryTokens, Photos, Backup to the landlord's own Google Drive

### Community 51 - "MeterPhotoFiles.kt"
Cohesion: 0.09
Nodes (3): AppDispatchers, MeterPhotoProvider, StringResourcesTest

### Community 55 - "DriveSignIn.ios.kt"
Cohesion: 0.10
Nodes (3): GoogleSignIn, rememberDriveSignIn(), topViewController()

### Community 56 - "FieldProblem"
Cohesion: 0.08
Nodes (23): BillField, NewMeterStart, Reading, InvalidBill, InvalidPayment, InvalidSettings, overpayment(), PaymentField (+15 more)

### Community 59 - "BillLineEntity"
Cohesion: 0.18
Nodes (5): toEntity(), BillDao, BillEntity, BillLineEntity, BillWithLines

### Community 62 - "ReminderScheduler"
Cohesion: 0.16
Nodes (7): iosDataModule(), ReminderScheduler, startRentManKoin(), AndroidReminderScheduler, sharedModules(), IosMeterPhotoStore, startRentManKoin()

### Community 63 - "HomeViewModelTest.kt"
Cohesion: 0.13
Nodes (9): BillRepository, ObserveBillTodosUseCase, ObserveHomeUseCase, ApplyRentIncreaseUseCase, ObserveRentIncreaseOffersUseCase, PostponeRentIncreaseUseCase, RentIncreaseRepository, UndoRentIncreaseUseCase (+1 more)

### Community 64 - "MeterPhotos.kt"
Cohesion: 0.18
Nodes (12): PhotoFiles, PhotoOpener, PhotoSource, readWithin(), rememberPhotoFiles(), rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource() (+4 more)

### Community 66 - "FakeBillRepository"
Cohesion: 0.18
Nodes (5): CreateBillUseCase, MuteRemindersUseCase, CreateBillUseCaseTest, FakeBillRepository, MuteRemindersUseCaseTest

### Community 67 - "ComposeView"
Cohesion: 0.14
Nodes (7): ComposeView, ContentView, .body, iOSApp, .body, sharedUI, SwiftUI

### Community 68 - "MviViewModel base class (toState, Eagerly)"
Cohesion: 0.14
Nodes (14): Data layer (:data, Room 3), Domain layer (:domain), formatRupees expect/actual (Indian grouping), Koin dependency injection, AGP 9 KMP module structure (androidApp, iosApp, sharedUI, data, domain), MviViewModel base class (toState, Eagerly), NavEvent one-shot SharedFlow, Navigation 3 (NavKey, NavDisplay) (+6 more)

### Community 69 - "MviViewModel"
Cohesion: 0.07
Nodes (17): InvalidRenter, RenterField, Address, Charge, DueDay, Name, OwnRate, Phone (+9 more)

### Community 70 - "Outcome"
Cohesion: 0.04
Nodes (22): BackupFromNewerVersion, BillExists, BillNotRemovable, BlankName, DomainError, DriveNotConnected, DriveUnavailable, DuplicateName (+14 more)

### Community 72 - "bill table"
Cohesion: 0.15
Nodes (10): Platform gateways and UI-only expect/actual (photo, backup files), bill table, Ledger (bills and payments merged), payment table (voidable), ADR 0002: Bills are immutable snapshots made of lines, ADR 0009: A wrong bill is removed and made again, Adjustment (glossary term), Electricity rate and meter reading rules (+2 more)

### Community 73 - "MessageEditScreen.kt"
Cohesion: 0.15
Nodes (7): Editor(), Label(), MessageEditRoute(), MessageEditScreen(), MessageField(), UnknownVariablePreview(), VariableChip()

### Community 75 - "RenterEntity"
Cohesion: 0.13
Nodes (6): FixedChargeEntity, RenterWithCharges, InsertedRenter, RenterDao, RenterEntity, toEntity()

### Community 76 - "ObserveRenterSummariesUseCase"
Cohesion: 0.10
Nodes (17): MonthDetail, MonthsInput, ObserveMonthDetailUseCase, observeMonthsInput(), ObserveMonthsUseCase, isNewestOf(), RestoreBillUseCase, flatMap() (+9 more)

### Community 77 - "MeterCropper.kt"
Cohesion: 0.05
Nodes (10): ActionRow(), CropArea(), LightButton(), MeterCropper(), MeterPhotoThumbnail(), MeterScanFlow(), Working(), ScreenTopBar() (+2 more)

### Community 79 - "PaymentEntity"
Cohesion: 0.21
Nodes (3): PaymentDao, PaymentEntity, toEntity()

### Community 82 - "Pending / balance derivation (PendingCalculator)"
Cohesion: 0.15
Nodes (11): Paise value class (money as Long), bill_line table, Pending / balance derivation (PendingCalculator), Renter status derivation (No bill, Paid, Late, Part paid, Unpaid), Due day (glossary term), Late (glossary term), Not due yet (glossary term), Part paid and Unpaid statuses (glossary terms) (+3 more)

### Community 85 - "renter table"
Cohesion: 0.17
Nodes (12): address table, fixed_charge table, rent_change table, renter table, Unbilled months derivation, ADR 0008 The landlord creates every bill, Remade bill uses rent of its own month (RentHistory.rentFor), Rent increase (glossary term) (+4 more)

### Community 86 - "MessageKind"
Cohesion: 0.08
Nodes (16): MonthReviewMessage, MessageKind, Bill, MonthReview, PaymentReceived, MessageTemplate, PaymentMessage, MonthReviewMessageTest (+8 more)

### Community 89 - "SettingsIntent"
Cohesion: 0.17
Nodes (13): Back, BackUpNow, BackupShared, BackupShareFailed, Close, OpenDrive, OpenMessage, Retry (+5 more)

### Community 90 - "Payment"
Cohesion: 0.08
Nodes (8): toDomain(), RoomPaymentRepository, PaymentMapperTest, RoomPaymentRepositoryTest, RenterMonth, Payment, PaymentId, previewRow()

### Community 91 - "DriveBackupScreen.kt"
Cohesion: 0.09
Nodes (8): CenteredMessage(), ErrorMessage(), Body(), ConnectedPreview(), DisconnectRow(), DriveBackupRoute(), DriveBackupScreen(), text()

### Community 92 - "BillingMonthTest"
Cohesion: 0.15
Nodes (3): MeterPhotoName, BillingMonthTest, MeterPhotoNameTest

### Community 93 - "MeterPhotoStore"
Cohesion: 0.08
Nodes (5): DriveStatus, MeterPhotoStore, AndroidMeterPhotoStore, FakeDriveBackupRepository, NoPhotos

### Community 95 - "DriveBackupViewModelTest.kt"
Cohesion: 0.12
Nodes (8): AutoBackUpToDriveUseCase, BackUpToDriveUseCase, ConnectDriveUseCase, DisconnectDriveUseCase, DriveBackupRepository, GetDriveStatusUseCase, RestoreFromDriveUseCase, RenterRepository

### Community 97 - "ARCHITECTURE.md (engineering contract)"
Cohesion: 0.36
Nodes (7): ARCHITECTURE.md (engineering contract), Workflow for every change (ponytail, review, graphify), DATABASE.md (schema v1), Home design, GLOSSARY.md (ubiquitous language), PLAN.md (product plan), Claude Design mockup (design/project)

### Community 99 - "NotificationPermission.kt"
Cohesion: 0.36
Nodes (4): AskForNotificationsOnce(), NotificationPermission, rememberNotificationPermission(), rememberNotificationPermission()

### Community 101 - "Address (glossary term)"
Cohesion: 0.29
Nodes (8): Add renter design, Address picker design, Address filter chips, Address (glossary term), Left renter (glossary term), Renter (glossary term), Unit (glossary term), Renter edit and mark left feature

### Community 102 - "GoogleDriveRepository"
Cohesion: 0.19
Nodes (3): DriveFile, FileList, GoogleDriveRepository

### Community 106 - "GoogleAuth.kt"
Cohesion: 0.13
Nodes (4): NotConnected, RefreshTokenAuth, TokenReply, TokenStore

### Community 107 - "Pending (glossary term)"
Cohesion: 0.29
Nodes (7): Record payment design, Renter ledger design, ADR 0001 Pending is computed, never stored, Paid ahead (glossary term), Payment (glossary term), Pending (glossary term), WhatsApp bill message (wa.me link)

### Community 108 - "GoogleDriveRepositoryTest.kt"
Cohesion: 0.10
Nodes (5): Code, DriveSignIn, Granted, rememberDriveSignIn(), rememberDriveSignIn()

### Community 109 - "BackupFiles"
Cohesion: 0.43
Nodes (3): BackupFiles, rememberBackupFiles(), rememberBackupFiles()

### Community 110 - "RoomSettingsRepository.kt"
Cohesion: 0.19
Nodes (4): RoomSettingsRepository, toDomain(), toEntity(), MigrationTest

### Community 111 - "Bill line (glossary term)"
Cohesion: 0.40
Nodes (6): Fixed monthly charges list, Bill preview design, Meter reading design, Bill (glossary term), Bill line (glossary term), Fixed charge (glossary term)

### Community 112 - "dataModule"
Cohesion: 0.10
Nodes (5): dataModule(), AppDispatchers, GoogleAuth, UploadLog, DefaultsUploadLog

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
Nodes (15): AppNavigation(), Navigator, AddRenterKey, AddressPickerKey, CreateBillKey, DriveKey, EditRenterKey, HomeKey (+7 more)

### Community 151 - "FakeRenterRepository"
Cohesion: 0.19
Nodes (5): RemoveBillUseCase, RemoveBillUseCaseTest, FakeAddressRepository, FakeRenterRepository, ObserveRenterSummariesUseCaseTest

### Community 155 - "PaymentUseCases.kt"
Cohesion: 0.12
Nodes (10): BillEntry, Movement, ObserveRenterLedgerUseCase, RecordPaymentUseCase, RemovePaymentUseCase, RenterLedger, RestorePaymentUseCase, AddressRepository (+2 more)

### Community 157 - "Corner"
Cohesion: 0.13
Nodes (9): Corner, BottomLeft, BottomRight, TopLeft, TopRight, PixelBox, Edge, Grab (+1 more)

### Community 159 - "RenterSummary"
Cohesion: 0.12
Nodes (19): BillTodo, HomeSnapshot, Lists, ObserveBillContextUseCase, RenterRow, RenterStatus, Late, NoBillYet (+11 more)

### Community 162 - "PlayServicesAuth"
Cohesion: 0.17
Nodes (3): androidDataModule(), PlayServicesAuth, PreferencesUploadLog

### Community 172 - "TextPiece"
Cohesion: 0.30
Nodes (11): around(), digitRows(), digitsOf(), findMeters(), fit(), MeterSpot, readingLines(), runsOf() (+3 more)

### Community 173 - "FakeSettingsRepository"
Cohesion: 0.25
Nodes (4): SaveMessageUseCase, FakeSettingsRepository, MessageUseCasesTest, ReminderSettingsUseCasesTest

### Community 175 - "DriveBackupStatus"
Cohesion: 0.15
Nodes (14): DriveBackupStatus, Close, Content, DriveBackupNavEvent, DriveBackupUiState, DriveMessage, BackedUp, Connected (+6 more)

### Community 176 - "StatusPill.kt"
Cohesion: 0.20
Nodes (4): colors(), label(), StatusColors, StatusPill()

### Community 179 - "MonthDetailIntent"
Cohesion: 0.18
Nodes (11): Back, Close, Content, Error, Loading, MonthDetailIntent, MonthDetailNavEvent, MonthDetailUiState (+3 more)

### Community 184 - "DriveBackupIntent"
Cohesion: 0.22
Nodes (9): Back, BackUpNow, Disconnect, DriveBackupIntent, Restore, Retry, SignedIn, SignInFailed (+1 more)

## Knowledge Gaps
- **150 isolated node(s):** `UnitTaken`, `InvalidBackup`, `DriveNotConnected`, `NothingToBackUp`, `DriveUnavailable` (+145 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 840 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **102 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `RenterId` connect `RenterId` to `Renter`, `Screen Test Helpers`, `AddRenterViewModelTest`, `RenterLeavingUseCasesTest`, `.useCase`, `safeCall`, `Create Bill Screen`, `Rent Increase and Billing Month`, `.viewModel`, `Add Renter Screen`, `RoomRentIncreaseRepositoryTest`, `Home Screen`, `RecordPaymentUseCaseTest`, `.encode`, `.useCase`, `BuildBillUseCaseTest`, `Paise`, `AppNavigation.kt`, `RenterMonthTest`, `FakeRenterRepository`, `CreateBillViewModel.kt`, `SettingsScreen.kt`, `RenterDetailScreen.kt`, `PaymentUseCases.kt`, `BillMapperTest`, `Modules.kt`, `.viewModel`, `RenterSummary`, `Reminders.kt`, `CreateBillViewModel`, `FakeRenterRepository`, `.plan`, `.plan`, `HomeIntent`, `RecordPaymentViewModelTest`, `MonthDetailScreen.kt`, `FakeRenterRepository`, `AddRenterViewModelTest.kt`, `ReminderNotificationDelegate.kt`, `MonthDetailIntent`, `SaveRenterUseCaseTest`, `FieldProblem`, `RenterId`, `ReminderSyncTest`, `.summary`, `HomeViewModelTest.kt`, `RoomBillRepositoryTest`, `FakeBillRepository`, `MviViewModel`, `Outcome`, `.renter`, `ReminderNotifications.kt`, `ReminderActionReceiver.kt`, `AppNavigation.kt`, `Payment`, `FakeBillRepository`, `RoomRenterRepositoryTest`, `.months`, `MonthsViewModelTest`, `UpdateRenterUseCase`?**
  _High betweenness centrality (0.153) - this node is a cross-community bridge._
- **Are the 21 inferred relationships involving `RenterId` (e.g. with `.aRenterWithNoBillHasNothingToMuteAndNothingChanges()` and `.aStorageFailureIsReported()`) actually correct?**
  _`RenterId` has 21 INFERRED edges - model-reasoned connections that need verification._
- **What connects `UnitTaken`, `InvalidBackup`, `DriveNotConnected` to the rest of the system?**
  _150 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Renter` be split into smaller, more focused modules?**
  _Cohesion score 0.09408033826638477 - nodes in this community are weakly interconnected._
- **Why does `Outcome` connect `Outcome` to `Renter`, `Screen Test Helpers`, `safeCall`, `Create Bill Screen`, `Rent Increase and Billing Month`, `.viewModel`, `.viewModel`, `RecordPaymentUseCaseTest`, `DriveBackupWorker.kt`, `BuildBillUseCaseTest`, `Paise`, `RenterId`, `CreateBillViewModel.kt`, `FakeRenterRepository`, `PaymentUseCases.kt`, `Modules.kt`, `AppSettings`, `FakeBillRepository`, `RenterSummary`, `Reminders.kt`, `MoneyInput`, `.plan`, `FakeDriveBackupRepository`, `FakeSettingsRepository`, `AddRenterViewModelTest.kt`, `GoogleDriveRepository.kt`, `IosReminderScheduler.kt`, `SaveSettingsUseCaseTest`, `BuildFixedChargeUseCaseTest`, `SaveRenterUseCaseTest`, `FieldProblem`, `AddAddressUseCaseTest`, `HomeViewModelTest.kt`, `FakeBillRepository`, `MviViewModel`, `ReminderNotifications.kt`, `ObserveRenterSummariesUseCase`, `BackupCodec.kt`, `MessageKind`, `Payment`, `MeterPhotoStore`, `DriveBackupViewModelTest.kt`, `DriveUseCasesTest`, `GoogleDriveRepository`, `GoogleDriveRepositoryTest.kt`, `RoomSettingsRepository.kt`, `UpdateRenterUseCase`?**
  _High betweenness centrality (0.150) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Paise` (e.g. with `.aRateIsAtMostNinetyNineRupeesSoAMissingDecimalPointIsCaught()` and `.parsesAPaymentWithUpToTwoDecimals()`) actually correct?**
  _`Paise` has 6 INFERRED edges - model-reasoned connections that need verification._
- **Should `Screen Test Helpers` be split into smaller, more focused modules?**
  _Cohesion score 0.13432835820895522 - nodes in this community are weakly interconnected._