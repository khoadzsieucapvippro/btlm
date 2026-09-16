/**
 * =============================================================================
 * TARGETED UNIT TESTS: ADMIN ROLE MANAGEMENT & ROLE ASSIGNMENT (TASK 9F.2)
 * Module: tests/frontend/unit/admin/admin-roles.test.mjs
 * 
 * Scope:
 * - Pure logic unit testing of admin-roles-page.js functions:
 *   1. hasAdminAccess: Admin role guard invariant (rejects ROLE_ADMIN, handles edge cases).
 *   2. isSelfAccount: Target account self-detection (matches by ID or normalized email/phone).
 *   3. parseRoleResponse: Backend RoleResponse parser contract (roleId, roleName).
 *   4. getRoleMetadata: Vietnamese labels, descriptions, design-token badges.
 *   5. areRoleSetsEqual: Order-independent, duplicate-tolerant no-op detection.
 *   6. validateRoleSelection: Validation invariants & POL-8D-02 self-demotion prevention.
 *   7. buildRoleUpdatePayload: Complete desired role set payload construction.
 *   8. parseAccountResponse & parseAccountsPageResponse: Defensive data normalization.
 *   9. buildAccountsQueryParams: Query serialization contract.
 *   10. calculatePagination: Accessible page array calculation.
 * =============================================================================
 */

import { describe, test } from 'node:test';
import assert from 'node:assert/strict';

import {
  hasAdminAccess,
  isSelfAccount,
  parseRoleResponse,
  getRoleMetadata,
  areRoleSetsEqual,
  validateRoleSelection,
  buildRoleUpdatePayload,
  parseAccountResponse,
  parseAccountsPageResponse,
  buildAccountsQueryParams,
  calculatePagination
} from '../../../../frontend/js/pages/admin-roles-page.js';

