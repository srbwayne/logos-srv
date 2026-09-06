package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class ProgressionExecutionFingerprint {
    private ProgressionExecutionFingerprint() {}

    public static String of(ProgressionExecutionIdentity identity, ExternalSubjectReference subject,
                            ExternalProgressionConfigurationReference configuration, ProgressionFact fact) {
        var value = new StringBuilder()
                .append(field(identity.source())).append(field(identity.idempotencyKey()))
                .append(field(subject.namespace())).append(field(subject.externalId()))
                .append(field(configuration.key())).append(field(String.valueOf(configuration.revision())));
        fact.details().forEach(detail -> value.append(field(detail.factorKey())).append(field(Double.toString(detail.value()))));
        return digest(value.toString());
    }

    public static String ofFrozen(ProgressionExecutionIdentity identity, ExternalSubjectReference subject,
                                  ExternalProgressionConfigurationReference configuration, ProgressionFact fact,
                                  UUID configurationVersionId, UUID skillPolicyVersionId) {
        var base = of(identity, subject, configuration, fact);
        return digest(base + field(String.valueOf(configurationVersionId))
                + field(String.valueOf(skillPolicyVersionId)));
    }

    private static String digest(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            var result = new StringBuilder(64);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String field(String value) {
        return value.length() + ":" + value;
    }
}
