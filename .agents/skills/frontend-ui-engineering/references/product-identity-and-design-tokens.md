# Product Visual Identity, Typography Hierarchy & Semantic Design Tokens

## 1. Product Context & Visual Identity Strategy

The application is a **Chinese Language Learning Platform** focused on Kangxi Radicals, Chinese Vocabulary, Spaced Repetition (SRS SM-2), and structured Lessons.

### 1.1 Anti-Generic AI SaaS Governance (Contextual & Objective)
Implementation agents SHOULD avoid default, unconsidered generative AI design tropes that compromise domain clarity:
- **AVOID generic purple/blue linear gradients** (such as `#667eea` $\rightarrow$ `#764ba2`) applied as unconsidered, default backgrounds. Subtle, contextual gradients with clear UI rationale are permitted.
- **AVOID decorative glassmorphism** (`backdrop-filter: blur(...)`) applied indiscriminately across panels where it degrades contrast or readability.
- **AVOID excessive rounded geometry on regular content containers**. Standard card containers should maintain structural clarity. Large border radii MAY be used when backed by visual or interaction rationale (e.g., pill badges, circular action buttons, search pills, interactive chips); components MUST NOT be rejected mechanically based solely on a radius number.
- **AVOID floating decorative blobs** and abstract vector shapes that provide zero educational utility.
- **AVOID card-nesting overload** where every piece of data is placed inside an identical card.
- **AVOID monotonous metric grids** (copying the same "icon + big number + label" card across every dashboard).

### 1.2 Visual Direction Guidance: Editorial & Pedagogical Inspiration
The visual direction MAY draw inspiration from **clean editorial typography and the structural characteristics of Chinese language learning materials**:
- **Exploratory Inspiration (Non-Mandatory)**: References to scholarly print, contemporary editorial balance, or focused study environments serve as potential design inspirations, NOT rigid visual mandates. Calligraphy motifs, ink washes, seal stamps, paper textures, and traditional ornamentation are **NEVER mandatory**.
- **Design Proposal Boundary**: Concrete visual decisions—including the exact color palette, typography pairing, ornamentation, surface textures, and visual metaphors—MUST be decided and approved in the **Design Direction Proposal**.
- **Skill Invariant**: The engineering skills DO NOT freeze the visual identity or enforce cultural aesthetics ahead of the proposal. Skills mandate usability, semantic hierarchy, accessibility, and clean token architecture.

### 1.3 Purposeful Decoration vs Gratuitous Ornamentation
Visual ornamentation must support the educational experience rather than distract from it:
- **Purpose-Driven Visuals**: Decorative elements SHOULD serve a clear functional purpose, such as:
  - Reinforcing information hierarchy (e.g. subtle section dividers).
  - Aiding wayfinding and orientation (e.g. role badges, breadcrumb icons).
  - Enhancing learning comprehension (e.g. radical stroke order diagrams).
  - Communicating system feedback (e.g. status banners, rating color cues).
- **No Gratuitous Graphics**: Do NOT add decorative graphics, background blobs, or abstract vector illustrations merely to fill empty whitespace.
- **Cultural Elements Non-Mandatory**: Cultural motifs (calligraphy, red seals, paper parchment textures, stone-rubbing patterns) are **NEVER mandatory**. If proposed in the Design Direction Proposal, they must be subtle and purposeful.
- **Proposal Authority**: The approved Design Direction Proposal remains the authoritative determinant of the final visual identity. Anti-template governance must maintain objective domain focus without becoming arbitrary aesthetic dogma.

---

## 2. Typography Hierarchy & Context-Aware CJK Rendering

Chinese characters (Hanzi / 汉字) possess intricate internal stroke compositions. However, **typography must serve information hierarchy and readability rather than a rigid blanket multiplier**:

### 2.1 Font Stack
```css
:root {
  /* Prioritized CJK font stack for crisp stroke rendering */
  --font-hanzi: "Noto Sans SC", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  /* Clean system font for Latin and Vietnamese tone accents */
  --font-latin: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  /* Monospace for pronunciation analysis, IDs, and code */
  --font-mono: SFMono-Regular, Menlo, Monaco, Consolas, "Courier New", monospace;
}
```

