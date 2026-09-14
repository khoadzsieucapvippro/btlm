/**
 * =============================================================================
 * BROWSER SMOKE TEST: FOUNDATION & INTERACTION FLOWS (TASK 9A.1)
 * Scope: Representative smoke flows (B1 Desktop, B2 Mobile, B3 Modal,
 * B4 3-State UI, B5 Toast Notifications).
 * Zero redundant viewport repetitions. Dual pageerror & console.error capture.
 * =============================================================================
 */

import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { ensureServer } from '../utils/static-server.mjs';
import { launchBrowser, createMonitoredPage } from '../utils/browser-runner.mjs';

describe('Browser Smoke: Foundation & Interactive Shell', () => {
  let serverHandle;
  let browser;
  let baseUrl;

  before(async () => {
    serverHandle = await ensureServer({ port: 3000 });
    baseUrl = serverHandle.baseUrl;
    browser = await launchBrowser({ headless: true });
  });

  after(async () => {
    if (browser) await browser.close();
    if (serverHandle) await serverHandle.close();
  });

  // B1: Desktop Shell
  test('B1: Desktop Shell (1280x720) renders landmarks without horizontal overflow or runtime errors', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('header.site-header').waitFor({ state: 'visible' });

      // Title & Heading
      const title = await page.title();
      assert.ok(title.includes('Bộ Thủ & Từ Vựng Tiếng Trung'), `Unexpected page title: "${title}"`);

      const h1Text = await page.locator('h1').textContent();
      assert.ok(h1Text.includes('Nền Tảng Học Bộ Thủ'), `Unexpected h1 text: "${h1Text}"`);

      // Landmarks
      assert.strictEqual(await page.locator('header.site-header').count(), 1, 'Header must be present');
      assert.strictEqual(await page.locator('nav.navbar').count(), 1, 'Nav must be present');
      assert.strictEqual(await page.locator('main.main-content-wrapper').count(), 1, 'Main landmark must be present');
      assert.strictEqual(await page.locator('footer.site-footer').count(), 1, 'Footer must be present');

      // Zero Horizontal Overflow
      const overflow = await page.evaluate(() => {
        return document.documentElement.scrollWidth > window.innerWidth;
      });
      assert.strictEqual(overflow, false, 'Desktop viewport (1280px) must not have horizontal page overflow');

      // Dual Error Catching Contract (Question 6)
      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors on desktop load:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // B2: Mobile Shell
  test('B2: Mobile Shell (375x667) toggles disclosure menu and maintains zero overflow', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 375,
      viewportHeight: 667
    });

    try {
      await page.goto(`${baseUrl}/index.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForFunction(() => typeof window.bootstrap !== 'undefined', { timeout: 10000 }).catch(() => {});

      const toggler = page.locator('button.navbar-toggler');
      const navCollapse = page.locator('#primaryNavMenu');

      assert.strictEqual(await toggler.isVisible(), true, 'Navbar toggler must be visible on mobile viewport');
      assert.strictEqual(await toggler.getAttribute('aria-expanded'), 'false', 'Initially closed');

      // Click to open
      await toggler.click();
      // Wait for bootstrap collapse animation to settle
      await page.waitForTimeout(400);

      assert.strictEqual(await toggler.getAttribute('aria-expanded'), 'true', 'aria-expanded must update to true');
      assert.strictEqual(await navCollapse.isVisible(), true, 'Menu must be visible when expanded');

      // Zero Horizontal Overflow on Mobile
      const overflow = await page.evaluate(() => {
        return document.documentElement.scrollWidth > window.innerWidth;
      });
      assert.strictEqual(overflow, false, 'Mobile viewport (375px) must not have horizontal page overflow');

      // Click to close
      await toggler.click();
      await page.waitForTimeout(400);
      assert.strictEqual(await toggler.getAttribute('aria-expanded'), 'false', 'aria-expanded must return to false');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors on mobile nav toggle:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // B3: Modal Dialog (Tested on dedicated verification page)
  test('B3: Modal Dialog opens with role="dialog", dismisses via Escape, and restores focus', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });
      await page.waitForFunction(() => typeof window.bootstrap !== 'undefined', { timeout: 10000 }).catch(() => {});

      const triggerBtn = page.locator('#triggerModalDemo');
      await triggerBtn.click();

      // Modal appears (Bootstrap .modal with role="dialog")
      const modal = page.locator('.modal');
      await modal.waitFor({ state: 'visible', timeout: 3000 });
      // Allow Bootstrap 5 transition and focus transfer to settle
      await page.waitForTimeout(400);

      assert.strictEqual(await modal.getAttribute('role'), 'dialog', 'Modal element must declare role="dialog"');
      assert.strictEqual(await modal.getAttribute('aria-modal'), 'true', 'Modal element must declare aria-modal="true"');

      // Dismiss via Escape key
      await page.keyboard.press('Escape');
      // Wait for fade transition out
      await page.waitForTimeout(400);
      await modal.waitFor({ state: 'detached', timeout: 3000 });

      // Verify focus restored to trigger button
      const isFocusRestored = await page.evaluate(() => {
        return document.activeElement === document.getElementById('triggerModalDemo');
      });
      assert.strictEqual(isFocusRestored, true, 'Focus must be restored to trigger button after Escape dismissal');

      const resultText = await page.locator('#modalResultText').textContent();
      assert.ok(resultText.includes('đóng/hủy'), `Expected cancel text in result, got "${resultText}"`);

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors during modal flow:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // B4: 3-State UI Lifecycle (Tested on dedicated verification page)
  test('B4: Three-State UI engine cycles through Loading -> Empty -> Error -> Ready', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#demoStateContainer').waitFor({ state: 'visible' });

      const container = page.locator('#demoStateContainer');

      // 1. Loading
      await page.locator('#btnStateLoading').click();
      assert.strictEqual(await container.getAttribute('aria-busy'), 'true', 'Container must have aria-busy="true" in loading state');
      assert.ok(await container.locator('.state-spinner').isVisible(), 'Spinner must be visible');

      // 2. Empty
      await page.locator('#btnStateEmpty').click();
      assert.strictEqual(await container.getAttribute('aria-busy'), null, 'aria-busy must be cleared in empty state');
      assert.strictEqual(await container.locator('.state-empty-glyph').textContent(), '空', 'Empty glyph must display 空');

      // 3. Error
      await page.locator('#btnStateError').click();
      assert.strictEqual(await container.getAttribute('aria-busy'), null, 'aria-busy must be cleared in error state');
      assert.ok(await container.locator('.btn-chinese-primary').isVisible(), 'Retry button must be visible');

      // 4. Ready
      await page.locator('#btnStateReady').click();
      assert.ok(await container.locator('.badge-status-approved').isVisible(), 'Ready badge must be restored');

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors in 3-state UI lifecycle:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

  // B5: Toast Notification System (Tested on dedicated verification page)
  test('B5: Toast notification renders with role="status" and aria-live="polite", and dismisses cleanly', async () => {
    const { page, context, getRuntimeErrors } = await createMonitoredPage(browser, {
      viewportWidth: 1280,
      viewportHeight: 720
    });

    try {
      await page.goto(`${baseUrl}/ui-verification.html`, { waitUntil: 'domcontentloaded' });
      await page.locator('#toastContainer').waitFor({ state: 'attached' });

      // Toast container must possess accessible live region attributes
      const container = page.locator('#toastContainer');
      assert.strictEqual(await container.getAttribute('role'), 'status', 'Toast container must declare role="status"');
      assert.strictEqual(await container.getAttribute('aria-live'), 'polite', 'Toast container must declare aria-live="polite"');

      // Trigger toast
      await page.locator('#triggerToastSuccess').click();

      const toast = page.locator('.toast-custom');
      await toast.waitFor({ state: 'visible', timeout: 3000 });

      assert.ok(await toast.locator('.toast-title').textContent(), 'Thao tác thành công');

      // Click close button on toast
      const closeBtn = toast.locator('.toast-close-btn');
      await closeBtn.click();

      await toast.waitFor({ state: 'detached', timeout: 3000 });

      const errors = getRuntimeErrors();
      assert.strictEqual(errors.length, 0, `Runtime errors in toast notification flow:\n${errors.join('\n')}`);
    } finally {
      await context.close();
    }
  });

});
