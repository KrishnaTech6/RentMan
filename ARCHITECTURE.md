# Rent Manager: architecture

Product spec: `PLAN.md`. Database: `DATABASE.md`. Vocabulary: `GLOSSARY.md`. Decisions: `docs/adr/`. This file is the engineering contract. Every change must follow it.

## 0. Stack (checked against current docs via context7, 2026-11-22)
| Piece | Version | Note |
|---|---|---|
| Gradle | 9.6.0 | AGP 9.5 minimum |
| JDK | 17 | AGP 9.5 minimum |
| Android Gradle Plugin | 9.5.2 | Built-in Kotlin. No `kotlin-android` plugin. |
| Kotlin | 2.4.21 | Stable context parameters and explicit backing fields are available. |
| Compose Multiplatform | 1.12.2 | Only `runtime`, `foundation` and `ui` are used so far. Material3 is not added yet (its bundled version is an alpha). |
| Lifecycle (JetBrains) | 2.11.0 | `ViewModel`, `collectAsStateWithLifecycle` |
| Navigation 3 (JetBrains) | 1.2.0 | `NavDisplay`, `NavKey`. Stable (the Compose Multiplatform changelog still says `rc01`, but `1.2.0` is on Maven Central). |
| Room | 3.0.3, `androidx.room3` | KSP only, coroutines-first, `SQLiteDriver`-based |
| SQLite driver | `BundledSQLiteDriver` (`androidx.sqlite`) | |
| Koin | BOM 4.2.2 | `koin-compose-viewmodel` |
| KSP | 2.3.14 | For Room 3 (added with the first Room slice) |
| kotlinx-coroutines | 1.11.0 | |
| kotlinx-serialization | 1.13.0 | Navigation keys and backup |
| Turbine / Kover | 1.2.1 / 0.9.11 | In the version catalog. Kover is not applied yet. |
| SQLite bundled driver | 2.7.1 | `androidx.sqlite:sqlite-bundled` |
| Android SDK | compile/target 37, min 26 | |
| iOS | deployment target 16.0 | Xcode 26.3. The Kotlin/Native runtime is built for iOS 18.5, so the linker warns about it. **Only run on iOS 26.3 so far, not verified on iOS 16 or 17.** Test on an older iPhone before release, or raise the target. |
| kotlinx-datetime | 0.8.0 | `Clock` and `Instant` come from `kotlin.time` in the stdlib. |

Versions live in `gradle/libs.versions.toml` and were resolved from Maven Central and Google Maven (stable releases only) on 2026-11-22. Not decided yet: WorkManager (latest stable `2.12.0`), Compose resources, Material3.
Rule: before writing setup code or bumping a dependency, query context7 again. Don't copy versions from memory or from this table.

## 1. Principles
1. **Domain-driven.** The domain layer is pure Kotlin, has no framework dependencies, and owns the business rules.
2. **MVI, unidirectional.** Intent → ViewModel → one `UiState` → UI. Nothing flows backwards.
3. **Reactive, not imperative.** Streams are Flow. Work is coroutines. In ViewModels, domain and data: no `var`, no manual `launch {}`, no `for`/`while` loops over state, no callbacks. Use operators: `map`, `combine`, `flatMapLatest`, `scan`, `fold`. Compose UI-only state (`remember`, scroll, focus) is allowed. There are two named exceptions: the app-scoped reminder collector (§7), and the internals of the stream operators in `mvi/` (for example `flatMapFirst`), which may use `launch`.
4. **Dumb UI.** Composables render a `UiState` and forward intents. They contain no business logic.
5. **SOLID.** ViewModels depend on use cases only, never on repositories or DAOs. Interfaces live in domain and implementations in data. Dependencies are injected. Thin pass-through use cases are an accepted cost of this rule.
6. **No mappers above the data layer.** ViewModels and UI use domain models directly. The only mappers are inside `:data`, between Room entities and domain models.
7. **Errors are values, caught once in the data layer.** See §3 and ADR 0007.

