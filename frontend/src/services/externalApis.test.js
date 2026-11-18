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

test('caches an unavailable backend forecast briefly instead of retrying for each itinerary date', async (t) => {
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
    return {
      ok: true,
      json: async () => ({
        isAvailable: false,
        isStale: false,
        forecastMessage: 'Live weather temporarily unavailable',
        daily: null
      })
    };
  };

  const first = await weatherService.getWeatherForecast(32.174, 77.231, '2026-11-17');
  const second = await weatherService.getWeatherForecast(32.174, 77.231, '2026-11-18');

  assert.equal(first.isAvailable, false);
  assert.equal(second.isAvailable, false);
  assert.equal(calls, 1);
});

test('preserves the backend stale flag and original forecast timestamp', async (t) => {
  const originalFetch = globalThis.fetch;
  const originalNow = Date.now;
  const testNow = originalNow();
  Date.now = () => testNow;
  t.after(() => {
    globalThis.fetch = originalFetch;
    Date.now = originalNow;
  });

  const forecastTimestamp = testNow - 7 * 60 * 60 * 1000;
  const forecast = {
    isAvailable: true,
    isStale: true,
    forecastTimestamp,
    daily: { time: ['2026-11-18'] }
  };
  globalThis.fetch = async () => ({ ok: true, json: async () => forecast });

  const result = await weatherService.getWeatherForecast(34.174, 79.231);

  assert.equal(result.isStale, true);
  assert.equal(result.forecastTimestamp, forecastTimestamp);
});
