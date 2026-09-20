# ROADMAP — LỘ TRÌNH TRIỂN KHAI DỰ ÁN MỚI TỪ ĐẦU (REBUILT FROM SCRATCH)

> **Mô hình kiến trúc triển khai:** 4 cấp độ kỹ thuật:  
> $\rightarrow$$\rightarrow$\text{PHASE (Giai đoạn lớn)} \longrightarrow \text{MODULE (Phân hệ kỹ thuật)} \longrightarrow \text{TASK (Lát cắt thực thi)} \longrightarrow \text{VERIFICATION CHECKPOINT (Chốt chặn nghiệm thu)}$\rightarrow$$\rightarrow$
> 
> **Nguyên tắc kỹ thuật dành cho AI Coding Agent:**  
> - **Scope rõ ràng:** Mỗi Task xác định rõ: Làm gì, Tại sao cần, Trong phạm vi nào (In-Scope), Không làm gì (Out-of-Scope).  
> - **Dependency thực tế:** Phân định chính xác giữa Hard Dependency (bắt buộc tuần tự ở cấp task/module), Parallel (có thể thực thi song song) và Service Dependency (ràng buộc mức nghiệp vụ, không tạo FK database giả).  
> - **Tiêu chí nghiệm thu định lượng:** Đánh giá dựa trên bằng chứng vật lý độc lập (Tests pass, Flyway validated, HTTP status đúng), không suy đoán cảm tính.  
> - **Ngữ cảnh vừa phải (Lean Context):** Một Task là một lát cắt thay đổi có ý nghĩa (Coherent Change), tránh gộp quá nhiều capability gây quá tải ngữ cảnh (context overload).  

---

## 1. TỔNG QUAN LỘ TRÌNH 12 GIAI ĐOẠN (ROADMAP OVERVIEW)

> [!NOTE]
> **Phân biệt Thứ tự Phân phối (Milestone Order) vs Đồ thị Phụ thuộc (Dependency Graph):**  
> - **Thứ tự Phase (Phase 0 $\rightarrow$\rightarrow$\rightarrow$ 11):** Là các cột mốc phân phối tính năng theo lộ trình hệ thống.  
> - **Đồ thị Phụ thuộc (Task/Module Dependencies):** Cho phép các module/task độc lập được triển khai song song (ví dụ: Module 2A và 2B thực thi song song; Module 2E Flyway Seed song song với JPA mappings; Module 1B phong bì phản hồi độc lập với tầng Persistence). Không gò ép tuyến tính cứng nhắc nếu dependency thực tế cho phép song song.

```text
Phase 0: Project Specification & Architecture Baseline [COMPLETED]
   │
   ▼
Phase 1: Spring Boot Foundation & Web Infrastructure [COMPLETED]
   │
   ├────────────────────────────────────────┬───────────────────────────────────────┐
   ▼ (Hard dependency cho Persistence)      ▼ (Response Envelope & Errors)          ▼ (Database Seed)
Phase 2: Persistence Layer & Seed Data      Phase 1 Module 1B: Response Envelope    Phase 2 Module 2E: Seed
(JPA Mappings 2A-2D, Test 2F) [COMPLETED]   (ApiResponse, GlobalExceptionHandler)   (Roles V2, Radicals V3)
   │                                        │ [COMPLETED]                           │ [COMPLETED]
   │                                        ├───────────────────────────────────────┤
   ▼                                        ▼                                       ▼
Phase 3: Authentication, Security & RBAC ◄──┴───────────────────────────────────────┘
(Security 6, JWT, Login/Register DTO & Controllers, User Profile) [COMPLETED]
   │
   ▼
Phase 4: Radical & Vocabulary Catalog Domain [COMPLETED]
(214 Radicals, Vocabulary Search, Admin CRUD)
   │
   ▼
Phase 5: Lesson Management & Excel Import [COMPLETED]
(Public Lessons, Creator Studio, POI 2-Step Import)
   │
   ▼
Phase 6: Content Moderation Workflow [COMPLETED]
(Moderator Queue, Approve/Reject Invariants, Audit Log)
   │
   ▼
Phase 7: Spaced Repetition System [COMPLETED]
(SM-2 Pure Algorithm, Review Session, Study Progress)
   │
   ▼
Phase 8: Personal Notes & User Settings [COMPLETED]
(Notes <= 500 chars, SRS Daily Limits) & Module 8D Backend Completion
   │
   ▼
Backend Remediation & Hardening Tasks (R1 → R3.11) [COMPLETED]
(R1/R1.1 New Card Engine & Concurrency, R2/R2.1/R2.1A Due-Only Review Policy & Moderation Race, R3.1/R3.1A Vocabulary Lifecycle & Row Serialization, R3.2 Lesson Lifecycle & Moderation History, R3.3/R3.3A Chinese Domain & Pinyin Normalization Search Integrity, R3.4 Auth Audit, R3.5 Notes Pagination, R3.6 Rate Limiter Cache, R3.7 Apache POI 5.4.0 CVE, R3.8 Radical Missing Pinyin Correction V7, R3.9 Database Index Performance & EXPLAIN Verification, R3.10 Production HTTP & Reverse Proxy Security Headers, R3.11 Backend Final Quality Gate & Pre-Frontend Release Seal)
   │
   ▼
Phase 9: Frontend UI & Client Integration [UNBLOCKED / READY TO COMMENCE]
(Foundation Shell, api/api.js, Learner, SRS, Creator, Moderator UI)
   │
   ▼
Phase 10: Security, Performance & Quality Hardening [NOT_STARTED]
(10A Security, 10B Performance, 10C Quality Gaps)
   │
   ▼
Phase 11: Final Integration, Release Validation & Delivery [NOT_STARTED]
(E2E Journeys, JAR Packaging, Delivery)
```

---

## 2. NỘI DUNG CHI TIẾT TỪNG GIAI ĐOẠN (PHASE → MODULE → TASK → CHECKPOINT)

---

### Phase 0 — Project Specification and Architecture Baseline
- **Mục tiêu:** Thiết lập nền tảng tài liệu đặc tả thẩm quyền, dọn dẹp mã nguồn cũ và cấu hình hạ tầng CSDL cục bộ.
- **Trạng thái:** **`COMPLETED`** (Đã nghiệm thu với đầy đủ bằng chứng vật lý).

#### Module 0A: Project Reset & Agent Skills Protection
* **Task 0A.1:** Xóa bỏ mã nguồn cũ và artifacts không phù hợp trong `backend/`, `frontend/`, `scripts/`, `temp/`, `docs/`.
* **Task 0A.2:** Xác minh tính toàn vẹn 19 Agent Skills trong `.agents/skills/`.
* **Checkpoint 0A:** `validate_skills.ps1` trả về `Validation PASSED`. Thư mục source ở trạng thái clean slate.

#### Module 0B: Authoritative Specifications & Database Physical Design
* **Task 0B.1:** Thiết lập bộ 9 tài liệu đặc tả chuẩn 14 bảng trong `.agents/`.
* **Task 0B.2:** Cài đặt và cấu hình MySQL Community Server 8.4 LTS (cổng 3306, `utf8mb4`, database `elearning_db`).
* **Task 0B.3:** Khởi tạo Git repository trên nhánh `main`, cấu hình `.gitignore` chuẩn.
* **Task 0B.4:** Soạn thảo tài liệu Thiết kế CSDL Vật lý chi tiết cho 14 bảng MySQL (`.agents/DATABASE_DESIGN.md`).
* **Checkpoint 0B:** MySQL kết nối thành công, Git commit `40ada12` và `f2b2000` sẵn sàng.
* **Phase 0 Integration Checkpoint:** 100% tài liệu và môi trường runtime CSDL sẵn sàng.

---

### Phase 1 — Spring Boot Foundation & Web Infrastructure
- **Mục tiêu:** Khởi tạo khung dự án Spring Boot 3, kết nối MySQL, kích hoạt schema Flyway V1, và thiết lập chuẩn phong bì phản hồi API tập trung.
- **Trạng thái:** **`COMPLETED`** (Module 1A: `COMPLETED`, Module 1B: `COMPLETED`).

#### Module 1A: Application Scaffold & Database Infrastructure [COMPLETED]
* **Task 1A.1:** Khởi tạo cấu trúc Spring Boot 3.3.5 với Java 21 LTS trong `backend/pom.xml`.
* **Task 1A.2:** Cấu hình `application.yml` HikariCP kết nối MySQL cổng 3306, kích hoạt Flyway, `ddl-auto=none`.
* **Task 1A.3:** Soạn thảo và thực thi Flyway migration `V1__init_schema.sql` tạo đúng 14 bảng nghiệp vụ.
* **Task 1A.4:** Class khởi động `ElearningApplication.java` và kiểm thử `ElearningApplicationTests.java`.
* **Checkpoint 1A [VERIFIED]:** `mvn clean test` trả về `BUILD SUCCESS`, schema `elearning_db` chứa đúng 15 bảng (14 bảng nghiệp vụ + `flyway_schema_history` version 1).

#### Module 1B: Web API Response Envelope & Global Error Handling [COMPLETED]
* **Task 1B.1: Base Response Models (`ApiResponse<T>`, `PageResponse<T>`, `ErrorCode` enum) [COMPLETED]**
  - *Mục đích:* Chuẩn hóa định dạng JSON trả về client theo Mục 1.2 và 1.3 của `API.md`.
  - *Phạm vi:* `com.elearning.dto.response.ApiResponse`, `PageResponse`, `com.elearning.common.ErrorCode`.
  - *Không làm:* Chưa viết Controller hay Service nghiệp vụ.
  - *depends_on:* `Task 1A.4`.
* **Task 1B.2: Global Exception Handler (`GlobalExceptionHandler`) [COMPLETED]**
  - *Mục đích:* Bắt toàn diện ngoại lệ tại `@RestControllerAdvice` và chuyển thành `ApiResponse` chuẩn.
  - *Phạm vi:* Xử lý `MethodArgumentNotValidException` (400), `BusinessException` (dynamic code/status), fallback `Exception` (500).
  - *depends_on:* `Task 1B.1`.
* **Checkpoint 1B [VERIFIED]:** `ApiResponseTests` và `GlobalExceptionHandlerTests` PASS 24/24 tests; kiểm chứng serialize JSON của `ApiResponse<T>`, `PageResponse<T>`, bắt lỗi validation trả về HTTP 400 kèm `errors[]`, `BusinessException` bảo toàn mã lỗi và HTTP status.
* **Phase 1 Integration Checkpoint [VERIFIED]:** `mvn clean test` PASS 25/25 tests (1 context test, 12 ApiResponse tests, 12 ExceptionHandler tests), ứng dụng nạp context thành công, kết nối MySQL ổn định, cấu trúc phong bì API và cơ chế bắt lỗi sẵn sàng cho các giai đoạn tiếp theo.

---

### Phase 2 — Persistence Layer & Database Seed Data
- **Mục tiêu:** Xây dựng tầng ánh xạ thực thể JPA (JPA Domain Mappings), Spring Data Repositories, và nạp dữ liệu danh mục ban đầu qua Flyway seed data (Roles, Radicals).
- **Ranh giới phụ thuộc:** Phụ thuộc vào `Module 1A` (Flyway V1 schema). **KHÔNG phụ thuộc vào `Module 1B`** (có thể triển khai song song).
- **Trạng thái:** **`COMPLETED`** (Đã nghiệm thu toàn diện Module 2A..2F qua `Checkpoint Phase 2` với 81/81 tests PASS).

#### Module 2A: Identity & Role Persistence Mapping [COMPLETED] [PARALLEL với Mod 2B]
* **Task 2A.1: JPA Entities Cụm Định danh (`ACCOUNT`, `USER_PROFILE`, `ROLE`, `ACCOUNT_ROLE`) [COMPLETED]**
  - *Phạm vi:* Entity cho `Account`, `UserProfile` (quan hệ 1:1 qua `@OneToOne`), `Role`, và ánh xạ quan hệ nhiều-nhiều N:N tài khoản - vai trò qua `@ManyToMany` kèm `@JoinTable(name = "account_role")`.
  - *Không làm:* Không viết logic đăng ký, đăng nhập hay Spring Security.
  - *depends_on:* `Task 1A.4`.
* **Task 2A.2: Spring Data JPA Repositories Cụm Định danh [COMPLETED]**
  - *Phạm vi:* `AccountRepository` (tìm theo `email_or_phone`, kiểm tra `existsByEmailOrPhone`), `UserProfileRepository` (tìm theo `account`), `RoleRepository` (tìm theo `role_name`).
  - *depends_on:* `Task 2A.1`.
* **Checkpoint 2A [VERIFIED]:** Chạy `@DataJpaTest` lưu và truy vấn thành công Account kèm UserProfile và Roles; `IdentityRepositoryTests` PASS 7/7 tests, `IdentityPersistenceTests` PASS 5/5 tests.

#### Module 2B: Dictionary Catalog Persistence Mapping [COMPLETED] [PARALLEL với Mod 2A]
* **Task 2B.1: JPA Entities Cụm Từ điển (`RADICAL`, `VOCABULARY`, `VOCAB_RADICAL`) [COMPLETED]**
  - *Phạm vi:* Entity cho `Radical`, `Vocabulary` (lưu cả `pinyin` có dấu và `pinyin_raw` không dấu độc lập), ánh xạ liên kết N:N `VOCAB_RADICAL` qua `@ManyToMany` kèm `@JoinTable`.
  - *depends_on:* `Task 1A.4`.
* **Task 2B.2: Spring Data JPA Repositories Cụm Từ điển [COMPLETED]**
  - *Phạm vi:* `RadicalRepository` (`findByCharacter`, `existsByCharacter`), `VocabularyRepository` (`findByHanziAndPinyinRaw`, `findByHanzi`, `findByPinyinRaw`, `searchByKeyword` kèm phân trang `Pageable`).
  - *depends_on:* `Task 2B.1`.
* **Checkpoint 2B [VERIFIED]:** Chạy `@DataJpaTest` lưu từ vựng liên kết bộ thủ và truy vấn tìm kiếm theo `pinyin_raw`, phân trang `Pageable`; `DictionaryRepositoryTests` PASS 8/8 tests, `DictionaryPersistenceTests` PASS 5/5 tests.

#### Module 2C: Lesson & Content Persistence Mapping [COMPLETED]
* **Task 2C.1: JPA Entities Cụm Bài học (`LESSON`, `LESSON_VOCABULARY`) [COMPLETED]**
  - *Phạm vi:* Entity cho `Lesson` (quan hệ ManyToOne với `Account` tác giả), bảng liên kết `LessonVocabulary` (composite PK qua `@EmbeddedId` + `@MapsId`, trường thứ tự `order_index`, `@OrderBy("orderIndex ASC")`).
  - *depends_on (Entity Mapping):* `Task 2A.1` (Account Entity), `Task 2B.1` (Vocabulary Entity).
* **Task 2C.2: Spring Data JPA Repositories Cụm Bài học [COMPLETED]**
  - *Phạm vi:* `LessonRepository` (lọc theo tác giả `created_by`, lọc theo trạng thái `status`), `LessonVocabularyRepository` (lấy từ vựng theo bài học sắp xếp `order_index ASC`).
  - *depends_on (Repository Test):* `Task 2C.1`, `Task 2A.2` (AccountRepository), `Task 2B.2` (VocabularyRepository) để tạo test fixture.
* **Checkpoint 2C [VERIFIED]:** Chạy `@DataJpaTest` tạo Lesson, thêm từ vựng kèm `order_index` và truy vấn danh sách sắp xếp đúng thứ tự; `LessonRepositoryTests` PASS 7/7 tests, `LessonPersistenceTests` PASS 5/5 tests.

#### Module 2D: Learning Progress, Audit & Personalization Persistence Mapping [COMPLETED]
* **Task 2D.1: JPA Entities Cụm Ghi chú & Kiểm toán (`USER_SRS_SETTING`, `PERSONAL_NOTE`, `MODERATION_LOG`) [COMPLETED]**
  - *Phạm vi:* Entity cho `UserSrsSetting` (1:1 với UserProfile), `PersonalNote` (ràng buộc độ dài $\rightarrow$\le 500$\rightarrow$ ký tự, không giới hạn 5 notes), `ModerationLog` (bất biến, quan hệ RESTRICT với Lesson và Account).
  - *depends_on:* `Task 2A.1` (UserProfile), `Task 2B.1` (Vocabulary), `Task 2C.1` (Lesson).
* **Task 2D.2: JPA Entities Cụm SRS với Tham chiếu Đa hình (`CARD_PROGRESS`, `REVIEW_LOG`) [COMPLETED]**
  - *Phạm vi:* Ánh xạ cặp trường `item_type` (`VARCHAR(20)`) và `item_id` (`BIGINT UNSIGNED`). **Tuyệt đối không tạo FK vật lý ở MySQL** theo đúng Phương án A đã duyệt.
  - *Lưu ý kỹ thuật:* Ràng buộc kiểm tra sự tồn tại của từ vựng hoặc bộ thủ là **Service/Business dependency**, không phải physical FK dependency. Do đó Entity mapping chỉ phụ thuộc vào `UserProfile`.
  - *depends_on:* `Task 2A.1` (UserProfile).
* **Task 2D.3: Spring Data JPA Repositories Cụm SRS, Ghi chú & Kiểm toán [COMPLETED]**
  - *Phạm vi:* `CardProgressRepository` (tìm thẻ đến hạn `next_review_at <= NOW()`), `ReviewLogRepository`, `PersonalNoteRepository`, `ModerationLogRepository`, `UserSrsSettingRepository`.
  - *depends_on:* `Task 2D.1`, `Task 2D.2`.
* **Checkpoint 2D: [COMPLETED]** Chạy `@DataJpaTest` kiểm tra lưu trữ thẻ học đa hình, truy vấn thẻ đến hạn ôn tập và lưu ghi chú cá nhân PASS 100%.

#### Module 2E: Database Seed Migrations (Flyway) [PARALLEL với Mod 2A..2D] [COMPLETED]
* **Task 2E.1: Flyway Seed Data V2: 4 Vai trò hệ thống (`V2__seed_roles.sql`) [COMPLETED]**
  - *Phạm vi:* Script seed 4 vai trò cố định: `1=Learner`, `2=Creator`, `3=Moderator`, `4=Admin`.
  - *depends_on:* `Task 1A.4`.
* **Task 2E.2: Flyway Seed Data V3: 214 Bộ thủ Khang Hy (`V3__seed_radicals.sql`) [COMPLETED]**
  - *Phạm vi:* Script seed 214 bộ thủ Khang Hy chuẩn từ dataset thẩm quyền `.agents/references/radicals.json`.
  - *depends_on:* `Task 1A.4`.
