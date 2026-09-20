# FRONTEND SKILLS CHANGELOG

> [!NOTE]
> **STATUS: HISTORICAL RECORD / AUDIT TRAIL**  
> **NOT CURRENT PROJECT CONTRACT**  
> This file tracks sequential version history and changes. Current binding contracts and invariants are defined in `PROJECT-CONTRACT.md` and `API.md`.

## [v2.2.5] - 2026-09-06

### 2.2.5 — Final Residual Contract Cleanup: R-01 to R-05 Resolution & Freeze Seal

**Methodology**: Systematic resolution of 5 verified residual contract items following adversarial review:

- **R-01 (ImportValidationReport fileStatus & Example Alignment)**: Confirmed from `ExcelParserServiceImpl.java:288-294` that backend emits `"INVALID"` (never `"INVALID_DATA"`) when data rows contain errors. Corrected `two-step-excel-import.md` and `PROJECT-CONTRACT.md`. Fixed example JSON in `two-step-excel-import.md` to resolve logical contradiction (`isValid: false`, `fileStatus: "INVALID"`, `invalidRowsCount: 2`, `validRowsCount: 23`, `totalRows: 25`).
- **R-02 (User Profile Roles Source of Truth)**: Verified `UserProfileResponse.java` contains NO `roles` property. Clarified in `PROJECT-CONTRACT.md`, `rbac-surfaces-and-layouts.md`, `auth-and-token-lifecycle.md`, and `user-profile-and-srs-settings.md` that user roles originate exclusively from `AuthResponse.roles` stored in client-side session state (`authManager`). `profile.html` must read roles from session state, not from `GET /api/v1/users/profile`.
- **R-03 (CORS Development vs Production & JWT Lifetime)**: Documented 24-hour token lifetime (`jwt.expiration-ms: 86400000`), lack of refresh-token / server logout endpoints, and clearly demarcated development CORS defaults (`http://localhost:5500`, `http://127.0.0.1:5500`, `http://localhost:3000`, `http://127.0.0.1:3000`) versus production deployment configuration via `APP_CORS_ALLOWED_ORIGINS` across `PROJECT-CONTRACT.md` and `auth-and-token-lifecycle.md`.
- **R-04 (`/srs/new-cards` Inventory & Parameter Constraints)**: Verified in `SrsController.java:59-80` that `GET /api/v1/srs/new-cards` requires mandatory query parameter `lessonId` (`Long`), while `GET /api/v1/srs/lessons/{lessonId}/new-cards` is a RESTful path-variable alias. Updated `api-contract.md` inventory table and endpoint details accordingly.
- **R-05 (Historical Audit Marking)**: Added explicit cautionary archive header and updated status badge in `skills/FRONTEND-SKILLS-AUDIT.md` designating it as an archived snapshot (v2.0.5) superseded by the active v2.2.5 baseline.

## [v2.2.4] - 2026-09-06

### 2.2.4 — Final Precision Cleanup & Freeze Preparation: Elimination of Stroke Drift, SRI Hash Alignment & Governance Refinement

**Methodology**: Adversarial audit remediation addressing all verified residual contract and documentation drifts across skills and references.

- **Elimination of Radical Stroke-Count Drift**: Completely removed all references to radical stroke-count filtering (1–17 strokes), `selectedStroke`, `#strokeFilters`, and `data-stroke` from `vanilla-js-architecture.md`, `rbac-surfaces-and-layouts.md`, and `product-identity-and-design-tokens.md`. Formally codified Invariant 8 in `PROJECT-CONTRACT.md`: Kangxi radicals in the backend database (`RADICAL`) and DTOs (`RadicalResponse`, `RadicalDetailResponse`) do NOT contain stroke count data; radical stroke filtering is categorized as `FUTURE / BLOCKED — NO AUTHORITATIVE BACKEND DATA SOURCE` and excluded from current frontend scope.
- **Subresource Integrity (SRI) Hash Alignment**: Updated the HTML initialization example in `vanilla-js-architecture.md` with official, verified cryptographic hashes from jsDelivr: Bootstrap 5.3.3 CSS (`integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH"`) and Bootstrap 5.3.3 JS (`integrity="sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz"`), along with `crossorigin="anonymous"`, eliminating contradictions with the security policy and Axis 9 review gate.
- **Personal Note Pagination in CURRENT_STATE.md**: Corrected outdated line 186 in `CURRENT_STATE.md` to reflect that `GET /api/v1/vocabularies/{vocabId}/notes` has been fully implemented with pagination returning `ApiResponse<PageResponse<PersonalNoteResponse>>` (`page`, `size` up to 100).
- **Fictional Lesson Description Removed**: Cleaned `rbac-surfaces-and-layouts.md` line 45 to remove `description` from the lesson editor header specification.
- **Requirement Governance & Taxonomy Refinement**: Updated `PROJECT-CONTRACT.md` Section 4 with a 5-tier request classification system (`VALID CURRENT-STATE REQUEST`, `CONTRACT CHANGE REQUEST`, `ARCHITECTURE CHANGE REQUEST`, `DATABASE CHANGE REQUEST`, `SECURITY-SENSITIVE CHANGE`) and clear defect taxonomy (`SECURITY-VIOLATION`, `ACCESSIBILITY-NONCOMPLIANT`, `CONTRACT-VIOLATION`, `ARCHITECTURE-VIOLATION`, `QUALITY-ISSUE`), replacing rigid refusal rules with a structured 6-step change impact protocol.

## [v2.2.3] - 2026-09-06

