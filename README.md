# E-Learning Platform: Kangxi Radicals & Chinese Vocabulary with SRS

An educational web platform for mastering the 214 Kangxi radicals and Chinese vocabulary through structured curriculum lessons, character decomposition, and long-term memory retention powered by an SM-2 Spaced Repetition System (SRS).

---

## Architecture & Technology Stack

### Backend
- **Runtime & Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.3.5
- **Security**: Spring Security 6, JJWT 0.12.6 (Stateless HMAC-SHA256, mandatory issuer validation, per-account authorization versioning)
- **Persistence**: Spring Data JPA / Hibernate 6, MySQL 8.4 Connector
- **Database Migrations**: Flyway Core (Versioned DDL/DML migrations V1–V7)
- **Validation**: Jakarta Validation / Hibernate Validator
- **Excel Ingestion**: Apache POI (Strict two-phase preview and atomic confirmation)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (MySQL 8.4)

### Frontend
- **Paradigm**: Zero-Build Modular Vanilla JavaScript (ES Modules, native browser execution)
- **Styling**: Vanilla CSS custom design system (Warm canvas, Cinnabar `#C83C23` accent, Celadon `#1B7A4B` badges, Scholar theme) + Bootstrap 5.3.3 grid & utilities
- **Typography**: Inter, Noto Sans SC, KaiTi (Traditional scholarly calligraphy aesthetic)
- **Accessibility**: WCAG 2.2 Level AA compliance (semantic landmarks, skip navigation, focus management, high contrast ratios)
- **Security Hardening**: Strict Content Security Policy (CSP), subresource integrity (SRI), zero dangerous DOM sinks (`innerHTML`), centralized API client with automatic Bearer token injection

### System Roles (4-Tier RBAC)
1. **Learner**: Public catalog browsing, Kangxi radical lookup, interactive lesson study, SRS flashcard reviews, personal vocabulary notes.
2. **Creator**: Personal lesson authoring, two-phase Excel curriculum import, submission to moderation queue.
3. **Moderator**: Lesson moderation queue review, structured approval/rejection with flagged field feedback and audit logging (`MODERATION_LOG`).
4. **Admin**: User and account management, role assignment, system-wide content governance, audit oversight.

---

## Prerequisites

- **Java Development Kit (JDK)**: Version 21+
- **Apache Maven**: Version 3.9+
- **Node.js**: Version 20+ (for test automation runner)
- **Database**: MySQL 8.4+ (or Docker for Testcontainers)

---

## Quick Start & Local Setup

### 1. Database Configuration

Create the MySQL database:

```sql
CREATE DATABASE elearning_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Flyway automatically handles schema generation and initial seed data (V1–V7) upon Spring Boot startup.

### 2. Backend Startup

Set required environment variables and launch the Spring Boot application:

```bash
cd backend

# Configure environment (example values for local development)
export JWT_SECRET="your-secure-256-bit-minimum-secret-key-for-jwt-signing"
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/elearning_db?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="your_password"

# Run application
mvn spring-boot:run
```

The backend REST API will be available at `http://localhost:8080`.

### 3. Frontend Startup

Serve the `frontend/` directory with any static HTTP file server:

```bash
# Using Node.js http-server or Python:
npx serve frontend -p 3000

# Or using Python:
# python -m http.server 3000 --directory frontend
```

Open `http://localhost:3000` in your browser.

---

## Testing & Verification

The repository includes a comprehensive multi-tier test architecture:

```bash
# Frontend Fast Tier (Static + Unit tests, ~2.5s)
npm run verify:frontend:fast

# Frontend Static Analysis (HTML5 semantics, SRI, DOM sinks, CSS tokens)
npm run verify:frontend:static

# Frontend Unit Tests (State machines, API client, SRS calculation, forms)
npm run verify:frontend:unit

# Frontend Accessibility Audit (WCAG 2.2 AA via Playwright / Chromium)
npm run verify:frontend:a11y

# Backend Unit & Controller Slices (No external database required)
cd backend
mvn test "-Dtest=*ServiceTests,*ControllerTests,*DtoTests,JwtUtilTests,ApiResponseTests"

# Full Backend Suite (Requires Docker for MySQL 8.4 Testcontainers)
mvn clean test
```

---

## Security Architecture

- **Stateless Authentication**: Pure JWT authentication without server sessions. Tokens bind to account status and `authorization_version` for instant session revocation upon role alteration or suspension.
- **Zero Client Trust**: All authorization checks are enforced server-side. User identity is extracted strictly from `SecurityContextHolder`, never accepted from request parameters or payloads (IDOR prevention).
- **Adversarial Input Sanitization**: Text and audio URLs undergo strict sanitization to prevent Stored and DOM-based Cross-Site Scripting (XSS).
- **Environment Driven**: Zero hardcoded secrets, database passwords, or JWT keys exist in source code or default configuration files.

---

## Project Specifications & Standards

Detailed architectural documentation and engineering decisions are tracked in `.agents/`:
- `.agents/API.md`: Authoritative REST API contracts and error code definitions
- `.agents/ARCHITECTURE.md`: High-level system topology and layer responsibilities
- `.agents/DATABASE_DESIGN.md`: Relational schema, indices, and Flyway migration policy
- `.agents/DECISIONS.md`: Authoritative Architectural Decision Records (ADR)
- `.agents/CURRENT_STATE.md`: Real-time implementation and verification state

---

## License

This project is private and proprietary. All rights reserved.
