/**
 * =============================================================================
 * STATIC SERVER LIFECYCLE MANAGER
 * Auto-detects existing local server; boots ephemeral server if none is active.
 * Owner tracking: NEVER kills external servers.
 * =============================================================================
 */

import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FRONTEND_DIR = path.resolve(__dirname, '../../../frontend');

const MIME_TYPES = {
  '.html': 'text/html; charset=UTF-8',
  '.css': 'text/css; charset=UTF-8',
  '.js': 'application/javascript; charset=UTF-8',
  '.mjs': 'application/javascript; charset=UTF-8',
  '.json': 'application/json; charset=UTF-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf'
};

/**
 * Authoritative Content Security Policy (CSP) Directives (Task 9G.3)
 */
export const DEV_CSP_DIRECTIVES = {
  'default-src': ["'self'"],
  'script-src': ["'self'", 'https://cdn.jsdelivr.net'],
  'style-src': ["'self'", 'https://cdn.jsdelivr.net'],
  'style-src-elem': ["'self'", 'https://cdn.jsdelivr.net'],
  'style-src-attr': ["'unsafe-inline'"],
  'font-src': ["'self'"],
  'img-src': ["'self'", 'data:', 'https:'],
  'media-src': ["'self'", 'blob:', 'https:'],
  'connect-src': ["'self'", 'http://localhost:8080', 'http://127.0.0.1:8080'],
  'object-src': ["'none'"],
  'base-uri': ["'self'"],
  'form-action': ["'self'"],
  'frame-ancestors': ["'none'"],
  'frame-src': ["'none'"]
};

export const PROD_CSP_DIRECTIVES = {
  ...DEV_CSP_DIRECTIVES,
  'connect-src': ["'self'"]
};

export function formatCspPolicy(directives) {
  return Object.entries(directives)
    .map(([key, values]) => `${key} ${values.join(' ')}`)
    .join('; ');
}

export const DEV_CSP_POLICY = formatCspPolicy(DEV_CSP_DIRECTIVES);
export const PROD_CSP_POLICY = formatCspPolicy(PROD_CSP_DIRECTIVES);


/**
 * Checks whether a URL is currently responding to HTTP requests.
 * @param {string} targetUrl 
 * @returns {Promise<boolean>}
 */
async function probeUrl(targetUrl) {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 1000);
    const res = await fetch(targetUrl, { method: 'HEAD', signal: controller.signal });
    clearTimeout(timeoutId);
    return res.status < 500;
  } catch {
    return false;
  }
}

/**
 * Creates an ephemeral static HTTP server serving the frontend directory.
 * @param {number} preferredPort 
 * @returns {Promise<{ server: http.Server, port: number }>}
 */
function createEphemeralServer(preferredPort = 3000) {
  return new Promise((resolve, reject) => {
    const server = http.createServer((req, res) => {
      const parsedUrl = new URL(req.url, `http://localhost:${preferredPort}`);
      let reqPath = parsedUrl.pathname;
      if (reqPath === '/' || reqPath === '') {
        reqPath = '/index.html';
      }

      const safePath = path.normalize(reqPath).replace(/^(\.\.[\/\\])+/, '');
      const filePath = path.join(FRONTEND_DIR, safePath);

      if (reqPath === '/favicon.ico' && !fs.existsSync(filePath)) {
        res.writeHead(204, { 'Content-Type': 'image/x-icon' });
        res.end();
        return;
      }

      if (reqPath === '/__test__/eval-probe.js') {
        const fixturePath = path.resolve(__dirname, '../fixtures/csp-eval-probe.js');
        if (fs.existsSync(fixturePath)) {
          res.writeHead(200, {
            'Content-Type': 'application/javascript; charset=UTF-8',
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Access-Control-Allow-Origin': '*',
            'X-Content-Type-Options': 'nosniff'
          });
          fs.createReadStream(fixturePath).pipe(res);
          return;
        }
      }

      if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
        res.writeHead(404, { 'Content-Type': 'text/plain; charset=UTF-8' });
        res.end(`404 Not Found: ${reqPath}`);
        return;
      }

      const ext = path.extname(filePath).toLowerCase();
      const contentType = MIME_TYPES[ext] || 'application/octet-stream';

      const headers = {
        'Content-Type': contentType,
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Access-Control-Allow-Origin': '*',
        'X-Content-Type-Options': 'nosniff'
      };

      if (ext === '.html') {
        headers['Content-Security-Policy'] = DEV_CSP_POLICY;
      }

      res.writeHead(200, headers);

      fs.createReadStream(filePath).pipe(res);
    });

    server.on('error', (err) => {
      if (err.code === 'EADDRINUSE') {
        // Try alternate port if preferred port is unexpectedly taken
        server.listen(0, '127.0.0.1');
      } else {
        reject(err);
      }
    });

    server.listen(preferredPort, '127.0.0.1', () => {
      const addr = server.address();
      const port = typeof addr === 'object' && addr !== null ? addr.port : preferredPort;
      resolve({ server, port });
    });
  });
}

/**
 * Ensures a server is running and returns its base URL and teardown handler.
 * @param {Object} [options]
 * @param {number} [options.port=3000]
 * @returns {Promise<{ baseUrl: string, isExternal: boolean, close: () => Promise<void> }>}
 */
export async function ensureServer({ port = 3000 } = {}) {
  const configuredUrl = process.env.FRONTEND_VERIFY_URL || process.env.BASE_URL;
  const candidateUrl = configuredUrl || `http://localhost:${port}/index.html`;

  const isResponding = await probeUrl(candidateUrl);
  if (isResponding) {
    const base = new URL(candidateUrl).origin;
    return {
      baseUrl: base,
      isExternal: true,
      close: async () => {
        // External server owned by user/daemon: DO NOT kill
      }
    };
  }

  // Not responding: boot ephemeral server
  const { server, port: actualPort } = await createEphemeralServer(port);
  const baseUrl = `http://localhost:${actualPort}`;

  return {
    baseUrl,
    isExternal: false,
    close: () => {
      return new Promise((resolve) => {
        server.close(() => resolve());
      });
    }
  };
}