### 2.2.3 — Frontend Skill Contract Remediation: Zero-Drift Alignment with Executable Java Backend

- **P0-1 Fix — `lessonDetail.lessonId` Routing Contract**: Fixed redirect pattern in `two-step-excel-import.md` to use `lessonDetail.lessonId` instead of `lessonDetail.id`, matching `LessonDetailResponse.java` (`getLessonId()`).
- **P0-2 Fix — Vocabulary Search Parameter**: Replaced `/vocabulary?q=` with `/vocabulary?search=` in `ui-states-and-forms.md`, strictly adhering to `VocabularyController.java` (`@RequestParam(required = false) String search`).
- **P0-3 Fix — 4-Tier Role Layer Distinction & JSON Contract**: Replaced all frontend `ROLE_ADMIN` checks with JSON role `"Admin"`. Documented 4-layer role distinction table across `auth-and-token-lifecycle.md`, `PROJECT-CONTRACT.md`, `state-and-event-management.md`, `rbac-surfaces-and-layouts.md`, and `playwright-e2e-patterns.md`: Database (`Admin`), JSON DTO (`["Admin", "Creator", "Moderator", "Learner"]`), Spring Security internal (`ROLE_Admin`), and Frontend Client check (`roles.includes('Admin')`).
- **P0-4 Fix — Rate Limiting Semantics**: Eliminated fictional `Retry-After` header references from `central-api-client.md`, `api-contract.md`, and `PROJECT-CONTRACT.md`. Documented authentic `GlobalExceptionHandler` error code `TOO_MANY_REQUESTS` (HTTP 429) without `Retry-After` header.
- **P1-1 Fix — DueCardResponse Payload Boundaries**: Formally documented in `api-contract.md`, `srs-learning-interaction.md`, and `PROJECT-CONTRACT.md` that `DueCardResponse.strokeCount` is hardcoded explicit `null` on the backend for both Vocabulary and Radical. Prohibited frontend agents from designing stroke filters with this field. Documented that rich media fields (`audioUrl`, `videoWritingUrl`, `pinyinRaw`, `radicals[]`) are omitted from due cards and must be fetched via individual detail endpoints.
- **P1-2 Fix — Lesson Request DTO Verification**: Verified that `CreateLessonRequest` contains only `title`, `excelFileUrl`, `vocabularyIds`, and `UpdateLessonRequest` contains only `title` and `excelFileUrl`. Zero `description` field exists.
- **P1-3 Fix — Personal Note Contract Alignment**: Verified pagination default size (20), max size (100), and confirmed that `DELETE /api/v1/notes/{id}` returns HTTP 200 OK with `ApiResponse<Void>` (not 204).
- **P1-4 Fix — Excel Import 2-Step Workflow**: Fully documented in `two-step-excel-import.md` and `PROJECT-CONTRACT.md` that Excel preview does not persist domain data, returns HTTP 200 OK with `fileStatus: "INVALID"` (`isValid: false`) on validation failure (note: interim changelog typo `"INVALID_DATA"` corrected to `"INVALID"` in v2.2.5), enforces `MAX_DATA_ROWS = 5000`, 10MB file limit (HTTP 413), title constraint difference (manual create $\le 200$ vs Excel confirm $\le 100$), and normalized header aliases.
- **P1-5 Fix — Admin Catalog Exploration**: Eliminated all fictional `GET /api/v1/admin/radicals` and `GET /api/v1/admin/vocabulary` endpoints across `rbac-surfaces-and-layouts.md`, `api-contract.md`, `PROJECT-CONTRACT.md`, and `API.md`. Documented that Admin browses master catalogs via public `GET /api/v1/radicals` and `GET /api/v1/vocabulary?search=`.
- **P1-6 Fix — Vocabulary Ordering Contract**: Documented that `POST /api/v1/creator/lessons/{id}/vocabularies/{vocabId}` automatically appends vocabulary with `nextOrderIndex` and does not accept `orderIndex` from client. Reordering is handled via `/{id}/reorder` supporting `orderedVocabIds` or `items`.
- **Standardized Canonical `parseFieldErrors()`**: Unified `parseFieldErrors(rawErrors)` across `ui-states-and-forms.md` and `api-contract.md` into a single canonical signature returning `Array<{ field: string|null, message: string }>` alongside companion `groupFieldErrors(rawErrors)` returning `Record<string, string[]>`.
- **API.md Full Audit**: Overwrote `.agents/API.md` cleanly to eliminate historical planned DTO drift (`RadicalDto`, `VocabularyDto`, `LessonDto`, `PersonalNoteDto`, etc.) and synchronized with current Java DTOs and controller routes.

## [v2.2.2] - 2026-09-06

### 2.2.2 — Final Contract Correctness Pass: Success Code Semantics, DTO Nullability Partition & Authority Adjudication

**Methodology**: Evidence-driven verification of executable Java backend source, integration tests, and `.agents/` documentation against remaining factual inconsistencies.

