# Cheatsheet

Keep open in a tab. Ordered by how often you'll reach for it.

## Commands

```bash
cd sandbox
./mvnw -o spring-boot:run                                  # run (offline = fastest)
./mvnw -o test                                             # whole suite
./mvnw -o test -Dtest=TaskApiIT                            # one class
./mvnw -o test -Dtest=TaskApiIT#createsThenReadsBackATask  # one method
./mvnw -o clean compile                                    # compile only, no tests
```

`/swagger-ui.html` · `/actuator/health` · psql:
`docker compose exec postgres psql -U postgres sandbox`

Bring the database up first, from the repo root: `./scripts/ensure-postgres.sh` (or just
`./run.sh`, which calls it before booting the app). It starts Docker Compose if you have Docker,
otherwise `postgresql@16` via Homebrew, and creates `sandbox`/`sandbox_test` if missing.

**If Docker dies mid-interview**, don't lose the suite — override the datasource on the command
line, which beats the profile's values without editing a file:

```bash
./mvnw -o test '-Dspring.datasource.url=jdbc:h2:mem:sanity;MODE=PostgreSQL;DB_CLOSE_DELAY=-1' \
  -Dspring.datasource.username=sa -Dspring.datasource.password= \
  -Dspring.datasource.driver-class-name=org.h2.Driver
./mvnw -o spring-boot:run -Dspring-boot.run.profiles=h2
```

Say that you fell back, and that nothing about locking or constraint timing is proven by that run.

## curl

```bash
curl -s localhost:8080/api/tasks | python3 -m json.tool

curl -s -D- -X POST localhost:8080/api/tasks -H 'Content-Type: application/json' \
  -d '{"title":"Ship it","description":"end to end"}'

curl -s -X PATCH localhost:8080/api/tasks/1 -H 'Content-Type: application/json' \
  -d '{"status":"DONE"}'

curl -s 'localhost:8080/api/tasks?status=DONE'
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE localhost:8080/api/tasks/1
```

