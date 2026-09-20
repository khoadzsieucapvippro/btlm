# Spring Security & JWT Troubleshooting Matrix

## 1. Phân biệt cơ bản (401 vs 403)
- **401 (Unauthorized)**: Lỗi Xác Thực (Authentication). Server không thể xác định bạn là ai (Missing token, Invalid Token, Expired Token).
- **403 (Forbidden)**: Lỗi Phân Quyền (Authorization). Server biết bạn là ai (Token hợp lệ), nhưng bạn KHÔNG CÓ QUYỀN truy cập (Role mismatch).

## 2. Xử lý 401 Unauthorized
| Symptom | Probable Cause | Diagnosis & Fix | Verification |
|---------|----------------|-----------------|--------------|
| Báo 401 khi không gửi token | Đúng thiết kế bảo vệ API. | Nếu đây là API Login/Register, quên thêm `requestMatchers(..).permitAll()`. | Thêm vào filter chain. |
| Có gửi header Auth nhưng vẫn 401 | Quên chữ `Bearer ` hoặc code parse Header sai. | Print/Log cái header nhận được xem có bị `null` không. Check Network Tab để đảm bảo Header `Authorization: Bearer xyz` được gửi đi. | Postman truyền đúng chuẩn. |
| Báo 401 kèm ExpiredJwtException | Token hết hạn (sau 24h). | Phiên đăng nhập quá 24h. Hệ thống không dùng refresh token; Frontend bắt buộc tự động xóa token và redirect ra `login.html`. | Đăng nhập lại để nhận token mới. |
| Báo 401 kèm SignatureException hoặc MalformedJwtException | Chữ ký sai, hoặc secret key bị đổi. | Token bị cắt xén, copy thiếu chữ. Kiểm tra biến môi trường `JWT_SECRET` có nhất quán không (backend đổi key sau khởi động lại). | Validate lại JWT. |
| Báo 401 dù token còn hạn | Mismatch `auth_ver` (DEC-42) hoặc tài khoản không `Active`. | Admin đã đổi vai trò, khóa tài khoản hoặc reset quyền. Server tăng `authorization_version` làm token cũ mất hiệu lực ngay lập tức. | Frontend xử lý 401 bằng cách logout và redirect về `login.html`. |
| Mọi thứ đúng nhưng SecurityContext rỗng | Filter chạy xong quên set Auth, hoặc quên đăng ký Filter. | Kiểm tra code Filter xem có dòng `SecurityContextHolder...setAuthentication(auth)` không. Đảm bảo custom filter được add vào SecurityFilterChain (`addFilterBefore`). | Debug đoạn gán context. |

## 3. Xử lý 403 Forbidden
| Symptom | Probable Cause | Diagnosis & Fix | Verification |
|---------|----------------|-----------------|--------------|
| Token hợp lệ nhưng báo 403 | Role Mismatch hoặc Object Ownership. | Người dùng không đủ role (vd: Learner truy cập Creator studio) hoặc Creator sửa bài học của người khác. Backend giữ nguyên session, thông báo không có quyền. | Đọc JWT payload xem mảng `roles`. |
| Báo 403 cho MỌI endpoint POST/PUT | Thiếu cấu hình CSRF. | Trong Stateless REST API, CSRF protection phải bị tắt: `csrf(AbstractHttpConfigurer::disable)`. | API POST chạy thành công. |
| Lỗi Role Prefix & Case Matching | Quy chuẩn Role 4 tầng. | DB & Token claim chứa `"Admin"`, `"Creator"`, v.v. `JwtAuthenticationFilter` tự động thêm tiền tố `ROLE_`. Trên Controller dùng `@PreAuthorize("hasAnyRole('Admin', 'ADMIN')")`. Phía FE kiểm tra `roles.includes('Admin')` (KHÔNG kiểm tra `ROLE_ADMIN`). | Kiểm tra tính nhất quán qua 4 tầng Role. |

## 4. CORS Preflight Interaction (401/403 che mờ lỗi CORS)
| Symptom | Probable Cause | Diagnosis & Fix | Verification |
|---------|----------------|-----------------|--------------|
| Lỗi `No Access-Control-Allow-Origin` DÙ đã config CORS | Request OPTIONS bị Spring Security chặn trước khi tới CORS config. | Thêm `http.cors(Customizer.withDefaults())` vào SecurityFilterChain để filter nương tay với request OPTIONS (Preflight). | Browser gửi request preflight thành công, không bị 401/403. |
