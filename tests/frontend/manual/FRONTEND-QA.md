# Living Frontend QA Checklist & Manual Verification Matrix

> **Governance Principle**: This document is the living manual QA checklist across all frontend phases (Phase 9A $\rightarrow$ 9F).
> Automated testing covers regression baselines, but assistive technology usability, cognitive visual hierarchy, and real hardware responsiveness require structured manual audits.
>
> **Rule**: DO NOT mark `[PASS]` on any item unless an engineer or human evaluator has physically executed the test.

---

## 1. Classification Standard

| Classification | Meaning | Execution Cadence |
|:---|:---|:---|
| **`[AUTO]`** | 100% covered by headless automated regression suites (`npm run verify:frontend`). | Executed on every commit/task gate. |
| **`[AUTO + MANUAL]`** | Automated structural assertions pass; human verification needed for visual/semantic context. | Executed upon feature completion. |
| **`[MANUAL]`** | Pure human/sensory evaluation (screen reader speech flow, zoom 200%, physical touch gestures). | Executed before milestone release. |

---

## 2. Core Shell, Design System & Accessibility Matrix (Task 9A.1 Baseline)

### 2.1 Visual Identity & Typography
- [x] `[AUTO]` **Anti-Template Governance**: No generic purple/indigo AI SaaS gradients present in `style.css`.
- [x] `[AUTO]` **Authentic Cinnabar Accent**: Primary action buttons use Scholarly Cinnabar (`#C83C23`).
- [x] `[AUTO]` **CJK Font Stack**: `Noto Sans SC` and authentic font fallbacks declared in `--font-hanzi`.
- [x] `[AUTO + MANUAL]` **CJK Visual Hierarchy**: Chinese characters in Radical grid (36px), Vocab card (24px), and Table cells (18px) render crisp and legible on high-DPI displays.
- [ ] `[MANUAL]` **Dark Mode / High Contrast**: When Windows High Contrast Mode is active, borders and focus indicators remain visible without disappearing into canvas background.

### 2.2 Viewports & Responsive Reflow
- [x] `[AUTO]` **Desktop Shell (1280px)**: Header, nav, main, footer render with zero horizontal overflow (`scrollWidth <= clientWidth`).
- [x] `[AUTO]` **Tablet Shell (768px)**: Grid layout reflows gracefully without overlapping cards or clipped text.
- [x] `[AUTO]` **Mobile Shell (375px)**: Navbar toggler is visible; menu expands/collapses cleanly without page-level horizontal scrolling.
- [ ] `[MANUAL]` **Extreme Viewport (320px)**: Test on iPhone SE (320px width); ensure long Chinese character descriptions wrap cleanly without breaking layouts.
- [ ] `[MANUAL]` **Browser Zoom (200%)**: Press `Ctrl` + `+` up to 200% zoom; ensure text reflows without overlapping and no content is clipped off-screen (WCAG 2.2 SC 1.4.4 & 1.4.10).

### 2.3 Keyboard & Screen Reader Operability
- [x] `[AUTO]` **Skip Navigation Link**: Tab key reveals `.skip-link` at top of viewport; pressing Enter shifts focus to `#mainContent`.
- [x] `[AUTO]` **Visible Focus Indicators**: Interactive elements show distinct `:focus-visible` outline ring.
- [x] `[AUTO]` **Accessible Control Names**: All icon buttons and interactive controls possess non-empty accessible names (`aria-label` or visible text).
- [ ] `[MANUAL]` **NVDA / VoiceOver Speech Flow**:
  - Activate NVDA (Windows) or VoiceOver (macOS).
  - Tab through navbar links; confirm screen reader reads "Trang chủ, liên kết", "214 Bộ thủ, liên kết".
  - Trigger Toast; confirm NVDA announces polite notification without interrupting ongoing speech.
  - Open Modal; confirm NVDA announces "Hộp thoại, Xác nhận kiểm duyệt bài học".
- [ ] `[MANUAL]` **Focus Trapping & Tab Cycling**: In open Modal, Tab cycles exclusively through modal buttons (`Đóng`, `Hủy`, `Phê duyệt bài`) and never leaks to background page elements.

### 2.4 Touch Targets & Motor Accessibility
- [x] `[AUTO]` **Target Size Minimum (SC 2.5.8)**: All independent interactive buttons meet $\ge 24 \times 24$ CSS px; inline text links verified under standard inline exceptions.
- [ ] `[MANUAL]` **Physical Mobile Touch Testing**: Tap navbar toggle and buttons on a physical touch screen; confirm activation occurs reliably on first tap without mis-hits.

---

## 3. Cumulative Feature Modules (Living Section)

*(To be expanded as subsequent Phase 9 tasks are implemented)*

### Module 9A.2: Centralized API Client & Session Manager
- [ ] `[AUTO]` Automated 401 interception and redirect with `?redirect=` deep link preservation.
- [ ] `[AUTO]` 429 exponential backoff retry for GET requests.
- [ ] `[MANUAL]` Network offline simulation: disconnect WiFi, perform action, verify non-intrusive offline toast.

### Module 9B: Authentication & Profiles
- [ ] `[AUTO]` Accessible Authentication (WCAG 2.2 SC 3.3.8): Password managers and copy-paste supported.
- [ ] `[MANUAL]` Profile image upload preview and avatar alt text.

### Module 9C: Public Lessons & SRS Study
- [ ] `[AUTO]` 3D Flashcard flip hotkeys (`Space`, `1`, `2`, `3`, `4`).
- [ ] `[MANUAL]` Motion sensitivity: test flashcard flip when `prefers-reduced-motion` is active.
