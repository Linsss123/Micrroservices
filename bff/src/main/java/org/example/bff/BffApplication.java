package org.example.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
/**
 * BFF-applikationens instegspunkt.
 *
 * Innehåller endast bootstrapping av Spring Boot och en enkel hälsokontroll för driftstest.
 */
public class BffApplication {
    /**
     * Startar Spring Boot-applikationen.
     */
    public static void main(String[] args) {
        SpringApplication.run(BffApplication.class, args);
    }

    @RestController
    /**
     * Minimal hälso-endpoint för att verifiera att processen är igång.
     */
    static class HealthController {
        /**
         * Returnerar en enkel strängindikator.
         *
         * @return "OK" vid fungerande tjänst
         */
        @GetMapping("/health")
        public String health() {
            return "OK";
        }
    }
}
