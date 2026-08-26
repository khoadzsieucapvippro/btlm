---
name: verification-before-completion
description: Bắt buộc xác minh (verify) bằng chứng thực tế trước khi báo cáo hoàn thành. Không báo DONE nếu chưa chạy code/test.
---

# 1. Purpose
Đảm bảo mọi task, bug fix, hay tính năng mới đều được xác minh thực tế bằng bằng chứng rõ ràng (run code, run tests, UI check) trước khi agent báo cáo "Hoàn thành". Khắc phục tình trạng "đoán là code chạy được".

# 2. When to use
- Ngay trước khi định nói "Done", "Fixed", "Completed", "It works" cho người dùng.
- Trở thành chốt chặn cuối cùng (Final Check) của mọi workflow.

# 3. When NOT to use
- Đang trong quá trình nghiên cứu, viết kế hoạch, hoặc chỉ review code tĩnh.

# 4. Inputs / Preconditions
- Đã thực hiện code implementation hoặc cấu hình xong.
- Nắm rõ Yêu cầu ban đầu (Original requirements) hoặc Tiêu chí chấp nhận (Acceptance Criteria).

# 5. Core workflow
1. **Identify what proves the claim**: Xác định điều kiện nào chứng minh code hoạt động.
2. **Run the relevant verification**: Thực thi command, test suite, build process hoặc truy cập UI.
3. **Inspect complete result**: Đọc toàn bộ output/log (không chỉ vài dòng đầu).
4. **Check exit code/errors/failures**: Xác nhận không có lỗi ẩn.
5. **Compare result with original requirements**: So sánh kết quả thực tế với spec ban đầu.
6. **Only then state the actual status**: Báo cáo kết quả cuối cùng cho user.

# 6. Decision points
- Có phương tiện nào để chạy verification không? (Nếu không có môi trường chạy, BẮT BUỘC báo cáo: "Implemented: YES. Verified: NO. Not verified because: ...")

# 7. Red flags
- Báo cáo hoàn thành chỉ vì "Lint passed" hoặc "Build passed" (Code build được không có nghĩa là chạy đúng logic).
- Dùng báo cáo của agent thay cho bằng chứng độc lập (tự huyễn hoặc "tôi viết đúng nên nó đúng").
- Claim completion without fresh verification evidence.

# 8. Verification
- Evidence phải fresh (được chạy SAU thay đổi code cuối cùng).
- Ghi rõ "Đã chạy lệnh XYZ và nhận kết quả ABC".

**Bảng Tiêu chuẩn Xác minh (Common Failures to avoid):**
| Claim (Báo cáo) | Requires (Yêu cầu bắt buộc) | Not Sufficient (Không đủ/Ngụy biện) |
| --- | --- | --- |
| Lỗi đã được sửa (Bug fixed) | Chạy lại đúng repro steps ban đầu và hết lỗi | Đã đổi code và "nghĩ là" nó đã fix |
| Pass test (Tests pass) | Output test `0 failures` | Test pass từ lần chạy trước |
| API hoạt động đúng | Response trả về đúng HTTP 200 và DTO format | Spring Boot khởi động không báo lỗi |
| Tính năng đáp ứng spec | Checklist requirement được tick từng dòng | Chỉ dựa vào test tự động sơ sài |

# 9. Exit criteria
- Cung cấp bằng chứng xác minh thành công. Nếu không thể xác minh, phải khai báo rõ phần nào chưa xác minh (Unverified areas) và rủi ro còn lại (Remaining risk).

<!-- Adapted from obra/superpowers (MIT License) -->
