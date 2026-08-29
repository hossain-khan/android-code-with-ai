# CodeMateX Design System & UI/UX Guidelines

Welcome to the **CodeMateX Design Language Specification**. This document establishes the design principles, color hierarchies, typography standards, component patterns, and adaptive layout rules for the CodeMateX Android app.

**Every AI agent and developer adding new screens, composables, or modifying existing UI MUST adhere to these guidelines.**

---

## 1. Core Design Pillars

```mermaid
graph TD
    Pillar1["1. Atmospheric & Dynamic"] --> Desc1["Jetcaster-inspired radial gradient lighting & topic-specific color accents"]
    Pillar2["2. Material 3 Expressive"] --> Desc2["Expressive tokens, wavy indicators, container color hierarchies & rounded shapes"]
    Pillar3["3. Material You Adaptive"] --> Desc3["Adaptive multi-pane layouts across Compact (phones) and Expanded (tablets/foldables)"]
    Pillar4["4. Developer-First Clarity"] --> Desc4["Monospace glyph badges, syntax-highlighted code blocks, and real-time benchmarking"]
```

1. **Atmospheric & Dynamic Lighting**:
   - Screens and hero components feature smooth top-center radial gradient lighting inspired by Google's Jetcaster sample (`Modifier.radialGradientScrim()`).
   - Each coding topic dynamically tints its background glow, badges, and accents.
2. **Material 3 Expressive Tokens**:
   - Standardize on Material 3 Expressive components (`CircularWavyProgressIndicator`, `LinearWavyProgressIndicator`, `FilledTonalButton`).
   - Strictly follow M3 surface container roles (`surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`).
3. **Material You Adaptive Multi-Pane Layouts**:
   - Mobile-first, but natively responsive. Single-column on compact phones, multi-column and 2-pane master-detail on foldables and tablets.
4. **Developer-Centric Precision**:
   - Clean monospace typography for code and glyphs (`KT`, `UI`, `ALG`, `BUG`), high-contrast syntax highlighting, and dedicated copy-to-clipboard interactions.

---

## 2. Color System & Surface Container Hierarchy

CodeMateX uses Material 3 dynamic color palettes with defined surface container tiers to establish visual hierarchy without heavy drop shadows.

### Surface Container Tier Rules

| Role | Token | Usage Example |
| :--- | :--- | :--- |
| **Base Canvas** | `MaterialTheme.colorScheme.surface` | Background of the screen scaffold |
| **Lowest Container** | `MaterialTheme.colorScheme.surfaceContainerLowest` | Inset content panels, code editor backgrounds |
| **Low Container** | `MaterialTheme.colorScheme.surfaceContainerLow` | Standard cards (`TopicCard`, `SessionCard`, `ModelCard`), unselected state |
| **Default Container** | `MaterialTheme.colorScheme.surfaceContainer` | Top app bars, bottom chat input dock, modal sheets |
| **High Container** | `MaterialTheme.colorScheme.surfaceContainerHigh` | Hero banners, highlighted benchmarking panels, active selected cards |
| **Highest Container** | `MaterialTheme.colorScheme.surfaceContainerHighest` | Chip pills, badges, secondary pill buttons, metadata tags |

### Topic Visual Identity & Dynamic Accents

Every coding topic defined in [`CodingTopic.kt`](../app/src/main/java/dev/hossain/codematex/data/model/CodingTopic.kt) has visual metadata in [`TopicTheme.kt`](../app/src/main/java/dev/hossain/codematex/ui/theme/TopicTheme.kt):

```kotlin
val visualInfo = topic.visualInfo
// visualInfo.accentColor  -> High-visibility topic theme color
// visualInfo.iconGlyph    -> 2-4 character monospace badge (e.g. "KT", "UI", "ALG")
// visualInfo.tagline      -> Engaging subtitle
// visualInfo.starterPrompts -> Interactive prompt suggestions
```

### Ambient Gradient Scrims ([GradientScrim.kt](../app/src/main/java/dev/hossain/codematex/ui/component/GradientScrim.kt))

