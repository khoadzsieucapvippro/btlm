# DOM-Based XSS Defense, Sink Auditing & Untrusted Data Rendering

> **Authority**: `OWASP ASVS 5.0 (V5: Validation, Sanitization and Encoding)` & `OWASP DOM-Based XSS Prevention Cheat Sheet`  
> **Target Scope**: Vanilla JS ES6+ modules (`frontend/js/**/*.js`) and dynamic DOM manipulation.

---

## 1. Threat Model: Untrusted Data Boundaries

In this application, all data originating from user input, external files, query strings, hash parameters, or server databases MUST be classified as **UNTRUSTED** for DOM execution contexts:

1. **User Personal Notes**: Freeform text entered by learners (`PersonalNoteRequest.content`).
2. **Chinese Vocabulary & Radicals**: Hanzi, Pinyin, Sino-Vietnamese readings, meanings, example sentences.
3. **Lesson Metadata**: Lesson titles, author emails, and timestamps.
4. **Moderation Feedback**: Rejection reasons (`rejectionReason`), flagged fields JSON strings, and review audit logs.
5. **Excel Spreadsheets**: Vocabulary rows parsed from uploaded `.xlsx` files.
6. **User Profiles**: Full names, usernames, phone numbers, and profile avatar URLs.
7. **URL Parameters**: Search terms, query strings (`?search=...`, `?page=...`), and hash fragments.

---

## 2. The 4-Tier DOM Security Classification

Every DOM manipulation pattern in this project belongs strictly to one of four tiers:

| Tier | Category | DOM APIs & Patterns | Engineering Rule |
|:---|:---|:---|:---|
| **Tier 1** | **SAFE BY DEFAULT** | `element.textContent`<br>`document.createElement(tag)`<br>`document.createTextNode(str)`<br>`element.replaceChildren(...)`<br>`container.innerHTML = ''` (clearing)<br>`element.classList.add/remove/toggle`<br>`element.setAttribute('data-*', value)` | **PREFERRED**: Use for 100% of standard dynamic rendering. Cannot execute script context. |
| **Tier 2** | **SAFE WITH VALIDATION** | `element.setAttribute('href', url)`<br>`element.setAttribute('src', url)`<br>`window.location.href = path`<br>`window.location.replace(path)` | **PERMITTED WITH WHITELIST**: Requires strict protocol validation (`http:`, `https:`, relative `/`, `#`). Never accept `javascript:` or `data:text/html`. |
| **Tier 3** | **SAFE WITH SANITIZATION** | Rich HTML rendering via an established, hardened sanitization library (e.g. DOMPurify). | **RESTRICTED**: Only permitted if business explicitly requires rich HTML formatting. In this platform, all content is plain text / structured fields; HTML input is **PROHIBITED**. |
| **Tier 4** | **DANGEROUS / PROHIBITED** | Concatenating variables into `innerHTML`<br>`outerHTML`<br>`insertAdjacentHTML` with dynamic data<br>`document.write()` / `document.writeln()`<br>`eval()` / `new Function()`<br>`setTimeout(string)` / `setInterval(string)`<br>Inline event attributes (`onclick`, `onerror`)<br>Dynamic CSS strings (`element.style = ...`)<br>`javascript:` or `vbscript:` pseudo-protocols | **STRICTLY PROHIBITED**: Any presence in git diff triggers immediate PR rejection. |

---

## 3. Dangerous DOM Sinks & Approved Safe Alternatives

