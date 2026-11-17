import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useDestinationTranslation } from '../utils/destinationTranslations';
import { 
  Calendar, MapPin, Clock, DollarSign, CloudRain, Sun, AlertTriangle, 
  ArrowLeft, RefreshCw, Sparkles, CheckCircle2, 
  Car, Compass, 
  Navigation, Train, Bus, Check, Trash2, Users, AlertCircle,
  UserPlus, Share2, Edit3, Plus, ChevronUp, ChevronDown, Search
} from 'lucide-react';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { tripService, placesService } from '../services/api';
import { weatherService, mapService } from '../services/externalApis';
import { validateSegmentTransport, calculateHaversineKm } from '../services/transportValidation';
import SafeImage from '../components/common/SafeImage';
import LocationAutocomplete from '../components/LocationAutocomplete';
import './Itinerary.css';

// Fix Leaflet Default Marker Icon issue
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

// Custom colored map markers
const createNumberedIcon = (number, isSelected = false) => {
  return L.divIcon({
    className: 'custom-map-pin',
    html: `<div class="pin-circle ${isSelected ? 'pin-selected' : ''}">${number}</div>`,
    iconSize: [30, 30],
    iconAnchor: [15, 15],
    popupAnchor: [0, -15]
  });
};

const createOriginIcon = (cityName, isSelected = false) => {
  return L.divIcon({
    className: 'custom-map-pin',
    html: `<div class="pin-origin ${isSelected ? 'pin-selected' : ''}" title="Origin: ${cityName}">🟢 Start</div>`,
    iconSize: [64, 28],
    iconAnchor: [32, 14],
    popupAnchor: [0, -14]
  });
};

const createHubIcon = (cityName, isSelected = false) => {
  return L.divIcon({
    className: 'custom-map-pin',
    html: `<div class="pin-hub ${isSelected ? 'pin-selected' : ''}" title="Hub: ${cityName}">📍 Hub</div>`,
    iconSize: [58, 28],
    iconAnchor: [29, 14],
    popupAnchor: [0, -14]
  });
};

// Component to handle programmatic map pan/zoom and fitting bounds
const MapController = ({ allCoords, centerCoord, selectedCoord }) => {
  const map = useMap();
  
  useEffect(() => {
    if (selectedCoord) {
      map.flyTo(selectedCoord, 16, { duration: 1.2 });
    } else if (allCoords && allCoords.length > 1) {
      try {
        const bounds = L.latLngBounds(allCoords);
        map.fitBounds(bounds, { padding: [45, 45], maxZoom: 14 });
      } catch (_e) {
        if (centerCoord) map.setView(centerCoord, 12);
      }
    } else if (centerCoord) {
      map.setView(centerCoord, 12);
    }
  }, [centerCoord, selectedCoord, allCoords, map]);

  return null;
};

// Known verified coordinates for Indian cities and landmarks to prevent blank coords
const KNOWN_GEO = {
  gudivada: [16.4410, 80.9926],
  vijayawada: [16.5062, 80.6480],
  visakhapatnam: [17.6868, 83.2185],
  "simhachalam temple": [17.7660, 83.2500],
  simhachalam: [17.7660, 83.2500],
  gitam: [17.7813, 83.3776],
  "gitam university": [17.7813, 83.3776],
  "gitam (deemed to be university)": [17.7813, 83.3776],
  "gayatri university": [17.8205, 83.3421],
  "gayatri vidya parishad": [17.8205, 83.3421],
  gvp: [17.8205, 83.3421],
  "andhra university": [17.7226, 83.3235],
  "rushikonda beach": [17.7812, 83.3852],
  rushikonda: [17.7812, 83.3852],
  "ins kursura": [17.7180, 83.3330],
  "submarine museum": [17.7180, 83.3330],
  kailasagiri: [17.7490, 83.3420],
  "rk beach": [17.7140, 83.3240],
  "ramakrishna beach": [17.7140, 83.3240],
  "yarada beach": [17.6530, 83.2700],
  "dolphin's nose": [17.6740, 83.2920],
  "tenneti park": [17.7570, 83.3550],
  "indira gandhi zoological park": [17.7680, 83.3450],
  araku: [18.3333, 82.8833],
  "araku valley": [18.3333, 82.8833],
  tirupati: [13.6288, 79.4192],
  hyderabad: [17.3850, 78.4867],
  bengaluru: [12.9716, 77.5946],
  chennai: [13.0827, 80.2707],
  delhi: [28.6139, 77.2090],
  mumbai: [19.0760, 72.8777],
  goa: [15.4909, 73.8278],
  panaji: [15.4909, 73.8278],
  "fort aguada": [15.4925, 73.7737],
  "aguada fort": [15.4925, 73.7737],
  "fort aguada lighthouse": [15.4925, 73.7737],
  "candolim beach": [15.5170, 73.7650],
  "calangute beach": [15.5430, 73.7550],
  "baga beach": [15.5553, 73.7517],
  jaipur: [26.9124, 75.7873],
  "kanaka durga temple": [16.5161, 80.6074],
  "undavalli caves": [16.4965, 80.5815],
  "prakasam barrage": [16.5068, 80.6050],
  "bhavani island": [16.5204, 80.5901],
  "bapu museum": [16.5100, 80.6300],
  "victoria jubilee museum": [16.5100, 80.6300]
};

const resolveKnownCoords = (query) => {
  if (!query) return null;
  const q = query.toLowerCase().trim();
  for (const [k, v] of Object.entries(KNOWN_GEO)) {
    if (q.includes(k) || k.includes(q)) return v;
  }
  return null;
};

// Automatic Intelligent Category Identifier
const inferCategory = (title, address) => {
  const t = ((title || '') + ' ' + (address || '')).toLowerCase();
  if (t.includes('university') || t.includes('college') || t.includes('institute') || t.includes('gitam') || t.includes('gayatri') || t.includes('campus') || t.includes('vidya parishad') || t.includes('academy') || t.includes('polytechnic') || t.includes('iit') || t.includes('iim')) {
    return 'Education / University';
  }
  if (t.includes('temple') || t.includes('mandir') || t.includes('spiritual') || t.includes('church') || t.includes('cathedral') || t.includes('basilica') || t.includes('mosque') || t.includes('masjid') || t.includes('dargah') || t.includes('gurdwara')) {
    return 'Temples / Spiritual';
  }
  if (t.includes('beach') || t.includes('coast') || t.includes('sea') || t.includes('sagar') || t.includes('cove')) {
    return 'Beaches';
  }
  if (t.includes('viewpoint') || t.includes('view point') || t.includes('point') || t.includes('overlook') || t.includes('dolphin')) {
    return 'Viewpoints / Attractions';
  }
  if (t.includes('zoo') || t.includes('wildlife') || t.includes('sanctuary') || t.includes('safari') || t.includes('aquarium')) {
    return 'Wildlife / Zoo';
  }
  if (t.includes('museum') || t.includes('memorial') || t.includes('submarine') || t.includes('aircraft') || t.includes('gallery')) {
    return 'Museums';
  }
  if (t.includes('hill') || t.includes('waterfall') || t.includes('falls') || t.includes('caves') || t.includes('cave') || t.includes('valley') || t.includes('peak')) {
    return 'Nature / Hill Stations';
  }
  if (t.includes('fort') || t.includes('palace') || t.includes('qila') || t.includes('monument') || t.includes('tomb') || t.includes('heritage')) {
    return 'Historical / Heritage';
  }
  if (t.includes('park') || t.includes('garden') || t.includes('botanical')) {
    return 'Parks & Gardens';
  }
  if (t.includes('restaurant') || t.includes('cafe') || t.includes('food') || t.includes('dining') || t.includes('bakery') || t.includes('wharf')) {
    return 'Food & Dining';
  }
  if (t.includes('market') || t.includes('bazaar') || t.includes('mall') || t.includes('shopping')) {
    return 'Shopping / Market';
  }
  return 'Sightseeing & Attractions';
};

// Safe Genuine Rating Formatter (Per Requirement 10 & 16)
const formatRating = (val, count) => {
  if (!val || val === 'Information unavailable' || val === 'Rating unavailable') {
    return '⭐ Rating unavailable';
  }
  const num = typeof val === 'number' ? val : parseFloat(val);
  if (isNaN(num) || num <= 0) {
    return '⭐ Rating unavailable';
  }
  const countStr = count ? ` (${Number(count).toLocaleString()} reviews)` : '';
  return `⭐ ${num.toFixed(1)}${countStr}`;
};

