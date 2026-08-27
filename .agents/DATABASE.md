# DATABASE BASELINE & SPECIFICATION (AUTHORITATIVE 14-TABLE MODEL)

> **Source of Truth:** Authoritative Database Baseline (User Specification)  
> **Status:**  
> - `APPROVED SPECIFICATION`: **14 TABLES APPROVED** (Nghiệp vụ, trường dữ liệu, quan hệ và quy tắc nghiệp vụ đã được phê duyệt)  
> - `PHYSICAL DATABASE STATE`: **IMPLEMENTED & VERIFIED** (MySQL 8.4 LTS `elearning_db`, Flyway V1-V3 applied, 14 domain tables + `flyway_schema_history`, 4 roles, 214 radicals)  
> - `JPA ENTITIES & REPOSITORIES`: **IMPLEMENTED & VERIFIED** (12 Entities, 12 Repositories, Hibernate `ddl-auto: validate` PASS trong Phase 2)  

---

## 1. SYSTEM OVERVIEW & BUSINESS FLOW (TỔNG QUAN LUỒNG NGHIỆP VỤ)

Luồng nghiệp vụ cốt lõi của hệ thống:
1. **Creator** tải lên một file Excel (`.xlsx`).
2. Hệ thống tạo ra một bài học (**Lesson**) chứa các mục từ vựng (**Vocabulary**).
3. Một mục từ vựng có thể liên kết với nhiều bộ thủ (**Radical**) thông qua bảng liên kết `VOCAB_RADICAL`.
4. **Moderator** kiểm duyệt bài học theo chu trình trạng thái:
   `Draft` → `Pending` → `Approved` / `Rejected`
5. Các bài học đã được phê duyệt (`Approved`) được công khai cho **Learner** sử dụng để học tập.
6. **Learner** học và ôn tập **Flashcard** theo thuật toán lặp lại ngắt quãng (**SRS**).
7. Mỗi lượt ôn tập được ghi nhận vào nhật ký `REVIEW_LOG`.
8. Tiến trình `CARD_PROGRESS` được cập nhật tương ứng để phục vụ các lần ôn tập tiếp theo.

---

## 2. ĐẶC TẢ CHI TIẾT 14 BẢNG DỮ LIỆU (APPROVED SPECIFICATION)

### 2.1. Phân hệ Tài khoản & Phân quyền (Account & RBAC)

#### 1. Bảng `ACCOUNT`
- **Mục đích:** Lưu trữ thông tin tài khoản và danh tính xác thực người dùng.
- **Các trường dữ liệu:**
  - `account_id`: Primary Key — Mã định danh tài khoản.
  - `email_or_phone`: Thông tin đăng nhập (Email hoặc số điện thoại).
  - `password_hash`: Chuỗi băm mật khẩu đã lưu.
  - `status`: Trạng thái tài khoản, nhận một trong các giá trị:
    - `Active`
    - `Inactive`
    - `Banned`

#### 2. Bảng `USER_PROFILE`
- **Mục đích:** Thông tin chi tiết hồ sơ người dùng.
- **Các trường dữ liệu:**
  - `user_id`: Primary Key — Mã hồ sơ người dùng.
  - `account_id`: Foreign Key tham chiếu tới `ACCOUNT` (Quan hệ 1:1 với `ACCOUNT`).
  - `full_name`: Tên hiển thị người dùng.
  - `avatar_url`: Đường dẫn ảnh đại diện (URL).

#### 3. Bảng `ROLE`
- **Mục đích:** Danh mục các vai trò trong hệ thống.
- **Các trường dữ liệu:**
  - `role_id`: Primary Key — Mã vai trò. Giá trị chuẩn:
    - `1` = `Learner`
    - `2` = `Creator`
    - `3` = `Moderator`
    - `4` = `Admin`
  - `role_name`: Tên vai trò.

#### 4. Bảng `ACCOUNT_ROLE`
- **Mục đích:** Bảng liên kết trung gian (Junction table) giữa `ACCOUNT` và `ROLE`.
- **Các trường dữ liệu:**
  - `account_id`: Foreign Key tham chiếu tới `ACCOUNT`.
  - `role_id`: Foreign Key tham chiếu tới `ROLE`.
- **Quan hệ:** `ACCOUNT N:N ROLE` thông qua `ACCOUNT_ROLE`.

---

### 2.2. Phân hệ Bộ thủ, Từ vựng & Bài học (Content Management)

