package com.bitcomputer.portal.bgc;

import java.time.LocalDateTime;

public class BackgroundCheck {
    private Integer id;
    private Integer employeeId;
    private String externalCheckId;
    private String status;
    private String criminalRecord;
    private String educationVerified;
    private String employmentVerified;
    private String creditScore;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime lastErrorAt;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime maskedAt;
    private Integer triggeredByAccountId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    public String getExternalCheckId() { return externalCheckId; }
    public void setExternalCheckId(String externalCheckId) { this.externalCheckId = externalCheckId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCriminalRecord() { return criminalRecord; }
    public void setCriminalRecord(String criminalRecord) { this.criminalRecord = criminalRecord; }
    public String getEducationVerified() { return educationVerified; }
    public void setEducationVerified(String educationVerified) { this.educationVerified = educationVerified; }
    public String getEmploymentVerified() { return employmentVerified; }
    public void setEmploymentVerified(String employmentVerified) { this.employmentVerified = employmentVerified; }
    public String getCreditScore() { return creditScore; }
    public void setCreditScore(String creditScore) { this.creditScore = creditScore; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getLastErrorAt() { return lastErrorAt; }
    public void setLastErrorAt(LocalDateTime lastErrorAt) { this.lastErrorAt = lastErrorAt; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getMaskedAt() { return maskedAt; }
    public void setMaskedAt(LocalDateTime maskedAt) { this.maskedAt = maskedAt; }
    public Integer getTriggeredByAccountId() { return triggeredByAccountId; }
    public void setTriggeredByAccountId(Integer triggeredByAccountId) { this.triggeredByAccountId = triggeredByAccountId; }
}
