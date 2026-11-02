import React from 'react';
import { 
  MapPin, Star, Clock, CheckCircle2, Navigation, Map, 
  ArrowRight, Compass, ShieldCheck, ExternalLink, Sparkles 
} from 'lucide-react';
import SafeImage from './common/SafeImage';
import './PlaceDiscoveryCard.css';

const PlaceDiscoveryCard = ({ place, onPlanTrip, onLocateOnMap }) => {
  if (!place) return null;

  const lat = place.lat ? parseFloat(place.lat).toFixed(4) : null;
  const lon = place.lon ? parseFloat(place.lon).toFixed(4) : null;

  return (
    <div className="place-discovery-card animate-fade-in">
      <div className="discovery-card-top">
        {/* Place Image */}
        <div className="discovery-image-container">
          <SafeImage 
            src={place.image || place.imageUrl || place.photoUrl || place.photo} 
            alt={place.name} 
            category={place.category || 'Indian Landmark'}
            destinationName={place.name}
            className="discovery-place-img" 
          />
          <span className="verified-badge">
            <CheckCircle2 size={13} /> 100% Real Location • Verified
          </span>
          {place.category && (
            <span className="category-pill">{place.category}</span>
          )}
        </div>

        {/* Place Core Details */}
        <div className="discovery-details-container">
          <div className="discovery-header-row">
            <div>
              <div className="location-sub-pill">
                <MapPin size={13} /> {place.city ? `${place.city}, ` : ''}{place.state || 'India'}
              </div>
              <h2 className="place-title">{place.name}</h2>
            </div>
            
            {lat && lon && (
              <div className="coordinates-badge" title="Verified Real GPS Coordinates">
                <span className="coord-label">GPS</span>
                <span>{lat}° N, {lon}° E</span>
              </div>
            )}
          </div>

          {/* Real Data Meta Grid */}
          <div className="discovery-meta-grid">
            <div className="meta-box">
              <span className="meta-title"><Star size={13} className="text-amber" /> Rating</span>
              <span className="meta-val">
                {place.rating && place.rating !== "Information unavailable" 
                  ? `★ ${place.rating}` 
                  : "Information unavailable"}
              </span>
            </div>

            <div className="meta-box">
              <span className="meta-title">Reviews</span>
              <span className="meta-val">
                {place.reviewsCount && place.reviewsCount !== "Information unavailable" 
                  ? `${place.reviewsCount} verified` 
                  : "Information unavailable"}
              </span>
            </div>

            <div className="meta-box">
              <span className="meta-title"><Clock size={13} /> Opening Hours</span>
              <span className="meta-val">
                {place.openingHours && place.openingHours !== "Information unavailable" 
                  ? place.openingHours 
                  : "Information unavailable"}
              </span>
            </div>
          </div>

          {/* Full Verified Address */}
          <div className="discovery-address-row">
            <span className="address-label">Address:</span>
            <span className="address-text">{place.address || `${place.city || ''}, ${place.state || 'India'}`}</span>
          </div>

          {/* Real Description / Extract */}
          {place.description && (
            <p className="discovery-description">{place.description}</p>
          )}

          {/* Main Action Buttons */}
          <div className="discovery-actions-row">
            <button 
              className="primary-btn plan-trip-btn hover-lift"
              onClick={() => onPlanTrip(place)}
            >
              <Navigation size={18} />
              <span>Plan Trip to This Destination</span>
              <ArrowRight size={16} />
            </button>

            {lat && lon && (
              <button 
                className="secondary-btn locate-map-btn hover-lift"
                onClick={() => onLocateOnMap(place)}
              >
                <Map size={18} />
                <span>Locate on Map</span>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Discovered Nearby Attractions Section */}
      {place.nearbyAttractions && place.nearbyAttractions.length > 0 && (
        <div className="nearby-attractions-section">
          <div className="nearby-section-header">
            <div>
              <span className="mini-tag-pill"><Sparkles size={12} /> Real Destination Discovery</span>
              <h3>Verified Attractions in & around {place.city || place.name}</h3>
              <p>Top real-world landmarks and points of interest verified nearby</p>
            </div>
          </div>

          <div className="nearby-attractions-grid">
            {place.nearbyAttractions.map((spot, idx) => (
              <div key={idx} className="nearby-spot-card hover-lift">
                <div className="spot-img-wrap">
                  <SafeImage 
                    src={spot.image} 
                    alt={spot.name || spot.title} 
                    category={spot.category || 'Sightseeing'}
                    destinationName={spot.name || spot.title}
                  />
                  <span className="spot-cat">{spot.category || 'Sightseeing'}</span>
                </div>
                <div className="spot-body">
                  <h4>{spot.name || spot.title}</h4>
                  {spot.lat && spot.lon && (
                    <span className="spot-coords">
                      {parseFloat(spot.lat).toFixed(4)}°, {parseFloat(spot.lon).toFixed(4)}°
                    </span>
                  )}
                  <button 
                    className="plan-spot-link"
                    onClick={() => onPlanTrip(spot)}
                  >
                    Plan with this spot <ArrowRight size={13} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default PlaceDiscoveryCard;