## 2. Project structure (current KMP layout, AGP 9)
Source: the JetBrains "recommended project structure" and "AGP 9 migration" guides. With AGP 9 the Android entry point **must** live in its own module and cannot share a module with common code.
```
RentMan/
  androidApp/    com.android.application. Only MainActivity, manifest, Koin start. → :sharedUI
  iosApp/        Xcode project. Only the Swift entry that hosts the Compose view.   → sharedUI framework
  sharedUI/      Compose UI, ViewModels, navigation, DI wiring. Exports the iOS framework.  → :domain, :data (wiring only)
  data/          Room, repository implementations, platform gateways.                      → :domain
  domain/        Models, repository interfaces, use cases, pure rules.                      → nothing
  gradle/libs.versions.toml     version catalog, the only place versions live
```
Convention plugins (`build-logic/`) are deferred. The three KMP modules repeat about 15 lines of build config, which is cheaper than the plugin machinery until there's a fourth module.
The iOS project is generated from `iosApp/project.yml` with `xcodegen`, and the generated `iosApp.xcodeproj` is the one to open in Xcode. The generated `Info.plist` must keep `CADisableMinimumFrameDurationOnPhone = true`: Compose Multiplatform aborts at launch without it (found when the first text field and list screen ran on iOS).
- Every KMP module (`domain`, `data`, `sharedUI`) uses the `org.jetbrains.kotlin.multiplatform` and **`com.android.kotlin.multiplatform.library`** plugins. The old `com.android.library` and `androidTarget {}` are not used.
- Android config lives in `kotlin { android { namespace; compileSdk; minSdk; androidResources { enable = true } } }` (resources only in `sharedUI`). `androidLibrary {}` is deprecated in favor of `android {}`. There is no top-level `android {}` block and no build variants in the shared modules. For tooling previews, use `androidRuntimeClasspath(libs.compose.uiTooling)` instead of `debugImplementation`.
- **Targets:** Android, `iosArm64`, `iosSimulatorArm64`, plus `jvm()` on `domain`, `data` and `sharedUI` **for tests only** (no JVM app is shipped). Reason: with the AGP 9 library plugin, Android host tests are opt-in (`withHostTestBuilder {}`) and Kover measures only JVM and Android code, so a JVM target gives fast tests with no emulator. Room 3 runs in memory on the JVM with `BundledSQLiteDriver`.
- Only `sharedUI` builds the iOS framework (`baseName = "sharedUI"`, `isStatic = true`). `domain` and `data` are not exported.
- The root `build.gradle.kts` declares every plugin with `apply false`.
- Gradle enforces only the wrong directions: `domain` depends on nothing and `data` can't depend on `sharedUI`. It does **not** stop `sharedUI` from importing `:data` classes, because `sharedUI` depends on `:data`. That guard is Kotlin visibility: everything in `:data` is `internal` except its Koin module, so `sharedUI` can only wire it in `di`.
- Don't split `sharedUI` into feature modules until build times or ownership require it.

## 3. Domain layer (`:domain`)
Bounded contexts, as packages:
- `tenancy`: Address, Renter, FixedCharge, RentChange, rent-increase policy.
- `billing`: Bill, BillLine, Payment, BillCalculator, Ledger (pending = bills − payments), unbilled months.
- `reminders`: ReminderPolicy, ReminderPlan, ReminderScheduler interface.
- `settings`, `backup`.

