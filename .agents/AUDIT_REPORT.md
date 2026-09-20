# AUDIT REPORT — EXTERNAL BACKEND AUDIT EVIDENCE

> [!WARNING]
> **STATUS: HISTORICAL**  
> **NOT CURRENT PROJECT CONTRACT**  
> This document is preserved for historical audit trail and record-keeping purposes only. It reflects an archived snapshot (2026-08-30, 775 tests, 3 migrations V1..V3) and does NOT govern current project architecture, contracts, or implementation. Refer to `PROJECT-CONTRACT.md`, `API.md`, and `CURRENT_STATE.md` for current normative authority.

> **Document Classification:** External Backend Audit — Historical Reference Material  
> **Status:** HISTORICAL ARCHIVE / AUDIT EVIDENCE #1 (Snapshot 2026-08-30, 775 tests, 3 migrations V1..V3)  
> **Audit Date:** 2026-08-30  
> **Auditor:** Claude Opus / External AI Backend Audit  
> **Project:** E-Learning Chinese Radicals & Vocabulary System  
> **Purpose:** Backend hardening and remediation input (15 findings remediated and closed in `BACKEND_REMEDIATION.md`)  

> [!NOTE]
> **AUDIT EVOLUTION & RELATIONSHIP:**
> ```text
> 1. Historical Audit #1 (2026-08-30): 15 findings identified (775 tests baseline).
>       ↓
> 2. Backend Remediation (2026-08-30): 15/15 findings CLOSED (881 tests baseline).
>       ↓
> 3. Module 8D Completion (2026-08-31): Tasks 8D.1..8D.8 completed (948 tests baseline).
>       ↓
> 4. Backend Full Audit & Second-Pass Challenge (2026-08-31):
>    - Initial verdict: APPROVED
>    - Second-pass challenge: APPROVED WITH CONDITIONS
>    - Residual items (POI CVE-2025-31672, unpaged notes, rate limiter TTL eviction) scheduled for Phase 10 Hardening.
> ```
> **Authoritative current state:** [`.agents/CURRENT_STATE.md`](file:///c:/Users/LENOVO/Downloads/elearning-1.0.0-20260819T062551Z-1-002/elearning-1.0.0/elearning-1.0.0/.agents/CURRENT_STATE.md).

> [!IMPORTANT]
> **THIS IS A HISTORICAL AUDIT DOCUMENT.**
>
> This file preserves the original findings of the external backend audit performed on 2026-08-30.
> All 15 findings from this audit have been resolved and closed in `.agents/BACKEND_REMEDIATION.md`.
> Current project state reflects Flyway `V1`..`V6` and `948/948 PASS` tests.

---

# Deep Backend Health, Security, Architecture & Production Readiness Audit

**Date:** 2026-08-30  
**Auditor:** AI Backend Auditor  
**Project:** E-Learning Chinese Radicals & Vocabulary System  
**Repository:** `elearning-1.0.0/backend/`

---

## A. Audit Scope

| Metric | Value |
|---|---|
| Main Java source files inspected | 85 |
| Test Java source files inspected | 73 |
| Migration files inspected | 3 (V1, V2, V3) |
| Configuration files inspected | 2 (pom.xml, application.yml) |
| `.agents/` files consulted | 12 (PROJECT_CONTEXT, ARCHITECTURE, DATABASE, DATABASE_DESIGN, API, DECISIONS, OPEN_QUESTIONS, CURRENT_STATE, PROGRESS, RUNBOOK, WORKFLOW, + rules) |
| External standards consulted | OWASP API Security Top 10 (2023), RFC 7519, Spring Security Reference, Spring Transaction Reference, MySQL 8.4 InnoDB Reference, Flyway Versioned Migrations, OWASP File Upload Cheat Sheet |

---

## B. Baseline Verification

| Item | Value |
|---|---|
| **Java version** | OpenJDK 21.0.12 (Temurin-21.0.12+8-LTS) |
| **Maven version** | Apache Maven 3.9.16 |
| **Spring Boot version** | 3.3.5 |
| **Build result** | **BUILD SUCCESS** |
| **Tests executed** | **775** |
| **Failures** | **0** |
| **Errors** | **0** |
| **Skipped** | **0** |
| **Build time** | 46.005 s |

> [!TIP]
> All 775 tests pass cleanly with zero failures, errors, or skipped tests. Build is stable.

---

## C. Architecture Assessment

| Architecture Rule | Status | Evidence |
|---|---|---|
| Controller → Service → Repository layering | **PASS** | All 11 controllers delegate to service interfaces. No repository injected directly into controllers. |
| DTO isolation (zero entity leak) | **PASS** | Every controller returns `ApiResponse<T>` with DTO types. Response DTOs use `fromEntity()` factory methods. No `@Entity` returned directly. |
| `spring.jpa.open-in-view: false` | **PASS** | Confirmed in `application.yml` line 25. |
| `ddl-auto: none` (Flyway controls schema) | **PASS** | Confirmed in `application.yml` line 27. |
| Stateless session management | **PASS** | `SessionCreationPolicy.STATELESS` in `SecurityConfig.java`. |
| CSRF disabled (appropriate for stateless JWT API) | **PASS** | Correct for stateless Bearer-token API per Spring Security reference. |
| Unified `ApiResponse<T>` envelope | **PASS** | All controllers wrap responses in `ApiResponse.success()` or `ApiResponse.error()`. |
| `GlobalExceptionHandler` as centralized error handler | **PASS** | Handles `MethodArgumentNotValidException`, `BusinessException`, `AuthenticationException`, `DataIntegrityViolationException`, `MaxUploadSizeExceededException`, `HttpMessageNotReadableException`, and fallback `Exception`. |
| Service interface → implementation pattern | **PASS** | All 10 service interfaces have matching `*Impl` classes. |
| No circular dependencies detected | **PASS** | Dependency injection graph is acyclic. |
| Business logic NOT in controllers | **PASS** | Controllers are thin; all business logic in service layer. |

---

## D. Security Assessment

### D.1 Authentication

| Check | Status | Evidence |
|---|---|---|
| Password hashing: BCrypt | **PASS** | `BCryptPasswordEncoder` bean in `SecurityConfig.java`. |
| JWT signing: HMAC-SHA256 (HS256) | **PASS** | `Keys.hmacShaKeyFor()` with 256-bit key in `JwtUtil.java`. |
| JWT key length validation (≥ 32 bytes) | **PASS** | Validated at construction time, line 42-44. |
| JWT expiration: 24h (86400000ms) | **PASS** | Configured in application.yml. |
| JWT `iat` (issued at) claim | **PASS** | Set via `.issuedAt(now)`. |
| JWT `exp` (expiration) claim | **PASS** | Set via `.expiration(expiryDate)`. |
| JWT `sub` (subject) claim | **PASS** | Set to `emailOrPhone`. |
| Malformed/expired/invalid-signature token handling | **PASS** | Caught via `JwtException` in `validateToken()`, returns false. |
| Missing token handling | **PASS** | Filter passes through to `SecurityFilterChain` which returns 401. |
| Account status check at login | **PASS** | `AuthServiceImpl.java` checks `"Active"` status explicitly at line ~108. |
| Authentication entry point returns JSON | **PASS** | Returns JSON envelope with `UNAUTHORIZED` code. |

#### BE-AUTH-001: JWT Filter Does Not Verify Account Status on Every Request
- **ID:** BE-AUTH-001
- **Severity:** P1
- **Category:** Authentication — Stale Token After Account Disable
- **Component:** `JwtAuthenticationFilter.java`
- **Evidence:** The filter extracts subject and roles directly from the JWT claims (lines 48-56) without loading the Account from the database. If an account is disabled/banned after a JWT is issued, the token remains valid until its 24-hour expiration.
- **Why:** A banned/disabled user retains full access for up to 24 hours. Per OWASP API2:2023 (Broken Authentication), token-based systems should have a mechanism to invalidate sessions for compromised accounts.
- **Expected:** Either (a) verify account status on each request via DB lookup, or (b) implement a token blocklist/revocation, or (c) reduce token TTL significantly.
- **External Authority:** OWASP API Security Top 10 2023 — API2:2023 Broken Authentication
- **Source:** https://owasp.org/API-Security/editions/2023/en/0xa2-broken-authentication/
- **Recommendation:** Add a lightweight account status check in the filter (cache-friendly, e.g., in-memory short TTL cache). Alternatively, accept as a **known trade-off** for the current single-user learning scope if documented.
- **Code change required:** Yes (filter modification + optional cache)

#### BE-AUTH-002: JWT Secret Hardcoded as Default in Both application.yml and @Value Annotation
- **ID:** BE-AUTH-002
- **Severity:** P1
- **Category:** Secrets Management
- **Component:** `application.yml` + `JwtUtil.java`
- **Evidence:** The JWT secret `404E635266556A586E327235...` is committed in source control as the default value in both `application.yml` line 40 and `@Value` annotation fallback in JwtUtil.java line 34. The secret is a well-known hex string that appears in many online tutorials.
- **Why:** Any developer who clones this repository can sign arbitrary JWTs. The same secret appears in multiple public tutorials, making it trivially guessable.
- **Expected:** No default secret in source code. Application should fail to start without an environment-provided secret.
- **External Authority:** OWASP Cheat Sheet — Secrets Management
- **Source:** https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html
- **Recommendation:** Remove the default fallback value. Require `JWT_SECRET` as a mandatory environment variable. Fail fast at startup if missing.
- **Code change required:** Yes

#### BE-AUTH-003: No JWT Issuer (`iss`) Claim
- **ID:** BE-AUTH-003
- **Severity:** P3
- **Category:** Authentication — JWT Best Practice
- **Component:** `JwtUtil.java`
- **Evidence:** The `Jwts.builder()` does not set `.issuer()`. Per RFC 7519 §4.1.1, the `iss` claim identifies the principal that issued the JWT.
- **Why:** Minor. Without `iss`, if the system ever needs to distinguish tokens from multiple issuers (e.g., migration, microservices), it cannot.
- **Expected:** Include `.issuer("elearning-backend")` and validate on parse.
- **External Authority:** RFC 7519 §4.1.1
- **Source:** https://www.rfc-editor.org/rfc/rfc7519#section-4.1.1
- **Recommendation:** Add `iss` claim. Low priority for single-service architecture.
- **Code change required:** Yes (minor)

### D.2 Authorization / BOLA / IDOR

| Check | Status | Evidence |
|---|---|---|
| Creator lesson ownership isolation | **PASS** | `checkOwnership()` in `CreatorLessonServiceImpl.java` compares `createdBy.accountId` with current account. |
| Personal note ownership | **PASS** | `user.getUserId().equals()` check in `PersonalNoteServiceImpl.java`. |
| SRS progress user isolation | **PASS** | `resolveCurrentUserProfile()` ensures all queries filter by current user in SrsServiceImpl. |
| UserSrsSetting user isolation | **PASS** | Settings are queried by `user` parameter in service. |
| Admin bypass for creator lessons | **PASS** | Admin role checked explicitly in `checkOwnership()`. |
| URL-level RBAC enforcement | **PASS** | SecurityConfig enforces `/admin/**` → ADMIN, `/moderator/**` → MODERATOR/ADMIN, `/creator/**` → CREATOR/ADMIN. |
| Moderation: only Pending → Approved/Rejected | **PASS** | State machine checks in ModerationServiceImpl. |

#### BE-AUTHZ-001: ModerationServiceImpl.getPendingLessonById Does Not Filter by Status
- **ID:** BE-AUTHZ-001
- **Severity:** P2
- **Category:** Information Disclosure / Authorization Weakness
- **Component:** `ModerationServiceImpl.java`
- **Evidence:** `getPendingLessonById()` at line 57-62 loads a lesson by ID without checking that `status == "Pending"`. A moderator could view Draft/Rejected lessons by guessing IDs.
- **Why:** Violates the principle that moderators should only see Pending lessons. While moderators are trusted internal users, this leaks non-submitted content.
- **Expected:** Add status filter: `if (!"Pending".equalsIgnoreCase(lesson.getStatus()))` → 404.
- **External Authority:** OWASP API1:2023 — Broken Object Level Authorization
- **Source:** https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/
- **Recommendation:** Add Pending status check to `getPendingLessonById()`.
- **Code change required:** Yes

### D.3 Mass Assignment

| Check | Status | Evidence |
|---|---|---|
| RegisterRequest: only `emailOrPhone`, `password`, `fullName` | **PASS** | No `role`, `status`, or `accountId` field in `RegisterRequest`. |
| ReviewCardRequest: no `userId` or `progressId` | **PASS** | Only `itemType`, `itemId`, `rating`, `reviewTimeSeconds`. |
| No status/role in lesson DTOs | **PASS** | CreateLessonRequest has only `title`, `excelFileUrl`, `vocabularyIds`. |
| No account ownership override via DTO | **PASS** | User resolved from SecurityContext in all services. |

### D.4 File Upload Security

| Check | Status | Evidence |
|---|---|---|
| File extension validation (.xlsx only) | **PASS** | `ExcelParserServiceImpl.java` line 104-111. |
| Magic byte validation (ZIP header PK\x03\x04) | **PASS** | Lines 117-136, validates 4-byte ZIP magic header. |
| File size limit (10MB) | **PASS** | Both in service (`MAX_FILE_SIZE_BYTES`) and Spring config (`max-file-size: 10MB`). |
| Encrypted file handling | **PASS** | `EncryptedDocumentException` caught at line 171. |
| Empty file handling | **PASS** | Multiple checks for null/empty streams. |
| Malformed file handling | **PASS** | `EmptyFileException`, `NotOfficeXmlFileException`, generic `Exception` all caught. |
| No file stored to disk | **PASS** | File is processed in-memory only via POI. No file system writes. |

#### BE-UPLOAD-001: No Row Count Limit on Excel Import
- **ID:** BE-UPLOAD-001
- **Severity:** P2
- **Category:** Resource Consumption / OWASP API4:2023
- **Component:** `ExcelParserServiceImpl.java`
- **Evidence:** The `processSheet()` loop at line 217 iterates from row 1 to `sheet.getLastRowNum()` with no maximum row count check. A 10MB .xlsx file could contain hundreds of thousands of rows.
- **Why:** Even with 10MB file size limit, a malicious .xlsx with minimal cell data could contain ~100k+ rows, causing long processing time and heavy DB queries (per-row vocabulary existence check).
- **Expected:** Enforce a maximum row count (e.g., 5000 rows) to prevent DoS.
- **External Authority:** OWASP API4:2023 — Unrestricted Resource Consumption
- **Source:** https://owasp.org/API-Security/editions/2023/en/0xa4-unrestricted-resource-consumption/
- **Recommendation:** Add `if (lastRowNum > MAX_ROWS) return error;` check before row iteration.
- **Code change required:** Yes

#### BE-UPLOAD-002: POI Workbook Loaded Fully Into Memory
- **ID:** BE-UPLOAD-002
- **Severity:** P2
- **Category:** Resource Consumption
- **Component:** `ExcelParserServiceImpl.java`
- **Evidence:** `WorkbookFactory.create(bis)` loads the entire workbook into memory. For a 10MB compressed xlsx, the decompressed DOM model can be 5-10x larger (50-100MB RAM).
- **Why:** Under concurrent uploads, multiple 10MB files could exhaust JVM heap.
- **Expected:** Consider SAX-based streaming parser (`XSSFReader` + `SheetContentsHandler`) for large files, or enforce a stricter file size limit.
- **Recommendation:** For current scope (single-user learning app), this is an **ACCEPTABLE TRADE-OFF** with the 10MB limit. Flag for future if concurrent usage increases.
- **Code change required:** No (for current scope)

### D.5 Input Validation

| Check | Status | Evidence |
|---|---|---|
| `@Valid` on request DTOs in controllers | **PASS** | Applied to `RegisterRequest`, `LoginRequest`, `CreateLessonRequest`, `ReviewCardRequest`, etc. |
| DTO field constraints (Bean Validation) | **PASS** | DTOs use `@NotBlank`, `@Size`, etc. |
| SRS rating range validation (1-4) | **PASS** | Validated in SrsServiceImpl lines 157-158. |
| Note content length ≤ 500 | **PASS** | Validated in PersonalNoteServiceImpl line 62-63. |
| Enum validation for item_type | **PASS** | Only "VOCABULARY" or "RADICAL" accepted in SrsServiceImpl. |

### D.6 Error Disclosure

| Check | Status | Evidence |
|---|---|---|
| Stack traces not leaked | **PASS** | Fallback handler logs internally, returns generic message. |
| SQL errors not leaked | **PASS** | `DataIntegrityViolationException` handler returns generic conflict message. |
| Internal class names not leaked | **PASS** | Error responses use `ErrorCode` enum codes only. |

### D.7 CORS

#### BE-CORS-001: No CORS Configuration Present
- **ID:** BE-CORS-001
- **Severity:** P2
- **Category:** Security Configuration
- **Component:** `SecurityConfig.java`
- **Evidence:** No `.cors()` configuration in SecurityFilterChain. No `@CrossOrigin` annotations on controllers. No `WebMvcConfigurer.addCorsMappings()` found.
- **Why:** When the frontend (served separately on a different port or static server) makes API calls, browsers will block cross-origin requests. This will be a blocker for Phase 9 (Frontend).
- **Expected:** Configure CORS with specific allowed origins before frontend integration.
- **Recommendation:** Add CORS configuration before Phase 9. This is expected pre-frontend work.
- **Code change required:** Yes (before FE integration)

---

## E. Transaction & Concurrency Assessment

### E.1 Transaction Boundaries

| Service Method | @Transactional | Multi-write? | Status |
|---|---|---|---|
| `AuthServiceImpl.register()` | `@Transactional` | Account + Profile + Role | **PASS** |
| `CreatorLessonServiceImpl.createLesson()` | `@Transactional` | Lesson + LessonVocabulary×N | **PASS** |
| `CreatorLessonServiceImpl.deleteMyLesson()` | `@Transactional` | LessonVocab delete + Lesson delete | **PASS** |
| `CreatorLessonServiceImpl.importLessonFromExcel()` | `@Transactional` | Lesson + Vocabulary×N + LessonVocab×N | **PASS** |
| `CreatorLessonServiceImpl.reorderVocabulary()` | `@Transactional` | LessonVocab×N (two-phase) | **PASS** |
| `ModerationServiceImpl.approveLesson()` | `@Transactional` | Lesson status + ModerationLog | **PASS** |
| `ModerationServiceImpl.rejectLesson()` | `@Transactional` | Lesson status + ModerationLog | **PASS** |
| `SrsServiceImpl.reviewCard()` | `@Transactional` | CardProgress + ReviewLog | **PASS** |
| `PersonalNoteServiceImpl.createNote()` | `@Transactional` | Single write | **PASS** |
| `UserSrsSettingServiceImpl.updateSetting()` | `@Transactional` | Single write | **PASS** |

> [!NOTE]
> All multi-write operations are correctly annotated with `@Transactional`. Spring's default `RuntimeException` rollback covers `BusinessException` (which extends `RuntimeException`). No self-invocation proxy bypass patterns detected.

### E.2 Transaction Failure Scenarios

| Scenario | Expected | Actual |
|---|---|---|
| Excel import: Lesson created → Vocab 3 fails | Full rollback | **PASS** — Single `@Transactional` boundary covers all. |
| SRS review: Progress saved → ReviewLog insert fails | Full rollback | **PASS** — Same transaction. |
| Moderation: Lesson status updated → ModerationLog fails | Full rollback | **PASS** — Same transaction. |
| Registration: Account saved → Profile fails | Full rollback | **PASS** — CascadeType.ALL handles this within single save. |

### E.3 Concurrency Assessment

#### BE-CONC-001: SRS Review Card — Race Condition on CardProgress
- **ID:** BE-CONC-001
- **Severity:** P2
- **Category:** Concurrency / Lost Update Risk
- **Component:** `SrsServiceImpl.reviewCard()`
- **Evidence:** The method reads CardProgress (line 178), calculates new state, then saves (line 218). Under MySQL InnoDB REPEATABLE READ (default), two concurrent requests for the same card by the same user could:
  1. Request A reads progress (EF=2.50, reps=1)
  2. Request B reads same progress (EF=2.50, reps=1) — same snapshot
  3. Request A calculates and saves (EF=2.36, reps=2)
  4. Request B calculates and saves (EF=2.36, reps=2) — overwrites A's result
  
  The unique constraint `uk_card_progress_user_item` prevents duplicate inserts but NOT lost updates on existing rows.
- **Why:** Lost update. The second review overwrites the first. Both create ReviewLog entries but CardProgress reflects only the last write.
- **Expected:** Use `@Version` optimistic locking on CardProgress entity, or `SELECT ... FOR UPDATE` pessimistic lock.
- **External Authority:** MySQL InnoDB Transaction Isolation Levels
- **Source:** https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html
- **Recommendation:** Add `@Version` column to CardProgress entity. This is the smallest correct concurrency control for this use case.
- **Code change required:** Yes (entity + migration)

#### BE-CONC-002: Daily Review Quota — TOCTOU Race
- **ID:** BE-CONC-002
- **Severity:** P2
- **Category:** Concurrency / Quota Bypass
- **Component:** `SrsServiceImpl.reviewCard()` and `SrsServiceImpl.getDueCards()`
- **Evidence:** `getDueCards()` checks the daily review count (line 99) but `reviewCard()` does NOT re-check the quota before processing a review. A user sending concurrent review requests could exceed the daily limit.
- **Why:** The review count is checked at read-time only, creating a time-of-check-time-of-use (TOCTOU) gap.
- **Expected:** Check quota inside `reviewCard()` within the same transaction boundary.
- **Recommendation:** Add quota check to `reviewCard()`. For current single-user scope, this is unlikely to be exploited, but it's a correctness gap.
- **Code change required:** Yes

#### BE-CONC-003: Double Moderation Race
- **ID:** BE-CONC-003
- **Severity:** P2
- **Category:** Concurrency / Double-Write
- **Component:** `ModerationServiceImpl`
- **Evidence:** Two moderators simultaneously approving the same Pending lesson: both read status as "Pending", both pass the check, both set "Approved" and create separate ModerationLog entries. The lesson ends up with two approval logs.
- **Why:** No pessimistic/optimistic lock on LESSON row during moderation.
- **Expected:** Use `@Version` on Lesson entity, or `SELECT ... FOR UPDATE`.
- **Recommendation:** Add optimistic locking (`@Version`) to Lesson entity. This prevents double-moderation cleanly.
- **Code change required:** Yes (entity + migration)

---

## F. Database Assessment

### F.1 Schema Alignment

| Table | Flyway SQL | JPA Entity | Status |
|---|---|---|---|
| ACCOUNT | 6 columns | Matches | **PASS** |
| ROLE | 2 columns | Matches | **PASS** |
| USER_PROFILE | 5 columns | Matches | **PASS** |
| ACCOUNT_ROLE | Composite PK | `@ManyToMany` JoinTable | **PASS** |
| RADICAL | 8 columns | Matches | **PASS** |
| VOCABULARY | 11 columns | Matches | **PASS** |
| VOCAB_RADICAL | Composite PK | Junction table | **PASS** |
| LESSON | 6 columns + timestamps | Matches | **PASS** |
| LESSON_VOCABULARY | Composite PK + order_index | Matches with `@EmbeddedId` | **PASS** |
| USER_SRS_SETTING | 4 columns | Matches | **PASS** |
| CARD_PROGRESS | 8 columns | Matches | **PASS** |
| REVIEW_LOG | 9 columns | Matches | **PASS** |
| MODERATION_LOG | 7 columns | Matches | **PASS** |
| PERSONAL_NOTE | 5 columns | Matches | **PASS** |

### F.2 Constraints

| Constraint | Status |
|---|---|
| `uk_account_email_or_phone` UNIQUE | **PASS** |
| `chk_account_status` CHECK | **PASS** |
| `chk_lesson_status` CHECK | **PASS** |
| `chk_card_progress_item_type` CHECK | **PASS** |
| `chk_review_log_item_type` CHECK | **PASS** |
| `uk_card_progress_user_item` UNIQUE | **PASS** |
| `uk_vocab_hanzi_pinyin_raw` UNIQUE | **PASS** |
| `uk_lesson_order_index` UNIQUE | **PASS** |
| `uk_user_srs_setting_user` UNIQUE | **PASS** |
| Foreign key cascade behavior | **PASS** |
| InnoDB engine, utf8mb4_unicode_ci | **PASS** |
| Indexes on frequently queried columns | **PASS** |

### F.3 Flyway Audit

| Check | Status |
|---|---|
| Sequential versioning (V1, V2, V3) | **PASS** |
| Naming convention | **PASS** |
| No modifying applied migrations | **PASS** |
| `baseline-on-migrate: false` | **PASS** |
| `clean-disabled: true` (production-safe) | **PASS** |

### F.4 Polymorphic Reference

- **Design:** `CARD_PROGRESS` and `REVIEW_LOG` use `item_type` + `item_id` referencing either VOCABULARY or RADICAL.
- **DB enforcement:** CHECK constraint limits `item_type` to `('VOCABULARY', 'RADICAL')`. No physical FK on `item_id`.
- **App enforcement:** SrsServiceImpl validates existence in catalog tables before creating/updating progress (lines 167-175).
- **Classification:** **ACCEPTABLE TRADE-OFF** — The application-level invariant is correctly enforced, and the CHECK constraint prevents invalid item_type values. Orphan records could theoretically arise if a Vocabulary/Radical is deleted while CardProgress exists, but CASCADE on `user_id` FK and RESTRICT on Vocabulary deletion mitigate this. The design is documented in DECISIONS.md (DES-04).

---

## G. SRS Assessment

### G.1 Algorithm Correctness

| Check | Status | Evidence |
|---|---|---|
| Rating semantics (1=Again, 2=Hard, 3=Good, 4=Easy) | **PASS** | Mapped to SM-2 quality scores (0, 3, 4, 5) in `SrsCalculator.java`. |
| EF formula: `EF' = EF + (0.1 - (5-q) × (0.08 + (5-q) × 0.02))` | **PASS** | Implemented at line 171-174. Verified: q=5→+0.10, q=4→0.00, q=3→-0.14, q=0→-0.80. |
| Minimum EF: 1.30 | **PASS** | Enforced at line 176-178. |
| Default EF: 2.50 | **PASS** | `DEFAULT_EASE_FACTOR = new BigDecimal("2.50")`. |
| Interval progression: 1d → 6d → ceil(I×EF') | **PASS** | Lines 220-234. Rep 0→1d, Rep 1→6d, Rep ≥2→ceil(prev×EF). |
| Again resets repetitions to 0, interval to 0 | **PASS** | Lines 190-193, 215-218. |
| BigDecimal precision (scale 2) | **PASS** | All EF operations use `setScale(2, HALF_UP)`. |
| Integer overflow protection | **PASS** | `ceiled > Integer.MAX_VALUE` check at line 231. |

### G.2 Quota Logic

| Check | Status | Evidence |
|---|---|---|
| Daily review limit from USER_SRS_SETTING | **PASS** | Default 100 if no setting. |
| Quota checked in getDueCards() | **PASS** | Lines 94-105. |
| Quota NOT checked in reviewCard() | **FAIL** | See BE-CONC-002. |
| New cards per day tracked in settings | **PASS** | Default 20. Reported in StudyStats. |

### G.3 Date/Time

#### BE-SRS-001: LocalDate.now() Without Explicit Timezone
- **ID:** BE-SRS-001
- **Severity:** P3
- **Category:** SRS Correctness / Timezone
- **Component:** `SrsServiceImpl.java`
- **Evidence:** `LocalDate.now().atStartOfDay()` at line 98 uses the JVM's default timezone. The datasource URL specifies `serverTimezone=UTC`. If the JVM timezone differs from UTC, the "start of day" for quota counting may not align with the DB server's day boundary.
- **Why:** In production, JVM timezone and MySQL timezone could differ, causing reviews near midnight to be miscounted.
- **Expected:** Use `LocalDate.now(ZoneId.of("UTC")).atStartOfDay()` or configure JVM timezone explicitly.
- **Recommendation:** Use explicit timezone. Low severity for current local development.
- **Code change required:** Yes (minor)

---

## H. API Contract Assessment

Based on comparison of `.agents/API.md` with actual controller implementations:

| Endpoint | API.md | Actual | Status |
|---|---|---|---|
| POST /api/v1/auth/register | 201 Created | ✓ | **MATCH** |
| POST /api/v1/auth/login | 200 OK | ✓ | **MATCH** |
| GET /api/v1/radicals | Public, paginated | ✓ | **MATCH** |
| GET /api/v1/vocabulary | Public, paginated, searchable | ✓ | **MATCH** |
| GET /api/v1/lessons | Public, Approved only | ✓ | **MATCH** |
| POST /api/v1/creator/lessons | 201 Created | ✓ | **MATCH** |
| POST /api/v1/creator/lessons/import | Multipart | ✓ | **MATCH** |
| POST /api/v1/creator/lessons/import/confirm | 201 Created | ✓ | **MATCH** |
| POST /api/v1/creator/lessons/{id}/submit | 200 OK | ✓ | **MATCH** |
| POST /api/v1/moderator/lessons/{id}/approve | 200 OK | ✓ | **MATCH** |
| POST /api/v1/moderator/lessons/{id}/reject | 200 OK | ✓ | **MATCH** |
| GET /api/v1/srs/due | 200 OK | ✓ | **MATCH** |
| POST /api/v1/srs/review | 200 OK | ✓ | **MATCH** |
| GET /api/v1/srs/stats | 200 OK | ✓ | **MATCH** |
| CRUD /api/v1/notes | 200/201 | ✓ | **MATCH** |

> [!NOTE]
> No mismatches found between API.md and actual controller implementations.

---

## I. Performance Assessment

### I.1 N+1 Query Risks

#### BE-PERF-001: N+1 in getDueCards() Polymorphic Hydration
- **ID:** BE-PERF-001
- **Severity:** P2
- **Category:** Performance / N+1 Query
- **Component:** `SrsServiceImpl.getDueCards()`
- **Evidence:** Lines 125-133: For each due CardProgress, the code executes a separate `vocabularyRepository.findById()` or `radicalRepository.findById()`. With 100 due cards, this generates 1 (query for progress) + 100 (individual hydration queries) = 101 SQL statements.
- **Why:** Scales poorly as users accumulate more due cards. At 100 reviews/day, this causes 100+ DB queries per page load.
- **Expected:** Batch fetch using `findAllById()` after grouping by item_type, then map results.
- **Recommendation:** Collect all vocabIds and radicalIds, batch-fetch, then hydrate. This reduces 101 queries to 3.
- **Code change required:** Yes

#### BE-PERF-002: N+1 in getMyLessons() Vocabulary Count
- **ID:** BE-PERF-002
- **Severity:** P3
- **Category:** Performance / N+1 Query
- **Component:** `CreatorLessonServiceImpl.getMyLessons()`
- **Evidence:** Line 110: For each lesson in the page, a `countByLesson_LessonId()` query executes individually. With page size 20, this is 1 + 20 = 21 queries.
- **Why:** Minor for page size 20, but an unnecessary N+1 pattern.
- **Expected:** Use a JPQL query with `SELECT lesson, COUNT(lv)` group join.
- **Recommendation:** Low priority given typical page size.
- **Code change required:** Optional

### I.2 Pagination

| Check | Status |
|---|---|
| Pageable supported on list endpoints | **PASS** |
| Default page size (20) | **PASS** |
| Maximum page size cap | **UNVERIFIED** — Spring Data uses client-provided `size` param. No explicit max-page-size configuration found. A client could request `?size=10000`. |

---

## J. Test Quality Assessment

### J.1 Coverage Summary

| Category | Approximate Count | Status |
|---|---|---|
| Unit tests (MockitoExtension) | ~300 | **Strong** |
| Controller/MockMvc tests | ~250 (28 test classes) | **Strong** |
| Integration tests (@SpringBootTest) | ~150 (28 test classes) | **Good** |
| Repository tests (@DataJpaTest) | ~40 (9 test classes) | **Adequate** |
| Security/RBAC tests | ~35 (19 files) | **Good** |
| Concurrency tests | **0** | **GAP** |
| Transaction rollback tests | **0** | **GAP** |
| Database constraint tests | **~1** | **GAP** |

### J.2 Strong Coverage Areas
- ✅ Controller input/output validation (extensive MockMvc coverage)
- ✅ Role-based access control (RBAC) — dedicated `RbacSecurityIntegrationTests`
- ✅ JWT authentication flow (generation, validation, expiration)
- ✅ Lesson lifecycle state machine transitions
- ✅ SRS SM-2 algorithm correctness (dedicated `SrsCalculatorTests`)
- ✅ Excel parsing validation (format, headers, rows, duplicates)
- ✅ Negative security tests (unauthorized/forbidden access patterns)

### J.3 Coverage Gaps

#### BE-TEST-001: No Concurrency Tests
- **ID:** BE-TEST-001
- **Severity:** P2
- **Category:** Test Coverage Gap
- **Evidence:** No uses of `CountDownLatch`, `ExecutorService`, or multithreaded test patterns found in the test suite.
- **Missing coverage:** SRS double-review race, double-moderation, concurrent quota bypass.

#### BE-TEST-002: No Transaction Rollback Tests
- **ID:** BE-TEST-002
- **Severity:** P2
- **Category:** Test Coverage Gap
- **Evidence:** While tests use `@Transactional` for test isolation, no test explicitly verifies partial-failure rollback scenarios.
- **Missing coverage:** Excel import partial failure, moderation log failure rollback.

#### BE-TEST-003: No Dedicated Test Database Configuration
- **ID:** BE-TEST-003
- **Severity:** P1
- **Category:** Test Infrastructure
- **Evidence:** No `application-test.properties` or `application-test.yml` exists. No H2 or Testcontainers dependency in pom.xml. `@SpringBootTest` and `@DataJpaTest` tests appear to use the main MySQL database.
- **Why:** Tests running against the development MySQL database is a severe anti-pattern. Test data mutations could corrupt development data. Test execution requires a running MySQL instance, reducing portability.
- **Expected:** Either H2 in-memory database for tests, or Testcontainers for isolated MySQL.
- **Recommendation:** Add H2 dependency with test scope and `application-test.yml` with `spring.datasource.url: jdbc:h2:mem:testdb`.
- **Code change required:** Yes

> [!WARNING]
> **BE-TEST-003 is a significant infrastructure issue.** The fact that 775 tests pass suggests that either (a) tests run with a real MySQL that happens to be available, or (b) MockMvc and Mockito tests don't actually touch the DB. The Integration tests likely require a running MySQL instance. This limits CI/CD portability.

---

## K. External Standards Review

| Finding | Standard | Source | Relevant Principle |
|---|---|---|---|
| BE-AUTH-001 (Stale token) | OWASP API2:2023 | https://owasp.org/API-Security/editions/2023/en/0xa2-broken-authentication/ | Token revocation capability |
| BE-AUTH-002 (Hardcoded secret) | OWASP Secrets Management | https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html | No secrets in source code |
| BE-AUTHZ-001 (Moderator sees all) | OWASP API1:2023 | https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/ | Object-level access validation |
| BE-CONC-001 (Lost update) | MySQL InnoDB Isolation | https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html | REPEATABLE READ does not prevent lost updates on same-row concurrent writes |
| BE-UPLOAD-001 (No row limit) | OWASP API4:2023 | https://owasp.org/API-Security/editions/2023/en/0xa4-unrestricted-resource-consumption/ | Rate/resource limiting |
| BE-AUTH-003 (No iss claim) | RFC 7519 §4.1.1 | https://www.rfc-editor.org/rfc/rfc7519#section-4.1.1 | JWT issuer identification |

---

## L. Finding Register

| ID | Severity | Category | Component | Status |
|---|---|---|---|---|
| BE-AUTH-001 | P1 | Stale Token After Disable | JwtAuthenticationFilter | **CONFIRMED** |
| BE-AUTH-002 | P1 | Hardcoded JWT Secret | application.yml + JwtUtil | **CONFIRMED** |
| BE-TEST-003 | P1 | No Test Database Isolation | pom.xml + test config | **CONFIRMED** |
| BE-AUTHZ-001 | P2 | Moderator Views All Statuses | ModerationServiceImpl | **CONFIRMED** |
| BE-CONC-001 | P2 | SRS Lost Update Race | SrsServiceImpl | **CONFIRMED** |
| BE-CONC-002 | P2 | Daily Quota TOCTOU Bypass | SrsServiceImpl | **CONFIRMED** |
| BE-CONC-003 | P2 | Double Moderation Race | ModerationServiceImpl | **CONFIRMED** |
| BE-UPLOAD-001 | P2 | No Excel Row Count Limit | ExcelParserServiceImpl | **CONFIRMED** |
| BE-PERF-001 | P2 | N+1 in getDueCards | SrsServiceImpl | **CONFIRMED** |
| BE-TEST-001 | P2 | No Concurrency Tests | Test suite | **CONFIRMED** |
| BE-TEST-002 | P2 | No Transaction Rollback Tests | Test suite | **CONFIRMED** |
| BE-CORS-001 | P2 | No CORS Configuration | SecurityConfig | **CONFIRMED** |
| BE-UPLOAD-002 | P2 | Full Memory Workbook Load | ExcelParserServiceImpl | **ACCEPTABLE TRADE-OFF** |
| BE-AUTH-003 | P3 | No JWT Issuer Claim | JwtUtil | **CONFIRMED** |
| BE-SRS-001 | P3 | Timezone in Daily Quota | SrsServiceImpl | **CONFIRMED** |
| BE-PERF-002 | P3 | N+1 in getMyLessons | CreatorLessonServiceImpl | **CONFIRMED** |

---

## M. Top 10 Risks (Ranked by Practical Importance)

1. **BE-AUTH-002 (P1)** — Hardcoded JWT secret in source control. Trivially exploitable.
2. **BE-AUTH-001 (P1)** — Banned users retain access for up to 24 hours after ban.
3. **BE-TEST-003 (P1)** — Tests depend on real MySQL, limiting CI/CD portability.
4. **BE-CONC-001 (P2)** — SRS CardProgress lost update under concurrent reviews.
5. **BE-CONC-003 (P2)** — Double moderation creates duplicate audit log entries.
6. **BE-CORS-001 (P2)** — Frontend integration will be blocked without CORS.
7. **BE-UPLOAD-001 (P2)** — Unbounded Excel row count enables DoS via large files.
8. **BE-CONC-002 (P2)** — Daily review quota can be bypassed via concurrent requests.
9. **BE-PERF-001 (P2)** — N+1 queries in SRS due card hydration degrades performance.
10. **BE-AUTHZ-001 (P2)** — Moderators can view non-Pending lessons via direct ID access.

---

## N. Strengths

The following are genuinely strong aspects of the backend implementation, supported by evidence:

1. **Complete DTO isolation** — Zero entity leak verified across all 11 controllers. All responses use dedicated DTOs with `fromEntity()` factory methods. This is a textbook implementation of API boundary separation.

2. **Comprehensive GlobalExceptionHandler** — Handles 8 distinct exception types with appropriate HTTP status codes, consistent `ApiResponse` envelope, and no information leakage. One of the best-structured error handling implementations for this project scope.

3. **Well-designed SRS algorithm engine** — `SrsCalculator` is pure, stateless, framework-independent, deterministic, and thoroughly documented. Clean separation of mathematical logic from infrastructure concerns. Uses `BigDecimal` for precision.

4. **Correct lesson state machine enforcement** — Both `CreatorLessonServiceImpl` and `ModerationServiceImpl` enforce state transitions with explicit guard clauses. All invalid transitions are rejected with appropriate error codes.

5. **Defense-in-depth file upload validation** — Extension check → magic byte check → size limit → POI exception handling → encrypted file handling. Five layers of validation before processing begins.

6. **Ownership isolation pattern** — Every user-scoped service method resolves the current user from `SecurityContext` and validates ownership before mutation. No user can access another user's notes, SRS progress, or lessons.

7. **Proper JPA configuration** — `open-in-view: false`, `ddl-auto: none`, `FetchType.LAZY` on all associations, entity `equals`/`hashCode` following Hibernate best practices (id-based equality, class-level hashCode).

8. **Flyway-controlled schema** — Schema is exclusively managed by Flyway migrations. No Hibernate auto-DDL. Clean-disabled for safety. Proper CHECK constraints and indexes.

9. **775 passing tests with zero failures** — High test count with strong MockMvc and integration coverage for the feature set. Security-negative tests (unauthorized/forbidden) are well represented.

10. **Thin controller architecture** — All controllers are thin delegators. Business logic, validation, and authorization live exclusively in the service layer.

---

## O. Recommended Remediation Order

> [!CAUTION]
> **DO NOT IMPLEMENT.** This section is advisory only.

### P0 — None identified

### P1 — Fix Before Frontend

#### 1. BE-AUTH-002: Remove Hardcoded JWT Secret
- **What:** Remove default fallback secret from both `application.yml` and `@Value` annotation. Fail fast at startup without `JWT_SECRET` env var.
- **Why:** Current secret is publicly known from tutorials.
- **Affected files:** `application.yml`, `JwtUtil.java`
- **DB migration required:** No
- **API contract impact:** No
- **Regression tests:** Update test configuration to provide test secret explicitly.

#### 2. BE-AUTH-001: Account Status Check in JWT Filter
- **What:** Add a lightweight account status lookup in `JwtAuthenticationFilter` (optionally with short-TTL cache).
- **Why:** Prevents banned users from accessing the system for up to 24 hours.
- **Affected files:** `JwtAuthenticationFilter.java`, potentially new cache config.
- **DB migration required:** No
- **API contract impact:** No
- **Regression tests:** Add test for disabled account with valid JWT.

#### 3. BE-TEST-003: Add Test Database Configuration
- **What:** Add H2 test dependency and `application-test.yml`.
- **Why:** Enables CI/CD without external MySQL dependency.
- **Affected files:** `pom.xml`, new `src/test/resources/application-test.yml`
- **DB migration required:** No
- **API contract impact:** No

### P2 — Fix Selected Before or During Frontend

#### 4. BE-CORS-001: Add CORS Configuration
- **What:** Configure CORS with specific allowed origins in SecurityConfig.
- **Why:** Frontend integration will fail without CORS headers.
- **Affected files:** `SecurityConfig.java`
- **DB migration required:** No

#### 5. BE-CONC-001 + BE-CONC-003: Add Optimistic Locking
- **What:** Add `@Version` column to `CardProgress` and `Lesson` entities.
- **Why:** Prevents lost updates and double moderation.
- **Affected files:** `CardProgress.java`, `Lesson.java`, new Flyway migration.
- **DB migration required:** **Yes** (add `version` column)

#### 6. BE-UPLOAD-001: Add Excel Row Count Limit
- **What:** Add `MAX_ROWS = 5000` check in `processSheet()`.
- **Why:** Prevents resource exhaustion via large Excel files.
- **Affected files:** `ExcelParserServiceImpl.java`
- **DB migration required:** No

#### 7. BE-AUTHZ-001: Add Pending Status Filter
- **What:** Add status check in `getPendingLessonById()`.
- **Affected files:** `ModerationServiceImpl.java`

#### 8. BE-PERF-001: Batch Fetch in getDueCards
- **What:** Replace per-item `findById` with batch `findAllById`.
- **Affected files:** `SrsServiceImpl.java`

### P3 — Low Priority

#### 9. BE-AUTH-003: Add JWT Issuer Claim
#### 10. BE-SRS-001: Use Explicit Timezone
#### 11. BE-PERF-002: Optimize getMyLessons Count Queries

---

## P. Backend Readiness Score

| Dimension | Score (0-10) | Justification |
|---|---|---|
| **Correctness** | **8/10** | Core business logic (SRS, lesson lifecycle, moderation) is correct. Minor quota enforcement gap (TOCTOU). |
| **Security** | **6/10** | Strong authentication/authorization design, but hardcoded JWT secret (P1) and stale-token-after-ban (P1) are significant. |
| **Architecture** | **9/10** | Excellent layering, DTO isolation, thin controllers. No architectural violations found. |
| **Database Integrity** | **9/10** | Schema is well-designed with proper constraints, indexes, FKs, and CHECK constraints. Flyway is correctly configured. |
| **Transactions** | **8/10** | All multi-write operations have proper `@Transactional` boundaries. Rollback behavior is correct. |
| **Concurrency** | **5/10** | No optimistic/pessimistic locking on any entity. Multiple confirmed race condition risks. |
| **Performance** | **7/10** | Adequate for current scope. N+1 patterns exist but are manageable at current scale. |
| **Test Quality** | **7/10** | 775 tests with strong functional coverage. Missing concurrency, transaction rollback, and DB constraint tests. Test DB isolation is a P1 concern. |
| **Maintainability** | **9/10** | Clean code structure, consistent patterns, good Javadoc, clear error messages. Vietnamese error messages are user-friendly. |
| **Production Readiness** | **6/10** | Not deployable to production with hardcoded secret. Missing CORS for frontend. No health/readiness probes. |

---

## Q. Final Verdict

### **C. NEEDS TARGETED BACKEND FIXES BEFORE FE**

**Rationale:**

The backend implementation is architecturally sound, well-structured, and functionally correct across its documented scope. The 775-test suite provides strong coverage for business logic and security boundaries. The codebase demonstrates professional engineering practices: clean layering, DTO isolation, centralized error handling, and proper Flyway schema management.

However, **three P1 findings prevent immediate frontend integration:**

1. The hardcoded JWT secret makes the authentication system trivially bypassable by anyone who reads the source code.
2. The stale-token-after-ban vulnerability means account security controls are not immediately effective.
3. The test infrastructure coupling to a real MySQL database limits CI/CD portability and could cause issues with frontend integration testing.

Additionally, **CORS configuration (P2) is a hard blocker** for any frontend that runs on a different origin — which is the standard development setup.

The concurrency risks (P2) are real but not critical for the current single-learner scope. They should be addressed before any multi-user deployment.

---

## R. Recommended Next Step

### Fix P1 findings + CORS, then move to Frontend

1. **Fix BE-AUTH-002** (hardcoded secret) — 15 minutes
2. **Fix BE-AUTH-001** (stale token check) — 1-2 hours
3. **Fix BE-TEST-003** (add H2 test config) — 30 minutes
4. **Fix BE-CORS-001** (CORS configuration) — 30 minutes
5. Then **proceed to Phase 9 (Frontend)**
6. **Fix P2 concurrency findings** (optimistic locking) in parallel with or after initial frontend work

> [!IMPORTANT]
> Do NOT implement these fixes. Do NOT start Phase 9. Do NOT decide OQ-08/PEN-01. This audit report is the sole deliverable.

---

*End of Audit Report*
