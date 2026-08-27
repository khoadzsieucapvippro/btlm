# CURRENT PROJECT STATE & AI HANDOFF DOCUMENT

> **Tài liệu:** Bàn giao Hiện trạng Thực tế Dự án (Primary AI Handoff & Project State Document)  
> **Dự án:** Hệ thống Website học Bộ thủ và Từ vựng Tiếng Trung (E-learning Chinese Radicals & Vocabulary)  
> **Vị trí file:** `.agents/CURRENT_STATE.md`  
> **Cam kết tính xác thực:** Mô tả **HIỆN TRẠNG THỰC TẾ (REAL ACTUAL STATE)** của mã nguồn, CSDL và cấu hình trong repository; KHÔNG phản ánh hiện trạng mong muốn (intended state) hay báo cáo lạc quan.  
> **Phiên bản cập nhật:** Sau khi hoàn thành Task 4A.1 (Phase 4 — Module 4A). Hiện đang ở Task 4A.2.  
> **Commit hash hiện tại:** `e8789f0`  
> **Bằng chứng kiểm thử gần nhất:** **`201/201 tests PASS, Failures: 0, Errors: 0, Build SUCCESS`** (thời gian: 43.69s).  

---

## 0. TỔNG QUAN ĐIỀU HÀNH CHO AI CODING AGENT MỚI (EXECUTIVE SUMMARY)

Một AI coding agent khi mở tài liệu này cần nắm ngay 6 câu trả lời cốt lõi:

| Câu hỏi | Câu trả lời chuẩn xác (Authoritative Answer) |
| :--- | :--- |
| **1. Where are we now?** | **Phase 4 — Radical and Vocabulary Catalog Domain**, phân hệ **Module 4A — Radical Catalog Vertical Slice**. |
| **2. What has been completed?** | **Phase 0** (Spec & DB Design), **Phase 1** (Scaffold & Web Infrastructure), **Phase 2** (Persistence & Seed Data V1-V3), **Phase 3** (Security, JWT, Auth, User Profile, RBAC Verification), và **Task 4A.1** (Radical DTOs & RadicalService). |
| **3. What is currently in progress?** | **Phase 4 — Radical and Vocabulary Catalog Domain** (chuẩn bị triển khai **Task 4A.2**). |
| **4. What is the next task?** | **`Task 4A.2 — RadicalController công khai & Admin CRUD Bộ thủ`** (`GET /api/v1/radicals/**` công khai và `POST/PUT/DELETE /api/v1/admin/radicals/**` yêu cầu role `Admin`). |
| **5. What must not be changed?** | - Tuyệt đối **không sửa** các file Flyway migration cũ (`V1`, `V2`, `V3`).<br>- Giữ nguyên cấu hình Hibernate `ddl-auto: none`.<br>- Admin CRUD là thao tác nghiệp vụ tại runtime qua Service/Repository, **tuyệt đối không sửa Flyway**.<br>- Tuyệt đối **không leak JPA Entity** ra Controller (100% qua DTO).<br>- Không tự ý thêm trường `radicalNumber` hay `strokeCount` (không có trong schema vật lý).<br>- Không can thiệp sang phân hệ Từ vựng (Module 4B). |
| **6. What evidence confirms the state?** | - `mvn clean test` PASS **201/201 tests**, 0 failures, 0 errors.<br>- MySQL `elearning_db` có 15 bảng (14 bảng nghiệp vụ + 1 bảng Flyway ở version 3), 4 roles, 214 bộ thủ Khang Hy.<br>- Git commit log sạch sẽ trên nhánh `main`. |

---

## 1. NGUỒN CHÂN LÝ VÀ ĐỐI CHIẾU HIỆN TRẠNG (SOURCE OF TRUTH)

Khi làm việc trên repository này, AI Agent mới **BẮT BUỘC** tuân thủ thứ tự ưu tiên thẩm quyền sau:
1. **Yêu cầu trực tiếp của User trong lượt tương tác hiện tại** (Prompt của User là quyết định tối cao).
2. **Tài liệu bàn giao hiện trạng thực tế này (`.agents/CURRENT_STATE.md`)**.
3. **Bằng chứng vật lý thực tế trong repository** (Code thực tế trong `backend/`, schema thực tế trong MySQL, Git commit history).
4. **Các quy tắc dự án** (`.agents/rules/`).
5. **Bộ tài liệu đặc tả chuẩn** (`.agents/DATABASE.md`, `.agents/DATABASE_DESIGN.md`, `.agents/API.md`, `.agents/ARCHITECTURE.md`, `.agents/DECISIONS.md`, `.agents/OPEN_QUESTIONS.md`, `.agents/PROGRESS.md`, `.agents/ROADMAP.md`).
6. **Agent Skills** trong `.agents/skills/`.

> [!CAUTION]
> **Quy tắc bất biến:**
> - Dự án được **XÂY DỰNG MỚI HOÀN TOÀN TỪ ĐẦU (REBUILD FROM SCRATCH)**.
> - Tuyệt đối không phục hồi, không tham chiếu, không sử dụng lại bất kỳ mã nguồn, entity, DTO, migration, controller, hay tài liệu triển khai cũ nào trước đợt reset.
> - Phân định rạch ròi giữa **ĐẶC TẢ ĐÃ DUYỆT (DOCUMENTED)** và **MÃ NGUỒN THỰC TẾ ĐÃ VIẾT (ACTUALLY IMPLEMENTED)**.

---

## 2. ĐỊNH DANH DỰ ÁN (PROJECT IDENTITY)

