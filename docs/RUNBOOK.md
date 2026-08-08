# The AI-assisted coding round — runbook

Written for a ~50-minute live interview where you drive an AI agent. Read `prompts/README.md` for
the prompts; this is the plan around them.

---

## Pre-flight (do this before the call, not during)

Two steps, in this order. Step 1 must run **online** — it is what makes step 2 possible.

```bash
cd sandbox

# 1. ONLINE, once. Populates ~/.m2 with every dependency.
./mvnw clean test           # expect: Failures: 0, Errors: 0 — write down the test count

# 2. OFFLINE preflight. Proves ~/.m2 is complete and the interview commands will work
#    with no network at all. If this fails, go back to step 1 — do not debug it live.
./mvnw -o clean test        # same count, same result
./mvnw -o spring-boot:run   # then open http://localhost:8080
```

Write the count down rather than trusting a number in a doc: it changes the moment you add or
delete a test, and its only job is to let you notice tests that have silently stopped running.

`-o` downloads nothing; it only succeeds once every artifact is already cached. A fresh machine, a
cleared `~/.m2`, or **any `pom.xml` change** breaks it until you run online again. That's why the
offline run is a verification step and not just a habit.

- [ ] `docker compose up -d` from the repo root, container reports healthy
- [ ] Online run done, then `./mvnw -o clean test` passes offline with the same test count
- [ ] App boots on the `postgres` profile and the UI lists the seeded tasks
- [ ] `docker` daemon starts on login (or you know the one command that starts it)
- [ ] `/` and `/swagger-ui.html` both load, and `/v3/api-docs` returns JSON (not a 500)
- [ ] Agent open at the **repo root** so it picks up `CLAUDE.md`
- [ ] `prompts/QUICK.md` open in a tab, ready to paste
- [ ] Swagger open in a tab, and a `docker compose exec postgres psql -U postgres sandbox` shell ready
- [ ] Screen share rehearsed: editor, browser, terminal all visible without alt-tab hunting
- [ ] Clean git tree, and you know the checkpoint habit below

During the call, keep `-o` on every Maven command: it removes dependency-resolution latency and any
dependence on the network holding up. Drop it for one run after editing `pom.xml`.

---

## Minute by minute

| Minutes | Do |
|---|---|
| 0–5 | **Agree the features.** Negotiate actively (below). Write them in a scratch file. |
| 5–10 | Paste the kickoff prompt from `prompts/QUICK.md`. Review and correct the model and API. No code yet. |
| 10–22 | **Feature 1 as one complete vertical slice.** Then demo it and checkpoint. |
| 22–32 | Feature 2, same loop. Checkpoint. |
| 32–40 | Feature 3, same loop. Checkpoint. |
| 40–45 | Tests on any rule still uncovered. Then `06-scope-review.md`. |
| 45–50 | `07-endgame.md` — demo pass, README, the closing narrative. |

### What "one complete vertical slice" means

Approving the model on paper is worth the five minutes. **Building it as a horizontal phase is
not.** Do not write all the entities and repositories first: that produces ten minutes with nothing
runnable, and it commits you to a schema you haven't exercised yet — the fields you got wrong stay
invisible until something finally calls them.

Slice one is the smallest thing that works end to end:

1. persistence for *this* behaviour only — and skip it entirely if the behaviour stores nothing,
2. the service method holding the rule,
3. the endpoint,
4. one integration test: happy path plus the rule,
5. enough UI to click it.

Then `./mvnw -o test`, one curl, and **checkpoint before adding the next behaviour**. The next
slice adds its own fields to the entity when it needs them.

The non-negotiable: **something demoable exists from minute 22 onward.** Never be in a state where
nothing runs.

### Checkpoint after every green feature

This is what makes the recovery move below actually work. The moment a feature is green and
demoable — tests pass, you've clicked through it — commit:

```bash
git add -A && git commit -q -m "feat: <feature>"
```

Ten seconds each, three or four times an hour. It costs nothing and it is the only thing that
turns "this experiment went wrong" into a five-second problem instead of a five-minute one.
Nobody is grading your commit messages.

---

## Negotiating the feature list (minutes 0–5)

This is leverage, not a formality. Shaping scope well is itself part of what's being graded.

- **Propose the vertical, not the surface.** "Rather than five endpoints, I'd do three features
  that each work end to end — data, API, and UI." That's the definition-of-done answer, delivered
  before you're asked.
