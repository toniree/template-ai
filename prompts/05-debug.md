# 05 · Debug

Interviews are lost here — not because the bug is hard, but because the loop is slow. Rules: paste
the **whole** stack trace (the cause is usually the last `Caused by`, which is exactly the part
people trim), one hypothesis at a time, hard-stop at 3 minutes.

## Build or test failure

```
<paste the entire error output, including the full stack trace>

Context: I just <what you changed>. Read the file(s) involved before proposing anything, give me
the single most likely cause and the smallest fix. Don't refactor anything adjacent. If you need
to see a file I haven't shown you, say which one — don't guess at its contents.
```

## It compiles but the behaviour is wrong

```
Expected: <what should happen>
Actual:   <what happens>
Repro:    curl <the exact command> -> <the exact response>

Read <Service/Controller> and tell me where the logic diverges before changing anything.
```

## The UI is broken but the API is fine

```
The API returns <paste the actual JSON>. The UI shows <what you see>. Look at static/app.js and
find the mismatch. Do not change the backend.
```

Nine times out of ten it's a field name that doesn't match the response.

## Spring failures, and what they usually are

| Symptom | Almost always |
|---|---|
| `UnsatisfiedDependencyException` / `NoSuchBeanDefinition` | missing `@Service`/`@Component`, or a class outside `com.templateai.sandbox` |
| `LazyInitializationException` | mapping to a DTO outside the service — `open-in-view` is off on purpose |
| 404 on an endpoint you just wrote | `@RequestMapping` path, or a missing `@RestController` |
| 400 with no detail | Jackson can't build the record — a field name mismatch |
| 500 on a request that should be a 400 | something catches `Exception` before `GlobalExceptionHandler` |
| `InvalidDataAccessApiUsage` on save | setting an `@Id` yourself on an IDENTITY column |
| Test passes alone, fails in the suite | shared H2 state — assert on your own row, not on table counts |
| Schema doesn't match the entity | app was running when you edited it; `create-drop` only runs at boot |

```
I'm hitting <symptom>. Check <likely file> against that table in prompts/05-debug.md and fix the
cause, not the symptom.
```

## Still broken at 3 minutes — cut the loss

```
This has eaten 3 minutes and I have <N> left. What's the simplest version of this behaviour that
definitely works, even if it's less elegant? Implement that, and note in one line what we traded.
```

Say that trade out loud. Choosing the boring path under time pressure is a senior signal; silently
burning ten minutes is not.

If even that fails: `git reset --hard HEAD` (plus `git clean -fd` if new files appeared) back to
your last checkpoint, and take a different route.
