# PROGRESS — BẢNG THEO DÕI TIẾN ĐỘ DỰ ÁN (PHASE → MODULE → TASK MATRIX)

> **Mô hình quản lý:** 4 cấp độ kỹ thuật:  
> $$\text{PHASE} \longrightarrow \text{MODULE} \longrightarrow \text{TASK} \longrightarrow \text{VERIFICATION CHECKPOINT}$$
> 
> **Nguyên tắc cập nhật trạng thái:**  
> - `NOT_STARTED`: Công việc chưa bắt đầu.  
> - `IN_PROGRESS`: Đang trong quá trình thực hiện.  
> - `COMPLETED`: Chỉ gán trạng thái này khi đã có bằng chứng nghiệm thu vật lý độc lập (Test pass, build pass, file artifact đã tạo và kiểm chứng).  
> - `BLOCKED`: Đang bị chặn bởi dependency hoặc câu hỏi mở.  
> - **Kỷ luật cốt lõi:** Tuyệt đối không đánh dấu COMPLETED cho bất kỳ task nào dựa trên giả định hoặc mã nguồn cũ đã xóa.  

---

## 1. BẢNG TRUY XUẤT LỊCH SỬ NHIỆM VỤ (TASK TRACEABILITY MAPPING)

Nhằm bảo toàn lịch sử thực thi và bằng chứng nghiệm thu từ các phiên trước, bảng ánh xạ định danh cũ $\rightarrow$ mới:

| Định danh cũ (Old Task ID) | Định danh chuẩn hóa mới (New Task ID) | Phase | Trạng thái thực tế | Ghi chú chuyển đổi |
| :--- | :--- | :--- | :--- | :--- |
| `TASK-0.1` | **Task 0A.1** | Phase 0 | `COMPLETED` | Xóa sạch legacy artifacts và source cũ. |
| `TASK-0.2` | **Task 0A.2** | Phase 0 | `COMPLETED` | Xác minh 19 Agent Skills trong `.agents/skills/`. |
| `TASK-0.3` | **Task 0B.1** | Phase 0 | `COMPLETED` | Thiết lập 9 tài liệu đặc tả chuẩn 14 bảng. |
| `TASK-0.4` | **Task 0B.2** | Phase 0 | `COMPLETED` | Cài đặt & cấu hình MySQL Community Server 8.4 LTS. |
| `TASK-0.5` | **Task 0B.3** | Phase 0 | `COMPLETED` | Khởi tạo Git repo, thiết lập `.gitignore`. |
| `TASK-0.6` | **Task 0B.4** | Phase 0 | `COMPLETED` | Thiết kế CSDL Vật lý 14 bảng (`DATABASE_DESIGN.md`). |
| `TASK-1.1` | **Task 1A.1** | Phase 1 | `COMPLETED` | Cấu trúc Spring Boot 3.3.5, Java 21, `backend/pom.xml`. |
| `TASK-1.2` | **Task 1A.2** | Phase 1 | `COMPLETED` | `application.yml` HikariCP kết nối MySQL 3306. |
| `TASK-1.3` | **Task 1A.3** | Phase 1 | `COMPLETED` | Flyway V1 migration tạo 14 bảng nghiệp vụ. |
| `TASK-1.4` | **Task 1A.4** | Phase 1 | `COMPLETED` | Kiểm thử Context Load & Flyway schema version 1. |
| `TASK-1.5` | **Task 1B.1, 1B.2** | Phase 1 | `NOT_STARTED` | Phân rã thành 2 task nhỏ: DTOs phong bì và GlobalExceptionHandler. |
| `TASK-2.1` | **Task 2E.1, 2E.2** | Phase 2 | `NOT_STARTED` | Chuyển thành Module 2E: Flyway Seed Data V2, V3. |
| `TASK-2.2` | **Task 2A.1..2D.2** | Phase 2 | `NOT_STARTED` | Phân rã thành 4 Modules miền nghiệp vụ khép kín (2A, 2B, 2C, 2D). |
| `TASK-2.3` | **Task 2A.2, 2B.2, 2C.2, 2D.3** | Phase 2 | `NOT_STARTED` | Repositories được gắn liền với từng Module miền tương ứng. |
| `TASK-2.4` | **Task 2F.1** | Phase 2 | `NOT_STARTED` | Chuyển thành Module 2F: Full Persistence Verification. |
| `TASK-10.2` (cũ) / `10B.2` | **Task 10C.1** | Phase 10 | `NOT_STARTED` | Tách riêng phần đóng khoảng trống kiểm thử thành Module 10C. |

