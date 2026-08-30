package dev.hossain.codematex.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.R
import dev.hossain.codematex.ui.component.GlowButton
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope
import kotlinx.coroutines.launch

private data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val heroIcon: ImageVector,
    val accentColor: Color,
    val features: List<FeatureItem>,
    val showFeedbackAction: Boolean = false,
)

private data class FeatureItem(
    val title: String,
    val description: String,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(OnboardingScreen::class, AppScope::class)
@Composable
fun OnboardingScreenUi(
    state: OnboardingScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is OnboardingScreen.State.Content -> OnboardingContent(state, modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingContent(
    state: OnboardingScreen.State.Content,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = state.currentPage) { state.pageCount }
    val coroutineScope = rememberCoroutineScope()

    // Sync pager state with presenter state
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            state.eventSink(OnboardingScreen.Event.PageChanged(page))
        }
    }

    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
    }

    val pages = getOnboardingPages()
    val currentPageData = pages[pagerState.currentPage]
    val uriHandler = LocalUriHandler.current
    val animatedAccentColor by animateColorAsState(
        targetValue = currentPageData.accentColor,
        animationSpec = tween(400),
        label = "onboarding_accent_glow",
    )

    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .radialGradientScrim(animatedAccentColor.copy(alpha = 0.16f)),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CodeMateX",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = pagerState.currentPage < state.pageCount - 1,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        TextButton(
                            onClick = { state.eventSink(OnboardingScreen.Event.SkipClicked) },
                        ) {
                            Text(
                                text = "Skip",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                currentPage = pagerState.currentPage,
                pageCount = state.pageCount,
                accentColor = animatedAccentColor,
                onNext = {
                    if (pagerState.currentPage < state.pageCount - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        state.eventSink(OnboardingScreen.Event.GetStartedClicked)
                    }
                },
                onGetStarted = { state.eventSink(OnboardingScreen.Event.GetStartedClicked) },
                onIndicatorClick = { page ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                },
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) { pageIndex ->
            OnboardingPageSlide(
                pageData = pages[pageIndex],
                onOpenFeedback = {
                    state.eventSink(OnboardingScreen.Event.OpenFeedbackClicked)
                    uriHandler.openUri(OnboardingScreen.GITHUB_ISSUES_URL)
                },
            )
        }
    }
}

@Composable
private fun OnboardingPageSlide(
    pageData: OnboardingPageData,
    onOpenFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Icon with Atmospheric Container
        Box(
            modifier =
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(pageData.accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = pageData.heroIcon,
                contentDescription = null,
                tint = pageData.accentColor,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = pageData.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = pageData.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Feature cards
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            pageData.features.forEach { feature ->
                FeatureCard(
                    feature = feature,
                    accentColor = pageData.accentColor,
                )
            }
        }

        // Optional Feedback / Issues CTA Button on Slide 4
        if (pageData.showFeedbackAction) {
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = onOpenFeedback,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                border = BorderStroke(1.dp, pageData.accentColor.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.github_logo),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share Feedback on GitHub",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FeatureCard(
    feature: FeatureItem,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                if (feature.iconRes != null) {
                    Icon(
                        painter = painterResource(feature.iconRes),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                } else if (feature.icon != null) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    currentPage: Int,
    pageCount: Int,
    accentColor: Color,
    onNext: () -> Unit,
    onGetStarted: () -> Unit,
    onIndicatorClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Animated Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pageCount) { index ->
                    val isSelected = currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "dot_width",
                    )
                    val color by animateColorAsState(
                        targetValue =
                            if (isSelected) {
                                accentColor
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.6f,
                                )
                            },
                        animationSpec = tween(300),
                        label = "dot_color",
                    )

                    Box(
                        modifier =
                            Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color),
                    )
                }
            }

            // Action Button (Next or Get Started)
            if (currentPage < pageCount - 1) {
                FilledTonalButton(
                    onClick = onNext,
                    shape = MaterialTheme.shapes.large,
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else {
                GlowButton(
                    onClick = onGetStarted,
                    shape = MaterialTheme.shapes.large,
                    glowColors =
                        listOf(
                            accentColor.copy(alpha = 0.9f),
                            Color(0xFF4285F4),
                            Color(0xFF9B72CB),
                            Color(0xFFD96570),
                        ),
                    glowRadius = 14.dp,
                    glowAlpha = 0.5f,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = accentColor,
                    )
                }
            }
        }
    }
}

