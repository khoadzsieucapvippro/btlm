---
name: frontend-ui-engineering
description: Quản lý kiến trúc giao diện HTML5/CSS3/Vanilla JS, hệ thống typography tối ưu cho học tiếng Trung (CJK Typography), bản sắc sản phẩm chống AI SaaS generic, phân định 4 bề mặt RBAC, và khả năng tiếp cận WCAG 2.2 AA.
---

# 1. Purpose
Định hướng và kiểm soát kiến trúc giao diện trực quan, hệ thống Design Tokens, phân cấp kiểu chữ Hán tự (CJK Typography), sự khác biệt giữa 4 bề mặt vai trò người dùng (Learner, Creator, Moderator, Admin), tương tác thẻ học SRS Flashcard 3D, và chuẩn tiếp cận WCAG 2.2 AA trên nền tảng HTML5 ngữ nghĩa, CSS3 và Bootstrap 5 hỗ trợ.

# 2. When to Use
- Thiết kế hoặc chỉnh sửa layout trang HTML, cấu trúc cây DOM, hoặc file style `frontend/css/style.css`.
- Xây dựng bản sắc trực quan, định nghĩa Design Tokens và biến màu sắc, khoảng cách.
- Tinh chỉnh giao diện theo vai trò người dùng (Học viên vs Tác giả vs Kiểm duyệt vs Quản trị).
- Triển khai giao diện thẻ học Flashcard SRS lật 3D, thanh tiến độ, hoặc bảng dữ liệu Admin.
- Kiểm toán và khắc phục các vấn đề về khả năng tiếp cận (Accessibility WCAG 2.2) và tương thích đa màn hình (Responsive).

# 3. Required Inputs
- File hợp đồng API chuẩn xác: `PROJECT-CONTRACT.md` và `frontend-api-integration/references/api-contract.md`.
- Bản thiết kế trực quan và bảng token: `references/product-identity-and-design-tokens.md`.
- Bản đồ trang và phân định vai trò: `references/rbac-surfaces-and-layouts.md`.
- Chuẩn tiếp cận và responsive: `references/accessibility-and-responsive.md`.

# 4. Source of Truth
- **W3C Web Content Accessibility Guidelines (WCAG) 2.2 Level AA**.
- **W3C WAI-ARIA 1.2 Specification & Authoring Practices Guide (APG)**.
- **WHATWG HTML Living Standard (Semantic Elements)**.
- `PROJECT-CONTRACT.md` cho các bất biến vai trò người dùng và phạm vi bề mặt UI.

# 5. Non-Negotiable Rules
- **Native Semantic HTML Priority**: Bắt buộc ưu tiên dùng các thẻ HTML ngữ nghĩa chuẩn (`<button>`, `<a>`, `<dialog>`, `<form>`, `<input>`, `<select>`, `<textarea>`, `<nav>`, `<main>`, `<header>`, `<footer>`) thay vì dùng `<div>`/`<span>` giả lập kèm ARIA.
- **Single Primary H1 Rule (Quy ước dự án — Project Convention)**: Mỗi trang HTML ưu tiên duy nhất 1 thẻ `<h1>` đại diện cho chủ đề chính theo quy ước kiến trúc dự án (không phải yêu cầu bắt buộc của WCAG); phân cấp `<h2>`..`<h6>` phải tuân theo thứ bậc logic (WCAG SC 1.3.1 & 2.4.6), không nhảy cóc cấp độ heading khi tạo subsection.
- **Target Size Standard**: Vùng bấm tối thiểu $\ge 24\times 24\text{ CSS px}$ (chuẩn WCAG SC 2.5.8 AA). Các nút điều khiển học tập chính trên màn hình cảm ứng ưu tiên đạt kích thước ergonomic xấp xỉ $44\times 44\text{ CSS px}$.
- **Visible Focus & Contrast**: Tuyệt đối không dùng `outline: none` nếu không có focus ring thay thế. Độ tương phản chữ thường $\ge 4.5:1$, chữ lớn $\ge 3:1$, ranh giới thành phần UI/icon $\ge 3:1$ (SC 1.4.11).
- **UI Hiding is NOT Authorization**: Việc ẩn/hiển thị nút bấm hay menu theo vai trò người dùng chỉ là hỗ trợ trải nghiệm (UX), tuyệt đối không coi là ranh giới bảo mật. Backend Spring Security là nơi duy nhất quyết định quyền hạn.
- **Anti-Template Objective Governance**: Cấm các khuôn mẫu AI SaaS sáo rỗng: không dùng nền tím/xanh gradient generic, không bo góc container quá đà thiếu căn cứ, không dùng màu xanh Bootstrap nguyên bản mà không áp dụng bảng màu sư phạm.

