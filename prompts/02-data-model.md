# 02 · Data model and API contract

Run this once, before any implementation. Reworking an entity at minute 30 is the single most
expensive thing that can happen to you; approving a model at minute 8 costs two minutes.

```
Design the data model and API for the MVP we just agreed:
<paste the MVP feature list from 01-scope-to-mvp>

Read the existing `task` package first and follow its shape exactly — one flat package per
feature, five files, DTO records in one <Feature>Dtos.java. Do NOT write code yet.

Give me:

1. DATA MODEL — the minimum set of entities. For each: fields with Java types, nullability, the
   owning side of every relationship, and which columns need an index (and why). Flag any field
   that isn't required by a feature above and either justify it in one line or drop it.

2. API — one line per endpoint: METHOD /path -> status codes. Mark demo-critical vs nice-to-have.
   Follow the conventions already in this repo: 201 + Location on create, partial PATCH not PUT,
   204 on delete, typed errors via ApiException.

3. VALIDATION — per request record, which fields are required and what annotation enforces each.
   Call out any field that must be boxed so an omitted value fails validation instead of
   defaulting to 0/false.

4. THE ONE RISK — the modeling decision most likely to be wrong, and the cheap way to hedge it.

Terse. Tables or bullets, no prose paragraphs.
```

## Then lock it in

Reply with corrections, and get only the persistence layer:

```
Adjust: <corrections>. Now write the entities and repositories only — no services, no controllers,
no DTOs. Then run ./mvnw -o test to confirm the context still starts, and stop so I can review the
model.
```

Locking the schema first makes every later prompt additive. That is what makes the back half of
the hour fast.

## If a rule needs a state machine

Worth being explicit about — it's what stops a pile of scattered `if`s:

```
<Entity> has statuses <A, B, C>. Legal transitions: <A -> B, B -> C>. Everything else is a 409 via
ApiException.conflict with a message naming both states.

Put the check in one private method in the service, written as an exhaustive switch so adding a
status is a compile error rather than a silently-permitted transition. No state-machine library,
no strategy pattern.
```
