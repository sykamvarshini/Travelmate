import React, { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Globe, Check, ChevronDown } from 'lucide-react';
import './LanguageSelector.css';

const LANGUAGES = [
  { code: 'en', label: 'English', nativeName: 'English' },
  { code: 'hi', label: 'Hindi', nativeName: 'हिन्दी' },
  { code: 'te', label: 'Telugu', nativeName: 'తెలుగు' },
  { code: 'ta', label: 'Tamil', nativeName: 'தமிழ்' },
  { code: 'kn', label: 'Kannada', nativeName: 'ಕನ್ನಡ' }
];

const LanguageSelector = () => {
  const { i18n } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  const currentLang = LANGUAGES.find(l => l.code === i18n.language) || LANGUAGES[0];

  const handleSelectLanguage = (code) => {
    i18n.changeLanguage(code);
    localStorage.setItem('travelmate_lang', code);
    setIsOpen(false);
  };

  // Close dropdown on click outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="language-selector-wrapper" ref={dropdownRef}>
      <button 
        type="button"
        className="lang-select-btn hover-lift"
        onClick={() => setIsOpen(!isOpen)}
        aria-label="Select Language"
        aria-expanded={isOpen}
      >
        <Globe size={17} className="lang-icon" />
        <span className="lang-active-label">{currentLang.nativeName}</span>
        <ChevronDown size={14} className={`lang-chevron ${isOpen ? 'open' : ''}`} />
      </button>

      {isOpen && (
        <div className="lang-dropdown-menu animate-fade-in">
          {LANGUAGES.map((lang) => {
            const isSelected = i18n.language === lang.code;
            return (
              <button
                key={lang.code}
                type="button"
                className={`lang-option-btn ${isSelected ? 'active' : ''}`}
                onClick={() => handleSelectLanguage(lang.code)}
              >
                <div className="lang-names">
                  <span className="lang-native">{lang.nativeName}</span>
                  <span className="lang-sub">{lang.label}</span>
                </div>
                {isSelected && <Check size={16} className="lang-check" />}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default LanguageSelector;
