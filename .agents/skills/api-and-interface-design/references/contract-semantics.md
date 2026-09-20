# API Contract Semantics

## 1. HTTP Status Code Consistency
- **200 OK**: Request thành công (Dùng cho GET, PUT, DELETE).
- **201 Created**: Tạo mới tài nguyên thành công (Dùng cho POST).
- **204 No Content**: Xóa thành công nhưng không muốn trả về body.
- **400 Bad Request**: Lỗi định dạng JSON, lỗi Validation form (Không có Email, Password quá ngắn).
- **401 Unauthorized**: Thiếu token, token sai, hết hạn.
- **403 Forbidden**: Có token đúng nhưng không đủ quyền hạn.
- **404 Not Found**: Endpoint không tồn tại, hoặc Entity ID không tồn tại.
- **500 Internal Server Error**: Lỗi code logic backend (NullPointerException, Database down).

## 2. API Response Wrapper (`ApiResponse<T>`)
- Hầu hết các phản hồi API nghiệp vụ sử dụng phong bì `ApiResponse<T>`. Các phản hồi đặc thù theo từng endpoint được ưu tiên áp dụng. Các phản hồi HTTP 204 No Content không chứa body và do đó không chứa `ApiResponse<T>`:
```json
{
  "code": "SUCCESS",
  "message": "Thao tác thành công",
  "data": { ... },
  "errors": []
}
```
- Khi xảy ra lỗi validation (HTTP 400), backend `GlobalExceptionHandler` trả về mã `VALIDATION_ERROR` và danh sách chuỗi lỗi chi tiết qua mảng `errors` (`List<String>` định dạng `"${field}: ${message}"`):
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

## 3. Authoritative Pagination Contract (`PageResponse<T>`)
Mọi danh sách có phân trang phải được bọc trong DTO `PageResponse<T>` nằm trong trường `data` của `ApiResponse`:
```json
{
  "code": "SUCCESS",
  "message": "Thành công",
  "data": {
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3,
    "items": [
      { "id": 1, "name": "Lesson 1" },
      { "id": 2, "name": "Lesson 2" }
    ]
  },
  "errors": []
}
```
Frontend bắt buộc phải dựa vào các thuộc tính `page`, `size`, `totalElements`, `totalPages`, `items` theo đúng DTO thực tế, tuyệt đối không suy diễn tên thuộc tính nội bộ của Spring Data `Page` (như `number`, `content`).
