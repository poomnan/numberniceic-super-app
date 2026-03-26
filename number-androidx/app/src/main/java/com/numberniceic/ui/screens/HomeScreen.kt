package com.numberniceic.ui.screens

import android.content.Intent
import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.numberniceic.R
import com.numberniceic.data.news.News24
import com.numberniceic.data.news.NewsHeadline
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.ui.news.NewsAct
import com.numberniceic.ui.news.NewsAllAct
import com.numberniceic.utils.ImageUrlResolver
import com.numberniceic.utils.PersonNewsCacheManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Fonts matching XML
private val kanitLightFont = FontFamily(Font(R.font.kanit_light, FontWeight.Normal))
private val sarabunFont = FontFamily(Font(R.font.sarabun_regular, FontWeight.Normal))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    var newsData by remember { mutableStateOf<News24?>(null) }
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Derive articles based on selection
    val articles = remember(newsData, selectedCategoryIndex) {
        when (selectedCategoryIndex) {
            0 -> newsData?.newsHot
            1 -> newsData?.newsFeedback
            2 -> newsData?.newsPhonenum
            3 -> newsData?.newsNameSur
            4 -> newsData?.newsTabian
            5 -> newsData?.newsHome
            6 -> newsData?.newsConcept
            else -> newsData?.newsHot
        } ?: emptyList()
    }

    // Load data on first launch
    LaunchedEffect(Unit) {
        // 1. Try Load from Cache
        val cached = PersonNewsCacheManager.loadNews24(context)
        if (cached != null) {
            newsData = cached
        }

        // 2. Fetch Network
        fetchArticles(context) { fetched ->
            if (fetched != null) {
                newsData = fetched
                // Save Cache
                PersonNewsCacheManager.saveNews24(context, fetched)
            }
        }
    }

    fun onRefresh() {
        isRefreshing = true
        fetchArticles(context) { response ->
            isRefreshing = false
            if (response != null) {
                newsData = response
                PersonNewsCacheManager.saveNews24(context, response)
                Toast.makeText(context, "รีเฟรชบทความสำเร็จ", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "ไม่สามารถรีเฟรชบทความได้", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openArticle(newsId: String) {
        val intent = Intent(context, NewsAct::class.java)
        intent.putExtra("newsId", newsId)
        context.startActivity(intent)
    }

    fun openAllArticles() {
        val intent = Intent(context, NewsAllAct::class.java)
        // Adjust newsIdType based on category
        val type = when(selectedCategoryIndex) {
            1 -> "1" // feedback
            2 -> "2" // phone
            3 -> "3" // namesur
            4 -> "4" // tabian
            5 -> "5" // home
            6 -> "6" // concept
            else -> "0" // hot
        }
        intent.putExtra("newsIdType", type)
        context.startActivity(intent)
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onRefresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. TOP Navigation Tabs
            CategoryTabs(
                selectedTabIndex = selectedCategoryIndex,
                onTabSelected = { selectedCategoryIndex = it }
            )

            if (selectedCategoryIndex == 0) {
                // --- DASHBOARD MODE (Show All Groups) ---

                // GROUP 1: ข่าวบทความที่น่าสนใจ (Hot News - Type 0)
                if (newsData?.newsHot?.isNotEmpty() == true) {
                    NewsHeader(selectedIndex = 0) // Yellow Header
                    val hot = newsData?.newsHot!!
                    FeaturedArticle(article = hot[0], onClick = { openArticle(hot[0].newsId ?: "") })
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (hot.size > 1) GridItem(article = hot[1], onClick = { openArticle(hot[1].newsId ?: "") })
                            if (hot.size > 3) GridItem(article = hot[3], onClick = { openArticle(hot[3].newsId ?: "") })
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            if (hot.size > 2) GridItem(article = hot[2], onClick = { openArticle(hot[2].newsId ?: "") })
                            ResourceBlock(articles = hot, onItemClick = { openArticle(it) }, onSeeAllClick = { openAllArticles() })
                        }
                    }
                }

                // GROUP 2: รีวิวลูกค้า (Type 1)
                val feedback = newsData?.newsFeedback
                if (!feedback.isNullOrEmpty()) {
                    NewsSection(
                        title = feedback.firstOrNull()?.category ?: "Reviews และ Feedback จากประสบการณ์ลูกค้า",
                        index = 1,
                        articles = feedback,
                        onItemClick = { openArticle(it) },
                        onSeeAllClick = { selectedCategoryIndex = 1 }
                    )
                }

                // GROUP 3: เบอร์โทรศัพท์ (Type 2)
                val phone = newsData?.newsPhonenum
                if (!phone.isNullOrEmpty()) {
                    NewsSection(
                        title = phone.firstOrNull()?.category ?: "ความรู้และบทความเกี่ยวกับเบอร์โทรศัพท์",
                        index = 2,
                        articles = phone,
                        onItemClick = { openArticle(it) },
                        onSeeAllClick = { selectedCategoryIndex = 2 }
                    )
                }

                // GROUP 4: ชื่อ-นามสกุล (Type 3)
                val nameSur = newsData?.newsNameSur
                if (!nameSur.isNullOrEmpty()) {
                    NewsSection(
                        title = nameSur.firstOrNull()?.category ?: "ชื่อและนามสกุลเสริมดวงชะตา",
                        index = 3,
                        articles = nameSur,
                        onItemClick = { openArticle(it) },
                        onSeeAllClick = { selectedCategoryIndex = 3 }
                    )
                }

                // GROUP 5: ทะเบียนรถ (Type 4)
                val tabian = newsData?.newsTabian
                if (!tabian.isNullOrEmpty()) {
                    NewsSection(
                        title = tabian.firstOrNull()?.category ?: "ศาสตร์ตัวเลขและทะเบียนรถมงคล",
                        index = 4,
                        articles = tabian,
                        onItemClick = { openArticle(it) },
                        onSeeAllClick = { selectedCategoryIndex = 4 }
                    )
                }

                // GROUP 6: บ้านเลขที่ (Type 5)
                val home = newsData?.newsHome
                if (!home.isNullOrEmpty()) {
                    NewsSection(
                        title = home.firstOrNull()?.category ?: "ทำนายดวงชะตาจากบ้านเลขที่",
                        index = 5,
                        articles = home,
                        onItemClick = { openArticle(it) },
                        onSeeAllClick = { selectedCategoryIndex = 5 }
                    )
                }

                // GROUP 7: หลักการใช้เลขมงคล (Type 6)
                val concept = newsData?.newsConcept
                if (!concept.isNullOrEmpty()) {
                    NewsSection(
                        title = concept.firstOrNull()?.category ?: "หลักการใช้และการเลือกเลขมงคลที่ถูกต้อง",
                        index = 6,
                        articles = concept,
                        onItemClick = { openArticle(it) },
                        onSeeAllClick = { selectedCategoryIndex = 6 }
                    )
                }

            } else {
                // --- CATEGORY MODE (Show Only Selected Category) ---
                val dynamicTitle = articles.firstOrNull()?.category
                NewsHeader(selectedIndex = selectedCategoryIndex, titleOverride = dynamicTitle)
                
                if (articles.isNotEmpty()) {
                    FeaturedArticle(article = articles[0], onClick = { openArticle(articles[0].newsId ?: "") })
                    
                    articles.drop(1).chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    GridItem(article = item, onClick = { openArticle(item.newsId ?: "") })
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("ไม่พบข้อมูลในหมวดนี้", fontFamily = kanitLightFont, color = Color.Gray)
                    }
                }
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun NewsHeader(selectedIndex: Int, titleOverride: String? = null) {
    val headerData = when(selectedIndex) {
        0 -> Pair(Color(0xFFFFD600), "ข่าวและบทความที่น่าสนใจ")
        1 -> Pair(Color(0xFF59C514), "Review จากลูกค้า") // Fallback
        2 -> Pair(Color(0xFF59C514), "วิธีเลือกซื้อเบอร์โทรศัพท์มงคล") // Fallback
        3 -> Pair(Color(0xFF59C514), "ทำนายดวงชะตาจากชื่อ-สกุล") // Fallback
        4 -> Pair(Color(0xFF59C514), "ทำนายดวงชะตาจากทะเบียนรถ") // Fallback
        5 -> Pair(Color(0xFF59C514), "ทำนายดวงชะตาจากบ้านเลขที่") // Fallback
        6 -> Pair(Color(0xFF59C514), "หลักการเลือกทะเบียนรถ") // Fallback
        else -> Pair(Color(0xFFFFD600), "ข่าวและบทความที่น่าสนใจ")
    }

    // Use dynamic title if available, otherwise use mapped title
    val displayTitle = if (!titleOverride.isNullOrEmpty()) titleOverride else headerData.second

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerData.first)
            .padding(vertical = 4.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.newspaper),
            contentDescription = null,
            modifier = Modifier.size(35.dp),
            colorFilter = if (selectedIndex != 0) ColorFilter.tint(Color.White) else null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = displayTitle,
            fontSize = 18.sp,
            color = if (selectedIndex == 0) Color(0xFF2F3A10) else Color.White,
            fontFamily = kanitLightFont
        )
    }
}

