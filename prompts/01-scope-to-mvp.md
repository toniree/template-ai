# 01 · Requirements → the smallest MVP

The highest-leverage two minutes of the hour. Turns a wish list into an ordered build plan where
something demoable exists after every step, and produces the cut list you say out loud.

```
Requirements as I understand them:
1. <requirement>
2. <requirement>
3. <requirement>
<add or remove lines — there may be two, there may be six>

Assumptions I've stated to the interviewer: <the ones from 00-kickoff, one line each>

Constraints: ~<N> minutes of build time left, solo, this scaffold. Do NOT write code yet.

Give me:

1. MVP — the smallest set of features that demonstrates the product working end to end. For each,
   one line: what the user can do, and how I'd show it. Cut anything whose absence I could explain
   in one sentence.

2. BUILD ORDER — the MVP features sequenced so each one is independently demoable when it lands.
   Minutes each. If the total exceeds <N-10>, tell me what to drop and stop there rather than
   compressing everything.

3. NOT IN THE MVP — everything from the requirements that didn't make it, one line each, with why.
   Separate the ones that are genuinely out of scope from the ones that are production follow-ups
   I should name out loud but not build.

4. THE RISKIEST ITEM — which single feature is most likely to eat twice its estimate, and the
   simpler version of it I could fall back to.

Terse. No code, no preamble.
```

## When they hand you more scope than fits

Say the sentence, then use this to pick:

```
That's <N> features and I have <M> minutes. Rank them by demo value per minute of build time and
tell me the cut line. I'd rather hand back three that work than five that don't.
```

## Guarding the line later

If you catch yourself building past the agreed MVP, stop and run
[`06-scope-review.md`](06-scope-review.md).
