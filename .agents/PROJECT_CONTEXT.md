# PROJECT CONTEXT — HỆ THỐNG HỌC BỘ THỦ VÀ TỪ VỰNG TIẾNG TRUNG

> **Trạng thái dự án:**  
> - **Backend Phase 0–8 & Module 8D:** `COMPLETED` (100% năng lực Backend và Quản trị hoàn tất).  
> - **Backend Hardening & Remediation (Tasks R1 → R3.11):** `COMPLETED` (R1 New Card Engine, R1.1 Concurrency, R2 Due-Only Review Policy, R2.1 Approved Lesson Eligibility, R2.1A Moderation Race, R3.1 Vocabulary Lifecycle, R3.1A Concurrency Hardening, R3.2 Lesson Lifecycle & Moderation History, R3.3/R3.3A Chinese Domain & Pinyin Normalization, R3.4 Object-Level Auth Audit, R3.5 Notes Pagination, R3.6 Rate Limiter Cache Eviction, R3.7 Apache POI 5.4.0 CVE, R3.8 Radical Pinyin V7, R3.9 Database Index Performance EXPLAIN, R3.10 Production HTTP & Reverse Proxy Security Headers, R3.11 Backend Final Quality Gate & Pre-Frontend Release Seal `CLOSED / VERIFIED`).  
> - **Backend Audit Status:** `SEALED & APPROVED FOR PHASE 9 FRONTEND INTEGRATION (DEC-41)` (100% ranh giới bảo mật, RBAC, JWT, Transactions, Concurrency, DTO isolation, Schema V1..V7 verified).  
> - **Bằng chứng kiểm thử:** **`1101/1101 tests PASS, Failures: 0, Errors: 0, Skipped: 0`** (`BUILD SUCCESS` trên Testcontainers MySQL 8.4, thời gian: ~04:38 min).  
> - **Nhiệm vụ tiếp theo:** **Task 9A.1 — Frontend Foundation, Core Shell, Design System & Browser-Safe UI** (Phase 9 Frontend bắt đầu).  
> - **Nguồn chân lý hiện trạng thực tế:** [`CURRENT_STATE.md`](CURRENT_STATE.md).  
> - **Cập nhật lần cuối:** 2026-09-09  

---

## 1. TỔNG QUAN HỆ THỐNG & MỤC TIÊU DỰ ÁN

Xây dựng nền tảng Web học tiếng Trung bài bản từ 214 Bộ thủ Khang Hy chuẩn, từ vựng theo bài học, câu ví dụ, hỗ trợ nhập liệu bài học từ file Excel (quy trình 2 bước: Preview & Confirm), và cỗ máy ôn tập ghi nhớ dài hạn qua thuật toán lặp lại ngắt quãng (**Spaced Repetition System - SRS / SM-2**).

Hệ thống quản lý phân quyền theo mô hình **Role-Based Access Control (RBAC)** với 4 vai trò chính thức trong bảng `ROLE`:
1. `Learner` (Vai trò `1`): Học bộ thủ, từ vựng, bài học công khai (`Approved`), ôn tập Flashcard SRS, tạo ghi chú cá nhân ($\le 500$ ký tự), tùy chỉnh hạn mức học SRS mỗi ngày.
2. `Creator` (Vai trò `2`): Quyền hạn Learner + tạo bài học cá nhân (`Draft`), upload file Excel import từ vựng (xem trước preview không ghi DB $\rightarrow$ xác nhận confirm nguyên tử), gửi bài học chờ duyệt (`Pending`), chỉnh sửa bài học bị từ chối (`Rejected`).
3. `Moderator` (Vai trò `3`): Xem hàng đợi bài học `Pending`, xem chi tiết bài học và từ vựng đính kèm, phê duyệt (`Approved`) để xuất bản công khai, từ chối (`Rejected`) kèm bắt buộc nhập `rejection_reason` và `flagged_fields` JSON; lưu vết bất biến vào `MODERATION_LOG`.
4. `Admin` (Vai trò `4`): Toàn quyền quản trị hệ thống, quản lý tài khoản (`Active`, `Inactive`, `Banned`), phân quyền vai trò, CRUD 214 Bộ thủ và Từ vựng danh mục gốc, giám sát kiểm toán.

---

