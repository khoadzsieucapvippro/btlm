/**
 * =============================================================================
 * FULL-STACK TEST DATA GENERATOR & SANITIZER
 * File: tests/fullstack/utils/test-data.mjs
 * 
 * Invariants:
 * - Disposable, unique test accounts per test run (no hardcoded credentials).
 * - Never log raw passwords, JWTs, or Authorization headers.
 * - Formats conform strictly to backend validation:
 *   - fullName: 2..50 chars
 *   - emailOrPhone: valid email or 10-digit Vietnamese phone
 *   - password: 6..100 chars (with lowercase, uppercase, digit, special char)
 * =============================================================================
 */

/**
 * Generates a unique disposable learner account for fullstack verification.
 * @param {string} [prefix='fs_learner'] 
 * @returns {{ fullName: string, emailOrPhone: string, password: string }}
 */
export function generateDisposableUser(prefix = 'fs_learner') {
  const timestamp = Date.now();
  const rand = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
  
  return {
    fullName: `Học Viên Test ${timestamp.toString().slice(-4)}`,
    emailOrPhone: `${prefix}_${timestamp}_${rand}@example.com`,
    password: `Pass_${timestamp}!123`
  };
}

/**
 * Safely sanitizes sensitive fields in an object before logging.
 * Replaces token, password, and authorization with '[PROTECTED]'.
 * @param {any} obj 
 * @returns {any}
 */
export function sanitizeForLog(obj) {
  if (!obj || typeof obj !== 'object') {
    return obj;
  }
  if (Array.isArray(obj)) {
    return obj.map(sanitizeForLog);
  }

  const sanitized = {};
  for (const [key, value] of Object.entries(obj)) {
    const lowerKey = key.toLowerCase();
    if (lowerKey.includes('password') || lowerKey.includes('token') || lowerKey.includes('secret') || lowerKey.includes('authorization')) {
      sanitized[key] = '[PROTECTED]';
    } else if (typeof value === 'object' && value !== null) {
      sanitized[key] = sanitizeForLog(value);
    } else {
      sanitized[key] = value;
    }
  }
  return sanitized;
}
