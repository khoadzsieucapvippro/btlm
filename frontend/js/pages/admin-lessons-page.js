/**
 * =============================================================================
 * ADMIN SYSTEM-WIDE LESSON OVERSIGHT CONTROLLER (TASK 9F.5)
 * Module: frontend/js/pages/admin-lessons-page.js
 *
 * Responsibilities:
 * - Integration with authoritative backend endpoints:
 *   * GET /api/v1/admin/lessons (Paginated system-wide lessons with status filter)
 *   * GET /api/v1/lessons/{id} (Public lesson detail for 'Approved' lessons)
 *   * GET /api/v1/moderator/lessons/{id} (Moderator detail for 'Pending' lessons)
 * - Strict Invariant: This page is 100% READ-ONLY. ZERO lesson mutations
 *   (No create, edit, delete, approve, reject, reorder, submit).
 * - Contract Limitations Respected:
 *   * Creator filter: BLOCKED BY CURRENT SEALED BACKEND CONTRACT (Omitted from UI).
 *   * Search parameter: Not supported by /admin/lessons (Omitted from UI).
 *   * Draft/Rejected detail: No universal admin detail endpoint exists;
 *     UI displays available summary data and a truthful contract limitation notice.
 * - Global Status Counts: Computed via server totalElements using lightweight
 *   size=1 probes executed in parallel; never calculated from single page slice.
 * - Role-guarded access: strictly accessible to 'Admin' (authManager.hasRole('Admin')).
 * - Safe DOM construction (zero unsafe innerHTML) & native HTML5 <dialog> modal.
 * =============================================================================
 */

import { apiClient, ApiError } from '../api/api.js';
import { authManager } from '../auth/auth-state.js';
import { initNavbarAuth } from '../ui/nav.js';
import { createSafeElement, clearContainer, sanitizeResourceUrl } from '../ui/security.js';
import { setComponentState, showToast } from '../ui/ui.js';

/* -----------------------------------------------------------------------------
 * 1. PURE LOGIC & UTILITY HELPERS (Exported for Unit Testing)
 * ----------------------------------------------------------------------------- */

/**
 * Valid lifecycle statuses supported by backend.
 */
export const VALID_LESSON_STATUSES = Object.freeze(['Draft', 'Pending', 'Approved', 'Rejected']);

/**
 * Verifies whether the current authenticated session has administrative access.
 * Strict Invariant: JSON role representation is canonical 'Admin' (rejects 'ROLE_' prefixes).
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
 * Normalizes a status filter string to a valid backend lifecycle status or empty string.
 *
 * @param {string|null|undefined} status
 * @returns {string} canonical status ('Draft', 'Pending', 'Approved', 'Rejected') or ''
 */
export function normalizeStatusFilter(status) {
  if (!status || typeof status !== 'string') return '';
  const trimmed = status.trim();
  if (trimmed.toLowerCase() === 'all' || trimmed === '') return '';
  const match = VALID_LESSON_STATUSES.find(s => s.toLowerCase() === trimmed.toLowerCase());
  return match || '';
}

/**
 * Maps a lesson status string to its Vietnamese human-friendly label.
 *
 * @param {string|null|undefined} status
 * @returns {string}
 */
export function mapStatusLabel(status) {
  if (!status || typeof status !== 'string') return 'Không xác định';
  switch (status.trim().toLowerCase()) {
    case 'draft':
      return 'Bản nháp';
    case 'pending':
      return 'Chờ duyệt';
    case 'approved':
      return 'Đã duyệt';
    case 'rejected':
      return 'Bị từ chối';
    default:
      return status;
  }
}

/**
 * Maps a lesson status to its Bootstrap badge presentation attributes.
 *
 * @param {string|null|undefined} status
 * @returns {{ className: string, label: string }}
 */
export function mapStatusBadge(status) {
  const label = mapStatusLabel(status);
  if (!status || typeof status !== 'string') {
    return { className: 'badge bg-light text-dark border', label };
  }
  switch (status.trim().toLowerCase()) {
    case 'draft':
      return { className: 'badge bg-secondary-subtle text-secondary border', label };
    case 'pending':
      return { className: 'badge bg-warning-subtle text-dark border border-warning', label };
    case 'approved':
      return { className: 'badge bg-success-subtle text-success border border-success', label };
    case 'rejected':
      return { className: 'badge bg-danger-subtle text-danger border border-danger', label };
    default:
      return { className: 'badge bg-light text-dark border', label };
  }
}

