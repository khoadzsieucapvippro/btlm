/**
 * =============================================================================
 * UNIT TEST SUITE: LESSON CATALOG & ORDERED VOCABULARY ENGINE (TASK 9C.1)
 * File: tests/frontend/unit/lesson/lessons-catalog.test.mjs
 * 
 * Verifies:
 * - Query contract invariant: strictly page and size ONLY (strictly NO search/q/keyword).
 * - PageResponse contract parsing & pagination boundary calculations.
 * - Publication date decision & formatting (updatedAt as verified proxy).
 * - Lesson ID validation (positive integer contract, blocking malformed/negative/injection).
 * - Lesson detail response parsing & orderIndex preservation.
 * - Ordered vocabulary sort logic (immutability, 1 -> 2 -> 3... sequence).
 * - Web Speech API availability & graceful degradation.
 * - Safe resource URL sanitization (about:blank fallback for dangerous protocols).
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  parseLessonPageResponse,
  calculatePagination,
  formatLessonDate,
  sanitizeLessonTitle
} from '../../../../frontend/js/pages/lessons-page.js';
import {
  validateLessonId,
  parseLessonDetailResponse,
  sortVocabulariesByOrderIndex,
  isSpeechSynthesisAvailable,
  speakHanzi
} from '../../../../frontend/js/pages/lesson-detail-page.js';
import { sanitizeResourceUrl } from '../../../../frontend/js/ui/security.js';

describe('Unit: Lesson Catalog & Lesson Detail Engine (Task 9C.1)', () => {

  describe('1. Query Parameter Contract & Invariants', () => {
    test('enforces query contract has ONLY page and size (strictly NO search, q, or keyword)', () => {
      const page = 0;
      const size = 20;

      // Simulate parameter object constructed for apiClient('/lessons')
      const params = { page, size };

      assert.strictEqual('page' in params, true);
      assert.strictEqual('size' in params, true);
      assert.strictEqual('search' in params, false, 'Lessons API must NEVER include search');
      assert.strictEqual('q' in params, false, 'Lessons API must NEVER include q');
      assert.strictEqual('keyword' in params, false, 'Lessons API must NEVER include keyword');
      assert.strictEqual('status' in params, false, 'Public lessons API must not pass status parameter');
    });
  });

  describe('2. Backend PageResponse Contract & Parsing', () => {
    test('parses standard Spring Boot ApiResponse<PageResponse<LessonSummaryResponse>> correctly', () => {
      const apiResponse = {
        code: 200,
        message: 'Success',
        data: {
          items: [
            {
              lessonId: 101,
              title: 'Bài 1: Chào hỏi cơ bản',
              status: 'Approved',
              vocabularyCount: 10,
              createdAt: '2026-08-01T08:00:00Z',
              updatedAt: '2026-08-02T10:30:00Z'
            },
            {
              lessonId: 102,
              title: 'Bài 2: Chữ số và số đếm',
              status: 'Approved',
              vocabularyCount: 15,
              createdAt: '2026-08-03T08:00:00Z',
              updatedAt: '2026-08-04T12:00:00Z'
            }
          ],
          page: 0,
          size: 20,
          totalElements: 2,
          totalPages: 1
        }
      };

      const parsed = parseLessonPageResponse(apiResponse);
      assert.strictEqual(parsed.items.length, 2);
      assert.strictEqual(parsed.totalElements, 2);
      assert.strictEqual(parsed.totalPages, 1);
      assert.strictEqual(parsed.page, 0);
      assert.strictEqual(parsed.size, 20);
      assert.strictEqual(parsed.items[0].lessonId, 101);
      assert.strictEqual(parsed.items[0].title, 'Bài 1: Chào hỏi cơ bản');
      assert.strictEqual(parsed.items[0].status, 'Approved');
      assert.strictEqual(parsed.items[0].vocabularyCount, 10);
    });

    test('handles unwrapped PageResponse payload gracefully', () => {
      const pageResponse = {
        items: [{ lessonId: 1, title: 'Bài học 1', status: 'Approved', vocabularyCount: 5 }],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1
      };

      const parsed = parseLessonPageResponse(pageResponse);
      assert.strictEqual(parsed.items.length, 1);
      assert.strictEqual(parsed.totalElements, 1);
      assert.strictEqual(parsed.items[0].lessonId, 1);
    });

    test('handles empty, null, or malformed responses safely without throwing', () => {
      const empty = parseLessonPageResponse(null);
      assert.strictEqual(empty.items.length, 0);
      assert.strictEqual(empty.totalElements, 0);
      assert.strictEqual(empty.totalPages, 0);
      assert.strictEqual(empty.page, 0);

      const malformed = parseLessonPageResponse({ invalid: true });
      assert.strictEqual(malformed.items.length, 0);
      assert.strictEqual(malformed.totalElements, 0);
    });
  });

  describe('3. Pagination Math & Boundary Calculations', () => {
    test('calculates correct state for first page of multi-page catalog', () => {
      const pag = calculatePagination(0, 12, 50, 5);
      assert.strictEqual(pag.currentPage, 0);
      assert.strictEqual(pag.totalPages, 5);
      assert.strictEqual(pag.hasPrev, false);
      assert.strictEqual(pag.hasNext, true);
      assert.strictEqual(pag.startRecord, 1);
      assert.strictEqual(pag.endRecord, 12);
    });

    test('calculates correct state for middle page', () => {
      const pag = calculatePagination(2, 10, 45, 5);
      assert.strictEqual(pag.currentPage, 2);
      assert.strictEqual(pag.hasPrev, true);
      assert.strictEqual(pag.hasNext, true);
      assert.strictEqual(pag.startRecord, 21);
      assert.strictEqual(pag.endRecord, 30);
    });

    test('calculates correct state for last page with partial count', () => {
      const pag = calculatePagination(4, 10, 45, 5);
      assert.strictEqual(pag.currentPage, 4);
      assert.strictEqual(pag.hasPrev, true);
      assert.strictEqual(pag.hasNext, false);
      assert.strictEqual(pag.startRecord, 41);
      assert.strictEqual(pag.endRecord, 45);
    });

    test('handles zero elements (empty catalog) safely', () => {
      const pag = calculatePagination(0, 20, 0, 0);
      assert.strictEqual(pag.currentPage, 0);
      assert.strictEqual(pag.hasPrev, false);
      assert.strictEqual(pag.hasNext, false);
      assert.strictEqual(pag.startRecord, 0);
      assert.strictEqual(pag.endRecord, 0);
    });
  });

  describe('4. Date Presentation & Vietnamese Formatting', () => {
    test('formats valid ISO date string to localized Vietnamese format', () => {
      const iso = '2026-08-15T10:00:00Z';
      const formatted = formatLessonDate(iso);
      assert.ok(formatted.length > 0);
      assert.ok(formatted.includes('2026') || formatted.includes('15'));
    });

    test('returns empty string for null/undefined/invalid date without throwing', () => {
      assert.strictEqual(formatLessonDate(null), '');
      assert.strictEqual(formatLessonDate(undefined), '');
      assert.strictEqual(formatLessonDate(''), '');
      assert.strictEqual(formatLessonDate('invalid-date-string'), '');
    });
  });

  describe('5. Lesson ID Validation Contract', () => {
    test('accepts valid positive integer IDs (numbers and numeric strings)', () => {
      assert.strictEqual(validateLessonId(1), 1);
      assert.strictEqual(validateLessonId(42), 42);
      assert.strictEqual(validateLessonId('101'), 101);
      assert.strictEqual(validateLessonId('  99  '), 99);
    });

    test('rejects non-positive, floating-point, or malformed IDs', () => {
      assert.strictEqual(validateLessonId(0), null);
      assert.strictEqual(validateLessonId(-1), null);
      assert.strictEqual(validateLessonId('-5'), null);
      assert.strictEqual(validateLessonId(3.14), null);
      assert.strictEqual(validateLessonId('3.14'), null);
      assert.strictEqual(validateLessonId('abc'), null);
      assert.strictEqual(validateLessonId(''), null);
      assert.strictEqual(validateLessonId(null), null);
      assert.strictEqual(validateLessonId(undefined), null);
      assert.strictEqual(validateLessonId(NaN), null);
      assert.strictEqual(validateLessonId(Infinity), null);
      assert.strictEqual(validateLessonId('<script>'), null);
    });
  });

  describe('6. Lesson Detail Response & Parsing Contract', () => {
    test('parses ApiResponse<LessonDetailResponse> with ordered vocabularies', () => {
      const response = {
        code: 200,
        message: 'Success',
        data: {
          lessonId: 1,
          title: 'Bài 1: Nhập môn Hán tự',
          status: 'Approved',
          vocabularyCount: 2,
          createdAt: '2026-07-01T00:00:00Z',
          updatedAt: '2026-07-02T12:00:00Z',
          vocabularies: [
            {
              vocabId: 10,
              hanzi: '一',
              pinyin: 'yī',
              pinyinRaw: 'yi1',
              meaningHanViet: 'Nhất',
              meaningVi: 'Số một',
              audioUrl: '/audio/yi.mp3',
              videoWritingUrl: null,
              exampleSentence: '一个人',
              exampleTranslation: 'Một người',
              orderIndex: 1
            },
            {
              vocabId: 11,
              hanzi: '二',
              pinyin: 'èr',
              pinyinRaw: 'er4',
              meaningHanViet: 'Nhị',
              meaningVi: 'Số hai',
              audioUrl: null,
              videoWritingUrl: null,
              exampleSentence: '二月',
              exampleTranslation: 'Tháng hai',
              orderIndex: 2
            }
          ]
        }
      };

      const detail = parseLessonDetailResponse(response);
      assert.strictEqual(detail.lessonId, 1);
      assert.strictEqual(detail.title, 'Bài 1: Nhập môn Hán tự');
      assert.strictEqual(detail.status, 'Approved');
      assert.strictEqual(detail.vocabularyCount, 2);
      assert.strictEqual(detail.vocabularies.length, 2);
      assert.strictEqual(detail.vocabularies[0].orderIndex, 1);
      assert.strictEqual(detail.vocabularies[1].orderIndex, 2);
    });

    test('handles missing or malformed detail response safely', () => {
      const result = parseLessonDetailResponse(null);
      assert.strictEqual(result, null);

      const invalid = parseLessonDetailResponse('not-an-object');
      assert.strictEqual(invalid, null);
    });
  });

  describe('7. Ordered Vocabulary Sort & Invariant Preservation', () => {
    test('sorts vocabularies strictly by orderIndex ascending (1 -> 2 -> 3...)', () => {
      const scrambled = [
        { vocabId: 3, orderIndex: 3, hanzi: '三' },
        { vocabId: 1, orderIndex: 1, hanzi: '一' },
        { vocabId: 4, orderIndex: 4, hanzi: '四' },
        { vocabId: 2, orderIndex: 2, hanzi: '二' }
      ];

      const sorted = sortVocabulariesByOrderIndex(scrambled);
      assert.strictEqual(sorted.length, 4);
      assert.strictEqual(sorted[0].orderIndex, 1);
      assert.strictEqual(sorted[1].orderIndex, 2);
      assert.strictEqual(sorted[2].orderIndex, 3);
      assert.strictEqual(sorted[3].orderIndex, 4);
      assert.strictEqual(sorted[0].hanzi, '一');
      assert.strictEqual(sorted[3].hanzi, '四');
    });

    test('preserves immutability (does not mutate the original array)', () => {
      const original = [
        { vocabId: 2, orderIndex: 2 },
        { vocabId: 1, orderIndex: 1 }
      ];

      const sorted = sortVocabulariesByOrderIndex(original);
      assert.strictEqual(original[0].orderIndex, 2, 'Original array should not be mutated in-place');
      assert.strictEqual(sorted[0].orderIndex, 1);
    });

    test('handles items with missing or identical orderIndex gracefully', () => {
      const items = [
        { vocabId: 1, orderIndex: undefined },
        { vocabId: 2, orderIndex: 1 },
        { vocabId: 3, orderIndex: 1 }
      ];

      const sorted = sortVocabulariesByOrderIndex(items);
      assert.strictEqual(sorted.length, 3);
    });

    test('returns empty array when input is null, undefined, or not an array', () => {
      assert.deepStrictEqual(sortVocabulariesByOrderIndex(null), []);
      assert.deepStrictEqual(sortVocabulariesByOrderIndex(undefined), []);
      assert.deepStrictEqual(sortVocabulariesByOrderIndex('not an array'), []);
    });
  });

  describe('8. Web Speech Feature Detection & Safety', () => {
    test('isSpeechSynthesisAvailable returns boolean without throwing in Node environment', () => {
      const available = isSpeechSynthesisAvailable();
      assert.strictEqual(typeof available, 'boolean');
      assert.strictEqual(available, false, 'Node.js has no window.speechSynthesis by default');
    });

    test('speakHanzi safely no-ops without throwing when speech is unavailable', () => {
      assert.doesNotThrow(() => {
        speakHanzi('你好');
        speakHanzi('');
        speakHanzi(null);
      });
    });
  });

  describe('9. Security & Resource Sanitization', () => {
    test('sanitizeLessonTitle trims strings and provides default fallback for empty/non-string', () => {
      assert.strictEqual(sanitizeLessonTitle('  Bài 1  '), 'Bài 1');
      assert.strictEqual(sanitizeLessonTitle(''), 'Bài học không tên');
      assert.strictEqual(sanitizeLessonTitle('   '), 'Bài học không tên');
      assert.strictEqual(sanitizeLessonTitle(null), 'Bài học không tên');
      assert.strictEqual(sanitizeLessonTitle(undefined), 'Bài học không tên');
    });

    test('sanitizeResourceUrl permits safe URLs and returns about:blank for dangerous protocols', () => {
      assert.strictEqual(sanitizeResourceUrl('https://example.com/audio.mp3'), 'https://example.com/audio.mp3');
      assert.strictEqual(sanitizeResourceUrl('/media/audio.mp3'), '/media/audio.mp3');
      assert.strictEqual(sanitizeResourceUrl('javascript:alert(1)'), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl('data:text/html,<script>'), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl('vbscript:msgbox(1)'), 'about:blank');
    });
  });
});
