# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

---

## Reporting a Vulnerability

We take the security of this platform very seriously. If you discover a security vulnerability, please report it responsibly:

1. **Do NOT open a public GitHub issue** to report sensitive security bugs or vulnerabilities.
2. Please contact the security team or maintainers privately via GitHub Security Advisories or by emailing the project maintainer.
3. Provide comprehensive details of the vulnerability, including:
   - Affected endpoint, component, or file
   - Reproduction steps, proof-of-concept payload, or request trace
   - Potential business and technical impact
4. You will receive an acknowledgment within 48 hours, followed by updates on the remediation timeline.

---

## Security Practices & Baseline Controls

This codebase enforces strict automated and architectural security invariants:

- **Zero Hardcoded Secrets**: Secrets (including JWT signing keys and database passwords) are injected exclusively via environment variables.
- **Stateless RBAC**: Endpoints strictly enforce 4-tier role-based access control. Cross-tenant access is prohibited by binding queries to authenticated account identities extracted from the security context (IDOR defense).
- **DOM XSS Defense**: Strict avoidance of dangerous DOM sinks (`innerHTML`, `outerHTML`, `document.write`). Safe DOM primitives (`textContent`, `createElement`, `setAttribute`) and URL sanitizers are systematically employed.
- **Content Security Policy (CSP)**: All frontend pages enforce a strict CSP preventing unauthorized script execution and restricting object, frame, and base-uri directives.
- **Subresource Integrity (SRI)**: All external CDN resources (CSS, JS, fonts) are cryptographically validated using sha384 hashes with anonymous CORS mode.
- **GitHub Push Protection**: We require and recommend GitHub Push Protection and Secret Scanning to block accidental credential commits.
