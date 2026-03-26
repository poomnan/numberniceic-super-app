package com.numberniceic.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.JsonObject
import com.numberniceic.data.chat.ChatMessage
import com.numberniceic.https.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse
import com.numberniceic.ui.theme.Sarabun

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMonitorScreen(navController: NavController) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    var inputMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // 🚀 Sync activeSessionId with selectedSessionId for notification suppression
    LaunchedEffect(selectedSessionId) {
        com.numberniceic.utils.ChatNotificationManager.activeSessionId = selectedSessionId
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                // Clear when leaving the screen
                com.numberniceic.utils.ChatNotificationManager.activeSessionId = null
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                com.numberniceic.utils.ChatNotificationManager.activeSessionId = selectedSessionId
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            com.numberniceic.utils.ChatNotificationManager.activeSessionId = null
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Handle back button - clear focus (close keyboard) then close activity
    BackHandler {
        focusManager.clearFocus()
        scope.launch {
            delay(100) // รอให้ keyboard ปิดเสร็จ
            (context as? android.app.Activity)?.finish()
        }
    }
    
    // Polling Logic
    LaunchedEffect(Unit) {
        // 1. Initial Load (History)
        try {
            val historyRes = RetrofitClient.api.getAdminRecentMessages(limit = 50).awaitResponse()
             if (historyRes.isSuccessful && historyRes.body() != null) {
                 val hist = historyRes.body()!!
                 messages = hist.sortedBy { it.messageId }
                 
                 // 🎯 Auto-scroll to latest message after loading
                 if (messages.isNotEmpty()) {
                     scope.launch {
                         delay(300) // รอให้ render เสร็จ
                         listState.scrollToItem(messages.size - 1)
                     }
                 }
             }
        } catch (e: Exception) {
            // log error
        }

        // 2. Poll Loop
        while (true) {
            try {
                // Poll all recent messages (Console view)
                val response = RetrofitClient.api.adminPollMessages().awaitResponse()
                if (response.isSuccessful && response.body() != null) {
                    val fetched = response.body()!!
                    
                    if (fetched.isNotEmpty()) {
                        // Upsert Logic: Update existing, Append new
                        val currentMap = messages.associateBy { it.messageId }.toMutableMap()
                        val hadNewMessages = fetched.any { !currentMap.containsKey(it.messageId) }
                        
                        fetched.forEach { msg ->
                            currentMap[msg.messageId] = msg
                        }
                        messages = currentMap.values.sortedBy { it.messageId }
                        
                        // 🎯 Auto-scroll if new message appears
                        if (hadNewMessages && messages.isNotEmpty()) {
                            scope.launch {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore poll errors
            }
            delay(2000)
        }
    }

    // Map session_id to user_name for better UX
    val sessionNames = remember(messages) {
        messages.filter { it.senderType == "customer" }
            .associate { it.sessionId to (it.senderName ?: "Guest") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Live Console Monitor", color = Color.White)
                        if (selectedSessionId != null) {
                            val name = sessionNames[selectedSessionId] ?: selectedSessionId
                            Text("Replying to: $name", color = Color.Yellow, fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        focusManager.clearFocus()
                        // ใช้ Activity.finish() เพื่อปิดหน้าจอนี้
                        (context as? android.app.Activity)?.finish()
                    }) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) 
                    }
                },
                actions = {
                   // Refresh button
                   IconButton(onClick = { 
                       scope.launch {
                           val historyRes = RetrofitClient.api.getAdminRecentMessages(limit = 100).awaitResponse()
                           if (historyRes.isSuccessful && historyRes.body() != null) {
                               messages = historyRes.body()!!.sortedBy { it.messageId }
                           }
                       }
                   }) {
                       Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                   }
                   
                   // ปุ่มยกเลิกการเลือก session
                   if (selectedSessionId != null) {
                       IconButton(onClick = { selectedSessionId = null }) {
                           Icon(Icons.Default.Close, "Clear Selection", tint = Color.White)
                       }
                   }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp, 
                modifier = Modifier
                    .imePadding()
                    .navigationBarsPadding() // 🛡️ Ensure input is above system nav bar
            ) {
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { 
                            val name = if (selectedSessionId != null) (sessionNames[selectedSessionId] ?: selectedSessionId) else "a message"
                            Text(if (selectedSessionId == null) "Select to reply..." else "Reply to $name...") 
                        },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    Button(
                        onClick = {
                            if (inputMessage.isNotBlank() && selectedSessionId != null) {
                                val textToSend = inputMessage
                                inputMessage = ""
                                
                                // 1. Optimistic Add (Temp Message)
                                val tempId = -System.currentTimeMillis() 
                                val tempMsg = ChatMessage(
                                    messageId = tempId,
                                    sessionId = selectedSessionId!!,
                                    senderType = "admin",
                                    senderName = "คุณนิน",
                                    message = textToSend,
                                    isRead = true,
                                    createdAt = org.joda.time.DateTime.now().toString(),
                                    isSending = true,
                                    isFailed = false
                                )
                                messages = messages + tempMsg
                                scope.launch { listState.animateScrollToItem(messages.size - 1) }

                                // 2. Send Network Request
                                sendMessage(selectedSessionId!!, textToSend, context) { sentMsg ->
                                    val currentList = messages.toMutableList()
                                    val idx = currentList.indexOfFirst { it.messageId == tempId }
                                    
                                    if (sentMsg != null) {
                                        // 🛡️ Check collision: Poller might have fetched it
                                        val alreadyExists = messages.any { it.messageId == sentMsg.messageId }
                                        
                                        if (idx != -1) {
                                            if (!alreadyExists) {
                                                currentList[idx] = sentMsg
                                            } else {
                                                currentList.removeAt(idx) // Poller got it, remove temp
                                            }
                                        } else {
                                            if (!alreadyExists) currentList.add(sentMsg)
                                        }
                                        messages = currentList
                                    } else {
                                        // Failed
                                        if (idx != -1) {
                                            currentList[idx] = tempMsg.copy(isFailed = true, isSending = false)
                                            messages = currentList
                                        }
                                    }
                                }
                            } else if (selectedSessionId == null) {
                                Toast.makeText(context, "Select a session first!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = inputMessage.isNotBlank() && selectedSessionId != null, 
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedSessionId != null) Color(0xFF1B5E20) else Color.Gray)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ไม่มีรายการแชท", color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { 
                        scope.launch {
                             val res = RetrofitClient.api.getAdminRecentMessages(limit = 100).awaitResponse()
                             if (res.isSuccessful && res.body() != null) {
                                 messages = res.body()!!.sortedBy { it.messageId }
                             }
                        }
                    }) {
                        Text("รีเฟรช")
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages, key = { it.messageId }) { msg ->
                        val targetName = sessionNames[msg.sessionId] ?: "..."
                        
                        SwipeToDismissBoxWrapper(
                            msg = msg,
                            onDelete = {
                                 deleteMessageFromServer(msg.messageId, context) { success ->
                                     if (success) {
                                         messages = messages.filter { it.messageId != msg.messageId }
                                     }
                                 }
                            }
                        ) {
                            ConsoleLogItem(
                                msg = msg, 
                                sessionName = targetName,
                                isSelected = msg.sessionId == selectedSessionId,
                                onClickSession = { sid, _ -> selectedSessionId = sid },
                                onRetry = { retryMsg ->
                                     // Retry logic
                                     val idxRetry = messages.indexOf(retryMsg)
                                     if (idxRetry != -1) {
                                         val list = messages.toMutableList()
                                         list[idxRetry] = retryMsg.copy(isFailed = false, isSending = true)
                                         messages = list
                                     }
        
                                     sendMessage(retryMsg.sessionId, retryMsg.message, context) { sent ->
                                         val currentList = messages.toMutableList()
                                         val idx = currentList.indexOfFirst { it.messageId == retryMsg.messageId }
                                         
                                         if (sent != null) {
                                              val alreadyExists = currentList.any { it.messageId == sent.messageId }
                                              if (idx != -1) {
                                                  if (!alreadyExists) {
                                                      currentList[idx] = sent
                                                  } else {
                                                      currentList.removeAt(idx)
                                                  }
                                              }
                                              messages = currentList
                                         } else {
                                             if (idx != -1) {
                                                 currentList[idx] = retryMsg.copy(isFailed = true, isSending = false)
                                                 messages = currentList
                                             }
                                         }
                                     }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissBoxWrapper(
    msg: ChatMessage,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
        content = {
            Box(modifier = Modifier.background(Color.White)) { 
                content() 
            }
        }
    )
}

@Composable
fun ConsoleLogItem(
    msg: ChatMessage, 
    sessionName: String,
    isSelected: Boolean,
    onClickSession: (String, String) -> Unit, 
    onRetry: (ChatMessage) -> Unit
) {
    val isAdmin = msg.senderType == "admin"
    val isCustomer = !isAdmin
    
    // Row Background (Selection Highlight)
    val rowBackgroundColor = if (isSelected) Color(0xFFFFFDE7) else Color.Transparent
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackgroundColor)
            .clickable { 
                // Determine display name for the session target
                val displayName = if (isAdmin) sessionName else (msg.senderName ?: "Guest")
                onClickSession(msg.sessionId, displayName) 
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isAdmin) Alignment.End else Alignment.Start
        ) {
            // 👤 Sender Name & Session Info (Tiny text above bubble)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                if (isAdmin) {
                    Text(
                        text = "To: $sessionName", // Show who we talked to (sessionName already has "คุณ" prefix)
                        fontSize = 10.sp, 
                        color = Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "คุณนิน", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = Color(0xFF2E7D32)
                    )
                } else {
                    Text(
                        text = msg.senderName ?: "Guest", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = getUserColor(msg.senderName ?: "")
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "(${msg.sessionId.take(6)}...)", 
                        fontSize = 10.sp, 
                        color = Color.Gray
                    )
                }
            }

            // 💬 The Chat Bubble
            Surface(
                color = if (isAdmin) Color(0xFFC8E6C9) else Color.White, // Green vs White
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isCustomer) 2.dp else 16.dp,
                    bottomEnd = if (isAdmin) 2.dp else 16.dp
                ),
                shadowElevation = 1.dp,
                border = if (isCustomer) BorderStroke(0.5.dp, Color(0xFFE0E0E0)) else null
            ) {
                Column {
                     Text(
                        text = msg.message,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                }
            }
            
            // 🕒 Timestamp & Status (Below Bubble)
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (msg.isSending) {
                    Text("Sending...", fontSize = 10.sp, color = Color.Gray)
                } else if (msg.isFailed) {
                     Icon(Icons.Default.Refresh, "Retry", tint = Color.Red, modifier = Modifier.size(12.dp).clickable { onRetry(msg) })
                     Spacer(Modifier.width(2.dp))
                     Text("Failed", fontSize = 10.sp, color = Color.Red, modifier = Modifier.clickable { onRetry(msg) })
                } else {
                    Text(
                        text = formatIsoDate(msg.createdAt),
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

fun sendMessage(sessionId: String, text: String, context: android.content.Context, onFinalize: (ChatMessage?) -> Unit) {
    val json = JsonObject().apply {
        addProperty("session_id", sessionId)
        addProperty("sender", "admin")
        addProperty("message", text)
        addProperty("name", "คุณนิน")
    }
    RetrofitClient.api.sendChatMessage(json).enqueue(object : Callback<ChatMessage> {
        override fun onResponse(call: Call<ChatMessage>, response: Response<ChatMessage>) {
            if (response.isSuccessful) {
                onFinalize(response.body())
            } else {
                Toast.makeText(context, "Send Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                onFinalize(null)
            }
        }
        override fun onFailure(call: Call<ChatMessage>, t: Throwable) {
            Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            onFinalize(null)
        }
    })
}

fun deleteMessageFromServer(messageId: Long, context: android.content.Context, onResult: (Boolean) -> Unit) {
    RetrofitClient.api.deleteMessage(messageId).enqueue(object : Callback<JsonObject> {
        override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
            if (response.isSuccessful) {
                onResult(true)
            } else {
                Toast.makeText(context, "Delete failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        }
        override fun onFailure(call: Call<JsonObject>, t: Throwable) {
            Toast.makeText(context, "Delete Error: ${t.message}", Toast.LENGTH_SHORT).show()
            onResult(false)
        }
    })
}

// 🕒 Helper to format date nicely
// Input: 2026-02-04T02:22:49.142024Z
// Output: 4 ก.พ. 09:22
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
        isoString 
    }
}

// 🎨 Helper to generate consistent color from string
fun getUserColor(name: String): Color {
    val colors = listOf(
        Color(0xFFE67E22), // Orange
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFF673AB7), // Deep Purple
        Color(0xFF3F51B5), // Indigo
        Color(0xFF2196F3), // Blue
        Color(0xFF00BCD4), // Cyan
        Color(0xFF009688), // Teal
        Color(0xFF4CAF50), // Green
        Color(0xFF8BC34A), // Light Green
        Color(0xFFCDDC39), // Lime
        Color(0xFFFFC107), // Amber
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF795548), // Brown
        Color(0xFF607D8B)  // Blue Grey
    )
    val hash = kotlin.math.abs(name.hashCode())
    return colors[hash % colors.size]
}
