/**
 * =============================================================================
 * STATIC CSP & RESOURCE GRAPH INVARIANTS AUDIT (TASK 9G.3)
 * File: tests/frontend/static/csp-resource-graph.test.mjs
 * 
 * Verifies repository-wide security invariants:
 * - 0 unintended inline scripts across all production HTML pages.
 * - 0 inline event handler attributes (onclick, onload, onerror, onsubmit, etc.).
 * - 0 unapproved external script origins (only https://cdn.jsdelivr.net allowed).
 * - 0 external font stylesheet or preconnect links (Google Fonts completely removed).
 * - All external scripts and stylesheets have valid Subresource Integrity (SRI) hashes.
 * - Authoritative CSP definitions (DEV and PROD) strictly forbid 'unsafe-inline'
 *   and 'unsafe-eval' in script-src, enforce object-src 'none', frame-ancestors 'none',
 *   frame-src 'none', and restrict font-src to 'self'.
 * =============================================================================
 */

import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  DEV_CSP_DIRECTIVES,
  PROD_CSP_DIRECTIVES,
  DEV_CSP_POLICY,
  PROD_CSP_POLICY
} from '../utils/static-server.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FRONTEND_DIR = path.resolve(__dirname, '../../../frontend');

// Production pages to inspect
const HTML_FILES = fs.readdirSync(FRONTEND_DIR)
  .filter(f => f.endsWith('.html'))
  .map(f => path.join(FRONTEND_DIR, f));

