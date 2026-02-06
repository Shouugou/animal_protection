package com.animalprotection.dto;

public class LawEvidenceRequest {
    private Long workOrderId;
    private String note;
    private String address;
    private Double latitude;
    private Double longitude;
    private java.util.List<String> attachments;

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    public java.util.List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(java.util.List<String> attachments) {
        this.attachments = attachments;
    }
}
