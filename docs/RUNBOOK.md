# The 50-minute AI round — runbook

Read `prompts/README.md` for the mechanics. This is the plan around them.

---

## Pre-flight (do this before the call, not during)

Two steps, in this order. Step 1 must run **online** — it is what makes step 2 possible.

```bash
cd sandbox

# 1. ONLINE, once. Populates ~/.m2 with every dependency.
./mvnw clean test           # expect: Tests run: 25, Failures: 0, Errors: 0

# 2. OFFLINE preflight. Proves ~/.m2 is complete and the interview commands will work
#    with no network at all. If this fails, go back to step 1 — do not debug it live.
./mvnw -o clean test        # expect: same 25, ~4s
./mvnw -o spring-boot:run   # then open http://localhost:8080
```

`-o` downloads nothing; it only succeeds once every artifact is already cached. A fresh machine, a
cleared `~/.m2`, or **any `pom.xml` change** breaks it until you run online again. That's why the
offline run is a verification step and not just a habit.

- [ ] Online run done, then `./mvnw -o clean test` passes with **25 tests**
- [ ] App boots, UI shows three seeded cards and a declined transaction
- [ ] `/` and `/swagger-ui.html` both load, and `/v3/api-docs` returns JSON (not a 500)
- [ ] Claude Code open at the **repo root** so it picks up `CLAUDE.md`
- [ ] `prompts/00-kickoff.md` open in a tab, ready to paste
- [ ] Swagger and H2 console `/h2-console` open in tabs
- [ ] Screen share rehearsed: editor, browser, terminal all visible without alt-tab hunting
- [ ] Working from `main` on a clean tree, and you know the checkpoint habit below

During the call, keep `-o` on every Maven command: it removes dependency-resolution latency and any
dependence on the network holding up. Drop it for one run after editing `pom.xml`.

---

## Minute by minute

| Minutes | Do |
|---|---|
| 0–5 | **Agree the features.** Negotiate actively (below). Write them in a scratch file. |
| 5–8 | Paste `00-kickoff.md`. Review the data model. Approve or correct it. Nothing else. |
| 8–12 | Entities + repositories only. Run `./mvnw -o test`. Green before continuing. |
| 12–22 | Feature 1 vertical: service → controller → curl → UI config. **Demo it, then checkpoint.** |
| 22–32 | Feature 2, same loop. Checkpoint. |
| 32–40 | Feature 3, same loop. Checkpoint. |
| 40–45 | Tests on the rule that matters (`04-tests.md`). |
| 45–50 | `05-endgame.md` — demo pass, README, the closing narrative. |

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

This is leverage, not a formality. You are allowed to shape scope, and shaping it well is itself
graded under Product Mindset.

- **Propose the vertical, not the surface.** "Rather than five endpoints, I'd do three features
  that each work end to end — data, API, and UI." That's the definition-of-done answer, delivered
  before you're asked.
- **Steer toward what you can prove.** One creation flow, one rule that enforces something (a limit,
  a state transition, an approval), one list/detail view with a real aggregate. That's a complete
  product story and it's the shape this scaffold already runs.
- **Name a cut yourself, immediately.** "I'll skip auth and treat the caller as an authenticated
  admin — say if you'd rather I spend time there." Volunteering the boundary reads as judgment.
  Being caught not having thought about it reads as an oversight.
- **Ask what "done" means to them.** "Does done here mean the API works, or that I can click
  through it?" Then build to that answer instead of guessing.

If the domain is corporate cards / spend / billing, the seeded `card` + `transaction` slice is
already the right shape — say so, keep it, and extend. If it's a different domain, delete both
packages and let `00-kickoff.md` regenerate the model; the common layer, error handling, config UI,
and test templates all carry over unchanged.

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

Over-engineering is the named #1 failure. Concretely, in this round, it means:

| Trap | Instead |
|---|---|
| Interface + impl for one service | Concrete `@Service` |
| A generic `BaseEntity` / `AbstractCrudService` | Copy the five files |
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
one file and one hypothesis (`prompts/03-debug.md`).

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
files the agent created behind. If you have not committed since the last green state, there is
nothing to go back to.

Losing four minutes of work beats losing the demo.

**You're behind at minute 35:** drop the third feature, announce it — *"I'd rather hand you two
features that work than three that don't"* — and spend the time making two solid. That sentence is
a better answer than the third feature would have been.
