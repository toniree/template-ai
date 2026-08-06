# template-ai

Interview scaffold for the Brex senior SWE round: a working corporate-card spend API with a
database, a REST layer, and a frontend that reads from it — plus the prompt library and playbooks
for the 100 minutes.

**Java 21 · Spring Boot 3.4 · H2 in PostgreSQL compatibility mode · vanilla-JS UI, no build step**

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
| [`docs/PROBLEM_TEMPLATE.md`](docs/PROBLEM_TEMPLATE.md) | Paste your design notes in; the agent writes back MVP scope, the critical invariant, and what's out of scope. |
| [`CLAUDE.md`](CLAUDE.md) | The working agreement the agent reads every session. Read it once yourself. |
| [`docs/ARCHITECTURE_TEMPLATE.md`](docs/ARCHITECTURE_TEMPLATE.md) | Fill in as you build; it's the page you talk from at the end. |
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
| Caller identity behind one swappable class, not threaded through signatures | `common/CurrentUser` |

## Reusable test support

`src/test/java/…/support/` — the parts that are slow to write correctly under time pressure:

| | |
|---|---|
| `ApiIntegrationTest` | Base class: `http` (MockMvc), `json`, the `test` profile, and `as(userId)` to call as a principal. Extend it and start writing assertions. |
| `Concurrently` | Fires N overlapping attempts at one row and counts winners — `Concurrently.run(8, attempt).assertExactlyOneWon()`. The test that proves your invariant actually holds, and the one interviewers remember. |
| `MutableClock` | A `Clock` you advance by hand, so a five-minute expiry rule costs no wall-clock time to test. `@Import(MutableClock.Config.class)`. |

All three are covered by their own tests (`SupportTest`, `MutableClockIT`), so a green suite means
the helpers themselves work, not just your code.

## Identity

`common/CurrentUser` reads an `X-User-Id` header — inject it, call `require()`, get the id or a 401.

It is **not authentication**: the caller supplies the header, so anyone can claim any identity. It
exists so that business code has exactly one place to ask "who is calling", which means swapping in
a verified JWT or session later touches `CurrentUser.find()` and nothing else. Say that out loud
rather than describing the app as having auth — the shortcut is fine, pretending it isn't one is not.

## Layout

```
CLAUDE.md            agent working agreement — loaded automatically
docs/                PROBLEM_TEMPLATE, ARCHITECTURE_TEMPLATE, runbook,
                     system-design prep, cheatsheet
prompts/             00-kickoff → 05-endgame
sandbox/
  src/main/java/com/templateai/sandbox/
    common/          ApiError, ApiException, GlobalExceptionHandler, PageResponse,
                     CurrentUser, AppConfig (Clock), RequestLoggingFilter  ← reusable
    card/            Card, CardRepository, CardDtos, CardService, CardController
    transaction/     Transaction, TransactionRepository, TransactionDtos,
                     TransactionService, TransactionController
    DemoData.java    seeds the h2 profile so the UI is never empty
  src/main/resources/static/
    app.js           config-driven UI — a screen is one entry in RESOURCES
    index.html, styles.css
  src/test/java/…/
    support/         ApiIntegrationTest, Concurrently, MutableClock         ← reusable
    card/, transaction/   CardApiIT, TransactionApiIT — copy these for new features
```

One flat package per feature, five files, no `impl/` or `mapper/` layers. Copy the shape.

The two `← reusable` directories know nothing about any domain and survive every reset. Everything
else is sample and is meant to be replaced.

## If the interview isn't about cards

```bash
rm -rf sandbox/src/main/java/com/templateai/sandbox/{card,transaction} \
       sandbox/src/test/java/com/templateai/sandbox/{card,transaction} \
       sandbox/src/main/java/com/templateai/sandbox/DemoData.java
```

Then run `prompts/00-kickoff.md`. Everything in `common/`, the error contract, the config-driven
UI, and the test templates carry over unchanged.

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
