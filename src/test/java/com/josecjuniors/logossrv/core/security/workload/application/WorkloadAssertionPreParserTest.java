package com.josecjuniors.logossrv.core.security.workload.application;

import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionFailureReason.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkloadAssertionPreParserTest {
    private final WorkloadAssertionPreParser parser = new WorkloadAssertionPreParser();
    private static final String HEADER = "{\"alg\":\"ES256\",\"typ\":\"logos-workload+jwt\",\"kid\":\"kid-a\"}";
    private static final String PAYLOAD = "{\"iss\":\"urn:akume:workload-issuer:lifeos\",\"sub\":\"lifeos\",\"aud\":\"urn:akume:service:logos\",\"iat\":1,\"exp\":2,\"jti\":\"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa\"}";

    @Test
    void returnsOnlyCandidateIssuerAndKid() {
        var hints = parser.parse(token(HEADER, PAYLOAD, new byte[64]));
        assertThat(hints.candidateIssuer()).isEqualTo(new WorkloadIssuer("urn:akume:workload-issuer:lifeos"));
        assertThat(hints.candidateKid()).isEqualTo(new WorkloadKeyId("kid-a"));
        assertThat(hints.getClass().getRecordComponents()).extracting("name").containsExactly("candidateIssuer", "candidateKid");
    }

    @Test
    void rejectsMalformedCompactShapeAndEncoding() {
        for (String token : new String[]{null, " ", "a.b", "a.b.c.d", ".b.c", "a..c", "a.b.", "a=.b.c",
                "a+b.c.d", "a/b.c.d", "a b.c.d", "é.b.c", "a.b.c="}) {
            assertReason(token, MALFORMED_ASSERTION);
        }
        assertReason("a".repeat(WorkloadAssertionProfile.MAX_COMPACT_CHARACTERS + 1), MALFORMED_ASSERTION);
    }

    @Test
    void rejectsDecodedSizeAndSignatureSize() {
        assertReason(token("x".repeat(1025), PAYLOAD, new byte[64]), MALFORMED_ASSERTION);
        assertReason(token(HEADER, "x".repeat(2049), new byte[64]), MALFORMED_ASSERTION);
        assertReason(token(HEADER, PAYLOAD, new byte[63]), MALFORMED_ASSERTION);
        assertReason(token(HEADER, PAYLOAD, new byte[65]), MALFORMED_ASSERTION);
    }

    @Test
    void rejectsDuplicateFieldsUnknownHeadersAndUnknownClaims() {
        assertReason(token("{\"alg\":\"ES256\",\"alg\":\"ES256\",\"typ\":\"logos-workload+jwt\",\"kid\":\"kid-a\"}", PAYLOAD, new byte[64]), MALFORMED_ASSERTION);
        assertReason(token(HEADER, PAYLOAD.replace("\"sub\":\"lifeos\"", "\"sub\":\"lifeos\",\"sub\":\"lifeos\""), new byte[64]), MALFORMED_ASSERTION);
        for (String extra : new String[]{"\"jku\":\"https://evil.invalid/key\"", "\"x5u\":\"x\"", "\"jwk\":{}",
                "\"x5c\":[]", "\"crit\":[]", "\"custom\":true"}) {
            assertReason(token(HEADER.substring(0, HEADER.length() - 1) + "," + extra + "}", PAYLOAD, new byte[64]), MALFORMED_ASSERTION);
        }
        assertReason(token(HEADER, PAYLOAD.substring(0, PAYLOAD.length() - 1) + ",\"extra\":1}", new byte[64]), MALFORMED_ASSERTION);
        assertReason(token(HEADER, PAYLOAD.replace("\"sub\":\"lifeos\"", "\"sub\":{}"), new byte[64]), SUBJECT_INVALID);
        assertReason(token(HEADER + "{}", PAYLOAD, new byte[64]), MALFORMED_ASSERTION);
        assertReason(token(HEADER, PAYLOAD.replace("\"aud\":\"urn:akume:service:logos\"", "\"aud\":[\"urn:akume:service:logos\",1]"), new byte[64]), AUDIENCE_INVALID);
    }

    @Test
    void rejectsWrongProfileAlgorithmTypeAndKid() {
        assertReason(token(HEADER.replace("ES256", "HS256"), PAYLOAD, new byte[64]), WRONG_ALGORITHM);
        assertReason(token(HEADER.replace("logos-workload+jwt", "JWT"), PAYLOAD, new byte[64]), WRONG_TYPE);
        assertReason(token(HEADER.replace("kid-a", "bad/kid"), PAYLOAD, new byte[64]), INVALID_KID);
        assertReason(token("{\"alg\":\"ES256\",\"typ\":\"logos-workload+jwt\"}", PAYLOAD, new byte[64]), INVALID_KID);
    }

    private void assertReason(String token, WorkloadAssertionFailureReason expected) {
        assertThatThrownBy(() -> parser.parse(token)).isInstanceOf(WorkloadAssertionVerificationException.class)
                .extracting(e -> ((WorkloadAssertionVerificationException) e).reason()).isEqualTo(expected);
    }

    private static String token(String header, String payload, byte[] signature) {
        return encode(header.getBytes(StandardCharsets.UTF_8)) + "." + encode(payload.getBytes(StandardCharsets.UTF_8))
                + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }
    private static String encode(byte[] bytes) { return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
}
