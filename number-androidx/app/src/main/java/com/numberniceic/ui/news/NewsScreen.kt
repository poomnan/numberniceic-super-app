package com.numberniceic.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.numberniceic.data.news.NewsHeadline

@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
    onNewsClick: (String) -> Unit
) {
    val newsCollection by viewModel.newsCollection.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(initial = false)

    val errorMessage by viewModel.errorMessage.observeAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Light Gray BG
    ) {
        // News List
        val newsList = newsCollection?.newsAll ?: emptyList()
        
        Column {
            // DEBUG INFO (Temporary)
            Text(
                text = "Status: ${if (isLoading) "Loading" else "Idle"} | Count: ${newsList.size}",
                modifier = Modifier.padding(8.dp),
                color = Color.Red,
                fontSize = 12.sp
            )
            if (errorMessage != null) {
                Text(text = "Error: $errorMessage", color = Color.Red, modifier = Modifier.padding(8.dp))
            }
            
            if (newsList.isEmpty() && !isLoading) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     Text("No News Available", fontSize = 20.sp, color = Color.Gray)
                 }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(newsList, key = { it.newsId ?: it.hashCode() }) { newsItem ->
                    NewsItemRow(newsItem = newsItem, onClick = {
                        onNewsClick(newsItem.newsId ?: "")
                    })
                }
            }
        }

        // Loading Indicator (Centered)
        if (isLoading && newsList.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun NewsItemRow(
    newsItem: NewsHeadline,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // News Image - TEMPORARILY DISABLED for debugging
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Gray)) {
                 Text("Image Placeholder", modifier = Modifier.align(Alignment.Center), color = Color.White)
            }
            /*
            if (!newsItem.newsImg.isNullOrEmpty()) {
                GlideImage(
                    model = newsItem.newsImg,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            } */

            // Text Content
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = newsItem.newsHeader ?: "No Header",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = newsItem.newsDesc ?: "No Description",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
