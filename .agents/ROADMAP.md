# ROADMAP — LỘ TRÌNH TRIỂN KHAI DỰ ÁN MỚI TỪ ĐẦU (REBUILT FROM SCRATCH)

> **Mô hình kiến trúc triển khai:** 4 cấp độ kỹ thuật:  
> $$\text{PHASE (Giai đoạn lớn)} \longrightarrow \text{MODULE (Phân hệ kỹ thuật)} \longrightarrow \text{TASK (Lát cắt thực thi)} \longrightarrow \text{VERIFICATION CHECKPOINT (Chốt chặn nghiệm thu)}$$
> 
> **Nguyên tắc kỹ thuật dành cho AI Coding Agent:**  
> - **Scope rõ ràng:** Mỗi Task xác định rõ: Làm gì, Tại sao cần, Trong phạm vi nào (In-Scope), Không làm gì (Out-of-Scope).  
> - **Dependency thực tế:** Phân định chính xác giữa Hard Dependency (bắt buộc tuần tự ở cấp task/module), Parallel (có thể thực thi song song) và Service Dependency (ràng buộc mức nghiệp vụ, không tạo FK database giả).  
> - **Tiêu chí nghiệm thu định lượng:** Đánh giá dựa trên bằng chứng vật lý độc lập (Tests pass, Flyway validated, HTTP status đúng), không suy đoán cảm tính.  
> - **Ngữ cảnh vừa phải (Lean Context):** Một Task là một lát cắt thay đổi có ý nghĩa (Coherent Change), tránh gộp quá nhiều capability gây quá tải ngữ cảnh (context overload).  

---

## 1. TỔNG QUAN LỘ TRÌNH 12 GIAI ĐOẠN (ROADMAP OVERVIEW)

