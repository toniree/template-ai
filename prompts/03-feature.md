# 03 · One feature, end to end

The workhorse. One prompt per feature, always a full vertical slice, always followed by running it.
Two features in one prompt means you review neither.

A slice is: the persistence this behaviour needs (none, if it stores nothing) → the service rule →
the endpoint → one test → enough UI to click it. Not "all the entities, then all the services".

```
Build <feature name> end to end. Read the `task` package first and follow it exactly.

Behaviour:
- <the rule, stated as input -> outcome>
- <edge case and what should happen>

API: <METHOD> /api/<path>
Returns: <status codes and what each means>

Structure: one flat package. The default for a persisted resource is <Feature>.java,
<Feature>Repository.java, <Feature>Dtos.java, <Feature>Service.java, <Feature>Controller.java —
but drop any of those that would have no responsibility here, and tell me which you dropped and
why. If this genuinely needs one concrete collaborator (an external client, a parser, an
algorithm), add it as a plain class in the same package and say so.

Validation goes on the request record. Business rules go in the service. Errors are
ApiException.notFound/badRequest/conflict — never a try/catch that builds a response. Map to a
response record inside the service transaction. Box numeric and boolean fields on request records
so an omitted field fails @NotNull instead of silently defaulting.

Do not add: an interface for the service, a generic base class, a mapper class, endpoints I did not
list, fields the behaviour above doesn't need, or a new dependency (ask me first). Do not touch
common/, the config, or the frontend.

When it compiles, run ./mvnw -o test and tell me the result. Then stop.
```

## Adding to an existing feature

```
Add <behaviour> to <Feature>Service. Change only that method plus whatever the DTO needs.
Don't touch the controller unless the HTTP contract actually changes. Run the tests after.
```

## Then, the test for it

Do this while the feature is fresh, not at the end:

```
Write one integration test for <Feature>, modelled on TaskApiIT: @SpringBootTest +
@AutoConfigureMockMvc + @ActiveProfiles("test"), MockMvc, real database, no mocks.

Cover exactly:
- the happy path
- <the one business rule that matters>
- one 400 (invalid payload) and one 404 (unknown id)

Create your own fixture rows in each test — the suite shares an H2 database, so never assert on
table-wide counts. Name tests as sentences describing the rule. Run ./mvnw -o test after.
```

## Shapes worth asking for by name

Each is a few lines and reads as senior when it shows up:

```
This list endpoint shows a per-row aggregate. Do it in one grouped query with a projection
interface, not a query per row.
```

Concurrency, only if the requirement has an invariant two simultaneous requests could break — ask
for the analysis before asking for a mechanism:

```
Can two concurrent requests to <endpoint> violate <the stated invariant>? If not, say so and change
nothing. If they can, tell me the cheapest mechanism that prevents it — a unique/check constraint,
a conditional UPDATE, @Version, or a pessimistic row lock — and why the cheaper ones don't fit.
Don't implement it yet.
```

Then, if you've agreed a pessimistic lock is the right one:

```
Add the row lock via a derived finder with @Lock(PESSIMISTIC_WRITE), inside the existing
@Transactional. Then show me the generated SQL so I can confirm `for update` is actually there.
```

```
<field> is an exact quantity — an integer type end to end, never double or float. Compare it
against its limit as `amount > limit - used` so a large input can't overflow past the check.
```

Then: `./mvnw -o test`, one curl, click it in the UI, and
`git add -A && git commit -q -m "feat: <feature>"`.
