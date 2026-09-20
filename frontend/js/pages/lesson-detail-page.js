/**
 * =============================================================================
 * PUBLIC LESSON DETAIL PAGE CONTROLLER (TASK 9C.1)
 * Module: frontend/js/pages/lesson-detail-page.js
 * 
 * Responsibilities:
 * - Reads and validates lessonId from URL query parameter (?id=...).
 * - Fetches approved public lesson detail from GET /api/v1/lessons/{id}.
 * - Enforces zero-leak public contract: non-approved/missing lessons render
 *   neutral 404 error without disclosing internal moderation state.
 * - Renders constituent vocabulary items strictly in orderIndex ascending sequence.
 * - Integrates Web Speech API pronunciation with graceful fallback.
 * - Pure helpers exported for unit testing (ID validation, parsing, ordering).
 * - Safe DOM construction (zero unsafe innerHTML) defending against XSS.
 * - Accessible landmarks, breadcrumb navigation, and keyboard interaction.
 * =============================================================================
 */

import { apiClient } from '../api/api.js';
import { showLoading, showError, showToast } from '../ui/ui.js';
import { createSafeElement, clearContainer } from '../ui/security.js';
import { initNavbarAuth } from '../ui/nav.js';
import { formatLessonDate, sanitizeLessonTitle } from './lessons-page.js';
import { openNotesModal } from '../ui/notes-modal.js';

/**
 * Validates the raw lesson ID string from URL parameter.
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
 * Parses standard Spring Boot ApiResponse<LessonDetailResponse>.
 * 
 * @param {any} envelope 
 * @returns {{
 *   lessonId: number,
 *   title: string,
 *   status: string,
 *   vocabularyCount: number,
 *   vocabularies: Array<{
 *     vocabId: number,
 *     hanzi: string,
 *     pinyin: string,
 *     pinyinRaw: string,
 *     meaningHanViet: string,
 *     meaningVi: string,
 *     audioUrl?: string,
 *     videoWritingUrl?: string,
 *     exampleSentence?: string,
 *     exampleTranslation?: string,
 *     orderIndex: number
 *   }>,
 *   createdAt: string,
 *   updatedAt: string
 * }|null}
 */
export function parseLessonDetailResponse(envelopeOrData) {
  if (!envelopeOrData) {
    return null;
  }

  const data = (envelopeOrData.data !== undefined && envelopeOrData.data !== null) 
    ? envelopeOrData.data 
    : envelopeOrData;

  if (!data || typeof data !== 'object') {
    return null;
  }

  const rawVocabs = Array.isArray(data.vocabularies) ? data.vocabularies : [];
  const vocabularies = sortVocabulariesByOrderIndex(rawVocabs);

  return {
    lessonId: data.lessonId ?? null,
    title: sanitizeLessonTitle(data.title),
    status: data.status || 'Approved',
    vocabularyCount: typeof data.vocabularyCount === 'number' ? data.vocabularyCount : vocabularies.length,
    vocabularies,
    createdAt: data.createdAt || '',
    updatedAt: data.updatedAt || ''
  };
}

/**
 * Deterministically sorts vocabulary items by orderIndex ascending (1, 2, 3...).
 * 
 * @param {Array<any>} vocabularies 
 * @returns {Array<any>}
 */
export function sortVocabulariesByOrderIndex(vocabularies) {
  if (!Array.isArray(vocabularies)) {
    return [];
  }
  return [...vocabularies].sort((a, b) => {
    const orderA = typeof a?.orderIndex === 'number' ? a.orderIndex : 0;
    const orderB = typeof b?.orderIndex === 'number' ? b.orderIndex : 0;
    return orderA - orderB;
  });
}

/**
 * Checks if browser supports SpeechSynthesis API safely.
 * 
 * @returns {boolean}
 */
export function isSpeechSynthesisAvailable() {
  return typeof window !== 'undefined' &&
    typeof window.speechSynthesis !== 'undefined' &&
    typeof window.SpeechSynthesisUtterance !== 'undefined';
}

