# Frontend Verification Infrastructure (Phase 9)

Welcome to the permanent Frontend Verification Suite for the eLearning Kangxi & SRS project.

This infrastructure is engineered for **Zero-Build Vanilla JS**, high execution speed, zero runtime overhead, and strict **cumulative regression**.

---

## 1. Test Pyramid & Execution Tiers

We enforce a 5-level test pyramid:

```text
[L4] Browser Subagent Acceptance (Manual QA & visual evidence)
       ↑
[L3] Accessibility Browser Checks (WCAG 2.2 SC 2.5.8, focus trap, live regions)
       ↑
[L2] Browser Smoke (Desktop shell, mobile toggle, modal, 3-state, toast)
       ↑
[L1] Unit / Contract Tests (security.js, ui.js state machine)
       ↑
[L0] Static Invariant Verification (HTML semantics, DOM sinks, CSS tokens, SRI)
```

---

## 2. Verification Commands

Run these commands from the repository root:

```bash
# 1. Fast Tier (L0 Static + L1 Unit) — ~0.76s (67 tests)
npm run verify:frontend:fast

# 2. Browser Smoke Tier (L2 Headless Chromium) — ~5.66s (8 tests)
npm run verify:frontend:browser

# 3. Accessibility Tier (L3 WCAG 2.2 AA Assertions) — ~2.80s (4 tests)
npm run verify:frontend:a11y

# 4. Gate Tier (L0 + L1 + L2 + L3) — ~7.48s (79 tests)
npm run verify:frontend:gate

# 5. Full Cumulative Regression — ~7.67s (79 tests)
npm run verify:frontend

# 6. Backend-in-the-loop Verification (Independent command)
npm run verify:backend:integration
```

---

## 3. Directory Layout

```text
tests/frontend/
├── static/                         # [L0] Static & deterministic invariant tests
│   ├── html-semantics.test.mjs    # Semantic tags, single h1 (project invariant), landmarks
│   ├── dom-sinks.test.mjs         # OWASP DOM XSS sinks, inline handlers, innerHTML policy
│   ├── css-tokens.test.mjs        # Design tokens, contrast ratios, anti-generic check
│   └── sri-integrity.test.mjs     # Subresource integrity hashes on CDN assets
├── unit/                           # [L1] Pure logic & contract tests
│   ├── security/
│   │   └── security.test.mjs      # escapeHtml, sanitizeNavigationUrl, sanitizeResourceUrl
│   ├── ui/
│   │   └── ui-primitives.test.mjs # 3-State transitions, toast contract, modal options
│   ├── auth/
│   │   └── auth-state.test.mjs    # Session manager, exact roles, corrupted JSON recovery, multi-tab
│   └── api/
│       └── api-client.test.mjs    # Headers, envelope unpacking, 204, 401/403/429, retry, timeout
├── e2e/                            # [L2] Fast browser smoke (User-visible behavior)
│   ├── foundation.browser.mjs     # B1 Desktop, B2 Mobile, B3 Modal, B4 3-State, B5 Toast
│   └── api-auth.browser.mjs       # BA1 Bearer injection, BA2 401 single-owner, BA3 cross-tab
├── accessibility/                  # [L3] Accessibility browser assertions & audit
│   └── a11y.browser.mjs           # SC 2.5.8 target size, focus restoration, dialog trap, ARIA
├── integration/                    # Independent backend verification
│   └── backend-integration.mjs    # Live Spring Boot health probe and catalog query
├── utils/                          # Common test infrastructure
│   ├── static-server.mjs          # Server probe & ephemeral HTTP server lifecycle
│   ├── browser-runner.mjs         # Headless browser launcher & locator helpers
│   └── setup-playwright-driver.mjs# Environment bootstrap utility (isolated)
├── manual/                         # [L4] Living manual QA checklist
│   └── FRONTEND-QA.md             # [AUTO], [AUTO + MANUAL], [MANUAL] test matrix
├── artifacts/                      # Test outputs (.gitignored)
├── runner.mjs                      # Central orchestrator with dynamic aggregation
└── README.md                       # This guide
```

---

## 4. Guidelines for Future Tasks (Task 9A.2, Module 9B, etc.)

### Rule 1: Cumulative Regression Is Non-Negotiable
When implementing Task 9A.2 (Centralized API Client):
1. Add new unit tests under `tests/frontend/unit/api/`.
2. Add new browser integration tests under `tests/frontend/e2e/`.
3. Register new test files in `tests/frontend/runner.mjs`.
4. Run `npm run verify:frontend` — **both old tests (9A.1) and new tests (9A.2) must pass**.

### Rule 2: Keep Browser Smoke Small & Representative
- Do not repeat the exact same user flow at 3 different viewports unless responsive reflow is specifically being verified.
- Prioritize user-visible locators (`getByRole`, `getByLabel`, visible text) over CSS classes.

### Rule 3: Zero Third-Party Production Runtime Dependencies
- `frontend/` must remain pure Vanilla JS ES6+ without bundling, Babel, Webpack, or npm dependencies.
- Verification tooling (`devDependencies`) is strictly isolated for test execution.
