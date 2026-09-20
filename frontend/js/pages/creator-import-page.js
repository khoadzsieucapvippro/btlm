/**
 * =============================================================================
 * CREATOR TWO-STEP EXCEL IMPORT CONTROLLER (TASK 9D.2)
 * Module: frontend/js/pages/creator-import-page.js
 * 
 * Responsibilities:
 * - Orchestrates Step 1 Preview & Step 2 Confirm workflow for Creator/Admin.
 * - Client pre-validation: .xlsx extension, non-empty, <= 10MB (UX defense-in-depth).
 * - Step 1: POST /api/v1/creator/lessons/import (multipart/form-data: 'file').
 *   Zero-mutation invariant: Does NOT persist any data into DB (read-only lookup).
 * - Visualizes ImportValidationReport (KPI metrics, validity badge, error list).
 * - Bounded table rendering for large sheets (up to 5,000 rows) with pagination
 *   and filtering (All, Errors, New, Existing).
 * - Critical State Consistency: Selecting a new file or removing file immediately
 *   invalidates and clears the previous preview report and locks Confirm.
 * - Step 2: POST /api/v1/creator/lessons/import/confirm (multipart/form-data: 'title', 'file').
 *   Atomic persistence: Uploads the ORIGINAL native File again; backend re-validates.
 * - Strict title validation: trimmed length 1..100 characters with live counter.
 * - On HTTP 201 Created: Navigates to creator-lesson-editor.html?id=<lessonId>.
 * - Safe DOM rendering: All Excel cell contents rendered safely (zero unsafe innerHTML).
 * =============================================================================
 */

import { apiClient, ApiError } from '../api/api.js';
import { authManager } from '../auth/auth-state.js';
import { initNavbarAuth } from '../ui/nav.js';
import { showToast } from '../ui/ui.js';
import { createSafeElement, clearContainer, escapeHtml } from '../ui/security.js';

export const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
export const MAX_IMPORT_TITLE_LENGTH = 100; // Authoritative backend constraint

/**
 * Validates an uploaded file on the client side (UX pre-check).
 * 
 * @param {File|null|undefined} file 
 * @returns {{ valid: boolean, error?: string }}
 */
export function validateExcelFile(file) {
  if (!file) {
    return { valid: false, error: 'Vui lòng chọn một tệp Excel để tiếp tục.' };
  }

  const name = String(file.name || '').trim();
  const lowerName = name.toLowerCase();

  if (!lowerName.endsWith('.xlsx')) {
    return {
      valid: false,
      error: 'Định dạng tệp không hợp lệ. Hệ thống chỉ chấp nhận tệp bảng tính Excel đuôi .xlsx chuẩn.'
    };
  }

  if (file.size === 0) {
    return {
      valid: false,
      error: 'Tệp đã chọn rỗng (kích thước 0 bytes). Vui lòng chọn tệp chứa dữ liệu từ vựng.'
    };
  }

  if (file.size > MAX_FILE_SIZE_BYTES) {
    return {
      valid: false,
      error: `Dung lượng tệp (${formatFileSize(file.size)}) vượt quá giới hạn tối đa cho phép là 10 MB.`
    };
  }

  return { valid: true };
}

/**
 * Validates a lesson title for Step 2 Confirm.
 * Enforces the authoritative 1..100 trimmed character limit from CreatorLessonServiceImpl.
 * 
 * @param {string|null|undefined} title 
 * @returns {{ valid: boolean, sanitizedTitle: string, error?: string }}
 */
export function validateImportTitle(title) {
  if (title === null || title === undefined) {
    return { valid: false, sanitizedTitle: '', error: 'Tiêu đề bài học không được để trống.' };
  }

  const trimmed = String(title).trim();

  if (trimmed.length === 0) {
    return { valid: false, sanitizedTitle: '', error: 'Tiêu đề bài học không được để trống.' };
  }

  if (trimmed.length > MAX_IMPORT_TITLE_LENGTH) {
    return {
      valid: false,
      sanitizedTitle: trimmed,
      error: `Tiêu đề bài học không được vượt quá ${MAX_IMPORT_TITLE_LENGTH} ký tự (hiện tại: ${trimmed.length} ký tự).`
    };
  }

  return { valid: true, sanitizedTitle: trimmed };
}

/**
 * Formats a byte size into human-readable representation.
 * 
 * @param {number} bytes 
 * @returns {string}
 */
export function formatFileSize(bytes) {
  if (typeof bytes !== 'number' || isNaN(bytes) || bytes < 0) {
    return '0 B';
  }
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  const kb = bytes / 1024;
  if (kb < 1024) {
    return `${kb.toFixed(1)} KB`;
  }
  const mb = kb / 1024;
  return `${mb.toFixed(2)} MB`;
}

/**
 * Normalizes and parses the backend ImportValidationReport into a reliable model.
 * 
 * @param {object} reportData 
 * @returns {object}
 */
