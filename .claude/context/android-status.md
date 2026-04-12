# Caltrack Android — Status & Integration Contract

> **Last updated:** 2026-04-11
> **Location:** `/Users/abhisheksharma/Caltrack/android/`
> **Stack:** Kotlin, Jetpack Compose, CameraX, Room, Hilt, Retrofit, Coil, Vico

---

## Build Status: COMPILES SUCCESSFULLY ✅

Last build: `assembleDebug` passed. App runs on emulator.

---

## What's DONE (Android)

### UI Screens — All wired to real ViewModels (no more sample data)
| Screen | File | Status |
|--------|------|--------|
| Login | `ui/auth/LoginScreen.kt` | Done — email/password + Google button + "Skip for now" dev mode. No real Cognito auth yet. |
| Dashboard | `ui/dashboard/DashboardScreen.kt` | **Done** — wired to DashboardViewModel. Real Room data. |
| Camera | `ui/camera/CameraScreen.kt` | **Done** — CameraX, captures photo, calls CameraViewModel.scanImage(). |
| Scan Result | `ui/camera/ScanResultSheet.kt` | **Done** — shows real ScanResponse from backend, allergen warnings, confidence. |
| History | `ui/history/HistoryScreen.kt` | **Done** — wired to HistoryViewModel. Real Room 7-day data, weekly chart. |
| Profile | `ui/profile/ProfileScreen.kt` | **Done** — wired to ProfileViewModel. Reads/writes DataStore + API. |
| Meal Detail | `ui/meal/MealDetailScreen.kt` | **Done** — wired to MealDetailViewModel. Real Room data. |

### ViewModels (all new)
| ViewModel | Description |
|-----------|-------------|
| `DashboardViewModel` | Room meals+goals for today, delete (local+remote), refresh |
| `HistoryViewModel` | 7-day Room range, weekly calorie chart, pull-to-refresh syncs from API |
| `ProfileViewModel` | DataStore for goals/allergies, syncs from API on open, saves to API |
| `CameraViewModel` | Image compress (target 500KB) → **3-step scan flow** (get upload URL → PUT to S3 → trigger scan) → ScanResponse → log meal. **Needs refactor** — see Pending §2 |
| `MealDetailViewModel` | Single meal by local ID from Room, delete (local+remote) |

### Data Layer
| File | Description |
|------|-------------|
| `data/local/entity/MealEntity.kt` | **v2** — added remoteId (UUID), imageKey (S3), mealTime, allergenWarnings |
| `data/local/dao/MealDao.kt` | **Updated** — getMealByIdOnce(), getMealByRemoteId(), deleteById(), getUnsyncedMeals() |
| `data/local/CaltrackDatabase.kt` | **v2** — MIGRATION_1_2 adds remoteId/imageKey/mealTime/allergenWarnings columns |
| `data/local/UserPreferencesStore.kt` | **New** — DataStore for auth token, user name/email, goals, allergies |
| `data/remote/ApiModels.kt` | **Updated** — matches backend contract exactly (UUID ids, ScanItems, ErrorDetail, user models) |
| `data/remote/CaltrackApi.kt` | **Updated** — all endpoints: meals CRUD, weekly, user profile/goals/allergies |
| `data/remote/AuthTokenInterceptor.kt` | **New** — OkHttp interceptor reads token from DataStore, adds Authorization header |
| `data/repository/MealRepository.kt` | **Updated** — logMealRemote, deleteMealRemote, syncMealsForDate, getMealByLocalId |
| `data/repository/UserRepository.kt` | **New** — profile, goals, allergies CRUD via Retrofit |
| `di/AppModule.kt` | **Updated** — AuthTokenInterceptor added, migration added, longer timeouts (60s) |

### Navigation
- `NavHost.kt` — **Updated** — Camera route uses CameraViewModel. Full scan flow: capture → Scanning overlay → ScanResultSheet → LoggingMeal overlay → navigate to Dashboard.
- MealDetailRoute no longer passes `mealId` param (SavedStateHandle used by VM)

