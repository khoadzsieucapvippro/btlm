/**
 * =============================================================================
 * TARGETED UNIT TESTS: ADMIN LESSON OVERSIGHT UI (TASK 9F.5)
 * Module: tests/frontend/unit/admin/admin-lessons.test.mjs
 * 
 * Scope:
 * - Pure logic unit testing of admin-lessons-page.js functions:
 *   1. hasAdminAccess: Admin role guard invariant (canonical 'Admin').
 *   2. normalizeStatusFilter: Normalization of status filter tokens ('All'/'' -> null, 'draft' -> 'Draft', etc.).
 *   3. mapStatusLabel & mapStatusBadge: Vietnamese labels and CSS badge classes for canonical lifecycle states.
 *   4. buildAdminLessonsParams: Query params builder ensuring omission of status on 'All', 0-based page, and size.
 *   5. calculatePaginationSummary: Human-readable pagination text with correct 1-based display ranges and zero-based page index.
 *   6. extractStatusCounts: Server-side totalElements extraction for all 5 KPI cards (Total, Draft, Pending, Approved, Rejected).
 *   7. determineDetailStrategy: Matrix matching backend routes (Approved -> /lessons, Pending -> /moderator/lessons, Draft/Rejected -> unsupported_admin_detail).
 *   8. formatLessonDate: Locale date formatting with graceful fallback for invalid/null dates.
 *   9. parsePageResponse: Normalizing PageResponse structure safely.
 * =============================================================================
 */

import { describe, test } from 'node:test';
import assert from 'node:assert/strict';

import {
  hasAdminAccess,
  normalizeStatusFilter,
  mapStatusLabel,
  mapStatusBadge,
  buildAdminLessonsParams,
  determineDetailStrategy,
  formatLessonDate,
  calculatePaginationSummary,
  extractStatusCounts,
  parsePageResponse
} from '../../../../frontend/js/pages/admin-lessons-page.js';

