package com.josecjuniors.logossrv.core.security.workload.application.port.out;

import com.josecjuniors.logossrv.core.security.workload.domain.TrustedWorkloadCredential;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;

import java.util.Optional;

public interface WorkloadTrustRegistry {
    Optional<TrustedWorkloadCredential> findByIssuerAndKid(WorkloadIssuer issuer, WorkloadKeyId kid);
}
