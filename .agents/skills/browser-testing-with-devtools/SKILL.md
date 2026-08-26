---
name: browser-testing-with-devtools
description: Kiểm tra giao diện và API trên trình duyệt. Sử dụng khi code Frontend (HTML/JS) có thay đổi. BẮT BUỘC để đảm bảo code thực sự chạy.
---

# 1. Purpose
Đảm bảo mã Frontend thực sự chạy đúng trên môi trường trình duyệt thay vì chỉ dựa vào phân tích tĩnh, phát hiện lỗi console hoặc network.

# 2. When to use
- Vừa hoàn thành viết script Vanilla JS/jQuery gọi API.
- Sửa đổi giao diện HTML/CSS/Bootstrap.

# 3. When NOT to use
- Chỉ sửa logic Backend/Database (không có tác động giao diện).

# 4. Inputs / Preconditions
- Backend đã chạy (nếu test tính năng full-stack).
- Frontend đang được serve.

# 5. Core workflow
1. Mở trang web trên trình duyệt.
2. Thao tác như người dùng thực (Click, gõ form).
3. Sử dụng DevTools (Console, Network) để theo dõi.
4. Bắt buộc kiểm tra 10 yếu tố sau (Verification Checklist).

# 6. Decision points
- Nếu Console báo lỗi CORS -> backend sai cấu hình.
- Nếu Network báo 401 -> lỗi auth/token.

# 7. Red flags
- Nhìn code FE thấy đúng nên báo DONE, bỏ qua test browser, thực tế JS bị lỗi TypeError.
- Quên kiểm tra giao diện không bị vỡ trên Mobile và Desktop (Responsive).

# 8. Verification
Bắt buộc kiểm tra 10 yếu tố sau:
1. [ ] Console errors (Không có lỗi đỏ).
2. [ ] Network request (Request bắn đi đúng endpoint).
3. [ ] HTTP status (200, 201...).
4. [ ] Request payload (Đúng format DTO JSON).
5. [ ] Response body (Backend trả về đúng data).
6. [ ] Authentication header (Có Bearer token).
7. [ ] DOM rendering (Data hiển thị lên HTML đúng).
8. [ ] Loading state (Có spinner khi đợi API).
9. [ ] Empty state (Có text khi mảng rỗng).
10. [ ] Error state (Báo lỗi cho user nếu API fail).

# 9. Exit criteria
- Toàn bộ checklist trên đều Pass. Ứng dụng chạy mượt.
