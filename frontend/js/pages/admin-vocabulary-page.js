/**
 * =============================================================================
 * ADMIN VOCABULARY CRUD CONTROLLER (TASK 9F.4)
 * Module: frontend/js/pages/admin-vocabulary-page.js
 * 
 * Responsibilities:
 * - Integration with authoritative backend endpoints:
 *   * GET    /api/v1/vocabulary (Public catalog search: search, page, size)
 *   * GET    /api/v1/vocabulary/{id} (Authoritative detail with constituent radicals)
 *   * POST   /api/v1/admin/vocabulary (Create vocabulary -> 201 Created)
 *   * PUT    /api/v1/admin/vocabulary/{id} (Update vocabulary -> 200 OK)
 *   * DELETE /api/v1/admin/vocabulary/{id} (Delete vocabulary -> 204 No Content)
 *   * GET    /api/v1/radicals (Catalog lookup for constituent radical picker)
 * - Strict Invariant: There is NO 'GET /api/v1/admin/vocabulary' endpoint.
 * - Role-guarded access: strictly accessible to 'Admin' (authManager.hasRole('Admin')).
 * - Constituent Kangxi radical assignment: sends complete set of integer IDs.
 * - Validation contracts: client UX validation supplementing authoritative backend.
 * - PinyinRaw derivation & contract compliance: strips tone diacritics and normalizes ü/v -> u.
 * - Conflict handling:
 *   * Duplicate (hanzi + pinyinRaw) on Create/Update -> 409 CONFLICT.
 *   * Dependent learning data on Delete (Lesson, PersonalNote, CardProgress, ReviewLog) -> 409 CONFLICT.
 * - HTTP 204 No Content handled cleanly without calling response.json().
 * - Safe DOM construction (zero unsafe innerHTML) and sanitized media URLs (sanitizeResourceUrl).
 * - Focus management, Escape key, and native HTML5 <dialog> modals.
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
 * Converts pinyin with tones (e.g. xué, nǐ, xiū, lǚ, nǚ) to raw toneless pinyin (xue, ni, xiu, lu, nu).
 * Matches backend ExcelParserServiceImpl.toPinyinRaw normalization.
 * 
 * @param {string} pinyin 
 * @returns {string}
 */
export function toPinyinRaw(pinyin) {
  if (!pinyin || typeof pinyin !== 'string') return '';
  return pinyin
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[üÜv]/g, 'u')
    .toLowerCase()
    .trim();
}

/**
 * Normalizes user search input string by trimming whitespace.
 * 
 * @param {*} query 
 * @returns {string}
 */