```
Phase 0: Project Specification & Architecture Baseline [COMPLETED]
   │
   ▼
Phase 1: Spring Boot Foundation & Web Infrastructure [IN_PROGRESS: Mod 1A Done, Mod 1B Pending]
   │
   ├────────────────────────────────────────┬───────────────────────────────────────┐
   ▼ (Hard dependency cho Persistence)      ▼ (Parallel với 2A/2B/2E)               ▼ (Parallel)
Phase 2: Persistence Layer & Seed Data      Phase 1 Module 1B: Response Envelope    Phase 2 Module 2E: Seed
(JPA Mappings 2A-2D, Test 2F)               (ApiResponse, GlobalExceptionHandler)   (Roles V2, Radicals V3)
   │                                        │                                       │
   │                                        ├───────────────────────────────────────┤
   ▼                                        ▼                                       ▼
Phase 3: Authentication, Security & RBAC ◄──┴───────────────────────────────────────┘
(Security 6, JWT, Login/Register DTO & Controllers, User Profile)
   │
   ▼
Phase 4: Radical & Vocabulary Catalog Domain (214 Radicals, Vocabulary Search, Admin CRUD)
   │
   ▼
Phase 5: Lesson Management & Excel Import (Public Lessons, Creator Studio, POI 2-Step Import)
   │
   ▼
Phase 6: Content Moderation Workflow (Moderator Queue, Approve/Reject Invariants, Audit Log)
   │
   ▼
Phase 7: Spaced Repetition System (SM-2 Pure Algorithm, Review Session, Study Progress)
   │
   ▼
Phase 8: Personal Notes & User Settings (Notes <= 500 chars, SRS Daily Limits)
   │
   ▼
Phase 9: Frontend UI & Client Integration (Layout, api.js, Learner, SRS, Creator, Moderator UI)
   │
   ▼
Phase 10: Security, Performance & Quality Hardening (10A Security, 10B Performance, 10C Quality Gaps)
   │
   ▼
Phase 11: Final Integration, Release Validation & Delivery (E2E Journeys, JAR Packaging, Delivery)
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
* **Task 1B.1: Base Response Models (`ApiResponse<T>`, `PageResponse<T>`, `ErrorCode` enum)**
  - *Mục đích:* Chuẩn hóa định dạng JSON trả về client theo Mục 1.2 và 1.3 của `API.md`.
  - *Phạm vi:* `com.elearning.dto.response.ApiResponse`, `PageResponse`, `com.elearning.common.ErrorCode`.
  - *Không làm:* Chưa viết Controller hay Service nghiệp vụ.
  - *depends_on:* `Task 1A.4`.
* **Task 1B.2: Global Exception Handler (`GlobalExceptionHandler`)**
  - *Mục đích:* Bắt toàn diện ngoại lệ tại `@RestControllerAdvice` và chuyển thành `ApiResponse` chuẩn.
  - *Phạm vi:* Xử lý `MethodArgumentNotValidException` (400), `BusinessException` (dynamic code/status), fallback `Exception` (500).
  - *depends_on:* `Task 1B.1`.
* **Checkpoint 1B [VERIFIED]:** `ApiResponseTests` và `GlobalExceptionHandlerTests` PASS 24/24 tests; kiểm chứng serialize JSON của `ApiResponse<T>`, `PageResponse<T>`, bắt lỗi validation trả về HTTP 400 kèm `errors[]`, `BusinessException` bảo toàn mã lỗi và HTTP status.
* **Phase 1 Integration Checkpoint [VERIFIED]:** `mvn clean test` PASS 25/25 tests (1 context test, 12 ApiResponse tests, 12 ExceptionHandler tests), ứng dụng nạp context thành công, kết nối MySQL ổn định, cấu trúc phong bì API và cơ chế bắt lỗi sẵn sàng cho các giai đoạn tiếp theo.

---

### Phase 2 — Persistence Layer & Database Seed Data
- **Mục tiêu:** Xây dựng tầng ánh xạ thực thể JPA (JPA Domain Mappings), Spring Data Repositories, và nạp dữ liệu danh mục ban đầu qua Flyway seed data (Roles, Radicals).
- **Ranh giới phụ thuộc:** Phụ thuộc vào `Module 1A` (Flyway V1 schema). **KHÔNG phụ thuộc vào `Module 1B`** (có thể triển khai song song).
- **Trạng thái:** **`IN_PROGRESS`** (Module 2A: `IN_PROGRESS`, Mod 2B-2E: `NOT_STARTED`).

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

#### Module 2D: Learning Progress, Audit & Personalization Persistence Mapping [IN_PROGRESS]
* **Task 2D.1: JPA Entities Cụm Ghi chú & Kiểm toán (`USER_SRS_SETTING`, `PERSONAL_NOTE`, `MODERATION_LOG`) [COMPLETED]**
  - *Phạm vi:* Entity cho `UserSrsSetting` (1:1 với UserProfile), `PersonalNote` (ràng buộc độ dài $\le 500$ ký tự, không giới hạn 5 notes), `ModerationLog` (bất biến, quan hệ RESTRICT với Lesson và Account).
  - *depends_on:* `Task 2A.1` (UserProfile), `Task 2B.1` (Vocabulary), `Task 2C.1` (Lesson).
* **Task 2D.2: JPA Entities Cụm SRS với Tham chiếu Đa hình (`CARD_PROGRESS`, `REVIEW_LOG`) [COMPLETED]**
  - *Phạm vi:* Ánh xạ cặp trường `item_type` (`VARCHAR(20)`) và `item_id` (`BIGINT UNSIGNED`). **Tuyệt đối không tạo FK vật lý ở MySQL** theo đúng Phương án A đã duyệt.
  - *Lưu ý kỹ thuật:* Ràng buộc kiểm tra sự tồn tại của từ vựng hoặc bộ thủ là **Service/Business dependency**, không phải physical FK dependency. Do đó Entity mapping chỉ phụ thuộc vào `UserProfile`.
  - *depends_on:* `Task 2A.1` (UserProfile).
* **Task 2D.3: Spring Data JPA Repositories Cụm SRS, Ghi chú & Kiểm toán [COMPLETED]**
  - *Phạm vi:* `CardProgressRepository` (tìm thẻ đến hạn `next_review_at <= NOW()`), `ReviewLogRepository`, `PersonalNoteRepository`, `ModerationLogRepository`, `UserSrsSettingRepository`.
  - *depends_on:* `Task 2D.1`, `Task 2D.2`.
* **Checkpoint 2D: [COMPLETED]** Chạy `@DataJpaTest` kiểm tra lưu trữ thẻ học đa hình, truy vấn thẻ đến hạn ôn tập và lưu ghi chú cá nhân PASS 100%.

#### Module 2E: Database Seed Migrations (Flyway) [PARALLEL với Mod 2A..2D]
* **Task 2E.1: Flyway Seed Data V2: 4 Vai trò hệ thống (`V2__seed_roles.sql`) [COMPLETED]**
  - *Phạm vi:* Script seed 4 vai trò cố định: `1=Learner`, `2=Creator`, `3=Moderator`, `4=Admin`.
  - *depends_on:* `Task 1A.4`.
* **Task 2E.2: Flyway Seed Data V3: 214 Bộ thủ Khang Hy (`V3__seed_radicals.sql`) [COMPLETED]**
  - *Phạm vi:* Script seed 214 bộ thủ Khang Hy chuẩn từ dataset thẩm quyền `.agents/references/radicals.json`.
  - *depends_on:* `Task 1A.4`.
* **Checkpoint 2E: [COMPLETED]** Flyway migration V2 và V3 chạy thành công. Kiểm tra `SELECT COUNT(*) FROM role` trả về 4; `SELECT COUNT(*) FROM radical` trả về 214.

#### Module 2F: Persistence Layer Verification & Schema Validation
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
- **Trạng thái:** **`NOT_STARTED`**.

#### Module 3A: Spring Security & JWT Infrastructure
* **Task 3A.1: Cấu hình `SecurityConfig`: Spring Security 6 `SecurityFilterChain`, `BCryptPasswordEncoder`, cấu hình session `STATELESS`, vô hiệu hóa CSRF cho REST [COMPLETED]**
* **Task 3A.2:** Xây dựng `JwtUtil` (sinh token, giải mã claims, kiểm tra hết hạn) và `JwtAuthenticationFilter` (chặn request, giải mã header `Bearer`, nạp SecurityContext).
* **Task 3A.3:** Triển khai `CustomUserDetailsService` và `CustomUserDetails` nạp người dùng từ `AccountRepository` và ánh xạ roles thành GrantedAuthorities.
* **Checkpoint 3A:** Unit test cho `JwtUtil` (tạo token, trích xuất claim `email_or_phone`, phát hiện token hết hạn/sai chữ ký) và `CustomUserDetailsService`.

#### Module 3B: Authentication & Registration Vertical Slice
* **Task 3B.1:** Request/Response DTOs: `RegisterRequest`, `LoginRequest`, `AuthResponse` với Jakarta Validation (`@NotBlank`, `@Size`, v.v.).
* **Task 3B.2:** `AuthService` và `AuthController` (`POST /api/v1/auth/register` gán mặc định role `Learner`, `POST /api/v1/auth/login` kiểm tra mật khẩu qua BCrypt, trả về JWT gói trong `ApiResponse`).
* **Checkpoint 3B:** MockMvc test: Đăng ký thành công trả về 201; Đăng nhập đúng trả về 200 kèm JWT; Đăng nhập sai mật khẩu trả về 401; Input thiếu trường trả về 400 kèm lỗi validation.

#### Module 3C: User Profile Vertical Slice
* **Task 3C.1:** DTOs (`UserProfileResponse`, `UpdateProfileRequest`), `UserProfileService` và `UserProfileController` (`GET /api/v1/users/profile`, `PUT /api/v1/users/profile` lấy và cập nhật profile của user đang đăng nhập qua `ApiResponse`).
* **Checkpoint 3C:** MockMvc test: Truy cập profile khi có JWT hợp lệ trả về 200; truy cập khi không có JWT trả về 401 Unauthorized.

#### Module 3D: Security & RBAC Verification
* **Task 3D.1:** Bộ kiểm thử tích hợp tự động cho phân quyền RBAC 4 vai trò (Learner, Creator, Moderator, Admin).
* **Checkpoint Phase 3:** MockMvc test xác minh chặn 403 Forbidden khi Learner cố truy cập endpoint yêu cầu quyền Admin/Moderator/Creator.

---

### Phase 4 — Radical and Vocabulary Catalog Domain
- **Mục tiêu:** Cung cấp RESTful APIs tra cứu danh mục 214 Bộ thủ Khang Hy và Từ vựng tiếng Trung, hỗ trợ tìm kiếm đa tiêu chí (pinyin có dấu, pinyin không dấu `pinyin_raw`, chữ Hán `hanzi`), lọc theo bộ thủ, phân trang chuẩn `PageResponse`, và Admin CRUD.
- **Ranh giới phụ thuộc:** Phụ thuộc vào `Module 1B` (cho Controller response contract), `Module 2B`, `Task 2E.2` (data 214 bộ thủ), và `Phase 3` (xác thực quyền Admin).
- **Trạng thái:** **`NOT_STARTED`**.

#### Module 4A: Radical Catalog Vertical Slice
* **Task 4A.1:** Radical DTOs (`RadicalResponse`, `RadicalDetailResponse`) và `RadicalService` (danh sách 214 bộ thủ, chi tiết bộ thủ theo ID/ký tự, danh sách từ vựng chứa bộ thủ).
* **Task 4A.2:** `RadicalController` (`GET /api/v1/radicals`, `GET /api/v1/radicals/{id}` công khai) và Admin endpoints (`POST/PUT/DELETE /api/v1/admin/radicals/**` yêu cầu role Admin).
* **Checkpoint 4A:** MockMvc test: Tra cứu công khai trả về đủ 214 bộ thủ; Gọi Admin API bằng quyền Learner bị từ chối 403 Forbidden.

#### Module 4B: Vocabulary Catalog & Search Vertical Slice
* **Task 4B.1:** Vocabulary DTOs (`VocabularyResponse`, `VocabularySearchCriteria`) và `VocabularyService` (tìm kiếm linh hoạt: theo chữ Hán, pinyin có dấu, pinyin thô không dấu `pinyin_raw`, lọc theo bộ thủ, phân trang qua `Pageable`).
* **Task 4B.2:** `VocabularyController` (`GET /api/v1/vocabularies`, `GET /api/v1/vocabularies/{id}` công khai) và Admin endpoints (`POST/PUT/DELETE /api/v1/admin/vocabularies/**`).
* **Checkpoint 4B:** MockMvc test: Tìm kiếm từ vựng với từ khóa tone-less (ví dụ: `ni` tìm ra `nǐ / 你`), kiểm tra dữ liệu phân trang trả về đúng định dạng `PageResponse`.

#### Module 4C: Catalog Domain Verification
* **Task 4C.1:** Kiểm thử tự động tích hợp tra cứu và bảo vệ quyền quản trị dữ liệu gốc.
* **Checkpoint Phase 4:** MockMvc test toàn diện các ca tìm kiếm từ vựng, chi tiết kèm bộ thủ liên kết, phân trang và xử lý từ khóa rỗng/không hợp lệ.

---

### Phase 5 — Lesson Management and Excel Import
- **Mục tiêu:** Cung cấp API quản lý bài học cho Creator, cơ chế phân tích cú pháp và import file Excel 2 bước bằng Apache POI, và API khám phá bài học công khai cho Learner.
- **Ranh giới phụ thuộc:** Phụ thuộc vào `Module 1B` (cho Controllers), `Module 2C`, `Phase 3` (Creator auth), `Phase 4` (Vocabulary catalog).
- **Trạng thái:** **`NOT_STARTED`**.

#### Module 5A: Public Lesson Exploration Vertical Slice
* **Task 5A.1:** Lesson DTOs (`LessonSummaryResponse`, `LessonDetailResponse`, `LessonVocabItemResponse`) và `LessonService`.
* **Task 5A.2:** `LessonController` (`GET /api/v1/lessons`, `GET /api/v1/lessons/{id}` công khai — **chỉ trả về bài học có `status = Approved`**, danh sách từ vựng sắp xếp đúng theo `order_index`).
* **Checkpoint 5A:** MockMvc test: Bài học ở trạng thái `Draft` hoặc `Pending` không xuất hiện trên API công khai (trả về 404/rỗng).

#### Module 5B: Creator Lesson Studio Vertical Slice
* **Task 5B.1:** Creator DTOs (`CreateLessonRequest`, `UpdateLessonRequest`, `ReorderVocabRequest`) và `CreatorLessonService` (CRUD bài học, kiểm tra quyền sở hữu tác giả, thay đổi thứ tự từ vựng, nộp bài kiểm duyệt `Draft` $\rightarrow$ `Pending`).
* **Task 5B.2:** `CreatorLessonController` (`POST/PUT/DELETE /api/v1/creator/lessons/**`).
* **Checkpoint 5B:** MockMvc test: Creator tạo bài học nháp, thêm từ vựng, đổi thứ tự, nộp bài duyệt; Creator A cố sửa bài của Creator B bị chặn 403 Forbidden.

#### Module 5C: Two-Step Excel Import Engine
* **Task 5C.1:** `ExcelParserService` sử dụng Apache POI (bước 1: đọc file `.xlsx`, validate từng hàng: chữ Hán, pinyin, nghĩa, kiểm tra từ vựng đã tồn tại hay tạo mới, xuất báo cáo validation preview kèm mã lỗi chi tiết).
* **Task 5C.2:** Excel Import Controller Endpoints (`POST /api/v1/creator/lessons/import/preview` bước 1, `POST /api/v1/creator/lessons/import/confirm` bước 2 lưu transactional).
* **Checkpoint 5C:** Test tự động upload file Excel chuẩn thành công; upload file sai định dạng cột hoặc file rỗng trả về preview lỗi chi tiết ở cấp độ hàng (Row-level validation error).

#### Module 5D: Lesson Lifecycle Verification
* **Task 5D.1:** Kiểm thử tự động vòng đời bài học từ tạo nháp, import Excel đến nộp duyệt.
* **Checkpoint Phase 5:** MockMvc test chu trình hoàn chỉnh: Import file Excel $\rightarrow$ Xem preview $\rightarrow$ Lưu bài nháp $\rightarrow$ Sắp xếp từ vựng $\rightarrow$ Nộp bài duyệt (`status = Pending`).

---

### Phase 6 — Content Moderation Workflow
- **Mục tiêu:** Cung cấp hàng đợi kiểm duyệt bài học cho Moderator, thực thi phê duyệt (`Approved`) hoặc từ chối (`Rejected` bắt buộc lý do và danh sách trường lỗi dạng JSON), và ghi vết bất biến vào `MODERATION_LOG`.
- **Ranh giới phụ thuộc:** Phụ thuộc vào `Module 1B` (cho Controllers), `Module 2C`, `Module 2D`, `Phase 3` (Moderator auth), `Phase 5` (Pending lessons).
- **Trạng thái:** **`NOT_STARTED`**.

#### Module 6A: Moderation Core Business Logic
* **Task 6A.1:** Moderation DTOs (`ModerationQueueResponse`, `ApproveLessonRequest`, `RejectLessonRequest` với `@NotBlank rejection_reason` và `flagged_fields` JSON) và `ModerationService`.
* **Task 6A.2:** Thực thi bất biến kiểm duyệt và ghi vết kiểm toán: Chỉ bài học `Pending` mới được duyệt/từ chối; Ghi log bất biến vào `MODERATION_LOG` (`ON DELETE RESTRICT`).
* **Checkpoint 6A:** Service test: Từ chối bài học thiếu lý do ném `BusinessException`; Duyệt bài học không ở trạng thái `Pending` bị từ chối; Log kiểm toán được lưu đúng trường.

#### Module 6B: Moderation REST Endpoints
* **Task 6B.1:** `ModeratorController` (`GET /api/v1/moderator/lessons/pending`, `POST /api/v1/moderator/lessons/{id}/approve`, `POST /api/v1/moderator/lessons/{id}/reject`, `GET /api/v1/moderator/logs/{lessonId}`).
* **Checkpoint 6B:** MockMvc test: Moderator xem hàng đợi duyệt, bấm duyệt bài (status thành `Approved`), bấm từ chối (status thành `Rejected` kèm log); Learner truy cập bị chặn 403.

#### Module 6C: Moderation Workflow Verification
* **Task 6C.1:** Kiểm thử tự động tích hợp quy trình xuất bản nội dung.
* **Checkpoint Phase 6:** Test chu trình: Creator nộp bài $\rightarrow$ Moderator từ chối $\rightarrow$ Creator sửa bài và nộp lại $\rightarrow$ Moderator phê duyệt $\rightarrow$ Bài học lập tức hiển thị trên Public Lesson API.

---

### Phase 7 — Spaced Repetition System (SRS SM-2 Engine)
- **Mục tiêu:** Triển khai cỗ máy tính toán lặp lại ngắt quãng SM-2 thuần túy toán học, quản lý phiên ôn tập thẻ học hàng ngày, ghi nhận nhật ký phản xạ `review_time_seconds` và cập nhật tiến trình ghi nhớ `CARD_PROGRESS`.
- **Ranh giới phụ thuộc:**
  - `Module 7A` (Toán học SM-2): **Độc lập hoàn toàn** (không phụ thuộc database, spring hay DTOs).
  - `Module 7B` (SRS Session): Phụ thuộc `Task 7A.1`, `Module 2D` (CardProgress), `Phase 3` (Learner auth), `Module 1B` (SrsController).
- **Trạng thái:** **`NOT_STARTED`**.

#### Module 7A: Pure SM-2 Calculation Engine
* **Task 7A.1:** Thành phần toán học thuần túy `SrsCalculator` (tính toán `interval_days`, `ease_factor`, `repetitions` dựa trên rating 1–4: 1=Again, 2=Hard, 3=Good, 4=Easy; chặn sàn $EF \ge 1.30$; chu kỳ bước nhảy 1 ngày, 6 ngày, v.v.).
* **Task 7A.2:** Unit test toán học toàn diện cho `SrsCalculator` kiểm tra đầy đủ các nhánh logic và điều kiện biên.
* **Checkpoint 7A:** Unit test độc lập toán học chạy PASS, xác nhận tính toán chính xác không phụ thuộc database hay Spring context.

#### Module 7B: SRS Review Session & Card Progress Vertical Slice
* **Task 7B.1:** SRS DTOs (`DueCardResponse`, `ReviewCardRequest`, `StudyStatsResponse`) và `SrsService` (lấy thẻ đến hạn `next_review_at <= NOW()`, áp dụng giới hạn ngày từ `USER_SRS_SETTING`, cập nhật `CARD_PROGRESS`, ghi nhật ký `REVIEW_LOG` kèm `review_time_seconds`).
* **Task 7B.2:** `SrsController` (`GET /api/v1/srs/due`, `POST /api/v1/srs/review`, `GET /api/v1/srs/stats`).
* **Checkpoint 7B:** MockMvc test: Học viên lấy danh sách thẻ cần học hôm nay, gửi kết quả đánh giá (Rating 3 kèm 4 giây phản xạ), kiểm tra `CARD_PROGRESS` cập nhật ngày ôn tập tiếp theo và `REVIEW_LOG` được ghi nhận.

#### Module 7C: SRS Verification & Polymorphic Resolution
* **Task 7C.1:** Kiểm thử tự động tích hợp cho cả 2 loại thẻ học: `VOCABULARY` và `RADICAL` (tham chiếu đa hình `item_type` + `item_id`).
* **Checkpoint Phase 7:** Test suite xác minh tính toàn vẹn tham chiếu đa hình ở tầng Service: từ chối tạo tiến trình nếu `item_id` không tồn tại trong bảng tương ứng.

---

### Phase 8 — Personal Notes & User Learning Settings
- **Mục tiêu:** Cung cấp tính năng ghi chú cá nhân của người học trên từng từ vựng (thực thi nghiêm ngặt `content <= 500 characters`, không áp dụng giới hạn 5 ghi chú) và tùy biến chỉ số học tập SRS cá nhân.
- **Ranh giới phụ thuộc:** Phụ thuộc vào `Module 1B` (cho Controllers), `Module 2D`, `Phase 3` (Learner auth), `Phase 4` (Vocabulary).
- **Trạng thái:** **`NOT_STARTED`**.

#### Module 8A: Personal Notes Vertical Slice
* **Task 8A.1:** Personal Note DTOs (`PersonalNoteRequest`, `PersonalNoteResponse`) và `PersonalNoteService` (CRUD ghi chú, kiểm tra độ dài $\le 500$ ký tự, đảm bảo không giới hạn số lượng ghi chú/từ vựng, bảo vệ quyền sở hữu người dùng).
* **Task 8A.2:** `PersonalNoteController` (`GET /api/v1/vocabularies/{vocabId}/notes`, `POST /api/v1/vocabularies/{vocabId}/notes`, `PUT/DELETE /api/v1/notes/{noteId}`).
* **Checkpoint 8A:** MockMvc test: Tạo ghi chú $\le 500$ chars thành công (201); Tạo ghi chú $> 500$ chars bị từ chối 400 Validation Error; User A cố sửa/xóa ghi chú của User B bị chặn 403 Forbidden.

#### Module 8B: SRS User Settings Vertical Slice
* **Task 8B.1:** Setting DTOs (`UserSrsSettingResponse`, `UpdateSrsSettingRequest`), `UserSrsSettingService` và `UserSrsSettingController` (`GET/PUT /api/v1/srs/settings`: `new_cards_per_day` mặc định 20, `max_review_per_day` mặc định 100).
* **Checkpoint 8B:** MockMvc test: Lấy cấu hình mặc định, cập nhật số thẻ mới mỗi ngày, xác thực giá trị dương $> 0$.

#### Module 8C: Personalization Verification
* **Task 8C.1:** Kiểm thử tự động tích hợp cho phân hệ cá nhân hóa học tập.
* **Checkpoint Phase 8:** Test tích hợp: Cập nhật setting giới hạn thẻ $\rightarrow$ ảnh hưởng trực tiếp đến số lượng thẻ trả về trong API lấy thẻ đến hạn ôn tập của Phase 7.

---

### Phase 9 — Frontend UI & Client API Integration
- **Mục tiêu:** Xây dựng giao diện người dùng bằng HTML5, CSS chuẩn và Vanilla JavaScript, kết nối toàn bộ hệ thống REST API thông qua client tập trung `api.js`, xử lý đầy đủ 3 trạng thái giao diện (Loading, Empty, Error).
- **Ranh giới phụ thuộc:** Phụ thuộc vào các backend APIs tương ứng (`Phase 3` $\rightarrow$ `Phase 8`).
- **Trạng thái:** **`NOT_STARTED`**.

#### Module 9A: Frontend Foundation & Centralized HTTP Client
* **Task 9A.1:** Khởi tạo layout chung (HTML5, responsive CSS grid/flexbox, thanh điều hướng navbar, footer, container modal thông báo).
* **Task 9A.2:** Xây dựng `frontend/js/api.js` tập trung (wrapper `fetch()`, tự động đính kèm `Authorization: Bearer <token>`, tự động bắt lỗi 401 xóa token và điều hướng về `login.html`, chuẩn hóa phản hồi `ApiResponse`).
* **Checkpoint 9A:** Kiểm tra trên trình duyệt: trang khung hiển thị đúng, `api.js` gửi request mẫu và bắt đúng 401 điều hướng login.

#### Module 9B: Learner Identity & Catalog Experience
* **Task 9B.1:** Màn hình Đăng ký, Đăng nhập và Hồ sơ cá nhân (`login.html`, `register.html`, `profile.html`: form validation, lưu token `localStorage`, xử lý 3 trạng thái loading/empty/error).
* **Task 9B.2:** Màn hình Tra cứu 214 Bộ thủ và Từ vựng (`radicals.html`, `vocabulary.html`: lưới bộ thủ Khang Hy, thanh tìm kiếm từ vựng với pinyin và chữ Hán, modal chi tiết nét viết và phát âm audio).
* **Checkpoint 9B:** Kiểm tra DevTools: Đăng ký $\rightarrow$ Đăng nhập $\rightarrow$ Lưu token $\rightarrow$ Xem hồ sơ $\rightarrow$ Tra cứu bộ thủ $\rightarrow$ Tìm kiếm từ vựng bằng pinyin không dấu.

#### Module 9C: Learner Lesson & Interactive SRS Experience
* **Task 9C.1:** Màn hình Khám phá Bài học & Thêm ghi chú cá nhân (`lessons.html`, `lesson-detail.html`: xem bài học công khai, danh sách từ vựng theo `order_index`, popover tạo và sửa ghi chú cá nhân $\le 500$ ký tự).
* **Task 9C.2:** Màn hình Ôn tập Flashcard SRS tương tác (`srs-review.html`: lật thẻ 3D, nút nghe phát âm audio, 4 nút đánh giá Again/Hard/Good/Easy, bộ đếm giây phản xạ `review_time_seconds`, màn hình tổng kết phiên học).
* **Checkpoint 9C:** Kiểm tra DevTools: Mở bài học $\rightarrow$ Đọc từ vựng $\rightarrow$ Ghi chú $\rightarrow$ Vào phòng ôn tập SRS $\rightarrow$ Lật thẻ $\rightarrow$ Bấm đánh giá $\rightarrow$ Thẻ được chuyển trạng thái đúng.

#### Module 9D: Creator Studio & Two-Step Import Experience
* **Task 9D.1:** Màn hình Creator Lesson Studio (`creator-lessons.html`: tạo bài học nháp, kéo thả sắp xếp thứ tự từ vựng, nút nộp bài kiểm duyệt).
* **Task 9D.2:** Giao diện Import Excel 2 bước (`creator-import.html`: dropzone kéo thả file `.xlsx`, render bảng preview dữ liệu kèm chỉ báo lỗi từng dòng, nút xác nhận lưu bài học).
* **Checkpoint 9D:** Kiểm tra DevTools: Tác giả tải file Excel $\rightarrow$ Hiển thị bảng preview $\rightarrow$ Xác nhận $\rightarrow$ Bài học được tạo ở trạng thái `Draft` $\rightarrow$ Nộp bài duyệt (`Pending`).

#### Module 9E: Moderator Review Dashboard Experience
* **Task 9E.1:** Màn hình Bàn làm việc Kiểm duyệt viên (`moderator.html`: danh sách bài chờ duyệt `Pending`, modal xem chi tiết nội dung và danh sách từ vựng, nút Phê duyệt `Approved`, modal Từ chối `Rejected` bắt buộc nhập lý do và chọn các trường lỗi).
* **Checkpoint 9E:** Kiểm tra DevTools: Moderator xem hàng đợi $\rightarrow$ Mở bài học kiểm tra $\rightarrow$ Duyệt bài hoặc từ chối kèm lý do $\rightarrow$ Kiểm tra trạng thái bài học cập nhật tức thì.

#### Module 9F: Browser Verification & UI Hardening
* **Task 9F.1:** Kiểm thử giao diện và tương tác toàn diện trên trình duyệt bằng Chrome DevTools (Console sạch không có lỗi JS, Network tab kiểm tra đúng định dạng JSON, kiểm tra tính đáp ứng responsive trên màn hình di động/desktop, kiểm tra chống XSS DOM).
* **Checkpoint Phase 9:** Toàn bộ 5 luồng trải nghiệm người dùng trên trình duyệt hoạt động mượt mà, không phát sinh lỗi giao diện.

---

### Phase 10 — Security, Performance & Quality Hardening
- **Mục tiêu:** Rà soát an ninh ứng dụng theo chuẩn OWASP, gia cố chống tấn công brute-force, tối ưu hóa hiệu năng truy vấn CSDL và hoàn thiện bộ kiểm thử tự động cho các kịch bản biên.
- **Ranh giới:** Đây là giai đoạn gia cố an ninh, tối ưu hiệu năng và đóng các khoảng trống kiểm thử (Coverage/Regression Gaps). Kiểm thử chức năng cơ bản đã được thực hiện tại từng Task/Module trước đó.
- **Trạng thái:** **`NOT_STARTED`**.

#### Module 10A: Application Security Hardening
* **Task 10A.1: Rà soát an ninh OWASP toàn diện & Gia cố bảo vệ tài nguyên**
  - *Phạm vi:* Cấu hình CORS chặt chẽ, kiểm tra chống XSS DOM, rà soát JPA parameterized query (ngăn ngừa SQL Injection), kiểm tra an toàn upload file Excel (MIME type, giới hạn dung lượng 10MB, chống path traversal).
  - *depends_on:* `Phase 9`.
* **Task 10A.2: Kiểm soát tần suất gọi (Rate Limiting) trên Auth endpoints**
  - *Phạm vi:* Cơ chế giới hạn tần suất gọi tại `/api/v1/auth/login` để phòng chống brute-force mật khẩu.
  - *depends_on:* `Task 10A.1`.
* **Checkpoint 10A:** Bộ test bảo mật xác nhận: payload độc hại được xử lý an toàn; gửi 20 request login liên tiếp kích hoạt HTTP 429 Too Many Requests.

#### Module 10B: Database & Application Performance
* **Task 10B.1: Tối ưu hóa truy vấn CSDL & Xác minh chỉ mục MySQL**
  - *Phạm vi:* Chạy `EXPLAIN` trên các truy vấn trọng yếu (`idx_card_progress_due`, `idx_vocab_pinyin_raw`, `idx_lesson_status`), loại trừ rủi ro slow query và table scan không mong muốn trên các tập dữ liệu lớn.
  - *depends_on:* `Phase 9`.
* **Checkpoint 10B:** Execution plan của MySQL qua `EXPLAIN` xác nhận các chỉ mục được tận dụng tối ưu, không có full table scan đối với các truy vấn tra cứu chính.

#### Module 10C: Quality Gap Closure
* **Task 10C.1: Hoàn thiện khoảng trống kiểm thử tự động & Kịch bản biên**
  - *Phạm vi:* Bổ sung test case cho các điều kiện biên của Service và Controller, kiểm tra hồi quy chéo giữa các phân hệ nghiệp vụ.
  - *depends_on:* `Task 10A.2`, `Task 10B.1`.
* **Checkpoint 10C:** Toàn bộ test suite chạy thành công không có lỗi hồi quy; các kịch bản biên quan trọng đều có automated test bao phủ.
* **Phase 10 Integration Checkpoint:** Lệnh `mvn clean verify` chạy thành công; các kiểm tra an ninh, hiệu năng và chất lượng đều đáp ứng tiêu chuẩn.

---

### Phase 11 — Final Integration, Release Validation & Packaging
- **Mục tiêu:** Thực thi các kịch bản kiểm thử tích hợp đầu cuối (Full User Journey E2E), đóng gói ứng dụng thành file JAR độc lập và bàn giao bộ tài liệu vận hành hoàn chỉnh.
- **Trạng thái:** **`NOT_STARTED`**.

#### Module 11A: End-to-End User Journey Validation
* **Task 11A.1:** Kịch bản E2E 1: Luồng Học viên (Đăng ký tài khoản $\rightarrow$ Khám phá bộ thủ $\rightarrow$ Thêm ghi chú từ vựng $\rightarrow$ Ôn tập Flashcard SRS $\rightarrow$ Kiểm tra tiến trình ghi nhớ).
* **Task 11A.2:** Kịch bản E2E 2: Luồng Xuất bản Nội dung (Tác giả import Excel $\rightarrow$ Xem preview $\rightarrow$ Lưu bài $\rightarrow$ Nộp duyệt $\rightarrow$ Moderator kiểm duyệt $\rightarrow$ Phê duyệt $\rightarrow$ Học viên học bài mới).
* **Checkpoint 11A:** Cả 2 kịch bản E2E tự động chạy thành công và vượt qua tất cả các bước kiểm chứng.

#### Module 11B: Production Packaging & Handover Artifacts
* **Task 11B.1:** Đóng gói sản phẩm: chạy `mvn clean package`, kiểm tra file thực thi `target/elearning-backend-1.0.0.jar` khởi động thành công độc lập với profile production và chạy migration trên CSDL sạch.
* **Task 11B.2:** Nghiệm thu bàn giao: xuất file Postman Collection đầy đủ cho toàn bộ API kèm biến môi trường, hoàn thiện tài liệu hướng dẫn vận hành hệ thống.
* **Checkpoint Phase 11:** Ứng dụng khởi động độc lập thành công, Postman Collection chạy pass toàn bộ endpoints, sẵn sàng bàn giao cho người dùng.
