/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: ADMIN RADICALS CRUD UI (TASK 9F.3)
 * File: tests/frontend/e2e/admin-radicals.browser.mjs
 *
 * Scenarios:
 * - RA-01: Admin opens admin-radicals.html -> sees catalog table, search input, and CRUD controls.
 * - RA-02: Admin creates a valid radical -> POST /api/v1/admin/radicals returns 201 -> UI reflects created state.
 * - RA-03: Duplicate-character create returns 409 CONFLICT -> form displays clear duplicate error.
 * - RA-04: Admin edits an existing radical -> PUT /api/v1/admin/radicals/{id} returns 200 -> UI reflects updated state.
 * - RA-05: Admin confirms deletion -> DELETE /api/v1/admin/radicals/{id} returns 204 No Content -> removed from UI.
 * - RA-06: Referenced-radical deletion returns 409 CONFLICT -> modal displays reference conflict message.
 * - RA-07: Non-Admin session is blocked by role guard.
 * - RA-08: Verification Center Section 17 -> diagnostic executes safely with zero mutations.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

function createMockRadicals() {
  return [
    {
      radicalId: 1,
      character: '一',
      pinyin: 'yī',
      meaningHanViet: 'Nhất',
      meaningVi: 'Số một',
      audioUrl: 'https://example.com/audio/yi.mp3',
      videoWritingUrl: '/media/video/yi.mp4'
    },
    {
      radicalId: 4,
      character: '丿',
      pinyin: 'piě',
      meaningHanViet: 'Phiệt',
      meaningVi: 'Nét phẩy',
      audioUrl: null,
      videoWritingUrl: null
    },
    {
      radicalId: 75,
      character: '木',
      pinyin: 'mù',
      meaningHanViet: 'Mộc',
      meaningVi: 'Cây, gỗ',
      audioUrl: 'https://example.com/audio/mu.mp3',
      videoWritingUrl: '/media/video/mu.mp4'
    },
    {
      radicalId: 85,
      character: '水',
      pinyin: 'shuǐ',
      meaningHanViet: 'Thủy',
      meaningVi: 'Nước',
      audioUrl: 'https://example.com/audio/shui.mp3',
      videoWritingUrl: null
    }
  ];
}

async function setupAdminSession(page, email = 'admin@example.com', accountId = 1) {
  await page.addInitScript(`
    localStorage.setItem('access_token', 'test_jwt_admin_token_task_9f3');
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
    localStorage.setItem('access_token', 'test_jwt_learner_token_task_9f3');
    localStorage.setItem('user_info', JSON.stringify({
      accountId: 99,
      emailOrPhone: 'learner@example.com',
      fullName: 'Học Viên Thường',
      roles: ['Learner']
    }));
  `);
}

