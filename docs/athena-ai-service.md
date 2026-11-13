# Athena AI Service — Specification

**Status:** Draft v0.1 (2026-11-13)  
**Relationship to the app:** optional paid AI provider for Athena Reader (PRD FR-017, FR-045, FR-046, §9). The app is open source (Apache-2.0). This service is closed source and lives in a separate private repository.  
**Visibility note:** this document sits in the public app repo for now. Before the repo goes public, keep only §3 (the client-facing contract) here and move the rest to the private service repo.

---

## 1. Purpose

Give Athena Reader users AI that works straight after sign-in: no API keys, no model choice, no setup. Charge a monthly subscription with an included allowance, plus top-ups for heavy use.

**Non-goals:** storing users' books, notes, or library data; syncing; a general-purpose public LLM API; serving clients other than Athena Reader.

## 2. Architecture

```
Athena Reader (desktop)
   │  OIDC Auth Code + PKCE (system browser, loopback redirect)
   ├──────────────────────────────► Keycloak  (sso.ukata.io, realm: ukata)
   │
   │  HTTPS, Bearer <access token>
   ▼
Athena AI Gateway  ──► Upstream LLM providers (keys held only here)
   │       │
   │       └──► Postgres: accounts, entitlements, usage ledger
   │            Redis (optional): rate limits, in-flight reservations
   ▲
   │  signed webhooks
Merchant of record (Paddle or Lemon Squeezy): checkout, subscriptions, tax, invoices
```

- **Gateway:** Kotlin + Ktor server (shares request/response models with the app), stateless, horizontally scalable behind TLS.
- **Upstream provider keys** exist only in the gateway's secret store. They never reach the client.
- One region at launch. Hosting and data location are recorded in the privacy policy.

## 3. Client contract (public)

Base URL: `https://ai.ukata.io/v1` (placeholder).

All endpoints require `Authorization: Bearer <Keycloak access token>`. The token must have audience `athena-api` and the client `athena-desktop`.

### 3.1 `POST /v1/chat/completions`

OpenAI-compatible request and response, including SSE streaming (`stream: true`). That lets the app reuse its existing provider client.

- `model` is a **tier alias**, never an upstream model name:
  - `athena-fast`: word lookups, sentence help, short answers.
  - `athena-deep`: mechanism explanations, follow-ups, diagrams, concept extraction.
  - The server maps aliases to upstream models and can change that mapping without an app release.
