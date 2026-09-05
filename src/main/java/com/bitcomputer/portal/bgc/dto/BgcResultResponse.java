package com.bitcomputer.portal.bgc.dto;

public class BgcResultResponse {
    private String checkId;
    private String employeeId;
    private String status;
    private Boolean criminalRecord;
    private Boolean educationVerified;
    private Boolean employmentVerified;
    private String creditScore;
    private String createdAt;
    private String completedAt;

    public String getCheckId() { return checkId; }
    public void setCheckId(String checkId) { this.checkId = checkId; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getCriminalRecord() { return criminalRecord; }
    public void setCriminalRecord(Boolean criminalRecord) { this.criminalRecord = criminalRecord; }
    public Boolean getEducationVerified() { return educationVerified; }
    public void setEducationVerified(Boolean educationVerified) { this.educationVerified = educationVerified; }
    public Boolean getEmploymentVerified() { return employmentVerified; }
    public void setEmploymentVerified(Boolean employmentVerified) { this.employmentVerified = employmentVerified; }
    public String getCreditScore() { return creditScore; }
    public void setCreditScore(String creditScore) { this.creditScore = creditScore; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
}
