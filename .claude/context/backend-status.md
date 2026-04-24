# Caltrack Backend — Status & API Contract

> **Last updated:** 2026-04-24
> **Location:** `/Users/abhisheksharma/IdeaProjects/Caltrack/`
> **Stack:** Spring Boot 4.0.5, Java 21, Maven, MySQL 8, AWS (Bedrock, S3, Cognito)

---

## Build Status: COMPILED ✅

`mvn clean install -DskipTests` passes. Controllers added. Dockerfile created.

---

## Original Plan

| Phase | Goal | Status |
|-------|------|--------|
| 1 | Core infrastructure (DB schema, entities, Flyway, security) | ✅ DONE |
| 2 | All controllers, services, DTOs, S3, Bedrock analysis | ✅ DONE (scan needs refactor) |
| 3 | Scan flow refactor (multipart → 3-step S3), response fixes | ⏳ PENDING (Android is waiting) |
| 4 | Testing, prod deploy (EC2 + RDS + ECR) | ⏳ PENDING |

---

## What's DONE

### Infrastructure
| Layer | Files | Status |
|-------|-------|--------|
| `pom.xml` | Root | Done — Spring Boot 4.0.5, AWS SDK 2.31.24, Flyway, OAuth2 Resource Server, Lombok |
| `application.properties` | Config | Done — JWT, AWS, rate limit, multipart, actuator |
| `application-local.properties` | Local profile | Done — MySQL localhost, port **8098**, mock JWT issuer |
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

### Services
| Service | Status |
|---------|--------|
| `UserService` | Done |
| `MealService` | Done — BUT `scanMeal()` still takes multipart file. **Must refactor** (see Pending §1) |
| `MealAnalysisService` | Done — Bedrock Converse API, Claude Vision (`us.anthropic.claude-sonnet-4-6`) |
| `S3Service` | Done — `uploadImage`, `getPresignedUrl` (5-min GET TTL), `deleteImage`. **Needs `generateUploadPresignedUrl()`** |
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
POST   /api/meals/scan            ← BREAKING: was multipart, must become JSON {imageKey, contentType}
POST   /api/meals
GET    /api/meals?date=YYYY-MM-DD
GET    /api/meals/{id}
PUT    /api/meals/{id}
DELETE /api/meals/{id}            ← must return 200 {status:success, data:null}, NOT 204
GET    /api/meals/weekly
```
New endpoint needed:
```
GET    /api/meals/scan/upload-url?contentType=image/jpeg   ← MUST ADD
```

---

## What's PENDING (Backend) — Priority Order

### 1. ⚡ BREAKING — Scan Flow Refactor (Android is blocked until this is done)

Android has **already been updated** to use the 3-step flow. Backend must match.

**Remove:**
- `@RequestPart MultipartFile image` from `POST /api/meals/scan`
- multipart config in `application.properties` (or leave for other uses)
- Rate limit on the old multipart endpoint

**Add — `GET /api/meals/scan/upload-url`:**
```java
// New DTO: UploadUrlResponse
{ "uploadUrl": "https://s3.../key?X-Amz-...", "imageKey": "meals/uuid.jpg", "expiresInSeconds": 300 }

// In S3Service:
public UploadUrlResponse generateUploadPresignedUrl(String contentType) {
    String key = "meals/" + UUID.randomUUID() + ".jpg";
    // S3Presigner → presigned PUT URL, 5-min expiry, Content-Type must match
    return new UploadUrlResponse(presignedPutUrl, key, 300);
}
```
Rate limit: 10/min (same as before, move to this endpoint).

**Refactor — `POST /api/meals/scan` (now JSON body):**
```java
// New DTO: ScanRequest
{ "imageKey": "meals/uuid.jpg", "contentType": "image/jpeg" }

