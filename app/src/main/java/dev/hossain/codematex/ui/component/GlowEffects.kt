package dev.hossain.codematex.ui.component

import android.graphics.Matrix
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews

/**
 * Default multi-color gradient palette for static glow borders and halos.
 * Uses Google / Gemini AI inspired aurora tones.
 *
 * Based on techniques by Sina Samaki (https://www.sinasamaki.com/glow-slider/).
 */
val DefaultGlowColors: List<Color> =
    listOf(
        Color(0xFF4285F4), // Google Blue
        Color(0xFF9B72CB), // Aurora Purple
        Color(0xFFD96570), // Coral Red
        Color(0xFFF4B400), // Amber Yellow
    )

/**
 * Default sweep-gradient color sequence for rotating continuous glow animations.
 */
val DefaultGlowSweepColors: List<Color> =
    listOf(
        Color.Transparent,
        Color.Transparent,
        Color(0xFFD96570), // Coral Red
        Color(0xFFF4B400), // Amber Yellow
        Color(0xFF34A853), // Green
        Color(0xFF4285F4), // Blue
        Color(0xFF9B72CB), // Purple
        Color.Transparent,
    )

/**
 * A generic UI container that surrounds its content with a static or interactive multi-color gradient
 * border and a soft ambient glow halo.
 *
 * ### Example Usage:
 * ```kotlin
 * GlowBox(
 *     shape = RoundedCornerShape(16.dp),
 *     glowColors = listOf(Color(0xFF4285F4), Color(0xFF9B72CB)),
 *     glowRadius = 18.dp,
 * ) {
 *     Text(
 *         text = "Ask AI Assistant",
 *         modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
 *     )
 * }
 * ```
 *
 * @param modifier The modifier to be applied to the outer layout.
 * @param glowColors The color stops for the gradient border and halo.
 * @param glowRadius The blur spread distance of the outer ambient glow halo.
 * @param glowAlpha Opacity multiplier for the outer glow halo (0f to 1f).
 * @param strokeWidth The thickness of the gradient border outline.
 * @param shape The shape of the container (e.g. [CircleShape], [RoundedCornerShape]).
 * @param backgroundColor The surface color fill behind the content.
 * @param content The composable content placed inside the glow box.
 *
 * @see Modifier.glowBorder
 * @see AnimatedGlowBox
 */
@Composable
fun GlowBox(
    modifier: Modifier = Modifier,
    glowColors: List<Color> = DefaultGlowColors,
    glowRadius: Dp = 16.dp,
    glowAlpha: Float = 0.45f,
    strokeWidth: Dp = 1.5.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .glowBorder(
                    glowColors = glowColors,
                    glowRadius = glowRadius,
                    glowAlpha = glowAlpha,
                    strokeWidth = strokeWidth,
                    shape = shape,
                ).background(color = backgroundColor, shape = shape)
                .clip(shape),
        content = content,
    )
}

/**
 * Modifier extension that draws an ambient outer glow halo and a crisp gradient border around the target element.
 *
 * ### Example Usage:
 * ```kotlin
 * Box(
 *     modifier = Modifier
 *         .glowBorder(
 *             glowColors = listOf(Color.Cyan, Color.Magenta),
 *             shape = CircleShape,
 *             glowRadius = 20.dp,
 *         )
 *         .background(MaterialTheme.colorScheme.surface, CircleShape)
 *         .padding(16.dp)
 * ) {
 *     Icon(Icons.Default.AutoAwesome, contentDescription = null)
 * }
 * ```
 *
 * @param glowColors The color list for the gradient border and outer halo.
 * @param glowRadius The blur radius for the outer atmospheric glow.
 * @param glowAlpha The opacity of the outer glow halo.
 * @param strokeWidth The width of the inner border outline.
 * @param shape The geometry shape of the border and glow outline.
 */
