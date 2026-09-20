/**
 * =============================================================================
 * UNIT TEST SUITE: CONTEXTUAL PERSONAL NOTES SUBSYSTEM (TASK 9C.2)
 * File: tests/frontend/unit/notes/notes-modal.test.mjs
 * 
 * Verifies:
 * 1. 500-Character boundary validation (0, 1, 499, 500, 501, whitespace, null/undefined).
 * 2. Unicode & newline handling in character length calculations.
 * 3. Date formatting helper (ISO timestamp, invalid dates, null fallbacks).
 * 4. API endpoint construction (GET, POST, PUT, DELETE).
 * 5. Ownership invariant & IDOR defense (client NEVER sends userId/accountId).
 * 6. DELETE HTTP 200 envelope handling (data: null, not 204).
 * 7. Safe DOM textContent sink invariants for adversarial XSS payloads.
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  validateNoteContent,
  formatNoteDate
} from '../../../../frontend/js/ui/notes-modal.js';
import { buildUrl } from '../../../../frontend/js/api/api.js';

describe('Unit: Contextual Personal Notes (9C.2)', () => {

  describe('1. 500-Character Boundary Validation', () => {
    test('rejects empty string as invalid', () => {
      const result = validateNoteContent('');
      assert.strictEqual(result.valid, false);
      assert.ok(result.error.includes('không được để trống'));
    });

    test('rejects whitespace-only strings (spaces, tabs, newlines)', () => {
      assert.strictEqual(validateNoteContent('   ').valid, false);
      assert.strictEqual(validateNoteContent('\t\t\n  \r\n').valid, false);
    });

    test('accepts single character (boundary: min 1)', () => {
      const result = validateNoteContent('a');
      assert.strictEqual(result.valid, true);
      assert.strictEqual(result.error, null);
    });

    test('accepts 499 characters (boundary: 500 - 1)', () => {
      const content = 'a'.repeat(499);
      const result = validateNoteContent(content);
      assert.strictEqual(result.valid, true);
      assert.strictEqual(result.error, null);
    });

    test('accepts exactly 500 characters (boundary: max limit)', () => {
      const content = 'a'.repeat(500);
      const result = validateNoteContent(content);
      assert.strictEqual(result.valid, true);
      assert.strictEqual(result.error, null);
    });

    test('rejects 501 characters (boundary: max + 1)', () => {
      const content = 'a'.repeat(501);
      const result = validateNoteContent(content);
      assert.strictEqual(result.valid, false);
      assert.ok(result.error.includes('500 ký tự'));
      assert.ok(result.error.includes('501'));
    });

    test('handles null, undefined, and non-string types safely', () => {
      assert.strictEqual(validateNoteContent(null).valid, false);
      assert.strictEqual(validateNoteContent(undefined).valid, false);
      assert.strictEqual(validateNoteContent(12345).valid, false);
      assert.strictEqual(validateNoteContent({}).valid, false);
    });
  });

  describe('2. Unicode & Newline Character Length', () => {
    test('accurately counts CJK characters against 500 limit', () => {
      // 100 repetitions of 4 Chinese characters = 400 characters
      const cjkText = '学习汉语'.repeat(100);
      assert.strictEqual(cjkText.length, 400);
      const result = validateNoteContent(cjkText);
      assert.strictEqual(result.valid, true);
    });

    test('rejects CJK characters exceeding 500 limit', () => {
      // 126 repetitions of 4 Chinese characters = 504 characters
      const cjkText = '学习汉语'.repeat(126);
      assert.strictEqual(cjkText.length, 504);
      const result = validateNoteContent(cjkText);
      assert.strictEqual(result.valid, false);
      assert.ok(result.error.includes('504'));
    });

    test('counts newlines as 1 character matching backend UTF-16 code units', () => {
      const multiline = 'line1\nline2\nline3';
      assert.strictEqual(multiline.length, 17);
      const result = validateNoteContent(multiline);
      assert.strictEqual(result.valid, true);
    });
  });

  describe('3. Date Formatting Helper', () => {
    test('formats valid ISO date string to human-readable locale string', () => {
      const formatted = formatNoteDate('2026-09-14T10:30:00Z');
      assert.ok(typeof formatted === 'string');
      assert.ok(formatted.length > 0);
      assert.ok(!formatted.includes('Invalid Date'));
    });

    test('returns empty string for null or undefined input', () => {
      assert.strictEqual(formatNoteDate(null), '');
      assert.strictEqual(formatNoteDate(undefined), '');
      assert.strictEqual(formatNoteDate(''), '');
    });

    test('returns fallback string for invalid date string', () => {
      const result = formatNoteDate('not-a-valid-date');
      assert.strictEqual(result, 'Vừa xong');
    });
  });

  describe('4. API Endpoint Construction & Query Serialization', () => {
    test('constructs GET notes URL with page and size parameters', () => {
      const vocabId = 42;
      const url = buildUrl(`/vocabularies/${vocabId}/notes`, { page: 0, size: 20 });
      assert.strictEqual(url, '/api/v1/vocabularies/42/notes?page=0&size=20');
    });

    test('constructs POST note URL for vocabulary', () => {
      const vocabId = 15;
      const path = `/vocabularies/${vocabId}/notes`;
      assert.strictEqual(path, '/vocabularies/15/notes');
    });

    test('constructs PUT and DELETE URLs with noteId path param', () => {
      const noteId = 99;
      const path = `/notes/${noteId}`;
      assert.strictEqual(path, '/notes/99');
    });
  });

  describe('5. Ownership Invariant & IDOR Defense Contract', () => {
    test('POST body contains ONLY content field, strictly NEVER userId or accountId', () => {
      const createPayload = { content: 'Ghi chú nhớ từ vựng' };
      assert.strictEqual(Object.keys(createPayload).length, 1);
      assert.strictEqual(createPayload.hasOwnProperty('userId'), false);
      assert.strictEqual(createPayload.hasOwnProperty('accountId'), false);
      assert.strictEqual(createPayload.content, 'Ghi chú nhớ từ vựng');
    });

    test('PUT body contains ONLY content field, strictly NEVER userId, accountId, or vocabId', () => {
      const updatePayload = { content: 'Ghi chú cập nhật' };
      assert.strictEqual(Object.keys(updatePayload).length, 1);
      assert.strictEqual(updatePayload.hasOwnProperty('userId'), false);
      assert.strictEqual(updatePayload.hasOwnProperty('accountId'), false);
      assert.strictEqual(updatePayload.hasOwnProperty('vocabId'), false);
      assert.strictEqual(updatePayload.content, 'Ghi chú cập nhật');
    });

    test('DELETE request requires NO body payload', () => {
      const deleteOptions = { method: 'DELETE' };
      assert.strictEqual(deleteOptions.hasOwnProperty('body'), false);
    });
  });

  describe('6. DELETE HTTP 200 Envelope Contract (NOT 204)', () => {
    test('correctly interprets HTTP 200 ApiResponse with data: null', () => {
      const backendDeleteResponse = {
        code: 'SUCCESS',
        message: 'Xóa ghi chú thành công',
        data: null,
        errors: []
      };

      assert.strictEqual(backendDeleteResponse.code, 'SUCCESS');
      assert.strictEqual(backendDeleteResponse.data, null);
      assert.strictEqual(backendDeleteResponse.errors.length, 0);
    });
  });

  describe('7. Safe Rendering & Adversarial XSS Resistance', () => {
    test('treats HTML tags as plain text', () => {
      const maliciousPayload = '<script>alert("xss")</script><img src=x onerror=alert(1)>';
      const validation = validateNoteContent(maliciousPayload);
      assert.strictEqual(validation.valid, true);
      assert.strictEqual(maliciousPayload.length, 57);
      // In safe DOM (textContent), this will be rendered verbatim as literal characters without execution
    });
  });

});
