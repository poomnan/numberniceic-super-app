package com.numberniceic.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete

import androidx.compose.ui.res.painterResource
import com.numberniceic.R
import androidx.compose.material3.*
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.numberniceic.https.RetrofitClient
import com.numberniceic.data.chat.ChatMessage
import com.numberniceic.data.chat.AdminChatStateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.awaitResponse
import com.google.gson.JsonObject
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import com.numberniceic.ui.components.VvipBadgeComponent
import com.numberniceic.ui.theme.NumberniceTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminChatStateManager.init(this)
        setContent {
            NumberniceTheme {
                AdminChatListScreen(
                    onBack = { finish() },
                    onChatSelected = { sessionId, displayName ->
                        val intent = Intent(this, AdminChatRoomActivity::class.java)
                        intent.putExtra("session_id", sessionId)
                        intent.putExtra("display_name", displayName)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatListScreen(onBack: () -> Unit, onChatSelected: (String, String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 🚀 INITIAL LOAD: Retrieve cached sessions, filtering out hidden ones
    val initialCached = remember { 
        AdminChatStateManager.getSessionsCache(context).filter { !AdminChatStateManager.isHidden(it.sessionId, it.messageId) } 
    }
    var sessions by remember { mutableStateOf(initialCached.associateBy { it.sessionId }) } 
    var sessionNames by remember { mutableStateOf(mapOf<String, String>()) } 
    var isLoadingHistory by remember { mutableStateOf(sessions.isEmpty()) } 
    var loadError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // 🚀 We don't need a separate reactiveSessions derived state anymore 
    // because AdminChatStateManager.readMessageMap is now a SnapshotStateMap.
    // Compose will automatically re-render any item that reads from it when it changes.

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                AdminChatStateManager.refreshSignal++ 
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        // Init state manager before anything else
        AdminChatStateManager.init(context)

        var retryCount = 0
        while (retryCount < 3) {
            try {
                loadError = null
                val historyRes = RetrofitClient.api.getAdminRecentMessages(limit = 100).awaitResponse()
                if (historyRes.isSuccessful) {
                    val freshSessions = historyRes.body()?.filter { !AdminChatStateManager.isHidden(it.sessionId, it.messageId) } ?: emptyList()
                    sessions = freshSessions.associateBy { it.sessionId }
                    
                    val names = mutableMapOf<String, String>()
                    freshSessions.forEach { msg ->
                        if (msg.senderType == "customer") {
                            val name = msg.senderName
                            val isGuest = !msg.sessionId.startsWith("u")
                            // Fix: Treat any name starting with 'g' as a raw ID for guests
                            val isGeneratedName = name.isNullOrBlank() || name.startsWith("g", ignoreCase = true)
                            
                            names[msg.sessionId] = if (isGuest && isGeneratedName) {
                                 val shortId = if (msg.sessionId.length > 4) msg.sessionId.takeLast(4) else msg.sessionId
                                 "Guest-$shortId"
                            } else if (!isGuest && isGeneratedName) {
                                 "คุณMember-${msg.sessionId.substring(1)}"
                            } else {
                                 name ?: "Unknown"
                            }
                        }
                    }
                    sessionNames = names
                    AdminChatStateManager.saveSessionsCache(context, freshSessions)
                    break // Success!
                } else {
                    val errCode = historyRes.code()
                    android.util.Log.e("AdminChat", "History Load Failed: $errCode")
                    loadError = "Server Error: $errCode"
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminChat", "History Load Error: ${e.message}")
                loadError = "Network Error: ${e.message}"
            }
            retryCount++
            if (retryCount < 3) delay(2000)
        }
        isLoadingHistory = false

        var pollCounter = 0
        while (true) {
            try {
                val res = if (pollCounter % 2 == 0) {
                    RetrofitClient.api.getAdminRecentMessages(limit = 100).awaitResponse()
                } else {
                    RetrofitClient.api.adminPollMessages().awaitResponse()
                }
                pollCounter++

                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    // 1. Baseline map: We'll filter *after* merging to ensure new messages unhide sessions
                    val sessionList = res.body() ?: emptyList()
                    val newSessions = sessions.toMutableMap()
                    val names = sessionNames.toMutableMap()
                    
                    sessionList.forEach { msg ->
                        // Only process if it's the latest message for this session
                        val existing = newSessions[msg.sessionId]
                        if (existing == null || msg.messageId >= existing.messageId) {
                            newSessions[msg.sessionId] = msg
                        }
                        
                        // 🚀 Always ensure we have a display name (regardless of who sent the latest message)
                        val rawName = msg.senderName
                        val isGuest = !msg.sessionId.startsWith("u")

                        // Derive the actual customer name from either senderName or sessionId
                        val resolvedName: String? = when {
                            // If sender is customer, use their senderName directly
                            msg.senderType == "customer" -> rawName
                            // If sender is admin (reply), try to extract customer name from reply format: "คุณนิน [ตอบกลับ NAME]"
                            msg.senderType == "admin" && !rawName.isNullOrBlank() && rawName.contains("[ตอบกลับ") -> {
                                rawName.substringAfter("[ตอบกลับ ").substringBefore("]").trim()
                                    .takeIf { it.isNotEmpty() && !it.startsWith("Guest") }
                            }
                            // Otherwise try the cached name we already have
                            else -> names[msg.sessionId]
                        }

                        val isGeneratedName = resolvedName.isNullOrBlank() ||
                            resolvedName.startsWith("g", ignoreCase = true) ||
                            resolvedName.startsWith("Guest (ID:", ignoreCase = true) ||
                            resolvedName.startsWith("Member ")

                        val displayName = if (isGuest) {
                            if (isGeneratedName) {
                                val shortId = if (msg.sessionId.length > 4) msg.sessionId.takeLast(4) else msg.sessionId
                                "Guest-$shortId"
                            } else {
                                resolvedName ?: run {
                                    val shortId = if (msg.sessionId.length > 4) msg.sessionId.takeLast(4) else msg.sessionId
                                    "Guest-$shortId"
                                }
                            }
                        } else {
                            if (isGeneratedName) {
                                "คุณMember-${msg.sessionId.substring(1)}"
                            } else {
                                resolvedName ?: "คุณMember-${msg.sessionId.substring(1)}"
                            }
                        }
                        names[msg.sessionId] = displayName
                    }
                    // 🎶 Play Sound if new customer messages arrive
                    val hasNewCustomerMsg = sessionList.any { msg ->
                        msg.senderType == "customer" && 
                        (sessions[msg.sessionId]?.let { msg.messageId > it.messageId } ?: true)
                    }
                    if (hasNewCustomerMsg) {
                        com.numberniceic.utils.SoundManager.playChatSound(context)
                    }

                    // Filter based on the new message-aware isHidden
                    val filteredSessions = newSessions.filterValues { !AdminChatStateManager.isHidden(it.sessionId, it.messageId) }
                    sessions = filteredSessions
                    sessionNames = names
                    AdminChatStateManager.saveSessionsCache(context, filteredSessions.values.toList())
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminChat", "Poll Loop Error: ${e.message}")
            }
            delay(3000)
        }
    }

    // Dialog State
    var showDeleteDialog by remember { mutableStateOf(false) }

    var sessionToDelete by remember { mutableStateOf<ChatMessage?>(null) }



    if (showDeleteDialog && sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false 
                sessionToDelete = null
            },
            title = { Text("ยืนยันการลบแชท") },
            text = { Text("คุณต้องการลบแชทนี้ออกจากรายการใช่หรือไม่?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionToDelete?.let { msg ->
                            // 🚀 Server-side deletion
                            RetrofitClient.api.deleteChatSession(msg.sessionId).enqueue(object : Callback<JsonObject> {
                                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                                    if (response.isSuccessful) {
                                        android.widget.Toast.makeText(context, "ลบแชทเรียบร้อยแล้ว", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                                override fun onFailure(call: Call<JsonObject>, t: Throwable) {}
                            })

                            // Locally hide/remove
                            AdminChatStateManager.hideSession(context, msg.sessionId, msg.messageId)
                            val newSessions = sessions.toMutableMap()
                            newSessions.remove(msg.sessionId)
                            sessions = newSessions
                        }
                        showDeleteDialog = false
                        sessionToDelete = null
                    }
                ) {
                    Text("ลบ", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        sessionToDelete = null
                    }
                ) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            primary = Color(0xFF4CAF50)
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "รายการแชท (Admin Chat)", 
                            color = Color.White, 
                            fontSize = 16.sp, 
                            maxLines = 1, 
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFF121212))) {
                if (isLoadingHistory && sessions.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF4CAF50))
                } else if (loadError != null && sessions.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("เกิดข้อผิดพลาดในการโหลดข้อมูล", color = Color.LightGray)
                        Text(loadError ?: "", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                AdminChatStateManager.refreshSignal++ 
                                (context as? ComponentActivity)?.let {
                                    val intent = it.intent
                                    it.finish()
                                    it.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("ลองใหม่")
                        }
                    }
                } else if (sessions.isEmpty()) {
                    Text("ไม่มีรายการแชท", modifier = Modifier.align(Alignment.Center), color = Color.LightGray)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = sessions.values.toList().sortedByDescending { it.createdAt }, 
                            key = { it.sessionId } 
                        ) { msg ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        sessionToDelete = msg
                                        showDeleteDialog = true
                                        false
                                    } else {
                                        false
                                    }
                                }
                            )
                            
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Red)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White
                                        )
                                    }
                                },
                                content = {
                                    val displayName = sessionNames[msg.sessionId] ?: "Session: ${msg.sessionId.take(8)}..."
                                    val isActuallyRead = AdminChatStateManager.isMessageRead(msg.sessionId, msg.messageId, msg.isRead)
                                    
                                    // Determine user status badge
                                    val isGuest = !msg.sessionId.startsWith("u")
                                    val vipRaw = msg.vipStatus ?: "normal"
                                    val vip = vipRaw.lowercase().trim()
                                    val userBadge = when {
                                        isGuest -> "GE"
                                        vip == "vvip" -> "VVIP"
                                        vip == "mvp" -> "MVP"
                                        vip.contains("vip") || vip == "gold" || vip == "silver" || vip == "diamond" || vip == "admin" -> "VIP"
                                        vip == "normal" -> "MB"
                                        else -> "GE"
                                    }
                                    
                                    val badgeColor = when(userBadge) {
                                        "GE" -> Color(0xFFFF9800)  // Orange for Guest
                                        "MB" -> Color(0xFF2196F3)  // Blue for Member
                                        else -> Color.Gray
                                    }

                                    ListItem(
                                        colors = ListItemDefaults.colors(containerColor = Color(0xFF1E1E1E)),
                                        leadingContent = {
                                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                                when (userBadge) {
                                                    "VVIP" -> VvipBadgeComponent(size = 48)
                                                    "VIP" -> androidx.compose.foundation.Image(
                                                        painter = painterResource(id = R.drawable.ic_vip02),
                                                        contentDescription = "VIP",
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                    "MVP" -> androidx.compose.foundation.Image(
                                                        painter = painterResource(id = R.drawable.icon_gold01),
                                                        contentDescription = "MVP",
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                    else -> Surface(
                                                        shape = androidx.compose.foundation.shape.CircleShape,
                                                        color = badgeColor.copy(alpha = 0.2f),
                                                        modifier = Modifier.size(48.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = userBadge,
                                                                color = badgeColor,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        headlineContent = { 
                                            Text(
                                                displayName, 
                                                fontWeight = FontWeight.Bold, 
                                                color = Color.White
                                            ) 
                                        },
                                        supportingContent = { 
                                            val displayMsg = if (msg.message.isNullOrBlank() && !msg.imageUrl.isNullOrBlank()) {
                                                "[รูปภาพ]"
                                            } else {
                                                msg.message
                                            }
                                            Text(
                                                text = displayMsg ?: "", 
                                                maxLines = 1, 
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, 
                                                color = Color.LightGray
                                            )
                                        },
                                        trailingContent = { 
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(formatTime(msg.createdAt ?: ""), fontSize = 11.sp, color = Color.Gray)
                                                if (!isActuallyRead && msg.senderType == "customer") {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(top = 4.dp)
                                                            .size(8.dp)
                                                            .background(Color(0xFFFF5252), androidx.compose.foundation.shape.CircleShape)
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.clickable { 
                                            AdminChatStateManager.markAsReadLocally(context, msg.sessionId, msg.messageId)
                                            onChatSelected(msg.sessionId, displayName) 
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp), 
                                        color = Color(0xFF2C2C2C)
                                    )
                                }
                            )
                        }
                    }
                }
            }
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
        isoString.take(16).replace("T", " ")
    }
}
