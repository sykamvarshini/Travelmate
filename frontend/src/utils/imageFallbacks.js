/**
 * TravelMate AI - Multi-Tier Image Fallback System
 * Tier 1: Real destination / landmark image (Local verified high-res photo or high-speed CDN)
 * Tier 2: Category or destination backup image
 * Tier 3: TravelMate AI branded SVG placeholder (Zero-network failsafe)
 */

export const LOCAL_DESTINATION_IMAGES = {
  visakhapatnam: "/images/destinations/visakhapatnam.jpg",
  vizag: "/images/destinations/visakhapatnam.jpg",
  vijayawada: "/images/destinations/vijayawada.jpg",
  "araku-valley": "/images/destinations/araku-valley.jpg",
  araku: "/images/destinations/araku-valley.jpg",
  tirupati: "/images/destinations/tirupati.jpg",
  tirumala: "/images/destinations/tirupati.jpg",
  goa: "/images/destinations/goa.jpg",
  jaipur: "/images/destinations/jaipur.jpg",
  kerala: "/images/destinations/munnar.jpg",
  munnar: "/images/destinations/munnar.jpg",
  manali: "/images/destinations/manali.jpg",
  ooty: "/images/destinations/ooty.jpg",
  udaipur: "/images/destinations/udaipur.jpg",
  varanasi: "/images/destinations/varanasi.jpg",
  hampi: "/images/destinations/hampi.jpg",
  hyderabad: "/images/destinations/hyderabad.jpg",
  madurai: "/images/destinations/madurai.jpg",
  rishikesh: "/images/destinations/rishikesh.jpg"
};

export const DESTINATION_FALLBACKS = {
  visakhapatnam: "https://images.unsplash.com/photo-1609854534028-b512f5246abc?w=1080&auto=format&fit=crop&q=80",
  vizag: "https://images.unsplash.com/photo-1609854534028-b512f5246abc?w=1080&auto=format&fit=crop&q=80",
  vijayawada: "/images/destinations/vijayawada.jpg",
  "araku-valley": "/images/destinations/araku-valley.jpg",
  araku: "/images/destinations/araku-valley.jpg",
  tirupati: "/images/destinations/tirupati.jpg",
  tirumala: "/images/destinations/tirupati.jpg",
  goa: "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=1080&auto=format&fit=crop&q=80",
  jaipur: "https://images.unsplash.com/photo-1599661046289-e31897846e41?w=1080&auto=format&fit=crop&q=80",
  kerala: "https://images.unsplash.com/photo-1596176530529-78163a4f7af2?w=1080&auto=format&fit=crop&q=80",
  munnar: "https://images.unsplash.com/photo-1596176530529-78163a4f7af2?w=1080&auto=format&fit=crop&q=80",
  manali: "https://images.unsplash.com/photo-1626621341517-bbf3d9990a23?w=1080&auto=format&fit=crop&q=80",
  ooty: "https://images.unsplash.com/photo-1589182373726-e4f658ab50f0?w=1080&auto=format&fit=crop&q=80",
  udaipur: "https://images.unsplash.com/photo-1615836245337-f5b9b2303f10?w=1080&auto=format&fit=crop&q=80",
  varanasi: "https://images.unsplash.com/photo-1561361513-2d000a50f0dc?w=1080&auto=format&fit=crop&q=80",
  hampi: "/images/destinations/hampi.jpg",
  hyderabad: "https://images.unsplash.com/photo-1743884092589-cbfe3689ebcf?w=1080&auto=format&fit=crop&q=80",
  madurai: "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?w=1080&auto=format&fit=crop&q=80",
  rishikesh: "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=1080&auto=format&fit=crop&q=80"
};

export const CATEGORY_FALLBACKS = {
  Spiritual: "/images/categories/Spiritual.jpg",
  Heritage: "/images/categories/Heritage.jpg",
  Beaches: "/images/categories/Beaches.jpg",
  Mountains: "/images/categories/Mountains.jpg",
  Nature: "/images/categories/Nature.jpg",
  Wildlife: "/images/categories/Wildlife.jpg",
  Adventure: "/images/categories/Adventure.jpg",
  Food: "/images/categories/Food.jpg",
  Museums: "/images/categories/Museums.jpg",
  Relaxation: "/images/categories/Relaxation.jpg"
};

/**
 * Generates an SVG Data URI that functions as an offline, zero-network fallback
 */
export function getTravelMateSvgPlaceholder(title = "Travel Destination", category = "TravelMate AI") {
  const safeTitle = (title || "Travel Destination").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
  const safeCat = (category || "Verified Place").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

  let gradStart = "#1e293b";
  let gradEnd = "#0f172a";
  let accent = "#38bdf8";

  const lowerCat = safeCat.toLowerCase();
  if (lowerCat.includes("spirit") || lowerCat.includes("temple")) {
    gradStart = "#78350f";
    gradEnd = "#451a03";
    accent = "#fbbf24";
  } else if (lowerCat.includes("beach") || lowerCat.includes("coast")) {
    gradStart = "#0369a1";
    gradEnd = "#082f49";
    accent = "#38bdf8";
  } else if (lowerCat.includes("mountain") || lowerCat.includes("nature") || lowerCat.includes("valley")) {
    gradStart = "#065f46";
    gradEnd = "#022c22";
    accent = "#34d399";
  } else if (lowerCat.includes("heritage") || lowerCat.includes("fort")) {
    gradStart = "#831843";
    gradEnd = "#500724";
    accent = "#f472b6";
  }

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500" fill="none">
    <defs>
      <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="${gradStart}"/>
        <stop offset="100%" stop-color="${gradEnd}"/>
      </linearGradient>
      <linearGradient id="overlay" x1="0%" y1="0%" x2="0%" y2="100%">
        <stop offset="60%" stop-color="transparent"/>
        <stop offset="100%" stop-color="rgba(0,0,0,0.6)"/>
      </linearGradient>
    </defs>
    <rect width="800" height="500" fill="url(#bgGrad)"/>
    <circle cx="400" cy="220" r="140" stroke="rgba(255,255,255,0.06)" stroke-width="2" stroke-dasharray="6 6"/>
    <circle cx="400" cy="220" r="90" stroke="rgba(255,255,255,0.08)" stroke-width="1.5"/>
    <circle cx="400" cy="220" r="4" fill="${accent}"/>
    <g transform="translate(376, 196)">
      <path d="M24 4L28 16L40 20L28 24L24 36L20 24L8 20L20 16L24 4Z" fill="${accent}" opacity="0.9"/>
    </g>
    <rect width="800" height="500" fill="url(#overlay)"/>
    <rect x="40" y="36" width="160" height="32" rx="16" fill="rgba(255,255,255,0.12)"/>
    <text x="56" y="57" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif" font-size="12" font-weight="600" fill="#ffffff" letter-spacing="1">✈ TRAVELMATE AI</text>
    <text x="40" y="415" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif" font-size="14" font-weight="600" fill="${accent}" letter-spacing="0.5">${safeCat.toUpperCase()}</text>
    <text x="40" y="452" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif" font-size="28" font-weight="700" fill="#ffffff">${safeTitle}</text>
  </svg>`;

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}
