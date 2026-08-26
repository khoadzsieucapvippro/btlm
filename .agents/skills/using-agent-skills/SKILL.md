---
name: using-agent-skills
description: Meta guidance for selecting relevant engineering skills based on task impact. (Skill-First, No Guessing, Skill Precedence).
---

# 1. Purpose
Sử dụng như bảng điều hướng (Routing) để gọi đúng các skill chuyên môn dựa trên tác động của task (Impact). Thống nhất nguyên tắc: Skill-First, No Guessing, và Skill Precedence.

# 2. When to use
- Khi bắt đầu một task để phân loại và xác định các skill chuyên môn cần kích hoạt.
- Ngay khi nhận được bất kỳ yêu cầu nào (để lên kế hoạch áp dụng skill).

# 3. When NOT to use
- Đã xác định rõ ràng skill chuyên biệt và đang tập trung thực thi task đó.

# 4. Inputs / Preconditions
- Đã hiểu yêu cầu cơ bản của task và nắm được Rule tổng trong `project-constraints.md` và `engineering-guardrails.md`.

# 5. Core workflow
1. **Skill-First Principle**: 
   - Xác định skill nào có thể áp dụng.
   - Nếu có skill phù hợp thì PHẢI đọc và tuân thủ skill đó.
   - Không bỏ qua skill vì lý do "This is simple" hoặc "I already know how to do this".
2. **No Guessing Principle**:
   - Inspect → Infer from evidence → Verify.
   - KHÔNG đoán file structure, đoán API tồn tại, đoán DB schema.
   - Nếu thiếu thông tin: Search/read trước. Không hỏi user nếu có thể tự tìm.
   - Nếu bắt buộc hỏi, phải State rõ: What is known, What was inspected, What is missing, Why it blocks.
3. **Skill Precedence Principle**:
   - 1. Project constraints.
   - 2. Security and hard constraints.
   - 3. Highly specific task skill.
   - 4. Domain/technology skill.
   - 5. General engineering workflow skill.
   - 6. General agent behavior.
4. **Phân loại task theo Impact**:
   - Simple UI/CSS -> `frontend-ui-engineering`
   - New feature -> `spec-driven-development` -> `planning-and-task-breakdown` -> `incremental-implementation`
   - DB change -> `database-flyway-mysql`
   - Backend/API -> `spring-boot-backend` -> `api-and-interface-design`
   - JWT/Auth -> `spring-security-jwt` -> `security-and-hardening`
   - Bug/Error -> `debugging-and-error-recovery`
   - Test -> `test-driven-development`
   - Final review -> `code-review-and-quality` -> `requesting-code-review` -> `receiving-code-review` -> `verification-before-completion`
   - High Risk Decision -> `adversarial-agent-review`

# 6. Decision points
- Có thư viện/framework nào cần kiểm tra (verify) version trong Official Documentation không?
- Khi nhiều skill cùng áp dụng, xác định thứ tự thực hiện theo Skill Precedence.

# 7. Red flags
- Bỏ qua quy trình bắt buộc bằng lý do "Task quá đơn giản".
- Tự ý đoán mò API hoặc phiên bản framework thay vì kiểm tra thực tế/Official Documentation.
- Tạo implementation mới khi implementation tương tự đã tồn tại.

# 8. Verification
- Mọi quyết định đều dựa trên evidence (có bằng chứng file/source/log cụ thể).
- Các rule cứng (project-constraints, engineering-guardrails) không bị vi phạm.

# 9. Exit criteria
- Có danh sách các skill sẽ dùng cho task hiện tại và tiến hành công việc dựa trên evidence thực tế, không đoán mò.
