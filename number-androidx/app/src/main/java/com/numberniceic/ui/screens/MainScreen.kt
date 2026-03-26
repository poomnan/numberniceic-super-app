package com.numberniceic.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.numberniceic.R
import com.numberniceic.ui.apersonnews.DashboardBottomSheet
import com.numberniceic.ui.auth.LoginBottomSheet
import com.numberniceic.ui.auth.MemberBottomSheet
import com.numberniceic.ui.auth.UserRegisAct
import com.numberniceic.BuildConfig
import com.numberniceic.https.RetrofitClient
import com.numberniceic.utils.UserContextManager
import androidx.fragment.app.FragmentActivity
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.awaitResponse
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.draw.drawWithContent
import com.numberniceic.utils.showSingle
import com.numberniceic.ui.components.VvipBadgeComponent

// Define Fonts - matching XML fonts
val kanitFont = FontFamily(Font(R.font.kanit_light, FontWeight.Normal))
val drawerFont = FontFamily(Font(R.font.browa_0, FontWeight.Normal))

sealed class Screen(val route: String, val title: String, val iconRes: Int) {
    object Home : Screen("home", "HOME", R.drawable.ic_world_white)
    object Phone : Screen("phone", "เบอร์โทร", R.drawable.mobile_phone)
    object Tabian : Screen("tabian", "ทะเบียน", R.drawable.car)
    object NameNick : Screen("name_nick", "ชื่อเล่น", R.drawable.icon_children)
    object NameSur : Screen("name_sur", "ชื่อสกุล", R.drawable.icon_family)
    object HomeNum : Screen("home_num", "บ้านเลขที่", R.drawable.ico_house)
    object Chat : Screen("chat", "Chat", R.drawable.article)
    object Admin : Screen("admin", "Console", R.drawable.ic_vip02)
    object AdminMonitor : Screen("admin_monitor", "Chat Monitor", R.drawable.ic_chat_bubble_animated)
    object AdminProducts : Screen("admin_products", "Manage Products", R.drawable.ic_shop_white)
    object AdminCategories : Screen("admin_categories", "Manage Categories", R.drawable.ic_shop_white)
    object Cart : Screen("cart", "ตะกร้าสินค้า", R.drawable.ic_cart_white)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val user = remember { com.numberniceic.utils.UserContextManager.userX(context) }
    val isAdmin = com.numberniceic.utils.UserContextManager.isAdmin(user)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val tabs = listOf(Screen.Home, Screen.Phone, Screen.Tabian, Screen.NameNick, Screen.NameSur, Screen.HomeNum)

