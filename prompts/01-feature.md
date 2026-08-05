# 01 · One feature, end to end

The workhorse. One prompt per feature, always a full vertical slice, always followed by running it.
Two features in one prompt means you review neither.

```
Build <feature name> end to end, following the card/ package exactly.

Behaviour:
- <the rule, stated as input -> outcome>
- <edge case and what should happen>

API: <METHOD> /api/<path>
Returns: <status codes and what each means>

Files: <Feature>.java, <Feature>Repository.java, <Feature>Dtos.java, <Feature>Service.java,
<Feature>Controller.java — one flat package, nothing else.

Validation goes on the request record. Business rules go in the service. Errors are
ApiException.notFound/badRequest/conflict. Map to a response record inside the service.
Box numeric fields on request records (Long, not long) so an omitted field fails @NotNull instead
of silently defaulting to 0.

Do not add: an interface for the service, a mapper class, endpoints I did not list, or fields
outside the ones the behaviour above needs. Stop when it compiles; I'll run it.
```

## Adding to an existing feature

```
Add <behaviour> to <Feature>Service. Change only that method plus whatever the DTO needs.
Don't touch the controller unless the HTTP contract actually changes.
```

## When the rule is a state machine

Money products are full of these. Being explicit here prevents a pile of scattered `if`s.

```
<Entity> has statuses <A, B, C>. Legal transitions: <A -> B, B -> C>. Everything else is a 409 via
ApiException.conflict with a message naming both states.

Put the check in one private method in the service. No state-machine library, no enum strategy
pattern — a switch or an EnumMap of allowed targets is the right size for this.
```

## Fintech shapes worth asking for by name

These read as senior when they show up unprompted, and each is a few lines:

```
Make <endpoint> idempotent on an Idempotency-Key header: unique column, return the stored result
with 200 on replay instead of 201. Follow TransactionService.authorize.
```

```
<listing endpoint> needs pagination — Pageable in, PageResponse.of(...) out, page size capped.
Follow TransactionController.list.
```

```
This list endpoint shows a per-row aggregate. Do it in one grouped query with a projection
interface like TransactionRepository.sumAmountGroupedByCard, not a query per row.
```

```
Amounts here are <currency> — long minor units on the entity, DTO, and API. The UI formats;
nothing else does.
```
