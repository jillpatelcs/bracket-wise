# BracketWise

BracketWise is an educational Ontario household savings scenario planner. It compares a user-defined allocation with a tax-aware modeled allocation across TFSA, RRSP, and FHSA accounts, then shows estimated tax savings and a five-year illustration under simplified 2026 assumptions.

## Current status

The calculation engine, REST API, and React workflow are implemented. The repository is being prepared for public launch on Vercel (frontend) and Render (backend). No database, authentication, or persistence is included.

## Stack

- Backend: Java 17, Spring Boot, Maven
- Frontend: React, TypeScript, Vite, npm
- Deployment: Vercel Hobby static hosting and Render free Docker web service

## Run locally

### Development servers

1. Start the backend from `backend/`:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
2. Start the frontend from `frontend/`:
   ```powershell
   npm ci
   npm run dev
   ```
3. Open http://localhost:5173. Vite proxies `/api` to the backend at `http://127.0.0.1:8080`.

On macOS/Linux, use `./mvnw spring-boot:run` for the backend.

### Optional Docker Compose

Docker Compose is only a convenience for local development; it is not required for either deployment.

```powershell
docker compose up --build
```

Then open http://localhost:4173. The API health endpoint is http://localhost:8080/api/v1/health.

## Deployment

Follow [DEPLOYMENT.md](./DEPLOYMENT.md) for Render and Vercel dashboard settings, environment variables, smoke tests, cold-start measurement, troubleshooting, and rollback steps.

## Checks

```powershell
cd backend
.\mvnw.cmd test
cd ..\frontend
npm ci
npm run lint
npx tsc -b --pretty false
npm test
npm run build
```

When Docker is installed, build and run the backend image with:

```powershell
docker build -t bracketwise-api -f backend/Dockerfile .
docker run --rm -p 8080:8080 -e PORT=8080 bracketwise-api
node scripts/smoke-test.mjs http://127.0.0.1:8080
```

The GitHub Actions workflow runs backend tests, frontend lint/type/tests/build, a backend Docker build and smoke test, and an integration smoke test through Vite's `/api` proxy.

## Project structure

- `backend/`: Spring Boot API, domain logic, Java 17 Docker image
- `frontend/`: React app, TypeScript, Vite, npm lockfile
- `scripts/`: deployment HTTP smoke test and cold-start measurement
- `PRD_BracketWise.md`: product requirements, assumptions, and phase plan

## Product and privacy notes

- The application is stateless and does not persist user data.
- It does not collect credentials or provide tax, legal, or investment advice.
- All savings and tax outputs are educational estimates only; confirm contribution room using CRA records.
- The in-app disclaimer must remain consistent with the approved wording in the PRD.
