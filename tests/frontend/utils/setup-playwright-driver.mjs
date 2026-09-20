/**
 * =============================================================================
 * PLAYWRIGHT ENVIRONMENT BOOTSTRAP & RECOVERY UTILITY
 * Usage: node tests/frontend/utils/setup-playwright-driver.mjs
 * Purpose: Diagnoses local browser installations and repairs missing drivers.
 * Note: This is an environment recovery utility, NOT a runtime dependency.
 * =============================================================================
 */

import fs from 'node:fs';
import path from 'node:path';
import { execSync } from 'node:child_process';

console.log('=== Playwright Environment Diagnostic & Bootstrap ===');

const localAppData = process.env.LOCALAPPDATA || path.join(process.env.USERPROFILE || '', 'AppData', 'Local');
const playwrightCache = path.join(localAppData, 'ms-playwright');
const playwrightGoCache = path.join(localAppData, 'ms-playwright-go', '1.57.0');

console.log('Checking Playwright Browser Cache at:', playwrightCache);
if (fs.existsSync(playwrightCache)) {
  const contents = fs.readdirSync(playwrightCache);
  console.log('Found browser installations:', contents.join(', '));
} else {
  console.warn('Playwright browser cache not found. Run: npx playwright install chromium');
}

console.log('Checking Playwright Go Cache at:', playwrightGoCache);
if (fs.existsSync(playwrightGoCache)) {
  console.log('Playwright Go 1.57.0 wrapper is present.');
} else {
  console.warn('Playwright Go 1.57.0 wrapper not detected. It can be populated from npm playwright-core if needed.');
}

console.log('Environment diagnostic complete.');
