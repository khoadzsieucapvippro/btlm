# Root Cause Tracing (Truy tìm nguyên nhân gốc rễ)

**Load tham chiếu này khi:** Bug xuất hiện ở sâu trong call stack, và việc sửa ở nơi báo lỗi chỉ là chữa triệu chứng.

## 1. Overview
Bugs thường hiển thị ở sâu trong call stack (ví dụ lỗi SQL constraint, NullPointerException ở hàm utils, lỗi parse JSON ở Frontend). Bản năng thường là chặn lỗi ngay tại chỗ đó (thêm `if (x != null)`), nhưng đó là sai lầm vì bạn đang chữa triệu chứng (treating a symptom).
**Nguyên tắc cốt lõi:** Phải lần ngược (trace backward) qua các lời gọi hàm cho đến khi tìm được nguồn gốc phát sinh dữ liệu sai, và sửa tại nguồn.

## 2. Quy trình Tracing

### Bước 1: Quan sát Triệu chứng (Observe the Symptom)
Ví dụ log báo:
`NullPointerException at UserMapper.toDTO(UserMapper.java:45)`

### Bước 2: Tìm nguyên nhân trực tiếp (Find Immediate Cause)
Đoạn code nào trực tiếp gây ra lỗi?
```java
// UserMapper.java
return new UserDTO(user.getId(), user.getProfile().getAddress()); // user.getProfile() bị null
```

### Bước 3: Đặt câu hỏi: Ai gọi hàm này? (What Called This?)
Lần ngược stack trace:
`UserService.getUserDetail(...) -> gọi UserMapper.toDTO(...)`

### Bước 4: Tiếp tục lần ngược (Keep Tracing Up)
Giá trị nào đã được truyền vào? Tại sao `user.getProfile()` bị null?
- À, do `userRepository.findById(id)` trả về User mà không kèm Profile (Lazy Loading trong Hibernate).

### Bước 5: Tìm nguồn gốc thực sự (Find Original Trigger)
Lỗi thực sự nằm ở Repository layer, nơi truy vấn DB thiếu `JOIN FETCH`.
**Cách sửa đúng (Fix at source):**
```java
// UserRepository.java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.id = :id")
Optional<User> findByIdWithProfile(Long id);
```
Không sửa bằng cách thêm `if (user.getProfile() != null)` ở Mapper, vì như thế làm mất dữ liệu trả về cho Frontend.

## 3. Lời khuyên
- Đọc kỹ `Caused by: ...` ở cuối StackTrace của Spring Boot.
- Sử dụng Breakpoint/Debug mode trong IDE hoặc thêm `System.out.println` / `console.log` để log đường đi của dữ liệu.

<!-- Adapted from obra/superpowers (MIT License) -->
