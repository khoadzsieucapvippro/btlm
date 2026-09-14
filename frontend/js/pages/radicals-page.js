/**
 * =============================================================================
 * KANGXI RADICALS CATALOG & PRESENTATION CONTROLLER (TASK 9B.2)
 * Module: frontend/js/pages/radicals-page.js
 * 
 * Responsibilities:
 * - Direct integration with Spring Boot backend via apiClient('/radicals').
 * - Robust defensive data loading (1-request size=214 with automatic multi-page fallback).
 * - Full-catalog invariant validation (1..214 sequential radicalId, no duplicates, no missing).
 * - High-speed, deterministic client-side search across character, meaningHanViet, meaningVi.
 * - Vietnamese accent-sensitive and accent-insensitive search normalization.
 * - Responsive client-side pagination with dynamic page size (24, 48, 96, 214).
 * - Fully accessible Detail Modal (HTML5 <dialog> with APG keyboard & focus restoration).
 * - Contextual media sanitization (audio/video) via sanitizeResourceUrl().
 * - 3-State UI lifecycle (Loading -> Ready / Empty / Error + Retry).
 * - Session-aware navbar integration via initNavbarAuth().
 * =============================================================================
 */

import { apiClient, ApiError } from '../api/api.js';
import { initNavbarAuth } from '../ui/nav.js';
import { createSafeElement, clearContainer, sanitizeResourceUrl } from '../ui/security.js';
import { setComponentState, showToast } from '../ui/ui.js';

/* -----------------------------------------------------------------------------
 * 1. PURE LOGIC & UTILITY HELPERS (Exported for Testability)
 * ----------------------------------------------------------------------------- */

/**
 * Removes Vietnamese tone marks and diacritics for flexible fuzzy searching
 * (e.g. "nuoc" matches "Nước", "moc" matches "Mộc").
 * Does NOT alter Chinese characters or standard Latin characters.
 * 
 * @param {string} str 
 * @returns {string}
 */
export function removeVietnameseDiacritics(str) {
  if (!str || typeof str !== 'string') return '';
  return str
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D');
}

/**
 * Normalizes user search input string by trimming whitespace and lowercasing.
 * 
 * @param {*} query 
 * @returns {string}
 */
export function normalizeSearchQuery(query) {
  if (query === null || query === undefined) return '';
  return String(query).trim().toLowerCase();
}

/**
 * Strictly verifies Kangxi Radical dataset invariants:
 * - Must be an array.
 * - If expectedTotal === 214, must have exactly 214 records.
 * - All radical IDs must be positive integers between 1 and expectedTotal.
 * - No duplicate radical IDs.
 * - No missing radical IDs (1..expectedTotal unbroken sequence).
 * 
 * @param {Array<Object>} items 
 * @param {number} [expectedTotal=214] 
 * @returns {{ valid: boolean, errors: string[] }}
 */
export function validateRadicalDataset(items, expectedTotal = 214) {
  const errors = [];

  if (!Array.isArray(items)) {
    return { valid: false, errors: ['Dữ liệu bộ thủ không phải là một mảng hợp lệ.'] };
  }

  if (expectedTotal > 0 && items.length !== expectedTotal) {
    errors.push(`Số lượng bộ thủ (${items.length}) không khớp với chỉ tiêu chuẩn (${expectedTotal}).`);
  }

  const seenIds = new Set();
  const duplicateIds = new Set();
  const invalidIds = [];

  for (const item of items) {
    if (!item || typeof item !== 'object') {
      errors.push('Phát hiện bản ghi bộ thủ rỗng hoặc không hợp lệ.');
      continue;
    }

    const id = Number(item.radicalId);
    if (!Number.isInteger(id) || id <= 0 || (expectedTotal > 0 && id > expectedTotal)) {
      invalidIds.push(item.radicalId);
      continue;
    }

    if (seenIds.has(id)) {
      duplicateIds.add(id);
    } else {
      seenIds.add(id);
    }
  }

  if (invalidIds.length > 0) {
    errors.push(`Phát hiện ID bộ thủ nằm ngoài phạm vi cho phép (1..${expectedTotal}): ${invalidIds.join(', ')}`);
  }

  if (duplicateIds.size > 0) {
    errors.push(`Phát hiện ID bộ thủ bị trùng lặp: ${Array.from(duplicateIds).join(', ')}`);
  }

  if (expectedTotal > 0 && seenIds.size !== expectedTotal) {
    const missing = [];
    for (let i = 1; i <= expectedTotal; i++) {
      if (!seenIds.has(i)) {
        missing.push(i);
      }
    }
    if (missing.length > 0 && missing.length <= 10) {
      errors.push(`Thiếu các bộ thủ có ID: ${missing.join(', ')}`);
    } else if (missing.length > 10) {
      errors.push(`Thiếu ${missing.length} bộ thủ trong danh mục (ví dụ: ${missing.slice(0, 5).join(', ')}...)`);
    }
  }

  return {
    valid: errors.length === 0,
    errors
  };
}

