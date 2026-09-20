/**
 * =============================================================================
 * MODERATOR AUDIT HISTORY PAGE CONTROLLER (TASK 9E.4)
 * Module: frontend/js/pages/moderator-history-page.js
 * 
 * Responsibilities:
 * - Direct integration with Spring Boot backend via apiClient('/moderator/history').
 * - Role-guarded lifecycle: strictly accessible to 'Moderator' and 'Admin'.
 * - Role-aware data semantics (Server-Determined):
 *   * Moderator: backend automatically filters personal logs (moderator_id = caller).
 *   * Admin: backend returns system-wide global moderation audit history.
 * - Server-side pagination matching PageResponse<ModerationLogResponse> contract.
 * - Defensive normalization against Jackson @JsonInclude(NON_NULL) omissions.
 * - Action normalization tolerating verified backend values ('Approve', 'Reject', 'Approved', 'Rejected').
 * - Safe flaggedFields parser supporting JSON string arrays, plain strings, and malformed content.
 * - 100% Read-Only immutable audit semantics: zero mutation endpoints or buttons.
 * - Safe DOM construction (zero unsafe innerHTML) defending against XSS.
 * - Three-state UI (Loading / Empty / Error with retry).
 * =============================================================================
 */

import { apiClient } from '../api/api.js';
import { authManager } from '../auth/auth-state.js';
import { initNavbarAuth } from '../ui/nav.js';
import { createSafeElement, clearContainer } from '../ui/security.js';
import { showLoading, showEmpty, showError } from '../ui/ui.js';

/* -----------------------------------------------------------------------------
 * 1. PURE LOGIC & UTILITY HELPERS (Exported for Testability)
 * ----------------------------------------------------------------------------- */

/**
 * Checks whether the current authenticated session has access to Moderation History.
 * Allowed roles: 'Moderator' or 'Admin'.
 * 
 * @param {object} [authManagerInstance=authManager] 
 * @returns {boolean}
 */
export function hasModeratorAccess(authManagerInstance = authManager) {
  if (!authManagerInstance || typeof authManagerInstance.hasRole !== 'function') {
    return false;
  }
  return authManagerInstance.hasRole('Moderator') || authManagerInstance.hasRole('Admin');
}

/**
 * Safely parses backend PageResponse<ModerationLogResponse> payload.
 * Defensively handles @JsonInclude(NON_NULL) omitted fields.
 * 
 * @param {any} envelopeOrData 
 * @returns {{
 *   items: Array<{
 *     logId: number,
 *     lessonId: number|null,
 *     lessonTitle: string|null,
 *     moderatorId: number|null,
 *     moderatorEmail: string|null,
 *     action: string,
 *     rejectionReason: string|null,
 *     flaggedFields: string|null,
 *     createdAt: string|null
 *   }>,
 *   page: number,
 *   size: number,
 *   totalElements: number,
 *   totalPages: number
 * }}
 */