* **Checkpoint 2E: [COMPLETED]** Flyway migration V2 và V3 chạy thành công. Kiểm tra `SELECT COUNT(*) FROM role` trả về 4; `SELECT COUNT(*) FROM radical` trả về 214.

#### Module 2F: Persistence Layer Verification & Schema Validation [COMPLETED]
* **Task 2F.1: Kiểm thử tích hợp toàn diện tầng Persistence & Schema Validation [COMPLETED]**
  - *Phạm vi:* Bật cấu hình Hibernate `spring.jpa.hibernate.ddl-auto: validate`, chạy toàn bộ test suite để đảm bảo các Entity mappings khớp chính xác với schema MySQL do Flyway quản lý.
  - *depends_on:* `Module 2A`, `Module 2B`, `Module 2C`, `Module 2D`, `Module 2E`.
* **Checkpoint Phase 2: [COMPLETED]** `mvn clean test` PASS với `ddl-auto: validate`. Không có cảnh báo sai khác kiểu dữ liệu, khóa chính hoặc khóa ngoại. Toàn bộ 81/81 tests PASS.

---

### Phase 3 — Authentication, Security & RBAC
- **Mục tiêu:** Xây dựng hệ thống bảo mật không trạng thái (Stateless Authentication) sử dụng Spring Security 6 và JWT, phân quyền Role-Based Access Control 4 vai trò, cung cấp API Đăng ký, Đăng nhập và Hồ sơ người dùng.
- **Ranh giới phụ thuộc:**
  - `Task 3A.1` & `3A.2` (Security Infrastructure): Chỉ phụ thuộc `Task 1A.1, 1B.2` (không phụ thuộc seed data).
  - `Task 3B.2` (AuthService / Register): Phụ thuộc `Task 1B.1, 1B.2, 2A.2, 2E.1` (cần Role Learner từ seed) và `3A.3`.
- **Trạng thái:** **`COMPLETED`** (Đã nghiệm thu toàn bộ Module 3A..3D qua `Checkpoint Phase 3` với 187/187 tests PASS).

#### Module 3A: Spring Security & JWT Infrastructure [COMPLETED]
* **Task 3A.1: Cấu hình `SecurityConfig`: Spring Security 6 `SecurityFilterChain`, `BCryptPasswordEncoder`, cấu hình session `STATELESS`, vô hiệu hóa CSRF cho REST [COMPLETED]**
* **Task 3A.2: Xây dựng `JwtUtil` (sinh token, giải mã claims, kiểm tra hết hạn) và `JwtAuthenticationFilter` (chặn request, giải mã header `Bearer`, nạp SecurityContext) [COMPLETED]**
* **Task 3A.3: Triển khai `CustomUserDetailsService` và `CustomUserDetails` nạp người dùng từ `AccountRepository` và ánh xạ roles thành GrantedAuthorities [COMPLETED]**
* **Checkpoint 3A: Unit test cho `JwtUtil` (tạo token, trích xuất claim `email_or_phone`, phát hiện token hết hạn/sai chữ ký) và `CustomUserDetailsService` [COMPLETED]**

#### Module 3B: Authentication & Registration Vertical Slice [COMPLETED]
* **Task 3B.1: Request/Response DTOs: `RegisterRequest`, `LoginRequest`, `AuthResponse` với Jakarta Validation (`@NotBlank`, `@Size`, v.v.) [COMPLETED]**
* **Task 3B.2: `AuthService` và `AuthController` (`POST /api/v1/auth/register` gán mặc định role `Learner`, `POST /api/v1/auth/login` kiểm tra mật khẩu qua BCrypt, trả về JWT gói trong `ApiResponse`) [COMPLETED]**
* **Checkpoint 3B: MockMvc test: Đăng ký thành công trả về 201; Đăng nhập đúng trả về 200 kèm JWT; Đăng nhập sai mật khẩu trả về 401; Input thiếu trường trả về 400 kèm lỗi validation [COMPLETED]**

#### Module 3C: User Profile Vertical Slice [COMPLETED]
* **Task 3C.1: DTOs (`UserProfileResponse`, `UpdateProfileRequest`), `UserProfileService` và `UserProfileController` (`GET /api/v1/users/profile`, `PUT /api/v1/users/profile` lấy và cập nhật profile của user đang đăng nhập qua `ApiResponse`) [COMPLETED]**
* **Checkpoint 3C: MockMvc test: Truy cập profile khi có JWT hợp lệ trả về 200; truy cập khi không có JWT trả về 401 Unauthorized [COMPLETED]**

#### Module 3D: Security & RBAC Verification [COMPLETED]
* **Task 3D.1: Bộ kiểm thử tích hợp tự động cho phân quyền RBAC 4 vai trò (Learner, Creator, Moderator, Admin) [COMPLETED]**
* **Checkpoint Phase 3: MockMvc test xác minh chặn 403 Forbidden khi Learner cố truy cập endpoint yêu cầu quyền Admin/Moderator/Creator [COMPLETED]**

---

### Phase 4 — Radical and Vocabulary Catalog Domain
- **Mục tiêu:** Cung cấp RESTful APIs tra cứu danh mục 214 Bộ thủ Khang Hy và Từ vựng tiếng Trung, hỗ trợ tìm kiếm đa tiêu chí (pinyin có dấu, pinyin không dấu `pinyin_raw`, chữ Hán `hanzi`), lọc theo bộ thủ, phân trang chuẩn `PageResponse`, và Admin CRUD.
- **Ranh giới phụ thuộc:** Phụ thuộc vào `Module 1B` (cho Controller response contract), `Module 2B`, `Task 2E.2` (data 214 bộ thủ), và `Phase 3` (xác thực quyền Admin).
- **Trạng thái:** **`COMPLETED`** (Module 4A: COMPLETED; Module 4B: COMPLETED; Module 4C: COMPLETED; Checkpoint Phase 4: COMPLETED).

#### Module 4A: Radical Catalog Vertical Slice [COMPLETED]
* **Task 4A.1: Radical DTOs (`RadicalResponse`, `RadicalDetailResponse`) và `RadicalService` [COMPLETED]**
  - *Phạm vi:* DTOs đóng gói metadata bộ thủ chuẩn (không chứa `radicalNumber`/`strokeCount` do không có trong DB schema; không chứa vocabulary list do thuộc phân hệ 4B theo `API.md`), `RadicalService` và `RadicalServiceImpl` (lấy toàn bộ 214 bộ thủ, phân trang `Pageable`, chi tiết theo ID/ký tự), xử lý lỗi không tìm thấy bằng `BusinessException(ErrorCode.NOT_FOUND)`.
  - *depends_on:* `Task 1B.1`, `Task 2B.2`, `Task 2E.2`.
  - *Kiểm thử:* `RadicalServiceTests` PASS 8/8 tests, `RadicalServiceIntegrationTests` PASS 6/6 tests (xác minh chính xác 214 bộ thủ Khang Hy trên MySQL thật).
* **Task 4A.2: `RadicalController` công khai & Admin CRUD Bộ thủ [COMPLETED]**
  - *Phạm vi:* `RadicalController` (`GET /api/v1/radicals`, `GET /api/v1/radicals/{id}` công khai), `AdminRadicalController` (`POST/PUT/DELETE /api/v1/admin/radicals/**` yêu cầu role Admin), request DTOs (`CreateRadicalRequest`, `UpdateRadicalRequest`) với Jakarta Validation, mutation methods trong `RadicalService` và `RadicalServiceImpl` (`createRadical`, `updateRadical`, `deleteRadical`).
  - *Ranh giới bất biến giữa Flyway và Runtime:* Thao tác Admin CRUD là nghiệp vụ runtime chạy qua Service/Repository, **tuyệt đối không sửa script Flyway migration**.
  - *depends_on:* `Task 1B.1`, `Task 1B.2`, `Task 4A.1`, `Mod 3A` (Security & RBAC).
  - *Kiểm thử:* `RadicalServiceTests` (18/18 PASS), `RadicalControllerTests` (12/12 PASS), `RadicalIntegrationTests` (11/11 PASS xác minh ma trận bảo mật và CRUD trên 214 bộ thủ thật).
* **Checkpoint 4A: MockMvc test: Tra cứu công khai trả về đủ 214 bộ thủ; Gọi Admin API bằng quyền Learner bị từ chối 403 Forbidden [COMPLETED]**

#### Module 4B: Vocabulary Catalog & Search Vertical Slice [COMPLETED]
* **Task 4B.1: Vocabulary DTOs (`VocabularyResponse`, `VocabularySearchCriteria`) và `VocabularyService` [COMPLETED]** (DTOs `VocabularyResponse`, `VocabularyDetailResponse` kèm `radicals`, `VocabularySearchCriteria`; `VocabularySpecification` JPA dynamic queries cho hanzi, pinyin, pinyinRaw shadow column, radicalId Many-to-Many join distinct an toàn; `VocabularyService` và `VocabularyServiceImpl` phân trang `Pageable`; 32 tests PASS: 7 DTO tests, 14 unit tests, 11 MySQL integration tests).
* **Task 4B.2: `VocabularyController` công khai & Admin CRUD Từ vựng [COMPLETED]** (`GET /api/v1/vocabulary`, `GET /api/v1/vocabulary/{id}` công khai; `POST/PUT/DELETE /api/v1/admin/vocabulary/**` chỉ Admin; request DTOs `CreateVocabularyRequest`, `UpdateVocabularyRequest`; mutation methods `createVocabulary`, `updateVocabulary`, `deleteVocabulary` trong service; `VocabularyControllerTests` PASS 12/12, `VocabularyIntegrationTests` PASS 18/18, `VocabularyServiceTests` PASS 29/29).
* **Checkpoint 4B: MockMvc test: Tìm kiếm từ vựng với từ khóa tone-less (ví dụ: `ni` tìm ra `nǐ / 你`), kiểm tra dữ liệu phân trang trả về đúng định dạng `PageResponse` [COMPLETED]**

#### Module 4C: Catalog Domain Verification [COMPLETED]
* **Task 4C.1: Kiểm thử tự động tích hợp tra cứu và bảo vệ quyền quản trị dữ liệu gốc [COMPLETED]** (`CatalogIntegrationTests` PASS 25/25 tests kiểm thử tích hợp toàn diện Catalog Domain trên Spring Boot context, CSDL MySQL thật và real JWT tokens).
* **Checkpoint Phase 4: MockMvc test toàn diện các ca tìm kiếm từ vựng, chi tiết kèm bộ thủ liên kết, phân trang và xử lý từ khóa rỗng/không hợp lệ [COMPLETED]**

---

### Phase 5 — Lesson Management and Excel Import [COMPLETED]
- **Mục tiêu:** Cung cấp API quản lý bài học cho Creator, cơ chế phân tích cú pháp và import file Excel 2 bước bằng Apache POI, và API khám phá bài học công khai cho Learner.
- **Ranh giới phụ thuộc:** Phụ thuộc vào `Module 1B` (cho Controllers), `Module 2C`, `Phase 3` (Creator auth), `Phase 4` (Vocabulary catalog).
- **Trạng thái:** **`COMPLETED (Toàn bộ Module 5A, 5B, 5C, 5D đã hoàn thành và nghiệm thu; Checkpoint Phase 5 VERIFIED)`**.

#### Module 5A: Public Lesson Exploration Vertical Slice [COMPLETED]
* **Task 5A.1 [COMPLETED]:** Lesson DTOs (`LessonSummaryResponse`, `LessonDetailResponse`, `LessonVocabItemResponse`) và `LessonService` (chỉ trả bài học `Approved`, ẩn các bài `Draft`, `Pending`, `Rejected` bằng 404, sắp xếp từ vựng theo `order_index` ASC, phòng ngừa N+1 bằng JPQL constructor projection `SIZE(l.lessonVocabularies)` và `JOIN FETCH lv.vocabulary`). PASS 23 tests (7 DTO, 9 Unit, 7 Integration).
* **Task 5A.2 [COMPLETED]:** `LessonController` (`GET /api/v1/lessons`, `GET /api/v1/lessons/{id}` công khai — **chỉ trả về bài học có `status = Approved`**, danh sách từ vựng sắp xếp đúng theo `order_index`). PASS 17 tests (8 Controller, 9 Integration).
* **Checkpoint 5A [COMPLETED]:** MockMvc test: Bài học ở trạng thái `Draft`, `Pending`, hoặc `Rejected` không xuất hiện trên API công khai (trả về 404/rỗng). Xác minh qua `LessonIntegrationTests` (PASS 9/9).

#### Module 5B: Creator Lesson Studio Vertical Slice [COMPLETED]
* **Task 5B.1 [COMPLETED]:** Creator DTOs (`CreateLessonRequest`, `UpdateLessonRequest`, `ReorderVocabRequest`, `VocabOrderItem`) và `CreatorLessonService` (CRUD bài học, kiểm tra quyền sở hữu tác giả, thay đổi thứ tự từ vựng 2-phase offset, nộp bài kiểm duyệt `Draft`/`Rejected` $\rightarrow$\rightarrow$\rightarrow$ `Pending`). PASS 32 tests (25 Unit, 7 Integration).
* **Task 5B.2 [COMPLETED]:** `CreatorLessonController` (`POST/PUT/DELETE /api/v1/creator/lessons/**`). PASS 28 tests (16 Unit, 12 Integration).
* **Checkpoint 5B [COMPLETED]:** MockMvc test: Creator tạo bài học nháp, thêm từ vựng, đổi thứ tự, nộp bài duyệt; Creator A cố sửa bài của Creator B bị chặn 403 Forbidden. Xác minh qua `CreatorLessonIntegrationTests` (PASS 12/12).

#### Module 5C: Two-Step Excel Import Engine [COMPLETED]
* **Task 5C.1 [COMPLETED]:** `ExcelParserService` sử dụng Apache POI `poi-ooxml:5.3.0` (đọc file `.xlsx`, validate an toàn dung lượng tối đa 10MB, kiểm tra magic bytes ZIP OOXML `PK\x03\x04`, mapping tiêu đề linh hoạt Việt/Anh, validate từng hàng, phát hiện trùng lặp trong file, đối chiếu vocabulary CSDL MySQL, xuất báo cáo `ImportValidationReport` kèm chi tiết lỗi từng hàng, bảo đảm bất biến zero-mutation ở bước preview). PASS 12 tests (10 Unit, 2 Integration).
* **Task 5C.2 [COMPLETED]:** Excel Import Controller Endpoints (`POST /api/v1/creator/lessons/import` bước 1 preview không ghi CSDL trả về 200 OK + `ImportValidationReport`, `POST /api/v1/creator/lessons/import/confirm` bước 2 lưu transactional trả về 201 Created + `LessonDetailResponse`). `CreatorLessonService.importLessonFromExcel`: atomic transaction lưu Lesson ('Draft'), Vocabulary mới, LessonVocabulary preserving order_index, rollback khi lỗi. `GlobalExceptionHandler` mapping 413 `FILE_TOO_LARGE` và 400 `VALIDATION_ERROR`. PASS 60 tests (30 Unit Service, 22 Unit Controller, 8 Integration).
* **Checkpoint 5C [COMPLETED]:** Test tự động upload file Excel chuẩn thành công; upload file sai định dạng cột hoặc file rỗng trả về preview lỗi chi tiết ở cấp độ hàng (Row-level validation error); bước preview bảo toàn tính zero-mutation trên CSDL MySQL; bước confirm thực hiện giao dịch nguyên tử lưu bài học và từ vựng. Xác minh qua `ExcelImportIntegrationTests` (PASS 8/8) và `ExcelParserServiceTests` (PASS 10/10).

#### Module 5D: Lesson Lifecycle Verification & Phase 5 Checkpoint [COMPLETED]
* **Task 5D.1 [COMPLETED]:** Kiểm thử tự động tích hợp toàn diện 14 phân vùng nghiệp vụ vòng đời bài học từ tạo nháp, thêm từ vựng, đổi thứ tự, import Excel 2 bước, cách ly quyền tác giả Creator Ownership Isolation 403, application transaction rollback độc lập trên MySQL, trạng thái bất biến state machine, đến nộp duyệt (`LessonLifecycleIntegrationTests` PASS 20/20).
* **Checkpoint Phase 5 [COMPLETED]:** MockMvc test chu trình hoàn chỉnh: Import file Excel $\rightarrow$\rightarrow$\rightarrow$ Xem preview zero-mutation $\rightarrow$\rightarrow$\rightarrow$ Lưu bài nháp nguyên tử $\rightarrow$\rightarrow$\rightarrow$ Sắp xếp từ vựng $\rightarrow$\rightarrow$\rightarrow$ Nộp bài duyệt (`status = Pending`) $\rightarrow$\rightarrow$\rightarrow$ Khám phá bài học công khai (chỉ hiển thị bài `Approved`). Full regression `mvn clean test` PASS 487/487 tests (0 failures, 0 errors).

---

### Phase 6 — Content Moderation Workflow [COMPLETED]
- **Mục tiêu:** Cung cấp hàng đợi kiểm duyệt bài học cho Moderator, thực thi phê duyệt (`Approved`) hoặc từ chối (`Rejected` bắt buộc lý do và danh sách trường lỗi dạng JSON), và ghi vết bất biến vào `MODERATION_LOG`.
- **Ranh giới phụ thuộc:** Phụ thuộc vào `Module 1B` (cho Controllers), `Module 2C`, `Module 2D`, `Phase 3` (Moderator auth), `Phase 5` (Pending lessons).
- **Trạng thái:** **`COMPLETED (Toàn bộ Module 6A, 6B, 6C đã hoàn thành và kiểm chứng)`**.

#### Module 6A: Moderation Core Business Logic [COMPLETED]
* **Task 6A.1 [COMPLETED]:** Moderation DTOs (`ModerationQueueResponse`, `ApproveLessonRequest`, `RejectLessonRequest` với `@NotBlank rejection_reason` và `flagged_fields` JSON) và `ModerationService`.
* **Task 6A.2 [COMPLETED]:** Thực thi bất biến kiểm duyệt và ghi vết kiểm toán: Chỉ bài học `Pending` mới được duyệt/từ chối; Ghi log bất biến vào `MODERATION_LOG` (`ON DELETE RESTRICT`).
* **Checkpoint 6A [COMPLETED]:** Service test: Từ chối bài học thiếu lý do ném `BusinessException`; Duyệt bài học không ở trạng thái `Pending` bị từ chối; Log kiểm toán được lưu đúng trường.

#### Module 6B: Moderation REST Endpoints [COMPLETED]
* **Task 6B.1 [COMPLETED]:** `ModeratorController` (`GET /api/v1/moderator/lessons/pending`, `GET /api/v1/moderator/lessons/{id}`, `POST /api/v1/moderator/lessons/{id}/approve`, `POST /api/v1/moderator/lessons/{id}/reject`).
* **Checkpoint 6B [COMPLETED]:** MockMvc test: Moderator xem hàng đợi duyệt, bấm duyệt bài (status thành `Approved`), bấm từ chối (status thành `Rejected` kèm log); Learner/Creator truy cập bị chặn 403.

