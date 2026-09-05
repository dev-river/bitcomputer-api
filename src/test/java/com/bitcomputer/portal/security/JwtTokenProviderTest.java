package com.bitcomputer.portal.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    private final JwtTokenProvider provider = new JwtTokenProvider(
        "dGVzdC1qd3Qtc2VjcmV0LWtleS10ZXN0LWp3dC1zZWNyZXQta2V5", 28800000L);

    @Test
    void generateThenParse_returnsOriginalClaims() {
        String token = provider.generateToken(1, "ADMIN", null);
        JwtTokenProvider.Claims claims = provider.parse(token);

        assertThat(claims.accountId()).isEqualTo(1);
        assertThat(claims.role()).isEqualTo("ADMIN");
        assertThat(claims.employeeId()).isNull();
    }

    @Test
    void parse_employeeToken_includesEmployeeId() {
        String token = provider.generateToken(2, "EMPLOYEE", 7);
        assertThat(provider.parse(token).employeeId()).isEqualTo(7);
    }

    @Test
    void parse_tamperedToken_throws() {
        String token = provider.generateToken(1, "ADMIN", null);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");
        assertThrows(io.jsonwebtoken.JwtException.class, () -> provider.parse(tampered));
    }
}
