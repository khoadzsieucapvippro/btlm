/**
 * =============================================================================
 * TARGETED UNIT TESTS: ADMIN RADICALS CRUD UI (TASK 9F.3)
 * Module: tests/frontend/unit/admin/admin-radicals.test.mjs
 * 
 * Scope:
 * - Pure logic unit testing of admin-radicals-page.js functions:
 *   1. hasAdminAccess: Admin role guard invariant (rejects ROLE_ADMIN, handles edge cases).
 *   2. removeVietnameseDiacritics: Vietnamese tone/diacritic normalization.
 *   3. normalizeSearchQuery: Trims and lowercases user search queries.
 *   4. filterRadicals: Multi-field fuzzy search (character, pinyin, meaningHanViet, meaningVi).
 *   5. validateRadicalForm: Client-side validation against backend contract limits.
 *   6. isValidMediaUrl: Safe URI scheme and relative path validator.
 *   7. buildRadicalPayload: Normalizes and constructs clean request DTO payload.
 *   8. classifyApiError: Error classification (409 duplicate vs referenced, 404 stale, 400 validation).
 *   9. calculatePagination: Page bounds and slicing calculation.
 * =============================================================================
 */

import { describe, test } from 'node:test';
import assert from 'node:assert/strict';

import {
  hasAdminAccess,
  removeVietnameseDiacritics,
  normalizeSearchQuery,
  filterRadicals,
  validateRadicalForm,
  isValidMediaUrl,
  buildRadicalPayload,
  classifyApiError,
  calculatePagination
} from '../../../../frontend/js/pages/admin-radicals-page.js';

