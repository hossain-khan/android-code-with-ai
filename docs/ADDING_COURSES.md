# Adding Guided Learning Courses

This guide explains how to add a new programming-language course to CodeMateX while
keeping content, progress tracking, navigation, UI, and testing consistent.

The current Guided Lessons implementation is a bundled, offline-first learning
experience. Course content is compiled into the app, while learner progress is stored
in Room.

## Current architecture

The feature is split into four layers:

```text
Course content
    ↓
LearningRepository
    ↓
Circuit presenters and screens
    ↓
Room lesson progress
```

Important existing files:

- `data/model/LearningModels.kt` — Course, chapter, lesson, block, and progress models.
- `data/repository/course/KotlinCourseContent.kt` — Bundled Kotlin Foundations course.
- `data/repository/course/PythonCourseContent.kt` — Bundled Python Foundations course.
- `data/repository/course/TypeScriptCourseContent.kt` — Bundled TypeScript Foundations course.
- `data/repository/course/GoCourseContent.kt` — Bundled Go Foundations course.
- `data/repository/course/RustCourseContent.kt` — Bundled Rust Foundations course.
- `data/repository/course/LearningRepository.kt` — Course/progress repository contract.
- `data/repository/course/LearningRepositoryImpl.kt` — Bundled content lookup and progress behavior.
- `data/local/LessonProgressEntity.kt` — Room progress entity.
- `data/local/LessonProgressDao.kt` — Progress DAO.
- `ui/screens/lessons/` — Catalog, chapter, and lesson screens.
- `data/local/SessionDatabase.kt` — Shared Room database and migrations.
- `docs/DESIGN_GUIDELINES.md` — Required UI/UX rules.

## Course content model

A course contains ordered chapters, and each chapter contains ordered lessons:

```text
Course
└── Chapter
    └── Lesson
        └── LessonBlock
```

Supported lesson blocks currently are:

- `LessonBlock.Markdown`
- `LessonBlock.Code` — carries a `runnable` flag (default `true`)
- `LessonBlock.Quiz`

Keep lessons focused. A good lesson should teach one concept, show a small example,
and end with a useful check or practice prompt.

### Marking non-runnable code blocks