/**
 * Builds query parameters for GET /api/v1/admin/lessons.
 * Omits 'status' if empty or 'All'.
 *
 * @param {object} options
 * @param {string} [options.status]
 * @param {number} [options.page=0]
 * @param {number} [options.size=20]
 * @returns {Record<string, string|number>}
 */
export function buildAdminLessonsParams(statusOrOptions = '', pageArg = 0, sizeArg = 20) {
  let status = '';
  let page = 0;
  let size = 20;

  if (statusOrOptions && typeof statusOrOptions === 'object') {
    status = statusOrOptions.status ?? '';
    page = statusOrOptions.page ?? 0;
    size = statusOrOptions.size ?? 20;
  } else {
    status = statusOrOptions ?? '';
    page = pageArg;
    size = sizeArg;
  }

  const normStatus = normalizeStatusFilter(status);
  const numSize = Number(size);
  const safeSize = (!Number.isFinite(numSize) || numSize <= 0) ? 20 : Math.floor(numSize);
  const params = {
    page: Math.max(0, Number(page) || 0),
    size: safeSize
  };
  if (normStatus) {
    params.status = normStatus;
  }
  return params;
}

/**
 * Determines the detail retrieval strategy based on lesson status.
 * Reflects current verified backend endpoint availability:
 * - 'Approved': Public endpoint GET /api/v1/lessons/{id}
 * - 'Pending': Moderator endpoint GET /api/v1/moderator/lessons/{id} (Admin is authorized)
 * - 'Draft' / 'Rejected': No universal admin detail endpoint in backend
 *
 * @param {string|null|undefined} status
 * @param {string|number|null} [lessonId=null]
 * @returns {{ strategy: 'APPROVED' | 'PENDING' | 'UNSUPPORTED', canFetchDetail: boolean, endpoint: string | null, limitationNotice: string | null }}
 */
export function determineDetailStrategy(status, lessonId = null) {
  const norm = normalizeStatusFilter(status);
  if (norm === 'Approved') {
    return {
      strategy: 'APPROVED',
      canFetchDetail: true,
      endpoint: lessonId ? `/lessons/${lessonId}` : '/lessons/{id}',
      limitationNotice: null
    };
  }
  if (norm === 'Pending') {
    return {
      strategy: 'PENDING',
      canFetchDetail: true,
      endpoint: lessonId ? `/moderator/lessons/${lessonId}` : '/moderator/lessons/{id}',
      limitationNotice: null
    };
  }
  return {
    strategy: 'UNSUPPORTED',
    canFetchDetail: false,
    endpoint: null,
    limitationNotice: `Nội dung chi tiết của bài học ở trạng thái "${mapStatusLabel(norm) || 'này'}" chưa có endpoint quản trị trong backend hiện tại.`
  };
}

/**
 * Formats an ISO date/timestamp string into a human-friendly Vietnamese format.
 *
 * @param {string|null|undefined} isoString
 * @returns {string}
 */
export function formatLessonDate(isoString) {
  if (!isoString) return '—';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch {
    return '—';
  }
}

/**
 * Calculates pagination boundaries and metadata.
 *
 * @param {number} page 0-indexed current page
 * @param {number} size page size
 * @param {number} totalElements total records in dataset
 * @returns {{ start: number, end: number, total: number, totalPages: number, currentPage: number, hasPrevious: boolean, hasNext: boolean }}
 */
export function calculatePaginationSummary(page, size, totalElements) {
  const safeTotal = Math.max(0, Number(totalElements) || 0);
  const safeSize = Math.max(1, Number(size) || 20);
  const totalPages = Math.max(1, Math.ceil(safeTotal / safeSize));
  const currentPage = Math.max(0, Math.min(Number(page) || 0, totalPages - 1));

  if (safeTotal === 0) {
    return {
      start: 0,
      end: 0,
      total: 0,
      totalPages: 1,
      currentPage: 0,
      hasPrevious: false,
      hasNext: false
    };
  }

  const start = currentPage * safeSize + 1;
  const end = Math.min(start + safeSize - 1, safeTotal);

  return {
    start,
    end,
    total: safeTotal,
    totalPages,
    currentPage,
    hasPrevious: currentPage > 0,
    hasNext: currentPage < totalPages - 1
  };
}

/**
 * Extracts status counts from a settled array of probe responses or response object.
 *
 * @param {Array<{ status: 'fulfilled'|'rejected', value?: any }> | Record<string, any>} results
 * @returns {{ total: number|null, draft: number|null, pending: number|null, approved: number|null, rejected: number|null }}
 */
