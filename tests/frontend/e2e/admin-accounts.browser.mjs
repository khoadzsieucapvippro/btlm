/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: ADMIN ACCOUNTS LIFECYCLE UI (TASK 9F.1)
 * File: tests/frontend/e2e/admin-accounts.browser.mjs
 *
 * Scenarios:
 * - AA-01: Admin opens admin-accounts.html -> sees account table with correct headers, columns, and data.
 * - AA-02: Admin search and status filter -> issues correct query parameters and renders filtered results.
 * - AA-03: Admin opens status confirmation modal for another user -> shows user identity, current status, and DEC-42 warning.
 * - AA-04: Admin submits status update -> sends PUT request, updates row with authoritative server response, shows toast.
 * - AA-05: Current Admin account cannot be self-disabled/self-banned -> row button is disabled with POL-8D-01 warning.
 * - AA-06: Role guard -> blocks non-Admin (Learner) and displays access denied message.
 * - AA-07: Verification Center diagnostic -> executes Section 15 probe successfully.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function createMockAccounts() {
  return [
    {
      accountId: 1,
      emailOrPhone: 'admin@example.com',
      fullName: 'Quản Trị Viên Chính',
      status: 'Active',
      roles: ['Admin'],
      createdAt: '2026-09-01T08:00:00Z',
      updatedAt: '2026-09-01T08:00:00Z'
    },
    {
      accountId: 2,
      emailOrPhone: 'learner1@example.com',
      fullName: 'Học Viên Một',
      status: 'Active',
      roles: ['Learner'],
      createdAt: '2026-09-05T09:30:00Z',
      updatedAt: '2026-09-05T09:30:00Z'
    },
    {
      accountId: 3,
      emailOrPhone: 'creator1@example.com',
      fullName: 'Tác Giả Một',
      status: 'Inactive',
      roles: ['Creator'],
      createdAt: '2026-09-10T14:15:00Z',
      updatedAt: '2026-09-10T14:15:00Z'
    },
    {
      accountId: 4,
      emailOrPhone: 'banned_user@example.com',
      fullName: 'Người Dùng Vi Phạm',
      status: 'Banned',
      roles: ['Learner'],
      createdAt: '2026-09-12T11:00:00Z',
      updatedAt: '2026-09-14T16:00:00Z'
    }
  ];
}

async function setupAdminSession(page, email = 'admin@example.com', accountId = 1) {
  await page.addInitScript(`
    localStorage.setItem('access_token', 'test_jwt_admin_token_task_9f1');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: ${accountId},
      emailOrPhone: '${email}',
      fullName: 'Quản Trị Viên Chính',
      roles: ['Admin']
    }));
  `);
}

async function setupLearnerSession(page) {
  await page.addInitScript(`
    localStorage.setItem('access_token', 'test_jwt_learner_token_task_9f1');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: 99,
      emailOrPhone: 'learner@example.com',
      fullName: 'Học Viên Thường',
      roles: ['Learner']
    }));
  `);
}

