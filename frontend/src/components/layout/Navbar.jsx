import React from 'react';
import { Link } from 'react-router-dom';
import { PlaneTakeoff, User } from 'lucide-react';
import './Navbar.css';

const Navbar = () => {
  const [userName, setUserName] = React.useState(null);

  const checkAuth = () => {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const name = localStorage.getItem('fullName') || sessionStorage.getItem('fullName');
    if (token) {
      setUserName(name || 'User'); // Fallback to 'User' if name isn't stored
    } else {
      setUserName(null);
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
          <Link to="/" className="nav-link">Home</Link>
          <Link to="/plan" className="nav-link">Plan a Trip</Link>
          {userName && <Link to="/dashboard" className="nav-link">My Trips</Link>}
        </div>
        
        <div className="navbar-actions">
          {userName ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <span style={{ fontWeight: '500', color: 'var(--text-primary)' }}>Hi, {userName.split(' ')[0]}</span>
              <button onClick={handleLogout} className="login-btn hover-lift" style={{ background: 'transparent', border: '1px solid var(--accent-primary)', color: 'var(--accent-primary)' }}>
                <span>Logout</span>
              </button>
            </div>
          ) : (
            <Link to="/auth" className="login-btn hover-lift">
              <User size={18} />
              <span>Sign In</span>
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
