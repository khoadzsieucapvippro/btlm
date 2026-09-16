/**
 * =============================================================================
 * MODERATOR REVIEW QUEUE PAGE CONTROLLER (TASK 9E.1)
 * Module: frontend/js/pages/moderator-queue-page.js
 * 
 * Responsibilities:
 * - Direct integration with Spring Boot backend via apiClient('/moderator/lessons/pending').
 * - Role-guarded lifecycle: strictly accessible to 'Moderator' and 'Admin'.
 * - Server-side pagination matching PageResponse<ModerationQueueResponse> contract.
 * - Accurate presentation of verified fields: lessonId, title, creatorEmail, vocabularyCount, updatedAt.
 * - Column label for date reflects verified semantics: "Cập nhật lần cuối".
 * - Review action navigation pointing to moderator-review.html?id=<lessonId>.
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
 * Checks whether the current authenticated session has access to the Moderator Queue.
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
 * Safely parses backend PageResponse<ModerationQueueResponse> payload.
 * Normalizes missing or nullable fields gracefully.
 * 
 * @param {any} envelopeOrData 
 * @returns {{
 *   items: Array<{
 *     lessonId: number,
 *     title: string,
 *     status: string,
 *     creatorId: number|null,
 *     creatorEmail: string|null,
 *     vocabularyCount: number,
 *     createdAt: string|null,
 *     updatedAt: string|null
 *   }>,
 *   page: number,
 *   size: number,
 *   totalElements: number,
 *   totalPages: number
 * }}
 */
export function parseModerationQueue(envelopeOrData) {
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
        lessonId: 0,
        title: 'Bài học không xác định',
        status: 'Pending',
        creatorId: null,
        creatorEmail: null,
        vocabularyCount: 0,
        createdAt: null,
        updatedAt: null
      };
    }

    return {
      lessonId: Math.max(0, Number(item.lessonId) || 0),
      title: typeof item.title === 'string' ? item.title.trim() : 'Không có tiêu đề',
      status: typeof item.status === 'string' ? item.status : 'Pending',
      creatorId: item.creatorId !== undefined && item.creatorId !== null ? Number(item.creatorId) : null,
      creatorEmail: typeof item.creatorEmail === 'string' && item.creatorEmail.trim().length > 0 ? item.creatorEmail.trim() : null,
      vocabularyCount: typeof item.vocabularyCount === 'number' ? Math.max(0, item.vocabularyCount) : 0,
      createdAt: typeof item.createdAt === 'string' ? item.createdAt : null,
      updatedAt: typeof item.updatedAt === 'string' ? item.updatedAt : null
    };
  });

  const page = Math.max(0, Number(data.page) || 0);
  const size = Math.max(1, Number(data.size) || 20);
  const totalElements = typeof data.totalElements === 'number' ? Math.max(0, data.totalElements) : items.length;
  const totalPages = typeof data.totalPages === 'number' ? Math.max(0, data.totalPages) : (totalElements > 0 ? Math.ceil(totalElements / size) : 0);

  return { items, page, size, totalElements, totalPages };
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

/**
 * Formats an ISO date string into DD/MM/YYYY HH:mm.
 * Returns '--' for null, undefined, or malformed date representations.
 * 
 * @param {string|null} isoString 
 * @returns {string}
 */