export function parseValidationReport(reportData) {
  const data = reportData?.data || reportData || {};

  return {
    isValid: Boolean(data.isValid),
    totalRows: Number(data.totalRows) || 0,
    validRowsCount: Number(data.validRowsCount) || 0,
    invalidRowsCount: Number(data.invalidRowsCount) || 0,
    newVocabCount: Number(data.newVocabCount) || 0,
    existingVocabCount: Number(data.existingVocabCount) || 0,
    fileStatus: String(data.fileStatus || (data.isValid ? 'VALID' : 'INVALID')),
    summaryMessage: String(data.summaryMessage || ''),
    rows: Array.isArray(data.rows) ? data.rows : [],
    errors: Array.isArray(data.errors) ? data.errors : []
  };
}

/**
 * Filters parsed rows based on selected filter tab.
 * 
 * @param {Array<object>} rows 
 * @param {'all'|'errors'|'new'|'existing'} filterMode 
 * @returns {Array<object>}
 */
export function filterReportRows(rows, filterMode = 'all') {
  if (!Array.isArray(rows)) return [];

  switch (filterMode) {
    case 'errors':
      return rows.filter(r => !r.isValid || (Array.isArray(r.errors) && r.errors.length > 0));
    case 'new':
      return rows.filter(r => !r.isExisting);
    case 'existing':
      return rows.filter(r => Boolean(r.isExisting));
    case 'all':
    default:
      return rows;
  }
}

/**
 * Applies bounded pagination to report rows.
 * 
 * @param {Array<object>} rows 
 * @param {number} page 1-indexed
 * @param {number} pageSize 
 * @returns {{ items: Array<object>, totalPages: number, currentPage: number, totalItems: number }}
 */
export function paginateReportRows(rows, page = 1, pageSize = 25) {
  const totalItems = Array.isArray(rows) ? rows.length : 0;
  const safePageSize = Math.max(1, Number(pageSize) || 25);
  const totalPages = Math.max(1, Math.ceil(totalItems / safePageSize));
  const currentPage = Math.min(Math.max(1, Number(page) || 1), totalPages);

  const startIndex = (currentPage - 1) * safePageSize;
  const items = Array.isArray(rows) ? rows.slice(startIndex, startIndex + safePageSize) : [];

  return {
    items,
    totalPages,
    currentPage,
    totalItems
  };
}

/**
 * Builds FormData for Step 1 Preview.
 * 
 * @param {File} file 
 * @returns {FormData}
 */
export function buildPreviewFormData(file) {
  const fd = new FormData();
  fd.append('file', file);
  return fd;
}

/**
 * Builds FormData for Step 2 Confirm.
 * 
 * @param {string} title 
 * @param {File} file 
 * @returns {FormData}
 */
export function buildConfirmFormData(title, file) {
  const fd = new FormData();
  fd.append('title', String(title).trim());
  fd.append('file', file);
  return fd;
}

/**
 * Main Controller Class for Creator Import Page.
 */
export class CreatorImportController {
  constructor() {
    this.state = {
      file: null,
      previewReport: null,
      filterMode: 'all',
      currentPage: 1,
      pageSize: 25,
      isPreviewing: false,
      isConfirming: false
    };

    // DOM Elements Cache
    this.authGuardContainer = document.getElementById('creatorAuthGuardContainer');
    this.authGuardMessage = document.getElementById('creatorAuthGuardMessage');
    this.mainContent = document.getElementById('creatorImportContent');

    // Step 1 Elements
    this.stepIndicator1 = document.getElementById('stepIndicator1');
    this.stepIndicator2 = document.getElementById('stepIndicator2');
    this.step1Section = document.getElementById('step1Section');
    this.fileDropZone = document.getElementById('fileDropZone');
    this.btnBrowseFile = document.getElementById('btnBrowseFile');
    this.fileInput = document.getElementById('excelFileInput');
    this.fileInfoCard = document.getElementById('fileInfoCard');
    this.selectedFileName = document.getElementById('selectedFileName');
    this.selectedFileSize = document.getElementById('selectedFileSize');
    this.selectedFileExt = document.getElementById('selectedFileExt');
    this.btnRemoveFile = document.getElementById('btnRemoveFile');
    this.clientFileErrorNotice = document.getElementById('clientFileErrorNotice');
    this.btnPreviewFile = document.getElementById('btnPreviewFile');
    this.btnPreviewSpinner = document.getElementById('btnPreviewSpinner');
    this.btnPreviewText = document.getElementById('btnPreviewText');

    // Preview Report Elements
    this.previewReportSection = document.getElementById('previewReportSection');
    this.reportStatusBadgeContainer = document.getElementById('reportStatusBadgeContainer');
    this.reportSummaryAlert = document.getElementById('reportSummaryAlert');
    this.metricTotalRows = document.getElementById('metricTotalRows');
    this.metricValidRows = document.getElementById('metricValidRows');
    this.metricInvalidRows = document.getElementById('metricInvalidRows');
    this.metricNewVocabs = document.getElementById('metricNewVocabs');
    this.metricExistingVocabs = document.getElementById('metricExistingVocabs');
    this.fileLevelErrorsContainer = document.getElementById('fileLevelErrorsContainer');
    this.fileLevelErrorsList = document.getElementById('fileLevelErrorsList');

    // Table & Filter Elements
    this.filterAllBtn = document.getElementById('filterAllBtn');
    this.filterErrorsBtn = document.getElementById('filterErrorsBtn');
    this.filterNewBtn = document.getElementById('filterNewBtn');
    this.filterExistingBtn = document.getElementById('filterExistingBtn');
    this.filterAllCount = document.getElementById('filterAllCount');
    this.filterErrorsCount = document.getElementById('filterErrorsCount');
    this.filterNewCount = document.getElementById('filterNewCount');
    this.filterExistingCount = document.getElementById('filterExistingCount');
    this.tableDisplayCount = document.getElementById('tableDisplayCount');
    this.previewTableBody = document.getElementById('previewTableBody');
    this.pageSizeSelect = document.getElementById('pageSizeSelect');
    this.btnPrevPage = document.getElementById('btnPrevPage');
    this.btnNextPage = document.getElementById('btnNextPage');
    this.pageIndicator = document.getElementById('pageIndicator');

    // Step 2 Confirm Elements
    this.step2Section = document.getElementById('step2Section');
    this.confirmImportForm = document.getElementById('confirmImportForm');
    this.importLessonTitle = document.getElementById('importLessonTitle');
    this.titleCharCounter = document.getElementById('titleCharCounter');
    this.importLessonTitleError = document.getElementById('importLessonTitleError');
    this.btnConfirmImport = document.getElementById('btnConfirmImport');
    this.btnConfirmSpinner = document.getElementById('btnConfirmSpinner');
    this.btnConfirmText = document.getElementById('btnConfirmText');
    this.btnResetImport = document.getElementById('btnResetImport');
    this.confirmStatusNotice = document.getElementById('confirmStatusNotice');
  }

