/**
 * =============================================================================
 * CENTRAL FRONTEND VERIFICATION RUNNER (TASK 9A.1.1)
 * Features:
 * - Tiered execution: fast, static, unit, browser, a11y, gate, full
 * - Dynamic test & suite count aggregation (never hardcoded)
 * - Automatic server lifecycle management (reuses existing or boots ephemeral)
 * - Performance budget tracking with non-fatal warnings
 * - Clean exit code: 0 on PASS, 1 on FAIL
 * =============================================================================
 */

import { spawn } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import fs from 'node:fs';
import { ensureServer } from './utils/static-server.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const ROOT_DIR = path.resolve(__dirname, '../..');

// Performance Budgets (soft guidelines, not hard failures)
const PERFORMANCE_BUDGETS = {
  fast: 2500,     // 2.5s budget for static + unit
  static: 1500,   // 1.5s
  unit: 1500,     // 1.5s
  browser: 12000, // 12s
  a11y: 8000,     // 8s
  gate: 22000,    // 22s for complete gate
  full: 25000     // 25s for full regression
};

/**
 * Parses command line arguments for --tier.
 */
function parseTier() {
  const arg = process.argv.find(a => a.startsWith('--tier='));
  if (arg) {
    return arg.split('=')[1].trim().toLowerCase();
  }
  return 'full';
}

/**
 * Discovers test files belonging to the specified tier.
 * @param {string} tier 
 * @returns {string[]}
 */
function resolveTestFiles(tier) {
  const staticFiles = [
    'tests/frontend/static/html-semantics.test.mjs',
    'tests/frontend/static/dom-sinks.test.mjs',
    'tests/frontend/static/css-tokens.test.mjs',
    'tests/frontend/static/sri-integrity.test.mjs',
    'tests/frontend/static/csp-resource-graph.test.mjs'
  ];

  const unitFiles = [
    'tests/frontend/unit/security/security.test.mjs',
    'tests/frontend/unit/ui/ui-primitives.test.mjs',
    'tests/frontend/unit/auth/auth-state.test.mjs',
    'tests/frontend/unit/api/api-client.test.mjs',
    'tests/frontend/unit/auth/auth-forms.test.mjs',
    'tests/frontend/unit/radicals/radicals-catalog.test.mjs',
    'tests/frontend/unit/vocabulary/vocabulary-catalog.test.mjs',
    'tests/frontend/unit/lesson/lessons-catalog.test.mjs',
    'tests/frontend/unit/notes/notes-modal.test.mjs',
    'tests/frontend/unit/srs/srs-session.test.mjs',
    'tests/frontend/unit/srs/srs-dashboard.test.mjs',
    'tests/frontend/unit/config/config.test.mjs',
    'tests/frontend/unit/lesson/creator-lessons.test.mjs',
    'tests/frontend/unit/lesson/creator-import.test.mjs',
    'tests/frontend/unit/lesson/creator-submission.test.mjs',
    'tests/frontend/unit/moderator/moderator-queue.test.mjs',
    'tests/frontend/unit/moderator/moderator-review.test.mjs'
  ];

  const browserFiles = [
    'tests/frontend/e2e/foundation.browser.mjs',
    'tests/frontend/e2e/api-auth.browser.mjs',
    'tests/frontend/e2e/auth-flow.browser.mjs',
    'tests/frontend/e2e/radicals.browser.mjs',
    'tests/frontend/e2e/vocabulary.browser.mjs',
    'tests/frontend/e2e/lessons.browser.mjs',
    'tests/frontend/e2e/personal-notes.browser.mjs',
    'tests/frontend/e2e/srs-review.browser.mjs',
    'tests/frontend/e2e/srs-dashboard.browser.mjs',
    'tests/frontend/e2e/verification-center.browser.mjs',
    'tests/frontend/e2e/creator-lessons.browser.mjs',
    'tests/frontend/e2e/creator-import.browser.mjs',
    'tests/frontend/e2e/creator-submission.browser.mjs',
    'tests/frontend/e2e/moderator-queue.browser.mjs',
    'tests/frontend/e2e/moderator-review.browser.mjs',
    'tests/frontend/e2e/dom-xss-adversarial.browser.mjs',
    'tests/frontend/e2e/csp-enforcement.browser.mjs',
    'tests/frontend/e2e/responsive-layout.browser.mjs'
  ];

  const a11yFiles = [
    'tests/frontend/accessibility/a11y.browser.mjs'
  ];

  switch (tier) {
    case 'static':
      return staticFiles;
    case 'unit':
      return unitFiles;
    case 'fast':
      return [...staticFiles, ...unitFiles];
    case 'browser':
      return browserFiles;
    case 'a11y':
      return a11yFiles;
    case 'gate':
    case 'full':
    default:
      return [...staticFiles, ...unitFiles, ...browserFiles, ...a11yFiles];
  }
}

