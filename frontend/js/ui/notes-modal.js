/**
 * =============================================================================
 * CONTEXTUAL PERSONAL NOTES MODAL (TASK 9C.2)
 * Module: frontend/js/ui/notes-modal.js
 * 
 * Responsibilities:
 * - Contextual note management embedded directly into vocabulary & lesson workflows.
 * - Reusable across vocabulary.html, lesson-detail.html, and ui-verification.html.
 * - Authenticated session enforcement (Single-Owner authManager; zero client userId spoofing).
 * - Anonymous user guard (prompts login without exposing note existence or calling API).
 * - Full CRUD: GET (paginated), POST (create), PUT (update), DELETE (with confirmation).
 * - Strict 500-character limit with live counter and accessible status announcer.
 * - Safe DOM rendering: 100% textContent / createElement (OWASP ASVS 5.0 DOM XSS defense).
 * - Native <dialog> modal (WCAG 2.2 APG Pattern, focus restoration, Escape handling).
 * - Race condition & double-submit protection.
 * =============================================================================
 */

import { apiClient, ApiError } from '../api/api.js';
import { authManager } from '../auth/auth-state.js';
import { createSafeElement, clearContainer } from './security.js';
import { showToast, openModal } from './ui.js';

export const MAX_NOTE_LENGTH = 500;
export const DEFAULT_PAGE_SIZE = 20;

/**
 * Validates note text length and presence.
 * 
 * @param {string} content 
 * @returns {{ valid: boolean, error?: string }}
 */
export function validateNoteContent(content) {
  if (content === null || content === undefined || typeof content !== 'string') {
    return { valid: false, error: 'Nội dung ghi chú không được để trống' };
  }
  const str = content.trim();
  if (str.length === 0) {
    return { valid: false, error: 'Nội dung ghi chú không được để trống' };
  }
  if (content.length > MAX_NOTE_LENGTH) {
    return {
      valid: false,
      error: `Nội dung ghi chú tối đa ${MAX_NOTE_LENGTH} ký tự (nhận được: ${content.length})`
    };
  }
  return { valid: true, error: null };
}

/**
 * Formats ISO timestamp to human-readable Vietnamese date/time string.
 * 
 * @param {string} isoString 
 * @returns {string}
 */
export function formatNoteDate(isoString) {
  if (!isoString) return '';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return 'Vừa xong';
    return d.toLocaleString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch {
    return String(isoString);
  }
}

/**
 * Notes Modal Controller Class
 */
export class NotesModalController {
  constructor() {
    this.vocabId = null;
    this.vocabContext = { hanzi: '', pinyin: '', meaning: '' };
    this.triggerElement = null;

    // State
    this.currentPage = 0;
    this.pageSize = DEFAULT_PAGE_SIZE;
    this.totalPages = 1;
    this.totalElements = 0;
    this.notes = [];

    this.editingNoteId = null;
    this.isSubmitting = false;
    this.activeRequestId = 0;

    // DOM References
    this.dialog = null;
    this.elements = {};
  }

  /**
   * Ensures the <dialog id="notesModal"> element exists in DOM, creating it lazily if needed.
   * @returns {HTMLDialogElement}
   */
  ensureDialog() {
    let dialog = document.getElementById('notesModal');
    if (!dialog) {
      dialog = createSafeElement('dialog', {
        className: 'radical-dialog notes-dialog',
        attrs: {
          id: 'notesModal',
          'aria-labelledby': 'notesModalTitle',
          'aria-describedby': 'notesVocabContext'
        }
      });
      document.body.appendChild(dialog);
    }
    this.dialog = dialog;
    this.renderDialogSkeleton();
    return dialog;
  }

