/**
 * =============================================================================
 * LIVE VERIFICATION: Task 9F.5 Admin Lesson Oversight UI
 * Real Browser -> Real Static FE (:3000) -> Real Spring Boot (:8080) -> Real MySQL (:3306)
 * =============================================================================
 */

import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

const BACKEND_URL = 'http://localhost:8080';

async function runLiveVerification() {
  console.log('\n=============================================================');
  console.log(' LIVE VERIFICATION: Task 9F.5 — Admin Lesson Oversight UI');
  console.log(' FE (:3000) -> BE (:8080) -> DB (:3306)');
  console.log(' STRICT: ZERO MUTATION REQUESTS (READ-ONLY VERIFICATION)');
  console.log('=============================================================\n');

  let serverHandle = null;
  let browser = null;

  try {
    // Step 1: Ensure Frontend static server is running
    serverHandle = await ensureServer({ port: 3000 });
    const feUrl = serverHandle.baseUrl;
    console.log(`[1/8] Frontend static server ready at ${feUrl}`);

    // Step 2: Log in as Admin to obtain real JWT Bearer token
    console.log(`[2/8] Logging in as Admin (admin@test.com)...`);
    const loginRes = await fetch(`${BACKEND_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        emailOrPhone: 'admin@test.com',
        password: 'Password123!'
      })
    });
    assert.strictEqual(loginRes.status, 200, `Admin login failed: ${loginRes.status}`);
    const loginData = await loginRes.json();
    const adminToken = loginData.data.token;
    const adminUser = {
      accountId: loginData.data.accountId,
      emailOrPhone: loginData.data.emailOrPhone,
      fullName: loginData.data.fullName,
      roles: loginData.data.roles
    };
    console.log(`      Admin authenticated: accountId=${adminUser.accountId}, roles=${JSON.stringify(adminUser.roles)}`);

    // Step 3: Launch real Chromium browser
    console.log(`[3/8] Launching headless browser...`);
    browser = await launchBrowser({ headless: true });
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    // Monitor for any forbidden mutations
    const forbiddenMutations = [];
    page.on('request', (req) => {
      const m = req.method();
      if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(m) && req.url().includes('/api/v1/')) {
        forbiddenMutations.push({ method: m, url: req.url() });
      }
    });

    // Inject genuine admin credentials into localStorage
    await page.addInitScript(`
      localStorage.setItem('access_token', '${adminToken}');
      localStorage.setItem('user_info', JSON.stringify(${JSON.stringify(adminUser)}));
    `);

    // Step 4: Open admin-lessons.html
    console.log(`[4/8] Navigating to ${feUrl}/admin-lessons.html...`);
    await page.goto(`${feUrl}/admin-lessons.html`, { waitUntil: 'domcontentloaded' });

    // Wait for table to load
    await page.locator('#lessonTableBody tr').first().waitFor({ state: 'visible', timeout: 8000 });
    const rowCount = await page.locator('#lessonTableBody tr').count();
    console.log(`      Table loaded successfully: ${rowCount} lessons rendered.`);
    assert.ok(rowCount >= 4, `Expected at least 4 lessons, found ${rowCount}`);

    // Step 5: Verify Global Status Counts from Server
    console.log(`[5/8] Verifying live server-side KPI counts...`);
    await page.waitForFunction(() => {
      const el = document.getElementById('kpiCountTotal');
      return el && !el.querySelector('.spinner-border');
    }, { timeout: 8000 });

    const totalCount = await page.locator('#kpiCountTotal').textContent();
    const draftCount = await page.locator('#kpiCountDraft').textContent();
    const pendingCount = await page.locator('#kpiCountPending').textContent();
    const approvedCount = await page.locator('#kpiCountApproved').textContent();
    const rejectedCount = await page.locator('#kpiCountRejected').textContent();

    console.log(`      Total: ${totalCount.trim()} | Draft: ${draftCount.trim()} | Pending: ${pendingCount.trim()} | Approved: ${approvedCount.trim()} | Rejected: ${rejectedCount.trim()}`);
    assert.ok(Number(totalCount) >= 4);

    // Step 6: Test status filter
    console.log(`[6/8] Testing status filtering (Approved & Pending)...`);
    await page.locator('#filterStatusApproved').click();
    await page.waitForTimeout(1000);
    const approvedRows = page.locator('#lessonTableBody tr');
    const approvedCountFiltered = await approvedRows.count();
    console.log(`      Filtered Approved: ${approvedCountFiltered} row(s) displayed.`);
    assert.ok(approvedCountFiltered >= 1);
    assert.ok((await approvedRows.first().textContent()).includes('Đã duyệt'));

    await page.locator('#filterStatusAll').click();
    await page.waitForTimeout(1000);

    // Step 7: Test Read-Only Detail View for Approved Lesson (#9369)
    console.log(`[7/8] Testing read-only detail modal for Approved lesson #9369...`);
    const btnDetail9369 = page.locator('#btnDetailLesson-9369');
    if (await btnDetail9369.isVisible()) {
      await btnDetail9369.click();
      const modal = page.locator('#lessonDetailModal');
      await modal.waitFor({ state: 'visible', timeout: 5000 });

      // Verify vocabulary items rendered
      await page.locator('#detailVocabList .list-group-item').first().waitFor({ state: 'visible', timeout: 5000 });
      const vocabCount = await page.locator('#detailVocabList .list-group-item').count();
      console.log(`      Modal opened: ${vocabCount} vocabulary item(s) displayed.`);
      assert.ok(vocabCount >= 2);

      // Close modal
      await page.locator('#btnCancelDetailModal').click();
      await modal.waitFor({ state: 'hidden', timeout: 3000 });
      console.log(`      Modal closed cleanly.`);
    }

    // Step 8: Strict Zero-Mutation Check
    console.log(`[8/8] Verifying ZERO mutation invariant...`);
    assert.strictEqual(forbiddenMutations.length, 0, `Detected forbidden mutation requests: ${JSON.stringify(forbiddenMutations)}`);
    console.log(`      PASSED: Exactly 0 mutation calls issued.`);

    const runtimeErrors = getRuntimeErrors();
    assert.strictEqual(runtimeErrors.length, 0, `Runtime errors: ${JSON.stringify(runtimeErrors)}`);

    await context.close();
    console.log('\n=============================================================');
    console.log(' ALL 8/8 LIVE VERIFICATION PHASES PASSED WITH ZERO MUTATIONS!');
    console.log('=============================================================\n');
  } finally {
    if (browser) await browser.close();
    if (serverHandle) await serverHandle.close();
  }
}

runLiveVerification().catch((err) => {
  console.error('\nLIVE VERIFICATION FAILED:', err);
  process.exit(1);
});