/**
 * Filters radicals client-side across:
 * 1. character (Chinese glyph substring/exact)
 * 2. meaningHanViet (Hán-Việt reading with/without tone accents)
 * 3. meaningVi (Vietnamese definition with/without tone accents)
 * 
 * @param {string} rawQuery 
 * @param {Array<Object>} items 
 * @returns {Array<Object>}
 */
export function filterRadicals(rawQuery, items) {
  if (!Array.isArray(items)) return [];
  const query = normalizeSearchQuery(rawQuery);
  if (!query) return items;

  const queryNoDiacritics = removeVietnameseDiacritics(query);

  return items.filter(item => {
    if (!item) return false;

    // 1. Chinese character match
    if (item.character && String(item.character).includes(query)) {
      return true;
    }

    // 2. Hán-Việt meaning match (with and without diacritics)
    if (item.meaningHanViet) {
      const hv = String(item.meaningHanViet).toLowerCase();
      if (hv.includes(query) || removeVietnameseDiacritics(hv).includes(queryNoDiacritics)) {
        return true;
      }
    }

    // 3. Vietnamese meaning match (with and without diacritics)
    if (item.meaningVi) {
      const vi = String(item.meaningVi).toLowerCase();
      if (vi.includes(query) || removeVietnameseDiacritics(vi).includes(queryNoDiacritics)) {
        return true;
      }
    }

    return false;
  });
}

/**
 * Calculates pagination slicing and bounds.
 * 
 * @param {number} totalItems 
 * @param {number} pageIndex - 0-indexed page number
 * @param {number} pageSize 
 * @returns {{ pageIndex: number, pageSize: number, totalPages: number, startIndex: number, endIndex: number }}
 */
export function calculatePagination(totalItems, pageIndex, pageSize) {
  const safeTotal = Math.max(0, Number(totalItems) || 0);
  const safeSize = Math.max(1, Number(pageSize) || 24);
  const totalPages = Math.max(1, Math.ceil(safeTotal / safeSize));
  const clampedPage = Math.max(0, Math.min(Number(pageIndex) || 0, totalPages - 1));

  const startIndex = clampedPage * safeSize;
  const endIndex = Math.min(safeTotal, startIndex + safeSize);

  return {
    pageIndex: clampedPage,
    pageSize: safeSize,
    totalPages,
    startIndex,
    endIndex
  };
}

/* -----------------------------------------------------------------------------
 * 2. RADICALS PAGE CONTROLLER CLASS
 * ----------------------------------------------------------------------------- */

export class RadicalsPageController {
  constructor() {
    // Application State
    this.fullCatalog = [];
    this.filteredCatalog = [];
    this.currentPage = 0;
    this.pageSize = 214; // Default to full 214 view for rapid scanning
    this.activeSearchQuery = '';
    this.activeTriggerEl = null;

    // DOM Element References
    this.elements = {};
  }

  /**
   * Initializes DOM references and event listeners.
   */
  init() {
    this.bindDomElements();
    this.bindEvents();

    // Initialize session-aware navigation bar
    initNavbarAuth('navAuthContainer');

    // Kick off catalog loading
    this.loadCatalog();
  }