describe('Browser E2E: Admin Accounts Lifecycle UI (Task 9F.1)', () => {
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

  // ---------------------------------------------------------------------------
  // AA-01: Admin opens page and sees accounts list in table
  // ---------------------------------------------------------------------------
  test('AA-01: Admin can open admin-accounts.html and view semantic account list', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      const accounts = createMockAccounts();
      await page.route(/\/api\/v1\/admin\/accounts(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: accounts,
              page: 0,
              size: 20,
              totalElements: accounts.length,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-accounts.html`);

      // 1. Heading verification
      const heading = page.getByRole('heading', { level: 1, name: /Quản Trị Vòng Đời Tài Khoản/i });
      await heading.waitFor({ state: 'visible', timeout: 5000 });

      // 2. Table visibility & semantic structure
      const table = page.getByRole('table');
      await table.waitFor({ state: 'visible', timeout: 5000 });

      // 3. Result count badge
      const countBadge = page.locator('#accountResultCountBadge');
      await countBadge.waitFor({ state: 'visible' });
      const countText = await countBadge.textContent();
      assert.match(countText, /4 tài khoản/);

      // 4. Rows verification
      const rows = page.locator('#accountsTableBody tr');
      const rowCount = await rows.count();
      assert.strictEqual(rowCount, 4);

      // Verify row 1 contains Admin email and badge "Tài khoản của bạn"
      const firstRow = rows.nth(0);
      const firstRowText = await firstRow.textContent();
      assert.match(firstRowText, /admin@example\.com/);
      assert.match(firstRowText, /Tài khoản của bạn/);

      assert.deepStrictEqual(getRuntimeErrors(), []);

    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // AA-02: Search and Status Filtering
  // ---------------------------------------------------------------------------
  test('AA-02: Admin search and filter updates table results with correct query params', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      let lastCapturedUrl = null;
      await page.route(/\/api\/v1\/admin\/accounts(\?.*)?$/, async (route) => {
        lastCapturedUrl = route.request().url();
        const urlObj = new URL(lastCapturedUrl);
        const search = urlObj.searchParams.get('search') || '';
        const status = urlObj.searchParams.get('status') || '';

        let filtered = createMockAccounts();
        if (status) {
          filtered = filtered.filter(a => a.status.toLowerCase() === status.toLowerCase());
        }
        if (search) {
          filtered = filtered.filter(a => a.emailOrPhone.includes(search) || (a.fullName && a.fullName.includes(search)));
        }

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: filtered,
              page: 0,
              size: 20,
              totalElements: filtered.length,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-accounts.html`);
      await page.waitForSelector('#accountsTableBody tr', { timeout: 5000 });

      // 1. Test Status Filter: select "Inactive"
      const statusSelect = page.locator('#statusFilterSelect');
      await statusSelect.selectOption('Inactive');
      await page.waitForTimeout(300);

      // Verify captured query param had status=Inactive
      assert.ok(lastCapturedUrl.includes('status=Inactive'), `Expected status=Inactive in URL: ${lastCapturedUrl}`);
      const filteredRows = page.locator('#accountsTableBody tr');
      assert.strictEqual(await filteredRows.count(), 1);
      assert.match(await filteredRows.first().textContent(), /creator1@example\.com/);

      // 2. Test Search: search for "learner"
      const searchInput = page.locator('#accountSearchInput');
      await searchInput.fill('learner');
      await statusSelect.selectOption(''); // All statuses
      const searchBtn = page.locator('#btnAccountSearch');
      await searchBtn.click();
      await page.waitForTimeout(300);

      assert.ok(lastCapturedUrl.includes('search=learner'), `Expected search=learner in URL: ${lastCapturedUrl}`);
      assert.strictEqual(await filteredRows.count(), 1);
      assert.match(await filteredRows.first().textContent(), /learner1@example\.com/);

      assert.deepStrictEqual(getRuntimeErrors(), []);

    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // AA-03: Open Status Confirmation Modal
  // ---------------------------------------------------------------------------
  test('AA-03: Admin opens status confirmation modal and sees target details and DEC-42 warning', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      await page.route(/\/api\/v1\/admin\/accounts(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: createMockAccounts(),
              page: 0,
              size: 20,
              totalElements: 4,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-accounts.html`);
      await page.waitForSelector('#accountsTableBody tr', { timeout: 5000 });

      // Click "Đổi trạng thái" for learner1 (accountId = 2)
      const changeBtn = page.locator('#btnChangeStatus_2');
      await changeBtn.waitFor({ state: 'visible' });
      await changeBtn.click();

      // Modal must be visible
      const modal = page.locator('#statusConfirmModal');
      await modal.waitFor({ state: 'visible', timeout: 3000 });

      // Target details in modal
      const emailText = await page.locator('#modalTargetEmail').textContent();
      assert.strictEqual(emailText.trim(), 'learner1@example.com');

      const nameText = await page.locator('#modalTargetFullName').textContent();
      assert.strictEqual(nameText.trim(), 'Học Viên Một');

      const currentStatusBadge = await page.locator('#modalCurrentStatusBadge').textContent();
      assert.strictEqual(currentStatusBadge.trim(), 'Hoạt động');

      // Warning text mentions authorization_version / token invalidation
      const warningText = await page.locator('#statusModalWarningNotice').textContent();
      assert.match(warningText, /authorization_version/i);
      assert.match(warningText, /thu hồi tức thì/i);

      // Dismiss modal via cancel button
      const cancelBtn = page.locator('#btnCancelStatusModal');
      await cancelBtn.click();
      await modal.waitFor({ state: 'hidden', timeout: 3000 });

      assert.deepStrictEqual(getRuntimeErrors(), []);

    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // AA-04: Submit Status Mutation and Authoritative Row Update
  // ---------------------------------------------------------------------------
  test('AA-04: Admin submits status update and UI reflects authoritative server response', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      await page.route(/\/api\/v1\/admin\/accounts(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: createMockAccounts(),
              page: 0,
              size: 20,
              totalElements: 4,
              totalPages: 1
            }
          })
        });
      });

      let putBodyCaptured = null;
      await page.route(/\/api\/v1\/admin\/accounts\/2\/status$/, async (route) => {
        putBodyCaptured = JSON.parse(route.request().postData());
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Cập nhật trạng thái tài khoản thành công',
            data: {
              accountId: 2,
              emailOrPhone: 'learner1@example.com',
              fullName: 'Học Viên Một',
              status: putBodyCaptured.status,
              roles: ['Learner'],
              createdAt: '2026-09-05T09:30:00Z',
              updatedAt: '2026-09-16T15:00:00Z'
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-accounts.html`);
      await page.waitForSelector('#btnChangeStatus_2', { timeout: 5000 });

      // Open modal
      await page.locator('#btnChangeStatus_2').click();
      const modal = page.locator('#statusConfirmModal');
      await modal.waitFor({ state: 'visible' });

      // Select new status: Banned
      const statusSelect = page.locator('#modalNewStatusSelect');
      await statusSelect.selectOption('Banned');

      // Click Confirm
      const confirmBtn = page.locator('#btnConfirmStatusUpdate');
      await confirmBtn.click();

      // Modal closes
      await modal.waitFor({ state: 'hidden', timeout: 3000 });

      // Verify PUT payload
      assert.deepStrictEqual(putBodyCaptured, { status: 'Banned' });

      // Authoritative row badge updated to "Bị cấm"
      const statusBadge = page.locator('#accountStatusBadge_2');
      await statusBadge.waitFor({ state: 'visible' });
      const badgeText = await statusBadge.textContent();
      assert.strictEqual(badgeText.trim(), 'Bị cấm');

      assert.deepStrictEqual(getRuntimeErrors(), []);

    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // AA-05: Self-Protection Policy (POL-8D-01)
  // ---------------------------------------------------------------------------
  test('AA-05: Current Admin account cannot be self-disabled or self-banned in UI', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      // Logged in as admin@example.com (accountId = 1)
      await setupAdminSession(page, 'admin@example.com', 1);

      await page.route(/\/api\/v1\/admin\/accounts(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: createMockAccounts(),
              page: 0,
              size: 20,
              totalElements: 4,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-accounts.html`);
      await page.waitForSelector('#accountsTableBody tr', { timeout: 5000 });

      // Row 1 represents the current admin
      const row1 = page.locator('#accountRow_1');
      await row1.waitFor({ state: 'visible' });

      // Verify the status action button in Row 1 is disabled
      const actionBtn = row1.locator('button');
      await actionBtn.waitFor({ state: 'visible' });

      const isDisabled = await actionBtn.isDisabled();
      assert.strictEqual(isDisabled, true, 'Status change button on self account must be disabled');

      const titleAttr = await actionBtn.getAttribute('title');
      assert.match(titleAttr, /POL-8D-01/, 'Button title must mention POL-8D-01');

      // Other non-self account buttons MUST be enabled
      const otherBtn = page.locator('#btnChangeStatus_2');
      assert.strictEqual(await otherBtn.isDisabled(), false, 'Non-self accounts must be interactive');

      assert.deepStrictEqual(getRuntimeErrors(), []);

    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // AA-06: Role Guard Blocks Non-Admin
  // ---------------------------------------------------------------------------
  test('AA-06: Non-Admin session (Learner) is blocked by role guard and sees access denied', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupLearnerSession(page);

      await page.goto(`${baseUrl}/admin-accounts.html`);

      // Access denied card must be visible
      const guardContainer = page.locator('#adminAuthGuardContainer');
      await guardContainer.waitFor({ state: 'visible', timeout: 5000 });

      const guardMessage = page.locator('#adminAuthGuardMessage');
      const text = await guardMessage.textContent();
      assert.match(text, /từ chối/i);

      // Workspace must be hidden
      const workspace = page.locator('#adminAccountsWorkspaceSection');
      const isHidden = await workspace.evaluate(el => el.classList.contains('d-none'));
      assert.strictEqual(isHidden, true, 'Workspace must be hidden for non-admin');

      assert.deepStrictEqual(getRuntimeErrors(), []);

    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // AA-07: Verification Center Section 15 Diagnostic Probe
  // ---------------------------------------------------------------------------
  test('AA-07: Verification Center Section 15 Admin Accounts diagnostic probe runs and reports PASS', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      await page.route(/\/api\/v1\/admin\/accounts(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: createMockAccounts().slice(0, 2),
              page: 0,
              size: 5,
              totalElements: 4,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${baseUrl}/ui-verification.html#section-admin-accounts`);

      const probeBtn = page.locator('#btnProbeAdminAccountsCapability');
      await probeBtn.waitFor({ state: 'visible', timeout: 5000 });
      await probeBtn.click();

      const outputEl = page.locator('#adminAccountsProbeOutput');
      await page.waitForFunction(() => {
        const text = document.getElementById('adminAccountsProbeOutput')?.textContent || '';
        return text.includes('[PROBE PASS]');
      }, { timeout: 5000 });

      const outputText = await outputEl.textContent();
      assert.match(outputText, /Role Guard Invariant \(Admin\): PASS/);
      assert.match(outputText, /Bảo vệ tự thân \(POL-8D-01 Logic\): PASS/);
      assert.match(outputText, /Chuẩn hóa Query Params: PASS/);
      assert.match(outputText, /\[PROBE PASS\]/);

      const actualErrors = getRuntimeErrors().filter(err =>
        !err.includes('SRS Review') &&
        !err.includes('SrsDashboard') &&
        !err.includes('status of 401')
      );
      assert.deepStrictEqual(actualErrors, []);

    } finally {
      await context.close();
    }
  });
});
