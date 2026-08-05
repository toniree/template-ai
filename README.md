# template-ai

A domain-neutral Spring Boot starter for AI-assisted coding interviews. It boots with a working
database, REST API, typed errors, Swagger, a small frontend, and a fast test suite — so the hour
goes into the problem you were actually given, not into scaffolding.

**Java 21 · Spring Boot 3.4 · H2 in-memory · vanilla-JS UI, no build step**

```bash
cd sandbox
./mvnw clean test             # ONCE, online — populates ~/.m2 (see below). 13 tests, ~4s
./mvnw -o spring-boot:run     # http://localhost:8080 — seeded and clickable
./mvnw -o test                # 13 tests, ~3s
```

**Run Maven online at least once before relying on `-o`.** The `-o` (offline) flag doesn't
download anything — it only works once every dependency is already in `~/.m2`. On a fresh machine,
or after any `pom.xml` change, `-o` fails with unresolvable artifacts. Do the online run as part of
your pre-flight, not on interview morning; [`docs/RUNBOOK.md`](docs/RUNBOOK.md) has the checklist.

## Start here

| | |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | The working agreement the agent reads every session. Read it once yourself. |
| [`docs/RUNBOOK.md`](docs/RUNBOOK.md) | Pre-flight, minute-by-minute plan, checkpoints, recovery moves. |
| [`prompts/`](prompts/README.md) | Eight copy-paste prompts, ordered by when you use them. |
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

## Layout

```
CLAUDE.md            agent working agreement — loaded automatically
docs/                runbook, cheatsheet
prompts/             00-kickoff → 07-endgame
sandbox/
  src/main/java/com/templateai/sandbox/
    common/          ApiError, ApiException, GlobalExceptionHandler,
                     AppConfig (Clock), RequestLoggingFilter
    task/            Task, TaskRepository, TaskDtos, TaskService, TaskController
    DemoData.java    seeds the h2 profile so the UI is never empty
  src/main/resources/static/
    app.js           the whole UI — one screen, written directly, no renderer framework
    index.html, styles.css
  src/test/…         TaskApiIT (copy this for new features), ErrorContractIT (keep as-is)
```

One flat package per feature, five files, no `impl/` or `mapper/` layers. Copy the shape.

## Replacing the sample domain

```bash
git rm -r sandbox/src/main/java/com/templateai/sandbox/task \
          sandbox/src/test/java/com/templateai/sandbox/task \
          sandbox/src/main/java/com/templateai/sandbox/DemoData.java
```

Then run [`prompts/00-kickoff.md`](prompts/00-kickoff.md). Everything in `common/`, the error
contract, the config, and the test template carry over unchanged.

For the frontend, change the constants at the top of `static/app.js`, the `<thead>` in
`index.html`, and the cells in `taskRow()`. Two things stay untouched: `ErrorContractIT` (repoint
its paths, keep its assertions) and `common/` (nothing in it knows what a task is).

## Checkpoint after every working feature

```bash
git add -A && git commit -q -m "feat: <feature>"
```

Ten seconds, three or four times an hour. It is the only thing that turns "this experiment went
wrong" into `git reset --hard HEAD` instead of a five-minute salvage job. An uncommitted green
state cannot be returned to.

## Deliberately not here

Auth, authorization, rate limiting, pagination, caching, async/queues, containerisation, an
external database, a frontend build step, and a CI pipeline. Each is the right call for production
and the wrong call for a one-hour scaffold. Discuss them; don't implement them on the clock.

This is **not production-ready**. It is production-*conscious*: the shapes that are hard to retrofit
(error contract, validation, layering, DTO boundaries, injected clock, integration tests) are
already right, and the operational concerns are deliberately absent.

## Profiles

| Profile | Use |
|---|---|
| `h2` (default) | in-memory, seeded on boot, zero setup — what you demo on |
| `test` | isolated in-memory database, no seed data, quiet logs |

H2 runs in `MODE=PostgreSQL`, so the SQL and JPA you write here maps onto a real Postgres later.
There is no external-database profile: nothing in this repo needs infrastructure to run.

## Handing this off as an archive

Build it from git, not by zipping the working tree — `git archive` ships only tracked files, so
`target/`, `.idea/`, `__MACOSX/`, and `.DS_Store` cannot leak in:

```bash
git archive --format=zip --prefix=template-ai/ -o /tmp/template-ai.zip HEAD
unzip -l /tmp/template-ai.zip | grep -E 'target/|\.idea/|__MACOSX|\.DS_Store' || echo "clean"
```

macOS Finder's "Compress" adds a `__MACOSX/` sidecar and resource-fork `._*` files; both are
gitignored here, but the Finder path adds them at zip time regardless. Use the command above.
