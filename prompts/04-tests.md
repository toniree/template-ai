# 04 · Tests

Tests are how you answer "where do you draw the line on production ready" with evidence instead of
opinion. In 50 minutes the answer is **a few tests on the rules that carry money or state**, not
coverage. Say that out loud — the deliberate line is the thing being graded.

Write them after a feature is agreed-done, not before. Run `./mvnw -o test` after each one.

## The default ask

```
Write an integration test for <Feature>, modelled on TransactionApiIT: @SpringBootTest +
@AutoConfigureMockMvc + @ActiveProfiles("test"), MockMvc, no mocks.

Cover exactly:
- the happy path
- <the one business rule that matters, e.g. the limit is enforced / the state transition is rejected>
- one 400 (invalid payload) and one 404 (unknown id)

Create your own fixture rows in each test — the suite shares an H2 database, so never assert on
table-wide counts. Name tests as sentences describing the rule.
```

## When a rule is arithmetic

Money bugs hide in rounding and boundaries. This is worth one prompt:

```
Add boundary cases for <rule>: exactly at the limit, one minor unit over, and zero. Assert on
amounts in minor units, no floating point anywhere in the test.
```

## When you need speed instead of fidelity

A full context boot is ~2s; a slice test is faster and enough for pure controller wiring:

```
Write a @WebMvcTest for <Feature>Controller with the service @MockitoBean'd — status codes and JSON
shape only, no database. Use this only for <endpoint>; the business rules stay in the IT.
```

## Naming

`*Test.java`, `*Tests.java` and `*IT.java` all run under `./mvnw -o test` — the Surefire includes
in `pom.xml` were widened so nothing gets silently skipped. (Stock Surefire ignores `*IT.java`,
which is a classic way to believe you have tests that never execute.)