fun Modifier.glowBorder(
    glowColors: List<Color> = DefaultGlowColors,
    glowRadius: Dp = 16.dp,
    glowAlpha: Float = 0.45f,
    strokeWidth: Dp = 1.5.dp,
    shape: Shape = RoundedCornerShape(16.dp),
): Modifier =
    this
        .drawBehind {
            val radiusPx = glowRadius.toPx()
            if (radiusPx > 0f && glowAlpha > 0f) {
                drawGlowHalo(
                    brush = Brush.horizontalGradient(glowColors),
                    glowRadius = radiusPx,
                    glowAlpha = glowAlpha,
                    shape = shape,
                )
            }
        }.border(
            width = strokeWidth,
            brush = Brush.horizontalGradient(glowColors),
            shape = shape,
        )

/**
 * A generic UI container that renders a continuous 360° rotating sweep-gradient neon border with an orbiting glow halo.
 * Ideal for "Ask AI", "Generating...", or highlighted interactive actions.
 *
 * ### Example Usage:
 * ```kotlin
 * AnimatedGlowBox(
 *     shape = CircleShape,
 *     animationDurationMillis = 3000,
 *     enabled = isGenerating,
 * ) {
 *     Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
 *         Icon(Icons.Default.AutoAwesome, contentDescription = null)
 *         Spacer(Modifier.width(8.dp))
 *         Text("Ask AI Tutor")
 *     }
 * }
 * ```
 *
 * @param modifier The modifier to be applied to the container.
 * @param glowColors The sweep gradient color stops for the rotating neon border.
 * @param glowRadius The blur spread of the orbiting glow halo.
 * @param glowAlpha Opacity multiplier for the outer glow (0f to 1f).
 * @param borderWidth The stroke width of the neon border.
 * @param shape The geometric shape of the container.
 * @param animationDurationMillis Duration in milliseconds for a full 360° rotation.
 * @param enabled Whether the rotating animation is currently active.
 * @param backgroundColor The surface color fill behind the content.
 * @param content The composable content inside the box.
 *
 * @see AnimatedGlowButton
 * @see Modifier.animatedGlowBorder
 */
@Composable
fun AnimatedGlowBox(
    modifier: Modifier = Modifier,
    glowColors: List<Color> = DefaultGlowSweepColors,
    glowRadius: Dp = 14.dp,
    glowAlpha: Float = 0.5f,
    borderWidth: Dp = 1.5.dp,
    shape: Shape = CircleShape,
    animationDurationMillis: Int = 3000,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .animatedGlowBorder(
                    glowColors = glowColors,
                    glowRadius = glowRadius,
                    glowAlpha = glowAlpha,
                    borderWidth = borderWidth,
                    shape = shape,
                    animationDurationMillis = animationDurationMillis,
                    enabled = enabled,
                ).background(color = backgroundColor, shape = shape)
                .clip(shape),
        content = content,
    )
}

/**
 * Modifier extension that applies a rotating sweep gradient border with an ambient neon glow halo.
 *
 * ### Example Usage:
 * ```kotlin
 * Button(
 *     onClick = { },
 *     modifier = Modifier.animatedGlowBorder(
 *         shape = CircleShape,
 *         animationDurationMillis = 2500,
 *     ),
 * ) {
 *     Text("Generating...")
 * }
 * ```
 *
 * @param glowColors Color sequence for the sweep gradient.
 * @param glowRadius Blur radius of the outer neon halo.
 * @param glowAlpha Opacity of the outer glow halo.
 * @param borderWidth Stroke thickness of the rotating border.
 * @param shape Geometry shape of the border.
 * @param animationDurationMillis Duration in milliseconds for one full 360° orbit.
 * @param enabled When false, renders a subtle static gradient border without rotating.
 */
