/**
 * =============================================================================
 * CREATOR LESSON STUDIO PAGE CONTROLLER (TASK 9D.1)
 * Module: frontend/js/pages/creator-lessons-page.js
 * 
 * Responsibilities:
 * - Direct integration with Spring Boot backend via apiClient('/creator/lessons').
 * - Role-guarded lifecycle: strictly accessible to 'Creator' and 'Admin'.
 * - Server-side pagination matching PageResponse<LessonSummaryResponse> contract.
 * - Minimal Draft lesson creation via POST /api/v1/creator/lessons.
 * - Safe DOM construction (zero unsafe innerHTML) defending against XSS.
 * - Accessible landmarks, pagination semantics, modal dialog focus management.
 * =============================================================================
 */

import { apiClient } from '../api/api.js';
import { authManager } from '../auth/auth-state.js';
import { initNavbarAuth } from '../ui/nav.js';
import { createSafeElement, clearContainer } from '../ui/security.js';
import { showLoading, showEmpty, showError, showToast } from '../ui/ui.js';

/* -----------------------------------------------------------------------------
 * 1. PURE LOGIC & UTILITY HELPERS (Exported for Testability)
 * ----------------------------------------------------------------------------- */

/**
 * Validates lesson title for creation.
 * Rules: non-empty, non-whitespace, maximum 200 characters.
 * 
 * @param {string} title 
 * @returns {{ valid: boolean, error?: string, sanitizedTitle: string }}
 */
export function validateCreateLessonTitle(title) {
  if (title === null || title === undefined) {
    return { valid: false, error: 'Tiêu đề bài học không được để trống', sanitizedTitle: '' };
  }
  const trimmed = String(title).trim();
  if (trimmed.length === 0) {
    return { valid: false, error: 'Tiêu đề bài học không được để trống', sanitizedTitle: '' };
  }
  if (trimmed.length > 200) {
    return { valid: false, error: 'Tiêu đề bài học không được vượt quá 200 ký tự', sanitizedTitle: trimmed };
  }
  return { valid: true, sanitizedTitle: trimmed };
}

/**
 * Safely parses backend PageResponse<LessonSummaryResponse> payload.
 * 
 * @param {any} envelopeOrData 
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
export function parseLessonSummaries(envelopeOrData) {
  if (!envelopeOrData || typeof envelopeOrData !== 'object') {
    return { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
  }

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
 * Maps backend lesson lifecycle status to CSS badge classes.
 * 
 * @param {string} status 
 * @returns {string}
 */
export function getStatusBadgeClass(status) {
  switch (String(status).toLowerCase()) {
    case 'approved':
      return 'badge-status badge-status-approved';
    case 'pending':
      return 'badge-status badge-status-pending';
    case 'rejected':
      return 'badge-status badge-status-rejected';
    case 'draft':
    default:
      return 'badge-status badge-status-draft';
  }
}

/**
 * Maps backend lesson lifecycle status to human-readable Vietnamese label.
 * 
 * @param {string} status 
 * @returns {string}
 */
export function getStatusLabel(status) {
  switch (String(status).toLowerCase()) {
    case 'approved':
      return 'Đã duyệt';
    case 'pending':
      return 'Chờ duyệt';
    case 'rejected':
      return 'Từ chối';
    case 'draft':
    default:
      return 'Bản nháp';
  }
}

/**
 * Formats an ISO date string into DD/MM/YYYY HH:mm.
 * 
 * @param {string} isoString 
 * @returns {string}
 */
