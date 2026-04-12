package com.caltrack.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_CALORIE_GOAL = intPreferencesKey("calorie_goal")
        val KEY_PROTEIN_GOAL = intPreferencesKey("protein_goal")
        val KEY_CARBS_GOAL = intPreferencesKey("carbs_goal")
        val KEY_FAT_GOAL = intPreferencesKey("fat_goal")
        val KEY_ALLERGIES = stringPreferencesKey("allergies") // comma-separated
    }

    val authToken: Flow<String?> = context.dataStore.data.map { it[KEY_AUTH_TOKEN] }

    val userName: Flow<String?> = context.dataStore.data.map { it[KEY_USER_NAME] }

    val userEmail: Flow<String?> = context.dataStore.data.map { it[KEY_USER_EMAIL] }

    val calorieGoal: Flow<Int> = context.dataStore.data.map { it[KEY_CALORIE_GOAL] ?: 2000 }

    val proteinGoal: Flow<Int> = context.dataStore.data.map { it[KEY_PROTEIN_GOAL] ?: 150 }

    val carbsGoal: Flow<Int> = context.dataStore.data.map { it[KEY_CARBS_GOAL] ?: 250 }

    val fatGoal: Flow<Int> = context.dataStore.data.map { it[KEY_FAT_GOAL] ?: 65 }

    val allergies: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_ALLERGIES]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { it[KEY_AUTH_TOKEN] = token }
    }

    suspend fun clearAuthToken() {
        context.dataStore.edit { it.remove(KEY_AUTH_TOKEN) }
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun saveUserEmail(email: String) {
        context.dataStore.edit { it[KEY_USER_EMAIL] = email }
    }

    suspend fun saveGoals(calorie: Int, protein: Int, carbs: Int, fat: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CALORIE_GOAL] = calorie
            prefs[KEY_PROTEIN_GOAL] = protein
            prefs[KEY_CARBS_GOAL] = carbs
            prefs[KEY_FAT_GOAL] = fat
        }
    }

    suspend fun saveAllergies(allergies: List<String>) {
        context.dataStore.edit { it[KEY_ALLERGIES] = allergies.joinToString(",") }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
