/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: CREATOR TWO-STEP EXCEL IMPORT UI (TASK 9D.2)
 * File: tests/frontend/e2e/creator-import.browser.mjs
 * 
 * Verifies:
 * - CI-01: Creator selects valid XLSX, previews report, views metrics & parsed rows.
 * - CI-02: Creator selects invalid XLSX, views row errors, confirm is blocked.
 * - CI-03: Valid preview -> title input -> confirm -> 201 created -> navigate to editor.
 * - CI-04: File change invariant: Changing file invalidates previous preview report.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function mockValidReport() {
  return {
    code: 'SUCCESS',
    message: 'Thao tác thành công',
    errors: [],
    data: {
      isValid: true,
      totalRows: 3,
      validRowsCount: 3,
      invalidRowsCount: 0,
      newVocabCount: 2,
      existingVocabCount: 1,
      fileStatus: 'VALID',
      summaryMessage: 'Tệp hoàn toàn hợp lệ (3/3 dòng chuẩn)',
      rows: [
        {
          rowNumber: 2,
          hanzi: '中',
          pinyin: 'zhōng',
          meaningHanViet: 'Trung',
          meaningVi: 'Ở giữa, trung tâm',
          isExisting: true,
          existingVocabId: 10,
          isValid: true,
          errors: []
        },
        {
          rowNumber: 3,
          hanzi: '文',
          pinyin: 'wén',
          meaningHanViet: 'Văn',
          meaningVi: 'Ngôn ngữ, văn học',
          isExisting: false,
          existingVocabId: null,
          isValid: true,
          errors: []
        },
        {
          rowNumber: 4,
          hanzi: '学',
          pinyin: 'xué',
          meaningHanViet: 'Học',
          meaningVi: 'Học tập',
          isExisting: false,
          existingVocabId: null,
          isValid: true,
          errors: []
        }
      ],
      errors: []
    }
  };
}

function mockInvalidReport() {
  return {
    code: 'SUCCESS',
    message: 'Thao tác thành công',
    errors: [],
    data: {
      isValid: false,
      totalRows: 2,
      validRowsCount: 1,
      invalidRowsCount: 1,
      newVocabCount: 1,
      existingVocabCount: 0,
      fileStatus: 'INVALID',
      summaryMessage: 'Phát hiện lỗi: 1/2 dòng không hợp lệ',
      rows: [
        {
          rowNumber: 2,
          hanzi: '你好',
          pinyin: 'nǐ hǎo',
          meaningHanViet: 'Nễ hảo',
          meaningVi: 'Xin chào',
          isExisting: false,
          existingVocabId: null,
          isValid: true,
          errors: []
        },
        {
          rowNumber: 3,
          hanzi: '',
          pinyin: 'cuò',
          meaningHanViet: 'Thác',
          meaningVi: 'Sai lầm',
          isExisting: false,
          existingVocabId: null,
          isValid: false,
          errors: ['Chữ Hán không được để trống']
        }
      ],
      errors: [
        {
          rowNumber: 3,
          columnName: 'Chữ Hán',
          errorCode: 'MISSING_REQUIRED_FIELD',
          errorMessage: 'Chữ Hán không được để trống'
        }
      ]
    }
  };
}

async function setupCreatorSession(page, role = 'Creator') {
  await page.addInitScript(`
    localStorage.setItem('access_token', 'test_creator_jwt_token');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: 2001,
      emailOrPhone: 'creator_studio_user@example.com',
      fullName: 'Tác Giả Kiểm Thử',
      roles: ['${role}']
    }));
  `);
}

