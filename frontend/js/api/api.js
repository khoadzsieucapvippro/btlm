/**
 * =============================================================================
 * CENTRALIZED HTTP API CLIENT (TASK 9A.2)
 * Module: frontend/js/api/api.js
 * 
 * Responsibilities:
 * - Single application network gateway (NO direct window.fetch in page scripts).
 * - Automatic Bearer token injection from authManager.
 * - Strict Content-Type policy (JSON for structured bodies, omitted for GET/no-body, native boundary for FormData).
 * - Standard envelope unpacking (ApiResponse<T> -> T).
 * - HTTP 204 No Content support without JSON parsing.
 * - Single-owner 401 Unauthorized lifecycle coordination via authManager.
 * - 403 Forbidden session preservation.
 * - 429 Rate limiting normalization (no assumed Retry-After).
 * - Safe idempotent GET retry (transient network failure only; max 1 retry).
 * - Timeout and abort discrimination (AbortSignal.timeout + AbortSignal.any).
 * - Validation error parsing helper (parseFieldErrors).
 * =============================================================================
 */

import { authManager } from '../auth/auth-state.js';

const DEFAULT_BASE_URL = '/api/v1';
const DEFAULT_TIMEOUT_MS = 15000;
const UPLOAD_TIMEOUT_MS = 60000;

/**
 * Standardized Application API Error Class
 */
export class ApiError extends Error {
  /**
   * @param {string} message Human-readable error description
   * @param {number} status HTTP status code (or 0 for network/abort, 408 for timeout)
   * @param {string} code Machine-readable error code ('VALIDATION_ERROR', 'UNAUTHORIZED', 'TIMEOUT', etc.)
   * @param {string[]} [errors=[]] Array of detailed field-level error messages
   * @param {Error|null} [originalError=null] Underlying cause if available
   */
  constructor(message, status = 0, code = 'UNKNOWN_ERROR', errors = [], originalError = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.errors = Array.isArray(errors) ? errors : [];
    this.originalError = originalError;
  }
}

/**
 * Resolves the active base URL for API requests.
 * Uses read-only deployment config window.__ENV__?.API_BASE_URL if configured,
 * defaulting to same-origin relative '/api/v1'.
 * 
 * @returns {string}
 */
export function getBaseUrl() {
  if (typeof window !== 'undefined' && window.__ENV__?.API_BASE_URL) {
    return String(window.__ENV__.API_BASE_URL).replace(/\/$/, '');
  }
  return DEFAULT_BASE_URL;
}

/**
 * Constructs a fully qualified URL from an endpoint path and optional query parameters.
 * Serializes query parameters safely using URLSearchParams to prevent injection or malformed encoding.
 * 
 * Supported types: string, number, boolean, array (appends each item).
 * Skips: null, undefined.
 * 
 * @param {string} endpoint e.g. '/lessons' or 'srs/due'
 * @param {object|null} [params=null] Query parameters dictionary
 * @returns {string}
 */
export function buildUrl(endpoint, params = null) {
  if (!endpoint || typeof endpoint !== 'string') {
    throw new TypeError('endpoint must be a non-empty string');
  }

  const base = getBaseUrl();
  const normalizedEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  let fullUrl = `${base}${normalizedEndpoint}`;

  if (params && typeof params === 'object') {
    const searchParams = new URLSearchParams();

    for (const [key, val] of Object.entries(params)) {
      if (val === undefined || val === null) {
        continue;
      }
      if (Array.isArray(val)) {
        val.forEach(item => {
          if (item !== undefined && item !== null) {
            searchParams.append(key, String(item));
          }
        });
      } else {
        searchParams.append(key, String(val));
      }
    }

    const qs = searchParams.toString();
    if (qs) {
      fullUrl += (fullUrl.includes('?') ? '&' : '?') + qs;
    }
  }

  return fullUrl;
}

/**
 * Parses field-level validation errors formatted by backend GlobalExceptionHandler.
 * Format: ["field: message", "field: message"]
 * 
 * Returns a key-value mapping: { fieldName: "message" }
 * Handles malformed strings gracefully without throwing.
 * 
 * @param {string[]} errors Array of error strings from envelope.errors
 * @returns {Record<string, string>}
 */
export function parseFieldErrors(errors) {
  const fieldErrors = {};
  if (!Array.isArray(errors)) {
    return fieldErrors;
  }

  for (const item of errors) {
    if (typeof item !== 'string') continue;
    const colonIndex = item.indexOf(':');
    if (colonIndex > 0) {
      const field = item.substring(0, colonIndex).trim();
      const message = item.substring(colonIndex + 1).trim();
      if (field && message && !fieldErrors[field]) {
        fieldErrors[field] = message;
      }
    } else {
      // General error without field prefix, assign to generic key if not present
      const trimmed = item.trim();
      if (trimmed && !fieldErrors._general) {
        fieldErrors._general = trimmed;
      }
    }
  }

  return fieldErrors;
}

