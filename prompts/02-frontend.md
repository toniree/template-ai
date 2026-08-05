# 02 · Frontend

Two paths. Pick by what the screen actually is, not by what preserves the abstraction.

| The screen is… | Do this |
|---|---|
| A list, a table, a create form — a CRUD view over one REST resource | Add one entry to `RESOURCES` in `static/app.js` |
| A dashboard, a chart, a map, a wizard, a detail/drill-down view, an approval queue — anything that isn't "rows plus a create form" | Write the smallest bespoke HTML/JS for it |

**Never widen the generic renderer to fit an unlike screen.** An abstraction stretched to cover its
second dissimilar case costs more than the duplication it avoids, and mid-interview is the worst
possible moment to pay that bill. Thirty lines of one-off DOM code is the cheap answer; a new
config key threaded through `renderTable`/`renderForm` is not.

---

## Path A — it fits `RESOURCES`

Everything generic is already written: table, create form, filters, pagination, toasts, money
formatting, status pills, double-submit protection.

```
Add a "<resource>" screen to RESOURCES in static/app.js for GET/POST /api/<path>.

Columns: <field -> label, note which are money and which are status enums>
Create form: <fields, note which are money (typed in dollars, sent in minor units) and which are
selects populated from another resource>
<Paged: true — the endpoint returns a PageResponse envelope.>

Config only: no new rendering functions, no changes to index.html or styles.css. If this screen
turns out not to fit the config shape, stop and tell me rather than adding config keys to make
it fit.
```

That last sentence matters — it's the guardrail that stops the agent quietly generalising the
renderer instead of telling you the screen doesn't belong there.

Product polish that stays inside the config, and is worth the minute:

```
Add a summary strip to the <resource> screen: <e.g. total outstanding, count by status, available
balance>. Use the existing `summary` hook and the money() helper. Only total within one currency.
```

```
On <resource>, a <declined/failed/rejected> result should surface as an error toast naming the
reason, not a generic success. Use the onCreated hook.
```

```
Add a row action on <resource> that <freezes/cancels/approves> via <METHOD> /api/<path>/{id}.
Use the existing rowActions config; hide it when <condition>.
```

---

## Path B — it doesn't fit

```
Build a <dashboard / wizard / detail view / approval queue> at <path or section> for <what it
shows and what the user does there>.

Write it as plain HTML + JS, self-contained, NOT through the RESOURCES config — this screen isn't
a table-plus-form and I don't want the generic renderer widened to accommodate it. Reuse only the
small helpers: request(), money(), pill(), timestamp(), toast(), escapeHtml().

Keep it under ~<N> lines. No framework, no build step, no new files beyond <one if needed>.
```

Reusing the helpers but not the renderer is the whole point: you get the error handling and money
formatting for free without deforming the config abstraction.

If it needs its own page rather than a section, say so explicitly:

```
Add static/<name>.html plus static/<name>.js, linked from the topnav in index.html. Same helpers,
same styles.css classes. Nothing in app.js changes.
```

---

## Retargeting the whole UI to a different domain

If the interview problem isn't cards, this converts the shell in one prompt:

```
Replace the RESOURCES config in static/app.js with screens for <resource A> and <resource B>,
matching the DTOs in <package A> and <package B>. Update the page <title> and .brand text in
index.html. Delete config for anything that no longer exists. Nothing below the RESOURCES block
should need to change — tell me if that turns out to be false.
```

If the answer comes back "that turns out to be false", believe it: that's the signal to take
Path B for the screen that doesn't fit, not to extend the renderer.