  /**
   * Resolves interactive DOM nodes.
   */
  bindDomElements() {
    this.elements = {
      stateContainer: document.getElementById('radicalStateContainer'),
      grid: document.getElementById('radicalGrid'),
      pagination: document.getElementById('radicalPagination'),
      searchInput: document.getElementById('radicalSearchInput'),
      searchClearBtn: document.getElementById('searchClearBtn'),
      pageSizeSelect: document.getElementById('pageSizeSelect'),
      resultCount: document.getElementById('radicalResultCount'),
      modal: document.getElementById('radicalDetailModal'),
      modalCloseBtn: document.getElementById('modalCloseBtn'),
      modalDismissBtn: document.getElementById('modalDismissBtn'),
      modalRadicalIdBadge: document.getElementById('modalRadicalIdBadge'),
      modalRadicalChar: document.getElementById('modalRadicalChar'),
      modalRadicalTitle: document.getElementById('modalRadicalTitle'),
      modalRadicalPinyin: document.getElementById('modalRadicalPinyin'),
      modalRadicalHanViet: document.getElementById('modalRadicalHanViet'),
      modalRadicalIdValue: document.getElementById('modalRadicalIdValue'),
      modalRadicalMeaning: document.getElementById('modalRadicalMeaning'),
      modalAudioSection: document.getElementById('modalAudioSection'),
      modalAudioPlayer: document.getElementById('modalAudioPlayer'),
      modalVideoSection: document.getElementById('modalVideoSection'),
      modalVideoPlayer: document.getElementById('modalVideoPlayer')
    };
  }

  /**
   * Binds user event handlers.
   */
  bindEvents() {
    // Search input handler
    if (this.elements.searchInput) {
      this.elements.searchInput.addEventListener('input', (e) => {
        this.handleSearchInput(e.target.value);
      });
    }

    // Search clear button
    if (this.elements.searchClearBtn) {
      this.elements.searchClearBtn.addEventListener('click', () => {
        if (this.elements.searchInput) {
          this.elements.searchInput.value = '';
          this.elements.searchInput.focus();
        }
        this.handleSearchInput('');
      });
    }

    // Page size change handler
    if (this.elements.pageSizeSelect) {
      this.elements.pageSizeSelect.addEventListener('change', (e) => {
        this.pageSize = parseInt(e.target.value, 10) || 214;
        this.currentPage = 0;
        this.renderCatalogView();
      });
    }

    // Modal close buttons
    if (this.elements.modalCloseBtn) {
      this.elements.modalCloseBtn.addEventListener('click', () => this.closeDetailModal());
    }
    if (this.elements.modalDismissBtn) {
      this.elements.modalDismissBtn.addEventListener('click', () => this.closeDetailModal());
    }

    // Modal Escape key and native cancel event
    if (this.elements.modal) {
      this.elements.modal.addEventListener('cancel', (e) => {
        e.preventDefault();
        this.closeDetailModal();
      });

      // Close on backdrop click
      this.elements.modal.addEventListener('click', (e) => {
        const rect = this.elements.modal.getBoundingClientRect();
        const isInDialog = (
          rect.top <= e.clientY && e.clientY <= rect.top + rect.height &&
          rect.left <= e.clientX && e.clientX <= rect.left + rect.width
        );
        if (!isInDialog) {
          this.closeDetailModal();
        }
      });

      // APG Dialog Pattern: Constrain sequential keyboard Tab navigation within modal dialog
      this.elements.modal.addEventListener('keydown', (e) => {
        if (e.key !== 'Tab') return;
        const focusable = Array.from(
          this.elements.modal.querySelectorAll('button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])')
        ).filter(el => el.offsetWidth > 0 || el.offsetHeight > 0 || el.getClientRects().length > 0);

        if (focusable.length === 0) {
          e.preventDefault();
          return;
        }

        const first = focusable[0];
        const last = focusable[focusable.length - 1];

        if (e.shiftKey) {
          if (document.activeElement === first) {
            last.focus();
            e.preventDefault();
          }
        } else {
          if (document.activeElement === last) {
            first.focus();
            e.preventDefault();
          }
        }
      });
    }
  }

