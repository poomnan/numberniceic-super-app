package com.numberniceic.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.util.Log
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {
    val context = LocalContext.current

    // Handle system back button
    BackHandler {
        (context as? android.app.Activity)?.finish()
    }

    val adminTools = listOf(
        AdminTool("Live Chat", "Monitor Live Chat", Icons.Default.Notifications, com.numberniceic.ui.admin.AdminMonitorActivity::class.java),
        AdminTool("Manage Products", "Products CRUD", Icons.Default.ShoppingCart, com.numberniceic.ui.admin.AdminProductActivity::class.java),
        AdminTool("Manage Categories", "Product Categories", Icons.Default.List, com.numberniceic.ui.admin.AdminCategoryActivity::class.java),
        AdminTool("Manage Dreams", "Dream Interpretations", Icons.Default.Star, com.numberniceic.ui.admin.AdminDreamActivity::class.java),
        AdminTool("Manage Users", "User Roles & Status", Icons.Default.Person, com.numberniceic.ui.admin.AdminUserActivity::class.java),
        AdminTool("Guest Addresses", "View Guest Shipping Addresses", Icons.Default.LocationOn, com.numberniceic.ui.admin.GuestAddressManagementActivity2::class.java),
        AdminTool("Articles", "Manage News & Tips", Icons.Default.Build, com.numberniceic.ui.admin.AdminArticlesAct::class.java)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ผู้ดูแลระบบ (Admin Dashboard)", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { (context as? android.app.Activity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF5F5F5))) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(adminTools) { tool ->
                    AdminToolCard(tool = tool) {
                        Log.d("AdminDashboard", "Clicked on: ${tool.title}")
                        Toast.makeText(context, "คลิก: ${tool.title}", Toast.LENGTH_SHORT).show()
                        
                        try {
                            context.startActivity(Intent(context, tool.activityClass))
                            Log.d("AdminDashboard", "Started activity: ${tool.activityClass.simpleName}")
                        } catch (e: Exception) {
                            Log.e("AdminDashboard", "Failed to start activity: ${tool.activityClass.simpleName}", e)
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}

data class AdminTool(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val activityClass: Class<*>
)

@Composable
fun AdminToolCard(tool: AdminTool, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.title,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF1B5E20)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tool.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black
            )
            Text(
                text = tool.description,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}
