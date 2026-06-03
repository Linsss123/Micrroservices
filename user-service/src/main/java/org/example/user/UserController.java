// Paketdeklaration för user-tjänstens REST-kontroller
package org.example.user;

// HTTP-statuskoder och svarstyp
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// REST-annoteringar (Controller, RequestMapping, CRUD-mappningar)
import org.springframework.web.bind.annotation.*;

// Samlingstyper för att lagra och returnera användare
import java.util.*;
// Trådsäker karta som fungerar i en enkel in‑memory‑lagring
import java.util.concurrent.ConcurrentHashMap;

// Markerar klassen som en REST-kontroller
@RestController
// Bas-URL för alla användarendpoints
@RequestMapping("/users")
public class UserController {

    // Enkel in‑memory‑butik för att lagra användare per ID
    private final Map<String, User> store = new ConcurrentHashMap<>();

    // Skapar en ny användare
    @PostMapping
    public ResponseEntity<User> create(@RequestBody CreateUserRequest req) {
        // Generera ID om inget angivits
        String id = (req.id == null || req.id.isBlank()) ? UUID.randomUUID().toString() : req.id;
        // Bygg användarobjektet
        User user = new User(id, req.username, req.displayName);
        // Spara i butiken
        store.put(id, user);
        // Returnera 201 Created med användaren som body
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    // Returnerar alla användare
    @GetMapping
    public List<User> list() {
        // Skapar en lista av alla värden i kartan
        return new ArrayList<>(store.values());
    }

    // Hämtar en användare med givet ID
    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable String id) {
        // Slå upp i butiken
        User user = store.get(id);
        // 404 om ej funnen
        if (user == null) return ResponseEntity.notFound().build();
        // 200 OK med användaren
        return ResponseEntity.ok(user);
    }

    // Uppdaterar fält på en användare (partiellt, behåller befintliga värden om null)
    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable String id, @RequestBody UpdateUserRequest req) {
        // Hämta befintlig post
        User existing = store.get(id);
        // 404 om den inte finns
        if (existing == null) return ResponseEntity.notFound().build();
        // Bygg uppdaterad användare med fallback till nuvarande värden
        User updated = new User(id,
                req.username != null ? req.username : existing.username,
                req.displayName != null ? req.displayName : existing.displayName);
        // Spara den uppdaterade posten
        store.put(id, updated);
        // Returnera 200 OK med uppdaterad användare
        return ResponseEntity.ok(updated);
    }

    // Tar bort en användare
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        // Ta bort posten från butiken
        User removed = store.remove(id);
        // 404 om inget togs bort
        if (removed == null) return ResponseEntity.notFound().build();
        // 204 No Content vid lyckad radering
        return ResponseEntity.noContent().build();
    }

    // Inkommande DTO för att skapa användare
    static class CreateUserRequest {
        public String id;
        public String username;
        public String displayName;
    }

    // Inkommande DTO för att uppdatera användare
    static class UpdateUserRequest {
        public String username;
        public String displayName;
    }

    // Modell som representerar en användare
    static class User {
        public final String id;
        public final String username;
        public final String displayName;

        public User(String id, String username, String displayName) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
        }
    }
}
