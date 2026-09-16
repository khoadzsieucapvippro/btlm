/**
 * =============================================================================
 * UNIT TESTS: MODERATOR AUDIT HISTORY LOGIC (TASK 9E.4)
 * Module: tests/frontend/unit/moderator/moderator-history.test.mjs
 * 
 * Scope:
 * - Response parsing & normalization under @JsonInclude(NON_NULL) omissions.
 * - Action normalization (Approve, Reject, Approved, Rejected, unknown).
 * - Safe flaggedFields parser (JSON array string, comma-separated, plain, malformed).
 * - Lesson identification & display fallback hierarchy.
 * - Timestamp formatting preserving audit precision.
 * - Pagination math and boundaries.
 * - Role-guard access control.
 * =============================================================================
 */

import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

import {
  parseModerationHistory,
  getLessonDisplayName,
  normalizeModerationAction,
  parseFlaggedFields,
  formatHistoryDate,
  calculatePagination,
  hasModeratorAccess
} from '../../../../frontend/js/pages/moderator-history-page.js';

describe('Unit: Moderator Audit History UI Logic (Task 9E.4)', () => {

  // 1. parseModerationHistory
  describe('1. Response Parsing & Normalization under @JsonInclude(NON_NULL)', () => {
    it('parses complete backend PageResponse<ModerationLogResponse> payload', () => {
      const payload = {
        code: 'SUCCESS',
        message: 'Thành công',
        data: {
          items: [
            {
              logId: 101,
              lessonId: 9001,
              lessonTitle: 'HSK 1 - Bài 1',
              moderatorId: 50,
              moderatorEmail: 'mod@example.com',
              action: 'Approve',
              rejectionReason: null,
              flaggedFields: null,
              createdAt: '2026-09-16T10:30:00'
            },
            {
              logId: 102,
              lessonId: 9002,
              lessonTitle: 'HSK 1 - Bài 2',
              moderatorId: 50,
              moderatorEmail: 'mod@example.com',
              action: 'Reject',
              rejectionReason: 'Thiếu pinyin chuẩn',
              flaggedFields: '["vocabularies[0].pinyin"]',
              createdAt: '2026-09-16T11:00:00'
            }
          ],
          page: 0,
          size: 20,
          totalElements: 2,
          totalPages: 1
        }
      };

      const parsed = parseModerationHistory(payload);
      assert.strictEqual(parsed.items.length, 2);
      assert.strictEqual(parsed.items[0].logId, 101);
      assert.strictEqual(parsed.items[0].lessonTitle, 'HSK 1 - Bài 1');
      assert.strictEqual(parsed.items[0].action, 'Approve');
      assert.strictEqual(parsed.items[1].rejectionReason, 'Thiếu pinyin chuẩn');
      assert.strictEqual(parsed.items[1].flaggedFields, '["vocabularies[0].pinyin"]');
      assert.strictEqual(parsed.totalElements, 2);
      assert.strictEqual(parsed.totalPages, 1);
    });

    it('defensively normalizes omitted fields when Jackson excludes null values', () => {
      // With @JsonInclude(NON_NULL), null fields are absent in JSON
      const payload = {
        data: {
          items: [
            {
              logId: 201,
              // lessonTitle is omitted
              // moderatorEmail is omitted
              action: 'Approve',
              // rejectionReason is omitted
              // flaggedFields is omitted
              createdAt: '2026-09-16T12:00:00'
            }
          ],
          page: 1,
          size: 10,
          totalElements: 15,
          totalPages: 2
        }
      };

      const parsed = parseModerationHistory(payload);
      assert.strictEqual(parsed.items.length, 1);
      const item = parsed.items[0];
      assert.strictEqual(item.logId, 201);
      assert.strictEqual(item.lessonTitle, null);
      assert.strictEqual(item.moderatorEmail, null);
      assert.strictEqual(item.action, 'Approve');
      assert.strictEqual(item.rejectionReason, null);
      assert.strictEqual(item.flaggedFields, null);
      assert.strictEqual(parsed.page, 1);
      assert.strictEqual(parsed.size, 10);
      assert.strictEqual(parsed.totalElements, 15);
      assert.strictEqual(parsed.totalPages, 2);
    });

    it('handles null or malformed envelope gracefully without throwing', () => {
      const empty1 = parseModerationHistory(null);
      assert.deepStrictEqual(empty1, { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

      const empty2 = parseModerationHistory({});
      assert.deepStrictEqual(empty2, { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

      const empty3 = parseModerationHistory({ data: { items: [null, undefined, 'invalid'] } });
      assert.strictEqual(empty3.items.length, 3);
      assert.strictEqual(empty3.items[0].logId, 0);
      assert.strictEqual(empty3.items[0].action, 'Unknown');
    });
  });

  // 2. getLessonDisplayName
  describe('2. Lesson Display Name & Fallback Hierarchy', () => {
    it('returns title when title is non-empty', () => {
      assert.strictEqual(getLessonDisplayName('HSK 1 - Bài 1', 9001), 'HSK 1 - Bài 1');
      assert.strictEqual(getLessonDisplayName('  Tiêu đề có khoảng trắng  ', 9002), 'Tiêu đề có khoảng trắng');
    });

    it('falls back to "Bài học #<lessonId>" when title is missing or empty', () => {
      assert.strictEqual(getLessonDisplayName(null, 9001), 'Bài học #9001');
      assert.strictEqual(getLessonDisplayName('', 9002), 'Bài học #9002');
      assert.strictEqual(getLessonDisplayName('   ', 9003), 'Bài học #9003');
    });

    it('falls back to "Bài học không xác định" when both title and lessonId are missing', () => {
      assert.strictEqual(getLessonDisplayName(null, null), 'Bài học không xác định');
      assert.strictEqual(getLessonDisplayName('', 0), 'Bài học không xác định');
      assert.strictEqual(getLessonDisplayName(undefined, undefined), 'Bài học không xác định');
    });
  });

  // 3. normalizeModerationAction
  describe('3. Action Normalization & Mismatch Tolerance', () => {
    it('maps verified backend value "Approve" and variant "Approved" to Phê duyệt', () => {
      const a1 = normalizeModerationAction('Approve');
      assert.strictEqual(a1.type, 'approve');
      assert.strictEqual(a1.label, 'Phê duyệt');
      assert.strictEqual(a1.badgeClass, 'badge-status badge-status-approved');

      const a2 = normalizeModerationAction('Approved');
      assert.strictEqual(a2.type, 'approve');
      assert.strictEqual(a2.label, 'Phê duyệt');

      const a3 = normalizeModerationAction('approve');
      assert.strictEqual(a3.type, 'approve');
    });

    it('maps verified backend value "Reject" and variant "Rejected" to Từ chối', () => {
      const r1 = normalizeModerationAction('Reject');
      assert.strictEqual(r1.type, 'reject');
      assert.strictEqual(r1.label, 'Từ chối');
      assert.strictEqual(r1.badgeClass, 'badge-status badge-status-danger');

      const r2 = normalizeModerationAction('Rejected');
      assert.strictEqual(r2.type, 'reject');
      assert.strictEqual(r2.label, 'Từ chối');

      const r3 = normalizeModerationAction('reject');
      assert.strictEqual(r3.type, 'reject');
    });

    it('tolerates unexpected or unknown action values gracefully without crashing', () => {
      const u1 = normalizeModerationAction('Flag');
      assert.strictEqual(u1.type, 'unknown');
      assert.strictEqual(u1.label, 'Flag');
      assert.strictEqual(u1.badgeClass, 'badge-status badge-status-draft');

      const u2 = normalizeModerationAction(null);
      assert.strictEqual(u2.type, 'unknown');
      assert.strictEqual(u2.label, 'Không xác định');
    });
  });

  // 4. parseFlaggedFields
  describe('4. Safe Flagged Fields Parser', () => {
    it('parses JSON string array and maps known fields to human-readable labels', () => {
      const jsonStr = '["title", "pinyin", "meaningVi", "vocabularies[0].pinyin"]';
      const parsed = parseFlaggedFields(jsonStr);

      assert.strictEqual(parsed.length, 4);
      assert.deepStrictEqual(parsed[0], { raw: 'title', label: 'Tiêu đề bài học' });
      assert.deepStrictEqual(parsed[1], { raw: 'pinyin', label: 'Phiên âm Pinyin' });
      assert.deepStrictEqual(parsed[2], { raw: 'meaningVi', label: 'Dịch nghĩa tiếng Việt' });
      assert.deepStrictEqual(parsed[3], { raw: 'vocabularies[0].pinyin', label: 'vocabularies[0].pinyin' });
    });

    it('parses comma-separated string fallback', () => {
      const commaStr = 'title, audioUrl, orderIndex';
      const parsed = parseFlaggedFields(commaStr);

      assert.strictEqual(parsed.length, 3);
      assert.strictEqual(parsed[0].label, 'Tiêu đề bài học');
      assert.strictEqual(parsed[1].label, 'Âm thanh phát âm');
      assert.strictEqual(parsed[2].label, 'Thứ tự từ vựng');
    });

    it('parses plain single string value safely', () => {
      const single = parseFlaggedFields('title');
      assert.strictEqual(single.length, 1);
      assert.strictEqual(single[0].raw, 'title');
      assert.strictEqual(single[0].label, 'Tiêu đề bài học');
    });

    it('defends against malformed JSON without throwing SyntaxError', () => {
      const malformed = '["unclosed, invalid json';
      const parsed = parseFlaggedFields(malformed);
      // Splits on comma fallback or returns single chip
      assert.ok(Array.isArray(parsed));
      assert.ok(parsed.length > 0);
      assert.ok(parsed[0].raw.length > 0);
    });

    it('returns empty array for null, undefined, or empty strings', () => {
      assert.deepStrictEqual(parseFlaggedFields(null), []);
      assert.deepStrictEqual(parseFlaggedFields(undefined), []);
      assert.deepStrictEqual(parseFlaggedFields(''), []);
      assert.deepStrictEqual(parseFlaggedFields('   '), []);
    });
  });

  // 5. formatHistoryDate
  describe('5. Audit Timestamp Formatting', () => {
    it('formats ISO timestamps with full seconds precision', () => {
      const iso = '2026-09-16T14:30:45';
      const formatted = formatHistoryDate(iso);
      assert.ok(formatted.includes('16/09/2026'));
      assert.ok(formatted.includes('14:30:45'));
    });

    it('returns "--" for null, undefined, or invalid date values', () => {
      assert.strictEqual(formatHistoryDate(null), '--');
      assert.strictEqual(formatHistoryDate(undefined), '--');
      assert.strictEqual(formatHistoryDate('invalid-date'), '--');
    });
  });

  // 6. calculatePagination
  describe('6. Pagination Math & Boundaries', () => {
    it('calculates correct pagination state for standard page', () => {
      const p = calculatePagination(1, 20, 45, 3);
      assert.strictEqual(p.currentPage, 1);
      assert.strictEqual(p.totalPages, 3);
      assert.strictEqual(p.hasPrev, true);
      assert.strictEqual(p.hasNext, true);
      assert.strictEqual(p.startRecord, 21);
      assert.strictEqual(p.endRecord, 40);
    });

    it('calculates boundary conditions for first and last page', () => {
      // First page
      const pFirst = calculatePagination(0, 20, 45, 3);
      assert.strictEqual(pFirst.hasPrev, false);
      assert.strictEqual(pFirst.hasNext, true);
      assert.strictEqual(pFirst.startRecord, 1);
      assert.strictEqual(pFirst.endRecord, 20);

      // Last page
      const pLast = calculatePagination(2, 20, 45, 3);
      assert.strictEqual(pLast.hasPrev, true);
      assert.strictEqual(pLast.hasNext, false);
      assert.strictEqual(pLast.startRecord, 41);
      assert.strictEqual(pLast.endRecord, 45);
    });

    it('handles zero elements gracefully', () => {
      const pZero = calculatePagination(0, 20, 0, 0);
      assert.strictEqual(pZero.hasPrev, false);
      assert.strictEqual(pZero.hasNext, false);
      assert.strictEqual(pZero.startRecord, 0);
      assert.strictEqual(pZero.endRecord, 0);
    });
  });

  // 7. hasModeratorAccess
  describe('7. Role-Guard Access Rules', () => {
    it('allows Moderator role', () => {
      const mockAuth = { hasRole: (role) => role === 'Moderator' };
      assert.strictEqual(hasModeratorAccess(mockAuth), true);
    });

    it('allows Admin role', () => {
      const mockAuth = { hasRole: (role) => role === 'Admin' };
      assert.strictEqual(hasModeratorAccess(mockAuth), true);
    });

    it('rejects Learner and Creator roles', () => {
      const mockLearner = { hasRole: (role) => role === 'Learner' };
      assert.strictEqual(hasModeratorAccess(mockLearner), false);

      const mockCreator = { hasRole: (role) => role === 'Creator' };
      assert.strictEqual(hasModeratorAccess(mockCreator), false);
    });

    it('rejects unauthenticated or null auth instance', () => {
      assert.strictEqual(hasModeratorAccess(null), false);
      assert.strictEqual(hasModeratorAccess({}), false);
    });
  });

});
