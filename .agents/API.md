# API SPECIFICATION — HỢP ĐỒNG RESTFUL API CHÍNH THỨC

> **Trạng thái thực thi hiện tại:**  
> - `CURRENT IMPLEMENTED BACKEND APIS`: **100% Backend REST APIs (Phase 3 → Phase 8D) & Hardening Verification (Tasks R1 → R3.11 & DEC-42)** đã hoàn thành, tích hợp Spring Security, DTO validation, pessimistic row-level locking, pagination, rate limiting, security headers và kiểm thử hồi quy đầy đủ (**`1101/1101 tests PASS, Failures: 0, Errors: 0, Skipped: 0`** trên Testcontainers MySQL 8.4).  
> - `CURRENT IMPLEMENTATION STAGE`: **Backend Final Quality Gate Cleared & Sealed (DEC-41) → Phase 9 Frontend UNBLOCKED / Sẵn sàng triển khai Task 9A.1**.  
> - **Tiền tố phiên bản thống nhất:** `/api/v1/...` trên toàn bộ hệ thống; `/actuator/health` cho health probe.

---

## 1. QUY CHUẨN CHUNG (GENERAL SPECIFICATION)

### 1.1. Base URL & Content-Type
- **Base URL:** `http://localhost:8080/api/v1`
- **Mặc định:** `application/json; charset=UTF-8`
- **Upload File:** `multipart/form-data` (Chỉ áp dụng cho import Excel)

### 1.2. Chuẩn bọc phản hồi (ApiResponse Envelope)
Hầu hết các phản hồi API nghiệp vụ sử dụng phong bì `ApiResponse<T>`. Các phản hồi đặc thù theo từng endpoint được ưu tiên áp dụng. Các phản hồi HTTP 204 No Content không chứa body và do đó không chứa `ApiResponse<T>`:
```json
{
  "code": "SUCCESS",
  "message": "Thao tác thành công",
  "errors": [],
  "data": {}
}
```

### 1.3. Chuẩn phân trang (PageResponse)
```json
{
  "code": "SUCCESS",
  "message": "Tải danh sách thành công",
  "errors": [],
  "data": {
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "items": []
  }
}
```

### 1.4. Bảng mã lỗi chuẩn (Standard Error Codes)
| Code | HTTP Status | Ý nghĩa nghiệp vụ |
| :--- | :--- | :--- |
| `SUCCESS` | 200 OK / 201 Created | Yêu cầu xử lý thành công. (Mọi response 200 OK và 201 Created thành công thực tế đều trả `code: "SUCCESS"` qua `ApiResponse.success`). |
| `CREATED` | 201 Created | Enum backend trong `ErrorCode.java`. Lưu ý: Trên thực tế các controller 201 hiện tại đều gọi `ApiResponse.success` trả `code: "SUCCESS"`. |
| `VALIDATION_ERROR` | 400 Bad Request | Lỗi định dạng dữ liệu đầu vào (chi tiết trong `errors[]`). |
| `BAD_REQUEST` | 400 Bad Request | Yêu cầu không hợp lệ về mặt ngữ cảnh / vi phạm chính sách bảo vệ. |
| `UNAUTHORIZED` | 401 Unauthorized | Chưa đăng nhập, token không hợp lệ, phiên làm việc hết hạn hoặc đã bị thu hồi (`authorization_version`). |
| `FORBIDDEN` | 403 Forbidden | Không có quyền truy cập tài nguyên (sai Role hoặc không phải chủ sở hữu). |
| `NOT_FOUND` | 404 Not Found | Không tìm thấy tài nguyên yêu cầu. |
| `CONFLICT` | 409 Conflict | Xung đột hoặc trùng lặp dữ liệu duy nhất. |
| `TOO_MANY_REQUESTS` | 429 Too Many Requests | Vượt quá ngưỡng giới hạn tần suất đăng nhập (Rate limit). Backend không trả header `Retry-After`. |
| `UNPROCESSABLE_ENTITY`| 422 Unprocessable | Vi phạm quy tắc nghiệp vụ (bài học rỗng, trạng thái bài học không hợp lệ, v.v.). |
| `FILE_TOO_LARGE` | 413 Payload Too Large | File import vượt quá dung lượng cho phép. |
| `FILE_TYPE_INVALID` | 415 Unsupported Media| File không đúng định dạng cho phép. |
| `INTERNAL_ERROR` | 500 Server Error | Lỗi nội bộ không lường trước phía server. |

