# Three-State UI Architecture, Forms & Error Handling

## 1. The Mandatory Three UI States

Every API-driven screen or component MUST explicitly implement three operational states. No screen may ever freeze, remain blank, or silently fail on network latency or zero records:

```text
       ┌────────────────┐
       │ 1. LOADING     │  (Spinner / Skeleton, Buttons Disabled)
       └───────┬────────┘
               │
        [API Completes]
         /           \
   (Success)       (Failure)
       /               \
┌──────────────┐  ┌──────────────┐
│ 2. DATA /    │  │ 3. ERROR     │  (Clear Message + Retry Button)
│    EMPTY     │  └──────────────┘
└──────────────┘
```

---

## 2. Implementation Pattern for States

### Reusable State Helper Pattern (`frontend/js/ui/ui.js`)
```javascript
// frontend/js/ui/ui.js

export function setComponentState(container, state, options = {}) {
  // Clear previous state elements
  container.querySelectorAll('.ui-state-element').forEach(el => el.remove());
  
  if (state === 'LOADING') {
    const spinner = document.createElement('div');
    spinner.className = 'ui-state-element text-center py-5';

    const spinnerBorder = document.createElement('div');
    spinnerBorder.className = 'spinner-border text-cinnabar';
    spinnerBorder.setAttribute('role', 'status');

    const srOnly = document.createElement('span');
    srOnly.className = 'visually-hidden';
    srOnly.textContent = 'Đang tải dữ liệu...';
    spinnerBorder.appendChild(srOnly);

    const msg = document.createElement('p');
    msg.className = 'text-muted mt-2';
    msg.textContent = options.message || 'Đang tải dữ liệu...';

    spinner.appendChild(spinnerBorder);
    spinner.appendChild(msg);
    container.appendChild(spinner);

  } else if (state === 'EMPTY') {
    const empty = document.createElement('div');
    empty.className = 'ui-state-element text-center py-5 text-muted';

    const icon = document.createElement('div');
    icon.className = 'empty-icon fs-1 mb-2';
    icon.textContent = '📭';

    const title = document.createElement('h5');
    title.textContent = options.title || 'Không tìm thấy dữ liệu';

    const desc = document.createElement('p');
    desc.className = 'small';
    desc.textContent = options.description || 'Chưa có bản ghi nào phù hợp với yêu cầu.';

    empty.appendChild(icon);
    empty.appendChild(title);
    empty.appendChild(desc);

    if (options.actionBtn) {
      // Structured safe button: { text, onClick, className }
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = options.actionBtn.className || 'btn btn-sm btn-outline-primary mt-2';
      btn.textContent = options.actionBtn.text || 'Hành động';
      if (typeof options.actionBtn.onClick === 'function') {
        btn.addEventListener('click', options.actionBtn.onClick);
      }
      empty.appendChild(btn);
    }

    container.appendChild(empty);

  } else if (state === 'ERROR') {
    const errorBox = document.createElement('div');
    errorBox.className = 'ui-state-element alert alert-danger my-3';
    errorBox.setAttribute('role', 'alert');
    
    const title = document.createElement('strong');
    title.textContent = options.title || 'Đã xảy ra lỗi!';
    
    const msg = document.createElement('div');
    msg.textContent = options.message || 'Không thể tải dữ liệu từ máy chủ.';
    
    errorBox.appendChild(title);
    errorBox.appendChild(msg);

    if (options.onRetry) {
      const retryBtn = document.createElement('button');
      retryBtn.type = 'button';
      retryBtn.className = 'btn btn-sm btn-outline-danger mt-2';
      retryBtn.textContent = 'Thử lại';
      retryBtn.addEventListener('click', options.onRetry);
      errorBox.appendChild(retryBtn);
    }

    container.appendChild(errorBox);
  }
}
```

---

## 3. Form Submission & Double-Submit Protection

### Race Condition & Re-Submission Prevention Rule
All form submissions must be wrapped in an `isSubmitting` flag and execute inside a `try ... finally` block:

