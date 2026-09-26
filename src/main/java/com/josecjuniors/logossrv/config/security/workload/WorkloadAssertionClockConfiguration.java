package com.josecjuniors.logossrv.config.security.workload;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class WorkloadAssertionClockConfiguration {
    @Bean
    public Clock workloadAssertionClock() {
        return Clock.systemUTC();
    }
}