describe('Static: CSP & Resource Graph Invariants Audit (Task 9G.3)', () => {

  describe('1. HTML Inline Script Remediation', () => {
    it('ensures zero inline <script> tags exist across all HTML documents', () => {
      const violations = [];

      for (const file of HTML_FILES) {
        const content = fs.readFileSync(file, 'utf8');
        const relPath = path.relative(FRONTEND_DIR, file);

        // Match <script ...> tags that do NOT have a src attribute
        // Non-capturing or capturing group for script tag openings
        const scriptRegex = /<script\b([^>]*)>([\s\S]*?)<\/script>/gi;
        let match;
        while ((match = scriptRegex.exec(content)) !== null) {
          const attrs = match[1];
          const inner = match[2].trim();

          // Check if src attribute is present
          const hasSrc = /\bsrc\s*=/i.test(attrs);
          if (!hasSrc && inner.length > 0) {
            violations.push({
              file: relPath,
              attrs: attrs.trim(),
              snippet: inner.substring(0, 80).replace(/\s+/g, ' ')
            });
          }
        }
      }

      assert.strictEqual(
        violations.length,
        0,
        `Found ${violations.length} inline script(s):\n${JSON.stringify(violations, null, 2)}`
      );
    });
  });

  describe('2. Inline Event Handler Audit', () => {
    it('ensures zero inline event handler attributes exist across all HTML documents', () => {
      const inlineHandlerRegex = /\s+(on[a-z]{3,15})\s*=\s*["']/gi;
      const violations = [];

      for (const file of HTML_FILES) {
        const content = fs.readFileSync(file, 'utf8');
        const relPath = path.relative(FRONTEND_DIR, file);

        let match;
        while ((match = inlineHandlerRegex.exec(content)) !== null) {
          const handlerName = match[1];
          // Find context around match
          const start = Math.max(0, match.index - 20);
          const end = Math.min(content.length, match.index + 60);
          const snippet = content.substring(start, end).replace(/\s+/g, ' ');

          violations.push({
            file: relPath,
            handler: handlerName,
            snippet
          });
        }
      }

      assert.strictEqual(
        violations.length,
        0,
        `Found ${violations.length} inline event handler(s):\n${JSON.stringify(violations, null, 2)}`
      );
    });
  });

  describe('3. External Trust Origins & Subresource Integrity (SRI)', () => {
    it('strictly restricts external script origins to approved CDN (cdn.jsdelivr.net)', () => {
      const externalScriptRegex = /<script\b[^>]*\bsrc=["'](https?:\/\/[^"']+)["'][^>]*>/gi;
      const violations = [];

      for (const file of HTML_FILES) {
        const content = fs.readFileSync(file, 'utf8');
        const relPath = path.relative(FRONTEND_DIR, file);

        let match;
        while ((match = externalScriptRegex.exec(content)) !== null) {
          const srcUrl = match[1];
          const parsed = new URL(srcUrl);
          if (parsed.hostname !== 'cdn.jsdelivr.net') {
            violations.push({ file: relPath, src: srcUrl, host: parsed.hostname });
          }
        }
      }

      assert.strictEqual(
        violations.length,
        0,
        `Found unapproved external script origin(s):\n${JSON.stringify(violations, null, 2)}`
      );
    });

    it('enforces SRI integrity and crossorigin attributes on all external scripts and stylesheets', () => {
      const externalResourceRegex = /<(?:script|link)\b([^>]*\b(?:src|href)=["']https?:\/\/[^"']+["'][^>]*)>/gi;
      const missingSri = [];

      for (const file of HTML_FILES) {
        const content = fs.readFileSync(file, 'utf8');
        const relPath = path.relative(FRONTEND_DIR, file);

        let match;
        while ((match = externalResourceRegex.exec(content)) !== null) {
          const tagAttrs = match[1];

          // Skip preconnect or dns-prefetch links
          if (/\brel=["'](?:preconnect|dns-prefetch)["']/i.test(tagAttrs)) {
            continue;
          }

          const hasIntegrity = /\bintegrity=["'](sha(?:256|384|512)-[A-Za-z0-9+/=]+)["']/i.test(tagAttrs);
          const hasCrossOrigin = /\bcrossorigin(?:=["']anonymous["'])?/i.test(tagAttrs);

          if (!hasIntegrity || !hasCrossOrigin) {
            missingSri.push({
              file: relPath,
              tag: match[0],
              hasIntegrity,
              hasCrossOrigin
            });
          }
        }
      }

      assert.strictEqual(
        missingSri.length,
        0,
        `Found external resources missing SRI or crossorigin:\n${JSON.stringify(missingSri, null, 2)}`
      );
    });

    it('confirms Google Fonts dependencies are completely absent', () => {
      const googleFontViolations = [];

      for (const file of HTML_FILES) {
        const content = fs.readFileSync(file, 'utf8');
        const relPath = path.relative(FRONTEND_DIR, file);

        if (/fonts\.(?:googleapis|gstatic)\.com/i.test(content)) {
          googleFontViolations.push(relPath);
        }
      }

      assert.strictEqual(
        googleFontViolations.length,
        0,
        `Found Google Fonts references in: ${googleFontViolations.join(', ')}`
      );
    });
  });

  describe('4. CSP Policy Specification Compliance', () => {
    it('forbids unsafe-inline and unsafe-eval in script-src for both DEV and PROD', () => {
      for (const [env, directives] of [['DEV', DEV_CSP_DIRECTIVES], ['PROD', PROD_CSP_DIRECTIVES]]) {
        const scriptSrc = directives['script-src'] || [];
        assert.ok(!scriptSrc.includes("'unsafe-inline'"), `${env} script-src MUST NOT contain 'unsafe-inline'`);
        assert.ok(!scriptSrc.includes("'unsafe-eval'"), `${env} script-src MUST NOT contain 'unsafe-eval'`);
        assert.ok(!scriptSrc.includes('*'), `${env} script-src MUST NOT contain wildcard '*'`);
      }
    });

    it('enforces object-src "none", frame-ancestors "none", and frame-src "none"', () => {
      for (const [env, directives] of [['DEV', DEV_CSP_DIRECTIVES], ['PROD', PROD_CSP_DIRECTIVES]]) {
        assert.deepStrictEqual(directives['object-src'], ["'none'"], `${env} object-src must be 'none'`);
        assert.deepStrictEqual(directives['frame-ancestors'], ["'none'"], `${env} frame-ancestors must be 'none'`);
        assert.deepStrictEqual(directives['frame-src'], ["'none'"], `${env} frame-src must be 'none'`);
        assert.deepStrictEqual(directives['base-uri'], ["'self'"], `${env} base-uri must be 'self'`);
        assert.deepStrictEqual(directives['form-action'], ["'self'"], `${env} form-action must be 'self'`);
      }
    });

    it('restricts font-src strictly to "self"', () => {
      for (const [env, directives] of [['DEV', DEV_CSP_DIRECTIVES], ['PROD', PROD_CSP_DIRECTIVES]]) {
        assert.deepStrictEqual(directives['font-src'], ["'self'"], `${env} font-src must be 'self'`);
      }
    });

    it('enforces environment distinction: PROD connect-src is "self" only, DEV includes localhost:8080', () => {
      assert.deepStrictEqual(PROD_CSP_DIRECTIVES['connect-src'], ["'self'"]);
      assert.ok(DEV_CSP_DIRECTIVES['connect-src'].includes("'self'"));
      assert.ok(DEV_CSP_DIRECTIVES['connect-src'].includes('http://localhost:8080'));
      assert.ok(DEV_CSP_DIRECTIVES['connect-src'].includes('http://127.0.0.1:8080'));
      assert.ok(!PROD_CSP_DIRECTIVES['connect-src'].includes('http://localhost:8080'));
    });

    it('formats valid CSP policy strings without trailing semicolons or empty directives', () => {
      for (const [env, policy] of [['DEV', DEV_CSP_POLICY], ['PROD', PROD_CSP_POLICY]]) {
        assert.ok(typeof policy === 'string' && policy.length > 50, `${env} policy string must be populated`);
        assert.ok(!policy.endsWith(';'), `${env} policy should not end with trailing semicolon`);
        const directives = policy.split('; ');
        assert.ok(directives.length >= 10, `${env} policy should contain at least 10 directives`);
        for (const dir of directives) {
          const parts = dir.split(' ');
          assert.ok(parts.length >= 2, `Directive "${dir}" must have a name and at least one source`);
        }
      }
    });
  });

});
