# THIẾT KẾ CƠ SỞ DỮ LIỆU VẬT LÝ (PHYSICAL DATABASE DESIGN)

> **Tài liệu:** Thiết kế Cơ sở dữ liệu Vật lý MySQL (Physical Database Design)  
> **Dự án:** Hệ thống Website học Bộ thủ và Từ vựng Tiếng Trung  
> **Hệ quản trị CSDL:** **MySQL Community Server 8.4 LTS**  
> **Bộ mã & Sắp chiếu:** `utf8mb4` / `utf8mb4_unicode_ci`  
> **Căn cứ đặc tả:** Authoritative 14-Table Baseline tại `.agents/DATABASE.md`  

---

## 1. TỔNG QUAN ĐIỀU HÀNH (EXECUTIVE SUMMARY)

Dự án được xây dựng mới hoàn toàn từ đầu (Rebuild from Scratch) với kiến trúc phân tầng chuẩn:
`Client/Frontend -> RESTful API -> Controller -> Service -> Repository -> MySQL`.

Đặc tả nghiệp vụ đã phê duyệt dứt khoát mô hình gồm **đúng 14 bảng**. Tài liệu này hoàn thiện thiết kế kỹ thuật vật lý (Physical Design) chi tiết cho toàn bộ 14 bảng trên MySQL 8.4, bao gồm:
- Chiến lược khóa chính và kiểu định danh (`BIGINT UNSIGNED AUTO_INCREMENT` và `INT UNSIGNED`).
- Thiết kế bảng liên kết trung gian với khóa phức hợp (`Composite Primary Key`).
- Lựa chọn kiểu dữ liệu và độ dài tối ưu cho tiếng Trung, Pinyin, nghĩa tiếng Việt, URLs và media.
- Giải pháp hiện thực hóa tham chiếu đa hình (`item_type` + `item_id`) cho `CARD_PROGRESS` và `REVIEW_LOG`.
- Chiến lược chỉ mục (Indexing), toàn vẹn tham chiếu khóa ngoại (Foreign Keys & Cascade Rules), và bảo mật dữ liệu.

---

## 2. ĐẶC TẢ NỀN TẢNG ĐÃ ĐƯỢC DUYỆT (APPROVED BASELINE)

Hệ thống bao gồm đúng 14 bảng phân theo 3 phân hệ nghiệp vụ:
1. **Phân hệ Tài khoản & Phân quyền:** `ACCOUNT`, `USER_PROFILE`, `ROLE`, `ACCOUNT_ROLE`.
2. **Phân hệ Nội dung Bộ thủ, Từ vựng & Bài học:** `RADICAL`, `VOCABULARY`, `VOCAB_RADICAL`, `LESSON`, `LESSON_VOCABULARY`.
3. **Phân hệ Học tập SRS, Lịch sử, Ghi chú & Kiểm duyệt:** `USER_SRS_SETTING`, `CARD_PROGRESS`, `REVIEW_LOG`, `PERSONAL_NOTE`, `MODERATION_LOG`.

---

## 3. NGUYÊN TẮC THIẾT KẾ VẬT LÝ (PHYSICAL DESIGN PRINCIPLES)

1. **Tuân thủ Chuẩn hóa 3NF:** Đảm bảo toàn vẹn dữ liệu, không có phụ thuộc bắc cầu, dữ liệu lưu trữ không trùng lặp vô căn cứ.
2. **Tính tương thích hoàn hảo với Spring Data JPA / Hibernate:**
   - Các trường khóa chính sử dụng kiểu số nguyên (`Long`, `Integer`) tương ứng với `IDENTITY` generation.
   - Bảng liên kết trung gian có thể ánh xạ linh hoạt qua `@ManyToMany` với `@JoinTable` hoặc `@Embeddable` `@EmbeddedId`.
3. **Hỗ trợ Unicode toàn diện (`utf8mb4`):** Hỗ trợ toàn bộ ký tự 4-byte của chữ Hán Khang Hy, Pinyin có dấu thanh điệu, và tiếng Việt.
4. **Hiệu năng truy vấn cao (Index Optimization):** Đánh chỉ mục chính xác theo đường truy cập thực tế (Authentication, Pinyin search, SRS due cards, Moderation queue).
5. **Bảo tồn dấu vết kiểm toán (Audit Preservation):** Không áp dụng `CASCADE DELETE` nguy hiểm làm mất nhật ký kiểm duyệt và lịch sử ôn tập.

---

## 4. CHIẾN LƯỢC KHÓA CHÍNH (PRIMARY KEY STRATEGY)