`-D-` prints headers (that's how you check `Location` on a 201). `-w '\nHTTP %{http_code}\n'`
appends the status to a body you also want to see.

## Spring annotations

| Annotation | Use |
|---|---|
| `@RestController` + `@RequestMapping("/api/x")` | JSON controller, base path |
| `@GetMapping @PostMapping @PatchMapping @PutMapping @DeleteMapping` | routes |
| `@RequestBody` `@PathVariable` `@RequestParam` `@RequestHeader` | bind body / URL segment / query param / header |
| `@Valid` | trigger Bean Validation on a `@RequestBody` |
| `@Service` | business logic; inject via constructor, no `@Autowired` needed |
| `@Transactional` | on the service class; `readOnly = true` on read methods |
| `@RestControllerAdvice` + `@ExceptionHandler` | central error handling |
| `@Entity @Id @GeneratedValue(strategy = IDENTITY)` | JPA entity + PK |
| `@ManyToOne(fetch = LAZY)` + `@JoinColumn` | the owning side of a relationship |
| `@Enumerated(EnumType.STRING)` | store enum names, never ordinals |
| `@Table(uniqueConstraints = …, indexes = …)` | declare constraints and indexes on the entity |
| `@Getter @Setter @NoArgsConstructor` | Lombok on entities — **never `@Data`** (breaks equals/hashCode with proxies) |
| `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")` | integration test |
| `@WebMvcTest(X.class)` + `@MockitoBean` | controller slice test (`@MockBean` is deprecated in Boot 3.4) |
| `@Profile("h2")` | bean only under that profile — how `DemoData` stays out of tests |

## Validation

`@NotNull` `@NotBlank` `@Positive` `@PositiveOrZero` `@Size(min,max)` `@Pattern(regexp)` `@Email`
`@Min` `@Max` `@Future` `@PastOrPresent`

Put them on the request record, never in a service `if`. **`@Pattern` and `@Size` both pass on
`null`; `@NotBlank` and `@NotNull` both reject it.** That difference is the whole trick for PATCH.

| The field is… | Use |
|---|---|
| Required, must have content | `@NotBlank` |
| Required, may be any value including empty | `@NotNull` |
| Optional (PATCH), but must have content if sent | `@Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")` |
| Optional (PATCH), may be empty if sent | nothing, or `@Size(max = n)` |

`@Size(min = 1)` is **not** a blank check — it counts characters, so `"   "` has length 3 and
passes. Use the pattern above when whitespace-only must be rejected; `TaskDtos.UpdateTaskRequest`
is the worked example, and `TaskApiIT.rejectsAPatchWhoseTitleIsOnlyWhitespace` is the test.

## Spring Data queries

```java
// derived — no SQL needed
Optional<Task> findByTitle(String title);
List<Task> findByStatusOrderByIdAsc(Task.Status status);
List<Task> findByStatusAndCreatedAtAfter(Task.Status s, Instant since);
boolean existsByTitle(String title);
long countByStatus(Task.Status status);

// built in, no method needed
tasks.findAll(Sort.by(Sort.Direction.ASC, "id"));

// aggregate in the DB, never in Java
@Query("select coalesce(sum(o.quantity), 0) from Item o where o.order.id = :id")
long totalFor(@Param("id") Long id);

// one grouped query instead of N — projection interface, no class needed
@Query("select i.order.id as orderId, sum(i.quantity) as total from Item i group by i.order.id")
List<OrderTotal> totals();
interface OrderTotal { Long getOrderId(); long getTotal(); }
```

Keywords: `Containing` `IgnoreCase` `Between` `LessThan` `GreaterThanEqual` `In` `IsNull` `Not`
`OrderBy…Asc/Desc` `Top10` `Distinct`.

## Pagination, if a problem asks for it

Not in the scaffold — it's ~15 lines when you need it, and dead weight when you don't.

```java
// controller: cap the size so one caller cannot ask for the whole table
@GetMapping
public PageResponse<TaskResponse> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
    return service.list(PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100)));
}

// never return Spring's Page directly — it serializes its internals and is not a stable contract
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    static <E, T> PageResponse<T> of(Page<E> p, Function<E, T> map) {
        return new PageResponse<>(p.getContent().stream().map(map).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
```

## Concurrency control, when a requirement needs it

First ask whether concurrent requests can actually break an invariant the problem states. Usually
they can't, and the answer is to do nothing and say so. When they can, work down this list and stop
at the first one that fits:

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

## Coming from SQL Server — what differs

H2 runs in `MODE=PostgreSQL` here, which smooths over the common **syntax** differences below. It
does not make H2 into PostgreSQL: locking behaviour, constraint enforcement, transaction isolation,
and anything written as native SQL still need verifying against a real PostgreSQL. Treat the table
as "what to type", not "what will behave identically".

| T-SQL | Postgres / H2 |
|---|---|
| `TOP 20` | `LIMIT 20` |
| `OFFSET n ROWS FETCH NEXT m ROWS ONLY` | `LIMIT m OFFSET n` |
| `ISNULL(a, b)` | `COALESCE(a, b)` |
| `GETUTCDATE()` | `NOW() AT TIME ZONE 'utc'` |
| `IDENTITY(1,1)` | `GENERATED BY DEFAULT AS IDENTITY` / `BIGSERIAL` |
| `[bracketed]` identifiers | `"double-quoted"` |
| `+` for string concat | `\|\|` or `CONCAT()` |
| `MERGE` | `INSERT … ON CONFLICT … DO UPDATE` |
| `WITH (UPDLOCK)` | `SELECT … FOR UPDATE` |
| `DATETIME2` | `TIMESTAMPTZ` (map to `java.time.Instant`) |
| `NEWID()` | `gen_random_uuid()` |

`NOLOCK` has no equivalent and isn't needed — Postgres readers don't block writers (MVCC).

## SQL you'll want at psql

```sql
SELECT * FROM tasks ORDER BY id;
SELECT status, COUNT(*) FROM tasks GROUP BY status;

CREATE INDEX ix_tasks_created ON tasks (created_at);
ALTER TABLE tasks ADD COLUMN assignee VARCHAR(120);

-- "at most one ACTIVE row per key" — the invariant a plain unique index gets wrong,
-- because it would also collide on the cancelled rows.
CREATE UNIQUE INDEX ux_tasks_one_active ON tasks (assignee) WHERE status = 'IN_PROGRESS';

-- who is blocking whom, when a demo hangs
SELECT pid, wait_event_type, wait_event, left(query, 60) FROM pg_stat_activity WHERE state <> 'idle';
SELECT pg_blocking_pids(pid), pid, left(query, 60) FROM pg_stat_activity WHERE cardinality(pg_blocking_pids(pid)) > 0;
```

## Postgres patterns worth naming out loud

```sql
-- a work queue on the database you already have; the answer to "why not Kafka"
SELECT * FROM jobs WHERE status = 'PENDING' ORDER BY id FOR UPDATE SKIP LOCKED LIMIT 1;

-- idempotency / upsert, atomic. Beats exists()-then-save, which is a race.
INSERT INTO payments (idempotency_key, ...) VALUES (?, ...) ON CONFLICT (idempotency_key) DO NOTHING;

-- the invariant that survives every bug above it
ALTER TABLE accounts ADD CONSTRAINT balance_non_negative CHECK (balance_minor >= 0);
```

Money is `BIGINT` in minor units (or `NUMERIC(19,4)`), never `double`. Compare a limit as
`amount > limit - used`, never `used + amount > limit`.

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
- **Tests interfering** — the suite shares one database. Assert on rows you created, never on
  table-wide counts.
- **Jackson can't deserialize a record** — JSON field names must match the record components exactly.
- **Schema didn't change** — `ddl-auto: create-drop` rebuilds on restart; if you edited an entity
  while the app was running, restart it.
- **UI shows nothing, API is fine** — the field name you're reading in `app.js` doesn't match the
  JSON. Check the network tab before touching the backend.
