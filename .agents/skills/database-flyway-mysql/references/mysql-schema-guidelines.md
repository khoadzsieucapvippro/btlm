# MySQL Schema Guidelines (Dành cho Flyway)

## 1. Phát hiện & Xử lý Tàn dư SQL Server (SQL Server Residue)
Không dùng bảng ánh xạ (mapping table) 1-1 mù quáng. Khi migrate từ SQL Server, hãy kiểm tra các dấu hiệu sau và ĐIỀU TRA ngữ cảnh trước khi quyết định kiểu dữ liệu MySQL tương ứng:
- **`DATETIME2` / `DATETIME`**: Không tự động chuyển thành `DATETIME(6)`. Hãy kiểm tra precision thực tế cần thiết, mapping của JPA (vd: cần lưu millisecond không?) và convention hiện tại trước khi chọn `DATETIME` (0 fractional seconds) hay `DATETIME(6)`.
- **`NEWID()`**: Không tự động thay bằng `UUID()`. Hãy kiểm tra chiến lược generate ID của JPA/Hibernate (UUID dạng string `VARCHAR(36)` hay binary `BINARY(16)`), format lưu trữ và index.
- **`IDENTITY`**: Ở MySQL thường dùng `AUTO_INCREMENT`, nhưng cần kiểm tra xem khóa chính là số hay chuỗi.
- **`GETDATE()` / `SYSDATETIME()`**: MySQL dùng `CURRENT_TIMESTAMP`. 
- **`NVARCHAR` / `NVARCHAR(MAX)`**: Đừng tự động chuyển thành `VARCHAR(255)`. Kiểm tra độ dài thực tế và requirements (có thể cần `TEXT`, `MEDIUMTEXT`...). Không cần tiền tố `N'...'` khi insert.
- **Hàm khác**: `ISNULL()` -> `IFNULL()` hoặc `COALESCE()`. XÓA bỏ lệnh `GO` (chỉ dùng `;`). Xóa ngoặc vuông `[ ]` bao quanh identifier.

## 2. Charset và Collation
Không hardcode một collation cho mọi project. Khi tạo bảng/cột mới, PHẢI:
- Kiểm tra default charset/collation của database hiện tại.
- Kiểm tra convention của các table khác trong hệ thống.
- Kiểm tra yêu cầu lưu trữ Unicode của ứng dụng.
- Nếu phù hợp và đồng nhất, ưu tiên dùng chuẩn chung (ví dụ `utf8mb4` với `utf8mb4_unicode_ci` hoặc `utf8mb4_0900_ai_ci`).

## 3. Quản lý Audit Timestamps (created_at, updated_at)
MySQL hỗ trợ tự động gán và cập nhật thời gian: `DEFAULT CURRENT_TIMESTAMP` và `ON UPDATE CURRENT_TIMESTAMP`.
**TUY NHIÊN**: Không bắt buộc dùng tính năng này của DB. Hãy kiểm tra convention hiện tại:
- Ứng dụng quản lý qua JPA/Hibernate (`@CreationTimestamp`, `@UpdateTimestamp`, `@PrePersist`)?
- Hay Database tự quản lý?
-> Chỉ dùng `ON UPDATE CURRENT_TIMESTAMP` nếu điều này khớp với mô hình ownership hiện tại của project. Cấm thay đổi cơ chế hiện có nếu task không yêu cầu.

## 4. An toàn khi dùng ALTER TABLE ... MODIFY
**CẢNH BÁO QUAN TRỌNG**: Lệnh `MODIFY` trong MySQL yêu cầu khai báo LẠI TOÀN BỘ định nghĩa cột. Nếu không ghi đủ, các thuộc tính cũ (DEFAULT, COMMENT, v.v.) sẽ BỊ XÓA NGẦM.

**Quy trình an toàn trước khi dùng MODIFY:**
1. Kiểm tra định nghĩa cột HIỆN TẠI đầy đủ (Data type, length, NULL/NOT NULL, DEFAULT, COMMENT, Collation...).
2. Xác định chính xác thuộc tính nào cần giữ lại.
3. Viết lại toàn bộ định nghĩa cũ + thuộc tính muốn thay đổi một cách tường minh.
4. Chỉ thay đổi đúng mục tiêu định thay đổi.
5. Kiểm chứng (Verify) lại cột sau khi chạy lệnh migration.

**Ví dụ thay đổi an toàn (Thêm NOT NULL vào cột đã có data):**
```sql
-- 1. Thêm cột NULL (hoặc kiểm tra cột hiện tại đã có COMMENT)
ALTER TABLE users ADD phone VARCHAR(20) NULL COMMENT 'User phone number';

-- 2. Cập nhật dữ liệu cũ
UPDATE users SET phone = '0000000000' WHERE phone IS NULL;

-- 3. Đổi thành NOT NULL (Phải ghi lại cả VARCHAR và COMMENT)
ALTER TABLE users MODIFY phone VARCHAR(20) NOT NULL COMMENT 'User phone number';
```

## 5. Khóa ngoại và Index
- **Index**: Dùng `CREATE INDEX idx_name ON table_name(column_name);`
- Khi `DROP TABLE`, MySQL sẽ báo lỗi nếu bảng đó đang được bảng khác tham chiếu (Foreign Key). Cần xóa bảng con trước, hoặc `DROP FOREIGN KEY` (phải xác định chính xác system-generated constraint name).