---

## 2. DANH MỤC REST APIS HỆ THỐNG (SYSTEM REST APIS CONTRACT)

> **Thống kê Hợp đồng Chuẩn hóa:**
> - **15 Controller classes** thuộc package `com.elearning.controller`.
> - **48 Java handler methods** trực tiếp xử lý các request nghiệp vụ.
> - **49 HTTP method+path mappings** (do `CreatorLessonController.reorderVocabulary` hỗ trợ đồng thời cả `PUT` và `POST` trên `/{id}/reorder`).
> - **1 Actuator health probe** (`GET /actuator/health` — cơ chế chẩn đoán kỹ thuật / verification probe, không phải phụ thuộc nghiệp vụ).
> *(Lưu ý lịch sử: Con số "45 endpoints" xuất hiện trong tài liệu kiểm toán Task R3.4 phản ánh phạm vi các endpoint nghiệp vụ tại thời điểm đó trước khi bổ sung và hợp nhất các route quản trị và tra cứu của Module 8D).*  
> Toàn bộ API nghiệp vụ trả về envelope `ApiResponse<T>`, ngoại trừ 3 endpoint DELETE (`DELETE /api/v1/creator/lessons/{id}`, `DELETE /api/v1/admin/radicals/{id}`, `DELETE /api/v1/admin/vocabulary/{id}`) trả về HTTP 204 No Content không có body.

### 2.1. Phân hệ Xác thực & Giám sát (Authentication & Ops)
- `POST /api/v1/auth/register`: Đăng ký tài khoản mới (Public). Request: `RegisterRequest` (`emailOrPhone`, `password`, `fullName`). Trả về: `201 Created`.
- `POST /api/v1/auth/login`: Đăng nhập (Public, kèm Rate Limiter 10 req/60s). Request: `LoginRequest` (`emailOrPhone`, `password`). Trả về: `200 OK` kèm `AuthResponse` (`token`, `type`, `accountId`, `emailOrPhone`, `fullName`, `roles`).
- `GET /api/v1/users/profile`: Lấy thông tin hồ sơ cá nhân của người dùng hiện tại (Authenticated). Trả về: `ApiResponse<UserProfileResponse>` (`userId`, `accountId`, `emailOrPhone`, `fullName`, `avatarUrl`, `createdAt`, `updatedAt`).
- `PUT /api/v1/users/profile`: Cập nhật thông tin hồ sơ cá nhân (Authenticated). Body: `UpdateProfileRequest` (`fullName`, `avatarUrl`). Trả về: `ApiResponse<UserProfileResponse>`.
- `GET /actuator/health`: Endpoint kiểm tra trạng thái hoạt động của hệ thống (Public). Trả về: `{"status":"UP"}`.

### 2.2. Phân hệ Tra cứu Bộ thủ & Từ vựng (Public Content)
- `GET /api/v1/radicals`: Danh sách 214 Bộ thủ có phân trang (Public). Params: `page`, `size`. Trả về: `ApiResponse<PageResponse<RadicalResponse>>`.
- `GET /api/v1/radicals/{id}`: Chi tiết một bộ thủ (Public). Path variable `id` kiểu `Integer` (1..214). Trả về: `ApiResponse<RadicalDetailResponse>`.
- `GET /api/v1/vocabulary`: Danh sách Từ vựng có phân trang và tìm kiếm (Public). Params: `page`, `size`, `search` (sử dụng query param `search`, không dùng `q`). Trả về: `ApiResponse<PageResponse<VocabularyResponse>>`.
- `GET /api/v1/vocabulary/{id}`: Chi tiết một từ vựng kèm danh sách bộ thủ cấu thành (Public). Trả về: `ApiResponse<VocabularyDetailResponse>`.