#### Module 6C: Moderation Workflow Verification [COMPLETED]
* **Task 6C.1 [COMPLETED]:** Kiểm thử tự động tích hợp quy trình xuất bản nội dung.
* **Checkpoint Phase 6 [COMPLETED]:** Test chu trình: Creator nộp bài $\rightarrow$\rightarrow$\rightarrow$ Moderator từ chối $\rightarrow$\rightarrow$\rightarrow$ Creator sửa bài và nộp lại $\rightarrow$\rightarrow$\rightarrow$ Moderator phê duyệt $\rightarrow$\rightarrow$\rightarrow$ Bài học lập tức hiển thị trên Public Lesson API.

---

### Phase 7 — Spaced Repetition System (SRS SM-2 Engine) [COMPLETED]
- **Mục tiêu:** Triển khai cỗ máy tính toán lặp lại ngắt quãng SM-2 thuần túy toán học, quản lý phiên ôn tập thẻ học hàng ngày, ghi nhận nhật ký phản xạ `review_time_seconds` và cập nhật tiến trình ghi nhớ `CARD_PROGRESS`.
- **Ranh giới phụ thuộc:**
  - `Module 7A` (Toán học SM-2): **Độc lập hoàn toàn** (không phụ thuộc database, spring hay DTOs).
  - `Module 7B` (SRS Session): Phụ thuộc `Task 7A.1`, `Module 2D` (CardProgress), `Phase 3` (Learner auth), `Module 1B` (SrsController).
- **Trạng thái:** **`COMPLETED`** (Đã nghiệm thu qua `SrsPolymorphicIntegrityTests` và Checkpoint Phase 7).

#### Module 7A: Pure SM-2 Calculation Engine [COMPLETED]
* **Task 7A.1 [COMPLETED]:** Thành phần toán học thuần túy `SrsCalculator` (tính toán `interval_days`, `ease_factor`, `repetitions` dựa trên rating 1–4: 1=Again, 2=Hard, 3=Good, 4=Easy; chặn sàn $\rightarrow$EF \ge 1.30$\rightarrow$; chu kỳ bước nhảy 1 ngày, 6 ngày, v.v.).
* **Task 7A.2 [COMPLETED]:** Unit test toán học toàn diện cho `SrsCalculator` kiểm tra đầy đủ các nhánh logic và điều kiện biên.
* **Checkpoint 7A [COMPLETED]:** Unit test độc lập toán học chạy PASS, xác nhận tính toán chính xác không phụ thuộc database hay Spring context.

#### Module 7B: SRS Review Session & Card Progress Vertical Slice [COMPLETED]
* **Task 7B.1 [COMPLETED]:** SRS DTOs (`DueCardResponse`, `ReviewCardRequest`, `StudyStatsResponse`) và `SrsService` (lấy thẻ đến hạn `next_review_at <= NOW()`, áp dụng giới hạn ngày từ `USER_SRS_SETTING`, cập nhật `CARD_PROGRESS`, ghi nhật ký `REVIEW_LOG` kèm `review_time_seconds`).
* **Task 7B.2 [COMPLETED]:** `SrsController` (`GET /api/v1/srs/due`, `POST /api/v1/srs/review`, `GET /api/v1/srs/stats`).
* **Checkpoint 7B [COMPLETED]:** MockMvc test (`SrsIntegrationTests`): Học viên lấy danh sách thẻ cần học hôm nay, gửi kết quả đánh giá (Rating 3 kèm 4 giây phản xạ), kiểm tra `CARD_PROGRESS` cập nhật ngày ôn tập tiếp theo và `REVIEW_LOG` được ghi nhận trên CSDL MySQL thật.

#### Module 7C: SRS Verification & Polymorphic Resolution [COMPLETED]
* **Task 7C.1 [COMPLETED]:** Kiểm thử tự động tích hợp `SrsPolymorphicIntegrityTests` cho cả 2 loại thẻ học: `VOCABULARY` và `RADICAL` (tham chiếu đa hình `item_type` + `item_id`).
* **Checkpoint Phase 7 [COMPLETED]:** Test suite xác minh tính toàn vẹn tham chiếu đa hình ở tầng Service: từ chối tạo tiến trình nếu `item_id` không tồn tại trong bảng tương ứng; hoàn thành toàn bộ Phase 7.

---

### Phase 8 — Personal Notes & User Learning Settings [COMPLETED]
- **Mục tiêu:** Cung cấp tính năng ghi chú cá nhân của người học trên từng từ vựng (thực thi nghiêm ngặt `content <= 500 characters`, không áp dụng giới hạn 5 ghi chú) và tùy biến chỉ số học tập SRS cá nhân.
- **Ranh giới phụ thuộc:** Phụ thuộc vào `Module 1B` (cho Controllers), `Module 2D`, `Phase 3` (Learner auth), `Phase 4` (Vocabulary).
- **Trạng thái:** **`COMPLETED`** (Đã nghiệm thu qua `Phase8PersonalizationIntegrationTests` và Checkpoint Phase 8).

#### Module 8A: Personal Notes Vertical Slice [COMPLETED]
* **Task 8A.1 [COMPLETED]:** Personal Note DTOs (`PersonalNoteRequest`, `PersonalNoteResponse`) và `PersonalNoteService` (CRUD ghi chú, kiểm tra độ dài $\rightarrow$\le 500$\rightarrow$ ký tự, đảm bảo không giới hạn số lượng ghi chú/từ vựng, bảo vệ quyền sở hữu người dùng).
* **Task 8A.2 [COMPLETED]:** `PersonalNoteController` (`GET /api/v1/vocabularies/{vocabId}/notes`, `POST /api/v1/vocabularies/{vocabId}/notes`, `PUT/DELETE /api/v1/notes/{noteId}`).
* **Checkpoint 8A [VERIFIED & PASSED]:** MockMvc test: Tạo ghi chú $\rightarrow$\le 500$\rightarrow$ chars thành công (201); Tạo ghi chú $\rightarrow$> 500$\rightarrow$ chars bị từ chối 400 Validation Error; User A cố sửa/xóa ghi chú của User B bị chặn 403 Forbidden.

#### Module 8B: SRS User Settings Vertical Slice [COMPLETED]
* **Task 8B.1 [COMPLETED]:** Setting DTOs (`UserSrsSettingResponse`, `UpdateSrsSettingRequest`), `UserSrsSettingService` và `UserSrsSettingController` (`GET/PUT /api/v1/srs/settings`: `new_cards_per_day` mặc định 20, `max_review_per_day` mặc định 100).
* **Checkpoint 8B [VERIFIED & PASSED]:** MockMvc test: Lấy cấu hình mặc định, cập nhật số thẻ mới mỗi ngày, xác thực giá trị dương $\rightarrow$> 0$\rightarrow$.

#### Module 8C: Personalization Verification [COMPLETED]
* **Task 8C.1 [COMPLETED]:** Kiểm thử tự động tích hợp cho phân hệ cá nhân hóa học tập (`Phase8PersonalizationIntegrationTests`: xác minh causal link giữa `PUT /api/v1/srs/settings` và `GET /api/v1/srs/due`, user isolation, daily quota exhaustion, limit saturation).
* **Checkpoint Phase 8 [VERIFIED & PASSED]:** Test tích hợp: Cập nhật setting giới hạn thẻ $\rightarrow$\rightarrow$\rightarrow$ ảnh hưởng trực tiếp và chuẩn xác đến số lượng thẻ trả về trong API lấy thẻ đến hạn ôn tập của Phase 7 (đạt 100%).

#### Module 8D: Backend Functional Completion & Pre-Frontend Gate [COMPLETED]
- **Mục tiêu:** Bổ sung đầy đủ các REST API và năng lực backend quản trị còn thiếu theo đặc tả gốc trước khi bước vào xây dựng frontend; bảo đảm an ninh tài khoản, tính toàn vẹn phân quyền, kiểm soát tương tranh và kiểm soát tần suất đăng nhập.
- **Task 8D.1 [COMPLETED]:** Quản trị Tài khoản Người dùng (`GET /api/v1/admin/accounts`, `PUT /api/v1/admin/accounts/{id}/status`, `AccountResponse`, chính sách chống tự khóa tài khoản `POL-8D-01`). PASS unit và integration tests.
- **Task 8D.2 [COMPLETED]:** Quản trị & Phân quyền Vai trò (`GET /api/v1/admin/roles`, `PUT /api/v1/admin/accounts/{id}/roles`, `RoleResponse`, chính sách chống tự tước quyền Admin `POL-8D-02`, khóa bi quan cấp hàng `findByIdForUpdate(accountId)`, thu hồi token cũ tức thì qua `authorization_version`). PASS unit, integration và concurrency tests (10 luồng đồng thời).
- **Task 8D.3 [COMPLETED]:** Lịch sử Kiểm duyệt Nội dung (`GET /api/v1/moderator/history`, phân quyền theo vai trò: Moderator xem lịch sử cá nhân, Admin xem toàn bộ lịch sử hệ thống). PASS unit và integration tests.
- **Task 8D.4 [COMPLETED]:** Giám sát Toàn bộ Bài học Hệ thống (`GET /api/v1/admin/lessons` phân trang lọc theo mọi trạng thái `Draft`, `Pending`, `Approved`, `Rejected`). PASS unit và integration tests.
- **Task 8D.5 [COMPLETED]:** Chuẩn hóa Hợp đồng Lỗi Xác thực (`H-01`: trả về generic HTTP 401 Unauthorized khi truy cập tài nguyên bảo vệ mà chưa đăng nhập). PASS unit và integration tests.
- **Task 8D.6 [COMPLETED]:** Kiểm soát Tần suất Đăng nhập (`LoginRateLimiter` sliding window in-memory 10 req/60s, HTTP 429 `TOO_MANY_REQUESTS`). PASS unit và integration tests.
- **Task 8D.7 [COMPLETED]:** Endpoint Giám sát Sức khỏe Tối giản (`GET /actuator/health` public trả về `{"status":"UP"}`). PASS integration tests.
- **Task 8D.8 [COMPLETED]:** Cổng Nghiệm thu Toàn diện (Pre-Frontend Gate): Chạy toàn bộ test suite trên Testcontainers MySQL 8.4: **`948/948 tests PASS`** (0 failures, 0 errors, 0 skipped). Đồng bộ hóa tài liệu dự án.

---

### Backend Remediation & Hardening Tasks (R1 → R3.3A) [COMPLETED]
- **Mục tiêu:** Kiểm toán chuyên sâu, vá lỗi và gia cố toàn diện các ranh giới nghiệp vụ trọng yếu của Backend: cỗ máy học từ mới theo bài học (R1), kiểm soát tương tranh hạn mức từ mới (R1.1), chính sách ôn tập đúng hạn Due-Only (R2), kiểm soát điều kiện bài học Approved khi học từ mới (R2.1), kiểm chứng tương tranh ranh giới kiểm duyệt (R2.1A), bảo vệ toàn vẹn vòng đời và lịch sử học tập của từ vựng (R3.1), khóa bi quan cấp hàng chống TOCTOU khi xóa từ vựng (R3.1A), bảo vệ lịch sử kiểm duyệt bài học và phân ly tiến trình học tập (R3.2), kiểm toán 214 bộ thủ Khang Hy và chuẩn hóa Unicode Pinyin tra cứu tiếng Trung (R3.3/R3.3A).
- **Trạng thái:** **`COMPLETED`** (Toàn bộ 10 task R1..R3.3A đã hoàn thành, kiểm chứng và bảo vệ bằng 1059 tests).

* **Task R1 [COMPLETED]:** Lesson-Driven New Card Engine & Daily New-Card Quota (`GET /api/v1/srs/new-cards`, `GET /api/v1/srs/lessons/{id}/new-cards`, phân tách candidate preview idempotent vs first review, thực thi kiểm tra hạn mức từ mới mỗi ngày `new_cards_per_day` trên `reviewCard()`).
* **Task R1.1 [COMPLETED]:** New Card Concurrency & Quota TOCTOU Verification (Khóa bi quan cấp hàng `findByIdWithLock(accountId)` trên `UserProfile`, bảo đảm không vượt quá hạn mức từ mới khi có nhiều luồng review đồng thời).
* **Task R2 [COMPLETED]:** Review Policy & SM-2 Variant Correctness (Chốt chặn máy chủ Server-Side Due-Only Enforcement: từ chối review sớm `nextReviewAt > now` với HTTP 409 `CONFLICT`; cho phép thẻ mới và thẻ học lại cùng ngày `interval = 0 && nextReviewAt = now`; phân định rành mạch giữa SM-2 variant của dự án và Standard SM-2; timezone `Asia/Ho_Chi_Minh`).
* **Task R2.1 [COMPLETED]:** New Card Approved-Lesson Eligibility Enforcement (Chốt chặn mutation boundary `POST /api/v1/srs/review`: từ chối tạo `CardProgress` cho từ vựng chưa thuộc bất kỳ bài học `Approved` nào với HTTP 422 `UNPROCESSABLE_ENTITY` và 0 DB mutation; bảo toàn tính độc lập của 214 bộ thủ Khang Hy; bảo toàn quyền ôn tập của các `CardProgress` lịch sử).
* **Task R2.1A [COMPLETED]:** Concurrent Eligibility / Moderation Boundary Verification (Kiểm chứng các kịch bản tương tranh thực sự giữa giao dịch `reviewCard()` và giao dịch kiểm duyệt `Approved` $\rightarrow$\rightarrow$\rightarrow$ `Rejected` với rào cản đa luồng; xác lập ngữ nghĩa MVCC Read Snapshot của MySQL 8.4 `REPEATABLE READ`).
* **Task R3.1 [COMPLETED]:** Vocabulary Lifecycle & Learning History Integrity (Chốt chặn toàn vẹn tham chiếu tại `deleteVocabulary`: chặn xóa nếu từ vựng thuộc bài học, có ghi chú cá nhân, có tiến trình `CardProgress`, hoặc có nhật ký `ReviewLog` với HTTP 409 `CONFLICT`; cho phép xóa an toàn từ vựng không tham chiếu với 204 No Content và cascade `VOCAB_RADICAL`).
* **Task R3.1A [COMPLETED]:** Vocabulary Delete / Learning Mutation Concurrency Hardening (Khóa độc quyền cấp hàng `findByIdWithLock(vocabId)` tại ranh giới đột biến; thiết lập thứ tự khóa nghiêm ngặt $\rightarrow$\text{Vocabulary} \prec \text{UserProfile} \prec \text{CardProgress}$\rightarrow$ phòng ngừa hiệu quả rủi ro Deadlock theo Dijkstra; bảo đảm không sinh bản ghi mồ côi dưới mọi kịch bản tương tranh).
* **Task R3.2 [COMPLETED]:** Lesson Lifecycle & Moderation History Integrity (Chốt chặn bảo vệ nhật ký kiểm duyệt bất biến `existsByLesson_LessonId` kết hợp DB FK `fk_moderation_log_lesson ON DELETE RESTRICT`; bài học `Draft` xóa an toàn cascade `LESSON_VOCABULARY` và bảo toàn `VOCABULARY`; bài học `Pending` và `Approved` bị khóa không cho xóa/sửa trực tiếp; phân quyền cấp đối tượng chặt chẽ; tiến trình SRS hoàn toàn độc lập với bài học).
* **Task R3.3 & R3.3A [COMPLETED]:** Chinese Domain, Kangxi Radicals & Pinyin Normalization Search Integrity (Kiểm toán 214 bộ thủ Khang Hy đối chiếu Unicode 17.0/Unihan; phát hiện 2 bản ghi thiếu Pinyin trong seed gốc ID 49 `jǐ` và ID 172 `zhuī`; chứng minh Zero Collision trên `uk_vocab_hanzi_pinyin_raw` giữa các chữ Hán khác nhau cùng âm như `绿` lǜ vs `路` lù; chứng minh tính bất biến tuyệt đối của chữ Hán CJK Unified Ideographs dưới chuẩn hóa Unicode; đồng bộ hóa pipeline `toPinyinRaw` giữa Excel Import và Runtime CRUD).
* **Checkpoint Backend Hardening [COMPLETED]:** Toàn bộ test suite chạy trên Testcontainers MySQL 8.4: **`1059/1059 tests PASS, Failures: 0, Errors: 0, Skipped: 0`** (`BUILD SUCCESS`).

---

### BACKEND HARDENING TASKS (R3.4 → R3.11) [COMPLETED]

Các nhiệm vụ kỹ thuật Backend đã được xác định qua các đợt audit trước, được xếp lịch thực thi theo thứ tự tuần tự trước khi chính thức mở cổng sang Phase 9 (Frontend):

* **Task R3.4 — Object-Level Authorization & API Boundary Audit**
  - *Status:* **`COMPLETED`**
  - *Priority:* `P0 (High)`
  - *Purpose:* Kiểm toán toàn diện ma trận phân quyền cấp đối tượng (Object-Level Authorization / IDOR) và phân quyền vai trò (Role-Based Method Security) trên tất cả các Controller endpoints. **Result: No issues found — 15 controllers, 45 endpoints audited per OWASP API1/API3/API5, RFC 9110.**

* **Task R3.5 — Personal Notes Pagination & Resource Consumption Hardening**
  - *Status:* **`COMPLETED`**
  - *Priority:* `P1`
  - *Purpose:* Bổ sung phân trang (`Pageable`) cho endpoint `GET /api/v1/vocabularies/{vocabId}/notes`. **Result: Added pagination with default=20, max=100, stable ordering by createdAt DESC.**

* **Task R3.6 — Login Rate Limiter TTL Cache Eviction & Memory Lifecycle**
  - *Status:* **`COMPLETED`**
  - *Priority:* `P1`
  - *Purpose:* Thêm cơ chế tự động giải phóng key IP khi Deque rỗng. **Result: Added explicit key eviction using `attemptHistory.remove(key, timestamps)` when Deque becomes empty after timestamp pruning (OWASP API4:2023).**

* **Task R3.7 — Dependency & Framework Security Hardening (Apache POI 5.4.0 / CVE-2025-31672)**
  - *Status:* **`COMPLETED`**
  - *Priority:* `P2`
  - *Purpose:* Nâng cấp thư viện Apache POI `poi-ooxml` từ `5.3.0` lên `5.4.0` (phiên bản vá chính thức đầu tiên của CVE-2025-31672), giữ nguyên Spring Boot 3.3.5 tương thích 100% với `commons-lang3:3.14.0`, bổ sung bộ kiểm thử bảo mật file ZIP OOXML trùng tên entry (`ExcelParserServiceTests$\rightarrow$CveAndHardeningSecurityTests`), full regression PASS 1068/1068 tests.

