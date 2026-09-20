/**
 * =============================================================================
 * ADMIN RADICALS CRUD CONTROLLER (TASK 9F.3)
 * Module: frontend/js/pages/admin-radicals-page.js
 * 
 * Responsibilities:
 * - Direct integration with Spring Boot authoritative backend endpoints:
 *   * GET    /api/v1/radicals (Catalog lookup via public endpoint)
 *   * POST   /api/v1/admin/radicals (Create a Kangxi radical -> 201 Created)
 *   * PUT    /api/v1/admin/radicals/{id} (Update radical -> 200 OK)
 *   * DELETE /api/v1/admin/radicals/{id} (Delete radical -> 204 No Content)
 * - Role-guarded access: strictly accessible to 'Admin' (authManager.hasRole('Admin')).
 * - Client-side search & diacritic-tolerant filtering across character, pinyin, Hán-Việt, Vietnamese meaning.
 * - Dynamic catalog sizing: reflects server state without hardcoding count === 214.
 * - Structured conflict handling:
 *   * Duplicate character create/update (409 CONFLICT) -> human-readable feedback.
 *   * Referenced-radical deletion (409 CONFLICT) -> explains foreign key / vocabulary linkage constraint.
 * - Native HTML5 <dialog> modals with focus trapping, focus restoration, and Escape handling.
 * - Safe DOM construction (zero unsafe innerHTML) and sanitized media URLs (sanitizeResourceUrl).
 * - Three-state UI lifecycle (Loading -> Ready / Empty / Error + Retry).
 * - Non-blocking toast notifications for administrative actions.
 * =============================================================================
 */

import { apiClient, ApiError } from '../api/api.js';
import { authManager } from '../auth/auth-state.js';
import { initNavbarAuth } from '../ui/nav.js';
import { createSafeElement, clearContainer, sanitizeResourceUrl } from '../ui/security.js';
import { setComponentState, showToast } from '../ui/ui.js';

/* -----------------------------------------------------------------------------
 * 1. PURE LOGIC & UTILITY HELPERS (Exported for Unit Testability)
 * ----------------------------------------------------------------------------- */

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
 * Removes Vietnamese tone marks and diacritics for flexible fuzzy searching.
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
 * Filters radicals client-side across:
 * 1. character (Chinese glyph substring/exact)
 * 2. pinyin (Pinyin transcription substring/exact)
 * 3. meaningHanViet (Hán-Việt reading with/without tone accents)
 * 4. meaningVi (Vietnamese definition with/without tone accents)
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

    // 2. Pinyin match
    if (item.pinyin) {
      const py = String(item.pinyin).toLowerCase();
      if (py.includes(query) || removeVietnameseDiacritics(py).includes(queryNoDiacritics)) {
        return true;
      }
    }

    // 3. Hán-Việt meaning match
    if (item.meaningHanViet) {
      const hv = String(item.meaningHanViet).toLowerCase();
      if (hv.includes(query) || removeVietnameseDiacritics(hv).includes(queryNoDiacritics)) {
        return true;
      }
    }

    // 4. Vietnamese meaning match
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
 * Validates radical creation or update form data against backend contract limits:
 * - character: required, 1..10 chars
 * - pinyin: required, 1..50 chars
 * - meaningHanViet: required, 1..100 chars
 * - meaningVi: required, 1..255 chars
 * - audioUrl: optional, max 500 chars, valid URI format if provided
 * - videoWritingUrl: optional, max 500 chars, valid URI format if provided
 * 
 * @param {object} data 
 * @returns {{ valid: boolean, errors: Record<string, string> }}
 */
export function validateRadicalForm(data) {
  const errors = {};
  if (!data || typeof data !== 'object') {
    return { valid: false, errors: { _general: 'Dữ liệu bộ thủ không hợp lệ.' } };
  }

  const character = typeof data.character === 'string' ? data.character.trim() : '';
  const pinyin = typeof data.pinyin === 'string' ? data.pinyin.trim() : '';
  const meaningHanViet = typeof data.meaningHanViet === 'string' ? data.meaningHanViet.trim() : '';
  const meaningVi = typeof data.meaningVi === 'string' ? data.meaningVi.trim() : '';
  const audioUrl = typeof data.audioUrl === 'string' ? data.audioUrl.trim() : '';
  const videoWritingUrl = typeof data.videoWritingUrl === 'string' ? data.videoWritingUrl.trim() : '';

  // Character validation
  if (!character) {
    errors.character = 'Ký tự bộ thủ không được để trống';
  } else if (character.length > 10) {
    errors.character = 'Ký tự bộ thủ không được vượt quá 10 ký tự';
  }

  // Pinyin validation
  if (!pinyin) {
    errors.pinyin = 'Pinyin không được để trống';
  } else if (pinyin.length > 50) {
    errors.pinyin = 'Pinyin không được vượt quá 50 ký tự';
  }

  // Meaning Han-Viet validation
  if (!meaningHanViet) {
    errors.meaningHanViet = 'Nghĩa Hán-Việt không được để trống';
  } else if (meaningHanViet.length > 100) {
    errors.meaningHanViet = 'Nghĩa Hán-Việt không được vượt quá 100 ký tự';
  }

  // Meaning Vietnamese validation
  if (!meaningVi) {
    errors.meaningVi = 'Nghĩa tiếng Việt không được để trống';
  } else if (meaningVi.length > 255) {
    errors.meaningVi = 'Nghĩa tiếng Việt không được vượt quá 255 ký tự';
  }

  // Audio URL validation (Optional)
  if (audioUrl) {
    if (audioUrl.length > 500) {
      errors.audioUrl = 'Đường dẫn audio không được vượt quá 500 ký tự';
    } else if (!isValidMediaUrl(audioUrl)) {
      errors.audioUrl = 'Đường dẫn audio phải là URL hợp lệ (bắt đầu bằng http://, https:// hoặc /)';
    }
  }

  // Video Writing URL validation (Optional)
  if (videoWritingUrl) {
    if (videoWritingUrl.length > 500) {
      errors.videoWritingUrl = 'Đường dẫn video không được vượt quá 500 ký tự';
    } else if (!isValidMediaUrl(videoWritingUrl)) {
      errors.videoWritingUrl = 'Đường dẫn video phải là URL hợp lệ (bắt đầu bằng http://, https:// hoặc /)';
    }
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors
  };
}

