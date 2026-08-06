# Problem specification

Paste your interview notes into part 1 and let the agent fill part 2. Sections 1–8 are **your**
input and may be rough, partial, or contradictory. The **Derived plan** is what the agent writes
back before it touches code — it is where a misread requirement becomes visible while correcting it
is still free.

**Do not block on completeness.** Missing detail becomes a stated assumption, not a question. Two
or three minutes here, then code.

---

## Part 1 — The problem as given

### 1. Statement
> Design a ___

### 2. Functional requirements
What a user can do. Number them — the agent refers back to these numbers when reporting what it
did and did not build.

1.
2.
3.

### 3. Nonfunctional requirements
Scale, latency, consistency, availability. Most of these are **talking points, not code** in a
50-minute build; the agent should say which ones it is designing for versus noting for later.

-

### 4. Entities
Nouns and their key fields. Relationships if known.

-

### 5. API design
Endpoints if the interviewer named any. **Named endpoints are a contract** — the agent implements
them as named or says loudly that it did not.

```
METHOD /path
```

### 6. Data flow
The path a request takes through the system for the main use case.

### 7. High-level design
Components and how they talk. Boxes and arrows in prose.

### 8. Deep dive
The part the interviewer wants to go deep on — usually the hard one. Concurrency, ranking,
pagination at scale, idempotency, expiry.

---

## Part 2 — Derived plan *(the agent writes this)*

### MVP scope
The smallest set of slices that demonstrates the functional requirements end to end. Ordered, each
independently demoable, each one a checkpoint commit.

1.
2.
3.

### Critical business invariant
**The one thing that must never be violated, in one sentence.** "A seat is booked by at most one
user." "An account balance never goes negative." "A job is claimed by exactly one worker."

Then: which mechanism enforces it — DB constraint, conditional update, optimistic lock, pessimistic
lock — and why that one. This is the single highest-value paragraph on the page; it is what the
interviewer is actually grading, and it drives the concurrency test.

- Invariant:
- Enforced by:
- Proven by: *(the test that fires concurrent attempts — see `Concurrently`)*

### Explicitly out of scope
Named, so it reads as a decision rather than an omission. Each with a one-clause reason.

-

### Assumptions
Gaps filled without asking. State the reading chosen and move on.

-

### Acceptance criteria
Concrete, checkable, tied to the numbered requirements above. This is the definition of done for
the session.

- [ ] Requirement 1: `curl` … returns …
- [ ] Invariant holds under concurrent attempts
- [ ] Invalid input returns 400 with field detail
- [ ] Unknown id returns 404
- [ ] Visible in the UI

### Production extensions
What you would add with real scale and time — described here, **not built**. This is where queues,
caches, search clusters, sharding, and read replicas belong. Having it written down is what lets
you say "I know, and here is exactly what I would do" instead of building it at minute 40.

-
