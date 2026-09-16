/**
 * =============================================================================
 * UNIT TEST SUITE: MODERATOR LESSON REVIEW & VOCABULARY INSPECTION (TASK 9E.2)
 * File: tests/frontend/unit/moderator/moderator-review.test.mjs
 * 
 * Verifies:
 * - 1. Route ID extraction & positive integer validation.
 * - 2. LessonDetailResponse DTO normalization & envelope unwrapping.
 * - 3. Strict preservation of backend vocabulary orderIndex sequence.
 * - 4. Null-safe linguistic fallbacks (Hanzi, Pinyin, meanings, audioUrl, examples).
 * - 5. Date formatting semantics.
 * - 6. VocabularyDetailResponse constituent radicals normalization.
 * - 7. Stale queue 404 recovery message wording.
 * - 8. Role guard access control rules.
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  validateReviewLessonId,
  extractLessonIdFromSearch,
  hasModeratorAccess,
  formatReviewDate,
  parseLessonReviewDetail,
  normalizeRadicalList,
  getStaleQueueErrorMessage,
  MAX_MODERATION_TEXT_LENGTH,
  validateRejectionReason,
  validateApprovalNote,
  serializeApprovePayload,
  serializeRejectPayload,
  mapModerationError,
  ModeratorReviewController
} from '../../../../frontend/js/pages/moderator-review-page.js';

describe('Unit: Moderator Lesson Review & Moderation Workflow UI (Task 9E.2 & 9E.3)', () => {

  describe('1. Route ID Extraction & Positive Integer Validation', () => {
    test('validates positive integers and rejects invalid inputs', () => {
      assert.strictEqual(validateReviewLessonId(1), 1);
      assert.strictEqual(validateReviewLessonId(105), 105);
      assert.strictEqual(validateReviewLessonId('105'), 105);
      assert.strictEqual(validateReviewLessonId(' 999 '), 999);

      assert.strictEqual(validateReviewLessonId(0), null);
      assert.strictEqual(validateReviewLessonId(-5), null);
      assert.strictEqual(validateReviewLessonId('-10'), null);
      assert.strictEqual(validateReviewLessonId('abc'), null);
      assert.strictEqual(validateReviewLessonId('12.34'), null);
      assert.strictEqual(validateReviewLessonId(null), null);
      assert.strictEqual(validateReviewLessonId(undefined), null);
      assert.strictEqual(validateReviewLessonId(''), null);
    });

    test('extracts lessonId from search string or URLSearchParams', () => {
      assert.strictEqual(extractLessonIdFromSearch('?id=42'), 42);
      assert.strictEqual(extractLessonIdFromSearch('?foo=bar&id=108&page=1'), 108);
      assert.strictEqual(extractLessonIdFromSearch(new URLSearchParams('id=256')), 256);

      assert.strictEqual(extractLessonIdFromSearch('?id=invalid'), null);
      assert.strictEqual(extractLessonIdFromSearch('?other=123'), null);
      assert.strictEqual(extractLessonIdFromSearch(''), null);
      assert.strictEqual(extractLessonIdFromSearch(null), null);
    });
  });

  describe('2. LessonDetailResponse DTO Normalization', () => {
    test('unwraps wrapped ApiResponse<LessonDetailResponse>', () => {
      const envelope = {
        code: 'SUCCESS',
        message: 'Thao tác thành công',
        data: {
          lessonId: 100,
          title: 'HSK 1 - Bài 1: Chào hỏi căn bản',
          status: 'Pending',
          vocabularyCount: 2,
          createdAt: '2026-09-01T08:00:00Z',
          updatedAt: '2026-09-02T10:00:00Z',
          vocabularies: [
            {
              vocabId: 10,
              hanzi: '你',
              pinyin: 'nǐ',
              pinyinRaw: 'ni3',
              meaningHanViet: 'Nhĩ',
              meaningVi: 'Bạn, anh, chị (ngôi thứ 2)',
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
              meaningVi: 'Tốt, đẹp, khỏe',
              audioUrl: null,
              videoWritingUrl: null,
              exampleSentence: null,
              exampleTranslation: null,
              orderIndex: 2
            }
          ]
        }
      };

      const result = parseLessonReviewDetail(envelope);
      assert.strictEqual(result.lessonId, 100);
      assert.strictEqual(result.title, 'HSK 1 - Bài 1: Chào hỏi căn bản');
      assert.strictEqual(result.status, 'Pending');
      assert.strictEqual(result.vocabularyCount, 2);
      assert.strictEqual(result.vocabularies.length, 2);

      // Verify item 1
      assert.strictEqual(result.vocabularies[0].vocabId, 10);
      assert.strictEqual(result.vocabularies[0].hanzi, '你');
      assert.strictEqual(result.vocabularies[0].pinyin, 'nǐ');
      assert.strictEqual(result.vocabularies[0].pinyinRaw, 'ni3');
      assert.strictEqual(result.vocabularies[0].audioUrl, 'https://example.com/audio/ni.mp3');
      assert.strictEqual(result.vocabularies[0].orderIndex, 1);

      // Verify item 2 null fallbacks
      assert.strictEqual(result.vocabularies[1].vocabId, 11);
      assert.strictEqual(result.vocabularies[1].hanzi, '好');
      assert.strictEqual(result.vocabularies[1].audioUrl, null);
      assert.strictEqual(result.vocabularies[1].exampleSentence, null);
      assert.strictEqual(result.vocabularies[1].orderIndex, 2);
    });

    test('throws descriptive error on null or empty payload', () => {
      assert.throws(() => parseLessonReviewDetail(null), /Dữ liệu bài học trống/);
      assert.throws(() => parseLessonReviewDetail(undefined), /Dữ liệu bài học trống/);
    });
  });

  describe('3. Authoritative Vocabulary Ordering Preservation', () => {
    test('strictly preserves backend array order without mutating orderIndex', () => {
      const data = {
        lessonId: 200,
        title: 'Thứ tự từ vựng bài học',
        status: 'Pending',
        vocabularies: [
          { vocabId: 50, hanzi: '三', pinyin: 'sān', orderIndex: 1 },
          { vocabId: 40, hanzi: '一', pinyin: 'yī', orderIndex: 2 },
          { vocabId: 60, hanzi: '二', pinyin: 'èr', orderIndex: 3 }
        ]
      };

      const result = parseLessonReviewDetail(data);

      // Verify exact sequence is preserved (三, 一, 二)
      assert.strictEqual(result.vocabularies[0].hanzi, '三');
      assert.strictEqual(result.vocabularies[0].orderIndex, 1);

      assert.strictEqual(result.vocabularies[1].hanzi, '一');
      assert.strictEqual(result.vocabularies[1].orderIndex, 2);

      assert.strictEqual(result.vocabularies[2].hanzi, '二');
      assert.strictEqual(result.vocabularies[2].orderIndex, 3);
    });
  });

  describe('4. Null-Safe Linguistic Fallbacks', () => {
    test('handles missing optional fields without crashing', () => {
      const data = {
        lessonId: 300,
        title: '',
        vocabularies: [
          {
            vocabId: null,
            hanzi: '',
            pinyin: null,
            meaningVi: ''
          }
        ]
      };

      const result = parseLessonReviewDetail(data);
      assert.strictEqual(result.title, 'Bài học không tên');
      assert.strictEqual(result.vocabularies[0].hanzi, '?');
      assert.strictEqual(result.vocabularies[0].pinyin, '');
      assert.strictEqual(result.vocabularies[0].meaningVi, 'Chưa có nghĩa tiếng Việt');
      assert.strictEqual(result.vocabularies[0].audioUrl, null);
    });
  });

  describe('5. Date Formatting Semantics', () => {
    test('formats ISO dates accurately or returns fallback', () => {
      const formatted = formatReviewDate('2026-09-01T08:30:00Z');
      assert.match(formatted, /\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}/);

      assert.strictEqual(formatReviewDate(null), '--');
      assert.strictEqual(formatReviewDate(''), '--');
      assert.strictEqual(formatReviewDate('invalid-date'), '--');
    });
  });

  describe('6. Radical List Normalization', () => {
    test('normalizes constituent radicals from VocabularyDetailResponse', () => {
      const response = {
        code: 'SUCCESS',
        data: {
          vocabId: 10,
          hanzi: '你',
          radicals: [
            {
              radicalId: 9,
              character: '人',
              pinyin: 'rén',
              meaningHanViet: 'Nhân',
              meaningVi: 'Người'
            },
            {
              radicalId: 57,
              character: '弓',
              pinyin: 'gōng',
              meaningHanViet: 'Cung',
              meaningVi: 'Cây cung'
            }
          ]
        }
      };

      const radicals = normalizeRadicalList(response);
      assert.strictEqual(radicals.length, 2);
      assert.strictEqual(radicals[0].character, '人');
      assert.strictEqual(radicals[0].pinyin, 'rén');
      assert.strictEqual(radicals[0].meaningHanViet, 'Nhân');

      assert.strictEqual(radicals[1].character, '弓');
      assert.strictEqual(radicals[1].meaningVi, 'Cây cung');
    });

    test('handles empty or missing radicals gracefully', () => {
      assert.deepStrictEqual(normalizeRadicalList(null), []);
      assert.deepStrictEqual(normalizeRadicalList({ data: { radicals: [] } }), []);
      assert.deepStrictEqual(normalizeRadicalList({ data: {} }), []);
    });
  });

  describe('7. Stale Queue Concurrency Wording', () => {
    test('returns exact required wording for stale or 404 lessons', () => {
      assert.strictEqual(
        getStaleQueueErrorMessage(),
        'Bài học không còn trong hàng đợi kiểm duyệt hoặc không tồn tại.'
      );
    });
  });

  describe('8. Role Guard Access Rules', () => {
    test('allows Moderator and Admin, rejects Learner and Creator', () => {
      const mockAuth = (role) => ({
        hasRole: (r) => r.toLowerCase() === role.toLowerCase()
      });

      assert.strictEqual(hasModeratorAccess(mockAuth('Moderator')), true);
      assert.strictEqual(hasModeratorAccess(mockAuth('Admin')), true);
      assert.strictEqual(hasModeratorAccess(mockAuth('Learner')), false);
      assert.strictEqual(hasModeratorAccess(mockAuth('Creator')), false);
      assert.strictEqual(hasModeratorAccess(null), false);
      assert.strictEqual(hasModeratorAccess({}), false);
    });
  });

  describe('9. Rejection Reason Validation (Task 9E.3)', () => {
    test('rejects blank, null, undefined, or empty reason', () => {
      assert.strictEqual(validateRejectionReason(null).valid, false);
      assert.strictEqual(validateRejectionReason(undefined).valid, false);
      assert.strictEqual(validateRejectionReason('').valid, false);
      assert.strictEqual(validateRejectionReason('   ').valid, false);
      assert.strictEqual(validateRejectionReason('\t\n  ').valid, false);
      assert.strictEqual(validateRejectionReason(123).valid, false);
    });

    test('accepts valid non-empty trimmed reason within 500 characters', () => {
      const res = validateRejectionReason('  Phát âm từ số 2 chưa chuẩn, vui lòng ghi âm lại.  ');
      assert.strictEqual(res.valid, true);
      assert.strictEqual(res.error, null);
      assert.strictEqual(res.reason, 'Phát âm từ số 2 chưa chuẩn, vui lòng ghi âm lại.');
    });

    test('enforces exact 500-character limit boundary', () => {
      const exact500 = 'A'.repeat(500);
      assert.strictEqual(validateRejectionReason(exact500).valid, true);

      const over500 = 'A'.repeat(501);
      const res = validateRejectionReason(over500);
      assert.strictEqual(res.valid, false);
      assert.ok(res.error.includes('500 ký tự'));
    });

    test('preserves Vietnamese diacritics and CJK characters accurately', () => {
      const cjk = 'Tiêu đề: 汉语教程, Từ vựng: 你好, 再见 chưa có câu ví dụ.';
      const res = validateRejectionReason(cjk);
      assert.strictEqual(res.valid, true);
      assert.strictEqual(res.reason, cjk);
    });
  });

  describe('10. Approval Note Validation (Task 9E.3)', () => {
    test('permits null, undefined, or empty approval note as optional', () => {
      assert.strictEqual(validateApprovalNote(null).valid, true);
      assert.strictEqual(validateApprovalNote(null).note, null);
      assert.strictEqual(validateApprovalNote(undefined).valid, true);
      assert.strictEqual(validateApprovalNote('').valid, true);
      assert.strictEqual(validateApprovalNote('   ').valid, true);
      assert.strictEqual(validateApprovalNote('   ').note, null);
    });

    test('accepts trimmed note within 500 characters', () => {
      const res = validateApprovalNote('  Nội dung đạt chuẩn HSK 1, duyệt xuất bản.  ');
      assert.strictEqual(res.valid, true);
      assert.strictEqual(res.note, 'Nội dung đạt chuẩn HSK 1, duyệt xuất bản.');
    });

    test('rejects approval note exceeding 500 characters', () => {
      const over500 = 'B'.repeat(501);
      const res = validateApprovalNote(over500);
      assert.strictEqual(res.valid, false);
      assert.ok(res.error.includes('500 ký tự'));
    });
  });

  describe('11. Approve Payload Serialization (Task 9E.3)', () => {
    test('serializes note correctly when present or absent', () => {
      assert.deepStrictEqual(serializeApprovePayload('  Bài học xuất sắc  '), {
        note: 'Bài học xuất sắc'
      });
      assert.deepStrictEqual(serializeApprovePayload(''), { note: null });
      assert.deepStrictEqual(serializeApprovePayload('   '), { note: null });
      assert.deepStrictEqual(serializeApprovePayload(null), { note: null });
      assert.deepStrictEqual(serializeApprovePayload(undefined), { note: null });
    });
  });

  describe('12. Reject Payload Serialization (Task 9E.3)', () => {
    test('serializes trimmed reason and JSON-string array flagged fields', () => {
      const payload = serializeRejectPayload('  Lỗi phát âm và phiên âm  ', ['title', 'pinyin']);
      assert.strictEqual(payload.rejectionReason, 'Lỗi phát âm và phiên âm');
      assert.strictEqual(payload.flaggedFields, '["title","pinyin"]');
    });

    test('serializes flaggedFields as null when array is empty or omitted', () => {
      assert.deepStrictEqual(serializeRejectPayload('Lý do chung', []), {
        rejectionReason: 'Lý do chung',
        flaggedFields: null
      });
      assert.deepStrictEqual(serializeRejectPayload('Lý do chung', null), {
        rejectionReason: 'Lý do chung',
        flaggedFields: null
      });
      assert.deepStrictEqual(serializeRejectPayload('Lý do chung', undefined), {
        rejectionReason: 'Lý do chung',
        flaggedFields: null
      });
    });

    test('preserves existing JSON string if passed as string directly', () => {
      const payload = serializeRejectPayload('Lý do', '["vocabularies[0].pinyin"]');
      assert.strictEqual(payload.flaggedFields, '["vocabularies[0].pinyin"]');
    });
  });

  describe('13. Concurrency (409 Conflict) & Error Mapping (Task 9E.3)', () => {
    test('maps 409 status and conflict messages to truthful conflict notification', () => {
      const err409 = { status: 409, message: 'Bài học đã được phê duyệt trước đó' };
      const mapped = mapModerationError(err409);
      assert.strictEqual(mapped.type, 'conflict');
      assert.ok(mapped.title.includes('409'));
      assert.ok(mapped.message.includes('trước đó'));

      const staleErr = { message: 'Dữ liệu đã bị thay đổi bởi một phiên làm việc khác, vui lòng thử lại' };
      const mappedStale = mapModerationError(staleErr);
      assert.strictEqual(mappedStale.type, 'conflict');
    });

    test('maps 400 Bad Request to validation error description', () => {
      const err400 = { status: 400, message: 'Lý do từ chối không được để trống' };
      const mapped = mapModerationError(err400);
      assert.strictEqual(mapped.type, 'validation');
      assert.ok(mapped.title.includes('400'));
    });

    test('maps 401 Unauthorized and 403 Forbidden properly', () => {
      const mapped401 = mapModerationError({ status: 401 });
      assert.strictEqual(mapped401.type, 'unauthorized');

      const mapped403 = mapModerationError({ status: 403 });
      assert.strictEqual(mapped403.type, 'forbidden');
    });

    test('falls back to generic error message for unknown failures', () => {
      const generic = mapModerationError({ status: 500, message: 'Database failure' });
      assert.strictEqual(generic.type, 'error');
      assert.strictEqual(generic.message, 'Database failure');
    });
  });

  describe('14. Controller State Transition & Anti-Duplicate Protection (Task 9E.3)', () => {
    test('toggles isSubmitting flag and button states during submission lifecycle', () => {
      const controller = new ModeratorReviewController();
      const mockBtn = { disabled: false };
      const mockText = { textContent: 'Xác nhận' };
      controller.dom = {
        btnConfirmApprove: mockBtn,
        btnConfirmApproveText: mockText,
        btnCancelApprove: { disabled: false },
        approveNoteInput: { disabled: false }
      };

      assert.strictEqual(controller.isSubmitting, false);

      controller.setSubmitting(true, 'approve');
      assert.strictEqual(controller.isSubmitting, true);
      assert.strictEqual(mockBtn.disabled, true);
      assert.strictEqual(mockText.textContent, 'Đang phê duyệt...');

      controller.setSubmitting(false, 'approve');
      assert.strictEqual(controller.isSubmitting, false);
      assert.strictEqual(mockBtn.disabled, false);
      assert.strictEqual(mockText.textContent, 'Xác nhận phê duyệt');
    });

    test('disableModerationActions hides action bars and disables buttons', () => {
      const controller = new ModeratorReviewController();
      const mockActionClass = {
        classes: [],
        classList: {
          add(c) { mockActionClass.classes.push(c); },
          remove(c) { mockActionClass.classes = mockActionClass.classes.filter(x => x !== c); }
        }
      };
      controller.dom = {
        moderationActionBar: mockActionClass,
        reviewBottomActionBar: mockActionClass,
        btnOpenRejectModal: { disabled: false },
        btnOpenApproveModal: { disabled: false },
        btnBottomRejectModal: { disabled: false },
        btnBottomApproveModal: { disabled: false }
      };

      controller.disableModerationActions();
      assert.ok(mockActionClass.classes.includes('d-none'));
      assert.strictEqual(controller.dom.btnOpenRejectModal.disabled, true);
      assert.strictEqual(controller.dom.btnOpenApproveModal.disabled, true);
    });
  });

});