- Optional header `X-Athena-Request-Type`: one of the PRD request types (`word`, `mechanism`, …). It is used for routing and analytics counts only, never for content.
- **Limits per request:**
  - input ≤ 8,000 tokens (PRD FR-012's ~1,500-word context budget fits comfortably);
  - output ≤ 2,000 tokens (fast) or 4,000 tokens (deep);
  - larger requests get `413`.
- The final usage is returned in the standard `usage` object (and in the final SSE chunk).

### 3.2 `GET /v1/models`

Lists the aliases the user's plan can use.

### 3.3 `GET /v1/me`

```json
{
  "user": { "id": "kc-uuid", "name": "…", "email": "…" },
  "plan": { "id": "reader-monthly", "name": "Reader", "status": "active", "renews_at": "2026-12-13T00:00:00Z" },
  "allowance": { "unit": "credits", "included": 2000, "used": 412, "resets_at": "2026-12-13T00:00:00Z" },
  "topup": { "balance": 500 },
  "links": { "manage": "https://…", "upgrade": "https://…", "topup": "https://…" }
}
```

### 3.4 Errors

The body is always `{"error": {"code", "message", "retry_after?"}}`, with a human-readable `message` the app can show directly.

| Status | Code | Meaning / app behaviour (FR-046) |
|---|---|---|
| 401 | `unauthenticated` | Token missing or expired: refresh once, then ask the user to sign in |
| 402 | `allowance_exhausted` | Allowance and top-ups are used up: show the renewal date and top-up link |
| 403 | `no_subscription` | Signed in but no active plan: show the plans link |
| 413 | `request_too_large` | Context exceeds the cap: the app should never send this; log a client bug locally |
| 429 | `rate_limited` | Too many requests or too much concurrency: retry after `retry_after` |
| 503 | `upstream_unavailable` | All upstreams for the tier are down: retryable |

## 4. Authentication (Keycloak)

**Realm:** existing realm on `sso.ukata.io`.

**Client `athena-desktop`:**
- public client (no secret), standard flow only, PKCE S256 required;
- redirect URIs: `http://127.0.0.1/*`, with loopback any-port matching verified on the deployed Keycloak version;
- Device Authorization Grant enabled as a fallback;
- refresh tokens with offline-capable rotation;
- an audience mapper adds `athena-api`.

**Gateway (resource server):**
- validates the JWT signature (realm JWKS, cached), `iss`, `aud`, `exp`, and `azp = athena-desktop`;
- uses Keycloak `sub` as the user ID.

**Token lifetimes:** access tokens 5–15 minutes, refresh tokens rotate on every use, with a configurable idle timeout.

**Entitlements live in the gateway's database, not in token claims.** Plan changes therefore take effect immediately, without waiting for the user to sign in again.

**Account deletion:** a self-service link deletes the user from both Keycloak and the gateway. Usage records are anonymized, except where tax law requires keeping invoices (those are held by the merchant of record).

## 5. Billing and entitlements

- **Merchant of record:** Paddle or Lemon Squeezy (OD-7). It handles checkout, VAT and sales tax, invoices, refunds, and dunning. The gateway never handles card data.
- **Products:**
  - **Reader** monthly subscription (an annual option can come later), which includes a monthly credit allowance.
  - **Top-up packs**, one-time purchases of credits. Top-up credits do not expire while the account exists (or expire after 12 months; decide in OD-7).
- **Consumption order:** monthly allowance first, then top-up balance. The unused allowance does not roll over.
- **Webhooks:** subscription created, renewed, updated, cancelled, past due; payment refunded; top-up purchased. Each webhook is verified by signature and processed idempotently by event ID. It updates `entitlements` and writes ledger entries.
- **Cancellation:** access continues until the period ends. A refund reverses the matching credits.
- **Linking:** checkout links carry the Keycloak `sub` as custom data, so purchases attach to the right account.

## 6. Metering

- **Credits** are an internal unit that hides model prices: `credits = ceil((input_tokens × in_rate + output_tokens × out_rate) × tier_multiplier)`. The rates are in server config.
- **Reserve, then settle:**
  1. Before calling upstream, reserve the maximum possible cost (input tokens counted + max output). Reject with 402 if the available credit is below the reservation.
  2. After the response (or on cancellation or disconnect), settle the actual usage reported by upstream, or counted from the stream if upstream reports none, and release the remainder.
- **Hard cap:** reservations make overspending impossible, even with concurrent requests.
- **Ledger:** an append-only `usage_ledger` with user ID, timestamp, tier, request type, input and output tokens, credits, reservation ID, and upstream model. **It never contains prompt or response content.**
- **Concurrency:** at most 2 in-flight requests per user (configurable).

## 7. Routing and models

- A config table maps each tier to an ordered list of upstream (provider, model) pairs, with failover on 5xx errors and timeouts.
- `athena-fast` → a small, fast, cheap model; `athena-deep` → a strong reasoning model.
- Translation quality matters for vocabulary lookups (PRD FR-020). Evaluate candidate models on a small fixed test set before changing the mapping.
- Upstream terms: the service resells API access inside its own product. Comply with each provider's commercial terms and usage policies, and pass through their content restrictions.

## 8. Privacy, security, abuse

- **No content logging.** Request and response bodies are never logged, traced, or stored. Error logs contain status codes and IDs only. Enforce this in code review, and with a log scrubber as a safety net.
- **Upstream retention:** use zero-data-retention or no-training terms where providers offer them. List the subprocessors in the privacy policy.
- **Abuse limits:**
  - per-user rate limits (requests per minute, credits per hour);
  - the context-size cap (§3.1), which also blocks whole-book uploads, in line with PRD FR-018;
  - anomaly alerts on spend spikes.
- **Operational metrics** (latency, error rates, credits per tier) are aggregated and never per-content.
- **Security:** TLS only. Secrets in a secret manager. Dependency scanning. Webhook signature verification. Parameterized SQL only.
- **Legal before launch:** privacy policy, terms of service, refund policy, and a data processing statement.

## 9. Open questions (OD-7)

| Question | Notes |
|---|---|
| Plan price and included credits | Model on expected use: e.g. 40 lookups + 5 deep explanations per reading day. Target ≥ 60% gross margin at the 90th-percentile user |
| Top-up pack sizes, price, and expiry | |
| Paddle vs. Lemon Squeezy | Compare fees, payout countries, and API quality |
| Free trial | E.g. a one-time 200 credits on first sign-in, capped per account |
| Launch regions and hosting location | Affects the privacy policy and latency |
| Annual plan | Post-launch |

## 10. Milestones

1. Gateway skeleton: JWT validation, `/v1/chat/completions` pass-through for one tier, streaming.
2. Metering: reservations, ledger, `/v1/me`, hard caps, rate limits.
3. Billing: merchant of record products, webhooks, entitlements, top-ups.
4. Routing and failover, model evaluation set.
5. Legal pages, monitoring, launch (paired with PRD Milestone 6).
