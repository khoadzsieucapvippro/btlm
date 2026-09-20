---
name: frontend-verification
description: Quản lý hạ tầng kiểm thử giao diện Frontend tích lũy (Cumulative Regression), thực thi phân tầng Test Pyramid (L0 Static, L1 Unit, L2 Browser Smoke, L3 A11y, L4 Subagent Acceptance), quản lý vòng đời server tự động và đảm bảo Zero Runtime Overhead.
---

# 1. Purpose
Xác lập quy chuẩn kiểm chứng giao diện người dùng tự động hóa và tích lũy (Cumulative Frontend Verification) cho toàn bộ Phase 9. Đảm bảo mọi task frontend khi phát triển mới đều phải bổ sung test tương ứng vào bộ regression suite chính thức tại `tests/frontend/`, và bắt buộc toàn bộ test cũ của các task trước đó đều phải PASS trước khi bàn giao.

# 2. When to Use
- Bắt đầu triển khai hoặc nghiệm thu bất kỳ task frontend nào (Task 9A.1, 9A.2, Module 9B, 9C, v.v.).
- Trước khi báo cáo hoàn thành bất kỳ thay đổi nào trong `frontend/`.
- Khi cần kiểm tra nhanh tính an toàn DOM, tính ngữ nghĩa HTML5, token CSS hoặc tính năng tiếp cận WCAG 2.2 Level AA.

# 3. Required Inputs
- Toàn bộ mã nguồn `frontend/`.
- Bộ kiểm thử tại `tests/frontend/`.
- Bộ runner tập trung: `tests/frontend/runner.mjs`.

# 4. Source of Truth
- **Playwright Best Practices** (User-visible locators, test isolation, resilient assertions).
- **Node.js Native Test Runner (`node:test`, `node:assert/strict`)**.
- **W3C WCAG 2.2 Level AA** & **WAI-ARIA APG**.
- **OWASP DOM-Based XSS Prevention Cheat Sheet**.
- `PROJECT-CONTRACT.md` & `references/playwright-e2e-patterns.md`.

# 5. Non-Negotiable Rules
- **Cumulative Regression Invariant**: Mọi task frontend mới BẮT BUỘC phải chạy toàn bộ regression suite (`npm run verify:frontend`). Không bao giờ được phép chỉ chạy riêng test của task hiện tại rồi báo DONE.
- **Zero Frontend Runtime Dependency**: Mã nguồn sản phẩm trong `frontend/` là Vanilla JS ES6+ thuần túy, tuyệt đối KHÔNG có runtime npm dependencies. Mọi công cụ kiểm thử chỉ được nằm trong `devDependencies` (`package.json`).
- **Strict Test Pyramid**:
  - Không biến Browser E2E hay Browser Subagent thành feedback loop chính.
  - Phân tầng rõ rệt: L0 Static + L1 Unit (Fast, $< 1\text{s}$) $\rightarrow$ L2 Browser Smoke (Representative, $\approx 5\text{–}8\text{s}$) $\rightarrow$ L3 A11y (WCAG SC 2.5.8, focus, $\approx 3\text{–}5\text{s}$) $\rightarrow$ L4 Browser Subagent Acceptance.
- **Dual Error Monitoring**: Mọi kịch bản browser test BẮT BUỘC phải bắt đồng thời cả `pageerror` (uncaught exceptions) và `console.error`. Nếu có bất kỳ lỗi console hoặc unhandled exception nào xuất hiện, test case phải đánh FAIL.
- **Server Lifecycle Hygiene**: Bộ runner tự động nhận diện server đang chạy để tái sử dụng; chỉ tạo server ephemeral khi chưa có server và bắt buộc dọn dẹp sạch sẽ khi kết thúc. Tuyệt đối không kill server bên ngoài của người dùng.

# 6. Workflow
1. **Phát triển tính năng mới**: Viết mã nguồn HTML/CSS/Vanilla JS trong `frontend/`.
2. **Bổ sung test tương ứng**:
   - Thêm unit test vào `tests/frontend/unit/` (logic thuần, contract).
   - Thêm browser smoke vào `tests/frontend/e2e/` (luồng người dùng thực tế).
   - Cập nhật checklist kiểm thử thủ công trong `tests/frontend/manual/FRONTEND-QA.md`.
3. **Thực thi vòng lặp kiểm chứng**:
   - Chạy kiểm thử siêu tốc: `npm run verify:frontend:fast` (~0.2s).
   - Chạy chốt chặn kiểm thử: `npm run verify:frontend:gate` (~7s).
   - Chạy toàn bộ regression tích lũy: `npm run verify:frontend`.
4. **Nghiệm thu trực quan**: Sử dụng Browser Subagent để kiểm tra giao diện thực tế và ghi nhận evidence (video/screenshot).

# 7. Verification Commands
```bash
npm run verify:frontend:fast     # L0 Static + L1 Unit (~0.2s)
npm run verify:frontend:browser  # L2 Browser Smoke (~7s)
npm run verify:frontend:a11y     # L3 Accessibility Checks (~4s)
npm run verify:frontend:gate     # Gate toàn diện trước khi commit (~7s)
npm run verify:frontend          # Full Cumulative Regression Suite (~8s)
```

# 8. Failure Conditions
- Bỏ qua regression test của các task trước đó.
- Thêm thư viện npm vào `frontend/`.
- Để lọt lỗi uncaught exception hoặc console.error trên trình duyệt.
- Tự chế tạo mini testing framework hoặc lặp lại test browser trên nhiều viewport mà không có lý do kiến trúc.
