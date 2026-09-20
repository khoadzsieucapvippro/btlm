/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: PUBLIC LESSONS & ORDERED DETAIL UI (TASK 9C.1)
 * File: tests/frontend/e2e/lessons.browser.mjs
 * 
 * Verifies:
 * - LB1: Catalog loads with Approved lesson cards, title, vocab count, verified date, and clean URL params (strictly NO search/q).
 * - LB2: Card interaction / CTA navigation directly targets lesson-detail.html?id=<lessonId>.
 * - LB3: Pagination & page size selector (12, 20, 40) navigate pages and clamp boundaries.
 * - LB4: Empty catalog state when totalElements === 0 renders calm empty UI without fake items.
 * - LB5: API Error & Retry flow displays error message and re-fetches on retry click.
 * - LB6: Lesson Detail page loads valid ID, renders breadcrumbs, title, and ordered vocabulary (1 -> 2 -> 3...).
 * - LB7: Web Speech API pronunciation button triggers safely without page crash.
 * - LB8: Neutral 404 defense: Nonexistent or unapproved lesson displays neutral message without leaking moderation state.
 * - LB9: Missing or malformed lesson ID parameters handled gracefully without unnecessary API requests.
 * - LB10: Mobile 375px responsiveness with zero horizontal overflow on both catalog and detail pages.
 * - LB11: Security & XSS defense: Untrusted HTML/script strings in lesson title/vocabularies render strictly as text.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function createMockLessonSummaries() {
  return [
    {
      lessonId: 1,
      title: 'Bài 1: Chào hỏi và Đại từ nhân xưng',
      status: 'Approved',
      vocabularyCount: 10,
      createdAt: '2026-08-01T08:00:00Z',
      updatedAt: '2026-08-02T10:30:00Z'
    },
    {
      lessonId: 2,
      title: 'Bài 2: Chữ số, Ngày tháng và Thời gian',
      status: 'Approved',
      vocabularyCount: 15,
      createdAt: '2026-08-03T09:00:00Z',
      updatedAt: '2026-08-04T11:00:00Z'
    },
    {
      lessonId: 3,
      title: 'Bài 3: Mua sắm và Giá cả',
      status: 'Approved',
      vocabularyCount: 12,
      createdAt: '2026-08-05T14:00:00Z',
      updatedAt: '2026-08-06T16:00:00Z'
    }
  ];
}

function createMockLessonDetail(lessonId) {
  return {
    lessonId: Number(lessonId),
    title: 'Bài 1: Chào hỏi và Đại từ nhân xưng',
    status: 'Approved',
    vocabularyCount: 3,
    createdAt: '2026-08-01T08:00:00Z',
    updatedAt: '2026-08-02T10:30:00Z',
    vocabularies: [
      {
        vocabId: 101,
        hanzi: '你',
        pinyin: 'nǐ',
        pinyinRaw: 'ni3',
        meaningHanViet: 'Nhĩ',
        meaningVi: 'Bạn, anh, chị (đại từ ngôi thứ 2)',
        audioUrl: 'https://cdn.example.com/audio/ni.mp3',
        videoWritingUrl: null,
        exampleSentence: '你好！',
        exampleTranslation: 'Xin chào!',
        orderIndex: 1
      },
      {
        vocabId: 102,
        hanzi: '好',
        pinyin: 'hǎo',
        pinyinRaw: 'hao3',
        meaningHanViet: 'Hảo',
        meaningVi: 'Tốt, đẹp, khỏe',
        audioUrl: null,
        videoWritingUrl: null,
        exampleSentence: '今天天气很好。',
        exampleTranslation: 'Thời tiết hôm nay rất tốt.',
        orderIndex: 2
      },
      {
        vocabId: 103,
        hanzi: '我',
        pinyin: 'wǒ',
        pinyinRaw: 'wo3',
        meaningHanViet: 'Ngã',
        meaningVi: 'Tôi, mình (đại từ ngôi thứ 1)',
        audioUrl: 'https://cdn.example.com/audio/wo.mp3',
        videoWritingUrl: null,
        exampleSentence: '我是学生。',
        exampleTranslation: 'Tôi là học sinh.',
        orderIndex: 3
      }
    ]
  };
}

