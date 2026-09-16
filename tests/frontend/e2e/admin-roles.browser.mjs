/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: ADMIN ROLE MANAGEMENT & ROLE ASSIGNMENT UI (TASK 9F.2)
 * File: tests/frontend/e2e/admin-roles.browser.mjs
 *
 * Scenarios:
 * - AR-01: Admin opens admin-roles.html -> sees role catalog overview, search bar, and accounts table.
 * - AR-02: Role catalog is dynamically populated from backend GET /api/v1/admin/roles response.
 * - AR-03: Admin selects a target account -> opens modal displaying current roles and editable checkboxes.
 * - AR-04: Admin changes complete role set -> sends complete array in PUT request, updates table with server response, shows toast.
 * - AR-05: Self-protection (POL-8D-02) -> for current Admin, Admin role checkbox is locked/disabled, but other roles can be edited.
 * - AR-06: No-op detection -> unchanged role set displays notice and avoids sending unnecessary PUT.
 * - AR-07: Access guard -> non-Admin session is blocked with role requirement alert.
 * - AR-08: Verification Center Section 16 -> diagnostic executes safely with zero role mutations.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function createMockRoles() {
  return [
    { roleId: 1, roleName: 'Learner' },
    { roleId: 2, roleName: 'Creator' },
    { roleId: 3, roleName: 'Moderator' },
    { roleId: 4, roleName: 'Admin' }
  ];
}

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
      fullName: 'Tác Giả & Kiểm Duyệt',
      status: 'Active',
      roles: ['Creator', 'Moderator'],
      createdAt: '2026-09-10T14:15:00Z',
      updatedAt: '2026-09-10T14:15:00Z'
    }
  ];
}

async function setupAdminSession(page, email = 'admin@example.com', accountId = 1) {
  await page.addInitScript(`
    localStorage.setItem('access_token', 'test_jwt_admin_token_task_9f2');
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
    localStorage.setItem('access_token', 'test_jwt_learner_token_task_9f2');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: 99,
      emailOrPhone: 'learner@example.com',
      fullName: 'Học Viên Thường',
      roles: ['Learner']
    }));
  `);
}

