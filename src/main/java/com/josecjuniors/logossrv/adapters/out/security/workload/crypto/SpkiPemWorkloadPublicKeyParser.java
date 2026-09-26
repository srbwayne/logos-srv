package com.josecjuniors.logossrv.adapters.out.security.workload.crypto;

import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadTrustRegistryIntegrityException;
import org.springframework.stereotype.Component;

import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SpkiPemWorkloadPublicKeyParser {
    public static final int MAX_PEM_CHARACTERS = 4096;
    private static final Pattern PEM = Pattern.compile(
            "\\A-----BEGIN PUBLIC KEY-----\\r?\\n([A-Za-z0-9+/=\\r\\n]+)\\r?\\n-----END PUBLIC KEY-----\\r?\\n?\\z");
    private static final ECParameterSpec P256 = p256Parameters();

    public ECPublicKey parseP256(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new WorkloadTrustRegistryIntegrityException("public key PEM is blank");
        }
        if (pem.length() > MAX_PEM_CHARACTERS) {
            throw new WorkloadTrustRegistryIntegrityException("public key PEM exceeds the size limit");
        }

        Matcher matcher = PEM.matcher(pem);
        if (!matcher.matches()) {
            throw new WorkloadTrustRegistryIntegrityException("public key is not a single SPKI PUBLIC KEY PEM block");
        }

        try {
            String base64 = matcher.group(1).replace("\r", "").replace("\n", "");
            byte[] der = Base64.getDecoder().decode(base64);
            if (!Base64.getEncoder().encodeToString(der).equals(base64)) {
                throw new WorkloadTrustRegistryIntegrityException("public key PEM base64 is not canonical");
            }
            var parsed = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
            if (!(parsed instanceof ECPublicKey ecPublicKey)) {
                throw new WorkloadTrustRegistryIntegrityException("public key is not an EC key");
            }
            if (!sameP256Parameters(ecPublicKey.getParams(), P256)) {
                throw new WorkloadTrustRegistryIntegrityException("public key is not on the P-256 curve");
            }
            return ecPublicKey;
        } catch (WorkloadTrustRegistryIntegrityException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new WorkloadTrustRegistryIntegrityException("public key PEM cannot be parsed", exception);
        }
    }

    private static ECParameterSpec p256Parameters() {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (Exception exception) {
            throw new IllegalStateException("JCA does not provide secp256r1 parameters", exception);
        }
    }

    private static boolean sameP256Parameters(ECParameterSpec actual, ECParameterSpec expected) {
        if (actual == null || !(actual.getCurve().getField() instanceof ECFieldFp actualField)
                || !(expected.getCurve().getField() instanceof ECFieldFp expectedField)) {
            return false;
        }
        ECPoint actualGenerator = actual.getGenerator();
        ECPoint expectedGenerator = expected.getGenerator();
        return actualField.getP().equals(expectedField.getP())
                && actual.getCurve().getA().equals(expected.getCurve().getA())
                && actual.getCurve().getB().equals(expected.getCurve().getB())
                && actualGenerator.getAffineX().equals(expectedGenerator.getAffineX())
                && actualGenerator.getAffineY().equals(expectedGenerator.getAffineY())
                && actual.getOrder().equals(expected.getOrder())
                && actual.getCofactor() == expected.getCofactor();
    }
}
