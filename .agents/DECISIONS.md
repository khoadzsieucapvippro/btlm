# DECISIONS — CÁC QUYẾT ĐỊNH KỸ THUẬT CỦA DỰ ÁN

> **Nguyên tắc quản lý quyết định:**  
> - Phân định rõ ràng giữa:  
>   1. **Quyết định đã được User phê duyệt (User-Approved Decisions)**  
>   2. **Quyết định thiết kế kỹ thuật vật lý (Technical Design Decisions)**  
>   3. **Quyết định còn chờ phê duyệt (Pending Approval Decisions)**  

---

## 1. QUYẾT ĐỊNH ĐÃ ĐƯỢC USER PHÊ DUYỆT (USER-APPROVED DECISIONS)

| ID | Phạm vi | Quyết định kỹ thuật | Chi tiết & Căn cứ phê duyệt |
| :--- | :--- | :--- | :--- |
| **DEC-01** | Implementation | **Rebuild From Scratch** | Xóa bỏ 100% mã nguồn và artifacts cũ. Xây dựng lại ứng dụng sạch sẽ từ đầu. |
| **DEC-02** | Database Spec | **14 Authoritative Tables** | Cơ sở dữ liệu nghiệp vụ gồm đúng 14 bảng theo đặc tả thẩm quyền. Không giảm thành 11 bảng. |
| **DEC-03** | Runtime / Java | **Java 21 LTS** | Sử dụng phiên bản JDK hiện có trên máy trạm: OpenJDK 21.0.12 LTS (Temurin-21.0.12+8). Không cài đặt đè Java khác. |
| **DEC-04** | Framework | **Spring Boot 3.3.x** | Sử dụng Spring Boot thế hệ 3.3.x để tương thích ổn định với Java 21 LTS và hệ sinh thái Spring Data JPA, Spring Security. |
| **DEC-05** | Database Server | **MySQL Community Server 8.4 LTS** | User đã ủy quyền cài đặt cục bộ. Agent đã cài đặt và cấu hình MySQL 8.4.9 trên cổng 3306, database `elearning_db`. Giữ nguyên SQL Server hiện có. |
| **DEC-06** | Version Control | **Git & GitHub** | User ủy quyền khởi tạo `git init`, file `.gitignore` tiêu chuẩn và thực hiện initial commit. |
| **DEC-07** | Personal Notes Rule | **Bác bỏ giới hạn 5 ghi chú** | Bác bỏ đề xuất giới hạn tối đa 5 ghi chú/từ vựng. Chỉ áp dụng duy nhất quy tắc đặc tả: `content <= 500 characters`. |
| **DEC-08** | Architecture | **Spring MVC Phân tầng** | `Client/Frontend -> RESTful API -> Controller -> Service -> Repository -> MySQL`. |
| **DEC-09** | API Standard | **RESTful API / JSON** | Giao tiếp qua chuẩn RESTful API, JSON payload, tiền tố thống nhất `/api/v1/...`, phong bì `ApiResponse<T>`. |
| **DEC-10** | DTO Boundary | **DTO Isolation (Zero Entity Leak)** | 100% request và response sử dụng DTO. Tuyệt đối cấm trả trực tiếp JPA Entity ra API. |
| **DEC-11** | Build Tool | **Apache Maven 3.9.16** | Sử dụng Apache Maven hiện có trên máy trạm để quản lý build và dependencies. |
| **DEC-12** | Migration Engine | **Flyway Migration** | Toàn quyền kiểm soát database schema qua Flyway. Cấm bật `ddl-auto=update`. |
| **DEC-13** | Security & Auth | **Spring Security & JWT** | Xác thực Stateless qua header `Authorization: Bearer <token>`, mật khẩu mã hóa BCrypt, phân quyền RBAC 4 vai trò. |
| **DEC-14** | Frontend Core | **HTML, CSS, JavaScript** | Công nghệ web tiêu chuẩn. Tuyệt đối không dùng framework SPA (React, Vue, Angular, Next.js, Nuxt). |
| **DEC-15** | Testing Tool | **Postman** | Postman là công cụ chuẩn mực kiểm thử REST API. |

---

## 2. QUYẾT ĐỊNH THIẾT KẾ KỸ THUẬT VẬT LÝ (TECHNICAL DESIGN DECISIONS)

*Chi tiết tại `.agents/DATABASE_DESIGN.md`*:

| ID | Phạm vi | Quyết định thiết kế | Chi tiết kỹ thuật |
| :--- | :--- | :--- | :--- |
| **DES-01** | Primary Keys | **BIGINT & INT UNSIGNED** | `INT UNSIGNED` cho bảng danh mục nhỏ (`ROLE`, `RADICAL`); `BIGINT UNSIGNED` cho toàn bộ các bảng nghiệp vụ còn lại. |
| **DES-02** | Key Generation | **AUTO_INCREMENT (IDENTITY)** | Sử dụng `AUTO_INCREMENT` trong MySQL, tương ứng `@GeneratedValue(strategy = GenerationType.IDENTITY)` trong JPA. |
| **DES-03** | Junction Tables | **Composite Primary Keys** | `ACCOUNT_ROLE (account_id, role_id)`, `VOCAB_RADICAL (vocab_id, radical_id)`, `LESSON_VOCABULARY (lesson_id, vocab_id)`. |
| **DES-04** | Polymorphic Reference | **Phương án A: item_type + item_id** | Giữ nguyên 2 trường như đặc tả, không tạo FK vật lý ở MySQL; kiểm soát toàn vẹn tại tầng Service / JPA. |
| **DES-05** | Charset / Collation | **utf8mb4 / utf8mb4_unicode_ci** | Hỗ trợ toàn diện 4-byte Unicode cho chữ Hán Khang Hy, Pinyin có dấu và tiếng Việt. |
| **DES-06** | Timestamps | **Specific + Lifecycle Auditing** | Giữ nguyên các timestamp bắt buộc (`PERSONAL_NOTE.created_at`, `MODERATION_LOG.created_at`, `REVIEW_LOG.reviewed_at`, `CARD_PROGRESS.next_review_at`). Bổ sung `created_at` và `updated_at` cho 5 bảng thực thể chính (`ACCOUNT`, `USER_PROFILE`, `LESSON`, `RADICAL`, `VOCABULARY`). |
| **DES-07** | Cascade Strategy | **Bảo vệ toàn vẹn lịch sử** | Áp dụng `ON DELETE RESTRICT` cho tác giả bài học, kiểm duyệt viên, vai trò hệ thống, và từ vựng trong bài học để chống mất dấu vết kiểm toán. |

---

## 3. QUYẾT ĐỊNH CÒN CHỜ PHÊ DUYỆT (PENDING USER APPROVAL)

- **PEN-01:** Lựa chọn giải pháp thư viện giao diện tĩnh cho Frontend (Bootstrap CDN vs Template eLearning tĩnh cũ) tại Phase 9.
