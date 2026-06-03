package org.example.message;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
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
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.mq.exchange:chat.exchange}")
    private String exchange;

    @Value("${app.mq.routing-key:message.published}")
    private String routingKey;

    public MessageController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping
    public ResponseEntity<Message> publish(@RequestBody PublishRequest req) {
        String id = UUID.randomUUID().toString();
        Message msg = new Message(id, req.senderId, req.text, Instant.now());
        store.put(id, msg);
        // Publish event to MQ
        String payload = "{\"type\":\"message-published\",\"id\":\"" + id + "\",\"senderId\":\"" + safe(req.senderId) + "\"}";
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } catch (Exception e) {
            // Logga men låt inte publiceringen blockera svaret
            System.err.println("[WARN] Failed to publish MQ event: " + e.getMessage());
        }
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

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
