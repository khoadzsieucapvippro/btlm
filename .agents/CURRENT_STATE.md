# CURRENT PROJECT STATE & AI HANDOFF DOCUMENT

> **Tài liệu:** Bàn giao Hiện trạng Thực tế Dự án (Primary AI Handoff & Project State Document)  
> **Dự án:** Hệ thống Website học Bộ thủ và Từ vựng Tiếng Trung (E-learning Chinese Radicals & Vocabulary)  
> **Vị trí file:** `.agents/CURRENT_STATE.md`  
> **Cam kết tính xác thực:** Mô tả **HIỆN TRẠNG THỰC TẾ (REAL ACTUAL STATE)** của mã nguồn, CSDL và cấu hình trong repository; KHÔNG phản ánh hiện trạng mong muốn (intended state) hay báo cáo lạc quan.  
> **Phiên bản cập nhật:** Sau khi hoàn thành Phase 1 (Spring Boot Foundation + MySQL + Flyway Schema).  
> **Commit hash hiện tại:** `5d59ff497e2d416618ea774e98354c5c27b310ac` (`5d59ff4`)  

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
  1. *Authentication & User Profile*: Đăng ký, đăng nhập, JWT stateless, hồ sơ cá nhân.
  2. *Curriculum & Dictionary*: 214 Bộ thủ Khang Hy, từ vựng (chữ Hán, Pinyin có dấu/không dấu, Hán-Việt, dịch nghĩa, media URLs), bài học và phân thứ tự từ vựng.
  3. *Moderation*: Quy trình kiểm duyệt bài học và lưu vết `MODERATION_LOG`.
  4. *Spaced Repetition System (SRS)*: Thuật toán SM-2, quản lý tiến trình thẻ (`CARD_PROGRESS`), nhật ký ôn tập (`REVIEW_LOG`).
  5. *Personalization*: Ghi chú từ vựng cá nhân (`PERSONAL_NOTE` $\le 500$ ký tự) và cài đặt SRS (`USER_SRS_SETTING`).

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
| **Cơ chế Migration** | Flyway | **Flyway 10.x** (`flyway-core` + `flyway-mysql`) | Là **CƠ QUAN THẨM QUYỀN DUY NHẤT** quản lý Database Schema |
| **Tầng Persistence** | Spring Data JPA / Hibernate | **Hibernate 6.5.3.Final** | Cấu hình `spring.jpa.hibernate.ddl-auto: none` (Cấm Hibernate tự sửa schema) |
| **Bảo mật (Kế hoạch)** | Spring Security & JJWT | Chưa triển khai code | Sẽ triển khai tại Phase 3 (JWT Stateless, BCrypt) |
| **Frontend (Kế hoạch)** | HTML, CSS, JavaScript | Chưa triển khai code | Web tiêu chuẩn, `frontend/` hiện là thư mục rỗng có `.gitkeep` |
| **Quản lý phiên bản** | Git | Git cục bộ nhánh `main` | Đã cấu hình `.gitignore`, initial commit + commit Phase 1 |

---

## 4. KIẾN TRÚC HỆ THỐNG (ARCHITECTURE)

### 4.1. Luồng phân tầng mục tiêu (Target Architecture)
```text
Frontend (HTML / CSS / Vanilla JS)
      │  HTTP Requests (JSON, Authorization: Bearer <JWT>)
      ▼
REST API Controllers (/api/v1/...)
      │  DTOs (Request validation @Valid)
      ▼
Service Layer (Business Logic, Transactions @Transactional, Security)
      │  Entities / Domain Objects
      ▼
Repository Layer (Spring Data JPA)
      │  SQL Queries
      ▼
MySQL 8.4 LTS Database (elearning_db)
```

### 4.2. Thẩm quyền quản trị Schema CSDL
* **Flyway là cơ quan thẩm quyền duy nhất:** Mọi thay đổi bảng, cột, khóa ngoại, chỉ mục phải thực hiện qua script `src/main/resources/db/migration/V{X}__{description}.sql`.
* **Hibernate hoàn toàn thụ động:** Cấu hình `spring.jpa.hibernate.ddl-auto: none`. Tuyệt đối không dùng `update`, `create`, `create-drop`.
* **Nguyên tắc DTO Boundary:** 100% request và response đi qua Controller phải dùng DTO. Tuyệt đối cấm trả trực tiếp JPA Entity ra API.

