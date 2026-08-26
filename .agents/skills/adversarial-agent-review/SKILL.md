---
name: adversarial-agent-review
description: Multi-agent debate review để xác minh chéo các quyết định rủi ro cao (kiến trúc, bảo mật, refactor lớn). Phân tách độc lập, dựa trên bằng chứng, giới hạn số vòng tranh luận.
---

# 1. Purpose
Tăng tính chính xác của các quyết định kỹ thuật bằng cách bắt buộc các subagent phải lập luận độc lập (không groupthink), đối chất (cross-examine) dựa trên bằng chứng thực tế, và ra phán quyết rõ ràng thay vì đồng thuận một chiều.

# 2. When to use
Sử dụng khi có rủi ro cao hoặc độ không chắc chắn lớn:
- Quyết định thay đổi kiến trúc lớn, công nghệ mới.
- Schema migration database diện rộng.
- Các PR, thay đổi liên quan đến Security/Authentication.
- Diagnosis bug phức tạp hoặc refactor có nguy cơ regression.
- Khi các agent có quan điểm mâu thuẫn về mặt kỹ thuật.

# 3. When NOT to use
- Tuyệt đối KHÔNG dùng cho các task nhỏ, typo, format code, 1-line fix, hoặc các công việc cơ học rõ ràng (Sẽ gây tốn tài nguyên vô ích).

# 4. Inputs / Preconditions
- Đã đóng gói Review Package (Context, Requirements, Actual Diff / Original Code).
- Không được đưa kết luận hoặc phán đoán của Agent A cho Agent B xem trong lần review đầu tiên.

# 5. Core workflow (The 4-Phase Protocol)
1. **PHASE 1: INDEPENDENT ANALYSIS**: Kích hoạt 2 subagent (Role A và Role B). Cung cấp chung Context nhưng cấm chúng giao tiếp với nhau. Yêu cầu từng subagent trả về `Findings` (ID, Severity, Claim, Evidence).
2. **PHASE 2: CROSS-EXAMINATION**: Kích hoạt Role C (Adversarial Critic). Cung cấp input là Findings của Role A và Role B. Role C tìm cách phản biện các kết luận vô căn cứ, tìm bằng chứng mâu thuẫn, và gắn status (`CONFIRMED`, `CHALLENGED`, `FALSE POSITIVE`, `INSUFFICIENT EVIDENCE`).
3. **PHASE 3: REBUTTAL**: Nếu có finding bị `CHALLENGED`, Role A/B được quyền phản hồi đúng MỘT vòng duy nhất (chấp nhận hoặc đưa thêm bằng chứng mới). Không tranh cãi dai dẳng.
4. **PHASE 4: SYNTHESIS**: Role D (Synthesizer/Judge) tổng hợp toàn bộ bằng chứng, phân định đúng sai không dựa vào số đông. Đưa ra `Final Verdict` (APPROVED, CHANGES REQUIRED, BLOCKED).

# 6. Decision points
- **Severity**: Có bị thổi phồng không? CRITICAL chỉ dành cho hổng bảo mật, mất data, app crash.
- **Internet vs Project Reality**: Lời khuyên "Best Practice" từ mạng có vi phạm Constraints hoặc config hiện tại của dự án không? (VD: Khuyên dùng Redis nhưng dự án quy định chỉ dùng Caching in-memory).

# 7. Red flags (Anti-Groupthink)
- Agent B copy kết luận của Agent A.
- Phê duyệt vì "Có nhiều Agent đồng ý" (Majority vote != Correctness).
- Khẳng định "Tôi nghĩ thế", "Best practice chung là thế" nhưng KHÔNG có dòng code, file path, hoặc log nào làm bằng chứng.
- Tranh luận quá 1 vòng mà không có bằng chứng mới.

# 8. Verification
- Mọi phán quyết cuối cùng phải trích dẫn Evidence (Project reality > Official Docs > Generic Best Practice).
- Không có issue CRITICAL / HIGH nào bị bỏ ngỏ.

# 9. Exit criteria
- Cung cấp báo cáo tổng hợp (Adversarial Review Result) liệt kê: Confirmed Issues, Challenged Findings, Unresolved Issues, và Final Verdict dựa trên Evidence.

# 10. References to load conditionally
- `prompts/independent-reviewer.md`: Template dùng để kích hoạt Role A/B (Phase 1).
- `prompts/adversarial-critic.md`: Template dùng để kích hoạt Role C (Phase 2).
- `prompts/synthesizer.md`: Template dùng để kích hoạt Role D (Phase 4).

<!-- Adapted from obra/superpowers, alecnielsen/adversarial-review, gumbel-ai/agent-debate -->