export function parseModerationHistory(envelopeOrData) {
  if (!envelopeOrData || typeof envelopeOrData !== 'object') {
    return { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
  }

  const data = (envelopeOrData.data && typeof envelopeOrData.data === 'object')
    ? envelopeOrData.data
    : envelopeOrData;

  const rawItems = Array.isArray(data.items) ? data.items : [];
  const items = rawItems.map(item => {
    if (!item || typeof item !== 'object') {
      return {
        logId: 0,
        lessonId: null,
        lessonTitle: null,
        moderatorId: null,
        moderatorEmail: null,
        action: 'Unknown',
        rejectionReason: null,
        flaggedFields: null,
        createdAt: null
      };
    }

    return {
      logId: Math.max(0, Number(item.logId) || 0),
      lessonId: item.lessonId !== undefined && item.lessonId !== null ? Math.max(0, Number(item.lessonId) || 0) : null,
      lessonTitle: typeof item.lessonTitle === 'string' && item.lessonTitle.trim().length > 0 ? item.lessonTitle.trim() : null,
      moderatorId: item.moderatorId !== undefined && item.moderatorId !== null ? Math.max(0, Number(item.moderatorId) || 0) : null,
      moderatorEmail: typeof item.moderatorEmail === 'string' && item.moderatorEmail.trim().length > 0 ? item.moderatorEmail.trim() : null,
      action: typeof item.action === 'string' && item.action.trim().length > 0 ? item.action.trim() : 'Unknown',
      rejectionReason: typeof item.rejectionReason === 'string' && item.rejectionReason.trim().length > 0 ? item.rejectionReason.trim() : null,
      flaggedFields: typeof item.flaggedFields === 'string' && item.flaggedFields.trim().length > 0 ? item.flaggedFields.trim() : null,
      createdAt: typeof item.createdAt === 'string' && item.createdAt.trim().length > 0 ? item.createdAt.trim() : null
    };
  });

  const page = Math.max(0, Number(data.page) || 0);
  const size = Math.max(1, Number(data.size) || 20);
  const totalElements = typeof data.totalElements === 'number' ? Math.max(0, data.totalElements) : items.length;
  const totalPages = typeof data.totalPages === 'number' ? Math.max(0, data.totalPages) : (totalElements > 0 ? Math.ceil(totalElements / size) : 0);

  return { items, page, size, totalElements, totalPages };
}

/**
 * Returns a human-friendly lesson identifier.
 * Priority: lessonTitle > "Bài học #<lessonId>" > "Bài học không xác định".
 * 
 * @param {string|null|undefined} lessonTitle 
 * @param {number|string|null|undefined} lessonId 
 * @returns {string}
 */
export function getLessonDisplayName(lessonTitle, lessonId) {
  if (typeof lessonTitle === 'string' && lessonTitle.trim().length > 0) {
    return lessonTitle.trim();
  }
  if (lessonId !== undefined && lessonId !== null && Number(lessonId) > 0) {
    return `Bài học #${lessonId}`;
  }
  return 'Bài học không xác định';
}

/**
 * Normalizes backend action values into display metadata.
 * Tolerates both current backend values ("Approve", "Reject") and documented variants ("Approved", "Rejected").
 * 
 * @param {string|null|undefined} action 
 * @returns {{
 *   type: 'approve' | 'reject' | 'unknown',
 *   label: string,
 *   badgeClass: string
 * }}
 */
export function normalizeModerationAction(action) {
  const norm = typeof action === 'string' ? action.trim().toLowerCase() : '';
  if (norm === 'approve' || norm === 'approved') {
    return {
      type: 'approve',
      label: 'Phê duyệt',
      badgeClass: 'badge-status badge-status-approved'
    };
  }
  if (norm === 'reject' || norm === 'rejected') {
    return {
      type: 'reject',
      label: 'Từ chối',
      badgeClass: 'badge-status badge-status-danger'
    };
  }
  return {
    type: 'unknown',
    label: action ? String(action).trim() : 'Không xác định',
    badgeClass: 'badge-status badge-status-draft'
  };
}

/**
 * Field taxonomy dictionary for human-readable labels.
 */
const KNOWN_FIELD_LABELS = {
  title: 'Tiêu đề bài học',
  pinyin: 'Phiên âm Pinyin',
  meaningvi: 'Dịch nghĩa tiếng Việt',
  meaning_vi: 'Dịch nghĩa tiếng Việt',
  examplesentence: 'Câu ví dụ',
  example_sentence: 'Câu ví dụ',
  audiourl: 'Âm thanh phát âm',
  audio_url: 'Âm thanh phát âm',
  orderindex: 'Thứ tự từ vựng',
  order_index: 'Thứ tự từ vựng'
};

/**
 * Safely parses the raw flaggedFields string stored in backend audit logs.
 * Supports:
 * - JSON string array (e.g. '["title", "vocabularies[0].pinyin"]')
 * - Comma-separated strings (e.g. 'title, pinyin')
 * - Single plain strings (e.g. 'title')
 * - Malformed JSON strings (syntax error safe fallback)
 * - Null/empty/undefined
 * 
 * @param {string|null|undefined} rawFlaggedFields 
 * @returns {Array<{ raw: string, label: string }>}
 */
export function parseFlaggedFields(rawFlaggedFields) {
  if (!rawFlaggedFields || typeof rawFlaggedFields !== 'string') {
    return [];
  }

  const trimmed = rawFlaggedFields.trim();
  if (!trimmed) {
    return [];
  }

  const toFieldObject = (item) => {
    const raw = String(item || '').trim();
    if (!raw) return null;
    const lowerKey = raw.toLowerCase();
    const label = KNOWN_FIELD_LABELS[lowerKey] || raw;
    return { raw, label };
  };

  // Check if string looks like a JSON array
  if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
    try {
      const parsed = JSON.parse(trimmed);
      if (Array.isArray(parsed)) {
        return parsed.map(toFieldObject).filter(Boolean);
      }
    } catch (_) {
      // Malformed JSON array, fall through to delimiter splitting
    }
  }

  // Comma-separated fallback
  if (trimmed.includes(',')) {
    return trimmed.split(',').map(toFieldObject).filter(Boolean);
  }

  // Single plain string
  const single = toFieldObject(trimmed);
  return single ? [single] : [];
}