### 4.3. Bảng phân định Hiện trạng Thực tế vs Mục tiêu
| Thành phần | Hiện trạng thực tế trong Repository | Trạng thái |
| :--- | :--- | :--- |
| **Backend Project Scaffold** | File `backend/pom.xml`, cấu trúc thư mục Maven chuẩn | **ĐÃ HOÀN THÀNH** |
| **Spring Boot Context & Startup** | `ElearningApplication.java`, chạy thành công | **ĐÃ HOÀN THÀNH** |
| **Database Connection & Pool** | HikariCP kết nối MySQL cổng 3306, `elearning_db` | **ĐÃ HOÀN THÀNH** |
| **Flyway Schema Migration** | Script `V1__init_schema.sql` đã apply thành công | **ĐÃ HOÀN THÀNH** |
| **14 Bảng CSDL Vật lý** | Đã tồn tại thực tế 100% trong `elearning_db` | **ĐÃ HOÀN THÀNH** |
| **JPA Entities & Mappings** | Chưa tạo bất kỳ file Entity nào | **CHƯA BẮT ĐẦU (Phase 2)** |
| **Spring Data Repositories** | Chưa tạo bất kỳ repository interface nào | **CHƯA BẮT ĐẦU (Phase 2)** |
| **Flyway Seed Data (V2, V3)** | Chưa tạo `V2__seed_roles.sql` và `V3__seed_radicals.sql` | **CHƯA BẮT ĐẦU (Phase 2)** |
| **Spring Security & JWT** | Chưa thêm dependency security, chưa viết filter | **CHƯA BẮT ĐẦU (Phase 3)** |
| **Controllers / Services / DTOs** | Chưa tạo bất kỳ controller hay service nào | **CHƯA BẮT ĐẦU (Phase 3-8)** |
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
* **Trạng thái Flyway:** Version `1` áp dụng thành công qua file `V1__init_schema.sql` (execution time: 527ms).
* **Kiểm chứng khóa chính:**
  * Bảng đơn: Khóa chính đơn (`account_id`, `user_id`, `role_id`, `radical_id`, `vocab_id`, `lesson_id`, `setting_id`, `progress_id`, `log_id`, `note_id`).
  * `USER_SRS_SETTING.setting_id`: Là Primary Key duy nhất (đã sửa triệt để lỗi khai báo lặp).
  * Bảng liên kết: Khóa chính phức hợp (`account_id, role_id`), (`vocab_id, radical_id`), (`lesson_id, vocab_id`).
* **Kiểm chứng quy tắc xóa khóa ngoại (ON DELETE):**
  * `MODERATION_LOG.lesson_id`: Tuân thủ nghiêm ngặt **`ON DELETE RESTRICT`** để bảo tồn lịch sử kiểm toán.
  * `LESSON.created_by`, `MODERATION_LOG.moderator_id`, `ACCOUNT_ROLE.role_id`, `VOCAB_RADICAL.radical_id`, `LESSON_VOCABULARY.vocab_id`: Đều là **`RESTRICT`**.
  * Các bảng quan hệ phụ thuộc chặt (`USER_PROFILE`, `CARD_PROGRESS`, `PERSONAL_NOTE`, v.v.): Đều là **`CASCADE`**.
* **Kiểm chứng tên cột đặc thù:**
  * Cột `review_time_seconds` trong bảng `REVIEW_LOG` tồn tại chính xác 100%.

---

## 6. QUYẾT ĐỊNH THIẾT KẾ CƠ SỞ DỮ LIỆU CẦN LƯU Ý

