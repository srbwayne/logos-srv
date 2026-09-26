package com.josecjuniors.logossrv.config.progression;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "logos.progression.subject-link")
public class ProgressionSubjectLinkProperties {
    private Duration challengeTtl = Duration.ofMinutes(10);

    public Duration getChallengeTtl() { return challengeTtl; }
    public void setChallengeTtl(Duration challengeTtl) { this.challengeTtl = challengeTtl; }
}
