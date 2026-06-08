package org.example.message;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/messages")
/**
 * REST-API för meddelanden med enkel MQ‑publicering (RabbitMQ) och in‑memory‑lagring.
 *
 * Översikt:
 * - POST /messages: Skapa ett meddelande och försök publicera en MQ‑händelse (best effort).
 * - GET /messages: Lista alla meddelanden (sorteras i tiden i klienten vid behov).
 * - GET /messages/{id}: Hämta ett specifikt meddelande.
 *
 * Lagring och robusthet:
 * - Meddelanden lagras i en trådsäker in‑memory‑karta och överlever inte omstart.
 * - MQ‑publicering är omgiven av try/catch; ett MQ‑fel blockerar inte API‑svaret.
 *
 * Not:
 * - I det här systemet sätts vanligtvis avsändarens identitet i BFF‑lagret utifrån JWT ("senderId" = användarnamn).
 *   Klienten skickar därför endast {"text"} till BFF; Message‑tjänsten tar emot kompletta fält efter proxyn.
 */
public class MessageController {

    private final RabbitTemplate rabbitTemplate;
    private final MessageRepository repo;

    @Value("${app.mq.exchange:chat.exchange}")
    private String exchange;

    @Value("${app.mq.routing-key:message.published}")
    private String routingKey;

    /**
     * Injekterar RabbitTemplate som används för publicering till MQ.
     */
    public MessageController(RabbitTemplate rabbitTemplate, MessageRepository repo) {
        this.rabbitTemplate = rabbitTemplate;
        this.repo = repo;
    }

    @PostMapping
    /**
     * Publicera ett nytt meddelande och skicka en MQ‑händelse.
     *
     * Flöde:
     * 1) Generera nytt id och bygg ett Message-objekt med aktuell tidsstämpel.
     * 2) Spara i in‑memory‑butiken.
     * 3) Bygg ett enkelt JSON‑payload för MQ (endast nödvändiga fält).
     * 4) Försök publicera till konfigurerad exchange + routing‑nyckel.
     * 5) Returnera 201 Created med det skapade meddelandet, oavsett MQ‑resultat.
     *
     * Felhantering:
     * - Eventuella undantag vid MQ‑publicering loggas som varning och ignoreras.
     */
    public ResponseEntity<MessageEntity> publish(@RequestBody PublishRequest req) {
        // 1) Generera id och bygg modellen
        String id = UUID.randomUUID().toString();
        MessageEntity msg = new MessageEntity(id, req.senderId, req.text, Instant.now());
        // 2) Spara i databasen
        repo.save(msg);
        // 3) Bygg MQ‑payload (manuell lättvikts‑JSON)
        String payload = "{\"type\":\"message-published\",\"id\":\"" + id + "\",\"senderId\":\"" + safe(req.senderId) + "\"}";
        try {
            // 4) Publicera MQ‑händelse
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } catch (Exception e) {
            System.err.println("[WARN] Failed to publish MQ event: " + e.getMessage());
        }
        // 5) Svara klienten
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    @GetMapping
    /**
     * Lista alla meddelanden i in‑memory‑butiken.
     *
     * Not: Sorteringen lämnas till klienten i detta enkla exempel – här returneras i godtycklig ordning.
     */
    public List<MessageEntity> list() {
        return new ArrayList<>(repo.findAll());
    }

    @GetMapping("/{id}")
    /**
     * Hämta ett specifikt meddelande.
     *
     * @param id meddelandets ID
     * @return 200 OK med Message eller 404 om inte funnen
     */
    public ResponseEntity<MessageEntity> get(@PathVariable String id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    static class PublishRequest {
        /**
         * ID på avsändaren.
         *
         * I helhetslösningen sätts detta i BFF från det autentiserade användarnamnet (JWT subject/uid).
         * Fältet lämnas kvar här för tydlighet i den interna modellen.
         */
        public String senderId;
        /** Själva textinnehållet i meddelandet. */
        public String text;
    }

    /**
     * Mycket enkel escaping för att undvika trasig JSON i manuellt byggt payload.
     *
     * Ersätter backslash och dubbelfnutt med escaped varianter.
     */
    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
