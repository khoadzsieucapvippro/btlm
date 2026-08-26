---
name: frontend-ui-engineering
description: Quản lý DOM Rendering, thiết kế giao diện với HTML/CSS/Vanilla JS/jQuery. Phân biệt User UI và Admin UI.
---

# 1. Purpose
Kiểm soát giao diện trực quan và các tương tác (DOM/Events) của người dùng trên nền tảng Web mà không cần các framework rườm rà như React.

# 2. When to use
- Tạo màn hình mới (User Interface hoặc Admin Dashboard).
- Chỉnh sửa CSS, SCSS, responsive layout (Bootstrap 5).
- Thêm sự kiện click, form submit, modal open, table rendering.

# 3. When NOT to use
- Để gọi API fetch data (Hãy dùng `frontend-api-integration`).
- TUYỆT ĐỐI KHÔNG dùng để chèn JSX/TSX hay setup webpack phức tạp.

# 4. Inputs / Preconditions
- Template Bootstrap 5 tĩnh đã được nạp.
- Yêu cầu UI đã được review sơ bộ bằng Impact Analysis.

# 5. Core workflow
1. **Identify Product Surface**: Màn hình này thuộc User hay Admin? (Đọc `references/user-vs-admin-ui.md`).
2. **Page Structure (HTML)**: Viết markup Semantic HTML, dùng Grid của Bootstrap.
3. **DOM Manipulation (JS)**: Select DOM elements (bằng `document.getElementById` hoặc `jQuery`).
4. **Event Delegation**: Gắn sự kiện (Đọc `references/dom-patterns.md`).
5. **Render Logic**: Viết hàm tạo HTML string từ data và gắn vào DOM (`innerHTML`).
6. **Responsive & Accessibility**: Thu gọn trình duyệt, kiểm tra điện thoại có bị vỡ chữ/bảng không.

# 6. Decision points
- Danh sách quá dài? -> Cần phân trang (Pagination) hoặc cuộn vô tận.
- Thao tác xóa dữ liệu nhạy cảm? -> Phải có Modal Confirmation.

# 7. Red flags
- Màn Admin (CRUD) sao chép y hệt thiết kế của màn User (Học bài) chỉ đổi mỗi màu sắc/menu.
- Bị dính lỗi XSS do gán trực tiếp dữ liệu input chưa sanitize vào `innerHTML`.
- Viết `onclick` lộn xộn thẳng vào trong chuỗi thẻ HTML.

# 8. Verification
- F12 Device Toolbar: Không có overflow-x gây thanh cuộn ngang trên mobile.
- Focus states: Có thể dùng phím Tab để nhảy giữa các input.

# 9. Exit criteria
- Render chính xác như mockup, responsive ổn định, không có JSX/TSX bị nhúng vào.

# 10. References to load conditionally
- Cần xây dựng màn hình mới? Đọc `references/user-vs-admin-ui.md` để hiểu ranh giới.
- Cần xử lý Render mảng vào bảng / Event Delegation? Đọc `references/dom-patterns.md`.
