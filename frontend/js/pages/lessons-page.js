/**
 * =============================================================================
 * PUBLIC LESSON CATALOG PAGE CONTROLLER (TASK 9C.1)
 * Module: frontend/js/pages/lessons-page.js
 * 
 * Responsibilities:
 * - Fetches approved public lessons from GET /api/v1/lessons?page=X&size=Y.
 * - Enforces zero-leak public contract: Only Approved lessons are displayed.
 * - Handles server-side pagination (PageResponse) with size switching (12, 20, 40).
 * - Implements three-state UI (Loading -> Empty -> Error + Retry).
 * - Pure helpers exported for unit testing (query serialization, pagination, date formatting).
 * - Safe DOM construction (zero unsafe innerHTML) defending against XSS.
 * - Accessible landmarks, pagination semantics (aria-current), and keyboard navigation.
 * =============================================================================
 */

import { apiClient } from '../api/api.js';
import { showLoading, showEmpty, showError } from '../ui/ui.js';
import { createSafeElement, clearContainer } from '../ui/security.js';
import { initNavbarAuth } from '../ui/nav.js';

/**
 * Parses standard Spring Boot ApiResponse<PageResponse<LessonSummaryResponse>>.
 * 
 * @param {any} envelope 
 * @returns {{
 *   items: Array<{
 *     lessonId: number,
 *     title: string,
 *     status: string,
 *     vocabularyCount: number,
 *     createdAt: string,
 *     updatedAt: string
 *   }>,
 *   page: number,
 *   size: number,
 *   totalElements: number,
 *   totalPages: number
 * }}
 */
export function parseLessonPageResponse(envelopeOrData) {
  if (!envelopeOrData || typeof envelopeOrData !== 'object') {
    return { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
  }

  // Support both wrapped ApiResponse { code, data: PageResponse } and unpacked PageResponse
  const data = (envelopeOrData.data && typeof envelopeOrData.data === 'object')
    ? envelopeOrData.data
    : envelopeOrData;

  const items = Array.isArray(data.items) ? data.items : [];
  const page = Math.max(0, Number(data.page) || 0);
  const size = Math.max(1, Number(data.size) || 20);
  const totalElements = typeof data.totalElements === 'number' ? Math.max(0, data.totalElements) : items.length;
  const totalPages = typeof data.totalPages === 'number' ? Math.max(0, data.totalPages) : (totalElements > 0 ? Math.ceil(totalElements / size) : 0);

  return { items, page, size, totalElements, totalPages };
}

/**
 * Calculates pagination boundaries and navigation state.
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
 * Formats an ISO date string (e.g. "2026-08-30T15:17:43") into Vietnamese display (DD/MM/YYYY).
 * 
 * @param {string} isoString 
 * @returns {string}
 */
export function formatLessonDate(isoString) {
  if (!isoString || typeof isoString !== 'string') {
    return '';
  }
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) {
      return '';
    }
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    return `${day}/${month}/${year}`;
  } catch {
    return '';
  }
}

/**
 * Sanitizes lesson title to prevent malicious injection or broken strings.
 * 
 * @param {string} title 
 * @returns {string}
 */
export function sanitizeLessonTitle(title) {
  if (typeof title !== 'string') {
    return 'Bài học không tên';
  }
  const trimmed = title.trim();
  return trimmed.length > 0 ? trimmed : 'Bài học không tên';
}

/**
 * Controller managing the public lessons catalog page.
 */
export class LessonsPageController {
  constructor() {
    this.state = {
      currentPage: 0,
      currentSize: 20,
      totalPages: 0,
      totalElements: 0,
      items: [],
      latestRequestId: 0,
      abortController: null
    };

    this.dom = {
      stateContainer: null,
      grid: null,
      pagination: null,
      pageSizeSelect: null,
      resultCount: null
    };
  }

  /**
   * Initializes DOM bindings and triggers catalog load.
   */
  init() {
    this.dom.stateContainer = document.getElementById('lessonsStateContainer');
    this.dom.grid = document.getElementById('lessonsGrid');
    this.dom.pagination = document.getElementById('lessonsPagination');
    this.dom.pageSizeSelect = document.getElementById('pageSizeSelect');
    this.dom.resultCount = document.getElementById('lessonsResultCount');

    if (!this.dom.grid || !this.dom.stateContainer) {
      return;
    }

    // Bind page size change
    if (this.dom.pageSizeSelect) {
      this.dom.pageSizeSelect.addEventListener('change', (e) => {
        const newSize = parseInt(e.target.value, 10);
        if ([12, 20, 40].includes(newSize)) {
          this.state.currentSize = newSize;
          this.state.currentPage = 0;
          this.loadLessons();
        }
      });
    }

    this.loadLessons();
  }

