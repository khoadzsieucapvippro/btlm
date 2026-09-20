---
name: frontend-api-integration
description: Quản lý kiến trúc giao tiếp HTTP API với Spring Boot, JWT & thu hồi phiên (authorization_version), chuẩn hóa phong bì ApiResponse, 3 trạng thái giao diện (Loading/Empty/Error), và quy trình Import Excel 2 bước.
---

# 1. Purpose
Kiểm soát toàn bộ tầng kết nối mạng giữa giao diện Vanilla JS và backend Spring Boot REST API (`/api/v1/*`). Đóng gói tập trung qua `apiClient` (`frontend/js/api/api.js`), tự động tiêm Bearer token, giải mã phong bì chuẩn `ApiResponse<T>`, xử lý mã lỗi HTTP, đồng bộ hóa vòng đời phiên người dùng (`authorization_version` DEC-42), và thực thi quy trình nhập liệu bài học Excel 2 bước an toàn.

# 2. When to Use
- Viết code gọi API từ Frontend (GET, POST, PUT, DELETE, Upload multipart).
- Cấu hình và sử dụng Central HTTP Client (`frontend/js/api/api.js`).
- Xử lý các mã lỗi máy chủ (400, 401, 403, 404, 409, 413, 415, 422, 429, 500) và sự cố mạng.
- Quản lý phiên đăng nhập, đăng ký, lưu trữ token, và đồng bộ hóa đa tab (Multi-tab logout).
- Triển khai các trạng thái giao diện (Loading spinner, Empty state, Error alert có retry, Submitting).
- Xây dựng form nhập liệu có kiểm tra tính hợp lệ (Validation) và xử lý upload file Excel 2 bước.

# 3. Required Inputs
- File hợp đồng API chuẩn xác: `references/api-contract.md` và `PROJECT-CONTRACT.md`.
- File Java Controller (`backend/src/main/java/com/elearning/controller/*Controller.java`) và DTO Request/Response (`backend/src/main/java/com/elearning/dto/*`).
- Base URL Backend có thể cấu hình linh hoạt (mặc định same-origin relative path `/api/v1`, cho phép override qua đối tượng cấu hình triển khai chỉ-đọc `window.__ENV__?.API_BASE_URL`).

# 4. Source of Truth
Tuân thủ nghiêm ngặt thứ bậc thẩm quyền:
1. **Mã nguồn Java Controllers và DTOs thực tế** (`backend/src/main/java/com/elearning/...`).
2. **Kiểm thử tích hợp Backend MockMvc / Testcontainers** (`backend/src/test/java/...`).
3. **Migration Flyway CSDL** (`V1`..`V7`).
4. **Hợp đồng API chuẩn** (`references/api-contract.md` và `PROJECT-CONTRACT.md`).
5. **Tài liệu `.agents/` khác** (nếu xung đột với code Java, code Java là chân lý tối cao).

# 5. Non-Negotiable Rules
- **Central Gateway Invariant**: Mọi request mạng MUST đi qua `apiClient()`. Tuyệt đối cấm gọi `window.fetch()` trần trụi trong các file script trang.
- **Auth Token Field**: Field serialized thực tế trong JSON response đăng nhập/đăng ký là `response.data.token` (NOT `accessToken`). Verified by `AuthIntegrationTests.java:89`.
- **Identity Session Boundary**: Không bao giờ gửi `userId` hay `accountId` trong request body để mạo nhận danh tính; server trích xuất từ JWT Bearer token trong `SecurityContextHolder`.
- **Delete Status Handling**: `DELETE /api/v1/admin/radicals/{id}`, `DELETE /api/v1/admin/vocabulary/{id}`, và `DELETE /api/v1/creator/lessons/{id}` trả về **HTTP 204 No Content** (không có body; cấm gọi `response.json()`). Chỉ `DELETE /api/v1/notes/{id}` trả về **HTTP 200 OK** với `ApiResponse<Void>`.
- **Idempotent Retry Only**: Chỉ request `GET` mới được tự động retry (tối đa 1 lần) khi gặp lỗi mạng tầng truyền tải (`TypeError`). Các phương thức Mutation (`POST`, `PUT`, `DELETE`) tuyệt đối KHÔNG được retry tự động.
- **Nullability Discipline**: Phân biệt rõ rệt giữa DTO có `@JsonInclude(NON_NULL)` (field bị khuyết / `undefined`) và DTO trả về `null` tường minh. Dùng nullish coalescing (`??`) để xử lý an toàn.

# 6. Workflow
1. **Tra cứu hợp đồng**: Mở `references/api-contract.md` và file Controller tương ứng, đối chiếu method, route, request body, query params và response DTO.
2. **Gọi qua apiClient**:
   ```javascript
   const data = await apiClient('/lessons', { method: 'GET' });
   ```
