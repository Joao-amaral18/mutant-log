# Mutant Log agent guide

## Product

Mutant Log is a local-first Android workout tracker for training programs, readiness, live workout logging, progression, rest timing, history, volume analysis, cardio, and JSON export. The shipped Room catalog is reference data; workouts and progress belong to the user.

## Start here

Trace a change through this path before editing:

`Compose screen -> MutantViewModel -> repository/DAO -> Room -> Flow -> Compose screen`

The app has one Gradle module, `app`, and one activity. `MainActivity.kt` owns edge-to-edge setup, notification intents, and an enum-based Compose navigation shell. There is no Navigation Compose graph.

Use these source areas as the map:

- `ui/screens`: product surfaces. `HomeScreen` manages protocols and workout entry; `ActiveWorkoutScreen` is the live session; `CompletedWorkoutEditor` edits recorded sessions; the remaining screens cover the exercise dossier and cardio.
- `ui/components`: cross-screen dialogs, the persisted rest timer UI/alerts, plate visualization, and `WorkoutTimerService`.
- `ui/designsystem`: canonical color, type, spacing, shape, motion tokens, and reusable `Mutant*` components.
- `ui/theme`: the app theme entry point plus compatibility aliases used by older screen code.
- `ui/viewmodel/MutantViewModel.kt`: application-scoped orchestration and UI state. It is intentionally the shared coordinator for all screens.
- `data/repository`: workflow boundaries. `MutantRepository` handles workout/program mutations; `AnalysisExportRepository` builds the shareable JSON snapshot.
- `data/db`: Room DAO/database, migrations, and bundled catalog/template data.
- `data/model`: Room entities and pure recommendation/progression logic.

## Data and workflow invariants

- Treat Room as the source of truth. UI clocks may tick locally, but active session, exercise index, next-set plan, and rest-timer state are persisted and observed through flows.
- A session is active when `finishedAt == null`; a completed session has a non-null `finishedAt`.
- Keep multi-table lifecycle operations transactional. Workout creation, completion, discard, completed-workout editing, routine progress, cascades, and personal-record rebuilding must remain internally consistent.
- The foreground timer service, notification actions, `MainActivity` intents, `RestTimerAlerts`, DAO timer commands, and active-workout UI form one feature. Check all of them when changing timer or notification behavior.
- Starting a workout waits until Room emits both the new session and its exercises. Preserve that loading contract so the UI does not briefly show an empty-workout state.
- Discarding is serialized against workout mutations with the view-model mutex. It removes only an active session, relies on Room cascades for dependent workout data, clears timer state/notifications, and keeps completed history.
- Completed-workout editing uses a detached `WorkoutDraft` and `updatedAt` optimistic concurrency. Saving normalizes exercise/set order and rebuilds personal records, including later records affected by historical edits.
- Finishing a programmed workout also persists the completed training-day position. Recommendations exclude rest days and prefer completed routine history, then persisted position, then the local calendar fallback.
- Seed only canonical reference data and default gyms on an empty catalog. A fresh install must have zero fabricated programs, workouts, sets, cardio, bodyweight, or progress.
- When an entity/schema changes, increment the Room version and add every required migration to `MutantDatabase`; there is no destructive-migration fallback. Add a migration test when the change is non-trivial.
- Keep pure rules such as progression and workout recommendation in `data/model`; keep database access in DAO/repositories and Android lifecycle work in the view model/service.
- Preserve cancellation by rethrowing `CancellationException` in broad coroutine error handlers.

## UI conventions

- Reuse `ui/designsystem/components` and canonical tokens before adding screen-local styling. New foundational tokens belong in `ui/designsystem`; `ui/theme` is a compatibility bridge.
- `MyApplicationTheme` in `ui/theme/Theme.kt` is the activity-level wrapper. A second `MutantTheme` exists in `ui/designsystem`, so use explicit imports when the name could be ambiguous.
- Keep the dark, high-contrast purple visual language and semantic success/warning/error colors. Support edge-to-edge and system insets.
- Hoist durable state into Room/view-model flows. Use `remember` only for transient presentation state such as an open dialog or unsaved field input.
- Preserve existing test tags and add stable tags for important interactions that text selectors cannot express.
- Large screens are already dense. Prefer extracting a focused composable over extending `HomeScreen` or `ActiveWorkoutScreen` with another unrelated block.

## Build and verification

Use JDK 21. Android SDK 36 Robolectric tests fail during sandbox setup on Java 17. On this Windows setup, Android Studio's bundled runtime is `C:\Program Files\Android\Android Studio\jbr`.

Run from the repository root:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Use the smallest relevant test during iteration, then run `testDebugUnitTest` before handoff. The JVM suite includes pure progression tests plus Robolectric coverage for Room cascades, workout startup, Compose dialogs, and notifications. Use `connectedDebugAndroidTest` only when a device/emulator is available and the change needs instrumentation coverage.

For database/workout changes, completion means tests cover the success path and the relevant rollback, cascade, process-reload, or stale-write edge. For UI changes, compile the app and inspect the affected screen at compact and normal phone sizes; screenshots in `app/` are references, not golden tests.

## Configuration and safety

- `.env` and `local.properties` are local-only. Never print or commit secrets. `.env.example` defines the expected shape.
- `GEMINI_API_KEY` is wired through the secrets plugin, but current core workout flows are local-first. Do not introduce a network dependency into them without an explicit product requirement.
- Release signing reads `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD`. Do not weaken or hard-code signing configuration to make a local build pass.
- Preserve unrelated staged and unstaged work. This repository may be used as a live design/workout prototype; inspect `git status` and the relevant diff before modifying a file.