/**
 * Validates whether a media URL has a safe web scheme or relative path.
 * 
 * @param {string} url 
 * @returns {boolean}
 */
export function isValidMediaUrl(url) {
  if (!url || typeof url !== 'string') return false;
  const trimmed = url.trim();
  if (trimmed.startsWith('/') && !trimmed.startsWith('//')) {
    return true; // Safe relative path
  }
  return /^https?:\/\//i.test(trimmed);
}

/**
 * Formats clean request payload for CreateRadicalRequest or UpdateRadicalRequest.
 * Optional URL fields with empty or whitespace-only content are normalized to null.
 * 
 * @param {object} formData 
 * @returns {{ character: string, pinyin: string, meaningHanViet: string, meaningVi: string, audioUrl: string|null, videoWritingUrl: string|null }}
 */
export function buildRadicalPayload(formData) {
  const char = typeof formData.character === 'string' ? formData.character.trim() : '';
  const pinyin = typeof formData.pinyin === 'string' ? formData.pinyin.trim() : '';
  const hanViet = typeof formData.meaningHanViet === 'string' ? formData.meaningHanViet.trim() : '';
  const vi = typeof formData.meaningVi === 'string' ? formData.meaningVi.trim() : '';
  const audio = typeof formData.audioUrl === 'string' ? formData.audioUrl.trim() : '';
  const video = typeof formData.videoWritingUrl === 'string' ? formData.videoWritingUrl.trim() : '';

  return {
    character: char,
    pinyin: pinyin,
    meaningHanViet: hanViet,
    meaningVi: vi,
    audioUrl: audio.length > 0 ? audio : null,
    videoWritingUrl: video.length > 0 ? video : null
  };
}

/**
 * Classifies an API error for human-readable feedback.
 * Distinguishes 409 Conflict (duplicate character vs referenced deletion), 404 Not Found, 400 Validation, and Network errors.
 * 
 * @param {Error|ApiError|any} err 
 * @param {'create'|'update'|'delete'|'load'} [operation='create']
 * @returns {{ isConflict: boolean, isNotFound: boolean, isValidation: boolean, status: number, userMessage: string, fieldErrors: Record<string, string> }}
 */