* **Task R3.8 — Kangxi Radicals Missing Pinyin Correction & Seed Data Ingestion**
  - *Status:* **`COMPLETED`**
  - *Priority:* `P2`
  - *Purpose:* Khắc phục lỗi thiếu Pinyin trong seed data gốc tại Flyway migration `V3` đối với 2 bộ thủ: ID 49 (`己` $\rightarrow$
ightarrow$\rightarrow$ `jǐ`) và ID 172 (`隹` $\rightarrow$
ightarrow$\rightarrow$ `zhuī`) thông qua migration tăng dần `V7__correct_radical_pinyin.sql`; bảo toàn 100% 214 radicals và empty `meaning_vi`.

* **Task R3.9 — Database Index Performance & Slow Query Verification (`EXPLAIN`)**
  - *Status:* **`COMPLETED`**
  - *Priority:* `P2`
  - *Purpose:* Chạy kiểm tra execution plan qua `EXPLAIN` và `EXPLAIN ANALYZE TREE` trên các truy vấn trọng yếu (`idx_card_progress_due`, `idx_vocab_pinyin_raw`, `idx_lesson_status`), xác minh loại bỏ 100% filesort và covering index scan; phát hiện redundant index `idx_vocab_hanzi`; kết luận `INDEXES VERIFIED, NO CHANGE REQUIRED`.

* **Task R3.10 — Production HTTP & Reverse Proxy Security Headers**
  - *Status:* **`COMPLETED`**
  - *Priority:* `P3`
  - *Purpose:* Kiểm chứng Spring Security 6 default headers (nosniff, DENY, XSS 0, no-cache, HSTS trên HTTPS); bổ sung Referrer-Policy strict-origin-when-cross-origin; cấu hình `server.forward-headers-strategy: $\rightarrow${SERVER_FORWARD_HEADERS_STRATEGY:framework}` cho Reverse Proxy; phân lập login rate limit theo client IP; kiểm chứng chiến lược vô hiệu hóa none; hoãn lại CSP có chủ đích sang Phase 9 Frontend.

* **Task R3.11 — Backend Final Quality Gate & Pre-Frontend Release Seal**
  - *Status:* **`COMPLETED`**
  - *Priority:* `P0 (Pre-Frontend Gate)`
  - *Purpose:* Chạy toàn bộ regression test suite, nghiệm thu hợp đồng REST API cuối cùng (45 endpoints), xác minh CSDL V1..V7, và đóng dấu niêm phong Backend (DEC-41).

* **Final Adversarial Backend Remediation & Security Hardening (DEC-42)**
  - *Status:* **`COMPLETED`**
  - *Priority:* `P0 (Security & Correctness)`
  - *Purpose:* Khắc phục và gia cố triệt để các phát hiện bảo mật và đúng đắn từ đợt kiểm thử đối kháng cuối cùng (DEC-42): (1) Khóa bi quan và đồng bộ hóa thu hồi token khi đổi trạng thái tài khoản (`findByIdForUpdate` + `incrementAuthorizationVersion`); (2) Quản lý bộ nhớ có ranh giới, dọn dẹp chủ động và chống bão quét CPU trong Rate Limiter (`cleanupExpiredEntries`, opportunistic CAS cooldown, fail-closed 10,000 keys per IP); (3) Ràng buộc toàn vẹn phần tử trong danh mục DTO (`@NotNull`, `@Positive`, `@Min(0)`) và chuẩn hóa xử lý lỗi HTTP 400 (`ConstraintViolationException`, `IllegalArgumentException`). Toàn bộ 1,101/1,101 tests PASS trên Testcontainers MySQL 8.4 (0 failures, 0 errors, 0 skipped).

---

### Phase 9 — Frontend UI & Client API Integration [UNBLOCKED / READY TO COMMENCE]
- **Mục tiêu:** Xây dựng toàn bộ giao diện người dùng bằng Vanilla JavaScript (ES6+ modular, native `fetch()`, zero-build complexity), kết hợp HTML5 ngữ nghĩa và CSS3 design tokens tùy biến (`frontend/css/style.css`). Tích hợp Bootstrap 5.3 CDN có SRI làm công cụ hỗ trợ layout grid và accessible UI primitives; kết nối trực tiếp với 15 Controllers (48 Java handler methods / 49 HTTP method+path mappings, 1 health probe) đã niêm phong của Spring Boot Backend.
- **Kiến trúc Frontend chuẩn:** 4 phân tầng mô-đun:
  - `frontend/js/api/api.js`: Central HTTP API Client.
  - `frontend/js/auth/auth-state.js`: Client-side session and auth state (`localStorage` untrusted convenience storage).
  - `frontend/js/ui/ui.js` & `frontend/js/ui/security.js`: Shared UI primitives và safe DOM helpers.
  - `frontend/js/pages/*-page.js`: Page-level controllers (một file module ES cho từng trang HTML).
  - `frontend/js/app.js`: Core shell initialization.
- **Ranh giới phụ thuộc:** Phụ thuộc vào Backend Final Quality Gate (DEC-41, DEC-42). Toàn bộ API Backend đã niêm phong.

#### Module 9A: Frontend Core Shell, Design System & API Client Architecture
* **Task 9A.1: Frontend Foundation, Core Shell, Design System & Browser-Safe UI**
  - *Mục tiêu:* Thiết lập nền tảng giao diện ban đầu của ứng dụng: khung cấu trúc HTML5 ngữ nghĩa (`frontend/index.html`), hệ thống design tokens CSS chuẩn mực (`frontend/css/style.css`), tích hợp Bootstrap 5.3 CDN có kiểm tra SRI, bộ công cụ safe DOM chống XSS (`frontend/js/ui/security.js`), các UI primitives tái sử dụng (`frontend/js/ui/ui.js`), và script khởi tạo ứng dụng (`frontend/js/app.js`).
  - *Phạm vi & Ranh giới (Boundary):* **Thuần giao diện tĩnh và UI primitives, không chứa bất kỳ logic mạng tầng ứng dụng/API nào (no application/API network logic).** Không chứa logic gọi API, không xử lý vòng đời phiên người dùng, không phụ thuộc Actuator health probe hay 401 redirect.
  - *Đặc tả Thiết kế Giao diện (Design Tokens & Shell):*
    - *Bảng Màu Nhận diện (Color Roles):* Nền giấy tuyên truyền thống ngà ấm (`Warm Rice Paper #FAF8F5`), nền card trắng sứ (`#FFFFFF`), chữ màu mực tàu mun tương phản cao (`#1A1A1A` cho văn bản chính), điểm nhấn son chu sa (`Cinnabar Vermilion #C83C23` cho các hành động chính, thẻ SRS cần ôn, điểm nhấn văn hóa), men ngọc bích (`Celadon Jade #2E7D5B` cho trạng thái thành công, bài học Approved, thẻ Easy), và vàng lưu ly trầm (`Amber #D97706` cho trạng thái cảnh báo, bài học Pending).
    - *Thang Khoảng cách (Spacing Scale) & Ngôn ngữ Hình khối:* Hệ thống lưới dựa trên bội số 4px/8px (4, 8, 12, 16, 24, 32, 48px); bán kính bo góc tinh tế (4px cho nút bấm và input, 6px - 8px cho panel và modal).
    - *Phân định Trực quan Người học vs Quản trị (User vs Admin Visual Distinction):*
      - *Giao diện Người học & Tác giả (Learner/Creator UI):* Không gian thoáng đãng, tập trung vào trải nghiệm đọc và tương tác thẻ học, cỡ chữ dễ nhìn, nút bấm có vùng chạm lớn ($\rightarrow$\ge 44\text{px}$\rightarrow$).
      - *Giao diện Kiểm duyệt & Quản trị (Moderator/Admin UI):* Mật độ thông tin cao (High-density layout), bảng dữ liệu gọn gàng, viền phân định trạng thái rõ ràng, công cụ lọc và đối soát nhanh, cảnh báo thao tác an toàn có xác nhận kép.
    - *Chiến lược Tiếp cận (Accessibility Baseline):* Đảm bảo độ tương phản màu sắc đạt chuẩn WCAG 2.2 AA ($\rightarrow$\ge 4.5:1$\rightarrow$ cho văn bản thường, $\rightarrow$\ge 3:1$\rightarrow$ cho văn bản lớn), focus ring rõ nét không bị che khuất (Focus Not Obscured 2.4.11), và hỗ trợ điều hướng bàn phím đầy đủ.
  - *Quyết định kỹ thuật (OQ-08 / PEN-01 RESOLVED):* Sử dụng Bootstrap 5.3 qua CDN làm khung lưới hỗ trợ (grid, utilities, accessible modal/toast primitives) có kiểm tra SRI, kết hợp CSS tùy biến có hệ thống (`frontend/css/style.css`) và Vanilla JavaScript ES6+ modular; không sử dụng SPA framework nặng (React/Vue/Angular) để đảm bảo zero-build complexity, tải nhanh và dễ bảo trì. Bootstrap chỉ đóng vai trò công cụ hỗ trợ, không quyết định bản sắc trực quan của sản phẩm.
  - *depends_on:* Backend Final Quality Gate (DEC-41, DEC-42).
  - *Deliverables:* `frontend/index.html`, `frontend/css/style.css`, `frontend/js/ui/ui.js` (toast/modal/3-state helpers), `frontend/js/ui/security.js` (safe DOM sanitization/node builders), `frontend/js/app.js`.
  - *Acceptance Criteria:*
    - [Objective] File `frontend/index.html` tải hợp lệ Bootstrap 5.3 CSS & JS bundle từ CDN kèm thuộc tính `integrity="sha384-..."` và `crossorigin="anonymous"`.
    - [Objective] Hệ thống CSS design tokens trong `style.css` định nghĩa đầy đủ bảng màu nhận diện, typography và spacing scale.
    - [Objective] Các hàm dựng DOM trong `security.js` triệt để sử dụng `textContent`, `document.createElement`, không cho phép tiêm chuỗi HTML thô chưa khử độc.
    - [Manual] Mở `index.html` trong trình duyệt: Khung giao diện responsive chính xác ở 375px (mobile), 768px (tablet) và 1200px (desktop); các UI primitives (modal, toast, loading spinner, error alert, empty state) kích hoạt hiển thị ổn định; navbar hiển thị mục điều hướng phù hợp; zero console error.

* **Task 9A.2: Centralized HTTP API Client & Session Manager (`frontend/js/api/api.js`)**
  - *Mục tiêu:* Xây dựng lớp giao tiếp HTTP API tập trung (`frontend/js/api/api.js`) đóng gói `fetch()` nguyên bản của trình duyệt, quản lý vòng đời phiên người dùng (`frontend/js/auth/auth-state.js`), và chuẩn hóa lỗi giao tiếp với máy chủ.
  - *Chức năng cốt lõi:*
    - *Base URL & Headers:* Tiền tố cấu hình linh hoạt (mặc định same-origin relative path `/api/v1`, cho phép override qua đối tượng cấu hình chỉ-đọc `window.__ENV__?.API_BASE_URL` cho môi trường dev cross-origin), tự động đính kèm header `Content-Type: application/json` (trừ upload multipart file), và tự động tiêm `Authorization: Bearer <token>` từ session manager.
    - *Token & Session Lifecycle (Untrusted Convenience Storage):* Quản lý token trong `localStorage`, hỗ trợ đăng xuất an toàn và dọn dẹp bộ nhớ/session. Nhận định rõ `localStorage` là client-controlled convenience storage, không phải vùng bảo mật; logic vai trò chỉ phục vụ điều hướng UX.
    - *Chuẩn hóa Phản hồi & Bóc tách Envelope:* Xử lý bóc tách định dạng phong bì chuẩn `ApiResponse<T>`: kiểm tra `code === "SUCCESS"`, trích xuất `data`, và chuyển đổi mảng `errors[]` chi tiết (chuỗi dạng `"$\rightarrow${field}: $\rightarrow${message}"` do `GlobalExceptionHandler` trả về) qua hàm helper `parseFieldErrors()` thành đối tượng lỗi dễ hiển thị trên form. Bỏ qua bóc tách JSON với phản hồi HTTP 204 No Content.
    - *Xử lý Mã lỗi HTTP Tập trung:*
      - *HTTP 401 Unauthorized:* Tự động xóa token khỏi storage, kích hoạt sự kiện `auth:expired`, chuyển hướng người dùng về `login.html` và hiển thị thông báo "Phiên đăng nhập đã hết hạn hoặc đã bị thu hồi, vui lòng đăng nhập lại".
      - *HTTP 403 Forbidden:* Bắt lỗi truy cập trái quyền, hiển thị thông báo "Bạn không có quyền thực hiện thao tác này".
      - *HTTP 429 Too Many Requests:* Bắt lỗi giới hạn tần suất (rate limiting), trích xuất thông điệp yêu cầu người dùng chờ đợi trước khi thử lại (ghi nhận backend không trả header `Retry-After`).
      - *HTTP 4xx / 5xx Normalization:* Chuyển đổi mã lỗi nghiệp vụ thành thông điệp tiếng Việt thân thiện, không làm lộ chi tiết kỹ thuật nội bộ.
      - *Sự cố Mạng (Network Failure) & Timeout:* Sử dụng `AbortController` thiết lập timeout mặc định (15 giây cho request thường, 60 giây cho upload file); xử lý bắt lỗi mạng hiển thị thông điệp "Không thể kết nối đến máy chủ, vui lòng kiểm tra mạng".
      - *Cơ chế Thử lại (Safe Retry):* Chỉ tự động thử lại tối đa 1 lần đối với các request GET an toàn (idempotent) khi gặp sự cố mạng chập chờn; tuyệt đối không tự động thử lại các mutation request (POST, PUT, DELETE) để phòng tránh duplicate side-effects.
    - *Health Probe Verification:* Sử dụng `GET /actuator/health` làm công cụ chẩn đoán xác minh kết nối (verification mechanism), hoàn toàn không đưa vào luồng nghiệp vụ của ứng dụng.
  - *depends_on:* Task 9A.1.
  - *Deliverables:* `frontend/js/api/api.js`, `frontend/js/auth/auth-state.js`.
  - *Acceptance Criteria:*
    - [Objective] Gọi thử nghiệm thành công probe kiểm tra sức khỏe `GET /actuator/health` trả về `{status: "UP"}`.
    - [Objective] `api.js` parse chính xác cấu trúc `{code, message, data, errors}` của `ApiResponse<T>`, đồng thời chuyển đổi mảng `errors: ["field: message"]` thành map `{field: message}` qua `parseFieldErrors()`.
    - [Objective] Các request GET idempotent có cơ chế timeout qua `AbortController` và retry tối đa 1 lần khi lỗi mạng, trong khi request POST/PUT/DELETE không bao giờ tự retry.
    - [Manual] Kiểm tra tương tác với token giả mạo/hết hạn trên trình duyệt: `api.js` bắt đúng HTTP 401, xóa token khỏi `localStorage`, kích hoạt toast cảnh báo và chuyển hướng an toàn về `login.html`.

* **Checkpoint 9A (Foundation & Client Integration Verified):** Kiểm tra tích hợp trên trình duyệt: Mở `index.html` $\rightarrow$ Khung nền hiển thị responsive chuẩn không lỗi console $\rightarrow$ `api.js` gọi thành công probe chẩn đoán tới Backend $\rightarrow$ Kịch bản 401 tự động chuyển hướng đăng nhập mượt mà.

