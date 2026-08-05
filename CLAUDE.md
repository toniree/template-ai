# Working agreement

Java 21 / Spring Boot 3.4 / Maven / H2. Vanilla-JS frontend, no build step.
This repo is a live-coding scaffold for a 50-minute interview. **Speed and clarity beat
completeness.** Read this before writing code.

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

## Layout — one flat package per feature, 4-5 files

```
com.templateai.sandbox
├── common/      ApiError, ApiException, GlobalExceptionHandler, PageResponse,
│                AppConfig (Clock bean), RequestLoggingFilter
├── card/        Card, CardRepository, CardDtos, CardService, CardController
└── transaction/ Transaction, TransactionRepository, TransactionDtos,
                 TransactionService, TransactionController
```

Copy that shape for a new feature. No `impl/`, no `jpa/`, no `mapper/` subpackages.
All request/response records for a feature live together in one `<Feature>Dtos.java`.

## Conventions that are not negotiable

**Money** — `long ...Minor` (cents) plus a 3-letter `currency`. Never `double`, never `float`.
The API speaks minor units; only the UI formats to dollars.

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
`PATCH` would zero a spend limit as a side effect. Required → `@NotNull`. Optional on a PATCH →
nullable, and the service applies only non-null fields.

**Queries** — aggregate in the database (`sum`, `count`, `group by`), never by loading rows and
summing in Java. If a list endpoint needs a per-row aggregate, fetch it for all rows in one
grouped query, like `TransactionRepository.sumAmountGroupedByCard`.

**Read-then-write on a rule** — if you read state, decide from it, and then write based on that
decision, the read must take a row lock or the rule is only advisory. Use
`CardRepository.findWithLockById` as the pattern, inside the service's existing `@Transactional`.
`@Lock` only works on a **derived** query method; combined with `@Query` it is silently ignored
and emits no `for update`. Never assume a lock applied — check the SQL.

**Status codes** — 201 + `Location` on create, 200 on read/update, 204 on delete, 400 validation,
404 unknown id, 409 state conflict. A business rejection that was correctly processed (a declined
charge) is a 201 with `status: DECLINED`, not a 4xx.

**Persistence** — `@ManyToOne(fetch = LAZY)` always. Lombok `@Getter @Setter @NoArgsConstructor` on
entities, never `@Data`. `@Enumerated(EnumType.STRING)`, never ordinal.

**Frontend** — `static/app.js` is config-driven. Adding a screen means adding one entry to the
`RESOURCES` object; do not write bespoke DOM code per resource.

## Commands

```bash
cd sandbox
./mvnw -o spring-boot:run     # http://localhost:8080  (offline flag = fastest start)
./mvnw -o test                # whole suite, ~10s
./mvnw -o test -Dtest=CardApiIT
```

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
