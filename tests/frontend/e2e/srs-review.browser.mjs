/**
 * =============================================================================
 * BROWSER E2E TEST: INTERACTIVE SRS FLASHCARD REVIEW SESSION (TASK 9C.3)
 * File: tests/frontend/e2e/srs-review.browser.mjs
 * 
 * Verifies:
 * - SR-01: Anonymous user displays login-required state without calling protected SRS API.
 * - SR-02: Authenticated user loads due cards session, renders prompt card, starts reaction timer.
 * - SR-03: 3D Flip interaction toggles between front prompt and back answer (click & Space).
 * - SR-04: Keyboard rating shortcut (1..4) submits authoritative review to backend and advances.
 * - SR-05: Rating Again (1) requeues card to the end of the local session queue.
 * - SR-06: Session completes with truthful statistics (total reviewed, Again, Good/Easy, time).
 * - SR-07: Lesson query mode (?lessonId=10) retrieves new cards from approved lesson.
 * - SR-08: Contextual personal notes modal opens from vocabulary card back-face.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function createMockDueCards() {
  return [
    {
      itemType: 'VOCABULARY',
      itemId: 101,
      hanzi: '学',
      pinyin: 'xué',
      meaningHanViet: 'Học',
      meaningVi: 'Học tập, nghiên cứu',
      exampleSentence: '我在大学学中文。',
      exampleTranslation: 'Tôi học tiếng Trung ở trường đại học.',
      strokeCount: null,
      repetitions: 1,
      easeFactor: 2.50,
      intervalDays: 1,
      nextReviewAt: '2026-09-14T10:00:00',
      isNew: false
    },
    {
      itemType: 'VOCABULARY',
      itemId: 102,
      hanzi: '书',
      pinyin: 'shū',
      meaningHanViet: 'Thư',
      meaningVi: 'Sách, vở',
      exampleSentence: '这是一本好书。',
      exampleTranslation: 'Đây là một cuốn sách hay.',
      strokeCount: null,
      repetitions: 0,
      easeFactor: 2.50,
      intervalDays: 0,
      nextReviewAt: null,
      isNew: true
    }
  ];
}

async function setupAuthenticatedSession(page) {
  await page.addInitScript(() => {
    localStorage.setItem('access_token', 'test_srs_learner_jwt');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: 1001,
      emailOrPhone: 'srs_learner@example.com',
      fullName: 'Học Viên SRS',
      roles: ['Learner']
    }));
  });
}

describe('Browser E2E: Interactive SRS Flashcard Review Session (9C.3)', () => {
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

  // SR-01: Anonymous Guard
  test('SR-01: Anonymous user displays login-required state without calling protected SRS API', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      let interceptedSrsCall = false;
      await page.route('**/api/v1/srs/**', async (route) => {
        interceptedSrsCall = true;
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'UNAUTHORIZED', message: 'Unauthorized' })
        });
      });

      await page.goto(`${baseUrl}/srs-review.html`, { waitUntil: 'domcontentloaded' });

      // Check auth required view
      const authState = page.locator('#srsAuthState');
      await authState.waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await authState.isVisible(), 'Authentication-required container must be visible');

      const loginBtn = page.locator('#btnSrsLoginRedirect');
      assert.ok(await loginBtn.isVisible(), 'Login redirect button must be visible');
      const href = await loginBtn.getAttribute('href');
      assert.ok(href.includes('login.html'), 'Link must direct to login.html');

      assert.strictEqual(interceptedSrsCall, false, 'Frontend must NOT call protected SRS API when unauthenticated');
      assert.strictEqual(getRuntimeErrors().length, 0, 'Zero unexpected runtime console/page errors');
    } finally {
      await context.close();
    }
  });

  // SR-02: Authenticated user loads due cards & starts reaction timer
  test('SR-02: Authenticated user loads due cards session, renders prompt card, starts reaction timer', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      await page.route('**/api/v1/srs/due', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: createMockDueCards()
          })
        });
      });

      await page.goto(`${baseUrl}/srs-review.html`, { waitUntil: 'domcontentloaded' });

      // Active session visible
      const activeSession = page.locator('#srsActiveSession');
      await activeSession.waitFor({ state: 'visible', timeout: 5000 });

      // Check Card 1 prompt
      const frontHanzi = page.locator('#cardFrontHanzi');
      await frontHanzi.waitFor({ state: 'visible' });
      assert.strictEqual(await frontHanzi.textContent(), '学', 'Front face must display Hanzi "学"');

      const frontPinyin = page.locator('#cardFrontPinyin');
      assert.strictEqual(await frontPinyin.textContent(), 'xué', 'Front face must display Pinyin "xué"');

      // Progress bar & counter
      const progressText = page.locator('#srsProgressText');
      assert.strictEqual(await progressText.textContent(), 'Thẻ 1 / 2');

      // Timer badge exists and contains ⏱️
      const timerBadge = page.locator('#reactionTimerBadge');
      assert.ok((await timerBadge.textContent()).includes('⏱️'));

      // Rating buttons must be disabled before card is flipped
      const btnGood = page.locator('#btnRatingGood');
      assert.strictEqual(await btnGood.isDisabled(), true, 'Rating buttons must be disabled while viewing front prompt');

      assert.strictEqual(getRuntimeErrors().length, 0, 'Zero unexpected runtime errors');
    } finally {
      await context.close();
    }
  });

  // SR-03: 3D Flip interaction (click and Space key)
  test('SR-03: 3D Flip interaction toggles between front prompt and back answer (click & Space)', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      await page.route('**/api/v1/srs/due', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: createMockDueCards()
          })
        });
      });

      await page.goto(`${baseUrl}/srs-review.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#srsActiveSession').waitFor({ state: 'visible', timeout: 5000 });

      const flashcard = page.locator('#flashcard');
      const cardFront = page.locator('#cardFront');
      const cardBack = page.locator('#cardBack');

      // Initial state
      assert.strictEqual(await cardFront.getAttribute('aria-hidden'), 'false');
      assert.strictEqual(await cardBack.getAttribute('aria-hidden'), 'true');

      // Click to flip
      await flashcard.click();
      await page.waitForTimeout(100);

      // Flipped state
      const isFlipped = await flashcard.evaluate(el => el.classList.contains('flipped'));
      assert.strictEqual(isFlipped, true, 'Flashcard must have class "flipped"');
      assert.strictEqual(await cardFront.getAttribute('aria-hidden'), 'true');
      assert.strictEqual(await cardBack.getAttribute('aria-hidden'), 'false');

      // Check answer details on back
      const meaningVi = page.locator('#cardBackMeaningVi');
      assert.strictEqual(await meaningVi.textContent(), 'Học tập, nghiên cứu');

      // Rating buttons must now be enabled
      const btnGood = page.locator('#btnRatingGood');
      assert.strictEqual(await btnGood.isDisabled(), false, 'Rating buttons must be enabled when flipped');

      // Press Space to flip back to front
      await page.keyboard.press('Space');
      await page.waitForTimeout(100);

      const isFlippedAfterSpace = await flashcard.evaluate(el => el.classList.contains('flipped'));
      assert.strictEqual(isFlippedAfterSpace, false, 'Flashcard must unflip when Space is pressed again');

      assert.strictEqual(getRuntimeErrors().length, 0, 'Zero unexpected runtime errors');
    } finally {
      await context.close();
    }
  });

  // SR-04: Keyboard rating shortcut (1..4) submits authoritative review and advances
  test('SR-04: Keyboard rating shortcut (1..4) submits authoritative review to backend and advances', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      await page.route('**/api/v1/srs/due', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: createMockDueCards()
          })
        });
      });

      let capturedPayload = null;
      await page.route('**/api/v1/srs/review', async (route) => {
        capturedPayload = JSON.parse(route.request().postData());
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: {
              itemType: 'VOCABULARY',
              itemId: 101,
              hanzi: '学',
              intervalDays: 6,
              repetitions: 2,
              easeFactor: 2.50
            }
          })
        });
      });

      await page.goto(`${baseUrl}/srs-review.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#srsActiveSession').waitFor({ state: 'visible', timeout: 5000 });

      // Flip card first
      await page.locator('#flashcard').click();
      await page.waitForTimeout(100);

      // Press '3' (Good)
      await page.keyboard.press('3');
      await page.waitForTimeout(200);

      // Verify payload sent to backend
      assert.ok(capturedPayload, 'POST /api/v1/srs/review must be invoked');
      assert.strictEqual(capturedPayload.itemType, 'VOCABULARY');
      assert.strictEqual(capturedPayload.itemId, 101);
      assert.strictEqual(capturedPayload.rating, 3);
      assert.ok(typeof capturedPayload.reviewTimeSeconds === 'number' && capturedPayload.reviewTimeSeconds >= 0);
      assert.strictEqual('userId' in capturedPayload, false, 'No userId in payload');

      // Should have advanced to Card 2 ("书")
      const frontHanzi = page.locator('#cardFrontHanzi');
      await frontHanzi.waitFor({ state: 'visible' });
      assert.strictEqual(await frontHanzi.textContent(), '书', 'Session must advance to card 2 ("书")');

      const progressText = page.locator('#srsProgressText');
      assert.strictEqual(await progressText.textContent(), 'Thẻ 2 / 2');

      assert.strictEqual(getRuntimeErrors().length, 0, 'Zero unexpected runtime errors');
    } finally {
      await context.close();
    }
  });

  // SR-05: Rating Again (1) requeues card to the end of the session
  test('SR-05: Rating Again (1) requeues card to the end of the local session queue', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      await page.route('**/api/v1/srs/due', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: [createMockDueCards()[0]] // 1 card
          })
        });
      });

      let reviewCount = 0;
      await page.route('**/api/v1/srs/review', async (route) => {
        reviewCount++;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: { itemId: 101, intervalDays: 0 }
          })
        });
      });

      await page.goto(`${baseUrl}/srs-review.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#srsActiveSession').waitFor({ state: 'visible', timeout: 5000 });

      // Flip card
      await page.locator('#flashcard').click();
      await page.waitForTimeout(100);

      // Select Again (button or key '1')
      await page.locator('#btnRatingAgain').click();
      await page.waitForTimeout(200);

      // Card should be requeued, queue is now 2 items, and card appears again
      const progressText = page.locator('#srsProgressText');
      assert.strictEqual(await progressText.textContent(), 'Thẻ 2 / 2', 'Requeued card must update progress to Thẻ 2 / 2');

      // The status badge should display "Học lại"
      const statusBadge = page.locator('#cardStatusBadgeFront');
      assert.strictEqual(await statusBadge.textContent(), 'Học lại');

      assert.strictEqual(reviewCount, 1, 'First review must have been recorded');
      assert.strictEqual(getRuntimeErrors().length, 0, 'Zero unexpected runtime errors');
    } finally {
      await context.close();
    }
  });

  // SR-06: Session completes with truthful statistics
  test('SR-06: Session completes with truthful statistics (total reviewed, Again, Good/Easy, time)', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      await page.route('**/api/v1/srs/due', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: [createMockDueCards()[0]] // 1 card
          })
        });
      });

      await page.route('**/api/v1/srs/review', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: { itemId: 101, intervalDays: 1 }
          })
        });
      });

      await page.goto(`${baseUrl}/srs-review.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#srsActiveSession').waitFor({ state: 'visible', timeout: 5000 });

      // Flip card and rate Easy (4)
      await page.locator('#flashcard').click();
      await page.waitForTimeout(100);
      await page.locator('#btnRatingEasy').click();

      // Session completes
      const completedState = page.locator('#srsCompletedState');
      await completedState.waitFor({ state: 'visible', timeout: 5000 });

      const totalReviewed = page.locator('#statTotalReviewed');
      assert.strictEqual(await totalReviewed.textContent(), '1');

      const goodEasyCount = page.locator('#statGoodEasyCount');
      assert.strictEqual(await goodEasyCount.textContent(), '1');

      const againCount = page.locator('#statAgainCount');
      assert.strictEqual(await againCount.textContent(), '0');

      const restartBtn = page.locator('#btnRestartSession');
      assert.ok(await restartBtn.isVisible());

      assert.strictEqual(getRuntimeErrors().length, 0, 'Zero unexpected runtime errors');
    } finally {
      await context.close();
    }
  });

  // SR-07: Lesson query mode (?lessonId=10) retrieves new cards from approved lesson
  test('SR-07: Lesson query mode (?lessonId=10) retrieves new cards from approved lesson', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      let requestedLessonParam = null;
      await page.route('**/api/v1/srs/new-cards*', async (route) => {
        const url = new URL(route.request().url());
        requestedLessonParam = url.searchParams.get('lessonId');
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: [createMockDueCards()[1]] // Card 2 with isNew=true
          })
        });
      });

      await page.goto(`${baseUrl}/srs-review.html?lessonId=10`, { waitUntil: 'domcontentloaded' });
      await page.locator('#srsActiveSession').waitFor({ state: 'visible', timeout: 5000 });

      assert.strictEqual(requestedLessonParam, '10', 'Endpoint must be requested with lessonId=10');

      // Mode badge displays Lesson context
      const modeBadge = page.locator('#srsSessionModeBadge');
      assert.strictEqual(await modeBadge.textContent(), 'Bài học #10');

      assert.strictEqual(getRuntimeErrors().length, 0, 'Zero unexpected runtime errors');
    } finally {
      await context.close();
    }
  });

  // SR-08: Contextual personal notes modal opens from vocabulary card back-face
  test('SR-08: Contextual personal notes modal opens from vocabulary card back-face', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAuthenticatedSession(page);

      await page.route('**/api/v1/srs/due', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'OK',
            data: [createMockDueCards()[0]] // Vocab card itemId: 101
          })
        });
      });

      let interceptedNotesCall = false;
      await page.route('**/api/v1/vocabularies/101/notes*', async (route) => {
        interceptedNotesCall = true;
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

      await page.goto(`${baseUrl}/srs-review.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#srsActiveSession').waitFor({ state: 'visible', timeout: 5000 });

      // Flip card to reveal answer and note button
      await page.locator('#flashcard').click();
      await page.waitForTimeout(100);

      const noteBtn = page.locator('#btnCardNote');
      assert.ok(await noteBtn.isVisible(), 'Ghi chú button must be visible on vocabulary card back');

      // Click note button
      await noteBtn.click();

      // Notes modal opens
      const notesModal = page.locator('#notesModal');
      await notesModal.waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await notesModal.isVisible(), 'Notes modal must be visible');
      assert.strictEqual(interceptedNotesCall, true, 'Notes API must be called for vocabId 101');

      // Close notes modal via Close button
      const closeBtn = page.locator('#notesModalCloseBtn');
      await closeBtn.click();
      await notesModal.waitFor({ state: 'hidden', timeout: 5000 });

      assert.strictEqual(getRuntimeErrors().length, 0, 'Zero unexpected runtime errors');
    } finally {
      await context.close();
    }
  });

});