---

## 2. MA TRẬN TIẾN ĐỘ CHI TIẾT (TASK TRACKING MATRIX)

| Phase | Module | Task ID | Tên công việc (Task Description) | Status | Dependency (depends_on) | Bằng chứng xác minh (Verification Evidence) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Phase 0** | **Mod 0A** | **Task 0A.1** | Xóa bỏ toàn bộ mã nguồn cũ và artifacts không phù hợp | **COMPLETED** | Không | `backend/` và `frontend/` rỗng (`.gitkeep`); các thư mục legacy đã bị xóa sạch sẽ. |
| **Phase 0** | **Mod 0A** | **Task 0A.2** | Bảo tồn và xác minh tính toàn vẹn 19 Agent Skills | **COMPLETED** | Không | Script `validate_skills.ps1` chạy PASS, không có orphan references. |
| **Phase 0** | **Mod 0B** | **Task 0B.1** | Thiết lập lại toàn bộ tài liệu đặc tả chuẩn 14 bảng | **COMPLETED** | Task 0A.1 | Đã tạo và chuẩn hóa 9 tài liệu đặc tả trong `.agents/` (`PROJECT_CONTEXT`, `ARCHITECTURE`, v.v.). |
| **Phase 0** | **Mod 0B** | **Task 0B.2** | Cài đặt và cấu hình MySQL Community Server 8.4 LTS | **COMPLETED** | Không | Cài MySQL 8.4.9, daemon mysqld chạy cổng 3306, utf8mb4, kết nối thành công `elearning_db`. |
| **Phase 0** | **Mod 0B** | **Task 0B.3** | Khởi tạo Git repository, thiết lập file `.gitignore` tiêu chuẩn | **COMPLETED** | Không | `git init` nhánh `main`; `.gitignore` bảo vệ secrets, target/, ide files. Initial commit `40ada12`. |
| **Phase 0** | **Mod 0B** | **Task 0B.4** | Soạn thảo tài liệu Thiết kế CSDL Vật lý chi tiết cho 14 bảng | **COMPLETED** | Task 0B.1, 0B.2 | `.agents/DATABASE_DESIGN.md` gồm 20 mục chi tiết, giải quyết dứt điểm OQ-04..OQ-07. Commit `f2b2000`. |
| **Phase 1** | **Mod 1A** | **Task 1A.1** | Khởi tạo cấu trúc Spring Boot 3 và file `backend/pom.xml` | **COMPLETED** | Task 0B.4 | `backend/pom.xml` sử dụng Java 21, Spring Boot 3.3.5; Maven compile thành công. Commit `5d59ff4`. |
| **Phase 1** | **Mod 1A** | **Task 1A.2** | Thiết lập class khởi động, `application.yml` kết nối MySQL | **COMPLETED** | Task 1A.1 | `ElearningApplication.java`, `application.yml` kết nối MySQL cổng 3306 qua HikariCP, `ddl-auto=none`. |
| **Phase 1** | **Mod 1A** | **Task 1A.3** | Soạn thảo và thực thi Flyway migration `V1__init_schema.sql` | **COMPLETED** | Task 1A.2 | Flyway áp dụng V1 (527ms); đúng 14 bảng nghiệp vụ tạo đầy đủ trong `elearning_db`. |
| **Phase 1** | **Mod 1A** | **Task 1A.4** | Kiểm thử tự động nạp Spring Context và xác minh schema CSDL | **COMPLETED** | Task 1A.3 | `mvn test` PASS (1 test run, 0 failures), Flyway schema version 1 validated. |
| **Phase 1** | **Mod 1B** | **Task 1B.1** | Xây dựng Base Response DTOs (`ApiResponse<T>`, `PageResponse<T>`, `ErrorCode`) | **NOT_STARTED** | Task 1A.4 | Định nghĩa chuẩn phong bì JSON theo Mục 1.2 & 1.3 của API.md. Unit test serialization Jackson. |
| **Phase 1** | **Mod 1B** | **Task 1B.2** | Xây dựng `GlobalExceptionHandler` (`@RestControllerAdvice`) | **NOT_STARTED** | Task 1B.1 | Bắt và chuẩn hóa `MethodArgumentNotValidException`, `BusinessException`, `AccessDeniedException` thành `ApiResponse`. |
| **Phase 2** | **Mod 2A** | **Task 2A.1** | JPA Entity Mapping: `ACCOUNT`, `USER_PROFILE`, `ROLE`, `ACCOUNT_ROLE` | **NOT_STARTED** | Task 1A.4 [PARALLEL với 2B] | Entity mapping, composite key `@IdClass` hoặc `@EmbeddedId`, quan hệ `@OneToOne`, `@ManyToMany`. |
| **Phase 2** | **Mod 2A** | **Task 2A.2** | Spring Data JPA Repositories Cụm Định danh | **NOT_STARTED** | Task 2A.1 | `AccountRepository`, `UserProfileRepository`, `RoleRepository` kèm derived query methods. Repository test. |
| **Phase 2** | **Mod 2B** | **Task 2B.1** | JPA Entity Mapping: `RADICAL`, `VOCABULARY`, `VOCAB_RADICAL` | **NOT_STARTED** | Task 1A.4 [PARALLEL với 2A] | Entity mapping, composite key cho bảng liên kết N:N `VOCAB_RADICAL`, lưu `pinyin` và `pinyin_raw`. |
| **Phase 2** | **Mod 2B** | **Task 2B.2** | Spring Data JPA Repositories Cụm Từ điển | **NOT_STARTED** | Task 2B.1 | `RadicalRepository`, `VocabularyRepository` kèm derived queries tìm kiếm theo pinyin/pinyin_raw/hanzi. |
| **Phase 2** | **Mod 2C** | **Task 2C.1** | JPA Entity Mapping: `LESSON`, `LESSON_VOCABULARY` | **NOT_STARTED** | Task 2A.1 (Account Entity), Task 2B.1 (Vocab Entity) | Entity mapping, composite key bảng liên kết, giữ đúng trường `order_index` thứ tự từ vựng. |
| **Phase 2** | **Mod 2C** | **Task 2C.2** | Spring Data JPA Repositories Cụm Bài học | **NOT_STARTED** | Task 2C.1, Task 2A.2, Task 2B.2 | `LessonRepository`, `LessonVocabularyRepository` lọc theo status (`Draft`, `Pending`, `Approved`). |
| **Phase 2** | **Mod 2D** | **Task 2D.1** | JPA Entity Mapping: `USER_SRS_SETTING`, `PERSONAL_NOTE`, `MODERATION_LOG` | **NOT_STARTED** | Task 2A.1 (UserProfile), Task 2B.1 (Vocab), Task 2C.1 (Lesson) | Entity mapping: setting 1:1, note $\le 500$ chars, log kiểm toán `ON DELETE RESTRICT`. |
| **Phase 2** | **Mod 2D** | **Task 2D.2** | JPA Entity Mapping: `CARD_PROGRESS`, `REVIEW_LOG` (Tham chiếu đa hình) | **NOT_STARTED** | Task 2A.1 (Account) | Mapping cặp trường `item_type` (`VARCHAR(20)`) và `item_id` (`BIGINT UNSIGNED`), KHÔNG tạo FK vật lý MySQL. |
| **Phase 2** | **Mod 2D** | **Task 2D.3** | Spring Data JPA Repositories Cụm SRS, Ghi chú & Kiểm toán | **NOT_STARTED** | Task 2D.1, Task 2D.2 | Repositories cho `CardProgress`, `ReviewLog`, `PersonalNote`, `ModerationLog`, `UserSrsSetting`. |
| **Phase 2** | **Mod 2E** | **Task 2E.1** | Flyway Seed Data V2: 4 Vai trò hệ thống (`V2__seed_roles.sql`) | **NOT_STARTED** | Task 1A.4 [PARALLEL với 2A..2D] | Script seed 4 vai trò cố định: `1=Learner`, `2=Creator`, `3=Moderator`, `4=Admin`. |
| **Phase 2** | **Mod 2E** | **Task 2E.2** | Flyway Seed Data V3: 214 Bộ thủ Khang Hy (`V3__seed_radicals.sql`) | **NOT_STARTED** | Task 1A.4 [PARALLEL với 2A..2D] | Script seed 214 bộ thủ Khang Hy chuẩn từ dataset `.agents/references/radicals.json`. |
| **Phase 2** | **Mod 2F** | **Task 2F.1** | Kiểm thử tích hợp toàn diện tầng Persistence & Schema Validation | **NOT_STARTED** | Mod 2A, 2B, 2C, 2D, 2E | Bật Hibernate `ddl-auto: validate`, chạy toàn bộ JPA integration tests khớp schema 14 bảng MySQL. |
| **Phase 3** | **Mod 3A** | **Task 3A.1** | Cấu hình Spring Security 6 FilterChain, BCrypt & Stateless Session | **NOT_STARTED** | Task 1A.1, Task 1B.2 | SecurityConfig, vô hiệu hóa CSRF cho REST, session STATELESS, phân quyền URL pattern. |
| **Phase 3** | **Mod 3A** | **Task 3A.2** | Xây dựng `JwtUtil` và `JwtAuthenticationFilter` | **NOT_STARTED** | Task 3A.1 | Tạo/giải mã JWT token, kiểm tra claims (`sub`, `roles`, `exp`), bộ lọc JWT trước UsernamePassword filter. |
| **Phase 3** | **Mod 3A** | **Task 3A.3** | Triển khai `CustomUserDetailsService` tải thông tin tài khoản | **NOT_STARTED** | Task 2A.2, Task 3A.2 | Nạp user từ `AccountRepository`, ánh xạ Authorities từ danh sách `Role`. Unit test. |
| **Phase 3** | **Mod 3B** | **Task 3B.1** | Auth DTOs (`RegisterRequest`, `LoginRequest`, `AuthResponse`) | **NOT_STARTED** | Task 1B.1 | DTOs validation với Jakarta Validation (`@NotBlank`, `@Size`). |
| **Phase 3** | **Mod 3B** | **Task 3B.2** | `AuthService` & `AuthController` (`/api/v1/auth/**`) | **NOT_STARTED** | Task 1B.1, 1B.2, 2A.2, 2E.1 (cần Role Learner), 3A.3, 3B.1 | Đăng ký (gán vai trò `Learner`), đăng nhập xác thực BCrypt, sinh JWT, trả về `ApiResponse<AuthResponse>`. |
| **Phase 3** | **Mod 3C** | **Task 3C.1** | User Profile DTOs, `UserProfileService` & `UserProfileController` | **NOT_STARTED** | Task 1B.1, 1B.2, 2A.2, 3B.2 | Endpoints `GET /api/v1/users/profile`, `PUT /api/v1/users/profile` lấy thông tin user đăng nhập. |
| **Phase 3** | **Mod 3D** | **Task 3D.1** | Kiểm thử tự động MockMvc cho Auth & RBAC 4 vai trò | **NOT_STARTED** | Mod 3A, 3B, 3C | MockMvc tests: register, login đúng/sai, token hết hạn, 401 khi không có token, 403 khi sai vai trò. |
| **Phase 4** | **Mod 4A** | **Task 4A.1** | Radical DTOs & `RadicalService` tra cứu Bộ thủ | **NOT_STARTED** | Task 1B.1, 2B.2, 2E.2 (cần data 214 bộ thủ) | DTOs hiển thị bộ thủ, Service lấy 214 bộ thủ, chi tiết bộ thủ theo ID/ký tự kèm từ vựng liên quan. |
| **Phase 4** | **Mod 4A** | **Task 4A.2** | `RadicalController` công khai & Admin CRUD Bộ thủ | **NOT_STARTED** | Task 1B.1, 1B.2, Task 4A.1, Mod 3A | `GET /api/v1/radicals/**` (công khai), `POST/PUT/DELETE /api/v1/admin/radicals/**` (chỉ Admin). |
| **Phase 4** | **Mod 4B** | **Task 4B.1** | Vocabulary DTOs, tiêu chí tìm kiếm phân trang & `VocabularyService` | **NOT_STARTED** | Task 1B.1, Task 2B.2 | Tìm kiếm theo chữ Hán, pinyin, pinyin không dấu (`pinyin_raw`), lọc theo bộ thủ, phân trang `PageResponse`. |
| **Phase 4** | **Mod 4B** | **Task 4B.2** | `VocabularyController` công khai & Admin CRUD Từ vựng | **NOT_STARTED** | Task 1B.1, 1B.2, Task 4B.1, Mod 3A | `GET /api/v1/vocabularies/**` (công khai), `POST/PUT/DELETE /api/v1/admin/vocabularies/**` (chỉ Admin). |
| **Phase 4** | **Mod 4C** | **Task 4C.1** | Kiểm thử tự động MockMvc cho phân hệ Bộ thủ & Từ vựng | **NOT_STARTED** | Mod 4A, 4B | MockMvc test tra cứu public, tìm kiếm pinyin không dấu, phân trang, và phân quyền Admin. |
| **Phase 5** | **Mod 5A** | **Task 5A.1** | Lesson DTOs & `LessonService` khám phá bài học công khai | **NOT_STARTED** | Task 1B.1, Task 2C.2 | DTOs tóm tắt bài học, chi tiết bài học kèm danh sách từ vựng theo `order_index`. |
| **Phase 5** | **Mod 5A** | **Task 5A.2** | `LessonController` xem bài học công khai (`GET /api/v1/lessons/**`) | **NOT_STARTED** | Task 1B.1, 1B.2, Task 5A.1 | Endpoint công khai chỉ trả về các bài học có trạng thái `Approved`. Trả 404 nếu bài chưa duyệt. |
| **Phase 5** | **Mod 5B** | **Task 5B.1** | Creator Lesson DTOs & `CreatorLessonService` quản lý bài viết | **NOT_STARTED** | Task 2C.2, Mod 3A | CRUD bài học của tác giả, kiểm tra quyền sở hữu, cập nhật thứ tự từ vựng, nộp bài (`Draft` $\rightarrow$ `Pending`). |
| **Phase 5** | **Mod 5B** | **Task 5B.2** | `CreatorLessonController` (`/api/v1/creator/lessons/**`) | **NOT_STARTED** | Task 1B.1, 1B.2, Task 5B.1 | REST endpoints cho Creator; kiểm tra bảo vệ tài nguyên (Creator A không thể sửa bài Creator B). |
| **Phase 5** | **Mod 5C** | **Task 5C.1** | `ExcelParserService` với Apache POI (bước 1: đọc & validate) | **NOT_STARTED** | Task 2B.2 | Đọc file `.xlsx`, validate từng dòng (chữ Hán, pinyin, nghĩa), đối chiếu từ vựng có sẵn, xuất báo cáo lỗi. |
| **Phase 5** | **Mod 5C** | **Task 5C.2** | Controller API Upload Preview & Xác nhận lưu bài học từ Excel | **NOT_STARTED** | Task 1B.1, 1B.2, Task 5C.1, Task 5B.2 | `POST .../import/preview` (bước 1) và `POST .../import/confirm` (bước 2 lưu dữ liệu). |
| **Phase 5** | **Mod 5D** | **Task 5D.1** | Kiểm thử tự động MockMvc cho Quản lý bài học & Import Excel | **NOT_STARTED** | Mod 5A, 5B, 5C | Test vòng đời bài học, upload file Excel mẫu hợp lệ / file lỗi, bảo vệ quyền Creator. |
| **Phase 6** | **Mod 6A** | **Task 6A.1** | Moderation DTOs & `ModerationService` logic nghiệp vụ duyệt | **NOT_STARTED** | Task 1B.1, Task 2C.2, Task 2D.3 | Lấy hàng đợi `Pending`, chuyển trạng thái `Approved` hoặc `Rejected` (bắt buộc lý do & `flagged_fields` JSON). |
| **Phase 6** | **Mod 6A** | **Task 6A.2** | Ghi vết kiểm duyệt bất biến vào `MODERATION_LOG` | **NOT_STARTED** | Task 6A.1 | Ghi nhận lịch sử kiểm duyệt đầy đủ `lesson_id`, `moderator_id`, `action`, `rejection_reason`. |
| **Phase 6** | **Mod 6B** | **Task 6B.1** | `ModeratorController` (`/api/v1/moderator/**`) | **NOT_STARTED** | Task 1B.1, 1B.2, Task 6A.2, Mod 3A | Endpoints hàng đợi duyệt, nút phê duyệt, nút từ chối, xem lịch sử kiểm duyệt (chỉ Moderator/Admin). |
| **Phase 6** | **Mod 6C** | **Task 6C.1** | Kiểm thử tự động MockMvc cho quy trình Phê duyệt & Từ chối | **NOT_STARTED** | Mod 6A, 6B | Test bất biến: từ chối thiếu lý do trả về 400, chỉ duyệt bài `Pending`, log không thể bị xóa. |
| **Phase 7** | **Mod 7A** | **Task 7A.1** | Triển khai thuật toán thuần túy SM-2 trong `SrsCalculator` | **NOT_STARTED** | Không [Pure Algorithm] | Tính `interval_days`, `ease_factor`, `repetitions` theo rating 1-4, chặn sàn $EF \ge 1.30$. |
| **Phase 7** | **Mod 7A** | **Task 7A.2** | Unit Test toán học cho thuật toán tính khoảng cách SM-2 | **NOT_STARTED** | Task 7A.1 | Đạt độ phủ các nhánh thuật toán SM-2: rating Again, Hard, Good, Easy, chu kỳ ngày. |
| **Phase 7** | **Mod 7B** | **Task 7B.1** | SRS DTOs & `SrsService` điều phối phiên học lặp lại ngắt quãng | **NOT_STARTED** | Task 7A.1, Task 2D.3 | Lấy thẻ đến hạn (`next_review_at <= NOW()`), áp dụng giới hạn ngày, cập nhật `CARD_PROGRESS`, ghi `REVIEW_LOG`. |
| **Phase 7** | **Mod 7B** | **Task 7B.2** | `SrsController` (`/api/v1/srs/**`) | **NOT_STARTED** | Task 1B.1, 1B.2, Task 7B.1, Mod 3A | `GET /api/v1/srs/due`, `POST /api/v1/srs/review` (kèm `review_time_seconds`), `GET /api/v1/srs/stats`. |
| **Phase 7** | **Mod 7C** | **Task 7C.1** | Kiểm thử tự động MockMvc cho phân hệ ôn tập SRS | **NOT_STARTED** | Mod 7A, 7B | Test lấy thẻ đến hạn, nộp đánh giá, cập nhật chu kỳ ôn tập, xác minh tham chiếu đa hình ở Service layer. |
| **Phase 8** | **Mod 8A** | **Task 8A.1** | Personal Note DTOs & `PersonalNoteService` | **NOT_STARTED** | Task 1B.1, Task 2D.3, Mod 3A | CRUD ghi chú từ vựng, kiểm tra chặt chẽ `content <= 500 chars`, không giới hạn 5 notes, bảo vệ sở hữu. |
| **Phase 8** | **Mod 8A** | **Task 8A.2** | `PersonalNoteController` (`/api/v1/notes/**`, `/api/v1/vocabularies/{id}/notes`) | **NOT_STARTED** | Task 1B.1, 1B.2, Task 8A.1 | REST endpoints cho ghi chú cá nhân của người học. Trả về 403 nếu cố sửa ghi chú của người khác. |
| **Phase 8** | **Mod 8B** | **Task 8B.1** | Setting DTOs, `UserSrsSettingService` & `UserSrsSettingController` | **NOT_STARTED** | Task 1B.1, 1B.2, Task 2D.3, Mod 3A | `GET/PUT /api/v1/srs/settings`: `new_cards_per_day` (mặc định 20), `max_review_per_day` (mặc định 100). |
| **Phase 8** | **Mod 8C** | **Task 8C.1** | Kiểm thử tự động MockMvc cho Ghi chú cá nhân & Cài đặt SRS | **NOT_STARTED** | Mod 8A, 8B | Test độ dài ghi chú 500 ký tự (400 nếu vượt quá), cách ly dữ liệu user, cập nhật setting SRS. |
| **Phase 9** | **Mod 9A** | **Task 9A.1** | Khởi tạo khung giao diện, layout dùng chung & hệ thống CSS | **NOT_STARTED** | Không | Khung HTML5, navbar responsive, footer, modal container, hệ thống class CSS layout chuẩn. |
| **Phase 9** | **Mod 9A** | **Task 9A.2** | Xây dựng HTTP Client tập trung `frontend/js/api.js` | **NOT_STARTED** | Task 9A.1, Task 1B.1, 1B.2 | Wrapper fetch, tự động đính kèm `Authorization: Bearer`, bắt lỗi 401 điều hướng login, chuẩn hóa lỗi. |
| **Phase 9** | **Mod 9B** | **Task 9B.1** | Giao diện Đăng ký, Đăng nhập & Xem hồ sơ cá nhân | **NOT_STARTED** | Task 9A.2, Phase 3 (Auth Endpoints) | `login.html`, `register.html`, `profile.html`: form validation, lưu JWT localStorage, xử lý 3 trạng thái. |
| **Phase 9** | **Mod 9B** | **Task 9B.2** | Giao diện Tra cứu 214 Bộ thủ & Từ vựng | **NOT_STARTED** | Task 9A.2, Phase 4 (Catalog Endpoints) | `radicals.html`, `vocabulary.html`: lưới 214 bộ thủ, thanh tìm kiếm pinyin/hanzi, modal nét viết/audio. |
| **Phase 9** | **Mod 9C** | **Task 9C.1** | Giao diện Khám phá Bài học & Thêm ghi chú cá nhân | **NOT_STARTED** | Task 9A.2, Mod 5A, Mod 8A | `lessons.html`, `lesson-detail.html`: danh sách bài học, bảng từ vựng, popover tạo ghi chú $\le 500$ chars. |
| **Phase 9** | **Mod 9C** | **Task 9C.2** | Giao diện Ôn tập Flashcard SRS tương tác | **NOT_STARTED** | Task 9A.2, Mod 7B | `srs-review.html`: lật thẻ 3D, audio phát âm, 4 nút rating (Again/Hard/Good/Easy), đếm giây phản xạ. |
| **Phase 9** | **Mod 9D** | **Task 9D.1** | Giao diện Creator Lesson Studio (soạn bài & sắp xếp từ vựng) | **NOT_STARTED** | Task 9A.2, Mod 5B | `creator-lessons.html`: form tạo bài học, danh sách từ vựng kéo thả đổi thứ tự, nút gửi duyệt. |
| **Phase 9** | **Mod 9D** | **Task 9D.2** | Giao diện Import Excel 2 bước cho Creator | **NOT_STARTED** | Task 9D.1, Mod 5C | Dropzone upload file Excel, render bảng dữ liệu xem trước (preview) kèm lỗi từng dòng trước khi confirm. |
| **Phase 9** | **Mod 9E** | **Task 9E.1** | Giao diện Bàn làm việc Kiểm duyệt viên (Moderator Dashboard) | **NOT_STARTED** | Task 9A.2, Phase 6 (Moderator Endpoints) | `moderator.html`: hàng đợi bài `Pending`, modal xem bài, nút duyệt, modal từ chối nhập lý do/chọn lỗi. |
| **Phase 9** | **Mod 9F** | **Task 9F.1** | Kiểm thử tích hợp UI trình duyệt & DevTools Console | **NOT_STARTED** | Mod 9A..9E | Kiểm tra Network gọi API, xử lý đủ 3 trạng thái (Loading, Empty, Error), chống XSS DOM. |
| **Phase 10** | **Mod 10A** | **Task 10A.1** | Rà soát an ninh ứng dụng & Gia cố bảo mật OWASP | **NOT_STARTED** | Phase 9 | Rà soát CORS, chống XSS, kiểm tra truy vấn JPA chống SQLi, kiểm tra an toàn upload file Excel. |
| **Phase 10** | **Mod 10A** | **Task 10A.2** | Kiểm soát tần suất gọi (Rate Limiting) trên Auth endpoints | **NOT_STARTED** | Task 10A.1 | Chống brute-force tấn công dò mật khẩu tại `/api/v1/auth/login`. |
| **Phase 10** | **Mod 10B** | **Task 10B.1** | Tối ưu hóa truy vấn CSDL & Xác minh chỉ mục MySQL | **NOT_STARTED** | Phase 9 | `EXPLAIN` kiểm tra hiệu năng chỉ mục `idx_card_progress_due`, `idx_vocab_pinyin_raw`, `idx_lesson_status`. |
| **Phase 10** | **Mod 10C** | **Task 10C.1** | Hoàn thiện khoảng trống kiểm thử tự động (Quality Gap Closure) | **NOT_STARTED** | Task 10A.2, Task 10B.1 | Bổ sung test coverage cho các edge cases và hồi quy kiểm thử trên toàn bộ phân hệ. |
| **Phase 11** | **Mod 11A** | **Task 11A.1** | Thực thi kịch bản E2E 1: Luồng Học viên hoàn chỉnh | **NOT_STARTED** | Phase 10 | Kịch bản tự động: Đăng ký $\rightarrow$ Học bộ thủ $\rightarrow$ Tạo ghi chú $\rightarrow$ Lật thẻ SRS $\rightarrow$ Thống kê. |
| **Phase 11** | **Mod 11A** | **Task 11A.2** | Thực thi kịch bản E2E 2: Luồng Tác giả & Kiểm duyệt hoàn chỉnh | **NOT_STARTED** | Phase 10 | Kịch bản tự động: Upload Excel $\rightarrow$ Xem preview $\rightarrow$ Lưu bài $\rightarrow$ Gửi duyệt $\rightarrow$ Duyệt bài $\rightarrow$ Xuất bản. |
| **Phase 11** | **Mod 11B** | **Task 11B.1** | Đóng gói sản phẩm cuối cùng & Kiểm tra triển khai sạch | **NOT_STARTED** | Mod 11A | `mvn clean package`, kiểm tra file JAR thực thi độc lập và chạy migration trên database sạch. |
| **Phase 11** | **Mod 11B** | **Task 11B.2** | Nghiệm thu và bàn giao bộ tài liệu hướng dẫn vận hành | **NOT_STARTED** | Task 11B.1 | Hoàn thiện tài liệu bàn giao, xuất file Postman Collection hoàn chỉnh cho toàn bộ API. |