#### 5. Bảng `RADICAL`
- **Mục đích:** Dữ liệu bộ thủ tiếng Trung. Hệ thống quản lý chuẩn **214 bộ thủ Khang Hy**.
- **Các trường dữ liệu:**
  - `radical_id`: Primary Key — Mã bộ thủ.
  - `character`: Ký tự bộ thủ (Ví dụ: 人, 水, 木).
  - `pinyin`: Cách phát âm Pinyin.
  - `meaning_han_viet`: Nghĩa / Âm Hán-Việt (Ví dụ: Nhân, Thủy, Mộc).
  - `meaning_vi`: Nghĩa tiếng Việt.
  - `audio_url`: URL file âm thanh phát âm (MP3).
  - `video_writing_url`: URL video hướng dẫn nét viết.

#### 6. Bảng `VOCABULARY`
- **Mục đích:** Dữ liệu từ vựng và chữ Hán (Hanzi).
- **Các trường dữ liệu:**
  - `vocab_id`: Primary Key — Mã từ vựng.
  - `hanzi`: Chữ Hán / Từ vựng.
  - `pinyin`: Pinyin có dấu thanh điệu (Ví dụ: xiū).
  - `pinyin_raw`: Pinyin thô không dấu để phục vụ tìm kiếm thông minh (Ví dụ: xiu).
  - `meaning_han_viet`: Âm Hán-Việt (Ví dụ: Hưu).
  - `meaning_vi`: Nghĩa tiếng Việt (Ví dụ: Nghỉ ngơi).
  - `audio_url`: URL phát âm từ vựng.
  - `video_writing_url`: URL video hướng dẫn viết chữ.
  - `example_sentence`: Câu ví dụ tiếng Trung.
  - `example_translation`: Bản dịch tiếng Việt của câu ví dụ.

#### 7. Bảng `VOCAB_RADICAL`
- **Mục đích:** Bảng liên kết trung gian (Junction table) giữa `VOCABULARY` và `RADICAL`.
- **Các trường dữ liệu:**
  - `vocab_id`: Foreign Key tham chiếu tới `VOCABULARY`.
  - `radical_id`: Foreign Key tham chiếu tới `RADICAL`.
- **Quy tắc nghiệp vụ:**
  - Một từ vựng có thể chứa nhiều bộ thủ cấu thành.
  - Một bộ thủ có thể xuất hiện trong nhiều từ vựng khác nhau.
- **Quan hệ:** `VOCABULARY N:N RADICAL` thông qua `VOCAB_RADICAL`.

#### 8. Bảng `LESSON`
- **Mục đích:** Bài học / tập hợp từ vựng được tạo hoặc import từ file Excel.
- **Các trường dữ liệu:**
  - `lesson_id`: Primary Key — Mã bài học.
  - `title`: Tên bài học hoặc tên file Excel được tải lên.
  - `excel_file_url`: URL hoặc đường dẫn lưu trữ file Excel gốc.
  - `created_by`: Foreign Key tham chiếu tới `ACCOUNT` đã tạo bài học.
  - `status`: Trạng thái kiểm duyệt, gồm 4 trạng thái:
    - `Draft`
    - `Pending`
    - `Approved`
    - `Rejected`

#### 9. Bảng `LESSON_VOCABULARY`
- **Mục đích:** Bảng liên kết trung gian (Junction table) giữa `LESSON` và `VOCABULARY`.
- **Các trường dữ liệu:**
  - `lesson_id`: Foreign Key tham chiếu tới `LESSON`.
  - `vocab_id`: Foreign Key tham chiếu tới `VOCABULARY`.
  - `order_index`: Thứ tự ban đầu của từ vựng trích xuất từ file Excel.
- **Quan hệ:** `LESSON N:N VOCABULARY` thông qua `LESSON_VOCABULARY`.

---

### 2.3. Phân hệ Học tập SRS, Lịch sử Ôn tập, Ghi chú & Kiểm duyệt

#### 10. Bảng `USER_SRS_SETTING`
- **Mục đích:** Cài đặt giới hạn học tập / thông số SRS cá nhân của Learner.
- **Các trường dữ liệu:**
  - `setting_id`: Primary Key — Mã cài đặt.
  - `user_id`: Foreign Key tham chiếu tới `USER_PROFILE` (Quan hệ 1:1 với `USER_PROFILE`).
  - `new_cards_per_day`: Giới hạn số thẻ mới tối đa mỗi ngày (Giá trị mặc định: `20`).
  - `max_review_per_day`: Giới hạn số thẻ ôn tập tối đa mỗi ngày (Giá trị mặc định: `100`).

#### 11. Bảng `CARD_PROGRESS`
- **Mục đích:** Tiến trình ghi nhớ SRS của từng người dùng trên từng thẻ học (Baseline thuật toán SM-2).
- **Các trường dữ liệu:**
  - `progress_id`: Primary Key — Mã tiến trình.
  - `user_id`: Foreign Key tham chiếu tới `USER_PROFILE` (Người học).
  - `item_type`: Loại thẻ học:
    - `VOCABULARY`
    - `RADICAL`
  - `item_id`: Mã định danh tương ứng của Từ vựng hoặc Bộ thủ.
  - `ease_factor`: Hệ số dễ nhớ (Ease Factor - EF) (Giá trị mặc định: `2.50`).
  - `interval_days`: Khoảng cách ôn tập hiện tại tính theo ngày.
  - `repetitions`: Số lần ôn tập thành công liên tiếp.
  - `next_review_at`: Thời điểm dự kiến cho lượt ôn tập tiếp theo.