export function extractStatusCounts(results) {
  const extractTotal = (res) => {
    if (!res) return null;
    if (res.status === 'fulfilled' && res.value) {
      const data = res.value.data || res.value;
      return typeof data?.totalElements === 'number' ? data.totalElements : null;
    }
    const data = res.data || res;
    if (typeof data?.totalElements === 'number') {
      return data.totalElements;
    }
    return null;
  };

  if (Array.isArray(results)) {
    return {
      total: extractTotal(results[0]),
      draft: extractTotal(results[1]),
      pending: extractTotal(results[2]),
      approved: extractTotal(results[3]),
      rejected: extractTotal(results[4])
    };
  }

  if (results && typeof results === 'object') {
    return {
      total: extractTotal(results.total),
      draft: extractTotal(results.draft),
      pending: extractTotal(results.pending),
      approved: extractTotal(results.approved),
      rejected: extractTotal(results.rejected)
    };
  }

  return { total: null, draft: null, pending: null, approved: null, rejected: null };
}

/**
 * Parses and normalizes a PageResponse structure from the backend API.
 *
 * @param {any} data
 * @returns {{ items: any[], page: number, size: number, totalElements: number, totalPages: number, first: boolean, last: boolean }}
 */
export function parsePageResponse(data) {
  if (!data) {
    return { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true };
  }
  const payload = data.data || data;
  const items = Array.isArray(payload.items)
    ? payload.items
    : (Array.isArray(payload.content) ? payload.content : []);
  const page = Number(payload.page ?? payload.number ?? 0);
  const size = Number(payload.size ?? 20);
  const totalElements = Number(payload.totalElements ?? items.length);
  const totalPages = Number(payload.totalPages ?? (totalElements > 0 ? Math.ceil(totalElements / size) : 0));
  const first = payload.first !== undefined ? Boolean(payload.first) : page === 0;
  const last = payload.last !== undefined ? Boolean(payload.last) : page >= totalPages - 1;

  return { items, page, size, totalElements, totalPages, first, last };
}

/* -----------------------------------------------------------------------------
 * 2. MAIN PAGE CONTROLLER CLASS
 * ----------------------------------------------------------------------------- */

export class AdminLessonsPageController {
  constructor() {
    this.elements = {};
    this.currentStatus = '';
    this.currentPage = 0;
    this.pageSize = 20;
    this.totalElements = 0;
    this.totalPages = 0;
    this.currentLessons = [];
    this.kpiCounts = { total: null, draft: null, pending: null, approved: null, rejected: null };
    this.isKpiLoading = false;
    this.lastFocusedElement = null;
  }

  /**
   * Initializes the controller lifecycle.
   */
  async init() {
    initNavbarAuth();
    this.cacheElements();

    if (!hasAdminAccess(authManager)) {
      this.renderAccessDenied();
      return;
    }

    this.bindEvents();
    // Load initial data in parallel
    await Promise.allSettled([
      this.loadStatusCounts(),
      this.loadLessons()
    ]);
  }

