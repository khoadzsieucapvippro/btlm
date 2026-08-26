# WORKFLOW — QUY TRÌNH PHÁT TRIỂN VÀ ĐIỀU PHỐI AGENT SKILLS

> **Mục tiêu:** Định hình quy trình chuẩn mực từ tiếp nhận yêu cầu, phân tích tác động, triển khai, đến xác minh và cập nhật tiến độ cho lần xây mới từ đầu.  
> **Nguyên tắc:** Skill-First, dựa trên bằng chứng (Evidence-based), chỉ kích hoạt các kỹ năng chuyên biệt liên quan đến phạm vi tác động (Minimal Necessary Skills).  

---

## 1. QUY TRÌNH 8 BƯỚC CHUẨN (STANDARD DEVELOPMENT LIFECYCLE)

```
1. UNDERSTAND       — Tiếp nhận và làm rõ yêu cầu của User
         │
         ▼
2. SPECIFY          — Đối chiếu Đặc tả kỹ thuật (Database, API Contract, Business Invariants)
         │
         ▼
3. PLAN             — Phân rã công việc thành các bước triển khai nhỏ (Impact-based planning)
         │
         ▼
4. IMPLEMENT        — Viết mã nguồn tăng dần (Incremental Implementation, No Over-engineering)
         │
         ▼
5. VALIDATE         — Kiểm thử thực tế (Maven build, Unit/MockMvc tests, DevTools console)
         │
         ▼
6. REVIEW           — Đối chiếu Actual Diff với quy chuẩn chất lượng và guardrails
         │
         ▼
7. DOCUMENT         — Cập nhật tài liệu kiến trúc, API contract, hoặc quyết định kỹ thuật mới
         │
         ▼
8. UPDATE PROGRESS  — Cập nhật trạng thái trong PROGRESS.md với bằng chứng nghiệm thu cụ thể
```

---

## 2. MA TRẬN ĐIỀU PHỐI KỸ NĂNG (SKILL ROUTING MATRIX)

Căn cứ vào kỹ năng điều phối `using-agent-skills`, Agent chỉ kích hoạt các skill tương ứng với bản chất của từng công việc:

| Loại công việc | Phạm vi tác động | Kỹ năng chính cần nạp | Kỹ năng phụ trợ |
| :--- | :--- | :--- | :--- |
| **Phát triển Backend logic** | Service, Repository, Entity, Transaction | `spring-boot-backend` | `test-driven-development` |
| **Thiết kế & sửa đổi API** | Controller, DTO, Request/Response | `api-and-interface-design` | `spring-boot-backend` |
| **Thay đổi Cơ sở dữ liệu** | MySQL Schema, Flyway migration, JPA mapping | `database-flyway-mysql` | `spring-boot-backend` |
| **Xác thực & Bảo mật** | JWT, Security Filter, Login, RBAC | `spring-security-jwt` | `security-and-hardening` |
| **Xây dựng Giao diện UI** | HTML, CSS, DOM Events, UI state | `frontend-ui-engineering` | `browser-testing-with-devtools` |
| **Tích hợp Frontend với API** | `api.js`, Fetch, JWT header, Form submit | `frontend-api-integration` | `frontend-ui-engineering` |
| **Điều tra & Sửa lỗi (Bug)** | Điều tra nguyên nhân gốc (Root cause) | `debugging-and-error-recovery` | Skill công nghệ liên quan |
| **Tính năng lớn / Phức tạp** | Thay đổi nhiều tầng, có quyết định kiến trúc | `spec-driven-development`<br>`planning-and-task-breakdown` | `incremental-implementation`<br>`adversarial-agent-review` |
| **Nghiệm thu hoàn thành** | Trước khi báo cáo hoàn thành task | `verification-before-completion` | `code-review-and-quality` |

---

## 3. NGUYÊN TẮC KỸ THUẬT BẮT BUỘC (GUARDRAILS & DISCIPLINE)

1. **Không đoán mò (No Guessing):** Mọi kết luận về lỗi, kiến trúc hay trạng thái code phải dựa trên file đọc thực tế hoặc log chạy lệnh thực tế.
2. **Không kích hoạt workflow nặng cho task nhỏ:** 
   - Task đơn giản (chỉnh sửa một class nhỏ, fix typo, format CSS): Sử dụng trực tiếp kỹ năng công nghệ liên quan, không cần qua bước debate hoặc viết spec dài dòng.
   - Task lớn (khởi tạo project, thiết kế migration, phân quyền bảo mật): Bắt buộc lập kế hoạch và phân tích tác động trước khi code.
3. **Bằng chứng xác minh trước khi báo cáo (Evidence before DONE):**
   - Chỉ được đánh dấu `COMPLETED` khi có kết quả chạy thực tế: lệnh Maven build pass, test pass, hoặc API test trả về status mong đợi.
   - Nếu không thể chạy do hạn chế môi trường (ví dụ chưa có MySQL server): Phải ghi rõ `RUNTIME = NOT VERIFIED` kèm lý do cụ thể.
4. **Bảo vệ hợp đồng giao tiếp (Contract Preservation):** Tuyệt đối không thay đổi schema database hoặc response format của API mà không cập nhật tài liệu và kiểm tra phía Frontend tương ứng.
