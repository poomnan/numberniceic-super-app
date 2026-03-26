package com.numberniceic.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.numberniceic.https.NetworkConfig
import com.numberniceic.https.RetrofitClient
import com.numberniceic.utils.ImageUrlResolver
import com.numberniceic.https.BackendHosts
import com.numberniceic.R
import com.numberniceic.utils.UserContextManager
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.OnReceiveContentListener
import androidx.core.view.ContentInfoCompat
import android.text.TextWatcher
import android.text.Editable
import android.graphics.Color as AndroidColor
import retrofit2.awaitResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBack: () -> Unit, initialSessionId: String? = null, initialMessage: String? = null) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    // State to trigger refresh on resume
    var refreshTrigger by remember { mutableStateOf(0) }

    // Listen to Lifecycle ON_RESUME to refresh data when returning from Login
    // Unified Lifecycle Observer moved to near usage or separate section for clarity
    // Removing the first one as the second one is more comprehensive.

    // Re-read user and session whenever refreshTrigger changes
    val user = remember(refreshTrigger) { 
        var u = UserContextManager.userX(context)
        // Fallback: If JSON parsing failed, try reading raw backup fields
        if (u == null) {
            val prefs = context.getSharedPreferences("userdata", Context.MODE_PRIVATE)
            val savedId = prefs.getString("saved_userid", null)
            val savedName = prefs.getString("saved_realname", null)
            val savedUser = prefs.getString("saved_username", null)
            
            if (savedId != null) {
                // Construct a temporary Userx object with backup data
                u = com.numberniceic.data.admin.Userx(
                    userId = savedId,
                    realName = savedName,
                    surname = null,
                    username = savedUser,
                    birthDay = null,
                    sHour = 0,
                    sMinute = 0,
                    sGender = null,
                    ageYear = 0,
                    ageMonth = 0,
                    ageWeek = 0,
                    ageDay = 0,
                    password = null,
                    status = null,
                    vipcode = null,
                    sProvince = null,
                    avatar = null,
                )
            }
        }
        u
    }

    // Load User Data with better fallbacks
    val userDataPrefs = context.getSharedPreferences("userdata", Context.MODE_PRIVATE)
    val backupUserId = userDataPrefs.getString("saved_userid", null) 
        ?: userDataPrefs.getString("userid", null)
        ?: (user?.userId)


    val userNameToUse = remember(user, refreshTrigger, backupUserId) {
        userDataPrefs.getString("saved_realname", null)?.takeIf { it.isNotBlank() }
            ?: userDataPrefs.getString("saved_username", null)?.takeIf { it.isNotBlank() }
            ?: user?.realName?.takeIf { it.isNotBlank() }
            ?: user?.username?.takeIf { it.isNotBlank() }
            ?: backupUserId // Fallback to ID as name if nothing else
    }

    val userAvatarId = remember(user, refreshTrigger) {
        user?.avatar?.takeIf { !it.isNullOrBlank() } ?: "10"
    }
    
    val chatPrefs = remember { context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE) }

    // Load fresh sessionId from Prefs, or use initial if provided
    var sessionId by remember { 
        mutableStateOf(initialSessionId ?: (chatPrefs.getString("session_id", "") ?: "")) 
    }
    var messages by remember { mutableStateOf(listOf<com.numberniceic.data.chat.ChatMessage>()) }
    var inputText by remember { mutableStateOf(initialMessage ?: "") }
    
    // Debug Stats State
    var lastLatency by remember { mutableStateOf(0L) }
    var pollCount by remember { mutableStateOf(0) }
    
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var zoomedImageUrl by remember { mutableStateOf<String?>(null) }

    val handleRichContent: (android.net.Uri) -> Unit = { uri ->
        if (sessionId.isNotEmpty()) {
            scope.launch {
                val tempId = -System.currentTimeMillis()
                val tempMsg = com.numberniceic.data.chat.ChatMessage(
                    messageId = tempId,
                    sessionId = sessionId,
                    senderType = "customer",
                    senderName = userNameToUse,
                    senderAvatar = userAvatarId,
                    message = "",
                    imageUrl = uri.toString(),
                    isRead = false,
                    createdAt = org.joda.time.DateTime.now().toString(),
                    isSending = true,
                    isFailed = false
                )
                messages = messages + tempMsg
                delay(100)
                listState.animateScrollToItem(messages.lastIndex)

                try {
                    val part = uriToMultipart(context, uri)
                    if (part == null) {
                        messages = messages.map { if (it.messageId == tempId) it.copy(isFailed = true, isSending = false) else it }
                        return@launch
                    }

                    val uploadRes = RetrofitClient.api.uploadChatImage(part).awaitResponse()
                    if (uploadRes.isSuccessful && uploadRes.body() != null) {
                        val serverUrl = uploadRes.body()!!.get("url").asString
                        val fullUrl = com.numberniceic.https.BackendHosts.NAMING_BASE + if (serverUrl.startsWith("/")) serverUrl else "/$serverUrl"

                        val json = JsonObject().apply {
                            addProperty("session_id", sessionId)
                            addProperty("sender", "customer")
                            addProperty("message", "")
                            addProperty("image_url", fullUrl)
                        }

                        val res = RetrofitClient.api.sendChatMessage(json).awaitResponse()
                        if (res.isSuccessful && res.body() != null) {
                            // 🎶 Play Sound when message is successfully sent to server
                            com.numberniceic.utils.SoundManager.playSendSound(context)

                            val realMsg = res.body()!!
                            val alreadyExists = messages.any { it.messageId == realMsg.messageId }
                            val currentList = messages.toMutableList()
                            val idx = currentList.indexOfFirst { it.messageId == tempId }
                            
                            if (idx != -1) {
                                if (!alreadyExists) {
                                    currentList[idx] = realMsg
                                } else {
                                    currentList.removeAt(idx)
                                }
                            } else if (!alreadyExists) {
                                currentList.add(realMsg)
                            }
                            // 🚀 Update and sort
                            messages = currentList.sortedBy { if (it.messageId < 0) -it.messageId else it.messageId }
                            
                            // 🚀 Ensure scroll after sending
                            delay(100)
                            listState.scrollToItem(messages.size - 1)
                        } else {
                            messages = messages.map { if (it.messageId == tempId) it.copy(isFailed = true, isSending = false) else it }
                        }
                    } else {
                        messages = messages.map { if (it.messageId == tempId) it.copy(isFailed = true, isSending = false) else it }
                    }
                } catch (e: Exception) {
                    messages = messages.map { if (it.messageId == tempId) it.copy(isFailed = true, isSending = false) else it }
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            handleRichContent(uri)
        }
    }
    
    LaunchedEffect(refreshTrigger, user) {
        val userPrefs = context.getSharedPreferences("userdata", Context.MODE_PRIVATE)
        
        val finalUid = (userPrefs.getString("saved_userid", null) ?: userPrefs.getString("userid", null))?.toIntOrNull() 
            ?: user?.userId?.toIntOrNull() 
            ?: 0

        var currentSess = chatPrefs.getString("session_id", "") ?: ""
        
        // 🛡️ Strict Validation: Ensure sessionId matches current UserID if logged in
        if (finalUid > 0) {
            val expectedSess = "u$finalUid"
            if (currentSess != expectedSess) {
                android.util.Log.d("ChatDebug", "Session mismatch. Resetting everything.")
                currentSess = expectedSess
                messages = emptyList() // 🚀 Clear old messages immediately
                chatPrefs.edit().putString("session_id", expectedSess).apply()
            }
        } else if (currentSess.isEmpty() || currentSess.startsWith("u")) {
            // Guest or Empty - Ensure we have a valid guest ID starting with 'g'
            if (currentSess.startsWith("u") || currentSess.isEmpty()) {
                currentSess = "g" + System.currentTimeMillis()
                messages = emptyList()
                chatPrefs.edit().putString("session_id", currentSess).apply()
                android.util.Log.d("ChatDebug", "Generated Optimistic Guest ID: $currentSess")
            }
        }
        sessionId = currentSess

        // 🚀 ALWAYS attempt Init to ensure Server-Side persistence (UPSERT)
        // This fixes the bug where Admin cannot reply if DB was reset but Client kept the ID
        try {
            val guestName = if (userNameToUse.isNullOrBlank()) "Guest" else "คุณ$userNameToUse"
            val deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
            
            // Get FCM token for notifications
            val fcmToken = try {
                val prefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
                prefs.getString("fcm_token", "") ?: ""
            } catch (e: Exception) {
                ""
            }
            
            val json = JsonObject().apply {
                addProperty("name", guestName)
                addProperty("user_id", finalUid)
                addProperty("session_id", sessionId) // Pass existing ID to resume
                addProperty("device_id", deviceId) // 🚀 Sticky Session for Guests
                addProperty("fcm_token", fcmToken) // 🔔 Enable notifications
            }
            
            android.util.Log.d("ChatDebug", "Ensuring Session Persistence for UID $finalUid (FCM: ${fcmToken.take(10)}...)...")
            val response = RetrofitClient.api.initChat(json).awaitResponse()
            if (response.isSuccessful) {
                val newId = response.body()?.get("session_id")?.asString ?: ""
                android.util.Log.d("ChatDebug", "InitChat Success: $newId (Previous: $sessionId)")
                
                // If it's a new session ID for the user (or diff from cache), update it
                // Logic: A Valid User should have stable session ID on server
                if (newId.isNotEmpty() && newId != sessionId) {
                    sessionId = newId
                    chatPrefs.edit().putString("session_id", newId).apply()
                }
            } else {
                android.util.Log.e("ChatDebug", "InitChat Failed: ${response.code()} ${response.errorBody()?.string()}")
                if (sessionId.isEmpty()) {
                    android.widget.Toast.makeText(context, "Server Error: ${response.code()}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("ChatDebug", "Init Error: ${e.message}")
            if (sessionId.isEmpty()) {
                // Only show toast if we are NOT navigating away and we don't have a session at all
                android.widget.Toast.makeText(context, "Network Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🆕 Update Last Read ID whenever messages changes - BUT ONLY IF RESUMED
    // Fixes bug where background activity eats the unread count
    LaunchedEffect(messages) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            if (messages.isNotEmpty()) {
                val lastMsg = messages.maxByOrNull { it.messageId }
                if (lastMsg != null) {
                    com.numberniceic.utils.ChatNotificationManager.setLastReadMessageId(context, lastMsg.messageId)
                    com.numberniceic.utils.ChatNotificationManager.clearUnreadCount(context)
                    
                    // 🆕 Notify Server that we have read the messages (Fixes Admin 'อ่านแล้ว' status)
                    if (!sessionId.isNullOrEmpty()) {
                        scope.launch {
                            try {
                                val response = com.numberniceic.https.RetrofitClient.api.markChatAsReadGo(sessionId).awaitResponse()
                                if (response.isSuccessful) {
                                    android.util.Log.d("ChatDebug", "Server: Marked as read for $sessionId")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ChatDebug", "Error marking read on server", e)
                            }
                        }
                    }
                }
            }
        }
    }

    if (zoomedImageUrl != null) {
        ImageViewerDialog(imageUrl = zoomedImageUrl!!) { zoomedImageUrl = null }
    }

    // 🆕 Manage 'isChatScreenOpen' state & Refreshing
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                com.numberniceic.utils.ChatNotificationManager.isChatScreenOpen = true
                refreshTrigger++ // 🚀 Trigger data reload
                
                // 🆕 Reset unread count & Mark server read
                com.numberniceic.utils.ChatNotificationManager.clearUnreadCount(context)
                
                // 🆕 Update Badge UI
                context.sendBroadcast(android.content.Intent("com.numberniceic.NEW_NOTIFICATION"))
                
                val sid = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE).getString("session_id", "") ?: ""
                if (sid.isNotEmpty()) {
                    RetrofitClient.api.markChatAsReadGo(sid).enqueue(object : retrofit2.Callback<com.google.gson.JsonObject> {
                        override fun onResponse(call: retrofit2.Call<com.google.gson.JsonObject>, response: retrofit2.Response<com.google.gson.JsonObject>) {
                            android.util.Log.d("ChatScreen", "Mark Read Result: ${response.code()}")
                        }
                        override fun onFailure(call: retrofit2.Call<com.google.gson.JsonObject>, t: Throwable) {}
                    })
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                com.numberniceic.utils.ChatNotificationManager.isChatScreenOpen = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            com.numberniceic.utils.ChatNotificationManager.isChatScreenOpen = false
        }
    }

    // 📌 Logic 2: High-Speed Sync Management
    LaunchedEffect(sessionId) {
        if (sessionId.isEmpty()) {
            messages = emptyList() // 🚀 Wipe if session becomes empty
            return@LaunchedEffect
        }
        
        // 🚀 Wipe messages before starting a NEW session sync
        messages = emptyList() 
        
        android.util.Log.d("ChatDebug", "High-speed sync started for $sessionId")
        while (true) {
            try {
                val start = System.currentTimeMillis()
                // Use history for the first load, then we can switch to poll if needed
                val response = RetrofitClient.api.getChatHistory(sessionId).awaitResponse()
                lastLatency = System.currentTimeMillis() - start
                pollCount++

                if (response.isSuccessful && response.body() != null) {
                    val fetched = response.body()!!
                    
                    // 🛡️ Optimistic UI + Server Sync: 
                    // Filter out local messages that have been confirmed by server (matched by content/image)
                    val localStateMsgs = messages.filter { it.messageId < 0 }.toMutableList()
                    val fetchedKeys = fetched.map { "${it.senderType}:${it.message}:${it.imageUrl}" }.toSet()
                    localStateMsgs.removeAll { msg ->
                        fetchedKeys.contains("${msg.senderType}:${msg.message}:${msg.imageUrl}")
                    }
                    
                    val merged = (fetched + localStateMsgs)
                        .distinctBy { it.messageId }
                        .sortedBy { if (it.messageId < 0) -it.messageId else it.messageId }
                    
                    if (merged != messages) {
                        val isFirstLoad = messages.isEmpty()
                        val hasNewTotal = merged.size > messages.size
                        
                        messages = merged
                        
                        // Scroll only if it's new data or first load
                        if (isFirstLoad || hasNewTotal) {
                            // 🎶 Play Sound if it's a new message from the other party (Admin)
                            if (!isFirstLoad && hasNewTotal) {
                                val lastMsg = merged.lastOrNull()
                                if (lastMsg?.senderType == "admin") {
                                    com.numberniceic.utils.SoundManager.playChatSound(context)
                                }
                            }
                            
                            // 🚀 Auto-scroll handled by MessageList's LaunchedEffect(messages.size)
                            // But for aggressive recovery on S8+, we still keep a small trigger here if needed
                            // for non-size-changing updates (rare in chat)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                delay(3000) // Error fallback
            }
            // ⚡ Relaxed to 1200ms to avoid overwhelming old CPUs (S8) 
            // and ensure SSL handshakes have time to breathe.
            delay(1200) 
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("คุยกับคุณนิน", color = Color.White) }, 
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF457E1E))
                )
                
                        // 🛠 Debug Bar (V5)
                val dispUid = (context.getSharedPreferences("userdata", Context.MODE_PRIVATE).getString("saved_userid", null)) ?: user?.userId ?: "0"
                Surface(color = Color.Black, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "สถานะ: ออนไลน์ | ${if (userNameToUse == null) "บุคคลทั่วไป" else "เชื่อมต่อคุณ$userNameToUse"}",
                            color = Color(0xFF81C784),
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp, 
                modifier = Modifier
                    .imePadding()
                    .navigationBarsPadding() // 🛡️ Fix for Samsung S22/Modern Nav Bars
            ) {
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(24.dp))
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                AppCompatEditText(ctx).apply {
                                    hint = "พิมพ์ข้อความ..."
                                    background = null
                                    textSize = 16f
                                    setTextColor(AndroidColor.BLACK)
                                    setHintTextColor(AndroidColor.GRAY)
                                    
                                    // 🚀 CRITICAL: Support stickers/GIFs natively!
                                    ViewCompat.setOnReceiveContentListener(
                                        this,
                                        arrayOf("image/*", "image/gif", "image/png", "image/jpeg", "image/webp"),
                                        object : OnReceiveContentListener {
                                            override fun onReceiveContent(view: android.view.View, payload: ContentInfoCompat): ContentInfoCompat? {
                                                val split = payload.partition { item -> item.uri != null }
                                                val uriData = split.first
                                                val other = split.second
                                                
                                                if (uriData != null) {
                                                    val clip = uriData.clip
                                                    if (clip.itemCount > 0) {
                                                        val uri = clip.getItemAt(0).uri
                                                        if (uri != null) {
                                                            handleRichContent(uri)
                                                        }
                                                    }
                                                }
                                                return other
                                            }
                                        }
                                    )

                                    addTextChangedListener(object : TextWatcher {
                                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                            inputText = s?.toString() ?: ""
                                        }
                                        override fun afterTextChanged(s: Editable?) {}
                                    })
                                }
                            },
                            update = { editText ->
                                if (editText.text.toString() != inputText) {
                                    editText.setText(inputText)
                                    editText.setSelection(inputText.length)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        )
                    }
                    
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach Image",
                            tint = Color.Gray
                        )
                    }

                    var isSending by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !sessionId.isNullOrEmpty()) {
                                val textToSend = inputText
                                inputText = ""
                                // Note: We don't block UI with isSending anymore, we use Optimistic UI
                                
                                // 1. Optimistic Add (Temp Message)
                                val tempId = -System.currentTimeMillis()
                                val tempMsg = com.numberniceic.data.chat.ChatMessage(
                                    messageId = tempId,
                                    sessionId = sessionId,
                                    senderType = "customer", // User is sending
                                    senderName = userNameToUse,
                                    senderAvatar = userAvatarId,
                                    message = textToSend,
                                    isRead = false,
                                    createdAt = org.joda.time.DateTime.now().toString(),
                                    isSending = true,
                                    isFailed = false
                                )
                                messages = messages + tempMsg
                                scope.launch { 
                                    delay(100)
                                    listState.animateScrollToItem(messages.lastIndex) 
                                }

                                scope.launch {
                                    val json = JsonObject().apply {
                                        addProperty("session_id", sessionId)
                                        addProperty("sender", "customer")
                                        addProperty("message", textToSend)
                                        
                                        // 🛡️ Piggyback Init: Send guest info with the message itself to avoid race conditions
                                        if (sessionId.startsWith("g") && messages.size <= 1) {
                                            val guestName = if (userNameToUse.isNullOrBlank()) "Guest" else "คุณ$userNameToUse"
                                            val deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
                                            val fcmToken = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE).getString("fcm_token", "") ?: ""
                                            
                                            addProperty("guest_name", guestName)
                                            addProperty("device_id", deviceId)
                                            addProperty("fcm_token", fcmToken)
                                            android.util.Log.d("ChatDebug", "Piggybacking Init Info for: $sessionId")
                                        }
                                    }
                                    try {
                                        android.util.Log.d("ChatDebug", "Sending message: $textToSend")
                                        // No need for separate InitChat call anymore

                                        val res = RetrofitClient.api.sendChatMessage(json).awaitResponse()
                                        if (res.isSuccessful && res.body() != null) {
                                            // 🎶 Play Sound
                                            com.numberniceic.utils.SoundManager.playSendSound(context)
                                            val realMsg = res.body()!!
                                            val currentList = messages.toMutableList()
                                            
                                            // 🛡️ Find temp message and replace
                                            val idx = currentList.indexOfFirst { it.messageId == tempId }
                                            
                                            // Check race condition (Poller might have it)
                                            val alreadyExists = messages.any { it.messageId == realMsg.messageId }
                                            
                                            if (idx != -1) {
                                                 if (!alreadyExists) {
                                                     currentList[idx] = realMsg
                                                 } else {
                                                     // If poller got it, remove temp
                                                     currentList.removeAt(idx)
                                                 }
                                            } else if (!alreadyExists) {
                                                currentList.add(realMsg)
                                            }
                                            messages = currentList
                                        } else {
                                            // Failed - Update temp message state
                                            val currentList = messages.toMutableList()
                                            val idx = currentList.indexOfFirst { it.messageId == tempId }
                                            if (idx != -1) {
                                                currentList[idx] = currentList[idx].copy(isFailed = true, isSending = false)
                                                messages = currentList
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ChatDebug", "Send failed", e)
                                        val currentList = messages.toMutableList()
                                        val idx = currentList.indexOfFirst { it.messageId == tempId }
                                        if (idx != -1) {
                                            currentList[idx] = currentList[idx].copy(isFailed = true, isSending = false)
                                            messages = currentList
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send Message", 
                            tint = Color(0xFF457E1E)
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (sessionId.isNullOrEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF457E1E))
                    Spacer(Modifier.height(16.dp))
                    Text("Connecting to support...", color = Color.Gray)
                }
            }
        } else {
            Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFD6E4F0))) {
                if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("เริ่มสนทนากับคุณนินได้เลย", color = Color.Gray)
                    }
                } else {
                    MessageList(messages, listState, userNameToUse, onRetry = { failedMsg ->
                        // 🔄 Retry Logic
                        val idx = messages.indexOf(failedMsg)
                        if (idx != -1) {
                            val retryList = messages.toMutableList()
                            retryList[idx] = failedMsg.copy(isFailed = false, isSending = true)
                            messages = retryList
                            
                            // Re-use logic: We need to expose send logic.
                            // But cleaner is to just launch the same logic block.
                            scope.launch {
                                // 1. Prepare JSON
                                val json = JsonObject().apply {
                                    addProperty("session_id", sessionId)
                                    addProperty("sender", "customer")
                                    addProperty("message", failedMsg.message)
                                }
                                try {
                                    val res = RetrofitClient.api.sendChatMessage(json).awaitResponse()
                                    if (res.isSuccessful && res.body() != null) {
                                        // 🎶 Play Sound
                                        com.numberniceic.utils.SoundManager.playSendSound(context)
                                        val realMsg = res.body()!!
                                        val currentList = messages.toMutableList()
                                        val i = currentList.indexOfFirst { it.messageId == failedMsg.messageId }
                                        if (i != -1) currentList[i] = realMsg
                                        messages = currentList
                                    } else {
                                        // Still failed
                                        val currentList = messages.toMutableList()
                                        val i = currentList.indexOfFirst { it.messageId == failedMsg.messageId }
                                        if (i != -1) currentList[i] = failedMsg.copy(isFailed = true, isSending = false)
                                        messages = currentList
                                    }
                                } catch (e: Exception) {
                                    val currentList = messages.toMutableList()
                                    val i = currentList.indexOfFirst { it.messageId == failedMsg.messageId }
                                    if (i != -1) currentList[i] = failedMsg.copy(isFailed = true, isSending = false)
                                    messages = currentList
                                }
                            }
                        }
                    }, onImageClick = { zoomedImageUrl = it })
                }
            }
        }
    }
}

