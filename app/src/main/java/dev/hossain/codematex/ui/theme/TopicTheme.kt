package dev.hossain.codematex.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import dev.hossain.codematex.data.model.CodingTopic

@Immutable
data class TopicVisualInfo(
    val topic: CodingTopic,
    val accentColor: Color,
    val secondaryAccentColor: Color,
    val tagline: String,
    val iconGlyph: String,
    val starterPrompts: List<String>,
)

val CodingTopic.visualInfo: TopicVisualInfo
    get() =
        when (this) {
            CodingTopic.KOTLIN -> {
                TopicVisualInfo(
                    topic = this,
                    accentColor = Color(0xFF7F52FF),
                    secondaryAccentColor = Color(0xFFC7BFFF),
                    tagline = "Modern, expressive & concise for Android and multiplatform",
                    iconGlyph = "Kt",
                    starterPrompts =
                        listOf(
                            "Explain Kotlin Coroutines vs Java Threads with a code snippet",
                            "What are inline value classes and when should I use them?",
                            "How does smart casting work with sealed interfaces?",
                            "Show me an idiomatic StateFlow pattern in Jetpack Compose",
                        ),
                )
            }

            CodingTopic.PYTHON -> {
                TopicVisualInfo(
                    topic = this,
                    accentColor = Color(0xFF3776AB),
                    secondaryAccentColor = Color(0xFFFFD43B),
                    tagline = "Versatile, readable & powerful for scripts, backend and AI",
                    iconGlyph = "Py",
                    starterPrompts =
                        listOf(
                            "Explain Python decorators with a practical logging example",
                            "How do async/await and asyncio event loops work in Python?",
                            "What is the difference between dataclasses and Pydantic models?",
                            "How does Python manage memory and the GIL?",
                        ),
                )
            }

            CodingTopic.JAVASCRIPT -> {
                TopicVisualInfo(
                    topic = this,
                    accentColor = Color(0xFFF7DF1E),
                    secondaryAccentColor = Color(0xFFF0DB4F),
                    tagline = "Dynamic, event-driven language powering modern web applications",
                    iconGlyph = "JS",
                    starterPrompts =
                        listOf(
                            "Explain JavaScript Event Loop and Microtask queue with examples",
                            "What is the difference between Prototypes and Classes in JS?",
                            "How do Closures work and how to avoid memory leaks?",
                            "Explain Promise.all vs Promise.allSettled with code",
                        ),
                )
            }

            CodingTopic.TYPESCRIPT -> {
                TopicVisualInfo(
                    topic = this,
                    accentColor = Color(0xFF3178C6),
                    secondaryAccentColor = Color(0xFF9CC7F5),
                    tagline = "Typed JavaScript for reliable web and server applications",
                    iconGlyph = "TS",
                    starterPrompts =
                        listOf(
                            "Explain TypeScript narrowing with a discriminated union",
                            "When should I use interfaces versus type aliases?",
                            "Show a generic function constrained by keyof",
                            "How should strict mode shape a TypeScript project?",
                        ),
                )
            }

            CodingTopic.RUST -> {
                TopicVisualInfo(
                    topic = this,
                    accentColor = Color(0xFFCE412B),
                    secondaryAccentColor = Color(0xFFDEA584),
                    tagline = "Blazing fast, memory-efficient & concurrency-safe systems programming",
                    iconGlyph = "Rs",
                    starterPrompts =
                        listOf(
                            "Explain Rust Ownership, Borrowing, and Lifetimes with diagrams",
                            "What is the difference between Arc<Mutex<T>> and RwLock<T>?",
                            "How do Rust Traits compare to Interfaces in other languages?",
                            "How to handle errors idiomatically using Result and Option?",
                        ),
                )
            }

            CodingTopic.GO -> {
                TopicVisualInfo(
                    topic = this,
                    accentColor = Color(0xFF00ADD8),
                    secondaryAccentColor = Color(0xFF5DC9E2),
                    tagline = "Simple, reliable & efficient language built for cloud and microservices",
                    iconGlyph = "Go",
                    starterPrompts =
                        listOf(
                            "Explain Goroutines, Channels, and Select statements with code",
                            "How does Go interface satisfaction work without 'implements'?",
                            "Explain context.Context best practices for cancellation and timeouts",
                            "How does Go's garbage collector minimize latency?",
                        ),
                )
            }

            CodingTopic.SWIFT -> {
                TopicVisualInfo(
                    topic = this,
                    accentColor = Color(0xFFF05138),
                    secondaryAccentColor = Color(0xFFFF8B7B),
                    tagline = "Fast, safe & intuitive programming for Apple platforms",
                    iconGlyph = "Sw",
                    starterPrompts =
                        listOf(
                            "Explain Swift Actors and async/await concurrency model",
                            "What is the difference between Structs (Value) and Classes (Reference)?",
                            "How does SwiftUI @StateObject vs @ObservedObject work?",
                            "Explain Memory Management in Swift with ARC and weak/unowned",
                        ),
                )
            }

            CodingTopic.ALGORITHMS -> {
                TopicVisualInfo(
                    topic = this,
                    accentColor = Color(0xFF9C27B0),
                    secondaryAccentColor = Color(0xFFE1BEE7),
                    tagline = "Data structures, dynamic programming & problem-solving patterns",
                    iconGlyph = "Alg",
                    starterPrompts =
                        listOf(
                            "Explain the Two-Pointer technique with LeetCode examples",
                            "How to approach Dynamic Programming (Top-down vs Bottom-up)?",
                            "Explain Dijkstra's shortest path algorithm step-by-step",
                            "What are the trade-offs between Balanced Trees and Hash Tables?",
                        ),
                )
            }

            CodingTopic.SYSTEM_DESIGN -> {
                TopicVisualInfo(
                    topic = this,
                    accentColor = Color(0xFF009688),
                    secondaryAccentColor = Color(0xFF80CBC4),
                    tagline = "Scalable architectures, distributed caching & high-availability systems",
                    iconGlyph = "Sys",
                    starterPrompts =
                        listOf(
                            "Design a high-scale URL Shortener (like Bitly) step-by-step",
                            "Explain CAP theorem and PACELC trade-offs with real databases",
                            "How to design a distributed Rate Limiter (Token Bucket vs Leaky Bucket)?",
                            "Explain Database Sharding, Replication, and Consistent Hashing",
                        ),
                )
            }

            CodingTopic.ANDROID -> {
                TopicVisualInfo(
                    topic = this,
                    accentColor = Color(0xFF3DDC84),
                    secondaryAccentColor = Color(0xFF80E8A8),
                    tagline = "Modern Android architecture with Jetpack Compose, Circuit & Room",
                    iconGlyph = "And",
                    starterPrompts =
                        listOf(
                            "Explain Jetpack Compose Recomposition lifecycle & derivedStateOf",
                            "How to architect offline-first apps with Room and WorkManager?",
                            "What are the benefits of Circuit UDF architecture over MVI/MVVM?",
                            "How to properly handle Edge-to-Edge and IME WindowInsets in Compose?",
                        ),
                )
            }

            CodingTopic.WEB -> {
                TopicVisualInfo(
                    topic = this,
                    accentColor = Color(0xFFFF5722),
                    secondaryAccentColor = Color(0xFFFFAB91),
                    tagline = "Full-stack web standards, performance optimization & APIs",
                    iconGlyph = "Web",
                    starterPrompts =
                        listOf(
                            "Explain Core Web Vitals (LCP, INP, CLS) and how to optimize them",
                            "What is the difference between SSR, SSG, and Client-Side Hydration?",
                            "How does HTTP/2 and HTTP/3 multiplexing improve web speed?",
                            "Explain modern Web Security: CSP, CORS, and CSRF protection",
                        ),
                )
            }

            CodingTopic.UNKNOWN -> {
                TopicVisualInfo(
                    topic = this,
                    accentColor = Color(0xFF9E9E9E),
                    secondaryAccentColor = Color(0xFFE0E0E0),
                    tagline = "Previously selected topic could not be restored",
                    iconGlyph = "?",
                    starterPrompts = emptyList(),
                )
            }
        }
