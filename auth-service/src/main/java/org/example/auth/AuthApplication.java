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
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    // Simple security configuration: allow all requests (we only expose /login here)
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(reg -> reg.anyRequest().permitAll());
        // Ingen Basic Auth; stateless för att undvika webbläsarens auth‑utmaningar
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @RestController
    static class LoginController {
        // NOTE: For demo only. Replace with environment/config secret in production.
        private static final String SECRET_BASE64 =
                // 256-bit key in base64 (randomly generated for demo purposes)
                "u6N2m0m3p7oW2xkq1N2c4V7y9B3d8F1h2J4l6O8q0R2t4W6y8A0C2E4G6I8K0M2";

        private SecretKey key() {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_BASE64));
        }

        @PostMapping("/login")
        public ResponseEntity<?> login(@RequestBody LoginRequest request) {
            // Very basic demo validation. Replace with real user lookup & password check.
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
            public String username;
            public String password;
        }

        static class LoginResponse {
            public final String token;

            public LoginResponse(String token) {
                this.token = token;
            }
        }
    }
}
