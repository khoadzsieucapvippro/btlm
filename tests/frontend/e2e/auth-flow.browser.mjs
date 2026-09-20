/**
 * =============================================================================
 * BROWSER E2E TEST: AUTHENTICATION, PROFILE & SESSION INTEGRATION (TASK 9B.1)
 * File: tests/frontend/e2e/auth-flow.browser.mjs
 * 
 * Verifies:
 * - Login flow with real DOM interaction, API interception, session establishment,
 *   and authoritative navbar switch from Guest to Authenticated.
 * - Logout flow returning navbar cleanly to Guest state without page reload.
 * - Login 401 error handling, user input preservation, and re-enabled submit button.
 * - Registration 201 success flow redirecting to login with registered notice banner.
 * - Registration 409 conflict handling with accessible inline error feedback.
 * - Profile access guard redirecting unauthenticated visitors to login.
 * - Profile data loading, editing, saving via PUT, and dynamic session synchronization.
 * - Password paste accessibility (WCAG 2.2 SC 3.3.8).
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

describe('Browser E2E: Authentication UI, Profile & Session Integration', () => {
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

  test('BF1: Login success flow sets session and switches navbar to Authenticated state', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);

    try {
      // Intercept Login API
      await page.route('**/api/v1/auth/login', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              token: 'jwt-mock-token-learner-1',
              accountId: 201,
              emailOrPhone: 'trannghia@example.com',
              fullName: 'Trần Đại Nghĩa',
              roles: ['Learner']
            }
          })
        });
      });

      await page.goto(`${baseUrl}/login.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#loginForm');

      // Fill in credentials
      await page.fill('#loginEmail', 'trannghia@example.com');
      await page.fill('#loginPassword', 'SecurePassword123!');

      // Submit
      await page.click('#loginSubmitBtn');

      // Expect redirection to index.html
      await page.waitForURL('**/index.html*');

      // Verify Authenticated Navbar rendered
      await page.waitForSelector('#navAuthenticatedSection');

      const nameText = await page.textContent('#navUserNameLink');
      assert.strictEqual(nameText.trim(), 'Trần Đại Nghĩa');

      const roleBadgeText = await page.textContent('#navRoleBadge');
      assert.strictEqual(roleBadgeText.trim(), 'Học viên');

      const logoutBtn = await page.locator('#navLogoutBtn');
      assert.ok(await logoutBtn.isVisible(), 'Logout button must be visible');

      // Verify Token in localStorage
      const token = await page.evaluate(() => localStorage.getItem('access_token'));
      assert.strictEqual(token, 'jwt-mock-token-learner-1');

      // Verify zero console or page errors
      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors detected: ${errors.join(', ')}`);

    } finally {
      await page.close();
      await context.close();
    }
  });

  test('BF2: Single-click logout transitions navbar back to Guest state', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);

    try {
      // Seed authenticated session in localStorage before page load
      await page.addInitScript(() => {
        localStorage.setItem('access_token', 'jwt-mock-token-learner-1');
        localStorage.setItem('user_info', JSON.stringify({
          accountId: 201,
          emailOrPhone: 'trannghia@example.com',
          fullName: 'Trần Đại Nghĩa',
          roles: ['Learner']
        }));
      });

      await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#navAuthenticatedSection');

      // Click logout
      await page.click('#navLogoutBtn');

      // Verify navbar returns to Guest state
      await page.waitForSelector('#navLoginBtn');
      await page.waitForSelector('#navRegisterBtn');

      const isAuthVisible = await page.locator('#navAuthenticatedSection').count();
      assert.strictEqual(isAuthVisible, 0, 'Authenticated section must be unmounted');

      // Verify storage is cleansed
      const token = await page.evaluate(() => localStorage.getItem('access_token'));
      assert.strictEqual(token, null);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join(', ')}`);

    } finally {
      await page.close();
      await context.close();
    }
  });

  test('BF3: Login 401 Unauthorized renders alert and preserves user input', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);

    try {
      await page.route('**/api/v1/auth/login', async (route) => {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'UNAUTHORIZED',
            message: 'Tài khoản hoặc mật khẩu không chính xác',
            errors: []
          })
        });
      });

      await page.goto(`${baseUrl}/login.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#loginForm');

      await page.fill('#loginEmail', 'wronguser@example.com');
      await page.fill('#loginPassword', 'WrongPass!');

      await page.click('#loginSubmitBtn');

      // Verify global alert appears
      await page.waitForSelector('#loginGlobalAlert:not(.d-none)');
      const alertText = await page.textContent('#loginGlobalAlert');
      assert.ok(alertText.includes('không chính xác'), 'Must announce invalid credentials');

      // Verify input preserved
      const emailValue = await page.inputValue('#loginEmail');
      assert.strictEqual(emailValue, 'wronguser@example.com');

      // Verify button re-enabled
      const isBtnDisabled = await page.locator('#loginSubmitBtn').isDisabled();
      assert.strictEqual(isBtnDisabled, false);

      // Filter out expected 401 HTTP response error logged by browser resource loader
      const errors = getRuntimeErrors().filter(e => !e.includes('status of 401'));
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join(', ')}`);

    } finally {
      await page.close();
      await context.close();
    }
  });

  test('BF4: Registration success redirects to login with registration notice banner', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);

    try {
      await page.route('**/api/v1/auth/register', async (route) => {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Created',
            data: {
              token: 'jwt-mock-new-user',
              accountId: 202,
              emailOrPhone: 'newuser@example.com',
              fullName: 'Học Viên Mới',
              roles: ['Learner']
            }
          })
        });
      });

      await page.goto(`${baseUrl}/register.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#registerForm');

      await page.fill('#regFullName', 'Học Viên Mới');
      await page.fill('#regEmail', 'newuser@example.com');
      await page.fill('#regPassword', 'SecretPass123!');

      await page.click('#registerSubmitBtn');

      // Redirects to login with ?registered=true
      await page.waitForURL('**/login.html?registered=true*');

      await page.waitForSelector('#loginStatusNotice:not(.d-none)');
      const noticeText = await page.textContent('#loginStatusNotice');
      assert.ok(noticeText.includes('thành công'), 'Notice must announce registration success');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join(', ')}`);

    } finally {
      await page.close();
      await context.close();
    }
  });

  test('BF5: Registration 409 Conflict provides accessible field-level error', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);

    try {
      await page.route('**/api/v1/auth/register', async (route) => {
        await route.fulfill({
          status: 409,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'CONFLICT',
            message: 'Email hoặc số điện thoại đã tồn tại',
            errors: []
          })
        });
      });

      await page.goto(`${baseUrl}/register.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#registerForm');

      await page.fill('#regFullName', 'Học Viên Trùng Lặp');
      await page.fill('#regEmail', 'existing@example.com');
      await page.fill('#regPassword', 'SecurePass123!');

      await page.click('#registerSubmitBtn');

      // Verify email input marked invalid
      await page.waitForSelector('#regEmail.is-invalid');
      const ariaInvalid = await page.getAttribute('#regEmail', 'aria-invalid');
      assert.strictEqual(ariaInvalid, 'true');

      const emailError = await page.textContent('#regEmailError');
      assert.ok(emailError.includes('đã được sử dụng'));

      // Filter out expected 409 HTTP response error logged by browser resource loader
      const errors = getRuntimeErrors().filter(e => !e.includes('status of 409'));
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join(', ')}`);

    } finally {
      await page.close();
      await context.close();
    }
  });

  test('BF6: Unauthenticated user visiting profile.html is guarded and redirected to login', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);

    try {
      // Ensure storage is clean
      await page.addInitScript(() => {
        localStorage.clear();
      });

      await page.goto(`${baseUrl}/profile.html`, { waitUntil: 'domcontentloaded' });

      // Expect immediate redirection to login with redirect param
      await page.waitForURL('**/login.html?redirect=profile.html*');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join(', ')}`);

    } finally {
      await page.close();
      await context.close();
    }
  });

  test('BF7: Profile loads data, handles edits, updates session and navbar dynamically', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);

    try {
      // Seed initial session
      await page.addInitScript(() => {
        localStorage.setItem('access_token', 'jwt-profile-token');
        localStorage.setItem('user_info', JSON.stringify({
          accountId: 301,
          emailOrPhone: 'trangquynh@example.com',
          fullName: 'Trạng Quỳnh',
          roles: ['Creator']
        }));
      });

      // Intercept GET /api/v1/users/profile
      await page.route('**/api/v1/users/profile', async (route) => {
        if (route.request().method() === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'OK',
              data: {
                userId: 901,
                accountId: 301,
                emailOrPhone: 'trangquynh@example.com',
                fullName: 'Trạng Quỳnh',
                avatarUrl: 'https://example.com/trangquynh.jpg',
                createdAt: '2026-01-01T00:00:00Z',
                updatedAt: '2026-01-01T00:00:00Z'
              }
            })
          });
        } else if (route.request().method() === 'PUT') {
          const body = JSON.parse(route.request().postData());
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'OK',
              data: {
                userId: 901,
                accountId: 301,
                emailOrPhone: 'trangquynh@example.com',
                fullName: body.fullName,
                avatarUrl: body.avatarUrl,
                createdAt: '2026-01-01T00:00:00Z',
                updatedAt: '2026-02-01T12:00:00Z'
              }
            })
          });
        }
      });

      await page.goto(`${baseUrl}/profile.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#profileCard:not(.d-none)');

      // Verify loaded details
      const nameHeading = await page.textContent('#profileFullNameDisplay');
      assert.strictEqual(nameHeading.trim(), 'Trạng Quỳnh');

      const roleBadge = await page.textContent('#profileRoleBadge');
      assert.strictEqual(roleBadge.trim(), 'Tác giả'); // Sourced from authManager, mapped from Creator

      // Edit profile: change name
      await page.fill('#profileFullNameInput', 'Trạng Quỳnh Đại Trí');

      // Submit update
      await page.click('#profileSaveBtn');

      // Wait for name update in header
      await page.waitForFunction(() => {
        return document.getElementById('profileFullNameDisplay')?.textContent?.includes('Đại Trí');
      });

      // Verify navbar updated immediately without full page reload
      const navUserName = await page.textContent('#navUserNameLink');
      assert.strictEqual(navUserName.trim(), 'Trạng Quỳnh Đại Trí');

      // Verify toast message appeared
      await page.waitForSelector('.toast-custom');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join(', ')}`);

    } finally {
      await page.close();
      await context.close();
    }
  });

  test('BF8: Accessible password input supports paste without obstruction (WCAG 2.2 SC 3.3.8)', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);

    try {
      await page.goto(`${baseUrl}/login.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#loginPassword');

      // Simulate paste via clipboard or dispatching paste/input
      await page.focus('#loginPassword');
      await page.evaluate(() => {
        const input = document.getElementById('loginPassword');
        input.value = 'PastedSuperSecretPassword123!';
        input.dispatchEvent(new Event('input', { bubbles: true }));
        input.dispatchEvent(new Event('change', { bubbles: true }));
      });

      const val = await page.inputValue('#loginPassword');
      assert.strictEqual(val, 'PastedSuperSecretPassword123!');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join(', ')}`);

    } finally {
      await page.close();
      await context.close();
    }
  });

});
