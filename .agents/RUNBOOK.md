# PROJECT RUNBOOK

> **Mục đích**
>
> File này là hướng dẫn vận hành project.
>
> Khi quay lại project sau một thời gian và không nhớ:
>
> * chạy project thế nào;
> * chạy database thế nào;
> * test backend thế nào;
> * test API bằng Postman thế nào;
> * lấy JWT thế nào;
> * kiểm tra lỗi thế nào;
> * kiểm tra project đang ở phase/task nào;
>
> thì đọc file này trước.

---

# 1. PROJECT QUICK INFO

## Project

Website học tiếng Trung, bao gồm:

```text
214 Bộ thủ Khang Hy
Vocabulary
Lesson
Flashcard
SRS
Personal Notes
Creator
Moderator
Admin
```

## Current technology stack

### Backend

```text
Java 21 LTS
Spring Boot 3.3.5
Spring MVC
Spring Security
Spring Data JPA
Hibernate 6
JWT
Maven
```

### Database

```text
MySQL 8.4 LTS
Database: elearning_db
Flyway
utf8mb4
```

### Frontend

```text
HTML
CSS
Vanilla JavaScript
```

---

# 2. PROJECT STRUCTURE

Project structure quan trọng:

```text
project-root/
│
├── .agents/
│   ├── PROJECT_CONTEXT.md
│   ├── ARCHITECTURE.md
│   ├── DATABASE.md
│   ├── DATABASE_DESIGN.md
│   ├── API.md
│   ├── DECISIONS.md
│   ├── ROADMAP.md
│   ├── PROGRESS.md
│   ├── CURRENT_STATE.md
│   └── RUNBOOK.md
│
├── backend/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   │
│   └── target/
│
└── frontend/
```

---

# 3. FIRST THING WHEN RETURNING TO PROJECT

Khi quay lại project sau một thời gian, làm theo thứ tự:

```text
1. Mở project trong VS Code
        ↓
2. Đọc .agents/CURRENT_STATE.md
        ↓
3. Đọc .agents/PROGRESS.md
        ↓
4. Xác định Current Phase
        ↓
5. Xác định Current Task
        ↓
6. Chạy test baseline
        ↓
7. Chỉ sau đó mới tiếp tục implementation
```

## Không làm ngay

Không:

```text
mở project
→ thấy code
→ sửa đại
→ chạy
```

Phải biết project đang ở đâu trước.

---

# 4. CURRENT PROJECT STATUS

Hiện trạng dự án:

```text
Phase 0 = COMPLETED
Phase 1 = COMPLETED
Phase 2 = COMPLETED
Phase 3 = COMPLETED
Phase 4 = COMPLETED
Phase 5 = COMPLETED (Modules 5A, 5B, 5C, 5D COMPLETED; Checkpoint Phase 5 VERIFIED)
Phase 6 = COMPLETED (Modules 6A, 6B, 6C COMPLETED; Checkpoint Phase 6 VERIFIED)
Phase 7 = COMPLETED (Modules 7A, 7B, 7C COMPLETED; Checkpoint Phase 7 VERIFIED)
Phase 8 = COMPLETED (Modules 8A, 8B, 8C COMPLETED; Checkpoints 8A, 8B, Checkpoint Phase 8 VERIFIED)
Backend Hardening Gate = COMPLETED (15/15 audit remediation items CLOSED / VERIFIED)
Module 8D = COMPLETED (8/8 functional completion tasks 8D.1..8D.8 CLOSED / VERIFIED)
Backend Remediation & Hardening Tasks = COMPLETED (R1 → R3.11 VERIFIED)
Backend Release Status = SEALED & APPROVED FOR PHASE 9 FRONTEND INTEGRATION (DEC-41)
Phase 9 = UNBLOCKED / READY TO COMMENCE (Module 9A Task 9A.1 CURRENT NEXT TASK)
```

Regression suite hiện tại đã đạt:

```text
PASS 1101/1101 tests
Failures: 0
Errors: 0
Skipped: 0
Build: SUCCESS (~04:38 min trên Testcontainers MySQL 8.4)
```

Nhiệm vụ vừa hoàn thành:

```text
Final Adversarial Backend Remediation & Security Hardening (DEC-42) / Task R3.11 Sealed (DEC-41) (COMPLETED / VERIFIED)
```

Nhiệm vụ kế tiếp duy nhất:

