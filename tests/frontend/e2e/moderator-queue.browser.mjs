/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: MODERATOR REVIEW QUEUE UI (TASK 9E.1)
 * File: tests/frontend/e2e/moderator-queue.browser.mjs
 *
 * Verifies:
 * - MQ-01: Authenticated Moderator loads queue -> renders Pending lessons, creator email, vocab count, updated date, and review link.
 * - MQ-02: Server-side pagination and page size selection work correctly.
 * - MQ-03: Authenticated Admin access to the same queue.
 * - MQ-04: Role guard blocks unauthorized roles (Creator/Learner) and shows warning container.
 * - MQ-05: Truthful empty state presentation when no lessons are pending.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function createMockPendingLessons() {
  return [
    {
      lessonId: 101,
      title: 'HSK 1 — Bài 1: Chào hỏi',
      status: 'Pending',
      creatorId: 2001,
      creatorEmail: 'creator_one@example.com',
      vocabularyCount: 8,
      createdAt: '2026-09-01T08:00:00Z',
      updatedAt: '2026-09-10T14:30:00Z'
    },
    {
      lessonId: 102,
      title: 'HSK 1 — Bài 2: Cảm ơn và Tạm biệt',
      status: 'Pending',
      creatorId: 2002,
      creatorEmail: 'creator_two@example.com',
      vocabularyCount: 12,
      createdAt: '2026-09-02T09:00:00Z',
      updatedAt: '2026-09-11T16:45:00Z'
    },
    {
      lessonId: 103,
      title: 'HSK 2 — Bài 3: Mua sắm',
      status: 'Pending',
      creatorId: 2003,
      creatorEmail: 'creator_three@example.com',
      vocabularyCount: 15,
      createdAt: '2026-09-03T10:00:00Z',
      updatedAt: '2026-09-12T11:20:00Z'
    }
  ];
}

async function setupModeratorSession(page, role = 'Moderator') {
  await page.addInitScript(`
    localStorage.setItem('access_token', 'test_moderator_jwt_token');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: 3001,
      emailOrPhone: 'moderator_user@example.com',
      fullName: 'Kiểm Duyệt Viên Mẫu',
      roles: ['${role}']
    }));
  `);
}

