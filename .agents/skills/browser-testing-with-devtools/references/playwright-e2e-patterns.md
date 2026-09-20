# Playwright End-to-End (E2E) Browser Automation Architecture

## 1. Role & Placement of Playwright in the Project

> **Conditional Adoption Invariant**: Playwright E2E automation is applicable **ONLY IF** the project explicitly initializes and configures the `e2e/` test suite. In a zero-build Vanilla JS environment without an established `e2e/` directory, implementation agents MUST NOT install npm packages solely to satisfy this skill. Testing is conducted via interactive Chrome DevTools, browser automation, or MCP inspection.

Playwright provides headless, deterministic browser automation to verify complete full-stack user journeys across the integrated Vanilla JS frontend and Spring Boot backend.

### 1.1 Technology Boundary & Prudent Browser Matrix
Playwright test assets are maintained in a dedicated `e2e/` directory decoupled from the production Java artifact:
- **Primary Baseline**: Chromium Desktop ($1280 \times 720$) and Chromium Mobile viewport ($375 \times 667$ iPhone SE emulation).
- **Secondary Cross-Engine Verification**: Firefox Desktop and WebKit Desktop for core rendering and 3D flashcard transform verification.

```text
e2e/
├── playwright.config.js          # Browser matrix, base URL, trace/video settings
├── global-setup.js               # Dynamic generation of auth states (never hardcoded)
├── fixtures/
│   ├── auth-fixtures.js          # Authenticated page fixtures for 4 roles
│   └── test-data.js              # Sample Excel files, valid/invalid payloads
├── tests/
│   ├── learner-journey.spec.js   # Complete Learner study loop
│   ├── creator-moderator.spec.js # Two-step import, authoring & moderation lifecycle
│   ├── admin-oversight.spec.js   # Account lifecycle, roles & catalog administration
│   ├── security-boundaries.spec.js# Token expiry, 401 redirect, 403 IDOR, 429 rate limit
│   └── session-lifecycle.spec.js # Reload, history navigation, DEC-42 revocation, multi-tab
└── package.json                  # Isolated devDependencies (@playwright/test)
```

### 1.2 Auth State Security & Git Exclusion Invariant (`PROJECT INVARIANT`)
- Playwright's `storageState` mechanism captures cookies and `localStorage` entries (including `access_token`) into JSON files for reusable test sessions.
- **Sensitive Credential Invariant**: Auth state JSON files contain actual authentication tokens and user credentials. **They MUST NEVER be committed to Git**.
- The `e2e/.auth/` directory MUST be added to `.gitignore`.
- Auth states MUST be generated dynamically during test execution (e.g. via `global-setup.js` or authenticated test fixtures), NEVER by committing pre-baked tokens into the repository.

---

## 2. The 4 Essential Role-Based E2E Journeys

### Journey 1: Learner Complete Learning Loop (`learner-journey.spec.js`)
```javascript
test('Learner complete study and SRS flashcard flow', async ({ page }) => {
  // 1. Register new account & Login
  await page.goto('/register.html');
  await page.fill('#username', 'testlearner');
  await page.fill('#password', 'TestPassword123!');
  await page.click('#btnRegister');
  await expect(page).toHaveURL(/login\.html/);

  await page.fill('#username', 'testlearner');
  await page.fill('#password', 'TestPassword123!');
  await page.click('#btnLogin');
  await expect(page).toHaveURL(/index\.html|radicals\.html/);

  // 2. Explore Radicals & Search Vocabulary
  await page.goto('/radicals.html');
  await expect(page.locator('.radical-card')).toHaveCount(214);

  await page.goto('/vocabulary.html');
  await page.fill('#searchInput', 'ni');
  await expect(page.locator('.vocab-row')).toContainText(['你', 'nǐ']);

  // 3. Open Approved Lesson & Create Contextual Note
  await page.goto('/lessons.html');
  await page.click('.lesson-card:first-child .btn-view');
  await page.click('.btn-add-note:first-child');
  await page.fill('#noteContent', 'Ghi chú học tập từ vựng này');
  await page.click('#btnSaveNote');
  await expect(page.locator('.note-item')).toContainText('Ghi chú học tập từ vựng này');

  // 4. SRS Flashcard Review Session
  await page.goto('/srs-review.html');
  await expect(page.locator('#charDisplay')).toBeVisible();
  
  // Flip card using Spacebar
  await page.keyboard.press('Space');
  await expect(page.locator('#flashcard')).toHaveClass(/is-flipped/);
  
  // Rate "Good" using hotkey '3'
  await page.keyboard.press('3');

  // 5. Verify Study Stats Dashboard Update
  await page.goto('/srs-dashboard.html');
  await expect(page.locator('#reviewsTodayCount')).not.toHaveText('0');
});
```

