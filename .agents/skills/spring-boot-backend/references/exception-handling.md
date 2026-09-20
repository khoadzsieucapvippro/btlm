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

## 3. Standard JSON Error Shape (`ApiResponse<Void>`)
Toàn bộ phản hồi lỗi được chuẩn hóa qua `ApiResponse<T>` bởi `GlobalExceptionHandler` (`@RestControllerAdvice`), tuyệt đối không dùng format mặc định của Spring Boot (`timestamp`, `status`, `path`):

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "data": null,
  "errors": [
    "emailOrPhone: Email hoặc số điện thoại không được để trống"
  ]
}
```

Khi là lỗi nghiệp vụ (`BusinessException` / `ResourceNotFoundException`), `errors` là mảng rỗng `[]`:
```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Không tìm thấy bài học với ID: 123",
  "data": null,
  "errors": []
}
```

## 4. Bắt lỗi DTO Validation
Bắt `MethodArgumentNotValidException` (khi Request DTO vi phạm các ràng buộc `@Valid`). Trả về HTTP 400 Bad Request, duyệt `BindingResult.getFieldErrors()` và định dạng từng chuỗi lỗi thành `"${field}: ${message}"` trong `errors[]` (`List<String>`). Frontend sử dụng hàm chuẩn `parseFieldErrors()` để phân tách và hiển thị dưới từng ô nhập liệu.
