import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Shield, 
  Users, 
  Compass, 
  Globe, 
  CalendarCheck, 
  RefreshCw, 
  LogOut, 
  Clock, 
  AlertCircle,
  CheckCircle2,
  MapPin
} from 'lucide-react';
import { adminService, authService } from '../services/api';
import { useDestinationTranslation } from '../utils/destinationTranslations';
import './AdminDashboard.css';

const AdminDashboard = () => {
  const navigate = useNavigate();
  const { getDestName } = useDestinationTranslation();

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [overview, setOverview] = useState({
    totalUsers: 0,
    totalTrips: 0,
    totalDestinations: 0,
    totalItineraries: 0,
    recentUsers: [],
    recentTrips: [],
    recentActivity: []
  });

  const adminEmail = localStorage.getItem('userEmail') || sessionStorage.getItem('userEmail') || 'Administrator';
  const adminName = localStorage.getItem('fullName') || sessionStorage.getItem('fullName') || 'Administrator';

  const fetchOverviewData = async (isManualRefresh = false) => {
    if (isManualRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setError('');

    try {
      const data = await adminService.getOverview();
      setOverview(data);
    } catch (err) {
      console.error('Failed to load admin overview:', err);
      const msg = err.response?.data?.message || 'Failed to load live database statistics. Please ensure you are logged in as an administrator.';
      setError(msg);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchOverviewData();
  }, []);

  const handleLogout = async () => {
    await authService.adminLogout();
    navigate('/auth?mode=admin');
  };

  const formatDate = (dateString) => {
    if (!dateString) return '—';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric'
      });
    } catch (e) {
      return dateString;
    }
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return '—';
    try {
      const date = new Date(dateString);
      return date.toLocaleString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateString;
    }
  };

  return (
    <div className="admin-dashboard-container animate-fade-in">
      {/* Header */}
      <div className="admin-header">
        <div className="admin-header-title">
          <div className="admin-shield-icon">
            <Shield size={26} />
          </div>
          <div>
            <h1>Admin Dashboard</h1>
            <p>System overview, live MySQL statistics, and administrative audit logs</p>
          </div>
        </div>

        <div className="admin-header-actions">
          <div className="admin-user-pill">
            <span style={{ fontWeight: '600' }}>{adminEmail}</span>
            <span className="admin-role-badge">ADMIN</span>
          </div>

          <button 
            onClick={() => fetchOverviewData(true)} 
            className={`admin-btn btn-refresh ${refreshing ? 'spinning' : ''}`}
            disabled={refreshing || loading}
            title="Refresh database statistics"
          >
            <RefreshCw size={16} />
            <span>{refreshing ? 'Refreshing...' : 'Refresh Data'}</span>
          </button>

          <button 
            onClick={handleLogout} 
            className="admin-btn btn-logout"
            title="Sign out of Administrator Portal"
          >
            <LogOut size={16} />
            <span>Admin Logout</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="admin-error-banner">
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {/* Overview Statistics Grid */}
      <div className="admin-stats-grid">
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-users">
            <Users size={26} />
          </div>
          <div className="stat-details">
            <span className="stat-value">{loading ? '...' : overview.totalUsers.toLocaleString()}</span>
            <span className="stat-label">Total Registered Users</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-trips">
            <Compass size={26} />
          </div>
          <div className="stat-details">
            <span className="stat-value">{loading ? '...' : overview.totalTrips.toLocaleString()}</span>
            <span className="stat-label">Total Trips Created</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-destinations">
            <Globe size={26} />
          </div>
          <div className="stat-details">
            <span className="stat-value">{loading ? '...' : overview.totalDestinations.toLocaleString()}</span>
            <span className="stat-label">Total Destinations Available</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-itineraries">
            <CalendarCheck size={26} />
          </div>
          <div className="stat-details">
            <span className="stat-value">{loading ? '...' : overview.totalItineraries.toLocaleString()}</span>
            <span className="stat-label">Total Itineraries Generated</span>
          </div>
        </div>
      </div>

      {/* Two-Column Grid: Recent Users & Recent Trips */}
      <div className="admin-content-grid">
        {/* Recent Registered Users */}
        <div className="admin-card">
          <div className="admin-card-header">
            <h2>
              <Users size={18} style={{ color: 'var(--accent-primary)' }} />
              Recent Registered Users
            </h2>
            <span className="admin-card-count">{overview.recentUsers?.length || 0} users</span>
          </div>
          <div className="admin-table-container">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Registered</th>
                </tr>
              </thead>
              <tbody>
                {overview.recentUsers && overview.recentUsers.length > 0 ? (
                  overview.recentUsers.map((user) => (
                    <tr key={user.id}>
                      <td>
                        <div className="user-cell">
                          <div className="user-avatar">
                            {user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
                          </div>
                          <span style={{ fontWeight: '500' }}>{user.fullName || 'Anonymous'}</span>
                        </div>
                      </td>
                      <td style={{ color: 'var(--text-secondary)' }}>{user.email}</td>
                      <td>
                        <span className={`role-tag ${user.role?.includes('ADMIN') ? 'admin' : 'user'}`}>
                          {user.role?.includes('ADMIN') ? 'ADMIN' : 'USER'}
                        </span>
                      </td>
                      <td style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                        {formatDate(user.createdAt)}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="4" className="empty-cell">
                      {loading ? 'Loading users from database...' : 'No registered users found.'}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Recently Created Trips */}
        <div className="admin-card">
          <div className="admin-card-header">
            <h2>
              <Compass size={18} style={{ color: '#16a34a' }} />
              Recently Created Trips
            </h2>
            <span className="admin-card-count">{overview.recentTrips?.length || 0} trips</span>
          </div>
          <div className="admin-table-container">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Trip Title</th>
                  <th>Destination</th>
                  <th>Creator</th>
                  <th>Duration</th>
                </tr>
              </thead>
              <tbody>
                {overview.recentTrips && overview.recentTrips.length > 0 ? (
                  overview.recentTrips.map((trip) => (
                    <tr key={trip.id}>
                      <td>
                        <span style={{ fontWeight: '600', color: 'var(--text-primary)' }}>
                          {trip.title}
                        </span>
                      </td>
                      <td>
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: 'var(--accent-primary)', fontWeight: '500' }}>
                          <MapPin size={13} />
                          {getDestName(trip.destinationCity)}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', flexDirection: 'column' }}>
                          <span style={{ fontSize: '0.85rem', fontWeight: '500' }}>{trip.userName}</span>
                          <span style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)' }}>{trip.userEmail}</span>
                        </div>
                      </td>
                      <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                        {trip.numDays ? `${trip.numDays} days` : '1 day'}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="4" className="empty-cell">
                      {loading ? 'Loading trips from database...' : 'No trips created yet.'}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Admin Activity Log Section */}
      <div className="admin-card">
        <div className="admin-card-header">
          <h2>
            <Clock size={18} style={{ color: '#d97706' }} />
            Administrator Activity Logs
          </h2>
          <span className="admin-card-count">{overview.recentActivity?.length || 0} recorded logs</span>
        </div>
        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th style={{ width: '180px' }}>Timestamp</th>
                <th>Administrator</th>
                <th>Action</th>
                <th>Details</th>
                <th>IP Address</th>
              </tr>
            </thead>
            <tbody>
              {overview.recentActivity && overview.recentActivity.length > 0 ? (
                overview.recentActivity.map((log) => {
                  let badgeClass = 'action';
                  if (log.action?.includes('LOGIN')) badgeClass = 'login';
                  else if (log.action?.includes('LOGOUT')) badgeClass = 'logout';

                  return (
                    <tr key={log.id}>
                      <td style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
                        {formatDateTime(log.timestamp)}
                      </td>
                      <td style={{ fontWeight: '500' }}>{log.adminEmail}</td>
                      <td>
                        <span className={`action-badge ${badgeClass}`}>
                          {log.action}
                        </span>
                      </td>
                      <td style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                        {log.details || '—'}
                      </td>
                      <td style={{ color: 'var(--text-tertiary)', fontSize: '0.82rem', fontFamily: 'monospace' }}>
                        {log.ipAddress || '—'}
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan="5" className="empty-cell">
                    {loading ? 'Loading activity logs...' : 'No administrator activity recorded yet.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;
