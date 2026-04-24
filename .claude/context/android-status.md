# Caltrack Android — Status & Integration Contract

> **Last updated:** 2026-04-24 (Phase 3 complete)
> **Location:** `/Users/abhisheksharma/Caltrack/android/`
> **Repo:** `https://github.com/Abhishek00018/foodlens` (branch: `main`)
> **Stack:** Kotlin, Jetpack Compose, CameraX, Room, Hilt, Retrofit, Coil, Vico

---

## Build Status: COMPILES ✅ | Git: PUSHED ✅

Last build: `assembleDebug` passed. Phase 3 complete — all 48 source files across 3 phases committed and pushed.

---

## Original Plan (3 Phases)

| Phase | Goal | Status |
|-------|------|--------|
| 1 | Wire all screens to real ViewModels (no hardcoded data) | ✅ DONE |
| 2 | Replace multipart scan with 3-step S3 flow | ✅ DONE |
| 3 | Auth (Cognito), WorkManager sync, error polish, onboarding | ✅ DONE |

---

## What's DONE (Android)

### UI Screens — All wired to real ViewModels (no sample data)
| Screen | File | Status |
|--------|------|--------|
| Login | `ui/auth/LoginScreen.kt` | Done — email/password + Google button + "Skip for now" dev mode. No real Cognito auth yet. |
| Dashboard | `ui/dashboard/DashboardScreen.kt` | Done — real Room data via DashboardViewModel |
| Camera | `ui/camera/CameraScreen.kt` | Done — CameraX, captures photo, calls CameraViewModel.scanImage() |
| Scan Result | `ui/camera/ScanResultSheet.kt` | Done — shows real ScanResponse, allergen warnings, confidence |
| History | `ui/history/HistoryScreen.kt` | Done — 7-day Room data, weekly Vico chart |
| Profile | `ui/profile/ProfileScreen.kt` | Done — reads/writes DataStore + API |
| Meal Detail | `ui/meal/MealDetailScreen.kt` | Done — single meal from Room via MealDetailViewModel |

### ViewModels
| ViewModel | Key Details |
|-----------|-------------|
| `DashboardViewModel` | Room meals+goals for today; nested combine (6 flows); delete local+remote |
| `HistoryViewModel` | 7-day Room range; weekly chart; pull-to-refresh syncs from API |
| `ProfileViewModel` | DataStore goals/allergies; syncs from API on open; saves to API |
| `CameraViewModel` | Compress → get presigned URL → PUT to S3 (dedicated s3Client, no auth) → triggerScan → ScanResponse |
| `MealDetailViewModel` | Single meal by local ID (SavedStateHandle); delete local+remote |

### Data Layer
| File | Description |
|------|-------------|
| `data/local/entity/MealEntity.kt` | v2 — added `remoteId` (UUID), `imageKey` (S3 key), `mealTime`, `allergenWarnings` |
| `data/local/dao/MealDao.kt` | Added: `getMealByIdOnce()` (suspend), `getMealByRemoteId()`, `deleteById()`, `getUnsyncedMeals()` |
| `data/local/CaltrackDatabase.kt` | v2 — `MIGRATION_1_2` adds all 4 new columns via ALTER TABLE |
| `data/local/UserPreferencesStore.kt` | NEW — DataStore: auth_token, user_name, user_email, goals, allergies |
| `data/remote/ApiModels.kt` | Matches backend contract exactly — UUID ids, Double macros, ErrorDetail, UploadUrlResponse, ScanRequest, full user models |
| `data/remote/CaltrackApi.kt` | All endpoints: scan (2-step), meals CRUD, user profile/goals/allergies |
| `data/remote/AuthTokenInterceptor.kt` | NEW — reads token via `runBlocking { prefsStore.authToken.firstOrNull() }` |
| `data/repository/MealRepository.kt` | getScanUploadUrl(), triggerScan(), logMeal, delete, sync, getMealByLocalId |
| `data/repository/UserRepository.kt` | NEW — profile, goals, allergies CRUD; `safeCall` helper |
| `di/AppModule.kt` | AuthTokenInterceptor added; MIGRATION_1_2 added; 60s timeouts; BASE_URL = `http://10.0.2.2:8098/` |

### Navigation
- `NavHost.kt` — Camera route uses `hiltViewModel<CameraViewModel>()`; `LaunchedEffect(scanState)` navigates to Dashboard on `MealLogged`; Scanning/LoggingMeal overlays with `CircularProgressIndicator`
- MealDetail uses `SavedStateHandle` for `mealId` — no param passing in NavHost

### Auth / DataStore
- `UserPreferencesStore` stores JWT token (blank until Cognito wired)
- `AuthTokenInterceptor` adds `Authorization: Bearer <token>` to all requests
- API calls fail with 401 gracefully — Room data still displays (offline-first)
- "Skip for now" on Login → no token → silent API failures → Room data used

### Security
- `.gitignore` — blocks `android/local.properties`, `android/.gradle/`, `android/app/build/`, `.env*`, keystores, `application-local.properties`
- `.gitleaks.toml` — secret scanning config; allowlist for `${ENV_VAR}`, `.claude/agents/` (Figma file keys)
- `.git-hooks/pre-commit` — runs `gitleaks protect --staged --verbose --redact` before every commit
- `gitleaks` installed via Homebrew; hook copied to `.git/hooks/pre-commit`

### Git
- Repo: `https://github.com/Abhishek00018/foodlens`
- Branch: `main`
- Commit: `18de2f2` — "feat(android): initial commit — full Android app with offline-first architecture"
- 62 files, 6164 insertions

---

## What's DONE (Phase 3 additions)

