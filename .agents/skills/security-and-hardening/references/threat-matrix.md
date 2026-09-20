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
- **Threat**: Upload file độc hại (`shell.php`, ZIP bomb, XML bomb, zip-slip path traversal).
- **Attack Surface trong dự án**: Tính năng nhập bài học từ Excel hai bước (`POST /api/v1/creator/lessons/import` & `/confirm`). *(Lưu ý: Avatar và Media trong dự án chỉ lưu dưới dạng URL string, không có API upload ảnh/audio lên server).*
- **Phòng vệ thực tế trong Backend (`ExcelParserServiceImpl.java`)**:
  - **Zero Disk Persistence**: File `.xlsx` được xử lý hoàn toàn qua `InputStream` trong bộ nhớ (try-with-resources), tuyệt đối không lưu ra file system của hệ điều hành.
  - **Định dạng & Magic Bytes**: Kiểm tra đuôi `.xlsx` và kiểm tra 4 magic bytes đầu tiên (`PK\x03\x04` - Zip header) trước khi đưa vào Apache POI.
  - **Giới hạn dung lượng cứng (DoS Guard)**: Tối đa 10MB (`MAX_FILE_SIZE_BYTES`), vượt quá trả về HTTP 413 `FILE_TOO_LARGE`.
  - **Giới hạn số dòng dữ liệu**: Tối đa 5.000 dòng (`MAX_DATA_ROWS = 5000` theo OWASP API4:2023), vượt quá trả về `ROW_LIMIT_EXCEEDED`.
  - **Thư viện an toàn**: Sử dụng Apache POI 5.4.0 vá triệt để lỗ hổng duplicate ZIP entry / OOXML decompression bomb (CVE-2025-31672).

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
