# Accessibility (WCAG 2.2 AA Baseline) & Behavior-Oriented Responsive Engineering

## 1. Accessibility Engineering Standards (W3C WCAG 2.2)

Accessibility is an integral engineering requirement, not an optional finishing touch.

> **Conformance Discipline**: A Lighthouse Accessibility score $\ge 90$ is an automated quality indicator, **NOT proof of full WCAG 2.2 conformance**. Manual keyboard verification and screen-reader semantics audits are mandatory.

---

## 2. Key WCAG 2.2 Requirements & Project Standards

### 2.1 Target Size (Minimum) & Ergonomic Targets
- **Normative Requirement (`NORMATIVE: WCAG 2.2 SC 2.5.8 - Level AA`)**: Interactive pointer targets (buttons, links, icon toggles, pagination controls) MUST have a target size of at least **$24 \times 24\text{ CSS pixels}$**, except where specific exceptions defined by W3C apply:
  - **Spacing Exception**: If a target is smaller than $24\times 24$ CSS px, it does NOT fail SC 2.5.8 if a 24 CSS px diameter circle centered on the target's bounding box does not intersect another target or the bounding circle of another undersized target. A target under $24\times 24\text{px}$ is NOT an automatic failure if this spacing requirement is met.
  - **Inline Exception**: The target is an inline link or control within a continuous block of text or sentence.
  - **Essential Exception**: A particular visual presentation of the target is essential to the information being conveyed.
  - **User Agent Exception**: The target size is determined solely by the browser user agent and has not been modified by the author.
- **Ergonomic Usability Target (`PROJECT USABILITY TARGET / UX RESEARCH`)**: Primary touch controls (flashcard answer rating buttons, mobile navigation toggles) SHOULD adopt a larger project usability target of approximately **$44 \times 44\text{ CSS pixels}$** (derived from WCAG SC 2.5.5 Level AAA recommendations and mobile OS human interface guidelines). Implementation agents MUST NOT mischaracterize $44\times 44\text{px}$ as the normative WCAG Level AA minimum.

### 2.2 Accessible Authentication & Credential Usability (`NORMATIVE: WCAG 2.2 SC 3.3.8 - Level AA`)
- Authentication flows MUST NOT rely on cognitive function tests (such as memorizing arbitrary numbers, solving arithmetic, or cognitive puzzle captchas) unless an alternative or assistance mechanism is provided.
- Authentication flows MUST NOT unnecessarily block paste operations, and MUST NOT unnecessarily block password-manager autofill (supporting tools like 1Password, Bitwarden, and native browser credential managers).
- *Boundary & Scoping*: Unblocking paste and autofill is an accessibility and security usability requirement; it MUST NOT be claimed that paste support alone proves full WCAG SC 3.3.8 conformance.
- Form inputs SHOULD include standard `autocomplete` attributes (`username`, `current-password`, `new-password`, `name`, `email`).

### 2.3 Focus Management & Keyboard Operability
- **Visible Focus (`NORMATIVE: WCAG 2.2 SC 2.4.7 - Level AA`)**: Every interactive element MUST have a visible keyboard focus indicator when receiving focus (`:focus-visible`). Never apply `outline: none` or `outline: 0` without providing an equal or superior visible focus ring.
- **Focus Appearance Guidance (`ENGINEERING RECOMMENDATION / LEVEL AAA INSPIRATION`)**: 
  - *Distinction*: WCAG SC 2.4.7 (Level AA) requires only that a focus indicator is visible; it does NOT establish a numerical contrast ratio. 
  - Quantitative requirements on indicator area and a minimum **$3:1$ contrast ratio** against the unfocused state and adjacent colors are defined in **WCAG SC 2.4.13 Focus Appearance (Level AAA)**.
  - Project stylesheets SHOULD adopt high-contrast focus rings satisfying SC 2.4.13 principles as an engineering best practice, but review agents MUST NOT misclassify 3:1 focus contrast as an SC 2.4.7 Level AA requirement:
    ```css
    :focus-visible {
      outline: 2px solid var(--color-accent-primary, #C83C23);
      outline-offset: 2px;
    }
    ```
