# Caltrack Backend — Session Context

> **Last updated:** 2026-04-24
> **Location:** `/Users/abhisheksharma/IdeaProjects/Caltrack/`
> **Repo:** `https://github.com/Abhishek00018/CalTrack` (branch: `main`)
> **Stack:** Spring Boot 4.0.5, Java 21, Maven, MySQL 8, AWS (Bedrock, S3, Cognito)

---

## Build Status: COMPILED ✅
`mvn clean install -DskipTests` — BUILD SUCCESS

---

## Git State

```
d71f5aa  Initial caltrack backend APIs          ← base (has credentials in history — already on GitHub)
2468451  chore: remove hardcoded credentials
6117006  Merge pull request #1 (fix/security-guardrails merged)
2f2df07  Removed application yml file           ← current HEAD / origin/main
```

- **Current branch:** `main`, clean, up to date with `origin/main`
- **⚠️ Uncommitted change on disk:** `AllergyCheckService.java` has a broken edit —
  a `ScheduledExecutorService` field + stray `scheduleAtFixedRate` block added inside
  `checkItemAllergens()`. Likely accidental. Must be reverted or properly completed before next commit.
  Quick fix: `git checkout -- src/main/java/com/caltrack/service/AllergyCheckService.java`

---

## Original Plan (This Session Covered Everything Below)

| Area | Goal | Status |
|------|------|--------|
| Controllers | UserController + MealController | ✅ Done |
| Build | First successful `mvn clean install` | ✅ Done |
| Dockerfile | Multi-stage Maven → JRE 21 alpine | ✅ Done |
| Scan flow | Refactor from multipart → presigned S3 upload | ✅ Done |
| AWS region | Fix from US placeholders to ap-south-1 (Mumbai) | ✅ Done |
| Security | Remove hardcoded credentials, gitignore rules, pre-commit hook | ✅ Done |
| Git hygiene | Clean branch history, proper PR workflow | ✅ Done |

---

## What Is DONE (All on main)

### Infrastructure
| File | Notes |
|------|-------|
| `pom.xml` | Spring Boot 4.0.5, AWS SDK 2.31.24, Flyway, OAuth2 Resource Server, Lombok |
| `application.properties` | AWS region `ap-south-1`, Cognito JWT, rate limit, actuator |
| `application-local.properties` | **Gitignored** — uses `${DB_USERNAME}` / `${DB_PASSWORD}` env vars. Port: 8098 |
| `application-local.properties.example` | Safe template committed — copy and fill in values |
| `application.yml` | **Deleted** (removed in commit 2f2df07) |
| `docker-compose.yml` | MySQL 8.0 on 3306, user: caltrack / pass: caltrack_pass |
| `V1__init_schema.sql` | 5 tables: user_profiles, daily_goals, user_allergies, meals, meal_items |
| `Dockerfile` | Multi-stage Maven build → JRE 21 alpine, exposes 8080 |
| `.gitignore` | Blocks credentials, keys, certs, env files, docker overrides, logs |
| `.git/hooks/pre-commit` | Blocks hardcoded passwords, AWS keys, cert files on every `git commit` |

### Entities, Repositories, Mappers — unchanged, all done
`BaseEntity`, `UserProfile`, `DailyGoal`, `UserAllergy`, `Meal`, `MealItem`
All repositories, `UserMapper`, `MealMapper`

### DTOs
**Request:** `ProfileRequest`, `GoalRequest`, `AllergyRequest`, `MealRequest`, `MealItemRequest`,
`ScanRequest` ← NEW `{ imageKey, contentType }`

**Response:** `ApiResponse<T>`, `ProfileResponse`, `GoalResponse`, `AllergyResponse`, `MealResponse`,
`MealItemResponse`, `ScanResponse`, `WeeklySummaryResponse`, `UserExportResponse`,
`UploadUrlResponse` ← NEW `{ uploadUrl, imageKey, expiresInSeconds }`

### Services
| Service | Status |
|---------|--------|
| `UserService` | Full CRUD — profile, goals, allergies, GDPR export/delete |
| `MealService` | `getUploadUrl(sub, contentType)` + `scanMeal(sub, imageKey, contentType)` + CRUD + weekly |
| `MealAnalysisService` | Bedrock Converse API, Claude Vision, structured JSON prompt, response parser |
| `S3Service` | `generateUploadPresignedUrl(userId, contentType)`, `downloadImage(key)`, `getPresignedUrl(key)`, `deleteImage(key)` |
| `AllergyCheckService` | Keyword-based, 10 allergens — **⚠️ broken edit on disk, see Git State above** |

### Controllers
**`UserController`** (`/api/user`) — all 8 endpoints ✅
**`MealController`** (`/api/meals`) — all 7 endpoints ✅

### Scan Flow (Presigned S3 — no multipart through backend)
```
Step 1: GET  /api/meals/scan/upload-url?contentType=image/jpeg
             ← { uploadUrl (presigned S3 PUT), imageKey, expiresInSeconds }

Step 2: Android PUTs image bytes directly to S3 via uploadUrl (backend not involved)

Step 3: POST /api/meals/scan
             body: { "imageKey": "meals/<profileId>/<uuid>.jpg", "contentType": "image/jpeg" }
             ← ScanResponse
             Backend: S3 GetObject → bytes → Bedrock → allergy check → return
```
- S3 keys scoped per user: `meals/<profileId>/<uuid>.<ext>`
- Key ownership validated on scan (key must start with `meals/<profileId>/`)
- Rate limit: 10/min on BOTH `GET /scan/upload-url` AND `POST /scan`