  /**
   * Fetches full Kangxi catalog with defensive pagination fallback and integrity validation.
   */
  async loadCatalog() {
    if (!this.elements.stateContainer) return;

    setComponentState(this.elements.stateContainer, 'LOADING', {
      message: 'Đang tải danh mục 214 bộ thủ Khang Hy...'
    });

    if (this.elements.grid) clearContainer(this.elements.grid);
    if (this.elements.pagination) this.elements.pagination.classList.add('d-none');

    try {
      // Step 1: Initial fetch with size=214
      const firstPageResp = await apiClient('/radicals', {
        params: { page: 0, size: 214 }
      });

      const items = Array.isArray(firstPageResp?.items) ? [...firstPageResp.items] : [];
      const totalElements = Number(firstPageResp?.totalElements) || items.length;
      const totalPages = Number(firstPageResp?.totalPages) || 1;

      // Step 2: Defensive fallback if backend truncated page size (e.g. capped at 20 or 50)
      if (totalPages > 1 && items.length < totalElements) {
        for (let page = 1; page < totalPages; page++) {
          const nextPageResp = await apiClient('/radicals', {
            params: { page, size: firstPageResp.size || 20 }
          });
          if (Array.isArray(nextPageResp?.items)) {
            items.push(...nextPageResp.items);
          }
        }
      }

      // Step 3: Verify ordering (radicalId ASC)
      items.sort((a, b) => (Number(a.radicalId) || 0) - (Number(b.radicalId) || 0));

      // Step 4: Validate dataset integrity (1..214 sequential)
      const integrity = validateRadicalDataset(items, 214);
      if (!integrity.valid) {
        console.warn('[RadicalCatalog] Data integrity anomaly detected:', integrity.errors);
      }

      // Step 5: Store authoritative catalog in memory
      this.fullCatalog = Object.freeze(items);
      this.filteredCatalog = this.fullCatalog;
      this.currentPage = 0;

      // Set ready state
      setComponentState(this.elements.stateContainer, 'READY');

      // Render view
      this.renderCatalogView();

    } catch (err) {
      console.error('[RadicalCatalog] Failed to load radicals catalog:', err);
      let errorTitle = 'Lỗi tải danh mục bộ thủ';
      let errorMessage = 'Không thể kết nối với máy chủ danh mục. Vui lòng kiểm tra lại.';

      if (err instanceof ApiError) {
        if (err.status === 0 || err.code === 'NETWORK_ERROR') {
          errorTitle = 'Không thể kết nối máy chủ';
          errorMessage = 'Không thể kết nối đến máy chủ backend (http://localhost:8080). Vui lòng kiểm tra trạng thái máy chủ.';
        } else if (err.status === 408 || err.code === 'TIMEOUT') {
          errorTitle = 'Hết thời gian chờ';
          errorMessage = 'Yêu cầu tải dữ liệu bộ thủ hết thời gian chờ (Timeout). Vui lòng thử lại.';
        } else if (err.status === 404 || err.code === 'NOT_FOUND') {
          errorTitle = 'Không tìm thấy tài nguyên';
          errorMessage = 'Dịch vụ danh mục bộ thủ không tồn tại (404). Vui lòng kiểm tra cấu hình máy chủ.';
        } else if (err.status >= 500) {
          errorTitle = 'Lỗi máy chủ nội bộ';
          errorMessage = err.message || `Máy chủ gặp sự cố khi xử lý dữ liệu (${err.status}). Vui lòng thử lại sau.`;
        } else {
          errorMessage = err.message || errorMessage;
        }
      }

      setComponentState(this.elements.stateContainer, 'ERROR', {
        title: errorTitle,
        message: errorMessage,
        retryText: 'Tải lại danh mục',
        onRetry: () => this.loadCatalog()
      });
    }
  }

  /**
   * Handles search filtering with instant client-side execution.
   * 
   * @param {string} query 
   */
  handleSearchInput(query) {
    this.activeSearchQuery = query;

    // Toggle clear button
    if (this.elements.searchClearBtn) {
      if (query.trim().length > 0) {
        this.elements.searchClearBtn.classList.remove('d-none');
      } else {
        this.elements.searchClearBtn.classList.add('d-none');
      }
    }

    // Filter full local dataset
    this.filteredCatalog = filterRadicals(query, this.fullCatalog);
    this.currentPage = 0; // Reset to page 1 on query change

    this.renderCatalogView();
  }