// In MealService.scanMeal():
// OLD: upload file to S3 (remove)
// NEW: imageKey already in S3 — fetch bytes via S3Client.getObject(bucket, key)
//      → send to MealAnalysisService (Bedrock) — unchanged
//      → run allergy check — unchanged
//      → return ScanResponse (include imageKey + imageUrl)
```

**ScanResponse — confirm all these fields:**
```json
{
  "foodName": "Grilled Chicken Salad",
  "calories": 580,
  "protein": 42.0, "carbs": 18.0, "fat": 22.0,
  "confidence": 0.92,
  "items": [{ "name": "...", "calories": 200, "protein": 15.0, "carbs": 5.0, "fat": 8.0, "weightGrams": 150 }],
  "allergenWarnings": ["Dairy"],
  "imageKey": "meals/uuid-here.jpg",
  "imageUrl": "https://s3.amazonaws.com/...?X-Amz-...",
  "notes": "..."
}
```

---

### 2. DELETE /api/meals/{id} — Response Body
Android sends `Response<ApiEnvelope<Unit>>` — expects a body.

**Required:** Return `200 {"status":"success","data":null}` (NOT 204 No Content).

Fix: `return ResponseEntity.ok(ApiResponse.success(null));`

---

### 3. MealResponse — Add `imageUrl` Field
Android needs presigned GET URL to display meal images in MealDetail and History.

**Add to `MealResponse`:**
```json
{ "imageKey": "meals/uuid.jpg", "imageUrl": "https://s3.amazonaws.com/...?X-Amz-...", ... }
```

**In `MealMapper.toMealResponse()`:**
```java
if (meal.getImageKey() != null) {
    response.setImageUrl(s3Service.getPresignedUrl(meal.getImageKey())); // existing method, 5-min TTL
}
```

Also add `imageUrl` to `ScanResponse` (presigned GET URL for the just-uploaded image).

---

### 4. MealRequest.date Field Name
Android sends: `{ "date": "2026-04-24", ... }`

Verify `MealRequest` DTO uses field name `date` (not `mealDate`). Add `@JsonProperty("date")` if needed.

---

### 5. JWT Auth — Dev/Test Mode
Android Cognito is not yet integrated. API calls arrive with NO `Authorization` header during dev.

Options (pick one):
- Add `@Profile("local")` security bypass that accepts `X-Dev-User-Id: test-user-1` as cognitoSub
- Disable auth entirely for local profile (`@ConditionalOnProperty`)
- Document how to generate a test JWT from the mock issuer in `application-local.properties`

**Remove before prod.**

---

### 6. Error Response Shape — Verify All Handlers
Android parses: `{"status":"error","error":{"code":"...","message":"..."}}`

Verify ALL handlers in `GlobalExceptionHandler` return `ApiResponse.error(code, message)` with `ErrorDetail` serialized as `{"code":"...","message":"..."}` (not `{"errorCode":...}`).

---

### 7. CORS — Confirm OPTIONS Preflight
Android emulator base URL: `http://10.0.2.2:8098`

`CorsConfig.allowedOrigins("*")` — confirm OPTIONS preflight requests are handled (Spring's `CorsConfigurationSource` in the security filter chain typically handles this automatically).

---

### 8. GET /api/meals/weekly — Response Shape
Android's Vico weekly chart expects exactly:
```json
{ "status": "success", "data": { "days": [{ "date": "2026-04-18", "totalCalories": 1850.0 }, ...] } }
```
Only `date` + `totalCalories` per day are consumed. Confirm `WeeklySummaryResponse` matches.

---

### 9. Testing
- Unit tests for services (mock repositories)
- Integration tests for controllers (MockMvc + H2 or Testcontainers)

---

### 10. Prod Deploy
- Dockerize + push to ECR
- Deploy to EC2, RDS MySQL, S3 bucket policy
- Update Android `BASE_URL` for production

---

## Full API Contract (Final — What Android Calls)

### Base URL
- **Local dev (emulator → host):** `http://10.0.2.2:8098` ← port 8098 (application-local.properties)
- **Production:** TBD (EC2)

### Authentication
```
Authorization: Bearer <cognito_jwt_token>
```
All endpoints require this header. (Currently unenforced in local dev — see §5 above.)

### Response Envelope (all responses)
```json
{ "status": "success", "data": { ... } }
{ "status": "error",   "error": { "code": "NOT_FOUND", "message": "..." } }
```

### Full Endpoint List
```
# Scan (3-step)
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
- Dates: ISO `"2026-04-24"`
- Macros: `Double` in JSON (`42.0`)
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