  /**
   * Caches DOM element references.
   */
  cacheElements() {
    this.elements = {
      // Guard & Content containers
      authGuardContainer: document.getElementById('adminAuthGuardContainer'),
      authGuardMessage: document.getElementById('adminAuthGuardMessage'),
      mainContent: document.getElementById('adminLessonsMainContent'),

      // KPI Count cards & badges
      btnRefreshKpi: document.getElementById('btnRefreshKpi'),
      refreshKpiSpinner: document.getElementById('refreshKpiSpinner'),
      kpiCardTotal: document.getElementById('kpiCardTotal'),
      kpiCountTotal: document.getElementById('kpiCountTotal'),
      kpiCardDraft: document.getElementById('kpiCardDraft'),
      kpiCountDraft: document.getElementById('kpiCountDraft'),
      kpiCardPending: document.getElementById('kpiCardPending'),
      kpiCountPending: document.getElementById('kpiCountPending'),
      kpiCardApproved: document.getElementById('kpiCardApproved'),
      kpiCountApproved: document.getElementById('kpiCountApproved'),
      kpiCardRejected: document.getElementById('kpiCardRejected'),
      kpiCountRejected: document.getElementById('kpiCountRejected'),

      // Status filter buttons
      filterStatusAll: document.getElementById('filterStatusAll'),
      filterStatusDraft: document.getElementById('filterStatusDraft'),
      filterStatusPending: document.getElementById('filterStatusPending'),
      filterStatusApproved: document.getElementById('filterStatusApproved'),
      filterStatusRejected: document.getElementById('filterStatusRejected'),

      // Page size & result summary
      pageSizeSelect: document.getElementById('pageSizeSelect'),
      resultCountBadge: document.getElementById('lessonResultCount'),

      // Table & State containers
      stateContainer: document.getElementById('lessonStateContainer'),
      tableCard: document.getElementById('lessonTableCard'),
      table: document.getElementById('lessonTable'),
      tableBody: document.getElementById('lessonTableBody'),
      pagination: document.getElementById('lessonPagination'),
      paginationSummary: document.getElementById('paginationSummary'),
      paginationControls: document.getElementById('paginationControls'),

      // Detail Modal Elements
      detailModal: document.getElementById('lessonDetailModal'),
      modalDetailCloseBtn: document.getElementById('modalDetailCloseBtn'),
      btnCancelDetailModal: document.getElementById('btnCancelDetailModal'),
      modalDetailTitle: document.getElementById('modalDetailTitle'),
      modalDetailDesc: document.getElementById('modalDetailDesc'),
      detailLessonTitle: document.getElementById('detailLessonTitle'),
      detailLessonStatusBadge: document.getElementById('detailLessonStatusBadge'),
      detailLessonId: document.getElementById('detailLessonId'),
      detailLessonTimestamp: document.getElementById('detailLessonTimestamp'),
      detailContractLimitationAlert: document.getElementById('detailContractLimitationAlert'),
      detailContractLimitationMessage: document.getElementById('detailContractLimitationMessage'),
      detailVocabLoadingPrompt: document.getElementById('detailVocabLoadingPrompt'),
      detailErrorAlert: document.getElementById('detailErrorAlert'),
      detailErrorMessage: document.getElementById('detailErrorMessage'),
      detailVocabSection: document.getElementById('detailVocabSection'),
      detailVocabCount: document.getElementById('detailVocabCount'),
      detailVocabEmptyPrompt: document.getElementById('detailVocabEmptyPrompt'),
      detailVocabList: document.getElementById('detailVocabList')
    };
  }

  /**
   * Renders access denied state when non-Admin accesses the page.
   */
  renderAccessDenied() {
    if (this.elements.authGuardContainer) {
      this.elements.authGuardContainer.classList.remove('d-none');
    }
    if (this.elements.mainContent) {
      this.elements.mainContent.classList.add('d-none');
    }
  }

  /**
   * Binds UI event listeners.
   */
  bindEvents() {
    // 1. Status Filter Buttons
    const filterButtons = [
      this.elements.filterStatusAll,
      this.elements.filterStatusDraft,
      this.elements.filterStatusPending,
      this.elements.filterStatusApproved,
      this.elements.filterStatusRejected
    ].filter(Boolean);

    for (const btn of filterButtons) {
      btn.addEventListener('click', (e) => {
        const targetStatus = e.currentTarget.getAttribute('data-status') || '';
        this.setStatusFilter(targetStatus);
      });
    }

    // 2. KPI Cards Clicking triggers filtering
    const kpiCards = [
      this.elements.kpiCardTotal,
      this.elements.kpiCardDraft,
      this.elements.kpiCardPending,
      this.elements.kpiCardApproved,
      this.elements.kpiCardRejected
    ].filter(Boolean);

    for (const card of kpiCards) {
      card.addEventListener('click', (e) => {
        const targetStatus = e.currentTarget.getAttribute('data-status') || '';
        this.setStatusFilter(targetStatus);
      });
    }

    // 3. Page Size Selector
    if (this.elements.pageSizeSelect) {
      this.elements.pageSizeSelect.addEventListener('change', (e) => {
        this.pageSize = Math.max(1, Number(e.target.value) || 20);
        this.currentPage = 0;
        this.loadLessons();
      });
    }

    // 4. Refresh KPI Button
    if (this.elements.btnRefreshKpi) {
      this.elements.btnRefreshKpi.addEventListener('click', () => {
        this.loadStatusCounts();
      });
    }

    // 5. Detail Modal Controls
    if (this.elements.modalDetailCloseBtn) {
      this.elements.modalDetailCloseBtn.addEventListener('click', () => {
        this.closeDetailModal();
      });
    }
    if (this.elements.btnCancelDetailModal) {
      this.elements.btnCancelDetailModal.addEventListener('click', () => {
        this.closeDetailModal();
      });
    }

    if (this.elements.detailModal) {
      // Backdrop click closes dialog
      this.elements.detailModal.addEventListener('click', (e) => {
        const rect = this.elements.detailModal.getBoundingClientRect();
        const isInDialog = (
          rect.top <= e.clientY && e.clientY <= rect.top + rect.height &&
          rect.left <= e.clientX && e.clientX <= rect.left + rect.width
        );
        if (!isInDialog) {
          this.closeDetailModal();
        }
      });

      // Escape key handles focus restoration
      this.elements.detailModal.addEventListener('cancel', (e) => {
        e.preventDefault();
        this.closeDetailModal();
      });
    }
  }

