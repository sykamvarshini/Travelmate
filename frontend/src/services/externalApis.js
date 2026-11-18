const WEATHER_CACHE_TTL_MS = 6 * 60 * 60 * 1000;
const MAX_STALE_WEATHER_AGE_MS = 24 * 60 * 60 * 1000;
const UNAVAILABLE_WEATHER_CACHE_TTL_MS = 30 * 1000;
const weatherCache = new Map();
const weatherRequests = new Map();
const WEATHER_API_BASE_URL = import.meta.env?.VITE_API_BASE_URL || 'http://localhost:8080/api';

const normalizeCoordinate = (value) => Math.round(Number(value) * 100) / 100;
const weatherCoordinateKey = (lat, lon) => `${normalizeCoordinate(lat).toFixed(2)}_${normalizeCoordinate(lon).toFixed(2)}`;

const getCachedWeather = (entry, allowExpired = false) => {
  const forecastTimestamp = entry?.data?.forecastTimestamp ?? entry?.timestamp;
  const age = entry ? Date.now() - forecastTimestamp : Infinity;
  const isUnavailable = entry?.data?.isAvailable === false;
  if (isUnavailable && allowExpired) return null;
  const ttl = isUnavailable ? UNAVAILABLE_WEATHER_CACHE_TTL_MS : WEATHER_CACHE_TTL_MS;
  if (!entry || (allowExpired ? age >= MAX_STALE_WEATHER_AGE_MS : age >= ttl)) return null;
  return entry.data;
};

const temporaryWeatherFallback = (entry) => {
  const cached = getCachedWeather(entry, true);
  return cached ? {
    ...cached,
    isStale: true,
    forecastMessage: 'Live weather temporarily unavailable — showing cached forecast.'
  } : null;
};

export const weatherService = {
  getWeatherForecast: async (lat, lon) => {
    const coordinateKey = weatherCoordinateKey(lat, lon);
    const coordinateEntry = weatherCache.get(coordinateKey);
    const coordinateCached = getCachedWeather(coordinateEntry);
    if (coordinateCached) return coordinateCached;

    let request = weatherRequests.get(coordinateKey);
    if (!request) {
      request = (async () => {
        const normalizedLat = normalizeCoordinate(lat);
        const normalizedLon = normalizeCoordinate(lon);
        const params = new URLSearchParams({ lat: String(normalizedLat), lon: String(normalizedLon) });

        try {
          const response = await fetch(`${WEATHER_API_BASE_URL}/weather/forecast?${params}`);
          if (!response.ok) throw new Error(`Weather API returned status ${response.status}`);
          const data = await response.json();
          if (data?.isAvailable === false || data?.daily) {
            weatherCache.set(coordinateKey, { data, timestamp: Date.now() });
          }
          return data;
        } catch (error) {
          console.warn('Weather forecast request failed:', error);
          return temporaryWeatherFallback(weatherCache.get(coordinateKey));
        } finally {
          weatherRequests.delete(coordinateKey);
        }
      })();
      weatherRequests.set(coordinateKey, request);
    }

    const data = await request;
    return data || temporaryWeatherFallback(weatherCache.get(coordinateKey));
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