@Composable
fun Modifier.animatedGlowBorder(
    glowColors: List<Color> = DefaultGlowSweepColors,
    glowRadius: Dp = 14.dp,
    glowAlpha: Float = 0.5f,
    borderWidth: Dp = 1.5.dp,
    shape: Shape = CircleShape,
    animationDurationMillis: Int = 3000,
    enabled: Boolean = true,
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "glow_rotation")
    val rotation by if (enabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = animationDurationMillis, easing = LinearEasing),
                ),
            label = "rotation_degrees",
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val haloColors =
        remember(glowColors) {
            glowColors.filter { it != Color.Transparent }.ifEmpty { DefaultGlowColors }
        }

    return this
        .drawBehind {
            val radiusPx = glowRadius.toPx()
            if (radiusPx > 0f && glowAlpha > 0f) {
                val haloBrush = createRotatingSweepBrush(haloColors, rotation)
                drawGlowHalo(
                    brush = haloBrush,
                    glowRadius = radiusPx,
                    glowAlpha = glowAlpha,
                    shape = shape,
                )
            }
        }.drawWithContent {
            drawContent()
            val strokePx = borderWidth.toPx()
            val outline = shape.createOutline(size, layoutDirection, this)
            val borderBrush = createRotatingSweepBrush(glowColors, rotation)

            drawOutline(
                outline = outline,
                brush = borderBrush,
                style = Stroke(width = strokePx),
            )
        }
}

/**
 * A ready-to-use button composable with built-in rotating glow border, tactile ripple feedback,
 * and Material 3 elevated styling.
 *
 * ### Example Usage:
 * ```kotlin
 * AnimatedGlowButton(
 *     onClick = { viewModel.onAskAiClicked() },
 *     isGlowing = true,
 * ) {
 *     Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
 *     Spacer(Modifier.width(8.dp))
 *     Text("Ask CodeMateX AI")
 * }
 * ```
 *
 * @param onClick Callback invoked when the button is clicked.
 * @param modifier Modifier applied to the button layout.
 * @param enabled Whether the button is interactable.
 * @param isGlowing Whether the rotating glow border is active.
 * @param glowColors Sweep gradient color list for the rotating glow.
 * @param shape Shape of the button (defaults to [CircleShape] pill).
 * @param containerColor Background container color.
 * @param contentColor Tint color for text and icons inside the button.
 * @param content Content row placed inside the button.
 */
@Composable
fun AnimatedGlowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isGlowing: Boolean = true,
    glowColors: List<Color> = DefaultGlowSweepColors,
    shape: Shape = CircleShape,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier =
            modifier
                .animatedGlowBorder(
                    glowColors = glowColors,
                    enabled = isGlowing && enabled,
                    shape = shape,
                ).clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

/**
 * Creates a [ShaderBrush] with a sweep gradient rotated by [rotationDegrees]
 * around the center of the element using a local [Matrix].
 */
private fun createRotatingSweepBrush(
    colors: List<Color>,
    rotationDegrees: Float,
): Brush =
    object : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            val shader =
                SweepGradientShader(
                    center = size.center,
                    colors = colors,
                )
            val matrix =
                Matrix().apply {
                    postRotate(rotationDegrees, size.width / 2f, size.height / 2f)
                }
            shader.setLocalMatrix(matrix)
            return shader
        }
    }

/**
 * Helper to draw a soft diffused glow halo behind an outline shape.
 */
private fun DrawScope.drawGlowHalo(
    brush: Brush,
    glowRadius: Float,
    glowAlpha: Float,
    shape: Shape,
) {
    val outline = shape.createOutline(size, layoutDirection, this)

    // Draw multi-layered soft outer rings for a rich diffused glow
    for (i in 1..3) {
        val stepFraction = i / 3f
        val alpha = (glowAlpha / i)
        drawOutline(
            outline = outline,
            brush = brush,
            alpha = alpha,
            style = Stroke(width = glowRadius * stepFraction),
        )
    }
}

// =========================================================================
// Previews
// =========================================================================

@ThemePreviews
@Composable
private fun GlowEffectsPreview() {
    CodeWithAIAppTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 1. Static Glow Box (Card style)
                GlowBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    glowRadius = 14.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "GlowBox Container",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Static multi-color gradient border with ambient halo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // 2. Animated Glow Button (Pill shape)
                AnimatedGlowButton(
                    onClick = {},
                    isGlowing = true,
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ask AI Assistant",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                // 3. Animated Glow Search / Input Box (Simulated)
                AnimatedGlowBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    borderWidth = 2.dp,
                    glowRadius = 16.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search coding topics or ask a question...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}
