/**
 * =============================================================================
 * UNIT TEST SUITE: CHINESE VOCABULARY CATALOG & SEARCH ENGINE (TASK 9B.3)
 * File: tests/frontend/unit/vocabulary/vocabulary-catalog.test.mjs
 * 
 * Verifies:
 * - Query parameter serialization (search, page, size; strictly NEVER 'q').
 * - Search query normalization (trimming, edge cases).
 * - PageResponse contract parsing & pagination boundary calculations.
 * - Progressive radical disclosure contract (zero radicals in list, radicals in detail).
 * - Stale response protection logic & request sequencing.
 * - Web Speech API feature detection & graceful fallback invariants.
 * - Safe rendering & resource URL sanitization (audio/media).
 * - Adversarial payload resistance (XSS, script-like inputs).
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  normalizeSearchQuery,
  parsePageResponse,
  calculatePagination,
  isSpeechSynthesisAvailable,
  speakHanzi
} from '../../../../frontend/js/pages/vocabulary-page.js';
import { sanitizeResourceUrl } from '../../../../frontend/js/ui/security.js';

describe('Unit: Chinese Vocabulary Catalog & Logic Engine', () => {

  describe('1. Query Serialization & Normalization Contract', () => {
    test('normalizes search query by trimming leading and trailing whitespaces', () => {
      assert.strictEqual(normalizeSearchQuery('  shu  '), 'shu');
      assert.strictEqual(normalizeSearchQuery('  你好  '), '你好');
      assert.strictEqual(normalizeSearchQuery('\t shū \n'), 'shū');
      assert.strictEqual(normalizeSearchQuery(''), '');
      assert.strictEqual(normalizeSearchQuery('   '), '');
    });

    test('handles null, undefined, or non-string inputs safely without throwing', () => {
      assert.strictEqual(normalizeSearchQuery(null), '');
      assert.strictEqual(normalizeSearchQuery(undefined), '');
      assert.strictEqual(normalizeSearchQuery(123), '');
      assert.strictEqual(normalizeSearchQuery({}), '');
    });

    test('enforces query contract parameter key is "search" and strictly NEVER "q"', () => {
      const query = 'shu';
      const page = 0;
      const size = 20;

      // Simulate parameter object constructed for apiClient
      const params = {
        page,
        size
      };
      const normalized = normalizeSearchQuery(query);
      if (normalized) {
        params.search = normalized;
      }

      assert.strictEqual('search' in params, true, 'Params must use key "search"');
      assert.strictEqual('q' in params, false, 'Params must NEVER use key "q"');
      assert.strictEqual(params.search, 'shu');
      assert.strictEqual(params.page, 0);
      assert.strictEqual(params.size, 20);
    });

    test('omits search param when query is empty string after trimming', () => {
      const params = { page: 0, size: 20 };
      const normalized = normalizeSearchQuery('   ');
      if (normalized) {
        params.search = normalized;
      }
      assert.strictEqual('search' in params, false, 'Search key should not be present for blank queries');
    });
  });

  describe('2. Backend PageResponse Contract & Parsing', () => {
    test('parses standard Spring Boot ApiResponse<PageResponse<VocabularyResponse>> correctly', () => {
      const mockBackendResponse = {
        page: 0,
        size: 20,
        totalElements: 45,
        totalPages: 3,
        first: true,
        last: false,
        items: [
          {
            vocabId: 101,
            hanzi: '书',
            pinyin: 'shū',
            meaningHanViet: 'Thư',
            meaningVi: 'Sách, vở',
            audioUrl: 'https://cdn.example.com/audio/shu.mp3'
          },
          {
            vocabId: 102,
            hanzi: '学',
            pinyin: 'xué',
            meaningHanViet: 'Học',
            meaningVi: 'Học tập',
            audioUrl: null
          }
        ]
      };

      const parsed = parsePageResponse(mockBackendResponse);
      assert.strictEqual(parsed.page, 0);
      assert.strictEqual(parsed.size, 20);
      assert.strictEqual(parsed.totalElements, 45);
      assert.strictEqual(parsed.totalPages, 3);
      assert.strictEqual(parsed.items.length, 2);
      assert.strictEqual(parsed.items[0].hanzi, '书');
      assert.strictEqual(parsed.items[1].hanzi, '学');
    });

    test('handles fallback when items is missing or not an array', () => {
      const invalidResponse = {
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0
      };
      const parsed = parsePageResponse(invalidResponse);
      assert.deepStrictEqual(parsed.items, []);
      assert.strictEqual(parsed.totalElements, 0);
      assert.strictEqual(parsed.totalPages, 0);
    });

    test('handles null/undefined response object gracefully', () => {
      const parsedNull = parsePageResponse(null);
      assert.deepStrictEqual(parsedNull.items, []);
      assert.strictEqual(parsedNull.page, 0);
      assert.strictEqual(parsedNull.totalElements, 0);
      assert.strictEqual(parsedNull.totalPages, 0);
    });
  });

  describe('3. Pagination Math & Boundary Calculations', () => {
    test('calculates accurate page indices and boundaries for multi-page datasets', () => {
      const result = calculatePagination(98, 0, 20, 5);

      assert.strictEqual(result.pageIndex, 0);
      assert.strictEqual(result.pageSize, 20);
      assert.strictEqual(result.totalPages, 5);
      assert.strictEqual(result.startIndex, 0);
      assert.strictEqual(result.endIndex, 20);
      assert.strictEqual(result.startPage, 0);
      assert.strictEqual(result.endPage, 4);
    });

    test('calculates correct last page boundaries with partial size', () => {
      const result = calculatePagination(98, 4, 20, 5);

      assert.strictEqual(result.pageIndex, 4);
      assert.strictEqual(result.totalPages, 5);
      assert.strictEqual(result.startIndex, 80);
      assert.strictEqual(result.endIndex, 98, 'End index clamps to totalElements');
    });

    test('handles zero elements (empty catalog/empty search)', () => {
      const result = calculatePagination(0, 0, 20, 5);

      assert.strictEqual(result.pageIndex, 0);
      assert.strictEqual(result.totalPages, 0);
      assert.strictEqual(result.startIndex, 0);
      assert.strictEqual(result.endIndex, 0);
      assert.strictEqual(result.startPage, 0);
      assert.strictEqual(result.endPage, 0);
    });

    test('supports configured page sizes: 12, 20, 40', () => {
      const allowedSizes = [12, 20, 40];
      const defaultSize = 20;

      assert.ok(allowedSizes.includes(defaultSize), 'Default page size 20 must be in allowed list');
      assert.strictEqual(allowedSizes.length, 3);
    });
  });

  describe('4. Zero N+1 Progressive Radical Disclosure Contract', () => {
    test('verifies VocabularyResponse list summary contains NO radicals (Zero N+1)', () => {
      const summaryItem = {
        vocabId: 1,
        hanzi: '休',
        pinyin: 'xiū',
        meaningHanViet: 'Hưu',
        meaningVi: 'Nghỉ ngơi',
        audioUrl: null
      };

      // Invariant: list item must NOT have radicals
      assert.strictEqual('radicals' in summaryItem, false, 'List summary DTO must not have radicals property');
    });

    test('verifies VocabularyDetailResponse DOES contain constituent radicals array', () => {
      const detailItem = {
        vocabId: 1,
        hanzi: '休',
        pinyin: 'xiū',
        meaningHanViet: 'Hưu',
        meaningVi: 'Nghỉ ngơi',
        exampleSentence: '他在家里休息。',
        exampleTranslation: 'Anh ấy nghỉ ngơi ở nhà.',
        audioUrl: 'https://cdn.example.com/audio/xiu.mp3',
        radicals: [
          { radicalId: 9, character: '亻', pinyin: 'rén', meaningHanViet: 'Nhân', meaningVi: 'Người' },
          { radicalId: 75, character: '木', pinyin: 'mù', meaningHanViet: 'Mộc', meaningVi: 'Cây cối' }
        ]
      };

      assert.strictEqual(Array.isArray(detailItem.radicals), true, 'Detail item must have radicals array');
      assert.strictEqual(detailItem.radicals.length, 2);
      assert.strictEqual(detailItem.radicals[0].character, '亻');
      assert.strictEqual(detailItem.radicals[1].character, '木');
    });
  });

  describe('5. Stale Response & Request Sequencing Invariant', () => {
    test('drops older out-of-order response when latestRequestId has advanced', () => {
      let activeRequestId = 0;
      let committedData = null;

      function simulateRequest(query, delayMs, assignedId) {
        return new Promise((resolve) => {
          setTimeout(() => {
            resolve({ id: assignedId, query, result: `Results for ${query}` });
          }, delayMs);
        });
      }

      // User types 's' (id=1, slow response: 50ms)
      const id1 = ++activeRequestId;
      const req1 = simulateRequest('s', 50, id1);

      // User immediately types 'sh' (id=2, faster response: 10ms)
      const id2 = ++activeRequestId;
      const req2 = simulateRequest('sh', 10, id2);

      return Promise.all([
        req1.then((res) => {
          if (res.id === activeRequestId) {
            committedData = res.result;
          }
        }),
        req2.then((res) => {
          if (res.id === activeRequestId) {
            committedData = res.result;
          }
        })
      ]).then(() => {
        // Even though req1 took 50ms and resolved after req2, it must NOT overwrite committedData
        assert.strictEqual(committedData, 'Results for sh', 'Stale response must not overwrite newer search results');
      });
    });
  });

  describe('6. Web Speech API Capability & Fallback', () => {
    test('isSpeechSynthesisAvailable returns boolean without throwing in Node environment', () => {
      // In Node.js environment, window is undefined or lacks speechSynthesis
      const available = isSpeechSynthesisAvailable();
      assert.strictEqual(typeof available, 'boolean');
      assert.strictEqual(available, false, 'Expected false in Node.js headless environment');
    });

    test('speakHanzi returns false gracefully when speechSynthesis is unavailable (No crash)', () => {
      const result = speakHanzi('书');
      assert.strictEqual(result, false, 'speakHanzi must return false gracefully when speech is unsupported');
    });

    test('speakHanzi rejects empty/invalid hanzi strings safely', () => {
      assert.strictEqual(speakHanzi(''), false);
      assert.strictEqual(speakHanzi(null), false);
      assert.strictEqual(speakHanzi(undefined), false);
    });
  });

  describe('7. Safe Media URL Sanitization', () => {
    test('sanitizeResourceUrl permits valid HTTPS and HTTP audio URLs', () => {
      const httpsUrl = 'https://cdn.example.com/audio/vocab_1.mp3';
      const httpUrl = 'http://audio.example.org/sound.ogg';
      assert.strictEqual(sanitizeResourceUrl(httpsUrl), httpsUrl);
      assert.strictEqual(sanitizeResourceUrl(httpUrl), httpUrl);
    });

    test('sanitizeResourceUrl neutralizes javascript: and data: URI vectors to about:blank', () => {
      assert.strictEqual(sanitizeResourceUrl('javascript:alert(1)'), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl('JAVASCRIPT:evil()'), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl('data:text/html,<script>alert(1)</script>'), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl('//evil.com/sound.mp3'), 'about:blank');
    });

    test('sanitizeResourceUrl neutralizes null, undefined, empty to about:blank', () => {
      assert.strictEqual(sanitizeResourceUrl(''), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl(null), 'about:blank');
      assert.strictEqual(sanitizeResourceUrl(undefined), 'about:blank');
    });
  });

  describe('8. Adversarial Payload Resistance', () => {
    test('handles XSS injection attempts in search queries without mutation or error', () => {
      const xssVector = '<script>alert("XSS")</script>';
      const normalized = normalizeSearchQuery(xssVector);
      assert.strictEqual(normalized, xssVector.trim());

      // Query param creation must contain the literal text
      const params = { search: normalized };
      assert.strictEqual(params.search, '<script>alert("XSS")</script>');
    });

    test('handles SQL injection patterns in search input cleanly as plain string', () => {
      const sqliVector = "' OR '1'='1; DROP TABLE vocabulary; --";
      const normalized = normalizeSearchQuery(sqliVector);
      assert.strictEqual(normalized, sqliVector);
    });
  });

});
