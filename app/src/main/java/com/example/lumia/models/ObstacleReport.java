package com.example.lumia.models;

/**
 * Model representing an obstacle report in the COMMUNITY module.
 */
public class ObstacleReport {

    private String id;
    private String type;
    private String description;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String userId;
    private String status;
    private String imageBase64;

    /**
     * Default empty constructor required for Firebase Firestore serialization/deserialization.
     */
    public ObstacleReport() {
        this.status = "active";
    }

    /**
     * Parameterized constructor without image.
     */
    public ObstacleReport(String id, String type, String description, double latitude, double longitude, long timestamp, String userId, String status) {
        this(id, type, description, latitude, longitude, timestamp, userId, status, null);
    }

    /**
     * Full parameterized constructor including imageBase64.
     */
    public ObstacleReport(String id, String type, String description, double latitude, double longitude, long timestamp, String userId, String status, String imageBase64) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.userId = userId;
        this.status = status != null ? status : "active";
        this.imageBase64 = imageBase64;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    /**
     * Alias for imageBase64 to maintain backward compatibility if accessed via imageUrl.
     */
    public String getImageUrl() {
        return imageBase64;
    }

    /**
     * Alias for imageBase64 to maintain backward compatibility if set via imageUrl.
     */
    public void setImageUrl(String imageUrl) {
        this.imageBase64 = imageUrl;
    }
}
