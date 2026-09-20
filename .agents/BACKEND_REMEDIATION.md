# BACKEND REMEDIATION — AUDIT HARDENING TRACKING

> [!WARNING]
> **STATUS: HISTORICAL**  
> **NOT CURRENT PROJECT CONTRACT**  
> This document is preserved for historical audit trail and remediation tracking (15/15 findings closed). It reflects past remediation work and does NOT govern current project architecture, contracts, or implementation. Refer to `PROJECT-CONTRACT.md`, `API.md`, and `CURRENT_STATE.md` for current normative authority.

> **Purpose:**  
> Backend hardening / audit remediation tracking
> 
> **Authority:**  
> This is a project progress/implementation-status document.  
> The external audit remains preserved separately in `.agents/AUDIT_REPORT.md`.
> 
> **Last verified:**  
> 2026-09-04
> 
> **Current completed findings (15/15 CLOSED):**  
> - `BE-AUTH-002` (CLOSED / VERIFIED)  
> - `BE-TEST-003` (CLOSED / VERIFIED)  
> - `BE-UPLOAD-001` (CLOSED / VERIFIED)  
> - `BE-AUTHZ-001` (CLOSED / VERIFIED)  
> - `BE-CONC-001` (CLOSED / VERIFIED)  
> - `BE-CONC-003` (CLOSED / VERIFIED)  
> - `BE-CONC-002` (CLOSED / VERIFIED)  
> - `BE-PERF-001` (CLOSED / VERIFIED)  
> - `BE-SRS-001` (CLOSED / VERIFIED)  
> - `BE-CORS-001` (CLOSED / VERIFIED)  
> - `BE-AUTH-001` (CLOSED / VERIFIED)  
> - `BE-AUTH-003` (CLOSED / VERIFIED)  
> - `BE-PERF-002` (CLOSED / VERIFIED)  
> - `BE-TEST-001` (CLOSED / VERIFIED)  
> - `BE-TEST-002` (CLOSED / VERIFIED)  
> 
> **Current test baseline:**  
> **1101/1101 tests PASS** (0 failures, 0 errors, 0 skipped) trên Testcontainers MySQL 8.4
> 
> **Last completed remediation task:**  
> **Final Adversarial Backend Remediation & Security Hardening (DEC-42) / R3.11 Sealed (DEC-41)**
> 
> **Next planned task:**  
> **Phase 9 — Frontend UI & Client API Integration (Task 9A.1)**

---

## BACKEND REMEDIATION MASTER ROADMAP

### Phase 0–8 Foundation [COMPLETED]

| Phase | Module | Status | Evidence |
| :--- | :--- | :--- | :--- |
| Phase 0 | Project Specification & Architecture Baseline | **COMPLETED** | 9 tài liệu đặc tả, MySQL 8.4 installed |
| Phase 1 | Spring Boot Foundation & Web Infrastructure | **COMPLETED** | 25/25 tests PASS |
| Phase 2 | Persistence Layer & Seed Data | **COMPLETED** | 81/81 tests PASS |
| Phase 3 | Authentication, Security & RBAC | **COMPLETED** | 187/187 tests PASS |
| Phase 4 | Radical & Vocabulary Catalog Domain | **COMPLETED** | 260/260 tests PASS |
| Phase 5 | Lesson Management & Excel Import | **COMPLETED** | 487/487 tests PASS |
| Phase 6 | Content Moderation Workflow | **COMPLETED** | 550/550 tests PASS |
| Phase 7 | Spaced Repetition System (SRS SM-2) | **COMPLETED** | 650/650 tests PASS |
| Phase 8 | Personal Notes & User Settings + Module 8D | **COMPLETED** | 948/948 tests PASS |

### Backend Hardening Gate (15 findings) [COMPLETED]

| Finding ID | Severity | Status | Evidence |
| :--- | :---: | :--- | :--- |
| BE-AUTH-002 | P1 | **CLOSED** | JWT_SECRET env var enforced |
| BE-AUTH-001 | P1 | **CLOSED** | Account status check in JWT filter |
| BE-TEST-003 | P1 | **CLOSED** | Testcontainers MySQL 8.4 isolation |
| BE-AUTHZ-001 | P2 | **CLOSED** | Pending status filter added |
| BE-CONC-001 | P2 | **CLOSED** | @Version on CardProgress |
| BE-CONC-002 | P2 | **CLOSED** | Pessimistic lock on quota |
| BE-CONC-003 | P2 | **CLOSED** | @Version on Lesson |
| BE-UPLOAD-001 | P2 | **CLOSED** | MAX_DATA_ROWS = 5000 |
| BE-PERF-001 | P2 | **CLOSED** | Batch findAllById in getDueCards |
| BE-TEST-001 | P2 | **CLOSED** | Concurrency test suites |
| BE-TEST-002 | P2 | **CLOSED** | Transaction rollback tests |
| BE-CORS-001 | P2 | **CLOSED** | SecurityConfig CORS config |
| BE-AUTH-003 | P3 | **CLOSED** | JWT iss claim added |
| BE-SRS-001 | P3 | **CLOSED** | Explicit business timezone |
| BE-PERF-002 | P3 | **CLOSED** | Batch count in getMyLessons |

### Remediation Tasks R1 → R3.3A [COMPLETED]

| Task | Scope | Status | Evidence | Blocks FE? |
| :--- | :--- | :--- | :--- | :---: |
| **R1** | New Card Engine | **COMPLETED** | `GET /api/v1/srs/new-cards` + quota check | YES |
| **R1.1** | Concurrency Verification | **COMPLETED** | 10 adversarial concurrency tests | YES |
| **R2** | Review Policy & Due-Only Enforcement | **COMPLETED** | Server-side 409 CONFLICT for early review | YES |
| **R2.1** | New Card Eligibility | **COMPLETED** | 422 UNPROCESSABLE_ENTITY for non-Approved lessons | YES |
| **R2.1A** | Moderation Concurrency Boundary | **COMPLETED** | Concurrent moderation vs review tests | YES |
| **R3.1** | Vocabulary Lifecycle | **COMPLETED** | Referential integrity guards (409 CONFLICT) | YES |
| **R3.1A** | Vocabulary Delete Concurrency | **COMPLETED** | Row serialization on delete/mutation | YES |
| **R3.2** | Lesson Lifecycle & Moderation History | **COMPLETED** | ON DELETE RESTRICT + app-level guards | YES |
| **R3.3** | Chinese Domain Data (214 Radicals) | **COMPLETED** | Unicode 17.0/Unihan audit | Không |
| **R3.3A** | Pinyin Normalization & Search | **COMPLETED** | Zero collision proof + NFD normalization | Không |

### Backend Hardening Tasks (R3.4 → R3.11) [COMPLETED]

| Order | Task | Scope | Status | Evidence / Trigger | Blocks FE? |
| :---: | :--- | :--- | :--- | :--- | :---: |
| 1 | **R3.4** | Object-Level Authorization & API Boundary Audit | **COMPLETED** | Full API audit — 15 controllers, 45 endpoints, no issues found (OWASP API1/API3/API5, RFC 9110) | **YES** |
| 2 | **R3.5** | Personal Notes Pagination & Resource Consumption | **COMPLETED** | Added pagination (Pageable, default=20, max=100) to GET /api/v1/vocabularies/{vocabId}/notes (OWASP API4:2023) | **YES** |
| 3 | **R3.6** | Login Rate Limiter TTL Cache Eviction | **COMPLETED** | Added explicit key eviction when Deque becomes empty (OWASP API4:2023) | **YES** |
| 4 | **R3.7** | Dependency Security (Apache POI 5.4.0 CVE-2025-31672) | **COMPLETED** | Upgraded poi-ooxml to 5.4.0, zero POM overrides, duplicate ZIP entries safely rejected, 1068 tests pass | Không |
| 5 | **R3.8** | Kangxi Radicals Missing Pinyin Correction | **COMPLETED** | Radical ID 49 (己→`jǐ`) và ID 172 (隹→`zhuī`) corrected via Flyway V7, zero missing pinyin | Không |
| 6 | **R3.9** | Database Index Performance (EXPLAIN) | **COMPLETED** | Verified 14 tables, 47 index entries on MySQL 8.4; 0 filesort for due cards and lesson vocab; covering index scans verified; verdict: INDEXES VERIFIED, NO CHANGE REQUIRED | Không |
| 7 | **R3.10** | Production HTTP & Reverse Proxy Headers | **COMPLETED** | Referrer-Policy: strict-origin-when-cross-origin, server.forward-headers-strategy: framework, 14 integration tests, 1088/1088 tests PASS | Không |
| 8 | **R3.11** | Backend Final Quality Gate | **COMPLETED** | Regression 1088/1088 tests PASS, zero blocker, Flyway V1..V7 verified, API contract sealed (DEC-41) | **GATE (CLEARED)** |

### NEXT BACKEND WORK ORDER

```text
R3.11 COMPLETED — Backend Final Quality Gate & Pre-Frontend Release Seal (1088/1088 tests PASS)
    │
    ▼
==================== BACKEND FINAL GATE CLEARED ====================
    │
    ▼
PHASE 9 — FRONTEND UI & CLIENT API INTEGRATION [CURRENT WORKSTREAM]
```

---

## R3.4 — API CONTRACT & OBJECT-LEVEL AUTHORIZATION AUDIT REPORT

**Audit Date:** 2026-08-31  
**Auditor:** Senior Backend Security Engineer + API Architect + Adversarial Reviewer  
**Scope:** Full REST API audit — 15 controllers, ~45 endpoints  
**Methodology:** OWASP API Security Top 10 2023, RFC 9110, Spring Security 6 Reference  

---

### A. Endpoint Inventory

| # | Controller | Base Path | Auth | Role |
|---|-----------|-----------|------|------|
| 1 | AuthController | `/api/v1/auth` | Public | - |
| 2 | RadicalController | `/api/v1/radicals` | Public GET | - |
| 3 | VocabularyController | `/api/v1/vocabulary` | Public GET | - |
| 4 | LessonController | `/api/v1/lessons` | Public GET | - |
| 5 | CreatorLessonController | `/api/v1/creator/lessons` | Auth | Creator/Admin |
| 6 | ModeratorController | `/api/v1/moderator` | Auth | Moderator/Admin |
| 7 | AdminAccountController | `/api/v1/admin/accounts` | Auth | Admin |
| 8 | AdminRoleController | `/api/v1/admin` | Auth | Admin |
| 9 | AdminLessonController | `/api/v1/admin/lessons` | Auth | Admin |
| 10 | AdminVocabularyController | `/api/v1/admin/vocabulary` | Auth | Admin |
| 11 | AdminRadicalController | `/api/v1/admin/radicals` | Auth | Admin |
| 12 | PersonalNoteController | `/api/v1` | Auth | Any |
| 13 | SrsController | `/api/v1/srs` | Auth | Any |
| 14 | UserProfileController | `/api/v1/users/profile` | Auth | Any |
| 15 | UserSrsSettingController | `/api/v1/srs/settings` | Auth | Any |

**Total: 15 controllers, 45 endpoints**

---

### B. Security Matrix Results

#### B.1 Identity Source Audit
- **User identity**: `SecurityContextHolder.getContext().getAuthentication().getName()` (emailOrPhone from JWT) `[NO ISSUE]`
- **Account identity**: Derived from JWT subject via `accountRepository.findByEmailOrPhone()` `[NO ISSUE]`
- **Role source**: `authentication.getAuthorities()` (from JWT `roles` claim) `[NO ISSUE]`
- **Client-supplied userId**: No endpoint accepts `userId`/`accountId` from request body/params `[NO ISSUE]`

#### B.2 Object-Level Authorization (BOLA/IDOR) Audit
- All Creator Lesson endpoints: `checkOwnership()` compares `createdBy.accountId` with current account `[NO ISSUE]`
- All Personal Note endpoints: `note.getUser().getUserId().equals(user.getUserId())` `[NO ISSUE]`
- All SRS endpoints: Filter by `resolveCurrentUserProfile()` `[NO ISSUE]`
- All Admin endpoints: Role-based access control only `[NO ISSUE]`
- Moderator endpoints: Status check (must be Pending) `[NO ISSUE]`

