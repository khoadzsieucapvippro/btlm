/**
 * =============================================================================
 * HEADLESS BROWSER RUNNER & VERIFICATION UTILITIES
 * Powered by playwright-core with resilient multi-browser resolution.
 * Automatically captures pageerror and console.error.
 * =============================================================================
 */

import { chromium } from 'playwright-core';
import fs from 'node:fs';
import path from 'node:path';

/**
 * Discovers the best available Chromium/Chrome/Edge executable.
 * @returns {string|undefined} executable path or undefined for default channel
 */
function discoverExecutablePath() {
  // 1. Explicit override via environment
  if (process.env.CHROME_BIN && fs.existsSync(process.env.CHROME_BIN)) {
    return process.env.CHROME_BIN;
  }

  // 2. Playwright cached chromium binary
  const localAppData = process.env.LOCALAPPDATA || path.join(process.env.USERPROFILE || '', 'AppData', 'Local');
  const candidates = [
    path.join(localAppData, 'ms-playwright', 'chromium-1200', 'chrome-win', 'chrome.exe'),
    path.join(localAppData, 'ms-playwright', 'chromium_headless_shell-1200', 'chrome-win', 'headless_shell.exe'),
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe'
  ];

  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }

  return undefined;
}

/**
 * Launches a browser instance.
 * @param {Object} [options]
 * @param {boolean} [options.headless=true]
 * @returns {Promise<import('playwright-core').Browser>}
 */
export async function launchBrowser({ headless = true } = {}) {
  const executablePath = discoverExecutablePath();
  const launchOptions = {
    headless,
    args: [
      '--no-sandbox',
      '--disable-dev-shm-usage',
      '--disable-gpu',
      '--no-default-browser-check'
    ]
  };

  if (executablePath) {
    launchOptions.executablePath = executablePath;
  } else {
    // Attempt system channel fallback
    launchOptions.channel = 'chrome';
  }

  try {
    return await chromium.launch(launchOptions);
  } catch (err) {
    // If channel 'chrome' failed, try 'msedge'
    if (!launchOptions.executablePath) {
      return await chromium.launch({ ...launchOptions, channel: 'msedge' });
    }
    throw err;
  }
}

/**
 * Creates an isolated browser page monitored for both console.error and pageerror.
 * @param {import('playwright-core').Browser} browser
 * @param {Object} [contextOptions]
 * @param {number} [contextOptions.viewportWidth=1280]
 * @param {number} [contextOptions.viewportHeight=720]
 * @returns {Promise<{ page: import('playwright-core').Page, context: import('playwright-core').BrowserContext, getRuntimeErrors: () => string[] }>}
 */
export async function createMonitoredPage(browser, { viewportWidth = 1280, viewportHeight = 720 } = {}) {
  const context = await browser.newContext({
    viewport: { width: viewportWidth, height: viewportHeight }
  });

  const page = await context.newPage();
  const runtimeErrors = [];

  // Question 6 Contract: MUST capture both pageerror and console.error
  page.on('pageerror', (err) => {
    runtimeErrors.push(`[PageError] ${err.message}\n${err.stack || ''}`);
  });

  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      const text = msg.text();
      // Filter out benign browser-internal favicon.ico 404s
      if (text.includes('favicon.ico')) {
        return;
      }
      runtimeErrors.push(`[ConsoleError] ${text}`);
    }
  });

  return {
    page,
    context,
    getRuntimeErrors: () => [...runtimeErrors]
  };
}