# 6. Workflow
1. **Surface Identification**: Xác định bề mặt vai trò (Learner, Creator, Moderator, Admin) theo `references/rbac-surfaces-and-layouts.md`.
2. **Semantic HTML Framing**: Khởi tạo layout với skip link (`#mainContent`), header, nav, main, footer.
3. **Typography & Token Application**: Áp dụng font chữ Hán (`Noto Sans SC`, `Microsoft YaHei`) và thang tỷ lệ phù hợp ngữ cảnh (Hero 48–64px, Catalog 20–24px, Form/Table 14–16px).
4. **Widget Interaction & Keyboard Operability**:
   - Modal: Quản lý focus trap, chuyển focus vào modal khi mở, trả focus về trigger khi đóng, cho phép phím `Escape`.
   - Flashcard SRS: Lật thẻ bằng CSS 3D `rotateY(180deg)` và phím tắt 1–4, Space (chỉ active ở mặt sau, che chắn khi focus ở input). Hỗ trợ `prefers-reduced-motion`.
5. **Responsive Adaptation**: Kiểm tra co giãn trên 3 mốc (Mobile <768px, Tablet 768–991px, Desktop $\ge 992\text{px}$). Bọc bảng trong `.table-responsive` cuộn cục bộ.

# 7. Security Constraints
- Dữ liệu hiển thị trong các thành phần UI (tên bài học, chữ Hán, nghĩa tiếng Việt, ghi chú) phải được chèn qua `textContent` hoặc `createSafeElement()` để phòng ngừa và ngăn chặn triệt để các bồn trũng DOM XSS (sinks) theo nguyên tắc defense-in-depth.
- Không đưa thông tin nhạy cảm vào thuộc tính DOM mở (`data-*` attributes không được chứa credentials hoặc secret tokens).

# 8. Accessibility Constraints
- Nút bấm chỉ có icon (Icon-only button) BẮT BUỘC có nhãn `aria-label` hoặc `.visually-hidden`. Icon trang trí đi kèm chữ phải có `aria-hidden="true"`.
- Bảng dữ liệu: Ngăn tràn ngang toàn trang bằng container cuộn cục bộ (`.table-responsive`). Cung cấp `<caption>` hoặc `aria-label`. Chỉ thêm `tabindex="0"` và `role="region"` khi vùng cuộn chứa nội dung thuần văn bản không có phần tử focusable bên trong, tránh tạo tab stop dư thừa.
- Các cập nhật bất đồng bộ phải báo qua `aria-live="polite"` mà không cướp focus của người học.

# 9. Verification
- **DevTools Elements & Accessibility Inspector**: Kiểm tra cây accessibility, kiểm tra accessible names trên mọi nút bấm và liên kết.
- **Keyboard-Only Test**: Hoàn thành toàn bộ luồng tương tác chỉ bằng bàn phím. Các phần tử tương tác phải tiếp cận và kích hoạt được với thứ tự focus hợp lý; native controls giữ nguyên hành vi phím mặc định; custom widgets tuân thủ mẫu APG tương ứng; phím Escape chỉ kích hoạt đóng trên các tương tác overlay/dialogs/menus có thể đóng.
- **Contrast Analyzer**: Xác minh màu sắc văn bản và viền input đạt tỷ lệ tương phản chuẩn bằng công cụ kiểm tra độ tương phản.
- **Responsive Toolbar**: Kiểm tra hiển thị trên 375px, 768px, và 1200px.

# 10. Failure Conditions
- Thẻ học hoặc nút bấm không thể bấm hoặc thao tác được bằng bàn phím.
- Thiếu nhãn accessible name trên các icon điều khiển âm thanh, tìm kiếm, hoặc đóng modal.
- Bảng quản trị làm xuất hiện thanh cuộn ngang toàn trang trên thiết bị di động.
- Dùng `<div>` gắn sự kiện click thay vì dùng thẻ `<button>`.

# 11. What NOT to Do
- KHÔNG dùng ARIA để thay thế thẻ HTML tự nhiên khi thẻ tự nhiên đã có sẵn ngữ nghĩa.
- KHÔNG lạm dụng `aria-expanded` cơ học: chỉ áp dụng khi widget role/mẫu APG hỗ trợ trạng thái mở rộng và phần tử thực sự điều khiển nội dung co giãn; không đặt lên container tĩnh, div trần, hay flashcard 3D.
- KHÔNG cài đặt các framework CSS nặng nề (Tailwind) hay thư viện UI ngoài phạm vi Bootstrap 5.3 CDN đã thỏa thuận.
- KHÔNG tạo một giao diện chung giống hệt nhau cho cả 4 vai trò.

# 12. References
- Bản sắc trực quan, quy chuẩn Hán tự, token màu sắc: Đọc `references/product-identity-and-design-tokens.md`.
- Phân định bố cục và trải nghiệm 4 vai trò RBAC: Đọc `references/rbac-surfaces-and-layouts.md`.
- Kỹ thuật thẻ học SRS Flashcard 3D flip & hotkeys: Đọc `references/srs-learning-interaction.md`.
- Quy chuẩn tiếp cận WCAG 2.2 AA & đa màn hình: Đọc `references/accessibility-and-responsive.md`.
