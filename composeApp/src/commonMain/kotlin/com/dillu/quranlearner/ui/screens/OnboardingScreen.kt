package com.dillu.quranlearner.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dillu.quranlearner.ui.theme.LocalNoorTypography
import com.dillu.quranlearner.ui.theme.NoorColors
import com.dillu.quranlearner.ui.theme.arabicForQuranScript

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val selectedGoal by viewModel.selectedGoal.collectAsState()
    val selectedScript by viewModel.selectedScript.collectAsState()
    val noorType = LocalNoorTypography.current

    var currentPage by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NoorColors.Background)
    ) {
        // Decorative gradient glow at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NoorColors.Primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Decorative gradient glow at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            NoorColors.Secondary.copy(alpha = 0.04f)
                        )
                    )
                )
        )

        // Watermark calligraphy
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "نور",
                style = noorType.arabicDisplay.copy(
                    fontSize = 180.sp,
                    color = NoorColors.Secondary.copy(alpha = 0.04f)
                )
            )
        }

        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
            },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                // ── Page 1: Welcome & Auth ──
                0 -> AuthPage(
                    onSkip = { currentPage = 1 },
                    noorType = noorType
                )
                // ── Page 2: Preferences ──
                1 -> PreferencesPage(
                    selectedScript = selectedScript,
                    selectedGoal = selectedGoal,
                    onScriptChange = { viewModel.setScript(it) },
                    onGoalChange = { viewModel.setGoal(it) },
                    onGetStarted = {
                        viewModel.completeOnboarding()
                        onComplete()
                    },
                    noorType = noorType
                )
            }
        }
    }
}

@Composable
private fun AuthPage(
    onSkip: () -> Unit,
    noorType: com.dillu.quranlearner.ui.theme.NoorTypography
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar with Skip button
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSkip) {
                Text(
                    "SKIP",
                    style = noorType.labelSm,
                    color = NoorColors.OnSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Hero section
        Text(
            text = "Welcome to",
            style = noorType.headlineLg.copy(color = NoorColors.OnSurface)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Noor",
            style = noorType.displayLg.copy(
                brush = Brush.linearGradient(
                    colors = listOf(NoorColors.Primary, NoorColors.Secondary)
                )
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Create an account to backup your progress\nacross all your devices.",
            style = noorType.bodyLg,
            color = NoorColors.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Google button
        GlassButton(
            onClick = { /* TODO: Supabase Google Auth */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = NoorColors.OnSurface)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Continue with Google", style = noorType.bodyMd.copy(fontWeight = FontWeight.SemiBold), color = NoorColors.OnSurface)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Email button
        GlassButton(
            onClick = { /* TODO: Supabase Email Auth */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Email, contentDescription = null, tint = NoorColors.OnSurface)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Continue with Email", style = noorType.bodyMd.copy(fontWeight = FontWeight.SemiBold), color = NoorColors.OnSurface)
        }

        /*
        Spacer(modifier = Modifier.height(12.dp))
        // Apple button
        GlassButton(
            onClick = { /* TODO: Supabase Apple Auth */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue with Apple", style = noorType.bodyMd.copy(fontWeight = FontWeight.SemiBold), color = NoorColors.OnSurface)
        }
        */

        Spacer(modifier = Modifier.weight(1f))

        // Skip & Use Offline
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .height(56.dp)
        ) {
            Text(
                "Skip & Use Offline",
                style = noorType.bodyMd.copy(fontWeight = FontWeight.SemiBold),
                color = NoorColors.OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreferencesPage(
    selectedScript: String,
    selectedGoal: Int,
    onScriptChange: (String) -> Unit,
    onGoalChange: (Int) -> Unit,
    onGetStarted: () -> Unit,
    noorType: com.dillu.quranlearner.ui.theme.NoorTypography
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Header
        Text(
            text = "Personalise",
            style = noorType.displayLg.copy(
                brush = Brush.linearGradient(
                    colors = listOf(NoorColors.Primary, NoorColors.Secondary)
                )
            )
        )
        Text(
            text = "Your Experience",
            style = noorType.displayLg.copy(color = NoorColors.OnSurface)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tailor Noor to match your spiritual journey.",
            style = noorType.bodyLg,
            color = NoorColors.OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Script Selection
        Text(
            text = "Choose Your Script",
            style = noorType.headlineMd,
            color = NoorColors.OnSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Flexible middle zone: cards absorb leftover height so everything
        // always fits on screen, from small phones to tablets.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ScriptCard(
                    label = "UTHMANI",
                    arabicSample = "بِسْمِ",
                    scriptForFont = "Uthmani",
                    isSelected = selectedScript == "Uthmani",
                    onClick = { onScriptChange("Uthmani") },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    noorType = noorType
                )
                ScriptCard(
                    label = "INDO-PAK",
                    arabicSample = "بِسۡمِ",
                    scriptForFont = "IndoPak",
                    isSelected = selectedScript == "IndoPak",
                    onClick = { onScriptChange("IndoPak") },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    noorType = noorType
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Daily Goal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Daily Ayah Goal",
                style = noorType.headlineMd,
                color = NoorColors.OnSurface
            )
            Text(
                text = "$selectedGoal",
                style = noorType.headlineMd,
                color = NoorColors.Secondary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Goal Slider in a glass panel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = NoorColors.SurfaceVariant.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, NoorColors.OutlineVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Slider(
                    value = selectedGoal.toFloat(),
                    onValueChange = { onGoalChange(it.toInt()) },
                    valueRange = 1f..30f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = NoorColors.Secondary,
                        activeTrackColor = NoorColors.Secondary,
                        inactiveTrackColor = NoorColors.OutlineVariant.copy(alpha = 0.3f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 Ayah", style = noorType.labelSm, color = NoorColors.OnSurfaceVariant)
                    Text("30 Ayahs", style = noorType.labelSm, color = NoorColors.OnSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Get Started Button
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(NoorColors.Primary, NoorColors.PrimaryContainer)
                        ),
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Get Started",
                        style = noorType.bodyLg.copy(fontWeight = FontWeight.SemiBold),
                        color = NoorColors.OnPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = NoorColors.OnPrimaryContainer
                    )
                }
            }
        }
    }
}

// ── Reusable Components ──

@Composable
private fun ScriptCard(
    label: String,
    arabicSample: String,
    scriptForFont: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    noorType: com.dillu.quranlearner.ui.theme.NoorTypography
) {
    val borderColor = if (isSelected) NoorColors.Primary else NoorColors.OutlineVariant.copy(alpha = 0.2f)
    val bgColor = if (isSelected) NoorColors.Primary.copy(alpha = 0.05f) else NoorColors.SurfaceContainerLow.copy(alpha = 0.5f)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = NoorColors.Primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(20.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = arabicSample,
                    style = noorType.arabicForQuranScript(scriptForFont).copy(
                        color = if (isSelected) NoorColors.Secondary else NoorColors.OnSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = noorType.labelSm,
                    color = if (isSelected) NoorColors.OnSurface else NoorColors.OnSurfaceVariant,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = NoorColors.SurfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, NoorColors.OutlineVariant.copy(alpha = 0.2f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}