- **Ghi chú quan trọng:** Cặp trường `item_type` + `item_id` là tham chiếu đa hình (polymorphic reference) tới `VOCABULARY` hoặc `RADICAL`. Không tự ý thay đổi cấu trúc này trong tài liệu đặc tả.

#### 12. Bảng `REVIEW_LOG`
- **Mục đích:** Lịch sử ghi nhận các lượt tương tác và đánh giá Flashcard của người học.
- **Các trường dữ liệu:**
  - `log_id`: Primary Key — Mã lượt ôn tập.
  - `user_id`: Foreign Key tham chiếu tới `USER_PROFILE`.
  - `item_type`: Loại thẻ: `VOCABULARY` hoặc `RADICAL`.
  - `item_id`: Mã ID của đối tượng tương ứng.
  - `rating`: Đánh giá của người học:
    - `1` = `Again` (Quên, học lại)
    - `2` = `Hard` (Khó nhớ)
    - `3` = `Good` (Nhớ tốt)
    - `4` = `Easy` (Rất dễ)
  - `interval_before`: Khoảng cách ôn tập trước khi tính toán.
  - `interval_after`: Khoảng cách ôn tập mới được tính toán.
  - `review_time_seconds`: Thời gian suy nghĩ / phản xạ trước khi lật thẻ tính bằng giây (*Tên trường chuẩn xác: `review_time_seconds`*).
  - `reviewed_at`: Thời điểm thực hiện lượt ôn tập.

#### 13. Bảng `PERSONAL_NOTE`
- **Mục đích:** Ghi chú cá nhân riêng tư của người học dành cho từng từ vựng.
- **Các trường dữ liệu:**
  - `note_id`: Primary Key — Mã ghi chú.
  - `user_id`: Foreign Key tham chiếu tới `USER_PROFILE`.
  - `vocab_id`: Foreign Key tham chiếu tới `VOCABULARY`.
  - `content`: Nội dung ghi chú (Quy tắc: Độ dài tối đa `500` ký tự).
  - `created_at`: Thời điểm tạo ghi chú.

#### 14. Bảng `MODERATION_LOG`
- **Mục đích:** Nhật ký lịch sử kiểm duyệt bài học.
- **Các trường dữ liệu:**
  - `log_id`: Primary Key — Mã nhật ký kiểm duyệt.
  - `lesson_id`: Foreign Key tham chiếu tới `LESSON`.
  - `moderator_id`: Foreign Key tham chiếu tới tài khoản `ACCOUNT` của kiểm duyệt viên.
  - `action`: Hành động kiểm duyệt:
    - `Approve`
    - `Reject`
  - `rejection_reason`: Lý do từ chối (Bắt buộc phải có khi `action` là `Reject`).
  - `flagged_fields`: Chuỗi JSON ghi nhận danh sách các trường bị đánh dấu sai sót / vi phạm.
  - `created_at`: Thời điểm thực hiện kiểm duyệt.

---

## 3. MA TRẬN QUAN HỆ CHÍNH THỨC (AUTHORITATIVE RELATIONSHIP MATRIX)

| Thực thể nguồn | Bản số (Cardinality) | Thực thể đích | Cơ chế liên kết / Ghi chú |
| :--- | :---: | :--- | :--- |
| `ACCOUNT` | **1 : 1** | `USER_PROFILE` | Khóa ngoại `USER_PROFILE.account_id` trỏ tới `ACCOUNT.account_id` |
| `ACCOUNT` | **N : N** | `ROLE` | Thông qua bảng liên kết trung gian `ACCOUNT_ROLE` |
| `VOCABULARY` | **N : N** | `RADICAL` | Thông qua bảng liên kết trung gian `VOCAB_RADICAL` |
| `LESSON` | **N : N** | `VOCABULARY` | Thông qua bảng liên kết trung gian `LESSON_VOCABULARY` (kèm `order_index`) |
| `USER_PROFILE` | **1 : N** | `CARD_PROGRESS` | Khóa ngoại `CARD_PROGRESS.user_id` |
| `USER_PROFILE` | **1 : N** | `REVIEW_LOG` | Khóa ngoại `REVIEW_LOG.user_id` |
| `USER_PROFILE` | **1 : N** | `PERSONAL_NOTE` | Khóa ngoại `PERSONAL_NOTE.user_id` |
| `LESSON` | **1 : N** | `MODERATION_LOG` | Khóa ngoại `MODERATION_LOG.lesson_id` |
| `USER_PROFILE` | **1 : 1** | `USER_SRS_SETTING` | Khóa ngoại `USER_SRS_SETTING.user_id` trỏ tới `USER_PROFILE.user_id` |

