# PROJECT TECHNICAL CONTRACT & ARCHITECTURAL INVARIANTS

> **Scope & Authority**: Canonical frontend-facing project contract derived from executable backend evidence. (Executable Java source code remains the primary authority).

---

## 1. System Identity & Stack Baseline

- **Application Domain**: Chinese Kangxi Radicals & Vocabulary Learning Platform with Spaced Repetition (SRS SM-2).
- **Frontend Architecture**:
  - Semantic HTML5.
  - CSS3 utilizing custom design tokens (typography, color roles, spacing scale).
  - Vanilla JavaScript (ES6+ modular, native `fetch()`, zero-build complexity, no SPA framework):
    - `frontend/js/api/api.js`: Central HTTP API client (`ApiResponse<T>`, 401 redirect, 429 backoff, `AbortController` timeout, GET-only retry).
    - `frontend/js/auth/auth-state.js`: Client-side session and auth state (`localStorage` untrusted convenience storage, role checking without `ROLE_` prefix).
    - `frontend/js/ui/ui.js` & `frontend/js/ui/security.js`: Shared UI primitives (3-state UI, modals, toasts) and safe DOM sanitizers (`textContent` enforcement).
    - `frontend/js/pages/*-page.js`: Page-level controllers (one ES module per HTML page).
    - `frontend/js/app.js`: Core shell initialization.
  - Bootstrap 5.3 (via CDN) strictly as a supporting utility tool for grid layouts and accessible modal/toast primitives; **Bootstrap default styling does NOT determine the product visual identity**.
- **Backend Architecture**:
  - Java 21 LTS, Spring Boot 3.3.5.
  - Spring Security 6 with stateless JWT Bearer token authentication.
  - Spring Data JPA with Hibernate ORM.
  - MySQL 8.4 InnoDB engine (`utf8mb4_unicode_ci`).
  - Flyway database migration management (V1 $\rightarrow$ V7 clean baseline).
  - API Surface Inventory:
    - 15 Controller classes in `com.elearning.controller`.
    - 48 REST endpoint handler methods across the 15 controllers under `/api/v1/*`.
    - 49 HTTP method+path mappings (due to dual PUT/POST `@RequestMapping` on `/{id}/reorder`).
    - 1 Health / diagnostic probe (`GET /actuator/health`). (Historical mentions of "45 endpoints" reflect pre-Module 8D audit scope).
  - Standardized JSON envelope: `ApiResponse<T>` (`code`, `message`, `data`, `errors[]`).
  - Automated Test Baseline: 1101 tests PASS (0 failures, 0 errors, 0 skipped; ~04:38 min on MySQL 8.4 Testcontainers). Historical milestone snapshots: 775, 881, 948, 1051, 1088. Note per OWASP: test pass rate confirms regression stability against current test specifications, but is not alone sufficient proof of application security.
  - Backend Release Status & Freeze: **SEALED under DEC-41** (1101/1101 tests PASS, V1..V7 clean, 49 HTTP mappings / 48 handlers verified).
    - **Explicit Exception to Backend Seal**: As established in DEC-40 and DEC-41, Content-Security-Policy (CSP) and any client-facing HTTP response security headers (in `SecurityConfig.java`, `application.yml`, or reverse-proxy Nginx configuration) were deliberately deferred to Phase 9 (Task 9G.3) and Phase 11 until the client-side resource graph (scripts, styles, CDNs, fonts, media) is finalized. Tuning or configuring CSP/security headers during Phase 9/11 is an explicit, pre-planned exception and does NOT violate the backend release seal.

---

## 2. The 4 Role-Based Access Control (RBAC) Surfaces

The application implements four distinct user surfaces based on backend authorities:

> [!IMPORTANT]
> **Role Layer Distinction & JSON Contract**:
> - **Database Schema**: `Admin`, `Creator`, `Moderator`, `Learner` (`V2__seed_roles.sql`)
> - **Backend JSON DTO**: `["Admin"]`, `["Creator"]`, `["Moderator"]`, `["Learner"]` (`AuthResponse.roles`, JWT claim `roles`). **No `ROLE_` prefix in JSON!**
> - **Spring Security**: `ROLE_Admin`, `ROLE_Creator`, `ROLE_Moderator`, `ROLE_Learner` (internal authority representation)
> - **Frontend Checks**: Client MUST check `roles.includes('Admin')` or `stateManager.hasRole('Admin')`. **NEVER check `ROLE_ADMIN` in frontend code.**

