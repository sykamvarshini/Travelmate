import os

itinerary_path = r'c:\Users\HP\OneDrive\Desktop\travel planning\frontend\src\pages\Itinerary.jsx'

with open(itinerary_path, 'r', encoding='utf-8') as f:
    code = f.read()

# 1. in getNextActivity
code = code.replace(
    'const allActivities = trip.days.flatMap(d => d.activities.filter(a => a.status !== "REPLACED" && a.status !== "CANCELLED"));',
    'const allActivities = (trip?.days || []).flatMap(d => (d.activities || []).filter(a => a.status !== "REPLACED" && a.status !== "CANCELLED"));'
)

# 2. in lastCompleted
code = code.replace(
    "const lastCompleted = trip?.days?.flatMap(d => d.activities).filter(a => a.status === 'COMPLETED').slice(-1)[0];",
    "const lastCompleted = trip?.days?.flatMap(d => (d.activities || [])).filter(a => a.status === 'COMPLETED').slice(-1)[0];"
)

# 3. in allActiveActivities
code = code.replace(
    'const allActiveActivities = trip.days \n    ? trip.days.flatMap(day => day.activities.filter(a => a.status !== "REPLACED" && a.status !== "CANCELLED"))\n    : [];',
    'const allActiveActivities = trip?.days \n    ? trip.days.flatMap(day => (day.activities || []).filter(a => a.status !== "REPLACED" && a.status !== "CANCELLED"))\n    : [];'
)

# 4. in calculateBudgetBreakdown
code = code.replace(
    'const activitiesCost = trip.days \n      ? trip.days.flatMap(d => d.activities).filter(a => a.status !== "REPLACED").reduce((sum, a) => sum + (a.costNumeric || 0), 0) * numTravelers',
    'const activitiesCost = trip?.days \n      ? trip.days.flatMap(d => (d.activities || [])).filter(a => a.status !== "REPLACED").reduce((sum, a) => sum + (a.costNumeric || 0), 0) * numTravelers'
)

# 5. in JSX map
code = code.replace(
    '{day.activities.map((activity, actIndex) => (',
    '{(day.activities || []).map((activity, actIndex) => ('
)

# 6. in loadTripData activity mapping fallback
code = code.replace(
    'activities: day.items ? day.items.map((item, actIdx) => {',
    'activities: (day.items && day.items.length > 0) ? day.items.map((item, actIdx) => {'
)

# 7. in transit distance calculation
code = code.replace(
    'for (let i = 0; i < day.activities.length; i++) {',
    'const acts = day.activities || [];\n          for (let i = 0; i < acts.length; i++) {'
)

code = code.replace(
    'const act = day.activities[i];',
    'const act = acts[i];'
)

code = code.replace(
    'const prev = day.activities[i - 1].coordinates;',
    'const prev = acts[i - 1]?.coordinates;'
)

code = code.replace(
    'act.transitInfo = `Local Cab / Auto (${day.activities[i - 1].title} → ${act.title})`;',
    'act.transitInfo = `Local Cab / Auto (${acts[i - 1]?.title || "Previous Stop"} → ${act.title})`;'
)

with open(itinerary_path, 'w', encoding='utf-8') as f:
    f.write(code)

print("Applied null safety patches successfully!")
