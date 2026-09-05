package com.bitcomputer.portal.admin.dto;

import com.bitcomputer.portal.bgc.BackgroundCheck;

import java.time.LocalDateTime;

public class BackgroundCheckHistoryResponse {
    private Integer id;
    private String status;
    private String criminalRecord;
    private String educationVerified;
    private String employmentVerified;
    private String creditScore;
    private String errorMessage;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime maskedAt;
    private Integer triggeredByAccountId;

    public static BackgroundCheckHistoryResponse from(BackgroundCheck c) {
        BackgroundCheckHistoryResponse dto = new BackgroundCheckHistoryResponse();
        dto.id = c.getId();
        dto.status = c.getStatus();
        dto.criminalRecord = c.getCriminalRecord();
        dto.educationVerified = c.getEducationVerified();
        dto.employmentVerified = c.getEmploymentVerified();
        dto.creditScore = c.getCreditScore();
        dto.errorMessage = c.getErrorMessage();
        dto.requestedAt = c.getRequestedAt();
        dto.completedAt = c.getCompletedAt();
        dto.maskedAt = c.getMaskedAt();
        dto.triggeredByAccountId = c.getTriggeredByAccountId();
        return dto;
    }

    public Integer getId() { return id; }
    public String getStatus() { return status; }
    public String getCriminalRecord() { return criminalRecord; }
    public String getEducationVerified() { return educationVerified; }
    public String getEmploymentVerified() { return employmentVerified; }
    public String getCreditScore() { return creditScore; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getMaskedAt() { return maskedAt; }
    public Integer getTriggeredByAccountId() { return triggeredByAccountId; }
}