/**
 * Pronounces Hanzi text using Web Speech API with Chinese mandarin voice.
 * 
 * @param {string} hanzi 
 * @returns {boolean} true if pronunciation initiated, false if unsupported/error
 */
export function speakHanzi(hanzi) {
  if (!hanzi || typeof hanzi !== 'string') {
    return false;
  }

  if (!isSpeechSynthesisAvailable()) {
    if (typeof document !== 'undefined') {
      showToast('Trình duyệt của bạn không hỗ trợ tính năng phát âm tự động.', 'info');
    }
    return false;
  }

  try {
    window.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(hanzi.trim());
    utterance.lang = 'zh-CN';
    utterance.rate = 0.85;

    const voices = window.speechSynthesis.getVoices();
    if (Array.isArray(voices) && voices.length > 0) {
      const zhVoice = voices.find(v => v.lang === 'zh-CN') ||
                      voices.find(v => v.lang.startsWith('zh'));
      if (zhVoice) {
        utterance.voice = zhVoice;
      }
    }

    utterance.onerror = () => {
      // Graceful error ignore
    };

    window.speechSynthesis.speak(utterance);
    return true;
  } catch {
    if (typeof document !== 'undefined') {
      showToast('Không thể phát âm từ vựng vào lúc này.', 'info');
    }
    return false;
  }
}

/**
 * Creates an SVG speaker icon element.
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
  svg.appendChild(polygon);

  const path1 = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  path1.setAttribute('d', 'M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07');
  svg.appendChild(path1);

  return svg;
}

/**
 * Controller managing the public lesson detail page.
 */
export class LessonDetailPageController {
  constructor() {
    this.lessonId = null;
    this.lessonData = null;

    this.dom = {
      stateContainer: null,
      contentWrapper: null,
      breadcrumbTitle: null,
      heading: null,
      vocabCountBadge: null,
      lessonDate: null,
      vocabList: null
    };
  }

  /**
   * Initializes DOM bindings and triggers lesson detail loading.
   */
  init() {
    this.dom.stateContainer = document.getElementById('lessonDetailStateContainer');
    this.dom.contentWrapper = document.getElementById('lessonDetailContent');
    this.dom.breadcrumbTitle = document.getElementById('breadcrumbLessonTitle');
    this.dom.heading = document.getElementById('lessonDetailHeading');
    this.dom.vocabCountBadge = document.getElementById('detailVocabCount');
    this.dom.lessonDate = document.getElementById('detailLessonDate');
    this.dom.vocabList = document.getElementById('lessonVocabList');

    if (!this.dom.stateContainer || !this.dom.contentWrapper) {
      return;
    }

    // Extract ID from URL
    const params = new URLSearchParams(window.location.search);
    const rawId = params.get('id');
    this.lessonId = validateLessonId(rawId);

    if (!this.lessonId) {
      this.renderInvalidIdState();
      return;
    }

    this.loadLessonDetail();
  }

  /**
   * Displays friendly error state when ID is missing or non-numeric.
   */
  renderInvalidIdState() {
    this.dom.contentWrapper.classList.add('d-none');
    showError(this.dom.stateContainer, {
      title: 'Mã bài học không hợp lệ',
      message: 'Mã bài học không hợp lệ hoặc thiếu thông tin định danh. Vui lòng chọn bài học từ danh mục bài học công khai.',
      retryText: 'Về danh mục bài học',
      onRetry: () => {
        window.location.href = 'lessons.html';
      }
    });

    if (this.dom.breadcrumbTitle) {
      this.dom.breadcrumbTitle.textContent = 'Mã bài học không hợp lệ';
    }
  }