### 2.3. Phân hệ Bài học công khai (Public Lessons)
- `GET /api/v1/lessons`: Danh sách các bài học đã được phê duyệt (`status = 'Approved'`) (Public). Params: `page`, `size`. Trả về: `ApiResponse<PageResponse<LessonSummaryResponse>>`.
- `GET /api/v1/lessons/{id}`: Chi tiết bài học kèm danh sách từ vựng trong bài học (Public). Trả về: `ApiResponse<LessonDetailResponse>`.

### 2.4. Phân hệ Tác giả bài học (Creator Management)
*Yêu cầu quyền: `ROLE_CREATOR` hoặc `ROLE_ADMIN` (JSON role: `Creator` hoặc `Admin`)*
- `POST /api/v1/creator/lessons`: Tạo bài học mới ở trạng thái bản nháp (`Draft`). Body: `CreateLessonRequest` (`title` $\le 200$, `excelFileUrl`, `vocabularyIds`). Trả về: `201 Created` kèm `ApiResponse<LessonDetailResponse>`.
- `GET /api/v1/creator/lessons`: Danh sách các bài học do chính creator tạo ra. Params: `page`, `size`. Trả về: `ApiResponse<PageResponse<LessonSummaryResponse>>`.
- `GET /api/v1/creator/lessons/{id}`: Chi tiết bài học của creator. Trả về: `ApiResponse<LessonDetailResponse>`.
- `PUT /api/v1/creator/lessons/{id}`: Cập nhật thông tin bài học (chỉ áp dụng cho `Draft` hoặc `Rejected`). Body: `UpdateLessonRequest` (`title` $\le 200$, `excelFileUrl`). Trả về: `ApiResponse<LessonDetailResponse>`.
- `DELETE /api/v1/creator/lessons/{id}`: Xóa bài học thuộc sở hữu của creator (`Draft` hoặc `Rejected` chưa có lịch sử kiểm duyệt). Trả về: `204 No Content` (không có body; không gọi `response.json()`).
- `POST /api/v1/creator/lessons/{id}/vocabularies/{vocabId}`: Gắn từ vựng vào bài học (backend tự động gán thứ tự tiếp theo ở cuối danh sách; không nhận `orderIndex` từ client). Trả về: `ApiResponse<LessonDetailResponse>`.
- `DELETE /api/v1/creator/lessons/{id}/vocabularies/{vocabId}`: Gỡ từ vựng khỏi bài học và tái lập thứ tự liên tục 1..N. Trả về: `ApiResponse<LessonDetailResponse>`.
- `PUT /api/v1/creator/lessons/{id}/reorder` (hỗ trợ cả `POST`): Cập nhật lại thứ tự từ vựng trong bài học. Body: `ReorderVocabRequest` (`orderedVocabIds` hoặc `items`). Trả về: `ApiResponse<LessonDetailResponse>`.
- `POST /api/v1/creator/lessons/import`: Upload và validate file Excel xem trước (Multipart `file`, max 10MB, max 5,000 data rows). Trả về: `ApiResponse<ImportValidationReport>` (HTTP 200 OK ngay cả khi file có lỗi dữ liệu dòng; zero DB mutations).
- `POST /api/v1/creator/lessons/import/confirm`: Xác nhận tạo bài học mới kèm từ vựng từ Excel. Params/Body: `title` ($\le 100$), `file`. Trả về: `201 Created` kèm `ApiResponse<LessonDetailResponse>`.
- `POST /api/v1/creator/lessons/{id}/submit`: Gửi bài học lên hàng đợi kiểm duyệt (`Draft`/`Rejected` -> `Pending`). Trả về: `200 OK` kèm `ApiResponse<LessonDetailResponse>`.

