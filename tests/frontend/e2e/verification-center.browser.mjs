/**
 * =============================================================================
 * DEDICATED E2E BROWSER TEST: PERMANENT VERIFICATION CENTER (TASK FE-REMEDIATION-05)
 * Module: tests/frontend/e2e/verification-center.browser.mjs
 * 
 * Scope:
 * - Dedicated test suite for frontend/ui-verification.html.
 * - Validates:
 *   1. Section structure (9A.1, 9A.2, 9B.1, 9B.2, Full-Stack)
 *   2. Genuine /actuator/health probe (correct endpoint, non-/api/v1 routing)
 *   3. Mocked error diagnostics (401, 403, 429, 204, Timeout, Retry, No-Retry)
 *   4. Query serialization tool (buildUrl)
 *   5. Session manager lifecycle & zero secret leakage
 *   6. Auth events monitor real-time tracking
 *   7. Client-side search engine testing (filterRadicals)
 *   8. WCAG 2.2 AA target size and accessibility basics
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

describe('Verification Center E2E: Dedicated Diagnostic Surface Suite', () => {
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

  // VC-01: Page Loads & Section Structure
  test('VC-01: Verification Center loads with all 5 functional sections and quick navigation bar', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('header.site-header').waitFor({ state: 'visible' });

      // Check primary H1
      const h1Text = await page.locator('h1#verificationHeading').textContent();
      assert.ok(h1Text.includes('Verification Center'), `Expected Verification Center in H1, got "${h1Text}"`);

      // Check quick navigation links
      const navLinks = page.locator('nav[aria-label="Điều hướng nhanh các phân khu kiểm chứng"] a');
      assert.strictEqual(await navLinks.count(), 12, 'Must contain 12 section jump links');

      // Check presence of all section headings
      assert.ok(await page.locator('#heading-9a1').isVisible(), 'Section 9A.1 heading must be visible');
      assert.ok(await page.locator('#heading-9a2').isVisible(), 'Section 9A.2 heading must be visible');
      assert.ok(await page.locator('#heading-9b1').isVisible(), 'Section 9B.1 heading must be visible');
      assert.ok(await page.locator('#heading-9b2').isVisible(), 'Section 9B.2 heading must be visible');
      assert.ok(await page.locator('#heading-9b3').isVisible(), 'Section 9B.3 heading must be visible');
      assert.ok(await page.locator('#heading-9c1').isVisible(), 'Section 9C.1 heading must be visible');
      assert.ok(await page.locator('#heading-9c2').isVisible(), 'Section 9C.2 heading must be visible');
      assert.ok(await page.locator('#heading-9c3').isVisible(), 'Section 9C.3 heading must be visible');
      assert.ok(await page.locator('#creatorVerificationHeading').isVisible(), 'Section 9D.1 heading must be visible');
      assert.ok(await page.locator('#creatorImportVerificationHeading').isVisible(), 'Section 9D.2 heading must be visible');
      assert.ok(await page.locator('#creatorSubmitVerificationHeading').isVisible(), 'Section 9D.3 heading must be visible');
      assert.ok(await page.locator('#liveDiagnosticsHeading').isVisible(), 'Section 12 Full-Stack heading must be visible');

      // Check badge legend
      assert.ok(await page.locator('.badge-mode-live').count() > 0, 'LIVE badge must be present');
      assert.ok(await page.locator('.badge-mode-mocked').count() > 0, 'MOCKED badge must be present');
      assert.ok(await page.locator('.badge-mode-unit').count() > 0, 'UNIT badge must be present');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors on page load:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-02: Health Diagnostic Probes Genuine /actuator/health
  test('VC-02: btnProbeHealth genuinely targets /actuator/health (NOT /api/v1/radicals)', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });

      let probedUrl = null;
      page.on('request', req => {
        if (req.url().includes('actuator') || req.url().includes('health')) {
          probedUrl = req.url();
        }
      });

      const btnHealth = page.locator('#btnProbeHealth');
      await btnHealth.waitFor({ state: 'visible' });
      await btnHealth.click();

      // Wait for diagnostic output update
      await page.waitForFunction(() => {
        const out = document.getElementById('apiSuccessOutput');
        return out && !out.textContent.includes('Nhấn một trong các nút');
      }, { timeout: 7000 });

      // Assert network request genuinely targeted /actuator/health
      assert.ok(probedUrl, 'Must send an HTTP request to an actuator/health endpoint');
      assert.ok(probedUrl.endsWith('/actuator/health'), `Probed URL must end with /actuator/health, got: ${probedUrl}`);
      assert.ok(!probedUrl.includes('/api/v1/actuator'), `Must NOT route actuator through /api/v1, got: ${probedUrl}`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during health probe:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-03: Mocked Diagnostics Identification & Execution
  test('VC-03: Mocked error diagnostics (401, 403, 429, 204, Timeout, Retry, No-Retry) execute cleanly', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });
      const consoleOut = page.locator('#mockedDiagnosticsOutput');

      // 1. Mock 401
      await page.locator('#btnMock401').click();
      let text = await consoleOut.textContent();
      assert.ok(text.includes('401'), `Expected 401 in console, got "${text}"`);
      assert.ok(text.includes('auth:expired'), 'Expected auth:expired event dispatch');

      // 2. Mock 403
      await page.locator('#btnMock403').click();
      text = await consoleOut.textContent();
      assert.ok(text.includes('403'), `Expected 403 in console, got "${text}"`);
      assert.ok(text.includes('bảo toàn'), 'Expected session preservation note');

      // 3. Mock 429
      await page.locator('#btnMock429').click();
      text = await consoleOut.textContent();
      assert.ok(text.includes('429'), `Expected 429 in console, got "${text}"`);
      assert.ok(text.includes('TOO_MANY_REQUESTS'), 'Expected TOO_MANY_REQUESTS code');

      // 4. Mock 204
      await page.locator('#btnMock204').click();
      text = await consoleOut.textContent();
      assert.ok(text.includes('204'), `Expected 204 in console, got "${text}"`);

      // 5. Mock Timeout
      await page.locator('#btnMockTimeout').click();
      text = await consoleOut.textContent();
      assert.ok(text.includes('408') || text.includes('TIMEOUT'), `Expected Timeout in console, got "${text}"`);

      // 6. Mock GET Retry
      await page.locator('#btnMockGetRetry').click();
      text = await consoleOut.textContent();
      assert.ok(text.includes('GET RETRY'), `Expected GET RETRY in console, got "${text}"`);

      // 7. Mock Mutation No-Retry
      await page.locator('#btnMockMutationNoRetry').click();
      text = await consoleOut.textContent();
      assert.ok(text.includes('NO-RETRY'), `Expected NO-RETRY in console, got "${text}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during mocked tests:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-04: Query Parameter Serialization Tool
  test('VC-04: Query serialization tool generates valid URL with buildUrl() without network requests', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });

      await page.fill('#queryTestPath', '/lessons');
      await page.fill('#queryTestPage', '1');
      await page.fill('#queryTestSize', '10');
      await page.click('#btnTestBuildUrl');

      const urlOutput = await page.locator('#serializedUrlOutput').textContent();
      assert.ok(urlOutput.includes('/lessons'), `Expected /lessons in output, got "${urlOutput}"`);
      assert.ok(urlOutput.includes('page=1'), `Expected page=1 in output, got "${urlOutput}"`);
      assert.ok(urlOutput.includes('size=10'), `Expected size=10 in output, got "${urlOutput}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during query serialization:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-05: Session Manager Lifecycle & Zero Secret Leakage
  test('VC-05: Session Manager controls transition states cleanly with zero raw secrets rendered', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });

      // 1. Initial guest state
      assert.strictEqual(await page.locator('#sessionStatusBadge').textContent(), 'GUEST');

      // 2. Set test session
      await page.locator('#btnSetTestSession').click();
      assert.strictEqual(await page.locator('#sessionStatusBadge').textContent(), 'AUTHENTICATED');
      assert.strictEqual(await page.locator('#tokenPresenceBadge').textContent(), 'PRESENT (Protected)');
      assert.ok((await page.locator('#sessionRoles').textContent()).includes('Learner'));

      // 3. Verify zero token leakage in DOM
      const domHtml = await page.content();
      assert.ok(!domHtml.includes('mock_jwt_test_token_for_verification_only'), 'Raw JWT token must NEVER be rendered in DOM');
      assert.ok(!domHtml.includes('Bearer mock_jwt'), 'Bearer header must NEVER be rendered in DOM');

      // 4. Clear session
      await page.locator('#btnClearTestSession').click();
      assert.strictEqual(await page.locator('#sessionStatusBadge').textContent(), 'GUEST');
      assert.strictEqual(await page.locator('#tokenPresenceBadge').textContent(), 'ABSENT');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during session transitions:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-06: Auth Events Live Monitor
  test('VC-06: Auth Events Monitor captures login, logout, and clear actions in real-time', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });

      // Set session -> dispatches auth:login
      await page.locator('#btnSetTestSession').click();
      await page.waitForTimeout(100);

      const logContent = await page.locator('#authEventsLog').textContent();
      assert.ok(logContent.includes('auth:login'), `Expected auth:login event in log, got: "${logContent}"`);

      // Clear events
      await page.locator('#btnClearAuthEvents').click();
      const clearedContent = await page.locator('#authEventsLog').textContent();
      assert.ok(clearedContent.includes('Chưa có sự kiện'), 'Log must be cleared');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during auth event monitoring:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-07: Radicals Client-Side Search Diagnostic
  test('VC-07: Radicals search diagnostic filters sample dataset accurately by glyph and diacritics', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });
      const searchResult = page.locator('#searchRadicalsResult');

      // 1. Search by Chinese glyph: 木
      await page.locator('#btnSearchGlyph').click();
      await page.locator('#btnTestClientSearch').click();
      let resText = await searchResult.textContent();
      assert.ok(resText.includes('[木]'), `Expected [木] in search result, got: "${resText}"`);
      assert.ok(resText.includes('Mộc'), 'Expected Mộc in search result');

      // 2. Search by Vietnamese with diacritics: Mộc
      await page.locator('#btnSearchWithDiacritics').click();
      await page.locator('#btnTestClientSearch').click();
      resText = await searchResult.textContent();
      assert.ok(resText.includes('[木]'), `Expected [木] in search result for "Mộc", got: "${resText}"`);

      // 3. Search without diacritics: moc
      await page.locator('#btnSearchWithoutDiacritics').click();
      await page.locator('#btnTestClientSearch').click();
      resText = await searchResult.textContent();
      assert.ok(resText.includes('[木]'), `Expected [木] in search result for "moc", got: "${resText}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during search diagnostic:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-08: Target Size Accessibility on Verification Center
  test('VC-08: Controls on ui-verification.html satisfy WCAG 2.2 SC 2.5.8 target size requirements', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });

      const targetViolations = await page.evaluate(() => {
        const controls = Array.from(document.querySelectorAll('button, a.btn, a.btn-chinese-primary, a.btn-chinese-secondary, a.btn-chinese-outline'));
        const violations = [];

        for (const el of controls) {
          const rect = el.getBoundingClientRect();
          if (rect.width === 0 && rect.height === 0) continue;
          if (window.getComputedStyle(el).display === 'none') continue;
          if (window.getComputedStyle(el).visibility === 'hidden') continue;

          // Target size threshold: >= 24px x 24px (WCAG 2.2 SC 2.5.8)
          if (rect.width < 24 || rect.height < 24) {
            violations.push(`${el.tagName.toLowerCase()} ("${(el.textContent || '').trim().slice(0, 30)}") size ${Math.round(rect.width)}x${Math.round(rect.height)}px < 24x24px`);
          }
        }
        return violations;
      });

      assert.strictEqual(
        targetViolations.length,
        0,
        `WCAG 2.2 SC 2.5.8 Target Size violations on ui-verification.html:\n${targetViolations.join('\n')}`
      );

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during a11y check:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-09: Section 9B.3 Vocabulary Diagnostics
  test('VC-09: Section 9B.3 diagnostic controls probe vocabulary catalog, search, detail, and speech API', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });

      // 1. Speech API Capability Check (always evaluates in browser environment)
      const btnSpeech = page.locator('#btnCheckSpeechApi');
      await btnSpeech.waitFor({ state: 'visible' });
      await btnSpeech.click();
      await page.waitForTimeout(100);

      const speechOut = await page.locator('#speechCapabilityOutput').textContent();
      assert.ok(
        speechOut.includes('[CAPABILITY PASS / KHẢ DỤNG]') || speechOut.includes('[CAPABILITY BLOCKED / KHÔNG KHẢ DỤNG]'),
        `Speech output must report capability status, got: "${speechOut}"`
      );

      // 2. Vocabulary Search Probe (search=shu)
      const btnSearchShu = page.locator('#btnProbeVocabSearchShu');
      await btnSearchShu.waitFor({ state: 'visible' });
      await btnSearchShu.click();
      await page.waitForTimeout(500);

      const searchOut = await page.locator('#vocabSearchOutput').textContent();
      assert.ok(
        searchOut.includes('[LIVE PASS / HTTP 200 OK]'),
        `Search probe must execute and receive [LIVE PASS / HTTP 200 OK], got: "${searchOut}"`
      );
      assert.ok(!searchOut.includes('LIVE FAIL'), `Search probe must not fail, got: "${searchOut}"`);

      // 3. Catalog Probe
      const btnCatalog = page.locator('#btnProbeVocabCatalog');
      await btnCatalog.click();
      await page.waitForTimeout(500);

      const catalogOut = await page.locator('#vocabCatalogOutput').textContent();
      assert.ok(
        catalogOut.includes('[LIVE PASS / HTTP 200 OK]'),
        `Catalog probe must execute and receive [LIVE PASS / HTTP 200 OK], got: "${catalogOut}"`
      );
      assert.ok(!catalogOut.includes('LIVE FAIL'), `Catalog probe must not fail, got: "${catalogOut}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during 9B.3 diagnostics:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-10: Section 9C.1 Public Lessons Diagnostics
  test('VC-10: Section 9C.1 diagnostic controls probe lesson catalog, approved filter invariant, orderIndex sorting, and neutral 404 defense', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });

      // 1. In-browser orderIndex sorting validation (deterministic unit probe)
      const btnSort = page.locator('#btnProbeLessonSortLogic');
      await btnSort.waitFor({ state: 'visible' });
      await btnSort.click();
      await page.waitForTimeout(100);

      const sortOut = await page.locator('#lessonDetailOutput').textContent();
      assert.ok(
        sortOut.includes('CHÍNH XÁC 100%'),
        `Sort logic probe must pass with CHÍNH XÁC 100%, got: "${sortOut}"`
      );
      assert.ok(!sortOut.includes('THẤT BẠI'), `Sort logic probe must not report failure, got: "${sortOut}"`);

      // 2. 404 Neutral Defense probe (requests non-existent/unapproved lesson ID 99999999)
      const btn404 = page.locator('#btnProbeLesson404');
      await btn404.waitFor({ state: 'visible' });
      await btn404.click();
      await page.waitForTimeout(600);

      const defOut = await page.locator('#lessonDetailOutput').textContent();
      assert.ok(
        defOut.includes('[LIVE PASS / PHÒNG THỦ 404 TRUNG LẬP THÀNH CÔNG]'),
        `404 Defense probe must report [LIVE PASS / PHÒNG THỦ 404 TRUNG LẬP THÀNH CÔNG], got: "${defOut}"`
      );
      assert.ok(
        defOut.includes('Không tìm thấy bài học hoặc bài học chưa được công khai.'),
        `404 Defense probe must verify exact neutral message, got: "${defOut}"`
      );
      assert.ok(!defOut.includes('LIVE FAIL'), `404 probe must not report LIVE FAIL, got: "${defOut}"`);

      // 3. Public Lesson Catalog Probe
      const btnCatalog = page.locator('#btnProbeLessonCatalog');
      await btnCatalog.waitFor({ state: 'visible' });
      await btnCatalog.click();
      await page.waitForTimeout(600);

      const catOut = await page.locator('#lessonCatalogOutput').textContent();
      assert.ok(
        catOut.includes('[LIVE PASS / HTTP 200 OK]'),
        `Lesson catalog probe must receive [LIVE PASS / HTTP 200 OK], got: "${catOut}"`
      );
      assert.ok(!catOut.includes('LIVE FAIL'), `Catalog probe must not report LIVE FAIL, got: "${catOut}"`);

      // 4. Approved Filter Invariant Probe (distinguishes empty vs populated catalog)
      const btnApproved = page.locator('#btnProbeLessonApprovedFilter');
      await btnApproved.waitFor({ state: 'visible' });
      await btnApproved.click();
      await page.waitForTimeout(600);

      const appOut = await page.locator('#lessonCatalogOutput').textContent();
      assert.ok(
        appOut.includes('[INCONCLUSIVE / NO PUBLIC LESSON DATA]') || appOut.includes('[LIVE PASS / XÁC MINH BẤT BIẾN THÀNH CÔNG]'),
        `Approved filter probe must report INCONCLUSIVE (0 items) or LIVE PASS (all approved), got: "${appOut}"`
      );
      assert.ok(!appOut.includes('LIVE FAIL'), `Approved probe must not report LIVE FAIL, got: "${appOut}"`);
      assert.ok(!appOut.includes('VI PHẠM'), `Approved probe must not find security violations, got: "${appOut}"`);

      // 5. Lesson Detail Dynamic Discovery Probe (avoids hardcoded ID=1 fake pass)
      const btnDetail = page.locator('#btnProbeLessonDetail');
      await btnDetail.waitFor({ state: 'visible' });
      await btnDetail.click();
      await page.waitForTimeout(600);

      const detailOut = await page.locator('#lessonDetailOutput').textContent();
      assert.ok(
        detailOut.includes('[NOT RUN / NO PUBLIC LESSON AVAILABLE]') || detailOut.includes('[LIVE PASS / HTTP 200 OK]'),
        `Lesson detail probe must dynamically report NOT RUN or LIVE PASS, got: "${detailOut}"`
      );
      assert.ok(!detailOut.includes('LIVE FAIL'), `Detail probe must not report LIVE FAIL, got: "${detailOut}"`);

      // Filter expected 404 from non-existent ID probe (99999999)
      const errors = getRuntimeErrors().filter(e => !e.includes('404'));
      assert.strictEqual(errors.length, 0, `Runtime errors during 9C.1 diagnostics:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-11: Section 9C.2 Personal Notes Diagnostics Probes
  test('VC-11: Section 9C.2 diagnostic controls probe 500-char boundary, anonymous guard, and notes API capability', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });

      // 1. Check Section 9C.2 heading
      const h9c2 = page.locator('#heading-9c2');
      await h9c2.waitFor({ state: 'visible' });
      assert.ok(await h9c2.isVisible(), 'Section 9C.2 heading must be visible');

      // 2. 500-Character Boundary Probe
      const btnCharLimit = page.locator('#btnProbeCharLimitValidation');
      await btnCharLimit.waitFor({ state: 'visible' });
      await btnCharLimit.click();
      await page.waitForTimeout(200);

      const valOut = await page.locator('#notesValidationOutput').textContent();
      assert.ok(valOut.includes('[UNIT TEST: validateNoteContent 500-char boundary]'), `Expected 500-char probe header, got: "${valOut}"`);
      assert.ok(valOut.includes('100% PASS'), `Expected 100% PASS in 500-char boundary check, got: "${valOut}"`);

      // 3. Anonymous Note Guard Probe
      const btnAnon = page.locator('#btnProbeAnonymousNoteGuard');
      await btnAnon.waitFor({ state: 'visible' });
      await btnAnon.click();
      await page.waitForTimeout(200);

      const anonOut = await page.locator('#notesValidationOutput').textContent();
      assert.ok(anonOut.includes('[CAPABILITY TEST: Anonymous Note Guard]'), `Expected anon guard header, got: "${anonOut}"`);
      assert.ok(anonOut.includes('SẴN SÀNG & HOẠT ĐỘNG CHUẨN XÁC'), `Expected anon guard ready status, got: "${anonOut}"`);

      // 4. Ownership Invariant Probe
      const btnOwnership = page.locator('#btnProbeNoteOwnershipInvariant');
      await btnOwnership.waitFor({ state: 'visible' });
      await btnOwnership.click();
      await page.waitForTimeout(200);

      const ownOut = await page.locator('#notesApiOutput').textContent();
      assert.ok(ownOut.includes('[CONTRACT VERIFICATION: Note Ownership Invariant]'), `Expected ownership invariant header, got: "${ownOut}"`);
      assert.ok(ownOut.includes('ĐẠT CHUẨN 100%'), `Expected ownership invariant pass, got: "${ownOut}"`);

      // 5. Notes API Capability Probe (in guest mode, must report ENVIRONMENT BLOCKED / CHƯA ĐĂNG NHẬP safely, not crash or fake pass)
      const btnNotesApi = page.locator('#btnProbeNotesApi');
      await btnNotesApi.waitFor({ state: 'visible' });
      await btnNotesApi.click();
      await page.waitForTimeout(200);

      const apiOut = await page.locator('#notesApiOutput').textContent();
      assert.ok(
        apiOut.includes('[ENVIRONMENT BLOCKED / CHƯA ĐĂNG NHẬP]') || apiOut.includes('[LIVE PASS / KẾT NỐI NOTES API THÀNH CÔNG]'),
        `Notes API probe must safely report BLOCKED or LIVE PASS, got: "${apiOut}"`
      );
      assert.ok(!apiOut.includes('LIVE FAIL'), `Notes API probe must not report LIVE FAIL in guest mode, got: "${apiOut}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during 9C.2 diagnostics:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-12: Section 9C.3 Interactive SRS Flashcard Review Session Diagnostics
  test('VC-12: Section 9C.3 diagnostic controls probe rating mapping, timer integrity, requeue policy, and SRS guard', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });

      // 1. Check Section 9C.3 heading
      const h9c3 = page.locator('#heading-9c3');
      await h9c3.waitFor({ state: 'visible' });
      assert.ok(await h9c3.isVisible(), 'Section 9C.3 heading must be visible');

      // 2. SRS Rating 1..4 Contract Probe
      const btnRating = page.locator('#btnProbeSrsRatingMapping');
      await btnRating.waitFor({ state: 'visible' });
      await btnRating.click();
      await page.waitForTimeout(200);

      const contractOut = await page.locator('#srsContractOutput').textContent();
      assert.ok(contractOut.includes('[UNIT / CONTRACT: SRS Rating Mapping 1..4]'), `Expected rating contract header, got: "${contractOut}"`);
      assert.ok(contractOut.includes('100% PASS'), `Expected 100% PASS for rating mapping, got: "${contractOut}"`);

      // 3. Reaction Timer Integrity Probe
      const btnTimer = page.locator('#btnProbeSrsTimer');
      await btnTimer.waitFor({ state: 'visible' });
      await btnTimer.click();
      await page.waitForTimeout(200);

      const timerOut = await page.locator('#srsContractOutput').textContent();
      assert.ok(timerOut.includes('[CAPABILITY TEST: Reaction Timer Integrity]'), `Expected timer probe header, got: "${timerOut}"`);
      assert.ok(timerOut.includes('"reviewTimeSeconds" (camelCase CHUẨN)'), `Expected camelCase reviewTimeSeconds, got: "${timerOut}"`);
      assert.ok(timerOut.includes('100% PASS'), `Expected 100% PASS for reaction timer, got: "${timerOut}"`);

      // 4. Again Requeue Policy Probe
      const btnRequeue = page.locator('#btnProbeSrsRequeue');
      await btnRequeue.waitFor({ state: 'visible' });
      await btnRequeue.click();
      await page.waitForTimeout(200);

      const requeueOut = await page.locator('#srsContractOutput').textContent();
      assert.ok(requeueOut.includes('[LOGIC TEST: Again Local Session Requeue Policy]'), `Expected requeue header, got: "${requeueOut}"`);
      assert.ok(requeueOut.includes('MAX_AGAIN_REVIEWS_PER_CARD (3)'), `Expected max again limit check, got: "${requeueOut}"`);
      assert.ok(requeueOut.includes('100% PASS'), `Expected 100% PASS for requeue policy, got: "${requeueOut}"`);

      // 5. Anonymous Guard Probe
      const btnAnon = page.locator('#btnProbeSrsAnonymousGuard');
      await btnAnon.waitFor({ state: 'visible' });
      await btnAnon.click();
      await page.waitForTimeout(200);

      const anonOut = await page.locator('#srsApiOutput').textContent();
      assert.ok(anonOut.includes('[CAPABILITY TEST: SRS Anonymous Guard]'), `Expected anon guard header, got: "${anonOut}"`);
      assert.ok(anonOut.includes('SẴN SÀNG & HOẠT ĐỘNG CHUẨN XÁC'), `Expected anon guard ready status, got: "${anonOut}"`);

      // 6. Due Cards API Probe (guest mode must report ENVIRONMENT BLOCKED / CHƯA ĐĂNG NHẬP safely)
      const btnDueApi = page.locator('#btnProbeSrsDueApi');
      await btnDueApi.waitFor({ state: 'visible' });
      await btnDueApi.click();
      await page.waitForTimeout(200);

      const apiOut = await page.locator('#srsApiOutput').textContent();
      assert.ok(
        apiOut.includes('[ENVIRONMENT BLOCKED / CHƯA ĐĂNG NHẬP]') || apiOut.includes('[LIVE PASS / KẾT NỐI SRS DUE CARDS THÀNH CÔNG]'),
        `SRS API probe must safely report BLOCKED or LIVE PASS, got: "${apiOut}"`
      );
      assert.ok(!apiOut.includes('LIVE FAIL'), `SRS API probe must not report LIVE FAIL in guest mode, got: "${apiOut}"`);

      // 7. SRS Daily Settings & Quota Math Probe (9C.4)
      const btnSettingsVal = page.locator('#btnProbeSrsSettingsValidation');
      await btnSettingsVal.waitFor({ state: 'visible' });
      await btnSettingsVal.click();
      await page.waitForTimeout(200);

      const valOut = await page.locator('#srsContractOutput').textContent();
      assert.ok(valOut.includes('[UNIT / CONTRACT: SRS Daily Settings Validation & Quota (9C.4)]'), `Expected settings validation header, got: "${valOut}"`);
      assert.ok(valOut.includes('100% PASS'), `Expected 100% PASS for settings validation, got: "${valOut}"`);

      // 8. SRS Stats & Settings Live API Probe (9C.4)
      const btnStatsApi = page.locator('#btnProbeSrsStatsApi');
      await btnStatsApi.waitFor({ state: 'visible' });
      await btnStatsApi.click();
      await page.waitForTimeout(200);

      const statsOut = await page.locator('#srsApiOutput').textContent();
      assert.ok(
        statsOut.includes('[ENVIRONMENT BLOCKED / CHƯA ĐĂNG NHẬP]') || statsOut.includes('[LIVE PASS / KẾT NỐI SRS STATS & SETTINGS THÀNH CÔNG]'),
        `SRS stats API probe must safely report BLOCKED or LIVE PASS, got: "${statsOut}"`
      );
      assert.ok(!statsOut.includes('LIVE FAIL'), `SRS stats API probe must not report LIVE FAIL in guest mode, got: "${statsOut}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during SRS diagnostics:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-13: Module 9D.1 Creator Studio Diagnostics
  test('VC-13: Section 9D.1 Creator Studio diagnostic probes execute and report status safely', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#creatorVerificationHeading').waitFor({ state: 'visible' });

      // 1. Probe Reorder Logic & Payload
      await page.locator('#btnProbeCreatorReorderLogic').click();
      await page.waitForTimeout(100);
      const reorderOut = await page.locator('#creatorContractOutput').textContent();
      assert.ok(reorderOut.includes('[UNIT PASS]'), `Reorder contract probe must pass, got: "${reorderOut}"`);

      // 2. Probe Title Validation
      await page.locator('#btnProbeCreatorValidation').click();
      await page.waitForTimeout(100);
      const valOut = await page.locator('#creatorContractOutput').textContent();
      assert.ok(valOut.includes('[UNIT PASS]'), `Validation probe must pass, got: "${valOut}"`);

      // 3. Probe Role Guard
      await page.locator('#btnProbeCreatorRoleGuard').click();
      await page.waitForTimeout(100);
      const roleOut = await page.locator('#creatorApiOutput').textContent();
      assert.ok(roleOut.includes('[CAPABILITY CHECK]'), `Role capability probe must report, got: "${roleOut}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during Creator diagnostics:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-14: Module 9D.2 Creator Two-Step Excel Import Diagnostics
  test('VC-14: Section 9D.2 Creator Excel Import diagnostic probes execute and report status safely', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#creatorImportVerificationHeading').waitFor({ state: 'visible' });

      // 1. Probe File & Title Rules
      await page.locator('#btnProbeImportValidation').click();
      await page.waitForTimeout(100);
      const valOut = await page.locator('#importContractOutput').textContent();
      assert.ok(valOut.includes('[UNIT PASS]'), `Validation probe must pass, got: "${valOut}"`);

      // 2. Probe Two-Step Invariant & Report Model
      await page.locator('#btnProbeImportReportLogic').click();
      await page.waitForTimeout(100);
      const invOut = await page.locator('#importLogicOutput').textContent();
      assert.ok(invOut.includes('[CAPABILITY PASS]'), `Invariant probe must pass, got: "${invOut}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during Excel Import diagnostics:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-15: Module 9D.3 Creator Lesson Submission & Status Visibility Diagnostics
  test('VC-15: Section 9D.3 Creator Submission diagnostic probes execute and report status safely', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#creatorSubmitVerificationHeading').waitFor({ state: 'visible' });

      // 1. Probe Submission Eligibility Rules
      await page.locator('#btnProbeSubmissionEligibility').click();
      await page.waitForTimeout(100);
      const eligOut = await page.locator('#submissionEligibilityOutput').textContent();
      assert.ok(eligOut.includes('[UNIT PASS]'), `Submission eligibility probe must pass, got: "${eligOut}"`);

      // 2. Probe Rejection Feedback Contract & Graceful Fallback
      await page.locator('#btnProbeRejectionContract').click();
      await page.waitForTimeout(100);
      const rejOut = await page.locator('#rejectionContractOutput').textContent();
      assert.ok(rejOut.includes('[CONTRACT PASS]'), `Rejection contract probe must pass, got: "${rejOut}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during Submission diagnostics:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // VC-16: Module 9F.1 Admin Accounts Lifecycle UI Diagnostics
  test('VC-16: Section 15 Admin Accounts Lifecycle UI diagnostic probes execute and report status safely', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#section-admin-accounts').waitFor({ state: 'visible' });

      // 1. Probe Admin Accounts Capability
      const btnProbe = page.locator('#btnProbeAdminAccountsCapability');
      await btnProbe.waitFor({ state: 'visible' });
      await btnProbe.click();
      await page.waitForTimeout(200);

      const out = await page.locator('#adminAccountsProbeOutput').textContent();
      assert.ok(out.includes('Role Guard Invariant (Admin): PASS'), `Expected role guard invariant PASS, got: "${out}"`);
      assert.ok(out.includes('Bảo vệ tự thân (POL-8D-01 Logic): PASS'), `Expected POL-8D-01 PASS, got: "${out}"`);
      assert.ok(out.includes('Chuẩn hóa Query Params: PASS'), `Expected query params PASS, got: "${out}"`);
      assert.ok(out.includes('[PROBE PASS]'), `Expected overall PROBE PASS, got: "${out}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during Admin Accounts diagnostics:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

});


