# Caltrack Backend — Status & API Contract

> **Last updated:** 2026-04-11
> **Location:** `/Users/abhisheksharma/IdeaProjects/Caltrack/`
> **Stack:** Spring Boot 4.0.5, Java 21, Maven, MySQL 8, AWS (Bedrock, S3, Cognito)

---

## Build Status: COMPILED ✅

`mvn clean install -DskipTests` passes. Controllers added. Dockerfile created.

---

## What's DONE

### Infrastructure
| Layer | Files | Status |
|-------|-------|--------|
| `pom.xml` | Root | Done — Spring Boot 4.0.5, AWS SDK 2.31.24, Flyway, OAuth2 Resource Server, Lombok |
| `application.properties` | Config | Done — JWT, AWS, rate limit, multipart, actuator |
| `application-local.properties` | Local profile | Done — MySQL localhost, Redis, mock JWT issuer |
| `docker-compose.yml` | `/Caltrack/docker-compose.yml` | Done — MySQL 8.0 on port 3306 (user: caltrack / pass: caltrack_pass) |
| `V1__init_schema.sql` | Flyway migration | Done — 5 tables: user_profiles, daily_goals, user_allergies, meals, meal_items |

### Entities (JPA)
| Entity | Table | Key Fields |
|--------|-------|------------|
| `BaseEntity` | (MappedSuperclass) | UUID id, createdAt, updatedAt |
| `UserProfile` | user_profiles | cognitoSub, email, name, photoUrl, heightCm, weightKg, age, gender, active |
| `DailyGoal` | daily_goals | calorieGoal(2000), proteinGoal(150), carbsGoal(250), fatGoal(65) |
| `UserAllergy` | user_allergies | allergen (string), unique per user |
| `Meal` | meals | name, totalCalories/Protein/Carbs/Fat, imageKey, mealDate, mealTime, overallConfidence, aiRawResponse, userEdited, allergenWarnings |
| `MealItem` | meal_items | name, weightGrams, calories, protein, carbs, fat, confidence, containsAllergens |

### Repositories, DTOs, Mappers, Services, Security
*(All done — see original plan. No changes needed here.)*

| Service | Status |
|---------|--------|
| `UserService` | Done |
| `MealService` | Done — BUT `scanMeal()` currently takes a multipart file. **Needs refactor** (see Pending §1) |
| `MealAnalysisService` | Done — Bedrock Converse API, Claude Vision |
| `S3Service` | Done — `uploadImage`, `getPresignedUrl` (5 min TTL), `deleteImage`. **Needs new method:** `generateUploadPresignedUrl()` |
| `AllergyCheckService` | Done |

### Controllers
**UserController** (`/api/user`) — Done ✅
```
GET    /api/user/profile
PUT    /api/user/profile
DELETE /api/user/profile
GET    /api/user/goals
PUT    /api/user/goals
GET    /api/user/allergies
PUT    /api/user/allergies
GET    /api/user/export
```

**MealController** (`/api/meals`) — Partially Done ⚠️
```
POST   /api/meals/scan            ← BREAKING CHANGE: was multipart, now JSON body {imageKey, contentType}
POST   /api/meals
GET    /api/meals?date=YYYY-MM-DD
GET    /api/meals/{id}
PUT    /api/meals/{id}
DELETE /api/meals/{id}
GET    /api/meals/weekly
```
New endpoint needed:
```
GET    /api/meals/scan/upload-url?contentType=image/jpeg   ← NEW endpoint
```

---

## What's PENDING (Backend)

### 1. ⚠️ BREAKING CHANGE — Scan Flow Refactor (PRIORITY)

**Old flow (remove this):**
```
POST /api/meals/scan   multipart/form-data   image file → ScanResponse
```

**New flow (implement this):**
```
Step 1: GET  /api/meals/scan/upload-url?contentType=image/jpeg  → UploadUrlResponse
Step 2: (Android PUTs image directly to S3 using the presigned URL — backend not involved)
Step 3: POST /api/meals/scan   { "imageKey": "...", "contentType": "image/jpeg" }  → ScanResponse
```

**Why:** Android uploads large image files directly to S3, avoiding proxying through the backend. This reduces backend memory pressure, latency, and cost.