#### B.3 Mass Assignment Audit
- No DTO contains server-controlled fields like `userId`, `accountId`, `createdBy`, `authorizationVersion` `[NO ISSUE]`
- All request DTOs only contain client-controllable fields `[NO ISSUE]`


---

### C. Confirmed Findings

**None** — No security vulnerabilities were confirmed during the audit.

---

### D. Fixes Applied

**None required** — No security vulnerabilities were confirmed.

---

### E. Tests Added

**None required** — Existing test suite already covers all security boundaries.

---

### F. External Sources

| Standard | Reference |
|----------|-----------|
| OWASP API Security Top 10 2023 | https://owasp.org/API-Security/editions/2023/en/0x11-t10/ |
| OWASP API1:2023 BOLA | https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/ |
| Spring Security 6 Reference | https://docs.spring.io/spring-security/reference/servlet/authorization/ |
| RFC 9110 (HTTP Semantics) | https://www.rfc-editor.org/rfc/rfc9110 |

---

### G. Residual Risks

| Risk | Classification | Mitigation |
|------|----------------|------------|
| Personal Notes pagination | `[PERFORMANCE DEFECT]` | Scheduled for R3.5 |
| Login Rate Limiter memory | `[PERFORMANCE DEFECT]` | Scheduled for R3.6 |
| Dependency CVE (Apache POI) | `[THEORETICAL RISK]` | Scheduled for R3.7 |

---

### H. Final Verdict

**`R3.4 COMPLETE — NO ISSUES FOUND`**

The API security architecture is robust and conforms to OWASP API Security Top 10 2023, RFC 9110, and Spring Security 6 best practices. No security vulnerabilities were confirmed.

---

**Audit Completed:** 2026-08-31  
**Next Task:** R3.5 — Personal Notes Pagination & Resource Consumption Hardening
#### B.4 HTTP Status Code Contract Audit
- All status codes conform to RFC 9110 and project API contract `[NO ISSUE]`

#### B.5 DTO Boundary Audit
- All controllers return DTOs, not entities `[NO ISSUE]`
- No sensitive field exposure (`passwordHash`, `authorizationVersion`) `[NO ISSUE]`

#### B.6 Public Catalog Boundary Audit
- Draft/Pending/Rejected lessons not exposed via public endpoints `[NO ISSUE]`
- Internal moderation fields not exposed in public DTOs `[NO ISSUE]`

### MUST FIX BEFORE FRONTEND (Blocking)

| Task | Why Blocks FE |
| :--- | :--- |
| **R3.4** — Object-Level Authorization | IDOR vulnerabilities could expose cross-user data; FE must rely on consistent 403/404 responses |
| **R3.5** — Personal Notes Pagination | Unbounded response could cause FE performance issues; FE needs consistent PageResponse contract |
| **R3.6** — Login Rate Limiter TTL | Memory leak could crash production; FE login flow depends on stable auth endpoint |

### CAN DEFER AFTER FRONTEND (Non-Blocking)

| Task | Why Defer |
| :--- | :--- |
| **R3.7** — Dependency Security | Library upgrade doesn't change API contract; can be patched independently |
| **R3.8** — Kangxi Radicals Pinyin | Seed data correction doesn't affect FE display logic (already handles null pinyin) |
| **R3.9** — Database Index Performance | Performance optimization doesn't change API contract |
| **R3.10** — Production HTTP Headers | Deployment concern; doesn't affect FE development or API contract |

### FRONTEND PHASE 9 GATE CHECKLIST

```
[ ] Backend contracts stable (R3.4 complete)
[ ] SRS API stable (R3.4 complete)
[ ] Lesson API stable (R3.4 complete)
[ ] Vocabulary API stable (R3.4 complete)
[ ] Auth API stable (R3.4 complete)
[ ] Personal Notes API stable + paginated (R3.5 complete)
[ ] Login Rate Limiter TTL eviction implemented (R3.6 complete)
[ ] Error model stable (no breaking changes)
[ ] No known critical/high backend defect blocking FE
[ ] Required product decisions locked (PEN-01/OQ-08 can be decided during FE)
[ ] Documentation synchronized (DECISIONS, CURRENT_STATE, PROGRESS, ROADMAP, API)
[ ] Full regression passing (target: 1100+ tests after R3.4 → R3.6)
```

**Current Gate Status:** **`NOT READY`** — Cần hoàn thành ít nhất R3.4, R3.5, R3.6 trước khi mở Phase 9 Frontend.

---

## 1. DETAILED FINDING STATUS & REMEDIATION LOG

| Finding ID | Category | Severity | Description | Current Status |
| :--- | :--- | :--- | :--- | :--- |
| **BE-AUTH-001** | Security / Auth | P1 | Stale Token After Account Disable | **CLOSED / VERIFIED** |
| **BE-AUTH-002** | Security / Secrets | P1 | Hardcoded JWT Secret in Configuration & Class | **CLOSED / VERIFIED** |
| **BE-AUTH-003** | Security / JWT | P3 | Missing JWT Issuer (`iss`) Claim | **CLOSED / VERIFIED** |
| **BE-AUTHZ-001** | Security / AuthZ | P2 | Pending-State Authorization Filter in `getPendingLessonById` | **CLOSED / VERIFIED** |
| **BE-CONC-001** | Concurrency | P2 | SRS Review Card Lost Update Race on `CardProgress` | **CLOSED / VERIFIED** |
| **BE-CONC-002** | Concurrency | P2 | Daily Review Quota TOCTOU Race Condition | **CLOSED / VERIFIED** |
| **BE-CONC-003** | Concurrency | P2 | Concurrent Double Moderation Race on Lessons | **CLOSED / VERIFIED** |
| **BE-UPLOAD-001** | Security / Denial-of-Service | P2 | Missing Excel Row Count / Memory Resource Limit | **CLOSED / VERIFIED** |
| **BE-UPLOAD-002** | Architecture / Memory | P3 | Apache POI DOM In-Memory Workbook vs Streaming | **ACCEPTABLE TRADE-OFF / REVIEWED** |
| **BE-CORS-001** | Security / Web | P3 | CORS Configuration Missing for Cross-Origin Clients | **CLOSED / VERIFIED** |
| **BE-PERF-001** | Performance | P2 | N+1 Query in `getDueCards()` Polymorphic Hydration | **CLOSED / VERIFIED** |
| **BE-PERF-002** | Performance | P3 | N+1 Query in `getMyLessons()` Vocabulary Count | **CLOSED / VERIFIED** |
| **BE-SRS-001** | Business Logic | P3 | `LocalDate.now()` Lacks Explicit Business Timezone | **CLOSED / VERIFIED** |
| **BE-TEST-001** | Testing Quality | P2 | Missing Multi-Threaded Concurrency Test Suite | **CLOSED / VERIFIED** |
| **BE-TEST-002** | Testing Quality | P2 | Missing Transaction Rollback Verification Tests | **CLOSED / VERIFIED** |
| **BE-TEST-003** | Testing Infrastructure | P1 | No Dedicated Test Database Isolation | **CLOSED / VERIFIED** |

---

## 2. Completed Remediations

### BE-AUTH-002 — Hardcoded JWT Secret in Configuration & Class
- **Status:** **CLOSED** (Verified on 2026-08-30)
- **Finding Summary:** The production configuration `application.yml` and `JwtUtil.java` previously contained a default hardcoded signing secret fallback (`your-256-bit-secret-key-here-must-be-at-least-32-bytes-long!`), creating an insecure fallback vulnerability if `JWT_SECRET` environment variable was omitted.
- **Remediation Implemented:**
  - Removed all hardcoded default values and insecure fallbacks from production `application.yml` (`jwt.secret: ${JWT_SECRET}`) and `JwtUtil.java` (`@Value("${jwt.secret}")`).
  - Added dedicated test-only secret in `backend/src/test/resources/application.yml` to supply an automated test key without relying on environment variables.
  - Implemented fail-fast validation in `JwtUtil` initializing beans only when a non-blank, $\ge 256$-bit secret is provided.
  - Added fail-fast test coverage (`JwtUtilTests.java`) testing empty, blank, short, and missing secret configurations.
  - Searched and verified zero references to the old hardcoded secret across the codebase.
- **Verification Evidence:**
  - Full regression test suite passed with **779 tests, 0 failures, 0 errors, 0 skipped**.
  - Verified fail-fast behavior with unset `$env:JWT_SECRET = $null` and custom runtime secrets.

---

### BE-TEST-003 — No Dedicated Test Database Isolation
- **Status:** **CLOSED** (Verified on 2026-08-30)
- **Finding Summary:** All `@SpringBootTest` and `@DataJpaTest` test classes connected directly to `jdbc:mysql://localhost:3306/elearning_db`, executing queries and mutating the developer's local development database without isolation.
- **Remediation Implemented:**
  - Selected **Testcontainers + MySQL 8.4** (`mysql:8.4.0`) to provide fully isolated, disposable database instances.
  - Configured test-scoped dependencies `org.springframework.boot:spring-boot-testcontainers`, `org.testcontainers:mysql`, and `org.testcontainers:junit-jupiter`.
  - Implemented a unified singleton `ApplicationContextInitializer` ([`TestcontainersInitializer.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/test/java/com/elearning/TestcontainersInitializer.java)) configured via `context.initializer.classes` in `src/test/resources/application.yml` that starts the container once and injects dynamic datasource properties (`spring.datasource.url`, etc.).
  - Applied `--lower-case-table-names=1` to the container to ensure case-insensitive table resolution matching production MySQL schema conventions across Linux containers and Windows hosts.
  - Cleaned up redundant configurations (removed unused `@TestConfiguration` class and deleted host-level `.docker-java.properties` file in favor of programmatic API negotiation).
  - Added automated test isolation verification ([`TestDatabaseIsolationTests.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/test/java/com/elearning/TestDatabaseIsolationTests.java)).
  - Verified pure unit tests remain fast and database-free.
- **Verification Evidence:**
  - Tests use the same MySQL 8.4 engine family and the same authoritative Flyway migrations ($V1 \rightarrow V2 \rightarrow V3$) as the project baseline.
  - Verified zero connection to `localhost:3306/elearning_db` (test suite passed 100% with the local port 3306 MySQL service offline).
  - Full backend regression test suite passed with **781 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in ~1m 03s).
  - Docker Desktop (WSL 2) is now an active prerequisite for running backend integration tests.

---

### BE-UPLOAD-001 — Missing Excel Row Count / Memory Resource Limit
- **Status:** **CLOSED** (Verified on 2026-08-30)
- **Finding Summary:** `ExcelParserServiceImpl` previously iterated data rows from index 1 through `sheet.getLastRowNum()` without an explicit server-side row count ceiling. Large workbooks could trigger unbounded per-row validations, regex transformations, and database queries (`vocabularyRepository.findByHanziAndPinyinRaw`), creating an unrestricted resource consumption DoS risk under OWASP API4:2023.
- **Remediation Implemented:**
  - Added constant `public static final int MAX_DATA_ROWS = 5000;` in [`ExcelParserServiceImpl.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/java/com/elearning/service/impl/ExcelParserServiceImpl.java).
  - Implemented early fail-fast boundary guard: `if (lastRowNum > MAX_DATA_ROWS)` in `processSheet(Sheet sheet)` immediately returning `ImportValidationReport.fileLevelError("ROW_LIMIT_EXCEEDED", "File Excel vượt quá giới hạn tối đa 5000 dòng dữ liệu", ErrorCode.VALIDATION_ERROR.getCode())` before entering the row-processing loop or executing any database existence lookups.
  - Preserved existing layered upload defenses (10MB file size check, ZIP magic byte verification `{0x50, 0x4B, 0x03, 0x04}`, `.xlsx` extension validation, encrypted file handling).
  - Preserved `BE-UPLOAD-002` as an accepted trade-off (POI usermodel for lesson import scale).
  - Added comprehensive boundary tests (`ExcelParserServiceTests.java` covering exact 5,000 rows accepted, 5,001 rows rejected, extreme index 10,000 rejected without iteration) and integration workflow tests (`ExcelImportIntegrationTests.java` verifying preview zero-mutation and confirm transactional rollback).
- **Verification Evidence:**
  - Full regression test suite passed with **786 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in ~1m 13s).

---

### BE-AUTHZ-001 — Pending-State Authorization Filter in getPendingLessonById
- **Status:** **CLOSED** (Verified on 2026-08-30)
- **Finding Summary:** `ModerationServiceImpl.getPendingLessonById` previously retrieved lessons by ID without verifying that the loaded lesson's status was `"Pending"`. An authenticated Moderator or Admin could supply the ID of a `Draft`, `Approved`, or `Rejected` lesson and receive its full moderation detail and vocabulary contents, violating object-state authorization (OWASP API1:2023 — Broken Object Level Authorization).
- **Remediation Implemented:**
  - Added strict domain state validation in [`ModerationServiceImpl.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/java/com/elearning/service/impl/ModerationServiceImpl.java):
    ```java
    if (!"Pending".equalsIgnoreCase(lesson.getStatus())) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId);
    }
    ```
  - Added null check for `lessonId` returning `NOT_FOUND` consistently.
  - Guaranteed zero information disclosure: accessing non-Pending or non-existent lessons via `GET /api/v1/moderator/lessons/{id}` returns standard HTTP 404 with `NOT_FOUND` error envelope and empty payload (`data: null`), hiding the lesson title, creator, vocabulary list, and internal state.
  - Maintained consistent RBAC and endpoint semantics for both Moderator and Admin without introducing undocumented bypasses.
  - Added unit test cases (`ModerationServiceTests.java` testing `Draft`, `Approved`, `Rejected`, and `null` ID rejection) and integration tests (`ModeratorControllerTests.java`, `ModeratorIntegrationTests.java` verifying 404 status and zero response body leakage).
