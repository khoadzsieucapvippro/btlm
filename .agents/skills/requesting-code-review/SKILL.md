---
name: requesting-code-review
description: Chuẩn bị package Code Review hoàn chỉnh trước khi nhận việc hoàn thành. Tránh tự chấm điểm mình một cách cảm tính.
---

# 1. Purpose
Tránh việc tự review bản thân một cách phiến diện ("tôi tự code thì tôi nghĩ nó đúng"). Cần tạo ra một Review Package đầy đủ ngữ cảnh để trình bày (với user hoặc reviewer subagent) nhằm đối chiếu lại với yêu cầu ban đầu.

# 2. When to use
- Bắt buộc: Sau khi hoàn thành một task, một feature lớn hoặc trước khi merge code vào nhánh chính.
- Bắt buộc: Trước khi kích hoạt `verification-before-completion`.

# 3. When NOT to use
- Khi đang loay hoay debug lỗi, code chưa chạy được.
- Đang tìm hiểu cấu trúc dự án.

# 4. Inputs / Preconditions
- Code chạy được, test pass.
- Nắm rõ Yêu cầu ban đầu (Original requirements).

# 5. Core workflow
1. **Prepare Diff**: Lấy Git diff hoặc list chính xác các thay đổi.
   `git diff HEAD~1` hoặc `git diff origin/main`.
2. **Prepare Context**: Mô tả ngắn gọn: Code này làm gì? Thay đổi những file chính nào?
3. **Establish Acceptance Criteria**: Nhắc lại spec ban đầu.
4. **Compile the Package**: Gộp Diff + Context + Criteria thành một khối thông tin rõ ràng.
5. **Request Review**: 
   - Nếu User là reviewer: Trình bày package này ra màn hình.
   - Nếu có Subagent (Code Reviewer): Gửi package này qua message cho subagent để đánh giá chéo.

# 6. Decision points
- Có cần test integration trước khi review không? (Tốt nhất là Test xong mới đưa đi Review).
- Lượng diff có quá lớn không? Nếu > 500 dòng, nên chia nhỏ thành nhiều package review.

# 7. Red flags
- Bỏ qua review và nhảy thẳng tới báo cáo "Done".
- Báo cáo review nhưng chỉ nói miệng "Code is clean" mà không show Git diff.
- Xóa lịch sử thay đổi để che giấu diff.

# 8. Verification
- Một package review hoàn chỉnh phải bao gồm 3 yếu tố: [What was implemented], [Original Plan/Spec], và [Actual Diff].

# 9. Exit criteria
- Package review được gửi đi thành công cho User hoặc Reviewer Subagent.

<!-- Adapted from obra/superpowers (MIT License) -->
