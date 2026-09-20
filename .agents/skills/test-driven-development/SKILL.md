---
name: test-driven-development
description: Viết test để xác minh chức năng, đảm bảo Test Quality. Sử dụng khi thêm logic Backend hoặc sửa bug.
---

# 1. Purpose
Đảm bảo mã nguồn hoạt động đúng kỳ vọng, chống lỗi hồi quy (regression) và chứng minh một bug đã thực sự được giải quyết thông qua Test Quality.

# 2. When to use
- Sửa bug (cần Prove-it pattern).
- Thêm Service/Controller/Repository mới ở Spring Boot.

# 3. When NOT to use
- Đổi mã màu CSS/HTML thuần (Frontend không yêu cầu tự động hóa unit test giao diện).
- Khi TDD không khả thi (trường hợp này phải ghi rõ lý do và dùng cách Verification khác).

# 4. Inputs / Preconditions
- Môi trường chạy Unit Test (JUnit, Mockito) đã được cấu hình trong dự án.

# 5. Core workflow (Red-Green-Refactor)
1. **RED**:
   - Viết failing test trước (để tái hiện bug hoặc định hình feature mới).
   - Chạy test và XÁC NHẬN nó fail đúng như mong đợi.
2. **GREEN**:
   - Viết minimum code (code tối thiểu) để test pass.
   - Chạy lại test và XÁC NHẬN nó pass (màu xanh).
3. **REFACTOR**:
   - Cải thiện code (nếu cần thiết).
   - Chạy lại test để đảm bảo code không bị vỡ.

# 6. Decision points
- Test logic thuần túy (Unit Test - dùng Mockito) hay test tương tác cơ sở dữ liệu/API (Integration/Slice Test - dùng `@DataJpaTest` hoặc `MockMvc`)?
- Nếu không thể dùng TDD, phương pháp xác minh thay thế là gì?

# 7. Red flags (Anti-patterns)
- Viết implementation trước rồi mới lấp liếm viết test vô nghĩa sau.
- Tự huyễn hoặc (Assume) test sẽ fail trước khi implement mà không thèm chạy thử test lúc nó màu đỏ.
- Viết test bị vướng các anti-patterns như Mirror Assertion, Change Detectors.
- Dùng API đã deprecated của Spring Boot (ví dụ: cần chú ý `@MockBean` vs `@MockitoBean` trong Spring Boot 3.4+).

# 8. Verification & Test Evidence Transparency
- Chạy các công cụ/terminal test (ví dụ `mvn test` hoặc test cụ thể).
- Terminal output hiển thị `BUILD SUCCESS`.
- **Minh bạch bằng chứng kiểm thử**: Phải phân biệt rõ giữa "EXECUTED NOW" (vừa chạy trong phiên làm việc hiện tại) và "VERIFIED FROM EXISTING ARTIFACT". Nếu phiên làm việc không thực thi lại toàn bộ Maven suite (ví dụ task chỉ sửa tài liệu), bắt buộc ghi rõ: *"Existing project test artifact reports 1101/1101 tests PASS; test suite was not re-executed in this session"*, tuyệt đối không nhận vơ kết quả cũ là vừa chạy mới.

# 9. Exit criteria
- Khi có sửa mã nguồn: Đã chạy RED -> GREEN -> REFACTOR thành công. Tất cả bài test (mới và cũ) đều pass, code pass các rule về chất lượng.

# 10. References to load conditionally
- Khi gặp khó khăn trong việc viết test (dùng mock, helper) hoặc test không bắt được lỗi: Đọc `references/testing-anti-patterns.md`.

<!-- Adapted from obra/superpowers (MIT License) -->
