/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: ADMIN VOCABULARY CRUD UI (TASK 9F.4)
 * File: tests/frontend/e2e/admin-vocabulary.browser.mjs
 *
 * Scenarios:
 * - VV-01: Admin opens admin-vocabulary.html -> sees catalog table, search input, and CRUD controls.
 * - VV-02: Server-side search sends search query to public vocabulary endpoint and renders filtered results.
 * - VV-03: Create modal opens and displays required fields, pinyinRaw helper, and constituent-radical selector.
 * - VV-04: Valid vocabulary creation -> POST /api/v1/admin/vocabulary returns 201 -> UI reflects created state.
 * - VV-05: Duplicate (hanzi + pinyinRaw) returns 409 CONFLICT -> form displays localized duplicate error.
 * - VV-06: Admin edits vocabulary -> GET /api/v1/vocabulary/{id} populates radicals -> PUT returns 200 -> table updates.
 * - VV-07: Admin confirms delete -> DELETE /api/v1/admin/vocabulary/{id} returns 204 No Content -> removed from UI.
 * - VV-08: Referenced vocabulary delete returns 409 CONFLICT -> modal explains learning data reference constraint.
 * - VV-09: Non-Admin session is blocked by role guard.
 * - VV-10: Verification Center Section 17B -> diagnostic executes safely with zero mutations.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function createMockRadicals() {
  return [
    { radicalId: 9, character: '人', pinyin: 'rén', meaningHanViet: 'Nhân', meaningVi: 'Người' },
    { radicalId: 75, character: '木', pinyin: 'mù', meaningHanViet: 'Mộc', meaningVi: 'Cây, gỗ' },
    { radicalId: 85, character: '水', pinyin: 'shuǐ', meaningHanViet: 'Thủy', meaningVi: 'Nước' }
  ];
}

function createMockVocabularies() {
  return [
    {
      vocabId: 1,
      hanzi: '你好',
      pinyin: 'nǐ hǎo',
      pinyinRaw: 'ni hao',
      meaningHanViet: 'nhĩ hảo',
      meaningVi: 'xin chào',
      audioUrl: 'https://example.com/audio/nihao.mp3',
      videoWritingUrl: '/media/video/nihao.mp4',
      exampleSentence: '你好，很高兴认识你。',
      exampleTranslation: 'Xin chào, rất vui được gặp bạn.'
    },
    {
      vocabId: 2,
      hanzi: '谢谢',
      pinyin: 'xièxie',
      pinyinRaw: 'xiexie',
      meaningHanViet: 'tạ tạ',
      meaningVi: 'cảm ơn',
      audioUrl: null,
      videoWritingUrl: null,
      exampleSentence: '谢谢你的帮助。',
      exampleTranslation: 'Cảm ơn sự giúp đỡ của bạn.'
    },
    {
      vocabId: 3,
      hanzi: '再见',
      pinyin: 'zàijiàn',
      pinyinRaw: 'zaijian',
      meaningHanViet: 'tái kiến',
      meaningVi: 'tạm biệt',
      audioUrl: null,
      videoWritingUrl: null,
      exampleSentence: null,
      exampleTranslation: null
    }
  ];
}

async function setupAdminSession(page, email = 'admin@example.com', accountId = 1) {
  await page.addInitScript(`
    localStorage.setItem('access_token', 'test_jwt_admin_token_task_9f4');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: ${accountId},
      emailOrPhone: '${email}',
      fullName: 'Quản Trị Viên Chính',
      roles: ['Admin']
    }));
  `);
}

async function setupLearnerSession(page) {
  await page.addInitScript(`
    localStorage.setItem('access_token', 'test_jwt_learner_token_task_9f4');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: 99,
      emailOrPhone: 'learner@example.com',
      fullName: 'Học Viên Thường',
      roles: ['Learner']
    }));
  `);
}

