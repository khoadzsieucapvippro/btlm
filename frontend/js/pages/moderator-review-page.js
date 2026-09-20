/**
 * =============================================================================
 * MODERATOR LESSON REVIEW & VOCABULARY INSPECTION CONTROLLER (TASK 9E.2)
 * Module: frontend/js/pages/moderator-review-page.js
 * 
 * Responsibilities:
 * - Direct integration with Spring Boot backend via apiClient('/moderator/lessons/{id}').
 * - Role-guarded lifecycle: strictly accessible to 'Moderator' and 'Admin'.
 * - Route ID validation (positive integer route parameter `id`).
 * - Accurate presentation of verified fields: lessonId, title, status, vocabularyCount, createdAt, updatedAt.
 * - Explicitly surfaces contract characteristics: Author metadata is not present in review contract.
 * - Strictly preserves backend vocabulary ordering (orderIndex ascending).
 * - Direct audio inspection via existing LessonVocabItemResponse.audioUrl (no autoplay, safe playback).
 * - Human-readable Pinyin primary, technical pinyinRaw in collapsible disclosure.
 * - On-demand lazy inspection for constituent radicals via GET /api/v1/vocabulary/{id} with in-memory caching.
 * - Stale queue concurrency handling (graceful 404 recovery with return-to-queue CTA).
 * - Safe DOM construction (zero unsafe innerHTML) defending against XSS.
 * =============================================================================
 */

import { apiClient } from '../api/api.js';
import { authManager } from '../auth/auth-state.js';
import { initNavbarAuth } from '../ui/nav.js';
import { createSafeElement, clearContainer, sanitizeResourceUrl } from '../ui/security.js';
import { showLoading, showError, showToast } from '../ui/ui.js';

/* -----------------------------------------------------------------------------
 * 1. PURE LOGIC & UTILITY HELPERS (Exported for Testability)
 * ----------------------------------------------------------------------------- */

/**
 * Maximum character limit for rejection reason and approval note.
 */
export const MAX_MODERATION_TEXT_LENGTH = 500;

/**
 * Validates the rejection reason input according to backend contract:
 * Must be non-null, non-empty when trimmed, and <= 500 characters.
 * 
 * @param {any} rawReason 
 * @returns {{ valid: boolean, error: string|null, reason: string }}
 */
export function validateRejectionReason(rawReason) {
  if (rawReason === null || rawReason === undefined || typeof rawReason !== 'string') {
    return { valid: false, error: 'Lý do từ chối không được để trống', reason: '' };
  }
  const trimmed = rawReason.trim();
  if (trimmed.length === 0) {
    return { valid: false, error: 'Lý do từ chối không được để trống', reason: '' };
  }
  if (rawReason.length > MAX_MODERATION_TEXT_LENGTH) {
    return {
      valid: false,
      error: `Lý do từ chối không được vượt quá ${MAX_MODERATION_TEXT_LENGTH} ký tự (nhận được ${rawReason.length} ký tự)`,
      reason: rawReason
    };
  }
  return { valid: true, error: null, reason: trimmed };
}

/**
 * Validates the optional approval note according to backend contract:
 * Optional (can be null/empty), but if provided, must be <= 500 characters.
 * 
 * @param {any} rawNote 
 * @returns {{ valid: boolean, error: string|null, note: string|null }}
 */
export function validateApprovalNote(rawNote) {
  if (rawNote === null || rawNote === undefined || typeof rawNote !== 'string') {
    return { valid: true, error: null, note: null };
  }
  const trimmed = rawNote.trim();
  if (trimmed.length === 0) {
    return { valid: true, error: null, note: null };
  }
  if (rawNote.length > MAX_MODERATION_TEXT_LENGTH) {
    return {
      valid: false,
      error: `Ghi chú phê duyệt không được vượt quá ${MAX_MODERATION_TEXT_LENGTH} ký tự (nhận được ${rawNote.length} ký tự)`,
      note: rawNote
    };
  }
  return { valid: true, error: null, note: trimmed };
}

/**
 * Serializes Approve request payload adhering to ApproveLessonRequest DTO.
 * 
 * @param {string|null} note 
 * @returns {{ note: string|null }}
 */
export function serializeApprovePayload(note) {
  if (typeof note === 'string' && note.trim().length > 0) {
    return { note: note.trim() };
  }
  return { note: null };
}

/**
 * Serializes Reject request payload adhering to RejectLessonRequest DTO.
 * `flaggedFields` is serialized as a JSON-string array (e.g. '["title","pinyin"]')
 * or null if no fields are selected.
 * 
 * @param {string} reason 
 * @param {string[]|string|null} flaggedFields 
 * @returns {{ rejectionReason: string, flaggedFields: string|null }}
 */
export function serializeRejectPayload(reason, flaggedFields = null) {
  const trimmedReason = String(reason || '').trim();
  let serializedFields = null;

  if (Array.isArray(flaggedFields) && flaggedFields.length > 0) {
    const validItems = flaggedFields.map(f => String(f).trim()).filter(Boolean);
    if (validItems.length > 0) {
      serializedFields = JSON.stringify(validItems);
    }
  } else if (typeof flaggedFields === 'string' && flaggedFields.trim().length > 0) {
    serializedFields = flaggedFields.trim();
  }

  return {
    rejectionReason: trimmedReason,
    flaggedFields: serializedFields
  };
}

/**
 * Maps error status / messages from backend moderation operations into localized, friendly explanations.
 * 
 * @param {any} err 
 * @returns {{ type: 'conflict'|'validation'|'unauthorized'|'forbidden'|'error', title: string, message: string }}
 */
export function mapModerationError(err) {
  const status = err?.status || err?.statusCode || 0;
  const msg = err?.message || '';

  if (status === 409 || msg.includes('409') || msg.includes('CONFLICT') || msg.includes('trước đó') || msg.includes('trạng thái') || msg.includes('phiên làm việc khác')) {
    return {
      type: 'conflict',
      title: 'Xung Đột Trạng Thái Kiểm Duyệt (409 Conflict)',
      message: msg || 'Bài học đã được phê duyệt, từ chối bởi một kiểm duyệt viên khác hoặc không còn ở trạng thái Chờ duyệt (Pending).'
    };
  }

  if (status === 400 || msg.includes('400') || msg.includes('VALIDATION_ERROR') || msg.includes('Lý do từ chối')) {
    return {
      type: 'validation',
      title: 'Dữ Liệu Kiểm Duyệt Không Hợp Lệ (400 Bad Request)',
      message: msg || 'Dữ liệu yêu cầu kiểm duyệt không thỏa mãn ràng buộc hệ thống. Vui lòng kiểm tra lại lý do từ chối.'
    };
  }

  if (status === 401) {
    return {
      type: 'unauthorized',
      title: 'Phiên Đăng Nhập Hết Hạn (401 Unauthorized)',
      message: 'Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại để tiếp tục thao tác.'
    };
  }

  if (status === 403) {
    return {
      type: 'forbidden',
      title: 'Không Có Quyền Kiểm Duyệt (403 Forbidden)',
      message: 'Tài khoản của bạn không có vai trò Kiểm duyệt viên (Moderator) hoặc Quản trị viên (Admin).'
    };
  }

  return {
    type: 'error',
    title: 'Thao Tác Thất Bại',
    message: msg || 'Đã xảy ra lỗi không lường trước khi gửi kết quả kiểm duyệt. Vui lòng thử lại.'
  };
}