3. **Xử lý giao diện 3 trạng thái**:
   - Trước khi gửi: Bật loading spinner, gán cờ `isSubmitting = true`, disable nút bấm.
   - Khi thành công: Render dữ liệu bằng DOM an toàn (hoặc hiển thị empty state nếu `items.length === 0`).
   - Khi lỗi: Bắt `ApiError`, hiển thị banner lỗi kèm nút "Thử lại", gỡ disable nút bấm trong khối `finally`.
4. **Ánh xạ lỗi Validation (HTTP 400)**: Gọi `parseFieldErrors(err.errors)` để tách `"${field}: ${message}"`, gán class `.is-invalid` vào input và điền text vào `.invalid-feedback`.
5. **Quy trình Excel Import 2 bước**:
   - Bước 1: Gửi file lên `POST /api/v1/creator/lessons/import`, nhận `ImportValidationReport`, hiển thị banner "Zero DB mutations".
   - Bước 2: Chỉ khi người dùng bấm Xác nhận kèm tên bài học, gửi `POST /api/v1/creator/lessons/import/confirm` lưu nguyên tử.

# 7. Security Constraints
- **Client Storage Untrusted**: `localStorage` là nơi lưu trữ tiện ích, không phải vùng tin cậy bảo mật. Mọi quyền hạn phải được Spring Security xác thực tại server.
- **Instant Revocation (DEC-42)**: Khi server trả về HTTP 401 do token hết hạn hoặc `authorization_version` thay đổi, `apiClient` phải lập tức xóa sạch token trong `localStorage` và điều hướng về `login.html`.
- **No Credential Logging**: Tuyệt đối không log password, token JWT hay thông tin định danh lên `console.log`.
- **FormData Boundary**: Khi upload file Excel, không đặt `Content-Type` thủ công để trình duyệt tự tạo boundary multipart hợp lệ.

# 8. Accessibility Constraints
- Nút bấm gửi form (`Submit`) phải có trạng thái phản hồi rõ ràng; khi đang gửi request, cập nhật `aria-busy="true"` và text thông báo (ví dụ: "Đang tải...").
- Các thông báo lỗi từ server phải được render trong vùng có `role="alert"` hoặc `aria-live="polite"` để phần mềm đọc màn hình thông báo kịp thời.
- Lỗi form phải liên kết với input thông qua `aria-describedby` và `aria-invalid="true"`.

# 9. Verification
- **DevTools Network Tab**: Xác minh URL chuẩn `/api/v1/*`, header `Authorization: Bearer <token>`, status code khớp hợp đồng.
- **204 No Content Test**: Kiểm tra các thao tác xóa không gây lỗi cú pháp JSON `SyntaxError`.
- **Double Submit Test**: Bấm liên tục 5 lần vào nút submit; xác nhận chỉ đúng 1 request được gửi đi.
- **Offline / Slow 3G Test**: Giả lập mạng chậm kiểm tra spinner; ngắt mạng kiểm tra thông báo lỗi thân thiện.

# 10. Failure Conditions
- Gọi `fetch()` trực tiếp thay vì thông qua `apiClient`.
- Đọc `response.data.accessToken` dẫn đến `undefined` làm mất phiên đăng nhập.
- Gọi `response.json()` trên response HTTP 204 No Content.
- Tự động retry các request mutation (POST/PUT/DELETE) gây trùng lặp dữ liệu.
- Giả định rằng upload file xem trước (Preview) đã lưu dữ liệu vào database.

# 11. What NOT to Do
- KHÔNG tự chế các wrapper fetch riêng rẽ ở từng trang HTML.
- KHÔNG sửa đổi mã nguồn Spring Boot backend để phục vụ frontend.
- KHÔNG lưu trữ auth state, application state trên `window` (`window.__ENV__` chỉ dùng cho read-only deployment config).
- KHÔNG nuốt lỗi trong khối `catch` bằng `console.log()` mà không thông báo cho người dùng.

# 12. References
- Hợp đồng 48 Handlers / 49 Mappings đầy đủ: Đọc `references/api-contract.md`.
- Chi tiết HTTP Client, Timeout, ApiResponse envelope & Safe Retry: Đọc `references/central-api-client.md`.
- Vòng đời JWT, mô hình đe dọa token & Server-side revocation: Đọc `references/auth-and-token-lifecycle.md`.
- Ba trạng thái UI chuẩn, Validation forms & Double submit: Đọc `references/ui-states-and-forms.md`.
- Quy trình Import Excel 2 bước & Báo cáo kiểm tra: Đọc `references/two-step-excel-import.md`.
- User Profile & SRS Settings API contracts: Đọc `references/user-profile-and-srs-settings.md`.
