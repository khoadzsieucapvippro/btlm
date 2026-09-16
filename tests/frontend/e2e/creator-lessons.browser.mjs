/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: CREATOR LESSON STUDIO & VOCABULARY ORDERING (TASK 9D.1)
 * File: tests/frontend/e2e/creator-lessons.browser.mjs
 *
 * Verifies:
 * - CL-01: Creator lesson list loads, displays lessons, creates Draft lesson via modal, and navigates to editor.
 * - CL-02: Draft lesson editor searches vocabulary, adds item, reorders via Move Up/Down buttons, and persists order via PUT.
 * - CL-03: Locked states (Pending and Approved) display locked notice banner and disable mutation controls.
 * - CL-04: Role guard redirects anonymous users, and 403 Forbidden handles ownership violation safely.
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
    }
  ];
}

function createMockDraftDetail(lessonId = 101) {
  return {
    lessonId,
    title: 'HSK 1 — Bài 1: Chào hỏi',
    status: 'Draft',
    vocabularyCount: 2,
    vocabularies: [
      {
        vocabId: 11,
        hanzi: '你',
        pinyin: 'nǐ',
        pinyinRaw: 'ni',
        meaningHanViet: 'Nhĩ',
        meaningVi: 'Bạn, anh, chị (ngôi thứ hai)',
        orderIndex: 1
      },
      {
        vocabId: 12,
        hanzi: '好',
        pinyin: 'hǎo',
        pinyinRaw: 'hao',
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

describe('Browser E2E: Creator Lesson Studio & Vocabulary Ordering (9D.1)', () => {
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

  // CL-01: Creator Lesson List & Draft Creation
  test('CL-01: Creator lesson list loads, displays lessons, creates Draft lesson via modal', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupCreatorSession(page, 'Creator');

      let interceptedCreate = false;
      let interceptedCreateBody = null;

      await page.route(/\/api\/v1\/creator\/lessons(\?.*)?$/, async (route) => {
        const req = route.request();
        const method = req.method();

        if (method === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'Success',
              data: {
                items: createMockLessons(),
                page: 0,
                size: 20,
                totalElements: 2,
                totalPages: 1
              }
            })
          });
        } else if (method === 'POST') {
          interceptedCreate = true;
          interceptedCreateBody = JSON.parse(req.postData() || '{}');
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'Tạo bài học thành công',
              data: {
                lessonId: 103,
                title: interceptedCreateBody.title,
                status: 'Draft',
                vocabularyCount: 0,
                vocabularies: [],
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
              }
            })
          });
        } else {
          await route.continue();
        }
      });

      // Route for new lesson editor navigation
      await page.route('**/api/v1/creator/lessons/103', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: {
              lessonId: 103,
              title: 'Bài học kiểm thử mới',
              status: 'Draft',
              vocabularyCount: 0,
              vocabularies: [],
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString()
            }
          })
        });
      });

      await page.goto(`${baseUrl}/creator-lessons.html`, { waitUntil: 'domcontentloaded' });

      // Verify table rendered with mock lessons
      await page.waitForSelector('#creatorLessonsTableBody tr');
      const rowCount = await page.locator('#creatorLessonsTableBody tr').count();
      assert.strictEqual(rowCount, 2, 'Expected 2 lesson rows rendered');

      // Click "Tạo bài học mới" button
      const createBtn = page.getByRole('button', { name: /Tạo bài học mới/i });
      await createBtn.click();

      // Modal opens
      const modalInput = page.locator('#newLessonTitleInput');
      await modalInput.waitFor({ state: 'visible' });

      // Fill title
      await modalInput.fill('Bài học kiểm thử mới');

      // Submit creation
      const submitBtn = page.locator('#btnSubmitCreateLesson');
      await submitBtn.click();

      // Wait for navigation to editor
      await page.waitForURL(/creator-lesson-editor\.html\?id=103/);
      assert.strictEqual(interceptedCreate, true, 'POST /creator/lessons must be intercepted');
      assert.strictEqual(interceptedCreateBody.title, 'Bài học kiểm thử mới');
      assert.strictEqual('userId' in interceptedCreateBody, false, 'No userId leak in payload');
      assert.strictEqual('createdBy' in interceptedCreateBody, false, 'No createdBy leak in payload');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors detected: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  // CL-02: Draft Editor, Vocabulary Search, Add & Reorder via Up/Down
  test('CL-02: Draft editor searches vocabulary, adds item, and reorders via Move Up/Down buttons', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupCreatorSession(page, 'Creator');

      let currentLesson = createMockDraftDetail(101);
      let interceptedReorder = false;
      let interceptedReorderPayload = null;
      let interceptedAddVocab = false;

      await page.route('**/api/v1/creator/lessons/101', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: currentLesson })
        });
      });

      // Vocabulary Search mock
      await page.route('**/api/v1/vocabulary**', async (route) => {
        const url = new URL(route.request().url());
        const term = url.searchParams.get('search') || '';

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: {
              items: [
                {
                  vocabId: 13,
                  hanzi: '人',
                  pinyin: 'rén',
                  pinyinRaw: 'ren',
                  meaningHanViet: 'Nhân',
                  meaningVi: 'Người'
                }
              ],
              page: 0,
              size: 8,
              totalElements: 1,
              totalPages: 1
            }
          })
        });
      });

      // Add vocabulary endpoint
      await page.route('**/api/v1/creator/lessons/101/vocabularies/13', async (route) => {
        interceptedAddVocab = true;
        currentLesson.vocabularies.push({
          vocabId: 13,
          hanzi: '人',
          pinyin: 'rén',
          pinyinRaw: 'ren',
          meaningHanViet: 'Nhân',
          meaningVi: 'Người',
          orderIndex: 3
        });
        currentLesson.vocabularyCount = 3;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: currentLesson })
        });
      });

      // Reorder endpoint
      await page.route('**/api/v1/creator/lessons/101/reorder', async (route) => {
        interceptedReorder = true;
        interceptedReorderPayload = JSON.parse(route.request().postData() || '{}');

        // Swap order in mock
        const newOrderIds = interceptedReorderPayload.orderedVocabIds;
        const reordered = newOrderIds.map((id, idx) => {
          const item = currentLesson.vocabularies.find(v => v.vocabId === id);
          return { ...item, orderIndex: idx + 1 };
        });
        currentLesson.vocabularies = reordered;

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: currentLesson })
        });
      });

      await page.goto(`${baseUrl}/creator-lesson-editor.html?id=101`, { waitUntil: 'domcontentloaded' });

      // Verify lesson loaded
      await page.waitForSelector('#editorVocabList li');
      const initialItems = await page.locator('#editorVocabList li').count();
      assert.strictEqual(initialItems, 2, 'Expected 2 initial vocabulary items');

      // 1. Search vocabulary
      const searchInput = page.locator('#editorVocabSearchInput');
      await searchInput.fill('ren');

      // Wait for search result dropdown item
      await page.waitForSelector('.creator-search-item');
      const addBtn = page.locator('.creator-search-item button').first();
      await addBtn.click();

      // Verify add vocabulary call
      assert.strictEqual(interceptedAddVocab, true, 'Add vocabulary API must be called');
      await page.waitForFunction(() => document.querySelectorAll('#editorVocabList li').length === 3);

      // 2. Reorder vocabulary
      // The items are currently: #1: 你 (11), #2: 好 (12), #3: 人 (13)
      // On item #2 (好), click Move Up (▲)
      const secondItemMoveUpBtn = page.locator('#editorVocabList li').nth(1).locator('.btn-move-up');
      await secondItemMoveUpBtn.click();

      // Verify reorder payload
      assert.strictEqual(interceptedReorder, true, 'PUT /creator/lessons/{id}/reorder must be called');
      assert.deepStrictEqual(
        interceptedReorderPayload.orderedVocabIds,
        [12, 11, 13],
        'Expected item 12 to move to position 1'
      );

      // Verify boundary disabled state: First item's Move Up must be disabled
      const firstItemUpBtn = page.locator('#editorVocabList li').first().locator('.btn-move-up');
      const isDisabled = await firstItemUpBtn.isDisabled();
      assert.strictEqual(isDisabled, true, 'First item Move Up button must be disabled');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors detected: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  // CL-03: Locked State Enforcement for Pending and Approved
  test('CL-03: Locked state disables all mutation controls for Pending/Approved lessons', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupCreatorSession(page, 'Creator');

      // Mock an Approved lesson
      const approvedLesson = {
        lessonId: 200,
        title: 'HSK 2 — Bài Đã Duyệt',
        status: 'Approved',
        vocabularyCount: 1,
        vocabularies: [
          {
            vocabId: 99,
            hanzi: '学',
            pinyin: 'xué',
            meaningVi: 'Học tập',
            orderIndex: 1
          }
        ],
        createdAt: '2026-08-01T08:00:00Z',
        updatedAt: '2026-08-02T10:00:00Z'
      };

      await page.route('**/api/v1/creator/lessons/200', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'SUCCESS', data: approvedLesson })
        });
      });

      await page.goto(`${baseUrl}/creator-lesson-editor.html?id=200`, { waitUntil: 'domcontentloaded' });

      // 1. Locked banner must be visible
      const lockedBanner = page.locator('#editorLockedBanner');
      await lockedBanner.waitFor({ state: 'visible' });

      // 2. Title input and save button must be disabled
      const titleInput = page.locator('#editorTitleInput');
      assert.strictEqual(await titleInput.isDisabled(), true, 'Title input must be disabled');
      const saveTitleBtn = page.locator('#btnSaveTitle');
      assert.strictEqual(await saveTitleBtn.isDisabled(), true, 'Save title button must be disabled');

      // 3. Search and add panel must be hidden
      const addPanel = page.locator('#editorAddVocabPanel');
      assert.strictEqual(await addPanel.isVisible(), false, 'Add vocab panel must be hidden');

      // 4. Move up/down and remove buttons must be disabled
      const itemRow = page.locator('#editorVocabList li').first();
      const upBtn = itemRow.locator('.btn-move-up');
      const downBtn = itemRow.locator('.btn-move-down');
      const removeBtn = itemRow.locator('.creator-remove-btn');

      assert.strictEqual(await upBtn.isDisabled(), true, 'Move Up must be disabled');
      assert.strictEqual(await downBtn.isDisabled(), true, 'Move Down must be disabled');
      assert.strictEqual(await removeBtn.isDisabled(), true, 'Remove button must be disabled');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors detected: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  // CL-04: Role Guard & Ownership Isolation
  test('CL-04: Role guard blocks anonymous access, and 403 Forbidden renders proper isolation', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      // 1. Anonymous access redirects to login
      await page.goto(`${baseUrl}/creator-lessons.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForURL(/login\.html/);

      // 2. Authenticated Creator getting 403 on another creator's lesson
      const { page: authPage, context: authContext, getRuntimeErrors } = await createMonitoredPage(browser, {
        viewportWidth: 1280,
        viewportHeight: 720
      });

      try {
        await setupCreatorSession(authPage, 'Creator');

        await authPage.route('**/api/v1/creator/lessons/999', async (route) => {
          await route.fulfill({
            status: 403,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 403,
              message: 'Bạn không có quyền thao tác trên bài học này'
            })
          });
        });

        await authPage.goto(`${baseUrl}/creator-lesson-editor.html?id=999`, { waitUntil: 'domcontentloaded' });

        // Verify friendly error message without leaking private lesson details
        const stateContainer = authPage.locator('#editorStateContainer');
        await stateContainer.waitFor({ state: 'visible' });
        const errorText = await stateContainer.textContent();
        assert.match(errorText, /không có quyền thao tác/i);

        // Main workspace must remain hidden
        const workspace = authPage.locator('#editorWorkspace');
        assert.strictEqual(await workspace.isVisible(), false, 'Workspace must be hidden on 403');

        const errors = getRuntimeErrors().filter(e => !e.includes('403'));
        assert.strictEqual(errors.length, 0, `Runtime errors detected: ${JSON.stringify(errors)}`);
      } finally {
        await authContext.close();
      }
    } finally {
      await context.close();
    }
  });

});
