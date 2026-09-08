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