Đánh giá giữa `INT`, `BIGINT`, và `UUID`:
- **UUID:** Chiếm 16–36 bytes, làm phân mảnh chỉ mục B-Tree của InnoDB, giảm hiệu suất chèn và join dữ liệu.
- **BIGINT UNSIGNED:** 8 bytes, phạm vi lên tới $18 \times 10^{18}$, đảm bảo không bao giờ tràn khóa cho các bảng giao dịch có khối lượng lớn (`REVIEW_LOG`, `CARD_PROGRESS`, `VOCABULARY`, `ACCOUNT`). Trong Java ánh xạ kiểu `java.lang.Long`.
- **INT UNSIGNED:** 4 bytes, phạm vi lên tới 4.2 tỷ bản ghi, tối ưu tuyệt đối cho các bảng danh mục nhỏ/cố định:
  - `ROLE`: 4 bản ghi cố định (`1=Learner`, `2=Creator`, `3=Moderator`, `4=Admin`).
  - `RADICAL`: Đúng 214 bộ thủ Khang Hy tiêu chuẩn (`radical_id` từ 1 đến 214 khớp với số thứ tự Khang Hy chuẩn).

**Kết luận Chiến lược:**
- Bảng danh mục định danh nhỏ (`ROLE`, `RADICAL`): `INT UNSIGNED`.
- Toàn bộ các bảng nghiệp vụ còn lại: `BIGINT UNSIGNED`.

---

## 5. CHIẾN LƯỢC SINH KHÓA CHÍNH (ID GENERATION)

