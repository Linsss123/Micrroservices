package org.example.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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

    private final UserRepository repo;

    public UserController(UserRepository repo) {
        this.repo = repo;
    }

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
        UserEntity entity = new UserEntity(id, req.username, req.password);
        UserEntity saved = repo.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserView.from(saved));
    }

    @GetMapping
    /**
     * Lista alla användare.
     *
     * @return en ny lista med alla användare utan lösenord
     */
    public List<UserView> list() {
        List<UserView> out = new ArrayList<>();
        for (UserEntity u : repo.findAll()) out.add(UserView.from(u));
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
        return repo.findById(id)
                .map(UserView::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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
        return repo.findById(id)
                .map(e -> {
                    if (req.username != null) e.setUsername(req.username);
                    if (req.password != null) e.setPassword(req.password);
                    return ResponseEntity.ok(UserView.from(repo.save(e)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    /**
     * Ta bort en användare.
     *
     * @param id användarens ID
     * @return 204 No Content om något togs bort, annars 404
     */
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
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

    // Intern in-memory modell ersatt av JPA-entity (UserEntity)

    /**
     * Svar-DTO utan lösenord.
     */
    static class UserView {
        public final String id;
        public final String username;

        UserView(String id, String username) {
            this.id = id; this.username = username;
        }

        static UserView from(UserEntity u) { return new UserView(u.getId(), u.getUsername()); }
    }

    @PostMapping("/verify")
    /**
     * Verifierar att kombinationen {username, password} stämmer mot lagrad användare.
     * @return 200 OK om match, annars 401 Unauthorized
     */
    public ResponseEntity<Void> verify(@RequestBody VerifyRequest req) {
        if (req == null || req.username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return repo.findByUsername(req.username)
                .filter(u -> Objects.equals(u.getPassword(), req.password))
                .map(u -> ResponseEntity.ok().<Void>build())
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    static class VerifyRequest {
        public String username;
        public String password;
    }
}