describe('Browser E2E: Moderator Review Queue UI (Task 9E.1)', () => {
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

  // MQ-01: Moderator views pending queue with metadata and review action
  test('MQ-01: Authenticated Moderator loads queue with verified fields and review link', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupModeratorSession(page, 'Moderator');

      const mockLessons = createMockPendingLessons();

      // Mock GET /api/v1/moderator/lessons/pending
      await page.route(/\/api\/v1\/moderator\/lessons\/pending(\?.*)?$/, async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Lấy danh sách bài học chờ duyệt thành công',
            data: {
              items: mockLessons,
              page: 0,
              size: 20,
              totalElements: 3,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${baseUrl}/moderator-queue.html`, { waitUntil: 'domcontentloaded' });

      // Verify main heading
      const heading = page.getByRole('heading', { level: 1, name: /hàng đợi kiểm duyệt bài học/i });
      await assert.doesNotReject(heading.waitFor({ state: 'visible', timeout: 5000 }));

      // Verify result badge
      const badge = page.locator('#queueResultCountBadge');
      await assert.doesNotReject(badge.waitFor({ state: 'visible', timeout: 5000 }));
      const badgeText = await badge.textContent();
      assert.match(badgeText, /3 bài học chờ duyệt/);

      // Verify table rows
      const rows = page.locator('.moderator-queue-row');
      await assert.doesNotReject(rows.first().waitFor({ state: 'visible', timeout: 5000 }));
      assert.strictEqual(await rows.count(), 3);

      // Verify row 1 content
      const firstRow = rows.first();
      const rowText = await firstRow.textContent();
      assert.match(rowText, /#101/);
      assert.match(rowText, /HSK 1 — Bài 1: Chào hỏi/);
      assert.match(rowText, /creator_one@example\.com/);
      assert.match(rowText, /8 từ/);
      assert.match(rowText, /Chờ duyệt/);
      assert.match(rowText, /10\/09\/2026/); // Cập nhật lần cuối

      // Verify Review Action Link
      const reviewLink = firstRow.getByRole('link', { name: /xem xét/i });
      await assert.doesNotReject(reviewLink.waitFor({ state: 'visible', timeout: 3000 }));
      const href = await reviewLink.getAttribute('href');
      assert.match(href, /moderator-review\.html\?id=101/);
    } finally {
      await context.close();
    }
  });

  // MQ-02: Server-side pagination and page size selection
  test('MQ-02: Server-side pagination and page size dropdown trigger appropriate requests', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupModeratorSession(page, 'Moderator');

      let requestedPage = null;
      let requestedSize = null;

      await page.route(/\/api\/v1\/moderator\/lessons\/pending(\?.*)?$/, async route => {
        const url = new URL(route.request().url());
        requestedPage = url.searchParams.get('page');
        requestedSize = url.searchParams.get('size');

        const p = Number(requestedPage) || 0;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: [
                {
                  lessonId: p === 0 ? 101 : 102,
                  title: p === 0 ? 'Bài học Trang 1' : 'Bài học Trang 2',
                  status: 'Pending',
                  creatorId: 2001,
                  creatorEmail: 'test@example.com',
                  vocabularyCount: 5,
                  createdAt: '2026-09-01T00:00:00Z',
                  updatedAt: '2026-09-02T00:00:00Z'
                }
              ],
              page: p,
              size: Number(requestedSize) || 20,
              totalElements: 25,
              totalPages: 2
            }
          })
        });
      });

      await page.goto(`${baseUrl}/moderator-queue.html`, { waitUntil: 'domcontentloaded' });

      // Wait for table to load
      await page.locator('.moderator-queue-row').first().waitFor({ state: 'visible', timeout: 5000 });
      assert.strictEqual(requestedPage, '0');

      // Click Next page button concurrently with waitForResponse
      const nextBtn = page.locator('#btnQueueNextPage');
      await assert.doesNotReject(nextBtn.waitFor({ state: 'visible', timeout: 3000 }));
      await Promise.all([
        page.waitForResponse(res => res.url().includes('/moderator/lessons/pending') && res.status() === 200),
        nextBtn.click()
      ]);

      assert.strictEqual(requestedPage, '1');

      const updatedRow = page.locator('.moderator-queue-row').first();
      await assert.doesNotReject(updatedRow.waitFor({ state: 'visible', timeout: 3000 }));
      assert.match(await updatedRow.textContent(), /Bài học Trang 2/);

      // Change page size to 12 concurrently with waitForResponse
      const sizeSelect = page.locator('#queuePageSizeSelect');
      await Promise.all([
        page.waitForResponse(res => res.url().includes('/moderator/lessons/pending') && res.status() === 200),
        sizeSelect.selectOption('12')
      ]);

      assert.strictEqual(requestedSize, '12');
      assert.strictEqual(requestedPage, '0');
    } finally {
      await context.close();
    }
  });

  // MQ-03: Admin role access to the same queue
  test('MQ-03: Authenticated Admin access to Moderator Queue', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupModeratorSession(page, 'Admin');

      await page.route(/\/api\/v1\/moderator\/lessons\/pending(\?.*)?$/, async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: [
                {
                  lessonId: 999,
                  title: 'Bài học Admin kiểm duyệt',
                  status: 'Pending',
                  creatorId: 100,
                  creatorEmail: 'author@domain.com',
                  vocabularyCount: 20,
                  createdAt: '2026-09-01T00:00:00Z',
                  updatedAt: '2026-09-05T00:00:00Z'
                }
              ],
              page: 0,
              size: 20,
              totalElements: 1,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${baseUrl}/moderator-queue.html`, { waitUntil: 'domcontentloaded' });

      // Table should render without being blocked
      const row = page.locator('.moderator-queue-row').first();
      await assert.doesNotReject(row.waitFor({ state: 'visible', timeout: 5000 }));
      assert.match(await row.textContent(), /Bài học Admin kiểm duyệt/);

      // Auth guard container must remain hidden
      const guardContainer = page.locator('#moderatorAuthGuardContainer');
      assert.strictEqual(await guardContainer.evaluate(el => el.classList.contains('d-none')), true);
    } finally {
      await context.close();
    }
  });

  // MQ-04: Role guard blocks unauthorized roles (Learner/Creator)
  test('MQ-04: Role guard blocks Learner role and shows warning container', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupModeratorSession(page, 'Learner');

      let apiCalled = false;
      await page.route(/\/api\/v1\/moderator\/lessons\/pending(\?.*)?$/, async route => {
        apiCalled = true;
        await route.fulfill({ status: 403, body: JSON.stringify({ code: 'FORBIDDEN', message: 'Không có quyền' }) });
      });

      await page.goto(`${baseUrl}/moderator-queue.html`, { waitUntil: 'domcontentloaded' });

      // Auth guard container must become visible
      const guardContainer = page.locator('#moderatorAuthGuardContainer');
      await assert.doesNotReject(guardContainer.waitFor({ state: 'visible', timeout: 5000 }));
      assert.match(await guardContainer.textContent(), /Yêu Cầu Quyền Kiểm Duyệt \(Moderator\)/);

      // Workspace section must be hidden
      const workspaceSection = page.locator('#moderatorWorkspaceSection');
      assert.strictEqual(await workspaceSection.evaluate(el => el.classList.contains('d-none')), true);

      // Client-side guard should not have called backend API
      assert.strictEqual(apiCalled, false);
    } finally {
      await context.close();
    }
  });

  // MQ-05: Truthful empty state presentation
  test('MQ-05: Shows truthful empty state when no lessons are pending', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupModeratorSession(page, 'Moderator');

      await page.route(/\/api\/v1\/moderator\/lessons\/pending(\?.*)?$/, async route => {
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

      await page.goto(`${baseUrl}/moderator-queue.html`, { waitUntil: 'domcontentloaded' });

      // Badge should show 0
      const badge = page.locator('#queueResultCountBadge');
      await assert.doesNotReject(badge.waitFor({ state: 'visible', timeout: 5000 }));
      assert.match(await badge.textContent(), /0 bài học/);

      // Empty state message
      const emptyState = page.locator('#queueStateContainer');
      await assert.doesNotReject(emptyState.waitFor({ state: 'visible', timeout: 5000 }));
      const emptyText = await emptyState.textContent();
      assert.match(emptyText, /Hàng đợi đang trống/);
      assert.match(emptyText, /không có bài học nào đang ở trạng thái Chờ duyệt/);

      // Table container must be hidden
      const tableContainer = page.locator('#queueTableContainer');
      assert.strictEqual(await tableContainer.evaluate(el => el.classList.contains('d-none')), true);
    } finally {
      await context.close();
    }
  });

});
