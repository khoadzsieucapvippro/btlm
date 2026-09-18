/**
 * =============================================================================
 * BROWSER E2E TEST SUITE: CONTENT SECURITY POLICY (CSP) ENFORCEMENT (TASK 9G.3)
 * File: tests/frontend/e2e/csp-enforcement.browser.mjs
 * 
 * Verifies live browser enforcement of the Content Security Policy:
 * - CSP-01: Document Delivery Boundary - HTML document receives Content-Security-Policy header.
 * - CSP-02: Positive Legitimate Resource Loading - Application JS, Bootstrap JS, CSS,
 *           favicons, and images load without triggering CSP violations.
 * - CSP-03: Negative Probe - Inline script execution is blocked by Chromium CSP.
 * - CSP-04: Negative Probe - eval() and dynamic code evaluation are blocked by CSP.
 * - CSP-05: Negative Probe - Unauthorized third-party scripts are blocked by CSP.
 * - CSP-06: Negative Probe - Child frames are blocked by frame-src 'none'.
 * - CSP-07: Negative Probe - Frame embedding is blocked by frame-ancestors 'none'.
 * - CSP-08: Connect-Src Origin Boundary - Verifies permitted vs forbidden connect origins.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

describe('Browser E2E: Content Security Policy (CSP) Enforcement (Task 9G.3)', () => {
  let serverHandle;
  let browser;
  let baseUrl;

  before(async () => {
    serverHandle = await ensureServer();
    baseUrl = serverHandle.baseUrl;
    browser = await launchBrowser({ headless: true });
  });

  after(async () => {
    if (browser) {
      await browser.close();
    }
    if (serverHandle && typeof serverHandle.close === 'function') {
      await serverHandle.close();
    }
  });

  // Helper to attach a security policy violation listener to the page
  async function attachViolationListener(page) {
    await page.addInitScript(() => {
      window.__cspViolations = [];
      window.addEventListener('securitypolicyviolation', (e) => {
        window.__cspViolations.push({
          violatedDirective: e.violatedDirective,
          effectiveDirective: e.effectiveDirective,
          blockedURI: e.blockedURI,
          disposition: e.disposition,
          statusCode: e.statusCode
        });
      });
    });
  }

  // ---------------------------------------------------------------------------
  // CSP-01: Document Delivery Boundary & HTTP Header Verification
  // ---------------------------------------------------------------------------
  test('CSP-01: Frontend HTML document receives valid Content-Security-Policy header', async () => {
    const context = await browser.newContext();
    const page = await context.newPage();

    const response = await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
    assert.ok(response, 'Response must exist');
    assert.strictEqual(response.status(), 200, 'Page status must be 200 OK');

    const headers = response.headers();
    const cspHeader = headers['content-security-policy'];
    assert.ok(cspHeader, 'Response must contain Content-Security-Policy header');

    // Verify key directives are present in the delivered header
    assert.match(cspHeader, /default-src 'self'/, 'Must contain default-src');
    assert.match(cspHeader, /script-src 'self' https:\/\/cdn\.jsdelivr\.net/, 'Must contain script-src');
    assert.match(cspHeader, /style-src 'self' https:\/\/cdn\.jsdelivr\.net/, 'Must contain style-src');
    assert.match(cspHeader, /font-src 'self'/, 'Must contain font-src');
    assert.match(cspHeader, /object-src 'none'/, 'Must contain object-src none');
    assert.match(cspHeader, /frame-ancestors 'none'/, 'Must contain frame-ancestors none');
    assert.match(cspHeader, /frame-src 'none'/, 'Must contain frame-src none');
    assert.match(cspHeader, /connect-src 'self'/, 'Must contain connect-src');

    // Verify absence of dangerous keywords in script-src
    assert.doesNotMatch(cspHeader, /script-src[^;]*'unsafe-inline'/, 'script-src must NOT contain unsafe-inline');
    assert.doesNotMatch(cspHeader, /script-src[^;]*'unsafe-eval'/, 'script-src must NOT contain unsafe-eval');

    await context.close();
  });

  // ---------------------------------------------------------------------------
  // CSP-02: Positive Legitimate Resource Loading & Clean Execution
  // ---------------------------------------------------------------------------
  test('CSP-02: Application JS, Bootstrap, CSS, and legitimate assets load without CSP violations', async () => {
    const context = await browser.newContext();
    const page = await context.newPage();
    await attachViolationListener(page);

    await page.goto(`${baseUrl}/index.html`, { waitUntil: 'networkidle' });

    // 1. Verify Application JS initialized
    const hasConfig = await page.evaluate(() => typeof window.__ENV__ !== 'undefined');
    assert.strictEqual(hasConfig, true, '__ENV__ must be defined by config.js');

    // 2. Verify Bootstrap JS bundle loaded from jsDelivr
    const hasBootstrap = await page.evaluate(() => typeof window.bootstrap !== 'undefined');
    assert.strictEqual(hasBootstrap, true, 'Bootstrap bundle must be loaded and available on window');

    // 3. Verify CSS styling computed properly
    const bodyBg = await page.evaluate(() => {
      const el = document.querySelector('body');
      return window.getComputedStyle(el).backgroundColor;
    });
    assert.ok(bodyBg, 'Body background must be computed');

    // 4. Verify ZERO CSP violations occurred during natural page lifecycle
    const violations = await page.evaluate(() => window.__cspViolations || []);
    assert.strictEqual(
      violations.length,
      0,
      `Expected 0 CSP violations for legitimate assets, but got: ${JSON.stringify(violations, null, 2)}`
    );

    await context.close();
  });

  // ---------------------------------------------------------------------------
  // CSP-03: Negative Probe - Inline Script Blocked by CSP
  // ---------------------------------------------------------------------------
  test('CSP-03: Dynamically injected inline script is strictly blocked by Chromium CSP', async () => {
    const context = await browser.newContext();
    const page = await context.newPage();
    await attachViolationListener(page);

    await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });

    // Attempt to inject and execute an inline script element
    await page.evaluate(() => {
      window.__inlineScriptExecuted = false;
      const script = document.createElement('script');
      script.textContent = 'window.__inlineScriptExecuted = true;';
      document.body.appendChild(script);
    });

    // Verify the inline script was prevented from executing
    const executed = await page.evaluate(() => window.__inlineScriptExecuted);
    assert.strictEqual(executed, false, 'Inline script MUST NOT execute under strict CSP');

    // Verify Chromium fired a securitypolicyviolation event
    const violations = await page.evaluate(() => window.__cspViolations || []);
    const scriptViolation = violations.find(v =>
      v.violatedDirective && v.violatedDirective.startsWith('script-src')
    );
    assert.ok(
      scriptViolation,
      `Expected script-src violation for inline script, but found violations: ${JSON.stringify(violations)}`
    );
    assert.strictEqual(scriptViolation.disposition, 'enforce', 'Violation must be enforced');

    await context.close();
  });

  // ---------------------------------------------------------------------------
  // CSP-04: Negative Probe - eval() and Function Constructor Blocked
  // ---------------------------------------------------------------------------
  test('CSP-04: Dynamic code execution via eval() and Function() is strictly blocked by CSP', async () => {
    const context = await browser.newContext();
    const page = await context.newPage();
    await attachViolationListener(page);

    await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });

    // Load an authorized 'self' script that attempts to invoke eval() and Function()
    await page.evaluate(() => {
      return new Promise((resolve) => {
        const script = document.createElement('script');
        script.src = '/__test__/eval-probe.js';
        script.onload = resolve;
        script.onerror = resolve;
        document.body.appendChild(script);
      });
    });

    const evalError = await page.evaluate(() => window.__probeEvalError);
    const evalResult = await page.evaluate(() => window.__probeEvalResult);
    const fnError = await page.evaluate(() => window.__probeFunctionError);
    const fnResult = await page.evaluate(() => window.__probeFunctionResult);

    assert.strictEqual(evalResult, undefined, 'eval() must not return a value');
    assert.match(
      evalError || '',
      /(Content Security Policy|unsafe-eval)/i,
      'Chromium must report unsafe-eval CSP blockage for eval()'
    );

    assert.strictEqual(fnResult, undefined, 'Function() constructor must not return a value');
    assert.match(
      fnError || '',
      /(Content Security Policy|unsafe-eval)/i,
      'Chromium must report unsafe-eval CSP blockage for Function()'
    );

    // Verify violation event fired with blockedURI === 'eval'
    const violations = await page.evaluate(() => window.__cspViolations || []);
    const evalViolation = violations.find(v =>
      (v.violatedDirective && v.violatedDirective.startsWith('script-src')) &&
      v.blockedURI === 'eval'
    );
    assert.ok(evalViolation, 'Chromium must report script-src violation for eval');

    await context.close();
  });

  // ---------------------------------------------------------------------------
  // CSP-05: Negative Probe - Unauthorized External Script Blocked
  // ---------------------------------------------------------------------------
  test('CSP-05: Unauthorized external script origin is strictly blocked by CSP', async () => {
    const context = await browser.newContext();
    const page = await context.newPage();
    await attachViolationListener(page);

    await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });

    // Attempt to load script from unauthorized origin
    const unauthorizedUrl = 'https://unauthorized-cdn.example.com/exploit.js';
    await page.evaluate((src) => {
      const script = document.createElement('script');
      script.src = src;
      document.head.appendChild(script);
    }, unauthorizedUrl);

    // Wait a short moment for browser CSP check
    await page.waitForTimeout(300);

    const violations = await page.evaluate(() => window.__cspViolations || []);
    const unauthorizedViolation = violations.find(v =>
      v.blockedURI && v.blockedURI.includes('unauthorized-cdn.example.com')
    );
    assert.ok(
      unauthorizedViolation,
      `Expected CSP violation for unauthorized domain, but violations were: ${JSON.stringify(violations)}`
    );

    await context.close();
  });

  // ---------------------------------------------------------------------------
  // CSP-06: Negative Probe - Child Iframe Loading Blocked (frame-src 'none')
  // ---------------------------------------------------------------------------
  test('CSP-06: Loading child frames is strictly blocked by frame-src "none"', async () => {
    const context = await browser.newContext();
    const page = await context.newPage();
    await attachViolationListener(page);

    await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });

    // Attempt to create a child iframe
    const targetUrl = 'https://example.com';
    await page.evaluate((src) => {
      const iframe = document.createElement('iframe');
      iframe.src = src;
      document.body.appendChild(iframe);
    }, targetUrl);

    await page.waitForTimeout(300);

    const violations = await page.evaluate(() => window.__cspViolations || []);
    const frameViolation = violations.find(v =>
      (v.violatedDirective && v.violatedDirective.startsWith('frame-src')) ||
      (v.effectiveDirective && v.effectiveDirective.startsWith('frame-src'))
    );
    assert.ok(
      frameViolation,
      `Expected frame-src violation for child iframe, but violations were: ${JSON.stringify(violations)}`
    );

    await context.close();
  });

  // ---------------------------------------------------------------------------
  // CSP-07: Negative Probe - Frame Embedding Blocked (frame-ancestors 'none')
  // ---------------------------------------------------------------------------
  test('CSP-07: Embedding application page in an iframe is blocked by frame-ancestors "none"', async () => {
    const context = await browser.newContext();
    const parentPage = await context.newPage();

    // In a blank parent document, attempt to embed the application via iframe
    await parentPage.setContent(`
      <!DOCTYPE html>
      <html>
      <head><title>Parent Attacker Frame</title></head>
      <body>
        <iframe id="testFrame" src="${baseUrl}/index.html"></iframe>
      </body>
      </html>
    `);

    const frameEl = parentPage.frameLocator('#testFrame');
    
    // Because frame-ancestors is 'none', Chromium blocks the response from rendering inside the frame
    // In Playwright, navigating the frame to a page with frame-ancestors 'none' results in an empty or blocked frame
    await parentPage.waitForTimeout(500);

    // Verify the inner document of the iframe failed to load or has no main application content
    let embeddedMainPresent = false;
    try {
      const mainLocator = frameEl.locator('#mainContent');
      embeddedMainPresent = await mainLocator.isVisible({ timeout: 1000 });
    } catch {
      embeddedMainPresent = false;
    }

    assert.strictEqual(
      embeddedMainPresent,
      false,
      'Application content MUST NOT be accessible when embedded in a third-party frame'
    );

    await context.close();
  });

  // ---------------------------------------------------------------------------
  // CSP-08: Connect-Src Origin Boundary Verification
  // ---------------------------------------------------------------------------
  test('CSP-08: Unauthorized fetch() origin triggers connect-src CSP violation', async () => {
    const context = await browser.newContext();
    const page = await context.newPage();
    await attachViolationListener(page);

    await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });

    // Attempt to fetch from unauthorized origin
    const unauthorizedApi = 'https://unauthorized-api.example.com/data';
    await page.evaluate(async (url) => {
      try {
        await fetch(url, { mode: 'no-cors' });
      } catch {
        // Expected network/CSP error
      }
    }, unauthorizedApi);

    await page.waitForTimeout(300);

    const violations = await page.evaluate(() => window.__cspViolations || []);
    const connectViolation = violations.find(v =>
      (v.violatedDirective && v.violatedDirective.startsWith('connect-src')) ||
      (v.effectiveDirective && v.effectiveDirective.startsWith('connect-src'))
    );
    assert.ok(
      connectViolation,
      `Expected connect-src violation for forbidden origin, but violations were: ${JSON.stringify(violations)}`
    );

    await context.close();
  });

});
