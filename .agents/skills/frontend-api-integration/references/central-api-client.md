# Centralized HTTP API Client Architecture (`frontend/js/api/api.js`)

## 1. Architectural Role & Spring Boot Alignment

The centralized HTTP API Client (`frontend/js/api/api.js`) serves as the **single gateway** for all network communication between the Vanilla JS frontend and the Spring Boot REST API.

### 1.1 Strict Architectural Invariants
- **NO page script may call native `window.fetch()` directly**. All requests MUST pass through `apiClient()`.
- **Configurable Base URL Strategy**:
  - In production / same-origin deployments (where Spring Boot or Nginx serves both static assets and API), the client SHOULD default to the relative path `/api/v1`.
  - In local development or cross-origin environments, the base URL MAY be configured via `window.__ENV__?.API_BASE_URL` (e.g., `http://localhost:8080/api/v1`).
  - **Runtime Configuration Invariant**: `window.__ENV__` is permitted EXCLUSIVELY for read-only runtime deployment configuration. Application state, authentication state, user data, and page state MUST NOT be stored on `window`. `window.__ENV__` is NOT a security boundary.
- Automatically injects the JWT Bearer token from the session manager.
- Unpacks the project's standard envelope contract `ApiResponse<T>` (Most business API responses use `ApiResponse<T>`. Endpoint-specific response behavior takes precedence. HTTP 204 responses contain no body and therefore do not contain `ApiResponse<T>`):
  ```json
  {
    "code": "SUCCESS",
    "message": "Thao tác thành công",
    "data": { ... },
    "errors": []
  }
  ```

---

## 2. Implementation Pattern (`frontend/js/api/api.js`)

```javascript
// frontend/js/api/api.js

// Configurable Base URL: Defaults to same-origin relative path '/api/v1'
// Note: window.__ENV__ is strictly read-only deployment configuration
const API_BASE_URL = (window.__ENV__?.API_BASE_URL || '/api/v1').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(message, status, code, errors = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.errors = errors; // Validation error strings (List<String> "field: message") from GlobalExceptionHandler
  }
}

export async function apiClient(endpoint, options = {}) {
  const normalizedEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  const url = `${API_BASE_URL}${normalizedEndpoint}`;
  const method = (options.method || 'GET').toUpperCase();
  
  // 1. Headers Configuration
  const headers = new Headers(options.headers || {});
  
  // Do NOT set Content-Type for FormData; browser boundary injection required
  if (!(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  // 2. Token Injection
  const token = localStorage.getItem('access_token');
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  // 3. Timeout Handling via AbortSignal.timeout()
  // Default: 15s for standard requests, 60s for file uploads
  const isUpload = options.body instanceof FormData;
  const timeoutMs = options.timeout || (isUpload ? 60000 : 15000);
  const timeoutSignal = AbortSignal.timeout(timeoutMs);

  const fetchConfig = {
    ...options,
    method,
    headers,
    signal: options.signal ? AbortSignal.any([options.signal, timeoutSignal]) : timeoutSignal
  };

  try {
    const response = await executeFetchWithSafeRetry(url, fetchConfig, method);

    // 4. Central HTTP Status Handling
    if (response.status === 401) {
      handleUnauthorizedSession();
      throw new ApiError('Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.', 401, 'UNAUTHORIZED');
    }

    if (response.status === 403) {
      throw new ApiError('Bạn không có quyền thực hiện thao tác này.', 403, 'FORBIDDEN');
    }

    if (response.status === 429) {
      let message = 'Quá nhiều yêu cầu đăng nhập hoặc thao tác, vui lòng thử lại sau.';
      let code = 'TOO_MANY_REQUESTS';
      try {
        const errJson = await response.json();
        if (errJson?.message) message = errJson.message;
        if (errJson?.code) code = errJson.code;
      } catch (_) {
        // Fallback if response body is not JSON
      }
      throw new ApiError(message, 429, code);
    }

    // 5. Parse Response Envelope
    // Explicitly handle 204 No Content (DELETE /api/v1/admin/* and /api/v1/creator/lessons/*)
    if (response.status === 204) {
      return null; // 204 No Content has no body; response.json() must NOT be called
    }

    const isJson = response.headers.get('content-type')?.includes('application/json');
    if (!isJson) {
      if (!response.ok) {
        throw new ApiError(`Lỗi máy chủ (${response.status})`, response.status, 'UNKNOWN_ERROR');
      }
      return null;
    }

    const envelope = await response.json();

    // Backend emits code: "SUCCESS" for all successful 200 OK and 201 Created responses (verified from ApiResponse.java & integration tests).
    if (!response.ok || envelope.code !== 'SUCCESS') {
      const errMessage = envelope.message || `Yêu cầu thất bại với mã ${response.status}`;
      throw new ApiError(errMessage, response.status, envelope.code || 'BUSINESS_ERROR', envelope.errors || []);
    }

    return envelope.data; // Return clean unpacked data payload

  } catch (err) {
    // Discriminate between timeout, user abort, and transient transport/network failures
    if (err.name === 'TimeoutError' || (err.name === 'AbortError' && fetchConfig.signal?.reason === 'timeout')) {
      throw new ApiError('Yêu cầu hết thời gian chờ (Timeout). Vui lòng thử lại.', 408, 'TIMEOUT');
    }
    if (err.name === 'AbortError') {
      throw new ApiError('Yêu cầu đã bị hủy.', 0, 'ABORTED');
    }
    // Fetch API standard: network failures reject with TypeError across modern browsers
    if (err instanceof TypeError || err.name === 'TypeError') {
      throw new ApiError('Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.', 0, 'NETWORK_ERROR');
    }
    throw err;
  }
}

// 6. Safe Idempotent Retry Policy
// STRICT RULES:
// - ONLY GET requests MAY be retried automatically (max 1 retry).
// - ONLY retry for transient transport-level failures (e.g. connection drops, offline).
// - MUST NOT retry:
//   * HTTP 4xx / 5xx server responses
//   * 401 / 403 authorization failures
//   * User cancellations or aborted requests (AbortError)
//   * Deterministic validation errors
//   * Any request known to be non-retryable
// - Mutation requests (POST, PUT, DELETE) MUST NEVER be retried automatically.
async function executeFetchWithSafeRetry(url, config, method) {
  try {
    return await fetch(url, config);
  } catch (err) {
    const isIdempotent = method === 'GET';
    // Transient network failure in Fetch standard rejects with TypeError, distinct from AbortError/TimeoutError
    const isTransientNetworkError = (err instanceof TypeError || err.name === 'TypeError') 
      && err.name !== 'AbortError' 
      && err.name !== 'TimeoutError';
    
    if (isIdempotent && isTransientNetworkError) {
      // Single retry after 1000ms delay for transient transport failures
      await new Promise(resolve => setTimeout(resolve, 1000));
      return await fetch(url, config);
    }
    throw err;
  }
}

function handleUnauthorizedSession() {
  localStorage.removeItem('access_token');
  localStorage.removeItem('user_info');
  window.dispatchEvent(new CustomEvent('auth:expired'));
  if (!window.location.pathname.endsWith('login.html')) {
    window.location.href = '/login.html?expired=true';
  }
}
```