- **Focus Order (`NORMATIVE: WCAG 2.2 SC 2.4.3 - Level A`)**: Sequential keyboard navigation (`Tab` / `Shift+Tab`) MUST follow a logical order that preserves meaning and operability, matching the visual reading hierarchy. Avoid positive `tabindex` values (`tabindex="1+"`), which disrupt natural DOM flow.
- **No Keyboard Trap (`NORMATIVE: WCAG 2.2 SC 2.1.2 - Level A`)**: Keyboard focus MUST NOT become trapped within any component. Users must be able to move focus away using standard keyboard keys (`Tab`, `Shift+Tab`, or `Escape`).
- **Modal & Dialog Focus Lifecycle (`ENGINEERING RECOMMENDATION / WAI APG`)**:
  - When a modal dialog opens, focus MUST move immediately inside the dialog (to the first interactive element or close button).
  - Focus MUST be constrained within the active modal while it is open (using Bootstrap's focus trap or native `<dialog>`).
  - When the modal is dismissed or closed, focus MUST return to the invoking control that triggered it.
- **Focus Not Obscured (Minimum) (`NORMATIVE: WCAG 2.2 SC 2.4.11 - Level AA`)**: Fixed headers, sticky navigation bars, and floating toolbars MUST NOT completely obscure elements when they receive keyboard focus (SC 2.4.12 Enhanced is Level AAA):
  ```css
  :target, [tabindex="-1"], input, button, select, textarea {
    scroll-margin-top: 80px; /* Accounts for fixed sticky navigation height */
  }
  ```
- **Bypass Blocks (`NORMATIVE: WCAG 2.2 SC 2.4.1 - Level A`)**: A mechanism MUST be provided to bypass repeated blocks of content (such as top navigation bars and site headers). Every HTML page containing repeated navigation MUST include a "Skip to main content" link as the first focusable element in the DOM:
  ```html
  <a href="#mainContent" class="visually-hidden-focusable btn btn-cinnabar position-absolute top-0 start-0 m-2 z-3">
    Chuyển đến nội dung chính
  </a>
  ```

### 2.4 Contrast Standards (Text & Non-Text)
- **Text Contrast Minimum (`NORMATIVE: WCAG 2.2 SC 1.4.3 - Level AA`)**:
  - **Normal text** ($< 24\text{px}$ or $< 18.5\text{px}$ bold): Minimum contrast ratio **$4.5:1$** against its background.
  - **Large-scale text**: Minimum contrast ratio **$3:1$**. Large-scale text is strictly defined by WCAG as text that is at least $18\text{pt}$ ($24\text{px}$) or $14\text{pt}$ ($18.5\text{px}$) bold.
  - **Hanzi Contrast Discipline**: Hanzi is a writing system and content type, not an intrinsic size category. The $3:1$ threshold applies ONLY when text actually satisfies the WCAG definition of large-scale text (such as Flashcard hero prompts). Standard body Hanzi text (in vocabulary tables, forms, and metadata chips) remains subject to standard normal text contrast requirements ($\ge 4.5:1$).
- **Non-Text Contrast (`NORMATIVE: WCAG 2.2 SC 1.4.11 - Level AA`)**:
  - The visual presentation of **User Interface Components** (active borders of input fields, boundaries of uncolored buttons, checkboxes, radio toggles) and **Graphical Objects** (icons conveying standalone meaning, status badges, charts) MUST have a contrast ratio of at least **$3:1$** against adjacent color(s).
  - *Distinction*: Do not apply the 4.5:1 normal text ratio to non-text components; SC 1.4.11 establishes 3:1 as the normative Level AA baseline for UI boundaries and icons. Focus indicators that change the border also satisfy SC 1.4.11 at 3:1.

### 2.5 Heading Structure & Logical Hierarchy
- **Heading Semantics (`NORMATIVE: WCAG 2.2 SC 1.3.1 - Level A & SC 2.4.6 - Level AA`)**: Headings and labels must describe the topic or purpose of the section. Headings (`<h1>` through `<h6>`) MUST represent the logical semantic hierarchy of the content:
  - Avoid skipping heading levels when creating nested subsections (`<h1>` $\rightarrow$ `<h2>` $\rightarrow$ `<h3>`, avoiding jumping directly from `<h1>` to `<h4>`).
  - **Moving back up is valid**: Navigating from a deep subsection (e.g. `<h3>`) back up to a higher-level heading (e.g. `<h2>`) to begin a new main section is completely valid and expected in document outlines.
  - **Visual Styling Separation**: Visual typography size MUST be governed by CSS utility classes or design tokens, NOT by choosing heading levels based purely on their default font size.
- **Page Heading Convention (`PROJECT CONVENTION`)**: Each page or primary view SHOULD prefer one primary `<h1>` representing the main subject of the document outline. Implementation agents and reviewers MUST understand that preferring one `<h1>` is a project architectural convention for document clarity, NOT a strict WCAG normative requirement.

### 2.6 Icon-Only Interactive Controls & Accessible Names
- **Semantic Elements (`NORMATIVE: WCAG 2.2 SC 4.1.2 - Level A`)**: Native semantic elements (`<button>`, `<a>`) SHOULD always be preferred over custom non-semantic clickable elements (`<div onclick="...">`).
- **Accessible Name Requirement (`NORMATIVE: WCAG 2.2 SC 4.1.2 - Level A`)**: Every icon-only interactive control MUST possess an unambiguous accessible name accessible to assistive technologies (`aria-label` or `.visually-hidden` text).
- **Icon Role Differentiation (`ENGINEERING RECOMMENDATION / WAI-ARIA 1.2`)**:
  - **Meaningful Standalone Icons**: Icons that convey standalone information without adjacent text MUST NOT be hidden with `aria-hidden="true"`; they must have an accessible name (e.g. `<svg role="img" aria-label="...">`).
  - **Icons inside Interactive Controls**: When an icon resides inside a `<button>` or `<a>` that already has an accessible name, the name belongs to the button; the inner icon SHOULD be marked `aria-hidden="true"` to prevent redundant or confusing screen-reader announcements:
    - Search: `<button type="button" aria-label="Tìm kiếm từ vựng"><i class="bi bi-search" aria-hidden="true"></i></button>`
    - Audio trigger: `<button type="button" class="btn-audio" aria-label="Phát âm từ vựng"><i class="bi bi-volume-up" aria-hidden="true"></i></button>`
    - Close modal: `<button type="button" class="btn-close" aria-label="Đóng hộp thoại"></button>`
  - **Decorative Icons with Visible Text**: Icons used alongside visible text labels MUST be marked with `aria-hidden="true"` so screen readers do not announce redundant symbol glyphs.

### 2.7 Non-Text Content & Audio Controls
- **Alternative Text (`NORMATIVE: WCAG 2.2 SC 1.1.1 - Level A`)**:
  - **Informational Images**: Images that convey learning content (stroke order diagrams, radical illustrations) MUST have descriptive `alt` text explaining essential information.
  - **Decorative Images**: Purely decorative illustrations, background textures, or divider graphics MUST use empty `alt=""` or `role="presentation"` / `aria-hidden="true"`. Do NOT invent meaningless alt text (e.g. `alt="decoration"`).
  - **Redundancy Avoidance**: If an image is accompanied by adjacent visible text that conveys the identical information, use `alt=""` on the image.
- **Audio Pronunciation Controls (`ARIA SEMANTICS DISCIPLINE`)**:
  - Pronunciation triggers MUST expose an accessible name (`aria-label="Phát âm từ 你 (nǐ)"`) and clear visual feedback during playback (e.g. animated wave or transient busy state).
  - **Toggle Semantics Rule**: The `aria-pressed` attribute is strictly reserved for controls with **toggle semantics** (such as an audio Mute/Unmute toggle or loop switch). A momentary play-pronunciation trigger does NOT have toggle semantics and MUST NOT use `aria-pressed="true"` to indicate transient playback state.

### 2.8 Dynamic Status Announcements (`NORMATIVE: WCAG 2.2 SC 4.1.3 - Level AA`)
- Toast notifications and asynchronous state updates MUST be exposed to assistive technologies without stealing active keyboard focus:
  ```html
  <div id="toastContainer" class="toast-container position-fixed bottom-0 end-0 p-3" 
       role="status" aria-live="polite" aria-atomic="true"></div>
  ```

### 2.9 Disclosure Controls & ARIA Expanded/Controls Semantics (`NORMATIVE: WAI-ARIA 1.2 & APG`)
- **Controlling Element Placement**: The `aria-expanded` and `aria-controls` attributes MUST be placed on the **interactive trigger control** (e.g. `<button type="button" aria-expanded="false" aria-controls="mobileNav">`), NEVER on a generic container `<div>` or on the collapsible target element itself.
- **Applicable Patterns & Widget Roles**: `aria-expanded` is used only when the applicable ARIA pattern/role supports an expandable state and the element actually controls expandable content (e.g., disclosure buttons, accordion headers, comboboxes, treeitems, menuitems controlling submenus). Follow the specific WAI-ARIA APG pattern. Do not mechanically apply `aria-expanded` to generic containers (such as plain `<div>`s) or unrelated static content.
- **Two-Sided / 3D Flip Card Boundary**: 3D two-sided flashcards coordinate front and back face visibility using `aria-hidden="true"` / `aria-hidden="false"`, NOT `aria-expanded`. `aria-expanded` is reserved strictly for collapsible disclosures, dropdown menus, and accordion panels.

---

## 3. Behavior-Oriented Responsive Design

Responsive design must focus on **how components behave under viewport adaptation**, not merely arbitrary fixed pixel thresholds:

```text
+-------------------+-------------------------------------------------------------+
| Viewport Mode     | Component Adaptation & Content Priority                     |
+-------------------+-------------------------------------------------------------+
| Mobile            | • Navigation collapses into accessible offcanvas/hamburger  |
| (< 768px)         | • Multi-column forms stack vertically                       |
|                   | • Flashcard occupies full viewport width (touch swipe/tap)  |
|                   | • Tables wrap in localized scroll containers                |
+-------------------+-------------------------------------------------------------+
| Tablet            | • 2-column catalog grids for Kangxi radicals                |
| (768px - 991px)   | • Sidebar collapses to compact icon/text bar                |
|                   | • Moderate horizontal space for dual-pane editors           |
+-------------------+-------------------------------------------------------------+
| Desktop           | • Generous margin constraints for reading (720px - 840px)   |
| (>= 992px)        | • High-density data tables with visible filters (Admin)     |
|                   | • Full persistent top navigation                            |
+-------------------+-------------------------------------------------------------+
```

### 3.1 Dense Table Scrolling Hygiene & Keyboard Accessibility
Wide tabular data (Admin accounts, Moderation queue, Excel preview) MUST NOT cause full-page body horizontal scroll. Tables should be wrapped in a localized container (e.g. `.table-responsive`):
- **Horizontal Overflow Containment**: Contain horizontal scrolling locally within the container without breaking page layout.
- **Accessible Naming**: Provide an accessible name via `<caption>` or `aria-label` when useful for screen-reader orientation.
- **Context-Sensitive Keyboard Scrolling**:
  - If the table contains **interactive focusable elements** (e.g., buttons, links, edit inputs), keyboard users already scroll the container when tabbing through child elements. **Omit `tabindex="0"`** on the container to prevent redundant, disorienting tab stops.
  - If the table contains **pure static tabular text** with no internal focusable controls, add `tabindex="0"` and `role="region"` to allow keyboard-only users to focus and scroll the region using arrow keys.
  - **Do NOT mechanically add `tabindex="0"` and `role="region"` to every table wrapper**.

```html
<!-- Table containing action buttons/links: NO tabindex on wrapper to avoid redundant tab stop -->
<div class="table-responsive">
  <table class="table table-hover align-middle mb-0" aria-label="Danh sách tài khoản người dùng">
    <!-- Table columns with action buttons -->
  </table>
</div>
```

### 3.2 Responsive Typography Scaling & CJK Line Wrapping
CJK characters do not use whitespace for word segmentation. In responsive layouts:
- Use `overflow-wrap: anywhere` or `word-break: break-all` on Chinese character containers to prevent layout breaking on mobile viewports.
- Keep Pinyin accompanied by Hanzi with `white-space: nowrap` inside individual vocabulary token chips to avoid splitting tone marks from characters across line breaks:
```css
.chinese-text-content {
  overflow-wrap: anywhere;
  line-height: 1.6;
}

.vocab-token-chip {
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
}
```

---

## 4. Source Basis

- **W3C / WAI WCAG 2.2**:
  - SC 1.1.1 Non-text Content (Level A): [Understanding SC 1.1.1](https://www.w3.org/WAI/WCAG22/Understanding/non-text-content.html) — `AUTHORITATIVE`
  - SC 1.3.1 Info and Relationships (Level A) & SC 2.4.6 Headings and Labels (Level AA): [Page Structure: Headings](https://www.w3.org/WAI/tutorials/page-structure/headings/) — `AUTHORITATIVE`
  - SC 1.4.1 Use of Color & SC 1.4.3 Contrast (Minimum) (Level AA): [Understanding SC 1.4.3](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html) — `AUTHORITATIVE`
  - SC 1.4.11 Non-text Contrast (Level AA): [Understanding SC 1.4.11](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html) — `AUTHORITATIVE`
  - SC 2.1.2 No Keyboard Trap (Level A) & SC 2.4.3 Focus Order (Level A): [Understanding SC 2.1.2](https://www.w3.org/WAI/WCAG22/Understanding/no-keyboard-trap.html) — `AUTHORITATIVE`
  - SC 2.4.1 Bypass Blocks (Level A): [Understanding SC 2.4.1](https://www.w3.org/WAI/WCAG22/Understanding/bypass-blocks.html) — `AUTHORITATIVE`
  - SC 2.4.7 Focus Visible (Level AA) & SC 2.4.11 Focus Not Obscured (Minimum) (Level AA): [Understanding SC 2.4.11](https://www.w3.org/WAI/WCAG22/Understanding/focus-not-obscured-minimum.html) — `AUTHORITATIVE`
  - SC 2.4.13 Focus Appearance (Level AAA) & SC 2.4.12 Focus Not Obscured (Enhanced) (Level AAA): [Understanding SC 2.4.13](https://www.w3.org/WAI/WCAG22/Understanding/focus-appearance.html) — `AUTHORITATIVE`
  - SC 2.5.8 Target Size (Minimum) (Level AA) & SC 2.5.5 Target Size (Enhanced) (Level AAA): [Understanding SC 2.5.8](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html) — `AUTHORITATIVE`
  - SC 3.3.8 Accessible Authentication (Minimum) (Level AA): [Understanding SC 3.3.8](https://www.w3.org/WAI/WCAG22/Understanding/accessible-authentication-minimum.html) — `AUTHORITATIVE`
  - SC 4.1.2 Name, Role, Value & SC 4.1.3 Status Messages (Level AA): [Understanding SC 4.1.3](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html) — `AUTHORITATIVE`
  - WAI-ARIA 1.2 Specification & Authoring Practices Guide (APG): [Dialog (Modal) Pattern](https://www.w3.org/WAI/ARIA/apg/patterns/dialog-modal/) & [Disclosure Pattern](https://www.w3.org/WAI/ARIA/apg/patterns/disclosure/) — `AUTHORITATIVE`
- **MDN Web Docs**:
  - HTML Heading Elements: [Heading elements](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/Heading_Elements) — `AUTHORITATIVE`
  - Button Accessibility & ARIA Label: [Using the aria-label attribute](https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Attributes/aria-label) — `AUTHORITATIVE`
  - ARIA: aria-pressed state for toggle buttons: [aria-pressed](https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Attributes/aria-pressed) — `AUTHORITATIVE`
  - ARIA: aria-expanded state for disclosure widgets: [aria-expanded](https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Attributes/aria-expanded) — `AUTHORITATIVE`
- **Bootstrap Official Documentation**:
  - Bootstrap 5.3 Modal Component Accessibility: [Modal - Accessibility](https://getbootstrap.com/docs/5.3/components/modal/#accessibility) — `AUTHORITATIVE`
  - Bootstrap 5.3 Visually Hidden Helper: [Visually hidden](https://getbootstrap.com/docs/5.3/helpers/visually-hidden/) — `AUTHORITATIVE`
