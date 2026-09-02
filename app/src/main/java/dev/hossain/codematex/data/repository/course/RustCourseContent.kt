package dev.hossain.codematex.data.repository.course

import dev.hossain.codematex.data.model.LearningChapter
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
import dev.hossain.codematex.data.model.LessonBlock

/**
 * Bundled Rust Foundations course based on the official Rust Book, Rust By Example,
 * Cargo documentation, and the standard library documentation.
 *
 * The course introduces Rust's ownership model progressively, then builds toward
 * traits, error handling, testing, iterators, and safe concurrency.
 */
object RustCourseContent {
    const val COURSE_ID = "rust-foundations"

    val course =
        LearningCourse(
            id = COURSE_ID,
            language = "Rust",
            title = "Rust Foundations",
            description = "A practical path from your first Rust program to ownership, traits, errors, testing, and concurrency.",
            version = 1,
            chapters =
                listOf(
                    chapter(
                        id = "rust-getting-started",
                        order = 1,
                        title = "Getting Started",
                        description = "Learn Rust's program structure, variables, types, and Cargo workflow.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "rust-first-program",
                                    order = 1,
                                    title = "Your First Rust Program",
                                    summary = "Create a binary crate and understand the main function and println! macro.",
                                    minutes = 12,
                                    markdown =
                                        """
                                        # Your first Rust program

                                        A Rust binary starts in `fn main()`. The `println!` macro writes formatted text to standard output. Rust projects are normally managed with Cargo, which creates the package manifest and build layout.

                                        Cargo keeps compilation, testing, formatting, and dependency management consistent across projects.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn main() {
                                            println!("Hello, Rust!");
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "rust-variables-and-mutability",
                                    order = 2,
                                    title = "Variables and Mutability",
                                    summary = "Use immutable bindings by default and opt into mutation with mut.",
                                    minutes = 15,
                                    markdown =
                                        """
                                        # Variables and mutability

                                        Bindings are immutable by default. Add `mut` when a value must change. This makes mutation visible at the declaration site and lets the compiler catch accidental reassignment.

                                        Rust also supports shadowing: a later `let` can reuse a name and may change its type.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn main() {
                                            let language = "Rust";
                                            let mut lessons = 24;
                                            lessons += 1;
                                            let lessons = lessons.to_string();
                                            println!("{language}: {lessons} lessons");
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does adding mut to a Rust binding allow?",
                                            options =
                                                listOf(
                                                    "The binding to be reassigned",
                                                    "The binding to outlive every scope",
                                                    "The binding to become a thread",
                                                    "The binding to skip type checking",
                                                ),
                                            answer = 0,
                                            explanation = "Bindings are immutable by default; mut explicitly permits reassignment.",
                                        ),
                                ),
                                lesson(
                                    id = "rust-scalar-and-compound-types",
                                    order = 3,
                                    title = "Scalar and Compound Types",
                                    summary = "Work with numbers, booleans, tuples, and arrays.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Scalar and compound types

                                        Rust's scalar types include integers, floating-point values, booleans, and characters. Compound types group values: tuples can contain different types, while arrays have a fixed length and one element type.

                                        Type annotations are useful when a literal could have more than one valid interpretation.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn main() {
                                            let count: u32 = 3;
                                            let ready = true;
                                            let point = (10, 20);
                                            let days = ["Mon", "Tue", "Wed"];
                                            println!("{count} {ready} {} {}", point.0, days[1]);
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "rust-control-flow",
                        order = 2,
                        title = "Expressions and Control Flow",
                        description = "Use functions, expressions, conditions, loops, and pattern matching.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "rust-functions-and-expressions",
                                    order = 1,
                                    title = "Functions and Expressions",
                                    summary = "Define functions and return values from expression blocks.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Functions and expressions

                                        Rust requires function parameter and return types to be declared. Most constructs are expressions, so a block can produce a value when its final expression has no semicolon.

                                        Use an explicit `return` for early exits; use a final expression for the normal result.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn square(value: i32) -> i32 {
                                            value * value
                                        }

                                        fn main() {
                                            let result = square(6);
                                            println!("{result}");
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "rust-if-and-loop",
                                    order = 2,
                                    title = "If, Loop, and While",
                                    summary = "Choose branches and repeat work with Rust's loop forms.",
                                    minutes = 17,
                                    markdown =
                                        """
                                        # If and loop expressions

                                        `if` is an expression, so both branches must produce compatible types when the result is used. `loop` repeats until `break`, while `while` repeats while a condition is true.

                                        A `break` expression can return a value from a loop.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn main() {
                                            let score = 82;
                                            let label = if score >= 60 { "pass" } else { "retry" };
                                            let mut attempts = 0;
                                            let total = loop {
                                                attempts += 1;
                                                if attempts == 3 {
                                                    break attempts * 10;
                                                }
                                            };
                                            println!("{label} {total}");
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What is special about an if expression in Rust?",
                                            options =
                                                listOf(
                                                    "It can produce a value",
                                                    "It can only compare strings",
                                                    "It always starts a new thread",
                                                    "It bypasses ownership rules",
                                                ),
                                            answer = 0,
                                            explanation =
                                                "Because if is an expression, its branches can produce a value of a compatible type.",
                                        ),
                                ),
                                lesson(
                                    id = "rust-match-and-patterns",
                                    order = 3,
                                    title = "Match and Patterns",
                                    summary = "Use exhaustive pattern matching to handle different shapes of data.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Match and patterns

                                        `match` compares a value against patterns and must account for every possible case. Patterns can destructure tuples, enums, and structs while binding inner values.

                                        The compiler's exhaustiveness checking makes unhandled cases visible during development.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn describe(value: Option<i32>) -> &'static str {
                                            match value {
                                                Some(number) if number > 0 => "positive",
                                                Some(_) => "non-positive",
                                                None => "missing",
                                            }
                                        }

                                        fn main() {
                                            println!("{}", describe(Some(3)));
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "rust-ownership",
                        order = 3,
                        title = "Ownership and Borrowing",
                        description = "Build the mental model that makes Rust memory-safe without a garbage collector.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "rust-ownership-and-moves",
                                    order = 1,
                                    title = "Ownership and Moves",
                                    summary = "Understand owners, scopes, moves, and copies.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Ownership and moves

                                        Every value has one owner. When the owner leaves scope, Rust drops the value. Assigning a heap-backed value such as a `String` to another binding moves ownership instead of copying the allocation.

                                        Simple scalar types implement `Copy`, so assigning them copies the value. Use `clone` when an explicit deep copy is needed.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn takes_ownership(text: String) {
                                            println!("{text}");
                                        }

                                        fn main() {
                                            let message = String::from("owned text");
                                            takes_ownership(message);
                                            let count = 3;
                                            let other = count;
                                            println!("{count} {other}");
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What normally happens when a String is assigned to another binding?",
                                            options =
                                                listOf(
                                                    "Ownership moves to the new binding",
                                                    "The string is automatically copied deeply",
                                                    "The string becomes a global",
                                                    "The compiler disables cleanup",
                                                ),
                                            answer = 0,
                                            explanation =
                                                "String is not Copy, so assignment moves ownership and prevents two owners of the same allocation.",
                                        ),
                                ),
                                lesson(
                                    id = "rust-references-and-borrowing",
                                    order = 2,
                                    title = "References and Borrowing",
                                    summary = "Read and modify values through references without taking ownership.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # References and borrowing

                                        A reference lets a function use a value without owning it. Immutable references can coexist, while a mutable reference requires exclusive access for its lifetime.

                                        These rules prevent data races and invalid pointers at compile time.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn length(text: &str) -> usize {
                                            text.len()
                                        }

                                        fn add_suffix(text: &mut String) {
                                            text.push('!');
                                        }

                                        fn main() {
                                            let mut message = String::from("hello");
                                            println!("{}", length(&message));
                                            add_suffix(&mut message);
                                            println!("{message}");
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "rust-slices-and-strings",
                                    order = 3,
                                    title = "Slices and String Views",
                                    summary = "Use slices to borrow contiguous parts of strings and collections.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Slices and string views

                                        A slice is a borrowed view into a contiguous sequence. `&str` is a string slice, while `&[T]` is a slice of elements. Slices do not own the data they reference.

                                        Prefer slice parameters when a function only needs to read part or all of a collection.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn first_word(text: &str) -> &str {
                                            text.split_whitespace().next().unwrap_or("")
                                        }

                                        fn main() {
                                            let sentence = String::from("Rust is fast");
                                            let word = first_word(&sentence);
                                            println!("{word}");
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "rust-structs-enums",
                        order = 4,
                        title = "Structs, Enums, and Data Modeling",
                        description = "Model domain data with named fields, enum variants, and pattern matching.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "rust-structs-and-methods",
                                    order = 1,
                                    title = "Structs and Methods",
                                    summary = "Define named data and attach behavior with impl blocks.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Structs and methods

                                        Structs group related values with named fields. An `impl` block defines associated functions and methods. A method receives `self`, `&self`, or `&mut self` depending on how it uses the instance.
                                        """.trimIndent(),
                                    code =
                                        """
                                        struct Rectangle {
                                            width: u32,
                                            height: u32,
                                        }

                                        impl Rectangle {
                                            fn area(&self) -> u32 {
                                                self.width * self.height
                                            }
                                        }

                                        fn main() {
                                            let shape = Rectangle {
                                                width: 4,
                                                height: 5,
                                            };
                                            println!("{}", shape.area());
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "rust-enums-and-option",
                                    order = 2,
                                    title = "Enums and Option",
                                    summary = "Represent alternatives and absence explicitly with enum types.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Enums and Option

                                        An enum value is one of several variants, and each variant may carry data. The standard `Option<T>` enum represents either `Some(T)` or `None`, making absence explicit instead of relying on null.

                                        Handle options with `match`, `if let`, or combinators such as `map` and `unwrap_or`.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn parse_count(input: &str) -> Option<u32> {
                                            input.parse().ok()
                                        }

                                        fn main() {
                                            let count = parse_count("42").unwrap_or(0);
                                            println!("{count}");
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does Option<T> represent?",
                                            options =
                                                listOf(
                                                    "Some(T) or None",
                                                    "Only a successful integer",
                                                    "A thread-safe channel",
                                                    "A mutable global value",
                                                ),
                                            answer = 0,
                                            explanation = "Option makes the presence or absence of a value explicit through Some and None.",
                                        ),
                                ),
                                lesson(
                                    id = "rust-pattern-destructuring",
                                    order = 3,
                                    title = "Destructuring with Patterns",
                                    summary = "Pull values out of tuples, structs, and enum variants with patterns.",
                                    minutes = 17,
                                    markdown =
                                        """
                                        # Pattern destructuring

                                        Patterns can decompose compound values. Use `let` patterns for tuples and `match` arms for enums or structs. `ref`, `&`, and `mut` patterns control how values are bound.

                                        Keep patterns readable and use `_` when a value is intentionally ignored.
                                        """.trimIndent(),
                                    code =
                                        """
                                        struct User {
                                            name: String,
                                            active: bool,
                                        }

                                        fn main() {
                                            let user = User {
                                                name: String::from("Ada"),
                                                active: true,
                                            };
                                            let User { name, active } = user;
                                            println!("{name} {active}");
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "rust-errors-collections",
                        order = 5,
                        title = "Collections and Error Handling",
                        description = "Use standard collections and make recoverable failures part of function types.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "rust-vectors-and-strings",
                                    order = 1,
                                    title = "Vectors and Strings",
                                    summary = "Store growable sequences and build owned UTF-8 text.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Vectors and strings

                                        `Vec<T>` stores a growable sequence of one type. `String` is an owned, growable UTF-8 string; `&str` is a borrowed string slice.

                                        Indexing a vector can panic if the index is out of bounds. Use `get` when absence should be handled safely.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn main() {
                                            let mut values = vec![1, 2, 3];
                                            values.push(4);
                                            let first = values.first().copied().unwrap_or(0);
                                            let mut text = String::from("Rust");
                                            text.push_str(" course");
                                            println!("{first} {text}");
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "rust-hashmaps-and-iterating",
                                    order = 2,
                                    title = "Hash Maps and Iteration",
                                    summary = "Associate keys with values and iterate through collections.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Hash maps and iteration

                                        `HashMap<K, V>` stores values by key. Inserting an existing key replaces its value. The `entry` API is useful for insert-if-missing and frequency counting.

                                        Iterators are lazy values that can be transformed with methods such as `map`, `filter`, and `collect`.
                                        """.trimIndent(),
                                    code =
                                        """
                                        use std::collections::HashMap;

                                        fn main() {
                                            let mut counts = HashMap::new();
                                            for word in ["rust", "rust", "safe"] {
                                                *counts.entry(word).or_insert(0) += 1;
                                            }
                                            println!("{}", counts.get("rust").copied().unwrap_or(0));
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "rust-result-and-question-mark",
                                    order = 3,
                                    title = "Result and the Question-Mark Operator",
                                    summary = "Return recoverable errors with Result and propagate them with ?.",
                                    minutes = 22,
                                    markdown =
                                        """
                                        # Result and ?

                                        `Result<T, E>` represents success or failure. A function can return `Ok(value)` or `Err(error)`. The `?` operator returns an error early from the current function when the result is `Err`.

                                        Use `?` to keep the happy path readable while still requiring callers to handle failure.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn parse_count(input: &str) -> Result<u32, std::num::ParseIntError> {
                                            let count = input.parse::<u32>()?;
                                            Ok(count)
                                        }

                                        fn main() {
                                            match parse_count("24") {
                                                Ok(count) => println!("{count}"),
                                                Err(error) => println!("error: {error}"),
                                            }
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does ? do when a Result is Err?",
                                            options =
                                                listOf(
                                                    "Returns the error from the current function",
                                                    "Converts the error into zero",
                                                    "Retries the operation forever",
                                                    "Ignores the error",
                                                ),
                                            answer = 0,
                                            explanation =
                                                "The question-mark operator propagates an error to the caller of the current function.",
                                        ),
                                ),
                            ),
                    ),
                    chapter(
                        id = "rust-traits-generics",
                        order = 6,
                        title = "Traits and Generics",
                        description = "Share behavior across types while keeping compile-time guarantees.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "rust-generic-functions",
                                    order = 1,
                                    title = "Generic Functions",
                                    summary = "Write reusable functions whose types are checked at compile time.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Generic functions

                                        A generic parameter represents a type chosen by the caller. Rust monomorphizes generic code, so the compiler can produce specialized implementations without sacrificing static checking.

                                        Add trait bounds when the implementation needs a capability from the generic type.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn largest<T: PartialOrd + Copy>(values: &[T]) -> T {
                                            let mut result = values[0];
                                            for &value in values {
                                                if value > result {
                                                    result = value;
                                                }
                                            }
                                            result
                                        }

                                        fn main() {
                                            println!("{}", largest(&[3, 7, 2]));
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "rust-traits-and-impl",
                                    order = 2,
                                    title = "Traits and Implementations",
                                    summary = "Define shared behavior and implement it for concrete types.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Traits and implementations

                                        A trait defines behavior that a type can implement. Trait bounds constrain generic functions to types supporting the required behavior.

                                        Rust's standard library uses traits extensively for formatting, iteration, conversion, and comparison.
                                        """.trimIndent(),
                                    code =
                                        """
                                        trait Summary {
                                            fn summarize(&self) -> String;
                                        }

                                        struct Article {
                                            title: String,
                                        }

                                        impl Summary for Article {
                                            fn summarize(&self) -> String {
                                                format!("Article: {}", self.title)
                                            }
                                        }

                                        fn print_summary<T: Summary>(item: &T) {
                                            println!("{}", item.summarize());
                                        }

                                        fn main() {
                                            print_summary(&Article {
                                                title: String::from("Ownership"),
                                            });
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does a Rust trait primarily define?",
                                            options =
                                                listOf(
                                                    "Shared behavior a type can implement",
                                                    "A database table",
                                                    "A runtime garbage collector",
                                                    "A mutable global variable",
                                                ),
                                            answer = 0,
                                            explanation = "Traits describe methods and behavior that implementing types agree to provide.",
                                        ),
                                ),
                                lesson(
                                    id = "rust-lifetimes",
                                    order = 3,
                                    title = "Lifetimes in Function Signatures",
                                    summary = "Make relationships between borrowed values explicit when the compiler needs help.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Lifetimes

                                        A lifetime describes how long a reference is valid. Most lifetimes are inferred, but a function returning one of several borrowed inputs may need an annotation to express the relationship.

                                        Lifetime annotations do not change how long values live; they describe constraints the compiler verifies.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn longest<'a>(first: &'a str, second: &'a str) -> &'a str {
                                            if first.len() >= second.len() {
                                                first
                                            } else {
                                                second
                                            }
                                        }

                                        fn main() {
                                            println!("{}", longest("rust", "ownership"));
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "rust-modules-testing",
                        order = 7,
                        title = "Modules, Iterators, and Testing",
                        description = "Organize crates, transform data, and protect behavior with tests.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "rust-modules-and-visibility",
                                    order = 1,
                                    title = "Modules and Visibility",
                                    summary = "Organize code into modules and control public API boundaries.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Modules and visibility

                                        Modules organize related code and create namespaces. Items are private by default; use `pub` to expose a module or item to its parent and external callers.

                                        Keep implementation details private and expose the smallest useful API.
                                        """.trimIndent(),
                                    code =
                                        """
                                        mod math {
                                            pub fn double(value: i32) -> i32 {
                                                value * 2
                                            }
                                        }

                                        fn main() {
                                            println!("{}", math::double(4));
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "rust-iterators-and-closures",
                                    order = 2,
                                    title = "Iterators and Closures",
                                    summary = "Transform sequences with lazy iterators and closures.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Iterators and closures

                                        Iterators produce values one at a time. Adapters such as `map` and `filter` are lazy; consumers such as `collect`, `sum`, and `for_each` execute the pipeline.

                                        Closures can capture values from their surrounding environment and often make iterator transformations concise.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn main() {
                                            let squares: Vec<i32> = (1..=5)
                                                .map(|number| number * number)
                                                .filter(|value| value % 2 == 1)
                                                .collect();
                                            println!("{squares:?}");
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "When does a typical iterator adapter such as map run?",
                                            options =
                                                listOf(
                                                    "When a consumer drives the iterator",
                                                    "Immediately when the adapter is written",
                                                    "Only after a network request",
                                                    "Never; iterators cannot execute",
                                                ),
                                            answer = 0,
                                            explanation =
                                                "Iterator adapters are lazy and run when a consumer such as collect or for_each requests items.",
                                        ),
                                ),
                                lesson(
                                    id = "rust-unit-tests",
                                    order = 3,
                                    title = "Unit Tests",
                                    summary = "Write focused tests with the built-in test framework and cargo test.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Unit tests

                                        Rust's test framework discovers functions marked with `#[test]`. Use `assert_eq!` for expected values and run the suite with `cargo test`.

                                        Keep tests close to the code they exercise when they validate private implementation behavior.
                                        """.trimIndent(),
                                    code =
                                        """
                                        fn add(left: i32, right: i32) -> i32 {
                                            left + right
                                        }

                                        #[cfg(test)]
                                        mod tests {
                                            use super::*;

                                            #[test]
                                            fn adds_two_numbers() {
                                                assert_eq!(add(2, 3), 5);
                                            }
                                        }

                                        fn main() {
                                            println!("{}", add(2, 3));
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "rust-concurrency-cargo",
                        order = 8,
                        title = "Concurrency and Cargo",
                        description = "Use safe threads and channels, smart pointers, and reproducible Cargo commands.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "rust-threads-and-move",
                                    order = 1,
                                    title = "Threads and move Closures",
                                    summary = "Spawn threads and transfer ownership safely into concurrent work.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Threads and move closures

                                        `std::thread::spawn` starts a native thread and returns a join handle. A `move` closure takes ownership of captured values, allowing the new thread to use them safely.

                                        Call `join` when the caller must wait for the spawned work to finish.
                                        """.trimIndent(),
                                    code =
                                        """
                                        use std::thread;

                                        fn main() {
                                            let message = String::from("hello from a thread");
                                            let handle = thread::spawn(move || message);
                                            println!("{}", handle.join().unwrap());
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "rust-channels-and-smart-pointers",
                                    order = 2,
                                    title = "Channels and Shared State",
                                    summary = "Send values between threads and introduce Arc and Mutex for shared state.",
                                    minutes = 22,
                                    markdown =
                                        """
                                        # Channels and shared state

                                        A channel transfers ownership of values between threads. For shared mutable state, `Arc<T>` provides shared ownership across threads and `Mutex<T>` provides synchronized access.

                                        Prefer message passing when it makes ownership and coordination clearer; use a mutex when shared state is the natural model.
                                        """.trimIndent(),
                                    code =
                                        """
                                        use std::sync::mpsc;
                                        use std::thread;

                                        fn main() {
                                            let (sender, receiver) = mpsc::channel();
                                            thread::spawn(move || {
                                                sender.send(String::from("message")).unwrap();
                                            });
                                            println!("{}", receiver.recv().unwrap());
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does a channel send between threads?",
                                            options =
                                                listOf(
                                                    "Owned values",
                                                    "Only references with no lifetime",
                                                    "Compiler warnings",
                                                    "Database connections",
                                                ),
                                            answer = 0,
                                            explanation =
                                                "Sending through a channel transfers ownership of values from the sender to the receiver.",
                                        ),
                                ),
                                lesson(
                                    id = "rust-cargo-and-package-design",
                                    order = 3,
                                    title = "Cargo and Package Design",
                                    summary = "Use Cargo commands and design a small, testable crate API.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Cargo and package design

                                        Cargo manages a package's manifest, targets, dependencies, builds, tests, and documentation. Common commands include `cargo new`, `cargo build`, `cargo run`, `cargo test`, `cargo fmt`, and `cargo clippy`.

                                        A maintainable crate has a small public surface, clear module boundaries, and tests for behavior that matters to callers.
                                        """.trimIndent(),
                                    code =
                                        """
                                        pub fn is_even(value: i32) -> bool {
                                            value % 2 == 0
                                        }

                                        fn main() {
                                            println!("{}", is_even(4));
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
                add(LessonBlock.Code("rust", code, runnable = codeRunnable))
                quiz?.let(::add)
            },
    )

    private fun quiz(
        question: String,
        options: List<String>,
        answer: Int,
        explanation: String,
    ) = LessonBlock.Quiz(
        question = question,
        options = options,
        answerIndex = answer,
        explanation = explanation,
    )
}
