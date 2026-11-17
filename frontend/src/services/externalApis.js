const WEATHER_CACHE_TTL_MS = 6 * 60 * 60 * 1000;
const MAX_STALE_WEATHER_AGE_MS = 24 * 60 * 60 * 1000;
const WEATHER_REQUEST_INTERVAL_MS = 1000;
const weatherCache = new Map();
const weatherDateCache = new Map();
const weatherRequests = new Map();
let weatherRateLimitUntil = 0;
let nextWeatherRequestAt = 0;

const normalizeCoordinate = (value) => Math.round(Number(value) * 100) / 100;
const weatherCoordinateKey = (lat, lon) => `${normalizeCoordinate(lat).toFixed(2)}_${normalizeCoordinate(lon).toFixed(2)}`;

const getCachedWeather = (entry, allowExpired = false) => {
  const age = entry ? Date.now() - entry.timestamp : Infinity;
  if (!entry || (allowExpired ? age >= MAX_STALE_WEATHER_AGE_MS : age >= WEATHER_CACHE_TTL_MS)) return null;
  return entry.data;
};

const getRateLimitCooldownMs = (response) => {
  const retryAfter = response.headers?.get?.('Retry-After');
  let retryAfterMs = retryAfter ? Number(retryAfter) * 1000 : NaN;
  if (retryAfter && Number.isNaN(retryAfterMs)) {
    retryAfterMs = Date.parse(retryAfter) - Date.now();
  }
  if (Number.isFinite(retryAfterMs)) {
    return Math.max(60_000, Math.min(retryAfterMs, 24 * 60 * 60 * 1000));
  }

  const indiaOffsetMs = 330 * 60 * 1000;
  const indiaNow = new Date(Date.now() + indiaOffsetMs);
  const nextIndiaMidnight = Date.UTC(
    indiaNow.getUTCFullYear(),
    indiaNow.getUTCMonth(),
    indiaNow.getUTCDate() + 1
  ) - indiaOffsetMs;
  return Math.max(60_000, Math.min(nextIndiaMidnight - Date.now(), 24 * 60 * 60 * 1000));
};

const temporaryWeatherFallback = (entry) => {
  const cached = getCachedWeather(entry, true);
  return cached ? { ...cached, forecastMessage: 'Live weather temporarily unavailable — showing cached forecast.' } : null;
};

export const weatherService = {
  getWeatherForecast: async (lat, lon, targetDate = null) => {
    const coordinateKey = weatherCoordinateKey(lat, lon);
    const dateCacheKey = `${coordinateKey}:${targetDate || 'forecast'}`;
    const dateCached = getCachedWeather(weatherDateCache.get(dateCacheKey));
    if (dateCached) return dateCached;

    const coordinateEntry = weatherCache.get(coordinateKey);
    const coordinateCached = getCachedWeather(coordinateEntry);
    if (coordinateCached) {
      weatherDateCache.set(dateCacheKey, coordinateEntry);
      return coordinateCached;
    }

    if (Date.now() < weatherRateLimitUntil) {
      return temporaryWeatherFallback(coordinateEntry);
    }

    let request = weatherRequests.get(coordinateKey);
    if (!request) {
      if (Date.now() < nextWeatherRequestAt) {
        return temporaryWeatherFallback(coordinateEntry);
      }
      nextWeatherRequestAt = Date.now() + WEATHER_REQUEST_INTERVAL_MS;
      request = (async () => {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 7000);
        const normalizedLat = normalizeCoordinate(lat);
        const normalizedLon = normalizeCoordinate(lon);
        const url = `https://api.open-meteo.com/v1/forecast?latitude=${normalizedLat}&longitude=${normalizedLon}&daily=weather_code,weathercode,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum&current=temperature_2m,precipitation,weather_code&forecast_days=16&timezone=Asia/Kolkata`;

        try {
          const response = await fetch(url, { signal: controller.signal });
          if (!response.ok) {
            console.warn(`Open-Meteo API returned status: ${response.status}`);
            if (response.status === 429) {
              weatherRateLimitUntil = Date.now() + getRateLimitCooldownMs(response);
            } else if (response.status >= 500) {
              weatherRateLimitUntil = Date.now() + 60 * 1000;
            }
            return null;
          }

          const data = await response.json();
          const entry = { data, timestamp: Date.now() };
          weatherCache.set(coordinateKey, entry);
          weatherDateCache.set(dateCacheKey, entry);
          return data;
        } catch (error) {
          console.warn('Open-Meteo forecast request failed:', error);
          weatherRateLimitUntil = Date.now() + 60 * 1000;
          return null;
        } finally {
          clearTimeout(timeout);
          weatherRequests.delete(coordinateKey);
        }
      })();
      weatherRequests.set(coordinateKey, request);
    }

    const data = await request;
    if (data) {
      weatherDateCache.set(dateCacheKey, weatherCache.get(coordinateKey));
      return data;
    }
    return temporaryWeatherFallback(weatherCache.get(coordinateKey));
  }
};

export const mapService = {
  getRoute: async (coordsList) => {
    if (!coordsList || coordsList.length < 2) return null;
    try {
      // OSRM format is longitude,latitude
      const coordsString = coordsList.map(coord => `${coord[1]},${coord[0]}`).join(';');
      // Free public OSRM server for routing
      const response = await fetch(`https://router.project-osrm.org/route/v1/driving/${coordsString}?overview=full&geometries=geojson`);
      const data = await response.json();
      
      if (data.code === 'Ok' && data.routes && data.routes.length > 0) {
        // OSRM returns GeoJSON coordinates as [longitude, latitude], leaflet expects [latitude, longitude]
        return data.routes[0].geometry.coordinates.map(coord => [coord[1], coord[0]]);
      }
      return null;
    } catch (error) {
      console.error("Failed to fetch route", error);
      return null;
    }
  }
};
