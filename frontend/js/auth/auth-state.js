/**
 * =============================================================================
 * SESSION & AUTHENTICATION STATE MANAGER (TASK 9A.2)
 * Module: frontend/js/auth/auth-state.js
 * 
 * Responsibilities:
 * - Single source of truth for client-side session credentials and user identity.
 * - Manages 'access_token' and 'user_info' in localStorage (untrusted convenience storage).
 * - Multi-tab session synchronization via StorageEvent.
 * - Single-owner handling of session expiration (401 Unauthorized transition).
 * - Exact role representation matching backend JSON DTOs ('Admin', 'Creator', 'Moderator', 'Learner').
 * 
 * SECURITY ARCHITECTURAL INVARIANTS:
 * 1. Storage Untrusted: localStorage is client-controlled convenience storage and MUST NOT
 *    be considered a secure security boundary. Stored credentials are vulnerable to DOM XSS.
 * 2. Backend Authority: Client-side role checks via hasRole() are for navigation visibility
 *    and UX ONLY. Spring Security (@PreAuthorize, JwtAuthenticationFilter, authorization_version DEC-42)
 *    is the SOLE AUTHORITATIVE authorization boundary.
 * 3. Exact Role Names: Backend AuthResponse.roles delivers ["Admin"], ["Creator"], ["Moderator"], ["Learner"].
 *    Client NEVER prepends 'ROLE_'.
 * 4. No Credential Logging: Passwords, tokens, and raw headers MUST NEVER be printed to console.
 * =============================================================================
 */

const TOKEN_KEY = 'access_token';
const USER_KEY = 'user_info';

let redirectHandler = defaultRedirectHandler;

/**
 * Default browser redirect behavior when a session expires.
 * Defends against infinite redirect loops if the user is already on login.html.
 */
function defaultRedirectHandler() {
  if (typeof window !== 'undefined' && window.location) {
    const pathname = window.location.pathname || '';
    if (!pathname.endsWith('login.html')) {
      window.location.href = '/login.html?expired=true';
    }
  }
}

/**
 * Safely parses JSON with defense against malformed or corrupted localStorage payloads.
 * Cleanses corrupted storage keys to prevent recurring runtime exceptions.
 * 
 * @param {string|null} raw 
 * @returns {object|null}
 */
function safeParseJson(raw) {
  if (!raw || typeof raw !== 'string') {
    return null;
  }
  try {
    const parsed = JSON.parse(raw);
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      return parsed;
    }
    // Corrupted non-object structure
    cleanseCorruptedUser();
    return null;
  } catch (_) {
    // Corrupted unparseable JSON string
    cleanseCorruptedUser();
    return null;
  }
}

function cleanseCorruptedUser() {
  try {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(USER_KEY);
    }
  } catch (_) {
    // Storage access blocked in strict sandbox
  }
}

/**
 * Central Session Manager Object
 */
