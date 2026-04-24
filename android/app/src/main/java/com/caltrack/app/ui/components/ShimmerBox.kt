package com.caltrack.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ShimmerDark = Color(0xFF1A1A2E)
private val ShimmerLight = Color(0xFF2A2A3E)

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(ShimmerDark, ShimmerLight, ShimmerDark),
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
    )
}

@Composable
fun ShimmerMealCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF16161F))
            .padding(16.dp)
    ) {
        // Thumbnail placeholder
        ShimmerBox(
            modifier = Modifier
                .width(56.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
            )
        }
    }
}

@Composable
fun ShimmerProgressCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF16161F))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            // Ring placeholder
            ShimmerBox(
                modifier = Modifier
                    .width(160.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(80.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
            )
        }
    }
}