---

## 3. Field-Level Error Normalization

When the backend returns HTTP 400 with validation violations, `GlobalExceptionHandler` populates `envelope.errors` as a `List<String>` where each element is formatted as `"${field}: ${message}"`:
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "data": null,
  "errors": [
    "emailOrPhone: Email or phone is required",
    "password: Password must be between 6 and 100 characters"
  ]
}
```

The consumer page catches `ApiError` and passes `err.errors` directly to `displayFieldErrors()` (which invokes `parseFieldErrors()` from `ui-states-and-forms.md` to split on delimiter `:` and bind errors to the matching input fields).

---

## 4. Source Basis

- **WHATWG Fetch Living Standard**:
  - Fetch Specification & Network Errors (`TypeError` on network failures): [WHATWG Fetch](https://fetch.spec.whatwg.org/#concept-network-error) — `AUTHORITATIVE`
  - Aborting Fetch Operations: [AbortSignal](https://fetch.spec.whatwg.org/#abort-fetch) — `AUTHORITATIVE`
- **IETF RFCs**:
  - RFC 9110 (HTTP Semantics) — Section 9.2.1 Safe & Idempotent Methods (GET retry safety): [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html#section-9.2.1) — `AUTHORITATIVE`
  - RFC 6750 — The OAuth 2.0 Authorization Framework: Bearer Token Usage: [RFC 6750](https://www.rfc-editor.org/rfc/rfc6750) — `AUTHORITATIVE`
- **MDN Web Docs**:
  - Using Fetch: [Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch) — `AUTHORITATIVE`
  - AbortSignal.timeout(): [AbortSignal.timeout()](https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal/timeout_static) — `AUTHORITATIVE`
- **OWASP**:
  - REST Security Cheat Sheet: [OWASP REST Security](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html) — `AUTHORITATIVE`
