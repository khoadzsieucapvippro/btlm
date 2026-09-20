---
name: frontend-code-review
description: Đánh giá chất lượng, bảo mật, khả năng tiếp cận (a11y) và tính thẩm mỹ mã nguồn Frontend trước khi hoàn tất dựa trên Actual Git Diff. Chặn triệt để lỗi DOM XSS, jQuery, vi phạm hợp đồng API, và khuôn mẫu AI SaaS generic.
---

# 1. Purpose
Thực hiện chốt chặn kiểm duyệt chất lượng kỹ thuật, tính toàn vẹn hợp đồng API, bảo mật client-side (DOM XSS), khả năng tiếp cận (WCAG 2.2 AA) và tiêu chuẩn giao diện người dùng cho mã nguồn Frontend trước khi đánh dấu hoàn thành bất kỳ task frontend nào. Mọi đánh giá phải dựa trên bằng chứng mã nguồn thực tế thu thập từ `git diff frontend/`.

# 2. When to Use
- Sau khi hoàn thành code giao diện HTML/CSS hoặc logic JavaScript cho một task Frontend, trước khi chuyển task sang trạng thái COMPLETED.
- Khi nhận code review feedback hoặc chuẩn bị bàn giao module frontend.
- Khi kiểm tra tính tuân thủ quy tắc Design-Before-Code và rà soát các mẫu thiết kế rập khuôn.

# 3. Required Inputs
- Toàn bộ thay đổi mã nguồn frontend đã được lưu trên đĩa (`git status` hiển thị đúng các file liên quan).
- Bản so sánh thay đổi thực tế từ `git diff frontend/`.
- Hợp đồng API chuẩn hóa tại `.agents/skills/frontend-api-integration/references/api-contract.md`.
- Bảng kiểm tra 9 trục tại `references/frontend-review-checklist.md`.
- Quy chuẩn thiết kế chống mẫu generic tại `references/anti-template-and-design-governance.md`.

# 4. Source of Truth
1. **Mã nguồn thực tế (`git diff frontend/`)**: Bằng chứng duy nhất xác nhận code làm gì; không tin tưởng các tuyên bố cảm tính hoặc báo cáo markdown cũ.
2. **Authoritative Backend Contracts**: `api-contract.md` và mã nguồn Java controller/DTO thực tế.
3. **OWASP ASVS 4.0.3 / 5.0 & W3C WCAG 2.2**: Tiêu chuẩn an ninh và khả năng tiếp cận có hiệu lực quốc tế.
4. **Project Contract**: `PROJECT-CONTRACT.md` và các quy chuẩn kiến trúc đã được phê duyệt.

# 5. Non-Negotiable Rules
- **No Uninterpolated innerHTML**: Tuyệt đối không nội suy dữ liệu động vào `innerHTML`, `outerHTML`, hoặc `insertAdjacentHTML`.
- **No Raw fetch in UI Modules**: Mọi tương tác mạng phải đi qua `apiClient` tập trung; không gọi `window.fetch()` trực tiếp trong file view/page.
- **Contract-Compliant DTO Consumption**: Tiêu thụ đúng tên trường thực tế (ví dụ: `response.data.token`, KHÔNG ĐƯỢC dùng `response.data.accessToken`).
- **HTTP 204 Handling**: Kiểm tra và xử lý an toàn cho endpoint trả về HTTP 204 No Content (DELETE admin/creator) mà không gọi `response.json()`.
- **No Banned Frameworks/Libs**: Không cài đặt hoặc nhúng jQuery, React, Vue, Angular, Tailwind CSS, Axios. Kiến trúc là HTML5 / CSS3 / Vanilla JS ES6+ kết hợp Bootstrap 5.3 CDN đã qua kiểm định SRI.
- **Accessibility Baselines**: Semantic HTML5 landmarks (<header>, <nav>, <main>, <footer>), nhãn biểu mẫu kết hợp `aria-describedby` cho thông báo lỗi, độ tương phản văn bản đạt tối thiểu 4.5:1 (WCAG SC 1.4.3), phím Tab/Enter/Space hoạt động trơn tru.

# 6. Workflow
1. **Extract Actual Diff**: Chạy lệnh kiểm tra thay đổi thực tế trên các file frontend (`git diff frontend/`).
2. **Review Against 9-Axis Checklist**: Đối chiếu từng thay đổi với bảng kiểm tra 9 trục (Architecture, Security & DOM Safety, API Contract, State & UI, Accessibility WCAG 2.2, Responsive, CJK Typography & Identity, Performance & Lifecycle, Dependencies) (Đọc `references/frontend-review-checklist.md`).
3. **Scan for Immediate Blockers**: Kiểm tra toàn bộ danh mục từ chối tức thì (Mandatory Rejection Triggers):
   - Có dùng `innerHTML` với biến nội suy không? $\rightarrow$ **REJECT**.
   - Có dùng `onclick="..."` inline không? $\rightarrow$ **REJECT**.
   - Có gọi `fetch()` trần trụi ngoài `apiClient` không? $\rightarrow$ **REJECT**.
   - Có nhúng jQuery, React, Tailwind không? $\rightarrow$ **REJECT**.
   - Có lạm dụng gradient tím/xanh hoặc template drift generic không? $\rightarrow$ **REJECT** (Đọc `references/anti-template-and-design-governance.md`).
