# Lightweight State Management, Event Delegation & Memory Hygiene

## 1. Lightweight Vanilla JS State Strategy

Because this project explicitly avoids frameworks like React or Redux, state must be managed with disciplined, predictable Vanilla JS patterns:

```text
+-------------------+-------------------------------------------------------------+
| State Scope       | Management Pattern & Boundary                               |
+-------------------+-------------------------------------------------------------+
| 1. Shared Auth    | Central singleton (`AuthState`) broadcasting custom DOM     |
|                   | events (`auth:login`, `auth:logout`, `auth:expired`)         |
+-------------------+-------------------------------------------------------------+
| 2. Page-Local     | Plain object (`const state = {...}`) scoped inside a        |
|                   | page's ES module closure; never exposed on `window`         |
+-------------------+-------------------------------------------------------------+
| 3. Request State  | Explicit flags (`isSubmitting`, `isLoading`, `hasError`)    |
|                   | managed in `try ... finally` blocks                         |
+-------------------+-------------------------------------------------------------+
```

### 1.1 Window & Global State Boundary Invariant
- **Strict Prohibition**: Application state, authentication state, user data, and page-local state MUST NOT be stored on `window`.
- **Runtime Deployment Config**: A single global read-only object `window.__ENV__` is permitted EXCLUSIVELY for deployment-level configuration (such as `window.__ENV__?.API_BASE_URL`).
- `window.__ENV__` is NOT a security boundary and MUST NEVER hold sensitive credentials or dynamic application state.

---

## 2. Shared Auth State Implementation (`frontend/js/auth/auth-state.js`)

```javascript
// frontend/js/auth/auth-state.js

class AuthStateManager {
  constructor() {
    this._token = localStorage.getItem('access_token');
    this._user = this._parseUser(localStorage.getItem('user_info'));
  }

  _parseUser(raw) {
    try {
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  isAuthenticated() {
    return !!this._token;
  }

  getUser() {
    return this._user;
  }

  /**
   * Checks if the authenticated user has a specific role.
   * Backend JSON returns role names without "ROLE_" prefix: "Admin", "Creator", "Moderator", "Learner".
   * This helper normalizes any role string to match JSON roles safely.
   */
  hasRole(roleName) {
    if (!this._user || !this._user.roles || !Array.isArray(this._user.roles)) return false;
    const cleanRole = roleName.startsWith('ROLE_') ? roleName.slice(5) : roleName;
    return this._user.roles.some(r => r.toLowerCase() === cleanRole.toLowerCase());
  }

  setSession(token, userInfo) {
    this._token = token;
    this._user = userInfo;
    localStorage.setItem('access_token', token);
    localStorage.setItem('user_info', JSON.stringify(userInfo));
    window.dispatchEvent(new CustomEvent('auth:change', { detail: { isAuthenticated: true, user: userInfo } }));
  }

  clearSession() {
    this._token = null;
    this._user = null;
    localStorage.removeItem('access_token');
    localStorage.removeItem('user_info');
    window.dispatchEvent(new CustomEvent('auth:change', { detail: { isAuthenticated: false, user: null } }));
  }
}

export const AuthState = new AuthStateManager();
```

---

## 3. Event Delegation Pattern

Never attach event listeners to hundreds of dynamically generated table rows or cards. Instead, attach a **single listener** to the stable parent container:

```javascript
// Good Practice: Single event listener on parent container
const tableBody = document.getElementById('lessonTableBody');

tableBody.addEventListener('click', (event) => {
  const actionBtn = event.target.closest('[data-action]');
  if (!actionBtn) return;

  const action = actionBtn.dataset.action;
  const lessonId = actionBtn.dataset.id;

  if (action === 'edit') {
    openEditModal(lessonId);
  } else if (action === 'delete') {
    confirmDeleteLesson(lessonId);
  } else if (action === 'reorder-up') {
    moveItemUp(lessonId);
  }
});
```

---

## 4. Debounced Search & Request Cancellation

When searching vocabularies or radicals, debounce input events by 300ms and cancel obsolete requests:

```javascript
export function debounce(func, waitMs = 300) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, waitMs);
  };
}

// In search page:
let searchAbortController = null;
let searchRequestId = 0;

const handleSearchInput = debounce(async (query) => {
  if (searchAbortController) {
    searchAbortController.abort(); // Cancel previous in-flight transport request
  }
  searchAbortController = new AbortController();
  const requestId = ++searchRequestId;

  try {
    const results = await CatalogApi.search(query, { signal: searchAbortController.signal });
    if (requestId !== searchRequestId) return; // Discard stale resolved response
    renderResults(results);
  } catch (err) {
    if (err.name !== 'AbortError' && requestId === searchRequestId) {
      showError(err.message);
    }
  }
}, 300);
```

---

## 5. Pragmatic DOM & Resource Lifecycle Management (GAP #9)

In Vanilla JS applications, failing to release long-lived resources created by dynamic components can lead to memory leaks, degraded tab responsiveness, and unexpected background behavior:
- **Scope & Boundary (`ENGINEERING RECOMMENDATION`)**:
  - Vanilla JS is **NOT a framework**: Do NOT invent complex, synthetic component lifecycle hooks or artificial wrappers for simple, static DOM elements.
  - Resource cleanup is required **ONLY when a component or module actually allocates dynamic, long-lived resources** that outlive natural JavaScript garbage collection.
- **Teardown Invariant (`PROJECT CONVENTION`)**: Any component, page controller, or module that creates dynamic, long-lived resources SHOULD clean them up when they are no longer needed (upon modal destruction, view navigation, or widget teardown).

### 5.1 Explicit Resource Cleanup Checklist
1. **Global & Document Event Listeners**:
   - Event listeners attached to `window` or `document` (e.g. `keydown`, `resize`, `storage`, custom event buses) MUST be removed via `removeEventListener()` or an `AbortSignal` when the view is torn down:
     ```javascript
     const pageAbortController = new AbortController();
     window.addEventListener('keydown', handleKeydown, { signal: pageAbortController.signal });
     // Teardown: pageAbortController.abort() automatically removes all listeners bound to this signal!
     ```
2. **Timers & Intervals**:
   - Store timer handles returned by `setInterval` and `setTimeout`. Clear them explicitly via `clearInterval(id)` or `clearTimeout(id)` when navigation occurs or interactions complete.
3. **Active AbortControllers & In-Flight Network Requests**:
   - Abort in-flight `fetch()` calls when a user navigates away or starts a conflicting interaction using `abortController.abort()`.
4. **DOM Observers**:
   - Disconnect active observers (`IntersectionObserver`, `ResizeObserver`, `MutationObserver`) via `observer.disconnect()` before discarding watched elements.
5. **Object URLs**:
   - When generating blob preview URLs for image or Excel file uploads (`URL.createObjectURL(file)`), always revoke them promptly via `URL.revokeObjectURL(url)` once the asset is rendered or replaced.
6. **Custom Event Subscriptions**:
   - Custom pub/sub emitters or `BroadcastChannel` instances must be explicitly unsubscribed or closed (`channel.close()`).
7. **Detached DOM Retention**:
   - Do NOT retain references to removed DOM elements in long-lived module-level arrays or maps. Allow the browser garbage collector to reclaim detached subtrees.

---

## 6. Source Basis

- **MDN Web Docs**:
  - Memory Management & GC: [Memory management](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Memory_management) — `AUTHORITATIVE`
  - EventTarget.addEventListener() with AbortSignal: [addEventListener with signal](https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener#signal) — `AUTHORITATIVE`
  - URL.revokeObjectURL(): [URL: revokeObjectURL()](https://developer.mozilla.org/en-US/docs/Web/API/URL/revokeObjectURL_static) — `AUTHORITATIVE`
  - IntersectionObserver.disconnect(): [IntersectionObserver: disconnect()](https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver/disconnect) — `AUTHORITATIVE`
- **Chromium / web.dev**:
  - Fix Memory Leaks in JavaScript: [Chrome DevTools Memory Profiling](https://developer.chrome.com/docs/devtools/memory-problems/) — `AUTHORITATIVE`
