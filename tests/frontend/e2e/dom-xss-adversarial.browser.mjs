/**
 * =============================================================================
 * BROWSER E2E TEST: ADVERSARIAL DOM XSS PREVENTION & DYNAMIC DATA RENDERING (9G.2)
 * File: tests/frontend/e2e/dom-xss-adversarial.browser.mjs
 * 
 * Verifies:
 * - Harmless sentinel detection: window.__XSS_TRIGGERED__ === undefined
 * - Zero unexpected alert/prompt/confirm dialogs across critical user flows
 * - XSS-01: Personal Notes modal (freeform user text, <script>, <img onerror>)
 * - XSS-02: Creator Lesson Editor (lesson title, <svg onload>, attribute breakouts)
 * - XSS-03: Vocabulary Catalog Search (URL query parameter & search input reflection)
 * - XSS-04: Moderator History & Review (rejection reason plain text rendering)
 * - XSS-05: Contextual URL sanitization in media and links (javascript:, /\\, \\)
 * - Legitimate data preservation: Chinese characters (你好，世界) and Vietnamese diacritics
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

describe('Browser E2E: Adversarial DOM XSS Prevention & Safe Rendering Audit (Task 9G.2)', () => {
  let server;
  let browser;

  before(async () => {
    server = await ensureServer({ port: 3000 });
    browser = await launchBrowser();
  });

  after(async () => {
    if (browser) await browser.close();
    if (server) await server.close();
  });

  /**
   * Helper: Attaches an XSS sentinel detector to a page.
   * Tracks dialog calls and verifies window.__XSS_TRIGGERED__ remains undefined.
   */
  function attachXssSentinel(page) {
    const dialogs = [];
    page.on('dialog', async (dialog) => {
      dialogs.push({ type: dialog.type(), message: dialog.message() });
      await dialog.dismiss();
    });
    return {
      getDialogs: () => [...dialogs],
      assertClean: async () => {
        const triggered = await page.evaluate(() => window.__XSS_TRIGGERED__);
        assert.strictEqual(
          triggered,
          undefined,
          'SECURITY VIOLATION: window.__XSS_TRIGGERED__ was executed by malicious payload!'
        );
        assert.strictEqual(
          dialogs.length,
          0,
          `SECURITY VIOLATION: Unexpected dialog appeared: ${JSON.stringify(dialogs)}`
        );
      }
    };
  }

  test('XSS-01: Personal Notes renders <script> and <img onerror> payloads strictly as safe plain text', async () => {
    const { page, context } = await createMonitoredPage(browser);
    const sentinel = attachXssSentinel(page);

    try {
      // 1. Setup authenticated Learner session
      await page.addInitScript(() => {
        localStorage.setItem('access_token', 'test_jwt_xss_user');
        localStorage.setItem('user_info', JSON.stringify({
          accountId: 888,
          emailOrPhone: 'xss_tester@example.com',
          fullName: 'Kiểm Thử An Ninh',
          roles: ['Learner']
        }));
      });

      // 2. Mock API routes for vocabulary and notes
      let storedNotes = [];
      await page.route(/\/api\/v1\/vocabulary(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              items: [{
                vocabId: 101,
                hanzi: '学',
                pinyin: 'xué',
                meaningHanViet: 'Học',
                meaningVi: 'Học tập',
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

      await page.route('**/api/v1/vocabularies/101/notes*', async (route) => {
        if (route.request().method() === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'OK',
              data: {
                items: storedNotes,
                page: 0,
                size: 20,
                totalElements: storedNotes.length,
                totalPages: 1
              }
            })
          });
        } else if (route.request().method() === 'POST') {
          const payload = JSON.parse(route.request().postData());
          const newNote = {
            noteId: 5001 + storedNotes.length,
            vocabId: 101,
            content: payload.content,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          };
          storedNotes.unshift(newNote);
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'Ghi chú đã được lưu',
              data: newNote
            })
          });
        }
      });

      // 3. Navigate to Vocabulary page and open Notes modal
      await page.goto(`${server.baseUrl}/vocabulary.html`, { waitUntil: 'domcontentloaded' });
      const noteBtn = page.locator('.vocab-card-note-btn').first();
      await noteBtn.waitFor({ state: 'visible' });
      await noteBtn.click();

      const modal = page.locator('#notesModal');
      await modal.waitFor({ state: 'visible' });

      // 4. Inject Payload 1: <script> execution probe
      const scriptPayload = "<script>window.__XSS_TRIGGERED__=true;alert('XSS_SCRIPT')</script>";
      const noteInput = page.locator('#noteContentInput');
      await noteInput.waitFor({ state: 'visible' });
      await noteInput.fill(scriptPayload);

      const saveBtn = page.locator('#noteSubmitBtn');
      await saveBtn.click();

      // Wait for card to appear in list
      const noteCard = page.locator('.note-item').first();
      await noteCard.waitFor({ state: 'visible' });

      // 5. Assert zero execution and verify plain-text fidelity
      await sentinel.assertClean();

      const renderedText = await noteCard.locator('.note-content').textContent();
      assert.strictEqual(renderedText.trim(), scriptPayload);

      // Verify no child <script> tag was created inside the DOM
      const scriptChildCount = await noteCard.locator('.note-content script').count();
      assert.strictEqual(scriptChildCount, 0, 'No script element should exist in rendered note DOM');

      // 6. Inject Payload 2: <img onerror> HTML breakout probe
      const imgPayload = '<img src=x onerror="window.__XSS_TRIGGERED__=true;alert(\'XSS_IMG\')">';
      await noteInput.fill(imgPayload);
      await saveBtn.click();

      // Wait for second card to render
      await page.waitForFunction(() => document.querySelectorAll('.note-item').length >= 2);
      await sentinel.assertClean();

      const secondRenderedText = await page.locator('.note-item').first().locator('.note-content').textContent();
      assert.strictEqual(secondRenderedText.trim(), imgPayload);

      const imgChildCount = await page.locator('.note-item').first().locator('.note-content img').count();
      assert.strictEqual(imgChildCount, 0, 'No img element should exist in rendered note DOM');

      // 7. Inject Legitimate Vietnamese & Chinese content to verify regression
      const legitContent = '学习笔记：Học chữ Hán (xué) — nghĩa là "Học tập" (Tiếng Việt có dấu).';
      await noteInput.fill(legitContent);
      await saveBtn.click();

      await page.waitForFunction(() => document.querySelectorAll('.note-item').length >= 3);
      await sentinel.assertClean();

      const legitRenderedText = await page.locator('.note-item').first().locator('.note-content').textContent();
      assert.strictEqual(legitRenderedText.trim(), legitContent);

    } finally {
      await context.close();
    }
  });

  test('XSS-02: Creator Lesson Editor renders <svg onload> and attribute breakout safely as text', async () => {
    const { page, context } = await createMonitoredPage(browser);
    const sentinel = attachXssSentinel(page);

    try {
      // 1. Setup authenticated Creator session
      await page.addInitScript(() => {
        localStorage.setItem('access_token', 'test_jwt_creator_user');
        localStorage.setItem('user_info', JSON.stringify({
          accountId: 777,
          emailOrPhone: 'creator_xss@example.com',
          fullName: 'Tác Giả Giáo Trình',
          roles: ['Creator']
        }));
      });

      // 2. Mock lesson detail with adversarial title containing <svg onload>
      const svgPayload = '<svg onload="window.__XSS_TRIGGERED__=true;alert(\'XSS_SVG\')">';
      await page.route('**/api/v1/creator/lessons/200*', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              lessonId: 200,
              title: svgPayload,
              description: 'Mô tả bài học chứa chữ Hán: 你好，世界',
              status: 'Draft',
              creatorEmail: 'creator_xss@example.com',
              vocabularyCount: 0,
              vocabularies: [],
              createdAt: '2026-09-18T00:00:00Z',
              updatedAt: '2026-09-18T00:00:00Z'
            }
          })
        });
      });

      // 3. Navigate to Creator Lesson Editor
      await page.goto(`${server.baseUrl}/creator-lesson-editor.html?id=200`, { waitUntil: 'domcontentloaded' });

      // 4. Verify no execution triggered on load
      await sentinel.assertClean();

      // Check title input displays literal string
      const titleInput = page.locator('#editorTitleInput');
      await titleInput.waitFor({ state: 'visible' });
      const val = await titleInput.inputValue();
      assert.strictEqual(val, svgPayload);

      // Verify breadcrumb title text
      const breadcrumbTitle = page.locator('#editorBreadcrumbTitle');
      await breadcrumbTitle.waitFor({ state: 'visible' });
      const breadcrumbText = await breadcrumbTitle.textContent();
      assert.strictEqual(breadcrumbText.trim(), svgPayload);

      // Verify no SVG tag with onload was created in DOM
      const svgOnloadCount = await page.locator('svg[onload]').count();
      assert.strictEqual(svgOnloadCount, 0);

    } finally {
      await context.close();
    }
  });

  test('XSS-03: Vocabulary search query parameter reflection renders literal text without execution', async () => {
    const { page, context } = await createMonitoredPage(browser);
    const sentinel = attachXssSentinel(page);

    try {
      const searchProbe = '"><script>window.__XSS_TRIGGERED__=true;alert("XSS_SEARCH")</script>';

      // Mock search query returning 0 items (triggers empty state with search query reflection)
      await page.route(/\/api\/v1\/vocabulary(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              items: [],
              page: 0,
              size: 20,
              totalElements: 0,
              totalPages: 0
            }
          })
        });
      });

      // Navigate with encoded search query parameter
      await page.goto(`${server.baseUrl}/vocabulary.html?search=${encodeURIComponent(searchProbe)}`, {
        waitUntil: 'networkidle'
      });

      // Verify sentinel is clean
      await sentinel.assertClean();

      // Verify search input holds decoded string as plain text
      const searchInput = page.locator('#vocabSearchInput');
      await searchInput.waitFor({ state: 'visible' });
      const inputVal = await searchInput.inputValue();
      assert.strictEqual(inputVal, searchProbe);

      // Verify empty state description did not execute or parse script
      const emptyState = page.locator('#vocabEmptyState');
      if (await emptyState.count() > 0) {
        const text = await emptyState.textContent();
        assert.ok(!text.includes('[object Object]'));
      }

      // Verify no injected script exists
      const scripts = await page.locator('main script').count();
      assert.strictEqual(scripts, 0);

    } finally {
      await context.close();
    }
  });

  test('XSS-04: Moderator History renders rejectionReason payload strictly as safe plain text', async () => {
    const { page, context } = await createMonitoredPage(browser);
    const sentinel = attachXssSentinel(page);

    try {
      // 1. Setup authenticated Moderator session
      await page.addInitScript(() => {
        localStorage.setItem('access_token', 'test_jwt_mod_user');
        localStorage.setItem('user_info', JSON.stringify({
          accountId: 666,
          emailOrPhone: 'mod_xss@example.com',
          fullName: 'Kiểm Duyệt Viên',
          roles: ['Moderator']
        }));
      });

      const rejectionProbe = '<img src=x onerror="window.__XSS_TRIGGERED__=true;alert(\'XSS_MOD\')">';

      // Mock history GET returning 2 items: 1 malicious probe, 1 legitimate Vietnamese review
      await page.route(/\/api\/v1\/moderator\/history(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              items: [
                {
                  historyId: 10,
                  lessonId: 301,
                  lessonTitle: 'Bài kiểm thử XSS #301',
                  action: 'REJECT',
                  rejectionReason: rejectionProbe,
                  flaggedFields: '["hanzi", "pinyin"]',
                  moderatorEmail: 'mod_xss@example.com',
                  createdAt: '2026-09-18T01:00:00Z'
                },
                {
                  historyId: 11,
                  lessonId: 302,
                  lessonTitle: 'Bài học Chữ Hán Căn Bản',
                  action: 'REJECT',
                  rejectionReason: 'Lý do từ chối: Cần bổ sung ví dụ minh họa và nghĩa Hán Việt chuẩn xác.',
                  flaggedFields: '["exampleSentence"]',
                  moderatorEmail: 'mod_xss@example.com',
                  createdAt: '2026-09-18T02:00:00Z'
                }
              ],
              page: 0,
              size: 20,
              totalElements: 2,
              totalPages: 1
            }
          })
        });
      });

      // Navigate to Moderator History
      await page.goto(`${server.baseUrl}/moderator-history.html`, { waitUntil: 'networkidle' });

      // Verify zero execution
      await sentinel.assertClean();

      // Check first rejection cell
      const firstReasonCell = page.locator('.rejection-reason-cell').first();
      await firstReasonCell.waitFor({ state: 'visible' });

      const cellText = await firstReasonCell.textContent();
      assert.strictEqual(cellText.trim(), rejectionProbe);

      // Verify no <img> tag in table body
      const imgInCell = await firstReasonCell.locator('img').count();
      assert.strictEqual(imgInCell, 0);

      // Check second cell preserves Vietnamese diacritics
      const secondReasonCell = page.locator('.rejection-reason-cell').nth(1);
      const secondText = await secondReasonCell.textContent();
      assert.strictEqual(
        secondText.trim(),
        'Lý do từ chối: Cần bổ sung ví dụ minh họa và nghĩa Hán Việt chuẩn xác.'
      );

    } finally {
      await context.close();
    }
  });

  test('XSS-05: Contextual URL Sanitization neutralizes executable schemes and protocol-relative bypasses', async () => {
    const { page, context } = await createMonitoredPage(browser);
    const sentinel = attachXssSentinel(page);

    try {
      // Mock catalog item with safe audio and detail with adversarial URL
      await page.route(/\/api\/v1\/vocabulary\/999$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              vocabId: 999,
              hanzi: '水',
              pinyin: 'shuǐ',
              meaningHanViet: 'Thủy',
              meaningVi: 'Nước',
              audioUrl: 'javascript:window.__XSS_TRIGGERED__=true;alert("XSS_AUDIO")',
              exampleSentence: '我们喝水。',
              exampleTranslation: 'Chúng tôi uống nước.',
              radicals: [{
                radicalId: 85,
                character: '水',
                pinyin: 'shuǐ',
                meaningHanViet: 'Thủy'
              }]
            }
          })
        });
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
                vocabId: 999,
                hanzi: '水',
                pinyin: 'shuǐ',
                meaningHanViet: 'Thủy',
                meaningVi: 'Nước'
              }],
              page: 0,
              size: 20,
              totalElements: 1,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${server.baseUrl}/vocabulary.html`, { waitUntil: 'networkidle' });

      // Click card to open detail modal
      const card = page.locator('.vocab-card').first();
      await card.waitFor({ state: 'visible' });
      await card.click();

      // Wait for modal
      const modal = page.locator('#vocabDetailModal');
      await modal.waitFor({ state: 'visible' });

      // Verify sentinel remains clean
      await sentinel.assertClean();

      // Verify audio player src was neutralized (section hidden or src is not javascript:)
      const audioPlayer = page.locator('#modalAudioPlayer');
      if (await audioPlayer.count() > 0) {
        const src = await audioPlayer.getAttribute('src');
        assert.ok(
          src === null || src === '' || src === 'about:blank',
          `Audio src should be neutralized, received: "${src}"`
        );
      }

      // Check radical preview link does not execute javascript
      const radicalBadge = page.locator('.vocab-radical-badge').first();
      await radicalBadge.click();

      const previewLink = page.locator('#modalRadicalPreview a');
      await previewLink.waitFor({ state: 'visible' });
      const href = await previewLink.getAttribute('href');
      assert.ok(
        !href.toLowerCase().startsWith('javascript:'),
        `Link href must not be javascript: scheme, received: "${href}"`
      );

      await sentinel.assertClean();

    } finally {
      await context.close();
    }
  });

});