#### Module 9B: Learner Identity & Chinese Catalog UI
* **Task 9B.1: Authentication & Learner Profile UI (`login.html`, `register.html`, `profile.html`)**
  - *Mục tiêu:* Xây dựng giao diện đăng ký tài khoản, đăng nhập xác thực và quản lý hồ sơ cá nhân người học.
  - *API kết nối:* `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `GET /api/v1/users/profile`, `PUT /api/v1/users/profile`.
  - *Tiếp cận & Khả năng Tiếp cận (WCAG 2.2):* Form đăng nhập tuân thủ tiêu chí Accessible Authentication 3.3.8 (cho phép dán mật khẩu từ trình quản lý mật khẩu, không chặn copy-paste); nhãn form `<label for="...">` rõ ràng; thông báo lỗi input hiển thị tức thì qua `aria-describedby`.
  - *depends_on:* Module 9A.
  - *Deliverables:* `frontend/login.html`, `frontend/register.html`, `frontend/profile.html`, `frontend/js/pages/auth-page.js`, `frontend/js/pages/profile-page.js`.
  - *Acceptance Criteria:* Đăng ký tài khoản mới thành công nhận HTTP 201 và chuyển hướng đăng nhập; đăng nhập thành công lưu token, cập nhật navbar hiển thị tên và vai trò; xem hồ sơ cá nhân hiển thị chính xác họ tên và avatar; cập nhật hồ sơ lưu thành công lên MySQL; gửi mật khẩu sai bắt đúng lỗi 401; gửi login liên tục $\rightarrow$\ge 10$\rightarrow$ lần bắt đúng mã lỗi HTTP 429 và hiển thị thông báo tạm khóa.
* **Task 9B.2: 214 Kangxi Radicals Catalog & Presentation UI (`radicals.html`)**
  - *Mục tiêu:* Xây dựng giao diện khám phá và tra cứu danh mục 214 bộ thủ Khang Hy chuẩn mực theo thứ tự số nét kinh điển.
  - *API kết nối:* `GET /api/v1/radicals` (Pageable), `GET /api/v1/radicals/{id}`.
  - *Thiết kế giao diện:* Lưới hiển thị 214 bộ thủ Khang Hy sắp xếp chuẩn xác theo `radical_id` tăng dần (thứ tự kinh điển radical_id 1–214); ô tìm kiếm theo Hán tự hoặc nghĩa Hán-Việt; modal chi tiết hiển thị Hán tự kích thước lớn, Pinyin chuẩn thanh điệu (chuẩn V7), âm Hán-Việt, giải nghĩa tiếng Việt, và trình phát audio/video nét viết (nếu có URL).
  - *depends_on:* Module 9A.
  - *Deliverables:* `frontend/radicals.html`, `frontend/js/pages/radicals-page.js`.
  - *Acceptance Criteria:* Hiển thị đầy đủ 214 bộ thủ Khang Hy không thiếu sót; tìm kiếm và lọc phản hồi mượt mà với debounce (300ms) và chỉ báo trạng thái tải dữ liệu; modal chi tiết mở nhanh và đóng bằng phím Escape hoặc nút bấm có ARIA label; hỗ trợ phân trang hoặc duyệt lưới mượt mà.
* **Task 9B.3: Chinese Vocabulary Catalog & Search UI (`vocabulary.html`)**
  - *Mục tiêu:* Xây dựng giao diện tra cứu từ vựng tiếng Trung với công cụ tìm kiếm đa tiêu chí.
  - *API kết nối:* `GET /api/v1/vocabulary` (search, pageable), `GET /api/v1/vocabulary/{id}`.
  - *Thiết kế giao diện:* Thanh tìm kiếm hỗ trợ nhập chữ Hán hoặc Pinyin (cả có dấu thanh điệu lẫn không dấu thanh điệu, e.g. `ni` $\rightarrow$\rightarrow$\rightarrow$ ra `nǐ` / `你`); bảng danh sách từ vựng hiển thị chữ Hán, Pinyin, âm Hán-Việt, nghĩa tiếng Việt, và các huy hiệu (badges) bộ thủ cấu thành; modal chi tiết từ vựng kèm câu ví dụ và nút phát âm audio (Web Speech API); phân trang danh mục rõ ràng (PageResponse).
  - *depends_on:* Module 9A.
  - *Deliverables:* `frontend/vocabulary.html`, `frontend/js/pages/vocabulary-page.js`.
  - *Acceptance Criteria:* Tìm kiếm bằng Pinyin không dấu trả về kết quả chuẩn xác; click vào huy hiệu bộ thủ mở modal thông tin bộ thủ đó; chuyển trang phân trang mượt mà; xử lý hoàn hảo 3 trạng thái Loading, Empty (không tìm thấy từ phù hợp), và Error.
* **Checkpoint 9B (Catalog & Identity Verified):** Kiểm tra hành vi thực tế: Đăng ký tài khoản mới $\rightarrow$\rightarrow$\rightarrow$ Đăng nhập thành công $\rightarrow$\rightarrow$\rightarrow$ Xem hồ sơ cá nhân $\rightarrow$\rightarrow$\rightarrow$ Mở danh mục 214 bộ thủ Khang Hy $\rightarrow$\rightarrow$\rightarrow$ Tìm kiếm từ vựng bằng Pinyin không dấu `ni` hiển thị đúng từ vựng tương ứng.

#### Module 9C: Learner Learning Experience & SRS Space
* **Task 9C.1: Public Lessons & Lesson Detail UI (`lessons.html`, `lesson-detail.html`)**
  - *Mục tiêu:* Xây dựng giao diện khám phá danh sách bài học công khai đã được phê duyệt (`Approved`) và trang chi tiết bài học.
  - *API kết nối:* `GET /api/v1/lessons` (Pageable), `GET /api/v1/lessons/{id}`.
  - *depends_on:* Module 9B.
  - *Deliverables:* `frontend/lessons.html`, `frontend/lesson-detail.html`, `frontend/js/pages/lessons-page.js`, `frontend/js/pages/lesson-detail-page.js`.
  - *Acceptance Criteria:* Chỉ hiển thị các bài học ở trạng thái `Approved` (bài Draft/Pending/Rejected tuyệt đối không hiển thị ở catalog công khai); danh sách từ vựng trong trang chi tiết bài học hiển thị tuần tự theo đúng thứ tự `order_index` (1, 2, 3...) do tác giả sắp xếp; hiển thị số lượng từ vựng và ngày xuất bản; xử lý 3 trạng thái Loading/Empty/Error đầy đủ.
* **Task 9C.2: Contextual Personal Notes UI (`frontend/js/ui/notes-modal.js`)**
  - *Mục tiêu:* Xây dựng phân hệ ghi chú cá nhân học viên tích hợp ngay trong ngữ cảnh bài học và danh mục từ vựng.
  - *API kết nối:* `GET /api/v1/vocabularies/{vocabId}/notes` (Pageable, default 20, max 100), `POST /api/v1/vocabularies/{vocabId}/notes`, `PUT /api/v1/notes/{noteId}`, `DELETE /api/v1/notes/{noteId}`.
  - *Bảo mật & Ràng buộc:* Giới hạn nội dung ghi chú $\rightarrow$\le 500$\rightarrow$ ký tự (có bộ đếm ký tự trực tiếp trên form); render an toàn chống XSS qua `textContent`; phân trang danh sách ghi chú; bảo đảm cô lập quyền sở hữu (người học chỉ thấy và thao tác được ghi chú của chính mình).
  - *depends_on:* Task 9C.1.
  - *Deliverables:* `frontend/js/ui/notes-modal.js`, modal/popover ghi chú trong `lesson-detail.html` và `vocabulary.html`.
  - *Acceptance Criteria:* Người học thêm ghi chú mới thành công hiển thị ngay lập tức trong danh sách; form chặn không cho nhập quá 500 ký tự; cập nhật và xóa ghi chú hoạt động trơn tru; người dùng chưa đăng nhập click vào nút ghi chú được nhắc nhở đăng nhập.
* **Task 9C.3: Interactive SRS Flashcard Review Session UI (`srs-review.html`)**
  - *Mục tiêu:* Xây dựng phòng ôn tập Flashcard tương tác theo thuật toán lặp lại ngắt quãng SM-2.
  - *API kết nối:* `GET /api/v1/srs/due` (lấy danh sách thẻ đến hạn), `GET /api/v1/srs/lessons/{id}/new-cards` (thẻ mới từ bài học đã duyệt), `POST /api/v1/srs/review` (gửi kết quả ôn tập: `rating` 1..4, `review_time_seconds`).
  - *Thiết kế Tương tác Flashcard:*
    - *Mặt trước (Prompt):* Chữ Hán kích thước lớn, phiên âm Pinyin, nút phát âm audio, và bộ đếm thời gian phản xạ (tính theo giây).
    - *Lật thẻ (3D Flip Interaction):* Lật thẻ 3D mượt mà bằng CSS `transform: rotateY(180deg)` khi bấm phím Space, phím Enter hoặc click vào thẻ.
    - *Mặt sau (Answer):* Nghĩa Hán-Việt, dịch nghĩa tiếng Việt, các bộ thủ cấu thành, câu ví dụ và ghi chú cá nhân của người học.
    - *4 Nút Đánh giá SM-2:*
      - Phím `1` / Nút đỏ: **Again** ($\rightarrow$q=0$\rightarrow$, học lại ngay, interval reset 0 ngày).
      - Phím `2` / Nút cam: **Hard** ($\rightarrow$q=3$\rightarrow$, nhớ khó khăn).
      - Phím `3` / Nút xanh lam: **Good** ($\rightarrow$q=4$\rightarrow$, nhớ tốt, khoảng cách chuẩn).
      - Phím `4` / Nút xanh lá: **Easy** ($\rightarrow$q=5$\rightarrow$, quá dễ, khoảng cách mở rộng tối đa).
    - *Xử lý Hàng đợi & Trạng thái:* Khi đánh giá một thẻ, tự động chuyển thẻ tiếp theo mượt mà; thẻ bị đánh giá `Again` được đưa về cuối phiên học để ôn lại trong ngày; khi hết thẻ hiển thị màn hình chúc mừng hoàn thành phiên học kèm thống kê tổng kết.
  - *depends_on:* Task 9C.1, Task 9C.2.
  - *Deliverables:* `frontend/srs-review.html`, `frontend/js/pages/srs-review-page.js`.
  - *Acceptance Criteria:* Lật thẻ mượt mà không giật hình; gửi request `POST /api/v1/srs/review` ghi nhận đúng `rating` và `review_time_seconds`; hỗ trợ đầy đủ phím tắt (Space lật thẻ, phím 1..4 đánh giá); kết thúc phiên hiển thị màn hình hoàn thành.
* **Task 9C.4: Study Statistics & SRS Daily Settings UI (`srs-dashboard.html`)**
  - *Mục tiêu:* Xây dựng bảng điều khiển thống kê học tập cá nhân và tùy chỉnh định ngạch ôn tập hàng ngày.
  - *API kết nối:* `GET /api/v1/srs/stats`, `GET /api/v1/srs/settings`, `PUT /api/v1/srs/settings`.
  - *Bảo đảm Hợp đồng API Thực tế:* Chỉ sử dụng các trường dữ liệu thực tế được trả về từ `StudyStatsResponse`: `cardsDue` (thẻ đến hạn), `reviewsToday` (số thẻ đã ôn hôm nay), `newCardsToday` (số thẻ mới đã học hôm nay), `newCardsLimit` (hạn mức thẻ mới hàng ngày), `maxReviewLimit` (hạn mức ôn tập tối đa hàng ngày). Tuyệt đối không tự bịa đặt các biểu đồ phân tích không có trong API backend.
  - *depends_on:* Task 9C.3.
  - *Deliverables:* `frontend/srs-dashboard.html`, `frontend/js/pages/srs-dashboard-page.js`.
  - *Acceptance Criteria:* Dashboard hiển thị trực quan 5 chỉ số thống kê từ API; form cập nhật `newCardsPerDay` và `maxReviewPerDay` kiểm tra số nguyên $\rightarrow$> 0$\rightarrow$; cập nhật setting thành công làm mới ngay lập tức các chỉ số thống kê trên màn hình.
* **Checkpoint 9C (Learning Loop Verified):** Kiểm tra hành vi thực tế: Người học mở bài học `Approved` $\rightarrow$\rightarrow$\rightarrow$ Đọc từ vựng theo thứ tự $\rightarrow$\rightarrow$\rightarrow$ Tạo một ghi chú cá nhân $\rightarrow$\rightarrow$\rightarrow$ Mở phòng ôn tập SRS `srs-review.html` $\rightarrow$\rightarrow$\rightarrow$ Lật thẻ và chọn rating 3 (Good) $\rightarrow$\rightarrow$\rightarrow$ Chuyển sang `srs-dashboard.html` kiểm tra chỉ số `reviewsToday` tăng thêm 1.

#### Module 9D: Creator Studio & Two-Step Excel Import Experience
* **Task 9D.1: Creator Lesson Studio & Vocabulary Ordering UI (`creator-lessons.html`, `creator-lesson-editor.html`)**
  - *Mục tiêu:* Xây dựng không gian làm việc dành riêng cho Tác giả nội dung: quản lý danh sách bài học cá nhân, soạn thảo bài học nháp, thêm/bớt từ vựng, và sắp xếp thứ tự từ vựng linh hoạt.
  - *API kết nối:* `GET /api/v1/creator/lessons` (Pageable), `POST /api/v1/creator/lessons`, `GET /api/v1/creator/lessons/{id}`, `PUT /api/v1/creator/lessons/{id}`, `DELETE /api/v1/creator/lessons/{id}`, `POST/DELETE /api/v1/creator/lessons/{id}/vocabularies/{vocabId}`, `PUT/POST /api/v1/creator/lessons/{id}/reorder`.
  - *Bảo mật & Cô lập Quyền sở hữu:* Giao diện chỉ cho phép tài khoản có vai trò `ROLE_CREATOR` hoặc `ROLE_ADMIN` truy cập; danh sách chỉ hiển thị bài học do chính tác giả tạo; kiểm tra phân quyền sở hữu an toàn.
  - *depends_on:* Module 9A, Module 9B.
  - *Deliverables:* `frontend/creator-lessons.html`, `frontend/creator-lesson-editor.html`, `frontend/js/pages/creator-lessons-page.js`, `frontend/js/pages/creator-lesson-editor-page.js`.
  - *Acceptance Criteria:* Creator tạo bài học mới ở trạng thái `Draft`; thêm từ vựng vào bài học; điều chỉnh thứ tự từ vựng bằng nút mũi tên lên/xuống hoặc kéo thả, gửi request reorder bảo toàn chuỗi `order_index` tuần tự; bài học ở trạng thái `Pending` hoặc `Approved` bị khóa nút chỉnh sửa để bảo toàn tính bất biến.
* **Task 9D.2: Two-Step Excel Import UI (`creator-import.html`)**
  - *Mục tiêu:* Xây dựng giao diện nhập liệu bài học từ file Excel 2 bước chuẩn mực: Phân tách rõ ràng giữa Xem trước dữ liệu (Preview) và Lưu bền vững vào cơ sở dữ liệu (Confirm).
  - *API kết nối:* `POST /api/v1/creator/lessons/import` (Preview), `POST /api/v1/creator/lessons/import/confirm` (Confirm).
  - *Quy trình 2 bước Nghiêm ngặt:*
    - *Bước 1 — Xem trước dữ liệu (Preview):* Dropzone nhận file `.xlsx` (giới hạn kích thước file 10MB); gửi request tới endpoint preview; server phân tích và trả về `ImportValidationReport`; giao diện hiển thị bảng dữ liệu xem trước gồm tổng số dòng hợp lệ, số dòng lỗi, và chi tiết lỗi trên từng ô/cột (e.g. thiếu Hán tự, sai định dạng Pinyin); **khẳng định rõ ràng trên UI: Bước này hoàn toàn KHÔNG ghi dữ liệu vào CSDL (zero DB mutation)**.
    - *Bước 2 — Xác nhận tạo bài học (Confirm):* Người dùng chỉ được bấm nút "Xác nhận Import" khi file không có lỗi nghiêm trọng; nhập tiêu đề bài học; gửi request confirm để tạo bài học nguyên tử (Atomic Transaction); sau khi confirm thành công, hệ thống điều hướng về trang biên tập bài học nháp (`Draft`).
  - *depends_on:* Task 9D.1.
  - *Deliverables:* `frontend/creator-import.html`, `frontend/js/pages/creator-import-page.js`.
  - *Acceptance Criteria:* Tải file Excel hợp lệ hiển thị bảng preview chuẩn; tải file hỏng hoặc file vượt 5000 dòng hiển thị thông báo lỗi rõ ràng; thao tác confirm tạo thành công bài học mới với danh sách từ vựng theo đúng thứ tự file Excel.
* **Task 9D.3: Submission Workflow & Status Visibility UI (`frontend/js/pages/creator-lessons-page.js`)**
  - *Mục tiêu:* Xây dựng luồng nộp bài học chờ kiểm duyệt và hiển thị phản hồi kiểm duyệt.
  - *API kết nối:* `POST /api/v1/creator/lessons/{id}/submit`.
  - *depends_on:* Task 9D.1, Task 9D.2.
  - *Deliverables:* Modal nộp bài và chỉ báo trạng thái trong `creator-lessons.html`.
  - *Acceptance Criteria:* Bài học ở trạng thái `Draft` hoặc `Rejected` có nút "Nộp duyệt"; bấm nộp có modal xác nhận; nộp thành công bài học chuyển sang trạng thái `Pending` và hiển thị huy hiệu vàng chờ duyệt; đối với bài học từng bị từ chối (`Rejected`), giao diện hiển thị rõ lý do từ chối và các trường lỗi (`flaggedFields`) do kiểm duyệt viên phản hồi để tác giả sửa chữa.
* **Checkpoint 9D (Content Creation Verified):** Kiểm tra hành vi thực tế: Creator đăng nhập $\rightarrow$\rightarrow$\rightarrow$ Tải file Excel hợp lệ tại `creator-import.html` $\rightarrow$\rightarrow$\rightarrow$ Xem bảng preview $\rightarrow$\rightarrow$\rightarrow$ Xác nhận import $\rightarrow$\rightarrow$\rightarrow$ Bài học nháp được tạo thành công $\rightarrow$\rightarrow$\rightarrow$ Đổi thứ tự 2 từ vựng $\rightarrow$\rightarrow$\rightarrow$ Bấm Nộp duyệt $\rightarrow$\rightarrow$\rightarrow$ Bài học chuyển trạng thái thành `Pending` và khóa chỉnh sửa.

#### Module 9E: Moderator Review Dashboard & History Experience
* **Task 9E.1: Moderation Queue UI (`moderator-queue.html`)**
  - *Mục tiêu:* Xây dựng bàn làm việc cho Kiểm duyệt viên hiển thị hàng đợi các bài học đang chờ phê duyệt (`Pending`).
  - *API kết nối:* `GET /api/v1/moderator/lessons/pending` (Pageable).
  - *Bảo mật & Phân quyền:* Chỉ cho phép người dùng có vai trò `ROLE_MODERATOR` hoặc `ROLE_ADMIN` truy cập; người dùng Learner/Creator bị từ chối 403.
  - *depends_on:* Module 9A, Module 9D.
  - *Deliverables:* `frontend/moderator-queue.html`, `frontend/js/pages/moderator-queue-page.js`.
  - *Acceptance Criteria:* Danh sách hiển thị đầy đủ các bài học `Pending` kèm tên tác giả, ngày nộp và số lượng từ vựng; hỗ trợ phân trang; click vào bài học dẫn sang màn hình xem xét chi tiết nội dung.
* **Task 9E.2: Lesson Review & Vocabulary Inspection UI (`moderator-review.html`)**
  - *Mục tiêu:* Xây dựng màn hình kiểm tra chi tiết nội dung bài học trước khi đưa ra quyết định duyệt.
  - *API kết nối:* `GET /api/v1/moderator/lessons/{id}`.
  - *depends_on:* Task 9E.1.
  - *Deliverables:* `frontend/moderator-review.html`, `frontend/js/pages/moderator-review-page.js`.
  - *Acceptance Criteria:* Hiển thị chi tiết tiêu đề bài học, thông tin tác giả, và toàn bộ bảng danh sách từ vựng theo đúng thứ tự `order_index`; cho phép nghe phát âm từng từ và kiểm tra tính chính xác của Pinyin, nghĩa tiếng Việt và bộ thủ cấu thành.
* **Task 9E.3: Approve & Reject Workflow with Flagged Fields (`frontend/js/pages/moderator-review-page.js`)**
  - *Mục tiêu:* Xây dựng modal thao tác phê duyệt (`Approve`) hoặc từ chối (`Reject`) bài học kèm phản hồi chi tiết.
  - *API kết nối:* `POST /api/v1/moderator/lessons/{id}/approve`, `POST /api/v1/moderator/lessons/{id}/reject` (`RejectLessonRequest`: `rejectionReason`, `flaggedFields`).
  - *depends_on:* Task 9E.2.
  - *Deliverables:* Modal phê duyệt và Modal từ chối tích hợp trong `moderator-review.html`.
  - *Acceptance Criteria:* Modal phê duyệt có xác nhận; Modal từ chối bắt buộc nhập lý do từ chối (không cho phép để trống, kiểm tra $\rightarrow$\le 500$\rightarrow$ ký tự) và cho phép tick chọn các trường vi phạm (e.g. `pinyin`, `meaning_vi`, `hanzi`); phê duyệt thành công bài chuyển sang `Approved` và xuất hiện ở catalog công khai; từ chối thành công bài chuyển sang `Rejected`; bài học được xử lý xong lập tức biến mất khỏi hàng đợi `Pending`.
* **Task 9E.4: Moderation History UI (`moderator-history.html`)**
  - *Mục tiêu:* Xây dựng màn hình tra cứu nhật ký lịch sử kiểm duyệt bất biến (`MODERATION_LOG`).
  - *API kết nối:* `GET /api/v1/moderator/history` (Pageable).
  - *depends_on:* Task 9E.3.
  - *Deliverables:* `frontend/moderator-history.html`, `frontend/js/pages/moderator-history-page.js`.
  - *Acceptance Criteria:* Moderator xem được lịch sử kiểm duyệt của chính mình; Admin xem được lịch sử kiểm duyệt toàn hệ thống; bảng hiển thị rõ thời gian, bài học, hành động (Approve/Reject), lý do và các trường bị đánh dấu lỗi; hỗ trợ phân trang.
* **Checkpoint 9E (Moderation Workflow Verified):** Kiểm tra hành vi thực tế: Moderator đăng nhập $\rightarrow$\rightarrow$\rightarrow$ Mở `moderator-queue.html` thấy bài học Pending của Creator $\rightarrow$\rightarrow$\rightarrow$ Bấm xem chi tiết $\rightarrow$\rightarrow$\rightarrow$ Bấm từ chối kèm lý do phản hồi $\rightarrow$\rightarrow$\rightarrow$ Kiểm tra bài học chuyển sang `Rejected` $\rightarrow$\rightarrow$\rightarrow$ Mở `moderator-history.html` kiểm tra nhật ký ghi nhận chính xác hành động kiểm duyệt.

#### Module 9F: Admin Administration & System Oversight Experience
* **Task 9F.1: User Account Lifecycle & Status Management UI (`admin-accounts.html`)**
  - *Mục tiêu:* Màn hình quản trị vòng đời tài khoản người dùng, tìm kiếm, lọc trạng thái và cập nhật trạng thái hoạt động.
  - *API kết nối:* `GET /api/v1/admin/accounts` (Pageable, status, search), `PUT /api/v1/admin/accounts/{id}/status`.
  - *Bảo vệ Tự thân (Self-Protection Policy POL-8D-01):* Giao diện vô hiệu hóa nút đổi trạng thái của chính tài khoản Admin đang đăng nhập để ngăn chặn việc tự khóa tài khoản của chính mình.
  - *depends_on:* Module 9A.
  - *Deliverables:* `frontend/admin-accounts.html`, `frontend/js/pages/admin-accounts-page.js`.
  - *Acceptance Criteria:* Chỉ cho phép `ROLE_ADMIN` truy cập; bảng phân trang danh sách tài khoản kèm bộ lọc trạng thái (`Active`, `Inactive`, `Banned`) và ô tìm kiếm theo email/phone; thao tác đổi trạng thái có modal xác nhận cảnh báo việc đổi trạng thái sẽ thu hồi tức thì token của tài khoản đó (DEC-42).
* **Task 9F.2: Administrative Role Assignment UI (`admin-roles.html`)**
  - *Mục tiêu:* Màn hình xem danh mục vai trò hệ thống và phân quyền vai trò cho tài khoản người dùng.
  - *API kết nối:* `GET /api/v1/admin/roles`, `PUT /api/v1/admin/accounts/{id}/roles`.
  - *Bảo vệ Tự thân (Self-Protection Policy POL-8D-02):* Giao diện vô hiệu hóa việc bỏ tick quyền `Admin` của chính tài khoản Admin đang đăng nhập.
  - *depends_on:* Task 9F.1.
  - *Deliverables:* `frontend/admin-roles.html`, `frontend/js/pages/admin-roles-page.js`.
  - *Acceptance Criteria:* Hiển thị danh mục 4 vai trò chuẩn (Learner, Creator, Moderator, Admin); modal phân quyền có checkbox chọn nhiều vai trò; cập nhật vai trò thành công hiển thị thông báo token cũ của tài khoản đó đã bị thu hồi qua cơ chế `authorization_version`.
* **Task 9F.3: Radicals Administration UI (`admin-radicals.html`)**
  - *Mục tiêu:* Màn hình quản trị CRUD danh mục bộ thủ dành riêng cho Quản trị viên.
  - *API kết nối:* `POST /api/v1/admin/radicals`, `PUT /api/v1/admin/radicals/{id}`, `DELETE /api/v1/admin/radicals/{id}`.
  - *depends_on:* Module 9A, Module 9B.
  - *Deliverables:* `frontend/admin-radicals.html`, `frontend/js/pages/admin-radicals-page.js`.
  - *Acceptance Criteria:* Thêm mới bộ thủ với đầy đủ validation; cập nhật bộ thủ; xóa bộ thủ hiển thị modal xác nhận; nếu bộ thủ đang có liên kết tham chiếu bị từ chối với HTTP 409 Conflict, giao diện bắt lỗi và hiển thị thông điệp cảnh báo toàn vẹn tham chiếu rõ ràng.
* **Task 9F.4: Vocabulary Administration UI (`admin-vocabulary.html`)**
  - *Mục tiêu:* Màn hình quản trị CRUD danh mục từ vựng tiếng Trung dành riêng cho Quản trị viên.
  - *API kết nối:* `POST /api/v1/admin/vocabulary`, `PUT /api/v1/admin/vocabulary/{id}`, `DELETE /api/v1/admin/vocabulary/{id}`.
  - *depends_on:* Module 9A, Module 9B.
  - *Deliverables:* `frontend/admin-vocabulary.html`, `frontend/js/pages/admin-vocabulary-page.js`.
  - *Acceptance Criteria:* Thêm mới từ vựng kèm liên kết bộ thủ cấu thành; cập nhật thông tin từ vựng; xóa từ vựng: khi từ vựng đã có liên kết bài học hoặc tiến trình ôn tập SRS, server trả về HTTP 409 Conflict, UI bắt đúng lỗi và giải thích rõ ràng không thể xóa do ràng buộc dữ liệu học tập.
* **Task 9F.5: System-Wide Lesson Oversight UI (`admin-lessons.html`)**
  - *Mục tiêu:* Màn hình giám sát và quản lý toàn bộ bài học trên hệ thống không phụ thuộc vào tác giả hay trạng thái.
  - *API kết nối:* `GET /api/v1/admin/lessons` (status, creatorId, pageable).
  - *depends_on:* Module 9A, Module 9D.
  - *Deliverables:* `frontend/admin-lessons.html`, `frontend/js/pages/admin-lessons-page.js`.
  - *Acceptance Criteria:* Xem danh sách bài học toàn hệ thống; bộ lọc đa năng theo trạng thái (`Draft`, `Pending`, `Approved`, `Rejected`) và theo tác giả; thống kê nhanh số lượng bài học theo từng trạng thái; xem chi tiết nội dung bất kỳ bài học nào.
* **Checkpoint 9F (Administration Verified):** Kiểm tra hành vi thực tế: Admin đăng nhập $\rightarrow$\rightarrow$\rightarrow$ Mở `admin-accounts.html` tìm kiếm tài khoản $\rightarrow$\rightarrow$\rightarrow$ Vô hiệu hóa tài khoản thành `Inactive` $\rightarrow$\rightarrow$\rightarrow$ Mở `admin-roles.html` cấp thêm quyền Creator $\rightarrow$\rightarrow$\rightarrow$ Thêm từ vựng mới tại `admin-vocabulary.html` $\rightarrow$\rightarrow$\rightarrow$ Giám sát toàn bộ bài học tại `admin-lessons.html`.

#### Module 9G: Frontend Security, Accessibility & Browser Hardening
* **Task 9G.1: Token Storage Threat Model Evaluation & Client Session Review**
  - *Mục tiêu:* Đánh giá mô hình đe dọa lưu trữ token phía client (`localStorage` vs `sessionStorage` vs `cookies`), lập tài liệu phân tích rủi ro trong bối cảnh kiến trúc JWT stateless của dự án.
  - *depends_on:* Hoàn thành toàn bộ Modules 9A $\rightarrow$\rightarrow$\rightarrow$ 9F.
  - *Phạm vi Đánh giá:*
    - `localStorage`: Tiện lợi cho phiên làm việc liên tục, không bị rủi ro CSRF trong kiến trúc REST API stateless, nhưng dễ tổn thương nếu ứng dụng có lỗ hổng DOM XSS.
    - `sessionStorage`: Giới hạn phạm vi theo tab, giảm thiểu thời gian lưu token nhưng gây bất tiện khi mở nhiều tab học tập.
    - *Kết luận & Giải pháp Phòng thủ Đa tầng:* Chấp nhận lưu trữ `localStorage` với điều kiện tiên quyết là phòng chống DOM XSS theo nguyên tắc Defense-in-Depth (Task 9G.2) kết hợp chính sách CSP nghiêm ngặt (Task 9G.3) và cơ chế thu hồi token tức thì phía server (`authorization_version` DEC-42).
  - *Deliverables:* Phân tích mô hình đe dọa lưu trữ token cập nhật vào `.agents/DECISIONS.md`.
* **Task 9G.2: DOM XSS Prevention & Dynamic Data Rendering Audit**
  - *Mục tiêu:* Rà soát toàn bộ 100% mã nguồn JavaScript của ứng dụng để khẳng định mọi dữ liệu không tin cậy (untrusted data từ user input và API responses) đều được render an toàn vào DOM.
  - *Ranh giới Tin cậy (Trust Boundary):*
    - Tuyệt đối cấm sử dụng `eval()`, `document.write()`, hoặc tạo động thẻ `<script>` từ dữ liệu bên ngoài.
    - Dữ liệu động từ API (chữ Hán, nghĩa tiếng Việt, câu ví dụ, ghi chú cá nhân, lý do từ chối kiểm duyệt) bắt buộc phải render qua `textContent` hoặc các API DOM an toàn (`document.createElement`, `classList`, `setAttribute`).
    - *Quy tắc innerHTML:* Không cấm `innerHTML` một cách cứng nhắc cực đoan; các đoạn mã HTML tĩnh thuần túy (e.g. icon SVG cố định, khung spinner template tĩnh nội bộ) được phép sử dụng khi ranh giới tin cậy được xác định rõ ràng là 100% tĩnh không chứa biến nội suy từ bên ngoài.
  - *depends_on:* Task 9G.1.
  - *Deliverables:* `frontend/js/ui/security.js` (DOM sanitization & safe rendering helpers), báo cáo kiểm toán DOM XSS.
  - *Acceptance Criteria:* Thử nghiệm inject các chuỗi payload XSS kinh điển (`<script>alert(1)</script>`, `<img src=x onerror=alert(1)>`, `javascript:alert(1)`) vào form ghi chú, form tạo bài học, ô tìm kiếm từ vựng và lý do kiểm duyệt; 100% các payload được render dưới dạng văn bản thuần túy (plain text), không có script nào được thực thi.
* **Task 9G.3: Resource Graph Audit & Content Security Policy (CSP) Construction**
  - *Mục tiêu:* Kiểm toán đồ thị tài nguyên thực tế của ứng dụng (CDN scripts, styles, web fonts, audio media) sau khi hoàn thành toàn bộ các màn hình giao diện, thiết lập cấu hình Content-Security-Policy (CSP) chặt chẽ và chuẩn bị cấu hình tích hợp vào Spring Security / Nginx.
  - *Ranh giới & Ngoại lệ Backend Seal:* Việc điều chỉnh/cấu hình Content-Security-Policy và các HTTP response security headers liên quan (tại `SecurityConfig.java`, `application.yml` hoặc Nginx reverse-proxy) là một ngoại lệ có kế hoạch được phê duyệt trước theo DEC-40 và DEC-41, không vi phạm trạng thái Backend Seal.
  - *depends_on:* Task 9G.2.
  - *Deliverables:* Cấu hình CSP định nghĩa trong `SecurityConfig.java` / `application.yml` (hoặc Nginx config), tài liệu tham chiếu đồ thị tài nguyên.
  - *Acceptance Criteria:* CSP được xây dựng dựa trên đồ thị tài nguyên thực tế: `default-src 'self'; script-src 'self' https://cdn.jsdelivr.net; style-src 'self' https://cdn.jsdelivr.net; font-src 'self' https://cdn.jsdelivr.net; img-src 'self' data:; connect-src 'self'; media-src 'self' blob:; object-src 'none'; frame-ancestors 'none'; base-uri 'self'; form-action 'self';`; không dùng `unsafe-inline` cho script; tải ứng dụng trên trình duyệt kiểm tra Console không có cảnh báo vi phạm CSP.