- **Verification Evidence:**
  - Full regression test suite passed with **795 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in ~1m 13s).

---

### BE-CONC-001 — SRS Review Card Lost Update Race on CardProgress
- **Status:** **CLOSED** (Verified on 2026-08-30)
- **Finding Summary:** `SrsServiceImpl.reviewCard()` performed a read-modify-write cycle on `CardProgress` (`findByUserAndItemTypeAndItemId` $\rightarrow$ calculate SM-2 state $\rightarrow$ `save(progress)`). Concurrent requests on the same `(user, item)` could read the same initial state, compute duplicate or conflicting transitions independently, and overwrite each other, causing silent lost updates and corrupting SM-2 learning metrics (`repetitions`, `interval_days`, `ease_factor`).
- **Remediation Implemented:**
  - Added `@Version` optimistic concurrency control column `version BIGINT UNSIGNED NOT NULL DEFAULT 0` to `CARD_PROGRESS` table via forward-only migration [`V4__add_version_to_card_progress.sql`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/resources/db/migration/V4__add_version_to_card_progress.sql).
  - Annotated `CardProgress` entity with `@Version @Column(name = "version", nullable = false) private Long version = 0L;` and corresponding getter/setter.
  - Configured centralized handling for `OptimisticLockingFailureException`, `OptimisticLockException`, and `StaleObjectStateException` in [`GlobalExceptionHandler.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/java/com/elearning/exception/GlobalExceptionHandler.java), transforming concurrency conflicts into HTTP 409 Conflict with `ErrorCode.CONFLICT` and user-friendly error envelope.
  - Added dedicated multi-threaded concurrency integration test [`SrsConcurrencyIntegrationTests.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/test/java/com/elearning/SrsConcurrencyIntegrationTests.java) using `CountDownLatch` barriers across worker threads: verified exactly 1 transaction succeeds (version $0 \rightarrow 1$), the conflicting stale transaction receives `OptimisticLockingFailureException`, and the failed transaction completely rolls back (saving exactly 1 `ReviewLog` and preserving valid SM-2 progress).
  - Added persistence tests in [`SrsProgressPersistenceTests.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/test/java/com/elearning/SrsProgressPersistenceTests.java) verifying version initialization and monotonic increments, and contract tests in [`SrsControllerTests.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/test/java/com/elearning/SrsControllerTests.java) verifying 409 mapping.
- **Verification Evidence:**
  - Focused concurrency & persistence tests passed 100% on Testcontainers MySQL 8.4.
  - Full regression test suite passed with **800 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in ~1m 15s).

---

### BE-CONC-003 — Concurrent Double Moderation Race on Lessons
- **Status:** **CLOSED** (Verified on 2026-08-30)
- **Finding Summary:** `ModerationServiceImpl.approveLesson()` and `rejectLesson()` verified that `lesson.getStatus()` was `"Pending"` before updating the status and saving a new `ModerationLog`. When two moderators submitted review decisions concurrently for the same `Pending` lesson, both transactions could load the lesson in `Pending` state before either committed, leading to race conditions where both transactions succeeded, duplicate `MODERATION_LOG` entries were recorded, or conflicting decisions (`Approved` vs `Rejected`) were applied.
- **Remediation Implemented:**
  - Added `@Version` optimistic concurrency control column `version BIGINT UNSIGNED NOT NULL DEFAULT 0` to `LESSON` table via forward-only migration [`V5__add_version_to_lesson.sql`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/resources/db/migration/V5__add_version_to_lesson.sql).
  - Annotated `Lesson` entity with `@Version @Column(name = "version", nullable = false) private Long version = 0L;` and corresponding getter/setter.
  - Reused centralized exception handling in [`GlobalExceptionHandler.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/java/com/elearning/exception/GlobalExceptionHandler.java) covering `ConcurrencyFailureException`, `OptimisticLockingFailureException`, `OptimisticLockException`, `StaleObjectStateException`, and `LockAcquisitionException`, returning HTTP 409 Conflict with standard error envelope.
  - Added dedicated multi-threaded concurrency integration tests in [`ModerationConcurrencyIntegrationTests.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/test/java/com/elearning/ModerationConcurrencyIntegrationTests.java) testing:
    1. Concurrent Approve vs Approve: exactly 1 transaction succeeds (version $0 \to 1$), the stale transaction fails with optimistic lock conflict and rolls back completely (saving exactly 1 `ModerationLog`).
    2. Concurrent Approve vs Reject: exactly 1 decision commits (final status either `Approved` or `Rejected`), the conflicting transaction rolls back, leaving zero contradictory logs in `MODERATION_LOG`.
    3. High-concurrency service calls (8 parallel threads): exactly 1 succeeds, all 7 other requests receive HTTP 409 Conflict, exactly 1 `ModerationLog` persisted.
    4. Sequential lesson lifecycle updates increment `@Version` monotonically ($0 \to 1 \to 2$).
  - Added unit contract tests in [`ModeratorControllerTests.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/test/java/com/elearning/ModeratorControllerTests.java) verifying optimistic lock conflict translates to HTTP 409 Conflict.
- **Verification Evidence:**
  - Focused moderation tests (78 tests) passed 100% on Testcontainers MySQL 8.4.
  - Full regression test suite passed with **806 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in 01:15 min on Testcontainers MySQL 8.4).

---

### BE-CONC-002 — Daily Review Quota TOCTOU Race Condition
- **Status:** **CLOSED** (Verified on 2026-08-30)
- **Finding Summary:** `getDueCards()` checked the daily review quota before selecting and returning due cards, but `reviewCard()` previously did not enforce the daily quota authoritatively prior to processing and persisting the review. Clients could bypass `getDueCards()` or submit concurrent review requests that raced around the quota decision, resulting in more completed reviews than the learner's configured `maxReviewPerDay`.
- **Note on Timezone Scope:** `BE-SRS-001` (`LocalDate.now()` lacks explicit business timezone) remains **OPEN** as an independent finding. Existing `LocalDate.now().atStartOfDay()` date semantics were intentionally preserved for this task without scope creep.
- **Remediation Implemented:**
  - Added authoritative daily review quota enforcement directly inside `@Transactional` [`SrsServiceImpl.reviewCard()`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/java/com/elearning/service/impl/SrsServiceImpl.java#L140-L240).
  - Added pessimistic write lock on the learner's `USER_PROFILE` row via [`UserProfileRepository.findByAccountEmailOrPhoneWithLock()`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/java/com/elearning/repository/UserProfileRepository.java) (`SELECT ... FOR UPDATE`), serializing concurrent review transactions for the same learner without cross-user contention.
  - Added locking read [`ReviewLogRepository.findTodayLogIdsWithLock()`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/java/com/elearning/repository/ReviewLogRepository.java) (`SELECT r.logId FROM ReviewLog r ... FOR UPDATE`) to bypass MySQL `REPEATABLE READ` MVCC snapshot isolation and evaluate the authoritative latest committed review count.
  - If `todayReviews >= maxReviewPerDay`, throws `BusinessException(ErrorCode.CONFLICT, "Bạn đã đạt giới hạn ôn tập tối đa trong ngày (N thẻ)")` (HTTP 409 Conflict), rolling back the transaction completely with zero `ReviewLog` or `CardProgress` side effects.
  - Added multi-threaded concurrency integration tests in [`SrsConcurrencyIntegrationTests.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/test/java/com/elearning/SrsConcurrencyIntegrationTests.java) verifying:
    1. Near-limit concurrency (limit = 3, existing = 2, two concurrent threads on different cards): exactly 1 succeeds, 1 fails with HTTP 409 Conflict, final review count is strictly 3 (never 4), losing request has 0 side effects.
    2. At-limit sequential calls: immediate rejection with HTTP 409 Conflict and 0 side effects.
    3. User isolation: User A exhausting quota does not block or impact User B's ability to review.
- **Verification Evidence:**
  - Focused SRS tests (73 tests) passed 100% on Testcontainers MySQL 8.4.
  - Repeated execution of `SrsConcurrencyIntegrationTests` passed consistently.
  - Full regression test suite passed with **810 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in 01:13 min on Testcontainers MySQL 8.4).

---

### BE-PERF-001 — Eliminate N+1 Queries in getDueCards() Polymorphic Hydration
- **Status:** **CLOSED** (Verified on 2026-08-30)
- **Finding Summary:** `SrsServiceImpl.getDueCards()` retrieved `CardProgress` rows and then iterated through each card in a loop, invoking `vocabularyRepository.findById()` or `radicalRepository.findById()` individually. For $N$ due cards, this resulted in $1 + N$ queries (1 query for `CardProgress` + $N$ individual item lookups), which scaled linearly with $N$.
- **Remediation Implemented:**
  - Refactored polymorphic content hydration in [`SrsServiceImpl.getDueCards()`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/java/com/elearning/service/impl/SrsServiceImpl.java#L125-L165) to use targeted bulk lookups:
    1. Collects unique `vocabIds` (`Set<Long>`) and `radicalIds` (`Set<Integer>`) across the retrieved due `CardProgress` list.
    2. Performs at most 1 bulk query for `VOCABULARY` (`vocabularyRepository.findAllById(vocabIds)`) using SQL `WHERE vocab_id IN (...)`.
    3. Performs at most 1 bulk query for `RADICAL` (`radicalRepository.findAllById(radicalIds)`) using SQL `WHERE radical_id IN (...)`.
    4. Builds fast in-memory lookup maps (`Map<Long, Vocabulary>` and `Map<Integer, Radical>`).
    5. Reconstructs `DueCardResponse` in the exact original chronological order of `dueProgressList`, preserving missing-item skipping semantics.
  - Replaced $O(N)$ item lookup round-trips with $O(1)$ bounded queries ($\le 2$ bulk item queries regardless of whether $N = 5, 10, 20, 50$).
  - Added dedicated query-count and performance integration tests in [`SrsPerformanceIntegrationTests.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/test/java/com/elearning/SrsPerformanceIntegrationTests.java) using Hibernate `Statistics.getPrepareStatementCount()` to verify query bounds on real MySQL 8.4 Testcontainers:
    1. Mixed polymorphic due cards ($N = 10$, 6 Vocabulary + 4 Radicals): total queries $\le 6$ (exactly 2 item queries, NOT $10$ item queries).
    2. Sublinear scale invariance ($N = 5$ vs $N = 15$ due cards): executed queries remain constant and bounded ($= 6$ total).
    3. Vocabulary-only due cards ($N = 5$): exactly 1 bulk query for vocabulary, 0 for radicals.
    4. Chronological order preservation across interleaved item types.
- **Verification Evidence:**
  - Focused SRS unit & integration tests (79 tests) passed 100% on Testcontainers MySQL 8.4.
  - Full regression test suite passed with **816 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in 01:14 min on Testcontainers MySQL 8.4).

---

### BE-SRS-001 — Make Business Timezone Explicit for SRS Date/Time Calculations
- **Status:** **CLOSED / VERIFIED** (Verified on 2026-08-30)
- **Finding Summary:** SRS date calculations in `SrsServiceImpl` previously invoked `LocalDate.now().atStartOfDay()` and `LocalDateTime.now()` without specifying an explicit business timezone. In Java, these methods rely on `ZoneId.systemDefault()`, making business-day boundaries (daily review quota reset at midnight) dependent on host/JVM OS timezones, risking inconsistent quota behavior across developer machines, CI, Docker containers, and production servers.
- **Remediation Implemented:**
  - Established authoritative business timezone `Asia/Ho_Chi_Minh` (UTC+7) in [`TimeConfig.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/java/com/elearning/config/TimeConfig.java) configured via property `app.business-timezone: ${APP_BUSINESS_TIMEZONE:Asia/Ho_Chi_Minh}`.
  - Registered a Spring-managed `Clock` bean (`Clock.system(ZoneId.of(businessTimezone))`) with strict validation (fails fast on invalid timezone during startup, NO silent fallback to system default).
  - Injected `Clock businessClock` into [`SrsServiceImpl.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/java/com/elearning/service/impl/SrsServiceImpl.java) and replaced all SRS business date/time calculations:
    1. `LocalDate.now(businessClock).atStartOfDay()` for calculating today's quota start boundary in `getDueCards()`, `reviewCard()`, and `getStudyStats()`.
    2. `LocalDateTime.now(businessClock)` for `nextReviewAt` calculations and setting `ReviewLog.reviewedAt`.
  - Added dedicated boundary and environment-independence integration tests in [`SrsTimezoneIntegrationTests.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/test/java/com/elearning/SrsTimezoneIntegrationTests.java):
    1. `testSpringInjectedClock_usesBusinessTimezone`: Proves injected `Clock` zone is `Asia/Ho_Chi_Minh`.
    2. `testTimeConfig_failsFastOnInvalidZone`: Proves invalid timezone strings throw `ZoneRulesException` / `IllegalArgumentException`.
    3. `testBusinessDayBoundary_midnightQuotaReset`: Fixed-clock simulation proving review at 23:59:50 belongs to Day 1, crossing midnight to 00:00:10 resets quota on Day 2, and subsequent review succeeds.
    4. `testBusinessDateEvaluation_independentOfSystemTimezone`: Proves business date evaluation is strictly based on `Asia/Ho_Chi_Minh` even when JVM/UTC timestamps differ across date boundaries.
- **Verification Evidence:**
  - Focused SRS tests (83 tests) passed 100% on Testcontainers MySQL 8.4.
  - Full regression test suite passed with **820 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in 01:13 min on Testcontainers MySQL 8.4).

