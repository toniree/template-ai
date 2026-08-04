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
- Swappable persistence: **JPA/H2** (`sql` profile, default) or **MongoDB** (`mongo` profile),
  behind a single `WidgetService` interface — swap by flipping one property, no code changes.
- springdoc/Swagger UI at `/swagger-ui.html` for quick manual poking during the interview
- Actuator health/info at `/actuator/health`

## Layout

```
sandbox/src/main/java/com/templateai/sandbox/
  common/exception/     ApiError, ResourceNotFoundException, GlobalExceptionHandler
  widget/                WidgetDto, WidgetService (interface), WidgetController
  widget/jpa/            WidgetEntity, WidgetJpaRepository, JpaWidgetService   (@Profile("sql"))
  widget/mongo/          WidgetDocument, WidgetMongoRepository, MongoWidgetService (@Profile("mongo"))
```

`Widget` is a throwaway example resource that demonstrates the full pattern (DTO validation,
not-found handling, both persistence backends). Delete it once the real interview prompt lands,
or copy the same folder shape (`<feature>/`, `<feature>/jpa/`, `<feature>/mongo/`) for the
resource you actually need to build.

## Running

```bash
cd sandbox
./mvnw spring-boot:run                      # SQL/H2 profile (default, zero setup)
./mvnw spring-boot:run -Dspring-boot.run.profiles=mongo   # requires a local mongod on :27017
```

Then:
- `http://localhost:8080/swagger-ui.html` — try the API
- `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:sandbox`, user `sa`, blank password) — SQL profile only
- `http://localhost:8080/actuator/health`

## Testing

```bash
./mvnw test
```

`WidgetControllerIT` runs a full create/get/update/delete flow through MockMvc against the SQL
profile — a template for testing whatever resource you build live.

## Switching to real SQL Server instead of H2

H2 is configured in MSSQLServer compatibility mode (`MODE=MSSQLServer` in the JDBC URL) so JPA/SQL
you write maps cleanly onto SQL Server semantics. If you want to point at a real instance:

1. Add `com.microsoft.sqlserver:mssql-jdbc` to `pom.xml`.
2. In `application.yml` under the `sql` profile, replace the H2 `datasource.url`/`driver-class-name`
   with your SQL Server connection string and `org.hibernate.dialect.SQLServerDialect`.

## Notes for interview day

- Default profile is `sql`/H2 because it needs no external services — safest choice if you don't
  know in advance whether Mongo will be reachable in the interview environment.

