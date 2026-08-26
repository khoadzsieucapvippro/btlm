# ROADMAP — LỘ TRÌNH TRIỂN KHAI DỰ ÁN MỚI TỪ ĐẦU (REBUILT FROM SCRATCH)

> **Nguyên tắc khởi tạo:** Lộ trình bắt đầu từ trạng thái sạch hoàn toàn (Clean Slate).  
> **Trạng thái thực thi:** Tuyệt đối KHÔNG đánh dấu bất kỳ Phase triển khai nào là COMPLETED dựa trên mã nguồn cũ đã xóa.  
> **Lưu ý:** Không áp đặt quy tắc 1 bảng = 1 JPA Entity. Cấu trúc ánh xạ Entity được thiết kế linh hoạt theo mô hình miền nghiệp vụ.  

---

## TỔNG QUAN 12 GIAI ĐOẠN (ROADMAP OVERVIEW)

```
Phase 0: Project Specification and Architecture Baseline (Hiện tại)
   │
   ▼
Phase 1: Spring Boot Project Foundation (Maven, Git, Base Exception Handling, Test Structure)
   │
   ▼
Phase 2: MySQL, Flyway and Persistence Foundation (14 Tables DDL Migrations, Entity Mapping, Repositories)
   │
   ▼
Phase 3: Authentication, JWT and RBAC (SecurityFilterChain, Login/Register, Role Access)
   │
   ▼
Phase 4: Radical and Vocabulary Domain (214 Radicals, Vocabulary, DTOs, Search & Pagination)
   │
   ▼
Phase 5: Lesson and Content Management (Creator APIs, Excel Import POI, Lifecycle Draft/Pending)
   │
   ▼
Phase 6: Content Moderation (Moderator Queue, Approve/Reject Invariants, Moderation Log)
   │
   ▼
Phase 7: SRS Engine (SM-2 Algorithm, Card Progress, Review Scheduling)
   │
   ▼
Phase 8: Learning and Review Features (Personal Notes Max 500 Chars, SRS Settings)
   │
   ▼
Phase 9: Frontend Implementation and API Integration (HTML/CSS/JS, api.js, 3 UI States)
   │
   ▼
Phase 10: Testing, Security and Quality (Unit & MockMvc Tests, OWASP Hardening)
   │
   ▼
Phase 11: Final Integration and Validation (Full User Journey E2E, Deployment Delivery)
```

---

## NỘI DUNG CHI TIẾT TỪNG GIAI ĐOẠN

### Phase 0 — Project Specification and Architecture Baseline
- **Mục tiêu:** Xóa bỏ toàn bộ mã nguồn và artifacts cũ; thiết lập lại baseline công nghệ, tài liệu đặc tả 14 bảng chuẩn và quy trình kỹ thuật.
- **Phạm vi:**
  - Xóa sạch mã nguồn cũ trong `backend/`, `frontend/`, `scripts/`, `temp/`, `docs/`.
  - Giữ nguyên và xác minh 19 Agent Skills trong `.agents/skills/`.
  - Rebuild bộ tài liệu đặc tả chuẩn trong `.agents/` (`PROJECT_CONTEXT`, `ARCHITECTURE`, `DATABASE`, `API`, `DECISIONS`, `OPEN_QUESTIONS`, `WORKFLOW`, `ROADMAP`, `PROGRESS`).
- **Trạng thái:** `COMPLETED`.

---

### Phase 1 — Spring Boot Project Foundation
- **Mục tiêu:** Khởi tạo cấu trúc dự án Spring Boot sạch sẽ và thiết lập môi trường phiên bản.
- **Phạm vi:**
  - Khởi tạo kho lưu trữ Git cục bộ (`git init`) và cấu hình file `.gitignore` chuẩn cho Java/Maven.
  - Tạo mới file `backend/pom.xml` với các dependency chính thức: Spring Web, Spring Security, Validation, Spring Data JPA, MySQL Connector, Flyway, JJWT, Apache POI, Lombok.
  - Tạo class khởi động chính `ElearningApplication.java` và cấu hình `application.yml` trỏ tới MySQL.
  - Cấu hình chuẩn phong bì `ApiResponse<T>`, `PageResponse<T>`, và `GlobalExceptionHandler` bắt toàn diện các exception.
  - Khởi tạo thư mục `backend/src/test` với kiểm thử kiểm tra nạp Spring Context cơ bản.
