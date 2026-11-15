import React, { useState, useEffect } from 'react';
import { MapPin, ArrowRight, Search, Sparkles, Loader2, Map, CheckCircle2, Compass } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useDestinationTranslation } from '../utils/destinationTranslations';
import { destinations, getAllCategories } from '../data/destinations';
import { placesService } from '../services/api';
import LocationAutocomplete from '../components/LocationAutocomplete';
import PlaceMapModal from '../components/PlaceMapModal';
import SafeImage from '../components/common/SafeImage';
import './Explore.css';

const Explore = () => {
  const { getDestName, getDestState } = useDestinationTranslation();
  const navigate = useNavigate();
  const location = useLocation();

  const [activeFilter, setActiveFilter] = useState('All');
  const [targetDestination, setTargetDestination] = useState('Vijayawada');
  const [dynamicPlaces, setDynamicPlaces] = useState([]);
  const [isLoadingDynamic, setIsLoadingDynamic] = useState(false);
  const [mapModalPlace, setMapModalPlace] = useState(null);

  const allCategories = [
    'All', 'Spiritual', 'Heritage', 'Nature', 'Beaches', 
    'Adventure', 'Food', 'Wildlife', 'Mountains', 'Museums', 'Shopping'
  ];

  useEffect(() => {
    // Parse URL parameter
    const params = new URLSearchParams(location.search);
    const categoryParam = params.get('category');
    const destParam = params.get('dest');
    
    if (destParam) {
      setTargetDestination(destParam);
    }

    if (categoryParam && allCategories.map(c => c.toLowerCase()).includes(categoryParam.toLowerCase())) {
      const matched = allCategories.find(c => c.toLowerCase() === categoryParam.toLowerCase());
      setActiveFilter(matched || 'All');
    } else {
      setActiveFilter('All');
    }
  }, [location]);

  // Fetch real category-based places dynamically
  useEffect(() => {
    if (activeFilter === 'All' && !targetDestination) {
      setDynamicPlaces([]);
      return;
    }

    const fetchCategoryPlaces = async () => {
      if (activeFilter === 'All') {
        setDynamicPlaces([]);
        return;
      }

      setIsLoadingDynamic(true);
      try {
        const dest = targetDestination.trim() || 'Vijayawada';
        const results = await placesService.getByCategory(dest, activeFilter);
        setDynamicPlaces(results || []);
      } catch (err) {
        console.error("Error fetching category places:", err);
        setDynamicPlaces([]);
      } finally {
        setIsLoadingDynamic(false);
      }
    };

    fetchCategoryPlaces();
  }, [activeFilter, targetDestination]);

  const handleDestinationSelect = (item) => {
    setTargetDestination(item.name);
  };

  const handlePlanTripToPlace = (spot) => {
    const params = new URLSearchParams();
    params.set('dest', spot.name || spot.title);
    if (spot.lat) params.set('lat', spot.lat);
    if (spot.lon) params.set('lon', spot.lon);
    if (spot.address) params.set('address', spot.address);
    navigate(`/plan?${params.toString()}`);
  };

  // Static fallback if activeFilter == All or no dynamic places returned
  const filteredDestinations = activeFilter === 'All' 
    ? destinations 
    : destinations.filter(dest => dest.categories.includes(activeFilter));

  return (
    <div className="explore-container animate-fade-in">
      <div className="explore-header">
        <div className="explore-badge">
          <Sparkles size={16} /> Dynamic India-Wide Category Discovery
        </div>
        <h1>Discover India by Interests</h1>
        <p>Explore real verified landmarks, beaches, temples, caves, and viewpoints across India.</p>
        
        {/* Dynamic Category Destination Selector */}
        <div className="explore-search-bar-wrap">
          <div className="explore-search-label">
            <Compass size={16} /> Explore In Destination:
          </div>
          <div className="explore-search-input-box">
            <LocationAutocomplete 
              value={targetDestination}
              onChange={(val) => setTargetDestination(val)}
              onSelect={handleDestinationSelect}
              placeholder="e.g. Vijayawada, Goa, Araku Valley, Visakhapatnam, Tirupati"
            />
          </div>
        </div>

        {/* Category Filter Chips */}
        <div className="filter-scroll-wrapper">
          <div className="filter-chips">
            {allCategories.map(filter => (
              <button 
                key={filter}
                className={`filter-chip ${activeFilter === filter ? 'active' : ''}`}
                onClick={() => setActiveFilter(filter)}
              >
                {filter}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Results Header / Meta */}
      <div className="results-meta">
        {isLoadingDynamic ? (
          <div className="loading-row">
            <Loader2 size={18} className="spin-icon text-primary" />
            <span>Discovering real verified <strong>{activeFilter}</strong> places near <strong>{targetDestination}</strong>...</span>
          </div>
        ) : dynamicPlaces.length > 0 ? (
          <p>
            Showing <strong>{dynamicPlaces.length}</strong> real verified <strong>{activeFilter}</strong> places discovered near <strong>{targetDestination}</strong>
          </p>
        ) : (
          <p>
            Showing {filteredDestinations.length} destination{filteredDestinations.length !== 1 ? 's' : ''} {activeFilter !== 'All' ? `for ${activeFilter}` : 'across India'}
          </p>
        )}
      </div>

      {/* Grid of Results */}
      <div className="explore-grid">
        {/* DYNAMIC DISCOVERED PLACES */}
        {dynamicPlaces.length > 0 ? (
          dynamicPlaces.map((spot, idx) => (
            <div key={idx} className="explore-card hover-lift">
              <div className="explore-image-wrapper">
                <SafeImage 
                  src={spot.image || spot.imageUrl || spot.photoUrl || spot.photo} 
                  alt={spot.name || 'Attraction'} 
                  category={spot.category || activeFilter}
                  destinationName={spot.name}
                  loading="lazy" 
                />
                <div className="explore-verified-tag">
                  <CheckCircle2 size={12} /> 100% Real Location
                </div>
              </div>
              
              <div className="explore-card-content">
                <div className="explore-card-header">
                  <h3>{spot.name || spot.title}</h3>
                  <span className="state-badge">
                    <MapPin size={12}/> {getDestName(targetDestination)}, India
                  </span>
                </div>
                
                {spot.lat && spot.lon && (
                  <div className="spot-coords-pill">
                    {parseFloat(spot.lat).toFixed(4)}° N, {parseFloat(spot.lon).toFixed(4)}° E
                  </div>
                )}

                <p className="explore-description">
                  {spot.description || `Verified real ${spot.category || activeFilter} point of interest located in and around ${targetDestination}.`}
                </p>
                
                <div className="explore-tags">
                  <span className="tag">{spot.category || activeFilter}</span>
                  <span className="tag">Verified GPS</span>
                </div>

                <div className="explore-card-footer">
                  <div className="card-actions-flex">
                    <button 
                      onClick={() => handlePlanTripToPlace(spot)} 
                      className="primary-btn sm-btn"
                    >
                      Plan a Trip
                    </button>
                    {spot.lat && spot.lon && (
                      <button 
                        onClick={() => setMapModalPlace(spot)} 
                        className="map-icon-btn"
                        title="Locate on Map"
                      >
                        <Map size={16} />
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))
        ) : filteredDestinations.length > 0 ? (
          /* CURATED DESTINATIONS FALLBACK */
          filteredDestinations.map((dest) => (
            <div key={dest.id} className="explore-card hover-lift">
              <Link to={`/plan?dest=${encodeURIComponent(dest.name)}`} className="explore-image-wrapper">
                <SafeImage 
                  src={dest.image || dest.imageUrl || dest.photoUrl || dest.heroImage} 
                  alt={dest.name} 
                  category={dest.categories?.[0] || 'Heritage'}
                  destinationId={dest.id}
                  destinationName={dest.name}
                  loading="lazy" 
                />
                <div className="explore-rating">★ {dest.rating}</div>
              </Link>
              
              <div className="explore-card-content">
                <div className="explore-card-header">
                  <h3>{getDestName(dest.name)}</h3>
                  <span className="state-badge"><MapPin size={12}/> {getDestState(dest.state)}</span>
                </div>
                
                <p className="explore-description">{dest.description}</p>
                
                <div className="explore-tags">
                  {dest.categories.slice(0, 3).map(tag => (
                    <span key={tag} className="tag">{tag}</span>
                  ))}
                  {dest.categories.length > 3 && <span className="tag">+{dest.categories.length - 3}</span>}
                </div>

                <div className="explore-card-footer">
                  <div className="best-time">
                    <small>Best time: {dest.bestTimeToVisit}</small>
                  </div>
                  <div className="card-actions">
                    <Link to={`/plan?dest=${encodeURIComponent(dest.name)}`} className="primary-btn sm-btn">
                      Plan a Trip
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          ))
        ) : (
          <div className="no-results">
            <p>No verified destinations found matching your criteria.</p>
            <button onClick={() => setActiveFilter('All')} className="secondary-btn">View All India</button>
          </div>
        )}
      </div>

      {/* Map Modal for Locate on Map */}
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

export default Explore;
