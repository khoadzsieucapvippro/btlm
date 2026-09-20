# The Spring Security JWT Flow

## 1. Luồng chạy tổng thể
Khi Frontend gửi request, nó sẽ đi qua các lớp (layers) sau trước khi tới Controller:

1. **Browser / HTTP Client**:
   Gửi Request (Kèm Header `Authorization: Bearer xyz...`).
2. **CORS Filter / Preflight**:
   Kiểm tra CORS (Origin, Method, Header). Nếu gửi `OPTIONS` preflight, Spring Security phải chừa đường cho nó đi qua.
3. **SecurityFilterChain**:
   Bắt đầu chuỗi Filter.
4. **Custom JWT Authentication Filter (`JwtAuthenticationFilter`)**:
   - `request.getHeader("Authorization")`.
   - Lọc tiền tố `Bearer `.
   - Parse và Validate Signature, Expiration (24h), Issuer (`elearning-backend`) bằng thư viện JJWT 0.12.x (`io.jsonwebtoken.Jwts`).
   - Trích xuất claim `auth_ver` và `roles`.
   - **Xác thực phiên bản ủy quyền (DEC-42)**: Kiểm tra `auth_ver` với database (`account.authorization_version`) và trạng thái tài khoản `Active`. Nếu mismatch hoặc tài khoản bị khóa/đổi quyền, từ chối ngay với HTTP 401 Unauthorized.
5. **Authentication & SecurityContext**:
   - Chuyển đổi danh sách role (vd: `"Admin"`) thành `SimpleGrantedAuthority("ROLE_" + role)`.
   - Tạo đối tượng `UsernamePasswordAuthenticationToken` (với `CustomUserDetails`).
   - Đưa vào `SecurityContextHolder.getContext().setAuthentication(auth)`.
6. **Authorization Filter**:
   - Xem endpoint này cấu hình `permitAll()` hay `authenticated()`.
   - Xem `@PreAuthorize` có yêu cầu Role trùng với Role trong Context không.
7. **Controller**:
   - Xử lý nghiệp vụ.

## 2. Spring Boot 3 / Spring Security 6 Updates
- Bỏ `WebSecurityConfigurerAdapter`. Cấu hình bằng cách khai báo Bean `SecurityFilterChain`.
- Bỏ `antMatchers()`, dùng `requestMatchers()`.
- Cấu hình stateless: `sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`.
