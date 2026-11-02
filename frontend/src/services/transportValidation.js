// Real Indian Commercial Airport Database (Scheduled Passenger Flights)
export const COMMERCIAL_AIRPORTS = {
  DEL: { code: 'DEL', city: 'Delhi', name: 'Indira Gandhi International Airport', coords: [28.5562, 77.1000] },
  BOM: { code: 'BOM', city: 'Mumbai', name: 'Chhatrapati Shivaji Maharaj International Airport', coords: [19.0896, 72.8656] },
  BLR: { code: 'BLR', city: 'Bengaluru', name: 'Kempegowda International Airport', coords: [13.1986, 77.7066] },
  HYD: { code: 'HYD', city: 'Hyderabad', name: 'Rajiv Gandhi International Airport', coords: [17.2403, 78.4294] },
  MAA: { code: 'MAA', city: 'Chennai', name: 'Chennai International Airport', coords: [12.9941, 80.1709] },
  CCU: { code: 'CCU', city: 'Kolkata', name: 'Netaji Subhash Chandra Bose International Airport', coords: [22.6547, 88.4467] },
  AMD: { code: 'AMD', city: 'Ahmedabad', name: 'Sardar Vallabhbhai Patel International Airport', coords: [23.0772, 72.6347] },
  PNQ: { code: 'PNQ', city: 'Pune', name: 'Pune International Airport', coords: [18.5822, 73.9197] },
  GOI: { code: 'GOI', city: 'Goa', name: 'Dabolim / Mopa International Airport', coords: [15.3800, 73.8314] },
  JAI: { code: 'JAI', city: 'Jaipur', name: 'Jaipur International Airport', coords: [26.8242, 75.8122] },
  LKO: { code: 'LKO', city: 'Lucknow', name: 'Chaudhary Charan Singh International Airport', coords: [26.7606, 80.8893] },
  COK: { code: 'COK', city: 'Kochi', name: 'Cochin International Airport', coords: [10.1518, 76.3930] },
  VTZ: { code: 'VTZ', city: 'Visakhapatnam', name: 'Visakhapatnam International Airport', coords: [17.7211, 83.2245] },
  VGA: { code: 'VGA', city: 'Vijayawada', name: 'Vijayawada (Gannavaram) Airport', coords: [16.5304, 80.7968] },
  TIR: { code: 'TIR', city: 'Tirupati', name: 'Tirupati International Airport', coords: [13.6325, 79.5434] },
  RJA: { code: 'RJA', city: 'Rajahmundry', name: 'Rajahmundry Domestic Airport', coords: [17.1106, 81.8284] },
  IXM: { code: 'IXM', city: 'Madurai', name: 'Madurai International Airport', coords: [9.8345, 78.0934] },
  CJB: { code: 'CJB', city: 'Coimbatore', name: 'Coimbatore International Airport', coords: [11.0298, 77.0434] },
  VNS: { code: 'VNS', city: 'Varanasi', name: 'Lal Bahadur Shastri International Airport', coords: [25.4524, 82.8590] },
  IXC: { code: 'IXC', city: 'Chandigarh', name: 'Shaheed Bhagat Singh International Airport', coords: [30.6735, 76.7885] },
  BBI: { code: 'BBI', city: 'Bhubaneswar', name: 'Biju Patnaik International Airport', coords: [20.2444, 85.8178] },
  PAT: { code: 'PAT', city: 'Patna', name: 'Jay Prakash Narayan Airport', coords: [25.5913, 85.0880] },
  IDR: { code: 'IDR', city: 'Indore', name: 'Devi Ahilya Bai Holkar Airport', coords: [22.7217, 75.8011] },
  SXR: { code: 'SXR', city: 'Srinagar', name: 'Sheikh ul-Alam International Airport', coords: [33.9871, 74.7741] },
  UDR: { code: 'UDR', city: 'Udaipur', name: 'Maharana Pratap Airport', coords: [24.6177, 73.8961] },
  IXE: { code: 'IXE', city: 'Mangalore', name: 'Mangaluru International Airport', coords: [12.9613, 74.8900] },
  CCJ: { code: 'CCJ', city: 'Calicut', name: 'Calicut International Airport', coords: [11.1368, 75.9553] },
  TRV: { code: 'TRV', city: 'Thiruvananthapuram', name: 'Thiruvananthapuram International Airport', coords: [8.4821, 76.9200] },
  GAU: { code: 'GAU', city: 'Guwahati', name: 'Lokpriya Gopinath Bordoloi Airport', coords: [26.1061, 91.5859] },
  STV: { code: 'STV', city: 'Surat', name: 'Surat International Airport', coords: [21.1141, 72.7419] },
  NAG: { code: 'NAG', city: 'Nagpur', name: 'Dr. Babasaheb Ambedkar International Airport', coords: [21.0922, 79.0472] },
  BHO: { code: 'BHO', city: 'Bhopal', name: 'Raja Bhoj Airport', coords: [23.2875, 77.3378] },
  ATQ: { code: 'ATQ', city: 'Amritsar', name: 'Sri Guru Ram Dass Jee International Airport', coords: [31.7096, 74.7973] },
  DED: { code: 'DED', city: 'Dehradun', name: 'Jolly Grant Airport', coords: [30.1897, 78.1803] },
  IXB: { code: 'IXB', city: 'Bagdogra', name: 'Bagdogra International Airport', coords: [26.6812, 88.3286] },
  IXZ: { code: 'IXZ', city: 'Port Blair', name: 'Veer Savarkar International Airport', coords: [11.6410, 92.7297] },
  AYJ: { code: 'AYJ', city: 'Ayodhya', name: 'Maharishi Valmiki International Airport', coords: [26.7369, 82.1557] }
};

