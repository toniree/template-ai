# Prompt: generate a full CRUD resource

Use when adding a new feature end to end, following the `widget/` pattern in this repo.

```
Add a new `<Feature>` resource following the exact same shape as `widget/`:

- `<Feature>Dto` — a record with Bean Validation annotations (@NotBlank, @PositiveOrZero, etc.
  as appropriate for each field)
- `<Feature>Service` — backend-agnostic interface (create/get/list/update/delete)
- `<Feature>Controller` — REST endpoints at `/api/<features>`, depends only on the interface,
  proper HTTP status codes (201 + Location on create, 204 on delete, 404 via
  ResourceNotFoundException)
- `jpa/<Feature>Entity`, `jpa/<Feature>JpaRepository`, `jpa/Jpa<Feature>Service` — implementation,
  annotated `@Profile({"h2","postgres"})`

Fields: <list the fields and types>
Relationships: <describe any relationships to other resources, or say "none">

Don't add anything beyond this — no mapper class, no generic base interface, no caching. Match
the existing widget/ code style exactly (constructor injection, no Lombok @Data on entities).
```