Rules:
- **Models** are immutable `data class`es with typed ids (`@JvmInline value class RenterId(val value: Long)`). Money is a `Paise` value class over `Long`. There is no `Double` for money.
- **Domain models carry no persistence annotations.** Room entities live in `:data`.
- **Pure rules** are plain functions or `fun interface`s, with no I/O: `BillCalculator`, `RentIncreasePolicy`, `PendingCalculator`. Time comes in as a `kotlin.time.Clock` **and a `TimeZone`** parameter and is never read from a global.
- **Repository interfaces** are in `:domain`.
- **Use cases:** one class per action with `operator fun invoke(...)`. Use cases compose repositories and pure rules. The ViewModel never does that.
  - Reads (built): `ObserveHomeUseCase`, `ObserveBillTodosUseCase`, `ObserveBillContextUseCase`, `ObserveRenterLedgerUseCase`, `ObserveRenterSummariesUseCase`, `ObserveAddressesUseCase`, `ObserveSettingsUseCase`, `ObserveRentIncreaseOffersUseCase`, `ObserveReminderPlanUseCase`, `ObserveRenterFormUseCase`, `ObserveLeftRentersUseCase`.
  - Writes (built): `BuildBillUseCase` (pure, no I/O), `CreateBillUseCase`, `RecordPaymentUseCase`, `RemovePaymentUseCase`, `RestorePaymentUseCase`, `SaveRenterUseCase`, `UpdateRenterUseCase`, `MarkRenterLeftUseCase`, `MoveRenterBackUseCase`, `AddAddressUseCase`, `SaveSettingsUseCase`, `ApplyRentIncreaseUseCase`, `PostponeRentIncreaseUseCase`, `UndoRentIncreaseUseCase`, `MuteRemindersUseCase`, `ExportBackupUseCase`, `RestoreBackupUseCase`, `UndoRestoreUseCase`, `ObserveBackupStatusUseCase`, `SuggestMeterReadingUseCase` (pure: picks the reading out of the text read from a photo), `RemoveBillUseCase` and `RestoreBillUseCase` (ADR 0009).
- **Errors are values.** `sealed interface Outcome<out T> { Success(value); Failure(error: DomainError) }` with `fold`. `DomainError` covers both expected failures (`ReadingBelowPrevious`, `InvalidPhone`) and a `Storage` failure for infrastructure problems.
  - Reads return `Flow<Outcome<T>>`. Writes are `suspend` and return `Outcome<T>`.
  - Use cases combine repository flows into one `Flow<Outcome<...>>`.
  - ViewModels contain **no `catch`**. They map `Outcome` to `UiState` with `fold`. Because failures are values, the stream stays alive after an error and a Retry intent works.

## 4. Data layer (`:data`)
- **Room 3** (`androidx.room3`), KSP only, with the Room Gradle plugin (`room3 { schemaDirectory(...) }`) and committed schema files. DAO functions are `suspend` or return `Flow`. The driver is `BundledSQLiteDriver`, set last in the builder. Details of the schema are in `DATABASE.md`.
- **The single catch.** Repository implementations wrap every read with a shared `safeFlow {}` helper and every write with `safeCall {}`. These catch infrastructure exceptions once and turn them into `Outcome.Failure(DomainError.Storage)`. `CancellationException` is always rethrown. Nothing below the repositories needs its own `try/catch`.
- **Entities are separate from domain models.** They are `internal` classes named `<Name>Entity` in `:data`, holding only primitives (`Long`, `Int`, `String`), so no type converters are needed. DAOs return entities and entity composites (`@Relation`, `@Embedded`).
- **Mappers live in `:data` only**, as `internal` extension functions per entity (`toDomain()`, `toEntity()`), used by the repository implementations. Nothing outside `:data` ever sees an entity.
- Repository implementations are `internal`, so only the DI module exposes them. They take injected `CoroutineDispatcher`s (an `AppDispatchers` interface) and never hardcode `Dispatchers.IO`.
- **Pending is never stored.** It's computed from bill lines and payments. A bill is a snapshot and is never recomputed from current settings.
- **Settings live in the database** (a single-row `app_settings` table), not in a separate store. That way a backup or restore is one atomic transaction. DataStore is not a dependency.
- Platform pieces sit behind interfaces in `:domain`, with `expect/actual` or Koin-provided implementations in `:data`: `DatabaseFactory`, `ReminderScheduler`. The meter camera, the photo viewer, the backup's share sheet and the file picker need the screen, so they are UI-only `expect/actual` composables (`rememberPhotoSource` (camera and gallery), `rememberPhotoFiles` (crop, read the text, discard), `rememberPhotoImage`, `rememberPhotoOpener`, `rememberBackupFiles`, like `rememberNotificationPermission`), not a domain gateway: the ViewModel gets the file's text as an intent and sends the JSON out as a navigation event. WhatsApp needs no gateway: the screen opens the `wa.me` link with the platform's `LocalUriHandler`.