```text
Task 9A.1 — Khởi tạo khung giao diện, layout dùng chung & hệ thống CSS (Phase 9 / Module 9A)
```

Sau khi quay lại project, luôn kiểm tra:

```text
.agents/PROGRESS.md
.agents/CURRENT_STATE.md
```

để xác nhận trạng thái mới nhất.

---

# 5. REQUIREMENTS BEFORE RUNNING

Kiểm tra các công cụ.

## Java

Mở terminal:

```bash
java -version
```

Project yêu cầu:

```text
Java 21
```

Nếu không phải Java 21:

> Không tiếp tục debug application trước khi sửa Java environment.

---

## Maven

Kiểm tra:

```bash
mvn -version
```

Project sử dụng Maven.

Nếu project có Maven Wrapper thì trên Windows có thể dùng:

```bat
mvnw.cmd -version
```

Maven Wrapper giúp project chạy với Maven version được cấu hình bởi project thay vì phụ thuộc hoàn toàn vào Maven cài global.

---

## MySQL

Kiểm tra MySQL đang chạy.

Có thể dùng:

```bash
mysql --version
```

Project database:

```text
MySQL 8.4 LTS
```

Database:

```text
elearning_db
```

---

# 6. OPEN PROJECT IN VS CODE

Mở:

```text
project-root
```

Không chỉ mở:

```text
backend/
```

nếu bạn cần làm việc với toàn bộ:

```text
.agents/
backend/
frontend/
```

Project root là context đầy đủ của project.

---

# 7. DATABASE STARTUP & VERIFICATION

Trước khi chạy backend, MySQL Server phải đang chạy và database `elearning_db` phải sẵn sàng:

```text
MySQL Server 8.4 (Port 3306)
         ↓
Database elearning_db sẵn sàng
         ↓
Backend startup & Flyway auto-migrate (V1..V7)
```

### Bước 7.1: Kiểm tra MySQL Server đang chạy

Trên Windows PowerShell:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqladmin.exe" -u root ping
```

* Nếu xuất hiện: `mysqld is alive` $\rightarrow$ MySQL đã sẵn sàng, chuyển sang Bước 7.3.
* Nếu báo lỗi kết nối (`connect to localhost failed`): tiến hành Bước 7.2 để khởi động MySQL.

### Bước 7.2: Khởi động MySQL Server Daemon (nếu chưa chạy)

Khởi động MySQL daemon với file cấu hình chuẩn của project (`my.ini`):

```powershell
Start-Process -FilePath "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" -ArgumentList '--defaults-file="C:\ProgramData\MySQL\MySQL Server 8.4\my.ini"'
```

Sau đó kiểm tra lại bằng lệnh ping:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqladmin.exe" -u root ping
```

### Bước 7.3: Kiểm tra Database `elearning_db`

Đăng nhập hoặc kiểm tra danh sách database bằng client MySQL:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -e "SHOW DATABASES;"
```

Xác nhận có cơ sở dữ liệu `elearning_db`.

---

# 8. CONFIGURATION & MANDATORY SECRETS (`application.yml`)

Nguồn chân lý cấu hình runtime duy nhất của Backend:
```text
backend/src/main/resources/application.yml
```

### 8.1. Tham số Bí mật BẮT BUỘC (`JWT_SECRET`)

> [!CAUTION]
> **Ràng buộc Bảo mật Khóa Cứng (Security Enforcement):**  
> `application.yml` định nghĩa `jwt.secret: ${JWT_SECRET}` và **tuyệt đối không để giá trị fallback mặc định** nhằm chống rủi ro rò rỉ secret lên Git.  
> Do đó, **BẮT BUỘC PHẢI CUNG CẤP `JWT_SECRET`** khi khởi động backend. Nếu thiếu, Spring Boot sẽ dừng ngay lập tức với lỗi:  
> `IllegalArgumentException: Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}"`.

* **Yêu cầu kỹ thuật:** Khóa bí mật phải có độ dài tối thiểu 256 bits (ít nhất 32 ký tự ASCII) để thuật toán mã hóa `HS256` của JJWT hoạt động an toàn.
* **Giá trị mẫu cho môi trường Local / Development:**
  ```text
  elearning-chinese-platform-secret-key-jwt-256-bits-minimum-length-key!
  ```

### 8.2. Cấu hình Kết nối CSDL & Cổng Mặc định

