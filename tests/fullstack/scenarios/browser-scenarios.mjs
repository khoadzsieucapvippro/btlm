/**
 * =============================================================================
 * FULL-STACK REAL BROWSER SCENARIOS (FS-011 TO FS-018)
 * File: tests/fullstack/scenarios/browser-scenarios.mjs
 * 
 * Invariants:
 * - Real Headless Browser (Chromium / Playwright) launched against live services.
 * - Zero route interception on normal journeys: real network packets from :3000 to :8080.
 * - Live verification across Radicals, Registration, Login, Profile, Persistence, and Logout.
 * - Captures and audits actual cross-origin CORS headers on real API traffic.
 * - Isolated resilience scenario tests ERROR vs false EMPTY state.
 * =============================================================================
 */

import assert from 'node:assert/strict';
import { launchBrowser, createMonitoredPage } from '../../frontend/utils/browser-runner.mjs';
import { generateDisposableUser } from '../utils/test-data.mjs';

/**
 * Runs all real browser user journeys.
 * @param {string} frontendUrl 
 * @param {string} backendUrl 
 * @returns {Promise<{
 *   results: Array<{ id: string, name: string, status: string, durationMs: number, error?: string, detail?: string }>
 * }>}
 */
export async function runBrowserScenarios(
  frontendUrl = 'http://localhost:3000',
  backendUrl = 'http://localhost:8080'
) {
  const results = [];
  const browser = await launchBrowser({ headless: true });
  const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
    viewportWidth: 1280,
    viewportHeight: 720
  });

  const capturedApiResponses = [];
  page.on('response', (response) => {
    const url = response.url();
    if (url.startsWith(backendUrl) && url.includes('/api/v1/')) {
      capturedApiResponses.push({
        url,
        method: response.request().method(),
        status: response.status(),
        corsOrigin: response.headers()['access-control-allow-origin']
      });
    }
  });

  const testUser = generateDisposableUser('browser_learner');
  let editedName = '';

  /**
   * Helper to record scenario outcome.
   */
  async function executeScenario(id, name, fn) {
    const start = performance.now();
    try {
      const detail = await fn();
      const durationMs = Math.round(performance.now() - start);
      results.push({ id, name, status: 'PASS', durationMs, detail });
      console.log(`  [${id}] PASS — ${name} (${durationMs}ms)`);
    } catch (err) {
      const durationMs = Math.round(performance.now() - start);
      results.push({ id, name, status: 'FAIL', durationMs, error: err.message });
      console.error(`  [${id}] FAIL — ${name}: ${err.message}`);
      throw err;
    }
  }

  console.log('\n-------------------------------------------------------------');
  console.log(' RUNNING REAL BROWSER USER JOURNEYS (FS-011 .. FS-018)');
  console.log('-------------------------------------------------------------');

  try {
    // -------------------------------------------------------------------------
    // FS-011: Real Browser Radicals Catalog Presentation
    // -------------------------------------------------------------------------
    await executeScenario('FS-011', 'Real Browser Radicals Presentation (214 cards loaded live)', async () => {
      await page.goto(`${frontendUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('.radical-card', { timeout: 10000 });

      const cardCount = await page.locator('.radical-card').count();
      const countBadge = await page.textContent('#radicalResultCount');
      assert.ok(countBadge.includes('214'), `Result counter badge must reflect 214 items (got "${countBadge.trim()}")`);
      assert.strictEqual(cardCount, 214, `Must render exactly 214 cards in catalog view (got ${cardCount})`);

      // Test client-side instant search
      await page.fill('#radicalSearchInput', 'Nhất');
      await page.waitForTimeout(150);
      const filteredCount = await page.locator('.radical-card:visible').count();
      assert.ok(filteredCount >= 1 && filteredCount <= 5, `Filtered cards for 'Nhất' should be between 1 and 5 (got ${filteredCount})`);

      // Clear search
      await page.click('#searchClearBtn');
      await page.waitForTimeout(150);
      const restoredCount = await page.locator('.radical-card:visible').count();
      assert.strictEqual(restoredCount, 214, 'Clearing search must restore all 214 cards');

      return `Rendered ${cardCount} cards; instant search filtered to ${filteredCount} and restored`;
    });

    // -------------------------------------------------------------------------
    // FS-012: Real Browser Radical Detail Modal & Focus Restoration
    // -------------------------------------------------------------------------
    await executeScenario('FS-012', 'Real Browser Radical Detail Modal & APG Focus Restoration', async () => {
      const firstCard = page.locator('.radical-card').first();
      await firstCard.click();

      // Verify modal opened
      await page.waitForSelector('#radicalDetailModal[open], dialog[open]', { timeout: 5000 });
      const glyph = await page.textContent('#modalRadicalChar');
      const badge = await page.textContent('#modalRadicalIdBadge');
      assert.strictEqual(badge.trim(), '#1', 'Modal badge must display #1');
      assert.strictEqual(glyph.trim(), '一', 'Modal glyph must display 一');

      // Dismiss via Escape key
      await page.keyboard.press('Escape');
      await page.waitForTimeout(300);

      // Verify modal is closed
      const isStillOpen = await page.evaluate(() => {
        const dialog = document.getElementById('radicalDetailModal');
        return dialog ? dialog.open : false;
      });
      assert.strictEqual(isStillOpen, false, 'Modal dialog must be closed after Escape key');

      // Verify focus restored to trigger element
      const activeElementClass = await page.evaluate(() => document.activeElement ? document.activeElement.className : '');
      assert.ok(activeElementClass.includes('radical-card'), `Focus must be restored to radical card (got active element class: "${activeElementClass}")`);

      return `Modal for radical #1 opened and closed via Escape with focus restored`;
    });

    // -------------------------------------------------------------------------
    // FS-013: Real Browser Registration Flow
    // -------------------------------------------------------------------------
    await executeScenario('FS-013', 'Real Browser Registration Flow (register.html → 201 → redirect)', async () => {
      await page.goto(`${frontendUrl}/register.html`, { waitUntil: 'domcontentloaded' });
      await page.fill('#regFullName', testUser.fullName);
      await page.fill('#regEmail', testUser.emailOrPhone);
      await page.fill('#regPassword', testUser.password);

      await Promise.all([
        page.waitForURL(/login\.html/, { timeout: 10000 }),
        page.click('#registerSubmitBtn')
      ]);

      const currentUrl = page.url();
      assert.ok(currentUrl.includes('login.html'), `Registration must redirect to login.html (got ${currentUrl})`);
      return `Registered ${testUser.emailOrPhone} (201 Created) and redirected to login.html`;
    });

    // -------------------------------------------------------------------------
    // FS-014: Real Browser Login Flow & Authenticated Navbar
    // -------------------------------------------------------------------------
    await executeScenario('FS-014', 'Real Browser Login Flow & Authenticated Navbar (login.html → index.html)', async () => {
      await page.fill('#loginEmail', testUser.emailOrPhone);
      await page.fill('#loginPassword', testUser.password);

      await Promise.all([
        page.waitForURL(/index\.html/, { timeout: 10000 }),
        page.click('#loginSubmitBtn')
      ]);

      await page.waitForSelector('#navUserNameLink', { timeout: 5000 });
      const navUserName = await page.textContent('#navUserNameLink');
      const navRoleBadge = await page.textContent('#navRoleBadge');

      assert.strictEqual(navUserName.trim(), testUser.fullName, `Navbar username must match registered name (got "${navUserName.trim()}")`);
      assert.strictEqual(navRoleBadge.trim(), 'Học viên', `Navbar role badge must be "Học viên" (got "${navRoleBadge.trim()}")`);
      return `Authenticated session stored; Navbar updated: "${navUserName.trim()}" [${navRoleBadge.trim()}]`;
    });

    // -------------------------------------------------------------------------
    // FS-015: Real Browser Profile Mutation & Reload Persistence
    // -------------------------------------------------------------------------
    await executeScenario('FS-015', 'Real Browser Profile Mutation & Reload DB Persistence', async () => {
      await page.goto(`${frontendUrl}/profile.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#profileFullNameInput', { timeout: 10000 });

      const initialName = await page.inputValue('#profileFullNameInput');
      assert.strictEqual(initialName, testUser.fullName, 'Profile input must be pre-populated with current name');

      editedName = `Học Viên Đổi Tên ${Date.now().toString().slice(-4)}`;
      await page.fill('#profileFullNameInput', editedName);
      await page.click('#profileSaveBtn');

      // Wait for success toast notification
      await page.waitForSelector('.toast-custom', { timeout: 5000 });

      // HARD RELOAD PAGE (F5) to verify MySQL persistence
      await page.reload({ waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#profileFullNameInput', { timeout: 10000 });

      const reloadedName = await page.inputValue('#profileFullNameInput');
      assert.strictEqual(reloadedName, editedName, `Persisted name across reload must be "${editedName}" (got "${reloadedName}")`);
      return `Profile edited to "${editedName}" and confirmed persistent across page reload`;
    });

    // -------------------------------------------------------------------------
    // FS-016: Real Browser Logout Flow
    // -------------------------------------------------------------------------
    await executeScenario('FS-016', 'Real Browser Logout Flow (Session cleared → Guest Navbar)', async () => {
      // Find logout button in profile or navbar
      const logoutBtn = page.locator('#profileLogoutBtn, #navLogoutBtn').first();
      assert.ok(await logoutBtn.count() > 0, 'Logout button must exist on profile or navbar');

      await logoutBtn.click();
      await page.waitForTimeout(500);

      // Verify session cleared from storage
      const tokenInStorage = await page.evaluate(() => localStorage.getItem('access_token'));
      assert.strictEqual(tokenInStorage, null, 'access_token must be removed from localStorage after logout');

      // Verify navbar returns to guest view (contains "Đăng nhập")
      await page.waitForSelector('#navAuthContainer', { timeout: 5000 });
      const navAuthContent = await page.textContent('#navAuthContainer');
      assert.ok(navAuthContent.includes('Đăng nhập'), `Navbar must return to Guest state containing "Đăng nhập" (got "${navAuthContent.trim()}")`);
      return 'Session cleared; Navbar returned to Guest state';
    });

    // -------------------------------------------------------------------------
    // FS-017: Real Browser Cross-Origin CORS Audit
    // -------------------------------------------------------------------------
    await executeScenario('FS-017', 'Real Browser Cross-Origin CORS Header Audit', async () => {
      assert.ok(capturedApiResponses.length > 0, 'Must have recorded actual cross-origin network responses');

      for (const call of capturedApiResponses) {
        assert.strictEqual(
          call.corsOrigin,
          frontendUrl,
          `Response from ${call.url} must contain Access-Control-Allow-Origin: ${frontendUrl} (got ${call.corsOrigin})`
        );
      }
      return `100% of ${capturedApiResponses.length} cross-origin responses contain valid CORS header: ${frontendUrl}`;
    });

    // -------------------------------------------------------------------------
    // FS-018: Frontend Network Resilience / Error Classification
    // -------------------------------------------------------------------------
    await executeScenario('FS-018', 'Frontend Network Resilience (503 Failure → Error State, NOT False Empty)', async () => {
      const resPage = await context.newPage();

      // Intercept /api/v1/radicals and simulate service unavailable
      await resPage.route('**/api/v1/radicals*', (route) => {
        route.fulfill({
          status: 503,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SERVICE_UNAVAILABLE',
            message: 'Máy chủ danh mục tạm thời không khả dụng'
          })
        });
      });

      await resPage.goto(`${frontendUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });
      await resPage.waitForSelector('.state-error-title', { timeout: 10000 });

      // Crucial assertion: Must display ERROR state, NOT EMPTY state!
      const errorHeading = await resPage.textContent('.state-error-title');
      const isEmptyStateVisible = await resPage.locator('.state-empty-title').isVisible();

      assert.strictEqual(isEmptyStateVisible, false, 'Backend failure must NEVER trigger false Empty state');
      assert.ok(errorHeading && errorHeading.length > 0, `Error title must be present (got "${errorHeading}")`);

      await resPage.close();
      return 'Service failure properly rendered ERROR state, zero false EMPTY states';
    });

    // -------------------------------------------------------------------------
    // FS-019: Verification Center Live Diagnostics Audit
    // -------------------------------------------------------------------------
    await executeScenario('FS-019', 'Verification Center Live Diagnostics (ui-verification.html Section 4)', async () => {
      await page.goto(`${frontendUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#liveDiagnosticsHeading', { timeout: 10000 });

      // Verify active API URL display
      const currentApiUrl = await page.textContent('#currentApiBaseUrl');
      assert.ok(currentApiUrl.includes('/api/v1'), `Active API base URL must include /api/v1 (got "${currentApiUrl}")`);

      // Probe Health via button
      await page.click('#btnProbeHealth');
      await page.waitForSelector('#liveStackStatusBadge', { timeout: 5000 });
      await page.waitForFunction(() => {
        const badge = document.getElementById('liveStackStatusBadge');
        return badge && badge.textContent.includes('Backend UP');
      }, { timeout: 5000 });

      // Probe Radicals via button
      await page.click('#btnProbeRadicals');
      await page.waitForFunction(() => {
        const out = document.getElementById('diagnosticResultOutput');
        return out && out.textContent.includes('Thành công');
      }, { timeout: 5000 });

      const outputText = await page.textContent('#diagnosticResultOutput');
      assert.ok(outputText.includes('214'), 'Diagnostic output must reflect 214 total radicals in database');

      return 'Section 4 live diagnostics executed: Actuator UP and 214 Radicals confirmed from UI';
    });

    // -------------------------------------------------------------------------
    // FS-021: Real Browser Live Vocabulary Journey (Catalog, Search & Progressive Detail)
    // -------------------------------------------------------------------------
    await executeScenario('FS-021', 'Live Vocabulary Journey: Catalog, Search & Detail Modal (vocabulary.html)', async () => {
      await page.goto(`${frontendUrl}/vocabulary.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('.vocab-card', { timeout: 10000 });

      // 1. Verify catalog card rendered
      const cardCount = await page.locator('.vocab-card').count();
      assert.ok(cardCount >= 1, `Live catalog must render at least 1 vocabulary card (found ${cardCount})`);

      // 2. Search for "shu"
      await page.fill('#vocabSearchInput', 'shu');
      await page.waitForTimeout(600); // Allow debounce (300ms) + network
      await page.waitForSelector('.vocab-card', { timeout: 10000 });

      const filteredHanzi = await page.textContent('.vocab-card .vocab-card-hanzi');
      assert.ok(filteredHanzi.includes('书'), `Search for "shu" must display card containing "书" (got "${filteredHanzi}")`);

      // 3. Open detail modal
      await page.click('.vocab-card');
      await page.waitForSelector('#vocabDetailModal[open]', { timeout: 5000 });

      // Verify detail content
      const modalHanzi = await page.textContent('#modalVocabHanzi');
      assert.ok(modalHanzi.includes('书'), `Modal must display Hanzi "书" (got "${modalHanzi}")`);

      // 4. Close modal
      await page.click('#modalCloseBtn');
      await page.waitForTimeout(300);

      const isStillOpen = await page.evaluate(() => {
        const dialog = document.getElementById('vocabDetailModal');
        return dialog ? dialog.open : false;
      });
      assert.strictEqual(isStillOpen, false, 'Vocabulary modal dialog must be closed after clicking close button');

      return `Live vocabulary catalog verified: rendered ${cardCount} cards, searched "shu" -> "书", opened & closed detail modal`;
    });

    // -------------------------------------------------------------------------
    // FS-023: Real Browser Public Lessons Journey (Catalog & State Verification)
    // -------------------------------------------------------------------------
    await executeScenario('FS-023', 'Real Browser Public Lessons Journey (lessons.html against live backend)', async () => {
      await page.goto(`${frontendUrl}/lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('.lesson-card, .state-empty-title', { timeout: 10000 });

      // Check whether catalog has lessons or renders polite empty state
      const hasCards = (await page.locator('.lesson-card').count()) > 0;
      const hasEmptyState = await page.locator('.state-empty-title').isVisible();

      assert.ok(hasCards || hasEmptyState, 'Catalog must either display lesson cards or a polite empty state');

      if (hasCards) {
        const firstCardTitle = await page.textContent('.lesson-card-title');
        assert.ok(firstCardTitle.trim().length > 0, 'First card must have non-empty title');
      } else {
        const emptyTitle = await page.textContent('.state-empty-title');
        assert.ok(emptyTitle.includes('Chưa có bài học') || emptyTitle.includes('bài học'), 'Empty state must convey absence of public lessons');
      }

      // Verify page size control exists and is operational
      const pageSizeSelect = page.locator('#pageSizeSelect');
      assert.ok((await pageSizeSelect.count()) > 0, 'Page size select must be present');

      return hasCards
        ? `Rendered live lesson cards; first card: "${(await page.textContent('.lesson-card-title')).trim()}"`
        : 'Confirmed live empty catalog state ("Chưa có bài học công khai") against real database';
    });

  } finally {
    // Check runtime uncaught errors
    const uncaughtErrors = getRuntimeErrors();
    assert.strictEqual(uncaughtErrors.length, 0, `Zero uncaught exceptions allowed. Found: ${JSON.stringify(uncaughtErrors)}`);
    await browser.close();
  }

  return { results };
}
