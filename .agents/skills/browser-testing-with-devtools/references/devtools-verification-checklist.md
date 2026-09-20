# Chrome DevTools Verification Protocol & Checklist

## 1. Mandatory Browser Verification Protocol

Before declaring any frontend task or UI screen completed, the engineer or agent **MUST** perform an interactive browser inspection using Chrome DevTools (or Chrome DevTools MCP tools). 

Static code analysis is NOT sufficient proof that client-side JavaScript runs correctly without runtime exceptions.

### 1.1 Local HTTP/HTTPS Origin Mandatory (No `file://` Scheme)
All browser inspection and testing MUST be served from a local HTTP/HTTPS origin (e.g. `http://localhost:3000` via `python -m http.server 3000`, `npx serve frontend`, or equivalent local static server).
- **`file://` Forbidden as Primary Integration Environment**: Loading files directly via `file:///...` causes fatal failures in modern web apps:
  1. ES Modules (`<script type="module">`) are blocked by CORS/Origin policy under `file://` in Chromium and WebKit.
  2. Cross-origin API calls to the Spring Boot backend (`http://localhost:8080/api/v1/*`) are blocked because the `file://` pseudo-origin evaluates to `Origin: null`, failing Spring Security CORS checks.
  3. `localStorage` and `sessionStorage` scope can collide or behave unpredictably across directory paths under `file://`.

---

## 2. DevTools 5-Pillar Inspection Checklist

```text
+-------------------+-------------------------------------------------------------+
| DevTools Pillar   | Required Verification Points & Acceptance Criteria          |
+-------------------+-------------------------------------------------------------+
| 1. Console        | • No uncaught runtime exceptions (TypeError, ReferenceError)|
|                   | • No unhandled promise rejections                           |
|                   | • Unexpected runtime errors & CSP violations MUST be        |
|                   |   investigated; expected/intentional warnings MAY remain     |
|                   | • Diagnostic Signal: Console cleanliness is a quality signal|
|                   |   rather than an absolute security proof                    |
|                   | • No debug logs containing raw JWT tokens or credentials    |
+-------------------+-------------------------------------------------------------+
| 2. Network        | • Base URL conforms strictly to /api/v1/*                   |
|                   | • Authorization: Bearer <token> header present on auth calls|
|                   | • Content-Type: application/json (omitted for FormData)     |
|                   | • Response status matches REST standard (200, 201, 204...)   |
|                   | • Response envelope matches ApiResponse<T> contract         |
|                   | • Safe handling of 401 (redirect), 403, 409, 429            |
+-------------------+-------------------------------------------------------------+
| 3. Elements & DOM | • Semantic HTML landmarks (<header>, <nav>, <main>, <footer>|
|    & Accessibility| • Heading hierarchy (SC 1.3.1, 2.4.6): Prefer one primary <h1>|
|                   |   per view (project convention); logical tree (h1->h2->h3)  |
|                   |   without downward skips; stepping up valid; CSS size tokens|
|                   | • Form <label for="id"> properly connected to input         |
|                   | • Field errors linked via aria-describedby to error message;|
|                   |   aria-invalid="true" set on error, cleared on valid entry; |
|                   |   errors signaled by text + icon, not color alone (SC 1.4.1)|
|                   | • Icon-only controls: Native <button>/<a> with accessible   |
|                   |   name (aria-label or .visually-hidden); inner decorative   |
|                   |   glyph aria-hidden="true"; verified in a11y tree           |
|                   | • Target size: >= 24x24px baseline or passes spacing circle |
|                   |   exception (SC 2.5.8); ~44x44px primary touch target       |
|                   | • Focus: Visible focus (:focus-visible, SC 2.4.7); focus    |
|                   |   not completely obscured by sticky header (SC 2.4.11)      |
+-------------------+-------------------------------------------------------------+
| 4. Device Mode    | • 375px (Mobile Portrait): No horizontal scrollbar on body  |
|                   | • 768px (Tablet): 2-column grids adapt cleanly               |
|                   | • 1200px (Desktop): Content width constrained comfortably   |
|                   | • Touch targets comfortable for thumb interaction           |
+-------------------+-------------------------------------------------------------+
| 5. Network Latency| • Throttle to "Slow 3G": Spinner appears immediately        |
|    & Offline      | • Throttle to "Offline": Friendly disconnected alert appears |
|                   | • Retry button re-executes GET request gracefully           |
+-------------------+-------------------------------------------------------------+
| 6. Performance &  | • Lighthouse audit: Diagnostic performance signal           |
|    Media Budgets  | • LCP (Largest Contentful Paint) <= 2.5s on desktop / mobile |
|                   | • Audio payload budget: Pronunciation audio <= 100KB per clip|
|                   | • Image & SVG budget: Decorative assets optimized / lazy-load|
|                   | • Memory: No detached DOM leaks or accumulating timers      |
+-------------------+-------------------------------------------------------------+
```

---

## 3. Interactive Double-Click & Race Condition Verification

1. Open the form (e.g. `creator-lesson-editor.html`).
2. Fill in valid data.
3. Open DevTools **Network Tab**.
4. Double-click or rapidly click the `Submit` button 3–5 times.
5. **Acceptance**: Exactly ONE network request is dispatched. The button enters disabled state during processing and re-enables on completion.

---

## 4. Performance, Core Web Vitals & Media Verification Protocol

Chinese language learning platforms handle frequent font rendering (CJK glyphs), audio clips (pinyin pronunciation), and stroke animations. Implementation and review agents MUST verify:

1. **LCP & Rendering Latency**:
   - Primary content (Radical grid, lesson vocabulary list, or flashcard deck) must render with LCP $\le 2.5\text{s}$ under standard network conditions.
   - Web fonts / system fallbacks (`font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif`) must avoid invisible text flashes (FOIT) by specifying appropriate font loading behavior.
2. **Audio & Media Asset Budgets**:
   - Pronunciation audio files (MP3/OGG/AAC) must be compressed to $\le 100\text{KB}$ per item or streamed on demand.
   - Audio playback MUST NOT auto-play on page load without user gesture (WCAG 1.4.2 Audio Control).
   - Images and non-critical SVG illustrations outside the initial viewport MUST utilize `loading="lazy"`.
3. **Memory & Detached DOM Nodes**:
   - In DevTools **Memory Tab**, take a Heap Snapshot before and after running 20 SRS flashcard flips.
   - Verify that card DOM elements and event listeners are properly garbage-collected and do not retain unbounded memory.

---

## 5. Source Basis

- **W3C Web Content Accessibility Guidelines (WCAG) 2.2**:
  - SC 1.3.1 Info and Relationships (Heading hierarchy and programmatic structure)
  - SC 1.4.1 Use of Color (Form error indications not relying on color alone)
  - SC 2.4.6 Headings and Labels (Descriptive headings and form control labels)
  - SC 2.5.8 Target Size (Minimum) (Normative $24 \times 24$ CSS px minimum)
  - SC 4.1.2 Name, Role, Value (Accessible names on icon-only interactive controls)
- **W3C WAI-ARIA 1.2 Specification**:
  - Accessible Name and Description Computation 1.2
  - `aria-describedby`, `aria-invalid`, `aria-hidden` state mappings
- **WHATWG HTML Living Standard**:
  - Module Scripts and CORS Origin Constraints (Sections on `<script type="module">` and fetching)
  - Semantic elements (`<main>`, `<nav>`, `<header>`, `<footer>`, `<button>`, `<label>`)
- **MDN Web Docs**:
  - Chrome DevTools: Inspecting the Accessibility Tree
  - Web Security: Same-origin policy and CORS with `file://` pseudo-origins

