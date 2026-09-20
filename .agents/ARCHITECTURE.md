# ARCHITECTURE — THIẾT KẾ KIẾN TRÚC MỤC TIÊU (TARGET ARCHITECTURE)

> **Trạng thái triển khai hiện tại:** **Phase 0–8, Module 8D & Hardening Remediation Tasks R1 → R3.11 & Final Remediation (DEC-42) COMPLETED** (Toàn bộ Backend, 15 audit remediation items, 8 phân hệ Module 8D, chuỗi kiểm toán & gia cố ranh giới R1..R3.10, chốt chặn chất lượng R3.11 và đợt kiểm thử đối kháng củng cố bảo mật cuối cùng DEC-42 đã hoàn thành, **`1101/1101 tests PASS, Failures: 0, Errors: 0, Skipped: 0`** trên Testcontainers MySQL 8.4; Backend Audit Status: `SEALED & APPROVED FOR PHASE 9 FRONTEND INTEGRATION` - DEC-41, DEC-42). Chính thức chuyển giao triển khai **Phase 9 — Frontend UI & Client API Integration** (Module 9A — Task 9A.1).  

---

## 1. TỔNG QUAN KIẾN TRÚC TẦNG (TIERED ARCHITECTURE)

```
Client / Frontend (HTML, CSS, JavaScript)
         │
         ▼ (HTTP / RESTful JSON / Bearer JWT)
REST API Layer (Spring Security Filter Chain)
         │
         ▼
Spring MVC Controller Layer (Request DTOs, Bean Validation @Valid, Response Wrapping)
         │
         ▼
Service Layer (Business Logic, Transactions @Transactional, Entity/DTO Mapping)
         │
         ▼
Repository Layer (Spring Data JPA, Derived Queries, Optimized JPQL)
         │
         ▼
MySQL Database (Schema controlled 100% by Flyway Migrations)
```

---

## 2. NGUYÊN TẮC THIẾT KẾ CHI TIẾT TỪNG THÀNH PHẦN

### 2.1. Client / Frontend & Tích hợp API
- **Công nghệ:** HTML5 ngữ nghĩa, CSS3 với hệ thống design tokens, JavaScript thuần (Vanilla JS ES6+ modular, native `fetch()`, zero-build complexity). Không sử dụng các SPA frameworks (React, Vue, Angular, Next.js, Nuxt). Bootstrap 5.3 CDN được sử dụng giới hạn như công cụ tiện ích layout grid và UI primitives (modal, toast) có kiểm tra SRI; không quyết định nhận diện hình ảnh của sản phẩm.
- **Cấu trúc module 4 tầng canonical:**
  - `frontend/js/api/api.js`: HTTP client tập trung (`fetch()`), tự động unwrap `ApiResponse<T>`, phân tích lỗi `parseFieldErrors()`, timeout bằng `AbortController`, backoff khi 429, retry cho các request GET idempotent.
  - `frontend/js/auth/auth-state.js`: Quản lý phiên đăng nhập và vai trò người dùng phía client.
  - `frontend/js/ui/ui.js` & `frontend/js/ui/security.js`: UI primitives chia sẻ (3-state UI: loading, empty, error; toasts, modals) và các bộ dựng DOM an toàn (`textContent`, sanitization, chống XSS).
  - `frontend/js/pages/*-page.js`: Controller cho từng trang HTML độc lập.
  - `frontend/js/app.js`: Khởi tạo và kết nối shell ứng dụng.
- **Ranh giới bảo mật Client Storage (OWASP Boundary):**
  - `localStorage` được xác định rõ là **untrusted client-side convenience storage**, không phải vùng lưu trữ bảo mật (security boundary).
  - Việc kiểm tra vai trò người dùng ở frontend (`roles.includes('Admin')` - không có tiền tố `ROLE_`) chỉ phục vụ điều hướng và hiển thị giao diện (UX).
  - Ranh giới xác thực và phân quyền duy nhất có giá trị bảo mật thuộc về backend Spring Security (`@PreAuthorize`, `SecurityFilterChain`, `authorization_version`).
