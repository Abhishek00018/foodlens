package com.caltrack.app.data.remote

import com.google.gson.annotations.SerializedName

// ─── Envelope ────────────────────────────────────────────────────────────────

data class ApiEnvelope<T>(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: T?,
    @SerializedName("error") val error: ErrorDetail?
)

data class ErrorDetail(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String
)

// ─── Scan ─────────────────────────────────────────────────────────────────────

/** Step 1: backend returns a presigned S3 PUT URL + the key to use in Step 3. */
data class UploadUrlResponse(
    @SerializedName("uploadUrl") val uploadUrl: String,
    @SerializedName("imageKey") val imageKey: String,
    @SerializedName("expiresInSeconds") val expiresInSeconds: Int
)

/** Step 3: Android sends the already-uploaded S3 key; backend fetches from S3 and runs Bedrock. */
data class ScanRequest(
    @SerializedName("imageKey") val imageKey: String,
    @SerializedName("contentType") val contentType: String = "image/jpeg"
)

data class ScanResponse(
    @SerializedName("foodName") val foodName: String,
    @SerializedName("calories") val calories: Int,
    @SerializedName("protein") val protein: Double,
    @SerializedName("carbs") val carbs: Double,
    @SerializedName("fat") val fat: Double,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("items") val items: List<ScanItemResponse> = emptyList(),
    @SerializedName("allergenWarnings") val allergenWarnings: List<String> = emptyList(),
    @SerializedName("imageKey") val imageKey: String?,
    @SerializedName("imageUrl") val imageUrl: String?,   // presigned GET URL (5-min TTL)
    @SerializedName("notes") val notes: String?
)

data class ScanItemResponse(
    @SerializedName("name") val name: String,
    @SerializedName("weightGrams") val weightGrams: Double?,
    @SerializedName("calories") val calories: Double,
    @SerializedName("protein") val protein: Double,
    @SerializedName("carbs") val carbs: Double,
    @SerializedName("fat") val fat: Double,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("containsAllergens") val containsAllergens: Boolean
)

// ─── Meals ────────────────────────────────────────────────────────────────────

data class MealResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("totalCalories") val totalCalories: Double,
    @SerializedName("totalProtein") val totalProtein: Double,
    @SerializedName("totalCarbs") val totalCarbs: Double,
    @SerializedName("totalFat") val totalFat: Double,
    @SerializedName("imageKey") val imageKey: String?,
    @SerializedName("imageUrl") val imageUrl: String?,  // presigned GET URL (5-min TTL, nullable)
    @SerializedName("mealDate") val mealDate: String,
    @SerializedName("mealTime") val mealTime: String?,
    @SerializedName("overallConfidence") val overallConfidence: Double?,
    @SerializedName("userEdited") val userEdited: Boolean,
    @SerializedName("allergenWarnings") val allergenWarnings: List<String> = emptyList(),
    @SerializedName("items") val items: List<MealItemResponse> = emptyList()
)

data class MealItemResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("weightGrams") val weightGrams: Double?,
    @SerializedName("calories") val calories: Double,
    @SerializedName("protein") val protein: Double,
    @SerializedName("carbs") val carbs: Double,
    @SerializedName("fat") val fat: Double,
    @SerializedName("confidence") val confidence: Double?,
    @SerializedName("containsAllergens") val containsAllergens: Boolean
)

data class MealRequest(
    @SerializedName("name") val name: String,
    @SerializedName("calories") val calories: Double,
    @SerializedName("protein") val protein: Double,
    @SerializedName("carbs") val carbs: Double,
    @SerializedName("fat") val fat: Double,
    @SerializedName("imageKey") val imageKey: String?,
    @SerializedName("date") val date: String,
    @SerializedName("mealTime") val mealTime: String? = null,
    @SerializedName("items") val items: List<MealItemRequest> = emptyList()
)

data class MealItemRequest(
    @SerializedName("name") val name: String,
    @SerializedName("weightGrams") val weightGrams: Double? = null,
    @SerializedName("calories") val calories: Double,
    @SerializedName("protein") val protein: Double,
    @SerializedName("carbs") val carbs: Double,
    @SerializedName("fat") val fat: Double,
    @SerializedName("confidence") val confidence: Double? = null,
    @SerializedName("containsAllergens") val containsAllergens: Boolean = false
)

// ─── Weekly ───────────────────────────────────────────────────────────────────

data class DailySummary(
    @SerializedName("date") val date: String,
    @SerializedName("totalCalories") val totalCalories: Double
)

data class WeeklyResponse(
    @SerializedName("days") val days: List<DailySummary>
)

// ─── User Profile ─────────────────────────────────────────────────────────────

data class ProfileResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("photoUrl") val photoUrl: String?,
    @SerializedName("heightCm") val heightCm: Double?,
    @SerializedName("weightKg") val weightKg: Double?,
    @SerializedName("age") val age: Int?,
    @SerializedName("gender") val gender: String?
)

data class ProfileRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("photoUrl") val photoUrl: String? = null,
    @SerializedName("heightCm") val heightCm: Double? = null,
    @SerializedName("weightKg") val weightKg: Double? = null,
    @SerializedName("age") val age: Int? = null,
    @SerializedName("gender") val gender: String? = null
)

data class GoalResponse(
    @SerializedName("calorieGoal") val calorieGoal: Int,
    @SerializedName("proteinGoal") val proteinGoal: Int,
    @SerializedName("carbsGoal") val carbsGoal: Int,
    @SerializedName("fatGoal") val fatGoal: Int
)

data class GoalRequest(
    @SerializedName("calorieGoal") val calorieGoal: Int,
    @SerializedName("proteinGoal") val proteinGoal: Int,
    @SerializedName("carbsGoal") val carbsGoal: Int,
    @SerializedName("fatGoal") val fatGoal: Int
)

data class AllergyResponse(
    @SerializedName("allergies") val allergies: List<String>
)

data class AllergyRequest(
    @SerializedName("allergies") val allergies: List<String>
)
