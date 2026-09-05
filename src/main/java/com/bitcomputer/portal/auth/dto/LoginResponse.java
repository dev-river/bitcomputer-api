package com.bitcomputer.portal.auth.dto;

public class LoginResponse {
    private final String token;
    private final boolean mustChangePassword;

    public LoginResponse(String token, boolean mustChangePassword) {
        this.token = token;
        this.mustChangePassword = mustChangePassword;
    }

    public String getToken() { return token; }
    public boolean isMustChangePassword() { return mustChangePassword; }
}
