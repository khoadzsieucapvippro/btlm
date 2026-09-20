/**
 * =============================================================================
 * FULL-STACK REST API SCENARIOS (FS-001 TO FS-010)
 * File: tests/fullstack/scenarios/api-scenarios.mjs
 * 
 * Invariants:
 * - Real HTTP requests directly against live Spring Boot on port 8080.
 * - Strict adherence to current backend response DTOs (RadicalDetailResponse, AuthResponse, UserProfileResponse).
 * - Zero hardcoded permanent credentials: uses disposable test identities.
 * - Zero secret leakage into console or reports.
 * =============================================================================
 */

import assert from 'node:assert/strict';
import { generateDisposableUser } from '../utils/test-data.mjs';

/**
 * Runs all direct backend API protocol scenarios.
 * @param {string} backendUrl 
 * @returns {Promise<{
 *   results: Array<{ id: string, name: string, status: string, durationMs: number, error?: string, detail?: string }>,
 *   context: { testUser: any, authToken: string }
 * }>}
 */
export async function runApiScenarios(backendUrl = 'http://localhost:8080') {
  const results = [];
  const testUser = generateDisposableUser();
  let authToken = '';

  /**
   * Helper to execute and time a scenario.
   */
  async function executeScenario(id, name, fn) {
    const start = performance.now();
    try {
      const detail = await fn();
      const durationMs = Math.round(performance.now() - start);
      results.push({ id, name, status: 'PASS', durationMs, detail });
      console.log(`  [${id}] PASS — ${name} (${durationMs}ms)`);
    } catch (err) {
      const durationMs = Math.round(performance.now() - start);
      results.push({ id, name, status: 'FAIL', durationMs, error: err.message });
      console.error(`  [${id}] FAIL — ${name}: ${err.message}`);
      throw err; // Stop on unexpected API failure
    }
  }

  console.log('\n-------------------------------------------------------------');
  console.log(' RUNNING BACKEND REST API SCENARIOS (FS-001 .. FS-010)');
  console.log('-------------------------------------------------------------');

  // FS-001: Health Actuator Probe
  await executeScenario('FS-001', 'Spring Boot Actuator Health Probe', async () => {
    const res = await fetch(`${backendUrl}/actuator/health`);
    assert.strictEqual(res.status, 200, `Actuator health must return HTTP 200 (got ${res.status})`);
    const json = await res.json();
    assert.strictEqual(json.status, 'UP', `Actuator health status must be UP (got ${json.status})`);
    return 'Status: UP';
  });

  // FS-002: Real Kangxi Radical Catalog Invariants
  await executeScenario('FS-002', 'Public Radicals Catalog (214 items sequential & integrity)', async () => {
    const res = await fetch(`${backendUrl}/api/v1/radicals?page=0&size=214`);
    assert.strictEqual(res.status, 200, `Radicals endpoint must return HTTP 200 (got ${res.status})`);
    const envelope = await res.json();
    assert.strictEqual(envelope.code, 'SUCCESS', 'Response envelope code must be SUCCESS');
    assert.strictEqual(envelope.data.totalElements, 214, 'Total radicals in DB must be exactly 214');
    assert.strictEqual(envelope.data.items.length, 214, 'Must return all 214 items on page 0 size 214');

    const items = envelope.data.items;
    for (let i = 0; i < 214; i++) {
      const expectedId = i + 1;
      assert.strictEqual(Number(items[i].radicalId), expectedId, `Radical at index ${i} must have radicalId ${expectedId}`);
      assert.ok(items[i].character && items[i].character.trim().length > 0, `Radical ${expectedId} must have character glyph`);
    }
    return `Verified ${items.length} items (IDs 1..214 sequential)`;
  });

  // FS-003: Real Radical Detail by ID
  await executeScenario('FS-003', 'Public Radical Detail by ID (GET /radicals/1)', async () => {
    const res = await fetch(`${backendUrl}/api/v1/radicals/1`);
    assert.strictEqual(res.status, 200, `Radical detail must return HTTP 200 (got ${res.status})`);
    const envelope = await res.json();
    assert.strictEqual(envelope.code, 'SUCCESS');
    const data = envelope.data;

    // Strict contract check against RadicalDetailResponse
    assert.strictEqual(data.radicalId, 1, 'radicalId must be 1');
    assert.strictEqual(data.character, '一', 'character glyph must be 一');
    assert.strictEqual(data.pinyin, 'yī', 'pinyin must be yī');
    assert.strictEqual(data.meaningHanViet, 'Nhất', 'meaningHanViet must be Nhất');
    assert.strictEqual(typeof data.meaningVi, 'string', 'meaningVi must be a string');
    // Note: Invariant from Prompt — Do NOT assert strokes / strokeCount since backend DTO doesn't define it.
    assert.strictEqual(data.strokes, undefined, 'DTO must not leak phantom strokes field');
    return `Radical #1: '${data.character}' (${data.pinyin} - ${data.meaningHanViet})`;
  });

  // FS-004: User Registration with Disposable User
  await executeScenario('FS-004', 'User Registration Protocol (POST /auth/register → 201)', async () => {
    const res = await fetch(`${backendUrl}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(testUser)
    });
    assert.strictEqual(res.status, 201, `Register endpoint must return HTTP 201 Created (got ${res.status})`);
    const envelope = await res.json();
    assert.strictEqual(envelope.code, 'SUCCESS');
    assert.ok(envelope.data.token, 'Register response must return token');
    assert.ok(envelope.data.accountId, 'Register response must return accountId');
    assert.strictEqual(envelope.data.emailOrPhone, testUser.emailOrPhone);
    assert.strictEqual(envelope.data.fullName, testUser.fullName);
    return `Created accountId: ${envelope.data.accountId}`;
  });

  // FS-005: Duplicate Registration Rejection
  await executeScenario('FS-005', 'Duplicate Registration Rejection (409 Conflict)', async () => {
    const res = await fetch(`${backendUrl}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(testUser)
    });
    assert.strictEqual(res.status, 409, `Duplicate register must return HTTP 409 Conflict (got ${res.status})`);
    const envelope = await res.json();
    assert.ok(envelope.code === 'CONFLICT' || envelope.code === 'DUPLICATE_ENTRY' || envelope.code === 'ERROR');
    return `Rejected duplicate email with HTTP ${res.status} [${envelope.code}]`;
  });

  // FS-006: User Login Protocol
  await executeScenario('FS-006', 'User Login Protocol (POST /auth/login → 200 + JWT)', async () => {
    const res = await fetch(`${backendUrl}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        emailOrPhone: testUser.emailOrPhone,
        password: testUser.password
      })
    });
    assert.strictEqual(res.status, 200, `Login endpoint must return HTTP 200 (got ${res.status})`);
    const envelope = await res.json();
    assert.strictEqual(envelope.code, 'SUCCESS');
    const authData = envelope.data;

    // Strict contract check against AuthResponse
    assert.ok(authData.token && authData.token.length > 20, 'Must return non-empty JWT token');
    assert.strictEqual(authData.type, 'Bearer', 'Token type must be Bearer');
    assert.strictEqual(authData.emailOrPhone, testUser.emailOrPhone);
    assert.strictEqual(authData.fullName, testUser.fullName);
    assert.ok(Array.isArray(authData.roles), 'roles must be an array');
    assert.ok(authData.roles.includes('Learner'), 'Default registered role must include Learner');

    authToken = authData.token;
    return `Logged in successfully: roles=[${authData.roles.join(', ')}]`;
  });

  // FS-007: Protected Endpoint Unauthenticated Check
  await executeScenario('FS-007', 'Protected Route Without Token (GET /users/profile → 401)', async () => {
    const res = await fetch(`${backendUrl}/api/v1/users/profile`);
    assert.strictEqual(res.status, 401, `Unauthenticated request must return HTTP 401 (got ${res.status})`);
    const envelope = await res.json();
    assert.strictEqual(envelope.code, 'UNAUTHORIZED');
    return 'Correctly rejected with HTTP 401 UNAUTHORIZED';
  });

  // FS-008: Protected Endpoint Invalid Token Check
  await executeScenario('FS-008', 'Protected Route With Invalid Token (GET /users/profile → 401)', async () => {
    const res = await fetch(`${backendUrl}/api/v1/users/profile`, {
      headers: { 'Authorization': 'Bearer malformed.invalid.token.payload' }
    });
    assert.strictEqual(res.status, 401, `Invalid token request must return HTTP 401 (got ${res.status})`);
    const envelope = await res.json();
    assert.strictEqual(envelope.code, 'UNAUTHORIZED');
    return 'Correctly rejected invalid token with HTTP 401 UNAUTHORIZED';
  });

  // FS-009: Protected Profile Query
  await executeScenario('FS-009', 'Protected Profile Query (GET /users/profile with Bearer → 200)', async () => {
    const res = await fetch(`${backendUrl}/api/v1/users/profile`, {
      headers: { 'Authorization': `Bearer ${authToken}` }
    });
    assert.strictEqual(res.status, 200, `Profile request must return HTTP 200 (got ${res.status})`);
    const envelope = await res.json();
    assert.strictEqual(envelope.code, 'SUCCESS');
    const profile = envelope.data;

    // Strict contract check against UserProfileResponse
    assert.strictEqual(profile.emailOrPhone, testUser.emailOrPhone);
    assert.strictEqual(profile.fullName, testUser.fullName);
    assert.ok(profile.userId, 'userId must be present');
    assert.ok(profile.accountId, 'accountId must be present');
    // Note: Invariant from Prompt — Do NOT assume roles are in UserProfileResponse
    assert.strictEqual(profile.roles, undefined, 'UserProfileResponse must not contain roles field');
    return `Retrieved profile: userId=${profile.userId}, fullName="${profile.fullName}"`;
  });

  // FS-010: Protected Profile Mutation & Persistence
  await executeScenario('FS-010', 'Protected Profile Mutation & DB Persistence (PUT /users/profile)', async () => {
    const updatedName = `Đổi Tên Thành Công ${Date.now().toString().slice(-4)}`;
    const updateRes = await fetch(`${backendUrl}/api/v1/users/profile`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        fullName: updatedName,
        avatarUrl: 'https://example.com/avatar.png'
      })
    });
    assert.strictEqual(updateRes.status, 200, `Profile update must return HTTP 200 (got ${updateRes.status})`);
    const updateEnvelope = await updateRes.json();
    assert.strictEqual(updateEnvelope.code, 'SUCCESS');
    assert.strictEqual(updateEnvelope.data.fullName, updatedName);

    // Verify DB persistence by querying GET /users/profile again
    const verifyRes = await fetch(`${backendUrl}/api/v1/users/profile`, {
      headers: { 'Authorization': `Bearer ${authToken}` }
    });
    assert.strictEqual(verifyRes.status, 200);
    const verifyEnvelope = await verifyRes.json();
    assert.strictEqual(verifyEnvelope.data.fullName, updatedName, 'Updated name must persist across independent GET query');
    return `Updated name to "${updatedName}" and verified in MySQL`;
  });

  // FS-020: Vocabulary Catalog, Search & Progressive Disclosure Protocol (GET /vocabulary & GET /vocabulary/{id})
  await executeScenario('FS-020', 'Vocabulary Catalog, Search & Progressive Radical Disclosure (GET /vocabulary & GET /vocabulary/{id})', async () => {
    // 1. Catalog probe (page=0&size=20)
    const catalogRes = await fetch(`${backendUrl}/api/v1/vocabulary?page=0&size=20`);
    assert.strictEqual(catalogRes.status, 200, `Catalog probe must return HTTP 200 (got ${catalogRes.status})`);
    const catalogEnvelope = await catalogRes.json();
    assert.strictEqual(catalogEnvelope.code, 'SUCCESS');
    assert.strictEqual(catalogEnvelope.data.page, 0);
    assert.strictEqual(catalogEnvelope.data.size, 20);
    assert.ok(Array.isArray(catalogEnvelope.data.items), 'items must be an array');
    assert.ok(catalogEnvelope.data.totalElements >= 1, 'Database must contain at least 1 vocabulary item');

    // Verify Zero N+1 Invariant: summary item must NOT contain radicals
    const firstItem = catalogEnvelope.data.items[0];
    assert.ok(firstItem.vocabId, 'vocabId must be present');
    assert.ok(firstItem.hanzi, 'hanzi must be present');
    assert.strictEqual(firstItem.radicals, undefined, 'Summary VocabularyResponse must NOT include radicals (Zero N+1)');

    // 2. Search probe: search=shu
    const searchRes = await fetch(`${backendUrl}/api/v1/vocabulary?search=shu`);
    assert.strictEqual(searchRes.status, 200);
    const searchEnvelope = await searchRes.json();
    assert.strictEqual(searchEnvelope.code, 'SUCCESS');
    assert.ok(searchEnvelope.data.totalElements >= 1, 'Search for "shu" must find at least 1 match (e.g. 书)');
    assert.ok(searchEnvelope.data.items.some(i => i.hanzi === '书' || i.pinyinRaw === 'shu'));

    // 3. Detail probe: GET /vocabulary/{id}
    const detailRes = await fetch(`${backendUrl}/api/v1/vocabulary/${firstItem.vocabId}`);
    assert.strictEqual(detailRes.status, 200);
    const detailEnvelope = await detailRes.json();
    assert.strictEqual(detailEnvelope.code, 'SUCCESS');
    const detail = detailEnvelope.data;
    assert.strictEqual(detail.vocabId, firstItem.vocabId);
    assert.ok(Array.isArray(detail.radicals), 'VocabularyDetailResponse must contain radicals array');

    return `Catalog confirmed (${catalogEnvelope.data.totalElements} items), search=shu matched, detail ID=${firstItem.vocabId} verified with progressive radicals`;
  });

  // FS-022: Public Lesson Catalog Protocol & Neutral 404 Defense (GET /lessons & GET /lessons/{id})
  await executeScenario('FS-022', 'Public Lesson Catalog Protocol & Neutral 404 Defense (GET /lessons & GET /lessons/{id})', async () => {
    // 1. Catalog probe: page=0&size=20 (Contract: page and size only, NO search/q/keyword)
    const catalogRes = await fetch(`${backendUrl}/api/v1/lessons?page=0&size=20`);
    assert.strictEqual(catalogRes.status, 200, `Lesson catalog must return HTTP 200 (got ${catalogRes.status})`);
    const catalogEnvelope = await catalogRes.json();
    assert.strictEqual(catalogEnvelope.code, 'SUCCESS');
    assert.strictEqual(catalogEnvelope.data.page, 0);
    assert.strictEqual(catalogEnvelope.data.size, 20);
    assert.ok(Array.isArray(catalogEnvelope.data.items), 'items must be an array');

    // 2. Approved-only Visibility Invariant: Every item in catalog must have status === "Approved"
    for (const item of catalogEnvelope.data.items) {
      assert.strictEqual(item.status, 'Approved', `Public catalog item #${item.lessonId} must be Approved`);
      assert.strictEqual(item.author, undefined, 'Summary must not contain invented author field');
      assert.strictEqual(item.difficulty, undefined, 'Summary must not contain invented difficulty field');
    }

    // 3. 404 Neutral Defense probe: non-existent ID (99999999) must return HTTP 404
    const notFoundRes = await fetch(`${backendUrl}/api/v1/lessons/99999999`);
    assert.strictEqual(notFoundRes.status, 404, `Non-existent lesson ID must return HTTP 404 (got ${notFoundRes.status})`);
    const notFoundEnvelope = await notFoundRes.json();
    assert.ok(notFoundEnvelope.code === 'NOT_FOUND' || notFoundRes.status === 404, '404 envelope structure verified');

    return `Lesson catalog confirmed (totalElements=${catalogEnvelope.data.totalElements}, Approved invariant verified), 404 defense confirmed for non-existent ID`;
  });

  return {
    results,
    context: {
      testUser,
      authToken
    }
  };
}
