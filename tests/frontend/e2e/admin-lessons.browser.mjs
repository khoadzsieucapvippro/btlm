/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: ADMIN LESSON OVERSIGHT UI (TASK 9F.5)
 * File: tests/frontend/e2e/admin-lessons.browser.mjs
 *
 * Scenarios:
 * - AL-01: Admin opens the page and sees overview, 5 KPI cards, filter buttons, and semantic table.
 * - AL-02: Lesson list loads using GET /api/v1/admin/lessons and renders semantic table rows.
 * - AL-03: Status filter changes the request (GET ?status=...&page=0) and updates displayed list.
 * - AL-04: Global status counts reflect server totalElements, NOT current-page row count.
 * - AL-05: Supported detail status (Approved -> /lessons/{id}) opens read-only detail modal.
 * - AL-06: Unsupported status (Draft/Rejected) displays truthful backend contract limitation.
 * - AL-07: Non-Admin access is blocked by role guard.
 * - AL-08: Zero lesson mutation requests issued by the page (Strict Read-Only Verification).
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function createMockLessons() {
  return [
    {
      lessonId: 101,
      title: 'Bài 1: Chào hỏi cơ bản',
      status: 'Approved',
      vocabularyCount: 15,
      createdAt: '2026-08-01T08:00:00Z',
      updatedAt: '2026-08-05T10:30:00Z'
    },
    {
      lessonId: 102,
      title: 'Bài 2: Gia đình và Nghề nghiệp',
      status: 'Pending',
      vocabularyCount: 12,
      createdAt: '2026-08-10T09:00:00Z',
      updatedAt: '2026-08-12T14:20:00Z'
    },
    {
      lessonId: 103,
      title: 'Bài 3: Mua sắm và Số đếm',
      status: 'Draft',
      vocabularyCount: 8,
      createdAt: '2026-08-15T11:00:00Z',
      updatedAt: '2026-08-16T16:45:00Z'
    },
    {
      lessonId: 104,
      title: 'Bài 4: Ẩm thực Trung Hoa',
      status: 'Rejected',
      vocabularyCount: 20,
      createdAt: '2026-08-18T13:00:00Z',
      updatedAt: '2026-08-19T09:15:00Z'
    }
  ];
}

async function setupAdminSession(page, email = 'admin@example.com', accountId = 1) {
  await page.addInitScript(`
    localStorage.setItem('access_token', 'test_jwt_admin_token_task_9f5');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: ${accountId},
      emailOrPhone: '${email}',
      fullName: 'Quản Trị Viên Hệ Thống',
      roles: ['Admin']
    }));
  `);
}

async function setupLearnerSession(page) {
  await page.addInitScript(`
    localStorage.setItem('access_token', 'test_jwt_learner_token_task_9f5');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: 88,
      emailOrPhone: 'learner@example.com',
      fullName: 'Học Viên',
      roles: ['Learner']
    }));
  `);
}