@Composable
private fun CategoryTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val categories = listOf(
        Pair("ทั้งหมด", R.drawable.newspaper),
        Pair("รีวิวลูกค้า", R.drawable.approval),
        Pair("เบอร์โทร", R.drawable.smartphone),
        Pair("ชื่อ-สกุล", R.drawable.icon_family),
        Pair("ทะเบียน", R.drawable.car),
        Pair("บ้านเลขที่", R.drawable.ico_house),
        Pair("ความรู้", R.drawable.information)
    )
    val selectedColor = Color(0xFFD3FFB2) // Light green
    val unselectedColor = Color.White.copy(alpha = 0.7f)
    val indicatorGold = Color(0xFFFFD600)
    
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = Color(0xFF457E1E), // primaryGreen
        contentColor = Color.White,
        edgePadding = 16.dp,
        divider = {},
        indicator = { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = indicatorGold
                )
            }
        }
    ) {
        categories.forEachIndexed { index, pair ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = pair.second),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selectedTabIndex == index) selectedColor else unselectedColor
                        )
                        Text(
                            text = pair.first,
                            fontFamily = kanitLightFont,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) selectedColor else unselectedColor
                        )
                    }
                },
                selectedContentColor = selectedColor,
                unselectedContentColor = unselectedColor
            )
        }
    }
}