### Journey 2: Creator & Moderator Lifecycle (`creator-moderator.spec.js`)
1. **Creator**: Creates draft lesson $\rightarrow$ Uploads Excel file $\rightarrow$ Inspects preview table (verifies 0 DB mutation banner) $\rightarrow$ Confirms import $\rightarrow$ Reorders vocab $\rightarrow$ Submits for review (`Pending`).
2. **Moderator**: Logs in $\rightarrow$ Opens review queue $\rightarrow$ Inspects lesson $\rightarrow$ Rejects with reason & `flaggedFields` $\rightarrow$ Creator edits and resubmits $\rightarrow$ Moderator approves $\rightarrow$ Learner sees lesson in public catalog.

### Journey 3: System Administration Flow (`admin-oversight.spec.js`)
1. Admin logs in $\rightarrow$ Navigates to `admin-accounts.html` $\rightarrow$ Filters by status `Active` $\rightarrow$ Updates user to `Inactive` (verifies self-protection policy POL-8D-01).
2. Navigates to `admin-roles.html` $\rightarrow$ Assigns role `"Creator"` (payload `roles: ["Creator"]`) $\rightarrow$ Verifies notification of token invalidation.
3. Adds new radical & vocabulary to master catalog.

### Journey 4: Security Boundaries & Error Flow (`security-boundaries.spec.js`)
1. Injects expired/tampered token into `localStorage` $\rightarrow$ Reloads protected page $\rightarrow$ Verifies instant HTTP 401 interception, session cleanup, and redirect to `login.html`.
2. Learner attempts direct access to `/admin-accounts.html` $\rightarrow$ Backend returns 403 Forbidden $\rightarrow$ UI displays access denied alert.
3. Rapidly submits login form $\ge 10$ times $\rightarrow$ Verifies HTTP 429 Too Many Requests response and countdown message.

---

## 3. Browser Navigation & Session Lifecycle Testing Patterns

Modern web applications must handle real-world browser navigation and authentication lifecycle events deterministically. Playwright suites MUST verify the following patterns:

### 3.1 Nine Essential Auth & Navigation Scenarios (`PROJECT INVARIANT`)

Authenticated UI suites MUST test all 9 of the following session lifecycle and navigation cases:

1. **Initial Login**: Successful authentication, storage of `access_token` and `user_info` in `localStorage`, and role-based redirect to default landing surface (`/learner/`, `/creator/`, etc.).
2. **Protected Route Access**: Authenticated requests systematically attach `Authorization: Bearer <token>`; unauthorized access displays appropriate barriers.
3. **Page Reload (`page.reload()`)**:
   - Verify UI reconstructs state correctly from `localStorage` or refetches from backend API without crashing or rendering a perpetual blank screen.
   - Verify user authentication session and role context persist across reloads.
4. **Direct URL Navigation (Deep Linking)**:
   - Deep link with valid auth token: Loads target resource directly.
   - Deep link with missing/expired token: Preserves target path (`/login.html?redirect=${encodeURIComponent('/creator/lesson-editor.html?id=12')}`) and redirects back upon successful authentication.
   - Deep link with invalid/non-existent entity ID (e.g. `?id=99999`): Renders normalized empty/not-found UI state with a clear return action, not a broken page.
5. **Explicit Logout Flow**: Clicking logout clears `localStorage`, revokes client session, and redirects to `/login.html`.
6. **Invalid / Expired Session Handling**: Tampered or expired tokens in storage trigger clean redirection to login on initial page mount without console errors.
7. **Backend HTTP 401 Interception**: When any API call returns 401 Unauthorized, client clears tokens and redirects to `/login.html` with an informative session expiry notice.
8. **Server-Side Authorization Invalidation (DEC-42)**:
   - When an account is deactivated or role altered by an Admin, the backend increments `authorization_version`.
   - The next background or user-initiated API request in the affected session returns HTTP 401.
   - The central `apiClient` must clear client-side storage, emit `auth:expired`, and redirect user to `/login.html`.
