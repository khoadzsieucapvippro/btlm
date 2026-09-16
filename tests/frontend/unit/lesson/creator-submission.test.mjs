/**
 * =============================================================================
 * UNIT TEST SUITE: CREATOR LESSON SUBMISSION & STATUS VISIBILITY (TASK 9D.3)
 * File: tests/frontend/unit/lesson/creator-submission.test.mjs
 * 
 * Verifies:
 * - canSubmitLesson: Draft/Rejected with >0 vocabularies = true.
 * - canSubmitLesson: Empty lessons (0 vocabularies) = false with appropriate reason.
 * - canSubmitLesson: Pending/Approved = false with appropriate reason.
 * - getSubmitButtonConfig: "Nộp duyệt" for Draft, "Nộp lại" for Rejected, null for others.
 * - isLessonEditable: Lifecycle locking contract (Draft/Rejected vs Pending/Approved).
 * - parseRejectionFeedback: Sealed backend contract detection (isContractBlocked: true).
 * - parseRejectionFeedback: Truthful Vietnamese fallback message verification.
 * - parseRejectionFeedback: Forward compatibility (rejectionReason, flaggedFields parsing).
 * - Security: No eval() used for flaggedFields; safe fallback for corrupt JSON.
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  canSubmitLesson,
  getSubmitButtonConfig,
  getStatusBadgeClass,
  getStatusLabel
} from '../../../../frontend/js/pages/creator-lessons-page.js';
import {
  isLessonEditable,
  parseLessonDetail,
  parseRejectionFeedback
} from '../../../../frontend/js/pages/creator-lesson-editor-page.js';

describe('Unit: Creator Lesson Submission & Status Visibility (Task 9D.3)', () => {

  describe('1. Submission Eligibility (canSubmitLesson)', () => {
    test('allows submission for Draft lesson with 1 or more vocabularies', () => {
      const res1 = canSubmitLesson('Draft', 1);
      const res5 = canSubmitLesson('draft', 5);
      const res10 = canSubmitLesson('DRAFT', 10);

      assert.strictEqual(res1.canSubmit, true);
      assert.strictEqual(res5.canSubmit, true);
      assert.strictEqual(res10.canSubmit, true);
    });

    test('allows submission for Rejected lesson with 1 or more vocabularies', () => {
      const res1 = canSubmitLesson('Rejected', 1);
      const res3 = canSubmitLesson('rejected', 3);
      const resHuge = canSubmitLesson('REJECTED', 100);

      assert.strictEqual(res1.canSubmit, true);
      assert.strictEqual(res3.canSubmit, true);
      assert.strictEqual(resHuge.canSubmit, true);
    });

    test('rejects submission for Draft lesson with zero vocabularies', () => {
      const resZero = canSubmitLesson('Draft', 0);
      const resNegative = canSubmitLesson('Draft', -1);
      const resNull = canSubmitLesson('Draft', null);

      assert.strictEqual(resZero.canSubmit, false);
      assert.match(resZero.reason, /ít nhất một từ vựng/);
      assert.strictEqual(resNegative.canSubmit, false);
      assert.strictEqual(resNull.canSubmit, false);
    });

    test('rejects submission for Rejected lesson with zero vocabularies', () => {
      const resZero = canSubmitLesson('Rejected', 0);

      assert.strictEqual(resZero.canSubmit, false);
      assert.match(resZero.reason, /ít nhất một từ vựng/);
    });

    test('rejects submission for Pending lesson regardless of vocabulary count', () => {
      const resPending = canSubmitLesson('Pending', 10);
      const resPendingLower = canSubmitLesson('pending', 5);

      assert.strictEqual(resPending.canSubmit, false);
      assert.match(resPending.reason, /đang chờ kiểm duyệt/);
      assert.strictEqual(resPendingLower.canSubmit, false);
    });

    test('rejects submission for Approved lesson regardless of vocabulary count', () => {
      const resApproved = canSubmitLesson('Approved', 20);
      const resApprovedLower = canSubmitLesson('approved', 15);

      assert.strictEqual(resApproved.canSubmit, false);
      assert.match(resApproved.reason, /đã được phê duyệt/);
      assert.strictEqual(resApprovedLower.canSubmit, false);
    });

    test('rejects submission for unknown, null, or empty status', () => {
      const resEmpty = canSubmitLesson('', 5);
      const resNull = canSubmitLesson(null, 5);
      const resInvalid = canSubmitLesson('ARCHIVED', 5);

      assert.strictEqual(resEmpty.canSubmit, false);
      assert.strictEqual(resNull.canSubmit, false);
      assert.strictEqual(resInvalid.canSubmit, false);
    });
  });

  describe('2. Submit Button Presentation Config (getSubmitButtonConfig)', () => {
    test('returns "Nộp duyệt" config for Draft status', () => {
      const cfg = getSubmitButtonConfig('Draft');
      const cfgLower = getSubmitButtonConfig('draft');

      assert.ok(cfg);
      assert.strictEqual(cfg.label, 'Nộp duyệt');
      assert.strictEqual(cfg.actionText, 'Nộp kiểm duyệt');
      assert.match(cfg.className, /btn-chinese-primary/);

      assert.ok(cfgLower);
      assert.strictEqual(cfgLower.label, 'Nộp duyệt');
    });

    test('returns "Nộp lại" config for Rejected status', () => {
      const cfg = getSubmitButtonConfig('Rejected');
      const cfgLower = getSubmitButtonConfig('rejected');

      assert.ok(cfg);
      assert.strictEqual(cfg.label, 'Nộp lại');
      assert.strictEqual(cfg.actionText, 'Nộp lại kiểm duyệt');
      assert.match(cfg.className, /btn-chinese-primary/);

      assert.ok(cfgLower);
      assert.strictEqual(cfgLower.label, 'Nộp lại');
    });

    test('returns null for Pending and Approved status', () => {
      assert.strictEqual(getSubmitButtonConfig('Pending'), null);
      assert.strictEqual(getSubmitButtonConfig('pending'), null);
      assert.strictEqual(getSubmitButtonConfig('Approved'), null);
      assert.strictEqual(getSubmitButtonConfig('approved'), null);
    });

    test('returns null for empty, null, or invalid status', () => {
      assert.strictEqual(getSubmitButtonConfig(''), null);
      assert.strictEqual(getSubmitButtonConfig(null), null);
      assert.strictEqual(getSubmitButtonConfig('UNKNOWN'), null);
    });
  });

  describe('3. Lifecycle Locking Contract (isLessonEditable)', () => {
    test('treats Draft and Rejected as editable', () => {
      assert.strictEqual(isLessonEditable('Draft'), true);
      assert.strictEqual(isLessonEditable('draft'), true);
      assert.strictEqual(isLessonEditable('Rejected'), true);
      assert.strictEqual(isLessonEditable('rejected'), true);
    });

    test('treats Pending and Approved as locked (read-only)', () => {
      assert.strictEqual(isLessonEditable('Pending'), false);
      assert.strictEqual(isLessonEditable('pending'), false);
      assert.strictEqual(isLessonEditable('Approved'), false);
      assert.strictEqual(isLessonEditable('approved'), false);
    });

    test('treats invalid, empty, or null as locked', () => {
      assert.strictEqual(isLessonEditable(''), false);
      assert.strictEqual(isLessonEditable(null), false);
      assert.strictEqual(isLessonEditable(undefined), false);
      assert.strictEqual(isLessonEditable('ARCHIVED'), false);
    });
  });

  describe('4. Status Badges & Labels', () => {
    test('maps all 4 statuses to correct CSS classes and labels', () => {
      assert.strictEqual(getStatusBadgeClass('Draft'), 'badge-status badge-status-draft');
      assert.strictEqual(getStatusLabel('Draft'), 'Bản nháp');

      assert.strictEqual(getStatusBadgeClass('Pending'), 'badge-status badge-status-pending');
      assert.strictEqual(getStatusLabel('Pending'), 'Chờ duyệt');

      assert.strictEqual(getStatusBadgeClass('Approved'), 'badge-status badge-status-approved');
      assert.strictEqual(getStatusLabel('Approved'), 'Đã duyệt');

      assert.strictEqual(getStatusBadgeClass('Rejected'), 'badge-status badge-status-rejected');
      assert.strictEqual(getStatusLabel('Rejected'), 'Từ chối');
    });
  });

  describe('5. Rejection Feedback & Contract Gap Handling (parseRejectionFeedback)', () => {
    test('returns no feedback for non-rejected lessons', () => {
      const draft = parseRejectionFeedback({ status: 'Draft' });
      assert.strictEqual(draft.hasFeedback, false);
      assert.strictEqual(draft.isContractBlocked, false);
      assert.strictEqual(draft.rejectionReason, '');

      const pending = parseRejectionFeedback({ status: 'Pending' });
      assert.strictEqual(pending.hasFeedback, false);
      assert.strictEqual(pending.isContractBlocked, false);

      const approved = parseRejectionFeedback({ status: 'Approved' });
      assert.strictEqual(approved.hasFeedback, false);
      assert.strictEqual(approved.isContractBlocked, false);
    });

    test('detects contract gap when Rejected lesson lacks rejectionReason', () => {
      const sealedLesson = {
        lessonId: 10,
        title: 'Bài học kiểm duyệt',
        status: 'Rejected',
        vocabularyCount: 2
      };

      const result = parseRejectionFeedback(sealedLesson);

      assert.strictEqual(result.hasFeedback, false);
      assert.strictEqual(result.isContractBlocked, true);
      assert.strictEqual(result.rejectionReason, '');
      assert.deepStrictEqual(result.flaggedFields, []);
      assert.strictEqual(
        result.displayMessage,
        'Bài học này đã bị từ chối. Thông tin phản hồi chi tiết chưa được cung cấp bởi API hiện tại.'
      );
    });

    test('parses real rejectionReason and flaggedFields when present (forward-compatibility)', () => {
      const mockBackendLesson = {
        lessonId: 20,
        title: 'Bài học bị từ chối có feedback',
        status: 'Rejected',
        rejectionReason: 'Nghĩa Hán Việt ở từ 2 không phù hợp với ngữ cảnh HSK 1',
        flaggedFields: ['vocabularies[1].meaningHanViet', 'title']
      };

      const result = parseRejectionFeedback(mockBackendLesson);

      assert.strictEqual(result.hasFeedback, true);
      assert.strictEqual(result.isContractBlocked, false);
      assert.strictEqual(result.rejectionReason, 'Nghĩa Hán Việt ở từ 2 không phù hợp với ngữ cảnh HSK 1');
      assert.deepStrictEqual(result.flaggedFields, ['vocabularies[1].meaningHanViet', 'title']);
      assert.strictEqual(result.displayMessage, 'Nghĩa Hán Việt ở từ 2 không phù hợp với ngữ cảnh HSK 1');
    });

    test('safely parses JSON string flaggedFields without eval', () => {
      const lessonWithJsonFields = {
        status: 'Rejected',
        rejectionReason: 'Lỗi định dạng',
        flaggedFields: '["title", "vocabularies[0].pinyin"]'
      };

      const result = parseRejectionFeedback(lessonWithJsonFields);

      assert.strictEqual(result.hasFeedback, true);
      assert.deepStrictEqual(result.flaggedFields, ['title', 'vocabularies[0].pinyin']);
    });

    test('safely falls back to comma-separated string flaggedFields', () => {
      const lessonWithCsvFields = {
        status: 'Rejected',
        rejectionReason: 'Lỗi định dạng',
        flaggedFields: 'title, vocabularies[0].pinyin'
      };

      const result = parseRejectionFeedback(lessonWithCsvFields);

      assert.strictEqual(result.hasFeedback, true);
      assert.deepStrictEqual(result.flaggedFields, ['title', 'vocabularies[0].pinyin']);
    });
  });

  describe('6. Authoritative Lesson Detail Parsing with Submission Attributes', () => {
    test('extracts status, vocabularies, and rejection attributes from envelope', () => {
      const raw = {
        data: {
          lessonId: 99,
          title: 'Bài 99: Kiểm thử nộp bài',
          status: 'Draft',
          vocabularyCount: 2,
          vocabularies: [
            { vocabId: 101, hanzi: '水', orderIndex: 1 },
            { vocabId: 102, hanzi: '火', orderIndex: 2 }
          ],
          createdAt: '2026-09-15T00:00:00Z',
          updatedAt: '2026-09-15T01:00:00Z'
        }
      };

      const parsed = parseLessonDetail(raw);

      assert.ok(parsed);
      assert.strictEqual(parsed.lessonId, 99);
      assert.strictEqual(parsed.title, 'Bài 99: Kiểm thử nộp bài');
      assert.strictEqual(parsed.status, 'Draft');
      assert.strictEqual(parsed.vocabularyCount, 2);
      assert.strictEqual(parsed.vocabularies.length, 2);
      assert.strictEqual(parsed.rejectionReason, '');
      assert.deepStrictEqual(parsed.flaggedFields, []);
    });
  });

});
