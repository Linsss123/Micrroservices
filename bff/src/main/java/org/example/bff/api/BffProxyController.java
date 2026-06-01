package org.example.bff.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BffProxyController {

    private final RestTemplate rest = new RestTemplate();

    @Value("${services.auth.base-url:http://localhost:8081}")
    private String AUTH_BASE;
    @Value("${services.user.base-url:http://localhost:8082}")
    private String USER_BASE;
    @Value("${services.message.base-url:http://localhost:8083}")
    private String MESSAGE_BASE;

    // 1) Login (öppen endpoint)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, AUTH_BASE + "/login", null, body);
    }

    // 2) User endpoints (kräver JWT enligt SecurityConfig)
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestHeader HttpHeaders headers, @RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, USER_BASE + "/users", headers, body);
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@RequestHeader HttpHeaders headers) {
        return forward(HttpMethod.GET, USER_BASE + "/users", headers, null);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@RequestHeader HttpHeaders headers, @PathVariable String id) {
        return forward(HttpMethod.GET, USER_BASE + "/users/" + id, headers, null);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@RequestHeader HttpHeaders headers, @PathVariable String id, @RequestBody Map<String, Object> body) {
        return forward(HttpMethod.PUT, USER_BASE + "/users/" + id, headers, body);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@RequestHeader HttpHeaders headers, @PathVariable String id) {
        return forward(HttpMethod.DELETE, USER_BASE + "/users/" + id, headers, null);
    }

    // 3) Message endpoints (kräver JWT enligt SecurityConfig)
    @PostMapping("/messages")
    public ResponseEntity<?> publishMessage(@RequestHeader HttpHeaders headers, @RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, MESSAGE_BASE + "/messages", headers, body);
    }

    @GetMapping("/messages")
    public ResponseEntity<?> listMessages(@RequestHeader HttpHeaders headers) {
        return forward(HttpMethod.GET, MESSAGE_BASE + "/messages", headers, null);
    }

    @GetMapping("/messages/{id}")
    public ResponseEntity<?> getMessage(@RequestHeader HttpHeaders headers, @PathVariable String id) {
        return forward(HttpMethod.GET, MESSAGE_BASE + "/messages/" + id, headers, null);
    }

    private ResponseEntity<?> forward(HttpMethod method, String url, HttpHeaders incoming, Object body) {
        HttpHeaders outHeaders = new HttpHeaders();
        // Vidarebefordra Content-Type och ev. Authorization om den finns
        outHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (incoming != null && incoming.containsKey(HttpHeaders.AUTHORIZATION)) {
            outHeaders.set(HttpHeaders.AUTHORIZATION, incoming.getFirst(HttpHeaders.AUTHORIZATION));
        }

        HttpEntity<?> entity = new HttpEntity<>(body, outHeaders);
        ResponseEntity<byte[]> resp = rest.exchange(URI.create(url), method, entity, byte[].class);

        HttpHeaders retHeaders = new HttpHeaders();
        MediaType ct = resp.getHeaders().getContentType();
        if (ct != null) retHeaders.setContentType(ct);
        return new ResponseEntity<>(resp.getBody(), retHeaders, resp.getStatusCode());
    }
}
