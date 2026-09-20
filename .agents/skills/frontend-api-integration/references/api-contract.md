# Canonical Frontend API Contract Reference

> **Authority**: Canonical frontend-facing contract derived from executable backend evidence. (Executable Java source code remains the primary authority).  
> **Backend Architecture**: Spring Boot 3.3.5 / Spring Security 6 / Java 21 LTS  
> **Scope**: All 48 Java Controller Handler Methods / 49 HTTP Method+Path Mappings across 15 Controllers, plus 1 Actuator Health Probe.

---

## 1. Global Network & Envelope Invariants

### 1.1 Base URL & Content-Type
- **Base URL**: Defaults to relative `/api/v1` for same-origin deployments, or `window.__ENV__?.API_BASE_URL || '/api/v1'` for configurable cross-origin environments.
- **Request Headers**:
  - `Content-Type: application/json; charset=UTF-8` (for all JSON mutation requests)
  - `Authorization: Bearer <token>` (automatically injected when session token exists)
- **Multipart Uploads**: For file uploads (`multipart/form-data`), **DO NOT set `Content-Type` manually**. Browser must inject boundary automatically.

### 1.2 Standard Response Envelope (`ApiResponse<T>`)
Most business API responses use `ApiResponse<T>`. Endpoint-specific response behavior takes precedence. HTTP 204 responses contain no body and therefore do not contain `ApiResponse<T>`.
```json
{
  "code": "SUCCESS",
  "message": "Thao tác thành công",
  "data": { ... },
  "errors": []
}
```
- On success (both HTTP 200 OK and HTTP 201 Created): `code` is ALWAYS `"SUCCESS"`, `message` is descriptive, `errors` is empty array `[]`, `data` contains the payload. (Note: `ErrorCode.CREATED` exists in the backend enum but is never emitted by controllers in current implementation; payload `code` is consistently `"SUCCESS"`).
- On error: `code` is error enum code (e.g. `"VALIDATION_ERROR"`, `"NOT_FOUND"`, `"CONFLICT"`), `message` is safe error description, `data` is `null`, `errors` contains error details.

### 1.3 Validation Error Parsing Contract
When Jakarta Bean Validation fails (`MethodArgumentNotValidException`, HTTP 400), `GlobalExceptionHandler` formats `errors` as a `List<String>` where each element is:
```text
"${field}: ${message}"
```
Example:
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "data": null,
  "errors": [
    "emailOrPhone: Email or phone is required",
    "password: Password must be between 6 and 100 characters"
  ]
}
```
Frontend form handlers must split each string on the first colon `:`:
```javascript
/**
 * Canonical Spring Boot validation error parser.
 * GlobalExceptionHandler returns ApiResponse.errors as List<String>:
 * ["title: Tiêu đề không được để trống", "emailOrPhone: Email hoặc số điện thoại không hợp lệ"]
 * This helper normalizes raw error strings into an array of { field, message } pairs,
 * preserving error ordering and handling unmapped errors where field is null.
 *
 * @param {Array<string|object>} rawErrors
 * @returns {Array<{ field: string|null, message: string }>}
 */
export function parseFieldErrors(rawErrors = []) {
  if (!Array.isArray(rawErrors)) return [];
  return rawErrors.map(err => {
    if (typeof err === 'string') {
      const colonIdx = err.indexOf(':');
      if (colonIdx !== -1) {
        return {
          field: err.slice(0, colonIdx).trim(),
          message: err.slice(colonIdx + 1).trim()
        };
      }
      return { field: null, message: err.trim() };
    }
    if (err && typeof err === 'object') {
      return {
        field: err.field || null,
        message: err.message || JSON.stringify(err)
      };
    }
    return { field: null, message: String(err) };
  });
}

/**
 * Groups parsed errors by field name for direct key lookup: { [field]: string[] }
 * Unmapped errors without a field are collected under key '_general'.
 *
 * @param {Array<string|object>} rawErrors
 * @returns {Record<string, string[]>}
 */
