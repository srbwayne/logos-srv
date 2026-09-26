package com.josecjuniors.logossrv.adapters.out.security.workload.crypto;

import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadTrustRegistryIntegrityException;
import com.josecjuniors.logossrv.support.test.WorkloadTestKeys;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpkiPemWorkloadPublicKeyParserTest {
    private final SpkiPemWorkloadPublicKeyParser parser = new SpkiPemWorkloadPublicKeyParser();

    @Test
    void acceptsEphemeralP256SpkiPublicKey() throws Exception {
        var parsed = parser.parseP256(WorkloadTestKeys.publicPem(WorkloadTestKeys.ec("secp256r1")));
        assertThat(parsed.getAlgorithm()).isEqualToIgnoringCase("EC");
        assertThat(parsed.getParams().getCurve().getField().getFieldSize()).isEqualTo(256);
        assertThat(parsed.getParams().getOrder().bitLength()).isEqualTo(256);
    }

    @Test
    void rejectsOtherCurvesKeyTypesPrivateMaterialAndMalformedPem() throws Exception {
        assertRejected(WorkloadTestKeys.publicPem(WorkloadTestKeys.ec("secp384r1")));
        assertRejected(WorkloadTestKeys.publicPem(WorkloadTestKeys.rsa()));
        assertRejected(WorkloadTestKeys.privatePem(WorkloadTestKeys.ec("secp256r1")));
        assertRejected("-----BEGIN PUBLIC KEY-----\nnot-base64!\n-----END PUBLIC KEY-----\n");
        assertRejected(WorkloadTestKeys.publicPem(WorkloadTestKeys.ec("secp256r1")) + "trailing");
    }

    @Test
    void rejectsOversizedPemBeforeParsing() throws Exception {
        String oversized = "x".repeat(SpkiPemWorkloadPublicKeyParser.MAX_PEM_CHARACTERS + 1);
        assertRejected(oversized);
    }

    private void assertRejected(String pem) {
        assertThatThrownBy(() -> parser.parseP256(pem))
                .isInstanceOf(WorkloadTrustRegistryIntegrityException.class);
    }
}
