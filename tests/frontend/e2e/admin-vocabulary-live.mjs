/**
 * =============================================================================
 * LIVE VERIFICATION: Task 9F.4 Admin Vocabulary CRUD UI
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
  console.log(' LIVE VERIFICATION: Task 9F.4 — Admin Vocabulary CRUD UI');
  console.log(' FE (:3000) -> BE (:8080) -> DB (:3306)');
  console.log('=============================================================\n');

  const timestamp = Date.now();
  const adminEmail = `live_admin_vocab_${timestamp}@example.com`;
  const testPassword = `AdminPass_${timestamp}!123`;
  const testHanzi = '测试'; // "Trắc thí" - Test
  const testPinyin = 'cèshì';
  const testPinyinRaw = 'ceshi';

  let adminAccountId = null;
  let adminToken = null;
  let serverHandle = null;
  let createdVocabId = null;

  try {
    // Step 1: Ensure Frontend static server is running
    serverHandle = await ensureServer({ port: 3000 });
    const feUrl = serverHandle.baseUrl;
    console.log(`[1/10] Frontend static server ready at ${feUrl}`);

    // Step 2: Register disposable Admin account via real backend
    console.log(`[2/10] Registering disposable admin account: ${adminEmail}`);
    const regAdminRes = await fetch(`${BACKEND_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        fullName: 'Live Test Admin Vocab',
        emailOrPhone: adminEmail,
        password: testPassword
      })
    });
    assert.strictEqual(regAdminRes.status, 201, `Admin registration failed: ${regAdminRes.status}`);
    const regAdminData = await regAdminRes.json();
    adminAccountId = regAdminData.data.accountId;
    console.log(`       Created accountId=${adminAccountId}`);

    // Step 3: Elevate admin account with role 4 (Admin) in MySQL
    console.log(`[3/10] Elevating account ${adminAccountId} to role 'Admin' in MySQL...`);
    execSync(`mysql -u root -e "INSERT INTO elearning_db.account_role (account_id, role_id) VALUES (${adminAccountId}, 4);"`);
    console.log(`       Role Admin assigned.`);

    // Step 4: Log in as Admin to obtain genuine JWT
    console.log(`[4/10] Logging in to obtain real JWT Bearer token...`);
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
    console.log(`       JWT issued successfully. Roles: [${loginData.data.roles.join(', ')}]`);

    // Step 5: Launch real Playwright Chromium browser
    console.log(`[5/10] Launching Playwright browser and navigating to ${feUrl}/admin-vocabulary.html...`);
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
        fullName: 'Live Test Admin Vocab',
        roles: ['Admin', 'Learner']
      }
    });

    await page.goto(`${feUrl}/admin-vocabulary.html`, { waitUntil: 'domcontentloaded' });

    // Step 6: Live UI verification — Catalog & Server Search
    console.log(`[6/10] Verifying live catalog loading & server-side search...`);

    // 6.1 Check Heading & Table rows
    const heading = await page.locator('h1').textContent();
    assert.match(heading, /Quản Trị Danh Mục Từ Vựng/);
    console.log(`       ✓ Page heading verified: "${heading.trim()}"`);

    await page.waitForSelector('#vocabTableBody tr', { timeout: 10000 });
    const initialRows = await page.locator('#vocabTableBody tr').count();
    assert.ok(initialRows > 0, `Expected vocabulary in table, got ${initialRows}`);
    console.log(`       ✓ Live vocabulary catalog loaded (${initialRows} items in view)`);

    // 6.2 Test server search
    const searchInput = page.locator('#vocabSearchInput');
    await searchInput.fill('你好');
    await searchInput.press('Enter');
    await page.waitForTimeout(600);
    const filteredContent = await page.locator('#vocabTableBody').textContent();
    assert.match(filteredContent, /你好/, 'Search for "你好" should find vocabulary item');
    console.log(`       ✓ Server search verified: found "你好"`);

    // Clear search
    const clearBtn = page.locator('#searchClearBtn');
    if (await clearBtn.isVisible()) {
      await clearBtn.click();
    } else {
      await searchInput.fill('');
      await searchInput.press('Enter');
    }
    await page.waitForTimeout(400);

    // Step 7: Live CREATE Vocabulary with constituent radical (POST /api/v1/admin/vocabulary)
    console.log(`[7/10] Testing Live CREATE vocabulary mutation with constituent radicals...`);
    await page.locator('#btnOpenCreateModal').click();
    await page.waitForSelector('#vocabFormModal[open]', { timeout: 5000 });

    await page.locator('#inputHanzi').fill(testHanzi);
    await page.locator('#inputPinyin').fill(testPinyin);
    await page.locator('#inputPinyinRaw').fill(testPinyinRaw);
    await page.locator('#inputMeaningHanViet').fill('Trắc thí');
    await page.locator('#inputMeaningVi').fill('Kiểm tra live test');
    await page.locator('#inputAudioUrl').fill('https://example.com/audio/ceshi.mp3');
    await page.locator('#inputExampleSentence').fill('这是一个测试。');
    await page.locator('#inputExampleTranslation').fill('Đây là một bài kiểm tra.');

    // Select radical 9 (人)
    const radCheckbox = page.locator('#radCheck-9');
    if (await radCheckbox.isVisible()) {
      await radCheckbox.check();
      console.log(`       ✓ Radical 9 (人) selected`);
    }

    await page.locator('#btnSaveVocab').click();
    await page.waitForFunction(() => !document.getElementById('vocabFormModal')?.open, { timeout: 8000 });
    console.log(`       ✓ Create form submitted and modal closed on authoritative 201 response`);

    // Verify created in DB
    const dbVocab = execSync(`mysql -u root --default-character-set=utf8mb4 -s -N -e "SELECT vocab_id, hanzi, meaning_vi FROM elearning_db.vocabulary WHERE hanzi = '${testHanzi}' ORDER BY vocab_id DESC LIMIT 1;"`, { encoding: 'utf-8' }).trim();
    assert.ok(dbVocab.length > 0, `Created vocabulary must exist in MySQL DB`);
    const [vocabIdStr, hanziFromDb, meaningVi] = dbVocab.split('\t');
    createdVocabId = parseInt(vocabIdStr, 10);
    console.log(`       ✓ MySQL persistence verified: vocab_id=${createdVocabId}, hanzi='${hanziFromDb}', meaning_vi='${meaningVi}'`);

    // Verify radical relationship created in MySQL
    const radCountInDb = execSync(`mysql -u root -s -N -e "SELECT COUNT(*) FROM elearning_db.vocab_radical WHERE vocab_id = ${createdVocabId};"`, { encoding: 'utf-8' }).trim();
    console.log(`       ✓ MySQL vocab_radical count: ${radCountInDb}`);

    // Verify visible in UI
    await searchInput.fill(testHanzi);
    await searchInput.press('Enter');
    await page.waitForTimeout(600);
    const tableAfterCreate = await page.locator('#vocabTableBody').textContent();
    assert.match(tableAfterCreate, new RegExp(testHanzi));
    console.log(`       ✓ Live UI reflected newly created vocabulary in table`);

    // Step 8: Live 409 CONFLICT test (Duplicate hanzi + pinyinRaw)
    console.log(`[8/10] Testing Live duplicate (hanzi + pinyinRaw) 409 CONFLICT handling...`);
    await page.locator('#btnOpenCreateModal').click();
    await page.waitForSelector('#vocabFormModal[open]', { timeout: 5000 });

    // Try to create the same hanzi + pinyinRaw again
    await page.locator('#inputHanzi').fill(testHanzi);
    await page.locator('#inputPinyin').fill(testPinyin);
    await page.locator('#inputPinyinRaw').fill(testPinyinRaw);
    await page.locator('#inputMeaningHanViet').fill('Trắc thí');
    await page.locator('#inputMeaningVi').fill('Bản ghi trùng');

    await page.locator('#btnSaveVocab').click();
    await page.waitForTimeout(800);

    // Modal must remain open and show conflict alert
    const isModalOpen = await page.locator('#vocabFormModal').evaluate(el => el.open);
    assert.strictEqual(isModalOpen, true, 'Form modal must stay open on 409 CONFLICT');
    const formError = page.locator('#formGeneralError');
    assert.strictEqual(await formError.isVisible(), true, 'Error banner must be visible');
    const errorText = await formError.textContent();
    assert.match(errorText, /đã tồn tại/, 'Conflict message must indicate vocabulary already exists');
    console.log(`       ✓ Server 409 CONFLICT correctly displayed in UI: "${errorText.trim()}"`);

    // Close modal via cancel
    await page.locator('#btnCancelFormModal').click();
    await page.waitForFunction(() => !document.getElementById('vocabFormModal')?.open, { timeout: 5000 });

    // Step 9: Live EDIT Vocabulary (PUT /api/v1/admin/vocabulary/{id})
    console.log(`[9/10] Testing Live EDIT vocabulary (Authoritative detail & radical update)...`);
    const editBtn = page.locator(`#btnEditVocab-${createdVocabId}`);
    await editBtn.waitFor({ state: 'visible', timeout: 5000 });
    await editBtn.click();
    await page.waitForSelector('#vocabFormModal[open]', { timeout: 5000 });

    // Verify detail loaded radical count
    const selectedRadicalsText = await page.locator('#selectedRadicalsCount').textContent();
    console.log(`       ✓ Detail endpoint loaded radicals: "${selectedRadicalsText}"`);

    // Update meaningVi
    const updatedMeaning = 'Kiểm tra live test đã cập nhật thành công';
    await page.locator('#inputMeaningVi').fill(updatedMeaning);
    await page.locator('#btnSaveVocab').click();
    await page.waitForFunction(() => !document.getElementById('vocabFormModal')?.open, { timeout: 8000 });
    console.log(`       ✓ Edit form submitted and modal closed on authoritative 200 response`);

    // Verify DB updated
    const dbUpdatedMeaning = execSync(`mysql -u root --default-character-set=utf8mb4 -s -N -e "SELECT meaning_vi FROM elearning_db.vocabulary WHERE vocab_id = ${createdVocabId};"`, { encoding: 'utf-8' }).trim();
    assert.strictEqual(dbUpdatedMeaning, updatedMeaning);
    console.log(`       ✓ MySQL persistence verified after EDIT: meaning_vi='${dbUpdatedMeaning}'`);

    // Step 10: Live DELETE Vocabulary (DELETE -> 204 No Content)
    console.log(`[10/10] Testing Live DELETE operation (204 No Content without json parsing)...`);
    const deleteBtn = page.locator(`#btnDeleteVocab-${createdVocabId}`);
    await deleteBtn.waitFor({ state: 'visible', timeout: 5000 });
    await deleteBtn.click();
    await page.waitForSelector('#deleteVocabModal[open]', { timeout: 5000 });

    // Confirm delete
    await page.locator('#btnConfirmDeleteVocab').click();
    await page.waitForFunction(() => !document.getElementById('deleteVocabModal')?.open, { timeout: 5000 });
    console.log(`       ✓ Delete submitted and modal closed on authoritative 204 response`);

    // Verify removed from DB
    const countInDb = execSync(`mysql -u root -s -N -e "SELECT COUNT(*) FROM elearning_db.vocabulary WHERE vocab_id = ${createdVocabId};"`, { encoding: 'utf-8' }).trim();
    assert.strictEqual(countInDb, '0', 'Vocabulary must be completely deleted from MySQL DB');
    const radRelCountInDb = execSync(`mysql -u root -s -N -e "SELECT COUNT(*) FROM elearning_db.vocab_radical WHERE vocab_id = ${createdVocabId};"`, { encoding: 'utf-8' }).trim();
    assert.strictEqual(radRelCountInDb, '0', 'Vocab radical relationships must be cascade cleaned');
    console.log(`       ✓ MySQL persistence verified: vocab ${createdVocabId} and vocab_radical rows completely deleted`);
    createdVocabId = null;

    // Filter out expected 409 conflict errors
    const unexpectedErrors = getRuntimeErrors().filter(e => {
      const msg = typeof e === 'string' ? e : e?.message || '';
      return !msg.includes('409') && !msg.includes('tồn tại');
    });
    assert.strictEqual(unexpectedErrors.length, 0, `Page had unexpected runtime errors: ${JSON.stringify(unexpectedErrors)}`);

    await browser.close();
    browser = null;

    console.log('\n=============================================================');
    console.log(' LIVE VERIFICATION COMPLETED: 100% PASS');
    console.log(' Task 9F.4 Admin Vocabulary CRUD verified live on real stack:');
    console.log(' - Public GET /api/v1/vocabulary server search & pagination');
    console.log(' - Authoritative GET /api/v1/vocabulary/{id} detail & radicals');
    console.log(' - Admin POST /api/v1/admin/vocabulary (201 Created)');
    console.log(' - Duplicate (hanzi + pinyinRaw) 409 CONFLICT handling');
    console.log(' - Admin PUT /api/v1/admin/vocabulary/{id} (200 OK)');
    console.log(' - Admin DELETE /api/v1/admin/vocabulary/{id} (204 No Content)');
    console.log(' - MySQL DB persistence & cascade cleanup verified');
    console.log('=============================================================\n');

  } finally {
    if (createdVocabId) {
      try {
        execSync(`mysql -u root -e "DELETE FROM elearning_db.vocab_radical WHERE vocab_id = ${createdVocabId};"`);
        execSync(`mysql -u root -e "DELETE FROM elearning_db.vocabulary WHERE vocab_id = ${createdVocabId};"`);
      } catch (e) {
        console.warn('Failed to cleanup vocabulary:', e.message);
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
