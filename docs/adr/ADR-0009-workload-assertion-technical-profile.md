# ADR-0009 — Workload Assertion Technical Profile

## Status

Accepted and frozen — HARD-001 technical profile. Depends on
[ADR-0005 — Workload Trust Uses Asymmetric Signed Assertions](ADR-0005-workload-trust-contract.md).

This ADR freezes the technical contract for workload authentication. It does
**not** authorize implementation.

## 1. Algorithm

The workload assertion profile uses **ES256** only: ECDSA on **EC P-256** with
**SHA-256**. The verifier allowlist is exactly `ES256`. It rejects `none`,
`EdDSA`, `Ed25519`, `RS256`, `PS256`, and every other algorithm for this
profile.

The current Logos stack uses JJWT 0.12.5, which directly supports ES256 and
exposes `EdDSA`, not RFC 9864's fully specified JOSE identifier `Ed25519`.
Custom algorithm plumbing or a JJWT upgrade is not justified for the first
HARD-001 implementation; no dependency change is currently required.

## 2. JOSE protected header

Required values:

```text
alg = ES256
typ = logos-workload+jwt
kid = required
```

`kid` must match `[A-Za-z0-9][A-Za-z0-9._-]{0,63}` and contain at most 64
ASCII characters. It is only a bounded, sanitized lookup selector scoped to
the trusted issuer/workload registry. Key IDs are not paths, URLs, SQL, or key
material and must never be reused.

Reject `jku`, `x5u`, `jwk`, and `x5c`, as well as unsupported `crit` values or
the `crit` header itself for this profile. The verifier never fetches key
material from an assertion-supplied URL or accepts caller-supplied key
material.

## 3. Claims

Required claims and exact profile values:

```text
iss = urn:akume:workload-issuer:lifeos
sub = lifeos
aud = urn:akume:service:logos
iat = required NumericDate
exp = required NumericDate
jti = canonical lowercase UUIDv4
```

`aud` must identify exactly the canonical Logos audience; reject absent,
additional, or different audiences. No authorization policy claims are part
of this contract. `allowedSources`, `allowedNamespaces`, and
`allowedOperations` are not authoritative assertion inputs; grants belong to
the HARD-002 relational policy.

## 4. Normalized principal

Only after complete authentication may the verifier produce:

```text
principalType        = WORKLOAD
principalId          = lifeos
authenticationMethod = ASYMMETRIC_SIGNED_ASSERTION
credentialIdentity   = verified registered key identity / kid
authenticationStatus = VERIFIED
```

The stable workload principal is not the key. Key rotation must not change
`WORKLOAD / lifeos`.

## 5. Untrusted header and claim lookup

Before signature verification, `iss`, `kid`, `sub`, and every other claim or
header value are untrusted input. In particular, unverified `sub` must not
establish which workload is trusted and no unverified claim may authorize an
operation.

The canonical credential-resolution sequence is:

1. Parse only enough protected-header and payload structure to obtain
   candidate `iss` and `kid`.
2. Validate their syntax and lengths for safe lookup.
3. Search the Logos-controlled trust registry using candidate issuer plus
   `kid` only.
4. Obtain from the registry the registered public key, registered algorithm,
   credential lifecycle, and associated stable workload principal.
5. Require `alg == ES256`, require an EC P-256 key, and verify the signature.
6. Only after signature verification, validate signed `iss`, `sub`, `aud`,
   `iat`, `exp`, and `jti` against this profile.
7. Require signed `sub` to match the stable principal associated with the
   verified credential.
8. Produce the normalized authenticated principal.

The registry relationship, not an unverified subject, determines which
principal owns the credential.

## 6. Lifetime and clock skew

The maximum assertion lifetime is **60 seconds**. Require `exp > iat` and
`exp - iat <= 60 seconds`. Each outbound authenticated request gets a fresh
assertion. An assertion must not be reused on an HTTP retry; a retry creates a
new assertion and new `jti`.

The maximum accepted clock skew is **15 seconds**. Reject `iat > now + 15s`,
expiration beyond permitted skew, and assertion age greater than the 60-second
TTL plus permitted skew. Clock skew does not replace clock synchronization.

Progression business idempotency remains independently keyed by
`source + idempotencyKey`; it is not assertion replay protection.

## 7. Assertion identity and replay

Generate a fresh random canonical lowercase UUIDv4 `jti` per outbound
authenticated HTTP request. Never derive it from business payload, source,
external ID, or progression idempotency key. The replay identity is
`issuer + jti`.

The first implementation uses PostgreSQL. A database uniqueness guarantee on
the replay identity must make check-and-record atomic. Concurrent presentation
of the same assertion may result in **at most one** successful
authentication. Retain the replay record at least through `exp + accepted
skew`.

Replay-store failure is fail-closed. There is no in-memory fallback or bypass;
Redis/Valkey is not required for the first implementation.

### Replay transaction boundary

Replay consumption is an authentication security event and must not
participate in the downstream business/progression transaction. Required
ordering:

1. Cryptographic, semantic, and credential/workload lifecycle validation
   succeeds.
