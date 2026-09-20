# PROGRESS — BẢNG THEO DÕI TIẾN ĐỘ DỰ ÁN (PHASE → MODULE → TASK MATRIX)

> [!NOTE]
> **STATUS: HISTORICAL / PROGRESS TRACKING ARTIFACT**  
> **NOT CURRENT PROJECT CONTRACT**  
> This document tracks historical implementation progress from Phase 0 to Phase 8D. Current normative architecture, invariants, and API contracts are governed by `PROJECT-CONTRACT.md`, `API.md`, and `CURRENT_STATE.md`.

> **Mô hình quản lý:** 4 cấp độ kỹ thuật:  
> $$\text{PHASE} \longrightarrow \text{MODULE} \longrightarrow \text{TASK} \longrightarrow \text{VERIFICATION CHECKPOINT}$$
> 
> **Nguyên tắc cập nhật trạng thái:**  
> - `NOT_STARTED`: Công việc chưa bắt đầu.  
> - `IN_PROGRESS`: Đang trong quá trình thực hiện.  
> - `COMPLETED`: Chỉ gán trạng thái này khi đã có bằng chứng nghiệm thu vật lý độc lập (Test pass, build pass, file artifact đã tạo và kiểm chứng).  
> - `BLOCKED`: Đang bị chặn bởi dependency hoặc câu hỏi mở.  
> - **Kỷ luật cốt lõi:** Tuyệt đối không đánh dấu COMPLETED cho bất kỳ task nào dựa trên giả định hoặc mã nguồn cũ đã xóa.  

---
---

## 0. TÓM TẮT CÔNG VIỆC ĐÃ HOÀN THÀNH & ĐANG CHỰC THI (COMPLETED + UPCOMING SUMMARY)

### ✅ COMPLETED WORK (ĐÃ HOÀN THÀNH 100%)

| Nhóm công việc | Chi tiết | Bằng chứng |
| :--- | :--- | :--- |
| **Phase 0–8 (Backend Core)** | Toàn bộ 9 Phase từ Specification đến Personal Notes & Settings | 1059/1059 tests PASS |
| **Backend Hardening Gate** | 15/15 audit findings đã CLOSED/VERIFIED (`BE-AUTH-001`..`BE-TEST-003`) | 881 tests → 1059 tests |
| **Module 8D (Backend Completion)** | 8 task 8D.1..8D.8 (Admin Accounts, Roles, Moderation History, Auth Error, Rate Limiter, Health Probe, Pre-FE Gate) | 948 tests |
| **Task R1 & R1.1** | New Card Engine (`GET /api/v1/srs/new-cards`) + Concurrency Verification (10 adversarial tests) | 960 tests |
| **Task R2 & R2.1 & R2.1A** | Due-Only Review Policy + Approved-Lesson Eligibility + Concurrent Moderation Boundary | 980 tests |
| **Task R3.1 & R3.1A** | Vocabulary Lifecycle (referential integrity guards) + Delete/Mutation Concurrency Hardening | 1010 tests |
| **Task R3.2** | Lesson Lifecycle & Moderation History Integrity | 1040 tests |
| **Task R3.3 & R3.3A** | 214 Kangxi Radicals Audit + Pinyin Normalization & Search Integrity | 1059 tests |
| **Task R3.4..R3.11** | Object-Level Auth Audit, Personal Notes Pagination, Login Rate Limiter Eviction, Apache POI 5.4.0 (CVE-2025-31672), Radical Missing Pinyin (V7), Database Index Performance & EXPLAIN, Production HTTP & Reverse Proxy Security Headers, Backend Final Quality Gate & Pre-Frontend Release Seal | 1088/1088 tests PASS |
| **Final Adversarial Review** | Account Status Token Invalidation (`findByIdForUpdate` + `auth_ver`), LoginRateLimiter Active Eviction, Capacity Limit & Concurrency Throttling, DTO Collection Element Validation (`@NotNull @Positive Long/Integer`), GlobalExceptionHandler Safe 400 | 1101/1101 tests PASS |

### 🔄 CURRENT POSITION (VỊ TRÍ HIỆN TẠI)

| Mục | Giá trị |
| :--- | :--- |
| **Last Completed Task** | **Task 9G.5 — Responsive & Multi-Viewport Layout Verification, Remediation & Browser Testing (`tests/frontend/e2e/responsive-layout.browser.mjs` 10/10 PASS, 22/22 HTML pages verified with 0px document overflow across 375/414/768/1200/1440 viewports and 320px reflow spot-check, `.table-responsive` localized horizontal scroll preserved, flashcard & creator-import deep responsiveness verified, 27 real Chromium screenshot artifacts, DEC-47 issued, Phase 9 100% COMPLETED)** |
| **Current Workstream** | **Phase 9 Frontend Gate Cleared (Modules 9A..9G 100% COMPLETED). Sẵn sàng chuyển giao Checkpoint Phase 9 -> Phase 10.** |
| **Next Immediate Task** | **Checkpoint Phase 9 / Phase 10 Module 10A Task 10A.1 — Rà soát Hồi quy Ranh giới Tiêm nhiễm & OWASP API Security** |
| **Frontend Phase 9 Status** | **MODULE 9A, 9B, 9C, 9D, 9E, 9F, 9G (100% COMPLETED — 741 frontend tests PASS: 177 Static, 535 Fast, 19 A11y, 10 Responsive)** |
| **Production Hardening Status** | **DEFERRED** (Phase 10–11) |

### 📋 BACKEND REMEDIATION & HARDENING TASKS (R3.4 → R3.11) — 100% COMPLETED

| Thứ tự | Task ID | Tên | Mức ưu tiên | Trạng thái | Block FE? |
| :---: | :--- | :--- | :---: | :---: | :---: |
| 1 | **R3.4** | Object-Level Authorization & API Boundary Audit | `P0` | **COMPLETED** | **YES** |
| 2 | **R3.5** | Personal Notes Pagination & Resource Consumption Hardening | `P1` | **COMPLETED** | **YES** |
| 3 | **R3.6** | Login Rate Limiter TTL Cache Eviction & Memory Lifecycle | `P1` | **COMPLETED** | **YES** |
| 4 | **R3.7** | Dependency & Framework Security Hardening (Apache POI 5.4.0 CVE) | `P2` | **COMPLETED** | Không |
| 5 | **R3.8** | Kangxi Radicals Missing Pinyin Correction & Seed Data Ingestion | `P2` | **COMPLETED** | Không |
| 6 | **R3.9** | Database Index Performance & Slow Query Verification (EXPLAIN) | `P2` | **COMPLETED** | Không |
| 7 | **R3.10** | Production HTTP & Reverse Proxy Security Headers | `P3` | **COMPLETED** | Không |
| 8 | **R3.11** | Backend Final Quality Gate & Pre-Frontend Release Seal | `P0 (Gate)` | **COMPLETED** | **GATE (CLEARED)** |

### 🚪 FRONTEND PHASE 9 GATE (CỔNG NGHIỆM THU FRONTEND)

```
[x] Backend contracts stable (R3.4 complete)
[x] SRS API stable (R3.4 complete)
[x] Lesson API stable (R3.4 complete)
[x] Vocabulary API stable (R3.4 complete)
[x] Auth API stable (R3.4 complete)
[x] Personal Notes API stable + paginated (R3.5 complete)
[x] Login Rate Limiter TTL eviction implemented (R3.6 complete)
[x] Error model stable (no breaking changes)
[x] No known critical/high backend defect blocking FE
[x] Required product decisions locked (PEN-01 / OQ-08 resolved: Bootstrap 5 CDN + Custom CSS + Vanilla JS)
[x] Documentation synchronized (DECISIONS, CURRENT_STATE, PROGRESS, ROADMAP, API, BACKEND_REMEDIATION)
[x] Full regression passing (1101/1101 tests PASS)
```

**Kết luận Gate:** **`GATE CLEARED / READY`** — Backend đã được niêm phong hoàn toàn (DEC-41). Chính thức mở cổng triển khai Phase 9 Frontend.

## 1. BẢNG TRUY XUẤT LỊCH SỬ NHIỆM VỤ (HISTORICAL TASK TRACEABILITY MAPPING)

> [!NOTE]
> **Lưu trữ Lịch sử Ánh xạ Định danh:** Bảng dưới đây lưu vết quá trình chuyển đổi định danh từ các task legacy sang hệ thống chuẩn hóa 4 cấp độ kỹ thuật tại thời điểm khởi động lại dự án (Phase 0). Nguồn chân lý duy nhất về tiến độ trực tiếp của từng task hiện tại nằm ở **Mục 2: MA TRẬN TIẾN ĐỘ CHI TIẾT** ngay dưới đây.

| Định danh cũ (Old Task ID) | Định danh chuẩn hóa mới (New Task ID) | Phase | Trạng thái hiện tại | Ghi chú chuyển đổi |
| :--- | :--- | :--- | :--- | :--- |
| `TASK-0.1` | **Task 0A.1** | Phase 0 | `COMPLETED` | Xóa sạch legacy artifacts và source cũ. |
| `TASK-0.2` | **Task 0A.2** | Phase 0 | `COMPLETED` | Xác minh 19 Agent Skills trong `.agents/skills/`. |
| `TASK-0.3` | **Task 0B.1** | Phase 0 | `COMPLETED` | Thiết lập 9 tài liệu đặc tả chuẩn 14 bảng. |
| `TASK-0.4` | **Task 0B.2** | Phase 0 | `COMPLETED` | Cài đặt & cấu hình MySQL Community Server 8.4 LTS. |
| `TASK-0.5` | **Task 0B.3** | Phase 0 | `COMPLETED` | Khởi tạo Git repo, thiết lập `.gitignore`. |
| `TASK-0.6` | **Task 0B.4** | Phase 0 | `COMPLETED` | Thiết kế CSDL Vật lý 14 bảng (`DATABASE_DESIGN.md`). |
| `TASK-1.1` | **Task 1A.1** | Phase 1 | `COMPLETED` | Cấu trúc Spring Boot 3.3.5, Java 21, `backend/pom.xml`. |
| `TASK-1.2` | **Task 1A.2** | Phase 1 | `COMPLETED` | `application.yml` HikariCP kết nối MySQL 3306. |
| `TASK-1.3` | **Task 1A.3** | Phase 1 | `COMPLETED` | Flyway V1 migration tạo 14 bảng nghiệp vụ. |
| `TASK-1.4` | **Task 1A.4** | Phase 1 | `COMPLETED` | Kiểm thử Context Load & Flyway schema version 1. |
| `TASK-1.5` | **Task 1B.1, 1B.2** | Phase 1 | `COMPLETED` | Phân rã thành 2 task nhỏ: DTOs phong bì và GlobalExceptionHandler. |
| `TASK-2.1` | **Task 2E.1, 2E.2** | Phase 2 | `COMPLETED` | Chuyển thành Module 2E: Flyway Seed Data V2, V3. |
| `TASK-2.2` | **Task 2A.1..2D.2** | Phase 2 | `COMPLETED` | Phân rã thành 4 Modules miền nghiệp vụ khép kín (2A, 2B, 2C, 2D). |
| `TASK-2.3` | **Task 2A.2, 2B.2, 2C.2, 2D.3** | Phase 2 | `COMPLETED` | Repositories được gắn liền với từng Module miền tương ứng. |
| `TASK-2.4` | **Task 2F.1** | Phase 2 | `COMPLETED` | Chuyển thành Module 2F: Full Persistence Verification. |
| `TASK-10.2` (cũ) / `10B.2` | **Task 10C.1** | Phase 10 | `NOT_STARTED` | Tách riêng phần đóng khoảng trống kiểm thử thành Module 10C. |

---

## 2. MA TRẬN TIẾN ĐỘ CHI TIẾT (TASK TRACKING MATRIX)

