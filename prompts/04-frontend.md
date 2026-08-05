# 04 · Frontend

`static/app.js` is one bespoke screen, deliberately not a renderer framework. Keep it that way:
copy and adapt for a second screen, write something separate for a different *kind* of screen, and
never generalise it into config-driven machinery mid-interview.

---

## Retarget the existing screen to a new resource

The common case — the sample `task` screen becomes your resource.

```
Retarget static/app.js and index.html from tasks to <resource>, matching the DTOs in <package>.

Change: the API constant and the status/label constants at the top of app.js, the <thead> in
index.html, the cells built in taskRow() (rename it), the page <title> and .brand text.

Columns: <field -> label, note which are enums and which are timestamps>
Create form: <fields, marking required ones>
Row actions: <e.g. change status via PATCH, delete>

Keep request(), toast(), escapeHtml(), timestamp() and the disabled-button double-submit guard
exactly as they are. Do not add a build step, a framework, or a config layer. Delete anything that
no longer applies rather than leaving it unused.
```

## A second screen of the same shape

```
Add a second screen for <resource> as a new <section> in index.html plus a matching block in
app.js. Copy the structure of the existing one — I would rather have two similar blocks than one
generic renderer with a config object. Reuse request(), toast(), escapeHtml(), timestamp().
```

## A screen that isn't a table plus a form

Dashboards, wizards, detail/drill-down views, approval queues. These do **not** belong in `app.js`.

```
Build a <dashboard / wizard / detail view / approval queue> at <path or section> showing <what it
shows and what the user does there>.

Write it as plain HTML + JS, self-contained. Reuse only the small helpers — request(), toast(),
escapeHtml(), timestamp() — and the existing styles.css classes. Do not widen the existing screen's
code to absorb this; if it needs its own page, add static/<name>.html + static/<name>.js linked
from the topnav.

Keep it under ~<N> lines. No framework, no build step.
```

Reusing the helpers but not the structure is the whole point: you get the error handling for free
without deforming anything.

---

## Non-negotiables for any UI prompt

Append these when the agent has drifted:

```
The UI must: surface the API's ApiError message (and details, joined) on failure — never a bare
"something went wrong"; disable the submit control while a request is in flight so a double-click
is one request; and re-read from the server after a write rather than patching local state.
```

## Checking it

Open it, create one thing, trigger one validation error, and watch the network tab. A screen that
renders but shows a stale field name is the most common silent failure — the JSON key in `app.js`
must match the response exactly.
