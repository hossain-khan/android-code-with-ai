# Repository Reviewer Guidelines - CodeMateX

Please enforce the following project-specific conventions and architecture constraints when reviewing pull requests:

## 1. Typography & Punctuation
- **No Em-Dash:** Never use the U+2014 em-dash character; always use a regular dash (`-`) across all code, comments, documentation, release notes, commit messages, and UI strings. Flag any em-dashes immediately.

## 2. Critical Memory & JNI Lifecycle Constraints
- **Callback Object Retention (JNI GC Race):** Always retain JNI callback structures (such as `MessageCallback` passed to `sendMessageAsync`) in a class-level member variable (e.g. `activeCallback` in `LlmEngineImpl`). Local callback instances in coroutines can be garbage-collected while native threads are unwinding, causing SIGSEGV crashes.
- **Defensively Wrap JNI Inputs:** Wrap token extraction and parsing inside `onMessage` callbacks in defensive `try-catch` blocks. Unhandled exceptions in JNI threads cause immediate process termination.
- **Sequential Async Message Seeding:** Async operations like context/history restoration (`restoreHistory`) must be fully suspended using `suspendCancellableCoroutine` until `onDone()` or `onError()` triggers. LiteRT-LM does not support concurrent `sendMessageAsync` calls on the same `Conversation` session.
- **Hardware Fallback Exception Handling:** Native LiteRT-LM exceptions throw `com.google.ai.edge.litertlm.LiteRtLmJniException` rather than standard Java exceptions. Catch this specific exception type during multi-tier fallback (NPU -> GPU -> CPU).

## 3. Circuit UDF & Metro DI
- **Presenters:**
  - Must annotate with `@CircuitInject`.
  - Must emit an immutable `State` data class with an `eventSink: (UiEvent) -> Unit` property.
  - Presenters must not reference Compose UI nodes or View types.
- **UI Composables:**
  - Pure functions receiving only `State` and `Modifier`.
  - No direct repository, database, or network access inside UI composables.
- **Dependency Injection:**
  - Use Metro DI (`@ContributesBinding`, `@ContributesTo(AppScope::class)` with `@BindingContainer`).
  - Scopes: Standardize on `AppScope`, `@ApplicationContext`, `@ActivityKey`, `@WorkerKey`.
  - Metro FIR compiler plugin generates Circuit and DI code; no KSP processors needed for DI or Circuit.

## 4. UI/UX Design System & Adaptive Guidelines
- Follow Material 3 Expressive and Material You Adaptive design specifications.
- **Surface Container Hierarchy:**
  - `surfaceContainerLow`: Standard cards (`TopicCard`, `SessionCard`, `ModelCard`).
  - `surfaceContainer`: Top app bars, bottom chat input dock, dialog surfaces.
  - `surfaceContainerHigh` / `surfaceContainerHighest`: Hero banners, highlighted benchmarking panels, chips, glyph badges.
- **Card Borders:** Cards must use subtle borders with `BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))`. Do not use `border` on `ElevatedCard`.
- **Adaptive Multi-Pane:** Never design exclusively for compact screens; check `WindowSizeClass` and support multi-pane/grid layouts on expanded screens.
- **Indicators:** Use `CircularWavyProgressIndicator` and `LinearWavyProgressIndicator` for loading/streaming states.
- **Previews:** Every new or updated composable must include comprehensive `@ThemePreviews` (and `@DevicePreviews` for full screens).

## 5. Circuit Shared Element Transitions
- **Dual Composable Entry:** Since `@CircuitInject` screen entry functions cannot take `SharedElementTransitionScope`, use the outer injector + inner implementation pattern.
- **Defensive Modifiers:** Use `Modifier.sharedBoundsNav()` and `Modifier.sharedElementNav()` from `SharedElementTransitions.kt` which safely no-op when `transitionScope == null`.
- **Scoped Keys:** Define all shared element transition keys in `SharedElementTransitions.kt` (singleton `data object` for screen-unique items, `data class` with unique IDs for dynamic list items).

## 6. Syntax Highlighting & Markdown
- Code blocks are rendered with `dev.hossain:compose-highlight` (`StreamingSyntaxHighlightedCode` or `SyntaxHighlightedCode`).
- Trailing actions must use the `actions: RowScope.(onCopy) -> Unit` slot rather than the removed `copyButton` parameter.
- Full custom chrome must use the `header: (onCopy) -> Unit` slot.
- Markdown messages must use `MarkdownMessage` and be wrapped by a root `HighlightThemeProvider`.

## 7. PR Hygiene & Auto-Merge Policy
- **Never Enable Auto-Merge:** Pull requests must remain open for manual review by the repository owner.
- **Verification:** Ensure `./gradlew formatKotlin && ./gradlew check` passes cleanly.
