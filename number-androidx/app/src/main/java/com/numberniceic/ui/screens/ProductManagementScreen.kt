package com.numberniceic.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import coil.compose.SubcomposeAsyncImage
import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.numberniceic.data.product.Product
import com.numberniceic.data.product.ProductCategory
import com.numberniceic.https.NetworkConfig
import com.numberniceic.https.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.awaitResponse
import com.google.gson.JsonObject
import com.numberniceic.ui.theme.Kanit
import com.numberniceic.utils.ImageUrlResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf(listOf<Product>()) }
    var categories by remember { mutableStateOf(listOf<ProductCategory>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    fun loadData() {
        scope.launch {
            isLoading = true
            try {
                val catRes = RetrofitClient.api.getProductCategories().awaitResponse()
                if (catRes.isSuccessful) categories = catRes.body() ?: emptyList()

                val prodRes = RetrofitClient.api.getProducts().awaitResponse()
                if (prodRes.isSuccessful) products = prodRes.body() ?: emptyList()
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
        if (showAddDialog) {
            showAddDialog = false
            editingProduct = null
        } else if (!navController.popBackStack()) {
            (context as? android.app.Activity)?.finish()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("จัดการสินค้า", color = Color.White) },
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
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
            }
        ) { padding ->
            val fixedTabs = listOf("เพทาย", "ฝันพยากรณ์")
            
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    contentColor = Color(0xFF1B5E20),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF1B5E20)
                        )
                    }
                ) {
                    fixedTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    text = title,
                                    fontSize = 13.sp, // Small font to fit in one row
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    fontFamily = Kanit
                                ) 
                            }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        val filteredProducts = products.filter { product ->
                            when (selectedTabIndex) {
                                0 -> product.categoryId == 1 // เพทาย
                                1 -> product.categoryId == 2 // ฝันพยากรณ์
                                else -> true
                            }
                        }

                        if (filteredProducts.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("ไม่มีข้อมูลสินค้า")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredProducts) { product ->
                                    ProductItemCard(
                                        product = product,
                                        onEdit = { 
                                            editingProduct = it
                                            showAddDialog = true 
                                        },
                                        onDelete = { productToDelete = it },
                                        onShip = { p ->
                                            scope.launch {
                                                val body = JsonObject().apply {
                                                    addProperty("product_id", p.id)
                                                    addProperty("status", "shipped")
                                                }
                                                val res = RetrofitClient.api.updateShippingStatus(body).awaitResponse()
                                                if (res.isSuccessful) {
                                                    Toast.makeText(context, "บันทึกการจัดส่งเรียบร้อย", Toast.LENGTH_SHORT).show()
                                                    loadData() // Refresh
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

        if (showAddDialog) {
            ProductEditOverlay(
                product = editingProduct,
                categories = categories,
                onDismiss = { showAddDialog = false; editingProduct = null },
                onSave = { savedProduct ->
                    scope.launch {
                        try {
                            val res = if (editingProduct == null) {
                                RetrofitClient.api.addProduct(savedProduct).awaitResponse()
                            } else {
                                RetrofitClient.api.updateProduct(savedProduct).awaitResponse()
                            }
                            
                            if (res.isSuccessful) {
                                Toast.makeText(context, "บันทึกสำเร็จ", Toast.LENGTH_SHORT).show()
                                loadData()
                                showAddDialog = false
                                editingProduct = null
                            } else {
                                val errorMsg = res.errorBody()?.string() ?: res.message()
                                Toast.makeText(context, "Error ${res.code()}: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Exception: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("ยืนยันการลบ") },
            text = { Text("คุณต้องการลบสินค้า '${productToDelete?.name}' ใช่หรือไม่?") },
            confirmButton = {
                Button(
                    onClick = {
                        val product = productToDelete!!
                        productToDelete = null
                        scope.launch {
                            val res = RetrofitClient.api.deleteProduct(product.id).awaitResponse()
                            if (res.isSuccessful) {
                                products = products.filter { it.id != product.id }
                                Toast.makeText(context, "ลบสำเร็จ", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("ลบ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

@Composable
fun ProductItemCard(product: Product, onEdit: (Product) -> Unit, onDelete: (Product) -> Unit, onShip: (Product) -> Unit) {
    val imageUrl = if (product.imageUrl.isNullOrBlank()) {
        "https://via.placeholder.com/150"
    } else {
        ImageUrlResolver.resolve(product.imageUrl)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = "Product Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { 
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.Gray
                        ) 
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color.Red.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onSuccess = { Log.d("ImageLoad", "✅ Success: $imageUrl") },
                    onError = { state -> 
                        Log.e("ImageLoad", "❌ Failed: $imageUrl | Error: ${state.result.throwable.message}")
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val formattedPrice = java.text.DecimalFormat("#,###").format(product.price)
                Text(text = "ราคา: $formattedPrice บาท", color = Color.Black, fontSize = 14.sp)
                Text(text = "หมวดหมู่: ${product.categoryName ?: "N/A"}", color = Color.Gray, fontSize = 12.sp)
            }
            
            if (product.shippingStatus == "ready") {
                Button(
                    onClick = { onShip(product) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).padding(end = 4.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("พร้อมส่ง", fontSize = 10.sp, color = Color.White)
                }
            } else if (product.shippingStatus == "shipped") {
                Text("ส่งแล้ว", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
            }

            IconButton(onClick = { onEdit(product) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Blue)
            }
            IconButton(onClick = { onDelete(product) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditOverlay(
    product: Product?,
    categories: List<ProductCategory>,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember(product) { mutableStateOf(product?.name ?: "") }
    var priceStr by remember(product) { mutableStateOf(product?.price?.toString() ?: "") }
    var description by remember(product) { mutableStateOf(product?.description ?: "") }
    var categoryId by remember(product) { mutableIntStateOf(product?.categoryId ?: 0) }
    var imageUrl by remember(product) { mutableStateOf(product?.imageUrl ?: "") }

    LaunchedEffect(categories, product) {
        if (categoryId == 0 && categories.isNotEmpty()) {
            categoryId = product?.categoryId ?: categories.first().id
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(if (product == null) "เพิ่มสินค้า" else "แก้ไขสินค้า", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextField(modifier = Modifier.fillMaxWidth(), value = name, onValueChange = { name = it }, label = { Text("ชื่อสินค้า") })
                TextField(modifier = Modifier.fillMaxWidth(), value = priceStr, onValueChange = { priceStr = it }, label = { Text("ราคา") })
                TextField(modifier = Modifier.fillMaxWidth(), value = description, onValueChange = { description = it }, label = { Text("รายละเอียด") })
                
                Text("ตัวอย่างรูปภาพ:", fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    val previewUrl = if (imageUrl.isBlank()) "https://via.placeholder.com/150"
                                    else ImageUrlResolver.resolve(imageUrl)
                    
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL รูปภาพ") },
                    supportingText = { Text("ต้องเป็นลิงก์ตรงที่ลงท้ายด้วย .jpg, .png", color = Color.Gray) }
                )
                
                Text("เลือกหมวดหมู่:", fontWeight = FontWeight.Bold)
                if (categories.isEmpty()) {
                    Text("กำลังโหลดหมวดหมู่...", color = Color.Gray, fontSize = 12.sp)
                } else {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = categoryId == cat.id,
                                onClick = { categoryId = cat.id },
                                label = { Text(cat.name) },
                                leadingIcon = if (categoryId == cat.id) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                                } else null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val price = priceStr.toDoubleOrNull() ?: 0.0
                        onSave(Product(
                            id = product?.id ?: 0,
                            categoryId = categoryId,
                            name = name,
                            description = description,
                            price = price,
                            imageUrl = imageUrl
                        ))
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    enabled = name.isNotBlank() && priceStr.isNotBlank() && categoryId != 0
                ) {
                    Text("บันทึกข้อมูล", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