  /**
   * Authoritative view rendering routine.
   */
  renderCatalogView() {
    const totalFiltered = this.filteredCatalog.length;
    const totalAll = this.fullCatalog.length;

    // Update Result Count Badge
    if (this.elements.resultCount) {
      if (this.activeSearchQuery.trim().length > 0) {
        this.elements.resultCount.textContent = `Khớp ${totalFiltered} / ${totalAll} bộ thủ`;
        this.elements.resultCount.className = totalFiltered > 0
          ? 'badge-status badge-status-active text-nowrap'
          : 'badge-status badge-status-pending text-nowrap';
      } else {
        this.elements.resultCount.textContent = `Hiển thị ${totalFiltered} / ${totalAll} bộ thủ`;
        this.elements.resultCount.className = 'badge-status badge-status-draft text-nowrap';
      }
    }

    // Handle Empty Catalog (total dataset is empty)
    if (totalAll === 0) {
      if (this.elements.grid) clearContainer(this.elements.grid);
      if (this.elements.pagination) this.elements.pagination.classList.add('d-none');

      setComponentState(this.elements.stateContainer, 'EMPTY', {
        glyph: '空',
        title: 'Chưa có dữ liệu bộ thủ',
        description: 'Hệ thống hiện chưa có dữ liệu danh mục 214 bộ thủ Khang Hy.',
        actionText: 'Tải lại danh mục',
        onAction: () => this.loadCatalog()
      });
      return;
    }

    // Handle Empty Search Result
    if (totalFiltered === 0) {
      if (this.elements.grid) clearContainer(this.elements.grid);
      if (this.elements.pagination) this.elements.pagination.classList.add('d-none');

      setComponentState(this.elements.stateContainer, 'EMPTY', {
        glyph: '空',
        title: 'Không tìm thấy bộ thủ phù hợp',
        description: `Không có bộ thủ nào khớp với từ khóa "${this.activeSearchQuery}". Bạn có thể thử tìm theo chữ Hán, âm Hán-Việt hoặc nghĩa tiếng Việt khác.`,
        actionText: 'Xóa bộ lọc tìm kiếm',
        onAction: () => {
          if (this.elements.searchInput) {
            this.elements.searchInput.value = '';
          }
          this.handleSearchInput('');
        }
      });
      return;
    }

    // Clear empty/error notices
    setComponentState(this.elements.stateContainer, 'READY');

    // Calculate pagination slice
    const pag = calculatePagination(totalFiltered, this.currentPage, this.pageSize);
    this.currentPage = pag.pageIndex;
    const pagedItems = this.filteredCatalog.slice(pag.startIndex, pag.endIndex);

    // Render cards grid
    this.renderGrid(pagedItems);

    // Render pagination controls
    this.renderPagination(pag);
  }

  /**
   * Safely renders radical cards into the grid.
   * 
   * @param {Array<Object>} items 
   */
  renderGrid(items) {
    if (!this.elements.grid) return;
    clearContainer(this.elements.grid);

    for (const radical of items) {
      const card = createSafeElement('button', {
        className: 'radical-card',
        attrs: {
          type: 'button',
          'data-radical-id': String(radical.radicalId),
          'aria-label': `Bộ thủ số ${radical.radicalId}: chữ ${radical.character}, âm ${radical.meaningHanViet}, nghĩa ${radical.meaningVi || 'chưa có'}`
        },
        children: [
          // Header: Radical ID
          createSafeElement('div', {
            className: 'radical-card-header',
            children: [
              createSafeElement('span', {
                className: 'radical-card-id',
                text: `#${radical.radicalId}`
              })
            ]
          }),

          // Center Glyph: Chinese Character
          createSafeElement('span', {
            className: 'radical-card-character',
            attrs: { lang: 'zh-Hans', 'aria-hidden': 'true' },
            text: radical.character || '?'
          }),

          // Pronunciation & Meanings
          createSafeElement('div', {
            className: 'w-100',
            children: [
              createSafeElement('div', {
                className: 'radical-card-pinyin',
                text: radical.pinyin || ''
              }),
              createSafeElement('div', {
                className: 'radical-card-hanviet',
                text: radical.meaningHanViet || ''
              }),
              createSafeElement('div', {
                className: 'radical-card-meaning',
                attrs: { title: radical.meaningVi || '' },
                text: radical.meaningVi || ''
              })
            ]
          })
        ]
      });

      // Bind click / keyboard activation
      card.addEventListener('click', (e) => {
        this.openDetailModal(radical, e.currentTarget);
      });

      this.elements.grid.appendChild(card);
    }
  }