/**
 * Runs node:test with spec reporter and aggregates results dynamically.
 * @param {string[]} files 
 * @returns {Promise<{ exitCode: number, output: string, summary: Object }>}
 */
function runTests(files) {
  return new Promise((resolve) => {
    const args = ['--test', '--test-concurrency=1', ...files];
    const child = spawn(process.execPath, args, {
      cwd: ROOT_DIR,
      env: { ...process.env, FORCE_COLOR: '1' },
      stdio: ['inherit', 'pipe', 'pipe']
    });

    let stdout = '';
    let stderr = '';

    child.stdout.on('data', (d) => {
      const text = d.toString();
      stdout += text;
      process.stdout.write(text);
    });

    child.stderr.on('data', (d) => {
      const text = d.toString();
      stderr += text;
      process.stderr.write(text);
    });

    child.on('close', (exitCode) => {
      // Parse summary line dynamically from node:test output
      // e.g. "ℹ tests 37\nℹ suites 9\nℹ pass 37\nℹ fail 0\nℹ duration_ms 167.2"
      const testsMatch = stdout.match(/ℹ tests (\d+)/);
      const suitesMatch = stdout.match(/ℹ suites (\d+)/);
      const passMatch = stdout.match(/ℹ pass (\d+)/);
      const failMatch = stdout.match(/ℹ fail (\d+)/);
      const durationMatch = stdout.match(/ℹ duration_ms ([\d.]+)/);

      const summary = {
        totalTests: testsMatch ? parseInt(testsMatch[1], 10) : 0,
        totalSuites: suitesMatch ? parseInt(suitesMatch[1], 10) : 0,
        passedTests: passMatch ? parseInt(passMatch[1], 10) : 0,
        failedTests: failMatch ? parseInt(failMatch[1], 10) : 0,
        durationMs: durationMatch ? parseFloat(durationMatch[1]) : 0
      };

      resolve({ exitCode: exitCode ?? 1, output: stdout + stderr, summary });
    });
  });
}

/**
 * Main execution routine.
 */
async function main() {
  const tier = parseTier();
  const testFiles = resolveTestFiles(tier);

  console.log('\n=============================================================');
  console.log(` FRONTEND VERIFICATION RUNNER — TIER: [${tier.toUpperCase()}]`);
  console.log(` Discovered ${testFiles.length} test suite file(s) for execution`);
  console.log('=============================================================\n');

  const needsServer = ['browser', 'a11y', 'gate', 'full'].includes(tier);
  let serverHandle = null;

  if (needsServer) {
    serverHandle = await ensureServer({ port: 3000 });
    console.log(`[Server] Target URL active at: ${serverHandle.baseUrl} (${serverHandle.isExternal ? 'reusing external' : 'ephemeral started'})\n`);
  }

  const startTime = performance.now();
  let result;

  try {
    result = await runTests(testFiles);
  } finally {
    if (serverHandle) {
      await serverHandle.close();
      if (!serverHandle.isExternal) {
        console.log('\n[Server] Ephemeral server closed cleanly.');
      }
    }
  }

  const totalTimeMs = Math.round(performance.now() - startTime);
  const budget = PERFORMANCE_BUDGETS[tier] || 20000;
  const isWithinBudget = totalTimeMs <= budget;

  console.log('\n-------------------------------------------------------------');
  console.log(' VERIFICATION SUMMARY (DYNAMIC AGGREGATION)');
  console.log('-------------------------------------------------------------');
  console.log(` Tier:       ${tier.toUpperCase()}`);
  console.log(` Suites:     ${result.summary.totalSuites}`);
  console.log(` Tests:      ${result.summary.totalTests}`);
  console.log(` Passed:     ${result.summary.passedTests}`);
  console.log(` Failed:     ${result.summary.failedTests}`);
  console.log(` Wall Time:  ${(totalTimeMs / 1000).toFixed(2)}s`);
  console.log(` Budget:     ${(budget / 1000).toFixed(2)}s max target`);

  if (!isWithinBudget) {
    console.warn(` [BUDGET WARNING] Execution time (${(totalTimeMs / 1000).toFixed(2)}s) exceeded target budget (${(budget / 1000).toFixed(2)}s). Review test isolation and transitions.`);
  } else {
    console.log(' Budget:     WITHIN PERFORMANCE BUDGET ✓');
  }

  const statusVerdict = result.exitCode === 0 ? 'PASS ✓' : 'FAIL ✗';
  console.log(` Status:     ${statusVerdict}`);
  console.log('=============================================================\n');

  process.exit(result.exitCode);
}

main().catch((err) => {
  console.error('Fatal runner error:', err);
  process.exit(1);
});
