package com.bitcomputer.portal.bgc.dto;

public class BgcCreateResponse {
    private String checkId;
    private String employeeId;
    private String status;
    private String createdAt;
    private Integer estimatedCompletionSeconds;
    private String message;

    public String getCheckId() { return checkId; }
    public void setCheckId(String checkId) { this.checkId = checkId; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public Integer getEstimatedCompletionSeconds() { return estimatedCompletionSeconds; }
    public void setEstimatedCompletionSeconds(Integer estimatedCompletionSeconds) { this.estimatedCompletionSeconds = estimatedCompletionSeconds; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
