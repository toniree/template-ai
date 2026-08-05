# Prompt: generate a JPA entity + repository

Use when adding persistence for a resource whose DTO/service interface already exist.

```
Generate the JPA layer for `<Feature>` following WidgetEntity/WidgetJpaRepository:

- `<Feature>Entity` — @Entity, Lombok @Getter/@Setter/@NoArgsConstructor (never @Data — it breaks
  equals/hashCode with lazy proxies), a convenience all-args constructor for the mutable fields
- `<Feature>JpaRepository extends JpaRepository<<Feature>Entity, <IdType>>` — add derived query
  methods only for the query patterns I actually need: <list them, e.g. "findByStatus",
  "findByOwnerIdOrderByCreatedAtDesc">
- `Jpa<Feature>Service implements <Feature>Service`, `@Profile({"h2","postgres"})`, mirrors
  JpaWidgetService's structure (findOrThrow helper, toDto helper)

Flag if any relationship needs @ManyToOne/@OneToMany and ask me about fetch type before assuming
EAGER or LAZY.
```
