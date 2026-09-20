# Spaced Repetition (SRS) Flashcard Interaction & UI Engineering

## 1. Interaction Architecture & SM-2 Variant

The frontend SRS review session (`srs-review.html`) implements a focused, keyboard-first flashcard review deck based on the project's SM-2 variant:
- **Rating 1 (Again)**: Reset interval to 0 days, card is pushed to the back of the queue for re-testing within the current session.
- **Rating 2 (Hard)**: Difficult recall, slight interval advancement.
- **Rating 3 (Good)**: Standard correct recall, interval progresses normally.
- **Rating 4 (Easy)**: Immediate confident recall, bonus ease factor.

---

### 2. Logical State Model vs Visual 3D Transform

### 2.1 Logical State as Authoritative Source of Truth
The CSS transform animation (`rotateY(180deg)`) **MUST NOT be the authoritative learning state**:
- The application MUST maintain an explicit logical state in JavaScript:
  ```javascript
  const reviewState = {
    currentCard: null,      // Card data DTO from GET /api/v1/srs/due (DueCardResponse)
    side: 'FRONT',          // Authoritative state: 'FRONT' | 'BACK'
    isFlipping: false,      // Animation in-flight guard
    isSubmitting: false,    // Mutation in-flight guard
    cardPresentedAt: null   // Timestamp for reviewTimeSeconds
  };
  ```
- **Unidirectional Presentation Flow**: Visual classes (e.g. `.is-flipped`) and accessibility states (such as `aria-hidden` on front/back faces) MUST be derived from `reviewState.side`, never by reading CSS computed styles or querying class lists to determine game logic.
- **Screen Reader Synchronization**: When `side === 'FRONT'`, the back face details are marked with `aria-hidden="true"`; when flipped to `'BACK'`, `aria-hidden` is toggled off and focus/announcements reflect the revealed definition. (Note: `aria-expanded` is reserved for disclosure triggers like accordions and dropdown buttons; 3D two-sided flashcards coordinate visibility via `aria-hidden` on each face).

> [!IMPORTANT]
> **DueCardResponse Payload Boundaries (`CRITICAL CONTRACT INVARIANT`)**:
> 1. `strokeCount` is hardcoded to `null` on the backend (`response.setStrokeCount(null)` in `DueCardResponse.fromVocabulary` and `fromRadical`). **Frontend MUST NOT attempt to use `DueCardResponse.strokeCount` to build stroke count filters.**
> 2. `DueCardResponse` does **NOT** contain `audioUrl`, `videoWritingUrl`, `pinyinRaw`, or `radicals[]`.
> 3. If the review UI needs audio playback (`#btnAudio`), stroke order video, or radical decomposition (`#backRadicals`), it must fetch detail via the respective detail endpoint:
>    - Vocabulary cards: `GET /api/v1/vocabulary/{id}` (`VocabularyDetailResponse`)
>    - Radical cards: `GET /api/v1/radicals/{id}` (`RadicalDetailResponse`)
>    - Contextual personal notes (`#backNote`): `GET /api/v1/vocabularies/{vocabId}/notes`

### 2.2 HTML Markup Structure
```html
<div class="flashcard-viewport" tabindex="0" role="region" aria-label="Flashcard Deck">
  <div class="flashcard-container" id="cardContainer" aria-live="polite">
    <div class="flashcard" id="flashcard">
      <!-- Front Face (Prompt) -->
      <div class="flashcard-face flashcard-front" id="frontFace" aria-hidden="false">
        <span class="badge-card-type" id="cardType">Hán Tự</span>
        <div class="character-display" id="charDisplay" lang="zh">你</div>
        <div class="pinyin-prompt" id="pinyinPrompt">nǐ</div>
        <button type="button" class="btn-audio" id="btnAudio" aria-label="Phát âm chữ Hán">
          <i class="bi bi-volume-up" aria-hidden="true"></i>
        </button>
        <p class="flip-hint text-muted">Nhấn <kbd>Space</kbd> hoặc chạm để lật thẻ</p>
      </div>

      <!-- Back Face (Answer) -->
      <div class="flashcard-face flashcard-back" id="backFace" aria-hidden="true">
        <div class="answer-header">
          <span class="hanzi-small" lang="zh" id="backHanzi">你</span>
          <span class="pinyin-detail" id="backPinyin">nǐ</span>
          <span class="sino-vietnamese" id="backSinoViet">Nhĩ</span>
        </div>
        <div class="meaning-display" id="backMeaning">Bạn, anh, chị (ngôi thứ 2 số ít)</div>
        <div class="radicals-decomposition" id="backRadicals">
          <!-- Populated safely via createElement -->
        </div>
        <div class="personal-note-section" id="backNote">
          <!-- Contextual learner note -->
        </div>
      </div>
    </div>
  </div>
</div>
```

