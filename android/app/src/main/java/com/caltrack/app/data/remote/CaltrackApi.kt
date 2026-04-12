package com.caltrack.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CaltrackApi {

    // ─── Scan (3-step flow) ───────────────────────────────────────────────────
    //
    // Step 1: Get a presigned S3 PUT URL + imageKey.
    //         Android uploads the image directly to S3 (no Retrofit call for that).
    // Step 3: Tell backend to read from S3, run Bedrock, return analysis.

    @GET("api/meals/scan/upload-url")
    suspend fun getScanUploadUrl(
        @Query("contentType") contentType: String = "image/jpeg"
    ): Response<ApiEnvelope<UploadUrlResponse>>

    @POST("api/meals/scan")
    suspend fun triggerScan(
        @Body request: ScanRequest
    ): Response<ApiEnvelope<ScanResponse>>

    // ─── Meals ────────────────────────────────────────────────────────────────

    @POST("api/meals")
    suspend fun logMeal(
        @Body meal: MealRequest
    ): Response<ApiEnvelope<MealResponse>>

    @GET("api/meals")
    suspend fun getMealsByDate(
        @Query("date") date: String
    ): Response<ApiEnvelope<List<MealResponse>>>

    @GET("api/meals/{id}")
    suspend fun getMealById(
        @Path("id") id: String
    ): Response<ApiEnvelope<MealResponse>>

    @PUT("api/meals/{id}")
    suspend fun updateMeal(
        @Path("id") id: String,
        @Body meal: MealRequest
    ): Response<ApiEnvelope<MealResponse>>

    @DELETE("api/meals/{id}")
    suspend fun deleteMeal(
        @Path("id") id: String
    ): Response<ApiEnvelope<Unit>>

    @GET("api/meals/weekly")
    suspend fun getWeeklySummary(): Response<ApiEnvelope<WeeklyResponse>>

    // ─── User Profile ─────────────────────────────────────────────────────────

    @GET("api/user/profile")
    suspend fun getProfile(): Response<ApiEnvelope<ProfileResponse>>

    @PUT("api/user/profile")
    suspend fun updateProfile(
        @Body request: ProfileRequest
    ): Response<ApiEnvelope<ProfileResponse>>

    @GET("api/user/goals")
    suspend fun getGoals(): Response<ApiEnvelope<GoalResponse>>

    @PUT("api/user/goals")
    suspend fun updateGoals(
        @Body request: GoalRequest
    ): Response<ApiEnvelope<GoalResponse>>

    @GET("api/user/allergies")
    suspend fun getAllergies(): Response<ApiEnvelope<AllergyResponse>>

    @PUT("api/user/allergies")
    suspend fun updateAllergies(
        @Body request: AllergyRequest
    ): Response<ApiEnvelope<AllergyResponse>>
}