  /**
   * Sets status filter, updates button styles, resets page to 0 and reloads.
   *
   * @param {string} status
   */
  setStatusFilter(status) {
    const normalized = normalizeStatusFilter(status);
    if (this.currentStatus === normalized && this.currentPage === 0) return;

    this.currentStatus = normalized;
    this.currentPage = 0;
    this.updateFilterButtonStyles();
    this.loadLessons();
  }

  /**
   * Updates visual active states of filter buttons.
   */
  updateFilterButtonStyles() {
    const map = [
      { btn: this.elements.filterStatusAll, status: '' },
      { btn: this.elements.filterStatusDraft, status: 'Draft' },
      { btn: this.elements.filterStatusPending, status: 'Pending' },
      { btn: this.elements.filterStatusApproved, status: 'Approved' },
      { btn: this.elements.filterStatusRejected, status: 'Rejected' }
    ];

    for (const item of map) {
      if (!item.btn) continue;
      const isActive = item.status === this.currentStatus;
      if (isActive) {
        item.btn.className = 'btn btn-sm btn-chinese-primary active';
      } else {
        item.btn.className = 'btn btn-sm btn-outline-secondary';
      }
    }
  }

  /**
   * Loads global status counts in parallel using lightweight size=1 requests.
   * Does NOT derive counts from the current single table page.
   */
  async loadStatusCounts() {
    if (this.isKpiLoading) return;
    this.isKpiLoading = true;

    if (this.elements.refreshKpiSpinner) {
      this.elements.refreshKpiSpinner.classList.remove('d-none');
    }

    try {
      const probeRequests = [
        apiClient('/admin/lessons', { params: { size: 1 } }),
        apiClient('/admin/lessons', { params: { status: 'Draft', size: 1 } }),
        apiClient('/admin/lessons', { params: { status: 'Pending', size: 1 } }),
        apiClient('/admin/lessons', { params: { status: 'Approved', size: 1 } }),
        apiClient('/admin/lessons', { params: { status: 'Rejected', size: 1 } })
      ];

      const settled = await Promise.allSettled(probeRequests);
      const counts = extractStatusCounts(settled);
      this.kpiCounts = counts;

      const formatCount = (val) => (typeof val === 'number' ? val.toLocaleString('vi-VN') : '—');

      if (this.elements.kpiCountTotal) this.elements.kpiCountTotal.textContent = formatCount(counts.total);
      if (this.elements.kpiCountDraft) this.elements.kpiCountDraft.textContent = formatCount(counts.draft);
      if (this.elements.kpiCountPending) this.elements.kpiCountPending.textContent = formatCount(counts.pending);
      if (this.elements.kpiCountApproved) this.elements.kpiCountApproved.textContent = formatCount(counts.approved);
      if (this.elements.kpiCountRejected) this.elements.kpiCountRejected.textContent = formatCount(counts.rejected);
    } catch (err) {
      console.warn('Failed to refresh status counts:', err);
    } finally {
      this.isKpiLoading = false;
      if (this.elements.refreshKpiSpinner) {
        this.elements.refreshKpiSpinner.classList.add('d-none');
      }
    }
  }

