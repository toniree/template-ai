# Working agreement

Java 21 / Spring Boot 3.4 / Maven / H2 in-memory. Vanilla-JS frontend, no build step.
This repo is a domain-neutral scaffold for a ~1-hour live-coding interview. **Speed and clarity
beat completeness.** Read this before writing code.

## The one rule

**Build the smallest thing that fully satisfies the stated requirement, then stop.**
When a requirement is ambiguous, implement the simplest reading and say which reading you picked —
do not implement both, and do not build for a requirement nobody asked for.

## Never do these without being asked

- Interfaces with a single implementation. Inject the concrete `@Service`.
- Mapper classes, MapStruct, ModelMapper. Use a `static from(...)` factory on the response record.
- Generic base classes: `BaseEntity`, `AbstractCrudService<T, ID>`, `CrudController<T>`.
- New dependencies. Ask first — everything needed is already in `pom.xml`.
- Caching, async, events, schedulers, retries, circuit breakers, message queues.
- Auth/security layers. Out of scope unless a feature explicitly requires them.
- DTO-per-layer. One request record + one response record per operation, that's it.
- Renaming, reformatting, or "cleaning up" code you weren't asked to touch.

## Layout — one flat package per feature, 5 files

```
com.templateai.sandbox
├── common/   ApiError, ApiException, GlobalExceptionHandler,
│             AppConfig (Clock bean), RequestLoggingFilter
└── task/     Task, TaskRepository, TaskDtos, TaskService, TaskController
```

`task/` is the sample domain — it exists only to prove the wiring works end to end. Copy its shape
for a new feature; delete or rename it once the real problem is known. No `impl/`, no `jpa/`, no
`mapper/` subpackages. All request/response records for a feature live in one `<Feature>Dtos.java`.

## Conventions that are not negotiable

**Layers** — Controller does HTTP only (status codes, headers, param binding). Service holds every
business rule and is `@Transactional`. Repository does queries. Entities never leave the service:
map to a DTO before returning. `spring.jpa.open-in-view` is off, so mapping must happen inside the
transaction.

**Errors** — throw `ApiException.notFound/badRequest/conflict`. Never try/catch to produce an HTTP
status; `GlobalExceptionHandler` owns every response shape. Validate input with Bean Validation
annotations on the request record, not with `if` statements in the service. That handler extends
`ResponseEntityExceptionHandler` so Spring's own exceptions keep their correct status codes — do
not remove that, or a catch-all turns every malformed request into a 500.

**Request records** — numeric and boolean fields are **boxed** (`Long`, not `long`). A primitive
silently becomes `0`/`false` when the caller omits it, which validation then accepts: a partial
`PATCH` would zero a field as a side effect. Required → `@NotNull`/`@NotBlank`. Optional on a PATCH
→ nullable, and the service applies only non-null fields.

**Queries** — filter, sort, and aggregate in the database (`where`, `order by`, `sum`, `count`,
`group by`), never by loading rows and doing it in Java. If a list endpoint needs a per-row
aggregate, fetch it for all rows in one grouped query with a projection interface, not one query
per row.

**Read-then-write on a rule** — if you read state, decide from it, and then write based on that
decision, the read must take a row lock or the rule is only advisory. Use a **derived** finder with
`@Lock(LockModeType.PESSIMISTIC_WRITE)` inside the service's existing `@Transactional`. On this
stack, `@Lock` on a derived finder emitted `for update` while the same annotation on an explicit
`@Query` did not — with no warning. **Never assume a lock applied**: `show-sql` is on under the
`h2` profile, so confirm `for update` is in the generated SQL.

**Numbers that must be exact** — counts, quantities, anything summed or compared against a limit:
integer types, never `double` or `float`. Compare a limit as `amount > limit - used`, never
`used + amount > limit` — the sum overflows for a large enough input, wraps negative, and reads as
"under the limit".

**Status codes** — 201 + `Location` on create, 200 on read/update, 204 on delete, 400 validation,
404 unknown id, 409 state conflict. A business rejection that was correctly processed (a request
the system understood and answered "no" to) is a 2xx carrying that outcome, not a 4xx.

**Persistence** — `@ManyToOne(fetch = LAZY)` always. Lombok `@Getter @Setter @NoArgsConstructor` on
entities, never `@Data`. `@Enumerated(EnumType.STRING)`, never ordinal.

**Time** — inject the `Clock` bean and call `Instant.now(clock)`. Never `Instant.now()` directly.

**Frontend** — `static/app.js` is a single bespoke screen, deliberately not a config-driven
renderer. Retarget it by changing the constants at the top, the `<thead>` in `index.html`, and the
cells in `taskRow()`. For a second screen, copy what you need; for a different *kind* of screen
(dashboard, wizard, detail view) write it separately. **Never generalise `app.js` into a framework**
— an abstraction stretched to cover its second unlike case costs more than the duplication it
avoids, and mid-interview is the worst time to pay it.

## Commands

```bash
cd sandbox
./mvnw -o spring-boot:run     # http://localhost:8080  (offline flag = fastest start)
./mvnw -o test                # whole suite: 13 tests, ~3s
./mvnw -o test -Dtest=TaskApiIT
```

`-o` only works because `~/.m2` is already populated. If you change `pom.xml`, drop the `-o` for
the next run so the new dependency can download, then go back to offline.

Swagger `/swagger-ui.html` · H2 console `/h2-console` (jdbc:h2:mem:sandbox, `sa`, no password).
Demo data is seeded by `DemoData.java` on the `h2` profile only.

## Definition of done for a feature

Endpoint works via curl · invalid input returns 400 with field details · unknown id returns 404 ·
it appears in the UI · one integration test covering the happy path and the main rule · no
`System.out.println` left behind. That is the bar. Not more.

## How to respond

Make the edit, don't paste code for me to copy. Keep diffs minimal and surgical. When you finish,
say in one or two lines what changed and what you deliberately left out. If you think something is
missing or wrong, say so in one sentence — don't silently build extra scope to cover it.
