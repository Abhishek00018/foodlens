package com.caltrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caltrack.app.ui.theme.NeonLime
import com.caltrack.app.ui.theme.TextSecondary

data class DayCalorie(
    val day: String,
    val calories: Int
)

@Composable
fun WeeklyChart(
    data: List<DayCalorie>,
    goal: Int = 2000,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(goal, data.maxOfOrNull { it.calories } ?: 0)
    val maxBarHeight = 120.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "This Week",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Daily calorie intake",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { dayData ->
                val fraction = if (maxVal > 0) dayData.calories.toFloat() / maxVal else 0f
                val barHeight = maxBarHeight * fraction
                val isOverGoal = dayData.calories > goal
                val barColor = if (isOverGoal) Color(0xFFCF6679) else NeonLime

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "${dayData.calories}",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(barColor.copy(alpha = 0.85f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dayData.day,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Goal line label
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(2.dp)
                    .background(NeonLime.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Goal: $goal kcal",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}