- **Trạng thái:** `COMPLETED`.

---

### Phase 2 — MySQL, Flyway and Persistence Foundation
- **Mục tiêu:** Xây dựng nền tảng lưu trữ dữ liệu 14 bảng trên MySQL và lớp ánh xạ Persistence JPA.
- **Phạm vi:**
  - Soạn thảo các file Flyway migration mới cho 14 bảng chuẩn:
    - `V1__init_schema.sql`: 14 bảng quan hệ, khóa chính, khóa ngoại, chỉ mục.
    - `V2__seed_roles.sql`: Seed 4 vai trò cố định (`1=Learner`, `2=Creator`, `3=Moderator`, `4=Admin`).
    - `V3__seed_radicals.sql`: Seed 214 Bộ thủ Khang Hy từ dữ liệu đã xác minh (`.agents/references/radicals.json`).
  - Thiết kế và triển khai lớp JPA Entities tương ứng theo mô hình miền (không áp đặt 1 bảng = 1 Entity, xử lý quan hệ N:N và quan hệ đa hình phù hợp).
  - Tạo các Spring Data JPA Repositories.
  - Chạy migration Flyway và kiểm thử truy cập dữ liệu.
- **Trạng thái:** `NOT_STARTED`.

---

### Phase 3 — Authentication, JWT and RBAC
- **Mục tiêu:** Triển khai phân hệ quản lý định danh và bảo mật phân quyền theo vai trò.
- **Phạm vi:**
  - Xây dựng `SecurityConfig`, `JwtUtil`, `JwtAuthenticationFilter`, `CustomUserDetailsService`.
  - Tạo Request/Response DTOs: `LoginRequest`, `RegisterRequest`, `LoginResponse`.
  - Tạo `AuthService` và `AuthController` (`POST /api/v1/auth/register`, `POST /api/v1/auth/login`).
  - Viết bộ kiểm thử tự động (Unit test và MockMvc integration test) cho Đăng ký, Đăng nhập, và bảo vệ endpoint theo quyền truy cập.
- **Trạng thái:** `NOT_STARTED`.

---

### Phase 4 — Radical and Vocabulary Domain
- **Mục tiêu:** Triển khai phân hệ dữ liệu danh mục 214 Bộ thủ và Từ vựng hệ thống.
- **Phạm vi:**
  - Thiết kế các DTO độc lập: `RadicalDto`, `VocabularyDto`, `RadicalRequest`, `VocabularyRequest`.
  - Triển khai Service và Controller cho tra cứu bộ thủ và từ vựng (`GET /api/v1/radicals`, `GET /api/v1/vocabulary`).
  - Đảm bảo trả về chuẩn phân trang `ApiResponse<PageResponse<T>>`.
  - Viết kiểm thử tự động cho chức năng tra cứu và phân trang.
- **Trạng thái:** `NOT_STARTED`.

---

### Phase 5 — Lesson and Content Management
- **Mục tiêu:** Xây dựng phân hệ quản lý bài học và import từ vựng cho Creator.
- **Phạm vi:**
  - Thiết kế `LessonDto`, `ImportValidationReport`.
  - Triển khai `ExcelImportService` sử dụng Apache POI xử lý file Excel theo luồng 2 bước (Preview/Validate -> Confirm).
  - Triển khai `CreatorLessonController` (`/api/v1/creator/lessons/**`) và `LessonController` (`/api/v1/lessons/**`).
  - Kiểm soát vòng đời bài học (`Draft` -> `Pending`), kiểm tra quyền sở hữu bài học.
  - Viết kiểm thử tự động cho việc import file Excel và tạo bài học.
- **Trạng thái:** `NOT_STARTED`.

---

### Phase 6 — Content Moderation
- **Mục tiêu:** Xây dựng hàng đợi và quy trình kiểm duyệt bài học cho Moderator.
- **Phạm vi:**
  - Thiết kế `ModerationLogDto`, `RejectRequest`.
  - Triển khai `ModerationService` và `ModerationController` (`/api/v1/moderator/**`).
  - Xử lý logic Phê duyệt (`Approve`) và Từ chối (`Reject` kèm lý do bắt buộc và chuỗi JSON `flagged_fields`).
  - Tự động ghi vết vào bảng `MODERATION_LOG`.
  - Viết kiểm thử tự động cho phân quyền kiểm duyệt và ghi log.
