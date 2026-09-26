# ADR-0005 — Verify External Subject Links Before Resolution

## Status

Accepted and implemented by LOGOS-PROGRESSION-TRUST-BOUNDARY-001-F2.

## Context

The former AppUser-authenticated self-claim endpoint allowed any authenticated
Logos user to claim an arbitrary `(namespace, externalId)`. C2 characterized
identity squatting and end-to-end misbinding: a later legitimate integration
execution resolved to the first claimant's player. Namespace authorization on
the execution API did not prove who owned the external identity.

## Decision

External linking requires two distinct authorities:

1. An authenticated AppUser expresses Logos-side intent by creating a
   short-lived, one-time challenge for a namespace. The request has no
   `externalId`; the challenge is bound to that AppUser's player.
2. A configured integration principal authorized for that namespace confirms
   the challenge and supplies the `externalId`.

The challenge is 256-bit random, opaque, and single-use. Logos persists only
its SHA-256 hash. Mapping update and challenge consumption are transactional
and lock the challenge row. The `logos-native` namespace remains Logos-managed.

Mappings have explicit verification status. Native mappings are
`LOGOS_NATIVE`; pre-existing external mappings migrate as `UNVERIFIED`; and
confirmed external mappings are `INTEGRATION_VERIFIED`. Progression resolution
accepts only native and integration-verified mappings. A valid confirmation
may promote/correct an unverified legacy mapping to the challenged player, but
a verified identity cannot be silently reassigned and conflicting ownership
returns HTTP 409.

## Rationale and limits

An authenticated AppUser is not an integration service: user authentication
proves control of a Logos account, not authority over an external namespace or
external ID. Likewise, source and namespace in a request are authorization
inputs, not trustworthy assertions. The AppUser challenge records intent;
the namespace-authorized integration supplies the external ID.

This is a POC trust model, not final OAuth/OIDC or cryptographic external-user
ownership proof. The integration remains trusted to assert the external ID for
its authenticated external user. Provider-issued proof may replace the
integration assertion later without changing the `(namespace, externalId)`
identity model. Administrative recovery for already verified links is not
provided by this slice.

## Consequences

- Ordinary AppUsers can no longer self-claim external identities.
- Legacy external mappings stop resolving until confirmed through the new
  flow, preventing old unverified claims from driving progression.
- Integration credentials and namespace authorization are required to finalize
  a link.
- Challenge records are ephemeral state and may be retained after expiry; a
  cleanup job is not part of this decision.
- OIDC/provider proof, service credential evolution, and verified-link
  administrative recovery remain follow-up work.