export function normalizeSearchQuery(query) {
  if (query === null || query === undefined) return '';
  return String(query).trim();
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
 * Validates vocabulary form data against backend DTO constraints:
 * - hanzi: required, 1..50 chars
 * - pinyin: required, 1..100 chars
 * - pinyinRaw: required, 1..100 chars
 * - meaningHanViet: required, 1..100 chars
 * - meaningVi: required, 1..255 chars
 * - audioUrl: optional, max 500 chars, valid URI format if provided
 * - videoWritingUrl: optional, max 500 chars, valid URI format if provided
 * - exampleSentence: optional, max 500 chars
 * - exampleTranslation: optional, max 500 chars
 * - radicalIds: optional, set of positive integers (empty set is valid!)
 * 
 * @param {object} data 
 * @returns {{ valid: boolean, errors: Record<string, string> }}
 */
export function validateVocabularyForm(data) {
  const errors = {};
  if (!data || typeof data !== 'object') {
    return { valid: false, errors: { _general: 'Dữ liệu từ vựng không hợp lệ.' } };
  }

  const hanzi = typeof data.hanzi === 'string' ? data.hanzi.trim() : '';
  const pinyin = typeof data.pinyin === 'string' ? data.pinyin.trim() : '';
  const pinyinRaw = typeof data.pinyinRaw === 'string' ? data.pinyinRaw.trim() : '';
  const meaningHanViet = typeof data.meaningHanViet === 'string' ? data.meaningHanViet.trim() : '';
  const meaningVi = typeof data.meaningVi === 'string' ? data.meaningVi.trim() : '';
  const audioUrl = typeof data.audioUrl === 'string' ? data.audioUrl.trim() : '';
  const videoWritingUrl = typeof data.videoWritingUrl === 'string' ? data.videoWritingUrl.trim() : '';
  const exampleSentence = typeof data.exampleSentence === 'string' ? data.exampleSentence.trim() : '';
  const exampleTranslation = typeof data.exampleTranslation === 'string' ? data.exampleTranslation.trim() : '';

  // 1. Hanzi validation
  if (!hanzi) {
    errors.hanzi = 'Chữ Hán không được để trống';
  } else if (hanzi.length > 50) {
    errors.hanzi = 'Chữ Hán không được vượt quá 50 ký tự';
  }

  // 2. Pinyin validation
  if (!pinyin) {
    errors.pinyin = 'Pinyin không được để trống';
  } else if (pinyin.length > 100) {
    errors.pinyin = 'Pinyin không được vượt quá 100 ký tự';
  }

  // 3. Pinyin Raw validation
  if (!pinyinRaw) {
    errors.pinyinRaw = 'Pinyin không dấu không được để trống';
  } else if (pinyinRaw.length > 100) {
    errors.pinyinRaw = 'Pinyin không dấu không được vượt quá 100 ký tự';
  }

  // 4. Meaning Han-Viet validation
  if (!meaningHanViet) {
    errors.meaningHanViet = 'Nghĩa Hán-Việt không được để trống';
  } else if (meaningHanViet.length > 100) {
    errors.meaningHanViet = 'Nghĩa Hán-Việt không được vượt quá 100 ký tự';
  }

  // 5. Meaning Vietnamese validation
  if (!meaningVi) {
    errors.meaningVi = 'Nghĩa tiếng Việt không được để trống';
  } else if (meaningVi.length > 255) {
    errors.meaningVi = 'Nghĩa tiếng Việt không được vượt quá 255 ký tự';
  }

  // 6. Audio URL validation (Optional)
  if (audioUrl) {
    if (audioUrl.length > 500) {
      errors.audioUrl = 'URL âm thanh không được vượt quá 500 ký tự';
    } else if (!isValidMediaUrl(audioUrl)) {
      errors.audioUrl = 'URL âm thanh phải là URL hợp lệ (bắt đầu bằng http://, https:// hoặc /)';
    }
  }

  // 7. Video Writing URL validation (Optional)
  if (videoWritingUrl) {
    if (videoWritingUrl.length > 500) {
      errors.videoWritingUrl = 'URL video viết chữ không được vượt quá 500 ký tự';
    } else if (!isValidMediaUrl(videoWritingUrl)) {
      errors.videoWritingUrl = 'URL video viết chữ phải là URL hợp lệ (bắt đầu bằng http://, https:// hoặc /)';
    }
  }

  // 8. Example Sentence validation (Optional)
  if (exampleSentence && exampleSentence.length > 500) {
    errors.exampleSentence = 'Câu ví dụ không được vượt quá 500 ký tự';
  }

  // 9. Example Translation validation (Optional)
  if (exampleTranslation && exampleTranslation.length > 500) {
    errors.exampleTranslation = 'Dịch nghĩa câu ví dụ không được vượt quá 500 ký tự';
  }

  // 10. Radical IDs validation (Optional, all must be positive integers)
  if (Array.isArray(data.radicalIds)) {
    for (const rId of data.radicalIds) {
      const num = Number(rId);
      if (!Number.isInteger(num) || num <= 0) {
        errors.radicalIds = 'ID bộ thủ phải là số nguyên dương';
        break;
      }
    }
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors
  };
}

/**
 * Formats clean request payload for CreateVocabularyRequest or UpdateVocabularyRequest.
 * Normalizes empty/whitespace optional fields to null, and ensures radicalIds is an integer array.
 * 
 * @param {object} formData 
 * @returns {object}
 */
export function buildVocabularyPayload(formData) {
  const hanzi = typeof formData.hanzi === 'string' ? formData.hanzi.trim() : '';
  const pinyin = typeof formData.pinyin === 'string' ? formData.pinyin.trim() : '';
  const pinyinRaw = typeof formData.pinyinRaw === 'string' ? formData.pinyinRaw.trim() : '';
  const meaningHanViet = typeof formData.meaningHanViet === 'string' ? formData.meaningHanViet.trim() : '';
  const meaningVi = typeof formData.meaningVi === 'string' ? formData.meaningVi.trim() : '';
  const audioUrl = typeof formData.audioUrl === 'string' ? formData.audioUrl.trim() : '';
  const videoWritingUrl = typeof formData.videoWritingUrl === 'string' ? formData.videoWritingUrl.trim() : '';
  const exampleSentence = typeof formData.exampleSentence === 'string' ? formData.exampleSentence.trim() : '';
  const exampleTranslation = typeof formData.exampleTranslation === 'string' ? formData.exampleTranslation.trim() : '';

  let radicalIds = [];
  if (Array.isArray(formData.radicalIds)) {
    radicalIds = Array.from(new Set(
      formData.radicalIds
        .map(Number)
        .filter(n => Number.isInteger(n) && n > 0)
    )).sort((a, b) => a - b);
  } else if (formData.radicalIds instanceof Set) {
    radicalIds = Array.from(formData.radicalIds)
      .map(Number)
      .filter(n => Number.isInteger(n) && n > 0)
      .sort((a, b) => a - b);
  }

  return {
    hanzi,
    pinyin,
    pinyinRaw,
    meaningHanViet,
    meaningVi,
    audioUrl: audioUrl.length > 0 ? audioUrl : null,
    videoWritingUrl: videoWritingUrl.length > 0 ? videoWritingUrl : null,
    exampleSentence: exampleSentence.length > 0 ? exampleSentence : null,
    exampleTranslation: exampleTranslation.length > 0 ? exampleTranslation : null,
    radicalIds
  };
}

/**
 * Checks whether an update payload represents a true no-op compared to the existing detail record.
 * 
 * @param {object} currentDetail 
 * @param {object} newPayload 
 * @returns {boolean}
 */
export function isNoOpUpdate(currentDetail, newPayload) {
  if (!currentDetail || !newPayload) return false;

  const eq = (a, b) => (a || null) === (b || null);

  if (!eq(currentDetail.hanzi, newPayload.hanzi)) return false;
  if (!eq(currentDetail.pinyin, newPayload.pinyin)) return false;
  if (!eq(currentDetail.pinyinRaw, newPayload.pinyinRaw)) return false;
  if (!eq(currentDetail.meaningHanViet, newPayload.meaningHanViet)) return false;
  if (!eq(currentDetail.meaningVi, newPayload.meaningVi)) return false;
  if (!eq(currentDetail.audioUrl, newPayload.audioUrl)) return false;
  if (!eq(currentDetail.videoWritingUrl, newPayload.videoWritingUrl)) return false;
  if (!eq(currentDetail.exampleSentence, newPayload.exampleSentence)) return false;
  if (!eq(currentDetail.exampleTranslation, newPayload.exampleTranslation)) return false;

  const currentRadicalIds = Array.isArray(currentDetail.radicals)
    ? currentDetail.radicals.map(r => Number(r.radicalId)).filter(n => n > 0).sort((a, b) => a - b)
    : [];
  const newRadicalIds = Array.isArray(newPayload.radicalIds)
    ? [...newPayload.radicalIds].sort((a, b) => a - b)
    : [];

  if (currentRadicalIds.length !== newRadicalIds.length) return false;
  for (let i = 0; i < currentRadicalIds.length; i++) {
    if (currentRadicalIds[i] !== newRadicalIds[i]) return false;
  }

  return true;
}

/**
 * Classifies an API error for human-readable feedback.
 * Distinguishes 409 Conflict (duplicate vocab vs referenced delete), 404 Not Found, 400 Validation, and Network errors.
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
      userMessage = rawMessage || 'Không thể xóa từ vựng đang được liên kết với bài học hoặc dữ liệu học tập (ghi chú, SRS, lịch sử ôn tập) trong hệ thống.';
    } else {
      userMessage = rawMessage || 'Từ vựng với chữ Hán và pinyin không dấu này đã tồn tại trong hệ thống. Vui lòng kiểm tra lại.';
    }
  } else if (isNotFound) {
    if (operation === 'delete') {
      userMessage = rawMessage || 'Không tìm thấy từ vựng hoặc dữ liệu đã bị xóa bởi quản trị viên khác.';
    } else {
      userMessage = rawMessage || 'Không tìm thấy từ vựng hoặc bộ thủ được yêu cầu trong hệ thống.';
    }
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
 * Filters radicals list client-side for the radical picker selector.
 * 
 * @param {string} rawQuery 
 * @param {Array<Object>} items 
 * @returns {Array<Object>}
 */
export function filterRadicalsList(rawQuery, items) {
  if (!Array.isArray(items)) return [];
  const query = (rawQuery || '').trim().toLowerCase();
  if (!query) return items;

  const queryRaw = toPinyinRaw(query);

  return items.filter(item => {
    if (!item) return false;
    if (item.character && String(item.character).includes(query)) return true;

    if (item.pinyin) {
      const py = String(item.pinyin).toLowerCase();
      if (py.includes(query) || toPinyinRaw(py).includes(queryRaw)) return true;
    }

    if (item.meaningHanViet) {
      const hv = String(item.meaningHanViet).toLowerCase();
      if (hv.includes(query) || toPinyinRaw(hv).includes(queryRaw)) return true;
    }

    if (item.meaningVi) {
      const vi = String(item.meaningVi).toLowerCase();
      if (vi.includes(query) || toPinyinRaw(vi).includes(queryRaw)) return true;
    }

    if (String(item.radicalId) === query) return true;
    return false;
  });
}

/**
 * Validates and safely parses backend PageResponse payload.
 * 
 * @param {Object} pageResponse 
 * @returns {{ page: number, size: number, totalElements: number, totalPages: number, items: Array<Object> }}
 */
export function parsePageResponse(pageResponse) {
  if (!pageResponse || typeof pageResponse !== 'object') {
    return { page: 0, size: 20, totalElements: 0, totalPages: 0, items: [] };
  }

  const items = Array.isArray(pageResponse.items) ? pageResponse.items : [];
  const page = Math.max(0, Number(pageResponse.page) || 0);
  const size = Math.max(1, Number(pageResponse.size) || 20);
  const totalElements = Math.max(0, Number(pageResponse.totalElements) || items.length);
  const totalPages = Math.max(0, Number(pageResponse.totalPages) || (totalElements > 0 ? Math.ceil(totalElements / size) : 0));

  return { page, size, totalElements, totalPages, items };
}

/**
 * Calculates accessible pagination boundaries and windowing.
 * 
 * @param {number} totalElements 
 * @param {number} pageIndex - 0-indexed
 * @param {number} pageSize 
 * @param {number} [maxButtons=5] 
 * @returns {{ pageIndex: number, pageSize: number, totalPages: number, startIndex: number, endIndex: number, startPage: number, endPage: number }}
 */
export function calculatePagination(totalElements, pageIndex, pageSize, maxButtons = 5) {
  const safeTotal = Math.max(0, Number(totalElements) || 0);
  const safeSize = Math.max(1, Number(pageSize) || 20);
  const totalPages = safeTotal > 0 ? Math.ceil(safeTotal / safeSize) : 0;
  const clampedPage = totalPages > 0 ? Math.max(0, Math.min(Number(pageIndex) || 0, totalPages - 1)) : 0;

  const startIndex = safeTotal > 0 ? clampedPage * safeSize : 0;
  const endIndex = Math.min(safeTotal, startIndex + safeSize);

  let startPage = Math.max(0, clampedPage - Math.floor(maxButtons / 2));
  let endPage = Math.min(Math.max(0, totalPages - 1), startPage + maxButtons - 1);
  if (endPage - startPage < maxButtons - 1 && totalPages > 0) {
    startPage = Math.max(0, endPage - maxButtons + 1);
  }

  return {
    pageIndex: clampedPage,
    pageSize: safeSize,
    totalPages,
    startIndex,
    endIndex,
    startPage,
    endPage
  };
}

/* -----------------------------------------------------------------------------
 * 2. PAGE CONTROLLER IMPLEMENTATION
 * ----------------------------------------------------------------------------- */

export class AdminVocabularyPageController {
  constructor() {
    // DOM Element Cache
    this.elements = {};

    // State Variables
    this.currentPage = 0;
    this.pageSize = 20;
    this.activeSearchQuery = '';
    this.totalElements = 0;
    this.totalPages = 0;
    this.currentItems = [];

    // Radicals Cache (Loaded on-demand on first modal open)
    this.cachedRadicals = null;
    this.selectedRadicalIds = new Set();
    this.pinyinRawTouched = false;

    // Active records
    this.currentEditDetail = null;
    this.currentDeleteTarget = null;
    this.lastFocusedElement = null;

    // Search input debounce timer
    this.searchDebounceTimer = null;
  }

  /**
   * Initializes the controller lifecycle.
   */
  async init() {
    this.cacheDomElements();
    initNavbarAuth();

    // Verify Administrative Role Guard
    if (!hasAdminAccess(authManager)) {
      this.renderAccessDenied();
      return;
    }

    this.bindEvents();
    await this.loadCatalog();
  }

  /**
   * Caches all required DOM elements.
   */
  cacheDomElements() {
    this.elements = {
      // Guard & Content Sections
      authGuardContainer: document.getElementById('adminAuthGuardContainer'),
      authGuardMessage: document.getElementById('adminAuthGuardMessage'),
      mainContent: document.getElementById('adminVocabMainContent'),

      // Search & Pagination Controls
      searchInput: document.getElementById('vocabSearchInput'),
      searchClearBtn: document.getElementById('searchClearBtn'),
      pageSizeSelect: document.getElementById('pageSizeSelect'),
      resultCountBadge: document.getElementById('vocabResultCount'),

      // Table & State Containers
      stateContainer: document.getElementById('vocabStateContainer'),
      tableCard: document.getElementById('vocabTableCard'),
      table: document.getElementById('vocabTable'),
      tableBody: document.getElementById('vocabTableBody'),
      pagination: document.getElementById('vocabPagination'),
      paginationSummary: document.getElementById('paginationSummary'),
      paginationControls: document.getElementById('paginationControls'),

      // Create / Edit Modal Elements
      btnOpenCreateModal: document.getElementById('btnOpenCreateModal'),
      formModal: document.getElementById('vocabFormModal'),
      modalFormTitle: document.getElementById('modalFormTitle'),
      modalFormDesc: document.getElementById('modalFormDesc'),
      modalFormCloseBtn: document.getElementById('modalFormCloseBtn'),
      vocabForm: document.getElementById('vocabForm'),
      vocabFormId: document.getElementById('vocabFormId'),
      formGeneralError: document.getElementById('formGeneralError'),

      // Form Inputs
      inputHanzi: document.getElementById('inputHanzi'),
      inputPinyin: document.getElementById('inputPinyin'),
      inputPinyinRaw: document.getElementById('inputPinyinRaw'),
      btnDerivePinyinRaw: document.getElementById('btnDerivePinyinRaw'),
      inputMeaningHanViet: document.getElementById('inputMeaningHanViet'),
      inputMeaningVi: document.getElementById('inputMeaningVi'),
      inputAudioUrl: document.getElementById('inputAudioUrl'),
      inputVideoWritingUrl: document.getElementById('inputVideoWritingUrl'),
      inputExampleSentence: document.getElementById('inputExampleSentence'),
      inputExampleTranslation: document.getElementById('inputExampleTranslation'),

      // Form Error Containers
      errorHanzi: document.getElementById('errorHanzi'),
      errorPinyin: document.getElementById('errorPinyin'),
      errorPinyinRaw: document.getElementById('errorPinyinRaw'),
      errorMeaningHanViet: document.getElementById('errorMeaningHanViet'),
      errorMeaningVi: document.getElementById('errorMeaningVi'),
      errorAudioUrl: document.getElementById('errorAudioUrl'),
      errorVideoWritingUrl: document.getElementById('errorVideoWritingUrl'),
      errorExampleSentence: document.getElementById('errorExampleSentence'),
      errorExampleTranslation: document.getElementById('errorExampleTranslation'),

      // Radical Picker Elements
      selectedRadicalsCount: document.getElementById('selectedRadicalsCount'),
      btnClearRadicals: document.getElementById('btnClearRadicals'),
      selectedRadicalsContainer: document.getElementById('selectedRadicalsContainer'),
      emptyRadicalsPrompt: document.getElementById('emptyRadicalsPrompt'),
      radicalFilterInput: document.getElementById('radicalFilterInput'),
      availableRadicalsList: document.getElementById('availableRadicalsList'),
      radicalsLoadingPrompt: document.getElementById('radicalsLoadingPrompt'),

      // Modal Actions
      btnCancelFormModal: document.getElementById('btnCancelFormModal'),
      btnSaveVocab: document.getElementById('btnSaveVocab'),
      saveVocabBtnSpinner: document.getElementById('saveVocabBtnSpinner'),
      saveVocabBtnText: document.getElementById('saveVocabBtnText'),

      // Delete Modal Elements
      deleteModal: document.getElementById('deleteVocabModal'),
      deleteModalCloseBtn: document.getElementById('deleteModalCloseBtn'),
      deleteTargetHanzi: document.getElementById('deleteTargetHanzi'),
      deleteTargetPinyin: document.getElementById('deleteTargetPinyin'),
      deleteTargetMeaning: document.getElementById('deleteTargetMeaning'),
      deleteTargetId: document.getElementById('deleteTargetId'),
      deleteModalError: document.getElementById('deleteModalError'),
      btnCancelDeleteModal: document.getElementById('btnCancelDeleteModal'),
      btnConfirmDeleteVocab: document.getElementById('btnConfirmDeleteVocab'),
      deleteVocabBtnSpinner: document.getElementById('deleteVocabBtnSpinner'),
      deleteVocabBtnText: document.getElementById('deleteVocabBtnText'),

      // Toast Notification Container
      toastContainer: document.getElementById('toastContainer')
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
    // 1. Search input events with debouncing
    if (this.elements.searchInput) {
      this.elements.searchInput.addEventListener('input', (e) => {
        const val = e.target.value;
        if (this.elements.searchClearBtn) {
          this.elements.searchClearBtn.classList.toggle('d-none', !val);
        }
        clearTimeout(this.searchDebounceTimer);
        this.searchDebounceTimer = setTimeout(() => {
          this.activeSearchQuery = normalizeSearchQuery(val);
          this.currentPage = 0;
          this.loadCatalog();
        }, 350);
      });

      this.elements.searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          clearTimeout(this.searchDebounceTimer);
          this.activeSearchQuery = normalizeSearchQuery(this.elements.searchInput.value);
          this.currentPage = 0;
          this.loadCatalog();
        }
      });
    }

    // 2. Search clear button
    if (this.elements.searchClearBtn) {
      this.elements.searchClearBtn.addEventListener('click', () => {
        if (this.elements.searchInput) {
          this.elements.searchInput.value = '';
          this.elements.searchInput.focus();
        }
        this.elements.searchClearBtn.classList.add('d-none');
        clearTimeout(this.searchDebounceTimer);
        this.activeSearchQuery = '';
        this.currentPage = 0;
        this.loadCatalog();
      });
    }

    // 3. Page size select
    if (this.elements.pageSizeSelect) {
      this.elements.pageSizeSelect.addEventListener('change', (e) => {
        this.pageSize = Math.max(1, Number(e.target.value) || 20);
        this.currentPage = 0;
        this.loadCatalog();
      });
    }

    // 4. Open create modal
    if (this.elements.btnOpenCreateModal) {
      this.elements.btnOpenCreateModal.addEventListener('click', () => {
        this.openCreateModal();
      });
    }

    // 5. Form input automatic pinyinRaw derivation
    if (this.elements.inputPinyin) {
      this.elements.inputPinyin.addEventListener('input', (e) => {
        if (!this.pinyinRawTouched && this.elements.inputPinyinRaw) {
          this.elements.inputPinyinRaw.value = toPinyinRaw(e.target.value);
        }
      });
    }

    if (this.elements.inputPinyinRaw) {
      this.elements.inputPinyinRaw.addEventListener('input', () => {
        this.pinyinRawTouched = true;
      });
    }

    if (this.elements.btnDerivePinyinRaw) {
      this.elements.btnDerivePinyinRaw.addEventListener('click', () => {
        if (this.elements.inputPinyin && this.elements.inputPinyinRaw) {
          this.elements.inputPinyinRaw.value = toPinyinRaw(this.elements.inputPinyin.value);
          this.pinyinRawTouched = true;
          this.elements.inputPinyinRaw.focus();
        }
      });
    }

    // 6. Radical picker controls
    if (this.elements.btnClearRadicals) {
      this.elements.btnClearRadicals.addEventListener('click', () => {
        this.selectedRadicalIds.clear();
        this.renderSelectedRadicalsSummary();
        this.updateAvailableRadicalsCheckboxes();
      });
    }

    if (this.elements.radicalFilterInput) {
      this.elements.radicalFilterInput.addEventListener('input', (e) => {
        this.renderAvailableRadicals(e.target.value);
      });
    }

    // 7. Form modal close / cancel
    if (this.elements.modalFormCloseBtn) {
      this.elements.modalFormCloseBtn.addEventListener('click', () => this.closeFormModal());
    }
    if (this.elements.btnCancelFormModal) {
      this.elements.btnCancelFormModal.addEventListener('click', () => this.closeFormModal());
    }

    // 8. Form modal save
    if (this.elements.btnSaveVocab) {
      this.elements.btnSaveVocab.addEventListener('click', () => this.handleSaveVocab());
    }
    if (this.elements.vocabForm) {
      this.elements.vocabForm.addEventListener('submit', (e) => {
        e.preventDefault();
        this.handleSaveVocab();
      });
    }

    // 9. Delete modal close / cancel
    if (this.elements.deleteModalCloseBtn) {
      this.elements.deleteModalCloseBtn.addEventListener('click', () => this.closeDeleteModal());
    }
    if (this.elements.btnCancelDeleteModal) {
      this.elements.btnCancelDeleteModal.addEventListener('click', () => this.closeDeleteModal());
    }

    // 10. Delete modal confirm
    if (this.elements.btnConfirmDeleteVocab) {
      this.elements.btnConfirmDeleteVocab.addEventListener('click', () => this.handleConfirmDelete());
    }

    // 11. Modal keyboard escape handling
    if (this.elements.formModal) {
      this.elements.formModal.addEventListener('cancel', (e) => {
        e.preventDefault();
        this.closeFormModal();
      });
    }
    if (this.elements.deleteModal) {
      this.elements.deleteModal.addEventListener('cancel', (e) => {
        e.preventDefault();
        this.closeDeleteModal();
      });
    }
  }

  /**
   * Fetches vocabulary catalog from GET /api/v1/vocabulary?search=...&page=...&size=...
   */
  async loadCatalog() {
    if (!this.elements.stateContainer) return;

    setComponentState(this.elements.stateContainer, 'LOADING', {
      message: 'Đang tải danh mục từ vựng từ máy chủ...'
    });

    if (this.elements.tableCard) {
      this.elements.tableCard.classList.add('d-none');
    }

    try {
      const params = {
        page: this.currentPage,
        size: this.pageSize
      };
      if (this.activeSearchQuery) {
        params.search = this.activeSearchQuery;
      }

      const response = await apiClient('/vocabulary', { params });
      const parsed = parsePageResponse(response);

      this.currentPage = parsed.page;
      this.pageSize = parsed.size;
      this.totalElements = parsed.totalElements;
      this.totalPages = parsed.totalPages;
      this.currentItems = parsed.items;

      this.updateResultBadge();

      if (this.totalElements === 0) {
        if (this.activeSearchQuery) {
          setComponentState(this.elements.stateContainer, 'EMPTY', {
            title: 'Không tìm thấy từ vựng',
            message: `Không có từ vựng nào khớp với từ khóa "${this.activeSearchQuery}". Vui lòng thử lại với từ khóa khác.`
          });
        } else {
          setComponentState(this.elements.stateContainer, 'EMPTY', {
            title: 'Danh mục từ vựng trống',
            message: 'Chưa có từ vựng nào trong hệ thống. Nhấn nút "Thêm từ vựng mới" để bắt đầu tạo.'
          });
        }
      } else {
        setComponentState(this.elements.stateContainer, 'READY');
        if (this.elements.tableCard) {
          this.elements.tableCard.classList.remove('d-none');
        }
        this.renderTableRows();
        this.renderPagination();
      }
    } catch (err) {
      const classified = classifyApiError(err, 'load');
      setComponentState(this.elements.stateContainer, 'ERROR', {
        title: 'Lỗi tải danh mục từ vựng',
        message: classified.userMessage,
        onRetry: () => this.loadCatalog()
      });
    }
  }

  /**
   * Updates result count status badge.
   */
  updateResultBadge() {
    if (!this.elements.resultCountBadge) return;
    if (this.totalElements === 0) {
      this.elements.resultCountBadge.textContent = '0 từ vựng';
      this.elements.resultCountBadge.className = 'badge-status badge-status-draft text-nowrap';
    } else {
      this.elements.resultCountBadge.textContent = `${this.totalElements.toLocaleString('vi-VN')} từ vựng`;
      this.elements.resultCountBadge.className = 'badge-status badge-status-published text-nowrap';
    }
  }

  /**
   * Renders semantic table rows for vocabulary items using safe DOM elements.
   */
  renderTableRows() {
    if (!this.elements.tableBody) return;
    clearContainer(this.elements.tableBody);

    for (const item of this.currentItems) {
      const tr = createSafeElement('tr', {
        attrs: { id: `vocabRow-${item.vocabId}` }
      });

      // 1. ID Column
      const tdId = createSafeElement('td', { className: 'text-center text-secondary small' });
      tdId.appendChild(createSafeElement('code', { text: `#${item.vocabId}` }));
      tr.appendChild(tdId);

      // 2. Chữ Hán Column
      const tdHanzi = createSafeElement('td', { className: 'text-center' });
      const spanHanzi = createSafeElement('span', {
        className: 'font-chinese fs-4 fw-bold text-chinese-red',
        text: item.hanzi || '—'
      });
      tdHanzi.appendChild(spanHanzi);
      tr.appendChild(tdHanzi);

      // 3. Pinyin Column
      const tdPinyin = createSafeElement('td', {
        className: 'fw-semibold text-dark',
        text: item.pinyin || '—'
      });
      tr.appendChild(tdPinyin);

      // 4. Pinyin không dấu Column
      const tdPinyinRaw = createSafeElement('td', {
        className: 'text-secondary font-monospace small',
        text: item.pinyinRaw || '—'
      });
      tr.appendChild(tdPinyinRaw);

      // 5. Nghĩa Hán-Việt Column
      const tdHanViet = createSafeElement('td', {
        className: 'text-dark',
        text: item.meaningHanViet || '—'
      });
      tr.appendChild(tdHanViet);

      // 6. Nghĩa tiếng Việt Column
      const tdVi = createSafeElement('td', {
        className: 'text-dark',
        text: item.meaningVi || '—'
      });
      tr.appendChild(tdVi);

      // 7. Media indicators
      const tdMedia = createSafeElement('td', { className: 'text-center' });
      const mediaGroup = createSafeElement('div', { className: 'd-flex justify-content-center gap-2' });

      if (item.audioUrl && isValidMediaUrl(item.audioUrl)) {
        const audioBadge = createSafeElement('span', {
          className: 'badge bg-light text-primary border',
          attrs: { title: 'Có âm thanh phát âm' },
          text: '🔊 Audio'
        });
        mediaGroup.appendChild(audioBadge);
      }
      if (item.videoWritingUrl && isValidMediaUrl(item.videoWritingUrl)) {
        const videoBadge = createSafeElement('span', {
          className: 'badge bg-light text-success border',
          attrs: { title: 'Có video tập viết' },
          text: '🎬 Video'
        });
        mediaGroup.appendChild(videoBadge);
      }
      if (!mediaGroup.hasChildNodes()) {
        const emptyBadge = createSafeElement('span', {
          className: 'text-muted small',
          text: '—'
        });
        mediaGroup.appendChild(emptyBadge);
      }
      tdMedia.appendChild(mediaGroup);
      tr.appendChild(tdMedia);

      // 8. Actions Column
      const tdActions = createSafeElement('td', { className: 'text-end pe-3' });
      const actionGroup = createSafeElement('div', { className: 'btn-group btn-group-sm' });

      // Edit Button
      const btnEdit = createSafeElement('button', {
        className: 'btn btn-outline-secondary',
        text: 'Sửa',
        attrs: {
          type: 'button',
          id: `btnEditVocab-${item.vocabId}`,
          'aria-label': `Chỉnh sửa từ vựng ${item.hanzi}`
        }
      });
      btnEdit.addEventListener('click', (e) => {
        this.lastFocusedElement = e.currentTarget;
        this.openEditModal(item.vocabId);
      });
      actionGroup.appendChild(btnEdit);

      // Delete Button
      const btnDelete = createSafeElement('button', {
        className: 'btn btn-outline-danger',
        text: 'Xóa',
        attrs: {
          type: 'button',
          id: `btnDeleteVocab-${item.vocabId}`,
          'aria-label': `Xóa từ vựng ${item.hanzi}`
        }
      });
      btnDelete.addEventListener('click', (e) => {
        this.lastFocusedElement = e.currentTarget;
        this.openDeleteModal(item);
      });
      actionGroup.appendChild(btnDelete);

      tdActions.appendChild(actionGroup);
      tr.appendChild(tdActions);

      this.elements.tableBody.appendChild(tr);
    }
  }

  /**
   * Renders server-side pagination controls.
   */
  renderPagination() {
    if (!this.elements.paginationSummary || !this.elements.paginationControls) return;

    const calc = calculatePagination(this.totalElements, this.currentPage, this.pageSize);

    // Summary Text
    const startDisplay = this.totalElements === 0 ? 0 : calc.startIndex + 1;
    const endDisplay = calc.endIndex;
    this.elements.paginationSummary.textContent =
      `Hiển thị ${startDisplay.toLocaleString('vi-VN')} – ${endDisplay.toLocaleString('vi-VN')} / ${this.totalElements.toLocaleString('vi-VN')} từ vựng (Trang ${calc.pageIndex + 1}/${calc.totalPages})`;

    // Controls
    clearContainer(this.elements.paginationControls);

    // First Page
    const btnFirst = createSafeElement('button', {
      className: 'btn btn-sm btn-outline-secondary',
      text: '«',
      attrs: {
        type: 'button',
        'aria-label': 'Về trang đầu tiên'
      }
    });
    btnFirst.disabled = calc.pageIndex === 0;
    btnFirst.addEventListener('click', () => {
      this.currentPage = 0;
      this.loadCatalog();
    });
    this.elements.paginationControls.appendChild(btnFirst);

    // Previous Page
    const btnPrev = createSafeElement('button', {
      className: 'btn btn-sm btn-outline-secondary',
      text: '‹',
      attrs: {
        type: 'button',
        'aria-label': 'Trang trước'
      }
    });
    btnPrev.disabled = calc.pageIndex === 0;
    btnPrev.addEventListener('click', () => {
      this.currentPage = Math.max(0, this.currentPage - 1);
      this.loadCatalog();
    });
    this.elements.paginationControls.appendChild(btnPrev);

    // Page Number Buttons
    for (let p = calc.startPage; p <= calc.endPage; p++) {
      const isCurrent = p === calc.pageIndex;
      const btnPage = createSafeElement('button', {
        className: isCurrent ? 'btn btn-sm btn-chinese-primary' : 'btn btn-sm btn-outline-secondary',
        text: String(p + 1),
        attrs: {
          type: 'button',
          'aria-label': `Trang ${p + 1}`,
          ...(isCurrent ? { 'aria-current': 'page' } : {})
        }
      });
      if (!isCurrent) {
        btnPage.addEventListener('click', () => {
          this.currentPage = p;
          this.loadCatalog();
        });
      }
      this.elements.paginationControls.appendChild(btnPage);
    }

    // Next Page
    const btnNext = createSafeElement('button', {
      className: 'btn btn-sm btn-outline-secondary',
      text: '›',
      attrs: {
        type: 'button',
        'aria-label': 'Trang kế tiếp'
      }
    });
    btnNext.disabled = calc.pageIndex >= calc.totalPages - 1;
    btnNext.addEventListener('click', () => {
      this.currentPage = Math.min(calc.totalPages - 1, this.currentPage + 1);
      this.loadCatalog();
    });
    this.elements.paginationControls.appendChild(btnNext);

    // Last Page
    const btnLast = createSafeElement('button', {
      className: 'btn btn-sm btn-outline-secondary',
      text: '»',
      attrs: {
        type: 'button',
        'aria-label': 'Đến trang cuối cùng'
      }
    });
    btnLast.disabled = calc.pageIndex >= calc.totalPages - 1;
    btnLast.addEventListener('click', () => {
      this.currentPage = Math.max(0, calc.totalPages - 1);
      this.loadCatalog();
    });
    this.elements.paginationControls.appendChild(btnLast);
  }

  /**
   * Ensures the radical catalog is loaded and cached from GET /api/v1/radicals.
   */
  async ensureRadicalsLoaded() {
    if (this.cachedRadicals !== null) {
      return this.cachedRadicals;
    }

    if (this.elements.radicalsLoadingPrompt) {
      this.elements.radicalsLoadingPrompt.classList.remove('d-none');
    }

    try {
      // Fetch radicals with max page size
      const firstResp = await apiClient('/radicals', { params: { page: 0, size: 250 } });
      const items = Array.isArray(firstResp?.items) ? [...firstResp.items] : [];
      const totalElements = Number(firstResp?.totalElements) || items.length;
      const totalPages = Number(firstResp?.totalPages) || 1;

      if (totalPages > 1 && items.length < totalElements) {
        for (let p = 1; p < totalPages; p++) {
          const nextResp = await apiClient('/radicals', { params: { page: p, size: firstResp.size || 250 } });
          if (Array.isArray(nextResp?.items)) {
            items.push(...nextResp.items);
          }
        }
      }

      items.sort((a, b) => (Number(a.radicalId) || 0) - (Number(b.radicalId) || 0));
      this.cachedRadicals = items;
    } catch (err) {
      this.cachedRadicals = [];
      showToast('Không thể tải danh mục bộ thủ cho bộ chọn.', 'error');
    } finally {
      if (this.elements.radicalsLoadingPrompt) {
        this.elements.radicalsLoadingPrompt.classList.add('d-none');
      }
    }

    return this.cachedRadicals;
  }

  /**
   * Opens create modal.
   */
  async openCreateModal() {
    this.lastFocusedElement = document.activeElement;
    this.currentEditDetail = null;
    this.selectedRadicalIds.clear();
    this.pinyinRawTouched = false;

    this.resetFormValidation();

    if (this.elements.modalFormTitle) {
      this.elements.modalFormTitle.textContent = 'Thêm Từ Vựng Mới';
    }
    if (this.elements.modalFormDesc) {
      this.elements.modalFormDesc.textContent = 'Nhập thông tin từ vựng tiếng Trung mới và gán các bộ thủ cấu thành.';
    }
    if (this.elements.vocabFormId) {
      this.elements.vocabFormId.value = '';
    }

    // Reset inputs
    if (this.elements.vocabForm) {
      this.elements.vocabForm.reset();
    }
    if (this.elements.radicalFilterInput) {
      this.elements.radicalFilterInput.value = '';
    }

    // Load radicals and render picker
    await this.ensureRadicalsLoaded();
    this.renderSelectedRadicalsSummary();
    this.renderAvailableRadicals('');

    this.showDialog(this.elements.formModal);
    if (this.elements.inputHanzi) {
      this.elements.inputHanzi.focus();
    }
  }

  /**
   * Opens edit modal for an existing vocabulary item.
   * Authoritatively fetches GET /api/v1/vocabulary/{id} to get complete data and constituent radicals.
   * 
   * @param {number} vocabId 
   */
  async openEditModal(vocabId) {
    this.resetFormValidation();

    try {
      const detail = await apiClient(`/vocabulary/${vocabId}`);
      if (!detail || typeof detail !== 'object') {
        showToast('Không tìm thấy dữ liệu chi tiết của từ vựng.', 'error');
        return;
      }

      this.currentEditDetail = detail;
      this.pinyinRawTouched = true; // prevent auto-overwrite in edit mode

      if (this.elements.modalFormTitle) {
        this.elements.modalFormTitle.textContent = `Chỉnh Sửa Từ Vựng #${detail.vocabId}`;
      }
      if (this.elements.modalFormDesc) {
        this.elements.modalFormDesc.textContent = `Cập nhật thông tin và bộ thủ cấu thành cho từ vựng "${detail.hanzi}".`;
      }
      if (this.elements.vocabFormId) {
        this.elements.vocabFormId.value = String(detail.vocabId);
      }

      // Populate form inputs
      if (this.elements.inputHanzi) this.elements.inputHanzi.value = detail.hanzi || '';
      if (this.elements.inputPinyin) this.elements.inputPinyin.value = detail.pinyin || '';
      if (this.elements.inputPinyinRaw) this.elements.inputPinyinRaw.value = detail.pinyinRaw || '';
      if (this.elements.inputMeaningHanViet) this.elements.inputMeaningHanViet.value = detail.meaningHanViet || '';
      if (this.elements.inputMeaningVi) this.elements.inputMeaningVi.value = detail.meaningVi || '';
      if (this.elements.inputAudioUrl) this.elements.inputAudioUrl.value = detail.audioUrl || '';
      if (this.elements.inputVideoWritingUrl) this.elements.inputVideoWritingUrl.value = detail.videoWritingUrl || '';
      if (this.elements.inputExampleSentence) this.elements.inputExampleSentence.value = detail.exampleSentence || '';
      if (this.elements.inputExampleTranslation) this.elements.inputExampleTranslation.value = detail.exampleTranslation || '';

      // Populate selected radicals from detail.radicals
      this.selectedRadicalIds.clear();
      if (Array.isArray(detail.radicals)) {
        for (const rad of detail.radicals) {
          if (rad.radicalId) {
            this.selectedRadicalIds.add(Number(rad.radicalId));
          }
        }
      }

      if (this.elements.radicalFilterInput) {
        this.elements.radicalFilterInput.value = '';
      }

      await this.ensureRadicalsLoaded();
      this.renderSelectedRadicalsSummary();
      this.renderAvailableRadicals('');

      this.showDialog(this.elements.formModal);
      if (this.elements.inputHanzi) {
        this.elements.inputHanzi.focus();
      }
    } catch (err) {
      const classified = classifyApiError(err, 'load');
      showToast(classified.userMessage, 'error');
    }
  }

  /**
   * Renders the badges of currently selected radicals.
   */
  renderSelectedRadicalsSummary() {
    if (!this.elements.selectedRadicalsContainer) return;
    clearContainer(this.elements.selectedRadicalsContainer);

    const count = this.selectedRadicalIds.size;
    if (this.elements.selectedRadicalsCount) {
      this.elements.selectedRadicalsCount.textContent = `${count} đã chọn`;
    }

    if (count === 0) {
      const prompt = createSafeElement('span', {
        className: 'text-muted small align-self-center fst-italic',
        text: 'Chưa chọn bộ thủ nào (không bắt buộc).',
        attrs: { id: 'emptyRadicalsPrompt' }
      });
      this.elements.selectedRadicalsContainer.appendChild(prompt);
      return;
    }

    const radicalsMap = new Map();
    if (Array.isArray(this.cachedRadicals)) {
      for (const r of this.cachedRadicals) {
        radicalsMap.set(Number(r.radicalId), r);
      }
    }

    const sortedIds = Array.from(this.selectedRadicalIds).sort((a, b) => a - b);

    for (const rId of sortedIds) {
      const rad = radicalsMap.get(rId);
      const char = rad?.character || `#${rId}`;
      const hv = rad?.meaningHanViet ? ` (${rad.meaningHanViet})` : '';

      const badge = createSafeElement('span', {
        className: 'badge bg-white text-dark border d-inline-flex align-items-center gap-1 py-1 px-2 me-1 mb-1'
      });

      const charSpan = createSafeElement('strong', {
        className: 'font-chinese text-chinese-red',
        text: char
      });
      badge.appendChild(charSpan);

      const labelSpan = createSafeElement('span', {
        className: 'small text-muted',
        text: hv
      });
      badge.appendChild(labelSpan);

      const removeBtn = createSafeElement('button', {
        className: 'btn-close ms-1',
        text: '',
        attrs: {
          type: 'button',
          style: 'font-size: 0.6rem;',
          'aria-label': `Bỏ chọn bộ thủ ${char}`
        }
      });
      removeBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        this.selectedRadicalIds.delete(rId);
        this.renderSelectedRadicalsSummary();
        this.updateAvailableRadicalsCheckboxes();
      });
      badge.appendChild(removeBtn);

      this.elements.selectedRadicalsContainer.appendChild(badge);
    }
  }

  /**
   * Renders available Kangxi radicals in the checkbox list, filtered by query.
   * 
   * @param {string} query 
   */
  renderAvailableRadicals(query = '') {
    if (!this.elements.availableRadicalsList) return;
    clearContainer(this.elements.availableRadicalsList);

    const filtered = filterRadicalsList(query, this.cachedRadicals || []);

    if (filtered.length === 0) {
      const emptyMsg = createSafeElement('div', {
        className: 'text-center text-muted small py-2',
        text: 'Không tìm thấy bộ thủ nào phù hợp.'
      });
      this.elements.availableRadicalsList.appendChild(emptyMsg);
      return;
    }

    const rowContainer = createSafeElement('div', { className: 'row g-1' });

    for (const rad of filtered) {
      const rId = Number(rad.radicalId);
      const isChecked = this.selectedRadicalIds.has(rId);

      const col = createSafeElement('div', { className: 'col-sm-6 col-md-4' });

      const formCheck = createSafeElement('div', { className: 'form-check form-check-inline py-1 px-2 m-0 w-100 rounded hover-bg-light' });

      const input = createSafeElement('input', {
        className: 'form-check-input',
        attrs: {
          type: 'checkbox',
          id: `radCheck-${rId}`,
          value: String(rId)
        }
      });
      input.checked = isChecked;

      input.addEventListener('change', (e) => {
        if (e.target.checked) {
          this.selectedRadicalIds.add(rId);
        } else {
          this.selectedRadicalIds.delete(rId);
        }
        this.renderSelectedRadicalsSummary();
      });

      const label = createSafeElement('label', {
        className: 'form-check-label small d-flex align-items-center gap-1 cursor-pointer w-100',
        attrs: { for: `radCheck-${rId}` }
      });

      const charStrong = createSafeElement('strong', {
        className: 'font-chinese text-chinese-red fs-6',
        text: rad.character || '—'
      });
      label.appendChild(charStrong);

      const infoSpan = createSafeElement('span', {
        className: 'text-truncate text-secondary',
        text: `${rad.pinyin || ''} ${rad.meaningHanViet ? '(' + rad.meaningHanViet + ')' : ''}`,
        attrs: { style: 'font-size: 0.8rem;' }
      });
      label.appendChild(infoSpan);

      formCheck.appendChild(input);
      formCheck.appendChild(label);
      col.appendChild(formCheck);
      rowContainer.appendChild(col);
    }

    this.elements.availableRadicalsList.appendChild(rowContainer);
  }

  /**
   * Synchronizes checked state of available checkboxes when badges are removed or cleared.
   */
  updateAvailableRadicalsCheckboxes() {
    if (!this.elements.availableRadicalsList) return;
    const checkboxes = this.elements.availableRadicalsList.querySelectorAll('input[type="checkbox"]');
    checkboxes.forEach(cb => {
      const rId = Number(cb.value);
      cb.checked = this.selectedRadicalIds.has(rId);
    });
  }

  /**
   * Resets form validation error states.
   */
  resetFormValidation() {
    if (this.elements.formGeneralError) {
      this.elements.formGeneralError.classList.add('d-none');
      this.elements.formGeneralError.textContent = '';
    }

    const fieldErrorKeys = [
      'Hanzi', 'Pinyin', 'PinyinRaw', 'MeaningHanViet', 'MeaningVi',
      'AudioUrl', 'VideoWritingUrl', 'ExampleSentence', 'ExampleTranslation'
    ];

    for (const key of fieldErrorKeys) {
      const inputEl = this.elements[`input${key}`];
      const errorEl = this.elements[`error${key}`];
      if (inputEl) inputEl.classList.remove('is-invalid');
      if (errorEl) {
        errorEl.textContent = '';
        errorEl.classList.remove('d-block');
      }
    }
  }

  /**
   * Applies field-level validation errors to the form.
   * 
   * @param {Record<string, string>} errors 
   */
  applyFieldErrors(errors) {
    if (errors._general && this.elements.formGeneralError) {
      this.elements.formGeneralError.textContent = errors._general;
      this.elements.formGeneralError.classList.remove('d-none');
    }

    const fieldMap = {
      hanzi: { input: this.elements.inputHanzi, error: this.elements.errorHanzi },
      pinyin: { input: this.elements.inputPinyin, error: this.elements.errorPinyin },
      pinyinRaw: { input: this.elements.inputPinyinRaw, error: this.elements.errorPinyinRaw },
      meaningHanViet: { input: this.elements.inputMeaningHanViet, error: this.elements.errorMeaningHanViet },
      meaningVi: { input: this.elements.inputMeaningVi, error: this.elements.errorMeaningVi },
      audioUrl: { input: this.elements.inputAudioUrl, error: this.elements.errorAudioUrl },
      videoWritingUrl: { input: this.elements.inputVideoWritingUrl, error: this.elements.errorVideoWritingUrl },
      exampleSentence: { input: this.elements.inputExampleSentence, error: this.elements.errorExampleSentence },
      exampleTranslation: { input: this.elements.inputExampleTranslation, error: this.elements.errorExampleTranslation }
    };

    let firstInvalidInput = null;

    for (const [field, msg] of Object.entries(errors)) {
      if (fieldMap[field]) {
        const { input, error } = fieldMap[field];
        if (input) {
          input.classList.add('is-invalid');
          if (!firstInvalidInput) firstInvalidInput = input;
        }
        if (error) {
          error.textContent = msg;
          error.classList.add('d-block');
        }
      } else if (!errors._general && this.elements.formGeneralError) {
        this.elements.formGeneralError.textContent = msg;
        this.elements.formGeneralError.classList.remove('d-none');
      }
    }

    if (firstInvalidInput) {
      firstInvalidInput.focus();
    }
  }

  /**
   * Handles Save Vocabulary (Create or Update).
   */
  async handleSaveVocab() {
    this.resetFormValidation();

    const editIdStr = this.elements.vocabFormId?.value;
    const isEdit = Boolean(editIdStr);
    const vocabId = isEdit ? Number(editIdStr) : null;

    // Collect data
    const rawData = {
      hanzi: this.elements.inputHanzi?.value,
      pinyin: this.elements.inputPinyin?.value,
      pinyinRaw: this.elements.inputPinyinRaw?.value,
      meaningHanViet: this.elements.inputMeaningHanViet?.value,
      meaningVi: this.elements.inputMeaningVi?.value,
      audioUrl: this.elements.inputAudioUrl?.value,
      videoWritingUrl: this.elements.inputVideoWritingUrl?.value,
      exampleSentence: this.elements.inputExampleSentence?.value,
      exampleTranslation: this.elements.inputExampleTranslation?.value,
      radicalIds: Array.from(this.selectedRadicalIds)
    };

    // Client validation
    const validation = validateVocabularyForm(rawData);
    if (!validation.valid) {
      this.applyFieldErrors(validation.errors);
      return;
    }

    const payload = buildVocabularyPayload(rawData);

    // No-op detection for update
    if (isEdit && this.currentEditDetail && isNoOpUpdate(this.currentEditDetail, payload)) {
      showToast('Không có thay đổi nào cần lưu.', 'info');
      this.closeFormModal();
      return;
    }

    // Set saving spinner
    this.setSavingState(true);

    try {
      let savedRecord;
      if (isEdit) {
        // Authoritative PUT /api/v1/admin/vocabulary/{id}
        savedRecord = await apiClient(`/admin/vocabulary/${vocabId}`, {
          method: 'PUT',
          body: JSON.stringify(payload)
        });
        showToast(`Cập nhật từ vựng "${payload.hanzi}" thành công.`, 'success');
      } else {
        // Authoritative POST /api/v1/admin/vocabulary
        savedRecord = await apiClient('/admin/vocabulary', {
          method: 'POST',
          body: JSON.stringify(payload)
        });
        showToast(`Tạo từ vựng "${payload.hanzi}" thành công.`, 'success');
      }

      this.closeFormModal();
      await this.loadCatalog();
    } catch (err) {
      const classified = classifyApiError(err, isEdit ? 'update' : 'create');

      if (classified.isConflict) {
        if (this.elements.formGeneralError) {
          this.elements.formGeneralError.textContent = classified.userMessage;
          this.elements.formGeneralError.classList.remove('d-none');
        }
      } else if (classified.isValidation && Object.keys(classified.fieldErrors).length > 0) {
        this.applyFieldErrors(classified.fieldErrors);
      } else {
        if (this.elements.formGeneralError) {
          this.elements.formGeneralError.textContent = classified.userMessage;
          this.elements.formGeneralError.classList.remove('d-none');
        }
      }
    } finally {
      this.setSavingState(false);
    }
  }

  /**
   * Sets UI state during save operation.
   * 
   * @param {boolean} isSaving 
   */
  setSavingState(isSaving) {
    if (this.elements.btnSaveVocab) {
      this.elements.btnSaveVocab.disabled = isSaving;
    }
    if (this.elements.saveVocabBtnSpinner) {
      this.elements.saveVocabBtnSpinner.classList.toggle('d-none', !isSaving);
    }
    if (this.elements.saveVocabBtnText) {
      this.elements.saveVocabBtnText.textContent = isSaving ? 'Đang lưu...' : 'Lưu từ vựng';
    }
  }

  /**
   * Opens delete confirmation modal for a vocabulary item.
   * 
   * @param {object} vocab 
   */
  openDeleteModal(vocab) {
    this.currentDeleteTarget = vocab;

    if (this.elements.deleteModalError) {
      this.elements.deleteModalError.classList.add('d-none');
      this.elements.deleteModalError.textContent = '';
    }

    if (this.elements.deleteTargetHanzi) {
      this.elements.deleteTargetHanzi.textContent = vocab.hanzi || '—';
    }
    if (this.elements.deleteTargetPinyin) {
      this.elements.deleteTargetPinyin.textContent = `${vocab.pinyin || ''} [${vocab.pinyinRaw || ''}]`;
    }
    if (this.elements.deleteTargetMeaning) {
      this.elements.deleteTargetMeaning.textContent = `${vocab.meaningHanViet || ''} — ${vocab.meaningVi || ''}`;
    }
    if (this.elements.deleteTargetId) {
      this.elements.deleteTargetId.textContent = `#${vocab.vocabId}`;
    }

    this.showDialog(this.elements.deleteModal);
    if (this.elements.btnCancelDeleteModal) {
      this.elements.btnCancelDeleteModal.focus();
    }
  }

  /**
   * Submits DELETE /api/v1/admin/vocabulary/{id} request to backend.
   * Properly handles 204 No Content without calling response.json().
   * Handles 409 reference conflicts (Lesson, PersonalNote, CardProgress, ReviewLog) safely.
   */
  async handleConfirmDelete() {
    if (!this.currentDeleteTarget || !this.currentDeleteTarget.vocabId) return;

    const id = this.currentDeleteTarget.vocabId;
    const hanzi = this.currentDeleteTarget.hanzi || '';

    this.setDeletingState(true);

    if (this.elements.deleteModalError) {
      this.elements.deleteModalError.classList.add('d-none');
      this.elements.deleteModalError.textContent = '';
    }

    try {
      // DELETE request resolves with null on 204 No Content
      await apiClient(`/admin/vocabulary/${id}`, {
        method: 'DELETE'
      });

      showToast(`Đã xóa từ vựng "${hanzi}" thành công.`, 'success');
      this.closeDeleteModal();

      // If this was the only item on the page and not page 0, go to previous page
      if (this.currentItems.length === 1 && this.currentPage > 0) {
        this.currentPage = this.currentPage - 1;
      }

      await this.loadCatalog();
    } catch (err) {
      const classified = classifyApiError(err, 'delete');

      if (this.elements.deleteModalError) {
        this.elements.deleteModalError.textContent = classified.userMessage;
        this.elements.deleteModalError.classList.remove('d-none');
      }
    } finally {
      this.setDeletingState(false);
    }
  }

  /**
   * Sets UI state during delete operation.
   * 
   * @param {boolean} isDeleting 
   */
  setDeletingState(isDeleting) {
    if (this.elements.btnConfirmDeleteVocab) {
      this.elements.btnConfirmDeleteVocab.disabled = isDeleting;
    }
    if (this.elements.deleteVocabBtnSpinner) {
      this.elements.deleteVocabBtnSpinner.classList.toggle('d-none', !isDeleting);
    }
    if (this.elements.deleteVocabBtnText) {
      this.elements.deleteVocabBtnText.textContent = isDeleting ? 'Đang xóa...' : 'Xác nhận xóa vĩnh viễn';
    }
  }

  /**
   * Closes the create/edit modal and restores focus.
   */
  closeFormModal() {
    this.closeDialog(this.elements.formModal);
    this.restoreFocus();
  }

  /**
   * Closes the delete modal and restores focus.
   */
  closeDeleteModal() {
    this.closeDialog(this.elements.deleteModal);
    this.restoreFocus();
  }

  /**
   * Opens HTML5 native dialog.
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
   * Closes HTML5 native dialog.
   * 
   * @param {HTMLDialogElement} dialog 
   */
  closeDialog(dialog) {
    if (!dialog) return;
    if (typeof dialog.close === 'function') {
      dialog.close();
    } else {
      dialog.removeAttribute('open');
    }
  }

  /**
   * Restores focus to the last focused element before modal open.
   */
  restoreFocus() {
    if (this.lastFocusedElement && typeof this.lastFocusedElement.focus === 'function') {
      this.lastFocusedElement.focus();
      this.lastFocusedElement = null;
    }
  }
}

/* -----------------------------------------------------------------------------
 * 3. AUTO-BOOTSTRAP IN BROWSER ENVIRONMENT
 * ----------------------------------------------------------------------------- */

if (typeof window !== 'undefined' && typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    const controller = new AdminVocabularyPageController();
    window._adminVocabularyPageController = controller;
    controller.init().catch(err => {
      console.error('[AdminVocabularyPage] Initialization error:', err);
    });
  });
}