/**
 * Formats an ISO date string into DD/MM/YYYY HH:mm:ss.
 * Returns '--' for null, undefined, or malformed representations.
 * 
 * @param {string|null|undefined} isoString 
 * @returns {string}
 */
export function formatHistoryDate(isoString) {
  if (!isoString || typeof isoString !== 'string') return '--';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return '--';
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    const hours = String(d.getHours()).padStart(2, '0');
    const mins = String(d.getMinutes()).padStart(2, '0');
    const secs = String(d.getSeconds()).padStart(2, '0');
    return `${day}/${month}/${year} ${hours}:${mins}:${secs}`;
  } catch (_) {
    return '--';
  }
}

/**
 * Calculates accessible pagination boundaries and navigation state.
 * 
 * @param {number} page 0-indexed current page
 * @param {number} size page size
 * @param {number} totalElements total records count
 * @param {number} totalPages total pages count
 * @returns {{
 *   currentPage: number,
 *   totalPages: number,
 *   hasPrev: boolean,
 *   hasNext: boolean,
 *   startRecord: number,
 *   endRecord: number
 * }}
 */
export function calculatePagination(page, size, totalElements, totalPages) {
  const safeTotalPages = Math.max(0, totalPages || 0);
  const safePage = Math.max(0, Math.min(page || 0, Math.max(0, safeTotalPages - 1)));
  const hasPrev = safePage > 0;
  const hasNext = safePage < safeTotalPages - 1;
  const startRecord = totalElements === 0 ? 0 : safePage * size + 1;
  const endRecord = Math.min((safePage + 1) * size, totalElements);

  return {
    currentPage: safePage,
    totalPages: safeTotalPages,
    hasPrev,
    hasNext,
    startRecord,
    endRecord
  };
}

/* -----------------------------------------------------------------------------
 * 2. MODERATOR HISTORY PAGE CONTROLLER
 * ----------------------------------------------------------------------------- */

export class ModeratorHistoryController {
  constructor() {
    this.currentPage = 0;
    this.pageSize = 20;
    this.totalElements = 0;
    this.totalPages = 0;
    this.isLoading = false;

    // DOM Elements
    this.authGuardContainer = document.getElementById('moderatorAuthGuardContainer');
    this.workspaceSection = document.getElementById('moderatorHistoryWorkspaceSection');
    this.historyHeading = document.getElementById('historyHeading');
    this.historySubHeading = document.getElementById('historySubHeading');
    this.resultCountBadge = document.getElementById('historyResultCountBadge');
    this.pageSizeSelect = document.getElementById('historyPageSizeSelect');
    this.stateContainer = document.getElementById('historyStateContainer');
    this.tableContainer = document.getElementById('historyTableContainer');
    this.tableBody = document.getElementById('moderatorHistoryTableBody');
    this.paginationNav = document.getElementById('historyPaginationNav');
    this.paginationSummary = document.getElementById('historyPaginationSummary');
    this.paginationList = document.getElementById('historyPaginationList');
  }

  /**
   * Initializes the controller, checks role-based access, and loads history.
   */
  async init() {
    initNavbarAuth();

    if (!hasModeratorAccess(authManager)) {
      this.renderUnauthorized();
      return;
    }

    this.renderAuthorizedWorkspace();
    this.bindEvents();
    await this.loadHistory(0);
  }