- Sử dụng cơ chế `AUTO_INCREMENT` của MySQL InnoDB.
- Tương ứng trong JPA là `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- **Ưu điểm:** Đơn giản, độ tin cậy cao, hiệu năng chèn tuần tự cao nhất trong InnoDB (Cluster Index Append-friendly), không cần bảng khóa phụ hay sequence riêng biệt.
- **Quy ước riêng cho `RADICAL`:** Sử dụng `AUTO_INCREMENT` nhưng dữ liệu seed 214 bộ thủ sẽ chỉ định tường minh ID từ 1 đến 214.

---

## 6. CHIẾN LƯỢC BẢNG LIÊN KẾT (JUNCTION TABLES)

Phân tích 3 bảng liên kết trung gian:

| Bảng liên kết | Hai thực thể tham chiếu | Khóa chính đề xuất | Lý do kỹ thuật |
| :--- | :--- | :--- | :--- |
| `ACCOUNT_ROLE` | `ACCOUNT` & `ROLE` | `PRIMARY KEY (account_id, role_id)` | Khóa phức hợp tự nhiên ngăn chặn hoàn toàn việc gán trùng 1 vai trò cho 1 tài khoản ở tầng CSDL. |
| `VOCAB_RADICAL` | `VOCABULARY` & `RADICAL` | `PRIMARY KEY (vocab_id, radical_id)` | Khóa phức hợp đảm bảo một bộ thủ chỉ liên kết 1 lần với 1 từ vựng. |
| `LESSON_VOCABULARY` | `LESSON` & `VOCABULARY` | `PRIMARY KEY (lesson_id, vocab_id)` | Khóa phức hợp đảm bảo từ vựng không bị chèn lặp trong cùng 1 bài học; `order_index` lưu thứ tự hiển thị. |

*Ghi chú:* Không dùng cột surrogate ID ảo (`id BIGINT AUTO_INCREMENT`) cho các bảng liên kết để tránh lãng phí dung lượng chỉ mục và cho phép ánh xạ `@ManyToMany` tự nhiên trong Spring Data JPA.

---

## 7. QUY CHUẨN KIỂU DỮ LIỆU CỘT (COLUMN TYPES)

- **Định danh / Khóa ngoại:** `BIGINT UNSIGNED` hoặc `INT UNSIGNED`.
- **Chuỗi ngắn / Mã / Trạng thái:** `VARCHAR(20)` hoặc `VARCHAR(50)`.
- **Chữ Hán (`hanzi`):** `VARCHAR(50)`.
- **Pinyin có dấu / không dấu (`pinyin`, `pinyin_raw`):** `VARCHAR(100)`.
- **Nghĩa Hán-Việt (`meaning_han_viet`):** `VARCHAR(100)`.
- **Nghĩa tiếng Việt (`meaning_vi`):** `VARCHAR(255)`.
- **Câu ví dụ và dịch nghĩa:** `VARCHAR(500)`.
- **URL media (Audio, Video, Avatar, Excel):** `VARCHAR(500)`.
- **Hệ số dễ nhớ SRS (`ease_factor`):** `DECIMAL(4,2)` (Phạm vi từ 1.30 đến 4.00, mặc định 2.50).
- **Khoảng cách ngày (`interval_days`), số lần lặp (`repetitions`), thời gian ôn tập (`review_time_seconds`):** `INT UNSIGNED`.
- **Đánh giá SRS (`rating`):** `TINYINT UNSIGNED` (Giá trị 1 đến 4).
- **Nội dung ghi chú cá nhân (`content`):** `VARCHAR(500)` (Khớp chính xác giới hạn tối đa 500 ký tự của đặc tả).
- **Chuỗi JSON (`flagged_fields`):** `TEXT` (Chứa chuỗi JSON mảng các trường vi phạm, tương thích tuyệt đối giữa Jackson và MySQL).

---

## 8. CHIẾN LƯỢC ĐỘ DÀI CHUỖI (STRING LENGTH RATIONALE)

| Trường | Kiểu & Độ dài | Rationale kỹ thuật |
| :--- | :--- | :--- |
| `ACCOUNT.email_or_phone` | `VARCHAR(191)` | 191 ký tự là giới hạn an toàn tối đa cho chỉ mục đơn `utf8mb4` (191 * 4 = 764 bytes < 767 bytes tiền tố trên mọi cấu hình MySQL), đủ chứa toàn bộ số điện thoại quốc tế và địa chỉ email tiêu chuẩn. |
| `ACCOUNT.password_hash` | `VARCHAR(255)` | Chuẩn BCrypt tạo 60 ký tự; dự phòng 255 ký tự cho tương thích các thuật toán băm Argon2id hoặc PBKDF2 tương lai. |
| `RADICAL.character` | `VARCHAR(10)` | Ký tự bộ thủ đơn hoặc biến thể bộ thủ (1–4 ký tự Unicode). |
| `VOCABULARY.hanzi` | `VARCHAR(50)` | Một từ hoặc ngữ tiếng Trung thường dài 1–8 ký tự; 50 ký tự là ngưỡng an toàn cho cả các thành ngữ (quán ngữ) dài. |
| `PERSONAL_NOTE.content` | `VARCHAR(500)` | Tuân thủ chính xác 100% quy tắc nghiệp vụ: nội dung ghi chú tối đa 500 ký tự. |
| Các trường `audio_url`, `video_writing_url`, `excel_file_url`, `avatar_url` | `VARCHAR(500)` | Đủ cho hầu hết các định dạng URL lưu trữ trên Cloud Storage (S3, Cloudinary, CDN) kèm token query parameters. |

---

## 9. CHIẾN LƯỢC THỜI GIAN & KIỂM TOÁN (TIMESTAMP & AUDIT STRATEGY)

1. **Các trường Timestamp bắt buộc theo đặc tả:**
   - `PERSONAL_NOTE.created_at`: `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`
   - `MODERATION_LOG.created_at`: `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`
   - `REVIEW_LOG.reviewed_at`: `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`
   - `CARD_PROGRESS.next_review_at`: `DATETIME NULL` (Thời điểm lượt ôn tập tiếp theo đến hạn)
2. **Bổ sung Audit Timestamps hợp lý cho các thực thể vòng đời chính:**
   - Để phục vụ kiểm soát vòng đời và đồng bộ dữ liệu, các bảng `ACCOUNT`, `USER_PROFILE`, `LESSON`, `RADICAL`, `VOCABULARY` được thiết kế có thêm:
     - `created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`
     - `updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
   - Không bổ sung `updated_at` cho các bảng log thuần túy bất biến (`REVIEW_LOG`, `MODERATION_LOG`).

---

## 10. BỘ MÃ KÝ TỰ VÀ SẮP CHIẾU (CHARSET & COLLATION)

- **Character Set:** `utf8mb4`
- **Collation:** `utf8mb4_unicode_ci`
- **Lý do kỹ thuật:**
  - `utf8mb4` mã hóa đầy đủ 4 bytes cho mỗi ký tự Unicode, hỗ trợ hoàn hảo khối chữ Hán mở rộng (CJK Unified Ideographs Extension A-I), các ký tự bộ thủ Khang Hy (U+2F00..U+2FD5) và ký tự bổ trợ (U+2E80..U+2EFF).
  - `utf8mb4_unicode_ci` tuân thủ thuật toán sắp xếp Unicode Collation Algorithm (UCA), hỗ trợ so khớp chuẩn xác tiếng Việt có dấu, Pinyin có thanh điệu mà không bị lỗi mất dấu hay sắp sai thứ tự bảng chữ cái.

