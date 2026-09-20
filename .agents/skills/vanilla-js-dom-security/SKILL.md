---
name: vanilla-js-dom-security
description: Quản lý kiến trúc Vanilla JS ES6+ module, Page lifecycle, Render DOM an toàn phòng chống XSS theo nguyên tắc Defense-in-Depth, Quản lý State gọn nhẹ, và gia cố bảo mật CSP.
---

# 1. Purpose
Thiết lập chuẩn mực kiến trúc mã nguồn JavaScript thuần (Vanilla JS ES6+ modular), kiểm soát ranh giới module giữa API và UI, thực thi triệt để nguyên tắc render DOM an toàn chống lỗ hổng DOM-based XSS (CWE-79), quản lý state gọn nhẹ không framework, và thiết lập chính sách Content Security Policy (CSP) dựa trên đồ thị tài nguyên thực tế.

# 2. When to Use
- Viết hoặc tái cấu trúc các file JavaScript trong thư mục `frontend/js/`.
- Xử lý render dữ liệu động từ API lên giao diện HTML (bảng từ vựng, chi tiết bài học, ghi chú cá nhân).
- Quản lý trạng thái chia sẻ (Auth state) hoặc trạng thái cục bộ của trang (Page-local state).
- Gắn sự kiện tương tác qua Event Delegation và xử lý dọn dẹp bộ nhớ (Memory hygiene).
- Kiểm toán an ninh mã nguồn client-side và cấu hình CSP.

# 3. Required Inputs
- Hợp đồng API chuẩn trong `PROJECT-CONTRACT.md` và `frontend-api-integration/references/api-contract.md`.
- Cấu trúc thư viện DOM an toàn: `frontend/js/ui/security.js`.
- Bảng kiểm tra an ninh DOM: `references/xss-and-dom-safety.md`.

# 4. Source of Truth
- **OWASP ASVS 5.0 (V5: Validation, Sanitization and Encoding)** và **OWASP DOM-Based XSS Prevention Cheat Sheet**.
- **WHATWG DOM & HTML Living Standard**.
- `PROJECT-CONTRACT.md` và mã nguồn backend Spring Boot (GlobalExceptionHandler, DTO validation).

# 5. Non-Negotiable Rules
- **No Untrusted Variable Interpolation in Markup**: CẤM TUYỆT ĐỐI dùng template literals hoặc nối chuỗi chứa dữ liệu người dùng/API vào `innerHTML`, `outerHTML`, hoặc `insertAdjacentHTML`.
- **Safe-by-Default Construction**: Ưu tiên `textContent`, `document.createElement()`, `element.setAttribute()` sau khi validate protocol.
- **Contextual URL Sanitization**: Xác thực URL theo đúng ngữ cảnh sử dụng: link điều hướng (`href`) cho phép `http:`, `https:`, relative `/`, `#`, `mailto:`, `tel:`; nguồn tài nguyên (`src`) cho phép `http:`, `https:`, relative `/` (chặn `mailto:`, `tel:`). Tuyệt đối cấm các scheme thực thi như `javascript:`, `data:text/html`, `vbscript:`.
- **No Inline Event Handlers**: Tuyệt đối cấm tạo các thuộc tính sự kiện inline (`onclick`, `onerror`, `onload`). Sử dụng `addEventListener()` hoặc Event Delegation.
- **Module Isolation**: Mỗi trang HTML chỉ nạp đúng 1 page entry module (`<script type="module" src="js/pages/*-page.js">`). Không tạo biến toàn cục trên `window` (ngoại trừ `window.__ENV__` chỉ-đọc cho deployment config).

# 6. Workflow
1. **Module Scoping**: Khởi tạo file module trang trong `frontend/js/pages/`. Nhập các helper từ `frontend/js/ui/security.js` và `frontend/js/api/api.js`.
2. **Untrusted Data Boundary Check**: Nhận diện toàn bộ dữ liệu từ API hoặc user input (chữ Hán, pinyin, ghi chú cá nhân, lý do kiểm duyệt, file Excel) là dữ liệu KHÔNG TIN CẬY.
3. **Safe DOM Construction**:
   - Render text thuần qua `element.textContent`.
   - Tạo element động qua `document.createElement()` hoặc `createSafeElement()`.
   - Xóa trắng container qua `element.replaceChildren()` hoặc `element.innerHTML = ''`.