| Tham số | Biến môi trường | Giá trị mặc định trong `application.yml` | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Port** | `server.port` | `8080` | Cổng HTTP REST API |
| **Reverse Proxy** | `server.forward-headers-strategy` | `framework` | Xử lý `X-Forwarded-For` (Task R3.10) |
| **DB URL** | `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/elearning_db?...` | MySQL 8.4 Community |
| **DB Username** | `SPRING_DATASOURCE_USERNAME` | `root` | Tài khoản CSDL local |
| **DB Password** | `SPRING_DATASOURCE_PASSWORD` | `""` (chuỗi rỗng) | Mặc định không mật khẩu |
| **Flyway** | `spring.flyway.enabled` | `true` | Tự động migrate V1..V7 khi khởi động |

---

# 9. RUN BACKEND — DEVELOPMENT MODE (QUY TRÌNH CHẠY LẦN LƯỢT)

Để khởi động backend hoàn chỉnh từ đầu, thực hiện tuần tự theo 4 bước:

```text
BƯỚC 1: Xác nhận MySQL đang chạy trên port 3306
   ↓
BƯỚC 2: Chuẩn bị khóa bí mật JWT_SECRET
   ↓
BƯỚC 3: Thực thi lệnh chạy Spring Boot Maven
   ↓
BƯỚC 4: Xác minh Health Check và API Endpoint
```

### Cách 1: Chạy trực tiếp truyền tham số qua Maven Argument (KHUYẾN NGHỊ TRÊN WINDOWS)

Từ thư mục gốc dự án (`project-root`):

```powershell
mvn -f backend/pom.xml spring-boot:run "-Dspring-boot.run.arguments=--jwt.secret=elearning-chinese-platform-secret-key-jwt-256-bits-minimum-length-key!"
```

Hoặc di chuyển vào thư mục `backend/`:

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.arguments=--jwt.secret=elearning-chinese-platform-secret-key-jwt-256-bits-minimum-length-key!"
```

> [!NOTE]
> Trên Windows PowerShell, bắt buộc phải bao bọc `"-Dspring-boot.run.arguments=..."` trong cặp dấu ngoặc kép `""` để tránh PowerShell ngắt chuỗi command-line arguments.

---

### Cách 2: Thiết lập biến môi trường trong Terminal Session

**Trên Windows PowerShell:**
```powershell
$env:JWT_SECRET="elearning-chinese-platform-secret-key-jwt-256-bits-minimum-length-key!"
mvn -f backend/pom.xml spring-boot:run
```

**Trên Windows Command Prompt (cmd):**
```cmd
set JWT_SECRET=elearning-chinese-platform-secret-key-jwt-256-bits-minimum-length-key!
mvn -f backend/pom.xml spring-boot:run
```

**Trên Linux / macOS (Bash / Zsh):**
```bash
export JWT_SECRET="elearning-chinese-platform-secret-key-jwt-256-bits-minimum-length-key!"
mvn -f backend/pom.xml spring-boot:run
```

---

# 10. BACKEND START SUCCESSFULLY & VERIFICATION

### 10.1. Dấu hiệu Khởi động Thành công trong Console Log

Khi Spring Boot khởi động hoàn tất, log console sẽ xuất hiện các dòng thông báo then chốt:

```text
... o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
... com.elearning.ElearningApplication       : Started ElearningApplication in X.XX seconds
```

Đồng thời, Flyway sẽ tự động kiểm tra và thực thi các migration tăng dần (`V1`..`V7`) vào CSDL `elearning_db` mà không cần can thiệp thủ công.

### 10.2. Lệnh Kiểm tra Nhanh (Smoke Test / Health Check)

Mở một terminal PowerShell khác và chạy các lệnh kiểm thử sau:

1. **Kiểm tra Health Probe (Spring Boot Actuator):**
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
   ```
   *Kết quả mong đợi:*
   ```json
   {"status": "UP"}
   ```

