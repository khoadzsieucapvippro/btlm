/**
 * =============================================================================
 * UNIT TEST: RUNTIME ENVIRONMENT CONFIGURATION (TASK FE-REMEDIATION-03)
 * Module: tests/frontend/unit/config/config.test.mjs
 * =============================================================================
 */

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const CONFIG_SCRIPT_PATH = path.resolve(__dirname, '../../../../frontend/js/config.js');
const configScriptCode = fs.readFileSync(CONFIG_SCRIPT_PATH, 'utf-8');

/**
 * Helper to run config.js in a sandboxed window context
 */
function runConfigInContext(mockWindow) {
  const context = vm.createContext({
    window: mockWindow,
    globalThis: mockWindow,
    Object
  });
  vm.runInContext(configScriptCode, context);
  return mockWindow.__ENV__;
}

describe('Runtime Environment Configuration (config.js)', () => {

  test('routes localhost:3000 to http://localhost:8080/api/v1', () => {
    const mockWin = {
      location: {
        hostname: 'localhost',
        port: '3000'
      }
    };

    const env = runConfigInContext(mockWin);
    assert.strictEqual(env.API_BASE_URL, 'http://localhost:8080/api/v1');
    assert.ok(Object.isFrozen(env), 'window.__ENV__ must be frozen');
  });

  test('routes 127.0.0.1:5500 to http://localhost:8080/api/v1', () => {
    const mockWin = {
      location: {
        hostname: '127.0.0.1',
        port: '5500'
      }
    };

    const env = runConfigInContext(mockWin);
    assert.strictEqual(env.API_BASE_URL, 'http://localhost:8080/api/v1');
  });

  test('routes other dev static ports (5173, 8000, 8081) on localhost to backend', () => {
    for (const port of ['5173', '8000', '8081']) {
      const mockWin = {
        location: {
          hostname: 'localhost',
          port
        }
      };
      const env = runConfigInContext(mockWin);
      assert.strictEqual(env.API_BASE_URL, 'http://localhost:8080/api/v1', `Port ${port} should map to backend`);
    }
  });

  test('defaults to relative /api/v1 on same-origin backend port 8080', () => {
    const mockWin = {
      location: {
        hostname: 'localhost',
        port: '8080'
      }
    };

    const env = runConfigInContext(mockWin);
    assert.strictEqual(env.API_BASE_URL, '/api/v1');
  });

  test('defaults to relative /api/v1 on production domains', () => {
    const mockWin = {
      location: {
        hostname: 'elearning.chinese.edu',
        port: ''
      }
    };

    const env = runConfigInContext(mockWin);
    assert.strictEqual(env.API_BASE_URL, '/api/v1');
  });

  test('preserves pre-injected window.__ENV__.API_BASE_URL override', () => {
    const mockWin = {
      location: {
        hostname: 'localhost',
        port: '3000'
      },
      __ENV__: {
        API_BASE_URL: 'https://staging-api.example.com/v1',
        CUSTOM_FLAG: true
      }
    };

    const env = runConfigInContext(mockWin);
    assert.strictEqual(env.API_BASE_URL, 'https://staging-api.example.com/v1');
    assert.strictEqual(env.CUSTOM_FLAG, true);
    assert.ok(Object.isFrozen(env));
  });

  test('handles missing or partial location object safely without throwing', () => {
    const mockWin = {};
    const env = runConfigInContext(mockWin);
    assert.strictEqual(env.API_BASE_URL, '/api/v1');
    assert.ok(Object.isFrozen(env));
  });
});