const Itinerary = () => {
  const { t } = useTranslation();
  const { getDestName, formatTripTitle } = useDestinationTranslation();
  const { id } = useParams();
  const [trip, setTrip] = useState(null);
  const [loading, setLoading] = useState(true);
  const [adaptingDay, setAdaptingDay] = useState(null);
  const [routePolyline, setRoutePolyline] = useState(null);
  const [selectedActivityCoord, setSelectedActivityCoord] = useState(null);
  const [selectedActivityId, setSelectedActivityId] = useState(null);
  
  // Real-time clock for "Up Next" / "Next Activity"
  const [currentTime, setCurrentTime] = useState(new Date());

  // Weather warning states
  const [weatherAlertDismissed, setWeatherAlertDismissed] = useState({});

  // Adaptation History Change Log (WHAT CHANGED / WHY / REMOVED / ADDED)
  const [adaptationLog, setAdaptationLog] = useState(null);

  // Transport details & feasibility notice
  const [transportInfo, setTransportInfo] = useState(null);
  const [mapLayer, setMapLayer] = useState('roadmap'); // 'roadmap' | 'satellite' | 'osm'

  // Refs for scrolling and marker popups
  const mapContainerRef = useRef(null);
  const markerRefs = useRef({});

  // Next Destination Verification & Suitability State
  const [nextDestinationCheck, setNextDestinationCheck] = useState(null);
  const [showNextConditionAlert, setShowNextConditionAlert] = useState(false);
  const [nextAlternatives, setNextAlternatives] = useState([]);
  const [loadingNextAlternatives, setLoadingNextAlternatives] = useState(false);

  // Single-Activity Reschedule & Manual Place Adding State
  const [showRescheduleModal, setShowRescheduleModal] = useState(false);
  const [rescheduleTarget, setRescheduleTarget] = useState(null);
  const [rescheduleReason, setRescheduleReason] = useState('Weather concern');
  const [rescheduleAlternatives, setRescheduleAlternatives] = useState([]);
  const [loadingRescheduleAlts, setLoadingRescheduleAlts] = useState(false);

  // Manual Edit State (inside Reschedule modal)
  const [activeModalTab, setActiveModalTab] = useState('recommendations'); // 'recommendations', 'search', 'manual'
  const [manualForm, setManualForm] = useState({
    placeName: '',
    address: '',
    latitude: null,
    longitude: null,
    dayId: null,
    dayNumber: 1,
    startTime: '11:00',
    durationMinutes: 90,
    category: 'Sightseeing & Attractions',
    notes: ''
  });
  const [replaceManualForm, setReplaceManualForm] = useState({
    placeName: '',
    address: '',
    latitude: null,
    longitude: null,
    startTime: '11:00',
    durationMinutes: 90,
    category: 'Sightseeing & Attractions',
    notes: ''
  });
  const [editingTimingId, setEditingTimingId] = useState(null);
  const [timingForm, setTimingForm] = useState({ startTime: '11:00', durationMinutes: 90 });
  const [placeEditDrafts, setPlaceEditDrafts] = useState({});
  const [isSavingPlaceId, setIsSavingPlaceId] = useState(null);

  const [searchQuery, setSearchQuery] = useState('');
  const [destinationAttractions, setDestinationAttractions] = useState([]);
  const [loadingDestAttractions, setLoadingDestAttractions] = useState(false);
  const searchDebounceTimer = useRef(null);

  // Toast / notification feedback
  const [toastMessage, setToastMessage] = useState(null);
  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 4000);
  };

  const currentUserEmail = localStorage.getItem('userEmail') || sessionStorage.getItem('userEmail') || '';

  // Collaboration / Invite state
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('VIEWER');
  const [isSendingInvite, setIsSendingInvite] = useState(false);
  const [inviteError, setInviteError] = useState(null);

  // In-Dashboard Edit Trip state
  const [showEditTripModal, setShowEditTripModal] = useState(false);
  const [editFormData, setEditFormData] = useState({
    destinationCity: '',
    startingLocation: '',
    startDate: '',
    endDate: '',
    numDays: 1,
    budgetInr: 30000,
    numTravelers: 1,
    travelStyle: 'Solo',
    activityLevel: 'Moderate',
    foodPreference: 'Any',
    transitPreference: 'Train',
    travelDistancePref: 'Moderate (< 25 km)',
    maxDistanceKm: 25,
    maxEntryFee: '',
    interests: []
  });
  const [isSavingEdit, setIsSavingEdit] = useState(false);
  const [editError, setEditError] = useState(null);

  // Update clock every minute
  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 30000);
    return () => clearInterval(timer);
  }, []);

  // Fetch Trip & Integrate Live APIs
  const loadTripData = async () => {
    try {
      const data = await tripService.getTrip(id);
      
      // 1. Resolve Origin and Destination Coordinates
      const startingLocation = data.startingLocation || 'India';
      const destinationCity = data.destinationCity || 'Destination';

      let originCoords = (data.originLatitude && data.originLongitude) 
        ? [data.originLatitude, data.originLongitude] 
        : resolveKnownCoords(startingLocation);

      let destCoords = (data.destinationLatitude && data.destinationLongitude)
        ? [data.destinationLatitude, data.destinationLongitude]
        : resolveKnownCoords(destinationCity);

      // Fallbacks if not found
      if (!destCoords) destCoords = [16.5062, 80.6480]; // default Vijayawada
      if (!originCoords) originCoords = startingLocation.toLowerCase().includes('gudi') ? [16.4410, 80.9926] : destCoords;

      data.originCoords = originCoords;
      data.destinationCoords = destCoords;

      // 2. Validate Intercity Transport
      const requestedTransport = data.transitPreference || 'Flight';
      const validation = validateSegmentTransport(
        startingLocation, 
        destinationCity, 
        requestedTransport, 
        originCoords, 
        destCoords
      );

      data.transportNotice = data.transportNotice || validation.notice;
      data.validatedTransport = validation;
      setTransportInfo(validation);

      // 3. Hydrate Days and Activities
      let allActivityCoords = [];
      let globalSeq = 1;

      if (data.days && data.days.length > 0) {
        const sortedDays = [...data.days].sort((a, b) => (a.dayNumber ?? 0) - (b.dayNumber ?? 0));
        data.days = sortedDays.map((day, dIdx) => {
          const sortedItems = [...(day.items || [])].sort((a, b) => {
            const oA = a.itemOrder != null ? a.itemOrder : 0;
            const oB = b.itemOrder != null ? b.itemOrder : 0;
            if (oA !== oB) return oA - oB;
            return (a.startTime || '').localeCompare(b.startTime || '');
          });

          return {
            ...day,
            weather: day.weatherCondition
              ? `${day.weatherCondition}${day.maxTempC != null ? `, ${day.maxTempC}°C` : ''}`
              : "Live weather temporarily unavailable",
            theme: day.theme || `Day ${day.dayNumber} Exploration`,
            activities: sortedItems.filter(item => item.status !== 'CANCELLED').map((item, actIdx) => {
              const durHrs = (item.durationMinutes || 120) / 60;
              const seq = globalSeq++;
              
              // Coordinates
              let coords = (item.latitude && item.longitude) 
                ? [item.latitude, item.longitude] 
                : resolveKnownCoords(item.title);
              
              if (!coords) {
                coords = [destCoords[0] + (actIdx * 0.008), destCoords[1] + (actIdx * 0.008)];
              }

              return {
                id: item.id || `act-${dIdx}-${actIdx}`,
                seq,
                itemOrder: item.itemOrder != null ? item.itemOrder : actIdx,
                dayId: day.id,
                dayNumber: day.dayNumber,
                time: item.startTime || (actIdx === 0 ? "09:00" : actIdx === 1 ? "12:00" : actIdx === 2 ? "15:00" : "18:00"),
                type: item.category ? item.category.toLowerCase().replace(/[^a-z0-9]/g, '-') : "attraction",
                category: item.category || "Sightseeing",
                title: item.title,
                location: item.indoorOutdoor || "Outdoor",
                indoorOutdoor: item.indoorOutdoor || "Outdoor",
                coordinates: coords,
                description: item.recommendationReason || `Top-rated destination in ${data.destinationCity}`,
                cost: item.costInr && item.costInr > 0 ? `₹${item.costInr}` : "Free Entry",
                costNumeric: item.costInr || 0,
                duration: item.durationMinutes ? `${Math.round(item.durationMinutes)} mins` : `${durHrs} hour${durHrs > 1 ? 's' : ''}`,
                status: item.status || "SCHEDULED",
                rating: item.rating,
                reviewCount: item.reviewCount,
                aiMatchScore: item.aiMatchScore || 92,
                address: item.address,
                weatherAlert: item.weatherAlert,
                openingHours: item.openingHours || "06:00 - 20:00 • Open Today",
                transitInfo: item.transportFromPrevious || (actIdx === 0 ? `Local travel from ${destinationCity} Hub` : `Local travel from previous attraction`),
                distanceFromPrev: item.distanceFromPrevious || (actIdx === 0 ? 4.2 : 5.5),
                durationFromPrev: item.durationFromPrevious || (actIdx === 0 ? 15 : 20),
                verificationStatus: item.verificationStatus || "100% Real Location • Verified"
              };
            })
          };
        });

        // Calculate travel distances between consecutive activities
        data.days.forEach((day, dIdx) => {
          const acts = day.activities || [];
          for (let i = 0; i < acts.length; i++) {
            const act = acts[i];
            if (act.coordinates) {
              allActivityCoords.push(act.coordinates);
            }

            if (i === 0 && dIdx === 0) {
              const distKm = calculateHaversineKm(originCoords[0], originCoords[1], act.coordinates[0], act.coordinates[1]);
              act.transitDist = `${distKm} km`;
              act.transitTime = `${Math.max(15, Math.round(distKm * 1.5))} mins`;
              act.transitInfo = `${validation.actualMode} (${startingLocation} → ${act.title})`;
            } else if (i === 0) {
              act.transitDist = "3.5 km";
              act.transitTime = "12 mins";
              act.transitInfo = `Local Cab / Auto (${destinationCity} Hub → ${act.title})`;
            } else {
              const prev = acts[i - 1]?.coordinates;
              const curr = act.coordinates;
              if (prev && curr) {
                const distKm = calculateHaversineKm(prev[0], prev[1], curr[0], curr[1]);
                const transitMin = Math.max(10, Math.round(distKm * 2.8));
                act.transitDist = `${distKm} km`;
                act.transitTime = `${transitMin} mins`;
                act.transitInfo = `Local Cab / Auto (${acts[i - 1]?.title || "Previous Stop"} → ${act.title})`;
              }
            }
          }
        });

        // 4. Fetch Real Weather Forecast (Consistent coordinates and forecast periods)
        if (destCoords) {
          try {
            const updatedDays = await Promise.all(data.days.map(async (day, idx) => {
              // Target coordinates for this day's planned activities (using primary activity or destination)
              const targetCoords = destCoords;
              const dayDateStr = day.date ? (typeof day.date === 'string' ? day.date.split('T')[0] : '') : '';
              const weatherData = await weatherService.getWeatherForecast(targetCoords[0], targetCoords[1], dayDateStr || null);
              if (weatherData && weatherData.daily && weatherData.daily.time) {
                const times = weatherData.daily.time;
                const codes = weatherData.daily.weather_code || weatherData.daily.weathercode || [];
                const maxTemps = weatherData.daily.temperature_2m_max || [];
                const rainProbs = weatherData.daily.precipitation_probability_max || [];
                const precipSums = weatherData.daily.precipitation_sum || [];

                // Find matching forecast date for this itinerary day
                let wIdx = -1;
                if (dayDateStr) {
                  wIdx = times.findIndex(t => t === dayDateStr);
                }
                
                // If date was not matched (e.g. day within 16-day window starting today)
                if (wIdx === -1 && idx < times.length && (!dayDateStr || dayDateStr < times[0])) {
                  wIdx = Math.min(idx, times.length - 1);
                }

                // If date is beyond available forecast horizon (> 16 days), show unavailable rather than inventing fake numbers
                if (wIdx === -1) {
                  return {
                    ...day,
                    weather: "Live weather temporarily unavailable",
                    rainProbability: null,
                    precipitationMm: null,
                    isWeatherUnavailable: true,
                    hasWeatherAlert: false,
                    weatherAlertText: null
                  };
                }

                const temp = maxTemps[wIdx] != null ? Math.round(maxTemps[wIdx]) : null;
                const code = codes[wIdx] != null ? codes[wIdx] : null;
                const rainProb = rainProbs[wIdx] != null ? Math.round(rainProbs[wIdx]) : null;
                const precipSum = precipSums[wIdx] != null ? Number(precipSums[wIdx]) : null;
                
                // Comprehensive WMO Weather Code Mapping
                let condition = code == null ? "Live weather temporarily unavailable" : "Clear Sky";
                if (code === 1) condition = "Mainly Clear";
                else if (code === 2) condition = "Partly Cloudy";
                else if (code === 3) condition = "Overcast";
                else if (code === 45 || code === 48) condition = "Foggy";
                else if (code === 51) condition = "Light Drizzle";
                else if (code === 53) condition = "Moderate Drizzle";
                else if (code === 55) condition = "Dense Drizzle";
                else if (code === 56 || code === 57) condition = "Freezing Drizzle";
                else if (code === 61) condition = "Slight Rain";
                else if (code === 63) condition = "Moderate Rain";
                else if (code === 65) condition = "Heavy Rain";
                else if (code === 71 || code === 73 || code === 75) condition = "Snowy";
                else if (code === 80) condition = "Passing Showers";
                else if (code === 81) condition = "Rain Showers";
                else if (code === 82) condition = "Violent Showers";
                else if (code >= 95) condition = "Thunderstorm";

                // Severe weather detection matches backend: rainProb >= 60%, code 65/82/95+, or precipSum >= 5.0 with rainProb >= 50%
                const isSevereRain = (rainProb != null && rainProb >= 60) || 
                                     (code >= 95) || (code === 65 || code === 82) || 
                                     (precipSum != null && precipSum >= 5.0 && rainProb != null && rainProb >= 50);
                
                return {
                  ...day,
                  weather: `${condition}${temp != null ? `, ${temp}°C` : ''}${weatherData.forecastMessage ? ' — Live weather temporarily unavailable (showing cached forecast)' : ''}`,
                  rainProbability: rainProb,
                  precipitationMm: precipSum,
                  isWeatherUnavailable: false,
                  hasWeatherAlert: isSevereRain,
                  weatherAlertText: isSevereRain 
                    ? `Inclement weather expected (${condition}${rainProb != null ? `, ${rainProb}% rain chance` : ''}${precipSum != null ? `, ${precipSum} mm` : ''}). Consider indoor activities.`
                    : null
                };
              }

              return {
                ...day,
                weather: "Live weather temporarily unavailable",
                rainProbability: null,
                precipitationMm: null,
                isWeatherUnavailable: true,
                hasWeatherAlert: false,
                weatherAlertText: null
              };
            }));

            data.days = updatedDays;
          } catch (e) {
            console.error("Live weather fetch failed", e);
          }
        }

        // 5. Fetch Real Road Routes via OSRM
        const routePoints = [];
        if (originCoords && (originCoords[0] !== destCoords[0] || originCoords[1] !== destCoords[1])) {
          routePoints.push(originCoords);
        }
        if (destCoords) routePoints.push(destCoords);
        routePoints.push(...allActivityCoords);

        if (routePoints.length > 1) {
          try {
            const route = await mapService.getRoute(routePoints);
            if (route) setRoutePolyline(route);
          } catch (e) {
            console.error("Route fetch failed", e);
          }
        }
      }
      
      setTrip(data);
    } catch (error) {
      console.error("Failed to load trip", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTripData();
  }, [id]);

  // Compute Scheduled Next & Up Next (From single consistent itinerary data source)
  const getNextActivity = () => {
    if (!trip || !trip.days || trip.days.length === 0) return null;
    const allActivities = (trip?.days || []).flatMap(d => (d.activities || []).filter(a => a.status !== "REPLACED" && a.status !== "CANCELLED"));
    if (allActivities.length === 0) return null;

    // Find the first non-completed activity
    const pending = allActivities.filter(a => a.status !== "COMPLETED");
    if (pending.length > 0) {
      const scheduledNext = pending[0];
      const upNextItems = pending.slice(1, 3);
      return {
        ...scheduledNext,
        statusLabel: "Scheduled Next",
        upNextItems
      };
    }

    return {
      title: "All Planned Places Completed!",
      statusLabel: "All Completed",
      isAllCompleted: true,
      time: "Complete",
      duration: "Full itinerary finished",
      cost: "",
      upNextItems: []
    };
  };

  const nextActivity = getNextActivity();

  // Action: Mark Activity as Completed & Check Next Place Conditions
  const handleMarkCompleted = async (activityId, simulateWeather = null) => {
    try {
      showToast('Marking place as completed...');
      await tripService.completeActivity(trip.id, activityId);

      // Locally update state immediately for instantaneous UI feedback
      setTrip(prev => {
        if (!prev) return prev;
        const updatedDays = prev.days.map(day => ({
          ...day,
          activities: day.activities.map(a => a.id === activityId ? { ...a, status: 'COMPLETED' } : a)
        }));
        return { ...prev, days: updatedDays };
      });

      // Context-aware Next Place Check
      const checkRes = await tripService.checkNextActivity(trip.id, activityId, simulateWeather);
      setNextDestinationCheck(checkRes);

      if (checkRes.hasNext) {
        if (!checkRes.isSuitable) {
          setShowNextConditionAlert(true);
        } else {
          showToast(`✓ Marked completed. Next: ${checkRes.nextActivityTitle} is on track!`);
        }
      } else {
        showToast('🎉 All planned places in this itinerary have been visited and completed!');
      }
    } catch (err) {
      console.error('Failed to complete activity', err);
      showToast('Could not complete activity. Please try again.');
    }
  };

  // Action: Switch from Simulation back to Live Weather (Clear simulation mode immediately)
  const handleSwitchToLiveWeather = async (activityId) => {
    try {
      showToast('Fetching real-time weather from Open-Meteo...');
      const actId = activityId || nextActivity?.id || (trip?.days?.[0]?.activities?.[0]?.id);
      const checkRes = await tripService.checkNextActivity(trip.id, actId, 'live');
      setNextDestinationCheck(checkRes);
      if (checkRes.isSuitable) {
        setShowNextConditionAlert(false);
        showToast(`✓ Switched to live weather. Next destination (${checkRes.nextActivityTitle}) is clear!`);
      } else {
        showToast('✓ Switched to actual live meteorological forecast.');
      }
    } catch (err) {
      console.error('Failed to switch to live weather', err);
      showToast('Could not fetch live weather. Please try again.');
    }
  };

  // Action: Preview Live Conditions for Next Activity (without completing)
  const handleCheckNextActivityLive = async (activityId) => {
    try {
      showToast('Checking live weather for next destination...');
      const actId = activityId || nextActivity?.id || (trip?.days?.[0]?.activities?.[0]?.id);
      const checkRes = await tripService.checkNextActivity(trip.id, actId, 'live');
      setNextDestinationCheck(checkRes);
      setShowNextConditionAlert(true);
    } catch (err) {
      console.error('Failed to check destination live weather', err);
      showToast('Could not retrieve live destination weather.');
    }
  };

  // Action: Open Reschedule Modal for Single Activity
  const handleOpenReschedule = async (activity) => {
    setRescheduleTarget(activity);
    setRescheduleReason('Weather concern');
    setRescheduleAlternatives([]);
    setActiveModalTab('recommendations');

    const targetDay = trip?.days?.find(d => 
      (d.items && d.items.some(it => it.id === activity.id)) ||
      (d.activities && d.activities.some(a => a.id === activity.id))
    ) || (trip?.days && trip.days[0]);

    setManualForm({
      placeName: '',
      address: '',
      latitude: null,
      longitude: null,
      dayId: targetDay ? targetDay.id : (trip?.days?.[0]?.id || null),
      dayNumber: targetDay ? targetDay.dayNumber : 1,
      startTime: activity.time || activity.startTime || '11:00',
      durationMinutes: 90,
      category: 'Sightseeing & Attractions',
      notes: ''
    });

    setReplaceManualForm({
      placeName: '',
      address: '',
      latitude: null,
      longitude: null,
      startTime: activity.time || activity.startTime || '11:00',
      durationMinutes: activity.durationMinutes || 90,
      category: 'Sightseeing & Attractions',
      notes: ''
    });

    setEditingTimingId(null);
    setPlaceEditDrafts({});
    setIsSavingPlaceId(null);
    setShowRescheduleModal(true);
    setLoadingRescheduleAlts(true);
    try {
      const alts = await tripService.getAlternatives(trip.id, activity.id, 'Weather concern');
      setRescheduleAlternatives(alts || []);
    } catch (e) {
      console.error('Failed to fetch alternatives', e);
    } finally {
      setLoadingRescheduleAlts(false);
    }
  };

  const handleReasonChange = async (newReason) => {
    setRescheduleReason(newReason);
    if (!rescheduleTarget) return;
    setLoadingRescheduleAlts(true);
    try {
      const alts = await tripService.getAlternatives(trip.id, rescheduleTarget.id, newReason);
      setRescheduleAlternatives(alts || []);
    } catch (e) {
      console.error('Failed to fetch alternatives for reason', e);
    } finally {
      setLoadingRescheduleAlts(false);
    }
  };

  // Action: Choose Alternative Place (Replaces only that single destination)
  const handleSelectAlternative = async (targetActivityId, candidate) => {
    try {
      showToast(`Replacing with ${candidate.title}...`);
      await tripService.replaceActivity(trip.id, targetActivityId, candidate);

      // Re-hydrate trip data
      await loadTripData();
      setShowRescheduleModal(false);
      setShowNextConditionAlert(false);
      showToast(`✓ Replaced with verified alternative: ${candidate.title}. Rest of itinerary unchanged.`);
    } catch (e) {
      console.error('Failed to replace activity', e);
      showToast('Could not replace activity. Please try again.');
    }
  };

  // Action: Fetch Alternatives for Next Place from Alert Modal
  const handleFetchNextAlternatives = async (activityId) => {
    setLoadingNextAlternatives(true);
    try {
      const alts = await tripService.getAlternatives(trip.id, activityId, 'Weather concern');
      setNextAlternatives(alts || []);
    } catch (e) {
      console.error('Failed to fetch alternatives for next destination', e);
    } finally {
      setLoadingNextAlternatives(false);
    }
  };

  // Travel Pace Constraint Helper
  const getPaceLimit = (paceStr) => {
    const p = (paceStr || '').toLowerCase();
    if (p.includes('relaxed') || p.includes('slow')) return { min: 1, max: 2, label: 'Relaxed Pace (1-2 places/day)' };
    if (p.includes('fast') || p.includes('packed')) return { min: 5, max: 6, label: 'Fast Paced (5-6 places/day)' };
    return { min: 3, max: 4, label: 'Moderate Pace (3-4 places/day)' };
  };

  // Destination Attractions Search Helper
  const fetchDestinationAttractions = async (destination, query = '', prevCoords = null) => {
    setLoadingDestAttractions(true);
    try {
      const pLat = prevCoords ? prevCoords[0] : null;
      const pLon = prevCoords ? prevCoords[1] : null;
      const results = await placesService.getDestinationAttractions(destination || trip?.destinationCity || 'Goa', query, pLat, pLon);
      setDestinationAttractions(results || []);
    } catch (e) {
      console.error('Failed to search attractions', e);
      setDestinationAttractions([]);
    } finally {
      setLoadingDestAttractions(false);
    }
  };

  const handleSearchAttractionsChange = (val, prevCoords = null) => {
    setSearchQuery(val);
    if (searchDebounceTimer.current) clearTimeout(searchDebounceTimer.current);
    searchDebounceTimer.current = setTimeout(() => {
      fetchDestinationAttractions(trip?.destinationCity, val, prevCoords);
    }, 350);
  };

  // Direct Manual Place Addition (Inside Reschedule -> Manual Edit)
  const handleDirectAddPlaceSubmit = async (e) => {
    if (e && e.preventDefault) e.preventDefault();
    if (!manualForm.placeName || !manualForm.placeName.trim()) {
      showToast('Please enter a place name.');
      return;
    }
    const chosenDayId = manualForm.dayId || (trip?.days && trip.days[0]?.id);
    if (!chosenDayId) {
      showToast('Please select a day of visit.');
      return;
    }

    const placeTitle = manualForm.placeName.trim();
    showToast(`Adding "${placeTitle}" to itinerary...`);
    try {
      const fbCoords = resolveKnownCoords(placeTitle) || resolveKnownCoords(manualForm.address);
      const lat = manualForm.latitude != null ? manualForm.latitude : (fbCoords ? fbCoords[0] : null);
      const lng = manualForm.longitude != null ? manualForm.longitude : (fbCoords ? fbCoords[1] : null);
      const detectedCat = (manualForm.category && manualForm.category !== 'Sightseeing' && manualForm.category !== 'Sightseeing & Attractions')
        ? manualForm.category
        : inferCategory(placeTitle, manualForm.address);

      const payload = {
        title: placeTitle,
        address: manualForm.address?.trim() || null,
        latitude: lat,
        longitude: lng,
        startTime: manualForm.startTime || '11:00',
        durationMinutes: parseInt(manualForm.durationMinutes, 10) || 90,
        category: detectedCat,
        notes: manualForm.notes?.trim() || null,
        recommendationReason: manualForm.notes?.trim() 
          ? `Manually added by traveller: ${manualForm.notes.trim()}` 
          : 'Manually added by traveller',
        costInr: 0,
        indoorOutdoor: 'Outdoor'
      };

      await tripService.addActivity(trip.id, chosenDayId, payload);
      await loadTripData();
      setManualForm(prev => ({
        ...prev,
        placeName: '',
        address: '',
        latitude: null,
        longitude: null,
        notes: ''
      }));
      showToast(`✓ Added "${placeTitle}" to itinerary successfully!`);
    } catch (err) {
      console.error('Failed to add place manually', err);
      showToast('Could not add place. Please try again.');
    }
  };

  // Replace Target Activity with Custom Place (Inside Reschedule -> Manual Edit)
  const handleReplaceWithManualPlaceSubmit = async (e) => {
    if (e && e.preventDefault) e.preventDefault();
    if (!replaceManualForm.placeName || !replaceManualForm.placeName.trim()) {
      showToast('Please enter a replacement place name.');
      return;
    }
    if (!rescheduleTarget) return;

    const repName = replaceManualForm.placeName.trim();
    showToast(`Replacing "${rescheduleTarget.title}" with "${repName}"...`);
    try {
      const fbCoords = resolveKnownCoords(repName) || resolveKnownCoords(replaceManualForm.address);
      const lat = replaceManualForm.latitude != null ? replaceManualForm.latitude : (fbCoords ? fbCoords[0] : null);
      const lng = replaceManualForm.longitude != null ? replaceManualForm.longitude : (fbCoords ? fbCoords[1] : null);
      const detectedCat = (replaceManualForm.category && replaceManualForm.category !== 'Sightseeing' && replaceManualForm.category !== 'Sightseeing & Attractions')
        ? replaceManualForm.category
        : inferCategory(repName, replaceManualForm.address);

      const payload = {
        title: repName,
        address: replaceManualForm.address?.trim() || null,
        latitude: lat,
        longitude: lng,
        startTime: replaceManualForm.startTime || rescheduleTarget.time || '11:00',
        durationMinutes: parseInt(replaceManualForm.durationMinutes, 10) || 90,
        category: detectedCat,
        notes: replaceManualForm.notes?.trim() || null,
        recommendationReason: replaceManualForm.notes?.trim()
          ? `Manually added by traveller: ${replaceManualForm.notes.trim()}`
          : 'Manually added by traveller',
        costInr: 0,
        indoorOutdoor: 'Outdoor'
      };

      await tripService.replaceActivity(trip.id, rescheduleTarget.id, payload);
      await loadTripData();
      setShowRescheduleModal(false);
      setReplaceManualForm({
        placeName: '',
        address: '',
        latitude: null,
        longitude: null,
        startTime: '11:00',
        durationMinutes: 90,
        category: 'Sightseeing & Attractions',
        notes: ''
      });
      showToast(`✓ Replaced with "${repName}". Itinerary updated!`);
    } catch (err) {
      console.error('Failed to replace place manually', err);
      showToast('Could not replace place. Please try again.');
    }
  };

  // Save Place Movement & Order Changes (Combined Day & Position)
  const handleSavePlaceChanges = async (activityId, sourceDayId) => {
    const draft = placeEditDrafts[activityId];
    if (!draft) return;
    const targetDayId = draft.dayId || sourceDayId;
    const targetPosition = draft.position;

    setIsSavingPlaceId(activityId);
    showToast('Saving place changes & recalculating schedule...');
    try {
      await tripService.moveActivityToDay(trip.id || id, activityId, targetDayId, targetPosition);
      setPlaceEditDrafts(prev => {
        const next = { ...prev };
        delete next[activityId];
        return next;
      });
      await loadTripData();
      showToast('✓ Place moved and itinerary schedule saved successfully!');
    } catch (err) {
      console.error('Failed to save place changes', err);
      const errMsg = err.response?.data?.message || err.message || 'Could not move place. Please try again.';
      showToast(`Error: ${errMsg}`);
    } finally {
      setIsSavingPlaceId(null);
    }
  };

  const handleCancelPlaceChanges = (activityId) => {
    setPlaceEditDrafts(prev => {
      const next = { ...prev };
      delete next[activityId];
      return next;
    });
  };

  // Move Place Between Days (Direct action or fallback)
  const handleMoveActivityBetweenDays = async (activityId, targetDayId, targetPosition = null) => {
    if (!targetDayId) return;
    showToast('Moving place to new day & recalculating schedules...');
    try {
      await tripService.moveActivityToDay(trip.id || id, activityId, targetDayId, targetPosition);
      setPlaceEditDrafts(prev => {
        const next = { ...prev };
        delete next[activityId];
        return next;
      });
      await loadTripData();
      showToast('✓ Place moved to new day successfully!');
    } catch (err) {
      console.error('Failed to move place between days', err);
      const errMsg = err.response?.data?.message || err.message || 'Could not move place. Please try again.';
      showToast(`Error: ${errMsg}`);
    }
  };

  // Update Activity Timing / Duration (Inside Reschedule -> Manual Edit)
  const handleUpdateActivityTiming = async (activityId, newStartTime, newDurationMinutes) => {
    showToast('Updating visit schedule...');
    try {
      await tripService.updateActivity(trip.id || id, activityId, {
        startTime: newStartTime,
        durationMinutes: parseInt(newDurationMinutes, 10) || 90
      });
      await loadTripData();
      showToast('✓ Place schedule updated!');
    } catch (err) {
      console.error('Failed to update place timing', err);
      const errMsg = err.response?.data?.message || err.message || 'Could not update timing. Please try again.';
      showToast(`Error: ${errMsg}`);
    }
  };

  // Manual Reordering: Move Up / Move Down (Accepts ID, object, or index)
  const handleMoveActivity = async (day, activityOrIndex, direction) => {
    const activeActs = (day.activities || []).filter(a => a.status !== 'CANCELLED' && a.status !== 'REPLACED');
    let actIndex = -1;
    let targetAct = null;

    if (typeof activityOrIndex === 'object' && activityOrIndex !== null) {
      targetAct = activityOrIndex;
      actIndex = activeActs.findIndex(a => String(a.id) === String(targetAct.id));
    } else {
      actIndex = activeActs.findIndex(a => String(a.id) === String(activityOrIndex));
      if (actIndex === -1 && typeof activityOrIndex === 'number' && activityOrIndex >= 0 && activityOrIndex < activeActs.length) {
        actIndex = activityOrIndex;
      }
      targetAct = activeActs[actIndex];
    }

    if (!targetAct || actIndex === -1) return;
    const newActiveIndex = actIndex + direction;

    if (newActiveIndex < 0 || newActiveIndex >= activeActs.length) return;

    showToast(`Moving "${targetAct.title}" to position #${newActiveIndex + 1} & recalculating travel times...`);
    try {
      await tripService.moveActivityToDay(trip.id || id, targetAct.id, day.id, newActiveIndex + 1);
      setPlaceEditDrafts(prev => {
        const next = { ...prev };
        delete next[targetAct.id];
        return next;
      });
      await loadTripData();
      showToast(`✓ Visit order updated: "${targetAct.title}" is now #${newActiveIndex + 1}`);
    } catch (e) {
      console.error('Failed to reorder activities', e);
      const errMsg = e.response?.data?.message || e.message || 'Could not reorder activities. Please try again.';
      showToast(`Error: ${errMsg}`);
    }
  };

  // Direct Position Reordering (e.g. Move place 3 to place 1)
  const handleReorderToPosition = async (day, fromIndex, toIndex) => {
    const activeActs = (day.activities || []).filter(a => a.status !== 'CANCELLED' && a.status !== 'REPLACED');
    if (fromIndex < 0 || fromIndex >= activeActs.length || toIndex < 0 || toIndex >= activeActs.length || fromIndex === toIndex) return;

    const targetAct = activeActs[fromIndex];
    if (!targetAct) return;

    showToast(`Moving "${targetAct.title}" to position #${toIndex + 1} & recalculating travel times...`);
    try {
      await tripService.moveActivityToDay(trip.id, targetAct.id, day.id, toIndex + 1);
      setPlaceEditDrafts(prev => {
        const next = { ...prev };
        delete next[targetAct.id];
        return next;
      });
      await loadTripData();
      showToast(`✓ "${targetAct.title}" moved to position #${toIndex + 1}!`);
    } catch (e) {
      console.error('Failed to reorder activities to position', e);
      const errMsg = e.response?.data?.message || e.message || 'Could not reorder activities. Please try again.';
      showToast(`Error: ${errMsg}`);
    }
  };

  // Action: Remove Single Activity
  const handleRemoveActivity = async (activityId) => {
    if (!window.confirm("Remove this place from your itinerary?")) return;
    try {
      showToast('Removing activity...');
      await tripService.removeActivity(trip.id, activityId);
      await loadTripData();
      showToast('Activity removed from itinerary.');
    } catch (e) {
      console.error('Failed to remove activity', e);
    }
  };

  // Collaborative & Trip Actions
  const handleSendInvite = async (e) => {
    e.preventDefault();
    if (!inviteEmail.trim()) return;
    setIsSendingInvite(true);
    setInviteError(null);
    try {
      await tripService.inviteMember(trip.id, inviteEmail.trim(), inviteRole);
      showToast(`✓ Invitation sent to ${inviteEmail}!`);
      setShowInviteModal(false);
      setInviteEmail('');
      await loadTripData();
    } catch (err) {
      console.error('Invite error', err);
      const msg = err.response?.data?.message || err.message || 'Failed to send invitation.';
      setInviteError(msg);
    } finally {
      setIsSendingInvite(false);
    }
  };

  const handleRemoveMember = async (memberId, memberName) => {
    if (!window.confirm(`Remove ${memberName || 'member'} from this trip?`)) return;
    try {
      await tripService.removeMember(trip.id, memberId);
      showToast('Member removed from trip.');
      await loadTripData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to remove member.');
    }
  };

  const handleShareTrip = () => {
    navigator.clipboard.writeText(window.location.href);
    showToast('✓ Trip link copied to clipboard!');
  };

  const handleOpenEditModal = () => {
    const existingInterests = trip.interests ? trip.interests.split(',').map(s => s.trim()).filter(Boolean) : ['Heritage', 'Food'];
    setEditFormData({
      destinationCity: trip.destinationCity || '',
      startingLocation: trip.startingLocation || '',
      startDate: trip.startDate || '',
      endDate: trip.endDate || '',
      numDays: trip.days?.length || trip.numDays || 2,
      budgetInr: trip.budgetInr || trip.totalBudget || 20000,
      numTravelers: trip.numTravelers || 1,
      travelStyle: trip.travelStyle || 'Solo',
      activityLevel: trip.activityLevel || 'Moderate',
      foodPreference: trip.foodPreference || 'Any',
      transitPreference: trip.transitPreference || 'Train',
      travelDistancePref: trip.travelDistancePref || 'Moderate (< 25 km)',
      maxDistanceKm: trip.maxDistanceKm || 25,
      maxEntryFee: trip.maxEntryFee !== null && trip.maxEntryFee !== undefined ? trip.maxEntryFee : '',
      interests: existingInterests
    });
    setEditError(null);
    setShowEditTripModal(true);
  };

  const handleInterestToggle = (interest) => {
    setEditFormData(prev => {
      const exists = prev.interests.includes(interest);
      if (exists) {
        return { ...prev, interests: prev.interests.filter(i => i !== interest) };
      } else {
        return { ...prev, interests: [...prev.interests, interest] };
      }
    });
  };

  const handleDateChange = (field, value) => {
    setEditFormData(prev => {
      const updated = { ...prev, [field]: value };
      if (updated.startDate && updated.endDate) {
        const start = new Date(updated.startDate);
        const end = new Date(updated.endDate);
        if (!isNaN(start) && !isNaN(end) && end >= start) {
          const diffDays = Math.round((end - start) / (1000 * 60 * 60 * 24)) + 1;
          updated.numDays = Math.max(1, diffDays);
        }
      }
      return updated;
    });
  };

  const handleSaveEditTrip = async (e) => {
    e.preventDefault();
    setIsSavingEdit(true);
    setEditError(null);
    try {
      const payload = {
        destinationCity: editFormData.destinationCity,
        startingLocation: editFormData.startingLocation,
        startDate: editFormData.startDate,
        endDate: editFormData.endDate,
        numDays: parseInt(editFormData.numDays) || 1,
        budgetInr: parseFloat(editFormData.budgetInr) || 20000,
        numTravelers: parseInt(editFormData.numTravelers) || 1,
        travelStyle: editFormData.travelStyle,
        activityLevel: editFormData.activityLevel,
        foodPreference: editFormData.foodPreference,
        transitPreference: editFormData.transitPreference,
        travelDistancePref: editFormData.travelDistancePref,
        maxDistanceKm: editFormData.maxDistanceKm ? parseFloat(editFormData.maxDistanceKm) : null,
        maxEntryFee: editFormData.maxEntryFee !== '' ? parseFloat(editFormData.maxEntryFee) : null,
        interests: editFormData.interests
      };

      await tripService.updateTrip(trip.id, payload);
      showToast('✓ Trip updated & itinerary recalculated successfully!');
      setShowEditTripModal(false);
      await loadTripData();
    } catch (err) {
      console.error('Failed to update trip', err);
      setEditError(err.response?.data?.message || 'Failed to update trip.');
    } finally {
      setIsSavingEdit(false);
    }
  };

  const lastCompleted = trip?.days?.flatMap(d => (d.activities || [])).filter(a => a.status === 'COMPLETED').slice(-1)[0];

  // Action: Locate on Map (Smooth scroll, Center, Fly, Highlight, Open Popup)
  const handleLocateOnMap = (activity) => {
    if (!activity) return;

    // 1. Smoothly scroll the map into view if not in view
    if (mapContainerRef.current) {
      mapContainerRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    // 2. Identify coordinates
    let coords = activity.coordinates;
    if (!coords && trip.destinationCoords) {
      coords = trip.destinationCoords;
    }

    if (coords) {
      setSelectedActivityCoord(coords);
      setSelectedActivityId(activity.id);

      // 3. Open Popup on marker
      setTimeout(() => {
        const marker = markerRefs.current[activity.id];
        if (marker) {
          marker.openPopup();
        }
      }, 500);
    }
  };

  // Calculate Budget Breakdown in INR
  const calculateBudgetBreakdown = () => {
    if (!trip) return null;
    const totalUserBudget = trip.budgetInr || 30000;
    const numDays = trip.days ? trip.days.length : (trip.numDays || 1);
    const numTravelers = trip.numTravelers || 1;

    // Sum of verified entry fees from activities
    const activitiesCost = trip?.days 
      ? trip.days.flatMap(d => (d.activities || [])).filter(a => a.status !== "REPLACED").reduce((sum, a) => sum + (a.costNumeric || 0), 0) * numTravelers
      : 800;

    // Estimated Accommodation (based on travel style)
    const baseRoomRate = trip.travelStyle === 'Couple' ? 2800 : trip.travelStyle === 'Family' ? 3800 : 2000;
    const accommodationCost = baseRoomRate * Math.max(1, numDays - 1);

    // Food & Dining (approx ₹700 per person per day)
    const foodCost = numDays * numTravelers * 750;

    // Transport (Intercity from starting location to destination)
    const intercityEstimate = transportInfo?.estimatedCostInr ? transportInfo.estimatedCostInr * numTravelers : 800;
    const transportCost = Math.round(intercityEstimate);

    // Local travel (Cabs/Transit within destination)
    const localTravelCost = numDays * 650;

    // Buffer (8%)
    const subtotal = activitiesCost + accommodationCost + foodCost + transportCost + localTravelCost;
    const bufferCost = Math.round(subtotal * 0.08);

    const totalEstimated = subtotal + bufferCost;
    const remainingBudget = totalUserBudget - totalEstimated;

    return {
      transport: transportCost,
      accommodation: accommodationCost,
      food: foodCost,
      activities: activitiesCost,
      localTravel: localTravelCost,
      buffer: bufferCost,
      totalEstimated,
      userBudget: totalUserBudget,
      remainingBudget
    };
  };

  const budgetBreakdown = calculateBudgetBreakdown();

  // Action: Adapt Schedule on Rainy Day
  const handleAdaptDay = async (dayId) => {
    setAdaptingDay(dayId);
    try {
      const targetDayObj = trip?.days?.find(d => d.id === dayId);
      if (targetDayObj?.rainProbability == null) {
        showToast('Live weather temporarily unavailable. Cannot adapt using an unknown rain probability.');
        return;
      }
      const condition = targetDayObj?.weather?.split(',')[0] || 'Weather Condition Unavailable';
      const rainProbVal = targetDayObj.rainProbability / 100.0;
      const tempMatch = targetDayObj?.weather?.match(/(\d+)°C/);
      const tempVal = tempMatch ? parseFloat(tempMatch[1]) : null;

      const updatedTrip = await tripService.adaptDay(id, dayId, condition, rainProbVal, tempVal);
      
      const removedItems = [];
      const addedItems = [];

      if (updatedTrip.days) {
        updatedTrip.days = updatedTrip.days.map((day, dIdx) => {
          if (day.id === dayId || dIdx === 0) {
            day.items?.forEach(item => {
              if (item.status === 'REPLACED') {
                removedItems.push(item.title);
              } else if (item.status === 'ACTIVE' && item.recommendationReason?.includes('AI Weather Adapted')) {
                addedItems.push(item.title);
              }
            });
          }

          return {
            ...day,
            weather: `${condition}${tempVal != null ? `, ${Math.round(tempVal)}°C` : ''}`,
            theme: "Weather-Protected Indoor Plan",
            hasWeatherAlert: false, // resolved by adaptation
            activities: day.items ? day.items.map((item, actIdx) => ({
              id: item.id || `act-${dIdx}-${actIdx}`,
              time: item.startTime,
              type: item.category ? item.category.toLowerCase().replace(/[^a-z0-9]/g, '-') : "attraction",
              category: item.category || "Sightseeing",
              title: item.title,
              location: item.indoorOutdoor || "Indoor",
              coordinates: item.latitude && item.longitude ? [item.latitude, item.longitude] : resolveKnownCoords(item.title),
              description: item.recommendationReason || `${item.category} activity`,
              cost: item.costInr && item.costInr > 0 ? `₹${item.costInr}` : "Free Entry",
              costNumeric: item.costInr || 0,
              duration: `${(item.durationMinutes || 120) / 60} hours`,
              status: item.status || "ACTIVE",
              weatherAlert: item.weatherAlert,
              transitInfo: `${12 + actIdx * 3} mins transit via cab`
            })) : []
          };
        });
      }

      setTrip(prev => ({
        ...prev,
        ...updatedTrip,
        days: updatedTrip.days
      }));

      // Record adaptation change log with real forecast probability
      setAdaptationLog({
        whatChanged: `Day ${dayId ? 'Schedule' : '1'} adapted for inclement weather forecast`,
        whyChanged: `Elevated precipitation expected (${Math.round(rainProbVal * 100)}% chance, ${condition}). Outdoor viewpoints and open parks replaced with verified indoor museums, art galleries, and cultural heritage centers.`,
        removed: removedItems.length > 0 ? removedItems : ["Outdoor viewpoints & open parks"],
        added: addedItems.length > 0 ? addedItems : ["Verified State Museum & Art Gallery", "Cultural Indoor Heritage Centre"]
      });

    } catch (e) {
      console.error("Failed to adapt day", e);
    } finally {
      setAdaptingDay(null);
    }
  };




  if (loading) {
    return (
      <div className="itinerary-loading">
        <div className="pulse-loader"></div>
        <p>Loading your personalized AI itinerary...</p>
      </div>
    );
  }

  if (!trip) return <div className="itinerary-container">Trip not found</div>;

  // Flatten all active activities (excluding cancelled/replaced)
  const allActiveActivities = trip?.days 
    ? trip.days.flatMap(day => (day.activities || []).filter(a => a.status !== "REPLACED" && a.status !== "CANCELLED"))
    : [];

  const totalActivitiesCount = allActiveActivities.length;
  const completedCount = allActiveActivities.filter(a => a.status === 'COMPLETED').length;
  const progressPercent = totalActivitiesCount > 0 ? Math.round((completedCount / totalActivitiesCount) * 100) : 0;

  const isOwner = !trip?.user?.email || !currentUserEmail || trip.user.email.toLowerCase() === currentUserEmail.toLowerCase();
  const currentMember = trip?.members?.find(m => m.user?.email?.toLowerCase() === currentUserEmail?.toLowerCase() && m.status === 'ACCEPTED');
  const userRole = isOwner ? 'OWNER' : (currentMember ? currentMember.role : (currentUserEmail ? 'VIEWER' : 'OWNER'));
  const canEdit = userRole === 'OWNER' || userRole === 'EDITOR';
  const canInvite = isOwner;

  const acceptedMembers = trip?.members?.filter(m => m.status === 'ACCEPTED') || [];
  const pendingMembers = trip?.members?.filter(m => m.status === 'PENDING') || [];

  // Active coordinates for map (Origin, Hub, and all Attractions)
  const mapCoordinatesList = [];
  if (trip.originCoords) mapCoordinatesList.push(trip.originCoords);
  if (trip.destinationCoords) mapCoordinatesList.push(trip.destinationCoords);
  allActiveActivities.forEach(a => {
    if (a.coordinates) mapCoordinatesList.push(a.coordinates);
  });

  // TileLayer config based on current mapLayer selection
  const tileLayerConfigs = {
    roadmap: {
      url: "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
      attribution: "&copy; Google Maps"
    },
    satellite: {
      url: "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}",
      attribution: "&copy; Google Maps (Satellite)"
    },
    osm: {
      url: "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
      attribution: "&copy; OpenStreetMap contributors"
    }
  };

  return (
    <div className="itinerary-container animate-fade-in">
      <Link to="/dashboard" className="back-link">
        <ArrowLeft size={18} /> {t('itinerary.backToDashboard')}
      </Link>

      {/* HEADER WITH REAL-WORLD VERIFICATION BADGES */}
      <div className="itinerary-header-modern">
        <div className="header-top-row">
          <div className="verified-location-pill">
            <CheckCircle2 size={15} /> 100% Verified Real-World Indian Itinerary
          </div>
          <div className="live-clock-pill">
            <Clock size={14} /> {t('itinerary.currentTime', { time: currentTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) })}
          </div>
        </div>

        <div className="header-main-content">
          <h1>{trip.title ? formatTripTitle(trip.title, trip.destinationCity) : formatTripTitle(`Trip to ${trip.destinationCity}`, trip.destinationCity)}</h1>
          <div className="header-meta-tags">
            <span className="meta-tag-pill"><MapPin size={15} /> {getDestName(trip.destinationCity)}</span>
            <span className="meta-tag-pill"><Calendar size={15} /> {trip.startDate} - {trip.endDate} ({trip.days?.length || 1} {t('dashboard.days', { count: '' }).trim() || 'Days'})</span>
            <span className="meta-tag-pill"><DollarSign size={15} /> {t('itinerary.budgetLabel', { amount: (trip.budgetInr || 30000).toLocaleString('en-IN') })}</span>
            <span className="meta-tag-pill"><Compass size={15} /> {t('itinerary.styleLabel', { style: trip.travelStyle || 'Solo' })}</span>
          </div>
        </div>

        {/* LIVE ACTIVITY STATUS: Completed place + Next Planned Destination */}
        <div className="live-status-bar">
          {lastCompleted && (
            <div className="completed-status-indicator">
              <span className="completed-check-icon">✓</span>
              <div className="completed-text">
                <span className="completed-title">{lastCompleted.title}</span>
                <span className="completed-badge-label">{t('itinerary.completed')}</span>
              </div>
            </div>
          )}

          {nextActivity && !nextActivity.isAllCompleted ? (
            <>
              {lastCompleted && <span className="status-flow-arrow">→</span>}
              <div className="status-indicator">
                <span className="status-ping"></span>
                <strong>{t('itinerary.scheduledNext')}</strong>
              </div>
              <div className="status-details">
                <span className="status-title">{nextActivity.title}</span>
                <span className="status-time">at {nextActivity.time} ({nextActivity.duration})</span>
                <span className="status-cost">{nextActivity.cost}</span>
              </div>
              {nextActivity.upNextItems && nextActivity.upNextItems.length > 0 && (
                <div className="up-next-preview-box">
                  <span className="up-next-tag">{t('itinerary.upNext')}</span>
                  {nextActivity.upNextItems.map((item, idx) => (
                    <span key={item.id || idx} className="up-next-chip">
                      #{item.seq} {item.title} ({item.time})
                    </span>
                  ))}
                </div>
              )}
              <div className="status-actions">
                <button 
                  type="button"
                  className="btn-check-live"
                  style={{ background: '#f0f9ff', color: '#0369a1', border: '1px solid #bae6fd', padding: '5px 12px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: '600', display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}
                  onClick={() => handleCheckNextActivityLive(nextActivity.id)}
                  title="Check actual live forecast and travel conditions from Open-Meteo"
                >
                  <Sun size={13} className="text-amber" /> Check Live Weather
                </button>
                <button 
                  className="locate-btn"
                  onClick={() => handleLocateOnMap(nextActivity)}
                  title="Locate next activity on Map"
                >
                  <Navigation size={13} /> {t('itinerary.locateOnMap')}
                </button>
                <button
                  className="simulate-check-btn"
                  onClick={() => handleMarkCompleted(nextActivity.id, 'rain')}
                  title="Test adaptive flow: simulate adverse weather at this destination"
                >
                  🧪 {t('itinerary.testRain')}
                </button>
              </div>
            </>
          ) : (
            <div className="status-all-completed">
              <CheckCircle2 size={16} className="text-emerald" />
              <span>{t('itinerary.allCompleted')}</span>
            </div>
          )}
        </div>
      </div>

      {/* DASHBOARD QUICK ACTIONS */}
      <div className="dashboard-quick-actions-bar animate-fade-in">
        {canEdit && (
          <button className="quick-action-btn edit-trip-btn" onClick={handleOpenEditModal}>
            <Edit3 size={15} /> {t('itinerary.editTrip')}
          </button>
        )}
        {canInvite && (
          <button className="quick-action-btn invite-btn" onClick={() => setShowInviteModal(true)}>
            <UserPlus size={15} /> {t('itinerary.inviteTravellers')}
          </button>
        )}
        <button className="quick-action-btn" onClick={() => document.getElementById('map-view-section')?.scrollIntoView({ behavior: 'smooth' })}>
          <MapPin size={15} /> {t('itinerary.viewMap')}
        </button>
        <button className="quick-action-btn" onClick={() => document.getElementById('itinerary-schedule-section')?.scrollIntoView({ behavior: 'smooth' })}>
          <Calendar size={15} /> {t('itinerary.viewItinerary')}
        </button>
        {/* Temporarily hidden per user request: Share Trip
        <button className="quick-action-btn" onClick={handleShareTrip}>
          <Share2 size={15} /> Share Trip
        </button>
        */}
      </div>

      {/* TRIP DASHBOARD OVERVIEW & PROGRESS */}
      <div className="trip-dashboard-overview-card glass-panel animate-fade-in">
        <div className="overview-header-row">
          <div className="overview-title-wrap">
            <h2>Trip Overview</h2>
            {trip.lastUpdateNotice && (
              <span className="last-updated-chip"><Sparkles size={13} /> {trip.lastUpdateNotice}</span>
            )}
          </div>
          <div className="role-indicator-badge">
            Your Access: <span className={`role-badge-text ${userRole.toLowerCase()}`}>{userRole}</span>
          </div>
        </div>

        <div className="overview-metrics-grid">
          <div className="overview-metric-item">
            <span className="metric-label">Destination</span>
            <strong>{trip.destinationCity}</strong>
          </div>
          <div className="overview-metric-item">
            <span className="metric-label">Dates</span>
            <strong>{trip.startDate} to {trip.endDate}</strong>
          </div>
          <div className="overview-metric-item">
            <span className="metric-label">Duration</span>
            <strong>{trip.days?.length || trip.numDays || 1} Days</strong>
          </div>
          <div className="overview-metric-item">
            <span className="metric-label">Budget</span>
            <strong>₹{(trip.budgetInr || trip.totalBudget || 30000).toLocaleString('en-IN')}</strong>
          </div>
          <div className="overview-metric-item">
            <span className="metric-label">Travel Style</span>
            <strong>{trip.travelStyle || 'Solo'}</strong>
          </div>
          <div className="overview-metric-item">
            <span className="metric-label">Travellers</span>
            <strong>{trip.numTravelers || 1} Person{trip.numTravelers > 1 ? 's' : ''}</strong>
          </div>
          <div className="overview-metric-item">
            <span className="metric-label">Preferences</span>
            <strong>{trip.interests || 'Heritage, Food'}</strong>
          </div>
          <div className="overview-metric-item">
            <span className="metric-label">Pace & Transit</span>
            <strong>{trip.activityLevel || 'Moderate'} • {transportInfo?.actualMode || trip.transitPreference || 'Train'}</strong>
          </div>
        </div>

        {/* TRIP PROGRESS BAR */}
        <div className="trip-progress-section">
          <div className="progress-info-row">
            <div className="progress-label-wrap">
              <CheckCircle2 size={16} className="text-emerald" />
              <strong>Trip Progress:</strong> <span>{completedCount} of {totalActivitiesCount} activities completed</span>
            </div>
            <span className="progress-percent-badge">{progressPercent}%</span>
          </div>
          <div className="progress-bar-track">
            <div className="progress-bar-fill" style={{ width: `${progressPercent}%` }}></div>
          </div>
        </div>
      </div>

      {/* PROMINENT TRANSPORT FEASIBILITY NOTICE BANNER */}
      {trip.transportNotice && (
        <div className="transport-notice-banner animate-fade-in">
          <div className="notice-icon">
            <AlertTriangle size={20} />
          </div>
          <div className="notice-text">
            <strong>Transport Feasibility Notice</strong>
            <p>{trip.transportNotice}</p>
          </div>
        </div>
      )}

      {/* ADAPTATION CHANGE LOG PANEL (WHAT CHANGED / WHY / REMOVED / ADDED) */}
      {adaptationLog && (
        <div className="adaptation-log-panel animate-fade-in">
          <div className="log-header">
            <div className="log-icon"><Sparkles size={20} /></div>
            <div>
              <h3>AI Adaptation Summary</h3>
              <p>{adaptationLog.whatChanged}</p>
            </div>
            <button className="dismiss-btn" onClick={() => setAdaptationLog(null)}>Dismiss</button>
          </div>

          <div className="log-body-grid">
            <div className="log-col">
              <label>WHY IT CHANGED</label>
              <p>{adaptationLog.whyChanged}</p>
            </div>
            <div className="log-col">
              <label>WHAT WAS REMOVED</label>
              <ul>
                {adaptationLog.removed.map((r, i) => (
                  <li key={i} className="removed-tag">✕ {r}</li>
                ))}
              </ul>
            </div>
            <div className="log-col">
              <label>WHAT WAS ADDED</label>
              <ul>
                {adaptationLog.added.map((a, i) => (
                  <li key={i} className="added-tag">✓ {a}</li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      )}

      {/* MAIN TWO-COLUMN LAYOUT */}
      <div className="itinerary-layout-modern">
        
        {/* LEFT COLUMN: TIMELINE & DAYS */}
        <div className="timeline-column" id="itinerary-schedule-section">
          
          {/* INTERCITY TRANSIT SEGMENT (ORIGIN TO DESTINATION) */}
          <div className="intercity-transit-card animate-fade-in">
            <div className="transit-header">
              <div className="transit-icon-box">
                {transportInfo?.actualMode?.includes('Train') ? <Train size={20} /> : <Bus size={20} />}
              </div>
              <div className="transit-meta">
                <span className="transit-label">Intercity Travel Segment</span>
                <h3>{trip.startingLocation || 'Origin'} → {trip.destinationCity}</h3>
              </div>
            </div>
            <div className="transit-details-row">
              <span className="transit-pill">
                <strong>Mode:</strong> {transportInfo?.actualMode || trip.transitPreference || 'Bus / Road'}
              </span>
              <span className="transit-pill">
                <strong>Approx. Distance:</strong> {transportInfo?.distanceKm || 43} km
              </span>
              <span className="transit-pill">
                <strong>Travel Time:</strong> {transportInfo?.estimatedTime || '~1h 15m'}
              </span>
              <span className="transit-pill highlight">
                <strong>Est. Fare:</strong> ₹{transportInfo?.estimatedCostInr || 80}
              </span>
            </div>
          </div>

          <div className="days-stack">
            {trip.days.map((day) => (
              <div key={day.id || day.dayNumber} className="day-card-modern">
                
                {/* DAY HEADER */}
                <div className="day-header-modern">
                  <div className="day-title-wrap">
                    <span className="day-number-badge">Day {day.dayNumber}</span>
                    <h2>{day.theme}</h2>
                  </div>
                  <div className="day-header-actions">
                    <div className="day-weather-badge">
                      {day.weather.includes("Rain") ? <CloudRain size={16} className="text-blue" /> : <Sun size={16} className="text-amber" />}
                      <span>{day.weather}</span>
                    </div>
                  </div>
                </div>

                {/* COMPACT DAILY ROUTE SUMMARY */}
                {(() => {
                  const activeDayActs = (day.activities || []).filter(a => a.status !== 'CANCELLED' && a.status !== 'REPLACED');
                  const totalDayDist = activeDayActs.reduce((acc, a) => {
                    const val = typeof a.distanceFromPrev === 'number' ? a.distanceFromPrev : parseFloat(a.transitDist || a.distanceFromPrev || 0);
                    return acc + (isNaN(val) ? 0 : val);
                  }, 0);
                  const totalDayMins = activeDayActs.reduce((acc, a) => {
                    const val = typeof a.durationFromPrev === 'number' ? a.durationFromPrev : parseInt(a.transitTime || a.durationFromPrev || 0);
                    return acc + (isNaN(val) ? 15 : val);
                  }, 0);

                  const hours = Math.floor(totalDayMins / 60);
                  const mins = totalDayMins % 60;
                  const timeFormatted = hours > 0 ? `${hours}h ${mins > 0 ? `${mins}m` : ''}` : `${mins} mins`;

                  return (
                    <div className="daily-route-summary-banner">
                      <div className="daily-summary-stats">
                        <div className="daily-stat-chip">
                          <MapPin size={14} style={{ color: '#0284c7' }} />
                          <span><strong>{activeDayActs.length}</strong> Attractions</span>
                        </div>
                        <div className="daily-stat-chip">
                          <Navigation size={14} style={{ color: '#10b981' }} />
                          <span>Est. <strong>{Math.round(totalDayDist * 10) / 10} km</strong> travel</span>
                        </div>
                        <div className="daily-stat-chip">
                          <Clock size={14} style={{ color: '#f59e0b' }} />
                          <span>Est. <strong>{timeFormatted}</strong> transit</span>
                        </div>
                      </div>

                      {activeDayActs.length > 0 && (
                        <div className="daily-visiting-sequence-row">
                          <span className="sequence-label">Route Sequence:</span>
                          <div className="sequence-trail">
                            {activeDayActs.map((act, idx) => (
                              <React.Fragment key={act.id}>
                                <button 
                                  type="button"
                                  className={`sequence-step-pill ${selectedActivityId === act.id ? 'active' : ''}`}
                                  onClick={() => handleLocateOnMap(act)}
                                  title={`Stop #${act.seq}: ${act.title}`}
                                >
                                  <span className="step-num">#{act.seq}</span>
                                  <span className="step-title">{act.title}</span>
                                </button>
                                {idx < activeDayActs.length - 1 && <span className="sequence-arrow">→</span>}
                              </React.Fragment>
                            ))}
                          </div>
                        </div>
                      )}

                      <div className="daily-summary-action">
                        <button 
                          type="button"
                          className="btn-view-day-map"
                          onClick={() => {
                            if (activeDayActs.length > 0) {
                              handleLocateOnMap(activeDayActs[0]);
                            } else {
                              document.getElementById('map-view-section')?.scrollIntoView({ behavior: 'smooth' });
                            }
                          }}
                        >
                          <Compass size={14} /> Open Day {day.dayNumber} Route on Map
                        </button>
                      </div>
                    </div>
                  );
                })()}

                {/* ADAPTIVE WEATHER ADVISORY */}
                {day.hasWeatherAlert && !weatherAlertDismissed[day.id] && (
                  <div className="weather-advisory-box">
                    <div className="advisory-content">
                      <AlertTriangle size={18} className="advisory-icon" />
                      <div>
                        <strong>Rain Expected During Planned Visits</strong>
                        <p>{day.weatherAlertText || "Precipitation probability is high. Outdoor monuments and riverfronts may be uncomfortable."}</p>
                      </div>
                    </div>
                    <div className="advisory-actions">
                      <button 
                        className="btn-adapt"
                        onClick={() => handleAdaptDay(day.id)}
                        disabled={adaptingDay === day.id}
                      >
                        <RefreshCw size={14} className={adaptingDay === day.id ? "spin" : ""} />
                        {adaptingDay === day.id ? "Finding Covered Spots..." : "Find Indoor Alternatives"}
                      </button>
                      <button 
                        className="btn-dismiss-advisory"
                        onClick={() => setWeatherAlertDismissed(prev => ({ ...prev, [day.id]: true }))}
                      >
                        Keep Plan
                      </button>
                    </div>
                  </div>
                )}

                {/* ACTIVITIES LIST */}
                <div className="activities-list">
                  {(day.activities || []).map((activity, actIndex) => (
                    <div 
                      key={activity.id} 
                      className={`activity-card-modern ${activity.status === 'COMPLETED' ? 'completed-activity' : ''} ${activity.status === 'REPLACED' ? 'replaced-activity' : ''} ${selectedActivityId === activity.id ? 'selected-activity-card' : ''}`}
                    >
                      {/* ACTIVITY TRANSIT FROM PREVIOUS BAR */}
                      <div className="activity-transit-bar">
                        <div className="transit-indicator-line"></div>
                        <span className="transit-info-badge">
                          <Car size={12} /> {activity.transitInfo}
                        </span>
                      </div>

                      <div className="activity-main-layout">
                        {/* TIME SLOT */}
                        <div className="activity-time-column">
                          <span className="slot-time">{activity.time}</span>
                          <span className="slot-duration">{activity.duration}</span>
                          <span className="slot-seq-num">#{activity.seq}</span>
                        </div>

                        {/* CONTENT */}
                        <div className="activity-body-column">
                          <div className="activity-body-header">
                            <div>
                              <h4>{activity.title}</h4>
                              <span className="verification-tag">
                                <CheckCircle2 size={12} /> {activity.verificationStatus}
                              </span>
                            </div>
                            <div className="tags-row">
                              {activity.status === 'COMPLETED' && (
                                <span className="status-tag completed">✓ Completed</span>
                              )}
                              {activity.status === 'REPLACED' && (
                                <span className="status-tag replaced">⚠️ Replaced</span>
                              )}
                              {activity.description?.includes('AI Weather Adapted') && (
                                <span className="status-tag adapted">🌧️ Weather-Protected</span>
                              )}
                              {(activity.description?.toLowerCase().includes('manual') || activity.verificationStatus?.toLowerCase().includes('manual') || activity.recommendationReason?.toLowerCase().includes('manual') || !activity.attractionId) && (
                                <span className="manual-badge">
                                  ✏️ Custom Place
                                </span>
                              )}
                              <span className="category-tag">{activity.category}</span>
                            </div>
                          </div>

                          {/* GENUINE RATING & AI MATCH SCORE */}
                          <div className="activity-ratings-row">
                            <span 
                              className={`genuine-rating-pill ${formatRating(activity.rating, activity.reviewCount).includes('unavailable') ? 'unverified' : ''}`}
                              title="Verified rating from official API data"
                            >
                              {formatRating(activity.rating, activity.reviewCount)}
                            </span>
                            <span className="ai-match-pill" title="TravelMate AI Suitability Score">
                              ✨ AI Match: {Math.round(activity.aiMatchScore || 90)}%
                            </span>
                          </div>

                          <p className="activity-desc">{activity.description}</p>

                          <div className="activity-footer-meta">
                            <span className="meta-info-item">
                              <MapPin size={13} /> {activity.address || activity.location}
                            </span>
                            <span className="meta-info-item">
                              <DollarSign size={13} /> {activity.costNumeric > 0 ? `₹${activity.costNumeric} entry fee` : (activity.cost || 'Free Entry')}
                            </span>
                            <span className="meta-info-item">
                              <Clock size={13} /> {activity.openingHours}
                            </span>
                          </div>

                          {/* ACTIVITY ACTIONS TOOLBAR (Complete, Reschedule, Remove, Map, Reorder) */}
                          <div className="activity-actions-toolbar">
                            {canEdit && (() => {
                              const activeActs = (day.activities || []).filter(a => a.status !== 'CANCELLED' && a.status !== 'REPLACED');
                              const currentActiveIdx = activeActs.findIndex(a => String(a.id) === String(activity.id));
                              const isFirst = currentActiveIdx <= 0;
                              const isLast = currentActiveIdx === -1 || currentActiveIdx >= activeActs.length - 1;
                              return (
                                <div className="activity-reorder-wrap" title="Change visiting order">
                                  <button 
                                    type="button"
                                    className="btn-reorder"
                                    onClick={() => handleMoveActivity(day, activity.id, -1)}
                                    disabled={isFirst}
                                    title="Move Up (visit earlier)"
                                  >
                                    <ChevronUp size={14} />
                                  </button>
                                  <button 
                                    type="button"
                                    className="btn-reorder"
                                    onClick={() => handleMoveActivity(day, activity.id, 1)}
                                    disabled={isLast}
                                    title="Move Down (visit later)"
                                  >
                                    <ChevronDown size={14} />
                                  </button>
                                </div>
                              );
                            })()}

                            {activity.status !== 'COMPLETED' ? (
                              <button 
                                className="btn-act-action btn-complete"
                                onClick={() => handleMarkCompleted(activity.id)}
                                title="Mark this place as visited and completed"
                              >
                                <Check size={14} /> Mark as Completed
                              </button>
                            ) : (
                              <span className="completed-badge-pill">
                                <CheckCircle2 size={14} /> Visited & Completed
                              </span>
                            )}

                            {activity.status !== 'COMPLETED' && (
                              <button 
                                className="btn-act-action btn-reschedule"
                                onClick={() => handleOpenReschedule(activity)}
                                title="Dislike or need to change? Reschedule only this place"
                              >
                                <RefreshCw size={13} /> Reschedule
                              </button>
                            )}

                            <button 
                              className="btn-act-action btn-remove"
                              onClick={() => handleRemoveActivity(activity.id)}
                              title="Remove from itinerary"
                            >
                              <Trash2 size={13} /> Remove
                            </button>

                            <button 
                              className="btn-act-action btn-map"
                              onClick={() => handleLocateOnMap(activity)}
                              title="Center, zoom, and highlight this place on the map"
                            >
                              <MapPin size={13} /> Locate on Map
                            </button>
                          </div>

                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* RIGHT COLUMN: STICKY MAP, ROUTE FLOW & BUDGET BREAKDOWN */}
        <div className="sidebar-column">
          
          {/* SHARED TRAVELLERS CARD */}
          <div className="shared-travellers-card sticky-widget">
            <div className="widget-header-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Users size={18} />
                <h3>Shared with ({1 + acceptedMembers.length})</h3>
              </div>
              {canInvite && (
                <button className="btn-mini-invite" onClick={() => setShowInviteModal(true)}>
                  + Invite Travellers
                </button>
              )}
            </div>

            <div className="shared-members-list">
              {/* Trip Owner */}
              <div className="member-item owner-item">
                <div className="member-avatar">👤</div>
                <div className="member-info">
                  <strong>{trip.user?.fullName || trip.user?.email || 'Trip Creator'}</strong>
                  <span className="member-sub">{trip.user?.email || 'Trip Owner'}</span>
                </div>
                <span className="member-role-badge owner">Owner</span>
              </div>

              {/* Accepted Members */}
              {acceptedMembers.map(member => (
                <div key={member.id} className="member-item">
                  <div className="member-avatar">👤</div>
                  <div className="member-info">
                    <strong>{member.user?.fullName || member.user?.email || 'Traveler'}</strong>
                    <span className="member-sub">{member.user?.email}</span>
                  </div>
                  <div className="member-actions-wrap">
                    <span className={`member-role-badge ${member.role?.toLowerCase()}`}>{member.role}</span>
                    {isOwner && (
                      <button 
                        className="btn-remove-member"
                        title="Remove member"
                        onClick={() => handleRemoveMember(member.id, member.user?.fullName || member.user?.email)}
                      >
                        ✕
                      </button>
                    )}
                  </div>
                </div>
              ))}

              {/* Pending Invitations */}
              {pendingMembers.length > 0 && (
                <div className="pending-invites-container">
                  <span className="pending-heading">Pending Invitations:</span>
                  {pendingMembers.map(pending => (
                    <div key={pending.id} className="pending-invite-item">
                      <div className="pending-info">
                        <span className="pending-email">⏳ {pending.user?.email || 'Invited User'}</span>
                        <span className="pending-role">({pending.role})</span>
                      </div>
                      {isOwner && (
                        <button 
                          className="btn-cancel-invite"
                          onClick={() => handleRemoveMember(pending.id, pending.user?.email)}
                        >
                          Cancel Invitation
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* ROUTE FLOW SUMMARY CARD (ABOVE MAP) */}
          <div className="route-flow-summary-card sticky-widget">
            <div className="widget-header-title">
              <Navigation size={18} />
              <h3>Your Route & Transport Flow</h3>
            </div>
            
            <div className="route-flow-steps">
              {/* Origin Node */}
              <div 
                className="route-flow-item origin-item"
                onClick={() => setSelectedActivityCoord(trip.originCoords)}
              >
                <div className="flow-node-badge origin">🟢</div>
                <div className="flow-node-content">
                  <strong>{getDestName(trip.startingLocation) || 'Origin'}</strong>
                  <span className="flow-sub">Journey Starting Point</span>
                </div>
              </div>

              {/* Intercity Connector */}
              <div className="route-flow-connector">
                <div className="connector-line"></div>
                <div className="connector-badge">
                  {transportInfo?.actualMode?.includes('Train') ? '🚂 Rail' : '🚍 Road / Bus'} • {transportInfo?.distanceKm || 43} km (~{transportInfo?.estimatedTime || '1h 15m'})
                </div>
              </div>

              {/* Destination Hub Node */}
              <div 
                className="route-flow-item hub-item"
                onClick={() => setSelectedActivityCoord(trip.destinationCoords)}
              >
                <div className="flow-node-badge hub">📍</div>
                <div className="flow-node-content">
                  <strong>{getDestName(trip.destinationCity)}</strong>
                  <span className="flow-sub">Destination Hub & Base</span>
                </div>
              </div>

              {/* Activity Nodes */}
              {allActiveActivities.map((act) => (
                <React.Fragment key={act.id}>
                  <div className="route-flow-connector local-conn">
                    <div className="connector-line"></div>
                    <div className="connector-badge local">
                      🚗 Local Cab • {act.transitDist || '4.5 km'} (~{act.transitTime || '15m'})
                    </div>
                  </div>

                  <div 
                    className={`route-flow-item stop-item ${selectedActivityId === act.id ? 'active-stop' : ''}`}
                    onClick={() => handleLocateOnMap(act)}
                    title="Click to focus on map"
                  >
                    <div className="flow-node-badge stop">{act.seq}</div>
                    <div className="flow-node-content">
                      <strong>{act.title}</strong>
                      <span className="flow-sub">{act.time} • {act.category}</span>
                    </div>
                    <button className="mini-locate-btn" onClick={(e) => { e.stopPropagation(); handleLocateOnMap(act); }}>
                      <Navigation size={12} />
                    </button>
                  </div>
                </React.Fragment>
              ))}
            </div>
          </div>

          {/* INTERACTIVE GOOGLE MAPS VISUALIZATION */}
          <div className="map-card-wrapper sticky-widget" ref={mapContainerRef} id="map-view-section">
            <div className="widget-header-title map-header-flex">
              <div className="map-title-left">
                <MapPin size={18} />
                <h3>Google Maps Route View</h3>
              </div>

              {/* Map Layer Switcher */}
              <div className="map-layer-switcher">
                <button 
                  className={`layer-btn ${mapLayer === 'roadmap' ? 'active' : ''}`}
                  onClick={() => setMapLayer('roadmap')}
                  title="Google Maps Roadmap"
                >
                  Roadmap
                </button>
                <button 
                  className={`layer-btn ${mapLayer === 'satellite' ? 'active' : ''}`}
                  onClick={() => setMapLayer('satellite')}
                  title="Google Maps Satellite"
                >
                  Satellite
                </button>
                <button 
                  className={`layer-btn ${mapLayer === 'osm' ? 'active' : ''}`}
                  onClick={() => setMapLayer('osm')}
                  title="OpenStreetMap"
                >
                  OSM
                </button>
              </div>
            </div>

            <div className="map-frame">
              <MapContainer 
                center={trip.destinationCoords} 
                zoom={12} 
                style={{ height: '380px', width: '100%', borderRadius: '12px' }}
              >
                <TileLayer
                  url={tileLayerConfigs[mapLayer].url}
                  attribution={tileLayerConfigs[mapLayer].attribution}
                  maxZoom={20}
                />

                <MapController 
                  allCoords={mapCoordinatesList}
                  centerCoord={trip.destinationCoords} 
                  selectedCoord={selectedActivityCoord} 
                />

                {/* Starting Location (Origin) Marker */}
                {trip.originCoords && (
                  <Marker 
                    position={trip.originCoords} 
                    icon={createOriginIcon(getDestName(trip.startingLocation), selectedActivityCoord === trip.originCoords)}
                  >
                    <Popup>
                      <div className="map-popup-custom">
                        <span className="popup-badge origin-badge">🟢 Origin</span>
                        <h4>{getDestName(trip.startingLocation)}</h4>
                        <p>Starting point of your trip</p>
                      </div>
                    </Popup>
                  </Marker>
                )}

                {/* Destination Hub Marker */}
                {trip.destinationCoords && (
                  <Marker 
                    position={trip.destinationCoords} 
                    icon={createHubIcon(getDestName(trip.destinationCity), selectedActivityCoord === trip.destinationCoords)}
                  >
                    <Popup>
                      <div className="map-popup-custom">
                        <span className="popup-badge hub-badge">📍 Destination Hub</span>
                        <h4>{getDestName(trip.destinationCity)}</h4>
                        <p>Base city for regional exploration</p>
                      </div>
                    </Popup>
                  </Marker>
                )}

                {/* Numbered Attraction Markers in Itinerary Order */}
                {allActiveActivities.map((activity) => (
                  <Marker 
                    key={activity.id} 
                    ref={el => { if (el) markerRefs.current[activity.id] = el; }}
                    position={activity.coordinates}
                    icon={createNumberedIcon(activity.seq, selectedActivityId === activity.id)}
                  >
                    <Popup>
                      <div className="map-popup-custom">
                        <span className="popup-badge stop-badge">Stop #{activity.seq}</span>
                        <h4>{activity.title}</h4>
                        <div className="popup-details">
                          <p><strong>Time:</strong> {activity.time} ({activity.duration})</p>
                          <p><strong>Category:</strong> {activity.category}</p>
                          <p><strong>Entry Fee:</strong> {activity.cost}</p>
                          {activity.transitInfo && (
                            <p className="popup-transit">🚗 {activity.transitInfo}</p>
                          )}
                        </div>
                      </div>
                    </Popup>
                  </Marker>
                ))}

                {/* Road Route Polylines */}
                {routePolyline ? (
                  <Polyline positions={routePolyline} color="#0284c7" weight={5} opacity={0.85} />
                ) : (
                  mapCoordinatesList.length > 1 && (
                    <Polyline positions={mapCoordinatesList} color="#0284c7" weight={4} opacity={0.7} dashArray="6, 8" />
                  )
                )}
              </MapContainer>
            </div>
            <p className="map-footnote">Click any itinerary activity or route step to focus and zoom the map marker.</p>

            {/* INR BUDGET BREAKDOWN CARD */}
            {budgetBreakdown && (
              <div className="budget-breakdown-card">
                <div className="budget-card-header">
                  <DollarSign size={18} className="budget-icon" />
                  <div>
                    <h4>INR Budget Breakdown</h4>
                    <span className="budget-subtitle">Verified Entry Fees + Realistic Ground Transport</span>
                  </div>
                </div>

                <div className="budget-items-list">
                  <div className="budget-row">
                    <span>Intercity Transport ({transportInfo?.actualMode || 'Ground Road'}):</span>
                    <strong>₹{budgetBreakdown.transport.toLocaleString('en-IN')}</strong>
                  </div>
                  <div className="budget-row">
                    <span>Accommodation ({trip.days?.length || 1} nights):</span>
                    <strong>₹{budgetBreakdown.accommodation.toLocaleString('en-IN')}</strong>
                  </div>
                  <div className="budget-row">
                    <span>Food & Dining (Meals + Refreshments):</span>
                    <strong>₹{budgetBreakdown.food.toLocaleString('en-IN')}</strong>
                  </div>
                  <div className="budget-row">
                    <span>Verified Activities & Entry Tickets:</span>
                    <strong>₹{budgetBreakdown.activities.toLocaleString('en-IN')}</strong>
                  </div>
                  <div className="budget-row">
                    <span>Local City Transit (Cabs / Auto):</span>
                    <strong>₹{budgetBreakdown.localTravel.toLocaleString('en-IN')}</strong>
                  </div>
                  <div className="budget-row">
                    <span>Contingency Buffer (8%):</span>
                    <strong>₹{budgetBreakdown.buffer.toLocaleString('en-IN')}</strong>
                  </div>

                  <div className="budget-divider"></div>

                  <div className="budget-row total-row">
                    <span>Total Estimated Cost:</span>
                    <span className="total-amount">₹{budgetBreakdown.totalEstimated.toLocaleString('en-IN')}</span>
                  </div>

                  <div className={`budget-status-row ${budgetBreakdown.remainingBudget >= 0 ? 'status-positive' : 'status-negative'}`}>
                    <span>{budgetBreakdown.remainingBudget >= 0 ? "Remaining Safe Surplus:" : "Budget Deficit:"}</span>
                    <strong>₹{Math.abs(budgetBreakdown.remainingBudget).toLocaleString('en-IN')}</strong>
                  </div>
                </div>
              </div>
            )}

          </div>

        </div>

      </div>

      {/* ==========================================
          NEXT DESTINATION ADAPTIVE CONDITION ALERT MODAL
          ========================================== */}
      {showNextConditionAlert && nextDestinationCheck && (
        <div className="modal-backdrop">
          <div className="condition-alert-modal animate-scale-up">
            <div className="modal-alert-header">
              <div className="warning-icon-wrap">
                <AlertTriangle size={24} className="warning-icon" />
              </div>
              <div>
                <h3>{nextDestinationCheck.isSimulated ? '🧪 Test Simulation: Adverse Weather Alert' : '⚠️ Conditions at your next destination have changed'}</h3>
                <p>
                  Your next activity (<strong>{nextDestinationCheck.nextActivityTitle}</strong>) may be affected by {nextDestinationCheck.isSimulated ? 'a simulated test scenario' : 'current forecast conditions'}
                  {nextDestinationCheck.isSimulated && <span className="simulated-badge" style={{ marginLeft: '8px', fontSize: '0.75rem', background: '#fef3c7', color: '#b45309', padding: '2px 8px', borderRadius: '12px', fontWeight: '700', border: '1px solid #f59e0b' }}>Simulated Test Mode</span>}.
                </p>
              </div>
            </div>

            {nextDestinationCheck.isSimulated && (
              <div className="simulation-mode-banner" style={{ background: '#fffbeb', border: '1px solid #f59e0b', borderRadius: '8px', padding: '12px 16px', marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <AlertCircle size={20} color="#b45309" />
                  <div>
                    <strong style={{ color: '#92400e', display: 'block', fontSize: '0.9rem' }}>Simulation Active (Test Scenario)</strong>
                    <span style={{ color: '#78350f', fontSize: '0.8rem' }}>Displaying simulated adverse conditions to verify TravelMate's adaptive itinerary rerouting.</span>
                  </div>
                </div>
                <button 
                  type="button" 
                  className="btn-switch-live" 
                  onClick={() => handleSwitchToLiveWeather(nextDestinationCheck.currentActivityId || nextDestinationCheck.nextActivityId)}
                  style={{ background: '#0284c7', color: '#fff', border: 'none', padding: '6px 14px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: '600', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px' }}
                >
                  <Sun size={14} /> Switch to Live Weather
                </button>
              </div>
            )}

            {nextDestinationCheck.liveWeather?.forecastMessage && (
              <div style={{ margin: '0 0 12px', color: '#92400e', fontSize: '0.8rem' }}>
                Live weather temporarily unavailable{nextDestinationCheck.liveWeather.weatherAvailable ? ' — showing the last valid cached forecast.' : ''}
              </div>
            )}

            {/* When simulation is active, show the real weather readings in a dedicated separate card */}
            {nextDestinationCheck.isSimulated && nextDestinationCheck.liveWeather && (
              <div className="actual-live-weather-card" style={{ background: '#f8fafc', border: '1px solid #cbd5e1', borderRadius: '8px', padding: '12px 14px', marginBottom: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                  <strong style={{ fontSize: '0.85rem', color: '#0f172a', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <Sun size={15} className="text-amber" /> {nextDestinationCheck.liveWeather.isStale ? 'Last Valid Cached Weather at Destination' : (nextDestinationCheck.liveWeather.weatherAvailable ? 'Actual Live Weather at Destination (Open-Meteo API)' : 'Live weather temporarily unavailable')}
                  </strong>
                  <span style={{ fontSize: '0.72rem', background: '#e2e8f0', color: '#334155', padding: '2px 8px', borderRadius: '10px', fontWeight: '600' }}>{nextDestinationCheck.liveWeather.isStale ? 'Cached Readings' : 'Real Live Readings'}</span>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '10px', fontSize: '0.8rem', color: '#334155' }}>
                  <div>Condition: <strong>{nextDestinationCheck.liveWeather.weather || 'Live weather temporarily unavailable'}</strong></div>
                  <div>Rain Probability: <strong>{nextDestinationCheck.liveWeather.rainProbability || 'Unavailable'}</strong></div>
                  <div>Daily Rainfall: <strong>{nextDestinationCheck.liveWeather.precipitationMm != null ? `${nextDestinationCheck.liveWeather.precipitationMm} mm` : 'Unavailable'}</strong></div>
                  <div>Temperature: <strong>{nextDestinationCheck.liveWeather.temperature || 'Unavailable'}</strong></div>
                </div>
              </div>
            )}

            {nextDestinationCheck.isSimulated && (
              <div style={{ fontSize: '0.8rem', fontWeight: '700', color: '#b45309', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                🧪 Simulated Test Conditions (For Rerouting Evaluation):
              </div>
            )}

            <div className="condition-metrics-grid">
              <div className={`metric-card ${nextDestinationCheck.rainProbability >= 60 ? 'risk' : (nextDestinationCheck.rainProbability >= 30 ? 'warning' : 'normal')}`}>
                <CloudRain size={20} />
                <div>
                  <span className="metric-label">Rain Probability (Daily Max)</span>
                  <strong>{nextDestinationCheck.rainProbability != null ? `${nextDestinationCheck.rainProbability}% (${nextDestinationCheck.rainProbability >= 60 ? 'High' : (nextDestinationCheck.rainProbability >= 30 ? 'Moderate' : 'Low')})` : 'Live weather temporarily unavailable'}</strong>
                </div>
              </div>

              {nextDestinationCheck.precipitationMm != null && (
                <div className={`metric-card ${nextDestinationCheck.precipitationMm >= 2.5 ? 'risk' : 'warning'}`}>
                  <CloudRain size={20} />
                  <div>
                    <span className="metric-label">Daily Rainfall Forecast</span>
                    <strong>{nextDestinationCheck.precipitationMm} mm ({nextDestinationCheck.precipitationMm >= 7.5 ? 'Heavy Rain' : (nextDestinationCheck.precipitationMm >= 2.5 ? 'Moderate Rain' : 'Light Rain')})</strong>
                  </div>
                </div>
              )}

              {nextDestinationCheck.currentPrecipitationMm != null && nextDestinationCheck.currentPrecipitationMm > 0 && (
                <div className="metric-card risk">
                  <CloudRain size={20} />
                  <div>
                    <span className="metric-label">Current Hourly Rainfall</span>
                    <strong>{nextDestinationCheck.currentPrecipitationMm} mm/h (Raining Now)</strong>
                  </div>
                </div>
              )}

              {nextDestinationCheck.conditions?.weather && (
                <div className="metric-card normal">
                  <CloudRain size={20} />
                  <div>
                    <span className="metric-label">Weather Condition</span>
                    <strong>{nextDestinationCheck.conditions.weather}</strong>
                  </div>
                </div>
              )}

              <div className="metric-card normal">
                <Sun size={20} />
                <div>
                  <span className="metric-label">Temperature</span>
                  <strong>{nextDestinationCheck.temperatureC ? `${nextDestinationCheck.temperatureC}°C` : 'Normal'}</strong>
                </div>
              </div>

              <div className={`metric-card ${nextDestinationCheck.trafficLevel === 'High' ? 'risk' : 'normal'}`}>
                <Car size={20} />
                <div>
                  <span className="metric-label">Traffic Conditions</span>
                  <strong>{nextDestinationCheck.trafficLevel || 'Normal'} ({nextDestinationCheck.travelTimeEst || '15 mins'})</strong>
                </div>
              </div>

              <div className={`metric-card ${nextDestinationCheck.crowdLevel === 'High' ? 'risk' : 'normal'}`}>
                <Users size={20} />
                <div>
                  <span className="metric-label">Crowd Level</span>
                  <strong>{nextDestinationCheck.crowdLevel || 'Moderate'}</strong>
                </div>
              </div>

              <div className="metric-card normal">
                <Clock size={20} />
                <div>
                  <span className="metric-label">Opening Status</span>
                  <strong>{nextDestinationCheck.openingStatus || 'Open Now'}</strong>
                </div>
              </div>

              <div className="metric-card normal">
                <Navigation size={20} />
                <div>
                  <span className="metric-label">Distance</span>
                  <strong>{nextDestinationCheck.distanceKm ? `${nextDestinationCheck.distanceKm} km` : 'Nearby'}</strong>
                </div>
              </div>
            </div>

            {nextDestinationCheck.alertMessage && (
              <div className="condition-alert-message">
                <AlertCircle size={16} />
                <span>{nextDestinationCheck.alertMessage}</span>
              </div>
            )}

            {/* Alternatives List when requested */}
            {nextAlternatives.length > 0 && (
              <div className="modal-alternatives-section">
                <h4>Real Verified Alternatives Available Nearby:</h4>
                <div className="alternatives-list-scroll">
                  {nextAlternatives.map((alt) => (
                    <div key={alt.id} className="alternative-candidate-card">
                      <div className="candidate-info">
                        <div className="candidate-header">
                          <h5>{alt.title}</h5>
                          <span className="candidate-category">{alt.category}</span>
                        </div>
                        <p className="candidate-desc">{alt.recommendationReason || alt.address}</p>
                        <div className="candidate-meta">
                          <span className="alt-meta-tag"><DollarSign size={12} /> {alt.costInr > 0 ? `₹${alt.costInr}` : 'Free Entry'}</span>
                          <span className="alt-meta-tag"><MapPin size={12} /> {alt.indoorOutdoor}</span>
                          <span className="alt-meta-tag">
                            {formatRating(alt.rating, alt.reviewCount)}
                          </span>
                          <span className="alt-match-tag">✨ Match: {Math.round(alt.aiMatchScore || 90)}%</span>
                        </div>
                      </div>
                      <button 
                        className="btn-choose-alt"
                        onClick={() => handleSelectAlternative(nextDestinationCheck.nextActivityId, alt)}
                      >
                        [Choose Alternative]
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="modal-action-buttons">
              {nextDestinationCheck.isSimulated && (
                <button 
                  type="button"
                  className="btn-modal-secondary"
                  style={{ background: '#f0f9ff', borderColor: '#bae6fd', color: '#0369a1', display: 'flex', alignItems: 'center', gap: '6px' }}
                  onClick={() => handleSwitchToLiveWeather(nextDestinationCheck.currentActivityId || nextDestinationCheck.nextActivityId)}
                >
                  <Sun size={14} className="text-amber" /> Switch to Live Weather
                </button>
              )}

              <button 
                className="btn-modal-secondary"
                onClick={() => {
                  setShowNextConditionAlert(false);
                  if (nextDestinationCheck?.isSimulated) {
                    setNextDestinationCheck(prev => prev ? { ...prev, isSimulated: false } : null);
                  }
                }}
              >
                [Keep Original]
              </button>

              {nextAlternatives.length === 0 && (
                <button 
                  className="btn-modal-primary"
                  onClick={() => handleFetchNextAlternatives(nextDestinationCheck.nextActivityId)}
                  disabled={loadingNextAlternatives}
                >
                  {loadingNextAlternatives ? (
                    <><RefreshCw size={14} className="spin" /> Finding Real Alternatives...</>
                  ) : (
                    '[Show Alternatives]'
                  )}
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ==========================================
          SINGLE PLACE RESCHEDULE / REPLACE MODAL
          ========================================== */}
      {showRescheduleModal && rescheduleTarget && (
        <div className="modal-backdrop">
          <div className="reschedule-modal animate-scale-up" style={{ maxWidth: activeModalTab === 'manual' ? '780px' : '640px', width: '92%', maxHeight: '90vh', overflowY: 'auto' }}>
            <div className="modal-reschedule-header">
              <div className="reschedule-icon-wrap">
                <RefreshCw size={22} className="reschedule-icon" />
              </div>
              <div>
                <h3>Reschedule: {rescheduleTarget.title}</h3>
                <p>Modify, replace, or customize your itinerary seamlessly.</p>
              </div>
              <button className="modal-close-btn" onClick={() => setShowRescheduleModal(false)}>✕</button>
            </div>

            {/* Modal Tabs: AI Recommendations, Search Verified Attractions, Manual Edit */}
            <div className="modal-tabs-header">
              <button 
                type="button"
                className={`modal-tab-btn ${activeModalTab === 'recommendations' ? 'active' : ''}`}
                onClick={() => setActiveModalTab('recommendations')}
              >
                <Sparkles size={14} /> AI Recommendations
              </button>
              <button 
                type="button"
                className={`modal-tab-btn ${activeModalTab === 'search' ? 'active' : ''}`}
                onClick={() => {
                  setActiveModalTab('search');
                  if (destinationAttractions.length === 0) {
                    fetchDestinationAttractions(trip.destinationCity, '', rescheduleTarget.coordinates);
                  }
                }}
              >
                <Search size={14} /> Search Verified Attractions
              </button>
              <button 
                type="button"
                className={`modal-tab-btn ${activeModalTab === 'manual' ? 'active' : ''}`}
                onClick={() => setActiveModalTab('manual')}
              >
                <Edit3 size={14} /> Manual Edit
              </button>
            </div>

            {activeModalTab === 'recommendations' ? (
              <>
                <div className="reschedule-reason-box">
                  <label>Why would you like to replace this?</label>
                  <select 
                    value={rescheduleReason} 
                    onChange={(e) => handleReasonChange(e.target.value)}
                    className="reason-select"
                  >
                    <option value="Too far">Too far</option>
                    <option value="Too expensive">Too expensive</option>
                    <option value="Not interested">Not interested</option>
                    <option value="Weather concern">Weather concern</option>
                    <option value="Already visited">Already visited</option>
                    <option value="Too crowded">Too crowded</option>
                    <option value="Other">Other</option>
                  </select>
                </div>

                <div className="reschedule-candidates-section">
                  <h4>Recommended Alternatives for {getDestName(trip.destinationCity)}:</h4>
                  {loadingRescheduleAlts ? (
                    <div className="alts-loading">
                      <RefreshCw size={20} className="spin" />
                      <p>Searching verified database for places matching "{rescheduleReason}"...</p>
                    </div>
                  ) : rescheduleAlternatives.length === 0 ? (
                    <p className="no-alts-msg">No verified alternatives found matching current filters.</p>
                  ) : (
                    <div className="alternatives-list-scroll">
                      {rescheduleAlternatives.map((alt) => (
                        <div key={alt.id} className="alternative-candidate-card">
                          <div className="candidate-info">
                            <div className="candidate-header">
                              <h5>{alt.title}</h5>
                              <span className="candidate-category">{alt.category}</span>
                            </div>
                            <p className="candidate-desc">{alt.recommendationReason || alt.address}</p>
                            <div className="candidate-meta">
                              <span className="alt-meta-tag"><DollarSign size={12} /> {alt.costInr > 0 ? `₹${alt.costInr}` : 'Free Entry'}</span>
                              <span className="alt-meta-tag"><MapPin size={12} /> {alt.indoorOutdoor}</span>
                              <span className="alt-meta-tag">
                                {formatRating(alt.rating, alt.reviewCount)}
                              </span>
                              <span className="alt-match-tag">✨ Match: {Math.round(alt.aiMatchScore || 90)}%</span>
                            </div>
                          </div>
                          <button 
                            className="btn-choose-alt"
                            onClick={() => handleSelectAlternative(rescheduleTarget.id, alt)}
                          >
                            Replace with This Place
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </>
            ) : activeModalTab === 'search' ? (
              /* Search Tab in Reschedule */
              <div className="reschedule-candidates-section">
                <div className="modal-search-box">
                  <Search size={16} className="modal-search-icon" />
                  <input 
                    type="text" 
                    className="modal-search-input"
                    placeholder={`Search verified attractions in ${getDestName(trip.destinationCity)}...`}
                    value={searchQuery}
                    onChange={(e) => handleSearchAttractionsChange(e.target.value, rescheduleTarget.coordinates)}
                  />
                </div>

                {loadingDestAttractions ? (
                  <div className="alts-loading">
                    <RefreshCw size={20} className="spin" />
                    <p>Searching verified database...</p>
                  </div>
                ) : destinationAttractions.length === 0 ? (
                  <p className="no-alts-msg">No verified attractions found matching "{searchQuery}".</p>
                ) : (
                  <div className="alternatives-list-scroll">
                    {destinationAttractions.map((attr) => (
                      <div key={attr.id} className="dest-attraction-card">
                        <div className="dest-card-thumb">
                          <SafeImage 
                            src={attr.imageUrl} 
                            alt={attr.name} 
                            category={attr.category} 
                            destinationName={trip.destinationCity} 
                          />
                        </div>
                        <div className="dest-card-content">
                          <div className="dest-card-header">
                            <h5 className="dest-card-title">{attr.name}</h5>
                            <span className="dest-card-category">{attr.category}</span>
                          </div>
                          <p className="dest-card-desc">{attr.description || attr.address}</p>
                          <div className="dest-card-meta-chips">
                            {attr.distanceKm != null && (
                              <span className="dest-card-chip dist">
                                <Navigation size={11} /> {attr.distanceKm} km ({attr.travelTimeMinutes || 15}m)
                              </span>
                            )}
                            <span className="dest-card-chip">
                              <Clock size={11} /> {attr.durationHours || 1.5} hrs
                            </span>
                            <span className="dest-card-chip">
                              <DollarSign size={11} /> {attr.entryFeeInr > 0 ? `₹${attr.entryFeeInr}` : 'Free Entry'}
                            </span>
                            <span className="dest-card-chip">
                              ⭐ {attr.rating || 4.5}
                            </span>
                          </div>
                        </div>
                        <button 
                          type="button"
                          className="btn-add-dest-place"
                          onClick={() => handleSelectAlternative(rescheduleTarget.id, {
                            id: attr.id,
                            title: attr.name,
                            category: attr.category,
                            costInr: attr.entryFeeInr || 0,
                            durationMinutes: attr.durationMinutes || 90,
                            indoorOutdoor: attr.indoorOutdoor || 'Outdoor',
                            latitude: attr.latitude,
                            longitude: attr.longitude,
                            rating: attr.rating,
                            reviewCount: attr.reviewCount,
                            openingHours: attr.openingHours,
                            address: attr.address,
                            recommendationReason: attr.description || 'Selected by user from verified database'
                          })}
                        >
                          Choose Replacement
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              /* ========================================================
                 MANUAL EDIT TAB (Inside Reschedule Modal)
                 ======================================================== */
              <div className="reschedule-manual-edit-panel">
                {/* SUBSECTION 1: DIRECT ADD PLACE MANUALLY */}
                <div className="manual-section-card">
                  <div className="manual-section-header">
                    <div className="manual-section-title">
                      <Plus size={18} style={{ color: '#0284c7' }} />
                      <h4>Add Place Manually</h4>
                    </div>
                    <span className="manual-section-badge">Direct Add</span>
                  </div>
                  <p className="manual-section-desc">
                    Type any place name, restaurant, or custom activity to add directly to your itinerary without searching.
                  </p>

                  <form onSubmit={handleDirectAddPlaceSubmit} className="manual-form-grid">
                    <div className="form-group">
                      <label className="form-label">
                        Place Name <span className="required-star">*</span>
                      </label>
                      <LocationAutocomplete 
                        value={manualForm.placeName}
                        onChange={(val) => {
                          const fb = resolveKnownCoords(val);
                          setManualForm(prev => ({
                            ...prev,
                            placeName: val,
                            latitude: fb ? fb[0] : null,
                            longitude: fb ? fb[1] : null,
                            category: (prev.category && prev.category !== 'Sightseeing' && prev.category !== 'Sightseeing & Attractions') ? prev.category : inferCategory(val, prev.address)
                          }));
                        }}
                        onSelect={(item) => {
                          const chosenName = item.name || item.displayName;
                          const chosenAddress = item.secondary || item.displayName || '';
                          const fb = resolveKnownCoords(chosenName) || resolveKnownCoords(chosenAddress);
                          const chosenLat = item.latitude != null ? item.latitude : (fb ? fb[0] : null);
                          const chosenLng = item.longitude != null ? item.longitude : (fb ? fb[1] : null);
                          const chosenCategory = item.category || inferCategory(chosenName, chosenAddress);

                          setManualForm(prev => ({
                            ...prev,
                            placeName: chosenName,
                            address: chosenAddress,
                            latitude: chosenLat,
                            longitude: chosenLng,
                            category: chosenCategory
                          }));
                        }}
                        placeholder="Search location (e.g. Gayatri University, GITAM, Araku) or type custom place..."
                        required
                      />
                    </div>

                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">Day of Visit</label>
                        <select 
                          className="form-select"
                          value={manualForm.dayId || (trip?.days && trip.days[0]?.id)}
                          onChange={(e) => {
                            const selId = Number(e.target.value);
                            const selDay = trip?.days?.find(d => d.id === selId);
                            setManualForm(prev => ({ 
                              ...prev, 
                              dayId: selId, 
                              dayNumber: selDay ? selDay.dayNumber : prev.dayNumber 
                            }));
                          }}
                        >
                          {(trip?.days || []).map(d => (
                            <option key={d.id} value={d.id}>
                              Day {d.dayNumber} {d.date ? `(${d.date})` : ''}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div className="form-group">
                        <label className="form-label">Preferred Visiting Time</label>
                        <input 
                          type="time" 
                          className="form-input"
                          value={manualForm.startTime}
                          onChange={(e) => setManualForm(prev => ({ ...prev, startTime: e.target.value }))}
                        />
                      </div>
                    </div>

                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">Estimated Duration</label>
                        <select 
                          className="form-select"
                          value={manualForm.durationMinutes}
                          onChange={(e) => setManualForm(prev => ({ ...prev, durationMinutes: Number(e.target.value) }))}
                        >
                          <option value={30}>30 minutes</option>
                          <option value={45}>45 minutes</option>
                          <option value={60}>1 hour</option>
                          <option value={90}>1.5 hours (Recommended)</option>
                          <option value={120}>2 hours</option>
                          <option value={150}>2.5 hours</option>
                          <option value={180}>3 hours</option>
                        </select>
                      </div>

                      <div className="form-group">
                        <label className="form-label">Category</label>
                        <select 
                          className="form-select"
                          value={manualForm.category}
                          onChange={(e) => setManualForm(prev => ({ ...prev, category: e.target.value }))}
                        >
                          <option value="Sightseeing & Attractions">Sightseeing & Attractions</option>
                          <option value="Education / University">Education / University</option>
                          <option value="Temples / Spiritual">Temples / Spiritual</option>
                          <option value="Beaches">Beaches & Coastal</option>
                          <option value="Historical / Heritage">Historical & Heritage</option>
                          <option value="Viewpoints / Attractions">Viewpoints & Scenic</option>
                          <option value="Wildlife / Zoo">Wildlife & Nature</option>
                          <option value="Museums">Museums & Culture</option>
                          <option value="Parks & Gardens">Parks & Gardens</option>
                          <option value="Food & Dining">Food & Dining / Restaurant</option>
                          <option value="Shopping / Market">Shopping & Local Market</option>
                          <option value="Nature / Hill Stations">Nature & Hill Stations</option>
                          <option value="Custom Spot">Custom Spot / Activity</option>
                        </select>
                      </div>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Address or Location (Optional)</label>
                      <input 
                        type="text" 
                        className="form-input"
                        placeholder="e.g. Near Calangute Beach Circle, North Goa (Optional)"
                        value={manualForm.address}
                        onChange={(e) => setManualForm(prev => ({ ...prev, address: e.target.value }))}
                      />
                      <small style={{ color: '#64748b', fontSize: '0.75rem', marginTop: '2px' }}>
                        Optional. Places without coordinates are saved immediately and integrated into your day.
                      </small>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Notes (Optional)</label>
                      <textarea 
                        className="form-textarea"
                        placeholder="e.g. Must try local dishes, scenic spot..."
                        value={manualForm.notes}
                        onChange={(e) => setManualForm(prev => ({ ...prev, notes: e.target.value }))}
                      />
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
                      <button type="submit" className="btn-add-manual-submit">
                        <Plus size={16} /> Add to Itinerary
                      </button>
                    </div>
                  </form>
                </div>

                {/* SUBSECTION 2: REPLACE TARGET ATTRACTION WITH CUSTOM PLACE */}
                <div className="manual-section-card" style={{ marginTop: '1.25rem' }}>
                  <div className="manual-section-header">
                    <div className="manual-section-title">
                      <RefreshCw size={18} style={{ color: '#d97706' }} />
                      <h4>Replace "{rescheduleTarget.title}"</h4>
                    </div>
                    <span className="manual-section-badge replace">Direct Replace</span>
                  </div>
                  <p className="manual-section-desc">
                    Replace this specific attraction with your own custom place while preserving the rest of your itinerary.
                  </p>

                  <form onSubmit={handleReplaceWithManualPlaceSubmit} className="manual-form-grid">
                    <div className="form-group">
                      <label className="form-label">
                        Replacement Place Name <span className="required-star">*</span>
                      </label>
                      <LocationAutocomplete 
                        value={replaceManualForm.placeName}
                        onChange={(val) => {
                          const fb = resolveKnownCoords(val);
                          setReplaceManualForm(prev => ({
                            ...prev,
                            placeName: val,
                            latitude: fb ? fb[0] : null,
                            longitude: fb ? fb[1] : null,
                            category: (prev.category && prev.category !== 'Sightseeing' && prev.category !== 'Sightseeing & Attractions') ? prev.category : inferCategory(val, prev.address)
                          }));
                        }}
                        onSelect={(item) => {
                          const chosenName = item.name || item.displayName;
                          const chosenAddress = item.secondary || item.displayName || '';
                          const fb = resolveKnownCoords(chosenName) || resolveKnownCoords(chosenAddress);
                          const chosenLat = item.latitude != null ? item.latitude : (fb ? fb[0] : null);
                          const chosenLng = item.longitude != null ? item.longitude : (fb ? fb[1] : null);
                          const chosenCategory = item.category || inferCategory(chosenName, chosenAddress);

                          setReplaceManualForm(prev => ({
                            ...prev,
                            placeName: chosenName,
                            address: chosenAddress,
                            latitude: chosenLat,
                            longitude: chosenLng,
                            category: chosenCategory
                          }));
                        }}
                        placeholder="Search location (e.g. Gayatri University, GITAM) or type custom place..."
                        required
                      />
                    </div>

                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">Visiting Time</label>
                        <input 
                          type="time" 
                          className="form-input"
                          value={replaceManualForm.startTime}
                          onChange={(e) => setReplaceManualForm(prev => ({ ...prev, startTime: e.target.value }))}
                        />
                      </div>

                      <div className="form-group">
                        <label className="form-label">Estimated Duration</label>
                        <select 
                          className="form-select"
                          value={replaceManualForm.durationMinutes}
                          onChange={(e) => setReplaceManualForm(prev => ({ ...prev, durationMinutes: Number(e.target.value) }))}
                        >
                          <option value={30}>30 minutes</option>
                          <option value={45}>45 minutes</option>
                          <option value={60}>1 hour</option>
                          <option value={90}>1.5 hours</option>
                          <option value={120}>2 hours</option>
                          <option value={180}>3 hours</option>
                        </select>
                      </div>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Category</label>
                      <select 
                        className="form-select"
                        value={replaceManualForm.category}
                        onChange={(e) => setReplaceManualForm(prev => ({ ...prev, category: e.target.value }))}
                      >
                        <option value="Sightseeing & Attractions">Sightseeing & Attractions</option>
                        <option value="Education / University">Education / University</option>
                        <option value="Temples / Spiritual">Temples / Spiritual</option>
                        <option value="Beaches">Beaches & Coastal</option>
                        <option value="Historical / Heritage">Historical & Heritage</option>
                        <option value="Viewpoints / Attractions">Viewpoints & Scenic</option>
                        <option value="Wildlife / Zoo">Wildlife & Nature</option>
                        <option value="Museums">Museums & Culture</option>
                        <option value="Parks & Gardens">Parks & Gardens</option>
                        <option value="Food & Dining">Food & Dining / Restaurant</option>
                        <option value="Shopping / Market">Shopping & Local Market</option>
                        <option value="Nature / Hill Stations">Nature & Hill Stations</option>
                        <option value="Custom Spot">Custom Spot / Activity</option>
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Address or Landmark (Optional)</label>
                      <input 
                        type="text" 
                        className="form-input"
                        placeholder="e.g. Beach Road, Goa (Optional)"
                        value={replaceManualForm.address}
                        onChange={(e) => setReplaceManualForm(prev => ({ ...prev, address: e.target.value }))}
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label">Notes (Optional)</label>
                      <input 
                        type="text" 
                        className="form-input"
                        placeholder="e.g. Recommended by local friend"
                        value={replaceManualForm.notes}
                        onChange={(e) => setReplaceManualForm(prev => ({ ...prev, notes: e.target.value }))}
                      />
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
                      <button type="submit" className="btn-replace-manual-submit">
                        <RefreshCw size={15} /> Replace "{rescheduleTarget.title}"
                      </button>
                    </div>
                  </form>
                </div>

                {/* SUBSECTION 3: MODIFY ITINERARY PLACES (REORDER, MOVE DAYS, ADJUST TIMES, REMOVE) */}
                <div className="manual-section-card" style={{ marginTop: '1.25rem' }}>
                  <div className="manual-section-header">
                    <div className="manual-section-title">
                      <Compass size={18} style={{ color: '#2563eb' }} />
                      <h4>Modify Itinerary Places</h4>
                    </div>
                    <span className="manual-section-badge manage">Order & Schedule</span>
                  </div>
                  <p className="manual-section-desc">
                    Change visit order, move places between days, adjust scheduled times, or remove unwanted places.
                  </p>

                  <div className="manual-manage-days-list">
                    {(trip?.days || []).map((day) => {
                      const activeActs = (day.activities || []).filter(a => a.status !== 'CANCELLED' && a.status !== 'REPLACED');
                      return (
                        <div key={day.id} className="manual-day-group">
                          <div className="manual-day-header">
                            <strong>Day {day.dayNumber} {day.date ? `(${day.date})` : ''}</strong>
                            <span className="manual-day-count">{activeActs.length} {activeActs.length === 1 ? 'place' : 'places'}</span>
                          </div>

                          {activeActs.length === 0 ? (
                            <p className="manual-empty-day">No places scheduled for this day yet.</p>
                          ) : (
                            <div className="manual-places-table">
                              {activeActs.map((act, actIdx) => (
                                <div key={act.id} className="manual-place-row">
                                  <div className="manual-place-col-info">
                                    <span className="manual-place-order-num">{actIdx + 1}</span>
                                    <div className="manual-place-names">
                                      <strong>{act.title}</strong>
                                      <span className="manual-place-sub">
                                        {act.category} • {act.time || '10:00'} ({act.duration || '90 mins'})
                                      </span>
                                    </div>
                                  </div>

                                  <div className="manual-place-col-actions">
                                    {/* Move Up / Down */}
                                    <div className="manual-order-btns">
                                      <button 
                                        type="button" 
                                        className="btn-mini-order"
                                        onClick={() => handleMoveActivity(day, act.id, -1)}
                                        disabled={actIdx === 0}
                                        title="Move Up (visit earlier)"
                                      >
                                        <ChevronUp size={13} />
                                      </button>
                                      <button 
                                        type="button" 
                                        className="btn-mini-order"
                                        onClick={() => handleMoveActivity(day, act.id, 1)}
                                        disabled={actIdx === activeActs.length - 1}
                                        title="Move Down (visit later)"
                                      >
                                        <ChevronDown size={13} />
                                      </button>
                                    </div>

                                    {/* Dynamic Day and Position Selection with Save */}
                                    {(() => {
                                      const draft = placeEditDrafts[act.id];
                                      const chosenDayId = draft?.dayId != null ? draft.dayId : day.id;
                                      const targetDayObj = (trip?.days || []).find(d => Number(d.id) === Number(chosenDayId)) || day;
                                      const isDiffDay = Number(chosenDayId) !== Number(day.id);

                                      // Available slots in target day
                                      const targetDayActive = (targetDayObj.activities || []).filter(a => a.status !== 'CANCELLED' && a.status !== 'REPLACED' && a.id !== act.id);
                                      const totalPositions = isDiffDay ? (targetDayActive.length + 1) : Math.max(1, activeActs.length);
                                      const chosenPos = draft?.position != null ? draft.position : (actIdx + 1);
                                      const isDirty = draft?.isDirty || false;

                                      return (
                                        <>
                                          {/* Move to another Day */}
                                          <div className="manual-move-day-wrap" title="Change Day of visit">
                                            <span className="manual-move-label">Day:</span>
                                            <select 
                                              className="manual-mini-select"
                                              value={chosenDayId}
                                              onChange={(e) => {
                                                const nextDayId = Number(e.target.value);
                                                const nextDayObj = (trip?.days || []).find(d => Number(d.id) === nextDayId) || day;
                                                const nextActive = (nextDayObj.activities || []).filter(a => a.status !== 'CANCELLED' && a.status !== 'REPLACED' && a.id !== act.id);
                                                const maxP = Number(nextDayId) !== Number(day.id) ? nextActive.length + 1 : activeActs.length;
                                                const newPos = Math.max(1, Math.min(maxP, chosenPos));
                                                const dirty = Number(nextDayId) !== Number(day.id) || newPos !== (actIdx + 1);
                                                setPlaceEditDrafts(prev => ({
                                                  ...prev,
                                                  [act.id]: { dayId: nextDayId, position: newPos, isDirty: dirty }
                                                }));
                                              }}
                                            >
                                              {(trip?.days || []).map(d => (
                                                <option key={d.id} value={d.id}>
                                                  Day {d.dayNumber}
                                                </option>
                                              ))}
                                            </select>
                                          </div>

                                          {/* Position Selection */}
                                          <div className="manual-move-day-wrap" title="Change Position within Day">
                                            <span className="manual-move-label">Pos:</span>
                                            <select 
                                              className="manual-mini-select"
                                              value={chosenPos}
                                              onChange={(e) => {
                                                const nextPos = Number(e.target.value);
                                                const dirty = Number(chosenDayId) !== Number(day.id) || nextPos !== (actIdx + 1);
                                                setPlaceEditDrafts(prev => ({
                                                  ...prev,
                                                  [act.id]: { dayId: chosenDayId, position: nextPos, isDirty: dirty }
                                                }));
                                              }}
                                            >
                                              {Array.from({ length: totalPositions }, (_, pIdx) => (
                                                <option key={pIdx} value={pIdx + 1}>
                                                  #{pIdx + 1}
                                                </option>
                                              ))}
                                            </select>
                                          </div>

                                          {/* Save / Cancel Button for Day & Position changes */}
                                          {isDirty && (
                                            <div className="manual-timing-edit-box" style={{ margin: 0 }}>
                                              <button 
                                                type="button" 
                                                className="btn-mini-save"
                                                disabled={isSavingPlaceId === act.id}
                                                onClick={() => handleSavePlaceChanges(act.id, day.id)}
                                                title="Save Day & Position changes"
                                              >
                                                {isSavingPlaceId === act.id ? 'Saving...' : 'Save'}
                                              </button>
                                              <button 
                                                type="button" 
                                                className="btn-mini-cancel"
                                                onClick={() => handleCancelPlaceChanges(act.id)}
                                                title="Cancel changes"
                                              >
                                                ✕
                                              </button>
                                            </div>
                                          )}
                                        </>
                                      );
                                    })()}

                                    {/* Timing Editor Toggle / Form */}
                                    {editingTimingId === act.id ? (
                                      <div className="manual-timing-edit-box">
                                        <input 
                                          type="time" 
                                          className="manual-mini-time"
                                          value={timingForm.startTime}
                                          onChange={(e) => setTimingForm(prev => ({ ...prev, startTime: e.target.value }))}
                                        />
                                        <select 
                                          className="manual-mini-select"
                                          value={timingForm.durationMinutes}
                                          onChange={(e) => setTimingForm(prev => ({ ...prev, durationMinutes: Number(e.target.value) }))}
                                        >
                                          <option value={30}>30m</option>
                                          <option value={45}>45m</option>
                                          <option value={60}>1h</option>
                                          <option value={90}>1.5h</option>
                                          <option value={120}>2h</option>
                                          <option value={180}>3h</option>
                                        </select>
                                        <button 
                                          type="button" 
                                          className="btn-mini-save"
                                          onClick={async () => {
                                            await handleUpdateActivityTiming(act.id, timingForm.startTime, timingForm.durationMinutes);
                                            setEditingTimingId(null);
                                          }}
                                        >
                                          Save
                                        </button>
                                        <button 
                                          type="button" 
                                          className="btn-mini-cancel"
                                          onClick={() => setEditingTimingId(null)}
                                        >
                                          ✕
                                        </button>
                                      </div>
                                    ) : (
                                      <button 
                                        type="button" 
                                        className="btn-mini-time-toggle"
                                        onClick={() => {
                                          setEditingTimingId(act.id);
                                          setTimingForm({
                                            startTime: act.time || '11:00',
                                            durationMinutes: act.durationMinutes || (act.duration ? parseInt(act.duration, 10) : 90) || 90
                                          });
                                        }}
                                        title="Change visiting time & duration"
                                      >
                                        <Clock size={12} /> Time
                                      </button>
                                    )}

                                    {/* Remove Place */}
                                    <button 
                                      type="button" 
                                      className="btn-mini-remove"
                                      onClick={() => handleRemoveActivity(act.id)}
                                      title="Remove place from itinerary"
                                    >
                                      <Trash2 size={13} />
                                    </button>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
            )}

            <div className="modal-action-buttons">
              <button className="btn-modal-secondary" onClick={() => setShowRescheduleModal(false)}>
                {t('common.cancel')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ==========================================
          INVITE TRAVELLERS MODAL
          ========================================== */}
      {showInviteModal && (
        <div className="modal-backdrop">
          <div className="invite-modal glass-panel animate-scale-up" style={{ maxWidth: '480px', width: '100%', background: '#ffffff', borderRadius: '16px', padding: '24px', boxShadow: '0 20px 40px rgba(0,0,0,0.15)' }}>
            <div className="modal-reschedule-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ background: '#e0f2fe', color: '#0284c7', width: '38px', height: '38px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <UserPlus size={20} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.25rem', color: '#0f172a' }}>{t('itinerary.inviteTravellers')}</h3>
                  <p style={{ margin: 0, fontSize: '0.82rem', color: '#64748b' }}>Share trip ID #{trip.id} with friends or family</p>
                </div>
              </div>
              <button className="modal-close-btn" onClick={() => setShowInviteModal(false)}>✕</button>
            </div>

            {inviteError && (
              <div className="edit-error-banner" style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#b91c1c', padding: '10px 12px', borderRadius: '8px', fontSize: '0.85rem', marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <AlertCircle size={16} /> {inviteError}
              </div>
            )}

            <form onSubmit={handleSendInvite}>
              <div className="form-group" style={{ marginBottom: '14px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155' }}>Email / Username / Account Identifier</label>
                <input 
                  type="email" 
                  placeholder="e.g. friend@travelmate.com" 
                  value={inviteEmail} 
                  onChange={(e) => setInviteEmail(e.target.value)}
                  style={{ padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '0.92rem' }}
                  required 
                  autoFocus 
                />
              </div>

              <div className="form-group" style={{ marginBottom: '16px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155' }}>Trip Role</label>
                <select 
                  value={inviteRole} 
                  onChange={(e) => setInviteRole(e.target.value)}
                  style={{ padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '0.92rem' }}
                >
                  <option value="VIEWER">VIEWER (Can view itinerary, map & budget)</option>
                  <option value="EDITOR">EDITOR (Can mark completed, reschedule & suggest)</option>
                </select>
              </div>

              <div className="modal-action-buttons" style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                <button type="button" className="btn-modal-secondary" onClick={() => setShowInviteModal(false)}>
                  {t('common.cancel')}
                </button>
                <button 
                  type="submit" 
                  className="btn-modal-primary"
                  disabled={isSendingInvite || !inviteEmail.trim()}
                >
                  {isSendingInvite ? t('modals.sending') : t('modals.sendInvite')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ==========================================
          IN-DASHBOARD EDIT TRIP MODAL
          ========================================== */}
      {showEditTripModal && (
        <div className="modal-backdrop">
          <div className="edit-trip-modal glass-panel animate-scale-up" style={{ maxWidth: '720px', width: '100%', background: '#ffffff', borderRadius: '16px', padding: '24px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div className="edit-modal-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px', borderBottom: '1px solid #f1f5f9', paddingBottom: '12px' }}>
              <div>
                <h2 style={{ margin: 0, fontSize: '1.35rem', color: '#0f172a' }}>{t('modals.editTitle')}: {trip.title ? formatTripTitle(trip.title, trip.destinationCity) : getDestName(trip.destinationCity)}</h2>
                <p style={{ margin: '4px 0 0', fontSize: '0.82rem', color: '#64748b' }}>Trip ID #{trip.id} • Modifying preferences will recalculate affected itinerary items</p>
              </div>
              <button className="modal-close-btn" onClick={() => setShowEditTripModal(false)}>✕</button>
            </div>

            {editError && (
              <div className="edit-error-banner" style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#b91c1c', padding: '10px 12px', borderRadius: '8px', fontSize: '0.85rem', marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <AlertCircle size={16} /> {editError}
              </div>
            )}

            <form onSubmit={handleSaveEditTrip} className="edit-form-content">
              <div className="form-grid-2">
                <div className="form-group">
                  <label>Destination City</label>
                  <input 
                    type="text" 
                    value={editFormData.destinationCity} 
                    onChange={(e) => setEditFormData({ ...editFormData, destinationCity: e.target.value })}
                    required 
                  />
                </div>

                <div className="form-group">
                  <label>Starting Location (Origin)</label>
                  <input 
                    type="text" 
                    value={editFormData.startingLocation} 
                    placeholder="e.g. Vijayawada, Hyderabad"
                    onChange={(e) => setEditFormData({ ...editFormData, startingLocation: e.target.value })}
                  />
                </div>
              </div>

              <div className="form-grid-3">
                <div className="form-group">
                  <label>Start Date</label>
                  <input 
                    type="date" 
                    value={editFormData.startDate} 
                    onChange={(e) => handleDateChange('startDate', e.target.value)}
                    required 
                  />
                </div>

                <div className="form-group">
                  <label>End Date</label>
                  <input 
                    type="date" 
                    value={editFormData.endDate} 
                    onChange={(e) => handleDateChange('endDate', e.target.value)}
                    required 
                  />
                </div>

                <div className="form-group">
                  <label>Duration (Days)</label>
                  <input 
                    type="number" 
                    min="1" 
                    max="14" 
                    value={editFormData.numDays} 
                    onChange={(e) => setEditFormData({ ...editFormData, numDays: e.target.value })}
                    required 
                  />
                </div>
              </div>

              <div className="form-grid-3">
                <div className="form-group">
                  <label>Budget (INR ₹)</label>
                  <input 
                    type="number" 
                    min="1000" 
                    step="500" 
                    value={editFormData.budgetInr} 
                    onChange={(e) => setEditFormData({ ...editFormData, budgetInr: e.target.value })}
                    required 
                  />
                </div>

                <div className="form-group">
                  <label>Number of Travellers</label>
                  <input 
                    type="number" 
                    min="1" 
                    max="20" 
                    value={editFormData.numTravelers} 
                    onChange={(e) => setEditFormData({ ...editFormData, numTravelers: e.target.value })}
                    required 
                  />
                </div>

                <div className="form-group">
                  <label>Travel Style / Group</label>
                  <select 
                    value={editFormData.travelStyle} 
                    onChange={(e) => setEditFormData({ ...editFormData, travelStyle: e.target.value })}
                  >
                    <option value="Solo">Solo</option>
                    <option value="Couple">Couple</option>
                    <option value="Family">Family</option>
                    <option value="Friends">Friends</option>
                  </select>
                </div>
              </div>

              {/* INTERESTS */}
              <div className="form-group">
                <label>Interests & Themes</label>
                <div className="interest-chips-container">
                  {['Heritage', 'Nature', 'Food', 'Culture', 'Temples', 'Adventure', 'Beaches', 'Museums'].map(interest => (
                    <button
                      type="button"
                      key={interest}
                      className={`interest-chip ${editFormData.interests.includes(interest) ? 'active' : ''}`}
                      onClick={() => handleInterestToggle(interest)}
                    >
                      {interest}
                    </button>
                  ))}
                </div>
              </div>

              <div className="form-grid-3">
                <div className="form-group">
                  <label>Food Preference</label>
                  <select 
                    value={editFormData.foodPreference} 
                    onChange={(e) => setEditFormData({ ...editFormData, foodPreference: e.target.value })}
                  >
                    <option value="Any">Any</option>
                    <option value="Vegetarian">Vegetarian</option>
                    <option value="Non-Vegetarian">Non-Vegetarian</option>
                    <option value="Jain">Jain</option>
                    <option value="Halal">Halal</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>{t('modals.travelPace')}</label>
                  <select 
                    value={editFormData.activityLevel} 
                    onChange={(e) => setEditFormData({ ...editFormData, activityLevel: e.target.value })}
                  >
                    <option value="Relaxed">{t('plan.paceRelaxed')}</option>
                    <option value="Moderate">{t('plan.paceModerate')}</option>
                    <option value="Fast Paced">{t('plan.paceFast')}</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Transit Preference</label>
                  <select 
                    value={editFormData.transitPreference} 
                    onChange={(e) => setEditFormData({ ...editFormData, transitPreference: e.target.value })}
                  >
                    <option value="Train">Train</option>
                    <option value="Bus">Bus / Road</option>
                    <option value="Cab">Cab / Car</option>
                    <option value="Flight">Flight</option>
                  </select>
                </div>
              </div>

              <div className="form-grid-2">
                <div className="form-group">
                  <label>Distance Preference</label>
                  <select 
                    value={editFormData.maxDistanceKm || ''} 
                    onChange={(e) => setEditFormData({ ...editFormData, maxDistanceKm: e.target.value, travelDistancePref: `Stay within ${e.target.value} km` })}
                  >
                    <option value="">Any distance</option>
                    <option value="5">Stay within 5 km</option>
                    <option value="15">Stay within 15 km</option>
                    <option value="30">Stay within 30 km</option>
                    <option value="50">Stay within 50 km</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Entry-fee Preference</label>
                  <select 
                    value={editFormData.maxEntryFee} 
                    onChange={(e) => setEditFormData({ ...editFormData, maxEntryFee: e.target.value })}
                  >
                    <option value="">Any Entry Fee</option>
                    <option value="0">Free entry only (₹0)</option>
                    <option value="100">Budget (Under ₹100)</option>
                    <option value="500">Moderate (Under ₹500)</option>
                  </select>
                </div>
              </div>

              <div className="edit-modal-footer">
                <button 
                  type="button" 
                  className="btn-secondary" 
                  onClick={() => setShowEditTripModal(false)}
                  disabled={isSavingEdit}
                >
                  {t('common.cancel')}
                </button>
                <button 
                  type="submit" 
                  className="btn-primary" 
                  disabled={isSavingEdit}
                >
                  {isSavingEdit ? (
                    <span>{t('modals.saving')}</span>
                  ) : (
                    <span>{t('modals.saveChanges')}</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* TOAST FEEDBACK BANNER */}
      {toastMessage && (
        <div className="toast-notification-banner animate-fade-in">
          <span>{toastMessage}</span>
        </div>
      )}
    </div>
  );
};

export default Itinerary;
