/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: MODERATOR AUDIT HISTORY UI (TASK 9E.4)
 * File: tests/frontend/e2e/moderator-history.browser.mjs
 *
 * Verifies:
 * - MH-01: Authenticated Moderator loads history -> personal scope heading & audit records with time, lesson, action, reason, flagged fields.
 * - MH-02: Authenticated Admin loads history -> system-wide scope heading & global moderation logs.
 * - MH-03: Role guard blocks unauthorized roles (Learner) and shows warning container.
 * - MH-04: Truthful empty state presentation when caller has zero moderation records.
 * - MH-05: Server-side pagination navigation requests correct page parameters.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function createMockHistoryLogs() {
  return [
    {
      logId: 101,
      lessonId: 9001,
      lessonTitle: 'HSK 1 — Bài 1: Chào hỏi',
      moderatorId: 3001,
      moderatorEmail: 'moderator_user@example.com',
      action: 'Approve',
      rejectionReason: null,
      flaggedFields: null,
      createdAt: '2026-09-16T09:15:30Z'
    },
    {
      logId: 102,
      lessonId: 9002,
      lessonTitle: 'HSK 1 — Bài 2: Gia đình',
      moderatorId: 3001,
      moderatorEmail: 'moderator_user@example.com',
      action: 'Reject',
      rejectionReason: 'Pinyin từ vựng số 2 chưa chuẩn thanh điệu',
      flaggedFields: '["title", "vocabularies[0].pinyin"]',
      createdAt: '2026-09-16T10:45:12Z'
    }
  ];
}

function createMockAdminGlobalLogs() {
  return [
    {
      logId: 201,
      lessonId: 9003,
      lessonTitle: 'HSK 2 — Bài 3: Đi chợ',
      moderatorId: 3001,
      moderatorEmail: 'moderator_alpha@example.com',
      action: 'Approve',
      rejectionReason: null,
      flaggedFields: null,
      createdAt: '2026-09-16T08:00:00Z'
    },
    {
      logId: 202,
      lessonId: 9004,
      lessonTitle: 'HSK 2 — Bài 4: Giao thông',
      moderatorId: 3002,
      moderatorEmail: 'moderator_beta@example.com',
      action: 'Reject',
      rejectionReason: 'Âm thanh phát âm bị rè',
      flaggedFields: '["audioUrl"]',
      createdAt: '2026-09-16T09:30:00Z'
    }
  ];
}

async function setupUserSession(page, role = 'Moderator', email = 'moderator_user@example.com') {
  await page.addInitScript(`
    localStorage.setItem('access_token', 'test_jwt_token_for_${role}');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: 3001,
      emailOrPhone: '${email}',
      fullName: 'Người Dùng Kiểm Thử',
      roles: ['${role}']
    }));
  `);
}