## 2. KIẾN TRÚC & STACK CÔNG NGHỆ KHÓA CỨNG

* **Backend:** Java 21 LTS (OpenJDK 21.0.12), Spring Boot 3.3.5, Spring MVC, Spring Security 6, Spring Data JPA, Hibernate 6.5.3, JJWT 0.12.6, Apache POI 5.4.0 (`poi-ooxml` vá CVE-2025-31672), Apache Maven 3.9.16.
* **Backend Architecture:** `Client/Frontend -> RESTful API -> Controller -> Service -> Repository -> MySQL`.
* **Database & Migration:** MySQL Community Server 8.4 LTS, Flyway Migration (`V1`..`V7` versioned migrations), Testcontainers MySQL 8.4.0 cho kiểm thử tự động.
* **Frontend:** HTML5 ngữ nghĩa, CSS3 design tokens (`style.css`), Bootstrap 5.3 CDN có SRI, Vanilla JavaScript ES6+ modular (`frontend/js/`). Cấu trúc module 4 tầng canonical (`api/`, `auth/`, `ui/`, `pages/`), zero-build complexity, tuyệt đối không dùng framework SPA (React, Vue, Angular, Next.js, Nuxt).
* **API Standard:** RESTful API, JSON payload, tiền tố `/api/v1/...`, phong bì phản hồi thống nhất `ApiResponse<T>` và `PageResponse<T>`.

---

## 3. THẨM QUYỀN TÀI LIỆU & HƯỚNG DẪN DÀNH CHO AI AGENT MỚI

Khi làm việc trên repository này, AI Coding Agent **BẮT BUỘC** tham chiếu các tài liệu chuyên biệt theo thứ tự ưu tiên:

1. **Hợp đồng Bất biến & Ranh giới Dự án:** [`PROJECT-CONTRACT.md`](PROJECT-CONTRACT.md) (Nguồn chân lý cao nhất cho các bất biến kiến trúc, hợp đồng API, RBAC 4 tầng, và quy trình xử lý yêu cầu thay đổi).
2. **Hiện trạng thực tế & Bằng chứng xác minh:** [`CURRENT_STATE.md`](CURRENT_STATE.md).
3. **Hợp đồng REST API chuẩn:** [`API.md`](API.md) (Toàn bộ 48 Java handlers / 49 HTTP routes đã được đối chiếu 1:1 với Backend).
4. **Tiến độ chi tiết từng Task/Module:** [`PROGRESS.md`](PROGRESS.md).
5. **Quyết định kỹ thuật đã phê duyệt:** [`DECISIONS.md`](DECISIONS.md) (`DEC-01`..`DEC-42`, `DES-01`..`DES-07`).
6. **Đặc tả CSDL 14 bảng & Thiết kế vật lý:** [`DATABASE.md`](DATABASE.md) và [`DATABASE_DESIGN.md`](DATABASE_DESIGN.md).
7. **Kiến trúc hệ thống:** [`ARCHITECTURE.md`](ARCHITECTURE.md).
8. **Lộ trình kế hoạch tương lai:** [`ROADMAP.md`](ROADMAP.md).
9. **Lịch sử khắc phục kiểm toán Backend:** [`BACKEND_REMEDIATION.md`](BACKEND_REMEDIATION.md) và [`AUDIT_REPORT.md`](AUDIT_REPORT.md).
10. **Quy tắc & Ràng buộc:** [`rules/engineering-guardrails.md`](rules/engineering-guardrails.md) và [`rules/project-constraints.md`](rules/project-constraints.md).
11. **Hướng dẫn vận hành & Kiểm thử:** [`RUNBOOK.md`](RUNBOOK.md).

---

## 4. KỶ LUẬT THỰC THI DÀNH CHO AGENT

* **Kỷ luật một task duy nhất:** Mỗi phiên làm việc chỉ tập trung giải quyết đúng 1 Task được giao; không tự ý nhảy cóc hoặc làm trước các task tương lai.
* **Bằng chứng xác thực (Evidence-First):** Tuyệt đối không báo `COMPLETED` nếu chưa chạy kiểm thử thực tế và có output thành công.
* **Bảo vệ toàn vẹn kiến trúc:** DTO isolation 100% (zero entity leak), không nhận `user_id`/`creator_id` từ client body, không sửa migrations Flyway cũ.
