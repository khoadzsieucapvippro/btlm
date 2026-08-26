# Context-Specific Threat Matrix & Defense

Không dùng lời khuyên chung chung "Sanitize input". Hãy phòng thủ theo Context:

## 1. SQL Injection (SQLi)
- **Threat**: Kẻ gian nhập mã SQL vào ô input (VD: `' OR 1=1 --`).
- **Attack Surface**: Các Repository tự nối chuỗi (Native Query).
- **Prevention**: 100% sử dụng JPA Method Names (`findByEmail`), hoặc `@Query` kèm Parameter Binding (`?1` hoặc `:email`).
- **Verification**: Gửi ký tự `'` hoặc `"` vào API, nếu DB ném lỗi SQL Syntax Error -> Có lỗ hổng.

## 2. Cross-Site Scripting (XSS)
- **Threat**: Kẻ gian nhập mã độc `<script>` hoặc `<img onerror="...">` vào bình luận/thông tin cá nhân.
- **Attack Surface**: Render chuỗi từ DB trực tiếp vào DOM bằng `innerHTML` hoặc React `dangerouslySetInnerHTML`.
- **Prevention**: 
  - Tại BE: Chỉ Validate, KHÔNG encode HTML (Để lưu nguyên bản).
  - Tại FE: Dùng `textContent` hoặc DOM element mapping. Nếu buộc dùng `innerHTML`, phải qua thư viện Sanitizer (vd: DOMPurify).

## 3. Insecure File Upload & Path Traversal
- **Threat**: Upload file `shell.php` hoặc tải lên file name `../../../windows/system32/cmd.exe`.
- **Attack Surface**: Tính năng thay avatar, upload audio/ảnh bài học.
- **Prevention**:
  - Không giữ nguyên tên file của user. Phải rename bằng `UUID`.
  - Check File Extension (Whitelist: `.jpg`, `.mp3`) và Content-Type.
  - Lưu file ra ổ cứng thì hàm resolve Path tuyệt đối không được cộng chuỗi trực tiếp từ user input.

## 4. Insecure Direct Object Reference (IDOR)
- **Threat**: User A có id=1, truyền lên URL `GET /api/users/2` để xem thông tin của User B.
- **Attack Surface**: Endpoint nhận ID.
- **Prevention**: Bất cứ lúc nào lấy data theo ID, phải kiểm tra ID đó có thuộc quyền sở hữu của `SecurityContextHolder.getContext().getAuthentication().getName()` hay không (Nếu không phải ADMIN).

## 5. Secret Management
- **Threat**: Lộ mật khẩu Database, JWT Secret trên Github.
- **Prevention**:
  - Các biến nhạy cảm để dạng placeholder `${JWT_SECRET}` trong `application.yml`.
  - Load từ Environment Variables.
  - KHÔNG BAO GIỜ hardcode secret trong file Java (`private String secret = "123456";`).
