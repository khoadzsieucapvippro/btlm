/**
 * =============================================================================
 * ACCESSIBILITY BROWSER VERIFICATION (TASK 9A.1)
 * Standards: W3C WCAG 2.2 Level AA & WAI-ARIA Authoring Practices Guide (APG)
 * Includes: SC 2.5.8 Target Size (Minimum) with exceptions, Keyboard Operability,
 * Focus Trap & Restoration, Accessible Names, and ARIA Live Regions.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

describe('Accessibility Verification: WCAG 2.2 AA Aligned', () => {
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

  test('WCAG 2.2 SC 2.5.8: Target Size (Minimum 24x24 CSS px) with legitimate exceptions', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('header.site-header').waitFor({ state: 'visible' });

      // Evaluate interactive controls on page
      const audit = await page.evaluate(() => {
        const controls = Array.from(document.querySelectorAll('button, a, input, select, textarea'));
        const results = [];

        for (const el of controls) {
          // Ignore hidden elements (e.g. collapsed menu or skip link when not focused)
          const rect = el.getBoundingClientRect();
          if (rect.width === 0 && rect.height === 0) continue;
          if (window.getComputedStyle(el).display === 'none') continue;
          if (window.getComputedStyle(el).visibility === 'hidden') continue;

          const isInline = window.getComputedStyle(el).display === 'inline' ||
            el.closest('p, span, li:not(.nav-item)');

          // Check dimension
          const meetsSize = rect.width >= 24 && rect.height >= 24;

          results.push({
            tag: el.tagName.toLowerCase(),
            text: (el.textContent || el.getAttribute('aria-label') || '').trim().slice(0, 30),
            width: Math.round(rect.width),
            height: Math.round(rect.height),
            isInline: Boolean(isInline),
            meetsSize
          });
        }

        return results;
      });

      const violations = [];
      for (const item of audit) {
        if (!item.meetsSize) {
          // If not meeting 24x24, check if it qualifies under the SC 2.5.8 Inline Exception
          if (item.isInline) {
            // Legitimate WCAG 2.2 Exception: Target is inline within a sentence/block of text
            continue;
          }
          violations.push(`${item.tag} ("${item.text}") is ${item.width}x${item.height}px (< 24x24px)`);
        }
      }

      assert.strictEqual(
        violations.length,
        0,
        `WCAG 2.2 SC 2.5.8 Target Size violations detected:\n${violations.join('\n')}`
      );

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during target size audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('WCAG 2.2 SC 2.4.1 & 2.4.7: Accessible Skip Link and Visible Focus Ring', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('header.site-header').waitFor({ state: 'visible' });

      // First Tab must reveal skip link
      await page.keyboard.press('Tab');
      // Allow CSS top transition (0.2s) to complete
      await page.waitForTimeout(300);

      const skipLink = page.locator('a.skip-link');
      const isSkipFocused = await page.evaluate(() => {
        return document.activeElement === document.querySelector('a.skip-link');
      });
      assert.strictEqual(isSkipFocused, true, 'Initial Tab key press must focus the skip link');

      // Verify skip link becomes visually positioned within viewport on focus
      const skipRect = await skipLink.boundingBox();
      assert.ok(skipRect && skipRect.y >= 0, `Skip link must become visible on focus (y: ${skipRect?.y})`);

      // Verify focus-visible styling (must have outline or box-shadow)
      const focusStyles = await page.evaluate(() => {
        const el = document.querySelector('a.skip-link');
        const s = window.getComputedStyle(el);
        return {
          outlineStyle: s.outlineStyle,
          outlineWidth: s.outlineWidth,
          boxShadow: s.boxShadow
        };
      });
      const hasVisibleRing = focusStyles.outlineStyle !== 'none' || focusStyles.boxShadow !== 'none';
      assert.strictEqual(hasVisibleRing, true, 'Skip link must possess visible focus ring (:focus-visible)');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors in skip link verification:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('WCAG 2.2 SC 4.1.2: All interactive controls possess non-empty accessible names', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('header.site-header').waitFor({ state: 'visible' });

      const unnamedControls = await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button, a'));
        const missing = [];

        for (const el of buttons) {
          // Visible text
          const text = (el.textContent || '').trim();
          const ariaLabel = el.getAttribute('aria-label');
          const ariaLabelledBy = el.getAttribute('aria-labelledby');

          const hasName = Boolean(text || ariaLabel || ariaLabelledBy);
          if (!hasName) {
            missing.push({
              tag: el.tagName.toLowerCase(),
              className: el.className,
              html: el.outerHTML.slice(0, 80)
            });
          }
        }
        return missing;
      });

      assert.strictEqual(
        unnamedControls.length,
        0,
        `Found interactive elements without accessible name:\n${JSON.stringify(unnamedControls, null, 2)}`
      );

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors in accessible names audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('WCAG 2.2 SC 4.1.3: Status messages container configured as polite live region', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#toastContainer').waitFor({ state: 'attached' });

      const liveRegion = page.locator('#toastContainer');
      assert.strictEqual(await liveRegion.getAttribute('role'), 'status', 'Toast container must have role="status"');
      assert.strictEqual(await liveRegion.getAttribute('aria-live'), 'polite', 'Toast container must have aria-live="polite"');
      assert.strictEqual(await liveRegion.getAttribute('aria-atomic'), 'true', 'Toast container must have aria-atomic="true"');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors in ARIA live region audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('WCAG 2.2 Form Accessibility: Explicit labels, aria-describedby, and semantic alerts on Auth & Profile', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      // 1. Audit login.html
      await page.goto(`${baseUrl}/login.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#loginForm');

      const loginInputs = ['#loginEmail', '#loginPassword'];
      for (const inputSel of loginInputs) {
        const inputId = inputSel.slice(1);
        const label = page.locator(`label[for="${inputId}"]`);
        assert.ok(await label.isVisible(), `Input ${inputSel} must have visible matching label[for="${inputId}"]`);
        const labelText = await label.textContent();
        assert.ok(labelText.trim().length > 0, `Label for ${inputSel} must have non-empty text`);

        const ariaDescribedBy = await page.getAttribute(inputSel, 'aria-describedby');
        assert.ok(ariaDescribedBy, `Input ${inputSel} must specify aria-describedby for error/help messages`);
      }

      // 2. Audit register.html
      await page.goto(`${baseUrl}/register.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#registerForm');

      const registerInputs = ['#regFullName', '#regEmail', '#regPassword'];
      for (const inputSel of registerInputs) {
        const inputId = inputSel.slice(1);
        const label = page.locator(`label[for="${inputId}"]`);
        assert.ok(await label.isVisible(), `Input ${inputSel} must have visible matching label[for="${inputId}"]`);
        const labelText = await label.textContent();
        assert.ok(labelText.trim().length > 0, `Label for ${inputSel} must have non-empty text`);

        const ariaDescribedBy = await page.getAttribute(inputSel, 'aria-describedby');
        assert.ok(ariaDescribedBy, `Input ${inputSel} must specify aria-describedby`);
      }

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors in auth a11y audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('WCAG 2.2 Radicals Catalog Accessibility: Target size on cards, search labels, and dialog semantics', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      // Mock /radicals API with sample 20 radicals
      await page.route('**/api/v1/radicals*', async (route) => {
        const sample = [];
        for (let i = 1; i <= 24; i++) {
          sample.push({
            radicalId: i,
            character: String.fromCodePoint(0x4E00 + i - 1),
            pinyin: `pinyin_${i}`,
            meaningHanViet: `Hán-Việt ${i}`,
            meaningVi: `Nghĩa ${i}`
          });
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: { page: 0, size: 24, totalElements: 24, totalPages: 1, items: sample }
          })
        });
      });

      await page.goto(`${baseUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('.radical-card');

      // 1. Audit Radical Cards Touch Target Size (WCAG 2.2 SC 2.5.8 >= 24x24 px)
      const cardSizes = await page.evaluate(() => {
        const cards = Array.from(document.querySelectorAll('.radical-card'));
        return cards.map(c => {
          const rect = c.getBoundingClientRect();
          return { width: rect.width, height: rect.height };
        });
      });

      assert.ok(cardSizes.length >= 24, 'Must evaluate at least 24 cards');
      for (const sz of cardSizes) {
        assert.ok(sz.width >= 44 && sz.height >= 44, `Card dimensions ${sz.width}x${sz.height} must satisfy >= 44x44px ergonomic touch target`);
      }

      // 2. Audit Dialog Semantics (aria-labelledby)
      const dialog = page.locator('#radicalDetailModal');
      assert.strictEqual(await dialog.getAttribute('aria-labelledby'), 'modalRadicalTitle');

      // 3. Audit Search Input Accessible Label
      const searchInput = page.locator('#radicalSearchInput');
      const searchLabel = page.locator('label[for="radicalSearchInput"]');
      assert.ok(await searchLabel.count() > 0, 'Search input must have associated label');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors in radicals a11y audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('WCAG 2.2 Verification Harness Accessibility: Target size, accessible names, and landmarks on ui-verification.html', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('header.site-header').waitFor({ state: 'visible' });

      // 1. Audit Target Size on Verification Page (WCAG 2.2 SC 2.5.8)
      const targetViolations = await page.evaluate(() => {
        const controls = Array.from(document.querySelectorAll('button, a, input, select, textarea'));
        const violations = [];

        for (const el of controls) {
          const rect = el.getBoundingClientRect();
          if (rect.width === 0 && rect.height === 0) continue;
          if (window.getComputedStyle(el).display === 'none') continue;
          if (window.getComputedStyle(el).visibility === 'hidden') continue;

          const isInline = window.getComputedStyle(el).display === 'inline' ||
            el.closest('p, span, li:not(.nav-item)');

          const meetsSize = rect.width >= 24 && rect.height >= 24;
          if (!meetsSize && !isInline) {
            violations.push(`${el.tagName.toLowerCase()} ("${(el.textContent || '').trim().slice(0, 30)}") is ${Math.round(rect.width)}x${Math.round(rect.height)}px (< 24x24px)`);
          }
        }
        return violations;
      });

      assert.strictEqual(
        targetViolations.length,
        0,
        `WCAG 2.2 SC 2.5.8 Target Size violations on ui-verification.html:\n${targetViolations.join('\n')}`
      );

      // 2. Audit Accessible Names for all verification buttons
      const verificationButtons = [
        '#triggerToastSuccess',
        '#triggerToastWarning',
        '#triggerToastDanger',
        '#triggerToastInfo',
        '#triggerModalDemo',
        '#btnStateLoading',
        '#btnStateEmpty',
        '#btnStateError',
        '#btnStateReady'
      ];

      for (const btnSel of verificationButtons) {
        const btn = page.locator(btnSel);
        assert.ok(await btn.count() > 0, `Button ${btnSel} must exist on ui-verification.html`);
        const text = await btn.textContent();
        assert.ok(text.trim().length > 0, `Button ${btnSel} must have non-empty accessible name`);
      }

      // 3. Status messages container configured as polite live region
      const liveRegion = page.locator('#toastContainer');
      assert.strictEqual(await liveRegion.getAttribute('role'), 'status', 'Toast container must have role="status"');
      assert.strictEqual(await liveRegion.getAttribute('aria-live'), 'polite', 'Toast container must have aria-live="polite"');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during verification harness a11y audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('WCAG 2.2 Vocabulary Catalog Accessibility: Target size on cards, search input label, and dialog semantics', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      // Mock /vocabulary API with sample 20 items
      await page.route('**/api/v1/vocabulary*', async (route) => {
        const sample = [];
        for (let i = 1; i <= 20; i++) {
          sample.push({
            vocabId: i,
            hanzi: String.fromCodePoint(0x4E00 + i - 1),
            pinyin: `pinyin_${i}`,
            meaningHanViet: `Hán-Việt ${i}`,
            meaningVi: `Nghĩa ${i}`
          });
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: { page: 0, size: 20, totalElements: 20, totalPages: 1, first: true, last: true, items: sample }
          })
        });
      });

      await page.goto(`${baseUrl}/vocabulary.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('.vocab-card');

      // 1. Audit Vocabulary Cards Touch Target Size (WCAG 2.2 SC 2.5.8 >= 24x24 px)
      const cardSizes = await page.evaluate(() => {
        const cards = Array.from(document.querySelectorAll('.vocab-card'));
        return cards.map(c => {
          const rect = c.getBoundingClientRect();
          return { width: rect.width, height: rect.height };
        });
      });

      assert.ok(cardSizes.length >= 20, 'Must evaluate at least 20 cards');
      for (const sz of cardSizes) {
        assert.ok(sz.width >= 44 && sz.height >= 44, `Card dimensions ${sz.width}x${sz.height} must satisfy >= 44x44px ergonomic touch target`);
      }

      // 2. Audit Dialog Semantics (aria-labelledby)
      const dialog = page.locator('#vocabDetailModal');
      assert.strictEqual(await dialog.getAttribute('aria-labelledby'), 'modalVocabTitle');

      // 3. Audit Search Input Accessible Label
      const searchInput = page.locator('#vocabSearchInput');
      const searchLabel = page.locator('label[for="vocabSearchInput"]');
      assert.ok(await searchLabel.count() > 0, 'Search input must have associated label');

      // 4. Audit Speech buttons accessible name
      const speechButtons = await page.locator('.vocab-speech-btn').all();
      for (const btn of speechButtons) {
        const ariaLabel = await btn.getAttribute('aria-label');
        assert.ok(ariaLabel && ariaLabel.trim().length > 0, 'Speech button must have accessible aria-label');
      }

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors in vocabulary a11y audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('WCAG 2.2 Public Lessons Catalog Accessibility: Target size on cards, page size select label, live status announcer, and pagination aria-current', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      // Mock /api/v1/lessons endpoint with multi-page response to verify pagination controls
      await page.route('**/api/v1/lessons**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              page: 0,
              size: 20,
              totalElements: 25,
              totalPages: 2,
              first: true,
              last: false,
              items: [
                { lessonId: 1, title: 'Bài 1: Xin Chào', status: 'Approved', vocabularyCount: 10, createdAt: '2026-09-01T08:00:00Z', updatedAt: '2026-09-02T09:00:00Z' },
                { lessonId: 2, title: 'Bài 2: Gia Đình', status: 'Approved', vocabularyCount: 15, createdAt: '2026-09-03T08:00:00Z', updatedAt: '2026-09-04T09:00:00Z' },
                { lessonId: 3, title: 'Bài 3: Mua Sắm', status: 'Approved', vocabularyCount: 12, createdAt: '2026-09-05T08:00:00Z', updatedAt: '2026-09-06T09:00:00Z' }
              ]
            }
          })
        });
      });

      await page.goto(`${baseUrl}/lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('.lesson-card');

      // 1. Audit Lesson Cards Touch Target Size (WCAG 2.2 SC 2.5.8 >= 44x44 px ergonomic)
      const cardSizes = await page.evaluate(() => {
        const cards = Array.from(document.querySelectorAll('.lesson-card'));
        return cards.map(c => {
          const rect = c.getBoundingClientRect();
          return { width: rect.width, height: rect.height };
        });
      });

      assert.strictEqual(cardSizes.length, 3, 'Must evaluate 3 lesson cards');
      for (const sz of cardSizes) {
        assert.ok(sz.width >= 44 && sz.height >= 44, `Card dimensions ${sz.width}x${sz.height} must satisfy >= 44x44px`);
      }

      // 2. Audit Page Size Select Label
      const pageSizeSelect = page.locator('#pageSizeSelect');
      const pageSizeLabel = page.locator('label[for="pageSizeSelect"]');
      assert.ok(await pageSizeLabel.count() > 0, 'Page size select must have associated <label>');

      // 3. Audit Live Status Announcer (aria-live="polite")
      const liveAnnouncer = page.locator('#lessonsResultCount');
      assert.strictEqual(await liveAnnouncer.getAttribute('aria-live'), 'polite', 'Live status announcer must have aria-live="polite"');

      // 4. Audit Pagination semantics (aria-current="page")
      const currentPageBtn = page.locator('.page-link[aria-current="page"]');
      assert.strictEqual(await currentPageBtn.count(), 1, 'Current page must have aria-current="page"');

      // 5. Audit semantic <article> cards
      const articleCards = page.locator('article.lesson-card');
      assert.strictEqual(await articleCards.count(), 3, 'Cards must use semantic <article>');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors in lessons catalog a11y audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('WCAG 2.2 Public Lesson Detail Accessibility: Breadcrumb navigation landmark, semantic ordered list, and speech buttons', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.route('**/api/v1/lessons/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              lessonId: 1,
              title: 'Bài 1: Chào Hỏi Cơ Bản',
              status: 'Approved',
              vocabularyCount: 2,
              createdAt: '2026-09-01T08:00:00Z',
              updatedAt: '2026-09-02T10:00:00Z',
              vocabularies: [
                { vocabId: 10, orderIndex: 1, hanzi: '你', pinyin: 'nǐ', meaningHanViet: 'Nhĩ', meaningVi: 'Bạn' },
                { vocabId: 11, orderIndex: 2, hanzi: '好', pinyin: 'hǎo', meaningHanViet: 'Hảo', meaningVi: 'Tốt' }
              ]
            }
          })
        });
      });

      await page.goto(`${baseUrl}/lesson-detail.html?id=1`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#lessonVocabList');

      // 1. Audit Breadcrumb Navigation Landmark
      const breadcrumbNav = page.locator('nav[aria-label="Đường dẫn bài học"]');
      assert.strictEqual(await breadcrumbNav.count(), 1, 'Must have accessible breadcrumb navigation landmark');

      const breadcrumbCurrent = page.locator('#breadcrumbLessonTitle[aria-current="page"]');
      assert.strictEqual(await breadcrumbCurrent.count(), 1, 'Current breadcrumb item must have aria-current="page"');

      // 2. Audit Semantic Ordered List
      const vocabList = page.locator('ol#lessonVocabList');
      assert.strictEqual(await vocabList.count(), 1, 'Vocabularies must be inside semantic <ol>');

      const listItems = page.locator('ol#lessonVocabList > li.lesson-vocab-item');
      assert.strictEqual(await listItems.count(), 2, 'Must render 2 semantic <li> items');

      // 3. Audit Single H1
      const h1Count = await page.locator('h1').count();
      assert.strictEqual(h1Count, 1, 'Page must have exactly one single H1');

      // 4. Audit Speech Button Accessibility
      const speechButtons = page.locator('.vocab-speech-btn');
      const count = await speechButtons.count();
      assert.strictEqual(count, 2, 'Must have 2 speech buttons');
      for (let i = 0; i < count; i++) {
        const ariaLabel = await speechButtons.nth(i).getAttribute('aria-label');
        assert.ok(ariaLabel && ariaLabel.trim().length > 0, 'Speech button must have accessible aria-label');
      }

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors in lesson detail a11y audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('WCAG 2.2 Contextual Personal Notes Modal Accessibility: Accessible dialog name, explicit textarea label, live char counter, and status region', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.addInitScript(() => {
        localStorage.setItem('access_token', 'test_jwt_notes_user');
        localStorage.setItem('user_info', JSON.stringify({
          accountId: 999,
          emailOrPhone: 'notes_tester@example.com',
          fullName: 'Học Viên Ghi Chú',
          roles: ['Learner']
        }));
      });

      await page.route(/\/api\/v1\/vocabulary(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              items: [{
                vocabId: 1,
                hanzi: '书',
                pinyin: 'shū',
                meaningHanViet: 'Thư',
                meaningVi: 'Sách, vở',
                audioUrl: null
              }],
              page: 0,
              size: 20,
              totalElements: 1,
              totalPages: 1
            }
          })
        });
      });

      await page.route('**/api/v1/vocabularies/1/notes*', async (route) => {
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

      await page.goto(`${baseUrl}/vocabulary.html`, { waitUntil: 'domcontentloaded' });
      const noteBtn = page.locator('.vocab-card-note-btn').first();
      await noteBtn.waitFor({ state: 'visible' });
      await noteBtn.click();

      const modal = page.locator('#notesModal');
      await modal.waitFor({ state: 'visible' });

      // 1. Accessible Dialog Name (SC 4.1.2)
      const labelledBy = await modal.getAttribute('aria-labelledby');
      assert.strictEqual(labelledBy, 'notesModalTitle', 'Modal must have aria-labelledby="notesModalTitle"');
      const titleEl = page.locator('#notesModalTitle');
      assert.ok(await titleEl.isVisible(), 'Dialog title element must be visible');
      assert.ok((await titleEl.textContent()).includes('Ghi Chú Cá Nhân'));

      // 2. Explicit Textarea Label (SC 1.3.1 & 4.1.2)
      const label = page.locator('label[for="noteContentInput"]');
      assert.strictEqual(await label.count(), 1, 'Textarea must have an explicit <label for="noteContentInput">');
      assert.ok((await label.textContent()).includes('Nội dung ghi chú'));

      // 3. Live Character Counter (SC 4.1.3 Status Messages)
      const counter = page.locator('#noteCharCounter');
      assert.strictEqual(await counter.getAttribute('aria-live'), 'polite', 'Char counter must have aria-live="polite"');

      // 4. Status Message Live Region (SC 4.1.3)
      const statusMsg = page.locator('#noteFeedback');
      assert.strictEqual(await statusMsg.getAttribute('role'), 'status', 'Status message must have role="status"');
      assert.strictEqual(await statusMsg.getAttribute('aria-live'), 'polite', 'Status message must have aria-live="polite"');

      // 5. Accessible Close Button (SC 4.1.2)
      const closeBtn = page.locator('#notesModalCloseBtn');
      const closeLabel = await closeBtn.getAttribute('aria-label');
      assert.ok(closeLabel && closeLabel.includes('Đóng'), 'Close button must have descriptive aria-label');

      // 6. Target Size Check (SC 2.5.8)
      const btnBox = await closeBtn.boundingBox();
      assert.ok(btnBox.width >= 24 && btnBox.height >= 24, 'Close button meets minimum target size');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors in notes modal a11y audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('WCAG 2.2 Interactive SRS Review Session & 3D Flashcard Accessibility', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.addInitScript(() => {
        localStorage.setItem('access_token', 'test_a11y_jwt');
        localStorage.setItem('user_info', JSON.stringify({
          accountId: 999,
          emailOrPhone: 'a11y_srs@example.com',
          fullName: 'A11y Learner',
          roles: ['Learner']
        }));
      });

      await page.route('**/api/v1/srs/due', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: [{
              itemType: 'VOCABULARY',
              itemId: 1,
              hanzi: '学',
              pinyin: 'xué',
              meaningHanViet: 'Học',
              meaningVi: 'Học tập',
              exampleSentence: '我在大学学中文。',
              exampleTranslation: 'Tôi học tiếng Trung ở trường đại học.',
              repetitions: 1,
              easeFactor: 2.50,
              intervalDays: 1,
              isNew: false
            }]
          })
        });
      });

      await page.goto(`${baseUrl}/srs-review.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#srsActiveSession').waitFor({ state: 'visible', timeout: 5000 });

      // 1. Flashcard Semantics & Keyboard Focus
      const flashcard = page.locator('#flashcard');
      assert.strictEqual(await flashcard.getAttribute('role'), 'button');
      assert.strictEqual(await flashcard.getAttribute('tabindex'), '0');
      const ariaLabel = await flashcard.getAttribute('aria-label');
      assert.ok(ariaLabel && ariaLabel.includes('Lật thẻ') || ariaLabel.includes('lật thẻ'));
      assert.strictEqual(await flashcard.getAttribute('aria-expanded'), 'false');

      // 2. Face aria-hidden coordination
      const cardFront = page.locator('#cardFront');
      const cardBack = page.locator('#cardBack');
      assert.strictEqual(await cardFront.getAttribute('aria-hidden'), 'false');
      assert.strictEqual(await cardBack.getAttribute('aria-hidden'), 'true');

      // Flip card
      await flashcard.click();
      await page.waitForTimeout(100);
      assert.strictEqual(await flashcard.getAttribute('aria-expanded'), 'true');
      assert.strictEqual(await cardFront.getAttribute('aria-hidden'), 'true');
      assert.strictEqual(await cardBack.getAttribute('aria-hidden'), 'false');

      // 3. Audio Button Accessible Name
      const audioBtn = page.locator('#btnAudioPronounceFront');
      const audioLabel = await audioBtn.getAttribute('aria-label');
      assert.ok(audioLabel && audioLabel.includes('Phát âm'));

      // 4. Rating Buttons Semantics & Target Size (SC 2.5.8 >= 24x24, target >= 44x44)
      const ratingButtons = [
        { id: '#btnRatingAgain', labelPart: 'Học lại' },
        { id: '#btnRatingHard', labelPart: 'Khó' },
        { id: '#btnRatingGood', labelPart: 'Tốt' },
        { id: '#btnRatingEasy', labelPart: 'Dễ' }
      ];

      for (const item of ratingButtons) {
        const btn = page.locator(item.id);
        assert.ok(await btn.isVisible(), `Button ${item.id} must be visible`);
        const btnAriaLabel = await btn.getAttribute('aria-label');
        assert.ok(btnAriaLabel && btnAriaLabel.includes(item.labelPart), `${item.id} must have descriptive accessible name`);

        const box = await btn.boundingBox();
        assert.ok(box.width >= 44 && box.height >= 44, `${item.id} should meet usability target size >= 44x44px (got ${box.width}x${box.height})`);
      }

      // 5. Status Feedback Live Region (SC 4.1.3)
      const feedback = page.locator('#ratingFeedback');
      assert.strictEqual(await feedback.getAttribute('role'), 'status');
      assert.strictEqual(await feedback.getAttribute('aria-live'), 'polite');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors in srs review a11y audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  test('WCAG 2.2 SRS Dashboard & Daily Settings Form Accessibility', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.addInitScript(() => {
        localStorage.setItem('access_token', 'test_a11y_jwt');
        localStorage.setItem('user_info', JSON.stringify({
          accountId: 9999,
          emailOrPhone: 'a11y_user@example.com',
          fullName: 'A11y Auditor',
          roles: ['Learner']
        }));
      });

      await page.route('**/api/v1/srs/stats', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: { cardsDue: 5, reviewsToday: 10, newCardsToday: 3, newCardsLimit: 20, maxReviewLimit: 100 }
          })
        });
      });

      await page.route('**/api/v1/srs/settings', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: { settingId: 101, newCardsPerDay: 20, maxReviewPerDay: 100 }
          })
        });
      });

      await page.goto(`${baseUrl}/srs-dashboard.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#dashboardContentSection').waitFor({ state: 'visible' });

      // 1. Inputs have associated labels & aria-describedby
      const newCardsInput = page.locator('#newCardsPerDayInput');
      const maxReviewInput = page.locator('#maxReviewPerDayInput');
      assert.ok(await newCardsInput.getAttribute('aria-describedby'), 'newCardsPerDayInput must have aria-describedby');
      assert.ok(await maxReviewInput.getAttribute('aria-describedby'), 'maxReviewPerDayInput must have aria-describedby');

      // 2. Button Target Sizes (SC 2.5.8 >= 24x24 px)
      const saveBtn = page.locator('#btnSaveSettings');
      const resetBtn = page.locator('#btnResetSettings');
      const saveBox = await saveBtn.boundingBox();
      const resetBox = await resetBtn.boundingBox();
      assert.ok(saveBox.width >= 44 && saveBox.height >= 38, 'Save button must meet target size');
      assert.ok(resetBox.width >= 44 && resetBox.height >= 38, 'Reset button must meet target size');

      // 3. Live Regions (SC 4.1.3)
      const formStatus = page.locator('#settingsFormStatus');
      assert.strictEqual(await formStatus.getAttribute('role'), 'status');
      assert.strictEqual(await formStatus.getAttribute('aria-live'), 'polite');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during srs dashboard a11y audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // WCAG 2.2 Creator Lesson Studio & Vocabulary Reordering Accessibility (Task 9D.1)
  test('WCAG 2.2 Creator Lesson Studio & Vocabulary Reordering Accessibility (Task 9D.1)', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.addInitScript(`
        localStorage.setItem('access_token', 'test_creator_jwt');
        localStorage.setItem('user_info', JSON.stringify({
          accountId: 2001,
          fullName: 'Tác Giả A11y',
          roles: ['Creator']
        }));
      `);

      const mockLesson = {
        lessonId: 101,
        title: 'HSK 1 — Bài 1: Chào hỏi',
        status: 'Draft',
        vocabularyCount: 2,
        vocabularies: [
          { vocabId: 1, hanzi: '你', pinyin: 'nǐ', meaningVi: 'Bạn', orderIndex: 1 },
          { vocabId: 2, hanzi: '好', pinyin: 'hǎo', meaningVi: 'Tốt', orderIndex: 2 }
        ],
        createdAt: '2026-09-01T08:00:00Z',
        updatedAt: '2026-09-02T10:00:00Z'
      };

      await page.route('**/api/v1/creator/lessons/101', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: mockLesson })
        });
      });

      await page.goto(`${baseUrl}/creator-lesson-editor.html?id=101`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#editorVocabList li');

      // 1. Semantic ordered list structure (<ol>)
      const listTag = await page.locator('#editorVocabList').evaluate(el => el.tagName.toLowerCase());
      assert.strictEqual(listTag, 'ol', 'Vocabulary list must use semantic <ol> ordered list element');

      // 2. Reorder buttons target size (WCAG 2.2 SC 2.5.8 >= 24x24 px)
      const upBtns = page.locator('.btn-move-up');
      const downBtns = page.locator('.btn-move-down');
      const removeBtns = page.locator('.creator-remove-btn');

      const secondUpBtn = upBtns.nth(1);
      const secondUpBox = await secondUpBtn.boundingBox();
      assert.ok(secondUpBox.width >= 24 && secondUpBox.height >= 24, 'Move Up button must satisfy SC 2.5.8 (>= 24x24 px)');

      const firstDownBtn = downBtns.first();
      const firstDownBox = await firstDownBtn.boundingBox();
      assert.ok(firstDownBox.width >= 24 && firstDownBox.height >= 24, 'Move Down button must satisfy SC 2.5.8 (>= 24x24 px)');

      // 3. Non-empty accessible names (WCAG 2.2 SC 4.1.2)
      assert.ok(await secondUpBtn.getAttribute('aria-label'), 'Move Up button must possess non-empty aria-label');
      assert.ok(await firstDownBtn.getAttribute('aria-label'), 'Move Down button must possess non-empty aria-label');
      assert.ok(await removeBtns.first().getAttribute('aria-label'), 'Remove button must possess non-empty aria-label');

      // 4. Status announcement live region (WCAG 2.2 SC 4.1.3)
      const liveStatus = page.locator('#reorderLiveStatus');
      assert.strictEqual(await liveStatus.getAttribute('role'), 'status', 'Reorder live region must have role="status"');
      assert.strictEqual(await liveStatus.getAttribute('aria-live'), 'polite', 'Reorder live region must be polite');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during creator studio a11y audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // WCAG 2.2 SC 2.4.11: Focus Not Obscured (Minimum) across responsive viewports
  test('WCAG 2.2 SC 2.4.11: Focus Not Obscured (Minimum) across 375px, 768px, and 1200px viewports', async () => {
    const viewports = [
      { name: 'Mobile (375px)', width: 375, height: 667 },
      { name: 'Tablet (768px)', width: 768, height: 1024 },
      { name: 'Desktop (1200px)', width: 1200, height: 800 }
    ];

    for (const vp of viewports) {
      const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
        viewportWidth: vp.width,
        viewportHeight: vp.height
      });

      try {
        await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
        await page.locator('header.site-header').waitFor({ state: 'visible' });

        // Tab sequentially through interactive controls
        for (let i = 0; i < 6; i++) {
          await page.keyboard.press('Tab');
          await page.waitForTimeout(100);

          const obscuredCheck = await page.evaluate(() => {
            const active = document.activeElement;
            if (!active || active === document.body) return { obscured: false };

            const header = document.querySelector('header.site-header');
            if (!header) return { obscured: false };

            // Elements inside header itself (e.g. skip link, navbar brand) are not obscured by the header
            if (header.contains(active)) return { obscured: false };

            const activeRect = active.getBoundingClientRect();
            const headerRect = header.getBoundingClientRect();
            // A control is completely obscured if activeRect lies entirely behind headerRect
            const isCompletelyObscured = (
              activeRect.bottom <= headerRect.bottom &&
              activeRect.top >= headerRect.top &&
              activeRect.left >= headerRect.left &&
              activeRect.right <= headerRect.right
            );

            return {
              tag: active.tagName.toLowerCase(),
              className: active.className,
              top: activeRect.top,
              bottom: activeRect.bottom,
              headerBottom: headerRect.bottom,
              obscured: isCompletelyObscured
            };
          });

          assert.strictEqual(
            obscuredCheck.obscured,
            false,
            `Focused element <${obscuredCheck.tag}> is completely obscured by header in ${vp.name}!`
          );
        }

        const errors = getRuntimeErrors();
        assert.strictEqual(errors.length, 0, `Runtime errors during SC 2.4.11 check on ${vp.name}:\n${errors.join('\n')}`);
      } finally {
        await context.close();
      }
    }
  });

  // WCAG 2.2 SC 3.3.8: Accessible Authentication (Cognitive Assistance, Autofill & Paste)
  test('WCAG 2.2 SC 3.3.8: Accessible Authentication (paste enabled, autocomplete, visible labels)', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      // 1. Audit login.html
      await page.goto(`${baseUrl}/login.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#loginForm').waitFor({ state: 'visible' });

      // Check autocomplete attributes
      const loginEmailAc = await page.locator('#loginEmail').getAttribute('autocomplete');
      assert.strictEqual(loginEmailAc, 'username', 'Login email/phone input must have autocomplete="username"');

      const loginPasswordAc = await page.locator('#loginPassword').getAttribute('autocomplete');
      assert.strictEqual(loginPasswordAc, 'current-password', 'Login password input must have autocomplete="current-password"');

      // Verify paste is not blocked on password field
      const loginPasteAllowed = await page.evaluate(() => {
        const input = document.getElementById('loginPassword');
        input.focus();
        const event = new ClipboardEvent('paste', {
          bubbles: true,
          cancelable: true,
          clipboardData: new DataTransfer()
        });
        const dispatched = input.dispatchEvent(event);
        return dispatched && !event.defaultPrevented;
      });
      assert.strictEqual(loginPasteAllowed, true, 'Paste operation must not be prevented on login password input');

      // 2. Audit register.html
      await page.goto(`${baseUrl}/register.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#registerForm').waitFor({ state: 'visible' });

      const regNameAc = await page.locator('#regFullName').getAttribute('autocomplete');
      assert.strictEqual(regNameAc, 'name', 'Register name input must have autocomplete="name"');

      const regEmailAc = await page.locator('#regEmail').getAttribute('autocomplete');
      assert.strictEqual(regEmailAc, 'username', 'Register email input must have autocomplete="username"');

      const regPasswordAc = await page.locator('#regPassword').getAttribute('autocomplete');
      assert.strictEqual(regPasswordAc, 'new-password', 'Register password input must have autocomplete="new-password"');

      // Verify paste is not blocked on register password field
      const regPasteAllowed = await page.evaluate(() => {
        const input = document.getElementById('regPassword');
        input.focus();
        const event = new ClipboardEvent('paste', {
          bubbles: true,
          cancelable: true,
          clipboardData: new DataTransfer()
        });
        const dispatched = input.dispatchEvent(event);
        return dispatched && !event.defaultPrevented;
      });
      assert.strictEqual(regPasteAllowed, true, 'Paste operation must not be prevented on register password input');

      // 3. Audit profile.html (with authenticated session)
      await page.addInitScript(() => {
        localStorage.setItem('access_token', 'test_a11y_jwt');
        localStorage.setItem('user_info', JSON.stringify({
          accountId: 888,
          emailOrPhone: 'tester@example.com',
          fullName: 'Người Dùng Kiểm Thử',
          roles: ['Learner']
        }));
      });

      await page.route('**/api/v1/users/profile', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: {
              userId: 888,
              accountId: 888,
              fullName: 'Người Dùng Kiểm Thử',
              emailOrPhone: 'tester@example.com',
              avatarUrl: null,
              createdAt: '2026-09-01T08:00:00Z'
            }
          })
        });
      });

      await page.goto(`${baseUrl}/profile.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#profileFullNameInput');
      const profileNameAc = await page.locator('#profileFullNameInput').getAttribute('autocomplete');
      assert.strictEqual(profileNameAc, 'name', 'Profile full name input must have autocomplete="name"');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during SC 3.3.8 audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // WCAG 2.2 APG Modal Dialog Focus Lifecycle: Entry, Trap, Escape, Focus Restoration
  test('WCAG 2.2 Dialog Focus Lifecycle: Entry, Modal Trap, Escape Key, Focus Restoration', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      // Mock radicals API
      await page.route('**/api/v1/radicals*', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              page: 0,
              size: 5,
              totalElements: 5,
              totalPages: 1,
              items: [
                { radicalId: 1, character: '一', pinyin: 'yī', meaningHanViet: 'Nhất', meaningVi: 'Số một' },
                { radicalId: 2, character: '丨', pinyin: 'gǔn', meaningHanViet: 'Cổn', meaningVi: 'Nét sổ dọc' }
              ]
            }
          })
        });
      });

      await page.goto(`${baseUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('.radical-card');

      const firstCard = page.locator('.radical-card').first();
      await firstCard.focus();

      // 1. Focus starts on the opener card
      const isCardFocusedBefore = await page.evaluate(() => {
        return document.activeElement === document.querySelector('.radical-card');
      });
      assert.strictEqual(isCardFocusedBefore, true, 'Opener card must be focused before activating modal');

      // 2. Open modal via click
      await firstCard.click();
      const dialog = page.locator('#radicalDetailModal');
      await dialog.waitFor({ state: 'visible' });
      assert.strictEqual(await dialog.evaluate(el => el.hasAttribute('open')), true, 'Dialog must have open attribute');

      // 3. Focus enters dialog
      const isFocusInsideDialog = await page.evaluate(() => {
        const dlg = document.querySelector('#radicalDetailModal');
        return dlg && dlg.contains(document.activeElement);
      });
      assert.strictEqual(isFocusInsideDialog, true, 'Focus must move inside the modal dialog on open');

      // 4. Tab key trapped inside dialog (cycles between focusable elements)
      for (let i = 0; i < 4; i++) {
        await page.keyboard.press('Tab');
        await page.waitForTimeout(50);
        const remainsInside = await page.evaluate(() => {
          const dlg = document.querySelector('#radicalDetailModal');
          return dlg && dlg.contains(document.activeElement);
        });
        assert.strictEqual(remainsInside, true, `Tab navigation (${i + 1}) must stay trapped inside dialog`);
      }

      // 5. Escape key closes dialog
      await page.keyboard.press('Escape');
      await page.waitForTimeout(200);
      const isClosed = await dialog.evaluate(el => !el.hasAttribute('open'));
      assert.strictEqual(isClosed, true, 'Escape key must close the modal dialog');

      // 6. Focus restores to the invoking opener element
      const isFocusRestored = await page.evaluate(() => {
        return document.activeElement === document.querySelector('.radical-card');
      });
      assert.strictEqual(isFocusRestored, true, 'Focus must be restored to opener element after dialog dismissal');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during dialog lifecycle audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // WCAG 2.2 Table Semantics & Relationships (SC 1.3.1 Info and Relationships)
  test('WCAG 2.2 Table Semantics: Semantic captions, header scope="col", and scrollable responsive containers', async () => {
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
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.route('**/api/v1/**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              page: 0,
              size: 20,
              totalElements: 0,
              totalPages: 0,
              first: true,
              last: true,
              items: []
            }
          })
        });
      });

      await page.addInitScript(() => {
        localStorage.setItem('access_token', 'test_admin_jwt');
        localStorage.setItem('user_info', JSON.stringify({
          accountId: 1,
          emailOrPhone: 'admin@example.com',
          fullName: 'Quản Trị Viên',
          roles: ['Admin', 'Creator', 'Moderator', 'Learner']
        }));
      });

      for (const pageName of tablePages) {
        await page.goto(`${baseUrl}/${pageName}`, { waitUntil: 'domcontentloaded' });

        const tableAudit = await page.evaluate(() => {
          const tables = Array.from(document.querySelectorAll('table'));
          return tables.map(t => {
            const caption = t.querySelector('caption');
            const captionText = caption ? (caption.textContent || '').trim() : '';
            const ths = Array.from(t.querySelectorAll('th'));
            const thsMissingScope = ths.filter(th => !th.getAttribute('scope'));
            const isResponsive = Boolean(t.closest('.table-responsive, .table-responsive-clean'));

            return {
              hasCaption: Boolean(caption && captionText.length > 0),
              captionText,
              thCount: ths.length,
              thsMissingScopeCount: thsMissingScope.length,
              isResponsive
            };
          });
        });

        assert.ok(tableAudit.length > 0, `Page ${pageName} must contain at least one <table>`);
        for (const tbl of tableAudit) {
          assert.strictEqual(tbl.hasCaption, true, `Table in ${pageName} must have a non-empty <caption>`);
          assert.strictEqual(tbl.thsMissingScopeCount, 0, `Table in ${pageName} has <th> without scope attribute`);
          assert.strictEqual(tbl.isResponsive, true, `Table in ${pageName} must be wrapped in responsive container`);
        }
      }

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during table a11y audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // WCAG 2.2 Universal Landmarks, Titles, and Skip Links across All 22 Frontend Pages
  test('WCAG 2.2 Universal Conformance across All 22 Frontend Pages: Titles, Skip Link, and Main Landmark', async () => {
    const all22Pages = [
      'admin-accounts.html',
      'admin-lessons.html',
      'admin-radicals.html',
      'admin-roles.html',
      'admin-vocabulary.html',
      'creator-import.html',
      'creator-lesson-editor.html',
      'creator-lessons.html',
      'index.html',
      'lesson-detail.html',
      'lessons.html',
      'login.html',
      'moderator-history.html',
      'moderator-queue.html',
      'moderator-review.html',
      'profile.html',
      'radicals.html',
      'register.html',
      'srs-dashboard.html',
      'srs-review.html',
      'ui-verification.html',
      'vocabulary.html'
    ];

    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.route('**/api/v1/**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              page: 0,
              size: 20,
              totalElements: 0,
              totalPages: 0,
              first: true,
              last: true,
              items: []
            }
          })
        });
      });

      await page.addInitScript(() => {
        localStorage.setItem('access_token', 'test_admin_jwt');
        localStorage.setItem('user_info', JSON.stringify({
          accountId: 1,
          emailOrPhone: 'admin@example.com',
          fullName: 'Quản Trị Viên',
          roles: ['Admin', 'Creator', 'Moderator', 'Learner']
        }));
      });

      for (const pageName of all22Pages) {
        await page.goto(`${baseUrl}/${pageName}`, { waitUntil: 'domcontentloaded' });

        const pageAudit = await page.evaluate(() => {
          const title = (document.title || '').trim();
          const main = document.querySelector('main#mainContent');
          const skipLink = document.querySelector('a.skip-link[href="#mainContent"]');
          const lang = document.documentElement.getAttribute('lang');

          return {
            title,
            hasMain: Boolean(main),
            hasSkipLink: Boolean(skipLink),
            lang
          };
        });

        assert.ok(pageAudit.title.length > 5, `Page ${pageName} must have meaningful <title> (got "${pageAudit.title}")`);
        assert.strictEqual(pageAudit.hasMain, true, `Page ${pageName} must have <main id="mainContent"> landmark`);
        assert.strictEqual(pageAudit.hasSkipLink, true, `Page ${pageName} must have skip link targeting #mainContent`);
        assert.strictEqual(pageAudit.lang, 'vi', `Page ${pageName} must specify lang="vi" on html element`);
      }

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during universal 22-page audit:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

});