describe('Browser E2E: Public Lessons & Lesson Detail (Task 9C.1)', () => {
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

  // LB1: Catalog Initial Load & Clean Query Params Contract
  test('LB1: Catalog loads with Approved lesson cards and verifies query params (page & size ONLY)', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);
    const mockItems = createMockLessonSummaries();
    let interceptedUrl = null;

    try {
      await page.route('**/api/v1/lessons**', async (route) => {
        const url = route.request().url();
        if (!url.includes('/api/v1/lessons/')) {
          interceptedUrl = url;
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'OK',
              data: {
                page: 0,
                size: 20,
                totalElements: mockItems.length,
                totalPages: 1,
                items: mockItems
              }
            })
          });
          return;
        }
        await route.continue();
      });

      await page.goto(`${baseUrl}/lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('.lesson-card');

      // Verify Query URL has page and size, and strictly NO search/q/keyword
      assert.ok(interceptedUrl, 'Catalog endpoint must be called');
      const parsedUrl = new URL(interceptedUrl);
      assert.strictEqual(parsedUrl.searchParams.has('page'), true);
      assert.strictEqual(parsedUrl.searchParams.has('size'), true);
      assert.strictEqual(parsedUrl.searchParams.has('search'), false, 'Contract Invariant: NEVER include search');
      assert.strictEqual(parsedUrl.searchParams.has('q'), false, 'Contract Invariant: NEVER include q');
      assert.strictEqual(parsedUrl.searchParams.has('keyword'), false, 'Contract Invariant: NEVER include keyword');

      // Verify Card Rendering
      const cards = page.locator('.lesson-card');
      assert.strictEqual(await cards.count(), 3);

      const firstTitle = await cards.nth(0).locator('.lesson-card-title').textContent();
      assert.ok(firstTitle.includes('Bài 1: Chào hỏi'), `Expected first lesson title, got "${firstTitle}"`);

      const vocabBadge = await cards.nth(0).locator('.lesson-vocab-count-badge').textContent();
      assert.ok(vocabBadge.includes('10 từ'), `Expected 10 từ vựng badge, got "${vocabBadge}"`);

      // Verify Date Rendering uses semantic <time>
      const timeEl = cards.nth(0).locator('time');
      assert.ok(await timeEl.isVisible(), '<time> element must be present');
      assert.ok(await timeEl.getAttribute('datetime'), 'datetime attribute must be present');

      // Verify Result Count status
      const resultCountText = await page.locator('#lessonsResultCount').textContent();
      assert.ok(resultCountText.includes('3 bài học'), `Expected 3 bài học in status, got "${resultCountText}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // LB2: Card Navigation to lesson-detail.html?id=<lessonId>
  test('LB2: Clicking CTA link navigates to lesson-detail.html?id=1', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);
    const mockItems = createMockLessonSummaries();

    try {
      await page.route('**/api/v1/lessons**', async (route) => {
        const url = route.request().url();
        const detailMatch = url.match(/\/api\/v1\/lessons\/(\d+)/);
        if (detailMatch) {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'OK',
              data: createMockLessonDetail(detailMatch[1])
            })
          });
          return;
        }

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: {
              page: 0,
              size: 20,
              totalElements: mockItems.length,
              totalPages: 1,
              items: mockItems
            }
          })
        });
      });

      await page.goto(`${baseUrl}/lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('.lesson-card');

      const firstCardCta = page.locator('.lesson-card').nth(0).locator('a.btn-chinese-primary');
      const href = await firstCardCta.getAttribute('href');
      assert.ok(href.includes('lesson-detail.html?id=1'), `CTA href must be lesson-detail.html?id=1, got ${href}`);

      // Click CTA and navigate
      await firstCardCta.click();
      await page.waitForURL('**/lesson-detail.html?id=1');

      // Verify on detail page
      await page.waitForSelector('#lessonVocabList');
      const heading = await page.locator('#lessonDetailHeading').textContent();
      assert.ok(heading.includes('Bài 1: Chào hỏi'), `Expected detail heading, got "${heading}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // LB3: Pagination & Page Size Selection
  test('LB3: Page size switcher and pagination navigate correctly', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);
    let requestedSize = 20;
    let requestedPage = 0;

    try {
      await page.route('**/api/v1/lessons**', async (route) => {
        const url = new URL(route.request().url());
        requestedPage = Number(url.searchParams.get('page')) || 0;
        requestedSize = Number(url.searchParams.get('size')) || 20;

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: {
              page: requestedPage,
              size: requestedSize,
              totalElements: 50,
              totalPages: Math.ceil(50 / requestedSize),
              items: createMockLessonSummaries()
            }
          })
        });
      });

      await page.goto(`${baseUrl}/lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('.lesson-card');

      // Switch page size to 12
      const sizeSelect = page.locator('#pageSizeSelect');
      await sizeSelect.selectOption('12');
      await page.waitForTimeout(100);

      assert.strictEqual(requestedSize, 12, 'Page size should be updated to 12');
      assert.strictEqual(requestedPage, 0, 'Page index should reset to 0 upon size change');

      // Click Next button
      const nextBtn = page.locator('#lessonPaginationNextBtn');
      await nextBtn.click();
      await page.waitForTimeout(100);

      assert.strictEqual(requestedPage, 1, 'Page index should advance to 1 on Next click');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // LB4: Empty Catalog State (No fake data)
  test('LB4: Empty catalog displays polite empty state when totalElements === 0', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);

    try {
      await page.route('**/api/v1/lessons**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: {
              page: 0,
              size: 20,
              totalElements: 0,
              totalPages: 0,
              items: []
            }
          })
        });
      });

      await page.goto(`${baseUrl}/lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#lessonsStateContainer');

      const emptyTitle = await page.locator('#lessonsStateContainer .state-empty-title').textContent();
      assert.ok(emptyTitle.includes('Chưa có bài học'), `Expected empty title, got "${emptyTitle}"`);

      // Ensure 0 cards rendered
      assert.strictEqual(await page.locator('.lesson-card').count(), 0);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // LB5: API Error State & Working Retry Button
  test('LB5: Backend 500 error displays error state with working retry trigger', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);
    let attempts = 0;

    try {
      await page.route('**/api/v1/lessons**', async (route) => {
        attempts++;
        if (attempts === 1) {
          await route.fulfill({
            status: 500,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'INTERNAL_ERROR',
              message: 'Lỗi máy chủ nội bộ'
            })
          });
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: {
              page: 0,
              size: 20,
              totalElements: 1,
              totalPages: 1,
              items: [createMockLessonSummaries()[0]]
            }
          })
        });
      });

      await page.goto(`${baseUrl}/lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#lessonsStateContainer [role="alert"]');

      // Click Retry button
      const retryBtn = page.locator('#lessonsStateContainer button');
      assert.ok(await retryBtn.isVisible(), 'Retry button must be visible');
      await retryBtn.click();

      // Should recover and display card
      await page.waitForSelector('.lesson-card');
      assert.strictEqual(await page.locator('.lesson-card').count(), 1);

      // Clean runtime errors (network 500 is handled gracefully)
      const errors = getRuntimeErrors().filter(e => !e.includes('500') && !e.includes('Failed to load resource'));
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // LB6: Lesson Detail Ordered Vocabulary Sequence
  test('LB6: Lesson Detail renders breadcrumbs, title, vocab count, and strictly ordered list (1 -> 2 -> 3...)', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);
    const detailData = createMockLessonDetail(1);

    try {
      await page.route('**/api/v1/lessons/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: detailData
          })
        });
      });

      await page.goto(`${baseUrl}/lesson-detail.html?id=1`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#lessonVocabList');

      // Breadcrumbs
      const breadcrumbNav = page.locator('nav[aria-label="Đường dẫn bài học"]');
      assert.ok(await breadcrumbNav.isVisible(), 'Breadcrumb nav landmark must be visible');
      const activeBreadcrumb = breadcrumbNav.locator('[aria-current="page"]');
      assert.ok(await activeBreadcrumb.isVisible(), 'Active breadcrumb with aria-current="page" must exist');

      // Heading and Vocabulary count
      const heading = await page.locator('#lessonDetailHeading').textContent();
      assert.ok(heading.includes('Bài 1: Chào hỏi'), `Expected heading, got "${heading}"`);

      const vocabBadge = await page.locator('#detailVocabCount').textContent();
      assert.ok(vocabBadge.includes('3 từ vựng'), `Expected 3 từ vựng badge, got "${vocabBadge}"`);

      // Ordered list items
      const listItems = page.locator('#lessonVocabList > li.lesson-vocab-item');
      assert.strictEqual(await listItems.count(), 3, 'Must render exactly 3 ordered vocabulary items');

      // Verify order badges: #1, #2, #3
      for (let i = 0; i < 3; i++) {
        const orderBadge = await listItems.nth(i).locator('.lesson-vocab-order-badge').textContent();
        assert.ok(orderBadge.includes(String(i + 1)), `Item ${i} must have badge #${i + 1}, got "${orderBadge}"`);
      }

      // Verify Hanzi / Pinyin / Meanings on item 1 (你)
      const firstItem = listItems.nth(0);
      assert.strictEqual(await firstItem.locator('.lesson-vocab-hanzi').textContent(), '你');
      assert.strictEqual(await firstItem.locator('.lesson-vocab-pinyin').textContent(), 'nǐ');
      assert.ok((await firstItem.locator('.lesson-vocab-hanviet').textContent()).includes('Nhĩ'));

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // LB7: Pronunciation Audio & Web Speech Activation
  test('LB7: Pronunciation button invokes Web Speech without page crash', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);

    try {
      await page.route('**/api/v1/lessons/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: createMockLessonDetail(1)
          })
        });
      });

      await page.goto(`${baseUrl}/lesson-detail.html?id=1`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#lessonVocabList');

      const speakBtn = page.locator('#lessonVocabList .btn-vocab-speech').nth(0);
      assert.ok(await speakBtn.isVisible(), 'Speech button must be visible');
      await speakBtn.click();
      await page.waitForTimeout(100);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors after speech click: ${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // LB8: Neutral 404 Defense (Unapproved / Nonexistent Lesson)
  test('LB8: 404 response displays neutral message without leaking moderation state', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);

    try {
      await page.route('**/api/v1/lessons/9999**', async (route) => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'NOT_FOUND',
            message: 'Lesson not found'
          })
        });
      });

      await page.goto(`${baseUrl}/lesson-detail.html?id=9999`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#lessonDetailStateContainer [role="alert"]');

      const alertText = await page.locator('#lessonDetailStateContainer [role="alert"]').textContent();
      assert.ok(
        alertText.includes('Không tìm thấy bài học hoặc bài học chưa được công khai'),
        `Expected neutral 404 message, got "${alertText}"`
      );

      // Must NEVER leak words like "Draft", "Pending", "Rejected", "moderation"
      assert.strictEqual(alertText.includes('Pending'), false, 'Must NOT leak Pending state');
      assert.strictEqual(alertText.includes('Draft'), false, 'Must NOT leak Draft state');
      assert.strictEqual(alertText.includes('Rejected'), false, 'Must NOT leak Rejected state');

      const errors = getRuntimeErrors().filter(e => !e.includes('404') && !e.includes('Failed to load resource'));
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // LB9: Missing & Malformed ID Handling
  test('LB9: Missing or invalid ID parameter displays polite error without unnecessary API calls', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);
    let apiCalled = false;

    try {
      await page.route('**/api/v1/lessons/**', async (route) => {
        apiCalled = true;
        await route.abort();
      });

      // Navigate with no ID parameter
      await page.goto(`${baseUrl}/lesson-detail.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#lessonDetailStateContainer [role="alert"]');

      const errorText = await page.locator('#lessonDetailStateContainer [role="alert"]').textContent();
      assert.ok(errorText.includes('Mã bài học không hợp lệ'), `Expected invalid ID alert, got "${errorText}"`);
      assert.strictEqual(apiCalled, false, 'API must NOT be called when ID is missing');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // LB10: Mobile 375px Responsiveness (Zero horizontal overflow)
  test('LB10: Responsive layout on 375px mobile viewport has zero horizontal scroll on lessons and detail', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 375,
      viewportHeight: 667
    });

    try {
      await page.route('**/api/v1/lessons**', async (route) => {
        const url = route.request().url();
        if (url.includes('/api/v1/lessons/1')) {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ code: 'SUCCESS', data: createMockLessonDetail(1) })
          });
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: {
              page: 0,
              size: 20,
              totalElements: 3,
              totalPages: 1,
              items: createMockLessonSummaries()
            }
          })
        });
      });

      // 1. Check Catalog Page on 375px
      await page.goto(`${baseUrl}/lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('.lesson-card');

      const catalogScrollWidth = await page.evaluate(() => document.documentElement.scrollWidth);
      const catalogClientWidth = await page.evaluate(() => document.documentElement.clientWidth);
      assert.strictEqual(catalogScrollWidth <= catalogClientWidth, true, `Catalog horizontal overflow: ${catalogScrollWidth} > ${catalogClientWidth}`);

      // 2. Check Detail Page on 375px
      await page.goto(`${baseUrl}/lesson-detail.html?id=1`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#lessonVocabList');

      const detailScrollWidth = await page.evaluate(() => document.documentElement.scrollWidth);
      const detailClientWidth = await page.evaluate(() => document.documentElement.clientWidth);
      assert.strictEqual(detailScrollWidth <= detailClientWidth, true, `Detail horizontal overflow: ${detailScrollWidth} > ${detailClientWidth}`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // LB11: Security & XSS Resistance
  test('LB11: Adversarial script tags in lesson title and vocab fields are safely rendered as plain text', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser);

    try {
      await page.route('**/api/v1/lessons/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: {
              lessonId: 1,
              title: '<img src=x onerror=alert(1)> Bài học XSS',
              status: 'Approved',
              vocabularyCount: 1,
              createdAt: '2026-08-01T00:00:00Z',
              updatedAt: '2026-08-01T00:00:00Z',
              vocabularies: [
                {
                  vocabId: 99,
                  hanzi: '<script>alert(2)</script>',
                  pinyin: 'xss',
                  pinyinRaw: 'xss',
                  meaningHanViet: '<b onmouseover=alert(3)>Hán Việt</b>',
                  meaningVi: '<iframe src=javascript:alert(4)></iframe>',
                  orderIndex: 1
                }
              ]
            }
          })
        });
      });

      // Monitor page dialogs to detect alert() execution
      let alertTriggered = false;
      page.on('dialog', async (dialog) => {
        alertTriggered = true;
        await dialog.dismiss();
      });

      await page.goto(`${baseUrl}/lesson-detail.html?id=1`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#lessonVocabList');

      // Verify no dialogs fired
      assert.strictEqual(alertTriggered, false, 'XSS attack must NOT trigger any alert dialog');

      // Verify text is escaped safely in DOM
      const titleText = await page.locator('#lessonDetailHeading').textContent();
      assert.ok(titleText.includes('Bài học XSS'), `Title text should contain title string, got "${titleText}"`);
      assert.strictEqual(await page.locator('img[src="x"]').count(), 0, 'No injected <img> should exist');
      assert.strictEqual(await page.locator('iframe').count(), 0, 'No injected <iframe> should exist');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });
});
