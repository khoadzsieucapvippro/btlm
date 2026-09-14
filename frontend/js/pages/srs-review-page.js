/**
 * =============================================================================
 * INTERACTIVE SRS FLASHCARD REVIEW SESSION CONTROLLER (TASK 9C.3)
 * Module: frontend/js/pages/srs-review-page.js
 * 
 * Responsibilities:
 * - Interactive Spaced Repetition (SRS) review room for vocabulary and radicals.
 * - Authenticated learner enforcement via authManager (blocks anonymous network calls).
 * - Mode A: Due cards retrieval (GET /api/v1/srs/due).
 * - Mode B: Approved-lesson new-card candidates (GET /api/v1/srs/new-cards?lessonId=X).
 * - 3D CSS Card Flip (Space/Enter/Click), with vestibular reduced-motion adaptation.
 * - Reaction timer measuring actual integer seconds (reviewTimeSeconds >= 0).
 * - Authoritative rating submission (POST /api/v1/srs/review, ratings 1..4).
 * - Client-side session queue with Again (rating 1) requeue policy (max 3 cycles).
 * - Optional audio pronunciation (detail endpoint + Web Speech API fallback).
 * - Contextual personal notes integration via NotesModalController (VOCABULARY only).
 * - Comprehensive 4-state lifecycle (Loading, Auth, Empty, Error, Active, Completed).
 * - 100% Safe DOM rendering (textContent / createElement; zero innerHTML sinks).
 * - Full WCAG 2.2 AA accessibility (accessible names, visible focus, aria-hidden coordination).
 * =============================================================================
 */

import { apiClient, ApiError } from '../api/api.js';
import { authManager } from '../auth/auth-state.js';
import { openNotesModal } from '../ui/notes-modal.js';
import { showToast } from '../ui/ui.js';
import { initNavbarAuth } from '../ui/nav.js';
import { sanitizeResourceUrl } from '../ui/security.js';

export const MAX_AGAIN_REVIEWS_PER_CARD = 3;

/**
 * Maps rating numeric keys or integers to descriptive metadata.
 * Rating contract: 1 = Again, 2 = Hard, 3 = Good, 4 = Easy.
 * 
 * @param {number|string} val 
 * @returns {{ rating: number, label: string, nameVi: string } | null}
 */
export function mapRatingValue(val) {
  const num = Number(val);
  switch (num) {
    case 1:
      return { rating: 1, label: 'Again', nameVi: 'Học lại' };
    case 2:
      return { rating: 2, label: 'Hard', nameVi: 'Khó' };
    case 3:
      return { rating: 3, label: 'Good', nameVi: 'Tốt' };
    case 4:
      return { rating: 4, label: 'Easy', nameVi: 'Dễ' };
    default:
      return null;
  }
}

/**
 * Calculates non-negative elapsed reaction time in seconds.
 * 
 * @param {number} startTimeMs 
 * @param {number} nowMs 
 * @returns {number} integer seconds >= 0
 */
export function calculateReviewTime(startTimeMs, nowMs) {
  const start = Number(startTimeMs) || 0;
  const now = Number(nowMs) || 0;
  return Math.max(0, Math.round((now - start) / 1000));
}

/**
 * Formats elapsed seconds into human-readable representation (e.g. "12s" or "2p 15s").
 * 
 * @param {number} seconds 
 * @returns {string}
 */
export function formatElapsedSeconds(seconds) {
  const sec = Math.max(0, Math.floor(Number(seconds) || 0));
  if (sec < 60) {
    return `${sec}s`;
  }
  const mins = Math.floor(sec / 60);
  const rem = sec % 60;
  return `${mins}p ${rem}s`;
}

/**
 * Validates and serializes the exact payload for POST /api/v1/srs/review.
 * Enforces:
 * - itemType: strictly "VOCABULARY" or "RADICAL"
 * - itemId: positive number
 * - rating: 1..4
 * - reviewTimeSeconds: integer >= 0
 * - NO userId or accountId leakage
 * 
 * @param {Object} params 
 * @returns {{ itemType: string, itemId: number, rating: number, reviewTimeSeconds: number }}
 */