* **Task 9G.4: Accessibility Audit (WCAG 2.2 Alignment)**
  - *Mục tiêu:* Kiểm toán khả năng tiếp cận toàn diện theo các tiêu chuẩn W3C WCAG 2.2 cấp độ AA.
  - *Hạng mục Kiểm tra:*
    - *Semantic Landmarks:* Cấu trúc trang sử dụng đầy đủ các thẻ ngữ nghĩa HTML5 (`<header>`, `<nav>`, `<main>`, `<section>`, `<footer>`).
    - *Headings Hierarchy:* Phân cấp tiêu đề rõ ràng từ `<h1>` đến `<h6>`, không nhảy cóc cấp độ.
    - *Accessible Forms & Auth (3.3.8):* Mọi ô nhập liệu có nhãn `<label>` liên kết; hỗ trợ copy-paste mật khẩu; lỗi form liên kết qua `aria-invalid` và `aria-describedby`.
    - *Focus Management & Focus Not Obscured (2.4.11):* Focus indicator viền rõ ràng; khi mở modal, focus được giữ bên trong modal (focus trap) và trả lại nút kích hoạt khi đóng modal; thanh header cố định không che khuất phần tử đang nhận focus.
    - *Target Size Minimum (2.5.8):* Mọi nút bấm, liên kết và phần tử tương tác đạt kích thước tối thiểu $\rightarrow$\ge 24\times 24\text{px}$\rightarrow$ (khuyến nghị $\rightarrow$\ge 44\times 44\text{px}$\rightarrow$ cho thiết bị cảm ứng).
    - *Keyboard Navigation:* 100% tính năng (tra cứu, mở modal, lật thẻ Flashcard, đánh giá SRS, nộp bài) thực hiện được hoàn toàn bằng bàn phím.
  - *depends_on:* Task 9G.3.
  - *Deliverables:* Báo cáo kiểm toán khả năng tiếp cận WCAG 2.2 bằng Chrome DevTools / Lighthouse a11y.
  - *Acceptance Criteria:*
    - [Objective] Điểm số kiểm tra tự động Lighthouse Accessibility đạt $\rightarrow$\ge 90$\rightarrow$; không có vi phạm nghiêm trọng (zero critical axe-core violations).
    - [Manual] Điều hướng toàn bộ ứng dụng bằng phím Tab/Enter/Space/Escape trơn tru, không có bẫy phím (keyboard trap); các modal khóa focus bên trong (focus trap) và hoàn trả focus về nút kích hoạt khi đóng; các phần tử tương tác hiển thị rõ ràng đường viền focus indicator (`:focus-visible`); độ tương phản màu sắc đáp ứng chuẩn WCAG 2.2 AA ($\rightarrow$\ge 4.5:1$\rightarrow$ cho text thường, $\rightarrow$\ge 3:1$\rightarrow$ cho large text).
* **Task 9G.5: Responsive & Multi-Viewport Layout Verification**
  - *Mục tiêu:* Kiểm tra giao diện và tính tương thích bố cục trên các kích thước màn hình phổ biến: Mobile (375px, 414px), Tablet (768px), và Desktop (1200px, 1440px).
  - *depends_on:* Task 9G.4.
  - *Deliverables:* Báo cáo kiểm chứng bố cục đa màn hình.
  - *Acceptance Criteria:* Không có lỗi tràn ngang (horizontal scrollbar ngoài ý muốn); bảng dữ liệu admin dense tables có thanh cuộn ngang nội bộ hoặc co giãn hợp lý; thẻ Flashcard hiển thị đẹp mắt và dễ thao tác chạm trên mobile; bảng xem trước import Excel hiển thị rõ ràng trên màn hình tablet trở lên.
* **Checkpoint Phase 9 (Frontend Seal Gate):** Toàn bộ giao diện cho 4 vai trò (Learner, Creator, Moderator, Admin) đã hoàn thành 100%, kết nối trơn tru với 48 REST endpoints backend (49 HTTP method+path mappings), đạt chuẩn bảo mật DOM XSS & CSP, đạt chuẩn tiếp cận WCAG 2.2 AA, responsive trên mọi kích thước, sẵn sàng bước vào Phase 10.

---

### Phase 10 — Post-Frontend Security, Quality, Performance & E2E Hardening
- **Mục tiêu:** Thực hiện kiểm toán toàn diện toàn bộ hệ sinh thái Full-Stack (Frontend + Backend + Database) sau khi tích hợp giao diện; đánh giá an ninh chuyên sâu theo các chuẩn kiểm soát OWASP API Security Top 10 và ASVS 5.0.0 được chọn lọc; đo lường hiệu năng client và query CSDL; thiết lập bộ kiểm thử tự động hóa đầu cuối E2E bằng Playwright cho toàn bộ các hành trình người dùng và thực hiện hồi quy nghiệm thu toàn diện.
- **Ranh giới:** Đây là giai đoạn **kiểm chứng chất lượng toàn diện của hệ thống tích hợp (System & Integration Hardening)**. Các khuyết tật backend cũ đã được khắc phục tại chuỗi R3.x; các nhiệm vụ ở đây tập trung vào **kiểm thử hồi quy (Regression Testing)**, tuyệt đối không tạo lại các task làm lại backend cũ.
- **Trạng thái:** **`NOT_STARTED`**.

#### Module 10A: Security Regression (OWASP / ASVS-Aligned)
* **Task 10A.1: OWASP API Security Applicability Review & Injection Boundary Regression**
  - *Mục tiêu:* Đánh giá toàn diện các hạng mục trong OWASP API Security Top 10 (2023) đối chiếu với các biểu mẫu giao diện người dùng và các ranh giới dịch vụ backend thực tế.
  - *Hạng mục Kiểm tra Đối kháng:*
    - *API1:2023 BOLA (Broken Object Level Authorization):* Thử nghiệm can thiệp ID bài học của Creator khác, ID ghi chú cá nhân của User khác qua UI fetch calls $\rightarrow$\rightarrow$\rightarrow$ bắt buộc nhận 403 Forbidden hoặc 404 Not Found.
    - *API2:2023 Broken Authentication:* Kiểm tra ranh giới đăng nhập, token tampering, missing signature, thiếu `iss` claim $\rightarrow$\rightarrow$\rightarrow$ nhận 401 Unauthorized.
    - *API3:2023 BOPLA (Broken Object Property Level Authorization):* Gửi các trường dư thừa hoặc nhạy cảm trong body request (`role`, `status`, `authorization_version`, `version`) $\rightarrow$\rightarrow$\rightarrow$ DTO bắt đúng `@JsonProperty(access = READ_ONLY)` hoặc bỏ qua, không gây đột biến ngoài ý muốn.
    - *API4:2023 Unrestricted Resource Consumption:* Gửi tham số phân trang âm, `size = 10000` trên danh mục và ghi chú cá nhân $\rightarrow$\rightarrow$\rightarrow$ bị chặn 400 Bad Request hoặc tự động ép về mức trần `max = 100`.
    - *API5:2023 BFLA (Broken Function Level Authorization):* Learner cố gọi endpoint Admin/Moderator/Creator $\rightarrow$\rightarrow$\rightarrow$ nhận 403 Forbidden.
    - *API7:2023 Security Misconfiguration & Injection:* Kiểm tra phòng chống hiệu quả SQL Injection trên các trường tìm kiếm Pinyin/Hán tự; kiểm tra Path Traversal trên tên file Excel upload.
  - *depends_on:* Checkpoint Phase 9.
  - *Acceptance Criteria:* Báo cáo rà soát khẳng định 100% các ranh giới bảo mật API hoạt động vững chắc; không phát hiện lỗ hổng mức High hoặc Critical nào.
