import React, { useState } from 'react';
import { Mail, Lock, User, ArrowRight, Shield } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { authService } from '../services/api';
import './Auth.css';

const Auth = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [isAdmin, setIsAdmin] = useState(false);
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    fullName: '',
    age: ''
  });
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      let response;
      if (isLogin) {
        // We'll pass the intended role (USER or ADMIN) to the login endpoint if our backend supports role-based login validation.
        // Or the backend will return the role in the JWT token/response.
        response = await authService.login({ email: formData.email, password: formData.password });
      } else {
        response = await authService.register({
          email: formData.email,
          password: formData.password,
          fullName: formData.fullName,
          age: parseInt(formData.age),
          role: isAdmin ? 'ADMIN' : 'USER'
        });
      }

      const { token, role, fullName } = response;
      const userEmail = response.email || formData.email;
      
      // Basic role verification on client-side
      if (isLogin && isAdmin && role !== 'ADMIN') {
        setError('Unauthorized: You are not an admin.');
        setLoading(false);
        return;
      }
      if (isLogin && !isAdmin && role === 'ADMIN') {
        // Technically an admin could log in as a user, but we'll enforce the UI toggle
        setError('Please use the Admin Sign In portal.');
        setLoading(false);
        return;
      }

      // If "remember me" is selected, we store in localStorage and clear sessionStorage, else vice versa
      if (rememberMe) {
        localStorage.setItem('token', token);
        localStorage.setItem('role', role);
        if (fullName) localStorage.setItem('fullName', fullName);
        if (userEmail) localStorage.setItem('userEmail', userEmail);
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('role');
        sessionStorage.removeItem('fullName');
        sessionStorage.removeItem('userEmail');
      } else {
        sessionStorage.setItem('token', token);
        sessionStorage.setItem('role', role);
        if (fullName) sessionStorage.setItem('fullName', fullName);
        if (userEmail) sessionStorage.setItem('userEmail', userEmail);
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        localStorage.removeItem('fullName');
        localStorage.removeItem('userEmail');
      }
      
      window.dispatchEvent(new Event('authChange'));
      navigate('/dashboard');

    } catch (err) {
      setError(err.response?.data?.message || 'Authentication failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container animate-fade-in">
      <div className="auth-card">
        <div className="auth-header">
          <h1>{isLogin ? 'Welcome Back' : 'Create Account'}</h1>
          <p>{isLogin ? 'Sign in to access your travel plans' : 'Join TravelMate AI today'}</p>
        </div>

        <div className="auth-tabs">
          <button 
            className={`auth-tab ${isLogin ? 'active' : ''}`}
            onClick={() => setIsLogin(true)}
          >
            Sign In
          </button>
          <button 
            className={`auth-tab ${!isLogin ? 'active' : ''}`}
            onClick={() => setIsLogin(false)}
          >
            Register
          </button>
        </div>

        {/* Role Toggle */}
        <div className="role-toggle">
          <button 
            className={`role-btn ${!isAdmin ? 'active' : ''}`}
            onClick={() => setIsAdmin(false)}
            type="button"
          >
            <User size={14} style={{display: 'inline', marginRight: '4px'}}/> User
          </button>
          <button 
            className={`role-btn ${isAdmin ? 'active' : ''}`}
            onClick={() => setIsAdmin(true)}
            type="button"
          >
            <Shield size={14} style={{display: 'inline', marginRight: '4px'}}/> Admin
          </button>
        </div>

        {error && <div className="error-message">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          {!isLogin && (
            <>
              <div className="form-group">
                <label>Full Name</label>
                <div className="input-wrapper">
                  <User size={18} className="input-icon" />
                  <input 
                    type="text" 
                    name="fullName" 
                    placeholder="John Doe"
                    value={formData.fullName} 
                    onChange={handleChange} 
                    required 
                  />
                </div>
              </div>
              <div className="form-group">
                <label>Age</label>
                <div className="input-wrapper">
                  <User size={18} className="input-icon" />
                  <input 
                    type="number" 
                    name="age" 
                    placeholder="25"
                    value={formData.age} 
                    onChange={handleChange} 
                    required 
                  />
                </div>
              </div>
            </>
          )}

          <div className="form-group">
            <label>Email Address</label>
            <div className="input-wrapper">
              <Mail size={18} className="input-icon" />
              <input 
                type="email" 
                name="email" 
                placeholder="you@example.com"
                value={formData.email} 
                onChange={handleChange} 
                required 
              />
            </div>
          </div>

          <div className="form-group">
            <label>Password</label>
            <div className="input-wrapper">
              <Lock size={18} className="input-icon" />
              <input 
                type="password" 
                name="password" 
                placeholder="••••••••"
                value={formData.password} 
                onChange={handleChange} 
                required 
              />
            </div>
          </div>

          {isLogin && (
            <div className="form-options">
              <label className="remember-me">
                <input 
                  type="checkbox" 
                  checked={rememberMe} 
                  onChange={(e) => setRememberMe(e.target.checked)}
                />
                Remember me
              </label>
              <a href="#" className="forgot-password">Forgot Password?</a>
            </div>
          )}

          <button type="submit" className="primary-btn submit-btn" disabled={loading}>
            {loading ? 'Processing...' : (isLogin ? (isAdmin ? 'Admin Sign In' : 'Sign In') : 'Create Account')}
            {!loading && <ArrowRight size={18} />}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Auth;
