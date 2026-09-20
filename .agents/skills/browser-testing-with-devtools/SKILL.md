---
name: browser-testing-with-devtools
description: Kiểm tra giao diện và API trên trình duyệt bằng Chrome DevTools và tự động hóa (Playwright nếu đã được cấu hình) cho 4 hành trình người dùng (Learner, Creator, Moderator, Admin). BẮT BUỘC trước khi báo DONE frontend.
---

# 1. Purpose
Đảm bảo toàn bộ mã nguồn Frontend (HTML/CSS/Vanilla JS) thực sự vận hành chính xác trên môi trường trình duyệt thực tế, không có lỗi JavaScript runtime, không vi phạm CSP, xử lý đúng mã lỗi HTTP và giao tiếp thông suốt với Spring Boot backend. Sử dụng kiểm tra tương tác trực quan qua Chrome DevTools / browser automation / MCP, kết hợp với bộ kiểm thử tự động hóa đầu cuối E2E nếu repository đã được cấu hình.

# 2. When to Use
- Vừa hoàn thành viết hoặc chỉnh sửa bất kỳ file HTML, CSS hoặc Vanilla JS nào.
- Trước khi báo cáo hoàn thành bất kỳ task frontend hoặc tích hợp giao diện nào.
- Khi kiểm tra tính tương thích, mã trạng thái HTTP, luồng xác thực và khả năng đáp ứng trên các khung nhìn (viewports).
- Khi chạy kiểm thử E2E tự động hóa (nếu dự án đã cấu hình bộ công cụ kiểm thử).

# 3. Required Inputs
- Hợp đồng API chuẩn xác: `PROJECT-CONTRACT.md` và `frontend-api-integration/references/api-contract.md`.
- Backend Spring Boot đang chạy trên cổng cấu hình (mặc định `:8080`).
- Môi trường phục vụ HTTP/HTTPS local (ví dụ: static resource handler của Spring Boot, Live Server, hoặc local HTTP static server). CẤM dùng `file://` protocol.

# 4. Source of Truth
- **Chrome DevTools Official Documentation (Chromium)**.
- **W3C WebDriver & Browser Automation Specifications**.
- **MDN Web Docs (Same-Origin Policy, CORS, Storage Boundaries)**.

# 5. Non-Negotiable Rules
- **HTTP/HTTPS Origin Requirement**: Kiểm thử tích hợp BẮT BUỘC chạy trên local HTTP/HTTPS origin. Tuyệt đối KHÔNG dùng `file://` làm môi trường tích hợp chính (gây lỗi CORS `Origin: null`, chặn module ES6 và sai lệch cookie/storage).
- **Observable Evidence Standard**: Không chấp nhận khẳng định "code trông đúng". Mọi báo cáo hoàn thành phải dựa trên bằng chứng quan sát được: không uncaught console errors, mã trạng thái HTTP chính xác, DOM phản hồi đúng.
- **Double Submit Verification**: Kiểm tra thao tác nhấp đúp/nhấp nhanh vào nút gửi yêu cầu; chỉ đúng 1 request mutation được phép gửi tới backend.
- **Strict Error Code Verification**: Xác minh giao diện ứng xử chính xác trước các mã lỗi: 400 (hiển thị field errors), 401 (xóa token và chuyển hướng), 403 (cảnh báo quyền, không xóa token), 409 (xung đột dữ liệu / ôn tập sớm), 429 (thông báo giới hạn tần suất).

# 6. Workflow
1. **Khởi động môi trường**: Khởi chạy Spring Boot backend và static server local phục vụ frontend.
2. **Interactive 5-Pillar DevTools Inspection**:
   - Console: Xác minh 0 uncaught exception.
   - Network: Đường dẫn `/api/v1/*`, header `Authorization: Bearer`, xử lý đúng payload và mã HTTP 204 No Content không body.
   - Elements: Cấu trúc HTML5 ngữ nghĩa, nhãn `<label for>`, vùng bấm $\ge 24\times 24\text{px}$, focus ring hiển thị rõ (SC 2.4.7).
   - Device Toolbar: Kiểm tra viewport Mobile (<768px), Tablet (768–991px), Desktop ($\ge 992\text{px}$), không có thanh cuộn ngang toàn trang.
   - Network Throttling: Chuyển mạng sang Slow 3G để kiểm tra Loading spinner, ngắt mạng kiểm tra thông báo lỗi mạng.
3. **Toolchain-Aware Verification**:
   - **NẾU repository đã cấu hình Playwright trong `e2e/`**: Thực thi các kịch bản kiểm thử đã cấu hình.
   - **NẾU repository CHƯA cấu hình Playwright**: KHÔNG tự ý cài đặt package npm chỉ để thỏa mãn skill. Thực hiện kiểm chứng đầy đủ thông qua Chrome DevTools thủ công, DevTools Protocol, hoặc subagent điều khiển trình duyệt.

# 7. Security Constraints
- Xác minh token không bị rò rỉ trong URL hoặc console logs.
- Kiểm tra CSP violations trên Console; không sử dụng inline scripts (`<script>` inline hay `on*` inline handlers).
- Kiểm tra các payload XSS trong form tìm kiếm/ghi chú không thực thi mã độc trong DOM.

# 8. Accessibility Constraints
- Sử dụng phím `Tab` để di chuyển qua các phần tử tương tác; đảm bảo focus indicator hiển thị rõ ràng và không bị che khuất bởi header cố định.
- Thao tác đóng dismissible overlays/dialogs bằng phím `Escape`; xác nhận focus quay trở lại nút bấm đã kích hoạt.

# 9. Verification
- Hoàn thành checklist 5 trụ cột trong `references/devtools-verification-checklist.md`.
- Ghi nhận log Console sạch (zero errors) và Network requests thành công làm bằng chứng xác thực.
- Nếu Playwright được cấu hình sẵn trong repository, chạy bộ test tự động hóa tương ứng.

# 10. Failure Conditions
- Có uncaught runtime exception (`TypeError`, `ReferenceError`) trên Console.
- Chạy ứng dụng qua giao thức `file://` khiến các lệnh gọi API bị chặn CORS.
- Nút submit cho phép click liên tục gửi nhiều mutation trùng lặp.
- Trang bị tràn ngang (horizontal overflow) trên mobile viewport $\ge 320\text{px}$.

# 11. What NOT to Do
- KHÔNG báo cáo task hoàn thành mà không mở trình duyệt kiểm tra thực tế.
- KHÔNG tự ý cài đặt `npm` / `playwright` vào dự án zero-build nếu repository chưa thiết lập môi trường đó.
- KHÔNG bỏ qua tab Network để lọt các request gửi sai payload hoặc thiếu token.
- KHÔNG bỏ qua việc kiểm tra bàn phím trước khi bàn giao giao diện.

# 12. References
- Quy trình kiểm tra chi tiết Chrome DevTools 5 trụ cột: Đọc [devtools-verification-checklist.md](references/devtools-verification-checklist.md).
- Kiến trúc kiểm thử tự động hóa E2E Playwright (tùy chọn theo cấu hình dự án): Đọc [playwright-e2e-patterns.md](references/playwright-e2e-patterns.md).
