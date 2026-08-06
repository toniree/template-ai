# template-ai

A domain-neutral Spring Boot starter for AI-assisted coding interviews. It boots with a working
database, REST API, typed errors, Swagger, a small frontend, and a fast test suite — so the hour
goes into the problem you were actually given, not into scaffolding.

**Java 21 · Spring Boot 3.4 · H2 in-memory · vanilla-JS UI, no build step**

```bash
cd sandbox
./mvnw clean test             # ONCE, online — populates ~/.m2 (see below)
./mvnw -o spring-boot:run     # http://localhost:8080 — seeded and clickable
./mvnw -o test                # whole suite
```

**Run Maven online at least once before relying on `-o`.** The `-o` (offline) flag doesn't
download anything — it only works once every dependency is already in `~/.m2`. On a fresh machine,
or after any `pom.xml` change, `-o` fails with unresolvable artifacts. Do the online run as part of
your pre-flight, not on interview morning; [`docs/RUNBOOK.md`](docs/RUNBOOK.md) has the checklist.

## Start here

| | |
|---|---|
| [`prompts/QUICK.md`](prompts/QUICK.md) | Four prompts covering the whole interview. Keep this one open. |
| [`CLAUDE.md`](CLAUDE.md) | The working agreement the agent reads every session. Read it once yourself. |
| [`docs/RUNBOOK.md`](docs/RUNBOOK.md) | Pre-flight, minute-by-minute plan, checkpoints, recovery moves. |
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

Auth, authorization, rate limiting, pagination, caching, async/queues, containerisation, an
external database, a frontend build step, and a CI pipeline.


## Profiles

| Profile | Use |
|---|---|
| `h2` (default) | in-memory, seeded on boot, zero setup — what you demo on |
| `test` | isolated in-memory database, no seed data, quiet logs |

H2 runs in `MODE=PostgreSQL`, which reduces the common syntax differences — identifier quoting,
`LIMIT`/`OFFSET`, `COALESCE`, sequence and identity declarations — so ordinary JPA and JPQL written
here won't need rewriting later. It is a compatibility mode, not an emulator: **locking behaviour,
constraint enforcement, transaction isolation, and any native SQL still have to be verified against
a real PostgreSQL** before you rely on them. Say that rather than claiming portability.
