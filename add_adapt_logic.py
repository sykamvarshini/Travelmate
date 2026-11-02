import re

with open('frontend/src/pages/Itinerary.jsx', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add adapt state variables
state_vars = """
  const [loading, setLoading] = useState(true);
  const [adaptingDay, setAdaptingDay] = useState(null);
"""
content = re.sub(r'const \[loading, setLoading\] = useState\(true\);', state_vars, content)

# 2. Add handleAdaptDay function inside the component
adapt_func = """
  const handleAdaptDay = async (dayId) => {
    setAdaptingDay(dayId);
    try {
      const updatedTrip = await tripService.adaptDay(id, dayId, 'Rainy', 0.8, 25.0);
      
      // Remap updated trip data
      if (updatedTrip.days) {
          updatedTrip.days = updatedTrip.days.map(day => ({
              ...day,
              weather: day.weatherCondition ? `${day.weatherCondition}, ${day.maxTempC || 30}°C` : "Sunny, 30°C",
              theme: "AI Generated Day",
              activities: day.items ? day.items.map(item => ({
                  id: item.id,
                  time: item.startTime,
                  type: item.category ? item.category.toLowerCase() : "attraction",
                  title: item.title,
                  location: item.indoorOutdoor,
                  coordinates: item.latitude && item.longitude ? [item.latitude, item.longitude] : null,
                  description: item.recommendationReason || `${item.category} activity`,
                  cost: `₹${item.costInr || 0}`,
                  duration: `${(item.durationMinutes || 60) / 60} hours`,
                  status: item.status
              })) : []
          }));
      }
      setTrip(updatedTrip);
      
    } catch (e) {
      console.error("Failed to adapt day", e);
    } finally {
      setAdaptingDay(null);
    }
  };

  if (loading) {
"""
content = re.sub(r'if \(loading\) \{', adapt_func, content)

# 3. Add Adapt button to the UI
render_old = r'\{day\.weatherAlert\}\n\s*</div>\n\s*\)\}'
render_new = """{day.weatherAlert}
                      <button 
                        className="adapt-btn" 
                        onClick={() => handleAdaptDay(day.id)}
                        disabled={adaptingDay === day.id}
                        style={{ marginLeft: 'auto', background: 'white', color: 'var(--danger-color)', border: 'none', padding: '4px 12px', borderRadius: '20px', cursor: 'pointer', fontSize: '12px', fontWeight: 'bold' }}
                      >
                        {adaptingDay === day.id ? 'Adapting...' : 'Adapt Schedule'}
                      </button>
                    </div>
                  )}"""
content = re.sub(render_old, render_new, content)

# 4. Add strike-through class for replaced items
activity_class_old = r'className=\{\`timeline-item type-\$\{activity\.type\}\`\}'
activity_class_new = 'className={`timeline-item type-${activity.type} ${activity.status === "REPLACED" ? "replaced-item" : ""}`}'
content = re.sub(activity_class_old, activity_class_new, content)

with open('frontend/src/pages/Itinerary.jsx', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated Itinerary.jsx")
