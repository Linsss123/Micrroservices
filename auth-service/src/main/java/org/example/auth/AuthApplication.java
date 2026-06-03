// Paketdeklaration för auth-tjänsten
package org.example.auth;

// JWT-bibliotek för att skapa signerade tokens
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
// Spring Boot-start och konfiguration
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
// HTTP-svarstyper
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// Enkel Security-konfiguration för att tillåta allt
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
// REST-annoteringar
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Hemlig nyckeltyp
import javax.crypto.SecretKey;
// Tidsstämplar för JWT iat/exp
import java.time.Instant;
// Datum-API för JWT
import java.util.Date;
// Map används för felrespons
import java.util.Map;

// Markerar denna klass som en Spring Boot-applikation
@SpringBootApplication
public class AuthApplication {
    // Startar auth-applikationen
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    // Enkel säkerhetskonfiguration: tillåt alla anrop (endast /login exponeras)
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Stäng av CSRF-skyddet
        http.csrf(csrf -> csrf.disable());
        // Tillåt alla requests utan autentisering
        http.authorizeHttpRequests(reg -> reg.anyRequest().permitAll());
        // Kör stateless för att undvika browserns Basic Auth-dialoger
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        // Bygg och returnera säkerhetskedjan
        return http.build();
    }

    // REST-kontroller som hanterar inloggning och utfärdar JWT
    @RestController
    static class LoginController {
        // OBS: Endast för demo. Ersätts med hemlighet från miljö/konfig i produktion.
        private static final String SECRET_BASE64 =
                // 256-bitars nyckel i base64 (slumpad för demo)
                "u6N2m0m3p7oW2xkq1N2c4V7y9B3d8F1h2J4l6O8q0R2t4W6y8A0C2E4G6I8K0M2";

        // Bygger hemlig HMAC-nyckel från base64-sträng
        private SecretKey key() {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_BASE64));
        }

        // Endpoint för inloggning som returnerar en JWT vid lyckad validering
        @PostMapping("/login")
        public ResponseEntity<?> login(@RequestBody LoginRequest request) {
            // Mycket enkel demo-validering. Ersätt med riktig användaruppslagning och lösenordskontroll.
            if (request == null || request.username == null || request.username.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }

            // Använd användarnamnet som subject/uid i token
            String userId = request.username.trim();
            // Nuvarande tid för iat och exp
            Instant now = Instant.now();
            // Bygg JWT med subject, iat, exp och en enkel claim
            String token = Jwts.builder()
                    .setSubject(userId)
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(now.plusSeconds(3600)))
                    .claim("uid", userId)
                    .signWith(key(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                    .compact();

            // Returnera token i ett svar-objekt
            return ResponseEntity.ok(new LoginResponse(token));
        }

        // Request-DTO för inloggning
        static class LoginRequest {
            public String username;
            public String password;
        }

        // Response-DTO som innehåller JWT-token
        static class LoginResponse {
            public final String token;

            public LoginResponse(String token) {
                this.token = token;
            }
        }
    }
}
