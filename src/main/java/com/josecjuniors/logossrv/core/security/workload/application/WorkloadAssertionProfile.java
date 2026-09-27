package com.josecjuniors.logossrv.core.security.workload.application;

import java.time.Duration;
import java.util.Set;

public final class WorkloadAssertionProfile {
    public static final String TYPE = "logos-workload+jwt";
    public static final String ALGORITHM = "ES256";
    public static final String ISSUER = "urn:akume:workload-issuer:lifeos";
    public static final String SUBJECT = "lifeos";
    public static final String AUDIENCE = "urn:akume:service:logos";
    public static final Set<String> HEADER_FIELDS = Set.of("alg", "typ", "kid");
    public static final Set<String> CLAIM_FIELDS = Set.of("iss", "sub", "aud", "iat", "exp", "jti");
    public static final Duration MAX_TTL = Duration.ofSeconds(60);
    public static final Duration CLOCK_SKEW = Duration.ofSeconds(15);
    public static final Duration MAX_AGE = MAX_TTL.plus(CLOCK_SKEW);
    public static final int MAX_COMPACT_CHARACTERS = 8185;

    private WorkloadAssertionProfile() {
    }
}
