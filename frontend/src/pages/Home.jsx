import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  Search, MapPin, Compass, Sun, Map, ArrowRight, ShieldCheck, 
  Camera, History, Mountain, Tent, Trees, Calendar, Users, 
  DollarSign, Sparkles, Navigation, CloudRain, Clock, CheckCircle2, ChevronRight,
  AlertCircle, Loader2
} from 'lucide-react';
import { destinations } from '../data/destinations';
import { tripService, placesService } from '../services/api';
import LocationAutocomplete from '../components/LocationAutocomplete';
import PlaceDiscoveryCard from '../components/PlaceDiscoveryCard';
import PlaceMapModal from '../components/PlaceMapModal';
import SafeImage from '../components/common/SafeImage';
import './Home.css';

const Home = () => {
  const navigate = useNavigate();
  const [selectedRegion, setSelectedRegion] = useState('All');
  const [userTrips, setUserTrips] = useState([]);
  const [searchForm, setSearchForm] = useState({
    origin: '',
    destination: '',
    startDate: '',
    duration: '3',
    travelStyle: 'Solo',
    lat: null,
    lon: null,
    address: ''
  });

  // Real Place Discovery states
  const [discoveredPlace, setDiscoveredPlace] = useState(null);
  const [isSearchingPlace, setIsSearchingPlace] = useState(false);
  const [indiaScopeNotice, setIndiaScopeNotice] = useState('');
  const [mapModalPlace, setMapModalPlace] = useState(null);

  useEffect(() => {
    const fetchRecentTrips = async () => {
      const token = localStorage.getItem('token');
      if (token) {
        try {
          const trips = await tripService.getUserTrips();
          setUserTrips(trips ? trips.slice(0, 3) : []);
        } catch (e) {
          console.error("Could not fetch user trips", e);
        }
      }
    };
    fetchRecentTrips();
  }, []);

  // Handle Real Place Discovery
  const handlePerformPlaceSearch = async (destinationQuery) => {
    const q = (destinationQuery || searchForm.destination || '').trim();
    if (!q) return;

    setIndiaScopeNotice('');
    setIsSearchingPlace(true);
    setDiscoveredPlace(null);

    try {
      const placeData = await placesService.search(q);
      if (placeData) {
        if (placeData.isIndia === false) {
          setIndiaScopeNotice(placeData.message || "TravelMate AI currently supports travel within India.");
          setDiscoveredPlace(null);
        } else {
          setDiscoveredPlace(placeData);
          setSearchForm(prev => ({
            ...prev,
            destination: placeData.name || q,
            lat: placeData.lat,
            lon: placeData.lon,
            address: placeData.address || ''
          }));
        }
      }
    } catch (err) {
      console.error("Search place error:", err);
      // Fallback: check if foreign
      const msg = err.response?.data?.message || err.message || '';
      if (msg.includes("supports travel within India")) {
        setIndiaScopeNotice("TravelMate AI currently supports travel within India.");
      }
    } finally {
      setIsSearchingPlace(false);
    }
  };

  const handleAutocompleteSelect = (item) => {
    setSearchForm(prev => ({
      ...prev,
      destination: item.name,
      lat: item.lat || null,
      lon: item.lon || null
    }));
    handlePerformPlaceSearch(item.name);
  };

  const handleQuickSearchSubmit = (e) => {
    e.preventDefault();
    if (!searchForm.destination.trim()) return;

    const params = new URLSearchParams();
    params.set('dest', searchForm.destination.trim());
    if (searchForm.origin) params.set('origin', searchForm.origin);
    if (searchForm.startDate) params.set('start', searchForm.startDate);
    if (searchForm.travelStyle) params.set('style', searchForm.travelStyle);
    if (searchForm.lat) params.set('lat', searchForm.lat);
    if (searchForm.lon) params.set('lon', searchForm.lon);
    if (searchForm.address) params.set('address', searchForm.address);
    navigate(`/plan?${params.toString()}`);
  };

  const handlePlanTripToPlace = (place) => {
    const params = new URLSearchParams();
    params.set('dest', place.name || place.title);
    if (place.lat) params.set('lat', place.lat);
    if (place.lon) params.set('lon', place.lon);
    if (place.address) params.set('address', place.address);
    if (searchForm.origin) params.set('origin', searchForm.origin);
    if (searchForm.startDate) params.set('start', searchForm.startDate);
    if (searchForm.travelStyle) params.set('style', searchForm.travelStyle);
    navigate(`/plan?${params.toString()}`);
  };

  const filteredDestinations = selectedRegion === 'All' 
    ? destinations 
    : destinations.filter(d => d.region === selectedRegion);

  const interestCategories = [
    { name: "Spiritual & Temples", icon: <ShieldCheck size={24} />, count: "120+ Verified", cat: "Spiritual" },
    { name: "Royal Forts & Palaces", icon: <History size={24} />, count: "95+ Verified", cat: "Heritage" },
    { name: "Scenic Mountains", icon: <Mountain size={24} />, count: "80+ Verified", cat: "Mountains" },
    { name: "Beaches & Coastline", icon: <Sun size={24} />, count: "65+ Verified", cat: "Beaches" },
    { name: "Wildlife & Jungles", icon: <Camera size={24} />, count: "45+ Verified", cat: "Wildlife" },
    { name: "Authentic Food Trails", icon: <Compass size={24} />, count: "110+ Verified", cat: "Food" },
    { name: "Adventure & Rapids", icon: <Tent size={24} />, count: "50+ Verified", cat: "Adventure" },
    { name: "Flora & Nature Parks", icon: <Trees size={24} />, count: "70+ Verified", cat: "Nature" }
  ];

  return (
    <div className="home-container animate-fade-in">
      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-content">
          <div className="hero-badge">
            <Sparkles size={16} /> Real India-Wide Place Discovery & Smart Itineraries
          </div>
          <h1>Design Your Perfect <span className="text-gradient">India Journey</span></h1>
          <p>
            Explore ANY Indian city, temple, beach, fort, waterfall, or town. 
            Powered by real location data, verified GPS coordinates, and weather-adaptive scheduling.
          </p>
          
          {/* Quick Smart Planner Card with Autocomplete */}
          <div className="hero-planner-card">
            <form onSubmit={handleQuickSearchSubmit} className="hero-planner-form">
              <div className="planner-field">
                <label><Navigation size={14} /> Origin City</label>
                <input 
                  type="text" 
                  placeholder="e.g., Delhi, Mumbai, Hyderabad" 
                  value={searchForm.origin}
                  onChange={(e) => setSearchForm({...searchForm, origin: e.target.value})}
                />
              </div>

              <div className="planner-field">
                <label><MapPin size={14} /> Destination (Any Place in India)</label>
                <LocationAutocomplete 
                  value={searchForm.destination}
                  onChange={(val) => setSearchForm({...searchForm, destination: val, lat: null, lon: null})}
                  onSelect={handleAutocompleteSelect}
                  placeholder="e.g. Kanaka Durga Temple, Araku, Gudivada"
                  required
                />
              </div>

              <div className="planner-field">
                <label><Calendar size={14} /> Start Date</label>
                <input 
                  type="date" 
                  value={searchForm.startDate}
                  onChange={(e) => setSearchForm({...searchForm, startDate: e.target.value})}
                />
              </div>

              <div className="planner-field">
                <label><Users size={14} /> Style</label>
                <select 
                  value={searchForm.travelStyle}
                  onChange={(e) => setSearchForm({...searchForm, travelStyle: e.target.value})}
                >
                  <option value="Solo">Solo Traveler</option>
                  <option value="Couple">Couple</option>
                  <option value="Family">Family</option>
                  <option value="Friends">Friends Group</option>
                </select>
              </div>

              <div className="planner-button-group">
                <button type="submit" className="primary-btn planner-btn">
                  <span>Start Planning</span>
                  <ArrowRight size={18} />
                </button>
              </div>
            </form>
            
            {/* Quick destination chips */}
            <div className="hero-quick-chips">
              <span className="chips-label">Popular Discovery Searches:</span>
              {[
                'Kanaka Durga Temple', 'Vijayawada', 'Gudivada', 'Undavalli Caves', 
                'Araku Valley', 'Tirupati', 'Srikalahasti', 'Charminar', 'Hampi', 'Ooty', 'Munnar', 'RK Beach'
              ].map((placeName) => (
                <button 
                  key={placeName} 
                  type="button" 
                  className="quick-chip"
                  onClick={() => {
                    setSearchForm(prev => ({ ...prev, destination: placeName }));
                    handlePerformPlaceSearch(placeName);
                  }}
                >
                  {placeName}
                </button>
              ))}
            </div>

            {/* Quick action: Inspect / Discover Details Button */}
            <div className="hero-instant-discover-row">
              <button 
                type="button" 
                className="instant-discover-btn"
                disabled={!searchForm.destination.trim() || isSearchingPlace}
                onClick={() => handlePerformPlaceSearch(searchForm.destination)}
              >
                {isSearchingPlace ? (
                  <>
                    <Loader2 size={16} className="spin-icon" />
                    <span>Verifying Real Location...</span>
                  </>
                ) : (
                  <>
                    <Search size={16} />
                    <span>Discover Real Place Details & Nearby Attractions</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* India Scope Alert (if user entered Paris, London, Dubai, etc.) */}
      {indiaScopeNotice && (
        <section className="india-notice-section animate-fade-in">
          <div className="india-notice-banner">
            <AlertCircle size={24} className="notice-icon" />
            <div className="notice-text">
              <h3>{indiaScopeNotice}</h3>
              <p>Explore real destinations across all Indian states and union territories — from hill stations and beaches to sacred temples and historic forts.</p>
            </div>
          </div>
        </section>
      )}

      {/* Real Place Discovery Result Card */}
      {discoveredPlace && (
        <section className="place-discovery-section padding-section">
          <div className="section-header">
            <div className="header-tag">Live Location Discovery</div>
            <h2>Real Place Found & Verified</h2>
            <p>100% verified location, GPS coordinates, real photographs, and nearby attractions.</p>
          </div>

          <PlaceDiscoveryCard 
            place={discoveredPlace}
            onPlanTrip={handlePlanTripToPlace}
            onLocateOnMap={(place) => setMapModalPlace(place)}
          />
        </section>
      )}

      {/* Live Recent Trips (if logged in and has trips) */}
      {userTrips.length > 0 && (
        <section className="recent-trips-section padding-section">
          <div className="section-header">
            <div className="header-tag">Resume Your Adventure</div>
            <h2>Your Recent Planned Trips</h2>
          </div>
          <div className="recent-trips-grid">
            {userTrips.map(trip => (
              <div key={trip.id} className="recent-trip-card">
                <div className="trip-card-header">
                  <h3>{trip.title || `Trip to ${trip.destinationCity}`}</h3>
                  <span className="trip-status-pill">{trip.status || 'PLANNED'}</span>
                </div>
                <div className="trip-meta-row">
                  <span><MapPin size={14} /> {trip.destinationCity}</span>
                  <span><Calendar size={14} /> {trip.startDate} ({trip.numDays || 1} Days)</span>
                  <span><DollarSign size={14} /> Est. ₹{trip.totalEstimatedCost ? Math.round(trip.totalEstimatedCost) : '0'}</span>
                </div>
                <Link to={`/trip/${trip.id}`} className="view-trip-link">
                  Open & Adapt Itinerary <ArrowRight size={16} />
                </Link>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Popular Indian Destinations (Curated Showcase) */}
      <section className="featured-section padding-section">
        <div className="section-header-flex">
          <div>
            <div className="header-tag">Curated India Showcase</div>
            <h2>Popular Indian Destinations</h2>
            <p>100% verified real landmarks, coordinates, and cultural attractions across India.</p>
          </div>
          <div className="region-filter-tabs">
            {['All', 'South', 'North', 'West'].map(region => (
              <button 
                key={region} 
                className={`region-tab ${selectedRegion === region ? 'active' : ''}`}
                onClick={() => setSelectedRegion(region)}
              >
                {region === 'All' ? 'All India' : region}
              </button>
            ))}
          </div>
        </div>

        <div className="destinations-grid">
          {filteredDestinations.slice(0, 8).map(dest => (
            <div key={dest.id} className="dest-card-modern hover-lift">
              <div className="dest-image-wrapper">
                <SafeImage 
                  src={dest.image || dest.imageUrl || dest.photoUrl || dest.heroImage} 
                  alt={dest.name} 
                  category={dest.categories?.[0] || 'Heritage'}
                  destinationId={dest.id}
                  destinationName={dest.name}
                  loading="lazy" 
                />
                <span className="dest-rating-badge">★ {dest.rating}</span>
                <span className="dest-verified-badge"><CheckCircle2 size={12} /> {dest.verifiedPlacesCount} Spots</span>
              </div>
              <div className="dest-card-body">
                <div className="dest-title-row">
                  <h3>{dest.name}</h3>
                  <span className="dest-state">{dest.state}</span>
                </div>
                <p>{dest.description}</p>
                <div className="dest-category-tags">
                  {dest.categories.slice(0, 3).map((cat, i) => (
                    <span key={i} className="mini-tag">{cat}</span>
                  ))}
                </div>
                <div className="dest-card-footer">
                  <span className="best-time">Best: {dest.bestTimeToVisit.split(',')[0]}</span>
                  <Link to={`/plan?dest=${encodeURIComponent(dest.name)}`} className="plan-dest-btn">
                    Plan Trip <ChevronRight size={16} />
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Explore by Interests */}
      <section className="interests-section padding-section bg-light">
        <div className="section-header">
          <div className="header-tag">Tailored To Your Passions</div>
          <h2>Explore India by Interests</h2>
          <p>Whether you seek high-altitude treks, sacred temples, or royal palaces.</p>
        </div>
        <div className="interests-grid">
          {interestCategories.map((item, idx) => (
            <Link 
              key={idx} 
              to={`/explore?category=${item.cat}`} 
              className="interest-card-elevated hover-lift"
            >
              <div className="interest-icon-circle">{item.icon}</div>
              <h4>{item.name}</h4>
              <span className="interest-count">{item.count}</span>
            </Link>
          ))}
        </div>
      </section>

      {/* How TravelMate AI Works */}
      <section className="how-it-works padding-section">
        <div className="section-header">
          <div className="header-tag">Intelligent Architecture</div>
          <h2>How TravelMate AI Plans Your Journey</h2>
          <p>A true context-aware system powered by machine learning and real-world Indian data.</p>
        </div>
        <div className="workflow-steps-grid">
          <div className="step-card">
            <div className="step-number">01</div>
            <h3>Traveller Profiling</h3>
            <p>Collects party size, preferred pace, age groups, culinary tastes, and budget constraints.</p>
          </div>
          <div className="step-card">
            <div className="step-number">02</div>
            <h3>Hybrid ML Ranking</h3>
            <p>Cosine Similarity vectorization and SVD Collaborative Filtering match verified Indian landmarks.</p>
          </div>
          <div className="step-card">
            <div className="step-number">03</div>
            <h3>Dynamic Verification</h3>
            <p>Every stop is verified using OpenStreetMap & Wikipedia Geocodes. No imaginary locations.</p>
          </div>
          <div className="step-card">
            <div className="step-number">04</div>
            <h3>Adaptive Re-Routing</h3>
            <p>Monitors live weather forecasts. If heavy rainfall is predicted, automatically swaps to indoor cultural alternatives.</p>
          </div>
        </div>
      </section>

      {/* Live India Travel Insight Widget */}
      <section className="weather-insight-section padding-section bg-gradient-light">
        <div className="insight-card-inner">
          <div className="insight-icon"><Sun size={36} className="text-warning" /></div>
          <div className="insight-text">
            <h3>Live Weather-Aware Travel Intelligence</h3>
            <p>
              TravelMate AI continuously connects with real-time meteorological models across India. 
              Visiting coastal Andhra Pradesh, Kerala backwaters, or the Himalayas? 
              Our engine detects rainfall risks beforehand and adjusts your schedule with 1-click alternatives.
            </p>
          </div>
          <Link to="/plan" className="primary-btn start-cta-btn">
            Create AI Itinerary <ArrowRight size={18} />
          </Link>
        </div>
      </section>

      {/* Interactive Map Modal for "Locate on Map" */}
      {mapModalPlace && (
        <PlaceMapModal 
          place={mapModalPlace}
          onClose={() => setMapModalPlace(null)}
          onPlanTrip={() => {
            const p = mapModalPlace;
            setMapModalPlace(null);
            handlePlanTripToPlace(p);
          }}
        />
      )}
    </div>
  );
};

export default Home;
