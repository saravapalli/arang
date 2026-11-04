package com.arang.intake;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Arang Producer Intake (P1) — application entry point.
 *
 * <p>See {@code docs/p1-producer-intake/sdd.md}. P1 implements the producer loop: WhatsApp/text/image
 * intake → draft → reply → token-gated review/edit/submit/delete → scheduled draft cleanup.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
public class IntakeApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntakeApplication.class, args);
    }

    /** System clock for production; tests replace this with a fixed clock (TTP-D2). */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