### 2.5. Phân hệ Kiểm duyệt (Moderation)
*Yêu cầu quyền: `ROLE_MODERATOR` hoặc `ROLE_ADMIN` (JSON role: `Moderator` hoặc `Admin`)*
- `GET /api/v1/moderator/lessons/pending`: Danh sách các bài học đang chờ duyệt (`status = 'Pending'`). Params: `page`, `size`. Trả về: `ApiResponse<PageResponse<ModerationQueueResponse>>`.
- `GET /api/v1/moderator/lessons/{id}`: Chi tiết bài học cần kiểm duyệt. Trả về: `ApiResponse<LessonDetailResponse>`.
- `POST /api/v1/moderator/lessons/{id}/approve`: Phê duyệt bài học (`Pending` -> `Approved`). Body: `ApproveLessonRequest` (`note`, optional). Trả về: `200 OK` kèm `ApiResponse<LessonDetailResponse>`.
- `POST /api/v1/moderator/lessons/{id}/reject`: Từ chối bài học (`Pending` -> `Rejected`). Body: `RejectLessonRequest` (`rejectionReason` $\le 500$, `flaggedFields`). Trả về: `200 OK` kèm `ApiResponse<LessonDetailResponse>`.
- `GET /api/v1/moderator/history`: Xem lịch sử các lượt kiểm duyệt (Moderator xem lịch sử của chính mình, Admin xem toàn bộ lịch sử hệ thống). Params: `page`, `size`. Trả về: `ApiResponse<PageResponse<ModerationLogResponse>>`.

### 2.6. Phân hệ Flashcard & Ôn tập SRS (Spaced Repetition System)
*Yêu cầu xác thực: Authenticated User (Mọi vai trò đăng nhập)*
- `GET /api/v1/srs/due`: Lấy danh sách thẻ đến hạn ôn tập hôm nay theo thuật toán SRS. Params: `itemType` (optional: `VOCABULARY`, `RADICAL`), `limit`. Trả về: `ApiResponse<List<DueCardResponse>>`. (Lưu ý: `DueCardResponse.strokeCount = null` luôn hardcoded; không có `audioUrl`, `videoWritingUrl`, `pinyinRaw`, `radicals[]`; client phải gọi endpoint detail tương ứng khi cần dữ liệu media).
- `GET /api/v1/srs/new-cards`: Khám phá các thẻ từ vựng mới thuộc bài học đã được phê duyệt (`status = 'Approved'`) theo thứ tự `orderIndex ASC` (R1 Engine). Params: `lessonId` (bắt buộc), `limit` (tùy chọn, tối đa theo hạn mức từ mới còn lại trong ngày). Phân tách Candidate Preview (idempotent, không tạo CardProgress trước khi người học review) và First Review. Trả về: `ApiResponse<List<DueCardResponse>>` với `isNew = true`.
- `GET /api/v1/srs/lessons/{lessonId}/new-cards`: Endpoint alias tương thích của `GET /api/v1/srs/new-cards`. Trả về: `ApiResponse<List<DueCardResponse>>`.
- `POST /api/v1/srs/review`: Gửi kết quả đánh giá thẻ (Rating 1–4, kèm số giây phản xạ `reviewTimeSeconds`) để tính toán SM-2, cập nhật hoặc khởi tạo `CARD_PROGRESS` và lưu vết bất biến `REVIEW_LOG`. Thực thi chốt chặn **Approved-Lesson Eligibility Enforcement (R2.1)**: Khi học từ mới lần đầu (`isNewCard = true && itemType = 'VOCABULARY'`), bắt buộc từ vựng phải thuộc ít nhất một bài học có trạng thái `Approved`; nếu không, trả về HTTP 422 `UNPROCESSABLE_ENTITY` ("Từ vựng chưa thuộc bất kỳ bài học nào đã được phê duyệt để học mới"). Bộ thủ Kangxi 214 thuộc danh mục độc lập không bị ràng buộc bài học. Thực thi chốt chặn **Due-Only Server Enforcement (R2)**: Từ chối các thẻ chưa đến hạn ôn tập (`nextReviewAt > now`) với mã lỗi HTTP 409 `CONFLICT` ("Thẻ chưa đến hạn ôn tập"). Thẻ mới hợp lệ lần đầu được phép học và trừ hạn mức `newCardsPerDay`; thẻ ôn tập lại hoặc học lại trong ngày (`Again` với `interval = 0`, `nextReviewAt = now`) được phép ôn tập ngay và không tính vào hạn mức từ mới. Các `CardProgress` đã tồn tại từ trước luôn được bảo toàn quyền ôn tập ngay cả khi bài học sau đó bị thay đổi trạng thái. Body: `ReviewCardRequest` (`itemType`, `itemId`, `rating`, `reviewTimeSeconds`). Trả về: `ApiResponse<DueCardResponse>`.
- `GET /api/v1/srs/stats`: Lấy thống kê học tập và hạn mức trong ngày của người học (`cardsDue`, `reviewsToday`, `newCardsLimit`, `maxReviewLimit`, `newCardsToday`). Trả về: `ApiResponse<StudyStatsResponse>`.
- `GET /api/v1/srs/settings`: Lấy cấu hình học tập cá nhân. Trả về: `ApiResponse<UserSrsSettingResponse>`.
- `PUT /api/v1/srs/settings`: Cập nhật cấu hình học tập cá nhân (`newCardsPerDay`, `maxReviewPerDay`). Trả về: `ApiResponse<UserSrsSettingResponse>`.