  /**
   * Renders the unauthorized warning state when caller lacks Moderator/Admin roles.
   */
  renderUnauthorized() {
    if (this.authGuardContainer) {
      this.authGuardContainer.classList.remove('d-none');
    }
    if (this.workspaceSection) {
      this.workspaceSection.classList.add('d-none');
    }
  }

  /**
   * Configures heading and UI scope based on user role (Admin vs Moderator).
   */
  renderAuthorizedWorkspace() {
    if (this.authGuardContainer) {
      this.authGuardContainer.classList.add('d-none');
    }
    if (this.workspaceSection) {
      this.workspaceSection.classList.remove('d-none');
    }

    const isAdmin = authManager.hasRole('Admin');
    if (this.historyHeading) {
      this.historyHeading.textContent = isAdmin
        ? 'Lịch Sử Kiểm Duyệt Hệ Thống'
        : 'Nhật Ký Kiểm Duyệt Của Tôi';
    }
    if (this.historySubHeading) {
      this.historySubHeading.textContent = isAdmin
        ? 'Toàn bộ nhật ký kiểm duyệt bài học trên toàn hệ thống phục vụ công tác kiểm toán (bất biến và chỉ đọc).'
        : 'Lịch sử các quyết định phê duyệt và từ chối bài học do bạn thực hiện (dữ liệu kiểm toán bất biến).';
    }
  }

  /**
   * Binds user event listeners (page size changes, etc.).
   */
  bindEvents() {
    if (this.pageSizeSelect) {
      this.pageSizeSelect.addEventListener('change', async (e) => {
        const newSize = parseInt(e.target.value, 10);
        if ([12, 20, 40].includes(newSize)) {
          this.pageSize = newSize;
          await this.loadHistory(0);
        }
      });
    }
  }

  /**
   * Fetches paginated history from GET /api/v1/moderator/history.
   * 
   * @param {number} targetPage 
   */
  async loadHistory(targetPage = 0) {
    if (this.isLoading) return;
    this.isLoading = true;

    // Show loading state
    if (this.stateContainer) {
      showLoading(this.stateContainer, 'Đang tải nhật ký kiểm duyệt...');
    }
    if (this.tableContainer) {
      this.tableContainer.classList.add('d-none');
    }
    if (this.paginationNav) {
      this.paginationNav.classList.add('d-none');
    }
    if (this.resultCountBadge) {
      this.resultCountBadge.textContent = 'Đang tải...';
      this.resultCountBadge.className = 'badge-status badge-status-draft text-nowrap';
    }

    try {
      const response = await apiClient('/moderator/history', {
        params: {
          page: targetPage,
          size: this.pageSize
        }
      });

      const parsed = parseModerationHistory(response);
      this.currentPage = parsed.page;
      this.totalElements = parsed.totalElements;
      this.totalPages = parsed.totalPages;

      if (parsed.items.length === 0) {
        this.renderEmptyState();
      } else {
        this.renderTable(parsed.items);
        this.renderPagination(parsed);
      }
    } catch (error) {
      this.renderErrorState(error, targetPage);
    } finally {
      this.isLoading = false;
    }
  }

  /**
   * Renders the empty state when no audit records are returned.
   */
  renderEmptyState() {
    if (this.stateContainer) {
      const isAdmin = authManager.hasRole('Admin');
      const emptyTitle = isAdmin ? 'Chưa có nhật ký kiểm duyệt hệ thống' : 'Chưa có nhật ký kiểm duyệt';
      const emptyDesc = isAdmin
        ? 'Chưa có quyết định phê duyệt hoặc từ chối bài học nào được ghi nhận trên hệ thống.'
        : 'Bạn chưa thực hiện quyết định phê duyệt hoặc từ chối bài học nào.';
      showEmpty(this.stateContainer, {
        glyph: '史',
        title: emptyTitle,
        description: emptyDesc
      });
    }
    if (this.tableContainer) {
      this.tableContainer.classList.add('d-none');
    }
    if (this.paginationNav) {
      this.paginationNav.classList.add('d-none');
    }
    if (this.resultCountBadge) {
      this.resultCountBadge.textContent = '0 mục';
      this.resultCountBadge.className = 'badge-status badge-status-draft text-nowrap';
    }
  }