export function formatLessonDate(isoString) {
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
 * Evaluates whether a lesson can be submitted for moderation.
 * Requirements:
 * - Status must be 'Draft' or 'Rejected'.
 * - Vocabulary count must be greater than 0.
 * 
 * @param {string} status 
 * @param {number} vocabCount 
 * @returns {{ canSubmit: boolean, reason?: string }}
 */
export function canSubmitLesson(status, vocabCount) {
  const s = String(status || '').trim().toLowerCase();
  const count = Number(vocabCount) || 0;

  if (s !== 'draft' && s !== 'rejected') {
    return {
      canSubmit: false,
      reason: s === 'pending'
        ? 'Bài học đang chờ kiểm duyệt'
        : s === 'approved'
          ? 'Bài học đã được phê duyệt'
          : 'Trạng thái bài học không hợp lệ để nộp kiểm duyệt'
    };
  }

  if (count <= 0) {
    return {
      canSubmit: false,
      reason: 'Bài học phải chứa ít nhất một từ vựng trước khi nộp kiểm duyệt'
    };
  }

  return { canSubmit: true };
}

/**
 * Returns submit button presentation config based on status.
 * 
 * @param {string} status 
 * @returns {{ label: string, actionText: string, className: string }|null}
 */
export function getSubmitButtonConfig(status) {
  const s = String(status || '').trim().toLowerCase();
  if (s === 'rejected') {
    return {
      label: 'Nộp lại',
      actionText: 'Nộp lại kiểm duyệt',
      className: 'btn-chinese-primary py-1 px-2 btn-sm text-decoration-none'
    };
  }
  if (s === 'draft') {
    return {
      label: 'Nộp duyệt',
      actionText: 'Nộp kiểm duyệt',
      className: 'btn-chinese-primary py-1 px-2 btn-sm text-decoration-none'
    };
  }
  return null;
}

/* -----------------------------------------------------------------------------
 * 2. PAGE CONTROLLER CLASS
 * ----------------------------------------------------------------------------- */

export class CreatorLessonsPageController {
  constructor() {
    this.currentPage = 0;
    this.pageSize = 20;
    this.totalElements = 0;
    this.totalPages = 0;
    this.isCreating = false;
    this.isSubmitting = false;
    this.pendingSubmitLesson = null;
    this.lastActiveElement = null;

    this.elements = {};
  }

  init() {
    initNavbarAuth();
    this.bindDomElements();
    this.bindEvents();

    // Enforce role guard for UX
    if (!authManager.isAuthenticated()) {
      window.location.href = 'login.html?redirect=creator-lessons.html';
      return;
    }

    const hasAccess = authManager.hasRole('Creator') || authManager.hasRole('Admin');
    if (!hasAccess) {
      this.showUnauthorizedState();
      return;
    }

    this.loadLessons();
  }

  bindDomElements() {
    this.elements = {
      authGuardContainer: document.getElementById('creatorAuthGuardContainer'),
      workspaceSection: document.getElementById('creatorWorkspaceSection'),
      stateContainer: document.getElementById('creatorStateContainer'),
      tableContainer: document.getElementById('creatorLessonsTableContainer'),
      tableBody: document.getElementById('creatorLessonsTableBody'),
      pageSizeSelect: document.getElementById('creatorPageSizeSelect'),
      resultCountBadge: document.getElementById('creatorResultCountBadge'),
      paginationNav: document.getElementById('creatorPaginationNav'),
      paginationSummary: document.getElementById('creatorPaginationSummary'),
      paginationList: document.getElementById('creatorPaginationList'),
      btnOpenCreateModal: document.getElementById('btnOpenCreateModal'),
      createModal: document.getElementById('createLessonModal'),
      btnCancelCreateModal: document.getElementById('btnCancelCreateModal'),
      btnDismissCreateModal: document.getElementById('btnDismissCreateModal'),
      createLessonForm: document.getElementById('createLessonForm'),
      newLessonTitleInput: document.getElementById('newLessonTitleInput'),
      createTitleError: document.getElementById('createTitleError'),
      createTitleCounter: document.getElementById('createTitleCounter'),
      btnSubmitCreateLesson: document.getElementById('btnSubmitCreateLesson'),
      // Task 9D.3 submit for moderation modal
      submitModal: document.getElementById('submitModerationModal'),
      btnCancelSubmitModal: document.getElementById('btnCancelSubmitModal'),
      btnDismissSubmitModal: document.getElementById('btnDismissSubmitModal'),
      btnConfirmSubmitModeration: document.getElementById('btnConfirmSubmitModeration'),
      submitModalLessonTitle: document.getElementById('submitModalLessonTitle'),
      submitModalLessonStatus: document.getElementById('submitModalLessonStatus'),
      submitModalError: document.getElementById('submitModalError')
    };
  }

  bindEvents() {
    // 1. Page size switcher
    if (this.elements.pageSizeSelect) {
      this.elements.pageSizeSelect.addEventListener('change', (e) => {
        this.pageSize = Math.max(1, Number(e.target.value) || 20);
        this.currentPage = 0;
        this.loadLessons();
      });
    }

    // 2. Open create modal
    if (this.elements.btnOpenCreateModal && this.elements.createModal) {
      this.elements.btnOpenCreateModal.addEventListener('click', () => {
        this.openCreateModal();
      });
    }

    // 3. Close create modal buttons
    const closeHandler = () => this.closeCreateModal();
    if (this.elements.btnCancelCreateModal) {
      this.elements.btnCancelCreateModal.addEventListener('click', closeHandler);
    }
    if (this.elements.btnDismissCreateModal) {
      this.elements.btnDismissCreateModal.addEventListener('click', closeHandler);
    }

    // 4. Modal live character counter and validation
    if (this.elements.newLessonTitleInput) {
      this.elements.newLessonTitleInput.addEventListener('input', (e) => {
        const val = e.target.value;
        if (this.elements.createTitleCounter) {
          this.elements.createTitleCounter.textContent = `${val.length}/200`;
        }
        if (this.elements.createTitleError) {
          this.elements.createTitleError.textContent = '';
        }
      });
    }

    // 5. Create lesson form submission
    if (this.elements.createLessonForm) {
      this.elements.createLessonForm.addEventListener('submit', (e) => {
        e.preventDefault();
        this.handleCreateLessonSubmit();
      });
    }

    // 6. Close submit modal buttons (Task 9D.3)
    const closeSubmitHandler = () => this.closeSubmitModal();
    if (this.elements.btnCancelSubmitModal) {
      this.elements.btnCancelSubmitModal.addEventListener('click', closeSubmitHandler);
    }
    if (this.elements.btnDismissSubmitModal) {
      this.elements.btnDismissSubmitModal.addEventListener('click', closeSubmitHandler);
    }

    // 7. Confirm submit button (Task 9D.3)
    if (this.elements.btnConfirmSubmitModeration) {
      this.elements.btnConfirmSubmitModeration.addEventListener('click', () => {
        this.handleConfirmSubmit();
      });
    }

    // 8. Handle native Escape on submit dialog
    if (this.elements.submitModal) {
      this.elements.submitModal.addEventListener('cancel', () => {
        this.pendingSubmitLesson = null;
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

  openCreateModal() {
    if (!this.elements.createModal) return;
    if (this.elements.newLessonTitleInput) {
      this.elements.newLessonTitleInput.value = '';
      if (this.elements.createTitleCounter) {
        this.elements.createTitleCounter.textContent = '0/200';
      }
      if (this.elements.createTitleError) {
        this.elements.createTitleError.textContent = '';
      }
    }
    if (typeof this.elements.createModal.showModal === 'function') {
      this.elements.createModal.showModal();
    } else {
      this.elements.createModal.setAttribute('open', 'true');
    }
    if (this.elements.newLessonTitleInput) {
      this.elements.newLessonTitleInput.focus();
    }
  }

  closeCreateModal() {
    if (!this.elements.createModal) return;
    if (typeof this.elements.createModal.close === 'function') {
      this.elements.createModal.close();
    } else {
      this.elements.createModal.removeAttribute('open');
    }
    if (this.elements.btnOpenCreateModal) {
      this.elements.btnOpenCreateModal.focus();
    }
  }

  openSubmitModal(lesson) {
    if (!this.elements.submitModal || !lesson) return;
    this.pendingSubmitLesson = lesson;
    this.lastActiveElement = document.activeElement;

    if (this.elements.submitModalLessonTitle) {
      this.elements.submitModalLessonTitle.textContent = lesson.title || `#${lesson.lessonId}`;
    }
    if (this.elements.submitModalLessonStatus) {
      this.elements.submitModalLessonStatus.textContent = getStatusLabel(lesson.status);
      this.elements.submitModalLessonStatus.className = getStatusBadgeClass(lesson.status);
    }
    if (this.elements.submitModalError) {
      this.elements.submitModalError.textContent = '';
    }
    if (this.elements.btnConfirmSubmitModeration) {
      this.elements.btnConfirmSubmitModeration.disabled = false;
      this.elements.btnConfirmSubmitModeration.textContent = lesson.status === 'Rejected'
        ? 'Nộp lại bài học'
        : 'Nộp kiểm duyệt';
    }

    if (typeof this.elements.submitModal.showModal === 'function') {
      this.elements.submitModal.showModal();
    } else {
      this.elements.submitModal.setAttribute('open', 'true');
    }
    if (this.elements.btnConfirmSubmitModeration) {
      this.elements.btnConfirmSubmitModeration.focus();
    }
  }

  closeSubmitModal() {
    if (!this.elements.submitModal) return;
    if (typeof this.elements.submitModal.close === 'function') {
      this.elements.submitModal.close();
    } else {
      this.elements.submitModal.removeAttribute('open');
    }
    this.pendingSubmitLesson = null;
    if (this.lastActiveElement && typeof this.lastActiveElement.focus === 'function') {
      this.lastActiveElement.focus();
    }
  }

  async handleConfirmSubmit() {
    if (!this.pendingSubmitLesson || this.isSubmitting) return;

    this.isSubmitting = true;
    if (this.elements.btnConfirmSubmitModeration) {
      this.elements.btnConfirmSubmitModeration.disabled = true;
      this.elements.btnConfirmSubmitModeration.textContent = 'Đang nộp...';
    }
    if (this.elements.submitModalError) {
      this.elements.submitModalError.textContent = '';
    }

    try {
      await apiClient(`/creator/lessons/${this.pendingSubmitLesson.lessonId}/submit`, { method: 'POST' });
      showToast({
        type: 'success',
        title: 'Nộp kiểm duyệt',
        message: 'Nộp bài học kiểm duyệt thành công!'
      });
      this.closeSubmitModal();
      this.loadLessons();
    } catch (err) {
      console.warn('[CreatorLessons] Submit error:', err);
      const msg = err.message || 'Không thể nộp bài học kiểm duyệt.';
      if (err.status === 409 || err.code === 'CONFLICT') {
        if (this.elements.submitModalError) {
          this.elements.submitModalError.textContent = 'Trạng thái bài học đã thay đổi trước đó. Đang làm mới danh sách...';
        }
        showToast({
          type: 'warning',
          title: 'Xung đột trạng thái',
          message: msg
        });
        setTimeout(() => {
          this.closeSubmitModal();
          this.loadLessons();
        }, 1200);
      } else {
        if (this.elements.submitModalError) {
          this.elements.submitModalError.textContent = msg;
        }
        showToast({
          type: 'danger',
          title: 'Lỗi nộp bài',
          message: msg
        });
      }
    } finally {
      this.isSubmitting = false;
      if (this.elements.btnConfirmSubmitModeration) {
        this.elements.btnConfirmSubmitModeration.disabled = false;
        this.elements.btnConfirmSubmitModeration.textContent = this.pendingSubmitLesson?.status === 'Rejected'
          ? 'Nộp lại bài học'
          : 'Nộp kiểm duyệt';
      }
    }
  }

  async handleCreateLessonSubmit() {
    if (this.isCreating) return;

    const rawTitle = this.elements.newLessonTitleInput ? this.elements.newLessonTitleInput.value : '';
    const validation = validateCreateLessonTitle(rawTitle);

    if (!validation.valid) {
      if (this.elements.createTitleError) {
        this.elements.createTitleError.textContent = validation.error;
      }
      if (this.elements.newLessonTitleInput) {
        this.elements.newLessonTitleInput.focus();
      }
      return;
    }

    this.isCreating = true;
    if (this.elements.btnSubmitCreateLesson) {
      this.elements.btnSubmitCreateLesson.disabled = true;
      this.elements.btnSubmitCreateLesson.textContent = 'Đang tạo bài học...';
    }

    try {
      // Send minimal, clean payload matching CreateLessonRequest
      const payload = {
        title: validation.sanitizedTitle,
        vocabularyIds: []
      };

      const response = await apiClient('/creator/lessons', {
        method: 'POST',
        body: payload
      });

      const lessonDetail = response?.data || response;
      const createdLessonId = lessonDetail?.lessonId;

      if (!createdLessonId) {
        throw new Error('Máy chủ không trả về mã bài học hợp lệ.');
      }

      showToast({
        type: 'success',
        title: 'Thành công',
        message: 'Tạo bài học mới thành công. Đang chuyển tới phòng biên soạn...'
      });

      this.closeCreateModal();

      // Navigate directly to lesson editor with new ID
      window.location.href = `creator-lesson-editor.html?id=${createdLessonId}`;
    } catch (err) {
      console.warn('[CreatorLessons] Creation failed:', err);
      const errMsg = err.message || 'Không thể tạo bài học. Vui lòng kiểm tra lại kết nối.';
      if (this.elements.createTitleError) {
        this.elements.createTitleError.textContent = errMsg;
      }
      showToast({
        type: 'danger',
        title: 'Lỗi tạo bài học',
        message: errMsg
      });
    } finally {
      this.isCreating = false;
      if (this.elements.btnSubmitCreateLesson) {
        this.elements.btnSubmitCreateLesson.disabled = false;
        this.elements.btnSubmitCreateLesson.textContent = 'Tạo và biên soạn →';
      }
    }
  }

  async loadLessons() {
    if (!this.elements.stateContainer) return;

    // 1. Show loading state
    showLoading(this.elements.stateContainer, { message: 'Đang tải danh sách bài học của bạn...' });
    if (this.elements.tableContainer) this.elements.tableContainer.classList.add('d-none');
    if (this.elements.paginationNav) this.elements.paginationNav.classList.add('d-none');
    if (this.elements.resultCountBadge) this.elements.resultCountBadge.textContent = 'Đang tải...';

    try {
      const response = await apiClient('/creator/lessons', {
        params: {
          page: this.currentPage,
          size: this.pageSize
        }
      });

      const parsed = parseLessonSummaries(response);
      this.currentPage = parsed.page;
      this.pageSize = parsed.size;
      this.totalElements = parsed.totalElements;
      this.totalPages = parsed.totalPages;

      clearContainer(this.elements.stateContainer);

      if (parsed.totalElements === 0) {
        // Empty state
        if (this.elements.resultCountBadge) {
          this.elements.resultCountBadge.textContent = '0 bài học';
          this.elements.resultCountBadge.className = 'badge-status badge-status-draft text-nowrap';
        }

        showEmpty(this.elements.stateContainer, {
          title: 'Chưa có bài học nào',
          description: 'Bạn chưa tạo bài học nào. Hãy nhấn "Tạo bài học mới" để bắt đầu biên soạn giáo trình học tập của bạn.'
        });
        return;
      }

      // Update count badge
      if (this.elements.resultCountBadge) {
        this.elements.resultCountBadge.textContent = `${parsed.totalElements} bài học`;
        this.elements.resultCountBadge.className = 'badge-status badge-status-approved text-nowrap';
      }

      // Render table rows
      this.renderTableRows(parsed.items);

      // Render pagination
      this.renderPagination(parsed.page, parsed.size, parsed.totalElements, parsed.totalPages);

      if (this.elements.tableContainer) this.elements.tableContainer.classList.remove('d-none');
    } catch (err) {
      console.warn('[CreatorLessons] Load failed:', err);
      clearContainer(this.elements.stateContainer);

      if (this.elements.resultCountBadge) {
        this.elements.resultCountBadge.textContent = 'Lỗi nạp dữ liệu';
        this.elements.resultCountBadge.className = 'badge-status badge-status-rejected text-nowrap';
      }

      showError(this.elements.stateContainer, {
        message: err.message || 'Không thể tải danh sách bài học từ máy chủ.',
        onRetry: () => this.loadLessons()
      });
    }
  }

  renderTableRows(items) {
    if (!this.elements.tableBody) return;
    clearContainer(this.elements.tableBody);

    for (const item of items) {
      const tr = createSafeElement('tr', {
        className: 'creator-lesson-row',
        children: [
          // ID
          createSafeElement('td', {
            className: 'text-center font-monospace small text-muted',
            text: `#${item.lessonId}`
          }),

          // Title
          createSafeElement('td', {
            children: [
              createSafeElement('span', {
                className: 'fw-bold text-dark d-block',
                text: item.title || 'Không có tiêu đề'
              })
            ]
          }),

          // Status Badge
          createSafeElement('td', {
            className: 'text-center',
            children: [
              createSafeElement('span', {
                className: getStatusBadgeClass(item.status),
                text: getStatusLabel(item.status)
              })
            ]
          }),

          // Vocabulary count
          createSafeElement('td', {
            className: 'text-center',
            children: [
              createSafeElement('span', {
                className: 'badge rounded-pill bg-light text-dark border',
                text: `${item.vocabularyCount || 0} từ`
              })
            ]
          }),

          // Updated date
          createSafeElement('td', {
            className: 'text-center small text-secondary',
            text: formatLessonDate(item.updatedAt || item.createdAt)
          }),

          // Actions
          createSafeElement('td', {
            className: 'text-end pe-3',
            children: [
              createSafeElement('div', {
                className: 'd-flex align-items-center justify-content-end gap-2 flex-wrap',
                children: [
                  ...(canSubmitLesson(item.status, item.vocabularyCount).canSubmit ? [
                    (() => {
                      const cfg = getSubmitButtonConfig(item.status);
                      const btn = createSafeElement('button', {
                        className: cfg?.className || 'btn-chinese-primary py-1 px-2 btn-sm text-decoration-none',
                        text: cfg?.label || 'Nộp duyệt',
                        attrs: {
                          type: 'button',
                          'data-lesson-id': String(item.lessonId),
                          'aria-label': `${cfg?.actionText || 'Nộp kiểm duyệt'}: ${item.title}`
                        }
                      });
                      btn.addEventListener('click', () => {
                        this.openSubmitModal(item);
                      });
                      return btn;
                    })()
                  ] : []),
                  createSafeElement('a', {
                    className: 'btn-chinese-outline py-1 px-3 btn-sm text-decoration-none',
                    text: (item.status === 'Draft' || item.status === 'Rejected') ? 'Biên soạn' : 'Xem chi tiết',
                    attrs: {
                      href: `creator-lesson-editor.html?id=${item.lessonId}`,
                      'aria-label': `Biên soạn bài học ${item.title}`
                    }
                  })
                ]
              })
            ]
          })
        ]
      });

      this.elements.tableBody.appendChild(tr);
    }
  }

  renderPagination(page, size, totalElements, totalPages) {
    if (!this.elements.paginationNav || !this.elements.paginationList) return;

    if (totalPages <= 1) {
      this.elements.paginationNav.classList.add('d-none');
      return;
    }

    this.elements.paginationNav.classList.remove('d-none');
    clearContainer(this.elements.paginationList);

    const calc = calculatePagination(page, size, totalElements, totalPages);

    if (this.elements.paginationSummary) {
      this.elements.paginationSummary.textContent = `Hiển thị ${calc.startRecord}–${calc.endRecord} / ${totalElements} bài học`;
    }

    // Previous button
    const prevLi = createSafeElement('li', {
      className: `page-item ${!calc.hasPrev ? 'disabled' : ''}`,
      children: [
        createSafeElement('button', {
          className: 'page-link',
          text: '‹ Trước',
          attrs: {
            type: 'button',
            'aria-label': 'Trang trước',
            ...(calc.hasPrev ? {} : { disabled: 'true' })
          }
        })
      ]
    });
    if (calc.hasPrev) {
      prevLi.querySelector('button').addEventListener('click', () => {
        this.currentPage = calc.currentPage - 1;
        this.loadLessons();
      });
    }
    this.elements.paginationList.appendChild(prevLi);

    // Page windowing
    const maxButtons = 5;
    let startPage = Math.max(0, calc.currentPage - Math.floor(maxButtons / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxButtons - 1);
    if (endPage - startPage < maxButtons - 1) {
      startPage = Math.max(0, endPage - maxButtons + 1);
    }

    for (let p = startPage; p <= endPage; p++) {
      const isCurrent = p === calc.currentPage;
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
        pageLi.querySelector('button').addEventListener('click', () => {
          this.currentPage = p;
          this.loadLessons();
        });
      }
      this.elements.paginationList.appendChild(pageLi);
    }

    // Next button
    const nextLi = createSafeElement('li', {
      className: `page-item ${!calc.hasNext ? 'disabled' : ''}`,
      children: [
        createSafeElement('button', {
          className: 'page-link',
          text: 'Sau ›',
          attrs: {
            type: 'button',
            'aria-label': 'Trang sau',
            ...(calc.hasNext ? {} : { disabled: 'true' })
          }
        })
      ]
    });
    if (calc.hasNext) {
      nextLi.querySelector('button').addEventListener('click', () => {
        this.currentPage = calc.currentPage + 1;
        this.loadLessons();
      });
    }
    this.elements.paginationList.appendChild(nextLi);
  }
}

// Auto-boot in browser environment if on creator-lessons.html
if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('creatorLessonsTableContainer')) {
      const controller = new CreatorLessonsPageController();
      controller.init();
    }
  });
}
