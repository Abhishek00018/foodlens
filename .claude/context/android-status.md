# Caltrack Android — Status & Integration Contract

> **Last updated:** 2026-04-24 (Phase 3 complete)
> **Location:** `/Users/abhisheksharma/Caltrack/android/`
> **Repo:** `https://github.com/Abhishek00018/foodlens` (branch: `main`)
> **Stack:** Kotlin, Jetpack Compose, CameraX, Room, Hilt, Retrofit, Coil, Vico

---

## Build Status: COMPILES ✅ | Git: PUSHED ✅

Last build: `assembleDebug` passed. All 3 phases complete and pushed.
Latest commit: `2e8b01f` — "feat(android): Phase 3 — auth, WorkManager sync, shimmer polish, onboarding"

---

## All 3 Phases — Complete

| Phase | Goal | Status |
|-------|------|--------|
| 1 | Wire all screens to real ViewModels (no hardcoded data) | ✅ DONE |
| 2 | Replace multipart scan with 3-step S3 flow | ✅ DONE |
| 3 | Auth (Cognito), WorkManager sync, error polish, onboarding | ✅ DONE |

---

## All Screens

| Screen | File | Notes |
|--------|------|-------|
| Login | `ui/auth/LoginScreen.kt` | Wired to `LoginViewModel` — real Cognito sign-in/sign-up/confirm flow; Google via Hosted UI (Custom Tabs); dev-only skip when `COGNITO_CLIENT_ID` is blank |
| Onboarding | `ui/onboarding/OnboardingScreen.kt` | 3-page flow: Welcome (name) → Goals (sliders) → Allergies (FilterChips); shown on first login |
| Dashboard | `ui/dashboard/DashboardScreen.kt` | Room data via `DashboardViewModel`; shimmer skeleton loading; NetworkBanner; Retry on error |
| Camera | `ui/camera/CameraScreen.kt` | CameraX capture → `CameraViewModel.scanImage()` → presigned S3 PUT → trigger scan |
| Scan Result | `ui/camera/ScanResultSheet.kt` | Shows `ScanResponse`: food name, macros, confidence, allergen warnings |
| History | `ui/history/HistoryScreen.kt` | 7-day Room range; weekly Vico chart; pull-to-refresh syncs from API |
| Profile | `ui/profile/ProfileScreen.kt` | Reads/writes DataStore + API; logout clears DataStore and navigates to Login |
| Meal Detail | `ui/meal/MealDetailScreen.kt` | Single meal from Room via `MealDetailViewModel`; delete local+remote |

---

## ViewModels

| ViewModel | Key Details |
|-----------|-------------|
| `StartupViewModel` | Combines `authToken` + `userName` flows → routes to `LoginRoute` / `OnboardingRoute` / `DashboardRoute` |
| `LoginViewModel` | `signIn`, `signUp`, `confirmSignUp`, `skipLogin` (dev only), `clearError`; checks `isLoggedIn()` on init |
| `OnboardingViewModel` | 3-page state machine; saves name/goals/allergies to DataStore + best-effort API sync on `complete()` |
| `DashboardViewModel` | Room meals+goals for today; 6-flow combine; delete local+remote; `refresh()` |
| `HistoryViewModel` | 7-day Room range; weekly chart data; pull-to-refresh syncs from API |
| `ProfileViewModel` | DataStore goals/allergies; syncs from API on open; saves to API; `logout()` |
| `CameraViewModel` | Compress → get presigned URL → PUT to S3 → `triggerScan()` → `ScanResponse` |
| `MealDetailViewModel` | Single meal by local ID (`SavedStateHandle`); delete local+remote |

---

## Data Layer

### Local (Room)
| File | Description |
|------|-------------|
| `data/local/entity/MealEntity.kt` | `remoteId` (UUID), `imageKey` (S3), `mealTime`, `allergenWarnings`, `synced` flag |
| `data/local/dao/MealDao.kt` | Full CRUD + `getMealByRemoteId()`, `getUnsyncedMeals()`, `getMealByIdOnce()` |
| `data/local/CaltrackDatabase.kt` | `MIGRATION_1_2` adds 4 new columns |
| `data/local/entity/DailyGoalEntity.kt` | Calorie + macro goals stored locally |
| `data/local/UserPreferencesStore.kt` | DataStore: `auth_token`, `access_token`, `refresh_token`, `user_name`, `user_email`, goals, allergies; `saveTokens()`, `clearAll()` |

### Remote
| File | Description |
|------|-------------|
| `data/remote/ApiModels.kt` | Matches backend contract: UUID ids, Double macros, `ApiEnvelope<T>`, `UploadUrlResponse`, `ScanRequest/Response`, full user models |
| `data/remote/CaltrackApi.kt` | All Retrofit endpoints (see API contract below) |
| `data/remote/AuthTokenInterceptor.kt` | Reads `authToken` from DataStore via `runBlocking { prefsStore.authToken.firstOrNull() }`, adds `Authorization: Bearer` header |
| `data/remote/CognitoService.kt` | OkHttp REST calls to Cognito: `SignUp`, `ConfirmSignUp`, `InitiateAuth` (USER_PASSWORD_AUTH + REFRESH_TOKEN_AUTH), `GlobalSignOut`; dev-mode guard when `COGNITO_CLIENT_ID` is blank |

