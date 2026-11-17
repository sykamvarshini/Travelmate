import assert from 'node:assert/strict';
import test from 'node:test';
import { weatherService } from './externalApis.js';

test('reuses a coordinate forecast for multiple forecast dates', async (t) => {
  const originalFetch = globalThis.fetch;
  const originalNow = Date.now;
  const testNow = originalNow();
  Date.now = () => testNow;
  t.after(() => {
    globalThis.fetch = originalFetch;
    Date.now = originalNow;
  });

  let calls = 0;
  const forecast = { daily: { time: ['2026-11-17', '2026-11-18'] } };
  globalThis.fetch = async () => {
    calls += 1;
    return { ok: true, json: async () => forecast };
  };

  const today = await weatherService.getWeatherForecast(30.174, 75.231, '2026-11-17');
  const tomorrow = await weatherService.getWeatherForecast(30.173, 75.229, '2026-11-18');

  assert.equal(calls, 1);
  assert.strictEqual(today, forecast);
  assert.strictEqual(tomorrow, forecast);
});

test('deduplicates simultaneous requests for normalized coordinates', async (t) => {
  const originalFetch = globalThis.fetch;
  const originalNow = Date.now;
  const testNow = originalNow() + 2000;
  Date.now = () => testNow;
  t.after(() => {
    globalThis.fetch = originalFetch;
    Date.now = originalNow;
  });

  let calls = 0;
  let resolveResponse;
  globalThis.fetch = () => {
    calls += 1;
    return new Promise((resolve) => {
      resolveResponse = resolve;
    });
  };

  const first = weatherService.getWeatherForecast(31.284, 76.412, '2026-11-17');
  const second = weatherService.getWeatherForecast(31.283, 76.414, '2026-11-18');

  assert.equal(calls, 1);
  resolveResponse({ ok: true, json: async () => ({ daily: { time: [] } }) });
  assert.deepEqual(await Promise.all([first, second]), [
    { daily: { time: [] } },
    { daily: { time: [] } }
  ]);
});

test('does not retry a rate-limited weather request during cooldown', async (t) => {
  const originalFetch = globalThis.fetch;
  const originalNow = Date.now;
  const testNow = originalNow() + 3000;
  Date.now = () => testNow;
  t.after(() => {
    globalThis.fetch = originalFetch;
    Date.now = originalNow;
  });

  let calls = 0;
  globalThis.fetch = async () => {
    calls += 1;
    return { ok: false, status: 429, headers: { get: () => null } };
  };

  const first = await weatherService.getWeatherForecast(32.174, 77.231, '2026-11-17');
  const second = await weatherService.getWeatherForecast(32.174, 77.231, '2026-11-18');

  assert.equal(first, null);
  assert.equal(second, null);
  assert.equal(calls, 1);
});