2. **Kiểm tra API Danh mục 214 Bộ thủ (Public Endpoint):**
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:8080/api/v1/radicals" -Method Get
   ```
   *Kết quả mong đợi:* Phản hồi chuẩn phong bì `ApiResponse` chứa danh sách 214 bộ thủ Khang Hy (`totalElements: 214`, `code: "SUCCESS"`).

---

# 11. STOP BACKEND & PORT MANAGEMENT

### 11.1. Dừng thông thường
Trong terminal đang chạy Spring Boot:
* Nhấn tổ hợp phím **`Ctrl + C`** và chờ process Tomcat giải phóng tài nguyên.

### 11.2. Giải phóng cổng 8080 nếu bị chiếm dụng (Port Conflict)
Nếu gặp lỗi `Port 8080 was already in use`, kiểm tra và kết thúc tiến trình đang chiếm port trên Windows PowerShell:

```powershell
# 1. Tìm Process ID (PID) đang chiếm port 8080
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object LocalAddress, LocalPort, OwningProcess, State

# 2. Dừng tiến trình chiếm dụng
Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess -Force
```

---

# 12. BUILD PROJECT

Build backend:

```bash
mvn -f backend/pom.xml clean package
```

Hoặc:

```bash
cd backend
mvn clean package
```

Mục đích:

```text
clean
↓
xóa build artifacts cũ
↓
compile
↓
test
↓
package
```

---

# 13. RUN ALL TESTS

Đây là lệnh quan trọng nhất trước và sau implementation:

```bash
mvn -f backend/pom.xml clean test
```

Hoặc:

```bash
cd backend
mvn clean test
```

Chỉ coi regression pass khi output thực tế có:

```text
BUILD SUCCESS
Failures: 0
Errors: 0
```

Không nói:

> “chắc là test pass”

nếu chưa chạy.

---

# 14. RUN ONE SPECIFIC TEST

Ví dụ:

```bash
mvn -f backend/pom.xml test -Dtest=ActualTestClassName
```

Ví dụ minh họa:

```bash
mvn -f backend/pom.xml test -Dtest=RadicalServiceImplTest
```

Nhưng:

> Chỉ dùng đúng test class thực tế trong source tree.

Không copy tên ví dụ nếu class không tồn tại.

---

# 15. BASELINE BEFORE IMPLEMENTATION

Trước khi AI coding agent hoặc developer sửa code:

```bash
mvn -f backend/pom.xml clean test
```

Ghi lại:

```text
Tests run
Failures
Errors
BUILD SUCCESS/FAILURE
```

Ví dụ:

```text
Baseline:

Tests: 187
Failures: 0
Errors: 0
Status: PASS
```

Sau implementation:

```bash
mvn -f backend/pom.xml clean test
```

So sánh:

```text
Before
↓
Implementation
↓
After
```

Mục tiêu:

```text
No regression
```

---

# 16. HOW TO RUN A PACKAGED APPLICATION (.JAR)

### Bước 16.1: Đóng gói ứng dụng thành file JAR thực thi
Từ thư mục gốc dự án:

```powershell
mvn -f backend/pom.xml clean package -DskipTests
```

File JAR thực thi được sinh ra tại:
```text
backend/target/elearning-backend-1.0.0.jar
```

### Bước 16.2: Khởi chạy file JAR độc lập
Truyền `jwt.secret` qua thuộc tính hệ thống Java (`-Djwt.secret=...`):

```powershell
java -Djwt.secret="elearning-chinese-platform-secret-key-jwt-256-bits-minimum-length-key!" -jar backend/target/elearning-backend-1.0.0.jar
```

Hoặc thiết lập biến môi trường trước khi chạy:

```powershell
$env:JWT_SECRET="elearning-chinese-platform-secret-key-jwt-256-bits-minimum-length-key!"
java -jar backend/target/elearning-backend-1.0.0.jar
```

---

# 17. FLYWAY STARTUP BEHAVIOR

Project dùng:

```text
Flyway
```

Schema database được quản lý bằng migration.

Khi application startup:

```text
Spring Boot
↓
Database connection
↓
Flyway checks migration history
↓
Apply missing migrations if required
↓
Application startup
```

Không:

```text
Hibernate ddl-auto=update
```

Không sửa database thủ công để thay thế migration.

Nếu Flyway báo lỗi:

```text
Migration checksum mismatch
Migration validation failed
Schema history problem
```

Dừng lại và kiểm tra:

```text
backend/src/main/resources/db/migration/
flyway_schema_history
```

Không xóa `flyway_schema_history` bừa.

---

# 18. CHECK FLYWAY DATABASE HISTORY

Trong MySQL:

```sql
USE elearning_db;