/**
 * Creates an AbortSignal that aborts after timeoutMs.
 * Uses native AbortSignal.timeout if supported, otherwise falls back to AbortController + setTimeout.
 * 
 * @param {number} timeoutMs 
 * @returns {AbortSignal}
 */
function createTimeoutSignal(timeoutMs) {
  if (typeof AbortSignal !== 'undefined' && typeof AbortSignal.timeout === 'function') {
    return AbortSignal.timeout(timeoutMs);
  }
  const controller = new AbortController();
  const timer = setTimeout(() => {
    controller.abort(new DOMException('The operation timed out.', 'TimeoutError'));
  }, timeoutMs);
  // Ensure unref in Node environment if available
  if (timer && typeof timer.unref === 'function') {
    timer.unref();
  }
  return controller.signal;
}

/**
 * Composes multiple AbortSignals into a single signal that aborts when any input signal aborts.
 * Uses native AbortSignal.any if supported, otherwise falls back to event listener composition.
 * 
 * @param {AbortSignal[]} signals 
 * @returns {AbortSignal}
 */
function composeSignals(signals) {
  const activeSignals = signals.filter(Boolean);
  if (activeSignals.length === 0) {
    return undefined;
  }
  if (activeSignals.length === 1) {
    return activeSignals[0];
  }

  if (typeof AbortSignal !== 'undefined' && typeof AbortSignal.any === 'function') {
    return AbortSignal.any(activeSignals);
  }

  const controller = new AbortController();
  for (const signal of activeSignals) {
    if (signal.aborted) {
      controller.abort(signal.reason);
      return controller.signal;
    }
    signal.addEventListener('abort', () => {
      controller.abort(signal.reason);
    }, { once: true });
  }
  return controller.signal;
}

/**
 * Executes fetch with a safe, idempotent retry policy.
 * 
 * STRICT RULES:
 * - ONLY GET requests MAY be retried (max 1 retry).
 * - ONLY transient transport/network failures (TypeError) qualify for retry.
 * - Mutation requests (POST, PUT, PATCH, DELETE) MUST NEVER be retried automatically.
 * - HTTP status errors (4xx, 5xx) MUST NEVER be retried.
 * - Aborts and Timeouts MUST NEVER be retried.
 * 
 * @param {string} url 
 * @param {RequestInit} config 
 * @param {string} method 
 * @param {boolean} allowRetry 
 * @returns {Promise<Response>}
 */
async function executeFetchWithSafeRetry(url, config, method, allowRetry) {
  try {
    return await fetch(url, config);
  } catch (err) {
    const isGet = method === 'GET';
    const isAbort = err.name === 'AbortError' || err.name === 'TimeoutError';
    const isTypeError = err instanceof TypeError || err.name === 'TypeError';

    if (allowRetry && isGet && isTypeError && !isAbort) {
      // Single transient network retry after 500ms delay
      await new Promise(resolve => setTimeout(resolve, 500));
      return await fetch(url, config);
    }
    throw err;
  }
}

/**
 * Single Application Gateway for all REST API communication.
 * 
 * @template T
 * @param {string} endpoint Path relative to API base (e.g. '/lessons', '/srs/due')
 * @param {object} [options={}] Request options
 * @param {string} [options.method='GET'] HTTP method
 * @param {object|null} [options.params=null] Query parameters dictionary
 * @param {any} [options.body] Request body (JSON object, FormData, string)
 * @param {HeadersInit} [options.headers={}] Additional HTTP headers
 * @param {number} [options.timeout] Custom timeout in milliseconds
 * @param {AbortSignal} [options.signal] External abort signal
 * @param {boolean} [options.retry=true] Whether to permit safe GET retry on transient network drops
 * @returns {Promise<T>} Unpacked payload from ApiResponse.data (or null for 204)
 * @throws {ApiError}
 */