### 2.2 Context-Aware Hanzi Sizing Matrix
| Context / Surface | Hanzi Sizing Ratio | Typical Size Range | Rationale |
| :--- | :--- | :--- | :--- |
| **Flashcard Prompt (Hero)** | $2.5\times - 4.0\times$ body | `48px - 64px` | Central visual anchor for memory recall during SRS study. |
| **Radical Grid Cell** | $1.8\times - 2.5\times$ body | `32px - 40px` | Clear visual legibility of Kangxi radical glyphs (1..214). |
| **Vocabulary Item / Catalog** | $1.2\times - 1.4\times$ body | `20px - 26px` | Stroke clarity while preserving clean row height. |
| **Dense Admin / Review Table** | $1.0\times - 1.15\times$ body | `15px - 18px` | Space efficiency and alignment with table metadata. |
| **Forms, Inputs & Chips** | $1.0\times$ body | `14px - 16px` | Consistent form control heights ($40\text{px} - 44\text{px}$). |

Pinyin text SHOULD maintain a slight letter-spacing (`0.03em - 0.05em`) to ensure tone diacritics (`ā, á, ǎ, à, ü`) remain distinctly legible.

---

## 3. Semantic Design Tokens & Palette Roles

> **Governance Notice**: The values below represent the **proposed semantic roles**. Concrete hex codes are established upon approval of the formal Design Direction Proposal. Tokens MUST be referenced via CSS variables in `frontend/css/style.css`.

### 3.1 Color Roles & Proposed Palette Tokens
```css
:root {
  /* Surface & Canvas Tokens (Paper Inspiration) */
  --color-bg-canvas: #FAF9F6;         /* Primary canvas background */
  --color-bg-surface: #FFFFFF;        /* Elevated cards, modals, table surfaces */
  --color-bg-subtle: #F3EFEA;         /* Sidebar navigation, table headers, hover */
  --color-border: #E5E0D8;            /* Delicate structural dividers */
  --color-border-subtle: #ECE8E1;

  /* Typography & Ink Tokens */
  --color-text-primary: #1A1A1A;      /* Deep ink charcoal for primary text & Hanzi */
  --color-text-secondary: #5C5750;    /* Muted ink for Pinyin and supporting labels */
  --color-text-muted: #8C867E;        /* Subtle metadata and placeholders */

  /* Functional Status Roles */
  --color-accent-primary: #C83C23;    /* Cinnabar: Key CTAs, SRS "Again", cultural emphasis */
  --color-accent-success: #2E7D5B;    /* Celadon: Approved status, SRS "Easy", confirmations */
  --color-accent-warning: #D97706;    /* Amber: Pending moderation, SRS "Hard" */
  --color-accent-info: #1E6091;       /* Cobalt: Explanatory notes, SRS "Good" */

  /* Elevation & Geometry (Crisp, Non-Puffy) */
  --radius-sm: 4px;                   /* Form controls, buttons, chips */
  --radius-md: 6px;                   /* Dropdowns, small cards */
  --radius-lg: 8px;                   /* Modals, main panels */

  --shadow-sm: 0 1px 2px rgba(26, 26, 26, 0.04);
  --shadow-md: 0 4px 12px rgba(26, 26, 26, 0.06);
  --shadow-lg: 0 12px 24px rgba(26, 26, 26, 0.08);

  /* Spacing Rhythm (4px / 8px Scale) */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 24px;
  --space-6: 32px;
  --space-7: 48px;
}
```

---

## 4. Role of Bootstrap 5

Bootstrap 5 is an approved **supporting utility tool**:
- **ALLOWED**: Grid layout (`container`, `row`, `col-*`), flexbox utilities (`d-flex`, `justify-content-between`), and accessible JS primitives (`bootstrap.Modal`, `bootstrap.Toast`).
- **MUST NOT**: Rely on Bootstrap's default purple/blue primary theme (`#0d6efd`) or uncustomized button/card appearances.

---

