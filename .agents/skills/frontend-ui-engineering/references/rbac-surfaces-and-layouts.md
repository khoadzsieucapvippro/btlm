# Product Surfaces & Layout Strategies Across 4 RBAC Roles

## 1. Intentional Role-Based Visual Differentiation

The application serves four distinct user roles. **Do not create a one-size-fits-all dashboard**. Each role has unique cognitive tasks, workflow velocities, and layout density needs:

```text
+-------------------+--------------------------------------------------------------+
| Role              | Primary Cognitive Mode & Layout Structure                    |
+-------------------+--------------------------------------------------------------+
| 1. Learner        | Calm, focused, distraction-free, generous margins, visual    |
| 2. Creator        | Authoring workbench, ordering sequences, validation previews |
| 3. Moderator      | High-speed inspection, dual-column diffs, flagged fields     |
| 4. Admin          | Dense operational grids, filter bars, strict 2-step confirms |
+-------------------+--------------------------------------------------------------+
```

---

## 2. Surface 1: Learner (Learning & Practice Surface)

### Goals & UX Philosophy
- **Immersion & Calm**: Low cognitive clutter, high readability, peaceful scholarly aesthetic.
- **Reading Comfort**: Generous line heights (`1.6 - 1.8`), container width constrained to `720px - 840px` for optimal eye-tracking.
- **Interactivity**: Clean flashcard deck with 3D flip, audio pronunciation triggers, quick contextual note-taking.

### Layout Rules
- **Header**: Minimalist navigation (Catalog, Lessons, SRS Review, Profile, SRS Settings).
- **Public Catalog (`radicals.html`, `vocabulary.html`)**: Grid cards for 214 Kangxi radicals (searchable by character, pinyin, or meaning; sorted by canonical index 1..214) and vocabulary items (searchable via `GET /api/v1/vocabulary?search=`). *(NOTE: Kangxi radicals in the backend database and DTOs do not contain stroke count data; stroke-count filtering is EXCLUDED from current scope as `FUTURE / BLOCKED — NO AUTHORITATIVE BACKEND DATA SOURCE`).*
- **Lesson Detail (`lesson-detail.html`)**: Linear reading flow. Ordered vocabulary list showing `order_index` sequence (1, 2, 3...) explicitly.
- **Flashcard Deck (`srs-review.html`)**: Centered viewport layout, zero sidebars, high-contrast prompt, keyboard rating hotkeys.
- **User Profile (`profile.html`)**: Form layout for viewing and editing `fullName` and `avatarUrl`. Displays `emailOrPhone` as read-only. Shows user role badges retrieved from active authenticated session state (`AuthResponse.roles`), NOT from `/users/profile` (`UserProfileResponse` contains personal details but no roles). Uses `GET/PUT /api/v1/users/profile` for profile data.
- **SRS Settings (`srs-settings.html`)**: Compact form for `newCardsPerDay` and `maxReviewPerDay` configuration. Contextual help text explaining setting effects on daily study sessions. Uses `GET/PUT /api/v1/srs/settings`.

---

## 3. Surface 2: Creator (Authoring Studio)

### Goals & UX Philosophy
- **Authoring Productivity**: Fast input, seamless re-ordering, immediate validation feedback.
- **State Transparency**: Clear visual badges for lesson states (`Draft` in neutral grey, `Pending` in amber, `Rejected` in crimson with error details, `Approved` in celadon green).

### Layout Rules
- **Lesson Editor (`creator-lesson-editor.html`)**: Dual-pane or stacked editor with:
  1. Metadata header (Title, current state badge; NOTE: `CreateLessonRequest`/`UpdateLessonRequest` do NOT have a `description` field).
  2. Vocabulary workbench: list of attached vocabulary items with drag/button reorder controls (`Move Up`, `Move Down`).
  3. Action bar: `Save Draft`, `Preview`, `Submit for Review` (with confirmation modal).
- **Two-Step Excel Import (`creator-import.html`)**:
  - Drag-and-drop zone (`.xlsx` only, max 10MB).
  - Validation preview table: explicit green/red row indicators with field-level issues (missing Hanzi, invalid pinyin).
  - **Zero-mutation banner**: explicitly informing user that preview does not touch the database.
  - Final `Confirm Import` action unblocked only when zero fatal validation errors exist.

