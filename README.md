# template-ai

A domain-neutral Spring Boot starter for AI-assisted coding interviews. It boots with a working
database, REST API, typed errors, Swagger, a small frontend, and a fast test suite — so the hour
goes into the problem you were actually given, not into scaffolding.

**Java 21 · Spring Boot 3.4 · PostgreSQL 16 in Docker · vanilla-JS UI, no build step**

> **This is the `postgres` branch.** It is the same scaffold as `generic`, with real PostgreSQL as
> the default for both running the app and running the suite. Use it when the problem turns on
> database behaviour — row locking under contention, constraint timing, isolation, `SKIP LOCKED`,
> `ON CONFLICT`. Use `generic` (in-memory H2, no Docker) when it doesn't.

```bash
./run.sh                      # repo root — starts Postgres if it isn't running, then the app
```

`./run.sh` calls `scripts/ensure-postgres.sh`, which is idempotent and safe to run before every
command: if nothing's listening on 5432 it starts Docker Compose (if you have Docker and the repo's
`docker-compose.yml`) or `postgresql@16` via `brew services` (if you don't), waits for it to accept
connections, then creates the `postgres` role and the `sandbox`/`sandbox_test` databases if they're
missing — with the same `postgres`/`postgres` credentials either way, so `application.yml` needs no
per-machine edits. Run it standalone before `test` too:

```bash
./scripts/ensure-postgres.sh
cd sandbox
./mvnw clean test             # ONCE, online — populates ~/.m2 (see below)
./mvnw -o test                # whole suite, against the sandbox_test database
```

Two databases, one Postgres: `sandbox` for the app, `sandbox_test` for the suite, so a test run
can't drop the schema out from under a live demo. With Docker, `docker compose down -v` throws it
all away; with Homebrew, `brew services stop postgresql@16`, and `dropdb sandbox sandbox_test` if
you want the databases gone too.

**Run Maven online at least once before relying on `-o`.** The `-o` (offline) flag doesn't
download anything — it only works once every dependency is already in `~/.m2`. On a fresh machine,
or after any `pom.xml` change, `-o` fails with unresolvable artifacts. Do the online run as part of
your pre-flight, not on interview morning; [`docs/RUNBOOK.md`](docs/RUNBOOK.md) has the checklist.

## Start here

| | |
|---|---|
| [`prompts/QUICK.md`](prompts/QUICK.md) | Four prompts covering the whole interview. Keep this one open. |
| [`docs/PROBLEM_TEMPLATE.md`](docs/PROBLEM_TEMPLATE.md) | Paste your design notes in; the agent writes back MVP scope, the critical invariant, and what's out of scope. |
| [`CLAUDE.md`](CLAUDE.md) | The working agreement the agent reads every session. Read it once yourself. |
| [`docs/RUNBOOK.md`](docs/RUNBOOK.md) | Pre-flight, minute-by-minute plan, checkpoints, recovery moves. |
| [`docs/ARCHITECTURE_TEMPLATE.md`](docs/ARCHITECTURE_TEMPLATE.md) | Fill in as you build; it's the page you talk from at the end. |
| [`prompts/`](prompts/README.md) | The long-form prompts, for when a step needs more structure. |
| [`docs/CHEATSHEET.md`](docs/CHEATSHEET.md) | Spring/JPA/HTTP reference and the gotchas that cost minutes. |

## What's already built

The plumbing, plus one sample resource to prove it works:

| | |
|---|---|
| `POST /api/tasks` | 201 + `Location` |
| `GET /api/tasks?status=` | 200, optional filter |
| `GET /api/tasks/{id}` | 200 / 404 |
| `PATCH /api/tasks/{id}` | 200 — partial, omitted fields untouched |
| `DELETE /api/tasks/{id}` | 204 / 404 |

A task is `id, title, description, status (TODO / IN_PROGRESS / DONE), createdAt, updatedAt`. There
are no users, projects, tags, comments, or relationships, on purpose — the sample is a fixture, not
a product.

**Patterns worth pointing at during the interview:**

| Pattern | Where |
|---|---|
| One typed error shape for every failure, correct 4xx for malformed requests | `common/GlobalExceptionHandler` |
| True partial `PATCH` — omitted fields are left alone, not blanked | `TaskService.update` |
| Boxed/nullable request fields, so an omitted value fails validation instead of defaulting | `TaskDtos` |
| Entities mapped to DTOs inside the transaction (`open-in-view` is off) | `TaskService` |
| Injected `Clock` instead of `Instant.now()` | `common/AppConfig` |
| Filtering pushed into SQL, not done in Java | `TaskRepository` |
| Integration tests over real HTTP and a real database, no mocks | `TaskApiIT`, `ErrorContractIT` |
| One request log line per API call: method, path, status, duration | `common/RequestLoggingFilter` |
| Caller identity behind one swappable class, not threaded through signatures | `common/CurrentUser` |

## Reusable test support

`src/test/java/…/support/` — the parts that are slow to write correctly under time pressure:

| | |
|---|---|
| `ApiIntegrationTest` | Base class: `http` (MockMvc), `json`, the `test` profile, `postJson`/`patchJson`, and `as(userId)` to call as a principal. Extend it and start writing assertions. |
| `ApiErrors` | One-line outcome assertions — `created()`, `validationError("title must not be blank")`, `notFound()`, `conflict()`, `unauthorized()`. Checks the whole error body, not just the status line. |
| `Concurrently` | Fires N overlapping attempts at one row and counts winners — `Concurrently.run(8, attempt).assertExactlyOneWon()`. The test that proves your invariant actually holds, and the one interviewers remember. |
| `MutableClock` | A `Clock` you advance by hand, so a five-minute expiry rule costs no wall-clock time to test. `@Import(MutableClock.Config.class)`. |

`TaskApiIT` covers `ApiIntegrationTest` and `ApiErrors` — it is the file to copy for a new feature's
happy path and error cases. `Concurrently` and `MutableClock` aren't exercised by the sample domain
(`Task` has no contended invariant or expiry rule to prove); reach for them, and their own coverage
in `SupportTest`/`MutableClockIT`, when your feature actually has one. **On this branch specifically:
"real PostgreSQL" only means the suite runs against it — it doesn't mean a lock/contention test
exists yet.** Nothing here has proven a `for update` claim true until you write a `Concurrently` test
for the feature that needs it and watch it fail without the fix.

## Identity

`common/CurrentUser` reads an `X-User-Id` header — inject it, call `require()`, get the id or a 401.

It is **not authentication**: the caller supplies the header, so anyone can claim any identity. It
exists so that business code has exactly one place to ask "who is calling", which means swapping in
a verified JWT or session later touches `CurrentUser.find()` and nothing else. Say that out loud
rather than describing the app as having auth — the shortcut is fine, pretending it isn't one is not.

## Layout

```
CLAUDE.md            agent working agreement — loaded automatically
docs/                PROBLEM_TEMPLATE, ARCHITECTURE_TEMPLATE, runbook, cheatsheet
prompts/             00-kickoff → 07-endgame
sandbox/
  src/main/java/com/templateai/sandbox/
    common/          ApiError, ApiException, GlobalExceptionHandler, CurrentUser,
                     AppConfig (Clock), RequestLoggingFilter        ← reusable, leave alone
    task/            Task, TaskRepository, TaskDtos, TaskService, TaskController
    DemoData.java    seeds postgres/h2 (never test) so the UI is never empty
  src/main/resources/static/
    app.js           the whole UI — one screen, written directly, no renderer framework
    index.html, styles.css
  src/test/java/…/
    support/         ApiIntegrationTest, ApiErrors, Concurrently,
                     MutableClock                                    ← reusable, leave alone
    task/            TaskApiIT (copy this for new features)
    common/          ErrorContractIT (keep as-is, just repoint its paths)
```

The two `← reusable` directories know nothing about any domain and survive every reset. Everything
else is sample and is meant to be replaced.

One flat package per feature, no `impl/` or `mapper/` layers. Five files is the default shape for a
persisted CRUD resource — drop any layer that has nothing to do, and add a concrete collaborator
class if the problem genuinely needs one. See [`CLAUDE.md`](CLAUDE.md) for what stays banned.

## Replacing the sample domain

**If the real problem has the same basic shape** — a persisted resource with a status you list,
create, and update — don't delete anything. Rename `task/` to the real name and adapt the fields.
You keep a working app the entire time, which is the whole point.

**If it's a different shape**, keep `task/` as a working reference while you build. Delete it only
once all three of these are green:

1. the first real vertical slice works end to end (endpoint responds, one test passes),
2. `static/app.js` and `index.html` point at the new resource,
3. `ErrorContractIT`'s paths are repointed at a real endpoint and it still passes.

Then, and only then:

```bash
git rm -r sandbox/src/main/java/com/templateai/sandbox/task \
          sandbox/src/test/java/com/templateai/sandbox/task \
          sandbox/src/main/java/com/templateai/sandbox/DemoData.java
./mvnw -o test && git add -A && git commit -q -m "chore: drop the sample domain"
```

Deleting it first leaves you with a repo that doesn't compile, no reference to copy from, and
nothing to demo if the next ten minutes go badly.

Whatever you do, `common/` stays untouched — nothing in it knows what a task is. For the frontend,
change the constants at the top of `static/app.js`, the `<thead>` in `index.html`, and the cells in
`taskRow()`.

## Checkpoint after every working feature

```bash
git add -A && git commit -q -m "feat: <feature>"
```

Ten seconds, three or four times an hour. It is the only thing that turns "this experiment went
wrong" into `git reset --hard HEAD` instead of a five-minute salvage job. An uncommitted green
state cannot be returned to.

## Deliberately not here

Auth, authorization, rate limiting, caching, async/queues, a frontend build step, and a CI pipeline.

**On this branch** (`postgres`), that list does *not* include an external database: real PostgreSQL
is the default, `./run.sh` is the whole setup, and `scripts/ensure-postgres.sh` starts it for you
(Docker if you have it, Homebrew otherwise). That's the point of this branch — see the top of this
README. The `generic` branch is the version where the claim above is fully true: in-memory H2, no
network, no service to start, `./mvnw -o spring-boot:run` and nothing else.

That is a scoping decision, not an oversight, and the reasoning is worth being able to state:

> 1. A synchronous in-process call. 2. A relational transaction — where constraints, conditional
> updates, and locks live. 3. A database-backed job table for work outliving the request.
> 4. An external queue, cache, stream, or search cluster **only** when the first three genuinely
> can't satisfy a stated requirement.

In a 50-minute build, each piece of infrastructure above line 3 costs setup time, adds a failure
mode you have to explain, and moves your critical invariant somewhere you can't demo. If the design
names Kafka or Redis, implement the behaviour against the database and describe the scaling path in
[`docs/ARCHITECTURE_TEMPLATE.md`](docs/ARCHITECTURE_TEMPLATE.md). "I'd put this behind a queue past
X throughput; below that it's a transaction" beats a half-wired broker.


## Profiles

| Profile | Use |
|---|---|
| `postgres` (default) | real PostgreSQL from `docker compose`, seeded on boot — what you demo on |
| `test` | the same container, separate `sandbox_test` database, no seed data, quiet logs |
| `h2` | escape hatch for when there is no container runtime — see the caveat below |

**Schema:** Hibernate generates it from the entities, `create-drop` on every profile. There is no
Flyway or Liquibase, deliberately: in a 50-minute build the schema changes every few minutes, and
hand-writing a migration for each change buys nothing when the database is thrown away on restart.
Say that if asked — "versioned migrations from the first release, generated schema while the model
is still moving" is the honest answer. `update` was rejected too: it silently declines the changes
it can't apply (narrowing a column, adding `NOT NULL` to a populated table), which leaves you
debugging a schema that doesn't match your code.

**Connection pools are small on purpose** — 10 for the app, 20 for the suite. A pool larger than the
container's `max_connections` turns a spike into connection errors instead of queueing, and a big
pool hides a lock you're holding across a whole request. The test pool matters more than it looks:
a `Concurrently.run(N, ...)` with `N` above the pool size serialises on connection acquisition and
stops being a concurrency test at all, while still passing. Raise the pool before raising `N`.

### The H2 escape hatch

```bash
./mvnw -o spring-boot:run -Dspring-boot.run.profiles=h2
```

H2 runs in `MODE=PostgreSQL`, which reduces the common syntax differences — identifier quoting,
`LIMIT`/`OFFSET`, `COALESCE`, sequence and identity declarations — so ordinary JPA and JPQL written
here won't need rewriting. It is a compatibility mode, not an emulator: **locking behaviour,
constraint enforcement, transaction isolation, and any native SQL still have to be verified against
a real PostgreSQL** before you rely on them. It gets the UI up when Docker isn't available; it does
not validate a single claim about concurrency. Say that rather than claiming portability.

The test suite has no H2 mode on this branch — it points at `sandbox_test` and fails to connect
without the container. That is deliberate: a green suite on H2 would be evidence about the wrong
database.
