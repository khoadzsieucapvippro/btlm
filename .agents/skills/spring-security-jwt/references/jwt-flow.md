# The Spring Security JWT Flow

## 1. Luồng chạy tổng thể
Khi Frontend gửi request, nó sẽ đi qua các lớp (layers) sau trước khi tới Controller:

1. **Browser / HTTP Client**:
   Gửi Request (Kèm Header `Authorization: Bearer xyz...`).
2. **CORS Filter / Preflight**:
   Kiểm tra CORS (Origin, Method, Header). Nếu gửi `OPTIONS` preflight, Spring Security phải chừa đường cho nó đi qua.
3. **SecurityFilterChain**:
   Bắt đầu chuỗi Filter.
4. **Custom JWT Authentication Filter**:
   - `request.getHeader("Authorization")`.
   - Lọc chữ `Bearer `.
   - Parse và Validate Signature/Expiration bằng thư viện `io.jsonwebtoken.Jwts`.
   - Lấy Subject (Username/Email) và Claims (Roles).
5. **Authentication & SecurityContext**:
   - Nếu token hợp lệ, tạo đối tượng `UsernamePasswordAuthenticationToken`.
   - Gọi `SecurityContextHolder.getContext().setAuthentication(auth)`.
6. **Authorization Filter**:
   - Xem endpoint này cấu hình `permitAll()` hay `authenticated()`.
   - Xem `@PreAuthorize` có yêu cầu Role trùng với Role trong Context không.
7. **Controller**:
   - Xử lý nghiệp vụ.

## 2. Spring Boot 3 / Spring Security 6 Updates
- Bỏ `WebSecurityConfigurerAdapter`. Cấu hình bằng cách khai báo Bean `SecurityFilterChain`.
- Bỏ `antMatchers()`, dùng `requestMatchers()`.
- Cấu hình stateless: `sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`.
