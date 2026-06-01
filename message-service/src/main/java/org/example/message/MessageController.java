package org.example.message;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final Map<String, Message> store = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<Message> publish(@RequestBody PublishRequest req) {
        String id = UUID.randomUUID().toString();
        Message msg = new Message(id, req.senderId, req.text, Instant.now());
        store.put(id, msg);
        // Stub: publish event to MQ (here we just log to stdout for the first iteration)
        System.out.println("[EVENT] message-published: {id=" + id + ", senderId=" + req.senderId + "}");
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    @GetMapping
    public List<Message> list() {
        ArrayList<Message> list = new ArrayList<>(store.values());
        list.sort(Comparator.comparing(m -> m.timestamp));
        return list;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Message> get(@PathVariable String id) {
        Message m = store.get(id);
        if (m == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(m);
    }

    static class PublishRequest {
        public String senderId;
        public String text;
    }

    static class Message {
        public final String id;
        public final String senderId;
        public final String text;
        public final Instant timestamp;

        public Message(String id, String senderId, String text, Instant timestamp) {
            this.id = id;
            this.senderId = senderId;
            this.text = text;
            this.timestamp = timestamp;
        }
    }
}
