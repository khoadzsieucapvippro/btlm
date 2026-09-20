/**
 * =============================================================================
 * DOM SECURITY & SAFE RENDERING UTILITIES
 * Architecture: Defense-in-Depth against DOM-based XSS (OWASP ASVS 5.0 V5)
 * =============================================================================
 */

/**
 * Encodes text into safe HTML entity representation using browser-native textContent
 * with robust character entity fallback for headless testing.
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
  if (typeof document !== 'undefined' && typeof document.createElement === 'function') {
    const div = document.createElement('div');
    div.textContent = String(str);
    return div.innerHTML;
  }
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/**
 * Contextual URL Validation for Navigation Sinks (anchor href, window.location).
 * Whitelist: http:, https:, mailto:, tel:, and safe relative paths (/, ./, #).
 * Strictly rejects protocol-relative (//), backslash host vectors (/\, \\),
 * ASCII control characters, and executable schemes: javascript:, vbscript:, data:, file:.
 * 
 * @param {string} url - candidate navigation URL
 * @returns {string} safe URL or '#' if rejected
 */
export function sanitizeNavigationUrl(url) {
  if (!url) return '#';
  const trimmed = String(url).trim();
  if (!trimmed) return '#';

  // Reject ASCII control characters (defense against obfuscated protocol injection)
  // eslint-disable-next-line no-control-regex
  if (/[\x00-\x1F\x7F]/.test(trimmed)) {
    return '#';
  }
  
  // Reject protocol-relative and backslash host vectors
  if (
    trimmed.startsWith('//') ||
    trimmed.startsWith('/\\') ||
    trimmed.startsWith('\\') ||
    trimmed.startsWith('\\/')
  ) {
    return '#';
  }

  // Safe relative paths & anchor fragments
  if (
    (/^\/[^/\\]/.test(trimmed)) ||
    trimmed === '/' ||
    (trimmed.startsWith('./') && !trimmed.startsWith('./\\')) ||
    trimmed.startsWith('#')
  ) {
    return trimmed;
  }
  
  try {
    const base = typeof window !== 'undefined' && window.location?.origin 
      ? window.location.origin 
      : 'http://localhost';
    const parsed = new URL(trimmed, base);
    const protocol = parsed.protocol.toLowerCase();
    if (['http:', 'https:', 'mailto:', 'tel:'].includes(protocol)) {
      return parsed.href;
    }
  } catch {
    // Malformed URL
  }
  return '#';
}

/**
 * Contextual URL Validation for Media & Resource Sinks (img src, audio src, video src).
 * Strictly restricts schemes to http:, https:, safe relative paths (/, ./), or safe raster data URIs.
 * Rejects protocol-relative (//), backslash host vectors (/\, \\), ASCII control characters,
 * mailto:, tel:, SVG data URIs, and all executable schemes.
 * 
 * @param {string} url - candidate resource URL
 * @returns {string} safe URL or 'about:blank' if rejected
 */
export function sanitizeResourceUrl(url) {
  if (!url) return 'about:blank';
  const trimmed = String(url).trim();
  if (!trimmed) return 'about:blank';

  // Reject ASCII control characters
  // eslint-disable-next-line no-control-regex
  if (/[\x00-\x1F\x7F]/.test(trimmed)) {
    return 'about:blank';
  }
  
  // Reject protocol-relative and backslash host vectors
  if (
    trimmed.startsWith('//') ||
    trimmed.startsWith('/\\') ||
    trimmed.startsWith('\\') ||
    trimmed.startsWith('\\/')
  ) {
    return 'about:blank';
  }

  // Safe relative paths
  if (
    (/^\/[^/\\]/.test(trimmed)) ||
    trimmed === '/' ||
    (trimmed.startsWith('./') && !trimmed.startsWith('./\\'))
  ) {
    return trimmed;
  }

  // Safe raster image data URIs (PNG, JPEG, WebP, GIF only; SVG excluded due to script-carrier risk)
  if (/^data:image\/(png|jpeg|jpg|webp|gif);base64,[A-Za-z0-9+/=]+$/i.test(trimmed)) {
    return trimmed;
  }

  try {
    const base = typeof window !== 'undefined' && window.location?.origin 
      ? window.location.origin 
      : 'http://localhost';
    const parsed = new URL(trimmed, base);
    const protocol = parsed.protocol.toLowerCase();
    if (['http:', 'https:'].includes(protocol)) {
      return parsed.href;
    }
  } catch {
    // Malformed URL
  }
  return 'about:blank';
}

/**
 * Programmatically constructs a safe DOM element with sanitized attributes and child nodes.
 * Enforces textContent over innerHTML, completely blocks dynamic script tags, inline event attributes,
 * dangerous iframe srcdoc, and routes URL attributes to contextual sanitizers.
 * 
 * @param {string} tag - HTML tag name (e.g. 'div', 'button', 'span', 'p')
 * @param {Object} options - configuration options
 * @param {string} [options.className=''] - CSS class names
 * @param {string} [options.text=''] - plain text content
 * @param {Object} [options.attrs={}] - attribute key-value map
 * @param {Array<Node|string>} [options.children=[]] - child nodes or text strings
 * @returns {HTMLElement} safely constructed DOM element
 */
export function createSafeElement(tag, { className = '', text = '', attrs = {}, children = [] } = {}) {
  const normalizedTag = String(tag || 'div').trim().toLowerCase();
  if (normalizedTag === 'script') {
    throw new Error('[Security] createSafeElement forbids dynamic script element creation');
  }

  const el = document.createElement(normalizedTag);
  if (className) el.className = className;
  if (text) el.textContent = text;
  
  for (const [key, value] of Object.entries(attrs)) {
    if (value === null || value === undefined) continue;

    const cleanKey = String(key).trim().toLowerCase();

    // Block dangerous inline event handlers (onclick, onerror, onload, onsubmit, etc.)
    if (cleanKey.startsWith('on')) {
      console.warn(`[Security] Blocked inline event attribute: "${key}"`);
      continue;
    }

    // Block iframe srcdoc (HTML execution sink)
    if (cleanKey === 'srcdoc') {
      console.warn(`[Security] Blocked dangerous iframe srcdoc attribute`);
      continue;
    }
    
    // Context-sensitive URL validation
    if (['href', 'action', 'formaction', 'xlink:href'].includes(cleanKey)) {
      el.setAttribute(key, sanitizeNavigationUrl(value));
    } else if (cleanKey === 'src') {
      el.setAttribute(key, sanitizeResourceUrl(value));
    } else if (cleanKey === 'style') {
      // Safe CSS style policy: reject untrusted style strings; allow only benign CSS declarations
      const styleStr = String(value).trim();
      if (/^[\w\s-:%.,#;]+$/i.test(styleStr) && !/[()<>'"]/.test(styleStr)) {
        el.setAttribute(key, styleStr);
      } else {
        console.warn(`[Security] Blocked untrusted style attribute: "${styleStr}"`);
      }
    } else {
      el.setAttribute(key, String(value));
    }
  }

  for (const child of children) {
    if (typeof child === 'string' || typeof child === 'number') {
      el.appendChild(document.createTextNode(String(child)));
    } else if (typeof Node !== 'undefined' ? child instanceof Node : Boolean(child && typeof child === 'object')) {
      el.appendChild(child);
    }
  }

  return el;
}

/**
 * Safely clears all child nodes from a container element.
 * 
 * @param {HTMLElement} container - container element to clear
 */
export function clearContainer(container) {
  if (!container) return;
  if (typeof container.replaceChildren === 'function') {
    container.replaceChildren();
  } else {
    container.textContent = '';
  }
}