### Auth / DataStore
- `UserPreferencesStore` stores JWT token (empty until Cognito is wired)
- `AuthTokenInterceptor` adds `Authorization: Bearer <token>` to all requests
- API calls fail with 401 gracefully when token is absent (offline-first Room data shows)
- Login "Skip for now" → no token → API calls fail silently, Room data used

---

## What's PENDING (Android)

### 1. Auth Integration (Cognito) — PRIORITY
- Replace dummy login with real AWS Cognito SDK
- Google Sign-In flow
- Email/password signup + signin
- Store JWT via `UserPreferencesStore.saveAuthToken()`
- Token refresh logic

### 2. Scan Flow Refactor — DONE ✅
- `CaltrackApi.kt` — multipart removed; `getScanUploadUrl()` + `triggerScan()` added
- `ApiModels.kt` — `UploadUrlResponse`, `ScanRequest` added; `imageUrl` added to `MealResponse` + `ScanResponse`
- `CameraViewModel.scanImage()` — 3-step flow implemented with dedicated `s3Client` (no auth header)
- `MealRepository` — `getScanUploadUrl()` + `triggerScan()` wired

### 3. Image Display for Logged Meals
- ScanResultSheet already shows `localImageUri` for local preview (no change needed)
- `imageKey` from ScanResponse is stored in Room (no change needed)
- For displaying images in MealDetail/History: backend can return presigned GET URL inline,
  or Android can call a future endpoint. Defer until needed.

### 3. Offline-First Sync
- `MealRepository.getUnsyncedMeals()` exists but no background WorkManager job yet
- Add WorkManager sync job that runs on connectivity restored

### 4. Error Handling improvements
- Shimmer skeleton screens for initial load
- Retry buttons on error states
- No-network banner

### 5. Onboarding Flow
- First-launch: set name, goals, allergies before Dashboard

### 6. Testing
- No tests written yet

---

## What Android EXPECTS from Backend

### BREAKING CHANGE: Scan flow is now 3 steps (no more multipart upload to backend)

**Old flow (removed):**
```
POST /api/meals/scan   multipart image → ScanResponse
```

**New flow:**
```
Step 1: GET  /api/meals/scan/upload-url?contentType=image/jpeg  → UploadUrlResponse
Step 2: PUT  <uploadUrl>  (direct to S3, raw bytes, Content-Type header required)
Step 3: POST /api/meals/scan  { imageKey, contentType }          → ScanResponse
```

### New API Models needed in `ApiModels.kt`
```kotlin
data class UploadUrlResponse(
    val uploadUrl: String,   // presigned S3 PUT URL, expires in expiresInSeconds
    val imageKey: String,    // use this key in ScanRequest
    val expiresInSeconds: Int
)

data class ScanRequest(
    val imageKey: String,
    val contentType: String? = "image/jpeg"
)
```

### Updated Retrofit Interface (`CaltrackApi.kt`)
Replace the old `scanMealImage` with:
```kotlin
// Step 1 — get upload URL (rate limited: 10/min)
@GET("api/meals/scan/upload-url")
suspend fun getScanUploadUrl(
    @Query("contentType") contentType: String = "image/jpeg"
): Response<ApiEnvelope<UploadUrlResponse>>

// Step 3 — trigger analysis after S3 upload (rate limited: 10/min)
@POST("api/meals/scan")
suspend fun triggerScan(@Body request: ScanRequest): Response<ApiEnvelope<ScanResponse>>

// Remove this — no longer exists:
// @Multipart @POST("api/meals/scan") suspend fun scanMealImage(...)
```

### Step 2 — Direct S3 PUT (NOT via Retrofit, use OkHttp directly)
S3 presigned PUT requires a raw HTTP PUT with the exact `Content-Type` header. Do NOT use Retrofit for this call.
```kotlin
// In CameraViewModel or a dedicated S3UploadService:
suspend fun uploadToS3(uploadUrl: String, imageBytes: ByteArray, contentType: String) {
    val client = OkHttpClient()
    val body = imageBytes.toRequestBody(contentType.toMediaType())
    val request = Request.Builder()
        .url(uploadUrl)
        .put(body)
        .header("Content-Type", contentType)
        .build()
    val response = client.newCall(request).execute()
    if (!response.isSuccessful) throw IOException("S3 upload failed: ${response.code}")
}
```

