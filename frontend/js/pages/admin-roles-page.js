/**
 * =============================================================================
 * ADMIN ROLE MANAGEMENT & ROLE ASSIGNMENT CONTROLLER (TASK 9F.2)
 * Module: frontend/js/pages/admin-roles-page.js
 * 
 * Responsibilities:
 * - Authoritative administrative integration with Spring Boot:
 *   * GET /api/v1/admin/roles (System Role Catalog)
 *   * GET /api/v1/admin/accounts (Account Search & Pagination)
 *   * PUT /api/v1/admin/accounts/{id}/roles (Complete Role Set Mutation)
 * - Role-guarded UX: strictly accessible to 'Admin' (authManager.hasRole('Admin')).
 * - Server-authoritative role catalog rendering (RoleResponse: roleId, roleName).
 * - Complete role set selection model (replaces entire role set, not incremental).
 * - Self-protection defense-in-depth (POL-8D-02):
 *   * For current Admin: Admin role is locked/checked (cannot remove own Admin role).
 *   * Other roles remain editable for self-assignment.
 * - Accurate server-side session invalidation disclosure (DEC-42):
 *   * When effective roles change, server increments authorization_version.
 *   * Truthfully disclosed without claiming frontend token revocation.
 * - No-op mutation prevention: detects unchanged role sets and prevents PUT.
 * - Native <dialog> modal with accessible keyboard interaction, focus restoration.
 * - Safe DOM construction (zero unsafe innerHTML) defending against XSS.
 * - Three-state UI (Loading / Empty / Error with retry callback).
 * - Non-blocking Toast notifications for administrative feedback.
 * =============================================================================
 */

import { apiClient, ApiError } from '../api/api.js';
import { authManager } from '../auth/auth-state.js';
import { initNavbarAuth, getRoleLabel, getRoleBadgeClass } from '../ui/nav.js';
import { createSafeElement, clearContainer } from '../ui/security.js';
import { setComponentState, showToast } from '../ui/ui.js';

/* -----------------------------------------------------------------------------
 * 1. PURE LOGIC & UTILITY HELPERS (Exported for Unit Testability)
 * ----------------------------------------------------------------------------- */

/**
 * Checks whether the current authenticated session has administrative access.
 * Strict Invariant: JSON role representation is 'Admin' (rejects 'ROLE_' prefixes).
 * 
 * @param {object} [authManagerInstance=authManager] 
 * @returns {boolean}
 */
export function hasAdminAccess(authManagerInstance = authManager) {
  if (!authManagerInstance || typeof authManagerInstance.hasRole !== 'function') {
    return false;
  }
  return authManagerInstance.hasRole('Admin');
}

/**
 * Determines whether a target account represents the currently authenticated administrator.
 * Matches by accountId (if available) or normalized emailOrPhone.
 * 
 * @param {object|null|undefined} targetAccount - AccountResponse item
 * @param {object|null|undefined} currentUser - User identity from authManager.getUser()
 * @returns {boolean}
 */
export function isSelfAccount(targetAccount, currentUser) {
  if (!targetAccount || !currentUser) {
    return false;
  }

  // Compare by account ID if both are available
  if (targetAccount.accountId !== undefined && targetAccount.accountId !== null &&
      currentUser.accountId !== undefined && currentUser.accountId !== null) {
    if (Number(targetAccount.accountId) === Number(currentUser.accountId)) {
      return true;
    }
  }

  // Compare by normalized email or phone
  const targetEmail = typeof targetAccount.emailOrPhone === 'string' ? targetAccount.emailOrPhone.trim().toLowerCase() : '';
  const currentEmail = typeof currentUser.emailOrPhone === 'string' ? currentUser.emailOrPhone.trim().toLowerCase() : '';

  return targetEmail.length > 0 && currentEmail.length > 0 && targetEmail === currentEmail;
}

/**
 * Safely parses and normalizes a single RoleResponse item from GET /api/v1/admin/roles.
 * Contract: roleId (Integer), roleName (String).
 * 
 * @param {any} item 
 * @returns {{ roleId: number, roleName: string }}
 */
export function parseRoleResponse(item) {
  if (!item || typeof item !== 'object') {
    return { roleId: 0, roleName: '' };
  }
  return {
    roleId: Math.max(0, Number(item.roleId) || 0),
    roleName: typeof item.roleName === 'string' ? item.roleName.trim() : ''
  };
}

/**
 * Provides client-side metadata (scholarly Vietnamese display label, description, badge class)
 * for known canonical system roles. Note: Descriptions are client-side copy, not backend data.
 * 
 * @param {string} roleName 
 * @returns {{ label: string, badgeClass: string, description: string, icon: string }}
 */
export function getRoleMetadata(roleName) {
  switch (roleName) {
    case 'Admin':
      return {
        label: 'Quản trị viên',
        badgeClass: 'badge-status badge-status-approved',
        description: 'Toàn quyền quản trị hệ thống, tài khoản người dùng và phân quyền vai trò.',
        icon: '🛡️'
      };
    case 'Creator':
      return {
        label: 'Người sáng tạo',
        badgeClass: 'badge-status badge-status-pending',
        description: 'Biên soạn nội dung bài học, cấu trúc từ vựng và câu hỏi ôn tập.',
        icon: '✍️'
      };
    case 'Moderator':
      return {
        label: 'Kiểm duyệt viên',
        badgeClass: 'badge-status badge-status-active',
        description: 'Kiểm duyệt chất lượng nội dung bài học trước khi phát hành đến học viên.',
        icon: '🔍'
      };
    case 'Learner':
      return {
        label: 'Học viên',
        badgeClass: 'badge-status badge-status-draft',
        description: 'Học tập bộ thủ, từ vựng, bài giảng và thực hiện ôn tập ngắt quãng (SRS).',
        icon: '📚'
      };
    default:
      return {
        label: roleName || 'Chưa định danh',
        badgeClass: 'badge-status badge-status-draft',
        description: 'Vai trò tùy biến trong hệ thống phân quyền.',
        icon: '🏷️'
      };
  }
}

