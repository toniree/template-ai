# Quick prompts

Four prompts for the whole interview. This is the file to have open — the numbered files are the
long-form versions to reach for when one of these needs more structure.

---

## 1 · Kickoff

Paste once, right after you hear the problem.

```
Problem, verbatim: "<paste it exactly>"

~50 minutes, solo, this Spring Boot + H2 scaffold. Read the repo first. No code yet.

Give me, terse, in this order:
1. The 3 questions worth asking the interviewer, ranked by how much the answer changes the build.
2. The assumptions I should state out loud for everything else — one line each.
3. The MVP: smallest feature set that demos end to end. What's cut, and which cuts are production
   follow-ups I should name rather than build.
4. The data model and API for that MVP only — entities with field types and nullability, indexes
   with a reason, endpoints as METHOD /path -> status codes, and which request fields must be
   boxed or non-blank.
5. Build order: each feature as an independently demoable vertical slice, minutes each. If the
   total doesn't fit, say what to drop.
6. Does the sample `task` package get renamed and adapted, or kept as reference while I build
   something different?

No preamble.
```

Ask the top two questions. State the assumptions out loud. Correct the model, then build.

---

## 2 · One vertical slice

One per feature. Never two features in one prompt.

```
Build <feature> as one complete vertical slice. Read the `task` package and follow its patterns.

Behaviour:
- <rule, as input -> outcome>
- <edge case -> what happens>

API: <METHOD> /api/<path> -> <status codes>

Slice means: only the persistence this behaviour needs (none if it stores nothing), the service
rule, the endpoint, one integration test modelled on TaskApiIT (happy path + the rule + one 400 +
one 404), and the smallest UI change to click it. Not all the entities first.

Validation goes on the request record, business rules in the service, errors via
ApiException.notFound/badRequest/conflict, entities mapped to a response record inside the
transaction. Box numeric/boolean request fields. Drop any of the five usual files that would have
nothing to do, and say which.

No service interface, no base class, no mapper, no endpoints I didn't list, no new dependency —
ask first. Don't touch common/ or the config.

Run ./mvnw -o test and report the real count and result. Then stop.
```

Then: curl it, click it, `git add -A && git commit -q -m "feat: <feature>"`.

---

## 3 · Debug — evidence first, 3-minute cap

```
<paste the ENTIRE error output or stack trace, including every "Caused by">

Expected: <what should happen>
Actual:   <what happens>
Repro:    <the exact curl or click> -> <the exact response>
I just changed: <what>

Read the relevant file before proposing anything. Give me the single most likely cause and the
smallest fix. Don't refactor anything adjacent, and don't guess at a file's contents — name it and
I'll paste it.
```

At 3 minutes, stop debugging and buy your way out:

```
This has eaten 3 minutes and I have <N> left. What's the simplest version of this behaviour that
definitely works, even if it's uglier? Implement that and tell me in one line what we traded.
```

Still stuck: `git reset --hard HEAD` (`git clean -fd` if new files appeared) back to the last
checkpoint, and take a different route. Say the trade out loud — choosing the boring path under
time pressure is the senior signal.

---

## 4 · Endgame

At T-10, whatever you're mid-way through:

```
<N> minutes left. State: <what works, what's half-done>.

1. Tell me what I can actually land in <N-5> minutes and what to abandon. Bias hard toward
   finishing what exists. "Land nothing new" is a valid answer — and revert anything half-built
   rather than leaving it broken.

2. Then verify, and report ONLY what you actually ran: ./mvnw -o test with the real count and
   result; a curl of every endpoint including one validation failure and one unknown id, with the
   real status codes; /swagger-ui.html and /v3/api-docs loading; the UI listing, creating, and
   showing an API error. Flag anything that failed — don't fix it yet.

3. Then the demo script: the exact clicks and curls that show each feature working, in an order
   that tells a story, including one rejected request. I'll walk it once before showing it.

4. Then three sentences I can say: one defending the data model, one naming the biggest thing I
   deliberately didn't build and why that was right for the time, one honest weakness and what I'd
   do instead.
```

In the last ten minutes, do not: refactor, rename, upgrade a dependency, start another feature, or
run a formatter. Every one of those has ended an interview one commit short of working.
