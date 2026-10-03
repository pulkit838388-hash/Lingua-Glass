package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.UserPreferencesRepository
import com.example.floating.FloatingOverlayManager
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassCardElevated
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun FloatingTranslatorControlScreen(
    preferencesRepository: UserPreferencesRepository,
    accentColor: Color = NeonCyan,
    isInAppSandboxActive: Boolean,
    onToggleInAppSandbox: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by preferencesRepository.userPreferencesFlow.collectAsState(initial = null)

    var hasOverlayPermission by remember { mutableStateOf(FloatingOverlayManager.hasOverlayPermission(context)) }

    // Recheck permission when screen resumes
    LaunchedEffect(Unit) {
        hasOverlayPermission = FloatingOverlayManager.hasOverlayPermission(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 14.dp)) {
            Text(
                text = "FLOATING TRANSLATOR",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "System-wide Liquid Glass Overlay over any Android app",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        // Permission Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = if (hasOverlayPermission) GlassCardSurface else Color(0xFF1C1318),
            accentGlow = if (hasOverlayPermission) Color(0xFF10B981) else Color(0xFFEF4444)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasOverlayPermission) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (hasOverlayPermission) "System Overlay Granted" else "Overlay Permission Required",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (hasOverlayPermission) "Ready to float over other apps" else "Needed to display bubble over browsers & chat",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (!hasOverlayPermission) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor)
                                .clickable {
                                    FloatingOverlayManager.openOverlaySettings(context)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Grant",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // System Floating Service Switch
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = GlassCardElevated,
            accentGlow = accentColor
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GTranslate,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "System Overlay Service",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Displays floating bubble over all apps",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                val isServiceActive = prefs?.isFloatingBubbleEnabled ?: false
                Switch(
                    checked = isServiceActive,
                    onCheckedChange = { enabled ->
                        if (enabled && !FloatingOverlayManager.hasOverlayPermission(context)) {
                            FloatingOverlayManager.openOverlaySettings(context)
                            scope.launch {
                                snackbarHostState.showSnackbar("Please grant 'Display over other apps' permission first")
                            }
                        } else {
                            scope.launch {
                                preferencesRepository.setFloatingBubbleEnabled(enabled)
                                if (enabled) {
                                    FloatingOverlayManager.startFloatingService(context)
                                    snackbarHostState.showSnackbar("Floating translator started")
                                } else {
                                    FloatingOverlayManager.stopFloatingService(context)
                                    snackbarHostState.showSnackbar("Floating translator stopped")
                                }
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // In-App Interactive Sandbox Switch
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = GlassCardSurface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x259D4EDD))
                            .border(1.dp, Color(0x559D4EDD), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = Color(0xFF9D4EDD),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "In-App Floating Bubble",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Test & drag bubble right inside this app",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = isInAppSandboxActive,
                    onCheckedChange = { onToggleInAppSandbox(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFF9D4EDD),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bubble Customization Controls
        Text(
            text = "BUBBLE CUSTOMIZATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = GlassCardSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Size Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Bubble Size", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = "${prefs?.floatingBubbleSizeDp ?: 58} dp",
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = (prefs?.floatingBubbleSizeDp ?: 58).toFloat(),
                    onValueChange = {
                        scope.launch { preferencesRepository.setFloatingBubbleSize(it.toInt()) }
                    },
                    valueRange = 46f..74f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Opacity Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Glass Opacity", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = "${((prefs?.floatingBubbleOpacity ?: 0.9f) * 100).toInt()}%",
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = prefs?.floatingBubbleOpacity ?: 0.9f,
                    onValueChange = {
                        scope.launch { preferencesRepository.setFloatingBubbleOpacity(it) }
                    },
                    valueRange = 0.5f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Floating Translation Modes Guide
        Text(
            text = "6 FLOATING TRANSLATION MODES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        val modes = listOf(
            Triple(Icons.Default.ContentPaste, "Clipboard Mode", "Copy any text in any app, then tap the floating bubble to translate in 1 tap."),
            Triple(Icons.Default.TouchApp, "Manual Input Mode", "Tap bubble to open mini floating glass panel to type or edit text."),
            Triple(Icons.Default.CropFree, "Selection Mode", "Highlight text anywhere on Android and select 'Translate with Lingua Glass'."),
            Triple(Icons.Default.Chat, "Chat Mode", "Translate incoming messages and reply in the recipient's native language."),
            Triple(Icons.Default.Layers, "Screen OCR Mode", "Extract text directly from screenshots, photos, and game dialogues."),
            Triple(Icons.Default.GTranslate, "Quick Translate Mode", "Instant target-language translation with minimal touch interaction.")
        )

        modes.forEach { (icon, title, desc) ->
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = GlassCardSurface
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x221E293B))
                            .border(1.dp, GlassBorderTop, CircleShape)
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = desc, color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