export function buildReviewPayload({ itemType, itemId, rating, reviewTimeSeconds }) {
  if (!itemType || typeof itemType !== 'string') {
    throw new TypeError('itemType must be a non-empty string ("VOCABULARY" or "RADICAL")');
  }
  const typeUpper = itemType.trim().toUpperCase();
  if (typeUpper !== 'VOCABULARY' && typeUpper !== 'RADICAL') {
    throw new TypeError(`itemType must be "VOCABULARY" or "RADICAL", received: ${itemType}`);
  }

  const id = Number(itemId);
  if (!Number.isInteger(id) || id <= 0) {
    throw new TypeError(`itemId must be a positive integer, received: ${itemId}`);
  }

  const r = Number(rating);
  if (!Number.isInteger(r) || r < 1 || r > 4) {
    throw new TypeError(`rating must be an integer between 1 and 4, received: ${rating}`);
  }

  const timeSec = Number(reviewTimeSeconds);
  if (!Number.isInteger(timeSec) || timeSec < 0) {
    throw new TypeError(`reviewTimeSeconds must be a non-negative integer, received: ${reviewTimeSeconds}`);
  }

  return {
    itemType: typeUpper,
    itemId: id,
    rating: r,
    reviewTimeSeconds: timeSec
  };
}

/**
 * Handles the local session requeue policy for Again cards.
 * Prevents accidental infinite loops via maxAgain limit.
 * 
 * @param {Array} queue 
 * @param {Object} queueItem 
 * @param {number} [maxAgain=MAX_AGAIN_REVIEWS_PER_CARD] 
 * @returns {{ requeued: boolean, attempts?: number, reason?: string }}
 */
export function handleAgainRequeue(queue, queueItem, maxAgain = MAX_AGAIN_REVIEWS_PER_CARD) {
  if (!queue || !Array.isArray(queue) || !queueItem) {
    return { requeued: false, reason: 'INVALID_ARGUMENTS' };
  }

  const currentAttempts = (queueItem.againAttempts || 0) + 1;
  queueItem.againAttempts = currentAttempts;

  if (currentAttempts <= maxAgain) {
    const requeuedItem = {
      card: { ...queueItem.card },
      againAttempts: currentAttempts,
      _requeued: true
    };
    queue.push(requeuedItem);
    return { requeued: true, attempts: currentAttempts };
  }

  return { requeued: false, reason: 'MAX_ATTEMPTS_REACHED' };
}

/**
 * Dispatches speech synthesis for Hanzi pronunciation with safe fallback.
 * 
 * @param {string} text 
 * @param {Object} [options]
 * @returns {boolean}
 */
export function speakHanzi(text, { lang = 'zh-CN', rate = 0.85 } = {}) {
  if (!text || typeof text !== 'string' || !text.trim()) {
    return false;
  }

  if (typeof window === 'undefined' || !window.speechSynthesis) {
    return false;
  }

  try {
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text.trim());
    utterance.lang = lang;
    utterance.rate = rate;

    const voices = window.speechSynthesis.getVoices?.() || [];
    const chineseVoice = voices.find(v => v.lang === 'zh-CN' || v.lang?.startsWith('zh'));
    if (chineseVoice) {
      utterance.voice = chineseVoice;
    }

    window.speechSynthesis.speak(utterance);
    return true;
  } catch (e) {
    console.warn('[SRS Audio] Speech synthesis dispatch failed:', e);
    return false;
  }
}

/**
 * Interactive SRS Review Session Controller
 */
