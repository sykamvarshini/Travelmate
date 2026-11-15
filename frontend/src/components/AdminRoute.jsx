import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';

const AdminRoute = ({ children }) => {
  const location = useLocation();
  const token = localStorage.getItem('token') || sessionStorage.getItem('token');
  const role = localStorage.getItem('role') || sessionStorage.getItem('role');

  const isAdmin = token && (role === 'ROLE_ADMIN' || role === 'ADMIN');

  if (!isAdmin) {
    return <Navigate to="/auth?mode=admin" state={{ from: location }} replace />;
  }

  return children;
};

export default AdminRoute;