SELECT *
FROM flyway_schema_history
ORDER BY installed_rank;
```

Kiểm tra:

```text
version
description
script
success
```

Nếu migration thất bại:

> Không tự sửa/xóa history table trước khi hiểu nguyên nhân.

---

# 19. DATABASE QUICK CHECK

Kiểm tra số radical:

```sql
SELECT COUNT(*)
FROM RADICAL;
```

Expected:

```text
214
```

Nếu không phải 214:

> kiểm tra Flyway migration/seed data trước khi sửa application code.

---

# 20. CHECK IMPORTANT DATA

## Roles

```sql
SELECT *
FROM ROLE;
```

Expected project roles:

```text
LEARNER
CREATOR
MODERATOR
ADMIN
```

Tên chính xác phải đối chiếu với data thực tế.

---

## Accounts

```sql
SELECT *
FROM ACCOUNT;
```

Không chia sẻ output có:

```text
password_hash
JWT
secret
credential
```

trong report/chat công khai.

---

# 21. API TESTING OVERVIEW

Hệ thống cung cấp danh mục API hoàn chỉnh gồm:
- **15 Controller classes** (`com.elearning.controller`)
- **48 Java handler methods**
- **49 HTTP method+path mappings** (do `/{id}/reorder` hỗ trợ cả PUT và POST)
- **1 Actuator health probe** (`GET /actuator/health`)

Chi tiết toàn bộ đặc tả API xem tại `.agents/API.md`.  
Bộ sưu tập tự động hóa **Postman Collection** đầy đủ bao phủ 100% 49 HTTP mappings (kèm assertion scripts) được định nghĩa và bàn giao chính thức tại **Task 11D.1** (`docs/postman_collection.json`).

Luồng kiểm thử API thủ công:

```text
Backend running
        ↓
Create/Register account
        ↓
Login
        ↓
Receive JWT
        ↓
Use JWT
        ↓
Call protected endpoint
        ↓
Verify HTTP status
        ↓
Verify ApiResponse
        ↓
Verify database if needed
```

Có thể dùng:

```text
Postman
```

hoặc:

```text
curl
```

---

# 22. BASE URL

Mặc định nếu project đang chạy port 8080:

```text
http://localhost:8080
```

API prefix:

```text
/api/v1
```

Ví dụ:

```text
http://localhost:8080/api/v1/...
```

Nếu port/context path đã thay đổi trong configuration:

> configuration thực tế thắng file hướng dẫn này.

---

# 23. API TEST — REGISTER

Endpoint chính xác phải đối chiếu với:

```text
.agents/API.md
```

Current authentication flow đã có Register/Login.

Trong Postman:

```text
Method: POST
URL: http://localhost:8080/api/v1/...
```

Chọn:

```text
Body
→ raw
→ JSON
```

Request body phải dùng đúng contract trong `.agents/API.md`.

Không tự thêm fields.

Sau khi gửi request, kiểm tra:

```text
HTTP status
ApiResponse structure
data
error code nếu failure
```

---

# 24. API TEST — LOGIN

Login endpoint phải đối chiếu exact path trong:

```text
.agents/API.md
```

Current authentication implementation trả JWT khi login thành công.

Expected flow:

```text
POST login
↓
200 OK
↓
ApiResponse
↓
JWT token
```

Response token phải lấy từ field thực tế mà Auth API đang trả.

Không giả định mọi version đều dùng:

```text
accessToken
```

hoặc:

```text
token
```

Nếu không chắc:

> mở `.agents/API.md` hoặc inspect `AuthController/AuthService/AuthResponse`.

---

# 25. SAVE JWT IN POSTMAN

Sau khi login thành công:

```text
copy JWT
```

Với protected API:

```text
Authorization
→ Type: Bearer Token
→ Paste JWT
```

HTTP header tương đương:

```http
Authorization: Bearer <JWT>
```

Không gửi:

```text
BearerBearer
Bearer "token"
JWT token
```

Header phải theo exact Bearer token format.

---

# 26. TEST CURRENT USER PROFILE API

Phase 3 đã hoàn thành current user profile vertical slice.

Endpoints:

```text
GET /api/v1/users/profile

