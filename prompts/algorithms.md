# Prompt: algorithm / logic-heavy piece

Use for a non-CRUD chunk of business logic (scoring, matching, scheduling, etc.) where you want
a plan before code.

```
Before writing code: restate the problem in your own words, list the inputs/outputs and their
types, state the complexity target if there is one, and enumerate edge cases (empty input, single
element, duplicates, ties, invalid input).

Then implement it as a plain method/class, no framework dependencies, so it's unit-testable in
isolation. Ask before introducing a new dependency.
```

Follow-up once code exists:

```
Given this implementation, what's the time/space complexity, and is there a simpler approach that
trades a bit of performance for clarity? I'd rather keep it readable unless the target explicitly
needs the faster version.
```
