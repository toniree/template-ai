# template-ai

Scaffold for the Brex Senior SWE AI-assisted coding interview (Java / Spring Boot equivalent of
[brexhq/ai_assisted_coding_interview](https://github.com/brexhq/ai_assisted_coding_interview)).

## Goal
During the interview, you'll build a small product from scratch. At the start, you and your
interviewers will agree on 3-5 core features to implement. The rest of the session will be focused on
execution and building. We're looking for working software with production-quality design decisions
that you’d be proud to hand to a customer. This is an ambitious task.

## Evaluation Criteria
Productivity - Can you complete the agreed-upon features?
AI Collaboration & Iteration - How effectively do you integrate AI into your development workflow?
API Design / Data Modeling - Do you create logical, efficient data relationships that support
scalable systems?
Definition of Done - Where do you draw the line on "production ready" and "quality"?
Product Mindset - Do you have opinions about the products you build?

## Stack

- Java 21, Spring Boot 3.4.1, Maven
- REST via Spring MVC, Bean Validation, global JSON error handling
- Swappable persistence: **JPA/H2** (`h2` profile, default) or **JPA/PostgreSQL** (`postgres`
  profile), behind a single `WidgetService` interface — swap by flipping one property, no code
  changes (same `WidgetEntity`/`WidgetJpaRepository`/`JpaWidgetService`, just a different datasource).
- springdoc/Swagger UI at `/swagger-ui.html` for quick manual poking during the interview
- Actuator health/info at `/actuator/health`

## Layout

```
sandbox/src/main/java/com/templateai/sandbox/
  common/exception/     ApiError, ResourceNotFoundException, GlobalExceptionHandler
  common/config/        ClockConfig (injectable java.time.Clock bean, for testable timestamps)
  common/logging/       RequestLoggingFilter (logs method, path, status, duration per request)
  widget/                WidgetDto, WidgetService (interface), WidgetController
  widget/jpa/            WidgetEntity, WidgetJpaRepository, JpaWidgetService   (@Profile({"h2","postgres"}))
```

Reusable prompt templates for common ask-Claude-to-do-X requests during the interview live in
`/prompts` at the repo root (`crud.md`, `controller.md`, `repository.md`, `tests.md`,
`algorithms.md`, `refactor.md`) — written in advance so you're not improvising prompts live.

`Widget` is a throwaway example resource that demonstrates the full pattern (DTO validation,
not-found handling, JPA persistence). Delete it once the real interview prompt lands, or copy the
same folder shape (`<feature>/`, `<feature>/jpa/`) for the resource you actually need to build.

## Running

```bash
cd sandbox
./mvnw spring-boot:run                                       # H2 profile (default, zero setup)
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres    # requires a local Postgres on :5432
```

To start a local Postgres for the `postgres` profile:

```bash
docker run -p 5432:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=sandbox postgres:16
```

Then:
- `http://localhost:8080/swagger-ui.html` — try the API
- `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:sandbox`, user `sa`, blank password) — H2 profile only
- `http://localhost:8080/actuator/health`

## Testing

```bash
./mvnw test
```

`WidgetControllerIT` runs a full create/get/update/delete flow through MockMvc against the H2
profile — a template for testing whatever resource you build live.

## Notes for interview day

- Default profile is `h2` because it needs no external services — safest choice if you don't
  know in advance whether a Postgres instance will be reachable in the interview environment.
- H2 runs in PostgreSQL compatibility mode (`MODE=PostgreSQL` in the JDBC URL) so the JPA/SQL you
  write maps cleanly onto real PostgreSQL semantics when you switch profiles.

