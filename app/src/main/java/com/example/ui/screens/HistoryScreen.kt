package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
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
import com.example.audio.TtsManager
import com.example.data.model.Languages
import com.example.data.model.TranslationItem
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    translationRepository: TranslationRepository,
    ttsManager: TtsManager,
    accentColor: Color = NeonCyan,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0 = All, 1 = Favorites
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val allHistory by translationRepository.allHistory.collectAsState(initial = emptyList())
    val favorites by translationRepository.favorites.collectAsState(initial = emptyList())

    val displayedItems = remember(selectedTab, searchQuery, allHistory, favorites) {
        val base = if (selectedTab == 0) allHistory else favorites
        if (searchQuery.isBlank()) {
            base
        } else {
            base.filter {
                it.sourceText.contains(searchQuery, ignoreCase = true) ||
                        it.translatedText.contains(searchQuery, ignoreCase = true) ||
                        it.sourceLangName.contains(searchQuery, ignoreCase = true) ||
                        it.targetLangName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val dateFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // History Header Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp)
        ) {
            Column {
                Text(
                    text = "TRANSLATION HISTORY",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${allHistory.size} total translations saved on-device",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            if (allHistory.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Segmented Tabs: All vs Bookmarks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GlassCardSurface)
                .border(1.dp, GlassBorderTop, RoundedCornerShape(16.dp))
                .padding(4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedTab == 0) accentColor else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = if (selectedTab == 0) Color.Black else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "All History (${allHistory.size})",
                        color = if (selectedTab == 0) Color.Black else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedTab == 1) accentColor else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = if (selectedTab == 1) Color.Black else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Saved (${favorites.size})",
                        color = if (selectedTab == 1) Color.Black else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search translations...", color = TextMuted, fontSize = 14.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = accentColor)
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GlassCardSurface,
                unfocusedContainerColor = GlassCardSurface,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = GlassBorderTop,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // List of history items
        if (displayedItems.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(text = if (selectedTab == 0) "📖" else "⭐", fontSize = 38.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (selectedTab == 0) "No translation history yet" else "No saved favorites",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (selectedTab == 0) "Translations you make will automatically appear here" else "Bookmark translations with the star/bookmark icon to save them",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(displayedItems, key = { it.id }) { item ->
                    val sourceLang = Languages.findByCode(item.sourceLangCode)
                    val targetLang = Languages.findByCode(item.targetLangCode)

                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = GlassCardSurface,
                        accentGlow = if (item.isFavorite) accentColor else null
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top row: Language tags and date
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${sourceLang.flag} ${sourceLang.name}",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = " → ",
                                        color = accentColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${targetLang.flag} ${targetLang.name}",
                                        color = accentColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = dateFormatter.format(Date(item.timestamp)),
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Source text
                            Text(
                                text = item.sourceText,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Translated text
                            Text(
                                text = item.translatedText,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons: Speak, Copy, Favorite, Delete
                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Speak button
                                IconButton(
                                    onClick = { ttsManager.speak(item.translatedText, targetLang) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Speak",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Copy button
                                IconButton(
                                    onClick = {
                                        val clip = ClipData.newPlainText("Translated Text", item.translatedText)
                                        clipboardManager.setPrimaryClip(clip)
                                        scope.launch { snackbarHostState.showSnackbar("Copied translation") }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Favorite bookmark button
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            translationRepository.toggleFavorite(item)
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Favorite",
                                        tint = if (item.isFavorite) accentColor else TextSecondary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // Delete button
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            translationRepository.deleteItem(item)
                                            snackbarHostState.showSnackbar("Deleted translation")
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = GlassCardSurface,
            title = { Text(text = "Clear History", color = TextPrimary) },
            text = {
                Text(
                    text = "Would you like to delete all translation history, or keep your saved favorites?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            translationRepository.clearHistory(keepFavorites = true)
                            showClearConfirmDialog = false
                            snackbarHostState.showSnackbar("Cleared non-favorite history")
                        }
                    }
                ) {
                    Text("Keep Favorites", color = accentColor)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            translationRepository.clearHistory(keepFavorites = false)
                            showClearConfirmDialog = false
                            snackbarHostState.showSnackbar("All history deleted")
                        }
                    }
                ) {
                    Text("Delete Everything", color = Color(0xFFEF4444))
                }
            }
        )
    }
}
