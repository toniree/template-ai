# 03 · One feature, end to end

The workhorse. One prompt per feature, always a full vertical slice, always followed by running it.
Two features in one prompt means you review neither.

```
Build <feature name> end to end. Read the `task` package first and follow it exactly.

Behaviour:
- <the rule, stated as input -> outcome>
- <edge case and what should happen>

API: <METHOD> /api/<path>
Returns: <status codes and what each means>

Files: <Feature>.java, <Feature>Repository.java, <Feature>Dtos.java, <Feature>Service.java,
<Feature>Controller.java — one flat package, nothing else.

Validation goes on the request record. Business rules go in the service. Errors are
ApiException.notFound/badRequest/conflict — never a try/catch that builds a response. Map to a
response record inside the service transaction. Box numeric and boolean fields on request records
so an omitted field fails @NotNull instead of silently defaulting.

Do not add: an interface for the service, a mapper class, endpoints I did not list, fields the
behaviour above doesn't need, or a new dependency (ask me first). Do not touch common/, the
config, or the frontend.

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

```
<endpoint> reads state, decides from it, then writes. Take a pessimistic row lock on the read via a
derived finder with @Lock(PESSIMISTIC_WRITE), inside the existing @Transactional. Then show me the
generated SQL so I can confirm `for update` is actually there.
```

```
<field> is an exact quantity — an integer type end to end, never double or float. Compare it
against its limit as `amount > limit - used` so a large input can't overflow past the check.
```

Then: `./mvnw -o test`, one curl, click it in the UI, and
`git add -A && git commit -q -m "feat: <feature>"`.