- **Authoritative 201 Success Code Semantics**: Verified from Java source (`AuthController.java`, `PersonalNoteController.java`, `CreatorLessonController.java`, `AdminRadicalController.java`, `AdminVocabularyController.java`, and `ApiResponse.java`) and integration tests (`AuthIntegrationTests.java:88`, `PersonalNoteIntegrationTests.java:179`, `CreatorLessonIntegrationTests.java:253`, `CatalogIntegrationTests.java:216`) that all HTTP 201 responses return `code: "SUCCESS"`. Clarified in `API.md` table 1.4 that `CREATED` in `ErrorCode.java` is an unused enum value in current controller implementations. Documented the invariant in `central-api-client.md` and `api-contract.md`.
- **DTO Nullability Partition Recomputed from Source**: Physically inspected all 22 response DTOs under `backend/src/main/java/com/elearning/dto/response/` and confirmed exact partition: 13 classes with `@JsonInclude(NON_NULL)` (`AccountResponse`, `AuthResponse`, `LessonDetailResponse`, `LessonSummaryResponse`, `LessonVocabItemResponse`, `ModerationLogResponse`, `ModerationQueueResponse`, `RadicalDetailResponse`, `RadicalResponse`, `RoleResponse`, `UserProfileResponse`, `VocabularyDetailResponse`, `VocabularyResponse`) vs 9 explicit null classes (`ApiResponse`, `DueCardResponse`, `ImportValidationReport`, `PageResponse`, `ParsedVocabularyItem`, `PersonalNoteResponse`, `RowValidationError`, `StudyStatsResponse`, `UserSrsSettingResponse`). Synchronized into `PROJECT-CONTRACT.md` (Invariant 7) and `api-contract.md`.
- **Contract Authority & Terminology Reconciled**: Formally adjusted contract scope definitions in `PROJECT-CONTRACT.md` and `api-contract.md` to *"Canonical frontend-facing contract derived from executable backend evidence. (Executable Java source code remains the primary authority)"*, preventing any ambiguity regarding documentation precedence over executable code.
- **Contextual URL Security Precision**: Updated `xss-and-dom-safety.md` to reflect OWASP ASVS 5.0 (V5.1.4, V5.2.3) and WHATWG URL Standard principles; replaced flaky regex anchors with robust `.startsWith()` checks; handled `data:` URIs contextually (safe raster images allowed in resource sinks, all data URIs rejected in navigation sinks); strictly prohibited executable schemes (`javascript:`, `vbscript:`).
- **HTTP Status Evidence Audited**: Formally mapped all 10 project status codes (400, 401, 403, 404, 409, 413, 415, 422, 429, 500) to specific backend exception handlers, controller endpoints, and trigger conditions with corresponding integration test citations.

## [v2.2.1] - 2026-09-06

### 2.2.1 — Final Governance Precision Cleanup: Overclaims, Toolchain Decoupling & Contextual Invariants

**Methodology**: Repository-wide adversarial pass targeting remaining contradictions, overclaims, and overly rigid skill constraints across all `.agents/` documents.

- **API Envelope Contradiction Resolved**: Eliminated all blanket statements claiming "All business endpoints return `ApiResponse<T>`". Replaced with canonical precedence: *"Most business API responses use `ApiResponse<T>`. Endpoint-specific response behavior takes precedence. HTTP 204 responses contain no body and therefore do not contain `ApiResponse<T>`."* Updated `api-contract.md`, `PROJECT-CONTRACT.md`, `API.md`, `contract-semantics.md`, `central-api-client.md`, and `ARCHITECTURE.md`.
- **ARIA-Expanded APG Alignment**: Removed restrictive fixed role lists; aligned `accessibility-and-responsive.md` and `frontend-ui-engineering/SKILL.md` to WAI-ARIA APG patterns (used only when the applicable pattern supports an expandable state and controls collapsible content; forbidden on generic containers).
- **Browser Testing Toolchain Decoupling**: Decoupled `browser-testing-with-devtools` and `playwright-e2e-patterns.md` from mandatory npm/Playwright toolchains. Established that Playwright is conditional on repository setup; for zero-build Vanilla JS, testing relies on Chrome DevTools, browser automation, or MCP inspection without installing arbitrary npm packages. Removed Vite/Node static server as mandatory input.
- **Defensible WCAG Conformance Language**: Replaced absolute claims ("fully complies with WCAG 2.2 AA", "zero WCAG violations") with defensible verification standards: *"Design and verify against applicable WCAG 2.2 Level AA success criteria."* Automated tooling is treated as supporting evidence, not proof of complete conformance.
- **Context-Sensitive Scrollable Table Rule**: Refactored `.table-responsive` table rules in `accessibility-and-responsive.md` and `frontend-ui-engineering/SKILL.md`. Mandated `tabindex="0"` and `role="region"` ONLY when a scrollable table contains pure static text, avoiding redundant and disorienting tab stops when child focusable controls already exist.
- **Keyboard Rules Precision**: Refined generic keyboard rules to ensure native controls retain default behaviors, custom widgets follow APG patterns, and the Escape key is required only on dismissible overlays/dialogs.
- **Dynamic CSS & innerHTML / escapeHtml Precision**: In `xss-and-dom-safety.md`, explicitly differentiated safe dynamic CSS (`classList`, constrained properties, custom properties) from dangerous string injections (`cssText`, style blocks). Explicitly documented that `escapeHtml()` has a narrow entity-encoding scope and is NEVER a substitute for safe DOM rendering (`textContent`, `createElement`, `replaceChildren`).
- **Contextual URL Sanitization**: Distinguished navigation links (`href`) supporting `mailto:`/`tel:` from resource sinks (`src`) which disallow communication schemes, while strictly rejecting executable schemes (`javascript:`, `vbscript:`, `data:text/html`).
- **Surface & View Inventory Architecture**: Clarified that the 23-view inventory in `rbac-surfaces-and-layouts.md` represents a functional reference map of user surfaces and endpoint bindings, NOT an immutable requirement for 23 physically distinct `.html` files.
- **Stale DTO Names in ARCHITECTURE.md**: Replaced remaining historical references to `RadicalDto`, `VocabularyDto`, and `LessonDto` in `ARCHITECTURE.md` with canonical `RadicalResponse`, `VocabularyResponse`, and `LessonDetailResponse`.