- **Trạng thái:** `NOT_STARTED`.

---

### Phase 7 — SRS Engine
- **Mục tiêu:** Xây dựng cỗ máy tính toán lặp lại ngắt quãng (Spaced Repetition System).
- **Phạm vi:**
  - Thiết kế `ReviewRequest`, `SrsSettingDto`, `FlashcardItemDto`.
  - Triển khai thuật toán tính toán SM-2 trong `SrsService`: tính `ease_factor` (mặc định 2.50), `interval_days`, `repetitions`, `next_review_at` dựa trên Rating 1–4.
  - Triển khai `SrsController`: lấy danh sách thẻ cần ôn tập hôm nay, gửi kết quả đánh giá (ghi nhận `review_time_seconds`), quản lý cài đặt SRS cá nhân.
  - Ghi nhật ký từng lần đánh giá vào `REVIEW_LOG`.
  - Viết unit test toán học kiểm chứng độ chính xác của thuật toán SRS.
- **Trạng thái:** `NOT_STARTED`.

---

### Phase 8 — Learning and Review Features
- **Mục tiêu:** Cung cấp tính năng ghi chú cá nhân và hoàn thiện trải nghiệm học tập.
- **Phạm vi:**
  - Thiết kế `PersonalNoteDto`, `PersonalNoteRequest`.
  - Triển khai `PersonalNoteService` và `PersonalNoteController` (`/api/v1/notes/**`).
  - Thực thi ràng buộc nghiệp vụ: độ dài `content` tối đa 500 ký tự.
  - Viết kiểm thử tự động cho phân hệ ghi chú.
- **Trạng thái:** `NOT_STARTED`.

---

### Phase 9 — Frontend Implementation and API Integration
- **Mục tiêu:** Xây dựng giao diện web chuẩn và tích hợp RESTful API hoàn chỉnh.
- **Phạm vi:**
  - Xây dựng module `frontend/js/api.js` đóng vai trò HTTP client tập trung, tự động gắn JWT và bắt lỗi 401.
  - Xây dựng module `frontend/js/auth.js` điều phối hiển thị menu theo trạng thái đăng nhập và vai trò.
  - Triển khai giao diện cho các màn hình: Trang chủ, Danh mục Bộ thủ, Danh mục Từ vựng, Bài học, Flashcard ôn tập SRS, Trang Creator import bài học, Dashboard Moderator kiểm duyệt, Dashboard Admin quản lý tài khoản.
  - Xử lý triệt để 3 trạng thái giao diện: Loading, Empty, Error.
  - Kiểm tra giao diện và tính tương thích trên trình duyệt qua DevTools.
- **Trạng thái:** `NOT_STARTED`.

---

### Phase 10 — Testing, Security and Quality
- **Mục tiêu:** Gia cố an ninh bảo mật và hoàn thiện bộ kiểm thử tự động.
- **Phạm vi:**
  - Rà soát các lỗ hổng bảo mật OWASP: SQL Injection, XSS, IDOR (kiểm tra quyền sở hữu), Path Traversal, File Upload validation.
  - Tối ưu hóa cấu hình CORS, bảo mật JWT secret.
  - Đạt độ phủ kiểm thử tự động (Unit & Integration tests) toàn diện cho tất cả các Service.
- **Trạng thái:** `NOT_STARTED`.

---

### Phase 11 — Final Integration and Validation
- **Mục tiêu:** Kiểm thử tích hợp toàn diện đầu cuối (End-to-End) và sẵn sàng bàn giao.
- **Phạm vi:**
  - Chạy kịch bản người dùng xuyên suốt (Full User Journey) từ Đăng ký -> Học tập -> Tạo bài -> Kiểm duyệt -> Ôn tập SRS -> Quản trị.
  - Đóng gói ứng dụng thành file jar có thể triển khai độc lập.
  - Hoàn thiện tài liệu hướng dẫn vận hành và bàn giao.
- **Trạng thái:** `NOT_STARTED`.