## 5. Presentation layer (`sharedUI`)
```
sharedUI/src/commonMain/kotlin/<pkg>/
  di/                    Koin modules (the only place that sees :data)
  navigation/            NavKey routes (@Serializable), NavDisplay host, NavEvent handling
  mvi/                   MviViewModel base
  components/            reusable composables only (see rule below)
  theme/                 colors, type, spacing tokens (from design/)
  feature/<name>/        HomeRoute, HomeScreen, HomeViewModel, HomeContract (Intent, UiState, NavEvent)
```

### MVI contract, per screen (`<Name>Contract.kt`)
```kotlin
sealed interface HomeIntent { data class SelectAddress(val id: AddressId?) : HomeIntent /* ... */ }

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val error: DomainError) : HomeUiState // the screen maps the error to a string resource
    data class Content(/* domain models and derived business values */) : HomeUiState
}

sealed interface HomeNavEvent { data class OpenRenter(val id: RenterId) : HomeNavEvent }
```
- A screen exposes exactly one `StateFlow<UiState>` and one `SharedFlow<NavEvent>`. Nothing else.
- `Content` carries domain models and business values already derived by use cases (for example a `RenterStatus` enum, pending as `Paise`, days late as `Int`). **Business derivations live in the domain, never in the UI.**
- **Formatting is not business logic.** Display formatting is done by pure functions inside reusable components (`AmountText(Paise)`, `DateText(LocalDate)`). `Content` never carries pre-formatted strings.
- **₹ formatting uses `expect/actual`** (`formatRupees`): Android `android.icu.text.NumberFormat`, iOS `NSNumberFormatter`, both with the `en-IN` locale, and ICU4J on the test-only JVM target. The JDK's own `NumberFormat` can't group digits the Indian way (it prints ₹124,000), so it must not be used. The `FormatRupeesTest` (₹1,24,000, ₹12,34,567, negatives) runs on the JVM and on the iOS simulator and passes on both; Android shares the ICU engine with the JVM target and was seen formatting ₹7,500 on the emulator.
- `NavEvent` also covers external launches (open WhatsApp, share sheet, call). They are one-shot effects too.

### ViewModel shape (base class `MviViewModel<I, S, N>`)
The base class and its exact operator shape are a **draft until the first slice (Address) proves it**. The rules below are fixed.
- Intents enter through one `MutableSharedFlow<I>`, fed only by `onIntent(i)`.
- State is a pipeline, not mutated. Read-only screens:
  ```kotlin
  override val uiState: StateFlow<HomeUiState> = intents
      .filterIsInstance<SelectAddress>().map { it.id }.onStart { emit(null) }
      .flatMapLatest { id -> observeDashboard(id) }                 // Flow<Outcome<Dashboard>>
      .map<_, HomeUiState> { it.fold(onSuccess = ::Content, onFailure = { e -> Error(e) }) }
      .toState(HomeUiState.Loading)                              // MviViewModel helper: scan + stateIn(Eagerly)
  ```
  There is no `onStart { emit(Loading) }`: `stateIn` already starts at Loading, and a restart keeps the last Content instead of flashing a spinner. There is no `.catch`: see §3.