PUT /api/v1/users/profile
```

## GET profile

```http
GET http://localhost:8080/api/v1/users/profile
Authorization: Bearer <JWT>
```

Expected:

```text
200 OK
```

Response phải theo:

```text
ApiResponse<UserProfileResponse>
```

---

## GET profile without JWT

Không gửi Authorization header.

Expected:

```text
401 Unauthorized
```

Không phải:

```text
200
403
500
```

trừ khi project security contract sau này được thay đổi có chủ đích.

---

# 27. TEST UPDATE PROFILE

```http
PUT http://localhost:8080/api/v1/users/profile
Authorization: Bearer <JWT>
Content-Type: application/json
```

Body phải theo DTO contract thực tế:

```text
UpdateProfileRequest
```

Các fields hiện cần kiểm tra trong implementation/source-of-truth.

Không gửi:

```json
{
  "userId": 123
}
```

để chọn profile owner.

Owner phải đến từ:

```text
JWT
↓
SecurityContext
↓
Current authenticated user
```

---

# 28. PROFILE SECURITY TEST

Tạo:

```text
User A
User B
```

Login bằng User A.

Lấy JWT của User A.

Gọi:

```text
GET /api/v1/users/profile
```

Expected:

```text
Profile A
```

Không có cơ chế:

```text
?userId=<B>
```

hoặc:

```text
/users/profile/<B>
```

cho phép User A đọc profile User B.

---

# 29. API TEST WITH CURL

Ví dụ format GET protected endpoint:

```bash
curl -X GET "http://localhost:8080/api/v1/users/profile" ^
  -H "Authorization: Bearer <JWT>"
```

Trên PowerShell/Linux/macOS có thể khác cách xuống dòng.

Nếu chỉ cần một dòng:

```bash
curl -X GET "http://localhost:8080/api/v1/users/profile" -H "Authorization: Bearer <JWT>"
```

---

# 30. TEST API CHECKLIST

Mỗi endpoint mới nên test tối thiểu:

```text
[ ] Valid request
[ ] Invalid request
[ ] Missing required field
[ ] Unauthorized request nếu endpoint protected
[ ] Forbidden request nếu endpoint role-protected
[ ] Resource not found
[ ] Validation error
[ ] Response structure
[ ] Database state nếu endpoint thay đổi dữ liệu
```

Không cần mọi endpoint đều có mọi case.

Chỉ test những case phù hợp với contract.

---

# 31. HOW TO CHECK API RESPONSE

Luôn kiểm tra:

```text
1. HTTP status
2. Response body
3. ApiResponse structure
4. data fields
5. error code
6. message
```

Không chỉ nhìn:

```text
Postman xanh
```

Ví dụ request có thể trả:

```text
200
```

nhưng data mapping sai.

Do đó phải kiểm tra body.

---

# 32. WHEN API RETURNS 401

Kiểm tra theo thứ tự:

```text
1. Có Authorization header không?
2. Header có đúng:
   Bearer <JWT>
3. JWT có bị expired không?
4. JWT có bị copy thiếu không?
5. JwtAuthenticationFilter có chạy không?
6. SecurityConfig endpoint có protected không?
```

Không sửa Service trước.

401 thường là security/authentication problem.

---

# 33. WHEN API RETURNS 403

Kiểm tra:

```text
1. User đã authenticated chưa?
2. User có role cần thiết không?
3. SecurityConfig có role restriction gì?
4. JWT chứa authority/role đúng không?
5. Role mapping có đúng không?
```

Phân biệt:

```text
401
→ chưa xác thực / JWT không hợp lệ

