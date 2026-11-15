import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useDestinationTranslation } from '../utils/destinationTranslations';
import { 
  Search, MapPin, Compass, Sun, ArrowRight, ShieldCheck, 
  Camera, History, Mountain, Tent, Trees, Calendar, Users, 
  DollarSign, Sparkles, Navigation, CheckCircle2, ChevronRight,
  AlertCircle, Loader2
} from 'lucide-react';
import { destinations } from '../data/destinations';
import { tripService, placesService } from '../services/api';
import LocationAutocomplete from '../components/LocationAutocomplete';
import PlaceDiscoveryCard from '../components/PlaceDiscoveryCard';
import PlaceMapModal from '../components/PlaceMapModal';
import SafeImage from '../components/common/SafeImage';
import HowToUseVideo from '../components/HowToUseVideo';
import './Home.css';

const Home = () => {
  const { t } = useTranslation();
  const { getDestName, getDestState, formatTripTitle } = useDestinationTranslation();
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
    { name: t('home.catSpiritual'), icon: <ShieldCheck size={24} />, count: t('home.verifiedCount', { count: "120+" }), cat: "Spiritual" },
    { name: t('home.catHeritage'), icon: <History size={24} />, count: t('home.verifiedCount', { count: "95+" }), cat: "Heritage" },
    { name: t('home.catMountains'), icon: <Mountain size={24} />, count: t('home.verifiedCount', { count: "80+" }), cat: "Mountains" },
    { name: t('home.catBeaches'), icon: <Sun size={24} />, count: t('home.verifiedCount', { count: "65+" }), cat: "Beaches" },
    { name: t('home.catWildlife'), icon: <Camera size={24} />, count: t('home.verifiedCount', { count: "45+" }), cat: "Wildlife" },
    { name: t('home.catFood'), icon: <Compass size={24} />, count: t('home.verifiedCount', { count: "110+" }), cat: "Food" },
    { name: t('home.catAdventure'), icon: <Tent size={24} />, count: t('home.verifiedCount', { count: "50+" }), cat: "Adventure" },
    { name: t('home.catNature'), icon: <Trees size={24} />, count: t('home.verifiedCount', { count: "70+" }), cat: "Nature" }
  ];

  return (
    <div className="home-container animate-fade-in">
      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-content">
          <div className="hero-badge">
            <Sparkles size={16} /> {t('home.heroBadge')}
          </div>
          <h1>{t('home.heroTitle')} <span className="text-gradient">{t('home.heroTitleHighlight')}</span></h1>
          <p>
            {t('home.heroSubtitle')}
          </p>
          
          {/* Quick Smart Planner Card with Autocomplete */}
          <div className="hero-planner-card">
            <form onSubmit={handleQuickSearchSubmit} className="hero-planner-form">
              <div className="planner-field">
                <label><Navigation size={14} /> {t('home.originCity')}</label>
                <input 
                  type="text" 
                  placeholder={t('home.originPlaceholder')} 
                  value={searchForm.origin}
                  onChange={(e) => setSearchForm({...searchForm, origin: e.target.value})}
                />
              </div>

              <div className="planner-field">
                <label><MapPin size={14} /> {t('home.destination')}</label>
                <LocationAutocomplete 
                  value={searchForm.destination}
                  onChange={(val) => setSearchForm({...searchForm, destination: val, lat: null, lon: null})}
                  onSelect={handleAutocompleteSelect}
                  placeholder={t('home.destinationPlaceholder')}
                  required
                />
              </div>

              <div className="planner-field">
                <label><Calendar size={14} /> {t('home.startDate')}</label>
                <input 
                  type="date" 
                  value={searchForm.startDate}
                  onChange={(e) => setSearchForm({...searchForm, startDate: e.target.value})}
                />
              </div>

              <div className="planner-field">
                <label><Users size={14} /> {t('home.style')}</label>
                <select 
                  value={searchForm.travelStyle}
                  onChange={(e) => setSearchForm({...searchForm, travelStyle: e.target.value})}
                >
                  <option value="Solo">{t('home.styleSolo')}</option>
                  <option value="Couple">{t('home.styleCouple')}</option>
                  <option value="Family">{t('home.styleFamily')}</option>
                  <option value="Friends">{t('home.styleFriends')}</option>
                </select>
              </div>

              <div className="planner-button-group">
                <button type="submit" className="primary-btn planner-btn">
                  <span>{t('home.startPlanning')}</span>
                  <ArrowRight size={18} />
                </button>
              </div>
            </form>
            
            {/* Quick destination chips */}
            <div className="hero-quick-chips">
              <span className="chips-label">{t('home.popularSearches')}</span>
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
                  {getDestName(placeName)}
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
                    <span>{t('home.verifyingLocation')}</span>
                  </>
                ) : (
                  <>
                    <Search size={16} />
                    <span>{t('home.discoverDetails')}</span>
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
              <h3>{indiaScopeNotice || t('home.indiaScopeTitle')}</h3>
              <p>{t('home.indiaScopeDesc')}</p>
            </div>
          </div>
        </section>
      )}

      {/* Real Place Discovery Result Card */}
      {discoveredPlace && (
        <section className="place-discovery-section padding-section">
          <div className="section-header">
            <div className="header-tag">{t('home.liveDiscoveryTag')}</div>
            <h2>{t('home.liveDiscoveryTitle')}</h2>
            <p>{t('home.liveDiscoverySub')}</p>
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
            <div className="header-tag">{t('home.recentTripsTag')}</div>
            <h2>{t('home.recentTripsTitle')}</h2>
          </div>
          <div className="recent-trips-grid">
            {userTrips.map(trip => (
              <div key={trip.id} className="recent-trip-card">
                <div className="trip-card-header">
                  <h3>{trip.title ? formatTripTitle(trip.title, trip.destinationCity) : formatTripTitle(`Trip to ${trip.destinationCity}`, trip.destinationCity)}</h3>
                  <span className="trip-status-pill">{trip.status || 'PLANNED'}</span>
                </div>
                <div className="trip-meta-row">
                  <span><MapPin size={14} /> {getDestName(trip.destinationCity)}</span>
                  <span><Calendar size={14} /> {trip.startDate} ({trip.numDays || 1} Days)</span>
                  <span><DollarSign size={14} /> Est. ₹{trip.totalEstimatedCost ? Math.round(trip.totalEstimatedCost) : '0'}</span>
                </div>
                <Link to={`/trip/${trip.id}`} className="view-trip-link">
                  {t('home.openItinerary')} <ArrowRight size={16} />
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
            <div className="header-tag">{t('home.curatedTag')}</div>
            <h2>{t('home.curatedTitle')}</h2>
            <p>{t('home.curatedSub')}</p>
          </div>
          <div className="region-filter-tabs">
            {[
              { id: 'All', label: t('home.allIndia') },
              { id: 'South', label: t('home.south') },
              { id: 'North', label: t('home.north') },
              { id: 'West', label: t('home.west') }
            ].map(tab => (
              <button 
                key={tab.id} 
                className={`region-tab ${selectedRegion === tab.id ? 'active' : ''}`}
                onClick={() => setSelectedRegion(tab.id)}
              >
                {tab.label}
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
                <span className="dest-verified-badge"><CheckCircle2 size={12} /> {dest.verifiedPlacesCount} {t('home.spots')}</span>
              </div>
              <div className="dest-card-body">
                <div className="dest-title-row">
                  <h3>{getDestName(dest.name)}</h3>
                  <span className="dest-state">{getDestState(dest.state)}</span>
                </div>
                <p>{dest.description}</p>
                <div className="dest-category-tags">
                  {dest.categories.slice(0, 3).map((cat, i) => (
                    <span key={i} className="mini-tag">{cat}</span>
                  ))}
                </div>
                <div className="dest-card-footer">
                  <span className="best-time">{t('home.best')}: {dest.bestTimeToVisit.split(',')[0]}</span>
                  <Link to={`/plan?dest=${encodeURIComponent(dest.name)}`} className="plan-dest-btn">
                    {t('home.planTripBtn')} <ChevronRight size={16} />
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
          <div className="header-tag">{t('home.interestsTag')}</div>
          <h2>{t('home.interestsTitle')}</h2>
          <p>{t('home.interestsSub')}</p>
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

      {/* How to Use TravelMate Interactive Walkthrough Video */}
      <HowToUseVideo />

      {/* How TravelMate AI Works */}
      <section className="how-it-works padding-section">
        <div className="section-header">
          <div className="header-tag">{t('home.howItWorksTag')}</div>
          <h2>{t('home.howItWorksTitle')}</h2>
          <p>{t('home.howItWorksSub')}</p>
        </div>
        <div className="workflow-steps-grid">
          <div className="step-card">
            <div className="step-number">01</div>
            <h3>{t('home.step1Title')}</h3>
            <p>{t('home.step1Desc')}</p>
          </div>
          <div className="step-card">
            <div className="step-number">02</div>
            <h3>{t('home.step2Title')}</h3>
            <p>{t('home.step2Desc')}</p>
          </div>
          <div className="step-card">
            <div className="step-number">03</div>
            <h3>{t('home.step3Title')}</h3>
            <p>{t('home.step3Desc')}</p>
          </div>
          <div className="step-card">
            <div className="step-number">04</div>
            <h3>{t('home.step4Title')}</h3>
            <p>{t('home.step4Desc')}</p>
          </div>
        </div>
      </section>

      {/* Live India Travel Insight Widget */}
      <section className="weather-insight-section padding-section bg-gradient-light">
        <div className="insight-card-inner">
          <div className="insight-icon"><Sun size={36} className="text-warning" /></div>
          <div className="insight-text">
            <h3>{t('home.weatherInsightTitle')}</h3>
            <p>
              {t('home.weatherInsightDesc')}
            </p>
          </div>
          <Link to="/plan" className="primary-btn start-cta-btn">
            {t('home.createAiItinerary')} <ArrowRight size={18} />
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