9. **History Navigation (`page.goBack()`, `page.goForward()`)**:
   - Verify UI query parameters (search keyword, pagination `?page=2`, active tab) sync bidirectionally with URL without trapping user in redirect loops.
   - Verify back-navigation from a protected page after logout redirects cleanly to `/login.html` instead of displaying stale protected data from browser cache.

### 3.2 Executable Playwright Test Patterns

```javascript
// e2e/tests/session-lifecycle.spec.js
import { test, expect } from '@playwright/test';

test.describe('Session Lifecycle & History Navigation', () => {

  // Scenario A: Page Reload & Deep Linking with Return Redirect
  test('Page reload preserves auth and deep link redirects on token expiry', async ({ page }) => {
    // 1. Authenticate and navigate to protected route
    await page.goto('/login.html');
    await page.fill('#username', 'learner1');
    await page.fill('#password', 'ValidPassword123!');
    await page.click('#btnLogin');
    await expect(page).toHaveURL(/radicals\.html/);

    // 2. Perform page reload; verify session and data survive
    await page.reload();
    await expect(page.locator('.radical-card')).toHaveCount(214);

    // 3. Simulate token expiration/tampering in localStorage
    await page.evaluate(() => {
      localStorage.setItem('auth_token', 'tampered.or.expired.jwt');
    });

    // 4. Navigate to deep-linked lesson editor; verify 401 intercept and redirect with return param
    await page.goto('/lessons.html?id=42');
    await expect(page).toHaveURL(/\/login\.html\?redirect=/);
    expect(page.url()).toContain(encodeURIComponent('/lessons.html?id=42'));
  });

  // Scenario B: Multi-Tab Synchronization & DEC-42 Server-Side Revocation
  test('Multi-tab logout sync and 401 server-side revocation', async ({ context }) => {
    // Open Tab A and authenticate
    const tabA = await context.newPage();
    await tabA.goto('/login.html');
    await tabA.fill('#username', 'creator1');
    await tabA.fill('#password', 'ValidPassword123!');
    await tabA.click('#btnLogin');
    await expect(tabA).toHaveURL(/creator-lessons\.html/);

    // Open Tab B within the same browser context (shares localStorage)
    const tabB = await context.newPage();
    await tabB.goto('/creator-lessons.html');
    await expect(tabB.locator('#lessonList')).toBeVisible();

    // Trigger logout in Tab A
    await tabA.click('#btnLogout');
    await expect(tabA).toHaveURL(/login\.html/);

    // Tab B detects storage change or receives 401 on next interaction
    await tabB.bringToFront();
    // Simulate interaction in Tab B
    await tabB.click('#btnCreateLesson');
    await expect(tabB).toHaveURL(/login\.html/);
  });
});
```

---

## 4. Source Basis

- **Playwright Official Documentation**:
  - BrowserContext, Page, Navigation, and Network Interception: [Playwright Network](https://playwright.dev/docs/network) — `AUTHORITATIVE`
  - Storage State and Authentication Management: [Playwright Authentication](https://playwright.dev/docs/auth) — `AUTHORITATIVE`
- **W3C Web Content Accessibility Guidelines (WCAG) 2.2**:
  - SC 2.1.2 No Keyboard Trap & SC 2.4.3 Focus Order during navigation: [Understanding SC 2.4.3](https://www.w3.org/WAI/WCAG22/Understanding/focus-order.html) — `AUTHORITATIVE`
- **WHATWG HTML Standard**:
  - Session history and navigation (History API, `popstate` event): [WHATWG HTML History](https://html.spec.whatwg.org/multipage/nav-history-apis.html) — `AUTHORITATIVE`
  - Web Storage API (`StorageEvent` on cross-tab synchronization): [WHATWG Web Storage](https://html.spec.whatwg.org/multipage/webstorage.html) — `AUTHORITATIVE`
- **OWASP Session Management Cheat Sheet**:
  - Token Revocation & Client-side Storage Handling on HTTP 401: [OWASP Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html) — `AUTHORITATIVE`