1. **`ROLE_LEARNER` (Learner Surface — JSON Role: `"Learner"`)**:
   - Public Kangxi radical catalog exploration (214 radicals).
   - Multi-criteria vocabulary search (Hanzi, Pinyin with tones, Pinyin without tones `pinyin_raw` via `GET /api/v1/vocabulary?search=`).
   - Public approved lesson discovery (`GET /api/v1/lessons`).
   - Contextual personal notes on vocabulary items.
   - Interactive SRS flashcard review sessions and daily study stats.
   - Personal user profile management (`GET/PUT /api/v1/users/profile`).
   - Personalized SRS study settings configuration (`GET/PUT /api/v1/srs/settings`).
2. **`ROLE_CREATOR` (Creator Studio — JSON Role: `"Creator"`)**:
   - Personal lesson authoring, draft editing, and vocabulary sequencing (`order_index`).
   - Adding vocabulary via `POST /{id}/vocabularies/{vocabId}` automatically assigns next `orderIndex`; client does not send `orderIndex`. Reordering uses `/{id}/reorder` with `orderedVocabIds` or `items`.
   - Two-step Excel import workflow.
   - Submission of draft/rejected lessons for moderation (`Draft`/`Rejected` $\rightarrow$ `Pending`).
3. **`ROLE_MODERATOR` (Moderation Queue — JSON Role: `"Moderator"`)**:
   - Inspection queue of `Pending` lesson submissions.
   - Auditing vocabulary items and pronunciation details.
   - Approval (`Approved`) or Rejection (`Rejected`) requiring mandatory `rejectionReason` ($\le 500$ chars) and structured `flaggedFields` JSON.
   - Immutable audit history query (`MODERATION_LOG`).
4. **`ROLE_ADMIN` (System Administration & Oversight — JSON Role: `"Admin"`)**:
   - Account lifecycle governance (filtering, search, setting status `Active`, `Inactive`, `Banned`).
   - Dynamic system role assignment (`Learner`, `Creator`, `Moderator`, `Admin`).
   - Master catalog CRUD for Kangxi radicals and vocabulary items (browsing uses public `GET /api/v1/radicals` and `GET /api/v1/vocabulary?search=`; there are no separate admin GET endpoints).
   - System-wide lesson monitoring across all authors and statuses.

---

## 3. Core Architectural & Security Invariants

These invariants are non-negotiable correctness and security baselines:

### Invariant 1: Identity, Authorization Authority & Client Storage Boundaries
- **User Identity is Session-Bound**: The client MUST NEVER supply `userId` or `accountId` in request bodies or query parameters to declare user identity. Identity is derived strictly on the backend from the validated JWT Bearer token (`SecurityContextHolder`).
- **Unified Identifier (`emailOrPhone`) & Password Policy**: Login (`POST /api/v1/auth/login`) and Registration (`POST /api/v1/auth/register`) use a single unified identifier field `emailOrPhone` (String, max 191 chars), allowing users to authenticate via email address or phone number. Registration enforces password length between 6 and 100 characters (`@Size(min = 6, max = 100)` in `RegisterRequest.java`).
- **Client Storage is Untrusted for Authorization**: `localStorage` is client-controlled convenience storage, NOT "secure storage". Stored `access_token`, `user_info`, and role strings MUST be treated as untrusted for security enforcement.
- **Frontend Role Checks Are For UX Only**: Client-side role inspection is strictly for conditional UI rendering (hiding/showing navigation links). The Spring Boot backend (`@PreAuthorize`, `SecurityFilterChain`) is the SOLE AUTHORITATIVE authorization boundary.
- **No Credential Logging**: Passwords, secrets, and raw JWT tokens MUST NOT be logged to `console.log` or telemetry channels.
- **AuthResponse JSON Serialization Warning (`AUTHORITATIVE`)**: `AuthResponse.java` has a Java field named `token` with `@JsonAlias("accessToken")`. However, `@JsonAlias` only affects **deserialization** (reading JSON into Java). The serialized JSON response field is `"token"` (NOT `"accessToken"`). Verified by `AuthIntegrationTests.java:89` — `jsonPath("$.data.token").isString()`. Frontend MUST use `response.data.token` to extract the JWT.
- **User Profile Roles Boundary**: `UserProfileResponse` (`GET /api/v1/users/profile`) contains personal profile details (`fullName`, `emailOrPhone`, `avatarUrl`, etc.) but has NO `roles` field. User roles are delivered exclusively via `AuthResponse.roles` upon authentication and managed in client-side session state (`authManager`). The frontend profile UI (`profile.html`) and navigation bars must render roles from the active session state, never by expecting them from the profile endpoint.
- **Runtime Deployment Config Boundary**: A global object `window.__ENV__` is permitted EXCLUSIVELY for read-only deployment configuration (e.g., `window.__ENV__?.API_BASE_URL`). Application state, auth state, and user data MUST NOT be stored on `window`. `window.__ENV__` is NOT a security boundary.

