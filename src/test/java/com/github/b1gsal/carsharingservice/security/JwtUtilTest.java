package com.github.b1gsal.carsharingservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {
    private static final String SECRET_STRING =
            "f9020jj39fn32b8d38n3290#ld3NWDW2289(&729&FGFUV&CF*^F*&*G@(G*D2gdg29I@VD(@(D@(VD";
    private static final Long EXPIRATION = 300000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET_STRING);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION);
    }

    @Test
    @DisplayName("Generate token - valid email - should generate non-blank JWT token")
    void generateToken_validEmail_shouldGenerateToken() {
        String email = "customer@example.com";

        String token = jwtUtil.generateToken(email);

        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Is valid token - valid token - should return true")
    void isValidToken_validToken_shouldReturnTrue() {
        String email = "customer@example.com";
        String token = jwtUtil.generateToken(email);

        boolean actual = jwtUtil.isValidToken(token);

        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("Is valid token - expired token - should return false")
    void isValidToken_expiredToken_shouldReturnFalse() {
        SecretKey secret = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("customer@example.com")
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(secret)
                .compact();

        boolean actual = jwtUtil.isValidToken(expiredToken);

        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("Is valid token - token signed with different key - should return false")
    void isValidToken_differentSecret_shouldReturnFalse() {
        String anotherSecretString =
                "another_super_secret_key_that_is_at_least_32_bytes_long_123456";
        SecretKey anotherSecret = Keys.hmacShaKeyFor(
                anotherSecretString.getBytes(StandardCharsets.UTF_8));
        String tokenWithDifferentSecret = Jwts.builder()
                .subject("customer@example.com")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(anotherSecret)
                .compact();

        boolean actual = jwtUtil.isValidToken(tokenWithDifferentSecret);

        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("Is valid token - malformed token - should return false")
    void isValidToken_malformedToken_shouldReturnFalse() {
        String invalidToken = "invalid.jwt.token";

        boolean actual = jwtUtil.isValidToken(invalidToken);

        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("Get email - valid token - should return subject email")
    void getEmail_validToken_shouldReturnEmail() {
        String expectedEmail = "customer@example.com";
        String token = jwtUtil.generateToken(expectedEmail);

        String actualEmail = jwtUtil.getEmail(token);

        assertThat(actualEmail).isEqualTo(expectedEmail);
    }
}