- **Sharing policy:** every screen is `Eagerly` shared, which `MviViewModel.toState` does for you. With `WhileSubscribed`, a restart after more than 5s in the background makes `scan` start again from its initial value: a form loses what was typed and a list flashes Loading.
- Form screens (Add renter, Reading, Payment) keep input in `Content` and fold intents with `scan`. Each intent is a pure reducer: `(Content, Intent) -> Content`.
- **Writes:** `Save` sets `isSaving = true` in `Content`, is ignored while `isSaving` is true, and runs the use case once. Do not use `flatMapLatest` for a write (a second tap would cancel a write that may already be committed). The `NavEvent` is emitted only after the write returns `Success`; on `Failure` the screen shows the error and `isSaving` goes back to false.
- The navigation `SharedFlow` is `MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = DROP_OLDEST)`, with no replay.
- ViewModels take **use cases and pure helpers only** in their constructor.

### Composables
- `HomeRoute(viewModel)` is the only composable that knows the ViewModel. It does `collectAsStateWithLifecycle()` on the state, collects `NavEvent` in a lifecycle-aware `LaunchedEffect` and calls the navigator, and passes `(state, onIntent)` down.
- `HomeScreen(state, onIntent)` is stateless and previewable with a hand-written `UiState`.
- Children are `private` inside their feature file, unless they're reused.
- **Reusable → `components/`** (`AmountText`, `DateText`, `StatusPill`, `PrimaryBottomButton`, `LabeledField`, `SectionHeader`, `ListCard`). A composable moves there as soon as a second screen needs it, not before.
- UI-only mechanical state (`LazyListState`, focus, animation) is allowed in composables. Anything the business cares about is not.
- Every `*Screen` has a `@Preview` for Loading, Error and Content.
- Text sizes are `sp` and heights are `minHeight`, never fixed, so large system font sizes don't clip. Check each screen at 200% font scale. All user-facing strings come from Compose resources, never hardcoded, so another language can be added later.

## 6. Navigation (Navigation 3)
- Routes are `@Serializable` `NavKey`s. The back stack is owned by one navigator in `navigation/` and rendered by `NavDisplay`.
- A feature never touches the back stack. It emits `NavEvent`s, and the `Route` composable hands them to the navigator.
- Navigation 3 `1.2.0` is stable. The `NavEvent` contract doesn't depend on it, so it could be swapped later.