  /**
   * Loads paginated lesson list via GET /api/v1/admin/lessons.
   */
  async loadLessons() {
    if (!this.elements.stateContainer) return;

    setComponentState(this.elements.stateContainer, 'LOADING', {
      message: 'Đang tải danh sách bài học toàn hệ thống...'
    });

    if (this.elements.tableCard) {
      this.elements.tableCard.classList.add('d-none');
    }

    try {
      const params = buildAdminLessonsParams({
        status: this.currentStatus,
        page: this.currentPage,
        size: this.pageSize
      });

      const response = await apiClient('/admin/lessons', { params });
      const data = response.data || response;

      this.currentLessons = Array.isArray(data.items) ? data.items : [];
      this.totalElements = Number(data.totalElements) || 0;
      this.totalPages = Math.max(1, Number(data.totalPages) || 1);
      this.currentPage = Number(data.page) || 0;
      this.pageSize = Number(data.size) || this.pageSize;

      this.updateResultBadge();

      if (this.totalElements === 0) {
        const statusLabel = this.currentStatus ? ` ở trạng thái "${mapStatusLabel(this.currentStatus)}"` : '';
        setComponentState(this.elements.stateContainer, 'EMPTY', {
          title: 'Không tìm thấy bài học',
          message: `Hiện không có bài học nào${statusLabel} trong hệ thống.`
        });
      } else {
        setComponentState(this.elements.stateContainer, 'READY');
        if (this.elements.tableCard) {
          this.elements.tableCard.classList.remove('d-none');
        }
        this.renderTableRows();
        this.renderPagination();
      }
    } catch (err) {
      setComponentState(this.elements.stateContainer, 'ERROR', {
        title: 'Lỗi nạp danh sách bài học',
        message: err?.message || 'Không thể kết nối đến máy chủ. Vui lòng thử lại.',
        onRetry: () => this.loadLessons()
      });
    }
  }

  /**
   * Updates result count status badge.
   */
  updateResultBadge() {
    if (!this.elements.resultCountBadge) return;
    if (this.totalElements === 0) {
      this.elements.resultCountBadge.textContent = '0 bài học';
      this.elements.resultCountBadge.className = 'badge-status badge-status-draft text-nowrap';
    } else {
      this.elements.resultCountBadge.textContent = `${this.totalElements.toLocaleString('vi-VN')} bài học`;
      this.elements.resultCountBadge.className = 'badge-status badge-status-published text-nowrap';
    }
  }

  /**
   * Renders semantic table rows using safe DOM elements.
   */
  renderTableRows() {
    if (!this.elements.tableBody) return;
    clearContainer(this.elements.tableBody);

    for (const lesson of this.currentLessons) {
      const tr = createSafeElement('tr', {
        attrs: { id: `lessonRow-${lesson.lessonId}` }
      });

      // 1. ID Column
      const tdId = createSafeElement('td', { className: 'text-center text-secondary small ps-3' });
      tdId.appendChild(createSafeElement('code', { text: `#${lesson.lessonId}` }));
      tr.appendChild(tdId);

      // 2. Title Column
      const tdTitle = createSafeElement('td', { className: 'fw-semibold text-dark' });
      tdTitle.appendChild(createSafeElement('span', { text: lesson.title || 'Bài học không tên' }));
      tr.appendChild(tdTitle);

      // 3. Status Column
      const tdStatus = createSafeElement('td', { className: 'text-center' });
      const badgeInfo = mapStatusBadge(lesson.status);
      const spanStatus = createSafeElement('span', {
        className: badgeInfo.className,
        text: badgeInfo.label
      });
      tdStatus.appendChild(spanStatus);
      tr.appendChild(tdStatus);

      // 4. Vocabulary Count Column
      const tdVocab = createSafeElement('td', { className: 'text-center text-secondary small' });
      const count = typeof lesson.vocabularyCount === 'number' ? lesson.vocabularyCount : 0;
      tdVocab.appendChild(createSafeElement('span', { text: `${count} từ` }));
      tr.appendChild(tdVocab);

      // 5. Updated Timestamp Column
      const tdUpdated = createSafeElement('td', { className: 'text-secondary small' });
      tdUpdated.appendChild(createSafeElement('span', {
        text: formatLessonDate(lesson.updatedAt || lesson.createdAt)
      }));
      tr.appendChild(tdUpdated);

      // 6. Actions Column (Read-only Detail View)
      const tdActions = createSafeElement('td', { className: 'text-end pe-3' });
      const btnDetail = createSafeElement('button', {
        className: 'btn btn-outline-secondary btn-sm',
        text: 'Xem chi tiết',
        attrs: {
          type: 'button',
          id: `btnDetailLesson-${lesson.lessonId}`,
          'aria-label': `Xem chi tiết bài học ${lesson.title || `#${lesson.lessonId}`}`
        }
      });
      btnDetail.addEventListener('click', (e) => {
        this.lastFocusedElement = e.currentTarget;
        this.openDetailModal(lesson);
      });
      tdActions.appendChild(btnDetail);
      tr.appendChild(tdActions);

      this.elements.tableBody.appendChild(tr);
    }
  }