describe('Task 9F.2 — Admin Roles Pure Logic Unit Tests', () => {

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

    test('returns false for non-admin roles (Learner, Creator, Moderator)', () => {
      const mockLearner = { hasRole: (role) => role === 'Learner' };
      assert.strictEqual(hasAdminAccess(mockLearner), false);

      const mockCreator = { hasRole: (role) => role === 'Creator' };
      assert.strictEqual(hasAdminAccess(mockCreator), false);

      const mockModerator = { hasRole: (role) => role === 'Moderator' };
      assert.strictEqual(hasAdminAccess(mockModerator), false);
    });

    test('defensively handles missing, null, or invalid authManager objects', () => {
      assert.strictEqual(hasAdminAccess(null), false);
      assert.strictEqual(hasAdminAccess(undefined), false);
      assert.strictEqual(hasAdminAccess({}), false);
      assert.strictEqual(hasAdminAccess({ hasRole: 'not-a-function' }), false);
    });
  });

  /* ---------------------------------------------------------------------------
   * 2. isSelfAccount (Self Detection Logic)
   * --------------------------------------------------------------------------- */
  describe('2. isSelfAccount', () => {
    test('returns true when accountId matches', () => {
      const target = { accountId: 42, emailOrPhone: 'target@example.com' };
      const current = { accountId: 42, emailOrPhone: 'current@example.com' };
      assert.strictEqual(isSelfAccount(target, current), true);
    });

    test('returns true when normalized email matches (case and whitespace insensitive)', () => {
      const target = { accountId: null, emailOrPhone: '  ADMIN@Example.COM  ' };
      const current = { accountId: null, emailOrPhone: 'admin@example.com' };
      assert.strictEqual(isSelfAccount(target, current), true);
    });

    test('returns false when neither accountId nor email match', () => {
      const target = { accountId: 10, emailOrPhone: 'user10@example.com' };
      const current = { accountId: 20, emailOrPhone: 'user20@example.com' };
      assert.strictEqual(isSelfAccount(target, current), false);
    });

    test('defensively returns false for null/undefined parameters', () => {
      assert.strictEqual(isSelfAccount(null, null), false);
      assert.strictEqual(isSelfAccount({ accountId: 1 }, null), false);
      assert.strictEqual(isSelfAccount(null, { accountId: 1 }), false);
    });
  });

  /* ---------------------------------------------------------------------------
   * 3. parseRoleResponse (Backend RoleResponse Contract)
   * --------------------------------------------------------------------------- */
  describe('3. parseRoleResponse', () => {
    test('correctly parses valid RoleResponse contract (roleId: Integer, roleName: String)', () => {
      const raw = { roleId: 4, roleName: 'Admin' };
      const parsed = parseRoleResponse(raw);
      assert.strictEqual(parsed.roleId, 4);
      assert.strictEqual(parsed.roleName, 'Admin');
    });

    test('trims whitespace and handles numeric coercion defensively', () => {
      const raw = { roleId: '2', roleName: '  Creator  ' };
      const parsed = parseRoleResponse(raw);
      assert.strictEqual(parsed.roleId, 2);
      assert.strictEqual(parsed.roleName, 'Creator');
    });

    test('returns fallback defaults for invalid or null payload', () => {
      assert.deepStrictEqual(parseRoleResponse(null), { roleId: 0, roleName: '' });
      assert.deepStrictEqual(parseRoleResponse(undefined), { roleId: 0, roleName: '' });
      assert.deepStrictEqual(parseRoleResponse('string'), { roleId: 0, roleName: '' });
    });
  });

  /* ---------------------------------------------------------------------------
   * 4. getRoleMetadata (Client-Side Labels, Descriptions & Badges)
   * --------------------------------------------------------------------------- */
  describe('4. getRoleMetadata', () => {
    test('returns scholarly metadata for all 4 canonical roles', () => {
      const admin = getRoleMetadata('Admin');
      assert.strictEqual(admin.label, 'Quản trị viên');
      assert.ok(admin.description.length > 0);
      assert.ok(admin.badgeClass.includes('badge-status'));

      const creator = getRoleMetadata('Creator');
      assert.strictEqual(creator.label, 'Người sáng tạo');

      const moderator = getRoleMetadata('Moderator');
      assert.strictEqual(moderator.label, 'Kiểm duyệt viên');

      const learner = getRoleMetadata('Learner');
      assert.strictEqual(learner.label, 'Học viên');
    });

    test('handles unknown roles gracefully without crashing', () => {
      const custom = getRoleMetadata('CustomRole');
      assert.strictEqual(custom.label, 'CustomRole');
      assert.ok(custom.badgeClass.includes('badge-status'));
    });
  });

  /* ---------------------------------------------------------------------------
   * 5. areRoleSetsEqual (No-Op Mutation Detection)
   * --------------------------------------------------------------------------- */
  describe('5. areRoleSetsEqual', () => {
    test('returns true for identical role sets regardless of element order', () => {
      assert.strictEqual(areRoleSetsEqual(['Admin', 'Creator'], ['Creator', 'Admin']), true);
      assert.strictEqual(areRoleSetsEqual(['Learner'], ['Learner']), true);
    });

    test('returns true when duplicate items exist in one or both inputs', () => {
      assert.strictEqual(areRoleSetsEqual(['Admin', 'Admin', 'Creator'], ['Creator', 'Admin']), true);
    });

    test('returns false when role sets differ in elements or size', () => {
      assert.strictEqual(areRoleSetsEqual(['Admin'], ['Admin', 'Creator']), false);
      assert.strictEqual(areRoleSetsEqual(['Admin', 'Moderator'], ['Admin', 'Creator']), false);
      assert.strictEqual(areRoleSetsEqual([], ['Admin']), false);
    });

    test('handles Set inputs seamlessly', () => {
      const setA = new Set(['Admin', 'Learner']);
      const setB = new Set(['Learner', 'Admin']);
      assert.strictEqual(areRoleSetsEqual(setA, setB), true);
    });
  });

  /* ---------------------------------------------------------------------------
   * 6. validateRoleSelection (Validation & Self-Demotion POL-8D-02)
   * --------------------------------------------------------------------------- */
  describe('6. validateRoleSelection', () => {
    const catalog = ['Admin', 'Creator', 'Moderator', 'Learner'];

    test('fails validation when selected roles list is empty', () => {
      const res = validateRoleSelection([], catalog, false);
      assert.strictEqual(res.valid, false);
      assert.ok(res.error.includes('ít nhất một vai trò'));
    });

    test('fails validation when selected roles contain non-catalog values', () => {
      const res = validateRoleSelection(['Admin', 'SuperUser'], catalog, false);
      assert.strictEqual(res.valid, false);
      assert.ok(res.error.includes('không tồn tại trong danh mục'));
    });

    test('POL-8D-02: fails validation when current Admin attempts to remove own Admin role', () => {
      const res = validateRoleSelection(['Creator', 'Learner'], catalog, true);
      assert.strictEqual(res.valid, false);
      assert.ok(res.error.includes('POL-8D-02'));
    });

    test('POL-8D-02: passes validation when current Admin retains Admin and edits other roles', () => {
      const res = validateRoleSelection(['Admin', 'Moderator'], catalog, true);
      assert.strictEqual(res.valid, true);
      assert.strictEqual(res.error, null);
    });

    test('allows removing Admin role when target is ANOTHER account (not self)', () => {
      const res = validateRoleSelection(['Learner'], catalog, false);
      assert.strictEqual(res.valid, true);
      assert.strictEqual(res.error, null);
    });
  });

  /* ---------------------------------------------------------------------------
   * 7. buildRoleUpdatePayload (Complete Role Set Contract)
   * --------------------------------------------------------------------------- */
  describe('7. buildRoleUpdatePayload', () => {
    test('produces { roles: [...] } containing unique complete desired roles', () => {
      const payload = buildRoleUpdatePayload(['Admin', 'Creator', 'Admin']);
      assert.deepStrictEqual(payload, { roles: ['Admin', 'Creator'] });
    });

    test('handles empty or iterable input safely', () => {
      assert.deepStrictEqual(buildRoleUpdatePayload([]), { roles: [] });
      assert.deepStrictEqual(buildRoleUpdatePayload(null), { roles: [] });
      const set = new Set(['Learner', 'Moderator']);
      assert.deepStrictEqual(buildRoleUpdatePayload(set), { roles: ['Learner', 'Moderator'] });
    });
  });

  /* ---------------------------------------------------------------------------
   * 8. parseAccountResponse & parseAccountsPageResponse
   * --------------------------------------------------------------------------- */
  describe('8. parseAccountResponse & parseAccountsPageResponse', () => {
    test('normalizes account response with status and roles', () => {
      const raw = {
        accountId: 15,
        emailOrPhone: 'user@example.com',
        fullName: 'Nguyễn Văn A',
        status: 'Active',
        roles: ['Learner', 'Creator']
      };
      const parsed = parseAccountResponse(raw);
      assert.strictEqual(parsed.accountId, 15);
      assert.strictEqual(parsed.emailOrPhone, 'user@example.com');
      assert.strictEqual(parsed.fullName, 'Nguyễn Văn A');
      assert.strictEqual(parsed.status, 'Active');
      assert.deepStrictEqual(parsed.roles, ['Learner', 'Creator']);
    });

    test('parses envelope and raw page responses correctly', () => {
      const envelope = {
        code: 200,
        message: 'Success',
        data: {
          items: [{ accountId: 1, emailOrPhone: 'a@b.c', status: 'Active', roles: ['Admin'] }],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1
        }
      };
      const result = parseAccountsPageResponse(envelope);
      assert.strictEqual(result.items.length, 1);
      assert.strictEqual(result.totalElements, 1);
      assert.strictEqual(result.totalPages, 1);
      assert.strictEqual(result.items[0].emailOrPhone, 'a@b.c');
    });
  });

  /* ---------------------------------------------------------------------------
   * 9. buildAccountsQueryParams
   * --------------------------------------------------------------------------- */
  describe('9. buildAccountsQueryParams', () => {
    test('builds query with page, size, and search', () => {
      const query = buildAccountsQueryParams({ page: 1, size: 10, search: '  test@example.com  ' });
      assert.strictEqual(query.page, 1);
      assert.strictEqual(query.size, 10);
      assert.strictEqual(query.search, 'test@example.com');
    });

    test('omits empty search and invalid status', () => {
      const query = buildAccountsQueryParams({ page: 0, size: 20, search: '   ', status: 'Invalid' });
      assert.strictEqual(query.page, 0);
      assert.strictEqual(query.size, 20);
      assert.strictEqual(query.search, undefined);
      assert.strictEqual(query.status, undefined);
    });
  });

  /* ---------------------------------------------------------------------------
   * 10. calculatePagination
   * --------------------------------------------------------------------------- */
  describe('10. calculatePagination', () => {
    test('returns simple array when totalPages <= maxVisible', () => {
      assert.deepStrictEqual(calculatePagination(0, 3), [0, 1, 2]);
    });

    test('returns empty array when totalPages <= 0', () => {
      assert.deepStrictEqual(calculatePagination(0, 0), []);
    });

    test('includes ellipsis when totalPages > maxVisible', () => {
      const pages = calculatePagination(0, 10, 5);
      assert.ok(pages.includes('...'));
      assert.strictEqual(pages[0], 0);
      assert.strictEqual(pages[pages.length - 1], 9);
    });
  });

});
