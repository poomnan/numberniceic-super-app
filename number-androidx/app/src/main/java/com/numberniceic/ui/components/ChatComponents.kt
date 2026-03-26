package com.numberniceic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.numberniceic.R

/**
 * A reusable Gold Shimmering Chat Icon with a premium light sweep effect.
 */
@Composable
fun GoldShimmerChatIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val shimmerTransition = rememberInfiniteTransition(label = "GoldShimmer")
    val transitionValue by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerValue"
    )

    val painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_chat_bubble_gold_1)

    // Using Box + drawWithContent to avoid MaterialTheme dependency for legacy views
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(size)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                // 1. Draw the base chat bubble (Gold)
                with(painter) {
                    draw(size = this@drawWithContent.size)
                }

                // 2. Apply the premium light sweep gradient
                val brush = Brush.linearGradient(
                    0.0f to Color.Transparent,
                    0.3f to Color.Transparent,
                    0.5f to Color.White.copy(alpha = 0.65f),
                    0.7f to Color.Transparent,
                    1.0f to Color.Transparent,
                    start = Offset(this.size.width * transitionValue, 0f),
                    end = Offset(this.size.width * (transitionValue + 0.65f), this.size.height)
                )
                // Use SrcAtop to keep the gold background and only composite the shimmer on top of literal pixels
                drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
            }
    )
}