export class SrsReviewController {
  constructor() {
    // Session Configuration & Mode
    this.mode = 'DUE_CARDS'; // 'DUE_CARDS' or 'LESSON_NEW_CARDS'
    this.lessonId = null;

    // Queue & Execution State
    this.sessionQueue = [];
    this.currentIndex = 0;
    this.isFlipped = false;
    this.isSubmitting = false;

    // Reaction Timer
    this.cardStartTime = 0;
    this.timerInterval = null;

    // Session Statistics
    this.stats = {
      totalReviewed: 0,
      againCount: 0,
      goodEasyCount: 0,
      totalTimeSeconds: 0
    };

    // Cache for detail audio URLs: itemId -> audioUrl
    this.audioCache = new Map();

    // Bound Event Handlers for Cleanup
    this.onKeydown = this.handleKeydown.bind(this);
    this.onUnload = this.cleanupTimer.bind(this);
  }

  /**
   * Initializes the review session page.
   */
  async init() {
    // Render navigation bar authentication state
    initNavbarAuth();

    // Cache DOM Elements
    this.dom = {
      loadingState: document.getElementById('srsLoadingState'),
      authState: document.getElementById('srsAuthState'),
      emptyState: document.getElementById('srsEmptyState'),
      emptyTitle: document.getElementById('srsEmptyTitle'),
      emptyDescription: document.getElementById('srsEmptyDescription'),
      errorState: document.getElementById('srsErrorState'),
      errorMessage: document.getElementById('srsErrorMessage'),
      btnRetry: document.getElementById('btnSrsRetry'),
      activeSession: document.getElementById('srsActiveSession'),
      completedState: document.getElementById('srsCompletedState'),

      // Session Header
      modeBadge: document.getElementById('srsSessionModeBadge'),
      progressText: document.getElementById('srsProgressText'),
      progressBarContainer: document.getElementById('srsProgressBarContainer'),
      progressBarFill: document.getElementById('srsProgressBarFill'),
      timerBadge: document.getElementById('reactionTimerBadge'),

      // Flashcard
      flashcard: document.getElementById('flashcard'),
      cardFront: document.getElementById('cardFront'),
      cardBack: document.getElementById('cardBack'),
      cardTypeFront: document.getElementById('cardTypeBadgeFront'),
      cardStatusFront: document.getElementById('cardStatusBadgeFront'),
      cardFrontHanzi: document.getElementById('cardFrontHanzi'),
      cardFrontPinyin: document.getElementById('cardFrontPinyin'),
      btnAudioPronounceFront: document.getElementById('btnAudioPronounceFront'),

      cardTypeBack: document.getElementById('cardTypeBadgeBack'),
      btnCardNote: document.getElementById('btnCardNote'),
      cardBackHanzi: document.getElementById('cardBackHanzi'),
      cardBackPinyin: document.getElementById('cardBackPinyin'),
      cardBackMeaningHv: document.getElementById('cardBackMeaningHv'),
      cardBackMeaningVi: document.getElementById('cardBackMeaningVi'),
      cardBackExampleBox: document.getElementById('cardBackExampleBox'),
      cardBackExampleSentence: document.getElementById('cardBackExampleSentence'),
      cardBackExampleTranslation: document.getElementById('cardBackExampleTranslation'),

      // Rating Controls
      ratingSection: document.getElementById('srsRatingSection'),
      ratingBar: document.getElementById('ratingControlsBar'),
      btnRatingAgain: document.getElementById('btnRatingAgain'),
      btnRatingHard: document.getElementById('btnRatingHard'),
      btnRatingGood: document.getElementById('btnRatingGood'),
      btnRatingEasy: document.getElementById('btnRatingEasy'),
      ratingFeedback: document.getElementById('ratingFeedback'),

      // Completion stats
      statTotalReviewed: document.getElementById('statTotalReviewed'),
      statAgainCount: document.getElementById('statAgainCount'),
      statGoodEasyCount: document.getElementById('statGoodEasyCount'),
      statTotalTime: document.getElementById('statTotalTime')
    };

    // Attach listeners
    this.bindEvents();

    // Parse Mode from Query Params
    this.parseQueryParams();

    // Start Session Loading
    await this.loadSession();
  }