  /**
   * Renders accessible pagination controls.
   * 
   * @param {Object} pag 
   */
  renderPagination(pag) {
    if (!this.elements.pagination) return;

    if (pag.totalPages <= 1) {
      this.elements.pagination.classList.add('d-none');
      clearContainer(this.elements.pagination);
      return;
    }

    this.elements.pagination.classList.remove('d-none');
    clearContainer(this.elements.pagination);

    // Summary info
    const summary = createSafeElement('div', {
      className: 'small text-secondary fw-medium',
      text: `Hiển thị ${pag.startIndex + 1}–${pag.endIndex} trong tổng số ${this.filteredCatalog.length} bộ thủ`
    });

    // Control buttons container
    const controls = createSafeElement('div', {
      className: 'pagination-controls',
      attrs: { role: 'group', 'aria-label': 'Điều khiển chuyển trang' }
    });

    // Previous button
    const prevBtn = createSafeElement('button', {
      className: 'pagination-btn',
      attrs: {
        type: 'button',
        'aria-label': 'Trang trước',
        id: 'paginationPrevBtn'
      },
      text: '‹'
    });
    if (pag.pageIndex === 0) {
      prevBtn.setAttribute('disabled', 'true');
    } else {
      prevBtn.addEventListener('click', () => {
        this.currentPage = pag.pageIndex - 1;
        this.renderCatalogView();
      });
    }
    controls.appendChild(prevBtn);

    // Page numbers (smart window)
    const maxButtons = 5;
    let startPage = Math.max(0, pag.pageIndex - Math.floor(maxButtons / 2));
    let endPage = Math.min(pag.totalPages - 1, startPage + maxButtons - 1);
    if (endPage - startPage < maxButtons - 1) {
      startPage = Math.max(0, endPage - maxButtons + 1);
    }

    for (let p = startPage; p <= endPage; p++) {
      const pageBtn = createSafeElement('button', {
        className: `pagination-btn ${p === pag.pageIndex ? 'active' : ''}`,
        attrs: {
          type: 'button',
          'aria-label': `Trang ${p + 1}`,
          'aria-current': p === pag.pageIndex ? 'page' : 'false'
        },
        text: String(p + 1)
      });

      if (p !== pag.pageIndex) {
        const targetPage = p;
        pageBtn.addEventListener('click', () => {
          this.currentPage = targetPage;
          this.renderCatalogView();
        });
      }

      controls.appendChild(pageBtn);
    }

    // Next button
    const nextBtn = createSafeElement('button', {
      className: 'pagination-btn',
      attrs: {
        type: 'button',
        'aria-label': 'Trang sau',
        id: 'paginationNextBtn'
      },
      text: '›'
    });
    if (pag.pageIndex >= pag.totalPages - 1) {
      nextBtn.setAttribute('disabled', 'true');
    } else {
      nextBtn.addEventListener('click', () => {
        this.currentPage = pag.pageIndex + 1;
        this.renderCatalogView();
      });
    }
    controls.appendChild(nextBtn);

    this.elements.pagination.appendChild(summary);
    this.elements.pagination.appendChild(controls);
  }

