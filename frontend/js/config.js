/**
 * =============================================================================
 * RUNTIME ENVIRONMENT CONFIGURATION (TASK FE-REMEDIATION-03)
 * Module: frontend/js/config.js
 * 
 * Architectural Invariants:
 * 1. Single Source of Truth: Configures runtime environment settings for frontend execution.
 * 2. Deterministic Dev Routing: When served from standard local static dev servers
 *    (localhost / 127.0.0.1 on ports 3000, 5500, 5173, 8000, 8081), automatically points
 *    API requests to the local Spring Boot backend ('http://localhost:8080/api/v1').
 * 3. Same-Origin Production Default: In same-origin deployments (e.g. Nginx reverse proxy,
 *    production domains, or when served on port 8080 directly), defaults to relative '/api/v1'.
 * 4. Override Preservation: If window.__ENV__ is pre-injected (e.g., by container injection,
 *    server-side templating, or automated test runners), respects existing API_BASE_URL.
 * 5. Immutability: The resulting configuration object is frozen via Object.freeze.
 * =============================================================================
 */

(function initRuntimeConfig(globalScope) {
  'use strict';

  const root = typeof window !== 'undefined'
    ? window
    : (typeof globalScope !== 'undefined' ? globalScope : globalThis);

  if (!root) {
    return;
  }

  // Preserve pre-existing __ENV__ properties if already defined
  const currentEnv = (root.__ENV__ && typeof root.__ENV__ === 'object') ? root.__ENV__ : {};

  let apiBaseUrl = currentEnv.API_BASE_URL;

  if (!apiBaseUrl) {
    const loc = root.location;
    if (loc) {
      const hostname = loc.hostname || '';
      const port = String(loc.port || '');

      const isLocalhost = hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '[::1]';
      const isDevStaticPort = ['3000', '5500', '5173', '8000', '8081'].includes(port);

      if (isLocalhost && isDevStaticPort) {
        apiBaseUrl = 'http://localhost:8080/api/v1';
      } else {
        apiBaseUrl = '/api/v1';
      }
    } else {
      apiBaseUrl = '/api/v1';
    }
  }

  // Define frozen read-only configuration
  root.__ENV__ = Object.freeze({
    ...currentEnv,
    API_BASE_URL: apiBaseUrl
  });

})(typeof window !== 'undefined' ? window : (typeof globalThis !== 'undefined' ? globalThis : this));
