package com.josecjuniors.logossrv.core.security.workload.application;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionFailureReason.*;

/** Bounded, untrusted compact-JWS structural parser. It performs no trust or crypto decision. */
@Component
public class WorkloadAssertionPreParser {
    public static final int MAX_HEADER_BYTES = 1024;
    public static final int MAX_PAYLOAD_BYTES = 2048;
    public static final int REQUIRED_ES256_SIGNATURE_BYTES = 64;
    private static final Pattern BASE64_URL = Pattern.compile("[A-Za-z0-9_-]+");
    private static final ObjectMapper UNTRUSTED_JSON = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(4)
                    .maxStringLength(MAX_PAYLOAD_BYTES)
                    .maxNumberLength(128)
                    .maxNameLength(64)
                    .build())
            .build()).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public UntrustedWorkloadAssertionHints parse(String compactAssertion) {
        if (compactAssertion == null || compactAssertion.isBlank()
                || compactAssertion.length() > WorkloadAssertionProfile.MAX_COMPACT_CHARACTERS
                || !isAscii(compactAssertion)) {
            throw failure(MALFORMED_ASSERTION);
        }

        String[] segments = compactAssertion.split("\\.", -1);
        if (segments.length != 3 || !BASE64_URL.matcher(segments[0]).matches()
                || !BASE64_URL.matcher(segments[1]).matches()
                || !BASE64_URL.matcher(segments[2]).matches()) {
            throw failure(MALFORMED_ASSERTION);
        }

        byte[] headerBytes = decodeCanonical(segments[0]);
        byte[] payloadBytes = decodeCanonical(segments[1]);
        byte[] signature = decodeCanonical(segments[2]);
        if (headerBytes.length > MAX_HEADER_BYTES || payloadBytes.length > MAX_PAYLOAD_BYTES
                || signature.length != REQUIRED_ES256_SIGNATURE_BYTES) {
            throw failure(MALFORMED_ASSERTION);
        }

        JsonNode header = parseObject(headerBytes);
        JsonNode payload = parseObject(payloadBytes);
        validateHeader(header);
        validatePayloadShape(payload);

        String candidateIssuer = textual(payload, "iss", ISSUER_INVALID);
        String candidateKid = textual(header, "kid", INVALID_KID);
        try {
            return new UntrustedWorkloadAssertionHints(new WorkloadIssuer(candidateIssuer),
                    new WorkloadKeyId(candidateKid));
        } catch (IllegalArgumentException exception) {
            throw new WorkloadAssertionVerificationException(
                    candidateIssuer == null || candidateIssuer.isBlank() || candidateIssuer.length() > 255
                            ? ISSUER_INVALID : INVALID_KID);
        }
    }

    private static void validateHeader(JsonNode header) {
        Set<String> names = fieldNames(header);
        if (!WorkloadAssertionProfile.HEADER_FIELDS.containsAll(names)) {
            throw failure(MALFORMED_ASSERTION);
        }
        String algorithm = textual(header, "alg", WRONG_ALGORITHM);
        if (!WorkloadAssertionProfile.ALGORITHM.equals(algorithm)) {
            throw failure(WRONG_ALGORITHM);
        }
        String type = textual(header, "typ", WRONG_TYPE);
        if (!WorkloadAssertionProfile.TYPE.equals(type)) {
            throw failure(WRONG_TYPE);
        }
        String kid = textual(header, "kid", INVALID_KID);
        try {
            new WorkloadKeyId(kid);
        } catch (IllegalArgumentException exception) {
            throw failure(INVALID_KID);
        }
        if (!names.equals(WorkloadAssertionProfile.HEADER_FIELDS)) throw failure(MALFORMED_ASSERTION);
    }

    private static void validatePayloadShape(JsonNode payload) {
        Set<String> fields = fieldNames(payload);
        if (!WorkloadAssertionProfile.CLAIM_FIELDS.containsAll(fields)) {
            throw failure(MALFORMED_ASSERTION);
        }
        requireClaim(payload, "iss", JsonNode::isTextual, ISSUER_INVALID);
        requireClaim(payload, "sub", JsonNode::isTextual, SUBJECT_INVALID);
        JsonNode audience = payload.get("aud");
        if (audience == null || !(audience.isTextual() || isStringArray(audience))) {
            throw failure(AUDIENCE_INVALID);
        }
        requireClaim(payload, "iat", JsonNode::isNumber, IAT_INVALID);
        requireClaim(payload, "exp", JsonNode::isNumber, EXP_INVALID);
        requireClaim(payload, "jti", JsonNode::isTextual, JTI_INVALID);
    }

    private static boolean isStringArray(JsonNode node) {
        if (!node.isArray()) return false;
        for (JsonNode item : node) {
            if (!item.isTextual()) return false;
        }
        return true;
    }

    private static void requireClaim(JsonNode object, String name,
                                     java.util.function.Predicate<JsonNode> type,
                                     WorkloadAssertionFailureReason reason) {
        JsonNode value = object.get(name);
        if (value == null || !type.test(value)) throw failure(reason);
    }

    private static String textual(JsonNode object, String name, WorkloadAssertionFailureReason reason) {
        JsonNode value = object.get(name);
        if (value == null || !value.isTextual()) throw failure(reason);
        return value.textValue();
    }

    private static JsonNode parseObject(byte[] json) {
        try {
            JsonNode node = UNTRUSTED_JSON.readTree(json);
            if (node == null || !node.isObject()) throw failure(MALFORMED_ASSERTION);
            return node;
        } catch (WorkloadAssertionVerificationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw failure(MALFORMED_ASSERTION);
        }
    }

    private static Set<String> fieldNames(JsonNode object) {
        java.util.HashSet<String> names = new java.util.HashSet<>();
        Iterator<String> fields = object.fieldNames();
        fields.forEachRemaining(names::add);
        return names;
    }

    private static byte[] decodeCanonical(String segment) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(segment);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(decoded);
            if (!encoded.equals(segment)) throw failure(MALFORMED_ASSERTION);
            return decoded;
        } catch (WorkloadAssertionVerificationException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw failure(MALFORMED_ASSERTION);
        }
    }

    private static boolean isAscii(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 0x7f) return false;
        }
        return true;
    }

    private static WorkloadAssertionVerificationException failure(WorkloadAssertionFailureReason reason) {
        return new WorkloadAssertionVerificationException(reason);
    }
}
