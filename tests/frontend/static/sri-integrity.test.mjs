/**
 * =============================================================================
 * STATIC VERIFICATION: SUBRESOURCE INTEGRITY (SRI) & CROSSORIGIN VALIDATOR
 * Verifies external CDN resources across ALL application HTML documents
 * strictly possess valid cryptographic SRI hashes and crossorigin="anonymous"
 * attributes, without mechanically forcing SRI on local assets.
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FRONTEND_DIR = path.resolve(__dirname, '../../../frontend');

describe('Static SRI & Cross-Origin Validation', () => {
  const htmlFiles = fs.readdirSync(FRONTEND_DIR).filter(f => f.endsWith('.html'));

  test('validates external static CDN resources have valid SRI and crossorigin="anonymous" across all pages', () => {
    const tagRegex = /<(link|script)\b([^>]+)>/gi;
    const externalCdnDomains = ['cdn.jsdelivr.net', 'cdnjs.cloudflare.com', 'unpkg.com', 'stackpath.bootstrapcdn.com'];

    let totalExternalAssets = 0;

    for (const fileName of htmlFiles) {
      const html = fs.readFileSync(path.join(FRONTEND_DIR, fileName), 'utf-8');
      let match;

      while ((match = tagRegex.exec(html)) !== null) {
        const tagName = match[1].toLowerCase();
        const attributesStr = match[2];

        // Extract href or src
        const srcMatch = attributesStr.match(/(?:href|src)=["']([^"']+)["']/i);
        if (!srcMatch) continue;

        const url = srcMatch[1].trim();

        // Check if this is an external static CDN asset
        const isExternalCdn = externalCdnDomains.some(domain => url.includes(domain));
        if (!isExternalCdn) {
          continue;
        }

        totalExternalAssets++;

        // 1. Must have integrity attribute with sha256/sha384/sha512
        const integrityMatch = attributesStr.match(/integrity=["'](sha(?:256|384|512)-[A-Za-z0-9+/=]+)["']/i);
        assert.ok(
          integrityMatch,
          `In ${fileName}: External CDN asset <${tagName} src/href="${url}"> must specify a valid cryptographic SRI hash (sha256/sha384/sha512).`
        );

        // 2. Must have crossorigin="anonymous"
        const crossOriginMatch = attributesStr.match(/crossorigin=["']anonymous["']/i);
        assert.ok(
          crossOriginMatch,
          `In ${fileName}: External CDN asset <${tagName} src/href="${url}"> with SRI must include crossorigin="anonymous".`
        );
      }
    }

    assert.ok(totalExternalAssets > 0, 'Must have verified external static CDN resources across HTML pages');
  });

  test('does NOT mandate SRI on local, same-origin relative assets (Non-Mechanical Policy)', () => {
    for (const fileName of htmlFiles) {
      const html = fs.readFileSync(path.join(FRONTEND_DIR, fileName), 'utf-8');
      const tagRegex = /<(link|script)\b([^>]+)>/gi;
      let match;

      while ((match = tagRegex.exec(html)) !== null) {
        const attributesStr = match[2];
        const srcMatch = attributesStr.match(/(?:href|src)=["']([^"']+)["']/i);
        if (!srcMatch) continue;

        const url = srcMatch[1].trim();
        const isLocalRelative = url.startsWith('/') || url.startsWith('./') || url.startsWith('css/') || url.startsWith('js/');

        if (isLocalRelative) {
          const hasIntegrity = attributesStr.includes('integrity=');
          assert.strictEqual(
            hasIntegrity,
            false,
            `In ${fileName}: Local relative asset (${url}) should not have brittle build-time SRI hash.`
          );
        }
      }
    }
  });
});