### Invariant 2: Server-Side Token Invalidation & CORS Boundaries
- The database schema enforces an `authorization_version` column on the `ACCOUNT` table.
- When an account's status changes (`Active` $\rightarrow$ `Inactive`/`Banned`) or roles are modified, the backend increments `authorization_version` and commits the transaction.
- The JWT payload contains the version claim (`auth_ver`, matching `JwtUtil.CLAIM_AUTH_VER = "auth_ver"`).
- `JwtAuthenticationFilter` validates `auth_ver` against the database; any mismatch or inactive account status results in an immediate **HTTP 401 Unauthorized**.
- The frontend client MUST intercept HTTP 401, purge local credentials, and redirect to the login interface.
- **JWT Lifetime & Revocation Lifecycle**: JWTs have a 24-hour validity (`jwt.expiration-ms: 86400000`). The backend does NOT provide a refresh-token endpoint and does NOT provide a server-side `/logout` endpoint. Client logout is strictly local state cleanup (`localStorage.removeItem('access_token')`). Server-side revocation is enforced solely via `authorization_version` (DEC-42).
- **CORS Configuration (Development vs Production)**:
  - *Development Configuration*: Backend defaults to allowing origins `http://localhost:5500`, `http://127.0.0.1:5500`, `http://localhost:3000`, and `http://127.0.0.1:3000` via `${APP_CORS_ALLOWED_ORIGINS:...}` (`SecurityConfig.java:99`).
  - *Production Deployment Invariant*: Production CORS origins are set via the `APP_CORS_ALLOWED_ORIGINS` environment variable. The frontend architecture must not hardcode development localhost origins as an architectural requirement; frontend assets must be served from origins permitted by deployment policy or via reverse proxy.

### Invariant 3: Two-Step Excel Import Boundary (Preview $\ne$ Confirm)
- **Step 1 — Preview (`POST /api/v1/creator/lessons/import`)**:
  - Accepts `multipart/form-data` with `@RequestParam("file") MultipartFile file` (.xlsx file, max 10MB; exceeding 10MB returns HTTP 413 `FILE_TOO_LARGE`).
  - Maximum data rows: 5,000 (`MAX_DATA_ROWS = 5000` excluding header row) per OWASP API4:2023 DoS prevention.
  - Validates row structure, Hanzi presence, Pinyin tone rules, and vocabulary duplicates. Supports Vietnamese and ASCII header aliases.
  - Returns `ApiResponse<ImportValidationReport>` (HTTP 200 OK) even if rows contain validation errors (`isValid: false`, `fileStatus: "INVALID"`).
  - **GUARANTEE**: Step 1 performs **ZERO database mutations** (Zero DB mutations invariant).
- **Step 2 — Confirm (`POST /api/v1/creator/lessons/import/confirm`)**:
  - Consumes `multipart/form-data` with `@RequestParam("title") String title` and `@RequestParam("file") MultipartFile file`.
  - Title constraint: **max 100 characters** in Excel confirm (`CreatorLessonServiceImpl:383-384`), distinct from manual lesson creation where `CreateLessonRequest.title` allows **max 200 characters**.
  - Executed ONLY after user reviews preview and confirms with a valid lesson title.
  - Performs an atomic `@Transactional` persistence creating the `Lesson` ('Draft') and constituent vocabulary items preserving Excel row order.
  - Returns HTTP 201 Created with `ApiResponse<LessonDetailResponse>`. Client redirects using `lessonDetail.lessonId` (NOT `lessonDetail.id`).