### BE-CORS-001 — Evaluate and Configure CORS for Cross-Origin Clients
- **Status:** **CLOSED / VERIFIED** (Verified on 2026-08-30)
- **Finding Summary:** The REST API previously lacked CORS configuration in `SecurityConfig.java` and had no `CorsConfigurationSource` bean. Because the frontend (Phase 9) is maintained in a standalone `frontend/` directory and served via static web servers / Live Server during development (`http://localhost:5500`, `http://127.0.0.1:5500`, `http://localhost:3000`, `http://127.0.0.1:3000`) or dedicated web servers in production, all browser-based `fetch()` requests across different origins would be blocked by the browser's Same-Origin Policy.
- **Architectural & Security Verification:**
  - Evaluated the deployment architecture: The backend is a pure stateless REST API (`http://localhost:8080`) using JWT in `Authorization: Bearer <token>` headers without cookies/sessions (`SessionCreationPolicy.STATELESS`).
  - Verified CORS requirements: Cross-origin browser access is **genuinely required** for development and decoupled production frontend hosting.
  - Implemented the smallest, strictest secure CORS policy:
    1. **Centralized Configuration:** Configured in `SecurityConfig.java` via `http.cors(Customizer.withDefaults())` and registered `@Bean CorsConfigurationSource`.
    2. **Explicit Allowlist (Zero Wildcards):** Origins configured via `app.cors.allowed-origins: ${APP_CORS_ALLOWED_ORIGINS:http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000,http://127.0.0.1:3000}`. Wildcard `*` is strictly forbidden.
    3. **Method & Header Allowlist:** Limited to `GET, POST, PUT, PATCH, DELETE, OPTIONS` and headers `Authorization, Content-Type, Accept, Origin, X-Requested-With`.
    4. **Path Scope:** Scoped exclusively to `/api/**`.
    5. **Preflight & Cache:** Bounded preflight cache `maxAge = 3600L` (1 hour). `allowCredentials = false` (no cookies).
    6. **Spring Security Preflight Ordering:** `CorsFilter` executes at the beginning of the filter chain, ensuring valid preflight `OPTIONS` requests receive `200 OK` without triggering false 401/403 rejection from `JwtAuthenticationFilter` or authentication entry points.
    7. **Auth & RBAC Preservation:** Verified that allowed CORS origins do not weaken authentication or RBAC (unauthenticated requests still return 401, unauthorized roles still return 403).
- **Remediation Implemented:**
  - Updated [`SecurityConfig.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/java/com/elearning/config/SecurityConfig.java) with `.cors(Customizer.withDefaults())` and `@Bean public CorsConfigurationSource corsConfigurationSource(...)`.
  - Configured `app.cors` properties in production [`application.yml`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/main/resources/application.yml) and test [`application.yml`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/test/resources/application.yml).
  - Created [`CorsSecurityIntegrationTests.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/test/java/com/elearning/CorsSecurityIntegrationTests.java) with 14 automated integration tests verifying allowed origins, disallowed origin 403 rejection, preflight handling, method/header filtering, auth/RBAC independence, and same-origin preservation.
- **Verification Evidence:**
  - `CorsSecurityIntegrationTests` passed 14/14 tests on Testcontainers MySQL 8.4.
  - Security test suite (`CorsSecurityIntegrationTests`, `SecurityConfigTests`, `RbacSecurityIntegrationTests`, `JwtAuthenticationFilterTests`) passed 53/53 tests.
  - Full regression test suite passed with **834 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in 01:15 min on Testcontainers MySQL 8.4).

### BE-AUTH-001 — Reject Stale JWT After Account Disable
- **Status:** **CLOSED / VERIFIED** (Verified on 2026-08-30)
- **Finding Summary:** The REST API previously performed purely cryptographic validation on incoming JWTs (`jwtUtil.validateToken(token)`). Once a JWT was issued, if an administrator disabled, locked, banned, or deactivated the account in the database (`status = 'Inactive'` or `'Banned'`), the previously issued JWT remained accepted until token expiration because the filter never verified the current server-side account status.
- **Root Cause & Security Property:**
  - *Vulnerability Flow:* `User authenticates -> JWT issued (1h expiry) -> Account is disabled/banned in DB -> Attacker/former user presents unexpired JWT -> Cryptographic signature & expiry valid -> SecurityContext populated -> Protected endpoints accepted.`
  - *Enforced Security Property:* An issued JWT is necessary but NOT sufficient for authentication. Authentication requires BOTH: (1) Valid cryptographic signature & unexpired claims, AND (2) Current server-side account status in the authoritative database is `Active`.
- **Remediation Implemented:**
  - **Injected `AccountRepository` into `JwtAuthenticationFilter`:**
    - Kept fail-closed cryptographic validation FIRST: if token is malformed, invalid signature, or expired, reject immediately without querying the database (preventing DoS).
    - If token is cryptographically valid, extracted `subject` (`emailOrPhone`) and queried `accountRepository.findStatusByEmailOrPhone(subject)`.
    - Verification check: `if (statusOpt.isPresent() && "Active".equalsIgnoreCase(statusOpt.get()))`.
    - Only populated `SecurityContextHolder` if the account exists and status is `Active`.
    - If status is not `Active` (e.g. `'Inactive'`, `'Banned'`) or account is missing: `SecurityContextHolder` remains unauthenticated. Downstream protected endpoints trigger Spring Security's `AuthenticationEntryPoint`, returning a standard generic `HTTP 401 UNAUTHORIZED` envelope (`{"code":"UNAUTHORIZED","message":"Chưa xác thực hoặc phiên đăng nhập đã hết hạn"}`).
  - **Lightweight DB Query Projection:** Added `@Query("SELECT a.status FROM Account a WHERE a.emailOrPhone = :emailOrPhone") Optional<String> findStatusByEmailOrPhone(String emailOrPhone)` in `AccountRepository.java`. Uses the unique indexed `email_or_phone` column and selects only the single `status` column (zero entity joins).
  - **Comprehensive Verification Suite:**
    - Updated unit tests in `JwtAuthenticationFilterTests.java` mocking `AccountRepository` (tested Active, Inactive/Disabled, Banned, Nonexistent, and Invalid token without DB lookup).
    - Created dedicated integration test suite `StaleTokenSecurityIntegrationTests.java` (7 automated integration tests):
      1. Valid JWT before disable -> 200 OK.
      2. Exact same unexpired JWT after disable (`account.setStatus("Inactive")`) -> 401 UNAUTHORIZED.
      3. Exact same unexpired JWT after re-enabling (`account.setStatus("Active")`) -> 200 OK (stateful authoritative check).
      4. Disabled Moderator accessing moderator queue -> 401 UNAUTHORIZED (proves authentication fails before RBAC).
      5. Disabled Creator accessing creator lessons -> 401 UNAUTHORIZED.
      6. Deleted/missing account with validly signed JWT -> 401 UNAUTHORIZED.
      7. Disabling Account A has zero effect on Account B (account isolation).
- **Verification Evidence:**
  - `StaleTokenSecurityIntegrationTests` passed 7/7 tests on Testcontainers MySQL 8.4.
  - `JwtAuthenticationFilterTests` passed 9/9 tests.
  - Full regression test suite passed with **844 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in 01:12 min on Testcontainers MySQL 8.4).

---

### BE-AUTH-003 — Missing JWT Issuer (iss) Claim
- **Status:** **CLOSED / VERIFIED** (Verified on 2026-08-30)
- **Finding Summary:** The previous JWT implementation generated tokens without an `iss` (Issuer) claim and validated tokens without checking `iss`. A token signed with the application's secret key but generated for an unexpected environment or issuer would be accepted.
- **Root Cause & Security Property:**
  - *Intended Security Property:* Access JWTs accepted IF AND ONLY IF: valid signature + unexpired + required identity claims (`sub`, `roles`) + `iss == configured expected issuer` + active account status in MySQL.
  - RFC 7519 defines `iss` as identifying the principal that issued the JWT. The project decision (`DEC-20`) makes `iss` mandatory and validated.
- **Remediation Implemented:**
  - **Authoritative Configuration:**
    - Added `jwt.issuer: ${JWT_ISSUER:elearning-backend}` in `backend/src/main/resources/application.yml` and `backend/src/test/resources/application.yml`.
  - **Token Generation & Validation in `JwtUtil.java`:**
    - Set `.issuer(this.issuer)` during token creation in `generateToken(...)`.
    - Enforced `.requireIssuer(this.issuer)` in the built-in JJWT 0.12.6 `JwtParser` (`Jwts.parser().requireIssuer(this.issuer).verifyWith(this.signingKey).build()`).
    - Tokens with missing `iss` throw `MissingClaimException` (a `JwtException`).
    - Tokens with mismatched `iss` throw `IncorrectClaimException` (a `JwtException`).
    - `validateToken(token)` catches `JwtException` and returns `false`, preventing unauthenticated tokens from reaching the database lookup or RBAC layers.
    - Added fail-fast validation in `JwtUtil` constructor rejecting null or blank issuer values.
  - **Security Filter Integration:**
    - `JwtAuthenticationFilter` calls `jwtUtil.validateToken(token)` first. If issuer validation fails, `SecurityContextHolder` remains empty, triggering Spring Security's generic `HTTP 401 UNAUTHORIZED` response.
  - **Comprehensive Verification:**
    - Unit tests in `JwtUtilTests.java` (20 tests) covering correct issuer acceptance, wrong issuer rejection, missing issuer rejection, cross-environment issuer rejection, and fail-fast startup checks.
    - Unit tests in `JwtAuthenticationFilterTests.java` (11 tests) verifying filter behavior on missing/wrong issuer.
    - Integration tests in `JwtIssuerSecurityIntegrationTests.java` (7 tests) verifying end-to-end authentication, RBAC precedence (401 before 403), disabled account rejection (BE-AUTH-001 preserved), and public endpoint accessibility.