  /**
   * Fetches public lessons from backend with request cancellation and sequencing.
   */
  async loadLessons() {
    // Cancel pending request if any
    if (this.state.abortController) {
      this.state.abortController.abort();
    }
    this.state.abortController = new AbortController();
    const currentRequestId = ++this.state.latestRequestId;

    showLoading(this.dom.stateContainer, 'Đang tải danh mục bài học...');
    this.clearGridAndPagination();

    if (this.dom.resultCount) {
      this.dom.resultCount.textContent = 'Đang tải...';
    }

    try {
      const envelope = await apiClient('/lessons', {
        params: {
          page: this.state.currentPage,
          size: this.state.currentSize
        },
        signal: this.state.abortController.signal
      });

      // Drop stale response if superseded
      if (currentRequestId !== this.state.latestRequestId) {
        return;
      }

      clearContainer(this.dom.stateContainer);

      const parsed = parseLessonPageResponse(envelope);
      this.state.items = parsed.items;
      this.state.currentPage = parsed.page;
      this.state.currentSize = parsed.size;
      this.state.totalElements = parsed.totalElements;
      this.state.totalPages = parsed.totalPages;

      if (this.state.totalElements === 0 || this.state.items.length === 0) {
        showEmpty(this.dom.stateContainer, {
          title: 'Chưa có bài học công khai',
          description: 'Hiện tại chưa có bài học nào được phê duyệt xuất bản. Vui lòng quay lại sau!'
        });
        if (this.dom.resultCount) {
          this.dom.resultCount.textContent = '0 bài học';
        }
        return;
      }

      // Update result count status
      if (this.dom.resultCount) {
        const p = calculatePagination(this.state.currentPage, this.state.currentSize, this.state.totalElements, this.state.totalPages);
        this.dom.resultCount.textContent = `Hiển thị ${p.startRecord}–${p.endRecord} / ${this.state.totalElements} bài học`;
      }

      this.renderLessonsGrid();
      this.renderPagination();

    } catch (err) {
      if (err.name === 'AbortError') {
        return; // Silent cancellation
      }

      if (currentRequestId !== this.state.latestRequestId) {
        return;
      }

      showError(this.dom.stateContainer, {
        title: 'Lỗi tải bài học',
        message: err.message || 'Không thể tải danh sách bài học. Vui lòng thử lại.',
        onRetry: () => this.loadLessons()
      });

      if (this.dom.resultCount) {
        this.dom.resultCount.textContent = 'Lỗi kết nối';
      }
    }
  }

  /**
   * Renders lesson cards into #lessonsGrid using safe DOM APIs.
   */
  renderLessonsGrid() {
    clearContainer(this.dom.grid);

    for (const lesson of this.state.items) {
      const article = createSafeElement('article', {
        className: 'lesson-card',
        attrs: { role: 'listitem' }
      });

      // Header: Title & status badge
      const header = createSafeElement('div', { className: 'lesson-card-header' });
      
      const titleHeading = createSafeElement('h2', { className: 'lesson-card-title' });
      const titleLink = createSafeElement('a', {
        text: sanitizeLessonTitle(lesson.title),
        attrs: {
          href: `lesson-detail.html?id=${encodeURIComponent(lesson.lessonId)}`,
          id: `lessonTitleLink_${lesson.lessonId}`
        }
      });
      titleHeading.appendChild(titleLink);
      header.appendChild(titleHeading);

      // Metadata: Vocabulary count & Date
      const meta = createSafeElement('div', { className: 'lesson-card-meta' });
      
      const vocabBadge = createSafeElement('span', {
        className: 'lesson-vocab-count-badge',
        text: `${lesson.vocabularyCount || 0} từ vựng`
      });
      meta.appendChild(vocabBadge);

      const dateStr = formatLessonDate(lesson.updatedAt || lesson.createdAt);
      if (dateStr) {
        const timeElem = createSafeElement('time', {
          className: 'lesson-date',
          text: `Cập nhật: ${dateStr}`,
          attrs: {
            datetime: lesson.updatedAt || lesson.createdAt || ''
          }
        });
        meta.appendChild(timeElem);
      }
      header.appendChild(meta);

      article.appendChild(header);

      // Footer: Action button
      const footer = createSafeElement('div', { className: 'lesson-card-footer' });
      
      const approvedBadge = createSafeElement('span', {
        className: 'badge-status badge-status-approved',
        text: 'Đã duyệt'
      });
      footer.appendChild(approvedBadge);

      const ctaLink = createSafeElement('a', {
        className: 'btn-chinese-primary py-1 px-3 text-decoration-none small',
        text: 'Xem bài học →',
        attrs: {
          href: `lesson-detail.html?id=${encodeURIComponent(lesson.lessonId)}`,
          'aria-label': `Xem chi tiết bài học ${sanitizeLessonTitle(lesson.title)}`
        }
      });
      footer.appendChild(ctaLink);

      article.appendChild(footer);
      this.dom.grid.appendChild(article);
    }
  }