1. **Chiến lược Tham chiếu Đa hình (`CARD_PROGRESS` và `REVIEW_LOG`):**
   * Sử dụng cặp trường: `item_type VARCHAR(20)` (`VOCABULARY` hoặc `RADICAL`) và `item_id BIGINT UNSIGNED`.
   * **Không tạo Foreign Key vật lý ở mức CSDL MySQL** cho `item_id` (đây là **Phương án A** đã được duyệt).
   * **Bắt buộc:** Tầng Service trong Spring Boot chịu trách nhiệm kiểm tra toàn vẹn tham chiếu (kiểm tra ID tồn tại trong bảng tương ứng trước khi thêm/sửa tiến trình học).
2. **Quy tắc Ghi chú Cá nhân (Personal Note):**
   * Giới hạn độ dài nội dung: `content <= 500 characters` (`VARCHAR(500)`).
   * **Bác bỏ hoàn toàn đề xuất giới hạn 5 ghi chú/từ vựng** (User đã khẳng định đề xuất này không hợp lệ, không được cài đặt ràng buộc CSDL hay code chặn 5 ghi chú).
3. **Audit Timestamps:**
   * 5 bảng thực thể chính (`ACCOUNT`, `USER_PROFILE`, `LESSON`, `RADICAL`, `VOCABULARY`) có cả `created_at` và `updated_at`.
   * Bảng log bất biến (`REVIEW_LOG`, `MODERATION_LOG`, `PERSONAL_NOTE`) chỉ có `created_at` hoặc `reviewed_at`.

---

## 7. HIỆN TRẠNG TRIỂN KHAI MÃ NGUỒN (IMPLEMENTATION STATUS)

