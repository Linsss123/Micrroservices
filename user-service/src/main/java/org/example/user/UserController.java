package org.example.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<String, User> store = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<User> create(@RequestBody CreateUserRequest req) {
        String id = (req.id == null || req.id.isBlank()) ? UUID.randomUUID().toString() : req.id;
        User user = new User(id, req.username, req.displayName);
        store.put(id, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping
    public List<User> list() {
        return new ArrayList<>(store.values());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable String id) {
        User user = store.get(id);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
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
    public ResponseEntity<Void> delete(@PathVariable String id) {
        User removed = store.remove(id);
        if (removed == null) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    static class CreateUserRequest {
        public String id;
        public String username;
        public String displayName;
    }

    static class UpdateUserRequest {
        public String username;
        public String displayName;
    }

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
