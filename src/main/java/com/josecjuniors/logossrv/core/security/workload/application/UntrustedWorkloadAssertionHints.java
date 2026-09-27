package com.josecjuniors.logossrv.core.security.workload.application;

import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;

/** Unverified lookup hints only; these values never establish an authenticated identity. */
public record UntrustedWorkloadAssertionHints(WorkloadIssuer candidateIssuer, WorkloadKeyId candidateKid) {
}
