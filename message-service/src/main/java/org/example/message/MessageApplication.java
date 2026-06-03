// Paketdeklaration för message-tjänsten
package org.example.message;

// Spring Boot-start och auto-konfiguration
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// REST-annoteringar
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Markerar denna klass som en Spring Boot-applikation
@SpringBootApplication
public class MessageApplication {
    // Huvudmetod som startar message-applikationen
    public static void main(String[] args) {
        SpringApplication.run(MessageApplication.class, args);
    }

    // Enkel hälso-kontroller för att verifiera drift
    @RestController
    static class HealthController {
        // Returnerar "OK" på /health
        @GetMapping("/health")
        public String health() {
            return "OK";
        }
    }
}
