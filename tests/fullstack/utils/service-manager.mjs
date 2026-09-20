/**
 * =============================================================================
 * FULL-STACK SERVICE LIFECYCLE & READINESS MANAGER
 * File: tests/fullstack/utils/service-manager.mjs
 * 
 * Invariants:
 * - Bounded health checks and probes (2–5s timeout, never hangs indefinitely).
 * - Reuse running services: NEVER kill or restart active services owned by user.
 * - If MySQL is not running on 3306, transparently reports MYSQL_NOT_RUNNING.
 * - If Spring Boot is not running on 8080, can boot via Maven with bounded poll.
 * - If Frontend is not running on 3000, boots ephemeral static server.
 * =============================================================================
 */

import net from 'node:net';
import { spawn } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { ensureServer } from '../../frontend/utils/static-server.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const ROOT_DIR = path.resolve(__dirname, '../../..');

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';
const FRONTEND_URL = process.env.FRONTEND_URL || 'http://localhost:3000';
const MYSQL_HOST = process.env.MYSQL_HOST || '127.0.0.1';
const MYSQL_PORT = Number(process.env.MYSQL_PORT) || 3306;

/**
 * Probes a TCP port with a strict bounded timeout.
 * @param {string} host 
 * @param {number} port 
 * @param {number} [timeoutMs=2500] 
 * @returns {Promise<boolean>}
 */
export function probeTcpPort(host, port, timeoutMs = 2500) {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    let settled = false;

    const finalize = (isOpen) => {
      if (!settled) {
        settled = true;
        socket.destroy();
        resolve(isOpen);
      }
    };

    socket.setTimeout(timeoutMs);
    socket.once('connect', () => finalize(true));
    socket.once('timeout', () => finalize(false));
    socket.once('error', () => finalize(false));

    try {
      socket.connect(port, host);
    } catch {
      finalize(false);
    }
  });
}

/**
 * Probes Spring Boot Actuator Health endpoint with bounded timeout.
 * @param {string} backendUrl 
 * @param {number} [timeoutMs=2500] 
 * @returns {Promise<{ isUp: boolean, data?: any, error?: string }>}
 */
export async function probeBackendHealth(backendUrl = BACKEND_URL, timeoutMs = 2500) {
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    const res = await fetch(`${backendUrl}/actuator/health`, {
      signal: controller.signal
    });
    clearTimeout(timer);
    if (res.ok) {
      const data = await res.json();
      return { isUp: data?.status === 'UP', data };
    }
    return { isUp: false, error: `HTTP ${res.status}` };
  } catch (err) {
    return { isUp: false, error: err.message };
  }
}

/**
 * Manages full stack readiness: verifies MySQL, Spring Boot, and Frontend.
 * @returns {Promise<{
 *   mysql: { ready: boolean, error?: string },
 *   backend: { ready: boolean, reused: boolean, process?: any, error?: string },
 *   frontend: { ready: boolean, baseUrl: string, isExternal: boolean, close: () => Promise<void> }
 * }>}
 */
export async function ensureFullStackServices() {
  console.log('[Phase 0] Environment Preflight & Service Discovery ...');

  // 1. Check MySQL
  const mysqlRunning = await probeTcpPort(MYSQL_HOST, MYSQL_PORT, 2500);
  console.log(`  MySQL (${MYSQL_HOST}:${MYSQL_PORT}): ${mysqlRunning ? 'RUNNING (Reused) ✓' : 'NOT RUNNING ✗'}`);

  const mysqlStatus = {
    ready: mysqlRunning,
    error: mysqlRunning ? undefined : `MySQL is not listening on ${MYSQL_HOST}:${MYSQL_PORT}.`
  };

  // 2. Check Backend Spring Boot
  let backendHealth = await probeBackendHealth(BACKEND_URL, 2500);
  let backendProcess = null;
  let backendReused = true;

  if (backendHealth.isUp) {
    console.log(`  Spring Boot (${BACKEND_URL}): RUNNING (Reused, Status: UP) ✓`);
  } else if (mysqlRunning) {
    console.log(`  Spring Boot (${BACKEND_URL}): Not responding. Attempting bounded startup via Maven ...`);
    backendReused = false;
    
    // Start Spring Boot
    const mvnCmd = process.platform === 'win32' ? 'mvn.cmd' : 'mvn';
    const jwtSecretArg = '-Dspring-boot.run.arguments=--jwt.secret=elearning-chinese-platform-secret-key-jwt-256-bits-minimum-length-key!';
    
    backendProcess = spawn(mvnCmd, ['-f', 'backend/pom.xml', 'spring-boot:run', jwtSecretArg], {
      cwd: ROOT_DIR,
      stdio: ['ignore', 'pipe', 'pipe'],
      detached: false
    });

    // Poll health for up to 60s
    const pollStart = Date.now();
    const pollTimeoutMs = 60000;
    while (Date.now() - pollStart < pollTimeoutMs) {
      await new Promise(r => setTimeout(r, 2000));
      backendHealth = await probeBackendHealth(BACKEND_URL, 2000);
      if (backendHealth.isUp) {
        console.log(`  Spring Boot (${BACKEND_URL}): Successfully started in ${Math.round((Date.now() - pollStart) / 1000)}s ✓`);
        break;
      }
    }
  } else {
    console.log(`  Spring Boot (${BACKEND_URL}): Skipped startup because MySQL is not running.`);
  }

  const backendStatus = {
    ready: backendHealth.isUp,
    reused: backendReused,
    process: backendProcess,
    error: backendHealth.isUp ? undefined : (backendHealth.error || 'Spring Boot failed to reach UP state within timeout.')
  };

  // 3. Check Frontend Static Server
  const frontendHandle = await ensureServer({ port: 3000 });
  console.log(`  Frontend (${frontendHandle.baseUrl}): ${frontendHandle.isExternal ? 'RUNNING (Reused external)' : 'STARTED (Ephemeral)'} ✓\n`);

  return {
    mysql: mysqlStatus,
    backend: backendStatus,
    frontend: {
      ready: true,
      baseUrl: frontendHandle.baseUrl,
      isExternal: frontendHandle.isExternal,
      close: frontendHandle.close
    }
  };
}