/**
 * Checks if two sets of roles are equal (order-independent, duplicate-free).
 * Used to detect no-op mutations before triggering network requests.
 * 
 * @param {string[]|Set<string>} rolesA 
 * @param {string[]|Set<string>} rolesB 
 * @returns {boolean}
 */
export function areRoleSetsEqual(rolesA, rolesB) {
  const setA = new Set(Array.isArray(rolesA) ? rolesA : Array.from(rolesA || []));
  const setB = new Set(Array.isArray(rolesB) ? rolesB : Array.from(rolesB || []));

  if (setA.size !== setB.size) {
    return false;
  }

  for (const item of setA) {
    if (!setB.has(item)) {
      return false;
    }
  }

  return true;
}

/**
 * Validates role selection prior to submission.
 * Invariants:
 * 1. Selected roles must be non-empty (at least one role required).
 * 2. All selected roles must belong to the verified backend catalog.
 * 3. Self-protection (POL-8D-02): If target is current Admin, 'Admin' role must be present.
 * 
 * @param {string[]} selectedRoles 
 * @param {string[]} availableCatalogRoleNames 
 * @param {boolean} isSelf 
 * @returns {{ valid: boolean, error: string|null }}
 */
export function validateRoleSelection(selectedRoles, availableCatalogRoleNames, isSelf) {
  if (!Array.isArray(selectedRoles) || selectedRoles.length === 0) {
    return {
      valid: false,
      error: 'Tài khoản phải có ít nhất một vai trò trong hệ thống.'
    };
  }

  const catalogSet = new Set(Array.isArray(availableCatalogRoleNames) ? availableCatalogRoleNames : []);

  for (const role of selectedRoles) {
    if (!catalogSet.has(role)) {
      return {
        valid: false,
        error: `Vai trò "${role}" không tồn tại trong danh mục hệ thống.`
      };
    }
  }

  if (isSelf && !selectedRoles.includes('Admin')) {
    return {
      valid: false,
      error: 'Không thể tự tước quyền Quản trị viên (Admin) của chính mình (POL-8D-02).'
    };
  }

  return {
    valid: true,
    error: null
  };
}

/**
 * Builds the complete role replacement payload for PUT /api/v1/admin/accounts/{id}/roles.
 * Invariant: Backend requires the COMPLETE desired role set in one request.
 * 
 * @param {string[]|Iterable<string>} selectedRoles 
 * @returns {{ roles: string[] }}
 */
export function buildRoleUpdatePayload(selectedRoles) {
  const uniqueRoles = Array.from(new Set(selectedRoles || []));
  return {
    roles: uniqueRoles
  };
}

/**
 * Safely parses and normalizes a single AccountResponse item.
 * 
 * @param {any} item 
 * @returns {{
 *   accountId: number,
 *   emailOrPhone: string,
 *   fullName: string|null,
 *   status: string,
 *   roles: string[],
 *   createdAt: string|null,
 *   updatedAt: string|null
 * }}
 */
export function parseAccountResponse(item) {
  if (!item || typeof item !== 'object') {
    return {
      accountId: 0,
      emailOrPhone: '',
      fullName: null,
      status: 'Inactive',
      roles: [],
      createdAt: null,
      updatedAt: null
    };
  }

  const rawStatus = typeof item.status === 'string' ? item.status.trim() : '';
  let normalizedStatus = 'Inactive';
  if (/^active$/i.test(rawStatus)) normalizedStatus = 'Active';
  else if (/^banned$/i.test(rawStatus)) normalizedStatus = 'Banned';
  else if (/^inactive$/i.test(rawStatus)) normalizedStatus = 'Inactive';

  return {
    accountId: Math.max(0, Number(item.accountId) || 0),
    emailOrPhone: typeof item.emailOrPhone === 'string' ? item.emailOrPhone.trim() : '',
    fullName: typeof item.fullName === 'string' && item.fullName.trim().length > 0 ? item.fullName.trim() : null,
    status: normalizedStatus,
    roles: Array.isArray(item.roles) ? [...item.roles] : [],
    createdAt: typeof item.createdAt === 'string' && item.createdAt.trim().length > 0 ? item.createdAt.trim() : null,
    updatedAt: typeof item.updatedAt === 'string' && item.updatedAt.trim().length > 0 ? item.updatedAt.trim() : null
  };
}

/**
 * Safely parses backend PageResponse<AccountResponse> payload.
 * 
 * @param {any} envelopeOrData 
 * @returns {{
 *   items: Array<ReturnType<typeof parseAccountResponse>>,
 *   page: number,
 *   size: number,
 *   totalElements: number,
 *   totalPages: number
 * }}
 */