### 2.7. Phân hệ Ghi chú cá nhân (Personal Notes)
*Yêu cầu xác thực: Authenticated User (Mọi vai trò đăng nhập; sở hữu và bảo vệ theo từng user)*
- `GET /api/v1/vocabularies/{vocabId}/notes`: Lấy danh sách ghi chú của người học cho từ vựng cụ thể (có phân trang). Params: `page` (0-indexed, mặc định 0), `size` (mặc định 20, server giới hạn tối đa 100). Trả về: `ApiResponse<PageResponse<PersonalNoteResponse>>`.
- `POST /api/v1/vocabularies/{vocabId}/notes`: Tạo ghi chú mới cho từ vựng (nội dung tối đa 500 ký tự). Body: `PersonalNoteRequest` (`content`). Trả về: `201 Created` kèm `ApiResponse<PersonalNoteResponse>`.
- `PUT /api/v1/notes/{id}`: Cập nhật nội dung ghi chú của bản thân. Body: `PersonalNoteRequest` (`content`). Trả về: `200 OK` kèm `ApiResponse<PersonalNoteResponse>`.
- `DELETE /api/v1/notes/{id}`: Xóa ghi chú cá nhân của bản thân. Trả về: `200 OK` kèm `ApiResponse<Void>` (`{"code":"SUCCESS","message":"Xóa ghi chú thành công","data":null,"errors":[]}`).