  /**
   * Binds DOM and keyboard events.
   */
  bindEvents() {
    if (this.dom.btnRetry) {
      this.dom.btnRetry.addEventListener('click', () => this.loadSession());
    }

    if (this.dom.flashcard) {
      this.dom.flashcard.addEventListener('click', (e) => {
        // Prevent flipping if clicked on an action button inside the card
        if (e.target.closest('button, a')) return;
        this.toggleFlip();
      });
    }

    if (this.dom.btnAudioPronounceFront) {
      this.dom.btnAudioPronounceFront.addEventListener('click', (e) => {
        e.stopPropagation();
        this.playPronunciation();
      });
    }

    if (this.dom.btnCardNote) {
      this.dom.btnCardNote.addEventListener('click', (e) => {
        e.stopPropagation();
        this.openNoteForCurrentCard();
      });
    }

    // Rating buttons
    const ratingButtons = [
      { el: this.dom.btnRatingAgain, rating: 1 },
      { el: this.dom.btnRatingHard, rating: 2 },
      { el: this.dom.btnRatingGood, rating: 3 },
      { el: this.dom.btnRatingEasy, rating: 4 }
    ];

    for (const item of ratingButtons) {
      if (item.el) {
        item.el.addEventListener('click', () => this.handleRating(item.rating));
      }
    }

    // Global Keyboard Listener
    window.addEventListener('keydown', this.onKeydown);
    window.addEventListener('beforeunload', this.onUnload);
  }

  /**
   * Parses lessonId from URL to determine review mode.
   */
  parseQueryParams() {
    if (typeof window === 'undefined') return;
    const urlParams = new URLSearchParams(window.location.search);
    const rawLessonId = urlParams.get('lessonId');

    if (rawLessonId && /^\d+$/.test(rawLessonId.trim()) && Number(rawLessonId.trim()) > 0) {
      this.mode = 'LESSON_NEW_CARDS';
      this.lessonId = Number(rawLessonId.trim());
    } else {
      this.mode = 'DUE_CARDS';
      this.lessonId = null;
    }
  }

  /**
   * Sets visible top-level state view.
   * 
   * @param {'loading'|'auth'|'empty'|'error'|'active'|'completed'} state 
   */
  setViewState(state) {
    const states = {
      loading: this.dom.loadingState,
      auth: this.dom.authState,
      empty: this.dom.emptyState,
      error: this.dom.errorState,
      active: this.dom.activeSession,
      completed: this.dom.completedState
    };

    for (const [key, el] of Object.entries(states)) {
      if (el) {
        if (key === state) {
          el.classList.remove('d-none');
        } else {
          el.classList.add('d-none');
        }
      }
    }
  }

  /**
   * Loads the flashcard review session from backend.
   */
  async loadSession() {
    this.cleanupTimer();
    this.setViewState('loading');

    // Authentication Guard
    if (!authManager.isAuthenticated()) {
      this.setViewState('auth');
      return;
    }

    try {
      let cards = [];
      if (this.mode === 'LESSON_NEW_CARDS') {
        // Mode B: Retrieve new card candidates for approved lesson
        cards = await apiClient('/srs/new-cards', {
          params: { lessonId: this.lessonId }
        });
      } else {
        // Mode A: Retrieve cards due for review
        cards = await apiClient('/srs/due');
      }

      if (!Array.isArray(cards) || cards.length === 0) {
        if (this.mode === 'LESSON_NEW_CARDS') {
          if (this.dom.emptyTitle) this.dom.emptyTitle.textContent = 'Không có từ mới trong bài học này';
          if (this.dom.emptyDescription) {
            this.dom.emptyDescription.textContent = 
              'Tất cả từ vựng trong bài học này đã được thêm vào tiến trình ôn tập SRS, hoặc bài học chưa có từ vựng.';
          }
        } else {
          if (this.dom.emptyTitle) this.dom.emptyTitle.textContent = 'Không có thẻ đến hạn hôm nay!';
          if (this.dom.emptyDescription) {
            this.dom.emptyDescription.textContent = 
              'Bạn đã hoàn thành toàn bộ các thẻ ôn tập cần thiết. Hãy học thêm bài mới hoặc quay lại vào ngày mai!';
          }
        }
        this.setViewState('empty');
        return;
      }

      // Initialize session queue
      this.sessionQueue = cards.map(c => ({
        card: c,
        againAttempts: 0
      }));

      this.currentIndex = 0;
      this.isFlipped = false;
      this.isSubmitting = false;
      this.stats = {
        totalReviewed: 0,
        againCount: 0,
        goodEasyCount: 0,
        totalTimeSeconds: 0
      };

      this.setViewState('active');
      this.renderCurrentCard();

    } catch (err) {
      console.error('[SRS Review] Failed to load session:', err);
      if (err.status === 401) {
        this.setViewState('auth');
        return;
      }

      if (this.dom.errorMessage) {
        this.dom.errorMessage.textContent = err.message || 'Đã xảy ra lỗi khi nạp dữ liệu thẻ ôn tập từ máy chủ.';
      }
      this.setViewState('error');
    }
  }

