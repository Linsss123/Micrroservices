package org.example.bff.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Optional;

@Configuration
/**
 * Säkerhetskonfiguration för BFF.
 *
 * Översikt:
 * - Stateless JWT-baserad autentisering. Inga sessioner, ingen Basic Auth.
 * - Öppnar hälsa, login och statiska resurser; kräver auth för övriga endpoints.
 * - Lägger in ett enkelt filter som validerar Bearer-token och placerar en Authentication i SecurityContext.
 *
 * Implementation:
 * - Delar hemlig HMAC-nyckel (demo) med auth-service för att validera signaturer.
 * - Minimal rollsättning: varje verifierat token får rollen ROLE_USER.
 */
public class SecurityConfig {

    private static final String SECRET_BASE64 =
            "u6N2m0m3p7oW2xkq1N2c4V7y9B3d8F1h2J4l6O8q0R2t4W6y8A0C2E4G6I8K0M2";

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_BASE64));
    }

    @Bean
    /**
     * Konfigurerar Spring Security för BFF.
     *
     * - CSRF avstängd (API + JWT).
     * - Tillåter vissa öppna endpoints och kräver auth för resten.
     * - Stateless sessions.
     * - Injekterar ett JWT-valideringsfilter före BasicAuthenticationFilter.
     */
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(reg -> reg
                .requestMatchers(
                        "/health",
                        "/api/login",
                        "/",              // serverar index.html
                        "/index.html",
                        "/favicon.ico",
                        "/static/**"
                ).permitAll()
                // Tillåt att skapa användare utan att vara inloggad, så att man kan registrera sig före login
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                .anyRequest().authenticated()
        );
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.addFilterBefore(new JwtFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }

    class JwtFilter extends OncePerRequestFilter {
        @Override
        /**
         * Extraherar och validerar Bearer-token från Authorization-headern.
         *
         * Flöde:
         * 1) Läs Authorization-headern och plocka ut token efter prefixet "Bearer ".
         * 2) Verifiera JWT-signatur och extrahera claims.
         * 3) Skapa en Authentication (JwtAuthToken) och placera i SecurityContext.
         * 4) Vid fel -> skicka 401 och avbryt kedjan.
         */
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
            String authHeader = req.getHeader(HttpHeaders.AUTHORIZATION);
            Optional<String> bearer = Optional.ofNullable(authHeader)
                    .filter(h -> h.startsWith("Bearer "))
                    .map(h -> h.substring(7));

            if (bearer.isPresent()) {
                try {
                    Claims claims = Jwts.parserBuilder()
                            .setSigningKey(key())
                            .build()
                            .parseClaimsJws(bearer.get())
                            .getBody();
                    String subject = claims.getSubject();
                    Authentication auth = new JwtAuthToken(subject);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception e) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().write("Invalid token");
                    return;
                }
            }

            chain.doFilter(req, res);
        }
    }

    static class JwtAuthToken extends AbstractAuthenticationToken {
        private final String principal;

        /**
         * Skapar ett autentiseringsobjekt för en JWT-bärare.
         * Alla användare tilldelas rollen ROLE_USER i detta exempel.
         */
        JwtAuthToken(String principal) {
            super(AuthorityUtils.createAuthorityList("ROLE_USER"));
            this.principal = principal;
            setAuthenticated(true);
        }

        @Override
        /** JWT har inga separata credentials i detta enkla exempel. */
        public Object getCredentials() {
            return "";
        }

        @Override
        /** Returnerar subject (t.ex. användar-ID) extraherat från token. */
        public Object getPrincipal() {
            return principal;
        }
    }
}
