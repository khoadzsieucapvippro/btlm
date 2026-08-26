# Spring Security & JWT Troubleshooting Matrix

## 1. Phân biệt cơ bản (401 vs 403)
- **401 (Unauthorized)**: Lỗi Xác Thực (Authentication). Server không thể xác định bạn là ai (Missing token, Invalid Token, Expired Token).
- **403 (Forbidden)**: Lỗi Phân Quyền (Authorization). Server biết bạn là ai (Token hợp lệ), nhưng bạn KHÔNG CÓ QUYỀN truy cập (Role mismatch).

## 2. Xử lý 401 Unauthorized
| Symptom | Probable Cause | Diagnosis & Fix | Verification |
|---------|----------------|-----------------|--------------|
| Báo 401 khi không gửi token | Đúng thiết kế bảo vệ API. | Nếu đây là API Login/Register, quên thêm `requestMatchers(..).permitAll()`. | Thêm vào filter chain. |
| Có gửi header Auth nhưng vẫn 401 | Quên chữ `Bearer ` hoặc code parse Header sai. | Print/Log cái header nhận được xem có bị `null` không. Check Network Tab để đảm bảo Header `Authorization: Bearer xyz` được gửi đi. | Postman truyền đúng chuẩn. |
| Báo 401 kèm ExpiredJwtException | Token hết hạn. | User để máy quá lâu. Frontend cần code auto redirect ra Login hoặc gọi Refresh Token. | Cấp lại token mới. |
| Báo 401 kèm SignatureException hoặc MalformedJwtException | Chữ ký sai, hoặc secret key bị đổi. | Token bị cắt xén, copy thiếu chữ. Kiểm tra biến môi trường `JWT_SECRET` có nhất quán không (backend đổi key sau khởi động lại). | Validate lại JWT. |
| Mọi thứ đúng nhưng SecurityContext rỗng | Filter chạy xong quên set Auth, hoặc quên đăng ký Filter. | Kiểm tra code Filter xem có dòng `SecurityContextHolder...setAuthentication(auth)` không. Đảm bảo custom filter được add vào SecurityFilterChain (`addFilterBefore`). | Debug đoạn gán context. |

## 3. Xử lý 403 Forbidden
| Symptom | Probable Cause | Diagnosis & Fix | Verification |
|---------|----------------|-----------------|--------------|
| Token hợp lệ nhưng báo 403 | Role Mismatch. | `@PreAuthorize("hasRole('ADMIN')")` nhưng trong JWT Token claims chỉ có Role `USER`. | Đọc JWT bằng jwt.io để xem payload. |
| Báo 403 cho MỌI endpoint POST/PUT | Thiếu cấu hình CSRF. | Trong Stateless REST API, CSRF protection phải bị tắt: `csrf(csrf -> csrf.disable())`. | API POST chạy thành công. |
| Lỗi Role Prefix | Spring tự động thêm chữ `ROLE_` khi dùng `hasRole()`. | Nếu Authority lưu trong token là `ADMIN`, phải đổi code thành `hasAuthority('ADMIN')` hoặc sửa data DB thành `ROLE_ADMIN`. | Kiểm tra logic cấp quyền lúc tạo token. |

## 4. CORS Preflight Interaction (401/403 che mờ lỗi CORS)
| Symptom | Probable Cause | Diagnosis & Fix | Verification |
|---------|----------------|-----------------|--------------|
| Lỗi `No Access-Control-Allow-Origin` DÙ đã config CORS | Request OPTIONS bị Spring Security chặn trước khi tới CORS config. | Thêm `http.cors(Customizer.withDefaults())` vào SecurityFilterChain để filter nương tay với request OPTIONS (Preflight). | Browser gửi request preflight thành công, không bị 401/403. |