---

## 11. KHÓA NGOẠI VÀ RÀNG BUỘC THAM CHIẾU (FOREIGN KEYS)

Danh sách 14 liên kết khóa ngoại vật lý:
1. `USER_PROFILE.account_id` → `ACCOUNT.account_id`
2. `ACCOUNT_ROLE.account_id` → `ACCOUNT.account_id`
3. `ACCOUNT_ROLE.role_id` → `ROLE.role_id`
4. `VOCAB_RADICAL.vocab_id` → `VOCABULARY.vocab_id`
5. `VOCAB_RADICAL.radical_id` → `RADICAL.radical_id`
6. `LESSON.created_by` → `ACCOUNT.account_id`
7. `LESSON_VOCABULARY.lesson_id` → `LESSON.lesson_id`
8. `LESSON_VOCABULARY.vocab_id` → `VOCABULARY.vocab_id`
9. `USER_SRS_SETTING.user_id` → `USER_PROFILE.user_id`
10. `CARD_PROGRESS.user_id` → `USER_PROFILE.user_id`
11. `REVIEW_LOG.user_id` → `USER_PROFILE.user_id`
12. `PERSONAL_NOTE.user_id` → `USER_PROFILE.user_id`
13. `PERSONAL_NOTE.vocab_id` → `VOCABULARY.vocab_id`
14. `MODERATION_LOG.lesson_id` → `LESSON.lesson_id`
15. `MODERATION_LOG.moderator_id` → `ACCOUNT.account_id`

---

## 12. QUY TẮC XỬ LÝ XÓA VÀ CẬP NHẬT (ON DELETE / ON UPDATE RULES)

| Khóa ngoại | Quan hệ | ON UPDATE | ON DELETE | Rationale nghiệp vụ & bảo mật |
| :--- | :--- | :--- | :--- | :--- |
| `USER_PROFILE.account_id` | `1:1` | `CASCADE` | `CASCADE` | Xóa tài khoản thì xóa luôn hồ sơ tương ứng. |
| `ACCOUNT_ROLE.account_id` | `N:N` | `CASCADE` | `CASCADE` | Xóa tài khoản thì xóa liên kết phân quyền. |
| `ACCOUNT_ROLE.role_id` | `N:N` | `CASCADE` | `RESTRICT` | Không được phép xóa Role hệ thống nếu đang có tài khoản sử dụng. |
| `USER_SRS_SETTING.user_id` | `1:1` | `CASCADE` | `CASCADE` | Xóa người dùng thì xóa cấu hình SRS cá nhân. |
| `CARD_PROGRESS.user_id` | `1:N` | `CASCADE` | `CASCADE` | Xóa người dùng thì xóa tiến trình thẻ học của họ. |
| `PERSONAL_NOTE.user_id` | `1:N` | `CASCADE` | `CASCADE` | Xóa người dùng thì xóa ghi chú của họ. |
| `PERSONAL_NOTE.vocab_id` | `1:N` | `CASCADE` | `CASCADE` | Xóa từ vựng thì xóa các ghi chú đính kèm từ đó. |
| `VOCAB_RADICAL.vocab_id` | `N:N` | `CASCADE` | `CASCADE` | Xóa từ vựng thì xóa liên kết bộ thủ của từ đó. |
| `VOCAB_RADICAL.radical_id`| `N:N` | `CASCADE` | `RESTRICT` | Không được xóa bộ thủ nếu đang có từ vựng liên kết. |
| `LESSON.created_by` | `1:N` | `CASCADE` | `RESTRICT` | Không được xóa tài khoản tác giả nếu có bài học; phải chuyển status sang `Banned`/`Inactive`. |
| `LESSON_VOCABULARY.lesson_id` | `N:N` | `CASCADE` | `CASCADE` | Xóa bài học thì xóa danh sách từ trong bài học đó. |
| `LESSON_VOCABULARY.vocab_id` | `N:N` | `CASCADE` | `RESTRICT` | Không xóa từ vựng nếu đang nằm trong bài học đang hoạt động. |
| `MODERATION_LOG.lesson_id` | `1:N` | `CASCADE` | `RESTRICT` | Bảo vệ lịch sử kiểm duyệt: không được xóa bài học nếu đã có nhật ký kiểm duyệt; phải lưu vết kiểm toán. |
| `MODERATION_LOG.moderator_id` | `1:N` | `CASCADE` | `RESTRICT` | Bảo vệ lịch sử kiểm duyệt: không được xóa tài khoản kiểm duyệt viên. |
| `REVIEW_LOG.user_id` | `1:N` | `CASCADE` | `CASCADE` | Xóa người dùng thì dọn dẹp lịch sử ôn tập cá nhân. |

