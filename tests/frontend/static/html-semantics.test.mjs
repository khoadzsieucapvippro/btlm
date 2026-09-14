/**
 * =============================================================================
 * STATIC VERIFICATION: HTML5 SEMANTICS & LANDMARKS
 * Enforces Project Invariants across all application HTML pages:
 * - Single primary <h1> heading per page
 * - Semantic landmarks: header, nav, main, footer
 * - Accessible skip link targeting #mainContent
 * - Strictly unique IDs per document
 * - Valid HTML5 DOCTYPE and lang="vi"
 * - Zero obsolete presentational markup
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

describe('Static HTML5 Semantics & Project Invariants', () => {
  const htmlFiles = fs.readdirSync(FRONTEND_DIR).filter(f => f.endsWith('.html'));

  assert.ok(htmlFiles.length >= 4, `Must find at least 4 HTML pages (index, login, register, profile). Found: ${htmlFiles.join(', ')}`);

  for (const fileName of htmlFiles) {
    const htmlPath = path.join(FRONTEND_DIR, fileName);
    const html = fs.readFileSync(htmlPath, 'utf-8');

    describe(`Page: ${fileName}`, () => {
      test('declares valid HTML5 DOCTYPE and language attribute', () => {
        assert.match(html, /<!DOCTYPE\s+html>/i, `${fileName} must begin with standard HTML5 DOCTYPE`);
        assert.match(html, /<html[^>]*lang=["']vi["']/i, `${fileName} must specify Vietnamese language lang="vi"`);
      });

      test('specifies responsive meta viewport and UTF-8 charset', () => {
        assert.match(html, /<meta[^>]*charset=["']UTF-8["']/i, `${fileName} must declare UTF-8 charset`);
        assert.match(html, /<meta[^>]*name=["']viewport["'][^>]*content=["'][^"']*width=device-width/i, `${fileName} must declare mobile-responsive viewport`);
      });

      test('enforces Single Primary H1 Rule (Project Architectural Invariant)', () => {
        const h1Matches = html.match(/<h1[\s>]/gi) || [];
        assert.strictEqual(h1Matches.length, 1, `Expected exactly 1 <h1> heading in ${fileName} (Project Invariant), found ${h1Matches.length}`);
      });

      test('contains essential semantic landmarks: header, nav, main, footer', () => {
        assert.match(html, /<header[\s>]/i, `${fileName} must include a semantic <header> landmark`);
        assert.match(html, /<nav[\s>]/i, `${fileName} must include a semantic <nav> landmark`);
        assert.match(html, /<main[\s>]/i, `${fileName} must include a semantic <main> landmark`);
        assert.match(html, /<footer[\s>]/i, `${fileName} must include a semantic <footer> landmark`);
      });

      test('includes accessible skip link targeting main content (WCAG 2.2 SC 2.4.1)', () => {
        const skipLinkMatch = html.match(/<a[^>]*href=["']#mainContent["'][^>]*class=["'][^"']*skip-link[^"']*["'][^>]*>(.*?)<\/a>/is);
        assert.ok(skipLinkMatch, `${fileName} must include skip link anchor with class "skip-link" pointing to #mainContent`);
        assert.match(html, /<main[^>]*id=["']mainContent["']/i, `${fileName} main landmark must have matching id="mainContent"`);
      });

      test('ensures all interactive and structural IDs are strictly unique', () => {
        const idRegex = /\sid=["']([^"']+)["']/gi;
        const ids = [];
        let match;
        while ((match = idRegex.exec(html)) !== null) {
          ids.push(match[1]);
        }
        const duplicates = ids.filter((item, index) => ids.indexOf(item) !== index);
        assert.strictEqual(duplicates.length, 0, `Duplicate IDs detected in ${fileName}: ${duplicates.join(', ')}`);
      });

      test('contains zero obsolete presentational elements', () => {
        const obsoleteTags = ['font', 'center', 'marquee', 'blink', 'big', 'strike'];
        for (const tag of obsoleteTags) {
          const regex = new RegExp(`<${tag}[\\s>]`, 'i');
          assert.ok(!regex.test(html), `Obsolete presentational tag <${tag}> must not be used in ${fileName}`);
        }
      });
    });
  }
});
