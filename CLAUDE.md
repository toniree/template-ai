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

## Starting a new problem

I will hand you a system design — statement, functional and nonfunctional requirements, entities,
API, data flow, high-level design, deep dive. It will be **rough, partial, and written fast**. Some
of it will be contradictory. That is normal; it is interview notes, not a spec.

Work through it in this order. Steps 1–3 are one short response, not a document:

1. **Read the repo before changing it.** `common/` already has the error contract, the `Clock`
   bean, and `CurrentUser`; `support/` already has the test helpers. Reusing them is faster than
   anything you would write, and re-inventing one in parallel is the most common way this scaffold
   gets worse.
2. **Restate the requirements in your own words, numbered**, and name **the critical business
   invariant in one sentence** — the thing that must never be violated. "A card's spend limit is
   never exceeded." Almost every problem worth setting has exactly one, and it is what the
   interviewer is really grading.
3. **Split the work three ways** and say which is which:
   - *MVP behaviour* — what must actually run.
   - *Interview shortcuts* — the deliberate simplifications (`X-User-Id` instead of auth, seeded
     fixtures instead of an admin CRUD, a stubbed payment that always succeeds).
   - *Production extensions* — described, never built.
4. **Give a short slice plan.** Three to five vertical slices, ordered so each one is demoable on
   its own. Not a layer plan — never "all the entities, then all the services".
5. **Build one slice at a time**, end to end: persistence → service → endpoint → test → UI.
6. **Run the tests after each slice** and report the real numbers.
7. **Say what you deferred** at the end, unprompted.

Fill gaps with the simplest reasonable assumption and state it in one line. Do not interview me
back — one genuinely blocking question is fine, five is a waste of the clock.
[`docs/PROBLEM_TEMPLATE.md`](docs/PROBLEM_TEMPLATE.md) is the long form of all this if I hand you
notes and ask you to normalise them first.

## Infrastructure: what to reach for

Work down this list and **stop at the first thing that satisfies the requirement**:

1. **A synchronous, in-process call.** Almost always the answer.
2. **A relational transaction** — the tool for consistency. Constraints, conditional updates, and
   locks live here.
3. **A database-backed job** — a table with a status column and a claim query, for work that must
   outlive the request.
4. **An external queue, cache, stream, or search cluster** — only when a stated requirement makes
   the first three genuinely insufficient.

Do not add Kafka, RabbitMQ, Redis, Elasticsearch, Docker, Kubernetes, microservices, distributed
locks, event sourcing, CQRS, a second database, real payments, or a real identity provider. Not
because they are wrong — because in a 50-minute build each one costs setup time, adds a failure
mode I have to explain, and moves the enforcement of my invariant somewhere I cannot demo.

If the design I gave you names one of them, that is a **talking point, not a task**: implement the
behaviour against the database and write the scaling path into the architecture notes. Say "I would
put this behind a queue at X throughput; below that it is a transaction" — that answer scores
better than a half-wired broker.

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