- **Verification Evidence:**
  - `JwtUtilTests` passed 20/20 tests.
  - `JwtIssuerSecurityIntegrationTests` passed 7/7 tests on Testcontainers MySQL 8.4.
  - Full regression test suite passed with **858 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in 01:18 min on Testcontainers MySQL 8.4).

---

### BE-PERF-002 — N+1 Query in getMyLessons() Vocabulary Count
- **Status:** **CLOSED / VERIFIED** (Verified on 2026-08-30)
- **Finding Summary:** In `CreatorLessonServiceImpl.getMyLessons(Pageable)`, after retrieving a page of $N$ lessons, a per-lesson loop executed `lessonVocabularyRepository.countByLesson_LessonId(lesson.getLessonId())` individually for each lesson. For a page size of 20, this produced 1 + 20 = 21 database queries ($O(N)$ query amplification).
- **Root Cause:** Calculating aggregates inside a Java mapping stream without bulk fetching or SQL group-by aggregation.
- **Remediation Implemented:**
  - **Bulk Aggregation Query in `LessonVocabularyRepository.java`:**
    ```java
    @Query("SELECT lv.lesson.lessonId, COUNT(lv) FROM LessonVocabulary lv WHERE lv.lesson.lessonId IN :lessonIds GROUP BY lv.lesson.lessonId")
    List<Object[]> countVocabulariesByLessonIds(@Param("lessonIds") Collection<Long> lessonIds);
    ```
    - Leverages the composite primary key index `PRIMARY KEY (lesson_id, vocab_id)` on `LESSON_VOCABULARY` for high-performance filtering and grouping.
    - Requires **no schema changes and no Flyway migrations**.
  - **In-Memory Count Hydration in `CreatorLessonServiceImpl.java`:**
    - Extracts `lessonIds` from the paginated result (`lessonPage.getContent()`).
    - If empty, returns immediately with 0 count queries.
    - Executes a single bulk aggregate query for all lesson IDs on the current page ($O(1)$ query complexity).
    - Constructs `Map<Long, Integer> countMap` and looks up counts with `countMap.getOrDefault(lesson.getLessonId(), 0)`.
    - Preserves exact DTO contract (`LessonSummaryResponse`), entity ordering, pagination metadata, and creator ownership isolation.
  - **Comprehensive Unit & Query-Count Integration Tests:**
    - Updated `CreatorLessonServiceTests.java` (32 tests) verifying bulk aggregation invocation, ordering preservation, mixed counts (e.g. 3, 0, 5), empty page handling, and verifying `countByLesson_LessonId` is never called.
    - Created `LessonPerformanceIntegrationTests.java` (6 tests) measuring query execution on Testcontainers MySQL 8.4 via Hibernate Statistics:
      1. $N = 10$ lessons: bounded query count $\le 4$ queries (was 13+ in legacy implementation).
      2. Scaling invariance: query count for 15 lessons equals query count for 5 lessons ($Q_{15} == Q_5 = 4$).
      3. Zero-count lesson defensive handling: lessons without vocabularies return `vocabularyCount = 0`.
      4. Creator ownership and count isolation: Creator A only sees and counts Creator A's lessons.
      5. Pagination metadata preservation across multiple pages.
      6. Empty lesson list executes 0 count aggregation queries.
- **Verification Evidence:**
  - `CreatorLessonServiceTests` passed 32/32 tests.
  - `LessonPerformanceIntegrationTests` passed 6/6 tests on Testcontainers MySQL 8.4.
  - Full regression test suite passed with **866 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in 01:24 min on Testcontainers MySQL 8.4).

---

### BE-TEST-001 — Strengthen Concurrency Integration Test Coverage for Critical State Transitions
- **Status:** **CLOSED / VERIFIED** (Verified on 2026-08-30)
- **Finding Summary:** Audit noted lack of multithreaded test patterns (`CountDownLatch`, `ExecutorService`) to verify critical state transitions (SRS double-review race, double-moderation, concurrent quota bypass, creator lifecycle races, and registration races).
- **Remediation Implemented:**
  - Established and reinforced 4 dedicated multi-threaded concurrency integration test suites on Testcontainers MySQL 8.4 using real transaction boundaries (`TransactionTemplate` / separate worker threads) and deterministic synchronization (`CountDownLatch`, `startLatch`/`readyLatch` barriers):
    1. **`ModerationConcurrencyIntegrationTests.java` (4 tests):**
       - Interleaved concurrent approve transactions: verifies `@Version` optimistic locking rejects stale approve transaction, rolls back `ModerationLog`, preserving exactly 1 approval log.
       - Interleaved mixed approve and reject transactions: verifies conflicting moderator decisions are rejected by optimistic locking without contradictory logs.
       - High-concurrency 8-thread service calls: exactly 1 succeeds, 7 fail with 409 Conflict.
       - Sequential monotonic `@Version` progression (0 -> 1 -> 2).
    2. **`CreatorLessonConcurrencyIntegrationTests.java` (4 tests):**
       - Concurrent `submitForModeration()`: 2 concurrent submissions on Draft lesson; exactly 1 succeeds (status -> `Pending`, version 0 -> 1), 1 receives 409 Conflict (`OptimisticLockingFailureException`).
       - Interleaved `updateMyLesson()` vs `submitForModeration()`: Tx1 updates title (version 0 -> 1), Tx2 fails with `OptimisticLockingFailureException` and rolls back; title preserved, status remains `Draft`.
       - Concurrent `deleteMyLesson()` vs `submitForModeration()`: prevents dangling / corrupted states.
       - Monotonic `@Version` progression across creator lifecycle.
    3. **`SrsConcurrencyIntegrationTests.java` (7 tests):**
       - Interleaved concurrent review transactions: deterministic `TransactionTemplate` overlap proving `@Version` rejects stale review and rolls back its `ReviewLog`.
       - Concurrent first-time review of new unreviewed cards: pessimistic lock and MySQL unique constraint `uk_card_progress_user_item` ensure exactly 1 `CardProgress` row is created without duplicate entries.
       - Concurrent review near daily quota limit: pessimistic write lock (`findTodayLogIdsWithLock`) enforces strict quota atomicity (zero limit overshoot).
       - User isolation: User A quota exhaustion does not block User B.
    4. **`AuthConcurrencyIntegrationTests.java` (2 tests):**
       - Concurrent registration with duplicate email: MySQL unique constraint `uk_account_email_or_phone` serializes registration; exactly 1 succeeds (JWT issued), 1 fails with 409 Conflict, failed registration rolls back completely with zero orphaned `UserProfile` or role records.
       - Concurrent registration with distinct emails: both succeed independently.
- **Verification Evidence:**
  - Focused concurrency test suites (`CreatorLessonConcurrencyIntegrationTests`, `AuthConcurrencyIntegrationTests`, `SrsConcurrencyIntegrationTests`, `ModerationConcurrencyIntegrationTests`) passed 17/17 tests on Testcontainers MySQL 8.4.
  - Full regression test suite passed with **874 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in 01:28 min on Testcontainers MySQL 8.4).

---

---

### BE-TEST-002 — Transaction Rollback Tests for Critical Multi-Step Operations
- **Status:** **CLOSED / VERIFIED** (Verified on 2026-08-30)
- **Finding Summary:** Audit noted lack of tests explicitly verifying partial-failure transactional rollback scenarios across critical multi-step operations (Excel import partial failure, moderation log failure, SRS review log failure, account registration failure, creator lesson delete/reorder failure).
- **Remediation Implemented:**
  - Created master transaction rollback verification suite [`TransactionRollbackIntegrationTests.java`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/backend/src/test/java/com/elearning/TransactionRollbackIntegrationTests.java) (7 tests) on Testcontainers MySQL 8.4 using real Spring-managed declarative transaction boundaries (`@Transactional` on service proxies) without test-managed transaction masking (`@Transactional(propagation = Propagation.NOT_SUPPORTED)`):
    1. **Excel Lesson Import Confirmation Rollback (`testExcelImportConfirmRollback_midStreamFailure_restoresDatabaseCleanly`):**
       - Mid-stream DB I/O failure injected on saving the 2nd `LessonVocabulary` item.
       - Confirms complete atomic rollback: 0 `Lesson` rows, 0 new `Vocabulary` rows, 0 `LessonVocabulary` rows remain in MySQL (exact baseline restored).
    2. **Lesson Moderation Approve Action Rollback (`testModerationApproveRollback_auditLogFailure_restoresPendingState`):**
       - DB failure injected on saving `ModerationLog`.
       - Confirms complete rollback: `Lesson` status remains `Pending`, `version` remains `0L`, 0 `ModerationLog` rows persisted.
    3. **Lesson Moderation Reject Action Rollback (`testModerationRejectRollback_auditLogFailure_restoresPendingState`):**
       - DB failure injected on saving `ModerationLog` for reject.
       - Confirms complete rollback: `Lesson` status remains `Pending`, `version` remains `0L`, 0 `ModerationLog` rows persisted.
    4. **SRS Card Review Mutation Rollback (`testSrsReviewRollback_reviewLogFailure_restoresCardProgressState`):**
       - DB failure injected on saving `ReviewLog` entry.
       - Confirms complete rollback: `CardProgress` state (`intervalDays`, `repetitions`, `easeFactor`, `version`) reverts 100% to pre-review state; 0 new `ReviewLog` rows persisted.
    5. **Account Registration Atomic Rollback (`testAuthRegistrationRollback_tokenGenerationFailure_restoresDatabaseCleanly`):**
       - Failure injected during JWT generation after entity saves.
       - Confirms complete rollback: `Account`, `UserProfile`, and `AccountRole` records rolled back atomically; zero orphan records in database.
    6. **Creator Lesson Deletion Cascade Rollback (`testCreatorLessonDeleteRollback_lessonDeleteFailure_restoresLessonVocabularyLinks`):**
       - DB failure injected when deleting `Lesson` record after deleting `LessonVocabulary` associations.
       - Confirms complete rollback: `Lesson` remains present, all associated `LessonVocabulary` links are restored with original `orderIndex` (1, 2).
    7. **Creator Lesson Vocabulary Reordering Rollback (`testCreatorLessonReorderRollback_phase2Failure_restoresOriginalOrderSequence`):**
       - DB failure injected during `saveAll` in reorder operation.
       - Confirms complete rollback: all `LessonVocabulary` `orderIndex` values revert to original sequence [1, 2, 3].
