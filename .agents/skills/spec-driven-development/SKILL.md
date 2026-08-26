---
name: spec-driven-development
description: Viết tài liệu đặc tả trước khi code. Sử dụng khi bắt đầu một tính năng mới lớn, yêu cầu mơ hồ hoặc có quyết định kiến trúc quan trọng.
---

# 1. Purpose
Đảm bảo mọi thay đổi lớn hoặc tính năng phức tạp được phân tích thiết kế kỹ lưỡng trước khi bắt đầu code, giảm thiểu việc đập đi xây lại do sai khác hiểu biết.

# 2. When to use
Chỉ kích hoạt nếu task:
- Lớn, mơ hồ.
- Ảnh hưởng nhiều layer (Frontend + Backend + DB).
- Thay đổi API / Database schema / Security cốt lõi.
- Có nhiều quyết định kiến trúc cần thống nhất.

# 3. When NOT to use
- Nếu task nhỏ, rõ ràng, phạm vi cô lập (hotfix, sửa CSS/bug nhỏ) -> KHÔNG bắt buộc tạo file spec riêng hoặc dừng lại chờ user xác nhận.

# 4. Inputs / Preconditions
- Yêu cầu liên quan đến việc thay đổi luồng nghiệp vụ đáng kể và cần đồng bộ giữa nhiều bên.

# 5. Core workflow
1. Đọc yêu cầu.
2. Phân tích tác động (Database, API, Frontend).
3. Viết nháp Spec (Định nghĩa API Contract, Database changes, UI Flow).
4. Yêu cầu Clarification/Confirmation khi:
   - Thiếu thông tin quan trọng.
   - Có nhiều phương án ảnh hưởng lớn.
   - Hoặc thay đổi có nguy cơ phá vỡ hệ thống.

# 6. Decision points
- Spec có làm thay đổi API contract hiện tại không? (Nếu có -> phải có kế hoạch migrate Frontend).

# 7. Red flags
- Đưa framework UI hiện đại (như React/Vue) vào Spec Frontend trong khi dự án dùng HTML/Vanilla JS/jQuery/Bootstrap.
- Trả về JSON Response không tuân thủ chuẩn API Response Consistency của dự án.

# 8. Verification
- Đọc lại Spec xem có mô tả rõ đầu vào/đầu ra và các edge cases chưa.
- Người dùng (USER) hiểu và phê duyệt (Approve) Spec trước khi tiến hành code.

# 9. Exit criteria
- Có bản đặc tả hoàn chỉnh mô tả API Contract, Database changes và logic, đã được xác nhận (nếu cần).
