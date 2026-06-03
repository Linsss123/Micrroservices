package org.example.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@SpringBootApplication
/**
 * Auth-tjänstens instegspunkt och enkla inloggnings-API.
 *
 * Översikt:
 * - Tillhandahåller ett minimalistiskt /login-endpoint som utfärdar ett signerat JWT.
 * - Tjänsten är helt öppen (permitAll) och stateless, lämpad som demo/POC.
 * - Hemlig nyckel är hårdkodad för enkelhet i demo – byt till säker konfiguration i produktion.
 */
public class AuthApplication {
    /** Startar Spring Boot-applikationen. */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    @Bean
    /**
     * Säkerhetskonfiguration: tillåter alla anrop och kör stateless.
     */
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(reg -> reg.anyRequest().permitAll());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @RestController
    /**
     * REST-kontroller för inloggning.
     *
     * Flöde för /login:
     * - Enkel validering av att användarnamn finns (demo).
     * - Bygger ett JWT med subject=användarnamn, iat/exp och en claim uid.
     * - Signerar med HMAC-SHA256 och returnerar token som JSON.
     */
    static class LoginController {
        private static final String SECRET_BASE64 =
                "u6N2m0m3p7oW2xkq1N2c4V7y9B3d8F1h2J4l6O8q0R2t4W6y8A0C2E4G6I8K0M2";

        private SecretKey key() {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_BASE64));
        }

        @PostMapping("/login")
        /**
         * Logga in och erhåll ett JWT.
         *
         * Validering: kräver att fältet username inte är tomt (demo).
         * @param request Enkel DTO med username/password
         * @return 200 OK med token vid lyckad validering, annars 401
         */
        public ResponseEntity<?> login(@RequestBody LoginRequest request) {
            if (request == null || request.username == null || request.username.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }

            String userId = request.username.trim();
            Instant now = Instant.now();
            String token = Jwts.builder()
                    .setSubject(userId)
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(now.plusSeconds(3600)))
                    .claim("uid", userId)
                    .signWith(key(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                    .compact();

            return ResponseEntity.ok(new LoginResponse(token));
        }

        static class LoginRequest {
            /** Användarnamn (krävs i denna demo). */
            public String username;
            /** Lösenord (ignoreras i denna demo). */
            public String password;
        }

        static class LoginResponse {
            /** Utfärdat JWT-token. */
            public final String token;

            public LoginResponse(String token) {
                this.token = token;
            }
        }
    }
}
