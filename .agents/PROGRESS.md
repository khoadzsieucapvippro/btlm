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
| **Phase 3** | **TASK-3.1** | Thiết lập Spring Security Filter Chain, `JwtUtil` và Stateless Session | **NOT_STARTED** | Phase 1, 2 | Chưa bắt đầu. |
| **Phase 3** | **TASK-3.2** | Xây dựng `AuthService`, DTOs và `AuthController` (`/api/v1/auth/**`) | **NOT_STARTED** | TASK-3.1 | Chưa bắt đầu. |
| **Phase 3** | **TASK-3.3** | Viết Unit & MockMvc Test cho Authentication và phân quyền RBAC 4 vai trò | **NOT_STARTED** | TASK-3.2 | Chưa bắt đầu. |
| **Phase 4** | **TASK-4.1** | Xây dựng DTOs và Service tra cứu Bộ thủ và Từ vựng | **NOT_STARTED** | Phase 2 | Chưa bắt đầu. |
| **Phase 4** | **TASK-4.2** | Xây dựng `RadicalController` và `VocabularyController` (`/api/v1/...`) | **NOT_STARTED** | TASK-4.1 | Chưa bắt đầu. |
| **Phase 4** | **TASK-4.3** | Viết kiểm thử tự động cho tra cứu danh mục và phân trang | **NOT_STARTED** | TASK-4.2 | Chưa bắt đầu. |
| **Phase 5** | **TASK-5.1** | Xây dựng `ExcelImportService` với Apache POI (luồng 2 bước) | **NOT_STARTED** | Phase 2, 4 | Chưa bắt đầu. |
| **Phase 5** | **TASK-5.2** | Xây dựng `CreatorLessonController` và `LessonController` (`/api/v1/...`) | **NOT_STARTED** | TASK-5.1 | Chưa bắt đầu. |
| **Phase 5** | **TASK-5.3** | Viết kiểm thử tự động cho quản lý bài học và import Excel | **NOT_STARTED** | TASK-5.2 | Chưa bắt đầu. |
| **Phase 6** | **TASK-6.1** | Xây dựng `ModerationService` và ghi vết `MODERATION_LOG` | **NOT_STARTED** | Phase 3, 5 | Chưa bắt đầu. |
| **Phase 6** | **TASK-6.2** | Xây dựng `ModerationController` (`/api/v1/moderator/**`) | **NOT_STARTED** | TASK-6.1 | Chưa bắt đầu. |
| **Phase 6** | **TASK-6.3** | Viết kiểm thử tự động cho quy trình Phê duyệt và Từ chối bài học | **NOT_STARTED** | TASK-6.2 | Chưa bắt đầu. |
| **Phase 7** | **TASK-7.1** | Xây dựng thuật toán SM-2 trong `SrsService` (tính interval, ease factor) | **NOT_STARTED** | Phase 2 | Chưa bắt đầu. |
| **Phase 7** | **TASK-7.2** | Xây dựng `SrsController` (`/api/v1/flashcards/**`, `/api/v1/srs/**`) | **NOT_STARTED** | TASK-7.1 | Chưa bắt đầu. |
| **Phase 7** | **TASK-7.3** | Viết Unit Test toán học cho thuật toán tính toán khoảng cách ôn tập SRS | **NOT_STARTED** | TASK-7.2 | Chưa bắt đầu. |
| **Phase 8** | **TASK-8.1** | Xây dựng `PersonalNoteService` và `PersonalNoteController` (`/api/v1/notes/**`)| **NOT_STARTED** | Phase 3, 4 | Chưa bắt đầu. |
| **Phase 8** | **TASK-8.2** | Triển khai quản lý Ghi chú cá nhân (nội dung <= 500 ký tự) và Cài đặt SRS | **NOT_STARTED** | TASK-8.1 | Chưa bắt đầu. |
| **Phase 8** | **TASK-8.3** | Viết kiểm thử tự động cho phân hệ Ghi chú cá nhân và Cài đặt SRS | **NOT_STARTED** | TASK-8.2 | Chưa bắt đầu. |
| **Phase 9** | **TASK-9.1** | Xây dựng `frontend/js/api.js` tập trung, tự động gắn JWT và bắt lỗi 401 | **NOT_STARTED** | Phase 3 | Chưa bắt đầu. |
| **Phase 9** | **TASK-9.2** | Xây dựng giao diện web cho các màn hình (Xử lý đủ 3 trạng thái) | **NOT_STARTED** | TASK-9.1 | Chưa bắt đầu. |
| **Phase 9** | **TASK-9.3** | Kiểm tra hiển thị và tương tác trên trình duyệt bằng DevTools | **NOT_STARTED** | TASK-9.2 | Chưa bắt đầu. |
| **Phase 10** | **TASK-10.1** | Rà soát và gia cố bảo mật OWASP (XSS, SQLi, IDOR, File Upload) | **NOT_STARTED** | Phase 3-9 | Chưa bắt đầu. |
| **Phase 10** | **TASK-10.2** | Đạt độ phủ kiểm thử tự động toàn diện cho hệ thống backend | **NOT_STARTED** | Phase 3-8 | Chưa bắt đầu. |
| **Phase 11** | **TASK-11.1** | Thực thi kịch bản kiểm thử tích hợp đầu cuối (Full User Journey E2E) | **NOT_STARTED** | Phase 1-10 | Chưa bắt đầu. |
| **Phase 11** | **TASK-11.2** | Đóng gói ứng dụng và nghiệm thu bàn giao dự án | **NOT_STARTED** | TASK-11.1 | Chưa bắt đầu. |
