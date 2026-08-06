# Architecture notes

Fill this in as you build — it is the page you talk from in the last ten minutes, and the thing a
reviewer reads first if the code is sent on afterwards. Keep it to one screen.

Anything here that is aspirational must say so. A design doc that describes what you *would* do as
though you did it is the fastest way to lose the room.

---

## What this is

One paragraph: the problem, and what the running app actually does today.

## Scope

**Built:**

**Deliberately not built:** *(with the one-clause reason each)*

## Entity model

```
Entity
  field: type          -- note anything non-obvious
  otherId -> Other     -- @ManyToOne(LAZY)
```

Why the model is shaped this way. Most usefully: what you flattened. "Artist and venue are fields
on Event, not their own tables — nothing in the requirements manages them independently."

## API

| Method | Path | Returns | Notes |
|---|---|---|---|
| | | | |

Note where a status code is doing real work: 409 for a conflict, 2xx carrying a business rejection,
404 on a nested collection whose parent is unknown.

## The critical invariant

The one rule that must never break, and the mechanism that enforces it.

- **Invariant:**
- **Mechanism:** *(DB constraint / conditional update / optimistic lock / pessimistic lock)*
- **Why this one:**
- **Proof:** *(the concurrent test, and what it asserts)*

Be specific about where enforcement lives. "The check and the write are one conditional `UPDATE`,
so two concurrent callers cannot both succeed; zero rows affected becomes a 409" is a senior
answer. "The service checks availability first" is not.

## Consistency and transactions

Where the transaction boundaries are and why. Anything read-then-write, and how it is made safe.

## Time

What depends on the clock — expiry, cutoffs, TTL — and how it is tested. (`Clock` bean, advanced by
`MutableClock` in tests, never `Instant.now()`.)

## Identity

`X-User-Id` via `CurrentUser` is a development stand-in for an authenticated principal, not
authentication: the caller supplies it, so any caller can claim any identity. In production it
comes from a verified token or session, and `CurrentUser.find()` is the only method that changes.

## Known gaps

Real ones, named. A stated gap costs far less than a discovered one.

-

## Production evolution

What changes at scale, and at roughly what point. Keep the ordering honest — most of these are
unnecessary until well past the traffic the interviewer described.

| Pressure | Response |
|---|---|
| Read volume on the hot query | Index; then cache; then read replica |
| Text search beyond `LIKE` | Dedicated search index |
| Slow work in the request path | DB-backed job table; queue only if that stops being enough |
| Multiple app instances | Already fine — enforcement is in the database, not in-process |
