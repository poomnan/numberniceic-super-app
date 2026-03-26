package com.numberniceic.ui.admin

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.gson.JsonObject
import com.numberniceic.data.chat.AdminChatStateManager
import com.numberniceic.data.chat.ChatMessage
import com.numberniceic.https.NetworkConfig
import com.numberniceic.https.RetrofitClient
import com.numberniceic.utils.UserContextManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.awaitResponse

// Fix Missing Imports

class AdminChatRoomActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminChatStateManager.init(this)
        val sessionId = intent.getStringExtra("session_id") ?: return finish()
        setContent {
            AdminChatRoomScreen(sessionId = sessionId, onBack = { finish() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatRoomScreen(sessionId: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var zoomedImageUrl by remember { mutableStateOf<String?>(null) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                com.numberniceic.utils.ChatNotificationManager.activeSessionId = sessionId
                com.numberniceic.utils.ChatNotificationManager.isChatScreenOpen = true
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                com.numberniceic.utils.ChatNotificationManager.activeSessionId = null
                com.numberniceic.utils.ChatNotificationManager.isChatScreenOpen = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // 🚀 Real-time Update via Broadcast Receiver (Re-implemented safely)
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: android.content.Intent?) {
                val type = intent?.getStringExtra("type")
                val notiSessionId = intent?.getStringExtra("session_id")
                
                // Check if it's a relevant chat message
                if ((type == "chat" || type == "customer_message") && notiSessionId == sessionId) {
                    // 🚀 Sync Logic: Accessing History to get the LATEST data immediately
                    scope.launch {
                        try {
                            val res = RetrofitClient.api.getChatHistory(sessionId, 5).awaitResponse()
                            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                                // Logic handled in polling loop primarily, but here we can force fetch
                                // Actually, let the AdminPoll handle the logic to avoid race conditions.
                                // We just trigger a fast poll cycle? 
                                // Or better: Do the merge here too.
                                val fetched = res.body()!!
                                val currentIds = messages.map { it.messageId }.toSet()
                                val newOnes = fetched.filter { !currentIds.contains(it.messageId) }
                                
                                if (newOnes.isNotEmpty()) {
                                    messages = (messages + newOnes).sortedBy { it.messageId }
                                    AdminChatStateManager.markAsReadLocally(ctx!!, sessionId, newOnes.last().messageId)
                                    // Auto-scroll handled by LaunchedEffect
                                }
                            }
                        } catch (e: Exception) { }
                    }
                }
            }
        }
        
        try {
            val filter = android.content.IntentFilter("com.numberniceic.NEW_NOTIFICATION")
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
             // Ignore registration errors on old devices or weird states
        }

        onDispose {
            try { context.unregisterReceiver(receiver) } catch(e:Exception){}
            com.numberniceic.utils.ChatNotificationManager.activeSessionId = null
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val handleRichContent: (android.net.Uri) -> Unit = { uri ->
        if (sessionId.isNotEmpty()) {
            scope.launch {
                val tempId = -System.currentTimeMillis()
                val tempMsg = ChatMessage(
                    messageId = tempId,
                    sessionId = sessionId,
                    senderType = "admin",
                    senderName = "คุณนิน",
                    message = "",
                    imageUrl = uri.toString(),
                    isRead = false,
                    createdAt = org.joda.time.DateTime.now().toString()
                )
                messages = messages + tempMsg
                // Scroll handled by LaunchedEffect

                try {
                    val part = uriToMultipart(context, uri)
                    if (part == null) {
                        messages = messages.filter { it.messageId != tempId }
                        return@launch
                    }

                    val uploadRes = RetrofitClient.api.uploadChatImage(part).awaitResponse()
                    if (uploadRes.isSuccessful && uploadRes.body() != null) {
                        val serverUrl = uploadRes.body()!!.get("url").asString
                        val fullUrl = NetworkConfig.BASE_URL + if (serverUrl.startsWith("/")) serverUrl else "/$serverUrl"

                        val json = JsonObject().apply {
                            addProperty("session_id", sessionId)
                            addProperty("sender", "admin")
                            addProperty("message", "")
                            addProperty("image_url", fullUrl)
                        }

                        val res = RetrofitClient.api.sendChatMessage(json).awaitResponse()
                        if (res.isSuccessful && res.body() != null) {
                            // 🎶 Play Sound
                            com.numberniceic.utils.SoundManager.playSendSound(context)
                            val realMsg = res.body()!!
                            // 🛡️ Guard: Only replace if poller hasn't already added it
                            if (messages.none { it.messageId == realMsg.messageId }) {
                                messages = messages.map { if (it.messageId == tempId) realMsg else it }
                            } else {
                                messages = messages.filter { it.messageId != tempId }
                            }
                        } else {
                            messages = messages.filter { it.messageId != tempId }
                        }
                    } else {
                        messages = messages.filter { it.messageId != tempId }
                    }
                } catch (e: Exception) {
                    messages = messages.filter { it.messageId != tempId }
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

    LaunchedEffect(Unit) {
        // 1. Mark as Read on entry
        try {
            RetrofitClient.api.markChatAsReadGo(sessionId).awaitResponse()
        } catch (e: Exception) {}

        // 2. Load History
        try {
            AdminChatStateManager.init(context)
            val res = RetrofitClient.api.getChatHistory(sessionId).awaitResponse()
            if (res.isSuccessful) {
                val hist = res.body() ?: emptyList()
                if (hist.isNotEmpty()) {
                    messages = hist
                    // 🚀 Mark last message as read locally
                    AdminChatStateManager.markAsReadLocally(context, sessionId, hist.last().messageId)
                    
                    // With reverseLayout, 0 is bottom.
                    kotlinx.coroutines.delay(100)
                    listState.scrollToItem(0)
                }
            }
        } catch (e: Exception) {}

        // Poll - 🚀 USE ADMIN POLLING (Global Stream) TO AVOID CONFLICT WITH CUSTOMER CONNECTION
        while (true) {
            try {
                // Using adminPollMessages() which subscribes to global admin stream
                val res = RetrofitClient.api.adminPollMessages().awaitResponse()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val allFetched = res.body()!!
                    
                    // 🚀 FILTER: Only care about messages for THIS session
                    val relevantFetched = allFetched.filter { it.sessionId == sessionId }
                    
                    if (relevantFetched.isNotEmpty()) {
                        val currentIds = messages.map { it.messageId }.toSet()
                        val newOnes = relevantFetched.filter { !currentIds.contains(it.messageId) }
                        
                        // 1. Merge Update (Smart Merge)
                        var updatedList = messages.map { oldMsg ->
                            relevantFetched.find { it.messageId == oldMsg.messageId } ?: oldMsg
                        }

                        // 2. 🚀 Read Status Domino Effect (Time-based Check)
                        // If we find any ADMIN message that is read, find the LATEST read time.
                        val maxReadTime = relevantFetched
                            .filter { it.senderType == "admin" && it.isRead }
                            .map { it.createdAt }
                            .maxOrNull() // Lexicographical string compare works for ISO8601

                        if (maxReadTime != null) {
                            updatedList = updatedList.map { msg ->
                                // If it's an admin message, unread, AND created BEFORE or ON the maxReadTime
                                if (msg.senderType == "admin" && !msg.isRead && msg.createdAt <= maxReadTime) {
                                    msg.copy(isRead = true)
                                } else msg
                            }
                        }

                        if (newOnes.isNotEmpty()) {
                            // 🚀 PREVENT DUPLICATES: 
                            // If we have a local "pending" admin message (ID < 0) 
                            // and the server just returned an admin message with the SAME content,
                            // we should consider the pending one "synced" and remove it immediately.
                            val pendingMeMsgs = updatedList.filter { it.messageId < 0 && it.senderType == "admin" }
                            val serverMeMsgs = newOnes.filter { it.senderType == "admin" }
                            
                            val syncedPendingIds = mutableSetOf<Long>()
                            pendingMeMsgs.forEach { pending ->
                                 if (serverMeMsgs.any { it.message == pending.message && it.imageUrl == pending.imageUrl }) {
                                     syncedPendingIds.add(pending.messageId)
                                 }
                            }
                            
                            if (syncedPendingIds.isNotEmpty()) {
                                updatedList = updatedList.filter { !syncedPendingIds.contains(it.messageId) }
                            }

                            updatedList = (updatedList + newOnes).sortedBy { if (it.messageId < 0) -it.messageId else it.messageId }

                            // 🎶 Play Sound if it's a new message from the other party (Customer)
                            if (newOnes.any { it.senderType == "customer" }) {
                                com.numberniceic.utils.SoundManager.playChatSound(context)
                            }

                            // 🚀 Mark NEW messages as read locally
                            AdminChatStateManager.markAsReadLocally(context, sessionId, newOnes.last().messageId)

                            messages = updatedList
                        } else {
                            // Just status update
                            messages = updatedList
                        }
                    }
                }
            } catch (e: Exception) {}
            delay(1000) // Slightly faster poll for Admin
        }
    }

    Scaffold(
        topBar = {
            val customerName = messages.find { it.senderType == "customer" && !it.senderName.isNullOrEmpty() }?.senderName
                ?: messages.find { it.senderType == "admin" && !it.senderName.isNullOrEmpty() && it.sessionId == sessionId }?.senderName // Fallback? 
                ?: sessionId.take(8)
            
            val titleText = if (customerName.startsWith("คุณ")) customerName else "คุณ$customerName"

            TopAppBar(
                title = { Text(titleText, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                // Divider
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))

                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    if (inputText.isEmpty()) {
                                        Text("พิมพ์ข้อความ...", color = Color.Gray)
                                    }
                                    innerTextField()
                                }
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions {
                                if (inputText.isNotBlank()) {
                                    val textToSend = inputText
                                    inputText = ""
                                    scope.launch {
                                        // Optimistic UI
                                        val tempId = -System.currentTimeMillis()
                                        val msg = ChatMessage(
                                            messageId = tempId,
                                            sessionId = sessionId,
                                            senderType = "admin",
                                            senderName = "คุณนิน",
                                            message = textToSend,
                                            imageUrl = "",
                                            isRead = false,
                                            createdAt = org.joda.time.DateTime.now().toString()
                                        )
                                        messages = messages + msg
                                        // Scroll handled by LaunchedEffect

                                        val json = JsonObject().apply {
                                            addProperty("session_id", sessionId)
                                            addProperty("sender", "admin")
                                            addProperty("message", textToSend)
                                        }
                                        try {
                                            val res = RetrofitClient.api.sendChatMessage(json).awaitResponse()
                                            if (res.isSuccessful && res.body() != null) {
                                                // 🎶 Play Sound
                                                com.numberniceic.utils.SoundManager.playSendSound(context)
                                                val newMsg = res.body()!!
                                                // 🛡️ Guard: Only replace if poller hasn't already added it
                                                if (messages.none { it.messageId == newMsg.messageId }) {
                                                    messages = messages.map { if (it.messageId == tempId) newMsg else it }
                                                } else {
                                                    messages = messages.filter { it.messageId != tempId }
                                                }
                                            } else {
                                                messages = messages.filter { it.messageId != tempId }
                                            }
                                        } catch (e: Exception) {
                                            messages = messages.filter { it.messageId != tempId }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Add, contentDescription = "Attach Image", tint = Color.Gray)
                    }
                    IconButton(onClick = {
                        if (inputText.isNotBlank()) {
                            val textToSend = inputText
                            inputText = ""
                            scope.launch {
                                // Optimistic Send
                                val tempId = -System.currentTimeMillis()
                                val msg = ChatMessage(messageId = tempId, sessionId = sessionId, senderType = "admin", senderName = "คุณนิน", senderAvatar = "admin", message = textToSend, imageUrl = "", isRead = false, createdAt = org.joda.time.DateTime.now().toString())
                                messages = messages + msg
                                // Scroll handled by LaunchedEffect

                                val json = JsonObject().apply {
                                    addProperty("session_id", sessionId)
                                    addProperty("sender", "admin")
                                    addProperty("message", textToSend)
                                }
                                try {
                                    val res = RetrofitClient.api.sendChatMessage(json).awaitResponse()
                                    if (res.isSuccessful && res.body() != null) {
                                        // 🎶 Play Sound
                                        com.numberniceic.utils.SoundManager.playSendSound(context)
                                        val newMsg = res.body()!!
                                        // 🛡️ Guard: Only replace if poller hasn't already added it
                                        if (messages.none { it.messageId == newMsg.messageId }) {
                                            messages = messages.map { if (it.messageId == tempId) newMsg else it }
                                        } else {
                                            messages = messages.filter { it.messageId != tempId }
                                        }
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color(0xFF1B5E20))
                    }

                }
            }
        }
    ) { padding ->
        if (zoomedImageUrl != null) {
            AdminImageViewerDialog(imageUrl = zoomedImageUrl!!) { zoomedImageUrl = null }
        }

        // 🚀 Internal Auto-scroll for S8+
        // Use last messageId as key to trigger scroll
        val lastMessageId = remember(messages) { messages.lastOrNull()?.messageId }

        LaunchedEffect(lastMessageId) {
            if (lastMessageId != null) {
                // S8+ / Older devices need more time for layout to settle
                // Try multiple times to ensure it anchors to bottom (index 0 in reverseLayout)
                repeat(3) { i ->
                    delay(if (i == 0) 600L else 400L)
                    try {
                        // With reverseLayout, 0 is bottom
                        listState.scrollToItem(0)
                        delay(100)
                        listState.animateScrollToItem(0)
                    } catch (e: Exception) {}
                }
            }
        }

        if (messages.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
                // Optionally, you can add a message here like "No messages yet"
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFD6E4F0)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true // 🚀 GOD MODE SCROLL: Anchor to bottom
            ) {
                // Feed messages in REVERSE order (Newest -> Oldest)
                items(messages.reversed(), key = { it.messageId }) { msg ->
                    AdminChatBubble(msg, onImageClick = { zoomedImageUrl = it })
                }
            }
        }
    }
}

@Composable
fun AdminChatBubble(msg: ChatMessage, onImageClick: (String) -> Unit) {
    val isAdmin = msg.senderType == "admin" // Me (The Admin holding this phone)
    
    // Invert alignment for Admin vs Customer to match Chat UX: Me = Right, Other = Left
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
        horizontalAlignment = if (isAdmin) Alignment.End else Alignment.Start
    ) {
        // Display Sender Name
        val rawName = msg.senderName ?: ""
        val displayName = if (isAdmin) {
            "คุณนิน"
        } else {
            if (rawName.isEmpty()) {
                "ลูกค้า"
            } else if (rawName.startsWith("คุณ")) {
                rawName
            } else {
                "คุณ$rawName"
            }
        }
        Text(
            text = displayName,
            fontSize = 11.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isAdmin) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {

            Surface(
                color = if (isAdmin) Color(0xFF8DE08D) else Color.White,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isAdmin) 18.dp else 4.dp,
                    bottomEnd = if (isAdmin) 4.dp else 18.dp
                ),
                shadowElevation = 1.dp
            ) {
                Column {
                    if (!msg.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = msg.imageUrl,
                            contentDescription = "Chat Image",
                            modifier = Modifier
                                .padding(4.dp)
                                .sizeIn(maxWidth = 240.dp, maxHeight = 320.dp)
                                .background(Color.LightGray, RoundedCornerShape(12.dp))
                                .clickable { msg.imageUrl?.let { onImageClick(it) } },
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                    if (!msg.message.isNullOrBlank()) {
                        Text(
                            text = msg.message,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            fontSize = 16.sp,
                            color = if (isAdmin) Color.Black else Color.Black
                        )
                    }
                }
            }
        }
        
        // Time & Read Status
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            if (isAdmin && msg.isRead) {
                Text(
                    text = "อ่านแล้ว",
                    fontSize = 10.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp)
                )
            }
            Text(
                text = formatTime(msg.createdAt),
                fontSize = 10.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

private fun formatTime(isoString: String): String {
    return try {
        val raw = if (isoString.length >= 19) isoString.substring(0, 19) else isoString
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        val date = inputFormat.parse(raw) ?: return isoString
        val outputFormat = java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale("th", "TH"))
        outputFormat.format(date)
    } catch (e: Exception) {
        isoString.takeLast(5)
    }
}

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

@Composable
fun AdminImageViewerDialog(imageUrl: String, onDismiss: () -> Unit) {
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
            AsyncImage(
                model = imageUrl,
                contentDescription = "Zoomed Image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Add,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp).graphicsLayer(rotationZ = 45f)
                )
            }
        }
    }
}