  /**
   * Renders server-side pagination controls.
   */
  renderPagination() {
    if (!this.elements.paginationSummary || !this.elements.paginationControls) return;

    const summary = calculatePaginationSummary(this.currentPage, this.pageSize, this.totalElements);
    this.elements.paginationSummary.textContent = `Hiển thị ${summary.start}–${summary.end} trong tổng số ${summary.total.toLocaleString('vi-VN')} bài học (Trang ${summary.currentPage + 1}/${summary.totalPages})`;

    clearContainer(this.elements.paginationControls);

    // Previous Button
    const prevLi = createSafeElement('li', {
      className: `page-item ${!summary.hasPrevious ? 'disabled' : ''}`
    });
    const prevBtn = createSafeElement('button', {
      className: 'page-link',
      text: '‹ Trước',
      attrs: {
        type: 'button',
        'aria-label': 'Trang trước',
        ...(summary.hasPrevious ? {} : { disabled: 'true' })
      }
    });
    if (summary.hasPrevious) {
      prevBtn.addEventListener('click', () => {
        this.currentPage--;
        this.loadLessons();
      });
    }
    prevLi.appendChild(prevBtn);
    this.elements.paginationControls.appendChild(prevLi);

    // Current Page Indicator
    const currentLi = createSafeElement('li', { className: 'page-item active' });
    const currentSpan = createSafeElement('span', {
      className: 'page-link',
      text: String(summary.currentPage + 1)
    });
    currentLi.appendChild(currentSpan);
    this.elements.paginationControls.appendChild(currentLi);

    // Next Button
    const nextLi = createSafeElement('li', {
      className: `page-item ${!summary.hasNext ? 'disabled' : ''}`
    });
    const nextBtn = createSafeElement('button', {
      className: 'page-link',
      text: 'Sau ›',
      attrs: {
        type: 'button',
        'aria-label': 'Trang kế tiếp',
        ...(summary.hasNext ? {} : { disabled: 'true' })
      }
    });
    if (summary.hasNext) {
      nextBtn.addEventListener('click', () => {
        this.currentPage++;
        this.loadLessons();
      });
    }
    nextLi.appendChild(nextBtn);
    this.elements.paginationControls.appendChild(nextLi);
  }

  /**
   * Opens and renders the read-only lesson detail modal.
   * Strictly adheres to verified endpoint availability by status:
   * - Approved: calls GET /api/v1/lessons/{id}
   * - Pending: calls GET /api/v1/moderator/lessons/{id}
   * - Draft / Rejected: displays truthful limitation banner
   *
   * @param {object} lesson
   */
  async openDetailModal(lesson) {
    if (!this.elements.detailModal) return;

    // Reset modal sections
    if (this.elements.detailLessonTitle) this.elements.detailLessonTitle.textContent = lesson.title || '—';
    if (this.elements.detailLessonId) this.elements.detailLessonId.textContent = `#${lesson.lessonId}`;
    if (this.elements.detailLessonTimestamp) this.elements.detailLessonTimestamp.textContent = formatLessonDate(lesson.updatedAt || lesson.createdAt);

    if (this.elements.detailLessonStatusBadge) {
      const badgeInfo = mapStatusBadge(lesson.status);
      this.elements.detailLessonStatusBadge.className = badgeInfo.className;
      this.elements.detailLessonStatusBadge.textContent = badgeInfo.label;
    }

    if (this.elements.detailContractLimitationAlert) {
      this.elements.detailContractLimitationAlert.classList.add('d-none');
    }
    if (this.elements.detailErrorAlert) {
      this.elements.detailErrorAlert.classList.add('d-none');
    }
    if (this.elements.detailVocabSection) {
      this.elements.detailVocabSection.classList.add('d-none');
    }
    if (this.elements.detailVocabLoadingPrompt) {
      this.elements.detailVocabLoadingPrompt.classList.add('d-none');
    }

    const detailStrategy = determineDetailStrategy(lesson.status, lesson.lessonId);

    this.showDialog(this.elements.detailModal);

    if (!detailStrategy.canFetchDetail) {
      // Truthful handling of backend gap for Draft/Rejected
      if (this.elements.detailContractLimitationAlert && this.elements.detailContractLimitationMessage) {
        this.elements.detailContractLimitationMessage.textContent = detailStrategy.limitationNotice;
        this.elements.detailContractLimitationAlert.classList.remove('d-none');
      }
      return;
    }

    // For Approved or Pending: fetch detail content
    if (this.elements.detailVocabLoadingPrompt) {
      this.elements.detailVocabLoadingPrompt.classList.remove('d-none');
    }

    try {
      const response = await apiClient(detailStrategy.endpoint);
      const detail = response.data || response;

      this.renderDetailVocabularies(detail.vocabularies || []);
    } catch (err) {
      if (this.elements.detailErrorAlert && this.elements.detailErrorMessage) {
        this.elements.detailErrorMessage.textContent = err?.message || 'Không thể tải chi tiết bài học từ máy chủ.';
        this.elements.detailErrorAlert.classList.remove('d-none');
      }
    } finally {
      if (this.elements.detailVocabLoadingPrompt) {
        this.elements.detailVocabLoadingPrompt.classList.add('d-none');
      }
    }
  }

