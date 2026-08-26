# Defense-in-Depth Validation (Xác thực nhiều lớp)

**Load tham chiếu này khi:** Cần sửa các lỗi do dữ liệu không hợp lệ (invalid data) gây ra crash hoặc security issue.

## 1. Overview
Khi bạn sửa một lỗi do dữ liệu sai, việc thêm một câu lệnh `if` tại một chỗ có vẻ là đủ. Nhưng cái check đó có thể dễ dàng bị bypass nếu code được refactor, hoặc gọi từ một nhánh khác.
**Nguyên tắc cốt lõi:** Validate ở MỌI lớp (every layer) mà dữ liệu đi qua. Làm cho việc xuất hiện lỗi là "không thể về mặt cấu trúc" (structurally impossible).

## 2. Tại sao cần nhiều lớp?
- 1 lớp validation: "Chúng ta đã sửa bug."
- Nhiều lớp validation: "Chúng ta làm cho bug không thể xảy ra nữa."
Các lớp khác nhau bắt các loại lỗi khác nhau:
- **Frontend / Entry validation:** Chặn dữ liệu rác ngay từ cửa.
- **Business Logic validation:** Đảm bảo dữ liệu hợp lý về mặt nghiệp vụ.
- **Database constraints:** Chặn dữ liệu sai rớt xuống ổ cứng.

## 3. Ví dụ trong Spring Boot / Frontend

### Layer 1: Frontend Validation
**Mục đích:** Chặn user nhập bậy bạ, phản hồi nhanh.
```javascript
// JS / jQuery
if (!email.includes('@')) {
    alert("Email không hợp lệ");
    return;
}
```

### Layer 2: API Entry Point (Controller)
**Mục đích:** Chặn request gọi từ Postman / cURL bypass giao diện. Dùng Bean Validation.
```java
public class UserDTO {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
}

@PostMapping("/users")
public ResponseEntity<?> createUser(@Valid @RequestBody UserDTO userDto) { ... }
```

### Layer 3: Business Logic (Service)
**Mục đích:** Đảm bảo logic nghiệp vụ (Ví dụ: Email đã tồn tại chưa?).
```java
public void createUser(UserDTO dto) {
    if (userRepository.existsByEmail(dto.getEmail())) {
        throw new BusinessException("Email đã được sử dụng");
    }
}
```

### Layer 4: Database Constraints (Flyway / MySQL)
**Mục đích:** Chốt chặn cuối cùng bảo vệ tính toàn vẹn dữ liệu.
```sql
-- V2__Add_email_constraint.sql
ALTER TABLE users ADD CONSTRAINT chk_email_format CHECK (email LIKE '%_@__%.__%');
ALTER TABLE users ADD CONSTRAINT uq_email UNIQUE(email);
```

<!-- Adapted from obra/superpowers (MIT License) -->