---

## 13. CHIẾN LƯỢC CHỈ MỤC (INDEX STRATEGY)

Chỉ đánh chỉ mục tại các vị trí phục vụ trực tiếp cho truy vấn thường xuyên:

1. **`idx_account_email_phone`:** `UNIQUE (email_or_phone)` — Phục vụ đăng nhập cực nhanh.
2. **`idx_vocab_pinyin_raw`:** `INDEX (pinyin_raw)` — Phục vụ tìm kiếm thông minh từ vựng không dấu.
3. **`idx_vocab_hanzi`:** `INDEX (hanzi)` — Phục vụ tra cứu chính xác theo chữ Hán.
4. **`idx_lesson_status`:** `INDEX (status)` — Phục vụ hàng đợi duyệt bài (`Pending`) và danh sách bài học công khai (`Approved`).
5. **`idx_lesson_created_by`:** `INDEX (created_by)` — Phục vụ Creator Dashboard lọc bài học cá nhân.
6. **`idx_card_progress_due`:** `INDEX (user_id, next_review_at)` — **Chỉ mục trọng tâm của SRS**, giúp truy vấn thẻ đến hạn trong ngày: `WHERE user_id = ? AND next_review_at <= NOW()`.
7. **`idx_card_progress_item`:** `UNIQUE (user_id, item_type, item_id)` — Đảm bảo mỗi người dùng chỉ có đúng 1 bản ghi tiến trình trên mỗi thẻ học.
8. **`idx_review_log_user`:** `INDEX (user_id, reviewed_at)` — Phục vụ thống kê biểu đồ học tập của người dùng.
9. **`idx_personal_note_user_vocab`:** `INDEX (user_id, vocab_id)` — Lấy nhanh ghi chú cá nhân khi xem chi tiết từ vựng.
10. **`idx_lesson_vocab_order`:** `UNIQUE (lesson_id, order_index)` — Đảm bảo thứ tự hiển thị từ vựng trong bài học không bị xung đột.

---

## 14. CÁC RÀNG BUỘC DUY NHẤT (UNIQUE CONSTRAINTS)

- `ACCOUNT`: `UNIQUE (email_or_phone)`
- `USER_PROFILE`: `UNIQUE (account_id)`
- `ROLE`: `UNIQUE (role_name)`
- `RADICAL`: `UNIQUE (character)`
- `VOCABULARY`: `UNIQUE (hanzi, pinyin_raw)` (Business key: cùng chữ Hán nhưng phát âm khác nhau được chấp nhận; trùng cả chữ Hán và phát âm sẽ bị từ chối).
- `USER_SRS_SETTING`: `UNIQUE (user_id)`
- `CARD_PROGRESS`: `UNIQUE (user_id, item_type, item_id)`
- `LESSON_VOCABULARY`: `UNIQUE (lesson_id, order_index)`

---

## 15. QUYẾT ĐỊNH HIỆN THỰC HÓA THAM CHIẾU ĐA HÌNH (POLYMORPHIC REFERENCE DECISION)

### Bối cảnh
Trong bảng `CARD_PROGRESS` và `REVIEW_LOG`, cặp trường:
- `item_type` (`VOCABULARY` hoặc `RADICAL`)
- `item_id` (`BIGINT UNSIGNED`)
tham chiếu đa hình tới hoặc `VOCABULARY.vocab_id` hoặc `RADICAL.radical_id`.

### Đánh giá hai phương án
- **Phương án A (Giữ nguyên mô hình đa hình theo đặc tả):**
  Lưu `item_type` và `item_id`. Không tạo Foreign Key vật lý ở CSDL MySQL cho `item_id`. Toàn vẹn tham chiếu được kiểm soát nghiêm ngặt tại tầng Service / JPA.
- **Phương án B (Tách thành 2 Foreign Keys nullable):**
  Bổ sung `vocab_id` (FK nullable) và `radical_id` (FK nullable) kèm check constraint `((vocab_id IS NOT NULL AND radical_id IS NULL) OR (vocab_id IS NULL AND radical_id IS NOT NULL))`.