### Updated `CameraViewModel` scan flow
Replace `scanImage()` to follow the 3-step flow:
```
1. Compress captured image (existing logic, target 500KB)
2. Call getScanUploadUrl(contentType = "image/jpeg") → get uploadUrl + imageKey
3. PUT image bytes directly to uploadUrl (OkHttp, see above)
4. Call triggerScan(ScanRequest(imageKey, "image/jpeg")) → ScanResponse
5. Show result in ScanResultSheet (unchanged)
```

### Image handling in `Section 2 — What's PENDING`
The existing "Image Handling for S3" pending item is now partially resolved:
- `imageKey` from ScanResponse is still stored in Room (unchanged)
- For displaying previously-logged meal images: call `GET /api/user/presigned-url?key=<imageKey>` (not yet implemented — backend can add this endpoint if needed) OR use the existing S3 presigned download URL pattern

### Retrofit Interface (full — all endpoints)
```kotlin
// Scan (new 2-endpoint pattern)
@GET("api/meals/scan/upload-url")
suspend fun getScanUploadUrl(@Query("contentType") contentType: String = "image/jpeg"): Response<ApiEnvelope<UploadUrlResponse>>

@POST("api/meals/scan")
suspend fun triggerScan(@Body request: ScanRequest): Response<ApiEnvelope<ScanResponse>>

// Meals (unchanged)
@POST("api/meals") suspend fun logMeal(@Body meal: MealRequest): Response<ApiEnvelope<MealResponse>>
@GET("api/meals") suspend fun getMealsByDate(@Query("date") date: String): Response<ApiEnvelope<List<MealResponse>>>
@GET("api/meals/{id}") suspend fun getMealById(@Path("id") id: String): Response<ApiEnvelope<MealResponse>>
@PUT("api/meals/{id}") suspend fun updateMeal(@Path("id") id: String, @Body meal: MealRequest): Response<ApiEnvelope<MealResponse>>
@DELETE("api/meals/{id}") suspend fun deleteMeal(@Path("id") id: String): Response<ApiEnvelope<Unit>>
@GET("api/meals/weekly") suspend fun getWeeklySummary(): Response<ApiEnvelope<WeeklyResponse>>

// User (unchanged)
@GET("api/user/profile") suspend fun getProfile(): Response<ApiEnvelope<ProfileResponse>>
@PUT("api/user/profile") suspend fun updateProfile(@Body request: ProfileRequest): Response<ApiEnvelope<ProfileResponse>>
@GET("api/user/goals") suspend fun getGoals(): Response<ApiEnvelope<GoalResponse>>
@PUT("api/user/goals") suspend fun updateGoals(@Body request: GoalRequest): Response<ApiEnvelope<GoalResponse>>
@GET("api/user/allergies") suspend fun getAllergies(): Response<ApiEnvelope<AllergyResponse>>
@PUT("api/user/allergies") suspend fun updateAllergies(@Body request: AllergyRequest): Response<ApiEnvelope<AllergyResponse>>
```

### Key Expectations
- Base URL: `http://10.0.2.2:8080/` (emulator → host localhost)  
  ⚠️ **Local dev port is `8098`** (overridden in `application-local.properties`) — update `AppModule.kt` if needed
- All responses wrapped in `ApiEnvelope<T>` { status, data, error: {code, message} }
- JWT passed via `Authorization: Bearer <token>` header
- **Image upload goes directly to S3 (NOT to backend)** — see new scan flow above
- Dates as ISO strings: `"2026-04-11"`
- UUIDs as strings
- Null fields omitted (Jackson NON_NULL)

---

## Shared Constants (must match between Android & Backend)
| Constant | Value |
|----------|-------|
| Max image size | 10MB |
| Target compressed size | 500KB |
| Default calorie goal | 2000 |
| Default protein goal | 150g |
| Default carbs goal | 250g |
| Default fat goal | 65g |
| Allergens list | Lactose, Gluten, Peanuts, Tree Nuts, Shellfish, Soy, Eggs, Fish, Sesame, Wheat |
| Low confidence threshold | 0.6 |
| Rate limit (scan) | 10 requests/minute |
