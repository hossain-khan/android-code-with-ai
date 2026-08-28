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
- `data/repository/KotlinCourseContent.kt` — Bundled Kotlin Foundations course.
- `data/repository/PythonCourseContent.kt` — Bundled Python Foundations course.
- `data/repository/LearningRepository.kt` — Course/progress repository contract.
- `data/repository/LearningRepositoryImpl.kt` — Bundled content lookup and progress behavior.
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
- `LessonBlock.Code`
- `LessonBlock.Quiz`

Keep lessons focused. A good lesson should teach one concept, show a small example,
and end with a useful check or practice prompt.

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
data/repository/PythonCourseContent.kt
data/repository/RustCourseContent.kt
data/repository/JavaScriptCourseContent.kt
```

Each content object should expose one stable course:

```kotlin
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

This verification is intentionally a suggested local authoring step, not a required
CI or Gradle build task. Do not execute arbitrary learner-provided code; only run
trusted course snippets during authoring.

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