* **Task 10A.2: Authentication, Session & Token Invalidation Regression**
  - *Mục tiêu:* Kiểm chứng thực tế trên trình duyệt về vòng đời token JWT và cơ chế thu hồi tức thì:
    - Token hết hạn tự động chuyển hướng đăng nhập kèm thông báo.
    - Tài khoản bị Admin chuyển sang trạng thái `Inactive` hoặc `Banned` $\rightarrow$\rightarrow$\rightarrow$ request kế tiếp nhận ngay HTTP 401 Unauthorized và bị đăng xuất ngay lập tức (xác minh kiểm tra trạng thái CSDL tại filter).
    - Tài khoản được phân quyền lại vai trò $\rightarrow$\rightarrow$\rightarrow$ token cũ bị từ chối 401 do lệch `authorization_version` (DEC-42), người dùng buộc phải đăng nhập lại để nhận token chứa vai trò mới.
    - Kiểm thử hành vi đa tab (Multi-tab): Đăng xuất ở Tab 1, Tab 2 thực hiện thao tác tiếp theo tự động phát hiện hết phiên và chuyển hướng an toàn.
  - *depends_on:* Task 10A.1.
  - *Acceptance Criteria:* Mở 2 tab trình duyệt: Tab 1 Admin đổi trạng thái tài khoản User A thành Inactive; Tab 2 User A gửi thao tác tạo ghi chú ngay lập tức nhận HTTP 401 Unauthorized và tự động chuyển về trang login.
* **Task 10A.3: Adversarial File Upload & Import Hardening Regression**
  - *Mục tiêu:* Kiểm thử đối kháng giao diện upload Excel bằng các kịch bản file độc hại:
    - File giả mạo định dạng: file binary `.exe` hoặc script đổi tên thành `.xlsx`.
    - File zip nén trùng tên entry (xác minh khả năng chống lỗ hổng Apache POI CVE-2025-31672 regression).
    - File có dung lượng vượt ngưỡng giới hạn (> 10MB).
    - File có số dòng vượt ngưỡng cho phép (> 5000 dòng).
    - File chứa công thức macro độc hại (`=cmd|' /C calc'!A0`).
  - *depends_on:* Task 10A.1.
  - *Acceptance Criteria:* Server từ chối an toàn với mã lỗi HTTP 400 Bad Request kèm thông điệp rõ ràng; không xảy ra hiện tượng tràn bộ nhớ (OutOfMemoryError), không làm nghẽn CPU, và không lưu file tạm độc hại vào hệ thống đĩa.
* **Task 10A.4: Error Normalization & Information Leakage Audit**
  - *Mục tiêu:* Kiểm tra toàn bộ các kịch bản mã lỗi HTTP (400, 401, 403, 404, 409, 413, 422, 429, 500) qua Network tab; xác nhận phản hồi không rò rỉ stack trace Java, câu lệnh SQL Hibernate, đường dẫn file hệ thống hay tên package nội bộ ra phía client.
  - *depends_on:* Task 10A.1.
  - *Acceptance Criteria:* 100% phản hồi lỗi tuân thủ đúng cấu trúc chuẩn `{code, message, data, errors}`, thông điệp bằng tiếng Việt thân thiện, mã HTTP khớp chuẩn REST.
* **Checkpoint 10A:** Báo cáo kiểm toán an ninh Full-Stack xác nhận hệ thống an toàn trước các lỗ hổng OWASP Top 10 và các chuẩn kiểm soát ASVS 5.0.0 được chọn lọc.

#### Module 10B: Frontend Quality, State Consistency & Network Resilience
* **Task 10B.1: Three-State UI Consistency Audit**
  - *Mục tiêu:* Rà soát từng màn hình của toàn bộ 4 vai trò để bảo đảm tính nhất quán tuyệt đối của 3 trạng thái giao diện:
    1. *Loading State:* Hiển thị spinner hoặc skeleton loading khi API đang phản hồi, nút bấm bị vô hiệu hóa để chống double submit.
    2. *Empty State:* Hiển thị hình minh họa tinh tế kèm thông điệp hướng dẫn khi danh sách trống (e.g. chưa có bài học nào, không có thẻ đến hạn hôm nay, không tìm thấy từ vựng).
    3. *Error State:* Hiển thị thông báo lỗi rõ ràng kèm nút "Thử lại" (Retry) khi API gặp sự cố.
  - *depends_on:* Checkpoint Phase 9.
  - *Acceptance Criteria:* Không màn hình nào bị đơ (frozen) hoặc để lại bảng trắng khi có độ trễ mạng hoặc khi không có dữ liệu.
* **Task 10B.2: Network Failure, Timeout & Degraded Connectivity Handling**
  - *Mục tiêu:* Thử nghiệm ứng dụng dưới các điều kiện mạng thực tế khắc nghiệt: ngắt kết nối mạng đột ngột (Offline), độ trễ cao (Slow 3G), và lỗi phản hồi 429 Too Many Requests từ rate limiter.
  - *Lưu ý Kỹ thuật Quan trọng:* Tuyệt đối không suy diễn sai lầm rằng việc mạng client bị rớt mạng đồng nghĩa với việc giao dịch backend đã rollback. Ranh giới mạng client và ranh giới giao dịch CSDL là hai phạm trù độc lập; giao diện client phải thông báo trạng thái mạng không chắc chắn và hỗ trợ kiểm tra lại an toàn.
  - *depends_on:* Task 10B.1.
  - *Acceptance Criteria:* Ứng dụng hiển thị thông báo offline/thử lại lịch sự; khi gặp HTTP 429 thông báo người dùng thời gian cần chờ trước khi thử lại; không phát sinh uncaught promise rejection trên console.
* **Task 10B.3: Cross-Browser & Multi-Viewport Layout Verification**
  - *Mục tiêu:* Kiểm tra giao diện và tính năng trên ma trận trình duyệt được lựa chọn hợp lý: Chromium, Firefox, WebKit và các kích thước màn hình phổ biến.
  - *depends_on:* Task 10B.1.
  - *Acceptance Criteria:* Không vỡ layout, không tràn ngang, các nút bấm và vùng chạm thuận tiện trên thiết bị cảm ứng, tính năng lật thẻ Flashcard hoạt động mượt mà trên cả 3 engine trình duyệt.
* **Checkpoint 10B:** Giao diện nhất quán, thích ứng tốt trên các trình duyệt và thiết bị di động, xử lý mượt mà sự cố mạng.

#### Module 10C: Backend Integration Regression & Contract Verification
* **Task 10C.1: Backend Integration Master Test Suite Regression**
  - *Mục tiêu:* Chạy lại toàn bộ bộ kiểm thử tự động của backend (`mvn clean test`) trên môi trường Testcontainers MySQL 8.4 độc lập.
  - *depends_on:* Checkpoint Phase 9.
  - *Acceptance Criteria:* Toàn bộ **1,101/1,101 tests PASS** (0 failures, 0 errors, 0 skipped), chứng minh các hoạt động tích hợp frontend không làm phát sinh bất kỳ hồi quy nào trên backend.
* **Task 10C.2: Transaction Rollback & Data Integrity Under Real API Failure Conditions**
  - *Mục tiêu:* Kiểm tra tính toàn vẹn của giao thức transaction rollback đa bảng khi xảy ra các lỗi nghiệp vụ thực sự tại tầng dịch vụ (vi phạm ràng buộc dữ liệu, xung đột trùng lặp, ngoại lệ giữa chừng khi import confirm hoặc duyệt bài).
  - *Phân biệt Rạch ròi:* Client disconnect hoặc timeout chỉ thể hiện sự gián đoạn truyền tin, không phải bằng chứng chứng minh giao dịch CSDL đã rollback. Việc kiểm chứng rollback phải dựa trên việc kiểm tra trực tiếp trạng thái CSDL thực tế sau khi ném ngoại lệ có chủ đích.
  - *depends_on:* Task 10C.1.
  - *Acceptance Criteria:* Khi xảy ra ngoại lệ giữa chừng trong giao dịch `@Transactional`, CSDL hoàn nguyên 100%, không để lại bất kỳ bản ghi mồ côi nào trong các bảng `LESSON`, `LESSON_VOCABULARY`, `CARD_PROGRESS`, `REVIEW_LOG`, `MODERATION_LOG`.
* **Task 10C.3: API Contract & Documentation Synchronization Regression**
  - *Mục tiêu:* So sánh đối chiếu trực tiếp giữa Controller DTOs, file tài liệu `API.md`, HTTP client `frontend/js/api/api.js`, và phản hồi thực tế từ backend.
  - *depends_on:* Task 10C.1.
  - *Acceptance Criteria:* Khớp 100% tên trường, kiểu dữ liệu, mã lỗi và cấu trúc phong bì; không có sự sai lệch nào giữa tài liệu đặc tả và mã nguồn thực thi.
* **Checkpoint 10C:** Backend và cơ sở dữ liệu giữ vững 100% tính toàn vẹn, hoàn thành kiểm thử hồi quy không lỗi.

#### Module 10D: Performance & Resource Behavior Verification
* **Task 10D.1: Client-Side Performance Baseline (Internal Performance Targets)**
  - *Mục tiêu:* Đo đạc các chỉ số hiệu năng phía client bằng Chrome DevTools / Lighthouse: Largest Contentful Paint (LCP), Cumulative Layout Shift (CLS), tổng dung lượng tài nguyên tải trang, và thời gian thực thi JavaScript.
  - *Nguyên tắc Vận hành:* Các chỉ số này được coi là **MỤC TIÊU HIỆU NĂNG NỘI BỘ (Internal Performance Targets)** để định hướng tối ưu hóa (e.g. LCP $\rightarrow$< 2.5\text{s}$\rightarrow$, CLS $\rightarrow$< 0.1$\rightarrow$, tổng dung lượng tài nguyên tải lần đầu $\rightarrow$< 1\text{MB}$\rightarrow$ khi nén gzip), không được coi là điều kiện tiên quyết mang tính lỗi sống còn (defect blocker) nếu chỉ chênh lệch nhỏ do biến thiên mạng/máy trạm.
  - *depends_on:* Module 10B.
  - *Acceptance Criteria:* Báo cáo đo lường Lighthouse hiệu năng ghi nhận kết quả chi tiết; tài nguyên tĩnh được nén và cache hợp lý; không có vòng lặp render thừa gây nghẽn CPU.
* **Task 10D.2: Database Query Plan Regression (`EXPLAIN` / `EXPLAIN ANALYZE`)**
  - *Mục tiêu:* Kiểm chứng kế hoạch thực thi câu lệnh SQL với tập dữ liệu mẫu lớn; đánh giá chỉ mục, số dòng quét (rows examined), chiến lược join, và các thao tác sắp xếp.
  - *Nguyên tắc Vận hành:* Thao tác `filesort` trong MySQL **KHÔNG TỰ ĐỘNG BỊ COI LÀ BUG**. Sắp xếp trong bộ nhớ (in-memory filesort) trên tập dữ liệu nhỏ đã được lọc là hành vi tối ưu bình thường của MySQL optimizer. Việc kiểm toán tập trung vào việc loại bỏ full table scan ngoài ý muốn trên các bảng lớn (`CARD_PROGRESS`, `VOCABULARY`) và bảo đảm các truy vấn trọng yếu đạt thời gian phản hồi trung bình $\rightarrow$< 100\text{ms}$\rightarrow$.
  - *depends_on:* Module 10C.
  - *Acceptance Criteria:* Kế hoạch thực thi `EXPLAIN` trên các truy vấn due cards và từ vựng xác nhận sử dụng đúng chỉ mục; không có quét toàn bảng bất thường.
* **Task 10D.3: Large Dataset & Resource Lifecycle Behavior**
  - *Mục tiêu:* Kiểm tra ứng dụng khi làm việc với tập dữ liệu lớn: phân trang danh mục từ vựng, nạp file Excel 5000 dòng, kích thước phản hồi API, và dọn dẹp bộ nhớ client (giải phóng event listeners, dọn dẹp timer khi chuyển trang).
  - *depends_on:* Task 10D.1, Task 10D.2.
  - *Acceptance Criteria:* Ứng dụng không bị rò rỉ bộ nhớ (memory leak) khi chuyển qua lại giữa các trang; bảng dữ liệu lớn phân trang mượt mà; file Excel 5000 dòng được xử lý trong giới hạn bộ nhớ cho phép.
* **Checkpoint 10D:** Hiệu năng đạt tiêu chuẩn trải nghiệm người dùng mượt mà và tối ưu hóa tài nguyên server.

#### Module 10E: Dedicated End-to-End (E2E) Browser Automation (Playwright)
* **Task 10E.1: Playwright Infrastructure & Test Fixture Setup**
  - *Mục tiêu:* Khởi tạo framework kiểm thử tự động hóa trình duyệt Playwright với cấu hình chạy headless trên CI và môi trường cục bộ; lựa chọn ma trận trình duyệt hợp lý (Chromium Desktop + Chromium Mobile viewport làm nền tảng chính; kiểm chứng chéo Firefox/WebKit); quản lý fixture tài khoản kiểm thử cho 4 vai trò.
  - *depends_on:* Checkpoint 10A, Checkpoint 10B.
  - *Deliverables:* `e2e/playwright.config.js`, `e2e/fixtures/`, `e2e/package.json`.
  - *Acceptance Criteria:* Playwright khởi chạy thành công, kết nối với frontend local và backend test server, tự động chụp screenshot/trace khi có assertion thất bại.
* **Task 10E.2: E2E Journey 1 — Learner Complete Learning Flow**
  - *Kịch bản:* Đăng ký tài khoản mới $\rightarrow$\rightarrow$\rightarrow$ Đăng nhập $\rightarrow$\rightarrow$\rightarrow$ Xem hồ sơ cá nhân $\rightarrow$\rightarrow$\rightarrow$ Khám phá danh mục bộ thủ $\rightarrow$\rightarrow$\rightarrow$ Tìm kiếm từ vựng bằng Pinyin $\rightarrow$\rightarrow$\rightarrow$ Mở bài học $\rightarrow$\rightarrow$\rightarrow$ Tạo ghi chú cá nhân $\rightarrow$\rightarrow$\rightarrow$ Vào phòng ôn tập Flashcard SRS $\rightarrow$\rightarrow$\rightarrow$ Lật thẻ $\rightarrow$\rightarrow$\rightarrow$ Đánh giá 4 mức rating $\rightarrow$\rightarrow$\rightarrow$ Kiểm tra số liệu cập nhật trên Study Stats Dashboard.
  - *depends_on:* Task 10E.1.
  - *Acceptance Criteria:* Toàn bộ hành trình chạy tự động 100% PASS từ đầu đến cuối, kiểm tra chính xác các assertion trên DOM và dữ liệu hiển thị.
* **Task 10E.3: E2E Journey 2 — Content Creation & Moderation Lifecycle Flow**
  - *Kịch bản:* Creator đăng nhập $\rightarrow$\rightarrow$\rightarrow$ Tạo bài học nháp $\rightarrow$\rightarrow$\rightarrow$ Tải file Excel preview $\rightarrow$\rightarrow$\rightarrow$ Xác nhận import $\rightarrow$\rightarrow$\rightarrow$ Đổi thứ tự từ vựng $\rightarrow$\rightarrow$\rightarrow$ Nộp bài duyệt $\rightarrow$\rightarrow$\rightarrow$ Đăng xuất $\rightarrow$\rightarrow$\rightarrow$ Moderator đăng nhập $\rightarrow$\rightarrow$\rightarrow$ Mở hàng đợi Pending $\rightarrow$\rightarrow$\rightarrow$ Kiểm tra bài $\rightarrow$\rightarrow$\rightarrow$ Từ chối kèm lý do $\rightarrow$\rightarrow$\rightarrow$ Creator sửa và nộp lại $\rightarrow$\rightarrow$\rightarrow$ Moderator phê duyệt $\rightarrow$\rightarrow$\rightarrow$ Learner đăng nhập thấy bài học mới xuất hiện trong danh sách học.
  - *depends_on:* Task 10E.1.
  - *Acceptance Criteria:* Xác nhận trạng thái bài học chuyển dịch chính xác qua từng bước (`Draft` $\rightarrow$\rightarrow$\rightarrow$ `Pending` $\rightarrow$\rightarrow$\rightarrow$ `Rejected` $\rightarrow$\rightarrow$\rightarrow$ `Pending` $\rightarrow$\rightarrow$\rightarrow$ `Approved`).
* **Task 10E.4: E2E Journey 3 — System Administration Flow**
  - *Kịch bản:* Admin đăng nhập $\rightarrow$\rightarrow$\rightarrow$ Tìm kiếm tài khoản $\rightarrow$\rightarrow$\rightarrow$ Vô hiệu hóa tài khoản $\rightarrow$\rightarrow$\rightarrow$ Phân quyền vai trò Creator/Moderator $\rightarrow$\rightarrow$\rightarrow$ Thêm từ vựng vào danh mục catalog $\rightarrow$\rightarrow$\rightarrow$ Giám sát bài học toàn hệ thống.
  - *depends_on:* Task 10E.1.
  - *Acceptance Criteria:* Thao tác quản trị của Admin thực thi thành công và phản ánh tức thì trên toàn bộ hệ thống.
* **Task 10E.5: E2E Journey 4 — Security Boundaries & Error Handling Flow**
  - *Kịch bản:* Token hết hạn tự động chuyển hướng 401 $\rightarrow$\rightarrow$\rightarrow$ Truy cập trái quyền nhận 403 Forbidden $\rightarrow$\rightarrow$\rightarrow$ Thao tác can thiệp sửa ghi chú người khác bị chặn $\rightarrow$\rightarrow$\rightarrow$ Gửi đăng nhập quá tần suất nhận 429 Too Many Requests kèm thông báo chờ đợi.
  - *depends_on:* Task 10E.1.
  - *Acceptance Criteria:* Xác minh thành công các ranh giới bảo mật cấp giao diện và phản hồi lỗi từ server.
