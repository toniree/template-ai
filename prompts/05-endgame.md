# 05 · Endgame — the last 10 minutes

The last ten minutes decide how the whole hour is remembered. A finished demo of three features
beats a broken half-fourth every time. Set a timer at T-10 and switch to this file regardless of
what you're mid-way through.

## T-10 · Triage

```
<N> minutes left. Current state: <what works, what's half-done>.

Tell me which of the unfinished work I can actually land in <N-5> minutes and what to abandon.
Bias hard toward finishing what exists over starting anything. If the answer is "land nothing
new, polish what works," say that.
```

If something is half-built, **revert it** rather than leave it broken. A clean three-feature app
demos; a four-feature app with a 500 does not.

## T-8 · Prove it works

```
Write the demo sequence for this app as a numbered list: the exact clicks or curl commands that
show each agreed feature working, in an order that tells a story. Include one failure case
(a rejected/declined/invalid request) — showing that the system says no correctly is the point.
```

Then actually run it once. Do not narrate a path you haven't walked.

## T-5 · The handoff README

Cheap, and it's the artifact that says "production mindset" without you claiming it:

```
Rewrite README.md for someone picking this up cold: what it does, how to run it, the data model
in 5 lines, the endpoints as a table, and a "what I'd do next with more time" section drawn from
what we actually cut. Under 60 lines. No marketing language.
```

## T-3 · Rehearse the two questions you will be asked

**"Is this production ready?"** — Answer with a line, not a hedge. The shape that lands:

> For a demo, yes: input is validated, errors are typed, money is integer minor units, the spend
> endpoint is idempotent, and the rules that move money have tests. For production I'd need
> <auth, an append-only ledger instead of a derived balance, reconciliation against the processor,
> idempotency-key expiry, and rate limits>. I left those out deliberately — they weren't in the
> four features we agreed on.

**"Why did you build it this way?"** — Have one sentence ready for each of: the data model, the
one thing you deliberately didn't build, and the one thing you'd change if you started over.
Owning a weakness reads far stronger than defending everything.

```
Given the code as it stands, give me three sentences: one defending the data model, one naming the
biggest thing I deliberately didn't build and why that was right for 50 minutes, and one honest
weakness with what I'd do instead. Be blunt, no hedging.
```

## Do not, in the last ten minutes

Refactor. Rename. Upgrade a dependency. Start a fifth feature. Run a formatter across the repo.
Every one of these has ended an interview one commit short of working.
