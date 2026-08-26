# CodeMateX Audit Remediation Re-Audit

**Re-audit date:** 2026-08-26  
**Revision reviewed:** `e01c297` (`main`, release `1.12.0`)  
**Original audit:** [`KOTLIN_ARCHITECTURE_AUDIT.md`](KOTLIN_ARCHITECTURE_AUDIT.md)  
**Issues reviewed:** `#145` through `#157`

## Executive Verdict

The remediation materially improved the codebase, but the epic is not complete under its
published acceptance criteria.

- **Complete:** 4 child issues (`#146`, `#149`, `#150`, `#155`)
- **Partial:** 7 child issues (`#145`, `#147`, `#148`, `#151`, `#152`, `#154`, `#156`)
- **Waived by project owner:** 1 child issue (`#153`)
- **Epic `#157`: Not complete**

The highest-risk remaining defect is in the JNI terminal callback path: an application callback
exception can leave inference suspended forever. Android integration and Compose tests from `#153`
are intentionally outside the accepted project scope; JVM unit tests are the required testing
standard for this re-audit.

## Findings

### High: JNI terminal callbacks can still hang inference

`SafeCallback.onDone()` calls `onTerminal()` and resumes the continuation in the same `try` block:

- [`LlmEngineImpl.kt`](../app/src/main/java/dev/hossain/codematex/runtime/LlmEngineImpl.kt#L414)

If `onTerminal()` throws, the exception is logged at line 420 but `resume(Unit)` is never reached.
The coroutine remains suspended while the terminal guard prevents later callbacks from completing
it. The cancellation branch of `onError()` has the same ordering problem at lines 428-442.

The callbacks catch `Exception`, not `Throwable`, so they also do not strictly meet the issue's
"cannot throw across JNI" acceptance criterion.

Existing tests cover a throwing content consumer and duplicate/late terminal callbacks, but not a
throwing terminal consumer:

- [`LlmEngineImplTest.kt`](../app/src/test/java/dev/hossain/codematex/runtime/LlmEngineImplTest.kt#L641)

**Affected issue:** `#145`  
**Required correction:** Dispatch terminal notifications inside their own no-throw boundary and
complete the continuation independently, preferably with `tryResume`/`completeResume`.

### Medium: Runtime fallback still misclassifies failures

History restoration catches every `Exception`:

- [`LlmEngineImpl.kt`](../app/src/main/java/dev/hossain/codematex/runtime/LlmEngineImpl.kt#L284)

Consequences:

- Coroutine cancellation can be swallowed on CPU.
- Programming/state errors on hardware are treated as backend failures and trigger CPU fallback.
- CPU restoration errors are logged and converted into apparent initialization success.

Only `BackendFailureException` should trigger fallback; cancellation must be rethrown.

Inference-time fallback also recreates CPU directly:

- [`LlmEngineImpl.kt`](../app/src/main/java/dev/hossain/codematex/runtime/LlmEngineImpl.kt#L123)
- [`LlmEngineImpl.kt`](../app/src/main/java/dev/hossain/codematex/runtime/LlmEngineImpl.kt#L167)

An active NPU inference failure therefore skips GPU, contrary to the promised NPU -> GPU -> CPU
chain. Initialization fallback itself correctly skips known unsupported backends.

**Affected issue:** `#147`

### Medium: Throughput is still not a valid token decode metric

The monotonic clock and empty-terminal overcount were fixed. However:

- Each `MessageCallback.onMessage()` invocation is counted as one token without establishing that
  the callback is token-granular.
- Decode speed uses `tokenCount / elapsedSinceFirstToken`. If callbacks are tokens, the first token
  belongs to TTFT and the decoded count over that interval is normally `tokenCount - 1`.
- Failed-backend callbacks remain in the tracker after `BackendFailed`, overstating the final CPU
  attempt.

See:

- [`ThroughputTracker.kt`](../app/src/main/java/dev/hossain/codematex/data/ThroughputTracker.kt#L29)
- [`ThroughputTracker.kt`](../app/src/main/java/dev/hossain/codematex/data/ThroughputTracker.kt#L48)
- [`ChatPresenter.kt`](../app/src/main/java/dev/hossain/codematex/ui/screens/chat/ChatPresenter.kt#L170)

The bundled LiteRT-LM `Conversation` API exposes `BenchmarkInfo`, including actual TTFT,
prefill/decode token counts, and decode tokens/second, but the runtime wrapper does not expose it:

- [`InferenceEngine.kt`](../app/src/main/java/dev/hossain/codematex/runtime/InferenceEngine.kt#L34)

**Affected issue:** `#152`

### Medium: Persona and configuration can diverge from the active runtime

Persona selection updates visible state before persistence and runtime switching. A DataStore write
failure exits the coroutine before `switchPersona()` and is not surfaced:

- [`ChatPresenter.kt`](../app/src/main/java/dev/hossain/codematex/ui/screens/chat/ChatPresenter.kt#L250)

The UI can then show the new persona while inference still uses the old system prompt. Initial
persona loading can also race model initialization because persona is not a key of the
initialization effect:

- [`ChatPresenter.kt`](../app/src/main/java/dev/hossain/codematex/ui/screens/chat/ChatPresenter.kt#L64)
- [`ChatPresenter.kt`](../app/src/main/java/dev/hossain/codematex/ui/screens/chat/ChatPresenter.kt#L98)

Model configuration remains incomplete:

- `ModelConfigOverlay` has no production caller.
- Store updates change displayed text but do not reset/reinitialize the active conversation.
- The presenter test verifies text changes, not propagation to runtime initialization.

See:

- [`ModelConfigOverlay.kt`](../app/src/main/java/dev/hossain/codematex/ui/overlay/ModelConfigOverlay.kt#L30)
- [`ModelConfigStore.kt`](../app/src/main/java/dev/hossain/codematex/data/repository/ModelConfigStore.kt#L15)
- [`ChatPresenterTest.kt`](../app/src/test/java/dev/hossain/codematex/ui/screens/chat/ChatPresenterTest.kt#L523)

**Affected issues:** `#151`, `#156`

### Medium: Permanent download failures still advance through mirrors

The final install is now checked and permanent/transient worker failures are typed. However, the
multi-URL loop tries the next mirror after every failure except cancellation:

- [`HttpModelDownloader.kt`](../app/src/main/java/dev/hossain/codematex/work/HttpModelDownloader.kt#L56)

That includes insufficient storage, installation failure, malformed input, and checksum mismatch.
This conflicts with the acceptance criterion that permanent failures must not retry. A test
explicitly expects fallback after checksum mismatch, codifying behavior opposite to the issue.

The WorkManager tests exercise extracted JVM logic, not a real `ModelDownloadWorker`:

- [`ModelDownloadWorkerTest.kt`](../app/src/test/java/dev/hossain/codematex/worker/ModelDownloadWorkerTest.kt#L12)

**Affected issue:** `#148`

### Verified: Room transaction and migration behavior

`SessionDao.replaceSession()` is now a proper Room `@Transaction`, release destructive fallback is
disabled, migrations are explicit, persisted IDs are stable, and UUID session IDs are used:

- [`SessionDao.kt`](../app/src/main/java/dev/hossain/codematex/data/local/SessionDao.kt#L33)
- [`DatabaseGraph.kt`](../app/src/main/java/dev/hossain/codematex/data/local/DatabaseGraph.kt#L12)
- [`ChatSessionRepositoryImpl.kt`](../app/src/main/java/dev/hossain/codematex/data/repository/ChatSessionRepositoryImpl.kt#L39)

Under the project's unit-test-first verification policy, the DAO transaction boundary, repository
tests, exported schemas, and passing migration tests are sufficient for this issue.

### Low: Quality cleanup acceptance criteria remain open

The return-value checker is enabled, two dead classes were removed, and
`failOnNoDiscoveredTests = false` was removed. Remaining gaps:

- Trial diagnostics/rationale are not documented beyond an inline Gradle comment.
- The checker is enabled alongside warnings-as-errors without documented triage.
- Retrofit and its serialization converter remain without a production consumer.
- The direct lifecycle runtime dependency has no production import.
- Lint still reports the original actionable `ModifierParameter` warning.
- All three `UsableSpace` warnings remain.

See:

- [`app/build.gradle.kts`](../app/build.gradle.kts#L122)
- [`app/build.gradle.kts`](../app/build.gradle.kts#L198)
- [`EmptyChatTopicStarters.kt`](../app/src/main/java/dev/hossain/codematex/ui/screens/chat/EmptyChatTopicStarters.kt#L37)
- [`HttpModelDownloader.kt`](../app/src/main/java/dev/hossain/codematex/work/HttpModelDownloader.kt#L37)

**Affected issue:** `#154`

## Per-Issue Status

| Issue | Verdict | Re-audit result |
|---|---|---|
| `#145` JNI/native lifecycle | **Partial** | Resource closure, retention, duplicate terminals, and content-consumer safety improved; terminal-consumer hangs and broad callback throwable safety remain. |
| `#146` transactional persistence | **Complete** | Transaction boundary, release migration policy, stable IDs, UUIDs, schemas, and migration tests are present. |
| `#147` backend fallback | **Partial** | Partial-output reset works; history error typing and NPU inference fallback remain incorrect. |
| `#148` atomic downloads | **Partial** | Checked install and worker classification landed; permanent errors still advance through mirrors and WorkManager integration is untested. |
| `#149` RAM policy | **Complete** | Byte-based policy, reserved-memory allowance, presenter boundary, and 8/12 GB tests are present. |
| `#150` history/summary isolation | **Complete** | Order is preserved and summary inference uses a separately closed conversation on the loaded engine. |
| `#151` chat state machine | **Partial** | Reset ID and save retry are fixed; no explicit phase model, and persona/reset failures can leave UI/runtime state inconsistent. |
| `#152` throughput metrics | **Partial** | Monotonic timing and terminal filtering landed; token semantics, decode math, and retry reset remain inaccurate. |
| `#153` Android test coverage | **Waived** | The project owner explicitly accepts JVM unit tests as sufficient and does not require the proposed Android test expansion. |
| `#154` quality cleanup | **Partial** | Some cleanup landed; checker documentation, dead dependencies, and named actionable lint findings remain. |
| `#155` presenter states | **Complete** | Error/not-found/retry states, stable keys, previews, and composition-safe filtering are present. |
| `#156` preferences/config boundary | **Partial** | Preference writes are awaited and tested; configuration UI/runtime propagation is not complete. |
| `#157` remediation epic | **Not complete** | Several production correctness and architecture criteria remain unsatisfied; Android test expansion is not a blocker. |

## Verified Improvements

- History restoration preserves alternating user/agent order.
- Summary inference uses an isolated conversation and closes it.
- Failed native sessions and partial factory resources are closed.
- Partial backend output is cleared before retry output.
- Release builds do not enable destructive Room migration fallback.
- Preference writes await DataStore and propagate write failures.
- Presenter error/not-found states and retry actions are implemented.
- Stable keys are used for session-detail messages.
- RAM decisions use one decimal-byte policy with realistic 8/12 GB boundaries.
- Chat reset clears the persisted session identity.
- Persistence failures preserve generated assistant output and expose retry.

## Verification Results

### JVM/build verification

```text
./gradlew check
BUILD SUCCESSFUL in 51s
244 tests, 0 failures, 0 errors, 0 skipped
Kover verification passed
```

Android lint passed with **80 warnings**, including one `ModifierParameter` warning and three
`UsableSpace` warnings.

### Android verification

```text
./gradlew connectedDebugAndroidTest
Pixel 9 Pro XL emulator, Android 16 / API 36
3 tests, all passed
```

All three tests are Room migration tests. Broader instrumentation and Compose tests are not
required by the current project policy. An attached physical device was unauthorized and was
skipped; the emulator completed normally.

## Recommended Reopening Order

1. Reopen `#145` for JNI terminal completion and cancellation/error typing.
2. Reopen `#152` and use LiteRT `BenchmarkInfo` rather than callback-count estimates.
3. Reopen `#151` and `#156` together for one explicit chat operation state and runtime config/persona propagation.
4. Reopen `#148` to stop mirror fallback on permanent failures.
5. Finish `#154` after correctness work so cleanup does not obscure behavioral changes.
