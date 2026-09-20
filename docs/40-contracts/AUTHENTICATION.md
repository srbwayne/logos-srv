# Authentication

## Current status

Logos currently uses `AppUser` authentication with stateless JWT access tokens.
This is POC/pre-production authentication and must not be represented as a
production-ready security design.

## JWT signing configuration

Tokens are signed and verified with the active symmetric HMAC signing key.
The runtime must provide the key through `LOGOS_JWT_SECRET`:

```text
LOGOS_JWT_SECRET=<base64-encoded-secret>
```

There is no runtime default signing key in the repository. Application startup
fails when neither `LOGOS_JWT_SECRET` nor a higher-precedence Spring property
provides `application.security.jwt.secret-key`.

The CI workflow supplies isolated deterministic test-only signing material by
setting the Spring property for its test command. That value is not a runtime
or deployment secret.

## Token behavior and rotation

Access tokens expire after 24 hours. Logos has no refresh-token mechanism.
Because verification accepts only the active HMAC signing key, changing
`LOGOS_JWT_SECRET` invalidates previously issued access tokens.

The JWT signing material previously tracked in Git history is considered
compromised and must never be reused. Generate a new independent secret outside
Git and provide it only through the runtime environment or another approved
external secret mechanism. This remediation does not rewrite Git history.