- **Verification Evidence:**
  - `TransactionRollbackIntegrationTests` passed 7/7 tests on Testcontainers MySQL 8.4.
  - Related integration tests (58 tests across `LessonLifecycleIntegrationTests`, `ModerationServiceIntegrationTests`, `SrsServiceIntegrationTests`, `SrsPolymorphicIntegrityTests`, `AuthIntegrationTests`) passed 58/58 tests.
  - Full clean regression test suite passed with **881 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS` in 01:25 min on Testcontainers MySQL 8.4).

---

## 3. Working Remediation Order & Status

**ALL 15 BACKEND HARDENING AUDIT FINDINGS ARE NOW 100% CLOSED AND VERIFIED.**

| Finding ID | Scope | Severity | Status | Verified Baseline |
| :--- | :--- | :--- | :--- | :--- |
| `BE-AUTH-002` | Remove Hardcoded JWT Secret | P1 | **CLOSED / VERIFIED** | 779 tests |
| `BE-TEST-003` | Test Database Isolation (MySQL 8.4 Testcontainers) | P1 | **CLOSED / VERIFIED** | 781 tests |
| `BE-UPLOAD-001` | Excel Import Row Count Limit (5000 rows) | P2 | **CLOSED / VERIFIED** | 786 tests |
| `BE-AUTHZ-001` | Pending-State Authorization Filter | P2 | **CLOSED / VERIFIED** | 795 tests |
| `BE-CONC-001` | CardProgress Optimistic Locking (`@Version` + V4) | P2 | **CLOSED / VERIFIED** | 800 tests |
| `BE-CONC-003` | Lesson Moderation Optimistic Locking (`@Version` + V5) | P2 | **CLOSED / VERIFIED** | 806 tests |
| `BE-CONC-002` | SRS Daily Review Quota TOCTOU Pessimistic Locking | P2 | **CLOSED / VERIFIED** | 810 tests |
| `BE-PERF-001` | Polymorphic Due Cards Bulk Hydration (N+1 eliminated) | P2 | **CLOSED / VERIFIED** | 816 tests |
| `BE-SRS-001` | Explicit Business Timezone (`Asia/Ho_Chi_Minh`) | P3 | **CLOSED / VERIFIED** | 820 tests |
| `BE-CORS-001` | Explicit Centralized CORS Policy | P2 | **CLOSED / VERIFIED** | 834 tests |
| `BE-AUTH-001` | Stale JWT Account Disable Authoritative DB Status Check | P1 | **CLOSED / VERIFIED** | 844 tests |
| `BE-AUTH-003` | Require and Validate JWT Issuer (`iss`) Claim | P3 | **CLOSED / VERIFIED** | 858 tests |
| `BE-PERF-002` | Bulk Aggregation for `getMyLessons()` Vocabulary Count | P3 | **CLOSED / VERIFIED** | 866 tests |
| `BE-TEST-001` | Concurrency Integration Test Coverage for State Transitions | P2 | **CLOSED / VERIFIED** | 874 tests |
| `BE-TEST-002` | Master Transaction Rollback & Atomicity Integration Tests | P2 | **CLOSED / VERIFIED** | **881 tests** |

---

## 4. Important Guidance for Future AI Agents

---

## 4. Important Guidance for Future AI Agents

### Audit Interpretation
- `.agents/AUDIT_REPORT.md` represents external audit evidence and suggestions, **NOT** authoritative implementation specifications.
- Future agents must independently verify each finding against:
  1. Current repository code and tests
  2. `.agents/` architecture and decisions (`ARCHITECTURE.md`, `DECISIONS.md`, `DATABASE.md`)
  3. Authoritative official technical and security documentation (Spring Security, OWASP, MySQL Reference)
  4. Audit report evidence

### Completed Findings
- **DO NOT REIMPLEMENT** `BE-AUTH-002` or `BE-TEST-003` unless concrete repository evidence proves a regression.

### Execution Scope
- Work strictly on **one finding per task/chat session**.
- Lifecycle workflow for every remediation:
  $$\text{Verify finding} \longrightarrow \text{Consult official sources} \longrightarrow \text{Choose solution} \longrightarrow \text{Implement} \longrightarrow \text{Regression test} \longrightarrow \text{Update status} \longrightarrow \text{Stop}$$

### Backend Hardening Gate
- Backend Hardening Gate (15/15 findings), Module 8D (Tasks 8D.1..8D.8), Tasks R1 → R3.11, and Final Adversarial Remediation (DEC-42) are **`COMPLETED`** and verified on MySQL 8.4 Testcontainers (**1101/1101 tests PASS**). Backend Final Quality Gate cleared and release sealed (DEC-41, DEC-42).

---

## 5. BACKEND HARDENING TASKS (R3.4 → R3.11) [COMPLETED]

Các nhiệm vụ kỹ thuật Backend Hardening đã hoàn thành toàn bộ và được niêm phong trước khi chuyển giao sang Phase 9 (Frontend):

| Task ID | Task Name | Status | Priority | Purpose & Residual Item Addressed |
| :--- | :--- | :---: | :---: | :--- |
| **Task R3.4** | Object-Level Authorization & API Boundary Audit | **`COMPLETED`** | **`P0`** | Kiểm toán toàn diện phân quyền cấp đối tượng (IDOR) và phân quyền vai trò trên tất cả các Controller endpoints (`CreatorLessonController`, `PersonalNoteController`, `ModeratorController`, `AdminController`, `SrsController`). 45 endpoints audited, zero issues found. |
| **Task R3.5** | Personal Notes Pagination & Resource Consumption Hardening | **`COMPLETED`** | **`P1`** | Bổ sung phân trang (`Pageable`, default=20, max=100) cho endpoint `GET /api/v1/vocabularies/{vocabId}/notes` (Giải quyết `RES-02` / OWASP API4:2023). |
| **Task R3.6** | Login Rate Limiter TTL Cache Eviction & Memory Lifecycle | **`COMPLETED`** | **`P1`** | Tích hợp cơ chế tự động giải phóng key IP rác theo thời gian (TTL cache eviction) trong `LoginRateLimiter` chống tích tụ bộ nhớ (Giải quyết `RES-03`). |
| **Task R3.7** | Dependency & Framework Security Hardening (Apache POI 5.4.0 / CVE-2025-31672) | **`COMPLETED`** | **`P2`** | Nâng cấp Apache POI `poi-ooxml` lên `5.4.0` vá lỗ hổng OOXML duplicate ZIP entry validation (Giải quyết `RES-01` / CVE-2025-31672), giữ nguyên Spring Boot 3.3.5 tương thích 100% `commons-lang3 3.14.0`. |
| **Task R3.8** | Kangxi Radicals Missing Pinyin Correction & Seed Data Ingestion | **`COMPLETED`** | **`P2`** | Khắc phục 2 bản ghi thiếu Pinyin trong seed gốc (ID 49 `jǐ` và ID 172 `zhuī`) qua Flyway migration `V7`, bảo toàn 100% 214 radicals và empty `meaning_vi`. |
| **Task R3.9** | Database Index Performance & Slow Query Verification (`EXPLAIN`) | **`COMPLETED`** | **`P2`** | Kiểm tra execution plan qua `EXPLAIN` và `EXPLAIN ANALYZE TREE` trên các truy vấn trọng yếu (`idx_card_progress_due`, `idx_vocab_pinyin_raw`, `idx_lesson_status`), xác minh 0 filesort và covering index scans; verdict: `INDEXES VERIFIED, NO CHANGE REQUIRED`. |
| **Task R3.10** | Production HTTP & Reverse Proxy Security Headers | **`COMPLETED`** | **`P3`** | Cấu hình `server.forward-headers-strategy: framework` cho `X-Forwarded-For` và production security headers (HSTS, nosniff, DENY, Referrer-Policy) (Giải quyết `RES-04`); CSP hoãn lại sang Phase 9. |
| **Task R3.11** | Backend Final Quality Gate & Pre-Frontend Release Seal | **`COMPLETED`** | **`P0 (Gate)`** | Toàn bộ 1088/1088 tests PASS, V1..V7 Flyway clean, 45 REST endpoints verified, Backend Release Seal active (DEC-41). Phase 9 Frontend unblocked. |

### Sơ đồ Thứ tự Chuyển giao:
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
Task 9A.1 — Frontend Core Shell, Shared Layout & API Client Architecture [NEXT IMMEDIATE TASK]
```

---

## TASK R3.7 — DEPENDENCY & FRAMEWORK SECURITY HARDENING: APACHE POI CVE-2025-31672 AUDIT & REMEDIATION

### 1. Bối cảnh & Khám phá Lỗ hổng
- **CVE:** CVE-2025-31672 (Apache Bugzilla Bug 69620, công bố ngày 8/4/2025).
- **Cơ chế:** Lỗ hổng xử lý file OOXML nén ZIP chứa các phần tử (ZIP entries) trùng lặp tên (duplicate entry names). Các phiên bản Apache POI trước 5.4.0 không kiểm tra tính duy nhất của tên entry, dẫn đến parser divergence, khả năng bypass kiểm tra nội dung, hoặc tiêu hao tài nguyên xử lý không kiểm soát.
- **Phiên bản baseline cũ:** `org.apache.poi:poi-ooxml:5.3.0` (Bị ảnh hưởng trực tiếp bởi CVE-2025-31672).
- **Phiên bản vá chính thức:** Apache POI từ **`5.4.0`** trở lên đã bổ sung cơ chế kiểm tra `InvalidFormatException: Input file contains more than 1 entry with the name ...`.

### 2. Phân tích Xung đột Đồ thị Phụ thuộc & Quyết định Chọn Phiên bản
1. **Thử nghiệm POI 5.5.1:**  
   - Kéo theo transitive dependency `commons-compress:1.28.0`.
   - `TarArchiveEntry` trong `commons-compress:1.28.0` gọi phương thức `SystemProperties.getUserName(String)` chỉ có từ `commons-lang3 >= 3.15.0`.
   - Tuy nhiên, Spring Boot 3.3.5 quản lý `commons-lang3:3.14.0`.
   - Gây lỗi nghiêm trọng `java.lang.NoSuchMethodError: 'java.lang.String org.apache.commons.lang3.SystemProperties.getUserName(java.lang.String)'` tại runtime trong Testcontainers Docker execution.
   - Việc nâng đè `commons-lang3` lên 3.15.0 vi phạm nguyên tắc không nâng Spring Boot BOM và gây rủi ro phá vỡ tương thích hệ thống.
2. **Lựa chọn POI 5.4.0 (Authoritative Patch):**  
   - Phiên bản vá chính thức đầu tiên của Apache cho CVE-2025-31672.
   - Kéo theo `commons-compress:1.27.1`.
   - Tương thích nhị phân 100% với `commons-lang3:3.14.0` của Spring Boot 3.3.5.
   - Zero POM dependency overrides, zero compile/runtime errors.

### 3. Thay đổi Mã nguồn & Bộ Kiểm thử Bảo mật Đối kháng
- **`backend/pom.xml`:** Cập nhật `<version>5.4.0</version>` cho `org.apache.poi:poi-ooxml`.
- **`ExcelParserServiceTests.java`:** Bổ sung `@Nested class CveAndHardeningSecurityTests` với 4 kịch bản kiểm thử:
  1. `testDuplicateZipEntries_sheetXml_rejectedSafely`: Sinh file XLSX chứa 2 entry `xl/worksheets/sheet1.xml` qua `ZipArchiveOutputStream`. Apache POI 5.4.0 phát hiện và ném `InvalidFormatException`, `ExcelParserServiceImpl` bắt lỗi và trả về `INVALID_FORMAT` an toàn (0 DB mutation).
  2. `testDuplicateZipEntries_contentTypes_rejectedSafely`: Sinh file XLSX chứa 2 entry `[Content_Types].xml`. Ném `InvalidFormatException` và được chuyển đổi về `INVALID_FORMAT`.
  3. `testMalformedZipWithValidMagicHeader_rejectedSafely`: File ZIP cắt ngắn với magic header hợp lệ trả về `INVALID_FORMAT`.
  4. `testResourceCleanup_inputStreamHandledSafely`: Kiểm chứng giải phóng an toàn tài nguyên InputStream trong khối try-with-resources.

### 4. Kết quả Nghiệm thu
- `mvn dependency:tree "-Dincludes=org.apache.poi"`: Chỉ tồn tại duy nhất `org.apache.poi:poi-ooxml:jar:5.4.0`, `poi:5.4.0`, `poi-ooxml-lite:5.4.0`. Zero older versions.
- `mvn dependency:tree "-Dincludes=org.apache.commons"`: `commons-compress:1.27.1`, `commons-lang3:3.14.0`, `commons-collections4:4.4`, `commons-math3:3.6.1`.
- `mvn clean test`: **`1068/1068 tests PASS`** (0 failures, 0 errors, 0 skipped) trên Testcontainers MySQL 8.4 LTS.
- **Trạng thái:** **`CLOSED / VERIFIED`**.

---

## TASK R3.8 — KANGXI RADICALS MISSING PINYIN CORRECTION & SEED DATA INGESTION REPORT

### 1. Bối cảnh & Phân tích Dữ liệu Gốc
- **Vấn đề phát hiện:** Trong seed data ban đầu (`V3__seed_radicals.sql` và `.agents/references/radicals.json`), tồn tại đúng 2 bộ thủ Khang Hy bị thiếu dữ liệu Pinyin (giá trị chuỗi rỗng `''`):
  - Radical ID 49: `(49, '己', '', 'Kỷ', '')`
  - Radical ID 172: `(172, '隹', '', 'Chuy', '')`
- **Toàn vẹn các trường khác:**
  - 212 bộ thủ còn lại đều có Pinyin đầy đủ, hợp lệ.
  - Trường `meaning_han_viet` đạt 100% (214/214 bộ thủ).
  - Trường `meaning_vi` là chuỗi rỗng `""` cho toàn bộ 214 bộ thủ (chờ dữ liệu dịch nghĩa chính thức được duyệt, không tự ý bịa nghĩa).
