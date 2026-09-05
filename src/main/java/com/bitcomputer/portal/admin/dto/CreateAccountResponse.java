package com.bitcomputer.portal.admin.dto;

public class CreateAccountResponse {
    private final String loginId;
    private final String tempPassword;

    public CreateAccountResponse(String loginId, String tempPassword) {
        this.loginId = loginId;
        this.tempPassword = tempPassword;
    }

    public String getLoginId() { return loginId; }
    public String getTempPassword() { return tempPassword; }
}