export function parseAccountsPageResponse(envelopeOrData) {
  if (!envelopeOrData || typeof envelopeOrData !== 'object') {
    return { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
  }

  const data = (envelopeOrData.data && typeof envelopeOrData.data === 'object')
    ? envelopeOrData.data
    : envelopeOrData;

  const rawItems = Array.isArray(data.items) ? data.items : [];
  const items = rawItems.map(parseAccountResponse);

  const page = Math.max(0, Number(data.page) || 0);
  const size = Math.max(1, Number(data.size) || 20);
  const totalElements = typeof data.totalElements === 'number' ? Math.max(0, data.totalElements) : items.length;
  const totalPages = typeof data.totalPages === 'number' ? Math.max(0, data.totalPages) : (totalElements > 0 ? Math.ceil(totalElements / size) : 0);

  return { items, page, size, totalElements, totalPages };
}

/**
 * Constructs clean query parameters for GET /api/v1/admin/accounts.
 * 
 * @param {object} params
 * @param {number} [params.page=0]
 * @param {number} [params.size=20]
 * @param {string} [params.status='']
 * @param {string} [params.search='']
 * @returns {Record<string, string|number>}
 */
export function buildAccountsQueryParams({ page = 0, size = 20, status = '', search = '' } = {}) {
  const numericPage = (page !== undefined && page !== null && !isNaN(Number(page))) ? Number(page) : 0;
  const numericSize = (size !== undefined && size !== null && !isNaN(Number(size))) ? Number(size) : 20;

  const query = {
    page: Math.max(0, numericPage),
    size: Math.max(1, numericSize)
  };

  if (status && typeof status === 'string') {
    const trimmedStatus = status.trim();
    if (['Active', 'Inactive', 'Banned'].includes(trimmedStatus)) {
      query.status = trimmedStatus;
    }
  }

  if (search && typeof search === 'string') {
    const trimmedSearch = search.trim();
    if (trimmedSearch.length > 0) {
      query.search = trimmedSearch;
    }
  }

  return query;
}

/**
 * Returns scholarly Vietnamese label and design-token CSS badge class for an account status.
 * 
 * @param {string|null|undefined} status 
 * @returns {{ label: string, badgeClass: string }}
 */
export function getStatusBadgeMeta(status) {
  switch (status) {
    case 'Active':
      return {
        label: 'Hoạt động',
        badgeClass: 'badge-status badge-status-active'
      };
    case 'Inactive':
      return {
        label: 'Tạm khóa',
        badgeClass: 'badge-status badge-status-inactive'
      };
    case 'Banned':
      return {
        label: 'Bị cấm',
        badgeClass: 'badge-status badge-status-banned'
      };
    default:
      return {
        label: status || 'Không rõ',
        badgeClass: 'badge-status badge-status-draft'
      };
  }
}

/**
 * Computes accessible pagination pages list for rendering.
 * 
 * @param {number} currentPage - 0-indexed current page
 * @param {number} totalPages - total number of pages
 * @param {number} [maxVisible=5] - max visible numbered buttons
 * @returns {Array<number|string>} e.g. [0, 1, 2, '...', 9]
 */
export function calculatePagination(currentPage, totalPages, maxVisible = 5) {
  if (totalPages <= 0) return [];
  if (totalPages <= maxVisible) {
    return Array.from({ length: totalPages }, (_, i) => i);
  }

  const pages = [];
  const half = Math.floor(maxVisible / 2);
  let start = Math.max(0, currentPage - half);
  let end = Math.min(totalPages - 1, start + maxVisible - 1);

  if (end - start + 1 < maxVisible) {
    start = Math.max(0, end - maxVisible + 1);
  }

  if (start > 0) {
    pages.push(0);
    if (start > 1) {
      pages.push('...');
    }
  }

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  if (end < totalPages - 1) {
    if (end < totalPages - 2) {
      pages.push('...');
    }
    pages.push(totalPages - 1);
  }

  return pages;
}

/* -----------------------------------------------------------------------------
 * 2. ADMIN ROLES PAGE CONTROLLER CLASS
 * ----------------------------------------------------------------------------- */

export class AdminRolesController {
  constructor() {
    // State
    this.roleCatalog = []; // Array of { roleId, roleName }
    this.accounts = [];    // Current page accounts
    this.page = 0;
    this.size = 20;
    this.search = '';
    this.totalElements = 0;
    this.totalPages = 0;

    // Modal Interaction State
    this.selectedAccount = null;
    this.initialRoles = [];
    this.pendingRoles = new Set();
    this.isSelf = false;
    this.lastActiveElement = null;
    this.isSubmitting = false;

    // Bound Event Handlers
    this.handleKeyDown = this.handleKeyDown.bind(this);
    this.handleBackdropClick = this.handleBackdropClick.bind(this);
  }

  /**
   * Initializes the controller, verifies RBAC, binds UI events, and loads data.
   */
  async init() {
    // Initialize common session header
    initNavbarAuth();

    // Verify Admin role authorization
    if (!hasAdminAccess(authManager)) {
      this.renderAccessDenied();
      return;
    }

    this.bindDomElements();
    this.bindEvents();

    // Trigger parallel initial data loading
    await Promise.all([
      this.loadRoleCatalog(),
      this.loadAccounts()
    ]);
  }

  /**
   * Shows access denied guard banner if caller lacks Admin role.
   */
  renderAccessDenied() {
    const guardContainer = document.getElementById('adminAuthGuardContainer');
    const workspaceSection = document.getElementById('adminRolesWorkspaceSection');
    if (guardContainer) guardContainer.classList.remove('d-none');
    if (workspaceSection) workspaceSection.classList.add('d-none');
  }

  /**
   * Caches critical DOM references.
   */
  bindDomElements() {
    this.dom = {
      roleCatalogContainer: document.getElementById('roleCatalogContainer'),
      btnRefreshRoleCatalog: document.getElementById('btnRefreshRoleCatalog'),
      accountSearchInput: document.getElementById('accountSearchInput'),
      btnAccountSearch: document.getElementById('btnAccountSearch'),
      btnAccountClearSearch: document.getElementById('btnAccountClearSearch'),
      pageSizeSelect: document.getElementById('pageSizeSelect'),
      accountStateContainer: document.getElementById('accountStateContainer'),
      accountsTableContainer: document.getElementById('accountsTableContainer'),
      accountsTableBody: document.getElementById('accountsTableBody'),
      accountResultCountBadge: document.getElementById('accountResultCountBadge'),
      accountsPaginationNav: document.getElementById('accountsPaginationNav'),
      accountsPaginationSummary: document.getElementById('accountsPaginationSummary'),
      accountsPaginationList: document.getElementById('accountsPaginationList'),

      // Modal Dialog
      modal: document.getElementById('roleAssignmentModal'),
      btnCloseModal: document.getElementById('btnCloseRoleModal'),
      btnCancelModal: document.getElementById('btnCancelRoleModal'),
      btnSaveRoles: document.getElementById('btnSaveRoles'),
      modalTargetEmail: document.getElementById('modalTargetEmail'),
      modalTargetFullName: document.getElementById('modalTargetFullName'),
      modalTargetAccountId: document.getElementById('modalTargetAccountId'),
      modalTargetSelfBadge: document.getElementById('modalTargetSelfBadge'),
      modalCurrentRolesContainer: document.getElementById('modalCurrentRolesContainer'),
      roleCheckboxesContainer: document.getElementById('roleCheckboxesContainer'),
      modalSelfDemotionNotice: document.getElementById('modalSelfDemotionNotice'),
      roleModalWarningNotice: document.getElementById('roleModalWarningNotice'),
      roleModalNoChangeNotice: document.getElementById('roleModalNoChangeNotice'),
      roleModalError: document.getElementById('roleModalError')
    };
  }

  /**
   * Attaches listeners to UI controls.
   */
  bindEvents() {
    // Refresh button
    if (this.dom.btnRefreshRoleCatalog) {
      this.dom.btnRefreshRoleCatalog.addEventListener('click', () => {
        this.loadRoleCatalog();
        this.loadAccounts();
      });
    }

    // Search controls
    if (this.dom.btnAccountSearch) {
      this.dom.btnAccountSearch.addEventListener('click', () => {
        this.page = 0;
        this.search = this.dom.accountSearchInput?.value?.trim() || '';
        this.loadAccounts();
      });
    }

    if (this.dom.accountSearchInput) {
      this.dom.accountSearchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          this.page = 0;
          this.search = this.dom.accountSearchInput.value.trim();
          this.loadAccounts();
        }
      });
    }

    if (this.dom.btnAccountClearSearch) {
      this.dom.btnAccountClearSearch.addEventListener('click', () => {
        if (this.dom.accountSearchInput) {
          this.dom.accountSearchInput.value = '';
        }
        this.search = '';
        this.page = 0;
        this.loadAccounts();
      });
    }

    // Page size dropdown
    if (this.dom.pageSizeSelect) {
      this.dom.pageSizeSelect.addEventListener('change', (e) => {
        this.size = Number(e.target.value) || 20;
        this.page = 0;
        this.loadAccounts();
      });
    }

    // Modal dialog controls
    if (this.dom.btnCloseModal) {
      this.dom.btnCloseModal.addEventListener('click', () => this.closeRoleModal());
    }
    if (this.dom.btnCancelModal) {
      this.dom.btnCancelModal.addEventListener('click', () => this.closeRoleModal());
    }
    if (this.dom.btnSaveRoles) {
      this.dom.btnSaveRoles.addEventListener('click', () => this.submitRoleUpdate());
    }
    const roleForm = document.getElementById('roleAssignmentForm');
    if (roleForm) {
      roleForm.addEventListener('submit', (e) => {
        e.preventDefault();
        this.submitRoleUpdate();
      });
    }

    if (this.dom.modal) {
      this.dom.modal.addEventListener('keydown', this.handleKeyDown);
      this.dom.modal.addEventListener('click', this.handleBackdropClick);
    }
  }

  /**
   * Native dialog escape key handling.
   */
  handleKeyDown(event) {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.closeRoleModal();
    }
  }

  /**
   * Closes modal when user clicks native backdrop.
   */
  handleBackdropClick(event) {
    if (event.target === this.dom.modal) {
      this.closeRoleModal();
    }
  }

  /* ---------------------------------------------------------------------------
   * 3. ROLE CATALOG DATA LOADING & RENDERING
   * --------------------------------------------------------------------------- */

  /**
   * Fetches the server-authoritative role catalog from GET /api/v1/admin/roles.
   */
  async loadRoleCatalog() {
    if (!this.dom.roleCatalogContainer) return;

    clearContainer(this.dom.roleCatalogContainer);
    const loadingCol = createSafeElement('div', {
      className: 'col-12 text-center text-muted py-3 small',
      children: [
        createSafeElement('span', { className: 'spinner-border spinner-border-sm me-2', attrs: { 'aria-hidden': 'true' } }),
        document.createTextNode('Đang nạp danh mục vai trò từ máy chủ...')
      ]
    });
    this.dom.roleCatalogContainer.appendChild(loadingCol);

    try {
      const response = await apiClient('/admin/roles');
      const rawList = Array.isArray(response) ? response : (Array.isArray(response?.data) ? response.data : []);
      this.roleCatalog = rawList.map(parseRoleResponse);
      this.renderRoleCatalog();
    } catch (err) {
      clearContainer(this.dom.roleCatalogContainer);
      const btnRetry = createSafeElement('button', {
        className: 'btn btn-sm btn-outline-danger',
        attrs: { type: 'button' },
        text: 'Thử lại'
      });
      btnRetry.addEventListener('click', () => this.loadRoleCatalog());

      const alert = createSafeElement('div', {
        className: 'alert alert-danger mb-0 small d-flex align-items-center justify-content-between',
        children: [
          createSafeElement('span', { text: `Không thể tải danh mục vai trò: ${err.message || 'Lỗi kết nối máy chủ.'}` }),
          btnRetry
        ]
      });

      const errCol = createSafeElement('div', {
        className: 'col-12 py-3',
        children: [alert]
      });
      this.dom.roleCatalogContainer.appendChild(errCol);
    }
  }

  /**
   * Renders role catalog items into cards.
   */
  renderRoleCatalog() {
    if (!this.dom.roleCatalogContainer) return;
    clearContainer(this.dom.roleCatalogContainer);

    if (this.roleCatalog.length === 0) {
      const emptyCol = createSafeElement('div', {
        className: 'col-12 text-center text-muted py-3 small',
        text: 'Không tìm thấy vai trò nào trên hệ thống.'
      });
      this.dom.roleCatalogContainer.appendChild(emptyCol);
      return;
    }

    for (const role of this.roleCatalog) {
      const meta = getRoleMetadata(role.roleName);

      const titleWrapper = createSafeElement('div', {
        className: 'd-flex align-items-center gap-2',
        children: [
          createSafeElement('span', { className: 'fs-5', attrs: { 'aria-hidden': 'true' }, text: meta.icon }),
          createSafeElement('strong', { className: 'text-dark', text: role.roleName })
        ]
      });

      const idBadge = createSafeElement('span', {
        className: 'badge bg-light text-secondary border small',
        text: `#${role.roleId}`
      });

      const headerDiv = createSafeElement('div', {
        className: 'd-flex align-items-center justify-content-between mb-2',
        children: [titleWrapper, idBadge]
      });

      const labelDiv = createSafeElement('div', {
        className: 'mb-2',
        children: [
          createSafeElement('span', { className: meta.badgeClass, text: meta.label })
        ]
      });

      const descP = createSafeElement('p', {
        className: 'text-secondary small mb-0 mt-auto',
        text: meta.description
      });

      const card = createSafeElement('div', {
        className: 'h-100 p-3 border rounded-3 bg-white shadow-xs d-flex flex-column',
        children: [headerDiv, labelDiv, descP]
      });

      const col = createSafeElement('div', {
        className: 'col-12 col-sm-6 col-lg-3',
        children: [card]
      });

      this.dom.roleCatalogContainer.appendChild(col);
    }
  }

  /* ---------------------------------------------------------------------------
   * 4. ACCOUNT LIST DATA LOADING & RENDERING
   * --------------------------------------------------------------------------- */

  /**
   * Fetches paginated accounts from GET /api/v1/admin/accounts.
   */
  async loadAccounts() {
    if (!this.dom.accountStateContainer) return;

    // Show loading state
    this.dom.accountsTableContainer?.classList.add('d-none');
    this.dom.accountsPaginationNav?.classList.add('d-none');

    setComponentState(this.dom.accountStateContainer, 'LOADING', {
      message: 'Đang tải danh sách tài khoản người dùng...'
    });

    const params = buildAccountsQueryParams({
      page: this.page,
      size: this.size,
      search: this.search
    });

    try {
      const response = await apiClient('/admin/accounts', { params });
      const pageData = parseAccountsPageResponse(response);

      this.accounts = pageData.items;
      this.totalElements = pageData.totalElements;
      this.totalPages = pageData.totalPages;

      this.updateResultBadge();

      if (this.accounts.length === 0) {
        setComponentState(this.dom.accountStateContainer, 'EMPTY', {
          title: 'Không tìm thấy tài khoản',
          description: this.search ? `Không có tài khoản nào khớp với từ khóa "${this.search}".` : 'Chưa có tài khoản người dùng nào trong hệ thống.'
        });
        return;
      }

      setComponentState(this.dom.accountStateContainer, 'READY');
      this.renderTable();
      this.renderPagination();
    } catch (err) {
      setComponentState(this.dom.accountStateContainer, 'ERROR', {
        title: 'Lỗi nạp danh sách tài khoản',
        message: err.message || 'Không thể kết nối đến máy chủ quản trị.',
        onRetry: () => this.loadAccounts()
      });
    }
  }

  /**
   * Updates result count badge.
   */
  updateResultBadge() {
    if (!this.dom.accountResultCountBadge) return;
    this.dom.accountResultCountBadge.textContent = `${this.totalElements} tài khoản`;
  }

  /**
   * Renders accounts table rows.
   */
  renderTable() {
    if (!this.dom.accountsTableBody || !this.dom.accountsTableContainer) return;
    clearContainer(this.dom.accountsTableBody);

    const currentUser = authManager.getUser();

    for (const account of this.accounts) {
      const isSelf = isSelfAccount(account, currentUser);
      const row = createSafeElement('tr', { attrs: { id: `accountRow_${account.accountId}` } });

      // ID
      row.appendChild(createSafeElement('td', {
        className: 'py-3 px-3 text-center text-muted small fw-semibold',
        text: `#${account.accountId}`
      }));

      // Email / Phone
      const emailChildren = [
        createSafeElement('div', { className: 'fw-medium text-dark text-break', text: account.emailOrPhone })
      ];
      if (isSelf) {
        emailChildren.push(
          createSafeElement('span', {
            className: 'badge bg-secondary ms-1 small',
            attrs: { title: 'Tài khoản hiện đang đăng nhập' },
            text: 'Bạn'
          })
        );
      }
      row.appendChild(createSafeElement('td', {
        className: 'py-3 px-3',
        children: emailChildren
      }));

      // Full Name
      row.appendChild(createSafeElement('td', {
        className: 'py-3 px-3 text-secondary',
        text: account.fullName || '—'
      }));

      // Current Roles
      const tdRoles = createSafeElement('td', {
        className: 'py-3 px-3',
        attrs: { id: `accountRolesCell_${account.accountId}` }
      });
      this.renderAccountRolesBadges(tdRoles, account.roles);
      row.appendChild(tdRoles);

      // Status
      const statusMeta = getStatusBadgeMeta(account.status);
      row.appendChild(createSafeElement('td', {
        className: 'py-3 px-2 text-center',
        children: [
          createSafeElement('span', { className: statusMeta.badgeClass, text: statusMeta.label })
        ]
      }));

      // Actions
      const btnEdit = createSafeElement('button', {
        className: 'btn btn-sm btn-outline-primary',
        attrs: {
          type: 'button',
          id: `btnEditRoles_${account.accountId}`,
          'aria-label': `Phân quyền tài khoản ${account.emailOrPhone}`
        },
        text: 'Phân quyền'
      });
      btnEdit.addEventListener('click', () => this.openRoleModal(account));

      row.appendChild(createSafeElement('td', {
        className: 'py-3 px-3 text-center',
        children: [btnEdit]
      }));

      this.dom.accountsTableBody.appendChild(row);
    }

    this.dom.accountsTableContainer.classList.remove('d-none');
  }

  /**
   * Helper to render role badges inside table cell.
   * 
   * @param {HTMLElement} container 
   * @param {string[]} roles 
   */
  renderAccountRolesBadges(container, roles) {
    clearContainer(container);
    if (!Array.isArray(roles) || roles.length === 0) {
      container.appendChild(createSafeElement('span', {
        className: 'badge bg-light text-muted border small',
        text: 'Chưa có vai trò'
      }));
      return;
    }

    const wrapper = createSafeElement('div', { className: 'd-flex flex-wrap gap-1 align-items-center' });
    for (const role of roles) {
      const badgeClass = getRoleBadgeClass(role);
      const label = getRoleLabel(role);
      wrapper.appendChild(createSafeElement('span', {
        className: `${badgeClass} small`,
        text: label
      }));
    }
    container.appendChild(wrapper);
  }

  /**
   * Renders pagination controls.
   */
  renderPagination() {
    if (!this.dom.accountsPaginationNav || !this.dom.accountsPaginationList || !this.dom.accountsPaginationSummary) return;

    if (this.totalPages <= 1) {
      this.dom.accountsPaginationNav.classList.add('d-none');
      return;
    }

    const startItem = this.page * this.size + 1;
    const endItem = Math.min((this.page + 1) * this.size, this.totalElements);
    this.dom.accountsPaginationSummary.textContent = `Hiển thị ${startItem} – ${endItem} trên ${this.totalElements} tài khoản`;

    clearContainer(this.dom.accountsPaginationList);

    // Prev Button
    const btnPrev = createSafeElement('button', {
      className: 'page-link',
      attrs: {
        type: 'button',
        'aria-label': 'Trang trước',
        ...(this.page === 0 ? { disabled: 'true' } : {})
      },
      text: '«'
    });
    if (this.page > 0) {
      btnPrev.addEventListener('click', () => {
        this.page--;
        this.loadAccounts();
      });
    }
    const liPrev = createSafeElement('li', {
      className: `page-item ${this.page === 0 ? 'disabled' : ''}`,
      children: [btnPrev]
    });
    this.dom.accountsPaginationList.appendChild(liPrev);

    // Page Numbers
    const pages = calculatePagination(this.page, this.totalPages);
    for (const p of pages) {
      if (p === '...') {
        this.dom.accountsPaginationList.appendChild(
          createSafeElement('li', {
            className: 'page-item disabled',
            children: [createSafeElement('span', { className: 'page-link', text: '…' })]
          })
        );
      } else {
        const isCurrent = p === this.page;
        const btnPage = createSafeElement('button', {
          className: 'page-link',
          attrs: {
            type: 'button',
            ...(isCurrent ? { 'aria-current': 'page' } : {})
          },
          text: String(p + 1)
        });
        if (!isCurrent) {
          btnPage.addEventListener('click', () => {
            this.page = p;
            this.loadAccounts();
          });
        }
        this.dom.accountsPaginationList.appendChild(
          createSafeElement('li', {
            className: `page-item ${isCurrent ? 'active' : ''}`,
            children: [btnPage]
          })
        );
      }
    }

    // Next Button
    const btnNext = createSafeElement('button', {
      className: 'page-link',
      attrs: {
        type: 'button',
        'aria-label': 'Trang sau',
        ...(this.page >= this.totalPages - 1 ? { disabled: 'true' } : {})
      },
      text: '»'
    });
    if (this.page < this.totalPages - 1) {
      btnNext.addEventListener('click', () => {
        this.page++;
        this.loadAccounts();
      });
    }
    const liNext = createSafeElement('li', {
      className: `page-item ${this.page >= this.totalPages - 1 ? 'disabled' : ''}`,
      children: [btnNext]
    });
    this.dom.accountsPaginationList.appendChild(liNext);

    this.dom.accountsPaginationNav.classList.remove('d-none');
  }

  /* ---------------------------------------------------------------------------
   * 5. ROLE ASSIGNMENT MODAL WORKFLOW
   * --------------------------------------------------------------------------- */

  /**
   * Opens role assignment modal dialog for target account.
   * 
   * @param {ReturnType<typeof parseAccountResponse>} account 
   */
  openRoleModal(account) {
    if (!this.dom.modal) return;

    this.selectedAccount = account;
    this.lastActiveElement = document.activeElement;
    this.isSelf = isSelfAccount(account, authManager.getUser());
    this.initialRoles = [...account.roles];
    this.pendingRoles = new Set(account.roles);

    // Populate target identity previews
    if (this.dom.modalTargetEmail) this.dom.modalTargetEmail.textContent = account.emailOrPhone;
    if (this.dom.modalTargetFullName) this.dom.modalTargetFullName.textContent = account.fullName || 'Chưa cập nhật';
    if (this.dom.modalTargetAccountId) this.dom.modalTargetAccountId.textContent = `#${account.accountId}`;

    // Self badge
    if (this.dom.modalTargetSelfBadge) {
      if (this.isSelf) {
        this.dom.modalTargetSelfBadge.classList.remove('d-none');
      } else {
        this.dom.modalTargetSelfBadge.classList.add('d-none');
      }
    }

    // Render current server roles preview badges
    if (this.dom.modalCurrentRolesContainer) {
      this.renderAccountRolesBadges(this.dom.modalCurrentRolesContainer, this.initialRoles);
    }

    // Self-demotion defense notice (POL-8D-02)
    if (this.dom.modalSelfDemotionNotice) {
      if (this.isSelf) {
        this.dom.modalSelfDemotionNotice.classList.remove('d-none');
      } else {
        this.dom.modalSelfDemotionNotice.classList.add('d-none');
      }
    }

    // Clear feedback alerts
    this.hideModalError();
    this.updateNoChangeNotice();

    // Render checkboxes from authoritative role catalog
    this.renderRoleCheckboxes();

    // Show native modal
    this.dom.modal.showModal();

    // Set sensible focus: first editable checkbox or cancel button
    const firstCheckbox = this.dom.roleCheckboxesContainer?.querySelector('input[type="checkbox"]:not(:disabled)');
    if (firstCheckbox) {
      firstCheckbox.focus();
    } else if (this.dom.btnCancelModal) {
      this.dom.btnCancelModal.focus();
    }
  }

  /**
   * Renders accessible checkboxes for each catalog role.
   */
  renderRoleCheckboxes() {
    if (!this.dom.roleCheckboxesContainer) return;
    clearContainer(this.dom.roleCheckboxesContainer);

    if (this.roleCatalog.length === 0) {
      const pWarn = createSafeElement('p', {
        className: 'text-danger small mb-0',
        text: 'Chưa tải được danh mục vai trò. Vui lòng tải lại trang.'
      });
      this.dom.roleCheckboxesContainer.appendChild(pWarn);
      return;
    }

    for (const role of this.roleCatalog) {
      const meta = getRoleMetadata(role.roleName);
      const isSelected = this.pendingRoles.has(role.roleName);
      const isLockedAdmin = this.isSelf && role.roleName === 'Admin';

      const checkboxAttrs = {
        type: 'checkbox',
        id: `roleCheckbox_${role.roleName}`,
        name: 'selectedRoles',
        value: role.roleName
      };
      if (isLockedAdmin) {
        checkboxAttrs['aria-describedby'] = 'modalSelfDemotionNotice';
      }

      const checkbox = createSafeElement('input', {
        className: 'form-check-input mt-1 ms-0',
        attrs: checkboxAttrs
      });

      if (isSelected || isLockedAdmin) {
        checkbox.checked = true;
      }
      if (isLockedAdmin) {
        checkbox.disabled = true;
      }

      // Checkbox Change Listener
      checkbox.addEventListener('change', (e) => {
        if (isLockedAdmin) return; // Invariant defense

        if (e.target.checked) {
          this.pendingRoles.add(role.roleName);
        } else {
          this.pendingRoles.delete(role.roleName);
        }

        this.onRoleSelectionChanged();
      });

      // Label Wrapper
      const titleChildren = [
        createSafeElement('strong', { className: 'text-dark', text: role.roleName }),
        createSafeElement('span', { className: meta.badgeClass, text: meta.label })
      ];

      if (isLockedAdmin) {
        titleChildren.push(
          createSafeElement('span', { className: 'badge bg-warning text-dark small', text: 'Cố định (POL-8D-02)' })
        );
      }

      const label = createSafeElement('label', {
        className: 'form-check-label flex-grow-1 cursor-pointer',
        attrs: { for: `roleCheckbox_${role.roleName}` },
        children: [
          createSafeElement('div', {
            className: 'd-flex align-items-center gap-2 flex-wrap',
            children: titleChildren
          }),
          createSafeElement('small', {
            className: 'text-secondary d-block mt-1',
            text: meta.description
          })
        ]
      });

      const itemWrapper = createSafeElement('div', {
        className: 'form-check p-2 rounded-2 border mb-1 bg-light d-flex align-items-start gap-2',
        children: [checkbox, label]
      });

      this.dom.roleCheckboxesContainer.appendChild(itemWrapper);
    }
  }

  /**
   * Invoked whenever any role checkbox changes state.
   */
  onRoleSelectionChanged() {
    this.hideModalError();
    this.updateNoChangeNotice();
  }

  /**
   * Toggles no-change indicator and adjusts save button state.
   */
  updateNoChangeNotice() {
    const selectedArray = Array.from(this.pendingRoles);
    const isUnchanged = areRoleSetsEqual(selectedArray, this.initialRoles);

    if (this.dom.roleModalNoChangeNotice) {
      if (isUnchanged) {
        this.dom.roleModalNoChangeNotice.classList.remove('d-none');
      } else {
        this.dom.roleModalNoChangeNotice.classList.add('d-none');
      }
    }
  }

  /**
   * Displays an error alert inside the modal.
   * 
   * @param {string} message 
   */
  showModalError(message) {
    if (!this.dom.roleModalError) return;
    this.dom.roleModalError.textContent = message;
    this.dom.roleModalError.classList.remove('d-none');
    this.dom.roleModalError.focus();
  }

  /**
   * Hides the error alert inside the modal.
   */
  hideModalError() {
    if (!this.dom.roleModalError) return;
    this.dom.roleModalError.textContent = '';
    this.dom.roleModalError.classList.add('d-none');
  }

  /**
   * Submits the complete desired role set to PUT /api/v1/admin/accounts/{id}/roles.
   */
  async submitRoleUpdate() {
    if (this.isSubmitting || !this.selectedAccount) return;

    // Build desired role array
    const selectedArray = Array.from(this.pendingRoles);

    // Defense-in-depth: For self, ensure 'Admin' is always included
    if (this.isSelf && !selectedArray.includes('Admin')) {
      selectedArray.push('Admin');
    }

    // 1. Validation
    const catalogRoleNames = this.roleCatalog.map(r => r.roleName);
    const validation = validateRoleSelection(selectedArray, catalogRoleNames, this.isSelf);
    if (!validation.valid) {
      this.showModalError(validation.error);
      return;
    }

    // 2. No-op detection
    if (areRoleSetsEqual(selectedArray, this.initialRoles)) {
      this.updateNoChangeNotice();
      this.showModalError('Tập hợp vai trò không thay đổi. Không cần gửi yêu cầu cập nhật.');
      return;
    }

    // 3. Prepare payload and lock UI
    this.isSubmitting = true;
    this.hideModalError();
    if (this.dom.btnSaveRoles) {
      this.dom.btnSaveRoles.disabled = true;
      this.dom.btnSaveRoles.textContent = 'Đang lưu...';
    }
    if (this.dom.btnCancelModal) {
      this.dom.btnCancelModal.disabled = true;
    }

    const payload = buildRoleUpdatePayload(selectedArray);

    try {
      // 4. Send PUT request
      const response = await apiClient(`/admin/accounts/${this.selectedAccount.accountId}/roles`, {
        method: 'PUT',
        body: payload
      });

      const updatedAccount = parseAccountResponse(response.data || response);

      // 5. Authoritative state synchronization
      this.selectedAccount.roles = updatedAccount.roles;

      // Update in accounts page list
      const existingAccount = this.accounts.find(a => a.accountId === this.selectedAccount.accountId);
      if (existingAccount) {
        existingAccount.roles = updatedAccount.roles;
      }

      // Update table cell badges immediately
      const cell = document.getElementById(`accountRolesCell_${this.selectedAccount.accountId}`);
      if (cell) {
        this.renderAccountRolesBadges(cell, updatedAccount.roles);
      }

      // Show authoritative feedback
      showToast('Cập nhật phân quyền thành công. Máy chủ đã cập nhật phiên làm việc của tài khoản.', 'success');

      // Close modal
      this.closeRoleModal();
    } catch (err) {
      const errMsg = err.message || (err.data && err.data.message) || 'Có lỗi xảy ra khi lưu phân quyền vai trò.';
      this.showModalError(errMsg);
    } finally {
      this.isSubmitting = false;
      if (this.dom.btnSaveRoles) {
        this.dom.btnSaveRoles.disabled = false;
        this.dom.btnSaveRoles.textContent = 'Lưu phân quyền';
      }
      if (this.dom.btnCancelModal) {
        this.dom.btnCancelModal.disabled = false;
      }
    }
  }

  /**
   * Closes the role assignment dialog and restores accessibility focus.
   */
  closeRoleModal() {
    if (!this.dom.modal) return;
    this.dom.modal.close();
    this.selectedAccount = null;
    this.pendingRoles.clear();
    this.initialRoles = [];

    // Restore focus
    if (this.lastActiveElement && typeof this.lastActiveElement.focus === 'function') {
      this.lastActiveElement.focus();
    }
  }
}

/* -----------------------------------------------------------------------------
 * 6. APPLICATION BOOTSTRAP
 * ----------------------------------------------------------------------------- */

/**
 * Page initialization entry point.
 * Guarded against execution when imported on non-admin-roles pages.
 * 
 * @returns {AdminRolesController|null}
 */
export function initAdminRolesPage() {
  if (typeof document === 'undefined') return null;
  const root = document.getElementById('adminRolesWorkspaceSection') || document.getElementById('rolesAccountsTable');
  if (!root) return null;

  const controller = new AdminRolesController();
  controller.init();
  return controller;
}

if (typeof window !== 'undefined' && typeof document !== 'undefined') {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      initAdminRolesPage();
    });
  } else {
    initAdminRolesPage();
  }
}



