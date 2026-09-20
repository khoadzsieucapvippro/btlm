# Content Security Policy (CSP) Construction & Client Hardening

## 1. Resource Graph Methodology for CSP

A Content Security Policy (CSP) is an essential defense-in-depth barrier against XSS and resource injection.

> **Engineering Rule**: **DO NOT invent or finalize a rigid CSP policy before the actual frontend resource graph exists**.
> A working CSP must be constructed based on the verified list of external and internal resources loaded by the frontend pages once HTML and CSS integration is complete.

---

## 2. Resource Graph Inventory Process

When pages are built, document every required network origin across standard directives:

```text
+-------------------+-------------------------------------------------------------+
| Directive         | Resource Type & Mapping Process                             |
+-------------------+-------------------------------------------------------------+
| default-src       | Fallback baseline; SHOULD be set to 'self'                  |
| script-src        | Application ES modules ('self') + Approved CDNs             |
| style-src         | style.css ('self') + Approved font/CSS CDNs                 |
| font-src          | Web fonts ('self') + Approved font CDNs (e.g., Google/jsDelivr)|
| img-src           | Local assets ('self') + Inline icons ('data:')              |
| media-src         | Audio pronunciation ('self', 'blob:', or explicit host)     |
| connect-src       | REST API endpoint ('self' for relative, or explicit backend)|
| object-src        | Plugins (Flash/Java); MUST be 'none'                        |
| frame-ancestors   | Clickjacking defense; MUST be 'none' or 'self'              |
| base-uri          | Base tag injection defense; MUST be 'self'                  |
| form-action       | Form submission targets; MUST be 'self'                     |
+-------------------+-------------------------------------------------------------+
```

---

## 3. Policy Formulation & Staging Rules

1. **Avoid `'unsafe-inline'` for Scripts**: The frontend is built with modular JavaScript files (`<script type="module" src="...">`). There is no operational need for inline `<script>` blocks or inline `onclick` handlers.
2. **Avoid `'unsafe-eval'`**: The application does not use string evaluation or dynamic template compilation.
3. **Staging via `Content-Security-Policy-Report-Only`**:
   - Before enforcing a strict CSP header in production, deploy the policy in `Report-Only` mode.
   - Monitor the browser console and report endpoints for legitimate assets being inadvertently blocked.
   - Once verified against all 4 user surfaces, switch to enforcing `Content-Security-Policy`.

---

## 4. Subresource Integrity (SRI) Discipline

When loading third-party libraries (e.g. Bootstrap 5.3) via public CDNs:
- **Explicit Versions**: The CDN URL MUST reference an exact version (e.g. `bootstrap@5.3.3`), NEVER `@latest`.
- **Verified SRI Hashes**: An `integrity="sha384-..."` attribute MUST match the exact cryptographic hash provided by the library maintainer or generated via `openssl dgst -sha384 -binary <file> | openssl base64 -A`.
- **DO NOT claim SRI compliance** without verifying the actual hash matches the loaded file.

---

## 5. Dependency Discipline

No new third-party JavaScript libraries or CSS frameworks may be introduced simply for convenience:
- Any proposed dependency requires:
  1. Technical justification (why standard browser APIs cannot fulfill the requirement).
  2. Bundle impact assessment.
  3. Security / vulnerability review.
  4. Permissive open-source license verification.