export function formatQueueDate(isoString) {
  if (!isoString || typeof isoString !== 'string') return '--';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return '--';
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    const hours = String(d.getHours()).padStart(2, '0');
    const mins = String(d.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year} ${hours}:${mins}`;
  } catch (_) {
    return '--';
  }
}

/**
 * Constructs the review destination URL for a given lessonId.
 * Points to the planned Task 9E.2 route: moderator-review.html?id=<lessonId>.
 * 
 * @param {number|string} lessonId 
 * @returns {string}
 */
export function getReviewUrl(lessonId) {
  const safeId = encodeURIComponent(String(lessonId || ''));
  return `moderator-review.html?id=${safeId}`;
}

/**
 * Maps queue status to badge styling and label.
 * The queue contains exclusively Pending lessons.
 * 
 * @param {string} [status='Pending'] 
 * @returns {{ label: string, badgeClass: string }}
 */
export function getQueueStatusBadge(status = 'Pending') {
  return {
    label: 'Chờ duyệt',
    badgeClass: 'badge-status badge-status-pending'
  };
}

/* -----------------------------------------------------------------------------
 * 2. MODERATOR QUEUE PAGE CONTROLLER
 * ----------------------------------------------------------------------------- */

export class ModeratorQueueController {
  constructor() {
    this.currentPage = 0;
    this.pageSize = 20;
    this.totalElements = 0;
    this.totalPages = 0;
    this.isLoading = false;

    this.elements = {};
  }

  init() {
    initNavbarAuth();
    this.bindDomElements();
    this.bindEvents();

    // Enforce role guard
    if (!authManager.isAuthenticated()) {
      window.location.href = 'login.html?redirect=moderator-queue.html';
      return;
    }

    if (!hasModeratorAccess(authManager)) {
      this.showUnauthorizedState();
      return;
    }

    this.loadQueue();
  }

  bindDomElements() {
    this.elements = {
      authGuardContainer: document.getElementById('moderatorAuthGuardContainer'),
      authGuardMessage: document.getElementById('moderatorAuthGuardMessage'),
      workspaceSection: document.getElementById('moderatorWorkspaceSection'),
      resultCountBadge: document.getElementById('queueResultCountBadge'),
      pageSizeSelect: document.getElementById('queuePageSizeSelect'),
      stateContainer: document.getElementById('queueStateContainer'),
      tableContainer: document.getElementById('queueTableContainer'),
      tableBody: document.getElementById('moderatorQueueTableBody'),
      paginationNav: document.getElementById('queuePaginationNav'),
      paginationSummary: document.getElementById('queuePaginationSummary'),
      paginationList: document.getElementById('queuePaginationList')
    };
  }

  bindEvents() {
    if (this.elements.pageSizeSelect) {
      this.elements.pageSizeSelect.addEventListener('change', (e) => {
        const newSize = Math.max(1, Number(e.target.value) || 20);
        if (newSize !== this.pageSize) {
          this.pageSize = newSize;
          this.currentPage = 0;
          this.loadQueue();
        }
      });
    }
  }

  showUnauthorizedState() {
    if (this.elements.workspaceSection) {
      this.elements.workspaceSection.classList.add('d-none');
    }
    if (this.elements.authGuardContainer) {
      this.elements.authGuardContainer.classList.remove('d-none');
    }
  }

  async loadQueue() {
    if (!this.elements.stateContainer) return;
    this.isLoading = true;

    // 1. Show loading state
    showLoading(this.elements.stateContainer, { message: 'Đang tải hàng đợi kiểm duyệt bài học...' });
    if (this.elements.tableContainer) this.elements.tableContainer.classList.add('d-none');
    if (this.elements.paginationNav) this.elements.paginationNav.classList.add('d-none');
    if (this.elements.resultCountBadge) {
      this.elements.resultCountBadge.textContent = 'Đang tải...';
      this.elements.resultCountBadge.className = 'badge-status badge-status-draft text-nowrap';
    }

    try {
      const response = await apiClient('/moderator/lessons/pending', {
        params: {
          page: this.currentPage,
          size: this.pageSize
        }
      });

      const parsed = parseModerationQueue(response);
      this.currentPage = parsed.page;
      this.pageSize = parsed.size;
      this.totalElements = parsed.totalElements;
      this.totalPages = parsed.totalPages;

      clearContainer(this.elements.stateContainer);

      if (parsed.totalElements === 0) {
        // Truthful Empty state
        if (this.elements.resultCountBadge) {
          this.elements.resultCountBadge.textContent = '0 bài học';
          this.elements.resultCountBadge.className = 'badge-status badge-status-draft text-nowrap';
        }

        showEmpty(this.elements.stateContainer, {
          title: 'Hàng đợi đang trống',
          description: 'Hiện tại không có bài học nào đang ở trạng thái Chờ duyệt (Pending).'
        });
        return;
      }

      // Update count badge
      if (this.elements.resultCountBadge) {
        this.elements.resultCountBadge.textContent = `${parsed.totalElements} bài học chờ duyệt`;
        this.elements.resultCountBadge.className = 'badge-status badge-status-pending text-nowrap';
      }

      // Render table rows
      this.renderTableRows(parsed.items);

      // Render pagination
      this.renderPagination(parsed.page, parsed.size, parsed.totalElements, parsed.totalPages);

      if (this.elements.tableContainer) this.elements.tableContainer.classList.remove('d-none');
    } catch (err) {
      console.warn('[ModeratorQueue] Load failed:', err);
      clearContainer(this.elements.stateContainer);

      if (this.elements.resultCountBadge) {
        this.elements.resultCountBadge.textContent = 'Lỗi nạp dữ liệu';
        this.elements.resultCountBadge.className = 'badge-status badge-status-rejected text-nowrap';
      }

      showError(this.elements.stateContainer, {
        message: err.message || 'Không thể tải hàng đợi kiểm duyệt từ máy chủ. Vui lòng thử lại.',
        onRetry: () => this.loadQueue()
      });
    } finally {
      this.isLoading = false;
    }
  }

  renderTableRows(items) {
    if (!this.elements.tableBody) return;
    clearContainer(this.elements.tableBody);

    const badgeInfo = getQueueStatusBadge('Pending');

    for (const item of items) {
      const reviewUrl = getReviewUrl(item.lessonId);
      const formattedDate = formatQueueDate(item.updatedAt || item.createdAt);
      const creatorDisplay = item.creatorEmail || (item.creatorId ? `ID #${item.creatorId}` : 'Chưa có thông tin');

      const tr = createSafeElement('tr', {
        className: 'moderator-queue-row',
        attrs: {
          'data-lesson-id': String(item.lessonId)
        },
        children: [
          // 1. Mã bài
          createSafeElement('td', {
            className: 'text-center font-monospace small text-muted',
            text: `#${item.lessonId}`
          }),

          // 2. Tiêu đề
          createSafeElement('td', {
            children: [
              createSafeElement('span', {
                className: 'fw-bold text-dark d-block',
                text: item.title || 'Không có tiêu đề'
              })
            ]
          }),

          // 3. Tác giả (Email)
          createSafeElement('td', {
            children: [
              createSafeElement('span', {
                className: 'font-monospace small text-secondary d-block text-break',
                text: creatorDisplay
              })
            ]
          }),

          // 4. Số từ vựng
          createSafeElement('td', {
            className: 'text-center',
            children: [
              createSafeElement('span', {
                className: 'badge rounded-pill bg-light text-dark border',
                text: `${item.vocabularyCount || 0} từ`
              })
            ]
          }),

          // 5. Cập nhật lần cuối
          createSafeElement('td', {
            className: 'text-center small text-secondary text-nowrap',
            text: formattedDate
          }),

          // 6. Trạng thái
          createSafeElement('td', {
            className: 'text-center',
            children: [
              createSafeElement('span', {
                className: badgeInfo.badgeClass,
                text: badgeInfo.label
              })
            ]
          }),

          // 7. Thao tác (Xem xét)
          createSafeElement('td', {
            className: 'text-end pe-3',
            children: [
              createSafeElement('a', {
                className: 'btn-chinese-primary py-1 px-3 text-decoration-none small text-nowrap',
                text: 'Xem xét',
                attrs: {
                  href: reviewUrl,
                  id: `btnReviewLesson_${item.lessonId}`,
                  'aria-label': `Xem xét bài học #${item.lessonId}: ${item.title}`
                }
              })
            ]
          })
        ]
      });

      this.elements.tableBody.appendChild(tr);
    }
  }

  renderPagination(page, size, totalElements, totalPages) {
    if (!this.elements.paginationNav) return;

    if (totalElements <= size && totalPages <= 1) {
      this.elements.paginationNav.classList.add('d-none');
      return;
    }

    const { currentPage, hasPrev, hasNext, startRecord, endRecord } = calculatePagination(
      page,
      size,
      totalElements,
      totalPages
    );

    // Summary text
    if (this.elements.paginationSummary) {
      this.elements.paginationSummary.textContent = `Hiển thị ${startRecord} - ${endRecord} trong ${totalElements} bài học chờ duyệt`;
    }

    // Buttons
    if (this.elements.paginationList) {
      clearContainer(this.elements.paginationList);

      // Prev button
      const prevLi = createSafeElement('li', {
        className: `page-item ${!hasPrev ? 'disabled' : ''}`,
        children: [
          createSafeElement('button', {
            className: 'page-link',
            text: '« Trước',
            attrs: {
              type: 'button',
              'aria-label': 'Trang trước',
              id: 'btnQueuePrevPage',
              ...(hasPrev ? {} : { disabled: 'true' })
            }
          })
        ]
      });
      if (hasPrev) {
        prevLi.querySelector('button').addEventListener('click', () => {
          this.goToPage(currentPage - 1);
        });
      }
      this.elements.paginationList.appendChild(prevLi);

      // Numbered pages (max 5 around current)
      const startP = Math.max(0, Math.min(currentPage - 2, totalPages - 5));
      const endP = Math.min(totalPages, Math.max(currentPage + 3, 5));

      for (let p = startP; p < endP; p++) {
        const isCurrent = p === currentPage;
        const pageLi = createSafeElement('li', {
          className: `page-item ${isCurrent ? 'active' : ''}`,
          children: [
            createSafeElement('button', {
              className: 'page-link',
              text: String(p + 1),
              attrs: {
                type: 'button',
                'aria-label': `Trang ${p + 1}`,
                ...(isCurrent ? { 'aria-current': 'page' } : {}),
                id: `btnQueuePage_${p + 1}`
              }
            })
          ]
        });
        if (!isCurrent) {
          pageLi.querySelector('button').addEventListener('click', () => {
            this.goToPage(p);
          });
        }
        this.elements.paginationList.appendChild(pageLi);
      }

      // Next button
      const nextLi = createSafeElement('li', {
        className: `page-item ${!hasNext ? 'disabled' : ''}`,
        children: [
          createSafeElement('button', {
            className: 'page-link',
            text: 'Sau »',
            attrs: {
              type: 'button',
              'aria-label': 'Trang sau',
              id: 'btnQueueNextPage',
              ...(hasNext ? {} : { disabled: 'true' })
            }
          })
        ]
      });
      if (hasNext) {
        nextLi.querySelector('button').addEventListener('click', () => {
          this.goToPage(currentPage + 1);
        });
      }
      this.elements.paginationList.appendChild(nextLi);
    }

    this.elements.paginationNav.classList.remove('d-none');
  }

  goToPage(newPage) {
    if (this.isLoading) return;
    this.currentPage = Math.max(0, Math.min(newPage, Math.max(0, this.totalPages - 1)));
    this.loadQueue();
  }
}

/**
 * Initializes the Moderator Queue page controller on DOMContentLoaded.
 * Guarded against execution when imported on non-moderator-queue pages.
 * 
 * @returns {ModeratorQueueController|null}
 */
export function initModeratorQueuePage() {
  if (typeof document === 'undefined') return null;
  const root = document.getElementById('moderatorQueueTableBody') || document.getElementById('moderatorQueueTable');
  if (!root) return null;

  const controller = new ModeratorQueueController();
  controller.init();
  return controller;
}

if (typeof window !== 'undefined' && typeof document !== 'undefined') {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      initModeratorQueuePage();
    });
  } else {
    initModeratorQueuePage();
  }
}


