package com.josecjuniors.logossrv.config.progression;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.time.Clock;

@Configuration
public class ProgressionClockConfiguration {
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock progressionClock() { return Clock.systemUTC(); }
}