2. The replay store atomically checks and records the assertion identity.
3. The replay record commits independently.
4. Only after that commit may the request proceed to HARD-002 authorization,
   a controller/application use case, or a progression transaction.

If authorization or business validation fails, progression fails, a
transaction rolls back, or a later server error occurs, the assertion remains
consumed. Replay durability must never depend on downstream transaction
success. An independent transaction, `REQUIRES_NEW`, or equivalent may
implement this semantic; this ADR does not select the Spring mechanism.

## 8. Trust registry

The persistence direction is a Logos-managed **relational** trust registry,
conceptually separating:

- stable workload principal;
- workload signing credential/public key;
- trust lifecycle audit history;
- assertion replay identity.

It must support stable principals, multiple keys during rotation, key
activation/revocation/retirement, whole-workload disable/revocation,
key-to-principal association, algorithm binding, public key material,
not-before/not-after lifecycle, and audit timestamps/history. LifeOS cannot
self-register trusted keys. HARD-002 grants must not be stored on key rows.

## 9. Key formats

Logos canonical public-key import/storage representation is **PEM Subject
Public Key Info**, with `-----BEGIN PUBLIC KEY-----`. On import/activation,
validate EC key type, P-256 curve, and ES256 compatibility.

The future LifeOS private-key representation is **PKCS#8 PEM**, with
`-----BEGIN PRIVATE KEY-----`. The private key remains with LifeOS: it is
never sent to or persisted by Logos.

## 10. Rotation and workload revocation

The stable principal remains `WORKLOAD / lifeos`. During bounded rotation
overlap, both `kid-A` and `kid-B` may be active and authenticate. Once `kid-A`
is revoked, it is rejected immediately while active `kid-B` remains valid.
HARD-002 grants do not change with key rotation. A key ID is never reused
after retirement or revocation.

If `WORKLOAD / lifeos` is disabled or revoked, all its credentials fail
authentication regardless of individual key state. This check occurs before
HARD-002 authorization; no active key overrides whole-workload revocation.

## 11. HTTP transport and profile separation

Canonical transport:

```http
Authorization: Bearer <workload assertion>
```

Do not use a shared-secret service header or AppUser token as workload
authentication. Workload and AppUser Bearer tokens use mutually exclusive
validation profiles. Workload-only routes use only the workload verifier;
invalid workload assertions never fall back to AppUser authentication. Human
authentication remains otherwise unchanged.

Profile separation uses all relevant boundaries: `typ`, issuer, audience,
algorithm, trusted key set, required claims, and workload-only route/profile.
It does not rely on a single discriminator.

## 12. Authentication responsibility

The future HARD-001 authentication adapter conceptually extracts Bearer
credentials, validates the header profile, safely resolves a registered
credential, verifies ES256, validates issuer/subject/audience/time/JTI and key
and workload lifecycle, atomically consumes replay identity, then creates the
normalized principal.

It does not authorize operations, sources, or namespaces; provision external
subjects; determine external ownership; or execute progression. Those belong
to HARD-002, HARD-003, or application use cases as appropriate.

## 13. Failure semantics and logging

Authentication failures return generic **401 Unauthorized**, without
revealing which trust check failed. This includes malformed assertions, wrong
type/algorithm, unknown credentials, revoked credentials, disabled workload,
signature/claim failures, expiry/future time, and replay. Replay-store
unavailability returns **503 Service Unavailable**, fails closed, and never
falls back to another authentication mode. Internal safe reason codes may
distinguish failures without exposing token/key contents.

Never log raw assertions, Authorization headers, private keys, or signatures.
Public keys are not request-logged. Do not log raw `jti` by default; use a
truncated cryptographic hash for correlation. `kid` may be audit metadata only
after syntax validation. `principalId` may be controlled audit metadata.

## 14. Future implementation test requirements

Implementation must prove at minimum:

- valid assertion authenticates; wrong key/signature and tampered assertion
  are rejected;
- unknown and revoked `kid`, disabled workload, wrong issuer, subject,
  audience, and `typ` are rejected;
- `alg=none`, all non-ES256 algorithms, expiry, future `iat`, lifetime over
  60 seconds, and missing/malformed `jti` are rejected;
- replay is rejected and two concurrent identical assertions yield at most
  one authentication;
- replay remains consumed after downstream business rollback/failure;
- key lookup does not trust `sub` before signature verification, and the
  verified subject matches the principal bound to the key;
- human JWT is rejected as workload and workload assertion is not interpreted
  as AppUser JWT;
- rotation overlap accepts A and B; after A revocation, A fails and B remains
  accepted;
- no HARD-002 grant derives from an assertion or key.

## 15. Migration and implementation authorization

No migration is authorized by this ADR. Future HARD-001 implementation will
likely need persistence for workload principals, workload signing keys, trust
audit, and assertion replay. The current Flyway head at the time of this ADR
is V44; do not create V45 as part of this decision freeze.

This ADR freezes the technical profile only. Production code, migrations,
dependency changes, deployment, or LifeOS changes require separate
implementation authorization.