@Composable
private fun NewsSection(
    title: String,
    index: Int,
    articles: List<NewsHeadline>,
    onItemClick: (String) -> Unit,
    onSeeAllClick: () -> Unit
) {
    if (articles.isEmpty()) return
    
    Column(modifier = Modifier.fillMaxWidth()) {
        NewsHeaderSection(title = title, index = index, onSeeAllClick = onSeeAllClick)
        
        // Show up to 4 items in rows of 2
        articles.take(4).chunked(2).forEach { rowArticles ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                rowArticles.forEach { article ->
                    Box(modifier = Modifier.weight(1f).padding(4.dp)) {
                        GridItemSmall(article = article, onClick = { onItemClick(article.newsId ?: "") })
                    }
                }
                // Fill space if row is not full
                if (rowArticles.size < 2) {
                     Spacer(modifier = Modifier.weight(1f).padding(4.dp))
                }
            }
        }
    }
}

@Composable
private fun NewsHeaderSection(title: String, index: Int, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF59C514))
            .padding(vertical = 6.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_clover),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.White,
                fontFamily = kanitLightFont
            )
        }
        Text(
            text = "ดูทั้งหมด >",
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier.clickable { onSeeAllClick() },
            fontFamily = kanitLightFont
        )
    }
}

@Composable
private fun GridItemSmall(article: NewsHeadline, onClick: () -> Unit) {
    val imageUrl = remember(article.newsImg) { ImageUrlResolver.resolve(article.newsImg) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(android.R.drawable.ic_menu_gallery)
        )
        Text(
            text = article.newsHeader ?: "",
            color = Color.Black,
            fontSize = 14.sp,
            fontFamily = kanitLightFont,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
private fun FeaturedArticle(article: NewsHeadline, onClick: () -> Unit) {
    val imageUrl = remember(article.newsImg) { ImageUrlResolver.resolve(article.newsImg) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Featured Image - 320dp height
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(android.R.drawable.ic_menu_gallery),
            error = painterResource(android.R.drawable.stat_notify_error)
        )

        // Card overlay at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(end = 40.dp, bottom = 12.dp)
                .background(Color(0xE6FFFFFF))
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Category - red text
            Text(
                text = article.category ?: "",
                color = Color(0xFFE53935),
                fontSize = 14.sp,
                fontFamily = kanitLightFont,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Title - single line
            Text(
                text = article.newsHeader ?: "",
                color = Color(0xFF212121),
                fontSize = 20.sp,
                fontFamily = kanitLightFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.offset(y = (-4).dp)
            )

            // Description - 2 lines
            Text(
                text = article.newsDesc ?: "",
                color = Color(0xFF757575),
                fontSize = 14.sp,
                fontFamily = sarabunFont,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GridItem(article: NewsHeadline, onClick: () -> Unit) {
    val imageUrl = remember(article.newsImg) { ImageUrlResolver.resolve(article.newsImg) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable(onClick = onClick)
    ) {
        // Image
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(android.R.drawable.ic_menu_gallery)
        )

        // Shadow gradient at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )

        // Title text at bottom center
        Text(
            text = article.newsTitleShort ?: article.newsHeader ?: "",
            color = Color.White,
            fontSize = 18.sp,
            fontFamily = kanitLightFont,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun ResourceBlock(
    articles: List<NewsHeadline>,
    onItemClick: (String) -> Unit,
    onSeeAllClick: () -> Unit
) {
    // Gradient matching bg_home_resource.xml: #7986CB -> #7E57C2 -> #5E35B1 at 45°
    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF7986CB), Color(0xFF7E57C2), Color(0xFF5E35B1)),
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(gradientBrush)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // List Item 1
        if (articles.size > 4) {
            ListItem(
                text = articles[4].newsTitleShort ?: articles[4].newsHeader ?: "",
                onClick = { onItemClick(articles[4].newsId ?: "") }
            )
        }
        
        // List Item 2
        if (articles.size > 5) {
            ListItem(
                text = articles[5].newsTitleShort ?: articles[5].newsHeader ?: "",
                onClick = { onItemClick(articles[5].newsId ?: "") }
            )
        }
        
        // List Item 3
        if (articles.size > 6) {
            ListItem(
                text = articles[6].newsTitleShort ?: articles[6].newsHeader ?: "",
                onClick = { onItemClick(articles[6].newsId ?: "") }
            )
        }
        
        // See All button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSeeAllClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ดูบทความทั้งหมด",
                color = Color(0xFFFFD600),
                fontSize = 18.sp,
                fontFamily = kanitLightFont
            )
            Spacer(modifier = Modifier.width(12.dp))
            Image(
                painter = painterResource(R.drawable.f_vip),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(Color(0xFFFFD600))
            )
        }
    }
}

@Composable
private fun ListItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Yellow dot - 8dp
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color(0xFFFFD600))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            fontFamily = kanitLightFont,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun fetchArticles(context: android.content.Context, onResult: (News24?) -> Unit) {
    val apiService = RetrofitClient.instance.create(ApiService::class.java)
    apiService.getNewsTopic24().enqueue(object : Callback<News24> {
        override fun onResponse(call: Call<News24>, response: Response<News24>) {
            if (response.isSuccessful && response.body() != null) {
                onResult(response.body())
            } else {
                onResult(null)
            }
        }

        override fun onFailure(call: Call<News24>, t: Throwable) {
            Log.e("HomeScreen", "Network Error: ${t.message}")
            onResult(null)
        }
    })
}