describe('Browser E2E: Admin Role Management & Assignment UI (Task 9F.2)', () => {
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
  // AR-01: Admin opens page and sees role catalog + accounts table
  // ---------------------------------------------------------------------------
  test('AR-01: Admin can open admin-roles.html and view role catalog overview and accounts table', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      const roles = createMockRoles();
      const accounts = createMockAccounts();

      await page.route(/\/api\/v1\/admin\/roles(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', message: 'Thành công', data: roles })
        });
      });

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

      await page.goto(`${baseUrl}/admin-roles.html`);

      // 1. Heading verification
      const heading = page.getByRole('heading', { level: 1, name: /Quản Trị Phân Quyền Vai Trò/i });
      await heading.waitFor({ state: 'visible', timeout: 5000 });

      // 2. Role catalog section
      const catalogHeading = page.getByRole('heading', { level: 2, name: /Danh Mục Vai Trò Hệ Thống/i });
      await catalogHeading.waitFor({ state: 'visible' });

      // 3. Wait for accounts table rows to render
      await page.waitForSelector('#accountsTableBody tr', { timeout: 5000 });
      const rows = page.locator('#accountsTableBody tr');
      assert.strictEqual(await rows.count(), 3);

      assert.deepStrictEqual(getRuntimeErrors(), []);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // AR-02: Role catalog is populated from backend response
  // ---------------------------------------------------------------------------
  test('AR-02: Role catalog is dynamically populated from backend GET /api/v1/admin/roles response', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      const roles = createMockRoles();
      await page.route(/\/api\/v1\/admin\/roles(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', message: 'Thành công', data: roles })
        });
      });

      await page.route(/\/api\/v1\/admin\/accounts(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-roles.html`);

      // Wait for catalog items to render
      await page.waitForSelector('#roleCatalogContainer strong', { timeout: 5000 });

      // Verify all 4 roles appear with their ID badges and titles
      for (const r of roles) {
        const roleCard = page.locator('#roleCatalogContainer').getByText(r.roleName, { exact: true });
        await roleCard.waitFor({ state: 'visible' });

        const idBadge = page.locator('#roleCatalogContainer').getByText(`#${r.roleId}`);
        await idBadge.waitFor({ state: 'visible' });
      }

      assert.deepStrictEqual(getRuntimeErrors(), []);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // AR-03: Admin selects a target account and opens role modal
  // ---------------------------------------------------------------------------
  test('AR-03: Admin opens role-assignment modal for target user showing identity and current roles', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      const roles = createMockRoles();
      const accounts = createMockAccounts();

      await page.route(/\/api\/v1\/admin\/roles(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', message: 'OK', data: roles })
        });
      });

      await page.route(/\/api\/v1\/admin\/accounts(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: { items: accounts, page: 0, size: 20, totalElements: accounts.length, totalPages: 1 }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-roles.html`);
      await page.waitForSelector('#accountsTableBody tr', { timeout: 5000 });

      // Click "Phân quyền" on account 2 (learner1)
      const btnEdit = page.locator('#btnEditRoles_2');
      await btnEdit.waitFor({ state: 'visible' });
      await btnEdit.click();

      // Modal appears
      const modal = page.locator('#roleAssignmentModal');
      await modal.waitFor({ state: 'visible', timeout: 3000 });

      // Target identity displayed
      const targetEmail = await page.locator('#modalTargetEmail').textContent();
      assert.strictEqual(targetEmail.trim(), 'learner1@example.com');

      const targetId = await page.locator('#modalTargetAccountId').textContent();
      assert.strictEqual(targetId.trim(), '#2');

      // Learner checkbox should be checked, others unchecked
      const learnerCheckbox = page.locator('#roleCheckbox_Learner');
      assert.strictEqual(await learnerCheckbox.isChecked(), true);

      const creatorCheckbox = page.locator('#roleCheckbox_Creator');
      assert.strictEqual(await creatorCheckbox.isChecked(), false);

      assert.deepStrictEqual(getRuntimeErrors(), []);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // AR-04: Admin changes complete role set and sends PUT
  // ---------------------------------------------------------------------------
  test('AR-04: Admin mutates complete role set, sends complete array via PUT, and UI reflects server response', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      const roles = createMockRoles();
      const accounts = createMockAccounts();

      await page.route(/\/api\/v1\/admin\/roles(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', message: 'OK', data: roles })
        });
      });

      await page.route(/\/api\/v1\/admin\/accounts(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: { items: accounts, page: 0, size: 20, totalElements: accounts.length, totalPages: 1 }
          })
        });
      });

      let capturedPutPayload = null;
      await page.route(/\/api\/v1\/admin\/accounts\/2\/roles$/, async (route) => {
        capturedPutPayload = JSON.parse(route.request().postData());
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Cập nhật phân quyền thành công',
            data: {
              accountId: 2,
              emailOrPhone: 'learner1@example.com',
              fullName: 'Học Viên Một',
              status: 'Active',
              roles: ['Learner', 'Creator'],
              createdAt: '2026-09-05T09:30:00Z',
              updatedAt: '2026-09-16T12:00:00Z'
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-roles.html`);
      await page.waitForSelector('#accountsTableBody tr', { timeout: 5000 });

      // Open modal on account 2
      await page.locator('#btnEditRoles_2').click();
      await page.locator('#roleAssignmentModal').waitFor({ state: 'visible', timeout: 3000 });

      // Check 'Creator' checkbox (now selected roles: Learner + Creator)
      await page.locator('#roleCheckbox_Creator').check();

      // Submit
      await page.locator('#btnSaveRoles').click();

      // Verify modal closed
      await page.waitForFunction(() => !document.getElementById('roleAssignmentModal').open, { timeout: 5000 });

      assert.ok(capturedPutPayload, 'PUT request must have been sent');
      assert.ok(Array.isArray(capturedPutPayload.roles), 'Payload must contain a roles array');
      assert.strictEqual(capturedPutPayload.roles.length, 2);
      assert.ok(capturedPutPayload.roles.includes('Learner'));
      assert.ok(capturedPutPayload.roles.includes('Creator'));

      // Check that the table cell for account 2 was updated with new role badges
      const rolesCell = page.locator('#accountRolesCell_2');
      const cellText = await rolesCell.textContent();
      assert.ok(cellText.includes('Học viên'));
      assert.ok(cellText.includes('Tác giả'));

      assert.deepStrictEqual(getRuntimeErrors(), []);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // AR-05: Self-protection (POL-8D-02)
  // ---------------------------------------------------------------------------
  test('AR-05: Current Admin editing own account cannot remove Admin, but can edit other roles', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page, 'admin@example.com', 1);

      const roles = createMockRoles();
      const accounts = createMockAccounts();

      await page.route(/\/api\/v1\/admin\/roles(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', message: 'OK', data: roles })
        });
      });

      await page.route(/\/api\/v1\/admin\/accounts(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: { items: accounts, page: 0, size: 20, totalElements: accounts.length, totalPages: 1 }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-roles.html`);
      await page.waitForSelector('#accountsTableBody tr', { timeout: 5000 });

      // Open modal on account 1 (Self Admin)
      await page.locator('#btnEditRoles_1').click();
      await page.locator('#roleAssignmentModal').waitFor({ state: 'visible', timeout: 3000 });

      // Self demotion notice must be visible
      const notice = page.locator('#modalSelfDemotionNotice');
      await notice.waitFor({ state: 'visible' });
      assert.ok((await notice.textContent()).includes('POL-8D-02'));

      // Admin checkbox must be checked and DISABLED
      const adminCheckbox = page.locator('#roleCheckbox_Admin');
      assert.strictEqual(await adminCheckbox.isChecked(), true);
      assert.strictEqual(await adminCheckbox.isDisabled(), true);

      // Other checkboxes (e.g. Creator) must be ENABLED
      const creatorCheckbox = page.locator('#roleCheckbox_Creator');
      assert.strictEqual(await creatorCheckbox.isDisabled(), false);

      assert.deepStrictEqual(getRuntimeErrors(), []);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // AR-06: No-op detection
  // ---------------------------------------------------------------------------
  test('AR-06: No-op role selection displays notice and avoids sending PUT request', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      const roles = createMockRoles();
      const accounts = createMockAccounts();

      await page.route(/\/api\/v1\/admin\/roles(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', message: 'OK', data: roles })
        });
      });

      await page.route(/\/api\/v1\/admin\/accounts(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: { items: accounts, page: 0, size: 20, totalElements: accounts.length, totalPages: 1 }
          })
        });
      });

      let putCalled = false;
      await page.route(/\/api\/v1\/admin\/accounts\/2\/roles$/, async (route) => {
        putCalled = true;
        await route.fulfill({ status: 200 });
      });

      await page.goto(`${baseUrl}/admin-roles.html`);
      await page.waitForSelector('#accountsTableBody tr', { timeout: 5000 });

      // Open modal on account 2 (Learner)
      await page.locator('#btnEditRoles_2').click();
      await page.locator('#roleAssignmentModal').waitFor({ state: 'visible', timeout: 3000 });

      // Initially unchanged -> No-change notice is visible
      const noChangeNotice = page.locator('#roleModalNoChangeNotice');
      await noChangeNotice.waitFor({ state: 'visible' });

      // Click save without changing anything
      await page.locator('#btnSaveRoles').click();

      // Assert no PUT was triggered
      assert.strictEqual(putCalled, false, 'No PUT should be sent for unchanged role set');

      assert.deepStrictEqual(getRuntimeErrors(), []);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // AR-07: Role Guard blocks non-Admin
  // ---------------------------------------------------------------------------
  test('AR-07: Non-Admin session is blocked and sees role guard alert', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupLearnerSession(page);

      await page.goto(`${baseUrl}/admin-roles.html`);

      // Role guard banner is visible
      const guard = page.locator('#adminAuthGuardContainer');
      await guard.waitFor({ state: 'visible', timeout: 5000 });

      // Workspace is hidden
      const workspace = page.locator('#adminRolesWorkspaceSection');
      assert.strictEqual(await workspace.isVisible(), false);

      assert.deepStrictEqual(getRuntimeErrors(), []);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // AR-08: Verification Center Section 16 Diagnostic Probe
  // ---------------------------------------------------------------------------
  test('AR-08: Section 16 Admin Role Management diagnostic probe executes safely with zero role mutations', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      let putCalled = false;
      await page.route(/\/api\/v1\/admin\/accounts\/.*\/roles$/, async (route) => {
        putCalled = true;
        await route.abort();
      });

      await page.goto(`${baseUrl}/ui-verification.html`);
      await page.locator('#section-admin-roles').waitFor({ state: 'visible', timeout: 5000 });

      const btnProbe = page.locator('#btnProbeAdminRolesCapability');
      await btnProbe.waitFor({ state: 'visible' });
      await btnProbe.click();
      await page.waitForTimeout(300);

      const out = await page.locator('#adminRolesProbeOutput').textContent();
      assert.ok(out.includes('RoleResponse Parser Contract: PASS'), `Expected parser contract PASS, got: "${out}"`);
      assert.ok(out.includes('Canonical Roles & Metadata: PASS'), `Expected metadata PASS, got: "${out}"`);
      assert.ok(out.includes('Complete Role Set Payload: PASS'), `Expected payload PASS, got: "${out}"`);
      assert.ok(out.includes('Phát hiện No-Op') && out.includes('PASS'), `Expected No-Op PASS, got: "${out}"`);
      assert.ok(out.includes('Bảo vệ tự thân (POL-8D-02): PASS'), `Expected POL-8D-02 PASS, got: "${out}"`);
      assert.ok(out.includes('ZERO PUT MUTATION'), `Expected zero mutation confirmation, got: "${out}"`);
      assert.ok(out.includes('[PROBE PASS]'), `Expected PROBE PASS, got: "${out}"`);

      // STRICT CHECK: Zero PUT mutations
      assert.strictEqual(putCalled, false, 'Verification Center must NOT trigger any PUT role mutations');

      assert.deepStrictEqual(getRuntimeErrors(), []);
    } finally {
      await context.close();
    }
  });

});
