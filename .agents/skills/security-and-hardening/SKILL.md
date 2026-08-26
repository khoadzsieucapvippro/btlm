---
name: security-and-hardening
description: Kỹ năng bảo mật, phân quyền, xử lý validation và gia cố ứng dụng chống lỗ hổng. (SQLi, XSS, Path Traversal, File Upload, Auth Bypass).
---

# 1. Purpose
Ngăn chặn các lỗ hổng bảo mật tại tầng Application (OWASP Top 10) thông qua cách Code Defensive (Phòng thủ).

# 2. When to use
- Viết code gọi xuống DB bằng parameter người dùng nhập.
- Làm tính năng Upload File, Avatar, Audio.
- Render chuỗi từ DB ra màn hình HTML.
- Phân quyền (Role-based access).

# 3. When NOT to use
- Khi chỉ chỉnh sửa giao diện tĩnh không chứa dữ liệu động.

# 4. Inputs / Preconditions
- Đã nắm rõ luồng dữ liệu (Data flow) từ User -> Controller -> DB.

# 5. Core workflow
1. **Trace the Data**: Đầu vào đến từ đâu? (Header, Body, URL, Upload). Đầu ra đi về đâu? (DB, HTML, File System).
2. **Consult Threat Matrix**: Mở `references/threat-matrix.md` đối chiếu loại dữ liệu với rủi ro bảo mật.
3. **Defense at Edge**: Validate JSR-380 (`@NotNull`, `@Size`, Regex) ngay tại DTO.
4. **Defense in Depth**: Dùng Parameterized query, Hash mật khẩu.
5. **Output Defense**: Encode lúc render để chống XSS.

# 6. Decision points
- Có cần lưu File không? -> Phải validate Extension, Content-Type, chặn Path Traversal.
- Dữ liệu trả ra FE có render dưới dạng Rich Text (HTML) không? -> Phải dùng thư viện Sanitize (như DOMPurify bên FE hoặc bóc tách bên BE).

# 7. Red flags
- Mật khẩu lưu dạng plain-text (Không dùng BCrypt).
- Nối chuỗi SQL (`"SELECT * FROM Users WHERE name = '" + name + "'"`).
- Khuyên "sanitize tất cả input" một cách mù quáng thay vì validate định dạng.

# 8. Verification
- Evidence: Thử truyền chuỗi `' OR 1=1 --` vào API xem có chọc thủng được logic không.
- Evidence: Thử truyền chuỗi `<script>alert(1)</script>` xem HTML có popup không.

# 9. Exit criteria
- Đã bịt các Payload tấn công thử nghiệm (Trả về 400 Bad Request hoặc chuỗi vô hại).

# 10. References to load conditionally
- Cần biết cách phòng chống 5 loại tấn công nguy hiểm nhất? Đọc `references/threat-matrix.md`.
