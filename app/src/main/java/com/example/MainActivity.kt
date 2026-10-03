package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.audio.TtsManager
import com.example.data.preferences.AccentPalette
import com.example.data.preferences.AppThemeMode
import com.example.service.translation.TranslationRepository
import com.example.ui.components.InAppFloatingBubbleSandbox
import com.example.ui.components.LiquidGlassBottomNav
import com.example.ui.components.NavigationTab
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.FloatingTranslatorControlScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MainTranslateScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.LinguaGlassTheme

class MainActivity : ComponentActivity() {

    private lateinit var ttsManager: TtsManager
    private lateinit var translationRepository: TranslationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as LinguaGlassApp
        translationRepository = TranslationRepository(
            app.database.translationDao(),
            app.preferencesRepository
        )
        ttsManager = TtsManager(this)

        // Extract shared or selected text from intent if present
        val initialText = extractTextFromIntent(intent)

        setContent {
            val userPrefs by app.preferencesRepository.userPreferencesFlow.collectAsState(initial = null)
            val themeMode = userPrefs?.themeMode ?: AppThemeMode.AMOLED_BLACK
            val accentPalette = userPrefs?.accentPalette ?: AccentPalette.CYAN
            val accentColor = Color(accentPalette.hex)

            LinguaGlassTheme(themeMode = themeMode, accentPalette = accentPalette) {
                LinguaGlassAppRoot(
                    translationRepository = translationRepository,
                    ttsManager = ttsManager,
                    preferencesRepository = app.preferencesRepository,
                    accentColor = accentColor,
                    initialSharedText = initialText
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractTextFromIntent(intent: Intent?): String {
        if (intent == null) return ""
        if (intent.action == Intent.ACTION_PROCESS_TEXT) {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            if (!text.isNullOrBlank()) return text
        }
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) return text
        }
        return ""
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}

@Composable
fun LinguaGlassAppRoot(
    translationRepository: TranslationRepository,
    ttsManager: TtsManager,
    preferencesRepository: com.example.data.preferences.UserPreferencesRepository,
    accentColor: Color,
    initialSharedText: String
) {
    var currentTab by remember { mutableStateOf(NavigationTab.TRANSLATE) }
    var inAppSandboxActive by remember { mutableStateOf(false) }
    var showFloatingControlCenter by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = currentTab != NavigationTab.TRANSLATE || showFloatingControlCenter) {
        if (showFloatingControlCenter) {
            showFloatingControlCenter = false
        } else {
            currentTab = NavigationTab.TRANSLATE
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Scaffold(
            containerColor = AmoledBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                LiquidGlassBottomNav(
                    selectedTab = currentTab,
                    onTabSelected = {
                        showFloatingControlCenter = false
                        currentTab = it
                    },
                    accentColor = accentColor
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showFloatingControlCenter) {
                    FloatingTranslatorControlScreen(
                        preferencesRepository = preferencesRepository,
                        accentColor = accentColor,
                        isInAppSandboxActive = inAppSandboxActive,
                        onToggleInAppSandbox = { inAppSandboxActive = it },
                        snackbarHostState = snackbarHostState
                    )
                } else {
                    when (currentTab) {
                        NavigationTab.TRANSLATE -> {
                            MainTranslateScreen(
                                translationRepository = translationRepository,
                                ttsManager = ttsManager,
                                accentColor = accentColor,
                                snackbarHostState = snackbarHostState,
                                initialSharedText = initialSharedText,
                                onNavigateToFloatingSettings = {
                                    showFloatingControlCenter = true
                                }
                            )
                        }
                        NavigationTab.CONVERSATION -> {
                            ConversationScreen(
                                translationRepository = translationRepository,
                                ttsManager = ttsManager,
                                accentColor = accentColor,
                                snackbarHostState = snackbarHostState
                            )
                        }
                        NavigationTab.HISTORY -> {
                            HistoryScreen(
                                translationRepository = translationRepository,
                                ttsManager = ttsManager,
                                accentColor = accentColor,
                                snackbarHostState = snackbarHostState
                            )
                        }
                        NavigationTab.SETTINGS -> {
                            SettingsScreen(
                                preferencesRepository = preferencesRepository,
                                translationRepository = translationRepository,
                                accentColor = accentColor,
                                snackbarHostState = snackbarHostState
                            )
                        }
                    }
                }
            }
        }

        // In-App Interactive Floating Glass Bubble Sandbox (overlaid on top of all screens)
        if (inAppSandboxActive) {
            InAppFloatingBubbleSandbox(
                translationRepository = translationRepository,
                ttsManager = ttsManager,
                accentColor = accentColor,
                onDismiss = { inAppSandboxActive = false }
            )
        }
    }
}
