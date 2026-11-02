import React from 'react';
import { X, MapPin, Navigation, ExternalLink } from 'lucide-react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import './PlaceMapModal.css';

// Fix Leaflet marker icon
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const customPin = L.divIcon({
  className: 'custom-map-pin',
  html: `<div class="pin-active-marker">📍</div>`,
  iconSize: [36, 36],
  iconAnchor: [18, 36],
  popupAnchor: [0, -36]
});

const PlaceMapModal = ({ place, onClose, onPlanTrip }) => {
  if (!place || !place.lat || !place.lon) return null;

  const lat = parseFloat(place.lat);
  const lon = parseFloat(place.lon);
  const googleMapsUrl = `https://www.google.com/maps/search/?api=1&query=${lat},${lon}`;

  return (
    <div className="place-map-modal-overlay" onClick={onClose}>
      <div className="place-map-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title-wrap">
            <span className="modal-badge"><MapPin size={14} /> Real Indian Coordinates</span>
            <h3>{place.name}</h3>
            <p className="modal-sub">
              {place.city ? `${place.city}, ` : ''}{place.state || 'India'} • {lat.toFixed(4)}° N, {lon.toFixed(4)}° E
            </p>
          </div>
          <button className="close-btn" onClick={onClose} aria-label="Close">
            <X size={20} />
          </button>
        </div>

        <div className="modal-map-wrap">
          <MapContainer 
            center={[lat, lon]} 
            zoom={14} 
            scrollWheelZoom={true} 
            style={{ width: '100%', height: '100%' }}
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <Marker position={[lat, lon]} icon={customPin}>
              <Popup>
                <div className="map-popup-inner">
                  <h4>{place.name}</h4>
                  <p>{place.address || `${place.city || ''}, ${place.state || ''}`}</p>
                  <span className="popup-coords">{lat.toFixed(4)}, {lon.toFixed(4)}</span>
                </div>
              </Popup>
            </Marker>
          </MapContainer>
        </div>

        <div className="modal-footer">
          <div className="modal-info-pills">
            <span className="pill">Verified: {place.category || 'Sightseeing'}</span>
            <a 
              href={googleMapsUrl} 
              target="_blank" 
              rel="noopener noreferrer" 
              className="google-maps-link"
            >
              Open in Google Maps <ExternalLink size={14} />
            </a>
          </div>

          <div className="modal-actions">
            <button className="secondary-btn" onClick={onClose}>Close Map</button>
            {onPlanTrip && (
              <button className="primary-btn" onClick={onPlanTrip}>
                <Navigation size={16} /> Plan Trip Here
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default PlaceMapModal;
