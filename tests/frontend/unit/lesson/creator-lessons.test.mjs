/**
 * =============================================================================
 * UNIT TEST SUITE: CREATOR LESSON STUDIO & VOCABULARY ORDERING (TASK 9D.1)
 * File: tests/frontend/unit/lesson/creator-lessons.test.mjs
 * 
 * Verifies:
 * - Lesson title validation (Create & Update): empty, whitespace, >200 chars.
 * - PageResponse contract parsing & pagination boundary calculations.
 * - Lesson ID validation: positive integer contract, injection defense.
 * - Editable state rules: Draft/Rejected=true vs Pending/Approved=false.
 * - Reorder payload builder: strictly { orderedVocabIds: [...] } with zero leaks.
 * - Array swap logic: Move Up / Down with boundary protection.
 * - Duplicate vocabulary detection.
 * - Authoritative lesson detail parsing & orderIndex preservation.
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  validateCreateLessonTitle,
  parseLessonSummaries,
  calculatePagination,
  getStatusBadgeClass,
  getStatusLabel,
  formatLessonDate
} from '../../../../frontend/js/pages/creator-lessons-page.js';
import {
  validateLessonId,
  isLessonEditable,
  validateLessonTitle,
  buildReorderPayload,
  moveVocabItem,
  isVocabInLesson,
  parseLessonDetail
} from '../../../../frontend/js/pages/creator-lesson-editor-page.js';

describe('Unit: Creator Lesson Studio & Vocabulary Ordering (Task 9D.1)', () => {

  describe('1. Lesson Title Validation (Create & Update)', () => {
    test('rejects empty, null, or undefined titles', () => {
      const rNull = validateCreateLessonTitle(null);
      const rUndef = validateCreateLessonTitle(undefined);
      const rEmpty = validateCreateLessonTitle('');

      assert.strictEqual(rNull.valid, false);
      assert.strictEqual(rUndef.valid, false);
      assert.strictEqual(rEmpty.valid, false);
      assert.match(rEmpty.error, /không được để trống/);
    });

    test('rejects whitespace-only titles', () => {
      const rSpaces = validateCreateLessonTitle('     ');
      const rTabs = validateCreateLessonTitle('\t  \n  ');

      assert.strictEqual(rSpaces.valid, false);
      assert.strictEqual(rTabs.valid, false);
      assert.match(rSpaces.error, /không được để trống/);
    });

    test('rejects titles exceeding 200 characters', () => {
      const longTitle = 'A'.repeat(201);
      const result = validateCreateLessonTitle(longTitle);

      assert.strictEqual(result.valid, false);
      assert.match(result.error, /không được vượt quá 200 ký tự/);
    });

    test('accepts valid titles and trims leading/trailing whitespace', () => {
      const validTitle = '   HSK 1 — Bài 1: Chào hỏi   ';
      const result = validateCreateLessonTitle(validTitle);

      assert.strictEqual(result.valid, true);
      assert.strictEqual(result.sanitizedTitle, 'HSK 1 — Bài 1: Chào hỏi');
    });

    test('validateLessonTitle enforces same rules for metadata updates', () => {
      assert.strictEqual(validateLessonTitle('').valid, false);
      assert.strictEqual(validateLessonTitle('A'.repeat(201)).valid, false);
      assert.strictEqual(validateLessonTitle('Bài mới').valid, true);
      assert.strictEqual(validateLessonTitle('  Bài mới  ').sanitizedTitle, 'Bài mới');
    });
  });

  describe('2. Backend PageResponse Contract & Parsing', () => {
    test('parses wrapped ApiResponse<PageResponse<LessonSummaryResponse>>', () => {
      const envelope = {
        code: 200,
        message: 'Success',
        data: {
          items: [
            { lessonId: 1, title: 'Bài 1', status: 'Draft', vocabularyCount: 5, createdAt: '2026-09-01T10:00:00Z', updatedAt: '2026-09-01T12:00:00Z' },
            { lessonId: 2, title: 'Bài 2', status: 'Pending', vocabularyCount: 8, createdAt: '2026-09-02T10:00:00Z', updatedAt: '2026-09-02T12:00:00Z' }
          ],
          page: 0,
          size: 20,
          totalElements: 2,
          totalPages: 1
        }
      };

      const parsed = parseLessonSummaries(envelope);
      assert.strictEqual(parsed.items.length, 2);
      assert.strictEqual(parsed.page, 0);
      assert.strictEqual(parsed.size, 20);
      assert.strictEqual(parsed.totalElements, 2);
      assert.strictEqual(parsed.totalPages, 1);
      assert.strictEqual(parsed.items[0].lessonId, 1);
      assert.strictEqual(parsed.items[1].status, 'Pending');
    });

    test('handles null, undefined, or empty envelope gracefully', () => {
      const pNull = parseLessonSummaries(null);
      const pEmpty = parseLessonSummaries({});

      assert.deepStrictEqual(pNull.items, []);
      assert.strictEqual(pNull.totalElements, 0);
      assert.deepStrictEqual(pEmpty.items, []);
      assert.strictEqual(pEmpty.totalPages, 0);
    });
  });

  describe('3. Pagination Boundary Math', () => {
    test('calculates correct startRecord, endRecord, hasPrev, and hasNext', () => {
      const calc = calculatePagination(0, 20, 45, 3);
      assert.strictEqual(calc.currentPage, 0);
      assert.strictEqual(calc.hasPrev, false);
      assert.strictEqual(calc.hasNext, true);
      assert.strictEqual(calc.startRecord, 1);
      assert.strictEqual(calc.endRecord, 20);

      const page2 = calculatePagination(2, 20, 45, 3);
      assert.strictEqual(page2.currentPage, 2);
      assert.strictEqual(page2.hasPrev, true);
      assert.strictEqual(page2.hasNext, false);
      assert.strictEqual(page2.startRecord, 41);
      assert.strictEqual(page2.endRecord, 45);
    });

    test('handles empty dataset (0 totalElements)', () => {
      const calc = calculatePagination(0, 20, 0, 0);
      assert.strictEqual(calc.hasPrev, false);
      assert.strictEqual(calc.hasNext, false);
      assert.strictEqual(calc.startRecord, 0);
      assert.strictEqual(calc.endRecord, 0);
    });
  });

  describe('4. Lesson ID Validation', () => {
    test('accepts valid positive integers (numbers and strings)', () => {
      assert.strictEqual(validateLessonId(1), 1);
      assert.strictEqual(validateLessonId(100), 100);
      assert.strictEqual(validateLessonId('42'), 42);
      assert.strictEqual(validateLessonId('  999  '), 999);
    });

    test('rejects negative numbers, zero, floats, and non-numeric inputs', () => {
      assert.strictEqual(validateLessonId(0), null);
      assert.strictEqual(validateLessonId(-5), null);
      assert.strictEqual(validateLessonId(3.14), null);
      assert.strictEqual(validateLessonId('abc'), null);
      assert.strictEqual(validateLessonId(''), null);
      assert.strictEqual(validateLessonId(null), null);
      assert.strictEqual(validateLessonId(undefined), null);
    });

    test('defends against SQLi and XSS injection vectors in ID parameter', () => {
      assert.strictEqual(validateLessonId("1' OR '1'='1"), null);
      assert.strictEqual(validateLessonId('<script>alert(1)</script>'), null);
      assert.strictEqual(validateLessonId('1; DROP TABLE lesson;'), null);
    });
  });

  describe('5. Editable State Lifecycle Rules', () => {
    test('strictly permits mutations for Draft and Rejected states', () => {
      assert.strictEqual(isLessonEditable('Draft'), true);
      assert.strictEqual(isLessonEditable('Rejected'), true);
      assert.strictEqual(isLessonEditable('draft'), true);
      assert.strictEqual(isLessonEditable('REJECTED'), true);
    });

    test('strictly forbids mutations for Pending and Approved states', () => {
      assert.strictEqual(isLessonEditable('Pending'), false);
      assert.strictEqual(isLessonEditable('Approved'), false);
      assert.strictEqual(isLessonEditable('pending'), false);
      assert.strictEqual(isLessonEditable('APPROVED'), false);
    });

    test('forbids mutations for missing, null, or unrecognized status', () => {
      assert.strictEqual(isLessonEditable(null), false);
      assert.strictEqual(isLessonEditable(undefined), false);
      assert.strictEqual(isLessonEditable(''), false);
      assert.strictEqual(isLessonEditable('Archived'), false);
    });
  });

  describe('6. Reorder Payload Builder & Membership Contract', () => {
    test('builds strictly { orderedVocabIds: [id1, id2, ...] }', () => {
      const vocabList = [
        { vocabId: 101, hanzi: '一', orderIndex: 1 },
        { vocabId: 202, hanzi: '二', orderIndex: 2 },
        { vocabId: 303, hanzi: '三', orderIndex: 3 }
      ];

      const payload = buildReorderPayload(vocabList);
      assert.deepStrictEqual(payload, {
        orderedVocabIds: [101, 202, 303]
      });
    });

    test('enforces zero identity/orderIndex leak in reorder payload', () => {
      const vocabList = [
        { vocabId: 101, userId: 99, accountId: 88, createdBy: 'hacker', orderIndex: 999 }
      ];

      const payload = buildReorderPayload(vocabList);
      assert.strictEqual('orderedVocabIds' in payload, true);
      assert.strictEqual('userId' in payload, false);
      assert.strictEqual('accountId' in payload, false);
      assert.strictEqual('createdBy' in payload, false);
      assert.strictEqual('orderIndex' in payload, false);
      assert.deepStrictEqual(payload.orderedVocabIds, [101]);
    });

    test('handles empty or malformed vocabulary list gracefully', () => {
      assert.deepStrictEqual(buildReorderPayload([]), { orderedVocabIds: [] });
      assert.deepStrictEqual(buildReorderPayload(null), { orderedVocabIds: [] });
    });
  });

  describe('7. Pure Array Swap (moveVocabItem)', () => {
    const list = [
      { vocabId: 10, hanzi: 'A' },
      { vocabId: 20, hanzi: 'B' },
      { vocabId: 30, hanzi: 'C' }
    ];

    test('swaps adjacent items correctly (Move Up: 1 -> 0)', () => {
      const moved = moveVocabItem(list, 1, 0);
      assert.strictEqual(moved[0].vocabId, 20);
      assert.strictEqual(moved[1].vocabId, 10);
      assert.strictEqual(moved[2].vocabId, 30);
      // Original array must NOT be mutated
      assert.strictEqual(list[0].vocabId, 10);
    });

    test('swaps adjacent items correctly (Move Down: 1 -> 2)', () => {
      const moved = moveVocabItem(list, 1, 2);
      assert.strictEqual(moved[0].vocabId, 10);
      assert.strictEqual(moved[1].vocabId, 30);
      assert.strictEqual(moved[2].vocabId, 20);
    });

    test('clamps boundary moves safely (out of bounds returns unchanged copy)', () => {
      const moveUpFromZero = moveVocabItem(list, 0, -1);
      assert.deepStrictEqual(moveUpFromZero, list);

      const moveDownFromLast = moveVocabItem(list, 2, 3);
      assert.deepStrictEqual(moveDownFromLast, list);

      const sameIndex = moveVocabItem(list, 1, 1);
      assert.deepStrictEqual(sameIndex, list);
    });
  });

  describe('8. Duplicate Vocabulary Detection (isVocabInLesson)', () => {
    const vocabs = [
      { vocabId: 101, hanzi: '学' },
      { vocabId: 202, hanzi: '习' }
    ];

    test('returns true when vocabulary ID is already in the lesson', () => {
      assert.strictEqual(isVocabInLesson(vocabs, 101), true);
      assert.strictEqual(isVocabInLesson(vocabs, '202'), true);
    });

    test('returns false when vocabulary ID is not in the lesson', () => {
      assert.strictEqual(isVocabInLesson(vocabs, 303), false);
      assert.strictEqual(isVocabInLesson(vocabs, null), false);
      assert.strictEqual(isVocabInLesson([], 101), false);
    });
  });

  describe('9. Authoritative Detail Parsing & Order Preservation', () => {
    test('sorts constituent vocabularies deterministically by orderIndex ascending', () => {
      const envelope = {
        data: {
          lessonId: 10,
          title: 'Bài 1: Chào hỏi',
          status: 'Draft',
          vocabularyCount: 3,
          vocabularies: [
            { vocabId: 30, hanzi: '三', orderIndex: 3 },
            { vocabId: 10, hanzi: '一', orderIndex: 1 },
            { vocabId: 20, hanzi: '二', orderIndex: 2 }
          ]
        }
      };

      const detail = parseLessonDetail(envelope);
      assert.strictEqual(detail.lessonId, 10);
      assert.strictEqual(detail.title, 'Bài 1: Chào hỏi');
      assert.strictEqual(detail.vocabularies.length, 3);
      assert.strictEqual(detail.vocabularies[0].vocabId, 10);
      assert.strictEqual(detail.vocabularies[1].vocabId, 20);
      assert.strictEqual(detail.vocabularies[2].vocabId, 30);
    });
  });

  describe('10. Status Badges & Labels Formatting', () => {
    test('maps status to correct CSS classes and Vietnamese labels', () => {
      assert.strictEqual(getStatusBadgeClass('Draft'), 'badge-status badge-status-draft');
      assert.strictEqual(getStatusBadgeClass('Pending'), 'badge-status badge-status-pending');
      assert.strictEqual(getStatusBadgeClass('Approved'), 'badge-status badge-status-approved');
      assert.strictEqual(getStatusBadgeClass('Rejected'), 'badge-status badge-status-rejected');

      assert.strictEqual(getStatusLabel('Draft'), 'Bản nháp');
      assert.strictEqual(getStatusLabel('Pending'), 'Chờ duyệt');
      assert.strictEqual(getStatusLabel('Approved'), 'Đã duyệt');
      assert.strictEqual(getStatusLabel('Rejected'), 'Từ chối');
    });

    test('formats ISO dates correctly into DD/MM/YYYY HH:mm', () => {
      const formatted = formatLessonDate('2026-09-14T15:30:00Z');
      assert.match(formatted, /\d{2}\/\d{2}\/2026 \d{2}:\d{2}/);
      assert.strictEqual(formatLessonDate(null), '--');
      assert.strictEqual(formatLessonDate(''), '--');
      assert.strictEqual(formatLessonDate('invalid-date'), '--');
    });
  });
});