@Composable
fun MessageList(
    messages: List<com.numberniceic.data.chat.ChatMessage>, 
    listState: LazyListState, 
    currentUserName: String?,
    onRetry: (com.numberniceic.data.chat.ChatMessage) -> Unit,
    onImageClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // 🚀 Auto-scroll internal to MessageList for better reliability on S8+
    // Use last messageId as key to trigger scroll even if size doesn't change (e.g. message replaced)
    val lastMessageId = remember(messages) { messages.lastOrNull()?.messageId }
    
    LaunchedEffect(lastMessageId) {
        if (lastMessageId != null) {
            // S8+ / Older devices need more time for layout to settle
            // We will try multiple times to ensure it's at the bottom
            repeat(3) { i ->
                delay(if (i == 0) 600L else 400L)
                try {
                    listState.scrollToItem(messages.size - 1)
                    // Small extra delay before animation to ensure UI thread is free
                    delay(100)
                    listState.animateScrollToItem(messages.size - 1)
                } catch (e: Exception) { }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp), 
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages, key = { it.messageId }) { msg ->
            ChatBubble(msg, currentUserName, onRetry, onImageClick)
        }
    }
}

@Composable
fun ChatBubble(
    msg: com.numberniceic.data.chat.ChatMessage, 
    localUserName: String?, 
    onRetry: (com.numberniceic.data.chat.ChatMessage) -> Unit,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    val isAdmin = msg.senderType == "admin"
    val displayName = if (isAdmin) {
        "คุณนิน"
    } else {
        if (!localUserName.isNullOrBlank()) "คุณ$localUserName" else msg.senderName ?: "Guest"
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isAdmin) Alignment.Start else Alignment.End
    ) {
        if (!isAdmin) {
            // Display sender name for user only (Admin knows who they are)
             Text(
                text = displayName,
                fontSize = 12.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(bottom = 4.dp, start = 8.dp, end = 8.dp)
            )
        } else {
             Text(
                text = displayName,
                fontSize = 12.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(bottom = 4.dp, start = 8.dp, end = 8.dp)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isAdmin) Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.Top
        ) {

            Surface(
                color = if (isAdmin) Color.White else if (msg.isFailed) Color(0xFFFFEBEE) else Color(0xFF8DE08D),
                shape = RoundedCornerShape(
                    topStart = if (isAdmin) 4.dp else 18.dp,
                    topEnd = 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = if (isAdmin) 18.dp else 4.dp
                ),
                shadowElevation = 1.dp
            ) {
                Column {
                    if (!msg.imageUrl.isNullOrBlank()) {
                        val resolvedUrl = ImageUrlResolver.resolveNamingChat(msg.imageUrl)

                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .sizeIn(maxWidth = 200.dp, maxHeight = 300.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .clickable { onImageClick(msg.imageUrl) }
                        ) {
                            SubcomposeAsyncImage(
                                model = resolvedUrl,
                                contentDescription = "Chat Image",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit,
                                loading = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    }
                                },
                                error = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Gray)
                                    }
                                },
                                onSuccess = {
                                    android.util.Log.d("ChatBubble", "Image loaded: $resolvedUrl")
                                },
                                onError = { state ->
                                    val error = state.result.throwable.message ?: "Unknown Error"
                                    android.util.Log.e("ChatBubble", "Image load failed [url: $resolvedUrl]: $error")
                                    // Toast removed as requested because some images might be deleted from server
                                }
                            )
                        }
                    }
                    if (!msg.message.isNullOrBlank()) {
                        Text(
                            text = msg.message,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            fontSize = 16.sp,
                            color = if (isAdmin) Color.Black else if (msg.isFailed) Color.Red else Color.Black
                        )
                    }
                }
            }
        }

        // 🕒 Status & Timestamp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        ) {
            if (msg.isSending) {
                CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.dp, color = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text("Sending...", fontSize = 10.sp, color = Color.Gray)
            } else if (msg.isFailed) {
                 Icon(Icons.Default.Refresh, "Retry", tint = Color.Red, modifier = Modifier.size(12.dp).clickable { onRetry(msg) })
                 Spacer(Modifier.width(4.dp))
                 Text("Failed. Tap to retry", fontSize = 10.sp, color = Color.Red, modifier = Modifier.clickable { onRetry(msg) })
            } else if (!msg.createdAt.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 📖 Show 'Read' status for Customer's messages if partner (Admin) viewed them
                    if (!isAdmin && msg.isRead) {
                        Text(
                            text = "อ่านแล้ว",
                            fontSize = 10.sp,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = formatIsoDate(msg.createdAt),
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}

// 🕒 Helper to format date nicely (Shared Logic)
private fun formatIsoDate(isoString: String?): String {
    if (isoString.isNullOrEmpty()) return ""
    return try {
        val raw = if (isoString.length >= 19) isoString.substring(0, 19) else isoString
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        inputFormat.timeZone = java.util.TimeZone.getDefault()
        val date = inputFormat.parse(raw) ?: return isoString
        val outputFormat = java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale("th", "TH"))
        outputFormat.timeZone = java.util.TimeZone.getDefault()
        outputFormat.format(date)
    } catch (e: Exception) {
        isoString.takeLast(5)
    }
}

@Composable
fun ImageViewerDialog(imageUrl: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            val resolvedUrl = ImageUrlResolver.resolveNamingChat(imageUrl)
            SubcomposeAsyncImage(
                model = resolvedUrl,
                contentDescription = "Zoomed Image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit,
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                },
                error = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.White)
                    }
                }
            )

            // Close button at top right
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Add, // Using Add as a cross placeholder if no close icon, but Icons.Default.Close is better
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp).graphicsLayer(rotationZ = 45f)
                )
            }
        }
    }
}

// 🆕 Helper to fix rotationZ if needed, actually graphicsLayer is standard

private fun uriToMultipart(context: Context, uri: android.net.Uri): MultipartBody.Part? {
    return try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val type = contentResolver.getType(uri) ?: "image/jpeg"
        val fileBytes = inputStream.readBytes()
        inputStream.close()
        
        val requestBody = fileBytes.toRequestBody(type.toMediaTypeOrNull())
        MultipartBody.Part.createFormData("file", "image.jpg", requestBody)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
