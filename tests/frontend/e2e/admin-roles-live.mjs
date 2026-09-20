/**
 * =============================================================================
 * LIVE VERIFICATION: Task 9F.2 Admin Role Management & Role Assignment UI
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
  console.log(' LIVE VERIFICATION: Task 9F.2 — Admin Role Management UI');
  console.log(' FE (:3000) -> BE (:8080) -> DB (:3306)');
  console.log('=============================================================\n');

  const timestamp = Date.now();
  const adminEmail = `live_admin_roles_${timestamp}@example.com`;
  const targetEmail = `live_target_roles_${timestamp}@example.com`;
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
        fullName: 'Live Test Admin Roles',
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
        fullName: 'Live Test Target Roles User',
        emailOrPhone: targetEmail,
        password: testPassword
      })
    });
    assert.strictEqual(regTargetRes.status, 201, `Target registration failed: ${regTargetRes.status}`);
    const regTargetData = await regTargetRes.json();
    targetAccountId = regTargetData.data.accountId;
    console.log(`      Created targetAccountId=${targetAccountId}`);

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
    console.log(`[6/8] Launching Playwright browser and navigating to ${feUrl}/admin-roles.html...`);
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
        fullName: 'Live Test Admin Roles',
        roles: ['Admin', 'Learner']
      }
    });

    await page.goto(`${feUrl}/admin-roles.html`, { waitUntil: 'domcontentloaded' });

    // Step 7: Live UI verification
    console.log(`[7/8] Verifying live page interactions and invariants...`);

    // 7.1 Verify Heading & Workspace rendered
    const h1 = page.locator('h1');
    const h1Text = await h1.textContent();
    assert.ok(h1Text.includes('Quản Trị Phân Quyền Vai Trò'), `Page title mismatch: ${h1Text}`);
    console.log(`      ✓ Page title verified: "${h1Text.trim()}"`);

    // 7.2 Verify authoritative roles catalog loaded from GET /api/v1/admin/roles
    await page.waitForSelector('#roleCatalogContainer strong', { timeout: 10000 });
    const roleItems = page.locator('#roleCatalogContainer strong');
    const roleCount = await roleItems.count();
    assert.ok(roleCount >= 4, `Expected at least 4 authoritative roles, got ${roleCount}`);
    console.log(`      ✓ Authoritative role catalog loaded with ${roleCount} roles (Admin, Creator, Learner, Moderator)`);

    // 7.3 Search and open role assignment modal for target account
    const searchInput = page.locator('#accountSearchInput');
    await searchInput.fill(targetEmail);
    await page.locator('#btnAccountSearch').click();
    await page.waitForTimeout(600);

    const editBtn = page.locator(`#btnEditRoles_${targetAccountId}`);
    await editBtn.waitFor({ state: 'visible', timeout: 5000 });
    await editBtn.click();

    // 7.4 Verify modal opened with target account identity
    const modal = page.locator('#roleAssignmentModal');
    await modal.waitFor({ state: 'visible', timeout: 3000 });
    const targetEmailLabel = await page.locator('#modalTargetEmail').textContent();
    assert.ok(targetEmailLabel.includes(targetEmail), `Target email mismatch: ${targetEmailLabel}`);
    console.log(`      ✓ Target account loaded into role assignment modal: ${targetEmailLabel.trim()}`);

    // 7.5 Add 'Creator' role to target account and submit
    const creatorCheckbox = page.locator('#roleCheckbox_Creator');
    await creatorCheckbox.check();

    const saveBtn = page.locator('#btnSaveRoles');
    assert.strictEqual(await saveBtn.isEnabled(), true, 'Save button must be enabled after role change');
    await saveBtn.click();

    // Wait for modal to close after server response
    await page.waitForFunction(() => !document.getElementById('roleAssignmentModal')?.open, { timeout: 5000 });
    console.log(`      ✓ Role mutation submitted and modal closed on authoritative response`);

    // 7.6 Verify server persistence in DB
    const dbRoles = execSync(`mysql -u root -s -N -e "SELECT r.role_name FROM elearning_db.account_role ar JOIN elearning_db.role r ON ar.role_id = r.role_id WHERE ar.account_id = ${targetAccountId} ORDER BY r.role_name;"`).toString().trim().split('\n').map(s => s.trim());
    assert.ok(dbRoles.includes('Creator'), `Expected Creator in DB roles, got: ${dbRoles.join(', ')}`);
    assert.ok(dbRoles.includes('Learner'), `Expected Learner in DB roles, got: ${dbRoles.join(', ')}`);
    console.log(`      ✓ MySQL persistence verified: Target account now has roles: [${dbRoles.join(', ')}]`);

    // 7.7 Verify Self-Protection POL-8D-02: Admin's own account has Admin checkbox disabled
    await searchInput.fill(adminEmail);
    await page.locator('#btnAccountSearch').click();
    await page.waitForTimeout(600);

    const editAdminBtn = page.locator(`#btnEditRoles_${adminAccountId}`);
    await editAdminBtn.waitFor({ state: 'visible', timeout: 5000 });
    await editAdminBtn.click();

    await modal.waitFor({ state: 'visible', timeout: 3000 });
    const adminCheckboxForSelf = page.locator('#roleCheckbox_Admin');
    assert.strictEqual(await adminCheckboxForSelf.isDisabled(), true, 'POL-8D-02: Admin role must be disabled for self');
    console.log(`      ✓ POL-8D-02 verified: Admin cannot remove own Admin role`);

    // Close modal via cancel button
    await page.locator('#btnCancelRoleModal').click();
    await page.waitForFunction(() => !document.getElementById('roleAssignmentModal')?.open, { timeout: 5000 });

    const errors = getRuntimeErrors();
    assert.strictEqual(errors.length, 0, `Page had runtime errors: ${JSON.stringify(errors)}`);

    await browser.close();
    browser = null;

    console.log('\n=============================================================');
    console.log(' LIVE VERIFICATION COMPLETED: 100% PASS');
    console.log(' Task 9F.2 Admin Role Management verified live on real stack.');
    console.log('=============================================================\n');

  } finally {
    if (adminAccountId || targetAccountId) {
      console.log(`[8/8] Cleaning up disposable test accounts...`);
      try {
        const ids = [adminAccountId, targetAccountId].filter(Boolean).join(', ');
        execSync(`mysql -u root -e "DELETE FROM elearning_db.account_role WHERE account_id IN (${ids});"`);
        execSync(`mysql -u root -e "DELETE FROM elearning_db.account WHERE account_id IN (${ids});"`);
        console.log(`      Cleanup completed.`);
      } catch (cleanupErr) {
        console.warn('Cleanup warning:', cleanupErr.message);
      }
    }
    if (serverHandle) {
      await serverHandle.close();
    }
  }
}

runLiveVerification().catch((err) => {
  console.error('\nLIVE VERIFICATION FAILED:', err);
  process.exit(1);
});