  /**
   * Renders accessible pagination controls.
   */
  renderPagination() {
    clearContainer(this.dom.pagination);

    if (this.state.totalPages <= 1) {
      return;
    }

    const navList = createSafeElement('ul', {
      className: 'pagination mb-0 gap-1 flex-wrap justify-content-center'
    });

    // Previous Button
    const prevLi = createSafeElement('li', {
      className: `page-item ${this.state.currentPage === 0 ? 'disabled' : ''}`
    });
    const prevBtn = createSafeElement('button', {
      className: 'page-link',
      text: '‹ Trước',
      attrs: {
        type: 'button',
        id: 'lessonPaginationPrevBtn',
        'aria-label': 'Trang trước',
        ...(this.state.currentPage === 0 ? { disabled: 'true' } : {})
      }
    });
    if (this.state.currentPage > 0) {
      prevBtn.addEventListener('click', () => {
        this.state.currentPage--;
        this.loadLessons();
      });
    }
    prevLi.appendChild(prevBtn);
    navList.appendChild(prevLi);

    // Page Number Buttons
    for (let i = 0; i < this.state.totalPages; i++) {
      const isCurrent = i === this.state.currentPage;
      const pageLi = createSafeElement('li', {
        className: `page-item ${isCurrent ? 'active' : ''}`
      });
      const pageBtn = createSafeElement('button', {
        className: 'page-link',
        text: String(i + 1),
        attrs: {
          type: 'button',
          'aria-label': `Trang ${i + 1}`,
          ...(isCurrent ? { 'aria-current': 'page' } : {})
        }
      });

      if (!isCurrent) {
        pageBtn.addEventListener('click', () => {
          this.state.currentPage = i;
          this.loadLessons();
        });
      }

      pageLi.appendChild(pageBtn);
      navList.appendChild(pageLi);
    }

    // Next Button
    const nextLi = createSafeElement('li', {
      className: `page-item ${this.state.currentPage >= this.state.totalPages - 1 ? 'disabled' : ''}`
    });
    const nextBtn = createSafeElement('button', {
      className: 'page-link',
      text: 'Sau ›',
      attrs: {
        type: 'button',
        id: 'lessonPaginationNextBtn',
        'aria-label': 'Trang sau',
        ...(this.state.currentPage >= this.state.totalPages - 1 ? { disabled: 'true' } : {})
      }
    });
    if (this.state.currentPage < this.state.totalPages - 1) {
      nextBtn.addEventListener('click', () => {
        this.state.currentPage++;
        this.loadLessons();
      });
    }
    nextLi.appendChild(nextBtn);
    navList.appendChild(nextLi);

    this.dom.pagination.appendChild(navList);
  }

  /**
   * Cleans up grid and pagination.
   */
  clearGridAndPagination() {
    if (this.dom.grid) {
      clearContainer(this.dom.grid);
    }
    if (this.dom.pagination) {
      clearContainer(this.dom.pagination);
    }
  }
}

// Auto-instantiate if on lessons.html
if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    initNavbarAuth('navAuthContainer');
    if (document.getElementById('lessonsGrid')) {
      const controller = new LessonsPageController();
      controller.init();
    }
  });
}