export function classifyApiError(err, operation = 'create') {
  const status = Number(err?.status) || 0;
  const rawMessage = typeof err?.message === 'string' ? err.message : '';
  const isConflict = status === 409;
  const isNotFound = status === 404;
  const isValidation = status === 400;
  const fieldErrors = {};

  if (Array.isArray(err?.errors)) {
    for (const item of err.errors) {
      if (typeof item === 'string') {
        const colonIndex = item.indexOf(':');
        if (colonIndex > 0) {
          const field = item.substring(0, colonIndex).trim();
          const msg = item.substring(colonIndex + 1).trim();
          if (field && msg) fieldErrors[field] = msg;
        }
      }
    }
  }

  let userMessage = rawMessage;

  if (isConflict) {
    if (operation === 'delete') {
      userMessage = rawMessage || 'Không thể xóa bộ thủ đang được liên kết với từ vựng hoặc dữ liệu bài học trong hệ thống.';
    } else {
      userMessage = rawMessage || 'Bộ thủ với ký tự này đã tồn tại trong hệ thống. Vui lòng kiểm tra lại.';
    }
  } else if (isNotFound) {
    userMessage = rawMessage || 'Không tìm thấy bộ thủ hoặc dữ liệu đã bị xóa bởi quản trị viên khác.';
  } else if (status === 403) {
    userMessage = 'Bạn không có quyền thực hiện thao tác quản trị này.';
  } else if (status === 401) {
    userMessage = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
  } else if (!userMessage) {
    if (status === 0 || err?.code === 'NETWORK_ERROR') {
      userMessage = 'Không thể kết nối đến máy chủ backend. Vui lòng kiểm tra lại đường truyền mạng.';
    } else {
      userMessage = 'Đã xảy ra sự cố trong quá trình xử lý. Vui lòng thử lại sau.';
    }
  }

  return {
    isConflict,
    isNotFound,
    isValidation,
    status,
    userMessage,
    fieldErrors
  };
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
  const safeSize = Math.max(1, Number(pageSize) || 50);
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
 * 2. ADMIN RADICALS PAGE CONTROLLER CLASS
 * ----------------------------------------------------------------------------- */

export class AdminRadicalsPageController {
  constructor() {
    // State
    this.fullCatalog = [];
    this.filteredCatalog = [];
    this.currentPage = 0;
    this.pageSize = 50;
    this.activeSearchQuery = '';
    this.activeRadicalForEdit = null;
    this.activeRadicalForDelete = null;
    this.isSubmitting = false;
    this.activeTriggerEl = null;

    // Element references
    this.elements = {};
  }

  /**
   * Initializes DOM references, security checks, and event wiring.
   */
  init() {
    this.bindDomElements();

    // Initialize session-aware navigation bar
    initNavbarAuth('navAuthContainer');

    // Role Guard Check
    const isAuthorized = hasAdminAccess();
    if (!isAuthorized) {
      if (this.elements.guardContainer) {
        this.elements.guardContainer.classList.remove('d-none');
      }
      if (this.elements.mainContent) {
        this.elements.mainContent.classList.add('d-none');
      }
      return;
    }

    if (this.elements.guardContainer) {
      this.elements.guardContainer.classList.add('d-none');
    }
    if (this.elements.mainContent) {
      this.elements.mainContent.classList.remove('d-none');
    }

    this.bindEvents();
    this.loadCatalog();
  }

  /**
   * Resolves interactive DOM nodes.
   */
  bindDomElements() {
    this.elements = {
      guardContainer: document.getElementById('adminAuthGuardContainer'),
      mainContent: document.getElementById('adminRadicalsMainContent'),
      stateContainer: document.getElementById('radicalStateContainer'),
      tableCard: document.getElementById('radicalTableCard'),
      tableBody: document.getElementById('radicalTableBody'),
      searchInput: document.getElementById('radicalSearchInput'),
      searchClearBtn: document.getElementById('searchClearBtn'),
      pageSizeSelect: document.getElementById('pageSizeSelect'),
      resultCount: document.getElementById('radicalResultCount'),
      paginationSummary: document.getElementById('paginationSummary'),
      paginationControls: document.getElementById('paginationControls'),
      btnOpenCreateModal: document.getElementById('btnOpenCreateModal'),

      // Form Modal
      formModal: document.getElementById('radicalFormModal'),
      modalFormTitle: document.getElementById('modalFormTitle'),
      modalFormDesc: document.getElementById('modalFormDesc'),
      modalFormCloseBtn: document.getElementById('modalFormCloseBtn'),
      radicalForm: document.getElementById('radicalForm'),
      radicalFormId: document.getElementById('radicalFormId'),
      formGeneralError: document.getElementById('formGeneralError'),
      inputCharacter: document.getElementById('inputCharacter'),
      inputPinyin: document.getElementById('inputPinyin'),
      inputMeaningHanViet: document.getElementById('inputMeaningHanViet'),
      inputMeaningVi: document.getElementById('inputMeaningVi'),
      inputAudioUrl: document.getElementById('inputAudioUrl'),
      inputVideoWritingUrl: document.getElementById('inputVideoWritingUrl'),
      errorCharacter: document.getElementById('errorCharacter'),
      errorPinyin: document.getElementById('errorPinyin'),
      errorMeaningHanViet: document.getElementById('errorMeaningHanViet'),
      errorMeaningVi: document.getElementById('errorMeaningVi'),
      errorAudioUrl: document.getElementById('errorAudioUrl'),
      errorVideoWritingUrl: document.getElementById('errorVideoWritingUrl'),
      btnCancelFormModal: document.getElementById('btnCancelFormModal'),
      btnSaveRadical: document.getElementById('btnSaveRadical'),
      saveRadicalBtnSpinner: document.getElementById('saveRadicalBtnSpinner'),
      saveRadicalBtnText: document.getElementById('saveRadicalBtnText'),

      // Delete Modal
      deleteModal: document.getElementById('deleteRadicalModal'),
      deleteModalTitle: document.getElementById('deleteModalTitle'),
      deleteModalCloseBtn: document.getElementById('deleteModalCloseBtn'),
      deleteTargetCharacter: document.getElementById('deleteTargetCharacter'),
      deleteTargetMeaning: document.getElementById('deleteTargetMeaning'),
      deleteTargetId: document.getElementById('deleteTargetId'),
      deleteModalError: document.getElementById('deleteModalError'),
      btnCancelDeleteModal: document.getElementById('btnCancelDeleteModal'),
      btnConfirmDeleteRadical: document.getElementById('btnConfirmDeleteRadical'),
      deleteRadicalBtnSpinner: document.getElementById('deleteRadicalBtnSpinner'),
      deleteRadicalBtnText: document.getElementById('deleteRadicalBtnText')
    };
  }

  /**
   * Binds user event listeners.
   */
  bindEvents() {
    // Search input
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

    // Page size select
    if (this.elements.pageSizeSelect) {
      this.elements.pageSizeSelect.addEventListener('change', (e) => {
        this.pageSize = parseInt(e.target.value, 10) || 50;
        this.currentPage = 0;
        this.renderCatalogView();
      });
    }

    // Open create modal button
    if (this.elements.btnOpenCreateModal) {
      this.elements.btnOpenCreateModal.addEventListener('click', () => {
        this.openCreateModal(this.elements.btnOpenCreateModal);
      });
    }

    // Form modal close/cancel
    if (this.elements.modalFormCloseBtn) {
      this.elements.modalFormCloseBtn.addEventListener('click', () => this.closeFormModal());
    }
    if (this.elements.btnCancelFormModal) {
      this.elements.btnCancelFormModal.addEventListener('click', () => this.closeFormModal());
    }

    // Form modal save
    if (this.elements.btnSaveRadical) {
      this.elements.btnSaveRadical.addEventListener('click', () => this.handleSaveRadical());
    }
    if (this.elements.radicalForm) {
      this.elements.radicalForm.addEventListener('submit', (e) => {
        e.preventDefault();
        this.handleSaveRadical();
      });
    }

    // Delete modal close/cancel
    if (this.elements.deleteModalCloseBtn) {
      this.elements.deleteModalCloseBtn.addEventListener('click', () => this.closeDeleteModal());
    }
    if (this.elements.btnCancelDeleteModal) {
      this.elements.btnCancelDeleteModal.addEventListener('click', () => this.closeDeleteModal());
    }

    // Delete modal confirm
    if (this.elements.btnConfirmDeleteRadical) {
      this.elements.btnConfirmDeleteRadical.addEventListener('click', () => this.handleConfirmDeleteRadical());
    }

    // Native backdrop clicks for modal 1
    if (this.elements.formModal) {
      this.elements.formModal.addEventListener('click', (e) => {
        const rect = this.elements.formModal.getBoundingClientRect();
        const isInDialog = (
          rect.top <= e.clientY && e.clientY <= rect.top + rect.height &&
          rect.left <= e.clientX && e.clientX <= rect.left + rect.width
        );
        if (!isInDialog && !this.isSubmitting) {
          this.closeFormModal();
        }
      });
      this.elements.formModal.addEventListener('cancel', (e) => {
        e.preventDefault();
        if (!this.isSubmitting) this.closeFormModal();
      });
    }

    // Native backdrop clicks for modal 2
    if (this.elements.deleteModal) {
      this.elements.deleteModal.addEventListener('click', (e) => {
        const rect = this.elements.deleteModal.getBoundingClientRect();
        const isInDialog = (
          rect.top <= e.clientY && e.clientY <= rect.top + rect.height &&
          rect.left <= e.clientX && e.clientX <= rect.left + rect.width
        );
        if (!isInDialog && !this.isSubmitting) {
          this.closeDeleteModal();
        }
      });
      this.elements.deleteModal.addEventListener('cancel', (e) => {
        e.preventDefault();
        if (!this.isSubmitting) this.closeDeleteModal();
      });
    }
  }

  /**
   * Fetches full radicals catalog via GET /api/v1/radicals with pagination aggregation.
   */
  async loadCatalog() {
    if (!this.elements.stateContainer) return;

    setComponentState(this.elements.stateContainer, 'LOADING', {
      message: 'Đang tải danh mục bộ thủ từ máy chủ...'
    });

    if (this.elements.tableCard) {
      this.elements.tableCard.classList.add('d-none');
    }

    try {
      // Step 1: Request catalog with large page size
      const firstPageResp = await apiClient('/radicals', {
        params: { page: 0, size: 250 }
      });

      const items = Array.isArray(firstPageResp?.items) ? [...firstPageResp.items] : [];
      const totalElements = Number(firstPageResp?.totalElements) || items.length;
      const totalPages = Number(firstPageResp?.totalPages) || 1;

      // Step 2: Defensive aggregation if backend caps max size
      if (totalPages > 1 && items.length < totalElements) {
        for (let page = 1; page < totalPages; page++) {
          const nextPageResp = await apiClient('/radicals', {
            params: { page, size: firstPageResp.size || 250 }
          });
          if (Array.isArray(nextPageResp?.items)) {
            items.push(...nextPageResp.items);
          }
        }
      }

      // Step 3: Sort by radicalId ascending
      items.sort((a, b) => (Number(a.radicalId) || 0) - (Number(b.radicalId) || 0));

      this.fullCatalog = items;
      this.filteredCatalog = filterRadicals(this.activeSearchQuery, this.fullCatalog);
      this.currentPage = 0;

      if (this.fullCatalog.length === 0) {
        setComponentState(this.elements.stateContainer, 'EMPTY', {
          title: 'Danh mục bộ thủ trống',
          message: 'Chưa có bộ thủ nào trong hệ thống. Nhấn nút "Thêm bộ thủ mới" phía trên để bắt đầu tạo.'
        });
      } else {
        setComponentState(this.elements.stateContainer, 'READY');
        if (this.elements.tableCard) {
          this.elements.tableCard.classList.remove('d-none');
        }
        this.renderCatalogView();
      }

    } catch (err) {
      console.error('[AdminRadicals] Failed to load catalog:', err);
      const classified = classifyApiError(err, 'load');
      setComponentState(this.elements.stateContainer, 'ERROR', {
        title: 'Lỗi tải danh mục bộ thủ',
        message: classified.userMessage,
        retryText: 'Thử lại',
        onRetry: () => this.loadCatalog()
      });
    }
  }

  /**
   * Handles user search query input.
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

    this.filteredCatalog = filterRadicals(query, this.fullCatalog);
    this.currentPage = 0;
    this.renderCatalogView();
  }

  /**
   * Authoritative render method for table, badge, and pagination.
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
        this.elements.resultCount.textContent = `Tổng cộng: ${totalAll} bộ thủ`;
        this.elements.resultCount.className = 'badge-status badge-status-draft text-nowrap';
      }
    }

    if (!this.elements.tableBody) return;
    clearContainer(this.elements.tableBody);

    if (totalFiltered === 0) {
      const tr = createSafeElement('tr', {
        children: [
          createSafeElement('td', {
            className: 'text-center py-5 text-secondary',
            attrs: { colspan: '7' },
            children: [
              createSafeElement('div', { className: 'mb-2 fs-3', text: '🔍' }),
              createSafeElement('div', { className: 'fw-semibold text-dark mb-1', text: 'Không tìm thấy bộ thủ phù hợp' }),
              createSafeElement('div', { className: 'small text-muted', text: `Không có kết quả nào khớp với từ khóa "${this.activeSearchQuery}".` })
            ]
          })
        ]
      });
      this.elements.tableBody.appendChild(tr);
      this.renderPagination(0, 0, this.pageSize);
      return;
    }

    // Pagination calculations
    const pageInfo = calculatePagination(totalFiltered, this.currentPage, this.pageSize);
    this.currentPage = pageInfo.pageIndex;
    const currentSlice = this.filteredCatalog.slice(pageInfo.startIndex, pageInfo.endIndex);

    // Build rows using safe DOM elements
    currentSlice.forEach((item) => {
      const row = this.buildTableRow(item);
      this.elements.tableBody.appendChild(row);
    });

    this.renderPagination(totalFiltered, pageInfo.pageIndex, pageInfo.pageSize);
  }

  /**
   * Constructs a single safe table row for a radical.
   * 
   * @param {object} item 
   * @returns {HTMLTableRowElement}
   */
  buildTableRow(item) {
    const id = item.radicalId !== undefined ? item.radicalId : '—';
    const character = item.character || '—';
    const pinyin = item.pinyin || '—';
    const meaningHanViet = item.meaningHanViet || '—';
    const meaningVi = item.meaningVi || '—';

    // Media badges
    const mediaNodes = [];
    if (item.audioUrl) {
      const safeAudio = sanitizeResourceUrl(item.audioUrl);
      if (safeAudio && safeAudio !== 'about:blank') {
        const audioLink = createSafeElement('a', {
          className: 'badge bg-light text-primary border text-decoration-none me-1',
          text: '🔊 Audio',
          attrs: {
            href: safeAudio,
            target: '_blank',
            rel: 'noopener noreferrer',
            title: `Nghe phát âm: ${character}`
          }
        });
        mediaNodes.push(audioLink);
      }
    }
    if (item.videoWritingUrl) {
      const safeVideo = sanitizeResourceUrl(item.videoWritingUrl);
      if (safeVideo && safeVideo !== 'about:blank') {
        const videoLink = createSafeElement('a', {
          className: 'badge bg-light text-success border text-decoration-none',
          text: '🎬 Video',
          attrs: {
            href: safeVideo,
            target: '_blank',
            rel: 'noopener noreferrer',
            title: `Xem video viết: ${character}`
          }
        });
        mediaNodes.push(videoLink);
      }
    }
    if (mediaNodes.length === 0) {
      mediaNodes.push(createSafeElement('span', { className: 'text-muted small', text: '—' }));
    }

    // Action buttons
    const btnEdit = createSafeElement('button', {
      className: 'btn btn-sm btn-chinese-secondary py-1 px-2 me-1',
      text: 'Sửa',
      attrs: {
        type: 'button',
        'aria-label': `Chỉnh sửa bộ thủ ${character}`
      }
    });
    btnEdit.addEventListener('click', () => {
      this.openEditModal(item, btnEdit);
    });

    const btnDelete = createSafeElement('button', {
      className: 'btn btn-sm btn-outline-danger py-1 px-2',
      text: 'Xóa',
      attrs: {
        type: 'button',
        'aria-label': `Xóa bộ thủ ${character}`
      }
    });
    btnDelete.addEventListener('click', () => {
      this.openDeleteModal(item, btnDelete);
    });

    return createSafeElement('tr', {
      children: [
        // 1. ID
        createSafeElement('td', { className: 'text-center text-secondary small', text: `#${id}` }),
        // 2. Character
        createSafeElement('td', {
          className: 'text-center',
          children: [
            createSafeElement('span', { className: 'font-chinese fs-4 text-chinese-red fw-bold', text: character })
          ]
        }),
        // 3. Pinyin
        createSafeElement('td', { className: 'fw-semibold text-dark', text: pinyin }),
        // 4. Meaning Han-Viet
        createSafeElement('td', { className: 'text-dark', text: meaningHanViet }),
        // 5. Meaning Vietnamese
        createSafeElement('td', { className: 'text-secondary', text: meaningVi }),
        // 6. Media
        createSafeElement('td', { className: 'text-center', children: mediaNodes }),
        // 7. Actions
        createSafeElement('td', { className: 'text-end pe-3 text-nowrap', children: [btnEdit, btnDelete] })
      ]
    });
  }

  /**
   * Renders pagination controls and textual summary.
   * 
   * @param {number} totalItems 
   * @param {number} pageIndex 
   * @param {number} pageSize 
   */
  renderPagination(totalItems, pageIndex, pageSize) {
    if (!this.elements.paginationSummary || !this.elements.paginationControls) return;

    clearContainer(this.elements.paginationSummary);
    clearContainer(this.elements.paginationControls);

    if (totalItems === 0) {
      this.elements.paginationSummary.textContent = 'Không có bản ghi nào.';
      return;
    }

    const { totalPages, startIndex, endIndex } = calculatePagination(totalItems, pageIndex, pageSize);

    // Summary text
    this.elements.paginationSummary.textContent = `Hiển thị ${startIndex + 1} – ${endIndex} trên tổng số ${totalItems} bộ thủ (Trang ${pageIndex + 1} / ${totalPages})`;

    if (totalPages <= 1) {
      return; // Single page doesn't need prev/next buttons
    }

    // Prev Button
    const prevBtn = createSafeElement('button', {
      className: `btn btn-sm ${pageIndex === 0 ? 'btn-outline-secondary disabled' : 'btn-outline-dark'}`,
      text: '‹ Trước',
      attrs: {
        type: 'button',
        'aria-label': 'Trang trước',
        ...(pageIndex === 0 ? { disabled: '' } : {})
      }
    });
    if (pageIndex > 0) {
      prevBtn.addEventListener('click', () => {
        this.currentPage = pageIndex - 1;
        this.renderCatalogView();
      });
    }
    this.elements.paginationControls.appendChild(prevBtn);

    // Page indicator
    const pageBadge = createSafeElement('span', {
      className: 'px-2 small text-secondary fw-semibold',
      text: `${pageIndex + 1} / ${totalPages}`
    });
    this.elements.paginationControls.appendChild(pageBadge);

    // Next Button
    const nextBtn = createSafeElement('button', {
      className: `btn btn-sm ${pageIndex >= totalPages - 1 ? 'btn-outline-secondary disabled' : 'btn-outline-dark'}`,
      text: 'Sau ›',
      attrs: {
        type: 'button',
        'aria-label': 'Trang sau',
        ...(pageIndex >= totalPages - 1 ? { disabled: '' } : {})
      }
    });
    if (pageIndex < totalPages - 1) {
      nextBtn.addEventListener('click', () => {
        this.currentPage = pageIndex + 1;
        this.renderCatalogView();
      });
    }
    this.elements.paginationControls.appendChild(nextBtn);
  }

  /* ---------------------------------------------------------------------------
   * 3. CREATE & EDIT MODAL HANDLERS
   * --------------------------------------------------------------------------- */

  /**
   * Opens form dialog in CREATE mode.
   * 
   * @param {HTMLElement} [triggerEl] 
   */
  openCreateModal(triggerEl = null) {
    this.activeTriggerEl = triggerEl;
    this.activeRadicalForEdit = null;

    if (this.elements.modalFormTitle) {
      this.elements.modalFormTitle.textContent = 'Thêm Bộ Thủ Mới';
    }
    if (this.elements.modalFormDesc) {
      this.elements.modalFormDesc.textContent = 'Nhập thông tin bộ thủ mới. Ký tự phải là duy nhất trong danh mục Khang Hy.';
    }
    if (this.elements.saveRadicalBtnText) {
      this.elements.saveRadicalBtnText.textContent = 'Tạo bộ thủ';
    }

    if (this.elements.radicalFormId) this.elements.radicalFormId.value = '';
    this.resetFormFields();
    this.clearValidationErrors();

    if (this.elements.formModal && typeof this.elements.formModal.showModal === 'function') {
      this.elements.formModal.showModal();
      if (this.elements.inputCharacter) {
        this.elements.inputCharacter.focus();
      }
    }
  }

  /**
   * Opens form dialog in EDIT mode prefilled with authoritative radical data.
   * 
   * @param {object} radical 
   * @param {HTMLElement} [triggerEl] 
   */
  openEditModal(radical, triggerEl = null) {
    if (!radical) return;
    this.activeTriggerEl = triggerEl;
    this.activeRadicalForEdit = radical;

    if (this.elements.modalFormTitle) {
      this.elements.modalFormTitle.textContent = `Chỉnh Sửa Bộ Thủ: ${radical.character || ''}`;
    }
    if (this.elements.modalFormDesc) {
      this.elements.modalFormDesc.textContent = `Cập nhật thông tin cho bộ thủ #${radical.radicalId}. Thay đổi ký tự sẽ kiểm tra tính duy nhất.`;
    }
    if (this.elements.saveRadicalBtnText) {
      this.elements.saveRadicalBtnText.textContent = 'Lưu cập nhật';
    }

    if (this.elements.radicalFormId) {
      this.elements.radicalFormId.value = String(radical.radicalId || '');
    }

    if (this.elements.inputCharacter) this.elements.inputCharacter.value = radical.character || '';
    if (this.elements.inputPinyin) this.elements.inputPinyin.value = radical.pinyin || '';
    if (this.elements.inputMeaningHanViet) this.elements.inputMeaningHanViet.value = radical.meaningHanViet || '';
    if (this.elements.inputMeaningVi) this.elements.inputMeaningVi.value = radical.meaningVi || '';
    if (this.elements.inputAudioUrl) this.elements.inputAudioUrl.value = radical.audioUrl || '';
    if (this.elements.inputVideoWritingUrl) this.elements.inputVideoWritingUrl.value = radical.videoWritingUrl || '';

    this.clearValidationErrors();

    if (this.elements.formModal && typeof this.elements.formModal.showModal === 'function') {
      this.elements.formModal.showModal();
      if (this.elements.inputCharacter) {
        this.elements.inputCharacter.focus();
      }
    }
  }

  /**
   * Closes form modal and restores focus.
   */
  closeFormModal() {
    if (this.elements.formModal && this.elements.formModal.open) {
      this.elements.formModal.close();
    }
    this.resetFormFields();
    this.clearValidationErrors();
    this.activeRadicalForEdit = null;

    if (this.activeTriggerEl && typeof this.activeTriggerEl.focus === 'function') {
      this.activeTriggerEl.focus();
    }
    this.activeTriggerEl = null;
  }

  /**
   * Clears form input values.
   */
  resetFormFields() {
    if (this.elements.inputCharacter) this.elements.inputCharacter.value = '';
    if (this.elements.inputPinyin) this.elements.inputPinyin.value = '';
    if (this.elements.inputMeaningHanViet) this.elements.inputMeaningHanViet.value = '';
    if (this.elements.inputMeaningVi) this.elements.inputMeaningVi.value = '';
    if (this.elements.inputAudioUrl) this.elements.inputAudioUrl.value = '';
    if (this.elements.inputVideoWritingUrl) this.elements.inputVideoWritingUrl.value = '';
  }

  /**
   * Clears all validation error messages and is-invalid classes.
   */
  clearValidationErrors() {
    if (this.elements.formGeneralError) {
      this.elements.formGeneralError.textContent = '';
      this.elements.formGeneralError.classList.add('d-none');
    }

    const fields = ['Character', 'Pinyin', 'MeaningHanViet', 'MeaningVi', 'AudioUrl', 'VideoWritingUrl'];
    fields.forEach(field => {
      const input = this.elements[`input${field}`];
      const errorDiv = this.elements[`error${field}`];
      if (input) input.classList.remove('is-invalid');
      if (errorDiv) errorDiv.textContent = '';
    });
  }

  /**
   * Submits Create or Update request to authoritative backend.
   */
  async handleSaveRadical() {
    if (this.isSubmitting) return;

    this.clearValidationErrors();

    const formData = {
      character: this.elements.inputCharacter ? this.elements.inputCharacter.value : '',
      pinyin: this.elements.inputPinyin ? this.elements.inputPinyin.value : '',
      meaningHanViet: this.elements.inputMeaningHanViet ? this.elements.inputMeaningHanViet.value : '',
      meaningVi: this.elements.inputMeaningVi ? this.elements.inputMeaningVi.value : '',
      audioUrl: this.elements.inputAudioUrl ? this.elements.inputAudioUrl.value : '',
      videoWritingUrl: this.elements.inputVideoWritingUrl ? this.elements.inputVideoWritingUrl.value : ''
    };

    // Client-side validation
    const validation = validateRadicalForm(formData);
    if (!validation.valid) {
      this.displayValidationErrors(validation.errors);
      return;
    }

    const payload = buildRadicalPayload(formData);
    const radicalIdStr = this.elements.radicalFormId ? this.elements.radicalFormId.value.trim() : '';
    const isEdit = radicalIdStr.length > 0;
    const operation = isEdit ? 'update' : 'create';

    this.setSubmitState(true);

    try {
      if (isEdit) {
        const id = parseInt(radicalIdStr, 10);
        const updated = await apiClient(`/admin/radicals/${id}`, {
          method: 'PUT',
          body: payload
        });

        // Update local full catalog with authoritative server representation
        const targetIdx = this.fullCatalog.findIndex(r => Number(r.radicalId) === id);
        if (targetIdx >= 0) {
          this.fullCatalog[targetIdx] = { ...this.fullCatalog[targetIdx], ...updated };
        }

        showToast(`Cập nhật bộ thủ "${payload.character}" thành công!`, 'success');
      } else {
        const created = await apiClient('/admin/radicals', {
          method: 'POST',
          body: payload
        });

        // Insert new radical into local catalog
        if (created) {
          this.fullCatalog.push(created);
          // Maintain sorted order
          this.fullCatalog.sort((a, b) => (Number(a.radicalId) || 0) - (Number(b.radicalId) || 0));
        }

        showToast(`Tạo mới bộ thủ "${payload.character}" thành công!`, 'success');
      }

      this.closeFormModal();
      this.filteredCatalog = filterRadicals(this.activeSearchQuery, this.fullCatalog);
      this.renderCatalogView();

    } catch (err) {
      console.error(`[AdminRadicals] Failed to ${operation} radical:`, err);
      const classified = classifyApiError(err, operation);

      if (classified.isValidation && Object.keys(classified.fieldErrors).length > 0) {
        this.displayValidationErrors(classified.fieldErrors);
      }

      if (this.elements.formGeneralError) {
        this.elements.formGeneralError.textContent = classified.userMessage;
        this.elements.formGeneralError.classList.remove('d-none');
      }
    } finally {
      this.setSubmitState(false);
    }
  }

  /**
   * Displays field-level errors on form inputs.
   * 
   * @param {Record<string, string>} errors 
   */
  displayValidationErrors(errors) {
    const fieldMap = {
      character: 'Character',
      pinyin: 'Pinyin',
      meaningHanViet: 'MeaningHanViet',
      meaningVi: 'MeaningVi',
      audioUrl: 'AudioUrl',
      videoWritingUrl: 'VideoWritingUrl'
    };

    let firstInvalidInput = null;

    Object.entries(errors).forEach(([field, msg]) => {
      if (field === '_general') {
        if (this.elements.formGeneralError) {
          this.elements.formGeneralError.textContent = msg;
          this.elements.formGeneralError.classList.remove('d-none');
        }
        return;
      }

      const mappedKey = fieldMap[field];
      if (mappedKey) {
        const input = this.elements[`input${mappedKey}`];
        const errorDiv = this.elements[`error${mappedKey}`];
        if (input) {
          input.classList.add('is-invalid');
          if (!firstInvalidInput) firstInvalidInput = input;
        }
        if (errorDiv) {
          errorDiv.textContent = msg;
        }
      }
    });

    if (firstInvalidInput) {
      firstInvalidInput.focus();
    }
  }

  /**
   * Controls form modal submission state (spinners, disabled controls).
   * 
   * @param {boolean} submitting 
   */
  setSubmitState(submitting) {
    this.isSubmitting = submitting;

    if (this.elements.saveRadicalBtnSpinner) {
      if (submitting) {
        this.elements.saveRadicalBtnSpinner.classList.remove('d-none');
      } else {
        this.elements.saveRadicalBtnSpinner.classList.add('d-none');
      }
    }

    if (this.elements.btnSaveRadical) {
      this.elements.btnSaveRadical.disabled = submitting;
    }
    if (this.elements.btnCancelFormModal) {
      this.elements.btnCancelFormModal.disabled = submitting;
    }
    if (this.elements.modalFormCloseBtn) {
      this.elements.modalFormCloseBtn.disabled = submitting;
    }
  }

  /* ---------------------------------------------------------------------------
   * 4. DELETE MODAL HANDLERS
   * --------------------------------------------------------------------------- */

  /**
   * Opens delete confirmation modal.
   * 
   * @param {object} radical 
   * @param {HTMLElement} [triggerEl] 
   */
  openDeleteModal(radical, triggerEl = null) {
    if (!radical) return;
    this.activeTriggerEl = triggerEl;
    this.activeRadicalForDelete = radical;

    if (this.elements.deleteTargetCharacter) {
      this.elements.deleteTargetCharacter.textContent = radical.character || '—';
    }
    if (this.elements.deleteTargetMeaning) {
      const pinyin = radical.pinyin ? ` (${radical.pinyin})` : '';
      const hv = radical.meaningHanViet || '';
      const vi = radical.meaningVi ? ` — ${radical.meaningVi}` : '';
      this.elements.deleteTargetMeaning.textContent = `${hv}${pinyin}${vi}`;
    }
    if (this.elements.deleteTargetId) {
      this.elements.deleteTargetId.textContent = `#${radical.radicalId}`;
    }

    if (this.elements.deleteModalError) {
      this.elements.deleteModalError.textContent = '';
      this.elements.deleteModalError.classList.add('d-none');
    }

    if (this.elements.deleteModal && typeof this.elements.deleteModal.showModal === 'function') {
      this.elements.deleteModal.showModal();
      if (this.elements.btnCancelDeleteModal) {
        this.elements.btnCancelDeleteModal.focus();
      }
    }
  }

  /**
   * Closes delete modal and restores focus.
   */
  closeDeleteModal() {
    if (this.elements.deleteModal && this.elements.deleteModal.open) {
      this.elements.deleteModal.close();
    }
    this.activeRadicalForDelete = null;

    if (this.elements.deleteModalError) {
      this.elements.deleteModalError.textContent = '';
      this.elements.deleteModalError.classList.add('d-none');
    }

    if (this.activeTriggerEl && typeof this.activeTriggerEl.focus === 'function') {
      this.activeTriggerEl.focus();
    }
    this.activeTriggerEl = null;
  }

  /**
   * Submits DELETE /api/v1/admin/radicals/{id} request to backend.
   * Handles 204 No Content without calling response.json().
   */
  async handleConfirmDeleteRadical() {
    if (this.isSubmitting || !this.activeRadicalForDelete) return;

    const target = this.activeRadicalForDelete;
    const id = target.radicalId;
    const character = target.character || `#${id}`;

    this.setDeleteSubmitState(true);

    try {
      // DELETE returns HTTP 204 No Content -> apiClient returns null without calling response.json()
      await apiClient(`/admin/radicals/${id}`, {
        method: 'DELETE'
      });

      // Remove from local full catalog
      this.fullCatalog = this.fullCatalog.filter(r => Number(r.radicalId) !== Number(id));

      showToast(`Đã xóa vĩnh viễn bộ thủ "${character}" khỏi hệ thống.`, 'success');
      this.closeDeleteModal();

      this.filteredCatalog = filterRadicals(this.activeSearchQuery, this.fullCatalog);
      this.renderCatalogView();

    } catch (err) {
      console.error('[AdminRadicals] Deletion failed:', err);
      const classified = classifyApiError(err, 'delete');

      if (this.elements.deleteModalError) {
        this.elements.deleteModalError.textContent = classified.userMessage;
        this.elements.deleteModalError.classList.remove('d-none');
      }

      if (classified.isNotFound) {
        // Stale state: radical was already deleted
        showToast('Bộ thủ không tồn tại hoặc đã được xóa trước đó. Đang làm mới danh mục...', 'warning');
        setTimeout(() => {
          this.closeDeleteModal();
          this.loadCatalog();
        }, 1500);
      }
    } finally {
      this.setDeleteSubmitState(false);
    }
  }

  /**
   * Controls delete modal submission state.
   * 
   * @param {boolean} submitting 
   */
  setDeleteSubmitState(submitting) {
    this.isSubmitting = submitting;

    if (this.elements.deleteRadicalBtnSpinner) {
      if (submitting) {
        this.elements.deleteRadicalBtnSpinner.classList.remove('d-none');
      } else {
        this.elements.deleteRadicalBtnSpinner.classList.add('d-none');
      }
    }

    if (this.elements.btnConfirmDeleteRadical) {
      this.elements.btnConfirmDeleteRadical.disabled = submitting;
    }
    if (this.elements.btnCancelDeleteModal) {
      this.elements.btnCancelDeleteModal.disabled = submitting;
    }
    if (this.elements.deleteModalCloseBtn) {
      this.elements.deleteModalCloseBtn.disabled = submitting;
    }
  }
}

/* -----------------------------------------------------------------------------
 * 3. AUTO-BOOTSTRAP IN BROWSER CONTEXT
 * ----------------------------------------------------------------------------- */

if (typeof window !== 'undefined' && typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    const pageController = new AdminRadicalsPageController();
    pageController.init();
  });
}