  /**
   * Renders the table rows using safe DOM methods.
   * 
   * @param {Array<object>} items 
   */
  renderTable(items) {
    if (this.stateContainer) {
      clearContainer(this.stateContainer);
    }
    if (this.tableContainer) {
      this.tableContainer.classList.remove('d-none');
    }
    if (!this.tableBody) return;

    clearContainer(this.tableBody);

    const fragment = document.createDocumentFragment();

    for (const item of items) {
      const row = document.createElement('tr');

      // 1. Timestamp Cell
      const timeCell = createSafeElement('td', {
        className: 'py-3 px-3 audit-timestamp-cell text-secondary font-monospace',
        text: formatHistoryDate(item.createdAt)
      });

      // 2. Lesson Cell
      const lessonTitleText = getLessonDisplayName(item.lessonTitle, item.lessonId);
      const lessonTitleEl = createSafeElement('span', {
        className: 'fw-semibold text-dark d-block',
        text: lessonTitleText
      });
      const lessonSubEl = item.lessonId ? createSafeElement('small', {
        className: 'text-muted font-monospace',
        text: `Mã bài: #${item.lessonId}`
      }) : null;

      const lessonCell = createSafeElement('td', {
        className: 'py-3 px-3',
        children: lessonSubEl ? [lessonTitleEl, lessonSubEl] : [lessonTitleEl]
      });

      // 3. Moderator Cell
      const modEmail = item.moderatorEmail || (item.moderatorId ? `ID #${item.moderatorId}` : '—');
      const modCell = createSafeElement('td', {
        className: 'py-3 px-3 moderator-identity-cell text-secondary',
        text: modEmail,
        attrs: { title: modEmail }
      });

      // 4. Action Cell
      const actionMeta = normalizeModerationAction(item.action);
      const actionBadge = createSafeElement('span', {
        className: actionMeta.badgeClass,
        text: actionMeta.label
      });
      const actionCell = createSafeElement('td', {
        className: 'py-3 px-2 text-center',
        children: [actionBadge]
      });

      // 5. Rejection Reason Cell
      const isReject = actionMeta.type === 'reject';
      const reasonText = (isReject && item.rejectionReason) ? item.rejectionReason : '—';
      const reasonCell = createSafeElement('td', {
        className: `py-3 px-3 rejection-reason-cell ${isReject && item.rejectionReason ? 'text-dark' : 'text-muted text-center'}`,
        text: reasonText
      });

      // 6. Flagged Fields Cell
      const flaggedList = isReject ? parseFlaggedFields(item.flaggedFields) : [];
      let flaggedContent;
      if (flaggedList.length > 0) {
        const chipsContainer = createSafeElement('div', {
          className: 'd-flex flex-wrap gap-1',
          children: flaggedList.map(f => createSafeElement('span', {
            className: 'flagged-field-chip',
            text: f.label,
            attrs: { title: f.raw }
          }))
        });
        flaggedContent = [chipsContainer];
      } else {
        flaggedContent = [createSafeElement('span', {
          className: 'text-muted text-center d-block',
          text: '—'
        })];
      }

      const flaggedCell = createSafeElement('td', {
        className: 'py-3 px-3',
        children: flaggedContent
      });

      row.appendChild(timeCell);
      row.appendChild(lessonCell);
      row.appendChild(modCell);
      row.appendChild(actionCell);
      row.appendChild(reasonCell);
      row.appendChild(flaggedCell);

      fragment.appendChild(row);
    }

    this.tableBody.appendChild(fragment);

    // Update Result Badge
    if (this.resultCountBadge) {
      this.resultCountBadge.textContent = `${this.totalElements} mục`;
      this.resultCountBadge.className = 'badge-status badge-status-approved text-nowrap';
    }
  }

