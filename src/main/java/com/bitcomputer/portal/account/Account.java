package com.bitcomputer.portal.account;

import java.time.LocalDateTime;

public class Account {
    private Integer id;
    private String loginId;
    private String passwordHash;
    private String role;
    private Integer employeeId;
    private boolean mustChangePassword;
    private Integer createdByAccountId;
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public Integer getCreatedByAccountId() { return createdByAccountId; }
    public void setCreatedByAccountId(Integer createdByAccountId) { this.createdByAccountId = createdByAccountId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
