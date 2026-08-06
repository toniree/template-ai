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

## Optional pattern: current-user profile

Only add this when the stated requirement calls for per-user data — "my X", a profile screen, or
anything scoped to "who's using the app right now." Don't add it speculatively.

**Static user, not auth.** No login, password, or session. A `user/` package holds `User` (id,
name, email — nothing else unless asked), `UserRepository`, `UserDtos` (`UserResponse`),
`UserService` (`list()`, and a `public User find(id)` other services can call to resolve/validate
an owner), `UserController` (`GET /api/users` for the picker, `GET /api/users/{id}`). Seed 2-3
users in `DemoData`.

**Frontend picks the user, not a login form.** On load, if no user is chosen (checked via
`localStorage`), show a blocking picker modal listing the static users. Once chosen, every request
carries an `X-User-Id` header (add it once inside the shared `request()` helper — don't thread it
through every call site). A small profile menu in the topnav shows the current user's name/email
and lets them switch or log out (log out just clears `localStorage` and reopens the picker).

**Scoping owned entities.** Add a nullable `@ManyToOne(fetch = LAZY) User owner` to whichever
entity the requirement is about (don't invent a join table). Read the owner id from the
`X-User-Id` header in the controller (`@RequestHeader("X-User-Id") Long userId`, required — a
missing header is correctly a 400 via Spring's own `MissingRequestHeaderException`, no extra
validation code needed) and validate it with `UserService.find(userId)` before using it. Add one
`findByOwnerId...` query and a `GET /api/<feature>/mine` endpoint backing the "my X" screen.

**Gotcha:** if you add a `hidden` modal/overlay, don't give its class an unconditional `display:
...` rule — that beats the `hidden` attribute's default `display: none` in an author stylesheet
(same specificity, later rule wins). Add `.your-overlay[hidden] { display: none; }` explicitly.

## Conventions that are not negotiable

**Money** — `long ...Minor` (cents) plus a 3-letter `currency`. Never `double`, never `float`.
The API speaks minor units; only the UI formats to dollars. Compare limits as
`amount > limit - spent`, never `spent + amount > limit` — the sum overflows and wraps negative.
Only total amounts within a single currency; converting needs an FX rate and a rate timestamp, so
don't invent one.

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
On this stack, `@Lock` on a derived finder emitted `for update` while the same annotation on an
explicit `@Query` did not — with no warning. **Never assume a lock applied**: turn on
`show-sql` and confirm `for update` is in the generated SQL.

**Status codes** — 201 + `Location` on create, 200 on read/update, 204 on delete, 400 validation,
404 unknown id, 409 state conflict. A business rejection that was correctly processed (a declined
charge) is a 201 with `status: DECLINED`, not a 4xx.

**Persistence** — `@ManyToOne(fetch = LAZY)` always. Lombok `@Getter @Setter @NoArgsConstructor` on
entities, never `@Data`. `@Enumerated(EnumType.STRING)`, never ordinal.

**Frontend** — `static/app.js` is config-driven: a table-and-form screen over a REST resource is
one entry in `RESOURCES`, so use that when the problem fits it. When a workflow doesn't fit — a
wizard, a dashboard, a detail view, anything that isn't a list plus a create form — write the
smallest bespoke HTML/JS for it instead. **Never widen the generic renderer to accommodate a
one-off screen**; an abstraction stretched to cover its second unlike case costs more than the
duplication it avoids, and mid-interview is the worst time to pay it.

## Commands

```bash
cd sandbox
./mvnw -o spring-boot:run     # http://localhost:8080  (offline flag = fastest start)
./mvnw -o test                # whole suite: 25 tests, ~3s
./mvnw -o test -Dtest=CardApiIT
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
