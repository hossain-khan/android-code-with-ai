package dev.hossain.codematex.data.repository.course

import dev.hossain.codematex.data.model.LearningChapter
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
import dev.hossain.codematex.data.model.LessonBlock

/**
 * Bundled Kotlin Foundations course derived from the Kotlin 1.9.20 documentation tour and
 * language-reference chapters. Content is intentionally concise and lesson-oriented rather than
 * mirroring the reference documentation page-for-page.
 */
object KotlinCourseContent {
    const val COURSE_ID = "kotlin-foundations"

    val course =
        LearningCourse(
            id = COURSE_ID,
            language = "Kotlin",
            title = "Kotlin Foundations",
            description = "A guided path from your first Kotlin program to idiomatic functions, classes, and coroutines.",
            version = 1,
            chapters =
                listOf(
                    chapter(
                        id = "kotlin-getting-started",
                        order = 1,
                        title = "Getting Started",
                        description = "Learn Kotlin's shape: entry points, variables, comments, and string templates.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "kotlin-hello-world",
                                    order = 1,
                                    title = "Hello, Kotlin",
                                    summary = "Write a program, understand main(), and print output.",
                                    minutes = 10,
                                    markdown =
                                        """
                                        # Hello, Kotlin

                                        Kotlin programs are built from declarations and expressions. A simple application starts in a `main` function, and `println` writes text to standard output.

                                        Kotlin uses braces for blocks, but many common statements do not need semicolons. The language is concise without hiding the structure of the program.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fun main() {
                                            println("Hello, Kotlin!")
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "kotlin-variables",
                                    order = 2,
                                    title = "Variables and Values",
                                    summary = "Choose between immutable val and mutable var declarations.",
                                    minutes = 12,
                                    markdown =
                                        """
                                        # Variables and values

                                        Prefer `val` for a reference that should not be reassigned. Use `var` only when the binding itself must change. Kotlin often infers the type from the initializer, but you can write it explicitly when it improves clarity.

                                        A `val` reference is stable, but the object it refers to may still be mutable. Immutability of the binding and immutability of the object are separate ideas.
                                        """.trimIndent(),
                                    code =
                                        """
                                        val language: String = "Kotlin"
                                        var lessonsCompleted = 0
                                        lessonsCompleted += 1
                                        println("${'$'}language: ${'$'}lessonsCompleted")
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "Which declaration should be the default choice?",
                                            options = listOf("var", "val", "lateinit var", "const val"),
                                            answer = 1,
                                            explanation = "Use val by default; choose var only when the binding must be reassigned.",
                                        ),
                                ),
                                lesson(
                                    id = "kotlin-strings-comments",
                                    order = 3,
                                    title = "Comments and String Templates",
                                    summary = "Document code and compose strings with embedded expressions.",
                                    minutes = 10,
                                    markdown =
                                        """
                                        # Comments and string templates

                                        Kotlin supports line comments and block comments. String templates insert a value with `${'$'}name` or evaluate an expression with `${'$'}{expression}`.

                                        Templates are useful for readable messages, but keep complex business logic out of the string itself.
                                        """.trimIndent(),
                                    code =
                                        """
                                        // A line comment
                                        /*
                                         * A block comment
                                         */
                                        val learners = 3
                                        println("There are ${'$'}learners learners")
                                        println("Next lesson: ${'$'}{learners + 1}")
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "kotlin-types",
                        order = 2,
                        title = "Types and Collections",
                        description = "Work with Kotlin's common scalar types and collection abstractions.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "kotlin-basic-types",
                                    order = 1,
                                    title = "Basic Types",
                                    summary = "Use numbers, booleans, characters, and strings safely.",
                                    minutes = 15,
                                    markdown =
                                        """
                                        # Basic types

                                        Kotlin provides numeric types such as `Int`, `Long`, `Float`, and `Double`, along with `Boolean`, `Char`, and `String`. The compiler checks assignments so values do not silently change meaning.

                                        Kotlin does not automatically widen numeric values. Convert explicitly when the destination type requires it.
                                        """.trimIndent(),
                                    code =
                                        """
                                        val count: Int = 42
                                        val distance: Long = 42L
                                        val ratio: Double = count.toDouble() / 10
                                        val ready: Boolean = ratio > 1
                                        println("${'$'}count, ${'$'}distance, ${'$'}ratio, ${'$'}ready")
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "kotlin-lists-sets-maps",
                                    order = 2,
                                    title = "Lists, Sets, and Maps",
                                    summary = "Choose the right collection and understand read-only versus mutable APIs.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Collections

                                        Use a `List` for ordered values, a `Set` for unique values, and a `Map` for key-value lookup. Factory functions such as `listOf`, `setOf`, and `mapOf` create read-only views.

                                        Mutable variants—`mutableListOf`, `mutableSetOf`, and `mutableMapOf`—expose operations that change the collection. Prefer read-only types at API boundaries when mutation is not part of the contract.
                                        """.trimIndent(),
                                    code =
                                        """
                                        val languages = listOf("Kotlin", "Python", "Rust")
                                        val uniqueTags = setOf("mobile", "mobile", "offline")
                                        val versions = mapOf("Kotlin" to "1.9", "Java" to "21")

                                        println(languages.first())
                                        println(uniqueTags.size)
                                        println(versions["Kotlin"])
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "Which collection guarantees unique elements?",
                                            options = listOf("List", "Set", "Map", "Sequence"),
                                            answer = 1,
                                            explanation = "A Set models membership and does not retain duplicate elements.",
                                        ),
                                ),
                                lesson(
                                    id = "kotlin-collection-operations",
                                    order = 3,
                                    title = "Collection Operations",
                                    summary = "Transform and query collections with map, filter, and related operators.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Collection operations

                                        Kotlin's standard library provides expressive operations for common collection work. `filter` keeps matching elements, `map` transforms each element, and `fold` combines elements into one result.

                                        These operations make the data flow visible. Name intermediate values when a chain becomes difficult to read.
                                        """.trimIndent(),
                                    code =
                                        """
                                        val scores = listOf(72, 91, 58, 84)
                                        val passing = scores
                                            .filter { it >= 60 }
                                            .map { it + 5 }

                                        val total = scores.fold(0) { sum, score -> sum + score }
                                        println(passing)
                                        println(total)
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "kotlin-control-flow",
                        order = 3,
                        title = "Control Flow",
                        description = "Express decisions, ranges, loops, and branching with Kotlin expressions.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "kotlin-if-when",
                                    order = 1,
                                    title = "if and when Expressions",
                                    summary = "Return values from conditions and replace long branches with when.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Conditions as expressions

                                        In Kotlin, `if` can produce a value, so a temporary variable is often unnecessary. `when` matches a subject against branches, or evaluates conditions without a subject.

                                        Prefer exhaustive `when` expressions for closed sets such as enums and sealed types.
                                        """.trimIndent(),
                                    code =
                                        """
                                        val score = 87
                                        val grade = if (score >= 90) "A" else "B"

                                        val message = when {
                                            score >= 90 -> "excellent"
                                            score >= 60 -> "passing"
                                            else -> "try again"
                                        }
                                        println("${'$'}grade: ${'$'}message")
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "kotlin-ranges-loops",
                                    order = 2,
                                    title = "Ranges and Loops",
                                    summary = "Iterate through ranges and collections with readable loop syntax.",
                                    minutes = 15,
                                    markdown =
                                        """
                                        # Ranges and loops

                                        The `..` operator creates an inclusive range. Use `..<` when the upper bound should be excluded, and `downTo` or `step` when iterating in another direction.

                                        Use `for` for iteration over a collection or range. Use `while` when the loop is driven by a condition whose state changes inside the loop.
                                        """.trimIndent(),
                                    code =
                                        """
                                        for (number in 1..5) {
                                            print("${'$'}number ")
                                        }

                                        for (index in 10 downTo 0 step 2) {
                                            print(index)
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "kotlin-break-continue",
                                    order = 3,
                                    title = "break, continue, and Labels",
                                    summary = "Control loop execution without making nested logic unreadable.",
                                    minutes = 12,
                                    markdown =
                                        """
                                        # Loop control

                                        `continue` skips to the next iteration. `break` exits the nearest loop. Labels can target an outer loop, but they should be used sparingly because a named helper function is often clearer.
                                        """.trimIndent(),
                                    code =
                                        """
                                        for (number in 1..10) {
                                            if (number % 2 == 0) continue
                                            if (number > 7) break
                                            println(number)
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "kotlin-functions",
                        order = 4,
                        title = "Functions and Lambdas",
                        description = "Build reusable behavior with typed functions, defaults, named arguments, and lambdas.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "kotlin-function-basics",
                                    order = 1,
                                    title = "Function Basics",
                                    summary = "Declare parameters, return types, and expression-bodied functions.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Functions

                                        Function declarations name inputs and the result type. Kotlin can infer the result of a block body in some contexts, but public functions benefit from explicit return types.

                                        A single-expression function uses `=` and is a good fit for a small calculation.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fun sum(first: Int, second: Int): Int {
                                            return first + second
                                        }

                                        fun square(value: Int): Int = value * value
                                        println(sum(2, 3))
                                        println(square(4))
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "kotlin-default-named-args",
                                    order = 2,
                                    title = "Default and Named Arguments",
                                    summary = "Make calls clearer and reduce overloads with defaults.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Defaults and named arguments

                                        Default parameter values let callers omit common choices. Named arguments make a call self-documenting and are especially useful when several parameters share a type.

                                        Defaults are part of the function's API. Choose them so the most common call is safe and unsurprising.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fun connect(
                                            host: String,
                                            port: Int = 443,
                                            secure: Boolean = true,
                                        ) = "${'$'}host:${'$'}port secure=${'$'}secure"

                                        println(connect(host = "example.com"))
                                        println(connect(host = "localhost", port = 8080, secure = false))
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "kotlin-lambdas",
                                    order = 3,
                                    title = "Lambda Expressions",
                                    summary = "Pass behavior as a value and use trailing lambdas with collection APIs.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Lambdas

                                        A lambda is an anonymous function written inside braces. Its last expression becomes the result. Lambdas can be stored, passed to functions, and returned from functions.

                                        When a function's final parameter is a lambda, Kotlin supports trailing-lambda syntax. The implicit `it` name is convenient for a single parameter; use explicit names when clarity matters.
                                        """.trimIndent(),
                                    code =
                                        """
                                        val doubled = listOf(1, 2, 3).map { value -> value * 2 }
                                        val longNames = listOf("Ada", "Kotlin", "Grace")
                                            .filter { it.length > 3 }
                                        println(doubled)
                                        println(longNames)
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "kotlin-classes",
                        order = 5,
                        title = "Classes and Objects",
                        description = "Model state and behavior with classes, properties, data classes, and sealed hierarchies.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "kotlin-classes-properties",
                                    order = 1,
                                    title = "Classes and Properties",
                                    summary = "Declare constructors, properties, and member functions.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Classes and properties

                                        Kotlin's primary constructor sits in the class header. A property declared with `val` or `var` can be initialized there and accessed like a field while still using getter and setter behavior.

                                        Keep state private when callers should use behavior rather than mutate representation directly.
                                        """.trimIndent(),
                                    code =
                                        """
                                        class User(
                                            val name: String,
                                            private var points: Int = 0,
                                        ) {
                                            fun addPoints(amount: Int) {
                                                points += amount
                                            }

                                            fun summary() = "${'$'}name has ${'$'}points points"
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "kotlin-data-classes",
                                    order = 2,
                                    title = "Data Classes",
                                    summary = "Use value-oriented classes for data transfer and comparison.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Data classes

                                        A `data class` is intended to hold data. The compiler generates useful `equals`, `hashCode`, `toString`, `copy`, and component functions from the primary-constructor properties.

                                        `copy` creates a new value with selected fields changed, which works well with immutable state updates.
                                        """.trimIndent(),
                                    code =
                                        """
                                        data class LessonProgress(
                                            val lessonId: String,
                                            val completed: Boolean,
                                        )

                                        val current = LessonProgress("functions", false)
                                        val finished = current.copy(completed = true)
                                        println(finished)
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "kotlin-sealed-enums-objects",
                                    order = 3,
                                    title = "Enums, Objects, and Sealed Types",
                                    summary = "Represent finite states and singleton behavior.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Closed hierarchies

                                        Enums represent a fixed set of named values. An `object` declaration creates a singleton. Sealed classes and interfaces describe a closed family of types, allowing the compiler to check `when` branches.

                                        These tools make state models explicit and reduce invalid combinations.
                                        """.trimIndent(),
                                    code =
                                        """
                                        sealed interface Result {
                                            data class Success(val value: String) : Result
                                            data class Failure(val reason: String) : Result
                                        }

                                        fun describe(result: Result) = when (result) {
                                            is Result.Success -> result.value
                                            is Result.Failure -> "Error: ${'$'}{result.reason}"
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "kotlin-null-safety",
                        order = 6,
                        title = "Null Safety",
                        description = "Make absent values explicit and handle them without accidental null dereferences.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "kotlin-nullable-types",
                                    order = 1,
                                    title = "Nullable Types",
                                    summary = "Distinguish String from String? and understand why the compiler helps.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Nullable types

                                        A type followed by `?` may contain `null`. Kotlin prevents direct access to nullable values until you prove they are present or choose a fallback.

                                        Treat nullability as part of the API contract. It tells callers what absence means before they run the code.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fun greeting(name: String?): String {
                                            return if (name != null) "Hello, ${'$'}name" else "Hello, stranger"
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "kotlin-safe-calls-elvis",
                                    order = 2,
                                    title = "Safe Calls and the Elvis Operator",
                                    summary = "Compose null-safe access with ?. and provide defaults with ?:.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Safe calls and Elvis

                                        The safe-call operator `?.` evaluates the right side only when the receiver is non-null. The Elvis operator `?:` supplies a fallback when the expression on its left is null.

                                        Use these operators to keep ordinary absence paths short. If absence is exceptional, make that explicit instead of hiding it behind a default.
                                        """.trimIndent(),
                                    code =
                                        """
                                        val input: String? = "kotlin"
                                        val length = input?.length ?: 0
                                        val upper = input?.uppercase() ?: "(missing)"
                                        println("${'$'}length ${'$'}upper")
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does `name?.length ?: 0` return when name is null?",
                                            options = listOf("null", "0", "Throws an exception", "The string \"0\""),
                                            answer = 1,
                                            explanation = "The safe call produces null and Elvis supplies the Int fallback 0.",
                                        ),
                                ),
                                lesson(
                                    id = "kotlin-let-null-assertion",
                                    order = 3,
                                    title = "let and the Null-Assertion Operator",
                                    summary = "Run scoped work for present values and understand the cost of !!.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Scoped null handling

                                        `let` is useful when a nullable value should trigger a small block only when present. The not-null assertion operator `!!` converts a nullable value to a non-null type, but throws if the value is actually null.

                                        Prefer safe calls, validation, or explicit errors over `!!`. Keep assertions at trusted boundaries where the invariant is genuinely established.
                                        """.trimIndent(),
                                    code =
                                        """
                                        val nickname: String? = "Ada"
                                        nickname?.let { value ->
                                            println("Nickname: ${'$'}value")
                                        }

                                        val required = nickname ?: error("Nickname is required")
                                        println(required)
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "kotlin-idioms",
                        order = 7,
                        title = "Idiomatic Kotlin",
                        description = "Compose small language features into readable, reusable code.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "kotlin-extension-functions",
                                    order = 1,
                                    title = "Extension Functions",
                                    summary = "Add discoverable operations to existing types without inheritance.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Extension functions

                                        An extension function adds a callable operation to a type without changing the type's source code. The receiver is available as `this` inside the function.

                                        Extensions are statically resolved, so use them for focused transformations and convenience APIs—not to imply that a type's runtime class has changed.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fun String.firstWord(): String =
                                            trim().substringBefore(' ')

                                        println("Kotlin makes code concise".firstWord())
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "kotlin-scope-functions",
                                    order = 2,
                                    title = "Scope Functions",
                                    summary = "Use let, run, apply, also, and with without losing readability.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Scope functions

                                        Scope functions execute a block in the context of an object. The main differences are whether the object is referenced as `this` or `it`, and whether the function returns the object or the block result.

                                        Choose the function based on intent: `apply` for configuring an object, `also` for side effects, and `let` for transforming or null-safe work. Avoid nesting several scope functions.
                                        """.trimIndent(),
                                    code =
                                        """
                                        data class Profile(var name: String = "", var verified: Boolean = false)

                                        val profile = Profile().apply {
                                            name = "Ada"
                                            verified = true
                                        }.also {
                                            println("Created ${'$'}{it.name}")
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "kotlin-delegation-operator",
                                    order = 3,
                                    title = "Delegation and Operators",
                                    summary = "Reuse behavior through delegation and understand operator conventions.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Delegation and operators

                                        Kotlin supports class delegation with `by`, allowing one object to forward an interface implementation to another object. Operator syntax maps to named functions such as `plus`, `get`, and `contains`.

                                        Use these features when the resulting code reads like the domain. Clever syntax should not obscure the underlying operation.
                                        """.trimIndent(),
                                    code =
                                        """
                                        interface Logger {
                                            fun log(message: String)
                                        }

                                        class ConsoleLogger : Logger {
                                            override fun log(message: String) = println(message)
                                        }

                                        class Service(logger: Logger) : Logger by logger
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "kotlin-coroutines",
                        order = 8,
                        title = "Coroutines",
                        description = "Understand suspending functions, structured concurrency, and concurrent work.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "kotlin-suspending-functions",
                                    order = 1,
                                    title = "Suspending Functions",
                                    summary = "Mark work that may suspend without blocking its thread.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Suspending functions

                                        A `suspend` function can pause and resume without blocking the underlying thread. Suspension is a language-level contract; it does not automatically create a new thread.

                                        A suspending function must be called from another suspend function or a coroutine scope.
                                        """.trimIndent(),
                                    code =
                                        """
                                        suspend fun loadLesson(id: String): String {
                                            return "Loaded lesson ${'$'}id"
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "kotlin-coroutine-builders",
                                    order = 2,
                                    title = "Coroutine Builders and Scope",
                                    summary = "Launch work inside a scope and keep its lifetime structured.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Coroutine builders

                                        `launch` starts work and returns a `Job`; `async` starts work that produces a result and returns a `Deferred`. Structured concurrency ties child coroutines to a scope so cancellation and failures have predictable lifetimes.

                                        In Android code, prefer lifecycle-aware scopes such as `viewModelScope` or a scope owned by the component that owns the work.
                                        """.trimIndent(),
                                    code =
                                        """
                                        suspend fun loadTwoLessons(): List<String> = coroutineScope {
                                            val first = async { loadLesson("one") }
                                            val second = async { loadLesson("two") }
                                            listOf(first.await(), second.await())
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does async return?",
                                            options = listOf("Job", "Deferred", "Thread", "Flow"),
                                            answer = 1,
                                            explanation = "async returns Deferred, whose await function retrieves the result.",
                                        ),
                                ),
                                lesson(
                                    id = "kotlin-flow-basics",
                                    order = 3,
                                    title = "Flow Basics",
                                    summary = "Model asynchronous streams of values with Flow.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Flow

                                        `Flow` represents an asynchronous stream that can emit multiple values over time. A flow is cold by default: its upstream work starts when a collector begins collecting.

                                        Operators such as `map`, `filter`, and `catch` transform or handle the stream. Collection should happen in a scope whose lifecycle matches the UI or feature.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fun lessonIds(): Flow<Int> = flow {
                                            emit(1)
                                            emit(2)
                                            emit(3)
                                        }

                                        suspend fun printLessons() {
                                            lessonIds().collect { id -> println("Lesson ${'$'}id") }
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                ),
        )

    private fun chapter(
        id: String,
        order: Int,
        title: String,
        description: String,
        lessons: List<LearningLesson>,
    ) = LearningChapter(
        id = id,
        courseId = COURSE_ID,
        order = order,
        title = title,
        description = description,
        lessons = lessons.map { it.copy(chapterId = id) },
    )

    private fun lesson(
        id: String,
        order: Int,
        title: String,
        summary: String,
        minutes: Int,
        markdown: String,
        code: String,
        quiz: LessonBlock.Quiz? = null,
    ) = LearningLesson(
        id = id,
        chapterId = "",
        order = order,
        title = title,
        summary = summary,
        estimatedMinutes = minutes,
        blocks =
            buildList {
                add(LessonBlock.Markdown(markdown))
                add(LessonBlock.Code("kotlin", code))
                quiz?.let(::add)
            },
    )

    private fun quiz(
        question: String,
        options: List<String>,
        answer: Int,
        explanation: String,
    ) = LessonBlock.Quiz(question, options, answer, explanation)
}