export async function apiClient(endpoint, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const url = buildUrl(endpoint, options.params);
  const headers = new Headers(options.headers || {});
  let body = options.body;

  // 1. Content-Type Policy
  const isFormData = typeof FormData !== 'undefined' && body instanceof FormData;
  const isSearchParams = typeof URLSearchParams !== 'undefined' && body instanceof URLSearchParams;

  if (isFormData) {
    // Architectural Invariant: Browser MUST generate multipart boundary automatically.
    headers.delete('Content-Type');
  } else if (body !== undefined && body !== null) {
    if (typeof body === 'object' && !isSearchParams) {
      body = JSON.stringify(body);
      if (!headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
      }
    } else if (typeof body === 'string' && !headers.has('Content-Type')) {
      const trimmed = body.trim();
      if ((trimmed.startsWith('{') && trimmed.endsWith('}')) || (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
        headers.set('Content-Type', 'application/json');
      }
    }
  } else {
    // No body: Do NOT set Content-Type header on bodyless requests (e.g. standard GET)
  }

  // 2. Authorization Policy: Auto-inject token from authManager
  const token = authManager.getToken();
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  // 3. Timeout & Abort Configuration
  const defaultTimeout = isFormData ? UPLOAD_TIMEOUT_MS : DEFAULT_TIMEOUT_MS;
  const timeoutMs = typeof options.timeout === 'number' && options.timeout > 0 ? options.timeout : defaultTimeout;
  const timeoutSignal = createTimeoutSignal(timeoutMs);
  const combinedSignal = composeSignals([options.signal, timeoutSignal]);

  const fetchConfig = {
    ...options,
    method,
    headers,
    body,
    signal: combinedSignal
  };

  const allowRetry = options.retry !== false;

  try {
    const response = await executeFetchWithSafeRetry(url, fetchConfig, method, allowRetry);

    // 4. HTTP 204 No Content Handling (Project Invariant: NEVER call response.json())
    if (response.status === 204) {
      return null;
    }

    // 5. HTTP 401 Unauthorized Handling (Single Owner transition via authManager)
    if (response.status === 401) {
      let errMessage = 'Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.';
      let code = 'UNAUTHORIZED';
      let errors = [];

      try {
        const errJson = await response.json();
        if (errJson?.message) errMessage = errJson.message;
        if (errJson?.code) code = errJson.code;
        if (Array.isArray(errJson?.errors)) errors = errJson.errors;
      } catch (_) {
        // Fallback if body is not JSON
      }

      authManager.handleUnauthorized();
      throw new ApiError(errMessage, 401, code, errors);
    }

    // 6. HTTP 403 Forbidden Handling (Preserve session, no logout)
    if (response.status === 403) {
      let errMessage = 'Bạn không có quyền thực hiện thao tác này.';
      let code = 'FORBIDDEN';
      let errors = [];

      try {
        const errJson = await response.json();
        if (errJson?.message) errMessage = errJson.message;
        if (errJson?.code) code = errJson.code;
        if (Array.isArray(errJson?.errors)) errors = errJson.errors;
      } catch (_) {
        // Fallback
      }

      throw new ApiError(errMessage, 403, code, errors);
    }

    // 7. HTTP 429 Too Many Requests Handling (No assumed Retry-After)
    if (response.status === 429) {
      let errMessage = 'Quá nhiều yêu cầu đăng nhập hoặc thao tác, vui lòng thử lại sau.';
      let code = 'TOO_MANY_REQUESTS';
      let errors = [];

      try {
        const errJson = await response.json();
        if (errJson?.message) errMessage = errJson.message;
        if (errJson?.code) code = errJson.code;
        if (Array.isArray(errJson?.errors)) errors = errJson.errors;
      } catch (_) {
        // Fallback
      }

      throw new ApiError(errMessage, 429, code, errors);
    }

    // 8. Content-Type Inspection & Envelope Unpacking
    const contentType = response.headers.get('content-type') || '';
    const isJson = contentType.includes('application/json');

    if (!isJson) {
      if (!response.ok) {
        const code = response.status === 404 ? 'NOT_FOUND' : (response.status >= 500 ? 'SERVER_ERROR' : 'UNKNOWN_ERROR');
        throw new ApiError(`Lỗi máy chủ (${response.status})`, response.status, code);
      }
      return null;
    }

    const envelope = await response.json();

    // Backend emits code: "SUCCESS" for all successful business responses
    if (!response.ok || envelope?.code !== 'SUCCESS') {
      const errMessage = envelope?.message || `Yêu cầu thất bại với mã ${response.status}`;
      const errCode = envelope?.code || (response.status === 400 ? 'VALIDATION_ERROR' : 'BUSINESS_ERROR');
      const errList = Array.isArray(envelope?.errors) ? envelope.errors : [];
      throw new ApiError(errMessage, response.status, errCode, errList);
    }

    // Return clean, unpacked payload
    return envelope.data !== undefined ? envelope.data : null;

  } catch (err) {
    if (err instanceof ApiError) {
      throw err;
    }

    // Discriminate between timeout, user abort, and transient transport/network failures
    const isTimeout = err.name === 'TimeoutError' || 
      (err.name === 'AbortError' && (fetchConfig.signal?.reason === 'timeout' || fetchConfig.signal?.reason?.name === 'TimeoutError'));

    if (isTimeout) {
      throw new ApiError('Yêu cầu hết thời gian chờ (Timeout). Vui lòng thử lại.', 408, 'TIMEOUT', [], err);
    }

    if (err.name === 'AbortError') {
      throw new ApiError('Yêu cầu đã bị hủy.', 0, 'ABORTED', [], err);
    }

    if (err instanceof TypeError || err.name === 'TypeError') {
      throw new ApiError('Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.', 0, 'NETWORK_ERROR', [], err);
    }

    throw new ApiError(err.message || 'Lỗi không xác định', 0, 'UNKNOWN_ERROR', [], err);
  }
}