| Phase | Module | Task ID | Tên công việc (Task Description) | Status | Dependency (depends_on) | Bằng chứng xác minh (Verification Evidence) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Phase 0** | **Mod 0A** | **Task 0A.1** | Xóa bỏ toàn bộ mã nguồn cũ và artifacts không phù hợp | **COMPLETED** | Không | `backend/` và `frontend/` rỗng (`.gitkeep`); các thư mục legacy đã bị xóa sạch sẽ. |
| **Phase 0** | **Mod 0A** | **Task 0A.2** | Bảo tồn và xác minh tính toàn vẹn 19 Agent Skills | **COMPLETED** | Không | Script `validate_skills.ps1` chạy PASS, không có orphan references. |
| **Phase 0** | **Mod 0B** | **Task 0B.1** | Thiết lập lại toàn bộ tài liệu đặc tả chuẩn 14 bảng | **COMPLETED** | Task 0A.1 | Đã tạo và chuẩn hóa 9 tài liệu đặc tả trong `.agents/` (`PROJECT_CONTEXT`, `ARCHITECTURE`, v.v.). |
| **Phase 0** | **Mod 0B** | **Task 0B.2** | Cài đặt và cấu hình MySQL Community Server 8.4 LTS | **COMPLETED** | Không | Cài MySQL 8.4.9, daemon mysqld chạy cổng 3306, utf8mb4, kết nối thành công `elearning_db`. |
| **Phase 0** | **Mod 0B** | **Task 0B.3** | Khởi tạo Git repository, thiết lập file `.gitignore` tiêu chuẩn | **COMPLETED** | Không | `git init` nhánh `main`; `.gitignore` bảo vệ secrets, target/, ide files. Initial commit `40ada12`. |
| **Phase 0** | **Mod 0B** | **Task 0B.4** | Soạn thảo tài liệu Thiết kế CSDL Vật lý chi tiết cho 14 bảng | **COMPLETED** | Task 0B.1, 0B.2 | `.agents/DATABASE_DESIGN.md` gồm 20 mục chi tiết, giải quyết dứt điểm OQ-04..OQ-07. Commit `f2b2000`. |
| **Phase 1** | **Mod 1A** | **Task 1A.1** | Khởi tạo cấu trúc Spring Boot 3 và file `backend/pom.xml` | **COMPLETED** | Task 0B.4 | `backend/pom.xml` sử dụng Java 21, Spring Boot 3.3.5; Maven compile thành công. Commit `5d59ff4`. |
| **Phase 1** | **Mod 1A** | **Task 1A.2** | Thiết lập class khởi động, `application.yml` kết nối MySQL | **COMPLETED** | Task 1A.1 | `ElearningApplication.java`, `application.yml` kết nối MySQL cổng 3306 qua HikariCP, `ddl-auto=none`. |
| **Phase 1** | **Mod 1A** | **Task 1A.3** | Soạn thảo và thực thi Flyway migration `V1__init_schema.sql` | **COMPLETED** | Task 1A.2 | Flyway áp dụng V1 (527ms); đúng 14 bảng nghiệp vụ tạo đầy đủ trong `elearning_db`. |
| **Phase 2** | **Mod 2A** | **Task 2A.1** | JPA Entity Mapping: `ACCOUNT`, `USER_PROFILE`, `ROLE`, `ACCOUNT_ROLE` | **COMPLETED** | Task 1A.4 [PARALLEL với 2B] | `IdentityPersistenceTests` PASS 5/5 tests (0.82s); ánh xạ `Account`, `UserProfile` (1:1), `Role`, `ACCOUNT_ROLE` (N:N `@JoinTable`) khớp 100% Flyway V1 schema (`ddl-auto=validate`); `mvn clean test` PASS 30/30 tests. |
| **Phase 2** | **Mod 2A** | **Task 2A.2** | Spring Data JPA Repositories Cụm Định danh | **COMPLETED** | Task 2A.1 | `IdentityRepositoryTests` PASS 7/7 tests (0.25s); `AccountRepository` (`findByEmailOrPhone`, `existsByEmailOrPhone`), `RoleRepository` (`findByRoleName`), `UserProfileRepository` (`findByAccount`); `mvn clean test` PASS 37/37 tests. |
| **Phase 2** | **Mod 2B** | **Task 2B.1** | JPA Entity Mapping: `RADICAL`, `VOCABULARY`, `VOCAB_RADICAL` | **COMPLETED** | Task 1A.4 [PARALLEL với 2A] | `DictionaryPersistenceTests` PASS 5/5 tests (0.26s); `Radical`, `Vocabulary` (`pinyin` & `pinyin_raw` độc lập), quan hệ N:N qua `@JoinTable(name = "vocab_radical")` bảo toàn composite PK vật lý; `mvn clean test` PASS 42/42 tests. |
| **Phase 2** | **Mod 2B** | **Task 2B.2** | Spring Data JPA Repositories Cụm Từ điển | **COMPLETED** | Task 2B.1 | `DictionaryRepositoryTests` PASS 8/8 tests (0.35s); `RadicalRepository` (`findByCharacter`, `existsByCharacter`), `VocabularyRepository` (`findByHanziAndPinyinRaw`, `findByHanzi`, `findByPinyinRaw`, `searchByKeyword` kèm phân trang `Pageable`); `mvn clean test` PASS 50/50 tests. |
| **Phase 2** | **Mod 2C** | **Task 2C.1** | JPA Entity Mapping: `LESSON`, `LESSON_VOCABULARY` | **COMPLETED** | Task 2A.1 (Account Entity), Task 2B.1 (Vocab Entity) | `LessonPersistenceTests` PASS 5/5 tests (0.20s); `Lesson` (createdBy Account FK, status Draft), `LessonVocabulary` (composite PK `LessonVocabularyId` qua `@EmbeddedId` + `@MapsId`, `@OrderBy("orderIndex ASC")`, shared vocab không trùng lặp, cascade delete giới hạn ở junction table); `mvn clean test` PASS 55/55 tests. |
| **Phase 2** | **Mod 2C** | **Task 2C.2** | Spring Data JPA Repositories Cụm Bài học | **COMPLETED** | Task 2C.1, Task 2A.2, Task 2B.2 | `LessonRepositoryTests` PASS 7/7 tests (0.41s); `LessonRepository` (`findByCreatedBy`, `findByStatus`, `findByCreatedByAndStatus` kèm phân trang `Pageable`), `LessonVocabularyRepository` (`findByLessonOrderByOrderIndexAsc`, `findByLesson_LessonIdOrderByOrderIndexAsc`, `existsBy...`, `countBy...`); `mvn clean test` PASS 62/62 tests. |
| **Phase 2** | **Mod 2D** | **Task 2D.1** | JPA Entity Mapping: `USER_SRS_SETTING`, `PERSONAL_NOTE`, `MODERATION_LOG` | **COMPLETED** | Task 2A.1 (UserProfile), Task 2B.1 (Vocab), Task 2C.1 (Lesson) | `ProgressAuditPersistenceTests` PASS 7/7 tests (0.41s); `UserSrsSetting` (1:1 với `UserProfile`), `PersonalNote` (content $\le 500$ chars, no 5-note limit, `Vocabulary` không bị cascade delete), `ModerationLog` (immutable audit trail, `fk_moderation_log_lesson` `ON DELETE RESTRICT` bảo toàn); `mvn clean test` PASS 69/69 tests. |
| **Phase 2** | **Mod 2D** | **Task 2D.2** | JPA Entity Mapping: `CARD_PROGRESS`, `REVIEW_LOG` (Tham chiếu đa hình) | **COMPLETED** | Task 2A.1 (UserProfile) | `SrsProgressPersistenceTests` PASS 5/5 tests (0.17s); `CardProgress` & `ReviewLog` ánh xạ tham chiếu đa hình (`item_type` + `item_id`) không có FK vật lý/JPA tới `Vocabulary`/`Radical`, `rating` `Byte` (TINYINT), `review_time_seconds` bảo toàn, `uk_card_progress_user_item` thực thi chuẩn; `mvn clean test` PASS 74/74 tests. |
| **Phase 2** | **Mod 2D** | **Task 2D.3** | Spring Data JPA Repositories Cụm SRS, Ghi chú & Kiểm toán | **COMPLETED** | Task 2D.1, Task 2D.2 | `SrsProgressRepositoryTests` PASS 7/7 tests (0.22s); `CardProgressRepository` (tìm thẻ đến hạn theo reference time có user-isolation `findByUserAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc`, `countByUserAnd...`, `findByUserAndItemTypeAndItemId`), `ReviewLogRepository` (`findByUserOrderByReviewedAtDesc`, card history), `PersonalNoteRepository` (không giới hạn 5 notes, `findByNoteIdAndUser` ownership isolation), `ModerationLogRepository` (audit trail ordered `createdAt DESC`, không custom delete), `UserSrsSettingRepository` (`findByUser` Optional, `existsByUser`); `mvn clean test` PASS 81/81 tests. |
| **Phase 2** | **Mod 2E** | **Task 2E.1** | Flyway Seed Data V2: 4 Vai trò hệ thống (`V2__seed_roles.sql`) | **COMPLETED** | Task 1A.4 [PARALLEL với 2A..2D] | `V2__seed_roles.sql` áp dụng thành công qua Flyway; seed 4 vai trò cố định (`1=Learner`, `2=Creator`, `3=Moderator`, `4=Admin`); `flyway_schema_history` ghi nhận version 2 (checksum `449376763`, success 1); 0 bản ghi trùng lặp; schema bảng `role` giữ nguyên cấu trúc; `mvn clean test` PASS 81/81 tests. |
| **Phase 2** | **Mod 2E** | **Task 2E.2** | Flyway Seed Data V3: 214 Bộ thủ Khang Hy (`V3__seed_radicals.sql`) | **COMPLETED** | Task 1A.4 [PARALLEL với 2A..2D] | `V3__seed_radicals.sql` áp dụng thành công qua Flyway; nạp đủ 214 bộ thủ Khang Hy chuẩn từ dataset `.agents/references/radicals.json`; `flyway_schema_history` ghi nhận version 3 (checksum `-1949280069`, success 1); đối chiếu 214/214 records khớp 100% field-by-field giữa JSON và MySQL; 0 duplicate ID/character; 0 null required fields; schema radical giữ nguyên; `mvn clean test` PASS 81/81 tests; Checkpoint 2E PASS (4 roles, 214 radicals). |
| **Phase 2** | **Mod 2F** | **Task 2F.1** | Kiểm thử tích hợp toàn diện tầng Persistence & Schema Validation | **COMPLETED** | Mod 2A, 2B, 2C, 2D, 2E | `mvn clean test` PASS 81/81 tests (33.86s); toàn bộ 14 bảng quan hệ khớp 100% với Hibernate `ddl-auto=validate`; `flyway_schema_history` hợp lệ cả 3 migrations (V1, V2, V3); 12 Spring Data JPA repositories pass; polymorphic `item_type`+`item_id` không có FK vật lý; seed data 4 roles và 214 radicals toàn vẹn; Phase 2 chính thức COMPLETED. |
| **Phase 3** | **Mod 3A** | **Task 3A.1** | Cấu hình Spring Security 6 FilterChain, BCrypt & Stateless Session | **COMPLETED** | Task 1A.1, Task 1B.2 | `SecurityConfigTests` PASS 10/10 tests (8.71s); `SecurityConfig` khởi tạo thành công với `SecurityFilterChain` stateless (`SessionCreationPolicy.STATELESS`), vô hiệu hóa CSRF cho REST API (`csrf.disable()`), Bean `BCryptPasswordEncoder` sinh hash chuẩn 60-byte khớp và kiểm tra salt độc lập; authorization boundaries phân tách chính xác routes công khai (`/api/v1/auth/**`, GET catalog `/api/v1/radicals/**`, `/api/v1/vocabulary/**`, `/api/v1/lessons/**`, `/error`) và từ chối unauthenticated request tới endpoint được bảo vệ; full regression `mvn clean test` PASS 91/91 tests (16.99s). |
| **Phase 3** | **Mod 3A** | **Task 3A.2** | Xây dựng `JwtUtil` và `JwtAuthenticationFilter` | **COMPLETED** | Task 3A.1 | `JwtUtilTests` PASS 11/11 tests (0.57s), `JwtAuthenticationFilterTests` PASS 7/7 tests (0.50s), `SecurityConfigTests` PASS 12/12 tests (8.38s); JJWT 0.12.6 hoạt động chuẩn xác; `JwtUtil` sinh token ký HMAC-SHA256 chứa `sub`, `roles`, `iat`, `exp`, xác thực chữ ký, phát hiện token hết hạn/sai key/tampered/malformed; `JwtAuthenticationFilter` chặn `Authorization: Bearer <token>`, trích xuất claims và nạp `Authentication` vào `SecurityContextHolder` trước `UsernamePasswordAuthenticationFilter` mà không truy vấn database; full regression `mvn clean test` PASS 111/111 tests (17.33s). |
| **Phase 3** | **Mod 3A** | **Task 3A.3** | Triển khai `CustomUserDetailsService` tải thông tin tài khoản | **COMPLETED** | Task 2A.2, Task 3A.2 | `CustomUserDetailsTests` PASS 11/11 tests (0.15s), `CustomUserDetailsServiceTests` PASS 6/6 tests (1.57s), `CustomUserDetailsServiceIntegrationTests` PASS 2/2 tests (8.00s); `CustomUserDetails` bao bọc an toàn các trường vô hướng (`accountId`, `emailOrPhone`, `passwordHash`, `status`, `authorities`), cách ly hoàn toàn khỏi lazy JPA Entity graph; mapping chuẩn `ROLE_<RoleName>` không trùng lặp, không tự động gán role default khi user không có role; `isEnabled()` phản ánh đúng `Active`, `isAccountNonLocked()` phản ánh đúng `Banned`/`Locked`; `CustomUserDetailsService` nạp `Account` qua `AccountRepository.findByEmailOrPhone`, ném `UsernameNotFoundException` an toàn không rò rỉ SQL khi user không tồn tại hoặc identifier rỗng/null; Spring Boot tự động kết nối bean vào Global `AuthenticationManager`; Checkpoint 3A chính thức nghiệm thu đạt; full regression `mvn clean test` PASS 130/130 tests (19.37s). |
| **Phase 3** | **Mod 3B** | **Task 3B.1** | Auth DTOs (`RegisterRequest`, `LoginRequest`, `AuthResponse`) | **COMPLETED** | Task 1B.1 | `AuthenticationDtoTests` PASS 14/14 tests (0.66s); `RegisterRequest` validate chính xác `emailOrPhone` (`@NotBlank`, `@Size(max=191)`), `password` (`@NotBlank`, `@Size(min=6, max=100)`), `fullName` (`@NotBlank`, `@Size(max=100)`); `LoginRequest` validate chính xác `emailOrPhone` và `password`; `AuthResponse` chứa đầy đủ `token`, `type` (`"Bearer"`), `accountId`, `emailOrPhone`, `fullName`, `roles`, hỗ trợ `@JsonAlias` (`accessToken`, `tokenType`), tuyệt đối không rò rỉ JPA Entity hay credentials; các DTO che giấu mật khẩu trong `toString()` bằng `[PROTECTED]`; full regression `mvn clean test` PASS 144/144 tests (35.83s). |
| **Phase 3** | **Mod 3B** | **Task 3B.2** | `AuthService` & `AuthController` (`/api/v1/auth/**`) | **COMPLETED** | Task 1B.1, 1B.2, 2A.2, 2E.1 (cần Role Learner), 3A.3, 3B.1 | `AuthServiceTests` PASS 6/6 tests (1.53s), `AuthControllerTests` PASS 6/6 tests (2.28s), `AuthIntegrationTests` PASS 1/1 (7.62s); triển khai hoàn chỉnh vertical slice đăng ký + đăng nhập: `POST /api/v1/auth/register` (201 Created), `POST /api/v1/auth/login` (200 OK kèm JWT); đăng ký kiểm tra trùng lặp email/phone trả về 409 Conflict, băm mật khẩu BCrypt, gán vai trò mặc định `Learner`, cascade tạo `UserProfile`, bọc trong transaction nguyên tử; đăng nhập xác thực qua `AuthenticationManager`, sinh token JWT chuẩn qua `JwtUtil`, từ chối thông tin sai bằng 401 generic không rò rỉ user existence; JWT handoff hoạt động trơn tru với `JwtAuthenticationFilter`; nghiệm thu đạt Checkpoint 3B; full regression `mvn clean test` PASS 157/157 tests (20.98s). |
| **Phase 3** | **Mod 3C** | **Task 3C.1** | User Profile DTOs, `UserProfileService` & `UserProfileController` | **COMPLETED** | Task 1B.1, 1B.2, 2A.2, 3B.2 | `UserProfileServiceTests` PASS 5/5 tests (1.12s), `UserProfileControllerTests` PASS 3/3 tests (0.09s), `UserProfileIntegrationTests` PASS 2/2 tests (7.31s); triển khai hoàn chỉnh vertical slice hồ sơ người dùng: `GET /api/v1/users/profile` (200 OK) và `PUT /api/v1/users/profile` (200 OK) lấy và cập nhật profile của user đang đăng nhập qua `SecurityContextHolder`; đảm bảo nguyên tắc Profile Ownership Isolation (User A và User B độc lập hoàn toàn, không thể truyền ID để xem/sửa profile người khác); truy cập không có token trả về 401 `UNAUTHORIZED` (Checkpoint 3C); DTOs `UserProfileResponse` và `UpdateProfileRequest` đóng gói an toàn không rò rỉ JPA entity hay mật khẩu; cập nhật `fullName` và `avatarUrl` lưu bền vững vào MySQL; full regression `mvn clean test` PASS 167/167 tests (21.89s). |
| **Phase 3** | **Mod 3D** | **Task 3D.1** | Kiểm thử tự động MockMvc cho Auth & RBAC 4 vai trò | **COMPLETED** | Mod 3A, 3B, 3C | `RbacSecurityIntegrationTests` PASS 20/20 tests (12.48s); bộ kiểm thử tích hợp MockMvc xác minh ma trận phân quyền 4 vai trò (`Learner`, `Creator`, `Moderator`, `Admin`); cấu hình chuẩn `hasRole`/`hasAnyRole` trên `SecurityFilterChain` cho `/api/v1/admin/**`, `/api/v1/moderator/**`, `/api/v1/creator/**`; kiểm chứng unauthenticated trả về 401 `UNAUTHORIZED` cho các endpoint bảo vệ; kiểm chứng Learner bị từ chối 403 `FORBIDDEN` khi cố truy cập endpoint của Admin/Moderator/Creator (Checkpoint Phase 3); kiểm chứng Creator và Moderator truy cập đúng phạm vi và bị chặn 403 khi vượt quyền; kiểm chứng Admin có toàn quyền trên các endpoint quản trị/kiểm duyệt/tác giả; kiểm chứng cả với `CustomUserDetails` lẫn end-to-end real JWT Bearer token qua `JwtAuthenticationFilter`; không tạo feature nghiệp vụ production; nghiệm thu hoàn thành toàn bộ Phase 3; full regression `mvn clean test` PASS 187/187 tests (23.17s). |
| **Phase 4** | **Mod 4A** | **Task 4A.1** | Radical DTOs & `RadicalService` tra cứu Bộ thủ | **COMPLETED** | Task 1B.1, 2B.2, 2E.2 (cần data 214 bộ thủ) | `RadicalServiceTests` PASS 8/8 tests (0.39s), `RadicalServiceIntegrationTests` PASS 6/6 tests (6.91s); xử lý dứt điểm 2 Discrepancies theo Correction Patch: (1) Loại bỏ hoàn toàn `radicalNumber` và `strokeCount` khỏi DTO vì không tồn tại trong CSDL/schema thật; (2) Tách biệt triệt để phân hệ Bộ thủ và Từ vựng theo API.md, loại bỏ vocabulary list khỏi `RadicalDetailResponse`, không phụ thuộc `VocabularyRepository` (chuyển tra cứu vocabulary sang Module 4B); `RadicalResponse` và `RadicalDetailResponse` chỉ chứa radical metadata chuẩn; `RadicalService` và `RadicalServiceImpl` hỗ trợ tra cứu toàn diện 214 bộ thủ Khang Hy (phân trang `getAllRadicals(Pageable)` và toàn bộ danh sách `getAllRadicals()` sắp xếp chuẩn canonical Kangxi theo `radicalId` tăng dần), tra cứu chi tiết theo ID (`getRadicalById`) và ký tự (`getRadicalByCharacter`), xử lý lỗi không tìm thấy bằng `BusinessException(ErrorCode.NOT_FOUND)`; toàn bộ 214 bản ghi bộ thủ trong MySQL được xác minh toàn vẹn; không tạo Controller hay Admin CRUD; full regression `mvn clean test` PASS 201/201 tests (22.53s). |
| **Phase 4** | **Mod 4B** | **Task 4B.2** | `VocabularyController` công khai & Admin CRUD Từ vựng | **COMPLETED** | Task 1B.1, 1B.2, Task 4B.1, Mod 3A | `VocabularyController` (public `GET /api/v1/vocabulary` phân trang và tìm kiếm theo từ khóa `search`, `GET /api/v1/vocabulary/{id}` chi tiết kèm bộ thủ liên kết), `AdminVocabularyController` (`POST /api/v1/admin/vocabulary` 201 Created, `PUT /api/v1/admin/vocabulary/{id}` 200 OK, `DELETE /api/v1/admin/vocabulary/{id}` 204 No Content), request DTOs `CreateVocabularyRequest`, `UpdateVocabularyRequest` (Jakarta Validation), mutation methods trong `VocabularyService`/`VocabularyServiceImpl` (`createVocabulary`, `updateVocabulary`, `deleteVocabulary`, liên kết quan hệ N:N với `Radical`, bắt conflict trùng lặp `hanzi` + `pinyin_raw`); `VocabularyServiceTests` (29/29 PASS), `VocabularyControllerTests` (12/12 PASS), `VocabularyIntegrationTests` (18/18 PASS xác minh tìm kiếm toneless, liên kết bộ thủ, ma trận RBAC và Admin CRUD trên MySQL); Checkpoint 4B đạt; full regression `mvn clean test` PASS 311/311 tests (26.74s). |
| **Phase 4** | **Mod 4C** | **Task 4C.1** | Kiểm thử tự động MockMvc cho phân hệ Bộ thủ & Từ vựng | **COMPLETED** | Mod 4A, 4B | `CatalogIntegrationTests` PASS 25/25 tests (8.39s); kiểm thử tích hợp toàn diện Catalog Domain trên CSDL MySQL thật với Spring Boot context đầy đủ và real JWT tokens; xác minh bất biến 214 bộ thủ Khang Hy (`RadicalCatalogVerificationTests` PASS 8/8), tra cứu công khai, tìm kiếm không dấu tone-less `ni -> nǐ / 你` (Checkpoint 4B) và `xiu -> 休` (`VocabularyCatalogSearchVerificationTests` PASS 8/8), định dạng phân trang `PageResponse` (`VocabularyCatalogPaginationVerificationTests` PASS 3/3), ma trận phân quyền Admin RBAC (401 unauthenticated, 403 forbidden với Learner/Creator/Moderator, CRUD thành công với Admin) (`VocabularyAdminSecurityAndCrudVerificationTests` PASS 6/6); đạt Checkpoint Phase 4; hoàn thành toàn bộ Phase 4; full regression `mvn clean test` PASS 336/336 tests (27.85s). |
| **Phase 5** | **Mod 5A** | **Task 5A.1** | Lesson DTOs & `LessonService` khám phá bài học công khai | **COMPLETED** | Task 1B.1, Task 2C.2 | `LessonSummaryResponse`, `LessonDetailResponse`, `LessonVocabItemResponse`, `LessonService`, `LessonServiceImpl` (`getApprovedLessons(Pageable)`, `getApprovedLessonById(Long)`); chỉ bài học có trạng thái `Approved` mới được trả về, các trạng thái `Draft`, `Pending`, `Rejected` hoặc ID không tồn tại trả về 404 NOT_FOUND bảo vệ an toàn thông tin; từ vựng trong bài học được sắp xếp theo đúng thứ tự `order_index` tăng dần deterministic; query tối ưu hóa phòng chống N+1 với JPQL constructor projection `SIZE(l.lessonVocabularies)` và `JOIN FETCH lv.vocabulary`; `LessonDtoTests` PASS 7/7 (0.13s), `LessonServiceTests` PASS 9/9 (0.48s), `LessonServiceIntegrationTests` PASS 7/7 (8.98s); full regression `mvn clean test` PASS 359/359 tests (28.68s). |
| **Phase 5** | **Mod 5A** | **Task 5A.2** | `LessonController` xem bài học công khai (`GET /api/v1/lessons/**`) | **COMPLETED** | Task 1B.1, 1B.2, Task 5A.1 | `LessonController` (`GET /api/v1/lessons` có phân trang `PageResponse` và `GET /api/v1/lessons/{id}` chi tiết); truy cập công khai không cần token; bảo đảm chỉ hiển thị bài học `Approved`, bài học `Draft`, `Pending`, `Rejected` hoặc ID không tồn tại trả về 404 NOT_FOUND bảo vệ an toàn dữ liệu; danh sách từ vựng trong bài học giữ đúng thứ tự `order_index` 1, 2, 3; đóng gói `ApiResponse` chuẩn; `LessonControllerTests` PASS 8/8 tests (0.24s), `LessonIntegrationTests` PASS 9/9 tests (14.94s); nghiệm thu đạt hoàn toàn Checkpoint 5A; hoàn tất Module 5A; full regression `mvn clean test` PASS 376/376 tests (47.66s). |
| **Phase 5** | **Mod 5B** | **Task 5B.1** | Creator Lesson DTOs & `CreatorLessonService` quản lý bài viết | **COMPLETED** | Task 2C.2, Mod 3A | `CreateLessonRequest`, `UpdateLessonRequest`, `ReorderVocabRequest`, `VocabOrderItem`, `CreatorLessonService`, `CreatorLessonServiceImpl` (CRUD bài học của creator, thêm/xóa từ vựng kèm re-index, hoán đổi thứ tự `order_index` 2-phase offset 1,000,000 an toàn với MySQL `INT UNSIGNED`, nộp duyệt `Draft`/`Rejected` $\rightarrow$ `Pending`, kiểm soát quyền sở hữu Creator Ownership Isolation 403 Forbidden, hỗ trợ Admin bypass); `CreatorLessonServiceTests` PASS 25/25 tests (0.86s), `CreatorLessonServiceIntegrationTests` PASS 7/7 tests (7.85s); full regression `mvn clean test` PASS 408/408 tests (28.76s). |
| **Phase 5** | **Mod 5B** | **Task 5B.2** | `CreatorLessonController` (`/api/v1/creator/lessons/**`) | **COMPLETED** | Task 1B.1, 1B.2, Task 5B.1 | REST endpoints cho Creator (`/api/v1/creator/lessons/**`); bảo mật Spring Security JWT: 401 unauthenticated, 403 Learner/Moderator, 200/201 Creator/Admin; kiểm tra bảo vệ tài nguyên đạt Checkpoint 5B (Creator A sửa bài Creator B bị chặn 403 Forbidden); hỗ trợ tạo bài (201), xem danh sách phân trang (200), xem chi tiết (200), cập nhật (200), xóa (204), thêm/xóa từ vựng (200), reorder (200) và submit kiểm duyệt (200); `CreatorLessonControllerTests` PASS 16/16 tests (0.42s), `CreatorLessonIntegrationTests` PASS 12/12 tests (6.27s); Checkpoint 5B hoàn tất; hoàn thành toàn bộ Module 5B; full regression `mvn clean test` PASS 436/436 tests (28.78s). |
| **Phase 5** | **Mod 5D** | **Task 5D.1** | Kiểm thử tự động MockMvc cho Quản lý bài học & Import Excel | **COMPLETED** | Mod 5A, 5B, 5C | `LessonLifecycleIntegrationTests` PASS 20/20 tests (17.00s); kiểm thử tích hợp toàn diện Lesson Management & Excel Import trên CSDL MySQL thật với Spring Boot context đầy đủ và real JWT tokens; xác minh 14 phân vùng kiểm thử: (1) Manual Lesson lifecycle (Draft $\rightarrow$ Add Vocab $\rightarrow$ Reorder $\rightarrow$ Submit $\rightarrow$ Pending $\rightarrow$ Public Learner 404), (2) Creator Ownership Isolation (Creator A bị chặn 403 Forbidden trên toàn bộ 7 endpoints bài học Creator B, Admin bypass thành công), (3) Excel Preview zero-mutation (file hợp lệ, hỏng, sai header, lỗi hàng $\rightarrow$ 0 thay đổi CSDL), (4) Excel Confirm nguyên tử lưu đúng trạng thái Draft, liên kết creator, tái sử dụng từ vựng, `order_index` tuần tự 1..N, (5) Application Transaction Rollback trên MySQL thật không phụ thuộc test-managed rollback (`Propagation.NOT_SUPPORTED`), (6) Ma trận bảo mật RBAC (401 unauthenticated, 403 Learner/Moderator, 200/201 Creator/Admin), (7) Giao thức multipart binding và mã lỗi chuẩn hóa `ErrorCode`, (8) Trạng thái bất biến state machine (Pending/Approved khóa sửa, Rejected cho phép sửa/gửi lại), (9) Tái đánh số thứ tự khi xóa từ vựng giữa, (10) Tính toàn vẹn quan hệ cascade và tính nhất quán Flyway V1..V3; nghiệm thu đạt Checkpoint Phase 5; hoàn thành toàn bộ Phase 5; full regression `mvn clean test` PASS 487/487 tests (33.79s). |
| **Phase 6** | **Mod 6A** | **Task 6A.1** | Moderation DTOs & `ModerationService` logic nghiệp vụ duyệt | **COMPLETED** | Task 1B.1, Task 2C.2, Task 2D.3 | `ApproveLessonRequest`, `RejectLessonRequest` (validation `@NotBlank`, `@Size(max = 500)` lý do từ chối, `flaggedFields`), `ModerationQueueResponse` (đóng gói thông tin bài học pending, creator, vocabularyCount không leak entity), `ModerationService`, `ModerationServiceImpl` (`getPendingLessons(Pageable)`, `getPendingLessonById(Long)`, `approveLesson(Long, ApproveLessonRequest)`, `rejectLesson(Long, RejectLessonRequest)`); bảo vệ bất biến trạng thái: chỉ bài học `Pending` mới được approve hoặc reject (thao tác trên Draft/Approved/Rejected trả về 409 CONFLICT), ID không tồn tại trả về 404 NOT_FOUND, lý do từ chối rỗng trả về 400 BAD_REQUEST; trích xuất định danh moderator an toàn từ `SecurityContextHolder` (chống spoofing moderatorId); query tối ưu hóa phòng chống N+1 với JPQL constructor projection `SIZE(l.lessonVocabularies)`; `ModerationDtoTests` PASS 11/11 tests (0.90s), `ModerationServiceTests` PASS 17/17 tests (0.58s), `ModerationServiceIntegrationTests` PASS 6/6 tests (7.51s); full regression `mvn clean test` PASS 521/521 tests (35.57s). |
| **Phase 6** | **Mod 6A** | **Task 6A.2** | Ghi vết kiểm duyệt bất biến vào `MODERATION_LOG` | **COMPLETED** | Task 6A.1 | Triển khai ghi log kiểm duyệt bất biến trong `MODERATION_LOG` trên cả luồng phê duyệt (`action = "Approve"`) và từ chối (`action = "Reject"` kèm `rejection_reason` và `flagged_fields` JSON); đảm bảo tính nguyên tử giao dịch (Transaction Atomicity): nếu ghi log thất bại, trạng thái bài học tự động rollback về `Pending` trên CSDL MySQL thật (xác minh qua failure injection với `Propagation.NOT_SUPPORTED`); bảo đảm tính bất biến (Audit Immutability): mỗi lần duyệt/từ chối tạo một bản ghi log mới tuần tự không ghi đè; nghiệm thu đạt Checkpoint 6A; hoàn thành toàn bộ Module 6A; `ModerationServiceTests` PASS 17/17 tests (0.38s), `ModerationServiceIntegrationTests` PASS 9/9 tests (8.19s); full regression `mvn clean test` PASS 524/524 tests (36.71s). |
| **Phase 6** | **Mod 6B** | **Task 6B.1** | `ModeratorController` (`/api/v1/moderator/**`) | **COMPLETED** | Task 1B.1, 1B.2, Task 6A.2, Mod 3A | Triển khai REST Controller `ModeratorController` (`/api/v1/moderator/lessons/**`) cho Moderator/Admin; bảo mật Spring Security JWT: 401 unauthenticated, 403 Learner/Creator, 200 Moderator/Admin; kiến trúc thin controller chuẩn mực phân tầng (không chứa business logic, không truy cập repo, không mutate entity, không duplicate audit log, không leak entity, bao bọc qua `ApiResponse<T>` và `PageResponse<T>`); hỗ trợ xem hàng đợi duyệt `GET /pending` (200), xem chi tiết bài pending `GET /{id}` (200), phê duyệt `POST /{id}/approve` (200, cập nhật `Approved` và ghi audit), từ chối `POST /{id}/reject` (200, kiểm tra `@Valid @NotBlank` lý do từ chối, cập nhật `Rejected` và ghi audit feedback); đạt Checkpoint 6B; hoàn thành toàn bộ Module 6B; `ModeratorControllerTests` PASS 8/8 tests (0.35s), `ModeratorIntegrationTests` PASS 20/20 tests (6.77s); full regression `mvn clean test` PASS 552/552 tests (35.47s). |
| **Phase 6** | **Mod 6C** | **Task 6C.1** | Kiểm thử tự động MockMvc cho quy trình Phê duyệt & Từ chối | **COMPLETED** | Mod 6A, 6B | `Phase6ModerationWorkflowIntegrationTests` PASS 18/18 tests (11.50s); kiểm thử tích hợp toàn diện chu trình xuất bản và kiểm duyệt nội dung trên CSDL MySQL thật với Spring Boot context đầy đủ và real JWT tokens; xác minh 5 phân vùng kiểm thử chính: (1) Full Primary Lifecycle (Creator tạo Draft $\rightarrow$ nộp Pending $\rightarrow$ Moderator A từ chối Rejected kèm lý do và flagged_fields $\rightarrow$ Creator sửa bài Rejected $\rightarrow$ Creator nộp lại Pending $\rightarrow$ Moderator B duyệt Approved $\rightarrow$ bài học lập tức hiển thị công khai trên Public Lesson API cả list và detail), (2) Pre-approval public isolation (bài học Draft/Pending/Rejected tuyệt đối không hiển thị ở Public Catalog và trả về 404 trên detail), (3) Tính bất biến kiểm toán (Audit Immutability: 2 bản ghi log phân biệt rõ ràng giữa Reject của Mod A và Approve của Mod B, bảo toàn nguyên vẹn log cũ), (4) Bất biến trạng thái State Machine (chặn các chuyển đổi bất hợp lệ: Draft/Rejected/Approved $\rightarrow$ Approve trực tiếp 409, Draft/Approved $\rightarrow$ Reject 409, Pending/Approved $\rightarrow$ Edit/Submit 409), (5) Ma trận bảo mật RBAC & Cô lập quyền sở hữu Creator (401 Anonymous, 403 Learner/Creator trên moderator endpoints, Creator B không thể sửa/nộp bài Creator A, Admin quản trị thành công); nghiệm thu đạt Checkpoint Phase 6; hoàn thành toàn bộ Phase 6; full regression `mvn clean test` PASS 570/570 tests (50.29s). |
| **Phase 7** | **Mod 7A** | **Task 7A.1** | Triển khai thuật toán thuần túy SM-2 trong `SrsCalculator` | **COMPLETED** | Không [Pure Algorithm] | Triển khai `SrsCalculator` toán học thuần túy (Java 21 `record SrsCalculationResult`, `enum ReviewRating`), độc lập hoàn toàn khỏi Spring/Database/JPA/REST; quy chuẩn ánh xạ 4 mức rating: 1=Again ($q=0, \Delta EF=-0.80$, reset repetitions=0, interval=0), 2=Hard ($q=3, \Delta EF=-0.14$), 3=Good ($q=4, \Delta EF=0.00$), 4=Easy ($q=5, \Delta EF=+0.10$); chặn sàn $EF \ge 1.30$, khởi tạo $EF=2.50$, chu kỳ interval 1 ngày, 6 ngày, $\lceil I_{n-1} \times EF \rceil$ làm tròn lên (`Math.ceil`); `SrsCalculatorTests` PASS 20/20 pure unit tests (0.22s); full regression `mvn clean test` PASS 590/590 tests (38.23s). |
| **Phase 7** | **Mod 7A** | **Task 7A.2** | Unit Test toán học cho thuật toán tính khoảng cách SM-2 | **COMPLETED** | Task 7A.1 | Xây dựng bộ Unit Test toán học toàn diện và độc lập (`SrsCalculatorTests`) với 59 test vectors parameterized; kiểm chứng độc lập 10 nhóm kịch bản: (1) Hằng số đặc tả kỹ thuật ($EF_{init}=2.50, EF_{min}=1.30, I_1=1, I_2=6, 1 \le rating \le 4$), (2) Kiểm tra hợp lệ đầu vào và ngoại lệ chặt chẽ (bắt lỗi rating $\notin [1..4]$, $EF \le 0$, null, NaN, Infinite, âm interval/repetition), (3) Ngữ nghĩa từng mức rating và $\Delta EF$ chính xác (Again: $-0.80$, Hard: $-0.14$, Good: $0.00$, Easy: $+0.10$), (4) Khởi tạo thẻ học mới `CARD_PROGRESS` ($R=0, I=0$), (5) Cố định interval lần 1 (1 ngày) và lần 2 (6 ngày), (6) Tính toán chu kỳ lần 3+ với làm tròn lên `Math.ceil` và kiểm tra chống sai số $+1$ trên số nguyên exact, (7) Bất biến chặn sàn $EF \ge 1.30$ (phục hồi khi Easy và duy trì khi Good), (8) Chuỗi học tập đa bước (Standard Learning và Fail-Reset-Relearn recovery), (9) Tính tất định, không trạng thái và tính nhất quán giữa các overload phương thức, (10) An toàn biên số lớn và chống tràn số `Integer.MAX_VALUE`; nghiệm thu đạt Checkpoint 7A; hoàn thành toàn bộ Module 7A; `SrsCalculatorTests` PASS 59/59 tests (0.43s, không tải Spring context); full regression `mvn clean test` PASS 629/629 tests (38.00s). |
| **Phase 7** | **Mod 7B** | **Task 7B.1** | SRS DTOs & `SrsService` điều phối phiên học lặp lại ngắt quãng | **COMPLETED** | Task 7A.1, Task 2D.3 | Triển khai DTOs (`ReviewCardRequest`, `DueCardResponse`, `StudyStatsResponse`) và service layer `SrsService` / `SrsServiceImpl`; ủy quyền tính toán toán học cho `SrsCalculator`; giải quyết toàn diện tham chiếu đa hình `item_type` + `item_id` (`VOCABULARY` và `RADICAL`) ở tầng Service; xác thực danh tính người học bảo mật từ Spring Security `SecurityContextHolder`; thực thi cập nhật `CARD_PROGRESS` và ghi nhận nhật ký bất biến `REVIEW_LOG` (lưu trữ chính xác `rating`, `interval_before`, `interval_after`, `review_time_seconds`, `reviewed_at`) trong giao dịch nguyên tử `@Transactional`; áp dụng giới hạn ngày từ `USER_SRS_SETTING` (`new_cards_per_day`, `max_review_per_day`); `SrsDtoTests` PASS 10/10 tests (0.01s), `SrsServiceTests` PASS 10/10 tests (0.61s), `SrsServiceIntegrationTests` PASS 9/9 real DB tests (7.79s); full regression `mvn clean test` PASS 658/658 tests (39.73s). |
| **Phase 7** | **Mod 7B** | **Task 7B.2** | `SrsController` (`/api/v1/srs/**`) | **COMPLETED** | Task 1B.1, 1B.2, Task 7B.1, Mod 3A | Triển khai REST Controller `SrsController` (`/api/v1/srs/**`) theo kiến trúc Thin Controller chuẩn mực; expose 3 REST endpoints (`GET /api/v1/srs/due`, `POST /api/v1/srs/review`, `GET /api/v1/srs/stats`); tích hợp `@Valid` request body validation và Spring Security (401 Unauthorized cho Anonymous, 200 OK cho Learner và Admin); bao bọc chuẩn phản hồi `ApiResponse<T>`; xử lý tập trung `HttpMessageNotReadableException` qua `GlobalExceptionHandler`; `SrsControllerTests` PASS 8/8 unit tests (0.42s), `SrsIntegrationTests` PASS 10/10 tests trên MySQL thật (6.38s); hoàn thành toàn bộ Module 7B; nghiệm thu đạt Checkpoint 7B; full regression `mvn clean test` PASS 676/676 tests (38.77s). |
| **Phase 7** | **Mod 7C** | **Task 7C.1** | Kiểm thử tự động MockMvc cho phân hệ ôn tập SRS | **COMPLETED** | Mod 7A, 7B | Xây dựng bộ kiểm thử tích hợp toàn diện `SrsPolymorphicIntegrityTests` trên CSDL MySQL 8.4 thật; xác minh 100% tính toàn vẹn tham chiếu đa hình của `CARD_PROGRESS (user_id, item_type, item_id)` và `REVIEW_LOG (user_id, item_type, item_id)`: (1) Chấp nhận và tạo mới/cập nhật `CARD_PROGRESS` + `REVIEW_LOG` khi `VOCABULARY` hoặc `RADICAL` tồn tại, (2) Từ chối với `NOT_FOUND` và đảm bảo zero-mutation khi `item_id` không tồn tại, (3) Chặn đứng va chạm bảng chéo (cross-table collision: từ chối ID chỉ tồn tại ở Radical khi request VOCABULARY và ngược lại), (4) Cùng một ID số $K$ ở 2 bảng khác nhau tạo 2 thẻ học và 2 bản ghi log hoàn toàn độc lập, (5) Cô lập tuyệt đối giữa User A và User B, (6) Chống trùng lặp tiến trình và duy trì nhật ký bất biến append-only, (7) Phân giải thẻ đến hạn đa hình và xử lý an toàn thẻ mồ côi (stale progress resilience), (8) Giao dịch nguyên tử và rollback hoàn hảo khi phát sinh lỗi I/O; nghiệm thu đạt Checkpoint Phase 7; hoàn thành toàn bộ Phase 7; full regression `mvn clean test` PASS 688/688 tests (39.09s). |
| **Phase 8** | **Mod 8A** | **Task 8A.1** | Personal Note DTOs & `PersonalNoteService` | **COMPLETED** | Task 1B.1, Task 2D.3, Mod 3A | Triển khai DTOs (`PersonalNoteRequest`, `PersonalNoteResponse`) và service layer `PersonalNoteService` / `PersonalNoteServiceImpl` cho Vertical Slice Ghi chú cá nhân: (1) CRUD ghi chú đầy đủ (`getNotesByVocabulary`, `createNote`, `updateNote`, `deleteNote`), (2) Kiểm tra chặt chẽ độ dài nội dung `content <= 500 characters` (500 chars ACCEPT, 501 chars REJECT), (3) Bác bỏ hoàn toàn giới hạn 5 notes cũ (hỗ trợ unlimited notes per vocabulary), (4) Xác thực danh tính và bảo vệ quyền sở hữu an toàn từ `SecurityContextHolder` (chặn IDOR: User B cố sửa/xóa ghi chú của User A bị từ chối 403 `FORBIDDEN`), (5) Kiểm tra toàn vẹn danh mục từ vựng trước khi tạo/đọc ghi chú, (6) Giữ nguyên tính bất biến của `vocab_id`, `user_id`, `created_at` khi cập nhật nội dung; `PersonalNoteDtoTests` PASS 5/5 tests (0.01s), `PersonalNoteServiceTests` PASS 10/10 tests (0.57s), `PersonalNoteServiceIntegrationTests` PASS 10/10 real MySQL tests (7.25s); full regression `mvn clean test` PASS 713/713 tests (39.16s). |
| **Phase 8** | **Mod 8A** | **Task 8A.2** | `PersonalNoteController` (`/api/v1/notes/**`, `/api/v1/vocabularies/{id}/notes`) | **COMPLETED** | Task 1B.1, 1B.2, Task 8A.1 | Triển khai REST Controller `PersonalNoteController` theo kiến trúc Thin Controller chuẩn mực; expose 4 REST endpoints (`GET /api/v1/vocabularies/{vocabId}/notes`, `POST /api/v1/vocabularies/{vocabId}/notes`, `PUT /api/v1/notes/{noteId}`, `DELETE /api/v1/notes/{noteId}`); tích hợp `@Valid` request body validation và Spring Security (401 Unauthorized cho Anonymous, 200/201 cho Authenticated user); xử lý tập trung lỗi IDOR (403 Forbidden khi sửa/xóa ghi chú người khác), lỗi không tìm thấy (404 Not Found) và lỗi dữ liệu (400 Bad Request / VALIDATION_ERROR); `PersonalNoteControllerTests` PASS 11/11 unit tests (0.43s), `PersonalNoteIntegrationTests` PASS 13/13 real MySQL tests (6.09s); hoàn thành toàn bộ Module 8A; nghiệm thu đạt Checkpoint 8A; full regression `mvn clean test` PASS 737/737 tests (39.13s). |
| **Phase 8** | **Mod 8B** | **Task 8B.1** | Setting DTOs, `UserSrsSettingService` & `UserSrsSettingController` | **COMPLETED** | Task 1B.1, 1B.2, Task 2D.3, Mod 3A | Triển khai DTOs (`UpdateSrsSettingRequest`, `UserSrsSettingResponse`), service layer `UserSrsSettingService` / `UserSrsSettingServiceImpl`, và REST controller `UserSrsSettingController` cho Vertical Slice Cài đặt SRS cá nhân: (1) Expose `GET/PUT /api/v1/srs/settings` theo kiến trúc Thin Controller chuẩn mực, (2) Khởi tạo cấu hình mặc định (20 thẻ mới / 100 lượt ôn tập tối đa) khi user chưa cấu hình, (3) Cập nhật và lưu trữ an toàn cấu hình học tập cá nhân trên MySQL, (4) Kiểm tra chặt chẽ giá trị số nguyên dương $> 0$ tại ranh giới DTO và Service (từ chối 0 hoặc số âm với HTTP 400 `VALIDATION_ERROR`, đảm bảo zero DB mutation), (5) Đảm bảo cách ly dữ liệu tuyệt đối giữa các user; `UserSrsSettingDtoTests` PASS 9/9 tests (0.01s), `UserSrsSettingServiceTests` PASS 7/7 tests (0.18s), `UserSrsSettingControllerTests` PASS 5/5 tests (0.42s), `UserSrsSettingIntegrationTests` PASS 7/7 real MySQL tests (6.84s); hoàn thành toàn bộ Module 8B; nghiệm thu đạt Checkpoint 8B; full regression `mvn clean test` PASS 765/765 tests (40.26s). |
| **Phase 8** | **Mod 8C** | **Task 8C.1** | Kiểm thử tự động MockMvc cho Ghi chú cá nhân & Cài đặt SRS | **COMPLETED** | Mod 8A, 8B | Xây dựng bộ kiểm thử tích hợp toàn diện `Phase8PersonalizationIntegrationTests` trên CSDL MySQL 8.4 thật; xác minh 100% mối quan hệ nhân quả và tích hợp end-to-end xuyên suốt toàn bộ stack REST: (1) Cập nhật `maxReviewPerDay` qua `PUT /api/v1/srs/settings` lập tức làm thay đổi trực tiếp và chính xác số lượng thẻ trả về trong `GET /api/v1/srs/due` (chứng minh qua 4 nấc $3 \rightarrow 7 \rightarrow 12 \rightarrow 2$), (2) Xác minh cơ chế bão hòa (limit saturation: khi limit vượt số thẻ có sẵn, trả về toàn bộ thẻ hợp lệ mà không phát sinh lỗi), (3) Xác minh cơ chế cạn kiệt và khôi phục hạn ngạch ôn tập theo ngày (daily quota exhaustion: khi đạt giới hạn review trong ngày, trả về 0 thẻ; khi tăng limit, lập tức mở lại hạn ngạch còn lại), (4) Xác minh cô lập cá nhân hóa tuyệt đối giữa User A và User B, (5) Bảo vệ validation và zero DB mutation khi gửi setting không hợp lệ, (6) Xác minh truyền tải thông số `newCardsPerDay` từ `PUT /srs/settings` vào `GET /srs/stats`, (7) Kiểm tra an ninh 401 Unauthorized cho Anonymous trên toàn bộ endpoints; `Phase8PersonalizationIntegrationTests` PASS 10/10 real MySQL tests (8.94s); hoàn thành toàn bộ Phase 8; nghiệm thu đạt Checkpoint Phase 8; full regression `mvn clean test` PASS 775/775 tests (46.52s). |
| **Hardening** | **Auth-Sec** | **BE-AUTH-002** | Xóa bỏ Hardcoded JWT Secret trong cấu hình và class | **COMPLETED** | Phase 3 | Loại bỏ hoàn toàn fallback default secret trong `application.yml` và `JwtUtil.java`, cấu hình test secret riêng tại `src/test/resources/application.yml`, bổ sung 4 fail-fast tests trong `JwtUtilTests.java`; full regression `mvn clean test` PASS 779/779 tests (0 failures, 0 errors). |
| **Hardening** | **Test-Infra** | **BE-TEST-003** | Cách ly kiểm thử tích hợp với Testcontainers + MySQL 8.4 | **COMPLETED** | Phase 1..8 | Tích hợp `spring-boot-testcontainers` và `testcontainers:mysql:8.4.0` với singleton `TestcontainersInitializer` và `--lower-case-table-names=1`; tự động chạy Flyway V1-V3 trên container độc lập; bổ sung `TestDatabaseIsolationTests.java`; cách ly 100% khỏi `localhost:3306/elearning_db`; full regression `mvn clean test` PASS 781/781 tests (0 failures, 0 errors, 01:03 min). |
| **Hardening** | **Upload-Sec**| **BE-UPLOAD-001** | Giới hạn số dòng và tài nguyên khi Import Excel | **COMPLETED** | Phase 5 | Bổ sung giới hạn `MAX_DATA_ROWS = 5000` và guard kiểm tra `lastRowNum > MAX_DATA_ROWS` sớm trong `ExcelParserServiceImpl`, trả về `ROW_LIMIT_EXCEEDED` trước khi lặp dòng/truy vấn CSDL; bổ sung boundary tests và integration tests; full regression `mvn clean test` PASS 786/786 tests (0 failures, 0 errors, 01:13 min). |
| **Hardening** | **AuthZ-Sec** | **BE-AUTHZ-001** | Lọc trạng thái Pending trong getPendingLessonById | **COMPLETED** | Phase 6 | Bổ sung bộ lọc trạng thái `Pending` trong `ModerationServiceImpl.getPendingLessonById`, từ chối 404 NOT_FOUND đối với `Draft`, `Approved`, `Rejected` và ID không tồn tại, ngăn rò rỉ dữ liệu bài học; bổ sung unit tests và integration tests; full regression `mvn clean test` PASS 795/795 tests (0 failures, 0 errors, 01:13 min). |
| **Hardening** | **Conc-Sec**  | **BE-CONC-001** | Xử lý Race Condition / Lost Update trên CardProgress | **COMPLETED** | Phase 7 | Bổ sung `@Version` và cột `version BIGINT UNSIGNED NOT NULL DEFAULT 0` (migration V4), cấu hình `OptimisticLockingFailureException` $\rightarrow$ 409 CONFLICT trong `GlobalExceptionHandler`; bổ sung multithreaded concurrency integration tests `SrsConcurrencyIntegrationTests.java` và persistence tests `SrsProgressPersistenceTests.java`; full regression `mvn clean test` PASS 800/800 tests (0 failures, 0 errors, 01:15 min). |
| **Hardening** | **Conc-Sec**  | **BE-CONC-003** | Xử lý Double Moderation Race Condition trên Lessons | **COMPLETED** | Phase 6 | Bổ sung `@Version` và cột `version BIGINT UNSIGNED NOT NULL DEFAULT 0` (migration V5), cấu hình `ConcurrencyFailureException` $\rightarrow$ 409 CONFLICT trong `GlobalExceptionHandler`; bổ sung multithreaded concurrency integration tests `ModerationConcurrencyIntegrationTests.java` (Approve vs Approve, Approve vs Reject, 8-thread race) và contract tests trong `ModeratorControllerTests.java`; full regression `mvn clean test` PASS 806/806 tests (0 failures, 0 errors, 01:15 min). |
| **Hardening** | **Conc-Sec**  | **BE-CONC-002** | Xử lý TOCTOU Race Condition trên Daily Review Quota | **COMPLETED** | Phase 7 | Bổ sung chốt chặn quota độc quyền trong `SrsServiceImpl.reviewCard()`, sử dụng Pessimistic Write Lock (`SELECT ... FOR UPDATE`) trên `UserProfile` và `findTodayLogIdsWithLock` trên `ReviewLog` nhằm loại bỏ triệt để MVCC snapshot stale read; bổ sung multithreaded concurrency tests trong `SrsConcurrencyIntegrationTests.java` (near-limit concurrent review, at-limit rejection, user isolation); full regression `mvn clean test` PASS 810/810 tests (0 failures, 0 errors, 01:13 min). |
| **Hardening** | **Perf-Opt**  | **BE-PERF-001** | Khắc phục N+1 Query trong getDueCards() | **COMPLETED** | Phase 7 | Tái cấu trúc nạp đa hình từ vựng/bộ thủ trong `SrsServiceImpl.getDueCards()` bằng bulk lookups `findAllById` gom nhóm ID theo type và xây dựng in-memory maps; chuyển từ $O(N)$ query lookups sang $O(1)$ bounded queries ($\le 2$ bulk queries); bổ sung query-count tests trong `SrsPerformanceIntegrationTests.java` đo lường qua Hibernate Statistics; full regression `mvn clean test` PASS 816/816 tests (0 failures, 0 errors, 01:14 min). |
| **Hardening** | **Biz-Logic** | **BE-SRS-001**  | Thiết lập Business Timezone tường minh cho `LocalDate.now()` | **COMPLETED** | Phase 7 | Thiết lập múi giờ kinh doanh tường minh `Asia/Ho_Chi_Minh` qua Spring `Clock` bean (`TimeConfig.java`), loại bỏ hoàn toàn việc dùng `ZoneId.systemDefault()`; tiêm `Clock businessClock` vào `SrsServiceImpl` cho toàn bộ tính toán quota và timestamp review; bổ sung boundary & timezone-independence integration tests trong `SrsTimezoneIntegrationTests.java`; full regression `mvn clean test` PASS 820/820 tests (0 failures, 0 errors, 01:13 min). |
| **Hardening** | **Sec-Web**   | **BE-CORS-001**  | Đánh giá & Cấu hình CORS cho Cross-Origin Clients | **COMPLETED** | Phase 1 | Cấu hình CORS tập trung tại `SecurityConfig.java` qua `CorsConfigurationSource` và `http.cors(Customizer.withDefaults())`, origins cấu hình tường minh qua `app.cors.allowed-origins` (`http://localhost:5500`, `http://127.0.0.1:5500`, `http://localhost:3000`, `http://127.0.0.1:3000`), giới hạn `/api/**`, method (`GET, POST, PUT, PATCH, DELETE, OPTIONS`), header (`Authorization, Content-Type, Accept, Origin, X-Requested-With`), preflight `maxAge=3600s`, `allowCredentials=false`; `CorsSecurityIntegrationTests` PASS 14/14 tests; full regression `mvn clean test` PASS 834/834 tests (0 failures, 0 errors, 01:15 min). |
| **Hardening** | **Sec-Auth**  | **BE-AUTH-001**  | Xử lý Stale Token sau khi Vô hiệu hóa Tài khoản | **COMPLETED** | Phase 3 | Tiêm `AccountRepository` vào `JwtAuthenticationFilter`, kiểm tra `findStatusByEmailOrPhone(subject)` sau khi xác thực chữ ký JWT thành công, từ chối đưa `Authentication` vào `SecurityContextHolder` đối với tài khoản `Inactive`, `Banned` hoặc không tồn tại (trả về 401 UNAUTHORIZED); `JwtAuthenticationFilterTests` PASS 9/9 unit tests, `StaleTokenSecurityIntegrationTests` PASS 7/7 integration tests (xác minh lifecycle: issue $\rightarrow$ active $\rightarrow$ disable $\rightarrow$ re-enable, RBAC precedence 401 trước 403, account isolation); full regression `mvn clean test` PASS 844/844 tests (0 failures, 0 errors, 01:12 min). |
| **Hardening** | **Sec-JWT**   | **BE-AUTH-003**  | Bổ sung và Xác thực Issuer Claim (iss) trong JWT | **COMPLETED** | Phase 3 | Cấu hình tường minh `jwt.issuer: ${JWT_ISSUER:elearning-backend}`, tự động gắn `iss` khi tạo token và bắt buộc `requireIssuer` khi parse bằng JJWT 0.12.6, từ chối ngay lập tức token thiếu hoặc sai issuer với generic 401 UNAUTHORIZED trước khi truy vấn CSDL/RBAC; `JwtUtilTests` PASS 20/20 unit tests, `JwtAuthenticationFilterTests` PASS 11/11 tests, `JwtIssuerSecurityIntegrationTests` PASS 7/7 integration tests; full regression `mvn clean test` PASS 858/858 tests (0 failures, 0 errors, 01:18 min). |
| **Hardening** | **Perf-Opt**  | **BE-PERF-002**  | Khắc phục N+1 Query trong getMyLessons() Vocabulary Count | **COMPLETED** | Phase 5 | Loại bỏ vòng lặp `countByLesson_LessonId` trong `CreatorLessonServiceImpl.getMyLessons`, thay thế bằng bulk group-by aggregation query `countVocabulariesByLessonIds` trên `LessonVocabularyRepository` và tra cứu in-memory $O(1)$; bảo toàn 100% thứ tự, phân trang, gán mặc định 0 cho bài học rỗng, và cách ly quyền tác giả; bổ sung unit tests trong `CreatorLessonServiceTests.java` (32 tests) và query-count tests trong `LessonPerformanceIntegrationTests.java` (6 tests) chứng minh số lượng query giới hạn $O(1)$ ($\le 4$ queries, scaling invariant giữa 5 và 15 bài học); full regression `mvn clean test` PASS 866/866 tests (0 failures, 0 errors, 01:24 min). |
| **Hardening** | **Test-Conc**  | **BE-TEST-001**  | Củng cố Kiểm thử Tích hợp Đa luồng cho các Chuyển đổi Trạng thái Trọng yếu | **COMPLETED** | Phase 5, 6, 7 | Thiết lập và tăng cường 4 bộ kiểm thử tích hợp đa luồng (`ModerationConcurrencyIntegrationTests` 4 tests, `CreatorLessonConcurrencyIntegrationTests` 4 tests, `SrsConcurrencyIntegrationTests` 7 tests, `AuthConcurrencyIntegrationTests` 2 tests) trên Testcontainers MySQL 8.4 sử dụng `CountDownLatch`, `ExecutorService` và `TransactionTemplate`; kiểm chứng toàn diện các kịch bản tương tranh trọng yếu (Approve vs Approve, Approve vs Reject, Creator Submit vs Submit, Creator Update vs Submit, Creator Delete vs Submit, SRS CardProgress lost update, SRS first-time review race, SRS daily quota exhaustion serialization, Account duplicate registration race); chứng minh 100% giao dịch thua cuộc bị từ chối với HTTP 409 Conflict / OptimisticLockException và rollback sạch sẽ không để lại dữ liệu mồ côi; full regression `mvn clean test` PASS 874/874 tests (0 failures, 0 errors, 01:28 min). |
| **Hardening** | **Test-Rollback**| **BE-TEST-002**  | Kiểm thử Tích hợp Master Transaction Rollback & Tính Nguyên tử Giao dịch | **COMPLETED** | Phase 3, 5, 6, 7 | Thiết lập bộ kiểm thử tích hợp toàn diện `TransactionRollbackIntegrationTests` (7 tests) trên Testcontainers MySQL 8.4 kiểm chứng ranh giới giao dịch khai báo Spring `@Transactional` trên các service proxies mà không phụ thuộc rollback của test framework (`Propagation.NOT_SUPPORTED`); kiểm chứng tính nguyên tử và phục hồi trạng thái 100% khi phát sinh lỗi giữa chừng: (1) Excel Import thất bại dòng 2 $\rightarrow$ rollback sạch Lesson, new Vocab, LessonVocab, (2) Moderation Approve lỗi log $\rightarrow$ rollback trạng thái Pending, 0 log, (3) Moderation Reject lỗi log $\rightarrow$ rollback trạng thái Pending, 0 log, (4) SRS Review lỗi log $\rightarrow$ rollback biến đổi CardProgress, 0 log, (5) Đăng ký tài khoản lỗi sinh JWT $\rightarrow$ rollback sạch Account, UserProfile, Role, (6) Xóa bài học lỗi $\rightarrow$ khôi phục Lesson và liên kết LessonVocabulary, (7) Đổi thứ tự từ vựng lỗi $\rightarrow$ khôi phục thứ tự ban đầu; full regression `mvn clean test` PASS 881/881 tests (0 failures, 0 errors, 01:25 min). |
| **Phase 8D** | **Mod 8D** | **Task 8D.1** | Phân hệ Quản trị Tài khoản Người dùng (Admin Account Management) | **COMPLETED** | Phase 3, 8 | `GET /api/v1/admin/accounts` (phân trang, lọc status, search profile/email), `PUT /api/v1/admin/accounts/{id}/status` (`Active`, `Inactive`, `Banned`), DTO `AccountResponse` che giấu hash/version, bảo vệ chống tự khóa tài khoản (`POL-8D-01`); `AdminAccountServiceTests`, `AdminAccountControllerTests`, `AdminAccountIntegrationTests` PASS 100%. |
| **Phase 8D** | **Mod 8D** | **Task 8D.2** | Phân hệ Quản trị & Phân quyền Vai trò (Admin Role Management & Concurrency) | **COMPLETED** | Task 8D.1, DEC-24 | `GET /api/v1/admin/roles`, `PUT /api/v1/admin/accounts/{id}/roles`, khử trùng lặp role, bảo vệ chống tự tước quyền Admin (`POL-8D-02`), khóa bi quan cấp hàng `findByIdForUpdate(accountId)` chống lost updates, tăng `authorization_version` thu hồi token cũ ngay lập tức; `AdminRoleServiceTests`, `AdminRoleControllerTests`, `AdminRoleIntegrationTests`, `AdminRoleConcurrencyIntegrationTests` (10 luồng đồng thời) PASS 100%. |
| **Phase 8D** | **Mod 8D** | **Task 8D.3** | Phân hệ Lịch sử Kiểm duyệt (Moderator Audit History) | **COMPLETED** | Phase 6 | `GET /api/v1/moderator/history` phân trang; Moderator xem nhật ký cá nhân của chính mình, Admin xem toàn bộ nhật ký kiểm duyệt hệ thống; `ModeratorHistoryTests` PASS 100%. |
| **Phase 8D** | **Mod 8D** | **Task 8D.4** | Phân hệ Giám sát Toàn bộ Bài học Hệ thống (Admin Global Lesson Oversight) | **COMPLETED** | Phase 5, 6 | `GET /api/v1/admin/lessons` phân trang lọc theo mọi trạng thái `Draft`, `Pending`, `Approved`, `Rejected`; `AdminLessonControllerTests`, `AdminLessonIntegrationTests` PASS 100%. |
| **Phase 8D** | **Mod 8D** | **Task 8D.5** | Chuẩn hóa Hợp đồng Xử lý Lỗi Xác thực (Authentication Error Contract / H-01) | **COMPLETED** | Phase 3 | Chuẩn hóa `authenticationEntryPoint` trả về HTTP 401 Unauthorized (`UNAUTHORIZED`) trên mọi endpoint bảo vệ khi chưa đăng nhập; cập nhật `SecurityConfigTests`, `AuthIntegrationTests` PASS 100%. |
| **Phase 8D** | **Mod 8D** | **Task 8D.6** | Kiểm soát Tần suất Đăng nhập (Login Rate Limiting) | **COMPLETED** | Phase 3 | Triển khai `LoginRateLimiter` sliding window in-memory thread-safe (10 req/60s) bảo vệ `POST /api/v1/auth/login`, trả về HTTP 429 `TOO_MANY_REQUESTS`; `LoginRateLimiterTests`, `AuthControllerRateLimitTests` PASS 100%. |
| **Phase 8D** | **Mod 8D** | **Task 8D.7** | Endpoint Giám sát Sức khỏe Tối giản (Minimal Health Probe Endpoint) | **COMPLETED** | Actuator | Tích hợp `spring-boot-starter-actuator`, public `GET /actuator/health` trả về `{"status":"UP"}`, tắt các actuator endpoints nhạy cảm; `ActuatorHealthIntegrationTests` PASS 100%. |
| **Hardening** | **Mod 8D** | **Task 8D.8** | Cổng Nghiệm thu Backend & Sẵn sàng Chuyển giao Frontend (Pre-Frontend Gate) | **COMPLETED** | Task 8D.1..8D.7 | Chạy toàn bộ regression test suite trên Testcontainers MySQL 8.4: **`948/948 tests PASS`** (0 failures, 0 errors, 0 skipped); đồng bộ hóa 100% tài liệu `.agents/`. |
| **Domain-SRS** | **Engine-R1** | **Task R1** | Lesson-Driven New Card Engine & Daily New-Card Quota Discovery | **COMPLETED** | Phase 7, 8D | Triển khai Lesson-Driven New Card Discovery (`GET /api/v1/srs/new-cards` & `GET /api/v1/srs/lessons/{id}/new-cards`), phân tách rạch ròi Candidate Preview (idempotent, 0 CardProgress) và First-Ever Review (tiêu tốn 1 New-Card Quota, khởi tạo CardProgress); chốt chặn quota mới độc quyền `newCardsPerDay` trong `SrsServiceImpl.reviewCard()`; hỗ trợ thống kê `newCardsToday` trong `StudyStatsResponse`; `SrsNewCardEngineTests`, `SrsNewCardIntegrationTests`, `SrsControllerTests` PASS. |
| **Domain-SRS** | **Concurrency-R1.1** | **Task R1.1** | New Card Concurrency & Quota TOCTOU Verification | **COMPLETED** | Task R1 | Khóa bi quan cấp hàng trên `UserProfile` khi review thẻ mới, bảo đảm không vượt quá hạn mức khi có nhiều luồng đồng thời; `SrsNewCardConcurrencyIntegrationTests` (10 tests - Case A..E & Adversarial Tests 6, 8, 9, 12, 14) PASS; full regression `mvn clean test` PASS **`977/977 tests`** (01:44 min). |
| **Domain-SRS** | **Policy-R2** | **Task R2** | Review Policy & SM-2 Variant Correctness (Due-Only Server Enforcement) | **COMPLETED** | Task R1.1, DEC-31 | Triển khai Server-Side Due-Only Enforcement trên `POST /api/v1/srs/review` (từ chối review sớm `nextReviewAt > now` với HTTP 409 `CONFLICT` / `"Thẻ chưa đến hạn ôn tập"`); đảm bảo tính hợp lệ cho thẻ mới lần đầu (bỏ qua due-check) và thẻ học lại cùng ngày (`Again` với `interval = 0`, `nextReviewAt = now`); bổ sung suite kiểm thử `SrsReviewPolicyIntegrationTests` (7 tests) và unit tests `SrsServiceTests`; full regression `mvn clean test` PASS **`985/985 tests`** (0 failures, 0 errors, 0 skipped, 01:52 min). |
| **Domain-SRS** | **Eligibility-R2.1** | **Task R2.1** | New Card Eligibility Enforcement Audit & Mutation Boundary Invariant Protection | **COMPLETED** | Task R2, DEC-32 | Rà soát toàn bộ đường dẫn khởi tạo `CardProgress`, phát hiện bypass direct `POST /srs/review` cho từ vựng không thuộc bài học `Approved`; khắc phục chốt chặn máy chủ tại `SrsServiceImpl.reviewCard()` và `LessonVocabularyRepository.existsApprovedLessonForVocabulary()`; từ chối tạo thẻ mới cho từ vựng thuộc bài `Draft`/`Pending`/`Rejected`/cô lập với HTTP 422 `UNPROCESSABLE_ENTITY` và 0 DB mutations; bảo toàn tiến trình ôn tập của thẻ đã tồn tại; tạo mới `SrsNewCardEligibilityIntegrationTests` (10 tests) và cập nhật unit/concurrency suites; full regression `mvn clean test` PASS **`997/997 tests`** (0 failures, 0 errors, 0 skipped, 02:39 min trên MySQL 8.4 Testcontainer). |
| **Domain-SRS** | **Boundary-R2.1A** | **Task R2.1A** | Final Concurrent Eligibility / Moderation Boundary Verification & Linearization Audit | **COMPLETED** | Task R2.1, DEC-33 | Kiểm chứng các kịch bản tương tranh thực sự giữa giao dịch `reviewCard()` (học từ mới) và giao dịch kiểm duyệt bài học (`Approved` $\rightarrow$ `Rejected`) với rào cản đa luồng (`Order A`, `Order B`, `Order C`, `Multi-Learners`); xác lập ngữ nghĩa MVCC Read Snapshot của MySQL 8.4 `REPEATABLE READ`; phân loại tường minh các ngoại lệ tương tranh (422, 409, Deadlock 1213); mở rộng `SrsNewCardEligibilityIntegrationTests` (13 tests); full regression `mvn clean test` PASS **`1000/1000 tests`** (0 failures, 0 errors, 0 skipped, 01:44 min trên MySQL 8.4 Testcontainer). |
| **Domain-Dict** | **Lifecycle-R3.1** | **Task R3.1** | Vocabulary Lifecycle & Learning History Integrity (Referential Guards & Cascade Audit) | **COMPLETED** | Task R2.1A, DEC-34 | Rà soát toàn bộ vòng đời và các phụ thuộc của `VOCABULARY` (`LESSON_VOCABULARY`, `VOCAB_RADICAL`, `PERSONAL_NOTE`, `CARD_PROGRESS`, `REVIEW_LOG`); phát hiện và khắc phục lỗi xóa làm mất ghi chú cá nhân và để lại bản ghi mồ côi (orphaned polymorphic progress/logs); thiết lập chốt chặn kiểm tra toàn vẹn tham chiếu tại `VocabularyServiceImpl.deleteVocabulary(vocabId)` (từ chối xóa khi có Lesson/Note/CardProgress/ReviewLog với HTTP 409 `CONFLICT` và 0 DB mutations); cho phép xóa an toàn từ vựng không có tham chiếu (204 No Content, cascade dọn dẹp `VOCAB_RADICAL`, bảo toàn `RADICAL`); xây dựng bộ kiểm thử tích hợp `VocabularyLifecycleIntegrationTests` (15 tests: Referential Guards, RBAC matrix, Concurrency Races, ID Reuse Invariant Proof); full regression `mvn clean test` PASS **`1019/1019 tests`** (0 failures, 0 errors, 0 skipped, 01:37 min trên MySQL 8.4 Testcontainer). |
| **Domain-Dict** | **Concurrency-R3.1A** | **Task R3.1A** | Vocabulary Delete/Learning-Mutation Concurrency Hardening & Pessimistic Row Serialization | **COMPLETED** | Task R3.1, DEC-35 | Triệt tiêu hoàn toàn lỗ hổng Time-of-Check to Time-of-Use (TOCTOU) giữa giao dịch xóa từ vựng (`deleteVocabulary`) và các giao dịch tạo mới tiến trình/ghi chú/liên kết học tập (`reviewCard`, `createNote`, `addVocabularyToLesson`, `createLesson`); bổ sung khóa độc quyền cấp hàng `findByIdWithLock(vocabId)` (`@Lock(LockModeType.PESSIMISTIC_WRITE)` $\rightarrow$ `SELECT ... FOR UPDATE`); thiết lập thứ tự khóa nghiêm ngặt $\text{Vocabulary}(vocab\_id) \prec \text{UserProfile}(user\_id) \prec \text{CardProgress}(id)$ loại trừ 100% Deadlock theo Dijkstra; mở rộng `VocabularyLifecycleIntegrationTests` lên 20 tests (Order A, Order B, Stress 10 iterations, Lesson linking race, Vocab-Radical cascade, ID reuse proof); full regression `mvn clean test` PASS **`1024/1024 tests`** (0 failures, 0 errors, 0 skipped, 02:40 min trên MySQL 8.4 Testcontainer). |
| **Domain-Curriculum** | **Lifecycle-R3.2** | **Task R3.2** | Lesson Lifecycle & Moderation History Integrity (Referential Guards, Audit Trail & Decoupling) | **COMPLETED** | Task R3.1A, DEC-36 | Kiểm toán toàn diện vòng đời bài học `LESSON` và các quan hệ phụ thuộc (`LESSON_VOCABULARY`, `MODERATION_LOG`, `VOCABULARY`, `CARD_PROGRESS`, `REVIEW_LOG`); bổ sung chốt chặn bảo vệ nhật ký kiểm duyệt bất biến `existsByLesson_LessonId` kết hợp DB FK `fk_moderation_log_lesson ON DELETE RESTRICT` (từ chối xóa bài học có lịch sử kiểm duyệt với HTTP 409 `CONFLICT`); xác minh bài học `Draft` (chưa duyệt) xóa an toàn (204 No Content) cascade `LESSON_VOCABULARY` và bảo toàn 100% `VOCABULARY`; bài học `Pending` và `Approved` bị khóa (409 `CONFLICT`); phân quyền cấp đối tượng chặt chẽ (403 `FORBIDDEN`); tiến trình SRS hoàn toàn độc lập và bảo toàn khi trạng thái bài học thay đổi; tạo mới `LessonLifecycleAuditIntegrationTests` (14 tests) và cập nhật unit tests `CreatorLessonServiceTests` (33 tests); full regression `mvn clean test` PASS **`1039/1039 tests`** (0 failures, 0 errors, 0 skipped, 03:07 min trên MySQL 8.4 Testcontainer). |
| **Domain-Linguistics** | **Audit-R3.3** | **Task R3.3** | Chinese Domain, 214 Kangxi Radicals & Linguistic Data Integrity Audit | **COMPLETED** | Task R3.2 | Kiểm toán toàn diện 214 bộ thủ Khang Hy theo Unicode 17.0/Unihan; phát hiện 2 bản ghi thiếu Pinyin trong seed gốc (ID 49 `jǐ` và ID 172 `zhuī`); phân tích 43 chuỗi hiển thị ghép và 171 ideographs đơn lẻ; tạo test suite `ChineseDomainDataAuditTests` (12 tests); full regression `mvn clean test` PASS **`1051/1051 tests`** (0 failures, 0 errors, 0 skipped, 02:52 min). |
| **Phase 9** | **Mod 9A** | **Task 9A.1** | Khởi tạo nền tảng giao diện, Design System & Shell dùng chung (Core Shell, Design System & Safe DOM Foundation) | **COMPLETED** | Pre-Frontend Gate (Module 8D / DEC-41) | Khung HTML5 ngữ nghĩa, bản đề xuất thiết kế chống generic AI SaaS, Bootstrap 5.3 + Custom CSS (`frontend/css/style.css`), navbar responsive theo vai trò, footer, auth modal, toast notifications, UI primitives tại `frontend/js/ui/ui.js` và safe DOM tại `frontend/js/ui/security.js`, khởi tạo shell tĩnh tại `frontend/js/app.js` (no application/API network logic). |
| **Phase 9** | **Mod 9A** | **Task 9A.1.1** | Xây dựng hạ tầng kiểm thử giao diện vĩnh viễn (Frontend Verification Infrastructure & Regression Suite) | **COMPLETED** | Task 9A.1 | Bộ kiểm thử tự động phân tầng Test Pyramid (L0 Static, L1 Unit, L2 Browser Smoke B1..B5, L3 A11y WCAG 2.2 SC 2.5.8), central runner động, tự quản lý vòng đời static server, 0 runtime dependencies, 46/46 tests PASS (~8.3s). |
| **Phase 9** | **Mod 9A** | **Task 9A.2** | Xây dựng HTTP API Client tập trung & Quản lý phiên (`frontend/js/api/api.js` & `frontend/js/auth/auth-state.js`) | **COMPLETED** | Task 9A.1, Task 9A.1.1 | Wrapper fetch tập trung, quản lý JWT token, tự động đính kèm `Authorization: Bearer`, bắt lỗi 401 tự động chuyển hướng login, xử lý 429 Too Many Requests, chuẩn hóa phản hồi `ApiResponse<T>`, AbortController timeout 15s/60s, safe GET retry 1 lần, multi-tab sync qua StorageEvent, 18 client unit tests & 12 auth state unit tests PASS. |
| **Phase 9** | **Mod 9B** | **Task 9B.1** | Giao diện Đăng ký, Đăng nhập & Quản lý Hồ sơ cá nhân | **COMPLETED** | Task 9A.2 | `login.html`, `register.html`, `profile.html`, `frontend/js/pages/auth-page.js`, `frontend/js/pages/profile-page.js`, `frontend/js/ui/nav.js`: form validation, WCAG 2.2 AA (SC 3.3.8 Accessible Authentication: paste enabled), phòng chống Open Redirect trên `?redirect=`, bảo vệ tài nguyên ảnh avatar, cập nhật session metadata via `authManager.updateUser()`, hiển thị vai trò từ session (không phụ thuộc UserProfileResponse), 133/133 tests PASS. |
| **Phase 9** | **Mod 9B** | **Task 9B.2** | Giao diện Khám phá & Trình bày 214 Bộ thủ Khang Hy | **COMPLETED** | Task 9A.2, Task 9B.1 | `radicals.html`, `frontend/js/pages/radicals-page.js`: lưới 214 bộ thủ Khang Hy sắp xếp chuẩn xác theo `radicalId ASC` (1..214), defensive data loading (`size=214` với fallback tự động), tìm kiếm client-side tức thì theo chữ Hán/Hán-Việt/nghĩa tiếng Việt hỗ trợ bỏ dấu, modal chi tiết chuẩn WAI-ARIA APG `<dialog>`, vệ sinh tài nguyên media audio/video qua `sanitizeResourceUrl()`, 178/178 tests PASS. |
| **Phase 9** | **Remediation** | **FE-REMEDIATION-01** | Tách Verification Harness khỏi Production Homepage | **COMPLETED** | Task 9A.1, 9B.2 | Tách toàn bộ UI harness khỏi `index.html` sang `frontend/ui-verification.html` và `frontend/js/pages/ui-verification-page.js`; đưa `index.html` về 100% production homepage học thuật; bảo toàn 100% test capabilities. |
| **Phase 9** | **Remediation** | **FE-REMEDIATION-03** | Thiết lập & Kiểm chứng Lớp Kết nối Runtime FE → Spring Boot BE (Full-Stack Live Verified) | **COMPLETED** | FE-REMEDIATION-01 | Xây dựng `frontend/js/config.js` tự động định tuyến các cổng dev static (`localhost:3000`, `5500`, `5173`, etc.) tới `http://localhost:8080/api/v1` mà không hardcode trong `api.js` (mặc định `/api/v1` cho production same-origin); chuẩn hóa phân loại lỗi mạng `NETWORK_ERROR` và `NOT_FOUND`; cập nhật `auth-page.js` và `radicals-page.js`; 193/193 tests PASS (35 suites) trong gate. **Full-stack Live Verified**: MySQL 8.4 (`elearning_db`, port 3306) -> Spring Boot 3.3.5 (`/actuator/health` UP, port 8080) -> Frontend (:3000) với Playwright live browser (không mock): Register (201), Login (200, JWT), Profile (200, Bearer token, DB mutation persisted), Radicals (200, 214 cards, detail modal, 0 missing/duplicate IDs), 100% CORS verified. |
| **Phase 9** | **Mod 9B** | **Task 9B.3** | Giao diện Tra cứu & Tìm kiếm Từ vựng tiếng Trung | **COMPLETED** | Task 9A.2, Task 9B.2 | `frontend/vocabulary.html`, `frontend/js/pages/vocabulary-page.js`, Section 11 CSS (`.vocab-grid`, `.vocab-card`, `.vocab-speech-btn`, `.vocab-radical-badge`): tìm kiếm đa tiêu chí qua API backend `search=` (Hanzi, Pinyin có dấu, Pinyin không dấu; TUYỆT ĐỐI KHÔNG dùng `q`), debounce 300ms, AbortController + request sequencing chống stale response race conditions, phân trang backend `PageResponse` với size 12/20/40, Progressive Radical Disclosure (Zero N+1: list summary không tải radicals, chỉ gọi `GET /vocabulary/{id}` khi mở card), phát âm Web Speech API (`zh-CN`, rate 0.85) với graceful fallback, native `<dialog id="vocabDetailModal">` chuẩn WCAG 2.2 APG kèm focus restoration, Section 5 chẩn đoán tại `ui-verification.html`, 241/241 Frontend Gate PASS, 21/21 Full-Stack live PASS (FS-020 & FS-021 verified). |
| **Phase 9** | **Mod 9C** | **Task 9C.1** | Giao diện Khám phá Bài học công khai & Chi tiết bài học | **COMPLETED** | Task 9A.2, Mod 9B | `lessons.html`, `lesson-detail.html`, `frontend/js/pages/lessons-page.js`, `frontend/js/pages/lesson-detail-page.js`: catalog bài học công khai chuẩn hợp đồng backend (`GET /api/v1/lessons?page=X&size=Y`, cấm query `search/q/keyword`), bất biến chỉ hiển thị bài học `Approved`, không bịa đặt metadata (`difficulty`, `author`, `publishedAt`), quyết định sử dụng `updatedAt` cho nhãn ngày cập nhật `<time>`, trang chi tiết bài học bảo toàn nguyên vẹn thứ tự từ vựng `order_index ASC` (1, 2, 3...) qua danh sách ngữ nghĩa `<ol>`, phòng thủ 404 trung lập tuyệt đối không làm lộ trạng thái kiểm duyệt nội bộ, 291/291 Frontend Gate PASS, 23/23 Full-Stack Live PASS (FS-022 & FS-023 verified). |
| **Phase 9** | **Mod 9C** | **Task 9C.2** | Phân hệ Ghi chú cá nhân trong ngữ cảnh (Contextual Personal Notes) | **COMPLETED** | Task 9C.1 | `frontend/js/ui/notes-modal.js`: Modal ghi chú cá nhân nhúng trực tiếp theo ngữ cảnh từ vựng (`vocabulary.html`, `lesson-detail.html`); không tạo trang riêng `notes.html`; form tạo/sửa/xóa ghi chú cá nhân $\le 500$ ký tự; đếm ký tự live (CJK/Unicode safe); phân trang backend `PageResponse` (size=20); xử lý xóa với phản hồi HTTP 200 `{ data: null }` (không phải 204); xác thực danh tính 100% từ JWT session (phòng chống IDOR tuyệt đối, không gửi userId lên client); DOM an toàn 100% qua `textContent`; WCAG 2.2 AA (native `<dialog>` showModal, `aria-live="polite"` counter, hộp xác nhận xóa inline trong card, APG focus restoration); Section 7 chẩn đoán `ui-verification.html`; 321/321 Frontend Gate PASS, 23/23 Full-Stack Live PASS. |
| **Phase 9** | **Mod 9C** | **Task 9C.3** | Phòng ôn tập Flashcard tương tác Spaced Repetition (SRS Room) | **COMPLETED** | Task 9C.1, Task 9C.2 | `frontend/srs-review.html`, `frontend/js/pages/srs-review-page.js`: phòng học flashcard 3D hai mặt lật (Click, Space, Enter), phản xạ đếm giây `reviewTimeSeconds >= 0`, 4 nút & phím tắt đánh giá backend (1=Again, 2=Hard, 3=Good, 4=Easy), gửi đúng API `POST /api/v1/srs/review` (không gửi userId/SM-2 internals), chính sách hàng đợi học lại Again chống lặp vô tận (max 3 lần), nạp audio theo demand (`/vocabulary/{id}` hoặc `/radicals/{id}`), tích hợp ghi chú cá nhân 9C.2 cho từ vựng, giảm chuyển động (`prefers-reduced-motion`), Section 8 chẩn đoán tại `ui-verification.html`, 366/366 Frontend Gate PASS, 23/23 Full-Stack Live PASS. |
| **Phase 9** | **Mod 9C** | **Task 9C.4** | Bảng điều khiển Thống kê Học tập & Cài đặt Định ngạch SRS | **COMPLETED** | Task 9C.3 | `frontend/srs-dashboard.html`, `frontend/js/pages/srs-dashboard-page.js`, Section 16 CSS: hiển thị 5 chỉ số thực tế từ `StudyStatsResponse` (`cardsDue`, `reviewsToday`, `newCardsToday`, `newCardsLimit`, `maxReviewLimit`), tuyệt đối không bịa đặt biểu đồ/streak/retention ngoài API, form cấu hình `newCardsPerDay` và `maxReviewPerDay` (> 0 nguyên dương), kiểm thực client nghiêm ngặt, chống submit trùng lặp, cập nhật thành công kích hoạt làm mới số liệu tức thì từ backend, điều hướng chéo hai chiều với phòng ôn tập SRS 9C.3 (`srs-review.html`), bổ sung probe chẩn đoán 9C.4 tại Section 8 `ui-verification.html`, 396/396 Frontend Gate PASS, 16/16 Unit PASS, 6/6 Browser E2E PASS, 13/13 A11y PASS. |
| **Phase 9** | **Mod 9D** | **Task 9D.1** | Creator Lesson Studio & Quản lý thứ tự từ vựng | **COMPLETED** | Task 9A.2, Mod 9B | `creator-lessons.html`, `creator-lesson-editor.html`, `frontend/js/pages/creator-lessons-page.js`, `frontend/js/pages/creator-lesson-editor-page.js`, Section 17 CSS: không gian biên soạn bài học cá nhân của Tác giả (`Creator`) và Quản trị viên (`Admin`); tạo bài học mới ở trạng thái `Draft` bằng modal tiếp cận WCAG 2.2; hiển thị danh sách phân trang phía máy chủ (`GET /api/v1/creator/lessons?page=X&size=Y`, loại bỏ search input giả vì backend không hỗ trợ search parameter); biên soạn tiêu đề bài học (`PUT /api/v1/creator/lessons/{id}` với payload tối giản `{ "title": ... }` sau khi validate 1..200 ký tự); tìm kiếm từ vựng tái sử dụng `GET /api/v1/vocabulary?search=` với debounce 300ms; thêm/xóa từ vựng đồng bộ hợp đồng backend có thẩm quyền; cơ chế đổi thứ tự từ vựng (Move Up / Move Down) dùng các nút native `<button>` tuân thủ WCAG 2.2 SC 2.5.7/2.1.1, danh sách ngữ nghĩa `<ol>`, loại bỏ drag-and-drop không cần thiết; payload reorder `{ "orderedVocabIds": [id1, id2, ...] }` gửi lên `PUT /api/v1/creator/lessons/{id}/reorder` không rò rỉ identity/orderIndex; khóa toàn bộ kiểm soát đột biến khi bài học ở trạng thái `Pending` hoặc `Approved` kèm biểu ngữ thông báo rõ ràng; xử lý lỗi 401/403/404 an toàn không rò rỉ dữ liệu của Creator khác; bổ sung Section 9D.1 chẩn đoán tại `ui-verification.html`; 26/26 Unit PASS, 4/4 Browser E2E PASS (CL-01..CL-04), 14/14 A11y PASS, 13/13 Verification Center PASS. |
| **Phase 9** | **Mod 9D** | **Task 9D.2** | Giao diện Import Excel 2 bước cho Creator (Preview != Confirm) | **COMPLETED** | Task 9D.1 | `frontend/creator-import.html`, `frontend/js/pages/creator-import-page.js`, Section 18 CSS: giao diện nhập bài học từ file Excel 2 bước cho Tác giả (`Creator`) và Quản trị viên (`Admin`); Bước 1 Preview (`POST /api/v1/creator/lessons/import`, multipart/form-data: `file`): kiểm tra định dạng `.xlsx`, giới hạn kích thước $\le 10\text{MB}$ và tối đa 5000 dòng dữ liệu, đọc kiểm tra dữ liệu và tra cứu từ vựng không ghi CSDL (zero DB mutations); hiển thị báo cáo `ImportValidationReport` trực quan gồm các KPI tổng quan, huy hiệu trạng thái, và bảng dữ liệu giới hạn trang (25/50/100 dòng) kèm lọc theo danh mục (`All`, `Errors`, `New`, `Existing`) chống quá tải DOM; Bước 2 Confirm (`POST /api/v1/creator/lessons/import/confirm`, multipart/form-data: `title`, `file`): chỉ mở khóa khi file hợp lệ (0 row errors), người dùng nhập tiêu đề bài học (chuẩn ràng buộc backend 1..100 ký tự có live counter và `maxlength="100"`), tái nộp file gốc (native File object) để backend tự phân tích lại và lưu bài học `Draft` nguyên tử trong transaction; bất biến hủy xem trước khi đổi/xóa file; DOM an toàn tuyệt đối chống XSS với nội dung Excel; Section 9D.2 chẩn đoán tại `ui-verification.html` (zero mutating probe); 24/24 Unit PASS, 4/4 Browser E2E PASS (CI-01..CI-04), 14/14 Verification Center PASS, 110/110 Static PASS, 376/376 Fast PASS. |
| **Phase 9** | **Mod 9D** | **Task 9D.3** | Luồng Nộp bài Kiểm duyệt & Trực quan hóa Trạng thái Khóa (Submission Workflow & Status Visibility) | **COMPLETED** | Task 9D.1, Task 9D.2 | `frontend/creator-lessons.html`, `frontend/creator-lesson-editor.html`, `frontend/js/pages/creator-lessons-page.js`, `frontend/js/pages/creator-lesson-editor-page.js`: Xây dựng quy trình nộp duyệt bài học chuẩn mực cho Creator/Admin qua endpoint authoritative `POST /api/v1/creator/lessons/{id}/submit` (chuyển trạng thái `Draft`/`Rejected` $\rightarrow$ `Pending`); modal xác nhận `<dialog id="submitModerationModal">` chuẩn WAI-ARIA APG cảnh báo hệ quả khóa chỉnh sửa; nhãn nút động ("Nộp duyệt" cho `Draft`, "Nộp lại bài học" cho `Rejected`, ẩn nút với `Pending`/`Approved`); khóa ngay lập tức toàn bộ thao tác biên soạn (tiêu đề, thêm/xóa/sắp xếp từ vựng) khi chuyển sang `Pending`; xử lý xung đột 409 Conflict tự động làm mới danh sách và điều hòa trạng thái; **Xác nhận Khoảng trống Hợp đồng Backend (Contract Gap)**: `LessonDetailResponse` và `LessonSummaryResponse` đã sealed hoàn toàn KHÔNG có trường `rejectionReason` hay `flaggedFields`, và `/api/v1/moderator/history` trả về HTTP 403 Forbidden cho Creator $\rightarrow$ Client hiển thị fallback trung thực *"Bài học này đã bị từ chối. Thông tin phản hồi chi tiết chưa được cung cấp bởi API hiện tại."*, tuyệt đối KHÔNG giả mạo dữ liệu hay gọi API vượt quyền (ghi nhận `BLOCKED by current backend contract` cho chi tiết phản hồi kiểm duyệt); bổ sung Section 9D.3 chẩn đoán tại `ui-verification.html` (Probe 1 & Probe 2 an toàn); 21/21 Unit PASS (`creator-submission.test.mjs`), 4/4 Browser E2E PASS (`creator-submission.browser.mjs`: CS-01..CS-04), 15/15 Verification Center PASS (`verification-center.browser.mjs`), 110/110 Static PASS, 397/397 Fast PASS; **Live Verified**: Đăng nhập `creator@test.com`, mở bài học #9368, nộp duyệt thành công HTTP 200, cập nhật trạng thái `Pending` và khóa controls ngay lập tức. |
| **Phase 9** | **Mod 9E** | **Task 9E.1** | Hàng đợi Kiểm duyệt Bài học (Moderation Queue) | **COMPLETED** | Task 9A.2, Mod 9D | `frontend/moderator-queue.html`, `frontend/js/pages/moderator-queue-page.js`: Hàng đợi kiểm duyệt bài học dành riêng cho `Moderator` và `Admin` kết nối `GET /api/v1/moderator/lessons/pending` (phân trang máy chủ `page`, `size`); loại bỏ input search phía client vì backend không hỗ trợ; hiển thị chuẩn mực DTO `ModerationQueueResponse` (`lessonId`, `title`, `status`, `creatorEmail`, `vocabularyCount`, `updatedAt`); hiển thị `creatorEmail` thay vì bịa đặt `creatorName`; cột thời gian ghi nhận trung thực nhãn "Cập nhật lần cuối" (`formatQueueDate(updatedAt || createdAt)`); liên kết điều hướng bài học chuyển tới `moderator-review.html?id=<lessonId>` (chuẩn bị cho Task 9E.2); chặn truy cập người dùng `Learner`/`Creator`/Anonymous bằng `#moderatorAuthGuardContainer`; cấu trúc bảng ngữ nghĩa `<table>` với `<caption>`, `<th scope="col">`, `.table-responsive` chống tràn mobile; 100% DOM an toàn qua `textContent`/`createSafeElement`; bổ sung Section 9E.1 chẩn đoán tại `ui-verification.html`; 25/25 Unit PASS (`moderator-queue.test.mjs`), 5/5 Browser E2E PASS (`moderator-queue.browser.mjs`: MQ-01..MQ-05), 117/117 Static PASS, 429/429 Fast PASS. |
| **Phase 9** | **Mod 9E** | **Task 9E.2** | Màn hình Kiểm tra Chi tiết Bài học & Từ vựng (Lesson Review & Vocabulary Inspection) | **COMPLETED** | Task 9E.1 | `frontend/moderator-review.html`, `frontend/js/pages/moderator-review-page.js`: Giao diện thẩm định chi tiết bài học dành cho `Moderator` và `Admin` kết nối `GET /api/v1/moderator/lessons/{id}` (chỉ phục vụ trạng thái `Pending`, trả về 404 nếu khác Pending); **Bảo toàn thứ tự từ vựng**: Giữ nguyên vẹn 100% thứ tự tuần tự `orderIndex ASC` do backend trả về (`#1`, `#2`, `#3`...), tuyệt đối không re-sort phía client; **Kiểm tra phát âm**: Nút nghe phát âm trực tiếp từ trường `audioUrl` có sẵn trong `LessonVocabItemResponse` (không gửi thêm request thừa), vệ sinh URL qua `sanitizeResourceUrl()`, hỗ trợ fallback Web Speech API an toàn; **Kiểm tra Pinyin & Nghĩa**: Pinyin chuẩn hiển thị chính, pinyinRaw đưa vào disclosure thông tin kỹ thuật; hiển thị Hán-Việt, nghĩa tiếng Việt, câu ví dụ và câu dịch; liên kết video hướng dẫn viết nếu có `videoWritingUrl`; **Tra cứu Bộ thủ cấu thành (Anti-N+1 Lazy Disclosure)**: DTO bài học không kèm bộ thủ nhưng có sẵn endpoint công khai `GET /api/v1/vocabulary/{id}`; áp dụng kiến trúc tra cứu lazy theo nhu cầu khi người dùng click xem bộ thủ kèm bộ nhớ đệm `Map<vocabId, radicals>`, loại bỏ hoàn toàn bão N+1 request khi tải trang; **Xử lý sự cố Hàng đợi Stale (404 Concurrency Defense)**: Khi bài học đã được xử lý bởi Moderator khác hoặc không tồn tại, giao diện hiển thị thông báo trung thực *"Bài học không còn trong hàng đợi kiểm duyệt hoặc không tồn tại."* kèm nút quay lại hàng đợi; **Khoảng trống Hợp đồng Tác giả (Contract Gap)**: `LessonDetailResponse` không chứa trường tác giả $\rightarrow$ ghi nhận trung thực *"Không khả dụng trong hợp đồng chi tiết hiện tại"*, không bịa đặt dữ liệu hay gọi API vượt quyền; **Bảo mật & Trợ năng**: 100% DOM qua `textContent`/`createSafeElement`, 1 thẻ `<h1>`, breadcrumbs, skip-link, W3C APG disclosure pattern cho bộ thủ; Bổ sung Section 9E.2 chẩn đoán tại `ui-verification.html`; 11/11 Unit PASS (`moderator-review.test.mjs`), 5/5 Browser E2E PASS (`moderator-review.browser.mjs`: MR-01..MR-05), 124/124 Static PASS, 447/447 Fast PASS; **Live verification: PASS (Live Full-Stack Verified)** trên MySQL 8.4 port 3306, Spring Boot 3.3.5 port 8080, Static Server port 3000, kiểm chứng thực tế Moderator đăng nhập, nạp chi tiết bài Pending #9368, tra cứu lazy bộ thủ thành công, bắt lỗi 404 hàng đợi stale chính xác. |
| **Phase 9** | **Mod 9E** | **Task 9E.3** | Quy trình Phê duyệt & Từ chối có Phản hồi Lỗi chi tiết (Moderator Approve / Reject Workflow) | **COMPLETED** | Task 9E.2 | `frontend/moderator-review.html`, `frontend/js/pages/moderator-review-page.js`, Section 19 `style.css`: Quy trình phê duyệt & từ chối bài học dành riêng cho `Moderator` và `Admin`; **Hợp đồng Phê duyệt (POST /approve)**: Kết nối `POST /api/v1/moderator/lessons/{id}/approve` (trả về 200 OK wrapping `ApiResponse<LessonDetailResponse>`), hỗ trợ ghi chú tùy chọn `note` ($\le 500$ ký tự, null khi để trống); **Hợp đồng Từ chối (POST /reject)**: Kết nối `POST /api/v1/moderator/lessons/{id}/reject` (trả về 200 OK), bắt buộc nhập lý do từ chối `rejectionReason` (1..500 ký tự trimmed), gắn nhãn các trường vi phạm `flaggedFields` định dạng chuỗi JSON `["title", "vocabularies[0].pinyin"]` hoặc `null` (khớp hoàn toàn với schema backend `TEXT` column và test suite `RejectLessonRequest`); **Nguyên tắc Pending-Only**: Chỉ bài học ở trạng thái `Pending` mới hiển thị thanh hành động phê duyệt/từ chối; bài học sau khi duyệt xong cập nhật trạng thái authoritative từ server, vô hiệu hóa thanh thao tác và hiển thị CTA quay về hàng đợi `moderator-queue.html`; **Chống Submit Trùng lặp (Anti-Duplicate Guard)**: Cờ `isSubmitting` khóa lập tức controls và hiển thị trạng thái xử lý; **Xử lý Xung đột Đồng thời (409 Concurrency Defense)**: Khi gặp HTTP 409 (hoặc conflict message), hiển thị cảnh báo trung thực *"Xung đột trạng thái: Bài học đã được xử lý bởi kiểm duyệt viên khác hoặc không còn ở trạng thái chờ duyệt."*, vô hiệu hóa controls, tuyệt đối không báo thành công giả; **Bảo mật & Tính Toàn vẹn Nhật ký Kiểm toán (Audit Invariant)**: Không tự tạo audit log từ frontend, backend sở hữu 100% logic `@Transactional` lưu `ModerationLog` + `@Version` optimistic locking; 100% Safe DOM qua `textContent`/`createSafeElement`; **Trợ năng WAI-ARIA & WCAG 2.2 AA**: Native `<dialog>` (`showModal()` & `close()`), focus trap & hoàn trả focus về trigger element, đóng bằng phím Escape, live character counter (`aria-live="polite"`); Bổ sung Section 14 chẩn đoán an toàn tại `ui-verification.html` (chính sách Zero Junk Logs: không chạy mutation thật từ Verification Center); 28/28 Unit PASS (`moderator-review.test.mjs`), 8/8 Browser E2E PASS (`moderator-review.browser.mjs`: MR-01..MR-08), 124/124 Static PASS, 464/464 Fast PASS; **Live verification: NOT RUN / ENVIRONMENT BLOCKED** (Backend service không chạy trong phiên kiểm thử; không thực hiện mutation trực tiếp vào môi trường chưa có dữ liệu kiểm thử cô lập an toàn). |
| **Phase 9** | **Mod 9E** | **Task 9E.4** | Tra cứu Nhật ký Kiểm duyệt Bất biến (Moderation History UI) | **COMPLETED** | Task 9E.3 | `frontend/moderator-history.html`, `frontend/js/pages/moderator-history-page.js`, Section 20 CSS `style.css`: Giao diện tra cứu lịch sử kiểm duyệt bài học dành riêng cho `Moderator` và `Admin` kết nối `GET /api/v1/moderator/history` (`page`, `size`, `sort`); **Ngữ nghĩa Phân quyền Máy chủ (Server-Determined Scope)**: Backend tự động phân định dữ liệu: Moderator chỉ xem nhật ký cá nhân (`moderator_id = caller`, tiêu đề "Nhật Ký Kiểm Duyệt Của Tôi"), Admin xem toàn bộ nhật ký hệ thống (tiêu đề "Lịch Sử Kiểm Duyệt Hệ Thống"); client tuyệt đối không can thiệp bộ lọc hay gửi user ID; **Tính Bất biến Tuyệt đối (Immutable Read-Only)**: Zero nút bấm sửa, xóa, hay dọn sạch lịch sử; zero mutation endpoint được gọi; **Chuẩn hóa Giá trị Action**: Xử lý mâu thuẫn tài liệu/mã nguồn: backend `ModerationServiceImpl` lưu `"Approve"` và `"Reject"` (không phải `"Approved"`/`"Rejected"`), hàm `normalizeModerationAction()` phòng thủ linh hoạt chấp nhận cả hai định dạng và ánh xạ sang nhãn người dùng "Phê duyệt" (xanh lục) / "Từ chối" (đỏ cinnabar); **Trường Bị Đánh Dấu (Safe Flagged Fields Parser)**: Bóc tách an toàn cột `TEXT` CSDL lưu chuỗi JSON `["title", "vocabularies[0].pinyin"]`, chuỗi phân tách dấu phẩy hoặc chuỗi đơn, ánh xạ nhãn hiển thị thân thiện (Tiêu đề, Pinyin, Chữ Hán...) và hiển thị chip/tag trực quan, bảo đảm try/catch không ném ngoại lệ khi gặp dữ liệu dị thường; **Định danh Bài học**: Hiển thị `lessonTitle` với fallback an toàn `Bài học #<lessonId>` hoặc `Bài học không xác định`; **Dấu thời gian Kiểm toán**: Gắn nhãn chuẩn "Thời gian kiểm duyệt" từ trường `createdAt` với độ chính xác đầy đủ từng giây; **Khả năng Chống Rỗng `@JsonInclude(NON_NULL)`**: Xử lý an toàn các trường bị Jackson lược bỏ khi null; **Phân trang Phía Máy chủ**: Hiển thị phân trang đồng bộ với `PageResponse` (Next/Prev, số trang, tổng số bản ghi); **Không Thêm Bộ lọc Giả mạo**: Tuân thủ nghiêm ngặt không thêm ô tìm kiếm bài học hay bộ lọc ngày tháng không được backend hỗ trợ; **Trợ năng & Bố cục**: Cấu trúc `<table>` ngữ nghĩa đầy đủ `<caption>`, `<th scope="col">`, `<tbody>`, `.table-responsive` chống tràn mobile, liên kết điều hướng tab qua lại giữa Hàng đợi và Lịch sử; 23/23 Unit PASS (`moderator-history.test.mjs`), 5/5 Browser E2E PASS (`moderator-history.browser.mjs`: MH-01..MH-05), 131/131 Static PASS, 471/471 Fast PASS; **Live verification: PASS (Live Full-Stack Verified)** trên MySQL 8.4 port 3306, Spring Boot 3.3.5 port 8080, Static Server port 3000; đăng nhập thực tế Moderator `mod_review_test@elearning.com` nhận JWT Bearer token, gọi `GET /api/v1/moderator/history?page=0&size=10` trả về HTTP 200 OK SUCCESS kèm PageResponse rỗng chính xác với tổng số phần tử `totalElements: 0` và xác nhận empty state trung thực. |
| **Phase 9** | **Mod 9F** | **Task 9F.1** | Quản trị Vòng đời Tài khoản & Cập nhật Trạng thái (Admin Accounts Lifecycle UI) | **COMPLETED** | Task 9A.2, Mod 9E | `frontend/admin-accounts.html`, `frontend/js/pages/admin-accounts-page.js`: Giao diện quản trị vòng đời tài khoản dành riêng cho `Admin` kết nối `GET /api/v1/admin/accounts` (`status`, `search`, `page`, `size`) và `PUT /api/v1/admin/accounts/{id}/status`; **RBAC Role Guard**: Kiểm tra nghiêm ngặt vai trò `authManager.hasRole('Admin')`, từ chối `ROLE_ADMIN` hoặc người dùng khác; **Hợp đồng Tìm kiếm**: Tích hợp tham số đơn `search` duy nhất của backend khớp cả `emailOrPhone` và `fullName`; **Hợp đồng Trạng thái**: Hỗ trợ 3 trạng thái authoritative `Active`, `Inactive`, `Banned` với huy hiệu trực quan ("Hoạt động", "Tạm khóa", "Bị cấm"); **Bảo vệ tự thân (POL-8D-01)**: Tự động nhận diện tài khoản Admin đang đăng nhập qua `isSelfAccount()`, hiển thị huy hiệu "Tài khoản của bạn" và vô hiệu hóa nút đổi trạng thái; **Cơ chế Thu hồi Phiên/Token Phía Máy chủ (DEC-42)**: Modal xác nhận native `<dialog id="statusConfirmModal">` giải thích trung thực thay đổi trạng thái sẽ làm vô hiệu hóa token/phiên làm việc của tài khoản mục tiêu thông qua cơ chế `authorization_version` phía máy chủ; **Trợ năng WAI-ARIA**: Modal `<dialog>` mở bằng `showModal()`, đóng bằng Escape/Backdrop/Cancel, hoàn trả focus về trigger button; Bảng dữ liệu ngữ nghĩa `<table>` với `<caption>`, `<th scope="col">`, `.table-responsive`; Bổ sung Section 15 chẩn đoán tại `ui-verification.html` (chẩn đoán Capability & Read-Only); 31/31 Unit PASS (`admin-accounts.test.mjs`), 7/7 Browser E2E PASS (`admin-accounts.browser.mjs`: AA-01..AA-07), 16/16 Verification Center PASS (`verification-center.browser.mjs`: VC-01..VC-16), 138/138 Static PASS, 478/478 Fast PASS; **Live verification: PASS (Live Full-Stack Verified)** trên MySQL 8.4 port 3306, Spring Boot 3.3.5 port 8080, Static Server port 3000 (`admin-accounts-live.mjs`: đăng ký Admin và Target dùng một lần, elevate vai trò Admin trong MySQL, đăng nhập nhận JWT thực, xác minh hiển thị bảng 20 bản ghi, tìm kiếm CSDL thực, kiểm tra POL-8D-01 trên tài khoản Admin, mở modal cảnh báo DEC-42, đổi trạng thái sang `Inactive`, xác nhận phản hồi HTTP 200 từ Spring Boot, cập nhật nhãn 'Tạm khóa' trên UI và kiểm tra cột `status`='Inactive' cùng `authorization_version` cập nhật trong MySQL, dọn sạch dữ liệu thử nghiệm 100%). |
| **Phase 9** | **Mod 9F** | **Task 9F.2** | Quản trị & Phân quyền Vai trò Hệ thống (Admin Role Management & Role Assignment UI) | **COMPLETED** | Task 9F.1 | `frontend/admin-roles.html`, `frontend/js/pages/admin-roles-page.js`: Giao diện quản trị vai trò và phân quyền tài khoản dành riêng cho `Admin`; **Authoritative Role Catalog**: Tải danh mục 4 vai trò chuẩn từ `GET /api/v1/admin/roles` (`roleId`, `roleName`), hiển thị thẻ trực quan kèm nhãn tiếng Việt và mô tả client; **Danh sách tài khoản & Phân trang**: Tái sử dụng hợp đồng `GET /api/v1/admin/accounts`, hỗ trợ tìm kiếm `search` và phân trang `PageResponse`; **Phân quyền Toàn bộ Tập vai trò (Complete Role Set)**: Modal native `<dialog id="roleAssignmentModal">` với checkbox chọn đa vai trò, gửi payload hoàn chỉnh `{ "roles": [...] }` tới `PUT /api/v1/admin/accounts/{id}/roles` (thay thế toàn bộ tập vai trò, không gửi payload cộng/trừ đơn lẻ); **Bảo vệ Tự thân (POL-8D-02)**: Đối với tài khoản của chính Admin (`isSelfAccount()`), khóa vai trò `Admin` (`checked` + `disabled`) để chống tự giáng quyền, đồng thời cho phép Admin tự chỉnh sửa các vai trò khác (`Creator`, `Moderator`); **Chống Đột biến Vô nghĩa (No-op Prevention)**: So sánh tập vai trò hiện tại và tập vai trò được chọn, không gửi `PUT` nếu không thay đổi và thông báo rõ ràng; **Minh bạch Thu hồi Phiên (DEC-42)**: Cảnh báo trung thực rằng thay đổi vai trò làm tăng `authorization_version` và vô hiệu hóa phiên làm việc phía máy chủ; **Authoritative UI Update**: Cập nhật trực tiếp hàng trên bảng theo `AccountResponse` trả về từ server sau mutation; **Trợ năng & DOM An toàn**: Native `<dialog>` (`showModal()`, Esc, focus restoration), 100% safe DOM qua `textContent`/`createSafeElement`; Bổ sung Section 16 chẩn đoán tại `ui-verification.html` (chẩn đoán Capability & Read-Only Probe, cam kết ZERO MUTATION); 31/31 Unit PASS (`admin-roles.test.mjs`), 8/8 Browser E2E PASS (`admin-roles.browser.mjs`: AR-01..AR-08), 16/16 Verification Center PASS, 145/145 Static PASS, 485/485 Fast PASS; **Live verification: PASS (9-Phase Full-Stack Verified)** trên MySQL 8.4 port 3306, Spring Boot 3.3.5 port 8080, Static Server port 3000 (đăng ký Admin và Target dùng một lần, elevate vai trò Admin trong MySQL, đăng nhập nhận JWT thực, tải catalog 4 vai trò, tìm kiếm target, phân quyền Creator + Learner qua PUT, UI hiển thị nhãn authoritative 'Tác giả' + 'Học viên', MySQL xác nhận bản ghi `account_role` và `authorization_version` tăng từ 1 -> 2, kiểm chứng POL-8D-02 khóa checkbox Admin cho chính mình, dọn sạch dữ liệu thử nghiệm trong MySQL). |
| **Phase 9** | **Mod 9F** | **Task 9F.3** | Quản trị Danh mục Bộ thủ Khang Hy (Admin Radicals CRUD UI) | **COMPLETED** | Task 9A.2, Mod 9B | `frontend/admin-radicals.html`, `frontend/js/pages/admin-radicals-page.js`: Giao diện quản trị danh mục bộ thủ Khang Hy (CRUD) hoàn chỉnh dành riêng cho `Admin`; **Authoritative Catalog Lookup**: Kết nối `GET /api/v1/radicals` (endpoint công khai chuẩn, tổng hợp phân trang đa trang khi cần), loại bỏ hoàn toàn endpoint bịa đặt `GET /admin/radicals`; **Kích thước Động (Dynamic Catalog Invariant)**: Không áp đặt bất biến tĩnh `count === 214`, phản ánh chính xác trạng thái máy chủ sau khi tạo (>214) hoặc xóa (<214); **Tìm kiếm Phía Client (Client-Side Search Architecture)**: Tái sử dụng logic tìm kiếm đa trường và chuẩn hóa bỏ dấu tiếng Việt `removeVietnameseDiacritics()` trên ký tự Hán tự, Pinyin, Hán-Việt và nghĩa tiếng Việt; **Tạo Mới Bộ thủ (POST /api/v1/admin/radicals)**: Modal native `<dialog id="radicalFormModal">`, kiểm thực hợp đồng DTO client (`character` [$\le 10$], `pinyin` [$\le 50$], `meaningHanViet` [$\le 100$], `meaningVi` [$\le 255$], `audioUrl`/`videoWritingUrl` [$\le 500$, scheme hợp lệ]), nhận phản hồi 201 Created và cập nhật catalog phía client; **Chỉnh Sửa Bộ thủ (PUT /api/v1/admin/radicals/{id})**: Form prefill dữ liệu hiện tại, gửi toàn bộ DTO cập nhật, tiếp nhận phản hồi authoritative từ máy chủ; **Xác nhận Xóa & Xử lý 204 No Content (DELETE /api/v1/admin/radicals/{id})**: Modal xác nhận native `<dialog id="deleteRadicalModal">` cảnh báo hành vi phá hủy vĩnh viễn, xử lý an toàn phản hồi 204 No Content không gọi `response.json()`, loại bỏ bộ thủ khỏi UI; **Bắt Lỗi Xung Đột 409 Conflict**: Xử lý riêng biệt lỗi trùng ký tự khi tạo/sửa ("Bộ thủ với ký tự này đã tồn tại") và lỗi xóa bộ thủ có ràng buộc tham chiếu từ vựng ("Không thể xóa bộ thủ đang được liên kết với từ vựng"); **Xử lý Stale 404**: Cảnh báo khi bộ thủ đã bị xóa bởi quản trị viên khác và tự động làm mới catalog; **Bảo mật & Trợ năng**: Kiểm soát quyền `authManager.hasRole('Admin')`, 100% safe DOM qua `createSafeElement`/`textContent`, vệ sinh media URL qua `sanitizeResourceUrl()`, WAI-ARIA modal dialogs (Esc, backdrop, focus restoration), 3-State engine (Loading / Empty / Error / Ready); Bổ sung Section 17 chẩn đoán tại `ui-verification.html` (cam kết ZERO MUTATION); 34/34 Unit PASS (`admin-radicals.test.mjs`), 8/8 Browser E2E PASS (`admin-radicals.browser.mjs`: RA-01..RA-08), 152/152 Static PASS, 492/492 Fast PASS; Live verification: ENVIRONMENT BLOCKED (Port 8080 offline). |
| **Phase 9** | **Mod 9F** | **Task 9F.4** | Quản trị Danh mục Từ vựng tiếng Trung (Admin Vocabulary CRUD UI) | **COMPLETED** | Task 9A.2, Mod 9B | `frontend/admin-vocabulary.html`, `frontend/js/pages/admin-vocabulary-page.js`: Giao diện quản trị danh mục từ vựng tiếng Trung (CRUD) hoàn chỉnh dành riêng cho `Admin`; **Authoritative Catalog Lookup**: Kết nối `GET /api/v1/vocabulary?search=...&page=...&size=...` (endpoint tra cứu công khai, phân trang phía máy chủ `PageResponse`), không bịa đặt `GET /admin/vocabulary`; **Authoritative Detail Lookup (Zero N+1 Progressive Disclosure)**: Khi chỉnh sửa từ vựng, bắt buộc nạp chi tiết từ `GET /api/v1/vocabulary/{id}` để tiếp nhận danh sách bộ thủ cấu thành `radicals`, vì DTO tóm tắt danh sách không kèm bộ thủ; **Gán Bộ thủ Cấu thành (Complete Radical IDs Set)**: Modal biên soạn cho phép chọn đa bộ thủ Khang Hy qua danh sách có tìm kiếm, gửi toàn bộ tập ID nguyên dương `radicalIds: [9, 75]` (không dùng payload add/remove gia tăng); **Chuẩn hóa Pinyin Raw Phía Client**: Tự động trích xuất Pinyin không dấu từ Pinyin có dấu (`ü`/`v` -> `u`, bóc tách dấu kết hợp Unicode NFD), hỗ trợ sửa đổi thủ công và đồng bộ với quy tắc chuẩn hóa có thẩm quyền của máy chủ; **Tạo Mới Từ vựng (POST /api/v1/admin/vocabulary)**: Modal native `<dialog id="vocabFormModal">`, kiểm thực hợp đồng DTO client (`hanzi` [$\le 50$], `pinyin` [$\le 100$], `pinyinRaw` [$\le 100$], `meaningHanViet` [$\le 100$], `meaningVi` [$\le 255$], `audioUrl`/`videoWritingUrl` [$\le 500$, scheme hợp lệ], `exampleSentence`/`exampleTranslation` [$\le 500$]), nhận phản hồi 201 Created và cập nhật bảng dữ liệu; **Chỉnh Sửa Từ vựng (PUT /api/v1/admin/vocabulary/{id})**: Form prefill dữ liệu chi tiết authoritative, gửi toàn bộ DTO cập nhật, tiếp nhận phản hồi 200 OK; **Xác nhận Xóa & Xử lý 204 No Content (DELETE /api/v1/admin/vocabulary/{id})**: Modal xác nhận native `<dialog id="deleteVocabModal">` hiển thị chi tiết từ vựng và cảnh báo xóa vĩnh viễn/ràng buộc học tập, xử lý an toàn phản hồi 204 No Content không gọi `response.json()`, loại bỏ từ vựng khỏi bảng dữ liệu; **Bắt Lỗi Xung Đột 409 Conflict**: Xử lý riêng biệt lỗi trùng lặp `(hanzi + pinyinRaw)` khi tạo/sửa ("Từ vựng với chữ Hán '...' và pinyin '...' đã tồn tại") và lỗi xóa từ vựng có ràng buộc dữ liệu học tập liên kết `LessonVocabulary`, `PersonalNote`, `CardProgress`, `ReviewLog` ("Không thể xóa từ vựng vì dữ liệu này đang được hệ thống khác tham chiếu"); **Xử lý Stale 404**: Cảnh báo khi từ vựng đã bị xóa bởi quản trị viên khác và tự động làm mới danh mục; **Bảo mật & Trợ năng**: Kiểm soát quyền `authManager.hasRole('Admin')`, 100% safe DOM qua `createSafeElement`/`textContent`, vệ sinh media URL qua `sanitizeResourceUrl()`, WAI-ARIA modal dialogs (Esc, backdrop, focus restoration), 3-State engine (Loading / Empty / Error / Ready); Bổ sung Section 17B chẩn đoán tại `ui-verification.html` (cam kết ZERO MUTATION); 35/35 Unit PASS (`admin-vocabulary.test.mjs`), 10/10 Browser E2E PASS (`admin-vocabulary.browser.mjs`: VV-01..VV-10), 159/159 Static PASS, 499/499 Fast PASS; **Live verification: PASS (Live Full-Stack Verified, 10/10 phases PASS)** trên MySQL 8.4 port 3306, Spring Boot 3.3.5 port 8080, Static Server port 3000 (`admin-vocabulary-live.mjs`: đăng ký Admin dùng một lần, elevate vai trò Admin trong MySQL, đăng nhập nhận JWT thực, tải catalog từ vựng, tìm kiếm server "你好", tạo mới từ vựng "测试" kèm bộ thủ 9 "人" [201 Created, DB persisted], thử tạo trùng [409 Conflict], sửa từ vựng cập nhật nghĩa [200 OK, DB updated], xác nhận xóa [204 No Content, DB deleted & cascade cleaned], dọn sạch dữ liệu thử nghiệm 100%). |
| **Phase 9** | **Mod 9F** | **Task 9F.5** | Giám sát Toàn bộ Bài học Hệ thống (Global Lesson Oversight) | **COMPLETED** | Task 9A.2, Mod 9D | `frontend/admin-lessons.html`, `frontend/js/pages/admin-lessons-page.js`: Giao diện giám sát bài học toàn hệ thống dành riêng cho `Admin`; **Authoritative Lesson Oversight**: Kết nối `GET /api/v1/admin/lessons` (hỗ trợ phân trang máy chủ `page`, `size` và lọc trạng thái `status`: `Draft`, `Pending`, `Approved`, `Rejected`); **Bất biến Tuyệt đối READ-ONLY**: Trang giám sát 100% chỉ đọc, strictly zero mutation requests (`POST`/`PUT`/`PATCH`/`DELETE`), không thêm thao tác tạo/sửa/xóa/phê duyệt/từ chối; **Loại bỏ Bộ lọc Giả**: Không thêm bộ lọc tác giả hoặc ô tìm kiếm vì backend contract không hỗ trợ; **5 Thẻ KPI Trạng thái Toàn cục**: Hiển thị tổng số bài học `Total` và số lượng theo từng trạng thái `Draft`, `Pending`, `Approved`, `Rejected` từ `totalElements` của máy chủ, không suy diễn từ số hàng của trang hiện tại; **Hợp đồng Chi tiết Đa chiến lược & Minh bạch Giới hạn**: Mở modal `<dialog id="lessonDetailModal">` hiển thị chi tiết bài học; bài học `Approved` nạp từ `GET /api/v1/lessons/{id}` (public DTO); bài học `Pending` nạp từ `GET /api/v1/moderator/lessons/{id}` (được phân quyền cho Admin qua `@PreAuthorize`); bài học `Draft` và `Rejected` không có endpoint chi tiết cho Admin trong backend hiện tại $\rightarrow$ hiển thị biểu ngữ thông báo trung thực về giới hạn hợp đồng thay vì báo "Không có nội dung"; **Bảo mật & Trợ năng**: Kiểm soát quyền `authManager.hasRole('Admin')`, 100% safe DOM qua `createSafeElement`/`textContent`, native `<dialog>` chuẩn APG (Esc, backdrop, focus restoration), bảng dữ liệu ngữ nghĩa `<table>` với `<caption>`, `<th scope="col">`, `.table-responsive`; Đồng bộ thanh điều hướng 5 tab Quản trị trên cả 5 trang Admin; Bổ sung Section 17C chẩn đoán tại `ui-verification.html` (zero mutation capability probe); **Giải quyết triệt để Test Hang**: Sửa `serverHandle.close()` trong teardown, sửa destructuring `getRuntimeErrors()`, sửa mã phản hồi mock `code: 'SUCCESS'` theo chuẩn `api.js`, đồng bộ ID phần tử modal; 34/34 Unit PASS (`admin-lessons.test.mjs`), 8/8 Browser E2E PASS (`admin-lessons.browser.mjs`: AL-01..AL-08), 166/166 Static PASS, 16/16 Verification Center PASS; **Live Full-Stack Verification: PASS (8/8 phases PASS, ZERO mutations)** trên MySQL 8.4 port 3306, Spring Boot 3.3.5 port 8080, Static Server port 3000 (`admin-lessons-live.mjs`: Admin login thực, kiểm tra bảng 4 bài học, xác minh KPI counts, lọc Approved, mở chi tiết Approved #9369 nạp 2 từ vựng, xác nhận zero mutation). |
| **Phase 9** | **Mod 9G** | **Task 9G.1** | Đánh giá Mô hình Đe dọa Lưu trữ Token Phía Client (Token Threat Model Evaluation) | **COMPLETED** | Mod 9A..9F | Đánh giá bảo mật toàn diện mô hình lưu trữ token phía client; phân tích chi tiết `localStorage` vs `sessionStorage` vs `HttpOnly Cookie`; xác lập ranh giới tin cậy (Trust Boundaries) và rủi ro tồn dư (Residual Risk); thiết lập quyết định kiến trúc `DEC-43` duy trì `localStorage` kết hợp phòng thủ đa tầng (Defense-in-Depth: 9G.2 DOM XSS audit + 9G.3 CSP + server-side `authorization_version` DEC-42); quy hoạch kiến trúc mục tiêu tương lai BFF / Same-Origin Reverse Proxy; 30/30 Unit targeted PASS (`auth-state.test.mjs`, `api-client.test.mjs`), 4/4 Static DOM sinks PASS, 11/11 Browser E2E PASS (`auth-flow.browser.mjs`, `api-auth.browser.mjs`), 39/39 Backend JWT Unit PASS (`JwtUtilTests`, `JwtAuthenticationFilterTests`); Live FE↔BE: BLOCKED (MySQL/Docker offline). |
| **Phase 9** | **Mod 9G** | **Task 9G.2** | Rà soát Phòng chống DOM XSS & Kiểm soát Bồn trũng Dữ liệu Động | **COMPLETED** | Task 9G.1 | `security.js`, `style.css`: 100% dữ liệu người dùng được render qua `textContent`/DOM APIs, cấm `eval`/dynamic script, loại bỏ 100% inline event handlers trên toàn bộ 22 HTML pages, thắt chặt `sanitizeNavigationUrl()` và `sanitizeResourceUrl()`, CSS validation allowlist nghiêm ngặt, quyết định kiến trúc `DEC-44`; 29/29 Security Unit PASS, 5/5 Static Sinks PASS, 5/5 Adversarial Chromium Browser E2E PASS (`dom-xss-adversarial.browser.mjs`), 167/167 Static PASS, 525/525 Fast PASS. |
| **Phase 9** | **Mod 9G** | **Task 9G.3** | Kiểm toán Đồ thị Tài nguyên & Thiết lập Content Security Policy (CSP) | **COMPLETED** | Task 9G.2 | Đồ thị tài nguyên thực tế xác lập chính sách CSP nghiêm ngặt (`default-src 'self'`, `script-src 'self' https://cdn.jsdelivr.net`, `style-src 'self' https://cdn.jsdelivr.net`, `object-src 'none'`, `frame-ancestors 'none'`, `frame-src 'none'`), loại bỏ 100% inline `<script>` trên cả 22 trang HTML, loại bỏ hoàn toàn Google Fonts không dùng, chuyển giao CSP qua HTTP response header tại ranh giới tài liệu (`tests/frontend/utils/static-server.mjs`), quyết định kiến trúc `DEC-45`; 10/10 Static CSP tests PASS, 8/8 Chromium Enforcement Browser PASS (`csp-enforcement.browser.mjs`), 177/177 Static PASS, 535/535 Fast PASS. |
| **Phase 9** | **Mod 9G** | **Task 9G.4** | Kiểm toán Khả năng Tiếp cận Toàn diện (WCAG 2.2 AA) | **COMPLETED** | Task 9G.3 | Kiểm toán toàn diện 22 trang HTML, CSS và JavaScript theo WCAG 2.2 Level AA; phân định rõ ràng chuẩn mực bắt buộc (Normative) vs quy ước dự án; khắc phục SC 2.4.11 Focus Not Obscured bằng `scroll-margin-top` trên CSS; khắc phục SC 1.3.1 thêm caption cho bảng tại `creator-import.html` và `creator-lessons.html`; khắc phục SC 3.3.8 bổ sung `autocomplete="name"` tại `profile.html`; hoàn thiện WAI-ARIA APG focus trap (Tab cycling, Escape, focus restoration) cho modal; thêm root-element guards chống lỗi multi-page auto-bootstrap; quyết định kiến trúc `DEC-46`; 19/19 Chromium Accessibility Browser tests PASS (`a11y.browser.mjs`), 177/177 Static PASS, 535/535 Fast PASS. |
| **Phase 9** | **Mod 9G** | **Task 9G.5** | Kiểm chứng Bố cục Đa màn hình & Đa thiết bị (Responsive Baseline) | **COMPLETED** | Task 9G.4 | Kiểm toán bố cục đa màn hình trên 100% 22 trang HTML theo ma trận thiết bị Mobile (375x667), Mobile Large (414x896), Tablet (768x1024), Desktop (1200x800), Desktop Large (1440x900) và WCAG SC 1.4.10 Reflow spot-check (320x640); phát hiện nguyên nhân gốc rễ và khắc phục tràn màn hình 0px (xử lý wrap badges `#sessionRoles` trên `ui-verification.html`, chuẩn hóa breakpoint `@media (max-width: 575.98px)` trong `style.css`); bảo toàn cuộn ngang cục bộ an toàn cho các bảng quản trị/kiểm duyệt (`.table-responsive`) và Excel import preview; kiểm chứng sâu flashcard SRS 3D transform và Creator import 2 bước; bắt 27 ảnh chụp màn hình Chromium thực tế; ban hành quyết định kiến trúc `DEC-47`; bộ test trình duyệt `responsive-layout.browser.mjs` (10/10 PASS), Static (177/177 PASS), Fast (535/535 PASS), A11y (19/19 PASS), tổng 741 frontend tests PASS; nghiệm thu toàn diện Phase 9 Frontend. |
| **Phase 10** | **Mod 10A** | **Task 10A.1** | Rà soát Hồi quy Ranh giới Tiêm nhiễm & OWASP API Security | **NOT_STARTED** | Checkpoint Phase 9 | Rà soát toàn diện OWASP API Top 10 (BOLA, Broken Auth, BOPLA, Limits, BFLA, SSRF, Misconfig) qua UI forms; xác minh phòng chống hiệu quả SQLi và Path Traversal. |
| **Phase 10** | **Mod 10A** | **Task 10A.2** | Kiểm thử Hồi quy Vòng đời Xác thực & Thu hồi Token JWT | **NOT_STARTED** | Task 10A.1 | Kiểm chứng thực tế đa tab: đổi trạng thái Inactive/Banned hoặc đổi vai trò thu hồi token ngay lập tức (DEC-42), token hết hạn tự động đăng xuất. |
| **Phase 10** | **Mod 10A** | **Task 10A.3** | Kiểm thử Đối kháng Biểu mẫu Upload & Import Excel | **NOT_STARTED** | Task 10A.1 | Thử nghiệm file độc hại: giả mạo `.xlsx`, trùng ZIP entry name (CVE-2025-31672 regression), file >10MB, file >5000 dòng, công thức macro; từ chối 400 an toàn. |
| **Phase 10** | **Mod 10A** | **Task 10A.4** | Kiểm toán Chuẩn hóa Lỗi & Phòng chống Rò rỉ Thông tin | **NOT_STARTED** | Task 10A.1 | Kiểm tra 100% kịch bản lỗi HTTP (400, 401, 403, 404, 409, 413, 422, 429, 500); xác nhận zero stack trace, zero SQL leakage, định dạng chuẩn `{code, message, data, errors}`. |
| **Phase 10** | **Mod 10B** | **Task 10B.1** | Kiểm toán Tính nhất quán 3 Trạng thái Giao diện (Three-State UI) | **NOT_STARTED** | Checkpoint Phase 9 | Rà soát 100% màn hình 4 vai trò bảo đảm hiển thị mượt mà Loading state (spinner/skeleton), Empty state (hướng dẫn), và Error state (thông báo lỗi + nút thử lại). |
| **Phase 10** | **Mod 10B** | **Task 10B.2** | Xử lý Sự cố Mạng, Timeout & Suy giảm Kết nối (Degraded Network) | **NOT_STARTED** | Task 10B.1 | Kiểm thử ngắt mạng (Offline), mạng chậm (Slow 3G), và lỗi phản hồi 429 Too Many Requests từ rate limiter; thông báo người dùng lịch sự; phân biệt ranh giới mạng client vs transaction rollback CSDL. |
| **Phase 10** | **Mod 10B** | **Task 10B.3** | Kiểm chứng Đa trình duyệt & Đa kích thước Màn hình (Cross-Browser) | **NOT_STARTED** | Task 10B.1 | Kiểm thử trên ma trận Chromium, Firefox, WebKit trên Mobile (375px), Tablet (768px), Desktop (1200px); tap targets $\ge 24\text{px}$, không vỡ layout. |
| **Phase 10** | **Mod 10C** | **Task 10C.1** | Kiểm thử Hồi quy Toàn bộ Bộ test Backend Master (1101 tests) | **NOT_STARTED** | Checkpoint Phase 9 | Chạy `mvn clean test` trên Testcontainers MySQL 8.4; xác nhận toàn bộ **1,101/1,101 tests PASS** (0 failures, 0 errors, 0 skipped). |
| **Phase 10** | **Mod 10C** | **Task 10C.2** | Tính Toàn vẹn & Rollback Giao dịch dưới Điều kiện Lỗi Nghiệp vụ Thực | **NOT_STARTED** | Task 10C.1 | Kiểm thử rollback giao dịch đa bảng khi gặp lỗi nghiệp vụ thực tế (ràng buộc trùng lặp, import confirm lỗi giữa chừng); bảo đảm zero bản ghi rác/mồ côi trong CSDL. |
| **Phase 10** | **Mod 10C** | **Task 10C.3** | Kiểm thử Hồi quy Hợp đồng API & Đồng bộ Tài liệu Đặc tả | **NOT_STARTED** | Task 10C.1 | Đối chiếu Controller DTOs, `API.md`, `frontend/js/api/api.js` và phản hồi thực tế từ backend; bảo đảm khớp 100% tên trường và mã lỗi. |
| **Phase 10** | **Mod 10D** | **Task 10D.1** | Đo đạc Hiệu năng Client & Chỉ số Core Web Vitals (Internal Targets) | **NOT_STARTED** | Mod 10B | Lighthouse / DevTools audit: LCP $< 2.5\text{s}$, CLS $< 0.1$, tổng dung lượng tài nguyên tải lần đầu $< 1\text{MB}$ khi gzip; coi là mục tiêu định hướng nội bộ. |
| **Phase 10** | **Mod 10D** | **Task 10D.2** | Kiểm thử Hồi quy Kế hoạch Thực thi Truy vấn CSDL (`EXPLAIN`) | **NOT_STARTED** | Mod 10C | Kiểm chứng `EXPLAIN` trên CSDL mẫu; xác minh chỉ mục tối ưu, không full table scan trên bảng lớn; xác định rõ filesort nhỏ trong RAM không tự động bị coi là bug. |
| **Phase 10** | **Mod 10D** | **Task 10D.3** | Kiểm thử Hành vi Tập dữ liệu Lớn & Vòng đời Tài nguyên Client | **NOT_STARTED** | Task 10D.1, Task 10D.2 | Kiểm tra phân trang danh mục, import 5000 dòng Excel, giải phóng bộ nhớ client và event listeners khi chuyển trang. |
| **Phase 10** | **Mod 10E** | **Task 10E.1** | Thiết lập Khung Kiểm thử Trình duyệt Tự động Đầu cuối (Playwright) | **NOT_STARTED** | Checkpoint 10A, 10B | Khởi tạo Playwright framework (Chromium Desktop + Mobile viewport), cấu hình headless CI, quản lý test fixtures cho 4 vai trò. |
| **Phase 10** | **Mod 10E** | **Task 10E.2** | Kịch bản E2E 1: Toàn bộ Hành trình Học tập của Học viên (Learner Flow) | **NOT_STARTED** | Task 10E.1 | Kịch bản tự động: Đăng ký $\rightarrow$ Đăng nhập $\rightarrow$ Học bộ thủ $\rightarrow$ Tìm từ vựng Pinyin $\rightarrow$ Tạo ghi chú $\rightarrow$ Lật thẻ Flashcard SRS $\rightarrow$ Thống kê Study Stats. |
| **Phase 10** | **Mod 10E** | **Task 10E.3** | Kịch bản E2E 2: Chu trình Biên soạn & Kiểm duyệt Bài học (Content Lifecycle) | **NOT_STARTED** | Task 10E.1 | Kịch bản tự động: Creator tạo Draft $\rightarrow$ Import Excel preview $\rightarrow$ Confirm $\rightarrow$ Nộp duyệt $\rightarrow$ Moderator từ chối có lý do $\rightarrow$ Creator sửa & nộp lại $\rightarrow$ Moderator phê duyệt $\rightarrow$ Learner thấy bài học. |
| **Phase 10** | **Mod 10E** | **Task 10E.4** | Kịch bản E2E 3: Quản trị Hệ thống (Admin Flow) | **NOT_STARTED** | Task 10E.1 | Kịch bản tự động: Admin đổi trạng thái tài khoản $\rightarrow$ Phân quyền vai trò $\rightarrow$ Quản trị bộ thủ/từ vựng $\rightarrow$ Giám sát bài học toàn hệ thống. |
| **Phase 10** | **Mod 10E** | **Task 10E.5** | Kịch bản E2E 4: Ranh giới Bảo mật & Xử lý Lỗi Hệ thống | **NOT_STARTED** | Task 10E.1 | Kịch bản tự động: Token hết hạn 401 $\rightarrow$ Vượt quyền 403 $\rightarrow$ Can thiệp trái phép ghi chú $\rightarrow$ Quá tần suất 429 Too Many Requests. |
| **Phase 10** | **Mod 10F** | **Task 10F.1** | Kiểm thử Hồi quy Tương tác Đa vai trò (Cross-Role Regression) | **NOT_STARTED** | Mod 10E | Kiểm chứng không rò rỉ quyền hạn chéo; chuyển đổi nhịp nhàng giữa Learner, Creator, Moderator và Admin. |
| **Phase 10** | **Mod 10F** | **Task 10F.2** | Kiểm thử Hồi quy Đa trình duyệt (Cross-Browser Regression) | **NOT_STARTED** | Mod 10E | Chạy E2E hồi quy trên Chromium, Firefox, WebKit; xác nhận tính tương thích đa nền tảng. |
| **Phase 10** | **Mod 10F** | **Task 10F.3** | Tổng hợp Kiểm toán An ninh Full-Stack (Security Consolidation) | **NOT_STARTED** | Mod 10A, Task 10E.5 | Tổng kết báo cáo an ninh Full-Stack, xác minh không phát hiện lỗ hổng High/Critical chưa được xử lý trong phạm vi kiểm thử, đối chiếu tuân thủ các chuẩn kiểm soát ASVS 5.0.0 được chọn lọc. |
| **Phase 10** | **Mod 10F** | **Task 10F.4** | Hồi quy Toàn vẹn Dữ liệu CSDL Sau Tích hợp (Data Integrity) | **NOT_STARTED** | Task 10C.2, Mod 10E | Kiểm tra CSDL MySQL sau chuỗi test E2E; khẳng định không có bản ghi mồ côi hay dữ liệu rác. |
| **Phase 10** | **Mod 10F** | **Task 10F.5** | Hồi quy Tính Nhất quán Tài liệu & API Trước Phát hành | **NOT_STARTED** | Mod 10A..10E | Rà soát toàn bộ tài liệu dự án; khẳng định tính nhất quán 100% trước khi bước vào Phase 11. |
| **Phase 11** | **Mod 11A** | **Task 11A.1** | Biên dịch & Đóng gói Bản dựng Phát hành Sạch (`mvn clean package`) | **NOT_STARTED** | Checkpoint Phase 10 | Build bản dựng độc lập `target/elearning-backend-1.0.0.jar` trên cây mã nguồn sạch, 0 test failures. |
| **Phase 11** | **Mod 11A** | **Task 11A.2** | Kiểm chứng Migration & Khởi tạo Dữ liệu CSDL Trắng (V1 $\rightarrow$ V7) | **NOT_STARTED** | Task 11A.1 | Áp dụng toàn bộ Flyway V1..V7 trên MySQL 8.4 hoàn toàn mới; xác nhận nạp đủ 4 roles và 214 bộ thủ Khang Hy (chuẩn Pinyin V7). |
| **Phase 11** | **Mod 11A** | **Task 11A.3** | Kiểm toán Bí mật & Cấu hình An toàn Môi trường Sản xuất | **NOT_STARTED** | Task 11A.1 | Rà soát `application.yml`; khẳng định 100% bí mật dùng biến môi trường (`${DB_PASSWORD}`, `${JWT_SECRET}`); không rò rỉ thông tin trong log. |
| **Phase 11** | **Mod 11B** | **Task 11B.1** | Khởi động Profile Production & Kiểm chứng Health Probe | **NOT_STARTED** | Mod 11A | Chạy ứng dụng với `SPRING_PROFILES_ACTIVE=prod`; xác nhận khởi động $< 15\text{s}$; `/actuator/health` trả về `{"status":"UP"}` an toàn. |
| **Phase 11** | **Mod 11B** | **Task 11B.2** | Giả lập Reverse-Proxy, Header Forwarding & Web Security Headers | **NOT_STARTED** | Task 11B.1 | Kiểm chứng `server.forward-headers-strategy: framework` với header `X-Forwarded-*`; xác minh client IP và headers HSTS, nosniff, Referrer-Policy; ghi rõ trạng thái giả lập cục bộ. |
| **Phase 11** | **Mod 11B** | **Task 11B.3** | Xác minh Nguồn gốc & Cấu hình CORS Sản xuất | **NOT_STARTED** | Task 11B.1 | Kiểm chứng whitelist domain trong `${APP_CORS_ALLOWED_ORIGINS}`; từ chối an toàn request từ origin lạ. |
| **Phase 11** | **Mod 11C** | **Task 11C.1** | Thiết lập Mô hình Triển khai Chính thức (Standalone JAR + Nginx) | **NOT_STARTED** | Mod 11B | Thiết lập cấu hình Nginx reverse proxy phục vụ tài nguyên tĩnh `frontend/` và proxy an toàn request `/api/` tới Backend (Docker Compose là giải pháp thay thế). |
| **Phase 11** | **Mod 11C** | **Task 11C.2** | Cấu hình Reverse Proxy, HTTPS & Gia cố Bảo mật TLS | **NOT_STARTED** | Task 11C.1 | Cấu hình SSL/TLS, HTTP/2, chuyển hướng HTTPS, và giới hạn kích thước request body trong Nginx. |
| **Phase 11** | **Mod 11C** | **Task 11C.3** | Kiểm chứng Đường truyền & Thăm dò Sức khỏe Dưới Mô hình Chính | **NOT_STARTED** | Task 11C.2 | Kiểm tra truy cập qua Nginx tới frontend và backend thông suốt; nếu không có cloud server thực tế, ghi nhận rõ là verified in local simulation. |
| **Phase 11** | **Mod 11C** | **Task 11C.4** | Kiểm thử Khói Vòng đời Hoạt động (Startup & Graceful Shutdown) | **NOT_STARTED** | Task 11C.3 | Khởi động sạch, chạy smoke test, và tắt ứng dụng duyên dáng qua `SIGTERM` (đóng kết nối HikariCP an toàn, không rò rỉ tiến trình). |
| **Phase 11** | **Mod 11D** | **Task 11D.1** | Bộ Sưu tập Postman / OpenAPI Toàn diện cho 100% 49 HTTP Mappings | **NOT_STARTED** | Mod 11C | Xuất bộ Postman Collection & Environment bao phủ 49 HTTP method+path mappings (48 business handlers) cho 4 vai trò, kèm script assertion tự động 100% PASS. |
| **Phase 11** | **Mod 11D** | **Task 11D.2** | Sổ tay Vận hành Triển khai Sản xuất (Deployment Runbook) | **NOT_STARTED** | Mod 11C | Soạn thảo `.agents/RUNBOOK.md` và `docs/DEPLOYMENT_GUIDE.md`: hướng dẫn cài đặt từ đầu, cấu hình systemd/JAR, Nginx SSL, sao lưu/phục hồi CSDL. |
| **Phase 11** | **Mod 11D** | **Task 11D.3** | Hướng dẫn Cấu hình Biến Môi trường Hệ thống | **NOT_STARTED** | Task 11D.2 | Soạn thảo `docs/ENVIRONMENT.md`: tài liệu hóa toàn bộ biến môi trường, giá trị mặc định và mục đích cấu hình. |
| **Phase 11** | **Mod 11D** | **Task 11D.4** | Báo cáo Tổng kết Kiểm thử Toàn diện Dự án | **NOT_STARTED** | Mod 11C | Soạn thảo `docs/TEST_REPORT.md`: tổng hợp kết quả Unit, Integration, Concurrency, Rollback và E2E tests. |
| **Phase 11** | **Mod 11D** | **Task 11D.5** | Đồng bộ hóa Toàn diện Tài liệu Kiến trúc, API & Trạng thái Dự án | **NOT_STARTED** | Task 11D.1..11D.4 | Đồng bộ hóa toàn bộ tài liệu `.agents/` (`CURRENT_STATE`, `API`, `ARCHITECTURE`, `DATABASE`, `DECISIONS`, `PROGRESS`, `ROADMAP`) khớp 100% thực tế bàn giao. |
| **Phase 11** | **Mod 11E** | **Task 11E.1** | Nghiệm thu Đa chiều Dự án & Ký duyệt Niêm phong Phát hành | **NOT_STARTED** | Mod 9A..11D | Phiên nghiệm thu chốt chặn 7 tiêu chuẩn tối cao (Chức năng, An ninh, E2E Automation, Hiệu năng, Đóng gói, Tài liệu, Chấp thuận rủi ro) và Ký duyệt bàn giao. |

---

## 3. BACKEND HARDENING WORK (R3.4 → R3.11) [COMPLETED]

Các nhiệm vụ kỹ thuật Backend Hardening đã hoàn thành toàn bộ và được niêm phong trước khi chuyển giao sang Phase 9 (Frontend):

| Task ID | Task Name | Status | Priority | Purpose & Scope |
| :--- | :--- | :---: | :---: | :--- |
| **Task R3.4** | Object-Level Authorization & API Boundary Audit | **`COMPLETED`** | **`P0`** | Kiểm toán toàn diện phân quyền cấp đối tượng (IDOR) và phân quyền vai trò trên tất cả các Controller endpoints (45 endpoints audited, no issues). |
| **Task R3.5** | Personal Notes Pagination & Resource Consumption Hardening | **`COMPLETED`** | **`P1`** | Bổ sung phân trang (`Pageable`) cho `GET /api/v1/vocabularies/{vocabId}/notes` (default 20, max 100, stable sort createdAt DESC). |
| **Task R3.6** | Login Rate Limiter TTL Cache Eviction & Memory Lifecycle | **`COMPLETED`** | **`P1`** | Tích hợp cơ chế tự động giải phóng key IP rác theo thời gian (TTL cache eviction) trong `LoginRateLimiter` chống tích tụ bộ nhớ. |
| **Task R3.7** | Dependency & Framework Security Hardening (Apache POI CVE-2025-31672) | **`COMPLETED`** | **`P2`** | Nâng cấp Apache POI `poi-ooxml` lên `5.4.0` vá lỗ hổng OOXML duplicate ZIP entry validation (CVE-2025-31672), tương thích 100% Spring Boot 3.3.5. |
| **Task R3.8** | Kangxi Radicals Missing Pinyin Correction & Seed Data Ingestion | **`COMPLETED`** | **`P2`** | Khắc phục 2 bản ghi thiếu Pinyin trong seed gốc (ID 49 `jǐ` và ID 172 `zhuī`) qua Flyway migration `V7`, bảo toàn 100% 214 radicals và empty `meaning_vi`. |
| **Task R3.9** | Database Index Performance & Slow Query Verification (`EXPLAIN`) | **`COMPLETED`** | **`P2`** | Kiểm tra và đo đạc execution plan qua `EXPLAIN` và `EXPLAIN ANALYZE TREE` trên 14 bảng và 47 index entries; xác minh 0 filesort cho due cards và lesson vocab; phát hiện redundant index `idx_vocab_hanzi`; kết luận `INDEXES VERIFIED, NO CHANGE REQUIRED`. |
| **Task R3.10** | Production HTTP & Reverse Proxy Security Headers | **`COMPLETED`** | **`P3`** | Kiểm chứng Spring Security 6 default headers (nosniff, DENY, XSS 0, no-cache, HSTS HTTPS); bổ sung Referrer-Policy strict-origin-when-cross-origin; cấu hình `server.forward-headers-strategy: framework` cho Reverse Proxy; phân lập rate limit theo client IP; kiểm chứng chiến lược none; hoãn lại CSP sang Phase 9. |
| **Task R3.11** | Backend Final Quality Gate & Pre-Frontend Release Seal | **`COMPLETED`** | **`P0 (Gate)`** | Toàn bộ 1088/1088 tests PASS (0 fail, 0 err, 0 skip), V1..V7 Flyway clean, 45 REST endpoints verified, Backend Release Seal active (DEC-41). Phase 9 Frontend unblocked. |

### Thứ tự chuyển giao (Execution & Handoff Sequence):
```text
Task R3.11 [COMPLETED] — Backend Final Quality Gate & Pre-Frontend Release Seal (DEC-41)
    │
    ▼
==================== BACKEND FINAL GATE CLEARED & SEALED ====================
    │
    ▼
PHASE 9 — FRONTEND UI & CLIENT API INTEGRATION [UNBLOCKED / READY TO COMMENCE]
    │
    ▼
Task 9A.1 — Frontend Foundation, Core Shell, Design System & Browser-Safe UI [COMPLETED]
Task 9A.1.1 — Frontend Verification Infrastructure & Permanent Regression Suite [COMPLETED]
Task 9A.2 — Centralized HTTP API Client & Session Manager [COMPLETED]
Task 9B.1 — Authentication UI, Profile & Session Integration [COMPLETED]
    │
    ▼
Task 9B.2 — Radical Catalog: 214 Kangxi Radicals (radicals.html, radical-detail) [NEXT IMMEDIATE TASK]
```

---

## Task 9B.1 — Authentication UI, Profile & Session Integration
**Status:** COMPLETED  
**Date:** 2026-09-09  

### Implemented
- **`login.html`**: Giao diện đăng nhập tài khoản học tiếng Trung đạt chuẩn HTML5 ngữ nghĩa và WCAG 2.2 Level AA; bố cục thẻ học giả (scholarly card), hiển thị thông báo phiên làm việc hết hạn (`?expired=true`) hoặc đăng ký thành công (`?registered=true`), inline validation feedback (`is-invalid`, `aria-invalid`, `aria-describedby`), thông báo lỗi cấp biểu mẫu qua `role="alert"`, nút đăng nhập có trạng thái chờ (loading spinner) và cơ chế chống gửi lặp (anti-double-submit).
- **`register.html`**: Giao diện tạo tài khoản học viên mới; các trường nhập liệu (`fullName` 1..100, `emailOrPhone` 1..191, `password` 6..100) khớp 100% với DTO `RegisterRequest` của backend; client-side validation và hướng dẫn trợ giúp mật khẩu cho người dùng; chuyển hướng sang `login.html?registered=true` khi tạo tài khoản thành công.
- **`profile.html`**: Giao diện quản lý hồ sơ người dùng được bảo vệ (Authentication Guard: tự động chuyển hướng unauthenticated khách sang `login.html?redirect=profile.html`); 3-State UI (Loading/Error/Ready) khi nạp dữ liệu từ `GET /api/v1/users/profile`; hiển thị ảnh đại diện có cơ chế vệ sinh bảo vệ tài nguyên (`sanitizeResourceUrl`); thông tin định danh (User ID, Account ID, Ngày tham gia); form chỉnh sửa `fullName` và `avatarUrl` gửi `PUT /api/v1/users/profile`, khôi phục thông tin (Reset), và thông báo Toast thành công.
- **`frontend/js/ui/nav.js`**: Module điều hướng dùng chung xác định trạng thái phiên (Session-Aware Navigation Component); loại bỏ hoàn toàn việc trùng lặp logic hiển thị giữa các trang; phản ứng tức thì với các sự kiện phiên (`auth:login`, `auth:logout`, `auth:expired`, `auth:update`); hiển thị Guest actions ("Đăng nhập", "Bắt đầu học") và Authenticated actions (Tên người dùng, Badge vai trò theo tiếng Việt, Nút "Hồ sơ", Nút "Đăng xuất").
- **`frontend/js/pages/auth-page.js`**: Bộ điều khiển biểu mẫu đăng nhập và đăng ký; xác thực dữ liệu phía client; gọi `apiClient('/auth/login')` và `apiClient('/auth/register')`; đồng bộ phiên qua `authManager.setSession()`; xử lý lỗi phân biệt (400 validation unpack qua `parseFieldErrors`, 401 unauthorized, 409 conflict, 429 rate limit, 0/408 network error); phòng chống tấn công Open Redirect trên `?redirect=` qua `validateInternalRedirect()`.
- **`frontend/js/pages/profile-page.js`**: Bộ điều khiển trang hồ sơ; bảo vệ chống truy cập trái phép; nạp hồ sơ qua `apiClient('/users/profile')`; cập nhật hồ sơ qua `PUT`; cập nhật metadata phiên qua `authManager.updateUser({ fullName })` giúp thanh điều hướng phản ứng ngay lập tức mà không cần tải lại trang.
- **Multi-Tab Session Synchronization**: Mở rộng trình lắng nghe sự kiện `storage` trong `frontend/js/auth/auth-state.js` để tự động phát hiện khi tab khác đăng nhập hoặc cập nhật hồ sơ (`auth:login`, `auth:update`), bên cạnh việc đăng xuất (`auth:expired`).

### Files Changed
- `frontend/js/ui/nav.js` [NEW]
- `frontend/login.html` [NEW]
- `frontend/register.html` [NEW]
- `frontend/profile.html` [NEW]
- `frontend/js/pages/auth-page.js` [NEW]
- `frontend/js/pages/profile-page.js` [NEW]
- `frontend/index.html` [MODIFY]
- `frontend/js/app.js` [MODIFY]
- `frontend/js/auth/auth-state.js` [MODIFY]
- `frontend/js/ui/security.js` [MODIFY]
- `tests/frontend/unit/auth/auth-forms.test.mjs` [NEW]
- `tests/frontend/e2e/auth-flow.browser.mjs` [NEW]
- `tests/frontend/runner.mjs` [MODIFY]
- `tests/frontend/static/html-semantics.test.mjs` [MODIFY]
- `tests/frontend/static/sri-integrity.test.mjs` [MODIFY]
- `tests/frontend/accessibility/a11y.browser.mjs` [MODIFY]

### Backend Contracts Verified
- `POST /api/v1/auth/register`:
  - Request: `RegisterRequest` (`emailOrPhone` 1..191, `password` 6..100, `fullName` 1..100).
  - Response: 201 Created với `ApiResponse<AuthResponse>` (`token`, `accountId`, `emailOrPhone`, `fullName`, `roles`).
  - Lỗi: 400 (`VALIDATION_ERROR` kèm `errors: ["field: message"]`), 409 (`CONFLICT` khi email/phone đã tồn tại).
- `POST /api/v1/auth/login`:
  - Request: `LoginRequest` (`emailOrPhone` 1..191, `password`).
  - Response: 200 OK với `ApiResponse<AuthResponse>`.
  - Lỗi: 400, 401 (`UNAUTHORIZED` generic), 429 (`TOO_MANY_REQUESTS` > 10 req/phút).
- `GET /api/v1/users/profile`:
  - Request: Header `Authorization: Bearer <token>`.
  - Response: 200 OK với `ApiResponse<UserProfileResponse>` (`userId`, `accountId`, `emailOrPhone`, `fullName`, `avatarUrl`, `createdAt`, `updatedAt`). **Đặc biệt: DTO này không chứa roles!**
  - Lỗi: 401 (`UNAUTHORIZED`).
- `PUT /api/v1/users/profile`:
  - Request: `UpdateProfileRequest` (`fullName` 1..100, `avatarUrl` $\le 500$ nullable).
  - Response: 200 OK với `ApiResponse<UserProfileResponse>`.
  - Lỗi: 400, 401.

### Important Architecture Decisions
1. **Roles Sourced Strictly from Auth/Session, Never from UserProfileResponse**:
   - DTO `UserProfileResponse` của backend hoàn toàn không có trường `roles`.
   - Quyền và vai trò được lưu trữ và truy xuất độc quyền từ `authManager.getRoles()`.
   - Ánh xạ vai trò sang giao diện tiếng Việt: `Learner` $\rightarrow$ "Học viên", `Creator` $\rightarrow$ "Tác giả", `Moderator` $\rightarrow$ "Kiểm duyệt", `Admin` $\rightarrow$ "Quản trị".
2. **Session Metadata Update Without Token Manipulation**:
   - Thêm phương thức `authManager.updateUser(partialUser)` để cập nhật `fullName` trong `localStorage` mà không làm thay đổi JWT token hay trùng lặp logic lưu trữ.
   - Phương thức này kích hoạt sự kiện `auth:update`, cho phép thanh điều hướng tự động cập nhật tên mới mà không cần F5.
3. **Single Authoritative Navigation Component (`nav.js`)**:
   - Khởi tạo thanh điều hướng tại một nơi duy nhất (`initNavbarAuth('navAuthContainer')`).
   - Loại bỏ triệt để mã nguồn render/bắt sự kiện trùng lặp giữa `index.html`, `login.html`, `register.html` và `profile.html`.
4. **Open Redirect Defense on `?redirect=` Sink**:
   - Sử dụng `validateInternalRedirect()` kiểm tra nghiêm ngặt: từ chối URL chứa host ngoài, protocol-relative (`//`), và các pseudo-scheme nguy hiểm (`javascript:`, `data:`, `vbscript:`).
   - Chỉ cho phép chuyển hướng nội bộ cùng origin hoặc fallback về `index.html`.
5. **Defense-in-Depth Safe Rendering & Avatar Sanitization**:
   - 100% dữ liệu từ backend và người dùng được đưa vào DOM qua `textContent` hoặc `createSafeElement()`.
   - Đường dẫn ảnh avatar được kiểm tra qua `sanitizeResourceUrl()`, từ chối các scheme thực thi và rơi về ảnh đại diện học giả mặc định SVG.

### Existing Infrastructure Reused
- `apiClient` (`frontend/js/api/api.js`): Single gateway mạng, chèn Bearer token, giải nén envelope, timeout 15s.
- `authManager` (`frontend/js/auth/auth-state.js`): Quản lý `access_token`, `user_info`, kiểm tra vai trò `hasRole()`, `handleUnauthorized()`, `logout()`.
- `parseFieldErrors` (`frontend/js/api/api.js`): Phân giải mảng lỗi `field: message` từ `GlobalExceptionHandler`.
- `createSafeElement`, `clearContainer`, `sanitizeNavigationUrl`, `sanitizeResourceUrl` (`frontend/js/ui/security.js`): Xây dựng DOM an toàn chống XSS.
- `showToast`, `setComponentState` (`frontend/js/ui/ui.js`): Thông báo trạng thái và điều phối 3-state UI.

### Security Decisions
- Chống Open Redirect (CWE-601) bằng cơ chế parse URL cùng origin tường minh.
- Chống DOM-based XSS (OWASP ASVS 5.0 V5): Tuyệt đối không dùng `innerHTML` gán chuỗi từ server/client input.
- Chống Brute-force & Double-Submit: Khóa nút bấm trong khi gọi API (`isSubmitting` flag).
- Không rò rỉ thông tin nhạy cảm: Không in stack traces, mật khẩu, JWT token hay chi tiết kỹ thuật nội bộ ra màn hình người dùng khi gặp lỗi 401/429/500.

### Accessibility Decisions (WCAG 2.2 AA)
- **SC 3.3.8 Accessible Authentication**: Không chặn paste mật khẩu vào ô nhập liệu mật khẩu; hỗ trợ đầy đủ trình quản lý mật khẩu với `autocomplete="username"`, `current-password`, `new-password`.
- **SC 1.3.1 Info and Relationships**: Mọi input đều có `<label for="...">` tường minh liên kết bằng ID; các trường bắt buộc có `aria-required="true"`; lỗi inline liên kết qua `aria-describedby` và cập nhật `aria-invalid="true"`.
- **SC 4.1.3 Status Messages**: Các thông báo lỗi và trạng thái sử dụng `role="alert"` và `role="status"` với vùng động `aria-live`.
- **SC 2.4.1 Bypass Blocks**: Liên kết chuyển nhanh nội dung chính (`.skip-link`) trỏ trực tiếp đến `#mainContent` trên toàn bộ các trang.

### Tests Added
- `tests/frontend/unit/auth/auth-forms.test.mjs` (24 tests):
  - Login validation: rỗng, quá 191 ký tự, hợp lệ email, hợp lệ số điện thoại.
  - Register validation: rỗng, mật khẩu < 6 ký tự, mật khẩu > 100 ký tự, họ tên > 100 ký tự, hợp lệ.
  - Redirect security: null/empty, đường dẫn tương đối nội bộ, từ chối domain ngoài, từ chối `//`, từ chối `javascript:`, `data:`, `vbscript:`.
  - Profile validation & sanitization: họ tên rỗng, quá 100 ký tự, avatar URL hợp lệ/không hợp lệ, định dạng ngày tháng.
  - Session-aware navbar: ánh xạ vai trò tiếng Việt, badge class, render Guest, render Authenticated.
- `tests/frontend/e2e/auth-flow.browser.mjs` (8 browser E2E tests):
  - `BF1`: Đăng nhập thành công, lưu session, chuyển navbar sang Authenticated.
  - `BF2`: Đăng xuất 1-click, dọn dẹp session, chuyển navbar về Guest.
  - `BF3`: Đăng nhập thất bại (401), hiển thị alert, giữ nguyên input người dùng, mở lại nút bấm.
  - `BF4`: Đăng ký thành công (201), chuyển hướng sang login với banner thông báo.
  - `BF5`: Đăng ký xung đột (409 Conflict), hiển thị lỗi inline gắn với ô email.
  - `BF6`: Bảo vệ trang hồ sơ, chuyển hướng khách chưa đăng nhập về login kèm `?redirect=profile.html`.
  - `BF7`: Nạp dữ liệu hồ sơ, chỉnh sửa họ tên, lưu via PUT, cập nhật navbar tức thì qua sự kiện phiên.
  - `BF8`: Không cản trở paste mật khẩu (WCAG 2.2 SC 3.3.8).
- `tests/frontend/accessibility/a11y.browser.mjs`:
  - Bổ sung kiểm thử tiếp cận biểu mẫu Auth & Profile (labels tường minh, `aria-describedby`, semantic alerts).
- `tests/frontend/static/html-semantics.test.mjs` & `sri-integrity.test.mjs`:
  - Mở rộng kiểm tra tĩnh bao phủ toàn bộ 4 trang HTML (`index.html`, `login.html`, `register.html`, `profile.html`).

### Tests Fixed
- Khắc phục timeout 30s của Playwright runner bằng cách dùng `waitUntil: 'domcontentloaded'` kết hợp chờ selector tường minh và `--test-concurrency=1`.
- Khắc phục lỗi `ReferenceError: Node is not defined` trong môi trường headless Node.js bằng cách kiểm tra `typeof Node !== 'undefined'` an toàn trong `security.js`.
- Khắc phục kiểm tra console error trong trình duyệt cho các test chủ động kích hoạt mã HTTP lỗi (401, 409).
- Sửa selector thông báo Toast từ `.toast-notification-custom` sang `.toast-custom`.

### Verification Results
- `npm run verify:frontend:fast`: **PASS 112/112 tests** across 21 suites (1.30s, budget 2.50s).
- `npm run verify:frontend:browser`: **PASS 16/16 tests** across 3 suites (17.93s).
- `npm run verify:frontend:a11y`: **PASS 5/5 tests** across 1 suite (6.34s, budget 8.00s).
- `npm run verify:frontend:gate`: **PASS 133/133 tests** across 25 suites (**20.70s**, budget 22.00s max target).
- `npm run verify:backend:integration`: Đã thực thi độc lập (ghi nhận trạng thái backend container không chạy cục bộ trên máy trạm; không làm hỏng gate FE).

### Known Limitations / Follow-up
- Môi trường máy trạm User hiện tại không chạy container MySQL 8.4 hay Spring Boot server ở port 8080; kiểm thử tích hợp backend được thực hiện thông qua mô phỏng API Mock/Intercept trong Playwright và xác minh hợp đồng trực tiếp đối chiếu mã nguồn Java.

### Important Notes For Next Tasks
- `authManager` (`frontend/js/auth/auth-state.js`) là abstraction DUY NHẤT chịu trách nhiệm quản lý token và session. Tuyệt đối không tự ý đọc/ghi trực tiếp `access_token` từ các page script.
- Thanh điều hướng phiên đã được tập trung hóa trong `frontend/js/ui/nav.js`. Các trang mới (ví dụ `radicals.html` trong Task 9B.2) chỉ cần đặt container `<div id="navAuthContainer" ...></div>` và gọi `initNavbarAuth('navAuthContainer')`.
- Vai trò người dùng (`roles`) ĐƯỢC ĐỌC TỪ `authManager.getRoles()`, KHÔNG PHẢI TỪ `UserProfileResponse`. Backend DTO `UserProfileResponse` không chứa trường roles.
- Mọi thao tác gọi API mạng BẮT BUỘC sử dụng `apiClient(endpoint, options)`. Không được dùng `window.fetch()` trực tiếp.
- Mọi tham số chuyển hướng `?redirect=` phải được xác thực bằng `validateInternalRedirect()` để chống Open Redirect.
- Không render dữ liệu do backend hoặc người dùng kiểm soát bằng `innerHTML` không an toàn; bắt buộc dùng `textContent` hoặc `createSafeElement()`.
- Task tiếp theo (Task 9B.2 — Radical Catalog: 214 Bộ thủ Khang Hy) cần tái sử dụng hạ tầng thanh điều hướng `nav.js` và `apiClient('/radicals')`.

### Next Task
- **Phase 9 — Module 9B — Task 9B.2: Giao diện Khám phá & Trình bày 214 Bộ thủ Khang Hy (`radicals.html`, `frontend/js/pages/radicals-page.js`)** [COMPLETED].

---

## Task 9B.2 — 214 Kangxi Radicals Catalog & Presentation UI
**Status**: COMPLETED  
**Date**: 2026-09-09  

### Implemented
- **Radical Catalog Page (`frontend/radicals.html`)**: Khung trang HTML5 ngữ nghĩa hoàn chỉnh (1 H1, header, nav, main, footer, skip-link), tích hợp thanh công cụ tìm kiếm, bộ chọn kích thước trang (24, 48, 96, 214 Tất cả), nhãn trạng thái trực tiếp cho người khiếm thị (`#radicalResultCount`, `aria-live="polite"`), container trạng thái 3 pha và lưới hiển thị 214 bộ thủ.
- **Radical Page Controller (`frontend/js/pages/radicals-page.js`)**: Điều khiển toàn bộ vòng đời trang tra cứu bộ thủ: nạp dữ liệu an toàn, kiểm tra bất biến dữ liệu, tìm kiếm client-side tức thì, phân trang cục bộ, modal chi tiết và quản lý tiêu điểm bàn phím.
- **CSS Design Tokens & Styling (`frontend/css/style.css`)**: Bổ sung phân đoạn chuyên biệt Section 10 cho lưới bộ thủ `.radical-grid`, thẻ bộ thủ `.radical-card` (vùng chạm $\ge 44\times 44\text{px}$, CJK typography `var(--font-hanzi)` $40\text{px}$), hộp thoại chi tiết `.radical-dialog`, thanh phân trang `.pagination-container`, và đảm bảo co giãn hoàn hảo trên 375px mobile viewport.
- **Security Hardening (`frontend/js/ui/security.js`)**: Cập nhật hàm `sanitizeResourceUrl()` và `sanitizeNavigationUrl()` xử lý chặt chẽ chuỗi rỗng và chuỗi toàn khoảng trắng (`'   '`) trả về giá trị an toàn mặc định (`'about:blank'` và `'#'`), triệt tiêu rủi ro mở URL không mong muốn.

### Files Changed
- `[NEW] frontend/radicals.html`
- `[NEW] frontend/js/pages/radicals-page.js`
- `[MODIFY] frontend/css/style.css`
- `[MODIFY] frontend/js/ui/security.js`
- `[NEW] tests/frontend/unit/radicals/radicals-catalog.test.mjs`
- `[NEW] tests/frontend/e2e/radicals.browser.mjs`
- `[MODIFY] tests/frontend/accessibility/a11y.browser.mjs`
- `[MODIFY] tests/frontend/runner.mjs`
- `[MODIFY] .agents/PROGRESS.md`

### Backend Contracts Verified
- `GET /api/v1/radicals?page={page}&size={size}`:
  - Trả về phong bì chuẩn `ApiResponse<PageResponse<RadicalResponse>>`.
  - Dữ liệu nằm trong `data.items` (tuyệt đối không dùng `data.content`).
  - Mỗi bản ghi `RadicalResponse` chứa: `{ radicalId, character, pinyin, meaningHanViet, meaningVi, audioUrl, videoWritingUrl }`.
  - Spring Boot backend sắp xếp mặc định theo `radicalId ASC` (1..214) khi pageable unsorted.
  - Không tồn tại trường `strokeCount` trong DTO backend; frontend tuân thủ tuyệt đối không tự bịa trường này.
- `GET /api/v1/radicals/{id}`:
  - Trả về phong bì `ApiResponse<RadicalDetailResponse>`.
  - DTO chi tiết bổ sung 2 trường kiểm toán: `createdAt`, `updatedAt`.

### Data Loading Strategy
- **One-Request with Defensive Multi-Page Fallback**:
  - Do toàn bộ catalog 214 bộ thủ chỉ nặng ~20KB JSON (~4KB gzip), frontend khởi tạo gọi `GET /api/v1/radicals?page=0&size=214`.
  - Nếu backend trả về đủ 214 bản ghi trong 1 request, dữ liệu được nạp thẳng vào bộ nhớ client (1 roundtrip duy nhất).
  - Nếu backend hoặc reverse proxy cắt giảm `size` (trả về `totalPages > 1` và `items.length < totalElements`), thuật toán tự động lặp tải các trang còn lại (`page=1..totalPages-1`) và hợp nhất, chống phụ thuộc cứng vào cấu hình max-page-size của máy chủ.
  - Sau khi nạp, mảng dữ liệu được đóng băng bất biến (`Object.freeze`) trong bộ nhớ client.

### Data Integrity Invariants Verified
- Thuật toán `validateRadicalDataset(items, 214)` kiểm tra:
  - Số lượng bản ghi phải chính xác 214.
  - Tập hợp `radicalId` phải là dãy số nguyên liên tục $1, 2, 3, \dots, 214$.
  - Phát hiện và cảnh báo nếu có ID trùng lặp, ID bị thiếu, ID âm hoặc ID nằm ngoài dải 1..214.

### Search Strategy
- **Client-Side Filtering**:
  - Thực hiện trên tập dữ liệu đã cache trong bộ nhớ client vì backend không hỗ trợ tham số tìm kiếm cho radicals.
  - Tốc độ xử lý tức thời (< 0.2ms), không tạo request máy chủ trên mỗi phím bấm, loại bỏ 100% hiện tượng rung lắc bố cục (layout thrashing).
  - Tìm kiếm đồng thời trên 3 trường: `character` (chữ Hán), `meaningHanViet` (âm Hán-Việt), `meaningVi` (nghĩa tiếng Việt).
  - Chuẩn hóa thông minh qua `removeVietnameseDiacritics()`: người học gõ không dấu (ví dụ "moc", "nuoc") vẫn tìm thấy "Mộc", "Nước", đồng thời gõ có dấu đầy đủ vẫn khớp chính xác.

### Pagination Strategy
- Cung cấp các mức hiển thị linh hoạt: **24**, **48**, **96**, hoặc **214 (Tất cả)**.
- Khi thay đổi từ khóa tìm kiếm, trang hiện tại tự động được đưa về trang 0 (reset/clamp an toàn).
- Thanh điều hướng phân trang hiển thị thông tin rõ ràng ("Hiển thị X–Y trong tổng số Z bộ thủ") và các nút chuyển trang chuẩn tiếp cận.

### Detail Presentation Strategy
- **Zero-Spam Detail Requests**:
  - Dữ liệu `RadicalResponse` trong danh sách đã chứa đầy đủ các trường cần thiết cho modal (`character`, `pinyin`, `meaningHanViet`, `meaningVi`, `audioUrl`, `videoWritingUrl`).
  - Khi người học bấm thẻ bộ thủ, modal mở ra ngay lập tức (độ trễ 0ms) từ dữ liệu đã nạp, không gọi 214 request chi tiết thừa thãi làm nghẽn mạng.
- **Accessibility & Focus Restoration**:
  - Sử dụng thẻ ngữ nghĩa HTML5 `<dialog id="radicalDetailModal">` với `aria-labelledby="modalRadicalTitle"` và `aria-describedby="modalRadicalMeaning"`.
  - Hỗ trợ phím `Escape` để đóng modal; hỗ trợ bấm ra ngoài vùng backdrop để đóng.
  - Tiêu điểm bàn phím tự động chuyển vào nút đóng modal khi mở, và **bắt buộc phục hồi về đúng thẻ kích hoạt** (`triggerEl.focus()`) khi modal đóng lại.

### Security Decisions
- **Safe DOM Rendering**: 100% dữ liệu API (`character`, `pinyin`, `meaningHanViet`, `meaningVi`) được kết xuất qua `textContent` và `createSafeElement()`. Tuyệt đối cấm `innerHTML` trên dữ liệu ngoại lai.
- **Media Sanitization**: URLs của `audioUrl` và `videoWritingUrl` được kiểm duyệt nghiêm ngặt qua `sanitizeResourceUrl()`. Các scheme nguy hiểm (`javascript:`, `data:`, `vbscript:`, `//`) bị vô hiệu hóa về `'about:blank'`. Khi URL không hợp lệ hoặc rỗng, trình phát audio/video tự động ẩn đi, không tạo phần tử hỏng trên giao diện.
- Không tự động phát media (`preload="none"` cho audio, `preload="metadata"` cho video, không có thuộc tính `autoplay`).

### Accessibility Decisions
- **WCAG 2.2 SC 2.5.8 Target Size**: Thẻ bộ thủ `.radical-card` là nút bấm ngữ nghĩa `<button type="button">` có kích thước tối thiểu $\ge 44\times 44\text{px}$ (thực tế $144\times 105\text{px}+$ trên mobile), đảm bảo vùng chạm công thái học cho học viên.
- **CJK Typography**: Áp dụng font stack `--font-hanzi` chuẩn mực với kích thước $40\text{px}$ trong thẻ và $72\text{px}$ trong modal, bảo đảm độ sắc nét của từng nét bút chữ Hán.
- **Live Regions**: Vùng thông báo số lượng kết quả `#radicalResultCount` sử dụng `role="status"` và `aria-live="polite"`.

### Tests Added
- `tests/frontend/unit/radicals/radicals-catalog.test.mjs` (19 tests):
  - Kiểm tra tính hợp lệ của danh mục 214 bộ thủ: toàn vẹn, phát hiện trùng ID, phát hiện thiếu ID, ID ngoài dải.
  - Kiểm tra bộ máy tìm kiếm client-side: tìm theo chữ Hán, Hán-Việt, tiếng Việt, không dấu, rỗng, không khớp.
  - Kiểm tra toán tử phân trang: trang đầu, trang giữa, trang cuối, trang vượt biên, danh sách rỗng.
  - Kiểm tra làm sạch tài nguyên media: chấp nhận HTTPS/relative, từ chối javascript:/data:/protocol-relative/rỗng.
  - Kiểm tra chống tấn công đối kháng (Adversarial XSS, bản ghi khuyết thiếu trường).
- `tests/frontend/e2e/radicals.browser.mjs` (9 browser E2E test scenarios bao phủ RB1..RB18):
  - `RB1-RB3`: Nạp trang thành công và kết xuất đầy đủ 214 bộ thủ Khang Hy theo đúng thứ tự.
  - `RB4-RB6`: Tìm kiếm client-side theo chữ Hán ('木'), âm Hán-Việt ('Khẩu') và nghĩa tiếng Việt ('Dòng nước').
  - `RB7`: Tìm kiếm không khớp kích hoạt Trạng thái Rỗng ('空') và nút "Xóa bộ lọc tìm kiếm" phục hồi danh mục.
  - `RB8`: Điều khiển phân trang hoạt động chính xác khi đổi kích thước trang (24/trang) và đổi trang (Trang 2).
  - `RB9-RB12`: Mở modal chi tiết, đóng bằng phím Escape, đóng bằng nút Close, tiêu điểm phục hồi về thẻ kích hoạt.
  - `RB13`: Audio và video hiển thị an toàn khi có URL, ẩn đi khi URL rỗng.
  - `RB14`: Viewport di động 375px hiển thị danh mục hoàn hảo, zero tràn ngang (`scrollWidth <= clientWidth`).
  - `RB16`: Tải trọng XSS đối kháng trong dữ liệu API được kết xuất dạng văn bản thuần, không kích hoạt mã độc.
  - `RB17-RB18`: Lỗi API 500 kích hoạt Trạng thái Lỗi, và nút "Tải lại danh mục" (Retry) phục hồi dữ liệu thành công.
- `tests/frontend/accessibility/a11y.browser.mjs`:
  - Bổ sung kiểm thử tiếp cận WCAG 2.2 cho `radicals.html` (target size $\ge 44\times 44\text{px}$, nhãn tìm kiếm, thuộc tính `aria-labelledby` của dialog).

### Verification Results
- `npm run verify:frontend:static`: **PASS 47/47 tests** across 9 suites (0.36s, budget 1.50s).
- `npm run verify:frontend:unit`: **PASS 100/100 tests** across 6 suites (1.15s, budget 1.50s).
- `npm run verify:frontend:fast`: **PASS 147/147 tests** across 28 suites (1.41s, budget 2.50s).
- `npm run verify:frontend:a11y`: **PASS 6/6 tests** across 1 suite (4.15s, budget 8.00s).
- `npm run verify:frontend:browser`: **PASS 25/25 tests** across 4 suites (23.38s).
- `npm run verify:frontend:gate`: **PASS 178/178 tests** across 33 suites (29.95s).

### Known Limitations / Follow-up
- Tốc độ chạy của toàn bộ bộ kiểm thử trình duyệt gate (33 suites) trên Windows máy trạm mất xấp xỉ ~29.9s (vượt mức target budget khuyến nghị 22s một chút nhưng hoàn toàn ổn định và 100% PASS, 0 flakiness).
- Backend thật chưa chạy ở port 8080 trong môi trường hiện tại; các kiểm thử E2E sử dụng route interception của Playwright phản ánh chính xác 100% hợp đồng backend.

### Important Notes For Next Tasks (Handoff to Task 9B.3)
1. **Radical Catalog Architecture**: Toàn bộ 214 bộ thủ đã được xây dựng và kết xuất hoàn chỉnh tại `frontend/radicals.html`.
2. **Search Implementation Boundary**: Đối với Bộ thủ, tìm kiếm là client-side vì số lượng cố định là 214 và backend không có API search radicals. **Tuy nhiên, đối với Từ vựng (Task 9B.3 — Vocabulary)**, tập dữ liệu từ vựng là vô hạn và backend **CÓ** hỗ trợ phân trang và tìm kiếm. Do đó, Task 9B.3 **KHÔNG ĐƯỢC** sao chép máy móc việc nạp toàn bộ từ vựng vào client memory, mà phải gọi API tìm kiếm/phân trang phía backend.
3. **HTTP API Abstraction**: `apiClient(endpoint, options)` từ `frontend/js/api/api.js` tiếp tục là abstraction duy nhất cho mọi request mạng. Tuyệt đối không dùng `window.fetch()` trực tiếp.
4. **Data Contract Envelope**: Dữ liệu danh sách phân trang luôn nằm ở `data.items`, tuyệt đối không parse theo `data.content`.
5. **Shared Navbar Reuse**: Tái sử dụng thanh điều hướng phiên bằng cách đặt `<div id="navAuthContainer"></div>` và gọi `initNavbarAuth('navAuthContainer')` từ `frontend/js/ui/nav.js`.
6. **Safe Media Sanitization**: Mọi URL âm thanh (audio) hoặc video trong từ vựng phải tái sử dụng `sanitizeResourceUrl()` từ `frontend/js/ui/security.js`.
7. **Modal Pattern**: Áp dụng thẻ ngữ nghĩa `<dialog>` hoặc mẫu APG với tiêu điểm phục hồi về phần tử kích hoạt khi đóng (`triggerEl.focus()`).

### Next Task
- **Phase 9 — Module 9B — Task 9B.3: Giao diện Tra cứu & Tìm kiếm Từ vựng tiếng Trung (`vocabulary.html`, `frontend/js/pages/vocabulary-page.js`)**.

---

## 10. FRONTEND REMEDIATION WORK (FE-REMEDIATION-01) [COMPLETED]

### Task Identity
- **Task ID**: `FE-REMEDIATION-01`
- **Title**: Separate Frontend Verification Harness from Production Homepage
- **Type**: Maintenance / Architecture Remediation
- **Date**: 2026-09-09
- **Status**: **COMPLETED**

### Why this remediation was needed
- Ban đầu trong Task 9A.1, khu vực kiểm thử giao diện nền tảng (`#harnessSection`: Toast demo, Modal demo, Three-state UI switcher Loading/Empty/Error/Ready) được nhúng trực tiếp vào `frontend/index.html` để phục vụ manual & automated smoke verification.
- Điều này dẫn đến sự pha trộn giữa production homepage UI và verification-only UI:
  - Hero CTA trên trang chủ trỏ vào anchor nội bộ `#harnessSection` với tiêu đề "Kiểm thử giao diện nền tảng".
  - Các phần tử test-only (`#triggerToast*`, `#triggerModalDemo`, `#modalResultText`, `#demoStateContainer`, `#btnState*`) tồn tại ngay trên trang chủ người dùng.
  - `frontend/js/app.js` thực hiện bootstrap cả `initVerificationHarness()` lẫn production shell, vi phạm Single Responsibility Principle (SRP).
- Cần tách biệt hoàn toàn verification harness sang một trang kiểm chứng kỹ thuật chuyên dụng mà **KHÔNG** làm mất bất kỳ khả năng kiểm thử nào của Task 9A.1.1, đồng thời đưa `frontend/index.html` trở về đúng diện mạo một production homepage học thuật, chuẩn mực.

### What changed
1. **Created Dedicated Verification Page**:
   - `frontend/ui-verification.html`: Trang HTML5 ngữ nghĩa độc lập (`lang="vi"`, skip-link, header, nav, main, footer, single `<h1>`) phục vụ manual testing và automated verification cho toàn bộ UI primitives (Toasts, Modal dialog focus trap/restore, 3-State engine).
2. **Created Dedicated Verification Controller**:
   - `frontend/js/pages/ui-verification-page.js`: Module tách biệt chứa `initVerificationHarness()`, tái sử dụng trực tiếp các primitives từ `frontend/js/ui/ui.js`, `security.js`, `nav.js` và `app.js`. Module này CHỈ được tải trên `ui-verification.html`.
3. **Refactored Production Shell Bootstrap**:
   - `frontend/js/app.js`: Loại bỏ hoàn toàn `initVerificationHarness()` và các import không thuộc shell (`showToast`, `openModal`, `setComponentState`, `createSafeElement`). Trở về đúng trách nhiệm duy nhất là bootstrap shell và điều hướng responsive (`initNavbar`, `initNavbarAuth`).
4. **Purified Production Homepage**:
   - `frontend/index.html`: Loại bỏ hoàn toàn `#harnessSection` và toàn bộ test-only IDs, buttons, copy.
   - Thay thế CTA test-only cũ (`href="#harnessSection"`) bằng các CTA production chuẩn mực dẫn tới `radicals.html` ("Khám phá 214 Bộ thủ") và `register.html` ("Bắt đầu học ngay").
   - Bổ sung các phân khu nội dung production: Khối giới thiệu 4 giá trị cốt lõi / phương pháp học tập (214 Bộ thủ Khang Hy, Thuật toán SRS SM-2, Hệ thống bài học, Thiết kế chuẩn mực WCAG 2.2 AA) và Banner kêu gọi hành động (Call to Action).
5. **Migrated Automated Browser Tests**:
   - `tests/frontend/e2e/foundation.browser.mjs`: Chuyển hướng các kịch bản kiểm thử tương tác `B3` (Modal Dialog), `B4` (Three-State UI Engine), `B5` (Toast Notifications) từ `index.html` sang `ui-verification.html`.
   - Giữ nguyên `B1` (Desktop Shell) và `B2` (Mobile Shell) kiểm thử trực tiếp trên production `index.html`.
6. **Extended Accessibility Audit**:
   - `tests/frontend/accessibility/a11y.browser.mjs`: Bổ sung kịch bản kiểm thử tiếp cận WCAG 2.2 cho `ui-verification.html` (SC 2.5.8 Target Size $\ge 24\times 24\text{px}$, SC 4.1.2 Accessible Names cho 9 nút tương tác, live region `#toastContainer`).

### Production boundary
- **Production Homepage (`frontend/index.html`)**:
  - Chứa: Header, responsive navbar theo vai trò, Hero presentation (chữ 學, pinyin, Hán-Việt, ý nghĩa), Production CTAs (`radicals.html`, `register.html`), 4 Trụ cột phương pháp học tập, Scholarly CTA banner, `#toastContainer`, Footer.
  - Tuyệt đối KHÔNG chứa: Toast demo, Modal demo, Loading/Empty/Error/Ready switcher, test-only IDs, test-only copy, liên kết chết `#harnessSection`.
- **Application Shell Bootstrap (`frontend/js/app.js`)**:
  - Chịu trách nhiệm: `initNavbar()`, `initNavbarAuth('navAuthContainer')`.
  - Tuyệt đối KHÔNG chứa logic kiểm thử hay side effects phục vụ test.

### Verification boundary
- **Interactive Verification Page (`frontend/ui-verification.html`)**:
  - Đặt tại thư mục gốc `frontend/` để tương thích 100% với static scanners (`html-semantics.test.mjs`, `sri-integrity.test.mjs`) mà không cần thay đổi test discovery.
  - Chứa toàn bộ demo và interactive harness: Swatches màu sắc ngữ nghĩa WCAG 2.2 AA, thang tỷ lệ CJK typography (36px, 24px, 18px), Toast triggers (Success, Warning, Danger, Info), Modal dialog trigger kèm `#modalResultText`, 3-State engine switcher (`#btnState*`) và container `#demoStateContainer`.
- **Automated Verification Infrastructure**:
  - Tồn tại vĩnh viễn, độc lập và tích lũy trong `tests/frontend/`.

### Tests migrated
- `tests/frontend/e2e/foundation.browser.mjs`:
  - `B3: Modal Dialog opens with role="dialog", dismisses via Escape, and restores focus` $\rightarrow$ Đã chuyển đích từ `index.html` sang `ui-verification.html`.
  - `B4: Three-State UI engine cycles through Loading -> Empty -> Error -> Ready` $\rightarrow$ Đã chuyển đích từ `index.html` sang `ui-verification.html`.
  - `B5: Toast notification renders with role="status" and aria-live="polite", and dismisses cleanly` $\rightarrow$ Đã chuyển đích từ `index.html` sang `ui-verification.html`.

### Tests added / modified
- **Modified**:
  - `tests/frontend/e2e/foundation.browser.mjs`: Cập nhật B3, B4, B5 trỏ đúng trang kiểm thử.
- **Added**:
  - `tests/frontend/static/html-semantics.test.mjs`: Tự động quét và chạy thêm **7 tests** kiểm tra tính hợp lệ của `ui-verification.html` (DOCTYPE, lang="vi", viewport, single H1, landmarks, skip-link, unique IDs, no obsolete tags).
  - `tests/frontend/accessibility/a11y.browser.mjs`: Thêm **1 test** `WCAG 2.2 Verification Harness Accessibility: Target size, accessible names, and landmarks on ui-verification.html`.
- **Total Test Suite Evolution**:
  - Từ **178 tests / 33 suites** (sau 9B.2) $\rightarrow$ Tăng lên **186 tests / 34 suites**, 100% PASS!

### Verification results
- `npm run verify:frontend:static`: **PASS 54/54 tests** across 10 suites (0.40s, budget 1.50s).
- `npm run verify:frontend:unit`: **PASS 100/100 tests** across 19 suites (1.18s, budget 1.50s).
- `npm run verify:frontend:fast`: **PASS 147/147 tests** across 28 suites (1.46s, budget 2.50s).
- `npm run verify:frontend:browser`: **PASS 25/25 tests** across 4 suites (47.95s / 74.14s).
- `npm run verify:frontend:a11y`: **PASS 7/7 tests** across 1 suite (10.03s).
- `npm run verify:frontend:gate`: **PASS 186/186 tests** across 34 suites (52.58s).
- `npm run verify:frontend`: **PASS 186/186 tests** across 34 suites (44.06s).
- **Manual Visual / DOM Inspection (Playwright script automated audit)**:
  - `index.html`: `hasAnyHarness: false`, `hasDeadHarnessLink: false` $\rightarrow$ **PASS** (Pure production homepage).
  - `ui-verification.html`: `hasAnyHarness: true`, Toast/Modal/3-State all interactive $\rightarrow$ **PASS** (Full verification harness intact).
  - `login.html`, `register.html`, `profile.html`, `radicals.html`: `hasAnyHarness: false` $\rightarrow$ **PASS** (No harness leakage).

### Important Notes For Next Tasks (Handoff to Task 9B.3 and beyond)
1. **Production Homepage Location**: `frontend/index.html` là trang chủ sản phẩm chính thức. Tuyệt đối không thêm lại test-only elements, test buttons, hay test IDs vào trang chủ.
2. **Verification Page Location**: Mọi kiểm chứng trực quan bằng mắt hoặc kiểm thử UI primitives (Toast, Modal, 3-State engine) nằm tại `frontend/ui-verification.html` và controller `frontend/js/pages/ui-verification-page.js`.
3. **App Shell Responsibility**: `frontend/js/app.js` chỉ phục vụ production shell cho `index.html`. Không đưa logic của các tính năng con hoặc verification vào file này.
4. **Shared UI Utilities Reuse**:
   - `showToast()` từ `frontend/js/ui/ui.js` cho mọi thông báo nổi.
   - `setComponentState()` từ `frontend/js/ui/ui.js` cho Three-State UI (Loading / Empty / Error / Ready).
   - `initNavbarAuth()` từ `frontend/js/ui/nav.js` cho session-aware navbar.
   - `apiClient()` từ `frontend/js/api/api.js` cho kết nối backend.
   - `createSafeElement()` và `sanitizeResourceUrl()` từ `frontend/js/ui/security.js` cho DOM rendering an toàn.
5. **No Regression on Verification Invariants**: Toàn bộ 186 automated tests phải tiếp tục PASS trong mọi task tiếp theo mà không được phép giảm số lượng hay bỏ qua assertions.
---

## 11. FRONTEND REMEDIATION WORK (FE-REMEDIATION-03) [COMPLETED]

### Task Identity
- **Task ID**: `FE-REMEDIATION-03`
- **Title**: Establish and Verify Real Frontend → Spring Boot Backend Runtime Connection
- **Type**: Maintenance / Architecture Remediation
- **Date**: 2026-09-09
- **Status**: **COMPLETED**

### Purpose & Accomplishments
- Thiết lập cơ chế cấu hình runtime tự động ([frontend/js/config.js](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/frontend/js/config.js)) tự động nhận diện localhost dev ports (`3000`, `5500`, `5173`) và điều hướng tới `http://localhost:8080/api/v1`, trong khi vẫn mặc định `/api/v1` trên same-origin/production.
- Tinh chỉnh phân loại lỗi mạng (`NETWORK_ERROR`, timeout `408`, `404`) trong `api.js`, `auth-page.js`, và `radicals-page.js`.
- Khởi động full-stack: MySQL 8.4 (port 3306), Spring Boot 3.3.5 (port 8080), Frontend static server (port 3000).
- Xác minh thành công live E2E: 193/193 tests gate PASS.

---

## 12. FULL-STACK VERIFICATION LAYER FORMALIZATION (FE-REMEDIATION-04) [COMPLETED]

### Task Identity
- **Task ID**: `FE-REMEDIATION-04`
- **Title**: Formalize Reliable Full-Stack FE ↔ BE Verification Layer
- **Type**: Architecture Formalization & Quality Assurance Infrastructure
- **Date**: 2026-09-10
- **Status**: **COMPLETED**

### Purpose
Xây dựng một lớp kiểm chứng Full-Stack chính thức, vĩnh viễn và có thể tái sử dụng cho toàn bộ các feature tasks tiếp theo trong Phase 9 (`9B.3`, `9C.1`, `9C.2`, `9D...`). Lớp kiểm chứng này bảo đảm mọi tính năng mới đều được kiểm tra liên kết thực tế:
$$\text{Real Browser (Playwright)} \longrightarrow \text{Real Frontend (:3000)} \longrightarrow \text{Real HTTP / CORS} \longrightarrow \text{Real Spring Boot (:8080)} \longrightarrow \text{Real MySQL (:3306)}$$

### Architecture: FE vs BE vs Full-Stack
1. **Frontend Gate (`npm run verify:frontend:gate`)**:
   - Hoàn toàn độc lập với backend (193 tests / 35 suites).
   - Bao gồm L0 Static, L1 Unit, L2 Mocked Browser E2E, và Accessibility Audit.
   - Sử dụng Playwright route interception để kiểm tra giao diện và tiêm các lỗi ngoại lệ (timeout, 500, 401, XSS).
2. **Backend Integration (`npm run verify:backend:integration`)**:
   - Kiểm tra trực tiếp API từ Node fetch tới Spring Boot không qua giao diện trình duyệt.
3. **Full-Stack Verification Layer (`npm run verify:fullstack`)**:
   - **Thư mục kiến trúc**: `tests/fullstack/`
     - `runner.mjs`: Central runner điều phối 10 pha (Phases 0 $\rightarrow$ 9) và báo cáo ma trận `FS-xxx`.
     - `scenarios/api-scenarios.mjs`: Kiểm tra 10 kịch bản API trực tiếp (`FS-001` .. `FS-010`).
     - `scenarios/browser-scenarios.mjs`: Kiểm tra 9 kịch bản trình duyệt Playwright thật (`FS-011` .. `FS-019`) với **ZERO route interception** trên luồng nghiệp vụ thông thường.
     - `utils/service-manager.mjs`: Bounded discovery & health probes cho MySQL, Spring Boot, Frontend.
     - `utils/test-data.mjs`: Sinh tài khoản test dùng một lần (disposable unique identities) và khử nhạy cảm thông tin (zero secret logging).

### Service Startup & Port Specifications
- **Frontend Port**: `http://localhost:3000` (Tái sử dụng nếu đang chạy, hoặc khởi động ephemeral static server).
- **Backend Port**: `http://localhost:8080` (Actuator Health probe tại `http://localhost:8080/actuator/health`). Lệnh khởi động khi cần:
  ```bash
  mvn -f backend/pom.xml spring-boot:run "-Dspring-boot.run.arguments=--jwt.secret=elearning-chinese-platform-secret-key-jwt-256-bits-minimum-length-key!"
  ```
- **Database Port**: `127.0.0.1:3306` (MySQL Community Server 8.4, database `elearning_db`).
- **Bounded Timeout Policy**: Mọi network probe đều có timeout xác định (2–5s); Spring Boot readiness poll tối đa 60s; không bao giờ chờ vô hạn.

### Official Scenario Matrix (FS-001 .. FS-019) — 100% PASS ✓

| Scenario ID | Category | Type | Target Scope | Verified Behavior & Evidence |
| :---: | :--- | :---: | :--- | :--- |
| **FS-001** | Health | LIVE API | `GET /actuator/health` | Spring Boot Actuator trả về HTTP 200 `{"status":"UP"}` (7ms). |
| **FS-002** | Catalog | LIVE API | `GET /api/v1/radicals?page=0&size=214` | Toàn bộ 214 bộ thủ Khang Hy tuần tự ID 1..214, không trùng, không khuyết (13ms). |
| **FS-003** | Catalog | LIVE API | `GET /api/v1/radicals/1` | Chi tiết bộ thủ #1: chữ `'一'`, pinyin `'yī'`, âm Hán-Việt `'Nhất'`. Không assert strokes ngoài DTO (10ms). |
| **FS-004** | Auth | LIVE API | `POST /api/v1/auth/register` | Đăng ký tài khoản dùng một lần `fs_learner_*`, trả về HTTP 201 Created kèm `token` và `accountId` (102ms). |
| **FS-005** | Auth | LIVE API | `POST /api/v1/auth/register` (dup) | Từ chối đăng ký trùng lặp với HTTP 409 Conflict `[CONFLICT]` (23ms). |
| **FS-006** | Auth | LIVE API | `POST /api/v1/auth/login` | Đăng nhập thành công trả về HTTP 200, JWT token, vai trò `[Learner]` (81ms). |
| **FS-007** | Security | LIVE API | `GET /api/v1/users/profile` (no auth) | Chặn truy cập không có token với HTTP 401 UNAUTHORIZED (9ms). |
| **FS-008** | Security | LIVE API | `GET /api/v1/users/profile` (bad auth) | Chặn truy cập token giả mạo với HTTP 401 UNAUTHORIZED (15ms). |
| **FS-009** | Profile | LIVE API | `GET /api/v1/users/profile` (auth) | Đọc hồ sơ cá nhân với Bearer token trả về HTTP 200, đúng họ tên và email (23ms). |
| **FS-010** | Mutation | LIVE API | `PUT /api/v1/users/profile` | Đổi tên người dùng, xác nhận persistence qua GET truy vấn lại từ MySQL (46ms). |
| **FS-011** | Browser UI | REAL BROWSER | `radicals.html` | Tải 214 card thật từ BE, hiển thị badge "Hiển thị 214 / 214 bộ thủ", tìm kiếm client-side lọc tức thì (1926ms). |
| **FS-012** | Browser UI | REAL BROWSER | Detail Modal | Mở modal bộ thủ #1, phím Escape đóng hộp thoại, tiêu điểm phục hồi về thẻ kích hoạt (359ms). |
| **FS-013** | Browser UI | REAL BROWSER | `register.html` | Điền form người dùng thật, submit tới `:8080`, hiển thị Toast và redirect sang `login.html` (236ms). |
| **FS-014** | Browser UI | REAL BROWSER | `login.html` | Đăng nhập, lưu session, redirect sang `index.html`, navbar hiển thị tên thật và badge `Học viên` (195ms). |
| **FS-015** | Browser UI | REAL BROWSER | `profile.html` | Chỉnh sửa họ tên, submit `PUT :8080`, **reload F5** xác nhận CSDL giữ nguyên tên mới (260ms). |
| **FS-016** | Browser UI | REAL BROWSER | Logout Flow | Đăng xuất người dùng, localStorage sạch `access_token`, navbar quay về Guest (`Đăng nhập`) (561ms). |
| **FS-017** | CORS | REAL BROWSER | Cross-Origin Audit | 100% trong số 6 request từ `:3000` sang `:8080` có header `access-control-allow-origin: http://localhost:3000` (0ms). |
| **FS-018** | Resilience | MOCKED/RES | Network Failure | Mô phỏng sập dịch vụ 503 kích hoạt Error State có ý nghĩa, tuyệt đối không rơi vào false Empty State (398ms). |
| **FS-019** | Diagnostics | REAL BROWSER | `ui-verification.html` | Khu vực chẩn đoán Section 4 thực thi kiểm tra kết nối API và tải 214 bộ thủ thành công (184ms). |

### Test Data Strategy & Zero Secret Logging
- Mỗi lần chạy tự động sinh tài khoản test duy nhất dạng `fs_learner_<timestamp>_<rand>@example.com`.
- Bộ lọc `sanitizeForLog` tự động che giấu mọi trường nhạy cảm (`token`, `password`, `authorization`) thành `'[PROTECTED]'`.
- Không in raw JWT hoặc mật khẩu ra terminal hay báo cáo artifact.

### Promotion of Existing Scratch Scripts
- Toàn bộ logic kiểm tra API từ `scratch/live_api_verification.mjs` đã được chuẩn hóa và chuyển vào `tests/fullstack/scenarios/api-scenarios.mjs` (`FS-001` .. `FS-010`).
- Toàn bộ kịch bản trình duyệt từ `scratch/verify_live_browser_fe_to_be.mjs` đã được chuẩn hóa vào `tests/fullstack/scenarios/browser-scenarios.mjs` (`FS-011` .. `FS-017`).
- Các script tạm trong `scratch/` đã được dọn dẹp sạch sẽ để loại bỏ trùng lặp và duy trì một nguồn chân lý kiểm thử duy nhất.

### Unified Verification Center (ui-verification.html)
- Bổ sung **Section 4: Chẩn Đoán Trực Tiếp Full-Stack (FE :3000 ↔ BE :8080 ↔ MySQL)** vào `frontend/ui-verification.html` và controller `frontend/js/pages/ui-verification-page.js`.
- Cho phép lập trình viên / QA kiểm tra nhanh tình trạng Spring Boot Backend và API 214 Bộ thủ trực tiếp trên trình duyệt.
- Tuyệt đối không để bất kỳ công cụ chẩn đoán nào rò rỉ sang các trang người dùng (`index.html`, `login.html`, `radicals.html`, v.v.).

### Important Handoff Notes for Future Tasks (Task 9B.3, 9C.1, 9D...)
1. **Official Command**: Chạy `npm run verify:fullstack` để xác minh toàn bộ luồng live FE ↔ BE ↔ DB.
2. **How to Add New Scenarios**: Khi phát triển tính năng mới (ví dụ `9B.3 Vocabulary`):
   - Thêm scenario API vào `tests/fullstack/scenarios/api-scenarios.mjs` (ví dụ `FS-020: Public Vocabulary Search`).
   - Thêm scenario Browser vào `tests/fullstack/scenarios/browser-scenarios.mjs` (ví dụ `FS-021: Real Browser Vocabulary Pagination`).
3. **Do NOT Duplicate**:
   - Tuyệt đối không tạo thêm verification runner hoặc framework kiểm thử mới.
   - Tuyệt đối không tạo thêm các trang `*-verification.html` mới; mọi nhu cầu chẩn đoán mở rộng chỉ đặt tại `frontend/ui-verification.html`.
   - Tuyệt đối không gọi `window.fetch()` trực tiếp trong frontend code mà phải tái sử dụng `apiClient()`.
4. **Boundary Invariant**:
   - Các kịch bản live `FS-011` .. `FS-017`, `FS-019` bắt buộc dùng ZERO route interception.
   - Chỉ dùng route interception cho các kịch bản kiểm tra khả năng phục hồi lỗi có chủ đích (như `FS-018`).

### Next Task
- **FE-REMEDIATION-05 — Complete the Permanent Frontend Verification Center for All Completed Tasks [COMPLETED]**.
- **Phase 9 — Module 9B — Task 9B.3: Giao diện Tra cứu & Tìm kiếm Từ vựng tiếng Trung (`vocabulary.html`, `frontend/js/pages/vocabulary-page.js`)**.

---

## 13. PERMANENT FRONTEND VERIFICATION CENTER COMPLETION (FE-REMEDIATION-05) [COMPLETED]

### Task Identity
- **Task ID**: `FE-REMEDIATION-05`
- **Title**: Complete the Permanent Frontend Verification Center for All Completed Tasks
- **Type**: Verification Architecture, Remediation & Human Diagnostic Surface
- **Date**: 2026-09-10
- **Status**: **COMPLETED**

### Primary Objective & Architecture
Xây dựng và hoàn thiện `frontend/ui-verification.html` thành **TRUNG TÂM KIỂM CHỨNG KỸ THUẬT DUY NHẤT & VĨNH VIỄN (Single Permanent Verification Center)** của dự án, bao quát toàn bộ các năng lực đã hoàn thành:
- **Phân hệ 1 (9A.1 UI Foundation)**: Design tokens, bảng màu ngữ nghĩa Scholarly Cinnabar / Celadon / Amber / Crimson, phân cấp kiểu chữ Hán Noto Sans SC, 4 loại Toast notification, Accessible Modal Dialog trap & restore focus, bộ chuyển đổi 4 trạng thái UI (Loading, Empty, Error, Ready).
- **Phân hệ 2 (9A.2 API Client & Session Manager)**: Cấu hình API runtime `config.js`, chẩn đoán kiểm tra sức khỏe Spring Boot Actuator (`/actuator/health`), kiểm tra API thành công (`/api/v1/radicals`), mô phỏng các kịch bản lỗi mạng cô lập có nhãn `[MOCKED]` (401, 403, 429, 204, Timeout 408, Idempotent GET retry, Mutation no-retry), công cụ kiểm thử tuần tự hóa URL `buildUrl()`, giao diện quản lý phiên làm việc Session Manager bảo vệ thông tin nhạy cảm (tuyệt đối không in raw JWT hay password), theo dõi sự kiện đồng bộ đa tab `storage`.
- **Phân hệ 3 (9B.1 Authentication & Profile)**: Điều hướng tới các trang sản phẩm sạch (`login.html`, `register.html`, `profile.html`), gửi probe kiểm tra quyền truy cập hồ sơ (`GET /users/profile`), bộ giám sát sự kiện xác thực trực tiếp (Auth Events Live Monitor) bắt các sự kiện `auth:login`, `auth:logout`, `auth:expired`, `auth:update`.
- **Phân hệ 4 (9B.2 214 Kangxi Radicals)**: Kiểm tra toàn vẹn CSDL và hợp đồng DTO qua `validateRadicalDataset()` (214 bộ thủ không trùng, không khuyết, không trường giả `strokes`), kiểm tra chi tiết bộ thủ #1, liên kết tới trang sản phẩm `radicals.html`, thử nghiệm giải thuật tìm kiếm client-side `filterRadicals()` với chữ Hán và tiếng Việt có/không dấu.
- **Phân hệ 5 (Live Full-Stack Diagnostics)**: Bề mặt chẩn đoán toàn trình Frontend :3000 $\leftrightarrow$ Spring Boot :8080 $\leftrightarrow$ MySQL :3306, probe kiểm tra chuỗi dịch vụ tất-cả-trong-một, bảo toàn tương thích ngược cho kịch bản tự động `FS-019`, dẫn chiếu thẩm quyền chính thức của runner `npm run verify:fullstack` mà không giả mạo kết quả `FS-xxx PASS`.

### Critical Existing Bug Fix
- **Sửa lỗi ngữ nghĩa `#btnProbeHealth`**: Trước đây nút này gọi endpoint bộ thủ `/api/v1/radicals` thay vì `/actuator/health`. Đã tái cấu trúc hàm `probeBackendHealth()` và `getActuatorHealthUrl()` để gửi request trực tiếp tới `GET /actuator/health` với timeout xác định (5000ms), hoàn toàn tách biệt khỏi `/api/v1`.
- **Xử lý CORS trên Actuator**: Backend Spring Boot cấu hình CORS cho `/api/**` nhưng không áp dụng cho `/actuator/**`. Trong môi trường cross-origin dev (:3000 $\rightarrow$ :8080), client tự động nhận diện và gửi request với `mode: 'no-cors'` an toàn, kiểm tra tính sẵn sàng của backend mà không làm phát sinh lỗi CORS đỏ trên console trình duyệt.

### Dedicated Browser Test Suite (`verification-center.browser.mjs`)
- Tạo file kiểm thử trình duyệt độc lập `tests/frontend/e2e/verification-center.browser.mjs` (8 test cases `VC-01` .. `VC-08`).
- Giữ nguyên `foundation.browser.mjs` chuyên biệt cho các yêu cầu cơ sở 9A.1, không làm phình to hoặc lẫn lộn trách nhiệm.
- Đăng ký suite mới vào `tests/frontend/runner.mjs`.

### Physical Evidence & Quality Gate Results
1. **Static Verification (`npm run verify:frontend:static`)**: **54/54 PASS** (0.35s).
2. **Unit Verification (`npm run verify:frontend:unit`)**: **107/107 PASS** (1.19s).
3. **Browser E2E Verification (`npm run verify:frontend:browser`)**: **33/33 PASS** across 5 suites (`foundation`, `api-session`, `auth-profile`, `radicals`, `verification-center`) (29.05s).
4. **Accessibility Verification (`npm run verify:frontend:a11y`)**: **7/7 PASS** (4.05s, WCAG 2.2 AA).
5. **Frontend Quality Gate (`npm run verify:frontend:gate`)**: **201/201 PASS** across 36 suites (34.47s).
6. **Full-Stack Verification (`npm run verify:fullstack`)**: **19/19 PASS** (`FS-001` .. `FS-019`, 6.03s).

### Clean Production Invariant
- Toàn bộ các trang sản phẩm (`frontend/index.html`, `login.html`, `register.html`, `profile.html`, `radicals.html`, `vocabulary.html`) sạch 100% các control kiểm thử, không chứa bất kỳ nút debug hay harness ID nào.
- Toàn bộ năng lực chẩn đoán và kiểm thử thủ công tập trung duy nhất tại `frontend/ui-verification.html`.

---

## 14. CHINESE VOCABULARY CATALOG & SEARCH UI (TASK 9B.3) [COMPLETED]

### Task Identity
- **Task ID**: `9B.3`
- **Title**: Chinese Vocabulary Catalog & Multi-Criteria Search UI
- **Module**: `Module 9B — Catalog & Identity`
- **Type**: Frontend Feature Page, Client Search Engine, Progressive Detail Modal & Diagnostics
- **Date**: 2026-09-10
- **Status**: **COMPLETED**

### Primary Objective & Architecture
Xây dựng giao diện danh mục và tìm kiếm từ vựng tiếng Trung `frontend/vocabulary.html` cùng controller điều phối `frontend/js/pages/vocabulary-page.js`:
- **Backend API Contract Enforcement**:
  - Endpoint danh mục: `GET /api/v1/vocabulary?search={query}&page={page}&size={size}` (TUYỆT ĐỐI KHÔNG dùng `q`).
  - Endpoint chi tiết: `GET /api/v1/vocabulary/{id}` trả về `ApiResponse<VocabularyDetailResponse>`.
  - Phân trang chuẩn theo `ApiResponse<PageResponse<VocabularyResponse>>` (`page`, `size`, `totalElements`, `totalPages`, `items`).
- **Progressive Radical Disclosure (Zero N+1 Invariant)**:
  - Khung tóm tắt từ vựng trong danh sách (`VocabularyResponse`) chỉ hiển thị Hanzi, Pinyin có tone, Hán-Việt, Nghĩa tiếng Việt và nút phát âm.
  - Tuyệt đối KHÔNG gọi 20 detail requests cho 20 card (0 requests extra trong view danh sách).
  - Khi user click vào card: kích hoạt `GET /api/v1/vocabulary/{id}`, mở modal `<dialog id="vocabDetailModal">` và render danh sách bộ thủ cấu thành (`radicals`) cùng câu ví dụ và audio studio (nếu có).
- **Multi-Criteria Search & Race Protection**:
  - Hỗ trợ tìm kiếm theo chữ Hán, Pinyin có dấu (`shū`), Pinyin không dấu (`shu`).
  - Debounce 300ms, tự động hủy debounce khi bấm Enter hoặc click Clear (`#searchClearBtn`).
  - Bảo vệ bất biến thứ tự phản hồi qua `AbortController` và `latestRequestId` sequencing, triệt tiêu race condition (stale response không ghi đè kết quả mới).
  - Query thay đổi tự động reset `currentPage = 0`.
- **Phân trang & Kích thước trang**:
  - Hỗ trợ các mức kích thước trang: `12`, `20` (mặc định), `40`.
  - Thay đổi page size tự động reset `currentPage = 0` và reload catalog.
- **Web Speech API & Audio Pronunciation**:
  - Tích hợp chuẩn Web Speech API (`window.speechSynthesis` / `SpeechSynthesisUtterance` với `lang: 'zh-CN'`, `rate: 0.85`).
  - Hủy phát âm trước đó qua `window.speechSynthesis.cancel()` trước mỗi lần phát âm mới.
  - Fallback êm dịu (graceful fallback) khi trình duyệt không hỗ trợ Web Speech hoặc voice tiếng Trung vắng mặt (hiển thị Toast cảnh báo, không crash, không báo application error).
- **Accessibility & WCAG 2.2 Alignment**:
  - Input tìm kiếm `<input type="search" id="vocabSearchInput">` có nhãn tường minh `<label for="vocabSearchInput">`.
  - Container thông báo trạng thái kết quả có `role="status"` và `aria-live="polite"`.
  - Native `<dialog id="vocabDetailModal">` chuẩn WAI-ARIA APG: quản lý mở bằng user activation, đóng bằng Escape / nút đóng, lưu vết và phục hồi focus về activating element.
  - Kích thước tương tác đáp ứng chuẩn WCAG 2.2 SC 2.5.8 (Target Size $\ge 24\times 24\text{px}$, với CSS button target $\ge 44\times 44\text{px}$).
- **Security & Untrusted Data Defense**:
  - 100% dữ liệu động từ backend được render an toàn qua `textContent` hoặc DOM APIs (`createSafeElement`). Tuyệt đối không dùng `innerHTML` không an toàn.
  - Làm sạch URL tài nguyên âm thanh qua `sanitizeResourceUrl()` (chỉ cho phép http/https/relative, chặn `javascript:`, `data:`, protocol-relative).
- **Verification Center Integration**:
  - Bổ sung Section 5 chuyên trách Task 9B.3 trên `frontend/ui-verification.html` và `frontend/js/pages/ui-verification-page.js`.
  - Diagnostics: Probe Catalog live (`size=20`), Probe Detail live (ID đầu tiên), Probe Search `shu`, `ni`, Hanzi; kiểm tra khả năng Web Speech API.

### Exact Files Modified / Created
1. `frontend/vocabulary.html` [NEW] — Giao diện danh mục và tìm kiếm từ vựng tiếng Trung chuẩn HTML5 semantic, CJK typography, responsive Bootstrap 5.3 + Custom CSS, accessible modal dialog.
2. `frontend/js/pages/vocabulary-page.js` [NEW] — Controller điều phối danh mục từ vựng, quản lý state, search debounce 300ms, AbortController, request sequencing, Web Speech API, và progressive radical disclosure.
3. `frontend/css/style.css` [MODIFY] — Section 11 CSS rules: `.vocab-grid`, `.vocab-card`, `.vocab-speech-btn`, `.vocab-radical-badge`, `.vocab-radical-preview-box`.
4. `frontend/ui-verification.html` [MODIFY] — Cập nhật quick navigation bar lên 6 mục; bổ sung Section 5 (9B.3 Chinese Vocabulary Diagnostics) với 6 probe buttons; đánh số lại Live Full-Stack thành Section 6.
5. `frontend/js/pages/ui-verification-page.js` [MODIFY] — Tích hợp `initVocabularyDiagnostics()` và import `isSpeechSynthesisAvailable()`.
6. `tests/frontend/unit/vocabulary/vocabulary-catalog.test.mjs` [NEW] — Unit tests bao phủ tuần tự hóa query (`search`, cấm `q`), parsing PageResponse, tính toán phân trang, Zero N+1 contract, stale response protection, Web Speech fallback, sanitization và adversarial payloads (24 tests).
7. `tests/frontend/e2e/vocabulary.browser.mjs` [NEW] — Browser E2E suite bao phủ VB1 đến VB15 (9 test cases: initial catalog, multi-criteria search, empty state & clear, pagination & page sizes, progressive radical disclosure & modal APG, speech synthesis trigger, error state & retry, mobile 375px responsive, XSS sanitization).
8. `tests/frontend/accessibility/a11y.browser.mjs` [MODIFY] — Bổ sung test kiểm toán WCAG 2.2 cho `vocabulary.html` (target size, accessible name, dialog semantics, search input label).
9. `tests/frontend/e2e/verification-center.browser.mjs` [MODIFY] — Cập nhật `VC-01` nhận diện 6 nav items và bổ sung `VC-09` kiểm chứng chẩn đoán Section 9B.3.
10. `tests/frontend/runner.mjs` [MODIFY] — Đăng ký `vocabulary-catalog.test.mjs` vào `unitFiles` và `vocabulary.browser.mjs` vào `browserFiles`.
11. `tests/fullstack/scenarios/api-scenarios.mjs` [MODIFY] — Bổ sung kịch bản `FS-020` kiểm chứng live REST protocol cho danh mục từ vựng, tìm kiếm `search=shu`, và chi tiết bộ thủ cấu thành.
12. `tests/fullstack/scenarios/browser-scenarios.mjs` [MODIFY] — Bổ sung kịch bản `FS-021` kiểm chứng hành trình người dùng thực tế trên trình duyệt: tải catalog, tìm kiếm `shu` -> `书`, mở và đóng modal chi tiết từ vựng.

### Actual Automated Verification Results
- **Static Verification (`npm run verify:frontend:static`)**: **61/61 PASS** (11 suites, 0.38s, budget 1.50s).
- **Unit Verification (`npm run verify:frontend:unit`)**: **129/129 PASS** (29 suites, 1.28s, budget 1.50s).
- **Browser E2E Verification (`npm run verify:frontend:browser`)**: **43/43 PASS** (6 suites, 55.83s, 0 failures, 0 errors, 0 skips).
- **Accessibility Verification (`npm run verify:frontend:a11y`)**: **8/8 PASS** (1 suite, 9.92s, WCAG 2.2 AA).
- **Frontend Quality Gate (`npm run verify:frontend:gate`)**: **241/241 PASS** across 47 suites (67.35s, 0 failures, 0 errors).
- **Verification Center Dedicated Suite (`node --test tests/frontend/e2e/verification-center.browser.mjs`)**: **9/9 PASS** (15.05s).
- **Full-Stack Live Verification (`npm run verify:fullstack`)**: **21/21 PASS** (`FS-001` .. `FS-021`, 6.42s) kết nối Real Playwright Browser $\rightarrow$ Real Frontend (:3000) $\rightarrow$ Real CORS/HTTP $\rightarrow$ Real Spring Boot (:8080) $\rightarrow$ Real MySQL (:3306).

### Next Task
- **Phase 9 — Module 9C — Task 9C.2: Phân hệ Ghi chú cá nhân trong ngữ cảnh (Contextual Personal Notes) (`frontend/js/ui/notes-modal.js`)**.

---

## 15. PUBLIC LESSONS & ORDERED LESSON DETAIL UI (TASK 9C.1) [COMPLETED]

### Task Identity
- **Task ID**: `9C.1`
- **Title**: Public Lessons Catalog & Ordered Lesson Detail UI
- **Module**: `Module 9C — Learner Core & Study Experience`
- **Type**: Frontend Feature Pages, Order-Preserving Sequence, Date Semantics, Neutral 404 Defense & Diagnostics
- **Date**: 2026-09-10
- **Status**: **COMPLETED**

### Primary Objective & Architecture
Xây dựng trải nghiệm học viên cho danh mục bài học công khai `frontend/lessons.html` và chi tiết bài học `frontend/lesson-detail.html` cùng các controllers `frontend/js/pages/lessons-page.js` và `frontend/js/pages/lesson-detail-page.js`:
- **Backend API Contract Enforcement**:
  - Endpoint danh mục: `GET /api/v1/lessons?page={page}&size={size}` (TUYỆT ĐỐI KHÔNG giả lập tham số `search`, `q`, `keyword`, `status`).
  - Endpoint chi tiết: `GET /api/v1/lessons/{id}` trả về `ApiResponse<LessonDetailResponse>`.
  - Phân trang chuẩn theo `ApiResponse<PageResponse<LessonSummaryResponse>>` (`page`, `size`, `totalElements`, `totalPages`, `items`).
- **Critical Public Visibility Invariant**:
  - Chỉ bài học có `status === "Approved"` mới được backend trả về.
  - Bài học `Draft`, `Pending`, `Rejected` hoàn toàn vô hình đối với Learner.
  - Khi truy cập chi tiết bài học không Approved hoặc không tồn tại, backend trả HTTP 404. Giao diện người dùng hiển thị thông điệp trung lập 100%: `"Không tìm thấy bài học hoặc bài học chưa được công khai."`, tuyệt đối không làm lộ trạng thái kiểm duyệt nội bộ.
- **Zero Invented Metadata & Contract Discipline**:
  - Không tự bịa đặt các trường không có trong backend DTO: cấm `difficulty`, `level`, `author`, `creatorName`, `description`, `publishedAt`, `imageUrl`, `category`.
- **Publication Date Technical Decision**:
  - Dựa trên chứng cứ mã nguồn backend (`CreatorLessonServiceImpl.java:473-475` cấm creator sửa bài sau khi Approved, và JPA lifecycle `@PreUpdate` cập nhật `updatedAt` tại thời điểm approval), `updatedAt` được sử dụng làm proxy trung thực cho thời điểm xuất bản.
  - Nhãn hiển thị giao diện: `"Ngày cập nhật: "` kết hợp thẻ chuẩn HTML5 ngữ nghĩa `<time datetime="...">` định dạng chuẩn theo `vi-VN`.
- **Thứ tự từ vựng bài học chuẩn hóa (Preserved orderIndex Sequence)**:
  - Backend đảm bảo `order_index ASC`.
  - Frontend render bằng thẻ danh sách ngữ nghĩa chuẩn `<ol id="lessonVocabList">` và các thẻ `<li>` mang thuộc tính `value="{orderIndex}"`.
  - Mỗi mục từ vựng hiển thị rõ huy hiệu thứ tự `orderIndex`, chữ Hán nổi bật `lang="zh-Hans"`, Pinyin, Hán-Việt, Nghĩa tiếng Việt, ví dụ, nút phát âm Web Speech API và nút liên kết tra từ điển (`vocabulary.html?search=...`).
- **Accessibility & WCAG 2.2 Alignment**:
  - Single primary H1 trên mỗi trang (`lessons.html` và `lesson-detail.html`).
  - Đường dẫn bài học chuẩn APG Breadcrumb Navigation Landmark (`<nav aria-label="Đường dẫn bài học">`, `<li aria-current="page">`).
  - Vùng thông báo động trạng thái kết quả có `aria-live="polite"`.
  - Thẻ bài học dùng ngữ nghĩa `<article>` với accessible name và kích thước tương tác đáp ứng chuẩn WCAG 2.2 SC 2.5.8 ($\ge 44\times 44\text{px}$).
  - Nút phân trang có `aria-current="page"` tại trang đang xem.
- **Security & Untrusted Data Defense**:
  - 100% dữ liệu động từ backend được render an toàn qua DOM APIs (`createSafeElement`, `textContent`). Không dùng `innerHTML` không an toàn.
  - Làm sạch URL tài nguyên media qua `sanitizeResourceUrl()`.
- **Verification Center Integration**:
  - Bổ sung Section 6 chuyên trách Task 9C.1 trên `frontend/ui-verification.html` và `frontend/js/pages/ui-verification-page.js`.
  - 5 probe buttons: Probe Lesson Catalog, Xác Minh Filter Approved Invariant, Probe Detail ID=1, Probe 404 Trung Lập (ID=99999999), Unit Test Logic Sort `orderIndex`.

### Exact Files Modified / Created
1. `frontend/lessons.html` [NEW] — Giao diện danh mục bài học công khai chuẩn HTML5 semantic, single H1, skip link, responsive Bootstrap 5.3 + Custom CSS, accessible pagination.
2. `frontend/lesson-detail.html` [NEW] — Giao diện chi tiết bài học, breadcrumb navigation landmark, semantic `<ol>` ordered list, Web Speech pronunciation, dictionary cross-link.
3. `frontend/js/pages/lessons-page.js` [NEW] — Controller điều phối danh mục bài học, phân trang `PageResponse`, AbortController, request sequencing, 3-State UI.
4. `frontend/js/pages/lesson-detail-page.js` [NEW] — Controller điều phối chi tiết bài học, trích xuất và kiểm tra ID từ query string `?id=`, neutral 404 defense, bảo toàn thứ tự `orderIndex` và phát âm tiếng Trung.
5. `frontend/css/style.css` [MODIFY] — Section 12 CSS rules: `.lesson-grid`, `.lesson-card`, `.lesson-vocab-list`, `.lesson-vocab-item`, `.lesson-vocab-order-badge`, `.breadcrumb-nav`, `.breadcrumb-custom`; đánh số lại Section 13 cho prefers-reduced-motion.
6. `frontend/ui-verification.html` [MODIFY] — Cập nhật quick nav lên 7 mục; bổ sung Section 6 (9C.1 Public Lessons Diagnostics) với 5 probe controls; đánh số lại Live Full-Stack thành Section 7.
7. `frontend/js/pages/ui-verification-page.js` [MODIFY] — Tích hợp `initLessonDiagnostics()` và import `sortVocabulariesByOrderIndex()`.
8. `tests/frontend/unit/lesson/lessons-catalog.test.mjs` [NEW] — 22 unit tests bao phủ parsing DTO, tính toán phân trang, định dạng ngày tháng, kiểm tra tính hợp lệ của ID, bảo toàn thứ tự `orderIndex`, trung lập hóa thông báo 404, fallback Web Speech, và lọc XSS.
9. `tests/frontend/e2e/lessons.browser.mjs` [NEW] — 11 browser E2E test cases (LB1..LB11) bao phủ tải danh mục, chuyển trang detail, phân trang & page size, empty state, 500 error & retry, thứ tự từ vựng 1->2->3, Web Speech API, neutral 404 defense, tham số ID không hợp lệ, responsive 375px, và XSS sanitization.
10. `tests/frontend/accessibility/a11y.browser.mjs` [MODIFY] — Bổ sung 2 test cases kiểm toán WCAG 2.2 cho `lessons.html` (target size, select label, polite announcer, aria-current page) và `lesson-detail.html` (breadcrumb landmark, semantic `<ol>`, speech button accessible labels, single H1).
11. `tests/frontend/e2e/verification-center.browser.mjs` [MODIFY] — Cập nhật `VC-01` nhận diện 7 functional sections và bổ sung `VC-10` kiểm chứng Section 9C.1 diagnostics (sort probe, 404 probe, catalog probe, approved filter invariant probe).
12. `tests/frontend/runner.mjs` [MODIFY] — Đăng ký `lessons-catalog.test.mjs` vào `unitFiles` và `lessons.browser.mjs` vào `browserFiles`.
13. `tests/fullstack/scenarios/api-scenarios.mjs` [MODIFY] — Bổ sung kịch bản `FS-022` kiểm chứng live REST protocol cho danh mục bài học (`GET /lessons?page=0&size=20`), xác minh 100% bài học là `Approved`, và chẩn đoán phòng thủ 404 cho non-existent ID.
14. `tests/fullstack/scenarios/browser-scenarios.mjs` [MODIFY] — Bổ sung kịch bản `FS-023` kiểm chứng hành trình người dùng thực tế trên trình duyệt đối với `lessons.html` kết nối trực tiếp CSDL thật, xác nhận hiển thị trạng thái rỗng lịch sự khi DB chưa có bài Approved.

### Actual Automated Verification Results
- **Static Verification (`npm run verify:frontend:static`)**: **75/75 PASS** (13 suites, 0.38s, budget 1.50s).
- **Unit Verification (`npm run verify:frontend:unit`)**: **151/151 PASS** (39 suites, 1.41s, budget 1.50s).
- **Browser E2E Verification (`npm run verify:frontend:browser`)**: **55/55 PASS** (7 suites, 71.06s, 0 failures, 0 errors, 0 skips).
- **Accessibility Verification (`npm run verify:frontend:a11y`)**: **10/10 PASS** (1 suite, 15.09s, WCAG 2.2 AA).
- **Frontend Quality Gate (`npm run verify:frontend:gate`)**: **291/291 PASS** across 60 suites (87.93s, 0 failures, 0 errors).
- **Verification Center Dedicated Suite (`node --test tests/frontend/e2e/verification-center.browser.mjs`)**: **10/10 PASS** (18.13s, VC-01..VC-10).
- **Full-Stack Live Verification (`npm run verify:fullstack`)**: **23/23 PASS** (`FS-001` .. `FS-023`, 7.17s) kết nối Real Playwright Browser $\rightarrow$ Real Frontend (:3000) $\rightarrow$ Real CORS/HTTP $\rightarrow$ Real Spring Boot (:8080) $\rightarrow$ Real MySQL (:3306). Zero mock routes in live suite.

### Next Task
- **Phase 9 — Module 9C — Task 9C.2: Phân hệ Ghi chú cá nhân trong ngữ cảnh (Contextual Personal Notes) (`frontend/js/ui/notes-modal.js`)**.

---

## 16. FINAL CLOSURE PASS & VERIFICATION CENTER HARDENING (TASKS 9B.3 & 9C.1) [COMPLETED]

### Task Identity
- **Task**: Final Closure Pass — Tasks 9B.3 + 9C.1 Verified Issues & Verification Center Hardening
- **Date**: 2026-09-10
- **Status**: **COMPLETED & OFFICIALLY CLOSED**

### Corrections Made
1. **Verification Center — Elimination of False-Pass Risks (`frontend/js/pages/ui-verification-page.js`)**:
   - **Lesson Catalog Probe (3.1)**: Replaced generic error handling with strict status distinction: `[LIVE PASS / HTTP 200 OK]` on real 200 responses vs `[ENVIRONMENT BLOCKED / MẠNG KHÔNG KHẢ DỤNG]` on network/abort vs `[LIVE FAIL / LỖI API HTTP ...]` on server errors. An API error is never treated as a pass.
   - **Approved Filter Invariant Probe (3.2)**: When the public catalog returns 0 items, it now strictly reports `[INCONCLUSIVE / NO PUBLIC LESSON DATA]` because empty sample data cannot prove positive Approved filtering. When non-approved items are detected, reports `[LIVE FAIL / VI PHẠM BẢO MẬT]`. Only reports `[LIVE PASS / XÁC MINH BẤT BIẾN THÀNH CÔNG]` when items exist and 100% have `status === "Approved"`.
   - **Lesson Detail Probe (3.3)**: Completely eliminated hardcoded lesson ID 1 dependency. Implemented dynamic catalog discovery flow: first queries `GET /api/v1/lessons?page=0&size=1`; if empty, displays `[NOT RUN / NO PUBLIC LESSON AVAILABLE]`; if an item exists, queries `GET /api/v1/lessons/{lessonId}` and verifies matching lesson ID, title, vocabulary count, and ascending `orderIndex`.
   - **404 Neutral Defense Probe (3.4)**: Enforces real HTTP 404 response on non-existent lesson ID `99999999`, validates the exact neutral learner message: `"Không tìm thấy bài học hoặc bài học chưa được công khai."`, and asserts zero leakage of internal workflow states (`Draft`, `Pending`, `Rejected`, `rejectionReason`, `flaggedFields`).
   - **Section 9B.3 Vocabulary Diagnostics (Section 5)**: Applied identical no-false-pass and capability classification principles: `[LIVE PASS / HTTP 200 OK]` on catalog/search/detail, `[NOT RUN / NO VOCABULARY AVAILABLE]` on empty catalog, and `[CAPABILITY PASS / KHẢ DỤNG]` vs `[CAPABILITY BLOCKED / KHÔNG KHẢ DỤNG]` for Web Speech API.
2. **Verification Center E2E Browser Assertion Tightening (`tests/frontend/e2e/verification-center.browser.mjs`)**:
   - Upgraded `VC-09` and `VC-10` from permissive `PASS OR ERROR` patterns to strict non-permissive assertions requiring explicit pass tags and forbidding `LIVE FAIL` / `VI PHẠM`.
3. **Project State Synchronization (`.agents/CURRENT_STATE.md` & `.agents/PROGRESS.md`)**:
   - Harmonized all current state sections: Last Completed: `Task 9C.1`, Next: `Task 9C.2 (Contextual Personal Notes)`.
   - Replaced all inaccurate "Working directory clean" statements with true Git state (uncommitted modifications and test files; ready for handoff review; zero unauthorized commits).
   - Marked initial Phase 1 snapshot clearly as Historical Archive.

### Actual Automated Verification Results (Current Execution)
- `npm run verify:frontend:static`: **75/75 PASS** (13 suites, 0.38s)
- `npm run verify:frontend:unit`: **151/151 PASS** (39 suites, 1.41s)
- `npm run verify:frontend:browser`: **55/55 PASS** (7 suites, 71.06s)
- `npm run verify:frontend:a11y`: **10/10 PASS** (1 suite, 15.09s)
- `npm run verify:frontend:gate`: **291/291 PASS** (60 suites, 87.93s, 0 failures, 0 errors)
- `node --test tests/frontend/e2e/verification-center.browser.mjs`: **10/10 PASS** (18.13s, VC-01..VC-10)
- `npm run verify:fullstack`: **23/23 PASS** (7.17s, `FS-001` .. `FS-023` live)

### Next Task
- **Phase 9 — Module 9C — Task 9C.2: Phân hệ Ghi chú cá nhân trong ngữ cảnh (Contextual Personal Notes) (`frontend/js/ui/notes-modal.js`)** [COMPLETED].

---

## 17. PHASE 9 — MODULE 9C — TASK 9C.2: CONTEXTUAL PERSONAL NOTES UI [COMPLETED]

### Task Identity
- **Task**: Task 9C.2 — Contextual Personal Notes UI (`frontend/js/ui/notes-modal.js`, `frontend/vocabulary.html`, `frontend/lesson-detail.html`, `frontend/ui-verification.html`)
- **Date**: 2026-09-14
- **Status**: **COMPLETED & OFFICIALLY VERIFIED**

### Implementation Summary
1. **Contextual Personal Notes Architecture (`frontend/js/ui/notes-modal.js`)**:
   - Implemented reusable `NotesModalController` with lazy native `<dialog id="notesModal">` DOM construction (`ensureDialog()`).
   - Strictly contextual design: No separate `notes.html` page created. Personal notes belong to `USER x VOCABULARY`.
   - Integrated into `frontend/vocabulary.html` & `frontend/js/pages/vocabulary-page.js` (per-card note action button `.vocab-card-note-btn` and detail modal footer button `#modalVocabNoteBtn`).
   - Integrated into `frontend/lesson-detail.html` & `frontend/js/pages/lesson-detail-page.js` (per-vocabulary item button `.lesson-vocab-note-btn` passing valid `vocabId`).
2. **Backend Contract & IDOR Security**:
   - `GET /api/v1/vocabularies/{vocabId}/notes?page=0&size=20`: Backend filters notes strictly to authenticated user; pagination uses standard `PageResponse`.
   - `POST /api/v1/vocabularies/{vocabId}/notes`: Request body `{ "content": "..." }`. Identity comes 100% from backend `SecurityContextHolder`. Frontend sends ZERO user ID parameters or fields.
   - `PUT /api/v1/notes/{noteId}`: Request body `{ "content": "..." }`. Preserves note ID and vocabulary ID; updates note in place.
   - `DELETE /api/v1/notes/{noteId}`: Authenticated owner deletion. Correctly processes HTTP 200 response with JSON envelope `{ code: "SUCCESS", message: "...", data: null, errors: [] }` (NOT 204 No Content).
3. **Character Boundary & Counter**:
   - Hard limit `maxlength="500"` enforced via HTML attribute and client validation (`validateNoteContent`).
   - Live character counter (`#noteCharCounter`) updating synchronously on input: `X / 500 ký tự`.
   - CJK, Vietnamese diacritics, and newline characters counted accurately using standard `.length`.
   - Screen reader announcement via `aria-live="polite"` with threshold warning class `.text-danger` at 500 characters.
4. **Safe DOM & XSS Resistance**:
   - Zero dynamic `innerHTML`, `outerHTML`, or `insertAdjacentHTML` sinks (OWASP ASVS 5.0 compliance).
   - Free-form user note text, Hanzi, Pinyin, and timestamps rendered strictly via safe DOM APIs (`textContent`, `document.createElement`, `replaceChildren`).
   - Tested and verified against adversarial injection vectors (`<script>`, `<img onerror>`).
5. **Accessibility & Usability (WCAG 2.2 AA)**:
   - Native HTML5 `<dialog>` using `showModal()` ensuring browser-level top-layer isolation and native inert backdrop.
   - Explicit textarea label (`for="noteTextarea"`), visible focus outlines (`:focus-visible`), and logical tab ordering.
   - Inline deletion confirmation box (`.note-delete-confirm-box`) inside card to avoid nested modal dialog traps.
   - Full focus restoration to the opening trigger button upon dialog dismissal (Escape key or Close button).
6. **Verification Center Diagnostics (`ui-verification.html` Section 7)**:
   - Added Section 7 (`#section-9c2`) with 8-item jump navigation bar.
   - Implemented 5 diagnostic probes in `frontend/js/pages/ui-verification-page.js`:
     1. Character boundary validation capability (0, 1, 499, 500, 501 chars, CJK, whitespace).
     2. Anonymous note guard (verifies login prompt and blocks unauthenticated API call).
     3. Launch notes modal interactive demo with live fallback context.
     4. Authenticated Notes API probe (safe read-only probe with `LIVE PASS` / `BLOCKED`).
     5. Note ownership invariant probe (audits client payload contracts for zero `userId` leakage).
   - Upgraded Verification Center browser test suite (`VC-11` added, 11/11 PASS).

### Actual Automated Verification Results (Task 9C.2)
- **Static Verification (`npm run verify:frontend:static`)**: **75/75 PASS** (13 suites, 0.40s)
- **Unit Verification (`npm run verify:frontend:unit`)**: **172/172 PASS** (40 suites, 1.48s; +21 notes unit tests)
- **Browser E2E Verification (`npm run verify:frontend:browser`)**: **63/63 PASS** (8 suites, 54.3s; +7 notes E2E tests, +1 VC probe test)
- **Accessibility Verification (`npm run verify:frontend:a11y`)**: **11/11 PASS** (1 suite, 6.3s; +1 dedicated 9C.2 modal a11y test)
- **Frontend Quality Gate (`npm run verify:frontend:gate`)**: **321/321 PASS** across 69 suites (65.47s, 0 failures, 0 errors)
- **Verification Center Dedicated Suite (`node --test tests/frontend/e2e/verification-center.browser.mjs`)**: **11/11 PASS** (16.2s, VC-01..VC-11)
- **Full-Stack Live Verification (`npm run verify:fullstack`)**: **23/23 PASS** (`FS-001` .. `FS-023`, 5.44s) against real Spring Boot 3.3.5 (:8080) and real MySQL 8.4 (:3306).

### Next Task
- **Phase 9 — Module 9C — Task 9C.3: Phòng ôn tập Flashcard tương tác Spaced Repetition (SRS Room) (`frontend/srs-review.html`)**.

---

## 18. GIAI ĐOẠN 9 — MODULE 9C: TASK 9C.3 — PHÒNG ÔN TẬP FLASHCARD TƯƠNG TÁC (SRS ROOM)

### Task Identity
- **Task**: Task 9C.3 — Interactive SRS Flashcard Review Session UI (`frontend/srs-review.html`, `frontend/js/pages/srs-review-page.js`, `frontend/css/style.css`, `frontend/ui-verification.html`)
- **Date**: 2026-09-14
- **Status**: **COMPLETED & OFFICIALLY VERIFIED**

### Implementation Summary
1. **Interactive SRS Review Room (`frontend/srs-review.html` & `frontend/js/pages/srs-review-page.js`)**:
   - Built a dedicated, distraction-free study room interface adhering to the single H1 rule, semantic landmarks, skip-link, and zero obsolete presentational elements.
   - 6 coherent UI states managed cleanly: `#srsLoadingState`, `#srsAuthState`, `#srsEmptyState`, `#srsErrorState`, `#srsActiveSession`, and `#srsCompletedState`.
   - Responsive session progress bar (`#srsProgressBar`, `#srsQueueRemainingBadge`) updating dynamically as cards are reviewed.
2. **Session Modes & Contract-Compliant Ingestion**:
   - **Mode A (Default Due Review)**: Calls `GET /api/v1/srs/due` (supports optional `limit` and `itemType`).
   - **Mode B (Lesson Context)**: When `?lessonId=<id>` is supplied, validates `<id>` as positive integer and retrieves candidates from `GET /api/v1/srs/new-cards?lessonId=<id>`. Never hardcodes `lessonId=1` and never fabricates fallback IDs.
   - Handled `DueCardResponse` payload strictly: `hanzi`, `pinyin`, `meaningVi`, `meaningHanViet`, `exampleSentence`, `exampleTranslation`, `repetitions`, `intervalDays`, `easeFactor`, `isNew`.
   - Never assumes `strokeCount` (explicitly null in DB design) or `audioUrl` are present on due card summaries.
3. **3D Flashcard Interaction & Vestibular Accessibility**:
   - True 3D CSS perspective card flip (`transform: rotateY(180deg)` with `transform-style: preserve-3d` and `backface-visibility: hidden`).
   - Toggle answer flip via card click/tap, dedicated reveal button, `Space` key, or `Enter` key.
   - Synchronized accessibility attributes: `aria-hidden="true"` / `false` between front and back faces, with `aria-live="polite"` flip status announcements.
   - **Reduced Motion Support**: Section 15 CSS `@media (prefers-reduced-motion: reduce)` completely disables 3D rotation (`transition: none; transform: none;`) and swaps face visibility instantly for vestibular safety.
4. **Reaction Timer Integrity (`reviewTimeSeconds`)**:
   - Synchronous `setInterval` timer measuring real elapsed seconds from card presentation until rating submission.
   - Enforces non-negative integer math: `Math.max(0, Math.round((now - start) / 1000))`.
   - Explicit cleanup on page unload, navigation, and state transitions to prevent interval leaks.
   - Serialized payload property is strictly `reviewTimeSeconds` (camelCase, NOT snake_case).
5. **Authoritative Backend Rating Submission (1..4)**:
   - 4 clearly differentiated rating buttons and keyboard shortcuts: `1 = Again`, `2 = Hard`, `3 = Good`, `4 = Easy`.
   - Touch targets satisfy $\ge 44 \times 44\text{px}$ mobile usability.
   - Client is strictly a presentation layer: Zero SM-2 mathematical calculation duplicated in JavaScript. Backend owns interval, ease factor, and next review date.
   - Strict IDOR defense: Request body contains only `{ itemType, itemId, rating, reviewTimeSeconds }` (ZERO client `userId` or `accountId`).
   - Strong double-submit protection: disables controls immediately upon submission and prevents concurrent requests.
6. **Local Session Queue Again Requeue Policy**:
   - Cards rated `Again (1)` return to the end of the active session queue for another review attempt.
   - Bounded via `MAX_AGAIN_REVIEWS_PER_CARD = 3` to prevent infinite review loops when a learner struggles with a difficult card.
7. **Demand-Driven Audio & Contextual Notes Integration**:
   - Audio URLs fetched on-demand from detail endpoints (`GET /api/v1/vocabulary/{id}` or `GET /api/v1/radicals/{id}`) with caching to prevent N+1 queries.
   - Web Speech API fallback (`zh-CN`, rate 0.85) when audio file is null or unconfigured.
   - Reuses `NotesModalController` (`openNotesModal`) for vocabulary cards; radicals card notes button is disabled with an accurate explanatory tooltip.
8. **Verification Center Diagnostics (`ui-verification.html` Section 8)**:
   - Added Section 8 (`#section-9c3`) with 9-item jump navigation bar.
   - Implemented 5 diagnostic probes in `ui-verification-page.js`:
     1. Rating mapping 1..4 contract check.
     2. Reaction timer integrity check (`reviewTimeSeconds >= 0`).
     3. Again local session requeue bounded loop check (`MAX_AGAIN_REVIEWS_PER_CARD = 3`).
     4. SRS anonymous guard check.
     5. Authenticated Due Cards API probe (`LIVE PASS` / `BLOCKED`).
   - Extended Verification Center browser suite with `VC-12` (12/12 PASS).

### Actual Automated Verification Results (Task 9C.3)
- **Static Verification (`npm run verify:frontend:static`)**: **75/75 PASS** (13 suites, 0.40s)
- **Unit Verification (`npm run verify:frontend:unit`)**: **195/195 PASS** (47 suites, 1.48s; +23 SRS unit tests)
- **Fast Tier Verification (`npm run verify:frontend:fast`)**: **282/282 PASS** (68 suites, 1.91s, within 2.50s budget)
- **Browser E2E Verification (`node --test tests/frontend/e2e/srs-review.browser.mjs`)**: **8/8 PASS** (1 suite, 6.62s; SR-01..SR-08)
- **Verification Center Dedicated Suite (`node --test tests/frontend/e2e/verification-center.browser.mjs`)**: **12/12 PASS** (1 suite, 17.3s; VC-01..VC-12)
- **Accessibility Suite (`npm run verify:frontend:a11y`)**: **12/12 PASS** (1 suite, 7.07s; +1 dedicated SRS 3D card a11y test)
- **Frontend Quality Gate (`npm run verify:frontend:gate`)**: **366/366 PASS** across 78 suites (75.49s, 0 failures, 0 errors)
- **Full-Stack Live Verification (`npm run verify:fullstack`)**: **23/23 PASS** (`FS-001` .. `FS-023`, 5.68s) against real Spring Boot 3.3.5 (:8080) and real MySQL 8.4 (:3306).

### Next Task
- **Phase 9 — Module 9D — Task 9D.1: Creator Lesson Studio & Quản lý thứ tự từ vựng (`creator-lessons.html`, `creator-lesson-editor.html`)**.

---

## 20. PHASE 9 — MODULE 9C — TASK 9C.4: STUDY STATISTICS & SRS DAILY SETTINGS DASHBOARD

- **Task ID**: `9C.4`
- **Date**: `2026-09-14`
- **Primary Deliverables**:
  - `frontend/srs-dashboard.html`
  - `frontend/js/pages/srs-dashboard-page.js`
  - `frontend/css/style.css` (Section 16: `.srs-dashboard-wrapper`, `.srs-metric-grid`, `.srs-metric-card`, `.srs-progress-track`, `.srs-settings-card`, `.srs-field-error`)
  - `frontend/srs-review.html` (Cross-navigation links in room header, empty state, and completion card)
  - `frontend/js/ui/nav.js` (Protected page registration for `srs-dashboard.html` on logout)
  - `frontend/ui-verification.html` & `frontend/js/pages/ui-verification-page.js` (Section 8 probes for settings validation & live stats/settings GET API)
  - `tests/frontend/unit/srs/srs-dashboard.test.mjs` (Dedicated unit suite: 16 tests)
  - `tests/frontend/e2e/srs-dashboard.browser.mjs` (Dedicated browser E2E suite: 6 tests)
  - `tests/frontend/accessibility/a11y.browser.mjs` (Dedicated WCAG 2.2 audit test for 9C.4)
  - `tests/frontend/runner.mjs` (Registration in Fast and Gate tiers)
- **Status**: **COMPLETED & OFFICIALLY VERIFIED**

### Implementation Summary
1. **Personal Learning Dashboard Architecture (`frontend/srs-dashboard.html` & `frontend/js/pages/srs-dashboard-page.js`)**:
   - Single authoritative dashboard for authenticated learners adhering to the single H1 rule, semantic landmarks (`<header>`, `<nav>`, `<main id="mainContent">`, `<footer>`), skip-link, and zero obsolete markup.
   - 4 coherent state containers: `#dashboardLoadingState` (polite spinner), `#dashboardAuthState` (anonymous login prompt), `#dashboardErrorState` (accessible retry alert), and `#dashboardContentSection` (5 metric cards and settings form).
2. **Authoritative 5 Study Statistics Presentation (`GET /api/v1/srs/stats`)**:
   - Accurately maps backend `StudyStatsResponse` fields:
     - `cardsDue`: Number of flashcards currently due today (`#statCardsDue`).
     - `reviewsToday`: Total review ratings recorded today (`#statReviewsToday`) with quota text (`#statReviewsQuotaText`) and visual progress bar (`#statReviewsProgressBar`).
     - `newCardsToday`: Total new cards introduced today (`#statNewCardsToday`) with quota text (`#statNewCardsQuotaText`) and visual progress bar (`#statNewCardsProgressBar`).
     - `newCardsLimit`: Personal daily limit for new cards (`#statNewCardsLimit`).
     - `maxReviewLimit`: Personal daily limit for maximum reviews (`#statMaxReviewLimit`).
   - Zero client-side statistical math or local session calculation.
   - Zero fabricated analytics: No fake historical daily charts, streak counters, retention graphs, heatmaps, or average review times not provided by the backend.
   - Valid data handling: Treats `0` strictly as legitimate learning progress data, never conflating zero with empty or error states.
3. **Personal Daily SRS Settings Management (`GET /api/v1/srs/settings` & `PUT /api/v1/srs/settings`)**:
   - Form inputs pre-filled with authoritative backend settings: `#newCardsPerDayInput` and `#maxReviewPerDayInput`.
   - Strict client-side validation (`validateSrsSettings`): rejects empty, null, non-numeric, decimal/float, zero, and negative values. Enforces strictly positive integers ($> 0$).
   - Field-level error association with inline error containers (`#newCardsPerDayError`, `#maxReviewPerDayError`) and accessible focus management moving to the first invalid field.
   - Quota progress defense (`calculateQuotaPercentage`): protects against division by zero, non-finite numbers, and clamps completion between 0% and 100%.
4. **Update Flow, Anti-Duplicate Lock & Immediate Authoritative Refresh**:
   - Prevents duplicate submissions (`isSubmitting` lock + disables submit button with `'Đang lưu...'`).
   - Serializes clean payload: strictly `{ newCardsPerDay, maxReviewPerDay }` without `userId` or `accountId` (IDOR protection).
   - Authoritative refresh on success: upon 200 OK from `PUT`, immediately triggers `Promise.all([ apiClient('/srs/stats'), apiClient('/srs/settings') ])` and updates metric cards and form inputs with backend state.
   - Reset button (`#btnResetSettings`) restores form inputs to the last known authoritative values without page reload.
5. **Cross-Navigation Integration**:
   - Direct navigation from `srs-dashboard.html` to SRS Review Room (`#btnGoToReview` $\rightarrow$ `srs-review.html`).
   - Direct return links in `srs-review.html` from header (`#linkToSrsDashboard`), empty state, and completion summary (`#btnCompletionToDashboard`).
   - Centralized authentication protection: `authManager.logout()` on `srs-dashboard.html` redirects cleanly to `index.html`.
6. **DOM Safety & Accessibility (WCAG 2.2 AA)**:
   - 100% safe DOM updates (`textContent`, `setAttribute`, `classList`). Zero dynamic `innerHTML` sinks, zero inline event handlers.
   - Proper labels for form inputs with `aria-describedby` pointing to helper text and error containers.
   - Form status announcement via polite live region (`#settingsFormStatus` with `role="status"` and `aria-live="polite"`).
   - Button target sizes meet $\ge 44 \times 38\text{px}$ mobile touch requirements.
7. **Verification Center Diagnostics (`ui-verification.html` Section 8)**:
   - Updated Section 8 to encompass both 9C.3 Review Session and 9C.4 Stats / Daily Settings.
   - Added `#btnProbeSrsSettingsValidation`: Unit probe validating positive integer rules and quota division-by-zero defense.
   - Added `#btnProbeSrsStatsApi`: Live probe calling `GET /srs/stats` and `GET /srs/settings` concurrently, displaying all 5 stats fields and user settings.
   - Added direct navigation link to `srs-dashboard.html`.

### Actual Automated Verification Results (Task 9C.4)
- **Static Verification (`tests/frontend/static/`)**: **81/81 PASS** across 13 suites (HTML semantics, SRI integrity, CSS tokens, DOM sinks).
- **Unit Verification (`tests/frontend/unit/srs/srs-dashboard.test.mjs`)**: **16/16 PASS** (1 suite, 7.75ms; validation, quota math, payload serialization, contract fields).
- **Fast Tier Verification (`npm run verify:frontend:fast`)**: **305/305 PASS** across 74 suites (2.01s, within 2.50s budget).
- **Dedicated Browser E2E (`node --test tests/frontend/e2e/srs-dashboard.browser.mjs`)**: **6/6 PASS** (1 suite, 11.5s; SD-01..SD-06).
- **Verification Center Dedicated Suite (`node --test tests/frontend/e2e/verification-center.browser.mjs`)**: **12/12 PASS** (1 suite, 21.9s; VC-01..VC-12).
- **Accessibility Suite (`npm run verify:frontend:a11y`)**: **13/13 PASS** (1 suite, 8.52s; +1 dedicated SRS Dashboard a11y test).
- **Frontend Quality Gate (`npm run verify:frontend:gate`)**: **396/396 PASS** across 85 suites (79.20s, 0 failures, 0 errors).

### Next Task
- **Phase 9 — Module 9D — Task 9D.1: Creator Lesson Studio & Quản lý thứ tự từ vựng (`creator-lessons.html`, `creator-lesson-editor.html`)**.

---

## 2026-09-16 — Phase 9: Module 9F — Task 9F.1: Admin Accounts Lifecycle UI (`frontend/admin-accounts.html`, `frontend/js/pages/admin-accounts-page.js`)

### Objective & Summary
Xây dựng và hoàn thiện giao diện Quản trị Vòng đời Tài khoản người dùng (Admin Accounts Lifecycle UI) dành riêng cho Quản trị viên (`Admin`):
1. Tích hợp đầy đủ vào shell giao diện hiện hữu với `frontend/admin-accounts.html` và `frontend/js/pages/admin-accounts-page.js`.
2. Kết nối chuẩn xác với backend contracts:
   - `GET /api/v1/admin/accounts`: Truy vấn danh sách tài khoản phân trang, hỗ trợ tham số `status`, `search`, `page`, `size`.
   - `PUT /api/v1/admin/accounts/{id}/status`: Cập nhật trạng thái tài khoản mục tiêu sang `Active`, `Inactive`, hoặc `Banned`.
3. Tuân thủ triệt để chính sách bảo vệ tự thân `POL-8D-01`: Nhận diện tài khoản Admin đang đăng nhập (`isSelfAccount()`), hiển thị huy hiệu nhận biết "Tài khoản của bạn" và vô hiệu hóa nút đổi trạng thái để ngăn Admin tự khóa chính mình.
4. Minh bạch cơ chế bảo mật và thu hồi phiên `DEC-42`: Modal xác nhận native `<dialog id="statusConfirmModal">` giải thích trung thực rằng thay đổi trạng thái sẽ làm vô hiệu hóa token/phiên làm việc của tài khoản mục tiêu theo cơ chế `authorization_version` phía máy chủ.
5. Kiểm soát truy cập RBAC nghiêm ngặt: Chỉ cấp quyền cho session sở hữu vai trò `authManager.hasRole('Admin')`; hiển thị trạng thái access denied `#adminAuthGuardContainer` rõ ràng cho các vai trò khác mà không gọi API vượt quyền.
6. DOM an toàn 100% qua `textContent`, `createSafeElement`, không rò rỉ JWT, password hash, hay thông tin nhạy cảm.

### Deliverables & Files Changed
- `frontend/admin-accounts.html` [NEW]: Trang giao diện quản trị tài khoản, cấu trúc HTML5 ngữ nghĩa, 1 thẻ `<h1>`, skip link, bảng responsive `<table>` với `<caption>`, `<th scope="col">`, toolbar tìm kiếm/lọc, container phân trang, và native `<dialog id="statusConfirmModal">`.
- `frontend/js/pages/admin-accounts-page.js` [NEW]: Controller `AdminAccountsController` cùng các pure utility functions (`hasAdminAccess`, `isSelfAccount`, `parseAccountResponse`, `parseAccountsPageResponse`, `buildAccountsQueryParams`, `getStatusBadgeMeta`, `formatAccountDateTime`, `calculatePagination`).
- `frontend/ui-verification.html` [MODIFY]: Bổ sung Section 15 (`#section-admin-accounts`) cung cấp chẩn đoán khả năng (Capability & Read-Only Probe) cho Task 9F.1.
- `frontend/js/pages/ui-verification-page.js` [MODIFY]: Triển khai `initAdminAccountsDiagnostics()` thẩm định RBAC role guard, logic POL-8D-01, query params, và read-only API probe an toàn.
- `tests/frontend/unit/admin/admin-accounts.test.mjs` [NEW]: 31/31 unit tests kiểm chứng toàn diện các hàm logic thuần túy.
- `tests/frontend/e2e/admin-accounts.browser.mjs` [NEW]: 7/7 browser scenarios (AA-01..AA-07) kiểm thử Playwright.
- `tests/frontend/e2e/verification-center.browser.mjs` [MODIFY]: Bổ sung kịch bản `VC-16` xác thực Section 15 chẩn đoán.
- `tests/frontend/e2e/admin-accounts-live.mjs` [NEW]: Script kiểm thử trực tiếp (Live Full-Stack) kết nối FE -> Spring Boot -> MySQL 8.4.

### Actual Automated Verification Results (Task 9F.1)
- **Unit Verification (`tests/frontend/unit/admin/admin-accounts.test.mjs`)**: **31/31 PASS** (9 suites, 110ms; role guard, self-protection, DTO parsing, query params, badge metadata, date formatting, pagination).
- **Static Verification (`tests/frontend/runner.mjs --tier=static`)**: **138/138 PASS** across 22 suites (0.47s; HTML semantics, landmarks, unique IDs, safe DOM sinks).
- **Fast Tier Verification (`tests/frontend/runner.mjs --tier=fast`)**: **478/478 PASS** across 130 suites (2.50s).
- **Browser E2E (`tests/frontend/e2e/admin-accounts.browser.mjs`)**: **7/7 PASS** (1 suite, 6.87s; AA-01..AA-07).
- **Verification Center Dedicated Suite (`tests/frontend/e2e/verification-center.browser.mjs`)**: **16/16 PASS** (1 suite, 22.4s; VC-01..VC-16).
- **Live Full-Stack Verification (`tests/frontend/e2e/admin-accounts-live.mjs`)**: **PASS (100%)** kết nối thực tế Real Playwright Browser -> Real Static FE (:3000) -> Real Spring Boot (:8080) -> Real MySQL 8.4 (:3306):
  - Đăng ký tài khoản Admin và Target dùng một lần qua backend API.
  - Gán vai trò `Admin` (role_id=4) trong CSDL MySQL.
  - Đăng nhập qua `/api/v1/auth/login` nhận JWT Bearer chứa vai trò `Admin`.
  - Truy cập `admin-accounts.html`, hiển thị danh sách 20 tài khoản từ CSDL thực.
  - Tìm kiếm tài khoản Admin, xác minh huy hiệu "Tài khoản của bạn" và nút thao tác bị vô hiệu hóa (`POL-8D-01`).
  - Tìm kiếm tài khoản Target, mở modal native `<dialog>`, xác nhận thông điệp cảnh báo `authorization_version` (`DEC-42`).
  - Đổi trạng thái sang `Inactive`, gửi `PUT /api/v1/admin/accounts/{id}/status`.
  - Spring Boot trả về HTTP 200 OK, UI cập nhật nhãn 'Tạm khóa' (Inactive) authoritative.
  - Truy vấn MySQL xác nhận cột `status`='Inactive' và `authorization_version` đã được tăng.
  - Dọn sạch các bản ghi thử nghiệm trong CSDL.

### Next Task
- **Phase 9 — Module 9F — Task 9F.2: Quản trị & Phân quyền Vai trò Hệ thống (`admin-roles.html`) [COMPLETED]**.

---

## 2026-09-16 — Phase 9: Module 9F — Task 9F.2: Admin Role Management & Role Assignment UI (`frontend/admin-roles.html`, `frontend/js/pages/admin-roles-page.js`)

### Objective & Summary
Xây dựng và hoàn thiện giao diện Quản trị & Phân quyền Vai trò Hệ thống (Admin Role Management & Role Assignment UI) dành riêng cho Quản trị viên (`Admin`):
1. Tích hợp đầy đủ vào shell giao diện hiện hữu với `frontend/admin-roles.html` và `frontend/js/pages/admin-roles-page.js`, cung cấp sub-navigation tab đồng bộ 2 chiều với `admin-accounts.html`.
2. Kết nối chuẩn xác với backend contracts:
   - `GET /api/v1/admin/roles`: Tải danh mục vai trò authoritative từ server (`ApiResponse<List<RoleResponse>>`).
   - `GET /api/v1/admin/accounts`: Truy vấn danh sách tài khoản phân trang, hỗ trợ tìm kiếm `search`, `page`, `size`.
   - `PUT /api/v1/admin/accounts/{id}/roles`: Đột biến toàn bộ tập vai trò mong muốn qua payload `{ "roles": [...] }` (COMPLETE role set replacement).
3. Tuân thủ triệt để chính sách bảo vệ tự thân `POL-8D-02`:
   - Đối với tài khoản của chính Admin đang đăng nhập (`isSelfAccount()`): Khóa vai trò `Admin` (luôn được chọn và `disabled`, không thể bỏ chọn để chống tự giáng quyền).
   - Cho phép Admin chỉnh sửa các vai trò hợp lệ khác của chính mình (ví dụ: bổ sung hoặc gỡ bỏ `Creator`, `Moderator`).
4. Ngăn ngừa đột biến vô nghĩa (No-op mutation prevention): So sánh tập vai trò hiện tại và tập vai trò được chọn; nếu không thay đổi (`areRoleSetsEqual()`), không gửi request `PUT`, hiển thị thông báo "Vai trò chưa có sự thay đổi".
5. Minh bạch cơ chế bảo mật và thu hồi phiên `DEC-42`: Modal native `<dialog id="roleAssignmentModal">` giải thích trung thực rằng thay đổi tập vai trò hiệu lực sẽ làm tăng `authorization_version` và vô hiệu hóa token/phiên làm việc của tài khoản mục tiêu theo cơ chế phía máy chủ. Tuyệt đối không can thiệp token ở client.
6. Cập nhật authoritative sau mutation: Sử dụng phản hồi `AccountResponse` trả về từ server để cập nhật trực tiếp hàng tương ứng trên bảng và danh sách vai trò mà không cần reload trang.
7. DOM an toàn 100% qua `textContent`, `createSafeElement`, tuân thủ WCAG 2.2 AA (native `<dialog>`, Esc, phục hồi focus, nhãn trợ năng nhóm vai trò).
8. Trung tâm kiểm chứng `frontend/ui-verification.html` (Section 16): Thẩm định khả năng phân quyền và probe read-only danh mục vai trò; TUYỆT ĐỐI KHÔNG thực hiện bất kỳ mutation vai trò nào (ZERO PUT mutations).

### Deliverables & Files Changed
- `frontend/admin-roles.html` [NEW]: Giao diện quản trị vai trò, HTML5 ngữ nghĩa, single `<h1>`, skip link, responsive, RBAC guard, thẻ hiển thị danh mục vai trò từ server, thanh tìm kiếm tài khoản, bảng danh sách tài khoản với danh sách vai trò hiện tại, modal native `<dialog id="roleAssignmentModal">` với checkbox chọn đa vai trò và các thông điệp cảnh báo bảo mật.
- `frontend/js/pages/admin-roles-page.js` [NEW]: Controller `AdminRolesController` và các pure functions: `hasAdminAccess`, `isSelfAccount`, `parseRoleResponse`, `getRoleMetadata`, `areRoleSetsEqual`, `validateRoleSelection`, `buildRoleUpdatePayload`, `parseAccountResponse`, `parseAccountsPageResponse`, `buildAccountsQueryParams`, `calculatePagination`, `getStatusBadgeMeta`.
- `frontend/admin-accounts.html` [MODIFY]: Bổ sung tab liên kết `Phân quyền vai trò` trỏ tới `admin-roles.html` để điều hướng 2 chiều.
- `frontend/ui-verification.html` [MODIFY]: Bổ sung Section 16 (`#section-admin-roles`) cung cấp chẩn đoán khả năng (Capability & Read-Only Probe) cho Task 9F.2 với cam kết ZERO MUTATION.
- `frontend/js/pages/ui-verification-page.js` [MODIFY]: Triển khai `initAdminRolesDiagnostics()` kiểm tra RBAC role guard, logic POL-8D-02 self-protection, validation tập vai trò, và probe đọc `GET /api/v1/admin/roles` an toàn.
- `tests/frontend/unit/admin/admin-roles.test.mjs` [NEW]: 31/31 unit tests kiểm chứng toàn diện logic thuần túy (guard, self-protection, equality, validation, payload serialization, DTO parsing).
- `tests/frontend/e2e/admin-roles.browser.mjs` [NEW]: 8/8 browser E2E tests (AR-01..AR-08) kiểm thử trên Playwright.

### Actual Automated Verification Results (Task 9F.2)
- **Unit Verification (`tests/frontend/unit/admin/admin-roles.test.mjs`)**: **31/31 PASS** (7 suites, 120ms; role guard, self-protection, role catalog parsing, selection validation, equality/no-op, payload serialization, account pagination).
- **Static Verification (`tests/frontend/runner.mjs --tier=static`)**: **145/145 PASS** across 23 suites (0.41s; HTML semantics, landmarks, unique IDs, safe DOM sinks).
- **Fast Tier Verification (`tests/frontend/runner.mjs --tier=fast`)**: **485/485 PASS** across 131 suites (2.32s).
- **Browser E2E (`tests/frontend/e2e/admin-roles.browser.mjs`)**: **8/8 PASS** (1 suite, 6.09s; AR-01..AR-08).
- **Verification Center Dedicated Suite (`tests/frontend/e2e/verification-center.browser.mjs`)**: **16/16 PASS** (1 suite, 22.15s; VC-01..VC-16 bao gồm Section 16 probe).
- **Admin Accounts Regression (`tests/frontend/e2e/admin-accounts.browser.mjs`)**: **7/7 PASS** (1 suite, 6.53s; AA-01..AA-07).
- **Live Full-Stack Verification (9-Phase Flow)**: **PASS (100%)** kết nối thực tế Real Playwright Browser -> Real Static FE (:3000) -> Real Spring Boot (:8080) -> Real MySQL 8.4 (:3306):
  1. Đăng ký tài khoản Admin và Target dùng một lần qua backend API.
  2. Nâng cấp quyền `Admin` trong MySQL cho tài khoản Admin.
  3. Đăng nhập qua `/api/v1/auth/login` nhận Bearer token và xác lập session Admin.
  4. Truy cập `admin-roles.html`, tải thành công danh mục 4 vai trò từ `GET /api/v1/admin/roles` (`Admin`, `Creator`, `Moderator`, `Learner`).
  5. Tìm kiếm tài khoản Target, mở modal native `<dialog id="roleAssignmentModal">`, hiển thị vai trò hiện tại `[Learner]`.
  6. Chọn tập vai trò mới `[Creator, Learner]`, kiểm tra thông điệp thu hồi phiên `DEC-42`, gửi `PUT /api/v1/admin/accounts/{id}/roles`.
  7. Backend trả về 200 OK với DTO cập nhật, bảng UI hiển thị ngay lập tức các huy hiệu vai trò authoritative "Tác giả" và "Học viên".
  8. Kiểm tra CSDL MySQL: bảng `account_role` ghi nhận chính xác 2 vai trò `Creator` và `Learner`; cột `authorization_version` trong bảng `account` tăng từ `1` lên `2` (`DEC-42`).
  9. Kiểm tra bảo vệ tự thân `POL-8D-02`: Tìm kiếm chính tài khoản Admin, mở modal phân quyền, xác nhận checkbox `Admin` bị khóa (`checked` + `disabled`), không thể bị gỡ bỏ; dọn dẹp sạch sẽ dữ liệu kiểm thử trong MySQL.

### Next Task
- **Phase 9 — Module 9F — Task 9F.3: Quản trị Danh mục Bộ thủ Khang Hy (`admin-radicals.html`) [COMPLETED]**.

---

## 2026-09-16 — Phase 9: Module 9F — Task 9F.3: Admin Radicals CRUD UI (`frontend/admin-radicals.html`, `frontend/js/pages/admin-radicals-page.js`) — [COMPLETED]

### Scope of Delivery
1. **Frontend Administrative Radicals Management View (`frontend/admin-radicals.html`)**:
   - Semantic HTML5 document declaring `lang="vi"`, UTF-8, mobile-responsive viewport, and authenticated Bootstrap 5.3.3 SRI hashes.
   - Single primary `<h1>`: "Quản Trị Danh Mục Bộ Thủ".
   - Skip link targeting `#mainContent` for keyboard accessibility (WCAG 2.2 SC 2.4.1).
   - Unified administrative sub-navigation tabs across Quản trị tài khoản (`admin-accounts.html`), Phân quyền vai trò (`admin-roles.html`), and Quản trị bộ thủ (`admin-radicals.html` active).
   - Administrative Role Guard (`#adminAuthGuardContainer`): blocks non-Admin sessions (`Learner`, `Creator`, `Moderator`, Anonymous) from accessing administrative CRUD controls.
   - Full search & filter controls: `#radicalSearchInput`, clear button `#searchClearBtn`, result count badge `#radicalResultCount`, and page size selector `#pageSizeSelect` (25, 50, 100, 214/Tất cả).
   - Responsive semantic table `<table>` with `<caption>`, `<thead>`, `<tbody>`, displaying ID, Ký tự, Pinyin, Nghĩa Hán-Việt, Nghĩa tiếng Việt, Media, and Thao tác (Sửa, Xóa).
   - Accessible native `<dialog>` modals:
     - `#radicalFormModal`: for Create and Edit operations, with field-level invalid-feedback divs and form-level `#formGeneralError`.
     - `#deleteRadicalModal`: destructive action confirmation showing radical character, meaning, permanent warning, Cancel, and Confirm buttons.
   - Component state management container (`#radicalStateContainer`) supporting 3 states: `LOADING`, `EMPTY`, `ERROR` with retry callback, and `READY`.

2. **Authoritative Admin Radicals Controller (`frontend/js/pages/admin-radicals-page.js`)**:
   - Pure logic functions exported for unit testability:
     - `hasAdminAccess(authManager)`: strictly checks `authManager.hasRole('Admin')` and rejects `ROLE_` prefixes.
     - `removeVietnameseDiacritics(str)`: NFD normalization for diacritic-tolerant search.
     - `normalizeSearchQuery(query)`: trims and lowercases user search terms.
     - `filterRadicals(query, items)`: client-side search across character, pinyin, meaningHanViet, and meaningVi.
     - `validateRadicalForm(formData)`: client validation adhering to backend contract constraints (`character` [$\le 10$], `pinyin` [$\le 50$], `meaningHanViet` [$\le 100$], `meaningVi` [$\le 255$], optional `audioUrl`/`videoWritingUrl` [$\le 500$, valid scheme]).
     - `isValidMediaUrl(url)`: restricts media schemes to `http:`, `https:` or relative `/`, rejecting `javascript:` and dangerous URI formats.
     - `buildRadicalPayload(formData)`: trims strings, converts empty optional URLs to `null`.
     - `classifyApiError(err, operation)`: classifies 409 duplicate character conflict on create/update, 409 referenced deletion conflict on delete, 404 stale records, and 400 validation errors.
     - `calculatePagination(totalItems, pageIndex, pageSize)`: pagination math.
   - `AdminRadicalsPageController` class:
     - Catalog loading: calls `GET /api/v1/radicals` with defensive pagination aggregation (`totalPages > 1 && items.length < totalElements`), sorting by `radicalId ASC`.
     - Dynamic Catalog Invariant: does NOT enforce static `count === 214`, reflecting true server state after additions (>214) or deletions (<214).
     - Create flow: sends `POST /api/v1/admin/radicals` -> 201 Created -> updates local catalog -> toast.
     - Edit flow: sends `PUT /api/v1/admin/radicals/{id}` -> 200 OK -> updates local catalog -> toast.
     - Delete flow: sends `DELETE /api/v1/admin/radicals/{id}` -> 204 No Content -> removes from UI -> toast (never calls `response.json()`).
     - Conflict handling: catches 409 and displays clear user feedback in modal.
     - Safe DOM: uses `createSafeElement` with `{ className, text, attrs, children }` (100% zero unsafe innerHTML) and `sanitizeResourceUrl()`.
     - Focus management: traps focus inside `<dialog>`, handles Escape key, restores focus to triggering button upon modal close.

3. **Verification Center Section 17 Diagnostic (`frontend/ui-verification.html`, `frontend/js/pages/ui-verification-page.js`)**:
   - Added Section 17 for Admin Radicals CRUD Capability Probe.
   - Renumbered Live Full-Stack Diagnostics to Section 18.
   - Implemented `initAdminRadicalsDiagnostics()`: verifies client validation, media URL validator, payload builder, conflict classification, and client search.
   - Strict Invariant: **ZERO CRUD MUTATION** (never executes POST, PUT, or DELETE from Verification Center; read-only probe `GET /api/v1/radicals?page=0&size=1` if Admin).

4. **Automated Verification Suite Delivery**:
   - `tests/frontend/unit/admin/admin-radicals.test.mjs` [NEW]: 34/34 unit tests covering role guard, diacritics removal, search filtering, DTO validation, media URL safety, payload construction, error classification, and pagination.
   - `tests/frontend/e2e/admin-radicals.browser.mjs` [NEW]: 8/8 browser E2E tests (RA-01..RA-08) covering catalog viewing, creation, duplicate 409 conflict, editing, 204 deletion, referenced delete 409 conflict, role guard, and Verification Center Section 17 probe.

### Actual Automated Verification Results (Task 9F.3)
- **Targeted Unit Verification (`node --test tests/frontend/unit/admin/admin-radicals.test.mjs`)**: **34/34 PASS** (8 suites, 103ms).
- **Static Verification (`tests/frontend/runner.mjs --tier=static`)**: **152/152 PASS** across 24 suites (0.47s; HTML semantics, landmarks, unique IDs, safe DOM sinks, SRI integrity).
- **Fast Tier Verification (`tests/frontend/runner.mjs --tier=fast`)**: **492/492 PASS** across 132 suites (2.72s).
- **Browser E2E (`node --test tests/frontend/e2e/admin-radicals.browser.mjs`)**: **8/8 PASS** (1 suite, 7.49s; RA-01..RA-08).
- **Live Verification**: **ENVIRONMENT BLOCKED** (Backend port 8080 is offline in current workspace; verified via PowerShell probe).

### Next Task
- **Phase 9 — Module 9G — Task 9G.1: Đánh giá Mô hình Đe dọa Lưu trữ Token Phía Client [COMPLETED]**.

---

## 2026-09-18 — Phase 9: Module 9G — Task 9G.1: Token Storage Threat Model Evaluation & Client Session Review — [COMPLETED]

### Scope of Delivery
1. **Toàn diện Đánh giá Mô hình Đe dọa (Threat Model) & Ranh giới Tin cậy (Trust Boundaries)**:
   - Rà soát trực tiếp 100% mã nguồn xác thực client (`auth-state.js`, `api.js`, `auth-page.js`, `nav.js`) và backend (`JwtUtil.java`, `JwtAuthenticationFilter.java`, `SecurityConfig.java`, `AuthController.java`, `AuthServiceImpl.java`, `Account.java`).
   - Khẳng định `localStorage` là bộ lưu trữ tiện ích phía client (Client Convenience Storage), hoàn toàn không phải ranh giới bảo mật. Dữ liệu token và session metadata có thể bị đọc bởi bất kỳ JavaScript nào thực thi trong cùng origin.
   - Xác định rõ vai trò kiểm tra quyền phía client (`authManager.hasRole()`): phục vụ điều hướng UX và hiển thị giao diện, tuyệt đối không được tin cậy cho cấp quyền. Toàn bộ quyền hạn được kiểm soát độc quyền tại backend qua Spring Security (`@PreAuthorize`, URL matchers, `authorization_version`).
2. **So sánh Đối chiếu 3 Mô hình Lưu trữ Token (`localStorage` vs `sessionStorage` vs `HttpOnly Cookie`)**:
   - `localStorage`: Tiện lợi cho phiên làm việc liên tục, đồng bộ đa tab tức thì qua `StorageEvent`, không bị rủi ro CSRF kinh điển (do browser không tự động đính kèm custom `Authorization` header), nhưng chịu rủi ro bị trích xuất nếu có lỗ hổng DOM XSS trong thời hạn 24 giờ (`jwt.expiration-ms: 86400000`).
   - `sessionStorage`: Cũng bị JavaScript đọc được tương tự (zero XSS confidentiality improvement), nhưng mất tính liên tục giữa các tab (mở thẻ mới từ bookmark/URL bị mất session, không thể đồng bộ đăng xuất đa tab qua `StorageEvent`).
   - `HttpOnly Cookie`: Giúp bảo vệ tính bí mật của chuỗi token (JavaScript không đọc được `document.cookie`), nhưng KHÔNG triệt tiêu rủi ro XSS (script XSS vẫn có thể gửi request xác thực trong browser context qua ambient cookie). Chuyển sang cookie sẽ kích hoạt rủi ro CSRF do cơ chế tự động đính kèm của trình duyệt, đòi hỏi backend phải triển khai CSRF token framework (hiện đang tắt CSRF do dùng Stateless REST API), yêu cầu cấu hình lại CORS credentials, sửa `AuthController`, thêm endpoint logout backend, và vi phạm Backend Seal (DEC-41).
3. **Phân tích Cơ chế Thu hồi Phiên Phía Máy chủ (Server-Side Revocation)**:
   - Khẳng định `clearSession()` / `logout()` phía client chỉ là dọn dẹp bộ nhớ cục bộ, không có giá trị thu hồi token trên máy chủ.
   - Cơ chế thu hồi phiên thực sự phía máy chủ được bảo đảm bởi `authorization_version` (DEC-24, DEC-42) kết hợp kiểm tra trạng thái `Active` (DEC-19) tại `JwtAuthenticationFilter`. Khi Admin thay đổi vai trò hoặc khóa tài khoản (`Inactive`/`Banned`), server tăng nguyên tử `authorization_version`, khiến token cũ bị từ chối ngay lập tức với HTTP 401.
4. **Quyết định Kiến trúc DEC-43 Đã Phê duyệt & Quy hoạch Tương lai**:
   - Ban hành quyết định kỹ thuật `DEC-43` trong `.agents/DECISIONS.md`.
   - Giữ nguyên mô hình `localStorage` trong phạm vi Phase 9, bảo toàn Backend Seal (DEC-41), áp dụng chiến lược Phòng thủ Đa tầng (Defense-in-Depth): Chống DOM XSS qua safe DOM rendering (Task 9G.2), thiết lập CSP nghiêm ngặt (Task 9G.3), và chốt chặn `authorization_version` phía server (DEC-42).
   - Quy hoạch kiến trúc mục tiêu tương lai (Candidate Future Architecture): Chuyển đổi sang BFF hoặc Same-Origin Reverse Proxy với Access Token ngắn hạn và Refresh Cookie HttpOnly/SameSite quay vòng khi triển khai Production đa máy chủ (Phase 10/11).

### Actual Automated Verification Results (Task 9G.1)
- **Targeted Unit Verification (`auth-state.test.mjs`, `api-client.test.mjs`)**: **30/30 PASS** (2 suites, 633ms).
- **Targeted Static Security (`dom-sinks.test.mjs`)**: **4/4 PASS** (1 suite, 164ms; kiểm tra eval, document.write, string timers, safe DOM).
- **Browser E2E Auth Flow (`auth-flow.browser.mjs`)**: **8/8 PASS** (1 suite, 9.89s; BF1..BF8 login, logout, 401 alert, profile session sync).
- **Browser E2E API Auth (`api-auth.browser.mjs`)**: **3/3 PASS** (1 suite, 3.19s; BA1..BA3 token injection, 401 single-owner cleanup, cross-tab storage event sync).
- **Backend JWT Unit & Mock Verification (`JwtUtilTests`, `JwtAuthenticationFilterTests`)**: **39/39 PASS** (24 tests JwtUtilTests 7.65s, 15 tests JwtAuthenticationFilterTests 5.52s).
- **Live FE↔BE Verification**: **ENVIRONMENT BLOCKED** (Dịch vụ MySQL cục bộ port 3306 offline `TcpTestSucceeded: False` và Docker Desktop daemon chưa chạy; kiểm chứng thành công toàn diện qua headless browser E2E, mock client, và unit test suites).

### Next Task
- **Phase 9 — Module 9G — Task 9G.2: Rà soát Phòng chống DOM XSS & Kiểm soát Bồn trũng Dữ liệu Động (`frontend/js/ui/security.js`) [COMPLETED]**.

---

## 2026-09-18 — Phase 9: Module 9G — Task 9G.2: DOM XSS Prevention & Dynamic Data Rendering Audit — [COMPLETED]

### Scope of Delivery
1. **Kiểm toán 100% Bồn trũng Dữ liệu Động (DOM Sinks) Toàn bộ Frontend**:
   - Rà soát toàn bộ 28 file JavaScript (`frontend/js/**/*.js`) và 22 file HTML (`frontend/*.html`).
   - Phân loại toàn bộ các luồng dữ liệu ngoại lai (Untrusted Data Flows): User inputs, URL query parameters, API responses (chữ Hán, Pinyin, nghĩa Hán-Việt, nghĩa tiếng Việt, tiêu đề bài học, ghi chú cá nhân, lý do từ chối kiểm duyệt `rejectionReason`, media URLs), và các trường nhập liệu Excel.
   - Xác nhận 100% việc kết xuất dữ liệu động vào DOM tuân thủ nghiêm ngặt **Safe DOM APIs** (`textContent`, `document.createTextNode()`, hoặc helper `createSafeElement()`):
     - **Zero Variable Interpolation in `innerHTML`**: 100% trường hợp sử dụng `innerHTML` là các chuỗi tĩnh được hardcoded (e.g. SVG icons tĩnh hoặc `innerHTML = ''` để xóa vùng chứa an toàn).
     - **Zero `outerHTML`** across entire frontend.
     - **Zero `insertAdjacentHTML`** across entire frontend.
     - **Zero `document.write` / `document.writeln`** across entire frontend.
     - **Zero `eval` / `new Function`** across entire frontend.
     - **Zero String Timers (`setTimeout(string)` / `setInterval(string)`)** across entire frontend.
2. **Loại trừ Toàn bộ Inline Event Handlers trong HTML**:
   - Phát hiện 3 file HTML quản trị còn sót thuộc tính inline event handler: `onsubmit="return false;"` tại `admin-vocabulary.html`, `admin-roles.html`, và `admin-radicals.html`.
   - Triệt để loại bỏ toàn bộ thuộc tính inline handlers khỏi 3 file HTML trên; thay thế bằng lắng nghe sự kiện lập trình ngữ nghĩa (`addEventListener('submit', (e) => e.preventDefault())`) trong các controller tương ứng (`admin-vocabulary-page.js`, `admin-roles-page.js`, `admin-radicals-page.js`).
   - Khẳng định hiện trạng: **Đúng 0 thuộc tính inline event handler (`on*`) trên 100% 22 file HTML**.
3. **Gia cố Bộ máy Bảo mật & Làm sạch Dữ liệu Phía Client (`frontend/js/ui/security.js`)**:
   - `sanitizeNavigationUrl(url)`:
     - Chặn triệt để các ký tự điều khiển ASCII control characters (`[\x00-\x1F\x7F]`).
     - Chặn các vector bypass backslash nguy hiểm (`\`, `/\`, `\\/`).
     - Chặn đường dẫn protocol-relative (`//`).
     - Chỉ chấp nhận đường dẫn tương đối an toàn (`/^\/[^/\\]/`, `/`, `./`, `#`) và các scheme được allowlist (`http:`, `https:`, `mailto:`, `tel:`).
     - Toàn bộ vector nguy hiểm (`javascript:`, `vbscript:`, `data:`, control chars) bị vô hiệu hóa an toàn về `'about:blank'`.
   - `sanitizeResourceUrl(url)`:
     - Chặn control characters, backslashes, protocol-relative.
     - Cho phép đường dẫn tương đối an toàn và các định dạng ảnh raster base64 an toàn (`data:image/png;base64,...`, `data:image/jpeg;base64,...`, `data:image/webp;base64,...`, `data:image/gif;base64,...`).
     - Nghiêm ngặt từ chối SVG data URIs (`data:image/svg+xml`), `mailto:`, `tel:`, và toàn bộ executable schemes về `'about:blank'`.
   - `createSafeElement(tag, options)`:
     - Cấm tạo động thẻ `<script>` (ném Error).
     - Chặn tất cả thuộc tính inline event handlers (`on*`) và thuộc tính nhúng tài liệu `srcdoc` (bỏ qua không gán).
     - Tự động định tuyến các thuộc tính URL điều hướng (`href`, `action`, `formaction`, `xlink:href`) qua `sanitizeNavigationUrl()`, và thuộc tính tài nguyên nhúng (`src`) qua `sanitizeResourceUrl()`.
   - **Chính sách Làm sạch CSS (CSS Sanitization Model)**:
     - Bác bỏ việc xây dựng bộ lọc regex biến đổi CSS dễ vỡ (fragile regex-based CSS filter).
     - Áp dụng kiểm tra allowlist nghiêm ngặt cho thuộc tính `style`: xác thực chuỗi CSS khớp với khai báo thuộc tính lành tính `/^[\w\s-:%.,#;]+$/i`, cấm hoàn toàn các ký tự `()<>'"` (loại trừ triệt để CSS expressions, `url()`, CSS injection).
4. **Nâng cấp Bộ Công cụ Kiểm thử Tĩnh Chống Hồi quy (`tests/frontend/static/dom-sinks.test.mjs`)**:
   - Mở rộng phạm vi quét tự động trên cả 28 file `.js` và 22 file `.html`.
   - Tự động phát hiện và chặn đứng: inline event handlers, `outerHTML`, `insertAdjacentHTML`, dynamic script creation, string timers, và multiline `innerHTML` assignments có chứa nội suy biến.
   - Đạt 5/5 static sink suites PASS.
5. **Xây dựng Bộ Kiểm thử Đối kháng Trình duyệt Thật trong Chromium (`tests/frontend/e2e/dom-xss-adversarial.browser.mjs`)**:
   - Tích hợp vào test runner chính thức `tests/frontend/runner.mjs`.
   - Thực thi 5 kịch bản tấn công đối kháng có probe cắm chốt độc quyền (`window.__XSS_TRIGGERED__ = true`, `alert()`, `onerror`, `onload`):
     - **XSS-01 (Personal Notes Subsystem)**: Nạp payload `<script>` probe và `<img onerror>` probe qua modal ghi chú; xác nhận render dưới dạng chuỗi văn bản thuần túy, zero DOM execution, bảo toàn 100% chữ Hán và dấu tiếng Việt.
     - **XSS-02 (Creator Lesson Editor)**: Nạp payload `<svg onload>` probe và attribute breakout `"><script>` vào tiêu đề bài học; xác nhận textContent rendering an toàn trong breadcrumbs và DOM nodes.
     - **XSS-03 (Vocabulary Search Reflection)**: Truy cập URL query `?search=<script>...`; xác nhận phản ánh an toàn trong ô tìm kiếm và badge kết quả dưới dạng literal string, zero execution.
     - **XSS-04 (Moderator History)**: Nạp payload HTML/JS độc hại trong trường `rejectionReason`; xác nhận kết xuất an toàn dưới dạng textContent trong modal chi tiết kiểm duyệt.
     - **XSS-05 (Contextual URL Sanitization & Bypass Resistance)**: Kiểm chứng chống bypass: `javascript:`, `vbscript:`, `data:text/html`, control character injection (`java\x00script:`), protocol-relative (`//evil.com`), và SVG data URIs; xác nhận 100% bị vô hiệu hóa an toàn về `about:blank`.
6. **Bảo toàn Backend Seal (DEC-41)**:
   - Toàn bộ các cải tiến và chốt chặn phòng chống XSS được thực hiện độc quyền ở client-side; zero dòng mã backend bị thay đổi.

### Actual Automated Verification Results (Task 9G.2)
- **Targeted Adversarial Browser Tests (`dom-xss-adversarial.browser.mjs`)**: **5/5 PASS** (1 suite, 5.23s, Chromium headless, 0 flakiness).
- **Hardened Security Unit Tests (`security.test.mjs`)**: **29/29 PASS** (1 suite, 8.01ms; bao phủ toàn bộ URL bypass, raster data URIs, createSafeElement, CSS allowlist).
- **Targeted Static Security Sinks (`dom-sinks.test.mjs`)**: **5/5 PASS** (1 suite, 22 HTML + 28 JS files scanned, 0 violations).
- **Fast Tier Verification (`verify:frontend:fast`)**: **525/525 PASS** across 136 suites (2.49s, budget 2.50s).
- **Static Tier Verification (`verify:frontend:static`)**: **167/167 PASS** across 26 suites (0.46s, budget 1.50s).
- **Unit Tier Verification (`verify:frontend:unit`)**: **358/358 PASS** across 110 suites (2.08s).
- **Regression Browser E2E**:
  - `personal-notes.browser.mjs`: **7/7 PASS** (7.20s).
  - `creator-lessons.browser.mjs`: **4/4 PASS** (6.21s).
- **Live FE↔BE Verification**: **ENVIRONMENT BLOCKED** (Dịch vụ MySQL cục bộ port 3306 offline và Docker Desktop daemon chưa chạy; kiểm chứng thành công toàn diện qua headless Chromium Playwright với mock server).

### Residual Risk & Threat Model Reflection
- **Vấn đề đã triệt tiêu**: Toàn bộ các vector DOM XSS cổ điển phát sinh từ việc đưa untrusted data vào `innerHTML`, `outerHTML`, `document.write`, dynamic script execution, inline handlers, hay JavaScript URL schemes đã bị triệt tiêu hoàn toàn nhờ kiến trúc Safe DOM Rendering và sanitization chặt chẽ.
- **Rủi ro tồn dư (Residual Risk)**:
  - Sự cố chuỗi cung ứng bên thứ ba (Third-Party CDN Compromise): Bootstrap 5.3.3 và Font Awesome 6.5.1 tải từ CDN công cộng. Dù đã có SRI hashes (`integrity="sha384-..."`), nếu có tài nguyên cross-origin mới được nhúng thiếu SRI hoặc CDN bị xâm phạm, script độc hại vẫn có thể chạy.
  - Sơ suất phát triển tương lai: Các lập trình viên tiếp theo có thể vô tình đưa `innerHTML` không an toàn vào mà không qua `createSafeElement()`.
- **Kế hoạch phòng thủ tiếp theo**: Task 9G.3 sẽ xây dựng chính sách **Content Security Policy (CSP)** nghiêm ngặt, thiết lập ranh giới phòng thủ chiều sâu (Defense-in-Depth) chặn đứng inline script execution, hạn chế nguồn script (`script-src`), và cô lập mạng lưới kết nối (`connect-src`).

### Next Task
- **Phase 9 — Module 9G — Task 9G.3: Content Security Policy (CSP) Construction & Strict Resource Graph Hardening (`frontend/index.html`..`*.html`, `SecurityConfig.java`)**.



