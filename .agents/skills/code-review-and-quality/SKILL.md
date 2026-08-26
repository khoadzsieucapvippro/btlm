---
name: code-review-and-quality
description: Đánh giá chất lượng mã nguồn trước khi hoàn tất dựa trên Actual Diff. Sử dụng sau khi code xong nhưng trước khi báo DONE.
---

# 1. Purpose
Kiểm tra chéo và nâng cao chất lượng mã nguồn (clean code, security, performance) thông qua Review dựa trên Actual Git Diff thay vì cảm tính.

# 2. When to use
- Trước khi chốt một task, module, một commit lớn hoặc một PR.
- Khi cần rà soát lại các thay đổi vừa thực hiện.

# 3. When NOT to use
- Đang trong quá trình debug, test còn fail (code chưa hoạt động).

# 4. Inputs / Preconditions
- Code đang hoạt động (test pass, chạy được).
- Có thông tin về Actual Diff (ví dụ dùng Git diff).

# 5. Core workflow
1. **Inspect Diff**: Xem lại toàn bộ thay đổi thực tế (`git diff` hoặc tương đương).
2. **Review against requirement**: Code có đáp ứng yêu cầu ban đầu không?
3. **Five-Axis & Scope Review**:
   - Scope creep: Có sửa lan man ngoài lề không?
   - Correctness: Edge cases, validation, error handling có đủ không?
   - Architecture & Conventions: Có vi phạm lớp, duplicate code, hay phá vỡ project conventions không?
   - Security: Có lộ secret, nguy cơ injection không?
   - Performance: N+1 query, resource leak?
4. **Action**: 
   - Fix trực tiếp nếu thuộc scope của task.
   - Report separately nếu phát hiện issue ngoài scope.

# 6. Decision points
- Việc refactor có tốn quá nhiều thời gian không? Nếu code đủ tốt và đạt yêu cầu, hãy Approve.

# 7. Red flags
- Tự review bằng niềm tin: "Tôi tự viết nên tôi biết nó đúng" (I wrote it, therefore it is correct).
- Review mà KHÔNG thèm đọc qua Actual Diff.
- Im lặng nuốt (silently include) các thay đổi ngoài scope vào chung một commit.

# 8. Verification
- Code gọn gàng hơn.
- Không có rủi ro Regression (Test vẫn pass).

# 9. Exit criteria
- Đã review qua Actual Diff, không phát hiện vi phạm rule chất lượng và scope.
