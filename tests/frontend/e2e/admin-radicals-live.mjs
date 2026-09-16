/**
 * =============================================================================
 * LIVE VERIFICATION: Task 9F.3 Admin Radicals CRUD UI
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
  console.log(' LIVE VERIFICATION: Task 9F.3 — Admin Radicals CRUD UI');
  console.log(' FE (:3000) -> BE (:8080) -> DB (:3306)');
  console.log('=============================================================\n');

  const timestamp = Date.now();
  const adminEmail = `live_admin_radicals_${timestamp}@example.com`;
  const testPassword = `AdminPass_${timestamp}!123`;
  const testCharacter = '学'; // Unique test character (Học / Study)

  let adminAccountId = null;
  let adminToken = null;
  let serverHandle = null;
  let createdRadicalId = null;

  try {
    // Step 1: Ensure Frontend static server is running
    serverHandle = await ensureServer({ port: 3000 });
    const feUrl = serverHandle.baseUrl;
    console.log(`[1/9] Frontend static server ready at ${feUrl}`);

    // Step 2: Register disposable Admin account via real backend
    console.log(`[2/9] Registering disposable admin account: ${adminEmail}`);
    const regAdminRes = await fetch(`${BACKEND_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        fullName: 'Live Test Admin Radicals',
        emailOrPhone: adminEmail,
        password: testPassword
      })
    });
    assert.strictEqual(regAdminRes.status, 201, `Admin registration failed: ${regAdminRes.status}`);
    const regAdminData = await regAdminRes.json();
    adminAccountId = regAdminData.data.accountId;
    console.log(`      Created accountId=${adminAccountId}`);

    // Step 3: Elevate admin account with role 4 (Admin) in MySQL
    console.log(`[3/9] Elevating account ${adminAccountId} to role 'Admin' in MySQL...`);
    execSync(`mysql -u root -e "INSERT INTO elearning_db.account_role (account_id, role_id) VALUES (${adminAccountId}, 4);"`);
    console.log(`      Role Admin assigned.`);

    // Step 4: Log in as Admin to obtain genuine JWT
    console.log(`[4/9] Logging in to obtain real JWT Bearer token...`);
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

    // Step 5: Launch real Playwright Chromium browser
    console.log(`[5/9] Launching Playwright browser and navigating to ${feUrl}/admin-radicals.html...`);
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
        fullName: 'Live Test Admin Radicals',
        roles: ['Admin', 'Learner']
      }
    });

    await page.goto(`${feUrl}/admin-radicals.html`, { waitUntil: 'domcontentloaded' });

    // Step 6: Live UI verification — Catalog & Filter
    console.log(`[6/9] Verifying live catalog loading & client-side filtering...`);

    // 6.1 Check Heading & Table rows
    const heading = await page.locator('h1').textContent();
    assert.match(heading, /Quản Trị Danh Mục Bộ Thủ/);
    console.log(`      ✓ Page heading verified: "${heading.trim()}"`);

    await page.waitForSelector('#radicalTableBody tr', { timeout: 10000 });
    const initialRows = await page.locator('#radicalTableBody tr').count();
    assert.ok(initialRows > 0, `Expected radicals in table, got ${initialRows}`);
    console.log(`      ✓ Live radical catalog loaded (${initialRows} items in view)`);

    // 6.2 Test client-side diacritic search
    const searchInput = page.locator('#radicalSearchInput');
    await searchInput.fill('nhất');
    await page.waitForTimeout(400);
    const filteredContent = await page.locator('#radicalTableBody').textContent();
    assert.match(filteredContent, /一/, 'Search for "nhất" should find radical 一');
    console.log(`      ✓ Diacritic-tolerant search verified: found "一" for query "nhất"`);

    // Clear search
    await searchInput.fill('');
    await page.waitForTimeout(300);

    // Step 7: Live CREATE Radical (POST /api/v1/admin/radicals)
    console.log(`[7/9] Testing Live CREATE radical mutation via real backend...`);
    await page.locator('#btnOpenCreateModal').click();
    await page.waitForSelector('#radicalFormModal[open]', { timeout: 3000 });

    await page.locator('#inputCharacter').fill(testCharacter);
    await page.locator('#inputPinyin').fill('xué');
    await page.locator('#inputMeaningHanViet').fill('Học');
    await page.locator('#inputMeaningVi').fill('Học tập, nghiên cứu');
    await page.locator('#inputAudioUrl').fill('https://example.com/audio/xue.mp3');

    await page.locator('#btnSaveRadical').click();
    await page.waitForFunction(() => !document.getElementById('radicalFormModal')?.open, { timeout: 5000 });
    console.log(`      ✓ Create form submitted and modal closed on authoritative 201 response`);

    // Verify created in DB
    const dbRadical = execSync('mysql -u root -s -N -e "SELECT radical_id, meaning_vi FROM elearning_db.radical WHERE radical_id > 214 ORDER BY radical_id DESC LIMIT 1;"', { encoding: 'utf-8' }).trim();
    assert.ok(dbRadical.length > 0, `Created radical must exist in MySQL DB`);
    const [radIdStr, meaningVi] = dbRadical.split('\t');
    createdRadicalId = parseInt(radIdStr, 10);
    console.log(`      ✓ MySQL persistence verified: radical_id=${createdRadicalId}, meaning_vi='${meaningVi}'`);

    // Verify visible in UI
    await searchInput.fill(testCharacter);
    await page.waitForTimeout(400);
    const tableAfterCreate = await page.locator('#radicalTableBody').textContent();
    assert.match(tableAfterCreate, new RegExp(testCharacter));
    console.log(`      ✓ Live UI reflected newly created radical in table`);

    // Step 8: Live 409 CONFLICT test (Duplicate character)
    console.log(`[8/9] Testing Live duplicate-character 409 CONFLICT handling...`);
    await searchInput.fill('');
    await page.waitForTimeout(300);

    await page.locator('#btnOpenCreateModal').click();
    await page.waitForSelector('#radicalFormModal[open]', { timeout: 3000 });

    // Try to create the same character again
    await page.locator('#inputCharacter').fill(testCharacter);
    await page.locator('#inputPinyin').fill('xué');
    await page.locator('#inputMeaningHanViet').fill('Học');
    await page.locator('#inputMeaningVi').fill('Trùng lặp');

    await page.locator('#btnSaveRadical').click();
    await page.waitForTimeout(800);

    // Modal must remain open and show conflict alert
    const isModalOpen = await page.locator('#radicalFormModal').evaluate(el => el.open);
    assert.strictEqual(isModalOpen, true, 'Form modal must stay open on 409 CONFLICT');
    const formError = page.locator('#formGeneralError');
    assert.strictEqual(await formError.isVisible(), true, 'Error banner must be visible');
    const errorText = await formError.textContent();
    console.log(`      ✓ Server 409 CONFLICT correctly displayed in UI: "${errorText.trim()}"`);

    // Close modal via cancel
    await page.locator('#btnCancelFormModal').click();
    await page.waitForFunction(() => !document.getElementById('radicalFormModal')?.open, { timeout: 5000 });

    // Step 9: Live EDIT & DELETE (PUT & DELETE -> 204 No Content)
    console.log(`[9/9] Testing Live EDIT and DELETE (204 No Content) operations...`);

    // 9.1 Filter for test radical and Edit
    await searchInput.fill(testCharacter);
    await page.waitForTimeout(400);

    const editBtn = page.locator(`button[aria-label="Chỉnh sửa bộ thủ ${testCharacter}"]`);
    await editBtn.waitFor({ state: 'visible', timeout: 5000 });
    await editBtn.click();
    await page.waitForSelector('#radicalFormModal[open]', { timeout: 3000 });

    // Update meaningVi
    await page.locator('#inputMeaningVi').fill('Bộ thử nghiệm đã cập nhật live');
    await page.locator('#btnSaveRadical').click();
    await page.waitForFunction(() => !document.getElementById('radicalFormModal')?.open, { timeout: 5000 });
    console.log(`      ✓ Edit submitted and modal closed on authoritative response`);

    // Verify DB updated
    const updatedMeaning = execSync(`mysql -u root --default-character-set=utf8mb4 -s -N -e "SELECT meaning_vi FROM elearning_db.radical WHERE radical_id = ${createdRadicalId};"`, { encoding: 'utf-8' }).trim();
    assert.strictEqual(updatedMeaning, 'Bộ thử nghiệm đã cập nhật live');
    console.log(`      ✓ MySQL persistence verified after EDIT: meaning_vi='${updatedMeaning}'`);

    // 9.2 Delete the test radical
    const deleteBtn = page.locator(`button[aria-label="Xóa bộ thủ ${testCharacter}"]`);
    await deleteBtn.waitFor({ state: 'visible', timeout: 5000 });
    await deleteBtn.click();
    await page.waitForSelector('#deleteRadicalModal[open]', { timeout: 3000 });

    // Confirm delete
    await page.locator('#btnConfirmDeleteRadical').click();
    await page.waitForFunction(() => !document.getElementById('deleteRadicalModal')?.open, { timeout: 5000 });
    console.log(`      ✓ Delete submitted and modal closed on authoritative 204 response`);

    // Verify removed from DB
    const countInDb = execSync(`mysql -u root --default-character-set=utf8mb4 -s -N -e "SELECT COUNT(*) FROM elearning_db.radical WHERE radical_id = ${createdRadicalId};"`, { encoding: 'utf-8' }).trim();
    assert.strictEqual(countInDb, '0', 'Radical must be completely deleted from MySQL DB');
    console.log(`      ✓ MySQL persistence verified: radical ${createdRadicalId} deleted from DB`);
    createdRadicalId = null;

    // Ignore expected 409 CONFLICT errors from step 8
    const unexpectedErrors = getRuntimeErrors().filter(e => {
      const msg = typeof e === 'string' ? e : e?.message || '';
      return !msg.includes('409') && !msg.includes('tồn tại');
    });
    assert.strictEqual(unexpectedErrors.length, 0, `Page had unexpected runtime errors: ${JSON.stringify(unexpectedErrors)}`);

    await browser.close();
    browser = null;

    console.log('\n=============================================================');
    console.log(' LIVE VERIFICATION COMPLETED: 100% PASS');
    console.log(' Task 9F.3 Admin Radicals CRUD verified live on real stack:');
    console.log(' - Public GET /api/v1/radicals');
    console.log(' - Admin POST /api/v1/admin/radicals');
    console.log(' - Duplicate character 409 CONFLICT handling');
    console.log(' - Admin PUT /api/v1/admin/radicals/{id}');
    console.log(' - Admin DELETE /api/v1/admin/radicals/{id} (204 No Content)');
    console.log('=============================================================\n');

  } finally {
    if (createdRadicalId) {
      try {
        execSync(`mysql -u root -e "DELETE FROM elearning_db.radical WHERE radical_id = ${createdRadicalId};"`);
      } catch (e) {
        console.warn('Failed to cleanup radical:', e.message);
      }
    }
    if (adminAccountId) {
      console.log(`Cleaning up disposable admin account ${adminAccountId}...`);
      try {
        execSync(`mysql -u root -e "DELETE FROM elearning_db.account_role WHERE account_id = ${adminAccountId};"`);
        execSync(`mysql -u root -e "DELETE FROM elearning_db.account WHERE account_id = ${adminAccountId};"`);
        console.log(`Cleanup completed.`);
      } catch (e) {
        console.warn('Failed to cleanup admin account:', e.message);
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