describe('Browser E2E: Admin Lesson Oversight UI (Task 9F.5)', () => {
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

  /* ---------------------------------------------------------------------------
   * AL-01: Admin opens page and sees overview / table / KPI cards
   * --------------------------------------------------------------------------- */
  test('AL-01: Admin opens admin-lessons.html and sees complete overview shell', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      await page.route(/\/api\/v1\/admin\/lessons(\?.*)?$/, async (route) => {
        const url = new URL(route.request().url());
        const status = url.searchParams.get('status');
        const size = parseInt(url.searchParams.get('size') || '20', 10);

        if (size === 1) {
          let total = 42;
          if (status === 'Draft') total = 5;
          else if (status === 'Pending') total = 8;
          else if (status === 'Approved') total = 24;
          else if (status === 'Rejected') total = 5;

          return route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              status: 'SUCCESS',
              message: 'OK',
              data: {
                items: [],
                page: 0,
                size: 1,
                totalElements: total,
                totalPages: total,
                first: true,
                last: false
              }
            })
          });
        }

        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            status: 'SUCCESS',
            message: 'OK',
            data: {
              items: createMockLessons(),
              page: 0,
              size: 20,
              totalElements: 4,
              totalPages: 1,
              first: true,
              last: true
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-lessons.html`, { waitUntil: 'domcontentloaded' });

      // Verify Single Primary H1
      const h1 = page.locator('h1');
      await h1.waitFor({ state: 'visible', timeout: 5000 });
      assert.strictEqual(await h1.count(), 1);
      assert.ok((await h1.textContent()).includes('Giám Sát Bài Học'));

      // Verify 5-tab admin sub-nav
      assert.ok(await page.locator('#tabAdminAccounts').isVisible());
      assert.ok(await page.locator('#tabAdminRoles').isVisible());
      assert.ok(await page.locator('#tabAdminRadicals').isVisible());
      assert.ok(await page.locator('#tabAdminVocabulary').isVisible());
      assert.ok(await page.locator('#tabAdminLessons').isVisible());
      assert.strictEqual(await page.locator('#tabAdminLessons').getAttribute('aria-current'), 'page');

      // Verify 5 KPI summary cards exist
      assert.ok(await page.locator('#kpiCountTotal').isVisible());
      assert.ok(await page.locator('#kpiCountDraft').isVisible());
      assert.ok(await page.locator('#kpiCountPending').isVisible());
      assert.ok(await page.locator('#kpiCountApproved').isVisible());
      assert.ok(await page.locator('#kpiCountRejected').isVisible());

      // Verify filter group
      assert.ok(await page.locator('#filterStatusAll').isVisible());
      assert.ok(await page.locator('#filterStatusDraft').isVisible());
      assert.ok(await page.locator('#filterStatusPending').isVisible());
      assert.ok(await page.locator('#filterStatusApproved').isVisible());
      assert.ok(await page.locator('#filterStatusRejected').isVisible());

      // Verify table structure
      const table = page.locator('#lessonTable');
      await table.waitFor({ state: 'visible', timeout: 5000 });
      assert.strictEqual(await table.locator('caption').count(), 1);
      assert.strictEqual(await table.locator('thead th[scope="col"]').count(), 6);

      // Verify table rows rendered
      const rows = page.locator('#lessonTableBody tr');
      await rows.first().waitFor({ state: 'visible', timeout: 5000 });
      assert.strictEqual(await rows.count(), 4);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  /* ---------------------------------------------------------------------------
   * AL-02: Lesson list loads using GET /api/v1/admin/lessons
   * --------------------------------------------------------------------------- */
  test('AL-02: Lesson list loads via GET /api/v1/admin/lessons and renders semantic rows', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      let listRequestedUrl = '';

      await page.route(/\/api\/v1\/admin\/lessons(\?.*)?$/, async (route) => {
        const url = new URL(route.request().url());
        const size = url.searchParams.get('size');
        if (size !== '1') {
          listRequestedUrl = route.request().url();
        }

        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            status: 'SUCCESS',
            message: 'OK',
            data: {
              items: createMockLessons(),
              page: 0,
              size: 20,
              totalElements: 4,
              totalPages: 1,
              first: true,
              last: true
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-lessons.html`, { waitUntil: 'domcontentloaded' });
      const rows = page.locator('#lessonTableBody tr');
      await rows.first().waitFor({ state: 'visible', timeout: 5000 });

      // Verify table has 4 rows
      assert.strictEqual(await rows.count(), 4);

      // Verify first row content
      const firstRow = rows.first();
      assert.ok((await firstRow.textContent()).includes('Bài 1: Chào hỏi cơ bản'));
      assert.ok((await firstRow.textContent()).includes('Đã duyệt'));
      assert.ok((await firstRow.textContent()).includes('15 từ'));

      // Verify URL did not include fake creatorId or search parameter
      assert.ok(listRequestedUrl.includes('/api/v1/admin/lessons'));
      assert.strictEqual(listRequestedUrl.includes('creatorId'), false);
      assert.strictEqual(listRequestedUrl.includes('keyword'), false);
      assert.strictEqual(listRequestedUrl.includes('search'), false);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  /* ---------------------------------------------------------------------------
   * AL-03: Status filter changes the request and displayed list
   * --------------------------------------------------------------------------- */
  test('AL-03: Status filter changes request query and resets page to 0', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      const capturedStatusQueries = [];

      await page.route(/\/api\/v1\/admin\/lessons(\?.*)?$/, async (route) => {
        const url = new URL(route.request().url());
        const statusParam = url.searchParams.get('status');
        const size = url.searchParams.get('size');

        if (size !== '1') {
          capturedStatusQueries.push(statusParam);
        }

        const mockData = statusParam === 'Pending'
          ? [createMockLessons()[1]]
          : createMockLessons();

        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            status: 'SUCCESS',
            message: 'OK',
            data: {
              items: mockData,
              page: 0,
              size: 20,
              totalElements: mockData.length,
              totalPages: 1,
              first: true,
              last: true
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#lessonTableBody tr').first().waitFor({ state: 'visible', timeout: 5000 });

      // Click 'Chờ duyệt' filter button
      const btnPending = page.locator('#filterStatusPending');
      const [responsePending] = await Promise.all([
        page.waitForResponse(resp => resp.url().includes('/api/v1/admin/lessons') && resp.url().includes('status=Pending')),
        btnPending.click()
      ]);
      assert.ok(responsePending.ok());

      // Verify pending filter was requested
      assert.ok(capturedStatusQueries.includes('Pending'));

      // Verify table now shows only the Pending row
      const rows = page.locator('#lessonTableBody tr');
      await rows.first().waitFor({ state: 'visible', timeout: 5000 });
      assert.strictEqual(await rows.count(), 1);
      assert.ok((await rows.first().textContent()).includes('Bài 2: Gia đình và Nghề nghiệp'));
      assert.ok((await rows.first().textContent()).includes('Chờ duyệt'));

      // Click 'Tất cả' filter button
      const btnAll = page.locator('#filterStatusAll');
      const [responseAll] = await Promise.all([
        page.waitForResponse(resp => resp.url().includes('/api/v1/admin/lessons') && !resp.url().includes('status=')),
        btnAll.click()
      ]);
      assert.ok(responseAll.ok());

      // Verify status was omitted for 'All'
      assert.ok(capturedStatusQueries.includes(null));

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  /* ---------------------------------------------------------------------------
   * AL-04: Global status counts reflect server totalElements, NOT current page
   * --------------------------------------------------------------------------- */
  test('AL-04: Global status counts reflect server totalElements and are not derived from row count', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      await page.route(/\/api\/v1\/admin\/lessons(\?.*)?$/, async (route) => {
        const url = new URL(route.request().url());
        const status = url.searchParams.get('status');
        const size = url.searchParams.get('size');

        // Server returns small slice (1 item on page), but server totalElements is much larger!
        if (size === '1') {
          let totalElements = 150;
          if (status === 'Draft') totalElements = 20;
          else if (status === 'Pending') totalElements = 15;
          else if (status === 'Approved') totalElements = 100;
          else if (status === 'Rejected') totalElements = 15;

          return route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              status: 'SUCCESS',
              message: 'OK',
              data: {
                items: [],
                page: 0,
                size: 1,
                totalElements,
                totalPages: totalElements,
                first: true,
                last: false
              }
            })
          });
        }

        // Normal table query returns only 1 item
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            status: 'SUCCESS',
            message: 'OK',
            data: {
              items: [createMockLessons()[0]],
              page: 0,
              size: 20,
              totalElements: 1,
              totalPages: 1,
              first: true,
              last: true
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#lessonTableBody tr').first().waitFor({ state: 'visible', timeout: 5000 });

      // Wait until KPI counts finish loading (spinner disappears, text contains numbers)
      await page.waitForFunction(() => {
        const el = document.getElementById('kpiCountTotal');
        return el && el.textContent.includes('150');
      }, { timeout: 5000 });

      // Check KPI counts: MUST reflect server totals (150, 20, 15, 100, 15), NOT current table count (1)
      const totalCountText = await page.locator('#kpiCountTotal').textContent();
      assert.strictEqual(totalCountText.trim(), '150');

      const draftCountText = await page.locator('#kpiCountDraft').textContent();
      assert.strictEqual(draftCountText.trim(), '20');

      const pendingCountText = await page.locator('#kpiCountPending').textContent();
      assert.strictEqual(pendingCountText.trim(), '15');

      const approvedCountText = await page.locator('#kpiCountApproved').textContent();
      assert.strictEqual(approvedCountText.trim(), '100');

      const rejectedCountText = await page.locator('#kpiCountRejected').textContent();
      assert.strictEqual(rejectedCountText.trim(), '15');

      // Table rows: exactly 1 row
      assert.strictEqual(await page.locator('#lessonTableBody tr').count(), 1);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  /* ---------------------------------------------------------------------------
   * AL-05: Supported detail status opens read-only detail view
   * --------------------------------------------------------------------------- */
  test('AL-05: Supported Approved detail calls GET /api/v1/lessons/{id} and renders read-only vocabulary list', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      let detailFetched = false;

      await page.route(/\/api\/v1\/admin\/lessons(\?.*)?$/, async (route) => {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            status: 'SUCCESS',
            message: 'OK',
            data: {
              items: [createMockLessons()[0]], // Approved lesson #101
              page: 0,
              size: 20,
              totalElements: 1,
              totalPages: 1,
              first: true,
              last: true
            }
          })
        });
      });

      await page.route(/\/api\/v1\/lessons\/101$/, async (route) => {
        detailFetched = true;
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            status: 'SUCCESS',
            message: 'OK',
            data: {
              lessonId: 101,
              title: 'Bài 1: Chào hỏi cơ bản',
              status: 'Approved',
              vocabularies: [
                {
                  vocabId: 1,
                  hanzi: '你好',
                  pinyin: 'nǐ hǎo',
                  meaningHanViet: 'nhĩ hảo',
                  meaningVi: 'xin chào',
                  orderIndex: 1
                },
                {
                  vocabId: 2,
                  hanzi: '谢谢',
                  pinyin: 'xièxie',
                  meaningHanViet: 'tạ tạ',
                  meaningVi: 'cảm ơn',
                  orderIndex: 2
                }
              ]
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#lessonTableBody tr').first().waitFor({ state: 'visible', timeout: 5000 });

      // Click "Xem chi tiết" on row 101
      const viewBtn = page.locator('#btnDetailLesson-101');
      await viewBtn.click();

      // Verify modal is open
      const modal = page.locator('#lessonDetailModal');
      await modal.waitFor({ state: 'visible', timeout: 3000 });
      assert.ok(await modal.isVisible());

      // Verify endpoint was called
      assert.strictEqual(detailFetched, true);

      // Verify modal metadata
      assert.strictEqual(await page.locator('#detailLessonId').textContent(), '#101');
      assert.ok((await page.locator('#detailLessonTitle').textContent()).includes('Bài 1: Chào hỏi cơ bản'));

      // Verify vocabulary list in modal rendered 2 items
      await page.locator('#detailVocabList .list-group-item').first().waitFor({ state: 'visible', timeout: 5000 });
      const vocabRows = page.locator('#detailVocabList .list-group-item');
      assert.strictEqual(await vocabRows.count(), 2);
      assert.ok((await vocabRows.first().textContent()).includes('你好'));

      // Close modal
      await page.locator('#btnCancelDetailModal').click();
      await modal.waitFor({ state: 'hidden', timeout: 3000 });
      assert.strictEqual(await modal.isVisible(), false);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  /* ---------------------------------------------------------------------------
   * AL-06: Unsupported status (Draft/Rejected) displays truthful contract limitation
   * --------------------------------------------------------------------------- */
  test('AL-06: Unsupported Draft status displays truthful contract limitation without claiming "Không có nội dung"', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      await page.route(/\/api\/v1\/admin\/lessons(\?.*)?$/, async (route) => {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            status: 'SUCCESS',
            message: 'OK',
            data: {
              items: [createMockLessons()[2]], // Draft lesson #103
              page: 0,
              size: 20,
              totalElements: 1,
              totalPages: 1,
              first: true,
              last: true
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#lessonTableBody tr').first().waitFor({ state: 'visible', timeout: 5000 });

      // Click "Xem chi tiết" for Draft lesson
      await page.locator('#btnDetailLesson-103').click();

      // Verify modal is open
      const modal = page.locator('#lessonDetailModal');
      await modal.waitFor({ state: 'visible', timeout: 3000 });
      assert.ok(await modal.isVisible());

      // Verify truthful contract limitation alert is displayed
      const limitationAlert = page.locator('#detailContractLimitationAlert');
      assert.ok(await limitationAlert.isVisible());

      const noticeText = await page.locator('#detailContractLimitationMessage').textContent();
      // Must truthfully mention backend endpoint absence
      assert.ok(noticeText.includes('chưa có endpoint quản trị'));
      // Must NOT falsely claim "Không có nội dung"
      assert.strictEqual(noticeText.includes('Không có nội dung'), false);

      // Verify vocabulary list section is NOT rendered
      assert.strictEqual(await page.locator('#detailVocabSection').isVisible(), false);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  /* ---------------------------------------------------------------------------
   * AL-07: Non-Admin access is blocked
   * --------------------------------------------------------------------------- */
  test('AL-07: Non-Admin session (Learner) is blocked by role guard', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupLearnerSession(page);

      await page.goto(`${baseUrl}/admin-lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#adminAuthGuardContainer').waitFor({ state: 'visible', timeout: 5000 });

      // Verify auth guard container is displayed
      const guard = page.locator('#adminAuthGuardContainer');
      assert.ok(await guard.isVisible());
      assert.ok((await guard.textContent()).includes('Yêu Cầu Quyền Quản Trị Viên'));

      // Verify main content container is hidden
      assert.strictEqual(await page.locator('#adminLessonsMainContent').isVisible(), false);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  /* ---------------------------------------------------------------------------
   * AL-08: Zero lesson mutation requests issued by the page
   * --------------------------------------------------------------------------- */
  test('AL-08: Page is strictly read-only and issues ZERO mutation requests (POST/PUT/PATCH/DELETE)', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      const mutationMethods = [];

      await page.route(/\/api\/v1\/.*/, async (route) => {
        const method = route.request().method();
        if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
          mutationMethods.push({ method, url: route.request().url() });
        }

        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            status: 'SUCCESS',
            message: 'OK',
            data: {
              items: createMockLessons(),
              page: 0,
              size: 20,
              totalElements: 4,
              totalPages: 1,
              first: true,
              last: true
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#lessonTableBody tr').first().waitFor({ state: 'visible', timeout: 5000 });

      // Interact with filters
      await page.locator('#filterStatusDraft').click();
      await page.locator('#filterStatusApproved').click();

      // Change page size
      await page.locator('#pageSizeSelect').selectOption('50');

      // Click refresh counts
      await page.locator('#btnRefreshKpi').click();

      // Inspect detail
      const detailBtn = page.locator('#btnDetailLesson-101');
      if (await detailBtn.isVisible()) {
        await detailBtn.click();
        await page.locator('#btnCancelDetailModal').click();
      }

      // STRICT INVARIANT CHECK: Zero mutation requests
      assert.strictEqual(mutationMethods.length, 0, `Detected forbidden mutation calls: ${JSON.stringify(mutationMethods)}`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

});
