package com.caltrack.app.ui.dashboard

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caltrack.app.ui.components.CalorieProgressRing
import com.caltrack.app.ui.components.MealItem
import com.caltrack.app.ui.components.NetworkBanner
import com.caltrack.app.ui.components.ShimmerMealCard
import com.caltrack.app.ui.components.ShimmerProgressCard
import com.caltrack.app.ui.components.SwipeableMealCard
import com.caltrack.app.ui.theme.DarkSurface
import com.caltrack.app.ui.theme.NeonLime
import com.caltrack.app.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    onCameraClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onMealClick: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (uiState.isLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { ShimmerProgressCard() }
                items(3) { ShimmerMealCard() }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        } else if (uiState.error != null && uiState.meals.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.error ?: "Something went wrong",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { viewModel.refresh() },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonLime)
                ) {
                    Text(text = "Retry", color = NeonLime)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Keep tracking your meals",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NeonLime)
                                .clickable { onProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "A",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                // Calorie ring + macros card
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CalorieProgressRing(
                                consumed = uiState.caloriesConsumed,
                                goal = uiState.calorieGoal,
                                size = 160.dp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                MacroStat(
                                    label = "Protein",
                                    current = uiState.proteinConsumed,
                                    goal = uiState.proteinGoal,
                                    unit = "g",
                                    color = NeonLime
                                )
                                MacroStat(
                                    label = "Carbs",
                                    current = uiState.carbsConsumed,
                                    goal = uiState.carbsGoal,
                                    unit = "g",
                                    color = Color(0xFFFFB74D)
                                )
                                MacroStat(
                                    label = "Fat",
                                    current = uiState.fatConsumed,
                                    goal = uiState.fatGoal,
                                    unit = "g",
                                    color = Color(0xFF80DEEA)
                                )
                            }
                        }
                    }
                }

                // Meals header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Meals",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${uiState.meals.size} items",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Meal cards
                if (uiState.meals.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No meals yet. Scan your first meal!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(uiState.meals, key = { it.id }) { meal ->
                        Box(modifier = Modifier.clickable { onMealClick(meal.id) }) {
                            SwipeableMealCard(
                                meal = meal,
                                onDelete = { deleted -> viewModel.deleteMeal(deleted) }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Scan Meal FAB
        ExtendedFloatingActionButton(
            onClick = onCameraClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            containerColor = NeonLime,
            contentColor = Color.Black,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = "📷  Scan Meal",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        NetworkBanner(modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun MacroStat(
    label: String,
    current: Int,
    goal: Int,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$current",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = unit,
                fontSize = 13.sp,
                color = color.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Text(
            text = "of $goal$unit",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
