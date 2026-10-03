package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.core.content.ContextCompat
import com.example.data.preferences.AccentPalette
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.floating.FloatingOverlayManager
import com.example.service.translation.TranslationRepository
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferencesRepository: UserPreferencesRepository,
    translationRepository: TranslationRepository,
    accentColor: Color = NeonCyan,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by preferencesRepository.userPreferencesFlow.collectAsState(initial = null)

    var customKeyInput by remember { mutableStateOf(prefs?.customApiKey ?: "") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showClearDatabaseDialog by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            snackbarHostState.showSnackbar(if (granted) "Microphone permission granted" else "Microphone permission denied")
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            snackbarHostState.showSnackbar(if (granted) "Notification permission granted" else "Notification permission denied")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Settings Header
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 14.dp)) {
            Text(
                text = "SETTINGS & SYSTEM",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "Personalization, AI engines, audio, and privacy",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        // Section: Visual Design & Accent
        Text(
            text = "THEME & ACCENT PALETTE",
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
                // Theme Mode selector
                Text(text = "Display Theme", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentMode = prefs?.themeMode ?: AppThemeMode.AMOLED_BLACK
                    val modes = listOf(
                        Triple(AppThemeMode.AMOLED_BLACK, "AMOLED Black", "#000000"),
                        Triple(AppThemeMode.DARK_GLASS, "Dark Glass", "#090B10"),
                        Triple(AppThemeMode.OBSIDIAN, "Obsidian", "#050508")
                    )

                    modes.forEach { (mode, label, _) ->
                        val isSelected = currentMode == mode
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) accentColor else Color(0x221E293B))
                                .border(1.dp, if (isSelected) accentColor else GlassBorderTop, RoundedCornerShape(12.dp))
                                .clickable {
                                    scope.launch { preferencesRepository.setThemeMode(mode) }
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Accent Palette circles
                Text(text = "Liquid Accent Color", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentPalette = prefs?.accentPalette ?: AccentPalette.CYAN
                    AccentPalette.values().forEach { palette ->
                        val isSelected = currentPalette == palette
                        val pColor = Color(palette.hex)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(pColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    scope.launch { preferencesRepository.setAccentPalette(palette) }
                                }
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: AI Translation Engine
        Text(
            text = "AI TRANSLATION ENGINE",
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
                val currentEngine = prefs?.translationEngine ?: "gemini"

                // Gemini Engine Option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (currentEngine == "gemini") accentColor.copy(alpha = 0.15f) else Color.Transparent)
                        .border(1.dp, if (currentEngine == "gemini") accentColor else Color.Transparent, RoundedCornerShape(14.dp))
                        .clickable { scope.launch { preferencesRepository.setTranslationEngine("gemini") } }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Gemini AI Smart (Flash)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Contextual nuance, pronunciation, grammar breakdowns", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    if (currentEngine == "gemini") {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Cloud Engine Option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (currentEngine == "cloud") accentColor.copy(alpha = 0.15f) else Color.Transparent)
                        .border(1.dp, if (currentEngine == "cloud") accentColor else Color.Transparent, RoundedCornerShape(14.dp))
                        .clickable { scope.launch { preferencesRepository.setTranslationEngine("cloud") } }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF60A5FA))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Fast Cloud Network", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Instant fallback, no key configuration required", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    if (currentEngine == "cloud") {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom API Key configuration button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x221E293B))
                        .clickable {
                            customKeyInput = prefs?.customApiKey ?: ""
                            showApiKeyDialog = true
                        }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Custom Gemini API Key", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (prefs?.customApiKey.isNullOrBlank()) "Using AI Studio build secret or default" else "Custom key configured (••••••••)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text(text = "Edit", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Audio & Behavior Toggles
        Text(
            text = "AUDIO & AUTOMATION",
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
                // Auto speak toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(text = "Auto-Pronounce Translations", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(text = "Speak translated text aloud immediately", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = prefs?.autoSpeakTranslation ?: false,
                        onCheckedChange = { scope.launch { preferencesRepository.setAutoSpeakTranslation(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accentColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto copy toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(text = "Auto-Copy to Clipboard", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(text = "Automatically copy upon translation", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = prefs?.autoCopyTranslated ?: false,
                        onCheckedChange = { scope.launch { preferencesRepository.setAutoCopyTranslated(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accentColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Haptic feedback toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(text = "Haptic Vibration Feedback", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(text = "Subtle tactile pulses on taps and drag gestures", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = prefs?.hapticFeedbackEnabled ?: true,
                        onCheckedChange = { scope.launch { preferencesRepository.setHapticFeedbackEnabled(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accentColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Permissions Center
        Text(
            text = "PERMISSIONS MANAGER",
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
                // Overlay permission
                val hasOverlay = FloatingOverlayManager.hasOverlayPermission(context)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = if (hasOverlay) Color(0xFF10B981) else Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "System Overlay Window", color = TextPrimary, fontSize = 13.sp)
                    }
                    Text(
                        text = if (hasOverlay) "Granted" else "Grant",
                        color = if (hasOverlay) Color(0xFF10B981) else accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            if (!hasOverlay) FloatingOverlayManager.openOverlaySettings(context)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mic permission
                val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = if (hasMic) Color(0xFF10B981) else Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Microphone (Voice & Speech)", color = TextPrimary, fontSize = 13.sp)
                    }
                    Text(
                        text = if (hasMic) "Granted" else "Request",
                        color = if (hasMic) Color(0xFF10B981) else accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            if (!hasMic) micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Privacy & Local Data
        Text(
            text = "PRIVACY & STORAGE",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "100% On-Device Local Privacy", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Translation history and bookmarks are stored exclusively on your device in local Room database SQLite. No user data is collected or tracked.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x22EF4444))
                        .border(1.dp, Color(0x44EF4444), RoundedCornerShape(12.dp))
                        .clickable { showClearDatabaseDialog = true }
                        .padding(vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Delete All Stored Data", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About Footer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Lingua Glass • AMOLED Black Edition", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "Version 1.0.0 • Liquid Glass AI Translator", color = TextMuted, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Custom API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            containerColor = GlassCardSurface,
            title = { Text("Gemini API Key", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        text = "Enter your Google Gemini API key to unlock context-aware linguistic translations. You can also configure this in AI Studio Secrets.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customKeyInput,
                        onValueChange = { customKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("AIzaSy...", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = GlassBorderTop,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            preferencesRepository.setCustomApiKey(customKeyInput.trim())
                            showApiKeyDialog = false
                            snackbarHostState.showSnackbar("API Key updated")
                        }
                    }
                ) {
                    Text("Save", color = accentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Clear Database Dialog
    if (showClearDatabaseDialog) {
        AlertDialog(
            onDismissRequest = { showClearDatabaseDialog = false },
            containerColor = GlassCardSurface,
            title = { Text("Delete All Data?", color = TextPrimary) },
            text = {
                Text(
                    text = "This will permanently remove all your saved translation history, favorites, and custom notes from this device.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            translationRepository.clearHistory(keepFavorites = false)
                            showClearDatabaseDialog = false
                            snackbarHostState.showSnackbar("All local data wiped successfully")
                        }
                    }
                ) {
                    Text("Delete Everything", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDatabaseDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}
