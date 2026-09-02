package dev.hossain.codematex.data.repository.course

import dev.hossain.codematex.data.model.LearningChapter
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
import dev.hossain.codematex.data.model.LessonBlock

/**
 * Bundled TypeScript Foundations course based only on the official TypeScript Handbook,
 * TypeScript Language Reference material, and TSConfig Reference.
 *
 * Examples focus on the type system and its relationship to JavaScript. Framework-specific
 * material is intentionally excluded so the lessons remain useful across runtimes.
 */
object TypeScriptCourseContent {
    const val COURSE_ID = "typescript-foundations"

    val course =
        LearningCourse(
            id = COURSE_ID,
            language = "TypeScript",
            title = "TypeScript Foundations",
            description = "A guided path from typed JavaScript basics to practical, reusable TypeScript designs.",
            version = 1,
            chapters =
                listOf(
                    chapter(
                        id = "typescript-basics",
                        order = 1,
                        title = "TypeScript Basics",
                        description = "Understand annotations, inference, primitive values, and collections.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "typescript-first-program",
                                    order = 1,
                                    title = "Your First TypeScript Program",
                                    summary = "Add types to a small program and understand the compile-to-JavaScript model.",
                                    minutes = 12,
                                    markdown =
                                        """
                                        # Your first TypeScript program

                                        TypeScript adds a static type system to JavaScript. The TypeScript compiler checks the source and emits JavaScript that a runtime can execute.

                                        Type annotations help describe intent while type inference keeps simple code concise. Types are removed from the emitted JavaScript; they do not become runtime validation automatically.
                                        """.trimIndent(),
                                    code =
                                        """
                                        function greet(name: string): string {
                                            return "Hello, " + name + "!"
                                        }

                                        const message = greet("Ada")
                                        console.log(message)
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "typescript-primitives-and-inference",
                                    order = 2,
                                    title = "Primitive Types and Inference",
                                    summary = "Use string, number, boolean, and inferred variable types.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Primitive types and inference

                                        TypeScript has types such as `string`, `number`, and `boolean`. In many cases the compiler infers a type from the initializer, so an explicit annotation is unnecessary.

                                        An inferred type still protects the value. If a variable is inferred as a number, assigning a string later is rejected during type checking.
                                        """.trimIndent(),
                                    code =
                                        """
                                        const language = "TypeScript"
                                        const lessons = 24
                                        let completed = false

                                        completed = true
                                        console.log(language, lessons, completed)
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does TypeScript usually infer for const lessons = 24?",
                                            options = listOf("string", "number", "boolean", "unknown"),
                                            answer = 1,
                                            explanation = "With const, lessons infers the literal type 24, which is itself a number.",
                                        ),
                                ),
                                lesson(
                                    id = "typescript-arrays-and-tuples",
                                    order = 3,
                                    title = "Arrays and Tuples",
                                    summary = "Describe collections with arrays and fixed-position data with tuples.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Arrays and tuples

                                        `string[]` and `Array<string>` describe arrays whose elements are strings. A tuple describes a fixed number of elements where each position can have a different type.

                                        Tuples are useful for small structured pairs or triples. Use an object when named fields make the data easier to understand.
                                        """.trimIndent(),
                                    code =
                                        """
                                        const topics: string[] = ["types", "functions"]
                                        const lesson: [string, number] = ["types", 1]

                                        topics.push("generics")
                                        console.log(lesson[0], lesson[1], topics.length)
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "typescript-object-types",
                        order = 2,
                        title = "Object Types",
                        description = "Model records, optional properties, readonly values, and literal shapes.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "typescript-object-shapes",
                                    order = 1,
                                    title = "Object Shapes",
                                    summary = "Describe the properties an object must provide.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Object shapes

                                        An object type describes the properties that are allowed or required. Excess property checks can catch misspelled property names when creating object literals.

                                        Type aliases give a reusable name to a type. Keep a type's shape close to the data contract it represents.
                                        """.trimIndent(),
                                    code =
                                        """
                                        type Lesson = {
                                            title: string
                                            minutes: number
                                        }

                                        const lesson: Lesson = {
                                            title: "Object types",
                                            minutes: 16,
                                        }

                                        console.log(lesson.title)
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "typescript-optional-readonly",
                                    order = 2,
                                    title = "Optional and readonly Properties",
                                    summary = "Represent absent values and prevent accidental reassignment.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Optional and readonly properties

                                        A property marked with `?` may be absent. A `readonly` property cannot be assigned through that property after initialization, although readonly does not deeply freeze an object at runtime.

                                        Optional values should be handled explicitly. Narrow the value before using an operation that requires it to exist.
                                        """.trimIndent(),
                                    code =
                                        """
                                        type Profile = {
                                            readonly id: string
                                            name: string
                                            bio?: string
                                        }

                                        const profile: Profile = { id: "p1", name: "Ada" }
                                        const description = profile.bio ?? "No biography"
                                        console.log(profile.id, description)
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does the ? in bio?: string mean?",
                                            options =
                                                listOf(
                                                    "bio is always null",
                                                    "bio may be absent",
                                                    "bio is readonly",
                                                    "bio must be a number",
                                                ),
                                            answer = 1,
                                            explanation = "An optional property may be omitted from an object.",
                                        ),
                                ),
                                lesson(
                                    id = "typescript-literal-types",
                                    order = 3,
                                    title = "Literal and Indexed Types",
                                    summary = "Restrict values to exact literals and describe keyed collections.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Literal and indexed types

                                        Literal types describe exact values such as `"draft"` or `200`. Combining literals with unions creates small, precise sets of allowed states.

                                        An index signature describes properties accessed by a dynamic key. Use it when the keys are not known ahead of time and all values share a consistent type.
                                        """.trimIndent(),
                                    code =
                                        """
                                        type Status = "draft" | "published"
                                        type Scores = { [userId: string]: number }

                                        const status: Status = "published"
                                        const scores: Scores = { ada: 10, grace: 9 }
                                        console.log(status, scores["ada"])
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "typescript-unions-narrowing",
                        order = 3,
                        title = "Unions and Narrowing",
                        description = "Represent alternatives and safely discover which type a value has.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "typescript-union-types",
                                    order = 1,
                                    title = "Union Types",
                                    summary = "Allow a value to be one of several types while preserving safety.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Union types

                                        A union type describes a value that may be one of several alternatives. Code using a union can access only members common to every alternative until it narrows the value.

                                        Unions model alternatives without making every property optional. Choose a union when the value has one of several distinct shapes.
                                        """.trimIndent(),
                                    code =
                                        """
                                        function printId(id: string | number): void {
                                            console.log(String(id))
                                        }

                                        printId("lesson-1")
                                        printId(42)
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "typescript-narrowing",
                                    order = 2,
                                    title = "Narrowing with Runtime Checks",
                                    summary = "Use typeof, equality, and truthiness checks to refine a union.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Narrowing

                                        TypeScript follows common JavaScript checks such as `typeof`, equality comparisons, and truthiness tests to narrow a value's type inside a branch.

                                        Narrowing is a compile-time analysis. The runtime check still executes as ordinary JavaScript, so it should reflect a real property of the value.
                                        """.trimIndent(),
                                    code =
                                        """
                                        function formatValue(value: string | number | null): string {
                                            if (value === null) return "missing"
                                            if (typeof value === "number") return value.toFixed(2)
                                            return value.toUpperCase()
                                        }

                                        console.log(formatValue(3.14159))
                                        console.log(formatValue("ready"))
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does typeof value === \"number\" help TypeScript do?",
                                            options =
                                                listOf(
                                                    "Convert every value to a number",
                                                    "Narrow value to number inside the branch",
                                                    "Freeze the value at runtime",
                                                    "Remove value from the program",
                                                ),
                                            answer = 1,
                                            explanation =
                                                "The check narrows the union so number members can be used " +
                                                    "safely in that branch.",
                                        ),
                                ),
                                lesson(
                                    id = "typescript-discriminated-unions",
                                    order = 3,
                                    title = "Discriminated Unions",
                                    summary = "Model state machines with a shared literal discriminant.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Discriminated unions

                                        A discriminated union gives each object alternative a shared property with a different literal value. Checking that property narrows the entire object to the matching member.

                                        This pattern makes impossible states harder to represent and works especially well for results, events, and UI state.
                                        """.trimIndent(),
                                    code =
                                        """
                                        type Result =
                                            | { kind: "ok"; value: string }
                                            | { kind: "error"; message: string }

                                        function describe(result: Result): string {
                                            if (result.kind === "ok") return result.value
                                            return "Error: " + result.message
                                        }

                                        console.log(describe({ kind: "ok", value: "Loaded" }))
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "typescript-functions",
                        order = 4,
                        title = "Functions",
                        description = "Type parameters, callbacks, overloads, and function contracts.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "typescript-function-types",
                                    order = 1,
                                    title = "Function Types",
                                    summary = "Describe parameters, return values, callbacks, and optional arguments.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Function types

                                        Function types describe the parameter list and return type of a callable value. Callback types make APIs explicit about the function they accept.

                                        A function returning `void` may still have side effects. Type the inputs and outputs that callers rely on, and let inference handle straightforward implementation details.
                                        """.trimIndent(),
                                    code =
                                        """
                                        type Formatter = (value: number) => string

                                        const formatScore: Formatter = (value) => value.toFixed(1)

                                        function mapScore(values: number[], format: Formatter): string[] {
                                            return values.map(format)
                                        }

                                        console.log(mapScore([1, 2, 3], formatScore))
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "typescript-generics-basics",
                                    order = 2,
                                    title = "Generic Functions",
                                    summary = "Preserve relationships between inputs and outputs with type parameters.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Generic functions

                                        A generic function introduces a type parameter that callers fill in. The parameter preserves a relationship between values instead of replacing useful information with `any`.

                                        Use generics when the operation works across types while preserving the specific type. Do not add a type parameter when it is not used to express a relationship.
                                        """.trimIndent(),
                                    code =
                                        """
                                        function first<T>(values: T[]): T | undefined {
                                            return values[0]
                                        }

                                        const firstTitle = first(["Kotlin", "Python", "TypeScript"])
                                        const firstCount = first([1, 2, 3])

                                        console.log(firstTitle, firstCount)
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "Why use a type parameter in first<T>(values: T[]): T | undefined?",
                                            options =
                                                listOf(
                                                    "To preserve the element type in the result",
                                                    "To disable type checking",
                                                    "To force every array to contain strings",
                                                    "To execute the function at compile time",
                                                ),
                                            answer = 0,
                                            explanation = "T connects the array element type to the returned element type.",
                                        ),
                                ),
                                lesson(
                                    id = "typescript-overloads",
                                    order = 3,
                                    title = "Function Overloads",
                                    summary = "Expose multiple call signatures while implementing one function body.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Function overloads

                                        Overloads describe several supported call signatures for one function. The implementation signature is written separately and must be broad enough to handle every overload.

                                        Prefer a union parameter when the return type does not change with the input. Use overloads when different inputs produce meaningfully different result types.
                                        """.trimIndent(),
                                    code =
                                        """
                                        function combine(first: string, second: string): string
                                        function combine(first: number, second: number): number
                                        function combine(first: string | number, second: string | number) {
                                            if (typeof first === "string" && typeof second === "string") {
                                                return first + second
                                            }
                                            return Number(first) + Number(second)
                                        }

                                        console.log(combine("Type", "Script"))
                                        console.log(combine(2, 3))
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "typescript-generics",
                        order = 5,
                        title = "Generic Design",
                        description = "Constrain type parameters and derive types from values and keys.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "typescript-generic-constraints",
                                    order = 1,
                                    title = "Generic Constraints",
                                    summary = "Require generic values to provide the operations an implementation needs.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Generic constraints

                                        A constraint limits a type parameter to types that satisfy a shape. This lets the implementation use known properties while keeping the function reusable.

                                        `extends` in a type parameter expresses a requirement; it does not mean a class must be inherited at runtime.
                                        """.trimIndent(),
                                    code =
                                        """
                                        function longer<T extends { length: number }>(first: T, second: T): T {
                                            return first.length >= second.length ? first : second
                                        }

                                        console.log(longer("TypeScript", "TS"))
                                        console.log(longer([1, 2, 3], [4]))
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "typescript-keyof-and-indexed-access",
                                    order = 2,
                                    title = "keyof and Indexed Access",
                                    summary = "Relate valid property keys to the values stored under those keys.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # keyof and indexed access

                                        `keyof` produces a union of the known property keys of a type. An indexed access such as `T[K]` looks up the value type for a key type.

                                        Together they can type helpers that accept only valid keys and return the corresponding property value.
                                        """.trimIndent(),
                                    code =
                                        """
                                        type User = { id: string; points: number }

                                        function getProperty<T, K extends keyof T>(value: T, key: K): T[K] {
                                            return value[key]
                                        }

                                        const user: User = { id: "ada", points: 10 }
                                        console.log(getProperty(user, "points"))
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does K extends keyof T ensure in getProperty?",
                                            options =
                                                listOf(
                                                    "K is a valid key of T",
                                                    "K is always a string value",
                                                    "T has no properties",
                                                    "The function mutates T",
                                                ),
                                            answer = 0,
                                            explanation = "The constraint restricts K to keys that exist on T.",
                                        ),
                                ),
                                lesson(
                                    id = "typescript-utility-types",
                                    order = 3,
                                    title = "Utility Types",
                                    summary = "Transform existing types with Partial, Pick, Record, and ReturnType.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Utility types

                                        TypeScript provides built-in generic utility types for common transformations. `Partial<T>` makes properties optional, `Pick<T, K>` selects properties, and `Record<K, T>` describes a mapping from keys to values.

                                        Utility types keep derived contracts connected to their source type. Prefer them over duplicating a type that should change when the original changes.
                                        """.trimIndent(),
                                    code =
                                        """
                                        type User = { id: string; name: string; points: number }
                                        type UserUpdate = Partial<Pick<User, "name" | "points">>
                                        type UserById = Record<string, User>

                                        const update: UserUpdate = { points: 11 }
                                        const users: UserById = {}
                                        console.log(update, users)
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "typescript-classes-modules",
                        order = 6,
                        title = "Classes and Modules",
                        description = "Organize behavior with classes and separate files with modules.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "typescript-classes",
                                    order = 1,
                                    title = "Classes and Access Modifiers",
                                    summary = "Type fields, constructors, methods, and public or private access.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Classes

                                        TypeScript classes combine JavaScript class syntax with type annotations and access modifiers. `public` is the default, while `private` and `protected` restrict access at compile time.

                                        TypeScript's access modifiers are erased from emitted JavaScript. Runtime privacy is a separate concern from compile-time access checking.
                                        """.trimIndent(),
                                    code =
                                        """
                                        class Counter {
                                            private value = 0

                                            increment(): number {
                                                this.value += 1
                                                return this.value
                                            }
                                        }

                                        const counter = new Counter()
                                        console.log(counter.increment())
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "typescript-interfaces-and-implements",
                                    order = 2,
                                    title = "Interfaces and implements",
                                    summary = "Describe contracts and have classes satisfy them.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Interfaces and implements

                                        An interface describes a contract that values can satisfy. A class can use `implements` to ask the compiler to check that it provides the required members.

                                        Interfaces are erased from JavaScript output. They describe assignability; they do not perform runtime checks or exist as constructors.
                                        """.trimIndent(),
                                    code =
                                        """
                                        interface Printable {
                                            print(): string
                                        }

                                        class Report implements Printable {
                                            print(): string {
                                                return "Report ready"
                                            }
                                        }

                                        console.log(new Report().print())
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "typescript-modules",
                                    order = 3,
                                    title = "Modules",
                                    summary = "Export and import values and types between files.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Modules

                                        A file with a top-level `import` or `export` is a module. Named exports make dependencies explicit, while default exports provide one primary exported value.

                                        Keep modules focused and import types with `import type` when the import is needed only during type checking. Module output behavior is also affected by the selected compiler configuration.
                                        """.trimIndent(),
                                    code =
                                        """
                                        // format.ts
                                        export function title(value: string): string {
                                            return value.trim().toUpperCase()
                                        }

                                        // app.ts
                                        import { title } from "./format"
                                        console.log(title("TypeScript"))
                                        """.trimIndent(),
                                    // Multi-file illustration (format.ts + app.ts); cannot type-check as one standalone file.
                                    codeRunnable = false,
                                ),
                            ),
                    ),
                    chapter(
                        id = "typescript-advanced-types",
                        order = 7,
                        title = "Advanced Types",
                        description = "Derive expressive types with conditional, mapped, and template literal types.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "typescript-conditional-types",
                                    order = 1,
                                    title = "Conditional Types",
                                    summary = "Select a type based on whether another type satisfies a condition.",
                                    minutes = 22,
                                    markdown =
                                        """
                                        # Conditional types

                                        A conditional type has the form `SomeType extends OtherType ? TrueType : FalseType`. It chooses a type based on assignability.

                                        Conditional types are most useful inside reusable type utilities. Keep them readable and introduce a named helper when the expression becomes difficult to inspect.
                                        """.trimIndent(),
                                    code =
                                        """
                                        type Message<T> = T extends string ? "text" : "other"

                                        type TextMessage = Message<string>
                                        type NumberMessage = Message<number>

                                        const textKind: TextMessage = "text"
                                        const numberKind: NumberMessage = "other"
                                        console.log(textKind, numberKind)
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "typescript-mapped-types",
                                    order = 2,
                                    title = "Mapped Types",
                                    summary = "Build a new type by transforming each property in an existing type.",
                                    minutes = 22,
                                    markdown =
                                        """
                                        # Mapped types

                                        A mapped type iterates over a union of property keys and creates a related type. It can add modifiers, change value types, or select a subset of properties.

                                        Mapped types are a type-level transformation. They do not loop over object values at runtime.
                                        """.trimIndent(),
                                    code =
                                        """
                                        type User = { id: string; name: string; points: number }
                                        type Nullable<T> = { [K in keyof T]: T[K] | null }

                                        const user: Nullable<User> = {
                                            id: "ada",
                                            name: null,
                                            points: 10,
                                        }

                                        console.log(user)
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does a mapped type transform?",
                                            options =
                                                listOf(
                                                    "Runtime array elements",
                                                    "Properties in a type",
                                                    "Only JavaScript comments",
                                                    "The Node.js event loop",
                                                ),
                                            answer = 1,
                                            explanation =
                                                "Mapped types transform the properties represented by a key " +
                                                    "union at the type level.",
                                        ),
                                ),
                                lesson(
                                    id = "typescript-template-literal-types",
                                    order = 3,
                                    title = "Template Literal Types",
                                    summary = "Construct string literal unions from other string literal types.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Template literal types

                                        Template literal types build new string literal types from existing literal unions. They are useful for naming conventions such as event names or property-derived API keys.

                                        This is a type-level feature. It does not create or interpolate a string at runtime.
                                        """.trimIndent(),
                                    code =
                                        """
                                        type Action = "save" | "load"
                                        type EventName = `on${'$'}{Capitalize<Action>}`

                                        const saveEvent: EventName = "onSave"
                                        const loadEvent: EventName = "onLoad"
                                        console.log(saveEvent, loadEvent)
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "typescript-config-runtime",
                        order = 8,
                        title = "Configuration and Runtime Boundaries",
                        description = "Use strict checking and validate values where static types meet runtime data.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "typescript-strict-mode",
                                    order = 1,
                                    title = "strict Type Checking",
                                    summary = "Understand why strict mode is the recommended starting point.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # strict mode

                                        The `strict` compiler option enables a broad family of type-checking rules. These rules expose uncertain values earlier, including possible null and undefined values.

                                        Strict checking does not make JavaScript runtime data trustworthy. It improves the guarantees for code that has already crossed into the typed program.
                                        """.trimIndent(),
                                    code =
                                        """
                                        function lengthOf(value: string | undefined): number {
                                            return value?.length ?? 0
                                        }

                                        console.log(lengthOf(undefined))
                                        console.log(lengthOf("typed"))
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "typescript-unknown-and-validation",
                                    order = 2,
                                    title = "unknown and Runtime Validation",
                                    summary = "Treat external values as unknown until a runtime check proves their shape.",
                                    minutes = 22,
                                    markdown =
                                        """
                                        # unknown and runtime validation

                                        `unknown` can hold any value, but it cannot be used as a more specific type until code narrows or validates it. This makes it safer than `any` for data from JSON, storage, or network boundaries.

                                        TypeScript types are erased at runtime. A type assertion changes the compiler's view but does not validate the value, so use a runtime check when correctness depends on external data.
                                        """.trimIndent(),
                                    code =
                                        """
                                        function isUser(value: unknown): value is { name: string } {
                                            return (
                                                typeof value === "object" &&
                                                value !== null &&
                                                "name" in value &&
                                                typeof value.name === "string"
                                            )
                                        }

                                        const input: unknown = { name: "Ada" }
                                        if (isUser(input)) console.log(input.name)
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "Why is unknown safer than any for external data?",
                                            options =
                                                listOf(
                                                    "unknown skips all compiler checks",
                                                    "unknown requires narrowing before specific use",
                                                    "unknown converts values to strings",
                                                    "unknown guarantees runtime validation",
                                                ),
                                            answer = 1,
                                            explanation =
                                                "unknown prevents specific operations until the value has been " +
                                                    "narrowed or validated.",
                                        ),
                                ),
                                lesson(
                                    id = "typescript-tsconfig",
                                    order = 3,
                                    title = "tsconfig and Project Boundaries",
                                    summary = "Configure the compiler with a project file and keep source and output boundaries clear.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # tsconfig

                                        A `tsconfig.json` file describes a TypeScript project. It can select source files and configure options such as `target`, `module`, `strict`, `rootDir`, and `outDir`.

                                        Compiler options describe how source is checked and emitted; they do not install a runtime or a package manager. Choose settings that match the JavaScript environment where the output will run.
                                        """.trimIndent(),
                                    code =
                                        """
                                        // tsconfig.json
                                        {
                                            "compilerOptions": {
                                                "target": "ES2022",
                                                "module": "NodeNext",
                                                "strict": true,
                                                "rootDir": "src",
                                                "outDir": "dist"
                                            },
                                            "include": ["src"]
                                        }
                                        """.trimIndent(),
                                    // JSON tsconfig.json example, not TypeScript source; cannot be type-checked as a .ts file.
                                    codeRunnable = false,
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
        codeRunnable: Boolean = true,
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
                add(LessonBlock.Code("typescript", code, runnable = codeRunnable))
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