403
→ đã xác thực nhưng không có quyền
```

---

# 34. WHEN API RETURNS 400

Kiểm tra:

```text
@RequestBody
@Valid
DTO fields
@NotBlank
@NotNull
@Size
JSON syntax
Content-Type
```

Đọc response body từ:

```text
GlobalExceptionHandler
```

Không sửa Controller trước khi biết validation nào fail.

---

# 35. WHEN API RETURNS 404

Kiểm tra:

```text
URL
HTTP method
@RequestMapping
@GetMapping
database resource exists?
```

Phân biệt:

```text
Endpoint not found
```

và:

```text
Resource not found
```

Hai lỗi này có nguyên nhân khác nhau.

---

# 36. WHEN API RETURNS 500

Không đoán.

Làm:

```text
1. Đọc terminal log
2. Tìm exception đầu tiên/root cause
3. Kiểm tra stack trace
4. Xác định layer gây lỗi
```

Phân loại:

```text
Controller
Service
Repository
Hibernate
Database
Security
Serialization
Validation
```

Không chỉ nhìn dòng cuối cùng.

---

# 37. COMMON DATABASE ERRORS

## Cannot connect to database

Kiểm tra:

```text
MySQL running?
host?
port?
database name?
username?
password?
```

---

## Table does not exist

Kiểm tra:

```text
Flyway migration
database name
schema
flyway_schema_history
```

---

## Data does not exist

Ví dụ:

```text
Radical expected but not found
```

Kiểm tra:

```sql
SELECT COUNT(*)
FROM RADICAL;
```

Expected:

```text
214
```

---

# 38. COMMON PORT ERROR

Nếu thấy:

```text
Port 8080 was already in use
```

Trên Windows:

```bat
netstat -ano | findstr :8080
```

Sau đó xác định PID.

Có thể kiểm tra process:

```bat
tasklist | findstr <PID>
```

Chỉ kill process sau khi xác định đúng process.

Không kill ngẫu nhiên process khác.

---

# 39. HOW TO DEBUG CODE

Khi backend đang lỗi:

```text
Request
↓
Controller
↓
Service
↓
Repository
↓
Database
```

Debug theo flow.

Không mở toàn bộ project và sửa ngẫu nhiên.

Ví dụ API trả dữ liệu sai:

```text
1. Check request
2. Check Controller
3. Check Service method
4. Check Repository result
5. Check Entity
6. Check database row
```

---

# 40. HOW TO CHECK DATABASE AFTER UPDATE

Nếu API update data:

```text
API request
↓
HTTP response
↓
Database query
↓
Confirm persistence
```

Ví dụ generic:

```sql
SELECT *
FROM <TABLE>
WHERE <condition>;
```

Không chỉ tin response JSON.

Đặc biệt với:

```text
Profile update
Admin CRUD
Vocabulary update
Lesson update
Notes
SRS progress
```

phải kiểm tra persistence khi cần.

---

# 41. HOW TO USE GIT BEFORE STARTING WORK

Trước khi sửa:

```bash
git status
```

Kiểm tra:

```text
modified files
untracked files
current branch
```

Nếu working tree đã có thay đổi:

> xác định đó là thay đổi cũ hay thay đổi cần giữ trước khi AI agent sửa tiếp.

Không để agent overwrite thay đổi chưa commit mà không biết.

---

# 42. GIT QUICK COMMANDS

## Check status

```bash
git status
```

## Check current branch

```bash
git branch --show-current
```

## Check changes

```bash
git diff
```

## Check recent commits

```bash
git log --oneline -10
```

---

# 43. BEFORE GIVING TASK TO AI CODING AGENT

Trước mỗi task:

```text
1. Check git status
2. Run baseline tests
3. Read CURRENT_STATE.md
4. Read PROGRESS.md
5. Confirm exact current task
6. Prepare implementation prompt
```

Sau đó mới gửi prompt.

---

# 44. AFTER AI CODING AGENT FINISHES

Không tin ngay câu:

```text
DONE
```

Phải kiểm tra:

```text
1. Agent changed which files?
2. git diff
3. Targeted tests
4. Full regression
5. API test nếu có endpoint
6. Database verification nếu data changes
7. Check scope creep
8. Check .agents progress update
```

---

# 45. REQUIRED POST-IMPLEMENTATION FLOW

```text
AI implementation
        ↓
Inspect changed files
        ↓
Run targeted tests
        ↓
Run full regression
        ↓
Run application
        ↓
Manual API test if applicable
        ↓
Database verification if applicable
        ↓
Review git diff
        ↓
Update PROGRESS/CURRENT_STATE
        ↓