export function groupFieldErrors(rawErrors = []) {
  const parsed = parseFieldErrors(rawErrors);
  const grouped = {};
  for (const { field, message } of parsed) {
    const key = field || '_general';
    if (!grouped[key]) grouped[key] = [];
    grouped[key].push(message);
  }
  return grouped;
}
```

### 1.4 Authoritative Pagination Envelope (`PageResponse<T>`)
Paged endpoints return `PageResponse<T>` within `response.data`:
```json
{
  "page": 0,
  "size": 20,
  "totalElements": 214,
  "totalPages": 11,
  "items": [ ... ]
}
```
- `page`: 0-indexed integer (`0` is first page).
- `size`: page size (e.g. 20).
- `totalElements`: total count of matching items (`long`).
- `totalPages`: total number of available pages (`int`).
- `items`: array containing page elements. **Field name is `items`, NEVER `content`**.

### 1.5 DTO Nullability Contract
Frontend agents must recognize the distinction between DTOs configured with Jackson `@JsonInclude(JsonInclude.Include.NON_NULL)` versus explicit null DTOs:

| Category | DTO Classes | Behavior when field is null | Safe Frontend Pattern |
|:---|:---|:---|:---|
| **NON_NULL (Omitted)** | `AccountResponse`, `AuthResponse`, `LessonDetailResponse`, `LessonSummaryResponse`, `LessonVocabItemResponse`, `ModerationLogResponse`, `ModerationQueueResponse`, `RadicalDetailResponse`, `RadicalResponse`, `RoleResponse`, `UserProfileResponse`, `VocabularyDetailResponse`, `VocabularyResponse` | Field is completely **absent** from JSON (returns `undefined` in JS) | `item.avatarUrl ?? '/default.png'`<br>`item.videoWritingUrl ?? null` |
| **Explicit Nulls** | `ApiResponse`, `DueCardResponse`, `ImportValidationReport`, `PageResponse`, `ParsedVocabularyItem`, `PersonalNoteResponse`, `RowValidationError`, `StudyStatsResponse`, `UserSrsSettingResponse` | Field is serialized explicitly as `null` or empty array/zero | `item.strokeCount === null`<br>`item.errors ?? []` |

---

## 2. Exhaustive Endpoint Catalog (48 Handlers / 49 Mappings)

### Surface 1: Authentication & User Profile (`/api/v1/auth`, `/api/v1/users/profile`)

#### 1. POST `/api/v1/auth/register`
- **Controller**: `AuthController.register`
- **Auth / Role**: Public (No auth required)
- **Request Body**: `RegisterRequest`
  - `emailOrPhone` (`String`, `@NotBlank`, `@Size(max = 191)`)
  - `password` (`String`, `@NotBlank`, `@Size(min = 6, max = 100)`)
  - `fullName` (`String`, `@NotBlank`, `@Size(max = 100)`)
- **Response**: HTTP 201 Created -> `ApiResponse<AuthResponse>`
- **AuthResponse Payload**:
  - `token` (`String`) — **CRITICAL: field is `token`, NOT `accessToken`**
  - `type` (`String`, e.g. `"Bearer"`)
  - `accountId` (`Long`)
  - `emailOrPhone` (`String`)
  - `fullName` (`String`)
  - `roles` (`List<String>`, e.g. `["Learner"]`)
- **Status / Errors**: 201 Created; 400 (validation error); 409 (duplicate emailOrPhone).

#### 2. POST `/api/v1/auth/login`
- **Controller**: `AuthController.login`
- **Auth / Role**: Public (Rate-limited: 10 requests / 60s per client IP)
- **Request Body**: `LoginRequest`
  - `emailOrPhone` (`String`, `@NotBlank`, `@Size(max = 191)`)
  - `password` (`String`, `@NotBlank`)
- **Response**: HTTP 200 OK -> `ApiResponse<AuthResponse>`
- **Status / Errors**: 200 OK; 400 (validation error); 401 (invalid credentials / disabled / banned account); 429 Too Many Requests (rate limit exceeded: returns code `TOO_MANY_REQUESTS`; backend does NOT set `Retry-After` header).

#### 3. GET `/api/v1/users/profile`
- **Controller**: `UserProfileController.getProfile`
- **Auth / Role**: Authenticated (Any logged-in user)
- **Query Params**: None
- **Response**: HTTP 200 OK -> `ApiResponse<UserProfileResponse>`
- **UserProfileResponse Payload** (`@JsonInclude(NON_NULL)`):
  - `userId` (`Long`)
  - `accountId` (`Long`)
  - `emailOrPhone` (`String`)
  - `fullName` (`String`)
  - `avatarUrl` (`String`, omitted if null)
  - `createdAt` (`String` / ISO-8601 LocalDateTime)
  - `updatedAt` (`String` / ISO-8601 LocalDateTime)
- **Status / Errors**: 200 OK; 401 (unauthenticated).

#### 4. PUT `/api/v1/users/profile`
- **Controller**: `UserProfileController.updateProfile`
- **Auth / Role**: Authenticated (Any logged-in user)
- **Request Body**: `UpdateProfileRequest`
  - `fullName` (`String`, `@NotBlank`, `@Size(max = 100)`)
  - `avatarUrl` (`String`, optional, `@Size(max = 500)`)
- **Response**: HTTP 200 OK -> `ApiResponse<UserProfileResponse>`
- **Status / Errors**: 200 OK; 400 (validation error); 401 (unauthenticated).

---

### Surface 2: Public Learning Catalogs (`/api/v1/radicals`, `/api/v1/vocabulary`, `/api/v1/lessons`)

#### 5. GET `/api/v1/radicals`
- **Controller**: `RadicalController.getAllRadicals`
- **Auth / Role**: Public (PermitAll)
- **Query Params**: `page` (`int`, default 0), `size` (`int`, default 20), `sort` (optional)
- **Response**: HTTP 200 OK -> `ApiResponse<PageResponse<RadicalResponse>>`
- **RadicalResponse Payload** (`@JsonInclude(NON_NULL)`):
  - `radicalId` (`Integer`) — **NOTE: Radical ID is `Integer` (1..214)**
  - `character` (`String`)
  - `pinyin` (`String`)
  - `meaningHanViet` (`String`)
  - `meaningVi` (`String`, omitted if null/empty)
  - `audioUrl` (`String`, omitted if null)
  - `videoWritingUrl` (`String`, omitted if null)

#### 6. GET `/api/v1/radicals/{id}`
- **Controller**: `RadicalController.getRadicalById`
- **Auth / Role**: Public (PermitAll)
- **Path Variable**: `id` (`Integer`, required)
- **Response**: HTTP 200 OK -> `ApiResponse<RadicalDetailResponse>`
- **RadicalDetailResponse Payload** (`@JsonInclude(NON_NULL)`):
  - `radicalId` (`Integer`), `character` (`String`), `pinyin` (`String`), `meaningHanViet` (`String`), `meaningVi` (`String`), `audioUrl` (`String`), `videoWritingUrl` (`String`), `createdAt` (`String`), `updatedAt` (`String`)
- **Status / Errors**: 200 OK; 404 (radical not found).

#### 7. GET `/api/v1/vocabulary`
- **Controller**: `VocabularyController.getVocabularies`
- **Auth / Role**: Public (PermitAll)
- **Query Params**:
  - `search` (`String`, optional — searches across Hanzi, Pinyin with tones, Pinyin without tones `pinyin_raw`, and Vietnamese meaning)
  - `page` (`int`, default 0), `size` (`int`, default 20), `sort` (optional)
- **Response**: HTTP 200 OK -> `ApiResponse<PageResponse<VocabularyResponse>>`
- **VocabularyResponse Payload** (`@JsonInclude(NON_NULL)`):
  - `vocabId` (`Long`), `hanzi` (`String`), `pinyin` (`String`), `pinyinRaw` (`String`), `meaningHanViet` (`String`), `meaningVi` (`String`), `audioUrl` (`String`), `videoWritingUrl` (`String`), `exampleSentence` (`String`), `exampleTranslation` (`String`)

#### 8. GET `/api/v1/vocabulary/{id}`
- **Controller**: `VocabularyController.getVocabularyById`
- **Auth / Role**: Public (PermitAll)
- **Path Variable**: `id` (`Long`, required)
- **Response**: HTTP 200 OK -> `ApiResponse<VocabularyDetailResponse>`
- **VocabularyDetailResponse Payload** (`@JsonInclude(NON_NULL)`):
  - Common vocab fields (`vocabId`, `hanzi`, `pinyin`, `pinyinRaw`, `meaningHanViet`, `meaningVi`, `audioUrl`, `videoWritingUrl`, `exampleSentence`, `exampleTranslation`, `createdAt`, `updatedAt`)
  - `radicals` (`List<RadicalResponse>`) — constituent Kangxi radicals, empty array `[]` if none.
- **Status / Errors**: 200 OK; 404 (vocabulary not found).

#### 9. GET `/api/v1/lessons`
- **Controller**: `LessonController.getApprovedLessons`
- **Auth / Role**: Public (PermitAll)
- **Query Params**: `page` (`int`, default 0), `size` (`int`, default 20), `sort` (optional)
- **Response**: HTTP 200 OK -> `ApiResponse<PageResponse<LessonSummaryResponse>>`
- **Behavior**: Returns strictly lessons with `status = 'Approved'`.
- **LessonSummaryResponse Payload** (`@JsonInclude(NON_NULL)`):
  - `lessonId` (`Long`), `title` (`String`), `status` (`String`, `"Approved"`), `vocabularyCount` (`Integer`), `createdAt` (`String`), `updatedAt` (`String`)

#### 10. GET `/api/v1/lessons/{id}`
- **Controller**: `LessonController.getApprovedLessonById`
- **Auth / Role**: Public (PermitAll)
- **Path Variable**: `id` (`Long`, required)
- **Response**: HTTP 200 OK -> `ApiResponse<LessonDetailResponse>`
- **Behavior**: Returns strictly if `status = 'Approved'`; returns 404 if draft, pending, or rejected.
- **LessonDetailResponse Payload** (`@JsonInclude(NON_NULL)`):
  - `lessonId` (`Long`), `title` (`String`), `status` (`String`), `vocabularyCount` (`Integer`), `createdAt` (`String`), `updatedAt` (`String`)
  - `vocabularies` (`List<LessonVocabItemResponse>`) — ordered deterministically by `orderIndex` ascending.
  - `LessonVocabItemResponse`: `vocabId` (`Long`), `hanzi` (`String`), `pinyin` (`String`), `pinyinRaw` (`String`), `meaningHanViet` (`String`), `meaningVi` (`String`), `audioUrl` (`String`), `videoWritingUrl` (`String`), `exampleSentence` (`String`), `exampleTranslation` (`String`), `orderIndex` (`Integer`)
- **Status / Errors**: 200 OK; 404 (not found or not approved).

---

### Surface 3: Spaced Repetition System (SRS) & Settings (`/api/v1/srs`)

#### 11. GET `/api/v1/srs/due`
- **Controller**: `SrsController.getDueCards`
- **Auth / Role**: Authenticated (Learner+)
- **Query Params**:
  - `itemType` (`String`, optional: `"VOCABULARY"` or `"RADICAL"`)
  - `limit` (`Integer`, optional)
- **Response**: HTTP 200 OK -> `ApiResponse<List<DueCardResponse>>` (NOTE: unpaged List, not PageResponse)
- **DueCardResponse Payload** (Explicit Nulls):
  - `itemType` (`String`: `"VOCABULARY"` or `"RADICAL"`)
  - `itemId` (`Long`)
  - `hanzi` (`String`), `pinyin` (`String`), `meaningVi` (`String`), `meaningHanViet` (`String`)
  - `exampleSentence` (`String`, explicit `null` for Radical)
  - `exampleTranslation` (`String`, explicit `null` for Radical)
  - `strokeCount` (`Integer`, **always explicit `null`**): Backend hardcodes `response.setStrokeCount(null)` for both Vocabulary and Radical. **Frontend MUST NOT rely on `DueCardResponse.strokeCount` to implement stroke count filters.**
  - `repetitions` (`Integer`), `easeFactor` (`BigDecimal`, e.g. `2.50`), `intervalDays` (`Integer`), `nextReviewAt` (`String` / null), `isNew` (`Boolean`)
- **DueCard Media & Decomposition Boundary**:
  - `DueCardResponse` does **NOT** contain `audioUrl`, `videoWritingUrl`, `pinyinRaw`, or `radicals[]`.
  - If flashcard UI requires audio playback, stroke order video, or radical breakdown, the client must query the respective detail endpoint:
    - Vocabulary: `GET /api/v1/vocabulary/{id}` (`VocabularyDetailResponse`)
    - Radical: `GET /api/v1/radicals/{id}` (`RadicalDetailResponse`)

#### 12. GET `/api/v1/srs/new-cards`
- **Controller**: `SrsController.getNewCardCandidates` (`SrsController.java:59-65`)
- **Auth / Role**: Authenticated (Learner+)
- **Query Params**:
  - `lessonId` (`Long`, **REQUIRED**) — must specify an approved lesson ID. Calling without `lessonId` triggers HTTP 400 Bad Request (`MissingServletRequestParameterException`).
  - `limit` (`Integer`, optional) — max new cards to return (capped by remaining daily new-card quota).
- **Response**: HTTP 200 OK -> `ApiResponse<List<DueCardResponse>>`
- **Behavior**: Retrieves candidate new vocabulary cards from an approved lesson (`status = 'Approved'`) in `orderIndex ASC` sequence without creating `CardProgress` rows (idempotent candidate preview; cards are initialized into SRS upon first review). All candidate cards have `isNew = true`.

#### 13. GET `/api/v1/srs/lessons/{lessonId}/new-cards`
- **Controller**: `SrsController.getNewCardCandidatesForLesson` (`SrsController.java:74-80`)
- **Auth / Role**: Authenticated (Learner+)
- **Path Variable**: `lessonId` (`Long`, **REQUIRED**)
- **Query Params**: `limit` (`Integer`, optional)
- **Response**: HTTP 200 OK -> `ApiResponse<List<DueCardResponse>>`
- **Behavior**: RESTful sub-resource alias of endpoint #12, delegating to the same underlying service method with identical return payload.

#### 14. POST `/api/v1/srs/review`
- **Controller**: `SrsController.reviewCard`
- **Auth / Role**: Authenticated (Learner+)
- **Request Body**: `ReviewCardRequest`
  - `itemType` (`String`, `@NotBlank`, `@Pattern(regexp = "^(?i)(VOCABULARY|RADICAL)$")`)
  - `itemId` (`Long`, `@NotNull`, `@Positive`)
  - `rating` (`Integer`, `@NotNull`, `@Min(1)`, `@Max(4)`) — 1=Again, 2=Hard, 3=Good, 4=Easy
  - `reviewTimeSeconds` (`Integer`, `@NotNull`, `@Min(0)`)
- **Response**: HTTP 200 OK -> `ApiResponse<DueCardResponse>` (returns updated card state)
- **Status / Errors**:
  - 200 OK (review accepted and progress persisted).
  - 400 (validation error).
  - 409 Conflict ("Thẻ chưa đến hạn ôn tập" — premature review rejection).
  - 422 Unprocessable Entity (vocabulary does not belong to any Approved lesson).

#### 15. GET `/api/v1/srs/stats`
- **Controller**: `SrsController.getStudyStats`
- **Auth / Role**: Authenticated (Learner+)
- **Query Params**: None
- **Response**: HTTP 200 OK -> `ApiResponse<StudyStatsResponse>`
- **StudyStatsResponse Payload**:
  - `cardsDue` (`long`), `reviewsToday` (`long`), `newCardsLimit` (`int`), `maxReviewLimit` (`int`), `newCardsToday` (`long`)

#### 16. GET `/api/v1/srs/settings`
- **Controller**: `UserSrsSettingController.getSettings`
- **Auth / Role**: Authenticated (Learner+)
- **Query Params**: None
- **Response**: HTTP 200 OK -> `ApiResponse<UserSrsSettingResponse>`
- **UserSrsSettingResponse Payload**:
  - `settingId` (`Long`), `newCardsPerDay` (`Integer`, default 20), `maxReviewPerDay` (`Integer`, default 100)

#### 17. PUT `/api/v1/srs/settings`
- **Controller**: `UserSrsSettingController.updateSettings`
- **Auth / Role**: Authenticated (Learner+)
- **Request Body**: `UpdateSrsSettingRequest`
  - `newCardsPerDay` (`Integer`, `@NotNull`, `@Positive`)
  - `maxReviewPerDay` (`Integer`, `@NotNull`, `@Positive`)
- **Response**: HTTP 200 OK -> `ApiResponse<UserSrsSettingResponse>`
- **Status / Errors**: 200 OK; 400 (validation error); 401 (unauthenticated).

---

### Surface 4: Personal Notes (`/api/v1/vocabularies/{vocabId}/notes`, `/api/v1/notes/{noteId}`)

#### 18. GET `/api/v1/vocabularies/{vocabId}/notes`
- **Controller**: `PersonalNoteController.getNotesByVocabulary`
- **Auth / Role**: Authenticated (Learner+)
- **Path Variable**: `vocabId` (`Long`, required)
- **Query Params**: `page` (`Integer`, default 0), `size` (`Integer`, default 20, server-capped at max 100)
- **Response**: HTTP 200 OK -> `ApiResponse<PageResponse<PersonalNoteResponse>>`
- **PersonalNoteResponse Payload**:
  - `noteId` (`Long`), `vocabId` (`Long`), `content` (`String`), `createdAt` (`String` / LocalDateTime)

#### 19. POST `/api/v1/vocabularies/{vocabId}/notes`
- **Controller**: `PersonalNoteController.createNote`
- **Auth / Role**: Authenticated (Learner+)
- **Path Variable**: `vocabId` (`Long`, required)
- **Request Body**: `PersonalNoteRequest`
  - `content` (`String`, `@NotNull`, `@Size(max = 500)`)
- **Response**: HTTP 201 Created -> `ApiResponse<PersonalNoteResponse>`
- **Status / Errors**: 201 Created; 400 (validation error); 404 (vocab not found).

#### 20. PUT `/api/v1/notes/{noteId}`
- **Controller**: `PersonalNoteController.updateNote`
- **Auth / Role**: Authenticated (Owner only)
- **Path Variable**: `noteId` (`Long`, required)
- **Request Body**: `PersonalNoteRequest`
  - `content` (`String`, `@NotNull`, `@Size(max = 500)`)
- **Response**: HTTP 200 OK -> `ApiResponse<PersonalNoteResponse>`
- **Status / Errors**: 200 OK; 400 (validation error); 403 (not owner); 404 (note not found).

#### 21. DELETE `/api/v1/notes/{noteId}`
- **Controller**: `PersonalNoteController.deleteNote`
- **Auth / Role**: Authenticated (Owner only)
- **Path Variable**: `noteId` (`Long`, required)
- **Response**: **HTTP 200 OK** -> `ApiResponse<Void>`
- **NOTE**: Returns `200 OK` with JSON envelope `{"code":"SUCCESS","message":"Xóa ghi chú thành công","data":null,"errors":[]}` (unlike admin delete endpoints which return 204 No Content).

---

### Surface 5: Creator Studio (`/api/v1/creator/lessons`)

#### 22. POST `/api/v1/creator/lessons`
- **Controller**: `CreatorLessonController.createLesson`
- **Auth / Role**: `ROLE_CREATOR` or `ROLE_ADMIN`
- **Request Body**: `CreateLessonRequest`
  - `title` (`String`, `@NotBlank`, `@Size(max = 200)`)
  - `excelFileUrl` (`String`, optional, `@Size(max = 500)`)
  - `vocabularyIds` (`List<Long>`, optional, elements `@NotNull @Positive`)
- **Response**: HTTP 201 Created -> `ApiResponse<LessonDetailResponse>`
- **Initial Status**: Created as `"Draft"`.

#### 23. GET `/api/v1/creator/lessons`
- **Controller**: `CreatorLessonController.getMyLessons`
- **Auth / Role**: `ROLE_CREATOR` or `ROLE_ADMIN`
- **Query Params**: `page` (`int`, default 0), `size` (`int`, default 20), `sort` (optional)
- **Response**: HTTP 200 OK -> `ApiResponse<PageResponse<LessonSummaryResponse>>`
- **Behavior**: Returns only lessons created by the authenticated creator across all statuses (`Draft`, `Pending`, `Approved`, `Rejected`).

#### 24. GET `/api/v1/creator/lessons/{id}`
- **Controller**: `CreatorLessonController.getMyLessonById`
- **Auth / Role**: `ROLE_CREATOR` (Owner) or `ROLE_ADMIN`
- **Path Variable**: `id` (`Long`, required)
- **Response**: HTTP 200 OK -> `ApiResponse<LessonDetailResponse>`
- **Status / Errors**: 200 OK; 403 (not owner); 404 (lesson not found).

#### 25. PUT `/api/v1/creator/lessons/{id}`
- **Controller**: `CreatorLessonController.updateMyLesson`
- **Auth / Role**: `ROLE_CREATOR` (Owner) or `ROLE_ADMIN`
- **Path Variable**: `id` (`Long`, required)
- **Request Body**: `UpdateLessonRequest`
  - `title` (`String`, optional, `@Size(max = 200)`)
  - `excelFileUrl` (`String`, optional, `@Size(max = 500)`)
- **Response**: HTTP 200 OK -> `ApiResponse<LessonDetailResponse>`
- **Behavior**: Allowed only if status is `Draft` or `Rejected`; rejected with 400/409 if `Pending` or `Approved`.

#### 26. DELETE `/api/v1/creator/lessons/{id}`
- **Controller**: `CreatorLessonController.deleteMyLesson`
- **Auth / Role**: `ROLE_CREATOR` (Owner) or `ROLE_ADMIN`
- **Path Variable**: `id` (`Long`, required)
- **Response**: **HTTP 204 No Content** (NO response body — `response.json()` must NOT be called)
- **Behavior**: Allowed only if `Draft` and never had moderation history. Returns 409 Conflict if history exists or status is not Draft.

#### 27. POST `/api/v1/creator/lessons/{id}/vocabularies/{vocabId}`
- **Controller**: `CreatorLessonController.addVocabularyToLesson`
- **Auth / Role**: `ROLE_CREATOR` (Owner) or `ROLE_ADMIN`
- **Path Variables**: `id` (`Long`, lesson ID), `vocabId` (`Long`, vocabulary ID)
- **Response**: HTTP 200 OK -> `ApiResponse<LessonDetailResponse>`
- **Behavior**: Appends vocabulary to lesson with next `orderIndex` automatically (`nextOrderIndex = countByLesson + 1`). Returns 409 if vocabulary already in lesson. **NOTE**: Client does NOT supply `orderIndex` in path, query, or body; ordering changes must be performed via endpoint #29/30 (`/reorder`) using `orderedVocabIds` or `items`.

#### 28. DELETE `/api/v1/creator/lessons/{id}/vocabularies/{vocabId}`
- **Controller**: `CreatorLessonController.removeVocabularyFromLesson`
- **Auth / Role**: `ROLE_CREATOR` (Owner) or `ROLE_ADMIN`
- **Path Variables**: `id` (`Long`, lesson ID), `vocabId` (`Long`, vocabulary ID)
- **Response**: HTTP 200 OK -> `ApiResponse<LessonDetailResponse>`
- **Behavior**: Removes vocabulary and re-indexes remaining items sequentially (1..N).

#### 29. PUT `/api/v1/creator/lessons/{id}/reorder`
- **Controller**: `CreatorLessonController.reorderVocabulary`
- **Auth / Role**: `ROLE_CREATOR` (Owner) or `ROLE_ADMIN`
- **Path Variable**: `id` (`Long`, required)
- **Request Body**: `ReorderVocabRequest`
  - Supports `orderedVocabIds` (`List<Long>`) OR `items` (`List<VocabOrderItem>` with `vocabId` and `orderIndex`)
- **Response**: HTTP 200 OK -> `ApiResponse<LessonDetailResponse>`

#### 30. POST `/api/v1/creator/lessons/{id}/reorder` (Dual Mapping)
- **Controller**: `CreatorLessonController.reorderVocabulary` (same handler mapped to both PUT and POST)
- **Exact duplicate route/body of #29**.

#### 31. POST `/api/v1/creator/lessons/{id}/submit`
- **Controller**: `CreatorLessonController.submitForModeration`
- **Auth / Role**: `ROLE_CREATOR` (Owner) or `ROLE_ADMIN`
- **Path Variable**: `id` (`Long`, required)
- **Response**: HTTP 200 OK -> `ApiResponse<LessonDetailResponse>`
- **Behavior**: Transitions status from `Draft` or `Rejected` -> `Pending`. Rejects empty lessons (422 Unprocessable Entity).

#### 32. POST `/api/v1/creator/lessons/import` (Step 1: Preview)
- **Controller**: `CreatorLessonController.importPreview`
- **Auth / Role**: `ROLE_CREATOR` or `ROLE_ADMIN`
- **Consumes**: `multipart/form-data`
- **Request Parameter**: `file` (`MultipartFile`, required `.xlsx` file, max 10MB)
- **Response**: HTTP 200 OK -> `ApiResponse<ImportValidationReport>`
- **GUARANTEE**: **Zero DB mutations**.
- **ImportValidationReport Payload**:
  - `isValid` (`Boolean`), `totalRows` (`Integer`), `validRowsCount` (`Integer`), `invalidRowsCount` (`Integer`), `newVocabCount` (`Integer`), `existingVocabCount` (`Integer`), `fileStatus` (`String`), `summaryMessage` (`String`), `rows` (`List<ParsedVocabularyItem>`), `errors` (`List<RowValidationError>`)

#### 33. POST `/api/v1/creator/lessons/import/confirm` (Step 2: Confirm)
- **Controller**: `CreatorLessonController.importConfirm`
- **Auth / Role**: `ROLE_CREATOR` or `ROLE_ADMIN`
- **Consumes**: `multipart/form-data`
- **Request Parameters**:
  - `title` (`String`, required lesson title)
  - `file` (`MultipartFile`, required `.xlsx` file)
- **Response**: HTTP 201 Created -> `ApiResponse<LessonDetailResponse>`
- **Behavior**: Atomically persists the `Lesson` ('Draft'), creates/links vocabulary items in Excel order, and commits in a single transaction.

---

### Surface 6: Moderation Queue (`/api/v1/moderator`)

#### 34. GET `/api/v1/moderator/lessons/pending`
- **Controller**: `ModeratorController.getPendingLessons`
- **Auth / Role**: `ROLE_MODERATOR` or `ROLE_ADMIN`
- **Query Params**: `page` (`int`, default 0), `size` (`int`, default 20), `sort` (optional)
- **Response**: HTTP 200 OK -> `ApiResponse<PageResponse<ModerationQueueResponse>>`
- **ModerationQueueResponse Payload** (`@JsonInclude(NON_NULL)`):
  - `lessonId` (`Long`), `title` (`String`), `status` (`String`, `"Pending"`), `creatorId` (`Long`), `creatorEmail` (`String`), `vocabularyCount` (`Integer`), `createdAt` (`String`), `updatedAt` (`String`)

#### 35. GET `/api/v1/moderator/lessons/{id}`
- **Controller**: `ModeratorController.getPendingLessonById`
- **Auth / Role**: `ROLE_MODERATOR` or `ROLE_ADMIN`
- **Path Variable**: `id` (`Long`, required)
- **Response**: HTTP 200 OK -> `ApiResponse<LessonDetailResponse>`
- **Status / Errors**: 200 OK; 404 (lesson not found or not in Pending status).

#### 36. POST `/api/v1/moderator/lessons/{id}/approve`
- **Controller**: `ModeratorController.approveLesson`
- **Auth / Role**: `ROLE_MODERATOR` or `ROLE_ADMIN`
- **Path Variable**: `id` (`Long`, required)
- **Request Body**: `ApproveLessonRequest` (optional, `@RequestBody(required = false)`)
  - `note` (`String`, optional, `@Size(max = 500)`)
- **Response**: HTTP 200 OK -> `ApiResponse<LessonDetailResponse>`
- **Behavior**: Transitions `Pending` -> `Approved`. Logs entry into `MODERATION_LOG`.

#### 37. POST `/api/v1/moderator/lessons/{id}/reject`
- **Controller**: `ModeratorController.rejectLesson`
- **Auth / Role**: `ROLE_MODERATOR` or `ROLE_ADMIN`
- **Path Variable**: `id` (`Long`, required)
- **Request Body**: `RejectLessonRequest` (REQUIRED)
  - `rejectionReason` (`String`, `@NotBlank`, `@Size(max = 500)`)
  - `flaggedFields` (`String`, optional, e.g. JSON string `["pinyin", "meaning_vi"]`)
- **Response**: HTTP 200 OK -> `ApiResponse<LessonDetailResponse>`
- **Behavior**: Transitions `Pending` -> `Rejected`. Logs reason and flagged fields into `MODERATION_LOG`.

#### 38. GET `/api/v1/moderator/history`
- **Controller**: `ModeratorController.getModeratorHistory`
- **Auth / Role**: `ROLE_MODERATOR` or `ROLE_ADMIN`
- **Query Params**: `page` (`int`, default 0), `size` (`int`, default 20), `sort` (optional)
- **Response**: HTTP 200 OK -> `ApiResponse<PageResponse<ModerationLogResponse>>`
- **Behavior**: Moderator sees their own moderation actions; Admin sees all system moderation logs.
- **ModerationLogResponse Payload** (`@JsonInclude(NON_NULL)`):
  - `logId` (`Long`), `lessonId` (`Long`), `lessonTitle` (`String`), `moderatorId` (`Long`), `moderatorEmail` (`String`), `action` (`String`: `"Approved"` or `"Rejected"`), `rejectionReason` (`String`), `flaggedFields` (`String`), `createdAt` (`String`)

---

### Surface 7: System Administration (`/api/v1/admin`)

#### 39. GET `/api/v1/admin/accounts`
- **Controller**: `AdminAccountController.getAccounts`
- **Auth / Role**: `ROLE_ADMIN`
- **Query Params**: `status` (optional: `"Active"`, `"Inactive"`, `"Banned"`), `search` (optional: email/phone/fullName), `page` (`int`, default 0), `size` (`int`, default 20), `sort` (optional)
- **Response**: HTTP 200 OK -> `ApiResponse<PageResponse<AccountResponse>>`
- **AccountResponse Payload** (`@JsonInclude(NON_NULL)`):
  - `accountId` (`Long`), `emailOrPhone` (`String`), `fullName` (`String`), `status` (`String`), `roles` (`List<String>`), `createdAt` (`String`), `updatedAt` (`String`)

#### 40. PUT `/api/v1/admin/accounts/{id}/status`
- **Controller**: `AdminAccountController.updateAccountStatus`
- **Auth / Role**: `ROLE_ADMIN`
- **Path Variable**: `id` (`Long`, required)
- **Request Body**: `UpdateAccountStatusRequest`
  - `status` (`String`, `@NotBlank`, `@Pattern(regexp = "^(?i)(Active|Inactive|Banned)$")`)
- **Response**: HTTP 200 OK -> `ApiResponse<AccountResponse>`
- **Protections & Invariants**:
  - Self-Lockout Protection (POL-8D-01): Admin cannot deactivate or ban their own currently logged-in account (400 Bad Request).
  - Server-Side Token Revocation (DEC-42): Increments `authorization_version`, immediately invalidating all active JWTs for that account with 401 on their next request.

#### 41. GET `/api/v1/admin/roles`
- **Controller**: `AdminRoleController.getRoles`
- **Auth / Role**: `ROLE_ADMIN`
- **Query Params**: None
- **Response**: HTTP 200 OK -> `ApiResponse<List<RoleResponse>>` (NOTE: unpaged List)
- **RoleResponse Payload**:
  - `roleId` (`Integer`), `roleName` (`String`, e.g. `"Learner"`, `"Creator"`, `"Moderator"`, `"Admin"`)

#### 42. PUT `/api/v1/admin/accounts/{id}/roles`
- **Controller**: `AdminRoleController.updateAccountRoles`
- **Auth / Role**: `ROLE_ADMIN`
- **Path Variable**: `id` (`Long`, required)
- **Request Body**: `UpdateAccountRolesRequest`
  - `roles` (`List<String>`, `@NotEmpty`)
- **Response**: HTTP 200 OK -> `ApiResponse<AccountResponse>`
- **Protections & Invariants**:
  - Self-Demotion Protection (POL-8D-02): Admin cannot remove the `Admin` role from their own logged-in account (400 Bad Request).
  - Server-Side Token Revocation (DEC-42): Increments `authorization_version`, revoking existing tokens immediately.

#### 43. GET `/api/v1/admin/lessons`
- **Controller**: `AdminLessonController.getAdminLessons`
- **Auth / Role**: `ROLE_ADMIN`
- **Query Params**: `status` (optional), `page` (`int`, default 0), `size` (`int`, default 20), `sort` (optional)
- **Response**: HTTP 200 OK -> `ApiResponse<PageResponse<LessonSummaryResponse>>`
- **Behavior**: Lists all system lessons across any creator and any status.

> [!NOTE]
> **Admin Catalog Browsing Endpoints**:
> There are NO separate GET endpoints for Admin Radicals or Admin Vocabulary (i.e. `GET /api/v1/admin/radicals` and `GET /api/v1/admin/vocabulary` do NOT exist). Admin pages explore the master catalogs via public endpoints `GET /api/v1/radicals` (paged) and `GET /api/v1/vocabulary?search=` (paged/searchable), and perform administrative mutations via endpoints #44–#49 below.

#### 44. POST `/api/v1/admin/radicals`
- **Controller**: `AdminRadicalController.createRadical`
- **Auth / Role**: `ROLE_ADMIN`
- **Request Body**: `CreateRadicalRequest`
  - `character` (`String`, `@NotBlank`, `@Size(max = 10)`)
  - `pinyin` (`String`, `@NotBlank`, `@Size(max = 50)`)
  - `meaningHanViet` (`String`, `@NotBlank`, `@Size(max = 100)`)
  - `meaningVi` (`String`, `@NotBlank`, `@Size(max = 255)`)
  - `audioUrl` (`String`, optional, `@Size(max = 500)`)
  - `videoWritingUrl` (`String`, optional, `@Size(max = 500)`)
- **Response**: HTTP 201 Created -> `ApiResponse<RadicalDetailResponse>`

#### 45. PUT `/api/v1/admin/radicals/{id}`
- **Controller**: `AdminRadicalController.updateRadical`
- **Auth / Role**: `ROLE_ADMIN`
- **Path Variable**: `id` (`Integer`, required — Kangxi radical ID 1..214)
- **Request Body**: `UpdateRadicalRequest` (same constraints as create)
- **Response**: HTTP 200 OK -> `ApiResponse<RadicalDetailResponse>`

#### 46. DELETE `/api/v1/admin/radicals/{id}`
- **Controller**: `AdminRadicalController.deleteRadical`
- **Auth / Role**: `ROLE_ADMIN`
- **Path Variable**: `id` (`Integer`, required)
- **Response**: **HTTP 204 No Content** (NO response body — `response.json()` must NOT be called)
- **Status / Errors**: 204 No Content; 404 (not found); 409 (foreign key reference conflict).

#### 47. POST `/api/v1/admin/vocabulary`
- **Controller**: `AdminVocabularyController.createVocabulary`
- **Auth / Role**: `ROLE_ADMIN`
- **Request Body**: `CreateVocabularyRequest`
  - `hanzi` (`String`, `@NotBlank`, `@Size(max = 50)`)
  - `pinyin` (`String`, `@NotBlank`, `@Size(max = 100)`)
  - `pinyinRaw` (`String`, `@NotBlank`, `@Size(max = 100)`)
  - `meaningHanViet` (`String`, `@NotBlank`, `@Size(max = 100)`)
  - `meaningVi` (`String`, `@NotBlank`, `@Size(max = 255)`)
  - `audioUrl` (`String`, optional, `@Size(max = 500)`)
  - `videoWritingUrl` (`String`, optional, `@Size(max = 500)`)
  - `exampleSentence` (`String`, optional, `@Size(max = 500)`)
  - `exampleTranslation` (`String`, optional, `@Size(max = 500)`)
  - `radicalIds` (`Set<Integer>`, optional, elements `@NotNull @Positive`)
- **Response**: HTTP 201 Created -> `ApiResponse<VocabularyDetailResponse>`

#### 48. PUT `/api/v1/admin/vocabulary/{id}`
- **Controller**: `AdminVocabularyController.updateVocabulary`
- **Auth / Role**: `ROLE_ADMIN`
- **Path Variable**: `id` (`Long`, required)
- **Request Body**: `UpdateVocabularyRequest` (same constraints as create)
- **Response**: HTTP 200 OK -> `ApiResponse<VocabularyDetailResponse>`

#### 49. DELETE `/api/v1/admin/vocabulary/{id}`
- **Controller**: `AdminVocabularyController.deleteVocabulary`
- **Auth / Role**: `ROLE_ADMIN`
- **Path Variable**: `id` (`Long`, required)
- **Response**: **HTTP 204 No Content** (NO response body — `response.json()` must NOT be called)
- **Behavior & Safeguards (DEC-34 & DEC-35)**:
  - Checks foreign key references: rejected with **409 Conflict** if vocabulary is referenced in any `LESSON_VOCABULARY`, `PERSONAL_NOTE`, `CARD_PROGRESS`, or `REVIEW_LOG`.
  - Allowed only when completely unreferenced, cascading cleanup of `VOCAB_RADICAL`.

---

### Ops Health Probe (`/actuator/health`)

#### 50. GET `/actuator/health`
- **Auth / Role**: Public (PermitAll)
- **Response**: HTTP 200 OK -> `{"status":"UP"}`
- **NOTE**: Actuator uses Spring Boot standard format, NOT `ApiResponse<T>`.

---

## 3. Summary Mapping Matrix

| # | Controller | Java Handler Method | HTTP Method | Route | Path Var Type | Request Body / Multipart | Response Payload | Status |
|---|---|---|---|---|---|---|---|---|
| 1 | AuthController | register | POST | /api/v1/auth/register | - | RegisterRequest | ApiResponse<AuthResponse> | 201 |
| 2 | AuthController | login | POST | /api/v1/auth/login | - | LoginRequest | ApiResponse<AuthResponse> | 200 |
| 3 | UserProfileController | getProfile | GET | /api/v1/users/profile | - | - | ApiResponse<UserProfileResponse> | 200 |
| 4 | UserProfileController | updateProfile | PUT | /api/v1/users/profile | - | UpdateProfileRequest | ApiResponse<UserProfileResponse> | 200 |
| 5 | RadicalController | getAllRadicals | GET | /api/v1/radicals | - | - | ApiResponse<PageResponse<RadicalResponse>> | 200 |
| 6 | RadicalController | getRadicalById | GET | /api/v1/radicals/{id} | Integer | - | ApiResponse<RadicalDetailResponse> | 200 |
| 7 | VocabularyController | getVocabularies | GET | /api/v1/vocabulary | - | - | ApiResponse<PageResponse<VocabularyResponse>> | 200 |
| 8 | VocabularyController | getVocabularyById | GET | /api/v1/vocabulary/{id} | Long | - | ApiResponse<VocabularyDetailResponse> | 200 |
| 9 | LessonController | getApprovedLessons | GET | /api/v1/lessons | - | - | ApiResponse<PageResponse<LessonSummaryResponse>> | 200 |
| 10 | LessonController | getApprovedLessonById | GET | /api/v1/lessons/{id} | Long | - | ApiResponse<LessonDetailResponse> | 200 |
| 11 | SrsController | getDueCards | GET | /api/v1/srs/due | - | - | ApiResponse<List<DueCardResponse>> | 200 |
| 12 | SrsController | getNewCardCandidates | GET | /api/v1/srs/new-cards | Query: lessonId (Req) | - | ApiResponse<List<DueCardResponse>> | 200 |
| 13 | SrsController | getNewCardCandidatesForLesson | GET | /api/v1/srs/lessons/{lessonId}/new-cards | Long | - | ApiResponse<List<DueCardResponse>> | 200 |
| 14 | SrsController | reviewCard | POST | /api/v1/srs/review | - | ReviewCardRequest | ApiResponse<DueCardResponse> | 200 |
| 15 | SrsController | getStudyStats | GET | /api/v1/srs/stats | - | - | ApiResponse<StudyStatsResponse> | 200 |
| 16 | UserSrsSettingController | getSettings | GET | /api/v1/srs/settings | - | - | ApiResponse<UserSrsSettingResponse> | 200 |
| 17 | UserSrsSettingController | updateSettings | PUT | /api/v1/srs/settings | - | UpdateSrsSettingRequest | ApiResponse<UserSrsSettingResponse> | 200 |
| 18 | PersonalNoteController | getNotesByVocabulary | GET | /api/v1/vocabularies/{vocabId}/notes | Long | - | ApiResponse<PageResponse<PersonalNoteResponse>> | 200 |
| 19 | PersonalNoteController | createNote | POST | /api/v1/vocabularies/{vocabId}/notes | Long | PersonalNoteRequest | ApiResponse<PersonalNoteResponse> | 201 |
| 20 | PersonalNoteController | updateNote | PUT | /api/v1/notes/{noteId} | Long | PersonalNoteRequest | ApiResponse<PersonalNoteResponse> | 200 |
| 21 | PersonalNoteController | deleteNote | DELETE | /api/v1/notes/{noteId} | Long | - | ApiResponse<Void> | 200 |
| 22 | CreatorLessonController | createLesson | POST | /api/v1/creator/lessons | - | CreateLessonRequest | ApiResponse<LessonDetailResponse> | 201 |
| 23 | CreatorLessonController | getMyLessons | GET | /api/v1/creator/lessons | - | - | ApiResponse<PageResponse<LessonSummaryResponse>> | 200 |
| 24 | CreatorLessonController | getMyLessonById | GET | /api/v1/creator/lessons/{id} | Long | - | ApiResponse<LessonDetailResponse> | 200 |
| 25 | CreatorLessonController | updateMyLesson | PUT | /api/v1/creator/lessons/{id} | Long | UpdateLessonRequest | ApiResponse<LessonDetailResponse> | 200 |
| 26 | CreatorLessonController | deleteMyLesson | DELETE | /api/v1/creator/lessons/{id} | Long | - | Void (No Content) | 204 |
| 27 | CreatorLessonController | addVocabularyToLesson | POST | /api/v1/creator/lessons/{id}/vocabularies/{vocabId} | Long, Long | - | ApiResponse<LessonDetailResponse> | 200 |
| 28 | CreatorLessonController | removeVocabularyFromLesson | DELETE | /api/v1/creator/lessons/{id}/vocabularies/{vocabId} | Long, Long | - | ApiResponse<LessonDetailResponse> | 200 |
| 29 | CreatorLessonController | reorderVocabulary | PUT | /api/v1/creator/lessons/{id}/reorder | Long | ReorderVocabRequest | ApiResponse<LessonDetailResponse> | 200 |
| 30 | CreatorLessonController | reorderVocabulary | POST | /api/v1/creator/lessons/{id}/reorder | Long | ReorderVocabRequest | ApiResponse<LessonDetailResponse> | 200 |
| 31 | CreatorLessonController | submitForModeration | POST | /api/v1/creator/lessons/{id}/submit | Long | - | ApiResponse<LessonDetailResponse> | 200 |
| 32 | CreatorLessonController | importPreview | POST | /api/v1/creator/lessons/import | - | file (multipart) | ApiResponse<ImportValidationReport> | 200 |
| 33 | CreatorLessonController | importConfirm | POST | /api/v1/creator/lessons/import/confirm | - | file, title (multipart) | ApiResponse<LessonDetailResponse> | 201 |
| 34 | ModeratorController | getPendingLessons | GET | /api/v1/moderator/lessons/pending | - | - | ApiResponse<PageResponse<ModerationQueueResponse>> | 200 |
| 35 | ModeratorController | getPendingLessonById | GET | /api/v1/moderator/lessons/{id} | Long | - | ApiResponse<LessonDetailResponse> | 200 |
| 36 | ModeratorController | approveLesson | POST | /api/v1/moderator/lessons/{id}/approve | Long | ApproveLessonRequest (opt) | ApiResponse<LessonDetailResponse> | 200 |
| 37 | ModeratorController | rejectLesson | POST | /api/v1/moderator/lessons/{id}/reject | Long | RejectLessonRequest | ApiResponse<LessonDetailResponse> | 200 |
| 38 | ModeratorController | getModeratorHistory | GET | /api/v1/moderator/history | - | - | ApiResponse<PageResponse<ModerationLogResponse>> | 200 |
| 39 | AdminAccountController | getAccounts | GET | /api/v1/admin/accounts | - | - | ApiResponse<PageResponse<AccountResponse>> | 200 |
| 40 | AdminAccountController | updateAccountStatus | PUT | /api/v1/admin/accounts/{id}/status | Long | UpdateAccountStatusRequest | ApiResponse<AccountResponse> | 200 |
| 41 | AdminRoleController | getRoles | GET | /api/v1/admin/roles | - | - | ApiResponse<List<RoleResponse>> | 200 |
| 42 | AdminRoleController | updateAccountRoles | PUT | /api/v1/admin/accounts/{id}/roles | Long | UpdateAccountRolesRequest | ApiResponse<AccountResponse> | 200 |
| 43 | AdminLessonController | getAdminLessons | GET | /api/v1/admin/lessons | - | - | ApiResponse<PageResponse<LessonSummaryResponse>> | 200 |
| 44 | AdminRadicalController | createRadical | POST | /api/v1/admin/radicals | - | CreateRadicalRequest | ApiResponse<RadicalDetailResponse> | 201 |
| 45 | AdminRadicalController | updateRadical | PUT | /api/v1/admin/radicals/{id} | Integer | UpdateRadicalRequest | ApiResponse<RadicalDetailResponse> | 200 |
| 46 | AdminRadicalController | deleteRadical | DELETE | /api/v1/admin/radicals/{id} | Integer | - | Void (No Content) | 204 |
| 47 | AdminVocabularyController | createVocabulary | POST | /api/v1/admin/vocabulary | - | CreateVocabularyRequest | ApiResponse<VocabularyDetailResponse> | 201 |
| 48 | AdminVocabularyController | updateVocabulary | PUT | /api/v1/admin/vocabulary/{id} | Long | UpdateVocabularyRequest | ApiResponse<VocabularyDetailResponse> | 200 |
| 49 | AdminVocabularyController | deleteVocabulary | DELETE | /api/v1/admin/vocabulary/{id} | Long | - | Void (No Content) | 204 |
| 50 | (Actuator) | health | GET | /actuator/health | - | - | {"status":"UP"} | 200 |
