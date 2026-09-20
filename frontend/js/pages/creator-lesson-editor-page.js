/**
 * =============================================================================
 * CREATOR LESSON EDITOR PAGE CONTROLLER (TASK 9D.1)
 * Module: frontend/js/pages/creator-lesson-editor-page.js
 * 
 * Responsibilities:
 * - Reads and validates lessonId from URL query parameter (?id=...).
 * - Loads authoritative lesson detail from GET /api/v1/creator/lessons/{id}.
 * - Enforces editable state lifecycle:
 *   - 'Draft' & 'Rejected': Editable (title, add, remove, reorder).
 *   - 'Pending' & 'Approved': Read-only (locked banners, disabled mutations).
 * - Enforces Creator ownership isolation with Admin bypass support.
 * - Title editing via PUT /api/v1/creator/lessons/{id}.
 * - Vocabulary search reusing GET /api/v1/vocabulary?search=<term>.
 * - Add vocabulary via POST /api/v1/creator/lessons/{id}/vocabularies/{vocabId}.
 * - Remove vocabulary via DELETE /api/v1/creator/lessons/{id}/vocabularies/{vocabId}.
 * - Reorder vocabulary via PUT /api/v1/creator/lessons/{id}/reorder with orderedVocabIds.
 * - Accessible native button reordering (Move Up / Move Down) satisfying WCAG 2.2 SC 2.5.7.
 * - Mutation-lock concurrency protection preventing conflicting or duplicate requests.
 * - Safe DOM construction (zero unsafe innerHTML) defending against XSS.
 * =============================================================================
 */

import { apiClient } from '../api/api.js';
import { authManager } from '../auth/auth-state.js';
import { initNavbarAuth } from '../ui/nav.js';
import { createSafeElement, clearContainer } from '../ui/security.js';
import { showLoading, showError, showToast } from '../ui/ui.js';
import { getStatusBadgeClass, getStatusLabel, canSubmitLesson, getSubmitButtonConfig } from './creator-lessons-page.js';

export { canSubmitLesson, getSubmitButtonConfig };

/* -----------------------------------------------------------------------------
 * 1. PURE LOGIC & UTILITY HELPERS (Exported for Testability)
 * ----------------------------------------------------------------------------- */

/**
 * Validates the raw lesson ID string from URL parameter.
 * Must be a positive integer.
 * 
 * @param {string|number|null|undefined} idParam 
 * @returns {number|null} Valid numeric ID, or null if invalid/missing
 */
export function validateLessonId(idParam) {
  if (idParam === null || idParam === undefined) {
    return null;
  }
  const str = String(idParam).trim();
  if (!/^\d+$/.test(str)) {
    return null;
  }
  const num = parseInt(str, 10);
  return num > 0 && Number.isSafeInteger(num) ? num : null;
}

/**
 * Checks whether a lesson is in an editable lifecycle state.
 * Backend contract: 'Draft' and 'Rejected' are editable; 'Pending' and 'Approved' are locked.
 * 
 * @param {string} status 
 * @returns {boolean}
 */
export function isLessonEditable(status) {
  if (!status || typeof status !== 'string') return false;
  const s = status.trim().toLowerCase();
  return s === 'draft' || s === 'rejected';
}

/**
 * Validates lesson title for metadata updates.
 * Rules: non-empty, non-whitespace, maximum 200 characters.
 * 
 * @param {string} title 
 * @returns {{ valid: boolean, error?: string, sanitizedTitle: string }}
 */