### 2.3 CSS 3D Transform & Reduced Motion
```css
.flashcard-container {
  perspective: 1000px;
  width: 100%;
  max-width: 520px;
  min-height: 380px;
  margin: 0 auto;
}

.flashcard {
  position: relative;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.flashcard.is-flipped {
  transform: rotateY(180deg);
}

.flashcard-face {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  border-radius: var(--radius-lg, 16px);
}

.flashcard-back {
  transform: rotateY(180deg);
}

/* Reduced Motion Safety */
@media (prefers-reduced-motion: reduce) {
  .flashcard {
    transition: none;
  }
  .flashcard.is-flipped {
    transform: none;
  }
  .flashcard-front {
    display: block;
  }
  .flashcard.is-flipped .flashcard-front {
    display: none;
  }
  .flashcard-back {
    display: none;
    transform: none;
  }
  .flashcard.is-flipped .flashcard-back {
    display: block;
  }
}
```

---

## 3. Keyboard Controls & Hotkey Safety

Review interactions MUST support robust keyboard operation with explicit safety guards:
- `<Space>` or `<Enter>`: Toggle front/back flip.
- `<1>`: Rate `Again` ($q=0$, interval reset).
- `<2>`: Rate `Hard` ($q=3$).
- `<3>`: Rate `Good` ($q=4$).
- `<4>`: Rate `Easy` ($q=5$).
- `<R>`: Replay audio pronunciation.

### 3.1 Hotkey Safety & Standards Alignment
1. **WCAG Character Key Shortcuts (`NORMATIVE: WCAG 2.2 SC 2.1.4 - Level A`)**:
   - SC 2.1.4 governs single character key shortcuts (such as `1`, `2`, `3`, `4`, `R`).
   - Where character key shortcuts are active, authors must ensure at least one mechanism is met:
     - **Turn off**: A mechanism is available to turn the shortcut off; or
     - **Remap**: A mechanism is available to remap the shortcut to include one or more non-printable key components (e.g. `Alt+1`); or
     - **Active only on focus**: The shortcut for a user interface component is only active when that component has focus.
2. **Project UX Rule (`PROJECT UX RULE / ENGINEERING RECOMMENDATION`)**:
   - The project SHOULD provide visible, accessible pointer/touch equivalent buttons on screen for every review action (Flip, Rate 1..4, Replay Audio). Users on touchscreens or without physical keyboards must never be blocked from completing study sessions.
3. **Input Shield (`PROJECT INVARIANT`)**:
   - Hotkeys MUST NOT trigger while focus is inside any text editing control:
     - `<input>`
     - `<textarea>`
     - `<select>`
     - Any element with `isContentEditable` or `[contenteditable]`
     - Any component intentionally owning keyboard input (e.g. personal note editor).
4. **Browser Native Key Preservation (`ENGINEERING RECOMMENDATION`)**:
   - Hotkey handlers MUST NOT intercept browser or system navigation shortcuts (`Tab`, `Escape`, `F5`, `Ctrl+R`, `Alt+Left`). Handlers must check and ignore events with `ctrlKey`, `metaKey`, or `altKey`.
5. **Logical State Alignment (`PROJECT INVARIANT`)**:
   - Rating hotkeys 1–4 MUST ONLY be active when `reviewState.side === 'BACK'` and `!reviewState.isSubmitting`. Pressing 1–4 while looking at the front face MUST be safely ignored.

