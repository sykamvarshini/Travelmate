package com.travelmate.service;

import com.travelmate.entity.ItineraryDay;
import com.travelmate.entity.ItineraryItem;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Intelligent Geographical Route Optimization Service.
 *
 * Implements Cluster-Based Open Traveling Salesperson Problem (TSP) heuristic,
 * Or-Opt (node relocation), and 2-Opt local search improvement to eliminate
 * backtracking, cluster attractions geographically into daily neighbourhood tours,
 * and calculate accurate inter-attraction distances and travel times.
 */
@Service
public class RouteOptimizationService {

    private static final int EARTH_RADIUS_KM = 6371;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @FunctionalInterface
    public interface CoordinateExtractor<T> {
        double[] getCoordinates(T item, Double defaultLat, Double defaultLon);
    }

    /**
     * Coordinate extractor for recommendations and discovered place maps.
     */
    public static final CoordinateExtractor<Map<String, Object>> MAP_EXTRACTOR = (map, defLat, defLon) -> {
        double lat = defLat != null ? defLat : 20.5937;
        double lon = defLon != null ? defLon : 78.9629;
        if (map == null) return new double[]{lat, lon};

        Object latObj = map.get("latitude");
        if (latObj == null) latObj = map.get("lat");
        if (latObj != null) {
            try { lat = Double.parseDouble(latObj.toString().trim()); } catch (Exception ignored) {}
        }

        Object lonObj = map.get("longitude");
        if (lonObj == null) lonObj = map.get("lon");
        if (lonObj != null) {
            try { lon = Double.parseDouble(lonObj.toString().trim()); } catch (Exception ignored) {}
        }

        return new double[]{lat, lon};
    };

    /**
     * Coordinate extractor for ItineraryItem entities.
     */
    public static final CoordinateExtractor<ItineraryItem> ITEM_EXTRACTOR = (item, defLat, defLon) -> {
        double lat = defLat != null ? defLat : 20.5937;
        double lon = defLon != null ? defLon : 78.9629;
        if (item == null) return new double[]{lat, lon};

        if (item.getLatitude() != null) lat = item.getLatitude();
        if (item.getLongitude() != null) lon = item.getLongitude();

        return new double[]{lat, lon};
    };

    /**
     * Calculates the great-circle Haversine distance between two points in km.
     */
    public double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculates the total open path travel cost from a starting anchor through a sequence of items.
     */
    private <T> double computeRouteCost(double startLat, double startLon, List<T> route,
                                        CoordinateExtractor<T> extractor, Double defLat, Double defLon) {
        if (route == null || route.isEmpty()) return 0.0;
        double cost = 0.0;
        double currLat = startLat;
        double currLon = startLon;
        for (T item : route) {
            double[] coords = extractor.getCoordinates(item, defLat, defLon);
            cost += haversineDistance(currLat, currLon, coords[0], coords[1]);
            currLat = coords[0];
            currLon = coords[1];
        }
        return cost;
    }