```javascript
let isSubmitting = false;

async function handleFormSubmit(event) {
  event.preventDefault();
  const form = event.target;

  if (isSubmitting) return; // Block double clicks / race conditions

  // Client-side HTML5 validation check
  if (!form.checkValidity()) {
    form.reportValidity();
    return;
  }

  const submitBtn = form.querySelector('button[type="submit"]');
  const originalBtnText = submitBtn.innerHTML;

  isSubmitting = true;
  submitBtn.disabled = true;
  submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang xử lý...';
  clearFormErrors(form);

  try {
    const formData = extractFormData(form);
    await LessonApi.create(formData);
    showToast('Tạo bài học thành công!', 'success');
    window.location.href = '/creator-lessons.html';

  } catch (err) {
    if (err.errors && err.errors.length > 0) {
      // Map field-level validation errors from Spring Boot GlobalExceptionHandler
      displayFieldErrors(form, err.errors);
    } else {
      showFormAlert(form, err.message);
    }
  } finally {
    isSubmitting = false;
    submitBtn.disabled = false;
    submitBtn.innerHTML = originalBtnText;
  }
}

/**
 * Canonical Spring Boot validation error parser.
 * GlobalExceptionHandler returns ApiResponse.errors as List<String>:
 * ["title: Tiêu đề không được để trống", "emailOrPhone: Email hoặc số điện thoại không hợp lệ"]
 * This helper normalizes raw error strings into an array of { field, message } pairs,
 * preserving error ordering and handling unmapped errors where field is null.
 *
 * @param {Array<string|object>} rawErrors
 * @returns {Array<{ field: string|null, message: string }>}
 */
export function parseFieldErrors(rawErrors = []) {
  if (!Array.isArray(rawErrors)) return [];
  return rawErrors.map(err => {
    if (typeof err === 'string') {
      const colonIdx = err.indexOf(':');
      if (colonIdx !== -1) {
        return {
          field: err.slice(0, colonIdx).trim(),
          message: err.slice(colonIdx + 1).trim()
        };
      }
      return { field: null, message: err.trim() };
    }
    if (err && typeof err === 'object') {
      return {
        field: err.field || null,
        message: err.message || JSON.stringify(err)
      };
    }
    return { field: null, message: String(err) };
  });
}

/**
 * Groups parsed errors by field name for direct key lookup: { [field]: string[] }
 * Unmapped errors without a field are collected under key '_general'.
 *
 * @param {Array<string|object>} rawErrors
 * @returns {Record<string, string[]>}
 */
export function groupFieldErrors(rawErrors = []) {
  const parsed = parseFieldErrors(rawErrors);
  const grouped = {};
  for (const { field, message } of parsed) {
    const key = field || '_general';
    if (!grouped[key]) grouped[key] = [];
    grouped[key].push(message);
  }
  return grouped;
}

function displayFieldErrors(form, rawErrors) {
  clearFormErrors(form);
  const parsedErrors = parseFieldErrors(rawErrors);
  const unmappedErrors = [];

  parsedErrors.forEach(({ field, message }) => {
    const input = field ? form.querySelector(`[name="${field}"]`) : null;
    if (input) {
      input.classList.add('is-invalid');
      input.setAttribute('aria-invalid', 'true');
      
      const errorId = `${input.id || field}-error`;
      const feedback = document.createElement('div');
      feedback.id = errorId;
      feedback.className = 'invalid-feedback d-flex align-items-center gap-1';
      // WCAG SC 1.4.1: Do not rely on color alone; include error icon and message
      const icon = document.createElement('i');
      icon.className = 'bi bi-exclamation-circle-fill';
      icon.setAttribute('aria-hidden', 'true');
      const textSpan = document.createElement('span');
      textSpan.textContent = message; // Safe textContent avoids DOM XSS (CWE-79)
      feedback.appendChild(icon);
      feedback.appendChild(textSpan);
      
      input.setAttribute('aria-describedby', errorId);
      input.parentNode.appendChild(feedback);
    } else {
      unmappedErrors.push(message);
    }
  });

  if (unmappedErrors.length > 0) {
    showFormAlert(form, unmappedErrors.join('; '));
  }
}

function showFormAlert(form, message) {
  const alert = document.createElement('div');
  alert.className = 'form-alert alert alert-danger d-flex align-items-center gap-2 mb-3';
  alert.setAttribute('role', 'alert');
  
  const icon = document.createElement('i');
  icon.className = 'bi bi-exclamation-triangle-fill flex-shrink-0';
  icon.setAttribute('aria-hidden', 'true');
  
  const span = document.createElement('span');
  span.textContent = message;
  
  alert.appendChild(icon);
  alert.appendChild(span);
  form.prepend(alert);
}

function clearFormErrors(form) {
  form.querySelectorAll('.is-invalid').forEach(el => {
    el.classList.remove('is-invalid');
    el.removeAttribute('aria-invalid');
    el.removeAttribute('aria-describedby');
  });
  form.querySelectorAll('.invalid-feedback').forEach(el => el.remove());
  form.querySelectorAll('.form-alert').forEach(el => el.remove());
}
```

