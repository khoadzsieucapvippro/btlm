/**
 * =============================================================================
 * CHINESE VOCABULARY CATALOG & SEARCH CONTROLLER (TASK 9B.3)
 * Module: frontend/js/pages/vocabulary-page.js
 * 
 * Responsibilities:
 * - Direct integration with Spring Boot backend via apiClient('/vocabulary').
 * - Multi-criteria search (Hanzi, Pinyin with tone, toneless Pinyin).
 * - Debounced input handling (300ms) with instant Enter/submit flush.
 * - Transport-level AbortController cancellation & request ID sequencing (stale response protection).
 * - Server-side pagination matching PageResponse<VocabularyResponse> contract.
 * - Progressive disclosure of constituent Kangxi radicals via detail endpoint (GET /vocabulary/{id}).
 * - Pronunciation synthesis via Web Speech API with Mandarin voice matching and graceful degradation.
 * - Accessible Native <dialog> modal with focus containment, Escape key, and focus restoration.
 * - Contextual media sanitization (audio) via sanitizeResourceUrl().
 * - 3-State UI lifecycle (Loading -> Ready / Empty / Error + Retry).
 * - Session-aware navigation bar integration via initNavbarAuth().
 * =============================================================================
 */

import { apiClient, ApiError } from '../api/api.js';
import { initNavbarAuth } from '../ui/nav.js';
import { createSafeElement, clearContainer, sanitizeResourceUrl, sanitizeNavigationUrl } from '../ui/security.js';
import { setComponentState, showToast } from '../ui/ui.js';
import { openNotesModal } from '../ui/notes-modal.js';

/* -----------------------------------------------------------------------------
 * 1. PURE LOGIC & UTILITY HELPERS (Exported for Testability)
 * ----------------------------------------------------------------------------- */

/**
 * Normalizes user search input string by trimming whitespace and lowercasing.
 * 
 * @param {*} query 
 * @returns {string}
 */