* **Tên dự án:** Website học Bộ thủ và Từ vựng Tiếng Trung kết hợp thuật toán lặp lại ngắt quãng (SRS).
* **Mục tiêu hệ thống:** Cung cấp nền tảng học tập bài bản tiếng Trung qua 214 bộ thủ Khang Hy chuẩn, từ vựng theo bài học, câu ví dụ, hỗ trợ nhập liệu bài học từ Excel (2 bước), và cỗ máy ôn tập ghi nhớ dài hạn SM-2 (SuperMemo-2).
* **Phân quyền người dùng (4 Roles RBAC):**
  1. `Learner` (Học viên): Học bộ thủ, từ vựng, bài học công khai; ôn tập Flashcard qua thuật toán SRS; ghi chú cá nhân; tùy chỉnh số lượng thẻ học mỗi ngày.
  2. `Creator` (Tác giả nội dung): Tạo bài học cá nhân, tải file Excel bài học lên hệ thống (xem trước dữ liệu trước khi lưu), gửi bài học chờ duyệt.
  3. `Moderator` (Kiểm duyệt viên): Duyệt hàng đợi bài học (`Pending`), phê duyệt (`Approve`) hoặc từ chối (`Reject`) kèm lý do và đánh dấu các trường lỗi JSON (`flagged_fields`).
  4. `Admin` (Quản trị viên): Toàn quyền quản trị hệ thống, quản lý tài khoản, phân quyền, giám sát dữ liệu và nhật ký kiểm toán.
* **5 Phân hệ nghiệp vụ chính:**
  1. *Authentication & User Profile*: Đăng ký, đăng nhập, JWT stateless, hồ sơ cá nhân (ĐÃ HOÀN THÀNH trong Phase 3).
  2. *Curriculum & Dictionary*: 214 Bộ thủ Khang Hy, từ vựng (chữ Hán, Pinyin có dấu/không dấu, Hán-Việt, dịch nghĩa, media URLs), bài học và phân thứ tự từ vựng (ĐANG TRIỂN KHAI trong Phase 4 & 5).
  3. *Moderation*: Quy trình kiểm duyệt bài học và lưu vết `MODERATION_LOG` (Phase 6).
  4. *Spaced Repetition System (SRS)*: Thuật toán SM-2, quản lý tiến trình thẻ (`CARD_PROGRESS`), nhật ký ôn tập (`REVIEW_LOG`) (Phase 7).
  5. *Personalization*: Ghi chú từ vựng cá nhân (`PERSONAL_NOTE` $\le 500$ ký tự) và cài đặt SRS (`USER_SRS_SETTING`) (Phase 8).

---

## 3. THÔNG SỐ CÔNG NGHỆ ĐÃ XÁC MINH (TECHNOLOGY BASELINE)

Toàn bộ môi trường đã được cài đặt, kích hoạt và kiểm chứng bằng lệnh thực tế:

| Thành phần | Công nghệ đã duyệt | Phiên bản thực tế xác minh | Ghi chú & Đường dẫn vật lý |
| :--- | :--- | :--- | :--- |
| **Hệ điều hành** | Windows 11 64-bit | Windows 11 Build 26200 amd64 | Máy trạm cục bộ của User |
| **Ngôn ngữ** | Java / JDK | **OpenJDK 21.0.12 LTS** | Eclipse Adoptium Temurin 21.0.12+8 tại `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot\` |
| **Công cụ Build** | Apache Maven | **Apache Maven 3.9.16** | Tại `C:\apache-maven-3.9.16-bin\apache-maven-3.9.16` |
| **Backend Framework** | Spring Boot | **Spring Boot 3.3.5** | Thế hệ Spring Boot 3.3.x ổn định với Java 21 LTS |
| **Mô hình Web** | Spring MVC | Đã tích hợp trong Spring Boot | RESTful API, JSON payload, UTF-8 |
| **Hệ quản trị CSDL** | MySQL Server | **MySQL Community Server 8.4.9 LTS** | Port: `3306`, DataDir: `C:\ProgramData\MySQL\MySQL Server 8.4\Data`, config: `my.ini` |
| **Tên Database** | MySQL Database | **`elearning_db`** | Charset: `utf8mb4`, Collation: `utf8mb4_unicode_ci` |
| **Cơ chế Migration** | Flyway | **Flyway 10.x** (`flyway-core` + `flyway-mysql`) | 3 migrations áp dụng thành công: `V1`, `V2`, `V3` |
| **Tầng Persistence** | Spring Data JPA / Hibernate | **Hibernate 6.5.3.Final** | Cấu hình `spring.jpa.hibernate.ddl-auto: none` (Cấm Hibernate tự sửa schema) |
| **Bảo mật** | Spring Security 6 & JJWT | **JJWT 0.12.6** + BCrypt | Đã triển khai hoàn tất trong Phase 3 (Stateless JWT, RBAC 4 vai trò) |
| **Frontend (Kế hoạch)** | HTML, CSS, JavaScript | Chưa triển khai code | Sẽ triển khai tại Phase 9 (`frontend/` hiện là thư mục rỗng có `.gitkeep`) |
| **Quản lý phiên bản** | Git | Git cục bộ nhánh `main` | Đã cấu hình `.gitignore`, commit hash hiện tại: `e8789f0` |

---

## 4. KIẾN TRÚC HỆ THỐNG VÀ RANH GIỚI VẬN HÀNH (ARCHITECTURE & BOUNDARIES)

### 4.1. Luồng phân tầng mục tiêu (Target Architecture)
```text
Frontend (HTML / CSS / Vanilla JS)
      │  HTTP Requests (JSON, Authorization: Bearer <JWT>)
      ▼
REST API Controllers (/api/v1/...)
      │  DTOs (Request validation @Valid, zero entity leak)
      ▼
Service Layer (Business Logic, Transactions @Transactional, Security)
      │  Entities / Domain Objects
      ▼
Repository Layer (Spring Data JPA)
      │  SQL Queries
      ▼
