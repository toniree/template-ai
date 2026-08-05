# template-ai

Interview scaffold for the Brex senior SWE round: a working corporate-card spend API with a
database, a REST layer, and a frontend that reads from it — plus the prompt library and playbooks
for the 100 minutes.

**Java 21 · Spring Boot 3.4 · H2 (Postgres-compatible) · vanilla-JS UI, no build step**

```bash
cd sandbox
./mvnw clean test             # ONCE, online — populates ~/.m2 (see below). 25 tests, ~4s
./mvnw -o spring-boot:run     # http://localhost:8080 — seeded and clickable
./mvnw -o test                # 25 tests, ~3s
```

**Run Maven online at least once before relying on `-o`.** The `-o` (offline) flag doesn't
download anything — it only works once every dependency is already in `~/.m2`. On a fresh machine,
or after any `pom.xml` change, `-o` fails with unresolvable artifacts. Do the online run as part of
your pre-flight, not at 9:59 on interview morning; see
[`docs/RUNBOOK.md`](docs/RUNBOOK.md) for the full checklist.

## Start here

| | |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | The working agreement the agent reads every session. Read it once yourself. |
| [`docs/RUNBOOK.md`](docs/RUNBOOK.md) | The AI round: pre-flight, minute-by-minute, how to negotiate scope. |
| [`docs/SYSTEM-DESIGN.md`](docs/SYSTEM-DESIGN.md) | The design round: clarifying questions, the scale arithmetic, fintech data models, failure modes. |
| [`prompts/`](prompts/README.md) | Six copy-paste prompts, ordered by when you use them. |
| [`docs/CHEATSHEET.md`](docs/CHEATSHEET.md) | Spring/JPA/HTTP reference, plus T-SQL → Postgres translations. |

## What's already built

A corporate-card slice, chosen because it exercises the patterns a fintech prompt grades — not
because you'll necessarily keep it.

- `POST /api/cards`, `GET /api/cards`, `GET /api/cards/{id}`, `PATCH /api/cards/{id}`
- `POST /api/transactions` (authorize), `GET /api/transactions?cardId&page&size`, `GET /api/transactions/{id}`

Cards carry a spend limit; authorizing a charge checks it and records the outcome. Declines are
stored rather than thrown away, because the decline history *is* the product.

**Patterns worth pointing at during the interview:**

| Pattern | Where |
|---|---|
| Money as `long` minor units + ISO-4217 currency, never a float | `Card`, `Transaction` |
| Idempotency on the money-moving POST via `Idempotency-Key`, unique index, 200 on replay | `TransactionService.authorize` |
| Business rejection is 201 + `DECLINED`, not a 4xx | `TransactionController` |
| Per-row aggregate in one grouped query, not N+1 | `TransactionRepository.sumAmountGroupedByCard` |
| Balance derived from entries so it cannot drift | `TransactionService.declineReason` |
| Row lock so two concurrent charges can't both pass the same limit check | `CardRepository.findWithLockById` |
| No `DELETE` on cards — financial records are cancelled, not deleted | `CardService.update` |
| One typed error shape for every failure, correct 4xx for malformed requests | `common/GlobalExceptionHandler` |
| True partial `PATCH` — omitted fields are left alone, not zeroed | `CardService.update` |
| Capped pagination behind a stable envelope | `common/PageResponse` |

## Layout

```
CLAUDE.md            agent working agreement — loaded automatically
docs/                runbook, system-design prep, cheatsheet
prompts/             00-kickoff → 05-endgame
sandbox/
  src/main/java/com/templateai/sandbox/
    common/          ApiError, ApiException, GlobalExceptionHandler, PageResponse,
                     AppConfig (Clock), RequestLoggingFilter
    card/            Card, CardRepository, CardDtos, CardService, CardController
    transaction/     Transaction, TransactionRepository, TransactionDtos,
                     TransactionService, TransactionController
    DemoData.java    seeds the h2 profile so the UI is never empty
  src/main/resources/static/
    app.js           config-driven UI — a screen is one entry in RESOURCES
    index.html, styles.css
  src/test/…         CardApiIT, TransactionApiIT — copy these for new features
```

One flat package per feature, five files, no `impl/` or `mapper/` layers. Copy the shape.

## If the interview isn't about cards

```bash
rm -rf sandbox/src/main/java/com/templateai/sandbox/{card,transaction} \
       sandbox/src/test/java/com/templateai/sandbox/{card,transaction} \
       sandbox/src/main/java/com/templateai/sandbox/DemoData.java
```

Then run `prompts/00-kickoff.md`. Everything in `common/`, the error contract, the config-driven
UI, and the test templates carry over unchanged.

## Handing this off as an archive

Build it from git, not by zipping the working tree — `git archive` ships only tracked files, so
`target/`, `.idea/`, `__MACOSX/`, and `.DS_Store` cannot leak in:

```bash
git archive --format=zip --prefix=template-ai/ -o /tmp/template-ai.zip HEAD
unzip -l /tmp/template-ai.zip | grep -E 'target/|\.idea/|__MACOSX|\.DS_Store' || echo "clean"
```

macOS Finder's "Compress" adds a `__MACOSX/` sidecar and resource-fork `._*` files; both are
gitignored here, but the Finder path adds them at zip time regardless. Use the command above.

## Profiles

| Profile | Use |
|---|---|
| `h2` (default) | in-memory, seeded on boot, zero setup — what you demo on |
| `test` | isolated in-memory database, no seed data, quiet logs |
| `postgres` | `docker run -p 5432:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=sandbox postgres:16` |

H2 runs in `MODE=PostgreSQL`, so the SQL and JPA you write maps onto real Postgres when you switch.

## Deliberately not here

Auth, an append-only double-entry ledger, capture/settlement as separate records from
authorization, reconciliation, idempotency-key expiry, rate limiting, cursor pagination. Each is
the right call for a scaffold and the wrong call for production —
[`docs/SYSTEM-DESIGN.md`](docs/SYSTEM-DESIGN.md) covers what changes and when.
