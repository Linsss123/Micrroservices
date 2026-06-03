package org.example.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
/**
 * User-tjänstens instegspunkt.
 *
 * Denna klass startar Spring Boot-konteksten och innehåller en minimal hälso‑endpoint
 * som kan användas av t.ex. orkestrering eller lastbalanserare för att kontrollera liveness.
 */
public class UserApplication {
    /** Startar User-tjänsten. */
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
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