  /**
   * Builds the internal layout skeleton of the dialog.
   */
  renderDialogSkeleton() {
    if (!this.dialog) return;
    clearContainer(this.dialog);

    const card = createSafeElement('div', { className: 'radical-modal-card notes-modal-card' });

    // 1. Header
    const header = createSafeElement('div', {
      className: 'notes-modal-header d-flex align-items-center justify-content-between p-3 border-bottom'
    });
    const titleGroup = createSafeElement('div', { className: 'd-flex align-items-center gap-2' });
    const seal = createSafeElement('span', {
      className: 'brand-seal-mark',
      attrs: { 'aria-hidden': 'true', style: 'width: 28px; height: 28px; font-size: 0.9rem;' },
      text: '记'
    });
    const title = createSafeElement('h2', {
      className: 'h5 modal-title mb-0 fw-bold text-dark',
      attrs: { id: 'notesModalTitle' },
      text: 'Ghi Chú Cá Nhân'
    });
    titleGroup.appendChild(seal);
    titleGroup.appendChild(title);

    const closeBtn = createSafeElement('button', {
      className: 'btn-close',
      attrs: {
        type: 'button',
        id: 'notesModalCloseBtn',
        'aria-label': 'Đóng ghi chú cá nhân'
      }
    });
    closeBtn.addEventListener('click', () => this.close());

    header.appendChild(titleGroup);
    header.appendChild(closeBtn);
    card.appendChild(header);

    // 2. Vocabulary Context Bar
    const contextBox = createSafeElement('div', {
      className: 'notes-context-box p-3 bg-light border-bottom d-flex align-items-center gap-3',
      attrs: { id: 'notesVocabContext' },
      children: [
        createSafeElement('div', {
          className: 'notes-context-hanzi text-center',
          attrs: { id: 'notesContextHanzi', lang: 'zh-Hans' },
          text: '—'
        }),
        createSafeElement('div', {
          className: 'notes-context-readings',
          children: [
            createSafeElement('div', {
              className: 'fw-bold text-danger fs-6',
              attrs: { id: 'notesContextPinyin' },
              text: '—'
            }),
            createSafeElement('div', {
              className: 'small text-muted',
              attrs: { id: 'notesContextMeaning' },
              text: '—'
            })
          ]
        })
      ]
    });
    card.appendChild(contextBox);

    // 3. Modal Body (Composer + Notes List)
    const body = createSafeElement('div', { className: 'notes-modal-body p-3' });

    // Composer Form
    const composerSection = createSafeElement('section', {
      className: 'notes-composer mb-4 p-3 rounded border bg-surface',
      attrs: { 'aria-labelledby': 'composerHeading' }
    });

    const composerHeading = createSafeElement('h3', {
      className: 'h6 fw-bold mb-2 text-dark',
      attrs: { id: 'composerHeading' },
      text: 'Thêm ghi chú mới'
    });
    composerSection.appendChild(composerHeading);

    const form = createSafeElement('form', {
      attrs: { id: 'noteComposerForm', novalidate: 'true' }
    });
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      this.handleSubmitNote();
    });

    const textareaWrapper = createSafeElement('div', { className: 'mb-2' });
    const label = createSafeElement('label', {
      className: 'visually-hidden',
      attrs: { for: 'noteContentInput' },
      text: 'Nội dung ghi chú cá nhân'
    });
    const textarea = createSafeElement('textarea', {
      className: 'form-control note-textarea',
      attrs: {
        id: 'noteContentInput',
        rows: '3',
        maxlength: String(MAX_NOTE_LENGTH),
        placeholder: 'Nhập ghi chú cho từ vựng này (tối đa 500 ký tự)...',
        'aria-describedby': 'noteCharCounter noteFeedback'
      }
    });
    textarea.addEventListener('input', () => this.updateCharacterCounter());

    textareaWrapper.appendChild(label);
    textareaWrapper.appendChild(textarea);
    form.appendChild(textareaWrapper);

    // Composer controls bar: Counter + Feedback + Buttons
    const controlsBar = createSafeElement('div', {
      className: 'd-flex align-items-center justify-content-between flex-wrap gap-2'
    });

    const counter = createSafeElement('div', {
      className: 'small text-muted note-char-counter',
      attrs: { id: 'noteCharCounter', 'aria-live': 'polite' },
      children: [
        createSafeElement('span', { attrs: { id: 'noteCharCountVal' }, text: '0' }),
        document.createTextNode(` / ${MAX_NOTE_LENGTH} ký tự`)
      ]
    });

    const actionBtns = createSafeElement('div', { className: 'd-flex align-items-center gap-2' });
    const cancelEditBtn = createSafeElement('button', {
      className: 'btn-chinese-secondary btn-sm d-none',
      attrs: { type: 'button', id: 'noteCancelEditBtn' },
      text: 'Hủy sửa'
    });
    cancelEditBtn.addEventListener('click', () => this.cancelEditing());

    const submitBtn = createSafeElement('button', {
      className: 'btn-chinese-primary btn-sm',
      attrs: { type: 'submit', id: 'noteSubmitBtn' },
      text: 'Lưu ghi chú'
    });

    actionBtns.appendChild(cancelEditBtn);
    actionBtns.appendChild(submitBtn);

    controlsBar.appendChild(counter);
    controlsBar.appendChild(actionBtns);
    form.appendChild(controlsBar);

    // Feedback container (alerts/status)
    const feedback = createSafeElement('div', {
      className: 'note-feedback-message mt-2 d-none',
      attrs: { id: 'noteFeedback', role: 'status', 'aria-live': 'polite' }
    });
    form.appendChild(feedback);

    composerSection.appendChild(form);
    body.appendChild(composerSection);

    // Existing Notes Section
    const notesSection = createSafeElement('section', {
      className: 'notes-list-section',
      attrs: { 'aria-labelledby': 'notesListHeading' }
    });

    const listHeader = createSafeElement('div', {
      className: 'd-flex align-items-center justify-content-between mb-2'
    });
    const listTitle = createSafeElement('h3', {
      className: 'h6 fw-bold mb-0 text-dark',
      attrs: { id: 'notesListHeading' },
      text: 'Ghi chú đã lưu'
    });
    const countBadge = createSafeElement('span', {
      className: 'badge-status badge-status-draft text-nowrap small',
      attrs: { id: 'notesCountBadge' },
      text: '0 ghi chú'
    });
    listHeader.appendChild(listTitle);
    listHeader.appendChild(countBadge);
    notesSection.appendChild(listHeader);

    // 3-State Container for Notes
    const stateContainer = createSafeElement('div', {
      attrs: { id: 'notesStateContainer' }
    });
    notesSection.appendChild(stateContainer);

    // Notes List Container
    const listContainer = createSafeElement('div', {
      className: 'notes-items-list d-flex flex-column gap-2',
      attrs: { id: 'notesItemsList', role: 'list', 'aria-label': 'Danh sách ghi chú cá nhân' }
    });
    notesSection.appendChild(listContainer);

    // Pagination Container
    const pagination = createSafeElement('nav', {
      className: 'pagination-container mt-3 d-none',
      attrs: { id: 'notesPagination', 'aria-label': 'Phân trang ghi chú cá nhân' }
    });
    notesSection.appendChild(pagination);

    body.appendChild(notesSection);
    card.appendChild(body);

    this.dialog.appendChild(card);

    // Store references
    this.elements = {
      card,
      closeBtn,
      contextHanzi: document.getElementById('notesContextHanzi'),
      contextPinyin: document.getElementById('notesContextPinyin'),
      contextMeaning: document.getElementById('notesContextMeaning'),
      composerHeading: document.getElementById('composerHeading'),
      form: document.getElementById('noteComposerForm'),
      textarea: document.getElementById('noteContentInput'),
      counterVal: document.getElementById('noteCharCountVal'),
      counterWrapper: document.getElementById('noteCharCounter'),
      submitBtn: document.getElementById('noteSubmitBtn'),
      cancelEditBtn: document.getElementById('noteCancelEditBtn'),
      feedback: document.getElementById('noteFeedback'),
      countBadge: document.getElementById('notesCountBadge'),
      stateContainer: document.getElementById('notesStateContainer'),
      itemsList: document.getElementById('notesItemsList'),
      pagination: document.getElementById('notesPagination')
    };

    // Native dialog cancel listener (Escape key)
    this.dialog.addEventListener('cancel', () => {
      this.handleDialogClosed();
    });
  }

  /**
   * Opens the notes modal for a specific vocabulary context.
   * 
   * @param {Object} options
   * @param {number|string} options.vocabId ID of vocabulary item
   * @param {string} [options.hanzi]
   * @param {string} [options.pinyin]
   * @param {string} [options.meaning]
   * @param {HTMLElement} [options.triggerElement] element to restore focus to on dismiss
   */
  async open({ vocabId, hanzi = '', pinyin = '', meaning = '', triggerElement = null } = {}) {
    this.triggerElement = triggerElement;

    // 1. Authenticated User Guard
    if (!authManager.isAuthenticated()) {
      await this.promptLogin();
      return;
    }

    const numericVocabId = Number(vocabId);
    if (!numericVocabId || isNaN(numericVocabId) || numericVocabId <= 0) {
      showToast({
        type: 'danger',
        title: 'Lỗi từ vựng',
        message: 'Mã từ vựng không hợp lệ.'
      });
      return;
    }

    this.vocabId = numericVocabId;
    this.vocabContext = {
      hanzi: hanzi || '—',
      pinyin: pinyin || '',
      meaning: meaning || ''
    };

    this.ensureDialog();
    this.resetComposer();
    this.updateContextDisplay();

    // 2. Open Native Dialog with Top-Layer Focus Trap
    if (typeof this.dialog.showModal === 'function') {
      this.dialog.showModal();
    } else {
      this.dialog.setAttribute('open', '');
      this.dialog.style.display = 'block';
    }

    // Move initial focus to textarea or close button
    if (this.elements.textarea) {
      this.elements.textarea.focus();
    }

    // 3. Fetch notes for this vocabulary
    this.currentPage = 0;
    await this.loadNotes();
  }

  /**
   * Closes the dialog cleanly and restores focus.
   */
  close() {
    if (!this.dialog) return;
    if (typeof this.dialog.close === 'function') {
      this.dialog.close();
    } else {
      this.dialog.removeAttribute('open');
      this.dialog.style.display = 'none';
    }
    this.handleDialogClosed();
  }

  handleDialogClosed() {
    this.cancelEditing();
    if (this.triggerElement && typeof this.triggerElement.focus === 'function') {
      try {
        this.triggerElement.focus();
      } catch (_) {}
    }
  }

  /**
   * Prompts guest/anonymous user to log in before using personal notes.
   */
  async promptLogin() {
    const returnUrl = typeof window !== 'undefined' && window.location
      ? `${window.location.pathname}${window.location.search}`
      : 'vocabulary.html';

    const confirmed = await openModal({
      title: 'Đăng nhập để ghi chú',
      body: 'Tính năng Ghi chú cá nhân yêu cầu tài khoản học viên để lưu trữ an toàn và đồng bộ bài học. Bạn có muốn đăng nhập ngay bây giờ không?',
      confirmText: 'Đăng nhập ngay',
      cancelText: 'Để sau'
    });

    if (confirmed && typeof window !== 'undefined') {
      window.location.href = `login.html?redirect=${encodeURIComponent(returnUrl)}`;
    }
  }

  /**
   * Updates context header inside the modal.
   */
  updateContextDisplay() {
    if (this.elements.contextHanzi) {
      this.elements.contextHanzi.textContent = this.vocabContext.hanzi;
    }
    if (this.elements.contextPinyin) {
      this.elements.contextPinyin.textContent = this.vocabContext.pinyin;
    }
    if (this.elements.contextMeaning) {
      this.elements.contextMeaning.textContent = this.vocabContext.meaning || 'Ghi chú học tập cho từ vựng này';
    }
  }

  /**
   * Updates character counter display and limit states.
   */
  updateCharacterCounter() {
    if (!this.elements.textarea || !this.elements.counterVal) return;
    const len = this.elements.textarea.value.length;
    this.elements.counterVal.textContent = String(len);

    if (len >= MAX_NOTE_LENGTH) {
      this.elements.counterWrapper.classList.add('text-danger', 'fw-bold');
      this.elements.counterWrapper.classList.remove('text-muted');
    } else if (len >= MAX_NOTE_LENGTH - 50) {
      this.elements.counterWrapper.classList.add('text-warning', 'fw-semibold');
      this.elements.counterWrapper.classList.remove('text-danger', 'text-muted');
    } else {
      this.elements.counterWrapper.className = 'small text-muted note-char-counter';
    }
  }

  /**
   * Shows inline feedback message in composer.
   * 
   * @param {string} message 
   * @param {'success'|'danger'|'info'} type 
   */
  showFeedback(message, type = 'danger') {
    if (!this.elements.feedback) return;
    this.elements.feedback.textContent = message;
    this.elements.feedback.className = `note-feedback-message mt-2 alert alert-${type} py-1 px-2 small mb-0`;
    this.elements.feedback.classList.remove('d-none');
    if (type === 'danger') {
      this.elements.feedback.setAttribute('role', 'alert');
    } else {
      this.elements.feedback.setAttribute('role', 'status');
    }
  }

  clearFeedback() {
    if (!this.elements.feedback) return;
    this.elements.feedback.textContent = '';
    this.elements.feedback.className = 'note-feedback-message mt-2 d-none';
  }

  /**
   * Resets composer form back to clean create state.
   */
  resetComposer() {
    this.editingNoteId = null;
    this.isSubmitting = false;
    if (this.elements.form) this.elements.form.reset();
    if (this.elements.textarea) this.elements.textarea.value = '';
    if (this.elements.composerHeading) this.elements.composerHeading.textContent = 'Thêm ghi chú mới';
    if (this.elements.submitBtn) {
      this.elements.submitBtn.textContent = 'Lưu ghi chú';
      this.elements.submitBtn.disabled = false;
    }
    if (this.elements.cancelEditBtn) {
      this.elements.cancelEditBtn.classList.add('d-none');
    }
    this.updateCharacterCounter();
    this.clearFeedback();
  }

  /**
   * Begins editing an existing note item.
   * 
   * @param {Object} note 
   */
  startEditing(note) {
    if (!note || !note.noteId) return;
    this.editingNoteId = note.noteId;
    this.clearFeedback();

    if (this.elements.composerHeading) {
      this.elements.composerHeading.textContent = 'Chỉnh sửa ghi chú';
    }
    if (this.elements.textarea) {
      this.elements.textarea.value = note.content || '';
      this.elements.textarea.focus();
    }
    if (this.elements.submitBtn) {
      this.elements.submitBtn.textContent = 'Lưu cập nhật';
      this.elements.submitBtn.disabled = false;
    }
    if (this.elements.cancelEditBtn) {
      this.elements.cancelEditBtn.classList.remove('d-none');
    }
    this.updateCharacterCounter();

    // Scroll composer into view
    if (this.elements.textarea) {
      this.elements.textarea.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }

  /**
   * Cancels editing mode and restores create form.
   */
  cancelEditing() {
    this.resetComposer();
  }

  /**
   * Loads personal notes from GET /api/v1/vocabularies/{vocabId}/notes?page={page}&size={size}.
   */
  async loadNotes() {
    if (!this.vocabId) return;
    const requestId = ++this.activeRequestId;

    this.renderLoadingState();

    try {
      const response = await apiClient(`/vocabularies/${this.vocabId}/notes`, {
        params: {
          page: this.currentPage,
          size: this.pageSize
        }
      });

      if (requestId !== this.activeRequestId) {
        return; // Stale request drop
      }

      const page = response || {};
      this.currentPage = typeof page.page === 'number' ? page.page : 0;
      this.pageSize = typeof page.size === 'number' ? page.size : DEFAULT_PAGE_SIZE;
      this.totalElements = typeof page.totalElements === 'number' ? page.totalElements : 0;
      this.totalPages = typeof page.totalPages === 'number' ? page.totalPages : 1;
      this.notes = Array.isArray(page.items) ? page.items : [];

      this.renderNotesList();

    } catch (err) {
      if (requestId !== this.activeRequestId) return;
      console.error('[PersonalNotes] Failed to load notes:', err);
      this.renderErrorState(err.message || 'Không thể tải danh sách ghi chú.');
    }
  }

  renderLoadingState() {
    clearContainer(this.elements.itemsList);
    clearContainer(this.elements.stateContainer);
    if (this.elements.pagination) this.elements.pagination.classList.add('d-none');

    const loader = createSafeElement('div', {
      className: 'p-3 text-center text-muted small',
      attrs: { role: 'status', 'aria-live': 'polite' },
      children: [
        createSafeElement('span', { className: 'spinner-border spinner-border-sm me-2', attrs: { 'aria-hidden': 'true' } }),
        document.createTextNode('Đang tải ghi chú cá nhân...')
      ]
    });
    this.elements.stateContainer.appendChild(loader);
  }

  renderErrorState(message) {
    clearContainer(this.elements.itemsList);
    clearContainer(this.elements.stateContainer);
    if (this.elements.pagination) this.elements.pagination.classList.add('d-none');

    const errorBox = createSafeElement('div', {
      className: 'alert alert-danger p-2 small mb-0 d-flex align-items-center justify-content-between',
      attrs: { role: 'alert' },
      children: [
        createSafeElement('span', { text: message }),
        createSafeElement('button', {
          className: 'btn btn-sm btn-outline-danger py-0 px-2',
          attrs: { type: 'button' },
          text: 'Thử lại'
        })
      ]
    });
    const retryBtn = errorBox.querySelector('button');
    retryBtn.addEventListener('click', () => this.loadNotes());

    this.elements.stateContainer.appendChild(errorBox);
  }

  /**
   * Renders notes items or polite empty state.
   */
  renderNotesList() {
    clearContainer(this.elements.stateContainer);
    clearContainer(this.elements.itemsList);

    // Update count badge
    if (this.elements.countBadge) {
      this.elements.countBadge.textContent = `${this.totalElements} ghi chú`;
    }

    if (this.notes.length === 0) {
      const emptyEl = createSafeElement('div', {
        className: 'p-3 text-center text-muted small bg-light rounded border',
        attrs: { id: 'notesEmptyState' },
        text: 'Bạn chưa có ghi chú nào cho từ vựng này. Hãy nhập ghi chú ở trên để lưu lại ngữ cảnh học tập.'
      });
      this.elements.stateContainer.appendChild(emptyEl);
      if (this.elements.pagination) this.elements.pagination.classList.add('d-none');
      return;
    }

    // Render list items safely using DOM APIs (Zero-unsafe innerHTML)
    for (const note of this.notes) {
      const itemEl = this.createNoteItemElement(note);
      this.elements.itemsList.appendChild(itemEl);
    }

    this.renderPagination();
  }

  /**
   * Creates a safe DOM card for an individual note item.
   * 
   * @param {Object} note 
   * @returns {HTMLElement}
   */
  createNoteItemElement(note) {
    const item = createSafeElement('div', {
      className: 'note-item card p-3 border rounded shadow-sm',
      attrs: {
        role: 'listitem',
        'data-note-id': String(note.noteId)
      }
    });

    // Top metadata row
    const topRow = createSafeElement('div', {
      className: 'd-flex align-items-center justify-content-between mb-2 text-muted small'
    });
    const timeEl = createSafeElement('time', {
      className: 'note-timestamp',
      attrs: { datetime: note.createdAt || '' },
      text: formatNoteDate(note.createdAt)
    });
    topRow.appendChild(timeEl);

    // Actions: Sửa / Xóa
    const actions = createSafeElement('div', { className: 'note-item-actions d-flex gap-1' });

    const editBtn = createSafeElement('button', {
      className: 'btn btn-sm btn-outline-secondary py-0 px-2 note-edit-btn',
      attrs: {
        type: 'button',
        'data-action': 'edit',
        'aria-label': `Sửa ghi chú tạo lúc ${formatNoteDate(note.createdAt)}`
      },
      text: 'Sửa'
    });
    editBtn.addEventListener('click', () => this.startEditing(note));

    const deleteBtn = createSafeElement('button', {
      className: 'btn btn-sm btn-outline-danger py-0 px-2 note-delete-btn',
      attrs: {
        type: 'button',
        'data-action': 'delete',
        'aria-label': `Xóa ghi chú tạo lúc ${formatNoteDate(note.createdAt)}`
      },
      text: 'Xóa'
    });

    // Inline deletion confirmation container (no nested modal conflicts)
    const confirmContainer = createSafeElement('div', {
      className: 'note-delete-confirm-box alert alert-warning p-2 mt-2 mb-0 d-none',
      attrs: { role: 'alert' }
    });

    deleteBtn.addEventListener('click', () => {
      confirmContainer.classList.remove('d-none');
      const confirmBtn = confirmContainer.querySelector('.note-confirm-del');
      if (confirmBtn) confirmBtn.focus();
    });

    actions.appendChild(editBtn);
    actions.appendChild(deleteBtn);
    topRow.appendChild(actions);
    item.appendChild(topRow);

    // Note content (strictly textContent, preserves newlines via CSS pre-wrap)
    const contentEl = createSafeElement('div', {
      className: 'note-content text-dark mb-1',
      text: note.content || ''
    });
    item.appendChild(contentEl);

    // Confirmation box inside card
    const confirmMsg = createSafeElement('div', {
      className: 'd-flex align-items-center justify-content-between flex-wrap gap-2',
      children: [
        createSafeElement('span', { className: 'fw-semibold small', text: 'Bạn có chắc chắn muốn xóa ghi chú này?' }),
        createSafeElement('div', {
          className: 'd-flex gap-2',
          children: [
            createSafeElement('button', {
              className: 'btn btn-sm btn-danger py-0 px-2 note-confirm-del',
              attrs: { type: 'button', 'data-action': 'confirm-delete' },
              text: 'Xóa ngay'
            }),
            createSafeElement('button', {
              className: 'btn btn-sm btn-secondary py-0 px-2 note-cancel-del',
              attrs: { type: 'button', 'data-action': 'cancel-delete' },
              text: 'Hủy'
            })
          ]
        })
      ]
    });
    confirmContainer.appendChild(confirmMsg);

    const confirmBtn = confirmMsg.querySelector('.note-confirm-del');
    const cancelBtn = confirmMsg.querySelector('.note-cancel-del');

    confirmBtn.addEventListener('click', async () => {
      await this.handleDeleteNote(note.noteId, confirmBtn);
    });

    cancelBtn.addEventListener('click', () => {
      confirmContainer.classList.add('d-none');
      deleteBtn.focus();
    });

    item.appendChild(confirmContainer);
    return item;
  }

  /**
   * Renders pagination controls for notes.
   */
  renderPagination() {
    if (!this.elements.pagination) return;

    if (this.totalPages <= 1) {
      this.elements.pagination.classList.add('d-none');
      clearContainer(this.elements.pagination);
      return;
    }

    this.elements.pagination.classList.remove('d-none');
    clearContainer(this.elements.pagination);

    const prevBtn = createSafeElement('button', {
      className: 'btn btn-sm btn-outline-secondary',
      attrs: {
        type: 'button',
        'aria-label': 'Trang ghi chú trước'
      },
      text: '← Trước'
    });
    if (this.currentPage <= 0) {
      prevBtn.disabled = true;
    } else {
      prevBtn.addEventListener('click', () => {
        this.currentPage--;
        this.loadNotes();
      });
    }

    const pageInfo = createSafeElement('span', {
      className: 'small text-muted align-self-center px-2',
      text: `Trang ${this.currentPage + 1} / ${this.totalPages}`
    });

    const nextBtn = createSafeElement('button', {
      className: 'btn btn-sm btn-outline-secondary',
      attrs: {
        type: 'button',
        'aria-label': 'Trang ghi chú kế tiếp'
      },
      text: 'Sau →'
    });
    if (this.currentPage >= this.totalPages - 1) {
      nextBtn.disabled = true;
    } else {
      nextBtn.addEventListener('click', () => {
        this.currentPage++;
        this.loadNotes();
      });
    }

    this.elements.pagination.appendChild(prevBtn);
    this.elements.pagination.appendChild(pageInfo);
    this.elements.pagination.appendChild(nextBtn);
  }

  /**
   * Submits create or update note request.
   */
  async handleSubmitNote() {
    if (this.isSubmitting) return;

    const rawContent = this.elements.textarea ? this.elements.textarea.value : '';
    const validation = validateNoteContent(rawContent);

    if (!validation.valid) {
      this.showFeedback(validation.error, 'danger');
      if (this.elements.textarea) this.elements.textarea.focus();
      return;
    }

    this.clearFeedback();
    this.isSubmitting = true;
    if (this.elements.submitBtn) {
      this.elements.submitBtn.disabled = true;
      this.elements.submitBtn.textContent = 'Đang lưu...';
    }

    const payload = { content: rawContent.trim() };

    try {
      if (this.editingNoteId) {
        // PUT /api/v1/notes/{noteId}
        const updated = await apiClient(`/notes/${this.editingNoteId}`, {
          method: 'PUT',
          body: payload
        });
        showToast({
          type: 'success',
          title: 'Thành công',
          message: 'Cập nhật ghi chú thành công.'
        });
        this.showFeedback('Cập nhật ghi chú thành công!', 'success');
        this.resetComposer();
        await this.loadNotes();
      } else {
        // POST /api/v1/vocabularies/{vocabId}/notes
        const created = await apiClient(`/vocabularies/${this.vocabId}/notes`, {
          method: 'POST',
          body: payload
        });
        showToast({
          type: 'success',
          title: 'Thành công',
          message: 'Tạo ghi chú mới thành công.'
        });
        this.showFeedback('Tạo ghi chú mới thành công!', 'success');
        this.resetComposer();
        // Load first page to show the newly created note at the top (createdAt DESC)
        this.currentPage = 0;
        await this.loadNotes();
      }
    } catch (err) {
      console.error('[PersonalNotes] Save error:', err);
      let msg = err.message || 'Lỗi khi lưu ghi chú.';
      if (err.status === 403) {
        msg = 'Bạn không có quyền chỉnh sửa ghi chú này.';
      } else if (err.status === 404) {
        msg = 'Từ vựng hoặc ghi chú không tồn tại.';
      } else if (err.status === 400 && Array.isArray(err.errors) && err.errors.length > 0) {
        msg = err.errors.join(', ');
      }
      this.showFeedback(msg, 'danger');
    } finally {
      this.isSubmitting = false;
      if (this.elements.submitBtn) {
        this.elements.submitBtn.disabled = false;
        this.elements.submitBtn.textContent = this.editingNoteId ? 'Cập nhật' : 'Lưu ghi chú';
      }
    }
  }

  /**
   * Deletes a note item via DELETE /api/v1/notes/{noteId}.
   * Backend returns HTTP 200 OK with ApiResponse<Void> envelope.
   * 
   * @param {number} noteId 
   * @param {HTMLButtonElement} btn 
   */
  async handleDeleteNote(noteId, btn) {
    if (btn) {
      btn.disabled = true;
      btn.textContent = 'Đang xóa...';
    }

    try {
      await apiClient(`/notes/${noteId}`, {
        method: 'DELETE'
      });

      showToast({
        type: 'success',
        title: 'Thành công',
        message: 'Đã xóa ghi chú thành công.'
      });

      // If user was currently editing this note, cancel editing
      if (this.editingNoteId === noteId) {
        this.cancelEditing();
      }

      // Check if current page is now empty and page > 0
      if (this.notes.length === 1 && this.currentPage > 0) {
        this.currentPage--;
      }

      await this.loadNotes();

    } catch (err) {
      console.error('[PersonalNotes] Delete error:', err);
      showToast({
        type: 'danger',
        title: 'Lỗi xóa ghi chú',
        message: err.message || 'Không thể xóa ghi chú này.'
      });
      if (btn) {
        btn.disabled = false;
        btn.textContent = 'Xóa ngay';
      }
    }
  }
}

// Global singleton instance
let modalInstance = null;

/**
 * Convenience entry point to open contextual personal notes.
 * 
 * @param {Object} options 
 * @param {number|string} options.vocabId 
 * @param {string} [options.hanzi] 
 * @param {string} [options.pinyin] 
 * @param {string} [options.meaning] 
 * @param {HTMLElement} [options.triggerElement] 
 */
export function openNotesModal(options) {
  if (!modalInstance) {
    modalInstance = new NotesModalController();
  }
  return modalInstance.open(options);
}
