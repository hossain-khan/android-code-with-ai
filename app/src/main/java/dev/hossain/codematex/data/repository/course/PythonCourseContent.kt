package dev.hossain.codematex.data.repository.course

import dev.hossain.codematex.data.model.LearningChapter
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
import dev.hossain.codematex.data.model.LessonBlock

/**
 * Bundled Python Foundations course based on the official Python Tutorial, Language Reference,
 * Built-in Functions, and Standard Library documentation.
 *
 * The material targets Python 3.10+ and focuses on transferable language fundamentals rather than
 * a particular framework or operating-system workflow.
 */
object PythonCourseContent {
    const val COURSE_ID = "python-foundations"

    val course =
        LearningCourse(
            id = COURSE_ID,
            language = "Python",
            title = "Python Foundations",
            description = "A practical path from your first Python 3.10+ script to reusable, testable programs.",
            version = 1,
            chapters =
                listOf(
                    chapter(
                        id = "python-getting-started",
                        order = 1,
                        title = "Getting Started",
                        description = "Learn Python's syntax, values, names, and readable program structure.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "python-hello-world",
                                    order = 1,
                                    title = "Hello, Python",
                                    summary = "Run a Python program and use print to produce output.",
                                    minutes = 10,
                                    markdown =
                                        """
                                        # Hello, Python

                                        Python programs are sequences of statements evaluated from top to bottom. The `print` function writes a readable representation of values to standard output.

                                        Python uses indentation to group statements instead of braces. Consistent four-space indentation makes the structure visible and is part of the language's syntax.
                                        """.trimIndent(),
                                    code =
                                        """
                                        def main():
                                            print("Hello, Python!")

                                        if __name__ == "__main__":
                                            main()
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "python-values-and-names",
                                    order = 2,
                                    title = "Values, Names, and Types",
                                    summary = "Bind values to names and inspect their types.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Values, names, and types

                                        A Python variable is a name bound to an object. Assignment binds or rebinds a name; it does not copy the object automatically.

                                        Python is dynamically typed, so a name can be rebound to an object of another type. The object itself still has a type, which you can inspect with `type` or test with `isinstance`.
                                        """.trimIndent(),
                                    code =
                                        """
                                        language = "Python"
                                        lessons = 24

                                        print(language)
                                        print(type(lessons).__name__)
                                        print(isinstance(lessons, int))
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does assignment usually do in Python?",
                                            options =
                                                listOf(
                                                    "Changes the object's type",
                                                    "Binds a name to an object",
                                                    "Copies every nested object",
                                                    "Declares a fixed variable type",
                                                ),
                                            answer = 1,
                                            explanation =
                                                "Assignment binds or rebinds a name to an object; " +
                                                    "it does not declare a fixed type.",
                                        ),
                                ),
                                lesson(
                                    id = "python-strings-and-comments",
                                    order = 3,
                                    title = "Strings and Comments",
                                    summary = "Work with text, formatted strings, and explanatory comments.",
                                    minutes = 12,
                                    markdown =
                                        """
                                        # Strings and comments

                                        Python strings are immutable sequences of Unicode characters. Use indexing and slicing to read portions of a string, and use f-strings when a formatted value should be easy to read.

                                        Comments begin with `#` and continue to the end of the line. A comment should explain intent or a non-obvious decision rather than repeat the code.
                                        """.trimIndent(),
                                    code =
                                        """
                                        name = "Ada"
                                        language = "Python"

                                        greeting = f"{name} is learning {language}"
                                        print(greeting)
                                        print(language[:2])
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "python-control-flow",
                        order = 2,
                        title = "Control Flow",
                        description = "Make decisions and repeat work with Python's readable control structures.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "python-conditionals",
                                    order = 1,
                                    title = "Conditions and Boolean Logic",
                                    summary = "Choose a path with if, elif, else, and Boolean expressions.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Conditions

                                        `if`, `elif`, and `else` select statements based on truth values. Python treats values such as `None`, `0`, and empty collections as false in a Boolean context.

                                        Use `and`, `or`, and `not` to combine conditions. Parentheses can make a complex condition easier to review.
                                        """.trimIndent(),
                                    code =
                                        """
                                        score = 87

                                        if score >= 90:
                                            grade = "A"
                                        elif score >= 60:
                                            grade = "Pass"
                                        else:
                                            grade = "Retry"

                                        print(grade)
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "python-for-and-while",
                                    order = 2,
                                    title = "for and while Loops",
                                    summary = "Iterate over sequences and repeat work while a condition is true.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Loops

                                        A `for` loop iterates over items from an iterable. A `while` loop repeats while its condition is true and should make progress toward termination.

                                        Use `range` for numeric sequences, `enumerate` when you need an index and value, and direct iteration when an index is not needed.
                                        """.trimIndent(),
                                    code =
                                        """
                                        names = ["Ada", "Grace", "Guido"]

                                        for index, name in enumerate(names, start=1):
                                            print(f"{index}. {name}")
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does enumerate(names, start=1) provide?",
                                            options =
                                                listOf(
                                                    "Only the first item",
                                                    "A sequence of indexes and items",
                                                    "A sorted copy of names",
                                                    "A dictionary keyed by names",
                                                ),
                                            answer = 1,
                                            explanation = "enumerate produces pairs containing an index and the corresponding item.",
                                        ),
                                ),
                                lesson(
                                    id = "python-match-and-loop-control",
                                    order = 3,
                                    title = "match, break, and continue",
                                    summary = "Use structural pattern matching and control loop execution.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Matching and loop control

                                        `match` compares a value against patterns and can make several structured cases easier to read. Use it when the cases describe the shape or meaning of a value.

                                        `break` exits the nearest loop, while `continue` moves to its next iteration. Keep control flow simple enough that the exit conditions remain obvious.
                                        """.trimIndent(),
                                    code =
                                        """
                                        command = "start"

                                        match command:
                                            case "start":
                                                message = "Starting"
                                            case "stop":
                                                message = "Stopping"
                                            case _:
                                                message = "Unknown command"

                                        print(message)
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "python-collections",
                        order = 3,
                        title = "Collections",
                        description = "Choose and transform Python's core collection types.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "python-lists-and-tuples",
                                    order = 1,
                                    title = "Lists and Tuples",
                                    summary = "Store ordered values and choose between mutable lists and immutable tuples.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Lists and tuples

                                        Lists are mutable ordered collections. Tuples are immutable ordered collections and are useful for fixed-size records or values that should not be changed.

                                        Both support indexing, slicing, and iteration. Prefer the simplest collection that expresses the allowed operations.
                                        """.trimIndent(),
                                    code =
                                        """
                                        lessons = ["syntax", "testing"]
                                        lessons.append("projects")

                                        coordinates = (43.7, -79.4)
                                        print(lessons[-1])
                                        print(coordinates[0])
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "python-dictionaries-and-sets",
                                    order = 2,
                                    title = "Dictionaries and Sets",
                                    summary = "Model key-value lookup and unique membership.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Dictionaries and sets

                                        A dictionary maps hashable keys to values. A set stores unique hashable values and is useful for membership checks and set operations.

                                        Use `dict.get` when a missing key should produce a default. Use a set when uniqueness or membership matters more than ordering by position.
                                        """.trimIndent(),
                                    code =
                                        """
                                        versions = {"Python": 3, "Kotlin": 2}
                                        tags = {"offline", "course", "offline"}

                                        print(versions.get("Python"))
                                        print(sorted(tags))
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What happens to the duplicate value in a set?",
                                            options =
                                                listOf(
                                                    "It is stored twice",
                                                    "It is removed because set members are unique",
                                                    "It becomes a dictionary key",
                                                    "It raises a syntax error",
                                                ),
                                            answer = 1,
                                            explanation = "A set contains each distinct value at most once.",
                                        ),
                                ),
                                lesson(
                                    id = "python-comprehensions",
                                    order = 3,
                                    title = "Comprehensions",
                                    summary = "Create collections from iterable data with concise, readable expressions.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Comprehensions

                                        List, set, and dictionary comprehensions build collections from an iterable. They can include a condition that filters the input.

                                        A comprehension is useful when the transformation fits on one readable expression. Use an ordinary loop when the logic needs several steps or side effects.
                                        """.trimIndent(),
                                    code =
                                        """
                                        scores = [72, 91, 58, 84]
                                        passing = [score for score in scores if score >= 60]
                                        labels = {score: "pass" for score in passing}

                                        print(passing)
                                        print(labels)
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "python-functions",
                        order = 4,
                        title = "Functions and Modules",
                        description = "Package behavior into reusable functions and importable modules.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "python-function-basics",
                                    order = 1,
                                    title = "Function Basics",
                                    summary = "Define functions with parameters, return values, and local scope.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Functions

                                        A function packages reusable behavior. Parameters receive input values, and `return` sends a value back to the caller.

                                        Names created inside a function are local by default. Keep functions focused so their inputs, outputs, and side effects are easy to understand.
                                        """.trimIndent(),
                                    code =
                                        """
                                        def total_price(price, tax_rate=0.13):
                                            return price * (1 + tax_rate)

                                        print(total_price(100))
                                        print(total_price(price=80, tax_rate=0))
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "python-arguments-and-scope",
                                    order = 2,
                                    title = "Arguments and Scope",
                                    summary = "Use positional, keyword, variadic, and keyword-only arguments safely.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Arguments and scope

                                        Python supports positional and keyword arguments, default values, and variadic `*args` and `**kwargs`. Keyword-only parameters can make important options explicit.

                                        Avoid mutable default arguments such as `items=[]`. The default object is created once when the function is defined, not once per call.
                                        """.trimIndent(),
                                    code =
                                        """
                                        def summarize(title, *items, uppercase=False):
                                            text = ", ".join(items)
                                            result = f"{title}: {text}"
                                            return result.upper() if uppercase else result

                                        print(summarize("Topics", "loops", "functions"))
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "When is a default argument expression evaluated?",
                                            options =
                                                listOf(
                                                    "Every time the function is called",
                                                    "When the function is defined",
                                                    "Only after an exception",
                                                    "When the module is deleted",
                                                ),
                                            answer = 1,
                                            explanation =
                                                "Default argument expressions are evaluated once when " +
                                                    "the function definition executes.",
                                        ),
                                ),
                                lesson(
                                    id = "python-modules-and-packages",
                                    order = 3,
                                    title = "Modules and Packages",
                                    summary = "Split code into files and import only the names a module should expose.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Modules and packages

                                        A module is a Python file that defines names. Importing a module executes its top-level code once and makes its names available through the module namespace.

                                        Packages organize related modules. Keep executable behavior behind a `main` function and use the `if __name__ == "__main__"` guard so importing a module does not unexpectedly run the program.
                                        """.trimIndent(),
                                    code =
                                        """
                                        # greetings.py
                                        def greet(name):
                                            return f"Hello, {name}!"

                                        # app.py
                                        from greetings import greet

                                        if __name__ == "__main__":
                                            print(greet("Ada"))
                                        """.trimIndent(),
                                    // Multi-file illustration (greetings.py + app.py); cannot lint as one standalone file.
                                    codeRunnable = false,
                                ),
                            ),
                    ),
                    chapter(
                        id = "python-errors-and-files",
                        order = 5,
                        title = "Errors and Files",
                        description = "Handle expected failures and work with files using standard-library tools.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "python-exceptions",
                                    order = 1,
                                    title = "Exceptions",
                                    summary = "Handle expected failures with try, except, else, and finally.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Exceptions

                                        Exceptions represent unusual conditions that interrupt normal control flow. Catch specific exception types that you can handle, and let unexpected errors remain visible.

                                        `else` runs when the protected code succeeds. `finally` runs whether an exception occurred or not, which makes it useful for cleanup.
                                        """.trimIndent(),
                                    code =
                                        """
                                        text = "42"

                                        try:
                                            number = int(text)
                                        except ValueError:
                                            number = 0
                                        else:
                                            print("Parsed successfully")
                                        finally:
                                            print(f"Value: {number}")
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "python-file-paths",
                                    order = 2,
                                    title = "Files and Paths",
                                    summary = "Read and write files with pathlib and context managers.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Files and paths

                                        `pathlib.Path` provides an object-oriented interface for filesystem paths. Use `Path.open` or the built-in `open` with a `with` statement so the file is closed automatically.

                                        Specify an encoding when reading text if the file's encoding is known. Keep filesystem work near the boundary of an application so the rest of the code can operate on values.
                                        """.trimIndent(),
                                    code =
                                        """
                                        from pathlib import Path

                                        path = Path("notes.txt")
                                        path.write_text("Learn Python", encoding="utf-8")

                                        with path.open(encoding="utf-8") as file:
                                            print(file.read())
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "python-json-and-data",
                                    order = 3,
                                    title = "JSON and Structured Data",
                                    summary = "Serialize common Python values and safely load JSON data.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # JSON

                                        JSON represents structured data with objects, arrays, strings, numbers, booleans, and null. Python's `json` module converts between JSON text and Python dictionaries, lists, and scalar values.

                                        Treat data loaded from a file or network as untrusted input. Validate required keys and value types before using them.
                                        """.trimIndent(),
                                    code =
                                        """
                                        import json

                                        settings = {"theme": "dark", "notifications": True}
                                        encoded = json.dumps(settings)
                                        decoded = json.loads(encoded)

                                        print(decoded["theme"])
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "Which Python type commonly represents a JSON object after json.loads?",
                                            options = listOf("dict", "tuple", "set", "bytes"),
                                            answer = 0,
                                            explanation = "A JSON object is commonly decoded into a Python dict.",
                                        ),
                                ),
                            ),
                    ),
                    chapter(
                        id = "python-objects",
                        order = 6,
                        title = "Objects and Data Models",
                        description = "Model domain data with classes, dataclasses, inheritance, and protocols.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "python-classes",
                                    order = 1,
                                    title = "Classes and Objects",
                                    summary = "Define types with attributes, methods, and initializers.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Classes and objects

                                        A class defines a blueprint for objects. `__init__` initializes an instance, and the first method parameter conventionally named `self` refers to that instance.

                                        Keep object invariants in one place. Validate initializer inputs when an invalid object would make later code harder to reason about.
                                        """.trimIndent(),
                                    code =
                                        """
                                        class User:
                                            def __init__(self, name):
                                                self.name = name

                                            def greeting(self):
                                                return f"Hello, {self.name}!"

                                        user = User("Ada")
                                        print(user.greeting())
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "python-dataclasses",
                                    order = 2,
                                    title = "Dataclasses",
                                    summary = "Create value-oriented classes with less boilerplate.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Dataclasses

                                        `dataclasses.dataclass` can generate methods such as `__init__`, `__repr__`, and `__eq__` from annotated fields. Dataclasses are useful for records whose identity is represented by their values.

                                        Use `field(default_factory=...)` for a mutable default so each instance receives its own collection.
                                        """.trimIndent(),
                                    code =
                                        """
                                        from dataclasses import dataclass, field

                                        @dataclass
                                        class Course:
                                            title: str
                                            tags: list[str] = field(default_factory=list)

                                        course = Course("Python Foundations")
                                        course.tags.append("offline")
                                        print(course)
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "Why use field(default_factory=list) in a dataclass?",
                                            options =
                                                listOf(
                                                    "To share one list across every instance",
                                                    "To create a fresh list for each instance",
                                                    "To make the list immutable",
                                                    "To convert the list to JSON",
                                                ),
                                            answer = 1,
                                            explanation = "default_factory creates a separate default list for each dataclass instance.",
                                        ),
                                ),
                                lesson(
                                    id = "python-iterators-generators",
                                    order = 3,
                                    title = "Iterators and Generators",
                                    summary = "Process values lazily with iter and yield.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Iterators and generators

                                        An iterator produces values one at a time. A generator function uses `yield` to suspend execution and resume later, which can avoid building a large collection in memory.

                                        Lazy processing is useful for streams and large inputs. It also means the work happens during iteration, not when the generator is created.
                                        """.trimIndent(),
                                    code =
                                        """
                                        def squares(values):
                                            for value in values:
                                                yield value * value

                                        for square in squares(range(4)):
                                            print(square)
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "python-quality",
                        order = 7,
                        title = "Readable and Testable Python",
                        description = "Use type hints, context managers, and tests to make code easier to maintain.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "python-type-hints",
                                    order = 1,
                                    title = "Type Hints",
                                    summary = "Document expected shapes with annotations without losing Python's flexibility.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Type hints

                                        Type annotations document the values a function expects and returns. Python does not enforce them at runtime by default, but editors and static type checkers can use them to find mistakes earlier.

                                        Keep annotations aligned with the actual contract. Use built-in generic forms such as `list[str]` in modern Python.
                                        """.trimIndent(),
                                    code =
                                        """
                                        def average(values: list[float]) -> float:
                                            return sum(values) / len(values)

                                        print(average([2.0, 4.0, 6.0]))
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "python-context-managers",
                                    order = 2,
                                    title = "Context Managers",
                                    summary = "Guarantee setup and cleanup around resources.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Context managers

                                        A context manager defines setup and cleanup behavior for a block of code. The `with` statement calls that protocol and guarantees cleanup when the block exits, including when an exception is raised.

                                        Files, locks, and temporary resources commonly use context managers. Prefer `with` over manually remembering cleanup calls.
                                        """.trimIndent(),
                                    code =
                                        """
                                        from contextlib import contextmanager

                                        @contextmanager
                                        def announcing(label):
                                            print(f"Start: {label}")
                                            try:
                                                yield
                                            finally:
                                                print(f"End: {label}")

                                        with announcing("lesson"):
                                            print("Working")
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "python-testing-basics",
                                    order = 3,
                                    title = "Testing with unittest",
                                    summary = "Turn expected behavior into repeatable checks with the standard library test runner.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Testing

                                        A test describes an expected behavior and fails when the implementation no longer satisfies it. The standard-library `unittest` module provides test cases, assertions, and a test runner.

                                        Keep tests focused on observable behavior. A small test suite gives refactoring a safety net and documents important edge cases.
                                        """.trimIndent(),
                                    code =
                                        """
                                        import unittest

                                        def add(first, second):
                                            return first + second

                                        class AddTests(unittest.TestCase):
                                            def test_adds_numbers(self):
                                                self.assertEqual(add(2, 3), 5)

                                        if __name__ == "__main__":
                                            unittest.main()
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What should a unit test primarily verify?",
                                            options =
                                                listOf(
                                                    "The exact internal implementation",
                                                    "Observable behavior for a focused case",
                                                    "The developer's preferred formatting",
                                                    "That every function has the same number of lines",
                                                ),
                                            answer = 1,
                                            explanation = "Good tests focus on observable behavior and meaningful edge cases.",
                                        ),
                                ),
                            ),
                    ),
                    chapter(
                        id = "python-next-steps",
                        order = 8,
                        title = "Practical Python",
                        description = "Connect the language to command-line tools, HTTP, and asynchronous work.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "python-command-line",
                                    order = 1,
                                    title = "Command-Line Programs",
                                    summary = "Build small command-line tools with argparse.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Command-line programs

                                        The `argparse` module turns command-line arguments into a documented interface. Parse input at the boundary, then pass ordinary values to functions that contain the application logic.

                                        A clear command-line interface makes a small script reusable by people and automation.
                                        """.trimIndent(),
                                    code =
                                        """
                                        import argparse

                                        parser = argparse.ArgumentParser()
                                        parser.add_argument("name")
                                        args = parser.parse_args()
                                        print(f"Hello, {args.name}!")
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "python-http-basics",
                                    order = 2,
                                    title = "HTTP and APIs",
                                    summary = "Understand requests, responses, JSON payloads, and failure handling.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # HTTP and APIs

                                        An HTTP request has a method, URL, headers, and sometimes a body. A response includes a status code, headers, and content. Client code should handle timeouts, unsuccessful status codes, and malformed payloads.

                                        The standard library includes `urllib` for HTTP access. Third-party clients can provide a friendlier interface, but the protocol concepts remain the same.
                                        """.trimIndent(),
                                    code =
                                        """
                                        from urllib.request import urlopen

                                        with urlopen("https://example.com", timeout=5) as response:
                                            body = response.read()
                                            print(response.status)
                                            print(len(body))
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "python-asyncio",
                                    order = 3,
                                    title = "Asyncio Overview",
                                    summary = "Recognize when cooperative asynchronous I/O can improve throughput.",
                                    minutes = 22,
                                    markdown =
                                        """
                                        # Asyncio

                                        `asyncio` supports concurrent code using `async` functions, `await`, tasks, and an event loop. It is useful when work spends time waiting for I/O and the libraries involved support asynchronous operation.

                                        Async code does not make CPU-heavy work automatically parallel. Choose threads, processes, or specialized tools when the bottleneck is computation.
                                        """.trimIndent(),
                                    code =
                                        """
                                        import asyncio

                                        async def fetch_label(label):
                                            await asyncio.sleep(0.01)
                                            return label

                                        async def main():
                                            results = await asyncio.gather(
                                                fetch_label("one"),
                                                fetch_label("two"),
                                            )
                                            print(results)

                                        asyncio.run(main())
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "When is asyncio usually most useful?",
                                            options =
                                                listOf(
                                                    "For waiting on many I/O operations",
                                                    "For making every CPU loop parallel",
                                                    "For replacing all data structures",
                                                    "For avoiding function definitions",
                                                ),
                                            answer = 0,
                                            explanation = "Asyncio is designed for cooperative concurrency around asynchronous I/O.",
                                        ),
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
                add(LessonBlock.Code("python", code, runnable = codeRunnable))
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
