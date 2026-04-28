package edu.iu.p532.rpl.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {
    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