- **Ràng buộc di chuyển (Migration Invariants):**
  - Tuyệt đối không chỉnh sửa các file Flyway đã áp dụng (`V1`..`V6`).
  - Phải tạo một forward migration mới: `V7__correct_radical_pinyin.sql`.
  - Giữ nguyên số lượng bộ thủ = 214, giữ nguyên ID, ký tự, và Han-Viet.

### 2. Thẩm tra Nguồn Chuẩn Ngôn ngữ (Authoritative Linguistic References)
Đối chiếu theo Unicode Standard Chapter 18 & Unihan Database UAX #38 (`kMandarin`):
1. **Radical ID 49:**
   - Database Character: `己` (`U+5DF1` - CJK Unified Ideograph)
   - Kangxi Radical Number: 49 (tương ứng biểu tượng bộ thủ `⼰` `U+2F30`)
   - Hán-Việt: `Kỷ`
   - Unihan property `kMandarin`: **`jǐ`** (âm đọc chuẩn tiếng Quan thoại: thanh 3 / âm trầm)
2. **Radical ID 172:**
   - Database Character: `隹` (`U+96B9` - CJK Unified Ideograph)
   - Kangxi Radical Number: 172 (tương ứng biểu tượng bộ thủ `⾫` `U+2FAB`)
   - Hán-Việt: `Chuy`
   - Unihan property `kMandarin`: **`zhuī`** (âm đọc chuẩn tiếng Quan thoại: thanh 1 / âm bằng)

### 3. Triển khai Flyway Migration V7
Tạo file `backend/src/main/resources/db/migration/V7__correct_radical_pinyin.sql` (UTF-8 without BOM):
```sql
-- Migration: V7__correct_radical_pinyin.sql
-- Description: Correct missing Pinyin values for Kangxi radicals 49 (己 -> jǐ) and 172 (隹 -> zhuī)

UPDATE `RADICAL` SET `pinyin` = 'jǐ' WHERE `radical_id` = 49 AND `character` = '己';
UPDATE `RADICAL` SET `pinyin` = 'zhuī' WHERE `radical_id` = 172 AND `character` = '隹';
```

### 4. Cập nhật Bộ Kiểm thử Tự động (Automated Verification Tests)
1. **`ChineseDomainDataAuditTests.java`:**
   - Cập nhật phương thức `testRadicalPinyinAudit()`:
     - Khẳng định `missingPinyinIds.isEmpty()` (100% bộ thủ đều có Pinyin).
     - Kiểm tra trực tiếp Radical 49: `character = "己"`, `pinyin = "jǐ"`, `meaningHanViet = "Kỷ"`, `meaningVi = ""`.
     - Kiểm tra trực tiếp Radical 172: `character = "隹"`, `pinyin = "zhuī"`, `meaningHanViet = "Chuy"`, `meaningVi = ""`.
2. **`LessonLifecycleIntegrationTests.java`:**
   - Cập nhật `FlywaySchemaConsistencyTests.testFlywaySchemaConsistency()`: kiểm chứng schema version hiện tại đã di chuyển thành công lên **`"7"`**.

### 5. Kết quả Kiểm thử & Bằng chứng Thực thi
- Chạy kiểm thử mục tiêu: `mvn test "-Dtest=ChineseDomainDataAuditTests,RadicalIntegrationTests,RadicalServiceIntegrationTests,RadicalControllerTests,RadicalServiceTests"`: **59/59 tests PASS**.
- Chạy hồi quy toàn bộ hệ thống: `mvn clean test`: **1068/1068 tests PASS** (0 failures, 0 errors, 0 skipped, 02:51 min trên Testcontainers MySQL 8.4).
- Flyway log xác nhận: `Successfully applied 7 migrations to schema elearning_test, now at version v7`.
- **Trạng thái:** **`CLOSED / VERIFIED`**.
- **Kết luận:** **`R3.8 COMPLETE — RADICAL PINYIN CORRECTED`**.

---

## R3.9 — DATABASE INDEX PERFORMANCE & SLOW QUERY VERIFICATION (EXPLAIN)

**Audit Date:** 2026-09-04  
**Auditor:** Senior Backend Engineer + Database Performance Engineer  
**Database Engine:** MySQL Community Server 8.4 LTS (InnoDB Storage Engine) via Testcontainers  
**Test Suite:** `DatabaseIndexPerformanceExplainTests.java` (6/6 tests PASS)  
**Full Regression Baseline:** 1074/1074 tests PASS (0 failures, 0 errors, 0 skipped, ~03:02 min)  
**Schema Version:** v7 (Flyway 7 migrations: V1..V7) — No new migration required  
**Final Verdict:** `R3.9 COMPLETE — INDEXES VERIFIED, NO CHANGE REQUIRED`

### 1. Phạm vi & Mục tiêu Kiểm thử
Xác minh thực nghiệm hiệu năng chỉ mục và kế hoạch thực thi câu truy vấn (`EXPLAIN` và `EXPLAIN ANALYZE FORMAT=TREE`) trên toàn bộ 14 bảng và 12 Repository JPA của dự án:
1. Lập danh mục chỉ mục vật lý (Schema & Index Inventory) trên MySQL 8.4.
2. Kiểm tra hàng đợi ôn tập thẻ học SRS `CARD_PROGRESS` (`user_id = ? AND next_review_at <= ? ORDER BY next_review_at ASC LIMIT 100`).
3. Kiểm tra tra cứu từ vựng `VOCABULARY` theo Pinyin (`idx_vocab_pinyin_raw`), theo chữ Hán (`uk_vocab_hanzi_pinyin_raw` leftmost prefix), và tìm kiếm từ khóa.
4. Kiểm tra danh sách bài học `LESSON` theo trạng thái (`idx_lesson_status`) và tác giả (`idx_lesson_created_by`).
5. Kiểm tra liên kết từ vựng trong bài học `LESSON_VOCABULARY` (`uk_lesson_order_index`).
6. Kiểm tra nhật ký kiểm duyệt `MODERATION_LOG` và nhật ký ôn tập `REVIEW_LOG` (`idx_review_log_user_date`).
7. Xác minh tính chuẩn mực của các bảng danh mục nhỏ (`ROLE`, `RADICAL`).
8. Đánh giá chỉ mục dư thừa (Redundant Index) và chỉ mục thiếu (Missing Index).

### 2. Danh mục Chỉ mục Vật lý CSDL (Physical Index Inventory — 14 bảng, 47 entries)
- **`ACCOUNT`:** `PRIMARY (account_id)`, `uk_account_email_or_phone (email_or_phone)`
- **`ROLE`:** `PRIMARY (role_id)`, `uk_role_name (role_name)`
- **`USER_PROFILE`:** `PRIMARY (user_id)`, `uk_user_profile_account (account_id)`
- **`ACCOUNT_ROLE`:** `PRIMARY (account_id, role_id)`, `fk_account_role_role (role_id)`
- **`RADICAL`:** `PRIMARY (radical_id)`, `uk_radical_character (character)` (214 rows, static catalog)
- **`VOCABULARY`:** `PRIMARY (vocab_id)`, `uk_vocab_hanzi_pinyin_raw (hanzi, pinyin_raw)`, `idx_vocab_pinyin_raw (pinyin_raw)`, `idx_vocab_hanzi (hanzi)`
- **`VOCAB_RADICAL`:** `PRIMARY (vocab_id, radical_id)`, `fk_vocab_radical_radical (radical_id)`
- **`LESSON`:** `PRIMARY (lesson_id)`, `idx_lesson_created_by (created_by)`, `idx_lesson_status (status)`
- **`LESSON_VOCABULARY`:** `PRIMARY (lesson_id, vocab_id)`, `uk_lesson_order_index (lesson_id, order_index)`, `fk_lesson_vocab_vocab (vocab_id)`
- **`USER_SRS_SETTING`:** `PRIMARY (setting_id)`, `uk_user_srs_setting_user (user_id)`
- **`CARD_PROGRESS`:** `PRIMARY (progress_id)`, `uk_card_progress_user_item (user_id, item_type, item_id)`, `idx_card_progress_due (user_id, next_review_at)`
- **`REVIEW_LOG`:** `PRIMARY (log_id)`, `idx_review_log_user_date (user_id, reviewed_at)`
- **`PERSONAL_NOTE`:** `PRIMARY (note_id)`, `idx_personal_note_user_vocab (user_id, vocab_id)`, `fk_personal_note_vocab (vocab_id)`
- **`MODERATION_LOG`:** `PRIMARY (log_id)`, `idx_moderation_lesson (lesson_id)`, `fk_moderation_log_moderator (moderator_id)`

### 3. Kết quả Thực nghiệm Kế hoạch Thực thi (EXPLAIN & EXPLAIN ANALYZE)

| Truy vấn | Câu lệnh SQL / Repository Method | Chỉ mục sử dụng | Loại truy cập (Type) | Extra / Ghi chú | Thời gian thực thi |
| :--- | :--- | :--- | :---: | :--- | :---: |
| **SRS Due Queue** | `SELECT ... FROM CARD_PROGRESS WHERE user_id=? AND next_review_at<=? ORDER BY next_review_at ASC LIMIT 100` | `idx_card_progress_due` | `range` | `Using index condition` (**0 filesort**) | **0.136 ms** |
| **SRS Due with Item Type** | `SELECT ... FROM CARD_PROGRESS WHERE user_id=? AND item_type=? AND next_review_at<=? ORDER BY next_review_at ASC LIMIT 100` | `idx_card_progress_due` | `range` | `Using index condition; Using where` (**0 filesort**) | **0.145 ms** |
| **SRS Due Count** | `SELECT COUNT(progress_id) FROM CARD_PROGRESS WHERE user_id=? AND next_review_at<=?` | `idx_card_progress_due` | `range` | `Using where; Using index` (**Covering Index Scan**) | **0.075 ms** |
| **SRS Item Unique Lookup** | `SELECT ... FROM CARD_PROGRESS WHERE user_id=? AND item_type=? AND item_id=?` | `uk_card_progress_user_item` | `const` | Rows fetched before execution | **0.00005 ms** |
| **SRS Item Exists Guard** | `SELECT 1 FROM CARD_PROGRESS WHERE item_type=? AND item_id=? LIMIT 1` | `uk_card_progress_user_item` | `index` | `Using where; Using index` (**Covering Index Scan**) | **0.027 ms** |
| **Vocab Pinyin Exact** | `SELECT ... FROM VOCABULARY WHERE pinyin_raw=? LIMIT 20` | `idx_vocab_pinyin_raw` | `ref` | Single key lookup (`key_len=402`) | **0.021 ms** |
| **Vocab Business Key** | `SELECT ... FROM VOCABULARY WHERE hanzi=? AND pinyin_raw=?` | `uk_vocab_hanzi_pinyin_raw` | `const` | Rows fetched before execution (`key_len=604`) | **0.00005 ms** |
| **Vocab Hanzi Exact** | `SELECT ... FROM VOCABULARY WHERE hanzi=? LIMIT 20` | `uk_vocab_hanzi_pinyin_raw` | `ref` | Leftmost prefix lookup (`key_len=202`) | **0.014 ms** |
| **Vocab Substring Search**| `SELECT ... FROM VOCABULARY WHERE hanzi LIKE ? OR LOWER(pinyin) LIKE ...` | `null` | `ALL` | Full table scan (bắt buộc cho leading wildcard `%...%`) | **0.025 ms** |
| **Lesson Approved Public**| `SELECT ... FROM LESSON WHERE status='Approved' LIMIT 20` | `idx_lesson_status` | `ref` | Dependent subquery dùng `uk_lesson_order_index` | **0.055 ms** |
| **Lesson Pending Queue** | `SELECT ... FROM LESSON l JOIN ACCOUNT a ON a.account_id=l.created_by WHERE l.status='Pending'` | `idx_lesson_status` | `ref` | Join account qua `PRIMARY` (`eq_ref`) | **0.064 ms** |
| **Lesson by Creator** | `SELECT ... FROM LESSON WHERE created_by=? LIMIT 20` | `idx_lesson_created_by` | `ref` | Single ref lookup (`key_len=8`) | **0.080 ms** |
| **Lesson by Creator+Status**| `SELECT ... FROM LESSON WHERE created_by=? AND status=? LIMIT 20` | `idx_lesson_status` | `ref` | Ref lookup filter created_by | **0.063 ms** |
| **LessonVocab Order Index**| `SELECT ... FROM LESSON_VOCABULARY lv JOIN VOCABULARY v ON lv.vocab_id=v.vocab_id WHERE lv.lesson_id=? ORDER BY lv.order_index ASC` | `uk_lesson_order_index` | `ref` | Covering index lookup (**0 filesort**), Join v `PRIMARY` | **0.019 ms** |
| **SRS New Card Candidates**| Antijoin `LESSON_VOCABULARY` $\Join$ `LESSON` $\Join$ `VOCABULARY` $\overline{\Join}$ `CARD_PROGRESS` | `uk_lesson_order_index`, `PRIMARY`, `uk_card_progress_user_item` | `ref` / `eq_ref` | Covering antijoin, sub-millisecond | **0.150 ms** |
| **Vocab Delete Lesson Guard**| `SELECT COUNT(lesson_id) FROM LESSON_VOCABULARY lv JOIN LESSON l ... WHERE lv.vocab_id=? AND status='Approved'` | `fk_lesson_vocab_vocab`, `PRIMARY` | `ref` / `eq_ref` | Covering index lookup on lv | **0.020 ms** |
| **ReviewLog Today Lock** | `SELECT log_id FROM REVIEW_LOG WHERE user_id=? AND reviewed_at>=? FOR UPDATE` | `idx_review_log_user_date` | `range` | `Using where; Using index` (**Covering Index Range Scan**) | **0.028 ms** |
| **Personal Notes by User+Vocab**| `SELECT ... FROM PERSONAL_NOTE WHERE user_id=? AND vocab_id=? ORDER BY created_at DESC LIMIT 20` | `fk_personal_note_vocab` / `idx_personal_note_user_vocab` | `ref` | Ref lookup, in-memory sort cho 1-5 notes | **0.030 ms** |

