package com.numberniceic.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.numberniceic.data.product.ProductCategory
import com.numberniceic.https.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf(listOf<ProductCategory>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var categoryToDelete by remember { mutableStateOf<ProductCategory?>(null) }

    fun loadData() {
        scope.launch {
            isLoading = true
            try {
                val res = RetrofitClient.api.getProductCategories().awaitResponse()
                if (res.isSuccessful) {
                    categories = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    // Handle system back button
    BackHandler {
        if (!navController.popBackStack()) {
            (context as? android.app.Activity)?.finish()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("จัดการประเภทสินค้า", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (!navController.popBackStack()) {
                            (context as? android.app.Activity)?.finish()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF1B5E20),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (categories.isEmpty()) {
                Text("ไม่มีข้อมูลประเภทสินค้า", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { category ->
                        CategoryItemCard(
                            category = category,
                            onEdit = { 
                                editingCategory = it
                                showAddDialog = true 
                            },
                            onDelete = { categoryToDelete = it }
                        )
                    }
                }
            }
        }
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("ยืนยันการลบ") },
            text = { Text("คุณต้องการลบหมวดหมู่ '${categoryToDelete?.name}' ใช่หรือไม่? (สินค้าในหมวดหมู่นี้อาจได้รับผลกระทบ)") },
            confirmButton = {
                Button(
                    onClick = {
                        val category = categoryToDelete!!
                        categoryToDelete = null
                        scope.launch {
                            try {
                                val res = RetrofitClient.api.deleteCategory(category.id).awaitResponse()
                                if (res.isSuccessful) {
                                    Toast.makeText(context, "ลบสำเร็จ", Toast.LENGTH_SHORT).show()
                                    loadData()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "ล้มเหลว: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("ลบ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    if (showAddDialog) {
        CategoryEditDialog(
            category = editingCategory,
            onDismiss = { 
                showAddDialog = false
                editingCategory = null 
            },
            onSave = { name, description ->
                scope.launch {
                    try {
                        val category = ProductCategory(
                            id = editingCategory?.id ?: 0,
                            name = name,
                            description = description
                        )
                        val res = if (editingCategory == null) {
                            RetrofitClient.api.addCategory(category).awaitResponse()
                        } else {
                            RetrofitClient.api.updateCategory(category).awaitResponse()
                        }

                        if (res.isSuccessful) {
                            Toast.makeText(context, "บันทึกสำเร็จ", Toast.LENGTH_SHORT).show()
                            loadData()
                            showAddDialog = false
                            editingCategory = null
                        } else {
                            Toast.makeText(context, "ผิดพลาด: ${res.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun CategoryItemCard(
    category: ProductCategory, 
    onEdit: (ProductCategory) -> Unit, 
    onDelete: (ProductCategory) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp,
                    color = Color.Black
                )
                if (!category.description.isNullOrBlank()) {
                    Text(
                        text = category.description, 
                        color = Color.Gray, 
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            IconButton(onClick = { onEdit(category) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF1B5E20))
            }
            IconButton(onClick = { onDelete(category) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun CategoryEditDialog(
    category: ProductCategory?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var description by remember { mutableStateOf(category?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "เพิ่มประเภทสินค้า" else "แก้ไขประเภทสินค้า") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ชื่อประเภทสินค้า") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("รายละเอียด") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, description) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                Text("บันทึก")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก")
            }
        }
    )
}
