import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useDestinationTranslation } from '../utils/destinationTranslations';
import { 
  Calendar, DollarSign, ArrowRight, Trash2, Edit3, 
  Users, Check, X, Sparkles, AlertCircle
} from 'lucide-react';
import { tripService } from '../services/api';
import './Dashboard.css';

const INTEREST_OPTIONS = [
  'Heritage', 'Nature', 'Food', 'Culture', 'Temples', 'Adventure', 'Beaches', 'Museums'
];

const Dashboard = () => {
  const { t } = useTranslation();
  const { getDestName, formatTripTitle } = useDestinationTranslation();
  const [trips, setTrips] = useState([]);
  const [invitations, setInvitations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  // Edit Trip Modal state
  const [editingTrip, setEditingTrip] = useState(null);
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

  const currentUserEmail = localStorage.getItem('userEmail') || sessionStorage.getItem('userEmail') || '';

  const showToast = (msg) => {
    setToast(msg);
    setTimeout(() => setToast(null), 4000);
  };

  const fetchTripsAndInvites = async () => {
    try {
      const [tripsData, invitesData] = await Promise.all([
        tripService.getUserTrips(),
        tripService.getUserInvitations()
      ]);
      setTrips(tripsData || []);
      setInvitations(invitesData || []);
    } catch (error) {
      console.error('Failed to fetch trips or invitations', error);
      setTrips([]);
      setInvitations([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTripsAndInvites();
  }, []);

  const handleDelete = async (tripId) => {
    if (window.confirm("Are you sure you want to delete this trip? All shared members will lose access.")) {
      try {
        await tripService.deleteTrip(tripId);
        showToast('Trip deleted successfully');
        fetchTripsAndInvites();
      } catch (error) {
        const msg = error.response?.data?.message || "Failed to delete trip.";
        alert(msg);
      }
    }
  };

  // Open Edit Modal with current values
  const handleOpenEdit = (trip) => {
    const existingInterests = trip.interests ? trip.interests.split(',').map(s => s.trim()).filter(Boolean) : ['Heritage', 'Food'];
    setEditingTrip(trip);
    setEditError(null);
    setEditFormData({
      destinationCity: trip.destinationCity || '',
      startingLocation: trip.startingLocation || '',
      startDate: trip.startDate || '',
      endDate: trip.endDate || '',
      numDays: trip.numDays || 2,
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

  // Date range change auto-calculates duration
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

  const handleSaveEdit = async (e) => {
    e.preventDefault();
    if (!editingTrip) return;
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

      await tripService.updateTrip(editingTrip.id, payload);
      showToast(`✓ Trip updated & itinerary recalculated successfully!`);
      setEditingTrip(null);
      await fetchTripsAndInvites();
    } catch (err) {
      console.error('Failed to update trip', err);
      setEditError(err.response?.data?.message || 'Failed to update trip. Please verify your inputs.');
    } finally {
      setIsSavingEdit(false);
    }
  };

  const handleRespondInvitation = async (inviteId, action) => {
    try {
      await tripService.respondToInvitation(inviteId, action);
      showToast(action === 'ACCEPT' ? '✓ Trip invitation accepted!' : 'Invitation declined.');
      fetchTripsAndInvites();
    } catch (error) {
      alert(error.response?.data?.message || 'Failed to respond to invitation.');
    }
  };

  // Helper to determine role badge
  const getRoleBadge = (trip) => {
    if (!currentUserEmail) return null;
    const isOwner = trip.user && trip.user.email && trip.user.email.toLowerCase() === currentUserEmail.toLowerCase();
    if (isOwner) {
      return <span className="trip-role-tag owner">Owner</span>;
    }
    const member = trip.members?.find(m => m.user?.email?.toLowerCase() === currentUserEmail.toLowerCase());
    if (member) {
      return <span className={`trip-role-tag ${member.role?.toLowerCase()}`}>{member.role}</span>;
    }
    return null;
  };

  if (loading) {
    return <div className="dashboard-container"><p>{t('common.loading')}</p></div>;
  }

  return (
    <div className="dashboard-container animate-fade-in">
      {/* Toast */}
      {toast && (
        <div className="dashboard-toast">
          <span>{toast}</span>
        </div>
      )}

      <div className="dashboard-header">
        <div className="header-content">
          <h1 className="text-gradient">{t('dashboard.title')}</h1>
          <p>{t('dashboard.subtitle')}</p>
        </div>
        <Link to="/plan" className="primary-btn" style={{textDecoration: 'none'}}>
          + {t('nav.planTrip')}
        </Link>
      </div>

      {/* PENDING TRIP INVITATIONS BANNER */}
      {invitations.length > 0 && (
        <div className="invitations-section animate-fade-in">
          <div className="invitations-card glass-panel">
            <div className="invitations-header">
              <Users size={20} className="icon-blue" />
              <h3>{t('dashboard.pendingInvites')} ({invitations.length})</h3>
            </div>
            <div className="invitations-list">
              {invitations.map(inv => (
                <div key={inv.id} className="invitation-item">
                  <div className="invitation-details">
                    <p className="invite-title">
                      <strong>{inv.invitedByName || inv.invitedByEmail || 'A traveler'}</strong> invited you to join the trip:
                    </p>
                    <div className="invite-trip-name">
                      <strong>{inv.tripTitle ? formatTripTitle(inv.tripTitle, inv.tripDestination) : formatTripTitle(`Trip to ${inv.tripDestination}`, inv.tripDestination)}</strong>
                      {inv.tripDestination && <span className="invite-dest">({inv.tripStartingLocation ? `${getDestName(inv.tripStartingLocation)} → ` : ''}{getDestName(inv.tripDestination)})</span>}
                    </div>
                    <div className="invite-meta">
                      {inv.tripStartDate && <span>📅 {inv.tripStartDate} to {inv.tripEndDate}</span>}
                      {inv.tripBudget && <span>💰 ₹{inv.tripBudget}</span>}
                      <span className="invite-role-tag">Role: {inv.role || 'VIEWER'}</span>
                    </div>
                  </div>
                  <div className="invitation-actions">
                    <button 
                      onClick={() => handleRespondInvitation(inv.id, 'ACCEPT')} 
                      className="btn-accept-invite"
                    >
                      <Check size={16} /> {t('dashboard.accept')}
                    </button>
                    <button 
                      onClick={() => handleRespondInvitation(inv.id, 'DECLINE')} 
                      className="btn-decline-invite"
                    >
                      <X size={16} /> {t('dashboard.decline')}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* TRIPS GRID */}
      <div className="trips-grid">
        {trips.length === 0 ? (
          <div className="empty-state glass-panel">
            <p>{t('dashboard.noTrips')}</p>
            <Link to="/plan" className="primary-btn" style={{marginTop: '15px', display: 'inline-block'}}>{t('dashboard.startPlanningBtn')}</Link>
          </div>
        ) : (
          trips.map(trip => (
            <div key={trip.id} className="trip-card glass-panel hover-lift">
              <div className="trip-card-header">
                <div>
                  <h3>{trip.title ? formatTripTitle(trip.title, trip.destinationCity) : formatTripTitle(`Trip to ${trip.destinationCity}`, trip.destinationCity)}</h3>
                  {getRoleBadge(trip)}
                </div>
                <span className={`status-badge ${trip.status === 'Generated' || trip.status === 'PLANNED' ? 'active' : ''}`}>
                  {trip.status || 'PLANNED'}
                </span>
              </div>
              
              <div className="trip-card-body">
                <div className="trip-detail">
                  <Calendar size={16} className="detail-icon" />
                  <span>{trip.startDate} to {trip.endDate} ({trip.days?.length || trip.numDays || 1} {t('dashboard.days', { count: '' }).trim() || 'Days'})</span>
                </div>
                <div className="trip-detail">
                  <DollarSign size={16} className="detail-icon" />
                  <span>₹{(trip.budgetInr || trip.totalBudget || 0).toLocaleString('en-IN')}</span>
                </div>
                {trip.members && trip.members.filter(m => m.status === 'ACCEPTED').length > 0 && (
                  <div className="trip-detail shared-indicator">
                    <Users size={16} className="detail-icon" />
                    <span>Shared with {trip.members.filter(m => m.status === 'ACCEPTED').length} traveler{trip.members.filter(m => m.status === 'ACCEPTED').length > 1 ? 's' : ''}</span>
                  </div>
                )}
                {trip.lastUpdateNotice && (
                  <div className="trip-last-update-note">
                    <Sparkles size={13} /> {trip.lastUpdateNotice}
                  </div>
                )}
              </div>

              {/* ACTION BAR: [Delete] [Edit] [View] */}
              <div className="trip-card-footer" style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                <button 
                  onClick={() => handleDelete(trip.id)} 
                  className="delete-btn" 
                  title="Delete Trip"
                  style={{background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px', fontSize: '0.9rem'}}
                >
                  <Trash2 size={16} /> {t('common.delete')}
                </button>

                <button 
                  onClick={() => handleOpenEdit(trip)} 
                  className="edit-btn" 
                  title="Edit trip inputs & preferences"
                  style={{background: 'none', border: 'none', color: '#0284c7', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px', fontWeight: 600, fontSize: '0.9rem'}}
                >
                  <Edit3 size={16} /> {t('common.edit')}
                </button>

                <Link to={`/trip/${trip.id}`} className="view-btn text-gradient" style={{textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '5px', fontWeight: 600, fontSize: '0.9rem'}}>
                  {t('dashboard.openTrip')} <ArrowRight size={16} />
                </Link>
              </div>
            </div>
          ))
        )}
      </div>

      {/* ========================================================
          EDIT TRIP MODAL (Preserves Trip ID, Recalculates Plan)
          ======================================================== */}
      {editingTrip && (
        <div className="modal-backdrop">
          <div className="edit-trip-modal glass-panel animate-scale-up">
            <div className="edit-modal-header">
              <div>
                <h2>{t('modals.editTitle')}: {editingTrip.title ? formatTripTitle(editingTrip.title, editingTrip.destinationCity) : getDestName(editingTrip.destinationCity)}</h2>
                <p className="edit-modal-subtitle">Trip ID #{editingTrip.id} • Modify preferences and recalculate itinerary.</p>
              </div>
              <button className="modal-close-btn" onClick={() => setEditingTrip(null)}>✕</button>
            </div>

            {editError && (
              <div className="edit-error-banner">
                <AlertCircle size={16} /> {editError}
              </div>
            )}

            <form onSubmit={handleSaveEdit} className="edit-form-content">
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

              {/* INTERESTS SELECTION */}
              <div className="form-group">
                <label>Interests & Themes</label>
                <div className="interest-chips-container">
                  {INTEREST_OPTIONS.map(interest => (
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
                  <label>Distance Preference (Max km from center)</label>
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
                  onClick={() => setEditingTrip(null)}
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

    </div>
  );
};

export default Dashboard;
