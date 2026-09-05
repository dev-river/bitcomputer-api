package com.bitcomputer.portal.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(@Value("${jwt.secret}") String base64Secret,
                             @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(int accountId, String role, Integer employeeId) {
        Date now = new Date();
        var builder = Jwts.builder()
            .subject(String.valueOf(accountId))
            .claim("role", role)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMs))
            .signWith(key);
        if (employeeId != null) {
            builder.claim("employeeId", employeeId);
        }
        return builder.compact();
    }

    public Claims parse(String token) {
        var jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        var body = jws.getPayload();
        Integer employeeId = body.get("employeeId", Integer.class);
        return new Claims(Integer.parseInt(body.getSubject()), body.get("role", String.class), employeeId);
    }

    public record Claims(int accountId, String role, Integer employeeId) {}
}