### Invariant 4: Spaced Repetition System (SRS SM-2 Variant)
- SM-2 variant maps user feedback ratings 1 to 4:
  - `1`: Again ($q=0$, interval reset to 0 days, card re-queued within session).
  - `2`: Hard ($q=3$).
  - `3`: Good ($q=4$).
  - `4`: Easy ($q=5$).
- Due-Card Enforcement: Backend rejects premature reviews (`nextReviewAt > now`) with **HTTP 409 Conflict**.
- Due Cards Query: `GET /api/v1/srs/due` retrieves active cards due for review (`List<DueCardResponse>`).
  - **`DueCardResponse.strokeCount` is always explicit `null`** (hardcoded on backend for both Vocabulary and Radical). Frontend MUST NOT attempt to use `DueCardResponse.strokeCount` for stroke filtering.
  - `DueCardResponse` does NOT contain `audioUrl`, `videoWritingUrl`, `pinyinRaw`, or `radicals[]`. The client must query detail endpoints (`GET /api/v1/vocabulary/{id}` or `GET /api/v1/radicals/{id}`) if rich media/decomposition is displayed.
- New Card Eligibility: A vocabulary item can only become a new SRS card if it belongs to at least one `Approved` lesson (`GET /api/v1/srs/new-cards?lessonId={id}` or `GET /api/v1/srs/lessons/{id}/new-cards`).
- Learning Mutation Boundary: Submitting review to `POST /api/v1/srs/review` requires payload `{ itemType, itemId, rating, reviewTimeSeconds }` (in camelCase, where `itemType` is "VOCABULARY" or "RADICAL", `itemId` is Long, `rating` is 1..4, and `reviewTimeSeconds` is integer $\ge 0$) for cognitive performance auditing.

### Invariant 5: API Envelope & Error Normalization
- Most business API responses use `ApiResponse<T>`. Endpoint-specific response behavior takes precedence. HTTP 204 responses contain no body and therefore do not contain `ApiResponse<T>`.
  ```json
  {
    "code": "SUCCESS",
    "message": "Thông điệp phản hồi",
    "data": { ... },
    "errors": []
  }
  ```
- Validation errors (HTTP 400) populate `errors[]` as `List<String>`, with each item formatted as `"${field}: ${message}"` (e.g. `"emailOrPhone: Email không hợp lệ"` or `"title: Tiêu đề không được để trống"`), generated by `GlobalExceptionHandler`. Frontend form handlers parse this string format using canonical helper `parseFieldErrors()`.
- Rate limiting errors (HTTP 429) return `code: "TOO_MANY_REQUESTS"`. The backend does NOT emit a `Retry-After` header; clients must not depend on this header.
- Error responses MUST NOT leak Java stack traces, database table schemas, or internal server paths.

### Invariant 6: Authoritative Pagination Contract (`PageResponse<T>`)
- Paged catalog endpoints return `PageResponse<T>` within the `ApiResponse<T>.data` payload:
  ```json
  {
    "page": 0,
    "size": 20,
    "totalElements": 214,
    "totalPages": 11,
    "items": [ ... ]
  }
  ```
- **REST DTO Contract Authoritative (`PROJECT INVARIANT`)**: The serialized JSON keys (`page`, `size`, `totalElements`, `totalPages`, `items`) are the authoritative frontend contract. Frontend code MUST NOT assume or infer internal Spring Data `Page` field names (such as `number` or `content`).
- Client UI MUST derive page navigation state strictly from these metadata properties and MUST NOT infer totals from `items.length`.

### Invariant 7: DTO Nullability Contract
Frontend code must distinguish between response DTOs using Jackson `@JsonInclude(JsonInclude.Include.NON_NULL)` versus explicit null serialization:
- **`@JsonInclude(NON_NULL)` (13 DTO classes)**: Null fields are completely omitted from JSON payloads and evaluate to `undefined` in JavaScript (`AccountResponse`, `AuthResponse`, `LessonDetailResponse`, `LessonSummaryResponse`, `LessonVocabItemResponse`, `ModerationLogResponse`, `ModerationQueueResponse`, `RadicalDetailResponse`, `RadicalResponse`, `RoleResponse`, `UserProfileResponse`, `VocabularyDetailResponse`, `VocabularyResponse`). Frontend code must safely handle omitted properties using nullish coalescing (`??`) or optional chaining (`?.`).
- **Explicit Nulls (9 DTO classes)**: Null fields are serialized explicitly as `null` or zero/empty collections (`ApiResponse`, `DueCardResponse`, `ImportValidationReport`, `PageResponse`, `ParsedVocabularyItem`, `PersonalNoteResponse`, `RowValidationError`, `StudyStatsResponse`, `UserSrsSettingResponse`).

