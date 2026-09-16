/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: MODERATOR LESSON REVIEW & VOCABULARY INSPECTION (TASK 9E.2)
 * File: tests/frontend/e2e/moderator-review.browser.mjs
 *
 * Verifies:
 * - MR-01: Authenticated Moderator loads lesson review -> verified header, creator note, and strictly ordered vocabularies.
 * - MR-02: Audio inspection control available when audioUrl exists, disabled badge when absent.
 * - MR-03: On-demand lazy inspection for constituent radicals via GET /api/v1/vocabulary/{id}.
 * - MR-04: Stale queue race condition / 404 recovery with friendly notification and return link.
 * - MR-05: Role guard blocks unauthorized role (Learner) and presents login banner.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function createMockLessonDetail(id = 101) {
  return {
    lessonId: id,
    title: 'HSK 1 — Bài 1: Chào hỏi căn bản',
    status: 'Pending',
    vocabularyCount: 3,
    createdAt: '2026-09-01T08:00:00Z',
    updatedAt: '2026-09-10T14:30:00Z',
    vocabularies: [
      {
        vocabId: 10,
        hanzi: '你',
        pinyin: 'nǐ',
        pinyinRaw: 'ni3',
        meaningHanViet: 'Nhĩ',
        meaningVi: 'Bạn, anh, chị (ngôi thứ hai số ít)',
        audioUrl: 'https://example.com/audio/ni.mp3',
        videoWritingUrl: 'https://example.com/video/ni.mp4',
        exampleSentence: '你好！',
        exampleTranslation: 'Xin chào!',
        orderIndex: 1
      },
      {
        vocabId: 11,
        hanzi: '好',
        pinyin: 'hǎo',
        pinyinRaw: 'hao3',
        meaningHanViet: 'Hảo',
        meaningVi: 'Tốt, đẹp, khỏe mạnh',
        audioUrl: null,
        videoWritingUrl: null,
        exampleSentence: '我很好。',
        exampleTranslation: 'Tôi rất khỏe.',
        orderIndex: 2
      },
      {
        vocabId: 12,
        hanzi: '再见',
        pinyin: 'zàijiàn',
        pinyinRaw: 'zai4jian4',
        meaningHanViet: 'Tái kiến',
        meaningVi: 'Tạm biệt, hẹn gặp lại',
        audioUrl: 'https://example.com/audio/zaijian.mp3',
        videoWritingUrl: null,
        exampleSentence: '明天再见！',
        exampleTranslation: 'Ngày mai gặp lại!',
        orderIndex: 3
      }
    ]
  };
}

async function setupModeratorSession(page, role = 'Moderator') {
  await page.addInitScript(`
    localStorage.setItem('access_token', 'test_moderator_jwt_token');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: 3001,
      emailOrPhone: 'moderator_user@example.com',
      fullName: 'Kiểm Duyệt Viên Mẫu',
      roles: ['${role}']
    }));
  `);
}