### 2.8. Phân hệ Quản trị hệ thống (Admin Management)
*Yêu cầu quyền: `ROLE_ADMIN` (JSON role: `Admin`)*  
*Ghi chú tra cứu danh mục: Quản trị viên duyệt danh mục Bộ thủ và Từ vựng thông qua các endpoint công khai `GET /api/v1/radicals` và `GET /api/v1/vocabulary?search=`. Hệ thống KHÔNG CÓ các endpoint `GET /api/v1/admin/radicals` hay `GET /api/v1/admin/vocabulary`.*
- `GET /api/v1/admin/accounts`: Danh sách tài khoản người dùng có phân trang, lọc theo trạng thái và tìm kiếm. Params: `status`, `search`, `page`, `size`. Trả về: `ApiResponse<PageResponse<AccountResponse>>`.
- `PUT /api/v1/admin/accounts/{id}/status`: Khóa hoặc kích hoạt tài khoản (`Active`, `Inactive`, `Banned`). Body: `UpdateAccountStatusRequest` (`status`). Trả về: `ApiResponse<AccountResponse>` (bảo vệ chống tự khóa tài khoản).
- `GET /api/v1/admin/roles`: Danh mục tất cả vai trò trong hệ thống (`Admin`, `Creator`, `Moderator`, `Learner`). Trả về: `ApiResponse<List<RoleResponse>>`.
- `PUT /api/v1/admin/accounts/{id}/roles`: Phân quyền / gán danh sách vai trò cho tài khoản. Body: `UpdateAccountRolesRequest` (`roles` — danh sách tên role như `["Creator"]`, `["Admin"]`). Trả về: `ApiResponse<AccountResponse>` (bảo vệ chống tự tước quyền Admin; đồng bộ hóa và tăng `authorization_version` để thu hồi token cũ ngay lập tức).
- `GET /api/v1/admin/lessons`: Quản lý toàn bộ danh sách bài học hệ thống qua mọi trạng thái (`Draft`, `Pending`, `Approved`, `Rejected`). Params: `status`, `page`, `size`. Trả về: `ApiResponse<PageResponse<LessonSummaryResponse>>`.
- `POST /api/v1/admin/radicals`: Tạo bộ thủ mới. Request: `CreateRadicalRequest` (`character`, `pinyin`, `meaningHanViet`, `meaningVi`, `audioUrl`, `videoWritingUrl`). Trả về: `201 Created` kèm `ApiResponse<RadicalDetailResponse>`.
- `PUT /api/v1/admin/radicals/{id}`: Cập nhật thông tin bộ thủ (Path variable `id` kiểu `Integer`). Request: `UpdateRadicalRequest` (`character`, `pinyin`, `meaningHanViet`, `meaningVi`, `audioUrl`, `videoWritingUrl`). Trả về: `200 OK` kèm `ApiResponse<RadicalDetailResponse>`.
- `DELETE /api/v1/admin/radicals/{id}`: Xóa bộ thủ gốc (Path variable `id` kiểu `Integer`). Trả về: `204 No Content` (không có response body; không gọi `response.json()`).
- `POST /api/v1/admin/vocabulary`: Tạo từ vựng mới. Request: `CreateVocabularyRequest` (`hanzi`, `pinyin`, `pinyinRaw`, `meaningHanViet`, `meaningVi`, `audioUrl`, `videoWritingUrl`, `exampleSentence`, `exampleTranslation`, `radicalIds`). Trả về: `201 Created` kèm `ApiResponse<VocabularyDetailResponse>`.
- `PUT /api/v1/admin/vocabulary/{id}`: Cập nhật từ vựng. Request: `UpdateVocabularyRequest` (`hanzi`, `pinyin`, `pinyinRaw`, `meaningHanViet`, `meaningVi`, `audioUrl`, `videoWritingUrl`, `exampleSentence`, `exampleTranslation`, `radicalIds`). Trả về: `200 OK` kèm `ApiResponse<VocabularyDetailResponse>`.
- `DELETE /api/v1/admin/vocabulary/{id}`: Xóa từ vựng gốc (Thực thi chốt chặn toàn vẹn tham chiếu `DEC-34` & `DEC-35`: từ chối với HTTP 409 `CONFLICT` nếu từ vựng đang liên kết với bài học, có ghi chú cá nhân, hoặc có tiến trình `CardProgress`/`ReviewLog`; chỉ cho phép xóa khi hoàn toàn không có tham chiếu phụ thuộc, cascade dọn dẹp `VOCAB_RADICAL`). Trả về: `204 No Content` (không có response body; không gọi `response.json()`).
