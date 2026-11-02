import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/layout/Navbar';
import Home from './pages/Home';

import PlanTrip from './pages/PlanTrip';
import Dashboard from './pages/Dashboard';
import Auth from './pages/Auth';
import Itinerary from './pages/Itinerary';
import Explore from './pages/Explore';

function App() {
  return (
    <Router>
      <div className="app">
        <Navbar />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/explore" element={<Explore />} />
          <Route path="/auth" element={<Auth />} />
          <Route path="/plan" element={<PlanTrip />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/trip/:id" element={<Itinerary />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