- **Xử lý phiên đăng nhập:** Tự động đính kèm header `Authorization: Bearer <token>` nếu có token trong `localStorage`. Tự động xóa token và điều hướng đến `login.html` khi nhận phản hồi HTTP `401 Unauthorized` hoặc khi token không hợp lệ.
- **Quản lý trạng thái UI:** Bắt buộc mọi màn hình dữ liệu phải xử lý đầy đủ 3 trạng thái:
  1. *Loading state:* Hiển thị spinner/chờ khi đang fetch dữ liệu.
  2. *Empty state:* Hiển thị thông báo thân thiện khi danh sách rỗng.
  3. *Error state:* Hiển thị thông báo lỗi rõ ràng và nút thử lại khi call API thất bại.
- **Bảo mật giao diện DOM:** Cấm sử dụng `innerHTML` không kiểm soát với dữ liệu từ người dùng hoặc external input; bắt buộc sử dụng `textContent`, `document.createElement`, hoặc các helper safe DOM từ `frontend/js/ui/security.js`.

### 2.2. REST API & DTO Boundaries
- **Phân tách ranh giới tuyệt đối (DTO Isolation):** 
  - Toàn bộ dữ liệu đi vào Controller phải thông qua **Request DTOs** (ví dụ: `LoginRequest`, `RegisterRequest`, `PersonalNoteRequest`).
  - Toàn bộ dữ liệu trả về cho client phải thông qua **Response DTOs** (ví dụ: `RadicalResponse`, `VocabularyResponse`, `LessonDetailResponse`).
  - **Tuyệt đối cấm** trả trực tiếp JPA Entity ra ngoài API để tránh rò rỉ cấu trúc database, tránh lỗi `LazyInitializationException` và vòng lặp tuần hoàn Jackson (infinite circular reference).
- **Chuẩn phong bì phản hồi (Unified ApiResponse Envelope):** Hầu hết các phản hồi API nghiệp vụ sử dụng phong bì `ApiResponse<T>`. Các phản hồi đặc thù theo từng endpoint được ưu tiên áp dụng. Các phản hồi HTTP 204 No Content không chứa body và do đó không chứa `ApiResponse<T>`:
  ```json
  {
    "code": "SUCCESS",
    "message": "Thông điệp cho client",
    "errors": [],
    "data": {}
  }
  ```
