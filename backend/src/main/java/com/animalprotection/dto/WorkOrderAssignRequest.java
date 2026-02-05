package com.animalprotection.dto;

public class WorkOrderAssignRequest {
    private Long assigneeUserId;

    public Long getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(Long assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }
}
