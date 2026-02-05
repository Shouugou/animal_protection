package com.animalprotection.dto;

public class AdoptionRequest {
    private Long animalId;
    private String applyForm;

    public Long getAnimalId() {
        return animalId;
    }

    public void setAnimalId(Long animalId) {
        this.animalId = animalId;
    }

    public String getApplyForm() {
        return applyForm;
    }

    public void setApplyForm(String applyForm) {
        this.applyForm = applyForm;
    }
}
