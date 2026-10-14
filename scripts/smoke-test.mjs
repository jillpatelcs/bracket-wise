import assert from 'node:assert/strict';
import { setTimeout as delay } from 'node:timers/promises';

const baseUrl = (process.argv[2] ?? 'http://127.0.0.1:8080').replace(/\/+$/, '');
const apiBaseUrl = (process.env.SMOKE_TEST_API_BASE_URL ?? baseUrl).replace(/\/+$/, '');
const checkFrontend = process.argv.includes('--frontend');
const corsOrigin = process.env.SMOKE_TEST_CORS_ORIGIN;
const fetchJson = (url, options = {}, timeoutMs = 20_000) =>
  fetch(url, { ...options, signal: AbortSignal.timeout(timeoutMs) });

const waitFor = async (url, validate, description) => {
  const deadline = Date.now() + 120_000;
  let lastError;
  while (Date.now() < deadline) {
    try {
      const response = await fetchJson(url, {}, Math.min(20_000, deadline - Date.now()));
      if (validate(response)) {
        return response;
      }
      lastError = new Error(`${description} returned HTTP ${response.status}.`);
      await response.body?.cancel();
    } catch (error) {
      lastError = error;
    }
    await delay(Math.min(2_000, Math.max(0, deadline - Date.now())));
  }
  throw new Error(`Timed out waiting for ${description}: ${lastError?.message ?? 'no response'}`);
};

if (checkFrontend) {
  const page = await waitFor(baseUrl, (response) => response.status === 200, 'frontend root');
  assert.match(await page.text(), /<title>BracketWise<\/title>/, 'frontend should serve the BracketWise app');
  console.log('PASS frontend root');
}

const healthResponse = await waitFor(
  `${apiBaseUrl}/api/v1/health`,
  (response) => response.status === 200,
  'health endpoint',
);
assert.equal((await healthResponse.json()).status, 'UP', 'health endpoint should report UP');
console.log('PASS GET /api/v1/health');

if (corsOrigin) {
  const allowedCorsResponse = await fetchJson(`${apiBaseUrl}/api/v1/calculate`, {
    method: 'OPTIONS',
    headers: {
      Origin: corsOrigin,
      'Access-Control-Request-Method': 'POST',
      'Access-Control-Request-Headers': 'content-type',
    },
  });
  assert.equal(allowedCorsResponse.status, 200, 'configured CORS origin should pass preflight');
  assert.equal(
    allowedCorsResponse.headers.get('access-control-allow-origin'),
    corsOrigin,
    'CORS should return the exact configured origin',
  );
  assert.match(
    allowedCorsResponse.headers.get('access-control-allow-methods') ?? '',
    /POST/i,
    'CORS should allow the calculation POST method',
  );
  assert.match(
    allowedCorsResponse.headers.get('access-control-allow-headers') ?? '',
    /content-type/i,
    'CORS should allow the JSON content-type header',
  );

  const deniedCorsResponse = await fetchJson(`${apiBaseUrl}/api/v1/calculate`, {
    method: 'OPTIONS',
    headers: {
      Origin: 'https://untrusted.invalid',
      'Access-Control-Request-Method': 'POST',
      'Access-Control-Request-Headers': 'content-type',
    },
  });
  assert.equal(deniedCorsResponse.status, 403, 'unconfigured CORS origin should be rejected');
  assert.equal(deniedCorsResponse.headers.get('access-control-allow-origin'), null);
  console.log('PASS exact CORS allowlist');
}

const validRequest = {
  mode: 'SINGLE',
  province: 'ON',
  taxYear: 2026,
  monthlySavings: 1000,
  assumptions: { expectedAnnualReturn: 0.05, rrspWithdrawalTaxRate: 0.2 },
  persons: [{
    id: 'A',
    canadianTaxResidentForFullYear: true,
    estimatedEmploymentIncomeFor2026: 54000,
    availableTfsaRoom: 12000,
    availableRrspDeductionLimit: 0,
    fhsaEligible: false,
    availableFhsaParticipationRoom: 0,
  }],
  userAllocation: [{ person: 'A', account: 'TFSA', percent: 100 }],
};

const validResponse = await fetchJson(`${apiBaseUrl}/api/v1/calculate`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(validRequest),
});
assert.equal(validResponse.status, 200, 'valid calculation should return HTTP 200');
const calculation = await validResponse.json();
assert.ok(calculation.configVersion, 'calculation should identify its configuration');
assert.ok(calculation.userAllocation, 'calculation should include the user-defined scenario');
assert.ok(calculation.modeledAllocation, 'calculation should include the modeled scenario');
console.log('PASS valid POST /api/v1/calculate');

const invalidResponse = await fetchJson(`${apiBaseUrl}/api/v1/calculate`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ ...validRequest, monthlySavings: -1 }),
});
assert.equal(invalidResponse.status, 400, 'invalid input should return HTTP 400');
assert.equal((await invalidResponse.json()).code, 'NEGATIVE_VALUE', 'invalid input should return a stable error code');
console.log('PASS invalid-input handling');
