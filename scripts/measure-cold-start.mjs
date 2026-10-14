import { performance } from 'node:perf_hooks';

const healthUrl = process.argv[2];
if (!healthUrl) {
  throw new Error('Usage: node scripts/measure-cold-start.mjs <health-endpoint-url>');
}

const start = performance.now();
const response = await fetch(healthUrl, { signal: AbortSignal.timeout(120_000) });
if (!response.ok) {
  throw new Error(`Health check returned HTTP ${response.status}.`);
}

const payload = await response.json();
if (payload.status !== 'UP') {
  throw new Error('Health check did not return status UP.');
}

const elapsedMs = Math.round(performance.now() - start);
console.log(`health_url=${healthUrl}`);
console.log(`cold_start_ms=${elapsedMs}`);
