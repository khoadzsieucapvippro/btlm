# Vanilla JavaScript Modular Architecture & Directory Structure

## 1. Modular Directory Structure (`frontend/`)

To prevent giant unmaintainable scripts and chaotic global variables, the Vanilla JS frontend is organized into clear ES6+ modular boundaries:

```text
frontend/
├── css/
│   └── style.css                  # Design tokens, CJK typography, custom styling
├── js/
│   ├── api/                       # Network & Backend Communication Layer
│   │   ├── api.js                 # Central Fetch wrapper (envelope, 401, timeout, retry)
│   │   ├── auth-api.js            # Authentication endpoints (/auth/*, /users/profile)
│   │   ├── catalog-api.js         # Radicals & Vocabulary endpoints
│   │   ├── lesson-api.js          # Public, Creator & Admin Lesson endpoints
│   │   ├── notes-api.js           # Personal notes endpoints (/vocabularies/{id}/notes, /notes/*)
│   │   ├── srs-api.js             # SRS review, settings & study stats endpoints
│   │   ├── moderator-api.js       # Moderator review & audit history endpoints
│   │   └── admin-api.js           # User accounts & role management endpoints
│   ├── auth/
│   │   └── auth-state.js          # Session state, token lifecycle, multi-tab sync
│   ├── ui/
│   │   ├── ui.js                  # Toast notifications, modal helpers, 3-state UI
│   │   └── security.js            # DOM sanitization, safe node builders, XSS defense
│   └── pages/                     # Single Controller per HTML Page
│       ├── auth-page.js           # login.html, register.html
│       ├── profile-page.js        # profile.html
│       ├── radicals-page.js       # radicals.html
│       ├── vocabulary-page.js     # vocabulary.html
│       ├── lessons-page.js        # lessons.html, lesson-detail.html
│       ├── notes-page.js          # Contextual notes modal & drawer controller
│       ├── srs-review-page.js     # srs-review.html
│       ├── srs-settings-page.js   # srs-settings.html
│       ├── creator-page.js        # creator-lessons.html, creator-lesson-editor.html, creator-import.html
│       ├── moderator-page.js      # moderator-queue.html, moderator-history.html
│       └── admin-page.js          # admin-accounts.html, admin-roles.html, admin-catalog.html, admin-lessons.html
├── assets/                        # Static SVGs, illustrations (empty states)
└── *.html                         # Semantic HTML5 entry points
```

---

## 2. ES6 Module Import Pattern & Page Lifecycle

Every HTML page imports its dedicated page script as a standard ES module:
```html
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>214 Bộ Thủ Khang Hy | E-Learning Hán Ngữ</title>
  <!-- Verified Subresource Integrity (SRI) for third-party CDN assets -->
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH" crossorigin="anonymous">
  <link rel="stylesheet" href="css/style.css">
</head>
<body>
  <!-- Semantic Header & Main Layout -->
  
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz" crossorigin="anonymous"></script>
  <script type="module" src="js/pages/radicals-page.js"></script>
</body>
</html>
```

### Standard Page Initialization Lifecycle
```javascript
// frontend/js/pages/radicals-page.js
import { CatalogApi } from '../api/catalog-api.js';
import { setComponentState } from '../ui/ui.js';
import { safeAppendElement } from '../ui/security.js';

// Page-local state (isolated to this module)
const state = {
  radicals: [],
  searchQuery: '',
  isLoading: false
};

// 1. DOM Ready Lifecycle
document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
  bindEventHandlers();
  loadRadicals();
});

// 2. Event Binding via Delegation / Event Handlers
function bindEventHandlers() {
  const searchInput = document.getElementById('radicalSearchInput');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      state.searchQuery = e.target.value.trim().toLowerCase();
      applyFiltersAndRender();
    });
  }
}

// 3. Data Loading with 3-State UI
async function loadRadicals() {
  const container = document.getElementById('radicalsGrid');
  setComponentState(container, 'LOADING');

  try {
    const data = await CatalogApi.getRadicals();
    state.radicals = data.items || data;
    applyFiltersAndRender();
  } catch (err) {
    setComponentState(container, 'ERROR', {
      message: err.message,
      onRetry: loadRadicals
    });
  }
}
```

---

## 3. Strict Module Boundaries

- **A Page Module must never access network primitives directly**: Always call a domain API module (`CatalogApi.getRadicals()`), never `fetch()`.
- **A Page Module must never parse JWT tokens directly**: Always import `AuthState` helpers (`AuthState.getUser()`, `AuthState.isAuthenticated()`).
- **Global Scope & Runtime Configuration Invariant**:
  - Application state, authentication state, user data, and page state MUST NOT be stored on `window`. All communication between modules must occur via explicit ES6 `import`/`export`.
  - A runtime configuration object `window.__ENV__` is permitted EXCLUSIVELY for read-only deployment configuration (e.g., `window.__ENV__?.API_BASE_URL`).
  - `window.__ENV__` MUST NOT be used for application state, user credentials, or treated as a security boundary.

---

## 4. Source Basis

- **ECMAScript / WHATWG HTML**:
  - JavaScript Modules: [HTML Spec: Script type module](https://html.spec.whatwg.org/multipage/scripting.html#attr-script-type) — `AUTHORITATIVE`
- **MDN Web Docs**:
  - JavaScript modules: [Guide to JavaScript modules](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Modules) — `AUTHORITATIVE`
  - Window object & Global Scope: [Window](https://developer.mozilla.org/en-US/docs/Web/API/Window) — `AUTHORITATIVE`
- **W3C / WAI-ARIA APG**:
  - Web Accessibility Initiative: [ARIA Authoring Practices Guide](https://www.w3.org/WAI/ARIA/apg/) — `AUTHORITATIVE`
