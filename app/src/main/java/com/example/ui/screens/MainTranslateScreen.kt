package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SpeechRecognizerHelper
import com.example.audio.TtsManager
import com.example.data.model.Language
import com.example.data.model.Languages
import com.example.data.model.TranslationItem
import com.example.service.translation.TranslationRepository
import com.example.service.translation.TranslationResult
import com.example.ui.components.GlassIconButton
import com.example.ui.components.LanguageSelectorModal
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.GlassBorderBottom
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassCardElevated
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainTranslateScreen(
    translationRepository: TranslationRepository,
    ttsManager: TtsManager,
    accentColor: Color = NeonCyan,
    snackbarHostState: SnackbarHostState,
    initialSharedText: String = "",
    onNavigateToFloatingSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var sourceText by remember { mutableStateOf(initialSharedText) }
    var translationResult by remember { mutableStateOf<TranslationResult?>(null) }
    var currentSavedItem by remember { mutableStateOf<TranslationItem?>(null) }
    var isTranslating by remember { mutableStateOf(false) }

    var sourceLang by remember { mutableStateOf(Languages.AUTO) }
    var targetLang by remember { mutableStateOf(Languages.findByCode("es")) }

    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }

    var isMicActive by remember { mutableStateOf(false) }
    var speechAudioRms by remember { mutableFloatStateOf(0f) }

    // Swap rotation animation
    var swapRotationDegrees by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(targetValue = swapRotationDegrees, label = "swap_rotation")

    // Speech Recognizer
    val speechRecognizer = remember {
        SpeechRecognizerHelper(
            context = context,
            onResult = { text ->
                sourceText = text
                isMicActive = false
            },
            onError = { err ->
                isMicActive = false
                scope.launch { snackbarHostState.showSnackbar(err) }
            },
            onRmsChanged = { rms ->
                speechAudioRms = rms
            },
            onStateChange = { active ->
                isMicActive = active
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    // Auto-translate if text was shared from external app
    remember(initialSharedText) {
        if (initialSharedText.isNotBlank()) {
            scope.launch {
                try {
                    isTranslating = true
                    val res = translationRepository.translate(
                        text = initialSharedText,
                        sourceLangCode = sourceLang.code,
                        targetLangCode = targetLang.code
                    )
                    translationResult = res
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Translation error: ${e.message}")
                } finally {
                    isTranslating = false
                }
            }
        }
    }

    fun executeTranslation() {
        if (sourceText.isBlank()) return
        isTranslating = true
        scope.launch {
            try {
                val res = translationRepository.translate(
                    text = sourceText,
                    sourceLangCode = sourceLang.code,
                    targetLangCode = targetLang.code,
                    saveToHistory = true
                )
                translationResult = res
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Translation failed: ${e.message}")
            } finally {
                isTranslating = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        // App Header Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LINGUA GLASS",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    text = "Liquid AMOLED AI Translator",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Quick floating bubble status pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassCardSurface)
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .clickable { onNavigateToFloatingSettings() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GTranslate,
                        contentDescription = "Floating Overlay",
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Floating",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Language Selector Glass Bar
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = GlassCardSurface,
            accentGlow = accentColor
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Source Language Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showSourcePicker = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(text = sourceLang.flag, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = sourceLang.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (sourceLang.code == "auto") "Auto Detect" else sourceLang.nativeName,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Swap Languages Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape)
                        .clickable {
                            if (sourceLang.code != "auto") {
                                swapRotationDegrees += 180f
                                val temp = sourceLang
                                sourceLang = targetLang
                                targetLang = temp
                                if (translationResult?.translatedText?.isNotEmpty() == true) {
                                    val previousTranslation = translationResult!!.translatedText
                                    sourceText = previousTranslation
                                    executeTranslation()
                                }
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap Languages",
                        tint = accentColor,
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(animatedRotation)
                    )
                }

                // Target Language Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showTargetPicker = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = targetLang.name,
                            color = accentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = targetLang.nativeName,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = targetLang.flag, fontSize = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Source Text Input Liquid Glass Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = GlassCardSurface,
            elevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Input header with auto-detect tag & clear button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (sourceLang.code == "auto") "DETECTING LANGUAGE" else sourceLang.name.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    Row {
                        if (sourceText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    sourceText = ""
                                    translationResult = null
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Text Field
                BasicTextField(
                    value = sourceText,
                    onValueChange = { sourceText = it },
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    decorationBox = { innerTextField ->
                        if (sourceText.isEmpty()) {
                            Text(
                                text = "Enter text or tap the microphone to speak...",
                                color = TextMuted,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Input Action Bar: Mic, Paste, TTS, Character count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Mic Button with pulsing animation
                        val micPulseScale by animateFloatAsState(
                            targetValue = if (isMicActive) (1f + (speechAudioRms.coerceIn(0f, 10f) * 0.05f)) else 1f,
                            label = "mic_pulse"
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .scale(micPulseScale)
                                .clip(CircleShape)
                                .background(if (isMicActive) accentColor else Color(0x221E293B))
                                .border(1.dp, if (isMicActive) accentColor else GlassBorderTop, CircleShape)
                                .clickable {
                                    if (isMicActive) {
                                        speechRecognizer.stopListening()
                                        isMicActive = false
                                    } else {
                                        speechRecognizer.startListening(sourceLang)
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = if (isMicActive) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = if (isMicActive) Color.Black else accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Paste Button
                        GlassIconButton(
                            icon = Icons.Default.ContentPaste,
                            onClick = {
                                val clip = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clip.isNullOrBlank()) {
                                    sourceText = clip
                                    scope.launch { snackbarHostState.showSnackbar("Pasted from clipboard") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Clipboard is empty") }
                                }
                            },
                            contentDescription = "Paste",
                            tint = TextSecondary,
                            size = 40.dp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Speak Source Text
                        if (sourceText.isNotEmpty()) {
                            GlassIconButton(
                                icon = Icons.Default.VolumeUp,
                                onClick = {
                                    ttsManager.speak(sourceText, sourceLang)
                                },
                                contentDescription = "Speak Source",
                                tint = TextSecondary,
                                size = 40.dp
                            )
                        }
                    }

                    Text(
                        text = "${sourceText.length} chars",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large One-Tap Translate Action Button
        LiquidGlassButton(
            text = if (isTranslating) "TRANSLATING..." else "TRANSLATE",
            onClick = { executeTranslation() },
            modifier = Modifier.fillMaxWidth(),
            icon = if (!isTranslating) Icons.Default.AutoAwesome else null,
            accentColor = accentColor,
            textColor = Color.Black,
            enabled = sourceText.isNotBlank() && !isTranslating
        )

        // Translation Result Liquid Glass Card
        AnimatedVisibility(
            visible = translationResult != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val result = translationResult ?: return@AnimatedVisibility
            Column(modifier = Modifier.padding(top = 18.dp)) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    backgroundColor = GlassCardElevated,
                    accentGlow = accentColor,
                    elevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Result Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${targetLang.flag} ${targetLang.name.uppercase()}",
                                    color = accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x3310B981))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = result.providerUsed,
                                        color = Color(0xFF10B981),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Favorite bookmark toggle
                            IconButton(
                                onClick = {
                                    val isFav = currentSavedItem?.isFavorite ?: false
                                    scope.launch {
                                        if (currentSavedItem != null) {
                                            translationRepository.toggleFavorite(currentSavedItem!!)
                                            currentSavedItem = currentSavedItem!!.copy(isFavorite = !isFav)
                                            snackbarHostState.showSnackbar(if (!isFav) "Added to Favorites" else "Removed from Favorites")
                                        }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (currentSavedItem?.isFavorite == true) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (currentSavedItem?.isFavorite == true) accentColor else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Translated Text
                        Text(
                            text = result.translatedText,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 28.sp
                        )

                        // Pronunciation / Romanization if available
                        if (!result.pronunciationOrRomanization.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pronunciation: ${result.pronunciationOrRomanization}",
                                color = accentColor.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // AI Nuance / Context explanation
                        if (!result.explanation.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x25000000))
                                    .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = result.explanation,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }

                        // Alternatives Pills
                        if (result.alternatives.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ALTERNATIVE PHRASINGS",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                result.alternatives.forEach { alt ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0x331E293B))
                                            .border(1.dp, GlassBorderBottom, RoundedCornerShape(10.dp))
                                            .clickable {
                                                translationResult = result.copy(translatedText = alt)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = alt, color = TextPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bottom Actions: Speak, Copy, Share
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Speak
                            GlassIconButton(
                                icon = Icons.Default.VolumeUp,
                                onClick = {
                                    ttsManager.speak(result.translatedText, targetLang)
                                },
                                contentDescription = "Pronounce",
                                tint = accentColor,
                                size = 42.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Copy
                            GlassIconButton(
                                icon = Icons.Default.ContentCopy,
                                onClick = {
                                    val clip = ClipData.newPlainText("Translated Text", result.translatedText)
                                    clipboardManager.setPrimaryClip(clip)
                                    scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
                                },
                                contentDescription = "Copy",
                                tint = accentColor,
                                size = 42.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Share
                            GlassIconButton(
                                icon = Icons.Default.Share,
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, result.translatedText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Translation"))
                                },
                                contentDescription = "Share",
                                tint = accentColor,
                                size = 42.dp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Language Switcher Bar
        Text(
            text = "QUICK TARGET LANGUAGES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(Languages.POPULAR) { lang ->
                val isSelected = lang.code == targetLang.code
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) accentColor.copy(alpha = 0.2f) else GlassCardSurface)
                        .border(1.dp, if (isSelected) accentColor else GlassBorderTop, RoundedCornerShape(12.dp))
                        .clickable {
                            targetLang = lang
                            if (sourceText.isNotBlank()) executeTranslation()
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "${lang.flag} ${lang.name}",
                        color = if (isSelected) accentColor else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Modal Sheet for Source Language
    if (showSourcePicker) {
        LanguageSelectorModal(
            title = "Translate from",
            selectedLanguage = sourceLang,
            allowAutoDetect = true,
            onLanguageSelected = { lang ->
                sourceLang = lang
                showSourcePicker = false
            },
            onDismissRequest = { showSourcePicker = false }
        )
    }

    // Modal Sheet for Target Language
    if (showTargetPicker) {
        LanguageSelectorModal(
            title = "Translate to",
            selectedLanguage = targetLang,
            allowAutoDetect = false,
            onLanguageSelected = { lang ->
                targetLang = lang
                showTargetPicker = false
                if (sourceText.isNotBlank()) executeTranslation()
            },
            onDismissRequest = { showTargetPicker = false }
        )
    }
}
