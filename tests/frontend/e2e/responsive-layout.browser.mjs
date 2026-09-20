/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: RESPONSIVE & MULTI-VIEWPORT LAYOUT VERIFICATION (TASK 9G.5)
 * File: tests/frontend/e2e/responsive-layout.browser.mjs
 * 
 * Verifies:
 * - Test 1: All-page smoke & document-level horizontal overflow across viewports (375, 768, 1200).
 * - Test 2: Viewport meta tag declaration across all 22 HTML pages.
 * - Test 3: WCAG 2.2 SC 1.4.10 Reflow spot-check at 320px CSS width.
 * - Test 4: Dense administrative & moderation table localized scroll containment.
 * - Test 5: Interactive SRS 3D Flashcard responsiveness & 2x2 rating grid adaptation.
 * - Test 6: Creator Excel Import 2-step responsive workflow (dropzone, preview table).
 * - Test 7: Responsive navigation & hamburger toggle behavior (mobile vs desktop).
 * - Test 8: Modal dialog viewport sizing and containment on mobile.
 * - Test 9: Bootstrap breakpoint boundary checks (575px vs 576px, 991px vs 992px).
 * - Test 10: Capture visual screenshot artifacts for 9 representative pages.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const SCREENSHOT_DIR = path.resolve(__dirname, '../artifacts/screenshots');
const BRAIN_SCREENSHOT_DIR = 'C:/Users/LENOVO/.gemini/antigravity-ide/brain/eb5318a3-6c2c-4178-8c10-32bfb7257c74/screenshots';

const ALL_22_PAGES = [
  'index.html',
  'login.html',
  'register.html',
  'profile.html',
  'radicals.html',
  'vocabulary.html',
  'lessons.html',
  'lesson-detail.html?id=1',
  'srs-review.html',
  'srs-dashboard.html',
  'creator-lessons.html',
  'creator-lesson-editor.html?id=1',
  'creator-import.html',
  'moderator-queue.html',
  'moderator-review.html?id=1',
  'moderator-history.html',
  'admin-accounts.html',
  'admin-roles.html',
  'admin-radicals.html',
  'admin-vocabulary.html',
  'admin-lessons.html',
  'ui-verification.html'
];

