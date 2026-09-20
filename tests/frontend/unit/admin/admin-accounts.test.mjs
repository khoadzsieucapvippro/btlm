/**
 * =============================================================================
 * UNIT TESTS: ADMIN ACCOUNTS LIFECYCLE UI LOGIC (TASK 9F.1)
 * Module: tests/frontend/unit/admin/admin-accounts.test.mjs
 * 
 * Scope:
 * - Role Guard verification: strict 'Admin' requirement, rejects 'ROLE_' prefixes.
 * - Self-Account Detection (POL-8D-01): identity matching by accountId & emailOrPhone.
 * - Response parsing & normalization under @JsonInclude(NON_NULL) omissions.
 * - PageResponse contract unpacker (items, page, size, totalElements, totalPages).
 * - Query parameters serialization matching GET /api/v1/admin/accounts.
 * - Status metadata mapping (Active, Inactive, Banned).
 * - Timestamp formatting preserving audit precision.
 * - Pagination window calculation.
 * =============================================================================
 */

import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

import {
  hasAdminAccess,
  isSelfAccount,
  parseAccountResponse,
  parseAccountsPageResponse,
  buildAccountsQueryParams,
  getStatusBadgeMeta,
  formatAccountDateTime,
  calculatePagination
} from '../../../../frontend/js/pages/admin-accounts-page.js';

