/**
 * =============================================================================
 * CSP EVALUATION TEST PROBE (TASK 9G.3)
 * File: tests/frontend/fixtures/csp-eval-probe.js
 * 
 * Verifies that code running within an authorized 'self' script context cannot
 * bypass Content Security Policy to execute eval() or dynamic Function constructors
 * when 'unsafe-eval' is absent from script-src.
 * =============================================================================
 */

try {
  window.__probeEvalResult = window.eval('42');
} catch (err) {
  window.__probeEvalError = err.message;
}

try {
  const fn = new Function('return 84');
  window.__probeFunctionResult = fn();
} catch (err) {
  window.__probeFunctionError = err.message;
}