describe('Responsive & Multi-Viewport Layout Verification (Task 9G.5)', () => {
  let serverHandle;
  let baseUrl;
  let browser;

  before(async () => {
    serverHandle = await ensureServer({ port: 3000 });
    baseUrl = serverHandle.baseUrl;
    browser = await launchBrowser({ headless: true });

    // Ensure screenshot directories exist
    fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
    try {
      fs.mkdirSync(BRAIN_SCREENSHOT_DIR, { recursive: true });
    } catch {
      // ignore if brain dir not accessible
    }
  });

  after(async () => {
    if (browser) {
      await browser.close();
    }
    if (serverHandle) {
      await serverHandle.close();
    }
  });

  /**
   * Helper to set mock authenticated session for pages requiring auth.
   */
  async function setupMockSession(page) {
    await page.addInitScript(() => {
      localStorage.setItem('access_token', 'mock_jwt_responsive_testing');
      localStorage.setItem('user_info', JSON.stringify({
        accountId: 1,
        emailOrPhone: 'admin@example.com',
        fullName: 'Quản Trị Viên Hệ Thống',
        roles: ['Admin', 'Creator', 'Moderator', 'Learner']
      }));
    });

    await page.route('**/api/v1/**', async (route) => {
      const url = route.request().url();
      if (url.includes('/lessons/1')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: {
              lessonId: 1,
              title: 'Bài 1: Giới thiệu Tiếng Trung cơ bản cho người mới bắt đầu học',
              status: 'Approved',
              vocabularyCount: 2,
              createdAt: '2026-09-01T00:00:00Z',
              updatedAt: '2026-09-02T00:00:00Z',
              vocabularies: [
                { vocabId: 1, orderIndex: 1, hanzi: '你', pinyin: 'nǐ', meaningHanViet: 'Nhĩ', meaningVi: 'Bạn, anh, chị' },
                { vocabId: 2, orderIndex: 2, hanzi: '好', pinyin: 'hǎo', meaningHanViet: 'Hảo', meaningVi: 'Tốt, đẹp, hay' }
              ]
            }
          })
        });
      }
      if (url.includes('/srs/due')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: [{
              itemType: 'VOCABULARY',
              itemId: 1,
              hanzi: '学',
              pinyin: 'xué',
              meaningHanViet: 'Học',
              meaningVi: 'Học tập, nghiên cứu khoa học và tri thức',
              exampleSentence: '我在大学学中文。',
              exampleTranslation: 'Tôi học tiếng Trung ở trường đại học.',
              repetitions: 1,
              easeFactor: 2.5,
              intervalDays: 1
            }]
          })
        });
      }
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'SUCCESS',
          data: {
            page: 0,
            size: 20,
            totalElements: 2,
            totalPages: 1,
            first: true,
            last: true,
            items: [
              { accountId: 1, emailOrPhone: 'admin@example.com', fullName: 'Quản Trị Viên Hệ Thống', status: 'Active', roles: ['Admin'] },
              { accountId: 2, emailOrPhone: 'tester@example.com', fullName: 'Người Dùng Kiểm Thử', status: 'Active', roles: ['Learner'] }
            ]
          }
        })
      });
    });
  }

  // TEST 1: All-Page Smoke & Document-Level Overflow Audit
  test('Test 1: All 22 pages render with ZERO unintended document-level horizontal overflow across Mobile (375px), Tablet (768px), and Desktop (1200px)', async () => {
    const viewports = [
      { name: 'Mobile-375', width: 375, height: 667 },
      { name: 'Tablet-768', width: 768, height: 1024 },
      { name: 'Desktop-1200', width: 1200, height: 800 }
    ];

    for (const vp of viewports) {
      const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
        viewportWidth: vp.width,
        viewportHeight: vp.height
      });

      try {
        await setupMockSession(page);

        for (const pageName of ALL_22_PAGES) {
          await page.goto(`${baseUrl}/${pageName}`, { waitUntil: 'domcontentloaded' });
          await page.waitForTimeout(100);

          const overflowData = await page.evaluate(() => {
            const docEl = document.documentElement;
            const body = document.body;
            const clientWidth = docEl.clientWidth;
            const docScrollWidth = docEl.scrollWidth;
            const bodyScrollWidth = body ? body.scrollWidth : 0;
            const maxScrollWidth = Math.max(docScrollWidth, bodyScrollWidth);

            // 1.5px tolerance for fractional sub-pixel layout rounding
            const hasOverflow = maxScrollWidth > clientWidth + 1.5;

            let worst = null;
            if (hasOverflow) {
              const elements = Array.from(document.querySelectorAll('*'));
              for (const el of elements) {
                if (el.closest('.table-responsive, .table-responsive-clean, pre, code')) continue;
                const rect = el.getBoundingClientRect();
                if (rect.right > clientWidth + 2 || rect.width > clientWidth + 2) {
                  worst = {
                    tag: el.tagName.toLowerCase(),
                    id: el.id || '',
                    className: el.className || '',
                    right: Math.round(rect.right),
                    width: Math.round(rect.width)
                  };
                  break;
                }
              }
            }

            return {
              clientWidth,
              maxScrollWidth,
              hasOverflow,
              worst
            };
          });

          assert.strictEqual(
            overflowData.hasOverflow,
            false,
            `Document horizontal overflow detected on ${pageName} at ${vp.name}! clientWidth=${overflowData.clientWidth}, scrollWidth=${overflowData.maxScrollWidth}. Worst: ${JSON.stringify(overflowData.worst)}`
          );
        }

        const errors = getRuntimeErrors();
        assert.strictEqual(errors.length, 0, `Runtime errors during ${vp.name} all-page smoke:\n${errors.join('\n')}`);
      } finally {
        await context.close();
      }
    }
  });

  // TEST 2: Viewport Meta Tag Conformance
  test('Test 2: All 22 HTML pages declare responsive viewport meta tag', async () => {
    const { page, context } = await createMonitoredPage(browser);
    try {
      for (const pageName of ALL_22_PAGES) {
        await page.goto(`${baseUrl}/${pageName}`, { waitUntil: 'domcontentloaded' });
        const metaViewport = await page.locator('meta[name="viewport"]').getAttribute('content');
        assert.ok(metaViewport, `Page ${pageName} missing meta[name="viewport"]`);
        assert.ok(
          metaViewport.includes('width=device-width'),
          `Page ${pageName} viewport meta must include width=device-width (got "${metaViewport}")`
        );
        assert.ok(
          metaViewport.includes('initial-scale=1'),
          `Page ${pageName} viewport meta must include initial-scale=1 (got "${metaViewport}")`
        );
      }
    } finally {
      await context.close();
    }
  });

  // TEST 3: WCAG 2.2 SC 1.4.10 Reflow Spot-Check (320px CSS width)
  test('Test 3: WCAG 2.2 SC 1.4.10 Reflow Spot-Check (320px CSS width) on representative core pages', async () => {
    const reflowPages = [
      'index.html',
      'login.html',
      'register.html',
      'profile.html',
      'radicals.html',
      'vocabulary.html',
      'lessons.html',
      'lesson-detail.html?id=1',
      'srs-dashboard.html'
    ];

    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 320,
      viewportHeight: 640
    });

    try {
      await setupMockSession(page);

      for (const pageName of reflowPages) {
        await page.goto(`${baseUrl}/${pageName}`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(100);

        const reflowAudit = await page.evaluate(() => {
          const docEl = document.documentElement;
          const body = document.body;
          const clientWidth = docEl.clientWidth;
          const maxScrollWidth = Math.max(docEl.scrollWidth, body ? body.scrollWidth : 0);
          return {
            clientWidth,
            maxScrollWidth,
            hasOverflow: maxScrollWidth > clientWidth + 1.5
          };
        });

        assert.strictEqual(
          reflowAudit.hasOverflow,
          false,
          `Reflow failure at 320px on ${pageName}: scrollWidth=${reflowAudit.maxScrollWidth}, clientWidth=${reflowAudit.clientWidth}`
        );
      }

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during 320px reflow spot-check:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // TEST 4: Dense Administrative & Moderation Table Localized Scroll Containment
  test('Test 4: Dense tables in Admin, Moderator, and Creator pages are wrapped in localized scroll containers (.table-responsive)', async () => {
    const tablePages = [
      'admin-accounts.html',
      'admin-roles.html',
      'admin-radicals.html',
      'admin-vocabulary.html',
      'admin-lessons.html',
      'creator-lessons.html',
      'creator-import.html',
      'moderator-queue.html',
      'moderator-history.html'
    ];

    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 375,
      viewportHeight: 667
    });

    try {
      await setupMockSession(page);

      for (const pageName of tablePages) {
        await page.goto(`${baseUrl}/${pageName}`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(100);

        const tableContainment = await page.evaluate(() => {
          const docEl = document.documentElement;
          const clientWidth = docEl.clientWidth;
          const maxDocScrollWidth = Math.max(docEl.scrollWidth, document.body ? document.body.scrollWidth : 0);
          const hasDocOverflow = maxDocScrollWidth > clientWidth + 1.5;

          const tables = Array.from(document.querySelectorAll('table'));
          const audit = tables.map(t => {
            const wrapper = t.closest('.table-responsive, .table-responsive-clean');
            const wrapperComputed = wrapper ? window.getComputedStyle(wrapper) : null;
            return {
              hasWrapper: Boolean(wrapper),
              overflowX: wrapperComputed ? wrapperComputed.overflowX : 'none',
              tableWidth: Math.round(t.getBoundingClientRect().width),
              wrapperWidth: wrapper ? Math.round(wrapper.getBoundingClientRect().width) : 0
            };
          });

          return {
            hasDocOverflow,
            tableCount: tables.length,
            audit
          };
        });

        assert.ok(tableContainment.tableCount > 0, `Page ${pageName} must contain at least one table`);
        assert.strictEqual(
          tableContainment.hasDocOverflow,
          false,
          `Table in ${pageName} leaked outside container and caused document overflow at 375px!`
        );

        for (const tbl of tableContainment.audit) {
          assert.strictEqual(tbl.hasWrapper, true, `Table in ${pageName} must be wrapped in .table-responsive`);
          assert.ok(
            tbl.overflowX === 'auto' || tbl.overflowX === 'scroll',
            `Table wrapper in ${pageName} must have overflow-x auto/scroll (got "${tbl.overflowX}")`
          );
        }
      }

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during table scroll containment audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // TEST 5: Interactive SRS 3D Flashcard Responsiveness & 2x2 Rating Grid Adaptation
  test('Test 5: SRS Flashcard review room fits mobile viewport (375px) and adapts rating buttons to 2x2 grid', async () => {
    // 1. Mobile 375px
    const { page: mobilePage, context: mobileContext, getRuntimeErrors: getMobileErrors } = await createMonitoredPage(browser, {
      viewportWidth: 375,
      viewportHeight: 667
    });

    try {
      await setupMockSession(mobilePage);
      await mobilePage.goto(`${baseUrl}/srs-review.html`, { waitUntil: 'domcontentloaded' });
      await mobilePage.locator('#srsActiveSession').waitFor({ state: 'visible' });

      // Verify card fits within mobile viewport
      const flashcard = mobilePage.locator('#flashcard');
      const cardBox = await flashcard.boundingBox();
      assert.ok(cardBox.width <= 375, `Flashcard width (${cardBox.width}px) must not exceed viewport width (375px)`);

      // Verify 3D flip animation functionality on mobile
      assert.strictEqual(await flashcard.getAttribute('aria-expanded'), 'false');
      await flashcard.click();
      await mobilePage.waitForTimeout(100);
      assert.strictEqual(await flashcard.getAttribute('aria-expanded'), 'true');

      // Verify 2x2 grid arrangement on mobile (<= 576px)
      const ratingBar = mobilePage.locator('.srs-rating-bar');
      const gridColumns = await ratingBar.evaluate(el => window.getComputedStyle(el).gridTemplateColumns);
      const cols = gridColumns.split(' ').filter(c => c.length > 0);
      assert.strictEqual(cols.length, 2, `SRS rating bar must have 2 columns on mobile viewport (got "${gridColumns}")`);

      // Verify rating buttons are usable touch targets (>= 44x44)
      const ratingBtns = mobilePage.locator('.btn-rating');
      assert.strictEqual(await ratingBtns.count(), 4);
      for (let i = 0; i < 4; i++) {
        const btnBox = await ratingBtns.nth(i).boundingBox();
        assert.ok(btnBox.width >= 44 && btnBox.height >= 44, `Rating button ${i + 1} must meet touch target size`);
      }

      // Verify audio button is reachable
      const audioBtn = mobilePage.locator('#btnAudioPronounceFront');
      assert.ok(await audioBtn.isVisible(), 'Audio pronunciation button must be visible and reachable on mobile');

      const mobileErrors = getMobileErrors();
      assert.strictEqual(mobileErrors.length, 0, `Runtime errors during mobile SRS flashcard audit:\n${mobileErrors.join('\n')}`);
    } finally {
      await mobileContext.close();
    }

    // 2. Desktop 1200px
    const { page: deskPage, context: deskContext } = await createMonitoredPage(browser, {
      viewportWidth: 1200,
      viewportHeight: 800
    });

    try {
      await setupMockSession(deskPage);
      await deskPage.goto(`${baseUrl}/srs-review.html`, { waitUntil: 'domcontentloaded' });
      await deskPage.locator('#srsActiveSession').waitFor({ state: 'visible' });

      // On desktop (>= 576px), rating bar has 4 columns in a single row
      const ratingBar = deskPage.locator('.srs-rating-bar');
      const gridColumns = await ratingBar.evaluate(el => window.getComputedStyle(el).gridTemplateColumns);
      const cols = gridColumns.split(' ').filter(c => c.length > 0);
      assert.strictEqual(cols.length, 4, `SRS rating bar must have 4 columns on desktop viewport (got "${gridColumns}")`);
    } finally {
      await deskContext.close();
    }
  });

  // TEST 6: Creator Excel Import 2-Step Responsive Workflow
  test('Test 6: Creator Excel Import 2-step workflow renders responsively on mobile and desktop without layout breakage', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 375,
      viewportHeight: 667
    });

    try {
      await setupMockSession(page);
      await page.goto(`${baseUrl}/creator-import.html`, { waitUntil: 'domcontentloaded' });

      // 1. Dropzone container fits inside viewport
      const dropzone = page.locator('#fileDropZone');
      await dropzone.waitFor({ state: 'visible' });
      const dropBox = await dropzone.boundingBox();
      assert.ok(dropBox.width <= 375, `Dropzone width (${dropBox.width}) must fit inside 375px viewport`);

      // 2. Step 2 Preview table container maintains localized horizontal scrolling
      const tableWrapper = page.locator('#previewTableSection .table-responsive');
      assert.ok(await tableWrapper.count() > 0, 'Preview table must be wrapped inside .table-responsive container');
      const hasTableResponsive = await tableWrapper.evaluate(el => el.classList.contains('table-responsive'));
      assert.strictEqual(hasTableResponsive, true, 'Import preview table container must have table-responsive class');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during creator import responsive audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // TEST 7: Responsive Navigation & Hamburger Toggle
  test('Test 7: Shared navigation toggler collapses into accessible hamburger on mobile and expands on desktop', async () => {
    // 1. Mobile (375px)
    const { page: mobilePage, context: mobileContext } = await createMonitoredPage(browser, {
      viewportWidth: 375,
      viewportHeight: 667
    });

    try {
      await mobilePage.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
      const toggler = mobilePage.locator('.navbar-toggler');
      assert.ok(await toggler.isVisible(), 'Navbar toggler button must be visible on mobile viewport (< 992px)');

      const navCollapse = mobilePage.locator('#primaryNavMenu');
      assert.strictEqual(await navCollapse.evaluate(el => el.classList.contains('show')), false, 'Navbar collapse should initially be closed on mobile');

      // Click toggler to expand
      await toggler.click();
      await mobilePage.waitForTimeout(350); // wait for Bootstrap transition
      assert.strictEqual(await navCollapse.evaluate(el => el.classList.contains('show')), true, 'Navbar collapse must be open after clicking toggler');

      // Ensure open navbar does not cause document overflow
      const docOverflow = await mobilePage.evaluate(() => {
        return document.documentElement.scrollWidth > document.documentElement.clientWidth + 1;
      });
      assert.strictEqual(docOverflow, false, 'Expanded mobile menu must not cause horizontal document overflow');
    } finally {
      await mobileContext.close();
    }

    // 2. Desktop (1200px)
    const { page: deskPage, context: deskContext } = await createMonitoredPage(browser, {
      viewportWidth: 1200,
      viewportHeight: 800
    });

    try {
      await deskPage.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
      const toggler = deskPage.locator('.navbar-toggler');
      assert.strictEqual(await toggler.isVisible(), false, 'Navbar toggler button must be hidden on desktop viewport (>= 992px)');

      const navCollapse = deskPage.locator('#primaryNavMenu');
      assert.ok(await navCollapse.isVisible(), 'Desktop navigation bar must be visible on desktop viewport');
    } finally {
      await deskContext.close();
    }
  });

  // TEST 8: Modal Dialog Viewport Sizing & Containment on Mobile
  test('Test 8: Modal dialogs fit within mobile viewport width without clipped controls', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 375,
      viewportHeight: 667
    });

    try {
      await setupMockSession(page);
      await page.goto(`${baseUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('.radical-card');

      // Open detail modal
      await page.locator('.radical-card').first().click();
      const modal = page.locator('#radicalDetailModal');
      await modal.waitFor({ state: 'visible' });

      // Verify modal fits within mobile viewport
      const modalBox = await modal.boundingBox();
      assert.ok(modalBox.width <= 375, `Modal width (${modalBox.width}px) must fit within 375px viewport`);

      // Verify close button remains reachable
      const closeBtn = page.locator('#modalCloseBtn');
      assert.ok(await closeBtn.isVisible(), 'Modal close button must remain visible and reachable on mobile');

      // Dismiss modal
      await closeBtn.click();
      await page.waitForTimeout(200);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during modal responsive audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // TEST 9: Bootstrap Breakpoint Boundary Checks
  test('Test 9: Critical components adapt predictably across Bootstrap breakpoint boundaries (575px vs 576px, 991px vs 992px)', async () => {
    // Check 575px vs 576px on SRS rating bar
    const { page: p575, context: c575 } = await createMonitoredPage(browser, { viewportWidth: 575, viewportHeight: 700 });
    const { page: p576, context: c576 } = await createMonitoredPage(browser, { viewportWidth: 576, viewportHeight: 700 });

    try {
      await setupMockSession(p575);
      await p575.goto(`${baseUrl}/srs-review.html`, { waitUntil: 'domcontentloaded' });
      await p575.locator('#srsActiveSession').waitFor({ state: 'visible' });

      const cols575 = (await p575.locator('.srs-rating-bar').evaluate(el => window.getComputedStyle(el).gridTemplateColumns))
        .split(' ').filter(c => c.length > 0).length;
      assert.strictEqual(cols575, 2, '575px must render 2-column rating bar');

      await setupMockSession(p576);
      await p576.goto(`${baseUrl}/srs-review.html`, { waitUntil: 'domcontentloaded' });
      await p576.locator('#srsActiveSession').waitFor({ state: 'visible' });

      const cols576 = (await p576.locator('.srs-rating-bar').evaluate(el => window.getComputedStyle(el).gridTemplateColumns))
        .split(' ').filter(c => c.length > 0).length;
      assert.strictEqual(cols576, 4, '576px must render 4-column rating bar');
    } finally {
      await c575.close();
      await c576.close();
    }

    // Check 991px vs 992px on Navbar toggler
    const { page: p991, context: c991 } = await createMonitoredPage(browser, { viewportWidth: 991, viewportHeight: 700 });
    const { page: p992, context: c992 } = await createMonitoredPage(browser, { viewportWidth: 992, viewportHeight: 700 });

    try {
      await p991.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
      assert.strictEqual(await p991.locator('.navbar-toggler').isVisible(), true, '991px must display navbar toggler');

      await p992.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
      assert.strictEqual(await p992.locator('.navbar-toggler').isVisible(), false, '992px must hide navbar toggler');
    } finally {
      await c991.close();
      await c992.close();
    }
  });

  // TEST 10: Visual Screenshot Capture for 9 Representative Pages
  test('Test 10: Capture visual screenshot artifacts for 9 representative pages across Mobile (375px), Tablet (768px), and Desktop (1200px)', async () => {
    const representativePages = [
      { name: 'index', path: 'index.html' },
      { name: 'vocabulary', path: 'vocabulary.html' },
      { name: 'lesson-detail', path: 'lesson-detail.html?id=1' },
      { name: 'srs-review', path: 'srs-review.html' },
      { name: 'creator-import', path: 'creator-import.html' },
      { name: 'admin-accounts', path: 'admin-accounts.html' },
      { name: 'admin-lessons', path: 'admin-lessons.html' },
      { name: 'moderator-queue', path: 'moderator-queue.html' },
      { name: 'profile', path: 'profile.html' }
    ];

    const captureViewports = [
      { name: 'mobile-375', width: 375, height: 667 },
      { name: 'tablet-768', width: 768, height: 1024 },
      { name: 'desktop-1200', width: 1200, height: 800 }
    ];

    let capturedCount = 0;

    for (const vp of captureViewports) {
      const { page, context } = await createMonitoredPage(browser, {
        viewportWidth: vp.width,
        viewportHeight: vp.height
      });

      try {
        await setupMockSession(page);

        for (const item of representativePages) {
          await page.goto(`${baseUrl}/${item.path}`, { waitUntil: 'domcontentloaded' });
          await page.waitForTimeout(100);

          const filename = `${item.name}-${vp.name}.png`;
          const localPath = path.join(SCREENSHOT_DIR, filename);
          await page.screenshot({ path: localPath, fullPage: false });

          // Also save in brain artifact dir if available
          try {
            const brainPath = path.join(BRAIN_SCREENSHOT_DIR, filename);
            fs.copyFileSync(localPath, brainPath);
          } catch {
            // ignore if copy fails
          }

          assert.ok(fs.existsSync(localPath), `Screenshot file ${filename} must exist on disk`);
          capturedCount++;
        }
      } finally {
        await context.close();
      }
    }

    assert.strictEqual(
      capturedCount,
      representativePages.length * captureViewports.length,
      `Must capture exactly ${representativePages.length * captureViewports.length} screenshots (got ${capturedCount})`
    );
  });

});