export const authManager = {
  /**
   * Retrieves the raw JWT token from local storage.
   * @returns {string|null}
   */
  getToken() {
    try {
      if (typeof localStorage === 'undefined') return null;
      const token = localStorage.getItem(TOKEN_KEY);
      return token && typeof token === 'string' && token.trim().length > 0 ? token.trim() : null;
    } catch (_) {
      return null;
    }
  },

  /**
   * Retrieves parsed user identity metadata from local storage.
   * Returns null if missing or malformed.
   * 
   * @returns {{ accountId?: number, emailOrPhone?: string, fullName?: string, roles: string[] }|null}
   */
  getUser() {
    try {
      if (typeof localStorage === 'undefined') return null;
      const raw = localStorage.getItem(USER_KEY);
      const user = safeParseJson(raw);
      if (!user) return null;

      // Normalization: Ensure roles is always an array of strings
      if (!Array.isArray(user.roles)) {
        user.roles = [];
      }
      return user;
    } catch (_) {
      return null;
    }
  },

  /**
   * Returns the list of roles assigned to the current user.
   * Format: ["Admin", "Creator", "Moderator", "Learner"]
   * @returns {string[]}
   */
  getRoles() {
    const user = this.getUser();
    return user && Array.isArray(user.roles) ? [...user.roles] : [];
  },

  /**
   * Checks if the authenticated user has a specific role for UX rendering.
   * Enforces exact case-sensitive matching against backend JSON role names.
   * STRICT: Rejects 'ROLE_' prefixes.
   * 
   * @param {string} roleName e.g. "Admin", "Creator", "Moderator", "Learner"
   * @returns {boolean}
   */
  hasRole(roleName) {
    if (!roleName || typeof roleName !== 'string') {
      return false;
    }
    const cleanRole = roleName.trim();
    if (cleanRole.startsWith('ROLE_')) {
      // Invariant: Backend JSON roles NEVER have 'ROLE_' prefix. Reject to prevent architectural drift.
      return false;
    }
    const roles = this.getRoles();
    return roles.includes(cleanRole);
  },

  /**
   * Checks whether an active access token is present.
   * @returns {boolean}
   */
  isAuthenticated() {
    return Boolean(this.getToken());
  },

  /**
   * Stores credentials from backend AuthResponse into local storage.
   * AuthResponse contract:
   * {
   *   token: string,
   *   type: "Bearer",
   *   accountId: number,
   *   emailOrPhone: string,
   *   fullName: string,
   *   roles: string[]
   * }
   * 
   * @param {object} authData Unpacked response.data from /api/v1/auth/login or register
   */
  setSession(authData) {
    if (!authData || typeof authData !== 'object') {
      throw new TypeError('authData must be a non-null object');
    }

    const token = authData.token;
    if (!token || typeof token !== 'string') {
      throw new Error('Invalid AuthResponse: missing required string field "token"');
    }

    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(TOKEN_KEY, token.trim());

        const userInfo = {
          accountId: authData.accountId ?? null,
          emailOrPhone: authData.emailOrPhone ?? '',
          fullName: authData.fullName ?? '',
          roles: Array.isArray(authData.roles) ? authData.roles : []
        };

        localStorage.setItem(USER_KEY, JSON.stringify(userInfo));
      }

      if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
        window.dispatchEvent(new CustomEvent('auth:login', {
          detail: {
            accountId: authData.accountId,
            emailOrPhone: authData.emailOrPhone,
            fullName: authData.fullName,
            roles: Array.isArray(authData.roles) ? [...authData.roles] : []
          }
        }));
      }
    } catch (err) {
      console.warn('Failed to persist session to storage:', err.message);
    }
  },

  /**
   * Updates existing user profile metadata in storage without modifying the token.
   * Dispatches 'auth:update' event so navigation components can react immediately.
   * 
   * @param {object} partialUser e.g. { fullName: 'New Name' }
   */
  updateUser(partialUser) {
    if (!partialUser || typeof partialUser !== 'object') {
      return;
    }
    const current = this.getUser() || {};
    const updated = {
      ...current,
      ...partialUser
    };
    if (!Array.isArray(updated.roles)) {
      updated.roles = Array.isArray(current.roles) ? current.roles : [];
    }
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(USER_KEY, JSON.stringify(updated));
      }
      if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
        window.dispatchEvent(new CustomEvent('auth:update', { detail: { user: updated } }));
      }
    } catch (err) {
      console.warn('Failed to update user session in storage:', err.message);
    }
  },

  /**
   * Purges access token and user identity from local storage idempotently.
   */
  clearSession() {
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
      }
    } catch (_) {
      // Storage access blocked in strict sandbox
    }
  },

  /**
   * SINGLE OWNER FOR 401 UNAUTHORIZED / SESSION EXPIRATION TRANSITIONS.
   * 
   * Invariant:
   * ONE unauthorized response
   *   -> ONE session expiration transition
   *   -> ONE 'auth:expired' event
   *   -> AT MOST ONE navigation redirect.
   * 
   * Prevents dual event firing and race condition loops between apiClient and authManager.
   */
  handleUnauthorized() {
    this.clearSession();

    if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
      window.dispatchEvent(new CustomEvent('auth:expired'));
    }

    if (typeof redirectHandler === 'function') {
      try {
        redirectHandler();
      } catch (_) {
        // Suppress redirect execution errors in headless test runners
      }
    }
  },

  /**
   * Performs user logout.
   * Clears credentials and dispatches 'auth:logout'.
   * 
   * @param {boolean} [shouldRedirect=true]
   */
  logout(shouldRedirect = true) {
    this.clearSession();

    if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
      window.dispatchEvent(new CustomEvent('auth:logout'));
    }

    if (shouldRedirect && typeof redirectHandler === 'function') {
      try {
        redirectHandler();
      } catch (_) {
        // Suppress redirect execution errors in headless test runners
      }
    }
  },

  /**
   * Configures a custom redirect handler for testing or router integration.
   * @param {Function|null} handler 
   */
  setRedirectHandler(handler) {
    redirectHandler = typeof handler === 'function' ? handler : defaultRedirectHandler;
  },

  /**
   * Resets redirect handler back to default browser behavior.
   */
  resetRedirectHandler() {
    redirectHandler = defaultRedirectHandler;
  }
};

/**
 * Multi-tab synchronization:
 * When another browser tab clears 'access_token', this tab immediately synchronizes
 * by transitioning session state cleanly without infinite reload loops.
 */
if (typeof window !== 'undefined' && typeof window.addEventListener === 'function') {
  window.addEventListener('storage', (event) => {
    // Only respond to remote storage changes (StorageEvent is not triggered in origin tab)
    if (event.key === TOKEN_KEY) {
      if (!event.newValue && event.oldValue) {
        // Another tab logged out
        authManager.handleUnauthorized();
      } else if (event.newValue && event.newValue !== event.oldValue) {
        // Another tab logged in
        window.dispatchEvent(new CustomEvent('auth:login', { detail: authManager.getUser() }));
      }
    } else if (event.key === USER_KEY) {
      // Another tab updated profile metadata
      window.dispatchEvent(new CustomEvent('auth:update', { detail: { user: authManager.getUser() } }));
    }
  });
}
