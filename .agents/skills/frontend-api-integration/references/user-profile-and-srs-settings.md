# User Profile & SRS Settings — API Contract & UI Specification

> **Confidence Level**: `AUTHORITATIVE` — All field names, routes, validation constraints, and response structures verified against Java source code (file:line references below).

---

## 1. User Profile Management

### 1.1 Get Current User Profile

- **Endpoint**: `GET /api/v1/users/profile`
- **Authentication**: Required (JWT Bearer token)
- **Authorization**: Any authenticated user
- **Controller**: `UserProfileController.java:30-33`

**Response** (`ApiResponse<UserProfileResponse>`):
```json
{
  "code": "SUCCESS",
  "message": "Thao tác thành công",
  "data": {
    "userId": 1,
    "accountId": 1,
    "emailOrPhone": "learner@example.com",
    "fullName": "Nguyễn Văn A",
    "avatarUrl": "https://example.com/avatar.jpg",
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-09-01T14:20:00"
  },
  "errors": []
}
```

**Response fields** (verified from `UserProfileResponse.java:15-21`):

| JSON Field | Java Type | Notes |
|---|---|---|
| `userId` | `Long` | UserProfile primary key |
| `accountId` | `Long` | Associated Account ID |
| `emailOrPhone` | `String` | From `Account.emailOrPhone` |
| `fullName` | `String` | From `UserProfile.fullName` |
| `avatarUrl` | `String` | Nullable. `@JsonInclude(NON_NULL)` — omitted from JSON when null |
| `createdAt` | `LocalDateTime` | ISO 8601 format |
| `updatedAt` | `LocalDateTime` | ISO 8601 format |

> **`@JsonInclude(JsonInclude.Include.NON_NULL)`** (line 12): Null fields are **omitted** from the JSON response entirely, not serialized as `null`. Frontend MUST use optional chaining (`data?.avatarUrl`) and NOT assume all fields are always present.

> [!IMPORTANT]
> **Roles Source of Truth Separation (`AUTHORITATIVE`)**:
> `UserProfileResponse` does **NOT** contain a `roles` field. User roles are delivered exclusively in `AuthResponse.roles` upon authentication (`POST /api/v1/auth/login` or `POST /api/v1/auth/register`) and stored in client-side session state (`authManager`). The profile UI (`profile.html`) and navigation header must render role badges from the active authenticated session state, never by attempting to read `profile.roles` from `GET /api/v1/users/profile`.

---

### 1.2 Update Current User Profile

- **Endpoint**: `PUT /api/v1/users/profile`
- **Authentication**: Required (JWT Bearer token)
- **Authorization**: Any authenticated user
- **Controller**: `UserProfileController.java:36-39`

**Request Body** (`UpdateProfileRequest`, verified from `UpdateProfileRequest.java:10-17`):
```json
{
  "fullName": "Nguyễn Văn B",
  "avatarUrl": "https://example.com/new-avatar.jpg"
}
```

| JSON Field | Validation | Source |
|---|---|---|
| `fullName` | `@NotBlank`, `@Size(max = 100)` | `UpdateProfileRequest.java:12-13` |
| `avatarUrl` | Optional, `@Size(max = 500)` | `UpdateProfileRequest.java:16` |

**Response**: `ApiResponse<UserProfileResponse>` (same structure as GET, with message `"Cập nhật hồ sơ thành công"`).

**Error cases**:
- HTTP 400 `VALIDATION_ERROR` — if `fullName` is blank or exceeds 100 chars, `errors: ["fullName: Full name is required"]`
- HTTP 401 `UNAUTHORIZED` — invalid/expired JWT

---

### 1.3 Frontend Implementation Notes

