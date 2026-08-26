# Exception Handling

## 1. Mục tiêu
Ngăn chặn Spring Boot trả về lỗi 500 kèm StackTrace thô sơ hoặc màn hình HTML "Whitelabel Error Page". Phải trả về chuẩn JSON API Contract.

## 2. Các thành phần
1. **Custom Exceptions**:
   - `BusinessException` (Lỗi logic, trùng lặp -> HTTP 400 hoặc 409).
   - `ResourceNotFoundException` (Không tìm thấy ID -> HTTP 404).
2. **Global Exception Handler**:
   - Class có annotation `@RestControllerAdvice`.
   - Các hàm có annotation `@ExceptionHandler(ExceptionClass.class)`.

## 3. Standard JSON Error Shape
```json
{
  "timestamp": "2023-10-01T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Email đã tồn tại",
  "path": "/api/users"
}
```

## 4. Bắt lỗi DTO Validation
Bắt `MethodArgumentNotValidException` (Sinh ra khi Request DTO trượt `@Valid`). Trả ra HTTP 400, lặp qua danh sách field lỗi và trả về list message.
