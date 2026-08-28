package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.LearningChapter
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
import dev.hossain.codematex.data.model.LessonBlock

/**
 * Bundled Go Foundations course based only on the official Tour of Go, language specification,
 * standard library documentation, and Go command documentation.
 *
 * Examples use the standard library and focus on the language's core model: simple types,
 * explicit errors, composition, and concurrency.
 */
object GoCourseContent {
    const val COURSE_ID = "go-foundations"

    val course =
        LearningCourse(
            id = COURSE_ID,
            language = "Go",
            title = "Go Foundations",
            description = "A practical path from your first Go program to typed APIs, errors, testing, and concurrency.",
            version = 1,
            chapters =
                listOf(
                    chapter(
                        id = "go-getting-started",
                        order = 1,
                        title = "Getting Started",
                        description = "Learn Go's program structure, declarations, values, and readable formatting.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "go-first-program",
                                    order = 1,
                                    title = "Your First Go Program",
                                    summary = "Write a package main program and understand the role of main.",
                                    minutes = 12,
                                    markdown =
                                        """
                                        # Your first Go program

                                        A Go executable starts in `package main` and begins at `func main()`. The `fmt` package provides common formatted I/O functions.

                                        Go source is conventionally formatted with `gofmt`. Formatting is part of the normal Go workflow and keeps code visually consistent.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        func main() {
                                            fmt.Println("Hello, Go!")
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "go-variables-and-types",
                                    order = 2,
                                    title = "Variables, Constants, and Types",
                                    summary = "Declare values with var, const, and short variable declarations.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Variables, constants, and types

                                        Go is statically typed. A declaration can provide a type explicitly, or the compiler can infer it from the initializer. Inside functions, `:=` declares and initializes new variables.

                                        Constants are values known by the compiler. Use them for named values that should not change during program execution.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        const course = "Go Foundations"
                                        var lessons int = 24

                                        func main() {
                                            completed := true
                                            fmt.Println(course, lessons, completed)
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does := do inside a function?",
                                            options =
                                                listOf(
                                                    "Declares and initializes at least one new variable",
                                                    "Creates a package",
                                                    "Converts every value to a string",
                                                    "Declares only constants",
                                                ),
                                            answer = 0,
                                            explanation = "The short declaration operator declares and initializes new local variables.",
                                        ),
                                ),
                                lesson(
                                    id = "go-strings-and-formatting",
                                    order = 3,
                                    title = "Strings and Formatting",
                                    summary = "Work with strings and format output using the standard fmt package.",
                                    minutes = 14,
                                    markdown =
                                        """
                                        # Strings and formatting

                                        A Go string is an immutable sequence of bytes, conventionally containing UTF-8 encoded text. The `fmt` package formats values with verbs such as `%s`, `%d`, and `%v`.

                                        Use `fmt.Sprintf` when you need a formatted string instead of writing directly to standard output.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        func main() {
                                            name := "Ada"
                                            message := fmt.Sprintf("Hello, %s!", name)
                                            fmt.Println(message)
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "go-control-flow",
                        order = 2,
                        title = "Control Flow and Collections",
                        description = "Use Go's compact control flow and built-in collection types.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "go-if-and-switch",
                                    order = 1,
                                    title = "if and switch",
                                    summary = "Make decisions with if statements and expressive switch statements.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # if and switch

                                        Go's `if` condition does not require parentheses. An initializer can appear before the condition, keeping a temporary value close to the branch that uses it.

                                        `switch` cases do not fall through automatically. A switch can match values or conditions and is often clearer than a long chain of else-if statements.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        func label(score int) string {
                                            switch {
                                            case score >= 90:
                                                return "excellent"
                                            case score >= 60:
                                                return "passing"
                                            default:
                                                return "retry"
                                            }
                                        }

                                        func main() {
                                            fmt.Println(label(87))
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "go-for-and-range",
                                    order = 2,
                                    title = "for and range",
                                    summary = "Repeat work with Go's single loop form and range iteration.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # for and range

                                        Go has one loop keyword: `for`. It supports a condition-only loop, a three-part loop, and iteration over collections with `range`.

                                        When ranging over a map, do not rely on iteration order. If stable output matters, collect and sort the keys explicitly.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        func main() {
                                            names := []string{"Ada", "Grace", "Ken"}
                                            for index, name := range names {
                                                fmt.Printf("%d: %s\n", index, name)
                                            }
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does range provide when iterating over a slice?",
                                            options =
                                                listOf(
                                                    "Only a random element",
                                                    "An index and the element value",
                                                    "A sorted copy",
                                                    "A new goroutine",
                                                ),
                                            answer = 1,
                                            explanation = "For a slice, range produces the index and the element value.",
                                        ),
                                ),
                                lesson(
                                    id = "go-slices-and-maps",
                                    order = 3,
                                    title = "Slices and Maps",
                                    summary = "Store ordered data with slices and key-value data with maps.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Slices and maps

                                        A slice is a flexible view over an array. It has a length and capacity and can be expanded with `append`. A map associates keys with values and reports whether a key was present through the optional second result.

                                        A nil map can be read but cannot receive assignments. Initialize a map with `make` or a map literal before writing to it.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        func main() {
                                            topics := []string{"types", "errors"}
                                            topics = append(topics, "testing")

                                            counts := map[string]int{"go": 1}
                                            counts["course"]++

                                            fmt.Println(topics, counts["course"])
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "go-functions",
                        order = 3,
                        title = "Functions and Methods",
                        description = "Return values explicitly, handle multiple results, and attach behavior to types.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "go-function-basics",
                                    order = 1,
                                    title = "Function Basics",
                                    summary = "Declare parameters, return values, and named function behavior.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Functions

                                        Go functions declare parameter and result types. A function can return more than one value, which is commonly used for a result and an error.

                                        Keep functions small and explicit. The caller should be able to see what data comes in, what comes out, and which failures are possible.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        func total(price float64, taxRate float64) float64 {
                                            return price * (1 + taxRate)
                                        }

                                        func main() {
                                            fmt.Println(total(100, 0.13))
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "go-multiple-results-and-errors",
                                    order = 2,
                                    title = "Multiple Results and Errors",
                                    summary = "Return a value with an error and handle the result explicitly.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Multiple results and errors

                                        Go does not use exceptions for ordinary failure paths. Functions commonly return `(value, error)`, and callers check the error before using the value.

                                        An error is a value. Handle it at the boundary where you can add context, recover, or decide what the program should do next.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import (
                                            "errors"
                                            "fmt"
                                        )

                                        func divide(first, second float64) (float64, error) {
                                            if second == 0 {
                                                return 0, errors.New("cannot divide by zero")
                                            }
                                            return first / second, nil
                                        }

                                        func main() {
                                            result, err := divide(10, 2)
                                            if err != nil {
                                                fmt.Println(err)
                                                return
                                            }
                                            fmt.Println(result)
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What should a caller usually do before using a value returned with error?",
                                            options =
                                                listOf(
                                                    "Ignore the error forever",
                                                    "Check whether the error is nil",
                                                    "Start a goroutine",
                                                    "Convert the error to a map",
                                                ),
                                            answer = 1,
                                            explanation =
                                                "A nil error indicates success; a non-nil error must be handled before trusting the result.",
                                        ),
                                ),
                                lesson(
                                    id = "go-methods-and-pointers",
                                    order = 3,
                                    title = "Methods and Pointers",
                                    summary = "Attach methods to named types and choose value or pointer receivers.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Methods and pointers

                                        A method is a function with a receiver. A value receiver receives a copy, while a pointer receiver can observe or change the original value.

                                        Use pointer receivers when a method mutates the receiver or copying the value would be undesirable. Go can automatically take or dereference an address in many method calls.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        type Counter struct {
                                            value int
                                        }

                                        func (counter *Counter) Increment() {
                                            counter.value++
                                        }

                                        func main() {
                                            counter := Counter{}
                                            counter.Increment()
                                            fmt.Println(counter.value)
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "go-structs-interfaces",
                        order = 4,
                        title = "Structs and Interfaces",
                        description = "Compose data and behavior with structs, interfaces, and embedding.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "go-structs",
                                    order = 1,
                                    title = "Structs",
                                    summary = "Model related data with named fields and struct literals.",
                                    minutes = 16,
                                    markdown =
                                        """
                                        # Structs

                                        A struct groups fields into a named type. Composite literals can initialize fields by name, which makes construction resilient to field reordering.

                                        Keep exported fields and methods intentional. An uppercase identifier is exported from its package; a lowercase identifier remains package-private.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        type Lesson struct {
                                            Title   string
                                            Minutes int
                                        }

                                        func main() {
                                            lesson := Lesson{Title: "Structs", Minutes: 16}
                                            fmt.Println(lesson.Title, lesson.Minutes)
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "go-interfaces",
                                    order = 2,
                                    title = "Interfaces",
                                    summary = "Use implicit interface satisfaction to depend on behavior.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Interfaces

                                        A Go type satisfies an interface implicitly when it provides the required methods. There is no `implements` keyword.

                                        Small interfaces are easier to satisfy and compose. Define an interface where it is consumed when the consumer needs only a narrow behavior.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        type Stringer interface {
                                            String() string
                                        }

                                        type Lesson struct {
                                            title string
                                        }

                                        func (lesson Lesson) String() string {
                                            return lesson.title
                                        }

                                        func printValue(value Stringer) {
                                            fmt.Println(value.String())
                                        }

                                        func main() {
                                            printValue(Lesson{title: "Interfaces"})
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "How does a Go type satisfy an interface?",
                                            options =
                                                listOf(
                                                    "By explicitly extending the interface",
                                                    "By providing the interface's required methods",
                                                    "By importing a special runtime package",
                                                    "By using a compiler annotation",
                                                ),
                                            answer = 1,
                                            explanation =
                                                "Interface satisfaction is implicit: the type must provide the required method set.",
                                        ),
                                ),
                                lesson(
                                    id = "go-composition-and-embedding",
                                    order = 3,
                                    title = "Composition and Embedding",
                                    summary = "Reuse behavior by composing structs instead of building deep inheritance trees.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Composition and embedding

                                        Go favors composition. Embedding a type in a struct promotes its fields and methods for convenient access, while the embedded value remains a member of the outer value.

                                        Embedding is not classical inheritance. Design the composed types around the behavior the package needs.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        type Logger struct{}

                                        func (Logger) Log(message string) {
                                            fmt.Println(message)
                                        }

                                        type Service struct {
                                            Logger
                                        }

                                        func main() {
                                            Service{}.Log("service ready")
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "go-packages-errors",
                        order = 5,
                        title = "Packages and Robust Errors",
                        description = "Organize packages and add context to failures with the standard errors package.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "go-packages-and-visibility",
                                    order = 1,
                                    title = "Packages and Visibility",
                                    summary = "Organize source files and control which names are exported.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Packages and visibility

                                        Every Go file belongs to a package. Files in the same package share package-level declarations, while imports connect different packages.

                                        An identifier beginning with an uppercase letter is exported. Keep implementation details unexported and expose the smallest useful API.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        type lesson struct {
                                            Title string
                                        }

                                        func newLesson(title string) lesson {
                                            return lesson{Title: title}
                                        }

                                        func main() {
                                            fmt.Println(newLesson("Packages").Title)
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "go-error-wrapping",
                                    order = 2,
                                    title = "Error Wrapping and Inspection",
                                    summary = "Add context with %w and inspect causes with errors.Is and errors.As.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Error wrapping and inspection

                                        `fmt.Errorf` with `%w` wraps an underlying error while adding context. `errors.Is` checks an error chain for a known sentinel, and `errors.As` finds a typed error in that chain.

                                        Preserve the cause when callers need to make a decision about it. Add context at package boundaries so failures explain what operation failed.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import (
                                            "errors"
                                            "fmt"
                                        )

                                        var ErrNotFound = errors.New("not found")

                                        func load() error {
                                            return fmt.Errorf("load lesson: %w", ErrNotFound)
                                        }

                                        func main() {
                                            fmt.Println(errors.Is(load(), ErrNotFound))
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What does %w preserve when used with fmt.Errorf?",
                                            options =
                                                listOf(
                                                    "The wrapped error in an inspectable error chain",
                                                    "The original stack frame in JavaScript",
                                                    "A goroutine's scheduling priority",
                                                    "The map iteration order",
                                                ),
                                            answer = 0,
                                            explanation = "The %w verb wraps the cause so errors.Is and errors.As can inspect the chain.",
                                        ),
                                ),
                                lesson(
                                    id = "go-tests-and-gofmt",
                                    order = 3,
                                    title = "Testing and Formatting",
                                    summary = "Use the standard go test command and keep source formatted with gofmt.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Testing and formatting

                                        Go's standard testing package discovers functions named `TestXxx` in files ending with `_test.go`. The `go test` command compiles the package and runs its tests.

                                        `gofmt` is the standard formatter, while `go vet` reports suspicious constructs that the compiler may accept. These tools are part of a normal Go development loop.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "testing"

                                        func add(first, second int) int {
                                            return first + second
                                        }

                                        func TestAdd(t *testing.T) {
                                            if got := add(2, 3); got != 5 {
                                                t.Fatalf("got %d, want 5", got)
                                            }
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "go-concurrency",
                        order = 6,
                        title = "Concurrency",
                        description = "Coordinate work with goroutines, channels, select, and context cancellation.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "go-goroutines",
                                    order = 1,
                                    title = "Goroutines",
                                    summary = "Start concurrent function calls and understand shared-state risks.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Goroutines

                                        A goroutine runs a function concurrently when called with the `go` statement. Starting work concurrently does not by itself make shared memory safe.

                                        A program must coordinate goroutine lifetime and shared data. Prefer communicating ownership through channels or protecting shared state with synchronization primitives.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import (
                                            "fmt"
                                            "sync"
                                        )

                                        func main() {
                                            var wait sync.WaitGroup
                                            wait.Add(1)
                                            go func() {
                                                defer wait.Done()
                                                fmt.Println("work completed")
                                            }()
                                            wait.Wait()
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "go-channels",
                                    order = 2,
                                    title = "Channels",
                                    summary = "Send values between goroutines with typed channels.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Channels

                                        Channels provide a typed way for goroutines to send and receive values. An unbuffered channel synchronizes sender and receiver; a buffered channel can hold a limited number of values.

                                        Close a channel only from the side that knows no more values will be sent. Receivers can use the two-result receive form to detect closure.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        func main() {
                                            messages := make(chan string)
                                            go func() {
                                                messages <- "ready"
                                            }()
                                            fmt.Println(<-messages)
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What is a channel used for?",
                                            options =
                                                listOf(
                                                    "Only storing immutable constants",
                                                    "Sending typed values between goroutines",
                                                    "Formatting source files",
                                                    "Replacing every struct",
                                                ),
                                            answer = 1,
                                            explanation = "Channels communicate typed values and can synchronize goroutines.",
                                        ),
                                ),
                                lesson(
                                    id = "go-select-and-context",
                                    order = 3,
                                    title = "select and Context",
                                    summary = "Wait on multiple channel operations and stop work through context cancellation.",
                                    minutes = 22,
                                    markdown =
                                        """
                                        # select and context

                                        `select` waits until one of several channel operations can proceed. A `default` case makes the select non-blocking, while a timeout can be expressed with `time.After`.

                                        `context.Context` carries cancellation and deadlines across API boundaries. Functions should stop work when `ctx.Done()` is closed and should not store contexts in long-lived structs.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import (
                                            "context"
                                            "fmt"
                                        )

                                        func work(ctx context.Context) string {
                                            select {
                                            case <-ctx.Done():
                                                return "cancelled"
                                            default:
                                                return "completed"
                                            }
                                        }

                                        func main() {
                                            fmt.Println(work(context.Background()))
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "go-standard-library",
                        order = 7,
                        title = "The Standard Library",
                        description = "Work with JSON, HTTP, and file paths using packages included with Go.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "go-json",
                                    order = 1,
                                    title = "JSON",
                                    summary = "Encode Go values and decode JSON with the encoding/json package.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # JSON

                                        The `encoding/json` package encodes exported struct fields by default. Struct tags such as ``json:"name"`` map Go field names to JSON keys.

                                        Decoding external data can fail because the input is malformed or does not match the expected shape. Treat decoding errors as part of the normal boundary contract.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import (
                                            "encoding/json"
                                            "fmt"
                                        )

                                        type User struct {
                                            Name string `json:"name"`
                                        }

                                        func main() {
                                            data, _ := json.Marshal(User{Name: "Ada"})
                                            var user User
                                            _ = json.Unmarshal(data, &user)
                                            fmt.Println(string(data), user.Name)
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "go-http-client",
                                    order = 2,
                                    title = "HTTP Clients",
                                    summary = "Make an HTTP request and inspect the response using net/http.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # HTTP clients

                                        The `net/http` package provides clients, requests, and responses. Always close a response body and check the response status before treating the body as successful data.

                                        Pass a context or timeout for real network calls so a slow server cannot hold resources forever.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import (
                                            "fmt"
                                            "net/http"
                                            "net/http/httptest"
                                        )

                                        func main() {
                                            server := httptest.NewServer(http.HandlerFunc(func(
                                                response http.ResponseWriter,
                                                request *http.Request,
                                            ) {
                                                fmt.Fprintln(response, "ok")
                                            }))
                                            defer server.Close()

                                            response, err := http.Get(server.URL)
                                            if err != nil {
                                                panic(err)
                                            }
                                            defer response.Body.Close()
                                            fmt.Println(response.StatusCode)
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "What should HTTP client code do with response.Body?",
                                            options =
                                                listOf(
                                                    "Always leave it open",
                                                    "Close it after the response is handled",
                                                    "Convert it to a goroutine",
                                                    "Use it as a map key",
                                                ),
                                            answer = 1,
                                            explanation = "Closing the response body releases resources associated with the HTTP response.",
                                        ),
                                ),
                                lesson(
                                    id = "go-file-paths",
                                    order = 3,
                                    title = "Files and Paths",
                                    summary = "Use os and path/filepath to work with files without hardcoded platform separators.",
                                    minutes = 18,
                                    markdown =
                                        """
                                        # Files and paths

                                        The `os` package provides filesystem operations, while `path/filepath` builds paths using the current platform's separator rules.

                                        Check filesystem errors explicitly. For temporary or test data, use the testing package's temporary directory support so cleanup is automatic.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import (
                                            "fmt"
                                            "os"
                                            "path/filepath"
                                        )

                                        func main() {
                                            path := filepath.Join(os.TempDir(), "go-course-note.txt")
                                            err := os.WriteFile(path, []byte("Go"), 0600)
                                            if err != nil {
                                                panic(err)
                                            }
                                            defer os.Remove(path)
                                            data, _ := os.ReadFile(path)
                                            fmt.Println(string(data))
                                        }
                                        """.trimIndent(),
                                ),
                            ),
                    ),
                    chapter(
                        id = "go-idiomatic-design",
                        order = 8,
                        title = "Idiomatic Go",
                        description = "Use generics, table tests, and simple package boundaries to finish practical programs.",
                        lessons =
                            listOf(
                                lesson(
                                    id = "go-generics",
                                    order = 1,
                                    title = "Generics",
                                    summary = "Write reusable functions with type parameters and constraints.",
                                    minutes = 22,
                                    markdown =
                                        """
                                        # Generics

                                        A type parameter lets a function or type work with several concrete types while preserving type checking. Constraints describe which operations the type parameter supports.

                                        Use generics when the algorithm is genuinely independent of the concrete type. Interfaces remain a good fit when behavior, rather than type structure, is the important abstraction.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        func first[T any](values []T) (T, bool) {
                                            var zero T
                                            if len(values) == 0 {
                                                return zero, false
                                            }
                                            return values[0], true
                                        }

                                        func main() {
                                            value, ok := first([]string{"Go", "Rust"})
                                            fmt.Println(value, ok)
                                        }
                                        """.trimIndent(),
                                ),
                                lesson(
                                    id = "go-table-tests",
                                    order = 2,
                                    title = "Table-Driven Tests",
                                    summary = "Test several cases with one focused test body.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Table-driven tests

                                        A table-driven test stores inputs, expected outputs, and case names in a slice, then runs the same assertion for each row. This keeps related cases visible and reduces duplicated test code.

                                        Use `t.Run` to give each case a name. Include boundary and failure cases, not only the happy path.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "testing"

                                        func double(value int) int {
                                            return value * 2
                                        }

                                        func TestDouble(t *testing.T) {
                                            tests := []struct {
                                                name  string
                                                input int
                                                want  int
                                            }{
                                                {name: "positive", input: 2, want: 4},
                                                {name: "zero", input: 0, want: 0},
                                            }

                                            for _, test := range tests {
                                                t.Run(test.name, func(t *testing.T) {
                                                    if got := double(test.input); got != test.want {
                                                        t.Fatalf("got %d, want %d", got, test.want)
                                                    }
                                                })
                                            }
                                        }
                                        """.trimIndent(),
                                    quiz =
                                        quiz(
                                            question = "Why use t.Run in a table-driven test?",
                                            options =
                                                listOf(
                                                    "To name and isolate each test case",
                                                    "To disable the test runner",
                                                    "To convert a slice to a map",
                                                    "To start a network server",
                                                ),
                                            answer = 0,
                                            explanation =
                                                "t.Run gives each table row a named subtest with its own reporting and execution scope.",
                                        ),
                                ),
                                lesson(
                                    id = "go-package-design",
                                    order = 3,
                                    title = "Simple Package Design",
                                    summary = "Keep APIs small, dependencies clear, and ownership easy to follow.",
                                    minutes = 20,
                                    markdown =
                                        """
                                        # Package design

                                        A package should have a focused purpose and a small exported surface. Package names should be short, lowercase, and describe the concept rather than the caller.

                                        Prefer straightforward composition over clever abstractions. A readable package boundary makes testing and future changes less expensive.
                                        """.trimIndent(),
                                    code =
                                        """
                                        package main

                                        import "fmt"

                                        type Greeter struct {
                                            Prefix string
                                        }

                                        func (greeter Greeter) Greet(name string) string {
                                            return greeter.Prefix + ", " + name
                                        }

                                        func main() {
                                            fmt.Println((Greeter{Prefix: "Hello"}).Greet("Go"))
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
                add(LessonBlock.Code("go", code))
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
