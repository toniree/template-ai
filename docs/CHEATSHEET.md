# Cheatsheet

-manual cp/pasta/summary -
## Concurrency control

```java
// 1. A constraint — the invariant becomes unrepresentable. No application logic to get wrong.
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_x_key", columnNames = "key"))
// a violation surfaces as DataIntegrityViolationException -> 409, already handled

// 2. An atomic conditional write — one round trip, no lock. 0 rows affected means "rejected".
//    Note the predicate: `:n <= a.cap - a.used`, NOT `a.used + :n <= a.cap`. The second one
//    overflows for a large enough :n, wraps negative, passes the check, and stores garbage.
@Modifying
@Query("update Account a set a.used = a.used + :n where a.id = :id and :n <= a.cap - a.used")
int consume(@Param("id") Long id, @Param("n") long n);

// 3. Optimistic locking — conflicts are rare, the loser retries or gets a 409.
@Version private long version;   // on the entity; the collision raises
                                 // OptimisticLockingFailureException -> 409, already handled

// 4. Pessimistic locking — real contention, and a retry loop would be worse. Costs you
//    serialized access to that row for the rest of the transaction.
@Lock(LockModeType.PESSIMISTIC_WRITE)          // keep it a DERIVED finder, see the gotcha below
Optional<Account> findWithLockById(Long id);
```

Whichever you pick, say why that one and not the others — the reasoning is the signal, not the
mechanism. If you use a lock, call it from inside the service's existing `@Transactional` method,
and confirm `for update` is in the logged SQL before you believe it.



## Production evolution

What changes at scale, and at roughly what point. Keep the ordering honest — most of these are
unnecessary until well past the traffic the interviewer described.

| Pressure | Response |
|---|---|
| Read volume on the hot query | Index; then cache; then read replica |
| Text search beyond `LIKE` | Dedicated search index |
| Slow work in the request path | DB-backed job table; queue only if that stops being enough |
| Multiple app instances | Already fine — enforcement is in the database, not in-process |

## HTTP status codes

| Code | Use it for |
|---|---|
| 200 | read, update |
| 201 | created — with a `Location` header |
| 204 | delete succeeded, no body |
| 400 | failed `@Valid`, malformed JSON, unusable input |
| 404 | unknown id |
| 409 | state conflict, duplicate, illegal transition |
| 422 | well-formed but semantically invalid (alternative to 400) |
| 500 | never on purpose |

A request the system understood and deliberately answered "no" to is a **2xx carrying that
outcome**, not a 4xx. Reserve 4xx for requests you could not process at all.


## Gotchas that cost minutes

- **`LazyInitializationException`** — you mapped to a DTO outside the service. `open-in-view` is
  off deliberately; do the mapping inside the `@Transactional` method.
- **`*IT.java` not running** — fixed here via widened Surefire includes, but it's stock Maven
  behaviour to skip them. Always check the test count, not just `BUILD SUCCESS`.
- **Primitive fields in a request record** — `long`/`int`/`boolean` default to `0`/`false` when the
  caller omits them, and `@PositiveOrZero` happily accepts `0`. A partial `PATCH` then silently
  wipes a field. Box them (`Long`) and use `@NotNull` for required.
- **A catch-all `@ExceptionHandler(Exception.class)`** intercepts Spring MVC's own exceptions, so
  bad path variables, bad query params, unknown routes, and wrong verbs all become 500 instead of
  400/404/405. Extend `ResponseEntityExceptionHandler` and override only what you want to reshape.
- **`@Lock` silently doing nothing** — on this stack, `@Lock` on a derived finder emitted
  `for update`, but the same annotation on an explicit `@Query` did not, with no warning or error.
  Treat that as "verify, don't assume": whenever a lock is load-bearing, check `for update` is
  actually in the logged SQL. A read-modify-write you believe is protected but isn't will still race.
- **A green concurrency test that proves nothing** — always check it *fails* when you remove the
  fix. Thread timing makes false passes common, especially inside a larger suite.
- **Tests interfering** — the suite shares one H2 database. Assert on rows you created, never on
  table-wide counts.
- **Jackson can't deserialize a record** — JSON field names must match the record components exactly.
- **Schema didn't change** — `ddl-auto: create-drop` rebuilds on restart; if you edited an entity
  while the app was running, restart it.
- **UI shows nothing, API is fine** — the field name you're reading in `app.js` doesn't match the
  JSON. Check the network tab before touching the backend.
