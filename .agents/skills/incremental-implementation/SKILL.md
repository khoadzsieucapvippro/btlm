---
name: incremental-implementation
description: Triển khai mã nguồn tăng dần dựa trên phân tích tác động (Impact-Based Workflow). Tránh code hàng loạt không kiểm soát.
---

# 1. Purpose
Triển khai mã nguồn tăng dần dựa trên phân tích tác động (Impact-Based Workflow) để kiểm soát rủi ro, hoàn thiện từng lát cắt (vertical slice) và tránh code hàng loạt không kiểm soát.

# 2. When to use
- Khi bắt đầu thực thi code một tính năng mới hoặc sửa đổi chạm đến nhiều file/layer.

# 3. When NOT to use
- Khi sửa lỗi nhỏ (hotfix) chỉ nằm ở một file/layer cụ thể không gây tác động lan truyền.

# 4. Inputs / Preconditions
- Đã có thiết kế hoặc danh sách các thay đổi cần thiết (impact analysis).

# 5. Core workflow
Tuyệt đối không ép mọi task vào chuỗi DB -> BE -> FE. Tuân thủ luồng phân tích tác động:
1. **Analyze impact**.
2. **Database change?**
   - YES -> Chạy Flyway workflow.
   - NO -> Bỏ qua Database workflow.
3. **Backend change?**
   - YES -> Implement Backend layer + relevant tests.
4. **API contract affected?**
   - YES -> Cập nhật API Contract + Đánh giá tác động lên Frontend.
5. **Frontend affected?**
   - YES -> Implement Centralized API integration + UI rendering.
6. **Verify** affected layers.
7. **Review**.

# 6. Decision points
- Nếu Backend chạy lỗi (build fail, test fail) -> Sửa ngay lập tức, KHÔNG viết tiếp Frontend.

# 7. Red flags
- Code hàng loạt nhiều chức năng/layer mà không kiểm tra (verify) từng bước.
- Chuyển sang tính năng khác khi lớp hiện tại còn lỗi.

# 8. Verification
- Ứng dụng không bị vỡ/crash. Lát cắt hiện tại hoạt động End-to-End từ lớp thấp nhất bị ảnh hưởng lên đến đỉnh.
- Luôn kiểm tra log Spring Boot trước khi code tiếp Frontend JS.

# 9. Exit criteria
- Hoàn thiện toàn bộ lát cắt này không lỗi lầm rồi mới chuyển sang lát cắt tiếp theo (Vertical Slice).