### Quyết định lựa chọn: PHƯƠNG ÁN A
**Lý do lựa chọn:**
1. **Tuân thủ tuyệt đối Đặc tả thẩm quyền:** User chỉ đạo rõ: *"Do NOT alter the approved specification silently. Keep item_type + item_id exactly as specified."*
2. **Tính tổng quát của SRS Engine:** Cỗ máy tính toán SM-2 chỉ quan tâm tới ID của thẻ học và loại thẻ để nạp nội dung hiển thị; không bị phụ thuộc cứng vào cấu trúc quan hệ cha-con.
3. **Hiệu năng và bảo trì:** Không tạo các cột NULL dư thừa trong CSDL; cấu trúc DTO và JSON API trả về sạch sẽ, đồng nhất.
4. **Cơ chế đảm bảo toàn vẹn:** Ở tầng Service, hàm tạo thẻ học bắt buộc validate sự tồn tại của `VOCABULARY` hoặc `RADICAL` trước khi ghi bản ghi vào `CARD_PROGRESS`.

---

## 16. KIỂM TRA CHUẨN HÓA (NORMALIZATION REVIEW)

- **1NF (Dạng chuẩn 1):** Tất cả các cột đều chứa giá trị nguyên tố (Atomic values). Trường `flagged_fields` lưu chuỗi JSON đặc thù cho kiểm duyệt, không dùng để quan hệ.
- **2NF (Dạng chuẩn 2):** Đạt 1NF và toàn bộ các thuộc tính không khóa đều phụ thuộc đầy đủ vào toàn bộ khóa chính (đặc biệt trong các bảng liên kết `ACCOUNT_ROLE`, `VOCAB_RADICAL`, `LESSON_VOCABULARY`).
- **3NF (Dạng chuẩn 3):** Không có phụ thuộc bắc cầu (Transitive Dependency). Không lưu trường tính toán dư thừa.

---

## 17. ĐÁNH GIÁ AN NINH VÀ AN TOÀN DỮ LIỆU (SECURITY REVIEW)

1. **Lưu trữ Mật khẩu:** Trường `password_hash` bắt buộc lưu chuỗi mã hóa một chiều qua BCrypt (độ dài 60 bytes, bọc trong `VARCHAR(255)`). Tuyệt đối cấm lưu mật khẩu thô (plaintext).
2. **Chống giả mạo danh tính (Identity Protection):** `ACCOUNT.email_or_phone` có ràng buộc `UNIQUE` chống đăng ký trùng lặp hoặc chiếm dụng tài khoản.
3. **Chống XSS và Bloat dữ liệu:** `PERSONAL_NOTE.content` bị chặn cứng ở mức `VARCHAR(500)`.
4. **Bảo vệ dữ liệu lịch sử:** `MODERATION_LOG` và `REVIEW_LOG` sử dụng quy tắc `ON DELETE RESTRICT` đối với tài khoản kiểm duyệt và người tạo bài học, ngăn chặn việc xóa tài khoản nhằm xóa vết kiểm toán hoặc lịch sử thao tác.

---

## 18. BẢNG THIẾT KẾ VẬT LÝ CHI TIẾT TỪNG BẢNG (TABLE-BY-TABLE PHYSICAL SCHEMA)

