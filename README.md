# BracketWise

BracketWise is an educational Ontario household savings scenario planner. It compares a user-defined allocation with a tax-aware modeled allocation across TFSA, RRSP, and FHSA accounts, then shows the estimated tax savings and five-year projection under simplified 2026 assumptions.

## Current phase status

The project has moved past the domain and API work and is now in the launch-prep phase.

- Complete: verified tax configuration, room validation, allocation rules, explanation reconciliation, household calculation engine, REST API, and a working React UI for the core scenario flow.
- In progress: operational hardening, local containerization, launch documentation, and smoke checks.
- Planned: deployment, cold-start measurement, final README polish, and public-facing demo assets.

## Stack

- Backend: Java 17, Spring Boot 3, Maven
- Frontend: React 19, TypeScript, Vite
- Containerization: Docker + Docker Compose

## Run locally

### Option 1: local dev servers

1. Start the backend:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
2. Start the frontend:
   ```bash
   cd frontend
   npm install
   npm run dev -- --host 0.0.0.0
   ```
3. Open http://localhost:5173

The frontend dev server proxies `/api` requests to the backend at `http://localhost:8080`.

### Option 2: Docker Compose

```bash
cd bracket-wise
docker compose up --build
```

Then open:

- Frontend: http://localhost:4173
- Backend: http://localhost:8080/api/v1/health

## Project structure

- `backend/`: Spring Boot API and domain logic
- `frontend/`: React app and static assets
- `PRD_BracketWise.md`: product requirements and implementation phase plan

## Notes

- The app is intentionally stateless and does not persist user data by default.
- All savings and tax outputs are educational estimates only, not CRA filings or tax advice.
- The disclaimer text shown in the product is included in the PRD and must remain consistent with the UI copy.

## Launch checklist

- Local Docker build works
- Frontend can reach the API through the default app flow
- Health endpoint responds successfully
- README instructions are enough for a new contributor to run the app locally
- Future deployment work should measure cold-start time and document the result before public launch
