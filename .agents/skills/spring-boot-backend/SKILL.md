---
name: spring-boot-backend
description: Điều phối phát triển Backend Spring Boot. Quản lý phân tầng Controller-Service-Repository, Xử lý lỗi, và Transaction.
---

# 1. Purpose
Kiểm soát kiến trúc Spring Boot (Java), phân tách rạch ròi trách nhiệm các tầng và chuẩn hóa việc xử lý lỗi (Exception).

# 2. When to use
- Thêm tính năng backend mới (CRUD, Pagination, Business Process).
- Cập nhật API, DTO, Entity relationship.
- Xử lý lỗi (Exception handling) ở backend.

# 3. When NOT to use
- Chỉ thay đổi schema (Dùng `database-flyway-mysql`).
- Chỉ cấu hình Security/CORS (Dùng `spring-security-jwt`).

# 4. Inputs / Preconditions
- Yêu cầu chức năng đã được làm rõ qua Impact Analysis.
- API Contract đã được nháp (Nếu ảnh hưởng Frontend).

# 5. Core workflow
1. **Analyze Impact**: Database có đổi không? (Đẩy qua Flyway). Security có đổi không?
2. **Define DTO Boundaries**: Request DTO (nhận), Response DTO (trả). Tuyệt đối không trả Entity.
3. **Controller Layer**: Chỉ map endpoint, kích hoạt `@Valid`, nhận DTO, gọi Service, bọc HTTP Status.
4. **Service Layer**: Code 100% business logic ở đây. Quản lý `@Transactional`.
5. **Repository Layer**: Gọi DB, quản lý Lazy/Eager loading, chống N+1.
6. **Exception Handling**: Bắn exception có ý nghĩa (VD: `ResourceNotFoundException`) để `@RestControllerAdvice` bắt và trả JSON.
7. **Testing & Verification**: Unit test Service, Integration test Controller.

# 6. Decision points
- Cần trả List hay Pagination? -> Tham khảo `references/persistence-patterns.md`.
- Lỗi nghiệp vụ xảy ra? -> Tham khảo `references/exception-handling.md`.
- Gặp lỗi 500 lạ? -> Tham khảo `references/troubleshooting.md`.

# 7. Red flags
- Viết Logic xử lý chuỗi/nghiệp vụ trong Controller.
- Trả trực tiếp JPA Entity ra Response (Lỗi serialize vòng lặp hoặc lộ thông tin).
- Dùng chuỗi SQL cộng tay trong Repository.

# 8. Verification
- Maven build thành công (`mvn clean test`).
- Controller chỉ chứa từ 3-5 dòng code/method.
- Logs không xuất hiện N+1 queries.

# 9. Exit criteria
- Tính năng chạy đúng yêu cầu, xử lý trọn vẹn cả Error Cases (400, 404) chứ không chỉ Success Cases (200).

# 10. References to load conditionally
- Cần hiểu rõ trách nhiệm 3 tầng? Đọc `references/architecture.md`.
- Bị dính N+1, Fetch Join, Pagination? Đọc `references/persistence-patterns.md`.
- Làm sao bắt lỗi trả về JSON chuẩn? Đọc `references/exception-handling.md`.
- App crash, lỗi bean, lỗi transaction? Đọc `references/troubleshooting.md`.
- Thiết kế chung? Đọc `references/backend-design-patterns.md`.