  /**
   * Renders the current active card in the session.
   */
  renderCurrentCard() {
    this.cleanupTimer();
    this.isFlipped = false;
    this.isSubmitting = false;

    if (this.dom.ratingFeedback) {
      this.dom.ratingFeedback.textContent = '';
    }

    // Reset 3D flashcard transform & state
    if (this.dom.flashcard) {
      this.dom.flashcard.classList.remove('flipped');
      this.dom.flashcard.setAttribute('aria-expanded', 'false');
    }

    if (this.dom.cardFront) this.dom.cardFront.setAttribute('aria-hidden', 'false');
    if (this.dom.cardBack) this.dom.cardBack.setAttribute('aria-hidden', 'true');

    // Disable rating buttons until card is flipped
    this.setRatingButtonsEnabled(false);

    const queueItem = this.sessionQueue[this.currentIndex];
    if (!queueItem || !queueItem.card) {
      this.showCompletedState();
      return;
    }

    const card = queueItem.card;
    const isVocab = (card.itemType || '').toUpperCase() === 'VOCABULARY';

    // Front face
    if (this.dom.cardTypeFront) {
      this.dom.cardTypeFront.textContent = isVocab ? 'Từ vựng' : 'Bộ thủ';
    }
    if (this.dom.cardStatusFront) {
      if (card.isNew) {
        this.dom.cardStatusFront.textContent = 'Mới';
        this.dom.cardStatusFront.classList.remove('d-none');
      } else if (queueItem._requeued) {
        this.dom.cardStatusFront.textContent = 'Học lại';
        this.dom.cardStatusFront.classList.remove('d-none');
      } else {
        this.dom.cardStatusFront.classList.add('d-none');
      }
    }
    if (this.dom.cardFrontHanzi) this.dom.cardFrontHanzi.textContent = card.hanzi || '';
    if (this.dom.cardFrontPinyin) this.dom.cardFrontPinyin.textContent = card.pinyin || '';

    // Back face
    if (this.dom.cardTypeBack) {
      this.dom.cardTypeBack.textContent = isVocab ? 'Từ vựng' : 'Bộ thủ';
    }
    if (this.dom.cardBackHanzi) this.dom.cardBackHanzi.textContent = card.hanzi || '';
    if (this.dom.cardBackPinyin) this.dom.cardBackPinyin.textContent = card.pinyin || '';
    
    // Meaning Hán-Việt
    if (this.dom.cardBackMeaningHv) {
      if (card.meaningHanViet) {
        this.dom.cardBackMeaningHv.textContent = card.meaningHanViet;
        this.dom.cardBackMeaningHv.classList.remove('d-none');
      } else {
        this.dom.cardBackMeaningHv.classList.add('d-none');
      }
    }

    // Meaning Vietnamese
    if (this.dom.cardBackMeaningVi) {
      this.dom.cardBackMeaningVi.textContent = card.meaningVi || '';
    }

    // Contextual Personal Note action (VOCABULARY only)
    if (this.dom.btnCardNote) {
      if (isVocab) {
        this.dom.btnCardNote.classList.remove('d-none');
      } else {
        this.dom.btnCardNote.classList.add('d-none');
      }
    }

    // Example Box (Vocabulary only, if present)
    if (this.dom.cardBackExampleBox) {
      if (card.exampleSentence) {
        if (this.dom.cardBackExampleSentence) this.dom.cardBackExampleSentence.textContent = card.exampleSentence;
        if (this.dom.cardBackExampleTranslation) this.dom.cardBackExampleTranslation.textContent = card.exampleTranslation || '';
        this.dom.cardBackExampleBox.classList.remove('d-none');
      } else {
        this.dom.cardBackExampleBox.classList.add('d-none');
      }
    }

    // Progress Bar & Counter
    const totalCount = this.sessionQueue.length;
    const currentNum = this.currentIndex + 1;
    if (this.dom.progressText) {
      this.dom.progressText.textContent = `Thẻ ${currentNum} / ${totalCount}`;
    }
    if (this.dom.modeBadge) {
      this.dom.modeBadge.textContent = this.mode === 'LESSON_NEW_CARDS'
        ? `Bài học #${this.lessonId}`
        : 'Thẻ đến hạn';
    }
    if (this.dom.progressBarContainer && this.dom.progressBarFill) {
      this.dom.progressBarContainer.setAttribute('aria-valuenow', String(currentNum));
      this.dom.progressBarContainer.setAttribute('aria-valuemax', String(totalCount));
      const pct = Math.min(100, Math.round((currentNum / totalCount) * 100));
      this.dom.progressBarFill.style.width = `${pct}%`;
    }

    // Start Reaction Timer
    this.cardStartTime = Date.now();
    if (this.dom.timerBadge) {
      this.dom.timerBadge.textContent = '⏱️ 0s';
    }

    this.timerInterval = setInterval(() => {
      const elapsed = calculateReviewTime(this.cardStartTime, Date.now());
      if (this.dom.timerBadge) {
        this.dom.timerBadge.textContent = `⏱️ ${formatElapsedSeconds(elapsed)}`;
      }
    }, 1000);
  }