- **Chuẩn phân trang (PageResponse):**
  ```json
  {
    "code": "SUCCESS",
    "message": "Tải dữ liệu thành công",
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

### 2.3. Spring MVC Controller Layer
- Chỉ chịu trách nhiệm tiếp nhận HTTP request, ánh xạ URL, validate payload qua `@Valid`, điều phối Service và trả về `ResponseEntity<ApiResponse<T>>` (hoặc `ResponseEntity.noContent().build()` không body cho HTTP 204).
- Không chứa nghiệp vụ tính toán, không truy cập database trực tiếp.
- Thống nhất quy ước tiền tố đường dẫn: `/api/v1/...` trên toàn bộ hệ thống.

### 2.4. Validation & Exception Handling
- **Input Validation:** Áp dụng Bean Validation (Jakarta Validation: `@NotBlank`, `@Size`, `@Pattern`, `@NotNull`, v.v.) trực tiếp trên Request DTOs.
- **Tập trung xử lý lỗi (@RestControllerAdvice):** `GlobalExceptionHandler` bắt và chuẩn hóa toàn bộ lỗi thành định dạng `ApiResponse` thống nhất:
  - `MethodArgumentNotValidException` / `BindException` -> HTTP 400 (`code: "VALIDATION_ERROR"`, `errors: ["${field}: ${message}", ...]` — `List<String>`, mỗi phần tử là chuỗi `"tên_field: thông_báo_lỗi"`).
  - `BusinessException(ErrorCode)` -> HTTP tương ứng (400, 404, 409, 422).
  - `AccessDeniedException` -> HTTP 403 (`code: "FORBIDDEN"`).
  - `BadCredentialsException` -> HTTP 401 (`code: "UNAUTHORIZED"`).
  - `MaxUploadSizeExceededException` -> HTTP 413 (`code: "FILE_TOO_LARGE"`).
  - `Exception` (fallback không mong muốn) -> HTTP 500 (`code: "INTERNAL_ERROR"`), log chi tiết stack trace trên server.

### 2.5. Authentication, JWT & Spring Security
- **Mô hình xác thực:** Stateless Authentication sử dụng JWT (JSON Web Token) kết hợp Spring Security và BCrypt password hashing.
- **Quy trình xác thực:**
  1. Client gửi credentials tới `/api/v1/auth/login`.
  2. Spring Security `DaoAuthenticationProvider` xác thực mật khẩu qua `PasswordEncoder`.
  3. Khi thành công, `JwtUtil` ký và tạo token chứa claims: `sub` (email/phone), `roles`, `exp`, `iat`.
  4. Token được trả về cho client lưu trong `localStorage`.
- **Security Filter Chain:**
  - `JwtAuthenticationFilter` chạy trước `UsernamePasswordAuthenticationFilter`.
  - Phân tích token từ header `Authorization: Bearer <token>`, nạp `Authentication` vào `SecurityContextHolder`.
  - Cấu hình session: `SessionCreationPolicy.STATELESS`.
  - Phân quyền URL: Public các endpoint tra cứu (`/api/v1/auth/**`, GET `/api/v1/radicals/**`, GET `/api/v1/vocabulary/**`, GET `/api/v1/lessons/**`).
  - Phân quyền theo vai trò: Áp dụng kiểm tra quyền ở cấp method (`@PreAuthorize("hasRole('CREATOR')")`, v.v.) và trên `SecurityFilterChain`.

### 2.6. Service Layer
- Chịu trách nhiệm thực thi toàn bộ quy tắc nghiệp vụ, tính toán thuật toán SRS (SM-2), điều phối import Excel (Apache POI), và quản lý Transaction (`@Transactional`).
- Đảm bảo các bất biến nghiệp vụ (Domain Invariants), ví dụ: chỉ tác giả bài học mới có quyền submit bài duyệt, bắt buộc có lý do khi từ chối bài học, v.v.

### 2.7. Repository & Persistence (Spring Data JPA / Hibernate)
- Sử dụng Spring Data JPA Repositories.
- **Nguyên tắc ánh xạ thực thể:** Không mặc định quy tắc 1 bảng = 1 JPA Entity class. Thiết kế Entity/Domain Model là một quyết định kỹ thuật sẽ được thiết kế cẩn trọng (ví dụ: các bảng liên kết như `ACCOUNT_ROLE`, `VOCAB_RADICAL` có thể được ánh xạ qua `@JoinTable` của `@ManyToMany` hoặc Entity riêng với `@EmbeddedId`).
- Tối ưu truy vấn tránh lỗi N+1 Query bằng cách sử dụng `JOIN FETCH` hoặc `@EntityGraph`.
- Tắt Open Session In View (`spring.jpa.open-in-view=false`) để đảm bảo ranh giới transaction rõ ràng trong Service layer.

### 2.8. Database Engine & Flyway Migration
- **Hệ quản trị CSDL:** **MySQL** (kết hợp Flyway kiểm soát schema).
- **Kiểm soát Schema bằng Flyway:** 
  - Toàn bộ bảng, cột, khóa chính, khóa ngoại, chỉ mục đều được định nghĩa qua các file script migration (`V1__...`, `V2__...`).
  - Cấm tuyệt đối bật `spring.jpa.hibernate.ddl-auto=update`. Hibernate chỉ chạy ở chế độ `ddl-auto: validate` hoặc `none`.
  - Mọi thay đổi schema phải thông qua file migration mới. Không chỉnh sửa migration đã apply.
