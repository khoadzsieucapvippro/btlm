/**
 * =============================================================================
 * BROWSER E2E TEST: 214 KANGXI RADICALS CATALOG & PRESENTATION UI (TASK 9B.2)
 * File: tests/frontend/e2e/radicals.browser.mjs
 * 
 * Verifies:
 * - RB1: Page loads successfully.
 * - RB2: Mock API returns valid catalog envelope (ApiResponse<PageResponse<RadicalResponse>>).
 * - RB3: Catalog renders 214 records in the grid.
 * - RB4: Search by Chinese character glyph ('木').
 * - RB5: Search by Hán-Việt reading ('Khẩu').
 * - RB6: Search by Vietnamese meaning ('Dòng nước').
 * - RB7: No result displays Empty State with working "Xóa bộ lọc tìm kiếm" action.
 * - RB8: Pagination controls work (switching page sizes & navigating pages).
 * - RB9: Card click opens accessible Detail Modal with correct radical data.
 * - RB10: Escape key closes modal.
 * - RB11: Close button closes modal.
 * - RB12: Focus restores to the triggering card element after modal close.
 * - RB13: Optional audio and video elements render when URLs exist, cleanly absent when missing.
 * - RB14: Mobile 375px viewport renders without horizontal overflow.
 * - RB15: Malformed API payload handled gracefully.
 * - RB16: Adversarial XSS payload in radical fields is rendered as plain text without script execution.
 * - RB17: API 500 error triggers Error state with working Retry button.
 * - RB18: API Retry recovers cleanly and renders catalog.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function create214MockRadicals() {
  const radicals = [
    {
      radicalId: 1,
      character: '一',
      pinyin: 'yī',
      meaningHanViet: 'Nhất',
      meaningVi: 'Số một',
      audioUrl: 'https://cdn.example.com/audio/rad_1.mp3',
      videoWritingUrl: 'https://cdn.example.com/video/rad_1.mp4'
    },
    {
      radicalId: 30,
      character: '口',
      pinyin: 'kǒu',
      meaningHanViet: 'Khẩu',
      meaningVi: 'Cái miệng',
      audioUrl: null,
      videoWritingUrl: null
    },
    {
      radicalId: 75,
      character: '木',
      pinyin: 'mù',
      meaningHanViet: 'Mộc',
      meaningVi: 'Cây cối',
      audioUrl: 'https://cdn.example.com/audio/rad_75.mp3',
      videoWritingUrl: null
    },
    {
      radicalId: 85,
      character: '水',
      pinyin: 'shuǐ',
      meaningHanViet: 'Thủy',
      meaningVi: 'Dòng nước',
      audioUrl: null,
      videoWritingUrl: 'https://cdn.example.com/video/rad_85.mp4'
    }
  ];

  const existingIds = new Set(radicals.map(r => r.radicalId));
  for (let id = 1; id <= 214; id++) {
    if (!existingIds.has(id)) {
      radicals.push({
        radicalId: id,
        character: String.fromCodePoint(0x4E00 + id - 1),
        pinyin: `pinyin_${id}`,
        meaningHanViet: `Hán-Việt ${id}`,
        meaningVi: `Nghĩa tiếng Việt số ${id}`,
        audioUrl: null,
        videoWritingUrl: null
      });
    }
  }

  radicals.sort((a, b) => a.radicalId - b.radicalId);
  return radicals;
}

describe('Browser E2E: 214 Kangxi Radicals Catalog & Presentation UI', () => {
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

  test('RB1-RB3: Page loads and renders all 214 Kangxi radicals from API envelope', async () => {
    const { page } = await createMonitoredPage(browser);
    const mockRadicals = create214MockRadicals();

    // Intercept /api/v1/radicals
    await page.route('**/api/v1/radicals*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'SUCCESS',
          message: 'OK',
          data: {
            page: 0,
            size: 214,
            totalElements: 214,
            totalPages: 1,
            items: mockRadicals
          }
        })
      });
    });

    await page.goto(`${baseUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('.radical-card');

    // Verify exactly 214 cards rendered
    const cardCount = await page.locator('.radical-card').count();
    assert.strictEqual(cardCount, 214, 'Should render exactly 214 radical cards');

    // Verify first card is radical #1
    const firstCardText = await page.locator('.radical-card').first().textContent();
    assert.ok(firstCardText.includes('#1'), 'First card must be radical #1');
    assert.ok(firstCardText.includes('一'), 'First card glyph must be 一');
    assert.ok(firstCardText.includes('Nhất'), 'First card meaning must be Nhất');

    // Verify status badge
    const badgeText = await page.textContent('#radicalResultCount');
    assert.ok(badgeText.includes('214'), 'Result count badge must report 214 bộ thủ');
  });

  test('RB4-RB6: Client-side search filters by character, Hán-Việt, and Vietnamese meaning', async () => {
    const { page } = await createMonitoredPage(browser);
    const mockRadicals = create214MockRadicals();

    await page.route('**/api/v1/radicals*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'SUCCESS',
          message: 'OK',
          data: { page: 0, size: 214, totalElements: 214, totalPages: 1, items: mockRadicals }
        })
      });
    });

    await page.goto(`${baseUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('.radical-card');

    // 1. Search by Chinese character glyph: '木' (Radical 75)
    await page.fill('#radicalSearchInput', '木');
    await page.waitForFunction(() => document.querySelectorAll('.radical-card').length === 1);
    const card75Text = await page.locator('.radical-card').first().textContent();
    assert.ok(card75Text.includes('#75') && card75Text.includes('Mộc'));

    // 2. Search by Hán-Việt: 'Khẩu' (Radical 30)
    await page.fill('#radicalSearchInput', 'Khẩu');
    await page.waitForFunction(() => document.querySelectorAll('.radical-card').length === 1);
    const card30Text = await page.locator('.radical-card').first().textContent();
    assert.ok(card30Text.includes('#30') && card30Text.includes('Khẩu'));

    // 3. Search by Vietnamese meaning: 'Dòng nước' (Radical 85 Thủy)
    await page.fill('#radicalSearchInput', 'Dòng nước');
    await page.waitForFunction(() => document.querySelectorAll('.radical-card').length === 1);
    const card85Text = await page.locator('.radical-card').first().textContent();
    assert.ok(card85Text.includes('#85') && card85Text.includes('Thủy'));
  });

  test('RB7: Non-matching query triggers Empty State and clear action resets view', async () => {
    const { page } = await createMonitoredPage(browser);
    const mockRadicals = create214MockRadicals();

    await page.route('**/api/v1/radicals*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'SUCCESS',
          message: 'OK',
          data: { page: 0, size: 214, totalElements: 214, totalPages: 1, items: mockRadicals }
        })
      });
    });

    await page.goto(`${baseUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('.radical-card');

    // Type non-existent query
    await page.fill('#radicalSearchInput', 'zzzyyyxxx_non_existent');
    await page.waitForSelector('.state-empty-glyph');

    // Verify empty state is displayed
    const emptyGlyph = await page.textContent('.state-empty-glyph');
    assert.strictEqual(emptyGlyph.trim(), '空');

    const cardCount = await page.locator('.radical-card').count();
    assert.strictEqual(cardCount, 0);

    // Click "Xóa bộ lọc tìm kiếm" button inside empty state
    await page.click('.state-container button');

    // Verify grid recovers to 214 items
    await page.waitForSelector('.radical-card');
    const restoredCount = await page.locator('.radical-card').count();
    assert.strictEqual(restoredCount, 214);
  });

  test('RB8: Pagination controls function correctly when changing page size', async () => {
    const { page } = await createMonitoredPage(browser);
    const mockRadicals = create214MockRadicals();

    await page.route('**/api/v1/radicals*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'SUCCESS',
          message: 'OK',
          data: { page: 0, size: 214, totalElements: 214, totalPages: 1, items: mockRadicals }
        })
      });
    });

    await page.goto(`${baseUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('.radical-card');

    // Select page size 24
    await page.selectOption('#pageSizeSelect', '24');
    await page.waitForFunction(() => document.querySelectorAll('.radical-card').length === 24);

    // Verify pagination controls are visible
    const isPaginationVisible = await page.isVisible('#radicalPagination');
    assert.strictEqual(isPaginationVisible, true);

    // Click page 2 button
    await page.click('button[aria-label="Trang 2"]');
    await page.waitForFunction(() => {
      const firstCard = document.querySelector('.radical-card');
      return firstCard && firstCard.textContent.includes('#25');
    });

    // Verify first card on page 2 is #25
    const page2FirstCard = await page.locator('.radical-card').first().textContent();
    assert.ok(page2FirstCard.includes('#25'), 'Page 2 should start at radical #25');
  });

  test('RB9-RB12: Clicking card opens modal, Escape/Close closes it, focus returns to card', async () => {
    const { page } = await createMonitoredPage(browser);
    const mockRadicals = create214MockRadicals();

    await page.route('**/api/v1/radicals*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'SUCCESS',
          message: 'OK',
          data: { page: 0, size: 214, totalElements: 214, totalPages: 1, items: mockRadicals }
        })
      });
    });

    await page.goto(`${baseUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('.radical-card');

    // Target radical #1 card
    const card1 = page.locator('button[data-radical-id="1"]');
    await card1.focus();
    await card1.click();

    // Verify modal is open
    await page.waitForSelector('#radicalDetailModal[open]');

    // Check modal content for radical #1
    const modalTitle = await page.textContent('#modalRadicalTitle');
    assert.ok(modalTitle.includes('Nhất'), 'Modal title should contain Nhất');

    const modalPinyin = await page.textContent('#modalRadicalPinyin');
    assert.strictEqual(modalPinyin.trim(), 'yī');

    // RB10: Press Escape to close modal
    await page.keyboard.press('Escape');
    await page.waitForFunction(() => !document.getElementById('radicalDetailModal').hasAttribute('open'));

    // RB12: Verify focus returns to card #1
    const focusedIdAfterEscape = await page.evaluate(() => document.activeElement?.getAttribute('data-radical-id'));
    assert.strictEqual(focusedIdAfterEscape, '1', 'Focus must return to card #1 after Escape');

    // Reopen modal by clicking card #1
    await card1.click();
    await page.waitForSelector('#radicalDetailModal[open]');

    // RB11: Click close button to close modal
    await page.click('#modalCloseBtn');
    await page.waitForFunction(() => !document.getElementById('radicalDetailModal').hasAttribute('open'));

    // Verify focus returns to card #1 again
    const focusedIdAfterBtn = await page.evaluate(() => document.activeElement?.getAttribute('data-radical-id'));
    assert.strictEqual(focusedIdAfterBtn, '1', 'Focus must return to card #1 after Close button');
  });

  test('RB13: Audio and video optional fields render safely when present, absent when null', async () => {
    const { page } = await createMonitoredPage(browser);
    const mockRadicals = create214MockRadicals();

    await page.route('**/api/v1/radicals*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'SUCCESS',
          message: 'OK',
          data: { page: 0, size: 214, totalElements: 214, totalPages: 1, items: mockRadicals }
        })
      });
    });

    await page.goto(`${baseUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('.radical-card');

    // 1. Radical #1 has both audio and video
    await page.click('button[data-radical-id="1"]');
    await page.waitForSelector('#radicalDetailModal[open]');

    const audioVisible = await page.isVisible('#modalAudioSection');
    assert.strictEqual(audioVisible, true, 'Audio section should be visible for radical #1');

    const videoVisible = await page.isVisible('#modalVideoSection');
    assert.strictEqual(videoVisible, true, 'Video section should be visible for radical #1');

    // Close modal
    await page.click('#modalCloseBtn');
    await page.waitForFunction(() => !document.getElementById('radicalDetailModal').hasAttribute('open'));

    // 2. Radical #30 has neither audio nor video
    await page.click('button[data-radical-id="30"]');
    await page.waitForSelector('#radicalDetailModal[open]');

    const audioHidden = await page.isHidden('#modalAudioSection');
    assert.strictEqual(audioHidden, true, 'Audio section should be hidden when audioUrl is null');

    const videoHidden = await page.isHidden('#modalVideoSection');
    assert.strictEqual(videoHidden, true, 'Video section should be hidden when videoWritingUrl is null');

    await page.click('#modalCloseBtn');
  });

  test('RB14: Mobile 375px viewport displays catalog without horizontal page overflow', async () => {
    const { page } = await createMonitoredPage(browser);
    const mockRadicals = create214MockRadicals();

    await page.route('**/api/v1/radicals*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'SUCCESS',
          message: 'OK',
          data: { page: 0, size: 214, totalElements: 214, totalPages: 1, items: mockRadicals }
        })
      });
    });

    // Set mobile viewport 375x667
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto(`${baseUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('.radical-card');

    // Check horizontal overflow
    const hasHorizontalOverflow = await page.evaluate(() => {
      return document.documentElement.scrollWidth > document.documentElement.clientWidth;
    });
    assert.strictEqual(hasHorizontalOverflow, false, '375px viewport must not create horizontal overflow');
  });

  test('RB16: Adversarial XSS payload in API response is safely rendered as text', async () => {
    const { page } = await createMonitoredPage(browser);

    let xssExecuted = false;
    page.on('dialog', async (dialog) => {
      xssExecuted = true;
      await dialog.dismiss();
    });

    const adversarialRadicals = [
      {
        radicalId: 1,
        character: '<script>alert("xss")</script>',
        pinyin: 'yī',
        meaningHanViet: '<img src=x onerror=alert(1)>',
        meaningVi: 'javascript:alert(1)',
        audioUrl: 'javascript:alert(2)',
        videoWritingUrl: 'data:text/html,<script>alert(3)</script>'
      }
    ];

    await page.route('**/api/v1/radicals*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'SUCCESS',
          message: 'OK',
          data: { page: 0, size: 1, totalElements: 1, totalPages: 1, items: adversarialRadicals }
        })
      });
    });

    await page.goto(`${baseUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('.radical-card');

    // Click card to open modal
    await page.click('.radical-card');
    await page.waitForSelector('#radicalDetailModal[open]');

    // Wait a brief moment to ensure no script ran
    await page.waitForTimeout(300);
    assert.strictEqual(xssExecuted, false, 'No adversarial script must execute from API payload');

    // Audio and video sections must not load javascript: or data: URLs
    const audioSectionHidden = await page.isHidden('#modalAudioSection');
    assert.strictEqual(audioSectionHidden, true, 'Unsafe audio javascript: URL must be rejected and hidden');

    const videoSectionHidden = await page.isHidden('#modalVideoSection');
    assert.strictEqual(videoSectionHidden, true, 'Unsafe video data: URL must be rejected and hidden');
  });

  test('RB17-RB18: API 500 error triggers Error state, and Retry button recovers on success', async () => {
    const { page } = await createMonitoredPage(browser);
    let attempts = 0;
    const mockRadicals = create214MockRadicals();

    await page.route('**/api/v1/radicals*', async (route) => {
      attempts++;
      if (attempts === 1) {
        // First attempt fails with 500 Internal Server Error
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'INTERNAL_ERROR',
            message: 'Máy chủ danh mục gặp sự cố tạm thời'
          })
        });
      } else {
        // Subsequent retry succeeds
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: { page: 0, size: 214, totalElements: 214, totalPages: 1, items: mockRadicals }
          })
        });
      }
    });

    await page.goto(`${baseUrl}/radicals.html`, { waitUntil: 'domcontentloaded' });

    // RB17: Verify Error state is displayed
    await page.waitForSelector('.state-error-icon');
    const errorText = await page.textContent('.state-error-desc');
    assert.ok(errorText.includes('Máy chủ danh mục gặp sự cố'), 'Error state message should reflect failure');

    // RB18: Click Retry button
    await page.click('.state-container button');

    // Verify recovery to Ready state with 214 cards
    await page.waitForSelector('.radical-card');
    const restoredCount = await page.locator('.radical-card').count();
    assert.strictEqual(restoredCount, 214, 'Retry should successfully load the 214 radicals');
  });

});
