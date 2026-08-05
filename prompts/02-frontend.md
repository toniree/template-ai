# 02 · Frontend

`static/app.js` is config-driven: a screen is one entry in the `RESOURCES` object. Everything —
table, create form, filters, pagination, toasts, money formatting, status pills — is generic and
already written. Adding a screen should be a config edit, never new DOM code.

```
Add a "<resource>" screen to RESOURCES in static/app.js for GET/POST /api/<path>.

Columns: <field -> label, note which are money and which are status enums>
Create form: <fields, note which are money (typed in dollars, sent in minor units) and which are
selects populated from another resource>
<Paged: true — the endpoint returns a PageResponse envelope.>

Config only. Do not add rendering functions, do not touch index.html or styles.css.
```

## Making it look like a product, not a CRUD dump

Cheap, and it is the difference between "it works" and "I'd hand this to a customer":

```
Add a summary strip to the <resource> screen: <e.g. total outstanding, count by status, available
balance>. Use the existing `summary` hook and the money() helper.
```

```
On <resource>, a <declined/failed/rejected> result should surface as an error toast naming the
reason, not a generic success. Use the onCreated hook.
```

```
Add a row action on <resource> that <freezes/cancels/approves> via <METHOD> /api/<path>/{id}.
Use the existing rowActions config; hide it when <condition>.
```

## Retargeting the whole UI to a different domain

If the interview problem isn't cards, this is the one prompt that converts the shell:

```
Replace the RESOURCES config in static/app.js with screens for <resource A> and <resource B>,
matching the DTOs in <package A> and <package B>. Update the page <title> and .brand text in
index.html. Delete config for anything that no longer exists. Nothing below the RESOURCES block
should need to change — tell me if that turns out to be false.
```
