package com.example.floating

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.LinguaGlassApp
import com.example.MainActivity
import com.example.R
import com.example.audio.TtsManager
import com.example.data.model.Language
import com.example.data.model.Languages
import com.example.service.translation.TranslationRepository
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingTranslatorService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val appViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = appViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private lateinit var translationRepository: TranslationRepository
    private var ttsManager: TtsManager? = null

    // Touch and drag coordinates
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val app = application as LinguaGlassApp
        translationRepository = TranslationRepository(app.database.translationDao(), app.preferencesRepository)
        ttsManager = TtsManager(this)

        initOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundNotification()
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingTranslatorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, LinguaGlassApp.FLOATING_CHANNEL_ID)
            .setContentTitle("Lingua Glass Active")
            .setContentText("Tap bubble on screen to translate text from any app")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close Bubble", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, notification)
    }

    private fun initOverlayView() {
        if (!FloatingOverlayManager.hasOverlayPermission(this)) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingTranslatorService)
            setViewTreeViewModelStoreOwner(this@FloatingTranslatorService)
            setViewTreeSavedStateRegistryOwner(this@FloatingTranslatorService)

            setContent {
                FloatingBubbleContent(
                    onDismissService = { stopSelf() },
                    onFocusChanged = { isExpanded ->
                        updateWindowFocusable(isExpanded)
                    }
                )
            }
        }

        // Handle dragging touch events on the container
        composeView.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(overlayView, params)
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    isDragging
                }
                else -> false
            }
        }

        overlayView = composeView
        try {
            windowManager?.addView(overlayView, layoutParams)
        } catch (_: Exception) {}
    }

    private fun updateWindowFocusable(isExpanded: Boolean) {
        val params = layoutParams ?: return
        if (isExpanded) {
            // Needs focus for typing in the text field
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            // Not focusable so background apps can receive touches
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        windowManager?.updateViewLayout(overlayView, params)
    }

    @Composable
    private fun FloatingBubbleContent(
        onDismissService: () -> Unit,
        onFocusChanged: (Boolean) -> Unit
    ) {
        var isExpanded by remember { mutableStateOf(false) }
        var sourceText by remember { mutableStateOf("") }
        var translatedText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var sourceLang by remember { mutableStateOf(Languages.AUTO) }
        var targetLang by remember { mutableStateOf(Languages.findByCode("es")) }

        val scope = rememberCoroutineScope()
        val clipboardManager = remember { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

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

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(8.dp)
        ) {
            // Collapsed Floating Glass Bubble
            if (!isExpanded) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xE610141D),
                                    Color(0xF0000000)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF00F0FF), Color(0x3300F0FF), Color(0x66FFFFFF))
                            ),
                            shape = CircleShape
                        )
                        .clickable {
                            isExpanded = true
                            onFocusChanged(true)
                            // Auto paste clipboard if available
                            val clip = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!clip.isNullOrBlank() && clip != sourceText) {
                                sourceText = clip
                                doTranslate(clip)
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.GTranslate,
                        contentDescription = "Floating Translator",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Expanded Floating Glass Translation Window
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xF207080C),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x8800F0FF), Color(0x2200F0FF), Color(0x11FFFFFF))
                        )
                    ),
                    shadowElevation = 24.dp,
                    modifier = Modifier
                        .width(320.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        // Header bar
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
                                        .background(Color(0xFF00F0FF))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LINGUA GLASS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Color(0xFF00F0FF)
                                )
                            }

                            Row {
                                // Full app open button
                                IconButton(
                                    onClick = {
                                        val intent = Intent(this@FloatingTranslatorService, MainActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        startActivity(intent)
                                        isExpanded = false
                                        onFocusChanged(false)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInFull,
                                        contentDescription = "Open App",
                                        tint = Color(0xFF8E9BAE),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Minimize back to bubble
                                IconButton(
                                    onClick = {
                                        isExpanded = false
                                        onFocusChanged(false)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Minimize",
                                        tint = Color(0xFF8E9BAE),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Language selector and swap row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x401A202C))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${sourceLang.flag} ${sourceLang.name}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            IconButton(
                                onClick = {
                                    if (sourceLang.code != "auto") {
                                        val temp = sourceLang
                                        sourceLang = targetLang
                                        targetLang = temp
                                        if (translatedText.isNotEmpty()) {
                                            val prevTrans = translatedText
                                            sourceText = prevTrans
                                            doTranslate(prevTrans)
                                        }
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Swap Languages",
                                    tint = Color(0xFF00F0FF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "${targetLang.flag} ${targetLang.name}",
                                color = Color(0xFF00F0FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Input Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x3011151F))
                                .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                BasicTextField(
                                    value = sourceText,
                                    onValueChange = {
                                        sourceText = it
                                    },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF00F0FF)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(55.dp),
                                    decorationBox = { innerTextField ->
                                        if (sourceText.isEmpty()) {
                                            Text(
                                                text = "Type, paste, or copy text anywhere...",
                                                color = Color(0xFF6B7280),
                                                fontSize = 12.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Paste clipboard
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

                                    // Translate button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF00F0FF))
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

                        // Translated Output Box
                        if (translatedText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x3300F0FF))
                                    .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(12.dp))
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
                                        // Speak output
                                        IconButton(
                                            onClick = {
                                                ttsManager?.speak(translatedText, targetLang)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Speak",
                                                tint = Color(0xFF00F0FF),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        // Copy output
                                        IconButton(
                                            onClick = {
                                                val clip = ClipData.newPlainText("Translated Text", translatedText)
                                                clipboardManager.setPrimaryClip(clip)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = Color(0xFF00F0FF),
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

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        appViewModelStore.clear()

        ttsManager?.shutdown()
        ttsManager = null

        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {}
            overlayView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START_FLOATING_TRANSLATOR"
        const val ACTION_STOP = "ACTION_STOP_FLOATING_TRANSLATOR"
    }
}