describe('Browser E2E: Moderator Audit History UI (Task 9E.4)', () => {
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

  // MH-01: Moderator views personal history logs with audit metadata
  test('MH-01: Authenticated Moderator loads personal history with verified audit fields', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupUserSession(page, 'Moderator');

      const mockLogs = createMockHistoryLogs();

      // Mock GET /api/v1/moderator/history
      await page.route(/\/api\/v1\/moderator\/history(\?.*)?$/, async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: mockLogs,
              page: 0,
              size: 20,
              totalElements: mockLogs.length,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${baseUrl}/moderator-history.html`, { waitUntil: 'domcontentloaded' });

      // Verify heading reflects personal history
      const heading = page.locator('#historyHeading');
      await heading.waitFor({ state: 'visible' });
      const headingText = await heading.textContent();
      assert.ok(headingText.includes('Nhật Ký Kiểm Duyệt Của Tôi'), `Expected personal heading, got "${headingText}"`);

      // Verify Audit badge
      const auditBadge = page.locator('#historyAuditBadge');
      await auditBadge.waitFor({ state: 'visible' });
      assert.ok((await auditBadge.textContent()).includes('Bất Biến'));

      // Verify Table rows rendered
      const rows = page.locator('#moderatorHistoryTableBody tr');
      await rows.first().waitFor({ state: 'visible' });
      assert.strictEqual(await rows.count(), 2);

      // Verify Row 1 (Approve action)
      const row1Text = await rows.nth(0).textContent();
      assert.ok(row1Text.includes('HSK 1 — Bài 1'));
      assert.ok(row1Text.includes('Phê duyệt'));
      assert.ok(row1Text.includes('moderator_user@example.com'));

      // Verify Row 2 (Reject action + Reason + Flagged chips)
      const row2Text = await rows.nth(1).textContent();
      assert.ok(row2Text.includes('HSK 1 — Bài 2'));
      assert.ok(row2Text.includes('Từ chối'));
      assert.ok(row2Text.includes('Pinyin từ vựng số 2 chưa chuẩn thanh điệu'));
      assert.ok(row2Text.includes('Tiêu đề bài học'));

      // Verify flagged chips presence
      const chips = rows.nth(1).locator('.flagged-field-chip');
      assert.ok((await chips.count()) >= 1);

      // Verify result count badge
      const countBadge = page.locator('#historyResultCountBadge');
      assert.ok((await countBadge.textContent()).includes('2 mục'));

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors on page load:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // MH-02: Admin views system-wide history logs
  test('MH-02: Authenticated Admin loads system-wide history with global logs', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupUserSession(page, 'Admin', 'admin_boss@example.com');

      const mockLogs = createMockAdminGlobalLogs();

      // Mock GET /api/v1/moderator/history
      await page.route(/\/api\/v1\/moderator\/history(\?.*)?$/, async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: mockLogs,
              page: 0,
              size: 20,
              totalElements: mockLogs.length,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${baseUrl}/moderator-history.html`, { waitUntil: 'domcontentloaded' });

      // Verify heading reflects system-wide history
      const heading = page.locator('#historyHeading');
      await heading.waitFor({ state: 'visible' });
      const headingText = await heading.textContent();
      assert.ok(headingText.includes('Lịch Sử Kiểm Duyệt Hệ Thống'), `Expected system-wide heading, got "${headingText}"`);

      // Verify multiple moderators are distinguished in the table
      const rows = page.locator('#moderatorHistoryTableBody tr');
      await rows.first().waitFor({ state: 'visible' });
      assert.strictEqual(await rows.count(), 2);

      const row1 = await rows.nth(0).textContent();
      const row2 = await rows.nth(1).textContent();
      assert.ok(row1.includes('moderator_alpha@example.com'));
      assert.ok(row2.includes('moderator_beta@example.com'));

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // MH-03: Role guard blocks Learner role
  test('MH-03: Role guard blocks Learner role and renders unauthorized warning', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupUserSession(page, 'Learner', 'learner@example.com');

      await page.goto(`${baseUrl}/moderator-history.html`, { waitUntil: 'domcontentloaded' });

      // Auth guard container must be visible
      const guard = page.locator('#moderatorAuthGuardContainer');
      await guard.waitFor({ state: 'visible' });
      assert.ok(await guard.isVisible(), 'Guard container must be visible for Learner');

      // Workspace section must be hidden
      const workspace = page.locator('#moderatorHistoryWorkspaceSection');
      assert.ok(await workspace.isHidden(), 'Workspace section must be hidden');
    } finally {
      await context.close();
    }
  });

  // MH-04: Truthful empty state presentation
  test('MH-04: Displays friendly empty state when no moderation records exist', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupUserSession(page, 'Moderator');

      // Mock GET /api/v1/moderator/history with 0 items
      await page.route(/\/api\/v1\/moderator\/history(\?.*)?$/, async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: [],
              page: 0,
              size: 20,
              totalElements: 0,
              totalPages: 0
            }
          })
        });
      });

      await page.goto(`${baseUrl}/moderator-history.html`, { waitUntil: 'domcontentloaded' });

      // State container should show empty message
      const stateContainer = page.locator('#historyStateContainer');
      await stateContainer.waitFor({ state: 'visible' });
      const stateText = await stateContainer.textContent();
      assert.ok(stateText.includes('Chưa có nhật ký kiểm duyệt'));

      // Table container must remain hidden
      const tableContainer = page.locator('#historyTableContainer');
      assert.ok(await tableContainer.isHidden(), 'Table container must be hidden when empty');

      // Count badge must show 0 mục
      const countBadge = page.locator('#historyResultCountBadge');
      assert.ok((await countBadge.textContent()).includes('0 mục'));
    } finally {
      await context.close();
    }
  });

  // MH-05: Server-side pagination interaction
  test('MH-05: Server-side pagination requests correct page parameters on click', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupUserSession(page, 'Moderator');

      let requestedPage = null;

      // Mock multi-page response
      await page.route(/\/api\/v1\/moderator\/history(\?.*)?$/, async route => {
        const url = new URL(route.request().url());
        requestedPage = url.searchParams.get('page');

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: createMockHistoryLogs(),
              page: Number(requestedPage || 0),
              size: 20,
              totalElements: 45,
              totalPages: 3
            }
          })
        });
      });

      await page.goto(`${baseUrl}/moderator-history.html`, { waitUntil: 'domcontentloaded' });

      // Pagination nav must be visible
      const nav = page.locator('#historyPaginationNav');
      await nav.waitFor({ state: 'visible' });

      // Click Next page button
      const nextBtn = nav.locator('button[aria-label="Trang kế tiếp"]');
      await nextBtn.waitFor({ state: 'visible' });
      await nextBtn.click();

      // Verify that page=1 was requested
      assert.strictEqual(requestedPage, '1', 'Expected next page request to send page=1');
    } finally {
      await context.close();
    }
  });

});
