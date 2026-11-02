import os
import shutil

dir_c = r'c:\Users\HP\OneDrive\Desktop\travel planning'
dir_e = r'E:\travel'

files_to_sync = [
    # Backend files
    r'backend/pom.xml',
    r'backend/src/main/resources/application.yml',
    r'backend/src/main/java/com/travelmate/config/JwtAuthenticationFilter.java',
    r'backend/src/main/java/com/travelmate/controller/TripController.java',
    r'backend/src/main/java/com/travelmate/dto/InviteRequest.java',
    r'backend/src/main/java/com/travelmate/dto/TripCreateRequest.java',
    r'backend/src/main/java/com/travelmate/entity/InvitationStatus.java',
    r'backend/src/main/java/com/travelmate/entity/ItineraryItem.java',
    r'backend/src/main/java/com/travelmate/entity/Trip.java',
    r'backend/src/main/java/com/travelmate/entity/TripMember.java',
    r'backend/src/main/java/com/travelmate/entity/TripRole.java',
    r'backend/src/main/java/com/travelmate/repository/TripMemberRepository.java',
    r'backend/src/main/java/com/travelmate/service/PlaceDiscoveryService.java',
    r'backend/src/main/java/com/travelmate/service/TripService.java',
    r'backend/src/main/java/com/travelmate/service/WeatherService.java',

    # Frontend files
    r'frontend/src/services/api.js',
    r'frontend/src/services/transportValidation.js',
    r'frontend/src/pages/Itinerary.jsx',
    r'frontend/src/pages/Itinerary.css',
    r'frontend/src/pages/Dashboard.jsx',
    r'frontend/src/pages/Dashboard.css',
    r'frontend/src/pages/Home.jsx',
    r'frontend/src/pages/Home.css',
    r'frontend/src/pages/PlanTrip.jsx',
    r'frontend/src/pages/PlanTrip.css',
    r'frontend/src/pages/Explore.jsx',
    r'frontend/src/pages/Auth.jsx',
    r'frontend/src/components/layout/Navbar.jsx',
    r'frontend/src/components/PlaceDiscoveryCard.jsx',
    r'frontend/src/components/PlaceDiscoveryCard.css',
    r'frontend/src/components/common/SafeImage.jsx',
    r'frontend/src/utils/imageFallbacks.js',
    r'frontend/src/data/destinations.js',
    r'frontend/src/index.css'
]

print("Copying individual files...")
for rel in files_to_sync:
    src = os.path.join(dir_c, rel)
    dst = os.path.join(dir_e, rel)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copy2(src, dst)
    print(f"  Copied: {rel}")

# Copy public/images directory if it exists in C
c_images = os.path.join(dir_c, r'frontend/public/images')
e_images = os.path.join(dir_e, r'frontend/public/images')
if os.path.exists(c_images):
    print("Copying public/images directory...")
    shutil.copytree(c_images, e_images, dirs_exist_ok=True)
    print("  Copied public/images")

print("Sync completed successfully!")
