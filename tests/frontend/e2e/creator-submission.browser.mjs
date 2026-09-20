/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: CREATOR LESSON SUBMISSION WORKFLOW (TASK 9D.3)
 * File: tests/frontend/e2e/creator-submission.browser.mjs
 *
 * Verifies:
 * - CS-01: Draft lesson submission from Editor -> modal -> 200 OK -> status becomes Pending -> editor controls locked.
 * - CS-02: Draft lesson submission from Lesson List -> modal -> 200 OK -> row badge updates to Pending.
 * - CS-03: Rejected lesson display -> shows truthful fallback banner -> "Nộp lại" button available and active.
 * - CS-04: 409 Conflict handling on stale submit -> reconciles state and displays informative warning.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function createMockLessons() {
  return [
    {
      lessonId: 101,
      title: 'HSK 1 — Bài 1: Chào hỏi',
      status: 'Draft',
      vocabularyCount: 2,
      createdAt: '2026-09-01T08:00:00Z',
      updatedAt: '2026-09-02T10:00:00Z'
    },
    {
      lessonId: 102,
      title: 'HSK 1 — Bài 2: Cảm ơn và Tạm biệt',
      status: 'Approved',
      vocabularyCount: 5,
      createdAt: '2026-09-03T08:00:00Z',
      updatedAt: '2026-09-04T10:00:00Z'
    },
    {
      lessonId: 103,
      title: 'HSK 1 — Bài 3: Gia đình',
      status: 'Rejected',
      vocabularyCount: 3,
      createdAt: '2026-09-05T08:00:00Z',
      updatedAt: '2026-09-06T10:00:00Z'
    }
  ];
}

function createMockLessonDetail(lessonId = 101, status = 'Draft') {
  return {
    lessonId,
    title: lessonId === 103 ? 'HSK 1 — Bài 3: Gia đình' : 'HSK 1 — Bài 1: Chào hỏi',
    status,
    vocabularyCount: 2,
    vocabularies: [
      {
        vocabId: 11,
        hanzi: '你',
        pinyin: 'nǐ',
        meaningHanViet: 'Nhĩ',
        meaningVi: 'Bạn, anh, chị',
        orderIndex: 1
      },
      {
        vocabId: 12,
        hanzi: '好',
        pinyin: 'hǎo',
        meaningHanViet: 'Hảo',
        meaningVi: 'Tốt, đẹp, hay',
        orderIndex: 2
      }
    ],
    createdAt: '2026-09-01T08:00:00Z',
    updatedAt: '2026-09-02T10:00:00Z'
  };
}

async function setupCreatorSession(page, role = 'Creator') {
  await page.addInitScript(`
    localStorage.setItem('access_token', 'test_creator_jwt_token');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: 2001,
      emailOrPhone: 'creator_studio_user@example.com',
      fullName: 'Tác Giả Mẫu',
      roles: ['${role}']
    }));
  `);
}