// Towns and pilgrimage centers that explicitly lack commercial airports
export const NON_AIRPORT_TOWNS = [
  'gudivada', 'machilipatnam', 'eluru', 'bhimavaram', 'tenali', 'guntur',
  'araku', 'araku valley', 'ooty', 'kodaikanal', 'munnar', 'hampi',
  'lepakshi', 'mahabalipuram', 'alappuzha', 'alleppey', 'rameswaram',
  'wayanad', 'chikmagalur', 'coorg', 'madikeri', 'rishikesh', 'kasol'
];

/**
 * Calculates Haversine distance in km between two lat/lng pairs
 */
export function calculateHaversineKm(lat1, lon1, lat2, lon2) {
  if (!lat1 || !lon1 || !lat2 || !lon2) return 50.0;
  const R = 6371; // Earth's radius in km
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c * 10) / 10;
}

/**
 * Checks if a city has a verified commercial airport
 */
export function findAirport(cityName) {
  if (!cityName) return null;
  const clean = cityName.trim().toLowerCase();
  
  // Check known non-airport towns first
  if (NON_AIRPORT_TOWNS.some(town => clean.includes(town) || town.includes(clean))) {
    return null;
  }

  for (const key of Object.keys(COMMERCIAL_AIRPORTS)) {
    const ap = COMMERCIAL_AIRPORTS[key];
    if (clean.includes(ap.city.toLowerCase()) || ap.city.toLowerCase().includes(clean)) {
      return ap;
    }
  }
  return null;
}

/**
 * Validates travel segment feasibility and determines realistic transport
 */
export function validateSegmentTransport(origin, destination, requestedMode, originCoords = null, destCoords = null) {
  const origClean = origin ? origin.trim() : 'Origin';
  const destClean = destination ? destination.trim() : 'Destination';

  let distanceKm = 45;
  if (originCoords && destCoords) {
    distanceKm = calculateHaversineKm(originCoords[0], originCoords[1], destCoords[0], destCoords[1]);
  }

  const modeLower = (requestedMode || 'Bus / Road').toLowerCase();
  const isFlight = modeLower.includes('flight') || modeLower.includes('air');
  const isTrain = modeLower.includes('train') || modeLower.includes('rail');

  const originAirport = findAirport(origClean);
  const destAirport = findAirport(destClean);

  if (isFlight) {
    // Check if flight is realistically possible
    if (!originAirport || !destAirport || distanceKm < 150) {
      let specificReason = '';
      if (!originAirport) {
        specificReason = `${origClean} does not have a commercial airport (nearest is ${destAirport ? destAirport.name : 'regional airport'}).`;
      } else if (!destAirport) {
        specificReason = `${destClean} does not have a commercial airport.`;
      } else {
        specificReason = `Distance (${distanceKm} km) is too short for scheduled passenger flights.`;
      }

      const recommendedGround = distanceKm > 300 
        ? 'Train (Indian Railways)' 
        : (distanceKm > 90 ? 'Intercity Express Bus / Train' : 'Bus / Road (Direct Highway)');

      const estHours = Math.round((distanceKm / 45) * 10) / 10;
      const hoursStr = estHours < 1 ? `${Math.round(estHours * 60)} mins` : `${Math.floor(estHours)}h ${Math.round((estHours % 1) * 60)}m`;

      return {
        isAvailable: false,
        requestedMode: 'Flight',
        actualMode: recommendedGround,
        distanceKm,
        estimatedTime: hoursStr,
        estimatedCostInr: distanceKm > 300 ? Math.round(distanceKm * 1.5) : Math.max(65, Math.round(distanceKm * 1.8)),
        notice: `Your selected transport is not available for this segment. A feasible alternative has been selected. (${origClean} → ${destClean}: Switched from Flight to ${recommendedGround} - ${specificReason})`,
        originAirport: null,
        destAirport: destAirport ? destAirport.code : null
      };
    } else {
      return {
        isAvailable: true,
        requestedMode: 'Flight',
        actualMode: `Flight (${originAirport.code} → ${destAirport.code})`,
        distanceKm,
        estimatedTime: '1h 20m + 2h airport check-in',
        estimatedCostInr: Math.round(3500 + distanceKm * 3.5),
        notice: null,
        originAirport: originAirport.code,
        destAirport: destAirport.code
      };
    }
  }

  if (isTrain) {
    const estHours = Math.round((distanceKm / 55) * 10) / 10;
    const hoursStr = estHours < 1 ? `${Math.round(estHours * 60)} mins` : `${Math.floor(estHours)}h ${Math.round((estHours % 1) * 60)}m`;
    return {
      isAvailable: true,
      requestedMode: 'Train',
      actualMode: 'Train (Indian Railways)',
      distanceKm,
      estimatedTime: hoursStr,
      estimatedCostInr: Math.max(60, Math.round(distanceKm * 1.2)),
      notice: null
    };
  }

  // Ground / Bus / Road Cab
  const estHours = Math.round((distanceKm / 45) * 10) / 10;
  const hoursStr = estHours < 1 ? `${Math.round(estHours * 60)} mins` : `${Math.floor(estHours)}h ${Math.round((estHours % 1) * 60)}m`;
  return {
    isAvailable: true,
    requestedMode: requestedMode || 'Bus / Road',
    actualMode: distanceKm <= 50 ? 'Bus / Road Transport' : 'Intercity Cab / Bus',
    distanceKm,
    estimatedTime: hoursStr,
    estimatedCostInr: Math.max(60, Math.round(distanceKm * 1.6)),
    notice: null
  };
}
