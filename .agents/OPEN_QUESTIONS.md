# OPEN QUESTIONS — CÁC VẤN ĐỀ VÀ QUYẾT ĐỊNH CẦN XÁC NHẬN

> [!NOTE]
> **STATUS: HISTORICAL / ARCHIVED QUESTIONS**  
> **NOT CURRENT PROJECT CONTRACT**  
> All architectural and technical questions have been resolved. Current binding technical invariants and architecture are defined in `PROJECT-CONTRACT.md` and `CURRENT_STATE.md`.

> **Nguyên tắc:** Ghi nhận các vấn đề kỹ thuật, môi trường hoặc quyết định thiết kế.  
> Cập nhật trạng thái minh bạch khi câu hỏi đã được User ủy quyền / giải quyết hoặc bị bác bỏ.  

---

## 1. CÂU HỎI MỞ CÒN LẠI (REMAINING OPEN QUESTIONS)

*(Hiện tại không còn câu hỏi mở nào gây tắc nghẽn (No blocking open questions). Toàn bộ các quyết định kiến trúc, công nghệ và ranh giới nghiệp vụ từ Phase 0 đến Phase 11 đã được định hình rõ ràng, khả thi và đạt được đồng thuận tuyệt đối).*

---

## 2. CÁC CÂU HỎI MỞ ĐÃ ĐƯỢC GIẢI QUYẾT (RESOLVED DECISIONS)

### OQ-08: Lựa chọn Thư viện Giao diện Tĩnh cho Frontend (Phase 9)
- **ID:** OQ-08 (PEN-01)
- **Trạng thái:** **RESOLVED (Đặc tả chính thức tại Module 9A / ROADMAP.md)**
- **Lý do & Kết quả:** Thống nhất lựa chọn **Phương án A**: Sử dụng **Vanilla JavaScript thuần (ES6+ modular)** kết hợp **Bootstrap 5 qua CDN** và **CSS tùy biến tối giản** (`frontend/css/style.css`).
  - *Lợi ích:* Đạt mục tiêu zero-build complexity (không cần cấu hình Node.js build pipeline phức tạp cho runtime phục vụ), tốc độ tải trang cao, dễ dàng kiểm soát triệt để bề mặt tấn công DOM XSS, tương thích tuyệt đối với chính sách bảo mật nội dung chặt chẽ Content-Security-Policy (CSP), và cấu trúc mã nguồn gọn nhẹ, dễ bảo trì lâu dài.

---

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

---

### OQ-09: Nâng cấp Bảo mật Apache POI & Đánh giá Vòng đời Spring Boot
- **ID:** OQ-09
- **Trạng thái:** **RESOLVED (Task R3.7 / DEC-36)**
- **Lý do & Kết quả:** Đã nâng cấp `org.apache.poi:poi-ooxml` lên **5.4.0** vá hoàn toàn lỗ hổng OOXML duplicate ZIP entry validation CVE-2025-31672, giữ nguyên Spring Boot 3.3.5 tương thích 100% với `commons-lang3:3.14.0`, bổ sung test suite adversarial `ExcelParserServiceTests$CveAndHardeningSecurityTests`.

---

### OQ-10: Bổ sung Phân trang cho Endpoint Personal Notes
- **ID:** OQ-10
- **Trạng thái:** **RESOLVED (Task R3.5 / DEC-34)**
- **Lý do & Kết quả:** Đã bổ sung phân trang (`Pageable`) cho `GET /api/v1/vocabularies/{vocabId}/notes` (default 20, max 100, stable sort `createdAt DESC`), ngăn chặn rủi ro DoS / memory exhaustion (OWASP API4:2023).

---

### OQ-11: Chiến lược Quản lý Bộ nhớ cho Login Rate Limiter
- **ID:** OQ-11
- **Trạng thái:** **RESOLVED (Task R3.6 / DEC-35)**
- **Lý do & Kết quả:** Đã triển khai cơ chế thu hồi key IP rác tự động bằng `attemptHistory.remove(key, timestamps)` khi Deque trống sau khi cắt tỉa timestamp ngoài cửa sổ sliding window.

---

### OQ-12: Cấu hình Reverse Proxy Header Forwarding cho Môi trường Production
- **ID:** OQ-12
- **Trạng thái:** **RESOLVED (Task R3.10 / DEC-40)**
- **Lý do & Kết quả:** Đã tích hợp `ForwardedHeaderFilter` trong Spring Security cấu hình và thiết lập `server.forward-headers-strategy: ${SERVER_FORWARD_HEADERS_STRATEGY:framework}` trong `application.yml`, kiểm chứng trích xuất đúng client IP thực tế từ header `X-Forwarded-For` cho login rate limiting.