export function validateLessonTitle(title) {
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
 * Builds the canonical reorder payload for PUT /creator/lessons/{id}/reorder.
 * Contract: strictly { orderedVocabIds: [id1, id2, ...] }.
 * 
 * @param {Array<{ vocabId: number }>} vocabList 
 * @returns {{ orderedVocabIds: number[] }}
 */
export function buildReorderPayload(vocabList) {
  if (!Array.isArray(vocabList)) {
    return { orderedVocabIds: [] };
  }
  const orderedVocabIds = vocabList
    .map(v => Number(v?.vocabId))
    .filter(id => Number.isInteger(id) && id > 0);
  return { orderedVocabIds };
}

/**
 * Pure helper to swap items in a list for reordering.
 * Boundary protected: returns shallow copy without mutation if out of bounds.
 * 
 * @param {Array<any>} list 
 * @param {number} fromIndex 
 * @param {number} toIndex 
 * @returns {Array<any>}
 */
export function moveVocabItem(list, fromIndex, toIndex) {
  if (!Array.isArray(list)) return [];
  const copy = [...list];
  if (
    fromIndex < 0 ||
    fromIndex >= copy.length ||
    toIndex < 0 ||
    toIndex >= copy.length ||
    fromIndex === toIndex
  ) {
    return copy;
  }
  const item = copy.splice(fromIndex, 1)[0];
  copy.splice(toIndex, 0, item);
  return copy;
}

/**
 * Checks whether a vocabulary ID already exists in the lesson's vocabulary array.
 * 
 * @param {Array<{ vocabId: number }>} vocabList 
 * @param {number} vocabId 
 * @returns {boolean}
 */
export function isVocabInLesson(vocabList, vocabId) {
  if (!Array.isArray(vocabList) || !vocabId) return false;
  return vocabList.some(item => Number(item?.vocabId) === Number(vocabId));
}

/**
 * Safely parses backend LessonDetailResponse payload and ensures orderIndex ascending.
 * 
 * @param {any} envelopeOrData 
 * @returns {{
 *   lessonId: number,
 *   title: string,
 *   status: string,
 *   vocabularyCount: number,
 *   vocabularies: Array<any>,
 *   createdAt: string,
 *   updatedAt: string
 * }|null}
 */
export function parseLessonDetail(envelopeOrData) {
  if (!envelopeOrData || typeof envelopeOrData !== 'object') {
    return null;
  }

  const data = (envelopeOrData.data && typeof envelopeOrData.data === 'object')
    ? envelopeOrData.data
    : envelopeOrData;

  const rawVocabs = Array.isArray(data.vocabularies) ? [...data.vocabularies] : [];
  rawVocabs.sort((a, b) => (Number(a?.orderIndex) || 0) - (Number(b?.orderIndex) || 0));

  return {
    lessonId: Number(data.lessonId) || 0,
    title: (typeof data.title === 'string') ? data.title.trim() : '',
    status: data.status || 'Draft',
    vocabularyCount: typeof data.vocabularyCount === 'number' ? data.vocabularyCount : rawVocabs.length,
    vocabularies: rawVocabs,
    rejectionReason: (typeof data.rejectionReason === 'string') ? data.rejectionReason.trim() : '',
    flaggedFields: data.flaggedFields || [],
    createdAt: data.createdAt || '',
    updatedAt: data.updatedAt || ''
  };
}

/**
 * Parses rejection feedback from lesson detail data.
 * Note: As of Task 9D.3, backend LessonDetailResponse does not expose rejectionReason or flaggedFields.
 * This parser supports forward-compatibility if they are provided, and returns truthful fallback otherwise.
 * 
 * @param {any} lessonData 
 * @returns {{
 *   hasFeedback: boolean,
 *   isContractBlocked: boolean,
 *   rejectionReason: string,
 *   flaggedFields: string[],
 *   displayMessage: string
 * }}
 */
export function parseRejectionFeedback(lessonData) {
  const status = String(lessonData?.status || '').trim().toLowerCase();
  if (status !== 'rejected') {
    return {
      hasFeedback: false,
      isContractBlocked: false,
      rejectionReason: '',
      flaggedFields: [],
      displayMessage: ''
    };
  }

  const rawReason = typeof lessonData?.rejectionReason === 'string' ? lessonData.rejectionReason.trim() : '';
  let fields = [];
  if (Array.isArray(lessonData?.flaggedFields)) {
    fields = lessonData.flaggedFields.map(f => String(f).trim()).filter(Boolean);
  } else if (typeof lessonData?.flaggedFields === 'string') {
    try {
      const parsed = JSON.parse(lessonData.flaggedFields);
      if (Array.isArray(parsed)) {
        fields = parsed.map(f => String(f).trim()).filter(Boolean);
      } else {
        fields = lessonData.flaggedFields.split(',').map(s => s.trim()).filter(Boolean);
      }
    } catch (_) {
      fields = lessonData.flaggedFields.split(',').map(s => s.trim()).filter(Boolean);
    }
  }

  if (rawReason) {
    return {
      hasFeedback: true,
      isContractBlocked: false,
      rejectionReason: rawReason,
      flaggedFields: fields,
      displayMessage: rawReason
    };
  }

  return {
    hasFeedback: false,
    isContractBlocked: true,
    rejectionReason: '',
    flaggedFields: [],
    displayMessage: 'Bài học này đã bị từ chối. Thông tin phản hồi chi tiết chưa được cung cấp bởi API hiện tại.'
  };
}

/* -----------------------------------------------------------------------------
 * 2. PAGE CONTROLLER CLASS
 * ----------------------------------------------------------------------------- */

export class CreatorLessonEditorController {
  constructor() {
    this.lessonId = null;
    this.currentLesson = null;
    this.isMutating = false;
    this.isSubmitting = false;
    this.lastActiveElement = null;

    // Search state
    this.searchDebounceTimer = null;
    this.searchDebounceDelayMs = 300;
    this.activeSearchQuery = '';

    this.elements = {};
  }

  init() {
    initNavbarAuth();
    this.bindDomElements();
    this.bindEvents();

    // 1. Auth guard
    if (!authManager.isAuthenticated()) {
      window.location.href = 'login.html?redirect=creator-lessons.html';
      return;
    }

    const hasRole = authManager.hasRole('Creator') || authManager.hasRole('Admin');
    if (!hasRole) {
      this.renderAccessDenied('Chỉ Tác giả (Creator) hoặc Quản trị viên (Admin) mới có quyền truy cập phòng biên soạn bài học.');
      return;
    }

    // 2. Validate URL parameter
    const urlParams = new URLSearchParams(window.location.search);
    const idParam = urlParams.get('id');
    const validId = validateLessonId(idParam);

    if (!validId) {
      this.renderInvalidLessonId();
      return;
    }

    this.lessonId = validId;
    this.loadLessonDetail();
  }

  bindDomElements() {
    this.elements = {
      breadcrumbTitle: document.getElementById('editorBreadcrumbTitle'),
      stateContainer: document.getElementById('editorStateContainer'),
      workspace: document.getElementById('editorWorkspace'),
      lockedBanner: document.getElementById('editorLockedBanner'),
      lockedBannerTitle: document.getElementById('editorLockedBannerTitle'),
      lockedBannerDesc: document.getElementById('editorLockedBannerDesc'),
      editorRejectedBanner: document.getElementById('editorRejectedBanner'),
      editorRejectedBannerContent: document.getElementById('editorRejectedBannerContent'),
      btnSubmitForModeration: document.getElementById('btnSubmitForModeration'),
      statusBadge: document.getElementById('editorStatusBadge'),
      vocabCountBadge: document.getElementById('editorVocabCountBadge'),
      titleForm: document.getElementById('editorTitleForm'),
      titleInput: document.getElementById('editorTitleInput'),
      titleError: document.getElementById('editorTitleError'),
      titleCounter: document.getElementById('editorTitleCounter'),
      btnSaveTitle: document.getElementById('btnSaveTitle'),
      addVocabPanel: document.getElementById('editorAddVocabPanel'),
      vocabSearchInput: document.getElementById('editorVocabSearchInput'),
      btnSearchClear: document.getElementById('btnEditorSearchClear'),
      searchResults: document.getElementById('editorSearchResults'),
      reorderLiveStatus: document.getElementById('reorderLiveStatus'),
      emptyVocabState: document.getElementById('editorEmptyVocabState'),
      vocabList: document.getElementById('editorVocabList'),
      // Task 9D.3 submit modal
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
    // 1. Title input live counter
    if (this.elements.titleInput) {
      this.elements.titleInput.addEventListener('input', (e) => {
        const val = e.target.value;
        if (this.elements.titleCounter) {
          this.elements.titleCounter.textContent = `${val.length}/200`;
        }
        if (this.elements.titleError) {
          this.elements.titleError.textContent = '';
        }
      });
    }

    // 2. Title save submission
    if (this.elements.titleForm) {
      this.elements.titleForm.addEventListener('submit', (e) => {
        e.preventDefault();
        this.handleSaveTitle();
      });
    }

    // 3. Submit for moderation button
    if (this.elements.btnSubmitForModeration) {
      this.elements.btnSubmitForModeration.addEventListener('click', () => {
        this.openSubmitModal();
      });
    }

    // 4. Submit modal cancel/dismiss buttons
    const closeSubmitHandler = () => this.closeSubmitModal();
    if (this.elements.btnCancelSubmitModal) {
      this.elements.btnCancelSubmitModal.addEventListener('click', closeSubmitHandler);
    }
    if (this.elements.btnDismissSubmitModal) {
      this.elements.btnDismissSubmitModal.addEventListener('click', closeSubmitHandler);
    }

    // 5. Submit modal confirm submission
    if (this.elements.btnConfirmSubmitModeration) {
      this.elements.btnConfirmSubmitModeration.addEventListener('click', () => {
        this.handleConfirmSubmit();
      });
    }

    // 6. Submit modal native Escape handling
    if (this.elements.submitModal) {
      this.elements.submitModal.addEventListener('cancel', () => {
        this.closeSubmitModal();
      });
    }

    // 3. Vocabulary search input with debounce
    if (this.elements.vocabSearchInput) {
      this.elements.vocabSearchInput.addEventListener('input', (e) => {
        this.handleSearchInput(e.target.value);
      });

      this.elements.vocabSearchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
          this.closeSearchResults();
        }
      });
    }

    // 4. Clear search button
    if (this.elements.btnSearchClear) {
      this.elements.btnSearchClear.addEventListener('click', () => {
        if (this.elements.vocabSearchInput) {
          this.elements.vocabSearchInput.value = '';
          this.elements.vocabSearchInput.focus();
        }
        this.closeSearchResults();
      });
    }

    // Close search results when clicking outside
    document.addEventListener('click', (e) => {
      if (
        this.elements.searchResults &&
        !this.elements.searchResults.contains(e.target) &&
        e.target !== this.elements.vocabSearchInput
      ) {
        this.closeSearchResults();
      }
    });
  }

  renderInvalidLessonId() {
    if (!this.elements.stateContainer) return;
    clearContainer(this.elements.stateContainer);
    showError(this.elements.stateContainer, {
      title: 'Mã bài học không hợp lệ',
      message: 'Mã bài học không hợp lệ hoặc bị thiếu. Vui lòng quay lại danh sách bài học để chọn bài học cần biên soạn.',
      retryText: 'Về danh sách bài học',
      onRetry: () => { window.location.href = 'creator-lessons.html'; }
    });
  }

  renderAccessDenied(message) {
    if (!this.elements.stateContainer) return;
    clearContainer(this.elements.stateContainer);
    showError(this.elements.stateContainer, {
      title: 'Không có quyền truy cập',
      message: message || 'Bạn không có quyền truy cập hoặc biên soạn bài học này.',
      retryText: 'Về danh sách bài học',
      onRetry: () => { window.location.href = 'creator-lessons.html'; }
    });
  }

  async loadLessonDetail() {
    if (!this.elements.stateContainer) return;

    showLoading(this.elements.stateContainer, { message: 'Đang tải thông tin bài học và danh sách từ vựng...' });
    if (this.elements.workspace) this.elements.workspace.classList.add('d-none');

    try {
      const response = await apiClient(`/creator/lessons/${this.lessonId}`);
      const detail = parseLessonDetail(response);

      if (!detail) {
        throw new Error('Dữ liệu bài học không hợp lệ.');
      }

      this.currentLesson = detail;
      clearContainer(this.elements.stateContainer);
      if (this.elements.workspace) this.elements.workspace.classList.remove('d-none');

      this.renderLessonState();
    } catch (err) {
      console.warn('[CreatorLessonEditor] Load failed:', err);
      clearContainer(this.elements.stateContainer);

      if (err.status === 403) {
        this.renderAccessDenied('Bạn không có quyền thao tác trên bài học này (bài học thuộc về tác giả khác).');
        return;
      }
      if (err.status === 404) {
        showError(this.elements.stateContainer, {
          title: 'Không tìm thấy bài học',
          message: 'Không tìm thấy bài học tương ứng trên hệ thống.',
          retryText: 'Về danh sách bài học',
          onRetry: () => { window.location.href = 'creator-lessons.html'; }
        });
        return;
      }

      showError(this.elements.stateContainer, {
        message: err.message || 'Không thể tải chi tiết bài học từ máy chủ.',
        onRetry: () => this.loadLessonDetail()
      });
    }
  }

  renderLessonState() {
    if (!this.currentLesson) return;

    const { title, status, vocabularies } = this.currentLesson;
    const editable = isLessonEditable(status);

    // 1. Breadcrumb
    if (this.elements.breadcrumbTitle) {
      this.elements.breadcrumbTitle.textContent = title || `Bài học #${this.lessonId}`;
    }

    // 2. Status Badge
    if (this.elements.statusBadge) {
      this.elements.statusBadge.className = getStatusBadgeClass(status);
      this.elements.statusBadge.textContent = getStatusLabel(status);
    }

    // 3. Vocab Count Badge
    if (this.elements.vocabCountBadge) {
      this.elements.vocabCountBadge.textContent = `${vocabularies.length} từ vựng`;
    }

    // 4. Locked Banner Presentation
    if (this.elements.lockedBanner) {
      if (!editable) {
        this.elements.lockedBanner.classList.remove('d-none');
        if (status === 'Approved') {
          this.elements.lockedBanner.className = 'creator-locked-banner creator-locked-banner-approved';
          if (this.elements.lockedBannerTitle) {
            this.elements.lockedBannerTitle.textContent = 'Bài học đã được Phê duyệt (Approved).';
          }
          if (this.elements.lockedBannerDesc) {
            this.elements.lockedBannerDesc.textContent = 'Nội dung bài học đã được kiểm duyệt chính thức và phát hành. Mọi thao tác chỉnh sửa, thêm/xóa/sắp xếp từ vựng đều bị khóa an toàn.';
          }
        } else {
          this.elements.lockedBanner.className = 'creator-locked-banner';
          if (this.elements.lockedBannerTitle) {
            this.elements.lockedBannerTitle.textContent = 'Bài học đang Chờ kiểm duyệt (Pending).';
          }
          if (this.elements.lockedBannerDesc) {
            this.elements.lockedBannerDesc.textContent = 'Bài học đang nằm trong hàng đợi kiểm duyệt. Để bảo toàn tính toàn vẹn dữ liệu thẩm định, bài học không thể sửa đổi trong thời gian này.';
          }
        }
      } else {
        this.elements.lockedBanner.classList.add('d-none');
      }
    }

    // 5. Title form controls
    if (this.elements.titleInput) {
      this.elements.titleInput.value = title || '';
      this.elements.titleInput.disabled = !editable;
      if (this.elements.titleCounter) {
        this.elements.titleCounter.textContent = `${(title || '').length}/200`;
      }
    }
    if (this.elements.btnSaveTitle) {
      this.elements.btnSaveTitle.disabled = !editable;
    }

    // 6. Vocabulary Add panel visibility
    if (this.elements.addVocabPanel) {
      if (editable) {
        this.elements.addVocabPanel.classList.remove('d-none');
      } else {
        this.elements.addVocabPanel.classList.add('d-none');
      }
    }

    // 7. Rejected Feedback Banner
    if (this.elements.editorRejectedBanner) {
      if (status === 'Rejected') {
        this.elements.editorRejectedBanner.classList.remove('d-none');
        const feedback = parseRejectionFeedback(this.currentLesson);
        if (this.elements.editorRejectedBannerContent) {
          clearContainer(this.elements.editorRejectedBannerContent);
          if (feedback.hasFeedback) {
            const reasonP = createSafeElement('p', {
              className: 'mb-1 fw-semibold text-danger',
              text: `Lý do từ chối: ${feedback.rejectionReason}`
            });
            this.elements.editorRejectedBannerContent.appendChild(reasonP);
            if (feedback.flaggedFields.length > 0) {
              const fieldsP = createSafeElement('p', {
                className: 'mb-0 small text-dark',
                text: `Các trường cần sửa đổi: ${feedback.flaggedFields.join(', ')}`
              });
              this.elements.editorRejectedBannerContent.appendChild(fieldsP);
            }
          } else {
            const fallbackP = createSafeElement('p', {
              className: 'mb-0',
              text: feedback.displayMessage
            });
            this.elements.editorRejectedBannerContent.appendChild(fallbackP);
          }
        }
      } else {
        this.elements.editorRejectedBanner.classList.add('d-none');
      }
    }

    // 8. Submit for Moderation Button
    if (this.elements.btnSubmitForModeration) {
      if (editable) {
        this.elements.btnSubmitForModeration.classList.remove('d-none');
        const isRejected = status === 'Rejected';
        this.elements.btnSubmitForModeration.textContent = isRejected ? 'Nộp lại bài học' : 'Nộp kiểm duyệt';
        const vocabCount = vocabularies.length;
        if (vocabCount === 0) {
          this.elements.btnSubmitForModeration.disabled = true;
          this.elements.btnSubmitForModeration.title = 'Bài học phải chứa ít nhất một từ vựng trước khi nộp kiểm duyệt';
        } else {
          this.elements.btnSubmitForModeration.disabled = false;
          this.elements.btnSubmitForModeration.title = isRejected ? 'Nộp lại bài học để kiểm duyệt' : 'Nộp bài học để kiểm duyệt';
        }
      } else {
        this.elements.btnSubmitForModeration.classList.add('d-none');
      }
    }

    // 9. Render ordered vocabulary items
    this.renderVocabList();
  }

  openSubmitModal() {
    if (!this.elements.submitModal || !this.currentLesson) return;

    const vocabCount = (this.currentLesson.vocabularies || []).length;
    const check = canSubmitLesson(this.currentLesson.status, vocabCount);
    if (!check.canSubmit) {
      showToast({
        type: 'warning',
        title: 'Chưa đủ điều kiện',
        message: check.reason || 'Bài học chưa đủ điều kiện để nộp kiểm duyệt'
      });
      return;
    }

    this.lastActiveElement = document.activeElement;

    if (this.elements.submitModalLessonTitle) {
      this.elements.submitModalLessonTitle.textContent = this.currentLesson.title || `#${this.lessonId}`;
    }
    if (this.elements.submitModalLessonStatus) {
      this.elements.submitModalLessonStatus.textContent = getStatusLabel(this.currentLesson.status);
      this.elements.submitModalLessonStatus.className = getStatusBadgeClass(this.currentLesson.status);
    }
    if (this.elements.submitModalError) {
      this.elements.submitModalError.textContent = '';
    }
    if (this.elements.btnConfirmSubmitModeration) {
      this.elements.btnConfirmSubmitModeration.disabled = false;
      this.elements.btnConfirmSubmitModeration.textContent = this.currentLesson.status === 'Rejected'
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
    if (this.lastActiveElement && typeof this.lastActiveElement.focus === 'function') {
      this.lastActiveElement.focus();
    }
  }

  async handleConfirmSubmit() {
    if (!this.currentLesson || this.isSubmitting) return;

    this.isSubmitting = true;
    if (this.elements.btnConfirmSubmitModeration) {
      this.elements.btnConfirmSubmitModeration.disabled = true;
      this.elements.btnConfirmSubmitModeration.textContent = 'Đang nộp...';
    }
    if (this.elements.submitModalError) {
      this.elements.submitModalError.textContent = '';
    }

    try {
      const res = await apiClient(`/creator/lessons/${this.lessonId}/submit`, { method: 'POST' });
      showToast({
        type: 'success',
        title: 'Nộp kiểm duyệt',
        message: 'Nộp bài học kiểm duyệt thành công!'
      });
      this.closeSubmitModal();
      const updated = parseLessonDetail(res);
      if (updated) {
        this.currentLesson = updated;
        this.renderLessonState();
      } else {
        this.loadLessonDetail();
      }
    } catch (err) {
      console.warn('[CreatorLessonEditor] Submit error:', err);
      const msg = err.message || 'Không thể nộp bài học kiểm duyệt.';
      if (err.status === 409 || err.code === 'CONFLICT') {
        if (this.elements.submitModalError) {
          this.elements.submitModalError.textContent = 'Trạng thái bài học đã thay đổi trước đó. Đang tải lại bài học...';
        }
        showToast({
          type: 'warning',
          title: 'Xung đột trạng thái',
          message: msg
        });
        setTimeout(() => {
          this.closeSubmitModal();
          this.loadLessonDetail();
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
        this.elements.btnConfirmSubmitModeration.textContent = this.currentLesson?.status === 'Rejected'
          ? 'Nộp lại bài học'
          : 'Nộp kiểm duyệt';
      }
    }
  }

  renderVocabList() {
    if (!this.elements.vocabList) return;
    clearContainer(this.elements.vocabList);

    const vocabularies = this.currentLesson?.vocabularies || [];
    const editable = isLessonEditable(this.currentLesson?.status);

    if (vocabularies.length === 0) {
      if (this.elements.emptyVocabState) {
        this.elements.emptyVocabState.classList.remove('d-none');
      }
      return;
    }

    if (this.elements.emptyVocabState) {
      this.elements.emptyVocabState.classList.add('d-none');
    }

    const total = vocabularies.length;

    vocabularies.forEach((vocab, index) => {
      const isFirst = index === 0;
      const isLast = index === total - 1;

      const li = createSafeElement('li', {
        className: 'creator-vocab-item',
        attrs: {
          'data-vocab-id': String(vocab.vocabId),
          'data-index': String(index)
        },
        children: [
          // Left: Order badge + Vocabulary content
          createSafeElement('div', {
            className: 'd-flex align-items-center gap-3 flex-wrap',
            children: [
              // Order badge
              createSafeElement('span', {
                className: 'creator-order-badge',
                text: `#${index + 1}`
              }),

              // Hanzi
              createSafeElement('span', {
                className: 'creator-vocab-hanzi',
                text: vocab.hanzi || '—'
              }),

              // Pinyin & Meanings
              createSafeElement('div', {
                children: [
                  createSafeElement('div', {
                    className: 'd-flex align-items-center gap-2 flex-wrap',
                    children: [
                      createSafeElement('span', {
                        className: 'fw-semibold text-primary font-monospace',
                        text: vocab.pinyin || vocab.pinyinRaw || ''
                      }),
                      createSafeElement('span', {
                        className: 'badge bg-light text-secondary border small',
                        text: vocab.meaningHanViet ? `Hán-Việt: ${vocab.meaningHanViet}` : ''
                      })
                    ]
                  }),
                  createSafeElement('div', {
                    className: 'small text-dark mt-1',
                    text: vocab.meaningVi || '—'
                  })
                ]
              })
            ]
          }),

          // Right: Reorder & Remove controls
          createSafeElement('div', {
            className: 'creator-reorder-actions',
            children: [
              // Move Up Button
              createSafeElement('button', {
                className: 'creator-reorder-btn btn-move-up',
                attrs: {
                  type: 'button',
                  'aria-label': `Di chuyển từ vựng '${vocab.hanzi}' lên trên`,
                  title: 'Di chuyển lên',
                  ...(editable && !isFirst && !this.isMutating ? {} : { disabled: 'true' })
                },
                children: [
                  createSafeElement('span', {
                    attrs: { 'aria-hidden': 'true' },
                    text: '▲'
                  })
                ]
              }),

              // Move Down Button
              createSafeElement('button', {
                className: 'creator-reorder-btn btn-move-down',
                attrs: {
                  type: 'button',
                  'aria-label': `Di chuyển từ vựng '${vocab.hanzi}' xuống dưới`,
                  title: 'Di chuyển xuống',
                  ...(editable && !isLast && !this.isMutating ? {} : { disabled: 'true' })
                },
                children: [
                  createSafeElement('span', {
                    attrs: { 'aria-hidden': 'true' },
                    text: '▼'
                  })
                ]
              }),

              // Remove Button
              createSafeElement('button', {
                className: 'creator-remove-btn ms-2',
                text: 'Xóa',
                attrs: {
                  type: 'button',
                  'aria-label': `Xóa từ vựng '${vocab.hanzi}' khỏi bài học`,
                  title: 'Xóa từ vựng',
                  ...(editable && !this.isMutating ? {} : { disabled: 'true' })
                }
              })
            ]
          })
        ]
      });

      // Bind actions if editable
      if (editable) {
        const moveUpBtn = li.querySelector('.btn-move-up');
        if (moveUpBtn && !isFirst) {
          moveUpBtn.addEventListener('click', () => {
            this.handleReorder(index, -1);
          });
        }

        const moveDownBtn = li.querySelector('.btn-move-down');
        if (moveDownBtn && !isLast) {
          moveDownBtn.addEventListener('click', () => {
            this.handleReorder(index, 1);
          });
        }

        const removeBtn = li.querySelector('.creator-remove-btn');
        if (removeBtn) {
          removeBtn.addEventListener('click', () => {
            this.handleRemoveVocab(vocab);
          });
        }
      }

      this.elements.vocabList.appendChild(li);
    });
  }

  async handleSaveTitle() {
    if (this.isMutating || !isLessonEditable(this.currentLesson?.status)) return;

    const rawTitle = this.elements.titleInput ? this.elements.titleInput.value : '';
    const validation = validateLessonTitle(rawTitle);

    if (!validation.valid) {
      if (this.elements.titleError) {
        this.elements.titleError.textContent = validation.error;
      }
      if (this.elements.titleInput) {
        this.elements.titleInput.focus();
      }
      return;
    }

    // Skip redundant request if title hasn't changed
    if (validation.sanitizedTitle === this.currentLesson.title) {
      showToast({ type: 'info', title: 'Thông báo', message: 'Tiêu đề bài học chưa thay đổi.' });
      return;
    }

    this.isMutating = true;
    if (this.elements.btnSaveTitle) {
      this.elements.btnSaveTitle.disabled = true;
      this.elements.btnSaveTitle.textContent = 'Đang lưu...';
    }

    try {
      const response = await apiClient(`/creator/lessons/${this.lessonId}`, {
        method: 'PUT',
        body: {
          title: validation.sanitizedTitle
        }
      });

      const updated = parseLessonDetail(response);
      if (updated) {
        this.currentLesson = updated;
        this.renderLessonState();
      }

      showToast({
        type: 'success',
        title: 'Cập nhật thành công',
        message: 'Tiêu đề bài học đã được lưu trên máy chủ.'
      });
    } catch (err) {
      console.warn('[CreatorLessonEditor] Title update failed:', err);
      const errMsg = err.message || 'Không thể cập nhật tiêu đề bài học.';
      if (this.elements.titleError) {
        this.elements.titleError.textContent = errMsg;
      }
      showToast({ type: 'danger', title: 'Lỗi cập nhật', message: errMsg });
    } finally {
      this.isMutating = false;
      if (this.elements.btnSaveTitle) {
        this.elements.btnSaveTitle.disabled = !isLessonEditable(this.currentLesson?.status);
        this.elements.btnSaveTitle.textContent = 'Lưu tiêu đề';
      }
    }
  }

  handleSearchInput(rawValue) {
    if (this.searchDebounceTimer) {
      clearTimeout(this.searchDebounceTimer);
    }

    const trimmed = (rawValue || '').trim();
    if (this.elements.btnSearchClear) {
      if (trimmed.length > 0) {
        this.elements.btnSearchClear.classList.remove('d-none');
      } else {
        this.elements.btnSearchClear.classList.add('d-none');
      }
    }

    if (trimmed.length === 0) {
      this.closeSearchResults();
      return;
    }

    this.searchDebounceTimer = setTimeout(() => {
      this.executeVocabSearch(trimmed);
    }, this.searchDebounceDelayMs);
  }

  async executeVocabSearch(query) {
    if (!this.elements.searchResults) return;

    this.elements.searchResults.classList.remove('d-none');
    clearContainer(this.elements.searchResults);
    this.elements.searchResults.appendChild(
      createSafeElement('div', {
        className: 'p-3 text-center text-muted small',
        text: `Đang tìm kiếm "${query}"...`
      })
    );

    try {
      const response = await apiClient('/vocabulary', {
        params: {
          search: query,
          page: 0,
          size: 8
        }
      });

      const items = Array.isArray(response?.data?.items)
        ? response.data.items
        : (Array.isArray(response?.items) ? response.items : []);

      clearContainer(this.elements.searchResults);

      if (items.length === 0) {
        this.elements.searchResults.appendChild(
          createSafeElement('div', {
            className: 'p-3 text-center text-muted small',
            text: `Không tìm thấy từ vựng nào khớp với "${query}".`
          })
        );
        return;
      }

      const existingVocabs = this.currentLesson?.vocabularies || [];

      for (const item of items) {
        const alreadyInLesson = isVocabInLesson(existingVocabs, item.vocabId);

        const row = createSafeElement('div', {
          className: 'creator-search-item',
          children: [
            // Left details
            createSafeElement('div', {
              className: 'd-flex align-items-center gap-2',
              children: [
                createSafeElement('span', {
                  className: 'creator-vocab-hanzi fs-5',
                  text: item.hanzi || ''
                }),
                createSafeElement('div', {
                  children: [
                    createSafeElement('span', {
                      className: 'fw-semibold text-primary font-monospace small',
                      text: item.pinyin || item.pinyinRaw || ''
                    }),
                    createSafeElement('div', {
                      className: 'small text-dark',
                      text: item.meaningVi || item.meaningHanViet || ''
                    })
                  ]
                })
              ]
            }),

            // Action button
            createSafeElement('button', {
              className: `btn btn-sm ${alreadyInLesson ? 'btn-light border text-muted' : 'btn-chinese-primary'} py-1 px-3`,
              text: alreadyInLesson ? 'Đã có trong bài' : '+ Thêm',
              attrs: {
                type: 'button',
                ...(alreadyInLesson || this.isMutating ? { disabled: 'true' } : {}),
                'aria-label': alreadyInLesson ? `Từ vựng ${item.hanzi} đã có trong bài` : `Thêm từ vựng ${item.hanzi} vào bài học`
              }
            })
          ]
        });

        if (!alreadyInLesson) {
          const addBtn = row.querySelector('button');
          if (addBtn) {
            addBtn.addEventListener('click', () => {
              this.handleAddVocab(item);
            });
          }
        }

        this.elements.searchResults.appendChild(row);
      }
    } catch (err) {
      console.warn('[CreatorLessonEditor] Vocab search failed:', err);
      clearContainer(this.elements.searchResults);
      this.elements.searchResults.appendChild(
        createSafeElement('div', {
          className: 'p-3 text-center text-danger small',
          text: 'Lỗi tìm kiếm từ vựng. Vui lòng thử lại.'
        })
      );
    }
  }

  closeSearchResults() {
    if (this.elements.searchResults) {
      this.elements.searchResults.classList.add('d-none');
      clearContainer(this.elements.searchResults);
    }
  }

  async handleAddVocab(vocabItem) {
    if (this.isMutating || !vocabItem?.vocabId) return;

    this.isMutating = true;

    try {
      const response = await apiClient(`/creator/lessons/${this.lessonId}/vocabularies/${vocabItem.vocabId}`, {
        method: 'POST'
      });

      const updated = parseLessonDetail(response);
      if (updated) {
        this.currentLesson = updated;
      }

      showToast({
        type: 'success',
        title: 'Đã thêm từ vựng',
        message: `Đã thêm "${vocabItem.hanzi}" vào bài học thành công.`
      });

      if (this.elements.reorderLiveStatus) {
        this.elements.reorderLiveStatus.textContent = `Đã thêm từ vựng '${vocabItem.hanzi}' vào cuối bài học.`;
      }

      this.closeSearchResults();
      if (this.elements.vocabSearchInput) {
        this.elements.vocabSearchInput.value = '';
      }
      if (this.elements.btnSearchClear) {
        this.elements.btnSearchClear.classList.add('d-none');
      }
    } catch (err) {
      console.warn('[CreatorLessonEditor] Add vocab failed:', err);
      const errMsg = err.message || 'Không thể thêm từ vựng vào bài học.';
      showToast({ type: 'danger', title: 'Lỗi thêm từ vựng', message: errMsg });
    } finally {
      this.isMutating = false;
      this.renderLessonState();
    }
  }

  async handleRemoveVocab(vocabItem) {
    if (this.isMutating || !vocabItem?.vocabId) return;

    const confirmed = window.confirm(`Bạn có chắc chắn muốn xóa từ vựng "${vocabItem.hanzi}" khỏi bài học này?`);
    if (!confirmed) return;

    this.isMutating = true;

    try {
      const response = await apiClient(`/creator/lessons/${this.lessonId}/vocabularies/${vocabItem.vocabId}`, {
        method: 'DELETE'
      });

      const updated = parseLessonDetail(response);
      if (updated) {
        this.currentLesson = updated;
      }

      showToast({
        type: 'info',
        title: 'Đã xóa từ vựng',
        message: `Đã xóa "${vocabItem.hanzi}" khỏi bài học.`
      });

      if (this.elements.reorderLiveStatus) {
        this.elements.reorderLiveStatus.textContent = `Đã xóa từ vựng '${vocabItem.hanzi}' khỏi bài học.`;
      }
    } catch (err) {
      console.warn('[CreatorLessonEditor] Remove vocab failed:', err);
      const errMsg = err.message || 'Không thể xóa từ vựng khỏi bài học.';
      showToast({ type: 'danger', title: 'Lỗi xóa từ vựng', message: errMsg });
    } finally {
      this.isMutating = false;
      this.renderLessonState();
    }
  }

  async handleReorder(fromIndex, direction) {
    if (this.isMutating || !isLessonEditable(this.currentLesson?.status)) return;

    const currentList = this.currentLesson?.vocabularies || [];
    const toIndex = fromIndex + direction;

    if (toIndex < 0 || toIndex >= currentList.length) return;

    const movedItem = currentList[fromIndex];
    const newOrder = moveVocabItem(currentList, fromIndex, toIndex);
    const payload = buildReorderPayload(newOrder);

    this.isMutating = true;
    this.disableReorderButtons();

    try {
      const response = await apiClient(`/creator/lessons/${this.lessonId}/reorder`, {
        method: 'PUT',
        body: payload
      });

      const updated = parseLessonDetail(response);
      if (updated) {
        this.currentLesson = updated;
      }

      // Announce via polite aria-live
      if (this.elements.reorderLiveStatus) {
        this.elements.reorderLiveStatus.textContent = `Đã chuyển từ vựng '${movedItem.hanzi}' đến vị trí ${toIndex + 1}/${newOrder.length}.`;
      }
    } catch (err) {
      console.warn('[CreatorLessonEditor] Reorder failed:', err);
      const errMsg = err.message || 'Không thể lưu thứ tự từ vựng mới.';
      showToast({ type: 'danger', title: 'Lỗi sắp xếp', message: errMsg });
    } finally {
      this.isMutating = false;
      this.renderLessonState();
      this.restoreFocusAfterReorder(movedItem.vocabId, direction);
    }
  }

  disableReorderButtons() {
    if (!this.elements.vocabList) return;
    const btns = this.elements.vocabList.querySelectorAll('button');
    btns.forEach(b => { b.disabled = true; });
  }

  restoreFocusAfterReorder(vocabId, direction) {
    if (!this.elements.vocabList) return;
    const targetLi = this.elements.vocabList.querySelector(`[data-vocab-id="${vocabId}"]`);
    if (targetLi) {
      const btnSelector = direction < 0 ? '.btn-move-up' : '.btn-move-down';
      const targetBtn = targetLi.querySelector(btnSelector);
      if (targetBtn && !targetBtn.disabled) {
        targetBtn.focus();
      }
    }
  }
}

// Auto-boot in browser environment if on creator-lesson-editor.html
if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('editorWorkspace')) {
      const controller = new CreatorLessonEditorController();
      controller.init();
    }
  });
}
