import React, { useState, useEffect } from 'react';
import { 
  MapPin, Calendar, DollarSign, Activity, Users, Send, Utensils, 
  Car, Info, Map, Sparkles, CheckCircle2, AlertCircle, Heart,
  Compass, Mountain, Trees, Sun, History, ShieldCheck, ArrowRight, Loader2, Clock
} from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { tripService } from '../services/api';
import LocationAutocomplete from '../components/LocationAutocomplete';
import './PlanTrip.css';

const PlanTrip = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const searchParams = new URLSearchParams(location.search);
  
  const initialDest = searchParams.get('dest') || '';
  const initialOrigin = searchParams.get('origin') || '';
  const initialStart = searchParams.get('start') || '';
  const initialStyle = searchParams.get('style') || 'Solo';

  const [formData, setFormData] = useState({
    startingLocation: initialOrigin,
    destinationCity: initialDest,
    locality: '',
    startDate: initialStart || new Date().toISOString().split('T')[0],
    endDate: '',
    planType: 'days', // 'days' | 'hours'
    durationHours: 3,
    startTime: '14:00',
    availableUntil: '17:00',
    maxDistanceKm: 10,
    travelRange: 'Moderate (5–10 km)',
    maxEntryFee: '',
    travelDistancePref: 'Nearby places only',
    numTravelers: 1,
    ageGroup: 'Adults (26-40)',
    budgetInr: 30000,
    travelStyle: initialStyle,
    interests: [],
    customInterests: '',
    activityLevel: 'Moderate',
    foodPreference: 'Any',
    transitPreference: 'Private Cab',
    specialRequirements: ''
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submissionStep, setSubmissionStep] = useState(0);
  const [errorMessage, setErrorMessage] = useState('');

  // Quick Interest tags
  const interestOptions = [
    { label: "Spiritual & Temples", icon: <ShieldCheck size={14} /> },
    { label: "Heritage & Forts", icon: <History size={14} /> },
    { label: "Scenic Viewpoints", icon: <Mountain size={14} /> },
    { label: "Beaches & Coastal", icon: <Sun size={14} /> },
    { label: "Authentic Food Trails", icon: <Utensils size={14} /> },
    { label: "Museums & Art", icon: <Compass size={14} /> },
    { label: "Nature & Parks", icon: <Trees size={14} /> },
    { label: "Adventure", icon: <Activity size={14} /> },
    { label: "Shopping & Handlooms", icon: <Heart size={14} /> },
    { label: "Entertainment", icon: <Sparkles size={14} /> }
  ];

  // Quick City Suggestions
  const popularOrigins = ['Gudivada', 'Hyderabad', 'Bengaluru', 'Visakhapatnam', 'Chennai', 'Delhi', 'Mumbai'];
  const popularDestinations = [
    'Vijayawada', 'Visakhapatnam', 'Araku Valley', 'Tirupati', 
    'Goa', 'Jaipur', 'Munnar', 'Varanasi', 'Ooty', 'Hampi', 'Madurai', 'Udaipur'
  ];

  // Quick Local Area Explorations
  const popularLocalities = [
    { city: 'Hyderabad', area: 'Kukatpally', label: 'Hyderabad → Kukatpally' },
    { city: 'Hyderabad', area: 'Banjara Hills', label: 'Hyderabad → Banjara Hills' },
    { city: 'Visakhapatnam', area: 'MVP Colony', label: 'Visakhapatnam → MVP Colony' },
    { city: 'Chennai', area: 'T Nagar', label: 'Chennai → T Nagar' },
    { city: 'Vijayawada', area: 'Kanaka Durga Temple area', label: 'Vijayawada → Kanaka Durga Temple area' },
    { city: 'Bengaluru', area: 'Indiranagar', label: 'Bengaluru → Indiranagar' }
  ];

  const toggleInterest = (interest) => {
    setFormData(prev => {
      const exists = prev.interests.includes(interest);
      if (exists) {
        return { ...prev, interests: prev.interests.filter(i => i !== interest) };
      } else {
        return { ...prev, interests: [...prev.interests, interest] };
      }
    });
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleDistanceRangeChange = (label, km) => {
    setFormData(prev => ({
      ...prev,
      travelRange: label,
      maxDistanceKm: km
    }));
  };

  // Compute duration in days if dates are present
  const getDurationDays = () => {
    if (formData.startDate && formData.endDate) {
      const d1 = new Date(formData.startDate);
      const d2 = new Date(formData.endDate);
      const diffTime = d2 - d1;
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
      return diffDays > 0 ? diffDays : 1;
    }
    return 1;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    
    // Check authentication
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (!token) {
      setErrorMessage('Please sign in or register to save your trip and generate an AI itinerary.');
      return;
    }

    // Check India-Only Scope
    const foreignCities = [
      "paris", "london", "dubai", "new york", "singapore", "bangkok", "tokyo", "rome", 
      "sydney", "toronto", "barcelona", "amsterdam", "los angeles", "berlin", "istanbul", 
      "kuala lumpur", "doha", "abu dhabi", "bali", "hong kong", "san francisco", "chicago", 
      "maldives", "switzerland", "zurich"
    ];
    const destLow = formData.destinationCity.trim().toLowerCase();
    if (foreignCities.some(fc => destLow === fc || destLow.startsWith(fc + " ") || destLow.startsWith(fc + ","))) {
      setErrorMessage("TravelMate AI currently supports travel within India.");
      return;
    }

    if (formData.planType === 'days' && formData.endDate && new Date(formData.endDate) < new Date(formData.startDate)) {
      setErrorMessage('End date cannot be earlier than start date.');
      return;
    }

    setIsSubmitting(true);
    setSubmissionStep(1);

    try {
      // Step progression animation
      setTimeout(() => setSubmissionStep(2), 700);
      setTimeout(() => setSubmissionStep(3), 1400);

      // Merge interests
      const combinedInterests = [...formData.interests];
      if (formData.customInterests.trim()) {
        const customParts = formData.customInterests.split(',').map(s => s.trim()).filter(Boolean);
        combinedInterests.push(...customParts);
      }
      if (combinedInterests.length === 0) {
        combinedInterests.push("Sightseeing", "Culture");
      }

      const isHourly = formData.planType === 'hours';
      const parsedHours = isHourly ? parseFloat(formData.durationHours) : null;

      const payload = {
        startingLocation: formData.startingLocation || 'India',
        destinationCity: formData.destinationCity.trim(),
        locality: formData.locality ? formData.locality.trim() : null,
        startDate: formData.startDate,
        endDate: isHourly ? formData.startDate : (formData.endDate || formData.startDate),
        numDays: isHourly ? 1 : getDurationDays(),
        durationHours: parsedHours,
        startTime: isHourly ? formData.startTime : "09:00",
        availableUntil: isHourly ? formData.availableUntil : "21:00",
        maxDistanceKm: formData.maxDistanceKm ? parseFloat(formData.maxDistanceKm) : null,
        maxEntryFee: formData.maxEntryFee !== '' && formData.maxEntryFee !== null ? parseFloat(formData.maxEntryFee) : null,
        travelDistancePref: formData.travelDistancePref,
        numTravelers: parseInt(formData.numTravelers) || 1,
        ageGroup: formData.ageGroup,
        budgetInr: parseFloat(formData.budgetInr) || 30000.0,
        travelStyle: formData.travelStyle,
        interests: combinedInterests,
        activityLevel: formData.activityLevel,
        foodPreference: formData.foodPreference,
        transitPreference: formData.transitPreference,
        specialRequirements: formData.specialRequirements
      };

      const createdTrip = await tripService.createTrip(payload);
      setSubmissionStep(4);
      
      // Navigate straight to itinerary
      setTimeout(() => {
        if (createdTrip && createdTrip.id) {
          navigate(`/trip/${createdTrip.id}`);
        } else {
          navigate('/dashboard');
        }
      }, 500);

    } catch (error) {
      console.error(error);
      setIsSubmitting(false);
      let msg = error.response?.data?.message || (typeof error.response?.data === 'string' ? error.response?.data : null);
      if (!msg) {
        if (error.response?.status === 403 || error.response?.status === 401) {
          msg = 'Your session has expired or authentication failed. Please sign in again or retry.';
        } else {
          msg = error.message || 'Error generating trip. Ensure backend is running.';
        }
      }
      setErrorMessage(typeof msg === 'string' ? msg : JSON.stringify(msg));
    }
  };

  return (
    <div className="plan-container animate-fade-in">
      <div className="plan-header">
        <div className="header-pill">
          <Sparkles size={16} /> Intelligent Journey Architect
        </div>
        <h1>Design Your <span className="text-gradient">India Journey</span></h1>
        <p>Tell our AI about your preferences and let it craft a structured, weather-adaptive itinerary.</p>
      </div>

      {errorMessage && (
        <div className="plan-error-banner">
          <AlertCircle size={18} />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Loading Modal Overlay during generation */}
      {isSubmitting && (
        <div className="generation-overlay">
          <div className="generation-modal">
            <div className="modal-spinner">
              <Loader2 size={44} className="spin-icon" />
            </div>
            <h3>TravelMate AI is Planning Your Trip</h3>
            <p className="destination-target">Destination: <strong>{formData.destinationCity}</strong></p>
            
            <div className="steps-progress-list">
              <div className={`step-item ${submissionStep >= 1 ? 'completed' : ''}`}>
                <CheckCircle2 size={16} /> Verifying real-world location & coordinates...
              </div>
              <div className={`step-item ${submissionStep >= 2 ? 'completed' : ''}`}>
                <CheckCircle2 size={16} /> Running Hybrid ML ranking & peer similarity...
              </div>
              <div className={`step-item ${submissionStep >= 3 ? 'completed' : ''}`}>
                <CheckCircle2 size={16} /> Connecting live 7-day meteorological forecast...
              </div>
              <div className={`step-item ${submissionStep >= 4 ? 'completed' : ''}`}>
                <CheckCircle2 size={16} /> Structuring daily timeline & interactive route...
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="form-wrapper">
        <form onSubmit={handleSubmit} className="plan-form">
          
          {/* SECTION 1: DESTINATION & ROUTE */}
          <div className="form-section">
            <h3 className="section-title">
              <span className="step-badge">1</span> Where & When Are You Travelling?
            </h3>

            {/* Plan Mode Toggle */}
            <div className="plan-mode-switcher">
              <button 
                type="button" 
                className={`mode-btn ${formData.planType === 'days' ? 'active' : ''}`}
                onClick={() => setFormData(prev => ({ ...prev, planType: 'days' }))}
              >
                <Calendar size={15} /> Multi-Day / Full Days
              </button>
              <button 
                type="button" 
                className={`mode-btn ${formData.planType === 'hours' ? 'active' : ''}`}
                onClick={() => setFormData(prev => ({ ...prev, planType: 'hours' }))}
              >
                <Clock size={15} /> Short Trip (30m – 6 Hours)
              </button>
            </div>
            
            <div className="form-grid">
              <div className="input-group">
                <label><MapPin size={16} className="input-icon" /> Starting Location (Origin in India)</label>
                <LocationAutocomplete 
                  value={formData.startingLocation}
                  onChange={(val) => setFormData(prev => ({ ...prev, startingLocation: val }))}
                  onSelect={(item) => setFormData(prev => ({ ...prev, startingLocation: item.name }))}
                  placeholder="e.g., Delhi, Mumbai, Bengaluru, Hyderabad, Kukatpally"
                  required
                />
                <div className="quick-select-row">
                  <span className="mini-label">Quick select:</span>
                  {popularOrigins.slice(0, 5).map(city => (
                    <button 
                      key={city} 
                      type="button" 
                      className="mini-chip-btn"
                      onClick={() => setFormData(prev => ({ ...prev, startingLocation: city }))}
                    >
                      {city}
                    </button>
                  ))}
                </div>
              </div>

              <div className="input-group">
                <label><Map size={16} className="input-icon" /> Destination City or Major Region</label>
                <LocationAutocomplete 
                  value={formData.destinationCity}
                  onChange={(val) => setFormData(prev => ({ ...prev, destinationCity: val }))}
                  onSelect={(item) => setFormData(prev => ({ ...prev, destinationCity: item.name }))}
                  placeholder="e.g. Hyderabad, Visakhapatnam, Vijayawada, Chennai"
                  required
                />
                <div className="quick-select-row">
                  <span className="mini-label">Popular:</span>
                  {popularDestinations.slice(0, 5).map(city => (
                    <button 
                      key={city} 
                      type="button" 
                      className="mini-chip-btn"
                      onClick={() => setFormData(prev => ({ ...prev, destinationCity: city }))}
                    >
                      {city}
                    </button>
                  ))}
                </div>
              </div>

              <div className="input-group full-width">
                <label><Compass size={16} className="input-icon" /> Specific Area / Locality (Local Level Exploration)</label>
                <input 
                  type="text"
                  name="locality"
                  value={formData.locality}
                  onChange={handleChange}
                  placeholder="e.g., Kukatpally, Banjara Hills, MVP Colony, T Nagar, Indiranagar"
                />
                <div className="quick-select-row">
                  <span className="mini-label">Direct Area Exploration:</span>
                  {popularLocalities.map(loc => (
                    <button 
                      key={loc.label} 
                      type="button" 
                      className="mini-chip-btn"
                      onClick={() => setFormData(prev => ({ 
                        ...prev, 
                        destinationCity: loc.city, 
                        locality: loc.area,
                        startingLocation: prev.startingLocation || loc.area
                      }))}
                    >
                      {loc.label}
                    </button>
                  ))}
                </div>
              </div>

              {formData.planType === 'days' ? (
                <>
                  <div className="input-group">
                    <label><Calendar size={16} className="input-icon" /> Start Date</label>
                    <input 
                      type="date" 
                      name="startDate" 
                      value={formData.startDate} 
                      onChange={handleChange} 
                      required 
                    />
                  </div>
                  
                  <div className="input-group">
                    <label><Calendar size={16} className="input-icon" /> End Date ({getDurationDays()} Days)</label>
                    <input 
                      type="date" 
                      name="endDate" 
                      value={formData.endDate} 
                      onChange={handleChange} 
                      required 
                    />
                  </div>
                </>
              ) : (
                <>
                  <div className="input-group">
                    <label><Clock size={16} className="input-icon" /> Trip Duration</label>
                    <select 
                      name="durationHours" 
                      value={formData.durationHours} 
                      onChange={handleChange}
                    >
                      <option value="0.5">30 minutes (Quick Spot)</option>
                      <option value="1">1 hour (1 Activity + Travel)</option>
                      <option value="2">2 hours (1-2 Activities + Travel)</option>
                      <option value="3">3 hours (2 Activities + Travel)</option>
                      <option value="4">4 hours (Half Day • 2-3 Activities)</option>
                      <option value="5">5 hours (3 Activities)</option>
                      <option value="6">6 hours (3-4 Activities)</option>
                    </select>
                  </div>

                  <div className="input-group">
                    <label><Calendar size={16} className="input-icon" /> Trip Date</label>
                    <input 
                      type="date" 
                      name="startDate" 
                      value={formData.startDate} 
                      onChange={handleChange} 
                      required 
                    />
                  </div>

                  <div className="input-group">
                    <label><Clock size={16} className="input-icon" /> Start Time</label>
                    <input 
                      type="time" 
                      name="startTime" 
                      value={formData.startTime} 
                      onChange={handleChange} 
                      required 
                    />
                  </div>

                  <div className="input-group">
                    <label><Clock size={16} className="input-icon" /> Available Until</label>
                    <input 
                      type="time" 
                      name="availableUntil" 
                      value={formData.availableUntil} 
                      onChange={handleChange} 
                      required 
                    />
                  </div>
                </>
              )}
            </div>
          </div>

          {/* SECTION 2: TRAVELLER PROFILE */}
          <div className="form-section">
            <h3 className="section-title">
              <span className="step-badge">2</span> Traveller Profile & Party
            </h3>
            
            <div className="form-grid">
              <div className="input-group">
                <label><Users size={16} className="input-icon" /> Number of Travelers</label>
                <input 
                  type="number" 
                  name="numTravelers" 
                  min="1" 
                  max="20" 
                  value={formData.numTravelers} 
                  onChange={handleChange} 
                />
              </div>

              <div className="input-group">
                <label>Travel Party Style</label>
                <select name="travelStyle" value={formData.travelStyle} onChange={handleChange}>
                  <option value="Solo">Solo Traveler</option>
                  <option value="Couple">Couple / Romantic</option>
                  <option value="Family">Family with Children</option>
                  <option value="Friends">Friends Group</option>
                </select>
              </div>

              <div className="input-group">
                <label>Age Group</label>
                <select name="ageGroup" value={formData.ageGroup} onChange={handleChange}>
                  <option value="Young Adults (18-25)">Young Adults (18-25)</option>
                  <option value="Adults (26-40)">Adults (26-40)</option>
                  <option value="Middle Aged (41-60)">Middle Aged (41-60)</option>
                  <option value="Seniors (60+)">Seniors (60+)</option>
                  <option value="Mixed Family">Mixed Family Ages</option>
                </select>
              </div>

              <div className="input-group">
                <label><DollarSign size={16} className="input-icon" /> Total Budget (INR)</label>
                <input 
                  type="number" 
                  name="budgetInr" 
                  min="5000" 
                  step="1000" 
                  value={formData.budgetInr} 
                  onChange={handleChange} 
                />
                <div className="quick-select-row">
                  <span className="mini-label">Presets:</span>
                  {[15000, 30000, 50000, 80000].map(amt => (
                    <button 
                      key={amt} 
                      type="button" 
                      className="mini-chip-btn"
                      onClick={() => setFormData(prev => ({ ...prev, budgetInr: amt }))}
                    >
                      ₹{amt.toLocaleString('en-IN')}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* SECTION 3: PREFERENCES & PACE */}
          <div className="form-section">
            <h3 className="section-title">
              <span className="step-badge">3</span> Interests, Pace & Preferences
            </h3>

            <div className="input-group full-width">
              <label>What are your primary travel interests? (Select all that apply)</label>
              <div className="interest-chips-container">
                {interestOptions.map(opt => {
                  const isSelected = formData.interests.includes(opt.label);
                  return (
                    <button 
                      key={opt.label}
                      type="button"
                      className={`interest-tag-btn ${isSelected ? 'selected' : ''}`}
                      onClick={() => toggleInterest(opt.label)}
                    >
                      {opt.icon}
                      <span>{opt.label}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="input-group full-width">
              <label>Additional Specific Interests (Optional)</label>
              <input 
                type="text" 
                name="customInterests" 
                placeholder="e.g., Photography, Local handicraft shopping, Waterfalls, Coffee tasting" 
                value={formData.customInterests} 
                onChange={handleChange} 
              />
            </div>

            <div className="form-grid">
              <div className="input-group">
                <label><Compass size={16} className="input-icon" /> How Far Do You Want To Travel? (Distance Range)</label>
                <select 
                  name="travelRange" 
                  value={formData.travelRange} 
                  onChange={(e) => {
                    const val = e.target.value;
                    let km = 10;
                    if (val.includes('0–2')) km = 2;
                    else if (val.includes('2–5')) km = 5;
                    else if (val.includes('5–10')) km = 10;
                    else if (val.includes('10–25')) km = 25;
                    else km = null;
                    handleDistanceRangeChange(val, km);
                  }}
                >
                  <option value="Very Close (0–2 km)">Very Close (0–2 km • Local Walking / Auto)</option>
                  <option value="Nearby (2–5 km)">Nearby (2–5 km • Short Local Commute)</option>
                  <option value="Moderate (5–10 km)">Moderate (5–10 km • City Suburb Range)</option>
                  <option value="Farther (10–25 km)">Farther (10–25 km • Extended City Exploration)</option>
                  <option value="Any Distance">Any Distance (No Radius Restriction)</option>
                </select>
              </div>

              <div className="input-group">
                <label><DollarSign size={16} className="input-icon" /> Max Attraction Entry Fee Filter</label>
                <select 
                  name="maxEntryFee" 
                  value={formData.maxEntryFee} 
                  onChange={handleChange}
                >
                  <option value="">Any Ticket Price (No Filter)</option>
                  <option value="0">Free Entry Only (₹0)</option>
                  <option value="50">Under ₹50 (₹1–₹50)</option>
                  <option value="100">Under ₹100 (₹50–₹100)</option>
                  <option value="200">Under ₹200 (₹100–₹200)</option>
                  <option value="500">Under ₹500 (₹200–₹500)</option>
                </select>
              </div>

              <div className="input-group">
                <label><Car size={16} className="input-icon" /> Travel Distance Preference</label>
                <select 
                  name="travelDistancePref" 
                  value={formData.travelDistancePref} 
                  onChange={handleChange}
                >
                  <option value="Minimal travel">Minimal travel (Keep to immediate surroundings)</option>
                  <option value="Nearby places only">Nearby places only (Within local neighbourhood)</option>
                  <option value="Moderate travel">Moderate travel (Balanced local commute)</option>
                  <option value="Explore farther">Explore farther (Discover perimeter & scenic attractions)</option>
                </select>
              </div>

              <div className="input-group">
                <label><Activity size={16} className="input-icon" /> Preferred Pace</label>
                <select name="activityLevel" value={formData.activityLevel} onChange={handleChange}>
                  <option value="Relaxed">Relaxed (1-2 places/day • Unhurried)</option>
                  <option value="Moderate">Moderate (3-4 places/day • Balanced)</option>
                  <option value="Packed">Fast-Paced (5+ places/day • Action packed)</option>
                </select>
              </div>

              <div className="input-group">
                <label><Utensils size={16} className="input-icon" /> Culinary / Food Preference</label>
                <select name="foodPreference" value={formData.foodPreference} onChange={handleChange}>
                  <option value="Any">Any Local Cuisine</option>
                  <option value="Vegetarian">Pure Vegetarian</option>
                  <option value="Non-Vegetarian">Non-Vegetarian / Seafood</option>
                  <option value="South Indian">Traditional South Indian</option>
                  <option value="North Indian">North Indian Delicacies</option>
                  <option value="Street Food">Local Street Food Trails</option>
                  <option value="Vegan">Vegan</option>
                </select>
              </div>

              <div className="input-group">
                <label><Car size={16} className="input-icon" /> Transportation Preference</label>
                <select name="transitPreference" value={formData.transitPreference} onChange={handleChange}>
                  <option value="Flight">Flight (Fastest / Air Travel)</option>
                  <option value="Train">Train (Indian Railways Express)</option>
                  <option value="Bus">Bus (Intercity State RTC / Volvo)</option>
                  <option value="Private Cab">Private Cab / Taxi (Most convenient)</option>
                  <option value="Self-Drive Rental">Self-Drive Rental Car / Bike</option>
                  <option value="Auto Rickshaw / Walking">Auto Rickshaw & Walking Strolls</option>
                </select>
                {formData.transitPreference === 'Flight' && formData.startingLocation && (
                  <div style={{ marginTop: '0.4rem', fontSize: '0.78rem', color: '#b45309', background: '#fef3c7', padding: '0.35rem 0.6rem', borderRadius: '6px' }}>
                    <Info size={13} style={{ display: 'inline', verticalAlign: 'middle', marginRight: '4px' }} />
                    Note: Air routes are automatically validated against real DGCA/commercial airport availability.
                  </div>
                )}
              </div>

              <div className="input-group">
                <label><Info size={16} className="input-icon" /> Special Requirements (Optional)</label>
                <input 
                  type="text" 
                  name="specialRequirements" 
                  placeholder="e.g., Senior citizen friendly (few stairs), Kid stroller, Pet friendly" 
                  value={formData.specialRequirements} 
                  onChange={handleChange} 
                />
              </div>
            </div>
          </div>

          <div className="form-actions">
            <button 
              type="submit" 
              className="primary-btn hover-lift generate-btn"
              disabled={isSubmitting}
            >
              <Send size={18} />
              <span>{isSubmitting ? 'Generating Itinerary...' : 'Generate AI Itinerary'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default PlanTrip;
