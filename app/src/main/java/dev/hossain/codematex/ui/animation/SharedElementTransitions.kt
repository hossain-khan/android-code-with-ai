package dev.hossain.codematex.ui.animation

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedTransitionKey

/**
 * Shared transition key for a coding topic's glyph badge (e.g. `KT`, `AND`).
 */
data class TopicGlyphSharedKey(
    val topicId: String,
) : SharedTransitionKey

/**
 * Shared transition key for a coding topic's title text (e.g. "Kotlin Fundamentals").
 */
data class TopicTitleSharedKey(
    val topicId: String,
) : SharedTransitionKey

/**
 * Shared transition key for a coding topic card container bounds transitioning to the top app bar.
 */
data class TopicCardSharedKey(
    val topicId: String,
) : SharedTransitionKey

/**
 * Shared transition key for a learning course card container bounds.
 */
data class CourseCardSharedKey(
    val courseId: String,
) : SharedTransitionKey

/**
 * Shared transition key for a learning course's language badge / glyph.
 */
data class CourseBadgeSharedKey(
    val courseId: String,
) : SharedTransitionKey

/**
 * Shared transition key for a learning course's title text.
 */
data class CourseTitleSharedKey(
    val courseId: String,
) : SharedTransitionKey

/**
 * Shared transition key for a learning course's progress indicator bar.
 */
data class CourseProgressSharedKey(
    val courseId: String,
) : SharedTransitionKey

/**
 * Shared transition key for a lesson item container bounds.
 */
data class LessonCardSharedKey(
    val lessonId: String,
) : SharedTransitionKey

/**
 * Shared transition key for a lesson title text.
 */
data class LessonTitleSharedKey(
    val lessonId: String,
) : SharedTransitionKey

/**
 * Shared transition key for the active model card container bounds.
 */
data object ActiveModelCardSharedKey : SharedTransitionKey

/**
 * Shared transition key for the active model display name text.
 */
data object ActiveModelTitleSharedKey : SharedTransitionKey

/**
 * Shared transition key for the active model's memory/accelerator status badge.
 */
data object ActiveModelBadgeSharedKey : SharedTransitionKey

/**
 * Shared transition key for a chat session card container bounds.
 */
data class SessionCardSharedKey(
    val sessionId: String,
) : SharedTransitionKey

/**
 * Shared transition key for a chat session's topic glyph badge.
 */
data class SessionGlyphSharedKey(
    val sessionId: String,
) : SharedTransitionKey

/**
 * Shared transition key for a chat session's title text.
 */
data class SessionTitleSharedKey(
    val sessionId: String,
) : SharedTransitionKey

/**
 * CompositionLocal providing the current [SharedElementTransitionScope] across
 * subcircuits and nested composables in the UI hierarchy.
 */
val LocalSharedElementTransitionScope = androidx.compose.runtime.compositionLocalOf<SharedElementTransitionScope?> { null }

/**
 * Applies a shared element transition using the [SharedElementTransitionScope.AnimatedScope.Navigation]
 * scope if [scope] is provided and has an active navigation animated scope.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedElementNav(
    scope: SharedElementTransitionScope?,
    key: Any,
): Modifier {
    if (scope == null) return this
    val animatedScope =
        scope.findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)
            ?: return this
    return with(scope) {
        this@sharedElementNav.sharedElement(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedScope,
        )
    }
}

/**
 * Convenience overload that reads [LocalSharedElementTransitionScope].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedElementNav(key: Any): Modifier = sharedElementNav(LocalSharedElementTransitionScope.current, key)

/**
 * Applies a shared bounds transition using the [SharedElementTransitionScope.AnimatedScope.Navigation]
 * scope if [scope] is provided and has an active navigation animated scope.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedBoundsNav(
    scope: SharedElementTransitionScope?,
    key: Any,
    boundsTransform: BoundsTransform? = null,
): Modifier {
    if (scope == null) return this
    val animatedScope =
        scope.findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)
            ?: return this
    return with(scope) {
        if (boundsTransform != null) {
            this@sharedBoundsNav.sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedScope,
                boundsTransform = boundsTransform,
            )
        } else {
            this@sharedBoundsNav.sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedScope,
            )
        }
    }
}

/**
 * Convenience overload that reads [LocalSharedElementTransitionScope].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedBoundsNav(
    key: Any,
    boundsTransform: BoundsTransform? = null,
): Modifier = sharedBoundsNav(LocalSharedElementTransitionScope.current, key, boundsTransform)
