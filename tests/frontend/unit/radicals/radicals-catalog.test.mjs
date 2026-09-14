/**
 * =============================================================================
 * UNIT TEST SUITE: KANGXI RADICALS CATALOG & LOGIC ENGINE (TASK 9B.2)
 * File: tests/frontend/unit/radicals/radicals-catalog.test.mjs
 * 
 * Verifies:
 * - Dataset validation & integrity invariants (1..214 sequential, duplicate/missing detection).
 * - Full-catalog client-side search filtering (character, Hán-Việt, Vietnamese meaning).
 * - Search normalization & Vietnamese diacritic handling.
 * - Pagination boundary calculations, slice ranges, and clamping.
 * - Media URL sanitization (audio/video) and rejection of unsafe schemes.
 * - Adversarial payload resistance (XSS vectors, null/undefined/missing fields).
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  validateRadicalDataset,
  filterRadicals,
  calculatePagination,
  removeVietnameseDiacritics,
  normalizeSearchQuery
} from '../../../../frontend/js/pages/radicals-page.js';
import { sanitizeResourceUrl } from '../../../../frontend/js/ui/security.js';

// Generator helper for synthetic radical fixture
function generateMockRadicals(count = 214) {
  const radicals = [];
  for (let i = 1; i <= count; i++) {
    radicals.push({
      radicalId: i,
      character: String.fromCodePoint(0x4E00 + i - 1),
      pinyin: `pinyin_${i}`,
      meaningHanViet: `HanViet_${i}`,
      meaningVi: `Nghĩa tiếng Việt số ${i}`,
      audioUrl: i % 2 === 0 ? `https://cdn.example.com/audio/rad_${i}.mp3` : null,
      videoWritingUrl: i % 3 === 0 ? `https://cdn.example.com/video/rad_${i}.mp4` : null
    });
  }
  return radicals;
}

describe('Unit: Kangxi Radicals Catalog & Presentation Logic', () => {

  describe('1. Dataset Invariant & Integrity Validation (validateRadicalDataset)', () => {
    test('passes valid full 214-radical sequential catalog', () => {
      const mock214 = generateMockRadicals(214);
      const result = validateRadicalDataset(mock214, 214);
      assert.strictEqual(result.valid, true);
      assert.strictEqual(result.errors.length, 0);
    });

    test('detects count mismatch when dataset has fewer than 214 items', () => {
      const partial = generateMockRadicals(200);
      const result = validateRadicalDataset(partial, 214);
      assert.strictEqual(result.valid, false);
      assert.ok(result.errors.some(e => e.includes('Số lượng bộ thủ (200) không khớp')));
    });

    test('detects duplicate radical IDs in dataset', () => {
      const mock = generateMockRadicals(214);
      // Introduce duplicate ID
      mock[10].radicalId = 1;
      const result = validateRadicalDataset(mock, 214);
      assert.strictEqual(result.valid, false);
      assert.ok(result.errors.some(e => e.includes('bị trùng lặp')));
    });

    test('detects missing radical ID in sequence (e.g. gap at ID 50)', () => {
      const mock = generateMockRadicals(214);
      mock[49].radicalId = 999; // corrupt ID 50
      const result = validateRadicalDataset(mock, 214);
      assert.strictEqual(result.valid, false);
      assert.ok(result.errors.some(e => e.includes('nằm ngoài phạm vi') || e.includes('Thiếu')));
    });

    test('rejects non-array or null input gracefully', () => {
      assert.strictEqual(validateRadicalDataset(null, 214).valid, false);
      assert.strictEqual(validateRadicalDataset(undefined, 214).valid, false);
      assert.strictEqual(validateRadicalDataset({}, 214).valid, false);
      assert.strictEqual(validateRadicalDataset('invalid', 214).valid, false);
    });

    test('detects negative or non-integer IDs', () => {
      const invalid = [
        { radicalId: -1, character: '一' },
        { radicalId: 'abc', character: '丨' },
        { radicalId: 0, character: '丶' }
      ];
      const result = validateRadicalDataset(invalid, 3);
      assert.strictEqual(result.valid, false);
      assert.ok(result.errors.length > 0);
    });
  });

  describe('2. Client-Side Search Engine (filterRadicals & Normalization)', () => {
    const sampleRadicals = [
      { radicalId: 1, character: '一', pinyin: 'yī', meaningHanViet: 'Nhất', meaningVi: 'Số một' },
      { radicalId: 30, character: '口', pinyin: 'kǒu', meaningHanViet: 'Khẩu', meaningVi: 'Cái miệng' },
      { radicalId: 75, character: '木', pinyin: 'mù', meaningHanViet: 'Mộc', meaningVi: 'Cây cối, gỗ' },
      { radicalId: 85, character: '水', pinyin: 'shuǐ', meaningHanViet: 'Thủy', meaningVi: 'Nước, dòng nước' },
      { radicalId: 86, character: '火', pinyin: 'huǒ', meaningHanViet: 'Hỏa', meaningVi: 'Lửa, ngọn lửa' }
    ];

    test('returns full dataset on empty or whitespace query', () => {
      assert.strictEqual(filterRadicals('', sampleRadicals).length, 5);
      assert.strictEqual(filterRadicals('   ', sampleRadicals).length, 5);
      assert.strictEqual(filterRadicals(null, sampleRadicals).length, 5);
      assert.strictEqual(filterRadicals(undefined, sampleRadicals).length, 5);
    });

    test('filters accurately by Chinese character glyph', () => {
      const result = filterRadicals('木', sampleRadicals);
      assert.strictEqual(result.length, 1);
      assert.strictEqual(result[0].radicalId, 75);
      assert.strictEqual(result[0].character, '木');
    });

    test('filters accurately by Hán-Việt reading with case-insensitivity', () => {
      const resultLower = filterRadicals('khẩu', sampleRadicals);
      assert.strictEqual(resultLower.length, 1);
      assert.strictEqual(resultLower[0].radicalId, 30);

      const resultUpper = filterRadicals('KHẨU', sampleRadicals);
      assert.strictEqual(resultUpper.length, 1);
      assert.strictEqual(resultUpper[0].radicalId, 30);
    });

    test('filters accurately by Vietnamese meaning', () => {
      const result = filterRadicals('dòng nước', sampleRadicals);
      assert.strictEqual(result.length, 1);
      assert.strictEqual(result[0].radicalId, 85);
      assert.strictEqual(result[0].meaningHanViet, 'Thủy');
    });

    test('supports accent-insensitive Vietnamese fuzzy search', () => {
      // Searching "moc" matches "Mộc"
      const resultMoc = filterRadicals('moc', sampleRadicals);
      assert.strictEqual(resultMoc.length, 1);
      assert.strictEqual(resultMoc[0].meaningHanViet, 'Mộc');

      // Searching "nuoc" matches "dòng nước"
      const resultNuoc = filterRadicals('nuoc', sampleRadicals);
      assert.strictEqual(resultNuoc.length, 1);
      assert.strictEqual(resultNuoc[0].meaningHanViet, 'Thủy');

      // Searching "nhat" matches "Nhất"
      const resultNhat = filterRadicals('nhat', sampleRadicals);
      assert.strictEqual(resultNhat.length, 1);
      assert.strictEqual(resultNhat[0].meaningHanViet, 'Nhất');
    });

    test('returns empty array when no radical matches', () => {
      const result = filterRadicals('từ khóa hoàn toàn không tồn tại', sampleRadicals);
      assert.strictEqual(result.length, 0);
    });

    test('removeVietnameseDiacritics helper preserves Chinese and Latin without accents', () => {
      assert.strictEqual(removeVietnameseDiacritics('Mộc'), 'Moc');
      assert.strictEqual(removeVietnameseDiacritics('Nước'), 'Nuoc');
      assert.strictEqual(removeVietnameseDiacritics('Đại'), 'Dai');
      assert.strictEqual(removeVietnameseDiacritics('木'), '木');
      assert.strictEqual(removeVietnameseDiacritics('Hello'), 'Hello');
      assert.strictEqual(removeVietnameseDiacritics(''), '');
      assert.strictEqual(removeVietnameseDiacritics(null), '');
    });

    test('normalizeSearchQuery trims and lowercases reliably', () => {
      assert.strictEqual(normalizeSearchQuery('  ThỦy  '), 'thủy');
      assert.strictEqual(normalizeSearchQuery(null), '');
      assert.strictEqual(normalizeSearchQuery(undefined), '');
      assert.strictEqual(normalizeSearchQuery(123), '123');
    });
  });

  describe('3. Pagination Calculation & Slicing (calculatePagination)', () => {
    test('calculates first page bounds correctly', () => {
      const pag = calculatePagination(214, 0, 24);
      assert.strictEqual(pag.pageIndex, 0);
      assert.strictEqual(pag.pageSize, 24);
      assert.strictEqual(pag.totalPages, 9); // ceil(214 / 24) = 9
      assert.strictEqual(pag.startIndex, 0);
      assert.strictEqual(pag.endIndex, 24);
    });

    test('calculates middle page bounds correctly', () => {
      const pag = calculatePagination(214, 4, 24);
      assert.strictEqual(pag.pageIndex, 4);
      assert.strictEqual(pag.startIndex, 96);
      assert.strictEqual(pag.endIndex, 120);
    });

    test('calculates last page bounds correctly with remainder', () => {
      const pag = calculatePagination(214, 8, 24);
      assert.strictEqual(pag.pageIndex, 8);
      assert.strictEqual(pag.startIndex, 192);
      assert.strictEqual(pag.endIndex, 214);
    });

    test('calculates full view (pageSize=214) in single page', () => {
      const pag = calculatePagination(214, 0, 214);
      assert.strictEqual(pag.pageIndex, 0);
      assert.strictEqual(pag.totalPages, 1);
      assert.strictEqual(pag.startIndex, 0);
      assert.strictEqual(pag.endIndex, 214);
    });

    test('clamps out-of-bounds page index to valid range', () => {
      // Requested page 99 when only 9 pages exist
      const pagOver = calculatePagination(214, 99, 24);
      assert.strictEqual(pagOver.pageIndex, 8);

      // Negative page index
      const pagUnder = calculatePagination(214, -5, 24);
      assert.strictEqual(pagUnder.pageIndex, 0);
    });

    test('handles zero total elements safely', () => {
      const pagEmpty = calculatePagination(0, 0, 24);
      assert.strictEqual(pagEmpty.pageIndex, 0);
      assert.strictEqual(pagEmpty.totalPages, 1);
      assert.strictEqual(pagEmpty.startIndex, 0);
      assert.strictEqual(pagEmpty.endIndex, 0);
    });
  });

  describe('4. Media Resource Sanitization for Radicals (sanitizeResourceUrl)', () => {
    test('accepts valid HTTPS audio and video URLs', () => {
      assert.strictEqual(
        sanitizeResourceUrl('https://cdn.example.com/audio/rad_1.mp3'),
        'https://cdn.example.com/audio/rad_1.mp3'
      );
      assert.strictEqual(
        sanitizeResourceUrl('https://cdn.example.com/video/rad_1.mp4'),
        'https://cdn.example.com/video/rad_1.mp4'
      );
    });

    test('accepts valid safe relative paths', () => {
      assert.strictEqual(sanitizeResourceUrl('/media/rad_1.mp3'), '/media/rad_1.mp3');
      assert.strictEqual(sanitizeResourceUrl('./assets/video.mp4'), './assets/video.mp4');
    });

    test('rejects malicious javascript: scheme in media URLs', () => {
      assert.strictEqual(sanitizeResourceUrl('javascript:alert(1)'), 'about:blank');
    });

    test('rejects data: schemes for resource audio/video sinks', () => {
      assert.strictEqual(sanitizeResourceUrl('data:text/html,<script>alert(1)</script>'), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl('data:audio/mp3;base64,AAAA'), 'about:blank');
    });

    test('rejects protocol-relative URLs (//open-redirect)', () => {
      assert.strictEqual(sanitizeResourceUrl('//malicious.com/audio.mp3'), 'about:blank');
    });

    test('handles null, undefined, and empty string safely', () => {
      assert.strictEqual(sanitizeResourceUrl(null), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl(undefined), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl(''), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl('   '), 'about:blank');
    });
  });

  describe('5. Adversarial Input Resiliency (XSS & Corrupt Structures)', () => {
    test('safely filters when radical records contain script tags in character or meanings', () => {
      const adversarialItems = [
        {
          radicalId: 1,
          character: '<script>alert("xss")</script>',
          pinyin: 'yī',
          meaningHanViet: '<img src=x onerror=alert(1)>',
          meaningVi: 'javascript:alert(1)'
        },
        {
          radicalId: 2,
          character: '丨',
          pinyin: 'gǔn',
          meaningHanViet: 'Cổn',
          meaningVi: 'Nét sổ'
        }
      ];

      // Filtering must run cleanly without throwing exceptions
      const results = filterRadicals('alert', adversarialItems);
      assert.strictEqual(results.length, 1);
      assert.strictEqual(results[0].radicalId, 1);
    });

    test('handles null/undefined fields inside individual radical records', () => {
      const malformedItems = [
        { radicalId: 1, character: null, pinyin: null, meaningHanViet: null, meaningVi: null },
        { radicalId: 2, character: undefined, pinyin: undefined, meaningHanViet: undefined, meaningVi: undefined },
        null,
        undefined
      ];

      // Must not crash
      const results = filterRadicals('木', malformedItems);
      assert.strictEqual(results.length, 0);
    });
  });

});