## 5. Shared Component Architecture & Governance

To balance maintainability, consistency, and zero-build Vanilla JS ergonomics, the UI architecture divides components into two distinct tiers:

### 5.1 Shared Primitives (Generic & Reusable)
Shared primitives are reusable UI elements that implement common interaction patterns without domain business logic:
- **Buttons**: Primary CTA, secondary, outline, icon-only button (with required accessible name).
- **Form Fields**: Text input, password toggle, select, textarea, checkbox, switch (with error linking via `aria-describedby`).
- **Modal / Dialog**: Confirmation dialog, detail modal, with accessible focus trapping and keyboard Escape handling.
- **Toast & Status**: Transient notifications (`role="status" aria-live="polite"`).
- **Badges & Tags**: Status indicators (Draft, Pending, Approved, Rejected, Active, Banned).
- **Table Wrappers**: Responsive scrolling containers (`.table-responsive`, with context-sensitive `tabindex="0" role="region"` applied only if the table has no internal focusable controls).
- **Pagination Controls**: Standard page links (`PageResponse` navigation).
- **Three-State UI Elements**: Loading spinner, Empty state placeholder, Error alert with retry button.

### 5.2 Domain Components (Context-Specific)
Domain components represent domain-specific learning entities:
- **Radical Grid Card**: Kangxi radical glyph, index (`radicalId` 1..214), Pinyin, Sino-Vietnamese (`meaningHanViet`), Vietnamese meaning (`meaningVi`), audio/video writing links when present. *(NOTE: Backend database and DTOs do not contain stroke count data; stroke-count display/filtering is EXCLUDED from current scope as `FUTURE / BLOCKED — NO AUTHORITATIVE BACKEND DATA SOURCE`).*
- **Vocabulary Catalog Item**: Hanzi, Pinyin with tones, Sino-Vietnamese, primary gloss, audio button.
- **Lesson Card**: Title, difficulty level, item count, author, moderation badge.
- **SRS Flashcard**: 3D prompt/answer face, radical decomposition tags, response timer.
- **Excel Import Preview Row**: Row number, status icon, validation violation badges.
- **Moderation Action Panel**: Rejection reason textarea, flagged fields selector, approve/reject buttons.

### 5.3 Shared Component Governance Invariants
1. **Behavioral Consistency**: Visually and behaviorally identical UI patterns across pages SHOULD be extracted into shared helper functions or templates under `frontend/js/components/`.
2. **No Page-Level Code Duplication**: Do NOT duplicate identical modal structures, toast containers, or status badges across multiple HTML files.
3. **No Monster Universal Components**: Do NOT force disparate components into a single complex "god component" with dozens of conditional flags when domain behaviors materially differ (e.g. keep Radical Card separate from Flashcard).
4. **Explicit Input Parameterization**: Shared components MUST accept explicit input arguments (e.g. data objects, callbacks) rather than relying on hidden global variables or ambient DOM state.
5. **Strict Token Consumption**: Shared components MUST consume visual properties from the design-token scale (`var(--color-...)`, `var(--space-...)`, `var(--radius-...)`) to maintain global styling consistency.

---

## 6. Source Basis

- **W3C / WAI-ARIA APG**:
  - ARIA Design Patterns: [Authoring Practices Guide](https://www.w3.org/WAI/ARIA/apg/patterns/) — `AUTHORITATIVE`
  - Decorative Images & Icons: [WAI Tutorials](https://www.w3.org/WAI/tutorials/images/decorative/) — `AUTHORITATIVE`
- **MDN Web Docs**:
  - CSS Custom Properties (Variables): [Using CSS custom properties](https://developer.mozilla.org/en-US/docs/Web/CSS/Using_CSS_custom_properties) — `AUTHORITATIVE`
- **Bootstrap Official Documentation**:
  - Bootstrap 5.3 Components: [Bootstrap Components Overview](https://getbootstrap.com/docs/5.3/components/) — `AUTHORITATIVE`
- **Nielsen Norman Group**:
  - Visual Design in UX: Decorative vs. Informational Visuals — `UX RESEARCH`


