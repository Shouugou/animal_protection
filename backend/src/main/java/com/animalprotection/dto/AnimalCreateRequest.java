package com.animalprotection.dto;

public class AnimalCreateRequest {
    private Long rescueTaskId;
    private String species;
    private String summary;

    public Long getRescueTaskId() {
        return rescueTaskId;
    }

    public void setRescueTaskId(Long rescueTaskId) {
        this.rescueTaskId = rescueTaskId;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
