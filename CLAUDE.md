# Working agreement

Java 21 / Spring Boot 3.4 / Maven / H2. Vanilla-JS frontend, no build step.
This repo is a live-coding scaffold for a 50-minute interview. **Speed and clarity beat
completeness.** Read this before writing code.

## The one rule

**Build the smallest thing that fully satisfies the stated requirement, then stop.**
When a requirement is ambiguous, implement the simplest reading and say which reading you picked —
do not implement both, and do not build for a requirement nobody asked for.

"Smallest" governs **how** you build it, never **whether**. If the prompt names endpoints, states a
workflow, or lists numbered behaviours, that is a contract — implement it as named, even when a
simpler design would deliver "the same outcome". Collapsing a specified two-step flow
(`authorize` → `capture`, `reserve` → `confirm`, `draft` → `submit`) into one call is the most
expensive mistake available here: the intermediate state is usually the whole point of the problem,
and the interesting rules — expiry, who may advance it, what happens on abandonment — live in it.

If you genuinely can't fit the full contract, cut it **loudly, not silently**: implement the named
shape for the core path and open your response with the one line "I did not implement X." A
reviewer forgives a stated gap; they do not forgive discovering it themselves.

## Never do these without being asked

- Interfaces with a single implementation. Inject the concrete `@Service`.
- Mapper classes, MapStruct, ModelMapper. Use a `static from(...)` factory on the response record.
- Generic base classes: `BaseEntity`, `AbstractCrudService<T, ID>`, `CrudController<T>`.
- New dependencies. Ask first — everything needed is already in `pom.xml`.
- Caching, async, events, schedulers, retries, circuit breakers, message queues.
- Auth/security layers. Out of scope unless a feature explicitly requires them.
- DTO-per-*layer*. One request record + one response record per operation, that's it. (Splitting by
  *audience* — public vs owner-only — is different, and required: see **Response records**.)
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

**Don't let the owner's name leak into the public list.** The moment an entity has an owner, the
browse endpoint that lists those entities is showing one user's data to every other user — see
**Response records**. The public list gets status only; the owner's name and email belong to
`/mine`, whose caller already knows them.

**Say what it stands in for.** Put one comment on the header parameter noting that `X-User-Id` is a
stand-in for an authenticated principal and would come from a verified session or token in
production — never from a caller-supplied header. It costs a line and it is the first thing a
reviewer asks about.

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

**Response records** — before returning a record, ask **who can call this endpoint**. A response
served on a public or browse-level endpoint must carry no personal data about a *different* user:
no name, email, phone, or address of whoever owns/booked/claimed the row. Status alone ("BOOKED",
"TAKEN", "SOLD") answers what the browsing caller legitimately needs. Reusing one record for both a
public listing and an owner-scoped `/mine` is how identity leaks out — the moment the same shape
serves both, split it (`SeatResponse` vs `MySeatResponse`) and let the public one omit the fields.
This is not the banned DTO-per-layer; it is one record per audience, and the audiences have
genuinely different contents.

**Queries** — aggregate in the database (`sum`, `count`, `group by`), never by loading rows and
summing in Java. If a list endpoint needs a per-row aggregate, fetch it for all rows in one
grouped query, like `TransactionRepository.sumAmountGroupedByCard`.

**Read-then-write on a rule** — if you read state, decide from it, and then write based on that
decision, the read must take a row lock or the rule is only advisory. Use
`CardRepository.findWithLockById` as the pattern, inside the service's existing `@Transactional`.
On this stack, `@Lock` on a derived finder emitted `for update` while the same annotation on an
explicit `@Query` did not — with no warning. **Never assume a lock applied**: turn on
`show-sql` and confirm `for update` is in the generated SQL.

Prefer the cheapest mechanism that actually enforces the invariant: a **database constraint**
(`unique`, `check`) makes it unrepresentable; a single conditional `UPDATE … WHERE <the invariant
still holds>` is atomic in one round trip, with zero rows affected meaning "rejected"; `@Version`
suits rare conflicts the caller can retry. Reach for the row lock when contention is real and a
retry loop would be worse. Say which one you picked and why.

**Prove it with a concurrent test, not a sequential one.** Two calls one after the other only prove
the second sees the first's result — that passes just as happily against a racy read-then-write. The
test that earns the claim fires N threads at one row simultaneously and asserts exactly one won:

```java
int threads = 8;
var start = new CountDownLatch(1);          // release everyone at once
var wins = new AtomicInteger();
var pool = Executors.newFixedThreadPool(threads);
for (int i = 0; i < threads; i++) {
    pool.submit(() -> {
        start.await();
        // call the SERVICE (or MockMvc), catch ApiException, count successes
    });
}
start.countDown();
pool.shutdown();
pool.awaitTermination(10, SECONDS);
assertThat(wins.get()).isEqualTo(1);        // and re-read the row to confirm final state
```

Traps that make this test lie: the test method must **not** be `@Transactional` (rollback isolation
hides the commits the other threads need to see); each thread needs its own transaction, so go
through the service or HTTP, never a repository call inside the test's own transaction; and assert
the losers were rejected for the right reason (409), not swallowed. One such test is worth more than
five happy-path ones when the problem is about contention.

**Status codes** — 201 + `Location` on create, 200 on read/update, 204 on delete, 400 validation,
404 unknown id, 409 state conflict. A business rejection that was correctly processed (a declined
charge) is a 201 with `status: DECLINED`, not a 4xx.

A nested collection — `GET /parents/{id}/children` — must **404 when the parent doesn't exist**.
Querying straight by the foreign key returns `[]` for a bogus parent id, which tells the caller the
same thing as a real parent that happens to have no children; those are different answers and the
caller can't distinguish them. Check the parent first (the service that owns it already has a
`find`/`get` that throws `notFound`), then query. Same for a nested aggregate or count.

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

Then **name the checkpoint**: when a slice is green, say so in one line and offer to commit it
(don't commit unasked). Work in vertical slices so there is always a green commit to fall back to —
a whole session's work sitting uncommitted in the working tree is one bad edit away from gone, and
"it was all still uncommitted" is the worst way to lose an interview build.

If the sample domain is gone, the README describing it is now **wrong, not merely stale** — say so.
A README advertising endpoints that no longer exist misleads the next reader, and in an interview
it reads as an unfinished repo.

## How to respond

Make the edit, don't paste code for me to copy. Keep diffs minimal and surgical. When you finish,
say in one or two lines what changed and what you deliberately left out. If you think something is
missing or wrong, say so in one sentence — don't silently build extra scope to cover it.

If I ask for a new feature while a **stated requirement is still unimplemented**, build what I asked
for, but open with one line naming the gap ("note: authorize/capture is still collapsed into one
call"). I may well have reprioritised on purpose — but I can only make that call if you surface it,
and polish added on top of a missing core requirement is the pattern that reads worst in review.
