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

## 2. API Response Wrapper vs Natural Responses
- Không ép buộc dùng Wrapper khổng lồ kiểu `{ "status": 200, "message": "OK", "data": {...} }` cho TẤT CẢ endpoint nếu không thích.
- Có thể trả `data` trực tiếp (Natural) nếu HTTP status code đã gánh vác ý nghĩa thành công.
- Tuy nhiên, **ERROR RESPONSE BẮT BUỘC DÙNG WRAPPER**:
```json
{
  "timestamp": "2023-10-01T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Email is required",
  "path": "/api/users"
}
```

## 3. Pagination Contract
Mọi danh sách có phân trang phải tuân thủ form sau (map trực tiếp từ `Page<T>` của Spring):
```json
{
  "content": [
    { "id": 1, "name": "Lesson 1" },
    { "id": 2, "name": "Lesson 2" }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 50,
  "totalPages": 5,
  "last": false
}
```
Frontend bắt buộc phải dựa vào `totalPages` để render các nút bấm [1] [2] [3].