  /**
   * Toggles 3D card flip between front prompt and back answer.
   */
  toggleFlip() {
    if (this.isSubmitting) return;

    this.isFlipped = !this.isFlipped;
    if (this.dom.flashcard) {
      if (this.isFlipped) {
        this.dom.flashcard.classList.add('flipped');
        this.dom.flashcard.setAttribute('aria-expanded', 'true');
      } else {
        this.dom.flashcard.classList.remove('flipped');
        this.dom.flashcard.setAttribute('aria-expanded', 'false');
      }
    }

    if (this.dom.cardFront) this.dom.cardFront.setAttribute('aria-hidden', this.isFlipped ? 'true' : 'false');
    if (this.dom.cardBack) this.dom.cardBack.setAttribute('aria-hidden', this.isFlipped ? 'false' : 'true');

    // Enable ratings when flipped; disable when on front
    this.setRatingButtonsEnabled(this.isFlipped);
  }

  /**
   * Enables or disables the 4 rating buttons.
   * 
   * @param {boolean} enabled 
   */
  setRatingButtonsEnabled(enabled) {
    const btns = [
      this.dom.btnRatingAgain,
      this.dom.btnRatingHard,
      this.dom.btnRatingGood,
      this.dom.btnRatingEasy
    ];
    for (const b of btns) {
      if (b) b.disabled = !enabled;
    }
  }

