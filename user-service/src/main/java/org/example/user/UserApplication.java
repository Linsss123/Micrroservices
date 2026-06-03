// Paketdeklaration för user-tjänsten
package org.example.user;

// Spring Boot-start och auto-konfiguration
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// REST-annoteringar
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Markerar denna klass som en Spring Boot-applikation
@SpringBootApplication
public class UserApplication {
    // Startar user-applikationen
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }

    // Enkel hälso-endpoint för statuskontroll
    @RestController
    static class HealthController {
        // Returnerar "OK" på /health
        @GetMapping("/health")
        public String health() {
            return "OK";
        }
    }
}
