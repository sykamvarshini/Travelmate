import re

with open('frontend/src/pages/Itinerary.jsx', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add imports
content = content.replace("import { tripService } from '../services/api';", "import { tripService } from '../services/api';\nimport { weatherService, mapService } from '../services/externalApis';")

# 2. Add new state variables
state_vars = """
  const [loading, setLoading] = useState(true);
  const [routePolyline, setRoutePolyline] = useState(null);
"""
content = re.sub(r'const \[loading, setLoading\] = useState\(true\);', state_vars, content)

# 3. Add fetching logic to useEffect
fetch_logic_old = r'// Try to find first coordinates for map center(.*?)data\.destinationCoords = coords \|\| \[15\.2993, 74\.1240\]; // Default to Goa if none found\n        \}'
fetch_logic_new = """// Try to find first coordinates for map center
            let coords = null;
            let allActivityCoords = [];
            
            for (const d of data.days) {
                const dayCoords = d.activities.filter(a => a.coordinates).map(a => a.coordinates);
                allActivityCoords = [...allActivityCoords, ...dayCoords];
                
                if (!coords && dayCoords.length > 0) {
                    coords = dayCoords[0];
                }
            }
            data.destinationCoords = coords || [15.2993, 74.1240];
            
            // 1. Fetch Real Weather
            if (coords) {
                try {
                    const weatherData = await weatherService.getWeatherForecast(coords[0], coords[1]);
                    if (weatherData && weatherData.daily) {
                        data.days = data.days.map((day, idx) => {
                            // Map weather up to 7 days
                            const wIdx = Math.min(idx, weatherData.daily.time.length - 1);
                            const temp = weatherData.daily.temperature_2m_max[wIdx];
                            const code = weatherData.daily.weathercode[wIdx];
                            const rainProb = weatherData.daily.precipitation_probability_max[wIdx];
                            
                            let condition = "Clear";
                            if (code >= 1 && code <= 3) condition = "Cloudy";
                            if (code >= 51 && code <= 67) condition = "Rainy";
                            if (code >= 71) condition = "Snowy";
                            if (code >= 95) condition = "Thunderstorm";
                            
                            const isRainy = condition === "Rainy" || condition === "Thunderstorm";
                            
                            return {
                                ...day,
                                weather: `${condition}, ${temp}°C`,
                                hasWeatherAlert: isRainy || rainProb > 50,
                                weatherAlert: (isRainy || rainProb > 50) ? `High chance of rain (${rainProb}%). Consider indoor activities.` : null
                            };
                        });
                    }
                } catch (e) {
                    console.error("Weather fetch failed", e);
                }
            }
            
            // 2. Fetch Real Routes
            if (allActivityCoords.length > 1) {
                try {
                    const route = await mapService.getRoute(allActivityCoords);
                    if (route) {
                        setRoutePolyline(route);
                    }
                } catch (e) {
                    console.error("Route fetch failed", e);
                }
            }
        }"""
content = re.sub(fetch_logic_old, fetch_logic_new, content, flags=re.DOTALL)

# 4. Use routePolyline instead of allCoords in Polyline
render_old = r'<Polyline \n.*?positions=\{allCoords\}.*?/>'
render_new = """{routePolyline ? (
                    <Polyline positions={routePolyline} color="var(--accent-primary)" weight={4} opacity={0.8} />
                  ) : (
                    <Polyline positions={allCoords} color="var(--accent-primary)" weight={3} opacity={0.6} dashArray="5, 10" />
                  )}"""
content = re.sub(render_old, render_new, content, flags=re.DOTALL)

with open('frontend/src/pages/Itinerary.jsx', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated Itinerary.jsx successfully")