  /**
   * Initializes the page controller and checks role capability.
   */
  async init() {
    initNavbarAuth();

    // Check capability: User must have Creator or Admin role
    const isAuth = authManager.isAuthenticated();
    const hasRole = authManager.hasRole('Creator') || authManager.hasRole('Admin');

    if (!isAuth) {
      this.showRoleGuard('Vui lòng đăng nhập với tài khoản Tác giả (Creator) hoặc Quản trị viên (Admin) để sử dụng chức năng nhập bài học.');
      return;
    }

    if (!hasRole) {
      this.showRoleGuard('Tài khoản của bạn chưa được cấp quyền Tác giả (Creator) hoặc Quản trị viên (Admin) để thực hiện tác vụ này.');
      return;
    }

    this.bindEvents();
  }

  /**
   * Displays the role guard notice and hides the main workflow.
   * 
   * @param {string} message 
   */
  showRoleGuard(message) {
    if (this.authGuardMessage) {
      this.authGuardMessage.textContent = message;
    }
    if (this.authGuardContainer) {
      this.authGuardContainer.classList.remove('d-none');
    }
    if (this.mainContent) {
      this.mainContent.classList.add('d-none');
    }
  }

  /**
   * Binds all user interaction and form events.
   */
  bindEvents() {
    // Browse button triggers hidden file input
    if (this.btnBrowseFile && this.fileInput) {
      this.btnBrowseFile.addEventListener('click', (e) => {
        e.stopPropagation();
        this.fileInput.click();
      });
    }

    // Dropzone click triggers hidden file input (excluding when clicking inner buttons)
    if (this.fileDropZone && this.fileInput) {
      this.fileDropZone.addEventListener('click', (e) => {
        if (e.target !== this.btnBrowseFile && !this.btnBrowseFile.contains(e.target)) {
          this.fileInput.click();
        }
      });

      // Drag and drop events
      ['dragenter', 'dragover'].forEach(eventType => {
        this.fileDropZone.addEventListener(eventType, (e) => {
          e.preventDefault();
          e.stopPropagation();
          this.fileDropZone.classList.add('dragover');
        });
      });

      ['dragleave', 'dragend'].forEach(eventType => {
        this.fileDropZone.addEventListener(eventType, (e) => {
          e.preventDefault();
          e.stopPropagation();
          this.fileDropZone.classList.remove('dragover');
        });
      });

      this.fileDropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        e.stopPropagation();
        this.fileDropZone.classList.remove('dragover');

        const files = e.dataTransfer?.files;
        if (files && files.length > 0) {
          this.handleFileSelected(files[0]);
        }
      });
    }

    // File input change handler
    if (this.fileInput) {
      this.fileInput.addEventListener('change', () => {
        const file = this.fileInput.files?.[0];
        if (file) {
          this.handleFileSelected(file);
        }
      });
    }

    // Remove / Change file button
    if (this.btnRemoveFile) {
      this.btnRemoveFile.addEventListener('click', () => {
        this.resetSelectedFile();
      });
    }

    // Step 1: Preview button click
    if (this.btnPreviewFile) {
      this.btnPreviewFile.addEventListener('click', () => {
        this.executePreview();
      });
    }

    // Table filter buttons
    if (this.filterAllBtn) {
      this.filterAllBtn.addEventListener('click', () => this.setFilterMode('all'));
    }
    if (this.filterErrorsBtn) {
      this.filterErrorsBtn.addEventListener('click', () => this.setFilterMode('errors'));
    }
    if (this.filterNewBtn) {
      this.filterNewBtn.addEventListener('click', () => this.setFilterMode('new'));
    }
    if (this.filterExistingBtn) {
      this.filterExistingBtn.addEventListener('click', () => this.setFilterMode('existing'));
    }

    // Table page size selector
    if (this.pageSizeSelect) {
      this.pageSizeSelect.addEventListener('change', () => {
        this.state.pageSize = Number(this.pageSizeSelect.value) || 25;
        this.state.currentPage = 1;
        this.renderTableContent();
      });
    }

    // Table pagination buttons
    if (this.btnPrevPage) {
      this.btnPrevPage.addEventListener('click', () => {
        if (this.state.currentPage > 1) {
          this.state.currentPage--;
          this.renderTableContent();
        }
      });
    }
    if (this.btnNextPage) {
      this.btnNextPage.addEventListener('click', () => {
        const filtered = filterReportRows(this.state.previewReport?.rows, this.state.filterMode);
        const { totalPages } = paginateReportRows(filtered, this.state.currentPage, this.state.pageSize);
        if (this.state.currentPage < totalPages) {
          this.state.currentPage++;
          this.renderTableContent();
        }
      });
    }

    // Step 2: Live Title character counter and validation
    if (this.importLessonTitle) {
      this.importLessonTitle.addEventListener('input', () => {
        this.updateTitleCounter();
      });
    }

    // Step 2: Confirm Form Submission
    if (this.confirmImportForm) {
      this.confirmImportForm.addEventListener('submit', (e) => {
        e.preventDefault();
        this.executeConfirm();
      });
    }

    // Reset button
    if (this.btnResetImport) {
      this.btnResetImport.addEventListener('click', () => {
        this.resetEntireWorkflow();
      });
    }
  }

  /**
   * Handles user selecting a file (via file dialog or drop).
   * CRITICAL INVARIANT: Changing file invalidates any existing preview report immediately.
   * 
   * @param {File} file 
   */
  handleFileSelected(file) {
    // 1. Immediately invalidate and clear previous preview state
    this.invalidatePreviewState();

    // 2. Client UX pre-validation
    const check = validateExcelFile(file);

    if (!check.valid) {
      this.state.file = null;
      if (this.clientFileErrorNotice) {
        this.clientFileErrorNotice.textContent = check.error;
        this.clientFileErrorNotice.classList.remove('d-none');
      }
      if (this.fileInfoCard) {
        this.fileInfoCard.classList.add('d-none');
      }
      if (this.btnPreviewFile) {
        this.btnPreviewFile.disabled = true;
      }
      return;
    }

    // 3. File is valid on client side
    this.state.file = file;

    if (this.clientFileErrorNotice) {
      this.clientFileErrorNotice.classList.add('d-none');
      this.clientFileErrorNotice.textContent = '';
    }

    // Render file metadata card
    if (this.selectedFileName) {
      this.selectedFileName.textContent = file.name;
    }
    if (this.selectedFileSize) {
      this.selectedFileSize.textContent = formatFileSize(file.size);
    }
    if (this.selectedFileExt) {
      const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
      this.selectedFileExt.textContent = ext || '.xlsx';
    }
    if (this.fileInfoCard) {
      this.fileInfoCard.classList.remove('d-none');
    }

    if (this.btnPreviewFile) {
      this.btnPreviewFile.disabled = false;
      this.btnPreviewFile.focus();
    }
  }

  /**
   * Resets selected file and returns to empty file selection state.
   */
  resetSelectedFile() {
    this.state.file = null;
    if (this.fileInput) {
      this.fileInput.value = '';
    }
    if (this.fileInfoCard) {
      this.fileInfoCard.classList.add('d-none');
    }
    if (this.clientFileErrorNotice) {
      this.clientFileErrorNotice.classList.add('d-none');
      this.clientFileErrorNotice.textContent = '';
    }
    if (this.btnPreviewFile) {
      this.btnPreviewFile.disabled = true;
    }
    this.invalidatePreviewState();
  }

  /**
   * Invalidates preview and Step 2 confirm state whenever file changes.
   */
  invalidatePreviewState() {
    this.state.previewReport = null;
    this.state.currentPage = 1;
    this.state.filterMode = 'all';

    // Hide preview report section
    if (this.previewReportSection) {
      this.previewReportSection.classList.add('d-none');
    }
    // Hide Step 2 section
    if (this.step2Section) {
      this.step2Section.classList.add('d-none');
    }

    // Reset step indicators
    if (this.stepIndicator1) {
      this.stepIndicator1.className = 'creator-step-badge creator-step-active';
    }
    if (this.stepIndicator2) {
      this.stepIndicator2.className = 'creator-step-badge creator-step-idle';
    }

    // Reset title form
    if (this.importLessonTitle) {
      this.importLessonTitle.value = '';
      this.importLessonTitle.classList.remove('is-invalid');
    }
    if (this.importLessonTitleError) {
      this.importLessonTitleError.classList.add('d-none');
      this.importLessonTitleError.textContent = '';
    }
    this.updateTitleCounter();
  }

  /**
   * Completely resets the entire workflow back to initial state.
   */
  resetEntireWorkflow() {
    this.resetSelectedFile();
    if (this.step1Section) {
      this.step1Section.scrollIntoView({ behavior: 'smooth' });
    }
  }

  /**
   * Executes Step 1: Upload Excel file to preview endpoint.
   * Endpoint: POST /api/v1/creator/lessons/import (multipart/form-data: 'file').
   */
  async executePreview() {
    if (!this.state.file || this.state.isPreviewing) return;

    const fileCheck = validateExcelFile(this.state.file);
    if (!fileCheck.valid) {
      showToast({ type: 'warning', title: 'Tệp không hợp lệ', message: fileCheck.error });
      return;
    }

    this.setPreviewLoading(true);

    try {
      const formData = buildPreviewFormData(this.state.file);
      const response = await apiClient('/creator/lessons/import', {
        method: 'POST',
        body: formData
      });

      const report = parseValidationReport(response);
      this.state.previewReport = report;

      this.renderPreviewReport(report);
    } catch (err) {
      let msg = err.message || 'Lỗi khi gửi tệp lên máy chủ để xem trước.';

      if (err.status === 413 || err.code === 'FILE_TOO_LARGE') {
        msg = 'Dung lượng tệp vượt quá giới hạn 10 MB của máy chủ.';
      } else if (err.status === 401) {
        msg = 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.';
      } else if (err.status === 403) {
        msg = 'Bạn không có quyền thực hiện chức năng nhập bài học.';
      }

      showToast({ type: 'danger', title: 'Xem trước thất bại', message: msg });
      if (this.clientFileErrorNotice) {
        this.clientFileErrorNotice.textContent = msg;
        this.clientFileErrorNotice.classList.remove('d-none');
      }
    } finally {
      this.setPreviewLoading(false);
    }
  }

  /**
   * Sets the in-flight loading state for Step 1 Preview button.
   * 
   * @param {boolean} loading 
   */
  setPreviewLoading(loading) {
    this.state.isPreviewing = loading;
    if (this.btnPreviewFile) {
      this.btnPreviewFile.disabled = loading;
    }
    if (this.btnPreviewSpinner) {
      this.btnPreviewSpinner.classList.toggle('d-none', !loading);
    }
    if (this.btnPreviewText) {
      this.btnPreviewText.textContent = loading ? 'Đang phân tích dữ liệu...' : 'Xem trước (Preview)';
    }
  }

  /**
   * Renders the parsed ImportValidationReport into the UI.
   * 
   * @param {object} report 
   */
  renderPreviewReport(report) {
    if (!this.previewReportSection) return;

    this.previewReportSection.classList.remove('d-none');

    // 1. Render Status Badge
    if (this.reportStatusBadgeContainer) {
      clearContainer(this.reportStatusBadgeContainer);
      let badgeClass = 'badge-status-draft';
      let badgeText = report.fileStatus;

      if (report.isValid) {
        badgeClass = 'badge-status-approved';
        badgeText = 'HỢP LỆ (100%)';
      } else if (report.invalidRowsCount > 0) {
        badgeClass = 'badge-status-pending';
        badgeText = `CHỨA ${report.invalidRowsCount} DÒNG LỖI`;
      } else {
        badgeClass = 'badge-status-rejected';
        badgeText = report.fileStatus || 'LỖI TỆP';
      }

      this.reportStatusBadgeContainer.appendChild(createSafeElement('span', {
        className: `badge-status ${badgeClass}`,
        text: badgeText
      }));
    }

    // 2. Render Summary Alert
    if (this.reportSummaryAlert) {
      this.reportSummaryAlert.className = `alert mb-4 small ${report.isValid ? 'alert-success' : 'alert-warning'}`;
      let message = report.summaryMessage;
      if (!message) {
        message = report.isValid 
          ? `Tệp Excel hoàn toàn hợp lệ (${report.validRowsCount}/${report.totalRows} dòng). Sẵn sàng chuyển sang Bước 2 để đặt tên và xác nhận lưu.`
          : `Phát hiện ${report.invalidRowsCount} dòng không hợp lệ trong tổng số ${report.totalRows} dòng. Vui lòng kiểm tra chi tiết lỗi bên dưới và sửa lại tệp trước khi xác nhận.`;
      }
      this.reportSummaryAlert.textContent = message;
    }

    // 3. Populate 4 KPI Metrics Cards
    if (this.metricTotalRows) this.metricTotalRows.textContent = String(report.totalRows);
    if (this.metricValidRows) this.metricValidRows.textContent = String(report.validRowsCount);
    if (this.metricInvalidRows) this.metricInvalidRows.textContent = String(report.invalidRowsCount);
    if (this.metricNewVocabs) this.metricNewVocabs.textContent = String(report.newVocabCount);
    if (this.metricExistingVocabs) this.metricExistingVocabs.textContent = String(report.existingVocabCount);

    // 4. Render File-Level Errors (if present)
    if (this.fileLevelErrorsContainer && this.fileLevelErrorsList) {
      clearContainer(this.fileLevelErrorsList);
      const fileErrors = report.errors.filter(e => e.rowNumber === null || e.columnName === 'FILE');

      if (fileErrors.length > 0) {
        fileErrors.forEach(err => {
          this.fileLevelErrorsList.appendChild(createSafeElement('li', {
            text: err.errorMessage || err.errorCode || 'Lỗi cấu trúc tệp không xác định'
          }));
        });
        this.fileLevelErrorsContainer.classList.remove('d-none');
      } else {
        this.fileLevelErrorsContainer.classList.add('d-none');
      }
    }

    // 5. Update Filter Tab Counts
    if (this.filterAllCount) this.filterAllCount.textContent = String(report.rows.length);
    const errorRowsCount = report.rows.filter(r => !r.isValid || (r.errors && r.errors.length > 0)).length;
    const newRowsCount = report.rows.filter(r => !r.isExisting).length;
    const existingRowsCount = report.rows.filter(r => Boolean(r.isExisting)).length;

    if (this.filterErrorsCount) this.filterErrorsCount.textContent = String(errorRowsCount);
    if (this.filterNewCount) this.filterNewCount.textContent = String(newRowsCount);
    if (this.filterExistingCount) this.filterExistingCount.textContent = String(existingRowsCount);

    // 6. Reset table filter and pagination
    this.state.filterMode = 'all';
    this.state.currentPage = 1;
    this.updateFilterButtonsUI();
    this.renderTableContent();

    // 7. Step 2 Visibility: Unlocks Step 2 ONLY IF preview is 100% valid
    if (report.isValid && report.totalRows > 0) {
      if (this.step2Section) {
        this.step2Section.classList.remove('d-none');
      }
      if (this.stepIndicator1) {
        this.stepIndicator1.className = 'creator-step-badge creator-step-done';
      }
      if (this.stepIndicator2) {
        this.stepIndicator2.className = 'creator-step-badge creator-step-active';
      }

      // Pre-fill suggested title based on file name if empty
      if (this.importLessonTitle && !this.importLessonTitle.value.trim() && this.state.file?.name) {
        const baseName = this.state.file.name.replace(/\.[^/.]+$/, '').trim();
        if (baseName.length <= MAX_IMPORT_TITLE_LENGTH) {
          this.importLessonTitle.value = baseName;
          this.updateTitleCounter();
        }
      }

      showToast({
        type: 'success',
        title: 'Xem trước thành công',
        message: `Tệp hợp lệ 100% (${report.validRowsCount} từ vựng). Bạn có thể tiếp tục chuyển sang Bước 2.`
      });
    } else {
      // Invalid report: Step 2 remains locked/hidden
      if (this.step2Section) {
        this.step2Section.classList.add('d-none');
      }
      if (this.stepIndicator1) {
        this.stepIndicator1.className = 'creator-step-badge creator-step-active';
      }
      if (this.stepIndicator2) {
        this.stepIndicator2.className = 'creator-step-badge creator-step-idle';
      }

      showToast({
        type: 'warning',
        title: 'Phát hiện lỗi',
        message: 'Tệp chứa dòng không hợp lệ. Vui lòng xem chi tiết bảng bên dưới để chỉnh sửa tệp.'
      });
    }

    // Smooth scroll to report
    this.previewReportSection.scrollIntoView({ behavior: 'smooth' });
  }

  /**
   * Sets the active filter tab for the preview table.
   * 
   * @param {'all'|'errors'|'new'|'existing'} mode 
   */
  setFilterMode(mode) {
    this.state.filterMode = mode;
    this.state.currentPage = 1;
    this.updateFilterButtonsUI();
    this.renderTableContent();
  }

  /**
   * Updates active styling of the filter button group.
   */
  updateFilterButtonsUI() {
    const buttons = [
      { el: this.filterAllBtn, mode: 'all' },
      { el: this.filterErrorsBtn, mode: 'errors' },
      { el: this.filterNewBtn, mode: 'new' },
      { el: this.filterExistingBtn, mode: 'existing' }
    ];

    buttons.forEach(({ el, mode }) => {
      if (!el) return;
      if (mode === this.state.filterMode) {
        el.classList.add('active');
      } else {
        el.classList.remove('active');
      }
    });
  }

  /**
   * Renders the bounded page of table rows safely using DOM APIs.
   */
  renderTableContent() {
    if (!this.previewTableBody) return;

    clearContainer(this.previewTableBody);

    const allRows = this.state.previewReport?.rows || [];
    const filteredRows = filterReportRows(allRows, this.state.filterMode);
    const pagination = paginateReportRows(filteredRows, this.state.currentPage, this.state.pageSize);

    // Update display counter label
    if (this.tableDisplayCount) {
      const from = pagination.totalItems === 0 ? 0 : (pagination.currentPage - 1) * this.state.pageSize + 1;
      const to = Math.min(pagination.currentPage * this.state.pageSize, pagination.totalItems);
      this.tableDisplayCount.textContent = `Hiển thị ${from}–${to} trong ${pagination.totalItems} dòng (${allRows.length} tổng tệp)`;
    }

    // Update pagination controls
    if (this.pageIndicator) {
      this.pageIndicator.textContent = `Trang ${pagination.currentPage} / ${pagination.totalPages}`;
    }
    if (this.btnPrevPage) {
      this.btnPrevPage.disabled = pagination.currentPage <= 1;
    }
    if (this.btnNextPage) {
      this.btnNextPage.disabled = pagination.currentPage >= pagination.totalPages;
    }

    if (pagination.items.length === 0) {
      const tr = document.createElement('tr');
      const td = document.createElement('td');
      td.colSpan = 8;
      td.className = 'text-center py-4 text-secondary fst-italic';
      td.textContent = 'Không có dòng nào phù hợp với bộ lọc đã chọn.';
      tr.appendChild(td);
      this.previewTableBody.appendChild(tr);
      return;
    }

    // Render table rows safely
    pagination.items.forEach(item => {
      const tr = document.createElement('tr');
      if (!item.isValid) {
        tr.className = 'table-danger';
      }

      // 1. Row number
      const tdRow = document.createElement('td');
      tdRow.className = 'text-center font-monospace fw-semibold';
      tdRow.textContent = String(item.rowNumber ?? '--');
      tr.appendChild(tdRow);

      // 2. Hanzi
      const tdHanzi = document.createElement('td');
      tdHanzi.className = 'cjk-character fw-bold fs-6';
      tdHanzi.textContent = item.hanzi || '--';
      tr.appendChild(tdHanzi);

      // 3. Pinyin
      const tdPinyin = document.createElement('td');
      tdPinyin.className = 'font-monospace';
      tdPinyin.textContent = item.pinyin || '--';
      tr.appendChild(tdPinyin);

      // 4. Meaning Han-Viet
      const tdHanViet = document.createElement('td');
      tdHanViet.textContent = item.meaningHanViet || '--';
      tr.appendChild(tdHanViet);

      // 5. Meaning Vietnamese
      const tdMeaningVi = document.createElement('td');
      tdMeaningVi.textContent = item.meaningVi || '--';
      tr.appendChild(tdMeaningVi);

      // 6. Classification badge (New vs Existing)
      const tdClassification = document.createElement('td');
      tdClassification.className = 'text-center';
      if (item.isExisting) {
        tdClassification.appendChild(createSafeElement('span', {
          className: 'badge bg-secondary-subtle text-secondary border border-secondary-subtle',
          text: 'Đã có'
        }));
      } else {
        tdClassification.appendChild(createSafeElement('span', {
          className: 'badge bg-primary-subtle text-primary border border-primary-subtle',
          text: 'Từ mới'
        }));
      }
      tr.appendChild(tdClassification);

      // 7. Validity status badge
      const tdStatus = document.createElement('td');
      tdStatus.className = 'text-center';
      if (item.isValid) {
        tdStatus.appendChild(createSafeElement('span', {
          className: 'badge bg-success-subtle text-success border border-success-subtle',
          text: '✓ Hợp lệ'
        }));
      } else {
        tdStatus.appendChild(createSafeElement('span', {
          className: 'badge bg-danger-subtle text-danger border border-danger-subtle',
          text: '✗ Lỗi'
        }));
      }
      tr.appendChild(tdStatus);

      // 8. Errors / Notes detail
      const tdErrors = document.createElement('td');
      if (Array.isArray(item.errors) && item.errors.length > 0) {
        const errContainer = document.createElement('div');
        errContainer.className = 'text-danger small fw-semibold';
        item.errors.forEach(errMsg => {
          const div = document.createElement('div');
          div.textContent = `• ${errMsg}`;
          errContainer.appendChild(div);
        });
        tdErrors.appendChild(errContainer);
      } else {
        tdErrors.className = 'text-secondary fst-italic small';
        tdErrors.textContent = item.exampleSentence ? `Ví dụ: ${item.exampleSentence}` : 'Đạt chuẩn';
      }
      tr.appendChild(tdErrors);

      this.previewTableBody.appendChild(tr);
    });
  }

  /**
   * Updates the character counter and visual validity of the lesson title input.
   */
  updateTitleCounter() {
    if (!this.importLessonTitle || !this.titleCharCounter) return;

    const length = this.importLessonTitle.value.trim().length;
    this.titleCharCounter.textContent = `${length} / ${MAX_IMPORT_TITLE_LENGTH}`;

    if (length > MAX_IMPORT_TITLE_LENGTH) {
      this.titleCharCounter.className = 'small text-danger fw-bold font-monospace';
      this.importLessonTitle.classList.add('is-invalid');
      if (this.importLessonTitleError) {
        this.importLessonTitleError.textContent = `Tiêu đề vượt quá ${MAX_IMPORT_TITLE_LENGTH} ký tự cho phép.`;
        this.importLessonTitleError.classList.remove('d-none');
      }
    } else {
      this.titleCharCounter.className = 'small text-secondary font-monospace';
      this.importLessonTitle.classList.remove('is-invalid');
      if (this.importLessonTitleError) {
        this.importLessonTitleError.classList.add('d-none');
        this.importLessonTitleError.textContent = '';
      }
    }
  }

  /**
   * Executes Step 2: Confirm lesson creation.
   * Endpoint: POST /api/v1/creator/lessons/import/confirm (multipart/form-data: 'title', 'file').
   * Atomic persistence: Backend re-validates the uploaded File and creates Draft lesson atomically.
   */
  async executeConfirm() {
    if (this.state.isConfirming) return;

    // Check critical state invariants
    if (!this.state.file) {
      showToast({ type: 'danger', title: 'Thiếu tệp', message: 'Tệp Excel không tồn tại. Vui lòng chọn tệp và xem trước lại.' });
      return;
    }

    if (!this.state.previewReport || !this.state.previewReport.isValid) {
      showToast({ type: 'warning', title: 'Chưa xem trước', message: 'Bạn phải xem trước và đảm bảo tệp hợp lệ 100% trước khi xác nhận.' });
      return;
    }

    // Validate title
    const rawTitle = this.importLessonTitle ? this.importLessonTitle.value : '';
    const titleCheck = validateImportTitle(rawTitle);

    if (!titleCheck.valid) {
      if (this.importLessonTitle) {
        this.importLessonTitle.classList.add('is-invalid');
        this.importLessonTitle.focus();
      }
      if (this.importLessonTitleError) {
        this.importLessonTitleError.textContent = titleCheck.error;
        this.importLessonTitleError.classList.remove('d-none');
      }
      showToast({ type: 'warning', title: 'Tiêu đề không hợp lệ', message: titleCheck.error });
      return;
    }

    // Set mutation lock
    this.setConfirmLoading(true);

    try {
      const confirmFormData = buildConfirmFormData(titleCheck.sanitizedTitle, this.state.file);
      const response = await apiClient('/creator/lessons/import/confirm', {
        method: 'POST',
        body: confirmFormData
      });

      const lesson = response?.data || response;
      const lessonId = lesson?.lessonId;

      if (!lessonId) {
        throw new Error('Máy chủ phản hồi thành công nhưng không trả về mã bài học hợp lệ.');
      }

      showToast({
        type: 'success',
        title: 'Import thành công!',
        message: `Đã tạo bài học '${lesson.title || titleCheck.sanitizedTitle}' (ID #${lessonId}) ở trạng thái Draft.`
      });

      if (this.confirmStatusNotice) {
        this.confirmStatusNotice.textContent = 'Import thành công! Đang chuyển hướng sang Creator Studio...';
      }

      // Navigate directly to the editor for the newly created Draft lesson
      setTimeout(() => {
        window.location.href = `creator-lesson-editor.html?id=${encodeURIComponent(lessonId)}`;
      }, 500);

    } catch (err) {
      let msg = err.message || 'Lỗi khi xác nhận lưu bài học từ Excel.';

      if (err.status === 422 || err.code === 'UNPROCESSABLE_ENTITY') {
        msg = 'Import không được lưu. Tệp Excel chứa lỗi hoặc không hợp lệ khi máy chủ kiểm tra lại.';
      } else if (err.status === 413 || err.code === 'FILE_TOO_LARGE') {
        msg = 'Tệp tải lên vượt quá giới hạn 10 MB của máy chủ.';
      } else if (err.status === 400) {
        msg = err.message || 'Dữ liệu yêu cầu không hợp lệ.';
      } else if (err.status === 401) {
        msg = 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.';
      } else if (err.status === 403) {
        msg = 'Bạn không có quyền xác nhận tạo bài học.';
      }

      showToast({ type: 'danger', title: 'Xác nhận thất bại', message: msg });
      if (this.confirmStatusNotice) {
        this.confirmStatusNotice.textContent = msg;
      }
      this.setConfirmLoading(false);
    }
  }

  /**
   * Sets in-flight loading and mutation lock for Step 2 Confirm button.
   * 
   * @param {boolean} loading 
   */
  setConfirmLoading(loading) {
    this.state.isConfirming = loading;
    if (this.btnConfirmImport) {
      this.btnConfirmImport.disabled = loading;
    }
    if (this.btnConfirmSpinner) {
      this.btnConfirmSpinner.classList.toggle('d-none', !loading);
    }
    if (this.btnConfirmText) {
      this.btnConfirmText.textContent = loading ? 'Đang lưu vào CSDL...' : 'Xác nhận Import bài học';
    }
    if (this.btnResetImport) {
      this.btnResetImport.disabled = loading;
    }
  }
}

/**
 * Page entry point executed on DOMContentLoaded.
 * Guarded against execution on non-import pages.
 * 
 * @returns {CreatorImportController|null}
 */
export function initCreatorImportPage() {
  if (typeof document === 'undefined') return null;

  // Only run when creatorImportContent exists
  const root = document.getElementById('creatorImportContent');
  if (!root) return null;

  const controller = new CreatorImportController();
  controller.init();
  return controller;
}

if (typeof window !== 'undefined' && typeof document !== 'undefined') {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      initCreatorImportPage();
    });
  } else {
    initCreatorImportPage();
  }
}

