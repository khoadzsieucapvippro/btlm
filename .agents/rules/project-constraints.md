---
name: project-constraints
description: Ràng buộc công nghệ cứng của dự án. LUÔN LUÔN TUÂN THỦ.
---

# TECH STACK KHÓA CỨNG

## Frontend
- HTML
- CSS
- JavaScript (Vanilla JS)
- Các thư viện tĩnh hiện có chỉ được sử dụng khi được lựa chọn cụ thể cho lần triển khai mới.

**KHÔNG tự ý chuyển sang:**
- React, Vue, Angular, Next.js, Nuxt hoặc bất kỳ frontend framework hiện đại nào.

## Backend
- Java
- Spring Boot
- Spring MVC
- Spring Web / REST API
- Spring Security
- JWT
- Spring Data JPA
- Hibernate
- Maven

## Database & Migration
- MySQL
- Flyway là cơ chế kiểm soát schema migration

## API Testing & Tools
- Postman

## Development & Version Control
- Git
- GitHub
- VS Code (Visual Studio Code)

# QUY TẮC CỨNG

- Không dùng `ddl-auto=update`.
- Không để Hibernate tự ý quản lý schema production/development thay Flyway.
- Không sửa migration đã được áp dụng chỉ để thay đổi schema. Schema thay đổi phải thông qua migration mới.
- Không hardcode JWT secret.
- Không hardcode absolute storage path phụ thuộc một máy cụ thể.
- Không trả JPA Entity trực tiếp làm public API contract nếu không có lý do đặc biệt được xác định rõ. DTO/API contract phải được kiểm soát riêng.
- Không tự ý đổi framework hoặc tech stack.
- Giữ nghiệp vụ cốt lõi của hệ thống học tiếng Trung.
- Không tự ý phá vỡ API contract đang được sử dụng.
- Không tự ý thay đổi database structure ngoài migration.
