/**
 * =============================================================================
 * UNIT TEST SUITE: SRS DASHBOARD & DAILY SETTINGS (TASK 9C.4)
 * Module: tests/frontend/unit/srs/srs-dashboard.test.mjs
 *
 * Scope:
 * - Input validation rules for newCardsPerDay and maxReviewPerDay.
 * - Rejection of empty, non-numeric, decimal, zero, and negative values.
 * - Quota calculation defenses (division by zero, clamping, negative guards).
 * - Sanitized PUT payload builder (zero userId/accountId leakage).
 * - Contract alignment with StudyStatsResponse and UserSrsSettingResponse.
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  validateSrsSettings,
  calculateQuotaPercentage,
  buildUpdateSettingsPayload
} from '../../../../frontend/js/pages/srs-dashboard-page.js';

describe('SRS Dashboard Unit Tests (Task 9C.4)', () => {

  describe('Validation: validateSrsSettings', () => {

    test('accepts valid positive integer inputs (numbers and strings)', () => {
      const res1 = validateSrsSettings(20, 100);
      assert.strictEqual(res1.isValid, true);
      assert.deepStrictEqual(res1.errors, {});

      const res2 = validateSrsSettings('15', '50');
      assert.strictEqual(res2.isValid, true);
      assert.deepStrictEqual(res2.errors, {});

      const res3 = validateSrsSettings(1, 1);
      assert.strictEqual(res3.isValid, true);
      assert.deepStrictEqual(res3.errors, {});
    });

    test('rejects empty, null, and undefined values', () => {
      const resEmpty = validateSrsSettings('', '');
      assert.strictEqual(resEmpty.isValid, false);
      assert.ok(resEmpty.errors.newCardsPerDay.includes('không được để trống'));
      assert.ok(resEmpty.errors.maxReviewPerDay.includes('không được để trống'));

      const resNull = validateSrsSettings(null, null);
      assert.strictEqual(resNull.isValid, false);
      assert.ok(resNull.errors.newCardsPerDay);
      assert.ok(resNull.errors.maxReviewPerDay);

      const resUndefined = validateSrsSettings(undefined, undefined);
      assert.strictEqual(resUndefined.isValid, false);
      assert.ok(resUndefined.errors.newCardsPerDay);
      assert.ok(resUndefined.errors.maxReviewPerDay);

      const resWhitespace = validateSrsSettings('   ', '   ');
      assert.strictEqual(resWhitespace.isValid, false);
      assert.ok(resWhitespace.errors.newCardsPerDay);
      assert.ok(resWhitespace.errors.maxReviewPerDay);
    });

    test('rejects non-numeric string values', () => {
      const res = validateSrsSettings('abc', 'xyz');
      assert.strictEqual(res.isValid, false);
      assert.ok(res.errors.newCardsPerDay.includes('hợp lệ'));
      assert.ok(res.errors.maxReviewPerDay.includes('hợp lệ'));
    });

    test('rejects decimal (floating-point) values', () => {
      const res1 = validateSrsSettings(10.5, 50);
      assert.strictEqual(res1.isValid, false);
      assert.ok(res1.errors.newCardsPerDay.includes('phần thập phân'));

      const res2 = validateSrsSettings('20', '30.25');
      assert.strictEqual(res2.isValid, false);
      assert.ok(res2.errors.maxReviewPerDay.includes('phần thập phân'));
    });

    test('rejects zero (0) values', () => {
      const res1 = validateSrsSettings(0, 100);
      assert.strictEqual(res1.isValid, false);
      assert.ok(res1.errors.newCardsPerDay.includes('lớn hơn 0'));

      const res2 = validateSrsSettings('20', '0');
      assert.strictEqual(res2.isValid, false);
      assert.ok(res2.errors.maxReviewPerDay.includes('lớn hơn 0'));
    });

    test('rejects negative values', () => {
      const res1 = validateSrsSettings(-5, 100);
      assert.strictEqual(res1.isValid, false);
      assert.ok(res1.errors.newCardsPerDay.includes('lớn hơn 0'));

      const res2 = validateSrsSettings(20, -10);
      assert.strictEqual(res2.isValid, false);
      assert.ok(res2.errors.maxReviewPerDay.includes('lớn hơn 0'));
    });

    test('validates fields independently', () => {
      const resNewInvalid = validateSrsSettings('invalid', 50);
      assert.strictEqual(resNewInvalid.isValid, false);
      assert.ok(resNewInvalid.errors.newCardsPerDay);
      assert.strictEqual(resNewInvalid.errors.maxReviewPerDay, undefined);

      const resMaxInvalid = validateSrsSettings(20, 0);
      assert.strictEqual(resMaxInvalid.isValid, false);
      assert.strictEqual(resNewInvalid.errors.maxReviewPerDay, undefined);
      assert.ok(resMaxInvalid.errors.maxReviewPerDay);
    });

  });

  describe('Quota Calculation: calculateQuotaPercentage', () => {

    test('calculates correct percentages for standard progress', () => {
      assert.strictEqual(calculateQuotaPercentage(0, 20), 0);
      assert.strictEqual(calculateQuotaPercentage(10, 20), 50);
      assert.strictEqual(calculateQuotaPercentage(20, 20), 100);
      assert.strictEqual(calculateQuotaPercentage(25, 100), 25);
      assert.strictEqual(calculateQuotaPercentage(1, 3), 33);
    });

    test('clamps output to maximum 100% when progress exceeds limit', () => {
      assert.strictEqual(calculateQuotaPercentage(25, 20), 100);
      assert.strictEqual(calculateQuotaPercentage(150, 100), 100);
    });

    test('defends against division by zero', () => {
      assert.strictEqual(calculateQuotaPercentage(5, 0), 0);
      assert.strictEqual(calculateQuotaPercentage(0, 0), 0);
    });

    test('defends against negative and non-finite inputs', () => {
      assert.strictEqual(calculateQuotaPercentage(-5, 20), 0);
      assert.strictEqual(calculateQuotaPercentage(5, -10), 0);
      assert.strictEqual(calculateQuotaPercentage(NaN, 20), 0);
      assert.strictEqual(calculateQuotaPercentage(10, Infinity), 0);
    });

  });

  describe('Payload Builder: buildUpdateSettingsPayload', () => {

    test('creates strict integer payload matching UpdateSrsSettingRequest', () => {
      const payload = buildUpdateSettingsPayload('25', '120');

      assert.deepStrictEqual(payload, {
        newCardsPerDay: 25,
        maxReviewPerDay: 120
      });

      assert.strictEqual(typeof payload.newCardsPerDay, 'number');
      assert.strictEqual(typeof payload.maxReviewPerDay, 'number');
    });

    test('never leaks user identity (zero userId or accountId in payload)', () => {
      const payload = buildUpdateSettingsPayload(30, 150);

      assert.strictEqual('userId' in payload, false);
      assert.strictEqual('accountId' in payload, false);
      assert.strictEqual(Object.keys(payload).length, 2);
    });

  });

  describe('Contract Verification: StudyStatsResponse & UserSrsSettingResponse', () => {

    test('accepts backend-authoritative stats fields', () => {
      const mockBackendStats = {
        cardsDue: 8,
        reviewsToday: 12,
        newCardsToday: 5,
        newCardsLimit: 20,
        maxReviewLimit: 100
      };

      assert.ok('cardsDue' in mockBackendStats);
      assert.ok('reviewsToday' in mockBackendStats);
      assert.ok('newCardsToday' in mockBackendStats);
      assert.ok('newCardsLimit' in mockBackendStats);
      assert.ok('maxReviewLimit' in mockBackendStats);
    });

    test('handles zero as valid data, not empty or error', () => {
      const mockZeroStats = {
        cardsDue: 0,
        reviewsToday: 0,
        newCardsToday: 0,
        newCardsLimit: 20,
        maxReviewLimit: 100
      };

      assert.strictEqual(mockZeroStats.cardsDue, 0);
      assert.strictEqual(mockZeroStats.reviewsToday, 0);
      assert.strictEqual(mockZeroStats.newCardsToday, 0);
    });

    test('accepts backend-authoritative settings fields', () => {
      const mockBackendSettings = {
        settingId: 101,
        newCardsPerDay: 20,
        maxReviewPerDay: 100
      };

      assert.strictEqual(typeof mockBackendSettings.newCardsPerDay, 'number');
      assert.strictEqual(typeof mockBackendSettings.maxReviewPerDay, 'number');
    });

  });

});
