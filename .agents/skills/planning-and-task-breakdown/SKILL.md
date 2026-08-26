---
name: planning-and-task-breakdown
description: Phân rã Spec thành các task nhỏ có thể thực thi theo cấu trúc Impact-Based Planning, kết hợp kiểm soát Scope.
---

# 1. Purpose
Phân rã Spec thành các task nhỏ, có thể thực thi độc lập (Vertical Slice) và giới hạn đúng Scope (phạm vi), đảm bảo không ai "vẽ rồng vẽ rắn" ngoài yêu cầu. Plan before significant implementation.

# 2. When to use
- Đã có spec/requirement rõ ràng nhưng công việc cần phân rã để không vượt quá khả năng xử lý trong một khối.
- Thay đổi nhiều bước hoặc ảnh hưởng kiến trúc/nhiều layer.

# 3. When NOT to use
- Thay đổi nhỏ và rõ ràng (Ví dụ: sửa 1 dòng CSS, typo) -> Có thể áp dụng `Inspect → Implement → Verify` trực tiếp.
- Khi yêu cầu còn mơ hồ (cần làm `spec-driven-development` trước).

# 4. Inputs / Preconditions
- Đặc tả (Spec) hoặc yêu cầu chi tiết đã được Understand.

# 5. Core workflow
1. **Scope Control**: Xác định ranh giới thay đổi:
   - Required change (Làm).
   - Related required change (Làm).
   - Optional improvement / Unrelated issue / Large refactor (KHÔNG làm, chỉ Report separately).
2. **Impact-Based Workflow**:
   - Tác động tới tầng nào thì lập task tầng đó. Không ép đi qua DB nếu không cần.
3. **Task Definition**: Mỗi task phải đủ cụ thể để agent khác đọc cũng làm được mà không cần đoán. Kế hoạch phải rõ:
   - File nào dự kiến thay đổi.
   - Tại sao cần thay đổi.
   - Dependency / Data flow.
   - Acceptance criteria / Verification method.

# 6. Decision points
- Có issue nào phát hiện ngoài scope không? Báo cáo riêng, không gộp âm thầm vào task hiện tại.
- Kế hoạch này có "testable" (kiểm thử được độc lập) sau mỗi Checkpoint không?

# 7. Red flags
- Plan viết chung chung như: "Update backend, Create API, Fix frontend".
- Tự mở rộng scope (Feature expansion, Unrelated bug fixes) mà không có sự đồng ý.
- Ghi chú Acceptance Criteria thừa thãi, không thể kiểm thử.

# 8. Verification
- Kế hoạch (`task.md` hoặc checklist) tuân thủ đúng nhánh Impact của nó, có phương pháp verify rõ ràng cho từng task.
- Không có bất kỳ task nào "lạc" ra ngoài scope yêu cầu ban đầu.

# 9. Exit criteria
- Có file kế hoạch (Implementation plan) rõ ràng, mỗi task cụ thể đến mức độ file/hàm, đã được review/approve trước khi Implement.
