# API SPECIFICATION — HỢP ĐỒNG RESTFUL API CHÍNH THỨC

> **Trạng thái thực thi hiện tại:**  
> - `CURRENT IMPLEMENTED APIS`:  
>   - `POST /api/v1/auth/register` — Đăng ký tài khoản (201 Created)  
>   - `POST /api/v1/auth/login` — Đăng nhập lấy JWT Bearer token (200 OK)  
>   - `GET /api/v1/users/profile` — Xem hồ sơ cá nhân của user đăng nhập (200 OK)  
>   - `PUT /api/v1/users/profile` — Cập nhật hồ sơ cá nhân của user đăng nhập (200 OK)  
> - `CURRENT IN PROGRESS`: `GET /api/v1/radicals/**` & Admin CRUD `/api/v1/admin/radicals/**` (Phase 4 — Task 4A.2)  
> - `PLANNED API`: Các phân hệ tiếp theo tuân thủ hợp đồng thiết kế đã phê duyệt bên dưới.  
> - **Tiền tố phiên bản thống nhất:** `/api/v1/...` trên toàn bộ hệ thống.  

---

## 1. QUY CHUẨN CHUNG (GENERAL SPECIFICATION)

### 1.1. Base URL & Content-Type
- **Base URL:** `http://localhost:8080/api/v1`
- **Mặc định:** `application/json; charset=UTF-8`
- **Upload File:** `multipart/form-data` (Chỉ áp dụng cho import Excel)

### 1.2. Chuẩn bọc phản hồi (ApiResponse Envelope)
Mọi response trả về client (kể cả thành công hay thất bại) đều phải tuân theo cấu trúc phong bì thống nhất:
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
| `SUCCESS` | 200 OK | Yêu cầu xử lý thành công. |
| `CREATED` | 201 Created | Tài nguyên mới được tạo thành công. |
| `VALIDATION_ERROR` | 400 Bad Request | Lỗi định dạng dữ liệu đầu vào (chi tiết trong `errors[]`). |
| `BAD_REQUEST` | 400 Bad Request | Yêu cầu không hợp lệ về mặt ngữ cảnh. |
| `UNAUTHORIZED` | 401 Unauthorized | Chưa đăng nhập, token không hợp lệ hoặc đã hết hạn. |
| `FORBIDDEN` | 403 Forbidden | Không có quyền truy cập tài nguyên (sai Role hoặc không phải chủ sở hữu). |
| `NOT_FOUND` | 404 Not Found | Không tìm thấy tài nguyên yêu cầu. |
| `CONFLICT` | 409 Conflict | Xung đột hoặc trùng lặp dữ liệu duy nhất. |
| `UNPROCESSABLE_ENTITY`| 422 Unprocessable | Vi phạm quy tắc nghiệp vụ (bài học rỗng, trạng thái bài học không hợp lệ, v.v.). |
| `FILE_TOO_LARGE` | 413 Payload Too Large | File import vượt quá dung lượng cho phép. |
| `FILE_TYPE_INVALID` | 415 Unsupported Media| File không đúng định dạng cho phép. |
| `INTERNAL_ERROR` | 500 Server Error | Lỗi nội bộ không lường trước phía server. |

---

## 2. DANH MỤC ENDPOINT KẾ HOẠCH (PLANNED REST APIS)

### 2.1. Phân hệ Xác thực (Authentication)
- `POST /api/v1/auth/register`: Đăng ký tài khoản mới (Public). Request: `RegisterRequest` (`emailOrPhone`, `password`, `fullName`). Trả về: `201 Created`.
- `POST /api/v1/auth/login`: Đăng nhập (Public). Request: `LoginRequest` (`emailOrPhone`, `password`). Trả về: `200 OK` kèm `LoginResponse` (`token`, `tokenType`, `roles`, `fullName`).

### 2.2. Phân hệ Tra cứu Bộ thủ & Từ vựng (Public Content)
- `GET /api/v1/radicals`: Danh sách 214 Bộ thủ có phân trang (Public). Params: `page`, `size`. Trả về: `ApiResponse<PageResponse<RadicalDto>>`.
- `GET /api/v1/radicals/{id}`: Chi tiết một bộ thủ (Public). Trả về: `ApiResponse<RadicalDto>`.
- `GET /api/v1/vocabulary`: Danh sách Từ vựng có phân trang và tìm kiếm (Public). Params: `page`, `size`, `search`. Trả về: `ApiResponse<PageResponse<VocabularyDto>>`.
- `GET /api/v1/vocabulary/{id}`: Chi tiết một từ vựng kèm danh sách bộ thủ cấu thành (Public). Trả về: `ApiResponse<VocabularyDto>`.

### 2.3. Phân hệ Bài học công khai (Public Lessons)
- `GET /api/v1/lessons`: Danh sách các bài học đã được phê duyệt (`status = 'Approved'`) (Public). Params: `page`, `size`. Trả về: `ApiResponse<PageResponse<LessonDto>>`.
- `GET /api/v1/lessons/{id}`: Chi tiết bài học kèm danh sách từ vựng trong bài học (Public). Trả về: `ApiResponse<LessonDto>`.

