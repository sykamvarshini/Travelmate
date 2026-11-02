package com.travelmate.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "attractions")
public class Attraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private AttractionCategory category;

    @Column(nullable = false, length = 150)
    private String name;

    private String subcategory;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double latitude;
    private Double longitude;

    @Column(length = 20)
    private String indoorOutdoor = "Outdoor"; // Indoor or Outdoor

    @Column(length = 30)
    private String activityIntensity = "Moderate"; // Low, Moderate, High

    @Column(length = 50)
    private String ageSuitability = "All Ages";

    private String openingTime = "09:00";
    private String closingTime = "18:00";
    private Double durationHours = 2.0;
    private Double entryFeeInr = 0.0;
    private Double rating = 4.5;
    private Integer reviewsCount = 1000;

    @Column(columnDefinition = "TEXT")
    private String tags;

    private Boolean isAvailable = true; // For closure testing
    private String closureReason;

    private Boolean featured = false;

    public Attraction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }

    public AttractionCategory getCategory() { return category; }
    public void setCategory(AttractionCategory category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getIndoorOutdoor() { return indoorOutdoor; }
    public void setIndoorOutdoor(String indoorOutdoor) { this.indoorOutdoor = indoorOutdoor; }

    public String getActivityIntensity() { return activityIntensity; }
    public void setActivityIntensity(String activityIntensity) { this.activityIntensity = activityIntensity; }

    public String getAgeSuitability() { return ageSuitability; }
    public void setAgeSuitability(String ageSuitability) { this.ageSuitability = ageSuitability; }

    public String getOpeningTime() { return openingTime; }
    public void setOpeningTime(String openingTime) { this.openingTime = openingTime; }

    public String getClosingTime() { return closingTime; }
    public void setClosingTime(String closingTime) { this.closingTime = closingTime; }

    public Double getDurationHours() { return durationHours; }
    public void setDurationHours(Double durationHours) { this.durationHours = durationHours; }

    public Double getEntryFeeInr() { return entryFeeInr; }
    public void setEntryFeeInr(Double entryFeeInr) { this.entryFeeInr = entryFeeInr; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(Integer reviewsCount) { this.reviewsCount = reviewsCount; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public String getClosureReason() { return closureReason; }
    public void setClosureReason(String closureReason) { this.closureReason = closureReason; }

    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }
}
