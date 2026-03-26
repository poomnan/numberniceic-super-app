package com.numberniceic.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuScreen(navController: NavController) {
    val context = LocalContext.current

    val adminItems = listOf(
        AdminMenuItem("แชทมอนิเตอร์", Icons.Default.Email, "admin_monitor"),
        AdminMenuItem("จัดการประเภทสินค้า", Icons.Default.List, "admin_categories"),
        AdminMenuItem("จัดการสินค้า", Icons.Default.ShoppingCart, "admin_products"),
        AdminMenuItem("จัดการความฝัน", Icons.Default.Info, "admin_dreams"),
        AdminMenuItem("จัดการสมาชิก", Icons.Default.Person, "admin_users"),
        AdminMenuItem("จัดการทะเบียน", Icons.Default.Place, "admin_tabian"),
        AdminMenuItem("จัดการบทความ", Icons.Default.Edit, "admin_articles")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Menu", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { (context as? android.app.Activity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(adminItems) { item ->
                    AdminCard(item) {
                        when (item.id) {
                            "admin_monitor" -> navController.navigate("admin_monitor")
                            "admin_categories" -> navController.navigate("admin_categories")
                            "admin_products" -> navController.navigate("admin_products")
                            "admin_dreams" -> context.startActivity(Intent(context, com.numberniceic.ui.admin.AdminDreamActivity::class.java))
                            "admin_users" -> context.startActivity(Intent(context, com.numberniceic.ui.admin.AdminUserActivity::class.java))
                            "admin_tabian" -> context.startActivity(Intent(context, com.numberniceic.ui.admin.AdminTabianActivity::class.java))
                            "admin_articles" -> context.startActivity(Intent(context, com.numberniceic.ui.admin.AdminArticlesAct::class.java))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminCard(item: AdminMenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(40.dp),
                tint = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
        }
    }
}

data class AdminMenuItem(
    val title: String,
    val icon: ImageVector,
    val id: String
)
