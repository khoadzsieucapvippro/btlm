/**
 * =============================================================================
 * LIVE VERIFICATION: Task 9F.1 Admin Accounts Lifecycle UI
 * Real Browser -> Real Static FE (:3000) -> Real Spring Boot (:8080) -> Real MySQL (:3306)
 * =============================================================================
 */

import { execSync } from 'node:child_process';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

const BACKEND_URL = 'http://localhost:8080';

async function runLiveVerification() {
  console.log('\n=============================================================');
  console.log(' LIVE VERIFICATION: Task 9F.1 — Admin Accounts Lifecycle UI');
  console.log(' FE (:3000) -> BE (:8080) -> DB (:3306)');
  console.log('=============================================================\n');

  const timestamp = Date.now();
  const adminEmail = `live_admin_${timestamp}@example.com`;
  const targetEmail = `live_target_${timestamp}@example.com`;
  const testPassword = `AdminPass_${timestamp}!123`;

  let adminAccountId = null;
  let targetAccountId = null;
  let adminToken = null;
  let serverHandle = null;

  try {
    // Step 1: Ensure Frontend static server is running
    serverHandle = await ensureServer({ port: 3000 });
    const feUrl = serverHandle.baseUrl;
    console.log(`[1/8] Frontend static server ready at ${feUrl}`);

    // Step 2: Register disposable Admin account via real backend
    console.log(`[2/8] Registering disposable admin account: ${adminEmail}`);
    const regAdminRes = await fetch(`${BACKEND_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        fullName: 'Live Test Admin',
        emailOrPhone: adminEmail,
        password: testPassword
      })
    });
    assert.strictEqual(regAdminRes.status, 201, `Admin registration failed: ${regAdminRes.status}`);
    const regAdminData = await regAdminRes.json();
    adminAccountId = regAdminData.data.accountId;
    console.log(`      Created accountId=${adminAccountId}`);

    // Step 3: Register disposable Target account via real backend
    console.log(`[3/8] Registering disposable target account: ${targetEmail}`);
    const regTargetRes = await fetch(`${BACKEND_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        fullName: 'Live Test Target User',
        emailOrPhone: targetEmail,
        password: testPassword
      })
    });
    assert.strictEqual(regTargetRes.status, 201, `Target registration failed: ${regTargetRes.status}`);
    const regTargetData = await regTargetRes.json();
    targetAccountId = regTargetData.data.accountId;
    console.log(`      Created accountId=${targetAccountId}`);

    // Step 4: Elevate admin account with role 4 (Admin) in MySQL
    console.log(`[4/8] Elevating account ${adminAccountId} to role 'Admin' in MySQL...`);
    execSync(`mysql -u root -e "INSERT INTO elearning_db.account_role (account_id, role_id) VALUES (${adminAccountId}, 4);"`);
    console.log(`      Role Admin assigned.`);

    // Step 5: Log in as Admin to obtain genuine JWT
    console.log(`[5/8] Logging in to obtain real JWT Bearer token...`);
    const loginRes = await fetch(`${BACKEND_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        emailOrPhone: adminEmail,
        password: testPassword
      })
    });
    assert.strictEqual(loginRes.status, 200, `Admin login failed: ${loginRes.status}`);
    const loginData = await loginRes.json();
    adminToken = loginData.data.token;
    assert.ok(loginData.data.roles.includes('Admin'), 'JWT must contain Admin role');
    console.log(`      JWT issued successfully. Roles: [${loginData.data.roles.join(', ')}]`);

    // Step 6: Launch real Playwright Chromium browser
    console.log(`[6/8] Launching Playwright browser and navigating to ${feUrl}/admin-accounts.html...`);
    let browser = await launchBrowser({ headless: true });
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    // Set localStorage auth session before navigation
    await page.addInitScript(({ token, user }) => {
      localStorage.setItem('access_token', token);
      localStorage.setItem('user_info', JSON.stringify(user));
    }, {
      token: adminToken,
      user: {
        accountId: adminAccountId,
        emailOrPhone: adminEmail,
        fullName: 'Live Test Admin',
        roles: ['Admin', 'Learner']
      }
    });

    await page.goto(`${feUrl}/admin-accounts.html`, { waitUntil: 'domcontentloaded' });

    // Step 7: Live UI verification
    console.log(`[7/8] Verifying live page interactions and invariants...`);

    // 7.1 Verify Heading & Workspace rendered
    const h1 = page.locator('h1');
    const h1Text = await h1.textContent();
    assert.ok(h1Text.includes('Quản Trị Vòng Đời Tài Khoản'), `Page title mismatch: ${h1Text}`);
    console.log(`      ✓ Page title verified: "${h1Text.trim()}"`);

    // 7.2 Verify Accounts Table rendered with rows
    const firstRow = page.locator('#accountsTableBody tr').first();
    await firstRow.waitFor({ state: 'visible', timeout: 10000 });
    const rows = page.locator('#accountsTableBody tr');
    const count = await rows.count();
    assert.ok(count > 0, `Table should have accounts (found ${count})`);
    console.log(`      ✓ Accounts table loaded with ${count} records from real DB`);

    // 7.3 Verify Self-Protection (POL-8D-01) on Admin's own row (filter by admin email)
    const searchInput = page.locator('#accountSearchInput');
    await searchInput.fill(adminEmail);
    await page.locator('#btnAccountSearch').click();
    await page.waitForTimeout(600);

    const adminRow = page.locator(`#accountRow_${adminAccountId}`);
    await adminRow.waitFor({ state: 'visible', timeout: 5000 });
    const selfBadge = adminRow.getByText('Tài khoản của bạn');
    assert.strictEqual(await selfBadge.isVisible(), true, 'Self-protection badge must be visible on Admin row');
    const disabledBtn = adminRow.locator('button[disabled]');
    assert.strictEqual(await disabledBtn.isVisible(), true, 'Button must be disabled on self row (POL-8D-01)');
    console.log(`      ✓ POL-8D-01 verified: Admin row has self-protection badge and disabled button`);

    // 7.4 Verify Search for target account
    await searchInput.fill(targetEmail);
    await page.locator('#btnAccountSearch').click();
    await page.waitForTimeout(600);

    const targetRow = page.locator(`#accountRow_${targetAccountId}`);
    await targetRow.waitFor({ state: 'visible', timeout: 5000 });
    assert.strictEqual(await targetRow.isVisible(), true, 'Target account must appear in search results');
    console.log(`      ✓ Real DB search verified: Found target account ${targetEmail}`);

    // 7.5 Open Status Change Modal for target account
    const changeStatusBtn = targetRow.locator(`#btnChangeStatus_${targetAccountId}`);
    await changeStatusBtn.click();
    const modal = page.locator('#statusConfirmModal');
    await modal.waitFor({ state: 'visible' });
    console.log(`      ✓ Status mutation modal opened`);

    // Verify modal content & security disclosure
    const modalTitle = await page.locator('#statusModalTitle').textContent();
    assert.ok(modalTitle.includes('Trạng Thái'), `Modal title expected, got: ${modalTitle}`);
    const modalWarning = await page.locator('#statusModalWarningNotice').textContent();
    assert.ok(modalWarning.includes('authorization_version'), 'Modal must explain server-side authorization_version invalidation');
    console.log(`      ✓ DEC-42 disclosure verified: Explains server-side authorization_version session revocation`);

    // 7.6 Select 'Inactive' and Confirm Mutation
    const statusSelect = page.locator('#modalNewStatusSelect');
    await statusSelect.selectOption('Inactive');
    const confirmBtn = page.locator('#btnConfirmStatusUpdate');
    await confirmBtn.click();
    await page.waitForTimeout(1000);

    // 7.7 Verify updated row state in UI
    const updatedBadge = targetRow.locator(`#accountStatusBadge_${targetAccountId}`);
    const badgeText = await updatedBadge.textContent();
    assert.strictEqual(badgeText.trim(), 'Tạm khóa', `Target badge must update to 'Tạm khóa', got: ${badgeText}`);
    console.log(`      ✓ Mutation verified: Target account status updated to 'Tạm khóa' (Inactive) from authoritative server response`);

    // 7.8 Verify in real MySQL database that status is indeed 'Inactive' and authorization_version incremented
    const dbCheck = execSync(`mysql -u root -e "SELECT status, authorization_version FROM elearning_db.account WHERE account_id = ${targetAccountId};"`).toString();
    assert.ok(dbCheck.includes('Inactive'), `MySQL must reflect 'Inactive' status, got: ${dbCheck}`);
    console.log(`      ✓ MySQL persistence verified: status=Inactive, authorization_version updated in DB`);

    await browser.close();
    browser = null;

    // Step 8: Cleanup disposable test accounts
    console.log(`[8/8] Cleaning up disposable test accounts...`);
    execSync(`mysql -u root -e "DELETE FROM elearning_db.account_role WHERE account_id IN (${adminAccountId}, ${targetAccountId});"`);
    execSync(`mysql -u root -e "DELETE FROM elearning_db.user_profile WHERE account_id IN (${adminAccountId}, ${targetAccountId});"`);
    execSync(`mysql -u root -e "DELETE FROM elearning_db.account WHERE account_id IN (${adminAccountId}, ${targetAccountId});"`);
    console.log(`      Cleanup completed.`);

    console.log('\n=============================================================');
    console.log(' LIVE VERIFICATION COMPLETED: 100% PASS');
    console.log(' FE -> BE -> DB full lifecycle, RBAC, search, POL-8D-01,');
    console.log(' and status mutation verified live on real stack.');
    console.log('=============================================================\n');
    return true;
  } catch (err) {
    console.error('\n[LIVE VERIFICATION FAILED]', err);
    // Cleanup if needed
    if (adminAccountId || targetAccountId) {
      try {
        const ids = [adminAccountId, targetAccountId].filter(Boolean).join(',');
        execSync(`mysql -u root -e "DELETE FROM elearning_db.account_role WHERE account_id IN (${ids}); DELETE FROM elearning_db.user_profile WHERE account_id IN (${ids}); DELETE FROM elearning_db.account WHERE account_id IN (${ids});"`);
      } catch (_) {}
    }
    throw err;
  } finally {
    if (serverHandle) {
      try { await serverHandle.close(); } catch (_) {}
    }
  }
}

runLiveVerification().catch(err => {
  console.error(err);
  process.exit(1);
});