    // 🛡️ Anti-Double Click / Debounce Navigation
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val safeNavigate: (String, (androidx.navigation.NavOptionsBuilder.() -> Unit)?) -> Unit = { route, builder ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > 500) { // 500ms cooldown
            lastClickTime = currentTime
            if (currentRoute != route) {
                if (builder != null) navController.navigate(route, builder)
                else navController.navigate(route)
            }
        }
    }

    // Global Chat State for Floating Preview
    var unreadCount by remember { mutableIntStateOf(com.numberniceic.utils.ChatNotificationManager.getUnreadCount(context)) }
    var lastIncomingMessage by remember { mutableStateOf<String?>(null) }
    var showMessagePreview by remember { mutableStateOf(false) }

    // Global Polling for Notifications
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
        
        var sId = prefs.getString("session_id", "")
        var maxSeenId = com.numberniceic.utils.ChatNotificationManager.getLastReadMessageId(context)
        
        while (true) {
            // Observer role only: sync with centralized manager
            val globalCount = com.numberniceic.utils.ChatNotificationManager.getUnreadCount(context)
            if (unreadCount != globalCount) unreadCount = globalCount
            delay(3000)
        }
    }

    // Colors matching app_main.xml styles
    val primaryGreen = Color(0xFF457E1E)
    val tabSelectedColor = Color(0xFFD3FFB2)
    val tabUnselectedColor = Color.White
    val indicatorGold = Color(0xFFFFD600)

    // VIP Pulsating Animation
    val infiniteTransition = rememberInfiniteTransition(label = "vip_shimmer")
    val vipScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val vipAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = Color.White
            ) {
                val user = UserContextManager.userX(context)
                // Strictly use the same logic as UserLogoutF.kt
                val isAdmin = UserContextManager.isAdmin(user)

                // Drawer content matching nav_header.xml exactly
                DrawerContent(
                    isAdmin = isAdmin,
                    onRegisterClick = {
                        scope.launch { drawerState.close() }
                        context.startActivity(Intent(context, UserRegisAct::class.java))
                    },
                    onAdminClick = {
                        scope.launch { drawerState.close() }
                        safeNavigate(Screen.Admin.route, null)
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        // TopAppBar matching toolbar_main
                        TopAppBar(
                            title = { Text("NUMBER", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, softWrap = false) },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = null)
                                }
                            },
                            actions = {
                                // Notification icon
                                IconButton(onClick = { }) { 
                                    Icon(Icons.Default.Notifications, contentDescription = null) 
                                }

                                // Chat icon with badge
                                IconButton(onClick = { 
                                    if (isAdmin) {
                                        context.startActivity(Intent(context, com.numberniceic.ui.admin.AdminChatActivity::class.java))
                                    } else {
                                        safeNavigate(Screen.Chat.route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                }) { 
                                    Box(modifier = Modifier.size(28.dp)) {
                                        com.numberniceic.ui.components.GoldShimmerChatIcon(modifier = Modifier.fillMaxSize())
                                        
                                        if (unreadCount > 0) {

                                            // ... badge logic ...
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 6.dp, y = (-6).dp)
                                                    .size(16.dp)
                                                    .background(Color.Red, androidx.compose.foundation.shape.CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    if (unreadCount > 9) "9+" else unreadCount.toString(),
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Sell Product (Shopping Cart) Icon
                                IconButton(onClick = { 
                                    safeNavigate(Screen.Cart.route, null)
                                }) { 
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_cart_white), 
                                        contentDescription = "เบอร์มงคลสำหรับขาย",
                                        modifier = Modifier.size(28.dp),
                                        tint = Color.White
                                    ) 
                                }

                                // VIP icon - Middle Button (ic_vip02 with pulsating effect)
                                IconButton(onClick = { 
                                    val activity = context as? FragmentActivity
                                    val user = UserContextManager.userX(context)
                                    if (user != null) {
                                        DashboardBottomSheet().showSingle(activity?.supportFragmentManager!!, "DashboardBottomSheet")
                                    } else {
                                        LoginBottomSheet().showSingle(activity?.supportFragmentManager!!, "LoginBottomSheet")
                                    }
                                }) { 
                                    // 🏅 ปรับคืนให้ใช้ VvipBadgeComponent ที่สวยงามเหมือนเดิม
                                    val vipcode = user?.vipcode?.lowercase()?.trim() ?: ""
                                    if (vipcode == "vvip") {
                                        MaterialTheme {
                                            VvipBadgeComponent(size = 36)
                                        }
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_vip02), 
                                            contentDescription = "VIP Dashboard",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .graphicsLayer(
                                                    scaleX = vipScale,
                                                    scaleY = vipScale,
                                                    alpha = vipAlpha
                                                ),
                                            tint = Color.Unspecified
                                        ) 
                                    }
                                }
                                // User icon - Right Button (icon_member)
                                IconButton(onClick = { 
                                    val activity = context as? FragmentActivity
                                    val user = UserContextManager.userX(context)
                                    if (user != null) {
                                        MemberBottomSheet().showSingle(activity?.supportFragmentManager!!, "MemberBottomSheet")
                                    } else {
                                        LoginBottomSheet().showSingle(activity?.supportFragmentManager!!, "LoginBottomSheet")
                                    }
                                }) { 
                                    Icon(
                                        painter = painterResource(id = R.drawable.icon_member), 
                                        contentDescription = "User Profile",
                                        modifier = Modifier.size(28.dp),
                                        tint = Color.Unspecified
                                    ) 
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = primaryGreen,
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White,
                                actionIconContentColor = Color.White
                            )
                        )

                        // TabRow matching tab_layout with tabMode="scrollable" - all tabs accessible
                        ScrollableTabRow(
                            selectedTabIndex = tabs.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0,
                            containerColor = primaryGreen,
                            contentColor = Color.White,
                            edgePadding = 0.dp,
                            indicator = { tabPositions ->
                                val index = tabs.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                                    color = indicatorGold
                                )
                            }
                        ) {
                            tabs.forEachIndexed { index, screen ->
                                val selected = currentRoute == screen.route
                                Tab(
                                    selected = selected,
                                    onClick = {
                                        safeNavigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        // Tab icon
                                        Icon(
                                            painter = painterResource(id = screen.iconRes), 
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = if (selected) tabSelectedColor else tabUnselectedColor.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Tab title matching MyCustomTabTextAppearance (slightly increased)
                                        Text(
                                            screen.title, 
                                            fontSize = 15.sp,
                                            fontFamily = kanitFont,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) tabSelectedColor else tabUnselectedColor.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Visible,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Surface(modifier = Modifier.padding(innerPadding).fillMaxSize(), color = Color(0xFFF5F5F5)) {
                    NavHost(navController = navController, startDestination = Screen.Home.route) {
                        composable(Screen.Home.route) { HomeScreen(navController) }
                        composable(Screen.Phone.route) { PhoneHomeScreen(navController) }
                        composable(Screen.Tabian.route) { LicensePlateScreen(navController) }
                        composable(Screen.NameNick.route) { NameNickScreen(navController) }
                        composable(Screen.NameSur.route) { NameSurScreen(navController) }
                        composable(Screen.HomeNum.route) { HomeNumScreen(navController) }
                        composable(Screen.Chat.route) { ChatScreen(onBack = { navController.popBackStack() }) }
                        composable(Screen.Admin.route) { AdminMenuScreen(navController) }
                        composable(Screen.AdminMonitor.route) { AdminMonitorScreen(navController) }
                        composable(Screen.AdminProducts.route) { ProductManagementScreen(navController) }
                        composable(Screen.AdminCategories.route) { CategoryManagementScreen(navController) }
                        composable(Screen.Cart.route) { CartScreen(navController = navController, onBack = { navController.popBackStack() }) }
                    }
                }
            }

            // Global Floating Chat Bubble - Now outside Scaffold to be on top of TopBar and everything
            // Global Floating Chat Bubble - Now outside Scaffold to be on top of TopBar and everything
            /* 
            if (currentRoute == null || (currentRoute != Screen.Chat.route && currentRoute != Screen.Admin.route)) {
                FloatingChatBubble(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 16.dp).zIndex(999f),
                    unreadCount = unreadCount,
                    previewMessage = if (showMessagePreview) lastIncomingMessage else null,
                    onClick = {
                        unreadCount = 0
                        showMessagePreview = false
                        safeNavigate(Screen.Chat.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
            */
        }
    }
}

@Composable
fun FloatingChatBubble(
    modifier: Modifier = Modifier, 
    unreadCount: Int = 0,
    previewMessage: String? = null,
    onClick: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Use fillMaxSize container with very high Z-index to stay on top
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(9999f), // Force top priority
        contentAlignment = Alignment.TopEnd // Initial position changed to Top-Right
    ) {
        Box(
            modifier = Modifier
                .padding(top = 280.dp, end = 20.dp) // Aligns with the red box in the user's image
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .wrapContentSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            // Message Preview (Fades in/out)
            androidx.compose.animation.AnimatedVisibility(
                visible = !previewMessage.isNullOrEmpty(),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(expandFrom = Alignment.End),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally(shrinkTowards = Alignment.End),
                modifier = Modifier.padding(end = 70.dp)
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.widthIn(max = 200.dp)
                ) {
                    Text(
                        text = previewMessage ?: "",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Black
                    )
                }
            }

            // The Circle Bubble
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer(shadowElevation = 20f, shape = androidx.compose.foundation.shape.CircleShape)
                    .background(Color(0xFF457E1E), androidx.compose.foundation.shape.CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.admin_nin_avatar),
                    contentDescription = "Chat",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                
                // Notification badge
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(22.dp)
                            .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (unreadCount > 9) "9+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Drawer content matching nav_header.xml exactly
 */
@Composable
private fun DrawerContent(isAdmin: Boolean, onRegisterClick: () -> Unit, onAdminClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFF05581E)) // Main background color
    ) {
        // Logo Header - matching RelativeLayout with logo_app_header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF05581E))
                .padding(horizontal = 65.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_app_header),
                contentDescription = "App Logo",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }
        

        
        // --- Admin Console Access (For Testing/Admins) ---
        if (isAdmin) {
            Button(
                onClick = {
                    onAdminClick() 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Admin Console Monitor", color = Color.White)
            }
        }

        // Register Button + Slogan Row - matching LinearLayout #154215
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF154215))
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onRegisterClick,
                modifier = Modifier.padding(start = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF056409))
            ) {
                Text("ลงทะเบียน", fontSize = 11.sp, fontFamily = drawerFont)
            }
            Text(
                text = "v${BuildConfig.VERSION_NAME} เพราะชีวิตมีค่า จงเลือกให้ดีที่สุด",
                color = Color(0xFFFDD835),
                fontSize = 12.sp,
                fontFamily = drawerFont,
                modifier = Modifier.padding(start = 9.dp)
            )
        }
        
        // Section 1: ทำนายเบอร์โทรศัพท์ - #7CB342
        DrawerSection(
            headerColor = Color(0xFF7CB342),
            title = "ทำนายเบอร์โทรศัพท์",
            content1 = "การต่อรองทางธุรกิจ ครอบครัว ความรัก คุณมักใช้โทรศัพท์เบอร์นี้ในการสื่อสารใช่หรือไม่? เจ้าเบอร์นี้แหละ มันติดตัวคุณเป็นเงา ที่สนิทยิ่งกว่าแฟน",
            content2 = "ยิ่งใช้ ยิ่งใกล้ชิด ยิ่งสนิท ยิ่งมีอิทธิพล จะดีหรือร้าย ลองพิสูจน์คำทำนายและแก้ไขด้วยตัวคุณเอง"
        )
        
        // Section 2: ทำนายชื่อเล่น ชื่อสกุล - #C0CA33
        DrawerSection(
            headerColor = Color(0xFFC0CA33),
            title = "ทำนายชื่อเล่น ชื่อสกุล",
            content1 = "ชื่อเล่นมันบ่งบอกว่าเป็นคุณ ตัวบุคคลและและการทำธุรกรรมต่างๆต้องมีชื่อเรียกขานต้องลงนามด้วยชื่อสกุล ท่านทราบไหมว่า ทุกการใช้ทุกการเรียกขาน มันหมายถึงการเรียกเลขกรรมเลขบุญประจำตัวของคุณให้แสดงผลเสมอซ้ำๆ",
            content2 = "ลองพิสูจน์คำทำนายด้วยตัวคุณเอง จากพลังเลขศาสตร์ พลังเงา และเลขเรียง จากชื่อของคุณ"
        )
        
        // Section 3: ทำนายทะเบียนรถ - #AD039BE5 (with alpha)
        DrawerSection(
            headerColor = Color(0xAD039BE5),
            title = "ทำนายทะเบียนรถ",
            content1 = "มีเงินซื้อรถราคาเป็นล้าน ประมูลทะเบียนรถราคาเป็นแสนเป็นล้าน แล้วท่านจะซื้อชีวิตและครอบครัวราคาเท่าไหร่ดี ? ชีวิตไม่มีขาย แต่คุณเลือกได้",
            content2 = "เลขทะเบียนรถที่โชว์เด่นหราเปรียบเหมือนบัตรประชาชนของรถทุกคัน เขาพาครอบครัวคุณผจญไปทุกที่บนถนน คุณเชื่อใจเขาได้แค่ไหน ??"
        )
        
        // Section 4: ทำนายบ้านเลขที่ - Header #DB81F3, background #C8FB8C00
        DrawerSection(
            headerColor = Color(0xFFDB81F3),
            title = "ทำนายบ้านเลขที่",
            content1 = "บ้านเลขที่ที่ใครมาหา หรือผ่านไปมาต่างก็มองเห็นแม้มันจะเป็นเพียงแค่ตัวเลขธรรมดา แต่ถ้ามันมีอิทธิพลต่อชีวิตของคุณละ ถ้ามันมีส่วนกำหนดชะตาชีวิตคุณให้ดีหรือร้าย และมันอาจเป็นภัยที่บ่อนทำลายชีวิตคุณและครอบคร้วได้ทั้งอนาคตและปัจจุบันละ",
            content2 = "หลายคนลงทุนซื้อบ้านซื้อคอนโดราคาหลายล้าน ตกแต่งอีกเป็นหมื่นเป็นแสน บ้านที่คุณต้องกลับและหลับนอนทุกวัน ครึ่งนึงของชีวิตทุกคนคือบ้าน จงเลือกสิ่งที่ดีที่สุดที่คุณเลือกได้"
        )
    }
}

/**
 * Drawer section matching each LinearLayout section in nav_header.xml
 */
@Composable
private fun DrawerSection(
    headerColor: Color,
    title: String,
    content1: String,
    content2: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header with colored background - 18sp
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontFamily = drawerFont,
            modifier = Modifier
                .fillMaxWidth()
                .background(headerColor)
                .padding(horizontal = 9.dp, vertical = 4.dp)
        )
        // Content 1 - black text on white background
        Text(
            text = content1,
            color = Color.Black,
            fontSize = 14.sp,
            fontFamily = drawerFont,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 9.dp, end = 9.dp, top = 4.dp)
        )
        // Content 2 - black text on white background with padding
        Text(
            text = content2,
            color = Color.Black,
            fontSize = 14.sp,
            fontFamily = drawerFont,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(9.dp)
        )
    }
}
