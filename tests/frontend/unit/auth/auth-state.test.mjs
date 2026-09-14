/**
 * =============================================================================
 * UNIT TEST: AUTH STATE MANAGER CONTRACT (TASK 9A.2)
 * Tests session lifecycle, exact role representation, corrupted storage resilience,
 * single-owner 401 expiration transition, and multi-tab synchronization.
 * =============================================================================
 */

import { test, describe, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';

// In-memory mock localStorage for isolated, fast Node.js testing
class MockLocalStorage {
  constructor() {
    this.store = new Map();
  }
  getItem(key) {
    return this.store.has(key) ? this.store.get(key) : null;
  }
  setItem(key, value) {
    this.store.set(key, String(value));
  }
  removeItem(key) {
    this.store.delete(key);
  }
  clear() {
    this.store.clear();
  }
}

// In-memory mock window for event dispatch and storage listeners
class MockWindow {
  constructor() {
    this.listeners = new Map();
    this.location = {
      pathname: '/index.html',
      href: '/index.html'
    };
  }

  addEventListener(type, handler) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set());
    }
    this.listeners.get(type).add(handler);
  }

  removeEventListener(type, handler) {
    if (this.listeners.has(type)) {
      this.listeners.get(type).delete(handler);
    }
  }

  dispatchEvent(event) {
    if (this.listeners.has(event.type)) {
      for (const handler of this.listeners.get(event.type)) {
        handler(event);
      }
    }
    return true;
  }
}

class MockCustomEvent {
  constructor(type, init = {}) {
    this.type = type;
    this.detail = init.detail;
  }
}

// Setup globals before importing module
const mockStorage = new MockLocalStorage();
const mockWin = new MockWindow();

globalThis.localStorage = mockStorage;
globalThis.window = mockWin;
globalThis.CustomEvent = MockCustomEvent;

// Dynamic import to bind mock globals
const { authManager } = await import('../../../../frontend/js/auth/auth-state.js');