private fun getOnboardingPages(): List<OnboardingPageData> =
    listOf(
        OnboardingPageData(
            title = "100% Private On-Device AI",
            subtitle = "Your private coding companion that runs completely on your hardware.",
            heroIcon = Icons.Default.Lock,
            accentColor = Color(0xFF388E3C), // Emerald green
            features =
                listOf(
                    FeatureItem(
                        icon = Icons.Default.Lock,
                        title = "Zero Data Shared",
                        description = "No servers, no API calls, and no prompt tracking. Your code never leaves your device.",
                    ),
                    FeatureItem(
                        icon = Icons.Default.Code,
                        title = "Works Completely Offline",
                        description = "Code, chat, and learn anytime—even on flights, commutes, or off-grid environments.",
                    ),
                    FeatureItem(
                        icon = Icons.Default.CheckCircle,
                        title = "Free & Open Weights",
                        description = "Powered by Google LiteRT-LM runtime with optimized Gemma, Qwen, and Phi-4 models.",
                    ),
                ),
        ),
        OnboardingPageData(
            title = "Interactive Guided Courses",
            subtitle = "Master modern languages through bite-sized lessons and instant quizzes.",
            heroIcon = Icons.AutoMirrored.Filled.MenuBook,
            accentColor = Color(0xFF1976D2), // Blue
            features =
                listOf(
                    FeatureItem(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "Curated Curriculum",
                        description = "Explore Kotlin Coroutines, Python AsyncIO, TypeScript Generics, Go, and Rust.",
                    ),
                    FeatureItem(
                        icon = Icons.Default.CheckCircle,
                        title = "Instant Quick Check Quizzes",
                        description = "Test your knowledge with immediate validation, explanations, and progress tracking.",
                    ),
                    FeatureItem(
                        icon = Icons.Default.Code,
                        title = "Contextual AI Tutor",
                        description = "Stuck on a concept? Tap 'Ask AI Tutor' on any lesson for Socratic guidance.",
                    ),
                ),
        ),
        OnboardingPageData(
            title = "Hardware Acceleration",
            subtitle = "Harness the power of your phone's GPU and unified memory architecture.",
            heroIcon = Icons.Default.Speed,
            accentColor = Color(0xFFF57C00), // Amber/Orange
            features =
                listOf(
                    FeatureItem(
                        icon = Icons.Default.Speed,
                        title = "GPU & NPU Accelerated",
                        description = "Optimized with OpenCL GPU acceleration for high token throughput and low latency.",
                    ),
                    FeatureItem(
                        icon = Icons.Default.Memory,
                        title = "Download Once, Run Forever",
                        description = "Choose models tailored to your phone's RAM (e.g. Gemma 2B or Qwen 1.5B).",
                    ),
                    FeatureItem(
                        icon = Icons.Default.CheckCircle,
                        title = "Smart Memory Management",
                        description = "Automatic background eviction frees 2–4 GB when idle to keep your device responsive.",
                    ),
                ),
        ),
        OnboardingPageData(
            title = "A New Frontier — Join Us!",
            subtitle = "On-device mobile AI is brand new technology. Help us shape the future of coding.",
            heroIcon = Icons.Default.BugReport,
            accentColor = Color(0xFF7B1FA2), // Purple
            showFeedbackAction = true,
            features =
                listOf(
                    FeatureItem(
                        icon = Icons.Default.Code,
                        title = "Bleeding-Edge Concept",
                        description = "Running multi-gigabyte neural networks locally is cutting-edge. Performance varies by device.",
                    ),
                    FeatureItem(
                        icon = Icons.Default.BugReport,
                        title = "Open to Feedback & Issues",
                        description = "Notice quirks or have ideas? We actively welcome issue reports and feature requests.",
                    ),
                    FeatureItem(
                        iconRes = R.drawable.github_logo,
                        title = "Community Driven",
                        description = "Join discussions on GitHub to request new model weights, languages, and features.",
                    ),
                ),
        ),
    )

@ThemePreviews
@Composable
private fun OnboardingScreenUiPage0PrivacyPreview() {
    CodeWithAIAppTheme {
        Surface {
            OnboardingScreenUi(
                state =
                    OnboardingScreen.State.Content(
                        currentPage = 0,
                        pageCount = 4,
                        hasDownloadedModel = false,
                        eventSink = {},
                    ),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun OnboardingScreenUiPage1CoursesPreview() {
    CodeWithAIAppTheme {
        Surface {
            OnboardingScreenUi(
                state =
                    OnboardingScreen.State.Content(
                        currentPage = 1,
                        pageCount = 4,
                        hasDownloadedModel = false,
                        eventSink = {},
                    ),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun OnboardingScreenUiPage2HardwarePreview() {
    CodeWithAIAppTheme {
        Surface {
            OnboardingScreenUi(
                state =
                    OnboardingScreen.State.Content(
                        currentPage = 2,
                        pageCount = 4,
                        hasDownloadedModel = false,
                        eventSink = {},
                    ),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun OnboardingScreenUiPage3CommunityFeedbackPreview() {
    CodeWithAIAppTheme {
        Surface {
            OnboardingScreenUi(
                state =
                    OnboardingScreen.State.Content(
                        currentPage = 3,
                        pageCount = 4,
                        hasDownloadedModel = false,
                        eventSink = {},
                    ),
            )
        }
    }
}