### Auth (Cognito via direct REST API)
| File | Description |
|------|-------------|
| `data/remote/CognitoService.kt` | OkHttp client calling Cognito REST: SignUp, ConfirmSignUp, InitiateAuth (USER_PASSWORD_AUTH + REFRESH_TOKEN_AUTH), GlobalSignOut |
| `data/repository/AuthRepository.kt` | Orchestrates Cognito calls, saves idToken/accessToken/refreshToken to DataStore, exposes `isDevMode()` / `isLoggedIn()` |
| `ui/auth/AuthUiState.kt` | Sealed class: Idle, Loading, SignedIn, NeedsConfirmation(email), Error |
| `ui/auth/LoginViewModel.kt` | @HiltViewModel: signIn, signUp, confirmSignUp, skipLogin (dev mode only), clearError. Checks isLoggedIn() on init. |
| `ui/auth/StartupViewModel.kt` | Combines authToken + userName flows → emits LoginRoute / OnboardingRoute / DashboardRoute |
| `ui/auth/LoginScreen.kt` | Wired to LoginViewModel: real auth flow, email confirmation screen, LinearProgressIndicator, error display, Hosted UI for Google, dev-only skip |
| `data/local/UserPreferencesStore.kt` | Added KEY_ACCESS_TOKEN, KEY_REFRESH_TOKEN, accessToken/refreshToken flows, saveTokens() |

### Cognito config (local.properties — gitignored)
Keys needed when Cognito is set up:
```
COGNITO_CLIENT_ID=<your-app-client-id>
COGNITO_REGION=ap-south-1
COGNITO_HOSTED_DOMAIN=<your-domain>.auth.ap-south-1.amazoncognito.com
COGNITO_REDIRECT_URI=caltrack://callback
```
When `COGNITO_CLIENT_ID` is empty → dev mode (skip auth works, real Cognito calls return error gracefully).

### WorkManager Offline Sync
| File | Description |
|------|-------------|
| `sync/SyncWorker.kt` | @HiltWorker: fetches unsynced meals, calls logMealRemote(), retries on failure |
| `CaltrackApp.kt` | Implements Configuration.Provider, injects HiltWorkerFactory, schedules SyncWorker (every 15min, CONNECTED constraint) |
| `data/repository/MealRepository.kt` | Added getUnsyncedMeals() |
| `di/AppModule.kt` | Provides WorkManager, CognitoService, AuthRepository |

### Error Handling Polish
| File | Description |
|------|-------------|
| `ui/components/ShimmerBox.kt` | ShimmerBox, ShimmerMealCard (72dp), ShimmerProgressCard (240dp) — InfiniteTransition + linearGradient |
| `ui/components/NetworkBanner.kt` | Animated offline banner using ConnectivityManager.NetworkCallback via produceState |
| `ui/dashboard/DashboardScreen.kt` | Replaced CircularProgressIndicator with shimmer skeletons, NetworkBanner at top, error+Retry state |

### Onboarding Flow
| File | Description |
|------|-------------|
| `ui/onboarding/OnboardingViewModel.kt` | 3-page state machine (Welcome/Goals/Allergies), saves to DataStore + best-effort API sync |
| `ui/onboarding/OnboardingScreen.kt` | AnimatedContent page transitions, goal sliders, allergen FilterChip grid |
| `ui/NavHost.kt` | StartupViewModel routes to Login/Onboarding/Dashboard; OnboardingRoute added |

### Navigation routing (StartupViewModel)
- `authToken == null` → LoginRoute
- `authToken != null && userName == null` → OnboardingRoute  
- Both set → DashboardRoute
- StartDestination shows loading spinner until flows emit

---

## What's PENDING (Android) — Next Session Priority Order

### 1. Testing
- No tests written yet
- Unit tests: ViewModels (mock repositories), Repository (mock API + DAO)
- UI tests: Compose test rules for Dashboard, Camera flow

### 2. Image Display for Old Meals
- `imageKey` stored in Room — backend can return presigned GET URL inline in `MealResponse.imageUrl`
- MealDetail + History cards: load `imageUri` (Coil) — will show placeholder for expired URLs
- Long-term: either fetch fresh presigned URL on demand

### 3. History screen error polish
- History screen still uses CircularProgressIndicator — should get shimmer + retry button same as Dashboard

### 4. Token refresh automation
- `AuthRepository.refreshTokens()` exists but isn't wired into an OkHttp Authenticator
- Add an `Authenticator` that auto-calls `refreshTokens()` on 401 responses

---

## What Android EXPECTS from Backend

### Scan Flow (3-step — BREAKING CHANGE from multipart)
```
Step 1: GET  /api/meals/scan/upload-url?contentType=image/jpeg  → UploadUrlResponse
Step 2: PUT  <uploadUrl>  (direct to S3, raw bytes — Android handles this, NOT via backend)
Step 3: POST /api/meals/scan  { imageKey, contentType }          → ScanResponse
```

### Key Expectations
- **Base URL (local dev):** `http://10.0.2.2:8098/` ← emulator → host, port 8098 (application-local.properties)
- **Base URL (prod):** TBD (EC2)
- All responses wrapped: `{ "status": "success", "data": {...} }` or `{ "status": "error", "error": { "code": "...", "message": "..." } }`
- JWT via `Authorization: Bearer <token>` (currently absent in dev — API returns 401, Room data shown)
- Dates as ISO strings: `"2026-04-24"`
- UUIDs as strings
- Null fields omitted (Jackson NON_NULL)
- Macros as `Double` in JSON (`42.0`)

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

### DELETE /api/meals/{id}
Must return `200 { "status": "success", "data": null }` — NOT 204 No Content.

### GET /api/meals/weekly
```json
{ "status": "success", "data": { "days": [{ "date": "2026-04-18", "totalCalories": 1850.0 }, ...] } }
```

---

## Shared Constants
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
| Backend local port | 8098 |