`LessonBlock.Code(language, code, runnable = true)` includes a `runnable` flag that controls
whether the [automated content validation](#automated-content-validation-ci) compiles or lints the
snippet in CI. Leave it `true` for a normal, self-contained example. Set it to `false` for a
deliberate fragment that cannot stand alone, so the validator skips it instead of reporting a false
failure. Typical fragments:

- A multi-file illustration concatenated into one block (for example `// format.ts` + `// app.ts`,
  or `# greetings.py` + `# app.py`).
- A configuration file shown as code (for example a `tsconfig.json`).
- A test-only snippet with no entry point (for example a Go file with only `TestXxx` functions).

Course content builders expose this through a `codeRunnable` parameter on their `lesson(...)` helper:

```kotlin
lesson(
    id = "typescript-modules",
    // ...
    code = "...",
    codeRunnable = false, // multi-file illustration; not a standalone compilable unit
)
```

When you set `codeRunnable = false`, add a matching assertion to the course's exporter test (see
[Automated content validation](#automated-content-validation-ci)) so the skip is intentional and
documented.

## Workflow for adding a course

### 1. Gather and inspect the source material

When the course is based on a PDF or other reference:

1. Confirm that the source is available and permitted for use.
2. Extract or inspect the table of contents.
3. Separate language fundamentals from tooling, frameworks, release notes, and
   platform-specific material.
4. Build a learner-oriented syllabus rather than copying the source page-by-page.
5. Prefer original explanations and short examples.

For a first course version, prioritize:

- Syntax and core concepts
- Common idioms
- Progressive difficulty
- Small runnable examples
- Practice and knowledge checks

Do not import a large reference document directly into the APK. Distill it into
structured lessons.

### 2. Define stable IDs

IDs are used by Room progress records and must remain stable after release.

Use lowercase, descriptive, hyphen-separated IDs:

```text
python-foundations
python-variables
python-functions
python-nullability
```

Rules:

- Course IDs must be globally unique.
- Chapter IDs must be unique within the course.
- Lesson IDs must be globally unique across all bundled courses.
- Never derive IDs from display titles at runtime.
- Do not rename an existing ID just because the visible title changes.
- If a lesson is substantially replaced, decide whether progress should carry over
  before changing its ID.

### 3. Add the bundled course content

Create a dedicated content file rather than putting a second language inside
`KotlinCourseContent.kt`.

Recommended naming:

```text
data/repository/course/PythonCourseContent.kt
data/repository/course/RustCourseContent.kt
data/repository/course/JavaScriptCourseContent.kt
```

Each content object should expose one stable course:

```kotlin
package dev.hossain.codematex.data.repository.course

object PythonCourseContent {
    const val COURSE_ID = "python-foundations"

    val course =
        LearningCourse(
            id = COURSE_ID,
            language = "Python",
            title = "Python Foundations",
            description = "...",
            version = 1,
            chapters = listOf(
                // Ordered chapters and lessons.
            ),
        )
}
```

Use a shared helper pattern so every lesson receives its parent chapter ID:

```kotlin
private fun chapter(
    id: String,
    order: Int,
    title: String,
    description: String,
    lessons: List<LearningLesson>,
) =
    LearningChapter(
        id = id,
        courseId = COURSE_ID,
        order = order,
        title = title,
        description = description,
        lessons = lessons.map { it.copy(chapterId = id) },
    )
```

Do not leave `chapterId` empty. Progress and future lesson-aware navigation depend
on the relationship being populated.

### 4. Write lesson content consistently

Each lesson should normally include:

1. A concise Markdown explanation.
2. One or more short code examples.
3. A quiz or practice-oriented check where appropriate.
4. An estimated completion time.

Recommended lesson structure:

```text
Concept
Why it matters
Small example
Common mistake
Knowledge check
```

Keep code examples:

- Short enough to read on a phone.
- Valid for the language/version being taught.
- Focused on the lesson concept.
- Annotated when syntax may be unfamiliar.

Escape `$` characters inside Kotlin raw strings when the example itself contains
string templates:

```kotlin
println("${'$'}name")
```

Otherwise, the course source will attempt to interpolate the example while compiling
the app.

### 5. Register the course in the repository

The current repository returns a static list from
`LearningRepositoryImpl.getCourses()`.

When adding another course, update the bundled course list and the lookup methods:

```kotlin
private val bundledCourses =
    listOf(
        KotlinCourseContent.course,
        PythonCourseContent.course,
        TypeScriptCourseContent.course,
        GoCourseContent.course,
        RustCourseContent.course,
    )
```

Then ensure these methods search all bundled courses rather than only Kotlin:

- `getCourses()`
- `getCourse(courseId)`
- `getChapter(chapterId)`
- `getLesson(lessonId)`
- `observeCourseProgress(courseId)`
- `markLessonStarted(lessonId)`
- `markLessonCompleted(lessonId)`

Progress writes must use the lesson’s actual `courseId`. Do not hardcode the Kotlin
course ID in shared repository logic.

For more than a few courses, refactor the repository to use a
`BundledLearningContentDataSource` and a course index instead of growing one
repository file.

## Suggested local verification

Before bundling new course materials, validate every code example with the
language's local compiler and formatter when available. For Go, Rust, TypeScript,
Python, and Kotlin this is now also enforced automatically in CI (see
[Automated content validation](#automated-content-validation-ci)), so running these
locally first is the fastest way to catch problems before opening a pull request.

For Go examples, run:

```bash
gofmt -w .
go test ./...
go vet ./...
```

For runnable examples, also run the relevant package with `go run .`. Keep
validation projects isolated per lesson when examples define their own `main`
function or package-level declarations.

For Rust examples, run:

```bash
rustfmt --check src/main.rs
cargo check
cargo test
cargo clippy -- -D warnings
```

Use a separate temporary Cargo project for each lesson when examples define their
own `main` function or crate-level items.

## Progress and database rules

Lesson progress is stored in the `lesson_progress` Room table. Adding course content
does not require a database migration because the schema is course-agnostic.

A migration is required only when changing the progress schema, for example:

- Adding quiz scores
- Adding bookmarks
- Adding notes
- Adding attempt history
- Changing progress timestamps

When changing the Room schema:

1. Increment the database version in `SessionDatabase.kt`.
2. Add a migration.
3. Register it in `DatabaseGraph.kt`.
4. Update the fake database used by unit tests.
5. Add or update migration tests.
6. Generate and commit the new Room schema JSON.
7. Copy the schema into the Android test assets when required by
   `MigrationTestHelper`.

Never use a destructive migration for release data.

## UI and navigation requirements

The existing screens are course-agnostic and should normally not need changes for a
new course:

- `LessonCatalogScreen`
- `ChapterScreen`
- `LessonScreen`

When adding a new course, verify that:

- The course appears in the catalog.
- Its progress is shown independently.
- Chapter and lesson navigation uses the correct IDs.
- Completing a lesson updates the correct course.
- Resetting one course does not reset another.
- Reopening a completed lesson does not downgrade it to `IN_PROGRESS`.

Follow `docs/DESIGN_GUIDELINES.md` for every new or modified composable:

- Use `surfaceContainerLow` for standard cards.
- Use `surfaceContainer` for top app bars and major surfaces.
- Add the required subtle `BorderStroke`.
- Use `radialGradientScrim()` with the language/topic accent.
- Use `CircularWavyProgressIndicator` and `LinearWavyProgressIndicator`.
- Use adaptive layouts with `currentWindowAdaptiveInfoV2()`.
- Use `GridCells.Adaptive(minSize = 340.dp)` for expanded grids.
- Provide expressive empty states.
- Use `MarkdownMessage` and `StreamingSyntaxHighlightedCode`.
- Add `@ThemePreviews` and `@DevicePreviews`.
- Wrap previews in `CodeWithAIAppTheme(dynamicColor = false) { Surface { ... } }`.

If a language needs a visual identity, add it to the existing topic metadata or
introduce a dedicated course visual metadata model instead of hardcoding colors in
individual screens.

## Q&A integration ("Ask AI Tutor")

Guided Lessons and open-ended Q&A are interconnected seamlessly:

- Lessons provide the curated offline learning path and knowledge checks.
- Each lesson provides an "Ask AI about this lesson" action that deep-links into `ChatScreen`.
- To avoid cluttering the user's permanent session history with transient study queries, lesson Q&A launches with `saveToHistory = false`.
- The chat session is pre-seeded with the lesson's title, course context, and summary, allowing the on-device AI tutor to answer deep questions, explain nuances, or generate interactive practice problems.
- Course theming is dynamic: `course.topic.visualInfo` provides matching ambient lighting and badge colors for all supported programming languages.

## Suggested local verification before release

Before publishing a new course or bundling new quiz material, perform a local Kotlin
compiler audit for every question that has executable behavior.

The recommended local tool is:

```bash
kotlinc-jvm -version
```

For content based on Kotlin 1.9.x, validate snippets with the compatible language
version:

```bash
kotlinc-jvm -language-version 1.9 Example.kt -include-runtime -d Example.jar
java -jar Example.jar
```

Use local compiler checks for:

- Expected program output
- Collection behavior
- Null-safety expressions
- Type inference and assignments
- Function and lambda behavior
- Whether a snippet should compile or fail
- Coroutine return types and behavior when the required dependency is available

For example, a quiz claiming that a `Set` removes duplicates should be backed by a
snippet that checks the resulting size. A quiz claiming that `async` returns a
`Deferred` should compile with an explicit `Deferred<T>` assignment.

Compiler verification does not prove the correctness of conceptual or stylistic
answers, such as whether `val` should be preferred over `var`. Those questions still
require source cross-reference and human review of the explanation.

The Kotlin course is now covered by the [automated content validation](#automated-content-validation-ci),
which compiles each runnable snippet with `kotlinc`. That CI check confirms snippets *compile*; this
local audit is still worthwhile for verifying runtime *behavior* (expected output, collection results,
null-safety) and for the coroutine snippets that CI skips because they need `kotlinx.coroutines` on the
classpath. Do not execute arbitrary learner-provided code; only run trusted course snippets during
authoring.

## Testing checklist

Add unit tests for each new course content provider:

- Course ID is stable.
- Course has at least one chapter and lesson.
- Chapter order is increasing.
- Lesson order is increasing within each chapter.
- Every lesson has the correct `chapterId`.
- Every lesson ID is unique.
- Every code block has a language.
- Quiz answer indexes are valid.

Add repository tests for:

- Course lookup.
- Chapter lookup.
- Lesson lookup.
- Independent progress per course.
- Start, complete, reopen, and reset behavior.

Add UI previews for:

- Empty catalog
- Course with no progress
- Course with partial progress
- Completed course
- Lesson with quiz
- Completed lesson
- Loading and error states
- Phone and expanded layouts

## Verification commands

Before handing off a course change, run:

```bash
./gradlew formatKotlin
./gradlew test
./gradlew check
```

If Room files changed, also verify the migration tests and generated schemas.

## Automated content validation (CI)

Code snippets in the bundled courses are validated automatically by the
`.github/workflows/course-content-validation.yml` workflow. It runs on pushes and pull requests to
`main`, but only when course content, the learning models, or the validation tooling changes (it is
path-filtered to `**/course/**`, `**/LearningModels.kt`, and
`app/src/test/java/dev/hossain/codematex/tools/**`). A failure is blocking.

The workflow runs one job per language, and the jobs run in parallel:

| Job | Toolchain | Per-snippet check |
|---|---|---|
| `validate-go` | Go (`stable`) | `go build ./...` + `go vet ./...` |
| `validate-rust` | Rust (`stable` + clippy) | `cargo build` + `cargo clippy -- -D warnings` |
| `validate-typescript` | Node + TypeScript 5 | `tsc --noEmit --strict --moduleDetection force` |
| `validate-python` | Python + `ruff` | `ruff check --select E9,F` (syntax + pyflakes) |
| `validate-kotlin` | `kotlinc` (downloaded) | `kotlinc` — compile raw, else wrapped in `fun main` |

### How it works

Each job:

1. Runs the language's exporter test (for example `GoSnippetExporterTest`), which reads the real
   course objects and writes every `runnable` snippet to
   `app/build/course-snippets/<language>/<lessonId>/` as a self-contained compilable unit. The
   export is triggered by setting the `CODEMATEX_SNIPPET_OUTPUT_DIR` environment variable, so it is
   a no-op during normal local test runs and has no filesystem side effects.
2. Compiles or lints each exported unit and fails the job if any snippet does not pass.

The exporters live in `app/src/test/java/dev/hossain/codematex/tools/`. They filter on the
`LessonBlock.Code.runnable` flag, so snippets marked `codeRunnable = false` are skipped (see
[Marking non-runnable code blocks](#marking-non-runnable-code-blocks)).

### Notes on the checks

- **TypeScript** uses `--moduleDetection force` so each snippet is treated as a module. This
  isolates top-level names and prevents false collisions with DOM globals such as `status` or
  `Report`. It runs `--strict` but not `noUnusedLocals`, so teaching snippets may declare values
  they do not use.
- **Python** is linted, not executed, so no snippet makes real network calls or needs third-party
  packages at validation time. `--select E9,F` catches syntax errors and pyflakes defects
  (undefined names, bad imports, redefinitions) without stylistic noise. It does not catch type or
  logic errors.
- **Go** and **Rust** snippets are compiled as complete programs, so they must include an entry
  point (`func main` / `fn main`) unless flagged non-runnable.
- **Kotlin** snippets come in two shapes: valid top-level declarations, and REPL-style top-level
  statements that a plain `.kt` file rejects. The exporter emits both a raw `snippet.kt` and a
  `wrapped.kt` (the snippet inside `fun main`); CI compiles the raw form first and falls back to the
  wrapped form, so a snippet passes if either compiles. `kotlinc` is not pre-installed on the runner,
  so the job downloads the official compiler. The check is stdlib-only, so snippets that need
  `kotlinx.coroutines` are flagged `codeRunnable = false`.

### Adding a new language to CI

When you bundle a new language course, extend the validation to cover it:

1. Add a `codeRunnable` parameter to the course's `lesson(...)` helper and mark any fragments
   `false`.
2. Create `<Language>SnippetExporter.kt` and `<Language>SnippetExporterTest.kt` in
   `app/src/test/java/dev/hossain/codematex/tools/`, following an existing pair (for example the Go
   or Rust exporter). The exporter writes one isolated, compilable unit per runnable snippet; the
   test verifies the export and asserts that intentional fragments are skipped, and gates the
   filesystem write behind `CODEMATEX_SNIPPET_OUTPUT_DIR`.
3. Add a `validate-<language>` job to `course-content-validation.yml` that sets up the toolchain,
   runs the exporter test with `CODEMATEX_SNIPPET_OUTPUT_DIR` set, and runs the language's
   compiler or linter over each exported unit.

Verify the checks pass on the real snippets before wiring the job into CI. Choose the compiler or
linter flags empirically: prefer the strictest configuration that produces no false failures on
existing, correct course content.

## Definition of done

A new course is ready when:

- Its content is distilled into stable bundled models.
- All IDs are stable and unique.
- The repository discovers every course.
- Progress is independent and persistent.
- The catalog, chapter, and lesson flows work end-to-end.
- The UI follows `docs/DESIGN_GUIDELINES.md`.
- Previews cover normal, empty, loading, error, and completed states.
- Unit tests cover content integrity and progress behavior.
- `formatKotlin`, `test`, and `check` pass.
- For a supported language (Go, Rust, TypeScript, Python, Kotlin), the
  [automated content validation](#automated-content-validation-ci) passes, and any non-runnable
  snippet is flagged `codeRunnable = false` with a matching exporter-test assertion.
