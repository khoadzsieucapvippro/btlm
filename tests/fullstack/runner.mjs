/**
 * =============================================================================
 * CENTRAL FULL-STACK VERIFICATION RUNNER (TASK FE-REMEDIATION-04)
 * File: tests/fullstack/runner.mjs
 * 
 * Flow:
 * Phase 0: Environment discovery & preflight
 * Phase 1: MySQL readiness (bounded probe on 3306)
 * Phase 2: Spring Boot readiness (bounded probe on 8080/actuator/health)
 * Phase 3: Frontend static server readiness (bounded probe on 3000)
 * Phase 4: Backend REST API scenarios (FS-001 .. FS-010)
 * Phase 5: Real Browser E2E user journeys (FS-011 .. FS-019)
 * Phase 6: Reporting, traceability & cleanup
 * =============================================================================
 */

import { ensureFullStackServices } from './utils/service-manager.mjs';
import { runApiScenarios } from './scenarios/api-scenarios.mjs';
import { runBrowserScenarios } from './scenarios/browser-scenarios.mjs';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';
const FRONTEND_URL = process.env.FRONTEND_URL || 'http://localhost:3000';

async function main() {
  console.log('\n=============================================================');
  console.log(' OFFICIAL FULL-STACK FE ↔ BE VERIFICATION RUNNER');
  console.log(' Target Architecture:');
  console.log('   Real Browser (Playwright)');
  console.log('      ↓');
  console.log(`   Real Frontend (${FRONTEND_URL})`);
  console.log('      ↓');
  console.log('   Real HTTP / CORS');
  console.log('      ↓');
  console.log(`   Real Spring Boot (${BACKEND_URL})`);
  console.log('      ↓');
  console.log('   Real MySQL (:3306)');
  console.log('=============================================================\n');

  const startTime = performance.now();
  let services = null;
  const allScenarios = [];

  try {
    // -------------------------------------------------------------------------
    // Phase 0..3: Service Discovery & Readiness
    // -------------------------------------------------------------------------
    services = await ensureFullStackServices();

    if (!services.mysql.ready) {
      console.error('\n[FATAL] Full-Stack verification blocked: MySQL is not running on 3306.');
      console.error('Resolution: Please start MySQL Community Server 8.4 on port 3306.');
      process.exit(1);
    }

    if (!services.backend.ready) {
      console.error('\n[FATAL] Full-Stack verification blocked: Spring Boot is not reachable at ' + BACKEND_URL);
      console.error('Details:', services.backend.error);
      process.exit(1);
    }

    // -------------------------------------------------------------------------
    // Phase 4: Direct Backend REST API Scenarios (FS-001 .. FS-010)
    // -------------------------------------------------------------------------
    const apiOutcome = await runApiScenarios(BACKEND_URL);
    allScenarios.push(...apiOutcome.results);

    // -------------------------------------------------------------------------
    // Phase 5: Real Browser User Journeys (FS-011 .. FS-019)
    // -------------------------------------------------------------------------
    const browserOutcome = await runBrowserScenarios(services.frontend.baseUrl, BACKEND_URL);
    allScenarios.push(...browserOutcome.results);

  } catch (err) {
    console.error('\n[FATAL] Unexpected error during full-stack verification execution:', err);
    process.exit(1);
  } finally {
    // Teardown ephemeral resources if any
    if (services && services.frontend && !services.frontend.isExternal) {
      await services.frontend.close();
      console.log('\n[Teardown] Ephemeral static server closed cleanly.');
    }
  }

  const totalWallTime = ((performance.now() - startTime) / 1000).toFixed(2);
  const passedCount = allScenarios.filter(s => s.status === 'PASS').length;
  const failedCount = allScenarios.filter(s => s.status === 'FAIL').length;
  const isOverallPass = failedCount === 0 && allScenarios.length > 0;

  // ---------------------------------------------------------------------------
  // Phase 6: Structured Traceability Matrix & Report
  // ---------------------------------------------------------------------------
  console.log('\n=============================================================');
  console.log(' FULL-STACK VERIFICATION TRACEABILITY MATRIX (FS-xxx)');
  console.log('=============================================================');
  console.log(
    'ID'.padEnd(8) +
    'Status'.padEnd(10) +
    'Duration'.padEnd(12) +
    'Scenario Name & Evidence'
  );
  console.log(''.padEnd(70, '-'));

  for (const s of allScenarios) {
    const statusFormatted = s.status === 'PASS' ? 'PASS ✓' : 'FAIL ✗';
    const durFormatted = `${s.durationMs}ms`.padEnd(12);
    const detail = s.detail ? ` → ${s.detail}` : (s.error ? ` [ERROR: ${s.error}]` : '');
    console.log(
      s.id.padEnd(8) +
      statusFormatted.padEnd(10) +
      durFormatted +
      s.name +
      detail
    );
  }

  console.log(''.padEnd(70, '-'));
  console.log(` Total Scenarios:  ${allScenarios.length}`);
  console.log(` Passed:           ${passedCount}`);
  console.log(` Failed:           ${failedCount}`);
  console.log(` Wall Time:        ${totalWallTime}s`);
  console.log(` Status:           ${isOverallPass ? 'FULL-STACK VERIFIED (PASS ✓)' : 'FAILED (FAIL ✗)'}`);
  console.log('=============================================================\n');

  process.exit(isOverallPass ? 0 : 1);
}

main().catch((err) => {
  console.error('Fatal runner error:', err);
  process.exit(1);
});
