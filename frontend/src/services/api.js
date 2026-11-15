import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

// Create an axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  }
});

// Interceptor to attach JWT token to requests if available
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor to handle expired or rejected tokens
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // 401 Unauthorized indicates token expiration on authenticated endpoints (excluding login endpoints)
    if (error.response && error.response.status === 401) {
      const isLoginRequest = error.config?.url?.includes('/auth/login') || error.config?.url?.includes('/auth/admin/login');
      const hadToken = localStorage.getItem('token') || sessionStorage.getItem('token');
      if (hadToken && !isLoginRequest) {
        console.warn('Session expired (status 401). Clearing session tokens.');
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        localStorage.removeItem('fullName');
        localStorage.removeItem('userEmail');
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('role');
        sessionStorage.removeItem('fullName');
        sessionStorage.removeItem('userEmail');
        window.dispatchEvent(new Event('authChange'));
      }
    }
    return Promise.reject(error);
  }
);

export const tripService = {
  createTrip: async (tripData) => {
    try {
      const response = await apiClient.post('/trips', tripData);
      return response.data;
    } catch (error) {
      console.error('Error creating trip:', error);
      throw error;
    }
  },
  
  getUserTrips: async () => {
    try {
      const response = await apiClient.get('/trips');
      return response.data;
    } catch (error) {
      console.error('Error fetching user trips:', error);
      throw error;
    }
  },
  
  getTrip: async (tripId) => {
    const response = await apiClient.get(`/trips/${tripId}`);
    return response.data;
  },
  
  adaptDay: async (tripId, dayId, weatherCondition, rainProb, temp) => {
    const response = await apiClient.post(`/trips/${tripId}/adapt`, null, {
      params: {
        dayId,
        weatherCondition,
        rainProb,
        temp
      }
    });
    return response.data;
  },

  deleteTrip: async (tripId) => {
    try {
      const response = await apiClient.delete(`/trips/${tripId}`);
      return response.data;
    } catch (error) {
      console.error('Error deleting trip:', error);
      throw error;
    }
  },

  completeActivity: async (tripId, activityId) => {
    try {
      const response = await apiClient.post(`/trips/${tripId}/activities/${activityId}/complete`);
      return response.data;
    } catch (error) {
      console.error('Error marking activity completed:', error);
      throw error;
    }
  },

  checkNextActivity: async (tripId, activityId, weatherOverride) => {
    try {
      const response = await apiClient.get(`/trips/${tripId}/activities/${activityId}/next-check`, {
        params: weatherOverride ? { weatherOverride } : {}
      });
      return response.data;
    } catch (error) {
      console.error('Error checking next activity:', error);
      throw error;
    }
  },

  getAlternatives: async (tripId, activityId, reason = 'Weather concern') => {
    try {
      const response = await apiClient.get(`/trips/${tripId}/activities/${activityId}/alternatives`, {
        params: { reason }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching alternatives:', error);
      throw error;
    }
  },

  replaceActivity: async (tripId, activityId, replacementData) => {
    try {
      const response = await apiClient.post(`/trips/${tripId}/activities/${activityId}/replace`, replacementData);
      return response.data;
    } catch (error) {
      console.error('Error replacing activity:', error);
      throw error;
    }
  },

  removeActivity: async (tripId, activityId) => {
    try {
      const response = await apiClient.delete(`/trips/${tripId}/activities/${activityId}`);
      return response.data;
    } catch (error) {
      console.error('Error removing activity:', error);
      throw error;
    }
  },

  updateTrip: async (tripId, tripData) => {
    try {
      const response = await apiClient.put(`/trips/${tripId}`, tripData);
      return response.data;
    } catch (error) {
      console.error('Error updating trip:', error);
      throw error;
    }
  },

  inviteMember: async (tripId, email, role = 'VIEWER') => {
    try {
      const response = await apiClient.post(`/trips/${tripId}/invite`, { email, role });
      return response.data;
    } catch (error) {
      console.error('Error inviting member:', error);
      throw error;
    }
  },

  getUserInvitations: async () => {
    try {
      const response = await apiClient.get('/trips/invitations');
      return response.data || [];
    } catch (error) {
      console.error('Error fetching invitations:', error);
      return [];
    }
  },

  respondToInvitation: async (inviteId, action) => {
    try {
      const response = await apiClient.post(`/trips/invitations/${inviteId}/respond`, null, {
        params: { action }
      });
      return response.data;
    } catch (error) {
      console.error('Error responding to invitation:', error);
      throw error;
    }
  },

  removeMember: async (tripId, memberId) => {
    try {
      const response = await apiClient.delete(`/trips/${tripId}/members/${memberId}`);
      return response.data;
    } catch (error) {
      console.error('Error removing member:', error);
      throw error;
    }
  },

  updateMemberRole: async (tripId, memberId, role) => {
    try {
      const response = await apiClient.put(`/trips/${tripId}/members/${memberId}/role`, { role });
      return response.data;
    } catch (error) {
      console.error('Error updating member role:', error);
      throw error;
    }
  },

  addActivity: async (tripId, dayId, activityData) => {
    try {
      const response = await apiClient.post(`/trips/${tripId}/days/${dayId}/activities`, activityData);
      return response.data;
    } catch (error) {
      console.error('Error adding activity to day:', error);
      throw error;
    }
  },

  reorderActivities: async (tripId, dayId, activityIds) => {
    try {
      const response = await apiClient.post(`/trips/${tripId}/days/${dayId}/reorder`, { activityIds });
      return response.data;
    } catch (error) {
      console.error('Error reordering activities:', error);
      throw error;
    }
  },

  updateActivity: async (tripId, activityId, updateData) => {
    try {
      const response = await apiClient.put(`/trips/${tripId}/activities/${activityId}`, updateData);
      return response.data;
    } catch (error) {
      console.error('Error updating activity:', error);
      throw error;
    }
  },

  moveActivityToDay: async (tripId, activityId, targetDayId, targetPosition = null) => {
    try {
      const payload = { targetDayId };
      if (targetPosition != null) {
        payload.targetPosition = targetPosition;
      }
      const response = await apiClient.post(`/trips/${tripId}/activities/${activityId}/move-day`, payload);
      return response.data;
    } catch (error) {
      console.error('Error moving activity to day:', error);
      throw error;
    }
  }
};

export const placesService = {
  autocomplete: async (query) => {
    if (!query || query.trim().length < 2) return [];
    try {
      const response = await apiClient.get('/places/autocomplete', {
        params: { query: query.trim() }
      });
      return response.data || [];
    } catch (error) {
      console.error('Error fetching autocomplete:', error);
      return [];
    }
  },

  search: async (query) => {
    if (!query || !query.trim()) return null;
    try {
      const response = await apiClient.get('/places/search', {
        params: { query: query.trim() }
      });
      return response.data;
    } catch (error) {
      console.error('Error searching place:', error);
      throw error;
    }
  },

  getNearby: async (lat, lon, destination, category, limit = 8) => {
    try {
      const response = await apiClient.get('/places/nearby', {
        params: { lat, lon, destination, category, limit }
      });
      return response.data || [];
    } catch (error) {
      console.error('Error fetching nearby places:', error);
      return [];
    }
  },

  getByCategory: async (destination, category) => {
    try {
      const response = await apiClient.get('/places/categories', {
        params: { destination, category }
      });
      return response.data || [];
    } catch (error) {
      console.error('Error fetching places by category:', error);
      return [];
    }
  },

  getDestinationAttractions: async (destination, query = '', prevLat = null, prevLon = null) => {
    try {
      const params = { destination, query };
      if (prevLat != null && prevLon != null) {
        params.prevLat = prevLat;
        params.prevLon = prevLon;
      }
      const response = await apiClient.get('/places/destination-attractions', { params });
      return response.data || [];
    } catch (error) {
      console.error('Error fetching destination attractions:', error);
      return [];
    }
  }
};

export const authService = {
  login: async (credentials) => {
    const response = await apiClient.post('/auth/login', credentials);
    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
    }
    return response.data;
  },

  adminLogin: async (credentials) => {
    const response = await apiClient.post('/auth/admin/login', credentials);
    return response.data;
  },
  
  register: async (userData) => {
    const response = await apiClient.post('/auth/register', userData);
    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
    }
    return response.data;
  },
  
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('fullName');
    localStorage.removeItem('userEmail');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('role');
    sessionStorage.removeItem('fullName');
    sessionStorage.removeItem('userEmail');
    window.dispatchEvent(new Event('authChange'));
  },

  adminLogout: async () => {
    try {
      await apiClient.post('/auth/admin/logout');
    } catch (e) {
      // Ignore network errors during logout
    }
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('fullName');
    localStorage.removeItem('userEmail');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('role');
    sessionStorage.removeItem('fullName');
    sessionStorage.removeItem('userEmail');
    window.dispatchEvent(new Event('authChange'));
  }
};

export const adminService = {
  getOverview: async () => {
    try {
      const response = await apiClient.get('/admin/overview');
      return response.data;
    } catch (error) {
      console.error('Error fetching admin overview:', error);
      throw error;
    }
  },

  getActivityLogs: async () => {
    try {
      const response = await apiClient.get('/admin/activity-logs');
      return response.data || [];
    } catch (error) {
      console.error('Error fetching admin activity logs:', error);
      throw error;
    }
  },

  logAction: async (action, details) => {
    try {
      const response = await apiClient.post('/admin/log-action', { action, details });
      return response.data;
    } catch (error) {
      console.error('Error logging admin action:', error);
      throw error;
    }
  }
};

export default apiClient;