  /**
   * Renders pagination controls.
   * 
   * @param {object} parsed 
   */
  renderPagination(parsed) {
    if (!this.paginationNav) return;

    if (parsed.totalPages <= 1 && parsed.totalElements <= parsed.size) {
      this.paginationNav.classList.add('d-none');
      return;
    }

    this.paginationNav.classList.remove('d-none');

    const pagination = calculatePagination(parsed.page, parsed.size, parsed.totalElements, parsed.totalPages);

    // Update Summary
    if (this.paginationSummary) {
      this.paginationSummary.textContent = `Hiển thị ${pagination.startRecord} - ${pagination.endRecord} trong ${parsed.totalElements} mục nhật ký`;
    }

    // Render Navigation Buttons
    if (this.paginationList) {
      clearContainer(this.paginationList);

      // Previous button
      const prevLi = createSafeElement('li', {
        className: `page-item ${!pagination.hasPrev ? 'disabled' : ''}`,
        children: [
          createSafeElement('button', {
            className: 'page-link',
            text: '« Trước',
            attrs: {
              type: 'button',
              'aria-label': 'Trang trước',
              ...(!pagination.hasPrev ? { disabled: 'true' } : {})
            }
          })
        ]
      });
      if (pagination.hasPrev) {
        prevLi.querySelector('button').addEventListener('click', () => {
          this.loadHistory(pagination.currentPage - 1);
        });
      }
      this.paginationList.appendChild(prevLi);

      // Page numbers (display max 5 nearby pages)
      const startPage = Math.max(0, pagination.currentPage - 2);
      const endPage = Math.min(pagination.totalPages - 1, startPage + 4);

      for (let p = startPage; p <= endPage; p++) {
        const isCurrent = p === pagination.currentPage;
        const pageLi = createSafeElement('li', {
          className: `page-item ${isCurrent ? 'active' : ''}`,
          children: [
            createSafeElement('button', {
              className: 'page-link',
              text: String(p + 1),
              attrs: {
                type: 'button',
                'aria-label': `Trang ${p + 1}`,
                ...(isCurrent ? { 'aria-current': 'page' } : {})
              }
            })
          ]
        });
        if (!isCurrent) {
          const pageNum = p;
          pageLi.querySelector('button').addEventListener('click', () => {
            this.loadHistory(pageNum);
          });
        }
        this.paginationList.appendChild(pageLi);
      }

      // Next button
      const nextLi = createSafeElement('li', {
        className: `page-item ${!pagination.hasNext ? 'disabled' : ''}`,
        children: [
          createSafeElement('button', {
            className: 'page-link',
            text: 'Sau »',
            attrs: {
              type: 'button',
              'aria-label': 'Trang kế tiếp',
              ...(!pagination.hasNext ? { disabled: 'true' } : {})
            }
          })
        ]
      });
      if (pagination.hasNext) {
        nextLi.querySelector('button').addEventListener('click', () => {
          this.loadHistory(pagination.currentPage + 1);
        });
      }
      this.paginationList.appendChild(nextLi);
    }
  }

  /**
   * Renders error state with a retry callback.
   * 
   * @param {Error} error 
   * @param {number} page 
   */
  renderErrorState(error, page) {
    if (this.stateContainer) {
      showError(this.stateContainer, {
        title: 'Không thể tải lịch sử kiểm duyệt',
        message: error?.message || 'Đã có lỗi xảy ra trong quá trình truy vấn nhật ký kiểm toán.',
        onRetry: () => this.loadHistory(page)
      });
    }
    if (this.tableContainer) {
      this.tableContainer.classList.add('d-none');
    }
    if (this.paginationNav) {
      this.paginationNav.classList.add('d-none');
    }
    if (this.resultCountBadge) {
      this.resultCountBadge.textContent = 'Lỗi kết nối';
      this.resultCountBadge.className = 'badge-status badge-status-danger text-nowrap';
    }
  }
}

/**
 * Initializes the Moderator History Page controller on DOMContentLoaded.
 * Guarded against execution when imported on non-moderator-history pages.
 * 
 * @returns {ModeratorHistoryController|null}
 */
export function initModeratorHistoryPage() {
  if (typeof document === 'undefined') return null;
  const root = document.getElementById('moderatorHistoryWorkspaceSection') || document.getElementById('moderatorHistoryTable');
  if (!root) return null;

  const controller = new ModeratorHistoryController();
  controller.init();
  return controller;
}

if (typeof window !== 'undefined' && typeof document !== 'undefined') {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      initModeratorHistoryPage();
    });
  } else {
    initModeratorHistoryPage();
  }
}


