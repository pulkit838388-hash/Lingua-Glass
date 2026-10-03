package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TtsManager
import com.example.data.model.Languages
import com.example.service.translation.TranslationRepository
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun InAppFloatingBubbleSandbox(
    translationRepository: TranslationRepository,
    ttsManager: TtsManager,
    accentColor: Color = NeonCyan,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val scope = rememberCoroutineScope()

    var isExpanded by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(80f) }
    var offsetY by remember { mutableFloatStateOf(400f) }

    var sourceText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var sourceLang by remember { mutableStateOf(Languages.AUTO) }
    var targetLang by remember { mutableStateOf(Languages.findByCode("es")) }

    fun doTranslate(text: String) {
        if (text.isBlank()) return
        isLoading = true
        scope.launch {
            try {
                val res = translationRepository.translate(
                    text = text,
                    sourceLangCode = sourceLang.code,
                    targetLangCode = targetLang.code,
                    saveToHistory = true
                )
                translatedText = res.translatedText
            } catch (e: Exception) {
                translatedText = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxX = constraints.maxWidth.toFloat() - 200f
        val maxY = constraints.maxHeight.toFloat() - 300f

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        offsetX.coerceIn(20f, maxX.coerceAtLeast(100f)).roundToInt(),
                        offsetY.coerceIn(50f, maxY.coerceAtLeast(200f)).roundToInt()
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        ) {
            if (!isExpanded) {
                // Collapsed Draggable Bubble
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(20.dp, CircleShape, ambientColor = accentColor.copy(alpha = 0.5f), spotColor = accentColor)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xE6101625), Color(0xF5000000))
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.3f), Color(0x66FFFFFF))
                            ),
                            shape = CircleShape
                        )
                        .clickable {
                            isExpanded = true
                            val clip = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!clip.isNullOrBlank() && clip != sourceText) {
                                sourceText = clip
                                doTranslate(clip)
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.GTranslate,
                        contentDescription = "Floating Translator Bubble",
                        tint = accentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            } else {
                // Expanded Glass Window
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xF506080D),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.8f), accentColor.copy(alpha = 0.2f), Color(0x15FFFFFF))
                        )
                    ),
                    shadowElevation = 28.dp,
                    modifier = Modifier.width(310.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Title bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FLOATING GLASS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = accentColor
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Text("Hide", color = Color(0xFF6B7280), fontSize = 10.sp)
                                }
                                IconButton(
                                    onClick = { isExpanded = false },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Minimize",
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Language Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x351E293B))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${sourceLang.flag} ${sourceLang.name}",
                                color = Color.White,
                                fontSize = 12.sp
                            )

                            IconButton(
                                onClick = {
                                    if (sourceLang.code != "auto") {
                                        val temp = sourceLang
                                        sourceLang = targetLang
                                        targetLang = temp
                                        if (translatedText.isNotEmpty()) {
                                            val prev = translatedText
                                            sourceText = prev
                                            doTranslate(prev)
                                        }
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Swap",
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "${targetLang.flag} ${targetLang.name}",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Text input box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x30131722))
                                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                BasicTextField(
                                    value = sourceText,
                                    onValueChange = { sourceText = it },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    ),
                                    cursorBrush = SolidColor(accentColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    decorationBox = { inner ->
                                        if (sourceText.isEmpty()) {
                                            Text(
                                                text = "Type or paste text to translate...",
                                                color = Color(0xFF6B7280),
                                                fontSize = 12.sp
                                            )
                                        }
                                        inner()
                                    }
                                )

                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = {
                                            val clip = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
                                            if (!clip.isNullOrBlank()) {
                                                sourceText = clip
                                                doTranslate(clip)
                                            }
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste",
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(accentColor)
                                            .clickable { doTranslate(sourceText) }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.Black,
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.5.dp
                                            )
                                        } else {
                                            Text(
                                                text = "Translate",
                                                color = Color.Black,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Translated Box
                        if (translatedText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = translatedText,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(
                                            onClick = { ttsManager.speak(translatedText, targetLang) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Speak",
                                                tint = accentColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val clip = ClipData.newPlainText("Translated", translatedText)
                                                clipboardManager.setPrimaryClip(clip)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = accentColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