describe('Auth State Manager Contract', () => {

  beforeEach(() => {
    mockStorage.clear();
    mockWin.location.pathname = '/index.html';
    mockWin.location.href = '/index.html';
    authManager.resetRedirectHandler();
  });

  afterEach(() => {
    mockStorage.clear();
    authManager.resetRedirectHandler();
  });

  test('setSession() correctly persists valid AuthResponse to storage and dispatches auth:login', () => {
    let loginEventDetail = null;
    const loginHandler = (e) => {
      loginEventDetail = e.detail;
    };
    mockWin.addEventListener('auth:login', loginHandler);

    const validAuthResponse = {
      token: 'test-jwt-token-xyz',
      type: 'Bearer',
      accountId: 42,
      emailOrPhone: 'learner@example.com',
      fullName: 'Trương Học Lương',
      roles: ['Learner']
    };

    authManager.setSession(validAuthResponse);

    assert.strictEqual(authManager.getToken(), 'test-jwt-token-xyz');
    assert.strictEqual(authManager.isAuthenticated(), true);

    const user = authManager.getUser();
    assert.ok(user, 'User object must exist');
    assert.strictEqual(user.accountId, 42);
    assert.strictEqual(user.emailOrPhone, 'learner@example.com');
    assert.strictEqual(user.fullName, 'Trương Học Lương');
    assert.deepStrictEqual(user.roles, ['Learner']);

    assert.ok(loginEventDetail, 'auth:login event must be dispatched');
    assert.strictEqual(loginEventDetail.accountId, 42);
    assert.strictEqual(loginEventDetail.emailOrPhone, 'learner@example.com');

    mockWin.removeEventListener('auth:login', loginHandler);
  });

  test('setSession() throws when authData is invalid or missing token', () => {
    assert.throws(() => {
      authManager.setSession(null);
    }, TypeError);

    assert.throws(() => {
      authManager.setSession({});
    }, /missing required string field "token"/);

    assert.throws(() => {
      authManager.setSession({ token: '' });
    }, /missing required string field "token"/);
  });

  test('getToken() returns null when token is missing, empty, or whitespace', () => {
    assert.strictEqual(authManager.getToken(), null);

    mockStorage.setItem('access_token', '   ');
    assert.strictEqual(authManager.getToken(), null);
  });

  test('getUser() recovers gracefully from corrupted JSON without throwing', () => {
    // Write corrupted JSON string directly to localStorage
    mockStorage.setItem('user_info', '{corrupted: invalid-json}');

    const user = authManager.getUser();
    assert.strictEqual(user, null, 'Corrupted JSON must return null');
    assert.strictEqual(mockStorage.getItem('user_info'), null, 'Corrupted key must be cleansed from storage');
  });

  test('getUser() recovers gracefully when stored JSON is a non-object primitive', () => {
    mockStorage.setItem('user_info', '"a-simple-string"');

    const user = authManager.getUser();
    assert.strictEqual(user, null, 'Non-object primitive must return null');
    assert.strictEqual(mockStorage.getItem('user_info'), null, 'Invalid key must be cleansed');
  });

  test('hasRole() strictly enforces exact case-sensitive role matching', () => {
    authManager.setSession({
      token: 'jwt-123',
      accountId: 1,
      roles: ['Creator', 'Learner']
    });

    assert.strictEqual(authManager.hasRole('Creator'), true);
    assert.strictEqual(authManager.hasRole('Learner'), true);
    assert.strictEqual(authManager.hasRole('Admin'), false);

    // Case-sensitivity: must not match different casing
    assert.strictEqual(authManager.hasRole('creator'), false);
    assert.strictEqual(authManager.hasRole('LEARNER'), false);
  });

  test('hasRole() strictly rejects ROLE_ prefix to enforce backend JSON contract', () => {
    authManager.setSession({
      token: 'jwt-123',
      accountId: 1,
      roles: ['Admin']
    });

    // Invariant: Backend JSON DTOs deliver ["Admin"], NEVER ["ROLE_ADMIN"]
    assert.strictEqual(authManager.hasRole('Admin'), true);
    assert.strictEqual(authManager.hasRole('ROLE_ADMIN'), false, 'ROLE_ prefix must be rejected');
    assert.strictEqual(authManager.hasRole('ROLE_Admin'), false, 'ROLE_ prefix must be rejected');
  });

  test('hasRole() returns false safely when unauthenticated or given invalid inputs', () => {
    authManager.clearSession();

    assert.strictEqual(authManager.hasRole('Admin'), false);
    assert.strictEqual(authManager.hasRole(null), false);
    assert.strictEqual(authManager.hasRole(''), false);
    assert.strictEqual(authManager.hasRole(123), false);
  });

  test('clearSession() purges token and user info idempotently', () => {
    authManager.setSession({
      token: 'jwt-token',
      accountId: 1,
      roles: ['Learner']
    });

    assert.strictEqual(authManager.isAuthenticated(), true);

    authManager.clearSession();

    assert.strictEqual(authManager.getToken(), null);
    assert.strictEqual(authManager.getUser(), null);
    assert.strictEqual(authManager.isAuthenticated(), false);
    assert.deepStrictEqual(authManager.getRoles(), []);

    // Calling clearSession again should not fail
    authManager.clearSession();
    assert.strictEqual(authManager.isAuthenticated(), false);
  });

  test('handleUnauthorized() performs single-owner 401 transition: clears session, dispatches ONE auth:expired, calls redirect', () => {
    let expiredEventCount = 0;
    const expiredHandler = () => {
      expiredEventCount++;
    };
    mockWin.addEventListener('auth:expired', expiredHandler);

    let redirectCalled = 0;
    authManager.setRedirectHandler(() => {
      redirectCalled++;
    });

    authManager.setSession({
      token: 'jwt-expiring',
      accountId: 10,
      roles: ['Learner']
    });

    // Execute single-owner transition
    authManager.handleUnauthorized();

    assert.strictEqual(authManager.isAuthenticated(), false);
    assert.strictEqual(expiredEventCount, 1, 'Exactly ONE auth:expired event must fire');
    assert.strictEqual(redirectCalled, 1, 'Redirect handler must be called once');

    mockWin.removeEventListener('auth:expired', expiredHandler);
  });

  test('logout() clears session, dispatches auth:logout, and triggers redirect', () => {
    let logoutEventCount = 0;
    const logoutHandler = () => {
      logoutEventCount++;
    };
    mockWin.addEventListener('auth:logout', logoutHandler);

    let redirectCalled = 0;
    authManager.setRedirectHandler(() => {
      redirectCalled++;
    });

    authManager.setSession({
      token: 'jwt-logout',
      accountId: 10,
      roles: ['Learner']
    });

    authManager.logout();

    assert.strictEqual(authManager.isAuthenticated(), false);
    assert.strictEqual(logoutEventCount, 1, 'auth:logout event must fire once');
    assert.strictEqual(redirectCalled, 1, 'Redirect handler must be called');

    mockWin.removeEventListener('auth:logout', logoutHandler);
  });

  test('Multi-tab storage event: remote token removal triggers handleUnauthorized()', () => {
    authManager.setSession({
      token: 'tab-token',
      accountId: 5,
      roles: ['Learner']
    });

    let expiredCount = 0;
    const expiredHandler = () => {
      expiredCount++;
    };
    mockWin.addEventListener('auth:expired', expiredHandler);

    let redirectCalled = 0;
    authManager.setRedirectHandler(() => {
      redirectCalled++;
    });

    // Simulate remote tab clearing access_token
    const remoteStorageEvent = {
      type: 'storage',
      key: 'access_token',
      oldValue: 'tab-token',
      newValue: null
    };

    mockWin.dispatchEvent(remoteStorageEvent);

    assert.strictEqual(authManager.isAuthenticated(), false);
    assert.strictEqual(expiredCount, 1, 'Remote logout must trigger local auth:expired');
    assert.strictEqual(redirectCalled, 1);

    mockWin.removeEventListener('auth:expired', expiredHandler);
  });

});