## [v2.2.0] - 2026-09-06

### 2.2.0 — Pre-Frontend Comprehensive Governance, Canonical Contract Reconstruction & 12-Section Skill Standardization

**Methodology**: Exhaustive adversarial audit against all 15 Java controller classes, 112 test classes, 19 request DTOs, 22 response DTOs, `SecurityConfig`, `JwtAuthenticationFilter`, `GlobalExceptionHandler`, and Flyway migrations V1..V7.

- **Canonical Frontend API Contract (`api-contract.md`)**: Created complete, single-source-of-truth reference at `.agents/skills/frontend-api-integration/references/api-contract.md` covering all 48 Java handler methods / 49 HTTP method+path mappings (+ 1 Actuator health probe), with exact request/response schemas, path variable types (`Integer` for Kangxi radicals 1..214 vs `Long` for entities), status codes, error handling, and nullability flags.
- **Precision Distinction of Handlers vs Mappings**: Reconciled that the system has **48 Java controller handler methods** and **49 HTTP method+path mappings** due to dual `@RequestMapping(value = "/{id}/reorder", method = {RequestMethod.PUT, RequestMethod.POST})` on `CreatorLessonController.reorderVocabulary`. Clarified in `API.md` and `PROJECT-CONTRACT.md`.
- **HTTP 204 No Content Handling**: Explicitly codified in `central-api-client.md` and `api-contract.md` that `DELETE /api/v1/admin/radicals/{id}`, `DELETE /api/v1/admin/vocabulary/{id}`, and `DELETE /api/v1/creator/lessons/{id}` return HTTP 204 No Content with an empty body. Central API client must inspect `response.status === 204` before calling `response.json()` to prevent fatal JSON parse exceptions.
- **DTO Nullability Contract Reconciled**: Formally categorized `@JsonInclude(NON_NULL)` DTOs (13 classes where absent fields imply null) vs explicit null DTOs (9 classes including `PageResponse` and `DueCardResponse`). Codified safe nullish coalescing (`??`) and property access for frontend agents.
- **URL Whitelist Protocol Hardening**: In `vanilla-js-dom-security/references/xss-and-dom-safety.md`, replaced fragile blacklist regular expressions with strict protocol whitelist (`http:`, `https:`, relative `/`, `#`, `mailto:`, `tel:`), completely eliminating DOM XSS bypass vectors (`javascript:`, `data:text/html`).
- **Memory-Only Token Trade-Off & Client Logout Semantics**: Documented in `auth-and-token-lifecycle.md` why `localStorage` is required for Vanilla JS Multi-Page Applications without cookie refresh endpoints, while documenting all security trade-offs, OWASP ASVS mitigation baselines, and distinguishing client-side credential cleanup from server-side `authorization_version` token revocation.
- **DTO Drift Fixes in Documentation**: Synchronized `.agents/API.md` by replacing outdated names (`RadicalDto`, `VocabularyDto`, `LessonDto`) with current canonical DTOs (`RadicalResponse`, `VocabularyResponse`, `LessonDetailResponse`), removing fictional `description` field from `CreateLessonRequest`/`UpdateLessonRequest`, and enumerating explicit CRUD endpoints for Admin Radicals and Vocabularies.
- **Full 12-Section Restructuring for All 5 Frontend Skills**: Standardized `frontend-api-integration`, `frontend-ui-engineering`, `vanilla-js-dom-security`, `browser-testing-with-devtools`, and `frontend-code-review` to strictly follow all 12 quality sections (PURPOSE, WHEN TO USE, REQUIRED INPUTS, SOURCE OF TRUTH, NON-NEGOTIABLE RULES, WORKFLOW, SECURITY CONSTRAINTS, ACCESSIBILITY CONSTRAINTS, VERIFICATION, FAILURE CONDITIONS, WHAT NOT TO DO, REFERENCES).


### 2.1.0 — Source-Verified Endpoint Inventory, Missing Feature Documentation & Cross-Document Synchronization

**Methodology**: All changes verified by directly opening all 15 Java controllers (48 handler methods confirmed via grep), all request/response DTOs, `GlobalExceptionHandler.java`, and integration test assertions (`AuthIntegrationTests.java:89` confirming `$.data.token`).

- **User Profile API Contract (previously MISSING)**: Created `frontend-api-integration/references/user-profile-and-srs-settings.md` documenting `GET/PUT /api/v1/users/profile` (`UserProfileResponse` fields: `userId`, `accountId`, `emailOrPhone`, `fullName`, `avatarUrl`, `createdAt`, `updatedAt`) and `GET/PUT /api/v1/srs/settings` (`UserSrsSettingResponse` fields: `settingId`, `newCardsPerDay`, `maxReviewPerDay`). All field names verified against Java DTO getter methods. Noted `@JsonInclude(NON_NULL)` behavior on `UserProfileResponse`.
- **ARCHITECTURE.md errors[] Format Fix (CRITICAL)**: Line 80 had `errors: [{field, message}]` — corrected to `errors: ["${field}: ${message}", ...]` (`List<String>`). This was identified in `FRONTEND-SKILLS-AUDIT.md` v2.0.5 but never propagated to `ARCHITECTURE.md` due to scope limitation.
- **AuthResponse JSON Serialization Clarification**: Added `AUTHORITATIVE` warning in `PROJECT-CONTRACT.md` that `@JsonAlias("accessToken")` only affects deserialization — the serialized JSON field is `"token"` not `"accessToken"`. Verified by `AuthIntegrationTests.java:89`: `jsonPath("$.data.token").isString()`.
- **Endpoint Count Correction (48, not 46/47)**: Fixed all documents claiming 46 or 47 endpoints to the verified count of 48 handler methods. Affected files: `PROJECT-CONTRACT.md`, `CHANGELOG.md`, `ROADMAP.md` (lines 647, 850), `PROGRESS.md` (line 246).
- **Complete HTML Site Map**: Added Section 6 to `rbac-surfaces-and-layouts.md` with exhaustive site map covering all 23+ HTML pages across Public, Learner, Creator, Moderator, and Admin surfaces, mapping each page to its API endpoints.
- **Missing Pages Added**: `profile.html`, `srs-settings.html`, `login.html`, `register.html`, `index.html` added to site map (were previously absent).
- **SKILL.md Reference Link**: Added `user-profile-and-srs-settings.md` to `frontend-api-integration/SKILL.md` references list.
- **Learner Surface Updated**: Added User Profile and SRS Settings to `PROJECT-CONTRACT.md` RBAC learner surface description.

