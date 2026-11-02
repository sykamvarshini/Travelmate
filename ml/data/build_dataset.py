import csv
import os
import random

def generate_datasets():
    data_dir = os.path.dirname(os.path.abspath(__file__))
    
    # 1. Indian Attractions Dataset
    attractions = [
        # GOA
        {
            "id": 1, "name": "Baga Beach", "city": "Goa", "state": "Goa",
            "category": "Beaches", "subcategory": "Water Sports & Nightlife",
            "rating": 4.6, "reviews_count": 14200, "duration_hours": 3.0, "entry_fee_inr": 0,
            "opening_time": "06:00", "closing_time": "23:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Evening", "activity_intensity": "High", "age_suitability": "Youth & Adults",
            "latitude": 15.5553, "longitude": 73.7517,
            "tags": "beach water-sports nightlife shacks sunset relaxation adventure",
            "description": "Famous beach known for thrilling watersports, lively beach shacks, and vibrant nightlife."
        },
        {
            "id": 2, "name": "Fort Aguada", "city": "Goa", "state": "Goa",
            "category": "History", "subcategory": "Fort & Lighthouse",
            "rating": 4.5, "reviews_count": 9800, "duration_hours": 2.0, "entry_fee_inr": 50,
            "opening_time": "09:00", "closing_time": "17:30", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 15.4920, "longitude": 73.7737,
            "tags": "fort history portuguese ocean-view lighthouse architecture photography",
            "description": "17th-century Portuguese fortress and lighthouse overlooking the vast Arabian Sea."
        },
        {
            "id": 3, "name": "Basilica of Bom Jesus", "city": "Goa", "state": "Goa",
            "category": "Culture", "subcategory": "Spiritual & Heritage Church",
            "rating": 4.7, "reviews_count": 11200, "duration_hours": 1.5, "entry_fee_inr": 0,
            "opening_time": "09:00", "closing_time": "18:30", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Morning", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 15.5009, "longitude": 73.9116,
            "tags": "unesco church baroque history spiritual architecture indoor calm",
            "description": "UNESCO World Heritage site holding the mortal remains of St. Francis Xavier, renowned for baroque architecture."
        },
        {
            "id": 4, "name": "Dudhsagar Waterfalls", "city": "Goa", "state": "Goa",
            "category": "Adventure", "subcategory": "Trek & Nature Waterfall",
            "rating": 4.8, "reviews_count": 8700, "duration_hours": 5.0, "entry_fee_inr": 500,
            "opening_time": "08:00", "closing_time": "16:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "High", "age_suitability": "Youth & Adults",
            "latitude": 15.3144, "longitude": 74.3143,
            "tags": "waterfall nature adventure trek jungle jeep-safari scenic photography",
            "description": "Four-tiered majestic waterfall cascading like milk on the Mandovi River along the Goa-Karnataka border."
        },
        {
            "id": 5, "name": "Goa State Museum", "city": "Goa", "state": "Goa",
            "category": "Museums", "subcategory": "Archaeology & Art",
            "rating": 4.2, "reviews_count": 3100, "duration_hours": 2.0, "entry_fee_inr": 20,
            "opening_time": "09:30", "closing_time": "17:30", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 15.4989, "longitude": 73.8278,
            "tags": "museum culture art artifacts history indoor quiet educational",
            "description": "State archaeological museum showcasing ancient stone sculptures, bronze items, and Goan history."
        },
        {
            "id": 6, "name": "Anjuna Flea Market", "city": "Goa", "state": "Goa",
            "category": "Shopping", "subcategory": "Street Bazaar & Handicrafts",
            "rating": 4.3, "reviews_count": 6400, "duration_hours": 2.5, "entry_fee_inr": 0,
            "opening_time": "09:00", "closing_time": "19:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 15.5786, "longitude": 73.7414,
            "tags": "shopping flea-market souvenirs jewelry clothes bohemian beachside",
            "description": "Legendary open-air market offering handicrafts, boho apparel, spices, jewelry, and street music."
        },
        {
            "id": 7, "name": "Spice Plantation Tour Ponda", "city": "Goa", "state": "Goa",
            "category": "Food", "subcategory": "Culinary Heritage & Agro-Tourism",
            "rating": 4.6, "reviews_count": 5200, "duration_hours": 3.0, "entry_fee_inr": 400,
            "opening_time": "09:00", "closing_time": "16:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 15.4012, "longitude": 74.0152,
            "tags": "food spices organic traditional-lunch nature flora rural authentic",
            "description": "Guided walking tour through aromatic spice plantations followed by authentic Goan buffet on banana leaves."
        },
        {
            "id": 8, "name": "Chapora Fort", "city": "Goa", "state": "Goa",
            "category": "History", "subcategory": "Scenic Ruins",
            "rating": 4.4, "reviews_count": 8900, "duration_hours": 1.5, "entry_fee_inr": 0,
            "opening_time": "06:00", "closing_time": "18:30", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Evening", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 15.6059, "longitude": 73.7368,
            "tags": "fort sunset panorama river photography iconic dil-chahta-hai",
            "description": "Hilltop fort ruins offering iconic sunset views of Vagator Beach and Chapora river."
        },
        {
            "id": 9, "name": "Palolem Beach & Kayaking", "city": "Goa", "state": "Goa",
            "category": "Beaches", "subcategory": "Scenic Bay & Water Sports",
            "rating": 4.8, "reviews_count": 12500, "duration_hours": 3.5, "entry_fee_inr": 0,
            "opening_time": "06:00", "closing_time": "22:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 15.0100, "longitude": 74.0232,
            "tags": "beach kayaking crescent-bay calm scenic swimming dolphin-spotting relaxation",
            "description": "Crescent-shaped serene beach in South Goa renowned for tranquil waters, dolphin trips, and kayaking."
        },
        {
            "id": 10, "name": "Museum of Christian Art", "city": "Goa", "state": "Goa",
            "category": "Museums", "subcategory": "Ecclesiastical Art",
            "rating": 4.5, "reviews_count": 1800, "duration_hours": 1.5, "entry_fee_inr": 100,
            "opening_time": "10:00", "closing_time": "17:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 15.5015, "longitude": 73.9135,
            "tags": "museum indoor religious-art heritage ivory paintings peaceful history",
            "description": "One-of-a-kind museum located in the Convent of Santa Monica housing Indo-Portuguese Christian artworks."
        },

        # JAIPUR
        {
            "id": 11, "name": "Amber Palace (Amer Fort)", "city": "Jaipur", "state": "Rajasthan",
            "category": "History", "subcategory": "Royal Fortress",
            "rating": 4.8, "reviews_count": 21000, "duration_hours": 3.0, "entry_fee_inr": 200,
            "opening_time": "08:00", "closing_time": "18:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 26.9855, "longitude": 75.8513,
            "tags": "fort heritage royal architecture unesco sheesh-mahal photography history",
            "description": "Majestic hilltop fort featuring red sandstone, marble courtyards, and the world-famous Sheesh Mahal."
        },
        {
            "id": 12, "name": "Hawa Mahal", "city": "Jaipur", "state": "Rajasthan",
            "category": "Culture", "subcategory": "Architectural Landmark",
            "rating": 4.6, "reviews_count": 18500, "duration_hours": 1.5, "entry_fee_inr": 50,
            "opening_time": "09:00", "closing_time": "17:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Morning", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 26.9239, "longitude": 75.8267,
            "tags": "palace pink-city architecture windows iconic photography history royal",
            "description": "The Palace of Winds constructed with 953 intricately designed jharokhas (casements) in pink sandstone."
        },
        {
            "id": 13, "name": "City Palace Jaipur", "city": "Jaipur", "state": "Rajasthan",
            "category": "Museums", "subcategory": "Royal Museum & Palace",
            "rating": 4.6, "reviews_count": 16000, "duration_hours": 2.5, "entry_fee_inr": 300,
            "opening_time": "09:30", "closing_time": "17:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 26.9258, "longitude": 75.8237,
            "tags": "palace museum royal royal-costumes arms art heritage courtyards",
            "description": "Stunning complex of courtyards, gardens, and buildings combining Rajput and Mughal architectural styles."
        },
        {
            "id": 14, "name": "Nahargarh Fort Sunset Viewpoint", "city": "Jaipur", "state": "Rajasthan",
            "category": "Adventure", "subcategory": "Hilltop Fortress & Trek",
            "rating": 4.7, "reviews_count": 13400, "duration_hours": 2.5, "entry_fee_inr": 50,
            "opening_time": "10:00", "closing_time": "21:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Evening", "activity_intensity": "Moderate", "age_suitability": "Youth & Adults",
            "latitude": 26.9373, "longitude": 75.8155,
            "tags": "sunset panorama city-lights hills fort photography cafe chill",
            "description": "Perched on the edge of the Aravalli Hills, providing breathtaking panoramic views of Jaipur city at dusk."
        },
        {
            "id": 15, "name": "Johari Bazaar & Bapu Bazaar", "city": "Jaipur", "state": "Rajasthan",
            "category": "Shopping", "subcategory": "Traditional Textiles & Jewelry",
            "rating": 4.5, "reviews_count": 9200, "duration_hours": 3.0, "entry_fee_inr": 0,
            "opening_time": "11:00", "closing_time": "21:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Evening", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 26.9200, "longitude": 75.8250,
            "tags": "shopping jewelry gemstones textiles jaipuri-quilts leather-shoes bazaar street-food",
            "description": "Vibrant traditional bazaars famous for authentic Kundan jewelry, bandhej textiles, and mojari footwear."
        },

        # KERALA (Munnar, Kochi, Alappuzha)
        {
            "id": 16, "name": "Tea Gardens & Kolukkumalai", "city": "Munnar", "state": "Kerala",
            "category": "Nature", "subcategory": "Plantation & Sunrise Trek",
            "rating": 4.9, "reviews_count": 15000, "duration_hours": 4.0, "entry_fee_inr": 150,
            "opening_time": "06:00", "closing_time": "17:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 10.0889, "longitude": 77.0595,
            "tags": "tea-estates mountains mist nature photography hills relaxation scenic trekking",
            "description": "Endless rolling green tea plantations shrouded in morning mist at the highest tea estate in the world."
        },
        {
            "id": 17, "name": "Eravikulam National Park", "city": "Munnar", "state": "Kerala",
            "category": "Wildlife", "subcategory": "Flora & Fauna Sanctuary",
            "rating": 4.6, "reviews_count": 9600, "duration_hours": 3.0, "entry_fee_inr": 200,
            "opening_time": "07:30", "closing_time": "16:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 10.1500, "longitude": 77.0600,
            "tags": "wildlife nilgiri-tahr mountains national-park nature endangered-species scenic",
            "description": "Protected mountain park sanctuary home to the endangered Nilgiri Tahr and blooming Neelakurinji flowers."
        },
        {
            "id": 18, "name": "Alappuzha Backwater Houseboat Cruise", "city": "Alappuzha", "state": "Kerala",
            "category": "Relaxation", "subcategory": "Houseboat & Scenic Lagoons",
            "rating": 4.9, "reviews_count": 18200, "duration_hours": 5.0, "entry_fee_inr": 3500,
            "opening_time": "11:00", "closing_time": "18:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 9.4981, "longitude": 76.3388,
            "tags": "backwaters houseboat relaxation kerala-cuisine waterways romantic serene family",
            "description": "Traditional wooden kettuvallam cruise through tranquil palm-fringed canals, paddy fields, and lagoons."
        },
        {
            "id": 19, "name": "Fort Kochi & Chinese Fishing Nets", "city": "Kochi", "state": "Kerala",
            "category": "Culture", "subcategory": "Heritage Port & Coastal Stroll",
            "rating": 4.5, "reviews_count": 12800, "duration_hours": 2.5, "entry_fee_inr": 0,
            "opening_time": "06:00", "closing_time": "20:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Evening", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 9.9658, "longitude": 76.2427,
            "tags": "coastal heritage fishing-nets art colonial cafes sunset history stroll",
            "description": "Historic colonial seaside town famous for cantilevered Chinese fishing nets, street art, and boutique cafes."
        },
        {
            "id": 20, "name": "Kerala Kathakali Centre", "city": "Kochi", "state": "Kerala",
            "category": "Culture", "subcategory": "Classical Performing Arts",
            "rating": 4.8, "reviews_count": 4700, "duration_hours": 2.0, "entry_fee_inr": 500,
            "opening_time": "17:00", "closing_time": "20:30", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Evening", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 9.9620, "longitude": 76.2435,
            "tags": "kathakali dance classical culture performance traditional makeup music indoor",
            "description": "Intimate cultural theatre offering traditional Kathakali classical dance drama, martial arts, and makeup demonstrations."
        },

        # DELHI & AGRA
        {
            "id": 21, "name": "Taj Mahal", "city": "Agra", "state": "Uttar Pradesh",
            "category": "History", "subcategory": "World Wonder & Monument",
            "rating": 4.9, "reviews_count": 45000, "duration_hours": 3.0, "entry_fee_inr": 250,
            "opening_time": "06:00", "closing_time": "18:30", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 27.1751, "longitude": 78.0421,
            "tags": "unesco wonder-of-world marble mughal romance history iconic architecture photography",
            "description": "Ivory-white marble mausoleum on the south bank of the Yamuna River, universally admired masterpiece of world heritage."
        },
        {
            "id": 22, "name": "Agra Fort", "city": "Agra", "state": "Uttar Pradesh",
            "category": "History", "subcategory": "Mughal Imperial Residence",
            "rating": 4.7, "reviews_count": 22000, "duration_hours": 2.5, "entry_fee_inr": 50,
            "opening_time": "06:00", "closing_time": "18:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 27.1795, "longitude": 78.0211,
            "tags": "unesco red-sandstone fort mughal palaces history yamuna-view",
            "description": "Immense red sandstone walled fortress city that was the main residence of Mughal emperors until 1638."
        },
        {
            "id": 23, "name": "Qutub Minar", "city": "Delhi", "state": "Delhi",
            "category": "History", "subcategory": "Ancient Victory Tower",
            "rating": 4.7, "reviews_count": 28000, "duration_hours": 2.0, "entry_fee_inr": 40,
            "opening_time": "07:00", "closing_time": "17:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 28.5245, "longitude": 77.1855,
            "tags": "unesco tower minaret medieval archaeology gardens history photography",
            "description": "73-meter tall minaret built of red sandstone and marble, surrounded by ancient ruins including the Iron Pillar."
        },
        {
            "id": 24, "name": "National Museum New Delhi", "city": "Delhi", "state": "Delhi",
            "category": "Museums", "subcategory": "National Art & History",
            "rating": 4.7, "reviews_count": 11500, "duration_hours": 3.0, "entry_fee_inr": 20,
            "opening_time": "10:00", "closing_time": "18:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 28.6118, "longitude": 77.2193,
            "tags": "museum indoor harappa indus-valley art bronze sculptures coins history",
            "description": "India's premier museum holding over 200,000 works of art representing 5,000 years of cultural heritage."
        },
        {
            "id": 25, "name": "Chandni Chowk Food Trail", "city": "Delhi", "state": "Delhi",
            "category": "Food", "subcategory": "Historic Street Culinary Experience",
            "rating": 4.8, "reviews_count": 19000, "duration_hours": 2.5, "entry_fee_inr": 300,
            "opening_time": "10:00", "closing_time": "22:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Evening", "activity_intensity": "Moderate", "age_suitability": "Youth & Adults",
            "latitude": 28.6506, "longitude": 77.2303,
            "tags": "food street-food paranthe-wali-gali chaat sweets culinary history heritage walk",
            "description": "Iconic centuries-old culinary walking trail tasting paranthas, jalebis, dahi bhalla, and kebabs in Old Delhi."
        },

        # MANALI & SHIMLA (Mountains & Adventure)
        {
            "id": 26, "name": "Solang Valley Adventure Arena", "city": "Manali", "state": "Himachal Pradesh",
            "category": "Adventure", "subcategory": "Paragliding & Snow Sports",
            "rating": 4.7, "reviews_count": 16500, "duration_hours": 4.0, "entry_fee_inr": 500,
            "opening_time": "09:00", "closing_time": "18:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "High", "age_suitability": "Youth & Adults",
            "latitude": 32.3166, "longitude": 77.1578,
            "tags": "adventure snow paragliding zorbing mountains himalayas thrilling skiing",
            "description": "High-altitude valley famed for adventure sports like paragliding, quad biking, zorbing, and winter skiing."
        },
        {
            "id": 27, "name": "Atal Tunnel & Sissu Valley", "city": "Manali", "state": "Himachal Pradesh",
            "category": "Mountains", "subcategory": "High Altitude Engineering & Vista",
            "rating": 4.9, "reviews_count": 14000, "duration_hours": 5.0, "entry_fee_inr": 0,
            "opening_time": "06:00", "closing_time": "17:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 32.4925, "longitude": 77.1643,
            "tags": "mountains tunnel engineering lahual waterfall snow scenic roadtrip",
            "description": "World's longest highway tunnel above 10,000 feet leading to the breathtaking snowy vistas of Lahaul."
        },
        {
            "id": 28, "name": "Hadimba Temple", "city": "Manali", "state": "Himachal Pradesh",
            "category": "Temples", "subcategory": "Ancient Cedar Forest Sanctuary",
            "rating": 4.6, "reviews_count": 12100, "duration_hours": 1.5, "entry_fee_inr": 0,
            "opening_time": "08:00", "closing_time": "18:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 32.2483, "longitude": 77.1804,
            "tags": "temple wooden-architecture deodar-forest spiritual peaceful mythology nature",
            "description": "Unique 16th-century wooden pagoda-style temple nestled amidst towering deodar cedar forests."
        },

        # VARANASI & RISHIKESH (Spiritual & Culture)
        {
            "id": 29, "name": "Dashashwamedh Ghat Ganga Aarti", "city": "Varanasi", "state": "Uttar Pradesh",
            "category": "Spiritual", "subcategory": "Evening River Ritual",
            "rating": 4.9, "reviews_count": 31000, "duration_hours": 2.0, "entry_fee_inr": 0,
            "opening_time": "18:00", "closing_time": "20:30", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Evening", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 25.3076, "longitude": 83.0104,
            "tags": "spiritual ganga-aarti river-ghats rituals fire chanting meditation iconic",
            "description": "Spectacular and deeply devotional evening prayer ceremony with brass lamps and Vedic chants on the sacred Ganges."
        },
        {
            "id": 30, "name": "Rishikesh White Water Rafting", "city": "Rishikesh", "state": "Uttarakhand",
            "category": "Adventure", "subcategory": "River Rapids & Cliff Jumping",
            "rating": 4.8, "reviews_count": 17800, "duration_hours": 3.5, "entry_fee_inr": 1200,
            "opening_time": "08:30", "closing_time": "16:30", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "High", "age_suitability": "Youth & Adults",
            "latitude": 30.1290, "longitude": 78.3242,
            "tags": "adventure river-rafting rapids ganga adrenaline cliff-jump mountains",
            "description": "World-class grade III & IV white water rafting expedition through the foothills of the Himalayas."
        },
        {
            "id": 31, "name": "Beatles Ashram (Chaurasi Kutia)", "city": "Rishikesh", "state": "Uttarakhand",
            "category": "Culture", "subcategory": "Meditation Hermitage & Murals",
            "rating": 4.5, "reviews_count": 6200, "duration_hours": 2.5, "entry_fee_inr": 150,
            "opening_time": "09:00", "closing_time": "16:30", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 30.1175, "longitude": 78.3150,
            "tags": "beatles meditation art graffiti forest peaceful history spiritual",
            "description": "Forest eco-ashram where The Beatles practiced Transcendental Meditation in 1968, adorned with psychedelic murals."
        },

        # BENGALURU & COORG & OOTY
        {
            "id": 32, "name": "Lalbagh Botanical Gardens", "city": "Bengaluru", "state": "Karnataka",
            "category": "Nature", "subcategory": "Historic Glasshouse & Flora",
            "rating": 4.6, "reviews_count": 24000, "duration_hours": 2.5, "entry_fee_inr": 30,
            "opening_time": "06:00", "closing_time": "19:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 12.9507, "longitude": 77.5848,
            "tags": "gardens flora glasshouse walking trees peaceful photography nature",
            "description": "240-acre botanical haven founded by Hyder Ali, featuring an iconic 1889 glass pavilion and centuries-old trees."
        },
        {
            "id": 33, "name": "Abbey Falls & Coffee Plantation", "city": "Coorg", "state": "Karnataka",
            "category": "Nature", "subcategory": "Waterfall & Spice Groves",
            "rating": 4.6, "reviews_count": 13900, "duration_hours": 2.0, "entry_fee_inr": 50,
            "opening_time": "09:00", "closing_time": "17:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 12.4542, "longitude": 75.7196,
            "tags": "waterfall coffee aroma nature greenery hanging-bridge mist photography",
            "description": "Cascading waterfall roaring amidst dense private coffee plantations and spice vines in the Western Ghats."
        },
        {
            "id": 34, "name": "Mysore Palace (Amba Vilas)", "city": "Mysuru", "state": "Karnataka",
            "category": "History", "subcategory": "Indo-Saracenic Royal Palace",
            "rating": 4.8, "reviews_count": 32000, "duration_hours": 2.5, "entry_fee_inr": 100,
            "opening_time": "10:00", "closing_time": "17:30", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Morning", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 12.3051, "longitude": 76.6551,
            "tags": "palace royal architecture illumination heritage museum golden-throne wodeyar",
            "description": "One of India's most visited royal palaces, famous for ornate durbar halls, stained glass, and weekend illuminations."
        },
        {
            "id": 35, "name": "Nilgiri Mountain Railway (Toy Train)", "city": "Ooty", "state": "Tamil Nadu",
            "category": "Mountains", "subcategory": "UNESCO Heritage Steam Train",
            "rating": 4.8, "reviews_count": 11800, "duration_hours": 3.0, "entry_fee_inr": 200,
            "opening_time": "07:10", "closing_time": "17:30", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Morning", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 11.4102, "longitude": 76.6950,
            "tags": "unesco toy-train steam-locomotive scenic bridges tunnels tea-plantations hills",
            "description": "Historic rack-and-pinion railway climbing through lush tea estates, dramatic bridges, and 16 mountain tunnels."
        }
    ,

        # ANDHRA PRADESH - VIJAYAWADA
        {
            "id": 36, "name": "Kanaka Durga Temple", "city": "Vijayawada", "state": "Andhra Pradesh",
            "category": "Spiritual", "subcategory": "Ancient Hilltop Shrine",
            "rating": 4.8, "reviews_count": 28000, "duration_hours": 2.0, "entry_fee_inr": 0,
            "opening_time": "04:00", "closing_time": "21:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 16.5165, "longitude": 80.6062,
            "tags": "temple spiritual goddess indrakeeladri krishna-river scenic heritage",
            "description": "Sacred 8th-century temple dedicated to Goddess Kanaka Durga, perched atop Indrakeeladri hill overlooking the Krishna River."
        },
        {
            "id": 37, "name": "Undavalli Caves", "city": "Vijayawada", "state": "Andhra Pradesh",
            "category": "History", "subcategory": "Rock-cut Cave Architecture",
            "rating": 4.6, "reviews_count": 14500, "duration_hours": 2.0, "entry_fee_inr": 25,
            "opening_time": "09:00", "closing_time": "17:30", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 16.4975, "longitude": 80.5796,
            "tags": "caves rock-cut ancient monolithic vishnu sandstone heritage photography",
            "description": "Monolithic 7th-century rock-cut cave temples featuring a colossal reclining statue of Lord Vishnu carved from granite."
        },
        {
            "id": 38, "name": "Prakasam Barrage", "city": "Vijayawada", "state": "Andhra Pradesh",
            "category": "Culture", "subcategory": "Iconic River Dam & Promenade",
            "rating": 4.5, "reviews_count": 18200, "duration_hours": 1.5, "entry_fee_inr": 0,
            "opening_time": "06:00", "closing_time": "22:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Evening", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 16.5074, "longitude": 80.6053,
            "tags": "barrage river krishna sunset breeze iconic illumination walking",
            "description": "Majestic 1.2-kilometer road bridge and barrage spanning the Krishna River, illuminated beautifully at dusk."
        },
        {
            "id": 39, "name": "Bapu Museum (Victoria Jubilee)", "city": "Vijayawada", "state": "Andhra Pradesh",
            "category": "Museums", "subcategory": "Archaeological & Art Museum",
            "rating": 4.5, "reviews_count": 6200, "duration_hours": 2.0, "entry_fee_inr": 50,
            "opening_time": "10:00", "closing_time": "17:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 16.5050, "longitude": 80.6385,
            "tags": "museum archaeology bapu art sculptures bronze coins indoor educational",
            "description": "State-of-the-art museum housing Buddhist and Jain sculptures, ancient bronze artifacts, weapons, and numismatics."
        },
        {
            "id": 40, "name": "Bhavani Island", "city": "Vijayawada", "state": "Andhra Pradesh",
            "category": "Adventure", "subcategory": "River Island & Water Sports",
            "rating": 4.4, "reviews_count": 11000, "duration_hours": 3.5, "entry_fee_inr": 100,
            "opening_time": "09:00", "closing_time": "18:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 16.5190, "longitude": 80.5910,
            "tags": "island river boating water-sports nature family picnic adventure",
            "description": "One of the largest river islands in India, situated in the Krishna River with boating, water roller, and leisure gardens."
        },
        {
            "id": 41, "name": "Kondapalli Fort", "city": "Vijayawada", "state": "Andhra Pradesh",
            "category": "History", "subcategory": "Medieval Fortress & Toy Craft",
            "rating": 4.3, "reviews_count": 7800, "duration_hours": 3.0, "entry_fee_inr": 50,
            "opening_time": "10:00", "closing_time": "17:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "High", "age_suitability": "Youth & Adults",
            "latitude": 16.6186, "longitude": 80.5367,
            "tags": "fort medieval trek kondapalli-toys gajapati history panorama hills",
            "description": "14th-century hilltop fortress built by the Musunuri Nayaks, renowned for military ruins and nearby Kondapalli wooden toy craft."
        },

        # ANDHRA PRADESH - VISAKHAPATNAM (VIZAG)
        {
            "id": 42, "name": "Kailasagiri Hilltop Park", "city": "Visakhapatnam", "state": "Andhra Pradesh",
            "category": "Nature", "subcategory": "Hilltop Vista & Cable Car",
            "rating": 4.7, "reviews_count": 24000, "duration_hours": 2.5, "entry_fee_inr": 50,
            "opening_time": "06:00", "closing_time": "19:30", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Evening", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 17.7490, "longitude": 83.3424,
            "tags": "hilltop ocean-view cable-car shiva-statue sunset ropeway panorama scenic",
            "description": "Hilltop park overlooking the Bay of Bengal featuring a 40-foot statue of Shiva & Parvathi, ropeway, and panoramic coastal views."
        },
        {
            "id": 43, "name": "INS Kursura Submarine Museum", "city": "Visakhapatnam", "state": "Andhra Pradesh",
            "category": "Museums", "subcategory": "Decommissioned Naval Submarine",
            "rating": 4.8, "reviews_count": 31000, "duration_hours": 1.5, "entry_fee_inr": 70,
            "opening_time": "14:00", "closing_time": "20:30", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 17.7171, "longitude": 83.3323,
            "tags": "submarine naval navy museum ocean rk-beach history indoor unique",
            "description": "South Asia's first submarine museum, housed inside a real decommissioned Soviet-built Kalvari-class submarine on RK Beach."
        },
        {
            "id": 44, "name": "TU 142 Aircraft Museum", "city": "Visakhapatnam", "state": "Andhra Pradesh",
            "category": "Museums", "subcategory": "Maritime Reconnaissance Aircraft",
            "rating": 4.7, "reviews_count": 19000, "duration_hours": 1.5, "entry_fee_inr": 70,
            "opening_time": "14:00", "closing_time": "20:30", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 17.7178, "longitude": 83.3331,
            "tags": "aircraft museum aviation flight naval simulator tupolev indoor",
            "description": "Preserved Tupolev TU-142 anti-submarine warfare aircraft transformed into an interactive aviation museum with flight simulators."
        },
        {
            "id": 45, "name": "Rushikonda Beach", "city": "Visakhapatnam", "state": "Andhra Pradesh",
            "category": "Beaches", "subcategory": "Blue Flag Beach & Water Sports",
            "rating": 4.6, "reviews_count": 22000, "duration_hours": 3.0, "entry_fee_inr": 0,
            "opening_time": "06:00", "closing_time": "20:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "High", "age_suitability": "All Ages",
            "latitude": 17.7820, "longitude": 83.3850,
            "tags": "beach blue-flag swimming water-sports speed-boat sea sand waves",
            "description": "Certified Blue Flag beach famed for golden sands, clear waters, surfing, jet-skiing, and surrounding rocky green hills."
        },
        {
            "id": 46, "name": "Simhachalam Temple", "city": "Visakhapatnam", "state": "Andhra Pradesh",
            "category": "Spiritual", "subcategory": "Medieval Kalinga Architecture",
            "rating": 4.8, "reviews_count": 26000, "duration_hours": 2.5, "entry_fee_inr": 0,
            "opening_time": "07:00", "closing_time": "21:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Morning", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 17.7667, "longitude": 83.2505,
            "tags": "temple narasimha kalinga architecture stone-carvings spiritual sandalwood",
            "description": "Ancient 11th-century temple dedicated to Varaha Lakshmi Narasimha covered in sandalwood paste, showcasing stunning Kalinga architecture."
        },
        {
            "id": 47, "name": "Ramakrishna Beach (RK Beach)", "city": "Visakhapatnam", "state": "Andhra Pradesh",
            "category": "Beaches", "subcategory": "Promenade & Cultural Coastal Strip",
            "rating": 4.5, "reviews_count": 28000, "duration_hours": 2.0, "entry_fee_inr": 0,
            "opening_time": "05:00", "closing_time": "23:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Evening", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 17.7145, "longitude": 83.3242,
            "tags": "beach promenade street-food sunset walking war-memorial coastline",
            "description": "Vibrant city beach with a sprawling coastal promenade, seafood stalls, War Memorial, and sea breezes."
        },

        # ANDHRA PRADESH - ARAKU VALLEY
        {
            "id": 48, "name": "Borra Caves", "city": "Araku Valley", "state": "Andhra Pradesh",
            "category": "Nature", "subcategory": "Million-Year Karst Limestone Caves",
            "rating": 4.8, "reviews_count": 35000, "duration_hours": 2.5, "entry_fee_inr": 80,
            "opening_time": "10:00", "closing_time": "17:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 18.2806, "longitude": 83.0396,
            "tags": "caves limestone stalactites stalagmites karst geology ananthagiri nature",
            "description": "Spectacular million-year-old subterranean caves filled with naturally formed stalactites and stalagmites illuminated with colorful lights."
        },
        {
            "id": 49, "name": "Araku Tribal Museum", "city": "Araku Valley", "state": "Andhra Pradesh",
            "category": "Museums", "subcategory": "Indigenous Culture & Lifestyle",
            "rating": 4.4, "reviews_count": 12000, "duration_hours": 1.5, "entry_fee_inr": 40,
            "opening_time": "09:00", "closing_time": "18:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 18.3312, "longitude": 82.8732,
            "tags": "museum tribal indigenous handicrafts art clay-models culture indoor",
            "description": "Dedicated cultural museum depicting the daily life, traditions, clay sculptures, and handicrafts of indigenous Eastern Ghat tribes."
        },
        {
            "id": 50, "name": "Coffee Museum Araku", "city": "Araku Valley", "state": "Andhra Pradesh",
            "category": "Food", "subcategory": "Coffee Plantation & Tasting Lounge",
            "rating": 4.6, "reviews_count": 9800, "duration_hours": 1.5, "entry_fee_inr": 50,
            "opening_time": "09:00", "closing_time": "19:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Morning", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 18.3320, "longitude": 82.8740,
            "tags": "coffee organic aroma plantation tasting chocolate cafe museum",
            "description": "Specialty museum showcasing the origin and process of organic Araku Arabica coffee with artisanal chocolates and tasting bar."
        },
        {
            "id": 51, "name": "Chaparai Water Cascades", "city": "Araku Valley", "state": "Andhra Pradesh",
            "category": "Adventure", "subcategory": "Natural Rock Slopes & Waterfall",
            "rating": 4.5, "reviews_count": 15000, "duration_hours": 2.5, "entry_fee_inr": 30,
            "opening_time": "08:00", "closing_time": "17:30", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 18.2917, "longitude": 82.7844,
            "tags": "waterfall cascades nature scenic stream bamboo-chicken hills picnic",
            "description": "Picturesque continuous water cascade flowing over smooth rock formations amidst dense Eastern Ghat forests."
        },
        {
            "id": 52, "name": "Padmapuram Botanical Gardens", "city": "Araku Valley", "state": "Andhra Pradesh",
            "category": "Nature", "subcategory": "Historic Nursery & Hanging Cottages",
            "rating": 4.2, "reviews_count": 8900, "duration_hours": 2.0, "entry_fee_inr": 40,
            "opening_time": "08:30", "closing_time": "18:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 18.3385, "longitude": 82.8682,
            "tags": "gardens flora tree-huts toy-train flowers botanical peaceful greenery",
            "description": "World War II era botanical sanctuary featuring rare flower orchards, treetop hanging huts, and a passenger toy train."
        },

        # ANDHRA PRADESH - TIRUPATI
        {
            "id": 53, "name": "Sri Venkateswara Swamy Temple (Tirumala)", "city": "Tirupati", "state": "Andhra Pradesh",
            "category": "Spiritual", "subcategory": "Vedic Pilgrimage & Golden Ananda Nilayam",
            "rating": 4.9, "reviews_count": 65000, "duration_hours": 4.0, "entry_fee_inr": 0,
            "opening_time": "03:00", "closing_time": "23:30", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 13.6833, "longitude": 79.3472,
            "tags": "temple balaji tirumala spiritual sacred dravidian pilgrimage hills",
            "description": "World-famous Hindu pilgrimage destination situated on the Seventh Peak of Tirumala hills, dedicated to Lord Venkateswara."
        },
        {
            "id": 54, "name": "Chandragiri Fort & Palace", "city": "Tirupati", "state": "Andhra Pradesh",
            "category": "History", "subcategory": "Vijayanagara Royal Stronghold",
            "rating": 4.5, "reviews_count": 11000, "duration_hours": 2.5, "entry_fee_inr": 50,
            "opening_time": "09:00", "closing_time": "17:30", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 13.5833, "longitude": 79.3167,
            "tags": "fort vijayanagara palace raja-mahal rani-mahal architecture museum history",
            "description": "11th-century fortress and crown citadel of the Vijayanagara Empire featuring the ornate Indo-Saracenic Raja Mahal."
        },
        {
            "id": 55, "name": "Sri Kapila Theertham", "city": "Tirupati", "state": "Andhra Pradesh",
            "category": "Spiritual", "subcategory": "Sacred Mountain Waterfall & Shiva Shrine",
            "rating": 4.7, "reviews_count": 14000, "duration_hours": 1.5, "entry_fee_inr": 0,
            "opening_time": "06:00", "closing_time": "20:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 13.6496, "longitude": 79.4265,
            "tags": "temple waterfall sacred shiva lake hills cave-sanctuary spiritual",
            "description": "Ancient Shiva temple situated at the base of Sheshadri hills where mountain waters cascade into a holy stepped temple pond."
        },
        {
            "id": 56, "name": "Regional Science Centre Tirupati", "city": "Tirupati", "state": "Andhra Pradesh",
            "category": "Museums", "subcategory": "Planetarium & Interactive Science",
            "rating": 4.4, "reviews_count": 5200, "duration_hours": 2.0, "entry_fee_inr": 40,
            "opening_time": "10:00", "closing_time": "18:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 13.6390, "longitude": 79.3970,
            "tags": "science planetarium space interactive educational museum indoor kids",
            "description": "Interactive science museum featuring a digital planetarium, 3D science theater, dino park, and planetary galleries."
        },

        # TELANGANA - HYDERABAD
        {
            "id": 57, "name": "Charminar & Laad Bazaar", "city": "Hyderabad", "state": "Telangana",
            "category": "History", "subcategory": "Qutb Shahi Monument & Pearl Bazaar",
            "rating": 4.7, "reviews_count": 48000, "duration_hours": 2.5, "entry_fee_inr": 25,
            "opening_time": "09:30", "closing_time": "19:00", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Evening", "activity_intensity": "Moderate", "age_suitability": "All Ages",
            "latitude": 17.3616, "longitude": 78.4747,
            "tags": "charminar qutb-shahi pearls shopping bazaar street-food architecture",
            "description": "1591 monument with four grand minarets surrounded by bustling traditional bazaars famous for lac bangles and Hyderabadi chai."
        },
        {
            "id": 58, "name": "Golconda Fort", "city": "Hyderabad", "state": "Telangana",
            "category": "History", "subcategory": "Medieval Acoustic Citadel & Diamond Vault",
            "rating": 4.7, "reviews_count": 39000, "duration_hours": 3.0, "entry_fee_inr": 50,
            "opening_time": "09:00", "closing_time": "17:30", "indoor_outdoor": "Outdoor",
            "best_time_of_day": "Morning", "activity_intensity": "High", "age_suitability": "Youth & Adults",
            "latitude": 17.3833, "longitude": 78.4011,
            "tags": "fort citadel acoustics diamonds kohinoor ruins ramparts history",
            "description": "Legendary medieval fortress once home to the Koh-i-Noor diamond, celebrated for its brilliant acoustic engineering."
        },
        {
            "id": 59, "name": "Salar Jung Museum", "city": "Hyderabad", "state": "Telangana",
            "category": "Museums", "subcategory": "Global Art & Antique Treasure",
            "rating": 4.8, "reviews_count": 34000, "duration_hours": 3.0, "entry_fee_inr": 50,
            "opening_time": "10:00", "closing_time": "17:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Afternoon", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 17.3713, "longitude": 78.4804,
            "tags": "museum art antiques clocked-statue veiled-rebecca jade indoor unesco",
            "description": "One of India's largest national museums showcasing the priceless one-man art collection of Salar Jung III, including the Veiled Rebecca."
        },
        {
            "id": 60, "name": "Birla Mandir & Planetarium", "city": "Hyderabad", "state": "Telangana",
            "category": "Spiritual", "subcategory": "Pure White Marble Hilltop Temple",
            "rating": 4.8, "reviews_count": 27000, "duration_hours": 2.0, "entry_fee_inr": 0,
            "opening_time": "07:00", "closing_time": "21:00", "indoor_outdoor": "Indoor",
            "best_time_of_day": "Evening", "activity_intensity": "Low", "age_suitability": "All Ages",
            "latitude": 17.4062, "longitude": 78.4691,
            "tags": "temple marble spiritual hussain-sagar view planetarium peaceful",
            "description": "Magnificent white Rajasthani marble temple perched high on Naubat Pahad, commanding panoramic views of the city and lake."
        }

    ]
    # Write attractions CSV
    attractions_file = os.path.join(data_dir, "indian_attractions.csv")
    fieldnames = list(attractions[0].keys())
    with open(attractions_file, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(attractions)
    print(f"Wrote {len(attractions)} attractions to {attractions_file}")

    # 2. Historical User Interactions Dataset (for Collaborative Filtering + Content model training)
    # 350 realistic users with varying demographics and ratings on activities
    random.seed(42)
    users = []
    travel_styles = ["Solo", "Couple", "Family", "Friends"]
    food_prefs = ["Vegetarian", "Non-Vegetarian", "Vegan", "Any"]
    activity_levels = ["Relaxed", "Moderate", "High Intensity"]
    budgets = ["Budget", "Mid-Range", "Luxury"]
    all_interests = [
        "Beaches", "Mountains", "Nature", "Adventure", "History", "Culture",
        "Temples", "Wildlife", "Food", "Shopping", "Nightlife", "Photography",
        "Relaxation", "Spiritual", "Museums"
    ]

    for uid in range(1, 351):
        age = random.choice([20, 22, 24, 27, 30, 35, 42, 55, 62])
        style = random.choice(travel_styles)
        fav_interests = random.sample(all_interests, k=random.randint(2, 4))
        users.append({
            "user_id": uid,
            "age": age,
            "travel_style": style,
            "food_pref": random.choice(food_prefs),
            "activity_level": random.choice(activity_levels),
            "budget": random.choice(budgets),
            "primary_interests": "|".join(fav_interests)
        })

    users_file = os.path.join(data_dir, "traveler_profiles.csv")
    with open(users_file, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=list(users[0].keys()))
        writer.writeheader()
        writer.writerows(users)
    print(f"Wrote {len(users)} traveler profiles to {users_file}")

    # Generate realistic interaction ratings (1-5) matching user interests
    interactions = []
    for u in users:
        u_interests = u["primary_interests"].split("|")
        # Give higher ratings to attractions that match user interests or travel style
        # Sample 5 to 12 attractions reviewed per user
        num_reviews = random.randint(6, 14)
        sampled_attractions = random.sample(attractions, k=num_reviews)
        for a in sampled_attractions:
            base_rating = 3.5
            if a["category"] in u_interests:
                base_rating += 1.0
            if u["age"] < 30 and a["activity_intensity"] == "High":
                base_rating += 0.5
            if u["age"] > 50 and a["activity_intensity"] == "Low":
                base_rating += 0.5
            if u["budget"] == "Budget" and a["entry_fee_inr"] == 0:
                base_rating += 0.3
            rating = min(5.0, max(1.0, round(base_rating + random.uniform(-0.5, 0.5), 1)))
            interactions.append({
                "user_id": u["user_id"],
                "attraction_id": a["id"],
                "rating": rating,
                "review_snippet": f"Great visit to {a['name']}. Loved the {a['category']} experience."
            })

    interactions_file = os.path.join(data_dir, "user_interactions.csv")
    with open(interactions_file, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=list(interactions[0].keys()))
        writer.writeheader()
        writer.writerows(interactions)
    print(f"Wrote {len(interactions)} user interaction ratings to {interactions_file}")

if __name__ == "__main__":
    generate_datasets()
