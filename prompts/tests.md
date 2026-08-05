# Prompt: generate tests

Use for either a controller slice test or a full integration test.

```
Write a @WebMvcTest for <Feature>Controller covering:
- the happy path for each endpoint (create/get/list/update/delete)
- one 400 case (failed @Valid)
- one 404 case (ResourceNotFoundException from the mocked service)

Mock the service with @MockBean. Assert on status code and JSON body via MockMvc + jsonPath.
Follow the structure of WidgetControllerIT.java, but as a slice test rather than full context.
```

```
Write a full integration test for <Feature>, modeled on WidgetControllerIT.java:
@SpringBootTest + @AutoConfigureMockMvc + @ActiveProfiles("h2"), a single test method that runs
create -> get -> update -> delete against the real H2-backed service, asserting state after each
step. No mocks.
```
