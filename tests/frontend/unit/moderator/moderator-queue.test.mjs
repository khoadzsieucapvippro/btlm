/**
 * =============================================================================
 * UNIT TEST SUITE: MODERATOR REVIEW QUEUE UI (TASK 9E.1)
 * File: tests/frontend/unit/moderator/moderator-queue.test.mjs
 * 
 * Verifies:
 * - 1. PageResponse parsing & envelope normalization.
 * - 2. Queue item normalization, null-safety, and zero data fabrication.
 * - 3. Date formatting semantics ("Cập nhật lần cuối" vs "Ngày tạo").
 * - 4. Pagination boundary mathematics (first, middle, last, empty, single-page).
 * - 5. Review action route generation (Task 9E.2 navigation path).
 * - 6. Status badge mapping (strictly Pending).
 * - 7. Role guard access rules (Moderator & Admin allowed; Learner & Creator denied).
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  parseModerationQueue,
  calculatePagination,
  formatQueueDate,
  getReviewUrl,
  getQueueStatusBadge,
  hasModeratorAccess
} from '../../../../frontend/js/pages/moderator-queue-page.js';

describe('Unit: Moderator Review Queue UI (Task 9E.1)', () => {

  describe('1. PageResponse Parsing & Envelope Normalization', () => {
    test('handles null, undefined, or malformed input without throwing', () => {
      const rNull = parseModerationQueue(null);
      const rUndef = parseModerationQueue(undefined);
      const rEmpty = parseModerationQueue({});
      const rString = parseModerationQueue('unexpected string');

      assert.deepStrictEqual(rNull, { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
      assert.deepStrictEqual(rUndef, { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
      assert.deepStrictEqual(rEmpty, { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
      assert.deepStrictEqual(rString, { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    });

    test('parses wrapped ApiResponse<PageResponse<ModerationQueueResponse>> envelope', () => {
      const envelope = {
        code: 'SUCCESS',
        message: 'Thao tác thành công',
        data: {
          items: [
            {
              lessonId: 101,
              title: 'HSK 1 — Bài 1: Chào hỏi',
              status: 'Pending',
              creatorId: 201,
              creatorEmail: 'creator1@test.com',
              vocabularyCount: 10,
              createdAt: '2026-09-01T08:00:00Z',
              updatedAt: '2026-09-02T10:30:00Z'
            }
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1
        }
      };

      const result = parseModerationQueue(envelope);
      assert.strictEqual(result.items.length, 1);
      assert.strictEqual(result.page, 0);
      assert.strictEqual(result.size, 20);
      assert.strictEqual(result.totalElements, 1);
      assert.strictEqual(result.totalPages, 1);
      assert.strictEqual(result.items[0].lessonId, 101);
      assert.strictEqual(result.items[0].title, 'HSK 1 — Bài 1: Chào hỏi');
      assert.strictEqual(result.items[0].creatorEmail, 'creator1@test.com');
    });

    test('parses raw PageResponse object directly without envelope wrapper', () => {
      const rawPageResponse = {
        items: [
          {
            lessonId: 102,
            title: 'HSK 1 — Bài 2: Cảm ơn',
            status: 'Pending',
            creatorId: 202,
            creatorEmail: 'creator2@test.com',
            vocabularyCount: 12,
            createdAt: '2026-09-03T08:00:00Z',
            updatedAt: '2026-09-04T09:15:00Z'
          }
        ],
        page: 1,
        size: 10,
        totalElements: 15,
        totalPages: 2
      };

      const result = parseModerationQueue(rawPageResponse);
      assert.strictEqual(result.items.length, 1);
      assert.strictEqual(result.page, 1);
      assert.strictEqual(result.size, 10);
      assert.strictEqual(result.totalElements, 15);
      assert.strictEqual(result.totalPages, 2);
    });
  });

  describe('2. Queue Item Normalization & Zero Data Fabrication', () => {
    test('normalizes missing or nullable creatorEmail gracefully without fabricating creatorName', () => {
      const itemWithNullEmail = {
        lessonId: 301,
        title: 'Bài học không có email',
        status: 'Pending',
        creatorId: 99,
        creatorEmail: null,
        vocabularyCount: 5,
        createdAt: '2026-09-01T00:00:00Z',
        updatedAt: '2026-09-01T00:00:00Z'
      };

      const parsed = parseModerationQueue({ items: [itemWithNullEmail] });
      const item = parsed.items[0];

      assert.strictEqual(item.lessonId, 301);
      assert.strictEqual(item.creatorEmail, null);
      // Ensure no creatorName is fabricated
      assert.strictEqual('creatorName' in item, false);
      assert.strictEqual(item.creatorId, 99);
    });

    test('normalizes missing or nullable vocabularyCount to 0', () => {
      const item = {
        lessonId: 302,
        title: 'Bài học chưa có từ vựng',
        status: 'Pending',
        vocabularyCount: null
      };

      const parsed = parseModerationQueue({ items: [item] });
      assert.strictEqual(parsed.items[0].vocabularyCount, 0);
    });

    test('normalizes corrupted or non-object row gracefully', () => {
      const parsed = parseModerationQueue({ items: [null, undefined, 'invalid'] });
      assert.strictEqual(parsed.items.length, 3);
      assert.strictEqual(parsed.items[0].lessonId, 0);
      assert.strictEqual(parsed.items[0].title, 'Bài học không xác định');
      assert.strictEqual(parsed.items[0].status, 'Pending');
    });

    test('ensures no submittedAt field is fabricated or conflated', () => {
      const item = {
        lessonId: 303,
        title: 'Kiểm tra trường thời gian',
        status: 'Pending',
        createdAt: '2026-09-01T12:00:00Z',
        updatedAt: '2026-09-05T18:00:00Z'
      };

      const parsed = parseModerationQueue({ items: [item] });
      assert.strictEqual('submittedAt' in parsed.items[0], false);
      assert.strictEqual(parsed.items[0].createdAt, '2026-09-01T12:00:00Z');
      assert.strictEqual(parsed.items[0].updatedAt, '2026-09-05T18:00:00Z');
    });
  });

  describe('3. Date Formatting Semantics ("Cập nhật lần cuối")', () => {
    test('formats valid ISO datetime string to DD/MM/YYYY HH:mm', () => {
      const iso = '2026-09-15T10:45:00Z';
      const formatted = formatQueueDate(iso);

      // Should be valid format DD/MM/YYYY HH:mm
      assert.match(formatted, /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/);
      assert.strictEqual(formatted.includes('2026'), true);
    });

    test('returns fallback "--" for null, undefined, or empty string', () => {
      assert.strictEqual(formatQueueDate(null), '--');
      assert.strictEqual(formatQueueDate(undefined), '--');
      assert.strictEqual(formatQueueDate(''), '--');
    });

    test('returns fallback "--" for non-date or malformed string', () => {
      assert.strictEqual(formatQueueDate('invalid-date-string'), '--');
      assert.strictEqual(formatQueueDate(123456789), '--');
      assert.strictEqual(formatQueueDate({}), '--');
    });
  });

  describe('4. Pagination Boundary Mathematics', () => {
    test('first page calculations (page 0 of 5, size 20, totalElements 100)', () => {
      const p = calculatePagination(0, 20, 100, 5);
      assert.strictEqual(p.currentPage, 0);
      assert.strictEqual(p.totalPages, 5);
      assert.strictEqual(p.hasPrev, false);
      assert.strictEqual(p.hasNext, true);
      assert.strictEqual(p.startRecord, 1);
      assert.strictEqual(p.endRecord, 20);
    });

    test('middle page calculations (page 2 of 5, size 10, totalElements 45)', () => {
      const p = calculatePagination(2, 10, 45, 5);
      assert.strictEqual(p.currentPage, 2);
      assert.strictEqual(p.totalPages, 5);
      assert.strictEqual(p.hasPrev, true);
      assert.strictEqual(p.hasNext, true);
      assert.strictEqual(p.startRecord, 21);
      assert.strictEqual(p.endRecord, 30);
    });

    test('last page calculations (page 4 of 5, size 10, totalElements 45)', () => {
      const p = calculatePagination(4, 10, 45, 5);
      assert.strictEqual(p.currentPage, 4);
      assert.strictEqual(p.totalPages, 5);
      assert.strictEqual(p.hasPrev, true);
      assert.strictEqual(p.hasNext, false);
      assert.strictEqual(p.startRecord, 41);
      assert.strictEqual(p.endRecord, 45);
    });

    test('empty queue calculations (totalElements 0, totalPages 0)', () => {
      const p = calculatePagination(0, 20, 0, 0);
      assert.strictEqual(p.currentPage, 0);
      assert.strictEqual(p.totalPages, 0);
      assert.strictEqual(p.hasPrev, false);
      assert.strictEqual(p.hasNext, false);
      assert.strictEqual(p.startRecord, 0);
      assert.strictEqual(p.endRecord, 0);
    });

    test('single-page result calculations (totalElements 7, size 20, totalPages 1)', () => {
      const p = calculatePagination(0, 20, 7, 1);
      assert.strictEqual(p.currentPage, 0);
      assert.strictEqual(p.totalPages, 1);
      assert.strictEqual(p.hasPrev, false);
      assert.strictEqual(p.hasNext, false);
      assert.strictEqual(p.startRecord, 1);
      assert.strictEqual(p.endRecord, 7);
    });

    test('handles negative or out-of-range page parameter safely', () => {
      const pNegative = calculatePagination(-5, 20, 50, 3);
      assert.strictEqual(pNegative.currentPage, 0);

      const pOver = calculatePagination(999, 20, 50, 3);
      assert.strictEqual(pOver.currentPage, 2);
    });
  });

  describe('5. Review Action Route Generation (Task 9E.2 Link)', () => {
    test('generates valid review URL pointing to moderator-review.html?id=<lessonId>', () => {
      assert.strictEqual(getReviewUrl(1001), 'moderator-review.html?id=1001');
      assert.strictEqual(getReviewUrl('9368'), 'moderator-review.html?id=9368');
    });

    test('safely encodes special characters in lesson ID', () => {
      assert.strictEqual(getReviewUrl('abc 123'), 'moderator-review.html?id=abc%20123');
    });
  });

  describe('6. Status Badge Mapping', () => {
    test('maps Pending status to standard badge-status-pending and "Chờ duyệt"', () => {
      const badge = getQueueStatusBadge('Pending');
      assert.strictEqual(badge.label, 'Chờ duyệt');
      assert.strictEqual(badge.badgeClass, 'badge-status badge-status-pending');
    });
  });

  describe('7. Role Guard Access Rules (RBAC)', () => {
    function createMockAuth(roles = []) {
      return {
        isAuthenticated: () => roles.length > 0,
        hasRole: (role) => roles.includes(role),
        getRoles: () => roles
      };
    }

    test('allows access for role Moderator', () => {
      const auth = createMockAuth(['Moderator']);
      assert.strictEqual(hasModeratorAccess(auth), true);
    });

    test('allows access for role Admin', () => {
      const auth = createMockAuth(['Admin']);
      assert.strictEqual(hasModeratorAccess(auth), true);
    });

    test('allows access for combined Moderator and Admin roles', () => {
      const auth = createMockAuth(['Moderator', 'Admin']);
      assert.strictEqual(hasModeratorAccess(auth), true);
    });

    test('denies access for role Creator', () => {
      const auth = createMockAuth(['Creator']);
      assert.strictEqual(hasModeratorAccess(auth), false);
    });

    test('denies access for role Learner', () => {
      const auth = createMockAuth(['Learner']);
      assert.strictEqual(hasModeratorAccess(auth), false);
    });

    test('denies access for unauthenticated or empty roles', () => {
      assert.strictEqual(hasModeratorAccess(createMockAuth([])), false);
      assert.strictEqual(hasModeratorAccess(null), false);
      assert.strictEqual(hasModeratorAccess(undefined), false);
    });
  });

});
