---
name: receiving-code-review
description: Xử lý feedback review một cách có hệ thống. Phân loại issue, không cãi bướng, nhưng cũng không sửa nhắm mắt.
---

# 1. Purpose
Hướng dẫn cách phản hồi lại kết quả review từ người dùng hoặc Subagent một cách có hệ thống. Code review đòi hỏi đánh giá kỹ thuật (technical evaluation), không phải biểu diễn cảm xúc (performative agreement).

# 2. When to use
- Ngay khi nhận được danh sách feedback/comment từ code reviewer.

# 3. When NOT to use
- Đang viết code ban đầu chưa xong.

# 4. Inputs / Preconditions
- Có danh sách feedback từ Code Review.

# 5. Core workflow
1. **READ**: Đọc toàn bộ feedback một cách bình tĩnh.
2. **UNDERSTAND**: Nếu có điểm nào không rõ (Ví dụ: "Fix 1-6" nhưng hiểu 1,2,3,6 còn 4,5 không hiểu), DỪNG LẠI và HỎI ngay lập tức. Đừng sửa 1,2,3,6 rồi mới hỏi.
3. **VERIFY**: Kiểm tra feedback so với thực tế codebase. Đôi khi reviewer có thể sai.
4. **EVALUATE**: Đánh giá sự phù hợp về mặt kỹ thuật. Feedback này có phù hợp với kiến trúc dự án hiện tại không?
5. **RESPOND**: Trả lời bằng luận điểm kỹ thuật hoặc phản biện hợp lý.
6. **IMPLEMENT**: Sửa từng lỗi một theo thứ tự ưu tiên: Critical -> Important -> Minor. Chạy test lại sau mỗi lần sửa.

# 6. Decision points
- Có feedback nào trái ngược với Design/Spec ban đầu không? Nếu có, phải Raise lên để làm rõ.
- Có feedback nào ép dùng công nghệ/khái niệm ngoài Project Constraints không? (Ví dụ: ép dùng React trong khi project chỉ cho phép HTML thuần). Phản biện lại dựa trên `project-constraints.md`.

# 7. Red flags (Forbidden Responses)
- Khen lấy khen để: "Bạn nói quá chuẩn!", "Feedback tuyệt vời!". Cấm performative agreement, chỉ tập trung vào technical.
- Đồng ý nhắm mắt: "Để tôi sửa ngay" khi chưa thực sự hiểu ý của reviewer.
- Lờ đi các issue Critical hoặc cãi bướng vô lý.

# 8. Verification
- Các lỗi được đánh dấu Critical/Important phải được sửa xong.
- Các phản hồi lại reviewer phải có căn cứ (ví dụ trích dẫn code hoặc tài liệu).

# 9. Exit criteria
- Đã xử lý xong (hoặc có giải trình rõ ràng cho) toàn bộ feedback trong package review. Code pass lại các bước Verification thông thường.

<!-- Adapted from obra/superpowers (MIT License) -->
