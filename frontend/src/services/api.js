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
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      const hadToken = localStorage.getItem('token') || sessionStorage.getItem('token');
      if (hadToken) {
        console.warn('Session expired or unauthorized (status ' + error.response.status + '). Clearing session tokens.');
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
  }
};

export default apiClient;