---

## 4. Surface 3: Moderator (Quality Control & Inspection Queue)

### Goals & UX Philosophy
- **High-Velocity Decision Making**: Quick scanning, audible pronunciation verification, contextual error flagging.
- **Audit Rigor**: Immutable rejection logs with mandatory explanation.

### Layout Rules
- **Queue Table (`moderator-queue.html`)**: Sortable table of `Pending` submissions showing submission timestamp, creator username, vocabulary count, and review status.
- **Inspection View (`moderator-review.html`)**:
  - Full lesson preview rendered exactly as a learner would experience it.
  - Quick-action floating drawer or sticky action bar: `Approve Lesson` (Celadon button) vs `Reject Lesson` (Crimson button).
  - **Reject Modal**: Mandatory text input for `rejectionReason` ($\le 500$ chars) and checkboxes for `flaggedFields` (`["pinyin", "meaning_vi", "hanzi"]`).

---

## 5. Surface 4: Admin (System Governance & Dense Operations)

### Goals & UX Philosophy
- **Operational Density**: Compact tables, zero unnecessary whitespace, instant multi-criteria filtering.
- **Safety Rails (Self-Protection)**:
  - Strict policy POL-8D-01: Admin cannot lock or set `Inactive` their own logged-in account.
  - Strict policy POL-8D-02: Admin cannot revoke `Admin` role from their own logged-in account.
  - All status/role mutations require 2-step confirmation modal alerting to instant JWT revocation (`authorization_version`).

### Layout Rules
- **Data Table Layouts (`admin-accounts.html`, `admin-roles.html`, `admin-radicals.html`, `admin-vocabulary.html`, `admin-lessons.html`)**:
  - Filter toolbar at top: status dropdown, search input with debounced query.
  - Compact table rows (`padding: 8px 12px`).
  - Monospace font for IDs, emails, timestamps.
  - Action dropdown or button group per row (`Edit`, `Change Status`, `Assign Roles`, `Delete`).
  - Standard PageResponse pagination footer (`Page X of Y`, `Page size selector: 10, 20, 50`).

---

## 6. Current Project Surface & View Inventory (Reference Architecture)

> **Current Project Surface & View Inventory**: This inventory documents the current reference layout of user surfaces and capability views across the 48 Java controller handlers / 49 HTTP method+path mappings. It is a functional reference map of views and endpoint bindings, NOT an immutable requirement that exactly 23 physical `.html` files must always exist. Implementation agents may modularize or co-locate views (e.g. modal overlays, tabbed panels, or unified auth controllers) where UX and architecture justify it.

### 6.1 Public Views & Pages (No Authentication Required)

| Page | File | Role | API Endpoints Used |
|---|---|---|---|
| Landing / Home | `index.html` | Public | None (static) |
| Login | `login.html` | Public | `POST /api/v1/auth/login` |
| Register | `register.html` | Public | `POST /api/v1/auth/register` |
| Radical Catalog | `radicals.html` | Public | `GET /api/v1/radicals` |
| Radical Detail | `radical-detail.html` | Public | `GET /api/v1/radicals/{id}` |
| Vocabulary Catalog | `vocabulary.html` | Public | `GET /api/v1/vocabulary` |
| Vocabulary Detail | `vocabulary-detail.html` | Public | `GET /api/v1/vocabulary/{id}` |
| Public Lessons | `lessons.html` | Public | `GET /api/v1/lessons` |
| Public Lesson Detail | `lesson-detail.html` | Public | `GET /api/v1/lessons/{id}` |

### 6.2 Learner Pages (Authenticated)

