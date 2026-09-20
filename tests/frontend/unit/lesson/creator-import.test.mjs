/**
 * =============================================================================
 * UNIT TEST SUITE: CREATOR TWO-STEP EXCEL IMPORT (TASK 9D.2)
 * File: tests/frontend/unit/lesson/creator-import.test.mjs
 * 
 * Verifies:
 * - File validation: .xlsx extension, non-empty, 10MB upper bound.
 * - Title validation: 1..100 trimmed character limit (CreatorLessonServiceImpl).
 * - File size formatting: human-readable units.
 * - Validation report parsing: normalization & safe defaults.
 * - Row filtering: all, errors, new, existing.
 * - Row pagination: bounded slicing for DOM safety (up to 5,000 rows).
 * - FormData construction: part 'file' for preview; parts 'title' & 'file' for confirm.
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  validateExcelFile,
  validateImportTitle,
  formatFileSize,
  parseValidationReport,
  filterReportRows,
  paginateReportRows,
  buildPreviewFormData,
  buildConfirmFormData,
  MAX_FILE_SIZE_BYTES,
  MAX_IMPORT_TITLE_LENGTH
} from '../../../../frontend/js/pages/creator-import-page.js';

describe('Unit: Creator Two-Step Excel Import UI (Task 9D.2)', () => {

  describe('1. File Pre-Validation (validateExcelFile)', () => {
    test('rejects null, undefined, or missing file', () => {
      const rNull = validateExcelFile(null);
      const rUndef = validateExcelFile(undefined);

      assert.strictEqual(rNull.valid, false);
      assert.strictEqual(rUndef.valid, false);
      assert.match(rNull.error, /chọn một tệp Excel/);
    });

    test('rejects non-.xlsx extensions (.xls, .csv, .txt, .pdf)', () => {
      const badExtensions = ['vocab.xls', 'words.csv', 'data.txt', 'sheet.pdf', 'script.exe'];

      for (const name of badExtensions) {
        const res = validateExcelFile({ name, size: 1024 });
        assert.strictEqual(res.valid, false, `Expected ${name} to be rejected`);
        assert.match(res.error, /\.xlsx/);
      }
    });

    test('accepts .xlsx extension case-insensitively (.XLSX, .Xlsx)', () => {
      const rUpper = validateExcelFile({ name: 'VOCAB.XLSX', size: 1024 });
      const rMixed = validateExcelFile({ name: 'Lessons_2026.Xlsx', size: 2048 });

      assert.strictEqual(rUpper.valid, true);
      assert.strictEqual(rMixed.valid, true);
    });

    test('rejects empty file (0 bytes)', () => {
      const res = validateExcelFile({ name: 'empty.xlsx', size: 0 });

      assert.strictEqual(res.valid, false);
      assert.match(res.error, /0 bytes|rỗng/);
    });

    test('rejects oversized file (> 10 MB)', () => {
      const oversized = validateExcelFile({
        name: 'large.xlsx',
        size: MAX_FILE_SIZE_BYTES + 1
      });

      assert.strictEqual(oversized.valid, false);
      assert.match(oversized.error, /10 MB/);
    });

    test('accepts valid file <= 10 MB', () => {
      const validSmall = validateExcelFile({ name: 'hsk1.xlsx', size: 25 * 1024 });
      const validBoundary = validateExcelFile({ name: 'max.xlsx', size: MAX_FILE_SIZE_BYTES });

      assert.strictEqual(validSmall.valid, true);
      assert.strictEqual(validBoundary.valid, true);
    });
  });

  describe('2. Import Title Validation (validateImportTitle)', () => {
    test('rejects null, undefined, and empty string', () => {
      const rNull = validateImportTitle(null);
      const rUndef = validateImportTitle(undefined);
      const rEmpty = validateImportTitle('');

      assert.strictEqual(rNull.valid, false);
      assert.strictEqual(rUndef.valid, false);
      assert.strictEqual(rEmpty.valid, false);
      assert.match(rEmpty.error, /không được để trống/);
    });

    test('rejects whitespace-only titles', () => {
      const rSpaces = validateImportTitle('      ');
      const rTabs = validateImportTitle('\t  \n  ');

      assert.strictEqual(rSpaces.valid, false);
      assert.strictEqual(rTabs.valid, false);
      assert.match(rSpaces.error, /không được để trống/);
    });

    test('rejects titles exceeding 100 characters (authoritative backend limit)', () => {
      const title101 = 'A'.repeat(101);
      const res = validateImportTitle(title101);

      assert.strictEqual(res.valid, false);
      assert.match(res.error, /100 ký tự/);
    });

    test('accepts titles of exactly 100 characters', () => {
      const title100 = 'B'.repeat(MAX_IMPORT_TITLE_LENGTH);
      const res = validateImportTitle(title100);

      assert.strictEqual(res.valid, true);
      assert.strictEqual(res.sanitizedTitle.length, 100);
    });

    test('trims surrounding whitespace from valid titles', () => {
      const res = validateImportTitle('   HSK 1 — Bài 1: Chào hỏi   ');

      assert.strictEqual(res.valid, true);
      assert.strictEqual(res.sanitizedTitle, 'HSK 1 — Bài 1: Chào hỏi');
    });
  });

  describe('3. File Size Formatting (formatFileSize)', () => {
    test('formats zero, bytes, KB, and MB correctly', () => {
      assert.strictEqual(formatFileSize(0), '0 B');
      assert.strictEqual(formatFileSize(512), '512 B');
      assert.strictEqual(formatFileSize(1024), '1.0 KB');
      assert.strictEqual(formatFileSize(25600), '25.0 KB');
      assert.strictEqual(formatFileSize(1048576), '1.00 MB');
      assert.strictEqual(formatFileSize(10485760), '10.00 MB');
      assert.strictEqual(formatFileSize(-10), '0 B');
      assert.strictEqual(formatFileSize(null), '0 B');
    });
  });

  describe('4. Validation Report Normalization (parseValidationReport)', () => {
    test('parses full valid report correctly', () => {
      const raw = {
        code: 'SUCCESS',
        data: {
          isValid: true,
          totalRows: 5,
          validRowsCount: 5,
          invalidRowsCount: 0,
          newVocabCount: 3,
          existingVocabCount: 2,
          fileStatus: 'VALID',
          summaryMessage: 'Tệp hoàn toàn hợp lệ',
          rows: [
            { rowNumber: 2, hanzi: '中', pinyin: 'zhōng', isExisting: true, isValid: true, errors: [] },
            { rowNumber: 3, hanzi: '文', pinyin: 'wén', isExisting: false, isValid: true, errors: [] }
          ],
          errors: []
        }
      };

      const parsed = parseValidationReport(raw);

      assert.strictEqual(parsed.isValid, true);
      assert.strictEqual(parsed.totalRows, 5);
      assert.strictEqual(parsed.validRowsCount, 5);
      assert.strictEqual(parsed.invalidRowsCount, 0);
      assert.strictEqual(parsed.newVocabCount, 3);
      assert.strictEqual(parsed.existingVocabCount, 2);
      assert.strictEqual(parsed.fileStatus, 'VALID');
      assert.strictEqual(parsed.rows.length, 2);
    });

    test('handles empty or null report safely with defaults', () => {
      const parsedNull = parseValidationReport(null);
      const parsedEmpty = parseValidationReport({});

      assert.strictEqual(parsedNull.isValid, false);
      assert.strictEqual(parsedNull.totalRows, 0);
      assert.strictEqual(parsedNull.rows.length, 0);
      assert.strictEqual(parsedNull.errors.length, 0);
      assert.strictEqual(parsedEmpty.fileStatus, 'INVALID');
    });
  });

  describe('5. Row Filtering (filterReportRows)', () => {
    const sampleRows = [
      { rowNumber: 2, hanzi: '学', isExisting: false, isValid: true, errors: [] },
      { rowNumber: 3, hanzi: '习', isExisting: true, isValid: true, errors: [] },
      { rowNumber: 4, hanzi: '', isExisting: false, isValid: false, errors: ['Chữ Hán không được để trống'] },
      { rowNumber: 5, hanzi: '大', isExisting: true, isValid: true, errors: [] }
    ];

    test('returns all rows for mode "all"', () => {
      const filtered = filterReportRows(sampleRows, 'all');
      assert.strictEqual(filtered.length, 4);
    });

    test('returns only invalid rows for mode "errors"', () => {
      const filtered = filterReportRows(sampleRows, 'errors');
      assert.strictEqual(filtered.length, 1);
      assert.strictEqual(filtered[0].rowNumber, 4);
    });

    test('returns only new vocabularies for mode "new"', () => {
      const filtered = filterReportRows(sampleRows, 'new');
      assert.strictEqual(filtered.length, 2);
      assert.deepStrictEqual(filtered.map(r => r.rowNumber), [2, 4]);
    });

    test('returns only existing vocabularies for mode "existing"', () => {
      const filtered = filterReportRows(sampleRows, 'existing');
      assert.strictEqual(filtered.length, 2);
      assert.deepStrictEqual(filtered.map(r => r.rowNumber), [3, 5]);
    });
  });

  describe('6. Bounded Row Pagination (paginateReportRows)', () => {
    const rows = Array.from({ length: 55 }, (_, i) => ({ rowNumber: i + 2, hanzi: `字${i}` }));

    test('slices page 1 correctly with pageSize 25', () => {
      const p1 = paginateReportRows(rows, 1, 25);
      assert.strictEqual(p1.items.length, 25);
      assert.strictEqual(p1.totalPages, 3);
      assert.strictEqual(p1.currentPage, 1);
      assert.strictEqual(p1.totalItems, 55);
      assert.strictEqual(p1.items[0].rowNumber, 2);
      assert.strictEqual(p1.items[24].rowNumber, 26);
    });

    test('slices page 2 correctly', () => {
      const p2 = paginateReportRows(rows, 2, 25);
      assert.strictEqual(p2.items.length, 25);
      assert.strictEqual(p2.currentPage, 2);
      assert.strictEqual(p2.items[0].rowNumber, 27);
    });

    test('slices page 3 (remaining items) correctly', () => {
      const p3 = paginateReportRows(rows, 3, 25);
      assert.strictEqual(p3.items.length, 5);
      assert.strictEqual(p3.currentPage, 3);
      assert.strictEqual(p3.items[4].rowNumber, 56);
    });

    test('clamps out-of-bounds page requests safely', () => {
      const pOver = paginateReportRows(rows, 999, 25);
      assert.strictEqual(pOver.currentPage, 3);

      const pUnder = paginateReportRows(rows, -5, 25);
      assert.strictEqual(pUnder.currentPage, 1);
    });
  });

  describe('7. FormData Builders (buildPreviewFormData & buildConfirmFormData)', () => {
    test('buildPreviewFormData creates FormData with part "file"', () => {
      const fakeFile = new File(['dummy'], 'vocab.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const fd = buildPreviewFormData(fakeFile);

      assert.ok(fd instanceof FormData);
      assert.strictEqual(fd.get('file')?.name, 'vocab.xlsx');
      assert.strictEqual(fd.has('title'), false);
    });

    test('buildConfirmFormData creates FormData with trimmed "title" and "file"', () => {
      const fakeFile = new File(['dummy'], 'vocab.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const fd = buildConfirmFormData('  HSK 1 Bài 1  ', fakeFile);

      assert.ok(fd instanceof FormData);
      assert.strictEqual(fd.get('title'), 'HSK 1 Bài 1');
      assert.strictEqual(fd.get('file')?.name, 'vocab.xlsx');
      assert.strictEqual(fd.has('lessonId'), false);
      assert.strictEqual(fd.has('report'), false);
    });
  });

});
