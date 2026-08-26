---
name: spring-security-jwt
description: Điều phối luồng Authentication & Authorization. Quản lý Request filter, JWT processing, CORS, và Error handling (401/403).
---

# 1. Purpose
Đảm bảo an toàn API bằng cơ chế Stateless Authentication (JWT), phân quyền (Authorization) và kiểm soát Request Cross-Origin (CORS) qua Spring SecurityFilterChain.

# 2. When to use
- Cấu hình phân quyền (Role/Authority) cho endpoint.
- Code logic Login (tạo token) / Logout.
- Khắc phục lỗi 401 Unauthorized, 403 Forbidden.
- Cấu hình CORS khi Frontend và Backend nằm ở port khác nhau.

# 3. When NOT to use
- Logic business thuần túy không liên quan tới token/role.

# 4. Inputs / Preconditions
- Đã cấu hình khóa bí mật (Secret Key) JWT trong `application.yml` (Tuyệt đối không hardcode trong Java).
- HTTP Request (từ UI hoặc cURL) có kèm Header `Authorization: Bearer <token>`.

# 5. Core workflow
1. **Analyze Security Need**: Endpoint mới cần public hay private? Cần Role gì?
2. **Update SecurityFilterChain**: Thêm vào `requestMatchers(..).permitAll()` nếu public.
3. **Controller/Service Authorization**: Đánh dấu `@PreAuthorize("hasRole('ADMIN')")` nếu private.
4. **JWT Flow Verification**: Xác minh luồng Filter xử lý tốt.
5. **Exception Translation**: Bọc lại lỗi Authentication/AccessDenied thành JSON chuẩn.

# 6. Decision points
- Lỗi từ chối truy cập là do Authentication (401) hay Authorization (403)? Đọc `references/troubleshooting-matrix.md`.
- Token lấy ở đâu? Đọc header `Authorization`.
- Yêu cầu Preflight (OPTIONS) bị chặn? Đọc `references/cors.md`.

# 7. Red flags
- Token được trích xuất nhưng `SecurityContextHolder.getContext().setAuthentication(...)` không được gọi.
- Lưu State vào HTTP Session trong khi dùng JWT.
- Cấu hình CORS mở toang `*` với `allowCredentials(true)` (Gây lỗi bảo mật / crash boot).

# 8. Verification
- Gọi API bằng cURL không token -> Nhận 401.
- Gọi API với token Role USER vào endpoint ADMIN -> Nhận 403.
- Login thành công trả ra Token đúng format.

# 9. Exit criteria
- Lỗi 401/403 được sửa triệt để, không còn log stacktrace dư thừa, trả về cấu trúc JSON đúng chuẩn.

# 10. References to load conditionally
- Cần nắm rõ luồng chạy của SecurityFilterChain và JWT Filter? Đọc `references/jwt-flow.md`.
- Đang kẹt ở lỗi 401/403 không rõ nguyên nhân? Đọc `references/troubleshooting-matrix.md`.
- Gặp lỗi "No Access-Control-Allow-Origin"? Đọc `references/cors.md`.