| Page | File | Role | API Endpoints Used |
|---|---|---|---|
| User Profile | `profile.html` | Any auth user | `GET /api/v1/users/profile`, `PUT /api/v1/users/profile` |
| SRS Settings | `srs-settings.html` | Any auth user | `GET /api/v1/srs/settings`, `PUT /api/v1/srs/settings` |
| SRS Review Session | `srs-review.html` | Learner+ | `GET /api/v1/srs/due`, `GET /api/v1/srs/new-cards`, `GET /api/v1/srs/lessons/{lessonId}/new-cards`, `POST /api/v1/srs/review`, `GET /api/v1/srs/stats` |
| Personal Notes | *(embedded in vocabulary-detail.html)* | Learner+ | `GET /api/v1/vocabularies/{vocabId}/notes`, `POST /api/v1/vocabularies/{vocabId}/notes`, `PUT /api/v1/notes/{noteId}`, `DELETE /api/v1/notes/{noteId}` |

### 6.3 Creator Pages (Creator / Admin)
> *Spring Security Authority: `ROLE_CREATOR` / `ROLE_ADMIN` | Frontend JSON Role: `hasRole('Creator')` or `hasRole('Admin')`*

| Page | File | Role | API Endpoints Used |
|---|---|---|---|
| My Lessons List | `creator-lessons.html` | Creator | `GET /api/v1/creator/lessons` |
| Lesson Editor | `creator-lesson-editor.html` | Creator | `POST /api/v1/creator/lessons`, `GET /api/v1/creator/lessons/{id}`, `PUT /api/v1/creator/lessons/{id}`, `DELETE /api/v1/creator/lessons/{id}`, `POST /{id}/vocabularies/{vocabId}`, `DELETE /{id}/vocabularies/{vocabId}`, `PUT+POST /{id}/reorder`, `POST /{id}/submit` |
| Excel Import | `creator-import.html` | Creator | `POST /api/v1/creator/lessons/import`, `POST /api/v1/creator/lessons/import/confirm` |

### 6.4 Moderator Pages (Moderator / Admin)
> *Spring Security Authority: `ROLE_MODERATOR` / `ROLE_ADMIN` | Frontend JSON Role: `hasRole('Moderator')` or `hasRole('Admin')`*

| Page | File | Role | API Endpoints Used |
|---|---|---|---|
| Moderation Queue | `moderator-queue.html` | Moderator | `GET /api/v1/moderator/lessons/pending` |
| Lesson Review | `moderator-review.html` | Moderator | `GET /api/v1/moderator/lessons/{id}`, `POST /api/v1/moderator/lessons/{id}/approve`, `POST /api/v1/moderator/lessons/{id}/reject` |
| Moderation History | `moderator-history.html` | Moderator | `GET /api/v1/moderator/history` |

### 6.5 Admin Pages (Admin)
> *Spring Security Authority: `ROLE_ADMIN` | Frontend JSON Role: `hasRole('Admin')`*  
> *Note on Master Catalog Browsing: There are NO fictional admin GET endpoints like `GET /api/v1/admin/radicals` or `GET /api/v1/admin/vocabulary`. Admin exploration tables query the public endpoints `GET /api/v1/radicals` and `GET /api/v1/vocabulary?search=`.*

| Page | File | Role | API Endpoints Used |
|---|---|---|---|
| Account Management | `admin-accounts.html` | Admin | `GET /api/v1/admin/accounts`, `PUT /api/v1/admin/accounts/{id}/status` |
| Role Management | `admin-roles.html` | Admin | `GET /api/v1/admin/roles`, `PUT /api/v1/admin/accounts/{id}/roles` |
| Lesson Oversight | `admin-lessons.html` | Admin | `GET /api/v1/admin/lessons` |
| Radical CRUD | `admin-radicals.html` | Admin | `GET /api/v1/radicals` (read), `POST /api/v1/admin/radicals`, `PUT /api/v1/admin/radicals/{id}`, `DELETE /api/v1/admin/radicals/{id}` |
| Vocabulary CRUD | `admin-vocabulary.html` | Admin | `GET /api/v1/vocabulary?search=` (read), `POST /api/v1/admin/vocabulary`, `PUT /api/v1/admin/vocabulary/{id}`, `DELETE /api/v1/admin/vocabulary/{id}` |

### 6.6 UNVERIFIED / NEEDS CONFIRMATION
- The prompt mentions "Admin xóa lesson của creator" but `AdminLessonController.java` only has 1 GET endpoint (list all lessons). There is **no admin DELETE lesson endpoint** in the current backend code. If this functionality is needed, it requires a backend change first.