4. **Event Delegation**: Gắn lắng nghe sự kiện trên container cha ổn định thông qua `event.target.closest('[data-action]')`.
5. **Memory Teardown**: Hủy bỏ `AbortController`, dọn dẹp event listeners trên `window` hoặc interval timers khi điều hướng trang.

# 7. Security Constraints
- Phân biệt 4 cấp độ thao tác DOM:
  1. **SAFE BY DEFAULT**: `textContent`, `createElement()`, `replaceChildren()`, `classList`, `innerHTML = ''`.
  2. **SAFE WITH VALIDATION**: `setAttribute('href', url)` / `setAttribute('src', url)` sau khi kiểm tra protocol whitelist; `location.href` sau khi kiểm tra same-origin.
  3. **SAFE WITH SANITIZATION**: Chỉ áp dụng khi bắt buộc render HTML có chủ đích bằng thư viện chuyên dụng (DOMPurify). Dự án này thuần text, KHÔNG nhận rich text HTML từ user.
  4. **DANGEROUS / PROHIBITED**: Nội suy biến vào `innerHTML`, `outerHTML`, `document.write`, `eval`, `new Function`, `setTimeout(string)`, `javascript:` URLs, inline `on*` attributes.

# 8. Accessibility Constraints
- Khi xóa và dựng lại các nút bấm động qua DOM, đảm bảo giữ nguyên nhãn trợ năng `aria-label` hoặc `.visually-hidden`.
- Khi cập nhật nội dung động trong DOM, bảo đảm vùng thông báo trạng thái có thuộc tính `aria-live="polite"` hoặc `role="status"` để người dùng trợ năng nhận biết.

# 9. Verification
- **Adversarial XSS Injection**: Nhập payload `<img src=x onerror=alert(1)>`, `<script>alert(1)</script>`, và `javascript:alert(1)` vào ô ghi chú, tìm kiếm và link; xác nhận render dưới dạng chuỗi văn bản an toàn hoặc bị trung hòa.
- **DevTools Console Audit**: Xác minh không có lỗi vi phạm CSP hoặc uncaught TypeError.
- **Heap Memory Inspection**: Chuyển đổi qua lại giữa các trang nhiều lần, xác nhận không có memory leak do detached DOM tree hay timer bị rò rỉ.

# 10. Failure Conditions
- Để lọt payload XSS thực thi mã JavaScript trong trình duyệt.
- Dùng `innerHTML = '<div>' + userText + '</div>'` thay vì `textContent`.
- Gán `href` hoặc `src` nhận trực tiếp URL người dùng mà không qua hàm `sanitizeUrl()`.
- Lạm dụng `window` làm nơi lưu trữ state của ứng dụng.

# 11. What NOT to Do
- KHÔNG cài đặt các thư viện nặng (jQuery, React, Vue, Redux). Dự án dùng Vanilla JS ES6+.
- KHÔNG dùng `innerHTML` để chèn icon kèm biến nội suy.
- KHÔNG viết style CSS động nối chuỗi trực tiếp từ input người dùng.
- KHÔNG bỏ qua việc dọn dẹp các event listener trên `window` trong các widget động.

# 12. References
- Cấu trúc thư mục Vanilla JS & Vòng đời khởi tạo trang: Đọc `references/vanilla-js-architecture.md`.
- Ranh giới tin cậy & Quy chuẩn render DOM an toàn chống XSS: Đọc `references/xss-and-dom-safety.md`.
- Quản lý State nhẹ, Event Delegation & Dọn dẹp bộ nhớ: Đọc `references/state-and-event-management.md`.
- Đồ thị tài nguyên thực tế & Cấu hình Content Security Policy (CSP): Đọc `references/csp-and-client-hardening.md`.
