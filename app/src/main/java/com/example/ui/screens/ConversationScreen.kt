package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SpeechRecognizerHelper
import com.example.audio.TtsManager
import com.example.data.model.Language
import com.example.data.model.Languages
import com.example.service.translation.TranslationRepository
import com.example.ui.components.GlassIconButton
import com.example.ui.components.LanguageSelectorModal
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

data class ConversationMessage(
    val id: Long = System.currentTimeMillis(),
    val speakerIndex: Int, // 1 or 2
    val originalText: String,
    val translatedText: String,
    val sourceLang: Language,
    val targetLang: Language
)

@Composable
fun ConversationScreen(
    translationRepository: TranslationRepository,
    ttsManager: TtsManager,
    accentColor: Color = NeonCyan,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var langPersonA by remember { mutableStateOf(Languages.findByCode("en")) }
    var langPersonB by remember { mutableStateOf(Languages.findByCode("es")) }

    var showPickerA by remember { mutableStateOf(false) }
    var showPickerB by remember { mutableStateOf(false) }

    var isPersonASpeaking by remember { mutableStateOf(false) }
    var isPersonBSpeaking by remember { mutableStateOf(false) }
    var audioRmsA by remember { mutableFloatStateOf(0f) }
    var audioRmsB by remember { mutableFloatStateOf(0f) }

    var isTranslating by remember { mutableStateOf(false) }
    var faceToFaceMode by remember { mutableStateOf(false) }

    val messages = remember { mutableStateListOf<ConversationMessage>() }

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Speech recognizer for Person A
    val recognizerA = remember {
        SpeechRecognizerHelper(
            context = context,
            onResult = { text ->
                isPersonASpeaking = false
                if (text.isNotBlank()) {
                    isTranslating = true
                    scope.launch {
                        try {
                            val res = translationRepository.translate(
                                text = text,
                                sourceLangCode = langPersonA.code,
                                targetLangCode = langPersonB.code,
                                saveToHistory = true
                            )
                            val msg = ConversationMessage(
                                speakerIndex = 1,
                                originalText = text,
                                translatedText = res.translatedText,
                                sourceLang = langPersonA,
                                targetLang = langPersonB
                            )
                            messages.add(msg)
                            // Auto-speak translated output for Person B
                            ttsManager.speak(res.translatedText, langPersonB)
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Translation error: ${e.message}")
                        } finally {
                            isTranslating = false
                        }
                    }
                }
            },
            onError = { err ->
                isPersonASpeaking = false
                scope.launch { snackbarHostState.showSnackbar(err) }
            },
            onRmsChanged = { rms -> audioRmsA = rms },
            onStateChange = { active -> isPersonASpeaking = active }
        )
    }

    // Speech recognizer for Person B
    val recognizerB = remember {
        SpeechRecognizerHelper(
            context = context,
            onResult = { text ->
                isPersonBSpeaking = false
                if (text.isNotBlank()) {
                    isTranslating = true
                    scope.launch {
                        try {
                            val res = translationRepository.translate(
                                text = text,
                                sourceLangCode = langPersonB.code,
                                targetLangCode = langPersonA.code,
                                saveToHistory = true
                            )
                            val msg = ConversationMessage(
                                speakerIndex = 2,
                                originalText = text,
                                translatedText = res.translatedText,
                                sourceLang = langPersonB,
                                targetLang = langPersonA
                            )
                            messages.add(msg)
                            // Auto-speak translated output for Person A
                            ttsManager.speak(res.translatedText, langPersonA)
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Translation error: ${e.message}")
                        } finally {
                            isTranslating = false
                        }
                    }
                }
            },
            onError = { err ->
                isPersonBSpeaking = false
                scope.launch { snackbarHostState.showSnackbar(err) }
            },
            onRmsChanged = { rms -> audioRmsB = rms },
            onStateChange = { active -> isPersonBSpeaking = active }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            recognizerA.destroy()
            recognizerB.destroy()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Conversation Header Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp)
        ) {
            Column {
                Text(
                    text = "CONVERSATION MODE",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Bilingual Real-Time Dialogue",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Row {
                // Face-to-Face Rotation Toggle
                IconButton(
                    onClick = { faceToFaceMode = !faceToFaceMode },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "Face to Face Mode",
                        tint = if (faceToFaceMode) accentColor else TextSecondary
                    )
                }

                // Clear Conversation Button
                if (messages.isNotEmpty()) {
                    IconButton(
                        onClick = { messages.clear() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Conversation",
                            tint = TextMuted
                        )
                    }
                }
            }
        }

        // Speaker A Controls Panel
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .rotate(if (faceToFaceMode) 180f else 0f),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = GlassCardSurface,
            accentGlow = if (isPersonASpeaking) accentColor else null
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showPickerA = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = langPersonA.flag, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Speaker 1 (${langPersonA.name})",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = langPersonA.nativeName,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                val micPulseA by animateFloatAsState(
                    targetValue = if (isPersonASpeaking) (1f + audioRmsA.coerceIn(0f, 10f) * 0.05f) else 1f,
                    label = "pulse_a"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .scale(micPulseA)
                        .clip(CircleShape)
                        .background(if (isPersonASpeaking) accentColor else Color(0x2500F0FF))
                        .border(1.dp, if (isPersonASpeaking) accentColor else GlassBorderTop, CircleShape)
                        .clickable {
                            if (isPersonASpeaking) {
                                recognizerA.stopListening()
                            } else {
                                recognizerA.startListening(langPersonA)
                            }
                        }
                ) {
                    Icon(
                        imageVector = if (isPersonASpeaking) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Speak Person 1",
                        tint = if (isPersonASpeaking) Color.Black else accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Conversation Dialogue Stream
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            if (messages.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "💬",
                        fontSize = 38.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap either microphone to speak",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Translations are spoken automatically in real time",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isSpeakerA = msg.speakerIndex == 1
                        Column(
                            horizontalAlignment = if (isSpeakerA) Alignment.Start else Alignment.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth(0.88f),
                                shape = RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = if (isSpeakerA) 4.dp else 20.dp,
                                    bottomEnd = if (isSpeakerA) 20.dp else 4.dp
                                ),
                                backgroundColor = if (isSpeakerA) GlassCardElevated else Color(0xFF141926),
                                accentGlow = if (isSpeakerA) accentColor else Color(0xFF9D4EDD)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isSpeakerA) "SPEAKER 1 (${msg.sourceLang.name})" else "SPEAKER 2 (${msg.sourceLang.name})",
                                            color = if (isSpeakerA) accentColor else Color(0xFF9D4EDD),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )

                                        IconButton(
                                            onClick = {
                                                ttsManager.speak(msg.translatedText, msg.targetLang)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Replay Speech",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Original spoken text
                                    Text(
                                        text = msg.originalText,
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Translated text
                                    Text(
                                        text = msg.translatedText,
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isTranslating) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xE60D111A))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Translating...", color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Speaker B Controls Panel
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = GlassCardSurface,
            accentGlow = if (isPersonBSpeaking) Color(0xFF9D4EDD) else null
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showPickerB = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = langPersonB.flag, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Speaker 2 (${langPersonB.name})",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = langPersonB.nativeName,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                val micPulseB by animateFloatAsState(
                    targetValue = if (isPersonBSpeaking) (1f + audioRmsB.coerceIn(0f, 10f) * 0.05f) else 1f,
                    label = "pulse_b"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .scale(micPulseB)
                        .clip(CircleShape)
                        .background(if (isPersonBSpeaking) Color(0xFF9D4EDD) else Color(0x259D4EDD))
                        .border(1.dp, if (isPersonBSpeaking) Color(0xFF9D4EDD) else GlassBorderTop, CircleShape)
                        .clickable {
                            if (isPersonBSpeaking) {
                                recognizerB.stopListening()
                            } else {
                                recognizerB.startListening(langPersonB)
                            }
                        }
                ) {
                    Icon(
                        imageVector = if (isPersonBSpeaking) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Speak Person 2",
                        tint = if (isPersonBSpeaking) Color.Black else Color(0xFF9D4EDD),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    if (showPickerA) {
        LanguageSelectorModal(
            title = "Speaker 1 Language",
            selectedLanguage = langPersonA,
            allowAutoDetect = false,
            onLanguageSelected = {
                langPersonA = it
                showPickerA = false
            },
            onDismissRequest = { showPickerA = false }
        )
    }

    if (showPickerB) {
        LanguageSelectorModal(
            title = "Speaker 2 Language",
            selectedLanguage = langPersonB,
            allowAutoDetect = false,
            onLanguageSelected = {
                langPersonB = it
                showPickerB = false
            },
            onDismissRequest = { showPickerB = false }
        )
    }
}