## 7. Reminders and background work
A local notification can't check "is pending ₹0 yet?" when it fires, so reminders are a **resync**, not a repeating timer:
- `ObserveReminderPlanUseCase` derives the desired schedule from the data: the monthly "create bills" prompt and each late renter's reminder, minus muted renters. Every payment, mute, bill or settings change changes the plan.
- `ReminderScheduler.sync(plan)` (interface in `:domain`, actual per platform) cancels and reschedules, and is idempotent.
- **Named exception to the no-manual-`launch` rule:** one app-scoped collector (`ReminderSync`, started with Koin at app start, with an injected `CoroutineScope`) collects the plan and calls `sync`. `resync()` schedules the current plan again when the phone's permission changed. It's the only place outside ViewModels that collects a flow. The two notification-button handlers also launch once each, as platform-edge handlers.
- **Android:** WorkManager (an exact alarm isn't needed for a reminder around 9 AM) and the `POST_NOTIFICATIONS` runtime permission, requested from Settings. The text comes from the same string resources as the screens (`getString`), and the code lives in `:sharedUI` `androidMain` (the plan, the scheduler, the worker, the action receiver).
- **iOS:** `UNUserNotificationCenter`. Apple limits pending local notifications to 64, so the plan is capped at `ReminderPlanner.MAX_SCHEDULED` (60). iOS silently drops requests made before the person allows notifications, so granting the permission calls `ReminderSync.resync()`.
- **Notification actions** ("Stop this month") enter through a thin platform handler (an Android receiver, an iOS notification delegate) that calls `MuteRemindersUseCase` from Koin and then lets the sync run. The handler contains no logic of its own.

## 8. DI (Koin)
- `:data` exposes only `androidDataModule(context)` and `iosDataModule()` (database, repositories, dispatchers, clock). `sharedUI/di` holds the use-case module (`:domain` stays free of Koin) and the ViewModel module.
- ViewModels are resolved with `koinViewModel()` only in `*Route` composables.
- Constructor injection everywhere, and no service locator calls inside classes.
- Koin starts once from each platform entry: `androidApp` (Application) and `iosApp` (Swift entry, via a Kotlin helper in `sharedUI`).

## 9. Testing and quality gates
- **Tests run on the JVM target** wherever possible: `commonTest` for domain rules (`BillCalculator`, rent increase, pending) and use cases, using in-memory **fakes** for repositories. No mocking framework.
- ViewModels are tested with `runTest` and Turbine against a fake use case or repository. Assert the sequence `Loading → Content`, error and retry, and the `NavEvent`s.
- Data: Room in-memory database tests for DAO queries (JVM, bundled driver), a test for every mapper (entity → domain), plus migration tests for every schema bump.
- Domain logic is written test-first (`mattpocock-skills:tdd`): `BillCalculator`, `RentIncreasePolicy`, `PendingCalculator`.
- Gates: warnings as errors, a Kover line-coverage floor on `:domain` (90%, JVM and Android code only), a Gradle version catalog and convention plugins in `build-logic/`. No detekt, spotless or architecture-test gate.
- CI (GitHub Actions): Linux runs build and all JVM tests plus the Kover check on every push. macOS runs only the iOS framework link. Dependency updates are manual, checked against context7.
- Before a release: `/code-review`, `/security-review`, `mobile-ui-ux-product-review` per screen, and `/ponytail-audit`.
- A PR is not done without tests for new domain logic and a ViewModel test for any new intent.

## 10. Conventions
- Kotlin official style. Immutable by default: `val`, `data class`, stable collections for UI state.
- No `!!`, no `GlobalScope`, no `runBlocking` outside tests, no `Dispatchers.*` outside `AppDispatchers`.
- No `Double` money. No `System.currentTimeMillis`. Use an injected `Clock` and `TimeZone`.
- Public API names say the domain thing, not the technology (`RenterRepository`, not `RenterRoomDao`). Names follow `GLOSSARY.md`.
- Undo, not confirm: destructive or edit actions run immediately and show an Undo for a few seconds. The one action Undo can't cover, restore, takes an automatic snapshot first (`DATABASE.md`).

## 11. Known trade-offs
- **Entity mappers in `:data`:** a small cost, and it keeps the domain free of Room and lets the schema change without touching the domain.
- **`SharedFlow` navigation can drop an event** if nothing is collecting when it's emitted, for example while the app is in the background. See ADR 0005.
- **Text fields have no programmatic label yet.** `BasicTextField` on Android exposes a `Modifier.semantics { contentDescription }` as a separate node next to the field, not on the field's own node (checked with `uiautomator dump`, three placements tried), so it can't act as the field's name. Today the visible header above the field is the only label a screen reader finds. When Material3 is added, use its `TextField` with a `label`, and check the field's accessibility node again with a screen reader before release.
- **Material3 is not a dependency yet.** The bundled version is an alpha. It's added when the date and time pickers are built, and themed to the design. Everything else is on stable releases.
- **`AppDispatchers.io` is `Dispatchers.Default`.** `Dispatchers.IO` isn't public in common code on iOS. Swap in a bounded dispatcher if blocking work ever starves the CPU pool.
- **`-Xexpect-actual-classes`** is enabled in `:data` because Room's generated `AppDatabaseConstructor` is an expect/actual object.
- **Eagerly-shared form state** keeps flows collecting while the app is in the background. At this data size the cost is negligible.
- **"No imperative code"** applies to our code. The platform edges (`expect/actual`, OS callbacks) get wrapped once into `Flow`s or suspend functions, and nothing leaks beyond that wrapper.
