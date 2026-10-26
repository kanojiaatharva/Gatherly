# Gatherly — GenAI Event Planning Assistant

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev)
[![TypeScript](https://img.shields.io/badge/TypeScript-Strict-blue.svg)](https://www.typescriptlang.org)
[![Render Deployable](https://img.shields.io/badge/Deploy%20to-Render-46E3B7.svg)](https://render.com)

Gatherly is a domain-driven event planning platform designed for orchestrating weddings, milestone birthdays, and community celebrations. Gatherly combines structured GenAI planning workflows with deterministic monetary accounting, strict user confirmation gates, task dependency cycle prevention, and real-world vendor quote comparison.

---

## Architecture & Repository Structure

```
Gatherly/
├── .github/
│   └── workflows/
│       └── ci.yml               # GitHub Actions CI (lint, typecheck, test, build)
├── backend/                     # Spring Boot 3.5.0 (Java 21) REST API
│   ├── Dockerfile               # Multi-stage hardened non-root container image
│   ├── .dockerignore            # Build context exclusions
│   ├── pom.xml                  # Maven dependencies & build lifecycle
│   └── src/
│       ├── main/java/com/gatherly/
│       │   ├── ai/              # Dual AI provider (mock fallback + OpenAI-compatible)
│       │   ├── auth/            # JWT auth, user principal, rate-limited auth endpoints
│       │   ├── budget/          # BigDecimal monetary arithmetic & budget management
│       │   ├── common/          # GlobalExceptionHandler, RateLimiterService, HealthController
│       │   ├── config/          # Spring Security 6, CSP headers, CORS allowlist
│       │   ├── events/          # Event lifecycle, impact simulator, ownership checks
│       │   ├── tasks/           # Directed acyclic graph task dependencies & timeline tracks
│       │   └── vendors/         # Anti-hallucination quote comparison & budget link
│       ├── main/resources/
│       │   ├── application.yml  # Base configuration (PORT, CORS, DB fallback)
│       │   ├── application-local.yml # Local H2 file database configuration
│       │   └── db/migration/    # Flyway versioned database migrations (V1, V2, V3)
│       └── test/                # 28 automated integration & validation test suites
├── frontend/                    # React 18 + TypeScript + Vite SPA
│   ├── package.json             # Scripts (dev, build, lint, test, preview)
│   ├── package-lock.json        # Pinned reproducible dependency tree
│   ├── tsconfig.json            # Strict TypeScript configuration
│   ├── eslint.config.js         # ESLint flat configuration
│   ├── index.html               # Semantic HTML entry with accessibility metadata
│   └── src/
│       ├── api.ts               # Resilient fetch API client with token management
│       ├── api.test.ts          # Vitest unit test suite (8 tests)
│       ├── App.tsx              # Main dashboard, wizard, tabs & domain modals
│       ├── styles.css           # Vanilla CSS design system & prefers-reduced-motion
│       └── components/          # Reusable modal and dialog components
├── database/
│   └── schema.sql               # Base database schema reference
├── .env.example                 # Environment variables template
├── .gitignore                   # Git exclusions (credentials, artifacts, data)
├── render.yaml                  # Render Blueprint (backend web service + frontend static site)
├── package.json                 # Top-level workspace script runner
└── README.md                    # Comprehensive documentation
```

---

## Core Features

### 1. Event Brief & Creation Wizard
- Multi-step validation: event name, event type (Wedding, Birthday, Community, Corporate), target celebration date, city/location, guest headcount, currency, target budget, style/atmosphere, dietary accommodations, accessibility needs, and priorities.
- Dashboard with quick metrics: headcount, budget burn, remaining balance, and countdown.

### 2. GenAI Plan Generation & Safe Revision
- **Structured Outputs**: Generates comprehensive plan structures (executive summary, assumptions, theme concepts, allocated budget categories, task checklists, timeline phases, vendor research categories with interview questions, and personalized ideas).
- **Dual AI Provider Architecture**:
  - `LIVE_MODEL` mode: Connects to any OpenAI-compatible API (`gpt-4o-mini`, `gpt-4o`, local LLMs) with automatic single-turn retry on schema validation failures.
  - `DEVELOPMENT_MOCK` mode: Deterministic, rule-based planner that runs offline without external API dependencies or API keys.
- **Safety & Preservation of Manual Edits**:
  - Revisions are returned as proposed diffs (`PlanRevisionProposal`). Persisted event state is **never** directly overwritten without explicit user review and acceptance.
  - User-confirmed budget items (`isEstimate == false` or `actualAmount != null`) and completed tasks are permanently preserved and protected from AI modification or deletion.
  - Discretionary budget trims respect committed actual expenditures.

### 3. Deterministic Budget Management
- **Monetary Arithmetic**: All financial calculations use `java.math.BigDecimal` with `RoundingMode.HALF_UP`.
- **Validation**: Enforces non-negative amounts (`@DecimalMin("0.00")`) and valid currency codes.
- **Auto-Confirmation**: Entering an `actualAmount` automatically confirms the expenditure and toggles `isEstimate = false`.
- **Proactive Alerts**: Over-budget detection (`isOverBudget`, `overBudgetAmount`) with color-coded progress bars and breakdown summaries.

### 4. Tasks, Dependencies & Timeline Views
- **Task Management**: Title, description, due date, status (`TODO`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`), priority (`LOW`, `MEDIUM`, `HIGH`, `URGENT`), and assignee label.
- **Prerequisite Dependencies**: Tasks can depend on another task (`dependsOnTaskId`).
- **Cycle Prevention**: Dependency graphs are verified up to 30 levels deep to prevent circular deadlocks (e.g., A $\rightarrow$ B $\rightarrow$ A) with HTTP 400.
- **Dynamic Blocked Status**: A downstream task is flagged `isBlocked = true` until its prerequisite is marked `COMPLETED` or `CANCELLED`.
- **Dual-Mode UI**:
  - **Interactive Checklist View**: Quick toggle checkboxes, category filtering, priority tags, and locked prerequisite badges.
  - **Milestone Timeline Track**: Chronological groupings into Overdue, Next 7 Days, Next 30 Days, Later, and Completed, with one-click **"Push to Tomorrow"** overdue date recovery.

### 5. Event Change Impact Simulator
- Simulates consequences when guest count, celebration date, or target budget change before saving.
- Differentiates variable costs (catering, beverages, tableware, favors) that scale with guest ratios from fixed overheads (venue base hire, DJ/audio).
- Flags compressed timelines and lists tasks whose deadlines fall past the proposed event date.

### 6. Vendor Organizer & Quote Comparison Matrix
- **Strict Anti-Hallucination Policy**: Gatherly strictly prohibits fabricating fake vendor businesses, invented reviews, or simulated pricing.
- **Audit & Verification**: Tracks vendor category, business name, contact information, quoted price, quote status (`UNVERIFIED`, `ESTIMATED`, `QUOTED`, `REJECTED`), booking status (`NOT_BOOKED`, `CONTACTED`, `SHORTLISTED`, `BOOKED`, `CANCELLED`), source channel (`DIRECT_PROPOSAL`, `Personal Referral`, `Venue Preferred List`), and verification timestamp (`lastVerifiedAt`).
- **Quote Comparison Matrix**: Groups vendor candidates by category, compares competing bids side-by-side against the category target budget, and calculates exact variance (savings or overage).
- **1-Click Booking & Budget Link**: Clicking **"Book & Link"** marks the vendor as `BOOKED`, locks the quoted price, updates the matching budget category's `actualAmount`, and sets `isEstimate = false`.

### 7. Security & Hardening
- **Stateless Authentication**: Spring Security 6 with BCrypt password hashing and HMAC-SHA256 JWT authorization tokens.
- **Resource Ownership Verification**: Every private endpoint verifies that the authenticated user owns the target event via `OwnershipService.verifyAndGetEvent`. Cross-user access is blocked with HTTP 403 Forbidden.
- **Public & AI Rate Limiting**: In-memory token bucket rate limiter protects both AI planning endpoints (`/plan/generate`, `/plan/revise`) and public authentication endpoints (`/api/auth/register`, `/api/auth/login`) with HTTP 429 Too Many Requests.
- **Security Headers & CORS**: Strict Content-Security-Policy (CSP), `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, and a configurable CORS allowlist (`CORS_ALLOWED_ORIGINS`).
- **Zero Information Leakage**: Centralized `GlobalExceptionHandler` logs errors safely without leaking internal database stack traces, credentials, or prompts to clients.

---

## Technology Stack

| Component | Technology | Version | Description |
|---|---|---|---|
| **Backend Framework** | Spring Boot | `3.5.0` | Core REST API framework |
| **Language Runtime** | Java | `21` | Modern LTS Java runtime |
| **Build Tool** | Apache Maven | `3.9+` | Build & dependency management |
| **Security** | Spring Security | `6.5.0` | Stateless filter chain & authorization |
| **JWT** | JJWT | `0.12.6` | JSON Web Token parsing & signing |
| **Database Migration** | Flyway | `11.7.2` | Versioned SQL schema migrations |
| **Persistence** | Spring Data JPA / Hibernate | `6.6.15` | Parameterized ORM queries |
| **Database** | MySQL / H2 | `8.0` / `2.3` | MySQL in prod, H2 in dev & tests |
| **Frontend Framework** | React | `18.3.1` | Declarative component UI |
| **Frontend Tooling** | Vite | `8.3.5` | Fast ESM build & dev server |
| **Language** | TypeScript | `5.7+` | Strict type checking (`strict: true`) |
| **Icons** | Lucide React | `latest` | Accessible lightweight icon library |
| **Frontend Testing** | Vitest | `5.0.5` | Deterministic unit test runner |
| **Linter** | ESLint | `10.13.0` | Flat configuration linter |
| **Containerization** | Docker | Multi-stage | Alpine-based non-root production image |

---

## Prerequisites

- **Java JDK**: Version 21 or later
- **Maven**: Version 3.9+ (or use Docker / IDE runner)
- **Node.js**: Version 20+ LTS
- **npm**: Version 10+
- **Docker**: (Optional) For containerized local testing and deployment
- **MySQL**: (Optional) MySQL 8.0+ for local MySQL development (H2 file database is used automatically by default when `SPRING_PROFILES_ACTIVE=local`)

---

## Environment Variables Reference

| Variable | Description | Default | Example | Secret? | Required on Render |
|---|---|---|---|:---:|:---:|
| `PORT` | Listening HTTP port bound to `0.0.0.0` | `8080` | `8080` | No | Render assigns automatically |
| `SERVER_PORT` | Fallback port identifier | `8080` | `8080` | No | No |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`local` or `prod`) | `local` | `local` | No | Yes |
| `JWT_SECRET` | HMAC-SHA256 signature secret (min 256 bits) | Pre-configured dev secret | `c2VjdXJlLWdhdGhlcmx5LWp3dC1rZXktZXhhbXBsZQ==` | **Yes** | Yes (`generateValue: true`) |
| `JWT_EXPIRATION_MS` | JWT expiration duration in milliseconds | `86400000` (24h) | `86400000` | No | No |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed origins | `http://localhost:5173,http://localhost:3000` | `https://gatherly-frontend.onrender.com` | No | Yes |
| `AUTH_RATE_LIMIT_PER_MINUTE` | Max auth requests per client IP per minute | `60` | `60` | No | No |
| `SPRING_DATASOURCE_URL` | JDBC URL for production database | *(Uses MySQL or local H2)* | `jdbc:mysql://host:3306/gatherly` | No | If using external DB |
| `DB_HOST` | Database host | `localhost` | `mysql.render.internal` | No | If using MySQL host |
| `DB_PORT` | Database port | `3306` | `3306` | No | No |
| `DB_NAME` | Database schema name | `gatherly` | `gatherly_db` | No | No |
| `DB_USERNAME` | Database username | `gatherly_app` | `gatherly_user` | No | If using MySQL |
| `DB_PASSWORD` | Database password | `replace_me` | `p@ssw0rd123!` | **Yes** | If using MySQL |
| `AI_PROVIDER` | AI provider implementation (`mock` or `openai`) | `mock` | `mock` | No | No |
| `AI_API_KEY` | OpenAI API key (for `openai` provider) | `""` | `sk-proj-...` | **Yes** | Only if `AI_PROVIDER=openai` |
| `AI_MODEL` | AI model name | `gpt-4o-mini` | `gpt-4o-mini` | No | No |
| `AI_BASE_URL` | OpenAI-compatible API base URL | `https://api.openai.com/v1` | `https://api.openai.com/v1` | No | No |
| `AI_TIMEOUT_SECONDS` | GenAI completion timeout in seconds | `25` | `30` | No | No |
| `AI_RATE_LIMIT_PER_MINUTE` | Max AI requests per user per minute | `15` | `20` | No | No |
| `VITE_API_BASE_URL` | Frontend API client base URL | `http://localhost:8080/api` | `https://gatherly-backend.onrender.com/api` | No | Yes (on static site) |

---

## Local Setup & Quick Start

### 1. Clone & Configure
```bash
git clone https://github.com/kanojiaatharva/Gatherly.git
cd Gatherly
cp .env.example .env
```

### 2. Start the Backend
The backend runs out-of-the-box with embedded persistent H2 storage without requiring external dependencies:
```bash
cd backend
mvn spring-boot:run
```
The backend initializes on `http://localhost:8080`. Verify with the health check:
```bash
curl http://localhost:8080/health
```
Response:
```json
{"status":"ok","service":"gatherly-api"}
```

To test running with an explicit port matching Render's runtime behavior:
```bash
PORT=8085 mvn spring-boot:run
curl http://localhost:8085/health
```

### 3. Start the Frontend
In a separate terminal:
```bash
cd frontend
npm ci
npm run dev
```
Open `http://localhost:5173` in your browser.

---

## Running Tests & Verifications

All tests and quality checks run with single commands:

### Backend Tests (Maven)
```bash
cd backend
mvn test
```
- **Total Backend Tests**: **28**
- **Failures**: **0**, **Errors**: **0**, **Skipped**: **0**
- Test suites:
  1. `AuthIntegrationTest`: Registration, login, profile token resolution, and auth rate-limiting.
  2. `EventValidationTest`: Constraints, dates, budget non-negativity, and both `/health` and `/api/health` verification.
  3. `EventOwnershipIntegrationTest`: Strict tenant isolation; cross-user event mutation rejection (HTTP 403).
  4. `EventFeaturesIntegrationTest`: Event update, patching, guest count adjustment, and deletion cascading.
  5. `PlanningIntegrationTest`: Deterministic AI plan generation, budget items, and timeline synthesis.
  6. `AiPlanningTests`: Plan revisions, proposal diff calculation, and manual edit preservation.
  7. `BudgetAndTaskTests`: Category totals, over-budget calculations, task prerequisite cycles, and impact simulator.
  8. `VendorSecurityAndQaIntegrationTest`: Sub-resource ownership, vendor verification, budget linking, and AI rate limiting.
  9. `GatherlyApplicationTests`: Spring Boot application context startup validation.

### Frontend Tests (Vitest)
```bash
cd frontend
npm test
```
- **Total Frontend Tests**: **8**
- **Failures**: **0**
- Tests: `ApiClient` token persistence, bearer headers, auto-login, error parsing, and query parameter serialization.

### Unified Workspace Tests
From repository root:
```bash
npm test                  # Runs frontend test suite
npm run test:backend      # Runs backend test suite
npm run test:all          # Runs both frontend and backend test suites
```

### Linter & Type Check
```bash
npm run lint              # ESLint across codebase (0 errors, 0 warnings)
npm run build             # TypeScript strict check (tsc -b) & Vite build
```

### Dependency Vulnerability Audit
```bash
npm audit                 # 0 vulnerabilities found
```

---

## Deploying to Render

Gatherly is configured for zero-friction deployment on Render using either the Infrastructure as Code Blueprint (`render.yaml`) or the Render Web Dashboard.

### Option A: 1-Click Blueprint Deployment (Recommended)

1. Push your repository to GitHub or GitLab.
2. In the [Render Dashboard](https://dashboard.render.com/), click **New** $\rightarrow$ **Blueprint**.
3. Connect your repository. Render will automatically detect [`render.yaml`](file:///render.yaml) and declare two services:
   - **`gatherly-backend`**: Docker Web Service running Spring Boot 3 on Java 21.
   - **`gatherly-frontend`**: Static Site building React 18 / Vite with SPA client routing rewrite rules.
4. Click **Apply**. Render will generate a cryptographically secure `JWT_SECRET` and deploy both services.
5. In the frontend service settings, update `VITE_API_BASE_URL` to point to your deployed backend URL: `https://<your-backend-slug>.onrender.com/api`.

---

### Option B: Manual Setup on Render

#### 1. Backend Web Service (`gatherly-backend`)
1. Click **New** $\rightarrow$ **Web Service**.
2. Connect your repository.
3. Configure the service settings:
   - **Name**: `gatherly-backend`
   - **Language / Environment**: `Docker`
   - **Dockerfile Path**: `./backend/Dockerfile`
   - **Docker Context**: `./backend`
   - **Instance Type**: `Free`
   - **Health Check Path**: `/health`
4. In **Environment Variables**, add:
   - `PORT`: `8080`
   - `SPRING_PROFILES_ACTIVE`: `local` (or `prod` if using external MySQL)
   - `JWT_SECRET`: *(Generate a random 32+ character string)*
   - `AI_PROVIDER`: `mock`
   - `CORS_ALLOWED_ORIGINS`: `https://<your-frontend-slug>.onrender.com,http://localhost:5173`
5. Click **Create Web Service**.

#### 2. Frontend Static Site (`gatherly-frontend`)
1. Click **New** $\rightarrow$ **Static Site**.
2. Connect your repository.
3. Configure settings:
   - **Name**: `gatherly-frontend`
   - **Build Command**: `cd frontend && npm ci && npm run build`
   - **Publish Directory**: `./frontend/dist`
4. In **Redirects / Rewrites**, add the client-side SPA routing rule:
   - **Type**: `Rewrite`
   - **Source**: `/*`
   - **Destination**: `/index.html`
5. In **Environment Variables**, add:
   - `NODE_VERSION`: `20.18.0`
   - `VITE_API_BASE_URL`: `https://<your-backend-slug>.onrender.com/api`
6. Click **Create Static Site**.

---

## API Reference & cURL Examples

All endpoints bind to `0.0.0.0` and default to port `8080` (or the configured `PORT`).

### Health Checks (No Auth Required)
```bash
# Lightweight root health check for Render
curl -i http://localhost:8080/health

# API namespace health check
curl -i http://localhost:8080/api/health
```

### 1. Register User
```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "planner@example.com",
    "password": "Password123!",
    "displayName": "Alex Morgan"
  }'
```

### 2. Login
```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "planner@example.com",
    "password": "Password123!"
  }'
```

### 3. Create Event
```bash
curl -i -X POST http://localhost:8080/api/events \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alex & Jordan Wedding",
    "eventType": "Wedding",
    "eventDate": "2027-06-15",
    "location": "Napa Valley, CA",
    "guestCount": 110,
    "currency": "USD",
    "totalBudget": 35000.00,
    "stylePreferences": "Rustic Wine Country Elegance",
    "dietaryRequirements": "Vegetarian & Gluten-Free options",
    "accessibilityNeeds": "Wheelchair accessible garden paths",
    "priorities": "Exceptional dining, live jazz acoustic band, candid photography"
  }'
```

### 4. Generate AI Plan
```bash
curl -i -X POST http://localhost:8080/api/events/1/plan/generate \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Emphasize outdoor vineyard sunset cocktail hour."}'
```

### 5. Simulate Impact of Event Changes
```bash
curl -i -X POST http://localhost:8080/api/events/1/impact-analysis \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "guestCount": 140,
    "totalBudget": 40000.00,
    "eventDate": "2027-05-15"
  }'
```

### 6. Add Vendor & Book Quote
```bash
# Add vendor quote
curl -i -X POST http://localhost:8080/api/events/1/vendors \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "category": "Photography",
    "name": "Golden Hour Visuals",
    "contactDetails": "hello@goldenhour.com",
    "quotedAmount": 3800.00,
    "quoteStatus": "QUOTED",
    "bookingStatus": "SHORTLISTED",
    "source": "DIRECT_PROPOSAL",
    "notes": "Full day coverage with second shooter"
  }'

# Book vendor and automatically link quote to budget actual amount
curl -i -X POST http://localhost:8080/api/events/1/vendors/1/book \
  -H "Authorization: Bearer <TOKEN>"
```

---

## Accessibility & Performance Features

- **Semantic Elements**: Full accessibility tree with semantic landmarks (`<aside>`, `<header>`, `<main>`, `<nav>`).
- **Input Accessibility**: All form elements have associated labels, explicit `name` and `id` attributes, and `aria-label` annotations.
- **Motion Reduction**: Full support for CSS `@media (prefers-reduced-motion: reduce)`, disabling non-essential animations for users with vestibular sensitivities.
- **Focus Indicators**: High-contrast, visible 2px focus outlines (`:focus-visible`) for complete keyboard navigation.
- **Database Performance**: Foreign keys and frequently queried fields are indexed (`idx_events_user_date`, `idx_budget_event`, `idx_tasks_event_due`, `idx_tasks_depends_on`).
- **N+1 Prevention**: Batch fetching with Spring Data JPA queries.

---

## Known Limitations & Roadmap

1. **Single-Tenant Event Ownership**:
   - Current release enforces strict single-user event tenancy. Shared team collaborator permissions (`OWNER`, `EDITOR`, `VIEWER`) are slated for the next release.
2. **External Vendor Discovery**:
   - External web scraping is disabled by default to guarantee 100% verified quotes without AI hallucinations. Pluggable `VendorDirectoryAdapter` interfaces are provided for connecting certified directory APIs.
3. **Calendar Export**:
   - `.ics` calendar file export and automated SMS/email reminders are planned for upcoming iterations.

---

## License

This project is licensed under the [MIT License](LICENSE).
