# PROJECT CONTEXT — HỆ THỐNG HỌC BỘ THỦ VÀ TỪ VỰNG TIẾNG TRUNG

> **Trạng thái:** Phase 0-3 COMPLETED, Phase 4 IN PROGRESS (Module 4A Task 4A.1 COMPLETED, Task 4A.2 CURRENT NEXT TASK)  
> **Current Implementation:** Phase 0, 1, 2, 3 hoàn tất (201/201 tests PASS). Đang triển khai Phase 4 (Module 4A: Radical Catalog).  
> **Cập nhật lần cuối:** 2026-08-27  

---

## 1. SYSTEM GOAL & PROJECT SCOPE (MỤC TIÊU & PHẠM VI DỰ ÁN)

Xây dựng hệ thống Website học Bộ thủ (214 bộ thủ Khang Hy) và Từ vựng Tiếng Trung hoàn chỉnh, ứng dụng thuật toán lặp lại ngắt quãng (**Spaced Repetition System - SRS**) giúp người học tối ưu hóa khả năng ghi nhớ từ vựng lâu dài. Hệ thống phục vụ 4 nhóm người dùng với luồng nghiệp vụ khép kín: Người học (Learner), Tác giả bài học (Creator), Người kiểm duyệt (Moderator), và Quản trị viên (Admin).

---

## 2. USER ROLES (CÁC VAI TRÒ NGƯỜI DÙNG)

Hệ thống quản lý phân quyền theo mô hình **Role-Based Access Control (RBAC)** với 4 vai trò chính thức trong bảng `ROLE`:

### 2.1. Learner (Vai trò `1 = Learner`)
- Đăng ký tài khoản, đăng nhập bằng Email hoặc Số điện thoại.
- Tra cứu danh mục 214 Bộ thủ Khang Hy và Từ vựng công khai.
- Xem danh sách và chi tiết các bài học đã được kiểm duyệt và phê duyệt (`Approved`).
- Học và ôn tập Flashcard theo thuật toán SRS (đánh giá 4 mức độ nhớ: `1 = Again`, `2 = Hard`, `3 = Good`, `4 = Easy`).
- Cấu hình chỉ số SRS cá nhân (`new_cards_per_day` mặc định `20`, `max_review_per_day` mặc định `100`).
- Tạo và quản lý ghi chú cá nhân (**Personal Note**) cho từng từ vựng (nội dung tối đa 500 ký tự).

### 2.2. Creator (Vai trò `2 = Creator`)
- Mang đầy đủ quyền hạn của Learner.
- Quản lý bài học do chính mình tạo ra theo vòng đời trạng thái (`Draft` -> `Pending` -> `Approved` / `Rejected`).
- Import danh sách từ vựng vào bài học từ file Excel.
- Gửi bài học lên hàng đợi kiểm duyệt (`Pending`).
- Chỉnh sửa/cập nhật lại bài học bị từ chối (`Rejected`) để gửi duyệt lại.

### 2.3. Moderator (Vai trò `3 = Moderator`)
- Xem hàng đợi các bài học đang chờ duyệt (`Pending`).
- Kiểm tra nội dung chi tiết bài học và danh sách từ vựng đính kèm.
- Phê duyệt bài học (`Approved`) để công khai cho toàn bộ người học.
- Từ chối bài học (`Rejected`) kèm bắt buộc nhập lý do từ chối (`rejection_reason`) và danh sách trường vi phạm (`flagged_fields` dạng chuỗi JSON).
- Xem lịch sử các thao tác kiểm duyệt trong `MODERATION_LOG`.

### 2.4. Admin (Vai trò `4 = Admin`)
- Quản trị toàn bộ tài khoản người dùng: phân quyền Role (`1 = Learner`, `2 = Creator`, `3 = Moderator`, `4 = Admin`), cập nhật trạng thái tài khoản (`Active`, `Inactive`, `Banned`).
- Quản trị dữ liệu danh mục gốc: CRUD 214 Bộ thủ, CRUD Từ vựng hệ thống.
- Giám sát toàn bộ bài học và hoạt động hệ thống.

---

## 3. MAIN DOMAINS (CÁC PHÂN HỆ NGHIỆP VỤ CHÍNH)

1. **Authentication & Identity:** Quản lý tài khoản đăng nhập (`ACCOUNT`), hồ sơ (`USER_PROFILE`), phân quyền nhiều-nhiều qua `ACCOUNT_ROLE`, cấp phát và xác thực JWT Bearer Token (Stateless).
2. **Radical Domain:** Quản lý 214 bộ thủ Khang Hy tiêu chuẩn trong `RADICAL`: Ký tự (`character`), Pinyin, Âm Hán-Việt, Nghĩa tiếng Việt, URL audio phát âm, URL video nét viết.
3. **Vocabulary Domain:** Quản lý từ vựng trong `VOCABULARY`: Chữ Hán (`hanzi`), Pinyin có dấu (`pinyin`), Pinyin thô (`pinyin_raw`), Âm Hán-Việt, Nghĩa tiếng Việt, câu ví dụ & dịch nghĩa, media. Quan hệ nhiều-nhiều với Bộ thủ qua bảng liên kết `VOCAB_RADICAL`.
4. **Lesson Management:** Quản lý bài học trong `LESSON`, liên kết từ vựng qua `LESSON_VOCABULARY` kèm thứ tự `order_index`. Import Excel từ Creator. Vòng đời trạng thái: `Draft` -> `Pending` -> `Approved` / `Rejected`.
5. **Content Moderation:** Quy trình kiểm duyệt bài học bởi Moderator, ghi vết toàn diện vào `MODERATION_LOG` (kèm `rejection_reason` bắt buộc khi từ chối và `flagged_fields` dạng JSON).
6. **Flashcard & SRS Engine:** Quản lý tiến trình học từng thẻ của từng user qua `CARD_PROGRESS`, thuật toán tính toán lặp lại ngắt quãng SM-2 (`ease_factor` mặc định 2.50, `interval_days`, `repetitions`, `next_review_at`). Hỗ trợ tham chiếu đa hình `item_type` (`VOCABULARY` hoặc `RADICAL`) và `item_id`. Cài đặt giới hạn trong `USER_SRS_SETTING`.
7. **Review Tracking:** Ghi nhận nhật ký từng lượt lật thẻ và đánh giá (1-4) vào `REVIEW_LOG` kèm thời gian phản xạ `review_time_seconds`.
8. **Personal Notes:** Ghi chú cá nhân của người học cho từ vựng trong `PERSONAL_NOTE` (nội dung tối đa 500 ký tự).

---

## 4. CONFIRMED TECHNOLOGY STACK (BASELINE ĐÃ PHÊ DUYỆT)

* **Backend:** Java, Spring Boot, Spring MVC, Maven.
* **Backend Architecture:** `Client/Frontend -> REST API -> Controller -> Service -> Repository -> MySQL`.
* **Database & Migration:** MySQL, Flyway Migration, Spring Data JPA / Hibernate.
* **Security:** Spring Security, JWT, Role-Based Access Control.
* **Frontend:** HTML, CSS, JavaScript (các thư viện tĩnh hiện có chỉ được sử dụng khi được lựa chọn cụ thể cho lần triển khai mới). Tuyệt đối không dùng framework SPA (React, Vue, Angular, Next.js, Nuxt).
* **API Standard:** RESTful API, JSON Request/Response.
* **API Testing:** Postman.
* **Version Control:** Git, GitHub.
* **IDE:** Visual Studio Code.
