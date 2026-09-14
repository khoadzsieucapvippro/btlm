/**
 * =============================================================================
 * STATIC VERIFICATION: CSS DESIGN TOKENS & ANTI-TEMPLATE GOVERNANCE
 * Enforces: Required tokens, pedagogical color palette, motion safety,
 * and anti-generic AI SaaS design governance (strictly rule-based).
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const CSS_PATH = path.resolve(__dirname, '../../../frontend/css/style.css');

describe('Static CSS Design Tokens & Anti-Template Governance', () => {
  const css = fs.readFileSync(CSS_PATH, 'utf-8');

  test('defines core architectural design tokens in :root', () => {
    const requiredTokens = [
      '--color-accent-primary',
      '--color-accent-hover',
      '--color-bg-canvas',
      '--color-bg-surface',
      '--color-text-primary',
      '--color-text-secondary',
      '--color-status-success',
      '--color-status-warning',
      '--color-status-danger',
      '--font-hanzi',
      '--space-1',
      '--space-2',
      '--space-3',
      '--space-4',
      '--radius-sm',
      '--radius-md',
      '--radius-lg'
    ];

    for (const token of requiredTokens) {
      assert.ok(
        css.includes(token),
        `Design token "${token}" must be defined in style.css`
      );
    }
  });

  test('configures CJK typography font stack with Noto Sans SC', () => {
    assert.match(
      css,
      /--font-hanzi:[^;]*Noto Sans SC/i,
      '--font-hanzi must include Noto Sans SC for authentic Chinese character rendering'
    );
  });

  test('enforces Anti-Template Rule 1: prohibits generic AI SaaS purple/indigo gradients', () => {
    // Prohibits cliche generic AI SaaS purple gradients (e.g. #6366f1 / #8b5cf6 / indigo-violet)
    const genericPurpleGradients = [
      /#6366f1/i,
      /#8b5cf6/i,
      /rgb\s*\(\s*99\s*,\s*102\s*,\s*241\s*\)/i,
      /rgb\s*\(\s*139\s*,\s*92\s*,\s*246\s*\)/i
    ];

    for (const pattern of genericPurpleGradients) {
      assert.ok(
        !pattern.test(css),
        `Generic AI SaaS purple/violet color signature (${pattern}) detected in style.css. Use pedagogical Chinese palette.`
      );
    }
  });

  test('enforces Anti-Template Rule 2: authentic Scholarly Cinnabar primary accent', () => {
    // Primary accent must resolve to authentic Cinnabar (#C83C23)
    assert.match(
      css,
      /--color-primitive-cinnabar-600:\s*#C83C23/i,
      '--color-primitive-cinnabar-600 must be authentic Scholarly Cinnabar (#C83C23)'
    );
    assert.match(
      css,
      /--color-accent-primary:\s*var\(--color-primitive-cinnabar-600\)/i,
      '--color-accent-primary must resolve to Scholarly Cinnabar'
    );
  });

  test('enforces Anti-Template Rule 3: container border-radius discipline', () => {
    // Reject excessive rounded card pill shapes (border-radius: 9999px on cards or > 24px)
    assert.ok(
      !/\.card-chinese[^}]*border-radius:\s*(?:9999px|50px|100px)/i.test(css),
      'Card containers must maintain clean, disciplined border-radius without pill shapes'
    );
  });

  test('includes prefers-reduced-motion media query for vestibular accessibility', () => {
    assert.match(
      css,
      /@media\s*\(\s*prefers-reduced-motion:\s*reduce\s*\)/i,
      'Must provide prefers-reduced-motion media query to disable transitions/animations for sensitive users'
    );
  });
});
