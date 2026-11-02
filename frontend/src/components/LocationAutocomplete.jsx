import React, { useState, useEffect, useRef } from 'react';
import { MapPin, Search, Loader2 } from 'lucide-react';
import { placesService } from '../services/api';
import './LocationAutocomplete.css';

const LocationAutocomplete = ({
  value,
  onChange,
  onSelect,
  placeholder = "e.g. Kanaka Durga Temple, Araku, Gudivada, Goa",
  className = "",
  autoFocus = false,
  required = false
}) => {
  const [suggestions, setSuggestions] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const containerRef = useRef(null);

  // Debounced search for suggestions
  useEffect(() => {
    if (!value || value.trim().length < 2) {
      setSuggestions([]);
      setIsOpen(false);
      return;
    }

    const timer = setTimeout(async () => {
      setIsLoading(true);
      try {
        const results = await placesService.autocomplete(value);
        setSuggestions(results);
        setIsOpen(results.length > 0);
      } catch (err) {
        console.error("Autocomplete error:", err);
      } finally {
        setIsLoading(false);
      }
    }, 280);

    return () => clearTimeout(timer);
  }, [value]);

  // Click outside listener
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (item) => {
    setIsOpen(false);
    if (onSelect) {
      onSelect(item);
    } else if (onChange) {
      onChange(item.name || item.displayName);
    }
  };

  return (
    <div className={`location-autocomplete-container ${className}`} ref={containerRef}>
      <div className="autocomplete-input-wrap">
        <input
          type="text"
          value={value}
          onChange={(e) => {
            onChange(e.target.value);
            if (!isOpen && e.target.value.length >= 2) setIsOpen(true);
          }}
          onFocus={() => {
            if (suggestions.length > 0) setIsOpen(true);
          }}
          placeholder={placeholder}
          required={required}
          autoFocus={autoFocus}
          autoComplete="off"
        />
        {isLoading && (
          <div className="autocomplete-loader">
            <Loader2 size={16} className="spin-icon" />
          </div>
        )}
      </div>

      {isOpen && suggestions.length > 0 && (
        <ul className="autocomplete-dropdown animate-fade-in">
          {suggestions.map((item, index) => (
            <li
              key={item.placeId || index}
              className="autocomplete-item"
              onClick={() => handleSelect(item)}
            >
              <div className="autocomplete-item-icon">
                <MapPin size={16} />
              </div>
              <div className="autocomplete-item-text">
                <span className="item-name">{item.name}</span>
                <span className="item-secondary">{item.secondary || item.displayName}</span>
              </div>
              {item.type && (
                <span className="item-type-tag">{item.type}</span>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default LocationAutocomplete;