- **Screen Scaffolds & Hero Banners**: Apply radial gradient scrims directly to the root scaffold:
```kotlin
Scaffold(
    modifier = modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.15f))
) { ... }
```

- **Card-Level Glow & Topic Borders**: Because `Card` paints `containerColor` on its internal surface, apply `.radialGradientScrim()` to the **inner container** (`Row` or `Column`) of the card rather than the outer card modifier, and pair with a topic-tinted border:
```kotlin
Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.25f)),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.12f))
            .padding(16.dp),
    ) { ... }
}
```

---

## 3. Typography & Shapes

### Typography Guidelines
- **Headlines & App Titles**: `MaterialTheme.typography.titleLarge` or `headlineSmall` with `FontWeight.Bold` / `FontWeight.ExtraBold`.
- **Topic Monospace Glyphs**: `FontFamily.Monospace` with `FontWeight.Bold` and `MaterialTheme.typography.labelSmall` inside rounded badges.
- **Body & Explanations**: `MaterialTheme.typography.bodyMedium` with `MaterialTheme.colorScheme.onSurface`.
- **Secondary Metadata**: `MaterialTheme.typography.bodySmall` / `labelSmall` with `MaterialTheme.colorScheme.onSurfaceVariant` or `outline`.

### Shape System
- **Cards & Hero Banners**: `MaterialTheme.shapes.large` (16.dp) or `extraLarge` (28.dp) with subtle 1.dp border (`BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))` or topic accent tint).
- **Pills & Glyphs**: `MaterialTheme.shapes.extraSmall` (4.dp) or `small` (8.dp).
- **Action Buttons & Avatar Icons**: `CircleShape` or `MaterialTheme.shapes.large`.
- **Asymmetric Message Bubbles**:
  - User bubble: `RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)`
  - Assistant bubble: `RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)`

---

## 4. Material You Adaptive Multi-Pane Architecture

CodeMateX supports phones, foldables, tablets, and landscape orientations using AndroidX Window Size Classes:

```kotlin
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
```

### Adaptive Layout Rules

| Screen Size | Breakpoint | Layout Strategy |
| :--- | :--- | :--- |
| **Compact (Phone Portrait)** | `< 600dp` width | Single vertical scrolling column with horizontal carousels for secondary items |
| **Medium (Foldables, Small Tablets)** | `600dp - 840dp` width | 2-column layout or adaptive grid (`GridCells.Adaptive(minSize = 340.dp)`) |
| **Expanded (Large Tablets, Desktop)** | `>= 840dp` width | Multi-pane layout: Master sidebar / Left controls (360dp width) + Main content right pane |

---

## 5. Standard Component Patterns

### A. Top App Bar & Nested Scroll
Always attach `pinnedScrollBehavior` to both `TopAppBar` and parent `Scaffold`:
```kotlin
val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
        TopAppBar(
            title = { Text("Screen Title", fontWeight = FontWeight.Bold) },
            scrollBehavior = scrollBehavior,
        )
    },
) { innerPadding ->
    // Always apply innerPadding to the root scrollable container
}
```

### B. Progress Indicators: Animated Wavy vs. Static Gauge
- **Indeterminate Loading**: Use `CircularWavyProgressIndicator()`.
- **In-Flight / Downloading Determinate Progress**: Use `LinearWavyProgressIndicator(progress = { progressFloat })` for fluid, animated motion during background model downloads.
- **Static Completion Gauges**: Use standard Material 3 `LinearProgressIndicator(progress = { progressFloat }, color = visualInfo.accentColor)` for static completion percentages (e.g. Course & Chapter progress headers) to avoid distracting continuous wave animation.

### C. Cards with Borders
When creating cards with containers, always use `Card` or `OutlinedCard` with explicit `border` (do NOT use `border` on `ElevatedCard`):
```kotlin
Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
) { ... }
```

### D. Empty States
Every screen with dynamic lists MUST include an expressive empty state:
- Icon or Topic Monospace Glyph in a circular tinted surface.
- Clear title and explanatory subtitle.
- Interactive prompt chips or primary action button.

