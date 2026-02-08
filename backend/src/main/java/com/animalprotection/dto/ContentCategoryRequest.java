package com.animalprotection.dto;

public class ContentCategoryRequest {
    private Long id;
    private String name;
    private Integer sortNo;
    private Integer status;
    private Long approvalFlowId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getApprovalFlowId() {
        return approvalFlowId;
    }

    public void setApprovalFlowId(Long approvalFlowId) {
        this.approvalFlowId = approvalFlowId;
    }
}
