# Backend Architecture & Boundaries

## 1. DTO vs Entity
- **Entity**: Lớp ánh xạ 1-1 với Database Table (thường dùng `@Entity`, `@Table`). KHÔNG ĐƯỢC phơi ra ngoài REST API để tránh lộ mật khẩu, dữ liệu dư thừa, và lỗi `StackOverflowError` (Infinite Recursion) khi parse JSON.
- **DTO (Data Transfer Object)**: Lớp thuần chứa data. Tạo riêng `UserCreateRequestDTO`, `UserResponseDTO`.

## 2. Controller Responsibility
- Bọc đường dẫn HTTP (`@GetMapping`, `@PostMapping`).
- Kiểm tra tính hợp lệ của Request DTO bằng `@Valid`.
- Chuyển tiếp DTO cho Service.
- Trả về HTTP Status Code (Ví dụ: `ResponseEntity.ok(data)` hoặc `ResponseEntity.status(HttpStatus.CREATED).body(data)`).
- **Tuyệt đối không**: Viết vòng lặp xử lý logic, tính toán giá tiền, hay gọi DB trực tiếp tại đây.

## 3. Service Responsibility
- Gánh 100% Business Logic.
- Quyết định dữ liệu sẽ được lưu, sửa, hay xóa.
- Ném ra custom Exception nếu logic nghiệp vụ bị vi phạm.
- Quản lý `@Transactional` (Rollback nếu có lỗi Runtime).

## 4. Repository Responsibility
- Chỉ extends `JpaRepository`.
- Định nghĩa các method query: `findByEmail(String email)`, `@Query("SELECT u FROM User u...")`.
- Không chứa logic nghiệp vụ (if/else).