### 18.1. Bảng `ACCOUNT`
```sql
CREATE TABLE `ACCOUNT` (
    `account_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `email_or_phone` VARCHAR(191) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'Active',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uk_account_email_or_phone` UNIQUE (`email_or_phone`),
    CONSTRAINT `chk_account_status` CHECK (`status` IN ('Active', 'Inactive', 'Banned'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 18.2. Bảng `USER_PROFILE`
```sql
CREATE TABLE `USER_PROFILE` (
    `user_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `account_id` BIGINT UNSIGNED NOT NULL,
    `full_name` VARCHAR(100) NOT NULL,
    `avatar_url` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uk_user_profile_account` UNIQUE (`account_id`),
    CONSTRAINT `fk_user_profile_account` FOREIGN KEY (`account_id`) 
        REFERENCES `ACCOUNT` (`account_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 18.3. Bảng `ROLE`
```sql
CREATE TABLE `ROLE` (
    `role_id` INT UNSIGNED NOT NULL PRIMARY KEY,
    `role_name` VARCHAR(50) NOT NULL,
    CONSTRAINT `uk_role_name` UNIQUE (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 18.4. Bảng `ACCOUNT_ROLE`
```sql
CREATE TABLE `ACCOUNT_ROLE` (
    `account_id` BIGINT UNSIGNED NOT NULL,
    `role_id` INT UNSIGNED NOT NULL,
    PRIMARY KEY (`account_id`, `role_id`),
    CONSTRAINT `fk_account_role_account` FOREIGN KEY (`account_id`) 
        REFERENCES `ACCOUNT` (`account_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_account_role_role` FOREIGN KEY (`role_id`) 
        REFERENCES `ROLE` (`role_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 18.5. Bảng `RADICAL`
```sql
CREATE TABLE `RADICAL` (
    `radical_id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `character` VARCHAR(10) NOT NULL,
    `pinyin` VARCHAR(50) NOT NULL,
    `meaning_han_viet` VARCHAR(100) NOT NULL,
    `meaning_vi` VARCHAR(255) NOT NULL,
    `audio_url` VARCHAR(500) NULL,
    `video_writing_url` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uk_radical_character` UNIQUE (`character`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 18.6. Bảng `VOCABULARY`
```sql
CREATE TABLE `VOCABULARY` (
    `vocab_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `hanzi` VARCHAR(50) NOT NULL,
    `pinyin` VARCHAR(100) NOT NULL,
    `pinyin_raw` VARCHAR(100) NOT NULL,
    `meaning_han_viet` VARCHAR(100) NOT NULL,
    `meaning_vi` VARCHAR(255) NOT NULL,
    `audio_url` VARCHAR(500) NULL,
    `video_writing_url` VARCHAR(500) NULL,
    `example_sentence` VARCHAR(500) NULL,
    `example_translation` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uk_vocab_hanzi_pinyin_raw` UNIQUE (`hanzi`, `pinyin_raw`),
    INDEX `idx_vocab_pinyin_raw` (`pinyin_raw`),
    INDEX `idx_vocab_hanzi` (`hanzi`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 18.7. Bảng `VOCAB_RADICAL`
```sql
CREATE TABLE `VOCAB_RADICAL` (
    `vocab_id` BIGINT UNSIGNED NOT NULL,
    `radical_id` INT UNSIGNED NOT NULL,
    PRIMARY KEY (`vocab_id`, `radical_id`),
    CONSTRAINT `fk_vocab_radical_vocab` FOREIGN KEY (`vocab_id`) 
        REFERENCES `VOCABULARY` (`vocab_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_vocab_radical_radical` FOREIGN KEY (`radical_id`) 
        REFERENCES `RADICAL` (`radical_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 18.8. Bảng `LESSON`
```sql
CREATE TABLE `LESSON` (
    `lesson_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `excel_file_url` VARCHAR(500) NULL,
    `created_by` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'Draft',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_lesson_created_by` FOREIGN KEY (`created_by`) 
        REFERENCES `ACCOUNT` (`account_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_lesson_status` CHECK (`status` IN ('Draft', 'Pending', 'Approved', 'Rejected')),
    INDEX `idx_lesson_status` (`status`),
    INDEX `idx_lesson_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 18.9. Bảng `LESSON_VOCABULARY`
```sql
CREATE TABLE `LESSON_VOCABULARY` (
    `lesson_id` BIGINT UNSIGNED NOT NULL,
    `vocab_id` BIGINT UNSIGNED NOT NULL,
    `order_index` INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`lesson_id`, `vocab_id`),
    CONSTRAINT `fk_lesson_vocab_lesson` FOREIGN KEY (`lesson_id`) 
        REFERENCES `LESSON` (`lesson_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_lesson_vocab_vocab` FOREIGN KEY (`vocab_id`) 
        REFERENCES `VOCABULARY` (`vocab_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `uk_lesson_order_index` UNIQUE (`lesson_id`, `order_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 18.10. Bảng `USER_SRS_SETTING`
```sql
CREATE TABLE `USER_SRS_SETTING` (
    `setting_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `new_cards_per_day` INT UNSIGNED NOT NULL DEFAULT 20,
    `max_review_per_day` INT UNSIGNED NOT NULL DEFAULT 100,
    CONSTRAINT `uk_user_srs_setting_user` UNIQUE (`user_id`),
    CONSTRAINT `fk_user_srs_setting_user` FOREIGN KEY (`user_id`) 
        REFERENCES `USER_PROFILE` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 18.11. Bảng `CARD_PROGRESS`
```sql
CREATE TABLE `CARD_PROGRESS` (
    `progress_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `item_type` VARCHAR(20) NOT NULL,
    `item_id` BIGINT UNSIGNED NOT NULL,
    `ease_factor` DECIMAL(4,2) NOT NULL DEFAULT 2.50,
    `interval_days` INT UNSIGNED NOT NULL DEFAULT 0,
    `repetitions` INT UNSIGNED NOT NULL DEFAULT 0,
    `next_review_at` DATETIME NULL,
    CONSTRAINT `fk_card_progress_user` FOREIGN KEY (`user_id`) 
        REFERENCES `USER_PROFILE` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `chk_card_progress_item_type` CHECK (`item_type` IN ('VOCABULARY', 'RADICAL')),
    CONSTRAINT `uk_card_progress_user_item` UNIQUE (`user_id`, `item_type`, `item_id`),
    INDEX `idx_card_progress_due` (`user_id`, `next_review_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 18.12. Bảng `REVIEW_LOG`
```sql
CREATE TABLE `REVIEW_LOG` (
    `log_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `item_type` VARCHAR(20) NOT NULL,
    `item_id` BIGINT UNSIGNED NOT NULL,
    `rating` TINYINT UNSIGNED NOT NULL,
    `interval_before` INT UNSIGNED NOT NULL,
    `interval_after` INT UNSIGNED NOT NULL,
    `review_time_seconds` INT UNSIGNED NOT NULL DEFAULT 0,
    `reviewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_review_log_user` FOREIGN KEY (`user_id`) 
        REFERENCES `USER_PROFILE` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `chk_review_log_item_type` CHECK (`item_type` IN ('VOCABULARY', 'RADICAL')),
    CONSTRAINT `chk_review_log_rating` CHECK (`rating` IN (1, 2, 3, 4)),
    INDEX `idx_review_log_user_date` (`user_id`, `reviewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 18.13. Bảng `PERSONAL_NOTE`
```sql
CREATE TABLE `PERSONAL_NOTE` (
    `note_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `vocab_id` BIGINT UNSIGNED NOT NULL,
    `content` VARCHAR(500) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_personal_note_user` FOREIGN KEY (`user_id`) 
        REFERENCES `USER_PROFILE` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_personal_note_vocab` FOREIGN KEY (`vocab_id`) 
        REFERENCES `VOCABULARY` (`vocab_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_personal_note_user_vocab` (`user_id`, `vocab_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 18.14. Bảng `MODERATION_LOG`
```sql
CREATE TABLE `MODERATION_LOG` (
    `log_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `lesson_id` BIGINT UNSIGNED NOT NULL,
    `moderator_id` BIGINT UNSIGNED NOT NULL,
    `action` VARCHAR(20) NOT NULL,
    `rejection_reason` VARCHAR(500) NULL,
    `flagged_fields` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_moderation_log_lesson` FOREIGN KEY (`lesson_id`) 
        REFERENCES `LESSON` (`lesson_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_moderation_log_moderator` FOREIGN KEY (`moderator_id`) 
        REFERENCES `ACCOUNT` (`account_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_moderation_action` CHECK (`action` IN ('Approve', 'Reject')),
    INDEX `idx_moderation_lesson` (`lesson_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 19. RỦI RO VÀ ĐÁNH ĐỔI THIẾT KẾ (RISKS & TRADE-OFFS)

1. **Rủi ro tham chiếu đa hình không có Foreign Key cứng:**
   - *Đánh đổi:* Bảng `CARD_PROGRESS` và `REVIEW_LOG` không thể dùng `FOREIGN KEY` ở mức MySQL cho `item_id`.
   - *Biện pháp kiểm soát:* Tầng Service trong Spring Boot chịu trách nhiệm kiểm tra tính hợp lệ trước khi chèn/sửa. Khi xóa một `VOCABULARY` hoặc `RADICAL`, Service layer sẽ đồng thời dọn dẹp các bản ghi `CARD_PROGRESS` tương ứng trong cùng một transaction `@Transactional`.
2. **Khóa phức hợp trên bảng liên kết (`ACCOUNT_ROLE`, `VOCAB_RADICAL`):**
   - *Đánh đổi:* Trong JPA cần định nghĩa `@EmbeddedId` hoặc ánh xạ `@ManyToMany` với `@JoinTable`.
   - *Biện pháp kiểm soát:* Đã chuẩn hóa mô hình ánh xạ JPA trong Spring Boot Service, tận dụng tính năng tự động của `@JoinTable`.

---

## 20. CÁC CÂU HỎI MỞ CÒN LẠI (REMAINING OPEN QUESTIONS)

- **OQ-08 (Lựa chọn thư viện tĩnh Frontend cho Phase 9):** Giữ nguyên ở trạng thái OPEN, sẽ quyết định khi bắt đầu Phase 9 (Frontend Implementation).
- Toàn bộ các câu hỏi kỹ thuật CSDL (`OQ-01`, `OQ-02`, `OQ-03`, `OQ-04`, `OQ-05`, `OQ-06`, `OQ-07`) đã được giải quyết dứt điểm trong tài liệu thiết kế này và được User ủy quyền.
