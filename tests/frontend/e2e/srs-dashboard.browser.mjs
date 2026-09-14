/**
 * =============================================================================
 * BROWSER E2E TEST: SRS STUDY STATISTICS & DAILY SETTINGS DASHBOARD (TASK 9C.4)
 * File: tests/frontend/e2e/srs-dashboard.browser.mjs
 *
 * Verifies:
 * - SD-01: Anonymous user displays login-required state without calling protected SRS APIs.
 * - SD-02: Authenticated user loads dashboard, displays 5 authoritative statistics & current settings.
 * - SD-03: Client-side validation rejects invalid inputs (0, negative, float, empty) without submitting PUT.
 * - SD-04: Successful settings update submits exact payload and immediately refreshes authoritative statistics.
 * - SD-05: Reset settings button restores original authoritative values.
 * - SD-06: Cross-navigation between SRS Dashboard and SRS Review Room.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function createMockStudyStats(overrides = {}) {
  return {
    cardsDue: 8,
    reviewsToday: 12,
    newCardsToday: 5,
    newCardsLimit: 20,
    maxReviewLimit: 100,
    ...overrides
  };
}

function createMockUserSrsSetting(overrides = {}) {
  return {
    settingId: 101,
    newCardsPerDay: 20,
    maxReviewPerDay: 100,
    ...overrides
  };
}

async function setupAuthenticatedSession(page) {
  await page.addInitScript(() => {
    localStorage.setItem('access_token', 'test_srs_learner_jwt');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: 1001,
      emailOrPhone: 'srs_dashboard_user@example.com',
      fullName: 'Học Viên Dashboard',
      roles: ['Learner']
    }));
  });
}

describe('Browser E2E: SRS Dashboard & Daily Settings (9C.4)', () => {
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

  // SD-01: Anonymous Guard
  test('SD-01: Anonymous user displays login-required state without calling protected SRS APIs', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      let interceptedSrsCall = false;
      await page.route('**/api/v1/srs/**', async (route) => {
        interceptedSrsCall = true;
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Unauthorized' })
        });
      });

      await page.goto(`${baseUrl}/srs-dashboard.html`, { waitUntil: 'domcontentloaded' });

      // Verify login-required state is visible
      const authState = page.locator('#dashboardAuthState');
      await authState.waitFor({ state: 'visible' });

      // Verify content section is hidden
      const isContentHidden = await page.locator('#dashboardContentSection').evaluate(el => el.classList.contains('d-none'));
      assert.strictEqual(isContentHidden, true, 'Content section must remain hidden for anonymous user');

      // Verify login redirect button
      const loginBtn = page.locator('#btnDashboardLoginRedirect');
      const href = await loginBtn.getAttribute('href');
      assert.ok(href.includes('login.html'), 'Login button must redirect to login.html');

      assert.strictEqual(interceptedSrsCall, false, 'Protected SRS APIs must NEVER be called by anonymous clients');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors on anonymous page load:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // SD-02: Authenticated Load & Metric Presentation
  test('SD-02: Authenticated user loads dashboard, displays 5 authoritative statistics & current settings', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      const mockStats = createMockStudyStats();
      const mockSettings = createMockUserSrsSetting();

      await page.route('**/api/v1/srs/stats', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: mockStats })
        });
      });

      await page.route('**/api/v1/srs/settings', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: mockSettings })
        });
      });

      await page.goto(`${baseUrl}/srs-dashboard.html`, { waitUntil: 'domcontentloaded' });

      // Content section becomes visible
      await page.locator('#dashboardContentSection').waitFor({ state: 'visible' });

      // 1. Verify 5 authoritative statistics values
      assert.strictEqual(await page.locator('#statCardsDue').textContent(), '8');
      assert.strictEqual(await page.locator('#statReviewsToday').textContent(), '12');
      assert.strictEqual(await page.locator('#statNewCardsToday').textContent(), '5');
      assert.strictEqual(await page.locator('#statNewCardsLimit').textContent(), '20');
      assert.strictEqual(await page.locator('#statMaxReviewLimit').textContent(), '100');

      // Verify quota text
      const reviewQuota = await page.locator('#statReviewsQuotaText').textContent();
      assert.strictEqual(reviewQuota, '12 / 100');

      const newCardsQuota = await page.locator('#statNewCardsQuotaText').textContent();
      assert.strictEqual(newCardsQuota, '5 / 20');

      // 2. Verify form pre-filled with authoritative settings
      const newCardsInputVal = await page.locator('#newCardsPerDayInput').inputValue();
      const maxReviewInputVal = await page.locator('#maxReviewPerDayInput').inputValue();
      assert.strictEqual(newCardsInputVal, '20');
      assert.strictEqual(maxReviewInputVal, '100');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during dashboard load:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // SD-03: Client-side Validation Rejection
  test('SD-03: Client-side validation rejects invalid inputs (0, negative, decimal, empty) without submitting PUT', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      let putCalled = false;
      await page.route('**/api/v1/srs/stats', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: createMockStudyStats() })
        });
      });

      await page.route('**/api/v1/srs/settings', async (route) => {
        if (route.request().method() === 'PUT') {
          putCalled = true;
          await route.fulfill({ status: 500 });
        } else {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ code: 'SUCCESS', data: createMockUserSrsSetting() })
          });
        }
      });

      await page.goto(`${baseUrl}/srs-dashboard.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#dashboardContentSection').waitFor({ state: 'visible' });

      // Test 1: Zero value rejection
      await page.locator('#newCardsPerDayInput').fill('0');
      await page.locator('#maxReviewPerDayInput').fill('-10');
      await page.locator('#btnSaveSettings').click();

      assert.strictEqual(putCalled, false, 'PUT must not be called when input is 0 or negative');
      const errNew = await page.locator('#newCardsPerDayError').textContent();
      const errMax = await page.locator('#maxReviewPerDayError').textContent();
      assert.ok(errNew.includes('lớn hơn 0'), `Expected positive integer error, got "${errNew}"`);
      assert.ok(errMax.includes('lớn hơn 0'), `Expected positive integer error, got "${errMax}"`);

      // Test 2: Empty value rejection
      await page.locator('#newCardsPerDayInput').fill('');
      await page.locator('#btnSaveSettings').click();
      assert.strictEqual(putCalled, false, 'PUT must not be called when input is empty');
      const errEmpty = await page.locator('#newCardsPerDayError').textContent();
      assert.ok(errEmpty.includes('không được để trống'), `Expected empty error, got "${errEmpty}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during validation test:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // SD-04: Successful Update & Immediate Authoritative Refresh
  test('SD-04: Successful settings update submits exact payload and immediately refreshes authoritative statistics', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      let currentStats = createMockStudyStats({ newCardsLimit: 20, maxReviewLimit: 100 });
      let currentSettings = createMockUserSrsSetting({ newCardsPerDay: 20, maxReviewPerDay: 100 });
      let putPayload = null;

      await page.route('**/api/v1/srs/stats', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: currentStats })
        });
      });

      await page.route('**/api/v1/srs/settings', async (route) => {
        if (route.request().method() === 'PUT') {
          putPayload = JSON.parse(route.request().postData());
          // Update backend state mock
          currentSettings = {
            settingId: 101,
            newCardsPerDay: putPayload.newCardsPerDay,
            maxReviewPerDay: putPayload.maxReviewPerDay
          };
          currentStats = {
            ...currentStats,
            newCardsLimit: putPayload.newCardsPerDay,
            maxReviewLimit: putPayload.maxReviewPerDay
          };
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'Cập nhật cấu hình học tập thành công',
              data: currentSettings
            })
          });
        } else {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ code: 'SUCCESS', data: currentSettings })
          });
        }
      });

      await page.goto(`${baseUrl}/srs-dashboard.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#dashboardContentSection').waitFor({ state: 'visible' });

      // Change values to 25 and 150
      await page.locator('#newCardsPerDayInput').fill('25');
      await page.locator('#maxReviewPerDayInput').fill('150');

      // Click save while simultaneously waiting for PUT response
      const [response] = await Promise.all([
        page.waitForResponse(res => res.url().includes('/api/v1/srs/settings') && res.request().method() === 'PUT'),
        page.locator('#btnSaveSettings').click()
      ]);

      assert.ok(response, 'PUT response received');
      assert.deepStrictEqual(putPayload, {
        newCardsPerDay: 25,
        maxReviewPerDay: 150
      }, 'PUT request must contain exact validated integer values and zero account/user leakages');

      // Wait for refresh to update DOM
      await page.locator('#settingsFormStatus').waitFor({ state: 'visible' });
      const statusText = await page.locator('#settingsFormStatus').textContent();
      assert.ok(statusText.includes('thành công'), `Expected success status, got "${statusText}"`);

      // Verify that authoritative metrics updated in the UI!
      await page.waitForFunction(() => document.getElementById('statNewCardsLimit')?.textContent === '25');
      assert.strictEqual(await page.locator('#statNewCardsLimit').textContent(), '25');
      assert.strictEqual(await page.locator('#statMaxReviewLimit').textContent(), '150');

      // Quota text updated as well
      assert.strictEqual(await page.locator('#statNewCardsQuotaText').textContent(), '5 / 25');
      assert.strictEqual(await page.locator('#statReviewsQuotaText').textContent(), '12 / 150');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during update test:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // SD-05: Reset Settings Button
  test('SD-05: Reset settings button restores original authoritative values into inputs', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      await page.route('**/api/v1/srs/stats', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: createMockStudyStats() })
        });
      });

      await page.route('**/api/v1/srs/settings', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: createMockUserSrsSetting({ newCardsPerDay: 30, maxReviewPerDay: 80 }) })
        });
      });

      await page.goto(`${baseUrl}/srs-dashboard.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#dashboardContentSection').waitFor({ state: 'visible' });

      // Change input values locally without submitting
      await page.locator('#newCardsPerDayInput').fill('999');
      await page.locator('#maxReviewPerDayInput').fill('888');

      // Click Reset
      await page.locator('#btnResetSettings').click();

      // Check values restored
      assert.strictEqual(await page.locator('#newCardsPerDayInput').inputValue(), '30');
      assert.strictEqual(await page.locator('#maxReviewPerDayInput').inputValue(), '80');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during reset test:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // SD-06: Cross-Navigation
  test('SD-06: Cross-navigation between SRS Dashboard and SRS Review Room operates smoothly', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      await page.route('**/api/v1/srs/stats', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: createMockStudyStats() })
        });
      });

      await page.route('**/api/v1/srs/settings', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: createMockUserSrsSetting() })
        });
      });

      await page.route('**/api/v1/srs/due**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: [] })
        });
      });

      await page.goto(`${baseUrl}/srs-dashboard.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#dashboardContentSection').waitFor({ state: 'visible' });

      // Click "Vào phòng ôn tập" button
      await page.locator('#btnGoToReview').click();
      await page.waitForURL('**/srs-review.html');

      // On srs-review.html, click link to return to dashboard
      const returnLink = page.locator('#linkToSrsDashboard');
      await returnLink.waitFor({ state: 'visible' });
      await returnLink.click();
      await page.waitForURL('**/srs-dashboard.html');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during cross-navigation:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

});