  /**
   * Dispatches audio playback for the active card.
   * Queries real detail endpoint (/vocabulary/{id} or /radicals/{id}) when not cached,
   * falling back cleanly to Web Speech API.
   */
  async playPronunciation() {
    const queueItem = this.sessionQueue[this.currentIndex];
    if (!queueItem || !queueItem.card) return;
    const card = queueItem.card;

    // Check memory cache
    const cacheKey = `${card.itemType}_${card.itemId}`;
    if (this.audioCache.has(cacheKey)) {
      const cachedUrl = this.audioCache.get(cacheKey);
      if (cachedUrl) {
        this.playAudioUrl(cachedUrl, card.hanzi);
        return;
      }
      // If cached as null, use speech synthesis fallback
      speakHanzi(card.hanzi);
      return;
    }

    // Fetch detail data from authoritative endpoint
    try {
      let audioUrl = null;
      if ((card.itemType || '').toUpperCase() === 'VOCABULARY') {
        const detail = await apiClient(`/vocabulary/${card.itemId}`);
        audioUrl = detail?.audioUrl || null;
      } else {
        const detail = await apiClient(`/radicals/${card.itemId}`);
        audioUrl = detail?.audioUrl || null;
      }

      this.audioCache.set(cacheKey, audioUrl);

      if (audioUrl) {
        this.playAudioUrl(audioUrl, card.hanzi);
      } else {
        speakHanzi(card.hanzi);
      }
    } catch (e) {
      console.warn('[SRS Audio] Could not fetch card detail audio, falling back to speech synthesis:', e);
      speakHanzi(card.hanzi);
    }
  }

  /**
   * Plays sanitized audio URL with fallback to Web Speech.
   * 
   * @param {string} rawUrl 
   * @param {string} fallbackHanzi 
   */
  playAudioUrl(rawUrl, fallbackHanzi) {
    const safeUrl = sanitizeResourceUrl(rawUrl);
    if (!safeUrl || safeUrl === 'about:blank') {
      speakHanzi(fallbackHanzi);
      return;
    }

    try {
      const audio = new Audio(safeUrl);
      audio.play().catch(() => {
        speakHanzi(fallbackHanzi);
      });
    } catch {
      speakHanzi(fallbackHanzi);
    }
  }

  /**
   * Opens contextual personal notes modal for the current vocabulary card.
   */
  openNoteForCurrentCard() {
    const queueItem = this.sessionQueue[this.currentIndex];
    if (!queueItem || !queueItem.card) return;
    const card = queueItem.card;

    if ((card.itemType || '').toUpperCase() !== 'VOCABULARY') {
      return;
    }

    openNotesModal({
      vocabId: card.itemId,
      hanzi: card.hanzi,
      pinyin: card.pinyin,
      meaning: card.meaningVi,
      triggerElement: this.dom.btnCardNote
    });
  }

  /**
   * Submits a rating review to the backend.
   * 
   * @param {number} rating 1=Again, 2=Hard, 3=Good, 4=Easy
   */
  async handleRating(rating) {
    if (!this.isFlipped || this.isSubmitting) return;

    const queueItem = this.sessionQueue[this.currentIndex];
    if (!queueItem || !queueItem.card) return;
    const card = queueItem.card;

    // Freeze reaction timer
    this.cleanupTimer();
    const elapsedSeconds = calculateReviewTime(this.cardStartTime, Date.now());

    // Lock submission controls
    this.isSubmitting = true;
    this.setRatingButtonsEnabled(false);
    if (this.dom.ratingFeedback) {
      this.dom.ratingFeedback.textContent = 'Đang ghi nhận kết quả đánh giá...';
    }

    try {
      const payload = buildReviewPayload({
        itemType: card.itemType,
        itemId: card.itemId,
        rating,
        reviewTimeSeconds: elapsedSeconds
      });

      // Submit authoritative review to backend
      const response = await apiClient('/srs/review', {
        method: 'POST',
        body: payload
      });

      // Update session statistics
      this.stats.totalReviewed++;
      this.stats.totalTimeSeconds += elapsedSeconds;
      if (rating === 1) {
        this.stats.againCount++;
      } else if (rating >= 3) {
        this.stats.goodEasyCount++;
      }

      // Handle Again local session requeue
      if (rating === 1) {
        const requeueResult = handleAgainRequeue(this.sessionQueue, queueItem, MAX_AGAIN_REVIEWS_PER_CARD);
        if (requeueResult.requeued) {
          showToast({
            type: 'info',
            title: 'Học lại',
            message: `Thẻ "${card.hanzi}" sẽ xuất hiện lại ở cuối phiên học.`
          });
        }
      }

      // Advance to next card
      this.currentIndex++;
      this.isSubmitting = false;

      if (this.currentIndex >= this.sessionQueue.length) {
        this.showCompletedState();
      } else {
        this.renderCurrentCard();
      }

    } catch (err) {
      console.error('[SRS Review] Review submission error:', err);
      this.isSubmitting = false;
      this.setRatingButtonsEnabled(true);

      if (err.status === 401) {
        this.setViewState('auth');
        return;
      }

      const msg = err.status === 409
        ? 'Thẻ chưa đến hạn ôn tập hoặc bạn đã đạt giới hạn ôn tập trong ngày.'
        : (err.message || 'Không thể gửi đánh giá thẻ. Vui lòng thử lại.');

      if (this.dom.ratingFeedback) {
        this.dom.ratingFeedback.textContent = msg;
      }

      showToast({
        type: 'danger',
        title: 'Lỗi gửi đánh giá',
        message: msg
      });
    }
  }

