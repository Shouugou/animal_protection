package com.animalprotection.dto;

public class VolunteerTaskCreateRequest {
    private String title;
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
    private String startAt;
    private String endAt;
    private Integer maxClaims;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getStartAt() {
        return startAt;
    }

    public void setStartAt(String startAt) {
        this.startAt = startAt;
    }

    public String getEndAt() {
        return endAt;
    }

    public void setEndAt(String endAt) {
        this.endAt = endAt;
    }

    public Integer getMaxClaims() {
        return maxClaims;
    }

    public void setMaxClaims(Integer maxClaims) {
        this.maxClaims = maxClaims;
    }
}