describe('Task 9F.3 — Admin Radicals Pure Logic Unit Tests', () => {

  /* ---------------------------------------------------------------------------
   * 1. hasAdminAccess (RBAC Role Guard Invariant)
   * --------------------------------------------------------------------------- */
  describe('1. hasAdminAccess', () => {
    test('returns true when session has authoritative Admin role', () => {
      const mockManager = { hasRole: (role) => role === 'Admin' };
      assert.strictEqual(hasAdminAccess(mockManager), true);
    });

    test('strictly rejects ROLE_ADMIN prefix (contract requires canonical Admin)', () => {
      const mockManager = { hasRole: (role) => role === 'ROLE_ADMIN' };
      assert.strictEqual(hasAdminAccess(mockManager), false);
    });

    test('returns false for non-admin roles (Learner, Creator, Moderator)', () => {
      const mockLearner = { hasRole: (role) => role === 'Learner' };
      assert.strictEqual(hasAdminAccess(mockLearner), false);

      const mockCreator = { hasRole: (role) => role === 'Creator' };
      assert.strictEqual(hasAdminAccess(mockCreator), false);

      const mockModerator = { hasRole: (role) => role === 'Moderator' };
      assert.strictEqual(hasAdminAccess(mockModerator), false);
    });

    test('returns false when authManager is undefined, null, or lacks hasRole', () => {
      assert.strictEqual(hasAdminAccess(null), false);
      assert.strictEqual(hasAdminAccess(undefined), false);
      assert.strictEqual(hasAdminAccess({}), false);
    });
  });

  /* ---------------------------------------------------------------------------
   * 2. removeVietnameseDiacritics & normalizeSearchQuery
   * --------------------------------------------------------------------------- */
  describe('2. Diacritics and Query Normalization', () => {
    test('removes Vietnamese tone marks and converts đ/Đ to d/D', () => {
      assert.strictEqual(removeVietnameseDiacritics('Mộc'), 'Moc');
      assert.strictEqual(removeVietnameseDiacritics('Nước'), 'Nuoc');
      assert.strictEqual(removeVietnameseDiacritics('đường đi'), 'duong di');
      assert.strictEqual(removeVietnameseDiacritics('ĐẠI HỌC'), 'DAI HOC');
    });

    test('preserves Chinese glyphs and standard ASCII strings', () => {
      assert.strictEqual(removeVietnameseDiacritics('木'), '木');
      assert.strictEqual(removeVietnameseDiacritics('shuǐ'), 'shui');
    });

    test('handles null, undefined, or empty strings gracefully', () => {
      assert.strictEqual(removeVietnameseDiacritics(''), '');
      assert.strictEqual(removeVietnameseDiacritics(null), '');
      assert.strictEqual(removeVietnameseDiacritics(undefined), '');
    });

    test('normalizes search query by trimming whitespace and lowercasing', () => {
      assert.strictEqual(normalizeSearchQuery('   MỘC   '), 'mộc');
      assert.strictEqual(normalizeSearchQuery('Shui'), 'shui');
      assert.strictEqual(normalizeSearchQuery(null), '');
      assert.strictEqual(normalizeSearchQuery(undefined), '');
    });
  });

  /* ---------------------------------------------------------------------------
   * 3. filterRadicals (Client-side Search Architecture)
   * --------------------------------------------------------------------------- */
  describe('3. filterRadicals', () => {
    const sampleRadicals = [
      { radicalId: 1, character: '一', pinyin: 'yī', meaningHanViet: 'Nhất', meaningVi: 'Số một' },
      { radicalId: 4, character: '丿', pinyin: 'piě', meaningHanViet: 'Phiệt', meaningVi: 'Nét phẩy' },
      { radicalId: 75, character: '木', pinyin: 'mù', meaningHanViet: 'Mộc', meaningVi: 'Cây, gỗ' },
      { radicalId: 85, character: '水', pinyin: 'shuǐ', meaningHanViet: 'Thủy', meaningVi: 'Nước' },
      { radicalId: 86, character: '火', pinyin: 'huǒ', meaningHanViet: 'Hỏa', meaningVi: 'Lửa' }
    ];

    test('returns all items when query is empty or whitespace', () => {
      const result = filterRadicals('', sampleRadicals);
      assert.strictEqual(result.length, 5);
      assert.strictEqual(filterRadicals('   ', sampleRadicals).length, 5);
    });

    test('matches by Chinese character glyph', () => {
      const result = filterRadicals('木', sampleRadicals);
      assert.strictEqual(result.length, 1);
      assert.strictEqual(result[0].radicalId, 75);
    });

    test('matches by Pinyin (exact or partial)', () => {
      const result = filterRadicals('mu', sampleRadicals);
      assert.strictEqual(result.length, 1);
      assert.strictEqual(result[0].character, '木');
    });

    test('matches by Hán-Việt meaning with accents and without accents', () => {
      const withAccents = filterRadicals('Thủy', sampleRadicals);
      assert.strictEqual(withAccents.length, 1);
      assert.strictEqual(withAccents[0].character, '水');

      const withoutAccents = filterRadicals('thuy', sampleRadicals);
      assert.strictEqual(withoutAccents.length, 1);
      assert.strictEqual(withoutAccents[0].character, '水');
    });

    test('matches by Vietnamese definition with accents and without accents', () => {
      const withAccents = filterRadicals('Nước', sampleRadicals);
      assert.strictEqual(withAccents.length, 1);
      assert.strictEqual(withAccents[0].character, '水');

      const withoutAccents = filterRadicals('nuoc', sampleRadicals);
      assert.strictEqual(withoutAccents.length, 1);
      assert.strictEqual(withoutAccents[0].character, '水');
    });

    test('returns empty array when no radical matches the query', () => {
      const result = filterRadicals('KhôngTồnTại', sampleRadicals);
      assert.strictEqual(result.length, 0);
    });

    test('safely handles non-array input', () => {
      assert.deepStrictEqual(filterRadicals('木', null), []);
      assert.deepStrictEqual(filterRadicals('木', undefined), []);
    });
  });

  /* ---------------------------------------------------------------------------
   * 4. validateRadicalForm & isValidMediaUrl (Contract Validation)
   * --------------------------------------------------------------------------- */
  describe('4. validateRadicalForm & Media URL Validation', () => {
    test('passes validation with valid required fields and optional URLs', () => {
      const validData = {
        character: '木',
        pinyin: 'mù',
        meaningHanViet: 'Mộc',
        meaningVi: 'Cây, gỗ',
        audioUrl: 'https://cdn.example.com/audio/mu.mp3',
        videoWritingUrl: '/media/video/mu.mp4'
      };
      const result = validateRadicalForm(validData);
      assert.strictEqual(result.valid, true);
      assert.deepStrictEqual(result.errors, {});
    });

    test('passes validation when optional URLs are omitted or empty', () => {
      const validData = {
        character: '火',
        pinyin: 'huǒ',
        meaningHanViet: 'Hỏa',
        meaningVi: 'Lửa',
        audioUrl: '',
        videoWritingUrl: null
      };
      const result = validateRadicalForm(validData);
      assert.strictEqual(result.valid, true);
    });

    test('flags missing required fields with clear error messages', () => {
      const emptyData = {
        character: '',
        pinyin: '   ',
        meaningHanViet: '',
        meaningVi: ''
      };
      const result = validateRadicalForm(emptyData);
      assert.strictEqual(result.valid, false);
      assert.ok(result.errors.character, 'Should flag empty character');
      assert.ok(result.errors.pinyin, 'Should flag empty pinyin');
      assert.ok(result.errors.meaningHanViet, 'Should flag empty meaningHanViet');
      assert.ok(result.errors.meaningVi, 'Should flag empty meaningVi');
    });

    test('enforces max length constraints (10, 50, 100, 255, 500)', () => {
      const tooLongData = {
        character: 'A'.repeat(11),
        pinyin: 'B'.repeat(51),
        meaningHanViet: 'C'.repeat(101),
        meaningVi: 'D'.repeat(256),
        audioUrl: 'https://example.com/' + 'E'.repeat(500),
        videoWritingUrl: 'https://example.com/' + 'F'.repeat(500)
      };
      const result = validateRadicalForm(tooLongData);
      assert.strictEqual(result.valid, false);
      assert.match(result.errors.character, /10/);
      assert.match(result.errors.pinyin, /50/);
      assert.match(result.errors.meaningHanViet, /100/);
      assert.match(result.errors.meaningVi, /255/);
      assert.match(result.errors.audioUrl, /500/);
      assert.match(result.errors.videoWritingUrl, /500/);
    });

    test('validates media URL formats via isValidMediaUrl', () => {
      assert.strictEqual(isValidMediaUrl('https://example.com/audio.mp3'), true);
      assert.strictEqual(isValidMediaUrl('http://example.com/audio.mp3'), true);
      assert.strictEqual(isValidMediaUrl('/media/audio.mp3'), true);

      // Rejects unsafe schemes and invalid formats
      assert.strictEqual(isValidMediaUrl('javascript:alert(1)'), false);
      assert.strictEqual(isValidMediaUrl('data:text/html,<script>'), false);
      assert.strictEqual(isValidMediaUrl('ftp://example.com/audio.mp3'), false);
      assert.strictEqual(isValidMediaUrl('//malicious.com/audio.mp3'), false);
      assert.strictEqual(isValidMediaUrl('not-a-url'), false);
    });

    test('flags invalid media URL scheme in form validation', () => {
      const invalidUrlData = {
        character: '木',
        pinyin: 'mù',
        meaningHanViet: 'Mộc',
        meaningVi: 'Cây, gỗ',
        audioUrl: 'javascript:alert(1)',
        videoWritingUrl: 'ftp://invalidscheme.com'
      };
      const result = validateRadicalForm(invalidUrlData);
      assert.strictEqual(result.valid, false);
      assert.ok(result.errors.audioUrl);
      assert.ok(result.errors.videoWritingUrl);
    });
  });

  /* ---------------------------------------------------------------------------
   * 5. buildRadicalPayload (Request Construction)
   * --------------------------------------------------------------------------- */
  describe('5. buildRadicalPayload', () => {
    test('trims whitespace on all string fields', () => {
      const raw = {
        character: '  木  ',
        pinyin: '  mù  ',
        meaningHanViet: '  Mộc  ',
        meaningVi: '  Cây, gỗ  ',
        audioUrl: '  https://example.com/mu.mp3  ',
        videoWritingUrl: '  /media/mu.mp4  '
      };
      const payload = buildRadicalPayload(raw);
      assert.strictEqual(payload.character, '木');
      assert.strictEqual(payload.pinyin, 'mù');
      assert.strictEqual(payload.meaningHanViet, 'Mộc');
      assert.strictEqual(payload.meaningVi, 'Cây, gỗ');
      assert.strictEqual(payload.audioUrl, 'https://example.com/mu.mp3');
      assert.strictEqual(payload.videoWritingUrl, '/media/mu.mp4');
    });

    test('normalizes empty or whitespace-only optional URLs to null', () => {
      const raw = {
        character: '木',
        pinyin: 'mù',
        meaningHanViet: 'Mộc',
        meaningVi: 'Cây, gỗ',
        audioUrl: '   ',
        videoWritingUrl: ''
      };
      const payload = buildRadicalPayload(raw);
      assert.strictEqual(payload.audioUrl, null);
      assert.strictEqual(payload.videoWritingUrl, null);
    });
  });

  /* ---------------------------------------------------------------------------
   * 6. classifyApiError (Conflict & Status Classification)
   * --------------------------------------------------------------------------- */
  describe('6. classifyApiError', () => {
    test('correctly classifies 409 duplicate character conflict on create/update', () => {
      const err = {
        status: 409,
        code: 'CONFLICT',
        message: "Bộ thủ với ký tự '木' đã tồn tại"
      };
      const classified = classifyApiError(err, 'create');
      assert.strictEqual(classified.isConflict, true);
      assert.strictEqual(classified.status, 409);
      assert.strictEqual(classified.userMessage, "Bộ thủ với ký tự '木' đã tồn tại");
    });

    test('correctly classifies 409 referenced radical conflict on delete', () => {
      const err = {
        status: 409,
        code: 'CONFLICT',
        message: 'Không thể xóa bộ thủ đang được liên kết với từ vựng'
      };
      const classified = classifyApiError(err, 'delete');
      assert.strictEqual(classified.isConflict, true);
      assert.strictEqual(classified.status, 409);
      assert.strictEqual(classified.userMessage, 'Không thể xóa bộ thủ đang được liên kết với từ vựng');
    });

    test('handles fallback conflict message when server message is missing', () => {
      const err = { status: 409 };
      const createClassified = classifyApiError(err, 'create');
      assert.match(createClassified.userMessage, /đã tồn tại/);

      const deleteClassified = classifyApiError(err, 'delete');
      assert.match(deleteClassified.userMessage, /liên kết/);
    });

    test('correctly classifies 404 not found (stale record)', () => {
      const err = {
        status: 404,
        code: 'NOT_FOUND',
        message: 'Không tìm thấy bộ thủ với ID: 999'
      };
      const classified = classifyApiError(err, 'delete');
      assert.strictEqual(classified.isNotFound, true);
      assert.strictEqual(classified.status, 404);
      assert.strictEqual(classified.userMessage, 'Không tìm thấy bộ thủ với ID: 999');
    });

    test('maps field errors for 400 validation error', () => {
      const err = {
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'Dữ liệu không hợp lệ',
        errors: [
          'character: Ký tự bộ thủ không được để trống',
          'pinyin: Pinyin không được để trống'
        ]
      };
      const classified = classifyApiError(err, 'create');
      assert.strictEqual(classified.isValidation, true);
      assert.strictEqual(classified.fieldErrors.character, 'Ký tự bộ thủ không được để trống');
      assert.strictEqual(classified.fieldErrors.pinyin, 'Pinyin không được để trống');
    });

    test('handles network failure (status 0)', () => {
      const err = { status: 0, code: 'NETWORK_ERROR' };
      const classified = classifyApiError(err, 'load');
      assert.match(classified.userMessage, /kết nối/i);
    });
  });

  /* ---------------------------------------------------------------------------
   * 7. calculatePagination (Pagination Bounds)
   * --------------------------------------------------------------------------- */
  describe('7. calculatePagination', () => {
    test('calculates correct bounds for first page', () => {
      const result = calculatePagination(100, 0, 25);
      assert.strictEqual(result.totalPages, 4);
      assert.strictEqual(result.startIndex, 0);
      assert.strictEqual(result.endIndex, 25);
      assert.strictEqual(result.pageIndex, 0);
    });

    test('calculates correct bounds for middle page', () => {
      const result = calculatePagination(100, 2, 25);
      assert.strictEqual(result.startIndex, 50);
      assert.strictEqual(result.endIndex, 75);
    });

    test('calculates correct bounds for last page with remainder', () => {
      const result = calculatePagination(214, 4, 50);
      assert.strictEqual(result.totalPages, 5);
      assert.strictEqual(result.startIndex, 200);
      assert.strictEqual(result.endIndex, 214);
    });

    test('clamps out-of-range page index to bounds', () => {
      const clampedHigh = calculatePagination(50, 10, 25);
      assert.strictEqual(clampedHigh.pageIndex, 1);

      const clampedLow = calculatePagination(50, -5, 25);
      assert.strictEqual(clampedLow.pageIndex, 0);
    });

    test('handles 0 total items', () => {
      const result = calculatePagination(0, 0, 50);
      assert.strictEqual(result.totalPages, 1);
      assert.strictEqual(result.startIndex, 0);
      assert.strictEqual(result.endIndex, 0);
    });
  });

});
