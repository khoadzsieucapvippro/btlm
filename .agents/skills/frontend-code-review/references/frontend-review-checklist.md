# Frontend Code Review Checklist & Acceptance Gates

## 1. Purpose of the Frontend Code Review Gate

Before marking any frontend task or component as `COMPLETED`, the actual git diff (`git diff frontend/`) must be systematically audited against this checklist.

Reviews MUST be grounded in **actual code evidence**, not assumptions or optimistic declarations.

---

## 2. Nine-Axis Review Checklist

```text
+-------------------+-----------------------------------------------------------------------------+
| Review Axis       | Inspection Criteria & Verification Points                                   |
+-------------------+-----------------------------------------------------------------------------+
| 1. Architecture   | [ ] Page script acts as coordinator; does NOT call fetch directly           |
|    & Component    | [ ] Module dependencies flow unidirectionally: Page -> API -> Core Client   |
|      Hygiene      | [ ] Zero custom variables or application state polluted onto `window`       |
|                   | [ ] Shared primitives (common/) vs domain components (domain/) strictly      |
|                   |     separated; zero copy-paste duplicate DOM chunks                         |
+-------------------+-----------------------------------------------------------------------------+
| 2. Security       | [ ] Zero dynamic string interpolation into `innerHTML` (CWE-79)             |
|    & DOM Safety   | [ ] All untrusted data (notes, vocab, Excel, feedback) set via textContent  |
|                   | [ ] Zero `eval()`, `new Function()`, or dynamic script creation             |
|                   | [ ] URLs in links/images sanitized against `javascript:` pseudo-protocols   |
+-------------------+-----------------------------------------------------------------------------+
| 3. API Contract   | [ ] Base URL configurable; defaults to relative `/api/v1/*`                 |
|                   | [ ] Bearer token attached automatically via central `apiClient`              |
|                   | [ ] Unpacking `ApiResponse<T>` envelope (`code`, `message`, `data`, `errors`)|
|                   | [ ] Safe idempotent retry limited to GET requests; NEVER on mutations        |
+-------------------+-----------------------------------------------------------------------------+
| 4. State & UI     | [ ] Explicit Three-State UI: Loading spinner, Empty state, Error alert      |
|                   | [ ] Form submit buttons protected against double clicks via `isSubmitting`   |
|                   | [ ] Field-level validation errors from Spring Boot mapped to form inputs    |
|                   | [ ] Older async responses discarded to prevent race condition overwrites    |
+-------------------+-----------------------------------------------------------------------------+
| 5. Accessibility  | [ ] Semantic HTML5 landmarks used (<header>, <nav>, <main>, <footer>)        |
|    (WCAG 2.2 AA)  | [ ] Headings: Prefer one primary <h1> per view (project convention);        |
|                   |     logical tree (h1->h2->h3) without downward skips; CSS size tokens       |
|                   | [ ] Icon-only controls: Native <button>/<a> with accessible name            |
|                   |     (aria-label or .visually-hidden); inner decorative icon aria-hidden    |
|                   | [ ] Form errors linked via aria-describedby; aria-invalid="true" on error;  |
|                   |     errors signaled by text + icon, not color alone (SC 1.4.1)              |
|                   | [ ] Target size: >= 24x24px baseline or passes spacing/inline exception (SC 2.5.8) |
|                   | [ ] Accessible Auth: Paste & password-manager autofill not blocked (SC 3.3.8)|
|                   | [ ] Focus indicator visible (SC 2.4.7); not obscured by header (SC 2.4.11)  |
|                   | [ ] Bypass Blocks: Skip-to-main-content link on views with repeat navigation (SC 2.4.1)|
|                   | [ ] Contrast: text >= 4.5:1 (>= 3:1 large text SC 1.4.3); UI components/icons >= 3:1 (SC 1.4.11)|
|                   | [ ] ARIA states: aria-expanded on trigger buttons; aria-pressed only on toggle controls|
|                   | [ ] All primary user interactions operable via keyboard (Tab, Enter, Space) |
+-------------------+-----------------------------------------------------------------------------+
| 6. Responsive     | [ ] Layout verified at mobile (<768px), tablet, and desktop (>=992px)       |
|                   | [ ] Zero horizontal scrollbar on document body                              |
|                   | [ ] Admin data tables wrapped in `.table-responsive` scrolling container     |
+-------------------+-----------------------------------------------------------------------------+
| 7. CJK Typography | [ ] Hanzi scaled contextually (prominent in study cards, compact in tables) |
|    & Identity     | [ ] Font family uses approved CJK stack (Noto Sans SC, Microsoft YaHei)     |
|                   | [ ] Approved semantic tokens (canvas, surface, ink, accents) applied via CSS|
|                   | [ ] Purposeful decoration: Visual ornaments serve clear function; cultural  |
|                   |     motifs permitted if in Design Proposal, but NEVER mandatory             |
|                   | [ ] No generic AI SaaS template drift (unmotivated gradients, meaningless blur)|
+-------------------+-----------------------------------------------------------------------------+
| 8. Performance    | [ ] Event listeners attached via delegation on dynamic item collections     |
|    & Lifecycle    | [ ] Search inputs debounced by 300ms with AbortController cancellation      |
|                   | [ ] All global event listeners, timers & observers cleaned up on teardown   |
+-------------------+-----------------------------------------------------------------------------+
| 9. Dependencies   | [ ] Zero unapproved libraries (No jQuery, React, Vue, Tailwind, Axios)      |
|                   | [ ] Bootstrap 5.3 used as supporting utility tool; does not dictate identity|
|                   | [ ] Subresource Integrity (SRI) attributes included on verified CDN tags    |
+-------------------+-----------------------------------------------------------------------------+
```

---

## 3. Mandatory Rejection Triggers (Immediate Blockers)

If ANY of the following patterns appear in the actual diff, the review **MUST BE REJECTED IMMEDIATELY**:

1. **`innerHTML` with variable interpolation**: `element.innerHTML = '<div>' + note + '</div>'` or `${untrustedData}`.
2. **Inline event attributes**: `<button onclick="...">`.
3. **Raw `fetch()` calls in page scripts**: Calling `window.fetch()` directly instead of through `apiClient()`.
4. **Hardcoded secrets or credentials**: Hardcoded passwords, test tokens, or private keys.
5. **Silent error swallowing**: `catch (err) { console.log(err); }` without updating UI state or re-enabling submit buttons.
6. **jQuery usage**: `$` or `jQuery` references in any form.
7. **Generic AI SaaS template drift**: Default purple/blue linear gradients (`#667eea` $\rightarrow$ `#764ba2`) applied without product rationale, or unmotivated floating blob decorations.
8. **Identical surface layout**: Admin table screens using the airy reading layout of Learner flashcards.

---

## 4. Source Basis

- **W3C Web Content Accessibility Guidelines (WCAG) 2.2**:
  - SC 1.3.1 Info and Relationships (Programmatic structure & heading hierarchy)
  - SC 1.4.1 Use of Color (Error signaling with text and icons)
  - SC 1.4.3 Contrast (Minimum) & SC 1.4.11 Non-text Contrast
  - SC 2.4.1 Bypass Blocks (Skip to main content)
  - SC 2.4.6 Headings and Labels
  - SC 2.5.8 Target Size (Minimum) ($24 \times 24$ CSS px minimum)
  - SC 4.1.2 Name, Role, Value (Accessible names on icon buttons)
- **W3C WAI-ARIA 1.2 Specification**:
  - `aria-label`, `aria-describedby`, `aria-invalid`, `aria-hidden`, `aria-expanded`, `aria-pressed`
- **OWASP Application Security Verification Standard (ASVS 4.0.3)**:
  - V5 Validation, Sanitization and Encoding (XSS prevention)
  - V8 Data Protection & Session Handling
- **WHATWG DOM & HTML Living Standards**:
  - Resource cleanup and event listener lifecycle
- **Bootstrap 5.3 Documentation**:
  - Utility classes, form validation feedback patterns

