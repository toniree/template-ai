# Prompt: refactor / review under time pressure

Use mid-interview when something works but needs cleanup, or when you want a second opinion
before moving on.

```
Review this <file/method> for:
- correctness edge cases (null/empty input, concurrent updates, invalid IDs)
- unnecessary complexity — anything that could be 3 plain lines instead of an abstraction
- consistency with the rest of the codebase (naming, constructor injection, exception handling)

Don't rewrite it yet — list what you'd change and why, ranked by how much it matters. I'll tell
you which ones to apply.
```

```
Diff my change against the version before it and tell me if I broke anything in
GlobalExceptionHandler or any other shared code.
```

```
I'm about to run out of time on this feature — what's the minimum viable version that still
satisfies the acceptance criteria we agreed on? What am I explicitly cutting, so I can call it out
to the interviewer rather than silently skip it?
```
