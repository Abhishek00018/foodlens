package com.caltrack.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caltrack.app.ui.theme.DarkSurface
import com.caltrack.app.ui.theme.DarkSurfaceVariant
import com.caltrack.app.ui.theme.NeonLime
import com.caltrack.app.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = state.currentPage,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                } else {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            },
            label = "onboarding_page"
        ) { page ->
            when (page) {
                0 -> WelcomePage(
                    name = state.name,
                    onNameChange = viewModel::setName,
                    onNext = viewModel::nextPage
                )
                1 -> GoalsPage(
                    calorieGoal = state.calorieGoal,
                    proteinGoal = state.proteinGoal,
                    carbsGoal = state.carbsGoal,
                    fatGoal = state.fatGoal,
                    onCalorieChange = viewModel::setCalorieGoal,
                    onProteinChange = viewModel::setProteinGoal,
                    onCarbsChange = viewModel::setCarbsGoal,
                    onFatChange = viewModel::setFatGoal,
                    onBack = viewModel::prevPage,
                    onNext = viewModel::nextPage
                )
                2 -> AllergiesPage(
                    selectedAllergies = state.selectedAllergies,
                    onToggle = viewModel::toggleAllergen,
                    onBack = viewModel::prevPage,
                    isLoading = state.isLoading,
                    onComplete = { viewModel.complete() }
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(
    name: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonLime,
        unfocusedBorderColor = DarkSurfaceVariant,
        focusedLabelColor = NeonLime,
        unfocusedLabelColor = TextSecondary,
        cursorColor = NeonLime,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(NeonLime),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "CT", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to Caltrack",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "AI-powered calorie tracking from photos",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
        ) {
            Text(text = "Next", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        }
    }
}

@Composable
private fun GoalsPage(
    calorieGoal: Int,
    proteinGoal: Int,
    carbsGoal: Int,
    fatGoal: Int,
    onCalorieChange: (Int) -> Unit,
    onProteinChange: (Int) -> Unit,
    onCarbsChange: (Int) -> Unit,
    onFatChange: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Set Your Daily Goals",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We'll track your progress against these targets",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(40.dp))

        GoalSlider(
            label = "Calories",
            value = calorieGoal,
            min = 500,
            max = 4000,
            unit = "kcal",
            color = NeonLime,
            onValueChange = onCalorieChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        GoalSlider(
            label = "Protein",
            value = proteinGoal,
            min = 50,
            max = 300,
            unit = "g",
            color = NeonLime,
            onValueChange = onProteinChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        GoalSlider(
            label = "Carbs",
            value = carbsGoal,
            min = 50,
            max = 500,
            unit = "g",
            color = Color(0xFFFFB74D),
            onValueChange = onCarbsChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        GoalSlider(
            label = "Fat",
            value = fatGoal,
            min = 20,
            max = 150,
            unit = "g",
            color = Color(0xFF80DEEA),
            onValueChange = onFatChange
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Back", color = Color.White)
            }
            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
            ) {
                Text(text = "Next", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
private fun GoalSlider(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    unit: String,
    color: Color,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextSecondary, fontSize = 14.sp)
            Text(
                text = "$value $unit",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = DarkSurfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllergiesPage(
    selectedAllergies: Set<String>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Any Food Allergies?",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We'll warn you when scanned meals contain these",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(40.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ALLERGEN_LIST.forEach { allergen ->
                val selected = allergen in selectedAllergies
                FilterChip(
                    selected = selected,
                    onClick = { onToggle(allergen) },
                    label = { Text(text = allergen, fontSize = 14.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonLime.copy(alpha = 0.2f),
                        selectedLabelColor = NeonLime,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        selectedBorderColor = NeonLime,
                        borderColor = DarkSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Back", color = Color.White)
            }
            Button(
                onClick = onComplete,
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Get Started",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
