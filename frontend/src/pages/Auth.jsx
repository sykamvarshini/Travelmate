import React, { useState, useEffect } from 'react';
import { Mail, Lock, User, ArrowRight, Shield } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { authService } from '../services/api';
import './Auth.css';

const Auth = () => {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

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

  useEffect(() => {
    const mode = searchParams.get('mode');
    if (mode === 'admin') {
      setIsLogin(true);
      setIsAdmin(true);
    }
  }, [searchParams]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleTabChange = (loginMode) => {
    setIsLogin(loginMode);
    setError('');
    // If switching to registration, force admin mode off because users cannot register as admin
    if (!loginMode) {
      setIsAdmin(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      let response;
      if (isLogin) {
        if (isAdmin) {
          // Dedicated Admin Login Endpoint - strictly verifies ROLE_ADMIN on backend
          response = await authService.adminLogin({
            email: formData.email,
            password: formData.password
          });
        } else {
          // Normal User Login Endpoint
          response = await authService.login({
            email: formData.email,
            password: formData.password
          });
        }
      } else {
        // Normal User Self-Registration - backend guarantees ROLE_USER
        response = await authService.register({
          email: formData.email,
          password: formData.password,
          fullName: formData.fullName,
          age: parseInt(formData.age, 10)
        });
      }

      const { token, role, fullName } = response;
      const userEmail = response.email || formData.email;

      // Store auth session
      const storage = rememberMe ? localStorage : sessionStorage;
      const nonStorage = rememberMe ? sessionStorage : localStorage;

      storage.setItem('token', token);
      storage.setItem('role', role);
      if (fullName) storage.setItem('fullName', fullName);
      if (userEmail) storage.setItem('userEmail', userEmail);

      nonStorage.removeItem('token');
      nonStorage.removeItem('role');
      nonStorage.removeItem('fullName');
      nonStorage.removeItem('userEmail');

      window.dispatchEvent(new Event('authChange'));

      // Redirect administrator to Admin Dashboard, regular users to Dashboard
      if (role === 'ROLE_ADMIN' || role === 'ADMIN') {
        navigate('/admin');
      } else {
        navigate('/dashboard');
      }

    } catch (err) {
      const errMsg = err.response?.data?.message || err.response?.data?.error || 'Authentication failed. Please try again.';
      setError(errMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container animate-fade-in">
      <div className="auth-card">
        <div className="auth-header">
          <h1>{isLogin ? (isAdmin ? 'Admin Portal' : t('auth.welcomeBack')) : t('auth.createAccount')}</h1>
          <p>{isLogin ? (isAdmin ? 'Authorized administrators sign-in' : t('auth.signInSub')) : t('auth.registerSub')}</p>
        </div>

        <div className="auth-tabs">
          <button 
            type="button"
            className={`auth-tab ${isLogin ? 'active' : ''}`}
            onClick={() => handleTabChange(true)}
          >
            {t('auth.signInBtn')}
          </button>
          <button 
            type="button"
            className={`auth-tab ${!isLogin ? 'active' : ''}`}
            onClick={() => handleTabChange(false)}
          >
            {t('auth.registerBtn')}
          </button>
        </div>

        {/* Role Toggle is available ONLY on the Sign-In tab */}
        {isLogin && (
          <div className="role-toggle">
            <button 
              className={`role-btn ${!isAdmin ? 'active' : ''}`}
              onClick={() => { setIsAdmin(false); setError(''); }}
              type="button"
            >
              <User size={14} style={{ display: 'inline', marginRight: '4px' }} /> User Login
            </button>
            <button 
              className={`role-btn ${isAdmin ? 'active' : ''}`}
              onClick={() => { setIsAdmin(true); setError(''); }}
              type="button"
            >
              <Shield size={14} style={{ display: 'inline', marginRight: '4px' }} /> Admin Portal
            </button>
          </div>
        )}

        {isLogin && isAdmin && (
          <div className="admin-portal-banner">
            <Shield size={16} />
            <span>Secure Administrator Authentication</span>
          </div>
        )}

        {error && <div className="error-message">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          {!isLogin && (
            <>
              <div className="form-group">
                <label>{t('auth.fullNameLabel')}</label>
                <div className="input-wrapper">
                  <User size={18} className="input-icon" />
                  <input 
                    type="text" 
                    name="fullName" 
                    placeholder={t('auth.fullNamePlaceholder')}
                    value={formData.fullName} 
                    onChange={handleChange} 
                    required 
                  />
                </div>
              </div>
              <div className="form-group">
                <label>{t('auth.ageLabel')}</label>
                <div className="input-wrapper">
                  <User size={18} className="input-icon" />
                  <input 
                    type="number" 
                    name="age" 
                    placeholder={t('auth.agePlaceholder')}
                    value={formData.age} 
                    onChange={handleChange} 
                    required 
                  />
                </div>
              </div>
            </>
          )}

          <div className="form-group">
            <label>{t('auth.emailLabel')}</label>
            <div className="input-wrapper">
              <Mail size={18} className="input-icon" />
              <input 
                type="email" 
                name="email" 
                placeholder={t('auth.emailPlaceholder')}
                value={formData.email} 
                onChange={handleChange} 
                required 
              />
            </div>
          </div>

          <div className="form-group">
            <label>{t('auth.passwordLabel')}</label>
            <div className="input-wrapper">
              <Lock size={18} className="input-icon" />
              <input 
                type="password" 
                name="password" 
                placeholder={t('auth.passwordPlaceholder')}
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
                {t('auth.rememberMe')}
              </label>
            </div>
          )}

          <button type="submit" className="primary-btn submit-btn" disabled={loading}>
            {loading ? t('common.loading') : (isLogin ? (isAdmin ? 'Admin Sign In' : t('auth.signInBtn')) : t('auth.registerBtn'))}
            {!loading && <ArrowRight size={18} />}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Auth;