### Unchanged Files (intentionally not modified)
- `FRONTEND-SKILLS-AUDIT.md` — its mention of `{field, message}` is in a "what was wrong" column describing the old bug, which is historically accurate.
- All other reference files in `frontend-ui-engineering/`, `vanilla-js-dom-security/`, `browser-testing-with-devtools/`, `frontend-code-review/` — reviewed, no errors found related to this session's scope.

### Conditions for Continued Trust
- Endpoint count (48) is accurate as of the current backend code. If any controller is added/removed, re-run `Select-String -Pattern "@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@RequestMapping.*method"` on all controller files.
- `UserProfileResponse` has `@JsonInclude(NON_NULL)` — if this annotation is removed, the FE must be updated to handle explicit `null` fields.
- `AuthResponse.token` serialization depends on Jackson's default getter-based naming. If `@JsonProperty("accessToken")` is added, FE must switch to `response.data.accessToken`.

## [v2.0.5] - 2026-09-05

### 2.0.5 — Real Backend Source Reconciliation & Safety-Net Governance
- **Mandatory Source-Code First Rule (`PROJECT INVARIANT`)**: Enforced in `frontend-api-integration/SKILL.md` that all frontend implementation agents MUST open and inspect actual Spring Boot Java Controllers (`backend/.../controller/*Controller.java`) and Request/Response DTOs (`backend/.../dto/*`) before writing any API calls, establishing Java source code as the sole authoritative single source of truth.
- **Two-Step Excel Import API Alignment**: Reconciled the import workflow across `PROJECT-CONTRACT.md`, `two-step-excel-import.md`, and `frontend-api-integration/SKILL.md`:
  - Step 1 (Preview): Corrected route to `POST /api/v1/creator/lessons/import` (multipart `file`), returning `ApiResponse<ImportValidationReport>` with real fields (`isValid`, `totalRows`, `validRowsCount`, `invalidRowsCount`, `newVocabCount`, `existingVocabCount`, `fileStatus`, `summaryMessage`, `rows`, `errors`).
  - Step 2 (Confirm): Corrected route to `POST /api/v1/creator/lessons/import/confirm` consuming `multipart/form-data` (`FormData` with `file` and `title`), creating the lesson and associated vocabulary atomically.
- **Validation Error Contract Parsing**: Aligned `PROJECT-CONTRACT.md`, `ui-states-and-forms.md`, `central-api-client.md`, and `contract-semantics.md` with Spring Boot's `GlobalExceptionHandler.java`, which returns `errors` as `List<String>` formatted as `"${field}: ${message}"`. Added `parseFieldErrors()` helper to split on delimiter `:` and properly populate `.is-invalid` inputs and `.invalid-feedback` containers.
- **JWT Version Claim Name Correction**: Replaced the erroneous claim name `ver` with `auth_ver` in `PROJECT-CONTRACT.md` and `auth-and-token-lifecycle.md` to match `JwtUtil.CLAIM_AUTH_VER = "auth_ver"` and `JwtAuthenticationFilter.java`.
- **Audit De-Inflation & Transparency**: Updated `FRONTEND-SKILLS-AUDIT.md` (Section 18.9 and Section 19) to clarify that `validate_skills.ps1` only checks YAML frontmatter and link syntax, formally retracting previous false claims of having verified fictional endpoints.

## [v2.0.4] - 2026-09-05

