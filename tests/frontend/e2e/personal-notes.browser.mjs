/**
 * =============================================================================
 * BROWSER E2E TEST: CONTEXTUAL PERSONAL NOTES SUBSYSTEM (TASK 9C.2)
 * File: tests/frontend/e2e/personal-notes.browser.mjs
 * 
 * Verifies:
 * - PN-01: Anonymous user clicking note action prompts login confirmation without calling note API.
 * - PN-02: Authenticated user opens notes modal from vocabulary card, displays context and empty state.
 * - PN-03: Live character counter updates dynamically deriving from textarea input up to 500 chars.
 * - PN-04: Note CRUD lifecycle: Create, Edit/Update, and Delete with inline confirmation.
 * - PN-05: Adversarial XSS payload rendered strictly as safe textContent without DOM execution.
 * - PN-06: Modal keyboard accessibility (Escape close) and focus restoration to triggering button.
 * - PN-07: Lesson Detail context triggers notes modal for the specific item's vocabId.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function createMockVocabularyItem() {
  return {
    vocabId: 1,
    hanzi: '书',
    pinyin: 'shū',
    meaningHanViet: 'Thư',
    meaningVi: 'Sách, vở',
    audioUrl: null
  };
}

async function setupVocabularyCatalogRoute(page) {
  await page.route(/\/api\/v1\/vocabulary(\?.*)?$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'SUCCESS',
        message: 'OK',
        data: {
          items: [createMockVocabularyItem()],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1
        }
      })
    });
  });
}

async function setupAuthenticatedSession(page) {
  await page.addInitScript(() => {
    localStorage.setItem('access_token', 'test_jwt_notes_user');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: 999,
      emailOrPhone: 'notes_tester@example.com',
      fullName: 'Học Viên Ghi Chú',
      roles: ['Learner']
    }));
  });
}

describe('Browser E2E: Contextual Personal Notes Subsystem (9C.2)', () => {
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

  // PN-01: Anonymous user guard
  test('PN-01: Anonymous user clicking note action prompts login confirmation without API invocation', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupVocabularyCatalogRoute(page);

      let interceptedNotesCall = false;
      await page.route('**/api/v1/vocabularies/*/notes*', async (route) => {
        interceptedNotesCall = true;
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'UNAUTHORIZED', message: 'Unauthorized' })
        });
      });

      await page.goto(`${baseUrl}/vocabulary.html`, { waitUntil: 'domcontentloaded' });

      // Click note button on first card
      const noteBtn = page.locator('.vocab-card-note-btn').first();
      await noteBtn.waitFor({ state: 'visible' });
      await noteBtn.click();

      // Check global confirmation dialog is displayed prompting login
      const confirmModal = page.locator('.modal.show, .modal[role="dialog"]');
      await confirmModal.waitFor({ state: 'visible' });
      const modalText = await confirmModal.textContent();
      assert.ok(
        modalText.includes('Đăng nhập để ghi chú') || modalText.includes('Ghi chú cá nhân'),
        `Expected login prompt in confirm dialog, got: "${modalText}"`
      );

      // Verify zero notes API calls were made
      assert.strictEqual(interceptedNotesCall, false, 'Notes API must strictly NEVER be called for anonymous users');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // PN-02: Authenticated user opens notes modal
  test('PN-02: Authenticated user opens notes modal displaying context and empty state', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      // Mock empty notes list
      await page.route('**/api/v1/vocabularies/1/notes*', async (route) => {
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

      await setupVocabularyCatalogRoute(page);
      await page.goto(`${baseUrl}/vocabulary.html`, { waitUntil: 'domcontentloaded' });

      const noteBtn = page.locator('.vocab-card-note-btn').first();
      await noteBtn.waitFor({ state: 'visible' });
      await noteBtn.click();

      const notesModal = page.locator('#notesModal');
      await notesModal.waitFor({ state: 'visible' });

      // Check vocabulary context header
      const hanziText = await page.locator('#notesContextHanzi').textContent();
      assert.ok(hanziText.length > 0, 'Context Hanzi should be displayed');

      // Check empty state
      const emptyState = page.locator('#notesEmptyState');
      await emptyState.waitFor({ state: 'visible' });
      const emptyText = await emptyState.textContent();
      assert.ok(emptyText.toLowerCase().includes('chưa có ghi chú nào'), `Expected empty message, got: "${emptyText}"`);

      // Check initial counter
      const counterText = await page.locator('#noteCharCounter').textContent();
      assert.ok(counterText.includes('0 / 500'), `Expected '0 / 500', got: "${counterText}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // PN-03: Live character counter updates dynamically
  test('PN-03: Live character counter updates dynamically deriving from textarea input', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

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

      await setupVocabularyCatalogRoute(page);
      await page.goto(`${baseUrl}/vocabulary.html`, { waitUntil: 'domcontentloaded' });
      const noteBtn = page.locator('.vocab-card-note-btn').first();
      await noteBtn.waitFor({ state: 'visible' });
      await noteBtn.click();

      const textarea = page.locator('#noteContentInput');
      await textarea.waitFor({ state: 'visible' });

      // Type 10 characters
      await textarea.fill('1234567890');
      let counter = await page.locator('#noteCharCounter').textContent();
      assert.ok(counter.includes('10 / 500'), `Expected '10 / 500', got: "${counter}"`);

      // Type CJK characters
      await textarea.fill('学习汉语');
      counter = await page.locator('#noteCharCounter').textContent();
      assert.ok(counter.includes('4 / 500'), `Expected '4 / 500', got: "${counter}"`);

      // Fill 500 characters
      const exact500 = 'a'.repeat(500);
      await textarea.fill(exact500);
      counter = await page.locator('#noteCharCounter').textContent();
      assert.ok(counter.includes('500 / 500'), `Expected '500 / 500', got: "${counter}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // PN-04: Note CRUD lifecycle
  test('PN-04: Note CRUD lifecycle: Create, Edit/Update, and Delete with inline confirmation', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      let mockNotes = [];

      // Intercept GET notes
      await page.route('**/api/v1/vocabularies/1/notes*', async (route) => {
        if (route.request().method() === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'OK',
              data: {
                items: mockNotes,
                page: 0,
                size: 20,
                totalElements: mockNotes.length,
                totalPages: 1
              }
            })
          });
        } else if (route.request().method() === 'POST') {
          const body = JSON.parse(route.request().postData());
          const newNote = {
            noteId: 777,
            vocabId: 1,
            content: body.content,
            createdAt: new Date().toISOString()
          };
          mockNotes.unshift(newNote);
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'Tạo ghi chú thành công',
              data: newNote
            })
          });
        }
      });

      // Intercept PUT /api/v1/notes/777
      await page.route('**/api/v1/notes/777', async (route) => {
        if (route.request().method() === 'PUT') {
          const body = JSON.parse(route.request().postData());
          const updatedNote = {
            noteId: 777,
            vocabId: 1,
            content: body.content,
            createdAt: new Date().toISOString()
          };
          mockNotes = [updatedNote];
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'Cập nhật ghi chú thành công',
              data: updatedNote
            })
          });
        } else if (route.request().method() === 'DELETE') {
          mockNotes = [];
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'Xóa ghi chú thành công',
              data: null,
              errors: []
            })
          });
        }
      });

      await setupVocabularyCatalogRoute(page);
      await page.goto(`${baseUrl}/vocabulary.html`, { waitUntil: 'domcontentloaded' });
      const noteBtn = page.locator('.vocab-card-note-btn').first();
      await noteBtn.waitFor({ state: 'visible' });
      await noteBtn.click();

      // 1. CREATE NOTE
      const textarea = page.locator('#noteContentInput');
      await textarea.waitFor({ state: 'visible' });
      await textarea.fill('Ghi chú ban đầu của tôi');

      const submitBtn = page.locator('#noteSubmitBtn');
      await submitBtn.click();

      // Verify created note appears in list
      const noteCard = page.locator('.note-item[data-note-id="777"]');
      await noteCard.waitFor({ state: 'visible' });
      const noteText = await noteCard.locator('.note-content').textContent();
      assert.strictEqual(noteText.trim(), 'Ghi chú ban đầu của tôi');

      // 2. EDIT NOTE
      const editBtn = noteCard.locator('button[data-action="edit"]');
      await editBtn.click();

      // Textarea should now contain note content and submit button text changes
      const currentVal = await textarea.inputValue();
      assert.strictEqual(currentVal, 'Ghi chú ban đầu của tôi');
      const submitText = await submitBtn.textContent();
      assert.ok(submitText.includes('Lưu cập nhật'));

      // Modify and save
      await textarea.fill('Ghi chú đã được chỉnh sửa');
      await submitBtn.click();

      // Verify updated content in list
      await page.waitForTimeout(300);
      const updatedText = await noteCard.locator('.note-content').textContent();
      assert.strictEqual(updatedText.trim(), 'Ghi chú đã được chỉnh sửa');

      // 3. DELETE NOTE
      const deleteBtn = noteCard.locator('button[data-action="delete"]');
      await deleteBtn.click();

      // Inline confirmation box appears
      const confirmBox = noteCard.locator('.note-delete-confirm-box');
      await confirmBox.waitFor({ state: 'visible' });
      assert.ok((await confirmBox.textContent()).includes('Bạn có chắc chắn muốn xóa'));

      // Confirm deletion
      const confirmDeleteBtn = confirmBox.locator('button[data-action="confirm-delete"]');
      await confirmDeleteBtn.click();

      // Note card is removed from DOM and empty state reappears
      await noteCard.waitFor({ state: 'detached' });
      const emptyState = page.locator('#notesEmptyState');
      await emptyState.waitFor({ state: 'visible' });

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // PN-05: Adversarial XSS payload rendered strictly as textContent
  test('PN-05: Adversarial XSS payload rendered strictly as safe textContent without DOM execution', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      const xssNote = {
        noteId: 888,
        vocabId: 1,
        content: '<script>window.__xss_note_exploited=true;</script><b id="maliciousNoteTag">Injected Bold</b>',
        createdAt: '2026-09-14T12:00:00Z'
      };

      await page.route('**/api/v1/vocabularies/1/notes*', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              items: [xssNote],
              page: 0,
              size: 20,
              totalElements: 1,
              totalPages: 1
            }
          })
        });
      });

      await setupVocabularyCatalogRoute(page);
      await page.goto(`${baseUrl}/vocabulary.html`, { waitUntil: 'domcontentloaded' });
      const noteBtn = page.locator('.vocab-card-note-btn').first();
      await noteBtn.waitFor({ state: 'visible' });
      await noteBtn.click();

      const noteCard = page.locator('.note-item[data-note-id="888"]');
      await noteCard.waitFor({ state: 'visible' });

      // 1. Verify malicious tag is NOT rendered as an HTML element
      const tagCount = await page.locator('#maliciousNoteTag').count();
      assert.strictEqual(tagCount, 0, 'Injected HTML tags must strictly NOT be parsed into DOM');

      // 2. Verify script did not execute
      const isExploited = await page.evaluate(() => Boolean(window.__xss_note_exploited));
      assert.strictEqual(isExploited, false, 'Injected script must strictly NOT execute');

      // 3. Verify content is displayed verbatim as text
      const contentEl = noteCard.locator('.note-content');
      const renderedText = await contentEl.textContent();
      assert.strictEqual(renderedText, xssNote.content);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // PN-06: Modal keyboard accessibility and focus restoration
  test('PN-06: Modal closes on Escape and restores focus to triggering button', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

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

      await setupVocabularyCatalogRoute(page);
      await page.goto(`${baseUrl}/vocabulary.html`, { waitUntil: 'domcontentloaded' });
      const noteBtn = page.locator('.vocab-card-note-btn').first();
      await noteBtn.waitFor({ state: 'visible' });
      await noteBtn.focus();
      await noteBtn.click();

      const notesModal = page.locator('#notesModal');
      await notesModal.waitFor({ state: 'visible' });

      // Press Escape key
      await page.keyboard.press('Escape');
      await notesModal.waitFor({ state: 'hidden' });

      // Verify focus is restored to the triggering button
      const isFocused = await noteBtn.evaluate((el) => document.activeElement === el);
      assert.strictEqual(isFocused, true, 'Focus must be restored to triggering button upon modal close');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // PN-07: Lesson Detail integration
  test('PN-07: Lesson Detail context opens notes modal passing correct vocabId', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      // Mock lesson detail with vocabularies
      await page.route('**/api/v1/lessons/1', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              lessonId: 1,
              title: 'Bài 1: Chào hỏi',
              description: 'Mô tả bài học',
              status: 'Approved',
              vocabularies: [
                { vocabId: 50, orderIndex: 1, hanzi: '你', pinyin: 'nǐ', meaningVi: 'Bạn' },
                { vocabId: 60, orderIndex: 2, hanzi: '好', pinyin: 'hǎo', meaningVi: 'Tốt' }
              ]
            }
          })
        });
      });

      let requestedVocabId = null;
      await page.route('**/api/v1/vocabularies/*/notes*', async (route) => {
        const url = route.request().url();
        const match = url.match(/\/vocabularies\/(\d+)\/notes/);
        if (match) {
          requestedVocabId = match[1];
        }
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

      await page.goto(`${baseUrl}/lesson-detail.html?id=1`, { waitUntil: 'domcontentloaded' });
      await page.locator('#lessonVocabList').waitFor({ state: 'visible' });

      // Find note buttons on lesson vocab items
      const noteBtns = page.locator('.lesson-vocab-note-btn');
      assert.strictEqual(await noteBtns.count(), 2, 'Must render note button for both lesson vocab items');

      // Click second vocab note button (vocabId=60)
      await noteBtns.nth(1).click();

      const notesModal = page.locator('#notesModal');
      await notesModal.waitFor({ state: 'visible' });

      // Verify the modal requested notes for vocabId=60
      assert.strictEqual(requestedVocabId, '60', 'Notes modal must request notes for the exact vocabId of the item');

      // Verify modal context shows Hanzi of the second item ('好')
      const hanziText = await page.locator('#notesContextHanzi').textContent();
      assert.strictEqual(hanziText.trim(), '好');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

});
