export const weatherService = {
  getWeatherForecast: async (lat, lon) => {
    try {
      // Open-Meteo free API - 16-day forecast with precipitation probability max, precipitation sum, weather codes and current conditions
      const response = await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&daily=weather_code,weathercode,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum&current=temperature_2m,precipitation,weather_code&forecast_days=16&timezone=Asia/Kolkata`);
      if (!response.ok) {
        console.warn(`Open-Meteo API returned status: ${response.status}`);
        return null;
      }
      const data = await response.json();
      return data;
    } catch (error) {
      console.error("Failed to fetch weather", error);
      return null;
    }
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