    /**
     * Optimizes an open route through a set of attractions starting from (startLat, startLon)
     * using Cluster-Based Open TSP sequencing, Or-Opt relocation, and 2-Opt local search refinement.
     */
    public <T> List<T> optimizeOpenRoute(double startLat, double startLon, List<T> items,
                                         CoordinateExtractor<T> extractor,
                                         Double defaultLat, Double defaultLon) {
        if (items == null || items.size() <= 1) {
            return items != null ? new ArrayList<>(items) : new ArrayList<>();
        }

        int n = items.size();
        if (n == 2) {
            double[] c0 = extractor.getCoordinates(items.get(0), defaultLat, defaultLon);
            double[] c1 = extractor.getCoordinates(items.get(1), defaultLat, defaultLon);
            double d0 = haversineDistance(startLat, startLon, c0[0], c0[1]);
            double d1 = haversineDistance(startLat, startLon, c1[0], c1[1]);
            return (d1 < d0) ? List.of(items.get(1), items.get(0)) : new ArrayList<>(items);
        }

        // 1. Spatial Hierarchical Clustering
        // Groups geographically nearby attractions into clusters (neighborhoods)
        double clusterRadius = 8.0;
        List<List<T>> clusters = new ArrayList<>();
        for (T item : items) {
            double[] coords = extractor.getCoordinates(item, defaultLat, defaultLon);
            List<T> bestCluster = null;
            double minDist = Double.MAX_VALUE;

            for (List<T> cluster : clusters) {
                for (T member : cluster) {
                    double[] mCoords = extractor.getCoordinates(member, defaultLat, defaultLon);
                    double d = haversineDistance(coords[0], coords[1], mCoords[0], mCoords[1]);
                    if (d < minDist) {
                        minDist = d;
                        bestCluster = cluster;
                    }
                }
            }

            if (bestCluster != null && minDist <= clusterRadius) {
                bestCluster.add(item);
            } else {
                List<T> newCluster = new ArrayList<>();
                newCluster.add(item);
                clusters.add(newCluster);
            }
        }

        // 2. Progressive Macro Cluster Sequencing with Backtracking Defense
        // Visit clusters progressively from start position, penalizing returning to already-visited areas
        List<List<T>> unvisitedClusters = new ArrayList<>(clusters);
        List<List<T>> orderedClusters = new ArrayList<>();
        List<double[]> visitedCentroids = new ArrayList<>();
        double currLat = startLat;
        double currLon = startLon;

        while (!unvisitedClusters.isEmpty()) {
            int bestIdx = 0;
            double bestScore = Double.MAX_VALUE;

            for (int i = 0; i < unvisitedClusters.size(); i++) {
                List<T> c = unvisitedClusters.get(i);
                double minDistanceToCluster = Double.MAX_VALUE;
                double sumLat = 0.0;
                double sumLon = 0.0;
                for (T member : c) {
                    double[] mCoords = extractor.getCoordinates(member, defaultLat, defaultLon);
                    double d = haversineDistance(currLat, currLon, mCoords[0], mCoords[1]);
                    if (d < minDistanceToCluster) {
                        minDistanceToCluster = d;
                    }
                    sumLat += mCoords[0];
                    sumLon += mCoords[1];
                }

                double cLat = sumLat / c.size();
                double cLon = sumLon / c.size();

                // Backtracking penalty: heavily penalize moving towards already visited cluster centroids
                double penalty = 0.0;
                for (double[] prevCentroid : visitedCentroids) {
                    double distToPrev = haversineDistance(cLat, cLon, prevCentroid[0], prevCentroid[1]);
                    double currToPrev = haversineDistance(currLat, currLon, prevCentroid[0], prevCentroid[1]);
                    if (distToPrev < currToPrev - 2.0) {
                        penalty += (currToPrev - distToPrev) * 2.5;
                    }
                }

                double score = minDistanceToCluster + penalty;
                if (score < bestScore) {
                    bestScore = score;
                    bestIdx = i;
                }
            }

            List<T> chosenCluster = unvisitedClusters.remove(bestIdx);
            orderedClusters.add(chosenCluster);

            double sumLat = 0.0;
            double sumLon = 0.0;
            double closestLat = currLat;
            double closestLon = currLon;
            double minD = Double.MAX_VALUE;

            for (T member : chosenCluster) {
                double[] mCoords = extractor.getCoordinates(member, defaultLat, defaultLon);
                sumLat += mCoords[0];
                sumLon += mCoords[1];
                double d = haversineDistance(currLat, currLon, mCoords[0], mCoords[1]);
                if (d < minD) {
                    minD = d;
                    closestLat = mCoords[0];
                    closestLon = mCoords[1];
                }
            }

            visitedCentroids.add(new double[]{sumLat / chosenCluster.size(), sumLon / chosenCluster.size()});
            currLat = closestLat;
            currLon = closestLon;
        }

        // 3. Intra-Cluster Route Construction
        // Visit all places within each cluster together before moving to the next cluster
        List<T> tour = new ArrayList<>(n);
        double microLat = startLat;
        double microLon = startLon;

        for (List<T> c : orderedClusters) {
            List<T> unvisitedInCluster = new ArrayList<>(c);
            while (!unvisitedInCluster.isEmpty()) {
                int bestMemberIdx = 0;
                double bestMemberDist = Double.MAX_VALUE;
                for (int m = 0; m < unvisitedInCluster.size(); m++) {
                    double[] mCoords = extractor.getCoordinates(unvisitedInCluster.get(m), defaultLat, defaultLon);
                    double d = haversineDistance(microLat, microLon, mCoords[0], mCoords[1]);
                    if (d < bestMemberDist) {
                        bestMemberDist = d;
                        bestMemberIdx = m;
                    }
                }
                T picked = unvisitedInCluster.remove(bestMemberIdx);
                tour.add(picked);
                double[] pCoords = extractor.getCoordinates(picked, defaultLat, defaultLon);
                microLat = pCoords[0];
                microLon = pCoords[1];
            }
        }

        // 4. Global Untangling: Or-Opt (Relocation) + 2-Opt (Subtour Inversion)
        // Eliminates stranded points and untangles any crossing paths across the complete open route
        boolean improved = true;
        int iterations = 0;
        final int MAX_ITERATIONS = 100;

        while (improved && iterations < MAX_ITERATIONS) {
            improved = false;
            iterations++;

            // Phase A: Or-Opt single node relocation
            for (int i = 0; i < n; i++) {
                T node = tour.get(i);
                double currentCost = computeRouteCost(startLat, startLon, tour, extractor, defaultLat, defaultLon);

                for (int j = 0; j < n; j++) {
                    if (i == j) continue;
                    List<T> trial = new ArrayList<>(tour);
                    trial.remove(i);
                    trial.add(j, node);

                    double trialCost = computeRouteCost(startLat, startLon, trial, extractor, defaultLat, defaultLon);
                    if (trialCost < currentCost - 1e-4) {
                        tour = trial;
                        improved = true;
                        break;
                    }
                }
                if (improved) break;
            }
            if (improved) continue;

            // Phase B: 2-Opt edge inversion
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    double currentCost = computeRouteCost(startLat, startLon, tour, extractor, defaultLat, defaultLon);
                    List<T> trial = new ArrayList<>(tour);
                    reverse(trial, i, j);

                    double trialCost = computeRouteCost(startLat, startLon, trial, extractor, defaultLat, defaultLon);
                    if (trialCost < currentCost - 1e-4) {
                        tour = trial;
                        improved = true;
                        break;
                    }
                }
                if (improved) break;
            }
        }

        return tour;
    }

    private <T> void reverse(List<T> list, int from, int to) {
        while (from < to) {
            T temp = list.get(from);
            list.set(from, list.get(to));
            list.set(to, temp);
            from++;
            to--;
        }
    }

    /**
     * Partitions and optimizes items across multiple days based on daily quotas.
     * Evaluates both progressive neighbourhood clustering and global tour partitioning,
     * selecting the strategy with minimal total travel distance.
     */
    public <T> List<List<T>> partitionAndOptimize(double startLat, double startLon,
                                                  List<T> items, List<Integer> dailyQuotas,
                                                  CoordinateExtractor<T> extractor,
                                                  Double defaultLat, Double defaultLon) {
        if (items == null || items.isEmpty() || dailyQuotas == null || dailyQuotas.isEmpty()) {
            return Collections.emptyList();
        }

        if (dailyQuotas.size() == 1) {
            List<T> singleDay = optimizeOpenRoute(startLat, startLon, items, extractor, defaultLat, defaultLon);
            return List.of(singleDay);
        }

        // Strategy A: Global Progressive Cluster Tour Partitioning with Intra-Day Refinement
        List<List<T>> planA = new ArrayList<>();
        List<T> globalTour = optimizeOpenRoute(startLat, startLon, items, extractor, defaultLat, defaultLon);

        int offset = 0;
        double currStartLatA = startLat;
        double currStartLonA = startLon;

        for (int q : dailyQuotas) {
            int end = Math.min(offset + q, globalTour.size());
            List<T> slice = new ArrayList<>(globalTour.subList(offset, end));
            offset = end;

            List<T> optDay = optimizeOpenRoute(currStartLatA, currStartLonA, slice, extractor, defaultLat, defaultLon);
            planA.add(optDay);
            if (!optDay.isEmpty()) {
                double[] lastCoords = extractor.getCoordinates(optDay.get(optDay.size() - 1), defaultLat, defaultLon);
                currStartLatA = lastCoords[0];
                currStartLonA = lastCoords[1];
            }
        }

        // Strategy B: Progressive Clustered Daily Assignment
        List<List<T>> planB = new ArrayList<>();
        List<T> poolB = new ArrayList<>(items);
        double currStartLatB = startLat;
        double currStartLonB = startLon;

        for (int q : dailyQuotas) {
            List<T> dayItems = new ArrayList<>();
            double cLat = currStartLatB;
            double cLon = currStartLonB;

            for (int step = 0; step < q && !poolB.isEmpty(); step++) {
                int bestIdx = 0;
                double bestDist = Double.MAX_VALUE;
                for (int i = 0; i < poolB.size(); i++) {
                    double[] coords = extractor.getCoordinates(poolB.get(i), defaultLat, defaultLon);
                    double d = haversineDistance(cLat, cLon, coords[0], coords[1]);
                    if (d < bestDist) {
                        bestDist = d;
                        bestIdx = i;
                    }
                }
                T chosen = poolB.remove(bestIdx);
                dayItems.add(chosen);
                double[] chosenCoords = extractor.getCoordinates(chosen, defaultLat, defaultLon);
                cLat = chosenCoords[0];
                cLon = chosenCoords[1];
            }

            List<T> optDay = optimizeOpenRoute(currStartLatB, currStartLonB, dayItems, extractor, defaultLat, defaultLon);
            planB.add(optDay);
            if (!optDay.isEmpty()) {
                double[] lastCoords = extractor.getCoordinates(optDay.get(optDay.size() - 1), defaultLat, defaultLon);
                currStartLatB = lastCoords[0];
                currStartLonB = lastCoords[1];
            }
        }

        double costA = computeTotalPlanDistance(startLat, startLon, planA, extractor, defaultLat, defaultLon);
        double costB = computeTotalPlanDistance(startLat, startLon, planB, extractor, defaultLat, defaultLon);

        return (costA <= costB) ? planA : planB;
    }

    /**
     * Computes the total travel distance of a multi-day plan.
     */
    private <T> double computeTotalPlanDistance(double startLat, double startLon,
                                                List<List<T>> plan,
                                                CoordinateExtractor<T> extractor,
                                                Double defaultLat, Double defaultLon) {
        double total = 0.0;
        double cLat = startLat;
        double cLon = startLon;

        for (List<T> day : plan) {
            for (T item : day) {
                double[] coords = extractor.getCoordinates(item, defaultLat, defaultLon);
                total += haversineDistance(cLat, cLon, coords[0], coords[1]);
                cLat = coords[0];
                cLon = coords[1];
            }
        }
        return total;
    }

    /**
     * Convenience method to partition and optimize recommendation maps.
     */
    public List<List<Map<String, Object>>> partitionAndOptimizeRecs(double startLat, double startLon,
                                                                   List<Map<String, Object>> recs,
                                                                   List<Integer> dailyQuotas,
                                                                   Double defaultLat, Double defaultLon) {
        return partitionAndOptimize(startLat, startLon, recs, dailyQuotas, MAP_EXTRACTOR, defaultLat, defaultLon);
    }

    /**
     * Convenience method to optimize a single day's ItineraryItem list.
     */
    public List<ItineraryItem> optimizeDayItems(double startLat, double startLon,
                                               List<ItineraryItem> items,
                                               Double defaultLat, Double defaultLon) {
        return optimizeOpenRoute(startLat, startLon, items, ITEM_EXTRACTOR, defaultLat, defaultLon);
    }

    /**
     * Recalculates consecutive transit distances, durations, costs, and scheduled start/end times
     * for all items in an ItineraryDay to ensure seamless consistency.
     */
    public void recalculateDaySchedule(ItineraryDay day, double startLat, double startLon,
                                       String startLocationName, LocalTime dayStartTime) {
        if (day == null || day.getItems() == null || day.getItems().isEmpty()) {
            return;
        }

        LocalTime currentClock = (dayStartTime != null) ? dayStartTime : LocalTime.of(9, 0);
        List<ItineraryItem> items = day.getItems();
        ItineraryItem lastActive = null;

        for (int j = 0; j < items.size(); j++) {
            ItineraryItem item = items.get(j);
            item.setItemOrder(j);

            // Skip cancelled or replaced items from schedule timings
            if ("CANCELLED".equalsIgnoreCase(item.getStatus()) || "REPLACED".equalsIgnoreCase(item.getStatus())) {
                continue;
            }

            int durMinutes = (item.getDurationMinutes() != null && item.getDurationMinutes() > 0)
                    ? item.getDurationMinutes()
                    : 60;

            if (lastActive == null) {
                if (item.getLatitude() != null && item.getLongitude() != null) {
                    double rawDist = haversineDistance(startLat, startLon, item.getLatitude(), item.getLongitude());
                    double distKm = Math.max(0.5, Math.round(rawDist * 10.0) / 10.0);
                    int transitMin = Math.max(15, Math.min(60, (int) Math.round((distKm / 25.0) * 60) + 8));
                    item.setDistanceFromPrevious(distKm);
                    item.setDurationFromPrevious(transitMin);
                    item.setEstimatedTransportCost(Math.max(40.0, Math.round(distKm * 15.0)));
                } else {
                    item.setDistanceFromPrevious(null);
                    item.setDurationFromPrevious(15);
                    item.setEstimatedTransportCost(0.0);
                }
                String fromName = (startLocationName != null && !startLocationName.trim().isEmpty())
                        ? startLocationName.trim()
                        : "Starting Hub";
                item.setTransportFromPrevious("Local Transit (" + fromName + " → " + item.getTitle() + ")");
            } else {
                if (lastActive.getLatitude() != null && item.getLatitude() != null) {
                    double rawDist = haversineDistance(lastActive.getLatitude(), lastActive.getLongitude(), item.getLatitude(), item.getLongitude());
                    double distKm = Math.max(0.2, Math.round(rawDist * 10.0) / 10.0);
                    int transitMin = Math.max(10, Math.min(45, (int) Math.round((distKm / 25.0) * 60) + 8));
                    item.setDistanceFromPrevious(distKm);
                    item.setDurationFromPrevious(transitMin);
                    item.setEstimatedTransportCost(Math.max(40.0, Math.round(distKm * 15.0)));
                } else {
                    item.setDistanceFromPrevious(null);
                    item.setDurationFromPrevious(15);
                    item.setEstimatedTransportCost(0.0);
                }
                item.setTransportFromPrevious("Local Transit (" + lastActive.getTitle() + " → " + item.getTitle() + ")");
            }

            // Midday lunch break buffer for 4+ items
            if (j == 3 && currentClock.isBefore(LocalTime.of(14, 0)) && currentClock.isAfter(LocalTime.of(12, 15))) {
                currentClock = currentClock.plusMinutes(35);
            }

            int transit = (item.getDurationFromPrevious() != null) ? item.getDurationFromPrevious() : 15;
            LocalTime activityStart = currentClock.plusMinutes(transit);
            LocalTime activityEnd = activityStart.plusMinutes(durMinutes);

            item.setStartTime(activityStart.format(TIME_FORMATTER));
            item.setEndTime(activityEnd.format(TIME_FORMATTER));
            currentClock = activityEnd;
            lastActive = item;
        }
    }
}