Stop
```

---

# 46. HOW TO REVIEW GIT DIFF

Run:

```bash
git diff
```

Check:

```text
unexpected files
unnecessary dependencies
database migration changes
frontend changes
unrelated refactoring
deleted existing code
scope creep
```

Nếu task chỉ là:

```text
Service
```

nhưng diff có:

```text
SecurityConfig
Flyway migration
Frontend
JWT
```

thì phải hỏi:

> Tại sao task này lại thay đổi những phần đó?

---

# 47. AI CODING AGENT — SAFE OPERATING RULE

Mỗi prompt implementation phải yêu cầu:

```text
Read source-of-truth
↓
Inspect actual source
↓
Inspect current implementation
↓
Identify contract
↓
Research official docs if needed
↓
Implement only assigned scope
↓
Write tests
↓
Run tests
↓
Review diff
↓
Update progress
↓
STOP
```

Không:

```text
Read task
→ immediately write code
```

---

# 48. SOURCE OF TRUTH PRIORITY

Khi có mâu thuẫn:

```text
1. Latest explicit project decision
2. Actual completed implementation with test evidence
3. PROGRESS.md
4. CURRENT_STATE.md
5. ROADMAP.md
6. API.md
7. Other historical documentation
```

Nhưng luôn kiểm tra context cụ thể.

Không tự động tin một đoạn documentation cũ nếu nó mâu thuẫn với:

```text
actual code
test evidence
latest progress update
```

---

# 49. BEFORE STARTING A NEW PHASE

Checklist:

```text
[ ] Previous phase completed
[ ] Full regression passed
[ ] PROGRESS.md updated
[ ] CURRENT_STATE.md updated
[ ] Current next task identified
[ ] No unresolved failure
[ ] No accidental schema change
[ ] Git working tree understood
```

---

# 50. TASK 5D.1 PREPARATION CHECKLIST

Trước khi bắt đầu:

```text
Task 5D.1 — Lesson Lifecycle Integration Verification & Checkpoint Phase 5
```

làm:

```text
[ ] Read .agents/ROADMAP.md
[ ] Read .agents/PROGRESS.md
[ ] Read .agents/CURRENT_STATE.md
[ ] Read .agents/API.md (Mục 2.4 & 2.5: Phân hệ Lesson và Import)
[ ] Inspect CreatorLessonService & CreatorLessonController (Module 5B & 5C)
[ ] Inspect ExcelParserService (Module 5C)
[ ] Run baseline:
    mvn -f backend/pom.xml clean test (467/467 tests PASS)
```

Sau đó mới gửi implementation prompt cho AI coding agent.

---

# 51. FAST DAILY WORKFLOW

Nếu chỉ quay lại tiếp tục code:

```text
Open project
↓
git status
↓
Read CURRENT_STATE.md
↓
Read PROGRESS.md
↓
Start MySQL
↓
Run backend
↓
Run relevant test
↓
Continue current task
```

---

# 52. FULL DEVELOPMENT WORKFLOW

```text
START
│
├── Open project
│
├── git status
│
├── Read .agents/CURRENT_STATE.md
│
├── Read .agents/PROGRESS.md
│
├── Identify exact task
│
├── Start MySQL
│
├── Check database
│
├── Run baseline tests
│
├── Create/review AI implementation prompt
│
├── AI implements task
│
├── Inspect git diff
│
├── Run targeted tests
│
├── Run full regression
│
├── Start backend
│
├── Test API manually if applicable
│
├── Verify database if applicable
│
├── Update PROGRESS.md
│
├── Update CURRENT_STATE.md
│
└── STOP
```

---

# 53. QUICK COMMAND CHEAT SHEET

## Check Java

```bash
java -version
```

## Check Maven

```bash
mvn -version
```

## Run backend

```bash
mvn -f backend/pom.xml spring-boot:run
```

## Run all tests

```bash
mvn -f backend/pom.xml clean test
```

## Run one test

```bash
mvn -f backend/pom.xml test -Dtest=<ActualTestClassName>
```

## Build package

```bash
mvn -f backend/pom.xml clean package
```

## Run packaged JAR

```bash
java -jar backend/target/<actual-file-name>.jar
```

## Git status

```bash
git status
```

## Git diff

```bash
git diff
```

## Recent commits

```bash
git log --oneline -10
```

## MySQL login

```bash
mysql -u root -p
```

## Check tables

```sql
SHOW TABLES;
```

## Check Flyway history

```sql
SELECT *
FROM flyway_schema_history
ORDER BY installed_rank;
```

## Check Radical count

```sql
SELECT COUNT(*)
FROM RADICAL;
```

Expected:

```text
214
```

---

# 54. FINAL RULE

Khi không biết chuyện gì đang xảy ra:

**Không đoán.**

Đi theo flow:

```text
CURRENT_STATE
↓
PROGRESS
↓
Actual source
↓
Test result
↓
Log
↓
Database
↓
Git diff
```

Thứ tự xử lý lỗi:

```text
Evidence
before
Assumption
```

Và trước khi nói một task hoàn thành:

```text
Implementation
+
Tests
+
Regression
+
Relevant manual verification
+
Evidence
=
COMPLETED
```
