/**
 * =============================================================================
 * TARGETED UNIT TESTS: ADMIN VOCABULARY CRUD UI (TASK 9F.4)
 * Module: tests/frontend/unit/admin/admin-vocabulary.test.mjs
 * 
 * Scope:
 * - Pure logic unit testing of admin-vocabulary-page.js functions:
 *   1. hasAdminAccess: Admin role guard invariant.
 *   2. toPinyinRaw: Tone diacritic stripping and ü/v -> u normalization matching backend.
 *   3. normalizeSearchQuery: Trims whitespace from queries.
 *   4. isValidMediaUrl: Safe web URI scheme and relative path validator.
 *   5. validateVocabularyForm: Client-side validation against backend DTO contracts.
 *   6. buildVocabularyPayload: Normalizes and constructs clean request payload.
 *   7. isNoOpUpdate: Detects unchanged updates to avoid redundant PUT requests.
 *   8. classifyApiError: Error classification (409 duplicate vs referenced, 404 stale, 400 validation).
 *   9. filterRadicalsList: Client-side radical filtering for constituent radical picker.
 *   10. parsePageResponse & calculatePagination: Server-side pagination helpers.
 * =============================================================================
 */

import { describe, test } from 'node:test';
import assert from 'node:assert/strict';

import {
  hasAdminAccess,
  toPinyinRaw,
  normalizeSearchQuery,
  isValidMediaUrl,
  validateVocabularyForm,
  buildVocabularyPayload,
  isNoOpUpdate,
  classifyApiError,
  filterRadicalsList,
  parsePageResponse,
  calculatePagination
} from '../../../../frontend/js/pages/admin-vocabulary-page.js';

