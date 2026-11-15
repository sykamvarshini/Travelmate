import React from 'react';
import { Link } from 'react-router-dom';
import { PlaneTakeoff, User, Shield } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import LanguageSelector from './LanguageSelector';
import './Navbar.css';

const Navbar = () => {
  const { t } = useTranslation();
  const [userName, setUserName] = React.useState(null);
  const [userRole, setUserRole] = React.useState(null);

  const checkAuth = () => {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const name = localStorage.getItem('fullName') || sessionStorage.getItem('fullName');
    const role = localStorage.getItem('role') || sessionStorage.getItem('role');
    if (token) {
      setUserName(name || 'User'); // Fallback to 'User' if name isn't stored
      setUserRole(role || 'ROLE_USER');
    } else {
      setUserName(null);
      setUserRole(null);
    }
  };

  React.useEffect(() => {
    checkAuth();
    window.addEventListener('authChange', checkAuth);
    return () => window.removeEventListener('authChange', checkAuth);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('fullName');
    localStorage.removeItem('userEmail');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('role');
    sessionStorage.removeItem('fullName');
    sessionStorage.removeItem('userEmail');
    setUserName(null);
    setUserRole(null);
    window.dispatchEvent(new Event('authChange'));
    window.location.href = '/';
  };

  return (
    <nav className="navbar glass-panel">
      <div className="navbar-container">
        <Link to="/" className="navbar-logo hover-lift">
          <PlaneTakeoff size={28} className="logo-icon" color="var(--accent-primary)" />
          <span className="logo-text text-gradient">TravelMate AI</span>
        </Link>
        
        <div className="navbar-links">
          <Link to="/" className="nav-link">{t('nav.home')}</Link>
          <Link to="/plan" className="nav-link">{t('nav.planTrip')}</Link>
          {userName && <Link to="/dashboard" className="nav-link">{t('nav.myTrips')}</Link>}
          {(userRole === 'ROLE_ADMIN' || userRole === 'ADMIN') && (
            <Link to="/admin" className="nav-link admin-nav-link" style={{ display: 'inline-flex', alignItems: 'center', gap: '5px', color: 'var(--accent-primary)', fontWeight: '600' }}>
              <Shield size={16} /> Admin
            </Link>
          )}
        </div>
        
        <div className="navbar-actions">
          <LanguageSelector />
          {userName ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <span style={{ fontWeight: '500', color: 'var(--text-primary)' }}>{t('nav.hiUser', { name: userName.split(' ')[0] })}</span>
              <button onClick={handleLogout} className="login-btn hover-lift" style={{ background: 'transparent', border: '1px solid var(--accent-primary)', color: 'var(--accent-primary)' }}>
                <span>{t('nav.logout')}</span>
              </button>
            </div>
          ) : (
            <Link to="/auth" className="login-btn hover-lift">
              <User size={18} />
              <span>{t('nav.signIn')}</span>
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
