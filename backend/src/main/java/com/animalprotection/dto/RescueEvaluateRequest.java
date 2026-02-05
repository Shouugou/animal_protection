package com.animalprotection.dto;

public class RescueEvaluateRequest {
    private Boolean needRescue;
    private String note;

    public Boolean getNeedRescue() {
        return needRescue;
    }

    public void setNeedRescue(Boolean needRescue) {
        this.needRescue = needRescue;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
