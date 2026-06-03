package org.example.message;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
/**
 * Message-tjänstens instegspunkt.
 *
 * Startar Spring Boot-konteksten och exponerar en minimal hälso‑endpoint för driftkontroll.
 */
public class MessageApplication {
    /** Startar Message-tjänsten. */
    public static void main(String[] args) {
        SpringApplication.run(MessageApplication.class, args);
    }

    @RestController
    /**
     * Enkel hälso‑kontroller.
     */
    static class HealthController {
        /**
         * Exponerar /health.
         * @return "OK" om tjänsten är uppe
         */
        @GetMapping("/health")
        public String health() {
            return "OK";
        }
    }
}