---

## 4. Async Stale Response & Race Condition Management (GAP #7)

In asynchronous, search-as-you-type, or multi-filter UIs, network responses may arrive out of order:
- **The Stale Overwrite Threat**: User launches Request A (query `sh`). User quickly changes to query `shu` (Request B launched). Request B returns fast with 2 items. Later, Request A returns slow with 10 items. If unhandled, the UI reverts to query A's results while the input displays `shu`.
- **Core Invariant (`PROJECT INVARIANT`)**: Older asynchronous responses **MUST NOT overwrite newer UI state**.

### 4.1 Discrimination of Network Failure Types
Implementation agents MUST clearly differentiate between distinct network outcomes:
1. **Cancellation (`AbortError`)**: Client-side cancellation triggered via `AbortController.abort()`. Not an error; handled silently without showing error toasts.
2. **Stale Response Prevention**: A request that completes successfully at the transport layer but whose response is discarded because a newer request has already superseded it.
3. **Timeout (`TimeoutError`)**: Request exceeded client-side timeout duration (e.g. 15s). Handled with a retryable alert.
4. **Transport Failure (`TypeError: Failed to fetch`)**: Network loss, DNS resolution failure, or server unreachable. Handled with offline/connection banner.
5. **HTTP Error (`ApiError` 4xx/5xx)**: Server responded with non-2xx status. Handled by mapping errors to UI components or redirects.

### 4.2 Approved Mitigation Techniques (`ENGINEERING RECOMMENDATION`)
> [!IMPORTANT]
> **AbortController Limits**: Calling `abortController.abort()` cancels in-flight transport requests, saving bandwidth. However, **`AbortController` does NOT automatically solve all race conditions alone** (for example, if Request A completed network transfer and started JSON processing before abort was executed, or if multiple async handlers run concurrently).
> Therefore, client code SHOULD combine transport cancellation with logical sequence tokens or query verification.

1. **Unified Search-as-You-Type Pattern (AbortController + Request Sequence)**:
   Combine transport cancellation with sequence counter verification to ensure complete race-condition immunity:
   ```javascript
   let currentSearchAbortController = null;
   let latestSearchRequestId = 0;

   async function searchVocabulary(query) {
     // Cancel previous in-flight network request
     if (currentSearchAbortController) {
       currentSearchAbortController.abort();
     }
     currentSearchAbortController = new AbortController();

     // Generate sequence token for this dispatch
     const requestId = ++latestSearchRequestId;
     
     try {
       const results = await apiClient(`/vocabulary?search=${encodeURIComponent(query)}`, {
         signal: currentSearchAbortController.signal
       });
       
       // Guard: discard if superseded by a newer query response
       if (requestId !== latestSearchRequestId) {
         return;
       }
       renderResults(results);
     } catch (err) {
       if (err.name === 'AbortError' || err.code === 'ABORTED') return; // Expected cancellation; ignore
       // Guard error display against stale superseded failures
       if (requestId === latestSearchRequestId) {
         showError(err.message);
       }
     }
   }
   ```
