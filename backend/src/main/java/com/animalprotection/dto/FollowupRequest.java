package com.animalprotection.dto;

public class FollowupRequest {
    private Long adoptionId;
    private String questionnaire;
    private String dueAt;

    public Long getAdoptionId() {
        return adoptionId;
    }

    public void setAdoptionId(Long adoptionId) {
        this.adoptionId = adoptionId;
    }

    public String getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(String questionnaire) {
        this.questionnaire = questionnaire;
    }

    public String getDueAt() {
        return dueAt;
    }

    public void setDueAt(String dueAt) {
        this.dueAt = dueAt;
    }
}