### E. Syntax Highlighting & Code Blocks
CodeMateX uses **`compose-highlight`** ([github.com/hossain-khan/android-compose-highlight](https://github.com/hossain-khan/android-compose-highlight)) (`dev.hossain:compose-highlight:0.34.0`):
- **Streaming Chat Messages**: LLM responses are parsed with `multiplatform-markdown-renderer-m3` (`v0.44.0`) in [`MarkdownMessage`](../app/src/main/java/dev/hossain/codematex/ui/component/MarkdownMessage.kt) using `StreamingSyntaxHighlightedCode`. It retains token spans during live inference to eliminate streaming flicker.
- **Static Lesson Blocks**: Rendered directly with `SyntaxHighlightedCode` in [`LessonScreenUi`](../app/src/main/java/dev/hossain/codematex/ui/screens/lessons/LessonScreenUi.kt) for lightweight, zero-recalculation static syntax highlighting with line numbers.

### F. Interactive Quizzes & Quick Checks
Quick-check quizzes in lesson screens feature real-time visual validation feedback:
- **Option Cards**: M3 `Surface` cards with circular letter glyphs (`A`, `B`, `C`, `D`).
- **Selection State**:
  - Correct selection: `MaterialTheme.colorScheme.primaryContainer` with `Icons.Default.Check`.
  - Incorrect selection: `MaterialTheme.colorScheme.errorContainer` with `Icons.Default.Close`.
  - Unrevealed options: `MaterialTheme.colorScheme.surfaceContainerHigh`.
- **Explanation Banner**: Topic- or error-bordered alert callout displaying an icon (`CheckCircle` / `Info`) and detailed explanation text.

### G. Curriculum Tracking & Lesson Status Badges
Chapter syllabi clearly distinguish lesson progression states:
- **Completed Lesson**: Solid topic-colored circular badge with `✓` (`Icons.Default.Check`), bold `Done` badge chip, and subtle topic accent border.
- **Current Active Lesson**: Ringed topic badge with `▶` (`Icons.Default.PlayArrow`), bold `Current` badge chip, and glowing container (`visualAccent.copy(alpha = 0.12f)` with 1.dp accent border).
- **Upcoming Lesson**: Subtle hollow outline ring (`BorderStroke(1.5.dp, colorScheme.outlineVariant)`) with muted typography.

### H. Compose Previews for Modular UI Elements
Whenever creating or updating modular composable components, always include comprehensive preview functions at the bottom of the file:
- Annotate with `@ThemePreviews` (and `@DevicePreviews` for full screen layouts).
- Always wrap preview calls with `CodeWithAIAppTheme(dynamicColor = false) { Surface { ... } }`.
- Provide previews for multiple visual states (nominal/default, preparing/loading, high-load/error, active).
- Ensure standalone modular components have realistic preview data and padding (`Modifier.padding(16.dp)`).

---

## 6. AI Agent Implementation Checklist

Before finishing any UI task, verify the following checklist:

- [ ] **M3 Surface Container Hierarchy**: Are cards using `surfaceContainerLow` and top/bottom bars using `surfaceContainer`?
- [ ] **Card Scrims & Topic Borders**: Is `radialGradientScrim` applied to the inner card container with `visualInfo.accentColor` borders?
- [ ] **Progress Indicators**: Is `LinearWavyProgressIndicator` used for downloads and static `LinearProgressIndicator` used for curriculum headers?
- [ ] **Adaptive Layout**: Is the screen responsive using `currentWindowAdaptiveInfoV2()`?
- [ ] **Compose Previews**: Are `@ThemePreviews` added with multiple visual states for new or modified modular UI components?
- [ ] **Curriculum Status Badges**: Are completed vs. current vs. upcoming lessons distinctly marked?
- [ ] **Empty States**: Are empty lists handled gracefully with informative visuals?
- [ ] **Formatting & Checks**: Did you run `./gradlew formatKotlin` and `./gradlew check`?