| Dangerous Execution Sink | Attack Vector | Approved Safe Alternative |
|:---|:---|:---|
| `element.innerHTML = '<div>' + text + '</div>'` | Script injection via HTML markup (`<img onerror=...>`, `<script>`) | `element.textContent = text`<br>Or build nodes via `document.createElement()`. |
| `element.outerHTML = ...` | Replaces element with unparsed attacker markup | `element.replaceWith(safeElement)` |
| `element.insertAdjacentHTML('beforeend', str)` | Injects unescaped markup into adjacent DOM positions | `element.appendChild(safeElement)` |
| `document.write(str)` / `document.writeln(str)` | Overwrites document stream with arbitrary HTML | Never use `document.write`. Build nodes with `createElement()`. |
| `<button onclick="deleteItem('${id}')">` | Arbitrary JS execution inside event handler attributes | `button.addEventListener('click', () => deleteItem(id))` or Event Delegation with `data-action="delete"`. |
| `<a href="${userUrl}">` or `<iframe src="${url}">` | `javascript:alert(document.cookie)` protocol execution | Validate URL with context-sensitive sanitizers (`sanitizeNavigationUrl()` for href, `sanitizeResourceUrl()` for src). |
| `window.location.href = returnUrl` | Open redirect or `javascript:` execution via query parameters | Validate return URL is strictly relative (`/path`) or matches same-origin. |
| `element.style.cssText = userCss` | CSS injection breaking layout or exploiting expression sinks | Use `classList`, constrained individual properties, or validated custom properties. |
| `setTimeout("doSomething(" + id + ")", 500)` | Implicit `eval()` string execution in timer | `setTimeout(() => doSomething(id), 500)` with closure/arrow function. |

---

## 4. Pragmatic `innerHTML` & Safe Text Rendering Policy

Ordinary dynamic text rendering MUST use:
- `element.textContent = text`
- `document.createElement()` + `appendChild()` / `append()`
- `document.createTextNode()`
- `element.replaceChildren(...)`
- `<template>` element cloning (`template.content.cloneNode(true)`)

```text
ALLOWED (Safe)                              PROHIBITED (Vulnerable)
+------------------------------------------+------------------------------------------+
| • Clearing containers:                   | • Interpolating variables into HTML:     |
|   container.innerHTML = '';              |   box.innerHTML = `<p>${userNote}</p>`;  |
| • Rendering 100% static trusted markup:  | • Concatenating API responses:           |
|   spinner.innerHTML =                    |   el.innerHTML = '<b>' + data.title + '</b>'
|     '<span class="spinner-border"></span>| • Inline event handlers:                 |
| • Trusted UI icons from local assets     |   btn.innerHTML = `<button onclick="..">`|
| • Static modal templates with zero data  | • escapeHtml(val) into innerHTML strings |
+------------------------------------------+------------------------------------------+
```

### 4.1 Safe vs Dangerous Dynamic CSS Manipulation
CSS manipulation must distinguish safe property bindings from dangerous string injections:
- **SAFE**:
  - `element.classList.add(...)` / `remove(...)` / `toggle(...)` (recommended for all state and theme changes).
  - Constrained individual style properties with validated values (e.g. `element.style.width = `${Math.max(0, Math.min(100, pct))}%``).
  - Validated CSS custom properties (e.g. `element.style.setProperty('--study-progress', `${pct}%`)`).
- **DANGEROUS**:
  - Arbitrary `element.style.cssText = userString`.
  - Arbitrary `element.setAttribute('style', userString)`.
  - Attacker-controlled CSS declarations or unvalidated selector/style block injection.

---

## 5. Approved Security Helper Library (`frontend/js/ui/security.js`)

