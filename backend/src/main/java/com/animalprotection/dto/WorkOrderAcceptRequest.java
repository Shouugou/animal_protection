package com.animalprotection.dto;

public class WorkOrderAcceptRequest {
    private Boolean needLawEnforcement;
    private Boolean transferToRescue;
    private String resultText;

    public Boolean getNeedLawEnforcement() {
        return needLawEnforcement;
    }

    public void setNeedLawEnforcement(Boolean needLawEnforcement) {
        this.needLawEnforcement = needLawEnforcement;
    }

    public Boolean getTransferToRescue() {
        return transferToRescue;
    }

    public void setTransferToRescue(Boolean transferToRescue) {
        this.transferToRescue = transferToRescue;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }
}
