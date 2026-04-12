package com.caltrack.app.ui.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caltrack.app.ui.theme.DarkBg
import com.caltrack.app.ui.theme.DarkSurface
import com.caltrack.app.ui.theme.DarkSurfaceVariant
import com.caltrack.app.ui.theme.NeonLime
import com.caltrack.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialize editable local state from ViewModel (synced once on first composition)
    var userName by remember(uiState.name) { mutableStateOf(uiState.name.ifBlank { "User" }) }
    var userEmail by remember(uiState.email) { mutableStateOf(uiState.email) }
    var calorieGoal by remember(uiState.calorieGoal) { mutableIntStateOf(uiState.calorieGoal) }
    var proteinGoal by remember(uiState.proteinGoal) { mutableIntStateOf(uiState.proteinGoal) }
    var carbsGoal by remember(uiState.carbsGoal) { mutableIntStateOf(uiState.carbsGoal) }
    var fatGoal by remember(uiState.fatGoal) { mutableIntStateOf(uiState.fatGoal) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showEditGoals by remember { mutableStateOf(false) }
    var showAllergyDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    var allergies by remember(uiState.allergies) { mutableStateOf(uiState.allergies) }

    // Sync from server on first open
    LaunchedEffect(Unit) {
        viewModel.syncFromServer()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        TopAppBar(
            title = { Text("Profile", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBg,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Profile header
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(NeonLime),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = userEmail,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = { /* TODO: edit profile */ }) {
                        Icon(Icons.Default.Edit, "Edit", tint = NeonLime)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Daily Goals Section
            SectionHeader("Daily Goals")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    GoalRow("Calories", "$calorieGoal kcal")
                    GoalRow("Protein", "${proteinGoal}g")
                    GoalRow("Carbs", "${carbsGoal}g")
                    GoalRow("Fat", "${fatGoal}g")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showEditGoals = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonLime.copy(alpha = 0.12f)
                        )
                    ) {
                        Text("Edit Goals", color = NeonLime, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Allergies & Restrictions
            SectionHeader("Allergies & Restrictions")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.clickable { showAllergyDialog = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (allergies.isEmpty()) {
                        Text(
                            text = "No allergies set. Tap to add.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allergies.forEach { allergy ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFCF6679).copy(alpha = 0.15f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFCF6679),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = allergy,
                                            color = Color(0xFFCF6679),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to manage allergies",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Settings
            SectionHeader("Settings")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        title = "Meal Reminders",
                        trailing = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonLime
                                )
                            )
                        }
                    )
                    SettingsRow(
                        icon = Icons.Default.Security,
                        title = "Privacy & Data",
                        trailing = {
                            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
                        }
                    )
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = "Account Settings",
                        trailing = {
                            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Logout
            Button(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCF6679).copy(alpha = 0.15f)
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFCF6679))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", color = Color(0xFFCF6679), fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Edit Goals Dialog
    if (showEditGoals) {
        EditGoalsDialog(
            calorieGoal = calorieGoal,
            proteinGoal = proteinGoal,
            carbsGoal = carbsGoal,
            fatGoal = fatGoal,
            onSave = { cal, pro, carb, fat ->
                calorieGoal = cal
                proteinGoal = pro
                carbsGoal = carb
                fatGoal = fat
                viewModel.updateGoals(cal, pro, carb, fat)
                showEditGoals = false
            },
            onDismiss = { showEditGoals = false }
        )
    }

    // Allergy Dialog
    if (showAllergyDialog) {
        AllergyDialog(
            currentAllergies = allergies,
            onSave = { newAllergies ->
                allergies = newAllergies
                viewModel.updateAllergies(newAllergies)
                showAllergyDialog = false
            },
            onDismiss = { showAllergyDialog = false }
        )
    }

    // Logout Confirm
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log Out", color = Color.White) },
            text = { Text("Are you sure you want to log out?", color = TextSecondary) },
            containerColor = DarkSurface,
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    viewModel.logout()
                    onLogout()
                }) {
                    Text("Log Out", color = Color(0xFFCF6679))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun GoalRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Text(text = value, color = NeonLime, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

@Composable
private fun EditGoalsDialog(
    calorieGoal: Int,
    proteinGoal: Int,
    carbsGoal: Int,
    fatGoal: Int,
    onSave: (Int, Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var cal by remember { mutableStateOf(calorieGoal.toString()) }
    var pro by remember { mutableStateOf(proteinGoal.toString()) }
    var carb by remember { mutableStateOf(carbsGoal.toString()) }
    var fat by remember { mutableStateOf(fatGoal.toString()) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonLime,
        unfocusedBorderColor = DarkSurfaceVariant,
        focusedLabelColor = NeonLime,
        unfocusedLabelColor = TextSecondary,
        cursorColor = NeonLime,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Edit Daily Goals", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = cal, onValueChange = { cal = it },
                    label = { Text("Calories (kcal)") },
                    colors = fieldColors, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = pro, onValueChange = { pro = it },
                    label = { Text("Protein (g)") },
                    colors = fieldColors, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = carb, onValueChange = { carb = it },
                    label = { Text("Carbs (g)") },
                    colors = fieldColors, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = fat, onValueChange = { fat = it },
                    label = { Text("Fat (g)") },
                    colors = fieldColors, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        cal.toIntOrNull() ?: calorieGoal,
                        pro.toIntOrNull() ?: proteinGoal,
                        carb.toIntOrNull() ?: carbsGoal,
                        fat.toIntOrNull() ?: fatGoal
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AllergyDialog(
    currentAllergies: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val commonAllergies = listOf(
        "Lactose", "Gluten", "Peanuts", "Tree Nuts", "Shellfish",
        "Soy", "Eggs", "Fish", "Sesame", "Wheat"
    )
    var selected by remember { mutableStateOf(currentAllergies.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Manage Allergies", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Select your food allergies/intolerances. You'll be alerted when scanning meals containing these items.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Chip grid
                val rows = commonAllergies.chunked(3)
                rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { allergy ->
                            val isSelected = allergy in selected
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) NeonLime.copy(alpha = 0.15f)
                                        else DarkSurfaceVariant
                                    )
                                    .clickable {
                                        selected = if (isSelected) selected - allergy
                                        else selected + allergy
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = allergy,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonLime else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selected.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
