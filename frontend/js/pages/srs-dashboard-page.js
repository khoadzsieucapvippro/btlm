/**
 * =============================================================================
 * STUDY STATISTICS & SRS DAILY SETTINGS CONTROLLER (TASK 9C.4)
 * Module: frontend/js/pages/srs-dashboard-page.js
 *
 * Responsibilities:
 * - Authoritative display of 5 SRS study statistics from GET /api/v1/srs/stats:
 *   cardsDue, reviewsToday, newCardsToday, newCardsLimit, maxReviewLimit.
 * - Manage personal daily SRS settings via GET/PUT /api/v1/srs/settings:
 *   newCardsPerDay (> 0), maxReviewPerDay (> 0).
 * - Client-side validation: positive integer discipline, reject empty/zero/negative/float.
 * - Prevent duplicate submission and ensure immediate authoritative refresh upon PUT success.
 * - Zero fabricated analytics or client-side statistical math (backend is source of truth).
 * - Safe DOM updates (100% textContent / setAttribute; zero innerHTML sinks).
 * - Accessible live regions, focus management, and session-aware navigation.
 * =============================================================================
 */

import { apiClient, ApiError } from '../api/api.js';
import { authManager } from '../auth/auth-state.js';
import { initNavbarAuth } from '../ui/nav.js';
import { showToast } from '../ui/ui.js';

/**
 * Validates SRS Daily Settings input values.
 * Contract: Both fields are required and must be strictly positive integers (> 0).
 * 
 * @param {string|number} newCardsPerDay
 * @param {string|number} maxReviewPerDay
 * @returns {{ isValid: boolean, errors: { newCardsPerDay?: string, maxReviewPerDay?: string } }}
 */