export function normalizeSearchQuery(query) {
  if (typeof query !== 'string') return '';
  return query.trim();
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

/**
 * Feature-detects Web Speech API SpeechSynthesis availability in current environment.
 * 
 * @returns {boolean}
 */
export function isSpeechSynthesisAvailable() {
  return typeof window !== 'undefined' &&
    'speechSynthesis' in window &&
    'SpeechSynthesisUtterance' in window &&
    typeof window.speechSynthesis?.speak === 'function';
}

/**
 * Synthesizes Chinese Mandarin pronunciation of given Hanzi text.
 * Gracefully handles missing voices or unsupported environments without throwing.
 * 
 * @param {string} text - Chinese character(s) to pronounce
 * @param {Object} [options]
 * @param {string} [options.lang='zh-CN'] - Language BCP-47 tag
 * @param {number} [options.rate=0.85] - Speech rate (slightly slower for learners)
 * @returns {boolean} true if utterance was dispatched, false otherwise
 */
export function speakHanzi(text, { lang = 'zh-CN', rate = 0.85 } = {}) {
  if (!text || typeof text !== 'string' || !text.trim()) {
    return false;
  }

  if (!isSpeechSynthesisAvailable()) {
    if (typeof document !== 'undefined') {
      showToast({
        type: 'info',
        title: 'Phát âm không khả dụng',
        message: 'Trình duyệt hiện tại không hỗ trợ Web Speech API.'
      });
    }
    return false;
  }

  try {
    // 1. Cancel previous pending speech to avoid Chromium queue lock
    window.speechSynthesis.cancel();

    // 2. Create and configure utterance
    const utterance = new SpeechSynthesisUtterance(text.trim());
    utterance.lang = lang;
    utterance.rate = rate;

    // 3. Try selecting a native Chinese voice if available
    const voices = window.speechSynthesis.getVoices?.() || [];
    const chineseVoice = voices.find(v => v && v.lang && (v.lang.startsWith('zh') || v.lang.startsWith('cmn')));
    if (chineseVoice) {
      utterance.voice = chineseVoice;
    }

    // 4. Non-fatal error listener
    utterance.onerror = (e) => {
      // Error code 'canceled' is expected on rapid clicks
      if (e.error !== 'canceled') {
        console.warn('[SpeechSynthesis] Utterance error:', e.error);
        if (typeof document !== 'undefined') {
          showToast({
            type: 'info',
            title: 'Phát âm tự động',
            message: 'Không thể phát âm thanh trên thiết bị này.'
          });
        }
      }
    };

    window.speechSynthesis.speak(utterance);
    return true;
  } catch (err) {
    console.warn('[SpeechSynthesis] Speech dispatch failed:', err);
    if (typeof document !== 'undefined') {
      showToast({
        type: 'warning',
        title: 'Lỗi phát âm',
        message: 'Không thể khởi động bộ phát âm trên trình duyệt.'
      });
    }
    return false;
  }
}

/**
 * Safely creates an SVG speaker icon element via createElementNS (Zero unsafe innerHTML).
 * 
 * @returns {SVGElement}
 */
export function createSpeakerIcon() {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('width', '16');
  svg.setAttribute('height', '16');
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('fill', 'none');
  svg.setAttribute('stroke', 'currentColor');
  svg.setAttribute('stroke-width', '2');
  svg.setAttribute('stroke-linecap', 'round');
  svg.setAttribute('stroke-linejoin', 'round');
  svg.setAttribute('aria-hidden', 'true');

  const polygon = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
  polygon.setAttribute('points', '11 5 6 9 2 9 2 15 6 15 11 19 11 5');

  const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  path.setAttribute('d', 'M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07');

  svg.appendChild(polygon);
  svg.appendChild(path);
  return svg;
}

/* -----------------------------------------------------------------------------
 * 2. VOCABULARY PAGE CONTROLLER CLASS
 * ----------------------------------------------------------------------------- */

export class VocabularyPageController {
  constructor() {
    // Application State
    this.currentPage = 0;
    this.pageSize = 20; // Default matching backend resolveEffectivePageable
    this.activeSearchQuery = '';
    this.activeTriggerEl = null;

    // Asynchronous Request Management & Stale Response Protection
    this.latestRequestId = 0;
    this.abortController = null;
    this.debounceTimer = null;
    this.debounceDelayMs = 300;

    // Cache for vocabulary details to avoid redundant network roundtrips on repeated clicks
    this.detailCache = new Map();

    // Active current modal item
    this.currentModalVocab = null;

    // DOM Element References
    this.elements = {};
  }

  /**
   * Initializes DOM references, events, session navbar, and initial data load.
   */
  init() {
    this.bindDomElements();
    this.bindEvents();

    // Initialize session-aware navigation bar
    initNavbarAuth('navAuthContainer');

    // Deep-linking: Seed initial query if present in URL (?search=...)
    let initialQuery = '';
    if (typeof window !== 'undefined' && window.location?.search) {
      const urlParams = new URLSearchParams(window.location.search);
      initialQuery = urlParams.get('search') || '';
    }

    if (initialQuery.trim()) {
      if (this.elements.searchInput) {
        this.elements.searchInput.value = initialQuery.trim();
      }
      this.activeSearchQuery = initialQuery.trim();
      if (this.elements.searchClearBtn) {
        this.elements.searchClearBtn.classList.remove('d-none');
      }
    }

    // Load initial catalog
    this.loadCatalog();
  }

  /**
   * Resolves interactive DOM nodes.
   */
  bindDomElements() {
    this.elements = {
      stateContainer: document.getElementById('vocabStateContainer'),
      grid: document.getElementById('vocabGrid'),
      pagination: document.getElementById('vocabPagination'),
      searchInput: document.getElementById('vocabSearchInput'),
      searchClearBtn: document.getElementById('searchClearBtn'),
      pageSizeSelect: document.getElementById('pageSizeSelect'),
      resultCount: document.getElementById('vocabResultCount'),
      modal: document.getElementById('vocabDetailModal'),
      modalCloseBtn: document.getElementById('modalCloseBtn'),
      modalDismissBtn: document.getElementById('modalDismissBtn'),
      modalVocabNoteBtn: document.getElementById('modalVocabNoteBtn'),
      modalVocabHanzi: document.getElementById('modalVocabHanzi'),
      modalSpeechBtn: document.getElementById('modalSpeechBtn'),
      modalVocabTitle: document.getElementById('modalVocabTitle'),
      modalVocabPinyin: document.getElementById('modalVocabPinyin'),
      modalVocabPinyinRaw: document.getElementById('modalVocabPinyinRaw'),
      modalVocabHanViet: document.getElementById('modalVocabHanViet'),
      modalVocabMeaning: document.getElementById('modalVocabMeaning'),
      modalRadicalsSection: document.getElementById('modalRadicalsSection'),
      modalRadicalsList: document.getElementById('modalRadicalsList'),
      modalRadicalsCount: document.getElementById('modalRadicalsCount'),
      modalRadicalPreview: document.getElementById('modalRadicalPreview'),
      modalExampleSection: document.getElementById('modalExampleSection'),
      modalVocabExampleSentence: document.getElementById('modalVocabExampleSentence'),
      modalVocabExampleTranslation: document.getElementById('modalVocabExampleTranslation'),
      modalAudioSection: document.getElementById('modalAudioSection'),
      modalAudioPlayer: document.getElementById('modalAudioPlayer')
    };
  }

  /**
   * Binds user event handlers.
   */
  bindEvents() {
    // 1. Search input handler (debounced)
    if (this.elements.searchInput) {
      this.elements.searchInput.addEventListener('input', (e) => {
        this.handleSearchInput(e.target.value);
      });

      // Enter key flushes debounce immediately
      this.elements.searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          this.flushDebounceAndSearch();
        }
      });
    }

    // 2. Search clear button
    if (this.elements.searchClearBtn) {
      this.elements.searchClearBtn.addEventListener('click', () => {
        this.handleClearSearch();
      });
    }

    // 3. Page size change handler
    if (this.elements.pageSizeSelect) {
      this.elements.pageSizeSelect.addEventListener('change', (e) => {
        this.pageSize = parseInt(e.target.value, 10) || 20;
        this.currentPage = 0; // Reset to first page
        this.loadCatalog();
      });
    }

    // 4. Modal close & dismiss buttons
    if (this.elements.modalCloseBtn) {
      this.elements.modalCloseBtn.addEventListener('click', () => this.closeDetailModal());
    }
    if (this.elements.modalDismissBtn) {
      this.elements.modalDismissBtn.addEventListener('click', () => this.closeDetailModal());
    }

    // 5. Modal Escape key and native cancel event
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

    // 6. Modal speech button
    if (this.elements.modalSpeechBtn) {
      this.elements.modalSpeechBtn.addEventListener('click', () => {
        if (this.currentModalVocab?.hanzi) {
          speakHanzi(this.currentModalVocab.hanzi);
        }
      });
    }

    // 7. Modal Note button
    if (this.elements.modalVocabNoteBtn) {
      this.elements.modalVocabNoteBtn.addEventListener('click', () => {
        if (this.currentModalVocab) {
          openNotesModal({
            vocabId: this.currentModalVocab.vocabId,
            hanzi: this.currentModalVocab.hanzi,
            pinyin: this.currentModalVocab.pinyin,
            meaning: this.currentModalVocab.meaningVi,
            triggerElement: this.elements.modalVocabNoteBtn
          });
        }
      });
    }
  }

  /**
   * Handles user typing in search input with 300ms debounce.
   * 
   * @param {string} rawValue 
   */
  handleSearchInput(rawValue) {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }

    // Toggle clear button immediately
    if (this.elements.searchClearBtn) {
      if (rawValue.trim().length > 0) {
        this.elements.searchClearBtn.classList.remove('d-none');
      } else {
        this.elements.searchClearBtn.classList.add('d-none');
      }
    }

    this.debounceTimer = setTimeout(() => {
      this.debounceTimer = null;
      this.activeSearchQuery = rawValue.trim();
      this.currentPage = 0; // Invariant: search change resets to page 0
      this.loadCatalog();
    }, this.debounceDelayMs);
  }

  /**
   * Immediately clears pending debounce timer and triggers search.
   */
  flushDebounceAndSearch() {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
      this.debounceTimer = null;
    }
    const val = this.elements.searchInput ? this.elements.searchInput.value.trim() : '';
    this.activeSearchQuery = val;
    this.currentPage = 0;
    this.loadCatalog();
  }

  /**
   * Clears search keyword, cancels in-flight requests, and loads default catalog.
   */
  handleClearSearch() {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
      this.debounceTimer = null;
    }
    if (this.elements.searchInput) {
      this.elements.searchInput.value = '';
      this.elements.searchInput.focus();
    }
    if (this.elements.searchClearBtn) {
      this.elements.searchClearBtn.classList.add('d-none');
    }
    this.activeSearchQuery = '';
    this.currentPage = 0;
    this.loadCatalog();
  }

  /**
   * Loads vocabulary catalog from backend with cancellation and stale response protection.
   */
  async loadCatalog() {
    if (!this.elements.stateContainer) return;

    // 1. Cancel previous in-flight request
    if (this.abortController) {
      this.abortController.abort();
    }
    this.abortController = new AbortController();

    // 2. Increment request ID token
    const currentRequestId = ++this.latestRequestId;

    // 3. Set UI Loading state
    const isSearching = this.activeSearchQuery.length > 0;
    setComponentState(this.elements.stateContainer, 'LOADING', {
      message: isSearching
        ? `Đang tìm kiếm từ vựng "${this.activeSearchQuery}"...`
        : 'Đang tải danh mục từ vựng...'
    });

    if (this.elements.grid) clearContainer(this.elements.grid);
    if (this.elements.pagination) this.elements.pagination.classList.add('d-none');

    try {
      // 4. Build query params matching contract: search, page, size (NEVER q)
      const params = {
        page: this.currentPage,
        size: this.pageSize
      };
      if (isSearching) {
        params.search = this.activeSearchQuery;
      }

      const response = await apiClient('/vocabulary', {
        params,
        signal: this.abortController.signal
      });

      // 5. Stale Response Protection: Check if this response belongs to the latest request
      if (currentRequestId !== this.latestRequestId) {
        return;
      }

      // 6. Parse and validate PageResponse
      const parsed = parsePageResponse(response);

      // Invariant: sync currentPage with backend returned page
      this.currentPage = parsed.page;

      if (parsed.totalElements === 0) {
        // Empty State: distinguish between empty catalog vs no search results
        if (isSearching) {
          showToast({
            type: 'info',
            title: 'Không tìm thấy',
            message: `Không có từ vựng nào khớp với "${this.activeSearchQuery}".`
          });
          setComponentState(this.elements.stateContainer, 'EMPTY', {
            glyph: '查',
            title: 'Không tìm thấy từ vựng',
            description: `Không có từ vựng nào khớp với từ khóa "${this.activeSearchQuery}". Bạn có thể thử tìm bằng Pinyin không dấu hoặc chữ Hán khác.`,
            actionText: 'Xóa bộ lọc tìm kiếm',
            onAction: () => this.handleClearSearch()
          });
        } else {
          setComponentState(this.elements.stateContainer, 'EMPTY', {
            glyph: '空',
            title: 'Danh mục trống',
            description: 'Hiện chưa có dữ liệu từ vựng nào trong hệ thống.'
          });
        }

        this.updateResultCountBadge(0, 0);
        return;
      }

      // 7. Transition to Ready state and render
      setComponentState(this.elements.stateContainer, 'READY');
      this.renderGrid(parsed.items);
      this.renderPagination(parsed);
      this.updateResultCountBadge(parsed.items.length, parsed.totalElements);

    } catch (err) {
      // Ignore intentional aborts from subsequent typing
      if (err.name === 'AbortError') {
        return;
      }

      // Only handle errors if request is still current
      if (currentRequestId === this.latestRequestId) {
        console.error('[VocabularyCatalog] Failed to load catalog:', err);
        let errorTitle = 'Lỗi tải danh mục từ vựng';
        let errorMessage = 'Không thể kết nối với máy chủ từ vựng. Vui lòng kiểm tra lại.';

        if (err instanceof ApiError) {
          if (err.status === 0 || err.code === 'NETWORK_ERROR') {
            errorTitle = 'Không thể kết nối máy chủ';
            errorMessage = 'Không thể kết nối đến máy chủ backend (http://localhost:8080). Vui lòng kiểm tra trạng thái máy chủ.';
          } else if (err.status === 408 || err.code === 'TIMEOUT') {
            errorTitle = 'Hết thời gian chờ';
            errorMessage = 'Yêu cầu tải dữ liệu từ vựng hết thời gian chờ (Timeout). Vui lòng thử lại.';
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
          retryText: 'Thử lại',
          onRetry: () => this.loadCatalog()
        });

        if (this.elements.resultCount) {
          this.elements.resultCount.textContent = 'Lỗi kết nối';
          this.elements.resultCount.className = 'badge-status badge-status-rejected text-nowrap';
        }
      }
    }
  }

  /**
   * Updates screen reader live region and visual status counter.
   * 
   * @param {number} currentCount 
   * @param {number} totalElements 
   */
  updateResultCountBadge(currentCount, totalElements) {
    if (!this.elements.resultCount) return;

    if (this.activeSearchQuery.length > 0) {
      if (totalElements === 0) {
        this.elements.resultCount.textContent = 'Không có kết quả';
        this.elements.resultCount.className = 'badge-status badge-status-pending text-nowrap';
      } else {
        this.elements.resultCount.textContent = `Tìm thấy ${totalElements} từ vựng`;
        this.elements.resultCount.className = 'badge-status badge-status-active text-nowrap';
      }
    } else {
      if (totalElements === 0) {
        this.elements.resultCount.textContent = '0 từ vựng';
        this.elements.resultCount.className = 'badge-status badge-status-draft text-nowrap';
      } else {
        const start = this.currentPage * this.pageSize + 1;
        const end = Math.min(totalElements, start + currentCount - 1);
        this.elements.resultCount.textContent = `Hiển thị ${start}–${end} / ${totalElements} từ vựng`;
        this.elements.resultCount.className = 'badge-status badge-status-draft text-nowrap';
      }
    }
  }

  /**
   * Renders vocabulary cards safely using DOM APIs (Zero-unsafe innerHTML).
   * 
   * @param {Array<Object>} items 
   */
  renderGrid(items) {
    if (!this.elements.grid) return;
    clearContainer(this.elements.grid);

    for (const vocab of items) {
      if (!vocab) continue;

      const card = createSafeElement('div', {
        className: 'vocab-card',
        attrs: {
          role: 'listitem',
          tabindex: '0',
          'aria-label': `Xem chi tiết từ vựng ${vocab.hanzi || ''} (${vocab.pinyin || ''} - ${vocab.meaningHanViet || ''})`
        }
      });

      // Top Row: Hanzi character + Action Buttons (Note + Speech)
      const topRow = createSafeElement('div', { className: 'vocab-card-top' });

      const hanziEl = createSafeElement('div', {
        className: 'vocab-card-hanzi',
        attrs: { lang: 'zh-Hans' },
        text: vocab.hanzi || '—'
      });

      const cardActions = createSafeElement('div', { className: 'd-flex align-items-center gap-1' });

      const noteBtn = createSafeElement('button', {
        className: 'vocab-speech-btn vocab-card-note-btn',
        attrs: {
          type: 'button',
          'aria-label': `Ghi chú cá nhân cho từ vựng ${vocab.hanzi || ''}`,
          title: 'Ghi chú cá nhân'
        },
        text: '记'
      });
      noteBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        openNotesModal({
          vocabId: vocab.vocabId,
          hanzi: vocab.hanzi,
          pinyin: vocab.pinyin,
          meaning: vocab.meaningVi,
          triggerElement: noteBtn
        });
      });

      const speechBtn = createSafeElement('button', {
        className: 'vocab-speech-btn',
        attrs: {
          type: 'button',
          'aria-label': `Phát âm từ ${vocab.hanzi || ''}`,
          title: `Phát âm: ${vocab.hanzi || ''}`
        }
      });
      // SVG Speaker Icon using safe DOM element creation
      const svgIcon = createSpeakerIcon();
      speechBtn.appendChild(svgIcon);

      speechBtn.addEventListener('click', (e) => {
        e.stopPropagation(); // Do not trigger card detail opening
        if (vocab.hanzi) {
          speakHanzi(vocab.hanzi);
        }
      });

      cardActions.appendChild(noteBtn);
      cardActions.appendChild(speechBtn);

      topRow.appendChild(hanziEl);
      topRow.appendChild(cardActions);

      // Meta Container: Pinyin, Hán-Việt, Vietnamese definition
      const metaContainer = createSafeElement('div', { className: 'vocab-card-meta' });

      const pinyinEl = createSafeElement('div', {
        className: 'vocab-card-pinyin',
        text: vocab.pinyin || '—'
      });

      const hanvietEl = createSafeElement('div', {
        className: 'vocab-card-hanviet',
        text: vocab.meaningHanViet ? `Hán-Việt: ${vocab.meaningHanViet}` : '—'
      });

      const meaningEl = createSafeElement('div', {
        className: 'vocab-card-meaning',
        attrs: { title: vocab.meaningVi || '' },
        text: vocab.meaningVi || 'Chưa có giải nghĩa.'
      });

      metaContainer.appendChild(pinyinEl);
      metaContainer.appendChild(hanvietEl);
      metaContainer.appendChild(meaningEl);

      card.appendChild(topRow);
      card.appendChild(metaContainer);

      // Activation handlers for detail modal
      card.addEventListener('click', (e) => {
        this.openDetailModal(vocab, e.currentTarget);
      });

      card.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          this.openDetailModal(vocab, e.currentTarget);
        }
      });

      this.elements.grid.appendChild(card);
    }
  }

  /**
   * Renders accessible pagination controls.
   * 
   * @param {Object} parsed 
   */
  renderPagination(parsed) {
    if (!this.elements.pagination) return;

    if (parsed.totalPages <= 1) {
      this.elements.pagination.classList.add('d-none');
      clearContainer(this.elements.pagination);
      return;
    }

    this.elements.pagination.classList.remove('d-none');
    clearContainer(this.elements.pagination);

    const pag = calculatePagination(parsed.totalElements, parsed.page, parsed.size);

    // Summary label
    const summary = createSafeElement('div', {
      className: 'small text-secondary fw-medium',
      text: `Hiển thị ${pag.startIndex + 1}–${pag.endIndex} trong tổng số ${parsed.totalElements} từ vựng`
    });

    // Control buttons container
    const controls = createSafeElement('div', {
      className: 'pagination-controls',
      attrs: { role: 'group', 'aria-label': 'Điều khiển chuyển trang từ vựng' }
    });

    // Previous Button
    const prevBtn = createSafeElement('button', {
      className: 'pagination-btn',
      attrs: {
        type: 'button',
        'aria-label': 'Trang trước',
        id: 'vocabPaginationPrevBtn'
      },
      text: '‹'
    });
    if (pag.pageIndex === 0) {
      prevBtn.setAttribute('disabled', 'true');
    } else {
      prevBtn.addEventListener('click', () => {
        this.currentPage = pag.pageIndex - 1;
        this.loadCatalog();
      });
    }
    controls.appendChild(prevBtn);

    // Numbered Buttons (Smart Window)
    for (let p = pag.startPage; p <= pag.endPage; p++) {
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
          this.loadCatalog();
        });
      }

      controls.appendChild(pageBtn);
    }

    // Next Button
    const nextBtn = createSafeElement('button', {
      className: 'pagination-btn',
      attrs: {
        type: 'button',
        'aria-label': 'Trang sau',
        id: 'vocabPaginationNextBtn'
      },
      text: '›'
    });
    if (pag.pageIndex >= pag.totalPages - 1) {
      nextBtn.setAttribute('disabled', 'true');
    } else {
      nextBtn.addEventListener('click', () => {
        this.currentPage = pag.pageIndex + 1;
        this.loadCatalog();
      });
    }
    controls.appendChild(nextBtn);

    this.elements.pagination.appendChild(summary);
    this.elements.pagination.appendChild(controls);
  }

  /**
   * Opens the accessible Detail Modal for a vocabulary item.
   * Leverages progressive disclosure: fetches constituent radicals via GET /vocabulary/{id}.
   * 
   * @param {Object} vocabSummary 
   * @param {HTMLElement} triggerEl 
   */
  async openDetailModal(vocabSummary, triggerEl) {
    if (!this.elements.modal) return;
    this.activeTriggerEl = triggerEl;
    this.currentModalVocab = vocabSummary;

    // 1. Immediately populate summary data into modal
    if (this.elements.modalVocabHanzi) {
      this.elements.modalVocabHanzi.textContent = vocabSummary.hanzi || '—';
    }
    if (this.elements.modalVocabTitle) {
      this.elements.modalVocabTitle.textContent = `Từ vựng: ${vocabSummary.hanzi || ''} (${vocabSummary.meaningHanViet || ''})`;
    }
    if (this.elements.modalVocabPinyin) {
      this.elements.modalVocabPinyin.textContent = vocabSummary.pinyin || '—';
    }
    if (this.elements.modalVocabPinyinRaw) {
      this.elements.modalVocabPinyinRaw.textContent = vocabSummary.pinyinRaw || '—';
    }
    if (this.elements.modalVocabHanViet) {
      this.elements.modalVocabHanViet.textContent = vocabSummary.meaningHanViet || '—';
    }
    if (this.elements.modalVocabMeaning) {
      this.elements.modalVocabMeaning.textContent = vocabSummary.meaningVi || 'Chưa có giải nghĩa chi tiết.';
    }

    // Reset radical preview box
    if (this.elements.modalRadicalPreview) {
      this.elements.modalRadicalPreview.classList.add('d-none');
      clearContainer(this.elements.modalRadicalPreview);
    }

    // Example sentence from summary
    this.updateExampleSection(vocabSummary.exampleSentence, vocabSummary.exampleTranslation);

    // Initial radicals loading indicator
    if (this.elements.modalRadicalsList) {
      clearContainer(this.elements.modalRadicalsList);
      this.elements.modalRadicalsList.appendChild(
        createSafeElement('span', { className: 'text-muted small', text: 'Đang tải thông tin bộ thủ...' })
      );
    }
    if (this.elements.modalRadicalsCount) {
      this.elements.modalRadicalsCount.textContent = '...';
    }

    // Audio Player setup
    this.setupAudioPlayer(vocabSummary.audioUrl);

    // 2. Open Modal with Native Focus Trap (WCAG 2.2 APG Pattern)
    if (typeof this.elements.modal.showModal === 'function') {
      this.elements.modal.showModal();
    } else {
      this.elements.modal.setAttribute('open', '');
      this.elements.modal.style.display = 'block';
    }

    // Move focus to modal close button
    if (this.elements.modalCloseBtn) {
      this.elements.modalCloseBtn.focus();
    }

    // 3. Progressive Disclosure: Fetch rich details including constituent radicals
    if (vocabSummary.vocabId) {
      await this.loadVocabularyDetail(vocabSummary.vocabId);
    }
  }

  /**
   * Fetches detailed vocabulary information including constituent radicals.
   * 
   * @param {number|string} vocabId 
   */
  async loadVocabularyDetail(vocabId) {
    try {
      let detail = this.detailCache.get(vocabId);
      if (!detail) {
        detail = await apiClient(`/vocabulary/${vocabId}`);
        if (detail) {
          this.detailCache.set(vocabId, detail);
        }
      }

      // Check if user hasn't closed modal or switched to another item
      if (!this.currentModalVocab || this.currentModalVocab.vocabId !== vocabId) {
        return;
      }

      // Update example sentences if enriched
      if (detail.exampleSentence || detail.exampleTranslation) {
        this.updateExampleSection(detail.exampleSentence, detail.exampleTranslation);
      }

      // Update audio if enriched
      if (detail.audioUrl) {
        this.setupAudioPlayer(detail.audioUrl);
      }

      // Render constituent Kangxi Radicals
      this.renderConstituentRadicals(detail.radicals);

    } catch (err) {
      console.warn('[VocabularyDetail] Failed to load detail for vocabId:', vocabId, err);
      if (this.elements.modalRadicalsList) {
        clearContainer(this.elements.modalRadicalsList);
        this.elements.modalRadicalsList.appendChild(
          createSafeElement('span', {
            className: 'text-muted small',
            text: 'Không thể tải thông tin bộ thủ cho từ này.'
          })
        );
      }
      if (this.elements.modalRadicalsCount) {
        this.elements.modalRadicalsCount.textContent = '0 bộ thủ';
      }
    }
  }

  /**
   * Renders constituent radical badges in the detail modal.
   * 
   * @param {Array<Object>} radicals 
   */
  renderConstituentRadicals(radicals) {
    if (!this.elements.modalRadicalsList) return;
    clearContainer(this.elements.modalRadicalsList);

    const radicalList = Array.isArray(radicals) ? radicals : [];

    if (this.elements.modalRadicalsCount) {
      this.elements.modalRadicalsCount.textContent = `${radicalList.length} bộ thủ`;
    }

    if (radicalList.length === 0) {
      this.elements.modalRadicalsList.appendChild(
        createSafeElement('span', {
          className: 'text-muted small',
          text: 'Từ vựng này không có bộ thủ liên kết.'
        })
      );
      return;
    }

    for (const rad of radicalList) {
      if (!rad) continue;

      const badge = createSafeElement('button', {
        className: 'vocab-radical-badge',
        attrs: {
          type: 'button',
          'aria-label': `Xem thông tin bộ thủ ${rad.character || ''} (${rad.meaningHanViet || ''})`
        },
        children: [
          createSafeElement('span', {
            className: 'vocab-radical-badge-glyph',
            attrs: { lang: 'zh-Hans' },
            text: rad.character || ''
          }),
          createSafeElement('span', {
            text: `${rad.pinyin || ''} (${rad.meaningHanViet || ''})`
          })
        ]
      });

      badge.addEventListener('click', () => {
        this.showRadicalPreview(rad);
      });

      this.elements.modalRadicalsList.appendChild(badge);
    }
  }

  /**
   * Expands constituent radical preview inside the modal and provides a safe link to 214 Radicals page.
   * 
   * @param {Object} radical 
   */
  showRadicalPreview(radical) {
    if (!this.elements.modalRadicalPreview) return;
    clearContainer(this.elements.modalRadicalPreview);
    this.elements.modalRadicalPreview.classList.remove('d-none');

    const preview = createSafeElement('div', {
      className: 'd-flex flex-column gap-2',
      children: [
        createSafeElement('div', {
          className: 'd-flex align-items-center justify-content-between flex-wrap gap-2',
          children: [
            createSafeElement('div', {
              className: 'd-flex align-items-center gap-2',
              children: [
                createSafeElement('span', {
                  className: 'fs-4 fw-bold font-hanzi text-dark',
                  attrs: { lang: 'zh-Hans' },
                  text: radical.character || ''
                }),
                createSafeElement('span', {
                  className: 'fw-semibold text-danger',
                  text: radical.pinyin || ''
                }),
                createSafeElement('span', {
                  className: 'small text-secondary',
                  text: `• Bộ #${radical.radicalId || ''} (${radical.meaningHanViet || ''})`
                })
              ]
            }),
            createSafeElement('a', {
              className: 'btn-chinese-outline btn-sm text-decoration-none',
              attrs: {
                href: sanitizeNavigationUrl(`radicals.html?search=${encodeURIComponent(radical.character || '')}`),
                target: '_blank',
                rel: 'noopener noreferrer'
              },
              text: 'Xem trong 214 Bộ Thủ ↗'
            })
          ]
        }),
        createSafeElement('div', {
          className: 'small text-secondary',
          text: radical.meaningVi ? `Nghĩa: ${radical.meaningVi}` : 'Chưa có giải nghĩa chi tiết cho bộ thủ này.'
        })
      ]
    });

    this.elements.modalRadicalPreview.appendChild(preview);
  }

  /**
   * Updates example sentence display.
   * 
   * @param {string} sentence 
   * @param {string} translation 
   */
  updateExampleSection(sentence, translation) {
    if (!this.elements.modalExampleSection) return;

    if (sentence && sentence.trim()) {
      if (this.elements.modalVocabExampleSentence) {
        this.elements.modalVocabExampleSentence.textContent = sentence.trim();
      }
      if (this.elements.modalVocabExampleTranslation) {
        this.elements.modalVocabExampleTranslation.textContent = translation ? translation.trim() : '';
      }
      this.elements.modalExampleSection.classList.remove('d-none');
    } else {
      this.elements.modalExampleSection.classList.add('d-none');
    }
  }

  /**
   * Safely configures authentic audio player.
   * 
   * @param {string} rawAudioUrl 
   */
  setupAudioPlayer(rawAudioUrl) {
    if (!this.elements.modalAudioSection || !this.elements.modalAudioPlayer) return;

    const safeUrl = sanitizeResourceUrl(rawAudioUrl);
    if (rawAudioUrl && safeUrl !== 'about:blank') {
      this.elements.modalAudioPlayer.pause();
      this.elements.modalAudioPlayer.src = safeUrl;
      this.elements.modalAudioPlayer.load();
      this.elements.modalAudioSection.classList.remove('d-none');
    } else {
      this.elements.modalAudioPlayer.pause();
      this.elements.modalAudioPlayer.removeAttribute('src');
      this.elements.modalAudioSection.classList.add('d-none');
    }
  }

  /**
   * Closes the Detail Modal and restores focus to the triggering element (WCAG 2.2 APG Pattern).
   */
  closeDetailModal() {
    if (!this.elements.modal) return;

    // Pause audio if active
    if (this.elements.modalAudioPlayer) {
      this.elements.modalAudioPlayer.pause();
    }

    // Cancel any active speech
    if (isSpeechSynthesisAvailable()) {
      window.speechSynthesis.cancel();
    }

    // Close modal
    if (typeof this.elements.modal.close === 'function') {
      this.elements.modal.close();
    } else {
      this.elements.modal.removeAttribute('open');
      this.elements.modal.style.display = 'none';
    }

    this.currentModalVocab = null;

    // Restore focus to triggering card
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
    const controller = new VocabularyPageController();
    controller.init();
    window.__vocabularyPageController = controller;
  });
}