### Security
- `SecurityConfig` — stateless JWT, Cognito groups → ROLE_, actuator public
- `CorsConfig` — all origins, standard methods, Authorization header exposed
- `AwsConfig` — all three AWS clients wired with `ap-south-1`
- `RateLimitFilter` — sliding window 10/min on scan endpoints
- `GlobalExceptionHandler` — all errors return `ApiResponse.error(code, msg)` envelope

---

## What Is PENDING (Next Session Start Here)

### P0 — Fix broken `AllergyCheckService.java` edit
```bash
git checkout -- src/main/java/com/caltrack/service/AllergyCheckService.java
```
Or complete the cache invalidation properly if it was intentional.

---

### P1 — DELETE /api/meals/{id} returns 204, Android expects 200
**File:** `MealController.java`, method `deleteMeal()`

Current:
```java
return ResponseEntity.noContent().build();  // 204 — no body
```
Required (Android `Response<ApiEnvelope<Unit>>` needs a parseable body):
```java
return ResponseEntity.ok(ApiResponse.success(null));  // 200 with envelope
```

---

### P2 — Add `imageUrl` (presigned GET) to `MealResponse` and `ScanResponse`
Android stores `imageKey` in Room but needs a presigned URL to display images via Coil.

**`MealResponse.java`** — add field:
```java
private String imageUrl;  // nullable, presigned GET URL, ~5 min TTL
```

**`MealMapper.java`** — inject `S3Service`, compute URL:
```java
// In toMealResponse():
if (meal.getImageKey() != null) {
    response.setImageUrl(s3Service.getPresignedUrl(meal.getImageKey()));
}
```

**`ScanResponse.java`** — add same `imageUrl` field, set it in `MealService.scanMeal()` after analysis.

`S3Service.getPresignedUrl(key)` already exists — just needs wiring.

---

### P3 — Local Dev JWT Bypass (so Android can test without Cognito)
All endpoints return 401 in local dev because Android sends no token yet (Cognito not integrated).

**Add to `SecurityConfig` or a new `@Configuration @Profile("local")`:**
```java
// Permit all requests on local profile, inject a fixed cognitoSub
// Option: accept header X-Dev-Sub: test-user-1 as the principal
// Gate strictly to spring.profiles.active=local
```

---

### P4 — Verify `MealRequest` date field name
Android sends `"date"` (not `"mealDate"`).
Check `MealRequest.java` has field `date` or `@JsonProperty("date")` on `mealDate`.

---

### P5 — Unit + Integration Tests
- Unit tests: `MealService`, `UserService`, `AllergyCheckService` (mock repos)
- Integration tests: controllers with MockMvc + Testcontainers (MySQL) or H2
- Nothing written yet

---

### P6 — Production Readiness (deferred until EC2/Cognito setup)
- Set real Cognito env vars: `AWS_COGNITO_ISSUER_URI`, `AWS_COGNITO_USER_POOL_ID`, `AWS_COGNITO_CLIENT_ID`
- Confirm Bedrock model ID for `ap-south-1` — current default `us.anthropic.claude-sonnet-4-6`
  uses a US cross-region inference prefix. May need `ap.anthropic.claude-...` or specific regional model.
- ECR push + EC2 deploy
- HTTPS (ALB or nginx in front)

---

## Full API Contract

### Base URLs
- **Local dev (emulator):** `http://10.0.2.2:8098` ← port 8098 (application-local.properties)
- **Production:** TBD (EC2)

### Auth
```
Authorization: Bearer <cognito_jwt_token>
```
All endpoints require this. Exception: `GET /actuator/health`

### Response Envelope
```json
{ "status": "success", "data": { ... } }
{ "status": "error",   "error": { "code": "NOT_FOUND", "message": "..." } }
```

### Endpoints
```
# Scan (3-step presigned flow)
GET    /api/meals/scan/upload-url?contentType=  → UploadUrlResponse
POST   /api/meals/scan                          → ScanResponse         body: ScanRequest

# Meals CRUD
POST   /api/meals                               → MealResponse         body: MealRequest
GET    /api/meals?date=YYYY-MM-DD              → List<MealResponse>
GET    /api/meals/{id}                          → MealResponse
PUT    /api/meals/{id}                          → MealResponse         body: MealRequest
DELETE /api/meals/{id}                          → 200 ApiResponse      ← P1: currently returns 204
GET    /api/meals/weekly                        → WeeklySummaryResponse

# User
GET    /api/user/profile                        → ProfileResponse
PUT    /api/user/profile                        → ProfileResponse      body: ProfileRequest
DELETE /api/user/profile                        → 204 (soft delete)
GET    /api/user/goals                          → GoalResponse
PUT    /api/user/goals                          → GoalResponse         body: GoalRequest
GET    /api/user/allergies                      → AllergyResponse
PUT    /api/user/allergies                      → AllergyResponse      body: AllergyRequest
GET    /api/user/export                         → UserExportResponse
```

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

### Data Conventions
- IDs: UUID strings
- Dates: ISO `"2026-04-24"`
- Confidence: `Double` `0.0–1.0`
- Macros: `Double` in JSON (`42.0`)
- `null` fields omitted (Jackson `NON_NULL`)
- Credentials: **never hardcoded** — use `DB_USERNAME` / `DB_PASSWORD` env vars

---

## How to Run Locally

```bash
# Set env vars (never put these in a file that git tracks)
export DB_USERNAME=root
export DB_PASSWORD=<your_mysql_password>

# Start MySQL (docker)
docker-compose up -d

# Run backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
# Runs on port 8098
```
