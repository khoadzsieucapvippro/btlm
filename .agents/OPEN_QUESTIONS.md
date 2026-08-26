# OPEN QUESTIONS — CÁC VẤN ĐỀ VÀ QUYẾT ĐỊNH CẦN XÁC NHẬN

> **Nguyên tắc:** Ghi nhận các vấn đề kỹ thuật, môi trường hoặc quyết định thiết kế.  
> Cập nhật trạng thái minh bạch khi câu hỏi đã được User ủy quyền / giải quyết hoặc bị bác bỏ.  

---

## 1. CÂU HỎI MỞ CÒN LẠI (REMAINING OPEN QUESTIONS)

### OQ-08: Lựa chọn Thư viện Giao diện Tĩnh cho Frontend (Phase 9)
- **ID:** OQ-08
- **Trạng thái:** **OPEN (Tạm hoãn đến Phase 9)**
- **Vấn đề:** Toàn bộ thư mục `frontend/` cũ đã bị xóa sạch sẽ để xây dựng lại từ đầu. Cần lựa chọn giải pháp CSS/JS tĩnh khi bước vào Phase 9 (Frontend Implementation).
- **Quyết định yêu cầu từ User khi đến Phase 9:**
  - *Phương án A:* Sử dụng Bootstrap qua CDN kết hợp CSS tùy biến tối giản và Vanilla JavaScript.
  - *Phương án B:* Tái sử dụng bộ template giao diện tĩnh eLearning (HTML Codex / Bootstrap 5 / Font Awesome) làm khung thiết kế nền.

---

## 2. CÁC CÂU HỎI MỞ ĐÃ ĐƯỢC GIẢI QUYẾT (RESOLVED DECISIONS)

### OQ-01: Cung cấp Môi trường MySQL Server Runtime
- **ID:** OQ-01
- **Trạng thái:** **RESOLVED**
- **Lý do & Kết quả:** User đã ủy quyền trực tiếp cho Agent cài đặt MySQL cục bộ. Agent đã cài đặt thành công **MySQL Community Server 8.4.9 LTS** trên Windows 11, khởi tạo datadir tại `C:\ProgramData\MySQL\MySQL Server 8.4\Data`, cấu hình `my.ini` (`utf8mb4` / `utf8mb4_unicode_ci`), chạy daemon trên cổng 3306 và tạo cơ sở dữ liệu `elearning_db`. Không gây ảnh hưởng tới SQL Server hiện có.

---

### OQ-02: Xác nhận Phiên bản Cụ thể cho Java và Spring Boot
- **ID:** OQ-02
- **Trạng thái:** **RESOLVED**
- **Lý do & Kết quả:** User chỉ đạo sử dụng phiên bản Java/JDK hiện đang được cài đặt trên máy trạm.
  - Kiểm tra thực tế: **OpenJDK 21.0.12 LTS** (Temurin-21.0.12+8) và **Maven 3.9.16**.
  - Lựa chọn phiên bản Spring Boot: **Spring Boot 3.3.x** (tương thích chính thức và ổn định nhất với Java 21 LTS).

---

### OQ-03: Khởi tạo Kho lưu trữ Phiên bản Git cục bộ
- **ID:** OQ-03
- **Trạng thái:** **RESOLVED**
- **Lý do & Kết quả:** User đã ủy quyền `git init`. Agent đã khởi tạo Git repository trên nhánh `main`, tạo file `.gitignore` tiêu chuẩn cho Java/Maven/Secrets, và đưa thư mục dự án sạch sẽ vào quản lý phiên bản.

---

### OQ-04: Thiết kế Vật lý cho Quan hệ Đa hình (`item_type` + `item_id`)
- **ID:** OQ-04
- **Trạng thái:** **RESOLVED trong DATABASE_DESIGN.md**
- **Lý do & Kết quả:** Chọn **Phương án A**: Giữ nguyên cặp trường `item_type` (`VARCHAR(20)`) và `item_id` (`BIGINT UNSIGNED`) theo đúng đặc tả của User. Không tạo FK vật lý ở MySQL; kiểm soát toàn vẹn dữ liệu chặt chẽ tại tầng Service / JPA.

---

### OQ-05: Quyết định Kiểu dữ liệu vật lý và Độ dài trường trên MySQL
- **ID:** OQ-05
- **Trạng thái:** **RESOLVED trong DATABASE_DESIGN.md**
- **Lý do & Kết quả:** Đã thiết kế bảng kiểu dữ liệu chi tiết cho toàn bộ 14 bảng trong `.agents/DATABASE_DESIGN.md` (khóa chính `BIGINT/INT UNSIGNED`, độ dài chuỗi tối ưu theo chuẩn `utf8mb4`, chỉ mục và ràng buộc duy nhất).

---

### OQ-06: Chiến lược Timestamp và Audit Trails
- **ID:** OQ-06
- **Trạng thái:** **RESOLVED trong DATABASE_DESIGN.md**
- **Lý do & Kết quả:** Giữ nguyên các trường timestamp nghiệp vụ bắt buộc (`PERSONAL_NOTE.created_at`, `MODERATION_LOG.created_at`, `REVIEW_LOG.reviewed_at`, `CARD_PROGRESS.next_review_at`). Bổ sung `created_at` và `updated_at` cho 5 bảng thực thể vòng đời cốt lõi (`ACCOUNT`, `USER_PROFILE`, `LESSON`, `RADICAL`, `VOCABULARY`).

---

### OQ-07: Ràng buộc Giới hạn Số lượng Ghi chú Cá nhân
- **ID:** OQ-07
- **Trạng thái:** **RESOLVED / REJECTED PROPOSAL (BÁC BỎ ĐỀ XUẤT)**
- **Lý do & Kết quả:** User đã khẳng định đề xuất giới hạn tối đa 5 ghi chú trên mỗi từ vựng không thuộc đặc tả nền tảng. Đặc tả thẩm quyền chỉ quy định `PERSONAL_NOTE.content <= 500 characters`. Hệ thống KHÔNG triển khai giới hạn 5 ghi chú này.