  /**
   * Fetches lesson detail from GET /api/v1/lessons/{id}.
   */
  async loadLessonDetail() {
    this.dom.contentWrapper.classList.add('d-none');
    showLoading(this.dom.stateContainer, { message: 'Đang nạp nội dung bài học...' });

    try {
      const envelope = await apiClient(`/lessons/${this.lessonId}`);
      clearContainer(this.dom.stateContainer);

      const parsed = parseLessonDetailResponse(envelope);
      if (!parsed) {
        throw new Error('Dữ liệu bài học không đúng định dạng');
      }

      this.lessonData = parsed;
      this.renderLessonContent();

    } catch (err) {
      this.dom.contentWrapper.classList.add('d-none');

      // Neutral 404 defense: Do NOT expose internal workflow state (Draft/Pending/Rejected)
      const is404 = err.status === 404 || (err.message && err.message.includes('404'));
      const friendlyMessage = is404
        ? 'Không tìm thấy bài học hoặc bài học chưa được công khai.'
        : (err.message || 'Không thể tải chi tiết bài học. Vui lòng thử lại.');

      showError(this.dom.stateContainer, {
        title: is404 ? 'Không tìm thấy bài học' : 'Lỗi nạp bài học',
        message: friendlyMessage,
        onRetry: () => this.loadLessonDetail()
      });

      if (this.dom.breadcrumbTitle) {
        this.dom.breadcrumbTitle.textContent = is404 ? 'Bài học không tìm thấy' : 'Lỗi nạp bài học';
      }
    }
  }

  /**
   * Renders lesson header metadata and ordered vocabulary list.
   */
  renderLessonContent() {
    const lesson = this.lessonData;
    if (!lesson) {
      return;
    }

    // 1. Update Title & Breadcrumbs
    document.title = `${lesson.title} | Chi Tiết Bài Học`;
    if (this.dom.heading) {
      this.dom.heading.textContent = lesson.title;
    }
    if (this.dom.breadcrumbTitle) {
      this.dom.breadcrumbTitle.textContent = lesson.title;
    }

    // 2. Update Metadata
    if (this.dom.vocabCountBadge) {
      this.dom.vocabCountBadge.textContent = `${lesson.vocabularyCount} từ vựng`;
    }

    if (this.dom.lessonDate) {
      const dateStr = formatLessonDate(lesson.updatedAt || lesson.createdAt);
      this.dom.lessonDate.textContent = dateStr || 'Chưa xác định';
      if (lesson.updatedAt || lesson.createdAt) {
        this.dom.lessonDate.setAttribute('datetime', lesson.updatedAt || lesson.createdAt);
      }
    }

    // 3. Render Ordered Vocabulary List
    clearContainer(this.dom.vocabList);

    if (lesson.vocabularies.length === 0) {
      const emptyLi = createSafeElement('li', {
        className: 'p-4 text-center text-muted bg-surface rounded-3 border',
        text: 'Bài học này hiện chưa có từ vựng nào.'
      });
      this.dom.vocabList.appendChild(emptyLi);
    } else {
      for (const item of lesson.vocabularies) {
        const li = this.createVocabularyItemElement(item);
        this.dom.vocabList.appendChild(li);
      }
    }

    // 4. Reveal Content
    this.dom.contentWrapper.classList.remove('d-none');
  }