* **Checkpoint 10E:** Toàn bộ 4 kịch bản E2E Playwright chạy tự động vượt qua 100% trên trình duyệt thực tế.

#### Module 10F: Full-Stack Acceptance Regression
* **Task 10F.1: Cross-Role Regression Testing**
  - *Mục tiêu:* Kiểm chứng sự tương tác đa vai trò không bị xung đột phiên, quyền hạn chuyển đổi nhịp nhàng giữa Learner, Creator, Moderator và Admin.
  - *depends_on:* Module 10E.
  - *Acceptance Criteria:* Không có hiện tượng rò rỉ quyền hạn chéo; vai trò nào thực thi đúng phạm vi vai trò đó.
* **Task 10F.2: Cross-Browser Regression Testing**
  - *Mục tiêu:* Chạy bộ test hồi quy E2E trên ma trận trình duyệt đã chọn (Chromium, Firefox, WebKit).
  - *depends_on:* Module 10E.
  - *Acceptance Criteria:* Toàn bộ các luồng nghiệp vụ cốt lõi hoạt động ổn định trên cả 3 engine trình duyệt.
* **Task 10F.3: Security Regression Consolidation**
  - *Mục tiêu:* Tổng hợp và đối chiếu toàn bộ kết quả kiểm toán an ninh Full-Stack, khẳng định không phát hiện khuyết tật bảo mật mức High/Critical nào chưa được xử lý trong phạm vi kiểm thử.
  - *depends_on:* Task 10A.1..10A.4, Task 10E.5.
  - *Acceptance Criteria:* Báo cáo an ninh xác nhận hệ thống phòng chống hiệu quả các nguy cơ OWASP Top 10 trong phạm vi kiểm thử, đối chiếu tuân thủ các chuẩn kiểm soát ASVS 5.0.0 được lựa chọn.
* **Task 10F.4: Data Integrity & Persistence Regression**
  - *Mục tiêu:* Kiểm tra cơ sở dữ liệu MySQL sau toàn bộ quá trình chạy kiểm thử E2E và tích hợp; khẳng định không có dữ liệu rác, không có bản ghi mồ côi trong các bảng quan hệ.
  - *depends_on:* Task 10C.2, Module 10E.
  - *Acceptance Criteria:* Toàn vẹn tham chiếu 100%; số lượng bản ghi nhất quán tuyệt đối.
* **Task 10F.5: Documentation & API Consistency Regression**
  - *Mục tiêu:* Rà soát đối chiếu toàn bộ tài liệu dự án để đảm bảo tính nhất quán trước khi chuyển sang giai đoạn phát hành.
  - *depends_on:* Toàn bộ các tasks trong Phase 10.
  - *Acceptance Criteria:* Mọi tài liệu phản ánh chính xác kết quả thực tế.
* **Checkpoint Phase 10 (System Hardening & E2E Seal):** Toàn bộ bộ test E2E Playwright chạy tự động vượt qua 100% các kịch bản hành trình người dùng; kiểm toán an ninh, hiệu năng và tính toàn vẹn Full-Stack hoàn tất xuất sắc.

---

### Phase 11 — Final Integration, Release Validation, Packaging & Delivery
- **Mục tiêu:** Đóng gói ứng dụng thành file thực thi JAR độc lập, xác minh khả năng tái tạo 100% từ môi trường sạch (Clean Environment Reproducibility), cấu hình hạ tầng vận hành chính thức (Standalone JAR + Nginx), xuất bản bộ hiện vật bàn giao đầy đủ (Postman Collection, Runbook, Deployment Guide), và thực hiện chốt chặn nghiệm thu dự án cuối cùng.
- **Ranh giới:** Giai đoạn này tập trung vào kỹ thuật phát hành (Release Engineering), kiểm chứng khả năng vận hành thực tế và bàn giao chính thức. **Phase 11 là giai đoạn cuối cùng của dự án**; Module 11E đóng vai trò là Cổng Nghiệm thu Tối cao (Final Project Acceptance Gate & Release Sign-Off), không cần tạo thêm Phase 12.
- **Trạng thái:** **`NOT_STARTED`**.

#### Module 11A: Release Candidate Validation
* **Task 11A.1: Clean Backend Build & Standalone JAR Compilation**
  - *Mục tiêu:* Thực thi lệnh build hoàn chỉnh `mvn clean package` trên cây mã nguồn sạch; kiểm tra artifact sinh ra `target/elearning-backend-1.0.0.jar`.
  - *depends_on:* Checkpoint Phase 10.
  - *Acceptance Criteria:* Build thành công với kết quả kiểm thử `0 failures, 0 errors, 0 skipped`; kích thước JAR hợp lý; không chứa file rác hoặc class thừa.
* **Task 11A.2: Clean Database Migration & Seed Verification (V1 $\rightarrow$\rightarrow$\rightarrow$ V7)**
  - *Mục tiêu:* Chạy Flyway migrations V1 $\rightarrow$\rightarrow$\rightarrow$ V7 từ đầu trên một CSDL MySQL 8.4 hoàn toàn mới và rỗng.
  - *depends_on:* Task 11A.1.
  - *Acceptance Criteria:* Toàn bộ 7 migration scripts áp dụng thành công theo đúng thứ tự; schema nhất quán 100% với đặc tả 14 bảng; 214 bộ thủ Khang Hy có đầy đủ Pinyin chuẩn xác (V7); zero migration errors.
* **Task 11A.3: Configuration & Secrets Security Audit**
  - *Mục tiêu:* Rà soát toàn bộ cấu hình `application.yml` và mã nguồn để khẳng định không có mật khẩu, secret key JWT hoặc thông tin nhạy cảm nào bị hardcode.
  - *depends_on:* Task 11A.1.
  - *Acceptance Criteria:* 100% các thông số môi trường sản xuất sử dụng biến môi trường hệ thống (`$\rightarrow${DB_USERNAME}`, `$\rightarrow${DB_PASSWORD}`, `$\rightarrow${JWT_SECRET}`); log level sản xuất được cấu hình an toàn không ghi log dữ liệu nhạy cảm.
* **Checkpoint 11A:** Bản dựng Release Candidate được kiểm chứng thành công, CSDL sạch migrate hoàn hảo, cấu hình an toàn không rò rỉ bí mật.

#### Module 11B: Production-Like Topology & Configuration Verification
* **Task 11B.1: Production Profile Startup & Health Probe Verification**
  - *Mục tiêu:* Khởi động ứng dụng với profile production (`SPRING_PROFILES_ACTIVE=prod`); kiểm tra endpoint `/actuator/health`.
  - *depends_on:* Module 11A.
  - *Acceptance Criteria:* Ứng dụng khởi động thành công trong vòng $\rightarrow$< 15\text{s}$\rightarrow$; endpoint `/actuator/health` trả về `{"status": "UP"}`; chi tiết nội bộ (`show-details: never`) được ẩn hoàn toàn theo cấu hình an toàn.
* **Task 11B.2: Reverse-Proxy Forwarding & Production Security Headers Simulation**
  - *Mục tiêu:* Kiểm chứng ứng dụng đứng sau cấu hình giả lập Reverse Proxy (Nginx) thông qua header `X-Forwarded-*` và cấu hình `server.forward-headers-strategy: framework`.
  - *Ghi chú Ranh giới Thực tế (Reality Boundary):* Nếu không có sẵn hạ tầng Cloudflare/Cloud live thực tế, ghi nhận rõ ràng và trung thực: **`NOT VERIFIED IN LIVE CLOUD INFRASTRUCTURE / VERIFIED IN LOCAL PROXY SIMULATION`**. Tuyệt đối không đánh đồng việc giả lập cục bộ với hạ tầng đám mây thực tế.
  - *depends_on:* Task 11B.1.
  - *Acceptance Criteria:* Ứng dụng nhận diện đúng client IP thực tế cho rate limiter; header HSTS được gửi chuẩn xác trên kết nối an toàn HTTPS (RFC 6797); các header `nosniff`, `DENY`, `Referrer-Policy` hiện diện đầy đủ.
* **Task 11B.3: Production CORS & Origin Verification**
  - *Mục tiêu:* Xác minh cấu hình CORS với các domain được cấp phép trong `$\rightarrow${APP_CORS_ALLOWED_ORIGINS}`.
  - *depends_on:* Task 11B.1.
  - *Acceptance Criteria:* Request từ origin hợp lệ được chấp thuận kèm header CORS tương ứng; request từ origin lạ bị từ chối an toàn không có header `Access-Control-Allow-Origin`.
* **Checkpoint 11B:** Ứng dụng vận hành ổn định trong mô hình topology sản xuất giả lập, đáp ứng đầy đủ tiêu chuẩn kết nối mạng và header bảo mật.

#### Module 11C: Primary Deployment Topology & Packaging
* **Task 11C.1: Primary Deployment Topology Setup (Standalone JAR + Nginx)**
  - *Mục tiêu:* Thiết lập mô hình triển khai chính thức được hỗ trợ (Primary Supported Deployment Topology): **Phương án B: Standalone Spring Boot JAR kết hợp Nginx Reverse Proxy** phục vụ tài nguyên tĩnh Frontend và proxy các request `/api/` tới Backend (Docker Compose được duy trì như một phương án thay thế được tài liệu hóa).
  - *depends_on:* Module 11B.
  - *Deliverables:* File cấu hình Nginx mẫu `deploy/nginx/elearning.conf`, service unit mẫu systemd `deploy/systemd/elearning.service`.
  - *Acceptance Criteria:* Cấu hình Nginx định tuyến chính xác: các file tĩnh `frontend/` được phục vụ trực tiếp với caching headers phù hợp; các request `/api/v1/` và `/actuator/health` được proxy an toàn tới `http://127.0.0.1:8080` kèm đầy đủ headers `X-Forwarded-*`.
* **Task 11C.2: Reverse Proxy, HTTPS & TLS Hardening**
  - *Mục tiêu:* Cấu hình chứng chỉ SSL/TLS (Let's Encrypt / self-signed cho staging), giao thức HTTP/2, và gia cố bảo mật web server (tắt server tokens, giới hạn kích thước body request `client_max_body_size 10M`).
  - *depends_on:* Task 11C.1.
  - *Acceptance Criteria:* File cấu hình Nginx hoàn chỉnh sẵn sàng cho TLS 1.2/1.3, mã hóa mạnh, và chuyển hướng tự động HTTP $\rightarrow$\rightarrow$\rightarrow$ HTTPS.
* **Task 11C.3: Health & Readiness Verification Under Primary Topology**
  - *Mục tiêu:* Kiểm tra đường truyền end-to-end từ trình duyệt qua Nginx tới Spring Boot backend.
  - *depends_on:* Task 11C.2.
  - *Acceptance Criteria:* Truy cập domain/cổng Nginx trả về trang chủ giao diện người dùng; gọi API thông suốt; endpoint health probe trả về UP.
* **Task 11C.4: Lifecycle Smoke Test (Startup & Graceful Shutdown)**
  - *Mục tiêu:* Kiểm tra vòng đời vận hành: khởi động sạch, thực thi smoke test nhanh, và tắt ứng dụng duyên dáng (Graceful Shutdown) qua tín hiệu `SIGTERM`.
  - *depends_on:* Task 11C.3.
  - *Acceptance Criteria:* Khi nhận tín hiệu tắt máy, Spring Boot hoàn thành các request đang xử lý dở dang, đóng kết nối HikariCP an toàn, không để lại tiến trình treo (zombie process) trên hệ điều hành.
* **Checkpoint 11C:** Gói sản phẩm hoàn chỉnh, có thể phân phối và khởi chạy độc lập theo đúng mô hình topology đã định hình.

#### Module 11D: Delivery Artifacts & Operational Documentation
* **Task 11D.1: Comprehensive Postman / OpenAPI Collection**
  - *Mục tiêu:* Xuất bản và kiểm chứng bộ Postman Collection hoàn chỉnh bao phủ 100% 49 HTTP method+path mappings (48 business handler methods) REST API backend hiện có; phân nhóm khoa học theo vai trò người dùng (Public, Learner, Creator, Moderator, Admin); tích hợp sẵn biến môi trường (`baseUrl`, `learnerToken`, `creatorToken`, `moderatorToken`, `adminToken`) và test script tự động kiểm tra `code == "SUCCESS"`.
  - *depends_on:* Module 11C.
  - *Deliverables:* `docs/postman_collection.json`, `docs/postman_environment.json`.
  - *Acceptance Criteria:* Chạy toàn bộ collection qua Newman hoặc Postman runner đạt 100% PASS trên tất cả các request.
* **Task 11D.2: Production Deployment Runbook & Environment Setup Guide**
  - *Mục tiêu:* Hoàn thiện tài liệu hướng dẫn triển khai và vận hành hệ thống trong `.agents/RUNBOOK.md` và `docs/DEPLOYMENT_GUIDE.md`: hướng dẫn cài đặt môi trường sạch, cấu hình biến môi trường, khởi tạo CSDL, khởi động service dạng daemon (systemd/JAR), cấu hình Nginx reverse proxy với HTTPS, và quy trình sao lưu/phục hồi dữ liệu.
  - *depends_on:* Module 11C.
  - *Deliverables:* `.agents/RUNBOOK.md`, `docs/DEPLOYMENT_GUIDE.md`.
  - *Acceptance Criteria:* Một kỹ sư mới đọc tài liệu có thể thiết lập và triển khai thành công hệ thống từ đầu mà không cần hỏi thêm tác giả.
* **Task 11D.3: Environment Configuration & Variables Guide**
  - *Mục tiêu:* Soạn thảo tài liệu đặc tả toàn bộ các biến môi trường hệ thống (`docs/ENVIRONMENT.md`).
  - *depends_on:* Task 11D.2.
  - *Deliverables:* `docs/ENVIRONMENT.md`.
  - *Acceptance Criteria:* Khai báo đầy đủ tên biến, giá trị mặc định, mục đích và mức độ nhạy cảm cho mọi tham số cấu hình.
* **Task 11D.4: Master Test & Verification Report**
  - *Mục tiêu:* Tổng hợp toàn bộ bằng chứng kiểm thử của dự án (Unit tests, Integration tests, Concurrency tests, Rollback tests, E2E Playwright tests) vào tài liệu báo cáo kiểm thử tổng thể (`docs/TEST_REPORT.md`).
  - *depends_on:* Module 11C.
  - *Deliverables:* `docs/TEST_REPORT.md`.
  - *Acceptance Criteria:* Báo cáo trình bày minh bạch số lượng tests, thời gian thực thi, độ bao phủ các kịch bản và kết quả đạt được.
* **Task 11D.5: Final Architecture, API & Project State Documentation Synchronization**
  - *Mục tiêu:* Đồng bộ hóa toàn diện toàn bộ tài liệu dự án (`CURRENT_STATE.md`, `API.md`, `ARCHITECTURE.md`, `DATABASE.md`, `DECISIONS.md`, `PROGRESS.md`, `ROADMAP.md`) phản ánh chính xác trạng thái bàn giao thực tế, kết quả kiểm thử cuối cùng và ma trận truy xuất nguồn gốc yêu cầu.
  - *depends_on:* Task 11D.1..11D.4.
  - *Acceptance Criteria:* Toàn bộ tài liệu nhất quán 100% với mã nguồn thực tế; không còn bất kỳ số liệu cũ hay task pending mâu thuẫn.
* **Checkpoint 11D:** Bộ hiện vật bàn giao (Delivery Artifacts) đầy đủ, chính xác, đã được kiểm nghiệm thực tế.

#### Module 11E: Final Project Acceptance Gate & Formal Handover
* **Task 11E.1: Multi-Dimensional Project Acceptance Gate & Release Sign-Off**
  - *Mục tiêu:* Tiến hành phiên nghiệm thu chốt chặn chất lượng tối cao của dự án trước khi bàn giao (Final Quality Gate) dựa trên 7 chiều tiêu chuẩn:
    1. **Nghiệm thu Chức năng (Functional Acceptance):** Đạt 100% yêu cầu nghiệp vụ cho cả 4 vai trò (Học viên, Tác giả, Kiểm duyệt viên, Quản trị viên).
    2. **Nghiệm thu Bảo mật (Security Acceptance):** Hoàn thành rà soát an ninh theo các chuẩn kiểm soát OWASP Top 10 và ASVS 5.0.0 được lựa chọn; zero lỗ hổng nghiêm trọng; bảo vệ phiên, token revocation tức thì, và kiểm soát ranh giới phân quyền không IDOR/BOLA.
    3. **Nghiệm thu Tự động hóa Đầu cuối (E2E Acceptance):** Toàn bộ 4 kịch bản E2E Playwright chạy tự động vượt qua 100% trên trình duyệt thực tế.
    4. **Nghiệm thu Hiệu năng (Performance Acceptance):** Đạt các mục tiêu hiệu năng nội bộ về tốc độ tải trang, Core Web Vitals và thời gian phản hồi truy vấn CSDL.
    5. **Nghiệm thu Đóng gói & Khả năng Tái tạo (Packaging & Reproducibility Acceptance):** Ứng dụng có thể build sạch từ mã nguồn (`mvn clean package`) và khởi chạy thành công trên CSDL trắng mới từ migrations Flyway V1 $\rightarrow$\rightarrow$\rightarrow$ V7.
    6. **Nghiệm thu Tài liệu & Hiện vật (Documentation Acceptance):** Postman collection 100% pass, Runbook rõ ràng, kiến trúc và API đồng bộ hoàn hảo.
    7. **Chấp thuận Giới hạn & Rủi ro Vận hành (Known-Risk Acceptance):** Khai báo minh bạch các giới hạn kiến trúc đã được chấp thuận: Rate limiter in-memory cho single node (cần Redis nếu scale-out ngang đa node), Reverse proxy trust boundary bắt buộc ghi đè header phía ngoài, và ghi nhận rủi ro vòng đời kết thúc OSS support của Spring Boot 3.3.x để lên kế hoạch nâng cấp trong các chu kỳ bảo trì tương lai.
  - *depends_on:* Toàn bộ các module từ 9A đến 11D.
  - *Acceptance Criteria:* Checklist nghiệm thu 7 tiêu chí đạt 100% tick chấp thuận; công bố biên bản bàn giao hoàn tất dự án (Final Release Seal).
* **Checkpoint Phase 11 (Final Project Delivery Seal):** Ứng dụng có thể tái tạo 100% từ môi trường sạch, gói phát hành độc lập hoàn chỉnh, test suite và E2E pass 100%, tài liệu vận hành đầy đủ, dự án hoàn thành xuất sắc toàn bộ mục tiêu đề ra.