describe('Browser E2E: Creator Two-Step Excel Import UI (Task 9D.2)', () => {
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
    if (serverHandle && !serverHandle.isExternal) {
      await serverHandle.close();
    }
  });

  // CI-01: Preview Valid File
  test('CI-01: Creator selects valid XLSX, previews report, views metrics & parsed rows', async () => {
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
      await setupCreatorSession(page, 'Creator');

      let previewIntercepted = false;
      await page.route('**/api/v1/creator/lessons/import', async (route) => {
        previewIntercepted = true;
        await route.fulfill({
          status: 200,
          contentType: 'application/json; charset=utf-8',
          body: JSON.stringify(mockValidReport())
        });
      });

      await page.goto(`${baseUrl}/creator-import.html`, { waitUntil: 'domcontentloaded' });

      // Set valid file
      await page.setInputFiles('#excelFileInput', {
        name: 'hsk1_vocab.xlsx',
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer: Buffer.from('fake-xlsx-content')
      });

      // Verify selected file card
      await page.locator('#fileInfoCard').waitFor({ state: 'visible' });
      assert.strictEqual(await page.locator('#selectedFileName').textContent(), 'hsk1_vocab.xlsx');

      // Click Preview
      await page.locator('#btnPreviewFile').click();

      // Verify report section appears
      await page.locator('#previewReportSection').waitFor({ state: 'visible' });
      assert.strictEqual(previewIntercepted, true, 'Preview API must be called');

      // Verify metrics
      assert.strictEqual(await page.locator('#metricTotalRows').textContent(), '3');
      assert.strictEqual(await page.locator('#metricValidRows').textContent(), '3');
      assert.strictEqual(await page.locator('#metricInvalidRows').textContent(), '0');

      // Verify table rows count
      const rows = await page.locator('#previewTableBody tr').count();
      assert.strictEqual(rows, 3);

      // Verify Step 2 is unlocked
      assert.ok(await page.locator('#step2Section').isVisible(), 'Step 2 must be unlocked for valid preview');

    } finally {
      await page.close();
      await context.close();
    }
  });

  // CI-02: Preview Invalid File Blocks Confirm
  test('CI-02: Creator selects invalid XLSX, views row errors, confirm is blocked', async () => {
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
      await setupCreatorSession(page, 'Creator');

      await page.route('**/api/v1/creator/lessons/import', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json; charset=utf-8',
          body: JSON.stringify(mockInvalidReport())
        });
      });

      await page.goto(`${baseUrl}/creator-import.html`, { waitUntil: 'domcontentloaded' });

      await page.setInputFiles('#excelFileInput', {
        name: 'invalid_data.xlsx',
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer: Buffer.from('fake-invalid-content')
      });

      await page.locator('#btnPreviewFile').click();
      await page.locator('#previewReportSection').waitFor({ state: 'visible' });

      // Verify metrics show 1 invalid row
      assert.strictEqual(await page.locator('#metricInvalidRows').textContent(), '1');

      // Verify table displays error
      const errorCellText = await page.locator('#previewTableBody tr.table-danger').textContent();
      assert.ok(errorCellText.includes('Chữ Hán không được để trống'));

      // Verify Step 2 remains locked/hidden
      const isStep2Hidden = await page.locator('#step2Section').evaluate(el => el.classList.contains('d-none'));
      assert.strictEqual(isStep2Hidden, true, 'Step 2 must remain hidden when report is invalid');

    } finally {
      await page.close();
      await context.close();
    }
  });

  // CI-03: Confirm Flow & Atomic Persistence
  test('CI-03: Valid preview -> title input -> confirm -> 201 created -> navigate to editor', async () => {
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
      await setupCreatorSession(page, 'Creator');

      await page.route('**/api/v1/creator/lessons/import', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json; charset=utf-8',
          body: JSON.stringify(mockValidReport())
        });
      });

      let confirmIntercepted = false;
      await page.route('**/api/v1/creator/lessons/import/confirm', async (route) => {
        confirmIntercepted = true;
        await route.fulfill({
          status: 201,
          contentType: 'application/json; charset=utf-8',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Import bài học từ Excel thành công',
            errors: [],
            data: {
              lessonId: 902,
              title: 'HSK 1 — Bài 1 Tác Giả',
              status: 'Draft',
              totalVocabs: 3
            }
          })
        });
      });

      await page.goto(`${baseUrl}/creator-import.html`, { waitUntil: 'domcontentloaded' });

      await page.setInputFiles('#excelFileInput', {
        name: 'confirm_test.xlsx',
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer: Buffer.from('fake-confirm-content')
      });

      await page.locator('#btnPreviewFile').click();
      await page.locator('#step2Section').waitFor({ state: 'visible' });

      // Enter title
      await page.locator('#importLessonTitle').fill('HSK 1 — Bài 1 Tác Giả');
      assert.strictEqual(await page.locator('#titleCharCounter').textContent().then(t => t.trim()), '21 / 100');

      // Click Confirm
      await page.locator('#btnConfirmImport').click();

      // Verify navigation to editor with returned lessonId 902
      await page.waitForURL(/creator-lesson-editor\.html\?id=902/);
      assert.strictEqual(confirmIntercepted, true, 'Confirm API must be called');

    } finally {
      await page.close();
      await context.close();
    }
  });

  // CI-04: File Change Invariant
  test('CI-04: Changing file after preview immediately invalidates previous preview & Step 2', async () => {
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
      await setupCreatorSession(page, 'Creator');

      await page.route('**/api/v1/creator/lessons/import', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json; charset=utf-8',
          body: JSON.stringify(mockValidReport())
        });
      });

      await page.goto(`${baseUrl}/creator-import.html`, { waitUntil: 'domcontentloaded' });

      // 1. Preview File A
      await page.setInputFiles('#excelFileInput', {
        name: 'file_a.xlsx',
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer: Buffer.from('fake-a')
      });

      await page.locator('#btnPreviewFile').click();
      await page.locator('#step2Section').waitFor({ state: 'visible' });

      // Step 2 is visible
      assert.ok(await page.locator('#step2Section').isVisible());

      // 2. Select File B
      await page.setInputFiles('#excelFileInput', {
        name: 'file_b.xlsx',
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer: Buffer.from('fake-b')
      });

      // 3. Verify previous preview report and Step 2 are immediately hidden
      const isReportHidden = await page.locator('#previewReportSection').evaluate(el => el.classList.contains('d-none'));
      const isStep2Hidden = await page.locator('#step2Section').evaluate(el => el.classList.contains('d-none'));

      assert.strictEqual(isReportHidden, true, 'Preview report must be hidden after file change');
      assert.strictEqual(isStep2Hidden, true, 'Step 2 must be hidden after file change');
      assert.strictEqual(await page.locator('#selectedFileName').textContent(), 'file_b.xlsx');

    } finally {
      await page.close();
      await context.close();
    }
  });

});
