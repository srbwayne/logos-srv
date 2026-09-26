package com.josecjuniors.logossrv.core.security.workload.domain;

import java.util.regex.Pattern;

public record WorkloadKeyId(String value) {
    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    public WorkloadKeyId {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Workload key id has invalid format");
        }
    }
}