describe('Browser E2E: Admin Radicals CRUD UI (Task 9F.3)', () => {
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
  // RA-01: Admin opens page and sees catalog + CRUD controls
  // ---------------------------------------------------------------------------
  test('RA-01: Admin can open admin-radicals.html and view catalog table and CRUD controls', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      const radicals = createMockRadicals();

      await page.route(/\/api\/v1\/radicals(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: radicals,
              totalElements: radicals.length,
              page: 0,
              size: 250,
              totalPages: 1
            }
          })
        });
      });

      await page.goto(`${baseUrl}/admin-radicals.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#radicalTableBody tr', { timeout: 5000 });

      // Check Heading & Navigation
      const heading = await page.locator('h1').textContent();
      assert.match(heading, /Quản Trị Danh Mục Bộ Thủ/);

      // Check Button "+ Thêm bộ thủ mới"
      const btnCreate = page.locator('#btnOpenCreateModal');
      assert.strictEqual(await btnCreate.isVisible(), true);

      // Check Table rows
      const rowCount = await page.locator('#radicalTableBody tr').count();
      assert.strictEqual(rowCount, radicals.length);

      // Check search input presence
      const searchInput = page.locator('#radicalSearchInput');
      assert.strictEqual(await searchInput.isVisible(), true);

      // Verify zero console errors
      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Unexpected errors: ${errors.map(e => e.message).join(', ')}`);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // RA-02: Admin creates a valid radical -> 201 Created -> UI reflects new state
  // ---------------------------------------------------------------------------
  test('RA-02: Admin creates a valid radical and UI reflects created server state', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      const radicals = createMockRadicals();
      let createPayloadReceived = null;

      await page.route(/\/api\/v1\/radicals(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: { items: radicals, totalElements: radicals.length, page: 0, size: 250, totalPages: 1 }
          })
        });
      });

      await page.route(/\/api\/v1\/admin\/radicals$/, async (route) => {
        if (route.request().method() === 'POST') {
          createPayloadReceived = JSON.parse(route.request().postData());
          const newRadical = {
            radicalId: 86,
            character: createPayloadReceived.character,
            pinyin: createPayloadReceived.pinyin,
            meaningHanViet: createPayloadReceived.meaningHanViet,
            meaningVi: createPayloadReceived.meaningVi,
            audioUrl: createPayloadReceived.audioUrl,
            videoWritingUrl: createPayloadReceived.videoWritingUrl
          };
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'Tạo bộ thủ thành công',
              data: newRadical
            })
          });
        } else {
          await route.continue();
        }
      });

      await page.goto(`${baseUrl}/admin-radicals.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#radicalTableBody tr', { timeout: 5000 });

      // Click "+ Thêm bộ thủ mới"
      await page.locator('#btnOpenCreateModal').click();
      await page.waitForSelector('#radicalFormModal[open]', { timeout: 3000 });

      // Fill in fields
      await page.locator('#inputCharacter').fill('火');
      await page.locator('#inputPinyin').fill('huǒ');
      await page.locator('#inputMeaningHanViet').fill('Hỏa');
      await page.locator('#inputMeaningVi').fill('Lửa');
      await page.locator('#inputAudioUrl').fill('https://example.com/audio/huo.mp3');

      // Submit
      await page.locator('#btnSaveRadical').click();

      // Wait for modal to close
      await page.waitForFunction(() => !document.getElementById('radicalFormModal')?.open, { timeout: 5000 });

      // Verify payload received
      assert.ok(createPayloadReceived, 'Backend should receive POST request');
      assert.strictEqual(createPayloadReceived.character, '火');
      assert.strictEqual(createPayloadReceived.pinyin, 'huǒ');
      assert.strictEqual(createPayloadReceived.meaningHanViet, 'Hỏa');
      assert.strictEqual(createPayloadReceived.meaningVi, 'Lửa');

      // Verify table now contains 5 rows
      const rowCount = await page.locator('#radicalTableBody tr').count();
      assert.strictEqual(rowCount, 5);

      // Verify new row is visible in table
      const newRowText = await page.locator('#radicalTableBody').textContent();
      assert.match(newRowText, /火/);
      assert.match(newRowText, /huǒ/);
      assert.match(newRowText, /Hỏa/);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // RA-03: Duplicate-character create returns clear 409 conflict feedback
  // ---------------------------------------------------------------------------
  test('RA-03: Duplicate-character create returns clear 409 conflict feedback', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      const radicals = createMockRadicals();

      await page.route(/\/api\/v1\/radicals(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: { items: radicals, totalElements: radicals.length, page: 0, size: 250, totalPages: 1 }
          })
        });
      });

      await page.route(/\/api\/v1\/admin\/radicals$/, async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 409,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'CONFLICT',
              message: "Bộ thủ với ký tự '木' đã tồn tại",
              data: null
            })
          });
        } else {
          await route.continue();
        }
      });

      await page.goto(`${baseUrl}/admin-radicals.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#radicalTableBody tr', { timeout: 5000 });

      // Click create modal
      await page.locator('#btnOpenCreateModal').click();
      await page.waitForSelector('#radicalFormModal[open]', { timeout: 3000 });

      // Fill duplicate character
      await page.locator('#inputCharacter').fill('木');
      await page.locator('#inputPinyin').fill('mù');
      await page.locator('#inputMeaningHanViet').fill('Mộc');
      await page.locator('#inputMeaningVi').fill('Cây cối');

      // Submit
      await page.locator('#btnSaveRadical').click();

      // Modal must remain open
      const isModalOpen = await page.locator('#radicalFormModal').evaluate(el => el.open);
      assert.strictEqual(isModalOpen, true, 'Form modal should remain open on conflict');

      // Error alert must be visible and contain conflict message
      const errorAlert = page.locator('#formGeneralError');
      await errorAlert.waitFor({ state: 'visible', timeout: 3000 });
      const errorText = await errorAlert.textContent();
      assert.match(errorText, /đã tồn tại/);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // RA-04: Admin edits an existing radical and UI reflects returned server state
  // ---------------------------------------------------------------------------
  test('RA-04: Admin edits an existing radical and UI reflects updated server state', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      const radicals = createMockRadicals();
      let putPayloadReceived = null;

      await page.route(/\/api\/v1\/radicals(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: { items: radicals, totalElements: radicals.length, page: 0, size: 250, totalPages: 1 }
          })
        });
      });

      await page.route(/\/api\/v1\/admin\/radicals\/75$/, async (route) => {
        if (route.request().method() === 'PUT') {
          putPayloadReceived = JSON.parse(route.request().postData());
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'SUCCESS',
              message: 'Cập nhật bộ thủ thành công',
              data: {
                radicalId: 75,
                character: '木',
                pinyin: 'mù',
                meaningHanViet: 'Mộc',
                meaningVi: putPayloadReceived.meaningVi,
                audioUrl: putPayloadReceived.audioUrl,
                videoWritingUrl: putPayloadReceived.videoWritingUrl
              }
            })
          });
        } else {
          await route.continue();
        }
      });

      await page.goto(`${baseUrl}/admin-radicals.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#radicalTableBody tr', { timeout: 5000 });

      // Click Edit on radical 75 (木)
      const editBtn = page.locator('button[aria-label="Chỉnh sửa bộ thủ 木"]');
      await editBtn.click();
      await page.waitForSelector('#radicalFormModal[open]', { timeout: 3000 });

      // Verify prefilled values
      const charVal = await page.locator('#inputCharacter').inputValue();
      const viVal = await page.locator('#inputMeaningVi').inputValue();
      assert.strictEqual(charVal, '木');
      assert.strictEqual(viVal, 'Cây, gỗ');

      // Edit meaningVi
      await page.locator('#inputMeaningVi').fill('Cây cối, gỗ rừng (Đã cập nhật)');

      // Submit
      await page.locator('#btnSaveRadical').click();
      await page.waitForFunction(() => !document.getElementById('radicalFormModal')?.open, { timeout: 5000 });

      // Verify PUT payload
      assert.ok(putPayloadReceived);
      assert.strictEqual(putPayloadReceived.meaningVi, 'Cây cối, gỗ rừng (Đã cập nhật)');

      // Verify table row updated with authoritative data
      const tableContent = await page.locator('#radicalTableBody').textContent();
      assert.match(tableContent, /Cây cối, gỗ rừng \(Đã cập nhật\)/);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // RA-05: Admin confirms deletion and successful 204 removes radical from UI
  // ---------------------------------------------------------------------------
  test('RA-05: Admin confirms deletion and successful 204 removes radical from UI', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      const radicals = createMockRadicals();
      let deleteRequestedId = null;

      await page.route(/\/api\/v1\/radicals(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: { items: radicals, totalElements: radicals.length, page: 0, size: 250, totalPages: 1 }
          })
        });
      });

      await page.route(/\/api\/v1\/admin\/radicals\/4$/, async (route) => {
        if (route.request().method() === 'DELETE') {
          deleteRequestedId = 4;
          // HTTP 204 No Content with empty body
          await route.fulfill({
            status: 204,
            body: ''
          });
        } else {
          await route.continue();
        }
      });

      await page.goto(`${baseUrl}/admin-radicals.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#radicalTableBody tr', { timeout: 5000 });

      // Click Delete on radical 4 (丿)
      const deleteBtn = page.locator('button[aria-label="Xóa bộ thủ 丿"]');
      await deleteBtn.click();
      await page.waitForSelector('#deleteRadicalModal[open]', { timeout: 3000 });

      // Verify target info in modal
      const targetChar = await page.locator('#deleteTargetCharacter').textContent();
      assert.match(targetChar, /丿/);

      // Confirm deletion
      await page.locator('#btnConfirmDeleteRadical').click();
      await page.waitForFunction(() => !document.getElementById('deleteRadicalModal')?.open, { timeout: 5000 });

      // Verify DELETE API called with ID 4
      assert.strictEqual(deleteRequestedId, 4);

      // Verify radical 4 is removed from table (4 rows -> 3 rows)
      const rowCount = await page.locator('#radicalTableBody tr').count();
      assert.strictEqual(rowCount, 3);

      const tableContent = await page.locator('#radicalTableBody').textContent();
      assert.doesNotMatch(tableContent, /Phiệt/);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // RA-06: Referenced-radical deletion returns clear 409 reference-conflict feedback
  // ---------------------------------------------------------------------------
  test('RA-06: Referenced-radical deletion returns clear 409 reference-conflict feedback', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);
      const radicals = createMockRadicals();

      await page.route(/\/api\/v1\/radicals(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: { items: radicals, totalElements: radicals.length, page: 0, size: 250, totalPages: 1 }
          })
        });
      });

      await page.route(/\/api\/v1\/admin\/radicals\/1$/, async (route) => {
        if (route.request().method() === 'DELETE') {
          await route.fulfill({
            status: 409,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'CONFLICT',
              message: 'Không thể xóa bộ thủ đang được liên kết với từ vựng',
              data: null
            })
          });
        } else {
          await route.continue();
        }
      });

      await page.goto(`${baseUrl}/admin-radicals.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#radicalTableBody tr', { timeout: 5000 });

      // Click delete on radical 1 (一)
      const deleteBtn = page.locator('button[aria-label="Xóa bộ thủ 一"]');
      await deleteBtn.click();
      await page.waitForSelector('#deleteRadicalModal[open]', { timeout: 3000 });

      // Confirm deletion
      await page.locator('#btnConfirmDeleteRadical').click();

      // Modal must remain open and display reference conflict error
      const isModalOpen = await page.locator('#deleteRadicalModal').evaluate(el => el.open);
      assert.strictEqual(isModalOpen, true, 'Delete modal should remain open when deletion is blocked');

      const errorAlert = page.locator('#deleteModalError');
      await errorAlert.waitFor({ state: 'visible', timeout: 3000 });
      const errorText = await errorAlert.textContent();
      assert.match(errorText, /liên kết với từ vựng/);

      // Radical must NOT be removed from table
      const rowCount = await page.locator('#radicalTableBody tr').count();
      assert.strictEqual(rowCount, radicals.length);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // RA-07: Non-Admin session is blocked by role guard
  // ---------------------------------------------------------------------------
  test('RA-07: Non-Admin is blocked by role guard and sees access requirement alert', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupLearnerSession(page);

      await page.goto(`${baseUrl}/admin-radicals.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#adminAuthGuardContainer', { timeout: 5000 });

      // Role guard container must be visible
      const guardContainer = page.locator('#adminAuthGuardContainer');
      assert.strictEqual(await guardContainer.isVisible(), true);

      // Main management content must be hidden
      const mainContent = page.locator('#adminRadicalsMainContent');
      assert.strictEqual(await mainContent.isVisible(), false);
    } finally {
      await context.close();
    }
  });

  // ---------------------------------------------------------------------------
  // RA-08: Verification Center Section 17 executes read-only capability probe
  // ---------------------------------------------------------------------------
  test('RA-08: Verification Center Section 17 executes capability probe with zero mutations', async () => {
    const { page, context } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await setupAdminSession(page);

      await page.route(/\/api\/v1\/radicals(\?.*)?$/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'SUCCESS',
            message: 'Thành công',
            data: {
              items: [{ radicalId: 1, character: '一', meaningHanViet: 'Nhất', meaningVi: 'Số một' }],
              totalElements: 214,
              page: 0,
              size: 1,
              totalPages: 214
            }
          })
        });
      });

      await page.goto(`${baseUrl}/ui-verification.html#section-admin-radicals`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#btnProbeAdminRadicalsCapability', { timeout: 5000 });

      // Run capability probe
      await page.locator('#btnProbeAdminRadicalsCapability').click();
      await page.waitForFunction(() => {
        const out = document.getElementById('adminRadicalsProbeOutput');
        return out && out.textContent.includes('[PROBE PASS]');
      }, { timeout: 5000 });

      const outputText = await page.locator('#adminRadicalsProbeOutput').textContent();
      assert.match(outputText, /1\. Form Validation \(Giới hạn DTO\): PASS/);
      assert.match(outputText, /2\. Media URL Security Check: PASS/);
      assert.match(outputText, /3\. Payload Construction: PASS/);
      assert.match(outputText, /4\. Conflict Classification \(409\/404\): PASS/);
      assert.match(outputText, /5\. Client-Side Search \(Diacritics\): PASS/);
      assert.match(outputText, /ZERO CRUD MUTATION/);
      assert.match(outputText, /\[PROBE PASS\]/);
    } finally {
      await context.close();
    }
  });

});