  /**
   * Creates a semantic <li> element for a single vocabulary item in the lesson.
   * 
   * @param {any} item 
   * @returns {HTMLLIElement}
   */
  createVocabularyItemElement(item) {
    const li = createSafeElement('li', {
      className: 'lesson-vocab-item',
      attrs: typeof item.orderIndex === 'number' ? { value: String(item.orderIndex) } : {}
    });

    // Main section: Order badge, Hanzi, Pinyin, Han-Viet
    const mainSection = createSafeElement('div', { className: 'lesson-vocab-main' });

    // Order Badge
    const orderBadge = createSafeElement('span', {
      className: 'lesson-vocab-order-badge',
      text: String(item.orderIndex || 1),
      attrs: { title: `Thứ tự bài học: #${item.orderIndex || 1}` }
    });
    mainSection.appendChild(orderBadge);

    // Hanzi Glyph
    const hanziSpan = createSafeElement('span', {
      className: 'lesson-vocab-hanzi',
      text: item.hanzi || '字',
      attrs: { lang: 'zh-Hans' }
    });
    mainSection.appendChild(hanziSpan);

    // Readings: Pinyin & Han-Viet
    const readingsDiv = createSafeElement('div', { className: 'lesson-vocab-readings' });
    const pinyinSpan = createSafeElement('span', {
      className: 'lesson-vocab-pinyin',
      text: item.pinyin || ''
    });
    readingsDiv.appendChild(pinyinSpan);

    const hanvietSpan = createSafeElement('span', {
      className: 'lesson-vocab-hanviet',
      text: item.meaningHanViet ? `[${item.meaningHanViet}]` : ''
    });
    readingsDiv.appendChild(hanvietSpan);

    mainSection.appendChild(readingsDiv);
    li.appendChild(mainSection);

    // Details section: Meaning & Example sentence
    const detailsDiv = createSafeElement('div', { className: 'lesson-vocab-details' });
    const meaningDiv = createSafeElement('div', {
      className: 'lesson-vocab-meaning',
      text: item.meaningVi || 'Đang cập nhật giải nghĩa'
    });
    detailsDiv.appendChild(meaningDiv);

    if (item.exampleSentence) {
      const exampleDiv = createSafeElement('div', { className: 'lesson-vocab-example' });
      const sentenceSpan = createSafeElement('span', {
        text: `Ví dụ: ${item.exampleSentence}`,
        attrs: { lang: 'zh-Hans' }
      });
      exampleDiv.appendChild(sentenceSpan);

      if (item.exampleTranslation) {
        const transSpan = createSafeElement('span', {
          text: ` (${item.exampleTranslation})`
        });
        exampleDiv.appendChild(transSpan);
      }
      detailsDiv.appendChild(exampleDiv);
    }
    li.appendChild(detailsDiv);

    // Actions: Speech button & View in dictionary
    const actionsDiv = createSafeElement('div', { className: 'lesson-vocab-actions' });

    // Speech Button
    const speechBtn = createSafeElement('button', {
      className: 'vocab-speech-btn btn-vocab-speech',
      attrs: {
        type: 'button',
        'aria-label': `Phát âm từ vựng ${item.hanzi || ''}`,
        title: 'Phát âm (Web Speech API)'
      }
    });
    speechBtn.appendChild(createSpeakerIcon());
    speechBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      speakHanzi(item.hanzi);
    });
    actionsDiv.appendChild(speechBtn);

    // Dictionary Navigation Link
    if (item.hanzi) {
      const dictLink = createSafeElement('a', {
        className: 'btn-chinese-secondary py-1 px-2 text-decoration-none small',
        text: 'Tra từ điển',
        attrs: {
          href: `vocabulary.html?search=${encodeURIComponent(item.hanzi)}`,
          'aria-label': `Tra cứu từ vựng ${item.hanzi} trong từ điển`
        }
      });
      actionsDiv.appendChild(dictLink);
    }

    // Personal Note Button
    if (item.vocabId) {
      const noteBtn = createSafeElement('button', {
        className: 'btn-chinese-secondary py-1 px-2 small lesson-vocab-note-btn',
        text: 'Ghi chú',
        attrs: {
          type: 'button',
          'aria-label': `Ghi chú cá nhân cho từ vựng ${item.hanzi || ''}`,
          title: 'Ghi chú cá nhân'
        }
      });
      noteBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        openNotesModal({
          vocabId: item.vocabId,
          hanzi: item.hanzi,
          pinyin: item.pinyin,
          meaning: item.meaningVi,
          triggerElement: noteBtn
        });
      });
      actionsDiv.appendChild(noteBtn);
    }

    li.appendChild(actionsDiv);
    return li;
  }
}

// Auto-instantiate if on lesson-detail.html
if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    initNavbarAuth('navAuthContainer');
    if (document.getElementById('lessonDetailHeading')) {
      const controller = new LessonDetailPageController();
      controller.init();
    }
  });
}