#### Step 1 — New endpoint: `GET /api/meals/scan/upload-url`

```java
// New DTO: UploadUrlResponse
{
  "uploadUrl":       "https://s3.amazonaws.com/bucket/key?X-Amz-...",  // presigned PUT URL
  "imageKey":        "meals/uuid-here.jpg",                             // key to use in Step 3
  "expiresInSeconds": 300
}
```

Implementation in `S3Service`:
```java
public UploadUrlResponse generateUploadPresignedUrl(String contentType) {
    String key = "meals/" + UUID.randomUUID() + ".jpg";
    // Use S3Presigner to create a presigned PUT URL with 5-min expiry
    // Content-Type must match what Android sends in Step 2
    return new UploadUrlResponse(presignedPutUrl, key, 300);
}
```

Rate limit: Apply same 10/min sliding window as before (on the `upload-url` endpoint, not on `/scan`).

#### Step 3 — Refactor `POST /api/meals/scan`

**New request body:**
```java
// New DTO: ScanRequest
{
  "imageKey":    "meals/uuid-here.jpg",   // required — key already uploaded to S3
  "contentType": "image/jpeg"             // optional, default "image/jpeg"
}
```

**Implementation in `MealService.scanMeal()`:**
```java
// Old: upload file to S3 (remove this)
// New: skip S3 upload — the key already exists, go straight to Bedrock
String imageKey = request.getImageKey();
// 1. Get the S3 object bytes (for Bedrock) — use S3Client.getObject(bucket, key)
// 2. Send to MealAnalysisService (Bedrock) — same as before
// 3. Run allergy check — same as before
// 4. Return ScanResponse (include imageKey in response)
```

**Remove:** `@RequestPart MultipartFile image` from the controller method.
**Remove:** multipart config from `application.properties` (or keep for other uses).
**Remove:** The `RateLimitFilter` on multipart POST — move rate limit to `GET /api/meals/scan/upload-url`.

#### ScanResponse — confirm these fields are present
```json
{
  "foodName": "Grilled Chicken Salad",
  "calories": 580,
  "protein": 42.0,
  "carbs": 18.0,
  "fat": 22.0,
  "confidence": 0.92,
  "items": [...],
  "allergenWarnings": ["Dairy"],
  "imageKey": "meals/uuid-here.jpg",   ← Android stores this in Room
  "notes": "..."
}
```

---

### 2. DELETE /api/meals/{id} — Response Body Confirmation

Android sends `Response<ApiEnvelope<Unit>>`.

**Required:** Return `200 {"status":"success","data":null}` (not 204 No Content).

Reason: Retrofit + Gson will try to parse the body. A 204 with no body causes `ApiEnvelope<Unit>` deserialization to return null, which Android handles OK — but for consistency with all other endpoints, return 200 with the success envelope.

Action: Verify the controller method returns `ResponseEntity.ok(ApiResponse.success(null))` or equivalent.

---

### 3. MealResponse — Add `imageUrl` (Presigned GET URL) Field

Android needs to display meal images from S3 in MealDetail and History screens. Since Android only stores `imageKey` (the S3 object key), the backend must provide a presigned GET URL.

**Add to `MealResponse`:**
```json
{
  "id": "...",
  "name": "...",
  "imageKey": "meals/uuid.jpg",
  "imageUrl": "https://s3.amazonaws.com/...?X-Amz-...&Expires=...",  ← NEW, nullable
  ...
}
```

**Implementation in `MealMapper.toMealResponse()`:**
```java
// If meal.getImageKey() != null:
String imageUrl = s3Service.getPresignedUrl(meal.getImageKey()); // existing method, 5-min TTL
response.setImageUrl(imageUrl);
```

**Also add `imageUrl` to `ScanResponse`** (presigned GET URL for the just-uploaded image).

Android's Coil image loader will use this URL directly. No changes needed on Android side.

---

### 4. MealRequest.date Field Name — Confirm

Android sends: `{ "date": "2026-04-11", ... }` (field name is `date`).

Verify `MealRequest` DTO has `@JsonProperty("date")` or a field named `date` (not `mealDate`).

---

### 5. JWT Auth — Dev/Test Mode