- **Steer toward what you can prove.** One creation flow, one rule that enforces something (a
  limit, a state transition, an approval), one list or detail view. That's a complete product story
  and it's the shape this scaffold already runs.
- **Name a cut yourself, immediately.** "I'll skip auth and treat the caller as an authenticated
  admin — say if you'd rather I spend time there." Volunteering the boundary reads as judgment.
  Being caught not having thought about it reads as an oversight.
- **Ask what "done" means to them.** "Does done here mean the API works, or that I can click
  through it?" Then build to that answer instead of guessing.

On the sample `task/` package: if the problem is a persisted resource with a status, **rename and
adapt it** rather than starting over. If it's a different shape, **leave it running** while you
build — it's your reference for the patterns and your fallback demo. Delete it once your first real
slice, the frontend, and a repointed `ErrorContractIT` are all green, then checkpoint. Deleting it
up front buys nothing and costs you a working app.

---

## While the agent works

Dead air is wasted evaluation time. Talk through:

- what you asked for and the constraint you put on it ("I told it not to add a service interface")
- the tradeoff you just made and what would change your mind
- what you're going to check the moment it finishes

Then **actually read the diff before running it.** Accepting code you haven't read is the failure
mode this round is designed to detect. Reading it out loud is the demonstration.

---

## Guardrails against the biggest mistake

Over-engineering is the most common way this round is lost. Concretely:

| Trap | Instead |
|---|---|
| Interface + impl for one service | Concrete `@Service` |
| A generic `BaseEntity` / `AbstractCrudService` | Copy the concrete files |
| All entities and repositories up front | One vertical slice at a time |
| Building the schema for features 4–5 while doing 1 | Model only what feature 1 needs |
| Adding a cache/queue "for scale" | Say the number that would justify it, then don't |
| Perfecting feature 1 while 2 and 3 don't exist | Ship all three at 80% |
| Refactoring at minute 44 | Freeze. Demo what works. |

If you catch yourself building an abstraction, ask: *is there a second caller today?* If no, inline
it and move on.

---

## Recovery moves

**Agent produced something over-built:**
> "Too much. Delete the interface and the mapper, put the logic directly in the service, keep the
> behaviour identical."

**Agent is thrashing on a bug:** stop prompting, read the stack trace yourself, then give it the
one file and one hypothesis (`prompts/05-debug.md`).

**Nothing compiles and you're lost:** throw the experiment away and go back to your last
checkpoint commit:

```bash
git reset --hard HEAD        # discards ALL uncommitted work, tracked and staged
git clean -fd                # only if the mess included new files
```

Then re-run `./mvnw -o test` to confirm you're actually green again before continuing.

This only rescues you as far back as your **last commit** — which is the entire reason for the
checkpoint habit above. `git checkout .` is not a substitute: it reverts tracked files to the
index, so it cannot recover a green state you never committed, and it silently leaves any new
files the agent created behind.

Losing four minutes of work beats losing the demo.

**You're behind at minute 35:** drop the third feature, announce it — *"I'd rather hand you two
features that work than three that don't"* — and spend the time making two solid. That sentence is
a better answer than the third feature would have been.

---

## Production follow-ups: discuss, don't build

Have one sentence ready for each. Naming them unprompted is the signal; implementing them on the
clock is the mistake.

| Not built | What you'd say |
|---|---|
| AuthN/AuthZ | "Caller is treated as a trusted admin. Real version: OIDC at the edge, role checks in the service." |
| Pagination | "List endpoints return everything. First thing I'd add at real row counts — `Pageable` in, a stable envelope out, page size capped." |
| Idempotency on writes | "A client timeout plus a retry is a duplicate today. I'd take a client key, store it under a unique index, and replay the stored response." |
| Concurrency control | "Nothing here has an invariant two requests can break. When one appears, I'd start with a unique constraint or a conditional update, and only reach for `@Version` or a row lock if those don't fit." |
| Schema migrations | "`ddl-auto: create-drop` is a demo affordance. Production is Flyway from day one." |
| Real database | "H2 in PostgreSQL compatibility mode, so the syntax carries over. Locking, constraint enforcement, isolation behaviour and any native SQL would need verifying against real PostgreSQL — plus migrations and pooling." |
| Observability | "One request log line today. Production wants metrics, traces, and structured logs with a correlation id." |
| Rate limiting, caching, async | "None of it earns its complexity at this volume. Here's the number that would change my mind." |