  /**
   * Handles keyboard shortcuts: Space/Enter (flip), 1..4 (ratings).
   * 
   * @param {KeyboardEvent} e 
   */
  handleKeydown(e) {
    // Ignore keystrokes inside form controls (inputs, textareas)
    const tag = (e.target?.tagName || '').toLowerCase();
    if (tag === 'input' || tag === 'textarea' || tag === 'select' || e.target?.isContentEditable) {
      return;
    }

    // Ignore keystrokes if a modal dialog is open (e.g. notes modal)
    const modal = document.getElementById('notesModal');
    if (modal && modal.open) {
      return;
    }

    // Only operate when active session is visible and not submitting
    if (this.dom.activeSession?.classList.contains('d-none') || this.isSubmitting) {
      return;
    }

    // Space or Enter flips the flashcard
    if (e.code === 'Space' || e.key === ' ' || e.key === 'Enter') {
      // If user focused on a real button, let the native button click fire
      if (tag === 'button' && e.target !== this.dom.flashcard) {
        return;
      }
      e.preventDefault();
      this.toggleFlip();
      return;
    }

    // Rating shortcuts 1..4 only work when card is flipped (answer revealed)
    if (this.isFlipped) {
      if (e.key === '1') {
        e.preventDefault();
        this.handleRating(1);
      } else if (e.key === '2') {
        e.preventDefault();
        this.handleRating(2);
      } else if (e.key === '3') {
        e.preventDefault();
        this.handleRating(3);
      } else if (e.key === '4') {
        e.preventDefault();
        this.handleRating(4);
      }
    }
  }

  /**
   * Displays the session completion screen with honest statistics.
   */
  showCompletedState() {
    this.cleanupTimer();
    this.setViewState('completed');

    if (this.dom.statTotalReviewed) {
      this.dom.statTotalReviewed.textContent = String(this.stats.totalReviewed);
    }
    if (this.dom.statAgainCount) {
      this.dom.statAgainCount.textContent = String(this.stats.againCount);
    }
    if (this.dom.statGoodEasyCount) {
      this.dom.statGoodEasyCount.textContent = String(this.stats.goodEasyCount);
    }
    if (this.dom.statTotalTime) {
      this.dom.statTotalTime.textContent = formatElapsedSeconds(this.stats.totalTimeSeconds);
    }
  }

  /**
   * Cleans up running timer intervals.
   */
  cleanupTimer() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  /**
   * Destroys the controller instance and unbinds global listeners.
   */
  destroy() {
    this.cleanupTimer();
    window.removeEventListener('keydown', this.onKeydown);
    window.removeEventListener('beforeunload', this.onUnload);
  }
}

// Global page instance auto-init on DOMContentLoaded
let srsControllerInstance = null;
if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    srsControllerInstance = new SrsReviewController();
    srsControllerInstance.init();
  });
}
