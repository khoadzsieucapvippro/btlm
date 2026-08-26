# PROGRESS — BẢNG THEO DÕI TIẾN ĐỘ DỰ ÁN MỚI (CLEAN STATE TRACKING)

> **Nguyên tắc quản lý trạng thái:**  
> - `NOT_STARTED`: Công việc chưa bắt đầu.  
> - `IN_PROGRESS`: Đang trong quá trình thực hiện.  
> - `COMPLETED`: Chỉ gán trạng thái này khi đã có bằng chứng nghiệm thu vật lý (Test pass, build pass, artifact đã tạo).  
> - `BLOCKED`: Đang bị chặn bởi câu hỏi mở (Open Question) hoặc phụ thuộc kỹ thuật chưa được giải quyết.  
> - **Tuyệt đối cấm:** Đánh dấu COMPLETED cho bất kỳ task triển khai nào dựa trên mã nguồn cũ đã xóa. Không áp đặt giả định 1 bảng = 1 JPA Entity.  

---

## BẢNG TIẾN ĐỘ CHI TIẾT (TASK TRACKING MATRIX)

| Phase | Task ID | Tên công việc (Task Description) | Status | Dependency | Bằng chứng xác minh (Verification Evidence) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Phase 0** | **TASK-0.1** | Xóa bỏ toàn bộ mã nguồn cũ và artifacts triển khai không còn phù hợp | **COMPLETED** | Không | Đã xóa sạch thư mục `backend/*`, `frontend/*`, `scripts/`, `temp/`, `docs/`, `.vs/` và các báo cáo cũ. `backend` và `frontend` là thư mục rỗng. |
| **Phase 0** | **TASK-0.2** | Bảo tồn và xác minh tính toàn vẹn 19 Agent Skills trong `.agents/skills/` | **COMPLETED** | Không | Script `validate_skills.ps1` trả về `Validation PASSED` (100% hợp lệ, không có orphan references). |
| **Phase 0** | **TASK-0.3** | Thiết lập lại toàn bộ tài liệu đặc tả chuẩn 14 bảng cho lần triển khai mới | **COMPLETED** | Không | Đã tạo và chuẩn hóa 9 tài liệu đặc tả trong `.agents/` (`PROJECT_CONTEXT`, `ARCHITECTURE`, `DATABASE`, `API`, `DECISIONS`, `OPEN_QUESTIONS`, `WORKFLOW`, `ROADMAP`, `PROGRESS`). |
| **Phase 0** | **TASK-0.4** | Cài đặt và cấu hình MySQL Community Server 8.4 LTS cục bộ | **COMPLETED** | Không | Cài đặt `Oracle.MySQL` 8.4.9 qua winget; cấu hình `my.ini` utf8mb4; tiến trình daemon mysqld chạy cổng 3306; kết nối `mysql.exe` thành công; tạo database `elearning_db`. Không ảnh hưởng tới SQL Server. |
| **Phase 0** | **TASK-0.5** | Khởi tạo Git repository, thiết lập file `.gitignore` tiêu chuẩn | **COMPLETED** | Không | `git init` thành công; `.gitignore` bảo vệ secrets, target/, ide files; initial commit sẵn sàng. |
| **Phase 0** | **TASK-0.6** | Soạn thảo tài liệu Thiết kế CSDL Vật lý chi tiết cho 14 bảng MySQL | **COMPLETED** | Không | Tài liệu `.agents/DATABASE_DESIGN.md` hoàn thành với đủ 20 mục chi tiết, giải quyết dứt điểm OQ-04, OQ-05, OQ-06, OQ-07. |
| **Phase 1** | **TASK-1.1** | Khởi tạo cấu trúc dự án Spring Boot 3 và file `backend/pom.xml` | **COMPLETED** | Phase 0 | `backend/pom.xml` sử dụng Java 21 LTS, Spring Boot 3.3.5, Maven 3.9.16; build thành công. |
| **Phase 1** | **TASK-1.2** | Thiết lập class khởi động, `application.yml` trỏ MySQL và Flyway cấu hình | **COMPLETED** | TASK-1.1 | `ElearningApplication.java`, `application.yml` kết nối MySQL cổng 3306 qua HikariCP, `ddl-auto=none`. |
| **Phase 1** | **TASK-1.3** | Soạn thảo và thực thi Flyway migration `V1__init_schema.sql` cho 14 bảng chuẩn | **COMPLETED** | TASK-1.2 | `V1__init_schema.sql` áp dụng thành công qua Flyway; 14 bảng nghiệp vụ tạo đầy đủ trong `elearning_db`. |
| **Phase 1** | **TASK-1.4** | Kiểm thử tự động nạp Spring Context và xác minh schema CSDL | **COMPLETED** | TASK-1.3 | `ElearningApplicationTests` pass 100%, `flyway_schema_history` ghi nhận version 1 `SUCCESS`. |
| **Phase 1** | **TASK-1.5** | Thiết lập chuẩn `ApiResponse`, `PageResponse` và `GlobalExceptionHandler` | **NOT_STARTED** | TASK-1.4 | Chờ triển khai ở bước kế tiếp. |
| **Phase 2** | **Task 2A.1** | JPA Mapping: Account, UserProfile, Role & AccountRole | **NOT_STARTED** | Phase 1 | Cụm bảng định danh: Entity, quan hệ N:N, composite key, Repositories, Unit test. |
| **Phase 2** | **Task 2A.2** | JPA Mapping: Radical, Vocabulary & VocabRadical | **NOT_STARTED** | Task 2A.1 | Cụm bảng từ điển: Entity, quan hệ N:N, composite key, Repositories, Unit test. |
| **Phase 2** | **Task 2A.3** | JPA Mapping: Lesson & LessonVocabulary | **NOT_STARTED** | Task 2A.2 | Cụm bảng bài học: Entity, quan hệ N:N kèm `order_index`, Repositories, Unit test. |
| **Phase 2** | **Task 2A.4** | JPA Mapping: SRS Setting, CardProgress, ReviewLog, Notes & ModerationLog | **NOT_STARTED** | Task 2A.3 | Cụm bảng SRS & kiểm toán: Xử lý tham chiếu đa hình (`item_type` + `item_id`), Repositories, Unit test. |
| **Phase 2** | **Task 2B.1** | Flyway Seed Data V2: 4 Vai trò hệ thống (`V2__seed_roles.sql`) | **NOT_STARTED** | Task 2A.1 | Seed 4 vai trò cố định: `Learner`, `Creator`, `Moderator`, `Admin`. |
| **Phase 2** | **Task 2B.2** | Flyway Seed Data V3: 214 Bộ thủ Khang Hy (`V3__seed_radicals.sql`) | **NOT_STARTED** | Task 2A.2 | Seed 214 bộ thủ Khang Hy chuẩn từ dataset `.agents/references/radicals.json`. |
| **Phase 2** | **Task 2C.1** | Kiểm thử tích hợp toàn diện tầng Persistence (Schema validation & JPA Tests) | **NOT_STARTED** | Task 2A.4, 2B.2 | Hibernate schema validation khớp 100% với 14 bảng CSDL; Integration test truy xuất dữ liệu seed. |
| **Phase 3** | **Task 3A.1** | Cấu hình Spring Security 6 FilterChain, BCrypt & Stateless Session | **NOT_STARTED** | Phase 2 | Thiết lập security core, tắt CSRF cho REST, cấu hình phân quyền endpoint cơ bản. |
| **Phase 3** | **Task 3A.2** | Xây dựng `JwtUtil`, trích xuất claims, mã hóa và `JwtAuthenticationFilter` | **NOT_STARTED** | Task 3A.1 | Bộ lọc JWT chặn request, giải mã Bearer token, gán Authentication vào SecurityContext. |
| **Phase 3** | **Task 3A.3** | Triển khai `CustomUserDetailsService` tải thông tin tài khoản và roles | **NOT_STARTED** | Task 3A.2 | Tải user từ `AccountRepository`, ánh xạ Authorities từ bảng `ROLE`. |
| **Phase 3** | **Task 3B.1** | DTOs xác thực (`RegisterRequest`, `LoginRequest`, `AuthResponse`) & `AuthService` | **NOT_STARTED** | Task 3A.3 | Nghiệp vụ đăng ký (gán vai trò mặc định `Learner`, mã hóa mật khẩu) và đăng nhập (kiểm tra pass, sinh JWT). |
| **Phase 3** | **Task 3B.2** | Xây dựng `AuthController` (`/api/v1/auth/register`, `/api/v1/auth/login`) | **NOT_STARTED** | Task 3B.1 | REST endpoints xác thực với validation input `@Valid` và trả về `ApiResponse<AuthResponse>`. |
| **Phase 3** | **Task 3B.3** | Xây dựng DTO, Service & Controller quản lý Hồ sơ người dùng (`/api/v1/users/profile`) | **NOT_STARTED** | Task 3B.2 | Lấy và cập nhật `full_name`, `avatar_url` của tài khoản hiện tại qua token. |
| **Phase 3** | **Task 3C.1** | Kiểm thử MockMvc & Unit Test cho Auth flow và RBAC 4 vai trò | **NOT_STARTED** | Task 3B.3 | Kiểm thử kịch bản đăng ký, đăng nhập sai pass, token hết hạn, kiểm tra 401 Unauthorized và 403 Forbidden. |
| **Phase 4** | **Task 4A.1** | DTOs & Service tra cứu Bộ thủ (`RadicalService`) | **NOT_STARTED** | Phase 2 | Danh sách 214 bộ thủ, chi tiết bộ thủ theo ID/ký tự, danh sách từ vựng cấu thành từ bộ thủ. |
| **Phase 4** | **Task 4A.2** | Xây dựng `RadicalController` công khai và Admin CRUD Bộ thủ | **NOT_STARTED** | Task 4A.1, Phase 3 | `GET /api/v1/radicals/**` (công khai), `POST/PUT/DELETE /api/v1/admin/radicals/**` (quyền Admin). |
| **Phase 4** | **Task 4B.1** | DTOs, tiêu chí tìm kiếm phân trang & `VocabularyService` | **NOT_STARTED** | Phase 2 | Tra cứu từ vựng theo pinyin, pinyin không dấu (`pinyin_raw`), chữ Hán (`hanzi`), lọc theo bộ thủ. |
| **Phase 4** | **Task 4B.2** | Xây dựng `VocabularyController` công khai và Admin CRUD Từ vựng | **NOT_STARTED** | Task 4B.1, Phase 3 | `GET /api/v1/vocabularies/**` (công khai), `POST/PUT/DELETE /api/v1/admin/vocabularies/**` (quyền Admin). |
| **Phase 4** | **Task 4C.1** | Kiểm thử tự động cho phân hệ Bộ thủ & Từ vựng | **NOT_STARTED** | Task 4A.2, 4B.2 | MockMvc test tra cứu, tìm kiếm pinyin không dấu, phân trang và bảo vệ quyền Admin. |
| **Phase 5** | **Task 5A.1** | DTOs bài học, sắp xếp từ vựng & `LessonService` | **NOT_STARTED** | Phase 4 | CRUD bài học cá nhân, thêm từ vựng kèm `order_index`, chuyển trạng thái `Draft` -> `Pending`. |
| **Phase 5** | **Task 5A.2** | Xây dựng `LessonController` xem bài học công khai (`Approved`) | **NOT_STARTED** | Task 5A.1 | `GET /api/v1/lessons`, `GET /api/v1/lessons/{id}` (chỉ trả về bài học đã được phê duyệt). |
| **Phase 5** | **Task 5A.3** | Xây dựng `CreatorLessonController` quản lý bài học của tác giả | **NOT_STARTED** | Task 5A.1, Phase 3 | `POST/PUT/DELETE /api/v1/creator/lessons/**` (quyền Creator, kiểm tra quyền sở hữu bài học). |
| **Phase 5** | **Task 5B.1** | Xây dựng `ExcelParserService` với Apache POI (bước 1: đọc & validate) | **NOT_STARTED** | Task 5A.3 | Đọc file `.xlsx`, validate từng dòng (chữ Hán, pinyin, nghĩa), trả về preview kèm mã lỗi chi tiết. |
| **Phase 5** | **Task 5B.2** | API Upload & Xác nhận lưu bài học từ Excel (bước 2: confirm) | **NOT_STARTED** | Task 5B.1 | `POST /api/v1/creator/lessons/import/preview`, `POST /api/v1/creator/lessons/import/confirm`. |
| **Phase 5** | **Task 5C.1** | Kiểm thử tự động cho quản lý bài học và import Excel | **NOT_STARTED** | Task 5B.2 | Kiểm thử vòng đời bài học, kiểm tra file Excel hợp lệ / sai định dạng, bảo vệ quyền Creator. |
| **Phase 6** | **Task 6A.1** | DTOs kiểm duyệt & `ModerationService` | **NOT_STARTED** | Phase 5, Phase 3 | Lấy hàng đợi `Pending`, duyệt (`Approved`), từ chối (`Rejected` bắt buộc lý do & `flagged_fields` JSON), ghi log `MODERATION_LOG`. |
| **Phase 6** | **Task 6B.1** | Xây dựng `ModeratorController` (`/api/v1/moderator/**`) | **NOT_STARTED** | Task 6A.1 | Endpoints hàng đợi duyệt, phê duyệt/từ chối bài học, xem lịch sử kiểm duyệt (quyền Moderator/Admin). |
| **Phase 6** | **Task 6C.1** | Kiểm thử tự động cho quy trình Phê duyệt & Từ chối bài học | **NOT_STARTED** | Task 6B.1 | Kiểm thử bất biến nghiệp vụ: chỉ duyệt bài `Pending`, bắt buộc lý do khi từ chối, tính bất biến của log. |
| **Phase 7** | **Task 7A.1** | Triển khai thuật toán thuần túy SM-2 trong `SrsCalculator` | **NOT_STARTED** | Phase 2 | Tính `interval_days`, `ease_factor`, `repetitions` theo đánh giá 1-4, chặn cận dưới $EF \ge 1.30$. |
| **Phase 7** | **Task 7A.2** | Unit Test toán học cho thuật toán tính toán khoảng cách SM-2 | **NOT_STARTED** | Task 7A.1 | Kiểm thử các ca biên: nhớ tốt liên tiếp, quên thẻ (`Again`), hệ số suy giảm, chu kỳ ngày chính xác. |
| **Phase 7** | **Task 7B.1** | DTOs ôn tập & `SrsService` (lấy thẻ đến hạn, ghi nhận kết quả ôn tập) | **NOT_STARTED** | Task 7A.2 | Lấy thẻ đến hạn (`next_review_at <= NOW()`), giới hạn ngày, cập nhật `CARD_PROGRESS`, ghi `REVIEW_LOG` kèm `review_time_seconds`. |
| **Phase 7** | **Task 7B.2** | Xây dựng `SrsController` (`/api/v1/srs/**`) | **NOT_STARTED** | Task 7B.1, Phase 3 | Endpoints lấy danh sách thẻ học hôm nay, nộp kết quả lật thẻ (`POST /review`), xem thống kê ghi nhớ. |
| **Phase 7** | **Task 7C.1** | Kiểm thử tự động cho phân hệ ôn tập SRS | **NOT_STARTED** | Task 7B.2 | Kiểm thử đa hình (`VOCABULARY` vs `RADICAL`), kiểm tra cập nhật tiến trình thẻ và nhật ký ôn tập. |
| **Phase 8** | **Task 8A.1** | DTOs & `PersonalNoteService` (CRUD ghi chú từ vựng cá nhân) | **NOT_STARTED** | Phase 4, Phase 3 | Tạo/sửa/xóa ghi chú, chặn cứng độ dài $\le 500$ ký tự, không áp dụng giới hạn 5 ghi chú. |
| **Phase 8** | **Task 8A.2** | Xây dựng `PersonalNoteController` (`/api/v1/notes/**`) | **NOT_STARTED** | Task 8A.1 | Endpoints quản lý ghi chú cá nhân, kiểm soát quyền sở hữu (chỉ xem/sửa ghi chú của chính mình). |
| **Phase 8** | **Task 8B.1** | DTOs, Service & Controller Cài đặt SRS (`/api/v1/srs/settings`) | **NOT_STARTED** | Phase 7, Phase 3 | Lấy và tùy chỉnh `new_cards_per_day` (mặc định 20), `max_review_per_day` (mặc định 100). |
| **Phase 8** | **Task 8C.1** | Kiểm thử tự động cho Ghi chú cá nhân và Cài đặt SRS | **NOT_STARTED** | Task 8A.2, 8B.1 | Kiểm thử giới hạn 500 ký tự, kiểm tra cách ly dữ liệu giữa các người dùng, cập nhật cấu hình SRS. |
| **Phase 9** | **Task 9A.1** | Khởi tạo khung giao diện, layout dùng chung & hệ thống CSS | **NOT_STARTED** | Phase 3 | Khung HTML5/CSS tĩnh, header/footer, thanh điều hướng responsive, modal container. |
| **Phase 9** | **Task 9A.2** | Xây dựng `frontend/js/api.js` tập trung | **NOT_STARTED** | Task 9A.1 | Wrapper gọi fetch, tự động đính kèm `Authorization: Bearer`, xử lý 401 chuyển về login, chuẩn hóa lỗi. |
| **Phase 9** | **Task 9B.1** | Giao diện Đăng ký, Đăng nhập & Xem hồ sơ cá nhân | **NOT_STARTED** | Task 9A.2, Phase 3 | Form đăng nhập/đăng ký, lưu JWT vào localStorage, hiển thị thông tin profile người dùng. |
| **Phase 9** | **Task 9B.2** | Giao diện Tra cứu 214 Bộ thủ & Từ vựng | **NOT_STARTED** | Task 9A.2, Phase 4 | Lưới 214 bộ thủ, thanh tìm kiếm từ vựng theo pinyin/chữ Hán, modal chi tiết nét viết và phát âm audio. |
| **Phase 9** | **Task 9B.3** | Giao diện Khám phá Bài học & Thêm ghi chú cá nhân | **NOT_STARTED** | Task 9A.2, Phase 5, 8 | Danh sách bài học công khai, bảng từ vựng trong bài, form ghi chú cá nhân $\le 500$ ký tự ngay tại từ vựng. |
| **Phase 9** | **Task 9B.4** | Giao diện Ôn tập Flashcard SRS tương tác | **NOT_STARTED** | Task 9A.2, Phase 7 | Thao tác lật thẻ mặt trước/sau, 4 nút đánh giá (Again/Hard/Good/Easy), đếm giây `review_time_seconds`. |
| **Phase 9** | **Task 9C.1** | Giao diện Tác giả bài học (Creator Studio & Import Excel) | **NOT_STARTED** | Task 9A.2, Phase 5 | Tạo bài học, kéo thả sắp xếp từ vựng, upload Excel xem trước bảng dữ liệu trước khi bấm xác nhận lưu. |
| **Phase 9** | **Task 9C.2** | Giao diện Bàn làm việc Kiểm duyệt viên (Moderator Dashboard) | **NOT_STARTED** | Task 9A.2, Phase 6 | Danh sách bài chờ duyệt, modal xem chi tiết, nút duyệt, modal từ chối nhập lý do và đánh dấu trường lỗi. |
| **Phase 9** | **Task 9D.1** | Kiểm thử giao diện và tương tác qua trình duyệt với DevTools | **NOT_STARTED** | Task 9B.1..9C.2 | Kiểm tra mạng (Network API calls), kiểm tra 3 trạng thái giao diện (Loading, Success, Error), tính đáp ứng. |
| **Phase 10** | **Task 10A.1** | Rà soát an ninh ứng dụng & Gia cố bảo mật OWASP | **NOT_STARTED** | Phase 9 | Rà soát CORS, chống XSS, kiểm tra chống SQLi trong truy vấn JPA, kiểm tra an toàn upload file Excel. |
| **Phase 10** | **Task 10A.2** | Kiểm soát tần suất gọi (Rate Limiting) trên Auth endpoints | **NOT_STARTED** | Task 10A.1 | Chống brute-force tấn công dò mật khẩu tại `/api/v1/auth/login`. |
| **Phase 10** | **Task 10B.1** | Tối ưu hóa truy vấn CSDL & Xác minh chỉ mục MySQL | **NOT_STARTED** | Task 10A.1 | Sử dụng `EXPLAIN` kiểm tra hiệu năng chỉ mục `idx_card_progress_due`, `idx_vocab_pinyin_raw`, `idx_lesson_status`. |
| **Phase 10** | **Task 10B.2** | Hoàn thiện độ phủ kiểm thử tự động (Unit & Integration Tests) | **NOT_STARTED** | Task 10B.1 | Bổ sung test coverage cho toàn bộ các service và controller trọng yếu. |
| **Phase 11** | **Task 11A.1** | Thực thi kịch bản E2E 1: Luồng Học viên (Đăng ký -> Học bộ thủ -> Ghi chú -> Ôn tập SRS) | **NOT_STARTED** | Phase 10 | Kiểm thử tích hợp trọn vẹn hành trình trải nghiệm người học từ đầu đến cuối. |
| **Phase 11** | **Task 11A.2** | Thực thi kịch bản E2E 2: Luồng Tác giả & Kiểm duyệt (Import Excel -> Nộp bài -> Duyệt bài -> Học) | **NOT_STARTED** | Phase 10 | Kiểm thử tích hợp trọn vẹn luồng xuất bản nội dung giữa Creator, Moderator và Learner. |
| **Phase 11** | **Task 11B.1** | Đóng gói sản phẩm cuối cùng & Kiểm tra triển khai sạch | **NOT_STARTED** | Task 11A.1, 11A.2 | `mvn clean package`, kiểm tra file JAR thực thi độc lập, kiểm tra chạy migration trên DB mới. |
| **Phase 11** | **Task 11B.2** | Nghiệm thu và bàn giao bộ tài liệu hướng dẫn vận hành | **NOT_STARTED** | Task 11B.1 | Hoàn thiện tài liệu bàn giao, xuất file Postman collection hoàn chỉnh cho toàn bộ API. |
