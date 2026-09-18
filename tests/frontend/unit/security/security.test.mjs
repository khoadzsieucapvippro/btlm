/**
 * =============================================================================
 * UNIT TEST: SECURITY UTILITIES CONTRACT & INPUT SANITIZATION
 * Tests: escapeHtml, sanitizeNavigationUrl, sanitizeResourceUrl, createSafeElement, clearContainer
 * Defense-in-Depth against DOM-based XSS (OWASP ASVS 5.0 V5)
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';

// Lightweight in-memory DOM simulation for Node.js unit testing of createSafeElement
class MockElement {
  constructor(tagName) {
    this.tagName = String(tagName).toUpperCase();
    this.children = [];
    this.attrs = {};
    this.className = '';
    this.textContent = '';
  }

  get innerHTML() {
    return String(this.textContent || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  setAttribute(key, value) {
    this.attrs[key] = String(value);
  }

  getAttribute(key) {
    return this.attrs[key] || null;
  }

  removeAttribute(key) {
    delete this.attrs[key];
  }

  appendChild(child) {
    this.children.push(child);
  }

  replaceChildren(...children) {
    this.children = [...children];
  }
}

globalThis.Node = MockElement;
globalThis.HTMLElement = MockElement;
globalThis.document = {
  createElement: (tag) => new MockElement(tag),
  createTextNode: (text) => {
    const el = new MockElement('#text');
    el.textContent = String(text);
    return el;
  }
};

// Dynamic import after globals are established
const {
  escapeHtml,
  sanitizeNavigationUrl,
  sanitizeResourceUrl,
  createSafeElement,
  clearContainer
} = await import('../../../../frontend/js/ui/security.js');

describe('Unit: Security & URL Sanitization Contract', () => {

  describe('escapeHtml()', () => {
    test('handles null and undefined by returning empty string', () => {
      assert.strictEqual(escapeHtml(null), '');
      assert.strictEqual(escapeHtml(undefined), '');
      assert.strictEqual(escapeHtml(''), '');
    });

    test('encodes HTML special characters to prevent markup injection', () => {
      const input = '<script>alert("XSS & attack\'s")</script>';
      const result = escapeHtml(input);
      assert.ok(!result.includes('<script>'), 'Must not retain raw <script> tag');
      assert.ok(result.includes('&lt;script&gt;'), 'Must encode < and >');
      assert.ok(result.includes('&amp;'), 'Must encode &');
      assert.ok(result.includes('&quot;'), 'Must encode "');
      assert.ok(result.includes('&#39;'), 'Must encode \'');
    });

    test('preserves Chinese characters and authentic typography', () => {
      const input = '學 & 習: 214 Bộ thủ Khang Hy (Học tập)';
      const result = escapeHtml(input);
      assert.ok(result.includes('學'), 'Must preserve Chinese ideograph 學');
      assert.ok(result.includes('習'), 'Must preserve Chinese ideograph 習');
      assert.ok(result.includes('&amp;'), 'Must safely encode ampersand');
    });
  });

  describe('sanitizeNavigationUrl()', () => {
    test('permits legitimate http and https URLs', () => {
      assert.strictEqual(
        sanitizeNavigationUrl('https://example.com/vocabulary'),
        'https://example.com/vocabulary'
      );
      assert.strictEqual(
        sanitizeNavigationUrl('http://localhost:8080/api/v1/health'),
        'http://localhost:8080/api/v1/health'
      );
    });

    test('permits safe relative paths and fragment anchors', () => {
      assert.strictEqual(sanitizeNavigationUrl('/radicals.html'), '/radicals.html');
      assert.strictEqual(sanitizeNavigationUrl('./lessons.html?id=5'), './lessons.html?id=5');
      assert.strictEqual(sanitizeNavigationUrl('#mainContent'), '#mainContent');
      assert.strictEqual(sanitizeNavigationUrl('/'), '/');
    });

    test('permits safe mailto and tel schemes for navigation', () => {
      assert.strictEqual(sanitizeNavigationUrl('mailto:support@elearning.vn'), 'mailto:support@elearning.vn');
      assert.strictEqual(sanitizeNavigationUrl('tel:+84987654321'), 'tel:+84987654321');
    });

    test('rejects and neutralizes javascript: execution scheme to #', () => {
      assert.strictEqual(sanitizeNavigationUrl('javascript:alert(document.cookie)'), '#');
      assert.strictEqual(sanitizeNavigationUrl('JAVASCRIPT:alert(1)'), '#');
      assert.strictEqual(sanitizeNavigationUrl('  javascript:void(0)  '), '#');
      assert.strictEqual(sanitizeNavigationUrl('JaVaScRiPt:alert(1)'), '#');
    });

    test('rejects protocol-relative and backslash host bypass vectors', () => {
      assert.strictEqual(sanitizeNavigationUrl('//malicious-site.com/steal-token'), '#');
      assert.strictEqual(sanitizeNavigationUrl('/\\\\malicious-site.com'), '#');
      assert.strictEqual(sanitizeNavigationUrl('\\\\malicious-site.com'), '#');
      assert.strictEqual(sanitizeNavigationUrl('\\/malicious-site.com'), '#');
    });

    test('rejects ASCII control characters in navigation URLs', () => {
      assert.strictEqual(sanitizeNavigationUrl('jav\x00ascript:alert(1)'), '#');
      assert.strictEqual(sanitizeNavigationUrl('\x01https://example.com'), '#');
      assert.strictEqual(sanitizeNavigationUrl('https://example.com\x7F/path'), '#');
    });

    test('rejects data:, vbscript:, and file: execution schemes', () => {
      assert.strictEqual(sanitizeNavigationUrl('data:text/html,<script>alert(1)</script>'), '#');
      assert.strictEqual(sanitizeNavigationUrl('vbscript:msgbox(1)'), '#');
      assert.strictEqual(sanitizeNavigationUrl('file:///etc/passwd'), '#');
    });

    test('returns # for invalid or empty inputs', () => {
      assert.strictEqual(sanitizeNavigationUrl(''), '#');
      assert.strictEqual(sanitizeNavigationUrl(null), '#');
      assert.strictEqual(sanitizeNavigationUrl(undefined), '#');
    });
  });

  describe('sanitizeResourceUrl()', () => {
    test('permits legitimate media resource URLs (http/https and relative)', () => {
      assert.strictEqual(
        sanitizeResourceUrl('https://cdn.example.com/audio/pinyin/xue2.mp3'),
        'https://cdn.example.com/audio/pinyin/xue2.mp3'
      );
      assert.strictEqual(sanitizeResourceUrl('/media/stroke-order/radical_001.svg'), '/media/stroke-order/radical_001.svg');
      assert.strictEqual(sanitizeResourceUrl('./images/logo.png'), './images/logo.png');
      assert.strictEqual(sanitizeResourceUrl('/'), '/');
    });

    test('permits safe raster image base64 data URIs (PNG, JPEG, WebP, GIF)', () => {
      const validPng = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==';
      assert.strictEqual(sanitizeResourceUrl(validPng), validPng);
    });

    test('strictly forbids SVG data URIs (script carrier risk)', () => {
      assert.strictEqual(sanitizeResourceUrl('data:image/svg+xml,<svg onload=alert(1)>'), 'about:blank');
    });

    test('strictly forbids mailto: and tel: in media/resource sinks', () => {
      assert.strictEqual(sanitizeResourceUrl('mailto:attacker@evil.com'), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl('tel:123456'), 'about:blank');
    });

    test('neutralizes javascript:, data:text/html, and protocol-relative schemes to about:blank', () => {
      assert.strictEqual(sanitizeResourceUrl('javascript:alert(1)'), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl('//evil.com/image.png'), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl('/\\\\evil.com/image.png'), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl('\\\\evil.com/image.png'), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl('data:text/html,<script>alert(1)</script>'), 'about:blank');
    });

    test('rejects ASCII control characters in resource URLs', () => {
      assert.strictEqual(sanitizeResourceUrl('https://example.com/audio\x00.mp3'), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl('\x02https://example.com/pic.jpg'), 'about:blank');
    });

    test('returns about:blank for invalid, empty, or null inputs', () => {
      assert.strictEqual(sanitizeResourceUrl(''), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl(null), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl(undefined), 'about:blank');
    });
  });

  describe('createSafeElement()', () => {
    test('creates element with safe textContent and class', () => {
      const el = createSafeElement('div', {
        className: 'card p-3',
        text: '你好，世界'
      });
      assert.strictEqual(el.tagName, 'DIV');
      assert.strictEqual(el.className, 'card p-3');
      assert.strictEqual(el.textContent, '你好，世界');
    });

    test('strictly forbids dynamic script tag creation', () => {
      assert.throws(() => {
        createSafeElement('script', { text: 'alert(1)' });
      }, /forbids dynamic script element creation/);
    });

    test('blocks all inline event handler attributes (on*)', () => {
      const el = createSafeElement('button', {
        attrs: {
          onclick: 'alert(1)',
          ONLOAD: 'alert(2)',
          ' onerror ': 'alert(3)',
          onsubmit: 'return false;'
        }
      });
      assert.strictEqual(el.getAttribute('onclick'), null);
      assert.strictEqual(el.getAttribute('ONLOAD'), null);
      assert.strictEqual(el.getAttribute('onerror'), null);
      assert.strictEqual(el.getAttribute('onsubmit'), null);
    });

    test('blocks dangerous iframe srcdoc attribute', () => {
      const el = createSafeElement('iframe', {
        attrs: {
          srcdoc: '<script>alert(1)</script>'
        }
      });
      assert.strictEqual(el.getAttribute('srcdoc'), null);
    });

    test('routes navigation attributes (href, action, formaction, xlink:href) to sanitizeNavigationUrl', () => {
      const a = createSafeElement('a', { attrs: { href: 'javascript:alert(1)' } });
      assert.strictEqual(a.getAttribute('href'), '#');

      const form = createSafeElement('form', { attrs: { action: 'javascript:alert(2)' } });
      assert.strictEqual(form.getAttribute('action'), '#');

      const btn = createSafeElement('button', { attrs: { formaction: '//attacker.com' } });
      assert.strictEqual(btn.getAttribute('formaction'), '#');

      const validA = createSafeElement('a', { attrs: { href: '/lessons/1' } });
      assert.strictEqual(validA.getAttribute('href'), '/lessons/1');
    });

    test('routes src attribute to sanitizeResourceUrl', () => {
      const img = createSafeElement('img', { attrs: { src: 'javascript:alert(1)' } });
      assert.strictEqual(img.getAttribute('src'), 'about:blank');

      const validImg = createSafeElement('img', { attrs: { src: '/images/logo.png' } });
      assert.strictEqual(validImg.getAttribute('src'), '/images/logo.png');
    });

    test('sanitizes style attribute to reject dangerous CSS injections', () => {
      const badEl = createSafeElement('div', {
        attrs: { style: "background: url('javascript:alert(1)')" }
      });
      assert.strictEqual(badEl.getAttribute('style'), null);

      const goodEl = createSafeElement('div', {
        attrs: { style: 'width: 28px; height: 28px; font-size: 0.9rem;' }
      });
      assert.strictEqual(goodEl.getAttribute('style'), 'width: 28px; height: 28px; font-size: 0.9rem;');
    });

    test('appends string, number, and Node children properly', () => {
      const childNode = createSafeElement('span', { text: 'Child' });
      const parent = createSafeElement('div', {
        children: ['Text child', 123, childNode]
      });
      assert.strictEqual(parent.children.length, 3);
      assert.strictEqual(parent.children[0].textContent, 'Text child');
      assert.strictEqual(parent.children[1].textContent, '123');
      assert.strictEqual(parent.children[2], childNode);
    });

    test('preserves Vietnamese diacritics and Chinese characters in text content', () => {
      const el = createSafeElement('p', {
        text: 'Chữ Hán: 學習 — Pinyin: xué xí — Nghĩa: Học tập (Tiếng Việt có dấu)'
      });
      assert.strictEqual(
        el.textContent,
        'Chữ Hán: 學習 — Pinyin: xué xí — Nghĩa: Học tập (Tiếng Việt có dấu)'
      );
    });
  });

  describe('clearContainer()', () => {
    test('clears child nodes via replaceChildren', () => {
      const container = createSafeElement('div');
      container.appendChild(createSafeElement('span', { text: '1' }));
      container.appendChild(createSafeElement('span', { text: '2' }));
      assert.strictEqual(container.children.length, 2);

      clearContainer(container);
      assert.strictEqual(container.children.length, 0);
    });

    test('handles null and undefined gracefully', () => {
      assert.doesNotThrow(() => clearContainer(null));
      assert.doesNotThrow(() => clearContainer(undefined));
    });
  });

});

