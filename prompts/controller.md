# Prompt: generate a REST controller

Use when you only need the controller layer for an already-defined service/DTO.

```
Generate a Spring REST controller for `<Feature>Service` following the pattern in
WidgetController.java:

- Constructor injection, no field @Autowired
- @Valid on @RequestBody params
- ResponseEntity where the status code matters (201 + Location header on create, 204 on delete),
  plain return type otherwise
- No business logic in the controller — it only calls the service and shapes the HTTP response
- No manual try/catch — exceptions bubble to GlobalExceptionHandler
- springdoc/OpenAPI annotations (@Operation, @ApiResponse) only where the default generated docs
  would be ambiguous (e.g. what a 404 means), not on every method
```
