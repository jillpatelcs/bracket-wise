# Deployment runbook

This release uses Vercel Hobby for the static React frontend and Render's free web-service plan for the Java API. The service is stateless: do not create a database, authentication service, persistence endpoint, paid add-on, or keep-awake job for this release. No cloud resources are created by this repository's CI.

Provider setup was checked against the official [Render Docker](https://render.com/docs/docker), [Render web service](https://render.com/docs/web-services), [Render health check](https://render.com/docs/health-checks), [Vercel Vite](https://vercel.com/docs/frameworks/frontend/vite), and [Vercel environment](https://vercel.com/docs/deployments/environments) documentation on 2026-10-13.

## Backend: Render

Create a **Web Service** connected to this GitHub repository:

| Render setting | Value |
|---|---|
| Name | `bracketwise-api` or another available service name |
| Branch | The release branch (normally `main`) |
| Root Directory | Repository root (leave the field blank/default) |
| Runtime / Language | `Docker` |
| Dockerfile Path | `backend/Dockerfile` |
| Docker build context | Repository root (`.`) |
| Instance type | `Free` |
| Health Check Path | `/api/v1/health` |
| Docker Command | Leave blank; use the Dockerfile `ENTRYPOINT` |
| Database / disk | None |

Keep Render's root directory at the repository root so the Docker build context includes the monorepo's root `.dockerignore`. The multi-stage Docker build copies only `backend/pom.xml` and `backend/src`, uses Maven 3.9.9 and Java 17 to package the backend, then runs the fixed application JAR on a Java 17 JRE as UID/GID `10001`. The server binds to `0.0.0.0` and listens on Render's `PORT`; without that variable it falls back to port `8080`.

### Render environment

`CORS_ORIGINS` is optional during the initial Render bootstrap. If omitted, the backend uses its explicit localhost development allowlist; it never falls back to `*`. Health checks and direct API requests remain available, but browsers on Vercel cannot call the API until the exact deployed production Vercel origin is added. Set it as soon as that URL is known:

| Variable | Value |
|---|---|
| `CORS_ORIGINS` | `https://<your-production-vercel-domain>`; optionally comma-separate an exact production custom domain |
| `JAVA_TOOL_OPTIONS` | Optional override. The Docker image supplies `-XX:InitialRAMPercentage=20.0 -XX:MaxRAMPercentage=65.0 -XX:+ExitOnOutOfMemoryError` by default. Leave unset unless measurements justify a change. |
| `PORT` | Normally supplied by Render; do not set manually. If explicitly configured, it must match the web-service port Render expects. |

For example, after the Vercel domain is assigned, use `CORS_ORIGINS=https://<assigned-production-domain>.vercel.app`. Origins must be exact scheme + host + optional port values, with no path or trailing slash. Comma-separate additional exact origins only when required. Wildcards (including `*.vercel.app`) are rejected during startup.

The application has safe error responses (generic 500 details, no serialized stack trace), console/stdout logging, and request logs limited to request ID, path, status, and latency. It does not log calculation request bodies or household financial values. Unexpected server errors may include diagnostic stack traces in private Render logs; clients only receive a generic problem response and request ID.

## Frontend: Vercel

Import the same GitHub repository as a Vercel project:

| Vercel setting | Value |
|---|---|
| Plan | Hobby |
| Root Directory | `frontend` |
| Framework Preset | `Vite` (normally auto-detected) |
| Install Command | `npm ci` |
| Build Command | `npm run build` (`tsc -b && vite build`) |
| Output Directory | `dist` |
| Node.js | `24.x` |

No `vercel.json` rewrite is needed: the current UI has no client-side URL routes requiring SPA fallback. The Vite build output is a static site.
Node.js `24.x` is Vercel's current default and supported LTS. Its current supported-version list is `24.x` and `22.x`; Node 20 was deprecated on 2026-10-01. The locked Vite/Vitest toolchain supports Node 22+, so use `24.x` in Vercel and CI.

Set this **public build-time** variable under Vercel project environment variables:

| Variable | Value |
|---|---|
| `VITE_API_BASE_URL` | `https://<your-render-service>.onrender.com` (origin only, no `/api` suffix) |

Set it for **Production** before the production build. Vite embeds `VITE_*` values in public JavaScript; never put credentials, secrets, or private tokens in them. Redeploy after changing this variable.

### Production versus Preview origins

The production Vercel URL is the only frontend origin to add to Render's `CORS_ORIGINS` by default. Preview deployments have distinct URLs; they are not implicitly trusted. A preview can render its UI, but browser API calls from that URL will be blocked unless you intentionally add that exact, stable preview origin to the Render allowlist and configure its Vercel Preview `VITE_API_BASE_URL`. Never allow arbitrary Vercel preview domains with a wildcard. Remove temporary preview origins when they are no longer needed.

If production uses both the assigned `*.vercel.app` domain and a custom domain, list each exact origin on Render if users will use both.

## Deployment sequence

1. Push the reviewed code to the intended GitHub branch. Confirm the GitHub Actions workflow is green. CI tests and builds; it does not deploy or create cloud resources.
2. In Render, create the backend Web Service with the settings above. Leave `CORS_ORIGINS` unset for bootstrap (safe localhost-only default), and do not configure a database. Wait for the first deploy to become healthy. At this point direct health/API checks work, while browser requests from Vercel are expected to fail CORS.
3. In the Render service, open **Settings → Health Checks** and set `/api/v1/health`. Save and verify `https://<render-service>.onrender.com/api/v1/health` returns `{"status":"UP"}`.
4. In Vercel, import the repository, set the project root to `frontend`, Node `24.x`, and the Vite build settings. Set Production `VITE_API_BASE_URL` to the assigned Render service origin, then deploy. Changing this variable later requires a new Vercel deployment/build.
5. Copy the assigned production Vercel origin (scheme + hostname only) into Render's `CORS_ORIGINS`. Add an exact custom production origin only if one is configured. Saving this variable requires the Render service to redeploy/restart; wait for it to become healthy.
6. Run the public smoke tests below against both assigned production URLs. Check the browser flow from the production Vercel site. Record measured cold-start results only after the real Render service can idle and wake.
7. Optionally test Vercel Preview deployment appearance. Do not expect its API calls to work until its exact origin is intentionally allowed on Render.

Use placeholders above until the providers assign real URLs; do not guess them.

## Smoke tests

With Node.js 24, run these against the actual public URLs:

```powershell
node scripts/smoke-test.mjs 'https://<your-render-service>.onrender.com'
$env:SMOKE_TEST_API_BASE_URL = 'https://<your-render-service>.onrender.com'
$env:SMOKE_TEST_CORS_ORIGIN = 'https://<your-production-vercel-domain>.vercel.app'
node scripts/smoke-test.mjs 'https://<your-production-vercel-domain>.vercel.app' --frontend
$env:SMOKE_TEST_API_BASE_URL = $null
$env:SMOKE_TEST_CORS_ORIGIN = $null
```

The script checks the frontend shell (when `--frontend` is supplied), `GET /api/v1/health`, a valid sample calculation, and a negative-input `400` response with the stable `NEGATIVE_VALUE` code. `SMOKE_TEST_API_BASE_URL` separates the Vercel site root from the Render API origin. When `SMOKE_TEST_CORS_ORIGIN` is set, it sends the browser-equivalent JSON preflight (`POST` with `content-type`) and verifies the exact allowed origin, method, and header, then rejects an unconfigured origin. It waits up to two minutes for the health endpoint to become available, accommodating a free-service wake-up. Do not paste real financial inputs into terminal commands or issue trackers.

The GitHub Actions job's container smoke test and CORS preflight run directly against the production Docker image. Its separate Vite-proxy check validates local integration only; it is not production cross-origin verification.

Also open the production site in a browser and:

1. Run the example household and confirm both allocation scenarios and explanations render.
2. Confirm the request succeeds from the production Vercel origin and that the browser console has no CORS error.
3. Submit an invalid value and confirm the API returns a validation message without a retry loop.
4. Confirm `GET /api/v1/health` works directly at the public Render URL.

## Cold-start measurement

Do not record an estimate or a CI timing as a production cold-start result. Once deployed:

1. Wait until Render indicates the free service has spun down after inactivity. Do not send health checks or other requests while waiting.
2. From a machine with Node.js 24, make exactly one timed request to the health path:
   ```powershell
   node scripts/measure-cold-start.mjs 'https://<your-render-service>.onrender.com/api/v1/health'
   ```
3. Record the actual `cold_start_ms` output, measurement date/time, service region, and any first-request failures in the launch notes. Repeat only if you want separate measurements; state how many runs and report each result rather than selecting a favorable value.

**Cold-start measurement:** Not yet measured; deployment and public URLs are not configured.

The frontend displays a wake-up notice and makes at most three calculation attempts. Each attempt is bounded by a 25-second timeout, with 750 ms and 1.5 s waits between attempts. Only network/timeouts and gateway/service-starting responses (`502`, `503`, `504`) are retried. Validation and other client errors are not retried. A user can retry after a final error.

## Troubleshooting

| Symptom | Check |
|---|---|
| Render build fails | Confirm the root directory is the repository root, Dockerfile path is `backend/Dockerfile`, and the Docker language/runtime is selected. |
| Render deploy never becomes healthy | Check Render logs for startup/configuration errors. Confirm `PORT` is not overridden incorrectly, the container binds `0.0.0.0`, and the health path is `/api/v1/health`. |
| App exits during startup after a CORS change | `CORS_ORIGINS` must contain exact HTTP(S) origins without wildcards, paths, query strings, or fragments. Use commas between origins. |
| Browser reports a CORS or network error | Confirm Vercel's production `VITE_API_BASE_URL` points to the Render origin, and Render `CORS_ORIGINS` includes the exact browser origin including scheme and any port. Redeploy/rebuild after variable changes. |
| First API request takes a long time or gets a 502/503/504 | The free backend may be waking. The UI retries bounded transient failures; wait for the message to finish, then try once more if needed. |
| Calculation returns 400 | This is input validation, not a cold-start condition. Read the field/code detail and correct the input; it is deliberately not retried. |
| Unexpected 500 | Give the displayed request ID to the maintainer and check private Render logs. The API does not return stack traces or request bodies to the browser. |

## Rollback

- **Vercel:** open the project deployment list, select the last known-good Production deployment, and use Vercel's promote/rollback action to restore it. Check the Production `VITE_API_BASE_URL` setting before promoting.
- **Render:** open the service's Deploys list and roll back to the last known-good deploy. If the problem is only a CORS allowlist change, restore the previous exact `CORS_ORIGINS` value and redeploy.
- Verify the frontend root, backend health, valid calculation, and invalid-input response after rollback.

## Local Docker Compose

`docker compose up --build` remains available for local convenience only. Render builds the backend from the repository-root context using `backend/Dockerfile`, and Vercel builds the frontend from `frontend/`; neither provider requires Compose.
