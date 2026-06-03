// Paketdeklaration för message-tjänstens kontroller
package org.example.message;

// RabbitTemplate används för att publicera meddelanden till RabbitMQ
import org.springframework.amqp.rabbit.core.RabbitTemplate;
// @Value för att hämta MQ-konfiguration från properties
import org.springframework.beans.factory.annotation.Value;
// HTTP-status och svarstyp
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// REST-annoteringar
import org.springframework.web.bind.annotation.*;

// Tidsstämpel för när meddelandet skapades
import java.time.Instant;
// Samlingar för lagring och sortering
import java.util.*;
// Trådsäker in‑memory‑lagring för meddelanden
import java.util.concurrent.ConcurrentHashMap;

// Markerar denna klass som en REST-kontroller
@RestController
// Bas-URL för meddelande-endpoints
@RequestMapping("/messages")
public class MessageController {

    // In‑memory‑butik med meddelanden, nycklade på ID
    private final Map<String, Message> store = new ConcurrentHashMap<>();
    // RabbitTemplate för att skicka MQ‑händelser
    private final RabbitTemplate rabbitTemplate;

    // Namn på MQ‑exchange (default: chat.exchange)
    @Value("${app.mq.exchange:chat.exchange}")
    private String exchange;

    // Routing‑nyckel för publicering (default: message.published)
    @Value("${app.mq.routing-key:message.published}")
    private String routingKey;

    // Konstruktorinjektion av RabbitTemplate
    public MessageController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // Skapar ett nytt meddelande och försöker publicera en MQ‑händelse
    @PostMapping
    public ResponseEntity<Message> publish(@RequestBody PublishRequest req) {
        // Generera unikt ID för meddelandet
        String id = UUID.randomUUID().toString();
        // Bygg meddelandeobjekt med nuvarande tidsstämpel
        Message msg = new Message(id, req.senderId, req.text, Instant.now());
        // Spara i in‑memory‑butiken
        store.put(id, msg);
        // Bygg enkelt JSON‑payload för MQ‑händelsen
        String payload = "{\"type\":\"message-published\",\"id\":\"" + id + "\",\"senderId\":\"" + safe(req.senderId) + "\"}";
        try {
            // Försök publicera till exchange med vald routing‑nyckel
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } catch (Exception e) {
            // Logga varning men blockera inte API‑svaret
            System.err.println("[WARN] Failed to publish MQ event: " + e.getMessage());
        }
        // Returnera 201 Created med skapat meddelande
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    // Returnerar alla meddelanden sorterade på tid
    @GetMapping
    public List<Message> list() {
        // Skapa en lista från butiken
        ArrayList<Message> list = new ArrayList<>(store.values());
        // Sortera i stigande ordning efter timestamp
        list.sort(Comparator.comparing(m -> m.timestamp));
        // Returnera listan
        return list;
    }

    // Hämtar ett specifikt meddelande via ID
    @GetMapping("/{id}")
    public ResponseEntity<Message> get(@PathVariable String id) {
        // Slå upp i butiken
        Message m = store.get(id);
        // 404 om det inte finns
        if (m == null) return ResponseEntity.notFound().build();
        // 200 OK med meddelandet
        return ResponseEntity.ok(m);
    }

    // DTO för inkommande publiceringsbegäran
    static class PublishRequest {
        public String senderId;
        public String text;
    }

    // Modell som representerar ett meddelande
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

    // Enkel escaping för att undvika trasig JSON i vårt manuellt byggda payload
    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
