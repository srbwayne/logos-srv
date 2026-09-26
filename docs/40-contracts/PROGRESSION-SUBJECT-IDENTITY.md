# Progression Subject Identity Linking HTTP V1

Status: CURRENT — POC/pre-release V1 integration boundary.

## Two-party linking flow

An authenticated AppUser starts linking by calling:

```http
POST /api/internal/v1/progression/subject-link-challenges
```

The request contains only a namespace, never an `externalId`:

```json
{"namespace":"lifeos"}
```

The authenticated player's Logos account is bound to the resulting challenge.
The response returns the namespace, an opaque random challenge token, and its
expiration. The default TTL is `PT10M` and is configurable. `logos-native` is
reserved for Logos and cannot be challenged.

A configured `PROGRESSION_INTEGRATION` principal authorized for that namespace
confirms the challenge:

```http
POST /api/internal/v1/progression/subject-identities
```

```json
{
  "namespace": "lifeos",
  "externalId": "user-123",
  "challengeToken": "<opaque token returned to the AppUser>"
}
```

An ordinary AppUser JWT cannot call this confirmation endpoint. The namespace
is checked against the integration principal's policy before confirmation.
Namespace and external ID normalization retain the existing contract:
namespace is trimmed, lower-cased with `Locale.ROOT`, nonblank, and limited to
64 characters; external ID is trimmed, nonblank, case-sensitive, opaque, and
limited to 255 characters.

The challenge is 256-bit cryptographically random data encoded Base64 URL-safe
without padding. Only its SHA-256 hash is persisted. It is short-lived and
single-use. Mapping creation/promotion and challenge consumption share one
transaction, with a row lock preventing concurrent reuse. Raw tokens and
integration secrets must not be logged.

## Identity semantics

The mapping identity is `(namespace, externalId)`, protected by the database
unique constraint. `/api/auth/register` continues to create its `logos-native`
mapping transactionally. Native rows have verification status `LOGOS_NATIVE`;
old external rows become `UNVERIFIED` on migration; successful integration
confirmations create `INTEGRATION_VERIFIED` mappings.

The progression resolver accepts only `LOGOS_NATIVE` and
`INTEGRATION_VERIFIED`. Legacy `UNVERIFIED` rows are not resolvable, preventing
pre-existing self-claims from routing progression. A valid integration
confirmation can promote/correct an `UNVERIFIED` legacy mapping to the player
bound to the challenge. A verified mapping is never silently reassigned: a
different challenged player receives HTTP 409 with
`PROGRESSION_SUBJECT_IDENTITY_CONFLICT`. Reconfirmation by its current owner is
idempotent.

## Trust boundary and limits

This POC combines Logos-side user intent (the AppUser-bound challenge) with
namespace-owner confirmation (the authorized integration supplies the
external ID). It prevents an ordinary AppUser from independently claiming an
external ID, but it is not final OAuth/OIDC or cryptographic proof of external
user ownership. The integration service remains trusted to assert which
external ID belongs to its authenticated external user. Provider-issued proof
and administrative recovery for already verified links remain future work.

New AppUser registrations continue to receive `logos-native:<AppUserId>`; no
external identity is inferred from email, and identity resolution never
creates a player.
