/**
 * =============================================================================
 * BROWSER SMOKE TEST: API CLIENT & AUTH STATE INTEGRATION (TASK 9A.2)
 * Scope:
 * - Verifies apiClient execution in a real Chromium browser context.
 * - Confirms Authorization header injection via route interception.
 * - Confirms 401 single-owner lifecycle and auth:expired event dispatch.
 * - Confirms cross-tab session termination synchronization via StorageEvent.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

describe('Browser Smoke: API Client & Auth State Integration', () => {
  let serverHandle;
  let browser;
  let baseUrl;

  before(async () => {
    serverHandle = await ensureServer({ port: 3000 });
    baseUrl = serverHandle.baseUrl;
    browser = await launchBrowser({ headless: true });
  });

  after(async () => {
    if (browser) await browser.close();
    if (serverHandle) await serverHandle.close();
  });

  test('BA1: apiClient executes in browser with Bearer token injection', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });

      // Intercept /api/v1/users/profile
      let interceptedAuthHeader = null;
      await page.route('**/api/v1/users/profile', async (route) => {
        interceptedAuthHeader = route.request().headers()['authorization'];
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              userId: 101,
              emailOrPhone: 'browser-learner@example.com',
              fullName: 'Học Viên Trình Duyệt'
            }
          })
        });
      });

      // Execute in browser
      const result = await page.evaluate(async () => {
        const { authManager } = await import('/js/auth/auth-state.js');
        const { apiClient } = await import('/js/api/api.js');

        // Set session
        authManager.setSession({
          token: 'browser-jwt-token-777',
          accountId: 101,
          emailOrPhone: 'browser-learner@example.com',
          fullName: 'Học Viên Trình Duyệt',
          roles: ['Learner']
        });

        const profile = await apiClient('/users/profile');
        return {
          profile,
          isAuthenticated: authManager.isAuthenticated(),
          roles: authManager.getRoles()
        };
      });

      assert.strictEqual(interceptedAuthHeader, 'Bearer browser-jwt-token-777');
      assert.strictEqual(result.profile.userId, 101);
      assert.strictEqual(result.profile.fullName, 'Học Viên Trình Duyệt');
      assert.strictEqual(result.isAuthenticated, true);
      assert.deepStrictEqual(result.roles, ['Learner']);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors in BA1:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('BA2: HTTP 401 triggers single-owner cleanup and auth:expired event in browser', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });

      // Intercept /api/v1/srs/due with 401
      await page.route('**/api/v1/srs/due', async (route) => {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'UNAUTHORIZED',
            message: 'Phiên đăng nhập đã hết hạn',
            errors: []
          })
        });
      });

      const outcome = await page.evaluate(async () => {
        const { authManager } = await import('/js/auth/auth-state.js');
        const { apiClient } = await import('/js/api/api.js');

        authManager.setSession({
          token: 'expired-token-999',
          accountId: 1,
          roles: ['Learner']
        });

        let expiredEventFired = false;
        window.addEventListener('auth:expired', () => {
          expiredEventFired = true;
        }, { once: true });

        // Override redirect handler to observe without actual navigation
        let redirectTriggered = false;
        authManager.setRedirectHandler(() => {
          redirectTriggered = true;
        });

        let caughtError = null;
        try {
          await apiClient('/srs/due');
        } catch (err) {
          caughtError = {
            name: err.name,
            status: err.status,
            code: err.code
          };
        }

        return {
          caughtError,
          expiredEventFired,
          redirectTriggered,
          tokenAfter: authManager.getToken(),
          isAuthenticatedAfter: authManager.isAuthenticated()
        };
      });

      assert.ok(outcome.caughtError, 'apiClient must throw on 401');
      assert.strictEqual(outcome.caughtError.status, 401);
      assert.strictEqual(outcome.caughtError.code, 'UNAUTHORIZED');
      assert.strictEqual(outcome.expiredEventFired, true, 'auth:expired event must fire on 401');
      assert.strictEqual(outcome.redirectTriggered, true, 'Redirect handler must be invoked');
      assert.strictEqual(outcome.tokenAfter, null, 'Token must be cleared from storage');
      assert.strictEqual(outcome.isAuthenticatedAfter, false);

      // Filter out the expected 401 HTTP response error logged by browser resource loader
      const errors = getRuntimeErrors().filter(e => !e.includes('status of 401'));
      assert.strictEqual(errors.length, 0, `Runtime errors in BA2:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('BA3: Cross-tab session synchronization in multi-page context', async () => {
    // Create shared browser context (shares localStorage across pages)
    const context = await browser.newContext({
      viewport: { width: 1280, height: 720 }
    });

    try {
      const pageA = await context.newPage();
      const pageB = await context.newPage();

      await pageA.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
      await pageB.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });

      // Establish session in Page A
      await pageA.evaluate(async () => {
        const { authManager } = await import('/js/auth/auth-state.js');
        authManager.setSession({
          token: 'tab-sync-jwt',
          accountId: 50,
          roles: ['Creator']
        });
      });

      // Confirm Page B reads token from shared storage
      const tokenInB = await pageB.evaluate(async () => {
        const { authManager } = await import('/js/auth/auth-state.js');
        return authManager.getToken();
      });
      assert.strictEqual(tokenInB, 'tab-sync-jwt');

      // Setup expiration listener in Page B
      await pageB.evaluate(async () => {
        const { authManager } = await import('/js/auth/auth-state.js');
        window._tabBExpired = false;
        window.addEventListener('auth:expired', () => {
          window._tabBExpired = true;
        });
        authManager.setRedirectHandler(() => {
          window._tabBRedirect = true;
        });
      });

      // Page A logs out
      await pageA.evaluate(async () => {
        const { authManager } = await import('/js/auth/auth-state.js');
        authManager.logout(false);
      });

      // Wait a moment for storage event to propagate between tabs
      await pageB.waitForTimeout(300);

      // Verify Page B recognized remote logout
      const syncResult = await pageB.evaluate(async () => {
        const { authManager } = await import('/js/auth/auth-state.js');
        return {
          isAuth: authManager.isAuthenticated(),
          token: authManager.getToken(),
          expiredFired: window._tabBExpired
        };
      });

      assert.strictEqual(syncResult.isAuth, false, 'Page B must be unauthenticated after Page A logout');
      assert.strictEqual(syncResult.token, null, 'Page B token must be null');
      assert.strictEqual(syncResult.expiredFired, true, 'Page B must have caught auth:expired event');

      await pageA.close();
      await pageB.close();
    } finally {
      await context.close();
    }
  });

});