```javascript
// Robust Keyboard Binding Pattern with Lifecycle Cleanup Support
export function initReviewKeyboardControls(signal = null) {
  const handler = (e) => {
    // 1. Input Shield: Never trigger shortcuts if typing inside form/editable elements
    const target = e.target;
    const isEditing = target.matches('input, textarea, select, [contenteditable]') || target.isContentEditable;
    if (isEditing) return;

    // 2. Modifiers Shield: Avoid hijacking browser shortcuts (e.g. Ctrl+R, Alt+F4)
    if (e.ctrlKey || e.metaKey || e.altKey) return;

    // 3. Flip Toggle
    if (e.code === 'Space' || e.key === ' ') {
      e.preventDefault();
      toggleCardFlip();
      return;
    }

    // 4. Rating Shortcuts (Only valid on BACK side and when idle)
    if (reviewState.side === 'BACK' && !reviewState.isSubmitting) {
      if (e.key === '1') { e.preventDefault(); handleRatingSubmit(1); }
      else if (e.key === '2') { e.preventDefault(); handleRatingSubmit(2); }
      else if (e.key === '3') { e.preventDefault(); handleRatingSubmit(3); }
      else if (e.key === '4') { e.preventDefault(); handleRatingSubmit(4); }
      else if (e.key === 'r' || e.key === 'R') { e.preventDefault(); playAudio(); }
    }
  };

  const options = signal ? { signal } : {};
  document.addEventListener('keydown', handler, options);
}
```

---

## 4. Flashcard Review Submission & Response Time Tracking (`reviewTimeSeconds`)

The backend requires `reviewTimeSeconds` (camelCase) on mutation `POST /api/v1/srs/review`:
- **API Endpoint**: `POST /api/v1/srs/review` (flat route conforming to `SrsController.java` and `API.md` section 2.6).
- **Request Body Contract (`ReviewCardRequest`)**:
  ```json
  {
    "itemType": "VOCABULARY",
    "itemId": 42,
    "rating": 3,
    "reviewTimeSeconds": 4
  }
  ```
- **Response**: `ApiResponse<DueCardResponse>` returning updated SM-2 interval, easeFactor, and nextReviewAt.
- **Timer Start**: When the front face of a card is presented (`reviewState.cardPresentedAt = Date.now()`).
- **Timer Stop**: When the learner selects a rating (1..4).
- **Elapsed Seconds**: `Math.max(1, Math.round((Date.now() - reviewState.cardPresentedAt) / 1000))`.
- **Sanity Bound**: Cap at 300 seconds (5 minutes) to prevent idle tabs from recording skewing hours:
  ```javascript
  async function submitReview(card, rating) {
    if (reviewState.isSubmitting) return;
    reviewState.isSubmitting = true;

    const rawSeconds = Math.round((Date.now() - reviewState.cardPresentedAt) / 1000);
    const reviewTimeSeconds = Math.min(300, Math.max(1, rawSeconds));

    try {
      const updatedCard = await apiClient('/srs/review', {
        method: 'POST',
        body: JSON.stringify({
          itemType: card.itemType, // "VOCABULARY" or "RADICAL"
          itemId: card.itemId,
          rating: rating,          // 1=Again, 2=Hard, 3=Good, 4=Easy
          reviewTimeSeconds: reviewTimeSeconds
        })
      });
      handleReviewSuccess(updatedCard, rating);
    } catch (err) {
      handleReviewError(err);
    } finally {
      reviewState.isSubmitting = false;
    }
  }
  ```

---

## 5. Session Complete & Summary View

When the card queue reaches 0:
1. Transition gracefully from the card deck to the **Session Summary Screen**.
2. Display:
   - Total cards reviewed.
   - Breakdown of ratings: `Again` count, `Hard` count, `Good` count, `Easy` count.
   - Average response time.
3. Provide actions: `Go to Dashboard`, `Review Another Lesson`.

---

## 6. Source Basis

- **W3C / WAI WCAG 2.2**:
  - SC 2.1.1 Keyboard (Level A) & SC 2.1.4 Character Key Shortcuts (Level A): [Understanding SC 2.1.4](https://www.w3.org/WAI/WCAG22/Understanding/character-key-shortcuts.html) — `AUTHORITATIVE`
  - SC 2.2.1 Timing Adjustable & SC 2.3.3 Animation from Interactions: [Understanding SC 2.3.3](https://www.w3.org/WAI/WCAG22/Understanding/animation-from-interactions.html) — `AUTHORITATIVE`
  - WAI-ARIA Authoring Practices Guide: [Keyboard Interface Design Principles](https://www.w3.org/WAI/ARIA/apg/practices/keyboard-interface/) — `AUTHORITATIVE`
- **MDN Web Docs**:
  - KeyboardEvent key & code: [KeyboardEvent](https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent) — `AUTHORITATIVE`
  - Event.target and element matching: [Element.matches()](https://developer.mozilla.org/en-US/docs/Web/API/Element/matches) — `AUTHORITATIVE`
  - CSS transforms & accessibility: [transform](https://developer.mozilla.org/en-US/docs/Web/CSS/transform) — `AUTHORITATIVE`
