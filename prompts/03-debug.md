# 03 · Debug

Debugging is where interviews are lost — not because the bug is hard, but because the loop is
slow. Rules: paste the **whole** stack trace (the cause is usually the last `Caused by`, which is
exactly the part people trim), one hypothesis at a time, and hard-stop at 3 minutes.

## Default

```
<paste the entire stack trace or full error output>

Context: I just <what you changed>. Give me the single most likely cause and the smallest fix.
If you need to see a file, say which one — don't guess at its contents.
```

## It compiles but the behaviour is wrong

```
Expected: <what should happen>
Actual:   <what happens>
Repro:    curl <the exact command> -> <the exact response>

Look at <Service/Controller> and tell me where the logic diverges before changing anything.
```

## Spring-specific failures, and what they usually are

| Symptom | Almost always |
|---|---|
| `UnsatisfiedDependencyException` / `NoSuchBeanDefinition` | missing `@Service`/`@Component`, or a class outside `com.templateai.sandbox` |
| `LazyInitializationException` | mapping to a DTO outside the service — `open-in-view` is off on purpose |
| 404 on an endpoint you just wrote | `@RequestMapping` path or missing `@RestController` |
| 400 with no detail | Jackson can't build the record — a field name mismatch |
| `InvalidDataAccessApiUsage` on save | setting an `@Id` yourself on an IDENTITY column |
| Test passes alone, fails in the suite | shared H2 state — assert on your own row, not on table counts |
| Frontend shows nothing, API is fine | field name in `columns` doesn't match the JSON |

```
I'm hitting <symptom>. Check <likely file> against that table in prompts/03-debug.md and fix the
cause, not the symptom.
```

## Still broken at 3 minutes — cut the loss

```
This has eaten 3 minutes and I have <N> left. What's the simplest version of this behaviour that
definitely works, even if it's less elegant? Implement that, and note in one line what we traded.
```

Say that trade out loud to the interviewer. Choosing to take the boring path under time pressure
is a senior signal; silently burning ten minutes is not.