### Invariant 8: Backend Data Source Authority & Zero-Fictional Feature Boundary
- **No Fictional UI Features**: Frontend interfaces MUST NOT be constructed around data fields or endpoints that the backend does not provide.
  - **Radical Stroke Count**: Neither the database table `RADICAL` nor the response DTOs (`RadicalResponse`, `RadicalDetailResponse`) contain a `strokeCount` field. Radicals are characterized exclusively by `radicalId` (1..214), `character`, `pinyin`, `meaningHanViet`, `meaningVi`, `audioUrl`, `videoWritingUrl`. Radical stroke filtering (1–17 strokes) is formally categorized as `FUTURE / BLOCKED — NO AUTHORITATIVE BACKEND DATA SOURCE` and is strictly excluded from current frontend scope.
  - **Lesson Metadata**: `CreateLessonRequest` and `UpdateLessonRequest` contain only `title`, `excelFileUrl`, and `vocabularyIds`. There is NO `description` field.
  - **Vocabulary Sequencing**: `POST /{id}/vocabularies/{vocabId}` automatically assigns `nextOrderIndex`; clients MUST NOT send an `orderIndex` parameter on item addition.
- **Subresource Integrity (SRI) Discipline**: All third-party CDN assets (Bootstrap 5.3.3 CSS and JS bundle) MUST include verified, authentic cryptographic SRI attributes (`integrity="sha384-..."`) and `crossorigin="anonymous"`.

---

## 4. Requirement Governance & Change Request Protocol

To maintain architectural integrity while remaining agile to evolving product requirements, all engineering requests are governed by the following protocol:

### 4.1 Request Classification
When an instruction or user prompt is received, it is categorized into:
1. **`VALID CURRENT-STATE REQUEST`**: Conforms to the current backend implementation, verified DTOs, and established contracts. Executes immediately.
2. **`CONTRACT CHANGE REQUEST`**: Proposes changes to API paths, HTTP methods, status codes, query parameters, or request/response DTO schemas.
3. **`ARCHITECTURE CHANGE REQUEST`**: Proposes changes to frontend technology stack, state management patterns, or design systems.
4. **`DATABASE CHANGE REQUEST`**: Proposes additions or alterations to Flyway migrations, database tables, or entity relationships.
5. **`SECURITY-SENSITIVE CHANGE`**: Touches authentication, JWT verification, role-based access control, or input/output encoding.

### 4.2 Defect & Non-Compliance Taxonomy
Technical issues identified during review are strictly differentiated:
- **`SECURITY-VIOLATION`**: Critical vulnerabilities (XSS, missing SRI, token leakage, missing authorization checks).
- **`ACCESSIBILITY-NONCOMPLIANT`**: Violations of WCAG 2.2 Level AA criteria (color contrast < 4.5:1, missing keyboard navigation, unlabeled icon buttons).
- **`CONTRACT-VIOLATION`**: Discrepancies between frontend consumer code and actual backend Spring Boot controller/DTO implementations.
- **`ARCHITECTURE-VIOLATION`**: Breaches of core architectural rules (e.g., using banned frameworks like jQuery/React, polluting `window`).
- **`QUALITY-ISSUE`**: Missing edge case handling (empty states, missing loading spinners, unhandled error toasts).

### 4.3 Change Impact Protocol
User requests proposing a change to established baselines MUST NOT be blindly refused with "source code overrides user". Instead, they are processed via a 6-step impact analysis:
1. **Identify Impacted Layers**: Determine exactly which Controllers, Services, DTOs, Entities, Migrations, Integration Tests, Frontend Contracts, and Documentation are affected.
2. **Backward Compatibility Review**: Assess if existing clients or test suites break.
3. **Security & Accessibility Audit**: Ensure the change does not introduce vulnerabilities or WCAG regressions.
4. **Implementation Plan Formulation**: Detail the step-by-step modification sequence.
5. **Stakeholder Alignment**: Present the impact and plan for user review.
6. **Execution & Regression Verification**: Apply changes to source, tests, and documentation atomically.


