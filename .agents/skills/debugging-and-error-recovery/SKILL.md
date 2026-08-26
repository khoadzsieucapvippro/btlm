---
name: debugging-and-error-recovery
description: Gỡ lỗi hệ thống bài bản (Systematic Debugging). Chống đoán mò, tìm root cause trước khi fix.
---

# 1. Purpose
Định hướng gỡ lỗi bài bản theo 4 Phase (Reproduce, Root cause, Minimal fix, Verify), cấm tuyệt đối tư duy sửa đại đoán mò.

# 2. When to use
- Có lỗi xuất hiện trong console, log Spring Boot hoặc trình duyệt.
- API trả về 500 hoặc 400 không rõ lý do.
- Test fail.

# 3. When NOT to use
- Hệ thống không có lỗi, đang cần develop feature mới.

# 4. Inputs / Preconditions
- Có dấu vết lỗi (StackTrace, error message, hoặc repro steps).

# 5. Core workflow (Systematic Debugging)
- DỪNG NGAY việc code tính năng mới (Stop-the-line).
- Tuân thủ 4 Phases:
  1. **Phase 1 — Reproduce and gather evidence**: Chạy lại hệ thống, chứng kiến lỗi tận mắt. Xác định điều kiện gây ra lỗi.
  2. **Phase 2 — Identify root cause**: Phân tích evidence (đọc log, xem stacktrace từ Caused By...). Phải trả lời được nguyên nhân thực sự là gì, thay vì chỉ thấy triệu chứng.
  3. **Phase 3 — Implement minimal correct fix**: Sửa MỘT phần nhỏ nhất đủ để giải quyết Root cause.
  4. **Phase 4 — Verify fix and check regression**: Chạy lại repro steps ở Phase 1. Đảm bảo lỗi đã hết VÀ hệ thống không sinh ra lỗi mới.

# 6. Decision points
- Trả lời 6 câu hỏi trước khi chốt fix:
  1. What exactly is failing?
  2. How was the failure reproduced?
  3. What evidence identifies the root cause?
  4. Why does this change address the root cause?
  5. How was the fix verified?
  6. What regression risk was checked?

# 7. Red flags
- Vòng lặp đoán mò vô tận: `Error → Guess → Edit random code → Error changes → Guess again`.
- Chỉ sửa cái ngọn (sửa triệu chứng) mà không tìm Root cause.
- Verification thất bại nhiều lần hoặc nguyên nhân không rõ nhưng vẫn cố sửa bừa. (Khi đó phải STOP guessing và Re-investigate từ evidence).

# 8. Verification
- Phải dùng đúng evidence đã thu thập ở Phase 1 để verify ở Phase 4. (Ví dụ: Chạy lại đúng request cURL đó, click lại đúng nút đó).

# 9. Exit criteria
- Đã thu thập đủ evidence mới chứng minh lỗi biến mất. Tính năng chạy mượt, không có regression.

# 10. References to load conditionally
- Đang loay hoay vì lỗi quá sâu trong stack, không biết gốc ở đâu? Đọc `references/root-cause-tracing.md`.
- Lỗi liên quan đến data rác, cần chặn từ xa? Đọc `references/defense-in-depth.md`.
- Lỗi chập chờn do timing/async/browser testing? Đọc `references/condition-based-waiting.md`.

<!-- Adapted from obra/superpowers (MIT License) -->
