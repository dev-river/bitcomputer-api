package com.bitcomputer.portal.auth.dto;

import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {
    private String currentPassword;
    @Size(min = 8, max = 100)
    private String newPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
