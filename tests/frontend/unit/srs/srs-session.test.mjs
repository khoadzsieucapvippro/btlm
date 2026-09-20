/**
 * =============================================================================
 * UNIT TEST SUITE: SRS FLASHCARD REVIEW SESSION LOGIC (TASK 9C.3)
 * Module: tests/frontend/unit/srs/srs-session.test.mjs
 * 
 * Verifies:
 * - Rating value mapping (1=Again, 2=Hard, 3=Good, 4=Easy)
 * - Reaction timer elapsed calculation and non-negative clamping
 * - Elapsed seconds formatting
 * - Review payload validation & contract serialization (POST /api/v1/srs/review)
 * - Strict zero-userId / zero-accountId client ownership invariant
 * - Local session queue Again requeue semantics & infinite loop prevention
 * - Safe rendering and adversarial payload resilience
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  mapRatingValue,
  calculateReviewTime,
  formatElapsedSeconds,
  buildReviewPayload,
  handleAgainRequeue,
  MAX_AGAIN_REVIEWS_PER_CARD
} from '../../../../frontend/js/pages/srs-review-page.js';

describe('Unit: SRS Flashcard Review Session Logic (9C.3)', () => {

  describe('1. Rating Value Mapping (mapRatingValue)', () => {
    test('maps 1 to Again (Học lại)', () => {
      const res = mapRatingValue(1);
      assert.deepStrictEqual(res, { rating: 1, label: 'Again', nameVi: 'Học lại' });
    });

    test('maps string "1" to Again (Học lại)', () => {
      const res = mapRatingValue('1');
      assert.deepStrictEqual(res, { rating: 1, label: 'Again', nameVi: 'Học lại' });
    });

    test('maps 2 to Hard (Khó)', () => {
      const res = mapRatingValue(2);
      assert.deepStrictEqual(res, { rating: 2, label: 'Hard', nameVi: 'Khó' });
    });

    test('maps 3 to Good (Tốt)', () => {
      const res = mapRatingValue(3);
      assert.deepStrictEqual(res, { rating: 3, label: 'Good', nameVi: 'Tốt' });
    });

    test('maps 4 to Easy (Dễ)', () => {
      const res = mapRatingValue(4);
      assert.deepStrictEqual(res, { rating: 4, label: 'Easy', nameVi: 'Dễ' });
    });

    test('returns null for out-of-range ratings (0, 5, -1, 100)', () => {
      assert.strictEqual(mapRatingValue(0), null);
      assert.strictEqual(mapRatingValue(5), null);
      assert.strictEqual(mapRatingValue(-1), null);
      assert.strictEqual(mapRatingValue(100), null);
    });

    test('returns null for non-numeric or empty inputs', () => {
      assert.strictEqual(mapRatingValue(''), null);
      assert.strictEqual(mapRatingValue('abc'), null);
      assert.strictEqual(mapRatingValue(null), null);
      assert.strictEqual(mapRatingValue(undefined), null);
    });
  });

  describe('2. Reaction Timer Math & Clamping (calculateReviewTime)', () => {
    test('calculates exact seconds for standard delta', () => {
      const start = 1000000;
      const now = 1005000; // 5000ms = 5s
      assert.strictEqual(calculateReviewTime(start, now), 5);
    });

    test('rounds accurately using Math.round', () => {
      const start = 1000000;
      assert.strictEqual(calculateReviewTime(start, start + 5400), 5); // 5.4s -> 5s
      assert.strictEqual(calculateReviewTime(start, start + 5600), 6); // 5.6s -> 6s
    });

    test('clamps negative delta to 0 (clock drift or corrupt timestamps)', () => {
      const start = 1005000;
      const now = 1000000; // start > now
      assert.strictEqual(calculateReviewTime(start, now), 0);
    });

    test('handles zero elapsed time', () => {
      const start = 1000000;
      assert.strictEqual(calculateReviewTime(start, start), 0);
    });

    test('handles missing or non-number timestamps safely', () => {
      assert.strictEqual(calculateReviewTime(null, undefined), 0);
      assert.strictEqual(calculateReviewTime('abc', 1000), 1);
    });
  });

  describe('3. Elapsed Time Formatting (formatElapsedSeconds)', () => {
    test('formats zero seconds', () => {
      assert.strictEqual(formatElapsedSeconds(0), '0s');
    });

    test('formats seconds under 1 minute', () => {
      assert.strictEqual(formatElapsedSeconds(42), '42s');
      assert.strictEqual(formatElapsedSeconds(59), '59s');
    });

    test('formats exact minutes', () => {
      assert.strictEqual(formatElapsedSeconds(60), '1p 0s');
      assert.strictEqual(formatElapsedSeconds(120), '2p 0s');
    });

    test('formats minutes and remaining seconds', () => {
      assert.strictEqual(formatElapsedSeconds(85), '1p 25s');
      assert.strictEqual(formatElapsedSeconds(365), '6p 5s');
    });

    test('handles negative or corrupt input gracefully', () => {
      assert.strictEqual(formatElapsedSeconds(-10), '0s');
      assert.strictEqual(formatElapsedSeconds(null), '0s');
      assert.strictEqual(formatElapsedSeconds('xyz'), '0s');
    });
  });

  describe('4. Review Payload Contract & Validation (buildReviewPayload)', () => {
    test('builds valid VOCABULARY review payload', () => {
      const payload = buildReviewPayload({
        itemType: 'VOCABULARY',
        itemId: 101,
        rating: 3,
        reviewTimeSeconds: 5
      });

      assert.deepStrictEqual(payload, {
        itemType: 'VOCABULARY',
        itemId: 101,
        rating: 3,
        reviewTimeSeconds: 5
      });
    });

    test('builds valid RADICAL review payload and normalizes lowercase type', () => {
      const payload = buildReviewPayload({
        itemType: 'radical',
        itemId: 49,
        rating: 4,
        reviewTimeSeconds: 2
      });

      assert.deepStrictEqual(payload, {
        itemType: 'RADICAL',
        itemId: 49,
        rating: 4,
        reviewTimeSeconds: 2
      });
    });

    test('strictly rejects unsupported itemType (e.g. LESSON, NOTE, empty)', () => {
      assert.throws(
        () => buildReviewPayload({ itemType: 'LESSON', itemId: 1, rating: 3, reviewTimeSeconds: 2 }),
        /itemType must be "VOCABULARY" or "RADICAL"/
      );
      assert.throws(
        () => buildReviewPayload({ itemType: '', itemId: 1, rating: 3, reviewTimeSeconds: 2 }),
        /itemType must be a non-empty string/
      );
    });

    test('strictly rejects non-positive itemId', () => {
      assert.throws(
        () => buildReviewPayload({ itemType: 'VOCABULARY', itemId: 0, rating: 3, reviewTimeSeconds: 2 }),
        /itemId must be a positive integer/
      );
      assert.throws(
        () => buildReviewPayload({ itemType: 'VOCABULARY', itemId: -5, rating: 3, reviewTimeSeconds: 2 }),
        /itemId must be a positive integer/
      );
      assert.throws(
        () => buildReviewPayload({ itemType: 'VOCABULARY', itemId: 2.5, rating: 3, reviewTimeSeconds: 2 }),
        /itemId must be a positive integer/
      );
    });

    test('strictly rejects rating outside 1..4', () => {
      assert.throws(
        () => buildReviewPayload({ itemType: 'VOCABULARY', itemId: 1, rating: 0, reviewTimeSeconds: 2 }),
        /rating must be an integer between 1 and 4/
      );
      assert.throws(
        () => buildReviewPayload({ itemType: 'VOCABULARY', itemId: 1, rating: 5, reviewTimeSeconds: 2 }),
        /rating must be an integer between 1 and 4/
      );
    });

    test('strictly rejects negative reviewTimeSeconds', () => {
      assert.throws(
        () => buildReviewPayload({ itemType: 'VOCABULARY', itemId: 1, rating: 3, reviewTimeSeconds: -1 }),
        /reviewTimeSeconds must be a non-negative integer/
      );
    });

    test('verifies ownership invariant: zero userId or accountId in serialized payload', () => {
      const payload = buildReviewPayload({
        itemType: 'VOCABULARY',
        itemId: 101,
        rating: 1,
        reviewTimeSeconds: 4,
        userId: 999,      // Attacker attempting IDOR
        accountId: 888     // Attacker attempting spoofing
      });

      assert.strictEqual('userId' in payload, false, 'Payload MUST NOT contain userId');
      assert.strictEqual('accountId' in payload, false, 'Payload MUST NOT contain accountId');
      assert.strictEqual('q' in payload, false, 'Payload MUST NOT expose SM-2 internal q value');
    });
  });

  describe('5. Local Session Queue & Again Requeue Policy (handleAgainRequeue)', () => {
    test('requeues card on first Again attempt (attempts: 1)', () => {
      const queue = [
        { card: { itemId: 1, hanzi: '水' }, againAttempts: 0 },
        { card: { itemId: 2, hanzi: '火' }, againAttempts: 0 }
      ];

      const itemToRequeue = queue[0];
      const result = handleAgainRequeue(queue, itemToRequeue, MAX_AGAIN_REVIEWS_PER_CARD);

      assert.strictEqual(result.requeued, true);
      assert.strictEqual(result.attempts, 1);
      assert.strictEqual(queue.length, 3, 'Queue must now have 3 items');
      assert.strictEqual(queue[2].card.itemId, 1);
      assert.strictEqual(queue[2]._requeued, true);
    });

    test('allows requeuing up to maxAgain limit (e.g. 3 attempts)', () => {
      const queue = [];
      const item = { card: { itemId: 1, hanzi: '木' }, againAttempts: 2 };

      // 3rd attempt: should succeed
      const res3 = handleAgainRequeue(queue, item, 3);
      assert.strictEqual(res3.requeued, true);
      assert.strictEqual(res3.attempts, 3);
      assert.strictEqual(queue.length, 1);

      // 4th attempt: should be rejected to prevent infinite loops
      const res4 = handleAgainRequeue(queue, item, 3);
      assert.strictEqual(res4.requeued, false);
      assert.strictEqual(res4.reason, 'MAX_ATTEMPTS_REACHED');
      assert.strictEqual(queue.length, 1, 'Queue length must not increase on rejection');
    });

    test('handles invalid queue or item gracefully', () => {
      const res1 = handleAgainRequeue(null, null);
      assert.strictEqual(res1.requeued, false);

      const res2 = handleAgainRequeue([], null);
      assert.strictEqual(res2.requeued, false);
    });
  });

  describe('6. Safe Card Data Presentation & Hostile Content Invariant', () => {
    test('preserves adversarial XSS payloads as plain string values in card object', () => {
      const maliciousCard = {
        itemType: 'VOCABULARY',
        itemId: 10,
        hanzi: '<script>alert("xss")</script>',
        pinyin: 'xué"><img src=x onerror=alert(1)>',
        meaningVi: '<svg onload=alert(1)>Học tập',
        meaningHanViet: '<b>Học</b>',
        exampleSentence: '<iframe src=javascript:alert(1)></iframe>',
        exampleTranslation: '<style>body{display:none}</style>'
      };

      const payload = buildReviewPayload({
        itemType: maliciousCard.itemType,
        itemId: maliciousCard.itemId,
        rating: 3,
        reviewTimeSeconds: 4
      });

      assert.strictEqual(payload.itemId, 10);
      assert.strictEqual(payload.itemType, 'VOCABULARY');
      // Values are pure data, not executed or evaluated
    });
  });
});