```javascript
// frontend/js/ui/security.js

/**
 * Encodes text into safe HTML entity representation using browser-native textContent.
 * 
 * IMPORTANT USAGE RESTRICTION:
 * escapeHtml() has a narrowly scoped encoding purpose. It is NOT a substitute for
 * safe contextual DOM rendering. NEVER adopt the anti-pattern:
 *   element.innerHTML = '<span>' + escapeHtml(value) + '</span>';
 * Ordinary dynamic rendering MUST use textContent, createElement(), or replaceChildren().
 * 
 * @param {*} str - input string
 * @returns {string} safely encoded string
 */
export function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  const div = document.createElement('div');
  div.textContent = String(str);
  return div.innerHTML;
}

/**
 * Contextual URL Validation per Sink (OWASP ASVS 5.0 V5.1.4, V5.2.3 & WHATWG URL Standard).
 * OWASP does not mandate one universal whitelist; URL policies must be contextual per sink:
 * - Executable schemes (javascript:, vbscript:) are strictly prohibited across all sinks.
 * - Navigation Sink (anchor href, window.location): allows http:, https:, mailto:, tel:, and safe relative paths.
 *   Rejects all data: URLs (such as data:text/html) to prevent script execution on navigation.
 * - Resource Sink (img src, audio src): allows http:, https:, safe relative paths, and safe raster image data URIs.
 * 
 * @param {string} url - candidate navigation URL
 * @returns {string} safe URL or '#'
 */
export function sanitizeNavigationUrl(url) {
  if (!url) return '#';
  const trimmed = String(url).trim();
  
  // Safe relative paths & fragments
  if ((trimmed.startsWith('/') && !trimmed.startsWith('//')) ||
      trimmed.startsWith('./') ||
      trimmed.startsWith('#')) {
    return trimmed;
  }
  
  try {
    const parsed = new URL(trimmed, window.location.origin);
    const protocol = parsed.protocol.toLowerCase();
    // Allow safe navigation protocols; reject javascript:, vbscript:, data:
    if (['http:', 'https:', 'mailto:', 'tel:'].includes(protocol)) {
      return parsed.href;
    }
  } catch (e) {
    // Malformed URL
  }
  return '#';
}

/**
 * Validates a media / resource URL (for img src or iframe src).
 * Strictly restricts schemes to http:, https:, safe relative paths, or safe raster image data URIs.
 * Rejects mailto:, tel:, and all executable schemes.
 * 
 * @param {string} url - candidate resource URL
 * @returns {string} safe URL or 'about:blank'
 */
export function sanitizeResourceUrl(url) {
  if (!url) return 'about:blank';
  const trimmed = String(url).trim();
  
  // Safe relative paths
  if ((trimmed.startsWith('/') && !trimmed.startsWith('//')) ||
      trimmed.startsWith('./')) {
    return trimmed;
  }
  
  // Safe raster image data URIs (PNG, JPEG, WebP, GIF only; SVG excluded due to script-carrier risk)
  if (/^data:image\/(png|jpeg|jpg|webp|gif);base64,[A-Za-z0-9+/=]+$/i.test(trimmed)) {
    return trimmed;
  }

  try {
    const parsed = new URL(trimmed, window.location.origin);
    const protocol = parsed.protocol.toLowerCase();
    if (['http:', 'https:'].includes(protocol)) {
      return parsed.href;
    }
  } catch (e) {
    // Malformed URL
  }
  return 'about:blank';
}

/**
 * Programmatically constructs a safe DOM element with sanitized attributes and child nodes.
 * @param {string} tag - HTML tag name (e.g. 'div', 'button', 'span')
 * @param {Object} options - configuration options
 * @returns {HTMLElement} safely constructed DOM element
 */
export function createSafeElement(tag, { className = '', text = '', attrs = {}, children = [] } = {}) {
  const el = document.createElement(tag);
  if (className) el.className = className;
  if (text) el.textContent = text;
  
  for (const [key, value] of Object.entries(attrs)) {
    // Block dangerous inline event handlers
    if (key.startsWith('on')) continue;
    
    // Context-sensitive URL validation
    if (key === 'href') {
      el.setAttribute(key, sanitizeNavigationUrl(value));
    } else if (key === 'src') {
      el.setAttribute(key, sanitizeResourceUrl(value));
    } else {
      el.setAttribute(key, value);
    }
  }

  for (const child of children) {
    if (typeof child === 'string') {
      el.appendChild(document.createTextNode(child));
    } else if (child instanceof Node) {
      el.appendChild(child);
    }
  }

  return el;
}
```

---

## 6. Adversarial XSS Regression Verification

Before marking any frontend task as complete, test all dynamic inputs and rendering sinks against standard adversarial test vectors:

1. **Script Tag Vector**: `<script>alert('XSS')</script>`
2. **Event Handler Image Vector**: `<img src=x onerror="alert(1)">`
3. **Pseudo-Protocol Link Vector**: `javascript:alert(document.domain)`
4. **SVG Vector**: `<svg onload="alert(1)">`
5. **Body Breakout Vector**: `"><script>alert(1)</script>`

**Acceptance Standard**:
- Vectors MUST render as literal text glyphs (e.g. `&lt;script&gt;` or plain characters in the DOM tree).
- Zero JavaScript alerts or unauthorized actions may trigger.
- The DevTools Console must show zero CSP script-src violation errors.