src/test/java/…
├── support/     ApiIntegrationTest, Concurrently, MutableClock   ← reuse, don't reinvent
├── card/        CardApiIT
└── transaction/ TransactionApiIT
```

Copy that shape for a new feature. No `impl/`, no `jpa/`, no `mapper/` subpackages.
All request/response records for a feature live together in one `<Feature>Dtos.java`.

`common/` and `support/` are the reusable half of the scaffold and know nothing about any domain.
**Leave them alone** unless the change is clearly justified — and if you do change one, say so
explicitly, because everything else depends on them.

## Who is calling — already built, don't rebuild it

`common/CurrentUser` reads `X-User-Id` and is **the only place identity comes from**. Inject it and
call `currentUser.require()` (the id, or 401). Do not add `@RequestHeader` parameters to controllers
and thread a user id down through service signatures — that is the version that has to be rewritten
when auth becomes real, which is exactly what this class exists to prevent.

It is **not authentication**: the caller supplies the header, so anyone can claim any id. Say that
plainly if asked, and never describe the app as having auth. Swapping in a verified JWT or session
later is a change to `CurrentUser.find()` and nothing else.

**A `User` entity is a separate decision.** `CurrentUser` gives you an id; that is enough for
ownership columns and `/mine` queries. Only add a `user/` package when the problem actually needs
user *records* — a profile screen, a name to display, a picker. If it does: `User` (id, name, email,
nothing more unless asked), repository, `UserService.find(id)` for other services to validate an
owner, and `GET /api/users` to back a picker. Seed two or three in `DemoData`.

**Scoping owned entities.** Add a nullable `@ManyToOne(fetch = LAZY) User owner` (or a plain
`ownerId` column if there is no `User` entity) to the entity the requirement is about — never a join
table. Add one `findByOwnerId...` query and a `GET /api/<feature>/mine` endpoint.

**Don't let the owner's name leak into the public list.** The moment an entity has an owner, the
browse endpoint listing those entities is showing one user's data to every other user — see
**Response records**. The public list gets status only; names and emails belong to `/mine`, whose
caller already knows them.

**Frontend, when the problem wants a visible identity:** no login form. On load, if `localStorage`
has no chosen user, show a blocking picker; afterwards send the header from the one shared
`request()` helper, never from each call site. A topnav menu switches user or clears it.

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
the second sees the first's result — that passes just as happily against a racy read-then-write.
`support/Concurrently` fires N overlapping attempts and counts the winners:

```java
Concurrently.run(8, () ->
        http.perform(postJson("/api/cards/1/charges", request).with(as(userId)))
                .andReturn().getResponse().getStatus() == 201)
    .assertExactlyOneWon();          // or .assertWinnersWere(n) for a capacity limit
```

Traps that make this test lie, all three of which it is your job to check: the test class must
**not** be `@Transactional` (rollback isolation hides the commits the other threads need to see);
each attempt needs its own transaction, so go through MockMvc or the service, never a repository
call inside the test's own transaction; and the losers must have failed for the right reason (409),
not been swallowed. One such test is worth more than five happy-path ones when the problem is about
contention — it is usually the single most valuable test in the build.

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

**Time** — inject the `Clock` bean and call `Instant.now(clock)`. Never `Instant.now()` directly.
Anything with an expiry, a cutoff, or a TTL is then testable by advancing
`support/MutableClock` instead of sleeping: `@Import(MutableClock.Config.class)`, autowire it, call
`clock.advance(Duration.ofMinutes(5))`. A direct `Instant.now()` ignores that entirely, which is the
practical reason for the rule — a five-minute hold rule is otherwise a five-minute test.

**Tests** — extend `support/ApiIntegrationTest` for anything HTTP-shaped: it brings `http`
(MockMvc), `json`, the `test` profile, and `as(userId)` for calling as a principal. Prefer a few
integration tests over real HTTP and a real database to many mocked unit tests — a mocked service
test mostly asserts that you wrote the mock correctly. The suite shares one database, so assert on
rows your test created, never on table-wide counts.

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

## Verifying a UI change

1. Start the app using the IntelliJ `▶ Run App (8080)` configuration.
2. Open or navigate the IDE browser to http://localhost:8080.
3. Verify the changed flow interactively.
4. Check http://localhost:8080/swagger-ui.html for API-only changes.
5. Report what was verified and any browser-console or request errors.

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

**Never claim a result you did not observe.** Do not say tests pass, an endpoint works, or the UI
renders unless you ran it and read the output — report the actual count and the actual outcome. If
something failed, say so with the error. If you could not run it at all — offline Maven, a port in
use, a missing tool — say that instead of inferring; "I could not execute the suite" is a fine
answer and a fabricated green one is not recoverable, because I will repeat it to an interviewer.
The same goes for reviews and analysis: distinguish what you executed from what you read.
