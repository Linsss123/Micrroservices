package org.example.bff.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api")
/**
 * BFF (Backend-for-Frontend) proxy-kontroller.
 *
 * Ansvar och design:
 * - Exponerar ett förenklat API för klienten och vidarebefordrar anrop till bakomliggande mikrotjänster
 *   (auth, user, message).
 * - Håller klienten isolerad från interna tjänsters adresser/kontrakt.
 * - Säkerhet (JWT) hanteras av Spring Security-filterkedjan; denna kontroller skickar enbart vidare
 *   inkommande Authorization-header när den finns.
 *
 * Implementationsval:
 * - Använder en enkel {@link RestTemplate} för att proxya HTTP-anrop synkront.
 * - Returnerar svar som byte[] för att vara transparent mot underliggande tjänsters innehåll/Content-Type.
 * - Bas-URL:er hämtas från properties med rimliga defaults för lokal utveckling.
 */
public class BffProxyController {

    private final RestTemplate rest = new RestTemplate();

    @Value("${services.auth.base-url:http://localhost:8081}")
    private String AUTH_BASE;
    @Value("${services.user.base-url:http://localhost:8082}")
    private String USER_BASE;
    @Value("${services.message.base-url:http://localhost:8083}")
    private String MESSAGE_BASE;

    /**
     * Logga in via auth-tjänsten.
     *
     * Flöde:
     * - Tar emot JSON-kropp från klienten (t.ex. {"username","password"}).
     * - Vidarebefordrar POST till auth-service /login utan Authorization-header (öppen endpoint).
     * - Returnerar auth-tjänstens svar oförändrat (vanligtvis ett JWT-token i JSON).
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, AUTH_BASE + "/login", null, body);
    }

    /**
     * Skapa användare via user-tjänsten.
     *
     * Säkerhet:
     * - Kräver giltig Authorization: Bearer <JWT>. Filtreras/valideras i SecurityConfig.
     *
     * Forwarding:
     * - Behåller Authorization-headern till user-service.
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestHeader HttpHeaders headers, @RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, USER_BASE + "/users", headers, body);
    }

    @GetMapping("/users")
    /**
     * Lista alla användare från user-tjänsten.
     */
    public ResponseEntity<?> listUsers(@RequestHeader HttpHeaders headers) {
        return forward(HttpMethod.GET, USER_BASE + "/users", headers, null);
    }

    @GetMapping("/users/{id}")
    /**
     * Hämta en specifik användare från user-tjänsten.
     */
    public ResponseEntity<?> getUser(@RequestHeader HttpHeaders headers, @PathVariable String id) {
        return forward(HttpMethod.GET, USER_BASE + "/users/" + id, headers, null);
    }

    @PutMapping("/users/{id}")
    /**
     * Uppdatera en användare via user-tjänsten (helt eller partiellt enligt user-API).
     */
    public ResponseEntity<?> updateUser(@RequestHeader HttpHeaders headers, @PathVariable String id, @RequestBody Map<String, Object> body) {
        return forward(HttpMethod.PUT, USER_BASE + "/users/" + id, headers, body);
    }

    @DeleteMapping("/users/{id}")
    /**
     * Ta bort en användare via user-tjänsten.
     */
    public ResponseEntity<?> deleteUser(@RequestHeader HttpHeaders headers, @PathVariable String id) {
        return forward(HttpMethod.DELETE, USER_BASE + "/users/" + id, headers, null);
    }

    /**
     * Publicera ett nytt meddelande via message-tjänsten och trigga MQ-händelse.
     */
    @PostMapping("/messages")
    public ResponseEntity<?> publishMessage(@RequestHeader HttpHeaders headers, @RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, MESSAGE_BASE + "/messages", headers, body);
    }

    @GetMapping("/messages")
    /**
     * Lista alla meddelanden från message-tjänsten.
     */
    public ResponseEntity<?> listMessages(@RequestHeader HttpHeaders headers) {
        return forward(HttpMethod.GET, MESSAGE_BASE + "/messages", headers, null);
    }

    @GetMapping("/messages/{id}")
    /**
     * Hämta ett specifikt meddelande från message-tjänsten.
     */
    public ResponseEntity<?> getMessage(@RequestHeader HttpHeaders headers, @PathVariable String id) {
        return forward(HttpMethod.GET, MESSAGE_BASE + "/messages/" + id, headers, null);
    }

    /**
     * Hjälpmetod som proxar ett inkommande BFF-anrop till en underliggande tjänst och returnerar svaret oförändrat.
     *
     * Steg i grova drag:
     * 1) Bygg utgående headers (alltid JSON Content-Type, kopiera Authorization om den finns).
     * 2) Skapa en {@link HttpEntity} med ev. body.
     * 3) Utför HTTP-anrop med {@link RestTemplate#exchange} och förvänta byte[] som kropp.
     * 4) Kopiera tillbaka Content-Type från tjänstens svar och returnera samma kropp och status.
     */
    private ResponseEntity<?> forward(HttpMethod method, String url, HttpHeaders incoming, Object body) {
        // 1) Sätt utgående headers och bevara Authorization om den finns
        HttpHeaders outHeaders = new HttpHeaders();
        outHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (incoming != null && incoming.containsKey(HttpHeaders.AUTHORIZATION)) {
            outHeaders.set(HttpHeaders.AUTHORIZATION, incoming.getFirst(HttpHeaders.AUTHORIZATION));
        }

        // 2) Bygg request-entity (kropp + headers)
        HttpEntity<?> entity = new HttpEntity<>(body, outHeaders);

        // 3) Anropa underliggande tjänst och få tillbaka rå kropp som byte[]
        ResponseEntity<byte[]> resp = rest.exchange(URI.create(url), method, entity, byte[].class);

        // 4) Kopiera relevant header (Content-Type) och returnera oförändrad kropp och status
        HttpHeaders retHeaders = new HttpHeaders();
        MediaType ct = resp.getHeaders().getContentType();
        if (ct != null) retHeaders.setContentType(ct);
        return new ResponseEntity<>(resp.getBody(), retHeaders, resp.getStatusCode());
    }
}