  /**
   * Opens the accessible Detail Modal for a radical.
   * Leverages cached catalog data instantly (0ms latency, zero spam requests).
   * 
   * @param {Object} radical 
   * @param {HTMLElement} triggerEl 
   */
  openDetailModal(radical, triggerEl) {
    if (!this.elements.modal) return;
    this.activeTriggerEl = triggerEl;

    // 1. Populate text content safely
    if (this.elements.modalRadicalIdBadge) {
      this.elements.modalRadicalIdBadge.textContent = `#${radical.radicalId}`;
    }
    if (this.elements.modalRadicalChar) {
      this.elements.modalRadicalChar.textContent = radical.character || '';
    }
    if (this.elements.modalRadicalTitle) {
      this.elements.modalRadicalTitle.textContent = `Bộ ${radical.meaningHanViet || ''} (${radical.character || ''})`;
    }
    if (this.elements.modalRadicalPinyin) {
      this.elements.modalRadicalPinyin.textContent = radical.pinyin || '—';
    }
    if (this.elements.modalRadicalHanViet) {
      this.elements.modalRadicalHanViet.textContent = radical.meaningHanViet || '—';
    }
    if (this.elements.modalRadicalIdValue) {
      this.elements.modalRadicalIdValue.textContent = `#${radical.radicalId} / 214`;
    }
    if (this.elements.modalRadicalMeaning) {
      this.elements.modalRadicalMeaning.textContent = radical.meaningVi || 'Chưa có giải nghĩa chi tiết.';
    }

    // 2. Safe Audio Player Handling
    if (this.elements.modalAudioSection && this.elements.modalAudioPlayer) {
      const rawAudio = radical.audioUrl;
      const safeAudio = sanitizeResourceUrl(rawAudio);

      if (rawAudio && safeAudio !== 'about:blank') {
        this.elements.modalAudioPlayer.pause();
        this.elements.modalAudioPlayer.src = safeAudio;
        this.elements.modalAudioPlayer.load();
        this.elements.modalAudioSection.classList.remove('d-none');
      } else {
        this.elements.modalAudioPlayer.pause();
        this.elements.modalAudioPlayer.removeAttribute('src');
        this.elements.modalAudioSection.classList.add('d-none');
      }
    }

    // 3. Safe Video Demonstration Handling
    if (this.elements.modalVideoSection && this.elements.modalVideoPlayer) {
      const rawVideo = radical.videoWritingUrl;
      const safeVideo = sanitizeResourceUrl(rawVideo);

      if (rawVideo && safeVideo !== 'about:blank') {
        this.elements.modalVideoPlayer.pause();
        this.elements.modalVideoPlayer.src = safeVideo;
        this.elements.modalVideoPlayer.load();
        this.elements.modalVideoSection.classList.remove('d-none');
      } else {
        this.elements.modalVideoPlayer.pause();
        this.elements.modalVideoPlayer.removeAttribute('src');
        this.elements.modalVideoSection.classList.add('d-none');
      }
    }

    // 4. Open Modal with Native Focus Trap / Management
    if (typeof this.elements.modal.showModal === 'function') {
      this.elements.modal.showModal();
    } else {
      this.elements.modal.setAttribute('open', '');
      this.elements.modal.style.display = 'block';
    }

    // Move initial focus into modal close button for accessibility
    if (this.elements.modalCloseBtn) {
      this.elements.modalCloseBtn.focus();
    }
  }

  /**
   * Closes the Detail Modal and restores focus to the triggering element.
   */
  closeDetailModal() {
    if (!this.elements.modal) return;

    // Pause any active media
    if (this.elements.modalAudioPlayer) {
      this.elements.modalAudioPlayer.pause();
    }
    if (this.elements.modalVideoPlayer) {
      this.elements.modalVideoPlayer.pause();
    }

    // Close modal
    if (typeof this.elements.modal.close === 'function') {
      this.elements.modal.close();
    } else {
      this.elements.modal.removeAttribute('open');
      this.elements.modal.style.display = 'none';
    }

    // Restore focus to triggering card (WCAG 2.2 APG Pattern)
    if (this.activeTriggerEl && typeof this.activeTriggerEl.focus === 'function') {
      this.activeTriggerEl.focus();
    }
    this.activeTriggerEl = null;
  }
}

/* -----------------------------------------------------------------------------
 * 3. BROWSER ENTRY POINT INITIALIZATION
 * ----------------------------------------------------------------------------- */
if (typeof window !== 'undefined' && typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    const controller = new RadicalsPageController();
    controller.init();
    window.__radicalsPageController = controller;
  });
}
