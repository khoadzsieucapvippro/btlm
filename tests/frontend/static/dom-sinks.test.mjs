/**
 * =============================================================================
 * STATIC VERIFICATION: DOM SINKS & SAFE RENDERING POLICY
 * Architecture: OWASP DOM-based XSS Prevention & Defense-in-Depth
 * Targeted pattern scanner for 100% frontend JavaScript and HTML markup.
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const JS_ROOT_DIR = path.resolve(__dirname, '../../../frontend/js');
const HTML_ROOT_DIR = path.resolve(__dirname, '../../../frontend');

/**
 * Recursively retrieves all files with specified extension in a directory.
 * @param {string} dir 
 * @param {string} ext 
 * @returns {string[]}
 */
function getFilesByExt(dir, ext) {
  const files = [];
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...getFilesByExt(fullPath, ext));
    } else if (entry.isFile() && entry.name.endsWith(ext)) {
      files.push(fullPath);
    }
  }
  return files;
}

describe('Static DOM Security & Sinks Policy', () => {
  const jsFiles = getFilesByExt(JS_ROOT_DIR, '.js');
  const htmlFiles = fs.readdirSync(HTML_ROOT_DIR, { withFileTypes: true })
    .filter(e => e.isFile() && e.name.endsWith('.html'))
    .map(e => path.join(HTML_ROOT_DIR, e.name));

  test('verifies JavaScript and HTML source files are present for analysis', () => {
    assert.ok(jsFiles.length > 0, 'Must have JavaScript source files in frontend/js/');
    assert.ok(htmlFiles.length > 0, 'Must have HTML source files in frontend/');
  });

  test('prohibits inline event handlers in HTML markup (e.g. onclick, onsubmit, onerror, onload)', () => {
    // Pattern: on* attribute assignment in HTML tags
    const inlineHtmlEventRegex = /\son[a-z]+\s*=/gi;

    for (const file of htmlFiles) {
      const relPath = path.basename(file);
      const rawContent = fs.readFileSync(file, 'utf-8');
      // Strip HTML comments (<!-- ... -->)
      const cleanContent = rawContent.replace(/<!--[\s\S]*?-->/g, '');

      const matches = cleanContent.match(inlineHtmlEventRegex) || [];
      assert.strictEqual(
        matches.length,
        0,
        `Inline HTML event handler attribute detected in ${relPath}: ${matches.join(', ')}. Use addEventListener() in module scripts instead.`
      );
    }
  });

  test('prohibits dangerous execution sinks in JS (eval, document.write, string timers, new Function, outerHTML, insertAdjacentHTML, dynamic script)', () => {
    const dangerousSinkPatterns = [
      { name: 'eval()', regex: /\beval\s*\(/g },
      { name: 'document.write()', regex: /document\.write(?:ln)?\s*\(/g },
      { name: 'new Function()', regex: /new\s+Function\s*\(/g },
      { name: 'setTimeout with string', regex: /setTimeout\s*\(\s*['"`]/g },
      { name: 'setInterval with string', regex: /setInterval\s*\(\s*['"`]/g },
      { name: 'outerHTML assignment', regex: /\.outerHTML\s*(\+?=)/g },
      { name: 'insertAdjacentHTML', regex: /\.insertAdjacentHTML\s*\(/g },
      { name: 'document.createElement("script")', regex: /document\.createElement\s*\(\s*['"`]script['"`]\s*\)/gi }
    ];

    for (const file of jsFiles) {
      const relPath = path.relative(JS_ROOT_DIR, file).replace(/\\/g, '/');
      const rawContent = fs.readFileSync(file, 'utf-8');
      // Strip comments to prevent false positives in documentation
      const cleanContent = rawContent
        .replace(/\/\*[\s\S]*?\*\//g, '')
        .replace(/\/\/.*$/gm, '');

      for (const sink of dangerousSinkPatterns) {
        const match = cleanContent.match(sink.regex);
        assert.strictEqual(
          match,
          null,
          `Dangerous sink "${sink.name}" detected in ${relPath}`
        );
      }
    }
  });

  test('prohibits inline event attribute assignments in JS (setAttribute("onclick", ...))', () => {
    const inlineHandlerRegex = /setAttribute\s*\(\s*['"]on[a-z]+/gi;

    for (const file of jsFiles) {
      const relPath = path.relative(JS_ROOT_DIR, file).replace(/\\/g, '/');
      const rawContent = fs.readFileSync(file, 'utf-8');
      const cleanContent = rawContent
        .replace(/\/\*[\s\S]*?\*\//g, '')
        .replace(/\/\/.*$/gm, '');

      const matches = cleanContent.match(inlineHandlerRegex) || [];
      assert.strictEqual(
        matches.length,
        0,
        `Inline event handler assignment detected in ${relPath}: ${matches.join(', ')}`
      );
    }
  });

  test('enforces safe innerHTML policy with explicit allowlist (Project Policy)', () => {
    // Policy:
    // 1. innerHTML = '' or innerHTML = "" is explicitly permitted for clearing DOM.
    // 2. div.innerHTML as getter in escapeHtml() is permitted.
    // 3. Any assignment with variable interpolation or string concatenation is FORBIDDEN.
    // 4. Any += assignment is FORBIDDEN.
    const innerHtmlAssignRegex = /\.innerHTML\s*(\+?=)\s*([^;]+);/g;

    for (const file of jsFiles) {
      const relPath = path.relative(JS_ROOT_DIR, file).replace(/\\/g, '/');
      const rawContent = fs.readFileSync(file, 'utf-8');
      // Strip block comments (/* ... */) and line comments (// ...)
      const cleanContent = rawContent
        .replace(/\/\*[\s\S]*?\*\//g, '')
        .replace(/\/\/.*$/gm, '');

      let match;
      while ((match = innerHtmlAssignRegex.exec(cleanContent)) !== null) {
        const operator = match[1].trim();
        const rhs = match[2].trim();

        // += is strictly forbidden
        assert.notStrictEqual(
          operator,
          '+=',
          `Unsafe innerHTML concatenation (+=) detected in ${relPath}`
        );

        // Allow only empty string resets
        const isSafeReset = rhs === "''" || rhs === '""';
        assert.ok(
          isSafeReset,
          `Unsafe innerHTML assignment detected in ${relPath} -> ".innerHTML ${operator} ${rhs}". Dynamic rendering must use textContent, createElement(), or replaceChildren().`
        );
      }
    }
  });
});

