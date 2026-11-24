# Rent Manager

Read `PLAN.md` (product), `ARCHITECTURE.md` (engineering contract) and `DATABASE.md` (schema) before changing code. The visual spec is `design/project/*.dc.html`.

Always use the latest versions and docs. Before writing any setup code or adding or bumping a dependency, query context7 for current docs. Don't rely on memory. The structure is AGP 9 / current KMP: `androidApp` + `iosApp` + `sharedUI` + `data` + `domain`, with the `com.android.kotlin.multiplatform.library` plugin.

Commands (run from the repo root, JDK 17+):
- All tests: `./gradlew :domain:jvmTest :data:jvmTest :sharedUI:jvmTest`
- Android app: `./gradlew :androidApp:assembleDebug`
- iOS framework: `./gradlew :sharedUI:linkDebugFrameworkIosSimulatorArm64`
- iOS app: `cd iosApp && xcodegen generate`, then build `iosApp.xcodeproj` for a simulator running iOS 16 or newer.
- The wrapper is Gradle 9.6.0. The system `gradle` (9.1) can't load the AGP 9.5 plugins, so always use `./gradlew`.

Non-negotiable rules:
- MVI, unidirectional. One `StateFlow<UiState>` and one `SharedFlow<NavEvent>` per screen. `UiState` is `sealed interface { Loading, Error, Content(data class) }`.
- Composables are dumb. They render `UiState` and call `onIntent`. All logic lives in the ViewModel.
- ViewModels depend on use cases only, never repositories or DAOs.
- Reactive only in ViewModels, domain and data: Flow and coroutines, with no `var`, no manual `launch {}`, and no loops over state. Compose UI-only state is allowed. The only other `launch` is the app-scoped reminder collector.
- Errors are values: `Outcome<T>`, caught once in the data layer (`safeFlow` / `safeCall`). No `try/catch` or `.catch` in ViewModels or use cases.
- Every screen is `Eagerly` shared (`MviViewModel.toState` does it). Writes ignore a second tap while `isSaving`, and navigate only after success.
- ViewModels and UI use domain models directly, with no presentation mappers or UI models. Room entities are separate, live in `:data` only, and are mapped by internal mappers there.
- Business derivations (status, days late, pending) live in the domain. Display formatting (₹, dates) lives in reusable components. ₹ formatting uses `expect/actual`.
- Reusable composables go in `components/`. Single-use ones stay `private` in their feature file.
- Money is `Long` paise in a value class. Pending is computed, never stored. Bills are snapshots, created only by the landlord.
- Time comes from an injected `Clock` and `TimeZone`, and dispatchers from `AppDispatchers`.
- Text is `sp`, heights are `minHeight`, strings come from resources.
- Tests: fakes, not mocks, on the JVM target. New domain logic needs tests (written first), and every new intent needs a ViewModel test.

Where this file or `ARCHITECTURE.md` conflicts with ponytail's "write the minimum" (one use case per action, previews, tests, contracts), the architecture wins. Ponytail still applies everywhere else: no extra features, no speculative abstraction.

Vocabulary is in `GLOSSARY.md`: use those terms for names in code, UI and docs (Renter, Address, Unit, Pending, Bill line...). Decisions are in `docs/adr/`. Don't reverse one without writing a new ADR.

Workflow for every change:
1. Write the code with ponytail (smallest complete change), following `ARCHITECTURE.md` and Google-level Kotlin and Compose practice.
2. Review it with `/ponytail:ponytail-review` before calling it done, and fix what it finds.
3. After the changes are pushed, run `/graphify` once to refresh the knowledge graph. Commit and push only when the user asks.