**Page**: `profile.html`
**JS Module**: `frontend/js/pages/profile-page.js` (or equivalent under project's module structure)

**UI Requirements**:
1. Display current profile data on page load (GET request)
2. Editable form with fields: `fullName` (required), `avatarUrl` (optional)
3. Handle 3 UI states: Loading → Data → Error
4. On successful update, show success toast/notification
5. Validation error display: parse `errors[]` strings using `parseFieldErrors()` helper and display inline under corresponding form fields
6. `avatarUrl` field: simple text input for URL (no file upload — backend accepts URL string only)

---

## 2. SRS Study Settings Management

### 2.1 Get Current User SRS Settings

- **Endpoint**: `GET /api/v1/srs/settings`
- **Authentication**: Required (JWT Bearer token)
- **Authorization**: Any authenticated user
- **Controller**: `UserSrsSettingController.java:36-39`

**Response** (`ApiResponse<UserSrsSettingResponse>`):
```json
{
  "code": "SUCCESS",
  "message": "Thao tác thành công",
  "data": {
    "settingId": 1,
    "newCardsPerDay": 20,
    "maxReviewPerDay": 100
  },
  "errors": []
}
```

**Response fields** (verified from `UserSrsSettingResponse.java:10-12`):

| JSON Field | Java Type | Notes |
|---|---|---|
| `settingId` | `Long` | Setting record primary key |
| `newCardsPerDay` | `Integer` | Daily new card limit. Default: 20 (from `fromEntity()` line 25) |
| `maxReviewPerDay` | `Integer` | Daily max review limit. Default: 100 (from `fromEntity()` line 25) |

---

### 2.2 Update Current User SRS Settings

- **Endpoint**: `PUT /api/v1/srs/settings`
- **Authentication**: Required (JWT Bearer token)
- **Authorization**: Any authenticated user
- **Controller**: `UserSrsSettingController.java:48-52`

**Request Body** (`UpdateSrsSettingRequest`, verified from `UpdateSrsSettingRequest.java:10-18`):
```json
{
  "newCardsPerDay": 30,
  "maxReviewPerDay": 150
}
```

| JSON Field | Validation | Source |
|---|---|---|
| `newCardsPerDay` | `@NotNull`, `@Positive` (must be > 0) | `UpdateSrsSettingRequest.java:12-13` |
| `maxReviewPerDay` | `@NotNull`, `@Positive` (must be > 0) | `UpdateSrsSettingRequest.java:16-17` |

**Response**: `ApiResponse<UserSrsSettingResponse>` (same structure as GET, with message `"Cập nhật cấu hình học tập thành công"`).

**Error cases**:
- HTTP 400 `VALIDATION_ERROR` — if either field is null, zero, or negative. Example errors: `["newCardsPerDay: Số thẻ mới mỗi ngày phải là số nguyên dương lớn hơn 0"]`
- HTTP 401 `UNAUTHORIZED` — invalid/expired JWT

---

### 2.3 Frontend Implementation Notes

**Page**: `srs-settings.html` (accessible from learner navigation or profile page)
**JS Module**: `frontend/js/pages/srs-settings-page.js`

**UI Requirements**:
1. Display current settings on page load (GET request)
2. Two number inputs: `newCardsPerDay` and `maxReviewPerDay` — both must be positive integers
3. Client-side pre-validation: ensure values > 0 before submit
4. Handle 3 UI states: Loading → Data → Error
5. Success feedback: toast/banner confirming save
6. Show relationship context: explain what these settings control in the SRS review flow (daily limits)

---

## Source Basis

| Quy tắc / Khẳng định | Nguồn | Cấp độ | Trích dẫn |
|---|---|---|---|
| `UserProfileController` routes & HTTP methods | Java source code | AUTHORITATIVE | `UserProfileController.java:21,30,36` |
| `UserProfileResponse` JSON field names | Java getter methods (Jackson serialization) | AUTHORITATIVE | `UserProfileResponse.java:54-108` |
| `@JsonInclude(NON_NULL)` omits null fields | Jackson documentation, verified in source | AUTHORITATIVE | `UserProfileResponse.java:12` |
| `UpdateProfileRequest` validation constraints | Jakarta Validation annotations | AUTHORITATIVE | `UpdateProfileRequest.java:12-17` |
| `UserSrsSettingController` routes & HTTP methods | Java source code | AUTHORITATIVE | `UserSrsSettingController.java:22,36,48` |
| `UserSrsSettingResponse` JSON field names | Java getter methods | AUTHORITATIVE | `UserSrsSettingResponse.java:10-12` |
| Default SRS values (20 new, 100 review) | `fromEntity()` null fallback | AUTHORITATIVE | `UserSrsSettingResponse.java:25` |
| `UpdateSrsSettingRequest` `@Positive` constraint | Jakarta Validation `@Positive` means > 0 | AUTHORITATIVE | `UpdateSrsSettingRequest.java:12-17` |
| Validation error format `"${field}: ${message}"` | `GlobalExceptionHandler.java` | AUTHORITATIVE | `GlobalExceptionHandler.java:37` |