### 2.0.4 — Post-adversarial audit corrections & freeze-ready consolidation
- **DOM XSS Elimination in Example Code**: Refactored `setComponentState` and `displayFieldErrors` in `ui-states-and-forms.md` to eliminate template literal variable interpolation into `innerHTML`; replaced unsafe raw string `options.actionHtml` with safe structured button definition `actionBtn: { text, onClick, className }`; rendered dynamic error text via `textContent` on a dedicated `<span>`.
- **ARIA Expanded Semantics & Flashcard Correction**: Removed `aria-expanded="false"` from generic `<div class="flashcard">` in `srs-learning-interaction.md`; added Section 2.9 to `accessibility-and-responsive.md` establishing that `aria-expanded` and `aria-controls` belong strictly on interactive disclosure controls (`<button>`), not generic containers or 3D cards.
- **Event Listener Teardown Pattern**: Wrapped review hotkey binding in `initReviewKeyboardControls(signal = null)` in `srs-learning-interaction.md`, supporting native `AbortSignal` for clean unbind upon view navigation.
- **Unified Race Condition Mitigations**: Demonstrated unified search-as-you-type pattern combining `AbortController` transport cancellation with monotonic `latestSearchRequestId` sequence token verification in `ui-states-and-forms.md` and `state-and-event-management.md`.
- **WCAG 2.2 Level AA Normative Criteria Additions**: Added explicit normative sections in `accessibility-and-responsive.md` for **SC 1.4.11 Non-text Contrast (Level AA)** ($\ge 3:1$ for UI boundaries and icons) and **SC 2.4.1 Bypass Blocks (Level A)** (Skip to main content link `<a href="#mainContent">`).
- **Cross-File Consistency Harmonization**: Aligned heading hierarchy and target size criteria across `frontend-ui-engineering/SKILL.md`, `browser-testing-with-devtools/SKILL.md`, and `frontend-review-checklist.md`.
- **Obsolete Contract Elimination in `contract-semantics.md`**: Replaced outdated Spring Data `Page` schema (`content`) with authoritative `PageResponse<T>` (`items`, `page`, `size`, `totalElements`, `totalPages`) and `ApiResponse<T>`.

## [v2.0.3] - 2026-09-05

### 2.0.3 — Final source-verified accessibility, semantics and governance corrections
- **Focus Standard Classification (WCAG 2.4.7 vs 2.4.13)**: Disentangled Level AA Focus Visible (SC 2.4.7, requiring visible focus) from Level AAA Focus Appearance (SC 2.4.13, defining quantitative area and 3:1 contrast against unfocused states); retained 3:1 contrast focus rings as an Engineering Recommendation without mischaracterizing it as an SC 2.4.7 Level AA requirement.
- **Headings Accessibility vs Project Convention**: Separated normative WCAG SC 1.3.1/2.4.6 requirements (headings reflect logical hierarchy, no downward skips) from the Project Convention preferring one primary `<h1>` per view/page.
- **Target Size Exceptions & Usability Targets**: Re-anchored SC 2.5.8 to its normative 24×24 CSS px baseline and W3C spacing exception (24 CSS px diameter circle test); distinguished from the $44\times 44$ px project touch usability target and Level AAA SC 2.5.5.
- **Audio Pronunciation Semantics & aria-pressed**: Corrected `aria-pressed` usage to apply strictly to controls with toggle semantics (e.g. Mute/Unmute); prohibited using `aria-pressed="true"` on momentary audio pronunciation play buttons.
- **Character Key Shortcuts (SC 2.1.4)**: Distinguished normative WCAG SC 2.1.4 (turn off, remap, or active on focus) from the Project UX Rule requiring visible pointer/touch equivalents, and Project Invariants shielding hotkeys from editable controls (`input`, `textarea`, `select`, `[contenteditable]`).
- **Icon Role Differentiation & Duplication Avoidance**: Differentiated meaningful standalone icons (requiring accessible names) from decorative icons inside buttons (where accessible names belong to the parent `<button>` and inner icons are marked `aria-hidden="true"` to prevent duplicate announcements).
- **Authoritative REST DTO Contract (`PageResponse<T>`)**: Established that the project's actual serialized REST JSON contract (`page`, `size`, `totalElements`, `totalPages`, `items`) is authoritative over internal Spring Data `Page` class method names.
- **Async Race Condition Scope & Limits**: Clarified that `AbortController` cancels transport requests but does not alone prevent all race conditions; codified combining cancellation with request sequence tokens (`latestRequestId`) or query verification.
- **Pragmatic Vanilla JS DOM Teardown**: Clarified that Vanilla JS is not a pseudo-framework; resource cleanup applies exclusively when dynamic, long-lived resources (global listeners, timers, active observers, object URLs) are actually allocated.
- **Playwright Auth State Security & Git Exclusion**: Mandated that sensitive `storageState` JSON files must never be committed to Git; required adding `e2e/.auth/` to `.gitignore` and generating auth states dynamically during test setup.
- **HTTP/HTTPS Origin Enforcement for Browser Testing**: Codified that primary integration testing must run on a valid local HTTP/HTTPS origin, strictly barring `file://` due to ES module blocking, CORS failures (`Origin: null`), and storage scoping issues.
- **Contextual Anti-Template & Non-Mandatory Cultural Motifs**: Affirmed that anti-template governance is contextual and objective (no mechanical radius/gradient bans); affirmed that Chinese cultural motifs are optional inspiration and NEVER mandatory grounds for PR rejection.
- **Normative vs Project Governance Taxonomy**: Categorized all skill rules into explicit tiers: `NORMATIVE`, `PROJECT INVARIANT`, `PROJECT CONVENTION`, `ENGINEERING RECOMMENDATION`, `UX RESEARCH`, and `COMMUNITY REFERENCE`.
- **Source Verification Record**: Appended Section 17 to `FRONTEND-SKILLS-AUDIT.md` verifying all changes against primary sources.

## [v2.0.2] - 2026-09-05

