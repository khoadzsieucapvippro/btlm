---
name: frontend-api-integration
description: Quản lý kiến trúc gọi API từ UI. Xử lý tập trung Auth, HTTP Errors, State Management (Loading/Error) và Form submission.
---

# 1. Purpose
Ngăn chặn tình trạng code `fetch/ajax` rải rác, duplicate xử lý Token và xử lý lỗi không đồng nhất trên giao diện.

# 2. When to use
- Viết code gọi API từ Frontend (GET, POST, PUT, DELETE, Upload).
- Xử lý State của giao diện khi tương tác API (Loading spinner, Disable button).
- Xử lý các mã lỗi (401, 403, 400, 500) và hiển thị thông báo.

# 3. When NOT to use
- Khi chỉ làm tĩnh HTML/CSS (Dùng `frontend-ui-engineering`).

# 4. Inputs / Preconditions
- Đã có tài liệu API Contract từ Backend.
- Đã biết Base URL của Backend (Thường là `http://localhost:8080/api`).

# 5. Core workflow
1. **Domain API Module**: Tạo/mở file API chuyên biệt (VD: `lesson-api.js`), KHÔNG gọi `fetch` trực tiếp trong file HTML hoặc file render UI.
2. **Central HTTP Client**: Gọi hàm Wrapper tập trung (đã bọc Base URL và JWT).
3. **UI State - PRE**: Bật Loading Spinner, Disable nút submit (Chống Double Submit).
4. **Execute Call**: Chờ `await` kết quả từ API Module.
5. **UI State - POST (Success)**: Tắt Loading, Xóa báo lỗi cũ, Cập nhật DOM (Dùng `frontend-ui-engineering`).
6. **UI State - POST (Error)**: Tắt Loading, Hiển thị toast/alert lỗi theo thông điệp chuẩn hóa.

# 6. Decision points
- Có file đính kèm không? -> Đọc `references/http-client.md` về FormData.
- API trả về 401? -> Redirect về màn hình login (xử lý ở tầng Central Client).
- Form có dễ bị bấm 2 lần? -> Dùng biến cờ `isSubmitting` để chặn Race condition.

# 7. Red flags
- Viết cứng `Bearer localStorage.getItem('token')` lặp đi lặp lại ở 50 hàm gọi API.
- Gọi API xong nhưng quên tắt Spinner nếu văng lỗi (Bắt buộc dùng `finally`).
- Backend đổi cấu trúc JSON nhưng Frontend cố sửa DOM ngẫu nhiên thay vì sửa ở lớp Normalize Data.

# 8. Verification
- DevTools Network: Headers có Authorization, response trả đúng JSON.
- Trải nghiệm UI: Bấm submit 2 lần thật nhanh chỉ gọi API 1 lần.

# 9. Exit criteria
- Lỗi mạng hoặc 500 hiện thông báo dễ hiểu cho end user. UI không bị treo/đơ.

# 10. References to load conditionally
- Cần mẫu code cho Central HTTP Client và FormData? Đọc `references/http-client.md`.
- Cách quản lý Loading/Empty State và chặn Double Submit? Đọc `references/ui-state.md`.
- Lỗi API Contract Mismatch, Backend đổi format? Đọc `references/api-contract.md`.
- Xem các mẫu code giao tiếp cơ bản: Đọc `references/integration-patterns.md`.