/**
 * Standard message when a lesson is no longer pending or not found in the queue.
 * @returns {string}
 */
export function getStaleQueueErrorMessage() {
  return 'Bài học không còn trong hàng đợi kiểm duyệt hoặc không tồn tại.';
}

/**
 * Validates whether the given value is a positive integer suitable for a route ID.
 * 
 * @param {any} rawId 
 * @returns {number|null}
 */
export function validateReviewLessonId(rawId) {
  if (rawId === null || rawId === undefined) return null;
  const str = String(rawId).trim();
  if (!/^\d+$/.test(str)) return null;
  const num = parseInt(str, 10);
  return (Number.isSafeInteger(num) && num > 0) ? num : null;
}

/**
 * Extracts and validates lesson ID from search params string or URL instance.
 * 
 * @param {string|URLSearchParams} searchInput 
 * @returns {number|null}
 */
export function extractLessonIdFromSearch(searchInput) {
  if (!searchInput) return null;
  const params = (searchInput instanceof URLSearchParams) 
    ? searchInput 
    : new URLSearchParams(typeof searchInput === 'string' ? searchInput : '');
  return validateReviewLessonId(params.get('id'));
}

/**
 * Checks whether the current authenticated session has access to Lesson Review.
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
 * Normalizes an ISO date string into a localized Vietnamese display string.
 * 
 * @param {string|null} isoString 
 * @returns {string}
 */