### 2.0.2 — Targeted quality patch with source-governed research
- **1. Heading Structure**: Codified W3C WCAG SC 1.3.1 & 2.4.6 heading rules: single `<h1>` per view, hierarchical tree without downward skips; affirmed upward stepping (e.g. `h3` -> `h2`) is structurally valid; decoupled semantic heading tags from visual size via CSS tokens.
- **2. Target Size**: Codified W3C WCAG SC 2.5.8 normative $24\times 24$ CSS px minimum (with spacing circle offset & inline exceptions); distinguished it from mobile ergonomic touch target guidelines (~$44\times 44$ px / Level AAA / iOS & Android human interface guidelines).
- **3. Focus Accessibility**: Enforced WCAG 2.2 focus suite: SC 2.4.7 visible focus indicators (`:focus-visible`), SC 2.4.3 logical focus order, SC 2.1.2 no keyboard traps, SC 2.4.11 focus not obscured (`scroll-margin-top` for sticky headers), and modal focus trap with return-on-close.
- **4. Icon-Only Controls**: Enforced native semantic `<button>`/`<a>` elements; mandated accessible names (`aria-label` or `.visually-hidden`); required `aria-hidden="true"` on decorative glyphs; barred unsemantic clickable `<div>`/`<span>` without ARIA roles and keyboard handlers.
- **5. Non-Text Content & Audio Accessibility**: Enforced informational images require concise `alt` text; decorative images use `alt=""` or `role="presentation"`; avoided redundant alt text ("photo of..."); required accessible names and visual state indicators on audio pronunciation controls.
- **6. Form Field Error Association**: Mandated explicit association of form inputs with error messages via `aria-describedby`; required setting `aria-invalid="true"` on error and clearing on correction; required multi-modal error cues (text + icon) to avoid relying on color alone (SC 1.4.1).
- **7. Async Stale Overwrite Prevention**: Established invariant that older async responses must never overwrite newer UI state; documented approved patterns: `AbortController`, request sequence token (`latestRequestId`), and active-query verification.
- **8. Server-Authoritative Pagination / Filter / Sort**: Mandated that backend `PageResponse` metadata is authoritative; prohibited inferring totals from client array length; enforced page reset to 0 on filter changes; codified URL deep linking with `URLSearchParams`.
- **9. Vanilla JS Resource Lifecycle Cleanup**: Provided complete teardown checklist and code patterns for global event listeners (via `AbortSignal`), interval/timeout timers, in-flight fetch requests, DOM observers (`MutationObserver`, `IntersectionObserver`, `ResizeObserver`), object URLs (`revokeObjectURL`), and detached DOM nodes.
- **10. SRS Flashcard Hotkey Safety**: Guarded study hotkeys (1-4, Space) from firing when focus is inside text editable controls (`input`, `textarea`, `select`, `[contenteditable]`); required pointer/touch equivalents for all keys; preserved browser shortcuts; active only on card BACK.
- **11. Flashcard Logical State vs Visual Transform**: Decoupled visual CSS `rotateY(180deg)` transforms from application state; mandated explicit logical state `{ side: 'FRONT' | 'BACK', isSubmitting: boolean }`; derived all presentation and ARIA attributes from logical state.
- **12. Playwright Navigation & Session Lifecycle Testing**: Added Section 3 to Playwright testing patterns covering page reload (`page.reload()`), history navigation (`goBack()`/`goForward()`), deep linking, 401 handling, DEC-42 server-side token revocation, and multi-tab synchronization with executable test specs.
- **13. HTTP/HTTPS Origin Enforcement**: Mandated local HTTP/HTTPS server origins for all browser integration testing; strictly barred `file://` due to ES module blocking, cross-origin API CORS failures, and storage inconsistencies.
- **14. Purposeful Decoration & Non-Mandatory Cultural Motifs**: Permitted and encouraged purposeful visual ornaments (hierarchy, wayfinding, pedagogical cues, branding, feedback); prohibited meaningless clutter; established that Chinese cultural motifs are optional and NEVER mandatory grounds for PR rejection.
- **15. Shared Component Architecture & Taxonomy**: Established strict taxonomy distinguishing Shared Primitives (`components/common/`) from Domain Components (`components/domain/`); codified governance against duplicate copy-paste UI chunks and domain logic leakage into generic components.
- **Source Basis**: Added explicit, authoritative source basis sections (W3C WCAG 2.2, WAI-ARIA 1.2, WHATWG HTML/DOM, MDN, OWASP, Playwright official, Bootstrap 5.3, NN/g) across all 11 modified reference files.

## [v2.0.1] - 2026-09-05

### 2.0.1 — Targeted technical wording and governance correction
- **1. window.__ENV__ Scope**: Explicitly restricted `window.__ENV__` to read-only deployment configuration; prohibited storing application, authentication, user, or page state on `window`, and clarified it is not a security boundary.
- **2. Fetch Network Error Normalization**: Replaced non-portable string checking (`err.message.includes('fetch')`) with Fetch API standard error types (`TypeError` vs `AbortError` vs `TimeoutError`).
- **3. Accurate GET Retry Semantics**: Clarified that only GET requests may be retried (max 1), strictly for transient transport-level failures; explicitly barred retrying 4xx/5xx responses, 401/auth failures, aborts, cancellations, or validation errors.
- **4. WCAG SC 3.3.8 Wording Precision**: Corrected accessible authentication wording so paste and password autofill support is recognized as a critical usability requirement without claiming it single-handedly proves SC 3.3.8 conformance.
- **5. Hanzi Contrast Discipline**: Clarified that Hanzi is a writing system rather than a size category; the 3:1 threshold applies exclusively when text meets the WCAG definition of large-scale text, while normal body Hanzi requires $\ge 4.5:1$.
- **6. Contextual Anti-Template Governance**: Replaced mechanical `border-radius >= 24px` hard rejection with objective, contextual evaluation; large radii are permitted when backed by interaction rationale (pills, chips, avatars).
- **7. Flexible Cultural Design Guidance**: Clarified that Modern East Asian Editorial aesthetics serve as non-mandatory inspiration rather than a rigid design requirement; final visual identity is determined in the Design Direction Proposal.
- **8. localStorage Trust Boundary**: Reinforced that `localStorage` is client-controlled and untrusted for authorization decisions; frontend role checks are purely for presentation/UX, with backend Spring Security remaining authoritative; prohibited logging credentials.
- **9. Normalized Absolute Language**: Replaced overreaching "100%" and "zero" claims with precise distinctions between security invariants, accessibility criteria, and diagnostic quality targets.
- **10. Realistic DevTools Console Rule**: Transitioned from absolute "zero warnings" to investigating unexpected runtime errors and CSP violations, acknowledging console cleanliness as a diagnostic signal.

