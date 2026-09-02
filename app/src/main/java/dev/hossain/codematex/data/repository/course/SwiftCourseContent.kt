package dev.hossain.codematex.data.repository.course

import dev.hossain.codematex.data.model.LearningChapter
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
import dev.hossain.codematex.data.model.LessonBlock

/**
 * Bundled Swift Foundations course based on official Swift documentation ("The Swift Programming
 * Language" - A Swift Tour and Language Reference).
 *
 * Content is structured into 9 chapters and 25 lessons, covering Swift fundamentals, type safety,
 * optionals, value vs reference types, closures, protocols, generics, and error handling.
 */
object SwiftCourseContent {
    const val COURSE_ID = "swift-foundations"

    val course =
        LearningCourse(
            id = COURSE_ID,
            language = "Swift",
            title = "Swift Foundations",
            description = "A comprehensive guide from your first Swift statement to optionals, protocols, structs, and error handling.",
            version = 1,
            chapters =
                listOf(
                    chapter(
                        id = "swift-getting-started",
                        order = 1,
                        title = "Getting Started",
                        description = "Learn Swift's syntax fundamentals: variables, constants, type inference, and string interpolation.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "swift-hello-world",
                                    order = 1,
                                    title = "Hello, Swift",
                                    summary = "Write your first Swift program using the print function.",
                                    minutes = 10,
                                    markdown =
                                        """
                                        # Hello, Swift

                                        In Swift, code written at global scope is automatically used as the entry point for the program. You do not need a separate `main()` function or class wrapper just to print text.

                                        Statements in Swift do not require a semicolon (`;`) at the end of each line. Semicolons are only required if you want to write multiple separate statements on a single line.
                                        """.trimIndent(),
                                    code =
                                        """
                                        let message = "Hello, Swift!"
                                        print(message)
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "swift-variables-and-constants",
                                    order = 2,
                                    title = "Constants and Variables",
                                    summary = "Declare values using let for constants and var for mutable variables.",
                                    minutes = 12,
                                    markdown =
                                        """
                                        # Constants and Variables

                                        Constants and variables associate a name with a value of a particular type.

                                        - Use `let` to declare a constant whose value cannot change after initialization.
                                        - Use `var` to declare a variable whose value can be reassigned.

                                        Idiomatic Swift strongly encourages declaring values with `let` whenever possible to promote immutability and prevent unintended side effects.
                                        """.trimIndent(),
                                    code =
                                        """
                                        let maxLoginAttempts = 3
                                        var currentAttempts = 0

                                        currentAttempts += 1
                                        print("Attempt \(currentAttempts) of \(maxLoginAttempts)")
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "Which keyword declares an immutable constant in Swift?",
                                            options = listOf("let", "var", "val", "const"),
                                            answer = 0,
                                            explanation = "Swift uses 'let' to declare constants and 'var' for mutable variables.",
                                        ),
                                ),
                                lesson(
                                    id = "swift-types-and-interpolation",
                                    order = 3,
                                    title = "Types and String Interpolation",
                                    summary = "Work with standard scalar types and format strings using string interpolation.",
                                    minutes = 12,
                                    markdown =
                                        """
                                        # Types and String Interpolation

                                        Swift provides standard scalar types: `Int` for integers, `Double` and `Float` for floating-point numbers, `Bool` for booleans, and `String` for text.

                                        Swift is a type-safe language. The compiler uses type inference to deduce the type of an expression automatically when you provide an initial value.

                                        String interpolation builds new string values from a mix of constants, variables, and expressions by wrapping each item inside `\(...)`.
                                        """.trimIndent(),
                                    code =
                                        """
                                        let language: String = "Swift"
                                        let version: Double = 6.0
                                        let isOpenSource: Bool = true

                                        let summary = "\(language) version \(version) is open source: \(isOpenSource)"
                                        print(summary)
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "swift-control-flow",
                        order = 2,
                        title = "Control Flow",
                        description = "Direct execution using conditional branches, pattern matching, and loops.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "swift-conditionals",
                                    order = 1,
                                    title = "Conditionals: if and else",
                                    summary = "Branch code execution based on boolean conditions.",
                                    minutes = 12,
                                    markdown =
                                        """
                                        # Conditionals: if and else

                                        In Swift, the conditional expression for an `if` statement must evaluate to a `Bool`. Unlike C or Objective-C, non-boolean values such as non-zero integers are not implicitly treated as true.

                                        Parentheses around the condition are optional, but braces (`{}`) around the statement body are always required.
                                        """.trimIndent(),
                                    code =
                                        """
                                        let score = 85

                                        if score >= 90 {
                                            print("Grade: A")
                                        } else if score >= 80 {
                                            print("Grade: B")
                                        } else {
                                            print("Grade: C")
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "swift-switch-pattern-matching",
                                    order = 2,
                                    title = "Pattern Matching with switch",
                                    summary = "Match values against multiple patterns using exhaustive switch statements.",
                                    minutes = 15,
                                    markdown =
                                        """
                                        # Pattern Matching with switch

                                        A `switch` statement in Swift is exhaustive: every possible value must be accounted for, typically by providing a `default` case.

                                        Key characteristics:
                                        - Swift switch cases do not fall through to the next case by default; no explicit `break` is needed.
                                        - Cases can match ranges (`1...5`), multiple values separated by commas, or tuples.
                                        """.trimIndent(),
                                    code =
                                        """
                                        let count = 42

                                        switch count {
                                        case 0:
                                            print("None")
                                        case 1..<10:
                                            print("A few")
                                        case 10..<100:
                                            print("Dozens")
                                        default:
                                            print("Many")
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "Do Swift switch cases require a 'break' statement to prevent fallthrough?",
                                            options =
                                                listOf(
                                                    "No, Swift cases do not fall through by default",
                                                    "Yes, omitting break causes a compile error",
                                                    "Yes, break is required to prevent falling into the next case",
                                                    "Only when matching range expressions",
                                                ),
                                            answer = 0,
                                            explanation = "Unlike C, Swift switch execution does not fall through the bottom of each case.",
                                        ),
                                ),
                                lesson(
                                    id = "swift-loops-and-ranges",
                                    order = 3,
                                    title = "Loops and Ranges",
                                    summary = "Iterate over ranges and sequences using for-in and while loops.",
                                    minutes = 12,
                                    markdown =
                                        """
                                        # Loops and Ranges

                                        Use the `for-in` loop to iterate over items in a collection, such as an array, or across ranges of numbers:
                                        - `1...5`: Closed range operator (includes 1 through 5).
                                        - `1..<5`: Half-open range operator (includes 1 through 4).

                                        If you don't need the loop index value, use an underscore (`_`) in place of a variable name to ignore it.
                                        """.trimIndent(),
                                    code =
                                        """
                                        var total = 0
                                        for number in 1...5 {
                                            total += number
                                        }
                                        print("Sum 1..5: \(total)")

                                        var counter = 3
                                        while counter > 0 {
                                            print("Countdown: \(counter)")
                                            counter -= 1
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "swift-optionals",
                        order = 3,
                        title = "Optionals and Nil Safety",
                        description = "Safely represent, unwrap, and operate on values that may be absent.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "swift-optionals-intro",
                                    order = 1,
                                    title = "Optionals and if let Binding",
                                    summary = "Declare optional values and safely unwrap them using optional binding.",
                                    minutes = 15,
                                    markdown =
                                        """
                                        # Optionals and if let Binding

                                        An optional represents two possibilities: either there is a value of a specified type, or there is no value at all (`nil`). You define an optional by appending `?` to the type name.

                                        Use *optional binding* (`if let`) to check whether an optional contains a value, and if so, make that value available as a temporary constant.
                                        """.trimIndent(),
                                    code =
                                        """
                                        let possibleNumber = "123"
                                        let convertedNumber: Int? = Int(possibleNumber)

                                        if let actualNumber = convertedNumber {
                                            print("Converted integer: \(actualNumber)")
                                        } else {
                                            print("Could not convert string to integer")
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does an optional variable in Swift represent?",
                                            options =
                                                listOf(
                                                    "A variable that contains either a value or nil",
                                                    "A pointer that defaults to 0",
                                                    "A lazily evaluated function",
                                                    "A mutable variable that can change types",
                                                ),
                                            answer = 0,
                                            explanation = "Optionals model presence or absence, preventing null pointer errors.",
                                        ),
                                ),
                                lesson(
                                    id = "swift-nil-coalescing",
                                    order = 2,
                                    title = "The Nil-Coalescing Operator",
                                    summary = "Provide default fallback values for optionals using ??.",
                                    minutes = 12,
                                    markdown =
                                        """
                                        # The Nil-Coalescing Operator

                                        The nil-coalescing operator (`a ?? b`) unwraps an optional `a` if it contains a value, or returns a default value `b` if `a` is `nil`.

                                        The expression `b` must match the type that is stored inside `a`. Nil-coalescing is short-circuiting: if `a` is not nil, `b` is never evaluated.
                                        """.trimIndent(),
                                    code =
                                        """
                                        let userNickname: String? = nil
                                        let defaultName = "Guest"

                                        let displayName = userNickname ?? defaultName
                                        print("Welcome, \(displayName)!")
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "swift-guard-and-optional-chaining",
                                    order = 3,
                                    title = "Guard and Optional Chaining",
                                    summary = "Enforce preconditions with guard and safely query nested properties with ?.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Guard and Optional Chaining

                                        A `guard` statement requires that a condition must be true for code after the guard to execute. Unlike `if let`, constants unwrapped in a `guard let` remain in scope for the rest of the enclosing function.

                                        *Optional chaining* (`?.`) allows you to call properties, methods, and subscripts on an optional that might currently be `nil`. If the optional contains `nil`, the call fails gracefully and evaluates to `nil`.
                                        """.trimIndent(),
                                    code =
                                        """
                                        func greet(name: String?) {
                                            guard let unwrappedName = name else {
                                                print("Hello, Anonymous!")
                                                return
                                            }
                                            print("Hello, \(unwrappedName.uppercased())!")
                                        }

                                        greet(name: "Alice")
                                        greet(name: nil)
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "swift-collections",
                        order = 4,
                        title = "Collections",
                        description = "Store and organize groups of values using Arrays, Sets, and Dictionaries.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "swift-arrays",
                                    order = 1,
                                    title = "Arrays",
                                    summary = "Create, modify, and iterate through ordered lists of values.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Arrays

                                        An *array* stores values of the same type in an ordered list. Arrays in Swift are zero-indexed.

                                        When declared with `var`, an array is mutable and supports adding (`append`), inserting, and removing items. When declared with `let`, an array is completely immutable.
                                        """.trimIndent(),
                                    code =
                                        """
                                        var fruits = ["Apple", "Banana", "Cherry"]
                                        fruits.append("Date")

                                        print("Count: \(fruits.count)")
                                        for (index, fruit) in fruits.enumerated() {
                                            print("\(index + 1): \(fruit)")
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "swift-sets",
                                    order = 2,
                                    title = "Sets",
                                    summary = "Store distinct unordered values and perform mathematical set operations.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Sets

                                        A *set* stores distinct values of the same type in a collection with no defined ordering. You can use a set instead of an array when the order of items is not important, or when you need to ensure that an item only appears once.

                                        A type must be *hashable* (`Hashable`) in order to be stored in a set. Swift standard types such as `String`, `Int`, and `Double` conform to `Hashable` by default.
                                        """.trimIndent(),
                                    code =
                                        """
                                        var genres: Set<String> = ["Rock", "Classical", "Jazz"]
                                        genres.insert("Jazz") // Duplicate insertion is ignored

                                        let otherGenres: Set<String> = ["Jazz", "Hip Hop"]
                                        let common = genres.intersection(otherGenres)

                                        print("Common genres: \(common)")
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What protocol must an element conform to in order to be stored in a Swift Set?",
                                            options = listOf("Hashable", "Comparable", "Codable", "Identifiable"),
                                            answer = 0,
                                            explanation = "Elements in a Set must conform to Hashable to guarantee uniqueness.",
                                        ),
                                ),
                                lesson(
                                    id = "swift-dictionaries",
                                    order = 3,
                                    title = "Dictionaries",
                                    summary = "Associate unique keys with corresponding values for fast lookup.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Dictionaries

                                        A *dictionary* stores associations between keys of the same type and values of the same type in a collection with no defined ordering.

                                        Accessing a dictionary by key subscript (`airports["JFK"]`) always returns an *optional* value, because the requested key might not exist in the dictionary.
                                        """.trimIndent(),
                                    code =
                                        """
                                        var airportCodes = ["SFO": "San Francisco", "LHR": "London Heathrow"]
                                        airportCodes["HND"] = "Tokyo Haneda"

                                        if let airportName = airportCodes["SFO"] {
                                            print("SFO is: \(airportName)")
                                        }

                                        for (code, name) in airportCodes {
                                            print("\(code) -> \(name)")
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "swift-functions-and-closures",
                        order = 5,
                        title = "Functions and Closures",
                        description = "Define reusable functions and work with self-contained closure expressions.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "swift-functions",
                                    order = 1,
                                    title = "Defining and Calling Functions",
                                    summary = "Use parameter names, argument labels, and return values in functions.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Defining and Calling Functions

                                        Functions in Swift are declared using the `func` keyword. Return types are specified using `->`.

                                        Each function parameter has both an *argument label* (used when calling the function) and a *parameter name* (used inside the function implementation). You can omit an argument label by writing an underscore (`_`).
                                        """.trimIndent(),
                                    code =
                                        """
                                        func greet(person: String, from hometown: String) -> String {
                                            return "Hello \(person)! Glad you could visit from \(hometown)."
                                        }

                                        let message = greet(person: "Taylor", from: "Nashville")
                                        print(message)
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "swift-closures",
                                    order = 2,
                                    title = "Closures and Trailing Syntax",
                                    summary = "Write inline closure expressions and utilize trailing closure syntax.",
                                    minutes = 15,
                                    markdown =
                                        """
                                        # Closures and Trailing Syntax

                                        *Closures* are self-contained blocks of functionality that can be passed around and used in your code. They are similar to lambdas in Kotlin or anonymous functions in JavaScript.

                                        When a closure is the final argument to a function, you can write it as a *trailing closure* outside the function's parentheses. Swift also provides shorthand argument names like `${'$'}0` and `${'$'}1`.
                                        """.trimIndent(),
                                    code =
                                        """
                                        let numbers = [5, 2, 8, 1, 9]

                                        // Trailing closure with shorthand argument names
                                        let sortedNumbers = numbers.sorted { ${'$'}0 < ${'$'}1 }
                                        print("Sorted: \(sortedNumbers)")
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What shorthand argument name represents the first parameter of a closure in Swift?",
                                            options = listOf("${'$'}0", "${'$'}1", "it", "self"),
                                            answer = 0,
                                            explanation = "Swift provides $0, $1, etc., as shorthand argument names.",
                                        ),
                                ),
                                lesson(
                                    id = "swift-higher-order-functions",
                                    order = 3,
                                    title = "Higher-Order Functions",
                                    summary = "Transform and filter collections using map, filter, and reduce.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Higher-Order Functions

                                        Swift collections provide functional transformations that accept closures:
                                        - `map`: Transforms each element into a new value.
                                        - `filter`: Keeps elements that satisfy a boolean predicate.
                                        - `reduce`: Combines all elements into a single accumulated result.
                                        """.trimIndent(),
                                    code =
                                        """
                                        let numbers = [1, 2, 3, 4, 5, 6]

                                        let evens = numbers.filter { ${'$'}0 % 2 == 0 }
                                        let squares = evens.map { ${'$'}0 * ${'$'}0 }
                                        let sumOfSquares = squares.reduce(0, +)

                                        print("Evens: \(evens)")
                                        print("Squares: \(squares)")
                                        print("Sum: \(sumOfSquares)")
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "swift-structures-and-classes",
                        order = 6,
                        title = "Structures and Classes",
                        description = "Model custom data types using value-type structs and reference-type classes.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "swift-structures",
                                    order = 1,
                                    title = "Structures: Value Types",
                                    summary = "Define structs with properties, methods, and memberwise initializers.",
                                    minutes = 15,
                                    markdown =
                                        """
                                        # Structures: Value Types

                                        In Swift, `struct` is the preferred way to define custom data types. Structures in Swift are *value types*: when assigned to a variable or passed to a function, their values are copied.

                                        Structures receive an automatically generated *memberwise initializer* if they do not define their own custom initializers. Methods that modify properties must be marked with `mutating`.
                                        """.trimIndent(),
                                    code =
                                        """
                                        struct Point {
                                            var x: Double
                                            var y: Double

                                            mutating func moveBy(dx: Double, dy: Double) {
                                                x += dx
                                                y += dy
                                            }
                                        }

                                        var origin = Point(x: 0.0, y: 0.0)
                                        origin.moveBy(dx: 5.0, dy: 3.0)
                                        print("Moved to: (\(origin.x), \(origin.y))")
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What modifier is required on a struct method that modifies its own properties?",
                                            options = listOf("mutating", "modifying", "inout", "override"),
                                            answer = 0,
                                            explanation = "Struct methods must be marked 'mutating' if they modify any stored properties.",
                                        ),
                                ),
                                lesson(
                                    id = "swift-classes",
                                    order = 2,
                                    title = "Classes: Reference Types",
                                    summary = "Create classes supporting inheritance, custom initializers, and reference semantics.",
                                    minutes = 15,
                                    markdown =
                                        """
                                        # Classes: Reference Types

                                        Classes are *reference types*: when assigned to a variable or passed to a function, a reference to the existing instance is used rather than a copy.

                                        Classes support object-oriented features that structures do not:
                                        - Inheritance: A subclass can inherit characteristics from a superclass.
                                        - Type casting: Check and interpret the type of a class instance at runtime.
                                        - Deinitializers (`deinit`): Free custom resources before an instance is deallocated.
                                        """.trimIndent(),
                                    code =
                                        """
                                        class Vehicle {
                                            var speed = 0

                                            func describe() -> String {
                                                return "Traveling at \(speed) km/h"
                                            }
                                        }

                                        class Bicycle: Vehicle {
                                            var hasBasket = true
                                        }

                                        let bike = Bicycle()
                                        bike.speed = 15
                                        print(bike.describe())
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "swift-value-vs-reference",
                                    order = 3,
                                    title = "Value vs. Reference Semantics",
                                    summary = "Understand how copy-on-assignment differs between structs and classes.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Value vs. Reference Semantics

                                        Understanding when to use structs versus classes is foundational to writing idiomatic Swift:
                                        - **Struct (Value Type)**: Each instance keeps a unique copy of its data. Modifying one copy does not affect another.
                                        - **Class (Reference Type)**: Multiple constants or variables can refer to the same single shared instance.

                                        Apple's Swift style recommends starting with `struct` by default and only using `class` when identity, inheritance, or shared mutable state is specifically required.
                                        """.trimIndent(),
                                    code =
                                        """
                                        struct ValueItem { var name: String }
                                        class RefItem { var name: String; init(name: String) { self.name = name } }

                                        var v1 = ValueItem(name: "A")
                                        var v2 = v1
                                        v2.name = "B"
                                        print("Struct v1: \(v1.name), v2: \(v2.name)") // v1 is unchanged ("A")

                                        let r1 = RefItem(name: "A")
                                        let r2 = r1
                                        r2.name = "B"
                                        print("Class r1: \(r1.name), r2: \(r2.name)") // both reflect "B"
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "swift-properties-and-methods",
                        order = 7,
                        title = "Properties and Methods",
                        description = "Compute values dynamically, observe property changes, and define static type members.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "swift-computed-properties",
                                    order = 1,
                                    title = "Computed Properties & Observers",
                                    summary = "Implement custom getters/setters and observe changes with willSet and didSet.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Computed Properties & Observers

                                        In addition to stored properties, classes and structures can define *computed properties*, which calculate a value rather than storing it directly.

                                        *Property observers* observe and respond to changes in a property’s value:
                                        - `willSet`: Called just before the value is stored (provides `newValue`).
                                        - `didSet`: Called immediately after the new value is stored (provides `oldValue`).
                                        """.trimIndent(),
                                    code =
                                        """
                                        struct Temperature {
                                            var celsius: Double

                                            var fahrenheit: Double {
                                                return (celsius * 9 / 5) + 32
                                            }
                                        }

                                        var temp = Temperature(celsius: 25.0)
                                        print("\(temp.celsius)°C is \(temp.fahrenheit)°F")
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "swift-static-members",
                                    order = 2,
                                    title = "Type Properties and Methods",
                                    summary = "Define properties and methods on the type itself using static.",
                                    minutes = 12,
                                    markdown =
                                        """
                                        # Type Properties and Methods

                                        You can define properties and methods that belong to the type itself, rather than to instances of that type.

                                        You define type properties and methods with the `static` keyword. For classes, you can use `class` instead of `static` if you want subclasses to be able to override the implementation.
                                        """.trimIndent(),
                                    code =
                                        """
                                        struct AppConfig {
                                            static let appName = "CodeMateX"
                                            static var maxRetries = 3

                                            static func logVersion() {
                                                print("\(appName) configured with max retries: \(maxRetries)")
                                            }
                                        }

                                        AppConfig.logVersion()
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "swift-protocols-and-generics",
                        order = 8,
                        title = "Protocols, Extensions, and Generics",
                        description = "Decouple interfaces with protocols, extend existing types, and write generic code.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "swift-protocols",
                                    order = 1,
                                    title = "Protocols and Conformance",
                                    summary = "Define contracts of methods and properties that conforming types implement.",
                                    minutes = 15,
                                    markdown =
                                        """
                                        # Protocols and Conformance

                                        A *protocol* defines a blueprint of methods, properties, and other requirements that suit a particular task or piece of functionality.

                                        Any type that satisfies the requirements of a protocol is said to *conform* to that protocol. Both structs and classes can conform to multiple protocols.
                                        """.trimIndent(),
                                    code =
                                        """
                                        protocol Describable {
                                            var summary: String { get }
                                        }

                                        struct TaskItem: Describable {
                                            let title: String
                                            let isCompleted: Bool

                                            var summary: String {
                                                return "\(title) - completed: \(isCompleted)"
                                            }
                                        }

                                        let task: Describable = TaskItem(title: "Learn Swift", isCompleted: true)
                                        print(task.summary)
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "Can a Swift struct conform to more than one protocol?",
                                            options =
                                                listOf(
                                                    "Yes, a struct can conform to multiple protocols",
                                                    "No, structs cannot conform to protocols",
                                                    "No, only classes can conform to multiple protocols",
                                                    "Only if all protocols are marked @objc",
                                                ),
                                            answer = 0,
                                            explanation = "Both Swift structures and classes can conform to any number of protocols.",
                                        ),
                                ),
                                lesson(
                                    id = "swift-extensions",
                                    order = 2,
                                    title = "Extensions",
                                    summary = "Add new functionality to existing types without subclassing.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Extensions

                                        *Extensions* add new functionality to an existing class, structure, enumeration, or protocol type. This includes the ability to extend types for which you do not have access to the original source code (retroactive modeling).

                                        Extensions can add computed properties, instance methods, type methods, and declare protocol conformance.
                                        """.trimIndent(),
                                    code =
                                        """
                                        extension Int {
                                            var squared: Int {
                                                return self * self
                                            }

                                            func repetitions(task: () -> Void) {
                                                for _ in 0..<self {
                                                    task()
                                                }
                                            }
                                        }

                                        print("Square of 4: \(4.squared)")
                                        3.repetitions {
                                            print("Swift!")
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "swift-generics",
                                    order = 3,
                                    title = "Generics and Constraints",
                                    summary = "Write flexible, reusable functions and types that work with any type.",
                                    minutes = 15,
                                    markdown =
                                        """
                                        # Generics and Constraints

                                        *Generic code* enables you to write flexible, reusable functions and types that can work with any type, subject to requirements that you define.

                                        You use placeholder type names (such as `T`) inside angle brackets (`<T>`). *Type constraints* specify that a type parameter must inherit from a specific class, or conform to a particular protocol (like `Equatable` or `Comparable`).
                                        """.trimIndent(),
                                    code =
                                        """
                                        func findFirstIndex<T: Equatable>(of target: T, in items: [T]) -> Int? {
                                            for (index, item) in items.enumerated() {
                                                if item == target {
                                                    return index
                                                }
                                            }
                                            return nil
                                        }

                                        let names = ["Alice", "Bob", "Charlie"]
                                        if let index = findFirstIndex(of: "Bob", in: names) {
                                            print("Found Bob at index \(index)")
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "swift-error-handling-and-async",
                        order = 9,
                        title = "Error Handling and Concurrency",
                        description = "Throw, catch, and propagate errors, and write asynchronous code with async/await.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "swift-error-handling",
                                    order = 1,
                                    title = "Error Handling: throws and do-catch",
                                    summary = "Model errors with enum Error and handle them using do, try, and catch.",
                                    minutes = 15,
                                    markdown =
                                        """
                                        # Error Handling: throws and do-catch

                                        In Swift, errors are represented by values of types that conform to the `Error` protocol. Enumerations are well-suited for modeling a group of related error conditions.

                                        Functions that can throw an error use the `throws` keyword. Callers must precede throwing calls with `try`, wrapped inside a `do-catch` block.
                                        """.trimIndent(),
                                    code =
                                        """
                                        enum PrinterError: Error {
                                            case outOfPaper
                                            case noToner
                                        }

                                        func send(job: Int, toPrinter printerName: String) throws -> String {
                                            if printerName.isEmpty {
                                                throw PrinterError.noToner
                                            }
                                            return "Job \(job) sent to \(printerName)"
                                        }

                                        do {
                                            let result = try send(job: 101, toPrinter: "Office Laser")
                                            print(result)
                                        } catch {
                                            print("Printing failed with error: \(error)")
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "Which keyword must precede an expression calling a throwing function in Swift?",
                                            options = listOf("try", "catch", "throw", "await"),
                                            answer = 0,
                                            explanation = "Calls to throwing functions must be prefixed with 'try' to acknowledge errors.",
                                        ),
                                ),
                                lesson(
                                    id = "swift-async-await",
                                    order = 2,
                                    title = "Asynchronous Code with async/await",
                                    summary = "Write structured asynchronous code using async and await keywords.",
                                    minutes = 15,
                                    markdown =
                                        """
                                        # Asynchronous Code with async/await

                                        Swift features modern, structured concurrency built into the language.

                                        - Mark a function as asynchronous by placing `async` before its return arrow (`->`).
                                        - When calling an asynchronous function, execution can be suspended while waiting for the result. You mark these suspension points with the `await` keyword.
                                        """.trimIndent(),
                                    code =
                                        """
                                        func fetchUserGreeting(name: String) async -> String {
                                            return "Hello, \(name)! Welcome to Swift concurrency."
                                        }

                                        Task {
                                            let greeting = await fetchUserGreeting(name: "Swift Learner")
                                            print(greeting)
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
                add(LessonBlock.Code("swift", code, runnable = codeRunnable))
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