describe('Browser E2E: Admin Vocabulary CRUD UI (Task 9F.4)', () => {
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

  // ---------------------------------------------------------------------------
  // VV-01: Admin opens page and sees catalog table and CRUD controls
  // ---------------------------------------------------------------------------
  test('VV-01: Admin can open admin-vocabulary.html and view catalog table and CRUD controls', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      const vocabs = createMockVocabularies();

      await page.route(/\/api\/v1\/vocabulary(\?.*)?$/, async (route) => {
        if (route.request().url().includes('/vocabulary/')) {
          return route.continue();
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: vocabs,
              page: 0,
              size: 20,
              totalElements: vocabs.length,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-vocabulary.html`, { waitUntil: 'domcontentloaded' });

      // Check header and primary h1
      const h1 = page.locator('h1');
      await h1.waitFor({ state: 'visible', timeout: 5000 });
      assert.strictEqual((await h1.textContent()).trim(), 'Quản Trị Danh Mục Từ Vựng');

      // Check search input and create button
      const searchInput = page.locator('#vocabSearchInput');
      assert.strictEqual(await searchInput.isVisible(), true);

      const btnCreate = page.locator('#btnOpenCreateModal');
      assert.strictEqual(await btnCreate.isVisible(), true);

      // Check table rows rendered
      const tableRows = page.locator('#vocabTableBody tr');
      await tableRows.first().waitFor({ state: 'visible', timeout: 5000 });
      assert.strictEqual(await tableRows.count(), 3);

      // Check first item content
      const firstRow = page.locator('#vocabRow-1');
      assert.ok(await firstRow.textContent().then(t => t.includes('你好')));
      assert.ok(await firstRow.textContent().then(t => t.includes('nǐ hǎo')));
      assert.ok(await firstRow.textContent().then(t => t.includes('xin chào')));

      // Runtime errors check
      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors: ${JSON.stringify(errors)}`);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // VV-02: Server-side search sends search query to public endpoint
  // ---------------------------------------------------------------------------
  test('VV-02: Server-side search sends search query and renders filtered results', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      let capturedQuery = null;

      await page.route(/\/api\/v1\/vocabulary(\?.*)?$/, async (route) => {
        const url = new URL(route.request().url());
        capturedQuery = url.searchParams.get('search');

        const filtered = capturedQuery
          ? createMockVocabularies().filter(v => v.hanzi.includes(capturedQuery) || v.pinyin.includes(capturedQuery))
          : createMockVocabularies();

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: filtered,
              page: 0,
              size: 20,
              totalElements: filtered.length,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-vocabulary.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#vocabRow-1').waitFor({ state: 'visible', timeout: 5000 });

      // Enter search query
      const searchInput = page.locator('#vocabSearchInput');
      await searchInput.fill('谢谢');
      await searchInput.press('Enter');

      // Wait for table to update
      await page.locator('#vocabRow-2').waitFor({ state: 'visible', timeout: 5000 });
      assert.strictEqual(capturedQuery, '谢谢');

      const rows = page.locator('#vocabTableBody tr');
      assert.strictEqual(await rows.count(), 1);
      assert.ok(await rows.first().textContent().then(t => t.includes('谢谢')));

      // Check clear button
      const clearBtn = page.locator('#searchClearBtn');
      assert.strictEqual(await clearBtn.isVisible(), true);
      await clearBtn.click();

      // Table restores
      await page.locator('#vocabRow-1').waitFor({ state: 'visible', timeout: 5000 });
      assert.strictEqual(await page.locator('#vocabTableBody tr').count(), 3);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // VV-03: Create modal opens with fields, pinyinRaw helper, and radical picker
  // ---------------------------------------------------------------------------
  test('VV-03: Create modal opens and displays required fields and constituent-radical selector', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      await page.route(/\/api\/v1\/vocabulary(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
          })
        });
      });

      await page.route(/\/api\/v1\/radicals(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: { items: createMockRadicals(), page: 0, size: 250, totalElements: 3, totalPages: 1 }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-vocabulary.html`, { waitUntil: 'domcontentloaded' });

      // Click Add Vocabulary button
      await page.locator('#btnOpenCreateModal').click();

      // Modal should be visible
      const modal = page.locator('#vocabFormModal');
      await modal.waitFor({ state: 'visible', timeout: 5000 });

      // Check inputs exist
      assert.strictEqual(await page.locator('#inputHanzi').isVisible(), true);
      assert.strictEqual(await page.locator('#inputPinyin').isVisible(), true);
      assert.strictEqual(await page.locator('#inputPinyinRaw').isVisible(), true);
      assert.strictEqual(await page.locator('#inputMeaningHanViet').isVisible(), true);
      assert.strictEqual(await page.locator('#inputMeaningVi').isVisible(), true);

      // Check auto-derive pinyinRaw
      await page.locator('#inputPinyin').fill('hǎo');
      assert.strictEqual(await page.locator('#inputPinyinRaw').inputValue(), 'hao');

      // Check radicals picker loaded
      const radCheck9 = page.locator('#radCheck-9');
      await radCheck9.waitFor({ state: 'visible', timeout: 5000 });
      assert.strictEqual(await radCheck9.isChecked(), false);

      // Check selecting radical 9
      await radCheck9.check();
      assert.strictEqual(await radCheck9.isChecked(), true);

      // Badges container shows 1 radical selected
      const selectedCount = page.locator('#selectedRadicalsCount');
      assert.strictEqual(await selectedCount.textContent(), '1 đã chọn');

      // Close modal
      await page.locator('#modalFormCloseBtn').click();
      assert.strictEqual(await modal.isVisible(), false);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // VV-04: Admin creates a valid vocabulary item -> 201 Created -> UI updates
  // ---------------------------------------------------------------------------
  test('VV-04: Admin creates a valid vocabulary item and UI reflects returned server state', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      const vocabs = createMockVocabularies();
      let createdPayload = null;

      await page.route(/\/api\/v1\/vocabulary(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: { items: vocabs, page: 0, size: 20, totalElements: vocabs.length, totalPages: 1 }
          })
        });
      });

      await page.route(/\/api\/v1\/radicals(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: { items: createMockRadicals(), page: 0, size: 250, totalElements: 3, totalPages: 1 }
          })
        });
      });

      await page.route(/\/api\/v1\/admin\/vocabulary$/, async (route) => {
        if (route.request().method() === 'POST') {
          createdPayload = JSON.parse(route.request().postData());
          const newVocab = {
            vocabId: 101,
            hanzi: createdPayload.hanzi,
            pinyin: createdPayload.pinyin,
            pinyinRaw: createdPayload.pinyinRaw,
            meaningHanViet: createdPayload.meaningHanViet,
            meaningVi: createdPayload.meaningVi,
            audioUrl: createdPayload.audioUrl,
            videoWritingUrl: createdPayload.videoWritingUrl,
            exampleSentence: createdPayload.exampleSentence,
            exampleTranslation: createdPayload.exampleTranslation,
            radicals: [{ radicalId: 9, character: '人', pinyin: 'rén', meaningHanViet: 'Nhân', meaningVi: 'Người' }]
          };
          vocabs.push(newVocab);
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'Tạo từ vựng thành công',
              data: newVocab
            })
          });
        } else {
          await route.continue();
        }
      });

      await page.goto(`${baseUrl}/admin-vocabulary.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#vocabRow-1').waitFor({ state: 'visible', timeout: 5000 });

      // Open create modal
      await page.locator('#btnOpenCreateModal').click();
      await page.locator('#vocabFormModal').waitFor({ state: 'visible', timeout: 5000 });

      // Fill in form
      await page.locator('#inputHanzi').fill('朋友');
      await page.locator('#inputPinyin').fill('péngyou');
      await page.locator('#inputPinyinRaw').fill('pengyou');
      await page.locator('#inputMeaningHanViet').fill('bằng hữu');
      await page.locator('#inputMeaningVi').fill('bạn bè');
      await page.locator('#inputAudioUrl').fill('https://example.com/audio/pengyou.mp3');

      // Select radical 9 (人)
      await page.locator('#radCheck-9').check();

      // Submit
      await page.locator('#btnSaveVocab').click();

      // Modal closes
      await page.locator('#vocabFormModal').waitFor({ state: 'hidden', timeout: 5000 });

      // Verify payload sent
      assert.strictEqual(createdPayload.hanzi, '朋友');
      assert.strictEqual(createdPayload.pinyin, 'péngyou');
      assert.strictEqual(createdPayload.pinyinRaw, 'pengyou');
      assert.deepStrictEqual(createdPayload.radicalIds, [9]);

      // Table should now show the new item
      const newRow = page.locator('#vocabRow-101');
      await newRow.waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await newRow.textContent().then(t => t.includes('朋友')));
      assert.ok(await newRow.textContent().then(t => t.includes('bằng hữu')));

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // VV-05: Duplicate (hanzi + pinyinRaw) returns 409 CONFLICT
  // ---------------------------------------------------------------------------
  test('VV-05: Duplicate (hanzi + pinyinRaw) creation produces clear 409 CONFLICT feedback', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      await page.route(/\/api\/v1\/vocabulary(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: { items: createMockVocabularies(), page: 0, size: 20, totalElements: 3, totalPages: 1 }
          })
        });
      });

      await page.route(/\/api\/v1\/radicals(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: { items: createMockRadicals(), page: 0, size: 250, totalElements: 3, totalPages: 1 }
          })
        });
      });

      await page.route(/\/api\/v1\/admin\/vocabulary$/, async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 409,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'CONFLICT',
              message: "Từ vựng với chữ Hán '你好' và pinyin 'ni hao' đã tồn tại",
              data: null
            })
          });
        }
      });

      await page.goto(`${baseUrl}/admin-vocabulary.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#vocabRow-1').waitFor({ state: 'visible', timeout: 5000 });

      // Open create modal
      await page.locator('#btnOpenCreateModal').click();
      await page.locator('#vocabFormModal').waitFor({ state: 'visible', timeout: 5000 });

      // Fill in duplicate data
      await page.locator('#inputHanzi').fill('你好');
      await page.locator('#inputPinyin').fill('nǐ hǎo');
      await page.locator('#inputPinyinRaw').fill('ni hao');
      await page.locator('#inputMeaningHanViet').fill('nhĩ hảo');
      await page.locator('#inputMeaningVi').fill('xin chào');

      // Click save
      await page.locator('#btnSaveVocab').click();

      // Form alert should be visible with conflict message
      const alert = page.locator('#formGeneralError');
      await alert.waitFor({ state: 'visible', timeout: 5000 });
      const alertText = await alert.textContent();
      assert.ok(alertText.includes("Từ vựng với chữ Hán '你好' và pinyin 'ni hao' đã tồn tại"));
      // Modal must remain open
      assert.strictEqual(await page.locator('#vocabFormModal').isVisible(), true);

      const errors = getRuntimeErrors().filter(e => !e.includes('409'));
      assert.strictEqual(errors.length, 0);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // VV-06: Admin edits existing vocabulary item -> PUT returns 200 -> UI updates
  // ---------------------------------------------------------------------------
  test('VV-06: Admin edits vocabulary, detail response populates radicals, and update reflects server state', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      const vocabs = createMockVocabularies();
      let putPayload = null;

      await page.route(/\/api\/v1\/vocabulary(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: { items: vocabs, page: 0, size: 20, totalElements: vocabs.length, totalPages: 1 }
          })
        });
      });

      await page.route(/\/api\/v1\/vocabulary\/1$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: {
              vocabId: 1,
              hanzi: '你好',
              pinyin: 'nǐ hǎo',
              pinyinRaw: 'ni hao',
              meaningHanViet: 'nhĩ hảo',
              meaningVi: 'xin chào',
              audioUrl: 'https://example.com/audio/nihao.mp3',
              videoWritingUrl: '/media/video/nihao.mp4',
              exampleSentence: '你好，很高兴认识你。',
              exampleTranslation: 'Xin chào, rất vui được gặp bạn.',
              radicals: [{ radicalId: 9, character: '人', pinyin: 'rén', meaningHanViet: 'Nhân', meaningVi: 'Người' }]
            }
          })
        });
      });

      await page.route(/\/api\/v1\/radicals(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: { items: createMockRadicals(), page: 0, size: 250, totalElements: 3, totalPages: 1 }
          })
        });
      });

      await page.route(/\/api\/v1\/admin\/vocabulary\/1$/, async (route) => {
        if (route.request().method() === 'PUT') {
          putPayload = JSON.parse(route.request().postData());
          const updated = {
            vocabId: 1,
            hanzi: putPayload.hanzi,
            pinyin: putPayload.pinyin,
            pinyinRaw: putPayload.pinyinRaw,
            meaningHanViet: putPayload.meaningHanViet,
            meaningVi: putPayload.meaningVi,
            audioUrl: putPayload.audioUrl,
            videoWritingUrl: putPayload.videoWritingUrl,
            exampleSentence: putPayload.exampleSentence,
            exampleTranslation: putPayload.exampleTranslation,
            radicals: [{ radicalId: 9 }, { radicalId: 75 }]
          };
          vocabs[0] = updated;
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'Cập nhật từ vựng thành công',
              data: updated
            })
          });
        }
      });

      await page.goto(`${baseUrl}/admin-vocabulary.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#vocabRow-1').waitFor({ state: 'visible', timeout: 5000 });

      // Click Sửa for vocab #1
      await page.locator('#btnEditVocab-1').click();

      // Form modal opens with prefilled data
      const modal = page.locator('#vocabFormModal');
      await modal.waitFor({ state: 'visible', timeout: 5000 });
      assert.strictEqual(await page.locator('#inputHanzi').inputValue(), '你好');
      assert.strictEqual(await page.locator('#inputMeaningVi').inputValue(), 'xin chào');

      // Radical 9 is selected
      const selectedCount = page.locator('#selectedRadicalsCount');
      assert.strictEqual(await selectedCount.textContent(), '1 đã chọn');

      // Add radical 75 (木)
      await page.locator('#radCheck-75').check();
      assert.strictEqual(await selectedCount.textContent(), '2 đã chọn');

      // Update meaningVi
      await page.locator('#inputMeaningVi').fill('xin chào, chào bạn');

      // Submit
      await page.locator('#btnSaveVocab').click();

      // Modal closes
      await modal.waitFor({ state: 'hidden', timeout: 5000 });

      // Verify payload sent
      assert.strictEqual(putPayload.meaningVi, 'xin chào, chào bạn');
      assert.deepStrictEqual(putPayload.radicalIds, [9, 75]);

      // Row reflects new meaning
      const updatedRow = page.locator('#vocabRow-1');
      await page.locator('#vocabRow-1').filter({ hasText: 'xin chào, chào bạn' }).waitFor({ state: 'visible', timeout: 5000 });
      assert.ok(await updatedRow.textContent().then(t => t.includes('xin chào, chào bạn')));

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // VV-07: Admin confirms delete -> 204 No Content -> item removed
  // ---------------------------------------------------------------------------
  test('VV-07: Admin confirms delete and successful 204 No Content removes vocabulary item', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      const vocabs = createMockVocabularies();
      let deleteCalled = false;

      await page.route(/\/api\/v1\/vocabulary(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: { items: vocabs, page: 0, size: 20, totalElements: vocabs.length, totalPages: 1 }
          })
        });
      });

      await page.route(/\/api\/v1\/admin\/vocabulary\/3$/, async (route) => {
        if (route.request().method() === 'DELETE') {
          deleteCalled = true;
          vocabs.splice(2, 1); // remove item 3
          await route.fulfill({
            status: 204
          });
        }
      });

      await page.goto(`${baseUrl}/admin-vocabulary.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#vocabRow-3').waitFor({ state: 'visible', timeout: 5000 });

      // Click delete button for vocab #3
      await page.locator('#btnDeleteVocab-3').click();

      // Delete confirmation modal opens
      const delModal = page.locator('#deleteVocabModal');
      await delModal.waitFor({ state: 'visible', timeout: 5000 });

      // Verify target info card
      assert.strictEqual(await page.locator('#deleteTargetHanzi').textContent(), '再见');
      assert.strictEqual(await page.locator('#deleteTargetId').textContent(), '#3');

      // Click confirm delete
      await page.locator('#btnConfirmDeleteVocab').click();

      // Modal closes
      await delModal.waitFor({ state: 'hidden', timeout: 5000 });
      assert.strictEqual(deleteCalled, true);

      // Row #3 is now gone
      await page.locator('#vocabRow-3').waitFor({ state: 'detached', timeout: 5000 });
      assert.strictEqual(await page.locator('#vocabTableBody tr').count(), 2);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // VV-08: Referenced vocabulary delete returns 409 CONFLICT
  // ---------------------------------------------------------------------------
  test('VV-08: Referenced vocabulary delete returns 409 CONFLICT and UI explains learning-data constraint', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      await page.route(/\/api\/v1\/vocabulary(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: { items: createMockVocabularies(), page: 0, size: 20, totalElements: 3, totalPages: 1 }
          })
        });
      });

      await page.route(/\/api\/v1\/admin\/vocabulary\/1$/, async (route) => {
        if (route.request().method() === 'DELETE') {
          await route.fulfill({
            status: 409,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'CONFLICT',
              message: 'Không thể xóa từ vựng đang được liên kết với bài học',
              data: null
            })
          });
        }
      });

      await page.goto(`${baseUrl}/admin-vocabulary.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#vocabRow-1').waitFor({ state: 'visible', timeout: 5000 });

      // Click delete for vocab #1
      await page.locator('#btnDeleteVocab-1').click();

      const delModal = page.locator('#deleteVocabModal');
      await delModal.waitFor({ state: 'visible', timeout: 5000 });

      // Confirm delete
      await page.locator('#btnConfirmDeleteVocab').click();

      // Conflict error appears in delete modal
      const errEl = page.locator('#deleteModalError');
      await errEl.waitFor({ state: 'visible', timeout: 5000 });
      const errMsg = await errEl.textContent();
      assert.ok(errMsg.includes('Không thể xóa từ vựng đang được liên kết với bài học'));

      // Modal must remain open and row #1 still exists
      assert.strictEqual(await delModal.isVisible(), true);
      assert.strictEqual(await page.locator('#vocabRow-1').isVisible(), true);

      // Close modal
      await page.locator('#btnCancelDeleteModal').click();
      await delModal.waitFor({ state: 'hidden', timeout: 5000 });

      const errors = getRuntimeErrors().filter(e => !e.includes('409'));
      assert.strictEqual(errors.length, 0);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // VV-09: Non-Admin session is blocked by role guard
  // ---------------------------------------------------------------------------
  test('VV-09: Non-Admin session is blocked by role guard', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupLearnerSession(page);

      await page.goto(`${baseUrl}/admin-vocabulary.html`, { waitUntil: 'domcontentloaded' });

      // Auth guard container must be visible
      const guardContainer = page.locator('#adminAuthGuardContainer');
      await guardContainer.waitFor({ state: 'visible', timeout: 5000 });

      // Main content must be hidden
      const mainContent = page.locator('#adminVocabMainContent');
      assert.strictEqual(await mainContent.isVisible(), false);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // VV-10: Verification Center Section 17B diagnostic executes safely
  // ---------------------------------------------------------------------------
  test('VV-10: Verification Center Section 17B diagnostic executes safely with zero mutations', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      let mutationDetected = false;

      // Ensure NO mutation endpoints are called
      await page.route(/\/api\/v1\/admin\/vocabulary(\/.*)?$/, async (route) => {
        if (['POST', 'PUT', 'DELETE'].includes(route.request().method())) {
          mutationDetected = true;
          await route.abort();
        } else {
          await route.continue();
        }
      });

      // Public vocabulary read-only probe
      await page.route(/\/api\/v1\/vocabulary(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            data: { items: createMockVocabularies().slice(0, 1), page: 0, size: 1, totalElements: 3, totalPages: 3 }
          })
        });
      });

      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });

      const btnProbe = page.locator('#btnProbeAdminVocabCapability');
      await btnProbe.waitFor({ state: 'visible', timeout: 5000 });
      await btnProbe.click();

      // Output should report PASS
      const output = page.locator('#adminVocabProbeOutput');
      await output.waitFor({ state: 'visible', timeout: 5000 });
      await page.waitForFunction(
        () => document.getElementById('adminVocabProbeOutput')?.textContent?.includes('[PROBE PASS]'),
        { timeout: 5000 }
      );

      const outputText = await output.textContent();
      assert.match(outputText, /1\. Form Validation \(Giới hạn DTO\): PASS/);
      assert.match(outputText, /2\. PinyinRaw Normalization: PASS/);
      assert.match(outputText, /3\. Payload Construction: PASS/);
      assert.match(outputText, /4\. No-Op Update Detection: PASS/);
      assert.match(outputText, /5\. Conflict Classification \(409\/404\): PASS/);
      assert.match(outputText, /ZERO CRUD MUTATION/);
      assert.match(outputText, /\[PROBE PASS\]/);

      assert.strictEqual(mutationDetected, false, 'STRICT INVARIANT VIOLATED: Mutation was attempted from verification center!');
    } finally {
      await context.close();
    }
  });

});