---

## 4. QUY TẮC NGHIỆP VỤ BẮT BUỘC BẢO TỒN (APPROVED BUSINESS RULES)

1. Hệ thống quản lý cố định **214 Bộ thủ Khang Hy**.
2. **Creator** tải lên file Excel để tạo nội dung bài học.
3. Vòng đời kiểm duyệt bài học: `Draft` → `Pending` → `Approved` / `Rejected`.
4. Chỉ các bài học ở trạng thái `Approved` mới được hiển thị công khai cho **Learner** học tập.
5. Thuật toán SRS nền tảng là biến thể của **SM-2**.
6. Giới hạn số thẻ mới học mỗi ngày mặc định: `new_cards_per_day = 20`.
7. Giới hạn số thẻ ôn tập tối đa mỗi ngày mặc định: `max_review_per_day = 100`.
8. Hệ số dễ nhớ ban đầu mặc định: `ease_factor = 2.50`.
9. Thang đánh giá ôn tập Flashcard gồm 4 mức: `1 = Again`, `2 = Hard`, `3 = Good`, `4 = Easy`.
10. Độ dài nội dung ghi chú cá nhân (`PERSONAL_NOTE.content`) tối đa là **500 ký tự**.
11. Bắt buộc phải nhập lý do từ chối (`rejection_reason`) khi hành động kiểm duyệt là `Reject`.
12. `flagged_fields` lưu trữ danh sách các trường dữ liệu vi phạm dưới dạng chuỗi JSON.

---

## 5. CÁC QUYẾT ĐỊNH TRIỂN KHAI CHƯA XÁC ĐỊNH (IMPLEMENTATION DECISIONS PENDING)

Các chi tiết dưới đây **KHÔNG PHẢI** là đặc tả nghiệp vụ đã phê duyệt, mà là các quyết định kỹ thuật vật lý sẽ được khảo sát, đề xuất và chốt khi bước vào **Phase 2 (MySQL, Flyway and Persistence Foundation)**:

1. **Kiểu dữ liệu vật lý cụ thể trên MySQL:** (Ví dụ: `INT` vs `BIGINT` cho từng loại ID, `VARCHAR` vs `TEXT` cho nội dung câu ví dụ, độ dài cụ thể của URL media, kiểu số học `DECIMAL` cho `ease_factor`).
2. **Chiến lược sinh khóa chính (Primary Key Generation):** Lựa chọn giữa `AUTO_INCREMENT`, `IDENTITY`, hay UUID/HiLo.
3. **Cấu trúc khóa của bảng liên kết (Junction Tables):** Quyết định bảng liên kết (`ACCOUNT_ROLE`, `VOCAB_RADICAL`, `LESSON_VOCABULARY`) sẽ dùng Khóa chính phức hợp (Composite PK) hay Khóa chính đại diện (Surrogate PK).
4. **Các chỉ mục và ràng buộc duy nhất kỹ thuật (Technical Indexes & UNIQUE Constraints):** Đánh giá việc tạo chỉ mục cho các trường tìm kiếm (`pinyin_raw`, `hanzi`, `status`), ràng buộc `UNIQUE` trên `email_or_phone`, hay ràng buộc duy nhất của tiến trình thẻ `(user_id, item_type, item_id)`.
5. **Cơ chế lưu trữ và Collation:** Lựa chọn Storage Engine (khuyến nghị `InnoDB`) và Collation (khuyến nghị `utf8mb4_unicode_ci`).
6. **Chiến lược thời gian kiểm toán (Audit Timestamps):** Xác định những bảng nào cần bổ sung `created_at` và `updated_at` bên cạnh các bảng đã có trong đặc tả (`PERSONAL_NOTE`, `MODERATION_LOG`, `REVIEW_LOG`).
7. **Quy tắc toàn vẹn tham chiếu (ON DELETE / ON UPDATE):** Xác định hành vi xóa (`CASCADE`, `SET NULL`, hay `RESTRICT`) khi xóa một tài khoản, từ vựng hay bài học.
8. **Hiện thực hóa quan hệ Đa hình (Polymorphic Reference Implementation):** Cách thiết kế bảng vật lý trên MySQL cho `item_type` + `item_id` (giữ nguyên 2 cột trong MySQL hay tách bảng quan hệ).
9. **Chiến lược ánh xạ JPA / Hibernate:** Không đồng nhất 1 bảng = 1 Entity class; quyết định chiến lược ánh xạ `@JoinTable`, `@ElementCollection`, `@EmbeddedId`, hoặc `@IdClass`.