```text
================================================================
               BẢNG TỔNG HỢP HIỆN TRẠNG TRIỂN KHAI
================================================================
[x] Phase 0 — Project Specification & Physical Database Design  [COMPLETED]
[x] Phase 1 — Spring Boot Foundation & Web Infrastructure       [COMPLETED]
[~] Phase 2 — Persistence Layer & Database Seed Data            [IN_PROGRESS: Mod 2A In Progress, Mod 2B-2E Pending]
[ ] Phase 3 — Authentication, Security & RBAC                   [NOT_STARTED]
[ ] Phase 4 — Radical & Vocabulary Catalog Domain               [NOT_STARTED]
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
* `Module 1A [COMPLETED]`:
  * `backend/pom.xml`: Khởi tạo thành công với Spring Boot 3.3.5, Java 21 LTS, dependencies: Web, JPA, MySQL Connector, Flyway, Validation, Test.
  * `backend/src/main/resources/application.yml`: Cấu hình kết nối MySQL `elearning_db`, kích hoạt Flyway, đặt Hibernate `ddl-auto: none`.
  * `backend/src/main/resources/db/migration/V1__init_schema.sql`: Khởi tạo trọn vẹn 14 bảng quan hệ, được Flyway áp dụng thành công.
  * `backend/src/main/java/com/elearning/ElearningApplication.java`: Class khởi động chuẩn của ứng dụng.
  * `backend/src/test/java/com/elearning/ElearningApplicationTests.java`: Test khởi động context, kích hoạt migration tự động thành công (1 test, 0 failure).
  * `.gitignore`: Đã hiệu chỉnh loại trừ đúng `target/`, các file IDE, secrets, và đưa thư mục `db/migration` vào Git an toàn.
* `Module 1B [COMPLETED]`:
  * `backend/src/main/java/com/elearning/dto/response/ApiResponse.java`: Phong bì JSON chuẩn theo `API.md` Mục 1.2 (`code`, `message`, `errors`, `data`), 4 factory methods.
  * `backend/src/main/java/com/elearning/dto/response/PageResponse.java`: Mô hình phân trang chuẩn theo `API.md` Mục 1.3 (`page`, `size`, `totalElements`, `totalPages`, `items`), factory method chuyển đổi từ Spring Data `Page<T>`.
  * `backend/src/main/java/com/elearning/common/ErrorCode.java`: Enum 12 mã lỗi chuẩn theo `API.md` Mục 1.4 (`code`, `defaultMessage`, `httpStatus`).
  * `backend/src/main/java/com/elearning/exception/BusinessException.java`: Custom runtime exception hỗ trợ dynamic `ErrorCode` và custom message.
  * `backend/src/main/java/com/elearning/exception/GlobalExceptionHandler.java`: Lớp `@RestControllerAdvice` tập trung bắt và chuẩn hóa `MethodArgumentNotValidException` (400), `BusinessException` (dynamic status/code), fallback `Exception` (500).
  * `backend/src/test/java/com/elearning/ApiResponseTests.java`: 12 unit tests kiểm chứng serialization Jackson, mapping Spring Data Page, và ErrorCode mapping PASS 12/12 tests.
  * `backend/src/test/java/com/elearning/GlobalExceptionHandlerTests.java`: 12 unit & slice tests kiểm chứng validation errors, dynamic business exceptions, generic fallback, và MockMvc precedence PASS 12/12 tests.
* `Module 2A [COMPLETED]`:
  * `backend/src/main/java/com/elearning/entity/Account.java`: JPA Entity cho bảng `ACCOUNT` (BIGINT AUTO_INCREMENT, `email_or_phone` UNIQUE, `password_hash`, `status`, timestamps, 1:1 `UserProfile`, N:N `@JoinTable(name = "account_role")` với `Role`).
  * `backend/src/main/java/com/elearning/entity/UserProfile.java`: JPA Entity cho bảng `USER_PROFILE` (BIGINT AUTO_INCREMENT, owning side 1:1 `Account` qua `@JoinColumn(name = "account_id")`, `full_name`, `avatar_url`, timestamps).
  * `backend/src/main/java/com/elearning/entity/Role.java`: JPA Entity cho bảng `ROLE` (INT `role_id`, `role_name` UNIQUE).
  * `backend/src/main/java/com/elearning/repository/AccountRepository.java`: Spring Data JPA Repository cho `Account`, hỗ trợ `findByEmailOrPhone` và `existsByEmailOrPhone`.
  * `backend/src/main/java/com/elearning/repository/UserProfileRepository.java`: Spring Data JPA Repository cho `UserProfile`, hỗ trợ `findByAccount`.
  * `backend/src/main/java/com/elearning/repository/RoleRepository.java`: Spring Data JPA Repository cho `Role`, hỗ trợ `findByRoleName`.
  * `backend/src/test/java/com/elearning/IdentityPersistenceTests.java`: 5 tests kiểm chứng mapping với Hibernate `ddl-auto=validate`, association 1:1, N:N junction table `account_role`, composite PK semantics, cascade delete PASS 5/5 tests.
  * `backend/src/test/java/com/elearning/IdentityRepositoryTests.java`: 7 tests kiểm chứng Spring Data JPA proxy, truy vấn tìm kiếm `email_or_phone`, `role_name`, `account` profile PASS 7/7 tests.
* `Module 2B [COMPLETED]`:
  * `backend/src/main/java/com/elearning/entity/Radical.java`: JPA Entity cho bảng `RADICAL` (INT AUTO_INCREMENT, `character` UNIQUE, `pinyin`, `meaning_han_viet`, `meaning_vi`, timestamps, N:N `vocabularies`).
  * `backend/src/main/java/com/elearning/entity/Vocabulary.java`: JPA Entity cho bảng `VOCABULARY` (BIGINT AUTO_INCREMENT, `hanzi`, `pinyin`, `pinyin_raw` lưu độc lập, `meaning_han_viet`, `meaning_vi`, timestamps, N:N `@JoinTable(name = "vocab_radical")` với `radicals`).
  * `backend/src/main/java/com/elearning/repository/RadicalRepository.java`: Spring Data JPA Repository cho `Radical`, hỗ trợ `findByCharacter` và `existsByCharacter`.
  * `backend/src/main/java/com/elearning/repository/VocabularyRepository.java`: Spring Data JPA Repository cho `Vocabulary`, hỗ trợ `findByHanziAndPinyinRaw`, `findByHanzi`, `findByPinyinRaw`, `searchByKeyword` kèm phân trang `Pageable`.
  * `backend/src/test/java/com/elearning/DictionaryPersistenceTests.java`: 5 tests kiểm chứng mapping với Hibernate `ddl-auto=validate`, association 2 chiều, composite PK junction table, và Set deduplication PASS 5/5 tests.
  * `backend/src/test/java/com/elearning/DictionaryRepositoryTests.java`: 8 tests kiểm chứng Spring Data JPA proxy, truy vấn tìm kiếm `character`, `hanzi`, `pinyin_raw`, tìm kiếm kết hợp đa tiêu chí, phân trang metadata PASS 8/8 tests.
* `Module 2C [IN_PROGRESS: Task 2C.1 COMPLETED, Task 2C.2 NOT_STARTED]`:
  * `backend/src/main/java/com/elearning/entity/Lesson.java`: JPA Entity cho bảng `LESSON` (BIGINT AUTO_INCREMENT, `title`, `excel_file_url`, `created_by` liên kết `@ManyToOne` với `Account`, `status`, timestamps, `@OneToMany` `lessonVocabularies` với `@OrderBy("orderIndex ASC")`).
  * `backend/src/main/java/com/elearning/entity/LessonVocabularyId.java`: Composite ID `@Embeddable` cho `(lesson_id, vocab_id)`.
  * `backend/src/main/java/com/elearning/entity/LessonVocabulary.java`: JPA Entity cho bảng `LESSON_VOCABULARY` (`@EmbeddedId` kèm `@MapsId("lessonId")` và `@MapsId("vocabId")`, `order_index`).
  * `backend/src/test/java/com/elearning/LessonPersistenceTests.java`: 5 tests kiểm chứng mapping với Hibernate `ddl-auto=validate`, foreign key `created_by`, composite PK semantics, sắp xếp `@OrderBy("orderIndex ASC")`, shared vocab không trùng lặp, và cascade delete bảo toàn `Account`/`Vocabulary` PASS 5/5 tests.

### Chi tiết CHƯA TRIỂN KHAI (NOT IMPLEMENTED):
* `Module 2C [IN_PROGRESS]`: Chưa tạo Spring Data JPA Repositories (`LessonRepository`, `LessonVocabularyRepository`) tại `Task 2C.2`.
* `Module 2D-2E [NOT_STARTED]`: Chưa viết JPA Entities/Repositories cho SRS, Notes, Logs, và chưa tạo seed data `V2`/`V3`.
* `Phase 3-8 [NOT_STARTED]`: Chưa có bất kỳ Service, Controller hay Security/JWT configuration nào.
* `Phase 9 [NOT_STARTED]`: Chưa có mã nguồn giao diện HTML/CSS/JS nào trong `frontend/`.

---

## 8. GIAI ĐOẠN VÀ NHIỆM VỤ TIẾP THEO (CURRENT PHASE & NEXT TASK)

Dựa trên kết quả triển khai và nghiệm thu thành công `Task 2C.1`:
* **Giai đoạn hiện tại (Current Phase):** **`Phase 2 — Persistence Layer & Database Seed Data`**
* **Phân hệ hiện tại (Current Module):** **`Module 2C — Lesson & Content Persistence Mapping`**
* **Nhiệm vụ kế tiếp duy nhất (Current Next Task):** **`Task 2C.2 — Spring Data JPA Repositories Cụm Bài học (LessonRepository, LessonVocabularyRepository)`**
* **Trạng thái:** **`NOT_STARTED`**
* **Vì sao đây là task tiếp theo duy nhất được chọn (Evidence-based Decision):**
  1. *Tuân thủ thứ tự phân rã Module 2C:* `Task 2C.1` đã cung cấp đầy đủ các lớp Entity Cụm Bài học (`Lesson`, `LessonVocabulary`, `LessonVocabularyId`). `Task 2C.2` là bước tự nhiên tiếp theo xây dựng tầng DAO/Repository interface kế thừa `JpaRepository` cho các entity này.
  2. *Cung cấp hạ tầng truy vấn cho Phase 5 & Phase 6:* `LessonRepository` (lọc theo tác giả `created_by`, lọc theo trạng thái `status` với phân trang `Pageable`) và `LessonVocabularyRepository` là thành phần phụ thuộc cốt lõi cho các tính năng quản lý bài học, duyệt bài và học tập.
  3. *Ngữ cảnh rõ ràng, độc lập:* Chỉ bao gồm repository interfaces và repository slice tests, không kéo logic Service hay Web Controller vào.
  4. *Nhiệm vụ tiếp sau đó:* Nghiệm thu Checkpoint 2C $\rightarrow$ Hoàn thành Module 2C $\rightarrow$ Bắt đầu `Module 2D` (`Task 2D.1 — JPA Entity Mapping: USER_SRS_SETTING, PERSONAL_NOTE, MODERATION_LOG`).

---

## 9. MÔ HÌNH QUẢN LÝ TIẾN ĐỘ CHUẨN HÓA (4-LEVEL PROGRESS MODEL)

Dự án áp dụng mô hình phân rã 4 cấp độ kỹ thuật:
$$\text{PHASE (Giai đoạn lớn)} \longrightarrow \text{MODULE (Phân hệ kỹ thuật)} \longrightarrow \text{TASK (Lát cắt thực thi)} \longrightarrow \text{VERIFICATION CHECKPOINT (Chốt chặn nghiệm thu)}$$

* Mỗi Task là một đơn vị công việc khép kín, có phạm vi rõ (In-scope / Out-of-scope), dependency thực tế, và tiêu chí nghiệm thu kiểm chứng được.
* Mỗi Module có Verification Checkpoint riêng trước khi chuyển module.
* Mỗi Phase có Integration Verification Checkpoint trước khi nghiệm thu hoàn tất phase.
* Tuyệt đối không tạo các task vụn vặt cho việc sửa 1-2 dòng code hoặc sửa format tài liệu.

---

## 10. ĐẶC TẢ CHI TIẾT NHIỆM VỤ TIẾP THEO: TASK 2C.2

### 1. What (Làm gì)
Xây dựng các interface Spring Data JPA Repository cho Cụm Bài học: `LessonRepository` và `LessonVocabularyRepository` kèm các derived query methods / pagination cần thiết và bộ kiểm thử repository tương ứng.

### 2. Why (Tại sao cần)
Để cung cấp tầng truy xuất dữ liệu an toàn kiểu (type-safe data access) cho Cụm Bài học, hỗ trợ tìm kiếm bài học theo tác giả `created_by`, lọc bài học theo trạng thái `status` (`Draft`, `Pending`, `Approved`, `Rejected`), và truy xuất danh sách từ vựng của bài học theo đúng thứ tự `order_index`.

### 3. In-Scope (Phạm vi thực hiện)
* Tạo các repository interfaces trong package `com.elearning.repository`:
  * `LessonRepository`: Kế thừa `JpaRepository<Lesson, Long>`, có các phương thức:
    * `Page<Lesson> findByCreatedBy(Account createdBy, Pageable pageable)`.
    * `Page<Lesson> findByStatus(String status, Pageable pageable)`.
    * `Page<Lesson> findByCreatedByAndStatus(Account createdBy, String status, Pageable pageable)`.
  * `LessonVocabularyRepository`: Kế thừa `JpaRepository<LessonVocabulary, LessonVocabularyId>`, có các phương thức:
    * `List<LessonVocabulary> findByLesson_LessonIdOrderByOrderIndexAsc(Long lessonId)`.
    * `boolean existsByLesson_LessonIdAndVocabulary_VocabId(Long lessonId, Long vocabId)`.
* Viết test `LessonRepositoryTests.java` kiểm chứng các derived queries và phân trang trên MySQL.

### 4. Out-of-Scope (Tuyệt đối KHÔNG làm ở Task 2C.2)
* Không viết Service logic, Controller, DTOs, Excel import hay Moderation logic.
* Không viết Repositories của Cụm SRS hay Ghi chú (`Module 2D`).
* Không sửa đổi schema database hay Flyway migrations.

### 5. Dependencies (Phụ thuộc)
* `depends_on`: `Task 2C.1` (ĐÃ HOÀN THÀNH — cung cấp `Lesson`, `LessonVocabulary`), `Task 2A.2` (`AccountRepository` cho test fixture), `Task 2B.2` (`VocabularyRepository` cho test fixture).

### 6. Files/Modules Likely Affected
* `backend/src/main/java/com/elearning/repository/LessonRepository.java` [NEW]
* `backend/src/main/java/com/elearning/repository/LessonVocabularyRepository.java` [NEW]
* `backend/src/test/java/com/elearning/LessonRepositoryTests.java` [NEW]

### 7. Acceptance Criteria (Tiêu chí nghiệm thu)
* `Given` các bài học đã lưu của nhiều tác giả, `When` gọi `findByCreatedBy`, `Then` trả về đúng danh sách bài học của tác giả đó có phân trang.
* `Given` các bài học với status khác nhau, `When` gọi `findByStatus("Approved", pageable)`, `Then` chỉ trả về các bài học đã duyệt.
* `Given` một bài học có nhiều từ vựng, `When` gọi `findByLesson_LessonIdOrderByOrderIndexAsc`, `Then` trả về danh sách từ vựng được sắp xếp chuẩn theo `order_index ASC`.

### 8. Verification Command
* Lệnh chạy: `mvn -f backend/pom.xml test -Dtest=LessonRepositoryTests`
* Kiểm thử hồi quy: `mvn -f backend/pom.xml clean test`

### 9. Completion Condition
* Toàn bộ repository queries chạy thành công, không phát sinh lỗi.
* `mvn clean test` tiếp tục PASS 100%.
* Cập nhật `Task 2C.2` thành `COMPLETED` trong `PROGRESS.md`.

### 10. Next Task
* `Task 2D.1 — JPA Entity Mapping: USER_SRS_SETTING, PERSONAL_NOTE, MODERATION_LOG`.

---

## 11. CÁC CÂU HỎI MỞ DUY NHẤT CÒN LẠI (OPEN QUESTIONS)

* **OQ-08 (Lựa chọn thư viện giao diện tĩnh cho Phase 9):**
  * *Trạng thái:* **OPEN (Tạm hoãn tới Phase 9)**
  * *Nội dung:* Quyết định lựa chọn sử dụng CDN Bootstrap tối giản hay tái sử dụng bộ template giao diện tĩnh cũ cho Phase 9. Sẽ được User quyết định khi hoàn tất toàn bộ backend APIs.
* **Toàn bộ các câu hỏi từ OQ-01 đến OQ-07 đã được giải quyết dứt điểm** (đã ghi nhận trong `.agents/OPEN_QUESTIONS.md` và `.agents/DECISIONS.md`).

---

## 12. BẰNG CHỨNG XÁC MINH GẦN NHẤT (RECENT VERIFICATION EVIDENCE)

1. **Kết quả Maven Test:**
   ```text
   [INFO] Running com.elearning.ElearningApplicationTests
   [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 6.473 s
   [INFO] BUILD SUCCESS
   ```
2. **Kết quả Maven Package:**
   ```text
   [INFO] Building jar: ...\backend\target\elearning-backend-1.0.0.jar
   [INFO] BUILD SUCCESS
   ```
3. **Kết quả Flyway Migration:**
   ```text
   [INFO] Current version of schema `elearning_db`: << Empty Schema >>
   [INFO] Migrating schema `elearning_db` to version "1 - init schema"
   [INFO] Successfully applied 1 migration to schema `elearning_db`, now at version v1 (execution time 00:00.527s)
   ```
4. **Kiểm tra CSDL MySQL cục bộ (`elearning_db`):**
   * Số bảng: 15 (14 bảng nghiệp vụ + `flyway_schema_history`).
   * Engine: `InnoDB`, Charset: `utf8mb4`, Collation: `utf8mb4_unicode_ci`.
   * Khóa ngoại `moderation_log.lesson_id`: `RESTRICT`.
   * Cột `review_time_seconds`: Tồn tại đúng chuẩn.
5. **Git Log gần nhất:**
   * Commit: `5d59ff497e2d416618ea774e98354c5c27b310ac`
   * Message: `feat(phase-1): establish Spring Boot foundation with Flyway V1 14-table schema`
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
2. **Khởi động từ file này:** Đọc `.agents/CURRENT_STATE.md` để nắm ngay hiện trạng và nhiệm vụ tiếp theo (`Task 1B.1`).
3. **Đối chiếu với thực tế:** Nếu phát hiện bất kỳ sự sai khác nào giữa tài liệu này và mã nguồn thực tế, AI phải báo cáo ngay sự sai khác cho User, không được tự ý ghi đè hay suy đoán lạc quan.

---

## 15. CHECKLIST TRẢ LỜI NHANH 11 CÂU HỎI CỐT LÕI

Một AI mới khi đọc xong tài liệu này có thể trả lời tức thì 11 câu hỏi trọng tâm:

1. **Dự án này là gì?**  
   $\rightarrow$ Hệ thống Website học 214 Bộ thủ và Từ vựng Tiếng Trung kết hợp thuật toán lặp lại ngắt quãng SM-2 (SRS), hỗ trợ nhập bài học từ Excel và quy trình kiểm duyệt bài học.
2. **Công nghệ nào đang được sử dụng?**  
   $\rightarrow$ Java 21 LTS (OpenJDK 21.0.12), Spring Boot 3.3.5, Apache Maven 3.9.16, MySQL Community Server 8.4.9 LTS, Flyway 10.x, Spring Data JPA / Hibernate 6.5.3, HTML/CSS/JavaScript.
3. **Kiến trúc hệ thống là gì?**  
   $\rightarrow$ Kiến trúc phân tầng: `Client/Frontend -> REST API -> Controller -> Service -> Repository -> MySQL`. Flyway là thẩm quyền duy nhất quản lý schema. DTO phân tách hoàn toàn với Entity.
4. **Mô hình CSDL nào đã được phê duyệt?**  
   $\rightarrow$ Mô hình đúng 14 bảng quan hệ chuẩn hóa 3NF, bộ mã `utf8mb4` / `utf8mb4_unicode_ci`, khóa chính `BIGINT/INT UNSIGNED AUTO_INCREMENT`.
5. **Những gì ĐÃ THỰC SỰ được triển khai?**  
   $\rightarrow$ Khung dự án Spring Boot 3.3.5 (`backend/pom.xml`), cấu hình datasource & Flyway (`application.yml`), script Flyway `V1__init_schema.sql`, class chính `ElearningApplication.java`, kiểm thử context `ElearningApplicationTests.java`, và kho Git sạch sẽ.
6. **Những gì ĐÃ ĐƯỢC XÁC MINH?**  
   $\rightarrow$ `mvn test` SUCCESS, `mvn clean package` SUCCESS, Flyway V1 áp dụng thành công, CSDL `elearning_db` đã tạo đủ đúng 14 bảng nghiệp vụ với đầy đủ ràng buộc và kiểu dữ liệu chuẩn xác.
7. **Những gì CHƯA ĐƯỢC triển khai?**  
   $\rightarrow$ Chưa có `ApiResponse<T>`, `PageResponse<T>`, `GlobalExceptionHandler`, JPA Entities, Repositories, DTOs, Controllers, Services, Security/JWT, Business Logic, Thuật toán SRS, Excel Import, Giao diện Frontend.
8. **Dự án đang ở Phase nào?**  
   $\rightarrow$ Đang ở cuối **Phase 1 — Spring Boot Foundation & Web Infrastructure** (Module 1A COMPLETED, Module 1B PENDING).
9. **Task nào là Task tiếp theo cần làm?**  
   $\rightarrow$ **Task 1B.1 — Base Response Models (ApiResponse<T>, PageResponse<T>, ErrorCode)** để chuẩn hóa cấu trúc phong bì API và hoàn tất 100% Phase 1.
10. **Ràng buộc nào TUYỆT ĐỐI KHÔNG ĐƯỢC VI PHẠM?**  
    $\rightarrow$ Không dùng lại mã nguồn cũ; không để Hibernate tự sửa schema (`ddl-auto: none`); không trả Entity ra API; không đổi tên cột `review_time_seconds`; không giới hạn 5 notes/vocab; không tạo FK MySQL cho tham chiếu đa hình `item_type + item_id`.
11. **Câu hỏi nào còn mở (Open Questions)?**  
    $\rightarrow$ Duy nhất câu hỏi **OQ-08** về việc lựa chọn thư viện tĩnh cho Frontend tại Phase 9.
