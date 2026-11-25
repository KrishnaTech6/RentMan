# Graph Report - RentMan  (2026-11-25)

## Corpus Check
- 258 files · ~88,281 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3412 nodes · 9660 edges · 164 communities (75 shown, 89 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 504 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Room Renter Repository Tests
- Shared UI Components
- Screen Test Helpers
- Add Renter ViewModel
- Renter Leaving Use Cases
- MVI Base and Payment ViewModels
- Renter Ledger Use Cases
- Data Layer Errors and Backup
- Create Bill Screen
- Rent Increase and Billing Month
- Renter Detail ViewModel
- Add Renter Screen
- Data Module and Room Repositories
- Home Screen
- Community 14
- Community 15
- Community 16
- Community 17
- Community 18
- Community 19
- Community 20
- Community 21
- Community 22
- Community 23
- Community 24
- Community 25
- Community 26
- Community 27
- Community 28
- Community 29
- Community 30
- Community 31
- Community 32
- Community 33
- Community 34
- Community 35
- Community 36
- Community 37
- Community 38
- Community 39
- Community 40
- Community 41
- Community 42
- Community 43
- Community 44
- Community 46
- Community 47
- Community 48
- Community 49
- Community 50
- Community 51
- Community 52
- Community 53
- Community 54
- Community 55
- Community 56
- Community 57
- Community 58
- Community 59
- Community 60
- Community 61
- Community 62
- Community 63
- Community 64
- Community 65
- Community 66
- Community 67
- Community 68
- Community 69
- Community 70
- Community 71
- Community 72
- Community 73
- Community 74
- Community 75
- Community 76
- Community 77
- Community 78
- Community 79
- Community 80
- Community 81
- Community 82
- Community 83
- Community 84
- Community 85
- Community 86
- Community 87
- Community 88
- Community 89
- Community 90
- Community 91
- Community 92
- Community 93
- Community 94
- Community 95
- Community 96
- Community 97
- Community 98
- Community 99
- Community 100
- Community 101
- Community 102
- Community 103
- Community 104
- Community 105
- Community 106
- Community 107
- Community 108
- Community 109
- Community 110
- Community 111
- Community 112
- Community 113
- Community 114
- Community 115
- Community 116
- Community 118
- Community 120
- Community 121
- Community 122
- Community 123
- Community 124
- Community 125
- Community 128
- Community 129
- Community 131
- Community 132
- Community 133
- Community 134
- Community 135
- Community 136
- Community 137
- Community 138
- Community 139
- Community 140
- Community 141
- Community 142
- Community 143
- Community 144

## God Nodes (most connected - your core abstractions)
1. `Outcome` - 217 edges
2. `DomainError` - 108 edges
3. `Paise` - 104 edges
4. `RenterId` - 82 edges
5. `Bill` - 62 edges
6. `BillingMonth` - 51 edges
7. `FakeRenterRepository` - 38 edges
8. `AddRenterViewModelTest` - 37 edges
9. `AddressId` - 36 edges
10. `BackupDao` - 35 edges

## Surprising Connections (you probably didn't know these)
- `Money history is immutable` --semantically_similar_to--> `Bill snapshot principle (no recompute from current settings)`  [INFERRED] [semantically similar]
  DATABASE.md → docs/adr/0002-bills-are-immutable-snapshots.md
- `Error` --references--> `DomainError`  [EXTRACTED]
  sharedUI/src/commonMain/kotlin/com/rentman/sharedui/feature/renter/AddRenterContract.kt → domain/src/commonMain/kotlin/com/rentman/domain/Outcome.kt
- `AddRenterViewModelTest` --calls--> `FixedCharge`  [INFERRED]
  sharedUI/src/commonTest/kotlin/com/rentman/sharedui/feature/renter/AddRenterViewModelTest.kt → domain/src/commonMain/kotlin/com/rentman/domain/billing/Bill.kt
- `Error` --references--> `DomainError`  [EXTRACTED]
  sharedUI/src/commonMain/kotlin/com/rentman/sharedui/feature/payment/RecordPaymentContract.kt → domain/src/commonMain/kotlin/com/rentman/domain/Outcome.kt
- `formatRupees()` --references--> `Paise`  [EXTRACTED]
  sharedUI/src/androidMain/kotlin/com/rentman/sharedui/components/CurrencyFormat.android.kt → domain/src/commonMain/kotlin/com/rentman/domain/money/Paise.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **MVI screen contract: UiState, NavEvent and MviViewModel** — architecture_mviviewmodel, architecture_uistate, architecture_navevent, architecture_dumb_ui [EXTRACTED 1.00]
- **Pending is derived from bill lines and payments, never stored** — database_pending_calculation, database_bill_line_table, database_payment_table, database_derived_not_stored, glossary_pending [EXTRACTED 1.00]
- **Bill immutability with the remove-and-remake exception** — docs_adr_0002_bills_are_immutable_snapshots, docs_adr_0009_a_wrong_bill_is_removed_and_made_again, database_bill_table, plan_remove_wrong_bill_feature, database_money_history_immutable [EXTRACTED 1.00]

## Communities (164 total, 89 thin omitted)

### Community 0 - "Room Renter Repository Tests"
Cohesion: 0.07
Nodes (7): InsertedRenter, RenterDao, RoomRenterRepository, RoomRenterRepositoryTest, FixedCharge, UpdateRenterUseCase, UpdateRenterUseCaseTest

### Community 1 - "Shared UI Components"
Cohesion: 0.05
Nodes (15): ActionButton(), ActionRow(), CenteredMessage(), ErrorMessage(), CropArea(), LightButton(), MeterCropper(), MeterPhotoThumbnail() (+7 more)

### Community 2 - "Screen Test Helpers"
Cohesion: 0.10
Nodes (8): toDomain(), BillLine, BillLineKind, Adjustment, Electricity, Rent, GetTodayUseCase, Clock

### Community 3 - "Add Renter ViewModel"
Cohesion: 0.07
Nodes (35): AddRenterIntent, AddCharge, AskToRaiseChanged, Back, BillsElectricityChanged, ChargeAmountChanged, ChargeLabelChanged, DueDayChanged (+27 more)

### Community 4 - "Renter Leaving Use Cases"
Cohesion: 0.06
Nodes (9): MarkRenterLeftUseCase, MoveRenterBackUseCase, ObserveLeftRentersUseCase, RenterRepository, RenterFormValidator, RenterUnits, toForm(), RenterLeavingUseCasesTest (+1 more)

### Community 5 - "MVI Base and Payment ViewModels"
Cohesion: 0.06
Nodes (19): AddressPickerIntent, Add, Confirm, NameChanged, Retry, Select, AddressPickerNavEvent, Picked (+11 more)

### Community 6 - "Renter Ledger Use Cases"
Cohesion: 0.08
Nodes (9): MuteRemindersUseCase, ObserveRenterSummariesUseCase, BillingMonthTest, ObserveRenterLedgerUseCaseTest, FakeAddressRepository, MuteRemindersUseCaseTest, ObserveRenterSummariesUseCaseTest, ObserveRentIncreaseOffersUseCaseTest (+1 more)

### Community 7 - "Data Layer Errors and Backup"
Cohesion: 0.07
Nodes (15): RoomBackupRepository, safeCall(), safeFlow(), BackupExport, BackupRepository, ExportBackupUseCase, ObserveBackupStatusUseCase, RestoreBackupUseCase (+7 more)

### Community 8 - "Create Bill Screen"
Cohesion: 0.05
Nodes (25): CreateBillIntent, Back, ChangeReading, ClearMeterPhoto, MeterReplacedChanged, MeterScanned, NewMeterStartChanged, ReadingChanged (+17 more)

### Community 9 - "Rent Increase and Billing Month"
Cohesion: 0.08
Nodes (16): toDomain(), RentChangeMapperTest, BillingMonth, remindersMutedFor(), ApplyRentIncreaseUseCase, ObserveRentIncreaseOffersUseCase, PostponeRentIncreaseUseCase, RentChange (+8 more)

### Community 10 - "Renter Detail ViewModel"
Cohesion: 0.08
Nodes (23): Back, Close, Content, Edit, Error, Loading, MarkLeft, MoveBack (+15 more)

### Community 11 - "Add Renter Screen"
Cohesion: 0.04
Nodes (5): ScreenTopBar(), AddRenterRoute(), AddRenterScreen(), AddressRow(), FilledWithProblemsPreview()

### Community 12 - "Data Module and Room Repositories"
Cohesion: 0.06
Nodes (11): RoomAddressRepository, toDomain(), dataModule(), AppDatabase, AppDatabaseConstructor, RoomAddressRepositoryTest, AddressRepository, AddAddressUseCase (+3 more)

### Community 13 - "Home Screen"
Cohesion: 0.06
Nodes (18): amountStyle(), BillTodoCard(), Centered(), Chip(), ContentBody(), EmptyPreview(), HomeRoute(), HomeScreen() (+10 more)

### Community 14 - "Community 14"
Cohesion: 0.10
Nodes (5): BackupStatus, MarkBackedUpUseCase, BackupUseCasesTest, FakeSettingsRepository, SettingsViewModelTest

### Community 15 - "Community 15"
Cohesion: 0.08
Nodes (12): PendingCalculator, LedgerEntry, BillEntry, PaymentEntry, ObserveRenterLedgerUseCase, Movement, RecordPaymentUseCase, RemovePaymentUseCase (+4 more)

### Community 16 - "Community 16"
Cohesion: 0.12
Nodes (3): BackupCodecTest, RoomBackupRepositoryTest, Clock

### Community 17 - "Community 17"
Cohesion: 0.06
Nodes (14): MainActivity, App(), AppNavigation(), Navigator, AddRenterKey, AddressPickerKey, CreateBillKey, EditRenterKey (+6 more)

### Community 18 - "Community 18"
Cohesion: 0.05
Nodes (40): BillField, NewMeterStart, Reading, DomainError, BackupFromNewerVersion, BillExists, BillNotRemovable, BlankName (+32 more)

### Community 19 - "Community 19"
Cohesion: 0.10
Nodes (5): BillMessage, BillContext, BuildBillUseCase, BillMessageTest, BuildBillUseCaseTest

### Community 20 - "Community 20"
Cohesion: 0.09
Nodes (18): toDomain(), RenterMapperTest, BillCalculator, Paise, Address, AddressId, BuildFixedChargeUseCase, ChargeField (+10 more)

### Community 21 - "Community 21"
Cohesion: 0.07
Nodes (12): Divider(), Section(), SegmentButton(), Segmented(), StepButton(), TextRow(), DateStepper(), Form() (+4 more)

### Community 22 - "Community 22"
Cohesion: 0.07
Nodes (6): ObserveSettingsUseCase, SetReminderRepeatUseCase, StepReminderTimeUseCase, ReminderSettingsUseCasesTest, inputReducers(), loadReducers()

### Community 23 - "Community 23"
Cohesion: 0.11
Nodes (13): Back, BackUpNow, BackupShared, BackupShareFailed, RateChanged, RepeatChanged, RestoreFrom, Retry (+5 more)

### Community 25 - "Community 25"
Cohesion: 0.06
Nodes (7): BackedUpPreview(), Form(), rateProblem(), repeatLabel(), SettingsRoute(), SettingsScreen(), TimeStepper()

### Community 26 - "Community 26"
Cohesion: 0.08
Nodes (12): BalanceCard(), BillRow(), Body(), EntryAction, EntryCard(), entryKey(), LeftRow(), PaymentRow() (+4 more)

### Community 27 - "Community 27"
Cohesion: 0.10
Nodes (9): AppSettingsEntity, SettingsDao, RoomSettingsRepository, toDomain(), toEntity(), RoomSettingsRepositoryTest, AppSettings, SettingsRepository (+1 more)

### Community 28 - "Community 28"
Cohesion: 0.12
Nodes (9): toDomain(), RoomPaymentRepository, PaymentMapperTest, RoomPaymentRepositoryTest, Payment, PaymentId, PaymentRepository, FakePaymentRepository (+1 more)

### Community 30 - "Community 30"
Cohesion: 0.13
Nodes (5): RentChangeDao, RentChangeEntity, toEntity(), RoomRentIncreaseRepository, RoomRentIncreaseRepositoryTest

### Community 31 - "Community 31"
Cohesion: 0.08
Nodes (7): readText(), rememberBackupFiles(), findActivity(), notificationsAllowed(), openNotificationSettings(), rememberNotificationPermission(), rememberNotificationPermission()

### Community 32 - "Community 32"
Cohesion: 0.08
Nodes (8): AddressPickerRoute(), AddressPickerScreen(), AddressRow(), ContentBody(), ContentPreview(), NewAddress(), RadioDot(), NavigationEffect()

### Community 33 - "Community 33"
Cohesion: 0.13
Nodes (7): BillTodo, HomeSnapshot, Lists, ObserveBillContextUseCase, ObserveBillTodosUseCase, ObserveHomeUseCase, UnbilledMonths

### Community 34 - "Community 34"
Cohesion: 0.16
Nodes (4): RaiseRent, SelectFilter, UndoRaise, HomeViewModelTest

### Community 36 - "Community 36"
Cohesion: 0.06
Nodes (32): ENABLE_USER_SCRIPT_SANDBOXING, EXCLUDED_ARCHS[sdk=iphonesimulator*], FRAMEWORK_SEARCH_PATHS, OTHER_LDFLAGS, PRODUCT_BUNDLE_IDENTIFIER, SWIFT_VERSION, iOS, path (+24 more)

### Community 37 - "Community 37"
Cohesion: 0.13
Nodes (4): MoneyInput, PhoneNumber, MoneyInputTest, PhoneNumberTest

### Community 38 - "Community 38"
Cohesion: 0.14
Nodes (5): CreateBillNavEvent, Close, Saved, CreateBillViewModel, State

### Community 39 - "Community 39"
Cohesion: 0.14
Nodes (6): PhotoItem, PhotoPickerDelegate, PreviewSource, rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource()

### Community 41 - "Community 41"
Cohesion: 0.11
Nodes (3): ObserveReminderPlanUseCase, ReminderPlanner, ReminderSync

### Community 42 - "Community 42"
Cohesion: 0.10
Nodes (22): RenterRow, RenterStatus, Late, NoBillYet, Paid, PartPaid, Unpaid, AddRenter (+14 more)

### Community 43 - "Community 43"
Cohesion: 0.16
Nodes (10): RecordPaymentIntent, AmountChanged, Back, DayStepped, ModeChanged, NoteChanged, Retry, Save (+2 more)

### Community 44 - "Community 44"
Cohesion: 0.08
Nodes (3): message(), messageResource(), modeLabel()

### Community 48 - "Community 48"
Cohesion: 0.19
Nodes (5): MeterPhotoFiles, rememberPhotoFiles(), rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource()

### Community 49 - "Community 49"
Cohesion: 0.18
Nodes (3): MonthSummary, Share, MonthSummaryTest

### Community 50 - "Community 50"
Cohesion: 0.13
Nodes (11): Corner, BottomLeft, BottomRight, TopLeft, TopRight, CropRect, PixelBox, Grab (+3 more)

### Community 52 - "Community 52"
Cohesion: 0.13
Nodes (4): ReminderPlan, ScheduledReminder, FakeReminderScheduler, IosReminderScheduler

### Community 55 - "Community 55"
Cohesion: 0.17
Nodes (5): Bill, BillRepository, isNewestOf(), RestoreBillUseCase, FakeBillRepository

### Community 56 - "Community 56"
Cohesion: 0.12
Nodes (18): overpayment(), PaymentField, Amount, Date, Note, PaymentForm, PaymentMode, Bank (+10 more)

### Community 62 - "Community 62"
Cohesion: 0.16
Nodes (6): androidDataModule(), ReminderScheduler, startRentManKoin(), AndroidReminderScheduler, sharedModules(), startRentManKoin()

### Community 64 - "Community 64"
Cohesion: 0.18
Nodes (12): PhotoFiles, PhotoOpener, PhotoSource, readWithin(), rememberPhotoFiles(), rememberPhotoImage(), rememberPhotoOpener(), rememberPhotoSource() (+4 more)

### Community 66 - "Community 66"
Cohesion: 0.26
Nodes (4): RemoveBillUseCase, FakeBillRepository, RemoveBillUseCaseTest, FakeRenterRepository

### Community 67 - "Community 67"
Cohesion: 0.14
Nodes (7): ComposeView, ContentView, .body, iOSApp, .body, sharedUI, SwiftUI

### Community 68 - "Community 68"
Cohesion: 0.14
Nodes (14): Data layer (:data, Room 3), Domain layer (:domain), formatRupees expect/actual (Indian grouping), Koin dependency injection, AGP 9 KMP module structure (androidApp, iosApp, sharedUI, data, domain), MviViewModel base class (toState, Eagerly), NavEvent one-shot SharedFlow, Navigation 3 (NavKey, NavDisplay) (+6 more)

### Community 69 - "Community 69"
Cohesion: 0.15
Nodes (4): MeterReadingParser, Candidate, SuggestMeterReadingUseCase, formatRupees()

### Community 72 - "Community 72"
Cohesion: 0.15
Nodes (10): Platform gateways and UI-only expect/actual (photo, backup files), bill table, Ledger (bills and payments merged), payment table (voidable), ADR 0002: Bills are immutable snapshots made of lines, ADR 0009: A wrong bill is removed and made again, Adjustment (glossary term), Electricity rate and meter reading rules (+2 more)

### Community 73 - "Community 73"
Cohesion: 0.18
Nodes (3): AmountText(), BalanceText(), formatRupees()

### Community 74 - "Community 74"
Cohesion: 0.17
Nodes (4): CreateBill, Reminder, RentPending, ReminderWorker

### Community 75 - "Community 75"
Cohesion: 0.23
Nodes (4): FixedChargeEntity, RenterWithCharges, RenterEntity, toEntity()

### Community 76 - "Community 76"
Cohesion: 0.16
Nodes (5): label(), shortLabel(), shortName(), ReminderText, text()

### Community 79 - "Community 79"
Cohesion: 0.23
Nodes (3): PaymentDao, PaymentEntity, toEntity()

### Community 82 - "Community 82"
Cohesion: 0.15
Nodes (11): Paise value class (money as Long), bill_line table, Pending / balance derivation (PendingCalculator), Renter status derivation (No bill, Paid, Late, Part paid, Unpaid), Due day (glossary term), Late (glossary term), Not due yet (glossary term), Part paid and Unpaid statuses (glossary terms) (+3 more)

### Community 83 - "Community 83"
Cohesion: 0.21
Nodes (3): AppDispatchers, AppDispatchers, ReminderActionReceiver

### Community 84 - "Community 84"
Cohesion: 0.35
Nodes (5): toEntity(), BillEntity, BillLineEntity, BillWithLines, sampleBackup()

### Community 85 - "Community 85"
Cohesion: 0.17
Nodes (12): address table, fixed_charge table, rent_change table, renter table, Unbilled months derivation, ADR 0008 The landlord creates every bill, Remade bill uses rent of its own month (RentHistory.rentFor), Rent increase (glossary term) (+4 more)

### Community 90 - "Community 90"
Cohesion: 0.22
Nodes (9): SettingsForm, BackupMessage, Restored, RestoreUndone, ShareFailed, Content, Error, Loading (+1 more)

### Community 95 - "Community 95"
Cohesion: 0.20
Nodes (10): RenterField, Address, Charge, DueDay, Name, OwnRate, Phone, Rent (+2 more)

### Community 96 - "Community 96"
Cohesion: 0.31
Nodes (4): clockDigits(), isAfternoon(), timeLabel(), TimeTextTest

### Community 97 - "Community 97"
Cohesion: 0.36
Nodes (7): ARCHITECTURE.md (engineering contract), Workflow for every change (ponytail, review, graphify), DATABASE.md (schema v1), Home design, GLOSSARY.md (ubiquitous language), PLAN.md (product plan), Claude Design mockup (design/project)

### Community 99 - "Community 99"
Cohesion: 0.36
Nodes (4): AskForNotificationsOnce(), NotificationPermission, rememberNotificationPermission(), rememberNotificationPermission()

### Community 101 - "Community 101"
Cohesion: 0.29
Nodes (8): Add renter design, Address picker design, Address filter chips, Address (glossary term), Left renter (glossary term), Renter (glossary term), Unit (glossary term), Renter edit and mark left feature

### Community 107 - "Community 107"
Cohesion: 0.29
Nodes (7): Record payment design, Renter ledger design, ADR 0001 Pending is computed, never stored, Paid ahead (glossary term), Payment (glossary term), Pending (glossary term), WhatsApp bill message (wa.me link)

### Community 109 - "Community 109"
Cohesion: 0.43
Nodes (3): BackupFiles, rememberBackupFiles(), rememberBackupFiles()

### Community 111 - "Community 111"
Cohesion: 0.40
Nodes (6): Fixed monthly charges list, Bill preview design, Meter reading design, Bill (glossary term), Bill line (glossary term), Fixed charge (glossary term)

### Community 115 - "Community 115"
Cohesion: 0.50
Nodes (5): ReminderSync and ReminderScheduler (resync, not timer), Reminder mute (remindersMutedMonth), Stop this month button, Reminder mute (glossary term), Reminders feature (ReminderPlanner, WorkManager, UNUserNotificationCenter)

### Community 116 - "Community 116"
Cohesion: 0.40
Nodes (5): app_settings table (single row), Backup file JSON and restore transaction (BackupCodec), backup_snapshot table, Schema changes: version 1 frozen from first real install, Backup and restore feature (share sheet, Undo last restore)

### Community 121 - "Community 121"
Cohesion: 0.67
Nodes (3): Close, SettingsNavEvent, ShareBackup

## Knowledge Gaps
- **139 isolated node(s):** `Back`, `PickAddress`, `Retry`, `Close`, `PickAddress` (+134 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 656 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **89 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Outcome` connect `Data Layer Errors and Backup` to `Room Renter Repository Tests`, `Screen Test Helpers`, `Renter Leaving Use Cases`, `Create Bill Screen`, `Rent Increase and Billing Month`, `Data Module and Room Repositories`, `Community 14`, `Community 15`, `Community 18`, `Community 20`, `Community 27`, `Community 28`, `Community 30`, `Community 35`, `Community 38`, `Community 47`, `Community 52`, `Community 53`, `Community 55`, `Community 56`, `Community 58`, `Community 59`, `Community 62`, `Community 65`, `Community 66`, `Community 78`, `Community 86`, `Community 92`, `Community 98`?**
  _High betweenness centrality (0.187) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Paise` (e.g. with `.aRateIsAtMostNinetyNineRupeesSoAMissingDecimalPointIsCaught()` and `.parsesAPaymentWithUpToTwoDecimals()`) actually correct?**
  _`Paise` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Back`, `PickAddress`, `Retry` to the rest of the system?**
  _139 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Room Renter Repository Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.07484629778134189 - nodes in this community are weakly interconnected._
- **Why does `DomainError` connect `Community 18` to `Shared UI Components`, `Screen Test Helpers`, `Add Renter ViewModel`, `MVI Base and Payment ViewModels`, `Data Layer Errors and Backup`, `Create Bill Screen`, `Rent Increase and Billing Month`, `Add Renter Screen`, `Data Module and Room Repositories`, `Community 15`, `Community 20`, `Community 21`, `Community 25`, `Community 28`, `Community 32`, `Community 35`, `Community 38`, `Community 44`, `Community 47`, `Community 52`, `Community 55`, `Community 56`, `Community 61`, `Community 62`, `Community 86`, `Community 92`?**
  _High betweenness centrality (0.105) - this node is a cross-community bridge._
- **Are the 13 inferred relationships involving `RenterId` (e.g. with `.aRenterWithNoBillHasNothingToMuteAndNothingChanges()` and `.aStorageFailureIsReported()`) actually correct?**
  _`RenterId` has 13 INFERRED edges - model-reasoned connections that need verification._
- **Should `Shared UI Components` be split into smaller, more focused modules?**
  _Cohesion score 0.05126050420168067 - nodes in this community are weakly interconnected._