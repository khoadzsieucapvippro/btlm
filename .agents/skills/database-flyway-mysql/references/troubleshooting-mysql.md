# Flyway Troubleshooting on MySQL

## Decision Framework: Schema State vs. Migration History
Khi gặp vấn đề với Flyway, hãy trả lời câu hỏi:
**Đây là lỗi về Database Schema State, lỗi Metadata/History, hay cả hai?**
- **Schema-state problem**: Kiểm tra và đối chiếu database thật.
- **Migration-history problem**: Kiểm tra metadata Flyway và nội dung file migration.
- **Cả hai**: Xử lý tính nhất quán của Schema thật trước, sau đó mới can thiệp metadata Flyway.

## 1. DDL KHÔNG Transactional trong MySQL
ĐÂY LÀ KHÁC BIỆT LỚN NHẤT so với SQL Server / PostgreSQL:
- **MySQL**: MỌI lệnh DDL (`CREATE TABLE`, `ALTER TABLE`) đều trigger *implicit commit*.
Nghĩa là nếu script `V3__Script.sql` có 5 lệnh `ALTER TABLE`, và nó bị lỗi ở lệnh thứ 3, thì lệnh 1 và 2 ĐÃ ĐƯỢC LƯU VÀO DATABASE vĩnh viễn. Flyway không thể rollback chúng.

## 2. Cách xử lý "Lỗi nửa chừng" (Failed Migration Halfway)
**Symptom**: App khởi động lỗi, báo migration failed. Bảng `flyway_schema_history` có dòng trạng thái `success = 0`.

**KHÔNG ĐƯỢC PHÉP**: Mù quáng xóa dòng failed hoặc mù quáng chạy `flyway repair` rồi chạy lại migration. `flyway repair` CHỈ sửa metadata, KHÔNG tự dọn dẹp các object thừa trong DB do fail nửa chừng.

**Quy trình khôi phục an toàn (Chỉ trên Dev/Local):**
1. **DỪNG LẠI và Inspect (Kiểm tra)** trạng thái thật của Database hiện tại.
2. Xác định chính xác lệnh nào trong file SQL đã chạy thành công, lệnh nào gây lỗi.
3. So sánh: Schema thực tế vs Ý định của script vs Flyway history.
4. **Dọn dẹp Schema thực tế**: Dùng tool DB xóa thủ công các Table/Column mà nửa đầu script đã kịp sinh ra để đưa DB về trạng thái sạch (trước migration).
5. Sau khi DB đã sạch, lúc này mới can thiệp Metadata: Dùng lệnh `flyway repair` (VD: `mvn flyway:repair` hoặc qua plugin) để xóa các bản ghi failed. Trong trường hợp không cấu hình được CLI/Plugin cục bộ, việc xóa tay (`DELETE FROM flyway_schema_history WHERE success = 0;`) là một ngoại lệ *chỉ được phép thực hiện một cách có nhận thức trên Local*.
6. Sửa lại chỗ sai trong script `.sql` gốc.
7. Chạy lại ứng dụng / Validation.

## 3. Lỗi Checksum Mismatch (Chỉnh sửa file đã apply)
**Symptom**: FlywayException: Migration checksum mismatch for version X.
**Nguyên nhân**: File `V...sql` ĐÃ chạy thành công, nhưng sau đó có người sửa nội dung file này.
**Quy tắc bất di bất dịch**: KHÔNG BAO GIỜ sửa file migration cũ đã apply. Nếu migration đã rời khỏi máy local (đã merge hoặc deploy), thì phải tạo file `V_Next...sql` mới để sửa sai.

**Quy trình xử lý Checksum Mismatch:**
1. Trả lời câu hỏi: Tại sao file bị sửa? (Sửa nhầm, Git conflict, hay cố ý?).
2. Nếu sửa nhầm hoặc vô tình thay đổi logic: **Hoàn tác (revert)** file về đúng nội dung cũ để khớp với Checksum đã lưu.
3. Nếu cố ý sửa (chỉ format code, không đổi logic) VÀ chỉ ở môi trường Local: Có thể sử dụng `flyway repair` để tính toán lại Checksum và đồng bộ metadata. KHÔNG lạm dụng repair để che đậy việc sửa logic migration trái phép.
4. Nếu logic schema cần thay đổi thực sự: Phải tạo file `V_Next...sql` (corrective migration). Khôi phục file cũ về nguyên bản.

## 4. Cấu hình Spring Boot Safety
Trên môi trường Production, nên vô hiệu hóa tính năng clean của Flyway để tránh thảm họa xóa nhầm toàn bộ schema:
- Dùng đúng property namespace của Spring Boot: `spring.flyway.clean-disabled=true` (Tuy nhiên, không cần thêm cấu hình thừa nếu bản thân default của framework đã làm vậy).