MySQL 8.4 LTS Database (elearning_db)
```

### 4.2. Ranh giới tuyệt đối giữa Flyway và Runtime CRUD
* **Flyway là cơ quan thẩm quyền duy nhất về Cấu trúc và Dữ liệu Nền tảng:**
  - Flyway chỉ quản lý: Schema DDL (`V1__init_schema.sql`), Migration cấu trúc, và Seed data hệ thống ban đầu (`V2__seed_roles.sql`, `V3__seed_radicals.sql`).
  - **TUYỆT ĐỐI CẤM:** Tạo Flyway migration script cho các thao tác thêm, sửa, xóa dữ liệu người dùng hay nội dung do Admin/Creator thực hiện tại runtime.
* **Service/Repository quản lý Runtime CRUD:**
  - Mọi thao tác thêm/sửa/xóa Bộ thủ, Từ vựng, Bài học của Admin/Creator thực hiện tại runtime thông qua `Service` $\rightarrow$ `Repository` $\rightarrow$ `MySQL`.
* **Hibernate hoàn toàn thụ động:** Cấu hình `spring.jpa.hibernate.ddl-auto: none`. Tuyệt đối không dùng `update`, `create`, `create-drop`.
* **Nguyên tắc DTO Boundary:** 100% request và response đi qua Controller phải dùng DTO. Tuyệt đối cấm trả trực tiếp JPA Entity ra API.

### 4.3. Bảng phân định Hiện trạng Thực tế vs Mục tiêu
| Thành phần | Hiện trạng thực tế trong Repository | Trạng thái |
| :--- | :--- | :--- |
| **Backend Project Scaffold** | File `backend/pom.xml`, cấu trúc thư mục Maven chuẩn | **ĐÃ HOÀN THÀNH (Phase 1)** |
| **Spring Boot Context & Startup** | `ElearningApplication.java`, chạy thành công | **ĐÃ HOÀN THÀNH (Phase 1)** |
| **Response Envelope & Error Handling** | `ApiResponse<T>`, `PageResponse<T>`, `ErrorCode`, `GlobalExceptionHandler` | **ĐÃ HOÀN THÀNH (Phase 1)** |
| **14 Bảng CSDL Vật lý** | Đã tồn tại thực tế 100% trong `elearning_db` | **ĐÃ HOÀN THÀNH (Phase 1)** |
| **JPA Entities & Mappings** | 12 Entities ánh xạ chính xác 14 bảng quan hệ | **ĐÃ HOÀN THÀNH (Phase 2)** |
| **Spring Data Repositories** | 12 JPA Repository interfaces | **ĐÃ HOÀN THÀNH (Phase 2)** |
| **Flyway Seed Data (V2, V3)** | 4 Roles và 214 Bộ thủ Khang Hy chuẩn | **ĐÃ HOÀN THÀNH (Phase 2)** |
| **Spring Security 6 & JWT** | FilterChain, BCrypt, JwtUtil, JwtAuthenticationFilter, CustomUserDetails | **ĐÃ HOÀN THÀNH (Phase 3)** |
| **Auth & Profile Endpoints** | `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/users/profile` | **ĐÃ HOÀN THÀNH (Phase 3)** |
| **RBAC Integration Verification** | 20 MockMvc tests kiểm chứng ma trận 4 vai trò | **ĐÃ HOÀN THÀNH (Phase 3)** |
| **Radical DTOs & Service** | `RadicalResponse`, `RadicalDetailResponse`, `RadicalService`, `RadicalServiceImpl` | **ĐÃ HOÀN THÀNH (Task 4A.1)** |
| **Radical Controller & Admin CRUD** | `RadicalController` (`/api/v1/radicals/**`, `/api/v1/admin/radicals/**`) | **NHIỆM VỤ TIẾP THEO (Task 4A.2)** |
| **Vocabulary Module** | DTOs, Service, Controller tìm kiếm từ vựng đa tiêu chí | **CHƯA BẮT ĐẦU (Module 4B)** |
| **Lesson Management & Excel Import**| Public Lesson, Creator Studio, POI Excel Import | **CHƯA BẮT ĐẦU (Phase 5)** |
| **Frontend Code** | Thư mục `frontend/` rỗng (chỉ có `.gitkeep`) | **CHƯA BẮT ĐẦU (Phase 9)** |

---

## 5. HIỆN TRẠNG CƠ SỞ DỮ LIỆU (DATABASE STATE)

### 5.1. Đặc tả 14 Bảng Nghiệp vụ Thẩm quyền
Hệ thống gồm đúng **14 bảng nghiệp vụ** (chi tiết tại `.agents/DATABASE.md` và `.agents/DATABASE_DESIGN.md`):
1. `ACCOUNT`: Tài khoản định danh đăng nhập (`email_or_phone`, `password_hash`, `status`).
2. `USER_PROFILE`: Hồ sơ cá nhân người dùng (`account_id` 1:1, `full_name`, `avatar_url`).
3. `ROLE`: 4 vai trò cố định (`1=Learner`, `2=Creator`, `3=Moderator`, `4=Admin`).
4. `ACCOUNT_ROLE`: Bảng liên kết trung gian N:N giữa tài khoản và vai trò (`account_id, role_id`).
5. `RADICAL`: 214 Bộ thủ Khang Hy chuẩn (`character`, `pinyin`, `meaning_han_viet`, `meaning_vi`, audio/video URLs).
6. `VOCABULARY`: Từ vựng tiếng Trung (`hanzi`, `pinyin`, `pinyin_raw`, `meaning_han_viet`, `meaning_vi`, câu ví dụ).
7. `VOCAB_RADICAL`: Bảng liên kết N:N phân rã từ vựng thành các bộ thủ cấu thành (`vocab_id, radical_id`).
8. `LESSON`: Bài học do Creator biên soạn (`title`, `excel_file_url`, `created_by`, `status`).
9. `LESSON_VOCABULARY`: Bảng liên kết N:N chứa danh sách từ vựng thuộc bài học kèm thứ tự (`order_index`).
10. `USER_SRS_SETTING`: Cấu hình học tập cá nhân (`user_id` 1:1, `new_cards_per_day`, `max_review_per_day`).
11. `CARD_PROGRESS`: Tiến trình ghi nhớ thẻ học theo SM-2 (`user_id`, `item_type`, `item_id`, `ease_factor`, `interval_days`, `repetitions`, `next_review_at`).
12. `REVIEW_LOG`: Nhật ký mỗi lần lật thẻ ôn tập (`user_id`, `item_type`, `item_id`, `rating`, `interval_before`, `interval_after`, `review_time_seconds`, `reviewed_at`).
13. `PERSONAL_NOTE`: Ghi chú riêng của học viên trên từng từ vựng (`user_id`, `vocab_id`, `content` $\le 500$ ký tự, `created_at`).
14. `MODERATION_LOG`: Lịch sử duyệt bài học của kiểm duyệt viên (`lesson_id`, `moderator_id`, `action`, `rejection_reason`, `flagged_fields`, `created_at`).

### 5.2. Hiện trạng Thực tế trong MySQL `elearning_db`
* **Số lượng bảng hiện có:** Đúng 15 bảng (14 bảng nghiệp vụ + 1 bảng hạ tầng `flyway_schema_history`).
* **Trạng thái Flyway:** Version `3` áp dụng thành công qua 3 file migration:
  - `V1__init_schema.sql`: Khởi tạo 14 bảng quan hệ.
  - `V2__seed_roles.sql`: Seed 4 vai trò cố định.
  - `V3__seed_radicals.sql`: Seed 214 Bộ thủ Khang Hy chuẩn.
* **Số lượng bản ghi CSDL nền tảng đã xác minh:**
  - `SELECT COUNT(*) FROM role;` $\rightarrow$ **4** bản ghi.
  - `SELECT COUNT(*) FROM radical;` $\rightarrow$ **214** bản ghi.
* **Kiểm chứng khóa ngoại và toàn vẹn tham chiếu:**
  - `MODERATION_LOG.lesson_id`: Tuân thủ nghiêm ngặt **`ON DELETE RESTRICT`** để bảo tồn lịch sử kiểm toán.
  - `LESSON.created_by`, `MODERATION_LOG.moderator_id`, `ACCOUNT_ROLE.role_id`, `VOCAB_RADICAL.radical_id`, `LESSON_VOCABULARY.vocab_id`: Đều là **`RESTRICT`**.
  - Các bảng quan hệ phụ thuộc chặt (`USER_PROFILE`, `CARD_PROGRESS`, `PERSONAL_NOTE`, v.v.): Đều là **`CASCADE`**.
  - Cột `review_time_seconds` trong bảng `REVIEW_LOG` tồn tại chính xác 100%.

---

## 6. QUYẾT ĐỊNH THIẾT KẾ CƠ SỞ DỮ LIỆU & CONTRACT CẦN LƯU Ý

1. **Chiến lược Tham chiếu Đa hình (`CARD_PROGRESS` và `REVIEW_LOG`):**
   * Sử dụng cặp trường: `item_type VARCHAR(20)` (`VOCABULARY` hoặc `RADICAL`) và `item_id BIGINT UNSIGNED`.
   * **Không tạo Foreign Key vật lý ở mức CSDL MySQL** cho `item_id` (đây là **Phương án A** đã được duyệt). Tầng Service trong Spring Boot chịu trách nhiệm kiểm tra toàn vẹn tham chiếu.
2. **Quy tắc Ghi chú Cá nhân (Personal Note):**
   * Giới hạn độ dài nội dung: `content <= 500 characters` (`VARCHAR(500)`).
   * **Bác bỏ hoàn toàn đề xuất giới hạn 5 ghi chú/từ vựng** (không được cài đặt ràng buộc CSDL hay code chặn 5 ghi chú).
3. **Audit Timestamps:**
   * 5 bảng thực thể chính (`ACCOUNT`, `USER_PROFILE`, `LESSON`, `RADICAL`, `VOCABULARY`) có cả `created_at` và `updated_at`.
   * Bảng log bất biến (`REVIEW_LOG`, `MODERATION_LOG`, `PERSONAL_NOTE`) chỉ có `created_at` hoặc `reviewed_at`.
4. **Quyết định Hợp đồng Radical DTOs (Correction Patch Task 4A.1):**
   * **Không có `radicalNumber` và `strokeCount`:** Cột vật lý trong bảng `RADICAL` chỉ có `radical_id, character, pinyin, meaning_han_viet, meaning_vi, audio_url, video_writing_url, created_at, updated_at`. DTO bám đúng schema thực tế, không tự thêm trường hay tính toán giả định.
   * **Không chứa danh sách Vocabulary trong `RadicalDetailResponse`:** Theo chuẩn `API.md`, Radical metadata độc lập hoàn toàn khỏi Vocabulary. Tra cứu từ vựng theo bộ thủ thuộc thẩm quyền của Module 4B (`VocabularyService`).

---

## 7. HIỆN TRẠNG TRIỂN KHAI MÃ NGUỒN (IMPLEMENTATION STATUS)

```text
================================================================
               BẢNG TỔNG HỢP HIỆN TRẠNG TRIỂN KHAI
================================================================
[x] Phase 0 — Project Specification & Physical Database Design  [COMPLETED]
[x] Phase 1 — Spring Boot Foundation & Web Infrastructure       [COMPLETED]
[x] Phase 2 — Persistence Layer & Database Seed Data            [COMPLETED]
[x] Phase 3 — Authentication, Security & RBAC                   [COMPLETED]
[~] Phase 4 — Radical & Vocabulary Catalog Domain               [IN_PROGRESS: Mod 4A Task 4A.1 COMPLETED, Task 4A.2 NEXT]
[ ] Phase 5 — Lesson Management & Excel Import                  [NOT_STARTED]
[ ] Phase 6 — Content Moderation Workflow                       [NOT_STARTED]
[ ] Phase 7 — Spaced Repetition System (SRS SM-2 Engine)        [NOT_STARTED]
[ ] Phase 8 — Personal Notes & User Settings                    [NOT_STARTED]
[ ] Phase 9 — Frontend UI & Client API Integration              [NOT_STARTED]
[ ] Phase 10 — Security, Performance & Quality Hardening        [NOT_STARTED]
[ ] Phase 11 — Final E2E Integration & Delivery                 [NOT_STARTED]
================================================================
```

### Chi tiết ĐÃ TRIỂN KHAI VÀ XÁC MINH (IMPLEMENTED):
* **Phase 0 [COMPLETED]:** Dọn dẹp legacy code, xác minh 19 Agent Skills (`validate_skills.ps1` PASS), bộ 9 tài liệu đặc tả chuẩn, cài đặt MySQL 8.4.9 LTS cổng 3306, thiết kế CSDL 14 bảng `DATABASE_DESIGN.md`.
* **Phase 1 [COMPLETED]:**
  * `Module 1A`: Spring Boot 3.3.5, Java 21 LTS, Flyway V1 14 bảng, `ElearningApplication`.
  * `Module 1B`: `ApiResponse<T>`, `PageResponse<T>`, `ErrorCode`, `BusinessException`, `GlobalExceptionHandler` (`ApiResponseTests` PASS 12/12, `GlobalExceptionHandlerTests` PASS 12/12).
* **Phase 2 [COMPLETED]:**
  * `Module 2A`: Entity & Repository cho `Account`, `UserProfile`, `Role`, `account_role` junction table (`IdentityPersistenceTests` PASS 5/5, `IdentityRepositoryTests` PASS 7/7).
  * `Module 2B`: Entity & Repository cho `Radical`, `Vocabulary`, `vocab_radical` junction table (`DictionaryPersistenceTests` PASS 5/5, `DictionaryRepositoryTests` PASS 8/8).
  * `Module 2C`: Entity & Repository cho `Lesson`, `LessonVocabulary` (`LessonPersistenceTests` PASS 5/5, `LessonRepositoryTests` PASS 7/7).
  * `Module 2D`: Entity & Repository cho `UserSrsSetting`, `PersonalNote`, `ModerationLog`, `CardProgress`, `ReviewLog` (`ProgressAuditPersistenceTests` PASS 7/7, `SrsProgressPersistenceTests` PASS 5/5, `SrsProgressRepositoryTests` PASS 7/7).
  * `Module 2E`: Flyway V2 seed 4 roles (`V2__seed_roles.sql`), Flyway V3 seed 214 radicals (`V3__seed_radicals.sql`).
  * `Module 2F`: Kiểm thử tích hợp `ddl-auto: validate` khớp 100% Flyway schema (Checkpoint Phase 2 PASS 81/81 tests).
* **Phase 3 [COMPLETED]:**
  * `Module 3A`: `SecurityConfig` (SecurityFilterChain, BCrypt, STATELESS, CSRF disable), `JwtUtil` (HMAC-SHA256, claims `sub`, `roles`, `iat`, `exp`), `JwtAuthenticationFilter` (OncePerRequestFilter, Bearer header), `CustomUserDetails` & `CustomUserDetailsService` (`ROLE_<RoleName>`, trạng thái Active/Inactive/Locked) (`JwtUtilTests` PASS 11/11, `JwtAuthenticationFilterTests` PASS 7/7, `CustomUserDetailsTests` PASS 11/11, `CustomUserDetailsServiceTests` PASS 6/6, `CustomUserDetailsServiceIntegrationTests` PASS 2/2).
  * `Module 3B`: `RegisterRequest`, `LoginRequest`, `AuthResponse` (Jakarta Validation, zero entity leak), `AuthService`, `AuthController` (`POST /api/v1/auth/register`, `POST /api/v1/auth/login`) (`AuthenticationDtoTests` PASS 14/14, `AuthServiceTests` PASS 6/6, `AuthControllerTests` PASS 6/6, `AuthIntegrationTests` PASS 1/1).
  * `Module 3C`: `UserProfileResponse`, `UpdateProfileRequest`, `UserProfileService`, `UserProfileController` (`GET /api/v1/users/profile`, `PUT /api/v1/users/profile`, ownership isolation, 401 unauthenticated) (`UserProfileServiceTests` PASS 5/5, `UserProfileControllerTests` PASS 3/3, `UserProfileIntegrationTests` PASS 2/2).
  * `Module 3D`: `RbacSecurityIntegrationTests` (20 tests kiểm chứng ma trận 4 vai trò Learner, Creator, Moderator, Admin; 401 unauthenticated, 403 forbidden, 200 OK với JWT thật). Checkpoint Phase 3 nghiệm thu đạt.
* **Phase 4 [IN_PROGRESS]:**
  * `Task 4A.1 [COMPLETED]`: `RadicalResponse`, `RadicalDetailResponse` (chứa radical metadata chuẩn, không có `radicalNumber`/`strokeCount`, không có vocabulary list), `RadicalService`, `RadicalServiceImpl` (`getAllRadicals(Pageable)`, `getAllRadicals()`, `getRadicalById(Integer)`, `getRadicalByCharacter(String)`), xử lý lỗi `BusinessException(ErrorCode.NOT_FOUND)`. `RadicalServiceTests` PASS 8/8 tests, `RadicalServiceIntegrationTests` PASS 6/6 tests (xác minh chính xác 214 bộ thủ Khang Hy trên MySQL thật).

### Chi tiết CHƯA TRIỂN KHAI (NOT IMPLEMENTED):
* `Task 4A.2 [NEXT / CURRENT]`: `RadicalController` công khai (`GET /api/v1/radicals/**`) và Admin endpoints (`POST/PUT/DELETE /api/v1/admin/radicals/**`).
* `Module 4B [NOT_STARTED]`: `Task 4B.1`, `Task 4B.2` (Vocabulary DTOs, Service tìm kiếm phân trang theo pinyin/hanzi, VocabularyController).
* `Module 4C [NOT_STARTED]`: `Task 4C.1` (Kiểm thử tích hợp Catalog Bộ thủ & Từ vựng).
* `Phase 5-8 [NOT_STARTED]`: Lesson, Moderation, SRS, Personal Notes.
* `Phase 9 [NOT_STARTED]`: Frontend HTML/CSS/JS (`frontend/` rỗng).
* `Phase 10-11 [NOT_STARTED]`: Hardening, Final Release.

---

## 8. GIAI ĐOẠN VÀ NHIỆM VỤ TIẾP THEO (CURRENT PHASE & NEXT TASK)

Dựa trên kết quả triển khai và nghiệm thu thành công `Task 4A.1`:
* **Giai đoạn hiện tại (Current Phase):** **`Phase 4 — Radical and Vocabulary Catalog Domain`**
* **Phân hệ hiện tại (Current Module):** **`Module 4A — Radical Catalog Vertical Slice`**
* **Nhiệm vụ vừa hoàn thành:** **`Task 4A.1 — Radical DTOs & RadicalService tra cứu Bộ thủ (COMPLETED)`**
* **Nhiệm vụ kế tiếp duy nhất (Current Next Task):** **`Task 4A.2 — RadicalController công khai & Admin CRUD Bộ thủ`**
* **Trạng thái:** **`NOT_STARTED`**
* **Vì sao đây là task tiếp theo duy nhất được chọn (Evidence-based Decision):**
  1. *Hoàn tất nền tảng Service Layer Bộ thủ:* `RadicalResponse`, `RadicalDetailResponse`, `RadicalService`, `RadicalServiceImpl` đã hoàn thiện và được kiểm chứng 100% qua cả Mockito unit tests và Spring Boot integration tests với CSDL MySQL thực tế (214 bộ thủ Khang Hy).
  2. *Tuân thủ lộ trình ROADMAP.md:* Theo đồ thị phụ thuộc (`depends_on: Task 1B.1, 1B.2, Task 4A.1, Mod 3A`), `Task 4A.2` là bước tiếp theo để công bố REST API controller công khai (`GET /api/v1/radicals`, `GET /api/v1/radicals/{id}`) và bảo vệ các thao tác CRUD quản trị của Admin (`POST/PUT/DELETE /api/v1/admin/radicals/**`) qua Spring Security.
  3. *Nhiệm vụ tiếp sau đó:* `Checkpoint 4A` $\rightarrow$ `Module 4B: Vocabulary Catalog & Search Vertical Slice`.

---

## 9. MÔ HÌNH QUẢN LÝ TIẾN ĐỘ CHUẨN HÓA (4-LEVEL PROGRESS MODEL)

Dự án áp dụng mô hình phân rã 4 cấp độ kỹ thuật:
$$\text{PHASE (Giai đoạn lớn)} \longrightarrow \text{MODULE (Phân hệ kỹ thuật)} \longrightarrow \text{TASK (Lát cắt thực thi)} \longrightarrow \text{VERIFICATION CHECKPOINT (Chốt chặn nghiệm thu)}$$

* Mỗi Task là một đơn vị công việc khép kín, có phạm vi rõ (In-scope / Out-of-Scope), dependency thực tế, và tiêu chí nghiệm thu kiểm chứng được.
* Mỗi Module có Verification Checkpoint riêng trước khi chuyển module.
* Mỗi Phase có Integration Verification Checkpoint trước khi nghiệm thu hoàn tất phase.
* Tuyệt đối không tạo các task vụn vặt cho việc sửa 1-2 dòng code hoặc sửa format tài liệu.

---

## 10. ĐẶC TẢ CHI TIẾT NHIỆM VỤ TIẾP THEO: TASK 4A.2

### 1. What (Làm gì)
Xây dựng `RadicalController` cung cấp:
- Các endpoint công khai (Public):
  - `GET /api/v1/radicals`: Danh sách 214 Bộ thủ có phân trang (`page`, `size`), trả về `ApiResponse<PageResponse<RadicalResponse>>`.
  - `GET /api/v1/radicals/{id}`: Chi tiết một bộ thủ, trả về `ApiResponse<RadicalDetailResponse>`.
- Các endpoint quản trị Admin (`/api/v1/admin/radicals/**`):
  - `POST /api/v1/admin/radicals`: Thêm bộ thủ mới (yêu cầu role `Admin`).
  - `PUT /api/v1/admin/radicals/{id}`: Cập nhật thông tin bộ thủ (yêu cầu role `Admin`).
  - `DELETE /api/v1/admin/radicals/{id}`: Xóa bộ thủ (yêu cầu role `Admin`).

### 2. Why (Tại sao cần)
Công bố giao diện REST API chính thức cho học viên tra cứu danh mục 214 bộ thủ Khang Hy và trao quyền cho quản trị viên quản lý dữ liệu danh mục gốc, đồng thời kích hoạt và kiểm chứng ranh giới phân quyền `ROLE_ADMIN` đã thiết lập ở Phase 3.

### 3. In-Scope (Phạm vi thực hiện)
* Tạo `RadicalController` (hoặc `AdminRadicalController` nếu tách theo phân hệ admin).
* Tạo DTOs request phục vụ Admin CRUD (`CreateRadicalRequest`, `UpdateRadicalRequest`) với Jakarta Validation.
* Bổ sung mutation methods trong `RadicalService` và `RadicalServiceImpl` phục vụ Admin CRUD (chạy qua JPA Repository, **tuyệt đối không sửa Flyway**).
* Viết WebMvc/MockMvc tests kiểm chứng:
  - Tra cứu công khai trả về HTTP 200 không cần JWT.
  - Gọi Admin endpoints không có token trả về 401 Unauthorized.
  - Gọi Admin endpoints với quyền `Learner` bị từ chối 403 Forbidden.
  - Gọi Admin endpoints với quyền `Admin` thành công (200 / 201).
* Nghiệm thu `Checkpoint 4A`.

### 4. Out-of-Scope (Tuyệt đối KHÔNG làm ở Task 4A.2)
* Không can thiệp sang phân hệ Từ vựng (thuộc Module 4B).
* Không sửa cấu trúc Flyway migration cũ (`V1`, `V2`, `V3`).
* Không can thiệp sang phân hệ Bài học (Phase 5).

### 5. Dependencies (Phụ thuộc)
* `depends_on`: `Task 1B.1`, `Task 1B.2`, `Task 4A.1`, `Mod 3A` (Security & RBAC).

### 6. Acceptance Criteria (Tiêu chí nghiệm thu)
* Public API trả về dữ liệu 214 bộ thủ chuẩn `ApiResponse<PageResponse<RadicalResponse>>`.
* Detail API trả về chi tiết bộ thủ `ApiResponse<RadicalDetailResponse>`.
* Phân quyền bảo vệ Admin API hoạt động chính xác (401/403/200).
* Nghiệm thu đạt `Checkpoint 4A`.
* Toàn bộ test suite tiếp tục PASS 100%.

### 7. Verification Command
* Lệnh chạy: `mvn -f backend/pom.xml clean test`

### 8. Completion Condition
* Toàn bộ các test cases công khai và bảo mật của `RadicalController` PASS.
* Nghiệm thu `Checkpoint 4A`.
* Cập nhật `Task 4A.2` thành `COMPLETED` trong `PROGRESS.md`.

### 9. Next Task
* `Module 4B — Vocabulary Catalog & Search Vertical Slice` (`Task 4B.1`).

---

## 11. CÁC CÂU HỎI MỞ DUY NHẤT CÒN LẠI (OPEN QUESTIONS)

* **OQ-08 (Lựa chọn thư viện giao diện tĩnh cho Phase 9):**
  * *Trạng thái:* **OPEN (Tạm hoãn tới Phase 9)**
  * *Nội dung:* Quyết định lựa chọn sử dụng CDN Bootstrap tối giản hay tái sử dụng bộ template giao diện tĩnh cũ cho Phase 9. Sẽ được User quyết định khi hoàn tất toàn bộ backend APIs.
* **Toàn bộ các câu hỏi từ OQ-01 đến OQ-07 đã được giải quyết dứt điểm** (đã ghi nhận trong `.agents/OPEN_QUESTIONS.md` và `.agents/DECISIONS.md`).

---

## 12. BẰNG CHỨNG XÁC MINH GẦN NHẤT (RECENT VERIFICATION EVIDENCE)

1. **Kết quả Maven Clean Test:**
   ```text
   [INFO] Tests run: 201, Failures: 0, Errors: 0, Skipped: 0
   [INFO] BUILD SUCCESS
   [INFO] Total time: 43.692 s
   ```
2. **Kiểm tra CSDL MySQL cục bộ (`elearning_db`):**
   * Số bảng: 15 (14 bảng nghiệp vụ + `flyway_schema_history` version 3).
   * Dữ liệu hạt giống: 4 roles, 214 radicals chuẩn Khang Hy.
   * Engine: `InnoDB`, Charset: `utf8mb4`, Collation: `utf8mb4_unicode_ci`.
   * Khóa ngoại `moderation_log.lesson_id`: `RESTRICT`.
   * Cột `review_time_seconds`: Tồn tại đúng chuẩn.
3. **Git Log gần nhất:**
   * Commit: `e8789f0` — `fix(catalog): resolve Task 4A.1 discrepancies per correction patch`
   * Commit trước: `01a02e4` — `feat(catalog): implement Task 4A.1 Radical DTOs & RadicalService`
   * Commit trước: `2e76d5c` — `docs: add authoritative project runbook .agents/RUNBOOK.md`
   * Trạng thái: `working tree clean`.

---

## 13. NGUYÊN TẮC BẮT BUỘC DÀNH CHO AI AGENT TIẾP QUẢN (RULES FOR NEW AI)

Bất kỳ AI Agent hoặc AI Model nào tiếp quản repo này cần tuân thủ 12 điều răn kỹ thuật:

1. **Đọc tài liệu này đầu tiên (`.agents/CURRENT_STATE.md`)** trước khi làm bất cứ hành động nào.
2. Đọc tiếp [`.agents/PROJECT_CONTEXT.md`](file:///.agents/PROJECT_CONTEXT.md), [`.agents/DECISIONS.md`](file:///.agents/DECISIONS.md), và [`.agents/OPEN_QUESTIONS.md`](file:///.agents/OPEN_QUESTIONS.md).
3. Đọc kỹ Skill tương ứng trong `.agents/skills/` trước khi lập kế hoạch hoặc code.
4. Kiểm tra cấu trúc file và git status thực tế trước khi sửa đổi.
5. Tuyệt đối tuân thủ đặc tả nền tảng 14 bảng và kiến trúc phân tầng chuẩn.
6. Tuyệt đối không dùng mã nguồn cũ đã xóa làm căn cứ tham chiếu.
7. Tuyệt đối không tự ý thay đổi kiến trúc hoặc đặc tả CSDL nếu không có sự ủy quyền của User.
8. Luôn luôn giữ Hibernate ở trạng thái thụ động (`ddl-auto: none`), để Flyway quản lý toàn bộ schema.
9. Đảm bảo 100% API đi qua DTO; cấm để lộ Entity ra ngoài Controller.
10. Chỉ đánh dấu một task là `COMPLETED` trong `.agents/PROGRESS.md` khi đã có bằng chứng nghiệm thu vật lý (Test pass, Build pass, Schema verified).
11. Giữ gìn sự trong sạch của Git: không commit mật khẩu, bí mật, `target/`, file rác.
12. Cập nhật tài liệu `CURRENT_STATE.md` này mỗi khi hoàn thành một task có ý nghĩa, một module hoặc chuyển phase.

---

## 14. QUY TRÌNH BÀN GIAO CHO AI MỚI (HANDOFF PROCEDURE)

Khi một Agent hoặc Model mới bắt đầu phiên làm việc:
1. **Không giả định có lịch sử hội thoại trước đó:** Mọi ngữ cảnh được cung cấp đầy đủ thông qua hệ thống tài liệu trong thư mục `.agents/`.
2. **Khởi động từ file này:** Đọc `.agents/CURRENT_STATE.md` để nắm ngay hiện trạng và nhiệm vụ tiếp theo (`Task 4A.2 — RadicalController công khai & Admin CRUD Bộ thủ`).
3. **Đối chiếu với thực tế:** Nếu phát hiện bất kỳ sự sai khác nào giữa tài liệu này và mã nguồn thực tế, AI phải báo cáo ngay sự sai khác cho User, không được tự ý ghi đè hay suy đoán lạc quan.

---

## 15. CHECKLIST TRẢ LỜI NHANH 11 CÂU HỎI CỐT LÕI

Một AI mới khi đọc xong tài liệu này có thể trả lời tức thì 11 câu hỏi trọng tâm:

1. **Dự án này là gì?**  
   $\rightarrow$ Hệ thống Website học 214 Bộ thủ và Từ vựng Tiếng Trung kết hợp thuật toán lặp lại ngắt quãng SM-2 (SRS), hỗ trợ nhập bài học từ Excel và quy trình kiểm duyệt bài học.
2. **Công nghệ nào đang được sử dụng?**  
   $\rightarrow$ Java 21 LTS (OpenJDK 21.0.12), Spring Boot 3.3.5, Apache Maven 3.9.16, MySQL Community Server 8.4.9 LTS, Flyway 10.x, Spring Data JPA / Hibernate 6.5.3, Spring Security 6, JJWT 0.12.6, BCrypt, HTML/CSS/JavaScript.
3. **Kiến trúc hệ thống là gì?**  
   $\rightarrow$ Kiến trúc phân tầng: `Client/Frontend -> REST API -> Controller -> Service -> Repository -> MySQL`. Flyway là thẩm quyền duy nhất quản lý schema. DTO phân tách hoàn toàn với Entity.
4. **Mô hình CSDL nào đã được phê duyệt?**  
   $\rightarrow$ Mô hình đúng 14 bảng quan hệ chuẩn hóa 3NF, bộ mã `utf8mb4` / `utf8mb4_unicode_ci`, khóa chính `BIGINT/INT UNSIGNED AUTO_INCREMENT`.
5. **Những gì ĐÃ THỰC SỰ được triển khai?**  
   $\rightarrow$ Khung Spring Boot, Flyway V1 (14 bảng), Flyway V2 (4 roles), Flyway V3 (214 radicals), Response Envelopes (`ApiResponse`, `PageResponse`, `ErrorCode`, `GlobalExceptionHandler`), 12 JPA Entities, 12 Repositories, Spring Security 6 Stateless JWT, Auth Service & Controller (`/api/v1/auth/**`), User Profile Service & Controller (`/api/v1/users/profile`), RBAC 4 vai trò (`RbacSecurityIntegrationTests`), và Radical Service & DTOs (`RadicalResponse`, `RadicalDetailResponse`, `RadicalService`, `RadicalServiceImpl`).
6. **Những gì ĐÃ ĐƯỢC XÁC MINH?**  
   $\rightarrow$ `mvn clean test` PASS 201/201 tests (0 failures, 0 errors), CSDL `elearning_db` tạo đủ đúng 14 bảng nghiệp vụ, nạp đủ 4 roles và 214 bộ thủ Khang Hy, xác minh bảo mật 401/403/200 OK trên MockMvc.
7. **Những gì CHƯA ĐƯỢC triển khai?**  
   $\rightarrow$ `RadicalController` (Task 4A.2), Vocabulary DTOs/Service/Controller (Module 4B), Bài học (Phase 5), Duyệt bài (Phase 6), SRS Engine (Phase 7), Ghi chú (Phase 8), Giao diện Frontend (Phase 9).
8. **Dự án đang ở Phase nào?**  
   $\rightarrow$ Đang ở **Phase 4 — Radical and Vocabulary Catalog Domain** (Module 4A: Task 4A.1 COMPLETED, Task 4A.2 NEXT / IN PROGRESS).
9. **Task nào là Task tiếp theo cần làm?**  
   $\rightarrow$ **`Task 4A.2 — RadicalController công khai & Admin CRUD Bộ thủ`**.
10. **Ràng buộc nào TUYỆT ĐỐI KHÔNG ĐƯỢC VI PHẠM?**  
    $\rightarrow$ Không dùng lại mã nguồn cũ; không để Hibernate tự sửa schema (`ddl-auto: none`); không sửa các migration V1-V3; không trả Entity ra API; không đổi tên cột `review_time_seconds`; không giới hạn 5 notes/vocab; không tạo FK MySQL cho tham chiếu đa hình `item_type + item_id`; không thêm `radicalNumber`/`strokeCount` vào DTO; không can thiệp sang Vocabulary ở Module 4A.
11. **Câu hỏi nào còn mở (Open Questions)?**  
    $\rightarrow$ Duy nhất câu hỏi **OQ-08** về việc lựa chọn thư viện tĩnh cho Frontend tại Phase 9.

---

## 16. LƯU TRỮ LỊCH SỬ THIẾT LẬP BAN ĐẦU (HISTORICAL ARCHIVE — PHASE 1 INITIAL SNAPSHOT)

> [!NOTE]
> **MỤC ĐÍCH LƯU TRỮ (FOR HISTORICAL RECORD ONLY):**  
> Phần dưới đây bảo lưu nguyên văn snapshot bàn giao tại thời điểm vừa hoàn thành Phase 1 (Commit `5d59ff4`), chỉ để theo dõi vết lịch sử phát triển ban đầu của dự án. **KHÔNG ĐƯỢC COI NỘI DUNG DƯỚI ĐÂY LÀ HIỆN TRẠNG DỰ ÁN.** Hiện trạng thực tế chính thức luôn tuân theo các Mục 0 đến 15 ở trên.

<details>
<summary><b>Nhấn để xem lại Snapshot lịch sử Phase 1 (Initial Foundation)</b></summary>

```text
Snapshot Commit: 5d59ff497e2d416618ea774e98354c5c27b310ac
Status tại thời điểm đó:
- Phase 1 hoàn tất (Scaffold Spring Boot 3.3.5, application.yml, Flyway V1 14 tables, ElearningApplicationTests 1/1 pass).
- Tầng Persistence, Security, Controller chưa bắt đầu.
- Nhiệm vụ tiếp theo tại thời điểm đó: Task 1B.1 (ApiResponse, PageResponse, ErrorCode).
```

</details>
