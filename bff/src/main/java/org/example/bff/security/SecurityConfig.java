// Paketdeklaration för säkerhetskonfigurationen i BFF
package org.example.bff.security;

// JWT-typer: claims, parser och nyckelhantering
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
// Servlet-API för filterkedjan
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// Spring-konfiguration och bean-definitioner
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// HTTP-headerkonstanter
import org.springframework.http.HttpHeaders;
// Spring Security-typer för autentisering och auktoriteter
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
// Basfilter som garanterar ett anrop per request
import org.springframework.web.filter.OncePerRequestFilter;
// Sessionspolicy för stateless säkerhet
import org.springframework.security.config.http.SessionCreationPolicy;

// Hemlig nyckel för HMAC-signering
import javax.crypto.SecretKey;
// IO-undantag från filterkedjan
import java.io.IOException;
// Hjälper oss att hantera avsaknad av header
import java.util.Optional;

// Markerar att denna klass innehåller Spring-konfiguration
@Configuration
public class SecurityConfig {

    // OBS: Måste matcha demonyckeln som används i auth-service
    private static final String SECRET_BASE64 =
            "u6N2m0m3p7oW2xkq1N2c4V7y9B3d8F1h2J4l6O8q0R2t4W6y8A0C2E4G6I8K0M2";

    // Bygger SecretKey från Base64-kodad sträng
    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_BASE64));
    }

    // Definierar säkerhetsfilterkedjan för BFF
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Stänger av CSRF-skydd (API-klienter förlitar sig på JWT här)
        http.csrf(csrf -> csrf.disable());
        // Tillåter vissa öppna endpoints och kräver auth för resten
        http.authorizeHttpRequests(reg -> reg
                // Öppna hälsa, inloggning och statiska resurser (index.html m.m.)
                .requestMatchers(
                        "/health",
                        "/api/login",
                        "/",              // serverar index.html
                        "/index.html",
                        "/favicon.ico",
                        "/static/**"
                ).permitAll()
                .anyRequest().authenticated()
        );
        // Kör helt stateless och utan Basic Auth för att undvika webbläsarens auth‑utmaningar
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        // Lägger in vårt JWT-filter före BasicAuthenticationFilter
        http.addFilterBefore(new JwtFilter(), BasicAuthenticationFilter.class);
        // Bygger och returnerar filterkedjan
        return http.build();
    }

    // Eget filter som plockar ut och validerar JWT från Authorization-headern
    class JwtFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
            // Hämtar Authorization-headern
            String authHeader = req.getHeader(HttpHeaders.AUTHORIZATION);
            // Extraherar token om headern börjar med "Bearer "
            Optional<String> bearer = Optional.ofNullable(authHeader)
                    .filter(h -> h.startsWith("Bearer "))
                    .map(h -> h.substring(7));

            // Om ett bearer-token finns, validera och sätt authentication
            if (bearer.isPresent()) {
                try {
                    // Parsar och verifierar JWT-signaturen
                    Claims claims = Jwts.parserBuilder()
                            .setSigningKey(key())
                            .build()
                            .parseClaimsJws(bearer.get())
                            .getBody();
                    // Hämtar subject (t.ex. användar-ID)
                    String subject = claims.getSubject();
                    // Skapar en autentisering och placerar i SecurityContext
                    Authentication auth = new JwtAuthToken(subject);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception e) {
                    // Vid fel i token: svara 401 och avbryt kedjan
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().write("Invalid token");
                    return;
                }
            }

            // Fortsätt filterkedjan
            chain.doFilter(req, res);
        }
    }

    // Enkel Authentication-implementering som bär användarens identitet
    static class JwtAuthToken extends AbstractAuthenticationToken {
        // Principal (t.ex. användar-ID eller namn)
        private final String principal;

        // Initierar med en standardroll och markerar som autentiserad
        JwtAuthToken(String principal) {
            super(AuthorityUtils.createAuthorityList("ROLE_USER"));
            this.principal = principal;
            setAuthenticated(true);
        }

        // Inga separata credentials för JWT-bärare här
        @Override
        public Object getCredentials() {
            return "";
        }

        // Returnerar den autentiserade principalen
        @Override
        public Object getPrincipal() {
            return principal;
        }
    }
}