  /**
   * Renders the ordered vocabularies list inside the detail modal.
   *
   * @param {Array<object>} vocabularies
   */
  renderDetailVocabularies(vocabularies) {
    if (!this.elements.detailVocabSection || !this.elements.detailVocabList) return;
    clearContainer(this.elements.detailVocabList);

    const count = Array.isArray(vocabularies) ? vocabularies.length : 0;
    if (this.elements.detailVocabCount) {
      this.elements.detailVocabCount.textContent = String(count);
    }

    if (count === 0) {
      if (this.elements.detailVocabEmptyPrompt) {
        this.elements.detailVocabEmptyPrompt.classList.remove('d-none');
      }
      this.elements.detailVocabSection.classList.remove('d-none');
      return;
    }

    if (this.elements.detailVocabEmptyPrompt) {
      this.elements.detailVocabEmptyPrompt.classList.add('d-none');
    }

    for (let i = 0; i < vocabularies.length; i++) {
      const item = vocabularies[i];
      const order = typeof item.orderIndex === 'number' ? item.orderIndex : i + 1;

      const row = createSafeElement('div', {
        className: 'list-group-item d-flex align-items-center justify-content-between py-2 px-3 gap-3'
      });

      // Left: Order + Hanzi + Pinyin
      const leftCol = createSafeElement('div', { className: 'd-flex align-items-center gap-3' });
      const orderBadge = createSafeElement('span', {
        className: 'badge bg-light text-secondary border small text-center',
        attrs: { style: 'width: 32px;' },
        text: `#${order}`
      });
      leftCol.appendChild(orderBadge);

      const hanziSpan = createSafeElement('span', {
        className: 'font-chinese fs-5 fw-bold text-chinese-red',
        text: item.hanzi || '—'
      });
      leftCol.appendChild(hanziSpan);

      const pinyinSpan = createSafeElement('span', {
        className: 'small text-dark fw-semibold',
        text: item.pinyin ? `[${item.pinyin}]` : ''
      });
      leftCol.appendChild(pinyinSpan);

      row.appendChild(leftCol);

      // Right: Meanings + Audio
      const rightCol = createSafeElement('div', { className: 'd-flex align-items-center gap-2 text-end' });
      if (item.meaningHanViet) {
        const hvSpan = createSafeElement('span', {
          className: 'small text-muted d-none d-sm-inline',
          text: `(${item.meaningHanViet})`
        });
        rightCol.appendChild(hvSpan);
      }

      const viSpan = createSafeElement('span', {
        className: 'small text-dark fw-medium',
        text: item.meaningVi || ''
      });
      rightCol.appendChild(viSpan);

      if (item.audioUrl) {
        const audioBadge = createSafeElement('span', {
          className: 'badge bg-light text-primary border ms-1',
          attrs: { title: 'Có phát âm' },
          text: '🔊'
        });
        rightCol.appendChild(audioBadge);
      }

      row.appendChild(rightCol);

      this.elements.detailVocabList.appendChild(row);
    }

    this.elements.detailVocabSection.classList.remove('d-none');
  }

  /**
   * Displays modal dialog.
   *
   * @param {HTMLDialogElement} dialog
   */
  showDialog(dialog) {
    if (!dialog) return;
    if (typeof dialog.showModal === 'function') {
      dialog.showModal();
    } else {
      dialog.setAttribute('open', '');
    }
  }

  /**
   * Closes detail modal dialog and restores focus.
   */
  closeDetailModal() {
    if (!this.elements.detailModal) return;
    if (typeof this.elements.detailModal.close === 'function') {
      this.elements.detailModal.close();
    } else {
      this.elements.detailModal.removeAttribute('open');
    }

    if (this.lastFocusedElement && typeof this.lastFocusedElement.focus === 'function') {
      this.lastFocusedElement.focus();
    }
  }
}

// Auto-initialize on DOM ready in browser
if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    const controller = new AdminLessonsPageController();
    controller.init();
  });
}
