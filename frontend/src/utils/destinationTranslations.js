import { useTranslation } from 'react-i18next';

/**
 * Centralized Destination & State Multilingual Translation System for TravelMate AI.
 * 
 * Supported Languages:
 * - en: English (Default)
 * - hi: Hindi (हिन्दी)
 * - te: Telugu (తెలుగు)
 * - ta: Tamil (தமிழ்)
 * - kn: Kannada (ಕನ್ನಡ)
 */

export const DESTINATION_TRANSLATIONS = {
  "visakhapatnam": {
    en: "Visakhapatnam",
    hi: "विशाखापट्टनम",
    te: "విశాఖపట్నం",
    ta: "விசாகப்பட்டினம்",
    kn: "ವಿಶಾಖಪಟ್ಟಣ"
  },
  "vijayawada": {
    en: "Vijayawada",
    hi: "विजयवाड़ा",
    te: "విజయవాడ",
    ta: "விஜயவாடா",
    kn: "ವಿಜಯವಾಡ"
  },
  "araku valley": {
    en: "Araku Valley",
    hi: "अराकू घाटी",
    te: "అరకు లోయ",
    ta: "அரக்கு பள்ளத்தாக்கு",
    kn: "ಅರಕು ಕಣಿವೆ"
  },
  "tirupati": {
    en: "Tirupati",
    hi: "तिरुपति",
    te: "తిరుపతి",
    ta: "திருப்பதி",
    kn: "ತಿರುಪತಿ"
  },
  "goa": {
    en: "Goa",
    hi: "गोवा",
    te: "గోవా",
    ta: "கோவா",
    kn: "ಗೋವಾ"
  },
  "jaipur": {
    en: "Jaipur",
    hi: "जयपुर",
    te: "జైపూర్",
    ta: "ஜெய்ப்பூர்",
    kn: "ಜೈಪುರ"
  },
  "munnar": {
    en: "Munnar",
    hi: "मुन्नार",
    te: "మూన్నార్",
    ta: "மூணார்",
    kn: "ಮೂನ್ನಾರ್"
  },
  "manali": {
    en: "Manali",
    hi: "मनाली",
    te: "మనాలి",
    ta: "மணாலி",
    kn: "ಮನಾಲಿ"
  },
  "ooty": {
    en: "Ooty",
    hi: "ऊटी",
    te: "ఊటీ",
    ta: "ஊட்டி",
    kn: "ಊಟಿ"
  },
  "udaipur": {
    en: "Udaipur",
    hi: "उदयपुर",
    te: "ఉదయ్‌పూర్",
    ta: "உதய்பூர்",
    kn: "ಉದಯಪುರ"
  },
  "varanasi": {
    en: "Varanasi",
    hi: "वाराणसी",
    te: "వారణాసి",
    ta: "வாரணாசி",
    kn: "ವಾರಣಾಸಿ"
  },
  "hampi": {
    en: "Hampi",
    hi: "हम्पी",
    te: "హంపి",
    ta: "ஹம்பி",
    kn: "ಹಂಪಿ"
  },
  "hyderabad": {
    en: "Hyderabad",
    hi: "हैदराबाद",
    te: "హైదరాబాద్",
    ta: "ஹைதராபாத்",
    kn: "ಹೈದರಾಬಾದ್"
  },
  "madurai": {
    en: "Madurai",
    hi: "मदुरै",
    te: "మదురై",
    ta: "மதுரை",
    kn: "ಮಧುರೈ"
  },
  "rishikesh": {
    en: "Rishikesh",
    hi: "ऋषिकेश",
    te: "రిషికేశ్",
    ta: "ரிஷிகேஷ்",
    kn: "ಋಷಿಕೇಶ"
  },
  "gudivada": {
    en: "Gudivada",
    hi: "गुड़ीवाड़ा",
    te: "గుడివాడ",
    ta: "குடிவாடா",
    kn: "ಗುಡಿವಾಡ"
  },
  "bengaluru": {
    en: "Bengaluru",
    hi: "बेंगलुरु",
    te: "బెంగళూరు",
    ta: "பெங்களூரு",
    kn: "ಬೆಂಗಳೂರು"
  },
  "chennai": {
    en: "Chennai",
    hi: "चेन्नई",
    te: "చెన్నై",
    ta: "சென்னை",
    kn: "ಚೆನ್ನೈ"
  },
  "delhi": {
    en: "Delhi",
    hi: "दिल्ली",
    te: "ఢిల్లీ",
    ta: "டெல்லி",
    kn: "ದೆಹಲಿ"
  },
  "new delhi": {
    en: "New Delhi",
    hi: "नई दिल्ली",
    te: "న్యూ ఢిల్లీ",
    ta: "புது தில்லி",
    kn: "ಹೊಸ ದೆಹಲಿ"
  },
  "mumbai": {
    en: "Mumbai",
    hi: "मुंबई",
    te: "ముంబై",
    ta: "மும்பை",
    kn: "ಮುಂಬೈ"
  },
  "kolkata": {
    en: "Kolkata",
    hi: "कोलकाता",
    te: "కోల్‌కతా",
    ta: "கொல்கத்தா",
    kn: "ಕೋಲ್ಕತ್ತಾ"
  },
  "agra": {
    en: "Agra",
    hi: "आगरा",
    te: "ఆగ్రా",
    ta: "ஆக்ரா",
    kn: "ಆಗ್ರಾ"
  },
  "amritsar": {
    en: "Amritsar",
    hi: "अमृतसर",
    te: "అమృత్‌సర్",
    ta: "அமிர்தசரஸ்",
    kn: "ಅಮೃತಸರ"
  },
  "kochi": {
    en: "Kochi",
    hi: "कोच्चि",
    te: "కొచ్చి",
    ta: "கொச்சி",
    kn: "ಕೊಚ್ಚಿ"
  },
  "pondicherry": {
    en: "Pondicherry",
    hi: "पांडिचेरी",
    te: "పాండిచ్చేరి",
    ta: "பாண்டிச்சேரி",
    kn: "ಪಾಂಡಿಚೇರಿ"
  },
  "puducherry": {
    en: "Puducherry",
    hi: "पुदुचेरी",
    te: "పుదుచ్చేరి",
    ta: "புதுச்சேரி",
    kn: "ಪುದುಚೇರಿ"
  },
  "shimla": {
    en: "Shimla",
    hi: "शिमला",
    te: "సిమ్లా",
    ta: "சிம்லா",
    kn: "ಶಿಮ್ಲಾ"
  },
  "darjeeling": {
    en: "Darjeeling",
    hi: "दार्जिलिंग",
    te: "డార్జిలింగ్",
    ta: "டார்ஜிலிங்",
    kn: "ಡಾರ್ಜಿಲಿಂಗ್"
  },
  "mysore": {
    en: "Mysore",
    hi: "मैसूर",
    te: "మైసూర్",
    ta: "மைசூர்",
    kn: "ಮೈಸೂರು"
  },
  "alleppey": {
    en: "Alleppey",
    hi: "अल्लेप्पी",
    te: "అలప్పుళ",
    ta: "ஆலப்புழா",
    kn: "ಆಲಪ್ಪುಳ"
  },
  "alappuzha": {
    en: "Alappuzha",
    hi: "अलप्पुझा",
    te: "అలప్పుళ",
    ta: "ஆலப்புழா",
    kn: "ಆಲಪ್ಪುಳ"
  },
  "kanyakumari": {
    en: "Kanyakumari",
    hi: "कन्याकुमारी",
    te: "కన్యాకుమారి",
    ta: "கன்னியாகுமரி",
    kn: "ಕನ್ಯಾಕುಮಾರಿ"
  },
  "gokarna": {
    en: "Gokarna",
    hi: "गोकर्ण",
    te: "గోకర్ణ",
    ta: "கோகர்ணா",
    kn: "ಗೋಕರ್ಣ"
  },
  "coorg": {
    en: "Coorg",
    hi: "कूर्ग",
    te: "కూర్గ్",
    ta: "கூர்க்",
    kn: "ಕೂರ್ಗ್"
  },
  "chikmagalur": {
    en: "Chikmagalur",
    hi: "चिकमगलूर",
    te: "చిక్కమగళూరు",
    ta: "சிக்மகளூர்",
    kn: "ಚಿಕ್ಕಮಗಳೂರು"
  },
  "pushkar": {
    en: "Pushkar",
    hi: "पुष्कर",
    te: "పుష్కర్",
    ta: "புஷ்கர்",
    kn: "ಪುಷ್ಕರ್"
  },
  "jodhpur": {
    en: "Jodhpur",
    hi: "जोधपुर",
    te: "జోధ్‌పూర్",
    ta: "ஜோத்பூர்",
    kn: "ಜೋಧಪುರ"
  },
  "jaisalmer": {
    en: "Jaisalmer",
    hi: "जैसलमेर",
    te: "జైసల్మేర్",
    ta: "ஜெய்சல்மேர்",
    kn: "ಜೈಸಲ್ಮೇರ್"
  },
  "leh": {
    en: "Leh",
    hi: "लेह",
    te: "లేహ్",
    ta: "லே",
    kn: "ಲೇಹ್"
  },
  "srinagar": {
    en: "Srinagar",
    hi: "श्रीनगर",
    te: "శ్రీనగర్",
    ta: "ஸ்ரீநகர்",
    kn: "ಶ್ರೀನಗರ"
  },
  "haridwar": {
    en: "Haridwar",
    hi: "हरिद्वार",
    te: "హరిద్వార్",
    ta: "ஹரித்வார்",
    kn: "ಹರಿದ್ವಾರ"
  },
  "shillong": {
    en: "Shillong",
    hi: "शिलांग",
    te: "షిల్లాంగ్",
    ta: "ஷில்லாங்",
    kn: "ಶಿಲ್ಲಾಂಗ್"
  },
  "gangtok": {
    en: "Gangtok",
    hi: "गंगटोक",
    te: "గాంగ్‌టక్",
    ta: "காங்டாக்",
    kn: "ಗ್ಯಾಂಗ್ಟಾಕ್"
  },
  "bhubaneswar": {
    en: "Bhubaneswar",
    hi: "भुवनेश्वर",
    te: "భువనేశ్వర్",
    ta: "புவனேஸ்வர்",
    kn: "ಭುವನೇಶ್ವರ"
  },
  "puri": {
    en: "Puri",
    hi: "पुरी",
    te: "పూరి",
    ta: "பூரி",
    kn: "ಪುರಿ"
  },
  "ayodhya": {
    en: "Ayodhya",
    hi: "अयोध्या",
    te: "అయోధ్య",
    ta: "அயோத்தி",
    kn: "ಅಯೋಧ್ಯೆ"
  },
  "mathura": {
    en: "Mathura",
    hi: "मथुरा",
    te: "మథుర",
    ta: "மதுரா",
    kn: "ಮಥುರಾ"
  },
  "rameswaram": {
    en: "Rameswaram",
    hi: "रामेश्वरम",
    te: "రామేశ్వరం",
    ta: "ராமேஸ்வரம்",
    kn: "ರಾಮೇಶ್ವರಂ"
  },
  "srikalahasti": {
    en: "Srikalahasti",
    hi: "श्रीकालहस्ती",
    te: "శ్రీకాళహస్తి",
    ta: "ஸ்ரீகாளஹஸ்தி",
    kn: "ಶ್ರೀಕಾಳಹಸ್ತಿ"
  },
  "kakinada": {
    en: "Kakinada",
    hi: "काकीनाडा",
    te: "కాకినాడ",
    ta: "காக்கிநாடா",
    kn: "ಕಾಕಿನಾಡ"
  },
  "rajahmundry": {
    en: "Rajahmundry",
    hi: "राजमुंदरी",
    te: "రాజమండ్రి",
    ta: "ராஜமுந்திரி",
    kn: "ರಾಜಮಂಡ್ರಿ"
  },
  "guntur": {
    en: "Guntur",
    hi: "गुंटूर",
    te: "గుంటూరు",
    ta: "குண்டூர்",
    kn: "ಗುಂಟೂರು"
  },
  "nellore": {
    en: "Nellore",
    hi: "नेल्लोर",
    te: "నెల్లూరు",
    ta: "நெல்லூர்",
    kn: "ನೆಲ್ಲೂರು"
  },
  "kurnool": {
    en: "Kurnool",
    hi: "कुरनूल",
    te: "కర్నూలు",
    ta: "கர்னூல்",
    kn: "ಕರ್ನೂಲು"
  },
  "anantapur": {
    en: "Anantapur",
    hi: "अनंतपुर",
    te: "అనంతపురం",
    ta: "அனந்தபூர்",
    kn: "ಅನಂತಪುರ"
  },
  "kadapa": {
    en: "Kadapa",
    hi: "कडपा",
    te: "కడప",
    ta: "கடப்பா",
    kn: "ಕಡಪ"
  },
  "mahabalipuram": {
    en: "Mahabalipuram",
    hi: "महाबलीपुरम",
    te: "మహాబలిపురం",
    ta: "மகாபலிபுரம்",
    kn: "ಮಹಾಬಲಿಪುರಂ"
  },
  "thanjavur": {
    en: "Thanjavur",
    hi: "तंजावुर",
    te: "తంజావూరు",
    ta: "தஞ்சாவூர்",
    kn: "ತಂಜಾವೂರು"
  },
  "kanchipuram": {
    en: "Kanchipuram",
    hi: "कांचीपुरम",
    te: "కాంచీపురం",
    ta: "காஞ்சிபுரம்",
    kn: "ಕಾಂಚೀಪುರಂ"
  }
};