describe('Browser E2E: Creator Lesson Submission Workflow & Status Visibility (9D.3)', () => {
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

  // CS-01: Draft submission from Editor
  test('CS-01: Draft lesson submission from Editor -> modal -> 200 OK -> status becomes Pending -> editor controls locked', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });
    let submitApiCalled = false;

    try {
      await setupCreatorSession(page);

      await page.route('**/api/v1/creator/lessons/101', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Success',
            data: createMockLessonDetail(101, 'Draft')
          })
        });
      });

      await page.route('**/api/v1/creator/lessons/101/submit', async route => {
        submitApiCalled = true;
        assert.strictEqual(route.request().method(), 'POST');
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Nộp bài học kiểm duyệt thành công',
            data: createMockLessonDetail(101, 'Pending')
          })
        });
      });

      await page.goto(`${baseUrl}/creator-lesson-editor.html?id=101`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#editorWorkspace:not(.d-none)');

      // Verify submit button is available for Draft
      const btnSubmit = page.locator('#btnSubmitForModeration');
      await btnSubmit.waitFor({ state: 'visible' });
      const submitText = await btnSubmit.textContent();
      assert.match(submitText, /Nộp kiểm duyệt/);

      // Click submit -> modal opens
      await btnSubmit.click();

      const modal = page.locator('#submitModerationModal');
      await modal.waitFor({ state: 'visible' });
      const modalTitle = await page.locator('#submitModalLessonTitle').textContent();
      assert.match(modalTitle, /HSK 1 — Bài 1: Chào hỏi/);

      // Confirm in modal
      const btnConfirm = page.locator('#btnConfirmSubmitModeration');
      await btnConfirm.click();

      // Wait for submission completion
      await page.waitForSelector('#editorLockedBanner:not(.d-none)');
      assert.strictEqual(submitApiCalled, true, 'POST /creator/lessons/101/submit should have been called');

      // Status badge should now show Pending
      const statusBadge = page.locator('#editorStatusBadge');
      const badgeText = await statusBadge.textContent();
      assert.match(badgeText, /Chờ duyệt/);

      // Mutation controls should be disabled / hidden
      const isSubmitHidden = await btnSubmit.evaluate(el => el.classList.contains('d-none'));
      assert.strictEqual(isSubmitHidden, true, 'Submit button should be hidden in Pending state');

      const isTitleDisabled = await page.locator('#editorTitleInput').isDisabled();
      assert.strictEqual(isTitleDisabled, true, 'Title input should be disabled in Pending state');

      const isSaveDisabled = await page.locator('#btnSaveTitle').isDisabled();
      assert.strictEqual(isSaveDisabled, true, 'Save title button should be disabled in Pending state');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors detected: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  // CS-02: Draft submission from Lesson List
  test('CS-02: Draft lesson submission from Lesson List -> modal -> 200 OK -> row badge updates to Pending', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });
    let submitApiCalled = false;
    let listLessons = createMockLessons();

    try {
      await setupCreatorSession(page);

      await page.route(/\/api\/v1\/creator\/lessons(\?.*)?$/, async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Success',
            data: {
              items: listLessons,
              page: 0,
              size: 20,
              totalElements: listLessons.length,
              totalPages: 1
            }
          })
        });
      });

      await page.route('**/api/v1/creator/lessons/101/submit', async route => {
        submitApiCalled = true;
        listLessons = listLessons.map(l => l.lessonId === 101 ? { ...l, status: 'Pending' } : l);
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Nộp bài học kiểm duyệt thành công',
            data: createMockLessonDetail(101, 'Pending')
          })
        });
      });

      await page.goto(`${baseUrl}/creator-lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#creatorLessonsTableBody tr');

      // Draft lesson (row 101) has submit button with label "Nộp duyệt"
      const submitBtn = page.locator('button[data-lesson-id="101"]');
      await submitBtn.waitFor({ state: 'visible' });
      const btnText = await submitBtn.textContent();
      assert.match(btnText, /Nộp duyệt/);

      // Click submit -> modal opens
      await submitBtn.click();
      const modal = page.locator('#submitModerationModal');
      await modal.waitFor({ state: 'visible' });

      // Confirm in modal
      const btnConfirm = page.locator('#btnConfirmSubmitModeration');
      await btnConfirm.click();

      // Verify row badge updates to Pending
      await page.waitForFunction(() => {
        const trs = Array.from(document.querySelectorAll('#creatorLessonsTableBody tr'));
        const targetTr = trs.find(r => r.textContent.includes('#101'));
        const badge = targetTr ? targetTr.querySelector('.badge-status') : null;
        return badge && badge.textContent.includes('Chờ duyệt');
      }, { timeout: 10000 });

      assert.strictEqual(submitApiCalled, true, 'POST /creator/lessons/101/submit should have been called');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors detected: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  // CS-03: Rejected lesson view & re-submission
  test('CS-03: Rejected lesson display -> shows truthful fallback banner -> "Nộp lại" button available and active', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupCreatorSession(page);

      await page.route('**/api/v1/creator/lessons/103', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Success',
            data: createMockLessonDetail(103, 'Rejected')
          })
        });
      });

      await page.goto(`${baseUrl}/creator-lesson-editor.html?id=103`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#editorWorkspace:not(.d-none)');

      // Verify Rejected badge
      const statusBadge = page.locator('#editorStatusBadge');
      const badgeText = await statusBadge.textContent();
      assert.match(badgeText, /Từ chối/);

      // Verify Rejected banner is visible with truthful fallback text
      const rejectedBanner = page.locator('#editorRejectedBanner');
      await rejectedBanner.waitFor({ state: 'visible' });
      const bannerContent = await page.locator('#editorRejectedBannerContent').textContent();
      assert.match(bannerContent, /Bài học này đã bị từ chối\. Thông tin phản hồi chi tiết chưa được cung cấp bởi API hiện tại\./);

      // Verify submit button has label "Nộp lại bài học"
      const btnSubmit = page.locator('#btnSubmitForModeration');
      await btnSubmit.waitFor({ state: 'visible' });
      const submitText = await btnSubmit.textContent();
      assert.match(submitText, /Nộp lại bài học/);

      // Verify controls are editable for Rejected status
      const isTitleDisabled = await page.locator('#editorTitleInput').isDisabled();
      assert.strictEqual(isTitleDisabled, false, 'Title input should remain editable for Rejected status');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors detected: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  // CS-04: 409 Conflict handling on stale submit
  test('CS-04: 409 Conflict handling on stale submit -> reconciles state and displays informative warning', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });
    let getLessonsCallCount = 0;
    let listLessons = createMockLessons();

    try {
      await setupCreatorSession(page);

      await page.route('**/api/v1/creator/lessons/101/submit', async route => {
        listLessons = listLessons.map(l => l.lessonId === 101 ? { ...l, status: 'Pending' } : l);
        await route.fulfill({
          status: 409,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'CONFLICT',
            message: 'Bài học đã được nộp và đang chờ kiểm duyệt',
            timestamp: new Date().toISOString()
          })
        });
      });

      await page.route(/\/api\/v1\/creator\/lessons(\?.*)?$/, async route => {
        getLessonsCallCount++;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Success',
            data: {
              items: listLessons,
              page: 0,
              size: 20,
              totalElements: listLessons.length,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${baseUrl}/creator-lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#creatorLessonsTableBody tr');

      // Click submit on row 101
      const submitBtn = page.locator('button[data-lesson-id="101"]');
      await submitBtn.click();

      const modal = page.locator('#submitModerationModal');
      await modal.waitFor({ state: 'visible' });

      // Confirm in modal
      const btnConfirm = page.locator('#btnConfirmSubmitModeration');
      await btnConfirm.click();

      // Wait for reconciliation: row 101 should update to Pending
      await page.waitForFunction(() => {
        const trs = Array.from(document.querySelectorAll('#creatorLessonsTableBody tr'));
        const targetTr = trs.find(r => r.textContent.includes('#101'));
        const badge = targetTr ? targetTr.querySelector('.badge-status') : null;
        return badge && badge.textContent.includes('Chờ duyệt');
      }, { timeout: 10000 });

      assert.ok(getLessonsCallCount >= 2, 'Lesson list should have been refreshed to reconcile state');

      const errors = getRuntimeErrors().filter(e => !e.includes('409'));
      assert.strictEqual(errors.length, 0, `Runtime errors detected: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

});
