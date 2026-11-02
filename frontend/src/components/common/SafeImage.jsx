import React, { useState } from 'react';
import { 
  LOCAL_DESTINATION_IMAGES, 
  DESTINATION_FALLBACKS, 
  CATEGORY_FALLBACKS, 
  getTravelMateSvgPlaceholder 
} from '../../utils/imageFallbacks';

/**
 * SafeImage - Failsafe image component for TravelMate AI
 * 
 * Flow:
 * 1. Primary real image (src)
 * 2. If unavailable -> Destination local / high-speed verified photo
 * 3. If unavailable -> Category-specific real photo fallback
 * 4. Final failsafe -> TravelMate AI branded SVG placeholder
 * 
 * Never displays a broken-image icon or empty area anywhere on the site.
 */
const SafeImage = ({
  src,
  alt = 'Travel Destination',
  category = 'Attraction',
  destinationId,
  destinationName,
  className = '',
  style = {},
  loading = 'lazy',
  onLoad,
  ...props
}) => {
  const destKey = (destinationId || destinationName || alt || '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '-');

  // Match key against available destination aliases
  const getDestinationPhoto = (key) => {
    if (!key) return null;
    if (LOCAL_DESTINATION_IMAGES[key]) return LOCAL_DESTINATION_IMAGES[key];
    const matchedLocal = Object.keys(LOCAL_DESTINATION_IMAGES).find(k => key.includes(k) || k.includes(key));
    if (matchedLocal) return LOCAL_DESTINATION_IMAGES[matchedLocal];

    if (DESTINATION_FALLBACKS[key]) return DESTINATION_FALLBACKS[key];
    const matchedOnline = Object.keys(DESTINATION_FALLBACKS).find(k => key.includes(k) || k.includes(key));
    if (matchedOnline) return DESTINATION_FALLBACKS[matchedOnline];

    return null;
  };

  const primarySrc = (src && !src.includes('thumb.wikimedia.org'))
    ? src
    : (getDestinationPhoto(destKey) || CATEGORY_FALLBACKS[category] || CATEGORY_FALLBACKS.Heritage);

  const [currentSrc, setCurrentSrc] = useState(primarySrc);
  const [fallbackStage, setFallbackStage] = useState(0); // 0: primary, 1: dest fallback, 2: category fallback, 3: svg
  const [isLoaded, setIsLoaded] = useState(false);
  const [hasError, setHasError] = useState(false);

  // Sync state if primary source changes
  const [prevPrimary, setPrevPrimary] = useState(primarySrc);
  if (primarySrc !== prevPrimary) {
    setPrevPrimary(primarySrc);
    setCurrentSrc(primarySrc);
    setFallbackStage(0);
    setIsLoaded(false);
    setHasError(false);
  }

  const triggerFallback = (stage) => {
    if (stage === 0) {
      // Step 1: Try verified local destination photo
      const destPhoto = getDestinationPhoto(destKey);
      if (destPhoto && destPhoto !== currentSrc) {
        setFallbackStage(1);
        setCurrentSrc(destPhoto);
        return;
      }
      // Step 2: Try category real photo
      const catPhoto = CATEGORY_FALLBACKS[category] || CATEGORY_FALLBACKS.Heritage;
      if (catPhoto && catPhoto !== currentSrc) {
        setFallbackStage(2);
        setCurrentSrc(catPhoto);
        return;
      }
      // Step 3: Failsafe SVG
      setFallbackStage(3);
      setCurrentSrc(getTravelMateSvgPlaceholder(destinationName || alt, category));
    } else if (stage === 1) {
      // Step 2: Try category real photo
      const catPhoto = CATEGORY_FALLBACKS[category] || CATEGORY_FALLBACKS.Heritage;
      if (catPhoto && catPhoto !== currentSrc) {
        setFallbackStage(2);
        setCurrentSrc(catPhoto);
        return;
      }
      setFallbackStage(3);
      setCurrentSrc(getTravelMateSvgPlaceholder(destinationName || alt, category));
    } else if (stage === 2) {
      // Step 3: Failsafe SVG
      setFallbackStage(3);
      setCurrentSrc(getTravelMateSvgPlaceholder(destinationName || alt, category));
    } else {
      setHasError(true);
    }
  };

  const handleError = () => {
    triggerFallback(fallbackStage);
  };

  const handleLoad = (e) => {
    setIsLoaded(true);
    setHasError(false);
    if (onLoad) onLoad(e);
  };

  return (
    <div 
      className={`safe-image-container ${isLoaded ? 'is-loaded' : 'is-loading'} ${className}`}
      style={{
        position: 'relative',
        overflow: 'hidden',
        backgroundColor: '#e2e8f0',
        width: '100%',
        height: '100%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        ...style
      }}
    >
      {/* Loading Shimmer Skeleton */}
      {!isLoaded && !hasError && (
        <div 
          className="safe-image-skeleton"
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            background: 'linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%)',
            backgroundSize: '200% 100%',
            animation: 'safeImageShimmer 1.5s infinite',
            zIndex: 1
          }}
        />
      )}

      {/* Actual Image with Failsafe Fallbacks */}
      <img
        src={currentSrc || getTravelMateSvgPlaceholder(destinationName || alt, category)}
        alt={alt}
        loading={loading}
        onLoad={handleLoad}
        onError={handleError}
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          display: 'block',
          opacity: isLoaded ? 1 : 0.8,
          transition: 'opacity 0.25s ease-in-out',
          zIndex: 2,
          position: 'relative'
        }}
        {...props}
      />
    </div>
  );
};

export default SafeImage;