2. **Request Sequence Token for Multi-Filter Views (`latestRequestId`)**:
   ```javascript
   let latestFilterRequestId = 0;

   async function fetchFilteredData(params) {
     const requestId = ++latestFilterRequestId;
     const data = await apiClient(`/lessons?${params}`);
     if (requestId !== latestFilterRequestId) {
       return; // Superseded by a newer request; discard stale payload
     }
     renderLessons(data);
   }
   ```
3. **Current-Query / Active-View Verification**: Verify that the response's original parameters match the active component state before touching the DOM (especially when users switch tabs or views before a fetch resolves).

---

## 5. Pagination, Filter & Sort State Governance (GAP #8)

For all catalog and list surfaces (Kangxi Radicals, Vocabulary Search, Lessons Catalog, Creator Drafts, Moderation Queue, Admin Accounts):

### Strict State Invariants
1. **Actual REST Response DTO Contract is Authoritative (`PROJECT INVARIANT`)**:
   - The project's authoritative pagination contract is the backend REST DTO `com.elearning.dto.response.PageResponse<T>` specified in `.agents/API.md` Section 1.3:
     ```json
     {
       "page": 0,
       "size": 20,
       "totalElements": 214,
       "totalPages": 11,
       "items": [ ... ]
     }
     ```
   - **No Internal Framework Inference**: Frontend code MUST use these exact serialized JSON keys (`page`, `size`, `totalElements`, `totalPages`, `items`). Implementation agents MUST NOT infer JSON field names directly from internal Spring Data `Page` methods (such as `content` or `number`).
2. **Never Infer Totals from Array Length (`PROJECT INVARIANT`)**: The UI MUST NOT assume total record count from `items.length` of the current page.
3. **Query Mutation Resets Page Index (`PROJECT CONVENTION`)**: Whenever the user changes a search term, filter dropdown, or sort column, the pagination state SHOULD automatically reset to the first page (`page = 0`).
4. **URL / State Synchronization (`ENGINEERING RECOMMENDATION`)**: Filter, sort, and page parameters SHOULD be reflected in `URLSearchParams` where deep linking is required, enabling reliable page refresh and back-button navigation.
5. **Atomic Query Superseding (`PROJECT INVARIANT`)**: Dispatching a new query with updated filters MUST invalidate in-flight or cached requests from previous filter states.

---

## 6. Source Basis

- **W3C / WAI WCAG 2.2**:
  - SC 1.4.1 Use of Color (Level A): [Understanding SC 1.4.1](https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html) — `AUTHORITATIVE`
  - SC 3.3.1 Error Identification (Level A) & SC 3.3.3 Error Suggestion (Level AA): [Understanding SC 3.3.1](https://www.w3.org/WAI/WCAG22/Understanding/error-identification.html) — `AUTHORITATIVE`
  - WAI-ARIA 1.2: `aria-invalid` & `aria-describedby`: [Accessible Forms](https://www.w3.org/WAI/tutorials/forms/notifications/) — `AUTHORITATIVE`
- **WHATWG Fetch & MDN Web Docs**:
  - Fetch Standard & AbortController: [AbortController API](https://developer.mozilla.org/en-US/docs/Web/API/AbortController) — `AUTHORITATIVE`
  - URLSearchParams: [URLSearchParams](https://developer.mozilla.org/en-US/docs/Web/API/URLSearchParams) — `AUTHORITATIVE`
  - Form validation with JavaScript: [Validating forms](https://developer.mozilla.org/en-US/docs/Learn/Forms/Form_validation) — `AUTHORITATIVE`
- **Project REST API Contract**:
  - `com.elearning.dto.response.PageResponse` & `.agents/API.md` Section 1.3 — `PROJECT INVARIANT / AUTHORITATIVE CONTRACT`
- **Bootstrap Official Documentation**:
  - Form Validation: [Validation & Feedback](https://getbootstrap.com/docs/5.3/forms/validation/) — `AUTHORITATIVE`
