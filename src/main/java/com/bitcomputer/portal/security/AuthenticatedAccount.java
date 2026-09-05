package com.bitcomputer.portal.security;

public record AuthenticatedAccount(int accountId, String role, Integer employeeId) {}
