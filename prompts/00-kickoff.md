# 00 · Kickoff — requirements to a plan you can approve

Run this **once**, immediately after agreeing the feature list, before any code. It converts a
verbal spec into a data model + API contract + explicit cut list. Everything downstream gets
cheaper because the model is settled.

Do not skip to code because the problem "seems obvious." Reworking an entity at minute 30 is the
single most expensive thing that can happen to you.

```
We agreed on these features for a <domain, e.g. corporate card spend management> product:

1. <feature>
2. <feature>
3. <feature>
4. <feature>

Constraints: 50 minutes total, this Spring Boot + H2 scaffold, solo. Working demo matters more
than coverage. Do NOT write any code yet.

Give me, in this order and nothing else:

1. DATA MODEL — the minimum set of entities. For each: fields with types, the owning side of every
   relationship, and which columns get an index. Call out anything that must be a `long` minor-unit
   amount. Flag any field you added that is not required by the four features above, and justify it
   in one line or drop it.

2. API — one line per endpoint: METHOD /path -> status codes. Mark which are needed for the demo
   and which are nice-to-have.

3. BUILD ORDER — the 4 features sequenced so that something demoable exists after each one.
   Estimate minutes each. If the total exceeds 40, say what you'd cut and stop there.

4. CUT LIST — what a production version needs that we are deliberately not building today
   (auth, ledger, reconciliation, rate limits, whatever applies). One line each, no code.
   I will say these out loud to the interviewer, so make them specific to this domain.

5. THE ONE RISK — the single modeling decision most likely to be wrong, and the cheap way to
   hedge it.

Be concrete and terse. No preamble.
```

## Then, before you build

Reply with corrections and lock it in:

```
Adjust: <corrections>. Now write the entities and repositories only — no services, no controllers.
Then stop so I can review the model.
```

Locking the schema first means every later prompt is additive. That is what makes the back half of
the hour fast.
