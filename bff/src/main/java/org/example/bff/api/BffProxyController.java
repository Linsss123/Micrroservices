// Paketdeklaration för BFF-API-klasser
package org.example.bff.api;

// Importerar @Value för att läsa konfigurationsegenskaper
import org.springframework.beans.factory.annotation.Value;
// Importerar Spring HTTP-typer (ResponseEntity, HttpHeaders, HttpMethod m.m.)
import org.springframework.http.*;
// Importerar Spring MVC-annoteringar för REST-endpoints
import org.springframework.web.bind.annotation.*;
// Klient för att anropa andra HTTP-tjänster
import org.springframework.web.client.RestTemplate;

// URI-klass för att bygga måladresser
import java.net.URI;
// Map används för generiska JSON-bodies
import java.util.Map;

// Markerar klassen som en REST-kontroller i Spring
@RestController
// Bas-URL för alla endpoints i denna kontroller
@RequestMapping("/api")
// Kontroller som vidarebefordrar anrop till bakomliggande mikrotjänster
public class BffProxyController {

    // HTTP-klient som används för att skicka vidare anrop
    private final RestTemplate rest = new RestTemplate();

    // Bas-URL till auth-tjänsten, med default om konfig saknas
    @Value("${services.auth.base-url:http://localhost:8081}")
    private String AUTH_BASE;
    // Bas-URL till user-tjänsten, med default
    @Value("${services.user.base-url:http://localhost:8082}")
    private String USER_BASE;
    // Bas-URL till message-tjänsten, med default
    @Value("${services.message.base-url:http://localhost:8083}")
    private String MESSAGE_BASE;

    // 1) Login (öppen endpoint)
    @PostMapping("/login")
    // Tar emot login-data och vidarebefordrar till auth-tjänsten
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        // Skickar vidare POST till /login utan inkommande headers
        return forward(HttpMethod.POST, AUTH_BASE + "/login", null, body);
    }

    // 2) User endpoints (kräver JWT enligt SecurityConfig)
    @PostMapping("/users")
    // Skapar en användare via user-tjänsten, behåller inkommande Authorization-header
    public ResponseEntity<?> createUser(@RequestHeader HttpHeaders headers, @RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, USER_BASE + "/users", headers, body);
    }

    @GetMapping("/users")
    // Hämtar lista av användare från user-tjänsten
    public ResponseEntity<?> listUsers(@RequestHeader HttpHeaders headers) {
        return forward(HttpMethod.GET, USER_BASE + "/users", headers, null);
    }

    @GetMapping("/users/{id}")
    // Hämtar en specifik användare från user-tjänsten
    public ResponseEntity<?> getUser(@RequestHeader HttpHeaders headers, @PathVariable String id) {
        return forward(HttpMethod.GET, USER_BASE + "/users/" + id, headers, null);
    }

    @PutMapping("/users/{id}")
    // Uppdaterar en användare via user-tjänsten
    public ResponseEntity<?> updateUser(@RequestHeader HttpHeaders headers, @PathVariable String id, @RequestBody Map<String, Object> body) {
        return forward(HttpMethod.PUT, USER_BASE + "/users/" + id, headers, body);
    }

    @DeleteMapping("/users/{id}")
    // Tar bort en användare via user-tjänsten
    public ResponseEntity<?> deleteUser(@RequestHeader HttpHeaders headers, @PathVariable String id) {
        return forward(HttpMethod.DELETE, USER_BASE + "/users/" + id, headers, null);
    }

    // 3) Message endpoints (kräver JWT enligt SecurityConfig)
    @PostMapping("/messages")
    // Publicerar ett meddelande via message-tjänsten
    public ResponseEntity<?> publishMessage(@RequestHeader HttpHeaders headers, @RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, MESSAGE_BASE + "/messages", headers, body);
    }

    @GetMapping("/messages")
    // Hämtar alla meddelanden från message-tjänsten
    public ResponseEntity<?> listMessages(@RequestHeader HttpHeaders headers) {
        return forward(HttpMethod.GET, MESSAGE_BASE + "/messages", headers, null);
    }

    @GetMapping("/messages/{id}")
    // Hämtar ett specifikt meddelande
    public ResponseEntity<?> getMessage(@RequestHeader HttpHeaders headers, @PathVariable String id) {
        return forward(HttpMethod.GET, MESSAGE_BASE + "/messages/" + id, headers, null);
    }

    // Hjälpmetod som skickar vidare ett inkommande BFF-anrop till underliggande tjänst
    private ResponseEntity<?> forward(HttpMethod method, String url, HttpHeaders incoming, Object body) {
        // Skapar nya headers för utgående anrop
        HttpHeaders outHeaders = new HttpHeaders();
        // Sätter Content-Type till JSON och vidarebefordrar ev. Authorization
        outHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (incoming != null && incoming.containsKey(HttpHeaders.AUTHORIZATION)) {
            outHeaders.set(HttpHeaders.AUTHORIZATION, incoming.getFirst(HttpHeaders.AUTHORIZATION));
        }

        // Bygger HTTP-entity (kropp + headers) att skicka
        HttpEntity<?> entity = new HttpEntity<>(body, outHeaders);
        // Utför HTTP-anropet och förväntar ett byte[]-svar (transparent proxy)
        ResponseEntity<byte[]> resp = rest.exchange(URI.create(url), method, entity, byte[].class);

        // Kopierar tillbaka Content-Type från svar till klienten
        HttpHeaders retHeaders = new HttpHeaders();
        MediaType ct = resp.getHeaders().getContentType();
        if (ct != null) retHeaders.setContentType(ct);
        // Returnerar svarskropp, headers och status oförändrat
        return new ResponseEntity<>(resp.getBody(), retHeaders, resp.getStatusCode());
    }
}
