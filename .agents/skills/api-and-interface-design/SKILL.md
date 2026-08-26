---
name: api-and-interface-design
description: Thiết kế hợp đồng giao tiếp (API Contract). Xây dựng DTO, quy chuẩn HTTP status, pagination và error normalization.
---

# 1. Purpose
Thống nhất chuẩn giao tiếp (Contract) giữa Backend và Frontend, tránh các lỗi bể giao diện do thay đổi dữ liệu ngầm.

# 2. When to use
- Khởi tạo endpoint API mới liên kết Frontend - Backend.
- Sửa HTTP Status code, format dữ liệu (thêm sửa xóa field).
- Xử lý phân trang (Pagination).

# 3. When NOT to use
- Khi chỉ thay đổi logic tính toán ngầm của backend mà input/output JSON y hệt cũ.

# 4. Inputs / Preconditions
- Yêu cầu chức năng mới đã chốt. Cả BE và FE đều cần nắm hợp đồng này trước khi code độc lập.

# 5. Core workflow
1. **Analyze Requirements**: Frontend cần data gì? Backend cần thông tin gì để xử lý?
2. **Define DTO**: Viết nháp cấu trúc JSON Request và Response.
3. **Consistency Check**: Xem `references/contract-semantics.md` để đảm bảo không vi phạm API RESPONSE CONSISTENCY.
4. **Impact Assessment**: Nếu sửa API cũ đang dùng, đánh giá ảnh hưởng để Frontend cập nhật, tránh văng lỗi `undefined`.
5. **Documentation**: Chốt hợp đồng bằng file Spec, hoặc Code Comment.

# 6. Decision points
- Có pagination không? -> Phải trả về chuẩn có `currentPage`, `totalItems` (Tham khảo `contract-semantics.md`).
- Success vs Error structure: Có thể dùng chung 1 format (Wrapper) HOẶC trả về tự nhiên nhưng cấu trúc lỗi phải tuân theo 1 format chuẩn.

# 7. Red flags
- Rò rỉ Implementation Details: Ném trực tiếp Entity Database ra API, vô tình phơi bày mật khẩu hoặc cột nhạy cảm.
- Thay đổi cấu trúc âm thầm không báo cho Client (Frontend).
- Luôn trả về HTTP 200 cho cả lỗi Validation và Exception.

# 8. Verification
- Gọi thử API bằng cURL/Postman, đối chiếu JSON trả về xem có khớp 100% với file Spec/hợp đồng không.

# 9. Exit criteria
- Đã chốt xong API Contract và ghi nhận để team thi công.

# 10. References to load conditionally
- Cần biết quy chuẩn trả Status Code (201, 400, 422)? Đọc `references/contract-semantics.md`.
- Cần format cấu trúc phân trang chuẩn? Đọc `references/contract-semantics.md`.