Android auth (Cognito) is not yet integrated. API calls arrive with NO Authorization header during dev/testing.

**Action:** Document how to generate a valid test JWT using the mock issuer in `application-local.properties`.

Example curl to get test token (fill in your mock issuer URL):
```bash
# With spring-security-test or Cognito local mock:
# Option: disable security for local profile (add @Profile("local") to SecurityConfig)
# OR: provide a static test JWT signed with the mock key
```

Suggestion: For local development, add a `@ConditionalOnProperty` or `@Profile("local")` security bypass that accepts a static header `X-Dev-User-Id: test-user-1` as the cognitoSub. Remove before prod.

---

### 6. Error Response Shape — Verify All Handlers

Android parses: `{"status":"error","error":{"code":"...","message":"..."}}`

Verify ALL exception handler methods in `GlobalExceptionHandler` return `ApiResponse.error(code, message)`. Jackson must serialize `ErrorDetail` as `{"code":"...","message":"..."}` (not `{"errorCode":...}`).

---

### 7. CORS — Confirm OPTIONS Preflight

Android emulator base URL: `http://10.0.2.2:8080`

`CorsConfig` sets `allowedOrigins("*")` — confirm this also handles OPTIONS preflight requests. Spring's `CorsConfigurationSource` bean typically handles this automatically when registered in the security filter chain.

---

### 8. GET /api/meals/weekly — Response Shape Confirmation

Android expects:
```json
{
  "status": "success",
  "data": {
    "days": [
      { "date": "2026-04-05", "totalCalories": 1850.0 },
      { "date": "2026-04-06", "totalCalories": 2100.0 },
      ...
    ]
  }
}
```

Only `date` and `totalCalories` per day are used by Android (WeeklyChart). Confirm `WeeklySummaryResponse` has this exact structure.

---

### 9. Testing
- Unit tests for services (mock repos)
- Integration tests for controllers (MockMvc + H2 or Testcontainers)

---

## Full API Contract (Final — What Android Calls)

### Base URL
- **Local dev (emulator):** `http://10.0.2.2:8080`
- **Production:** TBD (EC2)

### Authentication
```
Authorization: Bearer <cognito_jwt_token>
```
All endpoints require this header except: actuator health.

### Response Envelope (all responses)
```json
{ "status": "success", "data": { ... } }
{ "status": "error",   "error": { "code": "NOT_FOUND", "message": "..." } }
```

### Full Endpoint List (what Android's Retrofit calls)

```
# Scan (3-step — see §1 above)
GET  /api/meals/scan/upload-url?contentType=image/jpeg  → UploadUrlResponse
POST /api/meals/scan                                     → ScanResponse      body: {imageKey, contentType}

# Meals CRUD
POST   /api/meals                     → MealResponse      body: MealRequest
GET    /api/meals?date=YYYY-MM-DD     → List<MealResponse>
GET    /api/meals/{id}                → MealResponse
PUT    /api/meals/{id}                → MealResponse      body: MealRequest
DELETE /api/meals/{id}                → 200 {status:success, data:null}
GET    /api/meals/weekly              → WeeklySummaryResponse

# User Profile
GET  /api/user/profile                → ProfileResponse
PUT  /api/user/profile                → ProfileResponse   body: ProfileRequest
GET  /api/user/goals                  → GoalResponse
PUT  /api/user/goals                  → GoalResponse      body: GoalRequest
GET  /api/user/allergies              → AllergyResponse
PUT  /api/user/allergies              → AllergyResponse   body: AllergyRequest
```

### Key Data Types
- All IDs: UUID strings
- Dates: ISO `"2026-04-07"`
- Macros: `Double` in JSON (`42.0`), Android converts to `Int` for display
- Confidence: `0.0–1.0`
- `null` fields omitted (Jackson `NON_NULL`)

### Error Codes
| Code | HTTP |
|------|------|
| `VALIDATION_ERROR` | 400 |
| `BAD_REQUEST` | 400 |
| `NOT_FOUND` | 404 |
| `FORBIDDEN` | 403 |
| `RATE_LIMIT_EXCEEDED` | 429 |
| `FILE_TOO_LARGE` | 413 |
| `ANALYSIS_FAILED` | 500 |
| `INTERNAL_ERROR` | 500 |