4. **Inspect Responsive & States**: Xác nhận bố cục hoạt động ở mobile (<768px), tablet, desktop; có đủ 3 trạng thái giao diện (Loading, Empty, Error).
5. **Action & Resolution**: Khắc phục trực tiếp các lỗi phát hiện được trong phạm vi task trước khi cấp quyền hoàn tất.

# 7. Security Constraints
- **CWE-79 (DOM-based XSS)**: Toàn bộ dữ liệu người dùng kiểm soát (Ghi chú, Từ vựng, Hanzi, Pinyin, Nghĩa, Feedback, Lỗi server) phải được gán qua `textContent` hoặc DOM element primitives an toàn.
- **URL Sanitization**: Tất cả URL động gắn vào `href` hoặc `src` phải được xác thực theo whitelist giao thức nghiêm ngặt (`http:`, `https:`, relative `/`, `#`, `mailto:`, `tel:`). Chặn triệt để `javascript:` và `data:text/html`.
- **Token Handling**: Không ghi log token hoặc thông tin xác thực vào console hay DOM attributes; thu hồi trạng thái đăng nhập sạch sẽ khi nhận HTTP 401 hoặc khi đăng xuất phía client.

# 8. Accessibility Constraints
- Thiết kế và kiểm chứng theo các tiêu chí thành công áp dụng của WCAG 2.2 Cấp độ AA (WCAG 2.2 Level AA success criteria). Kiểm toán tự động chỉ là bằng chứng hỗ trợ, không thay thế cho kiểm tra thủ công bàn phím và ngữ nghĩa trợ năng.
- Cấm div/span soup; sử dụng native HTML elements (<button>, <a>, <input>, <form>).
- Các icon-only buttons bắt buộc có `aria-label` hoặc `.visually-hidden` text.
- Lỗi biểu mẫu phải được liên kết ngữ nghĩa với trường nhập liệu bằng `aria-describedby` và đánh dấu `aria-invalid="true"`.
- Đảm bảo focus indicator hiển thị rõ ràng (không dùng `outline: none` mà không có kiểu focus thay thế).

# 9. Verification
- Chạy `git diff frontend/` và xác nhận không có bất kỳ dòng diff nào vi phạm 9 trục kiểm tra.
- Thực thi DevTools audit hoặc browser testing xác nhận zero console errors, zero failed network requests bất thường, và không có rò rỉ bộ nhớ listener.
- Xác nhận các kiểm tra tương phản màu sắc và bàn phím (Tab, Shift+Tab, Enter, Space; phím Escape trên dismissible overlays) hoạt động đúng theo mẫu tương tác áp dụng.

# 10. Failure Conditions
- Phát hiện bất kỳ vi phạm nào trong Danh mục từ chối tức thì (Mandatory Rejection Triggers).
- Bỏ qua bước kiểm tra `git diff` và khẳng định cảm tính rằng "code đã hoàn hảo".
- Bỏ qua việc bọc container `.table-responsive` cho bảng dữ liệu lớn gây tràn màn hình ngang trên thiết bị di động.
- Để sót lỗi HTTP 204 No Content gây crash JSON parser khi xóa thực thể.

# 11. What NOT to Do
- KHÔNG phê duyệt PR hoặc task chỉ dựa trên lời cam đoan mà không đọc diff thực tế.
- KHÔNG cho phép dùng `innerHTML` với lý do "dữ liệu từ backend của mình nên an toàn".
- KHÔNG từ chối giao diện một cách cảm tính mà không chỉ ra điều khoản vi phạm trong tiêu chuẩn thiết kế hoặc checklist.
- KHÔNG yêu cầu viết lại backend hoặc tự ý bịa đặt endpoint khi frontend gặp khó khăn trong việc hiển thị dữ liệu.

# 12. References
- Bảng kiểm tra 9 trục & Danh mục từ chối tức thì: Đọc [frontend-review-checklist.md](references/frontend-review-checklist.md).
- Quy chuẩn chống khuôn mẫu AI SaaS & Nguyên tắc Design-Before-Code: Đọc [anti-template-and-design-governance.md](references/anti-template-and-design-governance.md).
- Hợp đồng API Frontend chuẩn hóa: Đọc [api-contract.md](../frontend-api-integration/references/api-contract.md).
- Hướng dẫn an toàn DOM: Đọc [xss-and-dom-safety.md](../vanilla-js-dom-security/references/xss-and-dom-safety.md).