export function validateSrsSettings(newCardsPerDay, maxReviewPerDay) {
  const errors = {};

  // Validate newCardsPerDay
  const newCardsStr = String(newCardsPerDay ?? '').trim();
  if (!newCardsStr) {
    errors.newCardsPerDay = 'Số lượng thẻ mới mỗi ngày không được để trống.';
  } else {
    const num = Number(newCardsStr);
    if (Number.isNaN(num)) {
      errors.newCardsPerDay = 'Số lượng thẻ mới phải là một số nguyên hợp lệ.';
    } else if (!Number.isInteger(num)) {
      errors.newCardsPerDay = 'Số lượng thẻ mới phải là số nguyên (không chứa phần thập phân).';
    } else if (num <= 0) {
      errors.newCardsPerDay = 'Số lượng thẻ mới phải là số nguyên dương lớn hơn 0.';
    }
  }

  // Validate maxReviewPerDay
  const maxReviewStr = String(maxReviewPerDay ?? '').trim();
  if (!maxReviewStr) {
    errors.maxReviewPerDay = 'Số lượt ôn tập tối đa không được để trống.';
  } else {
    const num = Number(maxReviewStr);
    if (Number.isNaN(num)) {
      errors.maxReviewPerDay = 'Số lượt ôn tập phải là một số nguyên hợp lệ.';
    } else if (!Number.isInteger(num)) {
      errors.maxReviewPerDay = 'Số lượt ôn tập phải là số nguyên (không chứa phần thập phân).';
    } else if (num <= 0) {
      errors.maxReviewPerDay = 'Số lượt ôn tập phải là số nguyên dương lớn hơn 0.';
    }
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
}

/**
 * Calculates a safe quota completion percentage (0 - 100%).
 * Defends against division by zero and clamps output.
 * 
 * @param {number} current Current progress count
 * @param {number} max Maximum limit
 * @returns {number} Integer between 0 and 100
 */
export function calculateQuotaPercentage(current, max) {
  const curNum = Number(current);
  const maxNum = Number(max);

  if (!Number.isFinite(curNum) || !Number.isFinite(maxNum) || maxNum <= 0 || curNum < 0) {
    return 0;
  }

  const pct = Math.round((curNum / maxNum) * 100);
  return Math.min(100, Math.max(0, pct));
}

/**
 * Constructs the sanitized PUT /api/v1/srs/settings payload.
 * Strictly avoids leaking any userId or accountId (IDOR protection).
 * 
 * @param {string|number} newCardsPerDay
 * @param {string|number} maxReviewPerDay
 * @returns {{ newCardsPerDay: number, maxReviewPerDay: number }}
 */
export function buildUpdateSettingsPayload(newCardsPerDay, maxReviewPerDay) {
  return {
    newCardsPerDay: parseInt(String(newCardsPerDay).trim(), 10),
    maxReviewPerDay: parseInt(String(maxReviewPerDay).trim(), 10)
  };
}

/**
 * Main Controller for the SRS Dashboard Page.
 */
export class SrsDashboardController {
  constructor() {
    // DOM Container Landmarks
    this.loadingState = document.getElementById('dashboardLoadingState');
    this.authState = document.getElementById('dashboardAuthState');
    this.errorState = document.getElementById('dashboardErrorState');
    this.errorMessage = document.getElementById('dashboardErrorMessage');
    this.contentSection = document.getElementById('dashboardContentSection');

    // Metric Value Sinks (Safe textContent only)
    this.statCardsDue = document.getElementById('statCardsDue');
    this.statReviewsToday = document.getElementById('statReviewsToday');
    this.statReviewsQuotaText = document.getElementById('statReviewsQuotaText');
    this.statReviewsProgressBar = document.getElementById('statReviewsProgressBar');
    this.statNewCardsToday = document.getElementById('statNewCardsToday');
    this.statNewCardsQuotaText = document.getElementById('statNewCardsQuotaText');
    this.statNewCardsProgressBar = document.getElementById('statNewCardsProgressBar');
    this.statNewCardsLimit = document.getElementById('statNewCardsLimit');
    this.statMaxReviewLimit = document.getElementById('statMaxReviewLimit');

    // Settings Form Elements
    this.settingsForm = document.getElementById('srsSettingsForm');
    this.newCardsPerDayInput = document.getElementById('newCardsPerDayInput');
    this.maxReviewPerDayInput = document.getElementById('maxReviewPerDayInput');
    this.newCardsPerDayError = document.getElementById('newCardsPerDayError');
    this.maxReviewPerDayError = document.getElementById('maxReviewPerDayError');
    this.btnSaveSettings = document.getElementById('btnSaveSettings');
    this.btnResetSettings = document.getElementById('btnResetSettings');
    this.settingsFormStatus = document.getElementById('settingsFormStatus');

    // Action Controls
    this.btnRefreshStats = document.getElementById('btnRefreshStats');
    this.btnDashboardRetry = document.getElementById('btnDashboardRetry');

    // In-memory Authoritative Cache
    this.authoritativeStats = null;
    this.authoritativeSettings = null;
    this.isSubmitting = false;
  }

  /**
   * Initializes the dashboard lifecycle, navbar auth, and event listeners.
   */
  async init() {
    initNavbarAuth();

    // Check Authentication Guard
    if (!authManager.isAuthenticated()) {
      this.showState('auth');
      return;
    }

    this.bindEvents();
    await this.loadDashboardData();
  }

  /**
   * Binds interactive DOM events.
   */
  bindEvents() {
    if (this.settingsForm) {
      this.settingsForm.addEventListener('submit', (e) => this.handleSettingsSubmit(e));
    }

    if (this.btnResetSettings) {
      this.btnResetSettings.addEventListener('click', () => this.handleResetSettings());
    }

    if (this.btnRefreshStats) {
      this.btnRefreshStats.addEventListener('click', () => this.handleRefreshStats());
    }

    if (this.btnDashboardRetry) {
      this.btnDashboardRetry.addEventListener('click', () => this.loadDashboardData());
    }

    // Clear inline field errors on user input
    if (this.newCardsPerDayInput) {
      this.newCardsPerDayInput.addEventListener('input', () => {
        if (this.newCardsPerDayError) this.newCardsPerDayError.textContent = '';
      });
    }

    if (this.maxReviewPerDayInput) {
      this.maxReviewPerDayInput.addEventListener('input', () => {
        if (this.maxReviewPerDayError) this.maxReviewPerDayError.textContent = '';
      });
    }
  }

  /**
   * Switches visible dashboard state container.
   * 
   * @param {'loading'|'auth'|'error'|'content'} state
   */
  showState(state) {
    if (this.loadingState) this.loadingState.classList.toggle('d-none', state !== 'loading');
    if (this.authState) this.authState.classList.toggle('d-none', state !== 'auth');
    if (this.errorState) this.errorState.classList.toggle('d-none', state !== 'error');
    if (this.contentSection) this.contentSection.classList.toggle('d-none', state !== 'content');
  }

  /**
   * Concurrently loads authoritative stats and settings from the backend.
   */
  async loadDashboardData() {
    this.showState('loading');

    try {
      const [stats, settings] = await Promise.all([
        apiClient('/srs/stats'),
        apiClient('/srs/settings')
      ]);

      this.authoritativeStats = stats;
      this.authoritativeSettings = settings;

      this.renderStatistics(stats);
      this.renderSettings(settings);

      this.showState('content');
    } catch (err) {
      console.error('[SrsDashboard] Failed to load dashboard data:', err);
      if (this.errorMessage) {
        this.errorMessage.textContent = err.message || 'Không thể tải dữ liệu thống kê từ máy chủ. Vui lòng thử lại sau.';
      }
      this.showState('error');
    }
  }

  /**
   * Renders the 5 authoritative backend statistics and quota indicators.
   * Safe DOM: strictly uses textContent and percentage width styling.
   * 
   * @param {object} stats StudyStatsResponse
   */
  renderStatistics(stats) {
    if (!stats) return;

    const cardsDue = stats.cardsDue ?? 0;
    const reviewsToday = stats.reviewsToday ?? 0;
    const newCardsToday = stats.newCardsToday ?? 0;
    const newCardsLimit = stats.newCardsLimit ?? 0;
    const maxReviewLimit = stats.maxReviewLimit ?? 0;

    // 1. Cards Due
    if (this.statCardsDue) {
      this.statCardsDue.textContent = String(cardsDue);
    }

    // 2. Reviews Today & Quota
    if (this.statReviewsToday) {
      this.statReviewsToday.textContent = String(reviewsToday);
    }
    if (this.statReviewsQuotaText) {
      this.statReviewsQuotaText.textContent = `${reviewsToday} / ${maxReviewLimit}`;
    }
    if (this.statReviewsProgressBar) {
      const reviewPct = calculateQuotaPercentage(reviewsToday, maxReviewLimit);
      this.statReviewsProgressBar.style.width = `${reviewPct}%`;
      this.statReviewsProgressBar.setAttribute('aria-valuenow', String(reviewPct));
    }

    // 3. New Cards Today & Quota
    if (this.statNewCardsToday) {
      this.statNewCardsToday.textContent = String(newCardsToday);
    }
    if (this.statNewCardsQuotaText) {
      this.statNewCardsQuotaText.textContent = `${newCardsToday} / ${newCardsLimit}`;
    }
    if (this.statNewCardsProgressBar) {
      const newPct = calculateQuotaPercentage(newCardsToday, newCardsLimit);
      this.statNewCardsProgressBar.style.width = `${newPct}%`;
      this.statNewCardsProgressBar.setAttribute('aria-valuenow', String(newPct));
    }

    // 4. New Cards Limit
    if (this.statNewCardsLimit) {
      this.statNewCardsLimit.textContent = String(newCardsLimit);
    }

    // 5. Max Review Limit
    if (this.statMaxReviewLimit) {
      this.statMaxReviewLimit.textContent = String(maxReviewLimit);
    }
  }

  /**
   * Populates the settings form inputs with authoritative values.
   * 
   * @param {object} settings UserSrsSettingResponse
   */
  renderSettings(settings) {
    if (!settings) return;

    if (this.newCardsPerDayInput && settings.newCardsPerDay !== undefined) {
      this.newCardsPerDayInput.value = String(settings.newCardsPerDay);
    }

    if (this.maxReviewPerDayInput && settings.maxReviewPerDay !== undefined) {
      this.maxReviewPerDayInput.value = String(settings.maxReviewPerDay);
    }

    // Clear any previous error messages
    if (this.newCardsPerDayError) this.newCardsPerDayError.textContent = '';
    if (this.maxReviewPerDayError) this.maxReviewPerDayError.textContent = '';
  }

  /**
   * Handles user submission of the settings form.
   * Validates client-side, prevents duplicate submissions, and on success,
   * immediately refreshes authoritative statistics and settings.
   * 
   * @param {Event} e 
   */
  async handleSettingsSubmit(e) {
    e.preventDefault();

    if (this.isSubmitting) return;

    const newCardsVal = this.newCardsPerDayInput ? this.newCardsPerDayInput.value : '';
    const maxReviewVal = this.maxReviewPerDayInput ? this.maxReviewPerDayInput.value : '';

    // 1. Client-side Validation
    const validation = validateSrsSettings(newCardsVal, maxReviewVal);
    if (!validation.isValid) {
      if (this.newCardsPerDayError) {
        this.newCardsPerDayError.textContent = validation.errors.newCardsPerDay || '';
      }
      if (this.maxReviewPerDayError) {
        this.maxReviewPerDayError.textContent = validation.errors.maxReviewPerDay || '';
      }

      // Accessible Focus: Move focus to the first erroneous input
      if (validation.errors.newCardsPerDay && this.newCardsPerDayInput) {
        this.newCardsPerDayInput.focus();
      } else if (validation.errors.maxReviewPerDay && this.maxReviewPerDayInput) {
        this.maxReviewPerDayInput.focus();
      }
      return;
    }

    // Clear inline errors
    if (this.newCardsPerDayError) this.newCardsPerDayError.textContent = '';
    if (this.maxReviewPerDayError) this.maxReviewPerDayError.textContent = '';
    if (this.settingsFormStatus) {
      this.settingsFormStatus.textContent = 'Đang lưu cấu hình...';
      this.settingsFormStatus.className = 'small text-secondary';
    }

    // 2. Lock Form & Prepare Payload
    this.isSubmitting = true;
    if (this.btnSaveSettings) {
      this.btnSaveSettings.disabled = true;
      this.btnSaveSettings.textContent = 'Đang lưu...';
    }

    const payload = buildUpdateSettingsPayload(newCardsVal, maxReviewVal);

    try {
      // 3. Submit PUT request
      const updatedSetting = await apiClient('/srs/settings', {
        method: 'PUT',
        body: payload
      });

      this.authoritativeSettings = updatedSetting;

      // 4. Announce success
      if (this.settingsFormStatus) {
        this.settingsFormStatus.textContent = '✓ Cập nhật cấu hình thành công!';
        this.settingsFormStatus.className = 'small text-success fw-semibold';
      }

      showToast({
        type: 'success',
        title: 'Cài đặt SRS',
        message: 'Cập nhật hạn mức học tập hàng ngày thành công.'
      });

      // 5. Authoritative Refresh: Fetch fresh statistics and sync settings
      await this.refreshAuthoritativeData();

    } catch (err) {
      console.error('[SrsDashboard] Failed to update settings:', err);
      const msg = err.message || 'Không thể lưu cài đặt. Vui lòng kiểm tra lại kết nối mạng.';

      if (this.settingsFormStatus) {
        this.settingsFormStatus.textContent = `⚠️ ${msg}`;
        this.settingsFormStatus.className = 'small text-danger fw-semibold';
      }

      showToast({
        type: 'danger',
        title: 'Lỗi cập nhật',
        message: msg
      });
    } finally {
      // 6. Restore submit button
      this.isSubmitting = false;
      if (this.btnSaveSettings) {
        this.btnSaveSettings.disabled = false;
        this.btnSaveSettings.textContent = 'Lưu thay đổi';
      }
    }
  }

  /**
   * Refreshes authoritative stats and settings from backend.
   */
  async refreshAuthoritativeData() {
    try {
      const [stats, settings] = await Promise.all([
        apiClient('/srs/stats'),
        apiClient('/srs/settings')
      ]);

      this.authoritativeStats = stats;
      this.authoritativeSettings = settings;

      this.renderStatistics(stats);
      this.renderSettings(settings);
    } catch (err) {
      console.warn('[SrsDashboard] Background refresh encountered an issue:', err);
    }
  }

  /**
   * Resets form values to the last known authoritative values.
   */
  handleResetSettings() {
    if (this.authoritativeSettings) {
      this.renderSettings(this.authoritativeSettings);
      if (this.settingsFormStatus) {
        this.settingsFormStatus.textContent = 'Đã khôi phục giá trị đã lưu.';
        this.settingsFormStatus.className = 'small text-muted';
      }
    }
  }

  /**
   * Manually refreshes statistics via the header action button.
   */
  async handleRefreshStats() {
    if (!this.btnRefreshStats) return;

    this.btnRefreshStats.disabled = true;
    const originalText = this.btnRefreshStats.textContent;
    this.btnRefreshStats.textContent = 'Đang tải...';

    try {
      const stats = await apiClient('/srs/stats');
      this.authoritativeStats = stats;
      this.renderStatistics(stats);

      showToast({
        type: 'info',
        title: 'Thống kê SRS',
        message: 'Đã cập nhật số liệu học tập mới nhất.'
      });
    } catch (err) {
      showToast({
        type: 'danger',
        title: 'Lỗi làm mới',
        message: err.message || 'Không thể làm mới số liệu thống kê.'
      });
    } finally {
      this.btnRefreshStats.disabled = false;
      this.btnRefreshStats.textContent = originalText;
    }
  }
}

// Auto-boot if running in browser window
if (typeof window !== 'undefined') {
  window.addEventListener('DOMContentLoaded', () => {
    const controller = new SrsDashboardController();
    controller.init();
  });
}