### 4. Phân tích Chuyên sâu Kế hoạch Thực thi & Đánh giá Chỉ mục

1. **`CARD_PROGRESS` Due Queue (`idx_card_progress_due`):**
   - Ràng buộc cấu trúc: `INDEX idx_card_progress_due (user_id, next_review_at)`.
   - Khi truy vấn `WHERE user_id = ? AND next_review_at <= ? ORDER BY next_review_at ASC`:
     - MySQL optimizer sử dụng `type = range` với `key_len = 14`.
     - Vì hai cột của index khớp chính xác theo thứ tự `user_id` (đẳng thức) và `next_review_at` (khoảng và sắp xếp tăng dần), **MySQL hoàn toàn không cần thực hiện `filesort`** (`Extra = Using index condition`).
     - Thời gian thực thi thực tế đạt **0.136 ms** trên 2,000 thẻ học.
2. **`CARD_PROGRESS` Due Count (`idx_card_progress_due`):**
   - Khi truy vấn `SELECT COUNT(progress_id) WHERE user_id = ? AND next_review_at <= ?`:
     - MySQL optimizer thực hiện `Covering index range scan` (`Extra = Using where; Using index`).
     - CSDL chỉ đọc trực tiếp cây B-Tree của chỉ mục `idx_card_progress_due`, hoàn toàn không chạm vào trang dữ liệu bảng chính (clustered index table pages), thời gian thực thi chỉ **0.075 ms**.
3. **`LESSON_VOCABULARY` In-Order Retrieval (`uk_lesson_order_index`):**
   - Ràng buộc duy nhất: `UNIQUE KEY uk_lesson_order_index (lesson_id, order_index)`.
   - Khi truy vấn `WHERE lesson_id = ? ORDER BY order_index ASC`:
     - MySQL optimizer thực hiện `Covering index lookup` (`Extra = Using index`, `type = ref`).
     - Thứ tự `order_index ASC` được đảm bảo tự nhiên bởi cấu trúc cây B-Tree của unique index, **loại bỏ 100% chi phí `filesort`**, thời gian thực thi **0.019 ms**.
4. **Phát hiện Chỉ mục Dư thừa (Redundant Index Audit — `VOCABULARY`):**
   - Bảng `VOCABULARY` hiện có hai chỉ mục bắt đầu bằng cột `hanzi`:
     1. `UNIQUE KEY uk_vocab_hanzi_pinyin_raw (hanzi, pinyin_raw)`
     2. `INDEX idx_vocab_hanzi (hanzi)`
   - Theo nguyên tắc Leftmost Prefix của cây B-Tree trong InnoDB, mọi truy vấn tìm kiếm `WHERE hanzi = ?` đều có thể tận dụng tiền tố ngoài cùng bên trái của `uk_vocab_hanzi_pinyin_raw` với `key_len = 202`.
   - Kết quả EXPLAIN thực tế khẳng định: khi tìm kiếm `WHERE hanzi = ?`, MySQL optimizer tự động lựa chọn `uk_vocab_hanzi_pinyin_raw` (`type = ref`, rows = 1, actual time = 0.014 ms).
   - Chỉ mục đơn cột `idx_vocab_hanzi` là **chỉ mục dư thừa về mặt cấu trúc (structurally redundant index)**. Tuy nhiên, chỉ mục này không gây sai lệch kế hoạch thực thi (optimizer luôn chọn index tốt nhất), dung lượng chiếm dụng không đáng kể trên tập từ vựng hiện tại, và việc xóa bỏ chỉ mục không mang lại cải thiện đọc đo lường được. Do đó, ghi nhận đánh giá kỹ thuật và giữ nguyên schema ổn định.
5. **Đánh giá Bảng nhỏ & Bảng danh mục (`ROLE`, `RADICAL`):**
   - `ROLE` chỉ có 4 bản ghi cố định; `RADICAL` có đúng 214 bản ghi chuẩn Khang Hy.
   - Toàn bộ dữ liệu của `ROLE` và `RADICAL` nằm trọn vẹn trong **một trang InnoDB Buffer Pool duy nhất (16 KB)**.
   - Việc MySQL optimizer chọn Table Scan (`type = ALL`) hoặc Clustered Index Scan cho các truy vấn danh mục là hoàn toàn tự nhiên và tối ưu nhất (tránh chi phí phân nhánh qua con trỏ chỉ mục phụ), thời gian thực thi < 0.02 ms. Không cần tạo thêm bất kỳ index nào cho các bảng này.

### 5. Kết luận Nghiệm thu
- **Trạng thái:** **`CLOSED / VERIFIED`**.
- **Flyway Migration:** **Không cần migration mới** (giữ nguyên schema version v7).
- **Hồi quy kiểm thử:** `mvn clean test` PASS **1074/1074 tests** (0 failures, 0 errors, 0 skipped).
- **Phán quyết cuối cùng:** **`R3.9 COMPLETE — INDEXES VERIFIED, NO CHANGE REQUIRED`**.

---

## R3.10 — PRODUCTION HTTP & REVERSE PROXY SECURITY HEADERS AUDIT & HARDENING

**Completion Date:** 2026-09-04  
**Role:** Senior Backend Engineer + Web Security Engineer  
**Status:** **`CLOSED / VERIFIED`**  
**Regression Baseline:** **1088/1088 tests PASS** (0 failures, 0 errors, 0 skipped)  
**Final Verdict:** **`R3.10 COMPLETE — PRODUCTION HTTP SECURITY HARDENED`**  

### 1. Summary of Actions
- **Referrer Policy Hardening:** Configured explicit `Referrer-Policy: strict-origin-when-cross-origin` in `SecurityConfig.java` via `headers.referrerPolicy(...)`.
- **Default Spring Security Headers Preserved:** Verified active enforcement of `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `X-XSS-Protection: 0`, and `Cache-Control: no-cache, no-store, max-age=0, must-revalidate` across all endpoints, including 401, 403, 404, and 500 error pathways.
- **HSTS Protocol Integrity:** Verified RFC 6797 Section 7.2 compliance. Spring Security strictly omits `Strict-Transport-Security` over plain HTTP (`http://localhost:8080`) and automatically emits `Strict-Transport-Security: max-age=31536000 ; includeSubDomains` when request is secure (HTTPS or recognized HTTPS via trusted reverse proxy).
- **Reverse Proxy / Forwarded Headers Configuration:** Configured `server.forward-headers-strategy: ${SERVER_FORWARD_HEADERS_STRATEGY:framework}` in both `src/main/resources/application.yml` and `src/test/resources/application.yml`. Activates Spring's `ForwardedHeaderFilter` to correctly adapt request scheme and client IP from trusted proxies (e.g. Nginx, Cloudflare) while allowing direct deployments to set `SERVER_FORWARD_HEADERS_STRATEGY=none`.
- **Client IP Rate Limiting Isolation:** Proved via integration tests that `LoginRateLimiter` isolates failed login attempts per client IP derived from `X-Forwarded-For`, preventing shared proxy IP exhaustion.
- **CSP Explicit Deferral:** Content Security Policy (CSP) is intentionally deferred to Phase 9 Frontend (Task 9A.1) as the backend is exclusively a JSON REST API (`application/json`) with no HTML/JS assets served.

### 2. Verification Evidence
- `HttpSecurityHeadersIntegrationTests.java`: 13 test methods covering public, authenticated, error pathways, HTTPS vs HTTP, proxy simulation, dev mode, and IP isolation.
- `ForwardedHeadersDisabledStrategyIntegrationTests.java`: 1 test method verifying spoof rejection when forward strategy is disabled.
- **Full Regression:** `mvn clean test` PASS **1088/1088 tests** (0 failures, 0 errors, 0 skipped).

---

## R3.11 — BACKEND FINAL QUALITY GATE & PRE-FRONTEND RELEASE SEAL

**Completion Date:** 2026-09-04  
**Role:** Principal Backend Engineer + Security Reviewer + Release Gatekeeper  
**Status:** **`CLOSED / VERIFIED (BACKEND SEALED)`**  
**Regression Baseline:** **1088/1088 tests PASS** (0 failures, 0 errors, 0 skipped, ~03:16 min trên Testcontainers MySQL 8.4)  
**Release Decision:** **`DEC-41`**  
**Final Verdict:** **`R3.11 PASS WITH DEFERRED ITEMS — BACKEND SEALED FOR PHASE 9`**  

### 1. Verification Summary
- **100% R3.x Hardening Reconciled:** Verified planned, implemented, tested, and documented state across all remediation tasks R3.4 through R3.10.
- **Database & Flyway Clean Gate:** Verified 7 migration scripts (`V1__init_schema.sql` through `V7__correct_radical_pinyin.sql`), checksum consistency, 0 pending, 0 failed migrations, zero modification of applied scripts.
- **Domain Invariants Preserved:**
  - Radicals: Exactly 214 contiguous radicals, Pinyin corrections (Radical 49 己 `jǐ`, Radical 172 隹 `zhuī`) verified, `meaning_vi` empty string preserved.
  - Vocabulary: Unique constraint `uk_vocab_hanzi_pinyin_raw` verified, referential guards protecting against accidental cascade or orphan records verified.
  - Lessons: Full lifecycle state transitions (Draft -> Pending -> Approved / Rejected), immutable moderation audit logs, delete protection on non-draft lessons verified.
  - SRS: SuperMemo-2 mathematical calculations, linearize due-only review policy, approved-lesson eligibility guard, optimistic locking (`@Version`) verified.
- **Authentication & Security Boundaries:**
  - BCrypt password encryption, server-side active status check, immediate role revocation via `authorization_version` (`auth_ver`), sliding window login rate limiter.
  - Object-level authorization (IDOR / BOLA) verified: zero cross-user leakage on personal notes, lessons, SRS reviews, or admin management.
- **API & DTO Contract Isolation:**
  - 100% DTO isolation verified across all 15 controllers and 45 endpoints. Zero JPA entities exposed.
  - Standard `ApiResponse<T>` envelope and `PageResponse<T>` pagination consistently enforced.
  - `GlobalExceptionHandler` handles 400, 401, 403, 404, 409, 413, 422, 500 without leaking stack traces or internal database details.
- **Dependency & Configuration Hygiene:**
  - Java 21, Spring Boot 3.3.5, Apache POI 5.4.0 (CVE-2025-31672 resolved), JJWT 0.12.6.
  - Zero hardcoded production secrets in `application.yml` (`${JWT_SECRET}` enforced).
- **Handoff Contract Prepared:** REST API contract sealed for Phase 9 Frontend. Backend phase closed.