describe('Unit: Admin Accounts Lifecycle UI Logic (Task 9F.1)', () => {

  // ---------------------------------------------------------------------------
  // 1. Role Guard & Authorization Check (Invariant: Strict 'Admin' representation)
  // ---------------------------------------------------------------------------
  describe('1. Role Guard Verification (hasAdminAccess)', () => {
    it('grants access to authenticated session with "Admin" role', () => {
      const mockAuth = {
        hasRole: (role) => role === 'Admin'
      };
      assert.strictEqual(hasAdminAccess(mockAuth), true);
    });

    it('denies access to non-Admin roles (Learner, Creator, Moderator)', () => {
      const mockAuth = {
        hasRole: (role) => ['Learner', 'Creator', 'Moderator'].includes(role) && role !== 'Admin'
      };
      assert.strictEqual(hasAdminAccess(mockAuth), false);
    });

    it('denies access when authManager instance has no roles', () => {
      const mockAuth = {
        hasRole: () => false
      };
      assert.strictEqual(hasAdminAccess(mockAuth), false);
    });

    it('strictly rejects legacy or prefixed "ROLE_ADMIN" role name', () => {
      const mockAuth = {
        hasRole: (role) => role === 'ROLE_ADMIN'
      };
      assert.strictEqual(hasAdminAccess(mockAuth), false);
    });

    it('handles null, undefined or malformed authManager safely', () => {
      assert.strictEqual(hasAdminAccess(null), false);
      assert.strictEqual(hasAdminAccess(undefined), false);
      assert.strictEqual(hasAdminAccess({}), false);
    });
  });

  // ---------------------------------------------------------------------------
  // 2. Self-Account Detection (Self-Protection Policy POL-8D-01)
  // ---------------------------------------------------------------------------
  describe('2. Self-Account Detection (isSelfAccount)', () => {
    const currentAdmin = {
      accountId: 1,
      emailOrPhone: 'admin@example.com',
      fullName: 'Quản Trị Viên',
      roles: ['Admin']
    };

    it('identifies self when target account has matching accountId', () => {
      const target = {
        accountId: 1,
        emailOrPhone: 'different_email@example.com'
      };
      assert.strictEqual(isSelfAccount(target, currentAdmin), true);
    });

    it('identifies self when target account has matching email (case-insensitive)', () => {
      const target = {
        accountId: 999, // Different ID, but same normalized email
        emailOrPhone: 'ADMIN@example.com'
      };
      assert.strictEqual(isSelfAccount(target, currentAdmin), true);
    });

    it('identifies self when target account has trimmed matching phone', () => {
      const phoneAdmin = {
        accountId: 2,
        emailOrPhone: '0912345678'
      };
      const target = {
        accountId: 2,
        emailOrPhone: ' 0912345678 '
      };
      assert.strictEqual(isSelfAccount(target, phoneAdmin), true);
    });

    it('returns false for different accounts', () => {
      const otherAccount = {
        accountId: 42,
        emailOrPhone: 'learner@example.com'
      };
      assert.strictEqual(isSelfAccount(otherAccount, currentAdmin), false);
    });

    it('returns false safely when target or current user is null/undefined', () => {
      assert.strictEqual(isSelfAccount(null, currentAdmin), false);
      assert.strictEqual(isSelfAccount(currentAdmin, null), false);
      assert.strictEqual(isSelfAccount(null, null), false);
      assert.strictEqual(isSelfAccount({}, {}), false);
    });
  });

  // ---------------------------------------------------------------------------
  // 3. Response Parsing & Normalization (AccountResponse contract)
  // ---------------------------------------------------------------------------
  describe('3. AccountResponse Parsing (parseAccountResponse)', () => {
    it('parses complete backend AccountResponse DTO', () => {
      const raw = {
        accountId: 10,
        emailOrPhone: 'user10@example.com',
        fullName: 'Nguyễn Văn A',
        status: 'Active',
        roles: ['Learner', 'Creator'],
        createdAt: '2026-09-01T10:00:00',
        updatedAt: '2026-09-15T15:30:00'
      };

      const parsed = parseAccountResponse(raw);
      assert.strictEqual(parsed.accountId, 10);
      assert.strictEqual(parsed.emailOrPhone, 'user10@example.com');
      assert.strictEqual(parsed.fullName, 'Nguyễn Văn A');
      assert.strictEqual(parsed.status, 'Active');
      assert.deepStrictEqual(parsed.roles, ['Learner', 'Creator']);
      assert.strictEqual(parsed.createdAt, '2026-09-01T10:00:00');
      assert.strictEqual(parsed.updatedAt, '2026-09-15T15:30:00');
    });

    it('handles @JsonInclude(NON_NULL) omissions (null fullName or updatedAt)', () => {
      const raw = {
        accountId: 11,
        emailOrPhone: 'user11@example.com',
        status: 'Inactive'
      };

      const parsed = parseAccountResponse(raw);
      assert.strictEqual(parsed.accountId, 11);
      assert.strictEqual(parsed.emailOrPhone, 'user11@example.com');
      assert.strictEqual(parsed.fullName, null);
      assert.strictEqual(parsed.status, 'Inactive');
      assert.deepStrictEqual(parsed.roles, []);
      assert.strictEqual(parsed.createdAt, null);
      assert.strictEqual(parsed.updatedAt, null);
    });

    it('normalizes status casing correctly (Active, Inactive, Banned)', () => {
      assert.strictEqual(parseAccountResponse({ status: 'ACTIVE' }).status, 'Active');
      assert.strictEqual(parseAccountResponse({ status: 'inactive' }).status, 'Inactive');
      assert.strictEqual(parseAccountResponse({ status: 'Banned' }).status, 'Banned');
      assert.strictEqual(parseAccountResponse({ status: 'unknown' }).status, 'Inactive');
    });

    it('handles null or malformed input without throwing', () => {
      const parsed = parseAccountResponse(null);
      assert.strictEqual(parsed.accountId, 0);
      assert.strictEqual(parsed.emailOrPhone, '');
      assert.strictEqual(parsed.status, 'Inactive');
      assert.deepStrictEqual(parsed.roles, []);
    });
  });

  // ---------------------------------------------------------------------------
  // 4. PageResponse Parsing & Envelope Unpacking
  // ---------------------------------------------------------------------------
  describe('4. PageResponse Envelope Parsing (parseAccountsPageResponse)', () => {
    it('parses standard ApiResponse envelope wrapping PageResponse<AccountResponse>', () => {
      const payload = {
        code: 'SUCCESS',
        message: 'Thành công',
        data: {
          items: [
            { accountId: 1, emailOrPhone: 'a@test.com', status: 'Active' },
            { accountId: 2, emailOrPhone: 'b@test.com', status: 'Banned' }
          ],
          page: 0,
          size: 20,
          totalElements: 2,
          totalPages: 1
        }
      };

      const parsed = parseAccountsPageResponse(payload);
      assert.strictEqual(parsed.items.length, 2);
      assert.strictEqual(parsed.items[0].accountId, 1);
      assert.strictEqual(parsed.items[1].status, 'Banned');
      assert.strictEqual(parsed.page, 0);
      assert.strictEqual(parsed.size, 20);
      assert.strictEqual(parsed.totalElements, 2);
      assert.strictEqual(parsed.totalPages, 1);
    });

    it('parses direct PageResponse object without outer envelope', () => {
      const data = {
        items: [{ accountId: 5, emailOrPhone: 'five@test.com', status: 'Active' }],
        page: 2,
        size: 10,
        totalElements: 25,
        totalPages: 3
      };

      const parsed = parseAccountsPageResponse(data);
      assert.strictEqual(parsed.items.length, 1);
      assert.strictEqual(parsed.page, 2);
      assert.strictEqual(parsed.size, 10);
      assert.strictEqual(parsed.totalElements, 25);
      assert.strictEqual(parsed.totalPages, 3);
    });

    it('handles empty or malformed payload gracefully', () => {
      const parsed = parseAccountsPageResponse(null);
      assert.deepStrictEqual(parsed.items, []);
      assert.strictEqual(parsed.page, 0);
      assert.strictEqual(parsed.size, 20);
      assert.strictEqual(parsed.totalElements, 0);
      assert.strictEqual(parsed.totalPages, 0);
    });
  });

  // ---------------------------------------------------------------------------
  // 5. Query Parameters Construction (buildAccountsQueryParams)
  // ---------------------------------------------------------------------------
  describe('5. Query Parameters Construction (buildAccountsQueryParams)', () => {
    it('constructs minimal default pagination query', () => {
      const query = buildAccountsQueryParams();
      assert.strictEqual(query.page, 0);
      assert.strictEqual(query.size, 20);
      assert.strictEqual('status' in query, false);
      assert.strictEqual('search' in query, false);
    });

    it('includes status only when valid (Active, Inactive, Banned)', () => {
      const qActive = buildAccountsQueryParams({ status: 'Active' });
      assert.strictEqual(qActive.status, 'Active');

      const qInactive = buildAccountsQueryParams({ status: 'Inactive' });
      assert.strictEqual(qInactive.status, 'Inactive');

      const qBanned = buildAccountsQueryParams({ status: 'Banned' });
      assert.strictEqual(qBanned.status, 'Banned');

      // 'All' or empty or invalid status must be omitted
      const qAll = buildAccountsQueryParams({ status: 'All' });
      assert.strictEqual('status' in qAll, false);

      const qEmpty = buildAccountsQueryParams({ status: '' });
      assert.strictEqual('status' in qEmpty, false);
    });

    it('trims search query and omits if blank', () => {
      const qSearch = buildAccountsQueryParams({ search: '  admin@example.com  ' });
      assert.strictEqual(qSearch.search, 'admin@example.com');

      const qBlank = buildAccountsQueryParams({ search: '   ' });
      assert.strictEqual('search' in qBlank, false);
    });

    it('enforces non-negative page and minimum size 1', () => {
      const qNegative = buildAccountsQueryParams({ page: -5, size: 0 });
      assert.strictEqual(qNegative.page, 0);
      assert.strictEqual(qNegative.size, 1);
    });
  });

  // ---------------------------------------------------------------------------
  // 6. Status Badge Metadata (getStatusBadgeMeta)
  // ---------------------------------------------------------------------------
  describe('6. Status Badge Metadata (getStatusBadgeMeta)', () => {
    it('returns Active metadata with badge-status-active', () => {
      const meta = getStatusBadgeMeta('Active');
      assert.strictEqual(meta.label, 'Hoạt động');
      assert.strictEqual(meta.badgeClass, 'badge-status badge-status-active');
    });

    it('returns Inactive metadata with badge-status-inactive', () => {
      const meta = getStatusBadgeMeta('Inactive');
      assert.strictEqual(meta.label, 'Tạm khóa');
      assert.strictEqual(meta.badgeClass, 'badge-status badge-status-inactive');
    });

    it('returns Banned metadata with badge-status-banned', () => {
      const meta = getStatusBadgeMeta('Banned');
      assert.strictEqual(meta.label, 'Bị cấm');
      assert.strictEqual(meta.badgeClass, 'badge-status badge-status-banned');
    });

    it('handles unexpected status gracefully with draft fallback', () => {
      const meta = getStatusBadgeMeta('UnknownStatus');
      assert.strictEqual(meta.label, 'UnknownStatus');
      assert.strictEqual(meta.badgeClass, 'badge-status badge-status-draft');
    });
  });

  // ---------------------------------------------------------------------------
  // 7. Audit Date Formatting (formatAccountDateTime)
  // ---------------------------------------------------------------------------
  describe('7. Timestamp Formatting (formatAccountDateTime)', () => {
    it('formats ISO timestamp into DD/MM/YYYY HH:mm', () => {
      const formatted = formatAccountDateTime('2026-09-16T14:30:00');
      assert.match(formatted, /16\/09\/2026 \d{2}:30/);
    });

    it('returns dash fallback for null or undefined date', () => {
      assert.strictEqual(formatAccountDateTime(null), '—');
      assert.strictEqual(formatAccountDateTime(undefined), '—');
      assert.strictEqual(formatAccountDateTime(''), '—');
    });

    it('returns raw string safely if unparseable', () => {
      assert.strictEqual(formatAccountDateTime('invalid-date-string'), 'invalid-date-string');
    });
  });

  // ---------------------------------------------------------------------------
  // 8. Pagination Window Calculation (calculatePagination)
  // ---------------------------------------------------------------------------
  describe('8. Pagination Calculation (calculatePagination)', () => {
    it('returns empty array when totalPages <= 0', () => {
      assert.deepStrictEqual(calculatePagination(0, 0), []);
    });

    it('returns all pages when totalPages <= maxVisible', () => {
      assert.deepStrictEqual(calculatePagination(0, 3, 5), [0, 1, 2]);
    });

    it('inserts ellipsis correctly when totalPages exceeds maxVisible', () => {
      // Beginning of multi-page list
      const pagesStart = calculatePagination(0, 10, 5);
      assert.deepStrictEqual(pagesStart, [0, 1, 2, 3, 4, '...', 9]);

      // Middle of multi-page list
      const pagesMiddle = calculatePagination(5, 10, 5);
      assert.deepStrictEqual(pagesMiddle, [0, '...', 3, 4, 5, 6, 7, '...', 9]);

      // End of multi-page list
      const pagesEnd = calculatePagination(9, 10, 5);
      assert.deepStrictEqual(pagesEnd, [0, '...', 5, 6, 7, 8, 9]);
    });
  });
});