/**
 * Common Aliases mapping to standard normalized destination keys.
 */
export const DESTINATION_ALIASES = {
  "vizag": "visakhapatnam",
  "visakha": "visakhapatnam",
  "araku": "araku valley",
  "araku-valley": "araku valley",
  "kerala": "munnar",
  "bangalore": "bengaluru",
  "bengaluru urban": "bengaluru",
  "mysuru": "mysore",
  "cochin": "kochi",
  "madras": "chennai",
  "bombay": "mumbai",
  "calcutta": "kolkata",
  "benares": "varanasi",
  "kashi": "varanasi"
};

/**
 * State Multilingual Translations
 */
export const STATE_TRANSLATIONS = {
  "andhra pradesh": {
    en: "Andhra Pradesh",
    hi: "आंध्र प्रदेश",
    te: "ఆంధ్రప్రదేశ్",
    ta: "ஆந்திரப் பிரதேசம்",
    kn: "ಆಂಧ್ರ ಪ್ರದೇಶ"
  },
  "telangana": {
    en: "Telangana",
    hi: "तेलंगाना",
    te: "తెలంగాణ",
    ta: "தெலங்கானா",
    kn: "ತೆಲಂಗಾಣ"
  },
  "tamil nadu": {
    en: "Tamil Nadu",
    hi: "तमिलनाडु",
    te: "తమిళనాడు",
    ta: "தமிழ்நாடு",
    kn: "ತಮಿಳುನಾಡು"
  },
  "karnataka": {
    en: "Karnataka",
    hi: "कर्नाटक",
    te: "కర్ణాటక",
    ta: "கர்நாடகா",
    kn: "ಕರ್ನಾಟಕ"
  },
  "kerala": {
    en: "Kerala",
    hi: "केरल",
    te: "కేరళ",
    ta: "கேரளா",
    kn: "ಕೇರಳ"
  },
  "goa": {
    en: "Goa",
    hi: "गोवा",
    te: "గోవా",
    ta: "கோவா",
    kn: "ಗೋವಾ"
  },
  "rajasthan": {
    en: "Rajasthan",
    hi: "राजस्थान",
    te: "రాజస్థాన్",
    ta: "ராஜஸ்தான்",
    kn: "ರಾಜಸ್ಥಾನ"
  },
  "himachal pradesh": {
    en: "Himachal Pradesh",
    hi: "हिमाचल प्रदेश",
    te: "హిమాచల్ ప్రదేశ్",
    ta: "இமாச்சலப் பிரதேசம்",
    kn: "ಹಿಮಾಚಲ ಪ್ರದೇಶ"
  },
  "uttar pradesh": {
    en: "Uttar Pradesh",
    hi: "उत्तर प्रदेश",
    te: "ఉత్తరప్రదేశ్",
    ta: "உத்தரப் பிரதேசம்",
    kn: "ಉತ್ತರ ಪ್ರದೇಶ"
  },
  "uttarakhand": {
    en: "Uttarakhand",
    hi: "उत्तराखंड",
    te: "ఉత్తరాఖండ్",
    ta: "உத்தராகண்ட்",
    kn: "ಉತ್ತರಾಖಂಡ"
  },
  "maharashtra": {
    en: "Maharashtra",
    hi: "महाराष्ट्र",
    te: "మహారాష్ట్ర",
    ta: "மகாராஷ்டிரா",
    kn: "ಮಹಾರಾಷ್ಟ್ರ"
  },
  "west bengal": {
    en: "West Bengal",
    hi: "पश्चिम बंगाल",
    te: "పశ్చిమ బెంగాల్",
    ta: "மேற்கு வங்காளம்",
    kn: "ಪಶ್ಚಿಮ ಬಂಗಾಳ"
  },
  "punjab": {
    en: "Punjab",
    hi: "पंजाब",
    te: "పంజాబ్",
    ta: "பஞ்சாப்",
    kn: "ಪಂಜಾಬ್"
  },
  "odisha": {
    en: "Odisha",
    hi: "ओडिशा",
    te: "ఒడిశా",
    ta: "ஒடிசா",
    kn: "ಒಡಿಶಾ"
  },
  "gujarat": {
    en: "Gujarat",
    hi: "गुजरात",
    te: "గుజరాత్",
    ta: "குஜராத்",
    kn: "ಗುಜರಾತ್"
  },
  "sikkim": {
    en: "Sikkim",
    hi: "सिक्किम",
    te: "సిక్కిం",
    ta: "சிக்கிம்",
    kn: "ಸಿಕ್ಕಿಂ"
  },
  "meghalaya": {
    en: "Meghalaya",
    hi: "मेघालय",
    te: "మేఘాలయ",
    ta: "மேகாலயா",
    kn: "ಮೇಘಾಲಯ"
  },
  "madhya pradesh": {
    en: "Madhya Pradesh",
    hi: "मध्य प्रदेश",
    te: "మధ్యప్రదేశ్",
    ta: "மத்தியப் பிரதேசம்",
    kn: "ಮಧ್ಯ ಪ್ರದೇಶ"
  }
};