## [v2.0.0] - 2026-09-05

### Major Architectural Upgrades
- **Created `PROJECT-CONTRACT.md`**: Centralized authoritative technical environment, stack boundaries, 4 RBAC role definitions, and business invariants (identity, DEC-42 server revocation, 2-step Excel boundary, 48 endpoints).
- **Decoupled Visual Invariants from Design Direction**: Converted hardcoded color hexes (`#FAF9F6`, `#1A1A1A`, `#C83C23`, `#2E7D5B`) in `frontend-ui-engineering` into semantic palette roles; concrete hexes will be frozen upon approval of the formal Design Direction Proposal.
- **Configurable Runtime Base URL**: Updated `central-api-client.md` to default to same-origin relative path `/api/v1` (for Spring Boot / Nginx reverse proxy serving), with dynamic override via `window.__ENV__?.API_BASE_URL` for local cross-origin development.
- **Threat-Model Grounding for Storage**: Structured JWT storage documentation in `auth-and-token-lifecycle.md` around explicit threat trade-offs between `localStorage`, `sessionStorage`, and `HttpOnly` cookies.
- **Pragmatic DOM Safety Policy**: Refined `xss-and-dom-safety.md` to clarify that `innerHTML` with static, trusted markup is permitted, while dynamic interpolation of untrusted data into markup strings is strictly prohibited (CWE-79).
- **Resource Graph Driven CSP**: Updated `csp-and-client-hardening.md` to establish a resource-inventory methodology rather than hardcoding premature CSP strings; enforced verified SRI hashes for CDN dependencies.
- **Accurate WCAG 2.2 AA Conformance**: Aligned `accessibility-and-responsive.md` with W3C SC 2.5.8 ($24\times 24$ CSS px target size or bounding circle offset), SC 3.3.8 (accessible authentication), and SC 2.4.11 (focus not obscured). Differentiated automated Lighthouse scores from actual WCAG conformance.
- **Objective Design Governance**: Grounded `anti-template-and-design-governance.md` in measurable rejection triggers rather than subjective aesthetic opinions.
- **Cleaned Task Sequencing Contamination**: Removed all references to roadmap task numbers ("Task 9A.1", "Phase 10") from the skill definitions to maintain modular separation of concerns.

### Files Modified & Created
- **Created**:
  - `.agents/skills/PROJECT-CONTRACT.md`
  - `.agents/skills/FRONTEND-SKILLS-AUDIT.md`
  - `.agents/skills/CHANGELOG.md`
- **Updated**:
  - `.agents/skills/frontend-ui-engineering/SKILL.md`
  - `.agents/skills/frontend-ui-engineering/references/product-identity-and-design-tokens.md`
  - `.agents/skills/frontend-ui-engineering/references/rbac-surfaces-and-layouts.md`
  - `.agents/skills/frontend-ui-engineering/references/srs-learning-interaction.md`
  - `.agents/skills/frontend-ui-engineering/references/accessibility-and-responsive.md`
  - `.agents/skills/frontend-api-integration/SKILL.md`
  - `.agents/skills/frontend-api-integration/references/central-api-client.md`
  - `.agents/skills/frontend-api-integration/references/auth-and-token-lifecycle.md`
  - `.agents/skills/frontend-api-integration/references/ui-states-and-forms.md`
  - `.agents/skills/frontend-api-integration/references/two-step-excel-import.md`
  - `.agents/skills/vanilla-js-dom-security/SKILL.md`
  - `.agents/skills/vanilla-js-dom-security/references/vanilla-js-architecture.md`
  - `.agents/skills/vanilla-js-dom-security/references/xss-and-dom-safety.md`
  - `.agents/skills/vanilla-js-dom-security/references/state-and-event-management.md`
  - `.agents/skills/vanilla-js-dom-security/references/csp-and-client-hardening.md`
  - `.agents/skills/browser-testing-with-devtools/SKILL.md`
  - `.agents/skills/browser-testing-with-devtools/references/devtools-verification-checklist.md`
  - `.agents/skills/browser-testing-with-devtools/references/playwright-e2e-patterns.md`
  - `.agents/skills/frontend-code-review/SKILL.md`
  - `.agents/skills/frontend-code-review/references/frontend-review-checklist.md`
  - `.agents/skills/frontend-code-review/references/anti-template-and-design-governance.md`
  - `.agents/skills/using-agent-skills/SKILL.md`

### Breaking Changes
- `apiClient` now defaults to `/api/v1` instead of `http://localhost:8080/api/v1`. If running cross-origin in local development without a proxy, configure `window.__ENV__ = { API_BASE_URL: 'http://localhost:8080/api/v1' }`.

### Notes for Engineers & Agents Using This Skill Package
1. Always consult `PROJECT-CONTRACT.md` before implementing any frontend module.
2. Produce and submit a concise Design Direction Proposal before writing new UI layout code.
3. Review actual diffs against `frontend-code-review/references/frontend-review-checklist.md` before marking any task as complete.