### 2.4. Phân hệ Tác giả bài học (Creator Management)
*Yêu cầu quyền: `ROLE_CREATOR` hoặc `ROLE_ADMIN`*
- `POST /api/v1/creator/lessons/import`: Bước 1: Upload và validate file Excel xem trước (Multipart `file`). Trả về: `ApiResponse<ImportValidationReport>`.
- `POST /api/v1/creator/lessons/import/confirm`: Bước 2: Xác nhận tạo bài học mới kèm từ vựng từ Excel. Params/Body: `title`, `file`. Trả về: `201 Created` kèm `ApiResponse<LessonDto>`.
- `GET /api/v1/creator/lessons`: Danh sách các bài học do chính creator tạo ra. Params: `page`, `size`. Trả về: `ApiResponse<PageResponse<LessonDto>>`.
- `GET /api/v1/creator/lessons/{id}`: Chi tiết bài học của creator. Trả về: `ApiResponse<LessonDto>`.
- `POST /api/v1/creator/lessons/{id}/submit`: Gửi bài học lên hàng đợi kiểm duyệt (`Draft`/`Rejected` -> `Pending`). Trả về: `200 OK`.

### 2.5. Phân hệ Kiểm duyệt (Moderation)
*Yêu cầu quyền: `ROLE_MODERATOR` hoặc `ROLE_ADMIN`*
- `GET /api/v1/moderator/lessons/pending`: Danh sách các bài học đang chờ duyệt (`status = 'Pending'`). Params: `page`, `size`. Trả về: `ApiResponse<PageResponse<LessonDto>>`.
- `GET /api/v1/moderator/lessons/{id}`: Chi tiết bài học cần kiểm duyệt. Trả về: `ApiResponse<LessonDto>`.
- `POST /api/v1/moderator/lessons/{id}/approve`: Phê duyệt bài học (`Pending` -> `Approved`). Trả về: `200 OK`.
- `POST /api/v1/moderator/lessons/{id}/reject`: Từ chối bài học (`Pending` -> `Rejected`). Body: `RejectRequest` (`rejectionReason`, `flaggedFields`). Trả về: `200 OK`.
- `GET /api/v1/moderator/history`: Xem lịch sử các lượt kiểm duyệt của bản thân. Params: `page`, `size`. Trả về: `ApiResponse<PageResponse<ModerationLogDto>>`.

### 2.6. Phân hệ Flashcard & Ôn tập SRS
*Yêu cầu xác thực: Authenticated User (Mọi vai trò đăng nhập)*
- `GET /api/v1/flashcards/vocabulary`: Lấy danh sách thẻ từ vựng đến hạn ôn tập hôm nay theo thuật toán SRS. Param: `limit`. Trả về: `ApiResponse<List<VocabularyDto>>`.
- `GET /api/v1/flashcards/radicals`: Lấy danh sách thẻ bộ thủ đến hạn ôn tập hôm nay. Param: `limit`. Trả về: `ApiResponse<List<RadicalDto>>`.
- `POST /api/v1/srs/review`: Gửi kết quả đánh giá thẻ (Rating 1–4) để tính toán SRS và lưu log. Body: `ReviewRequest` (`itemType`, `itemId`, `rating`, `reviewTimeSeconds`). Trả về: `200 OK`.
- `GET /api/v1/srs/settings`: Lấy cấu hình học tập cá nhân. Trả về: `ApiResponse<SrsSettingDto>`.
- `PUT /api/v1/srs/settings`: Cập nhật cấu hình học tập cá nhân (`newCardsPerDay`, `maxReviewPerDay`). Trả về: `ApiResponse<SrsSettingDto>`.

### 2.7. Phân hệ Ghi chú cá nhân (Personal Notes)
*Yêu cầu xác thực: Authenticated User*
- `GET /api/v1/notes?vocabId={id}`: Lấy danh sách ghi chú của user cho từ vựng. Trả về: `ApiResponse<List<PersonalNoteDto>>`.
- `POST /api/v1/notes`: Tạo ghi chú mới cho từ vựng (nội dung tối đa 500 ký tự). Body: `PersonalNoteRequest` (`vocabId`, `content`). Trả về: `201 Created`.
- `PUT /api/v1/notes/{id}`: Cập nhật nội dung ghi chú. Body: `PersonalNoteRequest`. Trả về: `200 OK`.
- `DELETE /api/v1/notes/{id}`: Xóa ghi chú cá nhân. Trả về: `204 No Content`.

### 2.8. Phân hệ Quản trị hệ thống (Admin Management)
*Yêu cầu quyền: `ROLE_ADMIN`*
- `GET /api/v1/admin/accounts`: Danh sách tài khoản người dùng có phân trang. Params: `page`, `size`. Trả về: `ApiResponse<PageResponse<AccountDto>>`.
- `PUT /api/v1/admin/accounts/{id}/status`: Khóa hoặc kích hoạt tài khoản (`Active`, `Inactive`, `Banned`). Body: `UpdateAccountStatusRequest`. Trả về: `ApiResponse<AccountDto>`.
- `GET /api/v1/admin/roles`: Danh mục tất cả vai trò trong hệ thống. Trả về: `ApiResponse<List<RoleDto>>`.
- `PUT /api/v1/admin/accounts/{id}/roles`: Phân quyền / gán danh sách vai trò cho tài khoản. Body: `UpdateAccountRolesRequest`. Trả về: `ApiResponse<AccountDto>`.
- `CRUD /api/v1/admin/radicals`: Thêm, sửa, xóa 214 Bộ thủ danh mục gốc.
- `CRUD /api/v1/admin/vocabulary`: Thêm, sửa, xóa Từ vựng danh mục gốc.
- `GET /api/v1/admin/lessons`: Quản lý toàn bộ danh sách bài học hệ thống.