### Repositories
| File | Description |
|------|-------------|
| `data/repository/AuthRepository.kt` | Wraps `CognitoService`; saves `idToken`/`accessToken`/`refreshToken` to DataStore; `isDevMode()`, `isLoggedIn()`, `signOut()` |
| `data/repository/MealRepository.kt` | Scan 3-step flow; meals CRUD (local+remote); sync; `getUnsyncedMeals()` |
| `data/repository/UserRepository.kt` | Profile, goals, allergies CRUD; `safeCall` helper |

---

## DI & Infrastructure

| File | Description |
|------|-------------|
| `di/AppModule.kt` | Provides: Room DB + DAOs; OkHttp (with `AuthTokenInterceptor`, 60s timeouts); Retrofit; `CaltrackApi`; `CognitoService`; `AuthRepository`; `WorkManager` |
| `di/RepositoryModule.kt` | Binds repository interfaces |
| `CaltrackApp.kt` | `@HiltAndroidApp`; implements `Configuration.Provider`; injects `HiltWorkerFactory`; schedules `SyncWorker` on startup |
| `sync/SyncWorker.kt` | `@HiltWorker`; `CoroutineWorker`; fetches unsynced meals → `logMealRemote()` per meal (best-effort); `Result.retry()` on failure; runs every 15 min with `CONNECTED` network constraint |

---

## UI Components

| File | Description |
|------|-------------|
| `ui/components/ShimmerBox.kt` | `ShimmerBox`, `ShimmerMealCard` (72dp), `ShimmerProgressCard` (240dp) — `InfiniteTransition` + `linearGradient` |
| `ui/components/NetworkBanner.kt` | Animated offline bar; `ConnectivityManager.NetworkCallback` via `produceState`; slides in/out |
| `ui/components/CalorieProgressRing.kt` | Animated arc progress ring |
| `ui/components/MealCard.kt` | Meal row with macros; swipeable |
| `ui/components/WeeklyChart.kt` | Vico bar chart for 7-day history |

---

## Navigation

`ui/NavHost.kt` — `CaltrackNavHost()`:
- Startup: `StartupViewModel` emits `null` (loading) → `LoginRoute` / `OnboardingRoute` / `DashboardRoute`; shows `CircularProgressIndicator` until first emission
- Routes: `LoginRoute`, `OnboardingRoute`, `DashboardRoute`, `CameraRoute`, `HistoryRoute`, `ProfileRoute`, `MealDetailRoute(mealId)`
- Camera: `hiltViewModel<CameraViewModel>()`; `LaunchedEffect(scanState)` navigates to Dashboard on `MealLogged`
- Profile logout: clears DataStore → `StartupViewModel` re-emits `LoginRoute` automatically

---

## Auth & Security

### Cognito config (in `local.properties` — gitignored)
```
COGNITO_CLIENT_ID=<your-app-client-id>
COGNITO_REGION=ap-south-1
COGNITO_HOSTED_DOMAIN=<your-domain>.auth.ap-south-1.amazoncognito.com
COGNITO_REDIRECT_URI=caltrack://callback
```
Read at build time via `BuildConfig` fields. When `COGNITO_CLIENT_ID` is empty → **dev mode**: "Skip for now" button is visible, `skipLogin()` stores a dummy token, all Cognito calls return an error gracefully.

### Token flow
1. `LoginViewModel.signIn()` → `CognitoService.signIn()` → `AuthRepository` saves `idToken` (auth_token), `accessToken`, `refreshToken` to DataStore
2. `AuthTokenInterceptor` sends `idToken` as `Authorization: Bearer` on every backend request
3. Backend validates against Cognito JWT; returns 401 if invalid → Room data still displayed (offline-first)
4. Logout: `AuthRepository.signOut()` → `CognitoService.globalSignOut(accessToken)` + `UserPreferencesStore.clearAll()`

### Security files
- `.gitignore` — blocks `android/local.properties`, build dirs, keystores, `application-local.properties`
- `.gitleaks.toml` — secret scanning; allowlist for `${ENV_VAR}`, `.claude/agents/`, `.claude/context/`
- `.git-hooks/pre-commit` — runs `gitleaks protect --staged --verbose --redact` before every commit

---

## What's PENDING — Next Session Priority Order

### 1. Token Refresh Automation
- `AuthRepository.refreshTokens()` + `CognitoService.refreshTokens()` are implemented but not wired
- Add an OkHttp `Authenticator` to `AppModule.kt` that auto-calls `refreshTokens()` on 401 and retries

