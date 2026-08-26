---
name: database-flyway-mysql
description: Quản lý thay đổi schema MySQL bằng Flyway. Bổ sung bảng, cột, khóa ngoại, và đảm bảo tương thích JPA/Hibernate.
---

# 1. Purpose
Kiểm soát sự thay đổi của Database Schema MySQL theo thời gian, đảm bảo an toàn dữ liệu và đồng bộ với cấu trúc Java Entity (JPA/Hibernate) thông qua Flyway.

# 2. When to use
- Thêm/Xóa/Sửa Table, Column, Index, Foreign Key, Constraints trong MySQL.
- Khởi tạo dữ liệu mẫu (Seed data) hoặc Data Migration.
- Xử lý lỗi Flyway checksum mismatch hoặc failed migration.

# 3. When NOT to use
- Sửa code Java không liên quan DB.
- Đang thử nghiệm Entity JPA chưa chốt phương án cuối (chỉ viết script Flyway khi đã chốt Entity).
- Truy vấn DB thông thường (READ).

# 4. Inputs / Preconditions
- Đã xác nhận project dùng MySQL (`mysql-connector-j`) và module Flyway MySQL (`flyway-mysql`).
- Thư mục chuẩn chứa script: `src/main/resources/db/migration`.
- Đã kiểm tra migration version cao nhất hiện tại để đặt tên file mới (Ví dụ: `V3__Add_users.sql`).

# 5. Core workflow (Migration Validation Workflow)
1. **INSPECT CURRENT SCHEMA**: Kiểm tra các file `.sql` cũ và Entity JPA hiện tại.
2. **INSPECT MIGRATION HISTORY**: Xem Flyway đang ở version nào. Không được sửa file đã apply (`SUCCESS`).
3. **IDENTIFY MINIMAL SCHEMA CHANGE**: Xác định thay đổi nhỏ nhất cần thiết cho feature. Không tự tiện cấu trúc lại bảng khác.
4. **CHECK MYSQL SYNTAX AND SEMANTICS**: Đảm bảo syntax chuẩn MySQL (InnoDB, utf8mb4, AUTO_INCREMENT, v.v.).
5. **CREATE MIGRATION**: Tạo file `.sql` mới.
6. **RUN MIGRATION**: Chạy ứng dụng để Spring Boot kích hoạt Flyway.
7. **VERIFY FLYWAY RESULT**: Xác nhận log báo success và bảng `flyway_schema_history` cập nhật.
8. **VERIFY DATABASE SCHEMA**: Kiểm tra cấu trúc bảng trong MySQL.
9. **VERIFY APPLICATION COMPATIBILITY**: Chạy thử JPA để chắc chắn mapping khớp (không bị lỗi Hibernate validation).

# 6. Decision points
- **Data Migration vs Schema Migration**: Nên tách riêng (VD: `V3.1__Schema.sql` và `V3.2__Seed_Data.sql`).
- **Thêm cột NOT NULL vào bảng ĐÃ CÓ DATA**: Phải làm 3 bước: Thêm cột NULL, UPDATE dữ liệu mặc định, ALTER thành NOT NULL. (MySQL cho phép gán DEFAULT thẳng lúc ADD COLUMN, nhưng tuỳ phiên bản và kích thước data).

# 7. Red flags
- Mở file `.sql` CŨ đã được apply để sửa (Sẽ gây lỗi Checksum mismatch).
- Dùng `ddl-auto=update` trong Hibernate thay vì viết script Flyway (Cấm tuyệt đối).
- Dùng cú pháp SQL Server (`NVARCHAR`, `IDENTITY`, `GO`, `GETDATE()`) trong script MySQL.
- Quên rà soát (Inspect) data/dependency trước khi chạy lệnh `DROP TABLE` hoặc `DROP COLUMN`.

# 8. Verification
- Ứng dụng Spring Boot khởi động thành công (không văng `FlywayException` hay `SchemaManagementException`).
- Bảng `flyway_schema_history` ghi nhận state `SUCCESS`.
- Entity JPA load/lưu dữ liệu không gặp lỗi mismatch.

# 9. Exit criteria
- Lịch sử Flyway được cập nhật, Schema MySQL khớp 100% với Entity Java, hệ thống hoạt động ổn định.

# 10. References to load conditionally
- Cần biết các khác biệt cú pháp MySQL so với SQL Server hay chuẩn bị type? Đọc `references/mysql-schema-guidelines.md`.
- Migration bị lỗi nửa chừng, Checksum mismatch trên MySQL? Đọc `references/troubleshooting-mysql.md`.
