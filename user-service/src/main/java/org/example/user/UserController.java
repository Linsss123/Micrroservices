package org.example.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/users")
/**
 * REST-API för enkel användarhantering (in‑memory).
 *
 * Översikt:
 * - Avsett som demo/POC utan persistent datalager – allt lagras i en trådsäker karta i minnet.
 * - CRUD‑endpoints för att skapa, lista, läsa, uppdatera och radera användare.
 * - Returnerar lämpliga HTTP‑koder: 201 vid skapande, 200 vid lyckad hämtning/uppdatering,
 *   204 vid radering och 404 när en resurs saknas.
 *
 * Begränsningar:
 * - Ingen validering, auth eller unikhetskontroll på username i detta exempel.
 */
public class UserController {

    private final Map<String, User> store = new ConcurrentHashMap<>();

    @PostMapping
    /**
     * Skapa en ny användare.
     *
     * Regeln för ID:
     * - Om begäran inte innehåller ett icke‑blankt id genereras ett slumpmässigt UUID.
     *
     * @param req begärandekropp med valfritt id, samt username och displayName
     * @return 201 Created med den skapade användaren
     */
    public ResponseEntity<User> create(@RequestBody CreateUserRequest req) {
        String id = (req.id == null || req.id.isBlank()) ? UUID.randomUUID().toString() : req.id;
        User user = new User(id, req.username, req.displayName);
        store.put(id, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping
    /**
     * Lista alla användare.
     *
     * @return en ny lista med alla användarobjekt i butiken
     */
    public List<User> list() {
        return new ArrayList<>(store.values());
    }

    @GetMapping("/{id}")
    /**
     * Hämta en användare via ID.
     *
     * @param id användarens unika ID
     * @return 200 OK med användaren, eller 404 om ej funnen
     */
    public ResponseEntity<User> get(@PathVariable String id) {
        User user = store.get(id);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    /**
     * Uppdatera en användares fält.
     *
     * Partiell uppdatering:
     * - Om fält i begäran är null behålls befintliga värden.
     *
     * @param id användarens ID
     * @param req inkommande fält att uppdatera
     * @return 200 OK med uppdaterad användare, eller 404 om resurs saknas
     */
    public ResponseEntity<User> update(@PathVariable String id, @RequestBody UpdateUserRequest req) {
        User existing = store.get(id);
        if (existing == null) return ResponseEntity.notFound().build();
        User updated = new User(id,
                req.username != null ? req.username : existing.username,
                req.displayName != null ? req.displayName : existing.displayName);
        store.put(id, updated);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    /**
     * Ta bort en användare.
     *
     * @param id användarens ID
     * @return 204 No Content om något togs bort, annars 404
     */
    public ResponseEntity<Void> delete(@PathVariable String id) {
        User removed = store.remove(id);
        if (removed == null) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    static class CreateUserRequest {
        /** Valfritt. Om tomt genereras UUID. */
        public String id;
        /** Användarnamn (ingen validering i demot). */
        public String username;
        /** Visningsnamn. */
        public String displayName;
    }

    static class UpdateUserRequest {
        /** Nytt användarnamn, eller null för att behålla befintligt. */
        public String username;
        /** Nytt visningsnamn, eller null för att behålla befintligt. */
        public String displayName;
    }

    static class User {
        /** Unikt ID. */
        public final String id;
        /** Användarnamn. */
        public final String username;
        /** Visningsnamn. */
        public final String displayName;

        public User(String id, String username, String displayName) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
        }
    }
}
