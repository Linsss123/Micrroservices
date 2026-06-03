// Paketdeklaration för BFF-applikationen
package org.example.bff;

// Importerar Spring Boot-startklassen
import org.springframework.boot.SpringApplication;
// Importerar annotering för att aktivera auto-konfiguration och komponentscanning
import org.springframework.boot.autoconfigure.SpringBootApplication;
// Importerar annotering för GET-endpoint
import org.springframework.web.bind.annotation.GetMapping;
// Importerar annotering för REST-kontroller
import org.springframework.web.bind.annotation.RestController;

// Markerar denna klass som en Spring Boot-applikation
@SpringBootApplication
public class BffApplication {
    // Huvudmetod som startar Spring Boot-applikationen
    public static void main(String[] args) {
        // Kör applikationen med BffApplication som konfiguration
        SpringApplication.run(BffApplication.class, args);
    }

    // Enkel hälso-kontroller för att verifiera att applikationen är igång
    @RestController
    static class HealthController {
        // Exponerar /health som returnerar "OK"
        @GetMapping("/health")
        public String health() {
            // Returnerar en fast sträng "OK"
            return "OK";
        }
    }
}