export function formatReviewDate(isoString) {
  if (!isoString) return '--';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return '--';
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year} ${hours}:${minutes}`;
  } catch {
    return '--';
  }
}

/**
 * Safely parses backend LessonDetailResponse payload.
 * Strictly preserves the backend vocabulary item ordering.
 * 
 * @param {any} envelopeOrData 
 * @returns {{
 *   lessonId: number,
 *   title: string,
 *   status: string,
 *   vocabularyCount: number,
 *   createdAt: string|null,
 *   updatedAt: string|null,
 *   vocabularies: Array<{
 *     vocabId: number,
 *     hanzi: string,
 *     pinyin: string,
 *     pinyinRaw: string|null,
 *     meaningHanViet: string|null,
 *     meaningVi: string,
 *     audioUrl: string|null,
 *     videoWritingUrl: string|null,
 *     exampleSentence: string|null,
 *     exampleTranslation: string|null,
 *     orderIndex: number
 *   }>
 * }}
 */
export function parseLessonReviewDetail(envelopeOrData) {
  if (!envelopeOrData) {
    throw new Error('Dữ liệu bài học trống');
  }

  // Handle ApiResponse envelope { code, message, data }
  const data = (envelopeOrData && typeof envelopeOrData === 'object' && 'data' in envelopeOrData && envelopeOrData.data !== undefined)
    ? envelopeOrData.data
    : envelopeOrData;

  if (!data || typeof data !== 'object') {
    throw new Error('Định dạng dữ liệu bài học không hợp lệ');
  }

  const rawVocabs = Array.isArray(data.vocabularies) ? data.vocabularies : [];
  
  // Normalizes each item without mutating backend order
  const vocabularies = rawVocabs.map((item, idx) => {
    const vId = validateReviewLessonId(item.vocabId) || (idx + 1);
    const orderIdx = typeof item.orderIndex === 'number' ? item.orderIndex : (idx + 1);
    return {
      vocabId: vId,
      hanzi: String(item.hanzi || '').trim() || '?',
      pinyin: String(item.pinyin || '').trim() || '',
      pinyinRaw: item.pinyinRaw ? String(item.pinyinRaw).trim() : null,
      meaningHanViet: item.meaningHanViet ? String(item.meaningHanViet).trim() : null,
      meaningVi: String(item.meaningVi || '').trim() || 'Chưa có nghĩa tiếng Việt',
      audioUrl: item.audioUrl ? String(item.audioUrl).trim() : null,
      videoWritingUrl: item.videoWritingUrl ? String(item.videoWritingUrl).trim() : null,
      exampleSentence: item.exampleSentence ? String(item.exampleSentence).trim() : null,
      exampleTranslation: item.exampleTranslation ? String(item.exampleTranslation).trim() : null,
      orderIndex: orderIdx
    };
  });

  return {
    lessonId: validateReviewLessonId(data.lessonId) || 0,
    title: String(data.title || 'Bài học không tên').trim(),
    status: String(data.status || 'Pending').trim(),
    vocabularyCount: typeof data.vocabularyCount === 'number' ? data.vocabularyCount : vocabularies.length,
    createdAt: data.createdAt || null,
    updatedAt: data.updatedAt || null,
    vocabularies
  };
}

/**
 * Normalizes radical items extracted from VocabularyDetailResponse.
 * 
 * @param {any} vocabDetailEnvelopeOrData 
 * @returns {Array<{
 *   radicalId: number,
 *   character: string,
 *   pinyin: string,
 *   meaningHanViet: string,
 *   meaningVi: string
 * }>}
 */
export function normalizeRadicalList(vocabDetailEnvelopeOrData) {
  if (!vocabDetailEnvelopeOrData) return [];
  const data = (vocabDetailEnvelopeOrData && typeof vocabDetailEnvelopeOrData === 'object' && 'data' in vocabDetailEnvelopeOrData)
    ? vocabDetailEnvelopeOrData.data
    : vocabDetailEnvelopeOrData;

  if (!data || !Array.isArray(data.radicals)) return [];

  return data.radicals.map(r => ({
    radicalId: typeof r.radicalId === 'number' ? r.radicalId : 0,
    character: String(r.character || '').trim(),
    pinyin: String(r.pinyin || '').trim(),
    meaningHanViet: String(r.meaningHanViet || '').trim(),
    meaningVi: String(r.meaningVi || '').trim()
  })).filter(r => r.character.length > 0);
}

/* -----------------------------------------------------------------------------
 * 2. MODERATOR REVIEW CONTROLLER CLASS
 * ----------------------------------------------------------------------------- */

export class ModeratorReviewController {
  constructor() {
    this.lessonId = null;
    this.lessonData = null;
    this.radicalCache = new Map(); // vocabId -> RadicalResponse[]
    this.currentAudio = null;
    this.isSubmitting = false;
    this.activeElementBeforeOpen = null;
    this.dom = {};
  }

  bindDomElements() {
    this.dom = {
      authGuardContainer: document.getElementById('moderatorAuthGuardContainer'),
      moderatorReviewSection: document.getElementById('moderatorReviewSection'),
      stateContainer: document.getElementById('reviewStateContainer'),
      headerCard: document.getElementById('reviewLessonHeaderCard'),
      breadcrumbLessonTitle: document.getElementById('breadcrumbLessonTitle'),
      lessonTitle: document.getElementById('reviewLessonTitle'),
      lessonIdBadge: document.getElementById('reviewLessonIdBadge'),
      lessonStatusBadge: document.getElementById('reviewLessonStatusBadge'),
      lessonVocabCountBadge: document.getElementById('reviewLessonVocabCountBadge'),
      lessonCreatedAt: document.getElementById('reviewLessonCreatedAt'),
      lessonUpdatedAt: document.getElementById('reviewLessonUpdatedAt'),
      lessonAuthorNote: document.getElementById('reviewLessonAuthorNote'),
      vocabSection: document.getElementById('reviewVocabSection'),
      vocabSummaryText: document.getElementById('reviewVocabSummaryText'),
      vocabList: document.getElementById('reviewVocabList'),

      // Moderation Action Controls
      moderationActionBar: document.getElementById('moderationActionBar'),
      btnOpenRejectModal: document.getElementById('btnOpenRejectModal'),
      btnOpenApproveModal: document.getElementById('btnOpenApproveModal'),
      moderationStatusAlert: document.getElementById('moderationStatusAlert'),
      reviewBottomActionBar: document.getElementById('reviewBottomActionBar'),
      btnBottomRejectModal: document.getElementById('btnBottomRejectModal'),
      btnBottomApproveModal: document.getElementById('btnBottomApproveModal'),

      // Approve Modal Elements
      approveModal: document.getElementById('approveModal'),
      approveModalTitle: document.getElementById('approveModalTitle'),
      btnCloseApproveModal: document.getElementById('btnCloseApproveModal'),
      approveNoteInput: document.getElementById('approveNoteInput'),
      approveNoteCount: document.getElementById('approveNoteCount'),
      btnCancelApprove: document.getElementById('btnCancelApprove'),
      btnConfirmApprove: document.getElementById('btnConfirmApprove'),
      btnConfirmApproveText: document.getElementById('btnConfirmApproveText'),

      // Reject Modal Elements
      rejectModal: document.getElementById('rejectModal'),
      rejectModalTitle: document.getElementById('rejectModalTitle'),
      btnCloseRejectModal: document.getElementById('btnCloseRejectModal'),
      rejectReasonInput: document.getElementById('rejectReasonInput'),
      rejectReasonCount: document.getElementById('rejectReasonCount'),
      rejectReasonError: document.getElementById('rejectReasonError'),
      flaggedFieldsContainer: document.getElementById('flaggedFieldsContainer'),
      btnCancelReject: document.getElementById('btnCancelReject'),
      btnConfirmReject: document.getElementById('btnConfirmReject'),
      btnConfirmRejectText: document.getElementById('btnConfirmRejectText')
    };
  }

  /**
   * Initializes page lifecycle: Auth guard check, query parameter parsing, event bindings, and initial fetch.
   */
  async init() {
    this.bindDomElements();
    initNavbarAuth();

    // 1. Role Guard check
    if (!hasModeratorAccess(authManager)) {
      this.renderAuthGuard();
      return;
    }

    // 2. Validate lesson ID from search params
    const rawSearch = window.location ? window.location.search : '';
    this.lessonId = extractLessonIdFromSearch(rawSearch);

    if (!this.lessonId) {
      this.renderInvalidIdState();
      return;
    }

    // 3. Bind dialog & moderation events
    this.bindModerationEvents();

    // 4. Load lesson details
    await this.fetchLessonDetail();
  }

  /**
   * Binds interactive events for moderation actions, modals, and live counters.
   */
  bindModerationEvents() {
    // Open Approve Modal triggers
    this.dom.btnOpenApproveModal?.addEventListener('click', () => this.openApproveDialog());
    this.dom.btnBottomApproveModal?.addEventListener('click', () => this.openApproveDialog());

    // Open Reject Modal triggers
    this.dom.btnOpenRejectModal?.addEventListener('click', () => this.openRejectDialog());
    this.dom.btnBottomRejectModal?.addEventListener('click', () => this.openRejectDialog());

    // Dismiss Approve Modal
    this.dom.btnCloseApproveModal?.addEventListener('click', () => this.closeApproveDialog());
    this.dom.btnCancelApprove?.addEventListener('click', () => this.closeApproveDialog());
    this.dom.approveModal?.addEventListener('cancel', (e) => {
      e.preventDefault();
      this.closeApproveDialog();
    });

    // Dismiss Reject Modal
    this.dom.btnCloseRejectModal?.addEventListener('click', () => this.closeRejectDialog());
    this.dom.btnCancelReject?.addEventListener('click', () => this.closeRejectDialog());
    this.dom.rejectModal?.addEventListener('cancel', (e) => {
      e.preventDefault();
      this.closeRejectDialog();
    });

    // Approve live counter
    this.dom.approveNoteInput?.addEventListener('input', () => this.updateApproveCount());

    // Reject live counter & inline error clear
    this.dom.rejectReasonInput?.addEventListener('input', () => {
      this.updateRejectCount();
      if (this.dom.rejectReasonError) {
        this.dom.rejectReasonError.classList.add('d-none');
      }
    });

    // Submission confirmations
    this.dom.btnConfirmApprove?.addEventListener('click', () => this.submitApprove());
    this.dom.btnConfirmReject?.addEventListener('click', () => this.submitReject());
  }

  /**
   * Opens the accessible Approve confirmation dialog.
   */
  openApproveDialog() {
    if (this.isSubmitting) return;
    this.activeElementBeforeOpen = document.activeElement;

    if (this.dom.approveNoteInput) {
      this.dom.approveNoteInput.value = '';
      this.updateApproveCount();
    }

    if (this.dom.approveModal) {
      if (typeof this.dom.approveModal.showModal === 'function') {
        this.dom.approveModal.showModal();
      } else {
        this.dom.approveModal.setAttribute('open', '');
      }
      this.dom.approveNoteInput?.focus();
    }
  }

  /**
   * Closes the Approve dialog and restores focus to trigger element.
   */
  closeApproveDialog() {
    if (this.dom.approveModal) {
      if (typeof this.dom.approveModal.close === 'function') {
        this.dom.approveModal.close();
      } else {
        this.dom.approveModal.removeAttribute('open');
      }
    }
    if (this.activeElementBeforeOpen && typeof this.activeElementBeforeOpen.focus === 'function') {
      this.activeElementBeforeOpen.focus();
    }
  }

  /**
   * Opens the accessible Reject confirmation dialog.
   */
  openRejectDialog() {
    if (this.isSubmitting) return;
    this.activeElementBeforeOpen = document.activeElement;

    if (this.dom.rejectReasonInput) {
      this.dom.rejectReasonInput.value = '';
      this.updateRejectCount();
    }
    if (this.dom.rejectReasonError) {
      this.dom.rejectReasonError.textContent = '';
      this.dom.rejectReasonError.classList.add('d-none');
    }
    if (this.dom.flaggedFieldsContainer) {
      const cbs = this.dom.flaggedFieldsContainer.querySelectorAll('input[type="checkbox"]');
      cbs.forEach(cb => { cb.checked = false; });
    }

    if (this.dom.rejectModal) {
      if (typeof this.dom.rejectModal.showModal === 'function') {
        this.dom.rejectModal.showModal();
      } else {
        this.dom.rejectModal.setAttribute('open', '');
      }
      this.dom.rejectReasonInput?.focus();
    }
  }

  /**
   * Closes the Reject dialog and restores focus to trigger element.
   */
  closeRejectDialog() {
    if (this.dom.rejectModal) {
      if (typeof this.dom.rejectModal.close === 'function') {
        this.dom.rejectModal.close();
      } else {
        this.dom.rejectModal.removeAttribute('open');
      }
    }
    if (this.activeElementBeforeOpen && typeof this.activeElementBeforeOpen.focus === 'function') {
      this.activeElementBeforeOpen.focus();
    }
  }

  /**
   * Updates character counter for Approve note textarea.
   */
  updateApproveCount() {
    if (!this.dom.approveNoteInput || !this.dom.approveNoteCount) return;
    const len = this.dom.approveNoteInput.value.length;
    this.dom.approveNoteCount.textContent = `${len} / ${MAX_MODERATION_TEXT_LENGTH}`;
    if (len > MAX_MODERATION_TEXT_LENGTH) {
      this.dom.approveNoteCount.classList.add('text-danger');
    } else {
      this.dom.approveNoteCount.classList.remove('text-danger');
    }
  }

  /**
   * Updates character counter for Reject reason textarea.
   */
  updateRejectCount() {
    if (!this.dom.rejectReasonInput || !this.dom.rejectReasonCount) return;
    const len = this.dom.rejectReasonInput.value.length;
    this.dom.rejectReasonCount.textContent = `${len} / ${MAX_MODERATION_TEXT_LENGTH}`;
    if (len > MAX_MODERATION_TEXT_LENGTH) {
      this.dom.rejectReasonCount.classList.add('text-danger');
    } else {
      this.dom.rejectReasonCount.classList.remove('text-danger');
    }
  }

  /**
   * Manages submission state, disabling buttons and showing spinner indicator to prevent duplicate mutation.
   * 
   * @param {boolean} isSubmitting 
   * @param {'approve'|'reject'} actionType 
   */
  setSubmitting(isSubmitting, actionType) {
    this.isSubmitting = isSubmitting;

    if (actionType === 'approve') {
      if (this.dom.btnConfirmApprove) this.dom.btnConfirmApprove.disabled = isSubmitting;
      if (this.dom.btnConfirmApproveText) {
        this.dom.btnConfirmApproveText.textContent = isSubmitting ? 'Đang phê duyệt...' : 'Xác nhận phê duyệt';
      }
      if (this.dom.btnCancelApprove) this.dom.btnCancelApprove.disabled = isSubmitting;
      if (this.dom.approveNoteInput) this.dom.approveNoteInput.disabled = isSubmitting;
    } else if (actionType === 'reject') {
      if (this.dom.btnConfirmReject) this.dom.btnConfirmReject.disabled = isSubmitting;
      if (this.dom.btnConfirmRejectText) {
        this.dom.btnConfirmRejectText.textContent = isSubmitting ? 'Đang từ chối...' : 'Xác nhận từ chối';
      }
      if (this.dom.btnCancelReject) this.dom.btnCancelReject.disabled = isSubmitting;
      if (this.dom.rejectReasonInput) this.dom.rejectReasonInput.disabled = isSubmitting;
      if (this.dom.flaggedFieldsContainer) {
        const cbs = this.dom.flaggedFieldsContainer.querySelectorAll('input[type="checkbox"]');
        cbs.forEach(cb => { cb.disabled = isSubmitting; });
      }
    }
  }

  /**
   * Disables all moderation mutation triggers once a decision is made or state is non-Pending.
   */
  disableModerationActions() {
    if (this.dom.moderationActionBar) {
      this.dom.moderationActionBar.classList.add('d-none');
    }
    if (this.dom.reviewBottomActionBar) {
      this.dom.reviewBottomActionBar.classList.add('d-none');
    }
    if (this.dom.btnOpenRejectModal) this.dom.btnOpenRejectModal.disabled = true;
    if (this.dom.btnOpenApproveModal) this.dom.btnOpenApproveModal.disabled = true;
    if (this.dom.btnBottomRejectModal) this.dom.btnBottomRejectModal.disabled = true;
    if (this.dom.btnBottomApproveModal) this.dom.btnBottomApproveModal.disabled = true;
  }

  /**
   * Updates the UI into the authoritative finalized state upon server 200 OK.
   * 
   * @param {'approve'|'reject'} action 
   * @param {object} responseData 
   */
  renderModerationSuccess(action, responseData) {
    const isApprove = action === 'approve';

    if (this.dom.lessonStatusBadge) {
      if (isApprove) {
        this.dom.lessonStatusBadge.className = 'badge bg-success-subtle text-success border border-success-subtle px-2 py-1';
        this.dom.lessonStatusBadge.textContent = 'Đã duyệt (Approved)';
      } else {
        this.dom.lessonStatusBadge.className = 'badge bg-danger-subtle text-danger border border-danger-subtle px-2 py-1';
        this.dom.lessonStatusBadge.textContent = 'Bị từ chối (Rejected)';
      }
    }

    this.disableModerationActions();

    if (this.dom.moderationStatusAlert) {
      clearContainer(this.dom.moderationStatusAlert);
      this.dom.moderationStatusAlert.className = `alert alert-${isApprove ? 'success' : 'warning'} d-flex flex-column flex-sm-row align-items-start align-items-sm-center justify-content-between gap-2 mt-2 mb-3`;

      const messageWrapper = createSafeElement('div', {
        children: [
          createSafeElement('strong', {
            text: isApprove ? 'Phê duyệt bài học thành công! ' : 'Đã từ chối bài học thành công! '
          }),
          createSafeElement('span', {
            text: isApprove
              ? 'Bài học đã chuyển sang trạng thái Approved và được xuất bản công khai.'
              : 'Bài học đã chuyển sang trạng thái Rejected và được phản hồi lại tác giả.'
          })
        ]
      });

      const returnLink = createSafeElement('a', {
        className: 'btn btn-sm btn-outline-dark text-nowrap',
        attrs: { 'href': 'moderator-queue.html', 'id': 'btnSuccessReturnQueue' },
        children: [
          createSafeElement('span', { attrs: { 'aria-hidden': 'true' }, text: '← ' }),
          createSafeElement('span', { text: 'Quay lại hàng đợi' })
        ]
      });

      this.dom.moderationStatusAlert.appendChild(messageWrapper);
      this.dom.moderationStatusAlert.appendChild(returnLink);
      this.dom.moderationStatusAlert.classList.remove('d-none');
    }

    showToast({
      type: isApprove ? 'success' : 'warning',
      title: isApprove ? 'Đã Phê Duyệt' : 'Đã Từ Chối',
      message: isApprove ? 'Bài học đã được duyệt thành công.' : 'Bài học đã bị từ chối thành công.'
    });
  }

  /**
   * Displays a truthful 409 Conflict state alert without faking success.
   * 
   * @param {string} [message] 
   */
  renderConflictState(message) {
    this.disableModerationActions();

    if (this.dom.moderationStatusAlert) {
      clearContainer(this.dom.moderationStatusAlert);
      this.dom.moderationStatusAlert.className = 'alert alert-danger d-flex flex-column flex-sm-row align-items-start align-items-sm-center justify-content-between gap-2 mt-2 mb-3';

      const messageWrapper = createSafeElement('div', {
        children: [
          createSafeElement('strong', { text: 'Xung đột trạng thái kiểm duyệt (409 Conflict): ' }),
          createSafeElement('span', { text: message || 'Bài học này đã được xử lý bởi một phiên làm việc khác hoặc không còn ở trạng thái Chờ duyệt.' })
        ]
      });

      const returnLink = createSafeElement('a', {
        className: 'btn btn-sm btn-danger text-nowrap',
        attrs: { 'href': 'moderator-queue.html', 'id': 'btnConflictReturnQueue' },
        children: [
          createSafeElement('span', { attrs: { 'aria-hidden': 'true' }, text: '← ' }),
          createSafeElement('span', { text: 'Quay lại hàng đợi' })
        ]
      });

      this.dom.moderationStatusAlert.appendChild(messageWrapper);
      this.dom.moderationStatusAlert.appendChild(returnLink);
      this.dom.moderationStatusAlert.classList.remove('d-none');
    }

    showToast({
      type: 'danger',
      title: 'Xung đột trạng thái',
      message: message || 'Bài học đã được xử lý bởi một phiên làm việc khác.'
    });
  }

  /**
   * Submits lesson approval to POST /api/v1/moderator/lessons/{id}/approve.
   */
  async submitApprove() {
    if (this.isSubmitting) return;

    const noteVal = this.dom.approveNoteInput ? this.dom.approveNoteInput.value : '';
    const noteCheck = validateApprovalNote(noteVal);
    if (!noteCheck.valid) {
      showToast({ type: 'danger', title: 'Lỗi ghi chú', message: noteCheck.error });
      return;
    }

    this.setSubmitting(true, 'approve');

    try {
      const payload = serializeApprovePayload(noteCheck.note);
      const response = await apiClient(`/moderator/lessons/${this.lessonId}/approve`, {
        method: 'POST',
        body: payload
      });

      this.closeApproveDialog();
      this.renderModerationSuccess('approve', response);
    } catch (err) {
      const mapped = mapModerationError(err);
      if (mapped.type === 'conflict') {
        this.closeApproveDialog();
        this.renderConflictState(mapped.message);
      } else {
        showToast({ type: 'danger', title: mapped.title, message: mapped.message });
      }
    } finally {
      this.setSubmitting(false, 'approve');
    }
  }

  /**
   * Submits lesson rejection to POST /api/v1/moderator/lessons/{id}/reject.
   */
  async submitReject() {
    if (this.isSubmitting) return;

    const reasonVal = this.dom.rejectReasonInput ? this.dom.rejectReasonInput.value : '';
    const reasonCheck = validateRejectionReason(reasonVal);
    if (!reasonCheck.valid) {
      if (this.dom.rejectReasonError) {
        this.dom.rejectReasonError.textContent = reasonCheck.error;
        this.dom.rejectReasonError.classList.remove('d-none');
      }
      this.dom.rejectReasonInput?.focus();
      return;
    }

    const checkedFields = Array.from(
      this.dom.flaggedFieldsContainer?.querySelectorAll('input[type="checkbox"]:checked') || []
    ).map(cb => cb.value);

    this.setSubmitting(true, 'reject');

    try {
      const payload = serializeRejectPayload(reasonCheck.reason, checkedFields);
      const response = await apiClient(`/moderator/lessons/${this.lessonId}/reject`, {
        method: 'POST',
        body: payload
      });

      this.closeRejectDialog();
      this.renderModerationSuccess('reject', response);
    } catch (err) {
      const mapped = mapModerationError(err);
      if (mapped.type === 'conflict') {
        this.closeRejectDialog();
        this.renderConflictState(mapped.message);
      } else if (mapped.type === 'validation') {
        if (this.dom.rejectReasonError) {
          this.dom.rejectReasonError.textContent = mapped.message;
          this.dom.rejectReasonError.classList.remove('d-none');
        }
        this.dom.rejectReasonInput?.focus();
      } else {
        showToast({ type: 'danger', title: mapped.title, message: mapped.message });
      }
    } finally {
      this.setSubmitting(false, 'reject');
    }
  }

  /**
   * Renders the unauthorized role guard banner.
   */
  renderAuthGuard() {
    if (this.dom.authGuardContainer) {
      this.dom.authGuardContainer.classList.remove('d-none');
    }
    if (this.dom.moderatorReviewSection) {
      this.dom.moderatorReviewSection.classList.add('d-none');
    }
  }

  /**
   * Renders invalid route parameter state.
   */
  renderInvalidIdState() {
    if (!this.dom.stateContainer) return;
    clearContainer(this.dom.stateContainer);

    showError(this.dom.stateContainer, {
      title: 'Mã bài học không hợp lệ',
      message: 'Vui lòng cung cấp mã số bài học hợp lệ từ hàng đợi kiểm duyệt.',
      retryText: 'Quay lại hàng đợi',
      onRetry: () => {
        window.location.href = 'moderator-queue.html';
      }
    });
  }

  /**
   * Fetches the pending lesson detail from GET /api/v1/moderator/lessons/{id}.
   */
  async fetchLessonDetail() {
    if (!this.dom.stateContainer) return;

    // Show loading state
    showLoading(this.dom.stateContainer, {
      message: `Đang tải chi tiết bài học #${this.lessonId}...`
    });

    if (this.dom.headerCard) this.dom.headerCard.classList.add('d-none');
    if (this.dom.vocabSection) this.dom.vocabSection.classList.add('d-none');
    if (this.dom.reviewBottomActionBar) this.dom.reviewBottomActionBar.classList.add('d-none');

    try {
      const response = await apiClient(`/moderator/lessons/${this.lessonId}`);
      this.lessonData = parseLessonReviewDetail(response);

      // Verify status invariant: strictly pending for review
      if (this.lessonData.status.toLowerCase() !== 'pending') {
        this.renderNonPendingState(this.lessonData.status);
        this.disableModerationActions();
        return;
      }

      // Success: render review interface
      clearContainer(this.dom.stateContainer);
      this.renderLessonHeader(this.lessonData);
      this.renderVocabularyList(this.lessonData.vocabularies);
      if (this.dom.reviewBottomActionBar) {
        this.dom.reviewBottomActionBar.classList.remove('d-none');
      }
    } catch (err) {
      // Differentiate 404 / Stale Queue concurrency from server error
      const status = err?.status || err?.statusCode || 0;
      if (status === 404 || err?.message?.includes('404') || err?.message?.includes('Không tìm thấy')) {
        this.renderStaleQueueState();
      } else {
        this.renderServerErrorState(err?.message);
      }
    }
  }

  /**
   * Renders graceful 404 / Stale Queue state when lesson was processed by another moderator or not found.
   */
  renderStaleQueueState() {
    if (!this.dom.stateContainer) return;
    clearContainer(this.dom.stateContainer);

    const alertCard = createSafeElement('div', {
      className: 'creator-panel-card text-center py-5 border',
      children: [
        createSafeElement('div', {
          className: 'state-empty-glyph mb-3 text-warning',
          attrs: { 'aria-hidden': 'true' },
          text: '审'
        }),
        createSafeElement('h2', {
          className: 'h5 fw-bold text-dark mb-2',
          text: 'Bài Học Không Còn Trong Hàng Đợi'
        }),
        createSafeElement('p', {
          className: 'text-secondary small mb-4 mx-auto',
          attrs: { 'style': 'max-width: 520px;' },
          text: getStaleQueueErrorMessage()
        }),
        createSafeElement('a', {
          className: 'btn-chinese-primary d-inline-flex align-items-center gap-2 text-decoration-none',
          attrs: { 'href': './moderator-queue.html', 'id': 'staleReturnToQueueBtn' },
          children: [
            createSafeElement('span', { attrs: { 'aria-hidden': 'true' }, text: '←' }),
            createSafeElement('span', { text: 'Quay lại hàng đợi kiểm duyệt' })
          ]
        })
      ]
    });

    this.dom.stateContainer.appendChild(alertCard);
  }

  /**
   * Renders notice when lesson has a status other than Pending.
   * @param {string} currentStatus 
   */
  renderNonPendingState(currentStatus) {
    if (!this.dom.stateContainer) return;
    clearContainer(this.dom.stateContainer);

    showError(this.dom.stateContainer, {
      title: 'Bài học không ở trạng thái chờ duyệt',
      message: `Bài học này hiện đang ở trạng thái "${currentStatus}". Khu vực này chỉ phục vụ thẩm định các bài học đang Chờ duyệt (Pending).`,
      retryText: 'Quay lại hàng đợi kiểm duyệt',
      onRetry: () => {
        window.location.href = 'moderator-queue.html';
      }
    });
  }

  /**
   * Renders generic network/server error with a retry trigger.
   * @param {string} [errorMessage] 
   */
  renderServerErrorState(errorMessage) {
    if (!this.dom.stateContainer) return;
    clearContainer(this.dom.stateContainer);

    showError(this.dom.stateContainer, {
      title: 'Không thể tải bài học',
      message: errorMessage || 'Đã xảy ra lỗi khi tải dữ liệu bài học. Vui lòng thử lại.',
      retryText: 'Thử lại',
      onRetry: () => this.fetchLessonDetail()
    });
  }

  /**
   * Populates the lesson header card with verified metadata.
   * @param {object} lesson 
   */
  renderLessonHeader(lesson) {
    if (!this.dom.headerCard) return;

    if (this.dom.lessonTitle) {
      this.dom.lessonTitle.textContent = lesson.title;
    }
    if (this.dom.breadcrumbLessonTitle) {
      this.dom.breadcrumbLessonTitle.textContent = lesson.title;
    }
    if (this.dom.lessonIdBadge) {
      this.dom.lessonIdBadge.textContent = `Bài học #${lesson.lessonId}`;
    }
    if (this.dom.lessonStatusBadge) {
      const st = String(lesson.status || 'Pending');
      if (st.toLowerCase() === 'approved') {
        this.dom.lessonStatusBadge.className = 'badge bg-success-subtle text-success border border-success-subtle px-2 py-1';
        this.dom.lessonStatusBadge.textContent = 'Đã duyệt (Approved)';
      } else if (st.toLowerCase() === 'rejected') {
        this.dom.lessonStatusBadge.className = 'badge bg-danger-subtle text-danger border border-danger-subtle px-2 py-1';
        this.dom.lessonStatusBadge.textContent = 'Bị từ chối (Rejected)';
      } else {
        this.dom.lessonStatusBadge.className = 'badge bg-warning-subtle text-warning-emphasis border border-warning-subtle px-2 py-1';
        this.dom.lessonStatusBadge.textContent = 'Chờ duyệt (Pending)';
      }
    }
    if (this.dom.lessonVocabCountBadge) {
      this.dom.lessonVocabCountBadge.textContent = `${lesson.vocabularyCount} từ vựng`;
    }
    if (this.dom.lessonCreatedAt) {
      this.dom.lessonCreatedAt.textContent = formatReviewDate(lesson.createdAt);
    }
    if (this.dom.lessonUpdatedAt) {
      this.dom.lessonUpdatedAt.textContent = formatReviewDate(lesson.updatedAt);
    }
    if (this.dom.lessonAuthorNote) {
      this.dom.lessonAuthorNote.textContent = 'Không khả dụng trong hợp đồng chi tiết hiện tại';
    }

    if (String(lesson.status).toLowerCase() === 'pending') {
      if (this.dom.moderationActionBar) this.dom.moderationActionBar.classList.remove('d-none');
      if (this.dom.reviewBottomActionBar) this.dom.reviewBottomActionBar.classList.remove('d-none');
    } else {
      this.disableModerationActions();
    }

    this.dom.headerCard.classList.remove('d-none');
  }

  /**
   * Renders the ordered list of constituent vocabulary cards.
   * Strictly preserves backend array orderIndex sequence.
   * 
   * @param {Array<object>} vocabularies 
   */
  renderVocabularyList(vocabularies) {
    if (!this.dom.vocabSection || !this.dom.vocabList) return;

    clearContainer(this.dom.vocabList);

    if (this.dom.vocabSummaryText) {
      this.dom.vocabSummaryText.textContent = `Tổng cộng ${vocabularies.length} mục từ vựng`;
    }

    if (vocabularies.length === 0) {
      const emptyItem = createSafeElement('li', {
        className: 'creator-panel-card text-center py-4 text-muted small',
        text: 'Bài học này hiện chưa có mục từ vựng nào.'
      });
      this.dom.vocabList.appendChild(emptyItem);
      this.dom.vocabSection.classList.remove('d-none');
      return;
    }

    vocabularies.forEach((vocab, index) => {
      const itemNode = this.createVocabularyItemElement(vocab, index);
      this.dom.vocabList.appendChild(itemNode);
    });

    this.dom.vocabSection.classList.remove('d-none');
  }

  /**
   * Creates a safe DOM card for a single vocabulary inspection item.
   * 
   * @param {object} vocab 
   * @param {number} index 
   * @returns {HTMLElement}
   */
  createVocabularyItemElement(vocab, index) {
    const itemCard = createSafeElement('li', {
      className: 'creator-panel-card p-3 p-md-4 mb-3 border position-relative',
      attrs: {
        'id': `vocab-review-item-${vocab.vocabId}`,
        'data-order': String(vocab.orderIndex)
      }
    });

    // 1. Top Bar: Order Badge & Vocab ID
    const topBar = createSafeElement('div', {
      className: 'd-flex justify-content-between align-items-center mb-3 pb-2 border-bottom',
      children: [
        createSafeElement('div', {
          className: 'd-flex align-items-center gap-2',
          children: [
            createSafeElement('span', {
              className: 'badge bg-dark px-2 py-1 font-monospace',
              text: `#${vocab.orderIndex}`
            }),
            createSafeElement('span', {
              className: 'small text-muted',
              text: `Mục thứ ${index + 1}`
            })
          ]
        }),
        createSafeElement('div', {
          className: 'small text-secondary font-monospace',
          text: `Vocab ID: ${vocab.vocabId}`
        })
      ]
    });

    // 2. Core Linguistic Row: Hanzi, Pinyin, Audio, Meanings
    const linguisticRow = createSafeElement('div', {
      className: 'row g-3 align-items-start mb-3',
      children: [
        // Left Column: Hanzi & Pronunciation
        createSafeElement('div', {
          className: 'col-md-4 text-md-start text-center',
          children: [
            createSafeElement('div', {
              className: 'display-5 fw-bold text-chinese-red mb-1',
              attrs: { 'lang': 'zh-CN' },
              text: vocab.hanzi
            }),
            createSafeElement('div', {
              className: 'h6 fw-semibold text-primary mb-2',
              text: vocab.pinyin || '--'
            }),
            this.createAudioControl(vocab)
          ]
        }),

        // Right Column: Meanings & Examples
        createSafeElement('div', {
          className: 'col-md-8',
          children: [
            // Meanings row
            createSafeElement('div', {
              className: 'mb-3',
              children: [
                createSafeElement('div', {
                  className: 'd-flex align-items-center gap-2 mb-1 flex-wrap',
                  children: [
                    createSafeElement('span', {
                      className: 'badge bg-secondary-subtle text-secondary border',
                      text: `Hán-Việt: ${vocab.meaningHanViet || '--'}`
                    })
                  ]
                }),
                createSafeElement('div', {
                  className: 'fw-bold text-dark fs-5',
                  text: vocab.meaningVi
                })
              ]
            }),

            // Examples block
            createSafeElement('div', {
              className: 'p-3 bg-light rounded small border mb-2',
              children: [
                createSafeElement('div', { className: 'text-muted mb-1 fw-semibold', text: 'Ví dụ minh họa:' }),
                createSafeElement('div', {
                  className: 'mb-1 text-dark fw-medium',
                  attrs: { 'lang': 'zh-CN' },
                  text: vocab.exampleSentence || 'Chưa có câu ví dụ minh họa'
                }),
                createSafeElement('div', {
                  className: 'text-secondary fst-italic',
                  text: vocab.exampleTranslation ? `Dịch: ${vocab.exampleTranslation}` : 'Chưa có dịch nghĩa câu ví dụ'
                })
              ]
            }),

            // Optional Writing Video action
            this.createVideoWritingAction(vocab)
          ]
        })
      ]
    });

    // 3. Constituent Radicals Inspection Section (Lazy loaded on disclosure)
    const radicalsSection = this.createRadicalsInspectionSection(vocab);

    // 4. Technical details disclosure (Pinyin thô)
    const technicalDetails = createSafeElement('details', {
      className: 'small text-secondary mt-2 pt-2 border-top',
      children: [
        createSafeElement('summary', {
          className: 'cursor-pointer text-muted',
          text: 'Thông tin kỹ thuật (Pinyin thô)'
        }),
        createSafeElement('div', {
          className: 'p-2 mt-2 bg-light rounded font-monospace small',
          children: [
            createSafeElement('span', { className: 'fw-semibold', text: 'Pinyin Raw: ' }),
            createSafeElement('span', { text: vocab.pinyinRaw || '(Không có)' }),
            createSafeElement('span', { text: ' | ' }),
            createSafeElement('span', { className: 'fw-semibold', text: 'OrderIndex: ' }),
            createSafeElement('span', { text: String(vocab.orderIndex) })
          ]
        })
      ]
    });

    itemCard.appendChild(topBar);
    itemCard.appendChild(linguisticRow);
    itemCard.appendChild(radicalsSection);
    itemCard.appendChild(technicalDetails);

    return itemCard;
  }

  /**
   * Creates an accessible audio playback control for the vocabulary item.
   * Directly consumes existing LessonVocabItemResponse.audioUrl without additional requests.
   * 
   * @param {object} vocab 
   * @returns {HTMLElement}
   */
  createAudioControl(vocab) {
    if (!vocab.audioUrl) {
      return createSafeElement('span', {
        className: 'badge bg-light text-muted border px-2 py-1',
        text: 'Không có phát âm'
      });
    }

    const safeAudioUrl = sanitizeResourceUrl(vocab.audioUrl);
    if (!safeAudioUrl || safeAudioUrl === 'about:blank') {
      return createSafeElement('span', {
        className: 'badge bg-light text-muted border px-2 py-1',
        text: 'URL phát âm không an toàn'
      });
    }

    const audioBtn = createSafeElement('button', {
      className: 'btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1',
      attrs: {
        'type': 'button',
        'aria-label': `Nghe phát âm chuẩn của từ ${vocab.hanzi}`,
        'id': `btn-audio-${vocab.vocabId}`
      },
      children: [
        createSafeElement('span', { attrs: { 'aria-hidden': 'true' }, text: '🔊' }),
        createSafeElement('span', { text: 'Nghe phát âm' })
      ]
    });

    audioBtn.addEventListener('click', () => {
      this.playAudio(safeAudioUrl, vocab.hanzi, audioBtn);
    });

    return audioBtn;
  }

  /**
   * Plays audio with safe error handling and visual button state indication.
   * 
   * @param {string} safeUrl 
   * @param {string} fallbackHanzi 
   * @param {HTMLButtonElement} buttonEl 
   */
  playAudio(safeUrl, fallbackHanzi, buttonEl) {
    if (this.currentAudio) {
      try {
        this.currentAudio.pause();
        this.currentAudio = null;
      } catch {
        // Safe ignore
      }
    }

    try {
      const audio = new Audio(safeUrl);
      this.currentAudio = audio;

      const originalText = buttonEl.querySelector('span:last-child');
      if (originalText) originalText.textContent = 'Đang phát...';

      audio.play().then(() => {
        audio.onended = () => {
          if (originalText) originalText.textContent = 'Nghe phát âm';
        };
      }).catch(() => {
        if (originalText) originalText.textContent = 'Nghe phát âm';
        this.speakFallback(fallbackHanzi);
      });
    } catch {
      this.speakFallback(fallbackHanzi);
    }
  }

  /**
   * Fallback speech synthesis if audio playback fails.
   * @param {string} hanzi 
   */
  speakFallback(hanzi) {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window && window.SpeechSynthesisUtterance) {
      try {
        const utterance = new SpeechSynthesisUtterance(hanzi);
        utterance.lang = 'zh-CN';
        window.speechSynthesis.speak(utterance);
      } catch {
        // Safe ignore
      }
    }
  }

  /**
   * Creates an optional writing video action link if videoWritingUrl is provided.
   * 
   * @param {object} vocab 
   * @returns {HTMLElement}
   */
  createVideoWritingAction(vocab) {
    if (!vocab.videoWritingUrl) {
      return createSafeElement('div');
    }

    const safeVideoUrl = sanitizeResourceUrl(vocab.videoWritingUrl);
    if (!safeVideoUrl || safeVideoUrl === 'about:blank') {
      return createSafeElement('div');
    }

    return createSafeElement('div', {
      className: 'mt-2',
      children: [
        createSafeElement('a', {
          className: 'btn btn-sm btn-outline-secondary d-inline-flex align-items-center gap-1',
          attrs: {
            'href': safeVideoUrl,
            'target': '_blank',
            'rel': 'noopener noreferrer',
            'aria-label': `Xem video hướng dẫn viết từ ${vocab.hanzi}`
          },
          children: [
            createSafeElement('span', { attrs: { 'aria-hidden': 'true' }, text: '🎥' }),
            createSafeElement('span', { text: 'Video hướng dẫn viết' })
          ]
        })
      ]
    });
  }

  /**
   * Creates the Constituent Radicals Inspection Section.
   * Conforms to W3C APG Disclosure pattern (button controlling hidden content with aria-expanded).
   * Lazy-loads radical decomposition on demand from GET /api/v1/vocabulary/{vocabId}.
   * 
   * @param {object} vocab 
   * @returns {HTMLElement}
   */
  createRadicalsInspectionSection(vocab) {
    const panelId = `radicals-panel-${vocab.vocabId}`;
    const container = createSafeElement('div', {
      className: 'mt-3 pt-3 border-top'
    });

    const toggleBtn = createSafeElement('button', {
      className: 'btn btn-sm btn-outline-secondary d-inline-flex align-items-center gap-2',
      attrs: {
        'type': 'button',
        'aria-expanded': 'false',
        'aria-controls': panelId,
        'id': `btn-radicals-toggle-${vocab.vocabId}`
      },
      children: [
        createSafeElement('span', { className: 'toggle-icon', attrs: { 'aria-hidden': 'true' }, text: '▶' }),
        createSafeElement('span', { text: 'Kiểm tra bộ thủ cấu thành' })
      ]
    });

    const panel = createSafeElement('div', {
      className: 'mt-2 p-3 bg-light rounded border d-none',
      attrs: {
        'id': panelId,
        'role': 'region',
        'aria-labelledby': `btn-radicals-toggle-${vocab.vocabId}`
      }
    });

    toggleBtn.addEventListener('click', async () => {
      const isExpanded = toggleBtn.getAttribute('aria-expanded') === 'true';
      const newExpanded = !isExpanded;
      toggleBtn.setAttribute('aria-expanded', String(newExpanded));

      const icon = toggleBtn.querySelector('.toggle-icon');
      if (icon) icon.textContent = newExpanded ? '▼' : '▶';

      if (newExpanded) {
        panel.classList.remove('d-none');
        await this.loadRadicalsForVocab(vocab.vocabId, panel);
      } else {
        panel.classList.add('d-none');
      }
    });

    container.appendChild(toggleBtn);
    container.appendChild(panel);
    return container;
  }

  /**
   * Lazy loads constituent radicals for a specific vocabulary ID, using session cache to avoid redundant calls.
   * 
   * @param {number} vocabId 
   * @param {HTMLElement} panel 
   */
  async loadRadicalsForVocab(vocabId, panel) {
    // 1. Check in-memory cache
    if (this.radicalCache.has(vocabId)) {
      this.renderRadicalsContent(this.radicalCache.get(vocabId), panel);
      return;
    }

    // 2. Show small loading state inside panel
    clearContainer(panel);
    panel.appendChild(createSafeElement('div', {
      className: 'small text-muted d-flex align-items-center gap-2',
      children: [
        createSafeElement('span', { className: 'spinner-border spinner-border-sm', attrs: { 'aria-hidden': 'true' } }),
        createSafeElement('span', { text: 'Đang tra cứu dữ liệu bộ thủ...' })
      ]
    }));

    try {
      const response = await apiClient(`/vocabulary/${vocabId}`);
      const radicals = normalizeRadicalList(response);
      this.radicalCache.set(vocabId, radicals);
      this.renderRadicalsContent(radicals, panel);
    } catch {
      clearContainer(panel);
      panel.appendChild(createSafeElement('div', {
        className: 'small text-muted fst-italic',
        text: 'Không thể tải thông tin bộ thủ cấu thành lúc này.'
      }));
    }
  }

  /**
   * Renders the radical badges into the disclosure panel.
   * 
   * @param {Array<object>} radicals 
   * @param {HTMLElement} panel 
   */
  renderRadicalsContent(radicals, panel) {
    clearContainer(panel);

    if (!radicals || radicals.length === 0) {
      panel.appendChild(createSafeElement('div', {
        className: 'small text-muted fst-italic',
        text: 'Không có thông tin bộ thủ cấu thành cho từ vựng này.'
      }));
      return;
    }

    const wrapper = createSafeElement('div', {
      children: [
        createSafeElement('div', {
          className: 'small fw-semibold text-dark mb-2',
          text: `Bộ thủ cấu thành (${radicals.length} bộ thủ):`
        }),
        createSafeElement('div', {
          className: 'd-flex flex-wrap gap-2',
          children: radicals.map(r => createSafeElement('div', {
            className: 'p-2 bg-white rounded border shadow-sm small d-flex align-items-center gap-2',
            children: [
              createSafeElement('span', {
                className: 'fs-5 fw-bold text-chinese-red px-1 border-end',
                attrs: { 'lang': 'zh-CN' },
                text: r.character
              }),
              createSafeElement('div', {
                className: 'd-flex flex-column',
                children: [
                  createSafeElement('span', {
                    className: 'fw-semibold text-primary',
                    text: r.pinyin ? `${r.pinyin}` : '--'
                  }),
                  createSafeElement('span', {
                    className: 'text-secondary',
                    text: r.meaningHanViet ? `Hán-Việt: ${r.meaningHanViet}` : (r.meaningVi || '--')
                  })
                ]
              })
            ]
          }))
        })
      ]
    });

    panel.appendChild(wrapper);
  }
}

/**
 * Global Page Initializer invoked on DOMContentLoaded.
 * Guarded against execution when imported on non-moderator-review pages.
 * 
 * @returns {ModeratorReviewController|null}
 */
export function initModeratorReviewPage() {
  if (typeof document === 'undefined') return null;
  const root = document.getElementById('approveModal') || document.getElementById('rejectModal');
  if (!root) return null;

  const controller = new ModeratorReviewController();
  controller.init();
  return controller;
}

if (typeof window !== 'undefined' && typeof document !== 'undefined') {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      initModeratorReviewPage();
    });
  } else {
    initModeratorReviewPage();
  }
}


