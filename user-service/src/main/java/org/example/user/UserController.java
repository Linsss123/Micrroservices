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
 * - Nytt: användare skapas med {username, password} och lösenord krävs för inloggning.
 * - Returnerar lämpliga HTTP‑koder: 201 vid skapande, 200 vid lyckad hämtning/uppdatering,
 *   204 vid radering och 404 när en resurs saknas.
 *
 * Säkerhetsnot:
 * - API:t returnerar aldrig lösenord i svar. Svar-DTO:n (UserView) innehåller endast id och username.
 *
 * Begränsningar:
 * - Ingen avancerad validering eller unikhetskontroll på username i denna demo.
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
     * @param req begärandekropp med valfritt id, samt obligatoriska username och password
     * @return 201 Created med den skapade användaren (utan lösenord i svaret)
     */
    public ResponseEntity<UserView> create(@RequestBody CreateUserRequest req) {
        String id = (req.id == null || req.id.isBlank()) ? UUID.randomUUID().toString() : req.id;
        User user = new User(id, req.username, req.password);
        store.put(id, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserView.from(user));
    }

    @GetMapping
    /**
     * Lista alla användare.
     *
     * @return en ny lista med alla användare utan lösenord
     */
    public List<UserView> list() {
        List<UserView> out = new ArrayList<>();
        for (User u : store.values()) out.add(UserView.from(u));
        return out;
    }

    @GetMapping("/{id}")
    /**
     * Hämta en användare via ID.
     *
     * @param id användarens unika ID
     * @return 200 OK med användaren, eller 404 om ej funnen
     */
    public ResponseEntity<UserView> get(@PathVariable String id) {
        User user = store.get(id);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(UserView.from(user));
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
    public ResponseEntity<UserView> update(@PathVariable String id, @RequestBody UpdateUserRequest req) {
        User existing = store.get(id);
        if (existing == null) return ResponseEntity.notFound().build();
        String newUsername = req.username != null ? req.username : existing.username;
        String newPassword = req.password != null ? req.password : existing.password;
        User updated = new User(id, newUsername, newPassword);
        store.put(id, updated);
        return ResponseEntity.ok(UserView.from(updated));
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
        /** Lösenord. I demo lagras det i klartext i minnet (gör inte så i produktion). */
        public String password;
    }

    static class UpdateUserRequest {
        /** Nytt användarnamn, eller null för att behålla befintligt. */
        public String username;
        /** Nytt lösenord, eller null för att behålla befintligt. */
        public String password;
    }

    static class User {
        /** Unikt ID. */
        public final String id;
        /** Användarnamn. */
        public final String username;
        /** Lösenord (klartext för enkel demo; använd hash i verkligheten). */
        public final String password;

        public User(String id, String username, String password) {
            this.id = id;
            this.username = username;
            this.password = password;
        }
    }

    /**
     * Svar-DTO utan lösenord.
     */
    static class UserView {
        public final String id;
        public final String username;

        UserView(String id, String username) {
            this.id = id; this.username = username;
        }

        static UserView from(User u) { return new UserView(u.id, u.username); }
    }

    @PostMapping("/verify")
    /**
     * Verifierar att kombinationen {username, password} stämmer mot lagrad användare.
     * @return 200 OK om match, annars 401 Unauthorized
     */
    public ResponseEntity<Void> verify(@RequestBody VerifyRequest req) {
        if (req == null || req.username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        for (User u : store.values()) {
            if (Objects.equals(u.username, req.username) && Objects.equals(u.password, req.password)) {
                return ResponseEntity.ok().build();
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    static class VerifyRequest {
        public String username;
        public String password;
    }
}