describe('Task 9F.5 — Admin Lesson Oversight Pure Logic Unit Tests', () => {

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

    test('rejects other roles (Moderator, Creator, User)', () => {
      const mockManager = { hasRole: (role) => role === 'Moderator' };
      assert.strictEqual(hasAdminAccess(mockManager), false);
    });

    test('returns false when authManager is null or undefined', () => {
      assert.strictEqual(hasAdminAccess(null), false);
      assert.strictEqual(hasAdminAccess(undefined), false);
      assert.strictEqual(hasAdminAccess({}), false);
    });
  });

  /* ---------------------------------------------------------------------------
   * 2. normalizeStatusFilter
   * --------------------------------------------------------------------------- */
  describe('2. normalizeStatusFilter', () => {
    test('returns empty string for All, ALL, all, empty string, null, and undefined', () => {
      assert.strictEqual(normalizeStatusFilter('All'), '');
      assert.strictEqual(normalizeStatusFilter('ALL'), '');
      assert.strictEqual(normalizeStatusFilter('all'), '');
      assert.strictEqual(normalizeStatusFilter(''), '');
      assert.strictEqual(normalizeStatusFilter('   '), '');
      assert.strictEqual(normalizeStatusFilter(null), '');
      assert.strictEqual(normalizeStatusFilter(undefined), '');
    });

    test('normalizes canonical statuses regardless of casing and surrounding spaces', () => {
      assert.strictEqual(normalizeStatusFilter('draft'), 'Draft');
      assert.strictEqual(normalizeStatusFilter('DRAFT'), 'Draft');
      assert.strictEqual(normalizeStatusFilter('  Draft  '), 'Draft');

      assert.strictEqual(normalizeStatusFilter('pending'), 'Pending');
      assert.strictEqual(normalizeStatusFilter('PENDING'), 'Pending');
      assert.strictEqual(normalizeStatusFilter('Pending'), 'Pending');

      assert.strictEqual(normalizeStatusFilter('approved'), 'Approved');
      assert.strictEqual(normalizeStatusFilter('APPROVED'), 'Approved');
      assert.strictEqual(normalizeStatusFilter('Approved'), 'Approved');

      assert.strictEqual(normalizeStatusFilter('rejected'), 'Rejected');
      assert.strictEqual(normalizeStatusFilter('REJECTED'), 'Rejected');
      assert.strictEqual(normalizeStatusFilter('Rejected'), 'Rejected');
    });

    test('returns empty string for unrecognized/invalid status tokens', () => {
      assert.strictEqual(normalizeStatusFilter('Unknown'), '');
      assert.strictEqual(normalizeStatusFilter('Published'), '');
      assert.strictEqual(normalizeStatusFilter('Archived'), '');
    });
  });

  /* ---------------------------------------------------------------------------
   * 3. mapStatusLabel & mapStatusBadge
   * --------------------------------------------------------------------------- */
  describe('3. mapStatusLabel & mapStatusBadge', () => {
    test('maps Draft status to Vietnamese label and badge class', () => {
      assert.strictEqual(mapStatusLabel('Draft'), 'Bản nháp');
      const badge = mapStatusBadge('Draft');
      assert.strictEqual(badge.label, 'Bản nháp');
      assert.ok(badge.className.includes('bg-secondary-subtle'));
    });

    test('maps Pending status to Vietnamese label and badge class', () => {
      assert.strictEqual(mapStatusLabel('Pending'), 'Chờ duyệt');
      const badge = mapStatusBadge('Pending');
      assert.strictEqual(badge.label, 'Chờ duyệt');
      assert.ok(badge.className.includes('bg-warning-subtle'));
    });

    test('maps Approved status to Vietnamese label and badge class', () => {
      assert.strictEqual(mapStatusLabel('Approved'), 'Đã duyệt');
      const badge = mapStatusBadge('Approved');
      assert.strictEqual(badge.label, 'Đã duyệt');
      assert.ok(badge.className.includes('bg-success-subtle'));
    });

    test('maps Rejected status to Vietnamese label and badge class', () => {
      assert.strictEqual(mapStatusLabel('Rejected'), 'Bị từ chối');
      const badge = mapStatusBadge('Rejected');
      assert.strictEqual(badge.label, 'Bị từ chối');
      assert.ok(badge.className.includes('bg-danger-subtle'));
    });

    test('handles casing-insensitive input and fallback for unknown status', () => {
      assert.strictEqual(mapStatusLabel('dRaFt'), 'Bản nháp');
      assert.strictEqual(mapStatusLabel('CUSTOM_STATUS'), 'CUSTOM_STATUS');
      const badge = mapStatusBadge('CUSTOM_STATUS');
      assert.strictEqual(badge.label, 'CUSTOM_STATUS');
      assert.strictEqual(mapStatusLabel(null), 'Không xác định');
      assert.strictEqual(mapStatusBadge(null).label, 'Không xác định');
    });
  });

  /* ---------------------------------------------------------------------------
   * 4. buildAdminLessonsParams
   * --------------------------------------------------------------------------- */
  describe('4. buildAdminLessonsParams', () => {
    test('omits status param when status is null (All lessons query)', () => {
      const params = buildAdminLessonsParams(null, 0, 10);
      assert.deepStrictEqual(params, { page: 0, size: 10 });
      assert.strictEqual('status' in params, false);
    });

    test('omits status param when status is "All"', () => {
      const params = buildAdminLessonsParams('All', 0, 10);
      assert.deepStrictEqual(params, { page: 0, size: 10 });
      assert.strictEqual('status' in params, false);
    });

    test('includes canonical status when filtering by specific lifecycle status', () => {
      const pDraft = buildAdminLessonsParams('Draft', 0, 10);
      assert.deepStrictEqual(pDraft, { status: 'Draft', page: 0, size: 10 });

      const pPending = buildAdminLessonsParams('Pending', 1, 20);
      assert.deepStrictEqual(pPending, { status: 'Pending', page: 1, size: 20 });

      const pApproved = buildAdminLessonsParams('Approved', 3, 50);
      assert.deepStrictEqual(pApproved, { status: 'Approved', page: 3, size: 50 });

      const pRejected = buildAdminLessonsParams('Rejected', 0, 10);
      assert.deepStrictEqual(pRejected, { status: 'Rejected', page: 0, size: 10 });
    });

    test('enforces zero-based page indexing and clamps negative page to 0', () => {
      const params = buildAdminLessonsParams(null, -5, 10);
      assert.strictEqual(params.page, 0);
    });

    test('applies default size if size is missing or invalid', () => {
      const p1 = buildAdminLessonsParams(null, 0, null);
      assert.strictEqual(p1.size, 20);
      const p2 = buildAdminLessonsParams(null, 0, 0);
      assert.strictEqual(p2.size, 20);
      const p3 = buildAdminLessonsParams(null, 0, -10);
      assert.strictEqual(p3.size, 20);
    });
  });

  /* ---------------------------------------------------------------------------
   * 5. calculatePaginationSummary
   * --------------------------------------------------------------------------- */
  describe('5. calculatePaginationSummary', () => {
    test('formats summary accurately for non-empty page', () => {
      const s1 = calculatePaginationSummary(0, 10, 35);
      assert.strictEqual(s1.start, 1);
      assert.strictEqual(s1.end, 10);
      assert.strictEqual(s1.total, 35);
      assert.strictEqual(s1.totalPages, 4);
      assert.strictEqual(s1.hasPrevious, false);
      assert.strictEqual(s1.hasNext, true);

      const s2 = calculatePaginationSummary(1, 10, 35);
      assert.strictEqual(s2.start, 11);
      assert.strictEqual(s2.end, 20);
      assert.strictEqual(s2.hasPrevious, true);
      assert.strictEqual(s2.hasNext, true);

      const s3 = calculatePaginationSummary(3, 10, 35);
      assert.strictEqual(s3.start, 31);
      assert.strictEqual(s3.end, 35);
      assert.strictEqual(s3.hasPrevious, true);
      assert.strictEqual(s3.hasNext, false);
    });

    test('formats summary accurately when totalElements is 0', () => {
      const s = calculatePaginationSummary(0, 10, 0);
      assert.strictEqual(s.start, 0);
      assert.strictEqual(s.end, 0);
      assert.strictEqual(s.total, 0);
      assert.strictEqual(s.totalPages, 1);
      assert.strictEqual(s.hasPrevious, false);
      assert.strictEqual(s.hasNext, false);
    });

    test('formats summary when itemCount is less than pageSize on page 0', () => {
      const s = calculatePaginationSummary(0, 10, 4);
      assert.strictEqual(s.start, 1);
      assert.strictEqual(s.end, 4);
      assert.strictEqual(s.total, 4);
      assert.strictEqual(s.totalPages, 1);
      assert.strictEqual(s.hasPrevious, false);
      assert.strictEqual(s.hasNext, false);
    });
  });

  /* ---------------------------------------------------------------------------
   * 6. extractStatusCounts (Global TotalElements Extraction)
   * --------------------------------------------------------------------------- */
  describe('6. extractStatusCounts', () => {
    test('extracts totalElements from server responses correctly', () => {
      const responses = {
        total: { totalElements: 120, items: [{ lessonId: 1 }] },
        draft: { totalElements: 15, items: [] },
        pending: { totalElements: 25, items: [] },
        approved: { totalElements: 70, items: [] },
        rejected: { totalElements: 10, items: [] }
      };
      const counts = extractStatusCounts(responses);
      assert.strictEqual(counts.total, 120);
      assert.strictEqual(counts.draft, 15);
      assert.strictEqual(counts.pending, 25);
      assert.strictEqual(counts.approved, 70);
      assert.strictEqual(counts.rejected, 10);
    });

    test('handles missing or failed response properties with null', () => {
      const responses = {
        total: { totalElements: 50 },
        draft: null, // Probe failed
        pending: undefined,
        approved: { totalElements: 30 },
        rejected: { status: 500 } // Error response
      };
      const counts = extractStatusCounts(responses);
      assert.strictEqual(counts.total, 50);
      assert.strictEqual(counts.draft, null);
      assert.strictEqual(counts.pending, null);
      assert.strictEqual(counts.approved, 30);
      assert.strictEqual(counts.rejected, null);
    });

    test('handles empty input object safely', () => {
      const counts = extractStatusCounts(null);
      assert.strictEqual(counts.total, null);
      assert.strictEqual(counts.draft, null);
      assert.strictEqual(counts.pending, null);
      assert.strictEqual(counts.approved, null);
      assert.strictEqual(counts.rejected, null);
    });
  });

  /* ---------------------------------------------------------------------------
   * 7. determineDetailStrategy & Unsupported Detail Handling
   * --------------------------------------------------------------------------- */
  describe('7. determineDetailStrategy & Unsupported Detail Handling', () => {
    test('selects public_detail strategy for Approved lessons', () => {
      const res = determineDetailStrategy('Approved', 42);
      assert.strictEqual(res.strategy, 'APPROVED');
      assert.strictEqual(res.endpoint, '/lessons/42');
      assert.strictEqual(res.canFetchDetail, true);
      assert.strictEqual(res.limitationNotice, null);
    });

    test('selects moderator_detail strategy for Pending lessons (Admin authorized)', () => {
      const res = determineDetailStrategy('Pending', 77);
      assert.strictEqual(res.strategy, 'PENDING');
      assert.strictEqual(res.endpoint, '/moderator/lessons/77');
      assert.strictEqual(res.canFetchDetail, true);
      assert.strictEqual(res.limitationNotice, null);
    });

    test('selects unsupported_admin_detail for Draft lessons with truthful limitation notice', () => {
      const res = determineDetailStrategy('Draft', 12);
      assert.strictEqual(res.strategy, 'UNSUPPORTED');
      assert.strictEqual(res.endpoint, null);
      assert.strictEqual(res.canFetchDetail, false);
      assert.ok(res.limitationNotice);
      // Verify truthful contract limitation: mentions absence of admin endpoint, NOT "không có nội dung"
      assert.ok(res.limitationNotice.includes('chưa có endpoint quản trị'));
      assert.strictEqual(res.limitationNotice.includes('Không có nội dung'), false);
    });

    test('selects unsupported_admin_detail for Rejected lessons with truthful limitation notice', () => {
      const res = determineDetailStrategy('Rejected', 18);
      assert.strictEqual(res.strategy, 'UNSUPPORTED');
      assert.strictEqual(res.endpoint, null);
      assert.strictEqual(res.canFetchDetail, false);
      assert.ok(res.limitationNotice);
      assert.ok(res.limitationNotice.includes('chưa có endpoint quản trị'));
      assert.strictEqual(res.limitationNotice.includes('Không có nội dung'), false);
    });

    test('handles unknown status with truthful limitation notice', () => {
      const res = determineDetailStrategy('Archived', 99);
      assert.strictEqual(res.strategy, 'UNSUPPORTED');
      assert.strictEqual(res.endpoint, null);
      assert.strictEqual(res.canFetchDetail, false);
      assert.ok(res.limitationNotice);
    });
  });

  /* ---------------------------------------------------------------------------
   * 8. formatLessonDate
   * --------------------------------------------------------------------------- */
  describe('8. formatLessonDate', () => {
    test('formats valid ISO datetime string', () => {
      const formatted = formatLessonDate('2026-09-17T12:30:00Z');
      assert.ok(formatted !== '—');
      // Should contain date elements
      assert.ok(formatted.includes('2026') || formatted.includes('17'));
    });

    test('returns em-dash placeholder for null, undefined, or empty date', () => {
      assert.strictEqual(formatLessonDate(null), '—');
      assert.strictEqual(formatLessonDate(undefined), '—');
      assert.strictEqual(formatLessonDate(''), '—');
    });

    test('returns em-dash placeholder for invalid date string', () => {
      assert.strictEqual(formatLessonDate('not-a-date'), '—');
    });
  });

  /* ---------------------------------------------------------------------------
   * 9. parsePageResponse
   * --------------------------------------------------------------------------- */
  describe('9. parsePageResponse', () => {
    test('extracts items and metadata correctly from valid PageResponse', () => {
      const raw = {
        items: [{ lessonId: 1, title: 'Lesson 1' }, { lessonId: 2, title: 'Lesson 2' }],
        page: 0,
        size: 10,
        totalElements: 2,
        totalPages: 1,
        first: true,
        last: true
      };
      const parsed = parsePageResponse(raw);
      assert.strictEqual(parsed.items.length, 2);
      assert.strictEqual(parsed.page, 0);
      assert.strictEqual(parsed.size, 10);
      assert.strictEqual(parsed.totalElements, 2);
      assert.strictEqual(parsed.totalPages, 1);
      assert.strictEqual(parsed.first, true);
      assert.strictEqual(parsed.last, true);
    });

    test('handles content property if returned instead of items', () => {
      const raw = {
        content: [{ lessonId: 3 }],
        number: 1,
        size: 20,
        totalElements: 25,
        totalPages: 2
      };
      const parsed = parsePageResponse(raw);
      assert.strictEqual(parsed.items.length, 1);
      assert.strictEqual(parsed.page, 1);
      assert.strictEqual(parsed.totalElements, 25);
    });

    test('safely falls back on empty or malformed input', () => {
      const parsed = parsePageResponse(null);
      assert.deepStrictEqual(parsed.items, []);
      assert.strictEqual(parsed.totalElements, 0);
      assert.strictEqual(parsed.totalPages, 0);
      assert.strictEqual(parsed.first, true);
      assert.strictEqual(parsed.last, true);
    });
  });

});
