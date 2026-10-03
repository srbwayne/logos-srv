# Progression Subject Identity Provisioning HTTP V1

Status: CURRENT — POC/pre-release V1 integration boundary.

## Endpoint

```http
POST /api/internal/v1/progression/subject-identities
```

The endpoint requires the existing authenticated AppUser JWT. The authenticated
user is always the target Logos player; the request cannot select another target.

### Request

```json
{
  "namespace": "lifeos",
  "externalId": "user-123"
}
```

`namespace` is trimmed, lowercased with `Locale.ROOT`, nonblank, and limited to
64 characters. `externalId` is trimmed, nonblank, case-sensitive, opaque, and
limited to 255 characters. The response contains the canonical values:

```json
{
  "namespace": "lifeos",
  "externalId": "user-123"
}
```

The `logos-native` namespace is managed by Logos and cannot be self-provisioned
through this endpoint. New `/api/auth/register` registrations create their
`logos-native` mapping transactionally.

## Semantics

The mapping identity is `(namespace, externalId)`. Repeating the same mapping
for the same authenticated player returns HTTP 200 without creating a
duplicate. Attempting to claim an existing identity owned by another player
returns HTTP 409 with code `PROGRESSION_SUBJECT_IDENTITY_CONFLICT`.

One player may own multiple external identities. Lookup never creates a player,
links by email, or changes an existing mapping.

## POC boundary

This is intentionally a pre-production POC. The authenticated AppUser JWT is
accepted as the trust mechanism and the caller's possession of an external ID
is not proof of external ownership. Service principals, OIDC, source and
namespace authorization, cross-service read authorization, and external
identity proof remain required before production exposure.

## C1A ownership classification

The endpoint remains a bounded, pre-production POC. A successful new
self-link is classified `EXTERNAL / ACTIVE / UNVERIFIED` with provenance
`POC_SELF_LINK`. The authenticated AppUser is recorded only as the action
actor. Authentication and possession of the submitted locator are not
ownership proof and do not produce `VERIFIED` status.

C1A stores this initial classification and append-only creation history. It
does not add lifecycle administration or enforce ownership state during
progression execution; existing subject resolution behavior is preserved.