describe('Task 9F.4 — Admin Vocabulary Pure Logic Unit Tests', () => {

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
      assert.strictEqual(hasAdminAccess({ hasRole: (r) => r === 'Learner' }), false);
      assert.strictEqual(hasAdminAccess({ hasRole: (r) => r === 'Creator' }), false);
      assert.strictEqual(hasAdminAccess({ hasRole: (r) => r === 'Moderator' }), false);
    });

    test('returns false when authManager is undefined, null, or lacks hasRole', () => {
      assert.strictEqual(hasAdminAccess(null), false);
      assert.strictEqual(hasAdminAccess(undefined), false);
      assert.strictEqual(hasAdminAccess({}), false);
    });
  });

  /* ---------------------------------------------------------------------------
   * 2. toPinyinRaw (Pinyin Normalization)
   * --------------------------------------------------------------------------- */
  describe('2. toPinyinRaw', () => {
    test('strips tone diacritics from accented pinyin', () => {
      assert.strictEqual(toPinyinRaw('hǎo'), 'hao');
      assert.strictEqual(toPinyinRaw('xué'), 'xue');
      assert.strictEqual(toPinyinRaw('zhōng'), 'zhong');
      assert.strictEqual(toPinyinRaw('guó'), 'guo');
    });

    test('normalizes ü, Ü, and v characters to u', () => {
      assert.strictEqual(toPinyinRaw('nǚ'), 'nu');
      assert.strictEqual(toPinyinRaw('lǜ'), 'lu');
      assert.strictEqual(toPinyinRaw('lü'), 'lu');
      assert.strictEqual(toPinyinRaw('nv'), 'nu');
      assert.strictEqual(toPinyinRaw('lǘ'), 'lu');
      assert.strictEqual(toPinyinRaw('nǖ'), 'nu');
    });

    test('trims whitespace and converts to lowercase', () => {
      assert.strictEqual(toPinyinRaw(' Nǐ Hǎo '), 'ni hao');
      assert.strictEqual(toPinyinRaw('Túshūguǎn'), 'tushuguan');
      assert.strictEqual(toPinyinRaw('Běijīng Dàxué'), 'beijing daxue');
    });

    test('handles empty or non-string inputs safely', () => {
      assert.strictEqual(toPinyinRaw(''), '');
      assert.strictEqual(toPinyinRaw(null), '');
      assert.strictEqual(toPinyinRaw(undefined), '');
    });
  });

  /* ---------------------------------------------------------------------------
   * 3. normalizeSearchQuery
   * --------------------------------------------------------------------------- */
  describe('3. normalizeSearchQuery', () => {
    test('trims leading and trailing whitespace', () => {
      assert.strictEqual(normalizeSearchQuery('   你好   '), '你好');
      assert.strictEqual(normalizeSearchQuery('  ni hao  '), 'ni hao');
    });

    test('handles empty, null, and undefined values', () => {
      assert.strictEqual(normalizeSearchQuery(''), '');
      assert.strictEqual(normalizeSearchQuery(null), '');
      assert.strictEqual(normalizeSearchQuery(undefined), '');
    });
  });

  /* ---------------------------------------------------------------------------
   * 4. isValidMediaUrl
   * --------------------------------------------------------------------------- */
  describe('4. isValidMediaUrl', () => {
    test('accepts valid http, https, and safe relative URLs', () => {
      assert.strictEqual(isValidMediaUrl('https://example.com/audio/test.mp3'), true);
      assert.strictEqual(isValidMediaUrl('http://example.com/video/test.mp4'), true);
      assert.strictEqual(isValidMediaUrl('/media/audio/test.mp3'), true);
      assert.strictEqual(isValidMediaUrl('/static/video/char.mp4'), true);
    });

    test('rejects unsafe schemes and malformed paths', () => {
      assert.strictEqual(isValidMediaUrl('javascript:alert(1)'), false);
      assert.strictEqual(isValidMediaUrl('data:text/html;base64,...'), false);
      assert.strictEqual(isValidMediaUrl('//malicious.com/payload.js'), false);
      assert.strictEqual(isValidMediaUrl(''), false);
      assert.strictEqual(isValidMediaUrl(null), false);
      assert.strictEqual(isValidMediaUrl(undefined), false);
    });
  });

  /* ---------------------------------------------------------------------------
   * 5. validateVocabularyForm (DTO Constraints)
   * --------------------------------------------------------------------------- */
  describe('5. validateVocabularyForm', () => {
    const validFullData = {
      hanzi: '你好',
      pinyin: 'nǐ hǎo',
      pinyinRaw: 'ni hao',
      meaningHanViet: 'nhĩ hảo',
      meaningVi: 'xin chào',
      audioUrl: 'https://example.com/audio.mp3',
      videoWritingUrl: '/media/video.mp4',
      exampleSentence: '你好，很高兴认识你。',
      exampleTranslation: 'Xin chào, rất vui được gặp bạn.',
      radicalIds: [9, 75]
    };

    test('accepts valid data with all fields populated', () => {
      const res = validateVocabularyForm(validFullData);
      assert.strictEqual(res.valid, true);
      assert.deepStrictEqual(res.errors, {});
    });

    test('accepts valid data with empty optional fields and empty radicalIds', () => {
      const minimalData = {
        hanzi: '学',
        pinyin: 'xué',
        pinyinRaw: 'xue',
        meaningHanViet: 'học',
        meaningVi: 'học tập',
        audioUrl: '',
        videoWritingUrl: '',
        exampleSentence: '',
        exampleTranslation: '',
        radicalIds: []
      };
      const res = validateVocabularyForm(minimalData);
      assert.strictEqual(res.valid, true);
      assert.deepStrictEqual(res.errors, {});
    });

    test('flags errors when required fields are missing or whitespace', () => {
      const invalidData = {
        hanzi: '   ',
        pinyin: '',
        pinyinRaw: '   ',
        meaningHanViet: '',
        meaningVi: '   '
      };
      const res = validateVocabularyForm(invalidData);
      assert.strictEqual(res.valid, false);
      assert.ok(res.errors.hanzi);
      assert.ok(res.errors.pinyin);
      assert.ok(res.errors.pinyinRaw);
      assert.ok(res.errors.meaningHanViet);
      assert.ok(res.errors.meaningVi);
    });

    test('flags errors when string length limits are exceeded', () => {
      const oversizedData = {
        hanzi: 'A'.repeat(51),
        pinyin: 'B'.repeat(101),
        pinyinRaw: 'C'.repeat(101),
        meaningHanViet: 'D'.repeat(101),
        meaningVi: 'E'.repeat(256),
        audioUrl: 'https://example.com/' + 'x'.repeat(490),
        videoWritingUrl: 'https://example.com/' + 'y'.repeat(490),
        exampleSentence: 'S'.repeat(501),
        exampleTranslation: 'T'.repeat(501)
      };
      const res = validateVocabularyForm(oversizedData);
      assert.strictEqual(res.valid, false);
      assert.ok(res.errors.hanzi);
      assert.ok(res.errors.pinyin);
      assert.ok(res.errors.pinyinRaw);
      assert.ok(res.errors.meaningHanViet);
      assert.ok(res.errors.meaningVi);
      assert.ok(res.errors.audioUrl);
      assert.ok(res.errors.videoWritingUrl);
      assert.ok(res.errors.exampleSentence);
      assert.ok(res.errors.exampleTranslation);
    });

    test('flags error when media URL format is unsafe', () => {
      const dataWithBadUrl = {
        ...validFullData,
        audioUrl: 'javascript:alert(1)'
      };
      const res = validateVocabularyForm(dataWithBadUrl);
      assert.strictEqual(res.valid, false);
      assert.ok(res.errors.audioUrl);
    });

    test('flags error when radical ID is not a positive integer', () => {
      const dataWithBadRadical = {
        ...validFullData,
        radicalIds: [9, -1, 'bad']
      };
      const res = validateVocabularyForm(dataWithBadRadical);
      assert.strictEqual(res.valid, false);
      assert.ok(res.errors.radicalIds);
    });
  });

  /* ---------------------------------------------------------------------------
   * 6. buildVocabularyPayload
   * --------------------------------------------------------------------------- */
  describe('6. buildVocabularyPayload', () => {
    test('trims text fields, converts empty strings to null for optional fields', () => {
      const formData = {
        hanzi: '  中国  ',
        pinyin: '  zhōngguó  ',
        pinyinRaw: '  zhongguo  ',
        meaningHanViet: '  Trung Quốc  ',
        meaningVi: '  Nước Trung Quốc  ',
        audioUrl: '   ',
        videoWritingUrl: '',
        exampleSentence: '   ',
        exampleTranslation: null,
        radicalIds: [9, 75]
      };

      const payload = buildVocabularyPayload(formData);

      assert.strictEqual(payload.hanzi, '中国');
      assert.strictEqual(payload.pinyin, 'zhōngguó');
      assert.strictEqual(payload.pinyinRaw, 'zhongguo');
      assert.strictEqual(payload.meaningHanViet, 'Trung Quốc');
      assert.strictEqual(payload.meaningVi, 'Nước Trung Quốc');
      assert.strictEqual(payload.audioUrl, null);
      assert.strictEqual(payload.videoWritingUrl, null);
      assert.strictEqual(payload.exampleSentence, null);
      assert.strictEqual(payload.exampleTranslation, null);
      assert.deepStrictEqual(payload.radicalIds, [9, 75]);
    });

    test('deduplicates, filters, and sorts radical IDs', () => {
      const formData = {
        hanzi: '测试',
        pinyin: 'cèshì',
        pinyinRaw: 'ceshi',
        meaningHanViet: 'trắc thí',
        meaningVi: 'kiểm tra',
        radicalIds: [75, 9, 75, -5, 'invalid', 9]
      };

      const payload = buildVocabularyPayload(formData);
      assert.deepStrictEqual(payload.radicalIds, [9, 75]);
    });

    test('handles Set instance for radicalIds', () => {
      const formData = {
        hanzi: '人',
        pinyin: 'rén',
        pinyinRaw: 'ren',
        meaningHanViet: 'nhân',
        meaningVi: 'người',
        radicalIds: new Set([9, 75])
      };

      const payload = buildVocabularyPayload(formData);
      assert.deepStrictEqual(payload.radicalIds, [9, 75]);
    });
  });

  /* ---------------------------------------------------------------------------
   * 7. isNoOpUpdate
   * --------------------------------------------------------------------------- */
  describe('7. isNoOpUpdate', () => {
    const detail = {
      hanzi: '你好',
      pinyin: 'nǐ hǎo',
      pinyinRaw: 'ni hao',
      meaningHanViet: 'nhĩ hảo',
      meaningVi: 'xin chào',
      audioUrl: null,
      videoWritingUrl: null,
      exampleSentence: null,
      exampleTranslation: null,
      radicals: [{ radicalId: 9 }, { radicalId: 75 }]
    };

    test('returns true when payload is identical to existing detail', () => {
      const payload = {
        hanzi: '你好',
        pinyin: 'nǐ hǎo',
        pinyinRaw: 'ni hao',
        meaningHanViet: 'nhĩ hảo',
        meaningVi: 'xin chào',
        audioUrl: null,
        videoWritingUrl: null,
        exampleSentence: null,
        exampleTranslation: null,
        radicalIds: [9, 75]
      };
      assert.strictEqual(isNoOpUpdate(detail, payload), true);
    });

    test('returns false when any text field changes', () => {
      const payload = {
        hanzi: '您好', // changed
        pinyin: 'nǐ hǎo',
        pinyinRaw: 'ni hao',
        meaningHanViet: 'nhĩ hảo',
        meaningVi: 'xin chào',
        audioUrl: null,
        videoWritingUrl: null,
        exampleSentence: null,
        exampleTranslation: null,
        radicalIds: [9, 75]
      };
      assert.strictEqual(isNoOpUpdate(detail, payload), false);
    });

    test('returns false when radical relationships change', () => {
      const payload = {
        hanzi: '你好',
        pinyin: 'nǐ hǎo',
        pinyinRaw: 'ni hao',
        meaningHanViet: 'nhĩ hảo',
        meaningVi: 'xin chào',
        audioUrl: null,
        videoWritingUrl: null,
        exampleSentence: null,
        exampleTranslation: null,
        radicalIds: [9] // removed 75
      };
      assert.strictEqual(isNoOpUpdate(detail, payload), false);
    });

    test('returns false when either argument is falsy', () => {
      assert.strictEqual(isNoOpUpdate(null, {}), false);
      assert.strictEqual(isNoOpUpdate({}, null), false);
    });
  });

  /* ---------------------------------------------------------------------------
   * 8. classifyApiError
   * --------------------------------------------------------------------------- */
  describe('8. classifyApiError', () => {
    test('classifies duplicate (hanzi + pinyinRaw) conflict on create/update', () => {
      const err = {
        status: 409,
        message: "Từ vựng với chữ Hán '你好' và pinyin 'ni hao' đã tồn tại"
      };
      const res = classifyApiError(err, 'create');
      assert.strictEqual(res.isConflict, true);
      assert.strictEqual(res.status, 409);
      assert.ok(res.userMessage.includes("Từ vựng với chữ Hán '你好'"));
    });

    test('classifies referenced learning data conflict on delete', () => {
      const err = {
        status: 409,
        message: 'Không thể xóa từ vựng đang được liên kết với bài học'
      };
      const res = classifyApiError(err, 'delete');
      assert.strictEqual(res.isConflict, true);
      assert.strictEqual(res.status, 409);
      assert.ok(res.userMessage.includes('liên kết với bài học'));
    });

    test('classifies 404 Not Found on delete as stale state', () => {
      const err = { status: 404 };
      const res = classifyApiError(err, 'delete');
      assert.strictEqual(res.isNotFound, true);
      assert.strictEqual(res.status, 404);
      assert.ok(res.userMessage.includes('Không tìm thấy từ vựng hoặc dữ liệu đã bị xóa'));
    });

    test('extracts field errors from 400 validation response', () => {
      const err = {
        status: 400,
        errors: [
          'hanzi: Chữ Hán không được để trống',
          'pinyin: Pinyin không được vượt quá 100 ký tự'
        ]
      };
      const res = classifyApiError(err, 'create');
      assert.strictEqual(res.isValidation, true);
      assert.strictEqual(res.fieldErrors.hanzi, 'Chữ Hán không được để trống');
      assert.strictEqual(res.fieldErrors.pinyin, 'Pinyin không được vượt quá 100 ký tự');
    });

    test('handles 403 Forbidden and 401 Unauthorized appropriately', () => {
      assert.ok(classifyApiError({ status: 403 }).userMessage.includes('không có quyền'));
      assert.ok(classifyApiError({ status: 401 }).userMessage.includes('hết hạn'));
    });

    test('handles network errors gracefully', () => {
      const res = classifyApiError({ status: 0, code: 'NETWORK_ERROR' });
      assert.ok(res.userMessage.includes('kết nối'));
    });
  });

  /* ---------------------------------------------------------------------------
   * 9. filterRadicalsList
   * --------------------------------------------------------------------------- */
  describe('9. filterRadicalsList', () => {
    const radicals = [
      { radicalId: 9, character: '人', pinyin: 'rén', meaningHanViet: 'nhân', meaningVi: 'người' },
      { radicalId: 75, character: '木', pinyin: 'mù', meaningHanViet: 'mộc', meaningVi: 'cây, gỗ' },
      { radicalId: 85, character: '水', pinyin: 'shuǐ', meaningHanViet: 'thủy', meaningVi: 'nước' }
    ];

    test('returns all radicals when query is empty', () => {
      assert.strictEqual(filterRadicalsList('', radicals).length, 3);
      assert.strictEqual(filterRadicalsList(null, radicals).length, 3);
    });

    test('filters by character, pinyin, or meaning', () => {
      const matchChar = filterRadicalsList('木', radicals);
      assert.strictEqual(matchChar.length, 1);
      assert.strictEqual(matchChar[0].character, '木');

      const matchPinyin = filterRadicalsList('ren', radicals);
      assert.strictEqual(matchPinyin.length, 1);
      assert.strictEqual(matchPinyin[0].character, '人');

      const matchVi = filterRadicalsList('nước', radicals);
      assert.strictEqual(matchVi.length, 1);
      assert.strictEqual(matchVi[0].character, '水');

      const matchId = filterRadicalsList('75', radicals);
      assert.strictEqual(matchId.length, 1);
      assert.strictEqual(matchId[0].character, '木');
    });
  });

  /* ---------------------------------------------------------------------------
   * 10. parsePageResponse & calculatePagination
   * --------------------------------------------------------------------------- */
  describe('10. Pagination Helpers', () => {
    test('safely parses valid PageResponse', () => {
      const mockResp = {
        page: 2,
        size: 20,
        totalElements: 55,
        totalPages: 3,
        items: [{ vocabId: 1 }, { vocabId: 2 }]
      };
      const parsed = parsePageResponse(mockResp);
      assert.strictEqual(parsed.page, 2);
      assert.strictEqual(parsed.size, 20);
      assert.strictEqual(parsed.totalElements, 55);
      assert.strictEqual(parsed.totalPages, 3);
      assert.strictEqual(parsed.items.length, 2);
    });

    test('calculates correct boundaries and windowing', () => {
      const calc = calculatePagination(100, 2, 20, 5);
      assert.strictEqual(calc.pageIndex, 2);
      assert.strictEqual(calc.pageSize, 20);
      assert.strictEqual(calc.totalPages, 5);
      assert.strictEqual(calc.startIndex, 40);
      assert.strictEqual(calc.endIndex, 60);
      assert.strictEqual(calc.startPage, 0);
      assert.strictEqual(calc.endPage, 4);
    });
  });

});