/**
 * Normalizes destination or state string for robust lookups.
 */
function normalizeKey(str) {
  if (!str || typeof str !== 'string') return '';
  return str
    .toLowerCase()
    .trim()
    .replace(/[,\-_\.]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

/**
 * Gets the localized display name for a given destination.
 * If no translation exists or language is 'en', returns the original name.
 * 
 * @param {string} rawName - Destination name (e.g., "Vijayawada", "araku-valley")
 * @param {string} lang - Language code ('en', 'hi', 'te', 'ta', 'kn')
 * @returns {string} - Localized name
 */
export function getDestinationName(rawName, lang = 'en') {
  if (!rawName || typeof rawName !== 'string') return rawName || '';
  if (!lang || lang === 'en') return rawName;

  const normalized = normalizeKey(rawName);
  const canonicalKey = DESTINATION_ALIASES[normalized] || normalized;

  // Direct match
  if (DESTINATION_TRANSLATIONS[canonicalKey] && DESTINATION_TRANSLATIONS[canonicalKey][lang]) {
    return DESTINATION_TRANSLATIONS[canonicalKey][lang];
  }

  // Partial substring match (e.g. "Visakhapatnam, Andhra Pradesh" -> "Visakhapatnam")
  for (const [key, mapping] of Object.entries(DESTINATION_TRANSLATIONS)) {
    if (normalized.includes(key) && mapping[lang]) {
      return mapping[lang];
    }
  }

  return rawName;
}

/**
 * Gets the localized display name for an Indian state.
 * 
 * @param {string} rawState - State name (e.g., "Andhra Pradesh")
 * @param {string} lang - Language code
 * @returns {string} - Localized state name
 */
export function getDestinationState(rawState, lang = 'en') {
  if (!rawState || typeof rawState !== 'string') return rawState || '';
  if (!lang || lang === 'en') return rawState;

  const normalized = normalizeKey(rawState);
  if (STATE_TRANSLATIONS[normalized] && STATE_TRANSLATIONS[normalized][lang]) {
    return STATE_TRANSLATIONS[normalized][lang];
  }

  return rawState;
}

/**
 * Formats trip titles like "Trip to Visakhapatnam" into localized strings like:
 * - Telugu: "విశాఖపట్నం యాత్ర"
 * - Hindi: "विशाखापट्टनम यात्रा"
 * - Tamil: "விசாகப்பட்டினம் பயணம்"
 * - Kannada: "ವಿಶಾಖಪಟ್ಟಣ ಪ್ರವಾಸ"
 * - English: "Trip to Visakhapatnam"
 */
export function formatTripTitle(title, destinationCity, lang = 'en') {
  const city = destinationCity || '';
  const localizedCity = getDestinationName(city, lang);

  if (!lang || lang === 'en') {
    return title || `Trip to ${city}`;
  }

  // If title was auto-generated "Trip to [City]"
  const isDefaultTitle = !title || title.toLowerCase().startsWith('trip to') || title.toLowerCase() === `trip to ${city.toLowerCase()}`;

  if (isDefaultTitle) {
    switch (lang) {
      case 'te':
        return `${localizedCity} యాత్ర`;
      case 'hi':
        return `${localizedCity} यात्रा`;
      case 'ta':
        return `${localizedCity} பயணம்`;
      case 'kn':
        return `${localizedCity} ಪ್ರವಾಸ`;
      default:
        return `Trip to ${localizedCity}`;
    }
  }

  // If custom title includes the city name, replace just the city name
  if (city && title.toLowerCase().includes(city.toLowerCase())) {
    const regex = new RegExp(city, 'gi');
    return title.replace(regex, localizedCity);
  }

  return title;
}

/**
 * Registers new destinations dynamically at runtime if added.
 */
export function registerDestination(key, translations) {
  if (!key || !translations) return;
  const normalized = normalizeKey(key);
  DESTINATION_TRANSLATIONS[normalized] = {
    en: translations.en || key,
    hi: translations.hi || key,
    te: translations.te || key,
    ta: translations.ta || key,
    kn: translations.kn || key
  };
}

/**
 * Custom React Hook for consuming destination translations with instant reactive updates.
 * Automatically re-renders when language changes via i18next without requiring a page refresh.
 */
export function useDestinationTranslation() {
  const { i18n } = useTranslation();
  const currentLang = i18n?.language || localStorage.getItem('travelmate_lang') || 'en';

  return {
    currentLang,
    getDestName: (name) => getDestinationName(name, currentLang),
    getDestState: (state) => getDestinationState(state, currentLang),
    formatTripTitle: (title, city) => formatTripTitle(title, city, currentLang)
  };
}

export default {
  DESTINATION_TRANSLATIONS,
  STATE_TRANSLATIONS,
  getDestinationName,
  getDestinationState,
  formatTripTitle,
  registerDestination,
  useDestinationTranslation
};
