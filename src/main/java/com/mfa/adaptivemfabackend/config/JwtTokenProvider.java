package com.mfa.adaptivemfabackend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:AdaptiveMFA_Secret_Key_For_Security_Token_Generation_2026_Project}")
    private String jwtSecret;

    // Thời gian hết hạn linh hoạt (ms)
    private final long TRUSTED_EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000L; // 7 ngày cho thiết bị quen
    private final long UNTRUSTED_EXPIRATION_TIME = 30 * 60 * 1000L;          // 30 phút cho thiết bị lạ

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Sinh JWT Token thực thụ với thời gian hết hạn linh hoạt theo isTrusted
     */
    public String generateToken(String username, boolean isTrusted) {
        Date now = new Date();
        long expirationMs = isTrusted ? TRUSTED_EXPIRATION_TIME : UNTRUSTED_EXPIRATION_TIME;
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("isTrusted", isTrusted)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Lấy Username từ JWT Token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey())
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * Kiểm tra JWT Token có hợp lệ và chưa hết hạn hay không
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}