describe('Browser E2E: Moderator Lesson Review UI (Task 9E.2)', () => {
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

  // MR-01: Moderator views Pending lesson with header metadata and ordered vocabulary items
  test('MR-01: Authenticated Moderator loads review page with verified metadata and ordered vocabularies', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupModeratorSession(page, 'Moderator');
      const mockDetail = createMockLessonDetail(101);

      // Mock GET /api/v1/moderator/lessons/101
      await page.route(/\/api\/v1\/moderator\/lessons\/101$/, async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thao tác thành công',
            data: mockDetail
          })
        });
      });

      await page.goto(`${baseUrl}/moderator-review.html?id=101`, { waitUntil: 'domcontentloaded' });

      // 1. Verify Page Title and Header
      const h1 = page.locator('#reviewLessonTitle');
      await h1.waitFor({ state: 'visible', timeout: 5000 });
      const h1Text = await h1.textContent();
      assert.ok(h1Text.includes('HSK 1 — Bài 1: Chào hỏi căn bản'));

      // 2. Verify Metadata Badges
      const idBadge = await page.locator('#reviewLessonIdBadge').textContent();
      assert.ok(idBadge.includes('#101'));

      const statusBadge = await page.locator('#reviewLessonStatusBadge').textContent();
      assert.ok(statusBadge.includes('Chờ duyệt (Pending)'));

      const vocabCountBadge = await page.locator('#reviewLessonVocabCountBadge').textContent();
      assert.ok(vocabCountBadge.includes('3 từ vựng'));

      // 3. Verify Author Note (unavailability truthfulness)
      const authorNote = await page.locator('#reviewLessonAuthorNote').textContent();
      assert.ok(authorNote.includes('Không khả dụng'));

      // 4. Verify Vocabulary List and exact order
      const vocabItems = page.locator('#reviewVocabList > li');
      const count = await vocabItems.count();
      assert.strictEqual(count, 3);

      const firstHanzi = await vocabItems.nth(0).locator('.display-5').textContent();
      const firstOrder = await vocabItems.nth(0).locator('.badge.bg-dark').textContent();
      assert.strictEqual(firstHanzi.trim(), '你');
      assert.strictEqual(firstOrder.trim(), '#1');

      const secondHanzi = await vocabItems.nth(1).locator('.display-5').textContent();
      const secondOrder = await vocabItems.nth(1).locator('.badge.bg-dark').textContent();
      assert.strictEqual(secondHanzi.trim(), '好');
      assert.strictEqual(secondOrder.trim(), '#2');

      const thirdHanzi = await vocabItems.nth(2).locator('.display-5').textContent();
      const thirdOrder = await vocabItems.nth(2).locator('.badge.bg-dark').textContent();
      assert.strictEqual(thirdHanzi.trim(), '再见');
      assert.strictEqual(thirdOrder.trim(), '#3');

      // 5. Verify Back to Queue Link
      const backLink = page.locator('#backToQueueBtn');
      const href = await backLink.getAttribute('href');
      assert.strictEqual(href, 'moderator-queue.html');
    } finally {
      await context.close();
    }
  });

  // MR-02: Audio inspection control behavior
  test('MR-02: Audio control displays active button when audioUrl is present, disabled state when missing', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupModeratorSession(page, 'Moderator');
      const mockDetail = createMockLessonDetail(101);

      await page.route(/\/api\/v1\/moderator\/lessons\/101$/, async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: mockDetail })
        });
      });

      await page.goto(`${baseUrl}/moderator-review.html?id=101`, { waitUntil: 'domcontentloaded' });

      // Item 1 (vocabId 10) has audioUrl -> button #btn-audio-10
      const audioBtn10 = page.locator('#btn-audio-10');
      await audioBtn10.waitFor({ state: 'visible', timeout: 5000 });
      const btnText = await audioBtn10.textContent();
      assert.ok(btnText.includes('Nghe phát âm'));

      // Item 2 (vocabId 11) has no audioUrl -> disabled badge
      const item11 = page.locator('#vocab-review-item-11');
      const noAudioBadge = item11.locator('text=Không có phát âm');
      await noAudioBadge.waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await noAudioBadge.isVisible());

      // Click audio button to verify interaction
      await audioBtn10.click();
      // Button remains accessible
      assert.ok(await audioBtn10.isEnabled());
    } finally {
      await context.close();
    }
  });

  // MR-03: Lazy Constituent Radicals Inspection
  test('MR-03: Clicking radical toggle lazy loads constituent radicals from GET /api/v1/vocabulary/{id}', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupModeratorSession(page, 'Moderator');
      const mockDetail = createMockLessonDetail(101);

      await page.route(/\/api\/v1\/moderator\/lessons\/101$/, async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: mockDetail })
        });
      });

      // Mock GET /api/v1/vocabulary/10 (radicals for 你)
      await page.route(/\/api\/v1\/vocabulary\/10$/, async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: {
              vocabId: 10,
              hanzi: '你',
              radicals: [
                {
                  radicalId: 9,
                  character: '亻',
                  pinyin: 'rén',
                  meaningHanViet: 'Nhân',
                  meaningVi: 'Bộ nhân đứng'
                }
              ]
            }
          })
        });
      });

      await page.goto(`${baseUrl}/moderator-review.html?id=101`, { waitUntil: 'domcontentloaded' });

      const toggleBtn = page.locator('#btn-radicals-toggle-10');
      await toggleBtn.waitFor({ state: 'visible', timeout: 5000 });

      // Before click: panel is hidden
      const panel = page.locator('#radicals-panel-10');
      assert.strictEqual(await toggleBtn.getAttribute('aria-expanded'), 'false');
      assert.ok(await panel.evaluate(el => el.classList.contains('d-none')));

      // Click to expand disclosure
      await toggleBtn.click();
      assert.strictEqual(await toggleBtn.getAttribute('aria-expanded'), 'true');
      assert.ok(await panel.evaluate(el => !el.classList.contains('d-none')));

      // Verify radical card is rendered
      const radicalChar = panel.locator('text=亻');
      await radicalChar.waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await radicalChar.isVisible());

      const radicalHanViet = panel.locator('text=Hán-Việt: Nhân');
      assert.ok(await radicalHanViet.isVisible());
    } finally {
      await context.close();
    }
  });

  // MR-04: Stale Queue / Concurrency 404 Recovery
  test('MR-04: Displays friendly stale queue notice with return-to-queue CTA on 404', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupModeratorSession(page, 'Moderator');

      // Mock 404 response for processed lesson
      await page.route(/\/api\/v1\/moderator\/lessons\/999$/, async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'NOT_FOUND',
            message: 'Không tìm thấy bài học với ID: 999',
            data: null
          })
        });
      });

      await page.goto(`${baseUrl}/moderator-review.html?id=999`, { waitUntil: 'domcontentloaded' });

      const staleNotice = page.locator('text=Bài học không còn trong hàng đợi kiểm duyệt hoặc không tồn tại.');
      await staleNotice.waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await staleNotice.isVisible());

      const returnLink = page.locator('#staleReturnToQueueBtn');
      await returnLink.waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await returnLink.isVisible());
    } finally {
      await context.close();
    }
  });

  // MR-05: Role guard blocks unauthorized roles
  test('MR-05: Role guard blocks Learner role and renders unauthorized warning', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupModeratorSession(page, 'Learner');

      await page.goto(`${baseUrl}/moderator-review.html?id=101`, { waitUntil: 'domcontentloaded' });

      const guardContainer = page.locator('#moderatorAuthGuardContainer');
      await guardContainer.waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await guardContainer.isVisible());

      const reviewSection = page.locator('#moderatorReviewSection');
      assert.ok(await reviewSection.evaluate(el => el.classList.contains('d-none')));
    } finally {
      await context.close();
    }
  });

  // MR-06: Approve workflow with optional note, 200 OK, and authoritative status update
  test('MR-06: Moderator approves Pending lesson -> modal -> note -> 200 OK -> status updates to Approved', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupModeratorSession(page, 'Moderator');
      const mockDetail = createMockLessonDetail(101);
      let approvePayload = null;

      // Mock GET /api/v1/moderator/lessons/101
      await page.route(/\/api\/v1\/moderator\/lessons\/101$/, async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thao tác thành công',
            data: mockDetail
          })
        });
      });

      // Mock POST /api/v1/moderator/lessons/101/approve
      await page.route(/\/api\/v1\/moderator\/lessons\/101\/approve$/, async route => {
        approvePayload = JSON.parse(route.request().postData());
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Phê duyệt bài học thành công',
            data: {
              ...mockDetail,
              status: 'Approved'
            }
          })
        });
      });

      await page.goto(`${baseUrl}/moderator-review.html?id=101`, { waitUntil: 'domcontentloaded' });

      // Action bar should be visible initially for Pending lesson
      const btnApprove = page.locator('#btnOpenApproveModal');
      await btnApprove.waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await btnApprove.isVisible());

      // Open approve dialog
      await btnApprove.click();
      const approveModal = page.locator('#approveModal');
      await approveModal.waitFor({ state: 'visible', timeout: 5000 });

      // Type approval note
      const noteInput = page.locator('#approveNoteInput');
      await noteInput.fill('Bài học rất tốt, phê duyệt xuất bản.');

      const noteCount = page.locator('#approveNoteCount');
      assert.ok((await noteCount.textContent()).includes('36 / 500'));

      // Confirm approval
      const btnConfirm = page.locator('#btnConfirmApprove');
      await Promise.all([
        page.waitForResponse(/\/api\/v1\/moderator\/lessons\/101\/approve$/),
        btnConfirm.click()
      ]);

      // Verify server payload
      assert.deepStrictEqual(approvePayload, {
        note: 'Bài học rất tốt, phê duyệt xuất bản.'
      });

      // Verify authoritative status update to Approved
      const statusBadge = page.locator('#reviewLessonStatusBadge');
      await statusBadge.waitFor({ state: 'visible', timeout: 5000 });
      assert.strictEqual(await statusBadge.textContent(), 'Đã duyệt (Approved)');

      // Verify moderation actions are now disabled/hidden
      const actionBar = page.locator('#moderationActionBar');
      assert.ok(await actionBar.evaluate(el => el.classList.contains('d-none')));

      // Verify return to queue CTA
      const returnBtn = page.locator('#btnSuccessReturnQueue');
      await returnBtn.waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await returnBtn.isVisible());
    } finally {
      await context.close();
    }
  });

  // MR-07: Reject workflow with required reason validation, flagged fields, and status update
  test('MR-07: Moderator rejects Pending lesson -> validates reason -> flagged fields -> 200 OK -> status updates to Rejected', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupModeratorSession(page, 'Moderator');
      const mockDetail = createMockLessonDetail(101);
      let rejectPayload = null;

      // Mock GET /api/v1/moderator/lessons/101
      await page.route(/\/api\/v1\/moderator\/lessons\/101$/, async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thao tác thành công',
            data: mockDetail
          })
        });
      });

      // Mock POST /api/v1/moderator/lessons/101/reject
      await page.route(/\/api\/v1\/moderator\/lessons\/101\/reject$/, async route => {
        rejectPayload = JSON.parse(route.request().postData());
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Từ chối bài học thành công',
            data: {
              ...mockDetail,
              status: 'Rejected'
            }
          })
        });
      });

      await page.goto(`${baseUrl}/moderator-review.html?id=101`, { waitUntil: 'domcontentloaded' });

      const btnReject = page.locator('#btnOpenRejectModal');
      await btnReject.waitFor({ state: 'visible', timeout: 5000 });
      await btnReject.click();

      const rejectModal = page.locator('#rejectModal');
      await rejectModal.waitFor({ state: 'visible', timeout: 5000 });

      // Attempt submit without reason -> should trigger validation error
      const btnConfirm = page.locator('#btnConfirmReject');
      await btnConfirm.click();

      const errorMsg = page.locator('#rejectReasonError');
      await errorMsg.waitFor({ state: 'visible', timeout: 3000 });
      assert.ok(await errorMsg.isVisible());
      assert.ok(rejectPayload === null); // Request was not sent

      // Now enter valid rejection reason
      const reasonInput = page.locator('#rejectReasonInput');
      await reasonInput.fill('Phát âm mẫu chưa chuẩn, vui lòng ghi âm lại.');

      // Check flagged fields: pinyin and audioUrl
      const cbPinyin = page.locator('input[name="flaggedFields"][value="pinyin"]');
      await cbPinyin.check();
      const cbAudio = page.locator('input[name="flaggedFields"][value="audioUrl"]');
      await cbAudio.check();

      // Submit rejection
      await Promise.all([
        page.waitForResponse(/\/api\/v1\/moderator\/lessons\/101\/reject$/),
        btnConfirm.click()
      ]);

      // Verify serialized payload matches backend contract
      assert.strictEqual(rejectPayload.rejectionReason, 'Phát âm mẫu chưa chuẩn, vui lòng ghi âm lại.');
      assert.strictEqual(rejectPayload.flaggedFields, '["pinyin","audioUrl"]');

      // Verify authoritative status update to Rejected
      const statusBadge = page.locator('#reviewLessonStatusBadge');
      await statusBadge.waitFor({ state: 'visible', timeout: 5000 });
      assert.strictEqual(await statusBadge.textContent(), 'Bị từ chối (Rejected)');

      // Verify moderation actions are now disabled/hidden
      const actionBar = page.locator('#moderationActionBar');
      assert.ok(await actionBar.evaluate(el => el.classList.contains('d-none')));

      // Verify return to queue CTA
      const returnBtn = page.locator('#btnSuccessReturnQueue');
      await returnBtn.waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await returnBtn.isVisible());
    } finally {
      await context.close();
    }
  });

  // MR-08: Concurrency 409 Conflict handling during moderation
  test('MR-08: Concurrency 409 Conflict on Approve displays truthful conflict alert without faking success', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupModeratorSession(page, 'Moderator');
      const mockDetail = createMockLessonDetail(101);

      // Mock GET /api/v1/moderator/lessons/101
      await page.route(/\/api\/v1\/moderator\/lessons\/101$/, async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thao tác thành công',
            data: mockDetail
          })
        });
      });

      // Mock POST /api/v1/moderator/lessons/101/approve -> returns 409 Conflict
      await page.route(/\/api\/v1\/moderator\/lessons\/101\/approve$/, async route => {
        await route.fulfill({
          status: 409,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'CONFLICT',
            message: 'Bài học đã được phê duyệt trước đó bởi kiểm duyệt viên khác',
            data: null
          })
        });
      });

      await page.goto(`${baseUrl}/moderator-review.html?id=101`, { waitUntil: 'domcontentloaded' });

      const btnApprove = page.locator('#btnOpenApproveModal');
      await btnApprove.waitFor({ state: 'visible', timeout: 5000 });
      await btnApprove.click();

      const btnConfirm = page.locator('#btnConfirmApprove');
      await Promise.all([
        page.waitForResponse(/\/api\/v1\/moderator\/lessons\/101\/approve$/),
        btnConfirm.click()
      ]);

      // Conflict alert must be displayed
      const conflictAlert = page.locator('#moderationStatusAlert');
      await conflictAlert.waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await conflictAlert.isVisible());
      assert.ok((await conflictAlert.textContent()).includes('409 Conflict'));

      // Status badge must NOT be faked as Approved
      const statusBadge = page.locator('#reviewLessonStatusBadge');
      assert.notStrictEqual(await statusBadge.textContent(), 'Đã duyệt (Approved)');

      // Return CTA must be provided
      const returnBtn = page.locator('#btnConflictReturnQueue');
      await returnBtn.waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await returnBtn.isVisible());
    } finally {
      await context.close();
    }
  });

});
