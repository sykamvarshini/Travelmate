pds_path = r"E:\travel\backend\src\main\java\com\travelmate\service\PlaceDiscoveryService.java"
with open(pds_path, "r", encoding="utf-8") as f:
    content = f.read()

# Fix unescaped quotes in Java string literals
broken_part = '''                String query = String.format(Locale.ROOT, "[out:json][timeout:5];("
                        + "node["tourism"~"attraction|museum|viewpoint|zoo|theme_park"](around:25000,%.4f,%.4f);"
                        + "node["historic"~"fort|monument|castle|ruins|archaeological_site"](around:25000,%.4f,%.4f);"
                        + "node["amenity"="place_of_worship"](around:25000,%.4f,%.4f);"
                        + (overpassFilter != null ? overpassFilter + "(around:25000,%.4f,%.4f);" : "")
                        + ");out center 15;", lat, lon, lat, lon, lat, lon);'''

fixed_part = '''                String query = String.format(Locale.ROOT, "[out:json][timeout:5];("
                        + "node[\\"tourism\\"~\\"attraction|museum|viewpoint|zoo|theme_park\\"](around:25000,%.4f,%.4f);"
                        + "node[\\"historic\\"~\\"fort|monument|castle|ruins|archaeological_site\\"](around:25000,%.4f,%.4f);"
                        + "node[\\"amenity\\"=\\"place_of_worship\\"](around:25000,%.4f,%.4f);"
                        + (overpassFilter != null ? overpassFilter + "(around:25000,%.4f,%.4f);" : "")
                        + ");out center 15;", lat, lon, lat, lon, lat, lon);'''

if broken_part in content:
    content = content.replace(broken_part, fixed_part)
    print("Fixed unescaped quotes in PlaceDiscoveryService.java.")
else:
    print("Warning: broken_part not found directly.")

with open(pds_path, "w", encoding="utf-8") as f:
    f.write(content)
