# Anti-Template Governance & Objective Design Review Criteria

## 1. The Design-Before-Code Rule

Before implementing any major frontend module, the engineering agent MUST formulate a concise **Design Direction Proposal** establishing:
1. **Product Visual Direction**: Focused, scholarly typography and pedagogical clarity tailored for Chinese language learning (exact visual direction decided in Design Direction Proposal).
2. **Typography Hierarchy**: Font family stack, Hanzi glyph sizing, Pinyin tone rendering.
3. **Semantic Color Roles**: Surface canvas, ink charcoal, accent vermilion (CTAs/Again), and celadon (Approved/Easy).
4. **Spacing & Elevation Scale**: 4px/8px geometric grid, subtle elevation shadows.
5. **Surface Distinction**: Intentional layout differences between Learner, Creator, Moderator, and Admin.
6. **Accessibility & Responsive Strategy**: WCAG 2.2 AA targets, mobile-first touch ergonomics.

---

## 2. Objective Criteria for Design Governance (No Subjective Rejections)

> **Governance Principle**: "Anti-Template" is **NOT a subjective aesthetic preference** used to arbitrarily reject implementations.
> It is an **OBJECTIVE ENGINEERING CRITERIA** designed to prevent unconsidered generative AI tropes that compromise usability and domain authenticity.

### 2.1 Objective Review Criteria for Generic Template Drift
An implementation is flagged under Design Governance ONLY when it exhibits unconsidered template drift compromising domain utility:

```text
Objective Anti-Pattern                      Measurable Evidence & Contextual Evaluation
+------------------------------------------+------------------------------------------+
| 1. Generic SaaS Gradient Tropes          | Use of default purple-to-blue linear     |
|                                          | gradients (#667eea -> #764ba2) as generic|
|                                          | backgrounds. (Contextual subtle gradients|
|                                          | with UI rationale are NOT flagged).      |
+------------------------------------------+------------------------------------------+
| 2. Unmotivated Puffy Geometry            | Regular content containers using         |
|                                          | excessive rounded geometry without       |
|                                          | visual/interaction rationale. Large      |
|                                          | radius (pills, chips, circular controls) |
|                                          | is permitted with clear design rationale.|
+------------------------------------------+------------------------------------------+
| 3. Indiscriminate Glassmorphism          | Slapping `backdrop-filter: blur(...)` on |
|                                          | opaque panels, causing contrast failures |
+------------------------------------------+------------------------------------------+
| 4. Uncustomized Bootstrap Defaults       | Relying on raw Bootstrap primary blue    |
|                                          | (#0d6efd) rather than project tokens     |
+------------------------------------------+------------------------------------------+
| 5. Surface Incoherence                   | Admin table screens copying the airy     |
|                                          | reading layout of Learner flashcards     |
+------------------------------------------+------------------------------------------+
| 6. Unreadable CJK Typography             | Hanzi displayed smaller than 14px in     |
|                                          | study cards, obscuring radical strokes   |
+------------------------------------------+------------------------------------------+
```

> **Contextual Evaluation Principle**:
> - Excessive rounded geometry SHOULD be avoided for regular content containers.
> - Large radius MAY be used when there is visual or interaction rationale (e.g. pill badges, circular avatars, search chips).
> - Review agents MUST NOT reject a component mechanically based solely on a specific border-radius number.
> - Purple/blue gradients are flagged ONLY when they reflect generic/default SaaS/AI template behavior, not in every instance of a gradient.
> - Anti-template governance MUST remain objective, contextual, and grounded in domain usability.

---

## 3. Disciplined Use of Bootstrap 5

Bootstrap 5 is an approved **supporting utility and layout framework**, NOT the visual identity of the project:
- **USE BOOTSTRAP FOR**:
  - Flexbox and Grid classes (`row`, `col-*`, `d-flex`, `align-items-center`).
  - Accessible modal and toast JavaScript primitives (`bootstrap.Modal`, `bootstrap.Toast`).
  - Breakpoint display utilities (`d-none d-lg-block`).
- **DO NOT USE BOOTSTRAP FOR**:
  - Raw uncustomized primary palette (`#0d6efd`).
  - Generic Bootstrap buttons without custom styling tokens.
  - Making the app look like an out-of-the-box Bootstrap demo template.

---

## 4. Purposeful Decoration & Non-Mandatory Cultural Elements

### 4.1 Purposeful Decoration Governance
Visual ornaments, accents, and decorations are NOT inherently bad. Clean engineering welcomes purposeful decoration:
- **PERMITTED & ENCOURAGED**:
  - Visual ornaments serving **Information Hierarchy** (differentiating active review targets from background context).
  - Decorative cues aiding **Wayfinding** (progress breadcrumbs in multi-step import, deck position indicators).
  - Accents enhancing **Pedagogical Comprehension** (radical highlighting, stroke-order decomposition guides).
  - Brand accents reinforcing a **Scholarly Learning Identity** (tailored seal accents, quiet borders, thematic tones).
  - Interaction cues providing **Feedback** (subtle rating pulse, flip transforms).
- **FORBIDDEN**:
  - Gratuitous geometric blobs, meaningless floating blur orbs, or arbitrary glassmorphism behind opaque cards.
  - Cluttering decorations that diminish WCAG 1.4.3 contrast or push essential study controls below the fold.

### 4.2 Non-Mandatory Nature of Cultural Motifs
- Traditional Chinese cultural motifs (e.g., ink wash backgrounds, seal stamps, calligraphy flourishes, bamboo flourishes) are **PERMITTED** when aligned with the task's Design Direction Proposal.
- **NEVER MANDATORY**: The absence of cultural or traditional motifs is **NEVER a valid basis to reject an implementation**.
- A modern, clean, high-contrast pedagogical aesthetic without traditional motifs is fully valid and acceptable.
- The visual direction is established by the upfront Design Direction Proposal, not by reviewer subjective taste.

---

## 5. Source Basis

- **UX RESEARCH**:
  - Nielsen Norman Group (NN/g): [Visual Hierarchy in UX](https://www.nngroup.com/articles/visual-hierarchy-ux-definition/) & [Aesthetic-Usability Effect](https://www.nngroup.com/articles/aesthetic-usability-effect/) — `UX RESEARCH`
- **AUTHORITATIVE**:
  - W3C Web Content Accessibility Guidelines (WCAG) 2.2: SC 1.4.3 Contrast (Minimum) & SC 1.4.11 Non-text Contrast: [Understanding SC 1.4.11](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html) — `AUTHORITATIVE`
  - Bootstrap 5.3 Documentation: [Design tokens & utility classes](https://getbootstrap.com/docs/5.3/customize/color/) — `AUTHORITATIVE`