### 2. History Screen Error Polish
- History still uses `CircularProgressIndicator` — should match Dashboard: shimmer skeleton + NetworkBanner + Retry button

### 3. Image Display for Old Meals
- `imageKey` stored in Room; `imageUri` holds the presigned GET URL (valid ~5 min)
- After URL expires, MealDetail/History cards show placeholder
- Fix: add `GET /api/user/presigned-url?key=` endpoint on backend, or re-fetch on demand in MealDetailViewModel

### 4. Unit + UI Tests
- No tests written yet
- Unit: ViewModels (mock repositories), Repositories (mock API + DAO)
- UI: Compose test rules for Dashboard, Camera flow

---

## What Android EXPECTS from Backend

### Scan Flow (3-step — presigned S3)
```
Step 1: GET  /api/meals/scan/upload-url?contentType=image/jpeg  → UploadUrlResponse
Step 2: PUT  <uploadUrl>  (direct to S3, raw bytes — Android handles, NOT via backend)
Step 3: POST /api/meals/scan  { imageKey, contentType }          → ScanResponse
```

### Key Expectations
- **Base URL (local dev):** `http://10.0.2.2:8098/` — emulator → host machine, port 8098
- **Base URL (prod):** TBD (EC2)
- All responses wrapped: `{ "status": "success", "data": {...} }` or `{ "status": "error", "error": { "code": "...", "message": "..." } }`
- JWT via `Authorization: Bearer <idToken>` — Cognito ID token, validated by backend
- Dates as ISO strings: `"2026-04-24"`
- UUIDs as strings
- Null fields omitted (Jackson `NON_NULL`)
- Macros as `Double` in JSON (`42.0`)
- `DELETE /api/meals/{id}` must return `200` with envelope — NOT `204 No Content`

### Full Retrofit Interface (CaltrackApi.kt)
```kotlin
// Scan
@GET("api/meals/scan/upload-url")
suspend fun getScanUploadUrl(@Query("contentType") contentType: String = "image/jpeg"): Response<ApiEnvelope<UploadUrlResponse>>

@POST("api/meals/scan")
suspend fun triggerScan(@Body request: ScanRequest): Response<ApiEnvelope<ScanResponse>>

// Meals CRUD
@POST("api/meals") suspend fun logMeal(@Body meal: MealRequest): Response<ApiEnvelope<MealResponse>>
@GET("api/meals") suspend fun getMealsByDate(@Query("date") date: String): Response<ApiEnvelope<List<MealResponse>>>
@GET("api/meals/{id}") suspend fun getMealById(@Path("id") id: String): Response<ApiEnvelope<MealResponse>>
@PUT("api/meals/{id}") suspend fun updateMeal(@Path("id") id: String, @Body meal: MealRequest): Response<ApiEnvelope<MealResponse>>
@DELETE("api/meals/{id}") suspend fun deleteMeal(@Path("id") id: String): Response<ApiEnvelope<Unit>>
@GET("api/meals/weekly") suspend fun getWeeklySummary(): Response<ApiEnvelope<WeeklyResponse>>

// User
@GET("api/user/profile") suspend fun getProfile(): Response<ApiEnvelope<ProfileResponse>>
@PUT("api/user/profile") suspend fun updateProfile(@Body request: ProfileRequest): Response<ApiEnvelope<ProfileResponse>>
@GET("api/user/goals") suspend fun getGoals(): Response<ApiEnvelope<GoalResponse>>
@PUT("api/user/goals") suspend fun updateGoals(@Body request: GoalRequest): Response<ApiEnvelope<GoalResponse>>
@GET("api/user/allergies") suspend fun getAllergies(): Response<ApiEnvelope<AllergyResponse>>
@PUT("api/user/allergies") suspend fun updateAllergies(@Body request: AllergyRequest): Response<ApiEnvelope<AllergyResponse>>
```

### ScanResponse — required fields
```json
{
  "foodName": "Grilled Chicken Salad",
  "calories": 580,
  "protein": 42.0,
  "carbs": 18.0,
  "fat": 22.0,
  "confidence": 0.92,
  "items": [{ "name": "...", "calories": 200, "protein": 15.0, "carbs": 5.0, "fat": 8.0, "weightGrams": 150 }],
  "allergenWarnings": ["Dairy"],
  "imageKey": "meals/uuid-here.jpg",
  "imageUrl": "https://s3.amazonaws.com/...?X-Amz-...",
  "notes": "..."
}
```

---

## Shared Constants
| Constant | Value |
|----------|-------|
| Max image size | 10MB |
| Target compressed size | 500KB |
| Default calorie goal | 2000 kcal |
| Default protein goal | 150g |
| Default carbs goal | 250g |
| Default fat goal | 65g |
| Allergens list | Lactose, Gluten, Peanuts, Tree Nuts, Shellfish, Soy, Eggs, Fish, Sesame, Wheat |
| Low confidence threshold | 0.6 |
| Rate limit (scan) | 10 requests/minute |
| Backend local port | 8098 |
