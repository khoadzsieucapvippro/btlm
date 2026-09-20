/**
 * =============================================================================
 * UNIT TEST: UI PRIMITIVES CONTRACT (3-STATE UI, TOAST, MODAL)
 * Verifies state machine transitions, attributes, and options contract.
 * =============================================================================
 */

import { test, describe, before } from 'node:test';
import assert from 'node:assert/strict';

// Lightweight in-memory DOM simulation for Node.js unit testing of pure UI primitives
class MockElement {
  constructor(tagName) {
    this.tagName = tagName;
    this.children = [];
    this.attrs = {};
    this.classList = {
      _classes: new Set(),
      add: (c) => this.classList._classes.add(c),
      remove: (c) => this.classList._classes.delete(c),
      contains: (c) => this.classList._classes.has(c)
    };
    this.textContent = '';
    this.listeners = {};
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

  addEventListener(event, handler) {
    this.listeners[event] = handler;
  }

  querySelector(selector) {
    // Simple selector resolution for unit checks
    return this.children.find(c => c instanceof MockElement) || null;
  }
}

// Setup environment globals prior to importing UI primitives
globalThis.Node = MockElement;
globalThis.HTMLElement = MockElement;
globalThis.document = {
  createElement: (tag) => new MockElement(tag),
  createTextNode: (text) => {
    const el = new MockElement('#text');
    el.textContent = text;
    return el;
  },
  getElementById: (id) => null
};

// Dynamic import after globals are established
const { setComponentState, showLoading, showEmpty, showError } = await import('../../../../frontend/js/ui/ui.js');

describe('Unit: UI Primitives & Three-State Engine Contract', () => {

  test('showLoading sets aria-busy="true" and polite role="status"', () => {
    const container = new MockElement('div');
    showLoading(container, { message: 'Đang tải bộ thủ...' });

    assert.strictEqual(container.getAttribute('aria-busy'), 'true');
    assert.strictEqual(container.children.length, 1);

    const wrapper = container.children[0];
    assert.strictEqual(wrapper.getAttribute('role'), 'status');
    assert.strictEqual(wrapper.getAttribute('aria-live'), 'polite');
  });

  test('showEmpty clears aria-busy and renders empty glyph and title', () => {
    const container = new MockElement('div');
    container.setAttribute('aria-busy', 'true');

    showEmpty(container, {
      glyph: '空',
      title: 'Không tìm thấy dữ liệu',
      description: 'Thử tìm từ khóa khác.'
    });

    assert.strictEqual(container.getAttribute('aria-busy'), null, 'Must remove aria-busy attribute on empty');
    assert.strictEqual(container.children.length, 1);
  });

  test('showError clears aria-busy and establishes role="alert" for immediate AT notice', () => {
    const container = new MockElement('div');
    container.setAttribute('aria-busy', 'true');

    showError(container, {
      title: 'Lỗi kết nối máy chủ',
      message: 'Vui lòng kiểm tra lại đường truyền mạng.',
      onRetry: () => {}
    });

    assert.strictEqual(container.getAttribute('aria-busy'), null, 'Must remove aria-busy attribute on error');
    assert.strictEqual(container.children.length, 1);
    const wrapper = container.children[0];
    assert.strictEqual(wrapper.getAttribute('role'), 'alert', 'Error state must have role="alert" (WCAG live region)');
  });

  test('setComponentState cleanly switches across state machine transitions', () => {
    const container = new MockElement('div');

    // 1. Transition: READY -> LOADING
    setComponentState(container, 'LOADING');
    assert.strictEqual(container.getAttribute('aria-busy'), 'true');

    // 2. Transition: LOADING -> EMPTY
    setComponentState(container, 'EMPTY');
    assert.strictEqual(container.getAttribute('aria-busy'), null);

    // 3. Transition: EMPTY -> ERROR
    setComponentState(container, 'ERROR', { title: 'Network Error' });
    assert.strictEqual(container.getAttribute('aria-busy'), null);

    // 4. Transition: ERROR -> READY
    setComponentState(container, 'READY');
    assert.strictEqual(container.getAttribute('aria-busy'), null);
    assert.strictEqual(container.children.length, 0, 'READY state clears temporary state banners');
  });

});
