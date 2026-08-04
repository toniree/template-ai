# Interview Cheatsheet

Quick reference for the Brex AI-assisted interview. Keep this open in a tab.

## Maven commands

```bash
cd sandbox

./mvnw spring-boot:run                                    # run, sql/H2 profile (default)
./mvnw spring-boot:run -Dspring-boot.run.profiles=mongo    # run, mongo profile

./mvnw test                                                # run all tests
./mvnw test -Dtest=WidgetControllerIT                      # run one test class
./mvnw test -Dtest=WidgetControllerIT#createGetUpdateDelete # run one test method

./mvnw clean compile                                       # fast compile check, no tests
./mvnw clean package -DskipTests                            # build jar without running tests
./mvnw dependency:tree                                      # inspect dependency graph / conflicts
./mvnw -q dependency:tree -Dincludes=<groupId>:<artifactId>  # find where a dep comes from
```

## Common Spring annotations

| Annotation | Use |
|---|---|
| `@SpringBootApplication` | Entry point; combines `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan` |
| `@RestController` | `@Controller` + `@ResponseBody`; class returns JSON, not views |
| `@RequestMapping("/x")` / `@GetMapping` `@PostMapping` `@PutMapping` `@DeleteMapping` `@PatchMapping` | Route methods |
| `@RequestBody` | Bind JSON request body to a method param |
| `@RequestParam` | Bind a query param (`?foo=bar`) |
| `@PathVariable` | Bind a `{segment}` from the URL |
| `@Valid` | Trigger Bean Validation on a `@RequestBody`/`@RequestParam` |
| `@Service` | Business-logic bean (stereotype of `@Component`) |
| `@Repository` | Persistence bean; also translates DB exceptions into `DataAccessException` |
| `@Component` | Generic managed bean when no more specific stereotype fits |
| `@Configuration` / `@Bean` | Java-based bean definitions |
| `@Autowired` | Field/constructor injection (prefer constructor injection — no annotation needed if there's only one constructor) |
| `@Profile("sql")` | Only register this bean when the given profile is active — how this repo's Mongo/SQL toggle works |
| `@ConditionalOnProperty` | Register a bean only if a property matches |
| `@Transactional` | Wrap a method in a DB transaction; put on `@Service` methods, not repositories/controllers. `readOnly = true` for read paths as an optimization hint |
| `@Entity` / `@Id` / `@GeneratedValue` | JPA entity + primary key |
| `@Table(name=...)` / `@Column(name=...)` | Override default table/column naming |
| `@OneToMany` / `@ManyToOne` / `@ManyToMany` | JPA relationships — watch for `fetch = FetchType.LAZY` vs eager, and N+1 queries |
| `@Document("collection")` / `@Id` | Mongo document mapping (spring-data-mongodb) |
| `@RestControllerAdvice` + `@ExceptionHandler` | Centralized error handling → this repo's `GlobalExceptionHandler` |
| `@NotBlank` `@NotNull` `@Positive` `@PositiveOrZero` `@Size` `@Email` | Bean Validation constraints on DTOs |
| `@Slf4j` (Lombok) | Injects a `log` field |
| `@Getter` `@Setter` `@NoArgsConstructor` `@AllArgsConstructor` `@Builder` `@Data` | Lombok boilerplate — avoid `@Data` on JPA entities (breaks `equals`/`hashCode`/`toString` w/ lazy proxies) |
| `@SpringBootTest` | Full context integration test |
| `@WebMvcTest(Controller.class)` | Slice test — controller layer only, mock the service |
| `@DataJpaTest` | Slice test — JPA repository layer only, in-memory DB |
| `@MockBean` / `@Autowired MockMvc` | Mock a bean in context / drive HTTP calls in tests |
| `@ActiveProfiles("sql")` | Pin which profile a test runs under |

## HTTP status codes

| Code | Meaning | When to use it here |
|---|---|---|
| 200 OK | Success, body returned | GET, PUT, PATCH success |
| 201 Created | Resource created | POST success — pair with `Location` header |
| 204 No Content | Success, no body | DELETE success |
| 400 Bad Request | Malformed/invalid input | Failed `@Valid`, bad param parsing |
| 401 Unauthorized | Missing/invalid auth | No/expired credentials |
| 403 Forbidden | Authenticated but not allowed | Authz failure |
| 404 Not Found | Resource doesn't exist | `ResourceNotFoundException` in this repo |
| 405 Method Not Allowed | Wrong HTTP verb for the route | |
| 409 Conflict | State conflict | Duplicate create, optimistic-lock/version clash |
| 422 Unprocessable Entity | Well-formed but semantically invalid | Alternative to 400 for business-rule validation |
| 500 Internal Server Error | Unhandled exception | Should be rare — catch and translate deliberately |

## SQL snippets (H2 / SQL Server–ish, per this repo's `sql` profile)

```sql
-- inspect schema live at http://localhost:8080/h2-console
SELECT * FROM widget_entity;

-- pagination
SELECT * FROM widget_entity ORDER BY id LIMIT 20 OFFSET 40;

-- filter + count
SELECT COUNT(*) FROM widget_entity WHERE quantity > 0;

-- join example (if you add a related table)
SELECT w.*, t.name AS tag_name
FROM widget_entity w
JOIN tag t ON t.widget_id = w.id;

-- upsert-ish (H2 syntax)
MERGE INTO widget_entity (id, name, quantity) KEY (id) VALUES (1, 'Gizmo', 5);

-- add a column / index quickly while iterating
ALTER TABLE widget_entity ADD COLUMN sku VARCHAR(64);
CREATE INDEX idx_widget_name ON widget_entity (name);
```

Spring Data derived-query equivalents (no SQL needed for simple cases):
```java
List<WidgetEntity> findByNameContainingIgnoreCase(String name);
List<WidgetEntity> findByQuantityGreaterThan(int quantity);
Page<WidgetEntity> findAll(Pageable pageable);

@Query("select w from WidgetEntity w where w.quantity = 0")
List<WidgetEntity> findOutOfStock();
```

## Useful AI prompts for this interview format

- "Here's the prompt: [paste]. Before writing code, list the entities, endpoints, and edge cases you'd expect me to be graded on."
- "Generate the DTO + validation annotations for X, matching the `WidgetDto` pattern in this repo."
- "Add a new `<Feature>` resource following the exact same shape as `widget/` — interface + `jpa/` + `mongo/` implementations behind `@Profile`."
- "Review this controller/service for edge cases: null handling, empty lists, concurrent updates, invalid IDs."
- "Write a `@WebMvcTest` for this controller that covers the happy path plus one 400 and one 404 case."
- "I'm about to run out of time — what's the minimum viable version of this that still passes the given tests?"
- "Explain the tradeoff between doing X in the service layer vs. the repository layer here."
- "Given this stack trace, what's the most likely cause?" (paste full trace, not a summary)
- "Diff my change against the original file and tell me if I broke anything in `GlobalExceptionHandler`."
- If stuck on requirements: "Restate the requirements as a numbered acceptance-criteria list so I can confirm I'm not missing one."

## Package conventions (this repo)

```
com.templateai.sandbox
├── common/
│   └── exception/        ApiError, ResourceNotFoundException, GlobalExceptionHandler
└── <feature>/            e.g. widget/
    ├── <Feature>Dto.java        shared DTO + validation, used by controller and both backends
    ├── <Feature>Service.java    backend-agnostic interface
    ├── <Feature>Controller.java REST endpoints, depends only on the interface
    ├── jpa/                     @Profile("sql") impl: Entity, JpaRepository, Service impl
    └── mongo/                   @Profile("mongo") impl: Document, MongoRepository, Service impl
```

Rule of thumb when adding a new resource: copy the `widget/` folder shape exactly. Controller and
DTO never import anything from `jpa/` or `mongo/` directly — only the `Service` interface.
